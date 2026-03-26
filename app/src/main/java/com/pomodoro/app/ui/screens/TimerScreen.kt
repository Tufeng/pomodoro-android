package com.pomodoro.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.pomodoro.app.R
import com.pomodoro.app.data.model.PomodoroPhase
import com.pomodoro.app.data.model.TimerState
import com.pomodoro.app.ui.components.CircularProgressRing
import com.pomodoro.app.ui.components.TimerControls
import com.pomodoro.app.ui.theme.*
import com.pomodoro.app.viewmodel.PomodoroViewModel

/**
 * 主计时界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: PomodoroViewModel,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.timerUiState.collectAsStateWithLifecycle()
    val todayCount by viewModel.todayCount.collectAsStateWithLifecycle()

    // 根据当前阶段选择颜色
    val phaseColor = when (uiState.phase) {
        PomodoroPhase.FOCUS -> PomodoroRed
        PomodoroPhase.SHORT_BREAK -> BreakBlue
        PomodoroPhase.LONG_BREAK -> LongBreakPurple
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("专注", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "设置"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

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
            // 分和秒分开计算，各自独立控制动画
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
                        // 每个数字字符独立占一个固定宽度槽位，彻底消除宽窄字符导致的偏移
                        DigitSlot(char = minutesStr[0], label = "m0")
                        DigitSlot(char = minutesStr[1], label = "m1")
                        // 冒号分隔符（固定不动）
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

            Spacer(modifier = Modifier.height(16.dp))

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
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "今日完成 $todayCount 个",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
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
}

/**
 * 单个数字字符槽位：固定宽度 + 纯淡入淡出动画
 * 每个 0-9 字符占据完全相同的空间，彻底消除宽窄字符引起的布局抖动
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
