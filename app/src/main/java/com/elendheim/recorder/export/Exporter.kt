package com.elendheim.recorder.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.elendheim.recorder.library.Recording
import com.elendheim.recorder.library.RecordingStore
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The two formats a recording can leave the app as. */
enum class ExportFormat(val ext: String, val mime: String) {
    WAV("wav", "audio/wav"),
    M4A("m4a", "audio/mp4")
}

/**
 * Moves recordings out of the private library and into the wider world:
 * saved to the phone's Music folder, or handed to another app via the share
 * sheet. The library itself never changes; export always copies out.
 */
object Exporter {

    /**
     * Save into the device's public Music collection (visible to files/music
     * apps). Returns a human-readable destination on success, or null on failure.
     */
    fun saveToMusic(context: Context, store: RecordingStore, recording: Recording, format: ExportFormat): String? {
        val source = prepareExportFile(context, store, recording, format) ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, source, recording.displayName, format)
            } else {
                saveToAppMusicDir(context, source, recording.displayName, format)
            }
        } finally {
            if (source.parentFile == context.cacheDir || source.name.startsWith("export_")) source.delete()
        }
    }

    /** Open the Android share sheet with the recording in the chosen format. */
    fun share(context: Context, store: RecordingStore, recording: Recording, format: ExportFormat): Boolean {
        val source = prepareExportFile(context, store, recording, format) ?: return false
        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val shareFile = File(sharedDir, "${safeName(recording.displayName)}.${format.ext}")
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

    /** Produce the actual file to export: the WAV itself, or a freshly encoded M4A. */
    private fun prepareExportFile(context: Context, store: RecordingStore, recording: Recording, format: ExportFormat): File? {
        val wav = store.fileFor(recording)
        if (!wav.exists()) return null
        return when (format) {
            ExportFormat.WAV -> wav
            ExportFormat.M4A -> {
                val (sampleRate, channels) = readWavFormat(wav)
                val out = File(context.cacheDir, "export_${recording.fileName}.m4a")
                if (AacEncoder.encode(wav, out, sampleRate, channels)) out else null
            }
        }
    }

    private fun saveViaMediaStore(context: Context, source: File, displayName: String, format: ExportFormat): String? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "${safeName(displayName)}.${format.ext}")
            put(MediaStore.Audio.Media.MIME_TYPE, format.mime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Elendheim Recorder")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } } ?: return null
        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return "Music/Elendheim Recorder"
    }

    private fun saveToAppMusicDir(context: Context, source: File, displayName: String, format: ExportFormat): String {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        val dest = File(dir, "${safeName(displayName)}.${format.ext}")
        source.copyTo(dest, overwrite = true)
        return dest.absolutePath
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
