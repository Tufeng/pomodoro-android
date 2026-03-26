package com.pomodoro.app.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.*
import androidx.core.app.NotificationCompat
import com.pomodoro.app.MainActivity
import com.pomodoro.app.R
import com.pomodoro.app.data.model.PomodoroConfig
import com.pomodoro.app.data.model.PomodoroPhase
import com.pomodoro.app.data.model.TimerState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 番茄钟前台服务
 * 负责后台保活计时，锁屏不中断
 */
class PomodoroTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "pomodoro_timer_channel"
        const val NOTIFICATION_ID = 1001

        // Intent Actions
        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_SKIP = "action_skip"
        const val ACTION_RESET = "action_reset"
        const val ACTION_UPDATE_CONFIG = "action_update_config"

        // Intent Extras
        const val EXTRA_CONFIG = "extra_config"

        // 全局状态（供 ViewModel 订阅）
        val timerState: MutableStateFlow<ServiceTimerState> = MutableStateFlow(ServiceTimerState())
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var countdownJob: Job? = null
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null

    private var config = PomodoroConfig()
    private var currentPhase = PomodoroPhase.FOCUS
    private var currentState = TimerState.IDLE
    private var remainingSeconds = config.focusDurationSeconds()
    private var completedPomodoros = 0

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val newConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_CONFIG, PomodoroConfig::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_CONFIG)
                }
                newConfig?.let { config = it }
                startTimer()
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_SKIP -> skipPhase()
            ACTION_RESET -> resetTimer()
            ACTION_UPDATE_CONFIG -> {
                val newConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_CONFIG, PomodoroConfig::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_CONFIG)
                }
                newConfig?.let {
                    config = it
                    if (currentState == TimerState.IDLE) {
                        remainingSeconds = config.phaseDurationSeconds(currentPhase)
                        emitState()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun startTimer() {
        currentState = TimerState.RUNNING
        remainingSeconds = config.phaseDurationSeconds(currentPhase)
        startForegroundService()
        startCountdown()
        emitState()
    }

    private fun pauseTimer() {
        currentState = TimerState.PAUSED
        countdownJob?.cancel()
        updateNotification()
        emitState()
    }

    private fun resumeTimer() {
        currentState = TimerState.RUNNING
        startCountdown()
        emitState()
    }

    private fun skipPhase() {
        countdownJob?.cancel()
        moveToNextPhase()
    }

    private fun resetTimer() {
        countdownJob?.cancel()
        currentState = TimerState.IDLE
        currentPhase = PomodoroPhase.FOCUS
        completedPomodoros = 0
        remainingSeconds = config.focusDurationSeconds()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        emitState()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            while (remainingSeconds > 0 && currentState == TimerState.RUNNING) {
                delay(1000L)
                if (currentState == TimerState.RUNNING) {
                    remainingSeconds--
                    emitState()
                    updateNotification()
                }
            }
            if (remainingSeconds <= 0 && currentState == TimerState.RUNNING) {
                onPhaseComplete()
            }
        }
    }

    private fun onPhaseComplete() {
        triggerAlert()
        if (currentPhase == PomodoroPhase.FOCUS) {
            completedPomodoros++
            // 通知 ViewModel 记录完成
            timerState.update { it.copy(phaseJustCompleted = PomodoroPhase.FOCUS) }
        }
        moveToNextPhase()
    }

    private fun moveToNextPhase() {
        currentPhase = when (currentPhase) {
            PomodoroPhase.FOCUS -> {
                if (completedPomodoros > 0 && completedPomodoros % config.longBreakInterval == 0) {
                    PomodoroPhase.LONG_BREAK
                } else {
                    PomodoroPhase.SHORT_BREAK
                }
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> PomodoroPhase.FOCUS
        }
        remainingSeconds = config.phaseDurationSeconds(currentPhase)
        currentState = TimerState.RUNNING
        startCountdown()
        emitState()
    }

    private fun triggerAlert() {
        if (config.vibrationEnabled) {
            val pattern = longArrayOf(0, 300, 200, 300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        }
        if (config.soundEnabled) {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, R.raw.timer_complete)
                mediaPlayer?.setOnCompletionListener { it.release() }
                mediaPlayer?.start()
            } catch (e: Exception) {
                // 音效文件不存在时静默处理
            }
        }
    }

    private fun emitState() {
        timerState.update {
            ServiceTimerState(
                phase = currentPhase,
                timerState = currentState,
                remainingSeconds = remainingSeconds,
                totalSeconds = config.phaseDurationSeconds(currentPhase),
                completedPomodoros = completedPomodoros,
                config = config
            )
        }
    }

    // ─── Foreground Service & Notification ───────────────────────

    private fun startForegroundService() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "番茄钟计时器",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示番茄钟计时进度"
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeText = "%02d:%02d".format(minutes, seconds)
        val phaseText = currentPhase.displayName()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🍅 $phaseText")
            .setContentText(timeText)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // 锁屏界面显示通知内容
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaPlayer?.release()
    }
}

/**
 * Service 向外暴露的状态数据类（可 Parcelable）
 */
data class ServiceTimerState(
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val timerState: TimerState = TimerState.IDLE,
    val remainingSeconds: Long = 25 * 60L,
    val totalSeconds: Long = 25 * 60L,
    val completedPomodoros: Int = 0,
    val config: PomodoroConfig = PomodoroConfig(),
    val phaseJustCompleted: PomodoroPhase? = null
)
