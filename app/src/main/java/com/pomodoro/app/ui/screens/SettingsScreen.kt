package com.pomodoro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pomodoro.app.R
import com.pomodoro.app.data.model.PomodoroConfig
import com.pomodoro.app.viewmodel.PomodoroViewModel

/**
 * 设置页面（Apple 风格分组列表）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PomodoroViewModel,
    onNavigateBack: () -> Unit
) {
    val config by viewModel.config.collectAsStateWithLifecycle()

    // 本地编辑状态（点保存才写入）
    var focusDuration by remember(config) { mutableIntStateOf(config.focusDurationMinutes) }
    var shortBreak by remember(config) { mutableIntStateOf(config.shortBreakMinutes) }
    var longBreak by remember(config) { mutableIntStateOf(config.longBreakMinutes) }
    var longBreakInterval by remember(config) { mutableIntStateOf(config.longBreakInterval) }
    var soundEnabled by remember(config) { mutableStateOf(config.soundEnabled) }
    var vibrationEnabled by remember(config) { mutableStateOf(config.vibrationEnabled) }

    // 是否有未保存的修改
    val hasChanges = focusDuration != config.focusDurationMinutes ||
            shortBreak != config.shortBreakMinutes ||
            longBreak != config.longBreakMinutes ||
            longBreakInterval != config.longBreakInterval ||
            soundEnabled != config.soundEnabled ||
            vibrationEnabled != config.vibrationEnabled

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    // 返回箭头：只返回，不保存
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        // 底部保存按钮
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        viewModel.saveConfig(
                            PomodoroConfig(
                                focusDurationMinutes = focusDuration,
                                shortBreakMinutes = shortBreak,
                                longBreakMinutes = longBreak,
                                longBreakInterval = longBreakInterval,
                                soundEnabled = soundEnabled,
                                vibrationEnabled = vibrationEnabled
                            )
                        )
                        onNavigateBack()
                    },
                    enabled = hasChanges,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (hasChanges) "保存设置" else "已保存",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ── 时长设置组 ────────────────────────────────────────
            item {
                SettingsGroup(title = "时长设置") {
                    SettingsSliderRow(
                        label = "专注时长",
                        value = focusDuration,
                        range = 1..120,
                        unit = "分钟",
                        onValueChange = { focusDuration = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    SettingsSliderRow(
                        label = "短休时长",
                        value = shortBreak,
                        range = 1..30,
                        unit = "分钟",
                        onValueChange = { shortBreak = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    SettingsSliderRow(
                        label = "长休时长",
                        value = longBreak,
                        range = 5..60,
                        unit = "分钟",
                        onValueChange = { longBreak = it }
                    )
                }
            }

            // ── 长休设置组 ────────────────────────────────────────
            item {
                SettingsGroup(title = "长休触发") {
                    SettingsSliderRow(
                        label = "每隔几个番茄",
                        value = longBreakInterval,
                        range = 2..8,
                        unit = "个",
                        onValueChange = { longBreakInterval = it }
                    )
                }
            }

            // ── 提醒设置组 ────────────────────────────────────────
            item {
                SettingsGroup(title = "提醒方式") {
                    SettingsToggleRow(
                        label = "声音提醒",
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    SettingsToggleRow(
                        label = "震动提醒",
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

// ─── 设置分组容器 ──────────────────────────────────────────────────
@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(content = content)
        }
    }
}

// ─── 滑块行 ────────────────────────────────────────────────────────
@Composable
fun SettingsSliderRow(
    label: String,
    value: Int,
    range: IntRange,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── 开关行 ────────────────────────────────────────────────────────
@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
