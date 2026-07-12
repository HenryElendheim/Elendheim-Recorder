package com.elendheim.recorder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.recorder.ui.theme.RecorderAccent
import com.elendheim.recorder.ui.theme.RecorderSurface
import com.elendheim.recorder.ui.theme.RecorderTextDim

@Composable
fun RecorderApp(viewModel: RecorderViewModel = viewModel()) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val recorderState by viewModel.recorderState.collectAsState()
    val library by viewModel.library.collectAsState()
    val playback by viewModel.playback.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.startRecording()
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    fun onToggleRecord() {
        if (recorderState.isRecording) {
            viewModel.stopRecording()
            return
        }
        val needed = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            viewModel.startRecording()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = RecorderSurface) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                    label = { Text("Record") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                    label = { Text("Library") },
                    colors = navColors()
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> RecordScreen(
                state = recorderState,
                onToggleRecord = { onToggleRecord() },
                modifier = Modifier.padding(padding)
            )
            else -> LibraryScreen(
                recordings = library,
                playback = playback,
                onTogglePlay = viewModel::togglePlay,
                onSeek = viewModel::seekTo,
                onRename = { rec, name -> viewModel.rename(rec.id, name) },
                onDelete = { rec -> viewModel.delete(rec.id) },
                onExport = viewModel::exportToMusic,
                onShare = viewModel::share,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = RecorderAccent,
    selectedTextColor = RecorderAccent,
    unselectedIconColor = RecorderTextDim,
    unselectedTextColor = RecorderTextDim,
    indicatorColor = RecorderSurface
)
