package com.example.myapplication.ui.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

val LocalLowPerf: ProvidableCompositionLocal<Boolean> = compositionLocalOf { false }

@Composable
fun rememberIsLowPerf(): Boolean {
    val ctx = LocalContext.current
    val override = LocalLowPerf.current
    return remember(override) {
        if (override) true else detectLowPerf(ctx)
    }
}

fun detectLowPerf(context: Context): Boolean {
    // Heuristics: low API, <3GB RAM, or Go device
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    val isGo = am.isLowRamDevice
    val lowApi = Build.VERSION.SDK_INT < 29
    val lowMem = mem.totalMem < 3L * 1024 * 1024 * 1024
    return isGo || lowApi || lowMem
}

