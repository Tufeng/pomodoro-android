package com.flowcycle.app.data.repository

import android.content.Context
import androidx.room.*
import com.flowcycle.app.data.model.FlowCycleRecord

@Database(entities = [FlowCycleRecord::class], version = 1, exportSchema = true)
abstract class FlowCycleDatabase : RoomDatabase() {
    abstract fun recordDao(): FlowCycleRecordDao

    companion object {
        @Volatile private var INSTANCE: FlowCycleDatabase? = null

        fun getInstance(context: Context): FlowCycleDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FlowCycleDatabase::class.java,
                    "flowcycle_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
