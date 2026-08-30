package com.fanstaf.selah.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fanstaf.selah.ui.theme.bookColor

@Composable
fun BrowseScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel,
    translations: List<CorpusTranslation>,
    onImport: () -> Unit,
) {
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
                FilterChip(selected = t.code == code, onClick = { code = t.code }, label = { Text(t.code) })
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
                onAdd = { vm.addToStudy(it) },
            )
        }
    }
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
