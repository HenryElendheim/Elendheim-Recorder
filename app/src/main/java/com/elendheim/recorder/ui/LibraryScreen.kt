package com.elendheim.recorder.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
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

private enum class SortMode { NEWEST, OLDEST, NAME }

@Composable
fun LibraryScreen(
    recordings: List<Recording>,
    folders: List<String>,
    playback: PlaybackState,
    onTogglePlay: (Recording) -> Unit,
    onSeek: (Int) -> Unit,
    onRename: (Recording, String) -> Unit,
    onDelete: (Recording) -> Unit,
    onMove: (Recording, String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onExport: (Recording, ExportFormat) -> Unit,
    onShare: (Recording, ExportFormat) -> Unit,
    confirmDelete: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var currentFolder by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.NEWEST) }
    var sortMenu by remember { mutableStateOf(false) }

    var renameTarget by remember { mutableStateOf<Recording?>(null) }
    var deleteTarget by remember { mutableStateOf<Recording?>(null) }
    var exportTarget by remember { mutableStateOf<Recording?>(null) }
    var shareTarget by remember { mutableStateOf<Recording?>(null) }
    var moveTarget by remember { mutableStateOf<Recording?>(null) }
    var newFolderDialog by remember { mutableStateOf(false) }
    var renameFolderTarget by remember { mutableStateOf<String?>(null) }
    var deleteFolderTarget by remember { mutableStateOf<String?>(null) }

    val searching = query.isNotBlank()
    val q = query.trim()

    // System back steps up a level (out of a folder or search) before leaving.
    BackHandler(enabled = searching || currentFolder != null) {
        if (searching) query = "" else currentFolder = null
    }

    val visibleFolders = if (searching) folders.filter { it.contains(q, ignoreCase = true) } else folders
    val filteredRecordings = when {
        searching -> recordings.filter { Format.matches(it.displayName, it.createdAt, q) }
        currentFolder == null -> recordings.filter { it.folder.isEmpty() }
        else -> recordings.filter { it.folder.equals(currentFolder, ignoreCase = true) }
    }
    val visibleRecordings = when (sortMode) {
        SortMode.NEWEST -> filteredRecordings.sortedByDescending { it.createdAt }
        SortMode.OLDEST -> filteredRecordings.sortedBy { it.createdAt }
        SortMode.NAME -> filteredRecordings.sortedBy { it.displayName.lowercase() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search name or date (e.g. July, 07)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )
            Box {
                IconButton(onClick = { sortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = colors.onSurfaceVariant)
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortItem("Newest first", sortMode == SortMode.NEWEST) { sortMode = SortMode.NEWEST; sortMenu = false }
                    SortItem("Oldest first", sortMode == SortMode.OLDEST) { sortMode = SortMode.OLDEST; sortMenu = false }
                    SortItem("Name (A–Z)", sortMode == SortMode.NAME) { sortMode = SortMode.NAME; sortMenu = false }
                }
            }
            IconButton(onClick = { newFolderDialog = true }) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = "New folder", tint = colors.primary)
            }
        }

        if (!searching && currentFolder != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentFolder = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to top level")
                }
                Text(
                    text = currentFolder ?: "",
                    color = colors.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Folder actions", tint = colors.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Rename folder") }, onClick = {
                            menu = false; renameFolderTarget = currentFolder
                        })
                        DropdownMenuItem(text = { Text("Delete folder") }, onClick = {
                            menu = false; deleteFolderTarget = currentFolder
                        })
                    }
                }
            }
        }

        if (visibleFolders.isEmpty() && visibleRecordings.isEmpty()) {
            EmptyState(searching)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                val showFolders = searching || currentFolder == null
                if (showFolders) {
                    items(visibleFolders, key = { "folder:$it" }) { folder ->
                        FolderRow(
                            name = folder,
                            count = recordings.count { it.folder.equals(folder, ignoreCase = true) },
                            onOpen = { query = ""; currentFolder = folder }
                        )
                    }
                }
                items(visibleRecordings, key = { it.id }) { recording ->
                    RecordingRow(
                        recording = recording,
                        playback = playback,
                        onTogglePlay = { onTogglePlay(recording) },
                        onSeek = onSeek,
                        onRename = { renameTarget = recording },
                        onMove = { moveTarget = recording },
                        onExport = { exportTarget = recording },
                        onShare = { shareTarget = recording },
                        onDelete = { if (confirmDelete) deleteTarget = recording else onDelete(recording) }
                    )
                }
            }
        }
    }

    // --- Dialogs ---

    renameTarget?.let { target ->
        TextInputDialog(
            title = "Rename recording",
            initial = target.displayName,
            confirmLabel = "Save",
            onConfirm = { onRename(target, it); renameTarget = null },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete recording?") },
            text = { Text("This removes \"${target.displayName}\" from your library. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(target); deleteTarget = null }) {
                    Text("Delete", color = colors.primary)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }

    exportTarget?.let { target ->
        FormatDialog(
            title = "Export to files",
            onPick = { onExport(target, it); exportTarget = null },
            onDismiss = { exportTarget = null }
        )
    }

    shareTarget?.let { target ->
        FormatDialog(
            title = "Share as",
            onPick = { onShare(target, it); shareTarget = null },
            onDismiss = { shareTarget = null }
        )
    }

    moveTarget?.let { target ->
        MoveDialog(
            folders = folders,
            current = target.folder,
            onPick = { onMove(target, it); moveTarget = null },
            onNewFolder = { moveTarget = null; newFolderDialog = true },
            onDismiss = { moveTarget = null }
        )
    }

    if (newFolderDialog) {
        TextInputDialog(
            title = "New folder",
            initial = "",
            confirmLabel = "Create",
            onConfirm = { onCreateFolder(it); newFolderDialog = false },
            onDismiss = { newFolderDialog = false }
        )
    }

    renameFolderTarget?.let { target ->
        TextInputDialog(
            title = "Rename folder",
            initial = target,
            confirmLabel = "Save",
            onConfirm = { onRenameFolder(target, it); currentFolder = it.trim(); renameFolderTarget = null },
            onDismiss = { renameFolderTarget = null }
        )
    }

    deleteFolderTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteFolderTarget = null },
            title = { Text("Delete folder?") },
            text = { Text("\"$target\" will be removed. Its recordings move back to the top level and are not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFolder(target); currentFolder = null; deleteFolderTarget = null
                }) { Text("Delete", color = colors.primary) }
            },
            dismissButton = { TextButton(onClick = { deleteFolderTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SortItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(if (selected) "•  $label" else label) },
        onClick = onClick
    )
}

@Composable
private fun FolderRow(name: String, count: Int, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onOpen, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.size(12.dp))
            Text(name, color = colors.onSurface, fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("$count", color = colors.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun RecordingRow(
    recording: Recording,
    playback: PlaybackState,
    onTogglePlay: () -> Unit,
    onSeek: (Int) -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val isActive = playback.playingId == recording.id
    val isPlaying = isActive && playback.isPlaying

    Card(
        onClick = onTogglePlay,
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = recording.displayName,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${Format.clock(recording.durationMs)}  ·  ${Format.date(recording.createdAt)}  ·  ${Format.size(recording.sizeBytes)}",
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More actions", tint = colors.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; onRename() })
                        DropdownMenuItem(text = { Text("Move to folder") }, onClick = { menuOpen = false; onMove() })
                        DropdownMenuItem(text = { Text("Export to files") }, onClick = { menuOpen = false; onExport() })
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
                    Text(Format.clock(playback.positionMs), color = colors.onSurfaceVariant, fontSize = 11.sp)
                    Text(Format.clock(duration), color = colors.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FormatDialog(title: String, onPick: (ExportFormat) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OptionRow("WAV — lossless master", "Full quality, larger file") { onPick(ExportFormat.WAV) }
                Spacer(Modifier.height(8.dp))
                OptionRow("MP3 — compact", "Smaller file, plays everywhere") { onPick(ExportFormat.MP3) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MoveDialog(
    folders: List<String>,
    current: String,
    onPick: (String) -> Unit,
    onNewFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            Column {
                OptionRow("Top level", if (current.isEmpty()) "Current" else "No folder") { onPick("") }
                folders.forEach { folder ->
                    Spacer(Modifier.height(6.dp))
                    OptionRow(folder, if (folder.equals(current, ignoreCase = true)) "Current" else "") { onPick(folder) }
                }
                Spacer(Modifier.height(6.dp))
                OptionRow("New folder…", "Create and move here") { onNewFolder() }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun OptionRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 12.dp)) {
            Text(title, color = colors.onSurface, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) Text(subtitle, color = colors.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (searching) "No matches" else "No recordings yet",
                color = colors.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (searching) "Try a different search." else "Tap the record button to capture your first take.",
                color = colors.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}
