package com.elendheim.branding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Drop-in Elendheim splash. A bold white wordmark with an optional coloured
 * sub-word beneath it. Copy the `branding` package into any app and call:
 *
 *   var showSplash by remember { mutableStateOf(true) }
 *   if (showSplash) ElendheimSplash(subWord = "Recorder") { showSplash = false }
 *   else AppContent()
 *
 * Kept short on purpose: the whole thing runs in well under a second so it
 * greets you without getting in the way.
 */
@Composable
fun ElendheimSplash(
    subWord: String? = null,
    subColor: Color = Color(0xFFE57373),
    holdMillis: Int = 160,
    fadeMillis: Int = 120,
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
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = ElendheimBrand.WordMark,
                color = Color.White,
                fontSize = ElendheimBrand.WordSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alpha)
            )
            if (subWord != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subWord,
                    color = subColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 6.sp,
                    modifier = Modifier.alpha(alpha)
                )
            }
        }
    }
}
