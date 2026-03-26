package com.pomodoro.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pomodoro.app.ui.theme.RingBackground
import com.pomodoro.app.ui.theme.RingBackgroundDark
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Apple 风格圆形进度环组件
 * @param progress 进度 0f-1f
 * @param color 进度颜色
 * @param size 组件尺寸
 * @param strokeWidth 线宽
 */
@Composable
fun CircularProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    strokeWidth: Dp = 14.dp,
    content: @Composable () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val trackColor = if (isDark) RingBackgroundDark else RingBackground

    // 平滑动画
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f

            // 背景轨道
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    this.size.width - strokePx,
                    this.size.height - strokePx
                )
            )

            // 进度弧
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    this.size.width - strokePx,
                    this.size.height - strokePx
                )
            )
        }
        content()
    }
}
