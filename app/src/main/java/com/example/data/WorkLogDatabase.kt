package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkLog::class, ScannedText::class], version = 7, exportSchema = false)
abstract class WorkLogDatabase : RoomDatabase() {
    abstract fun workLogDao(): WorkLogDao
    abstract fun scannedTextDao(): ScannedTextDao

    fun vacuum() {
        try {
            val db = this.openHelper.writableDatabase
            db.execSQL("PRAGMA wal_checkpoint(FULL)")
            db.execSQL("VACUUM")
            db.execSQL("PRAGMA optimize")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: WorkLogDatabase? = null

        fun getDatabase(context: Context): WorkLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkLogDatabase::class.java,
                    "work_log_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
