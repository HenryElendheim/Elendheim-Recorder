package com.elendheim.recorder.audio

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Turns a raw PCM file into a valid .wav by prepending the standard 44-byte
 * header once the total length is known. The PCM samples are already
 * little-endian 16-bit (see [Recorder]), so the body is a straight copy.
 */
object WavWriter {

    fun writeWav(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int = 16
    ) {
        val dataLen = pcmFile.length().toInt()
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        BufferedOutputStream(FileOutputStream(wavFile)).use { out ->
            out.write(header(dataLen, sampleRate, channels, byteRate, blockAlign, bitsPerSample))
            pcmFile.inputStream().use { it.copyTo(out) }
            out.flush()
        }
    }

    private fun header(
        dataLen: Int,
        sampleRate: Int,
        channels: Int,
        byteRate: Int,
        blockAlign: Int,
        bitsPerSample: Int
    ): ByteArray {
        val buf = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray(Charsets.US_ASCII))
        buf.putInt(36 + dataLen)                    // chunk size
        buf.put("WAVE".toByteArray(Charsets.US_ASCII))
        buf.put("fmt ".toByteArray(Charsets.US_ASCII))
        buf.putInt(16)                              // subchunk1 size (PCM)
        buf.putShort(1)                             // audio format 1 = PCM
        buf.putShort(channels.toShort())
        buf.putInt(sampleRate)
        buf.putInt(byteRate)
        buf.putShort(blockAlign.toShort())
        buf.putShort(bitsPerSample.toShort())
        buf.put("data".toByteArray(Charsets.US_ASCII))
        buf.putInt(dataLen)
        return buf.array()
    }
}
