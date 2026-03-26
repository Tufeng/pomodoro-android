package com.pomodoro.app

import com.pomodoro.app.data.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * PomodoroConfig 单元测试
 */
class PomodoroConfigTest {

    @Test
    fun `focusDurationSeconds should return minutes times 60`() {
        val config = PomodoroConfig(focusDurationMinutes = 25)
        assertEquals(1500L, config.focusDurationSeconds())
    }

    @Test
    fun `shortBreakSeconds should return minutes times 60`() {
        val config = PomodoroConfig(shortBreakMinutes = 5)
        assertEquals(300L, config.shortBreakSeconds())
    }

    @Test
    fun `longBreakSeconds should return minutes times 60`() {
        val config = PomodoroConfig(longBreakMinutes = 15)
        assertEquals(900L, config.longBreakSeconds())
    }

    @Test
    fun `phaseDurationSeconds returns correct value for each phase`() {
        val config = PomodoroConfig(
            focusDurationMinutes = 25,
            shortBreakMinutes = 5,
            longBreakMinutes = 15
        )
        assertEquals(1500L, config.phaseDurationSeconds(PomodoroPhase.FOCUS))
        assertEquals(300L, config.phaseDurationSeconds(PomodoroPhase.SHORT_BREAK))
        assertEquals(900L, config.phaseDurationSeconds(PomodoroPhase.LONG_BREAK))
    }

    @Test
    fun `default config should have expected values`() {
        val config = PomodoroConfig()
        assertEquals(25, config.focusDurationMinutes)
        assertEquals(5, config.shortBreakMinutes)
        assertEquals(15, config.longBreakMinutes)
        assertEquals(4, config.longBreakInterval)
        assertTrue(config.soundEnabled)
        assertTrue(config.vibrationEnabled)
    }
}

/**
 * TimerUiState 单元测试
 */
class TimerUiStateTest {

    @Test
    fun `progress should be 0 when timer has not started`() {
        val state = TimerUiState(
            remainingSeconds = 1500L,
            totalSeconds = 1500L
        )
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun `progress should be 0_5 when half time elapsed`() {
        val state = TimerUiState(
            remainingSeconds = 750L,
            totalSeconds = 1500L
        )
        assertEquals(0.5f, state.progress, 0.001f)
    }

    @Test
    fun `progress should be 1_0 when all time elapsed`() {
        val state = TimerUiState(
            remainingSeconds = 0L,
            totalSeconds = 1500L
        )
        assertEquals(1.0f, state.progress, 0.001f)
    }

    @Test
    fun `progress should be 0 when totalSeconds is 0`() {
        val state = TimerUiState(
            remainingSeconds = 0L,
            totalSeconds = 0L
        )
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun `formattedTime should display MM_SS format`() {
        val state = TimerUiState(remainingSeconds = 1500L)
        assertEquals("25:00", state.formattedTime)
    }

    @Test
    fun `formattedTime should pad single digit seconds`() {
        val state = TimerUiState(remainingSeconds = 65L)
        assertEquals("01:05", state.formattedTime)
    }

    @Test
    fun `formattedTime should show 00_00 when finished`() {
        val state = TimerUiState(remainingSeconds = 0L)
        assertEquals("00:00", state.formattedTime)
    }
}

/**
 * PomodoroPhase 单元测试
 */
class PomodoroPhaseTest {

    @Test
    fun `displayName should return correct Chinese name`() {
        assertEquals("专注", PomodoroPhase.FOCUS.displayName())
        assertEquals("短休息", PomodoroPhase.SHORT_BREAK.displayName())
        assertEquals("长休息", PomodoroPhase.LONG_BREAK.displayName())
    }
}
