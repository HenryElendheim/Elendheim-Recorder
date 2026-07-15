package com.elendheim.recorder.audio

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live state of the current (or idle) recording, driven by the service. */
data class RecorderState(
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitude: Float = 0f,   // normalised 0..1 for the level meter
    val pitch: String? = null    // e.g. "C4", when pitch display is enabled
)

/**
 * A tiny shared bridge between the recording service and the UI. The service
 * owns the work and pushes state here; the UI only reads. Keeping this as the
 * single meeting point is what lets the audio engine stay UI-free.
 */
object RecordingController {

    private val _state = MutableStateFlow(RecorderState())
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    // Fired once when a take is finalised, so the library can refresh itself.
    private val _finished = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val finished: SharedFlow<Unit> = _finished.asSharedFlow()

    fun update(elapsedMs: Long, amplitude: Float, pitch: String?) {
        _state.value = RecorderState(
            isRecording = true,
            elapsedMs = elapsedMs,
            amplitude = amplitude,
            pitch = pitch
        )
    }

    fun markIdle() {
        _state.value = RecorderState()
    }

    fun notifyFinished() {
        _finished.tryEmit(Unit)
    }
}
