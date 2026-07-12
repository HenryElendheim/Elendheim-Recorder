package com.elendheim.branding

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * Every brand constant in one small file so the whole suite can share and
 * retune the look from one place. Copy the `branding` package into any
 * Elendheim app unchanged.
 */
object ElendheimBrand {
    val Background = Color(0xFF2B2B2B)   // dark gray
    val SoftRed = Color(0xFFE57373)      // soft red (tweak freely)
    val WordSize = 42.sp
    val WordSpacing = 4.sp
    const val WordMark = "Elendheim"
}
