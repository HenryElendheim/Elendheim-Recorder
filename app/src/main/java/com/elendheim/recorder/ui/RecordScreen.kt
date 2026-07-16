package com.elendheim.recorder.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.recorder.audio.RecorderState

@Composable
fun RecordScreen(
    state: RecorderState,
    showPitch: Boolean,
    keepScreenOn: Boolean,
    onToggleRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    val view = LocalView.current
    val holdScreen = keepScreenOn && state.isRecording
    DisposableEffect(holdScreen) {
        view.keepScreenOn = holdScreen
        onDispose { view.keepScreenOn = false }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = Format.clock(state.elapsedMs),
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            color = colors.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (state.isRecording) "Recording" else "Ready",
            color = if (state.isRecording) colors.primary else colors.onSurfaceVariant,
            fontSize = 16.sp,
            letterSpacing = 2.sp
        )

        if (showPitch) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (state.isRecording) (state.pitch ?: "—") else "—",
                color = colors.primary,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "pitch",
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(40.dp))

        LevelMeter(amplitude = if (state.isRecording) state.amplitude else 0f)

        Spacer(Modifier.height(48.dp))

        RecordButton(isRecording = state.isRecording, onClick = onToggleRecord)
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 0.92f else 1f,
        animationSpec = tween(200),
        label = "recordScale"
    )
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = CircleShape,
        color = colors.primary,
        modifier = Modifier
            .size(112.dp)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                tint = colors.onPrimary,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun LevelMeter(amplitude: Float) {
    val colors = MaterialTheme.colorScheme
    val bars = 24
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until bars) {
            val distanceFromCentre = kotlin.math.abs(i - bars / 2f) / (bars / 2f)
            val weight = 1f - distanceFromCentre * 0.5f
            val level = (amplitude * weight).coerceIn(0f, 1f)
            val barHeight by animateFloatAsState(
                targetValue = 6f + level * 58f,
                animationSpec = tween(120),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (level > 0.02f) colors.primary else colors.surface)
            )
        }
    }
}
