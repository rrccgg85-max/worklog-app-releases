package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkLog
import com.example.ui.theme.DarkOrangeAccent
import com.example.ui.theme.SarabunFontFamily
import com.example.ui.theme.SciFiSurfaceDark
import com.example.util.ImageExportUtils
import com.example.util.SnUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CaseClosedCelebrationDialog(
    workLog: WorkLog,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSavingImage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SciFiSurfaceDark,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF065F46).copy(alpha = 0.3f))
                        .border(BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.8f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "ปิดเคสสำเร็จเรียบร้อย",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    fontFamily = SarabunFontFamily,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "เคส ${workLog.formattedCaseNumber}",
                        color = Color(0xFF818CF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = SarabunFontFamily
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "การบันทึกสรุปปิดงานเสร็จสิ้นอย่างสมบูรณ์ ระบบประมวลผลการทำงานดังนี้:",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = SarabunFontFamily
                )

                // Check list items
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                ) {
                    CelebrationCheckItem(text = "บันทึกสรุปแนวทางแก้ไข & ช่างผู้รับผิดชอบ")
                    CelebrationCheckItem(text = "ส่งรายงานสรุปเข้า LINE Chat Bot อัตโนมัติ")
                    CelebrationCheckItem(text = "อัปโหลดและซิงค์ข้อมูลลง Google Drive / Sheets")
                    if (workLog.imageUriList.isNotEmpty()) {
                        CelebrationCheckItem(text = "แนบรูปภาพหลักฐานงาน (${workLog.imageUriList.size} ภาพ)")
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!isSavingImage) {
                            isSavingImage = true
                            Toast.makeText(context, "กำลังสร้างภาพสรุปปิดเคส...", Toast.LENGTH_SHORT).show()
                            scope.launch(Dispatchers.Default) {
                                try {
                                    val snInfo = SnUtils.extractSerialNumbers(workLog.solutions + "\n" + workLog.rawText)
                                    val bitmap = ImageExportUtils.generateSummaryBitmap(
                                        context = context,
                                        workLog = workLog,
                                        solutions = workLog.solutions,
                                        oldSn = snInfo.oldSn ?: "",
                                        newSn = snInfo.newSn ?: "",
                                        technician = workLog.technician
                                    )
                                    val uri = ImageExportUtils.saveBitmapToGallery(
                                        context = context,
                                        bitmap = bitmap,
                                        title = "Summary_${workLog.formattedCaseNumber}"
                                    )
                                    bitmap.recycle()
                                    withContext(Dispatchers.Main) {
                                        if (uri != null) {
                                            Toast.makeText(context, "บันทึกรูปสรุปเข้าแกลเลอรี่เรียบร้อย! 📸", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "ไม่สามารถบันทึกภาพได้", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isSavingImage = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkOrangeAccent,
                        contentColor = Color(0xFF111827)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("celebration_save_summary_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFF111827),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSavingImage) "กำลังสร้างภาพ..." else "บันทึกภาพสรุปงานลงแกลเลอรี่ 📸",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = SarabunFontFamily
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("celebration_done_btn")
                ) {
                    Text(
                        text = "เสร็จสิ้น (ตกลง)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = SarabunFontFamily
                    )
                }
            }
        }
    )
}

@Composable
private fun CelebrationCheckItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            color = Color(0xFFE2E8F0),
            fontSize = 11.5.sp,
            fontFamily = SarabunFontFamily
        )
    }
}
