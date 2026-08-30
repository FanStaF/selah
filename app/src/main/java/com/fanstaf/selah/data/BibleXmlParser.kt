package com.fanstaf.selah.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Parses the Beblia "Holy-Bible-XML-Format":
 *
 *   <bible translation="English KJV" status="Public Domain">
 *     <testament name="Old">
 *       <book number="1">
 *         <chapter number="1">
 *           <verse number="1">In the beginning...</verse>
 *
 * Books are numbered 1..66 (Protestant order), so the testament wrapper is informational only.
 * Streams verses to [onVerse] to avoid holding the whole document in memory twice.
 */
object BibleXmlParser {

    data class Meta(val translationAttr: String, val verseCount: Int)

    fun parse(
        input: InputStream,
        onVerse: (bookNumber: Int, chapter: Int, verse: Int, text: String) -> Unit,
    ): Meta {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var translationAttr = ""
        var book = 0
        var chapter = 0
        var verse = 0
        var count = 0
        val sb = StringBuilder()
        var inVerse = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "bible" -> translationAttr = parser.getAttributeValue(null, "translation") ?: ""
                    "book" -> book = parser.getAttributeValue(null, "number")?.toIntOrNull() ?: book
                    "chapter" -> chapter = parser.getAttributeValue(null, "number")?.toIntOrNull() ?: chapter
                    "verse" -> {
                        verse = parser.getAttributeValue(null, "number")?.toIntOrNull() ?: 0
                        sb.setLength(0)
                        inVerse = true
                    }
                }
                XmlPullParser.TEXT -> if (inVerse) sb.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.name == "verse") {
                    inVerse = false
                    val text = sb.toString().trim()
                    if (book > 0 && chapter > 0 && verse > 0 && text.isNotEmpty()) {
                        onVerse(book, chapter, verse, text)
                        count++
                    }
                }
            }
            event = parser.next()
        }
        return Meta(translationAttr = translationAttr.trim(), verseCount = count)
    }

    /**
     * Derives a short display code from the translation attribute — an embedded uppercase
     * abbreviation like "ESV"/"KJV" if present, else a cleaned slug.
     * e.g. "English ESV 2016 == ..." -> "ESV"; "English KJV" -> "KJV".
     */
    fun deriveCode(translationAttr: String): String {
        val head = translationAttr.substringBefore("==").trim()
        Regex("\\b[A-Z]{2,6}\\b").find(head)?.let { return it.value }
        val slug = head.filter { it.isLetterOrDigit() }.uppercase()
        return if (slug.isEmpty()) "BIBLE" else slug.take(8)
    }

    /** Human-readable name — the part before " == " (the license blurb), trimmed. */
    fun deriveName(translationAttr: String): String =
        translationAttr.substringBefore("==").trim().ifEmpty { "Imported Bible" }
}
