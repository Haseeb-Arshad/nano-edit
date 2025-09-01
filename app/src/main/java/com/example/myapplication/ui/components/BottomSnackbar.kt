package com.example.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.motion.Motion

@Composable
fun BottomSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("snackbarHost"),
        snackbar = { data ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(animationSpec = tween(Motion.Durations.Medium)) { it },
                exit = slideOutVertically(animationSpec = tween(Motion.Durations.Medium)) { it }
            ) { Snackbar(data) }
        }
    )
}

class StackedSnackbarHostState(val capacity: Int = 2) {
    private val _items = mutableStateListOf<SnackbarVisuals>()
    val items: List<SnackbarVisuals> get() = _items
    suspend fun show(message: String, action: String? = null, duration: SnackbarDuration = SnackbarDuration.Short) {
        if (_items.size >= capacity) _items.removeFirst()
        _items += object : SnackbarVisuals {
            override val actionLabel: String? = action
            override val duration: SnackbarDuration = duration
            override val message: String = message
            override val withDismissAction: Boolean = action == null
        }
    }
    fun dismissFirst() { if (_items.isNotEmpty()) _items.removeFirst() }
}

@Composable
fun StackedBottomSnackbars(
    state: StackedSnackbarHostState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        state.items.forEachIndexed { index, visuals ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(animationSpec = tween(Motion.Durations.Medium)) { it },
                exit = slideOutVertically(animationSpec = tween(Motion.Durations.Medium)) { it }
            ) {
                Snackbar(
                    modifier = Modifier.fillMaxWidth(),
                    action = null,
                    dismissAction = null,
                    actionOnNewLine = false
                ) { androidx.compose.material3.Text(visuals.message) }
            }
        }
    }
}

