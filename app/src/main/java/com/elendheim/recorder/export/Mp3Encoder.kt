package com.elendheim.recorder.export

import com.naman14.androidlame.LameBuilder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Encodes a WAV master into an MP3 with the LAME encoder. Android has no built
 * in MP3 encoder, so we lean on a bundled LAME (a prebuilt native library that
 * ships in the dependency — no NDK build on our side).
 */
object Mp3Encoder {

    private const val WAV_HEADER_BYTES = 44L

    /** Returns true on success. Reads PCM from [wavFile], writes [outFile]. */
    fun encode(
        wavFile: File,
        outFile: File,
        sampleRate: Int,
        channels: Int,
        bitRate: Int = 192
    ): Boolean {
        val lame = LameBuilder()
            .setInSampleRate(sampleRate)
            .setOutSampleRate(sampleRate)
            .setOutChannels(channels)
            .setOutBitrate(bitRate)
            .setQuality(5)
            .build()

        val raf = RandomAccessFile(wavFile, "r")
        return try {
            raf.seek(WAV_HEADER_BYTES)

            val byteBufSize = 8192
            val pcmBytes = ByteArray(byteBufSize)
            val samples = ShortArray(byteBufSize / 2)
            val mp3Buf = ByteArray((byteBufSize / 2 * 1.25).toInt() + 7200)

            BufferedOutputStream(FileOutputStream(outFile)).use { out ->
                while (true) {
                    val read = raf.read(pcmBytes)
                    if (read <= 0) break
                    val sampleCount = read / 2
                    ByteBuffer.wrap(pcmBytes, 0, read)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()
                        .get(samples, 0, sampleCount)

                    val encoded = if (channels == 1) {
                        // Mono: LAME reads the left buffer only.
                        lame.encode(samples, samples, sampleCount, mp3Buf)
                    } else {
                        lame.encodeBufferInterLeaved(samples, sampleCount / 2, mp3Buf)
                    }
                    if (encoded > 0) out.write(mp3Buf, 0, encoded)
                }
                val flushed = lame.flush(mp3Buf)
                if (flushed > 0) out.write(mp3Buf, 0, flushed)
                out.flush()
            }
            true
        } catch (t: Throwable) {
            outFile.delete()
            false
        } finally {
            runCatching { lame.close() }
            runCatching { raf.close() }
        }
    }
}
