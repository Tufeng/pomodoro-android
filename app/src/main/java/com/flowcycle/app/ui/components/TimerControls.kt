package com.flowcycle.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flowcycle.app.R
import com.flowcycle.app.data.model.TimerState

@Composable
fun TimerControls(
    timerState: TimerState,
    primaryColor: Color,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (timerState != TimerState.IDLE) {
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_reset),
                    contentDescription = "重置",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(52.dp))
        }

        Button(
            onClick = when (timerState) {
                TimerState.IDLE -> onStart
                TimerState.RUNNING -> onPause
                TimerState.PAUSED -> onResume
            },
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                painter = painterResource(
                    when (timerState) {
                        TimerState.RUNNING -> R.drawable.ic_pause
                        else -> R.drawable.ic_play
                    }
                ),
                contentDescription = when (timerState) {
                    TimerState.RUNNING -> "暂停"
                    TimerState.PAUSED -> "继续"
                    else -> "开始"
                },
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        if (timerState != TimerState.IDLE) {
            IconButton(
                onClick = onSkip,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip),
                    contentDescription = "跳过",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(52.dp))
        }
    }
}
