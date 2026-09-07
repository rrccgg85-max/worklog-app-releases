package com.example.util

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Custom OkHttp client helper designed specifically for Google Apps Script Webhooks.
 *
 * Google Apps Script Web Apps respond with a 302 Found redirect from script.google.com
 * to script.googleusercontent.com. Standard HTTP clients automatically convert 302 redirects
 * from POST to GET (per RFC), causing Google Apps Script to return HTTP 401 Unauthorized or 405.
 *
 * This client disables auto-redirects and handles redirects manually, re-issuing POST requests
 * with the body intact to the redirected Location header.
 */
object GoogleScriptNetworkClient {

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false) // Handle 302 redirects manually to ensure redirected request uses GET for Google Apps Script echo server
        .followSslRedirects(false)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .removeHeader("Authorization")
                .removeHeader("Bearer")
                .header("User-Agent", "Mozilla/5.0 (Android; WorkLogApp)")
                .build()
            chain.proceed(request)
        }
        .build()

    fun executePost(webhookUrl: String, jsonPayload: String): ResponseResult {
        return try {
            var currentUrl = webhookUrl.trim()
            var redirectCount = 0
            val maxRedirects = 5

            // Send payload as text/plain to avoid CORS preflight issues on Google Apps Script
            val body = jsonPayload.toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

            var request = Request.Builder()
                .url(currentUrl)
                .post(body)
                .build()

            var response: Response = baseClient.newCall(request).execute()

            // Follow 301/302/303 redirects from script.google.com -> script.googleusercontent.com
            while ((response.code in 300..399) && redirectCount < maxRedirects) {
                val location = response.header("Location") ?: break
                val statusCode = response.code
                response.close()

                currentUrl = location
                redirectCount++
                Log.d("GoogleScriptClient", "Redirecting POST ($statusCode -> step $redirectCount) via GET to: $currentUrl")

                // CRITICAL FIX: Google Apps Script echo endpoint (script.googleusercontent.com) ONLY accepts GET requests!
                // Sending POST to script.googleusercontent.com causes HTTP 405 Method Not Allowed.
                request = Request.Builder()
                    .url(currentUrl)
                    .get()
                    .build()

                response = baseClient.newCall(request).execute()
            }

            val statusCode = response.code
            val responseBody = response.body?.string() ?: ""
            response.close()

            val isHtmlResponse = responseBody.contains("<!DOCTYPE html", ignoreCase = true) || 
                                responseBody.contains("<html", ignoreCase = true)

            if (statusCode in 200..299) {
                if (isHtmlResponse && responseBody.contains("ServiceLogin", ignoreCase = true)) {
                    ResponseResult.Error(
                        statusCode = 401,
                        message = "HTTP 401 (Unauthorized): Google Apps Script บังคับให้เข้าสู่ระบบ กรุณาตรวจสอบการตั้งค่าเว็บแอป ให้สิทธิ์ 'ผู้มีสิทธิ์เข้าถึง' (Who has access) เป็น 'ทุกคน' (Anyone)",
                        responseBody = responseBody.take(300)
                    )
                } else {
                    ResponseResult.Success(statusCode, responseBody)
                }
            } else if (statusCode == 405) {
                ResponseResult.Error(
                    statusCode = 405,
                    message = "HTTP 405 (Method Not Allowed): ใน Google Apps Script ไม่พบฟังก์ชัน doPost(e) กรุณาตรวจสอบในไฟล์สคริปต์ว่ามีฟังก์ชัน doPost(e) และกด 'การนำไปใช้งานใหม่' (New deployment) แล้ว",
                    responseBody = responseBody.take(300)
                )
            } else if (statusCode == 401) {
                ResponseResult.Error(
                    statusCode = 401,
                    message = "HTTP 401 (Unauthorized): กรุณาตรวจสอบใน Google Apps Script ว่าได้เลือก 'ผู้มีสิทธิ์เข้าถึง' (Who has access) เป็น 'ทุกคน' (Anyone) และกด 'การนำไปใช้งานใหม่' (New deployment)",
                    responseBody = responseBody.take(300)
                )
            } else {
                ResponseResult.Error(
                    statusCode = statusCode,
                    message = "เซิร์ฟเวอร์ตอบกลับด้วยรหัสข้อผิดพลาด: HTTP $statusCode",
                    responseBody = responseBody.take(300)
                )
            }
        } catch (e: Exception) {
            Log.e("GoogleScriptClient", "Request failed", e)
            ResponseResult.Error(
                statusCode = -1,
                message = "ไม่สามารถเชื่อมต่อได้: ${e.localizedMessage ?: e.message}",
                responseBody = e.stackTraceToString().take(300)
            )
        }
    }

    fun executeGet(webhookUrl: String): ResponseResult {
        return try {
            var currentUrl = webhookUrl.trim()
            var redirectCount = 0
            val maxRedirects = 5

            var request = Request.Builder()
                .url(currentUrl)
                .get()
                .build()

            var response: Response = baseClient.newCall(request).execute()

            while ((response.code in 300..399) && redirectCount < maxRedirects) {
                val location = response.header("Location") ?: break
                response.close()

                currentUrl = location
                redirectCount++

                request = Request.Builder()
                    .url(currentUrl)
                    .get()
                    .build()

                response = baseClient.newCall(request).execute()
            }

            val statusCode = response.code
            val responseBody = response.body?.string() ?: ""
            response.close()

            if (statusCode in 200..299 || statusCode == 405) {
                ResponseResult.Success(statusCode, responseBody)
            } else {
                ResponseResult.Error(
                    statusCode = statusCode,
                    message = "เซิร์ฟเวอร์ตอบกลับด้วยรหัสข้อผิดพลาด: HTTP $statusCode",
                    responseBody = responseBody.take(300)
                )
            }
        } catch (e: Exception) {
            ResponseResult.Error(
                statusCode = -1,
                message = "ไม่สามารถเชื่อมต่อได้: ${e.localizedMessage ?: e.message}",
                responseBody = e.stackTraceToString().take(300)
            )
        }
    }

    sealed class ResponseResult {
        data class Success(val statusCode: Int, val body: String?) : ResponseResult()
        data class Error(val statusCode: Int, val message: String, val responseBody: String? = null) : ResponseResult()
    }
}
