package com.fanstaf.selah.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Genre color-coding for the book grid, in the spirit of Logos. Mid-dark tones chosen so white
 * text reads in both light and dark themes.
 */
fun bookColor(bookNumber: Int): Color = when (bookNumber) {
    in 1..5 -> Color(0xFF6E6733)     // Law (Pentateuch) — olive
    in 6..17 -> Color(0xFF2E5D5D)    // OT History — teal
    in 18..22 -> Color(0xFF6E3B2E)   // Wisdom & Poetry — rust/brown
    in 23..27 -> Color(0xFF33603C)   // Major Prophets — green
    in 28..39 -> Color(0xFF6E2E3D)   // Minor Prophets — maroon
    in 40..43 -> Color(0xFF6E6733)   // Gospels — olive
    44 -> Color(0xFF2E5D5D)          // Acts — teal
    in 45..57 -> Color(0xFF6E3B2E)   // Pauline Epistles — brown
    in 58..65 -> Color(0xFF33603C)   // General Epistles — green
    else -> Color(0xFF6E2E3D)        // Revelation — maroon
}
