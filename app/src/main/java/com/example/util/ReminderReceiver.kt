package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("worklog_prefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("daily_reminder_enabled", true)
            if (enabled) {
                val hour = prefs.getInt("daily_reminder_hour", 17)
                val minute = prefs.getInt("daily_reminder_minute", 0)
                ReminderScheduler.scheduleDailyLogReminder(context, hour, minute)
            }
            ReminderScheduler.scheduleBackupReminder(context)
            return
        }

        val prefs = context.getSharedPreferences("worklog_prefs", Context.MODE_PRIVATE)
        val soundEnabled = prefs.getBoolean("notification_sound_enabled", true)
        val soundType = prefs.getString("notification_sound_type", "default") ?: "default"
        val vibeEnabled = prefs.getBoolean("notification_vibration_enabled", true)
        val vibePatternType = prefs.getString("notification_vibration_pattern", "default") ?: "default"

        val soundUri: Uri? = if (soundEnabled) {
            when (soundType) {
                "urgent" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        } else null

        val vibePatternArray: LongArray = if (vibeEnabled) {
            when (vibePatternType) {
                "long" -> longArrayOf(0, 1000, 500, 1000)
                "double" -> longArrayOf(0, 200, 100, 200, 100, 200)
                else -> longArrayOf(0, 300, 200, 300)
            }
        } else longArrayOf(0)

        // Trigger physical device vibration when testing or receiving notification
        if (vibeEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createWaveform(vibePatternArray, -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createWaveform(vibePatternArray, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(vibePatternArray, -1)
                    }
                }
            } catch (_: Exception) {}
        }

        val isBackupReminder = intent.getStringExtra("type") == "backup_reminder"
        val isDailyWorkLogReminder = intent.getStringExtra("type") == "daily_work_log_reminder"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (isDailyWorkLogReminder) {
            val channelId = "daily_log_reminders_${soundType}_${vibePatternType}_${if (soundEnabled) "s" else "nos"}_${if (vibeEnabled) "v" else "nov"}"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "แจ้งเตือนบันทึกงานประจำวัน",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "แจ้งเตือนให้ลงบันทึกผลการปฏิบัติงานประจำวันก่อนจบวัน"
                    if (soundUri != null) {
                        val audioAttrs = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                        setSound(soundUri, audioAttrs)
                    } else {
                        setSound(null, null)
                    }
                    enableVibration(vibeEnabled)
                    if (vibeEnabled) {
                        vibrationPattern = vibePatternArray
                    }
                }
                notificationManager.createNotificationChannel(channel)
            }

            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to_create", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                203,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("⏰ ได้เวลาบันทึกการทำงานประจำวัน")
                .setContentText("อย่าลืมสรุปภารกิจและลงบันทึกงานของวันนี้ลงในระบบ")
                .setStyle(NotificationCompat.BigTextStyle().bigText("อย่าลืมสรุปภารกิจและลงบันทึกงานของวันนี้ลงในระบบ\nกดที่นี่เพื่อเปิดหน้าลงบันทึกงานใหม่ได้ทันที!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (soundUri != null) {
                builder.setSound(soundUri)
            }
            if (vibeEnabled) {
                builder.setVibrate(vibePatternArray)
            }

            notificationManager.notify(203, builder.build())

            // Auto-reschedule for the next day if enabled
            val hour = intent.getIntExtra("hour", 17)
            val minute = intent.getIntExtra("minute", 0)
            ReminderScheduler.scheduleDailyLogReminder(context, hour, minute)
            return
        }

        if (isBackupReminder) {
            val channelId = "backup_reminders_${soundType}_${vibePatternType}_${if (soundEnabled) "s" else "nos"}_${if (vibeEnabled) "v" else "nov"}"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "แจ้งเตือนสำรองข้อมูล",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "แจ้งเตือนให้สำรองข้อมูลรายงานประจำสัปดาห์ / เดือน"
                    if (soundUri != null) {
                        val audioAttrs = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                        setSound(soundUri, audioAttrs)
                    } else {
                        setSound(null, null)
                    }
                    enableVibration(vibeEnabled)
                    if (vibeEnabled) {
                        vibrationPattern = vibePatternArray
                    }
                }
                notificationManager.createNotificationChannel(channel)
            }

            val clickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to_settings", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                202,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("💾 เตือนสำรองข้อมูลรายงานประจำเดือน")
                .setContentText("ครบกำหนดสำรองข้อมูลแล้ว กรุณาเข้าสู่เมนูตั้งค่าเพื่อ Backup Report")
                .setStyle(NotificationCompat.BigTextStyle().bigText("ครบกำหนดรอบ 30 วัน / วันสุดท้ายของเดือนแล้ว\nกรุณาเข้าสู่เมนูตั้งค่าและทำรายการส่งออกไฟล์ Excel หรือ PDF เพื่อสำรองข้อมูลให้ปลอดภัย"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            if (soundUri != null) {
                builder.setSound(soundUri)
            }
            if (vibeEnabled) {
                builder.setVibrate(vibePatternArray)
            }

            notificationManager.notify(202, builder.build())
            return
        }

        val customerName = intent.getStringExtra("customerName") ?: "ลูกค้าทั่วไป"
        val onsiteTime = intent.getStringExtra("onsiteTime") ?: ""
        val category = intent.getStringExtra("category") ?: ""
        val logIdStr = intent.getStringExtra("logId") ?: ""
        val requestCode = if (logIdStr.isNotEmpty()) logIdStr.hashCode() else 201

        val channelId = "case_reminders_${soundType}_${vibePatternType}_${if (soundEnabled) "s" else "nos"}_${if (vibeEnabled) "v" else "nov"}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "แจ้งเตือนนัดหมาย/เคส",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "แจ้งเตือนเมื่อใกล้ถึงเวลานัดหมายเข้า Onsite หรือเปิดเคส"
                if (soundUri != null) {
                    val audioAttrs = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    setSound(soundUri, audioAttrs)
                } else {
                    setSound(null, null)
                }
                enableVibration(vibeEnabled)
                if (vibeEnabled) {
                    vibrationPattern = vibePatternArray
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Click action to open MainActivity
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val categoryStr = if (category.isNotEmpty()) " [$category]" else ""
        val titleText = "⏰ ใกล้ถึงเวลานัดหมาย$categoryStr"
        val contentText = if (onsiteTime.isNotEmpty()) "คุณมีนัดกับคุณ $customerName เวลา $onsiteTime" else "ทดสอบระบบแจ้งเตือนนัดหมายเรียบร้อย!"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\n\nกรุณาเตรียมความพร้อมและเดินทางอย่างปลอดภัย"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (soundUri != null) {
            builder.setSound(soundUri)
        }
        if (vibeEnabled) {
            builder.setVibrate(vibePatternArray)
        }

        notificationManager.notify(requestCode, builder.build())
    }
}
