package com.fanstaf.selah.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseDao {
    @Query("SELECT * FROM verses ORDER BY orderIndex, id")
    fun observeAll(): Flow<List<Verse>>

    @Query("SELECT * FROM verses WHERE active = 1 ORDER BY orderIndex, id")
    suspend fun activeVerses(): List<Verse>

    /** Active verses within a set, or all active verses when [setId] is VerseSet.ALL (-1). */
    @Query("SELECT * FROM verses WHERE active = 1 AND (:setId < 0 OR setId = :setId) ORDER BY orderIndex, id")
    suspend fun activeVersesScoped(setId: Long): List<Verse>

    @Query("UPDATE verses SET setId = :to WHERE setId = :from")
    suspend fun reassignSet(from: Long, to: Long)

    @Query("UPDATE verses SET translation = :newCode WHERE translation = :oldCode")
    suspend fun recodeStudy(oldCode: String, newCode: String)

    @Query("SELECT * FROM verses WHERE bookNumber IS NULL")
    suspend fun versesWithoutCoords(): List<Verse>

    @Query("SELECT COUNT(*) FROM verses")
    suspend fun count(): Int

    @Query("SELECT * FROM verses WHERE id = :id")
    suspend fun byId(id: Long): Verse?

    @Query("SELECT COUNT(*) FROM verses WHERE translation = :translation AND bookNumber = :book AND chapter = :chapter AND verse = :verse")
    suspend fun countMatching(translation: String, book: Int, chapter: Int, verse: Int): Int

    @Insert
    suspend fun insert(verse: Verse): Long

    @Insert
    suspend fun insertAll(verses: List<Verse>)

    @Update
    suspend fun update(verse: Verse)

    @Delete
    suspend fun delete(verse: Verse)

    @Query("UPDATE verses SET exposures = exposures + 1, lastShownAt = :ts WHERE id = :id")
    suspend fun markShown(id: Long, ts: Long)

    @Query("UPDATE verses SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}
