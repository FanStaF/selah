package com.fanstaf.selah.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Query("SELECT * FROM verse_sets ORDER BY orderIndex, id")
    fun observeSets(): Flow<List<VerseSet>>

    @Query("SELECT * FROM verse_sets ORDER BY orderIndex, id")
    suspend fun allSets(): List<VerseSet>

    @Query("SELECT COUNT(*) FROM verse_sets")
    suspend fun count(): Int

    @Query("SELECT id FROM verse_sets ORDER BY orderIndex, id LIMIT 1")
    suspend fun firstSetId(): Long?

    @Insert
    suspend fun insert(set: VerseSet): Long

    @Update
    suspend fun update(set: VerseSet)

    @Delete
    suspend fun delete(set: VerseSet)
}
