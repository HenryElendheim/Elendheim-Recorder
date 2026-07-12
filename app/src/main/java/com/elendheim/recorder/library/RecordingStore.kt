package com.elendheim.recorder.library

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * The recordings library, kept in app-private storage where we have full
 * control. That is what makes rename and delete simple and reliable: no
 * MediaStore friction here (exports copy files out to public storage instead).
 *
 * Each recording is a .wav file. Editable metadata (display name, duration,
 * created time) lives in a small JSON index alongside the files.
 */
class RecordingStore(context: Context) {

    private val dir: File = File(context.filesDir, "recordings").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")

    /** Directory recordings are written to (shared with the FileProvider paths). */
    val recordingsDir: File get() = dir

    fun fileFor(recording: Recording): File = File(dir, recording.fileName)

    /** All recordings, newest first. */
    fun list(): List<Recording> {
        val index = readIndex()
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".wav") }
            ?.map { file ->
                val meta = index.optJSONObject(file.name)
                Recording(
                    id = file.name,
                    displayName = meta?.optString("displayName") ?: defaultName(file.name),
                    fileName = file.name,
                    durationMs = meta?.optLong("durationMs") ?: 0L,
                    sizeBytes = file.length(),
                    createdAt = meta?.optLong("createdAt") ?: file.lastModified()
                )
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    /** Register a freshly finalised recording in the index. */
    fun add(fileName: String, displayName: String, durationMs: Long, createdAt: Long) {
        val index = readIndex()
        index.put(fileName, JSONObject().apply {
            put("displayName", displayName)
            put("durationMs", durationMs)
            put("createdAt", createdAt)
        })
        writeIndex(index)
    }

    fun rename(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val index = readIndex()
        val meta = index.optJSONObject(id) ?: JSONObject().also { index.put(id, it) }
        meta.put("displayName", trimmed)
        writeIndex(index)
    }

    fun delete(id: String) {
        File(dir, id).delete()
        val index = readIndex()
        if (index.has(id)) {
            index.remove(id)
            writeIndex(index)
        }
    }

    private fun defaultName(fileName: String): String =
        fileName.removeSuffix(".wav").replace('_', ' ')

    private fun readIndex(): JSONObject =
        if (indexFile.exists()) {
            runCatching { JSONObject(indexFile.readText()) }.getOrDefault(JSONObject())
        } else {
            JSONObject()
        }

    private fun writeIndex(index: JSONObject) {
        indexFile.writeText(index.toString())
    }
}
