package com.fanstaf.selah

import android.content.Context
import com.fanstaf.selah.data.SettingsStore
import com.fanstaf.selah.data.VerseDatabase
import com.fanstaf.selah.data.VerseRepository

/**
 * Tiny manual DI container. The graph is small enough that Hilt would be overhead; both the
 * Activity and the foreground service reach shared singletons through here.
 */
object AppGraph {
    @Volatile private var initialized = false

    lateinit var repository: VerseRepository
        private set
    lateinit var settings: SettingsStore
        private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext
            repository = VerseRepository(VerseDatabase.get(app).verseDao(), app)
            settings = SettingsStore(app)
            initialized = true
        }
    }
}
