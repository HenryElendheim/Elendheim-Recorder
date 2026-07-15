package com.elendheim.recorder.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.elendheim.recorder.library.Recording
import com.elendheim.recorder.library.RecordingStore
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The formats a recording can leave the app as. */
enum class ExportFormat(val ext: String, val mime: String, val label: String) {
    WAV("wav", "audio/wav", "WAV"),
    MP3("mp3", "audio/mpeg", "MP3")
}

/**
 * Moves recordings out of the private library and into the wider world: saved
 * to a location the user picks (Files) or handed to another app via the share
 * sheet. The library itself never changes; export always copies out.
 */
object Exporter {

    fun suggestedFileName(recording: Recording, format: ExportFormat): String =
        "${safeName(recording.displayName)}.${format.ext}"

    /** Write the recording, in the chosen format, into a document [uri] the user picked. */
    fun writeToUri(
        context: Context,
        store: RecordingStore,
        recording: Recording,
        format: ExportFormat,
        uri: Uri
    ): Boolean {
        val source = prepareExportFile(context, store, recording, format) ?: return false
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
                true
            } ?: false
        } catch (t: Throwable) {
            false
        } finally {
            if (source.name.startsWith("export_")) source.delete()
        }
    }

    /** Open the Android share sheet with the recording in the chosen format. */
    fun share(context: Context, store: RecordingStore, recording: Recording, format: ExportFormat): Boolean {
        val source = prepareExportFile(context, store, recording, format) ?: return false
        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val shareFile = File(sharedDir, suggestedFileName(recording, format))
        source.copyTo(shareFile, overwrite = true)
        if (source.name.startsWith("export_")) source.delete()

        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", shareFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = format.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    /** Produce the file to export: the WAV itself, or a freshly encoded MP3. */
    private fun prepareExportFile(context: Context, store: RecordingStore, recording: Recording, format: ExportFormat): File? {
        val wav = store.fileFor(recording)
        if (!wav.exists()) return null
        return when (format) {
            ExportFormat.WAV -> wav
            ExportFormat.MP3 -> {
                val (sampleRate, channels) = readWavFormat(wav)
                val out = File(context.cacheDir, "export_${recording.fileName}.mp3")
                if (Mp3Encoder.encode(wav, out, sampleRate, channels)) out else null
            }
        }
    }

    /** Read sample rate and channel count straight from the WAV header. */
    private fun readWavFormat(wav: File): Pair<Int, Int> {
        RandomAccessFile(wav, "r").use { raf ->
            val header = ByteArray(44)
            raf.readFully(header)
            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val channels = bb.getShort(22).toInt()
            val sampleRate = bb.getInt(24)
            val safeChannels = if (channels in 1..2) channels else 1
            val safeRate = if (sampleRate in 8000..192000) sampleRate else 44100
            return safeRate to safeChannels
        }
    }

    private fun safeName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifEmpty { "recording" }
}
