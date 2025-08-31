package com.example.myapplication.controller

import android.net.Uri
import com.example.myapplication.data.EditUiState
import com.example.myapplication.data.EditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class EditController @Inject constructor(
    private val repo: EditRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state

    fun setSource(uri: Uri) {
        _state.update { it.copy(sourceUri = uri) }
    }

    fun setPrompt(text: String) {
        _state.update { it.copy(prompt = text) }
    }

    fun applyEdit(offline: Boolean = false) {
        val src = state.value.sourceUri ?: return
        val prompt = state.value.prompt
        _state.update { it.copy(isLoading = true, error = null, resultUrl = null) }
        scope.launch {
            runCatching {
                if (offline) {
                    // Offline: pretend to transform and echo the original URI
                    _state.update { it.copy(isLoading = false, resultUrl = src.toString()) }
                } else {
                    val jobId = repo.submitEdit(src.toString(), prompt)
                    var result = repo.pollResult(jobId)
                    // Poll with a small delay to avoid busy loop
                    while (!result.isTerminal) {
                        kotlinx.coroutines.delay(1000)
                        result = repo.pollResult(jobId)
                    }
                    if (result.url != null) {
                        _state.update { it.copy(isLoading = false, resultUrl = result.url) }
                    } else {
                        _state.update { it.copy(isLoading = false, error = result.error ?: "Unknown error") }
                    }
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
