package com.flowcycle.app.data.repository

import androidx.room.*
import com.flowcycle.app.data.model.FlowCycleRecord
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO：节律历史记录操作
 */
@Dao
interface FlowCycleRecordDao {

    @Insert
    suspend fun insert(record: FlowCycleRecord)

    @Query("SELECT COUNT(*) FROM flowcycle_records WHERE date = :date")
    fun getTodayCount(date: String): Flow<Int>

    @Query("SELECT * FROM flowcycle_records ORDER BY completedAt DESC LIMIT 100")
    fun getRecentRecords(): Flow<List<FlowCycleRecord>>

    @Query("DELETE FROM flowcycle_records")
    suspend fun clearAll()
}
