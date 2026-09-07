package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.CaseDetailDialog
import com.example.ui.components.CloseCaseDialog
import com.example.ui.components.WorkLogCard
import com.example.ui.components.CompactWorkLogCard
import com.example.ui.components.CaseClosedCelebrationDialog
import com.example.ui.components.CheckInOutCaptureDialog
import com.example.ui.components.OcrTextScannerDialog
import com.example.ui.theme.BentoBackgroundDark
import com.example.ui.theme.BentoSurfaceDark
import com.example.ui.theme.DarkOrangeAccent
import com.example.ui.theme.DarkOrangeBorder
import com.example.ui.theme.SarabunFontFamily

import com.example.ui.components.TechGridBackground
import com.example.ui.theme.SciFiBackgroundDark
import com.example.ui.theme.SciFiBorderDark
import com.example.ui.theme.SciFiCyanAccent
import com.example.ui.theme.SciFiCyanGlow
import com.example.ui.theme.SciFiOrangeAccent
import com.example.ui.theme.SciFiOrangeBorder
import com.example.ui.theme.SciFiSurfaceDark
import com.example.ui.theme.SciFiSurfaceRecessed
import com.example.ui.theme.PromptFontFamily
import com.example.ui.theme.SarabunFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogListScreen(
    viewModel: WorkLogViewModel,
    onNavigateToCreate: () -> Unit
) {
    val context = LocalContext.current

    val workLogs by viewModel.filteredWorkLogs.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedPriority by viewModel.selectedPriority.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()
    val lastTechnicianName by viewModel.lastTechnicianName.collectAsState()

    // Dialog & Interactive States
    var logToClose by remember { mutableStateOf<WorkLog?>(null) }
    var logToViewDetail by remember { mutableStateOf<WorkLog?>(null) }
    var logToCheckIn by remember { mutableStateOf<WorkLog?>(null) }
    var logToCheckOut by remember { mutableStateOf<WorkLog?>(null) }
    var isCheckingOut by remember { mutableStateOf(false) }
    var celebrationLog by remember { mutableStateOf<WorkLog?>(null) }
    var showOcrScanner by remember { mutableStateOf(false) }
    var isFilterExpanded by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    val handleCloseCaseClick: (WorkLog) -> Unit = { log ->
        if (!log.isCheckedIn) {
            Toast.makeText(context, "⚠️ ไม่สามารถปิดเคสได้: กรุณากด Check IN หน้างานก่อน", Toast.LENGTH_LONG).show()
        } else if (!log.isCheckedOut) {
            Toast.makeText(context, "⚠️ ไม่สามารถปิดเคสได้: กรุณากด Check OUT หน้างานก่อน", Toast.LENGTH_LONG).show()
        } else {
            logToClose = log
        }
    }

    val openCount = allLogs.count { it.status == WorkLog.STATUS_OPEN }
    val closedCount = allLogs.count { it.status == WorkLog.STATUS_CLOSED }

    TechGridBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Screen Header Title & View Layout Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "รายการงาน",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        fontFamily = SarabunFontFamily
                    )
                    Text(
                        text = "ค้นหาและกรองเคสการทำงานทั้งหมดของคุณ",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        fontFamily = SarabunFontFamily
                    )
                }

                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SciFiSurfaceDark)
                        .border(BorderStroke(1.dp, SciFiBorderDark), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = if (isGridView) "มุมมองรายการ" else "มุมมอง 2 แถว",
                        tint = DarkOrangeAccent
                    )
                }
            }

            // Search Bar & Filter Toggle Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = com.example.util.ThaiTextFilter.processThaiBackspace(searchQuery, it) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        autoCorrect = false
                    ),
                    placeholder = {
                        Text(
                            "ค้นหาข้อความ, หมวดหมู่, ช่าง...",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            fontFamily = SarabunFontFamily
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = SciFiCyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "ล้าง",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SciFiSurfaceDark,
                        unfocusedContainerColor = SciFiSurfaceDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SciFiCyanAccent,
                        unfocusedBorderColor = SciFiBorderDark
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_logs_input")
                )

                // Filter Button
                OutlinedButton(
                    onClick = { isFilterExpanded = !isFilterExpanded },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isFilterExpanded || selectedCategory != null) SciFiOrangeBorder else SciFiBorderDark
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SciFiSurfaceDark,
                        contentColor = SciFiOrangeAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = SciFiOrangeAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "กรอง",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SciFiOrangeAccent,
                        fontFamily = SarabunFontFamily
                    )
                }
            }

            // Status View Tabs (Active Work List vs Archive View)
            val selectedTabIndex = when (filterStatus) {
                WorkLog.STATUS_CLOSED -> 1
                "ทั้งหมด" -> 2
                else -> 0
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    "📋 งานปัจจุบัน ($openCount)" to WorkLog.STATUS_OPEN,
                    "📦 คลังเคสปิดแล้ว ($closedCount)" to WorkLog.STATUS_CLOSED,
                    "🌐 ทั้งหมด (${allLogs.size})" to "ทั้งหมด"
                )

                tabs.forEachIndexed { idx, (label, statusVal) ->
                    val isSelected = selectedTabIndex == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF0B1724) else SciFiSurfaceRecessed)
                            .border(
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) SciFiCyanGlow else SciFiBorderDark
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.filterStatus.value = statusVal }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) SciFiCyanAccent else Color(0xFF94A3B8),
                            fontFamily = SarabunFontFamily
                        )
                    }
                }
            }


            // Collapsible Category & Date Filter Chips
            AnimatedVisibility(visible = isFilterExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "ช่วงเวลา:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SciFiCyanAccent,
                        fontFamily = SarabunFontFamily
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ทั้งหมด", "วันนี้", "เมื่อวาน", "7 วันล่าสุด", "30 วันล่าสุด").forEach { dateOption ->
                            val isSelected = selectedDateFilter == dateOption
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectedDateFilter.value = dateOption },
                                label = {
                                    Text(
                                        dateOption,
                                        fontSize = 11.sp,
                                        fontFamily = SarabunFontFamily
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SciFiSurfaceDark,
                                    labelColor = Color(0xFFCBD5E1),
                                    selectedContainerColor = SciFiCyanGlow,
                                    selectedLabelColor = Color(0xFF080E17)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Text(
                        text = "กรองตามหมวดหมู่:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SciFiCyanAccent,
                        fontFamily = SarabunFontFamily
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectedCategory.value = null },
                            label = {
                                Text(
                                    "ทุกหมวดหมู่",
                                    fontSize = 11.sp,
                                    fontFamily = SarabunFontFamily
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SciFiSurfaceDark,
                                labelColor = Color(0xFFCBD5E1),
                                selectedContainerColor = SciFiOrangeAccent,
                                selectedLabelColor = Color(0xFF080E17)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        WorkLog.CATEGORIES.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectedCategory.value = if (isSelected) null else cat
                                },
                                label = {
                                    Text(
                                        cat,
                                        fontSize = 11.sp,
                                        fontFamily = SarabunFontFamily
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SciFiSurfaceDark,
                                    labelColor = Color(0xFFCBD5E1),
                                    selectedContainerColor = SciFiOrangeAccent,
                                    selectedLabelColor = Color(0xFF080E17)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }


                }
            }

            // Work Logs List or Empty State Card
            if (workLogs.isEmpty()) {
                val isArchiveView = filterStatus == WorkLog.STATUS_CLOSED
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SciFiSurfaceDark),
                        border = BorderStroke(1.dp, SciFiBorderDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SciFiSurfaceRecessed)
                                    .border(BorderStroke(1.dp, SciFiCyanGlow), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isArchiveView) Icons.Default.Archive else Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = SciFiCyanAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Text(
                                text = if (isArchiveView) "ยังไม่มีเคสในคลังจัดเก็บ" else "ไม่พบรายการงาน",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White,
                                fontFamily = SarabunFontFamily
                            )

                            Text(
                                text = if (isArchiveView) {
                                    if (searchQuery.isNotBlank() || selectedCategory != null) {
                                        "ไม่พบเคสที่ปิดแล้วตามเงื่อนไขการค้นหา"
                                    } else {
                                        "เมื่อคุณกด 'ปิดเคส' ในรายการงานหลัก เคสที่ดำเนินการเสร็จสิ้นแล้วจะถูกย้ายมาเก็บไว้ในคลังนี้โดยอัตโนมัติ"
                                    }
                                } else if (searchQuery.isNotBlank() || selectedCategory != null) {
                                    "ลองเปลี่ยนคำค้นหาหรือตัวกรองหมวดหมู่"
                                } else {
                                    "กดที่แถบ \"ลงงาน\" ด้านล่างเพื่อเริ่มบันทึกเคสการทำงานใหม่"
                                },
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = SarabunFontFamily,
                                lineHeight = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(workLogs, key = { it.id }) { log ->
                            CompactWorkLogCard(
                                workLog = log,
                                onCardClick = { logToViewDetail = log },
                                onCloseCaseClick = { handleCloseCaseClick(log) },
                                onCheckInClick = { logToCheckIn = it },
                                onCheckOutClick = { logToCheckOut = it }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(workLogs, key = { it.id }) { log ->
                            WorkLogCard(
                                workLog = log,
                                onCardClick = { logToViewDetail = log },
                                onCloseCaseClick = { handleCloseCaseClick(log) },
                                onCheckInClick = { logToCheckIn = it },
                                onCheckOutClick = { logToCheckOut = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // Close Case Dialog
    logToClose?.let { log ->
        CloseCaseDialog(
            workLog = log,
            initialTechnician = lastTechnicianName,
            onDismiss = { logToClose = null },
            onConfirmClose = { solutions, technician, imageUri ->
                val closedLog = log.copy(
                    status = WorkLog.STATUS_CLOSED,
                    solutions = solutions,
                    technician = technician,
                    imageUri = imageUri
                )
                viewModel.closeCase(
                    logId = log.id,
                    solutions = solutions,
                    technician = technician,
                    imageUri = imageUri,
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    },
                    onComplete = {
                        Toast.makeText(context, "ปิดเคสสำเร็จ!", Toast.LENGTH_SHORT).show()
                    }
                )
                celebrationLog = closedLog
                logToClose = null
            }
        )
    }

    // Celebration Dialog on Case Closure
    celebrationLog?.let { closedLog ->
        CaseClosedCelebrationDialog(
            workLog = closedLog,
            onDismiss = { celebrationLog = null }
        )
    }

    // Quick OCR Scanner Dialog
    if (showOcrScanner) {
        OcrTextScannerDialog(
            viewModel = viewModel,
            onDismiss = { showOcrScanner = false }
        )
    }

    // Case Detail Dialog
    logToViewDetail?.let { log ->
        LaunchedEffect(log.id) {
            if (log.caseNumber.isNotBlank()) {
                viewModel.markCaseNotificationsAsRead(log.caseNumber)
            }
        }
        CaseDetailDialog(
            workLog = log,
            onDismiss = { logToViewDetail = null },
            onReopenCase = {
                viewModel.reopenCase(log.id)
                Toast.makeText(context, "เปิดเคสใหม่เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                logToViewDetail = null
            },
            onDeleteCase = {
                viewModel.deleteLog(log)
                Toast.makeText(context, "ลบงานสำเร็จ และซิงค์ลบใน Google Sheets", Toast.LENGTH_SHORT).show()
                logToViewDetail = null
            },
            onEditCase = { rawText, category, solutions, technician, priority ->
                viewModel.updateWorkLog(
                    workLog = log,
                    newRawText = rawText,
                    newCategory = category,
                    newSolutions = solutions,
                    newTechnician = technician,
                    newPriority = priority,
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    },
                    onComplete = {
                        Toast.makeText(context, "แก้ไขเคสสำเร็จ และบันทึกข้อมูลเรียบร้อย!", Toast.LENGTH_SHORT).show()
                    }
                )
                logToViewDetail = null
            },
            onPushToLineBot = { selectedLog ->
                viewModel.pushClosedCaseToLineBot(selectedLog) { result ->
                    if (result.isSuccess) {
                        Toast.makeText(context, "ส่งรายงานเข้า LINE Chat Bot สำเร็จ! ✅", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "LINE Bot: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onCheckInClick = { logToViewDetail = null; logToCheckIn = it },
            onCheckOutClick = { logToViewDetail = null; logToCheckOut = it }
        )
    }

    // Check IN Capture Dialog
    logToCheckIn?.let { log ->
        CheckInOutCaptureDialog(
            workLog = log,
            isCheckIn = true,
            onDismiss = { logToCheckIn = null },
            onConfirm = { uri ->
                viewModel.recordCheckIn(
                    workLog = log,
                    imageUri = uri,
                    onSuccess = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        logToCheckIn = null
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // Check OUT Capture Dialog
    logToCheckOut?.let { log ->
        CheckInOutCaptureDialog(
            workLog = log,
            isCheckIn = false,
            isProcessingExternal = isCheckingOut,
            onDismiss = {
                if (!isCheckingOut) {
                    logToCheckOut = null
                }
            },
            onConfirm = { uri ->
                isCheckingOut = true
                viewModel.recordCheckOut(
                    workLog = log,
                    imageUri = uri,
                    onSuccess = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        isCheckingOut = false
                        logToCheckOut = null
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        isCheckingOut = false
                    }
                )
            }
        )
    }
}

