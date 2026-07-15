package com.elendheim.recorder.ui.theme

import androidx.compose.ui.graphics.Color

// Standard palette. Nudged a touch brighter than the original so text reads
// well on dark gray by default, leaving real headroom for the high-contrast
// mode to go further.
val RecorderBackground = Color(0xFF2B2B2B)   // dark gray (matches brand)
val RecorderSurface = Color(0xFF363636)      // slightly lifted panels
val RecorderAccent = Color(0xFFE57373)       // soft red (record button, highlights)
val RecorderText = Color(0xFFF2F2F2)         // near-white body text on dark
val RecorderTextDim = Color(0xFFAEAEAE)      // secondary text

// High-contrast palette. Pure black + pure white with a brighter red, so the
// accessibility toggle makes a real, visible difference.
val HcBackground = Color(0xFF000000)
val HcSurface = Color(0xFF1A1A1A)
val HcAccent = Color(0xFFFF8A80)
val HcText = Color(0xFFFFFFFF)
val HcTextDim = Color(0xFFE0E0E0)
val HcOutline = Color(0xFFFFFFFF)
