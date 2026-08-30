package com.fanstaf.selah.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A bundled or imported full translation available for browsing. The corpus is reference data,
 * kept separate from the user's study list ([Verse]); picking a verse copies a snapshot into a
 * study verse.
 */
@Entity(tableName = "corpus_translation", indices = [Index(value = ["code"], unique = true)])
data class CorpusTranslation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val verseCount: Int,
)

@Entity(
    tableName = "corpus_verse",
    indices = [Index(value = ["translationCode", "bookNumber", "chapter"])],
)
data class CorpusVerse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val translationCode: String,
    val bookNumber: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
)
