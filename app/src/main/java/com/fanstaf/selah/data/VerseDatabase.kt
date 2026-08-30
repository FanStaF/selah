package com.fanstaf.selah.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Verse::class], version = 1, exportSchema = false)
abstract class VerseDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao

    companion object {
        @Volatile private var instance: VerseDatabase? = null

        fun get(context: Context): VerseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VerseDatabase::class.java,
                    "selah.db",
                ).build().also { instance = it }
            }
    }
}
