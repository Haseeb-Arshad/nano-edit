package com.example.myapplication.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.example.myapplication.BuildConfig
import com.example.myapplication.network.ApiService
import com.example.myapplication.network.EditJobDto
import com.example.myapplication.network.EditRequestDto
import com.example.myapplication.network.NanoBananaApi
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class EditResult(val isTerminal: Boolean, val url: String? = null, val error: String? = null)

class EditRepository @Inject constructor(
    private val api: NanoBananaApi,
    private val backend: ApiService
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

    suspend fun uploadFullResAndPoll(
        context: Context,
        uri: Uri,
        prompt: String,
        onProgress: (Int) -> Unit = {}
    ): EditResult {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val filename = queryDisplayName(resolver, uri) ?: "image.jpg"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return EditResult(true, error = "Cannot open input stream")
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", filename, body)
        val promptBody: RequestBody = prompt.toRequestBody("text/plain".toMediaTypeOrNull())
        val bearer = "Bearer ${BuildConfig.NB_API_KEY}"

        val accepted = backend.uploadForEdit(filePart, promptBody, bearer)
        if (!accepted.isSuccessful) {
            return EditResult(true, error = "Upload failed: HTTP ${accepted.code()}")
        }
        val jobId = accepted.body()?.job_id ?: return EditResult(true, error = "Missing job id")

        // Poll status
        while (true) {
            delay(1000)
            val statusResp = backend.getEditStatus(jobId, bearer)
            if (!statusResp.isSuccessful) continue
            val s = statusResp.body() ?: continue
            s.progress?.let { onProgress(it) }
            when (s.status.lowercase()) {
                "done" -> return EditResult(true, url = s.result_url)
                "failed" -> return EditResult(true, error = s.error ?: "Failed")
            }
        }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = resolver.query(uri, null, null, null, null)
        cursor?.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && it.moveToFirst()) name = it.getString(idx)
        }
        return name
    }
}
