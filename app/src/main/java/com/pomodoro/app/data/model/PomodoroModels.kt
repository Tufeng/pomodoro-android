package com.pomodoro.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 番茄钟阶段枚举
 */
enum class PomodoroPhase {
    FOCUS,       // 专注阶段
    SHORT_BREAK, // 短休息
    LONG_BREAK;  // 长休息

    fun displayName(): String = when (this) {
        FOCUS -> "专注"
        SHORT_BREAK -> "短休息"
        LONG_BREAK -> "长休息"
    }
}

/**
 * 计时器状态枚举
 */
enum class TimerState {
    IDLE,    // 空闲（未开始）
    RUNNING, // 运行中
    PAUSED   // 暂停
}

/**
 * 番茄钟配置数据类（Parcelable 供 Service Intent 传参）
 */
@Parcelize
data class PomodoroConfig(
    val focusDurationMinutes: Int = 25,       // 专注时长（分钟）
    val shortBreakMinutes: Int = 5,            // 短休时长（分钟）
    val longBreakMinutes: Int = 15,            // 长休时长（分钟）
    val longBreakInterval: Int = 4,            // 每N个番茄触发长休
    val soundEnabled: Boolean = true,          // 是否开启声音提醒
    val vibrationEnabled: Boolean = true       // 是否开启震动提醒
) : Parcelable {
    fun focusDurationSeconds(): Long = focusDurationMinutes * 60L
    fun shortBreakSeconds(): Long = shortBreakMinutes * 60L
    fun longBreakSeconds(): Long = longBreakMinutes * 60L

    fun phaseDurationSeconds(phase: PomodoroPhase): Long = when (phase) {
        PomodoroPhase.FOCUS -> focusDurationSeconds()
        PomodoroPhase.SHORT_BREAK -> shortBreakSeconds()
        PomodoroPhase.LONG_BREAK -> longBreakSeconds()
    }
}

/**
 * 计时器 UI 状态
 */
data class TimerUiState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val timerState: TimerState = TimerState.IDLE,
    val remainingSeconds: Long = 25 * 60L,
    val totalSeconds: Long = 25 * 60L,
    val completedPomodoros: Int = 0,
    val config: PomodoroConfig = PomodoroConfig()
) {
    val progress: Float
        get() = if (totalSeconds > 0) {
            1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())
        } else 0f

    val formattedTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
}
