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

    /**
     * A lowercase bag of date tokens for searching: full date, day, month
     * number, month names, year, weekday. So typing "07" finds July (and the
     * 7th), "july" finds July, "2026" finds the year.
     */
    fun searchDateTokens(epochMs: Long): String {
        val d = Date(epochMs)
        val patterns = listOf("yyyy-MM-dd", "d", "dd", "MM", "MMM", "MMMM", "yyyy", "EEEE")
        return patterns.joinToString(" ") {
            SimpleDateFormat(it, Locale.getDefault()).format(d)
        }.lowercase(Locale.getDefault())
    }

    /** True if [query] matches the recording's name or any of its date tokens. */
    fun matches(displayName: String, createdAt: Long, query: String): Boolean {
        val q = query.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) return true
        if (displayName.lowercase(Locale.getDefault()).contains(q)) return true
        return searchDateTokens(createdAt).contains(q)
    }
}
