package com.example.myapplication.data

import com.example.myapplication.network.EditJobDto
import com.example.myapplication.network.EditRequestDto
import com.example.myapplication.network.NanoBananaApi
import javax.inject.Inject

data class EditResult(val isTerminal: Boolean, val url: String? = null, val error: String? = null)

class EditRepository @Inject constructor(
    private val api: NanoBananaApi
) {
    suspend fun submitEdit(imageUrl: String, prompt: String): String {
        return api.createEdit(EditRequestDto(imageUrl = imageUrl, prompt = prompt)).id
    }

    suspend fun pollResult(id: String): EditResult {
        val dto: EditJobDto = api.getJob(id)
        return when (dto.status.lowercase()) {
            "done" -> EditResult(true, url = dto.resultUrl)
            "failed" -> EditResult(true, error = dto.error ?: "Failed")
            else -> EditResult(false)
        }
    }
}

