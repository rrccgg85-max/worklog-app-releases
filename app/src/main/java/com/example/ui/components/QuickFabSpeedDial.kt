package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WorkLog
import com.example.ui.ActiveCaseStatus
import com.example.ui.WorkLogViewModel
import com.example.ui.theme.KanitFontFamily
import com.example.ui.theme.SciFiBorderDark
import com.example.ui.theme.SciFiCyanAccent
import com.example.ui.theme.SciFiCyanGlow
import com.example.ui.theme.SciFiOrangeAccent
import com.example.ui.theme.SciFiSurfaceRecessed

@Composable
fun QuickFabSpeedDial(
    viewModel: WorkLogViewModel,
    onVoiceInputRequested: () -> Unit,
    onOcrScanRequested: () -> Unit,
    onQuickUrgentCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showUrgentCaseDialog by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "fab_rotation"
    )

    Box(
        modifier = modifier.testTag("fab_speed_dial_container"),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Semi-transparent scrim when speed dial is open
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = false }
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
        ) {
            // Speed Dial Expanded Options
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(200)) + expandVertically(spring(dampingRatio = 0.8f)),
                exit = fadeOut(tween(150)) + shrinkVertically(spring(dampingRatio = 0.8f))
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: 🚨 New Urgent Case (เปิดเคสด่วน)
                    SpeedDialOption(
                        label = "⚡ เปิดเคสด่วน (Urgent Case)",
                        icon = Icons.Default.FlashOn,
                        accentColor = Color(0xFFEF4444),
                        containerColor = Color(0xFF2A0F12),
                        borderColor = Color(0xFFEF4444),
                        testTag = "btn_fab_urgent_case",
                        onClick = {
                            isExpanded = false
                            showUrgentCaseDialog = true
                        }
                    )

                    // Option 2: 🎙️ Quick Voice Input (พิมพ์ด้วยเสียง)
                    SpeedDialOption(
                        label = "🎙️ บันทึกด้วยเสียง (Voice Input)",
                        icon = Icons.Default.Mic,
                        accentColor = SciFiOrangeAccent,
                        containerColor = Color(0xFF221A0F),
                        borderColor = SciFiOrangeAccent.copy(alpha = 0.6f),
                        testTag = "btn_fab_voice_input",
                        onClick = {
                            isExpanded = false
                            onVoiceInputRequested()
                        }
                    )

                    // Option 3: 📸 Quick OCR Text Scanner
                    SpeedDialOption(
                        label = "📸 สแกนข้อความเอกสาร (OCR Scan)",
                        icon = Icons.Default.Widgets,
                        accentColor = Color(0xFF38BDF8),
                        containerColor = Color(0xFF0C2134),
                        borderColor = Color(0xFF38BDF8).copy(alpha = 0.6f),
                        testTag = "btn_fab_ocr_scan",
                        onClick = {
                            isExpanded = false
                            onOcrScanRequested()
                        }
                    )
                }
            }

            // Main Floating Action Button (Cyber / Material 3 Glow)
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                containerColor = SciFiCyanAccent,
                contentColor = Color(0xFF080E17),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier
                    .size(58.dp)
                    .border(
                        BorderStroke(2.dp, SciFiCyanGlow.copy(alpha = 0.7f)),
                        CircleShape
                    )
                    .testTag("fab_main_shortcuts")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "ปุ่มลัดคำสั่งด่วน (Shortcuts)",
                    tint = Color(0xFF080E17),
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotationAngle)
                )
            }
        }
    }

    // Modal Dialog: New Urgent Case
    if (showUrgentCaseDialog) {
        QuickUrgentCaseDialog(
            viewModel = viewModel,
            onDismiss = { showUrgentCaseDialog = false },
            onSuccess = {
                showUrgentCaseDialog = false
                onQuickUrgentCreated()
            }
        )
    }
}

@Composable
private fun SpeedDialOption(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        // Label Pill
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
            shadowElevation = 4.dp
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = KanitFontFamily,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }

        // Small Round Action Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(containerColor)
                .border(BorderStroke(1.5.dp, borderColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun QuickUrgentCaseDialog(
    viewModel: WorkLogViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var urgentText by remember { mutableStateOf("") }
    var customerOrLocation by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ซ่อมบำรุง") }
    var isSubmitting by remember { mutableStateOf(false) }

    val categories = listOf("ซ่อมบำรุง", "ฮาร์ดแวร์", "เน็ตเวิร์ก", "ติดตั้ง", "บริการลูกค้า")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.5.dp, Color(0xFFEF4444)), RoundedCornerShape(16.dp))
                    .testTag("dialog_urgent_case"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Urgent Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "เปิดเคสด่วน (Urgent Case)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = KanitFontFamily
                                )
                                Text(
                                    text = "บันทึกและส่งรายงานแจ้งเตือนเร่งด่วนทันที",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCA5A5),
                                    fontFamily = KanitFontFamily
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "ปิด", tint = Color.White)
                        }
                    }

                    // Priority Badge (Pre-locked to HIGH / ด่วนมาก)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF450A0A))
                            .border(BorderStroke(1.dp, Color(0xFFDC2626)), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                            Text(
                                text = "ระดับความเร่งด่วน: ด่วนพิเศษ (HIGH PRIORITY)",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = KanitFontFamily
                            )
                        }
                    }

                    // Category Selection
                    Text(
                        text = "เลือกหมวดหมู่งาน:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = KanitFontFamily
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFEF4444).copy(alpha = 0.25f) else SciFiSurfaceRecessed)
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) Color(0xFFEF4444) else SciFiBorderDark),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color(0xFFFCA5A5) else Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = KanitFontFamily
                                )
                            }
                        }
                    }

                    // Urgent Details Input Field
                    OutlinedTextField(
                        value = urgentText,
                        onValueChange = { urgentText = it },
                        placeholder = { Text("รายละเอียดปัญหา / อาการเสียเร่งด่วน...", fontFamily = KanitFontFamily, fontSize = 13.sp, color = Color(0xFF64748B)) },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_urgent_case_details"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = SciFiSurfaceRecessed,
                            unfocusedContainerColor = SciFiSurfaceRecessed,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Customer / Location Input (Optional)
                    OutlinedTextField(
                        value = customerOrLocation,
                        onValueChange = { customerOrLocation = it },
                        placeholder = { Text("ชื่อลูกค้า / สถานที่เกิดเหตุ (ถ้ามี)...", fontFamily = KanitFontFamily, fontSize = 13.sp, color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_urgent_case_location"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = SciFiSurfaceRecessed,
                            unfocusedContainerColor = SciFiSurfaceRecessed,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Text("ยกเลิก", color = Color(0xFF94A3B8), fontFamily = KanitFontFamily)
                        }

                        Button(
                            onClick = {
                                if (urgentText.isBlank()) {
                                    Toast.makeText(context, "กรุณากรอกรายละเอียดปัญหาด่วน", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSubmitting = true
                                val combinedDetails = buildString {
                                    append("🚨 [เคสด่วน / HIGH PRIORITY]\n")
                                    append(urgentText.trim())
                                    if (customerOrLocation.isNotBlank()) {
                                        append("\n📍 สถานที่/ลูกค้า: ${customerOrLocation.trim()}")
                                    }
                                }

                                // Ensure case status is Active
                                viewModel.setCaseStatus(ActiveCaseStatus.ACTIVE)

                                viewModel.createWorkLog(
                                    rawText = combinedDetails,
                                    customCategory = selectedCategory,
                                    priority = WorkLog.PRIORITY_HIGH,
                                    onError = { error ->
                                        isSubmitting = false
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    },
                                    onComplete = {
                                        isSubmitting = false
                                        Toast.makeText(context, "⚡ เปิดเคสด่วนสำเร็จ!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    }
                                )
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("btn_submit_urgent_case"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("บันทึกเปิดเคสด่วน", fontWeight = FontWeight.Bold, fontFamily = KanitFontFamily)
                        }
                    }
                }
            }
        }
    }
}
