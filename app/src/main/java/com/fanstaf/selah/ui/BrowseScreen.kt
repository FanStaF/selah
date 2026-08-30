package com.fanstaf.selah.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fanstaf.selah.data.BookNames
import com.fanstaf.selah.data.CorpusTranslation
import com.fanstaf.selah.data.CorpusVerse
import com.fanstaf.selah.data.VerseSet
import com.fanstaf.selah.ui.theme.bookColor

@Composable
fun BrowseScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel,
    translations: List<CorpusTranslation>,
    onImport: () -> Unit,
) {
    val sets by vm.sets.collectAsState()
    val selectedSetId by vm.selectedSetId.collectAsState()
    val editingTranslation by vm.editingTranslation.collectAsState()
    var pendingAdd by remember { mutableStateOf<CorpusVerse?>(null) }

    var code by remember { mutableStateOf<String?>(null) }
    var book by remember { mutableStateOf<Int?>(null) }
    var chapter by remember { mutableStateOf<Int?>(null) }
    var selectedVerse by remember { mutableStateOf<Int?>(null) }

    var books by remember { mutableStateOf<List<Int>>(emptyList()) }
    var chapters by remember { mutableStateOf<List<Int>>(emptyList()) }
    var verses by remember { mutableStateOf<List<CorpusVerse>>(emptyList()) }

    LaunchedEffect(translations) {
        if (code == null || translations.none { it.code == code }) {
            code = translations.firstOrNull { it.code == "KJV" }?.code ?: translations.firstOrNull()?.code
        }
    }
    LaunchedEffect(code) {
        book = null; chapter = null; selectedVerse = null
        books = code?.let { vm.books(it) } ?: emptyList()
    }
    LaunchedEffect(code, book) {
        chapter = null; selectedVerse = null
        chapters = if (code != null && book != null) vm.chapters(code!!, book!!) else emptyList()
    }
    LaunchedEffect(code, book, chapter) {
        selectedVerse = null
        verses = if (code != null && book != null && chapter != null) vm.versesIn(code!!, book!!, chapter!!) else emptyList()
    }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Browse", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onImport) {
                Icon(Icons.Filled.FileDownload, contentDescription = null)
                Text("  Import", maxLines = 1)
            }
        }

        // Translation selector
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            translations.forEach { t ->
                TranslationChip(
                    code = t.code,
                    selected = t.code == code,
                    onClick = { code = t.code },
                    onLongClick = { vm.openTranslationEditor(t) },
                )
            }
        }

        when {
            code == null -> Text(
                "No translations yet. Import a Bible XML file to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Step 1 — book grid (color-coded by genre)
            book == null -> LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(books, key = { it }) { b ->
                    GridCell(
                        label = BookNames.abbrev(b),
                        bg = bookColor(b),
                        fg = Color.White,
                        onClick = { book = b },
                    )
                }
            }

            // Step 2 — chapter grid
            chapter == null -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Crumb(BookNames.name(book!!)) { book = null }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(chapters, key = { it }) { c ->
                        GridCell(
                            label = "$c",
                            bg = MaterialTheme.colorScheme.surfaceVariant,
                            fg = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { chapter = c },
                        )
                    }
                }
            }

            // Step 3 — verse-number grid; tap navigates to the verse in context
            selectedVerse == null -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Crumb("${BookNames.name(book!!)} $chapter") { chapter = null }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(verses, key = { it.id }) { v ->
                        GridCell(
                            label = "${v.verse}",
                            bg = MaterialTheme.colorScheme.surfaceVariant,
                            fg = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { selectedVerse = v.verse },
                        )
                    }
                }
            }

            // Step 4 — read the chapter in context; the chosen verse is highlighted, tap + to add
            else -> ReadingView(
                modifier = Modifier.weight(1f),
                bookName = BookNames.name(book!!),
                chapter = chapter!!,
                verses = verses,
                selectedVerse = selectedVerse!!,
                onBack = { selectedVerse = null },
                onAdd = { pendingAdd = it },
            )
        }
    }

    pendingAdd?.let { cv ->
        SetPickerDialog(
            sets = sets,
            defaultSetId = selectedSetId,
            onDismiss = { pendingAdd = null },
            onPick = { setId -> vm.addToStudy(cv, setId); pendingAdd = null },
            onCreateAndPick = { name -> vm.createSet(name) { id -> vm.addToStudy(cv, id) }; pendingAdd = null },
        )
    }

    editingTranslation?.let { t ->
        TranslationDialog(
            translation = t,
            onDismiss = { vm.closeTranslationEditor() },
            onSave = { c, n -> vm.saveTranslation(t, c, n) },
            onDelete = { vm.deleteTranslation(t) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TranslationChip(
    code: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Text(
            code,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TranslationDialog(
    translation: CorpusTranslation,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    var codeText by remember(translation) { mutableStateOf(translation.code) }
    var nameText by remember(translation) { mutableStateOf(translation.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Translation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${translation.verseCount} verses",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = codeText, onValueChange = { codeText = it },
                    label = { Text("Short code (shown on chips)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = nameText, onValueChange = { nameText = it },
                    label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = onDelete) { Text("Remove this translation") }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(codeText, nameText) }, enabled = codeText.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SetPickerDialog(
    sets: List<VerseSet>,
    defaultSetId: Long,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    onCreateAndPick: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    // Show the current set first as the obvious default.
    val ordered = sets.sortedByDescending { it.id == defaultSetId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) "New set" else "Add to which set?") },
        text = {
            if (creating) {
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    label = { Text("Set name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column {
                    ordered.forEach { s ->
                        val isDefault = s.id == defaultSetId
                        TextButton(
                            onClick = { onPick(s.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (isDefault) "${s.name}  (current)" else s.name,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Divider()
                    TextButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("＋ New set…", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(
                    onClick = { onCreateAndPick(newName.trim()) },
                    enabled = newName.isNotBlank(),
                ) { Text("Create & add") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        dismissButton = if (creating) {
            { TextButton(onClick = { creating = false }) { Text("Back") } }
        } else null,
    )
}

@Composable
private fun ReadingView(
    modifier: Modifier = Modifier,
    bookName: String,
    chapter: Int,
    verses: List<CorpusVerse>,
    selectedVerse: Int,
    onBack: () -> Unit,
    onAdd: (CorpusVerse) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedVerse, verses) {
        val idx = verses.indexOfFirst { it.verse == selectedVerse }
        if (idx >= 0) listState.scrollToItem(idx)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Crumb("$bookName $chapter") { onBack() }
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(verses, key = { it.id }) { v ->
                val highlighted = v.verse == selectedVerse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (highlighted) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        )
                        .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        "${v.verse}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 8.dp, top = 4.dp),
                    )
                    Text(
                        v.text,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onAdd(v) }) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = "Add ${v.verse} to my verses",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCell(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(2.dp),
        )
    }
}

@Composable
private fun Crumb(label: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onBack).padding(vertical = 4.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
        Text(
            "  $label",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
