package com.elendheim.recorder.ui

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elendheim.recorder.audio.RecordingController
import com.elendheim.recorder.audio.RecordingService
import com.elendheim.recorder.data.SettingsStore
import com.elendheim.recorder.export.ExportFormat
import com.elendheim.recorder.export.Exporter
import com.elendheim.recorder.library.Recording
import com.elendheim.recorder.library.RecordingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlaybackState(
    val playingId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0
)

class RecorderViewModel(app: Application) : AndroidViewModel(app) {

    private val store = RecordingStore(app)
    private val settingsStore = SettingsStore.get(app)

    val recorderState = RecordingController.state
    val settings = settingsStore.settings

    private val _library = MutableStateFlow<List<Recording>>(emptyList())
    val library: StateFlow<List<Recording>> = _library.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackState())
    val playback: StateFlow<PlaybackState> = _playback.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var player: MediaPlayer? = null
    private var pollJob: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            RecordingController.finished.collect { refresh() }
        }
    }

    fun refresh() {
        _library.value = store.list()
        _folders.value = store.folders()
    }

    // --- Settings ---

    fun setHighContrast(value: Boolean) = settingsStore.setHighContrast(value)
    fun setShowPitch(value: Boolean) = settingsStore.setShowPitch(value)
    fun setMonitoring(value: Boolean) = settingsStore.setMonitoring(value)

    // --- Recording ---

    fun startRecording() = RecordingService.start(getApplication())

    fun stopRecording() = RecordingService.stop(getApplication())

    // --- Library edits ---

    fun rename(id: String, newName: String) {
        store.rename(id, newName)
        refresh()
    }

    fun delete(id: String) {
        if (_playback.value.playingId == id) stopPlayback()
        store.delete(id)
        refresh()
    }

    fun moveToFolder(id: String, folder: String) {
        store.moveToFolder(id, folder)
        refresh()
    }

    fun createFolder(name: String) {
        store.createFolder(name)
        refresh()
    }

    fun renameFolder(oldName: String, newName: String) {
        store.renameFolder(oldName, newName)
        refresh()
    }

    fun deleteFolder(name: String) {
        store.deleteFolder(name)
        refresh()
    }

    // --- Playback ---

    fun togglePlay(recording: Recording) {
        val current = _playback.value
        val existing = player
        if (current.playingId == recording.id && existing != null) {
            if (existing.isPlaying) {
                existing.pause()
                stopPolling()
                _playback.value = current.copy(isPlaying = false)
            } else {
                existing.start()
                _playback.value = current.copy(isPlaying = true)
                startPolling()
            }
            return
        }

        stopPlayback()
        val file = store.fileFor(recording)
        val p = MediaPlayer()
        runCatching {
            p.setDataSource(file.absolutePath)
            p.prepare()
        }.onFailure {
            p.release()
            _messages.tryEmit("Could not play recording")
            return
        }
        p.setOnCompletionListener { onPlaybackComplete() }
        player = p
        p.start()
        _playback.value = PlaybackState(recording.id, true, 0, p.duration)
        startPolling()
    }

    fun seekTo(ms: Int) {
        player?.seekTo(ms)
        _playback.value = _playback.value.copy(positionMs = ms)
    }

    fun stopPlayback() {
        stopPolling()
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
        _playback.value = PlaybackState()
    }

    private fun onPlaybackComplete() {
        stopPolling()
        _playback.value = _playback.value.copy(isPlaying = false, positionMs = 0)
    }

    private fun startPolling() {
        stopPolling()
        pollJob = viewModelScope.launch {
            while (isActive) {
                val p = player ?: break
                if (p.isPlaying) {
                    _playback.value = _playback.value.copy(positionMs = p.currentPosition)
                }
                delay(200)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    // --- Export ---

    fun suggestedFileName(recording: Recording, format: ExportFormat): String =
        Exporter.suggestedFileName(recording, format)

    fun exportToUri(uri: Uri, recording: Recording, format: ExportFormat) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                Exporter.writeToUri(getApplication(), store, recording, format, uri)
            }
            _messages.tryEmit(if (ok) "Exported as ${format.label}" else "Export failed")
        }
    }

    fun share(recording: Recording, format: ExportFormat) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                Exporter.share(getApplication(), store, recording, format)
            }
            if (!ok) _messages.tryEmit("Could not prepare file to share")
        }
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }
}
