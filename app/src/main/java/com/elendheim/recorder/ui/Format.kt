package com.elendheim.recorder.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small display helpers shared by the screens. */
object Format {

    fun clock(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun clock(ms: Int): String = clock(ms.toLong())

    fun size(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.0f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.getDefault(), "%.1f MB", mb)
    }

    fun date(epochMs: Long): String =
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epochMs))
}
