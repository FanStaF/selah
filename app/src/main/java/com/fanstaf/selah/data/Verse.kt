package com.fanstaf.selah.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One memorizable verse. For v0.1 there is a single implicit collection: every verse with
 * [active] == true is in the rotation shown after unlock. Multiple named collections + a proper
 * spaced-repetition [MemoryState] split are planned (see BIBLE-MEMORY-PLAN.md); until then the light
 * memory fields live here on the row.
 */
@Entity(tableName = "verses")
data class Verse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val text: String,
    val translation: String,
    /** "BUNDLED" (shipped starter set) or "USER" (typed by the user). */
    val source: String,
    /** In the after-unlock rotation. */
    val active: Boolean = true,
    val exposures: Int = 0,
    val lastShownAt: Long = 0L,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val SOURCE_BUNDLED = "BUNDLED"
        const val SOURCE_USER = "USER"
    }
}
