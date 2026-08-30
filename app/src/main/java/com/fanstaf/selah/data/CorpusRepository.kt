package com.fanstaf.selah.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

class CorpusRepository(
    private val corpusDao: CorpusDao,
    private val verseDao: VerseDao,
    private val appContext: Context,
) {
    fun observeTranslations(): Flow<List<CorpusTranslation>> = corpusDao.observeTranslations()

    suspend fun hasTranslation(code: String): Boolean = corpusDao.translation(code) != null

    /** Load the bundled KJV into the corpus. */
    suspend fun importBundledKjv() {
        appContext.assets.open("kjv.xml").use { importStream(it, forcedCode = "KJV") }
    }

    /**
     * Parse a Bible XML stream and store it as a corpus translation (replacing any with the same
     * code). Returns the stored translation, or null if nothing parsed.
     */
    suspend fun importStream(input: InputStream, forcedCode: String? = null): CorpusTranslation? {
        val verses = ArrayList<CorpusVerse>(31_200)
        lateinit var codeHolder: String
        val meta = BibleXmlParser.parse(input) { book, chapter, verse, text ->
            // translationCode is filled after we know it; capture into a placeholder list first.
            verses.add(CorpusVerse(translationCode = "", bookNumber = book, chapter = chapter, verse = verse, text = text))
        }
        if (verses.isEmpty()) return null

        val code = forcedCode ?: BibleXmlParser.deriveCode(meta.translationAttr)
        val name = if (forcedCode == "KJV") "King James Version" else BibleXmlParser.deriveName(meta.translationAttr)
        codeHolder = code
        val coded = verses.map { it.copy(translationCode = codeHolder) }

        val translation = CorpusTranslation(code = code, name = name, verseCount = coded.size)
        corpusDao.replaceTranslation(translation, coded)
        return translation
    }

    /**
     * Rename a translation's short code and/or full name. Changing the code cascades to corpus
     * verses and the user's saved snapshots. Returns false if the new code collides.
     */
    suspend fun updateTranslation(old: CorpusTranslation, newCode: String, newName: String): Boolean {
        val code = newCode.trim()
        val name = newName.trim().ifEmpty { code }
        if (code.isEmpty()) return false
        if (code != old.code && corpusDao.translation(code) != null) return false
        if (code == old.code) {
            corpusDao.setName(old.code, name)
        } else {
            corpusDao.setCodeAndName(old.code, code, name)
            corpusDao.recodeVerses(old.code, code)
            verseDao.recodeStudy(old.code, code)
        }
        return true
    }

    suspend fun deleteTranslation(t: CorpusTranslation) {
        corpusDao.deleteVerses(t.code)
        corpusDao.deleteTranslation(t.code)
    }

    suspend fun books(code: String): List<Int> = corpusDao.books(code)
    suspend fun chapters(code: String, book: Int): List<Int> = corpusDao.chapters(code, book)
    suspend fun versesIn(code: String, book: Int, chapter: Int): List<CorpusVerse> =
        corpusDao.versesIn(code, book, chapter)

    /**
     * Copy a corpus verse into the user's study list (a stable snapshot). Skips if the same verse
     * in the same translation is already there. Returns true if added.
     */
    suspend fun addToStudy(cv: CorpusVerse, setId: Long): Boolean {
        val exists = verseDao.countMatching(cv.translationCode, cv.bookNumber, cv.chapter, cv.verse) > 0
        if (exists) return false
        verseDao.insert(
            Verse(
                reference = BookNames.reference(cv.bookNumber, cv.chapter, cv.verse),
                text = cv.text,
                translation = cv.translationCode,
                source = Verse.SOURCE_CORPUS,
                active = true,
                orderIndex = Int.MAX_VALUE,
                bookNumber = cv.bookNumber,
                chapter = cv.chapter,
                verse = cv.verse,
                setId = setId,
            ),
        )
        return true
    }
}
