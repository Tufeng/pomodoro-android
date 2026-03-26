package com.pomodoro.app.data.repository

import androidx.room.*
import com.pomodoro.app.data.model.PomodoroRecord
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO：番茄钟历史记录操作
 */
@Dao
interface PomodoroRecordDao {

    @Insert
    suspend fun insert(record: PomodoroRecord)

    @Query("SELECT COUNT(*) FROM pomodoro_records WHERE date = :date")
    fun getTodayCount(date: String): Flow<Int>

    @Query("SELECT * FROM pomodoro_records ORDER BY completedAt DESC LIMIT 100")
    fun getRecentRecords(): Flow<List<PomodoroRecord>>

    @Query("DELETE FROM pomodoro_records")
    suspend fun clearAll()
}
