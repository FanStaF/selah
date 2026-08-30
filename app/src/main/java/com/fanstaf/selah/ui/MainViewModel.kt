package com.fanstaf.selah.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fanstaf.selah.AppGraph
import com.fanstaf.selah.data.DisplayMode
import com.fanstaf.selah.data.SelectionStrategy
import com.fanstaf.selah.data.Settings
import com.fanstaf.selah.data.Verse
import com.fanstaf.selah.service.UnlockService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppGraph.repository
    private val store = AppGraph.settings

    val settings: StateFlow<Settings> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    val verses: StateFlow<List<Verse>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Only turns the feature on if overlay permission is already granted (checked by the Activity). */
    fun setEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            store.setEnabled(enabled)
            if (enabled) UnlockService.start(app) else UnlockService.stop(app)
        }
    }

    fun setDuration(seconds: Int) = viewModelScope.launch { store.setDuration(seconds) }
    fun setMode(mode: DisplayMode) = viewModelScope.launch { store.setMode(mode) }
    fun setSelection(s: SelectionStrategy) = viewModelScope.launch { store.setSelection(s) }
    fun setMinInterval(minutes: Int) = viewModelScope.launch { store.setMinInterval(minutes) }
    fun setSingleVerse(id: Long) = viewModelScope.launch { store.setSingleVerseId(id) }
    fun setFontScale(scale: Float) = viewModelScope.launch { store.setFontScale(scale) }

    fun addVerse(reference: String, text: String, translation: String) =
        viewModelScope.launch { repo.addUserVerse(reference, text, translation) }

    fun updateVerse(verse: Verse) = viewModelScope.launch { repo.update(verse) }
    fun deleteVerse(verse: Verse) = viewModelScope.launch { repo.delete(verse) }
    fun setActive(id: Long, active: Boolean) = viewModelScope.launch { repo.setActive(id, active) }
}
