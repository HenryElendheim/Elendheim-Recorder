package com.elendheim.recorder.audio

import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A small pitch estimator: a YIN-style difference function on a PCM buffer,
 * turned into the nearest musical note (e.g. C4, F#3). Good enough to show the
 * note you are hitting while recording; it is not a tuner-grade instrument.
 */
object PitchDetector {

    private val noteNames = arrayOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    )

    /** Returns a note label, or null when the signal is too quiet or unclear. */
    fun detect(buffer: ShortArray, size: Int, sampleRate: Int): String? {
        val n = minOf(size, 2048)
        if (n < 1024) return null

        // Quiet frames have no meaningful pitch.
        var energy = 0.0
        for (i in 0 until n) {
            val s = buffer[i].toDouble()
            energy += s * s
        }
        val rms = sqrt(energy / n)
        if (rms < 500.0) return null

        val maxLag = n / 2
        val minLag = maxOf(2, sampleRate / 1600)   // ceiling of roughly 1600 Hz
        val window = n - maxLag

        // Cumulative-mean-normalised difference function (YIN, simplified).
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

        // First dip below the threshold is the fundamental; refine to its local min.
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
            if (best < 0 || minVal > 0.4) return null
        }

        val freq = sampleRate.toDouble() / best
        if (freq < 40.0 || freq > 2000.0) return null

        val midi = (69.0 + 12.0 * (ln(freq / 440.0) / ln(2.0))).roundToInt()
        if (midi < 24 || midi > 108) return null

        val name = noteNames[((midi % 12) + 12) % 12]
        val octave = midi / 12 - 1
        return "$name$octave"
    }
}
