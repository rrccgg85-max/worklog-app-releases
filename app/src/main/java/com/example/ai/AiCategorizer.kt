package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Intelligent AI Service for Work Log Categorization.
 * Integrates with Gemini API (gemini-3.5-flash) using a structured prompt to analyze
 * Thai free-text work descriptions and automatically assign a category.
 * Provides instant fallback to keyword-based classification if Gemini API is offline or unconfigured.
 */
object AiCategorizer {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun performOcr(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (!isValidApiKey(apiKey)) {
            Log.w("AiCategorizer", "Gemini API key is not valid or configured.")
            return@withContext null
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            // Compress and encode bitmap to base64
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val systemInstructionText = """
                คุณคือระบบ AI ผู้ช่วยดึงข้อมูลข้อความภาษาอังกฤษ ตัวเลข ลิงก์ (URLs) อีเมล และโค้ด/Serial Number จากรูปภาพอย่างแม่นยำที่สุด
                กฎเหล็ก:
                1. ละเว้นภาษาไทยทั้งหมด ไม่ต้องดึงข้อความภาษาไทยออกมา
                2. เน้นดึงเฉพาะข้อความภาษาอังกฤษ, ลิงก์เว็บ (เช่น https://..., www....), อีเมล, และตัวเลข/รหัสต่างๆ อย่างถูกต้องครบถ้วน
                3. ไม่ต้องแสดงคำอธิบายหรือบทสนทนาเพิ่มเติมใดๆ ทั้งสิ้น แสดงเฉพาะรายการข้อความที่ดึงมาได้เท่านั้น
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
                            put(JSONObject().put("text", "กรุณาดึงข้อความภาษาอังกฤษ ตัวเลข ลิงก์ และ Serial Numbers จากรูปภาพนี้โดยละเว้นภาษาไทย"))
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
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
                Log.w("AiCategorizer", "Gemini API OCR request failed with status ${response.code}: $responseBodyStr")
                return@withContext null
            }

            val rootObj = JSONObject(responseBodyStr)
            val candidates = rootObj.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            if (parts.length() == 0) return@withContext null

            val generatedText = parts.getJSONObject(0).optString("text", "")
            if (generatedText.isNotBlank()) {
                return@withContext generatedText.trim()
            }
        } catch (e: Exception) {
            Log.e("AiCategorizer", "Error performing Gemini OCR", e)
        }
        return@withContext null
    }

    suspend fun categorizeText(rawText: String): AiClassificationResult = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) {
            return@withContext AiClassificationResult(
                category = "งานทั่วไป",
                confidence = "การวิเคราะห์เริ่มต้น (Default)",
                explanation = "ข้อความว่างเปล่า"
            )
        }

        // Check if Gemini API key is configured and valid
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }
        if (isValidApiKey(apiKey)) {
            val geminiResult = callGeminiApi(rawText, apiKey)
            if (geminiResult != null) {
                return@withContext geminiResult
            }
        }

        // Fallback to local rule-based classification if Gemini is unavailable
        categorizeTextRuleBased(rawText)
    }

    private fun isValidApiKey(apiKey: String?): Boolean {
        return !apiKey.isNullOrBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                !apiKey.contains("placeholder", ignoreCase = true)
    }

    private fun callGeminiApi(rawText: String, apiKey: String): AiClassificationResult? {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val systemInstructionText = """
                คุณคือระบบ AI ผู้ช่วยวิเคราะห์และจัดหมวดหมู่งานบันทึกปฏิบัติงาน (Work Log) ภาษาไทยสำหรับช่างและเจ้าหน้าที่ปฏิบัติงาน
                
                กรุณาวิเคราะห์ข้อความบันทึกงานภาษาไทย แล้วจัดหมวดหมู่งานให้อยู่ใน 1 จาก 5 หมวดหมู่นี้เท่านั้น:
                1. "ส่งสินค้า" - การจัดส่ง พัสดุ สินค้า ขนส่ง รถส่งของ คลังสินค้า Delivery Ship
                2. "ซ่อมบำรุง" - การซ่อมแซม อุปกรณ์เสีย ชำรุด บำรุงรักษา แอร์ ไฟฟ้า เครื่องจักร อะไหล่ Repair Maintenance
                3. "ติดตั้ง" - การติดตั้งอุปกรณ์ วางระบบ เดินสาย cctv กล้อง ตั้งค่า อินเทอร์เน็ต Installation Setup
                4. "บริการลูกค้า" - การสอบถาม ตอบคำถาม บริการ ให้ข้อมูล แนะนำ ติดต่อลูกค้า Support Service
                5. "งานทั่วไป" - งานเอกสาร การประชุม ตรวจงาน หรือเรื่องทั่วไปอื่นๆ
                
                พร้อมทั้งสกัดข้อมูลลูกค้าถ้าพบในข้อความ:
                - customerName: ชื่อลูกค้า หรือ ผู้ติดต่อ (ถ้าไม่มีให้เป็น null)
                - phone: เบอร์โทรศัพท์ (ถ้าไม่มีให้เป็น null)
                - buildingOrAddress: สถานที่ อาคาร ห้อง หรือที่อยู่ (ถ้าไม่มีให้เป็น null)
                - confidence: ระดับความมั่นใจ เช่น "สูงมาก (Gemini AI)", "สูง (Gemini AI)", "ปานกลาง (Gemini AI)"
                - explanation: เหตุผลสั้นๆ 1 ประโยคภาษาไทย อธิบายเหตุผลที่จัดเข้าหมวดหมู่นี้
                
                ตอบกลับเป็น JSON เท่านั้นในรูปแบบ:
                {
                  "category": "หมวดหมู่ที่เลือก",
                  "confidence": "ระดับความเชื่อมั่น",
                  "explanation": "เหตุผลสั้นๆ",
                  "customerName": "ชื่อลูกค้าหรือ null",
                  "phone": "เบอร์โทรศัพท์หรือ null",
                  "buildingOrAddress": "สถานที่หรือ null"
                }
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
                            put(JSONObject().put("text", "ข้อความบันทึกงาน: $rawText"))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
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
                Log.w("AiCategorizer", "Gemini API request failed with status ${response.code}: $responseBodyStr")
                return null
            }

            val rootObj = JSONObject(responseBodyStr)
            val candidates = rootObj.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            val generatedText = parts.getJSONObject(0).optString("text", "")
            if (generatedText.isBlank()) return null

            val jsonOutput = JSONObject(generatedText)
            val rawCategory = jsonOutput.optString("category", "งานทั่วไป")
            val confidence = jsonOutput.optString("confidence", "สูงมาก (Gemini AI)")
            val explanation = jsonOutput.optString("explanation", "วิเคราะห์โดย Gemini AI")
            val customerName = jsonOutput.optString("customerName").takeIf { it != "null" && it.isNotBlank() }
            val phone = jsonOutput.optString("phone").takeIf { it != "null" && it.isNotBlank() }
            val buildingOrAddress = jsonOutput.optString("buildingOrAddress").takeIf { it != "null" && it.isNotBlank() }

            val regexExtracted = CustomerInfoExtractor.extract(rawText)

            return AiClassificationResult(
                category = normalizeCategory(rawCategory),
                confidence = confidence,
                explanation = explanation,
                customerName = customerName ?: regexExtracted.customerName,
                phone = phone ?: regexExtracted.phone,
                buildingOrAddress = buildingOrAddress ?: regexExtracted.buildingOrAddress
            )
        } catch (e: Exception) {
            Log.e("AiCategorizer", "Error calling Gemini API for categorization", e)
            return null
        }
    }

    private fun normalizeCategory(category: String): String {
        return when {
            category.contains("ส่ง") || category.contains("ขนส่ง") || category.contains("delivery", ignoreCase = true) -> "ส่งสินค้า"
            category.contains("ซ่อม") || category.contains("บำรุง") || category.contains("repair", ignoreCase = true) -> "ซ่อมบำรุง"
            category.contains("ติดตั้ง") || category.contains("install", ignoreCase = true) -> "ติดตั้ง"
            category.contains("บริการ") || category.contains("ลูกค้า") || category.contains("support", ignoreCase = true) -> "บริการลูกค้า"
            else -> "งานทั่วไป"
        }
    }

    fun categorizeTextRuleBased(rawText: String): AiClassificationResult {
        val cleanText = rawText.lowercase()

        val deliveryKeywords = listOf(
            "ส่ง", "สินค้า", "ขนส่ง", "พัสดุ", "เดลิเวอรี่", "จัดส่ง", "ออเดอร์",
            "ส่งมอบ", "นำส่ง", "ไปรษณีย์", "กระจายสินค้า", "คลัง", "รถส่ง", "ship", "delivery"
        )

        val maintenanceKeywords = listOf(
            "ซ่อม", "เสีย", "พัง", "แก้ไข", "อาการ", "แอร์", "ไฟฟ้า", "เครื่อง",
            "ตรวจเช็ค", "เปลี่ยน", "ไฟดัก", "น้ำรั่ว", "ชำรุด", "ติดขัด", "บำรุง",
            "มอเตอร์", "ปั๊ม", "เบรกเกอร์", "บอร์ด", "repair", "fix", "maintenance"
        )

        val installationKeywords = listOf(
            "ติดตั้ง", "วางระบบ", "เดินสาย", "ตั้งค่า", "ประกอบ", "เซ็ตระบบ",
            "อินเทอร์เน็ต", "กล้อง", "cctv", "กล่อง", "ต่อสาย", "แผง", "install", "setup"
        )

        val customerServiceKeywords = listOf(
            "ลูกค้า", "ตอบ", "สอบถาม", "ให้บริการ", "ให้ข้อมูล", "สัญญาณ", "ดูแล",
            "รับเรื่อง", "ร้องเรียน", "คำขอ", "แนะนำ", "support", "service", "help"
        )

        var bestCategory = "งานทั่วไป"
        var maxScore = 0

        fun calculateScore(keywords: List<String>): Int {
            return keywords.sumOf { keyword ->
                if (cleanText.contains(keyword)) 1 else 0
            }
        }

        val deliveryScore = calculateScore(deliveryKeywords)
        val maintenanceScore = calculateScore(maintenanceKeywords)
        val installationScore = calculateScore(installationKeywords)
        val customerScore = calculateScore(customerServiceKeywords)

        val scores = mapOf(
            "ส่งสินค้า" to deliveryScore,
            "ซ่อมบำรุง" to maintenanceScore,
            "ติดตั้ง" to installationScore,
            "บริการลูกค้า" to customerScore
        )

        val highestEntry = scores.maxByOrNull { it.value }
        if (highestEntry != null && highestEntry.value > 0) {
            bestCategory = highestEntry.key
            maxScore = highestEntry.value
        }

        val confidence = when {
            maxScore >= 3 -> "สูงมาก (High Confidence)"
            maxScore == 2 -> "สูง (Medium-High)"
            maxScore == 1 -> "ปานกลาง (Medium)"
            else -> "การวิเคราะห์เริ่มต้น (Default)"
        }

        val explanation = when (bestCategory) {
            "ส่งสินค้า" -> "ตรวจพบคำที่เกี่ยวกับการขนส่งหรือส่งมอบสินค้า"
            "ซ่อมบำรุง" -> "ตรวจพบคำที่เกี่ยวกับอาการเสีย การเปลี่ยนอะไหล่ หรือซ่อมแซม"
            "ติดตั้ง" -> "ตรวจพบคำเกี่ยวกับการตั้งค่า หรือติดตั้งอุปกรณ์/ระบบ"
            "บริการลูกค้า" -> "ตรวจพบคำเกี่ยวกับการสอบถาม ติดต่อ หรือบริการลูกค้า"
            else -> "จัดเข้าหมวดหมู่งานทั่วไปสำหรับการบันทึกมาตรฐาน"
        }

        val extractedInfo = CustomerInfoExtractor.extract(rawText)

        return AiClassificationResult(
            category = bestCategory,
            confidence = confidence,
            explanation = explanation,
            customerName = extractedInfo.customerName,
            phone = extractedInfo.phone,
            buildingOrAddress = extractedInfo.buildingOrAddress
        )
    }
}

data class AiClassificationResult(
    val category: String,
    val confidence: String,
    val explanation: String,
    val customerName: String? = null,
    val phone: String? = null,
    val buildingOrAddress: String? = null
)
