package com.fanstaf.selah.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fanstaf.selah.AppGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Restarts the unlock service after a reboot — but only if the user had it enabled. BOOT_COMPLETED
 * *is* an exempted broadcast, so this manifest receiver is delivered.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        AppGraph.init(context)
        val enabled = runBlocking { AppGraph.settings.settings.first().enabled }
        if (enabled) UnlockService.start(context)
    }
}
