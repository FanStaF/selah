package com.fanstaf.selah.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fanstaf.selah.data.Settings

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    settings: Settings,
    overlayGranted: Boolean,
    activeCount: Int,
    onRequestOverlay: () -> Unit,
    onEnableToggle: (Boolean) -> Unit,
    onPreview: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Selah", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Scripture in the moments between",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Master enable card
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Show a verse after unlock", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (settings.enabled) "On — a verse appears when you unlock"
                        else "Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.enabled, onCheckedChange = onEnableToggle)
            }
        }

        if (!overlayGranted) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Permission needed", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Selah draws the verse over your screen for a few seconds. Grant \"Display over " +
                            "other apps\" so it can appear after you unlock. It never blocks your phone.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = onRequestOverlay) { Text("Grant permission") }
                }
            }
        }

        // Preview / status
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "$activeCount ${if (activeCount == 1) "verse" else "verses"} in your rotation",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Mode: ${settings.mode.name.lowercase().replaceFirstChar { it.uppercase() }}  ·  " +
                        "${settings.durationSeconds}s  ·  " +
                        if (settings.minIntervalMinutes == 0) "every unlock"
                        else "at most every ${settings.minIntervalMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onPreview) { Text("Preview verse now") }
            }
        }

        // OEM survival note
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Keeping it reliable", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Some phones (Samsung, Xiaomi, Oppo, and others) aggressively stop background " +
                        "apps, which can keep the verse from appearing. If it stops working, allow " +
                        "Selah to run in the background / disable battery optimization and enable " +
                        "autostart for it in your system settings.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
