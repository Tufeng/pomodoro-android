package com.pomodoro.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 历史记录实体（Room 数据库）
 */
@Entity(tableName = "pomodoro_records")
data class PomodoroRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val completedAt: Long = System.currentTimeMillis(), // 完成时间戳
    val focusDurationMinutes: Int,                       // 本次专注时长
    val date: String                                     // 日期字符串 yyyy-MM-dd
)
