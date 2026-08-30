package com.fanstaf.selah.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fanstaf.selah.data.DisplayMode
import com.fanstaf.selah.data.DisplayStyle
import com.fanstaf.selah.data.SelectionStrategy
import com.fanstaf.selah.data.Settings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: Settings,
    onDuration: (Int) -> Unit,
    onMode: (DisplayMode) -> Unit,
    onSelection: (SelectionStrategy) -> Unit,
    onMinInterval: (Int) -> Unit,
    onFontScale: (Float) -> Unit,
    onDisplayStyle: (DisplayStyle) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        SettingCard("Display mode") {
            ChipRow {
                FilterChip(
                    selected = settings.mode == DisplayMode.READ,
                    onClick = { onMode(DisplayMode.READ) },
                    label = { Text("Read") },
                )
                FilterChip(
                    selected = settings.mode == DisplayMode.RECALL,
                    onClick = { onMode(DisplayMode.RECALL) },
                    label = { Text("Recall") },
                )
            }
            Text(
                if (settings.mode == DisplayMode.RECALL)
                    "Shows the reference first — try to recall it — then reveals the verse."
                else "Shows the full verse right away.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingCard("Which verse") {
            ChipRow {
                FilterChip(
                    selected = settings.selection == SelectionStrategy.SEQUENTIAL,
                    onClick = { onSelection(SelectionStrategy.SEQUENTIAL) },
                    label = { Text("In order") },
                )
                FilterChip(
                    selected = settings.selection == SelectionStrategy.RANDOM,
                    onClick = { onSelection(SelectionStrategy.RANDOM) },
                    label = { Text("Random") },
                )
                FilterChip(
                    selected = settings.selection == SelectionStrategy.SINGLE,
                    onClick = { onSelection(SelectionStrategy.SINGLE) },
                    label = { Text("Single") },
                )
            }
            if (settings.selection == SelectionStrategy.SINGLE) {
                Text(
                    "Pick the single verse on the Verses tab (tap ★).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingCard("Display style") {
            ChipRow {
                FilterChip(
                    selected = settings.displayStyle == DisplayStyle.FULLSCREEN,
                    onClick = { onDisplayStyle(DisplayStyle.FULLSCREEN) },
                    label = { Text("Full screen") },
                )
                FilterChip(
                    selected = settings.displayStyle == DisplayStyle.CARD,
                    onClick = { onDisplayStyle(DisplayStyle.CARD) },
                    label = { Text("Card") },
                )
            }
            Text(
                if (settings.displayStyle == DisplayStyle.FULLSCREEN)
                    "Covers the screen for a calm, undistracted moment. Tap anywhere to dismiss."
                else "A small floating card — never blocks your phone; tap outside to dismiss.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingCard("Duration: ${settings.durationSeconds}s") {
            Slider(
                value = settings.durationSeconds.toFloat(),
                onValueChange = { onDuration(it.roundToInt()) },
                valueRange = 3f..10f,
                steps = 6,
            )
        }

        SettingCard("How often") {
            ChipRow {
                intervalChip("Every unlock", 0, settings.minIntervalMinutes, onMinInterval)
                intervalChip("15 min", 15, settings.minIntervalMinutes, onMinInterval)
                intervalChip("30 min", 30, settings.minIntervalMinutes, onMinInterval)
                intervalChip("1 hr", 60, settings.minIntervalMinutes, onMinInterval)
                intervalChip("2 hr", 120, settings.minIntervalMinutes, onMinInterval)
            }
            Text(
                "How long to wait after showing a verse before showing the next one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingCard("Text size: ${(settings.fontScale * 100).roundToInt()}%") {
            Slider(
                value = settings.fontScale,
                onValueChange = { onFontScale((it * 10).roundToInt() / 10f) },
                valueRange = 0.8f..1.6f,
                steps = 7,
            )
        }
    }
}

@Composable
private fun intervalChip(
    label: String,
    minutes: Int,
    current: Int,
    onSelect: (Int) -> Unit,
) {
    FilterChip(
        selected = current == minutes,
        onClick = { onSelect(minutes) },
        label = { Text(label) },
    )
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
