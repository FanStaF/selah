package com.fanstaf.selah

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.fanstaf.selah.service.UnlockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SelahApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        createNotificationChannel()
        // Seed bundled verses off the main thread on first launch.
        CoroutineScope(Dispatchers.IO).launch { AppGraph.repository.ensureSeeded() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UnlockService.CHANNEL_ID,
                getString(R.string.fgs_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = getString(R.string.fgs_channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
