package com.elendheim.recorder.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One analysed moment: the note (MIDI) heard at [timeMs], or NO_PITCH. */
data class PitchFrame(val timeMs: Int, val midi: Int)

/**
 * Reads a finished WAV and estimates the note over time — a pitch track used
 * to show the note while playing back and to draw the piano roll. Runs off the
 * main thread; results are small and cacheable per recording.
 */
object PitchAnalyzer {

    private const val WINDOW = 2048
    private const val WAV_HEADER_BYTES = 44
    private const val MAX_FRAMES = 6000   // bound work/memory for long takes

    fun analyze(wavFile: File, stepMs: Int = 70): List<PitchFrame> {
        if (!wavFile.exists() || wavFile.length() <= WAV_HEADER_BYTES) return emptyList()

        RandomAccessFile(wavFile, "r").use { raf ->
            val header = ByteArray(WAV_HEADER_BYTES)
            raf.readFully(header)
            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val sampleRate = bb.getInt(24).let { if (it in 8000..192000) it else 44100 }

            val dataBytes = (wavFile.length() - WAV_HEADER_BYTES).coerceAtLeast(0)
            val totalSamples = (dataBytes / 2).toInt()
            if (totalSamples < WINDOW) return emptyList()

            val pcm = ShortArray(totalSamples)
            raf.seek(WAV_HEADER_BYTES.toLong())
            val chunk = ByteArray(8192)
            var filled = 0
            while (filled < totalSamples) {
                val want = minOf(chunk.size, (totalSamples - filled) * 2)
                val read = raf.read(chunk, 0, want)
                if (read <= 0) break
                val count = read / 2
                ByteBuffer.wrap(chunk, 0, count * 2)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(pcm, filled, count)
                filled += count
            }

            var step = maxOf(1, sampleRate * stepMs / 1000)
            val estimated = (filled - WINDOW) / step + 1
            if (estimated > MAX_FRAMES) step = (filled - WINDOW) / MAX_FRAMES + 1

            val frames = ArrayList<PitchFrame>()
            val window = ShortArray(WINDOW)
            var start = 0
            while (start + WINDOW <= filled) {
                System.arraycopy(pcm, start, window, 0, WINDOW)
                val midi = PitchDetector.detectMidi(window, WINDOW, sampleRate)
                frames.add(PitchFrame((start.toLong() * 1000 / sampleRate).toInt(), midi))
                start += step
            }
            return frames
        }
    }
}
