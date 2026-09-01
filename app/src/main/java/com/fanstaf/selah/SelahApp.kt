package com.fanstaf.selah

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.fanstaf.selah.service.UnlockService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SelahApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        createNotificationChannel()
        // Seed bundled starter verses + the KJV corpus off the main thread on first launch.
        // The corpus is seeded once (tracked by a flag) so a later rename/delete doesn't re-add it.
        CoroutineScope(Dispatchers.IO).launch {
            AppGraph.repository.ensureSeeded()
            // Fill in book/chapter/verse for any verses missing them (bundled starters, older
            // typed verses) so Biblical sort covers everything.
            AppGraph.repository.backfillCoords()
            val settings = AppGraph.settings.settings.first()
            if (!settings.kjvSeeded) {
                if (!AppGraph.corpus.hasTranslation("KJV")) AppGraph.corpus.importBundledKjv()
                AppGraph.settings.setKjvSeeded(true)
            }
        }
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
