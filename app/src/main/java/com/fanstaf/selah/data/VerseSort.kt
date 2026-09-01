package com.fanstaf.selah.data

/** How the Verses list is ordered — which also drives the sequential after-unlock rotation. */
enum class SortOrder { BIBLICAL, RECENT, MANUAL }

/**
 * Sort a verse list. Biblical = canonical book/chapter/verse (verses without coordinates fall to
 * the end, ordered by reference); Recent = most recently added first; Manual = the user's saved
 * arrangement (orderIndex).
 */
fun sortVerses(verses: List<Verse>, order: SortOrder): List<Verse> = when (order) {
    SortOrder.BIBLICAL -> verses.sortedWith(
        compareBy(
            { it.bookNumber ?: Int.MAX_VALUE },
            { it.chapter ?: Int.MAX_VALUE },
            { it.verse ?: Int.MAX_VALUE },
            { it.reference },
            { it.id },
        ),
    )
    SortOrder.RECENT -> verses.sortedWith(
        compareByDescending<Verse> { it.createdAt }.thenByDescending { it.id },
    )
    SortOrder.MANUAL -> verses.sortedWith(compareBy({ it.orderIndex }, { it.id }))
}
