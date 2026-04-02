package com.flowcycle.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.flowcycle.app.ui.screens.ScenesScreen
import com.flowcycle.app.ui.screens.SettingsScreen
import com.flowcycle.app.ui.screens.TimerScreen
import com.flowcycle.app.ui.theme.FlowCycleTheme
import com.flowcycle.app.viewmodel.FlowCycleViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FlowCycleViewModel by viewModels()

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // 用户选择后不强制，计时功能仍可正常使用
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            FlowCycleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FlowCycleApp(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * 底部导航枚举
 */
enum class BottomTab(
    val label: String,
    val iconRes: Int,
    val contentDescription: String
) {
    TIMER("计时", R.drawable.ic_timer, "计时"),
    SCENES("场景", R.drawable.ic_scenes, "场景"),
    SETTINGS("设置", R.drawable.ic_settings, "设置")
}

/**
 * 主应用容器（底部导航）
 */
@Composable
fun FlowCycleApp(viewModel: FlowCycleViewModel) {
    var selectedTab by remember { mutableStateOf(BottomTab.TIMER) }
    // 场景编辑器展开时隐藏全局 TopBar / BottomBar
    var scenesEditorVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (!scenesEditorVisible) {
                FlowCycleTopBar(selectedTab = selectedTab)
            }
        },
        bottomBar = {
            if (!scenesEditorVisible) {
                FlowCycleBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    // 编辑器全屏时不使用 Scaffold 的 padding
                    if (scenesEditorVisible) Modifier
                    else Modifier.padding(paddingValues)
                )
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(150))
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    BottomTab.TIMER -> TimerScreen(viewModel = viewModel)
                    BottomTab.SCENES -> ScenesScreen(
                        viewModel = viewModel,
                        onEditorVisibilityChange = { scenesEditorVisible = it }
                    )
                    BottomTab.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}

/**
 * 顶部 AppBar（根据当前 Tab 显示标题）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlowCycleTopBar(selectedTab: BottomTab) {
    TopAppBar(
        title = {
            when (selectedTab) {
                BottomTab.TIMER -> {
                    // 计时页显示 App 名称
                    Text(
                        text = "节律",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                BottomTab.SCENES -> Text("场景", fontWeight = FontWeight.SemiBold)
                BottomTab.SETTINGS -> Text("设置", fontWeight = FontWeight.SemiBold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/**
 * 底部导航栏
 */
@Composable
private fun FlowCycleBottomBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = tab.contentDescription,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
