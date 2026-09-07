package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "work_logs",
    indices = [
        Index("status"),
        Index("timestamp"),
        Index("syncStatus")
    ]
)
data class WorkLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val rawText: String,
    val category: String,
    val status: String = STATUS_OPEN,
    val solutions: String = "",
    val technician: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val syncStatus: String = SYNC_PENDING,
    val caseNumber: String = "",
    val priority: String = PRIORITY_MEDIUM,
    val checkInTime: Long? = null,
    val checkOutTime: Long? = null,
    val checkInImageUri: String? = null,
    val checkOutImageUri: String? = null
) {
    val isCheckedIn: Boolean
        get() = checkInTime != null

    val isCheckedOut: Boolean
        get() = checkOutTime != null

    val formattedCheckInTime: String?
        get() {
            val t = checkInTime ?: return null
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm น.", java.util.Locale("th", "TH"))
            return sdf.format(java.util.Date(t))
        }

    val formattedCheckOutTime: String?
        get() {
            val t = checkOutTime ?: return null
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm น.", java.util.Locale("th", "TH"))
            return sdf.format(java.util.Date(t))
        }
    val formattedCaseNumber: String
        get() {
            val cn = caseNumber ?: ""
            if (cn.isNotBlank()) {
                return if (cn.startsWith("#")) cn else "#$cn"
            }
            val sdf = java.text.SimpleDateFormat("ddMMyy", java.util.Locale.US)
            val dateStr = sdf.format(java.util.Date(timestamp))
            return "#${dateStr}001"
        }

    val rawCaseNumber: String
        get() {
            val cn = caseNumber ?: ""
            if (cn.isNotBlank()) {
                return cn.removePrefix("#")
            }
            val sdf = java.text.SimpleDateFormat("ddMMyy", java.util.Locale.US)
            val dateStr = sdf.format(java.util.Date(timestamp))
            return "${dateStr}001"
        }

    val cleanRawText: String
        get() = com.example.ai.CustomerInfoExtractor.cleanRawText(rawText ?: "")

    val imageUriList: List<String>
        get() {
            val uri = imageUri ?: return emptyList()
            if (uri.isBlank()) return emptyList()
            return uri.split("|").map { it.trim() }.filter { it.isNotBlank() }
        }

    companion object {
        const val STATUS_OPEN = "เปิดเคส"
        const val STATUS_CLOSED = "ปิดเคส"

        const val SYNC_PENDING = "PENDING"
        const val SYNC_SYNCED = "SYNCED"
        const val SYNC_FAILED = "FAILED"

        const val PRIORITY_HIGH = "High"
        const val PRIORITY_MEDIUM = "Medium"
        const val PRIORITY_LOW = "Low"

        val PRIORITIES = listOf(PRIORITY_HIGH, PRIORITY_MEDIUM, PRIORITY_LOW)

        fun getPriorityLabel(priority: String): String {
            return when (priority) {
                PRIORITY_HIGH -> "High (ด่วนมาก)"
                PRIORITY_LOW -> "Low (ปกติ)"
                else -> "Medium (ปานกลาง)"
            }
        }

        val CATEGORIES = listOf(
            "ส่งสินค้า",
            "ซ่อมบำรุง",
            "ติดตั้ง",
            "บริการลูกค้า",
            "งานทั่วไป"
        )
    }

    /**
     * Generates the formatted text summary when a case is closed:
     *
     * **ปิดเคส**
     *
     * [ข้อความดิบเดิมที่ตัดข้อมูลติดต่อแก้ไขออก]
     *
     * **Solutions = [ข้อความ Solutions ที่กรอก]**
     *
     * ช่าง = [ชื่อช่างที่กรอก]
     */
    fun buildClosedSummaryTemplate(): String {
        val textToInclude = cleanRawText
        val sol = solutions ?: ""
        val tech = technician ?: ""
        return buildString {
            appendLine("**ปิดเคส**")
            if (textToInclude.isNotBlank()) {
                appendLine()
                append(textToInclude.trim())
            }
            append("\n\n**Solutions = ")
            append(if (sol.isNotBlank()) sol.trim() else "-")
            append("**\n\nช่าง = ")
            append(if (tech.isNotBlank()) tech.trim() else "-")
        }
    }
}
