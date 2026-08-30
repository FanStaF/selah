package com.fanstaf.selah.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A named set/collection of verses to work on. Every study [Verse] belongs to one set. */
@Entity(tableName = "verse_sets")
data class VerseSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val orderIndex: Int = 0,
) {
    companion object {
        /** Sentinel for "all sets" in the rotation scope and the Verses filter. */
        const val ALL: Long = -1L
    }
}
