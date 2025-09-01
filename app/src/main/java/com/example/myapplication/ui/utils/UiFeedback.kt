package com.example.myapplication.ui.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun clickFeedback() {
    LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

@Composable
fun successFeedback() {
    LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun errorFeedback() {
    LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)
    vibrate(30)
}

@Composable
private fun vibrate(durationMs: Long) {
    val ctx = LocalContext.current
    val vib = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (android.os.Build.VERSION.SDK_INT >= 26) {
        vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.EFFECT_HEAVY_CLICK))
    } else {
        @Suppress("DEPRECATION")
        vib.vibrate(durationMs)
    }
}

