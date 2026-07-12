package com.elendheim.branding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/**
 * Drop-in Elendheim splash. Copy the `branding` package into any app and call:
 *
 *   var showSplash by remember { mutableStateOf(true) }
 *   if (showSplash) ElendheimSplash { showSplash = false }
 *   else AppContent()
 *
 * Nothing app-specific lives inside, which is what keeps it portable: one
 * palette file, one Composable, one callback.
 */
@Composable
fun ElendheimSplash(
    holdMillis: Int = 1400,
    fadeMillis: Int = 700,
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(fadeMillis),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        visible = true                       // fade in
        delay((fadeMillis + holdMillis).toLong())
        visible = false                      // fade out
        delay(fadeMillis.toLong())
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ElendheimBrand.Background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ElendheimBrand.WordMark,
            color = ElendheimBrand.SoftRed,
            fontSize = ElendheimBrand.WordSize,
            fontWeight = FontWeight.Light,
            letterSpacing = ElendheimBrand.WordSpacing,
            modifier = Modifier.alpha(alpha)
        )
    }
}
