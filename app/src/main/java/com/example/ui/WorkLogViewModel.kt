package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiCategorizer
import com.example.ai.AiClassificationResult
import com.example.data.WorkLog
import com.example.data.ScannedText
import com.example.data.WorkLogDatabase
import com.example.data.WorkLogRepository
import com.example.data.RepairCase
import com.example.data.Technician
import com.example.data.FirestoreCaseRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.example.util.UpdateManager
import com.example.util.AppVersionInfo

enum class ActiveCaseStatus {
    ACTIVE,
    CLOSED
}

class WorkLogViewModel(
    application: Application,
    private val repository: WorkLogRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("worklog_prefs", Context.MODE_PRIVATE)
    val firestoreRepository by lazy { FirestoreCaseRepository(application) }

    // Version & Patch Update Management
    val updateManager by lazy { UpdateManager(application) }
    val availableUpdateInfo = MutableStateFlow<AppVersionInfo?>(null)
    val isCheckingUpdate = MutableStateFlow(false)
    val lastUpdateCheckTime = MutableStateFlow(prefs.getLong("last_update_check_time", 0L))
    val updateCheckStatusMessage = MutableStateFlow<String?>(null)
    val customPatchUrl = MutableStateFlow(prefs.getString("custom_patch_url", "") ?: "")
    val isDownloadingPatch = MutableStateFlow(false)
    val showUpdateAlertBanner = MutableStateFlow(true)
    val targetSettingsSubPage = MutableStateFlow<String?>(null)
    private var realtimeUpdateListener: com.google.firebase.firestore.ListenerRegistration? = null

    // Case status state management system
    val currentCaseStatus = MutableStateFlow(ActiveCaseStatus.ACTIVE)
    val isCaseActive = MutableStateFlow(true)

    fun setCaseStatus(status: ActiveCaseStatus) {
        currentCaseStatus.value = status
        isCaseActive.value = (status == ActiveCaseStatus.ACTIVE)
    }

    fun toggleCaseStatus() {
        if (currentCaseStatus.value == ActiveCaseStatus.ACTIVE) {
            setCaseStatus(ActiveCaseStatus.CLOSED)
        } else {
            setCaseStatus(ActiveCaseStatus.ACTIVE)
        }
    }

    // Sync status states
    val syncState = MutableStateFlow(SyncState.SYNCED)
    val lastSyncedTime = MutableStateFlow<Long>(prefs.getLong("last_synced_time", System.currentTimeMillis()))
    val simulateSyncError = MutableStateFlow(prefs.getBoolean("simulate_sync_error", false))
    val googleSheetsWebhookUrl = MutableStateFlow(prefs.getString("google_sheets_webhook_url", "https://script.google.com/macros/s/AKfycbwYLIgjWsziAPW8KfTZMxYOwH6xOG5BfXQp8fqekzIARaXI00r9eGIsNwy1jTNU4rvs5w/exec") ?: "https://script.google.com/macros/s/AKfycbwYLIgjWsziAPW8KfTZMxYOwH6xOG5BfXQp8fqekzIARaXI00r9eGIsNwy1jTNU4rvs5w/exec")

    // Connection testing states
    val testConnectionResult = MutableStateFlow<TestConnectionResult?>(null)
    val isTestingConnection = MutableStateFlow(false)

    // LINE Chat Bot (Messaging API) Settings & State
    val lineBotChannelToken = MutableStateFlow(prefs.getString("line_bot_channel_token", "") ?: "")
    val lineBotTargetId = MutableStateFlow(prefs.getString("line_bot_target_id", "") ?: "")
    val lineBotAutoNotifyNewCase = MutableStateFlow(prefs.getBoolean("line_bot_auto_notify_new_case", false))
    val lineBotAutoNotifyCloseCase = MutableStateFlow(prefs.getBoolean("line_bot_auto_notify_close_case", true))
    val testLineBotResult = MutableStateFlow<com.example.util.LineMessagingApiClient.LineApiResult?>(null)
    val lineBotDiagnosticReport = MutableStateFlow<com.example.util.LineMessagingApiClient.LineDiagnosticReport?>(null)
    val isTestingLineBot = MutableStateFlow(false)

    // Notification & Sound/Vibration Settings
    val dailyReminderEnabled = MutableStateFlow(prefs.getBoolean("daily_reminder_enabled", true))
    val dailyReminderHour = MutableStateFlow(prefs.getInt("daily_reminder_hour", 17)) // Default 17:00 (5:00 PM)
    val dailyReminderMinute = MutableStateFlow(prefs.getInt("daily_reminder_minute", 0))
    val caseReminderEnabled = MutableStateFlow(prefs.getBoolean("case_reminder_enabled", true))
    val reminderLeadTimeMinutes = MutableStateFlow(prefs.getInt("reminder_lead_time_minutes", 30))
    val notificationSoundEnabled = MutableStateFlow(prefs.getBoolean("notification_sound_enabled", true))
    val notificationSoundType = MutableStateFlow(prefs.getString("notification_sound_type", "default") ?: "default")
    val notificationVibrationEnabled = MutableStateFlow(prefs.getBoolean("notification_vibration_enabled", true))
    val notificationVibrationPattern = MutableStateFlow(prefs.getString("notification_vibration_pattern", "default") ?: "default")
    val backupReminderEnabled = MutableStateFlow(prefs.getBoolean("backup_reminder_enabled", true))

    // Search and Filters (Persisted across app restarts)
    val searchQuery = MutableStateFlow("")
    val filterStatus = MutableStateFlow(
        prefs.getString("saved_filter_status", WorkLog.STATUS_OPEN).let { status ->
            if (status == "ทั้งหมด") WorkLog.STATUS_OPEN else (status ?: WorkLog.STATUS_OPEN)
        }
    ) // WorkLog.STATUS_OPEN ("เปิดเคส") or WorkLog.STATUS_CLOSED ("ปิดเคส") or "ทั้งหมด"
    val selectedCategory = MutableStateFlow<String?>(prefs.getString("saved_selected_category", null))
    val selectedPriority = MutableStateFlow<String?>(null)
    val selectedDateFilter = MutableStateFlow(prefs.getString("saved_date_filter", "ทั้งหมด") ?: "ทั้งหมด") // "ทั้งหมด", "วันนี้", "เมื่อวาน", "7 วันล่าสุด", "30 วันล่าสุด", "กำหนดเอง"
    val customStartDate = MutableStateFlow<Long?>(null)
    val customEndDate = MutableStateFlow<Long?>(null)

    // Last technician name pre-fill memory
    val lastTechnicianName = MutableStateFlow(prefs.getString("last_technician", "") ?: "")

    // Current logged-in technician
    var currentTechnician: Technician? = null
        private set

    // Firestore Listener Registration สำหรับติดตามเคสแบบ Realtime
    private var casesSnapshotListener: com.google.firebase.firestore.ListenerRegistration? = null
    // StateFlow ภายในเก็บรายการเคสสดจาก Firestore สำหรับแสดงผล
    private val _firestoreWorkLogs = MutableStateFlow<List<WorkLog>>(emptyList())
    // All logs StateFlow สำหรับแสดงผลและนับสถิติ (ดึงตรงจาก Firestore Realtime)
    val allLogs: StateFlow<List<WorkLog>> = _firestoreWorkLogs.asStateFlow()

    private var realtimeCasesJob: kotlinx.coroutines.Job? = null
    private var notificationsJob: kotlinx.coroutines.Job? = null
    private val alertedNotificationIds = mutableSetOf<String>()

    // Unread notifications for the UI if needed
    val unreadNotifications = MutableStateFlow<List<FirestoreCaseRepository.NotificationDoc>>(emptyList())

    fun setCurrentTechnician(technician: Technician) {
        this.currentTechnician = technician
        if (technician.name.isNotBlank()) {
            lastTechnicianName.value = technician.name
            prefs.edit().putString("last_technician", technician.name).apply()
        }
        startRealtimeCasesListener(technician.id)
    }

    private val _activityLogsState = kotlinx.coroutines.flow.MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogsState: kotlinx.coroutines.flow.StateFlow<List<ActivityLog>> = _activityLogsState

    /**
     * Records activity log in Firestore "activity_logs" collection.
     */
    fun logActivity(
        technicianId: String,
        technicianName: String,
        action: String,           // "LOGIN" | "LOGOUT" | "OPEN_CASE" | "CLOSE_CASE"
        caseId: String? = null,
        caseTitle: String? = null
    ) {
        if (technicianId.isBlank()) return

        val cleanTechId = technicianId.trim()
        val cleanTechName = technicianName.trim()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val logData = hashMapOf<String, Any?>(
                    "technicianId" to cleanTechId,
                    "technicianName" to cleanTechName,
                    "action" to action,
                    "caseId" to caseId,
                    "caseTitle" to caseTitle,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "deviceInfo" to android.os.Build.MODEL
                )
                db.collection("activity_logs")
                    .add(logData)
                    .addOnSuccessListener {
                        android.util.Log.d("WorkLogViewModel", "Activity logged: $action for $cleanTechName")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("WorkLogViewModel", "Failed to log activity: ${e.message}", e)
                    }
            } catch (e: Exception) {
                android.util.Log.e("WorkLogViewModel", "Error logging activity", e)
            }
        }
    }

    /**
     * Loads the last 50 activity logs for the current technician.
     */
    fun loadMyActivityLogs(technicianId: String) {
        if (technicianId.isBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("activity_logs")
                    .whereEqualTo("technicianId", technicianId.trim())
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            android.util.Log.w("WorkLogViewModel", "Activity logs snapshot notice: ${e.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val logs = snapshot.documents.mapNotNull { doc ->
                                try {
                                    val id = doc.id
                                    val techId = doc.getString("technicianId") ?: ""
                                    val techName = doc.getString("technicianName") ?: ""
                                    val action = doc.getString("action") ?: ""
                                    val caseId = doc.getString("caseId")
                                    val caseTitle = doc.getString("caseTitle")
                                    val timestamp = doc.getTimestamp("timestamp")
                                    val deviceInfo = doc.getString("deviceInfo")
                                    ActivityLog(
                                        id = id,
                                        technicianId = techId,
                                        technicianName = techName,
                                        action = action,
                                        caseId = caseId,
                                        caseTitle = caseTitle,
                                        timestamp = timestamp,
                                        deviceInfo = deviceInfo
                                    )
                                } catch (ex: Exception) {
                                    null
                                }
                            }.sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }.take(50)
                            _activityLogsState.value = logs
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.w("WorkLogViewModel", "Error loading activity logs: ${e.message}")
            }
        }
    }

    /**
     * Closes a repair case by ID.
     */
    fun closeCaseById(
        caseId: String,
        caseTitle: String,
        technicianId: String,
        technicianName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                firestoreRepository.updateCaseFields(
                    caseId = caseId,
                    fields = mapOf(
                        "status" to "CLOSED",
                        "closedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ),
                    userId = null, // Will default to currentAuthUid in updateCaseFields
                    onSuccess = {
                        logActivity(
                            technicianId = technicianId,
                            technicianName = technicianName,
                            action = "CLOSE_CASE",
                            caseId = caseId,
                            caseTitle = caseTitle
                        )
                        // Mark notifications as read immediately
                        markCaseNotificationsAsRead(caseId)
                        // Also update local Room database if applicable
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val localLog = repository.getLogByIdDirect(caseId)
                                    ?: allLogs.value.find { it.caseNumber == caseId }
                                    ?: allLogs.value.find { it.id == caseId }
                                if (localLog != null) {
                                    val updatedLog = localLog.copy(
                                        status = WorkLog.STATUS_CLOSED,
                                        syncStatus = WorkLog.SYNC_PENDING
                                    )
                                    repository.update(updatedLog)
                                    com.example.widget.WorkLogWidgetProvider.updateAllWidgets(getApplication())
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("WorkLogViewModel", "Local update failed in closeCaseById", e)
                            }
                        }
                        onSuccess()
                    },
                    onError = { e ->
                        android.util.Log.e("WorkLogViewModel", "closeCaseById Firestore update failed", e)
                        onError(e.localizedMessage ?: e.message ?: "Failed to close case")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("WorkLogViewModel", "closeCaseById error", e)
                onError(e.localizedMessage ?: e.message ?: "Exception occurred")
            }
        }
    }

    /**
     * เริ่มต้น Firestore addSnapshotListener เพื่อดึงและติดตามข้อมูลเคสซ่อมแบบ Realtime
     * เมื่อแอดมินแก้ไขหรือลบเคสบนเว็บ Firestore ข้อมูลใน StateFlow จะอัปเดตทันที
     * 
     * - ถ้า technicianId ยังไม่ถูก set จะยังไม่ query จนกว่าจะได้ ID
     * - มีการ handle ฟิลด์ null / ฟิลด์เก่า อย่างปลอดภัย ไม่ให้แอป crash
     * - บันทึกลง Room Database ใน Background เพื่อใช้เป็น Offline Cache
     */
    fun startRealtimeCasesListener(technicianId: String? = currentTechnician?.id) {
        // 1. ยกเลิก Listener เดิมออกก่อนเพื่อป้องกัน Listener ซ้ำซ้อน
        casesSnapshotListener?.remove()
        casesSnapshotListener = null
        realtimeCasesJob?.cancel()
        notificationsJob?.cancel()
        
        // 2. ดึง ID ของช่างปัจจุบัน
        val targetId = (technicianId ?: currentTechnician?.id ?: "").trim()
        
        // ถ้า currentTechnicianId ยังไม่ถูก set ให้รอก่อน ไม่ต้อง query จนกว่าจะได้ id
        if (targetId.isBlank()) {
            android.util.Log.d("WorkLogViewModel", "startRealtimeCasesListener: technicianId is empty, waiting for ID...")
            return
        }

        android.util.Log.d("WorkLogViewModel", "startRealtimeCasesListener: Starting snapshot listener for technicianId=$targetId")

        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            
            // 3. เริ่ม SnapshotListener บน repair_cases ที่ technicianId ตรงกัน
            casesSnapshotListener = firestore.collection("repair_cases")
                .whereEqualTo("technicianId", targetId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("WorkLogViewModel", "Firestore SnapshotListener error: ${error.message}", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        // 4. Map เป็น WorkLog object และ handle กรณี field เป็น null อย่างปลอดภัย
                        val liveLogs = snapshot.documents.mapNotNull { doc ->
                            try {
                                val caseId = doc.getString("caseId") ?: doc.id
                                val customerName = doc.getString("customerName") ?: ""
                                val docPhone = (doc.getString("decryptedPhone") ?: doc.getString("phone") ?: doc.getString("encryptedPhone") ?: "").trim()
                                val phoneStr = com.example.util.EncryptionHelper.decrypt(docPhone).ifBlank { docPhone }
                                val docAddr = (doc.getString("decryptedAddress") ?: doc.getString("address") ?: doc.getString("encryptedAddress") ?: "").trim()
                                val addrStr = com.example.util.EncryptionHelper.decrypt(docAddr).ifBlank { docAddr }
                                val details = (doc.getString("details") ?: doc.getString("description") ?: "").trim()

                                val fullRawText = if (details.isNotBlank()) {
                                    details
                                } else {
                                    buildString {
                                        if (customerName.isNotBlank()) append("ลูกค้า: ").append(customerName).append("\n")
                                        if (phoneStr.isNotBlank()) append("โทร: ").append(phoneStr).append("\n")
                                        if (addrStr.isNotBlank()) append("ที่อยู่: ").append(addrStr).append("\n")
                                    }.trim()
                                }

                                val rawStatus = (doc.getString("status") ?: "OPEN").trim()
                                val caseStatus = if (rawStatus.equals("CLOSED", ignoreCase = true) || 
                                    rawStatus.equals("ปิดเคส", ignoreCase = true) || 
                                    rawStatus.equals("ซ่อมเสร็จแล้ว", ignoreCase = true)) {
                                    WorkLog.STATUS_CLOSED
                                } else {
                                    WorkLog.STATUS_OPEN
                                }

                                val techName = (doc.getString("technicianName") ?: doc.getString("assignedTechName") ?: "").trim()
                                val tsAny = doc.get("timestamp")
                                val tsLong = when (tsAny) {
                                    is com.google.firebase.Timestamp -> tsAny.seconds * 1000
                                    is Long -> tsAny
                                    is Double -> tsAny.toLong()
                                    is Number -> tsAny.toLong()
                                    is java.util.Date -> tsAny.time
                                    else -> System.currentTimeMillis()
                                }

                                val imgUrl = doc.getString("imageUrl") ?: (doc.get("imageUrls") as? List<*>)?.firstOrNull() as? String ?: ""
                                val checkInUrl = doc.getString("checkInImageUrl") ?: ""
                                val checkOutUrl = doc.getString("checkOutImageUrl") ?: ""
                                val cat = (doc.getString("category") ?: doc.getString("taskName") ?: "งานซ่อมทั่วไป").trim()
                                val prio = (doc.getString("priority") ?: "ปกติ").trim()
                                val sol = (doc.getString("solutions") ?: "").trim()

                                val checkInTimeVal = when (val cTime = doc.get("checkInTime")) {
                                    is Long -> cTime
                                    is Double -> cTime.toLong()
                                    is Number -> cTime.toLong()
                                    is com.google.firebase.Timestamp -> cTime.seconds * 1000
                                    is String -> cTime.toLongOrNull()
                                    else -> null
                                }
                                val checkOutTimeVal = when (val cTime = doc.get("checkOutTime")) {
                                    is Long -> cTime
                                    is Double -> cTime.toLong()
                                    is Number -> cTime.toLong()
                                    is com.google.firebase.Timestamp -> cTime.seconds * 1000
                                    is String -> cTime.toLongOrNull()
                                    else -> null
                                }

                                val existingLocal = allLogs.value.find { it.id == caseId || it.caseNumber == caseId }

                                val finalCheckInTime = checkInTimeVal 
                                    ?: existingLocal?.checkInTime 
                                    ?: if (checkInUrl.isNotBlank()) tsLong else null

                                val finalCheckOutTime = checkOutTimeVal 
                                    ?: existingLocal?.checkOutTime 
                                    ?: if (checkOutUrl.isNotBlank()) tsLong else null

                                val finalCheckInUri = checkInUrl.ifBlank { existingLocal?.checkInImageUri }
                                val finalCheckOutUri = checkOutUrl.ifBlank { existingLocal?.checkOutImageUri }

                                WorkLog(
                                    id = caseId,
                                    rawText = if (fullRawText.isNotBlank()) fullRawText else "เคสซ่อมจากระบบ",
                                    category = if (cat.isNotBlank()) cat else "งานซ่อมทั่วไป",
                                    priority = if (prio.isNotBlank()) prio else "ปกติ",
                                    status = caseStatus,
                                    technician = if (techName.isNotBlank()) techName else (currentTechnician?.name ?: ""),
                                    timestamp = if (tsLong > 0) tsLong else System.currentTimeMillis(),
                                    imageUri = imgUrl.ifBlank { null },
                                    syncStatus = WorkLog.SYNC_SYNCED,
                                    caseNumber = caseId,
                                    checkInTime = finalCheckInTime,
                                    checkOutTime = finalCheckOutTime,
                                    checkInImageUri = finalCheckInUri,
                                    checkOutImageUri = finalCheckOutUri,
                                    solutions = sol
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("WorkLogViewModel", "Error parsing Firestore document ${doc.id}: ${e.message}", e)
                                null
                            }
                        }.sortedByDescending { it.timestamp }

                        // 5. อัปเดต StateFlow ทันที เพื่อให้ UI (LogListScreen) ได้ข้อมูลสดจาก Firestore
                        _firestoreWorkLogs.value = liveLogs

                        // 6. Offline Cache: บันทึก/อัปเดตลง Room Database ใน Background เพื่อเก็บไว้เปิดดูแบบออฟไลน์
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                liveLogs.forEach { log ->
                                    val existing = repository.getLogByIdOrCaseNumberDirect(log.id)
                                    if (existing == null) {
                                        repository.insert(log)
                                    } else {
                                        repository.update(log)
                                    }
                                }

                                // ลบ Room records ที่ไม่มีใน Firestore snapshot แล้ว
                                // (เคสที่ถูกลบจากเว็บ Dashboard)
                                val liveIds = liveLogs.map { it.id }.toSet()
                                val localLogs = repository.allLogs.first()
                                localLogs
                                    .filter { it.id !in liveIds && it.caseNumber !in liveIds }
                                    .forEach { staleLog ->
                                        try {
                                            repository.delete(staleLog)
                                        } catch (e: Exception) {
                                            android.util.Log.e("WorkLogViewModel", "Failed to delete stale log ${staleLog.id}: ${e.message}")
                                        }
                                    }
                            } catch (e: Exception) {
                                android.util.Log.e("WorkLogViewModel", "Offline cache update error: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("WorkLogViewModel", "Error attaching Firestore SnapshotListener: ${e.message}", e)
        }

        // 7. ติดตาม Notification งานใหม่
        notificationsJob = viewModelScope.launch {
            try {
                firestoreRepository.getUnreadNotifications(targetId).collect { docs ->
                    unreadNotifications.value = docs
                    for (doc in docs) {
                        if (!alertedNotificationIds.contains(doc.id)) {
                            // Run checking on Dispatchers.IO to be completely non-blocking
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    // 0. Check if notification is older than 10 minutes (600,000 ms)
                                    val now = System.currentTimeMillis()
                                    if (doc.timestampLong != 0L && (now - doc.timestampLong > 600_000L)) {
                                        android.util.Log.d("WorkLogViewModel", "Notification ${doc.id} is old (${doc.timestampLong}). Marking as read silently.")
                                        firestoreRepository.markNotificationAsRead(doc.id)
                                        return@launch
                                    }

                                    // 1. Check in-memory list first
                                    var isClosed = allLogs.value.any { log ->
                                        (log.caseNumber == doc.caseId || log.id == doc.caseId) && log.status == WorkLog.STATUS_CLOSED
                                    }

                                    // 2. If not closed in memory, check local Room DB cache
                                    if (!isClosed) {
                                        val localLog = repository.getLogByIdOrCaseNumberDirect(doc.caseId)
                                        if (localLog?.status == WorkLog.STATUS_CLOSED) {
                                            isClosed = true
                                        }
                                    }

                                    // 3. If still not closed, check Firestore directly to see if the case has been closed on the cloud/web dashboard
                                    if (!isClosed) {
                                        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        var closedRef = false
                                        try {
                                            val docSnap = firestore.collection("repair_cases").document(doc.caseId).get().await()
                                            if (docSnap.exists()) {
                                                val rawStatus = docSnap.getString("status") ?: ""
                                                closedRef = rawStatus.equals("CLOSED", ignoreCase = true) || 
                                                            rawStatus.equals("ปิดเคส", ignoreCase = true) || 
                                                            rawStatus.equals("ซ่อมเสร็จแล้ว", ignoreCase = true)
                                            } else {
                                                val querySnap = firestore.collection("repair_cases")
                                                    .whereEqualTo("caseId", doc.caseId)
                                                    .limit(1)
                                                    .get()
                                                    .await()
                                                val matchingDoc = querySnap.documents.firstOrNull()
                                                if (matchingDoc != null) {
                                                    val rawStatus = matchingDoc.getString("status") ?: ""
                                                    closedRef = rawStatus.equals("CLOSED", ignoreCase = true) || 
                                                                rawStatus.equals("ปิดเคส", ignoreCase = true) || 
                                                                rawStatus.equals("ซ่อมเสร็จแล้ว", ignoreCase = true)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("WorkLogViewModel", "Direct Firestore case status check failed", e)
                                        }
                                        if (closedRef) {
                                            isClosed = true
                                        }
                                    }

                                    if (isClosed) {
                                        // If the case is already closed, automatically mark the notification as read on Firestore and skip alert
                                        android.util.Log.d("WorkLogViewModel", "Case #${doc.caseId} is already closed. Marking notification ${doc.id} as read and skipping alert.")
                                        firestoreRepository.markNotificationAsRead(doc.id)
                                    } else {
                                        // It's a genuine new open case notification
                                        alertedNotificationIds.add(doc.id)
                                        // Mark as read immediately on Firestore so it doesn't alert again on next app launch
                                        firestoreRepository.markNotificationAsRead(doc.id)
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            android.widget.Toast.makeText(
                                                getApplication(),
                                                "🔔 มีแจ้งเตือนเคสใหม่: #${doc.caseId} (${doc.customerName})",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("WorkLogViewModel", "Error in notification check coroutine", e)
                                    // Fallback to showing notification on error
                                    alertedNotificationIds.add(doc.id)
                                    // Mark as read anyway to avoid infinite alert loops on error
                                    firestoreRepository.markNotificationAsRead(doc.id)
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        android.widget.Toast.makeText(
                                            getApplication(),
                                            "🔔 มีแจ้งเตือนเคสใหม่: #${doc.caseId} (${doc.customerName})",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("WorkLogViewModel", "Error collecting notifications flow: ${e.message}", e)
                }
            }
        }
    }

    fun markCaseNotificationsAsRead(caseId: String) {
        val currentUnread = unreadNotifications.value
        val toMark = currentUnread.filter { it.caseId == caseId }
        for (notif in toMark) {
            firestoreRepository.markNotificationAsRead(notif.id)
        }
    }

    init {
        // Migrate placeholder to actual webhook if current value is placeholder or blank
        val currentWebhook = prefs.getString("google_sheets_webhook_url", "") ?: ""
        if (currentWebhook.isBlank() || currentWebhook.contains("placeholder")) {
            prefs.edit().putString("google_sheets_webhook_url", "https://script.google.com/macros/s/AKfycbwYLIgjWsziAPW8KfTZMxYOwH6xOG5BfXQp8fqekzIARaXI00r9eGIsNwy1jTNU4rvs5w/exec").apply()
            googleSheetsWebhookUrl.value = "https://script.google.com/macros/s/AKfycbwYLIgjWsziAPW8KfTZMxYOwH6xOG5BfXQp8fqekzIARaXI00r9eGIsNwy1jTNU4rvs5w/exec"
        }

        // Schedule monthly backup reminder on startup
        com.example.util.ReminderScheduler.scheduleBackupReminder(application)

        // Cancel daily work log reminder if any was registered previously
        com.example.util.ReminderScheduler.cancelDailyLogReminder(application)

        // Perform initial retrieve sync on startup
        syncWithCloud()

        // โหลดข้อมูลจาก Room Cache มาแสดงเบื้องต้นก่อน ระหว่างรอ Firestore โหลดครั้งแรก
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cachedLogs = repository.allLogs.first()
                // แสดง cache เฉพาะตอนที่ยังไม่มี technicianId
                // (ยังไม่ได้ login) เพื่อให้มีข้อมูลแสดงระหว่างโหลด
                // แต่พอ Firestore snapshot มาแล้ว ให้ใช้ข้อมูลจาก
                // Firestore เสมอ ไม่ override กลับด้วย cache
                if (_firestoreWorkLogs.value.isEmpty()
                    && cachedLogs.isNotEmpty()
                    && currentTechnician == null) {
                    _firestoreWorkLogs.value = cachedLogs
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkLogViewModel", "Initial cache load error: ${e.message}")
            }
        }

        // Real-time Firestore sync listener (addSnapshotListener) for assigned cases
        startRealtimeCasesListener()

        // Start listening for version & patch updates
        startListeningForUpdates()

        viewModelScope.launch {
            try {
                _firestoreWorkLogs.collect { logs ->
                    logs.filter { (it.status ?: "") == WorkLog.STATUS_OPEN }.forEach { log ->
                        try {
                            com.example.util.ReminderScheduler.scheduleReminder(application, log)
                        } catch (e: Exception) {
                            android.util.Log.e("WorkLogViewModel", "Error scheduling reminder for log ${log.id}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkLogViewModel", "Error in logs collector", e)
            }
        }

        viewModelScope.launch {
            filterStatus.collect { status ->
                prefs.edit().putString("saved_filter_status", status).apply()
            }
        }

        viewModelScope.launch {
            selectedCategory.collect { cat ->
                if (cat == null) {
                    prefs.edit().remove("saved_selected_category").apply()
                } else {
                    prefs.edit().putString("saved_selected_category", cat).apply()
                }
            }
        }

        viewModelScope.launch {
            selectedDateFilter.collect { dateFilter ->
                prefs.edit().putString("saved_date_filter", dateFilter).apply()
            }
        }
    }

    fun updateDailyReminderEnabled(enabled: Boolean) {
        dailyReminderEnabled.value = enabled
        prefs.edit().putBoolean("daily_reminder_enabled", enabled).apply()
        if (enabled) {
            com.example.util.ReminderScheduler.scheduleDailyLogReminder(
                getApplication(),
                dailyReminderHour.value,
                dailyReminderMinute.value
            )
        } else {
            com.example.util.ReminderScheduler.cancelDailyLogReminder(getApplication())
        }
    }

    fun updateDailyReminderTime(hour: Int, minute: Int) {
        dailyReminderHour.value = hour
        dailyReminderMinute.value = minute
        prefs.edit()
            .putInt("daily_reminder_hour", hour)
            .putInt("daily_reminder_minute", minute)
            .apply()

        if (dailyReminderEnabled.value) {
            com.example.util.ReminderScheduler.scheduleDailyLogReminder(
                getApplication(),
                hour,
                minute
            )
        }
    }

    fun sendTestDailyReminder() {
        com.example.util.ReminderScheduler.sendInstantTestNotification(getApplication())
    }

    fun updateCaseReminderEnabled(enabled: Boolean) {
        caseReminderEnabled.value = enabled
        prefs.edit().putBoolean("case_reminder_enabled", enabled).apply()
        rescheduleAllOpenCaseReminders()
    }

    fun updateReminderLeadTime(minutes: Int) {
        reminderLeadTimeMinutes.value = minutes
        prefs.edit().putInt("reminder_lead_time_minutes", minutes).apply()
        rescheduleAllOpenCaseReminders()
    }

    fun updateNotificationSoundEnabled(enabled: Boolean) {
        notificationSoundEnabled.value = enabled
        prefs.edit().putBoolean("notification_sound_enabled", enabled).apply()
    }

    fun updateNotificationSoundType(type: String) {
        notificationSoundType.value = type
        prefs.edit().putString("notification_sound_type", type).apply()
    }

    fun updateNotificationVibrationEnabled(enabled: Boolean) {
        notificationVibrationEnabled.value = enabled
        prefs.edit().putBoolean("notification_vibration_enabled", enabled).apply()
    }

    fun updateNotificationVibrationPattern(pattern: String) {
        notificationVibrationPattern.value = pattern
        prefs.edit().putString("notification_vibration_pattern", pattern).apply()
    }

    fun updateBackupReminderEnabled(enabled: Boolean) {
        backupReminderEnabled.value = enabled
        prefs.edit().putBoolean("backup_reminder_enabled", enabled).apply()
        if (enabled) {
            com.example.util.ReminderScheduler.scheduleBackupReminder(getApplication())
        }
    }

    fun rescheduleAllOpenCaseReminders() {
        viewModelScope.launch {
            allLogs.value.filter { it.status == WorkLog.STATUS_OPEN }.forEach { log ->
                if (caseReminderEnabled.value) {
                    com.example.util.ReminderScheduler.scheduleReminder(getApplication(), log)
                } else {
                    com.example.util.ReminderScheduler.cancelReminder(getApplication(), log)
                }
            }
        }
    }

    fun sendTestNotificationWithSettings() {
        val intent = android.content.Intent(getApplication(), com.example.util.ReminderReceiver::class.java).apply {
            putExtra("customerName", "ลูกค้าทดสอบ (เคสจำลอง)")
            putExtra("onsiteTime", "14:00 น.")
            putExtra("category", "ทดสอบการตั้งค่าเสียงและสั่น")
            putExtra("logId", "TEST_NOTIF_${System.currentTimeMillis()}")
        }
        getApplication<Application>().sendBroadcast(intent)
    }

    fun toggleSimulateSyncError() {
        // Kept for backward compatibility but disabled
    }

    fun testConnection(onComplete: (TestConnectionResult) -> Unit = {}) {
        val webhookUrl = prefs.getString("google_sheets_webhook_url", googleSheetsWebhookUrl.value) ?: googleSheetsWebhookUrl.value
        if (webhookUrl.isBlank() || webhookUrl.contains("placeholder") || !webhookUrl.startsWith("http")) {
            val result = TestConnectionResult(
                isSuccess = false,
                message = "URL ของ Webhook ไม่ถูกต้อง หรือยังไม่ได้กำหนด"
            )
            testConnectionResult.value = result
            onComplete(result)
            return
        }

        isTestingConnection.value = true
        testConnectionResult.value = null // Clear previous result
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val responseResult = com.example.util.GoogleScriptNetworkClient.executeGet(webhookUrl)
            val result = when (responseResult) {
                is com.example.util.GoogleScriptNetworkClient.ResponseResult.Success -> {
                    TestConnectionResult(
                        isSuccess = true,
                        statusCode = responseResult.statusCode,
                        message = "เชื่อมต่อสำเร็จ! (HTTP ${responseResult.statusCode}) สคริปต์ Google Sheets ตอบรับคำขอเรียบร้อยแล้ว",
                        responseBody = responseResult.body?.take(300)
                    )
                }
                is com.example.util.GoogleScriptNetworkClient.ResponseResult.Error -> {
                    TestConnectionResult(
                        isSuccess = false,
                        statusCode = responseResult.statusCode,
                        message = responseResult.message,
                        responseBody = responseResult.responseBody
                    )
                }
            }
            testConnectionResult.value = result
            isTestingConnection.value = false
            onComplete(result)
        }
    }

    fun updateLineBotChannelToken(token: String) {
        val trimmed = token.trim()
        lineBotChannelToken.value = trimmed
        prefs.edit().putString("line_bot_channel_token", trimmed).apply()
    }

    fun updateLineBotTargetId(targetId: String) {
        val trimmed = targetId.trim()
        lineBotTargetId.value = trimmed
        prefs.edit().putString("line_bot_target_id", trimmed).apply()
    }

    fun updateLineBotAutoNotifyNewCase(enabled: Boolean) {
        lineBotAutoNotifyNewCase.value = enabled
        prefs.edit().putBoolean("line_bot_auto_notify_new_case", enabled).apply()
    }

    fun updateLineBotAutoNotifyCloseCase(enabled: Boolean) {
        lineBotAutoNotifyCloseCase.value = enabled
        prefs.edit().putBoolean("line_bot_auto_notify_close_case", enabled).apply()
    }

    fun testLineBotConnection(onComplete: (com.example.util.LineMessagingApiClient.LineApiResult) -> Unit = {}) {
        val token = prefs.getString("line_bot_channel_token", lineBotChannelToken.value)?.trim() ?: lineBotChannelToken.value.trim()
        val targetId = prefs.getString("line_bot_target_id", lineBotTargetId.value)?.trim() ?: lineBotTargetId.value.trim()
        if (token.isBlank()) {
            val res = com.example.util.LineMessagingApiClient.LineApiResult(false, 0, "กรุณากรอก Channel Access Token")
            testLineBotResult.value = res
            onComplete(res)
            return
        }

        isTestingLineBot.value = true
        testLineBotResult.value = null
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val report = com.example.util.LineMessagingApiClient.runComprehensiveDiagnostics(token, targetId)
            lineBotDiagnosticReport.value = report
            val botName = report.botProfile?.displayName ?: "LINE Bot"
            val res = if (report.isOverallSuccess) {
                com.example.util.LineMessagingApiClient.LineApiResult(
                    isSuccess = true,
                    statusCode = 200,
                    message = "เชื่อมต่อสำเร็จ! พบ LINE Bot: '$botName'" + if (targetId.isNotBlank()) " และส่งข้อความทดสอบเข้าห้องแชทเรียบร้อยแล้ว 🎉" else "",
                    botName = botName,
                    latencyMs = report.totalLatencyMs
                )
            } else {
                val failedStep = report.steps.firstOrNull { it.status == com.example.util.LineMessagingApiClient.StepStatus.FAILED }
                val errorMsg = failedStep?.summary ?: "การเชื่อมต่อล้มเหลว กรุณาตรวจสอบผลการวินิจฉัย"
                com.example.util.LineMessagingApiClient.LineApiResult(
                    isSuccess = false,
                    statusCode = failedStep?.httpCode ?: 0,
                    message = errorMsg,
                    botName = botName,
                    latencyMs = report.totalLatencyMs
                )
            }
            testLineBotResult.value = res
            isTestingLineBot.value = false
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(res)
            }
        }
    }

    fun runLineBotDiagnostics(onComplete: (com.example.util.LineMessagingApiClient.LineDiagnosticReport) -> Unit = {}) {
        val token = prefs.getString("line_bot_channel_token", lineBotChannelToken.value)?.trim() ?: lineBotChannelToken.value.trim()
        val targetId = prefs.getString("line_bot_target_id", lineBotTargetId.value)?.trim() ?: lineBotTargetId.value.trim()

        isTestingLineBot.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val report = com.example.util.LineMessagingApiClient.runComprehensiveDiagnostics(token, targetId)
            lineBotDiagnosticReport.value = report
            val botName = report.botProfile?.displayName ?: "LINE Bot"
            val res = if (report.isOverallSuccess) {
                com.example.util.LineMessagingApiClient.LineApiResult(
                    isSuccess = true,
                    statusCode = 200,
                    message = "วินิจฉัยสำเร็จ! ระบบพร้อมใช้งาน (บอท: $botName)",
                    botName = botName,
                    latencyMs = report.totalLatencyMs
                )
            } else {
                val failedStep = report.steps.firstOrNull { it.status == com.example.util.LineMessagingApiClient.StepStatus.FAILED }
                com.example.util.LineMessagingApiClient.LineApiResult(
                    isSuccess = false,
                    statusCode = failedStep?.httpCode ?: 0,
                    message = failedStep?.summary ?: "พบข้อผิดพลาดระหว่างวินิจฉัย",
                    botName = botName,
                    latencyMs = report.totalLatencyMs
                )
            }
            testLineBotResult.value = res
            isTestingLineBot.value = false
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(report)
            }
        }
    }

    fun clearLineBotDiagnosticReport() {
        lineBotDiagnosticReport.value = null
        testLineBotResult.value = null
    }

    fun syncWithCloud() {
        val webhookUrl = prefs.getString("google_sheets_webhook_url", googleSheetsWebhookUrl.value) ?: googleSheetsWebhookUrl.value
        if (webhookUrl.isBlank() || webhookUrl.contains("placeholder") || !webhookUrl.startsWith("http")) {
            syncState.value = SyncState.ERROR
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (syncState.value == SyncState.UPLOADING || syncState.value == SyncState.RETRIEVING) return@launch
            
            syncState.value = SyncState.RETRIEVING
            try {
                // Only sync logs that are pending synchronization (PENDING / FAILED)
                val logsList = repository.getPendingSyncWorkLogsList()

                if (logsList.isEmpty()) {
                    val res = com.example.util.GoogleScriptNetworkClient.executeGet(webhookUrl)
                    if (res is com.example.util.GoogleScriptNetworkClient.ResponseResult.Success) {
                        val now = System.currentTimeMillis()
                        prefs.edit().putLong("last_synced_time", now).apply()
                        lastSyncedTime.value = now
                        syncState.value = SyncState.SYNCED
                    } else {
                        syncState.value = SyncState.ERROR
                    }
                } else {
                    var hasError = false
                    for (workLog in logsList) {
                        try {
                            val jsonPayload = buildWorkLogJsonPayload(workLog)

                            val res = com.example.util.GoogleScriptNetworkClient.executePost(webhookUrl, jsonPayload)
                            if (res is com.example.util.GoogleScriptNetworkClient.ResponseResult.Success) {
                                repository.updateSyncStatus(workLog.id, WorkLog.SYNC_SYNCED)
                            } else {
                                hasError = true
                                repository.updateSyncStatus(workLog.id, WorkLog.SYNC_FAILED)
                                android.util.Log.e("GoogleSheetSync", "Failed to sync log ${workLog.id}")
                            }
                        } catch (e: Exception) {
                            hasError = true
                            repository.updateSyncStatus(workLog.id, WorkLog.SYNC_FAILED)
                            android.util.Log.e("GoogleSheetSync", "Error syncing log ${workLog.id}", e)
                        }
                    }

                    if (!hasError) {
                        val now = System.currentTimeMillis()
                        prefs.edit().putLong("last_synced_time", now).apply()
                        lastSyncedTime.value = now
                        syncState.value = SyncState.SYNCED
                    } else {
                        syncState.value = SyncState.ERROR
                    }
                }
            } catch (e: java.lang.Exception) {
                android.util.Log.e("GoogleSheetSync", "Failed to connect to Google Sheets", e)
                syncState.value = SyncState.ERROR
            }
        }
    }

    private fun triggerAutoUpload() {
        val webhookUrl = prefs.getString("google_sheets_webhook_url", googleSheetsWebhookUrl.value) ?: googleSheetsWebhookUrl.value
        if (webhookUrl.isBlank() || webhookUrl.contains("placeholder") || !webhookUrl.startsWith("http")) {
            syncState.value = SyncState.ERROR
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (syncState.value == SyncState.UPLOADING || syncState.value == SyncState.RETRIEVING) return@launch
            
            syncState.value = SyncState.UPLOADING
            try {
                val res = com.example.util.GoogleScriptNetworkClient.executeGet(webhookUrl)
                if (res is com.example.util.GoogleScriptNetworkClient.ResponseResult.Success) {
                    val now = System.currentTimeMillis()
                    prefs.edit().putLong("last_synced_time", now).apply()
                    lastSyncedTime.value = now
                    syncState.value = SyncState.SYNCED
                } else {
                    syncState.value = SyncState.ERROR
                }
            } catch (e: java.lang.Exception) {
                android.util.Log.e("GoogleSheetSync", "Auto upload check failed", e)
                syncState.value = SyncState.ERROR
            }
        }
    }

    // AI Analysis State for Create Screen
    val isAnalyzing = MutableStateFlow(false)
    val aiResult = MutableStateFlow<AiClassificationResult?>(null)

    // Automated Gemini Project Completion Report Agent State
    val isGeneratingCompletionReport = MutableStateFlow(false)
    val completionReportResult = MutableStateFlow<String?>(null)

    fun generateProjectCompletionReport() {
        viewModelScope.launch {
            isGeneratingCompletionReport.value = true
            try {
                val currentLogs = repository.allLogs.first()
                val closedLogs = currentLogs.filter { it.status == WorkLog.STATUS_CLOSED }
                val reportText = com.example.ai.ProjectCompletionAgent.generateReport(closedLogs)
                completionReportResult.value = reportText
            } catch (e: Exception) {
                android.util.Log.e("WorkLogViewModel", "Error generating completion report", e)
                completionReportResult.value = "เกิดข้อผิดพลาดในการสร้างรายงานสรุป: ${e.localizedMessage}"
            } finally {
                isGeneratingCompletionReport.value = false
            }
        }
    }

    fun clearCompletionReport() {
        completionReportResult.value = null
    }

    private data class DateFilterParams(
        val categoryFilter: String?,
        val priorityFilter: String?,
        val dateFilter: String,
        val customStart: Long?,
        val customEnd: Long?
    )

    // Combined Work Logs Flow (ดึงข้อมูลแบบ Realtime จาก Firestore และ Filter ใน Memory)
    val filteredWorkLogs: StateFlow<List<WorkLog>> = combine(
        combine(_firestoreWorkLogs, searchQuery, filterStatus) { logs, query, statusFilter ->
            Triple(logs, query, statusFilter)
        },
        combine(selectedCategory, selectedPriority, selectedDateFilter, customStartDate, customEndDate) { catFilter, prioFilter, dateFilter, startMs, endMs ->
            DateFilterParams(catFilter, prioFilter, dateFilter, startMs, endMs)
        }
    ) { (logs, query, statusFilter), params ->
        logs.filter { log ->
            // Filter by Status
            val matchesStatus = when (statusFilter) {
                WorkLog.STATUS_OPEN -> log.status == WorkLog.STATUS_OPEN
                WorkLog.STATUS_CLOSED -> log.status == WorkLog.STATUS_CLOSED
                else -> true
            }

            // Filter by Category
            val matchesCategory = params.categoryFilter == null || log.category == params.categoryFilter

            // Filter by Priority
            val matchesPriority = params.priorityFilter == null || log.priority == params.priorityFilter

            // Filter by Search Query
            val matchesQuery = query.isBlank() ||
                    (log.rawText ?: "").contains(query, ignoreCase = true) ||
                    (log.category ?: "").contains(query, ignoreCase = true) ||
                    (log.priority ?: "").contains(query, ignoreCase = true) ||
                    (log.technician ?: "").contains(query, ignoreCase = true) ||
                    (log.solutions ?: "").contains(query, ignoreCase = true) ||
                    (log.caseNumber ?: "").contains(query, ignoreCase = true) ||
                    log.formattedCaseNumber.contains(query, ignoreCase = true)

            // Filter by Date (Respect selectedDateFilter, customStartDate, customEndDate)
            val matchesDate = matchesDateFilter(
                log.timestamp,
                params.dateFilter,
                params.customStart,
                params.customEnd
            )

            matchesStatus && matchesCategory && matchesPriority && matchesQuery && matchesDate
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Room Scanned Text History
    val allScannedTexts: StateFlow<List<ScannedText>> = repository.allScannedTexts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertScannedText(text: String, scannedFrom: String = "Camera") {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val existing = allScannedTexts.value.find { it.text.trim() == trimmed }
            if (existing != null) {
                repository.deleteScannedText(existing)
            }
            repository.insertScannedText(ScannedText(text = trimmed, scannedFrom = scannedFrom))
        }
    }

    fun deleteScannedText(scannedText: ScannedText) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.deleteScannedText(scannedText)
        }
    }

    fun deleteScannedTextById(id: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.deleteScannedTextById(id)
        }
    }

    fun clearAllScannedTexts() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.deleteAllScannedTexts()
        }
    }

    private fun matchesDateFilter(
        timestamp: Long,
        filter: String,
        startMs: Long?,
        endMs: Long?
    ): Boolean {
        if (filter == "ทั้งหมด") return true

        val cal = java.util.Calendar.getInstance()
        val now = System.currentTimeMillis()

        fun getStartOfDay(ms: Long): Long {
            cal.timeInMillis = ms
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun getEndOfDay(ms: Long): Long {
            cal.timeInMillis = ms
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
            cal.set(java.util.Calendar.MINUTE, 59)
            cal.set(java.util.Calendar.SECOND, 59)
            cal.set(java.util.Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

        val startOfToday = getStartOfDay(now)
        val endOfToday = getEndOfDay(now)

        return when (filter) {
            "วันนี้" -> timestamp in startOfToday..endOfToday
            "เมื่อวาน" -> {
                cal.timeInMillis = now
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                val startOfYesterday = getStartOfDay(cal.timeInMillis)
                val endOfYesterday = getEndOfDay(cal.timeInMillis)
                timestamp in startOfYesterday..endOfYesterday
            }
            "7 วันล่าสุด" -> {
                cal.timeInMillis = now
                cal.add(java.util.Calendar.DAY_OF_YEAR, -6)
                val start7DaysAgo = getStartOfDay(cal.timeInMillis)
                timestamp in start7DaysAgo..endOfToday
            }
            "30 วันล่าสุด" -> {
                cal.timeInMillis = now
                cal.add(java.util.Calendar.DAY_OF_YEAR, -29)
                val start30DaysAgo = getStartOfDay(cal.timeInMillis)
                timestamp in start30DaysAgo..endOfToday
            }
            "กำหนดเอง" -> {
                val start = startMs ?: 0L
                val end = endMs ?: Long.MAX_VALUE
                timestamp in start..end
            }
            else -> true
        }
    }

    // Google Sheets Webhook URL StateFlow

    fun updateGoogleSheetsWebhookUrl(url: String) {
        googleSheetsWebhookUrl.value = url
        prefs.edit().putString("google_sheets_webhook_url", url).apply()
        syncWithCloud() // Automatically test connection!
    }

    private fun buildWorkLogJsonPayload(workLog: WorkLog): String {
        return org.json.JSONObject().apply {
            val rawCaseNo = workLog.rawCaseNumber
            val formattedCaseNo = workLog.formattedCaseNumber
            val imgUrl = workLog.imageUri ?: ""
            val isClosed = workLog.status == WorkLog.STATUS_CLOSED
            val actionStr = if (isClosed) "close_case" else "open_case"
            val statusThai = if (isClosed) "ปิดเคส" else "เปิดเคส"

            // Action & Status for routing
            put("action", actionStr)
            put("type", actionStr)
            put("Action", actionStr)
            put("Type", actionStr)

            put("status", workLog.status)
            put("Status", workLog.status)
            put("statusThai", statusThai)
            put("state", workLog.status)

            // Identifiers
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

            // Timestamp
            put("timestamp", workLog.timestamp)
            put("Timestamp", workLog.timestamp)
            put("date", workLog.timestamp)
            put("Date", workLog.timestamp)

            // Task Name (For Column C "TaskName")
            put("TaskName", workLog.category)
            put("taskName", workLog.category)
            put("task_name", workLog.category)
            put("Task_Name", workLog.category)
            put("title", workLog.category)
            put("Title", workLog.category)

            // Task Details (For Column D "TaskDetail")
            put("TaskDetail", workLog.rawText)
            put("taskDetail", workLog.rawText)
            put("task_detail", workLog.rawText)
            put("Task_Detail", workLog.rawText)
            put("rawText", workLog.rawText)
            put("detail", workLog.rawText)
            put("Detail", workLog.rawText)
            put("description", workLog.rawText)
            put("Description", workLog.rawText)

            // Category (For Column E "Category")
            put("Category", workLog.category)
            put("category", workLog.category)
            put("categoryName", workLog.category)

            // Image URL (For Column F "ImageUrl" / Open Case Image)
            val imagesList = workLog.imageUriList
            val openImgPath = if (imagesList.isNotEmpty()) imagesList[0] else ""
            val closeImgPath = if (imagesList.size > 1) imagesList[1] else if (isClosed && imagesList.isNotEmpty()) imagesList[0] else ""

            val openImgBase64 = com.example.util.CompressionUtils.convertUriOrPathToBase64(getApplication(), openImgPath)
            val closeImgBase64 = com.example.util.CompressionUtils.convertUriOrPathToBase64(getApplication(), closeImgPath)

            val openDataUri = com.example.util.CompressionUtils.getDataUri(getApplication(), openImgPath)
            val closeDataUri = com.example.util.CompressionUtils.getDataUri(getApplication(), closeImgPath)

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
                val b64 = com.example.util.CompressionUtils.convertUriOrPathToBase64(getApplication(), imgPath)
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

            // Solutions & Solution
            put("Solutions", workLog.solutions)
            put("solutions", workLog.solutions)
            put("Solution", workLog.solutions)
            put("solution", workLog.solutions)
            put("Resolution", workLog.solutions)

            // Technician
            put("Technician", workLog.technician)
            put("technician", workLog.technician)
            put("TechName", workLog.technician)
            put("tech_name", workLog.technician)

            // Check IN & Check OUT details
            put("checkInTime", workLog.checkInTime ?: 0L)
            put("checkOutTime", workLog.checkOutTime ?: 0L)
            put("checkInTimeString", workLog.formattedCheckInTime ?: "")
            put("checkOutTimeString", workLog.formattedCheckOutTime ?: "")
            put("checkInImageUri", workLog.checkInImageUri ?: "")
            put("checkOutImageUri", workLog.checkOutImageUri ?: "")

            put("syncStatus", workLog.syncStatus)

            // Closed Summary Template
            put("closedSummary", workLog.buildClosedSummaryTemplate())
        }.toString()
    }

    private fun syncToGoogleSheets(workLog: WorkLog) {
        val webhookUrl = prefs.getString("google_sheets_webhook_url", googleSheetsWebhookUrl.value) ?: googleSheetsWebhookUrl.value
        if (webhookUrl.isBlank() || webhookUrl.contains("placeholder") || !webhookUrl.startsWith("http")) {
            android.util.Log.d("GoogleSheetSync", "Webhook URL is blank or placeholder. Scheduling background WorkManager sync.")
            syncState.value = SyncState.ERROR
            com.example.sync.SyncScheduler.scheduleSync(getApplication())
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            syncState.value = SyncState.UPLOADING
            try {
                val jsonPayload = buildWorkLogJsonPayload(workLog)

                val res = com.example.util.GoogleScriptNetworkClient.executePost(webhookUrl, jsonPayload)
                if (res is com.example.util.GoogleScriptNetworkClient.ResponseResult.Success) {
                    android.util.Log.d("GoogleSheetSync", "Google Sheet sync successful!")
                    val now = System.currentTimeMillis()
                    prefs.edit().putLong("last_synced_time", now).apply()
                    lastSyncedTime.value = now
                    syncState.value = SyncState.SYNCED
                    repository.updateSyncStatus(workLog.id, WorkLog.SYNC_SYNCED)
                } else {
                    android.util.Log.e("GoogleSheetSync", "Google Sheet sync failed")
                    syncState.value = SyncState.ERROR
                    repository.updateSyncStatus(workLog.id, WorkLog.SYNC_FAILED)
                    com.example.sync.SyncScheduler.scheduleSync(getApplication())
                }
            } catch (e: java.lang.Exception) {
                android.util.Log.e("GoogleSheetSync", "Error syncing to Google Sheets. Queuing for WorkManager.", e)
                syncState.value = SyncState.ERROR
                repository.updateSyncStatus(workLog.id, WorkLog.SYNC_FAILED)
                com.example.sync.SyncScheduler.scheduleSync(getApplication())
            }
        }
    }

    fun createCase(
        rawText: String,
        customCategory: String? = null,
        customTimestamp: Long? = null,
        imageUri: String? = null,
        priority: String = WorkLog.PRIORITY_MEDIUM,
        technicianId: String = currentTechnician?.id ?: "",
        technicianName: String = currentTechnician?.name ?: "",
        createdBy: String = "technician",
        onError: ((String) -> Unit)? = null,
        onComplete: () -> Unit = {}
    ) {
        if (currentCaseStatus.value == ActiveCaseStatus.CLOSED || !isCaseActive.value) {
            val errorMsg = "ไม่สามารถบันทึกงานใหม่ได้ เนื่องจากสถานะเคสอยู่ในสถานะ ปิด (Closed Case)"
            onError?.invoke(errorMsg)
            return
        }
        if (rawText.isBlank()) {
            onError?.invoke("กรุณากรอกข้อความรายละเอียดงาน")
            return
        }

        val encryptedPrefs = com.example.util.EncryptedPrefsManager(getApplication())
        val finalTechName = technicianName.trim().ifBlank {
            currentTechnician?.name?.trim()?.takeIf { it.isNotBlank() }
                ?: encryptedPrefs.getSavedTechnicianName()
                ?: encryptedPrefs.getTechnicianName()
                ?: lastTechnicianName.value.trim().takeIf { it.isNotBlank() }
                ?: "ช่างประจำเคส"
        }
        if (finalTechName.isNotBlank()) {
            lastTechnicianName.value = finalTechName
            prefs.edit().putString("last_technician", finalTechName).apply()
        }

        val currentAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val finalTechId = currentAuthUid?.trim()?.takeIf { it.isNotBlank() }
            ?: technicianId.trim().ifBlank {
                currentTechnician?.id?.trim()?.takeIf { it.isNotBlank() }
                    ?: encryptedPrefs.getSavedTechnicianId()
                    ?: encryptedPrefs.getTechnicianId()
                    ?: "unknown_technician"
            }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                isAnalyzing.value = true
                val cleanedText = com.example.util.CompressionUtils.cleanAndCompressText(rawText)

                val finalCategory = customCategory ?: run {
                    val classification = AiCategorizer.categorizeText(cleanedText)
                    classification.category
                }

                val targetTimestamp = customTimestamp ?: System.currentTimeMillis()

                val processedImageUri = com.example.util.CompressionUtils.compressMultipleImages(
                    getApplication(),
                    imageUri
                )

                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = targetTimestamp
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis

                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                cal.set(java.util.Calendar.MILLISECOND, 999)
                val endOfDay = cal.timeInMillis

                val taskCountOnDay = repository.getTaskCountForDayRangeDirect(startOfDay, endOfDay)
                val seq = taskCountOnDay + 1
                val sdf = java.text.SimpleDateFormat("ddMMyy", java.util.Locale.US)
                val dateFormatted = sdf.format(java.util.Date(targetTimestamp))
                val generatedCaseNo = "$dateFormatted${String.format(java.util.Locale.US, "%03d", seq)}"

                val newLog = WorkLog(
                    id = generatedCaseNo,
                    rawText = cleanedText,
                    category = finalCategory,
                    status = WorkLog.STATUS_OPEN,
                    solutions = "",
                    technician = finalTechName,
                    timestamp = targetTimestamp,
                    imageUri = processedImageUri,
                    syncStatus = WorkLog.SYNC_PENDING,
                    caseNumber = generatedCaseNo,
                    priority = priority
                )

                repository.insert(newLog)
                com.example.util.ReminderScheduler.scheduleReminder(getApplication(), newLog)
                com.example.widget.WorkLogWidgetProvider.updateAllWidgets(getApplication())

                // Sync to Firebase Firestore with AES Encryption and Storage Image Uploads
                try {
                    val extracted = com.example.ai.CustomerInfoExtractor.extract(cleanedText)
                    
                    var uploadedUrls: List<String> = emptyList()
                    if (!processedImageUri.isNullOrBlank()) {
                        try {
                            uploadedUrls = firestoreRepository.uploadImagesAndGetUrls(
                                caseId = generatedCaseNo,
                                imageUriString = processedImageUri,
                                prefix = "attach"
                            )
                            android.util.Log.d("WorkLogViewModel", "Uploaded new case images to Firebase Storage: $uploadedUrls")
                        } catch (stEx: Exception) {
                            android.util.Log.e("WorkLogViewModel", "Firebase Storage upload error: ${stEx.message}", stEx)
                        }
                    }

                    val primaryImageUrl = uploadedUrls.firstOrNull() ?: ""

                    val repairCase = RepairCase.createEncryptedCase(
                        caseId = generatedCaseNo,
                        customerName = extracted.customerName ?: "ลูกค้าทั่วไป",
                        plainPhone = extracted.phone ?: "",
                        plainAddress = extracted.buildingOrAddress ?: "",
                        details = cleanedText,
                        status = WorkLog.STATUS_OPEN,
                        technicianName = finalTechName,
                        imageUrl = primaryImageUrl,
                        imageUrls = uploadedUrls
                    ).copy(
                        technicianId = finalTechId,
                        assignedTechId = finalTechId
                    )

                    firestoreRepository.saveCase(
                        case = repairCase,
                        technicianId = finalTechId,
                        technicianName = finalTechName,
                        createdBy = createdBy,
                        onSuccess = {
                            android.util.Log.d("WorkLogViewModel", "Firestore synced with technicianId: $finalTechId, imageUrl: ${repairCase.imageUrl}")
                            logActivity(
                                technicianId = finalTechId,
                                technicianName = finalTechName,
                                action = "OPEN_CASE",
                                caseId = generatedCaseNo,
                                caseTitle = repairCase.customerName.ifBlank { "ลูกค้าทั่วไป" }
                            )
                        },
                        onError = { firebaseEx ->
                            android.util.Log.e("WorkLogViewModel", "Firebase save failed: ${firebaseEx.message}", firebaseEx)
                        }
                    )
                } catch (fbEx: Exception) {
                    android.util.Log.e("WorkLogViewModel", "Firebase Firestore exception", fbEx)
                }

                triggerAutoUpload()
                syncToGoogleSheets(newLog)
                isAnalyzing.value = false
                aiResult.value = null

                searchQuery.value = ""
                selectedCategory.value = null
                if (filterStatus.value == WorkLog.STATUS_CLOSED) {
                    filterStatus.value = "ทั้งหมด"
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                isAnalyzing.value = false
                android.util.Log.e("WorkLogViewModel", "createCase error", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError?.invoke("บันทึกไม่สำเร็จ: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun createWorkLog(
        rawText: String,
        customCategory: String? = null,
        customTimestamp: Long? = null,
        imageUri: String? = null,
        priority: String = WorkLog.PRIORITY_MEDIUM,
        customTechnician: String? = null,
        onError: ((String) -> Unit)? = null,
        onComplete: () -> Unit = {}
    ) {
        createCase(
            rawText = rawText,
            customCategory = customCategory,
            customTimestamp = customTimestamp,
            imageUri = imageUri,
            priority = priority,
            technicianId = currentTechnician?.id ?: "",
            technicianName = customTechnician ?: currentTechnician?.name ?: "",
            createdBy = "technician",
            onError = onError,
            onComplete = onComplete
        )
    }

    fun analyzeTextForCategory(rawText: String) {
        if (rawText.isBlank()) {
            aiResult.value = null
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isAnalyzing.value = true
            val classification = AiCategorizer.categorizeText(rawText)
            aiResult.value = classification
            isAnalyzing.value = false
        }
    }

    fun closeCase(
        logId: String,
        solutions: String,
        technician: String,
        imageUri: String? = null,
        onError: ((String) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val trimmedTech = technician.trim()
                if (trimmedTech.isNotBlank()) {
                    prefs.edit().putString("last_technician", trimmedTech).apply()
                    lastTechnicianName.value = trimmedTech
                }

                // Compress attached image(s) if provided
                val processedImageUri = com.example.util.CompressionUtils.compressMultipleImages(
                    getApplication(),
                    imageUri
                )

                val cleanedSolutions = com.example.util.CompressionUtils.cleanAndCompressText(solutions)

                // Find current log with direct DB query first for guaranteed hit
                val currentLog = repository.getLogByIdDirect(logId)
                    ?: allLogs.value.find { it.id == logId }
                    ?: filteredWorkLogs.value.find { it.id == logId }

                if (currentLog != null) {
                    if (!currentLog.isCheckedOut) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onError?.invoke("ไม่สามารถปิดเคสได้: กรุณากด Check OUT หน้างานก่อน")
                        }
                        return@launch
                    }

                    val finalImageUri = when {
                        !processedImageUri.isNullOrBlank() && !currentLog.imageUri.isNullOrBlank() -> {
                            val existingList = currentLog.imageUriList
                            val newList = processedImageUri.split("|").map { it.trim() }.filter { it.isNotBlank() }
                            (existingList + newList).distinct().joinToString("|")
                        }
                        !processedImageUri.isNullOrBlank() -> processedImageUri
                        else -> currentLog.imageUri
                    }

                    val updatedLog = currentLog.copy(
                        status = WorkLog.STATUS_CLOSED,
                        solutions = cleanedSolutions,
                        technician = trimmedTech,
                        imageUri = finalImageUri,
                        syncStatus = WorkLog.SYNC_PENDING
                    )
                    com.example.util.ReminderScheduler.cancelReminder(getApplication(), currentLog)
                    repository.update(updatedLog)
                    updateLogInLiveList(updatedLog)
                    com.example.widget.WorkLogWidgetProvider.updateAllWidgets(getApplication())

                    // Sync to Firestore
                    try {
                        val extracted = com.example.ai.CustomerInfoExtractor.extract(currentLog.rawText)
                        val caseDocId = currentLog.caseNumber.ifBlank { currentLog.id }

                        val encryptedPrefs = com.example.util.EncryptedPrefsManager(getApplication())
                        val currentAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        val resolvedTechId = currentAuthUid?.trim()?.takeIf { it.isNotBlank() }
                            ?: currentTechnician?.id?.trim()?.takeIf { it.isNotBlank() }
                            ?: encryptedPrefs.getSavedTechnicianId()
                            ?: encryptedPrefs.getTechnicianId()
                            ?: "unknown_technician"
                        val resolvedTechName = trimmedTech.trim().ifBlank {
                            currentTechnician?.name?.trim()?.takeIf { it.isNotBlank() }
                                ?: encryptedPrefs.getSavedTechnicianName()
                                ?: encryptedPrefs.getTechnicianName()
                                ?: "ช่างประจำเคส"
                        }

                        val repairCase = RepairCase.createEncryptedCase(
                            caseId = caseDocId,
                            customerName = extracted.customerName ?: "ลูกค้าทั่วไป",
                            plainPhone = extracted.phone ?: "",
                            plainAddress = extracted.buildingOrAddress ?: "",
                            details = currentLog.rawText,
                            status = WorkLog.STATUS_CLOSED,
                            technicianName = resolvedTechName
                        ).copy(
                            technicianId = resolvedTechId,
                            assignedTechId = resolvedTechId
                        )
                        firestoreRepository.saveCase(
                            case = repairCase,
                            technicianId = resolvedTechId,
                            technicianName = resolvedTechName,
                            onSuccess = {
                                android.util.Log.d("WorkLogViewModel", "Firestore closed case updated: ${repairCase.caseId}")
                                logActivity(
                                    technicianId = resolvedTechId,
                                    technicianName = resolvedTechName,
                                    action = "CLOSE_CASE",
                                    caseId = caseDocId,
                                    caseTitle = extracted.customerName ?: "ลูกค้าทั่วไป"
                                )
                            },
                            onError = { e ->
                                android.util.Log.e("WorkLogViewModel", "Firestore close case error: ${e.message}")
                            }
                        )

                        // Explicitly update close status, solutions, closedBy, timestamps to Firestore
                        val closeFields = mutableMapOf<String, Any?>(
                            "status" to "CLOSED",
                            "solutions" to cleanedSolutions,
                            "closedBy" to resolvedTechName,
                            "closedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        if (currentLog.checkInTime != null && currentLog.checkInTime!! > 0) {
                            closeFields["checkInTime"] = currentLog.checkInTime
                            closeFields["checkInTimeString"] = currentLog.formattedCheckInTime ?: ""
                        }
                        if (currentLog.checkOutTime != null && currentLog.checkOutTime!! > 0) {
                            closeFields["checkOutTime"] = currentLog.checkOutTime
                            closeFields["checkOutTimeString"] = currentLog.formattedCheckOutTime ?: ""
                        }

                        firestoreRepository.updateCaseFields(
                            caseId = caseDocId,
                            fields = closeFields,
                            userId = resolvedTechId
                        )

                        // Mark any pending notifications as read immediately so they don't alert anymore
                        markCaseNotificationsAsRead(caseDocId)
                        if (currentLog.caseNumber.isNotBlank()) {
                            markCaseNotificationsAsRead(currentLog.caseNumber)
                        }

                        // Upload closing images and sync URLs to Firestore
                        if (!finalImageUri.isNullOrBlank()) {
                            firestoreRepository.uploadAndSyncCaseImages(
                                caseId = caseDocId,
                                imageUriString = finalImageUri,
                                checkInImageUri = currentLog.checkInImageUri,
                                checkOutImageUri = currentLog.checkOutImageUri,
                                onSuccess = { urls ->
                                    android.util.Log.d("WorkLogViewModel", "Uploaded closing images to Firebase Storage: $urls")
                                },
                                onError = { ex ->
                                    android.util.Log.e("WorkLogViewModel", "Failed to upload closing images to Storage: ${ex.message}")
                                }
                            )
                        }
                    } catch (_: Exception) {}

                    // Auto Push to LINE Bot if configured
                    val token = prefs.getString("line_bot_channel_token", lineBotChannelToken.value)?.trim() ?: lineBotChannelToken.value.trim()
                    val targetId = prefs.getString("line_bot_target_id", lineBotTargetId.value)?.trim() ?: lineBotTargetId.value.trim()
                    val shouldNotify = prefs.getBoolean("line_bot_auto_notify_close_case", lineBotAutoNotifyCloseCase.value)

                    android.util.Log.d("LineMessagingApi", "closeCase -> shouldNotify=$shouldNotify, tokenLength=${token.length}, targetId=$targetId")

                    if (shouldNotify && token.isNotBlank() && targetId.isNotBlank()) {
                        try {
                            val pushRes = com.example.util.LineMessagingApiClient.sendClosedCaseNotification(
                                token = token,
                                targetId = targetId,
                                workLog = updatedLog,
                                solutions = cleanedSolutions,
                                technician = trimmedTech,
                                context = getApplication()
                            )
                            android.util.Log.d("LineMessagingApi", "LINE Bot closed case push result: $pushRes")
                        } catch (e: Exception) {
                            android.util.Log.e("WorkLogViewModel", "LINE Bot closed case notification failed", e)
                        }
                    }

                    triggerAutoUpload()
                    syncToGoogleSheets(updatedLog)

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete?.invoke()
                    }
                } else {
                    android.util.Log.e("WorkLogViewModel", "closeCase failed: log with id $logId not found")
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError?.invoke("ไม่พบข้อมูลเคส ID: $logId")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkLogViewModel", "closeCase exception", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError?.invoke("ปิดเคสไม่สำเร็จ: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun pushClosedCaseToLineBot(workLog: WorkLog, onComplete: (com.example.util.LineMessagingApiClient.LineApiResult) -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val token = prefs.getString("line_bot_channel_token", lineBotChannelToken.value)?.trim() ?: lineBotChannelToken.value.trim()
            val targetId = prefs.getString("line_bot_target_id", lineBotTargetId.value)?.trim() ?: lineBotTargetId.value.trim()

            if (token.isBlank() || targetId.isBlank()) {
                val res = com.example.util.LineMessagingApiClient.LineApiResult(false, 0, "กรุณากรอก Token และ Target ID ในหน้าตั้งค่า LINE Bot ก่อน")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(res)
                }
                return@launch
            }

            val pushRes = com.example.util.LineMessagingApiClient.sendClosedCaseNotification(
                token = token,
                targetId = targetId,
                workLog = workLog,
                solutions = workLog.solutions,
                technician = workLog.technician,
                context = getApplication()
            )

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(pushRes)
            }
        }
    }

    fun reopenCase(logId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val currentLog = filteredWorkLogs.value.find { it.id == logId } ?: allLogs.value.find { it.id == logId }
            if (currentLog != null) {
                val updatedLog = currentLog.copy(
                    status = WorkLog.STATUS_OPEN,
                    syncStatus = WorkLog.SYNC_PENDING
                )
                repository.update(updatedLog)
                com.example.util.ReminderScheduler.scheduleReminder(getApplication(), updatedLog)
                com.example.widget.WorkLogWidgetProvider.updateAllWidgets(getApplication())
                triggerAutoUpload()
                syncToGoogleSheets(updatedLog)
            }
        }
    }

    fun updateWorkLog(
        workLog: WorkLog,
        newRawText: String? = null,
        newCategory: String? = null,
        newSolutions: String? = null,
        newTechnician: String? = null,
        newStatus: String? = null,
        newPriority: String? = null,
        onError: ((String) -> Unit)? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val updatedLog = workLog.copy(
                    rawText = newRawText ?: workLog.rawText,
                    category = newCategory ?: workLog.category,
                    solutions = newSolutions ?: workLog.solutions,
                    technician = newTechnician ?: workLog.technician,
                    status = newStatus ?: workLog.status,
                    priority = newPriority ?: workLog.priority,
                    syncStatus = WorkLog.SYNC_PENDING
                )
                repository.update(updatedLog)
                updateLogInLiveList(updatedLog)
                com.example.widget.WorkLogWidgetProvider.updateAllWidgets(getApplication())

                // Sync to Firestore
                try {
                    val extracted = com.example.ai.CustomerInfoExtractor.extract(updatedLog.rawText)
                    val caseDocId = updatedLog.caseNumber.ifBlank { updatedLog.id }

                    val encryptedPrefs = com.example.util.EncryptedPrefsManager(getApplication())
                    val currentAuthUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    val resolvedTechId = currentAuthUid?.trim()?.takeIf { it.isNotBlank() }
                        ?: currentTechnician?.id?.trim()?.takeIf { it.isNotBlank() }
                        ?: encryptedPrefs.getSavedTechnicianId()
                        ?: encryptedPrefs.getTechnicianId()
                        ?: "unknown_technician"
                    val resolvedTechName = updatedLog.technician.trim().ifBlank {
                        currentTechnician?.name?.trim()?.takeIf { it.isNotBlank() }
                            ?: encryptedPrefs.getSavedTechnicianName()
                            ?: encryptedPrefs.getTechnicianName()
                            ?: "ช่างประจำเคส"
                    }

                    val repairCase = RepairCase.createEncryptedCase(
                        caseId = caseDocId,
                        customerName = extracted.customerName ?: "ลูกค้าทั่วไป",
                        plainPhone = extracted.phone ?: "",
                        plainAddress = extracted.buildingOrAddress ?: "",
                        details = updatedLog.rawText,
                        status = updatedLog.status,
                        technicianName = resolvedTechName
                    ).copy(
                        technicianId = resolvedTechId,
                        assignedTechId = resolvedTechId
                    )
                    firestoreRepository.saveCase(
                        case = repairCase,
                        technicianId = resolvedTechId,
                        technicianName = resolvedTechName,
                        onSuccess = {
                            android.util.Log.d("WorkLogViewModel", "Firestore update case synced: ${repairCase.caseId}")
                            if (updatedLog.status == WorkLog.STATUS_CLOSED || newStatus == WorkLog.STATUS_CLOSED || newStatus == "ปิดเคส") {
                                logActivity(
                                    technicianId = resolvedTechId,
                                    technicianName = resolvedTechName,
                                    action = "CLOSE_CASE",
                                    caseId = caseDocId,
                                    caseTitle = extracted.customerName ?: "ลูกค้าทั่วไป"
                                )
                            }
                        },
                        onError = { e ->
                            android.util.Log.e("WorkLogViewModel", "Firestore update case error: ${e.message}")
                        }
                    )

                    // Upload images to Storage if present
                    if (!updatedLog.imageUri.isNullOrBlank()) {
                        firestoreRepository.uploadAndSyncCaseImages(
                            caseId = caseDocId,
                            imageUriString = updatedLog.imageUri,
                            checkInImageUri = updatedLog.checkInImageUri,
                            checkOutImageUri = updatedLog.checkOutImageUri
                        )
                    }
                } catch (_: Exception) {}

                triggerAutoUpload()
                syncToGoogleSheets(updatedLog)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkLogViewModel", "updateWorkLog error", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError?.invoke("อัปเดตไม่สำเร็จ: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    private fun updateLogInLiveList(updatedLog: WorkLog) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val currentList = _firestoreWorkLogs.value.toMutableList()
            val index = currentList.indexOfFirst { 
                it.id == updatedLog.id || (it.caseNumber.isNotBlank() && it.caseNumber == updatedLog.caseNumber) 
            }
            if (index != -1) {
                currentList[index] = updatedLog
                _firestoreWorkLogs.value = currentList
            } else {
                currentList.add(0, updatedLog)
                _firestoreWorkLogs.value = currentList
            }
        }
    }

    fun recordCheckIn(
        workLog: WorkLog,
        imageUri: android.net.Uri,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val checkInTime = System.currentTimeMillis()
                val stampedPath = com.example.util.ImageExportUtils.stampAndSaveCheckInOutImage(
                    context = getApplication(),
                    imageUri = imageUri,
                    isCheckIn = true,
                    caseNumber = workLog.formattedCaseNumber,
                    timestamp = checkInTime
                )
                if (stampedPath.isNullOrBlank()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError("ไม่สามารถประทับเวลาลงบนรูปภาพได้")
                    }
                    return@launch
                }

                val existingList = workLog.imageUriList
                val newList = (existingList + stampedPath).distinct().joinToString(" | ")

                val updatedLog = workLog.copy(
                    checkInTime = checkInTime,
                    checkInImageUri = stampedPath,
                    imageUri = newList,
                    syncStatus = WorkLog.SYNC_PENDING
                )
                repository.update(updatedLog)
                updateLogInLiveList(updatedLog)
                com.example.widget.WorkLogWidgetProvider.updateAllWidgets(getApplication())
                triggerAutoUpload()
                syncToGoogleSheets(updatedLog)

                val formattedTime = updatedLog.formattedCheckInTime ?: ""
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess("Check IN สำเร็จ! บันทึกภาพลงคลังภาพมือถือแล้ว ($formattedTime)")
                }

                // Sync stamped check-in image to Firebase Storage & update Firestore document
                try {
                    val caseDocId = updatedLog.caseNumber.ifBlank { updatedLog.id }

                    // Sync Check In timestamp fields to Firestore
                    val checkInFields = mutableMapOf<String, Any?>(
                        "checkInTime" to checkInTime,
                        "checkInTimeString" to (updatedLog.formattedCheckInTime ?: ""),
                        "checkInAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    firestoreRepository.updateCaseFields(
                        caseId = caseDocId,
                        fields = checkInFields
                    )

                    firestoreRepository.uploadAndSyncCaseImages(
                        caseId = caseDocId,
                        checkInImageUri = stampedPath,
                        onSuccess = { urls ->
                            android.util.Log.d("WorkLogViewModel", "Synced Check IN photo to Firebase Storage: $urls")
                        },
                        onError = { ex ->
                            android.util.Log.e("WorkLogViewModel", "Failed to sync Check IN photo to Firebase Storage: ${ex.message}")
                        }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("WorkLogViewModel", "Check IN Storage sync exception", e)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError("เกิดข้อผิดพลาดในการ Check IN: ${e.localizedMessage}")
                }
            }
        }
    }

    fun recordCheckOut(
        workLog: WorkLog,
        imageUri: android.net.Uri,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val checkOutTime = System.currentTimeMillis()
                val stampedPath = com.example.util.ImageExportUtils.stampAndSaveCheckInOutImage(
                    context = getApplication(),
                    imageUri = imageUri,
                    isCheckIn = false,
                    caseNumber = workLog.formattedCaseNumber,
                    timestamp = checkOutTime
                )
                if (stampedPath.isNullOrBlank()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError("ไม่สามารถประทับเวลาลงบนรูปภาพได้")
                    }
                    return@launch
                }

                val existingList = workLog.imageUriList
                val newList = (existingList + stampedPath).distinct().joinToString(" | ")

                val updatedLog = workLog.copy(
                    checkOutTime = checkOutTime,
                    checkOutImageUri = stampedPath,
                    imageUri = newList,
                    syncStatus = WorkLog.SYNC_PENDING
                )
                repository.update(updatedLog)
                updateLogInLiveList(updatedLog)
                com.example.widget.WorkLogWidgetProvider.updateAllWidgets(getApplication())
                triggerAutoUpload()
                syncToGoogleSheets(updatedLog)

                val formattedTime = updatedLog.formattedCheckOutTime ?: ""
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess("Check OUT สำเร็จ! บันทึกภาพลงคลังภาพมือถือแล้ว ($formattedTime)")
                }

                // Sync stamped check-out image to Firebase Storage & update Firestore document
                try {
                    val caseDocId = updatedLog.caseNumber.ifBlank { updatedLog.id }

                    // Sync Check Out timestamp fields to Firestore
                    val checkOutFields = mutableMapOf<String, Any?>(
                        "checkOutTime" to checkOutTime,
                        "checkOutTimeString" to (updatedLog.formattedCheckOutTime ?: ""),
                        "checkOutAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    firestoreRepository.updateCaseFields(
                        caseId = caseDocId,
                        fields = checkOutFields
                    )

                    firestoreRepository.uploadAndSyncCaseImages(
                        caseId = caseDocId,
                        checkOutImageUri = stampedPath,
                        onSuccess = { urls ->
                            android.util.Log.d("WorkLogViewModel", "Synced Check OUT photo to Firebase Storage: $urls")
                        },
                        onError = { ex ->
                            android.util.Log.e("WorkLogViewModel", "Failed to sync Check OUT photo to Firebase Storage: ${ex.message}")
                        }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("WorkLogViewModel", "Check OUT Storage sync exception", e)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError("เกิดข้อผิดพลาดในการ Check OUT: ${e.localizedMessage}")
                }
            }
        }
    }

    fun performCheckOut(
        logId: String,
        imageUri: android.net.Uri,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val workLog = allLogs.value.find { it.id == logId }
        if (workLog != null) {
            recordCheckOut(workLog, imageUri, onSuccess, onError)
        } else {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val dbLog = repository.getLogByIdDirect(logId)
                if (dbLog != null) {
                    recordCheckOut(dbLog, imageUri, onSuccess, onError)
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError("ไม่พบข้อมูลบันทึกงาน ID: $logId")
                    }
                }
            }
        }
    }

    fun deleteLog(workLog: WorkLog) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. ลบ local Room และ components ที่เกี่ยวข้อง
                com.example.util.ReminderScheduler.cancelReminder(getApplication(), workLog)
                repository.delete(workLog)
                com.example.widget.WorkLogWidgetProvider.updateAllWidgets(getApplication())
                deleteFromGoogleSheets(workLog)

                // 2. ลบ Firestore ด้วย (ถ้ามี caseNumber หรือ id)
                val caseId = (workLog.caseNumber ?: "").ifBlank { workLog.id }
                if (caseId.isNotBlank()) {
                    firestoreRepository.deleteCase(
                        caseId = caseId,
                        onSuccess = {
                            android.util.Log.d("WorkLogViewModel", "Firestore case deleted successfully: $caseId")
                        },
                        onError = { e ->
                            android.util.Log.e("WorkLogViewModel", "Failed to delete from Firestore: ${e.message}", e)
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("WorkLogViewModel", "deleteLog error: ${e.message}", e)
            }
        }
    }

    fun clearAllLogs(onComplete: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            allLogs.value.forEach { log ->
                com.example.util.ReminderScheduler.cancelReminder(getApplication(), log)
            }
            repository.deleteAll()
            repository.optimizeDatabase()
            com.example.widget.WorkLogWidgetProvider.updateAllWidgets(getApplication())
            deleteAllFromGoogleSheets()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private fun deleteFromGoogleSheets(workLog: WorkLog) {
        val webhookUrl = prefs.getString("google_sheets_webhook_url", googleSheetsWebhookUrl.value) ?: googleSheetsWebhookUrl.value
        if (webhookUrl.isBlank() || webhookUrl.contains("placeholder") || !webhookUrl.startsWith("http")) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val rawCaseNo = workLog.rawCaseNumber
                val formattedCaseNo = workLog.formattedCaseNumber

                val jsonPayload = org.json.JSONObject().apply {
                    put("action", "delete")
                    put("type", "delete")
                    put("Action", "delete")
                    put("Type", "delete")
                    put("status", "delete")
                    put("Status", "delete")

                    put("CaseID", rawCaseNo)
                    put("caseId", rawCaseNo)
                    put("case_id", rawCaseNo)
                    put("Case_ID", rawCaseNo)
                    put("CaseId", rawCaseNo)
                    put("caseNumber", rawCaseNo)
                    put("case_number", rawCaseNo)
                    put("CaseNumber", rawCaseNo)
                    put("caseNo", formattedCaseNo)
                    put("id", workLog.id)
                }.toString()
                com.example.util.GoogleScriptNetworkClient.executePost(webhookUrl, jsonPayload)
            } catch (e: Exception) {
                android.util.Log.e("GoogleSheetDelete", "Error deleting from Google Sheet", e)
            }
        }
    }

    private fun deleteAllFromGoogleSheets() {
        val webhookUrl = prefs.getString("google_sheets_webhook_url", googleSheetsWebhookUrl.value) ?: googleSheetsWebhookUrl.value
        if (webhookUrl.isBlank() || webhookUrl.contains("placeholder") || !webhookUrl.startsWith("http")) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonPayload = org.json.JSONObject().apply {
                    put("action", "deleteAll")
                    put("type", "deleteAll")
                    put("Action", "deleteAll")
                    put("Type", "deleteAll")
                    put("status", "deleteAll")
                    put("Status", "deleteAll")
                }.toString()
                com.example.util.GoogleScriptNetworkClient.executePost(webhookUrl, jsonPayload)
            } catch (e: Exception) {
                android.util.Log.e("GoogleSheetDeleteAll", "Error deleting all from Google Sheet", e)
            }
        }
    }

    fun optimizeDatabase(onComplete: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.optimizeDatabase()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun pruneOldLogs(daysToKeep: Int = 30, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pruned = repository.pruneOldSyncedClosedLogs(daysToKeep)
            com.example.widget.WorkLogWidgetProvider.updateAllWidgets(getApplication())
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(pruned)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // ปิดและยกเลิก Firestore SnapshotListener ทันทีเมื่อ ViewModel ถูกทำลาย เพื่อป้องกัน memory leak
        casesSnapshotListener?.remove()
        casesSnapshotListener = null
        realtimeUpdateListener?.remove()
        realtimeUpdateListener = null
        realtimeCasesJob?.cancel()
        notificationsJob?.cancel()
    }

    // =========================================================================
    // Patch & Software Update Methods
    // =========================================================================

    fun startListeningForUpdates() {
        try {
            realtimeUpdateListener?.remove()
            realtimeUpdateListener = updateManager.startRealtimeUpdateListener { info ->
                availableUpdateInfo.value = info
                showUpdateAlertBanner.value = true
            }
            // Background initial check
            checkForUpdates(manual = false)
        } catch (e: Exception) {
            android.util.Log.w("WorkLogViewModel", "Error starting update listener: ${e.message}")
        }
    }

    fun checkForUpdates(
        manual: Boolean = false,
        customUrl: String? = null,
        onResult: ((Boolean, AppVersionInfo?) -> Unit)? = null
    ) {
        isCheckingUpdate.value = true
        if (manual) {
            updateCheckStatusMessage.value = "กำลังตรวจสอบเวอร์ชันล่าสุด..."
        }
        val urlToUse = customUrl ?: customPatchUrl.value.ifBlank { null }
        updateManager.checkForUpdates(
            customUrl = urlToUse,
            onUpdateAvailable = { info ->
                isCheckingUpdate.value = false
                availableUpdateInfo.value = info
                showUpdateAlertBanner.value = true
                val now = System.currentTimeMillis()
                lastUpdateCheckTime.value = now
                prefs.edit().putLong("last_update_check_time", now).apply()
                updateCheckStatusMessage.value = "พบ Patch ใหม่: v${info.versionName}"
                onResult?.invoke(true, info)
            },
            onNoUpdate = {
                isCheckingUpdate.value = false
                val now = System.currentTimeMillis()
                lastUpdateCheckTime.value = now
                prefs.edit().putLong("last_update_check_time", now).apply()
                updateCheckStatusMessage.value = "คุณกำลังใช้งานเวอร์ชันล่าสุดแล้ว (v${com.example.BuildConfig.VERSION_NAME})"
                onResult?.invoke(false, null)
            },
            onError = { err ->
                isCheckingUpdate.value = false
                updateCheckStatusMessage.value = "ตรวจสอบไม่สำเร็จ: $err"
                onResult?.invoke(false, null)
            }
        )
    }

    fun downloadAndInstallPatch(apkUrl: String) {
        if (apkUrl.isBlank()) return
        isDownloadingPatch.value = true
        updateManager.downloadAndInstallApk(
            apkUrl = apkUrl,
            fileName = "worklog_patch_${System.currentTimeMillis()}.apk",
            onDownloadStarted = {
                isDownloadingPatch.value = false
            }
        )
    }

    fun dismissUpdateBanner() {
        showUpdateAlertBanner.value = false
    }

    fun restoreUpdateBanner() {
        showUpdateAlertBanner.value = true
    }

    fun saveCustomPatchUrl(url: String) {
        val trimmed = url.trim()
        customPatchUrl.value = trimmed
        prefs.edit().putString("custom_patch_url", trimmed).apply()
    }

    fun simulateTestPatch(onSimulated: ((AppVersionInfo) -> Unit)? = null) {
        val nextVersionCode = com.example.BuildConfig.VERSION_CODE + 1
        val simulatedInfo = AppVersionInfo(
            versionCode = nextVersionCode,
            versionName = "5.1.0-Patch",
            apkUrl = "https://github.com/aistudio/worklog-assistant/releases/download/v5.1.0/worklog-patch.apk",
            changelog = "• เพิ่มระบบปุ่มแจ้งเตือนเมื่อมีอัปเดตเวอร์ชันใหม่\n• เพิ่มเมนูดาวน์โหลดและติดตั้ง Patch ในหน้าตั้งค่า\n• ปรับปรุงประสิทธิภาพและความรวดเร็วในการทำงาน",
            releaseDate = "6 ก.ย. 2026",
            patchSize = "18.4 MB",
            isCritical = false
        )
        availableUpdateInfo.value = simulatedInfo
        showUpdateAlertBanner.value = true
        updateManager.sendUpdateNotification(simulatedInfo)
        updateCheckStatusMessage.value = "จำลองพบ Patch ใหม่ v${simulatedInfo.versionName} เรียบร้อย"
        onSimulated?.invoke(simulatedInfo)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WorkLogViewModel::class.java)) {
                val database = WorkLogDatabase.getDatabase(application)
                val repo = WorkLogRepository(database)
                return WorkLogViewModel(application, repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

enum class SyncState {
    SYNCED,
    UPLOADING,
    RETRIEVING,
    ERROR
}

data class TestConnectionResult(
    val isSuccess: Boolean,
    val message: String,
    val statusCode: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val responseBody: String? = null
)

data class ActivityLog(
    val id: String = "",
    val technicianId: String = "",
    val technicianName: String = "",
    val action: String = "",
    val caseId: String? = null,
    val caseTitle: String? = null,
    val timestamp: com.google.firebase.Timestamp? = null,
    val deviceInfo: String? = null
)
