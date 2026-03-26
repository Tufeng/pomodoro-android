package com.pomodoro.app.data.repository

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.pomodoro.app.data.model.PomodoroRecord

@Database(entities = [PomodoroRecord::class], version = 1, exportSchema = true)
abstract class PomodoroDatabase : RoomDatabase() {
    abstract fun recordDao(): PomodoroRecordDao

    companion object {
        @Volatile private var INSTANCE: PomodoroDatabase? = null

        fun getInstance(context: Context): PomodoroDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PomodoroDatabase::class.java,
                    "pomodoro_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
