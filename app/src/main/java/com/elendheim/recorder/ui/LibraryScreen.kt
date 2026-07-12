package com.elendheim.recorder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.recorder.export.ExportFormat
import com.elendheim.recorder.library.Recording
import com.elendheim.recorder.ui.theme.RecorderAccent
import com.elendheim.recorder.ui.theme.RecorderSurface
import com.elendheim.recorder.ui.theme.RecorderTextDim

@Composable
fun LibraryScreen(
    recordings: List<Recording>,
    playback: PlaybackState,
    onTogglePlay: (Recording) -> Unit,
    onSeek: (Int) -> Unit,
    onRename: (Recording, String) -> Unit,
    onDelete: (Recording) -> Unit,
    onExport: (Recording, ExportFormat) -> Unit,
    onShare: (Recording, ExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var renameTarget by remember { mutableStateOf<Recording?>(null) }
    var deleteTarget by remember { mutableStateOf<Recording?>(null) }
    var exportTarget by remember { mutableStateOf<Recording?>(null) }
    var shareTarget by remember { mutableStateOf<Recording?>(null) }

    if (recordings.isEmpty()) {
        EmptyLibrary(modifier)
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
        ) {
            items(recordings, key = { it.id }) { recording ->
                RecordingRow(
                    recording = recording,
                    playback = playback,
                    onTogglePlay = { onTogglePlay(recording) },
                    onSeek = onSeek,
                    onRename = { renameTarget = recording },
                    onExport = { exportTarget = recording },
                    onShare = { shareTarget = recording },
                    onDelete = { deleteTarget = recording }
                )
            }
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            initial = target.displayName,
            onConfirm = { newName ->
                onRename(target, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete recording?") },
            text = { Text("This removes \"${target.displayName}\" from your library. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    deleteTarget = null
                }) { Text("Delete", color = RecorderAccent) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    exportTarget?.let { target ->
        FormatDialog(
            title = "Save to Music",
            onPick = { format ->
                onExport(target, format)
                exportTarget = null
            },
            onDismiss = { exportTarget = null }
        )
    }

    shareTarget?.let { target ->
        FormatDialog(
            title = "Share as",
            onPick = { format ->
                onShare(target, format)
                shareTarget = null
            },
            onDismiss = { shareTarget = null }
        )
    }
}

@Composable
private fun RecordingRow(
    recording: Recording,
    playback: PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isActive = playback.playingId == recording.id
    val isPlaying = isActive && playback.isPlaying

    Card(colors = CardDefaults.cardColors(containerColor = RecorderSurface)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = RecorderAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = recording.displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${Format.clock(recording.durationMs)}  ·  ${Format.date(recording.createdAt)}  ·  ${Format.size(recording.sizeBytes)}",
                        color = RecorderTextDim,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More actions",
                            tint = RecorderTextDim
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; onRename() })
                        DropdownMenuItem(text = { Text("Save to Music") }, onClick = { menuOpen = false; onExport() })
                        DropdownMenuItem(text = { Text("Share") }, onClick = { menuOpen = false; onShare() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
                    }
                }
            }

            if (isActive) {
                val duration = if (playback.durationMs > 0) playback.durationMs else recording.durationMs.toInt()
                Slider(
                    value = playback.positionMs.toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Format.clock(playback.positionMs), color = RecorderTextDim, fontSize = 11.sp)
                    Text(Format.clock(duration), color = RecorderTextDim, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename recording") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("Save", color = RecorderAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FormatDialog(title: String, onPick: (ExportFormat) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                FormatOption("WAV — lossless master", "Full quality, larger file") { onPick(ExportFormat.WAV) }
                Spacer(Modifier.height(8.dp))
                FormatOption("M4A — compact (AAC)", "Smaller file, great for sharing") { onPick(ExportFormat.M4A) }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun FormatOption(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = RecorderSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 12.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text(subtitle, color = RecorderTextDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No recordings yet",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap the record button to capture your first take.",
                color = RecorderTextDim,
                fontSize = 14.sp
            )
        }
    }
}
