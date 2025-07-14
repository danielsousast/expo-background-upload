package expo.modules.backgroundupload

import android.content.Context
import androidx.work.*
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class BackgroundUploadModule : Module() {
  private val workManager by lazy { WorkManager.getInstance(appContext.reactContext!!) }
  private val activeUploads = mutableMapOf<String, UUID>()

  override fun definition() = ModuleDefinition {
    Name("BackgroundUpload")

    Events("onUploadProgress", "onUploadComplete")

    AsyncFunction("startUploadAsync") { fileUri: String, options: Map<String, Any>, promise: Promise ->
      try {
        val uploadId = UUID.randomUUID().toString()
        val url = options["url"] as? String ?: throw Exception("URL is required")
        val headers = options["headers"] as? Map<String, String> ?: emptyMap()
        val fieldName = options["fieldName"] as? String ?: "file"
        val fileName = options["fileName"] as? String ?: "upload"
        val contentType = options["contentType"] as? String ?: "application/octet-stream"

        val workData = Data.Builder()
          .putString(BackgroundUploadWorker.KEY_FILE_URI, fileUri)
          .putString(BackgroundUploadWorker.KEY_URL, url)
          .putString(BackgroundUploadWorker.KEY_HEADERS, headersToJson(headers))
          .putString(BackgroundUploadWorker.KEY_FIELD_NAME, fieldName)
          .putString(BackgroundUploadWorker.KEY_FILE_NAME, fileName)
          .putString(BackgroundUploadWorker.KEY_CONTENT_TYPE, contentType)
          .putString(BackgroundUploadWorker.KEY_UPLOAD_ID, uploadId)
          .putString(BackgroundUploadWorker.KEY_MODULE_NAME, name)
          .build()

        val uploadRequest = OneTimeWorkRequestBuilder<BackgroundUploadWorker>()
          .setInputData(workData)
          .setConstraints(
            Constraints.Builder()
              .setRequiredNetworkType(NetworkType.CONNECTED)
              .build()
          )
          .build()

        activeUploads[uploadId] = uploadRequest.id
        workManager.enqueue(uploadRequest)

        // Observar o progresso do trabalho
        observeWorkProgress(uploadRequest.id, uploadId)

        promise.resolve(uploadId)
      } catch (e: Exception) {
        promise.reject("UPLOAD_ERROR", e.message, e)
      }
    }

    Function("cancelUpload") { uploadId: String ->
      activeUploads[uploadId]?.let { workId ->
        workManager.cancelWorkById(workId)
        activeUploads.remove(uploadId)
        return@Function true
      }
      return@Function false
    }
  }

  private fun observeWorkProgress(workId: UUID, uploadId: String) {
    CoroutineScope(Dispatchers.Main).launch {
      workManager.getWorkInfoByIdLiveData(workId).observeForever { workInfo ->
        when (workInfo?.state) {
          WorkInfo.State.RUNNING -> {
            val progress = workInfo.progress
            val type = progress.getString("type")
            
            when (type) {
              "progress" -> {
                sendEvent("onUploadProgress", mapOf(
                  "uploadId" to uploadId,
                  "progress" to progress.getFloat("progress", 0f),
                  "bytesUploaded" to progress.getLong("bytesUploaded", 0L),
                  "totalBytes" to progress.getLong("totalBytes", 0L)
                ))
              }
              "complete" -> {
                sendEvent("onUploadComplete", mapOf(
                  "uploadId" to uploadId,
                  "success" to progress.getBoolean("success", false),
                  "response" to progress.getString("response"),
                  "statusCode" to progress.getInt("statusCode", 0)
                ))
                activeUploads.remove(uploadId)
              }
            }
          }
          WorkInfo.State.SUCCEEDED -> {
            activeUploads.remove(uploadId)
          }
          WorkInfo.State.FAILED -> {
            sendEvent("onUploadComplete", mapOf(
              "uploadId" to uploadId,
              "success" to false,
              "error" to "Upload failed",
              "statusCode" to 0
            ))
            activeUploads.remove(uploadId)
          }
          WorkInfo.State.CANCELLED -> {
            sendEvent("onUploadComplete", mapOf(
              "uploadId" to uploadId,
              "success" to false,
              "error" to "Upload cancelled",
              "statusCode" to 0
            ))
            activeUploads.remove(uploadId)
          }
          else -> {}
        }
      }
    }
  }

  private fun headersToJson(headers: Map<String, String>): String {
    if (headers.isEmpty()) return "{}"
    
    val jsonBuilder = StringBuilder("{")
    headers.entries.forEachIndexed { index, entry ->
      if (index > 0) jsonBuilder.append(",")
      jsonBuilder.append("\"${entry.key}\":\"${entry.value}\"")
    }
    jsonBuilder.append("}")
    return jsonBuilder.toString()
  }
}
