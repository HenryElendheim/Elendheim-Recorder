package com.elendheim.recorder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.elendheim.recorder.audio.PitchDetector
import com.elendheim.recorder.audio.PitchFrame
import kotlin.math.abs

/** The note label heard nearest [positionMs] in the track, or null. */
fun currentNote(frames: List<PitchFrame>, positionMs: Int): String? {
    if (frames.isEmpty()) return null
    val nearest = frames.minByOrNull { abs(it.timeMs - positionMs) } ?: return null
    return if (nearest.midi == PitchDetector.NO_PITCH) null else PitchDetector.noteName(nearest.midi)
}

/**
 * A compact piano roll: time runs left to right, pitch bottom to top. Detected
 * notes are drawn as accent blocks, octave lines mark the grid, and a playhead
 * tracks playback.
 */
@Composable
fun PianoRoll(
    frames: List<PitchFrame>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val voiced = frames.filter { it.midi != PitchDetector.NO_PITCH }

    val minMidi = (voiced.minOfOrNull { it.midi } ?: 55) - 1
    val maxMidi = (voiced.maxOfOrNull { it.midi } ?: 67) + 1
    val range = (maxMidi - minMidi).coerceAtLeast(4)
    val totalMs = (frames.lastOrNull()?.timeMs ?: 1).coerceAtLeast(1)

    val gridColor = colors.onSurfaceVariant.copy(alpha = 0.25f)
    val noteColor = colors.primary
    val headColor = colors.onBackground

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
    ) {
        val w = size.width
        val h = size.height
        val rowH = (h / range).coerceAtLeast(2f)

        // Octave gridlines (every C).
        for (m in minMidi..maxMidi) {
            if (m % 12 == 0) {
                val y = h - ((m - minMidi).toFloat() / range) * h
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
        }

        // Note blocks.
        val blockW = (if (frames.size > 1) w / frames.size * 1.3f else 4f).coerceAtLeast(2f)
        voiced.forEach { f ->
            val x = f.timeMs.toFloat() / totalMs * w
            val y = h - ((f.midi - minMidi + 0.5f) / range) * h
            drawRect(
                color = noteColor,
                topLeft = Offset(x, y - rowH / 2f),
                size = Size(blockW, rowH)
            )
        }

        // Playhead.
        val px = progress.coerceIn(0f, 1f) * w
        drawLine(headColor, Offset(px, 0f), Offset(px, h), strokeWidth = 2f)
    }
}
