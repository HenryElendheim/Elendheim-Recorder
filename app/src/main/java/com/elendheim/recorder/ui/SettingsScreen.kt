package com.elendheim.recorder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.recorder.data.AppSettings

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onHighContrast: (Boolean) -> Unit,
    onShowPitch: (Boolean) -> Unit,
    onPianoRoll: (Boolean) -> Unit,
    onMonitoring: (Boolean) -> Unit,
    onNamePrefix: (String) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onConfirmDelete: (Boolean) -> Unit,
    onResetNumbering: () -> Unit,
    appVersion: String,
    recordingCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SectionHeader("Recording")
        val nextName = "${settings.namePrefix.trim().ifEmpty { "Recording" }} ${settings.nameCounter}"
        OutlinedTextField(
            value = settings.namePrefix,
            onValueChange = onNamePrefix,
            singleLine = true,
            label = { Text("Recording name") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Next recording: $nextName",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onResetNumbering) {
                Text("Reset numbering", color = MaterialTheme.colorScheme.primary)
            }
        }
        SettingSwitch(
            title = "Keep screen on while recording",
            subtitle = "Stops the screen sleeping mid-take.",
            checked = settings.keepScreenOn,
            onCheckedChange = onKeepScreenOn
        )
        SettingSwitch(
            title = "Confirm before deleting",
            subtitle = "Ask first when removing a recording.",
            checked = settings.confirmDelete,
            onCheckedChange = onConfirmDelete
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("Accessibility")
        SettingSwitch(
            title = "High contrast",
            subtitle = "Pure black and white with a brighter accent, for easier reading.",
            checked = settings.highContrast,
            onCheckedChange = onHighContrast
        )
        SettingSwitch(
            title = "Show pitch",
            subtitle = "Displays the note you're hitting while recording and while playing back, like C4 or F#3.",
            checked = settings.showPitch,
            onCheckedChange = onShowPitch
        )
        SettingSwitch(
            title = "Piano roll while playing",
            subtitle = "Visualises a recording's notes over time, with a moving playhead.",
            checked = settings.pianoRoll,
            onCheckedChange = onPianoRoll
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("Monitoring")
        SettingSwitch(
            title = "Hear yourself",
            subtitle = "Plays your input back as you record. Best with headphones — speakers will feed back.",
            checked = settings.monitoring,
            onCheckedChange = onMonitoring
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("Export")
        Text(
            text = "When you export a recording you choose WAV or MP3 each time, then pick where it saves.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("About")
        Text(
            text = "Elendheim Recorder $appVersion",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (recordingCount == 1) "1 recording in your library" else "$recordingCount recordings in your library",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Text(
            text = "No cloud, no accounts, no network access. Your recordings stay on your phone.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(title, color = colors.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = colors.onSurfaceVariant, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.onSurfaceVariant,
                uncheckedTrackColor = colors.surface
            )
        )
    }
}
