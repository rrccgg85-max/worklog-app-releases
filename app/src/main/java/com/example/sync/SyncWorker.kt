package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.WorkLog
import com.example.data.WorkLogDatabase
import com.example.data.WorkLogRepository
import com.example.util.GoogleScriptNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Background worker that synchronizes pending local tasks with Google Sheets Webhook.
 * Runs automatically when network connectivity is restored.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = WorkLogDatabase.getDatabase(applicationContext)
        val repository = WorkLogRepository(database)
        val prefs = applicationContext.getSharedPreferences("worklog_prefs", Context.MODE_PRIVATE)
        val webhookUrl = prefs.getString("google_sheets_webhook_url", null)

        if (webhookUrl.isNullOrBlank() || webhookUrl.contains("placeholder") || !webhookUrl.startsWith("http")) {
            Log.d(TAG, "No valid Google Sheets Webhook URL set. Skipping background sync.")
            return@withContext Result.success()
        }

        val pendingLogs = repository.getPendingSyncWorkLogsList()
        if (pendingLogs.isEmpty()) {
            Log.d(TAG, "No pending logs to sync.")
            return@withContext Result.success()
        }

        Log.d(TAG, "Found ${pendingLogs.size} pending logs for background sync.")

        var allSynced = true

        for (workLog in pendingLogs) {
            try {
                val jsonPayload = JSONObject().apply {
                    val rawCaseNo = workLog.rawCaseNumber
                    val formattedCaseNo = workLog.formattedCaseNumber
                    val imgUrl = workLog.imageUri ?: ""
                    val isClosed = workLog.status == WorkLog.STATUS_CLOSED
                    val actionStr = if (isClosed) "close_case" else "open_case"
                    val statusThai = if (isClosed) "ปิดเคส" else "เปิดเคส"

                    put("action", actionStr)
                    put("type", actionStr)
                    put("Action", actionStr)
                    put("Type", actionStr)

                    put("status", workLog.status)
                    put("Status", workLog.status)
                    put("statusThai", statusThai)
                    put("state", workLog.status)

                    put("id", workLog.id)
                    put("CaseID", rawCaseNo)
                    put("caseId", rawCaseNo)
                    put("case_id", rawCaseNo)
                    put("Case_ID", rawCaseNo)
                    put("CaseId", rawCaseNo)
                    put("caseNumber", rawCaseNo)
                    put("case_number", rawCaseNo)
                    put("CaseNumber", rawCaseNo)
                    put("caseNo", formattedCaseNo)
                    put("ticketNo", rawCaseNo)

                    put("timestamp", workLog.timestamp)
                    put("Timestamp", workLog.timestamp)
                    put("date", workLog.timestamp)
                    put("Date", workLog.timestamp)

                    put("TaskName", workLog.category)
                    put("taskName", workLog.category)
                    put("task_name", workLog.category)
                    put("Task_Name", workLog.category)
                    put("title", workLog.category)
                    put("Title", workLog.category)

                    put("TaskDetail", workLog.rawText)
                    put("taskDetail", workLog.rawText)
                    put("task_detail", workLog.rawText)
                    put("Task_Detail", workLog.rawText)
                    put("rawText", workLog.rawText)
                    put("detail", workLog.rawText)
                    put("Detail", workLog.rawText)
                    put("description", workLog.rawText)
                    put("Description", workLog.rawText)

                    put("Category", workLog.category)
                    put("category", workLog.category)
                    put("categoryName", workLog.category)

                    // Image URL (For Column F "ImageUrl" / Open Case Image)
                    val imagesList = workLog.imageUriList
                    val openImgPath = if (imagesList.isNotEmpty()) imagesList[0] else ""
                    val closeImgPath = if (imagesList.size > 1) imagesList[1] else if (isClosed && imagesList.isNotEmpty()) imagesList[0] else ""

                    val openImgBase64 = com.example.util.CompressionUtils.convertUriOrPathToBase64(applicationContext, openImgPath)
                    val closeImgBase64 = com.example.util.CompressionUtils.convertUriOrPathToBase64(applicationContext, closeImgPath)

                    val openDataUri = com.example.util.CompressionUtils.getDataUri(applicationContext, openImgPath)
                    val closeDataUri = com.example.util.CompressionUtils.getDataUri(applicationContext, closeImgPath)

                    val openDisplayUrl = when {
                        openImgPath.startsWith("http://") || openImgPath.startsWith("https://") -> openImgPath
                        openDataUri.isNotBlank() -> openDataUri
                        else -> openImgPath
                    }

                    val closeDisplayUrl = when {
                        closeImgPath.startsWith("http://") || closeImgPath.startsWith("https://") -> closeImgPath
                        closeDataUri.isNotBlank() -> closeDataUri
                        else -> closeImgPath
                    }

                    val allImagesBase64Array = org.json.JSONArray()
                    imagesList.forEach { imgPath ->
                        val b64 = com.example.util.CompressionUtils.convertUriOrPathToBase64(applicationContext, imgPath)
                        if (b64.isNotBlank()) {
                            allImagesBase64Array.put(b64)
                        }
                    }

                    put("ImageUrl", openDisplayUrl)
                    put("imageUrl", openDisplayUrl)
                    put("image_url", openDisplayUrl)
                    put("Image_Url", openDisplayUrl)
                    put("imgaeUrl", openDisplayUrl)
                    put("imageUri", openImgPath)
                    put("image_uri", openImgPath)
                    put("ImageUri", openImgPath)
                    put("image", openDisplayUrl)
                    put("images", openDisplayUrl)
                    put("ImageBase64", openImgBase64)
                    put("imageBase64", openImgBase64)
                    put("imageBase64DataUri", openDataUri)
                    put("imagesBase64", allImagesBase64Array)

                    // Close Case Image URL (For Column J "CloseImageUrl" / Closed Case Image)
                    put("CloseImageUrl", closeDisplayUrl)
                    put("closeImageUrl", closeDisplayUrl)
                    put("close_image_url", closeDisplayUrl)
                    put("Close_Image_Url", closeDisplayUrl)
                    put("closeImageUri", closeImgPath)
                    put("CloseImageBase64", closeImgBase64)
                    put("closeImageBase64", closeImgBase64)
                    put("closeImageBase64DataUri", closeDataUri)

                    put("Solutions", workLog.solutions)
                    put("solutions", workLog.solutions)
                    put("Solution", workLog.solutions)
                    put("solution", workLog.solutions)
                    put("Resolution", workLog.solutions)

                    put("Technician", workLog.technician)
                    put("technician", workLog.technician)
                    put("TechName", workLog.technician)
                    put("tech_name", workLog.technician)

                    put("syncStatus", workLog.syncStatus)

                    put("closedSummary", workLog.buildClosedSummaryTemplate())
                }.toString()

                val result = GoogleScriptNetworkClient.executePost(webhookUrl, jsonPayload)
                if (result is GoogleScriptNetworkClient.ResponseResult.Success) {
                    Log.d(TAG, "Successfully synced work log ID: ${workLog.id}")
                    repository.updateSyncStatus(workLog.id, WorkLog.SYNC_SYNCED)
                } else {
                    Log.e(TAG, "Sync failed for ID ${workLog.id}")
                    repository.updateSyncStatus(workLog.id, WorkLog.SYNC_FAILED)
                    allSynced = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network exception syncing work log ${workLog.id}", e)
                repository.updateSyncStatus(workLog.id, WorkLog.SYNC_FAILED)
                allSynced = false
            }
        }

        if (allSynced) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
    }
}
