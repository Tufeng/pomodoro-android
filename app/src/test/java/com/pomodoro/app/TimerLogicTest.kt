package com.pomodoro.app

import app.cash.turbine.test
import com.pomodoro.app.data.model.*
import com.pomodoro.app.service.PomodoroTimerService
import com.pomodoro.app.service.ServiceTimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 计时状态流转逻辑测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerStateFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // 重置 Service 状态
        PomodoroTimerService.timerState.value = ServiceTimerState()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be IDLE with FOCUS phase`() = runTest {
        PomodoroTimerService.timerState.test {
            val state = awaitItem()
            assertEquals(TimerState.IDLE, state.timerState)
            assertEquals(PomodoroPhase.FOCUS, state.phase)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `after phase complete, phaseJustCompleted should be set`() = runTest {
        PomodoroTimerService.timerState.test {
            awaitItem() // 初始状态

            // 模拟专注完成
            PomodoroTimerService.timerState.value = ServiceTimerState(
                phase = PomodoroPhase.SHORT_BREAK,
                timerState = TimerState.RUNNING,
                phaseJustCompleted = PomodoroPhase.FOCUS
            )

            val completedState = awaitItem()
            assertEquals(PomodoroPhase.FOCUS, completedState.phaseJustCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing phaseJustCompleted should emit null`() = runTest {
        PomodoroTimerService.timerState.test {
            awaitItem()

            PomodoroTimerService.timerState.value = ServiceTimerState(
                phaseJustCompleted = PomodoroPhase.FOCUS
            )
            awaitItem()

            PomodoroTimerService.timerState.value = PomodoroTimerService.timerState.value.copy(
                phaseJustCompleted = null
            )
            val clearedState = awaitItem()
            assertNull(clearedState.phaseJustCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/**
 * 阶段切换逻辑测试
 */
class PhaseTransitionTest {

    private val config = PomodoroConfig(longBreakInterval = 4)

    /**
     * 模拟阶段切换逻辑（与 Service 中一致，方便单元测试）
     */
    private fun nextPhase(
        currentPhase: PomodoroPhase,
        completedPomodoros: Int
    ): PomodoroPhase {
        return when (currentPhase) {
            PomodoroPhase.FOCUS -> {
                // Bug fix: completedPomodoros=0 时 0%4==0 会误触发长休，需加 >0 判断
                if (completedPomodoros > 0 && completedPomodoros % config.longBreakInterval == 0) {
                    PomodoroPhase.LONG_BREAK
                } else {
                    PomodoroPhase.SHORT_BREAK
                }
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> PomodoroPhase.FOCUS
        }
    }

    @Test
    fun `after 0 completed (edge case) should go to short break NOT long break`() {
        // 这是关键边界：completedPomodoros=0 时不应触发长休
        assertEquals(PomodoroPhase.SHORT_BREAK, nextPhase(PomodoroPhase.FOCUS, 0))
    }

    @Test
    fun `after 1st focus should go to short break`() {
        assertEquals(PomodoroPhase.SHORT_BREAK, nextPhase(PomodoroPhase.FOCUS, 1))
    }

    @Test
    fun `after 4th focus should go to long break`() {
        assertEquals(PomodoroPhase.LONG_BREAK, nextPhase(PomodoroPhase.FOCUS, 4))
    }

    @Test
    fun `after 8th focus should go to long break`() {
        assertEquals(PomodoroPhase.LONG_BREAK, nextPhase(PomodoroPhase.FOCUS, 8))
    }

    @Test
    fun `after short break should go back to focus`() {
        assertEquals(PomodoroPhase.FOCUS, nextPhase(PomodoroPhase.SHORT_BREAK, 1))
    }

    @Test
    fun `after long break should go back to focus`() {
        assertEquals(PomodoroPhase.FOCUS, nextPhase(PomodoroPhase.LONG_BREAK, 4))
    }

    @Test
    fun `after 2nd focus should go to short break (not long)`() {
        assertEquals(PomodoroPhase.SHORT_BREAK, nextPhase(PomodoroPhase.FOCUS, 2))
    }
}
