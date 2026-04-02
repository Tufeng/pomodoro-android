package com.flowcycle.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 计时阶段枚举
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
 * 场景配置（每个场景独立的时长和提醒设置）
 */
@Parcelize
data class Scene(
    val id: String,                          // 唯一标识，用 UUID 或预设名
    val name: String,                        // 场景名称（显示给用户）
    val focusDurationMinutes: Int = 25,      // 专注时长（分钟）
    val shortBreakMinutes: Int = 5,          // 短休时长（分钟）
    val longBreakMinutes: Int = 15,          // 长休时长（分钟）
    val longBreakInterval: Int = 4,          // 每N个专注触发长休
    val soundEnabled: Boolean = true,        // 声音提醒
    val vibrationEnabled: Boolean = true,    // 震动提醒
    val isPreset: Boolean = false            // 是否内置预设（不可删除）
) : Parcelable {
    fun focusDurationSeconds(): Long = focusDurationMinutes * 60L
    fun shortBreakSeconds(): Long = shortBreakMinutes * 60L
    fun longBreakSeconds(): Long = longBreakMinutes * 60L

    fun phaseDurationSeconds(phase: PomodoroPhase): Long = when (phase) {
        PomodoroPhase.FOCUS -> focusDurationSeconds()
        PomodoroPhase.SHORT_BREAK -> shortBreakSeconds()
        PomodoroPhase.LONG_BREAK -> longBreakSeconds()
    }

    /** 转为旧版 PomodoroConfig 兼容格式（供 Service 使用） */
    fun toConfig(): PomodoroConfig = PomodoroConfig(
        focusDurationMinutes = focusDurationMinutes,
        shortBreakMinutes = shortBreakMinutes,
        longBreakMinutes = longBreakMinutes,
        longBreakInterval = longBreakInterval,
        soundEnabled = soundEnabled,
        vibrationEnabled = vibrationEnabled
    )
}

/**
 * 内置预设场景
 */
object PresetScenes {
    val CLASSIC = Scene(
        id = "preset_classic",
        name = "经典番茄",
        focusDurationMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        longBreakInterval = 4,
        isPreset = true
    )
    val DEEP_FOCUS = Scene(
        id = "preset_deep_focus",
        name = "深度学习",
        focusDurationMinutes = 45,
        shortBreakMinutes = 10,
        longBreakMinutes = 20,
        longBreakInterval = 3,
        isPreset = true
    )
    val FITNESS = Scene(
        id = "preset_fitness",
        name = "健身训练",
        focusDurationMinutes = 45,
        shortBreakMinutes = 10,
        longBreakMinutes = 20,
        longBreakInterval = 2,
        isPreset = true
    )

    val ALL = listOf(CLASSIC, DEEP_FOCUS, FITNESS)
}

/**
 * 番茄钟配置数据类（Parcelable 供 Service Intent 传参）
 * 保持兼容旧版
 */
@Parcelize
data class PomodoroConfig(
    val focusDurationMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakInterval: Int = 4,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
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
    val config: PomodoroConfig = PomodoroConfig(),
    val currentSceneName: String = "经典番茄"   // 当前场景名，用于计时页显示
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
