package com.fanstaf.selah.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.fanstaf.selah.data.Verse

@Composable
fun VersesScreen(
    modifier: Modifier = Modifier,
    verses: List<Verse>,
    singleVerseId: Long,
    selectionIsSingle: Boolean,
    onAdd: (String, String, String) -> Unit,
    onUpdate: (Verse) -> Unit,
    onDelete: (Verse) -> Unit,
    onSetActive: (Long, Boolean) -> Unit,
    onSetSingle: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf<Verse?>(null) }
    var adding by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Your verses",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(verses, key = { it.id }) { verse ->
                VerseRow(
                    verse = verse,
                    isSingle = selectionIsSingle && verse.id == singleVerseId,
                    showStar = selectionIsSingle,
                    onToggleActive = { onSetActive(verse.id, it) },
                    onEdit = { editing = verse },
                    onDelete = { onDelete(verse) },
                    onSetSingle = { onSetSingle(verse.id) },
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add verse") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }

    if (adding) {
        VerseDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = { ref, text, tr -> onAdd(ref, text, tr); adding = false },
        )
    }
    editing?.let { verse ->
        VerseDialog(
            initial = verse,
            onDismiss = { editing = null },
            onSave = { ref, text, tr ->
                onUpdate(verse.copy(reference = ref, text = text, translation = tr))
                editing = null
            },
        )
    }
}

@Composable
private fun VerseRow(
    verse: Verse,
    isSingle: Boolean,
    showStar: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetSingle: () -> Unit,
) {
    Card {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${verse.reference}  ·  ${verse.translation}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        verse.text,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                    )
                }
                if (showStar) {
                    IconButton(onClick = onSetSingle) {
                        Icon(
                            imageVector = if (isSingle) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Use as single verse",
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (verse.active) "In rotation" else "Paused",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = verse.active,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Box(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
private fun VerseDialog(
    initial: Verse?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var reference by remember { mutableStateOf(initial?.reference ?: "") }
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var translation by remember { mutableStateOf(initial?.translation ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add verse" else "Edit verse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference (e.g. John 3:16)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Verse text") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text("Translation (e.g. KJV, ESV)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(reference.trim(), text.trim(), translation.trim()) },
                enabled = reference.isNotBlank() && text.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
