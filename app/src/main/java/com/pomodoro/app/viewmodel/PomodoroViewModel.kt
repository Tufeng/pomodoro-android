package com.pomodoro.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.app.data.model.*
import com.pomodoro.app.data.preferences.PomodoroPreferences
import com.pomodoro.app.data.repository.PomodoroDatabase
import com.pomodoro.app.data.repository.PomodoroRecordDao
import com.pomodoro.app.service.PomodoroTimerService
import com.pomodoro.app.service.ServiceTimerState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主 ViewModel：连接 Service 状态与 UI
 */
class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = PomodoroPreferences(application)
    private val recordDao: PomodoroRecordDao =
        PomodoroDatabase.getInstance(application).recordDao()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ─── 配置流 ───────────────────────────────────────────────────
    val config: StateFlow<PomodoroConfig> = preferences.configFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PomodoroConfig())

    // ─── 计时器 UI 状态（来自 Service，并融合最新 Preferences config）──
    // 当计时器处于 IDLE 时，用 Preferences 里最新的 config 覆盖显示
    // 这样保存设置后返回计时器页面能立即看到新的时长
    val timerUiState: StateFlow<TimerUiState> = combine(
        PomodoroTimerService.timerState,
        preferences.configFlow
    ) { serviceState, latestConfig ->
        val effectiveConfig = if (serviceState.timerState == TimerState.IDLE) {
            latestConfig
        } else {
            serviceState.config
        }
        TimerUiState(
            phase = serviceState.phase,
            timerState = serviceState.timerState,
            remainingSeconds = if (serviceState.timerState == TimerState.IDLE) {
                effectiveConfig.phaseDurationSeconds(serviceState.phase)
            } else {
                serviceState.remainingSeconds
            },
            totalSeconds = effectiveConfig.phaseDurationSeconds(serviceState.phase),
            completedPomodoros = serviceState.completedPomodoros,
            config = effectiveConfig
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            TimerUiState(config = config.value)
        )

    // ─── 今日完成数 ───────────────────────────────────────────────
    val todayCount: StateFlow<Int> = recordDao
        .getTodayCount(dateFormatter.format(Date()))
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ─── 监听 Service 完成事件，写入历史记录 ──────────────────────
    init {
        viewModelScope.launch {
            PomodoroTimerService.timerState
                .filter { it.phaseJustCompleted == PomodoroPhase.FOCUS }
                .collect { state ->
                    val record = PomodoroRecord(
                        focusDurationMinutes = state.config.focusDurationMinutes,
                        date = dateFormatter.format(Date())
                    )
                    recordDao.insert(record)
                    // 清除事件标记（防止重复触发）
                    PomodoroTimerService.timerState.update {
                        it.copy(phaseJustCompleted = null)
                    }
                }
        }
    }

    // ─── 计时器控制 ───────────────────────────────────────────────

    fun startTimer() {
        sendServiceAction(PomodoroTimerService.ACTION_START, config.value)
    }

    fun pauseTimer() {
        sendServiceAction(PomodoroTimerService.ACTION_PAUSE)
    }

    fun resumeTimer() {
        sendServiceAction(PomodoroTimerService.ACTION_RESUME)
    }

    fun skipPhase() {
        sendServiceAction(PomodoroTimerService.ACTION_SKIP)
    }

    fun resetTimer() {
        sendServiceAction(PomodoroTimerService.ACTION_RESET)
    }

    fun saveConfig(newConfig: PomodoroConfig) {
        viewModelScope.launch {
            preferences.saveConfig(newConfig)
        }
        // 如果当前空闲，更新 Service 配置
        if (timerUiState.value.timerState == TimerState.IDLE) {
            sendServiceAction(PomodoroTimerService.ACTION_UPDATE_CONFIG, newConfig)
        }
    }

    private fun sendServiceAction(action: String, configOverride: PomodoroConfig? = null) {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, PomodoroTimerService::class.java).apply {
            this.action = action
            configOverride?.let { putExtra(PomodoroTimerService.EXTRA_CONFIG, it) }
                ?: putExtra(PomodoroTimerService.EXTRA_CONFIG, config.value)
        }
        ctx.startForegroundService(intent)
    }
}
