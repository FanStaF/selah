package com.fanstaf.selah.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fanstaf.selah.data.DisplayStyle
import com.fanstaf.selah.data.SelectionStrategy
import com.fanstaf.selah.data.Settings
import com.fanstaf.selah.data.Verse
import com.fanstaf.selah.data.VerseSet
import com.fanstaf.selah.data.sortVerses
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    settings: Settings,
    verses: List<Verse>,
    sets: List<VerseSet>,
    overlayGranted: Boolean,
    onEnableToggle: (Boolean) -> Unit,
    onRequestOverlay: () -> Unit,
    onPreview: () -> Unit,
    onSelectSourceSet: (Long) -> Unit,
    onSingleSource: () -> Unit,
    onOrderRandom: (Boolean) -> Unit,
    onDuration: (Int) -> Unit,
    onMinInterval: (Int) -> Unit,
    onFontScale: (Float) -> Unit,
    onDisplayStyle: (DisplayStyle) -> Unit,
    onPickSingleExisting: (Long) -> Unit,
    onTypeSingle: (String, String, String) -> Unit,
    onPickSingleFromBible: () -> Unit,
) {
    val isSingle = settings.selection == SelectionStrategy.SINGLE
    val singleVerse = verses.firstOrNull { it.id == settings.singleVerseId }
    val scopedCount = when {
        isSingle -> if (singleVerse != null) 1 else 0
        settings.scopeSetId == VerseSet.ALL -> verses.count { it.active }
        else -> verses.count { it.active && it.setId == settings.scopeSetId }
    }

    var showMore by remember { mutableStateOf(false) }
    var singleMenu by remember { mutableStateOf(false) }
    var choosingExisting by remember { mutableStateOf(false) }
    var typingSingle by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Selah", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Scripture in the moments between",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Enable
        Card {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show a verse after unlock", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (settings.enabled) "On — a verse appears when you unlock" else "Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.enabled, onCheckedChange = onEnableToggle)
            }
        }

        if (!overlayGranted) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
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

        // What you'll see — the source control
        SectionCard("What you'll see") {
            SourceDropdown(
                sets = sets,
                isSingle = isSingle,
                scopeSetId = settings.scopeSetId,
                onSelectSet = onSelectSourceSet,
                onSingle = onSingleSource,
            )

            if (isSingle) {
                Text(
                    singleVerse?.let { "${it.reference}  ·  ${it.translation}" } ?: "No verse chosen yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                singleVerse?.text?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Box {
                    OutlinedButton(onClick = { singleMenu = true }) { Text("Choose verse") }
                    DropdownMenu(expanded = singleMenu, onDismissRequest = { singleMenu = false }) {
                        DropdownMenuItem(text = { Text("From your verses") }, onClick = { singleMenu = false; choosingExisting = true })
                        DropdownMenuItem(text = { Text("Type a new verse") }, onClick = { singleMenu = false; typingSingle = true })
                        DropdownMenuItem(text = { Text("Select from the Bible") }, onClick = { singleMenu = false; onPickSingleFromBible() })
                    }
                }
            } else {
                // Order toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.selection == SelectionStrategy.SEQUENTIAL,
                        onClick = { onOrderRandom(false) },
                        label = { Text("In order") },
                    )
                    FilterChip(
                        selected = settings.selection == SelectionStrategy.RANDOM,
                        onClick = { onOrderRandom(true) },
                        label = { Text("Random") },
                    )
                }
            }

            Text(
                "$scopedCount ${if (scopedCount == 1) "verse" else "verses"} in rotation",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onPreview) { Text("Preview verse now") }
        }

        // How it appears
        SectionCard("How it appears") {
            Text("Duration: ${settings.durationSeconds}s", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = { onDuration((settings.durationSeconds - 1).coerceAtLeast(2)) },
                    enabled = settings.durationSeconds > 2,
                ) { Icon(Icons.Filled.Remove, contentDescription = "Less") }
                Text("${settings.durationSeconds}s", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
                FilledTonalIconButton(
                    onClick = { onDuration((settings.durationSeconds + 1).coerceAtMost(60)) },
                    enabled = settings.durationSeconds < 60,
                ) { Icon(Icons.Filled.Add, contentDescription = "More") }
            }

            Text("How often", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                intervalChip("Every unlock", 0, settings.minIntervalMinutes, onMinInterval)
                intervalChip("15 min", 15, settings.minIntervalMinutes, onMinInterval)
                intervalChip("30 min", 30, settings.minIntervalMinutes, onMinInterval)
                intervalChip("1 hr", 60, settings.minIntervalMinutes, onMinInterval)
                intervalChip("2 hr", 120, settings.minIntervalMinutes, onMinInterval)
            }

            TextButton(onClick = { showMore = !showMore }) {
                Icon(if (showMore) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                Text(if (showMore) "  Fewer options" else "  More options")
            }
            AnimatedVisibility(visible = showMore) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Display style", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(settings.displayStyle == DisplayStyle.FULLSCREEN, { onDisplayStyle(DisplayStyle.FULLSCREEN) }, { Text("Full screen") })
                        FilterChip(settings.displayStyle == DisplayStyle.CARD, { onDisplayStyle(DisplayStyle.CARD) }, { Text("Card") })
                    }
                    Text("Text size: ${(settings.fontScale * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = settings.fontScale,
                        onValueChange = { onFontScale((it * 10).roundToInt() / 10f) },
                        valueRange = 0.8f..1.6f,
                        steps = 7,
                    )
                }
            }
        }

        // Reliability
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
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
    }

    if (choosingExisting) {
        ChooseVerseDialog(
            verses = sortVerses(verses, com.fanstaf.selah.data.SortOrder.BIBLICAL),
            onDismiss = { choosingExisting = false },
            onPick = { id -> onPickSingleExisting(id); choosingExisting = false },
        )
    }
    if (typingSingle) {
        TypeVerseDialog(
            onDismiss = { typingSingle = false },
            onSave = { ref, text, tr -> onTypeSingle(ref, text, tr); typingSingle = false },
        )
    }
}

@Composable
private fun SourceDropdown(
    sets: List<VerseSet>,
    isSingle: Boolean,
    scopeSetId: Long,
    onSelectSet: (Long) -> Unit,
    onSingle: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when {
        isSingle -> "Single verse"
        scopeSetId == VerseSet.ALL -> "All verses"
        else -> sets.firstOrNull { it.id == scopeSetId }?.name ?: "All verses"
    }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(label, maxLines = 1)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose source")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All verses") }, onClick = { onSelectSet(VerseSet.ALL); expanded = false })
            sets.forEach { s ->
                DropdownMenuItem(text = { Text(s.name) }, onClick = { onSelectSet(s.id); expanded = false })
            }
            Divider()
            DropdownMenuItem(text = { Text("Single verse") }, onClick = { onSingle(); expanded = false })
        }
    }
}

@Composable
private fun ChooseVerseDialog(
    verses: List<Verse>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a verse") },
        text = {
            if (verses.isEmpty()) {
                Text("You haven't added any verses yet.")
            } else {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    verses.forEach { v ->
                        TextButton(onClick = { onPick(v.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${v.reference}  ·  ${v.translation}", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TypeVerseDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var reference by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Type a verse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.OutlinedTextField(reference, { reference = it }, label = { Text("Reference (e.g. John 3:16)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(text, { text = it }, label = { Text("Verse text") }, modifier = Modifier.fillMaxWidth())
                androidx.compose.material3.OutlinedTextField(translation, { translation = it }, label = { Text("Translation (e.g. KJV, ESV)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(reference.trim(), text.trim(), translation.trim()) },
                enabled = reference.isNotBlank() && text.isNotBlank(),
            ) { Text("Set verse") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun intervalChip(label: String, minutes: Int, current: Int, onSelect: (Int) -> Unit) {
    FilterChip(selected = current == minutes, onClick = { onSelect(minutes) }, label = { Text(label) })
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
