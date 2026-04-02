package com.flowcycle.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowcycle.app.data.model.*
import com.flowcycle.app.data.preferences.FlowCyclePreferences
import com.flowcycle.app.data.repository.FlowCycleDatabase
import com.flowcycle.app.data.repository.FlowCycleRecordDao
import com.flowcycle.app.service.FlowCycleTimerService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主 ViewModel：连接 Service 状态、场景管理与 UI
 */
class FlowCycleViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = FlowCyclePreferences(application)
    private val recordDao: FlowCycleRecordDao =
        FlowCycleDatabase.getInstance(application).recordDao()

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ─── 场景列表流 ───────────────────────────────────────────────
    val scenes: StateFlow<List<Scene>> = preferences.scenesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PresetScenes.ALL)

    // ─── 默认场景 ID 流 ───────────────────────────────────────────
    val defaultSceneId: StateFlow<String> = preferences.defaultSceneIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PresetScenes.CLASSIC.id)

    // ─── DataStore 是否已就绪（防止启动时闪屏） ───────────────────
    // defaultSceneIdFlow 第一次发射真实值后置 true
    val isReady: StateFlow<Boolean> = preferences.defaultSceneIdFlow
        .map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ─── 计时页当前使用的场景 ID（仅内存，不写 DataStore）──────────
    // 初始值跟随 defaultSceneId，用户在计时页切换后独立变化
    private val _activeSceneId = MutableStateFlow<String?>(null)

    // 对外暴露：计时页用这个来高亮当前选中场景
    val activeSceneId: StateFlow<String> = combine(
        _activeSceneId,
        defaultSceneId
    ) { active, default ->
        active ?: default
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PresetScenes.CLASSIC.id)

    // ─── 当前激活场景（供计时器使用） ────────────────────────────
    val currentScene: StateFlow<Scene> = combine(scenes, activeSceneId) { sceneList, activeId ->
        sceneList.find { it.id == activeId } ?: PresetScenes.CLASSIC
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PresetScenes.CLASSIC)

    // ─── 计时器 UI 状态 ───────────────────────────────────────────
    val timerUiState: StateFlow<TimerUiState> = combine(
        FlowCycleTimerService.timerState,
        currentScene
    ) { serviceState, scene ->
        val effectiveConfig = if (serviceState.timerState == TimerState.IDLE) {
            scene.toConfig()
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
            config = effectiveConfig,
            currentSceneName = if (serviceState.timerState == TimerState.IDLE) {
                scene.name
            } else {
                serviceState.sceneName
            }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TimerUiState(currentSceneName = PresetScenes.CLASSIC.name)
    )

    // ─── 今日完成数 ───────────────────────────────────────────────
    val todayCount: StateFlow<Int> = recordDao
        .getTodayCount(dateFormatter.format(Date()))
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ─── 监听 Service 完成事件，写入历史记录 ──────────────────────
    init {
        viewModelScope.launch {
            FlowCycleTimerService.timerState
                .filter { it.phaseJustCompleted == PomodoroPhase.FOCUS }
                .collect { state ->
                    val record = FlowCycleRecord(
                        focusDurationMinutes = state.config.focusDurationMinutes,
                        date = dateFormatter.format(Date()),
                        sceneName = state.sceneName
                    )
                    recordDao.insert(record)
                    FlowCycleTimerService.timerState.update {
                        it.copy(phaseJustCompleted = null)
                    }
                }
        }
    }

    // ─── 计时器控制 ───────────────────────────────────────────────

    fun startTimer() {
        val scene = currentScene.value
        sendServiceAction(FlowCycleTimerService.ACTION_START, scene.toConfig(), scene.name)
    }

    fun pauseTimer() {
        sendServiceAction(FlowCycleTimerService.ACTION_PAUSE)
    }

    fun resumeTimer() {
        sendServiceAction(FlowCycleTimerService.ACTION_RESUME)
    }

    fun skipPhase() {
        sendServiceAction(FlowCycleTimerService.ACTION_SKIP)
    }

    fun resetTimer() {
        sendServiceAction(FlowCycleTimerService.ACTION_RESET)
    }

    // ─── 场景管理 ─────────────────────────────────────────────────

    /**
     * 计时页切换当前使用场景（仅内存，不影响场景页的"默认"标记）
     */
    fun switchScene(sceneId: String, resetTimer: Boolean = false) {
        _activeSceneId.value = sceneId
        if (resetTimer) {
            sendServiceAction(FlowCycleTimerService.ACTION_RESET)
        } else if (timerUiState.value.timerState == TimerState.IDLE) {
            val scene = scenes.value.find { it.id == sceneId } ?: return
            sendServiceAction(
                FlowCycleTimerService.ACTION_UPDATE_CONFIG,
                scene.toConfig(),
                scene.name
            )
        }
    }

    /**
     * 保存（新建或编辑）场景
     */
    fun saveScene(scene: Scene) {
        viewModelScope.launch {
            val currentList = scenes.value.toMutableList()
            val nonPreset = currentList.filter { !it.isPreset }.toMutableList()
            val existingIndex = nonPreset.indexOfFirst { it.id == scene.id }
            if (existingIndex >= 0) {
                nonPreset[existingIndex] = scene
            } else {
                nonPreset.add(scene)
            }
            preferences.saveCustomScenes(nonPreset)
        }
    }

    /**
     * 删除场景（预设场景不可删除）
     */
    fun deleteScene(sceneId: String) {
        viewModelScope.launch {
            val currentList = scenes.value.filter { !it.isPreset }.toMutableList()
            currentList.removeAll { it.id == sceneId }
            preferences.saveCustomScenes(currentList)

            // 如果删除的是当前默认场景，切回经典番茄
            if (defaultSceneId.value == sceneId) {
                preferences.setDefaultSceneId(PresetScenes.CLASSIC.id)
            }
        }
    }

    /**
     * 设置默认场景
     */
    fun setDefaultScene(sceneId: String) {
        viewModelScope.launch {
            preferences.setDefaultSceneId(sceneId)
        }
    }

    private fun sendServiceAction(
        action: String,
        config: PomodoroConfig? = null,
        sceneName: String? = null
    ) {
        val ctx = getApplication<Application>()
        val effectiveConfig = config ?: currentScene.value.toConfig()
        val intent = Intent(ctx, FlowCycleTimerService::class.java).apply {
            this.action = action
            putExtra(FlowCycleTimerService.EXTRA_CONFIG, effectiveConfig)
            sceneName?.let { putExtra(FlowCycleTimerService.EXTRA_SCENE_NAME, it) }
                ?: putExtra(
                    FlowCycleTimerService.EXTRA_SCENE_NAME,
                    currentScene.value.name
                )
        }
        ctx.startForegroundService(intent)
    }
}
