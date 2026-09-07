package com.example.util

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.MainActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Data class representing Version Configuration from Firebase Firestore / Remote JSON
 */
data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
    val releaseDate: String = "",
    val patchSize: String = "",
    val isCritical: Boolean = false
)

/**
 * UpdateManager handles in-app updates outside Google Play Store.
 * Fetches update metadata from Firebase Firestore or Remote URL, downloads APK via DownloadManager,
 * displays system notifications to technicians, and launches FileProvider installation intent.
 */
class UpdateManager(private val context: Context) {

    private val TAG = "UpdateManager"
    private val CHANNEL_ID = "app_updates_channel"
    private val NOTIFICATION_ID = 777

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "อัปเดตเวอร์ชันและแพตช์ระบบ"
            val descriptionText = "แจ้งเตือนเมื่อมีแพตช์เวอร์ชันใหม่สำหรับช่างบริการ"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if a new app update is available.
     * First queries Firebase Firestore (collection: app_updates, doc: latest or app_config/version),
     * and if not found, queries remote JSON URL.
     */
    fun checkForUpdates(
        customUrl: String? = null,
        onUpdateAvailable: (AppVersionInfo) -> Unit,
        onNoUpdate: () -> Unit = {},
        onError: ((String) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val currentVersionCode = BuildConfig.VERSION_CODE
            var foundUpdate: AppVersionInfo? = null

            // 1. Try checking Firebase Firestore
            try {
                val firestore = FirebaseFirestore.getInstance()
                val doc = firestore.collection("app_updates").document("latest").get().await()
                if (doc.exists()) {
                    val remoteVersionCode = doc.getLong("versionCode")?.toInt() ?: 0
                    val remoteVersionName = doc.getString("versionName") ?: ""
                    val apkUrl = doc.getString("apkUrl") ?: ""
                    val changelog = doc.getString("changelog") ?: "ปรับปรุงประสิทธิภาพและแก้ไขบัค"
                    val releaseDate = doc.getString("releaseDate") ?: ""
                    val patchSize = doc.getString("patchSize") ?: ""
                    val isCritical = doc.getBoolean("isCritical") ?: false

                    Log.d(TAG, "Firestore update check: current=$currentVersionCode, remote=$remoteVersionCode")

                    if (remoteVersionCode > currentVersionCode && apkUrl.isNotBlank()) {
                        foundUpdate = AppVersionInfo(
                            versionCode = remoteVersionCode,
                            versionName = remoteVersionName,
                            apkUrl = apkUrl,
                            changelog = changelog,
                            releaseDate = releaseDate,
                            patchSize = patchSize,
                            isCritical = isCritical
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore update check failed or not configured: ${e.message}")
            }

            // 2. If no Firestore update found and customUrl or default URL is available, try fetching remote JSON
            if (foundUpdate == null) {
                val urlToFetch = if (!customUrl.isNullOrBlank()) {
                    customUrl
                } else {
                    "https://firebasestorage.googleapis.com/v0/b/your-app.appspot.com/o/version.json?alt=media"
                }

                try {
                    val jsonString = fetchVersionJson(urlToFetch)
                    if (jsonString.isNotBlank()) {
                        val jsonObject = JSONObject(jsonString)
                        val remoteVersionCode = jsonObject.optInt("versionCode", 0)
                        val remoteVersionName = jsonObject.optString("versionName", "")
                        val apkUrl = jsonObject.optString("apkUrl", "")
                        val changelog = jsonObject.optString("changelog", "ปรับปรุงประสิทธิภาพและแก้ไขบัค")
                        val releaseDate = jsonObject.optString("releaseDate", "")
                        val patchSize = jsonObject.optString("patchSize", "")
                        val isCritical = jsonObject.optBoolean("isCritical", false)

                        Log.d(TAG, "JSON update check: current=$currentVersionCode, remote=$remoteVersionCode")

                        if (remoteVersionCode > currentVersionCode && apkUrl.isNotBlank()) {
                            foundUpdate = AppVersionInfo(
                                versionCode = remoteVersionCode,
                                versionName = remoteVersionName,
                                apkUrl = apkUrl,
                                changelog = changelog,
                                releaseDate = releaseDate,
                                patchSize = patchSize,
                                isCritical = isCritical
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Remote JSON update check failed: ${e.message}")
                }
            }

            withContext(Dispatchers.Main) {
                if (foundUpdate != null) {
                    sendUpdateNotification(foundUpdate)
                    onUpdateAvailable(foundUpdate)
                } else {
                    onNoUpdate()
                }
            }
        }
    }

    /**
     * Start real-time Firestore listener for version updates.
     * When an admin publishes an update document in Firestore, technician apps are notified in real-time.
     */
    fun startRealtimeUpdateListener(
        onUpdateAvailable: (AppVersionInfo) -> Unit
    ): ListenerRegistration? {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("app_updates").document("latest")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Realtime update listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val remoteVersionCode = snapshot.getLong("versionCode")?.toInt() ?: 0
                        val remoteVersionName = snapshot.getString("versionName") ?: ""
                        val apkUrl = snapshot.getString("apkUrl") ?: ""
                        val changelog = snapshot.getString("changelog") ?: "ปรับปรุงประสิทธิภาพและแก้ไขข้อผิดพลาด"
                        val releaseDate = snapshot.getString("releaseDate") ?: ""
                        val patchSize = snapshot.getString("patchSize") ?: ""
                        val isCritical = snapshot.getBoolean("isCritical") ?: false

                        val currentVersionCode = BuildConfig.VERSION_CODE
                        if (remoteVersionCode > currentVersionCode && apkUrl.isNotBlank()) {
                            val info = AppVersionInfo(
                                versionCode = remoteVersionCode,
                                versionName = remoteVersionName,
                                apkUrl = apkUrl,
                                changelog = changelog,
                                releaseDate = releaseDate,
                                patchSize = patchSize,
                                isCritical = isCritical
                            )
                            sendUpdateNotification(info)
                            onUpdateAvailable(info)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start real-time update listener: ${e.message}")
            null
        }
    }

    /**
     * Sends an Android status bar notification alerting technician of the new version patch.
     */
    fun sendUpdateNotification(info: AppVersionInfo) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_TARGET_TAB", 3)
                putExtra("EXTRA_SUB_PAGE", "PATCH_UPDATE")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("🚀 มีการอัปเดตเวอร์ชันใหม่: ${info.versionName}")
                .setContentText("แตะที่นี่เพื่อเปิดเมนูดาวน์โหลด Patch ใหม่")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "พบการอัปเดตแอปพลิเคชันเวอร์ชันใหม่: ${info.versionName}\n" +
                        "รายการปรับปรุง:\n${info.changelog}\n\n" +
                        "แตะเพื่อเปิดแอปและดาวน์โหลด Patch ใหม่ทันที!"
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post system notification: ${e.message}")
        }
    }

    private fun fetchVersionJson(urlStr: String): String {
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.w(TAG, "Http connection failed: ${e.message}")
            ""
        }
    }

    /**
     * Downloads APK file using Android DownloadManager and installs automatically when completed.
     */
    fun downloadAndInstallApk(apkUrl: String, fileName: String = "app_patch_update.apk", onDownloadStarted: (() -> Unit)? = null) {
        // Check REQUEST_INSTALL_PACKAGES permission on Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "กรุณาเปิดสิทธิ์ 'อนุญาตให้ติดตั้งแอปจากแหล่งที่ไม่รู้จัก' ก่อนติดตั้ง", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        try {
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
                setTitle("กำลังดาวน์โหลด Patch อัปเดตแอป...")
                setDescription("ดาวน์โหลดไฟล์ติดตั้งเวอร์ชันใหม่")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationUri(Uri.fromFile(destinationFile))
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Toast.makeText(context, "กำลังดาวน์โหลดไฟล์ Patch ใหม่...", Toast.LENGTH_SHORT).show()
            onDownloadStarted?.invoke()

            // Register Receiver for download completion
            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.w(TAG, "Receiver already unregistered")
                        }
                        installApk(destinationFile)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Download APK failed: ${e.message}", e)
            Toast.makeText(context, "ดาวน์โหลดล้มเหลว: ${e.message}", Toast.LENGTH_LONG).show()
            
            // Fallback: Open in browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Browser fallback failed: ${ex.message}")
            }
        }
    }

    /**
     * Prompts Android OS to install APK via FileProvider URI Intent
     */
    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(context, "ไม่พบไฟล์ APK ที่ดาวน์โหลด", Toast.LENGTH_SHORT).show()
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch APK installer: ${e.message}", e)
            Toast.makeText(context, "ข้อผิดพลาดในการติดตั้ง: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

