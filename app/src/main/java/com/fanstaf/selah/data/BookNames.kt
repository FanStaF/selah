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

    fun name(bookNumber: Int): String = names.getOrElse(bookNumber) { "Book $bookNumber" }

    /** "Colossians 2:8" */
    fun reference(bookNumber: Int, chapter: Int, verse: Int): String =
        "${name(bookNumber)} $chapter:$verse"

    fun isNewTestament(bookNumber: Int): Boolean = bookNumber >= 40
}
