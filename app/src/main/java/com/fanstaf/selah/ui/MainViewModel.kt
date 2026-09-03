package com.fanstaf.selah.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fanstaf.selah.AppGraph
import com.fanstaf.selah.data.CorpusTranslation
import com.fanstaf.selah.data.CorpusVerse
import com.fanstaf.selah.data.DisplayStyle
import com.fanstaf.selah.data.SelectionStrategy
import com.fanstaf.selah.data.BookNames
import com.fanstaf.selah.data.Settings
import com.fanstaf.selah.data.SortOrder
import com.fanstaf.selah.data.Verse
import com.fanstaf.selah.data.VerseSet
import com.fanstaf.selah.service.UnlockService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppGraph.repository
    private val corpus = AppGraph.corpus
    private val store = AppGraph.settings

    val translations: StateFlow<List<CorpusTranslation>> = corpus.observeTranslations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Transient user-facing message (verse added, import result). */
    val message = MutableStateFlow<String?>(null)
    fun clearMessage() { message.value = null }

    /** Programmatic tab navigation: 0=Home, 1=Verses, 2=Browse; null=none. */
    val navTarget = MutableStateFlow<Int?>(null)
    fun consumeNav() { navTarget.value = null }

    /** True while the user is picking a single verse from the Bible (in the Browse tab). */
    val pickSingleFromBible = MutableStateFlow(false)
    fun startSingleFromBible() {
        pickSingleFromBible.value = true
        navTarget.value = 2
    }

    /** The translation currently being edited (import just finished, or long-pressed). */
    val editingTranslation = MutableStateFlow<CorpusTranslation?>(null)
    fun openTranslationEditor(t: CorpusTranslation) { editingTranslation.value = t }
    fun closeTranslationEditor() { editingTranslation.value = null }

    fun saveTranslation(old: CorpusTranslation, code: String, name: String) = viewModelScope.launch {
        val ok = corpus.updateTranslation(old, code, name)
        if (ok) {
            message.value = "Saved ${code.trim()}"
            editingTranslation.value = null
        } else {
            message.value = "That code is already in use"
        }
    }

    fun deleteTranslation(t: CorpusTranslation) = viewModelScope.launch {
        corpus.deleteTranslation(t)
        message.value = "Removed ${t.code}"
        editingTranslation.value = null
    }

    val settings: StateFlow<Settings> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    val verses: StateFlow<List<Verse>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sets: StateFlow<List<VerseSet>> = repo.observeSets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The set currently in focus on the Verses screen (also the default target when adding). */
    val selectedSetId = MutableStateFlow(VerseSet.ALL)
    fun selectSet(id: Long) { selectedSetId.value = id }

    /** Only turns the feature on if overlay permission is already granted (checked by the Activity). */
    fun setEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            store.setEnabled(enabled)
            if (enabled) UnlockService.start(app) else UnlockService.stop(app)
        }
    }

    fun setDuration(seconds: Int) = viewModelScope.launch { store.setDuration(seconds) }
    fun setSelection(s: SelectionStrategy) = viewModelScope.launch { store.setSelection(s) }
    fun setMinInterval(minutes: Int) = viewModelScope.launch { store.setMinInterval(minutes) }
    fun setSingleVerse(id: Long) = viewModelScope.launch { store.setSingleVerseId(id) }

    /** Pick an existing verse as the single verse. */
    fun chooseSingleExisting(id: Long) = viewModelScope.launch {
        store.setSingleVerseId(id)
        store.setSelection(SelectionStrategy.SINGLE)
    }

    fun setOrderRandom(random: Boolean) = viewModelScope.launch {
        store.setSelection(if (random) SelectionStrategy.RANDOM else SelectionStrategy.SEQUENTIAL)
    }
    fun setSingleSource() = viewModelScope.launch { store.setSelection(SelectionStrategy.SINGLE) }
    fun setFontScale(scale: Float) = viewModelScope.launch { store.setFontScale(scale) }
    fun setDisplayStyle(style: DisplayStyle) = viewModelScope.launch { store.setDisplayStyle(style) }
    fun setScopeSetId(id: Long) = viewModelScope.launch { store.setScopeSetId(id) }

    /** Add a typed verse to a set (or to the default set when [setId] is ALL/unset). */
    fun addVerse(reference: String, text: String, translation: String, setId: Long) =
        viewModelScope.launch {
            val target = if (setId >= 0) setId else repo.ensureDefaultSet()
            repo.addUserVerse(reference, text, translation, target)
        }

    /** Choose the source for the rotation: a set (or All), leaving single-verse mode if it was on. */
    fun selectSourceSet(scopeSetId: Long) = viewModelScope.launch {
        store.setScopeSetId(scopeSetId)
        if (settings.value.selection == SelectionStrategy.SINGLE) {
            store.setSelection(SelectionStrategy.SEQUENTIAL)
        }
    }

    /** Type a new verse and make it the single verse. */
    fun addVerseAsSingle(reference: String, text: String, translation: String) = viewModelScope.launch {
        val target = repo.ensureDefaultSet()
        val id = repo.addUserVerse(reference, text, translation, target)
        store.setSingleVerseId(id)
        store.setSelection(SelectionStrategy.SINGLE)
    }

    fun updateVerse(verse: Verse) = viewModelScope.launch {
        // Re-derive coordinates from the (possibly edited) reference so Biblical sort stays correct.
        val coords = BookNames.parse(verse.reference)
        repo.update(verse.copy(bookNumber = coords?.first, chapter = coords?.second, verse = coords?.third))
    }
    fun deleteVerse(verse: Verse) = viewModelScope.launch { repo.delete(verse) }
    fun setActive(id: Long, active: Boolean) = viewModelScope.launch { repo.setActive(id, active) }

    fun setSortOrder(order: SortOrder) = viewModelScope.launch { store.setSortOrder(order) }
    fun setVersesCompact(compact: Boolean) = viewModelScope.launch { store.setVersesCompact(compact) }

    /** Switch to manual sort, seeding the arrangement from the currently shown order. */
    fun enableManual(orderedIds: List<Long>) = viewModelScope.launch {
        repo.setManualOrder(orderedIds)
        store.setSortOrder(SortOrder.MANUAL)
    }
    fun setManualOrder(orderedIds: List<Long>) = viewModelScope.launch { repo.setManualOrder(orderedIds) }

    // --- Sets ---

    fun createSet(name: String, onCreated: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repo.createSet(name)
        onCreated(id)
    }
    fun renameSet(set: VerseSet, name: String) = viewModelScope.launch { repo.renameSet(set, name) }
    fun deleteSet(set: VerseSet) = viewModelScope.launch { repo.deleteSet(set) }

    // --- Corpus browse + import ---

    suspend fun books(code: String): List<Int> = corpus.books(code)
    suspend fun chapters(code: String, book: Int): List<Int> = corpus.chapters(code, book)
    suspend fun versesIn(code: String, book: Int, chapter: Int): List<CorpusVerse> =
        corpus.versesIn(code, book, chapter)

    fun setBrowseTranslation(code: String) = viewModelScope.launch { store.setBrowseTranslation(code) }

    /**
     * Add one or more contiguous corpus verses as a single study entry. When [asSingle] is set (the
     * "pick single verse from the Bible" flow) the entry also becomes the single verse and we
     * navigate back Home.
     */
    fun addRangeToStudy(verses: List<CorpusVerse>, setId: Long, asSingle: Boolean = false) = viewModelScope.launch {
        if (verses.isEmpty()) return@launch
        val target = if (setId >= 0) setId else repo.ensureDefaultSet()
        val result = corpus.addRangeToStudy(verses, target)
        val sorted = verses.sortedBy { it.verse }
        val first = sorted.first()
        val last = sorted.last()
        val ref = if (first.verse == last.verse) {
            BookNames.reference(first.bookNumber, first.chapter, first.verse)
        } else {
            "${BookNames.name(first.bookNumber)} ${first.chapter}:${first.verse}-${last.verse}"
        }
        if (asSingle) {
            store.setSingleVerseId(result.id)
            store.setSelection(SelectionStrategy.SINGLE)
            pickSingleFromBible.value = false
            navTarget.value = 0
            message.value = "Single verse: $ref"
        } else {
            message.value = if (result.added) "Added $ref" else "Already in your verses"
        }
    }

    fun importFromUri(uri: Uri) = viewModelScope.launch {
        message.value = "Importing…"
        val result = withContext(Dispatchers.IO) {
            runCatching {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    corpus.importStream(it)
                }
            }.getOrNull()
        }
        if (result != null) {
            message.value = "Imported ${result.name} (${result.verseCount} verses)"
            // Open the editor so the name/code can be adjusted right after import.
            editingTranslation.value = result
        } else {
            message.value = "Import failed — is it a Bible XML file?"
        }
    }
}
