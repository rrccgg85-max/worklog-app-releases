package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.WorkLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Automated Agent powered by Gemini API (gemini-3.5-flash).
 * Scans completed work logs (closed cases) and generates a comprehensive,
 * structured project completion summary report for executive overview and project handover.
 */
object ProjectCompletionAgent {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun generateReport(closedLogs: List<WorkLog>): String = withContext(Dispatchers.IO) {
        if (closedLogs.isEmpty()) {
            return@withContext "⚠️ ยังไม่มีเคสงานที่ปิดแล้วในระบบ กรุณาปิดเคสอย่างน้อย 1 รายการก่อนสร้างรายงานสรุปการปิดโครงการ"
        }

        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (isValidApiKey(apiKey)) {
            val reportFromGemini = callGeminiAgent(closedLogs, apiKey)
            if (!reportFromGemini.isNullOrBlank()) {
                return@withContext reportFromGemini
            }
        }

        // Fallback to local rule-based report generation
        generateLocalReport(closedLogs)
    }

    private fun isValidApiKey(apiKey: String?): Boolean {
        return !apiKey.isNullOrBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                !apiKey.contains("placeholder", ignoreCase = true)
    }

    private fun callGeminiAgent(closedLogs: List<WorkLog>, apiKey: String): String? {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale("th", "TH")).format(Date())

            val formattedLogsContext = closedLogs.mapIndexed { index, log ->
                val logDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("th", "TH")).format(Date(log.timestamp))
                """
                รายการที่ ${index + 1}:
                - เลขเคส: ${log.formattedCaseNumber}
                - วันที่-เวลา: $logDate
                - หมวดหมู่: ${log.category}
                - รายละเอียดงาน/ปัญหา: ${log.rawText}
                - วิธีแก้ไข/Solutions: ${if (log.solutions.isNotBlank()) log.solutions else "ไม่มีระบุ"}
                - ช่างผู้ปฏิบัติงาน: ${if (log.technician.isNotBlank()) log.technician else "ไม่ได้ระบุ"}
                """.trimIndent()
            }.joinToString("\n\n")

            val systemInstructionText = """
                คุณคือ "Gemini Project Completion AI Agent" ผู้เชี่ยวชาญการวิเคราะห์ข้อมูลและจัดทำรายงานสรุปปิดโครงการสำหรับองค์กร
                หน้าที่ของคุณคือสแกนรายการบันทึกงานที่ปิดเคสสมบูรณ์แล้ว (Completed Work Logs) แล้วเรียบเรียงสรุปเป็น "รายงานสรุปปิดโครงการและผลการดำเนินงาน (Project Completion Report)" ภาษาไทยที่เป็นทางการ อ่านง่าย กระชับ และเป็นมืออาชีพ

                โครงสร้างรายงานที่ต้องการ:
                1. 📊 **สรุปภาพรวมการดำเนินงาน (Executive Overview)**
                   - สรุปภาพรวมของงานทั้งหมดที่เสร็จสิ้น ($dateStr)
                   - จำนวนเคสที่ดำเนินการเสร็จสิ้นเรียบร้อย
                
                2. 🛠️ **สรุปผลงานและวิธีแก้ไขหลักแยกตามหมวดหมู่ (Key Deliverables & Solutions)**
                   - จัดกลุ่มงานตามหมวดหมู่ (เช่น ซ่อมบำรุง, ติดตั้ง, ส่งสินค้า, บริการลูกค้า)
                   - สรุปปัญหาที่พบและโซลูชันแก้ไขปัญหาหลักๆ ที่ได้ดำเนินการไปแล้ว

                3. 👨‍🔧 **สรุปการปฏิบัติงานของทีมช่างและเจ้าหน้าที่ (Team Contribution)**
                   - สรุปรายชื่อช่าง/เจ้าหน้าที่ที่มีส่วนร่วมและสถิติงานที่ปิดได้

                4. 💡 **ข้อแนะนำเพิ่มเติมและการส่งมอบงาน (Handover & Recommendations)**
                   - ข้อเสนอแนะสั้นๆ 2-3 ข้อ สำหรับการบำรุงรักษาเชิงป้องกันหรือติดตามผลในอนาคต

                กฎการตอบ:
                - ตอบด้วยข้อความภาษาไทยจัดรูปแบบ Markdown ให้สวยงาม
                - ใช้ Bullet points และ Emoji ประกอบเพื่อความสบายตา
                - ห้ามใส่ข้อมูลที่ไม่เกี่ยวกับข้อมูลที่ส่งไป
            """.trimIndent()

            val prompt = """
                วันที่ออกรายงาน: $dateStr
                จำนวนเคสที่ปิดแล้วทั้งหมด: ${closedLogs.size} เคส

                === รายการบันทึกงานที่ปิดเคสสมบูรณ์ (Completed Work Logs) ===
                $formattedLogsContext
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstructionText))
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                })
            }

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBodyStr = response.body?.string()
            response.close()

            if (!response.isSuccessful || responseBodyStr.isNullOrBlank()) {
                Log.w("ProjectCompletionAgent", "Gemini API request failed status ${response.code}: $responseBodyStr")
                return null
            }

            val rootObj = JSONObject(responseBodyStr)
            val candidates = rootObj.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            val textResult = parts.getJSONObject(0).optString("text", "")
            if (textResult.isBlank()) null else textResult
        } catch (e: Exception) {
            Log.e("ProjectCompletionAgent", "Error generating project completion report with Gemini", e)
            null
        }
    }

    private fun generateLocalReport(closedLogs: List<WorkLog>): String {
        val dateStr = try {
            SimpleDateFormat("dd MMMM yyyy", Locale("th", "TH")).format(Date())
        } catch (e: Exception) {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        }

        val totalClosed = closedLogs.size
        val categoriesCount = closedLogs.groupBy { it.category }
        val techniciansCount = closedLogs.groupBy { if (it.technician.isNotBlank()) it.technician else "ไม่ระบุช่าง" }

        return buildString {
            append("📋 **รายงานสรุปการปิดโครงการ (Project Completion Report)**\n")
            append("📅 วันที่ออกรายงาน: $dateStr\n")
            append("-------------------------------------------\n\n")

            append("📊 **1. ภาพรวมผลการดำเนินงาน (Executive Summary)**\n")
            append("• ดำเนินการปิดเคสเสร็จสมบูรณ์รวมทั้งสิ้น: **$totalClosed รายการ**\n")
            append("• สถานะโครงการ: ✅ **เสร็จสิ้นสมบูรณ์ (100% Completed)**\n\n")

            append("🛠️ **2. สรุปผลงานแยกตามหมวดหมู่ (Deliverables by Category)**\n")
            categoriesCount.forEach { (category, logs) ->
                append("• **หมวด $category** (${logs.size} เคส):\n")
                logs.take(3).forEach { log ->
                    append("   - [${log.formattedCaseNumber}] ${log.rawText}")
                    if (log.solutions.isNotBlank()) {
                        append(" (โซลูชัน: ${log.solutions})")
                    }
                    append("\n")
                }
                if (logs.size > 3) {
                    append("   - ...และรายการอื่นๆ อีก ${logs.size - 3} เคส\n")
                }
            }
            append("\n")

            append("👨‍🔧 **3. สถิติการปฏิบัติงานของทีมช่าง (Technician Overview)**\n")
            techniciansCount.forEach { (tech, logs) ->
                append("• **$tech**: ดำเนินการปิดเคสสำเร็จ ${logs.size} รายการ\n")
            }
            append("\n")

            append("💡 **4. ข้อเสนอแนะและการส่งมอบงาน (Handover Notes)**\n")
            append("• งานทั้งหมดได้รับการแก้ไขและตรวจสอบความเรียบร้อยตามมาตรฐาน\n")
            append("• แนะนำให้ทำความสะอาดอุปกรณ์และบันทึกประวัติเพื่อการบำรุงรักษาในรอบถัดไป\n")
        }
    }
}
