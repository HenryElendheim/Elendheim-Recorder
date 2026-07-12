package com.elendheim.recorder.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * The capture engine: AudioRecord -> raw PCM on disk, nothing more. It knows
 * nothing about the UI, the service, or WAV headers so it stays testable and
 * reusable. PCM is written as it arrives, so a crash never loses more than the
 * last buffer.
 */
class Recorder {

    val sampleRate = 44100                 // 44.1 kHz, CD quality
    val channels = 1                       // mono
    val bitsPerSample = 16

    @Volatile
    var currentAmplitude: Float = 0f
        private set

    @Volatile
    private var recording = false
    private var thread: Thread? = null

    val isRecording: Boolean get() = recording

    /** Begin capturing into [pcmFile]. Returns false if the mic could not open. */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(pcmFile: File): Boolean {
        if (recording) return true

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuf <= 0) return false

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, channelConfig, audioFormat, minBuf * 2
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        recording = true
        recorder.startRecording()

        thread = Thread {
            val shorts = ShortArray(minBuf)
            val bytes = ByteArray(minBuf * 2)
            BufferedOutputStream(FileOutputStream(pcmFile)).use { out ->
                while (recording) {
                    val read = recorder.read(shorts, 0, shorts.size)
                    if (read > 0) {
                        var peak = 0
                        for (i in 0 until read) {
                            val a = abs(shorts[i].toInt())
                            if (a > peak) peak = a
                        }
                        currentAmplitude = peak / 32767f

                        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until read) bb.putShort(shorts[i])
                        out.write(bytes, 0, read * 2)
                    }
                }
                out.flush()
            }
            recorder.stop()
            recorder.release()
        }.apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
        return true
    }

    /** Stop capturing and wait for the writer thread to flush the last buffer. */
    fun stop() {
        recording = false
        currentAmplitude = 0f
        thread?.join()
        thread = null
    }
}
