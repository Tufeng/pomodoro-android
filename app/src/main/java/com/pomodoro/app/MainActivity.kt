package com.pomodoro.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pomodoro.app.ui.screens.SettingsScreen
import com.pomodoro.app.ui.screens.TimerScreen
import com.pomodoro.app.ui.theme.PomodoroTheme
import com.pomodoro.app.viewmodel.PomodoroViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PomodoroViewModel by viewModels()

    // Android 13+ 通知权限申请（POST_NOTIFICATIONS 运行时权限）
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // 用户选择后不强制要求，计时功能仍可正常使用，仅通知不显示
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13（API 33）及以上需动态申请通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            PomodoroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PomodoroApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun PomodoroApp(viewModel: PomodoroViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "timer"
    ) {
        composable("timer") {
            TimerScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}


@Composable
fun PomodoroApp(viewModel: PomodoroViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "timer"
    ) {
        composable("timer") {
            TimerScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
