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

// Upload the queue, then download new community prices. Runs on a worker thread.
object SyncManager {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val MIN_GAP_MS = 30_000L           // throttle for automatic triggers
    private val running = AtomicBoolean(false)       // only one sync at a time
    @Volatile private var lastFinished = 0L

    data class Result(val uploaded: Int, val downloaded: Int, val message: String)

    // force = true for the Sync button and Submit; automatic triggers are throttled.
    // onDone (optional) is called on the UI thread, so it may touch Views.
    fun syncNow(ctx: Context, force: Boolean = false, onDone: ((Result) -> Unit)? = null) {
        val app = ctx.applicationContext             // never keep an Activity alive in a thread
        val main = Handler(Looper.getMainLooper())
        if (!force && System.currentTimeMillis() - lastFinished < MIN_GAP_MS) {
            Log.d("SYNC", "throttled: synced less than 30 s ago")
            onDone?.let { main.post { it(Result(0, 0, "Synced moments ago")) } }
            return
        }
        if (!running.compareAndSet(false, true)) {
            onDone?.let { main.post { it(Result(0, 0, "A sync is already running")) } }
            return
        }
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

    private fun run(ctx: Context): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result(0, 0, "Not signed in")
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
                // anything else (e.g. rules rejected this row): skip it, keep going
            }
        }

        // 2. DOWNLOAD. Look back 14 days past the last sync: an offline phone may upload old rows late.
        val prefs = ctx.getSharedPreferences("sync", Context.MODE_PRIVATE)
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

    private fun isConnectionProblem(ex: Exception): Boolean {
        val cause = (ex as? ExecutionException)?.cause ?: ex
        return ex is TimeoutException ||
                (cause is FirebaseFirestoreException &&
                        cause.code == FirebaseFirestoreException.Code.UNAVAILABLE)
    }
}