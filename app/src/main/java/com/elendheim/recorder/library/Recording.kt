package com.elendheim.recorder.library

/** One recording: a .wav file on disk plus the light metadata we show in the list. */
data class Recording(
    val id: String,            // stable key (the file name)
    val displayName: String,   // user-facing, editable
    val fileName: String,      // actual file on disk
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAt: Long
)
