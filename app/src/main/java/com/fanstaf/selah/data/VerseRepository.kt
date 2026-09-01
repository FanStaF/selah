package com.fanstaf.selah.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class VerseRepository(
    private val dao: VerseDao,
    private val setDao: SetDao,
    private val appContext: Context,
) {
    fun observeAll(): Flow<List<Verse>> = dao.observeAll()
    fun observeSets(): Flow<List<VerseSet>> = setDao.observeSets()

    /** Ensure a default set exists and return its id. */
    suspend fun ensureDefaultSet(): Long {
        setDao.firstSetId()?.let { return it }
        return setDao.insert(VerseSet(name = "My Verses", orderIndex = 0))
    }

    /** On first launch, seed the bundled starter verses into the default set. */
    suspend fun ensureSeeded() {
        val defaultSet = ensureDefaultSet()
        if (dao.count() == 0) {
            dao.insertAll(BundledVerses.load(appContext).map { it.copy(setId = defaultSet) })
        }
    }

    suspend fun activeVersesScoped(setId: Long): List<Verse> = dao.activeVersesScoped(setId)

    suspend fun byId(id: Long): Verse? = dao.byId(id)

    suspend fun addUserVerse(reference: String, text: String, translation: String, setId: Long): Long {
        val ref = reference.trim()
        val coords = BookNames.parse(ref)
        return dao.insert(
            Verse(
                reference = ref,
                text = text.trim(),
                translation = translation.trim().ifEmpty { "—" },
                source = Verse.SOURCE_USER,
                active = true,
                orderIndex = Int.MAX_VALUE,
                setId = setId,
                bookNumber = coords?.first,
                chapter = coords?.second,
                verse = coords?.third,
            ),
        )
    }

    /** Fill in book/chapter/verse for any verses that lack them but have a parseable reference. */
    suspend fun backfillCoords() {
        dao.versesWithoutCoords().forEach { v ->
            BookNames.parse(v.reference)?.let { (b, c, vv) ->
                dao.update(v.copy(bookNumber = b, chapter = c, verse = vv))
            }
        }
    }

    suspend fun update(verse: Verse) = dao.update(verse)
    suspend fun delete(verse: Verse) = dao.delete(verse)
    suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)
    suspend fun markShown(id: Long, ts: Long) = dao.markShown(id, ts)

    // --- Sets ---

    suspend fun createSet(name: String): Long {
        val order = setDao.count()
        return setDao.insert(VerseSet(name = name.trim(), orderIndex = order))
    }

    suspend fun renameSet(set: VerseSet, name: String) = setDao.update(set.copy(name = name.trim()))

    /** Delete a set, moving its verses to another remaining set (or the default). */
    suspend fun deleteSet(set: VerseSet) {
        val remaining = setDao.allSets().firstOrNull { it.id != set.id }
        val target = remaining?.id ?: ensureDefaultSet()
        if (target == set.id) return // never delete the last set
        dao.reassignSet(set.id, target)
        setDao.delete(set)
    }
}
