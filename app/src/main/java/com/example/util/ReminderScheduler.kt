package com.example.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.WorkLog
import com.example.ai.CustomerInfoExtractor

object ReminderScheduler {
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleReminder(context: Context, log: WorkLog) {
        val prefs = context.getSharedPreferences("worklog_prefs", Context.MODE_PRIVATE)
        val caseReminderEnabled = prefs.getBoolean("case_reminder_enabled", true)
        if (!caseReminderEnabled) return

        val info = CustomerInfoExtractor.extract(log.rawText)
        val onsiteTime = info.onsiteTime ?: return

        // Calculate begin time based on log's saved timestamp and onsite time
        val beginTime = CustomerInfoExtractor.parseBeginTimeMillis(onsiteTime, log.timestamp)
        
        // Lead time in minutes before appointment (0, 15, 30, 60, 120)
        val leadTimeMinutes = prefs.getInt("reminder_lead_time_minutes", 30)
        val triggerTime = beginTime - (leadTimeMinutes * 60 * 1000L)

        // Only schedule if the trigger time is in the future
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("customerName", info.customerName ?: "ลูกค้าทั่วไป")
            putExtra("onsiteTime", onsiteTime)
            putExtra("category", log.category)
            putExtra("logId", log.id)
        }

        val requestCode = log.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule alarm", e)
        }
    }

    fun cancelReminder(context: Context, log: WorkLog) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = log.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to cancel alarm", e)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleBackupReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", "backup_reminder")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            202,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate last day of the current month
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        cal.set(java.util.Calendar.HOUR_OF_DAY, 10) // 10:00 AM
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        if (cal.timeInMillis <= System.currentTimeMillis()) {
            // It's already past for this month, set for next month's last day
            cal.add(java.util.Calendar.MONTH, 1)
            cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        }

        val triggerTime = cal.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule backup alarm", e)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleDailyLogReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", "daily_work_log_reminder")
            putExtra("hour", hour)
            putExtra("minute", minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            203,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerTime = cal.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
            Log.d("ReminderScheduler", "Scheduled daily work log reminder for $hour:$minute")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule daily work log reminder", e)
        }
    }

    fun cancelDailyLogReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            203,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            Log.d("ReminderScheduler", "Cancelled daily work log reminder")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to cancel daily work log reminder", e)
        }
    }

    fun sendInstantTestNotification(context: Context) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", "daily_work_log_reminder")
            putExtra("hour", 17)
            putExtra("minute", 0)
        }
        context.sendBroadcast(intent)
    }
}
