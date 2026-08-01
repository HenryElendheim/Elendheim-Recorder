package com.elendheim.recorder.audio

import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A small pitch estimator: a YIN-style difference function on a PCM buffer,
 * turned into the nearest musical note. Good enough to show the note you are
 * hitting or the shape of a melody; it is not a tuner-grade instrument.
 */
object PitchDetector {

    private val noteNames = arrayOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    )

    const val NO_PITCH = -1

    /** Nearest MIDI note for the buffer, or [NO_PITCH] when too quiet/unclear. */
    fun detectMidi(buffer: ShortArray, size: Int, sampleRate: Int): Int {
        val n = minOf(size, 2048)
        if (n < 1024) return NO_PITCH

        var energy = 0.0
        for (i in 0 until n) {
            val s = buffer[i].toDouble()
            energy += s * s
        }
        val rms = sqrt(energy / n)
        if (rms < 500.0) return NO_PITCH

        val maxLag = n / 2
        val minLag = maxOf(2, sampleRate / 1600)   // ceiling of roughly 1600 Hz
        val window = n - maxLag

        val yin = DoubleArray(maxLag)
        var runningSum = 0.0
        for (lag in 1 until maxLag) {
            var sum = 0.0
            for (i in 0 until window) {
                val d = buffer[i].toDouble() - buffer[i + lag].toDouble()
                sum += d * d
            }
            runningSum += sum
            yin[lag] = if (runningSum > 0.0) sum * lag / runningSum else 1.0
        }

        val threshold = 0.15
        var best = -1
        var lag = minLag
        while (lag < maxLag) {
            if (yin[lag] < threshold) {
                while (lag + 1 < maxLag && yin[lag + 1] < yin[lag]) lag++
                best = lag
                break
            }
            lag++
        }
        if (best < 0) {
            var minVal = Double.MAX_VALUE
            for (l in minLag until maxLag) {
                if (yin[l] < minVal) {
                    minVal = yin[l]
                    best = l
                }
            }
            if (best < 0 || minVal > 0.4) return NO_PITCH
        }

        val freq = sampleRate.toDouble() / best
        if (freq < 40.0 || freq > 2000.0) return NO_PITCH

        val midi = (69.0 + 12.0 * (ln(freq / 440.0) / ln(2.0))).roundToInt()
        if (midi < 24 || midi > 108) return NO_PITCH
        return midi
    }

    /** Human-readable note name for a MIDI number, e.g. 60 -> "C4". */
    fun noteName(midi: Int): String {
        val name = noteNames[((midi % 12) + 12) % 12]
        val octave = midi / 12 - 1
        return "$name$octave"
    }

    /** Note label for a buffer, or null when too quiet/unclear. */
    fun detect(buffer: ShortArray, size: Int, sampleRate: Int): String? {
        val midi = detectMidi(buffer, size, sampleRate)
        return if (midi == NO_PITCH) null else noteName(midi)
    }
}
