package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.PreferencesRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val repo = remember { PreferencesRepository(ctx) }
    val scope = rememberCoroutineScope()
    val offline by repo.offlineFlow.collectAsState(initial = false)
    val quality by repo.qualityFlow.collectAsState(initial = 90)
    val watermark by repo.watermarkFlow.collectAsState(initial = false)

    Column(Modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Offline mode", modifier = Modifier.weight(1f))
            Switch(checked = offline, onCheckedChange = { checked -> scope.launch { repo.setOffline(checked) } })
        }
        Divider(Modifier.padding(vertical = 12.dp))
        Text("Default quality: $quality")
        Slider(
            value = quality.toFloat(),
            onValueChange = { v -> scope.launch { repo.setQuality(v.toInt()) } },
            valueRange = 50f..100f
        )
        Divider(Modifier.padding(vertical = 12.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Watermark edits", modifier = Modifier.weight(1f))
            Switch(checked = watermark, onCheckedChange = { checked -> scope.launch { repo.setWatermark(checked) } })
        }
    }
}

