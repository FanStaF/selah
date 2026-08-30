package com.fanstaf.selah.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Verse::class, CorpusTranslation::class, CorpusVerse::class, VerseSet::class],
    version = 3,
    exportSchema = false,
)
abstract class VerseDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao
    abstract fun corpusDao(): CorpusDao
    abstract fun setDao(): SetDao

    companion object {
        @Volatile private var instance: VerseDatabase? = null

        // v1 -> v2: add corpus coordinate columns to the study table and create the corpus tables.
        // Additive and non-destructive, so existing study verses are preserved.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `verses` ADD COLUMN `bookNumber` INTEGER")
                db.execSQL("ALTER TABLE `verses` ADD COLUMN `chapter` INTEGER")
                db.execSQL("ALTER TABLE `verses` ADD COLUMN `verse` INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `corpus_translation` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`code` TEXT NOT NULL, `name` TEXT NOT NULL, `verseCount` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_corpus_translation_code` " +
                        "ON `corpus_translation` (`code`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `corpus_verse` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`translationCode` TEXT NOT NULL, `bookNumber` INTEGER NOT NULL, " +
                        "`chapter` INTEGER NOT NULL, `verse` INTEGER NOT NULL, `text` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_corpus_verse_translationCode_bookNumber_chapter` " +
                        "ON `corpus_verse` (`translationCode`, `bookNumber`, `chapter`)",
                )
            }
        }

        // v2 -> v3: add the verse_sets table + a setId on study verses, seed a default "My Verses"
        // set, and move existing verses into it.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `verse_sets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL)",
                )
                db.execSQL("INSERT INTO `verse_sets` (`name`, `orderIndex`) VALUES ('My Verses', 0)")
                db.execSQL("ALTER TABLE `verses` ADD COLUMN `setId` INTEGER")
                db.execSQL(
                    "UPDATE `verses` SET `setId` = " +
                        "(SELECT `id` FROM `verse_sets` ORDER BY `orderIndex`, `id` LIMIT 1)",
                )
            }
        }

        fun get(context: Context): VerseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VerseDatabase::class.java,
                    "selah.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
