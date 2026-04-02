package com.flowcycle.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 设置页面（v2.0）
 * 只保留通用设置（声音 / 震动 已移至场景配置）
 * 此页面预留扩展空间
 */
@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SettingsGroup(title = "关于", topPadding = 16.dp) {
                SettingsInfoRow(label = "应用名称", value = "节律 FlowCycle")
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                SettingsInfoRow(label = "版本", value = "2.0.0")
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                SettingsInfoRow(label = "理念", value = "按你的节奏，专注每一刻")
            }
        }

        item {
            SettingsGroup(title = "使用提示") {
                SettingsHintRow(
                    hint = "在「场景」页面创建和管理你的专注方案，每个场景可以独立配置时长和提醒方式。"
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                SettingsHintRow(
                    hint = "计时页面点击场景名称可快速切换当前使用的场景。"
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 16.dp, top = topPadding, bottom = 8.dp)
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

@Composable
fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsHintRow(hint: String) {
    Text(
        text = hint,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
