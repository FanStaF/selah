package com.fanstaf.selah.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.fanstaf.selah.data.BookNames
import com.fanstaf.selah.data.CorpusTranslation
import com.fanstaf.selah.data.CorpusVerse

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

    var books by remember { mutableStateOf<List<Int>>(emptyList()) }
    var chapters by remember { mutableStateOf<List<Int>>(emptyList()) }
    var verses by remember { mutableStateOf<List<CorpusVerse>>(emptyList()) }

    LaunchedEffect(translations) {
        if (code == null || translations.none { it.code == code }) {
            code = translations.firstOrNull { it.code == "KJV" }?.code ?: translations.firstOrNull()?.code
        }
    }
    LaunchedEffect(code) {
        book = null; chapter = null
        books = code?.let { vm.books(it) } ?: emptyList()
    }
    LaunchedEffect(code, book) {
        chapter = null
        chapters = if (code != null && book != null) vm.chapters(code!!, book!!) else emptyList()
    }
    LaunchedEffect(code, book, chapter) {
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
                FilterChip(
                    selected = t.code == code,
                    onClick = { code = t.code },
                    label = { Text(t.code) },
                )
            }
        }

        when {
            code == null -> Text(
                "No translations yet. Import a Bible XML file to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            book == null -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(books, key = { it }) { b ->
                    Card(onClick = { book = b }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            BookNames.name(b),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            chapter == null -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Crumb(BookNames.name(book!!)) { book = null }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(chapters, key = { it }) { c ->
                        ElevatedCard(onClick = { chapter = c }, modifier = Modifier.size(56.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("$c", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Crumb("${BookNames.name(book!!)} $chapter") { chapter = null }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(verses, key = { it.id }) { v ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${v.verse}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(end = 10.dp),
                                )
                                Text(
                                    v.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { vm.addToStudy(v) }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add to my verses")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Crumb(label: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AssistChip(
            onClick = onBack,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
        )
    }
}
