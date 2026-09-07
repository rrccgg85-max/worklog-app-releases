package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Settings
import com.example.ui.screens.CreateLogScreen
import com.example.ui.screens.LogListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SummaryScreen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.WorkLogViewModel
import com.example.ui.screens.CreateLogScreen
import com.example.ui.screens.LogListScreen
import com.example.ui.screens.SummaryScreen
import com.example.ui.theme.CustomBgCyan
import com.example.ui.theme.CustomOceanBlue
import com.example.ui.theme.CustomOceanBlueDark
import com.example.ui.theme.DeepNavyText
import com.example.ui.theme.WorkLogTheme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext

import androidx.activity.SystemBarStyle

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

import com.example.data.FirestoreCaseRepository
import com.example.data.Technician
import com.example.ui.screens.TechnicianAuthScreen
import com.example.util.AppVersionInfo
import com.example.util.EncryptedPrefsManager
import com.example.util.UpdateManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {

    private val viewModel: WorkLogViewModel by viewModels {
        WorkLogViewModel.Factory(application)
    }

    private val repository by lazy { FirestoreCaseRepository(applicationContext) }
    private val encryptedPrefs by lazy { EncryptedPrefsManager(this) }
    private val updateManager by lazy { UpdateManager(this) }

    private val initialTabState = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase & App Check safely with try-catch
        try {
            FirebaseApp.initializeApp(this)
            val appCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                // ใช้ Debug Provider สำหรับ development และเครื่องที่ยังไม่ได้ลงทะเบียน SHA-256
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                // ใช้ Play Integrity สำหรับ production
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AppCheck", "Init failed: ${e.message}")
        }

        // Safe startup and edge-to-edge layout configuration
        super.onCreate(savedInstanceState)
        
        handleWidgetIntent(intent)

        // Remove fullscreen flags and ensure fitsSystemWindows is set
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
            var themeMode by remember {
                mutableStateOf(prefs.getString("app_theme_mode", "dark") ?: "dark")
            }

            var isLoggedIn by remember { mutableStateOf(repository.isUserLoggedIn || encryptedPrefs.isLoggedIn()) }
            val availableUpdate by viewModel.availableUpdateInfo.collectAsState()
            var showUpdateDialog by remember { mutableStateOf(false) }

            // Observe availableUpdate to prompt dialog
            LaunchedEffect(availableUpdate) {
                if (availableUpdate != null) {
                    showUpdateDialog = true
                }
            }

            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            WorkLogTheme(darkTheme = isDarkTheme) {
                val currentTechnician = remember(isLoggedIn) {
                    com.example.data.Technician(
                        id = encryptedPrefs.getTechnicianId(),
                        name = encryptedPrefs.getTechnicianName(),
                        pin = encryptedPrefs.getTechnicianPin(),
                        role = encryptedPrefs.getTechnicianRole().ifBlank { "ช่างบริการภาคสนาม" }
                    )
                }

                LaunchedEffect(isLoggedIn, currentTechnician) {
                    if (isLoggedIn && currentTechnician.name.isNotBlank()) {
                        viewModel.setCurrentTechnician(currentTechnician)
                    }
                }

                if (!isLoggedIn) {
                    TechnicianAuthScreen(
                        repository = repository,
                        encryptedPrefs = encryptedPrefs,
                        viewModel = viewModel,
                        onAuthSuccess = { tech ->
                            viewModel.setCurrentTechnician(tech)
                            isLoggedIn = true
                        }
                    )
                } else {
                    MainAppScreen(
                        viewModel = viewModel,
                        currentTechnician = currentTechnician,
                        repository = repository,
                        encryptedPrefs = encryptedPrefs,
                        themeMode = themeMode,
                        initialTab = initialTabState.intValue,
                        onThemeModeChange = { newMode ->
                            themeMode = newMode
                            prefs.edit().putString("app_theme_mode", newMode).apply()
                        },
                        onLogout = {
                            val techId = encryptedPrefs.getSavedTechnicianId() ?: ""
                            val techName = encryptedPrefs.getSavedTechnicianName() ?: ""
                            viewModel.logActivity(
                                technicianId = techId,
                                technicianName = techName,
                                action = "LOGOUT"
                            )
                            repository.signOut()
                            encryptedPrefs.clearSession()
                            isLoggedIn = false
                        }
                    )
                }

                // In-App Update Alert Dialog
                if (showUpdateDialog && availableUpdate != null) {
                    val info = availableUpdate!!
                    AlertDialog(
                        onDismissRequest = { showUpdateDialog = false },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NewReleases,
                                    contentDescription = null,
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "พบแอปเวอร์ชันใหม่ (${info.versionName})",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.example.ui.theme.PromptFontFamily
                                )
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "มีการปล่อย Patch อัปเดตใหม่สำหรับช่างบริการ",
                                    fontSize = 13.sp,
                                    fontFamily = com.example.ui.theme.PromptFontFamily
                                )
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "📋 รายการปรับปรุงใน Patch นี้:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8),
                                            fontFamily = com.example.ui.theme.PromptFontFamily
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = info.changelog,
                                            fontSize = 12.sp,
                                            color = Color(0xFFE2E8F0),
                                            lineHeight = 17.sp,
                                            fontFamily = com.example.ui.theme.PromptFontFamily
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.downloadAndInstallPatch(info.apkUrl)
                                    showUpdateDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF97316),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "📥 ดาวน์โหลดและติดตั้ง Patch",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.example.ui.theme.PromptFontFamily
                                )
                            }
                        },
                        dismissButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    showUpdateDialog = false
                                    initialTabState.intValue = 3
                                    viewModel.targetSettingsSubPage.value = "PATCH_UPDATE"
                                }) {
                                    Text(
                                        text = "เปิดเมนู Patch",
                                        color = Color(0xFF38BDF8),
                                        fontFamily = com.example.ui.theme.PromptFontFamily
                                    )
                                }
                                TextButton(onClick = { showUpdateDialog = false }) {
                                    Text(
                                        text = "ไว้ทีหลัง",
                                        color = Color.Gray,
                                        fontFamily = com.example.ui.theme.PromptFontFamily
                                    )
                                }
                            }
                        },
                        containerColor = com.example.ui.theme.SciFiSurfaceDark,
                        titleContentColor = Color.White,
                        textContentColor = Color.White
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: android.content.Intent?) {
        if (intent == null) return
        if (intent.action == "com.example.ACTION_ADD_WORK" || intent.hasExtra("EXTRA_TARGET_TAB")) {
            val tab = intent.getIntExtra("EXTRA_TARGET_TAB", 0)
            initialTabState.intValue = tab
        } else if (intent.getBooleanExtra("navigate_to_create", false)) {
            initialTabState.intValue = 0
        } else if (intent.getBooleanExtra("navigate_to_settings", false)) {
            initialTabState.intValue = 3
        }
        val subPage = intent.getStringExtra("EXTRA_SUB_PAGE")
        if (subPage != null) {
            initialTabState.intValue = 3
            viewModel.targetSettingsSubPage.value = subPage
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: WorkLogViewModel,
    currentTechnician: com.example.data.Technician? = null,
    repository: FirestoreCaseRepository? = null,
    encryptedPrefs: EncryptedPrefsManager? = null,
    themeMode: String = "dark",
    initialTab: Int = 0,
    onThemeModeChange: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val navPrefs = remember { context.getSharedPreferences("app_nav_prefs", Context.MODE_PRIVATE) }
    val savedTab = remember { navPrefs.getInt("selected_tab", initialTab) }

    val activeTech = remember(currentTechnician) {
        currentTechnician ?: com.example.data.Technician(
            id = encryptedPrefs?.getTechnicianId() ?: "",
            name = encryptedPrefs?.getTechnicianName() ?: "ช่างประจำเคส",
            pin = encryptedPrefs?.getTechnicianPin() ?: "",
            role = encryptedPrefs?.getTechnicianRole() ?: "ช่างบริการภาคสนาม"
        )
    }

    var selectedTab by remember { mutableIntStateOf(if (initialTab != 0) initialTab else savedTab) }

    androidx.compose.runtime.LaunchedEffect(initialTab) {
        if (initialTab != 0) {
            selectedTab = initialTab
        }
    }

    androidx.compose.runtime.LaunchedEffect(selectedTab) {
        navPrefs.edit().putInt("selected_tab", selectedTab).apply()
    }

    val syncState by viewModel.syncState.collectAsState()
    val googleSheetsWebhookUrl by viewModel.googleSheetsWebhookUrl.collectAsState()
    val availableUpdate by viewModel.availableUpdateInfo.collectAsState()
    val showBanner by viewModel.showUpdateAlertBanner.collectAsState()

    val isGoogleSheetsConnected = remember(syncState, googleSheetsWebhookUrl) {
        val isConfigured = googleSheetsWebhookUrl.isNotBlank() &&
                !googleSheetsWebhookUrl.contains("placeholder") &&
                googleSheetsWebhookUrl.startsWith("http")
        isConfigured && syncState != com.example.ui.SyncState.ERROR
    }

    var showOcrScannerDialog by remember { mutableStateOf(false) }

    val navItems = remember {
        listOf(
            NavItem(
                title = "ลงงาน",
                selectedIcon = Icons.Default.AddCircle,
                unselectedIcon = Icons.Outlined.AddCircleOutline,
                testTag = "tab_create_log"
            ),
            NavItem(
                title = "รายการงาน",
                selectedIcon = Icons.Default.FormatListBulleted,
                unselectedIcon = Icons.Outlined.FormatListBulleted,
                testTag = "tab_log_list"
            ),
            NavItem(
                title = "สรุปรายงาน",
                selectedIcon = Icons.Default.Assessment,
                unselectedIcon = Icons.Outlined.Assessment,
                testTag = "tab_summary"
            ),
            NavItem(
                title = "ตั้งค่า",
                selectedIcon = Icons.Default.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "tab_settings"
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = com.example.ui.theme.BentoBackgroundDark,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Header: WORKLOG.SYS • ONLINE / OFFLINE (Sci-Fi Cyber Header ref)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.theme.SciFiBackgroundDark)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Glowing Cyan Dot + WORKLOG.SYS & Logged in Technician Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(com.example.ui.theme.SciFiCyanGlow)
                        )
                        Column {
                            Text(
                                text = "WORKLOG.SYS",
                                color = com.example.ui.theme.SciFiCyanAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = com.example.ui.theme.PromptFontFamily,
                                letterSpacing = 1.2.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onLogout() }
                            ) {
                                Text(
                                    text = "👤 ${activeTech.name}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontFamily = com.example.ui.theme.PromptFontFamily
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(ออกจากระบบ)",
                                    color = com.example.ui.theme.SciFiOrangeAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.example.ui.theme.PromptFontFamily
                                )
                            }
                        }
                    }

                    // Right: Actions (Patch Alert Button + interactive OCR Text Scanner Button)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (availableUpdate != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                    .border(BorderStroke(1.dp, Color(0xFFEF4444)), RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedTab = 3
                                        viewModel.targetSettingsSubPage.value = "PATCH_UPDATE"
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("btn_header_patch_update")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Patch ใหม่",
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Patch v${availableUpdate?.versionName ?: ""}",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.example.ui.theme.PromptFontFamily
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.example.ui.theme.SciFiCyanGlow.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, com.example.ui.theme.SciFiCyanAccent.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                .clickable { showOcrScannerDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("btn_header_ocr_scanner")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = "สแกนดึงข้อความ",
                                tint = com.example.ui.theme.SciFiCyanAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "สแกนดึงข้อความ",
                                color = com.example.ui.theme.SciFiCyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = com.example.ui.theme.PromptFontFamily,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Dark Cyber Navigation Bar with Active Cyan Pill Indicator (Ref Design)
            NavigationBar(
                containerColor = Color(0xFF080E17),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF162332))
            ) {
                val navColors = NavigationBarItemDefaults.colors(
                    indicatorColor = com.example.ui.theme.SciFiCyanGlow,
                    selectedIconColor = Color(0xFF080E17),
                    selectedTextColor = com.example.ui.theme.SciFiCyanAccent,
                    unselectedIconColor = Color(0xFF64748B),
                    unselectedTextColor = Color(0xFF64748B)
                )

                // Tab 0: "ลงงาน"
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "ลงงาน",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "ลงงาน",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            fontFamily = com.example.ui.theme.PromptFontFamily
                        )
                    },
                    colors = navColors,
                    modifier = Modifier.testTag("tab_create_log")
                )

                // Tab 1: "รายการ"
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "รายการ",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "รายการ",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            fontFamily = com.example.ui.theme.PromptFontFamily
                        )
                    },
                    colors = navColors,
                    modifier = Modifier.testTag("tab_log_list")
                )

                // Tab 2: "รายงาน"
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "รายงาน",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "รายงาน",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            fontFamily = com.example.ui.theme.PromptFontFamily
                        )
                    },
                    colors = navColors,
                    modifier = Modifier.testTag("tab_summary")
                )

                // Tab 3: "ตั้งค่า"
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        if (availableUpdate != null) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        contentColor = Color.White
                                    ) {
                                        Text("NEW", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "ตั้งค่า",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "ตั้งค่า",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "ตั้งค่า",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            fontFamily = com.example.ui.theme.PromptFontFamily
                        )
                    },
                    colors = navColors,
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top In-App Patch Alert Banner
            if (availableUpdate != null && showBanner) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NewReleases,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "⚡ มีการอัปเดต Patch ใหม่: v${availableUpdate?.versionName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = com.example.ui.theme.PromptFontFamily
                                )
                                Text(
                                    text = "แตะเพื่อเปิดเมนูดาวน์โหลดและติดตั้ง Patch",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = com.example.ui.theme.PromptFontFamily
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    selectedTab = 3
                                    viewModel.targetSettingsSubPage.value = "PATCH_UPDATE"
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color(0xFF0F172A)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = "โหลด Patch",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.example.ui.theme.PromptFontFamily
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissUpdateBanner() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "ปิด",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        0 -> CreateLogScreen(
                            viewModel = viewModel,
                            currentTechnician = activeTech,
                            onLogCreated = {
                                selectedTab = 1
                            }
                        )
                        1 -> LogListScreen(
                            viewModel = viewModel,
                            onNavigateToCreate = {
                                selectedTab = 0
                            }
                        )
                        2 -> SummaryScreen(
                            viewModel = viewModel
                        )
                        3 -> SettingsScreen(
                            viewModel = viewModel,
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            onLogout = onLogout
                        )
                    }
                }

                if (showOcrScannerDialog) {
                    com.example.ui.components.OcrTextScannerDialog(
                        viewModel = viewModel,
                        onDismiss = { showOcrScannerDialog = false }
                    )
                }
            }
        }
    }
}

private data class NavItem(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)
