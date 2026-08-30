package com.fanstaf.selah.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CorpusDao {
    @Query("SELECT * FROM corpus_translation ORDER BY name")
    fun observeTranslations(): Flow<List<CorpusTranslation>>

    @Query("SELECT * FROM corpus_translation WHERE code = :code LIMIT 1")
    suspend fun translation(code: String): CorpusTranslation?

    @Insert
    suspend fun insertTranslation(t: CorpusTranslation): Long

    @Insert
    suspend fun insertVerses(verses: List<CorpusVerse>)

    @Query("DELETE FROM corpus_verse WHERE translationCode = :code")
    suspend fun deleteVerses(code: String)

    @Query("DELETE FROM corpus_translation WHERE code = :code")
    suspend fun deleteTranslation(code: String)

    @Query("UPDATE corpus_translation SET name = :name WHERE code = :code")
    suspend fun setName(code: String, name: String)

    @Query("UPDATE corpus_translation SET code = :newCode, name = :name WHERE code = :oldCode")
    suspend fun setCodeAndName(oldCode: String, newCode: String, name: String)

    @Query("UPDATE corpus_verse SET translationCode = :newCode WHERE translationCode = :oldCode")
    suspend fun recodeVerses(oldCode: String, newCode: String)

    @Query("SELECT DISTINCT bookNumber FROM corpus_verse WHERE translationCode = :code ORDER BY bookNumber")
    suspend fun books(code: String): List<Int>

    @Query("SELECT DISTINCT chapter FROM corpus_verse WHERE translationCode = :code AND bookNumber = :book ORDER BY chapter")
    suspend fun chapters(code: String, book: Int): List<Int>

    @Query("SELECT * FROM corpus_verse WHERE translationCode = :code AND bookNumber = :book AND chapter = :chapter ORDER BY verse")
    suspend fun versesIn(code: String, book: Int, chapter: Int): List<CorpusVerse>

    @Transaction
    suspend fun replaceTranslation(t: CorpusTranslation, verses: List<CorpusVerse>) {
        deleteVerses(t.code)
        deleteTranslation(t.code)
        insertTranslation(t)
        // Insert in chunks to keep individual statements small.
        verses.chunked(2000).forEach { insertVerses(it) }
    }
}
