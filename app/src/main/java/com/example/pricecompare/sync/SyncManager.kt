package com.example.pricecompare.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.pricecompare.db.PriceDao
import com.example.pricecompare.logic.PriceSelector
import com.example.pricecompare.util.NetworkUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

// Coordinates background two-way sync: uploads pending prices, then downloads new ones.
// Used by MainActivity, SubmitFragment, ProfileFragment, and SyncReceiver.
object SyncManager {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val MIN_GAP_MS = 30_000L           // throttle for automatic triggers
    private val running = AtomicBoolean(false)       // only one sync at a time
    @Volatile private var lastFinished = 0L

    data class Result(val uploaded: Int, val downloaded: Int, val message: String)

    // Starts a background sync if allowed by throttle and concurrency, then posts to onDone.
    // `force = true` bypasses the 30-second cooldown for manual Sync or Submit actions.
    fun syncNow(ctx: Context, force: Boolean = false, onDone: ((Result) -> Unit)? = null) {
        val app = ctx.applicationContext             // never keep an Activity alive in a thread
        // Handler tied to the main (UI) thread so callbacks can safely update Views.
        val main = Handler(Looper.getMainLooper())
        // Skip automatic syncs within 30 seconds of the last successful sync.
        if (!force && System.currentTimeMillis() - lastFinished < MIN_GAP_MS) {
            Log.d("SYNC", "throttled: synced less than 30 s ago")
            onDone?.let { main.post { it(Result(0, 0, "Synced moments ago")) } }
            return
        }
        // Atomically ensures only one sync thread runs at a time.
        if (!running.compareAndSet(false, true)) {
            onDone?.let { main.post { it(Result(0, 0, "A sync is already running")) } }
            return
        }
        // Move network and database I/O onto a worker thread off the UI thread.
        Thread {
            val result = try {
                run(app)
            } catch (e: Exception) {
                Result(0, 0, "Sync failed (${e.javaClass.simpleName}). Will retry later.")
            } finally {
                running.set(false)
            }
            Log.d("SYNC", "finished: ${result.message}")
            onDone?.let { main.post { it(result) } }
        }.start()
    }

    // Executes upload then download synchronously on the worker thread and returns a Result.
    private fun run(ctx: Context): Result {
        // Bail out early if the user is not signed in to Firebase Auth.
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result(0, 0, "Not signed in")
        // Bail out when offline so queued rows stay local for the next sync.
        if (!NetworkUtil.isOnline(ctx)) return Result(0, 0, "Offline: your prices are saved and will upload later")
        val dao = PriceDao(ctx)

        // 1. UPLOAD this user's queue, oldest first.
        var uploaded = 0
        for (e in dao.pending(uid)) {
            try {
                FirestoreRepository.upload(e)
                dao.markSynced(e.id, e.remoteId!!)
                uploaded++
            } catch (ex: Exception) {
                if (isConnectionProblem(ex)) break   // no point trying the rest
                // Permanent row error (e.g. security rule rejection): stop retrying this row.
                dao.markFailed(e.id)
            }
        }

        // 2. DOWNLOAD. Look back 14 days past the last sync: an offline phone may upload old rows late.
        val prefs = ctx.getSharedPreferences("sync", Context.MODE_PRIVATE)
        // Re-scan 14 days before last sync to catch delayed uploads from offline phones.
        val since = (prefs.getLong("last_download", 0L) - PriceSelector.FRESH_DAYS * DAY_MS)
            .coerceAtLeast(0L)
        val started = System.currentTimeMillis()
        var downloaded = 0
        for (e in FirestoreRepository.fetchSince(since)) {
            if (!dao.hasRemote(e.remoteId!!) && dao.insert(e) != -1L) downloaded++   // skip duplicates
        }
        prefs.edit().putLong("last_download", started).apply()
        lastFinished = System.currentTimeMillis()    // only a real sync starts the throttle window
        return Result(uploaded, downloaded, "Uploaded $uploaded, downloaded $downloaded")
    }

    // Returns true if an upload exception is a transient network or Firestore outage.
    private fun isConnectionProblem(ex: Exception): Boolean {
        val cause = (ex as? ExecutionException)?.cause ?: ex
        return ex is TimeoutException ||
                cause is TimeoutException ||
                (cause is FirebaseFirestoreException &&
                        cause.code == FirebaseFirestoreException.Code.UNAVAILABLE)
    }
}