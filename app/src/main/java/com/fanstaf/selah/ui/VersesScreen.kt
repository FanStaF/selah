package com.fanstaf.selah.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.fanstaf.selah.data.SortOrder
import com.fanstaf.selah.data.Verse
import com.fanstaf.selah.data.VerseSet
import com.fanstaf.selah.data.sortVerses
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun VersesScreen(
    modifier: Modifier = Modifier,
    verses: List<Verse>,
    sets: List<VerseSet>,
    selectedSetId: Long,
    singleVerseId: Long,
    selectionIsSingle: Boolean,
    sortOrder: SortOrder,
    compact: Boolean,
    onSelectSet: (Long) -> Unit,
    onCreateSet: (String) -> Unit,
    onRenameSet: (VerseSet, String) -> Unit,
    onDeleteSet: (VerseSet) -> Unit,
    onSortOrder: (SortOrder) -> Unit,
    onEnableManual: (List<Long>) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onToggleCompact: () -> Unit,
    onAdd: (String, String, String, Long) -> Unit,
    onUpdate: (Verse) -> Unit,
    onDelete: (Verse) -> Unit,
    onSetActive: (Long, Boolean) -> Unit,
    onSetSingle: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf<Verse?>(null) }
    var adding by remember { mutableStateOf(false) }
    var creatingSet by remember { mutableStateOf(false) }
    var renamingSet by remember { mutableStateOf<VerseSet?>(null) }
    var confirmDeleteSet by remember { mutableStateOf<VerseSet?>(null) }
    // Id of the verse the carousel viewer opened at (null = viewer closed).
    var viewerStart by remember { mutableStateOf<Long?>(null) }

    val currentSet = sets.firstOrNull { it.id == selectedSetId }
    val filtered = if (selectedSetId == VerseSet.ALL) verses else verses.filter { it.setId == selectedSetId }
    val shown = sortVerses(filtered, sortOrder)

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SetSelectorRow(
                sets = sets,
                currentSet = currentSet,
                sortOrder = sortOrder,
                compact = compact,
                onSelectSet = onSelectSet,
                onNewSet = { creatingSet = true },
                onRename = { currentSet?.let { renamingSet = it } },
                onDelete = { currentSet?.let { confirmDeleteSet = it } },
                onSortOrder = onSortOrder,
                onManual = { onEnableManual(shown.map { it.id }) },
                onToggleCompact = onToggleCompact,
            )

            if (shown.isEmpty()) {
                Text(
                    "No verses here yet. Use Browse to pick verses, or ＋ to type one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else if (sortOrder == SortOrder.MANUAL) {
                ManualVerseList(
                    shown = shown,
                    compact = compact,
                    singleVerseId = singleVerseId,
                    selectionIsSingle = selectionIsSingle,
                    onReorder = onReorder,
                    onToggleActive = onSetActive,
                    onEdit = { editing = it },
                    onDelete = onDelete,
                    onSetSingle = onSetSingle,
                    onOpen = { viewerStart = it.id },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
                ) {
                    items(shown, key = { it.id }) { verse ->
                        val isSingle = selectionIsSingle && verse.id == singleVerseId
                        if (compact) {
                            CompactVerseRow(verse, isSingle, selectionIsSingle, { onSetActive(verse.id, it) }, { editing = verse }, { onDelete(verse) }, { onSetSingle(verse.id) }, { viewerStart = verse.id })
                        } else {
                            VerseRow(verse, isSingle, selectionIsSingle, { onSetActive(verse.id, it) }, { editing = verse }, { onDelete(verse) }, { onSetSingle(verse.id) }, { viewerStart = verse.id })
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add verse") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )

        val viewerIndex = viewerStart?.let { id -> shown.indexOfFirst { it.id == id } } ?: -1
        androidx.compose.runtime.LaunchedEffect(viewerStart, viewerIndex) {
            if (viewerStart != null && viewerIndex < 0) viewerStart = null
        }
        if (viewerStart != null && viewerIndex >= 0) {
            VerseViewer(
                verses = shown,
                startIndex = viewerIndex,
                onClose = { viewerStart = null },
                onToggleActive = onSetActive,
                onEdit = { editing = it },
                onDelete = { v ->
                    val wasLast = shown.size <= 1
                    onDelete(v)
                    if (wasLast) viewerStart = null
                },
            )
        }
    }

    if (adding) {
        VerseDialog(null, { adding = false }, { ref, text, tr -> onAdd(ref, text, tr, selectedSetId); adding = false })
    }
    editing?.let { verse ->
        VerseDialog(verse, { editing = null }, { ref, text, tr ->
            onUpdate(verse.copy(reference = ref, text = text, translation = tr)); editing = null
        })
    }
    if (creatingSet) {
        SetNameDialog("New set", "", { creatingSet = false }, { name -> onCreateSet(name); creatingSet = false })
    }
    renamingSet?.let { s ->
        SetNameDialog("Rename set", s.name, { renamingSet = null }, { name -> onRenameSet(s, name); renamingSet = null })
    }
    confirmDeleteSet?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDeleteSet = null },
            title = { Text("Delete \"${s.name}\"?") },
            text = { Text("Verses in this set will move to another set. This can't be undone.") },
            confirmButton = { TextButton(onClick = { onDeleteSet(s); confirmDeleteSet = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDeleteSet = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ManualVerseList(
    shown: List<Verse>,
    compact: Boolean,
    singleVerseId: Long,
    selectionIsSingle: Boolean,
    onReorder: (List<Long>) -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onEdit: (Verse) -> Unit,
    onDelete: (Verse) -> Unit,
    onSetSingle: (Long) -> Unit,
    onOpen: (Verse) -> Unit,
) {
    val listState = rememberLazyListState()
    // Local working copy; reset only when the set of verses changes (not on mere reordering).
    var data by remember(shown.map { it.id }.toSet()) { mutableStateOf(shown) }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        data = data.toMutableList().apply { add(to.index, removeAt(from.index)) }
        onReorder(data.map { it.id })
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
    ) {
        items(data, key = { it.id }) { verse ->
            ReorderableItem(reorderState, key = verse.id) { isDragging ->
                val isSingle = selectionIsSingle && verse.id == singleVerseId
                Card(
                    onClick = { onOpen(verse) },
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 1.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.draggableHandle().padding(12.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${verse.reference}  ·  ${verse.translation}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                            )
                            if (!compact) {
                                Text(
                                    verse.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                )
                            }
                        }
                        if (selectionIsSingle) {
                            IconButton(onClick = { onSetSingle(verse.id) }, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    if (isSingle) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Use as single verse",
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                        Switch(checked = verse.active, onCheckedChange = { onToggleActive(verse.id, it) })
                        IconButton(onClick = { onEdit(verse) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { onDelete(verse) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetSelectorRow(
    sets: List<VerseSet>,
    currentSet: VerseSet?,
    sortOrder: SortOrder,
    compact: Boolean,
    onSelectSet: (Long) -> Unit,
    onNewSet: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onSortOrder: (SortOrder) -> Unit,
    onManual: () -> Unit,
    onToggleCompact: () -> Unit,
) {
    var setMenu by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    val label = currentSet?.name ?: "All verses"

    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            OutlinedButton(onClick = { setMenu = true }) {
                Text(label, maxLines = 1)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose set")
            }
            DropdownMenu(expanded = setMenu, onDismissRequest = { setMenu = false }) {
                DropdownMenuItem(text = { Text("All verses") }, onClick = { onSelectSet(VerseSet.ALL); setMenu = false })
                sets.forEach { s ->
                    DropdownMenuItem(text = { Text(s.name) }, onClick = { onSelectSet(s.id); setMenu = false })
                }
                Divider()
                DropdownMenuItem(text = { Text("＋ New set…") }, onClick = { onNewSet(); setMenu = false })
            }
        }
        Box {
            IconButton(onClick = { overflow = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Sort and view options")
            }
            DropdownMenu(expanded = overflow, onDismissRequest = { overflow = false }) {
                DropdownMenuItem(
                    text = { Text("Biblical order") },
                    leadingIcon = { if (sortOrder == SortOrder.BIBLICAL) Icon(Icons.Filled.Check, null) },
                    onClick = { onSortOrder(SortOrder.BIBLICAL); overflow = false },
                )
                DropdownMenuItem(
                    text = { Text("Recently added") },
                    leadingIcon = { if (sortOrder == SortOrder.RECENT) Icon(Icons.Filled.Check, null) },
                    onClick = { onSortOrder(SortOrder.RECENT); overflow = false },
                )
                DropdownMenuItem(
                    text = { Text("Manual order") },
                    leadingIcon = { if (sortOrder == SortOrder.MANUAL) Icon(Icons.Filled.Check, null) },
                    onClick = { onManual(); overflow = false },
                )
                Divider()
                DropdownMenuItem(
                    text = { Text("Compact view") },
                    leadingIcon = { if (compact) Icon(Icons.Filled.Check, null) },
                    onClick = { onToggleCompact(); overflow = false },
                )
                if (currentSet != null) {
                    Divider()
                    DropdownMenuItem(text = { Text("Rename set") }, onClick = { onRename(); overflow = false })
                    if (sets.size > 1) {
                        DropdownMenuItem(text = { Text("Delete set") }, onClick = { onDelete(); overflow = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactVerseRow(
    verse: Verse,
    isSingle: Boolean,
    showStar: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetSingle: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(onClick = onOpen) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${verse.reference}  ·  ${verse.translation}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (showStar) {
                IconButton(onClick = onSetSingle, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (isSingle) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Use as single verse",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Switch(checked = verse.active, onCheckedChange = onToggleActive)
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
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
    onOpen: () -> Unit,
) {
    Card(onClick = onOpen) {
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
                Switch(checked = verse.active, onCheckedChange = onToggleActive, modifier = Modifier.padding(start = 8.dp))
                Box(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        }
    }
}

@Composable
private fun VerseViewer(
    verses: List<Verse>,
    startIndex: Int,
    onClose: () -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onEdit: (Verse) -> Unit,
    onDelete: (Verse) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (verses.size - 1).coerceAtLeast(0)),
        pageCount = { verses.size },
    )
    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val v = verses[page]
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        v.reference.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        v.text,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        v.translation,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val current = pagerState.currentPage
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(enabled = current > 0, onClick = { scope.launch { pagerState.animateScrollToPage(current - 1) } }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous")
                }
                Text("${current + 1} of ${verses.size}", style = MaterialTheme.typography.titleSmall)
                IconButton(enabled = current < verses.size - 1, onClick = { scope.launch { pagerState.animateScrollToPage(current + 1) } }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next")
                }
            }

            verses.getOrNull(current)?.let { v ->
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (v.active) "In rotation" else "Paused",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Switch(checked = v.active, onCheckedChange = { onToggleActive(v.id, it) }, modifier = Modifier.padding(start = 8.dp))
                    }
                    IconButton(onClick = { onEdit(v) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { onDelete(v) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
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
                    value = reference, onValueChange = { reference = it },
                    label = { Text("Reference (e.g. John 3:16)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Verse text") }, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = translation, onValueChange = { translation = it },
                    label = { Text("Translation (e.g. KJV, ESV)") }, singleLine = true,
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

@Composable
private fun SetNameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Set name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
