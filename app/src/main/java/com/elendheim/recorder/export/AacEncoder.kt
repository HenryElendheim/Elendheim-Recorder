package com.elendheim.recorder.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

/**
 * Encodes a WAV master into a compact .m4a (AAC) using Android's built-in
 * MediaCodec / MediaMuxer. This is the native, no-NDK path for a small file.
 *
 * MP3 is intentionally not here: Android has no built-in MP3 encoder, so true
 * MP3 export means bundling LAME through the NDK. That is the next milestone;
 * M4A gives us a compact export today without shipping native code.
 */
object AacEncoder {

    private const val MIME = "audio/mp4a-latm"
    private const val TIMEOUT_US = 10_000L
    private const val WAV_HEADER_BYTES = 44L

    /** Returns true on success. Reads PCM from [wavFile] and writes [outFile]. */
    fun encode(
        wavFile: File,
        outFile: File,
        sampleRate: Int,
        channels: Int,
        bitRate: Int = 128_000
    ): Boolean {
        val format = MediaFormat.createAudioFormat(MIME, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }

        val codec = MediaCodec.createEncoderByType(MIME)
        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val pcm = RandomAccessFile(wavFile, "r")

        return try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            pcm.seek(WAV_HEADER_BYTES)

            val bytesPerFrame = 2 * channels
            val bufferInfo = MediaCodec.BufferInfo()
            var trackIndex = -1
            var muxerStarted = false
            var totalFramesSent = 0L
            var inputDone = false
            var outputDone = false
            val chunk = ByteArray(8 * 1024)

            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex) ?: ByteBuffer.allocate(0)
                        inBuf.clear()
                        val read = pcm.read(chunk, 0, minOf(chunk.size, inBuf.capacity()))
                        if (read <= 0) {
                            codec.queueInputBuffer(
                                inIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            inBuf.put(chunk, 0, read)
                            val ptsUs = totalFramesSent * 1_000_000L / sampleRate
                            codec.queueInputBuffer(inIndex, 0, read, ptsUs, 0)
                            totalFramesSent += read / bytesPerFrame
                        }
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                while (outIndex >= 0) {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0 && muxerStarted) {
                        val outBuf = codec.getOutputBuffer(outIndex) ?: ByteBuffer.allocate(0)
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, outBuf, bufferInfo)
                    }
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                }
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED && !muxerStarted) {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }
            true
        } catch (t: Throwable) {
            outFile.delete()
            false
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
            runCatching { muxer.stop() }
            runCatching { muxer.release() }
            runCatching { pcm.close() }
        }
    }
}
