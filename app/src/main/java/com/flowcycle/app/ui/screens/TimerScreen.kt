package com.flowcycle.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowcycle.app.R
import com.flowcycle.app.data.model.PomodoroPhase
import com.flowcycle.app.data.model.Scene
import com.flowcycle.app.data.model.TimerState
import com.flowcycle.app.ui.components.CircularProgressRing
import com.flowcycle.app.ui.components.TimerControls
import com.flowcycle.app.ui.theme.*
import com.flowcycle.app.viewmodel.FlowCycleViewModel

/**
 * 主计时界面（v2.0）
 * - 显示当前场景名，点击可切换场景
 * - 计时中切换场景时弹窗确认
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: FlowCycleViewModel
) {
    val uiState by viewModel.timerUiState.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()
    val scenes by viewModel.scenes.collectAsStateWithLifecycle()
    val activeSceneId by viewModel.activeSceneId.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()

    // DataStore 未就绪前不渲染任何内容，避免初始默认值闪屏
    if (!isReady) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    // 场景切换弹出框状态
    var showScenePicker by remember { mutableStateOf(false) }
    // 计时中切换场景的确认弹窗
    var pendingSwitchSceneId by remember { mutableStateOf<String?>(null) }

    val phaseColor = when (uiState.phase) {
        PomodoroPhase.FOCUS -> PomodoroRed
        PomodoroPhase.SHORT_BREAK -> BreakBlue
        PomodoroPhase.LONG_BREAK -> LongBreakPurple
    }

    // 场景切换确认弹窗（计时中）
    if (pendingSwitchSceneId != null) {
        AlertDialog(
            onDismissRequest = { pendingSwitchSceneId = null },
            title = { Text("切换场景") },
            text = { Text("当前正在计时中，切换场景后将重置计时。确认切换吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.switchScene(pendingSwitchSceneId!!, resetTimer = true)
                    pendingSwitchSceneId = null
                }) {
                    Text("确认切换", color = PomodoroRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSwitchSceneId = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 场景选择底部弹出
    if (showScenePicker) {
        ScenePickerSheet(
            scenes = scenes,
            currentSceneId = activeSceneId,
            onDismiss = { showScenePicker = false },
            onSelect = { sceneId ->
                showScenePicker = false
                if (uiState.timerState != TimerState.IDLE) {
                    // 计时中，需要确认
                    if (sceneId != activeSceneId) {
                        pendingSwitchSceneId = sceneId
                    }
                } else {
                    viewModel.switchScene(sceneId, resetTimer = false)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── 阶段标签 ──────────────────────────────────────────
        AnimatedContent(
            targetState = uiState.phase,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "phaseLabel"
        ) { phase ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = phaseColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = phase.displayName(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = phaseColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 进度环 + 计时数字 ─────────────────────────────────
        val minutes = (uiState.remainingSeconds / 60).toInt()
        val seconds = (uiState.remainingSeconds % 60).toInt()
        val minutesStr = "%02d".format(minutes)
        val secondsStr = "%02d".format(seconds)

        CircularProgressRing(
            progress = uiState.progress,
            color = phaseColor,
            size = 280.dp,
            strokeWidth = 14.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    DigitSlot(char = minutesStr[0], label = "m0")
                    DigitSlot(char = minutesStr[1], label = "m1")
                    Text(
                        text = ":",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Thin,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    DigitSlot(char = secondsStr[0], label = "s0")
                    DigitSlot(char = secondsStr[1], label = "s1")
                }
                Text(
                    text = when (uiState.timerState) {
                        TimerState.PAUSED -> "已暂停"
                        TimerState.RUNNING -> uiState.phase.displayName()
                        TimerState.IDLE -> "准备好了"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── 场景名（可点击切换） ──────────────────────────────
        Row(
            modifier = Modifier
                .clickable { showScenePicker = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnimatedContent(
                targetState = uiState.currentSceneName,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "sceneName"
            ) { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = "切换场景",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 今日完成统计 ──────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(minOf(uiState.completedPomodoros, 8)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = PomodoroRed,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
            if (uiState.completedPomodoros > 8) {
                Text(
                    text = "+${uiState.completedPomodoros - 8}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            if (todayCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "今日完成 $todayCount 个",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── 控制按钮 ──────────────────────────────────────────
        TimerControls(
            timerState = uiState.timerState,
            primaryColor = phaseColor,
            onStart = viewModel::startTimer,
            onPause = viewModel::pauseTimer,
            onResume = viewModel::resumeTimer,
            onSkip = viewModel::skipPhase,
            onReset = viewModel::resetTimer
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

/**
 * 场景选择底部弹出（ModalBottomSheet）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenePickerSheet(
    scenes: List<Scene>,
    currentSceneId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "选择场景",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            HorizontalDivider()
            LazyColumn {
                items(scenes) { scene ->
                    val isSelected = scene.id == currentSceneId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(scene.id) }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = scene.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) PomodoroRed else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${scene.focusDurationMinutes}分专注 · ${scene.shortBreakMinutes}分短休",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = PomodoroRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 24.dp))
                }
            }
        }
    }
}

/**
 * 单个数字字符槽位：固定宽度 + 纯淡入淡出动画
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun DigitSlot(char: Char, label: String) {
    Box(
        modifier = Modifier.width(40.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = char,
            transitionSpec = {
                fadeIn(animationSpec = tween(150)) togetherWith
                        fadeOut(animationSpec = tween(150))
            },
            label = label
        ) { digit ->
            Text(
                text = digit.toString(),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 64.sp,
                fontWeight = FontWeight.Thin,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
