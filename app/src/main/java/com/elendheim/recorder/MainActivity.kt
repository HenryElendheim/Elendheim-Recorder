package com.elendheim.recorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.elendheim.branding.ElendheimSplash
import com.elendheim.recorder.ui.RecorderApp
import com.elendheim.recorder.ui.theme.ElendheimRecorderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElendheimRecorderTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    ElendheimSplash { showSplash = false }
                } else {
                    RecorderApp()
                }
            }
        }
    }
}
