package com.example.util

import android.util.Log
import com.example.data.WorkLog
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Client for interacting directly with LINE Messaging API (Push Messages & Diagnostics).
 * Allows sending real-time case notifications to LINE Bot users or groups and running
 * comprehensive connectivity/handshake diagnostics.
 */
object LineMessagingApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private const val PUSH_ENDPOINT = "https://api.line.me/v2/bot/message/push"
    private const val BOT_INFO_ENDPOINT = "https://api.line.me/v2/bot/info"
    private const val QUOTA_ENDPOINT = "https://api.line.me/v2/bot/message/quota"
    private const val CONSUMPTION_ENDPOINT = "https://api.line.me/v2/bot/message/quota/consumption"

    data class LineApiResult(
        val isSuccess: Boolean,
        val statusCode: Int,
        val message: String,
        val botName: String? = null,
        val latencyMs: Long = 0,
        val requestId: String? = null
    )

    enum class StepStatus {
        SUCCESS, FAILED, WARNING, SKIPPED
    }

    data class DiagnosticStep(
        val stepNumber: Int,
        val name: String,
        val endpoint: String,
        val status: StepStatus,
        val latencyMs: Long = 0,
        val httpCode: Int = 0,
        val summary: String,
        val details: String? = null,
        val rawResponse: String? = null
    )

    data class BotProfileData(
        val displayName: String,
        val userId: String,
        val basicId: String,
        val chatMode: String,
        val markAsReadMode: String,
        val pictureUrl: String?
    )

    data class QuotaData(
        val type: String, // "limited" or "none"
        val value: Long = 0,
        val totalUsage: Long = 0
    )

    data class TargetValidationData(
        val targetId: String,
        val isValidFormat: Boolean,
        val targetType: String, // "User ID (ขึ้นต้นด้วย U)", "Group ID (ขึ้นต้นด้วย C)", "Room ID (ขึ้นต้นด้วย R)", "ไม่ถูกต้อง"
        val notes: String
    )

    data class LineDiagnosticReport(
        val isOverallSuccess: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val totalLatencyMs: Long = 0,
        val steps: List<DiagnosticStep> = emptyList(),
        val botProfile: BotProfileData? = null,
        val quota: QuotaData? = null,
        val targetValidation: TargetValidationData? = null,
        val recommendations: List<String> = emptyList(),
        val formattedLog: String = ""
    )

    /**
     * Run full-suite diagnostic test:
     * 1. DNS Resolution & Endpoint Reachability
     * 2. Handshake & Bot Profile Verification (/v2/bot/info)
     * 3. Monthly Message Quota & Consumption Check
     * 4. Target ID Syntax & Type Verification
     * 5. Test Push Message Delivery & Delivery Handshake (/v2/bot/message/push)
     */
    fun runComprehensiveDiagnostics(token: String, targetId: String?): LineDiagnosticReport {
        val startTime = System.currentTimeMillis()
        val steps = mutableListOf<DiagnosticStep>()
        val recommendations = mutableListOf<String>()
        val rawLogBuilder = StringBuilder()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("th", "TH"))

        rawLogBuilder.appendLine("==========================================")
        rawLogBuilder.appendLine("   LINE MESSAGING API DIAGNOSTIC REPORT   ")
        rawLogBuilder.appendLine("==========================================")
        rawLogBuilder.appendLine("Timestamp: ${sdf.format(Date(startTime))}")
        rawLogBuilder.appendLine("Endpoint Host: api.line.me")

        val trimmedToken = token.trim()
        val trimmedTargetId = targetId?.trim() ?: ""

        var botProfile: BotProfileData? = null
        var quotaData: QuotaData? = null
        var targetValidation: TargetValidationData? = null
        var isOverallSuccess = true

        // --- STEP 1: DNS & Network Reachability ---
        val step1Start = System.currentTimeMillis()
        var dnsIp = ""
        var step1Success = false
        try {
            val address = InetAddress.getByName("api.line.me")
            dnsIp = address.hostAddress ?: "Unknown"
            val step1Latency = System.currentTimeMillis() - step1Start
            step1Success = true
            steps.add(
                DiagnosticStep(
                    stepNumber = 1,
                    name = "DNS Resolution & Host Reachability",
                    endpoint = "api.line.me",
                    status = StepStatus.SUCCESS,
                    latencyMs = step1Latency,
                    httpCode = 200,
                    summary = "เชื่อมต่อไปยัง api.line.me สำเร็จ ($dnsIp)",
                    details = "Resolved IP: $dnsIp, Latency: ${step1Latency}ms"
                )
            )
            rawLogBuilder.appendLine("[Step 1] DNS Resolution: SUCCESS ($dnsIp) in ${step1Latency}ms")
        } catch (e: Exception) {
            val step1Latency = System.currentTimeMillis() - step1Start
            isOverallSuccess = false
            steps.add(
                DiagnosticStep(
                    stepNumber = 1,
                    name = "DNS Resolution & Host Reachability",
                    endpoint = "api.line.me",
                    status = StepStatus.FAILED,
                    latencyMs = step1Latency,
                    httpCode = 0,
                    summary = "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตหรือหาเซิร์ฟเวอร์ LINE ได้",
                    details = e.localizedMessage
                )
            )
            recommendations.add("โปรดตรวจสอบการเชื่อมต่ออินเทอร์เน็ต (Wi-Fi หรือ Mobile Data) ของอุปกรณ์")
            rawLogBuilder.appendLine("[Step 1] DNS Resolution: FAILED (${e.localizedMessage})")
        }

        // Check if token is empty
        if (trimmedToken.isBlank()) {
            isOverallSuccess = false
            steps.add(
                DiagnosticStep(
                    stepNumber = 2,
                    name = "Token Authentication & Bot Handshake",
                    endpoint = BOT_INFO_ENDPOINT,
                    status = StepStatus.FAILED,
                    summary = "ยังไม่ได้ระบุ Channel Access Token",
                    details = "กรุณากรอก Long-lived Channel Access Token จาก LINE Developers Console"
                )
            )
            recommendations.add("กรุณาคัดลอก Channel Access Token (Long-lived) จากแท็บ Messaging API ใน LINE Developers Console มาวางในช่องตั้งค่า")
            
            val totalLatency = System.currentTimeMillis() - startTime
            return LineDiagnosticReport(
                isOverallSuccess = false,
                totalLatencyMs = totalLatency,
                steps = steps,
                recommendations = recommendations,
                formattedLog = rawLogBuilder.toString()
            )
        }

        // --- STEP 2: Token Authentication & Bot Profile Handshake ---
        val step2Start = System.currentTimeMillis()
        var step2Success = false
        try {
            val infoReq = Request.Builder()
                .url(BOT_INFO_ENDPOINT)
                .header("Authorization", "Bearer $trimmedToken")
                .header("User-Agent", "WorkLog-Assistant-Android/1.0")
                .get()
                .build()

            val response = client.newCall(infoReq).execute()
            val step2Latency = System.currentTimeMillis() - step2Start
            val code = response.code
            val bodyStr = response.body?.string() ?: ""
            val reqId = response.header("x-line-request-id")
            response.close()

            rawLogBuilder.appendLine("[Step 2] GET /v2/bot/info -> HTTP $code (${step2Latency}ms), Request-ID: $reqId")

            if (code in 200..299) {
                step2Success = true
                val json = JSONObject(bodyStr)
                val displayName = json.optString("displayName", "LINE Bot")
                val botUserId = json.optString("userId", "-")
                val basicId = json.optString("basicId", "-")
                val chatMode = json.optString("chatMode", "bot")
                val markAsRead = json.optString("markAsReadMode", "auto")
                val pictureUrl = json.optString("pictureUrl", null)

                botProfile = BotProfileData(
                    displayName = displayName,
                    userId = botUserId,
                    basicId = basicId,
                    chatMode = chatMode,
                    markAsReadMode = markAsRead,
                    pictureUrl = pictureUrl
                )

                steps.add(
                    DiagnosticStep(
                        stepNumber = 2,
                        name = "Token Authentication & Bot Handshake",
                        endpoint = BOT_INFO_ENDPOINT,
                        status = StepStatus.SUCCESS,
                        latencyMs = step2Latency,
                        httpCode = code,
                        summary = "ยืนยันตัวตนสำเร็จ! พบ LINE Bot: '$displayName' ($basicId)",
                        details = "Bot Name: $displayName, Basic ID: $basicId, Mode: $chatMode",
                        rawResponse = bodyStr
                    )
                )
            } else {
                isOverallSuccess = false
                val errorDesc = when (code) {
                    401 -> "Channel Access Token ไม่ถูกต้องหรือหมดอายุ (HTTP 401 Unauthorized)"
                    403 -> "ไม่มีสิทธิ์เข้าถึง Messaging API สำหรับ Token นี้ (HTTP 403 Forbidden)"
                    else -> "เซิร์ฟเวอร์ LINE ตอบกลับข้อผิดพลาด HTTP $code"
                }
                steps.add(
                    DiagnosticStep(
                        stepNumber = 2,
                        name = "Token Authentication & Bot Handshake",
                        endpoint = BOT_INFO_ENDPOINT,
                        status = StepStatus.FAILED,
                        latencyMs = step2Latency,
                        httpCode = code,
                        summary = errorDesc,
                        details = bodyStr,
                        rawResponse = bodyStr
                    )
                )

                if (code == 401) {
                    recommendations.add("Token ผิดพลาดหรือหมดอายุ: ไปที่ LINE Developers Console > Messaging API > Channel access token แล้วกด 'Reissue' แล้วนำ Token ใหม่มาวาง")
                }
            }
        } catch (e: Exception) {
            val step2Latency = System.currentTimeMillis() - step2Start
            isOverallSuccess = false
            steps.add(
                DiagnosticStep(
                    stepNumber = 2,
                    name = "Token Authentication & Bot Handshake",
                    endpoint = BOT_INFO_ENDPOINT,
                    status = StepStatus.FAILED,
                    latencyMs = step2Latency,
                    httpCode = 0,
                    summary = "การเชื่อมต่อล้มเหลว: ${e.localizedMessage}",
                    details = e.localizedMessage
                )
            )
            recommendations.add("เกิดปัญหาในการเชื่อมต่อไปยัง $BOT_INFO_ENDPOINT: ${e.localizedMessage}")
        }

        // --- STEP 3: Quota & Usage Check (Optional Info) ---
        if (step2Success) {
            val step3Start = System.currentTimeMillis()
            try {
                val quotaReq = Request.Builder()
                    .url(QUOTA_ENDPOINT)
                    .header("Authorization", "Bearer $trimmedToken")
                    .get()
                    .build()
                val quotaRes = client.newCall(quotaReq).execute()
                val quotaBody = quotaRes.body?.string() ?: ""
                val quotaCode = quotaRes.code
                quotaRes.close()

                val consumReq = Request.Builder()
                    .url(CONSUMPTION_ENDPOINT)
                    .header("Authorization", "Bearer $trimmedToken")
                    .get()
                    .build()
                val consumRes = client.newCall(consumReq).execute()
                val consumBody = consumRes.body?.string() ?: ""
                val consumCode = consumRes.code
                consumRes.close()

                val step3Latency = System.currentTimeMillis() - step3Start

                if (quotaCode in 200..299) {
                    val quotaJson = JSONObject(quotaBody)
                    val qType = quotaJson.optString("type", "none")
                    val qValue = quotaJson.optLong("value", 0)

                    val cUsage = if (consumCode in 200..299) {
                        JSONObject(consumBody).optLong("totalUsage", 0)
                    } else 0

                    quotaData = QuotaData(
                        type = qType,
                        value = qValue,
                        totalUsage = cUsage
                    )

                    val quotaSummary = if (qType == "limited") {
                        "โควตาเดือนนี้: ใช้ไป $cUsage / $qValue ข้อความ"
                    } else {
                        "โควตาไม่จำกัด (Unlimited), ใช้ไปแล้ว $cUsage ข้อความ"
                    }

                    steps.add(
                        DiagnosticStep(
                            stepNumber = 3,
                            name = "Monthly Message Quota & Usage Check",
                            endpoint = QUOTA_ENDPOINT,
                            status = StepStatus.SUCCESS,
                            latencyMs = step3Latency,
                            httpCode = quotaCode,
                            summary = quotaSummary,
                            details = "Type: $qType, Limit: $qValue, Consumed: $cUsage"
                        )
                    )
                    rawLogBuilder.appendLine("[Step 3] Quota: $quotaSummary (${step3Latency}ms)")
                } else {
                    steps.add(
                        DiagnosticStep(
                            stepNumber = 3,
                            name = "Monthly Message Quota Check",
                            endpoint = QUOTA_ENDPOINT,
                            status = StepStatus.WARNING,
                            latencyMs = step3Latency,
                            httpCode = quotaCode,
                            summary = "ตรวจสอบโควตาไม่สำเร็จ (HTTP $quotaCode) แต่ยังคงใช้งานข้อความได้",
                            details = quotaBody
                        )
                    )
                }
            } catch (e: Exception) {
                steps.add(
                    DiagnosticStep(
                        stepNumber = 3,
                        name = "Monthly Message Quota Check",
                        endpoint = QUOTA_ENDPOINT,
                        status = StepStatus.WARNING,
                        summary = "ข้ามการตรวจสอบโควตา (${e.localizedMessage})"
                    )
                )
            }
        }

        // --- STEP 4: Target ID Analysis & Push Delivery Test ---
        val step4Start = System.currentTimeMillis()
        if (trimmedTargetId.isBlank()) {
            targetValidation = TargetValidationData(
                targetId = "",
                isValidFormat = false,
                targetType = "ยังไม่ได้ระบุ",
                notes = "ยังไม่ได้ระบุ Target ID สำหรับรับข้อความแจ้งเตือน"
            )
            steps.add(
                DiagnosticStep(
                    stepNumber = 4,
                    name = "Push Notification Handshake Delivery",
                    endpoint = PUSH_ENDPOINT,
                    status = StepStatus.WARNING,
                    summary = "ข้ามการทดสอบ Push Message เนื่องจากยังไม่ได้ระบุ Target ID (User ID / Group ID)",
                    details = "กรุณากรอก User ID (ขึ้นต้นด้วย U) หรือ Group ID (ขึ้นต้นด้วย C) เพื่อทดสอบการส่งข้อความจริง"
                )
            )
            recommendations.add("หากต้องการทดสอบรับข้อความในห้องแชท ให้กรอก User ID (ดูที่แท็บ Basic Settings ใน LINE Developers Console) หรือ Group ID")
        } else {
            // Validate Target ID format
            val targetType = when {
                trimmedTargetId.startsWith("U") && trimmedTargetId.length >= 30 -> "LINE User ID (ส่วนตัว)"
                trimmedTargetId.startsWith("C") && trimmedTargetId.length >= 30 -> "LINE Group ID (กลุ่มแชท)"
                trimmedTargetId.startsWith("R") && trimmedTargetId.length >= 30 -> "LINE Room ID (ห้องแชท)"
                trimmedTargetId.startsWith("@") -> "LINE Basic ID (@ID - ผิดประเภท)"
                else -> "รูปแบบ ID ไม่ตรงตามมาตรฐาน LINE (ควรขึ้นต้นด้วย U หรือ C ความยาว 33 ตัวอักษร)"
            }

            val isValidFormat = (trimmedTargetId.startsWith("U") || trimmedTargetId.startsWith("C") || trimmedTargetId.startsWith("R")) && trimmedTargetId.length >= 30

            targetValidation = TargetValidationData(
                targetId = trimmedTargetId,
                isValidFormat = isValidFormat,
                targetType = targetType,
                notes = if (isValidFormat) "รูปแบบ ID ถูกต้องตามมาตรฐาน LINE" else "คำเตือน: รูปแบบ ID อาจไม่ถูกต้อง"
            )

            if (!isValidFormat && trimmedTargetId.startsWith("@")) {
                recommendations.add("Target ID ที่ขึ้นต้นด้วย @ คือ Basic ID ของบอท ไม่ใช่ User ID ของผู้รับ ให้ไปดูที่แท็บ Basic Settings > 'Your user ID' (ขึ้นต้นด้วย U...)")
            }

            // Attempt test push message
            if (step2Success) {
                try {
                    val testTimeStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss น.", Locale("th", "TH")).format(Date())
                    val testPayloadText = buildString {
                        appendLine("🧪 [ทดสอบการเชื่อมต่อระบบ - LINE Messaging API]")
                        appendLine("━━━━━━━━━━━━━━━━━━━")
                        appendLine("✅ การทำ Handshake และยืนยันตัวตนสำเร็จ 100%")
                        appendLine("🤖 บอท: ${botProfile?.displayName ?: "WorkLog Assistant"}")
                        appendLine("🆔 Basic ID: ${botProfile?.basicId ?: "-"}")
                        appendLine("🎯 Target: $targetType")
                        appendLine("🕒 เวลาทดสอบ: $testTimeStr")
                        appendLine("━━━━━━━━━━━━━━━━━━━")
                        appendLine("📌 สถานะ: ระบบพร้อมส่งการแจ้งเตือนเปิดเคสใหม่และปิดเคสเข้าสู่ห้องแชทนี้แล้ว!")
                    }

                    val payloadJson = JSONObject().apply {
                        put("to", trimmedTargetId)
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", testPayloadText)
                            })
                        })
                    }

                    val body = payloadJson.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    val pushReq = Request.Builder()
                        .url(PUSH_ENDPOINT)
                        .header("Authorization", "Bearer $trimmedToken")
                        .header("Content-Type", "application/json")
                        .post(body)
                        .build()

                    val pushResponse = client.newCall(pushReq).execute()
                    val step4Latency = System.currentTimeMillis() - step4Start
                    val pushCode = pushResponse.code
                    val pushBody = pushResponse.body?.string() ?: ""
                    val pushReqId = pushResponse.header("x-line-request-id")
                    pushResponse.close()

                    rawLogBuilder.appendLine("[Step 4] POST /v2/bot/message/push -> HTTP $pushCode (${step4Latency}ms), Request-ID: $pushReqId")

                    if (pushCode in 200..299) {
                        steps.add(
                            DiagnosticStep(
                                stepNumber = 4,
                                name = "Push Notification Handshake Delivery",
                                endpoint = PUSH_ENDPOINT,
                                status = StepStatus.SUCCESS,
                                latencyMs = step4Latency,
                                httpCode = pushCode,
                                summary = "ส่งข้อความทดสอบสำเร็จ! (HTTP $pushCode) ข้อความถูกส่งเข้าแชทแล้ว 🎉",
                                details = "Request-ID: ${pushReqId ?: "N/A"}, Latency: ${step4Latency}ms",
                                rawResponse = pushBody.ifBlank { "{ \"status\": \"success\" }" }
                            )
                        )
                    } else {
                        isOverallSuccess = false
                        val errorAdvice = when {
                            pushCode == 400 && pushBody.contains("Failed to send messages") -> {
                                recommendations.add("บอทยังไม่ได้เป็นเพื่อนกับคุณ หรือยังไม่ได้ถูกเชิญเข้ากลุ่ม: กรุณาสแกน QR Code เพื่อเพิ่มเพื่อนบอทใน LINE หรือเชิญบอทเข้ากลุ่มก่อน")
                                "ส่งไม่สำเร็จ (HTTP 400): บอทยังไม่ได้เป็นเพื่อนกับ User นี้ หรือยังไม่ได้เข้าร่วมกลุ่มแชท"
                            }
                            pushCode == 400 -> {
                                recommendations.add("ตรวจสอบ Target ID: ต้องเป็น User ID (ขึ้นต้นด้วย U) หรือ Group ID (ขึ้นต้นด้วย C) ที่ถูกต้อง")
                                "ส่งไม่สำเร็จ (HTTP 400): ข้อมูล Target ID ไม่ถูกต้อง ($pushBody)"
                            }
                            pushCode == 403 -> {
                                recommendations.add("โควตาข้อความ Push Message ของบัญชี LINE Official Account นี้อาจหมดแล้ว (จำกัด 500 ข้อความ/เดือนสำหรับ Free Tier)")
                                "ส่งไม่สำเร็จ (HTTP 403): โควตาข้อความหมดหรือไม่มีสิทธิ์ Push Message"
                            }
                            else -> "ส่งไม่สำเร็จ (HTTP $pushCode): $pushBody"
                        }

                        steps.add(
                            DiagnosticStep(
                                stepNumber = 4,
                                name = "Push Notification Handshake Delivery",
                                endpoint = PUSH_ENDPOINT,
                                status = StepStatus.FAILED,
                                latencyMs = step4Latency,
                                httpCode = pushCode,
                                summary = errorAdvice,
                                details = "Request-ID: ${pushReqId ?: "N/A"}\nBody: $pushBody",
                                rawResponse = pushBody
                            )
                        )
                    }
                } catch (e: Exception) {
                    val step4Latency = System.currentTimeMillis() - step4Start
                    isOverallSuccess = false
                    steps.add(
                        DiagnosticStep(
                            stepNumber = 4,
                            name = "Push Notification Handshake Delivery",
                            endpoint = PUSH_ENDPOINT,
                            status = StepStatus.FAILED,
                            latencyMs = step4Latency,
                            httpCode = 0,
                            summary = "เกิดข้อผิดพลาดขณะส่งข้อความทดสอบ: ${e.localizedMessage}",
                            details = e.localizedMessage
                        )
                    )
                    recommendations.add("การเชื่อมต่อ Push Message ล้มเหลว: ${e.localizedMessage}")
                }
            } else {
                steps.add(
                    DiagnosticStep(
                        stepNumber = 4,
                        name = "Push Notification Handshake Delivery",
                        endpoint = PUSH_ENDPOINT,
                        status = StepStatus.SKIPPED,
                        summary = "ข้ามการส่งข้อความทดสอบเนื่องจาก Step 2 ยืนยันตัวตนไม่ผ่าน"
                    )
                )
            }
        }

        val totalDuration = System.currentTimeMillis() - startTime
        rawLogBuilder.appendLine("==========================================")
        rawLogBuilder.appendLine("OVERALL RESULT: ${if (isOverallSuccess) "SUCCESS" else "FAILED"} (Total Time: ${totalDuration}ms)")
        rawLogBuilder.appendLine("==========================================")

        return LineDiagnosticReport(
            isOverallSuccess = isOverallSuccess,
            totalLatencyMs = totalDuration,
            steps = steps,
            botProfile = botProfile,
            quota = quotaData,
            targetValidation = targetValidation,
            recommendations = recommendations,
            formattedLog = rawLogBuilder.toString()
        )
    }

    /**
     * Test LINE Bot Channel Access Token by calling LINE Bot Info endpoint
     * and optionally sending a test message if targetId is provided.
     */
    fun testConnection(token: String, targetId: String? = null): LineApiResult {
        val report = runComprehensiveDiagnostics(token, targetId)
        val botName = report.botProfile?.displayName ?: "LINE Bot"
        return if (report.isOverallSuccess) {
            LineApiResult(
                isSuccess = true,
                statusCode = 200,
                message = "เชื่อมต่อสำเร็จ! พบ LINE Bot: '$botName'" + if (!targetId.isNullOrBlank()) " และส่งข้อความทดสอบเข้าห้องแชทเรียบร้อยแล้ว 🎉" else "",
                botName = botName,
                latencyMs = report.totalLatencyMs
            )
        } else {
            val failedStep = report.steps.firstOrNull { it.status == StepStatus.FAILED }
            val errorMsg = failedStep?.summary ?: "การเชื่อมต่อล้มเหลว กรุณาตรวจสอบการตั้งค่า"
            LineApiResult(
                isSuccess = false,
                statusCode = failedStep?.httpCode ?: 0,
                message = errorMsg,
                botName = botName,
                latencyMs = report.totalLatencyMs
            )
        }
    }

    /**
     * Send a plain text push message to a target User ID, Group ID, or Room ID.
     */
    fun sendTextMessage(token: String, targetId: String, text: String): LineApiResult {
        val trimmedToken = token.trim()
        val trimmedTarget = targetId.trim()

        if (trimmedToken.isBlank() || trimmedTarget.isBlank()) {
            return LineApiResult(false, 0, "Token หรือ Target ID ว่างเปล่า")
        }

        val start = System.currentTimeMillis()
        return try {
            val payload = JSONObject().apply {
                put("to", trimmedTarget)
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", text)
                    })
                }
                put("messages", messagesArray)
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(PUSH_ENDPOINT)
                .header("Authorization", "Bearer $trimmedToken")
                .header("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - start
            val responseBody = response.body?.string() ?: ""
            val statusCode = response.code
            val reqId = response.header("x-line-request-id")
            response.close()

            if (statusCode in 200..299) {
                Log.d("LineMessagingApi", "Push message success: HTTP $statusCode in ${latency}ms (Req: $reqId)")
                LineApiResult(true, statusCode, "ส่งข้อความสำเร็จ", latencyMs = latency, requestId = reqId)
            } else {
                Log.e("LineMessagingApi", "Push message failed: HTTP $statusCode - $responseBody (Req: $reqId)")
                val errorMsg = if (statusCode == 401) {
                    "HTTP 401: Token ไม่ถูกต้องหรือหมดอายุ"
                } else if (statusCode == 400) {
                    "HTTP 400: ข้อผิดพลาดจาก LINE ($responseBody) โปรดตรวจว่าบอทถูกเพิ่มเป็นเพื่อนหรืออยู่ในกลุ่มแล้ว"
                } else if (statusCode == 403) {
                    "HTTP 403: โควตาข้อความ Push รายเดือนหมดหรือไม่มีสิทธิ์"
                } else {
                    "HTTP $statusCode: $responseBody"
                }
                LineApiResult(false, statusCode, errorMsg, latencyMs = latency, requestId = reqId)
            }
        } catch (e: Exception) {
            Log.e("LineMessagingApi", "Error sending text message", e)
            LineApiResult(false, 0, "ส่งข้อความล้มเหลว: ${e.localizedMessage}", latencyMs = System.currentTimeMillis() - start)
        }
    }

    /**
     * Build and send formatted notification for Open Case.
     */
    fun sendNewCaseNotification(token: String, targetId: String, workLog: WorkLog): LineApiResult {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm น.", Locale("th", "TH"))
        val dateStr = sdf.format(Date(workLog.timestamp))
        val rawText = workLog.rawText.trim()

        val textMessage = buildString {
            appendLine("🔔 [แจ้งเตือนเปิดเคสใหม่ - WorkLog]")
            appendLine("━━━━━━━━━━━━━━━━━━━")
            appendLine("📌 เลขที่เคส: ${workLog.formattedCaseNumber}")
            appendLine("📂 หมวดหมู่งาน: ${workLog.category}")
            appendLine("⚡ ระดับความสำคัญ: ${workLog.priority}")
            appendLine("🕒 เวลาที่บันทึก: $dateStr")
            appendLine("━━━━━━━━━━━━━━━━━━━")
            appendLine("📝 รายละเอียดงาน:")
            appendLine(rawText.ifBlank { "(ไม่มีรายละเอียดเพิ่มเติม)" })
            appendLine("━━━━━━━━━━━━━━━━━━━")
            appendLine("สถานะ: 🟡 รอช่างเข้าดำเนินการ")
        }

        return sendTextMessage(token, targetId, textMessage)
    }

    /**
     * Uploads local image URI or file to a public temporary image host (envs.sh or tmpfiles.org)
     * so it can be sent via LINE Messaging API image message.
     */
    private fun uploadImageToPublicHost(context: android.content.Context, uriOrPath: String): String? {
        try {
            if (uriOrPath.startsWith("http://") || uriOrPath.startsWith("https://")) {
                return uriOrPath
            }
            val file = if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
                val uri = android.net.Uri.parse(uriOrPath)
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                val tempFile = java.io.File.createTempFile("line_img_", ".jpg", context.cacheDir)
                tempFile.outputStream().use { output -> inputStream.copyTo(output) }
                inputStream.close()
                tempFile
            } else {
                val f = java.io.File(uriOrPath)
                if (f.exists()) f else return null
            }

            val requestBody = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull()))
                .build()

            val request = Request.Builder()
                .url("https://envs.sh")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val url = response.body?.string()?.trim()
                    if (!url.isNullOrBlank() && url.startsWith("http")) {
                        return url
                    }
                }
            }

            // Fallback to tmpfiles.org
            val formBody = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull()))
                .build()

            val tmpRequest = Request.Builder()
                .url("https://tmpfiles.org/api/v1/upload")
                .post(formBody)
                .build()

            client.newCall(tmpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val data = json.optJSONObject("data")
                    val rawUrl = data?.optString("url", "") ?: ""
                    if (rawUrl.isNotBlank()) {
                        return rawUrl.replace("tmpfiles.org/", "tmpfiles.org/dl/")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LineMessagingApi", "Failed to upload image for LINE bot", e)
        }
        return null
    }

    /**
     * Build and send formatted notification for Closed Case with attached images.
     */
    fun sendClosedCaseNotification(
        token: String,
        targetId: String,
        workLog: WorkLog,
        solutions: String,
        technician: String,
        context: android.content.Context? = null
    ): LineApiResult {
        val updatedWorkLog = workLog.copy(
            solutions = solutions.ifBlank { workLog.solutions },
            technician = technician.ifBlank { workLog.technician }
        )
        val textMessage = updatedWorkLog.buildClosedSummaryTemplate()

        val trimmedToken = token.trim()
        val trimmedTarget = targetId.trim()

        if (trimmedToken.isBlank() || trimmedTarget.isBlank()) {
            return LineApiResult(false, 0, "Token หรือ Target ID ว่างเปล่า")
        }

        val start = System.currentTimeMillis()
        return try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", textMessage)
                })

                if (context != null) {
                    val imagesList = updatedWorkLog.imageUriList
                    imagesList.take(4).forEach { imgUriStr ->
                        val publicUrl = uploadImageToPublicHost(context, imgUriStr)
                        if (!publicUrl.isNullOrBlank()) {
                            put(JSONObject().apply {
                                put("type", "image")
                                put("originalContentUrl", publicUrl)
                                put("previewImageUrl", publicUrl)
                            })
                        }
                    }
                }
            }

            val payload = JSONObject().apply {
                put("to", trimmedTarget)
                put("messages", messagesArray)
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(PUSH_ENDPOINT)
                .header("Authorization", "Bearer $trimmedToken")
                .header("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - start
            val responseBody = response.body?.string() ?: ""
            val statusCode = response.code
            val reqId = response.header("x-line-request-id")
            response.close()

            if (statusCode in 200..299) {
                Log.d("LineMessagingApi", "Push closed case notification success: HTTP $statusCode in ${latency}ms")
                LineApiResult(true, statusCode, "ส่งข้อความและรูปภาพปิดเคสสำเร็จ", latencyMs = latency, requestId = reqId)
            } else {
                Log.e("LineMessagingApi", "Push closed case notification failed: HTTP $statusCode - $responseBody")
                val errorMsg = when (statusCode) {
                    401 -> "HTTP 401: Token ไม่ถูกต้องหรือหมดอายุ"
                    400 -> "HTTP 400: ข้อผิดพลาดจาก LINE ($responseBody) โปรดตรวจว่าบอทถูกเพิ่มเป็นเพื่อนหรืออยู่ในกลุ่มแล้ว"
                    403 -> "HTTP 403: โควตาข้อความ Push รายเดือนหมดหรือไม่มีสิทธิ์"
                    else -> "HTTP $statusCode: $responseBody"
                }
                LineApiResult(false, statusCode, errorMsg, latencyMs = latency, requestId = reqId)
            }
        } catch (e: Exception) {
            Log.e("LineMessagingApi", "Error sending closed case notification", e)
            LineApiResult(false, 0, "ส่งข้อความล้มเหลว: ${e.localizedMessage}", latencyMs = System.currentTimeMillis() - start)
        }
    }
}
