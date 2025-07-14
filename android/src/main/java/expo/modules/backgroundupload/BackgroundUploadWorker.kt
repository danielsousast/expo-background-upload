package expo.modules.backgroundupload

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.modules.Module
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class BackgroundUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_FILE_URI = "file_uri"
        const val KEY_URL = "url"
        const val KEY_HEADERS = "headers"
        const val KEY_FIELD_NAME = "field_name"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_CONTENT_TYPE = "content_type"
        const val KEY_UPLOAD_ID = "upload_id"
        const val KEY_MODULE_NAME = "module_name"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val fileUri = inputData.getString(KEY_FILE_URI) ?: return@withContext Result.failure()
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val uploadId = inputData.getString(KEY_UPLOAD_ID) ?: return@withContext Result.failure()
        val moduleName = inputData.getString(KEY_MODULE_NAME) ?: return@withContext Result.failure()
        val headers = inputData.getString(KEY_HEADERS) ?: "{}"
        val fieldName = inputData.getString(KEY_FIELD_NAME) ?: "file"
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "upload"
        val contentType = inputData.getString(KEY_CONTENT_TYPE) ?: "application/octet-stream"

        try {
            val file = getFileFromUri(fileUri)
            if (file == null || !file.exists()) {
                sendCompleteEvent(uploadId, false, "File not found", 0, moduleName)
                return@withContext Result.failure()
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val requestBody = createMultipartBody(file, fieldName, fileName, contentType, uploadId, moduleName)
            val request = createRequest(url, headers, requestBody)

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            sendCompleteEvent(
                uploadId,
                response.isSuccessful,
                if (response.isSuccessful) responseBody else "Upload failed",
                response.code,
                moduleName
            )

            if (response.isSuccessful) {
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            sendCompleteEvent(uploadId, false, e.message ?: "Unknown error", 0, moduleName)
            Result.failure()
        }
    }

    private fun getFileFromUri(uriString: String): File? {
        return try {
            val uri = Uri.parse(uriString)
            when (uri.scheme) {
                "file" -> File(uri.path!!)
                "content" -> {
                    // Para content URIs, precisaríamos copiar para um arquivo temporário
                    // Por simplicidade, assumindo file URIs por enquanto
                    null
                }
                else -> File(uriString)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createMultipartBody(
        file: File,
        fieldName: String,
        fileName: String,
        contentType: String,
        uploadId: String,
        moduleName: String
    ): RequestBody {
        val mediaType = contentType.toMediaType()
        val fileBody = ProgressRequestBody(file.asRequestBody(mediaType), file.length()) { bytesUploaded, totalBytes ->
            val progress = if (totalBytes > 0) bytesUploaded.toFloat() / totalBytes.toFloat() else 0f
            sendProgressEvent(uploadId, progress, bytesUploaded, totalBytes, moduleName)
        }
        
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(fieldName, fileName, fileBody)
            .build()
    }

    private fun createRequest(url: String, headersJson: String, body: RequestBody): Request {
        val builder = Request.Builder()
            .url(url)
            .post(body)

        // Parse headers JSON (simplified)
        try {
            if (headersJson != "{}") {
                // Aqui você poderia usar uma biblioteca JSON como Gson
                // Por simplicidade, assumindo headers básicos
            }
        } catch (e: Exception) {
            // Ignore header parsing errors
        }

        return builder.build()
    }

    private fun sendProgressEvent(uploadId: String, progress: Float, bytesUploaded: Long, totalBytes: Long, moduleName: String) {
        val data = workDataOf(
            "type" to "progress",
            "uploadId" to uploadId,
            "progress" to progress,
            "bytesUploaded" to bytesUploaded,
            "totalBytes" to totalBytes
        )
        setProgressAsync(data)
    }

    private fun sendCompleteEvent(uploadId: String, success: Boolean, response: String, statusCode: Int, moduleName: String) {
        val data = workDataOf(
            "type" to "complete",
            "uploadId" to uploadId,
            "success" to success,
            "response" to response,
            "statusCode" to statusCode
        )
        setProgressAsync(data)
    }
}