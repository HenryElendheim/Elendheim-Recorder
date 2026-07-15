package com.elendheim.recorder.library

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * The recordings library, kept in app-private storage where we have full
 * control. That is what makes rename, delete, and folders simple and reliable:
 * no MediaStore friction here (exports copy files out to public storage).
 *
 * Each recording is a .wav file. Editable metadata (display name, duration,
 * created time, folder) lives in a small JSON index alongside the files.
 * Folders are just labels — files stay flat on disk — which keeps file paths
 * stable and moving a recording instant.
 */
class RecordingStore(context: Context) {

    private val dir: File = File(context.filesDir, "recordings").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")

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
                    displayName = meta?.optString("displayName")?.ifEmpty { defaultName(file.name) }
                        ?: defaultName(file.name),
                    fileName = file.name,
                    durationMs = meta?.optLong("durationMs") ?: 0L,
                    sizeBytes = file.length(),
                    createdAt = meta?.optLong("createdAt") ?: file.lastModified(),
                    folder = meta?.optString("folder") ?: ""
                )
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    /** Folder names, sorted. Includes empty folders the user created. */
    fun folders(): List<String> {
        val index = readIndex()
        val names = sortedSetOf<String>(String.CASE_INSENSITIVE_ORDER)
        val stored = index.optJSONArray(KEY_FOLDERS)
        if (stored != null) {
            for (i in 0 until stored.length()) stored.optString(i).takeIf { it.isNotEmpty() }?.let(names::add)
        }
        // Also surface any folder a recording references, in case it drifted.
        index.keys().forEach { key ->
            if (key != KEY_FOLDERS) {
                index.optJSONObject(key)?.optString("folder")?.takeIf { it.isNotEmpty() }?.let(names::add)
            }
        }
        return names.toList()
    }

    /** Register a freshly finalised recording in the index. */
    fun add(fileName: String, displayName: String, durationMs: Long, createdAt: Long) {
        val index = readIndex()
        index.put(fileName, JSONObject().apply {
            put("displayName", displayName)
            put("durationMs", durationMs)
            put("createdAt", createdAt)
            put("folder", "")
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

    /** Move a recording into [folder] ("" for the top level). */
    fun moveToFolder(id: String, folder: String) {
        val index = readIndex()
        val meta = index.optJSONObject(id) ?: JSONObject().also { index.put(id, it) }
        meta.put("folder", folder.trim())
        writeIndex(index)
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val index = readIndex()
        val list = index.optJSONArray(KEY_FOLDERS) ?: JSONArray()
        if (!containsIgnoreCase(list, trimmed)) {
            list.put(trimmed)
            index.put(KEY_FOLDERS, list)
            writeIndex(index)
        }
    }

    fun renameFolder(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val index = readIndex()
        val list = index.optJSONArray(KEY_FOLDERS) ?: JSONArray()
        val updated = JSONArray()
        for (i in 0 until list.length()) {
            val v = list.optString(i)
            updated.put(if (v.equals(oldName, ignoreCase = true)) trimmed else v)
        }
        index.put(KEY_FOLDERS, updated)
        index.keys().forEach { key ->
            if (key != KEY_FOLDERS) {
                val meta = index.optJSONObject(key)
                if (meta?.optString("folder").equals(oldName, ignoreCase = true)) {
                    meta?.put("folder", trimmed)
                }
            }
        }
        writeIndex(index)
    }

    /** Delete a folder; its recordings move back to the top level. */
    fun deleteFolder(name: String) {
        val index = readIndex()
        val list = index.optJSONArray(KEY_FOLDERS) ?: JSONArray()
        val updated = JSONArray()
        for (i in 0 until list.length()) {
            val v = list.optString(i)
            if (!v.equals(name, ignoreCase = true)) updated.put(v)
        }
        index.put(KEY_FOLDERS, updated)
        index.keys().forEach { key ->
            if (key != KEY_FOLDERS) {
                val meta = index.optJSONObject(key)
                if (meta?.optString("folder").equals(name, ignoreCase = true)) {
                    meta?.put("folder", "")
                }
            }
        }
        writeIndex(index)
    }

    private fun containsIgnoreCase(array: JSONArray, value: String): Boolean {
        for (i in 0 until array.length()) {
            if (array.optString(i).equals(value, ignoreCase = true)) return true
        }
        return false
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

    companion object {
        private const val KEY_FOLDERS = "_folders"
    }
}
