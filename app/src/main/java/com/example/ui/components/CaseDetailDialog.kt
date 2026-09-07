package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.PaddingValues
import com.example.ai.CustomerInfoExtractor
import androidx.compose.material.icons.filled.Tag
import com.example.util.SnUtils
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.WorkLog
import com.example.ui.theme.SarabunFontFamily
import com.example.ui.theme.StatusClosedContainerLight
import com.example.ui.theme.StatusClosedLight
import com.example.ui.theme.StatusOpenLight
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun CaseDetailDialog(
    workLog: WorkLog,
    onDismiss: () -> Unit,
    onReopenCase: () -> Unit,
    onDeleteCase: () -> Unit,
    onEditCase: ((rawText: String, category: String, solutions: String, technician: String, priority: String) -> Unit)? = null,
    onPushToLineBot: ((WorkLog) -> Unit)? = null,
    onCheckInClick: ((WorkLog) -> Unit)? = null,
    onCheckOutClick: ((WorkLog) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val isClosed = workLog.status == WorkLog.STATUS_CLOSED
    val formattedSummary = workLog.buildClosedSummaryTemplate()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var viewingPhotoUri by remember { mutableStateOf<String?>(null) }

    if (showDeleteConfirmDialog) {
        var remainingTimeMs by remember { mutableLongStateOf(3000L) }
        var isCancelled by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val startTime = System.currentTimeMillis()
            val totalDuration = 3000L
            while (!isCancelled) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = totalDuration - elapsed
                if (remaining <= 0) {
                    remainingTimeMs = 0L
                    showDeleteConfirmDialog = false
                    onDeleteCase()
                    break
                }
                remainingTimeMs = remaining
                delay(30L)
            }
        }

        AlertDialog(
            onDismissRequest = {
                isCancelled = true
                showDeleteConfirmDialog = false
            },
            containerColor = com.example.ui.theme.BentoSurfaceDark,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "กำลังลบเคส ${workLog.formattedCaseNumber}...",
                        fontWeight = FontWeight.Bold,
                        fontFamily = SarabunFontFamily,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ระบบกำลังนับถอยหลัง 3 วินาทีเพื่อดำเนินการลบเคส ${workLog.formattedCaseNumber} ออกจากแอปและ Google Sheets\n\nหากกดผิด สามารถกดปุ่ม \"กดเพื่อไม่ลบ (ยกเลิก)\" เพื่อยกเลิกการลบได้ทันที",
                        fontFamily = SarabunFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0)
                    )

                    val progress = (remainingTimeMs / 3000f).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.error,
                            trackColor = Color(0xFF374151)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "สถานะ: กำลังนับถอยหลังลบ",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = SarabunFontFamily
                            )
                            Text(
                                text = "เหลือ ${(remainingTimeMs / 1000f).let { String.format(java.util.Locale.US, "%.1f", it) }} วินาที",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = SarabunFontFamily
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isCancelled = true
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.DarkOrangeAccent,
                        contentColor = Color(0xFF111827)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🛑 กดเพื่อไม่ลบ (ยกเลิก)", fontFamily = SarabunFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        isCancelled = true
                        showDeleteConfirmDialog = false
                        onDeleteCase()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ลบทันที", fontFamily = SarabunFontFamily)
                }
            }
        )
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm น.", Locale("th", "TH")) }
    val dateString = dateFormat.format(Date(workLog.timestamp))

    if (showEditDialog) {
        var rawTextTFValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(workLog.cleanRawText.ifBlank { workLog.rawText ?: "" })) }
        var categoryTFValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(workLog.category ?: "")) }
        var solutionsTFValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(workLog.solutions ?: "")) }
        var technicianTFValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(workLog.technician ?: "")) }
        var prioritySelected by remember { mutableStateOf(workLog.priority ?: WorkLog.PRIORITY_MEDIUM) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("แก้ไขข้อมูลเคส ${workLog.formattedCaseNumber}", fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = rawTextTFValue,
                        onValueChange = { rawTextTFValue = com.example.util.ThaiTextFilter.processThaiBackspace(rawTextTFValue, it) },
                        label = { Text("รายละเอียดงาน (Raw Text)", fontFamily = SarabunFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                            autoCorrect = false
                        ),
                        minLines = 3
                    )



                    OutlinedTextField(
                        value = categoryTFValue,
                        onValueChange = { categoryTFValue = com.example.util.ThaiTextFilter.processThaiBackspace(categoryTFValue, it) },
                        label = { Text("หมวดหมู่ (Category)", fontFamily = SarabunFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                            autoCorrect = false
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = solutionsTFValue,
                        onValueChange = { solutionsTFValue = com.example.util.ThaiTextFilter.processThaiBackspace(solutionsTFValue, it) },
                        label = { Text("แนวทางแก้ไข (Solutions)", fontFamily = SarabunFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                            autoCorrect = false
                        ),
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = technicianTFValue,
                        onValueChange = { technicianTFValue = com.example.util.ThaiTextFilter.processThaiBackspace(technicianTFValue, it) },
                        label = { Text("ช่างผู้รับผิดชอบ (Technician)", fontFamily = SarabunFontFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                            autoCorrect = false
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hasOverride = workLog.rawText.contains("ข้อมูลติดต่อแก้ไข") || workLog.rawText.contains("ข้อมูลแก้ไข")
                        val currentExtracted = CustomerInfoExtractor.extract(workLog.rawText)
                        val finalRaw = if (hasOverride) {
                            CustomerInfoExtractor.updateRawTextWithManualInfo(
                                currentRawText = rawTextTFValue.text.trim(),
                                name = currentExtracted.customerName ?: "",
                                phone = currentExtracted.phone ?: "",
                                location = currentExtracted.buildingOrAddress ?: "",
                                onsiteTime = currentExtracted.onsiteTime ?: ""
                            )
                        } else {
                            rawTextTFValue.text.trim()
                        }
                        onEditCase?.invoke(
                            finalRaw,
                            categoryTFValue.text.trim(),
                            solutionsTFValue.text.trim(),
                            technicianTFValue.text.trim(),
                            prioritySelected
                        )
                        showEditDialog = false
                    }
                ) {
                    Text("บันทึกการแก้ไข", fontFamily = SarabunFontFamily)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) {
                    Text("ยกเลิก", fontFamily = SarabunFontFamily)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "รายละเอียด ${workLog.formattedCaseNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = SarabunFontFamily
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        onEditCase?.let {
                            IconButton(
                                onClick = { showEditDialog = true },
                                modifier = Modifier.size(36.dp).testTag("edit_case_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "แก้ไขเคส",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.size(36.dp).testTag("delete_case_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "ลบงาน",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val statusTextColor = if (isClosed) {
                        Color(0xFF10B981)
                    } else {
                        Color(0xFFEF4444)
                    }
                    val statusBgColor = if (isClosed) {
                        Color(0xFF064E3B).copy(alpha = 0.6f)
                    } else {
                        Color(0xFF7F1D1D).copy(alpha = 0.6f)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = workLog.status,
                            color = statusTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            fontFamily = SarabunFontFamily
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2D3748))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = workLog.formattedCaseNumber,
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            fontFamily = SarabunFontFamily
                        )
                    }

                    if (workLog.syncStatus == WorkLog.SYNC_SYNCED) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF064E3B).copy(alpha = 0.8f))
                                .border(BorderStroke(0.5.dp, Color(0xFF10B981)), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Synced to Drive/Cloud",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (workLog.imageUriList.isNotEmpty()) "Drive Synced" else "Synced",
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = SarabunFontFamily,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF334155))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = workLog.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            softWrap = false,
                            fontFamily = SarabunFontFamily
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "บันทึกเมื่อ: $dateString",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // AI Extracted Info Card (Customer Name, Phone, Building/Address)
                ExtractedCustomerInfoCard(
                    rawText = workLog.rawText,
                    onManualInfoEdited = { name, phone, location, onsiteTime ->
                        val updatedRaw = CustomerInfoExtractor.updateRawTextWithManualInfo(
                            currentRawText = workLog.rawText,
                            name = name,
                            phone = phone,
                            location = location,
                            onsiteTime = onsiteTime
                        )
                        onEditCase?.invoke(updatedRaw, workLog.category, workLog.solutions, workLog.technician, workLog.priority)
                    }
                )

                // Check IN & Check OUT Status Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "📌 บันทึกเวลา Check IN / Check OUT หน้างาน",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = SarabunFontFamily
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Check IN Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (workLog.isCheckedIn) Color(0xFF064E3B) else Color(0xFF1E293B))
                                    .border(
                                        BorderStroke(1.dp, if (workLog.isCheckedIn) Color(0xFF10B981) else Color(0xFF334155)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (workLog.isCheckedIn && !workLog.checkInImageUri.isNullOrBlank()) {
                                            viewingPhotoUri = workLog.checkInImageUri
                                        } else if (!isClosed) {
                                            onCheckInClick?.invoke(workLog)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (workLog.isCheckedIn) Color(0xFF34D399) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Check IN",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (workLog.isCheckedIn) Color(0xFF34D399) else Color(0xFFCBD5E1),
                                            fontFamily = SarabunFontFamily
                                        )
                                    }

                                    if (workLog.isCheckedIn) {
                                        Text(
                                            text = workLog.formattedCheckInTime ?: "",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = SarabunFontFamily
                                        )
                                        if (!workLog.checkInImageUri.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                            ) {
                                                AsyncImage(
                                                    model = workLog.checkInImageUri,
                                                    contentDescription = "รูป Check IN",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "ยังไม่ได้ Check IN",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = SarabunFontFamily
                                        )
                                        if (!isClosed) {
                                            Button(
                                                onClick = { onCheckInClick?.invoke(workLog) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF10B981),
                                                    contentColor = Color(0xFF0F172A)
                                                ),
                                                shape = RoundedCornerShape(5.dp),
                                                modifier = Modifier.fillMaxWidth().height(26.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("กด Check IN", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                                            }
                                        }
                                    }
                                }
                            }

                            // Check OUT Box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (workLog.isCheckedOut) Color(0xFF78350F) else Color(0xFF1E293B))
                                    .border(
                                        BorderStroke(1.dp, if (workLog.isCheckedOut) Color(0xFFF59E0B) else Color(0xFF334155)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (workLog.isCheckedOut && !workLog.checkOutImageUri.isNullOrBlank()) {
                                            viewingPhotoUri = workLog.checkOutImageUri
                                        } else if (workLog.isCheckedIn && !isClosed) {
                                            onCheckOutClick?.invoke(workLog)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (workLog.isCheckedOut) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Check OUT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (workLog.isCheckedOut) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
                                            fontFamily = SarabunFontFamily
                                        )
                                    }

                                    if (workLog.isCheckedOut) {
                                        Text(
                                            text = workLog.formattedCheckOutTime ?: "",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = SarabunFontFamily
                                        )
                                        if (!workLog.checkOutImageUri.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                            ) {
                                                AsyncImage(
                                                    model = workLog.checkOutImageUri,
                                                    contentDescription = "รูป Check OUT",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = if (workLog.isCheckedIn) "รอ Check OUT" else "ต้อง Check IN ก่อน",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = SarabunFontFamily
                                        )
                                        if (workLog.isCheckedIn && !isClosed) {
                                            Button(
                                                onClick = { onCheckOutClick?.invoke(workLog) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFFF59E0B),
                                                    contentColor = Color(0xFF0F172A)
                                                ),
                                                shape = RoundedCornerShape(5.dp),
                                                modifier = Modifier.fillMaxWidth().height(26.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("กด Check OUT", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isClosed) {
                    Text(
                        text = "สรุปปิดเคส (ปิดเคส + Solutions = ):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val snInfo = remember(workLog.solutions) { SnUtils.extractSerialNumbers(workLog.solutions + "\n" + workLog.rawText) }
                    if (snInfo.hasSerialNumbers) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🔍 ประวัติ Serial Number ของเคสนี้:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                snInfo.oldSn?.let { old ->
                                    if (old.isNotBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFEF2F2))
                                                .border(BorderStroke(0.5.dp, Color(0xFFFCA5A5)), RoundedCornerShape(8.dp))
                                                .clickable { SnUtils.copySnToClipboard(context, "S/N เก่า", old) }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Tag,
                                                    contentDescription = null,
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "S/N เดิม (เก่า): $old",
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB91C1C)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "คัดลอก",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                snInfo.newSn?.let { new ->
                                    if (new.isNotBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFECFDF5))
                                                .border(BorderStroke(0.5.dp, Color(0xFF6EE7B7)), RoundedCornerShape(8.dp))
                                                .clickable { SnUtils.copySnToClipboard(context, "S/N ใหม่", new) }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Tag,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "S/N เปลี่ยนแทน (ใหม่): $new",
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857)
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "คัดลอก",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Template Output Card formatted exactly as requested
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("summary_template_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = formattedSummary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = SarabunFontFamily,
                                    lineHeight = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val images = workLog.imageUriList
                    if (images.isNotEmpty()) {
                        Text(
                            text = "ภาพถ่าย / หลักฐานงาน (${images.size} รูป):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (images.size == 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.05f))
                            ) {
                                AsyncImage(
                                    model = images.first(),
                                    contentDescription = "ภาพหลักฐานงาน",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(images) { imgUri ->
                                    Box(
                                        modifier = Modifier
                                            .size(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.05f))
                                    ) {
                                        AsyncImage(
                                            model = imgUri,
                                            contentDescription = "ภาพหลักฐานงาน",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Summary Image Export Card (Save Image to Gallery)
                    var isSavingImage by remember { mutableStateOf(false) }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F1E2E)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ภาพสรุปรายงานปิดเคส (Case Summary Image):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    fontFamily = SarabunFontFamily
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!isSavingImage) {
                                            isSavingImage = true
                                            Toast.makeText(context, "กำลังสร้างรูปภาพสรุปปิดเคส...", Toast.LENGTH_SHORT).show()
                                            scope.launch(Dispatchers.Default) {
                                                try {
                                                    val snInfo = com.example.util.SnUtils.extractSerialNumbers(workLog.solutions + "\n" + workLog.rawText)
                                                    val summaryBitmap = com.example.util.ImageExportUtils.generateSummaryBitmap(
                                                        context = context,
                                                        workLog = workLog,
                                                        solutions = workLog.solutions,
                                                        oldSn = snInfo.oldSn ?: "",
                                                        newSn = snInfo.newSn ?: "",
                                                        technician = workLog.technician
                                                    )
                                                    try {
                                                        val savedUri = com.example.util.ImageExportUtils.saveBitmapToGallery(
                                                            context = context,
                                                            bitmap = summaryBitmap,
                                                            title = "CaseSummary_${workLog.formattedCaseNumber}_${workLog.id}"
                                                        )
                                                        withContext(Dispatchers.Main) {
                                                            if (savedUri != null) {
                                                                Toast.makeText(context, "บันทึกภาพสรุปปิดเคสลงแกลเลอรี่สำเร็จ! 🎉", Toast.LENGTH_LONG).show()
                                                            } else {
                                                                Toast.makeText(context, "ล้มเหลวในการบันทึกภาพ", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    } finally {
                                                        summaryBitmap.recycle()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                } finally {
                                                    isSavingImage = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSavingImage,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("save_summary_image_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0284C7),
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (isSavingImage) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("กำลังบันทึกภาพ...", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "บันทึกภาพสรุป",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("บันทึกภาพสรุปปิดเคส", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                                    }
                                }
                            }
                        }
                    }

                    // Copy & Share Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("WorkLog Closed Summary", formattedSummary)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "คัดลอกข้อความปิดเคสเรียบร้อยแล้ว!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_summary_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.CustomAccentBlue,
                                contentColor = com.example.ui.theme.SlateDarkText
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = com.example.ui.theme.SlateDarkText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("คัดลอกข้อความ", fontWeight = FontWeight.Bold, color = com.example.ui.theme.SlateDarkText)
                        }

                        OutlinedButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, formattedSummary)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "แชร์ผลสรุปปิดเคส")
                                context.startActivity(shareIntent)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Open case display
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ข้อความดิบตอนลงงาน:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = workLog.rawText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Push to LINE Bot action button
                onPushToLineBot?.let { pushAction ->
                    OutlinedButton(
                        onClick = { pushAction(workLog) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF06C755)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF06C755))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "ส่งเข้า LINE Bot",
                            tint = Color(0xFF06C755),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isClosed) "ส่งรายงานปิดเคสเข้า LINE Bot" else "ส่งแจ้งเตือนเคสนี้เข้า LINE Bot",
                            color = Color(0xFF06C755),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SarabunFontFamily
                        )
                    }
                }

                // Reopen option if closed
                if (isClosed) {
                    OutlinedButton(
                        onClick = onReopenCase,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("กลับไปเปิดเคสใหม่")
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "ลบเคส",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ลบเคส",
                        fontFamily = SarabunFontFamily,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                onEditCase?.let {
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "แก้ไข",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "แก้ไขข้อมูล",
                            fontFamily = SarabunFontFamily,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "ตกลง",
                        fontFamily = SarabunFontFamily,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )

    viewingPhotoUri?.let { photoUri ->
        AlertDialog(
            onDismissRequest = { viewingPhotoUri = null },
            confirmButton = {
                Button(
                    onClick = { viewingPhotoUri = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ปิด", fontFamily = SarabunFontFamily)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "รูปหลักฐานสแตมป์เวลา Check IN/OUT",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
