package com.fanstaf.selah.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.fanstaf.selah.AppGraph
import com.fanstaf.selah.MainActivity
import com.fanstaf.selah.R
import com.fanstaf.selah.data.SelectionStrategy
import com.fanstaf.selah.data.Settings
import com.fanstaf.selah.data.Verse
import com.fanstaf.selah.ui.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The persistent process that catches unlock. ACTION_USER_PRESENT is not an exempted implicit
 * broadcast, so the receiver is registered at runtime here and the service keeps it alive. On
 * unlock it draws a verse overlay (via [OverlayController]) subject to the frequency gate, then
 * gets out of the way.
 */
class UnlockService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var overlay: OverlayController
    private var receiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        overlay = OverlayController(this)
        registerUnlockReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        return START_STICKY
    }

    private fun registerUnlockReceiver() {
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "receiver onReceive: ${intent.action}")
                if (intent.action == Intent.ACTION_USER_PRESENT) {
                    scope.launch { onUnlock() }
                }
            }
        }
        receiver = r
        // USER_PRESENT is a protected broadcast (only the system can send it), so EXPORTED is safe
        // and — unlike NOT_EXPORTED, which was observed to drop it on-device — actually delivered.
        ContextCompat.registerReceiver(
            this,
            r,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_EXPORTED,
        )
        Log.d(TAG, "USER_PRESENT receiver registered")
    }

    private suspend fun onUnlock() {
        val settings = AppGraph.settings.settings.first()
        Log.d(TAG, "onUnlock: enabled=${settings.enabled} interval=${settings.minIntervalMinutes} lastShown=${settings.lastShownAt}")
        if (!settings.enabled) return

        val now = System.currentTimeMillis()
        if (settings.minIntervalMinutes > 0 &&
            now - settings.lastShownAt < settings.minIntervalMinutes * 60_000L
        ) {
            Log.d(TAG, "onUnlock: skipped by frequency gate")
            return
        }
        if (isCallActive()) {
            Log.d(TAG, "onUnlock: skipped (call active)")
            return
        }

        val chosen = withContext(Dispatchers.IO) { selectVerse(settings) }
        if (chosen == null) {
            Log.d(TAG, "onUnlock: no active verse to show")
            return
        }

        Log.d(TAG, "onUnlock: showing ${chosen.verse.reference}")
        overlay.show(chosen.verse, settings.displayStyle, settings.durationSeconds, settings.fontScale)

        scope.launch(Dispatchers.IO) {
            AppGraph.repository.markShown(chosen.verse.id, now)
            AppGraph.settings.setLastShownAt(now)
            if (settings.selection == SelectionStrategy.SEQUENTIAL) {
                AppGraph.settings.setCursor(chosen.nextCursor)
            }
        }
    }

    private data class Pick(val verse: Verse, val nextCursor: Int)

    private suspend fun selectVerse(settings: Settings): Pick? {
        // Sequential rotation follows the same order as the Verses list (default Biblical).
        val active = com.fanstaf.selah.data.sortVerses(
            AppGraph.repository.activeVersesScoped(settings.scopeSetId),
            settings.sortOrder,
        )
        if (active.isEmpty()) return null
        return when (settings.selection) {
            SelectionStrategy.SINGLE -> {
                val v = active.firstOrNull { it.id == settings.singleVerseId } ?: active.first()
                Pick(v, settings.sequentialCursor)
            }
            SelectionStrategy.RANDOM -> Pick(active.random(), settings.sequentialCursor)
            SelectionStrategy.SEQUENTIAL -> {
                val idx = ((settings.sequentialCursor % active.size) + active.size) % active.size
                Pick(active[idx], settings.sequentialCursor + 1)
            }
        }
    }

    private fun isCallActive(): Boolean {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        return am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.fgs_notification_title))
            .setContentText(getString(R.string.fgs_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(open)
            .build()
    }

    override fun onDestroy() {
        receiver?.let { runCatching { unregisterReceiver(it) } }
        receiver = null
        overlay.removeNow()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SelahSvc"
        const val CHANNEL_ID = "selah_unlock"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.fanstaf.selah.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, UnlockService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, UnlockService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
