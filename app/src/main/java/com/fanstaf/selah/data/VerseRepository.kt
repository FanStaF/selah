package com.fanstaf.selah.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class VerseRepository(
    private val dao: VerseDao,
    private val appContext: Context,
) {
    fun observeAll(): Flow<List<Verse>> = dao.observeAll()

    /** On first launch, seed the bundled starter verses. */
    suspend fun ensureSeeded() {
        if (dao.count() == 0) dao.insertAll(BundledVerses.load(appContext))
    }

    suspend fun activeVerses(): List<Verse> = dao.activeVerses()

    suspend fun byId(id: Long): Verse? = dao.byId(id)

    suspend fun addUserVerse(reference: String, text: String, translation: String): Long =
        dao.insert(
            Verse(
                reference = reference.trim(),
                text = text.trim(),
                translation = translation.trim().ifEmpty { "—" },
                source = Verse.SOURCE_USER,
                active = true,
                orderIndex = Int.MAX_VALUE, // new user verses sort last
            ),
        )

    suspend fun update(verse: Verse) = dao.update(verse)
    suspend fun delete(verse: Verse) = dao.delete(verse)
    suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)
    suspend fun markShown(id: Long, ts: Long) = dao.markShown(id, ts)
}
