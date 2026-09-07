package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.data.FirestoreCaseRepository
import com.example.data.FirestoreCaseRepository.TechnicianProfile
import com.example.data.Technician
import com.example.ui.theme.PromptFontFamily
import com.example.ui.theme.SarabunFontFamily
import com.example.util.EncryptedPrefsManager
import com.example.ui.WorkLogViewModel

/**
 * Minimal & Clean Technician Authentication Screen.
 * Minimalist dark design: clean typography, elegant monochrome & subtle slate accents.
 */
@Composable
fun TechnicianAuthScreen(
    repository: FirestoreCaseRepository,
    encryptedPrefs: EncryptedPrefsManager,
    viewModel: WorkLogViewModel,
    onAuthSuccess: (Technician) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Remembered device technician details
    val hasDeviceAssigned = remember { encryptedPrefs.hasRememberedDeviceTechnician() }
    val rememberedName = remember { encryptedPrefs.getRememberedDeviceTechnicianName() }
    val rememberedEmail = remember { encryptedPrefs.getRememberedDeviceTechnicianEmail() }
    val rememberedRole = remember { encryptedPrefs.getRememberedDeviceTechnicianRole() }

    // Roster of technicians loaded from Firestore Web Dashboard
    var techniciansList by remember { mutableStateOf<List<TechnicianProfile>>(emptyList()) }
    var isFetchingRoster by remember { mutableStateOf(true) }

    // Selected technician state
    var selectedTechnician by remember {
        mutableStateOf<TechnicianProfile?>(
            if (hasDeviceAssigned && rememberedName.isNotBlank()) {
                TechnicianProfile(
                    id = rememberedEmail.ifBlank { "dev_tech" },
                    name = rememberedName,
                    email = rememberedEmail,
                    role = if (rememberedRole == "ช่างบริการภาคสนาม") "IT Support Onsite" else rememberedRole
                )
            } else null
        )
    }

    // Modal/Dialog for selecting another technician
    var showTechnicianPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Direct Email Login Mode
    var isDirectEmailMode by remember { mutableStateOf(false) }
    var directEmailInput by remember { mutableStateOf(rememberedEmail) }

    // Password / Credential input
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val passwordFocusRequester = remember { FocusRequester() }

    // "จำว่าเครื่องนี้ใช้ชื่อช่างนี้เสมอ" Checkbox (default true)
    var rememberOnThisDevice by remember { mutableStateOf(true) }

    // Loading & Feedback
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }

    // Auto-focus the password/credential field immediately when technician is selected or dialog closed
    LaunchedEffect(selectedTechnician, showTechnicianPicker, isDirectEmailMode) {
        if (!showTechnicianPicker && (selectedTechnician != null || isDirectEmailMode)) {
            delay(200)
            try {
                passwordFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus requester safe fallback
            }
        }
    }

    // Function to perform authentication immediately
    val performAuthSubmit: () -> Unit = {
        focusManager.clearFocus()
        if (isDirectEmailMode) {
            val email = directEmailInput.trim()
            if (email.isBlank()) {
                errorMessage = "กรุณากรอกอีเมลสำหรับเข้าสู่ระบบ"
            } else if (passwordInput.isBlank()) {
                errorMessage = "กรุณากรอกรหัสผ่าน"
            } else if (!isAuthenticating) {
                isAuthenticating = true
                errorMessage = null
                repository.authenticateWithEmailDirect(
                    emailInput = email,
                    passwordInput = passwordInput,
                    rememberOnDevice = rememberOnThisDevice,
                    onSuccess = { authenticatedTech ->
                        isAuthenticating = false
                        viewModel.setCurrentTechnician(authenticatedTech)
                        viewModel.startRealtimeCasesListener(authenticatedTech.id)
                        viewModel.logActivity(
                            technicianId = authenticatedTech.id,
                            technicianName = authenticatedTech.name,
                            action = "LOGIN"
                        )
                        Toast.makeText(
                            context,
                            "เข้าสู่ระบบ: ${authenticatedTech.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                        onAuthSuccess(authenticatedTech)
                    },
                    onError = { error ->
                        isAuthenticating = false
                        errorMessage = error
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        } else {
            val tech = selectedTechnician
            if (tech == null) {
                errorMessage = "กรุณาเลือกชื่อช่างก่อนเข้าสู่ระบบ หรือสลับไปกรอกอีเมล"
            } else if (tech.email.isBlank()) {
                errorMessage = "ไม่พบอีเมลของช่าง ${tech.name} กรุณาติดต่อผู้ดูแลระบบ"
            } else if (passwordInput.isBlank()) {
                errorMessage = "กรุณากรอกรหัสผ่าน"
            } else if (!isAuthenticating) {
                isAuthenticating = true
                errorMessage = null
                repository.authenticateTechnician(
                    tech = tech,
                    passOrPin = passwordInput,
                    rememberOnDevice = rememberOnThisDevice,
                    onSuccess = { authenticatedTech ->
                        isAuthenticating = false
                        viewModel.setCurrentTechnician(authenticatedTech)
                        viewModel.startRealtimeCasesListener(authenticatedTech.id)
                        viewModel.logActivity(
                            technicianId = authenticatedTech.id,
                            technicianName = authenticatedTech.name,
                            action = "LOGIN"
                        )
                        Toast.makeText(
                            context,
                            "เข้าสู่ระบบ: ${authenticatedTech.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                        onAuthSuccess(authenticatedTech)
                    },
                    onError = { error ->
                        isAuthenticating = false
                        errorMessage = error
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    // Fetch technicians list from Firestore in Realtime (gracefully fallback if permission is restricted)
    DisposableEffect(Unit) {
        isFetchingRoster = true
        errorMessage = null
        val fs = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        try {
            listenerRegistration = fs.collection("technicians")
                .addSnapshotListener { snapshot, error ->
                    isFetchingRoster = false
                    if (error == null && snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            val name = (doc.getString("name") 
                                ?: doc.getString("displayName") 
                                ?: doc.getString("fullName") 
                                ?: doc.getString("technicianName") 
                                ?: "").trim()
                            val email = (doc.getString("email") ?: doc.getString("mail") ?: "").trim()
                            val rawRole = (doc.getString("role") ?: doc.getString("position") ?: "IT Support Onsite").trim()
                            val role = if (rawRole == "ช่างบริการภาคสนาม") "IT Support Onsite" else rawRole

                            if (name.isNotBlank()) {
                                TechnicianProfile(
                                    id = doc.id,
                                    name = name,
                                    email = email,
                                    role = role,
                                    pin = ""
                                )
                            } else null
                        }
                        if (list.isNotEmpty()) {
                            techniciansList = list
                            if (selectedTechnician == null) {
                                selectedTechnician = list.first()
                            } else {
                                val match = list.find { it.id == selectedTechnician?.id || it.name == selectedTechnician?.name }
                                if (match != null) {
                                    selectedTechnician = match
                                }
                            }
                        }
                    } else if (error != null) {
                        android.util.Log.w("TechnicianAuthScreen", "Note: Firestore technicians collection is restricted or empty (${error.message}). Falling back gracefully.")
                        // If no technician was preselected or in the list, automatically switch to Direct Email Mode
                        if (selectedTechnician == null && techniciansList.isEmpty()) {
                            isDirectEmailMode = true
                        }
                    }
                }
        } catch (e: Exception) {
            isFetchingRoster = false
            android.util.Log.w("TechnicianAuthScreen", "Note: Exception while setting up technicians listener: ${e.message}")
            if (selectedTechnician == null && techniciansList.isEmpty()) {
                isDirectEmailMode = true
            }
        }

        onDispose {
            try {
                listenerRegistration?.remove()
            } catch (e: Exception) {
                // Ignore cleanup error
            }
        }
    }

    // Colors for Ultra Minimal aesthetic
    val bgMain = Color(0xFF0F141C) // Pure dark slate
    val cardBg = Color(0xFF161D27) // Clean card surface
    val surfaceInput = Color(0xFF1E2633) // Muted input field
    val borderMuted = Color(0xFF283243) // Thin clean border
    val textPrimary = Color(0xFFF1F5F9) // Clean white
    val textSecondary = Color(0xFF94A3B8) // Muted slate gray
    val textTertiary = Color(0xFF64748B) // Subtle dark gray
    val accentClean = Color(0xFFE2E8F0) // Clean light accent (minimal monochrome feel)
    val buttonBg = Color(0xFFF8FAFC) // Crisp white minimal button
    val buttonText = Color(0xFF0F172A) // Dark text on button

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgMain)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Minimal Icon Header
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(surfaceInput),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Title
            Text(
                text = "เข้าสู่ระบบช่าง",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = textPrimary,
                fontFamily = SarabunFontFamily
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "เลือกชื่อและกรอกรหัสผ่านเพื่อเริ่มงาน",
                fontSize = 13.sp,
                color = textSecondary,
                fontFamily = SarabunFontFamily
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Error Message Banner (if any)
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1318)),
                        border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                            fontFamily = SarabunFontFamily,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // ==========================================
            // MAIN CARD (Minimalist & Flat)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderMuted)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mode Switcher: Dropdown vs Direct Email
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2633))
                            .padding(3.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    isDirectEmailMode = false
                                    errorMessage = null
                                },
                            color = if (!isDirectEmailMode) Color(0xFF283243) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "เลือกชื่อช่าง",
                                color = if (!isDirectEmailMode) textPrimary else textTertiary,
                                fontSize = 12.5.sp,
                                fontWeight = if (!isDirectEmailMode) FontWeight.SemiBold else FontWeight.Normal,
                                fontFamily = SarabunFontFamily,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    isDirectEmailMode = true
                                    errorMessage = null
                                },
                            color = if (isDirectEmailMode) Color(0xFF283243) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "กรอกอีเมลช่าง",
                                color = if (isDirectEmailMode) textPrimary else textTertiary,
                                fontSize = 12.5.sp,
                                fontWeight = if (isDirectEmailMode) FontWeight.SemiBold else FontWeight.Normal,
                                fontFamily = SarabunFontFamily,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    if (isDirectEmailMode) {
                        // Direct Email Input Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "อีเมลช่าง (Email)",
                                fontSize = 12.5.sp,
                                color = textSecondary,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SarabunFontFamily
                            )

                            OutlinedTextField(
                                value = directEmailInput,
                                onValueChange = {
                                    directEmailInput = it
                                    errorMessage = null
                                },
                                placeholder = {
                                    Text(
                                        text = "เช่น Benz@Worklog.com",
                                        color = textTertiary,
                                        fontSize = 14.sp,
                                        fontFamily = SarabunFontFamily
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = surfaceInput,
                                    unfocusedContainerColor = surfaceInput,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary,
                                    focusedBorderColor = Color(0xFF64748B),
                                    unfocusedBorderColor = borderMuted
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_direct_technician_email")
                            )
                        }
                    } else {
                        // 1. Technician Selector Dropdown
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ชื่อช่าง",
                                fontSize = 12.5.sp,
                                color = textSecondary,
                                fontWeight = FontWeight.Medium,
                                fontFamily = SarabunFontFamily
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        errorMessage = null
                                        showTechnicianPicker = true
                                    }
                                    .testTag("btn_select_technician_picker"),
                                color = surfaceInput,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, borderMuted)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF283243)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedTechnician != null) {
                                            Text(
                                                text = selectedTechnician!!.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = textPrimary,
                                                fontFamily = PromptFontFamily
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = textSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedTechnician?.name ?: "แตะเพื่อเลือกชื่อช่าง",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = if (selectedTechnician != null) textPrimary else textTertiary,
                                            fontFamily = SarabunFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (selectedTechnician != null && selectedTechnician!!.role.isNotBlank()) {
                                            Text(
                                                text = selectedTechnician!!.role,
                                                fontSize = 11.5.sp,
                                                color = textSecondary,
                                                fontFamily = SarabunFontFamily
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select technician",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Password / PIN Input (6-digit numeric)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "รหัสผ่าน (6 หลัก)",
                            fontSize = 12.5.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Medium,
                            fontFamily = SarabunFontFamily
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { input ->
                                val digitsOnly = input.filter { it.isDigit() }.take(6)
                                passwordInput = digitsOnly
                                errorMessage = null
                            },
                            placeholder = {
                                Text(
                                    text = "กรอกรหัสผ่านตัวเลข 6 หลัก",
                                    color = textTertiary,
                                    fontSize = 14.sp,
                                    fontFamily = SarabunFontFamily
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Key,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle visibility",
                                        tint = textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = surfaceInput,
                                unfocusedContainerColor = surfaceInput,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = Color(0xFF64748B),
                                unfocusedBorderColor = borderMuted
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { performAuthSubmit() },
                                onDone = { performAuthSubmit() }
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(passwordFocusRequester)
                                .testTag("input_technician_password")
                        )
                    }

                    // 3. Remember Checkbox & Forgot Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { rememberOnThisDevice = !rememberOnThisDevice }
                        ) {
                            Checkbox(
                                checked = rememberOnThisDevice,
                                onCheckedChange = { rememberOnThisDevice = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentClean,
                                    checkmarkColor = Color(0xFF0F172A),
                                    uncheckedColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "จำชื่อช่างในเครื่องนี้",
                                fontSize = 12.sp,
                                color = if (rememberOnThisDevice) textSecondary else textTertiary,
                                fontFamily = SarabunFontFamily
                            )
                        }

                        val forgotEmail = if (isDirectEmailMode) directEmailInput else (selectedTechnician?.email ?: "")
                        if (forgotEmail.isNotBlank()) {
                            Text(
                                text = "ลืมรหัสผ่าน?",
                                color = textSecondary,
                                fontSize = 12.sp,
                                fontFamily = SarabunFontFamily,
                                modifier = Modifier
                                    .clickable {
                                        resetEmailInput = forgotEmail
                                        showResetDialog = true
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4. Submit Button (Clean high-contrast minimal button)
                    val isSubmitEnabled = !isAuthenticating && if (isDirectEmailMode) {
                        directEmailInput.isNotBlank()
                    } else {
                        selectedTechnician != null
                    }

                    Button(
                        onClick = { performAuthSubmit() },
                        enabled = isSubmitEnabled,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBg,
                            contentColor = buttonText,
                            disabledContainerColor = Color(0xFF283243),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_technician_login_submit")
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = buttonText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "กำลังตรวจสอบ...",
                                fontFamily = SarabunFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "เข้าสู่ระบบ",
                                fontFamily = SarabunFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    androidx.compose.material3.TextButton(
                        onClick = {
                            Toast.makeText(context, "กรุณาติดต่อผู้ดูแลระบบ (Admin) เพื่อรีเซ็ตรหัส PIN ของคุณ", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("btn_forgot_pin_contact_admin")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ถ้าลืมรหัส Pin ให้ติดต่อ admin",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp,
                            fontFamily = SarabunFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Minimal Footer
            Text(
                text = "ระบบบริหารจัดการงานบริการภาคสนาม",
                color = textTertiary,
                fontSize = 11.5.sp,
                fontFamily = SarabunFontFamily
            )
        }
    }

    // =========================================================================
    // DIALOG: TECHNICIAN SELECTION LIST (Minimal Clean Dialog)
    // =========================================================================
    if (showTechnicianPicker) {
        val filteredList = techniciansList.filter {
            searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.role.contains(searchQuery, ignoreCase = true) ||
            it.email.contains(searchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showTechnicianPicker = false },
            containerColor = cardBg,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "เลือกชื่อช่าง",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        fontFamily = SarabunFontFamily
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isFetchingRoster = true
                                repository.fetchTechniciansRoster { list ->
                                    techniciansList = list
                                    isFetchingRoster = false
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { showTechnicianPicker = false }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Minimal search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ค้นหาชื่อช่าง...", color = textTertiary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = surfaceInput,
                            unfocusedContainerColor = surfaceInput,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = Color(0xFF64748B),
                            unfocusedBorderColor = borderMuted
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isFetchingRoster) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp,
                                color = textPrimary
                            )
                        }
                    } else if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = if (techniciansList.isEmpty()) "ยังไม่มีรายชื่อช่าง" else "ไม่พบรายชื่อช่าง",
                                    color = textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    fontFamily = SarabunFontFamily
                                )
                                Text(
                                    text = if (techniciansList.isEmpty()) "หากติดสิทธิ์ Firestore Rules หรือยังไม่โหลดรายชื่อ สามารถสลับไปกรอกอีเมลเข้าสู่ระบบได้โดยตรง" else "ลองค้นหาด้วยคำอื่น",
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = SarabunFontFamily,
                                    textAlign = TextAlign.Center
                                )
                                if (techniciansList.isEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            isDirectEmailMode = true
                                            showTechnicianPicker = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF283243),
                                            contentColor = textPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "กรอกอีเมลเข้าสู่ระบบโดยตรง",
                                            fontSize = 12.5.sp,
                                            fontFamily = SarabunFontFamily
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredList, key = { it.id.ifBlank { it.name } }) { tech ->
                                val isSelected = selectedTechnician?.name == tech.name

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedTechnician = tech
                                            passwordInput = ""
                                            errorMessage = null
                                            showTechnicianPicker = false
                                        },
                                    color = if (isSelected) Color(0xFF283243) else surfaceInput,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF64748B) else borderMuted
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) textPrimary else Color(0xFF242E3D)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = tech.name.take(1).uppercase(),
                                                color = if (isSelected) buttonText else textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                fontFamily = PromptFontFamily
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tech.name,
                                                color = textPrimary,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.5.sp,
                                                fontFamily = SarabunFontFamily
                                            )
                                            if (tech.role.isNotBlank()) {
                                                Text(
                                                    text = tech.role,
                                                    color = textSecondary,
                                                    fontSize = 11.sp,
                                                    fontFamily = SarabunFontFamily
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = textPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTechnicianPicker = false }) {
                    Text("ปิด", color = textPrimary, fontFamily = SarabunFontFamily)
                }
            }
        )
    }

    // =========================================================================
    // DIALOG: PASSWORD RESET
    // =========================================================================
    if (showResetDialog) {
        var isSendingReset by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = cardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "รีเซ็ตรหัสผ่าน",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    fontFamily = SarabunFontFamily
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ระบบจะส่งลิงก์สำหรับตั้งรหัสผ่านใหม่ไปยังอีเมล:",
                        color = textSecondary,
                        fontSize = 13.sp,
                        fontFamily = SarabunFontFamily
                    )
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text("อีเมล", fontFamily = SarabunFontFamily) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = surfaceInput,
                            unfocusedContainerColor = surfaceInput,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = Color(0xFF64748B),
                            unfocusedBorderColor = borderMuted
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmailInput.isBlank()) {
                            Toast.makeText(context, "กรุณาระบุอีเมล", Toast.LENGTH_SHORT).show()
                            return@Button
                            }
                        isSendingReset = true
                        repository.sendPasswordReset(
                            email = resetEmailInput,
                            onSuccess = {
                                isSendingReset = false
                                showResetDialog = false
                                Toast.makeText(context, "ส่งลิงก์รีเซ็ตรหัสผ่านเรียบร้อยแล้ว", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                isSendingReset = false
                                Toast.makeText(context, "ไม่สามารถส่งอีเมลได้: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isSendingReset,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBg, contentColor = buttonText)
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = buttonText)
                    } else {
                        Text("ส่งลิงก์รีเซ็ต", fontWeight = FontWeight.Bold, fontFamily = SarabunFontFamily)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("ยกเลิก", color = textSecondary, fontFamily = SarabunFontFamily)
                }
            }
        )
    }
}

