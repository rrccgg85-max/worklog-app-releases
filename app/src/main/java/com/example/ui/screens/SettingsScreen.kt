package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import android.app.TimePickerDialog
import java.util.Calendar
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WorkLogViewModel
import com.example.ui.theme.BentoBackgroundDark
import com.example.ui.theme.BentoSurfaceDark
import com.example.ui.theme.DarkOrangeAccent
import com.example.ui.theme.DarkOrangeBorder
import com.example.ui.theme.KanitFontFamily
import com.example.util.ExportUtils

/**
 * Enum defining navigation states for Settings Screen
 */
enum class SettingsSubPage {
    MAIN,
    NOTIFICATION_SETTINGS,
    GOOGLE_SHEETS_LINE,
    LINE_MESSAGING_API,
    SYNC_MAINTENANCE,
    ABOUT_TERMS,
    ACTIVITY_LOGS,
    PATCH_UPDATE
}

@Composable
fun SettingsScreen(
    viewModel: WorkLogViewModel,
    themeMode: String = "dark",
    onThemeModeChange: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentSubPage by remember { mutableStateOf(SettingsSubPage.MAIN) }

    val targetSubPage by viewModel.targetSettingsSubPage.collectAsState()
    LaunchedEffect(targetSubPage) {
        if (targetSubPage == "PATCH_UPDATE") {
            currentSubPage = SettingsSubPage.PATCH_UPDATE
            viewModel.targetSettingsSubPage.value = null
        }
    }

    // Intercept back button to navigate back to Settings Main Menu
    BackHandler(enabled = currentSubPage != SettingsSubPage.MAIN) {
        currentSubPage = SettingsSubPage.MAIN
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBackgroundDark)
    ) {
        AnimatedContent(
            targetState = currentSubPage,
            transitionSpec = {
                if (targetState != SettingsSubPage.MAIN) {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width / 2 } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut()
                    )
                }
            },
            label = "SettingsNavigationTransition"
        ) { targetPage ->
            when (targetPage) {
                SettingsSubPage.MAIN -> {
                    MainSettingsMenuView(
                        viewModel = viewModel,
                        onNavigateTo = { page -> currentSubPage = page },
                        onLogout = onLogout
                    )
                }
                SettingsSubPage.NOTIFICATION_SETTINGS -> {
                    NotificationSettingsSubScreen(
                        viewModel = viewModel,
                        onBack = { currentSubPage = SettingsSubPage.MAIN }
                    )
                }
                SettingsSubPage.GOOGLE_SHEETS_LINE -> {
                    GoogleSheetsAndLineSettingsSubScreen(
                        viewModel = viewModel,
                        onBack = { currentSubPage = SettingsSubPage.MAIN }
                    )
                }
                SettingsSubPage.LINE_MESSAGING_API -> {
                    LineChatBotSettingsSubScreen(
                        viewModel = viewModel,
                        onBack = { currentSubPage = SettingsSubPage.MAIN }
                    )
                }
                SettingsSubPage.SYNC_MAINTENANCE -> {
                    SyncAndMaintenanceSettingsSubScreen(
                        viewModel = viewModel,
                        onBack = { currentSubPage = SettingsSubPage.MAIN }
                    )
                }
                SettingsSubPage.ABOUT_TERMS -> {
                    AboutAndTermsSettingsSubScreen(
                        onBack = { currentSubPage = SettingsSubPage.MAIN },
                        onNavigateToPatch = { currentSubPage = SettingsSubPage.PATCH_UPDATE }
                    )
                }
                SettingsSubPage.ACTIVITY_LOGS -> {
                    ActivityLogsSubScreen(
                        viewModel = viewModel,
                        onBack = { currentSubPage = SettingsSubPage.MAIN }
                    )
                }
                SettingsSubPage.PATCH_UPDATE -> {
                    PatchUpdateSubScreen(
                        viewModel = viewModel,
                        onBack = { currentSubPage = SettingsSubPage.MAIN }
                    )
                }
            }
        }
    }
}

// =========================================================================
// 1. MAIN SETTINGS MENU (List Menu Cards / Navigation Tiles)
// =========================================================================

@Composable
private fun MainSettingsMenuView(
    viewModel: WorkLogViewModel,
    onNavigateTo: (SettingsSubPage) -> Unit,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val encryptedPrefs = remember { com.example.util.EncryptedPrefsManager(context) }
    val techName = encryptedPrefs.getTechnicianName().ifBlank { "ช่างประจำเคส" }
    val techEmail = encryptedPrefs.getTechnicianEmail().ifBlank { "Firebase Authenticated" }
    val techRole = encryptedPrefs.getTechnicianRole()
    val techId = encryptedPrefs.getTechnicianId()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ตั้งค่าระบบ",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                fontFamily = KanitFontFamily
            )
            Text(
                text = "เลือกหมวดหมู่ที่ต้องการตั้งค่าและจัดการข้อมูลระบบ",
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                fontFamily = KanitFontFamily
            )
        }

        // Technician Profile & Account Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(DarkOrangeAccent.copy(alpha = 0.2f))
                                .border(1.dp, DarkOrangeAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = DarkOrangeAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = techName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    fontFamily = KanitFontFamily
                                )
                                if (encryptedPrefs.hasRememberedDeviceTechnician() && encryptedPrefs.getRememberedDeviceTechnicianName() == techName) {
                                    Surface(
                                        color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, Color(0xFF06B6D4))
                                    ) {
                                        Text(
                                            text = "ช่างประจำเครื่อง",
                                            color = Color(0xFF22D3EE),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = KanitFontFamily,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = techEmail,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = KanitFontFamily
                            )
                            if (techRole.isNotBlank()) {
                                Text(
                                    text = "ตำแหน่ง: $techRole",
                                    color = DarkOrangeAccent,
                                    fontSize = 11.sp,
                                    fontFamily = KanitFontFamily
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val techId = viewModel.currentTechnician?.id ?: ""
                            val techName = viewModel.currentTechnician?.name ?: ""
                            viewModel.logActivity(
                                technicianId = techId,
                                technicianName = techName,
                                action = "LOGOUT"
                            )
                            onLogout()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_settings_logout")
                    ) {
                        Text(
                            text = "ออกจากระบบ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = KanitFontFamily
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

        // Menu Card 0: Notification, Sound & Vibration Settings
        SettingsNavigationCard(
            title = "การตั้งค่าระบบแจ้งเตือน เสียง และการสั่น",
            subtitle = "กำหนดเวลาเตือนล่วงหน้าเมื่อใกล้ถึงเคสนัดหมาย, เปิด/ปิดเสียงเตือน, รูปแบบการสั่น และการเตือนสรุปงานประจำวัน",
            icon = Icons.Default.NotificationsActive,
            iconBackgroundColor = Color(0xFFF59E0B).copy(alpha = 0.15f),
            iconTintColor = Color(0xFFF59E0B),
            badgeText = "Sound & Vibration",
            badgeColor = Color(0xFFF59E0B),
            testTag = "menu_notifications",
            onClick = { onNavigateTo(SettingsSubPage.NOTIFICATION_SETTINGS) }
        )

        // Menu Card 2: Sync & Maintenance System
        SettingsNavigationCard(
            title = "ระบบซิงค์ข้อมูลและบำรุงรักษา",
            subtitle = "ส่งออกไฟล์รายงาน Excel/PDF, เคลียร์แคชเคสเก่า, บีบอัดฐานข้อมูล SQLite และจัดการข้อมูล",
            icon = Icons.Default.OfflineBolt,
            iconBackgroundColor = Color(0xFF0284C7).copy(alpha = 0.15f),
            iconTintColor = Color(0xFF38BDF8),
            badgeText = "Export & Maintenance",
            badgeColor = Color(0xFF38BDF8),
            testTag = "menu_sync_maintenance",
            onClick = { onNavigateTo(SettingsSubPage.SYNC_MAINTENANCE) }
        )

        // Menu Card: Software Update & Download Patch
        val availableUpdate by viewModel.availableUpdateInfo.collectAsState()
        SettingsNavigationCard(
            title = "อัปเดตเวอร์ชันและดาวน์โหลด Patch ใหม่",
            subtitle = "ตรวจสอบเวอร์ชันใหม่, โหลดไฟล์ Patch (APK), ติดตั้งและอัปเดตระบบของช่าง",
            icon = Icons.Default.SystemUpdate,
            iconBackgroundColor = if (availableUpdate != null) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF06B6D4).copy(alpha = 0.15f),
            iconTintColor = if (availableUpdate != null) Color(0xFFF87171) else Color(0xFF22D3EE),
            badgeText = if (availableUpdate != null) "⚡ พบ Patch ใหม่ (v${availableUpdate?.versionName})" else "v${com.example.BuildConfig.VERSION_NAME}",
            badgeColor = if (availableUpdate != null) Color(0xFFEF4444) else Color(0xFF22D3EE),
            testTag = "menu_patch_update",
            onClick = { onNavigateTo(SettingsSubPage.PATCH_UPDATE) }
        )

        // Menu Card 3: About App & Terms of Use
        SettingsNavigationCard(
            title = "เกี่ยวกับแอปพลิเคชันและประวัติ",
            subtitle = "ข้อมูลเวอร์ชันแอปพลิเคชัน เงื่อนไขการใช้งาน (Terms of Use) และประวัติการอัปเดตระบบ",
            icon = Icons.Default.Info,
            iconBackgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.15f),
            iconTintColor = Color(0xFFA78BFA),
            badgeText = "v5.0.0 Pro",
            badgeColor = Color(0xFFA78BFA),
            testTag = "menu_about_terms",
            onClick = { onNavigateTo(SettingsSubPage.ABOUT_TERMS) }
        )

        // Menu Card 5: Activity Log
        SettingsNavigationCard(
            title = "ประวัติการใช้งาน (Activity Log)",
            subtitle = "ดูรายการประวัติและกิจกรรมการล็อกอิน ล็อกเอาต์ และประวัติจัดการเคสทั้งหมดของคุณ",
            icon = Icons.Default.Description,
            iconBackgroundColor = Color(0xFF10B981).copy(alpha = 0.15f),
            iconTintColor = Color(0xFF10B981),
            badgeText = "ประวัติกิจกรรม",
            badgeColor = Color(0xFF10B981),
            testTag = "menu_activity_log",
            onClick = { onNavigateTo(SettingsSubPage.ACTIVITY_LOGS) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // System Status summary banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ระบบพร้อมใช้งาน (Offline SQLite Ready)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = KanitFontFamily
                    )
                    Text(
                        text = "บันทึกและประมวลผลงานซ่อมบำรุงในตัวเครื่องได้อย่างปลอดภัย",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = KanitFontFamily
                    )
                }
            }
        }
    }
}

/**
 * Reusable Navigation Tile / Card for Settings Menu
 */
@Composable
private fun SettingsNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTintColor: Color,
    badgeText: String,
    badgeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
        border = BorderStroke(1.dp, Color(0xFF374151))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = KanitFontFamily
                    )
                }

                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = KanitFontFamily,
                    lineHeight = 16.sp
                )

                // Category Badge
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = KanitFontFamily
                    )
                }
            }

            // Trailing Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "เปิดเมนู",
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Top App Bar Header for Sub-Settings pages
 */
@Composable
private fun SubSettingsHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF374151), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "ย้อนกลับ",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                fontFamily = KanitFontFamily
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontFamily = KanitFontFamily
            )
        }
    }
}

// =========================================================================
// 2. SUB-PAGE 1: Google Sheets Webhook Sub-screen
// =========================================================================

@Composable
private fun GoogleSheetsAndLineSettingsSubScreen(
    viewModel: WorkLogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val googleSheetsWebhookUrl by viewModel.googleSheetsWebhookUrl.collectAsState()
    val testConnectionResult by viewModel.testConnectionResult.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()

    var showScriptModal by remember { mutableStateOf(false) }

    val scriptCode = """
function doGet(e) {
  return ContentService.createTextOutput("OK");
}

function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var action = (data.action || data.type || data.Action || data.Type || "").toString().trim().toLowerCase();

    if (action === "test") {
      return ContentService.createTextOutput("Test connection successful");
    }

    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();

    // 1. จัดการการลบข้อมูลทั้งหมด (Delete All / Clear All)
    if (action === "deleteall" || action === "clearall") {
      var lastRow = sheet.getLastRow();
      if (lastRow > 1) {
        sheet.deleteRows(2, lastRow - 1);
      }
      return ContentService.createTextOutput("All logs deleted successfully");
    }

    var caseId = data.CaseID || data.caseId || data.case_id || data.caseNumber || data.caseNo || data.id || "";

    // ป้องกันการประมวลผลหากไม่มีเลขเคส (CaseID)
    if (!caseId || caseId.toString().trim() === "") {
      return ContentService.createTextOutput("Ignored: Missing CaseID");
    }

    var targetCaseId = String(caseId).replace("#", "").trim();
    var targetCaseIdNum = parseInt(targetCaseId, 10);
    var values = sheet.getDataRange().getValues();
    var foundIndex = -1;
    for (var i = 1; i < values.length; i++) {
      var rowCaseId = String(values[i][0]).replace("#", "").trim();
      var rowCaseIdNum = parseInt(rowCaseId, 10);
      if (rowCaseId && (rowCaseId === targetCaseId || (!isNaN(targetCaseIdNum) && !isNaN(rowCaseIdNum) && rowCaseIdNum === targetCaseIdNum))) {
        foundIndex = i + 1; // 1-based row index
        break;
      }
    }

    // 2. จัดการการลบเฉพาะเคสนี้ (Delete Single Case)
    if (action === "delete" || action === "remove" || action === "delete_case") {
      if (foundIndex > 0) {
        sheet.deleteRow(foundIndex);
        return ContentService.createTextOutput("Case deleted successfully");
      } else {
        return ContentService.createTextOutput("Case not found for deletion");
      }
    }

    // 3. สร้างโฟลเดอร์เฉพาะของเคสนี้ใน Google Drive อิงตามเลข ID Case และสร้างโฟลเดอร์ย่อยแยกหมวดหมู่
    var caseFolderName = "Case_" + String(caseId).replace("#", "").trim();
    var caseFolders = DriveApp.getFoldersByName(caseFolderName);
    var caseFolder = caseFolders.hasNext() ? caseFolders.next() : DriveApp.createFolder(caseFolderName);

    // สร้างโฟลเดอร์ย่อยแยกกัน ไม่ปะปนกัน (เปิดเคส/หน้างาน vs ปิดเคส)
    var openSubFolders = caseFolder.getFoldersByName("1_Open_And_Work_Photos");
    var openFolder = openSubFolders.hasNext() ? openSubFolders.next() : caseFolder.createFolder("1_Open_And_Work_Photos");

    var closedSubFolders = caseFolder.getFoldersByName("2_Closed_Case_Photos");
    var closedFolder = closedSubFolders.hasNext() ? closedSubFolders.next() : caseFolder.createFolder("2_Closed_Case_Photos");

    // 4. จัดการการสร้างใหม่ หรือแก้ไข/อัปเดตเคส (Create / Update Case)
    var timestamp = new Date(data.timestamp || Date.now());
    var taskName = data.TaskName || data.taskName || data.category || "";
    var taskDetail = data.TaskDetail || data.taskDetail || data.rawText || "";
    var category = data.Category || data.category || "";
    var status = data.Status || data.status || "Open";
    var solutions = data.Solutions || data.solutions || "";
    var technician = data.Technician || data.technician || "";

    // จัดการรูปภาพเปิดเคส (Save Open Image to Open Folder)
    var imageUrl = data.ImageUrl || data.imageUrl || data.imageUri || "";
    if (data.ImageBase64 || data.imageBase64) {
      imageUrl = saveBase64ToFolder(data.ImageBase64 || data.imageBase64, openFolder, "Case_" + caseId + "_Open.jpg");
    }

    // จัดการรูปภาพขณะทำงาน / รูปหลายรูป (imagesBase64 array)
    if (data.imagesBase64 && Array.isArray(data.imagesBase64)) {
      for (var k = 0; k < data.imagesBase64.length; k++) {
        if (data.imagesBase64[k]) {
          saveBase64ToFolder(data.imagesBase64[k], openFolder, "Case_" + caseId + "_Work_" + (k + 1) + ".jpg");
        }
      }
    }

    // จัดการรูปภาพปิดเคส / สรุปปิดเคส (Save Close Image to Closed Folder)
    var closeImageUrl = data.CloseImageUrl || data.closeImageUrl || data.close_image_url || "";
    if (data.CloseImageBase64 || data.CloseImageBase64) {
      closeImageUrl = saveBase64ToFolder(data.CloseImageBase64 || data.CloseImageBase64, closedFolder, "Case_" + caseId + "_Closed.jpg");
    }

    // ตรวจสอบและสร้างหัวคอลัมน์ J (CloseImageUrl) หากยังไม่มี
    if (sheet.getLastColumn() < 10) {
      sheet.getRange(1, 10).setValue("CloseImageUrl");
    }

    if (foundIndex > 0) {
      // อัปเดตข้อมูลแถวเดิมของเคสนี้
      if (taskName) sheet.getRange(foundIndex, 3).setValue(taskName);
      if (taskDetail) sheet.getRange(foundIndex, 4).setValue(taskDetail);
      if (category) sheet.getRange(foundIndex, 5).setValue(category);
      if (imageUrl) sheet.getRange(foundIndex, 6).setValue(imageUrl);
      sheet.getRange(foundIndex, 7).setValue(status);
      if (solutions !== undefined) sheet.getRange(foundIndex, 8).setValue(solutions);
      if (technician !== undefined) sheet.getRange(foundIndex, 9).setValue(technician);
      if (closeImageUrl) sheet.getRange(foundIndex, 10).setValue(closeImageUrl);
    } else {
      // เพิ่มแถวใหม่
      var sheetCaseId = isNaN(caseId) ? caseId : "'" + caseId;
      sheet.appendRow([sheetCaseId, timestamp, taskName, taskDetail, category, imageUrl, status, solutions, technician, closeImageUrl]);
    }

    return ContentService.createTextOutput("Success");
  } catch (err) {
    return ContentService.createTextOutput("Error: " + err.toString());
  }
}

// ฟังก์ชันบันทึกรูปภาพ Base64 ลงในโฟลเดอร์เคส Google Drive
function saveBase64ToFolder(base64Data, folder, fileName) {
  try {
    var decoded = Utilities.base64Decode(base64Data);
    var blob = Utilities.newBlob(decoded, "image/jpeg", fileName);
    var file = folder.createFile(blob);
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    return file.getUrl();
  } catch (e) {
    return "";
  }
}
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back header
        SubSettingsHeader(
            title = "Google Sheets Webhook",
            subtitle = "ตั้งค่า URL Webhook และคัดลอกโค้ด Apps Script สำหรับซิงค์ข้อมูล",
            onBack = onBack
        )

        HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

        // Section Header: Google Sheets
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = DarkOrangeAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "ตั้งค่า Google Sheets Webhook",
                color = DarkOrangeAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = KanitFontFamily
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "กรอก Webhook URL ของ Google Apps Script เพื่อซิงค์ข้อมูลลงใน Google Sheet อัตโนมัติ",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = KanitFontFamily
                )

                OutlinedTextField(
                    value = googleSheetsWebhookUrl,
                    onValueChange = { viewModel.updateGoogleSheetsWebhookUrl(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sheets_webhook_input"),
                    placeholder = { Text("https://script.google.com/macros/s/...", color = Color(0xFF6B7280)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkOrangeAccent,
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedContainerColor = Color(0xFF111827),
                        unfocusedContainerColor = Color(0xFF111827),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Diagnostic Connection test card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("connection_diagnostic_card"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            isTestingConnection -> DarkOrangeAccent
                            testConnectionResult == null -> Color(0xFF374151)
                            testConnectionResult?.isSuccess == true -> DarkOrangeBorder
                            else -> Color(0xFFEF4444)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "สถานะการเชื่อมต่อ Webhook",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = KanitFontFamily
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isTestingConnection -> DarkOrangeAccent.copy(alpha = 0.2f)
                                            testConnectionResult == null -> Color(0xFF374151)
                                            testConnectionResult?.isSuccess == true -> DarkOrangeAccent.copy(alpha = 0.2f)
                                            else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = when {
                                        isTestingConnection -> "กำลังทดสอบ..."
                                        testConnectionResult == null -> "ยังไม่ได้ทดสอบ"
                                        testConnectionResult?.isSuccess == true -> "เชื่อมต่อสำเร็จ"
                                        else -> "เชื่อมต่อล้มเหลว"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isTestingConnection -> DarkOrangeAccent
                                        testConnectionResult == null -> Color(0xFF94A3B8)
                                        testConnectionResult?.isSuccess == true -> DarkOrangeAccent
                                        else -> Color(0xFFEF4444)
                                    },
                                    fontFamily = KanitFontFamily
                                )
                            }
                        }

                        testConnectionResult?.let { res ->
                            Text(
                                text = res.message,
                                fontSize = 12.sp,
                                color = if (res.isSuccess) DarkOrangeAccent else Color(0xFFEF4444),
                                fontFamily = KanitFontFamily
                            )
                        }

                        Button(
                            onClick = { viewModel.testConnection() },
                            enabled = !isTestingConnection && googleSheetsWebhookUrl.isNotBlank() && !googleSheetsWebhookUrl.contains("placeholder"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("test_connection_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkOrangeAccent,
                                contentColor = Color(0xFF111827),
                                disabledContainerColor = Color(0xFF374151),
                                disabledContentColor = Color(0xFF6B7280)
                            )
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF111827),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color(0xFF111827),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "ทดสอบการเชื่อมต่อ Webhook",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827),
                                fontFamily = KanitFontFamily
                            )
                        }

                        OutlinedButton(
                            onClick = { showScriptModal = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DarkOrangeAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = DarkOrangeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "คัดลอกโค้ด Google Apps Script",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkOrangeAccent,
                                fontFamily = KanitFontFamily
                            )
                        }
                    }
                }
            }
        }
    }

    if (showScriptModal) {
        val clipboardManager = LocalClipboardManager.current
        var copied by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showScriptModal = false },
            title = {
                Text(
                    text = "โค้ด Google Apps Script สมบูรณ์",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontFamily = KanitFontFamily
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "คัดลอกโค้ดนี้ไปวางใน Google Apps Script (Extensions -> Apps Script) เพื่อให้อัปเดตสถานะและรายละเอียดเคสเดิมใน Google Sheet โดยไม่สร้างเคสเปล่าและไม่สร้างแถวซ้ำตอนปิดเคส:",
                        fontSize = 12.sp,
                        color = Color(0xFFD1D5DB),
                        fontFamily = KanitFontFamily
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = scriptCode,
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(scriptCode))
                        copied = true
                        Toast.makeText(context, "คัดลอกโค้ดลงคลิปบอร์ดแล้ว", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkOrangeAccent, contentColor = Color(0xFF111827))
                ) {
                    Text(
                        text = if (copied) "คัดลอกแล้ว!" else "คัดลอกโค้ดทั้งหมด",
                        fontWeight = FontWeight.Bold,
                        fontFamily = KanitFontFamily
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showScriptModal = false }) {
                    Text("ปิด", color = Color.White, fontFamily = KanitFontFamily)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// =========================================================================
// 3. SUB-PAGE 2: Sync & Maintenance Sub-screen
// =========================================================================

@Composable
fun SyncAndMaintenanceSettingsSubScreen(
    viewModel: WorkLogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allLogs by viewModel.allLogs.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Header
        SubSettingsHeader(
            title = "ระบบซิงค์ข้อมูลและบำรุงรักษา",
            subtitle = "ส่งออกไฟล์รายงาน เคลียร์แคช และบำรุงรักษาฐานข้อมูล SQLite",
            onBack = onBack
        )

        HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

        // Section 1: Export Reports
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = DarkOrangeAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "1. ส่งออกไฟล์รายงาน (Export Reports)",
                color = DarkOrangeAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = KanitFontFamily
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ส่งออกข้อมูลเคสงานทั้งหมดที่บันทึกไว้ในแอป",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${allLogs.size} รายการ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontFamily = KanitFontFamily
                        )
                    }
                }

                // Export Excel Button
                Button(
                    onClick = { ExportUtils.exportToCsv(context, allLogs) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("export_excel_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkOrangeAccent,
                        contentColor = Color(0xFF111827)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = Color(0xFF111827),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ส่งออกไฟล์ Excel (.csv)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        fontFamily = KanitFontFamily,
                        fontSize = 13.sp
                    )
                }

                // Export PDF Button
                Button(
                    onClick = { ExportUtils.exportToPdf(context, allLogs) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("export_pdf_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF374151),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ส่งออกไฟล์ PDF (.pdf)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = KanitFontFamily,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Section 2: Database Maintenance
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.OfflineBolt,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "2. การบำรุงรักษาฐานข้อมูล (Database Maintenance)",
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = KanitFontFamily
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "เพิ่มประสิทธิภาพและลดขนาดพื้นที่จัดเก็บของฐานข้อมูล SQLite ภายในเครื่อง",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = KanitFontFamily
                )

                // Prune Old Synced Closed Cases (Cache Clearing)
                Button(
                    onClick = {
                        viewModel.pruneOldLogs(daysToKeep = 30) { count ->
                            Toast.makeText(context, "เคลียร์แคชเคสปิดงานย้อนหลัง 30 วัน เรียบร้อยแล้ว ($count รายการ)", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("prune_cache_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "เคลียร์แคชเคสที่ปิดแล้ว (ย้อนหลัง 30 วัน)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = KanitFontFamily,
                        fontSize = 13.sp
                    )
                }

                // Optimize Database (VACUUM)
                Button(
                    onClick = {
                        viewModel.optimizeDatabase {
                            Toast.makeText(context, "บีบอัดฐานข้อมูล SQLite (VACUUM) สำเร็จ", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("optimize_db_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "บีบอัดฐานข้อมูล SQL (Optimize DB)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = KanitFontFamily,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Section 3: Danger Zone
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "3. ล้างฐานข้อมูลทั้งหมด (Danger Zone)",
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = KanitFontFamily
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ลบรายการประวัติการทำงานทั้งหมดออกจากเครื่อง การกระทำนี้ไม่สามารถย้อนกลับได้",
                    fontSize = 12.sp,
                    color = Color(0xFFFCA5A5),
                    fontFamily = KanitFontFamily
                )

                // Delete All Data Button (Clear Database SQL)
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("clear_monthly_data_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ล้างฐานข้อมูล SQL (Clear Data SQL)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = KanitFontFamily,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deleteConfirmationText = ""
            },
            containerColor = BentoSurfaceDark,
            title = {
                Text(
                    text = "ยืนยันการลบข้อมูลทั้งหมด",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    fontFamily = KanitFontFamily
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "พิมพ์คำว่า \"ยืนยันการลบ\" เพื่อล้างรายการงานทั้งหมดในระบบ",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily
                    )
                    OutlinedTextField(
                        value = deleteConfirmationText,
                        onValueChange = { deleteConfirmationText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_confirm_input"),
                        placeholder = { Text("พิมพ์ ยืนยันการลบ", color = Color(0xFF6B7280)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF374151),
                            focusedContainerColor = Color(0xFF111827),
                            unfocusedContainerColor = Color(0xFF111827),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllLogs {
                            showDeleteDialog = false
                            deleteConfirmationText = ""
                            Toast.makeText(context, "ล้างข้อมูลทั้งหมดเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = deleteConfirmationText == "ยืนยันการลบ",
                    modifier = Modifier.testTag("delete_confirm_submit"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text("ยืนยันลบข้อมูล", fontFamily = KanitFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deleteConfirmationText = ""
                    }
                ) {
                    Text("ยกเลิก", fontFamily = KanitFontFamily, color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

// =========================================================================
// 4. SUB-PAGE 3: About App & Terms Sub-screen
// =========================================================================

@Composable
fun AboutAndTermsSettingsSubScreen(
    onBack: () -> Unit,
    onNavigateToPatch: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Header
        SubSettingsHeader(
            title = "เกี่ยวกับแอปพลิเคชันและประวัติ",
            subtitle = "ข้อมูลเวอร์ชัน ข้อกำหนดการใช้งาน และบันทึกการปรับปรุงระบบ",
            onBack = onBack
        )

        HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

        // Jump to Patch Update Button Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToPatch?.invoke() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ศูนย์อัปเดตและดาวน์โหลด Patch ใหม่",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = KanitFontFamily
                        )
                        Text(
                            text = "ตรวจสอบเวอร์ชันและกดโหลดไฟล์ติดตั้ง Patch ทันที",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = KanitFontFamily
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // App Information Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkOrangeAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = DarkOrangeAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Work Log Assistant Pro",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = KanitFontFamily
                        )
                        Text(
                            text = "Version 5.0.0 (Official Release)",
                            fontSize = 12.sp,
                            color = DarkOrangeAccent,
                            fontFamily = KanitFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = "ระบบบันทึกประวัติการทำงานของช่างและวิศวกรซ่อมบำรุง ยกระดับระบบความปลอดภัยด้วย Firebase Authentication, Scoped Firestore Real-time Sync พร้อมฐานข้อมูลออฟไลน์ SQLite และการเข้ารหัสข้อมูลในเครื่อง (AES-256)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = KanitFontFamily,
                    lineHeight = 18.sp
                )
            }
        }

        // Terms of Use Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFFA78BFA),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "เงื่อนไขการใช้งานและความปลอดภัยข้อมูล (Terms & Security)",
                color = Color(0xFFA78BFA),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = KanitFontFamily
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. การยืนยันตัวตนและความปลอดภัย (Firebase Authentication)\n" +
                           "การเข้าใช้งานระบบของช่างผูกกับบัญชี Email และ Password เฉพาะบุคคลผ่าน Firebase Authentication พร้อมระบบกู้คืนรหัสผ่าน และการแยกระดับสิทธิ์ระหว่างผู้ดูแลระบบ (Admin) กับช่างผู้ปฏิบัติงาน (Technician) อย่างชัดเจน\n\n" +
                           "2. การเข้ารหัสและจัดเก็บข้อมูลส่วนบุคคล (Data Encryption & Jetpack Security)\n" +
                           "ข้อมูลเซสชัน อัตลักษณ์ผู้ใช้งาน และการตั้งค่าในอุปกรณ์ ได้รับการปกป้องด้วยการเข้ารหัสขั้นสูงระดับฮาร์ดแวร์/ซอฟต์แวร์มาตรฐานสากล (Android Jetpack Security / EncryptedSharedPreferences AES-256 GCM) ป้องกันการดึงข้อมูลโดยไม่ได้รับอนุญาต\n\n" +
                           "3. สิทธิ์การเข้าถึงข้อมูลเคส (Scoped Access Control & Firestore Rules)\n" +
                           "ช่างสามารถเข้าถึงและแก้ไขเฉพาะงานและเคสที่ได้รับมอบหมายหรือเป็นผู้สร้าง (Scoped by Technician UID) ผ่านกฎความปลอดภัย Firebase Firestore Rules พร้อมระบบแจ้งเตือนงานและข้อความสั่งการเฉพาะบุคคลแบบ Real-time\n\n" +
                           "4. สถาปัตยกรรมข้อมูลแบบไฮบริด (Offline SQLite & Cloud Sync)\n" +
                           "บันทึกและประมวลผลงานซ่อมบำรุงหน้างานได้อย่างรวดเร็วในเครื่องผ่าน SQLite / Room Database และทำการซิงค์อัปเดตข้อมูลขึ้นคลาวด์ Firebase Firestore และ Google Sheets Webhook อัตโนมัติเมื่อมีสัญญาณอินเทอร์เน็ต\n\n" +
                           "5. การส่งออกและสำรองข้อมูล (Data Export & Backup Policy)\n" +
                           "ช่างสามารถส่งออกไฟล์รายงานในรูปแบบ Excel (.csv) และ PDF (.pdf) เพื่อเก็บเป็นหลักฐานสำรองข้อมูลการปฏิบัติงานส่วนบุคคลได้ตลอดเวลา",
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB),
                    fontFamily = KanitFontFamily,
                    lineHeight = 18.sp
                )
            }
        }

        // Changelog / Version History Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "ประวัติการอัปเดต (Version History / Log Change)",
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = KanitFontFamily
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // v5.0.0 (LATEST)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkOrangeAccent)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("LATEST", color = Color(0xFF111827), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Version 5.0.0",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkOrangeAccent,
                            fontFamily = KanitFontFamily
                        )
                    }
                    Text(
                        text = "• ยกระดับระบบความปลอดภัยเต็มรูปแบบด้วย Firebase Authentication (Email & Password) ยกเลิกการใช้ Anonymous Auth\n" +
                               "• เพิ่มโหมดสลับการเข้าสู่ระบบแบบเลือกชื่อช่าง หรือกรอกอีเมลเข้าสู่ระบบโดยตรง (Direct Email Login) พร้อมระบบกู้คืนรหัสผ่าน\n" +
                               "• ปรับปรุงการจัดเก็บและดึงข้อมูล Firestore ให้ Scoped อิงตาม Firebase Auth UID ของช่างโดยตรง เพื่อความปลอดภัยและสอดคล้องกับ Firestore Rules\n" +
                               "• เพิ่มระบบการแจ้งเตือนงานและกล่องข้อความส่วนบุคคล (Technician Notifications & Alert) รองรับข้อความสั่งการและเคสใหม่แบบ Real-time\n" +
                               "• การเข้ารหัสความปลอดภัยข้อมูลเซสชันและข้อมูลระบุตัวตนในตัวเครื่องด้วย Android Jetpack EncryptedSharedPreferences (AES-256 GCM)\n" +
                               "• ปรับโครงสร้างเมนูและการตั้งค่าให้กระชับ รวดเร็ว และเป็นระเบียบมากยิ่งขึ้น",
                        fontSize = 11.sp,
                        color = Color(0xFFD1D5DB),
                        fontFamily = KanitFontFamily,
                        lineHeight = 17.sp
                    )
                }

                HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

                // v4.1.0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Version 4.1.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily
                    )
                    Text(
                        text = "• เพิ่มระบบสร้างโฟลเดอร์แยกตามเลข ID Case (เช่น Case_xxxx) ใน Google Drive อัตโนมัติเมื่อทำการปิดเคส\n" +
                               "• รองรับการอัปโหลดรูปภาพทั้งหมดขณะปฏิบัติงานและรูปสรุปปิดเคสเข้าสู่โฟลเดอร์เคสใน Google Drive ได้อย่างครบถ้วน\n" +
                               "• ปรับปรุงรูปแบบข้อความแจ้งเตือนปิดเคสใน LINE Chat Bot ให้แสดงเลขที่เคส (# Case ID) และฟอร์มข้อความตามต้นแบบอย่างแม่นยำ พร้อมแนบรูปภาพปิดเคสอัตโนมัติ\n" +
                               "• ปรับปรุง UI และจัดระเบียบกล่องข้อความ/ปุ่มกดทั่วทั้งแอป ป้องกันปัญหาตัวอักษรตกบรรทัด",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily,
                        lineHeight = 17.sp
                    )
                }

                HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

                // v4.0.0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Version 4.0.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily
                    )
                    Text(
                        text = "• ปรับปรุงหน้าจอการตั้งค่าใหม่ทั้งหมดแบบแยกหมวดหมู่และกดแยกหน้า (Sub-pages Navigation)\n" +
                               "• ปรับปรุงการสแกน S/N ละเอียดมากขึ้น ป้องกันการสแกนเร็วเกินไป ปรับแต่ง UI ให้เลื่อนดูรายการที่สแกนสำเร็จได้สะดวกยิ่งขึ้น\n" +
                               "• เพิ่มขอบเขต/แถบเล็งแนวโปร่งใสบนหน้าจอกล้องสแกน เพื่อช่วยช่างเล็งระบุตำแหน่ง S/N ได้ถูกต้องและแม่นยำยิ่งขึ้น\n" +
                               "• เพิ่มระบบจัดการลายน้ำรูปภาพ ช่างสามารถกรอกเพิ่มลายน้ำข้อความสั้น หรือลบลายน้ำเก่าออกได้อย่างสมบูรณ์แบบ\n" +
                               "• ปรับปรุงการเซฟสรุปรายงานภาพปิดเคส (Case Summary Bitmap) ให้กระชับ สวยงาม รายละเอียดครบถ้วนในภาพแผ่นเดียว\n" +
                               "• ปรับเปลี่ยนฟอนต์แอปเป็นแบบ Kanit ทั่วทั้งแอปเพื่อความสวยงามและทันสมัย",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily,
                        lineHeight = 17.sp
                    )
                }

                HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

                // v3.1.0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Version 3.1.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily
                    )
                    Text(
                        text = "• เพิ่มการจัดการล้างข้อมูลเก่า เคลียร์ประวัติ 30 วันล่วงหน้า บำรุงรักษาฐานข้อมูล SQLite ผ่านฟังก์ชันบีบอัด VACUUM\n" +
                               "• ปรับปรุงระบบแจ้งเตือน LINE Notify และ LINE Chat Bot ให้ทำงานร่วมกับ Apps Script ได้เสถียรยิ่งขึ้น",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily,
                        lineHeight = 17.sp
                    )
                }

                HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

                // v3.0.0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Version 3.0.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily
                    )
                    Text(
                        text = "• เพิ่ม Google Sheets Webhook เพื่ออัปเดตและสำรองรายการเข้าคลาวด์อัตโนมัติ\n" +
                               "• ปรับปรุงระบบส่งออกไฟล์รายงานเป็น Excel (.csv) และ PDF (.pdf)",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily,
                        lineHeight = 17.sp
                    )
                }

                HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

                // v2.0.0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Version 2.0.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily
                    )
                    Text(
                        text = "• เพิ่มระบบจัดการหมวดหมู่งาน, ระบบค้นหา และตัวกรองสถานะงานเปิด/ปิดเคส\n" +
                               "• รองรับการแนบรูปภาพและบันทึกข้อมูลแบบออฟไลน์ด้วย Room Database",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily,
                        lineHeight = 17.sp
                    )
                }

                HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

                // v1.0.0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Version 1.0.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily
                    )
                    Text(
                        text = "• เปิดตัวแอปพลิเคชัน MyWorkLog พื้นฐานสำหรับการบันทึกงานซ่อมบำรุงและจัดการรายการซ่อมในเครื่อง",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontFamily = KanitFontFamily,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Credits Card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "เครดิตและลิขสิทธิ์ (Credits)",
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = KanitFontFamily
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MyWorkLog By Benzz & AI Studio",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = KanitFontFamily
                )
                Text(
                    text = "Professional Field Work Logging & Cloud Sync Assistant",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = KanitFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "© 2026 All Rights Reserved.",
                    fontSize = 11.sp,
                    color = DarkOrangeAccent,
                    fontFamily = KanitFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// =========================================================================
// 5. LINE CHAT BOT (MESSAGING API) SETTINGS SUB-SCREEN
// =========================================================================

@Composable
fun LineChatBotSettingsSubScreen(
    viewModel: WorkLogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val savedToken by viewModel.lineBotChannelToken.collectAsState()
    val savedTargetId by viewModel.lineBotTargetId.collectAsState()
    val autoNotifyNewCase by viewModel.lineBotAutoNotifyNewCase.collectAsState()
    val autoNotifyCloseCase by viewModel.lineBotAutoNotifyCloseCase.collectAsState()
    val isTesting by viewModel.isTestingLineBot.collectAsState()
    val testResult by viewModel.testLineBotResult.collectAsState()
    val diagnosticReport by viewModel.lineBotDiagnosticReport.collectAsState()

    var inputToken by remember(savedToken) { mutableStateOf(savedToken) }
    var inputTargetId by remember(savedTargetId) { mutableStateOf(savedTargetId) }
    var isTokenVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sub-page Header with Back Navigation
        SubSettingsHeader(
            title = "LINE Chat Bot (Messaging API)",
            subtitle = "ส่งการแจ้งเตือนสรุปปิดเคสพร้อมแนบรูปภาพเข้า LINE Bot อัตโนมัติ",
            onBack = onBack
        )

        // Card 1: Bot Credentials & Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF06C755).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFF06C755),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "ตั้งค่า Channel Access Token & Target ID",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = KanitFontFamily
                    )
                }

                Text(
                    text = "กรอก Channel Access Token (Long-lived) และ User ID หรือ Group ID เพื่อให้ระบบสามารถยิง Push Message แจ้งเตือนเข้าสู่แชท LINE ได้ทันที",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = KanitFontFamily,
                    lineHeight = 16.sp
                )

                // Channel Access Token Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Channel Access Token (Long-lived)",
                        color = Color(0xFFD1D5DB),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = KanitFontFamily
                    )
                    OutlinedTextField(
                        value = inputToken,
                        onValueChange = { 
                            inputToken = it
                            viewModel.updateLineBotChannelToken(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("line_bot_token_input"),
                        placeholder = {
                            Text(
                                "วาง Channel Access Token จาก LINE Developers...",
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp,
                                fontFamily = KanitFontFamily
                            )
                        },
                        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                    Icon(
                                        imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "สลับการมองเห็น Token",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (inputToken.isNotBlank()) {
                                    IconButton(onClick = { 
                                        inputToken = ""
                                        viewModel.updateLineBotChannelToken("")
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "ล้าง Token",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF06C755),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    inputToken = clip.trim()
                                    viewModel.updateLineBotChannelToken(inputToken)
                                    Toast.makeText(context, "วาง Token จาก Clipboard และบันทึกแล้ว", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "ไม่มีข้อความใน Clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text(
                                "📋 วางจาก Clipboard",
                                color = Color(0xFF06C755),
                                fontSize = 12.sp,
                                fontFamily = KanitFontFamily
                            )
                        }
                    }
                }

                // Target ID Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Target ID (User ID / Group ID / Room ID)",
                        color = Color(0xFFD1D5DB),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = KanitFontFamily
                    )
                    OutlinedTextField(
                        value = inputTargetId,
                        onValueChange = { 
                            inputTargetId = it 
                            viewModel.updateLineBotTargetId(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("line_bot_target_id_input"),
                        placeholder = {
                            Text(
                                "เช่น U12345678... หรือ C12345678...",
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp,
                                fontFamily = KanitFontFamily
                            )
                        },
                        trailingIcon = {
                            if (inputTargetId.isNotBlank()) {
                                IconButton(onClick = { 
                                    inputTargetId = ""
                                    viewModel.updateLineBotTargetId("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "ล้าง Target ID",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF06C755),
                            unfocusedBorderColor = Color(0xFF4B5563),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Text(
                        text = "• User ID: ดูได้จากแท็บ Basic Settings ใน LINE Developers Console (ขึ้นต้นด้วย U)\n• Group ID: รับผ่าน Webhook Event เมื่อเชิญบอทเข้ากลุ่ม (ขึ้นต้นด้วย C)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = KanitFontFamily,
                        lineHeight = 15.sp
                    )
                }

                // Save button
                Button(
                    onClick = {
                        viewModel.updateLineBotChannelToken(inputToken)
                        viewModel.updateLineBotTargetId(inputTargetId)
                        Toast.makeText(context, "บันทึกการตั้งค่า LINE Bot เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_line_bot_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C755)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "💾 บันทึกการตั้งค่า LINE Bot",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = KanitFontFamily,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Card 2: Notification Options (Switches)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "ตัวเลือกการส่งแจ้งเตือนอัตโนมัติ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = KanitFontFamily
                    )
                }

                // Switch 1: Auto notify new case
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔔 แจ้งเตือนเมื่อเปิดเคสใหม่",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = KanitFontFamily
                        )
                        Text(
                            text = "ส่งข้อมูลสรุป เลขเคส หมวดหมู่ และรายละเอียดทันทีที่กดบันทึกเคสใหม่",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = KanitFontFamily
                        )
                    }
                    Switch(
                        checked = autoNotifyNewCase,
                        onCheckedChange = { viewModel.updateLineBotAutoNotifyNewCase(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF06C755),
                            uncheckedThumbColor = Color(0xFF9CA3AF),
                            uncheckedTrackColor = Color(0xFF374151)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

                // Switch 2: Auto notify close case
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "✅ แจ้งเตือนเมื่อปิดเคสเสร็จสิ้น",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = KanitFontFamily
                        )
                        Text(
                            text = "ส่งรายงานสรุปการปิดเคส วิธีแก้ไข S/N เก่า/ใหม่ ชื่อช่าง พร้อมรูปภาพหลักฐานที่แนบไว้เข้า LINE Bot",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = KanitFontFamily
                        )
                    }
                    Switch(
                        checked = autoNotifyCloseCase,
                        onCheckedChange = { viewModel.updateLineBotAutoNotifyCloseCase(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF06C755),
                            uncheckedThumbColor = Color(0xFF9CA3AF),
                            uncheckedTrackColor = Color(0xFF374151)
                        )
                    )
                }
            }
        }

        // Card 3: LINE Diagnostic Dashboard & Send Test Payload
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF06C755).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF06C755),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "LINE Diagnostic Dashboard",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = KanitFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Live Status Badge
                    val badgeBg = when {
                        isTesting -> Color(0xFF1E3A8A)
                        diagnosticReport == null -> Color(0xFF374151)
                        diagnosticReport!!.isOverallSuccess -> Color(0xFF065F46)
                        else -> Color(0xFF991B1B)
                    }
                    val badgeText = when {
                        isTesting -> "กำลังตรวจสอบ..."
                        diagnosticReport == null -> "ยังไม่ได้วินิจฉัย"
                        diagnosticReport!!.isOverallSuccess -> "🟢 เชื่อมต่อพร้อม"
                        else -> "🔴 ขัดข้อง"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = KanitFontFamily,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    text = "แดชบอร์ดวินิจฉัยสถานะเรียลไทม์ ตรวจสอบ Handshake, DNS, โควตา และส่ง Test Payload เพื่อทดสอบการรับส่งข้อความกับ LINE Messaging API",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = KanitFontFamily,
                    lineHeight = 16.sp
                )

                // Send Test Payload & Run Diagnostics Button
                Button(
                    onClick = {
                        viewModel.updateLineBotChannelToken(inputToken)
                        viewModel.updateLineBotTargetId(inputTargetId)
                        viewModel.testLineBotConnection()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("send_test_payload_btn"),
                    enabled = !isTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06C755)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("กำลังรันการวินิจฉัยและส่ง...", fontFamily = KanitFontFamily, fontSize = 12.sp, color = Color.White, maxLines = 1)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🚀 Send Test Payload & วินิจฉัย", fontFamily = KanitFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Bot Profile & Quota Info Box if diagnostic succeeded or has profile
                diagnosticReport?.botProfile?.let { profile ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937)),
                        border = BorderStroke(1.dp, Color(0xFF374151))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🤖 LINE Bot: ${profile.displayName}",
                                    color = Color(0xFF86EFAC),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = KanitFontFamily
                                )
                                Text(
                                    text = "Latency: ${diagnosticReport?.totalLatencyMs ?: 0} ms",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontFamily = KanitFontFamily
                                )
                            }
                            Text(
                                text = "• Basic ID: ${profile.basicId} | User ID: ${profile.userId.take(10)}...",
                                color = Color(0xFFD1D5DB),
                                fontSize = 11.sp,
                                fontFamily = KanitFontFamily
                            )
                            diagnosticReport?.quota?.let { quota ->
                                Text(
                                    text = "• โควตาข้อความ: ${if (quota.type == "limited") "ใช้ไป ${quota.totalUsage} / ${quota.value}" else "ไม่จำกัด (Unlimited), ใช้ไป ${quota.totalUsage}"}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontFamily = KanitFontFamily
                                )
                            }
                        }
                    }
                }

                // Diagnostic Steps breakdown
                diagnosticReport?.steps?.let { steps ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "📋 รายละเอียดการตรวจสอบทีละขั้นตอน (${steps.count { it.status == com.example.util.LineMessagingApiClient.StepStatus.SUCCESS }}/${steps.size} ผ่าน):",
                            color = Color(0xFFD1D5DB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = KanitFontFamily
                        )

                        steps.forEach { step ->
                            val stepColor = when (step.status) {
                                com.example.util.LineMessagingApiClient.StepStatus.SUCCESS -> Color(0xFF22C55E)
                                com.example.util.LineMessagingApiClient.StepStatus.FAILED -> Color(0xFFEF4444)
                                com.example.util.LineMessagingApiClient.StepStatus.WARNING -> Color(0xFFF59E0B)
                                com.example.util.LineMessagingApiClient.StepStatus.SKIPPED -> Color(0xFF6B7280)
                            }
                            val stepIcon = when (step.status) {
                                com.example.util.LineMessagingApiClient.StepStatus.SUCCESS -> Icons.Default.CheckCircle
                                com.example.util.LineMessagingApiClient.StepStatus.FAILED -> Icons.Default.Error
                                com.example.util.LineMessagingApiClient.StepStatus.WARNING -> Icons.Default.Error
                                com.example.util.LineMessagingApiClient.StepStatus.SKIPPED -> Icons.Default.Info
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = stepIcon,
                                    contentDescription = null,
                                    tint = stepColor,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Step ${step.stepNumber}: ${step.name}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = KanitFontFamily
                                    )
                                    Text(
                                        text = step.summary,
                                        color = stepColor,
                                        fontSize = 11.sp,
                                        fontFamily = KanitFontFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // Recommendations if any
                diagnosticReport?.recommendations?.takeIf { it.isNotEmpty() }?.let { recs ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF450a0a)),
                        border = BorderStroke(1.dp, Color(0xFFdc2626))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💡 คำแนะนำในการแก้ไขปัญหา:",
                                color = Color(0xFFfca5a5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = KanitFontFamily
                            )
                            recs.forEach { rec ->
                                Text(
                                    text = "• $rec",
                                    color = Color(0xFFfecaca),
                                    fontSize = 11.sp,
                                    fontFamily = KanitFontFamily,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Legacy testResult fallback banner if no diagnosticReport
                if (diagnosticReport == null) {
                    testResult?.let { result ->
                        val isSuccess = result.isSuccess
                        val containerBg = if (isSuccess) Color(0xFF052e16) else Color(0xFF450a0a)
                        val borderColor = if (isSuccess) Color(0xFF16a34a) else Color(0xFFdc2626)
                        val textColor = if (isSuccess) Color(0xFF86efac) else Color(0xFFfca5a5)
                        val iconVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = containerBg),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (isSuccess) "ผลการทดสอบ: สำเร็จ (HTTP ${result.statusCode})" else "ผลการทดสอบ: ไม่สำเร็จ",
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = KanitFontFamily
                                    )
                                    Text(
                                        text = result.message,
                                        color = textColor.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontFamily = KanitFontFamily,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 4: LINE Developers Step-by-Step Setup Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "คู่มือการตั้งค่า LINE Developers Console",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = KanitFontFamily
                    )
                }

                val guideSteps = listOf(
                    "1. เข้าสู่ระบบ https://developers.line.biz ด้วยบัญชี LINE",
                    "2. สร้าง Provider ใหม่ (เช่น WorkLog Assistant) และสร้าง Channel ชนิด Messaging API",
                    "3. ไปที่แท็บ 'Messaging API' เลื่อนลงมาที่หัวข้อ 'Channel access token' แล้วกด 'Issue' เพื่อสร้าง Long-lived Token",
                    "4. คัดลอก Token ที่ได้มาวางในช่อง 'Channel Access Token' ด้านบน",
                    "5. สำหรับ User ID ให้ไปที่แท็บ 'Basic settings' เลื่อนดูที่ 'Your user ID' (ขึ้นต้นด้วย U) หรือหากต้องการส่งเข้ากลุ่ม ให้ดึงบอทเข้ากลุ่มแล้วนำ Group ID มากรอก",
                    "6. สแกน QR Code เพื่อเพิ่มเพื่อนบอทใน LINE หรือเชิญบอทเข้ากลุ่มที่ต้องการรับการแจ้งเตือน"
                )

                guideSteps.forEach { step ->
                    Text(
                        text = step,
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp,
                        fontFamily = KanitFontFamily,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// =========================================================================
// NOTIFICATION & SOUND/VIBRATION SETTINGS SUB-SCREEN
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationSettingsSubScreen(
    viewModel: WorkLogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val caseReminderEnabled by viewModel.caseReminderEnabled.collectAsState()
    val reminderLeadTimeMinutes by viewModel.reminderLeadTimeMinutes.collectAsState()
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsState()
    val dailyReminderHour by viewModel.dailyReminderHour.collectAsState()
    val dailyReminderMinute by viewModel.dailyReminderMinute.collectAsState()
    val backupReminderEnabled by viewModel.backupReminderEnabled.collectAsState()

    val soundEnabled by viewModel.notificationSoundEnabled.collectAsState()
    val soundType by viewModel.notificationSoundType.collectAsState()
    val vibrationEnabled by viewModel.notificationVibrationEnabled.collectAsState()
    val vibrationPattern by viewModel.notificationVibrationPattern.collectAsState()

    var showTimePickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SubSettingsHeader(
            title = "ตั้งค่าระบบแจ้งเตือน เสียง และการสั่น",
            subtitle = "จัดการการสั่น เสียงเตือน และเวลาเตือนล่วงหน้าเมื่อถึงเคสนัดหมาย",
            onBack = onBack
        )

        HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

        // CARD 1: หมวดหมู่การแจ้งเตือนหลัก (Notification Categories & Master Switches)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "หมวดหมู่การแจ้งเตือน (Notification Categories)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = KanitFontFamily
                    )
                }

                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                // Item 1: Case Reminders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "📌 แจ้งเตือนเคสนัดหมายเข้าบริการ",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            fontFamily = KanitFontFamily
                        )
                        Text(
                            text = "ส่งสัญญาณเตือนก่อนถึงเวลานัดหมายเข้า Onsite หรือเปิดเคสบริการ",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = KanitFontFamily
                        )
                    }
                    Switch(
                        checked = caseReminderEnabled,
                        onCheckedChange = { viewModel.updateCaseReminderEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFF59E0B),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // Item 2: Daily Log Reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "⏰ แจ้งเตือนบันทึกงานประจำวัน",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            fontFamily = KanitFontFamily
                        )
                        Text(
                            text = "เตือนให้สรุปและลงบันทึกภารกิจก่อนจบวัน เวลาปัจจุบัน: ${String.format("%02d:%02d น.", dailyReminderHour, dailyReminderMinute)}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = KanitFontFamily
                        )
                    }
                    Switch(
                        checked = dailyReminderEnabled,
                        onCheckedChange = { viewModel.updateDailyReminderEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFF59E0B),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }

                if (dailyReminderEnabled) {
                    OutlinedButton(
                        onClick = { showTimePickerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF59E0B).copy(alpha = 0.08f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "เปลี่ยนเวลาเตือนบันทึกงาน (${String.format("%02d:%02d น.", dailyReminderHour, dailyReminderMinute)})",
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            fontFamily = KanitFontFamily
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // Item 3: Monthly Backup Reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "💾 เตือนสำรองข้อมูลประจำเดือน",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            fontFamily = KanitFontFamily
                        )
                        Text(
                            text = "แจ้งเตือนให้ส่งออกไฟล์รายงาน Excel/PDF ในวันสุดท้ายของเดือน",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = KanitFontFamily
                        )
                    }
                    Switch(
                        checked = backupReminderEnabled,
                        onCheckedChange = { viewModel.updateBackupReminderEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFF59E0B),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }
            }
        }

        // CARD 2: ระยะเวลาเตือนล่วงหน้าเมื่อเคสจะถึงนัดหมาย (Case Reminder Lead Time)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ระยะเวลาเตือนล่วงหน้า (Lead Time)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = KanitFontFamily
                        )
                        Text(
                            text = "เลือกเวลาที่ต้องการให้ระบบแจ้งเตือนล่วงหน้าก่อนถึงเวลานัดหมาย",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = KanitFontFamily
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                val leadTimeOptions = listOf(
                    0 to "⏱️ เมื่อถึงเวลานัดพอดี (0 นาที)",
                    15 to "⏱️ 15 นาทีก่อนเวลานัด",
                    30 to "⏱️ 30 นาทีก่อนเวลานัด (แนะนำ)",
                    60 to "⏱️ 1 ชั่วโมงก่อนเวลานัด",
                    120 to "⏱️ 2 ชั่วโมงก่อนเวลานัด"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    leadTimeOptions.forEach { (minutes, label) ->
                        val isSelected = reminderLeadTimeMinutes == minutes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else Color(0xFF1E293B)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.updateReminderLeadTime(minutes) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFFFCD34D) else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    fontFamily = KanitFontFamily
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // CARD 3: ตั้งค่าเสียงแจ้งเตือน (Sound Settings)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF22C55E).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ตั้งค่าเสียงแจ้งเตือน",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = KanitFontFamily
                            )
                            Text(
                                text = "เปิด/ปิดเสียง และเลือกระดับสัญญาณเสียงเตือน",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = KanitFontFamily
                            )
                        }
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.updateNotificationSoundEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF22C55E),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }

                if (soundEnabled) {
                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                    val soundOptions = listOf(
                        "default" to "🎵 เสียงเตือนมาตรฐานระบบ (System)",
                        "urgent" to "🚨 เสียงเตือนด่วน/ฉุกเฉิน (High Priority Alarm)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        soundOptions.forEach { (typeKey, label) ->
                            val isSelected = soundType == typeKey
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFF22C55E).copy(alpha = 0.15f)
                                        else Color(0xFF1E293B)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF22C55E) else Color(0xFF334155),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.updateNotificationSoundType(typeKey) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFF86EFAC) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        fontFamily = KanitFontFamily,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CARD 4: ตั้งค่ารูปแบบการสั่นเตือน (Vibration Settings)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFA855F7).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ตั้งค่าการสั่นเตือน",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = KanitFontFamily
                            )
                            Text(
                                text = "เปิด/ปิดการสั่น และเลือกจังหวะการสั่นเตือน",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = KanitFontFamily
                            )
                        }
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { viewModel.updateNotificationVibrationEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFA855F7),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }

                if (vibrationEnabled) {
                    HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

                    val vibrationOptions = listOf(
                        "default" to "📳 สั่นปกติ (Standard - 300ms)",
                        "long" to "📳 สั่นยาวเตือนความจำ (Long Pulse - 1000ms)",
                        "double" to "📳 สั่นจังหวะคู่ (Double Pulse)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vibrationOptions.forEach { (patternKey, label) ->
                            val isSelected = vibrationPattern == patternKey
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFFA855F7).copy(alpha = 0.15f)
                                        else Color(0xFF1E293B)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFA855F7) else Color(0xFF334155),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.updateNotificationVibrationPattern(patternKey) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFFE9D5FF) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        fontFamily = KanitFontFamily,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFFA855F7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CARD 5: ทดสอบส่งการแจ้งเตือนทันที (Instant Live Test)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "ทดสอบสัญญาณแจ้งเตือน (Instant Live Test)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = KanitFontFamily
                    )
                }

                Text(
                    text = "กดปุ่มด้านล่างเพื่อส่งการแจ้งเตือนจำลองเข้ามือถือทันที เพื่อทดสอบการได้ยินเสียงและการสั่นตามที่ตั้งค่าไว้",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = KanitFontFamily
                )

                Button(
                    onClick = {
                        viewModel.sendTestNotificationWithSettings()
                        Toast.makeText(context, "🔔 ส่งสัญญาณแจ้งเตือนทดสอบเรียบร้อยแล้ว!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔔 ทดสอบส่งการแจ้งเตือนทันที (Test Sound & Vibration)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = KanitFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // TimePicker Dialog for Daily Reminder
    if (showTimePickerDialog) {
        val calendar = Calendar.getInstance()
        val currentHour = if (dailyReminderHour >= 0) dailyReminderHour else calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = if (dailyReminderMinute >= 0) dailyReminderMinute else calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                viewModel.updateDailyReminderTime(selectedHour, selectedMinute)
                showTimePickerDialog = false
                Toast.makeText(
                    context,
                    "บันทึกเวลาเตือนเป็น ${String.format("%02d:%02d น.", selectedHour, selectedMinute)} เรียบร้อย",
                    Toast.LENGTH_SHORT
                ).show()
            },
            currentHour,
            currentMinute,
            true
        )

        DisposableEffect(Unit) {
            timePickerDialog.show()
            onDispose {
                if (timePickerDialog.isShowing) {
                    timePickerDialog.dismiss()
                }
            }
        }
    }
}

@Composable
fun ActivityLogsSubScreen(
    viewModel: WorkLogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val encryptedPrefs = remember { com.example.util.EncryptedPrefsManager(context) }
    val techId = encryptedPrefs.getSavedTechnicianId() ?: encryptedPrefs.getTechnicianId()
    
    val activityLogs by viewModel.activityLogsState.collectAsState()

    // Trigger load when screen becomes visible
    LaunchedEffect(techId) {
        if (techId.isNotBlank()) {
            viewModel.loadMyActivityLogs(techId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SubSettingsHeader(
            title = "ประวัติการใช้งาน (Activity Log)",
            subtitle = "ตรวจสอบประวัติและกิจกรรมที่เกิดขึ้นบนอุปกรณ์ของท่าน",
            onBack = onBack
        )

        HorizontalDivider(color = Color(0xFF1F2937), thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "กิจกรรมล่าสุด (สูงสุด 50 รายการ)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = KanitFontFamily
            )
            IconButton(
                onClick = {
                    if (techId.isNotBlank()) {
                        viewModel.loadMyActivityLogs(techId)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "รีเฟรช",
                    tint = DarkOrangeAccent
                )
            }
        }

        if (activityLogs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "ไม่พบประวัติกิจกรรมการใช้งานในช่วงนี้",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontFamily = KanitFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                activityLogs.forEach { log ->
                    val actionLabel = when (log.action) {
                        "LOGIN" -> "เข้าสู่ระบบ (Login)"
                        "LOGOUT" -> "ออกจากระบบ (Logout)"
                        "OPEN_CASE" -> "เปิดเคสงานซ่อม (Open)"
                        "CLOSE_CASE" -> "ปิดเคสเสร็จสิ้น (Closed)"
                        else -> log.action
                    }

                    val actionBgColor = when (log.action) {
                        "LOGIN" -> Color(0xFF10B981).copy(alpha = 0.15f)
                        "LOGOUT" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        "OPEN_CASE" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                        "CLOSE_CASE" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        else -> Color(0xFF4B5563).copy(alpha = 0.15f)
                    }

                    val actionTextColor = when (log.action) {
                        "LOGIN" -> Color(0xFF34D399)
                        "LOGOUT" -> Color(0xFFF87171)
                        "OPEN_CASE" -> Color(0xFF60A5FA)
                        "CLOSE_CASE" -> Color(0xFFFBBF24)
                        else -> Color(0xFF9CA3AF)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("activity_log_item_${log.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = actionBgColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = actionLabel,
                                        color = actionTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = KanitFontFamily,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                val dateStr = log.timestamp?.toDate()?.let { d ->
                                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.US)
                                    sdf.format(d)
                                } ?: "ไม่ระบุเวลา"
                                Text(
                                    text = dateStr,
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontFamily = KanitFontFamily
                                )
                            }

                            Text(
                                text = "ช่างผู้ทำกิจกรรม: ${log.technicianName}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = KanitFontFamily
                            )

                            if (!log.caseId.isNullOrBlank() || !log.caseTitle.isNullOrBlank()) {
                                Surface(
                                    color = Color(0xFF1F2937),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "เลขที่เคส: ${log.caseId ?: "-"}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            fontFamily = KanitFontFamily
                                        )
                                        Text(
                                            text = "ชื่อลูกค้า: ${log.caseTitle ?: "-"}",
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = KanitFontFamily
                                        )
                                    }
                                }
                            }

                            if (!log.deviceInfo.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "บันทึกจากอุปกรณ์: ${log.deviceInfo}",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        fontFamily = KanitFontFamily
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

// =========================================================================
// 8. SUB-PAGE 7: In-App Version Update & Download Patch Sub-screen
// =========================================================================

@Composable
fun PatchUpdateSubScreen(
    viewModel: WorkLogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val availableUpdate by viewModel.availableUpdateInfo.collectAsState()
    val isChecking by viewModel.isCheckingUpdate.collectAsState()
    val isDownloading by viewModel.isDownloadingPatch.collectAsState()
    val statusMessage by viewModel.updateCheckStatusMessage.collectAsState()
    val lastCheckTime by viewModel.lastUpdateCheckTime.collectAsState()
    val customUrl by viewModel.customPatchUrl.collectAsState()
    var editUrlText by remember(customUrl) { mutableStateOf(customUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoBackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SubSettingsHeader(
            title = "อัปเดตเวอร์ชันและดาวน์โหลด Patch",
            subtitle = "ตรวจสอบเวอร์ชันใหม่, โหลดไฟล์ Patch (APK), ติดตั้งอัปเดตระบบของช่าง",
            onBack = onBack
        )

        // 1. Current Version Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "เวอร์ชันของระบบที่ติดตั้งอยู่",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = KanitFontFamily
                        )
                        Text(
                            text = "v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = KanitFontFamily
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF374151), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "สถานะระบบ:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontFamily = KanitFontFamily
                        )
                        Text(
                            text = if (availableUpdate != null) "⚡ มี Patch ใหม่พร้อมให้อัปเดต" else "✔️ เป็นเวอร์ชันล่าสุดแล้ว",
                            color = if (availableUpdate != null) Color(0xFFF97316) else Color(0xFF34D399),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = KanitFontFamily
                        )
                    }
                    if (lastCheckTime > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ตรวจล่าสุดเมื่อ:",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = KanitFontFamily
                            )
                            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastCheckTime))
                            Text(
                                text = dateStr,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontFamily = KanitFontFamily
                            )
                        }
                    }
                }
            }
        }

        // 2. Action Check For Updates & Simulation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.checkForUpdates(manual = true) },
                enabled = !isChecking,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("btn_check_for_updates"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                )
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("กำลังตรวจสอบ...", fontSize = 13.sp, fontFamily = KanitFontFamily)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ตรวจสอบ Patch ใหม่", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = KanitFontFamily)
                }
            }

            OutlinedButton(
                onClick = {
                    viewModel.simulateTestPatch {
                        Toast.makeText(context, "จำลองพบ Patch ใหม่ v5.1.0 เรียบร้อย", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .height(46.dp)
                    .testTag("btn_simulate_patch"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF64748B)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1))
            ) {
                Text("ทดสอบ Patch", fontSize = 12.sp, fontFamily = KanitFontFamily)
            }
        }

        // Status Feedback Box
        if (!statusMessage.isNullOrBlank()) {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF475569)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = statusMessage ?: "",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontFamily = KanitFontFamily
                    )
                }
            }
        }

        // 3. New Patch Card (when update is available or simulated)
        availableUpdate?.let { patch ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.5.dp, Color(0xFFF97316))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF97316).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = Color(0xFFFB923C),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "🚀 พบ Patch เวอร์ชันใหม่!",
                                color = Color(0xFFFB923C),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = KanitFontFamily
                            )
                            Text(
                                text = "เวอร์ชัน ${patch.versionName} (Build #${patch.versionCode})",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = KanitFontFamily
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ขนาดไฟล์: ${patch.patchSize.ifBlank { "18.4 MB" }}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = KanitFontFamily
                            )
                            Text(
                                text = "วันที่: ${patch.releaseDate.ifBlank { "ล่าสุด" }}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontFamily = KanitFontFamily
                            )
                        }
                    }

                    Text(
                        text = "📋 รายการปรับปรุงในเวอร์ชันนี้ (Changelog):",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = KanitFontFamily
                    )

                    Surface(
                        color = Color(0xFF090D16),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = patch.changelog,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = KanitFontFamily,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Big Download Button
                    Button(
                        onClick = {
                            viewModel.downloadAndInstallPatch(patch.apkUrl)
                        },
                        enabled = !isDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_download_patch_now"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF97316),
                            contentColor = Color.White
                        )
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "กำลังดาวน์โหลดไฟล์ Patch...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = KanitFontFamily
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "📥 กดดาวน์โหลดและติดตั้ง Patch ใหม่ (APK)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = KanitFontFamily
                            )
                        }
                    }

                    // Fallback Direct Browser Link
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(patch.apkUrl)).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "เปิดเบราว์เซอร์ไม่สำเร็จ: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                    ) {
                        Text(
                            text = "เปิดดาวน์โหลดผ่านเบราว์เซอร์สำรอง (Direct Download)",
                            fontSize = 11.sp,
                            fontFamily = KanitFontFamily
                        )
                    }
                }
            }
        }

        // 4. Server & Custom URL Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF374151))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "กำหนดเซิร์ฟเวอร์อัปเดต (Update Server Source)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = KanitFontFamily
                    )
                }

                Text(
                    text = "ระบบตรวจจับอัตโนมัติจาก Firestore (`app_updates/latest`) หรือหากต้องการระบุ URL ตรงสำหรับดาวน์โหลดไฟล์ version.json สามารถระบุด้านล่างนี้:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = KanitFontFamily
                )

                OutlinedTextField(
                    value = editUrlText,
                    onValueChange = { editUrlText = it },
                    label = { Text("Custom Patch URL (JSON หรือ APK Direct Link)", fontSize = 11.sp) },
                    placeholder = { Text("https://your-domain.com/version.json", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE2E8F0),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            viewModel.saveCustomPatchUrl(editUrlText)
                            Toast.makeText(context, "บันทึก URL แหล่ง Patch เรียบร้อย", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("บันทึก URL", fontSize = 12.sp, fontFamily = KanitFontFamily)
                    }
                }
            }
        }
    }
}
