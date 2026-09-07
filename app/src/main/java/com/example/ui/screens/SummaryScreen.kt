package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.ui.components.Category30DayChart
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkLog
import com.example.ui.WorkLogViewModel
import com.example.ui.theme.BentoBackgroundDark
import com.example.ui.theme.BentoSurfaceDark
import com.example.ui.theme.DarkOrangeAccent
import com.example.ui.theme.DarkOrangeBorder
import com.example.ui.theme.SarabunFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SummaryScreen(
    viewModel: WorkLogViewModel
) {
    val context = LocalContext.current
    val allLogs by viewModel.allLogs.collectAsState()

    var currentCalendar by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    var selectedDayCal by remember { mutableStateOf<java.util.Calendar?>(null) }
    var selectedDayStr by remember { mutableStateOf("") }

    val selectedDayLogs = remember(allLogs, selectedDayCal) {
        val targetCal = selectedDayCal
        if (targetCal == null) null
        else {
            allLogs.filter { log ->
                val logCal = java.util.Calendar.getInstance().apply { timeInMillis = log.timestamp }
                logCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
                logCal.get(java.util.Calendar.DAY_OF_YEAR) == targetCal.get(java.util.Calendar.DAY_OF_YEAR)
            }
        }
    }

    val totalCount = allLogs.size
    val openCount = allLogs.count { it.status == WorkLog.STATUS_OPEN }
    val closedCount = allLogs.count { it.status == WorkLog.STATUS_CLOSED }

    val completionRate = if (totalCount > 0) (closedCount.toFloat() / totalCount.toFloat() * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoBackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Screen Header Title
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "สรุปผล",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                fontFamily = SarabunFontFamily
            )
            Text(
                text = "สถิติและรายงานผลการทำงานภาพรวมของคุณ",
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                fontFamily = SarabunFontFamily
            )
        }

        // Stats Overview Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ภาพรวมการดำเนินงาน",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = SarabunFontFamily
                )

                // Completion Rate Progress Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "อัตราการปิดเคสสำเร็จ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8),
                            fontFamily = SarabunFontFamily
                        )
                        Text(
                            text = "$completionRate%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOrangeAccent,
                            fontFamily = SarabunFontFamily
                        )
                    }
                    LinearProgressIndicator(
                        progress = if (totalCount > 0) closedCount.toFloat() / totalCount else 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = DarkOrangeAccent,
                        trackColor = Color(0xFF111827)
                    )
                }

                // Grid Stats: Total, Open, Closed Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111827))
                            .border(BorderStroke(1.dp, Color(0xFF374151)), RoundedCornerShape(8.dp))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "งานทั้งหมด",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontFamily = SarabunFontFamily
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalCount",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }

                    // Open Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111827))
                            .border(BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "เปิดเคสอยู่",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontFamily = SarabunFontFamily
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$openCount",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }

                    // Closed Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111827))
                            .border(BorderStroke(1.dp, DarkOrangeBorder), RoundedCornerShape(8.dp))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ปิดเคสแล้ว",
                                fontSize = 11.sp,
                                color = DarkOrangeAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SarabunFontFamily
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$closedCount",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }
                }
            }
        }

        // 30-Day Category Distribution Dashboard Chart
        Category30DayChart(allLogs = allLogs)

        // Calendar Component
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ปฏิทินบันทึกงานประจำวัน",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = SarabunFontFamily
                )

                val monthCalendar = remember(currentCalendar) {
                    val cal = currentCalendar.clone() as java.util.Calendar
                    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    cal
                }

                val firstDayOfWeek = monthCalendar.get(java.util.Calendar.DAY_OF_WEEK)
                val daysInMonth = monthCalendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

                val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale("th", "TH"))
                val monthYearTitle = monthYearFormatter.format(monthCalendar.time)

                val dayLabels = listOf("อา", "จ", "อ", "พ", "พฤ", "ศ", "ส")

                val totalCells = (firstDayOfWeek - 1) + daysInMonth
                val rowsCount = (totalCells + 6) / 7

                // Calendar Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val cal = currentCalendar.clone() as java.util.Calendar
                            cal.add(java.util.Calendar.MONTH, -1)
                            currentCalendar = cal
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "เดือนก่อนหน้า",
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    Text(
                        text = monthYearTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DarkOrangeAccent,
                        fontFamily = SarabunFontFamily
                    )

                    IconButton(
                        onClick = {
                            val cal = currentCalendar.clone() as java.util.Calendar
                            cal.add(java.util.Calendar.MONTH, 1)
                            currentCalendar = cal
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "เดือนถัดไป",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                // Day Labels Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayLabels.forEachIndexed { index, label ->
                        val labelColor = when (index) {
                            0 -> Color(0xFFEF4444)
                            6 -> DarkOrangeAccent
                            else -> Color(0xFF94A3B8)
                        }
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = labelColor,
                            fontFamily = SarabunFontFamily
                        )
                    }
                }

                // Days Grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until rowsCount) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                val dayNum = cellIndex - (firstDayOfWeek - 2)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayNum in 1..daysInMonth) {
                                        val cellCal = monthCalendar.clone() as java.util.Calendar
                                        cellCal.set(java.util.Calendar.DAY_OF_MONTH, dayNum)

                                        val cellLogs = allLogs.filter { log ->
                                            val logCal = java.util.Calendar.getInstance().apply { timeInMillis = log.timestamp }
                                            logCal.get(java.util.Calendar.YEAR) == cellCal.get(java.util.Calendar.YEAR) &&
                                            logCal.get(java.util.Calendar.DAY_OF_YEAR) == cellCal.get(java.util.Calendar.DAY_OF_YEAR)
                                        }

                                        val hasTasks = cellLogs.isNotEmpty()

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .clickable {
                                                    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("th", "TH"))
                                                    selectedDayStr = sdf.format(cellCal.time)
                                                    selectedDayCal = cellCal.clone() as java.util.Calendar
                                                }
                                                .padding(2.dp)
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasTasks) DarkOrangeAccent else Color.White,
                                                fontFamily = SarabunFontFamily
                                            )
                                            if (hasTasks) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(DarkOrangeAccent)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Quick Summary section
        val todayLogs = remember(allLogs) {
            val todayCal = java.util.Calendar.getInstance()
            allLogs.filter { log ->
                val logCal = java.util.Calendar.getInstance().apply { timeInMillis = log.timestamp }
                logCal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR) &&
                logCal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR)
            }
        }

        val todayTotal = todayLogs.size
        val todayOpen = todayLogs.count { it.status == WorkLog.STATUS_OPEN }
        val todayClosed = todayLogs.count { it.status == WorkLog.STATUS_CLOSED }

        val quickSummaryText = remember(todayLogs) {
            val todayStr = try {
                SimpleDateFormat("dd MMMM yyyy", Locale("th", "TH")).format(Date())
            } catch (e: Exception) {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            }
            buildString {
                append("📝 รายงานสรุปด่วนประจำวัน ($todayStr)\n")
                append("-------------------------------\n")
                append("• เคสงานวันนี้ทั้งหมด: $todayTotal เคส\n")
                append("• ปิดเคสแล้ว: $todayClosed เคส | อยู่ระหว่างดำเนินการ: $todayOpen เคส\n\n")
                append("รายการและผลลัพธ์งานวันนี้:\n")
                if (todayLogs.isEmpty()) {
                    append(" - ยังไม่มีรายการบันทึกสำหรับวันนี้\n")
                } else {
                    todayLogs.forEachIndexed { index, log ->
                        val statusIcon = if (log.status == WorkLog.STATUS_CLOSED) "✅" else "⏳"
                        append(" ${index + 1}. $statusIcon [${log.category}] ${log.rawText}")
                        if (log.solutions.isNotEmpty()) {
                            append(" -> แก้ไข: ${log.solutions}")
                        }
                        append("\n")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "รายงานสรุปด่วนประจำวัน",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = SarabunFontFamily
                )

                // Quick Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111827))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("วันนี้ทั้งหมด", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontFamily = SarabunFontFamily)
                            Text("$todayTotal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = SarabunFontFamily)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111827))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ปิดแล้ว", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkOrangeAccent, fontFamily = SarabunFontFamily)
                            Text("$todayClosed", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = SarabunFontFamily)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF111827))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("รอดำเนินการ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontFamily = SarabunFontFamily)
                            Text("$todayOpen", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = SarabunFontFamily)
                        }
                    }
                }

                // Key findings checklist
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (todayLogs.isEmpty()) {
                        Text(
                            text = "• ยังไม่มีข้อมูลรายการบันทึกสำหรับวันนี้",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            fontFamily = SarabunFontFamily
                        )
                    } else {
                        todayLogs.forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (log.status == WorkLog.STATUS_CLOSED) "✅" else "⏳",
                                    fontSize = 13.sp
                                )
                                Column {
                                    Text(
                                        text = "[${log.category}] ${log.rawText}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        fontFamily = SarabunFontFamily
                                    )
                                    if (log.solutions.isNotEmpty()) {
                                        Text(
                                            text = "แก้ไข: ${log.solutions}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = SarabunFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Daily Quick Report", quickSummaryText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "คัดลอกรายงานสรุปด่วนประจำวันเรียบร้อยแล้ว!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("copy_quick_report_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkOrangeAccent,
                        contentColor = Color(0xFF111827)
                    )
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF111827), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "คัดลอกรายงานสรุปด่วนประจำวัน",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        fontFamily = SarabunFontFamily,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Tapped Day Details Popup Dialog
    if (selectedDayLogs != null) {
        val logs = selectedDayLogs!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedDayCal = null },
            confirmButton = {
                TextButton(onClick = { selectedDayCal = null }) {
                    Text("ปิด", fontFamily = SarabunFontFamily, fontWeight = FontWeight.Bold, color = DarkOrangeAccent)
                }
            },
            containerColor = BentoSurfaceDark,
            title = {
                Text(
                    text = "งานประจำวันที่ $selectedDayStr",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = SarabunFontFamily,
                    color = Color.White
                )
            },
            text = {
                if (logs.isEmpty()) {
                    Text("ไม่มีบันทึกงานสำหรับวันนี้", fontFamily = SarabunFontFamily, color = Color(0xFF94A3B8))
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(logs.size) { idx ->
                            val log = logs[idx]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                                border = BorderStroke(1.dp, Color(0xFF374151))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "[${log.category}]",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = DarkOrangeAccent,
                                            fontFamily = SarabunFontFamily
                                        )
                                        Text(
                                            text = log.status,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.status == WorkLog.STATUS_CLOSED) DarkOrangeAccent else Color(0xFFEF4444),
                                            fontFamily = SarabunFontFamily
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = log.rawText,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontFamily = SarabunFontFamily
                                    )
                                    if (log.solutions.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Solutions: ${log.solutions}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = SarabunFontFamily
                                        )
                                    }
                                    if (!log.technician.isNullOrBlank()) {
                                        Text(
                                            text = "ช่างผู้รับผิดชอบ: ${log.technician}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = SarabunFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

