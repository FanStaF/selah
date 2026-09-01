package com.fanstaf.selah.data

import android.content.Context
import org.json.JSONObject

/** Loads the shipped starter verses from assets/starter_verses.json (public-domain KJV text). */
object BundledVerses {
    fun load(context: Context): List<Verse> {
        val json = context.assets.open("starter_verses.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(json).getJSONArray("verses")
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val reference = o.getString("reference")
                val coords = BookNames.parse(reference)
                add(
                    Verse(
                        reference = reference,
                        text = o.getString("text"),
                        translation = o.optString("translation", "KJV"),
                        source = Verse.SOURCE_BUNDLED,
                        active = true,
                        orderIndex = i,
                        bookNumber = coords?.first,
                        chapter = coords?.second,
                        verse = coords?.third,
                    ),
                )
            }
        }
    }
}
