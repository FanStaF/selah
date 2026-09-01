package com.fanstaf.selah.data

/** Standard Protestant 66-book ordering (Genesis=1 … Revelation=66), matching the Beblia XML. */
object BookNames {
    private val names = arrayOf(
        "", // 1-based
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
        "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
        "Nehemiah", "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
        "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
        "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah",
        "Malachi", "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
        "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians",
        "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
        "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation",
    )

    private val abbrevs = arrayOf(
        "",
        "Gen", "Exod", "Lev", "Num", "Deut", "Josh", "Judg", "Ruth", "1 Sam", "2 Sam",
        "1 Kings", "2 Kings", "1 Chron", "2 Chron", "Ezra", "Neh", "Esther", "Job", "Ps", "Prov",
        "Eccles", "Song", "Isa", "Jer", "Lam", "Ezek", "Dan", "Hos", "Joel", "Amos", "Obad",
        "Jonah", "Micah", "Nah", "Hab", "Zeph", "Haggai", "Zech", "Mal", "Matt", "Mark", "Luke",
        "John", "Acts", "Rom", "1 Cor", "2 Cor", "Gal", "Eph", "Phil", "Col", "1 Thess", "2 Thess",
        "1 Tim", "2 Tim", "Titus", "Philem", "Heb", "James", "1 Pet", "2 Pet", "1 John", "2 John",
        "3 John", "Jude", "Rev",
    )

    fun name(bookNumber: Int): String = names.getOrElse(bookNumber) { "Book $bookNumber" }

    fun abbrev(bookNumber: Int): String = abbrevs.getOrElse(bookNumber) { "$bookNumber" }

    /** "Colossians 2:8" */
    fun reference(bookNumber: Int, chapter: Int, verse: Int): String =
        "${name(bookNumber)} $chapter:$verse"

    fun isNewTestament(bookNumber: Int): Boolean = bookNumber >= 40

    private fun norm(s: String): String = s.lowercase().trim().replace(Regex("\\s+"), " ")

    private val lookup: Map<String, Int> by lazy {
        buildMap {
            for (i in 1..66) {
                put(norm(names[i]), i)
                put(norm(abbrevs[i]), i)
            }
            // Common variants.
            put("psalm", 19)
            put("song of songs", 22)
            put("canticles", 22)
            put("phil", 50) // disambiguates toward Philippians (Philemon stays "philem")
        }
    }

    private val refRegex = Regex("^(.*?)\\s+(\\d+):(\\d+)")

    /**
     * Parse a reference like "John 3:16", "1 Corinthians 13:4", or "Colossians 2:8-10" into
     * (bookNumber, chapter, startVerse). Returns null if the book isn't recognized.
     */
    fun parse(reference: String): Triple<Int, Int, Int>? {
        val m = refRegex.find(reference.trim()) ?: return null
        val book = lookup[norm(m.groupValues[1])] ?: return null
        val chapter = m.groupValues[2].toIntOrNull() ?: return null
        val verse = m.groupValues[3].toIntOrNull() ?: return null
        return Triple(book, chapter, verse)
    }
}
