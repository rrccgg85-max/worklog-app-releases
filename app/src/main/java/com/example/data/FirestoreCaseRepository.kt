package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.util.EncryptedPrefsManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing Firebase Authentication, Multi-Technician Firestore path (users/{userId}/repair_cases),
 * and Firebase Storage image uploads.
 */
class FirestoreCaseRepository(private val context: Context? = null) {

    private val TAG = "FirestoreRepository"

    val auth: FirebaseAuth? by lazy {
        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth initialization failed: ${e.message}", e)
            null
        }
    }

    val firestore: FirebaseFirestore? by lazy {
        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val instance = FirebaseFirestore.getInstance()
            try {
                // Enable Offline Disk Persistence for offline operations on field sites
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build()
                    )
                    .build()
                instance.firestoreSettings = settings
                Log.d(TAG, "Firestore Offline Persistence Enabled successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Firestore settings already initialized or warning: ${e.message}")
            }
            instance
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Firestore initialization failed: ${e.message}", e)
            null
        }
    }

    val storage: FirebaseStorage? by lazy {
        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Storage initialization failed: ${e.message}", e)
            null
        }
    }

    val currentUserId: String?
        get() = auth?.currentUser?.uid ?: run {
            context?.let { EncryptedPrefsManager(it).getTechnicianId().ifBlank { null } }
        }

    val currentUserEmail: String?
        get() = auth?.currentUser?.email ?: run {
            context?.let { EncryptedPrefsManager(it).getTechnicianEmail().ifBlank { null } }
        }

    val currentUserDisplayName: String?
        get() = auth?.currentUser?.displayName ?: run {
            context?.let { EncryptedPrefsManager(it).getTechnicianName().ifBlank { null } }
        }

    val isUserLoggedIn: Boolean
        get() = auth?.currentUser != null

    fun ensureAuth(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val a = auth
        if (a == null) {
            onFailure(Exception("Firebase Auth not initialized"))
            return
        }
        if (a.currentUser == null && currentUserId == null) {
            onFailure(Exception("กรุณาเข้าสู่ระบบก่อนดำเนินการ"))
        } else {
            onSuccess()
        }
    }

    fun resolveDocumentReference(
        caseId: String,
        onResolved: (com.google.firebase.firestore.DocumentReference) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val fs = firestore
        if (fs == null) {
            onError(Exception("Firestore is not initialized"))
            return
        }
        if (caseId.isBlank()) {
            onResolved(fs.collection("repair_cases").document())
            return
        }
        val trimmed = caseId.trim()
        val directRef = fs.collection("repair_cases").document(trimmed)
        directRef.get()
            .addOnSuccessListener { snap ->
                if (snap != null && snap.exists()) {
                    Log.d(TAG, "resolveDocumentReference: Document found directly with Document ID '$trimmed'")
                    onResolved(directRef)
                } else {
                    // Fallback to query by field 'caseId' for legacy documents
                    fs.collection("repair_cases")
                        .whereEqualTo("caseId", trimmed)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot != null && !querySnapshot.isEmpty) {
                                val docId = querySnapshot.documents.first().id
                                Log.d(TAG, "resolveDocumentReference: Resolved field 'caseId' '$trimmed' to Document ID '$docId'")
                                onResolved(fs.collection("repair_cases").document(docId))
                            } else {
                                Log.d(TAG, "resolveDocumentReference: Document not found with caseId '$trimmed'. Using '$trimmed' as Document ID.")
                                onResolved(directRef)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "resolveDocumentReference: Query failed for caseId '$trimmed', falling back to direct document", e)
                            onResolved(directRef)
                        }
                }
            }
            .addOnFailureListener {
                // If direct get fails, check query fallback
                fs.collection("repair_cases")
                    .whereEqualTo("caseId", trimmed)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot != null && !querySnapshot.isEmpty) {
                            val docId = querySnapshot.documents.first().id
                            onResolved(fs.collection("repair_cases").document(docId))
                        } else {
                            onResolved(directRef)
                        }
                    }
                    .addOnFailureListener { e ->
                        onResolved(directRef)
                    }
            }
    }

    // ==========================================
    // Firebase Authentication: Login, Register, SignOut
    // ==========================================

    /**
     * Sign In existing Technician with Email and Password
     */
    fun signInWithEmail(
        email: String,
        pass: String,
        rememberOnDevice: Boolean = true,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val a = auth
        if (a == null) {
            onError("ระบบยืนยันตัวตน Firebase ยังไม่พร้อมใช้งาน")
            return
        }

        a.signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    val uid = user.uid
                    val emailStr = user.email ?: email
                    val fallbackName = user.displayName ?: emailStr.substringBefore("@")
                    
                    // Fetch profile from Firestore to get full name & role if available
                    firestore?.collection("users")?.document(uid)?.get()
                        ?.addOnSuccessListener { doc ->
                            val docName = doc.getString("displayName")?.ifBlank { null }
                            val docRole = doc.getString("role")?.ifBlank { null } ?: "ช่างบริการภาคสนาม"
                            val finalName = docName ?: fallbackName

                            context?.let { ctx ->
                                EncryptedPrefsManager(ctx).saveTechnicianSession(
                                    id = uid,
                                    name = finalName,
                                    email = emailStr,
                                    role = docRole,
                                    rememberOnDevice = rememberOnDevice
                                )
                            }
                            
                            // Update last login
                            firestore?.collection("users")?.document(uid)?.set(
                                mapOf("lastLogin" to System.currentTimeMillis()),
                                SetOptions.merge()
                            )

                            onSuccess(user)
                        }
                        ?.addOnFailureListener {
                            // Even if Firestore read fails, proceed with fallback Auth info
                            context?.let { ctx ->
                                EncryptedPrefsManager(ctx).saveTechnicianSession(
                                    id = uid,
                                    name = fallbackName,
                                    email = emailStr,
                                    role = "ช่างบริการภาคสนาม",
                                    rememberOnDevice = rememberOnDevice
                                )
                            }
                            onSuccess(user)
                        } ?: run {
                            context?.let { ctx ->
                                EncryptedPrefsManager(ctx).saveTechnicianSession(
                                    id = uid,
                                    name = fallbackName,
                                    email = emailStr,
                                    role = "ช่างบริการภาคสนาม",
                                    rememberOnDevice = rememberOnDevice
                                )
                            }
                            onSuccess(user)
                        }
                } else {
                    onError("ไม่พบข้อมูลผู้ใช้งาน")
                }
            }
            .addOnFailureListener { ex ->
                val errorMsg = mapAuthErrorMessage(ex)
                Log.e(TAG, "Sign in failed: ${ex.message}", ex)
                onError(errorMsg)
            }
    }

    /**
     * Register New Technician with Email, Password, Name, and Role
     */
    fun registerWithEmail(
        email: String,
        pass: String,
        technicianName: String,
        role: String,
        rememberOnDevice: Boolean = true,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val a = auth
        if (a == null) {
            onError("ระบบยืนยันตัวตน Firebase ยังไม่พร้อมใช้งาน")
            return
        }

        a.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    val uid = user.uid
                    val finalName = technicianName.trim().ifBlank { email.substringBefore("@") }
                    val finalRole = role.trim().ifBlank { "ช่างบริการภาคสนาม" }

                    // Set displayName in Firebase Auth Profile
                    val profileUpdates = userProfileChangeRequest {
                        displayName = finalName
                    }
                    user.updateProfile(profileUpdates)

                    // Create User Profile document in Firestore users/{uid}
                    val userProfile = mapOf(
                        "uid" to uid,
                        "email" to email.trim(),
                        "displayName" to finalName,
                        "role" to finalRole,
                        "createdAt" to System.currentTimeMillis(),
                        "lastLogin" to System.currentTimeMillis()
                    )
                    firestore?.collection("users")?.document(uid)?.set(userProfile, SetOptions.merge())

                    // Save local session & remember device
                    context?.let { ctx ->
                        EncryptedPrefsManager(ctx).saveTechnicianSession(
                            id = uid,
                            name = finalName,
                            email = email.trim(),
                            role = finalRole,
                            rememberOnDevice = rememberOnDevice
                        )
                    }

                    onSuccess(user)
                } else {
                    onError("ลงทะเบียนไม่สำเร็จ ไม่พบผู้ใช้")
                }
            }
            .addOnFailureListener { ex ->
                val errorMsg = mapAuthErrorMessage(ex)
                Log.e(TAG, "Register failed: ${ex.message}", ex)
                onError(errorMsg)
            }
    }

    /**
     * Send Password Reset Email
     */
    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val a = auth
        if (a == null) {
            onError("ระบบยืนยันตัวตน Firebase ยังไม่พร้อมใช้งาน")
            return
        }
        if (email.isBlank()) {
            onError("กรุณาระบุอีเมลที่ต้องการรีเซ็ตรหัสผ่าน")
            return
        }

        a.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { ex ->
                val errorMsg = mapAuthErrorMessage(ex)
                onError(errorMsg)
            }
    }

    /**
     * Sign Out
     */
    fun signOut() {
        try {
            auth?.signOut()
            context?.let { EncryptedPrefsManager(it).clearSession() }
            Log.d(TAG, "User signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}", e)
        }
    }

    private fun mapAuthErrorMessage(ex: Exception): String {
        val msg = ex.message?.lowercase() ?: ""
        return when {
            msg.contains("invalid-credential") ||
            msg.contains("invalid_credential") ||
            msg.contains("supplied auth credential") ||
            msg.contains("credential") ||
            msg.contains("wrong-password") ||
            msg.contains("user-not-found") ->
                "รหัสผ่านหรือ PIN ไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง"
            msg.contains("email-already-in-use") || msg.contains("email-already-exists") ->
                "อีเมลนี้ถูกลงทะเบียนใช้งานแล้ว กรุณาเข้าสู่ระบบหรือใช้อีเมลอื่น"
            msg.contains("weak-password") ->
                "รหัสผ่านต้องมีความยาวอย่างน้อย 6 ตัวอักษร"
            msg.contains("invalid-email") ->
                "รูปแบบอีเมลไม่ถูกต้อง กรุณาตรวจสอบอีเมล"
            msg.contains("network") ->
                "เกิดข้อผิดพลาดในการเชื่อมต่อเครือข่าย กรุณาตรวจสอบอินเทอร์เน็ต"
            msg.contains("too-many-requests") ->
                "มีการพยายามเข้าสู่ระบบผิดพลาดบ่อยเกินไป กรุณารอสักครู่แล้วลองใหม่"
            else ->
                "รหัสผ่านหรือ PIN ไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง"
        }
    }

    /**
     * Data class representing a technician profile managed from Web Dashboard / Firestore.
     */
    data class TechnicianProfile(
        val id: String = "",
        val name: String = "",
        val email: String = "",
        val role: String = "IT Support Onsite",
        val pin: String = ""
    )

    /**
     * Fetches the roster of registered technicians directly from Firestore ('technicians' collection).
     */
    fun fetchTechniciansRoster(onResult: (List<TechnicianProfile>) -> Unit) {
        val resultList = mutableListOf<TechnicianProfile>()
        val seenNames = mutableSetOf<String>()

        val fs = firestore
        if (fs == null) {
            onResult(emptyList())
            return
        }

        try {
            fs.collection("technicians").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            val name = (doc.getString("name") 
                                ?: doc.getString("displayName") 
                                ?: doc.getString("fullName") 
                                ?: doc.getString("technicianName") 
                                ?: "").trim()
                            val email = (doc.getString("email") ?: doc.getString("mail") ?: "").trim()
                            val rawRole = (doc.getString("role") ?: doc.getString("position") ?: "IT Support Onsite").trim()
                            val role = if (rawRole == "ช่างบริการภาคสนาม") "IT Support Onsite" else rawRole
                            val pin = (doc.getString("pinCode") ?: doc.getString("pin") ?: doc.getString("passcode") ?: doc.getString("password") ?: "").trim()

                            if (name.isNotEmpty() && !seenNames.contains(name)) {
                                seenNames.add(name)
                                resultList.add(TechnicianProfile(id = doc.id, name = name, role = role, email = email, pin = pin))
                            }
                        }
                    }
                    onResult(resultList)
                }
                .addOnFailureListener {
                    onResult(emptyList())
                }
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }

    /**
     * Authenticates a technician selected from the roster using email and password,
     * or fallback to Firestore PIN verification if Firebase Auth succeeds or fails.
     */
    fun authenticateTechnician(
        tech: TechnicianProfile,
        passOrPin: String,
        rememberOnDevice: Boolean = true,
        onSuccess: (Technician) -> Unit,
        onError: (String) -> Unit
    ) {
        val password = passOrPin.trim()
        if (password.isBlank()) {
            onError("กรุณากรอกรหัสผ่านหรือ PIN เพื่อเข้าใช้งาน")
            return
        }

        val email = tech.email.trim()
        val a = auth ?: FirebaseAuth.getInstance()

        // Handler when auth succeeds (either via Firebase Auth or PIN match)
        fun handleSuccess(finalName: String, finalRole: String, pinCode: String) {
            context?.let { ctx ->
                EncryptedPrefsManager(ctx).saveTechnicianSession(
                    id = tech.id,
                    name = finalName,
                    email = email,
                    role = finalRole,
                    rememberOnDevice = rememberOnDevice
                )
            }

            logActivity(
                technicianId = tech.id,
                technicianName = finalName,
                action = "LOGIN"
            )

            onSuccess(
                Technician(
                    id = tech.id,
                    name = finalName,
                    pin = pinCode,
                    role = finalRole
                )
            )
        }

        // Helper to check PIN stored in Firestore document
        fun checkFirestorePin(onFailed: () -> Unit) {
            val fs = firestore ?: FirebaseFirestore.getInstance()
            fs.collection("technicians").document(tech.id).get()
                .addOnSuccessListener { doc ->
                    val storedPin = (doc?.getString("pinCode")
                        ?: doc?.getString("pin")
                        ?: doc?.getString("passcode")
                        ?: doc?.getString("password")
                        ?: tech.pin).trim()

                    if (doc != null && doc.exists() && storedPin.isNotBlank() && storedPin == password) {
                        val finalName = (doc.getString("name") ?: tech.name).ifBlank { tech.name }
                        val rawRole = (doc.getString("role") ?: tech.role).ifBlank { "IT Support Onsite" }
                        val finalRole = if (rawRole == "ช่างบริการภาคสนาม") "IT Support Onsite" else rawRole
                        handleSuccess(finalName, finalRole, storedPin)
                    } else {
                        onFailed()
                    }
                }
                .addOnFailureListener {
                    onFailed()
                }
        }

        if (email.isNotBlank()) {
            a.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        val finalName = tech.name.ifBlank { user.displayName ?: email.substringBefore("@") }
                        val finalRole = tech.role.ifBlank { "IT Support Onsite" }
                        handleSuccess(finalName, finalRole, "")
                    } else {
                        checkFirestorePin {
                            onError("รหัสผ่านหรือ PIN ไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง")
                        }
                    }
                }
                .addOnFailureListener { ex ->
                    checkFirestorePin {
                        val errorMsg = mapAuthErrorMessage(ex)
                        Log.e(TAG, "signInWithEmailAndPassword failed for ${tech.name}: ${ex.message}")
                        onError(errorMsg)
                    }
                }
        } else {
            checkFirestorePin {
                onError("รหัสผ่านหรือ PIN ไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง")
            }
        }
    }

    /**
     * Authenticates technician directly with email and password, then loads profile from Firestore.
     */
    fun authenticateWithEmailDirect(
        emailInput: String,
        passwordInput: String,
        rememberOnDevice: Boolean = true,
        onSuccess: (Technician) -> Unit,
        onError: (String) -> Unit
    ) {
        val email = emailInput.trim()
        val password = passwordInput.trim()
        if (email.isBlank()) {
            onError("กรุณากรอกอีเมล")
            return
        }
        if (password.isBlank()) {
            onError("กรุณากรอกรหัสผ่าน")
            return
        }

        val a = auth ?: FirebaseAuth.getInstance()
        if (a == null) {
            onError("ระบบยืนยันตัวตน Firebase ยังไม่พร้อมใช้งาน")
            return
        }

        a.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    val fs = firestore ?: FirebaseFirestore.getInstance()
                    
                    // Rule 3: Search technicians collection by email
                    fs.collection("technicians")
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            val doc = querySnapshot?.documents?.firstOrNull { d -> 
                                val docEmail = (d.getString("email") ?: d.getString("mail") ?: "").trim()
                                docEmail.equals(email, ignoreCase = true)
                            }
                            if (doc != null) {
                                val techId = doc.id // This is the Technician Document ID
                                val name = (doc.getString("name") 
                                    ?: doc.getString("displayName") 
                                    ?: doc.getString("fullName") 
                                    ?: user.displayName 
                                    ?: email.substringBefore("@")).trim()
                                val rawRole = (doc.getString("role") ?: doc.getString("position") ?: "IT Support Onsite").trim()
                                val role = if (rawRole == "ช่างบริการภาคสนาม") "IT Support Onsite" else rawRole

                                context?.let { ctx ->
                                    EncryptedPrefsManager(ctx).saveTechnicianSession(
                                        id = techId, // Store Technician Document ID
                                        name = name,
                                        email = email,
                                        role = role,
                                        rememberOnDevice = rememberOnDevice
                                    )
                                }

                                logActivity(
                                    technicianId = techId,
                                    technicianName = name,
                                    action = "LOGIN"
                                )

                                onSuccess(
                                    Technician(
                                        id = techId,
                                        name = name,
                                        pin = "",
                                        role = role
                                    )
                                )
                            } else {
                                // Rule 3: Authentication succeeded but technician document was not found by email.
                                // Do NOT fallback to auth UID. Show clear error and sign out.
                                a.signOut()
                                onError("ไม่พบข้อมูลช่างที่เชื่อมโยงกับบัญชีนี้ กรุณาติดต่อผู้ดูแลระบบ")
                            }
                        }
                        .addOnFailureListener { ex ->
                            a.signOut()
                            Log.e(TAG, "Querying technician by email failed: ${ex.message}")
                            onError("เกิดข้อผิดพลาดในการดึงข้อมูลโปรไฟล์ช่าง: ${ex.localizedMessage ?: ex.message}")
                        }
                } else {
                    onError("ไม่พบข้อมูลผู้ใช้งาน")
                }
            }
            .addOnFailureListener { ex ->
                val errorMsg = mapAuthErrorMessage(ex)
                Log.e(TAG, "authenticateWithEmailDirect failed: ${ex.message}")
                onError(errorMsg)
            }
    }

    private fun showToast(message: String) {
        context?.let { ctx ->
            try {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show Toast: ${e.message}")
            }
        }
    }

    /**
     * Saves or updates a repair case in Firestore root "repair_cases" collection
     */
    fun saveCase(
        case: RepairCase,
        userId: String? = null,
        technicianId: String = "",
        technicianName: String = "",
        createdBy: String = "technician",
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val fs = firestore
        if (fs == null) {
            val ex = Exception("Firestore is not initialized")
            Log.e(TAG, "saveCase failed: Firestore is null")
            showToast("Firebase Error: Firestore ไม่ถูกเปิดใช้งาน")
            onError(ex)
            return
        }

        ensureAuth(
            onSuccess = {
                try {
                    val currentAuthUid = auth?.currentUser?.uid
                    var finalTechId = technicianId.trim().ifBlank {
                        case.technicianId.trim()
                    }
                    if (finalTechId.isBlank() || finalTechId == "unknown_technician") {
                        finalTechId = context?.let { ctx ->
                            val prefs = EncryptedPrefsManager(ctx)
                            prefs.getSavedTechnicianId() ?: prefs.getTechnicianId()
                        } ?: ""
                    }
                    if (finalTechId.isBlank()) {
                        finalTechId = "unknown_technician"
                    }

                    var finalTechName = technicianName.trim().ifBlank {
                        case.technicianName.trim()
                    }
                    if (finalTechName.isBlank() || finalTechName == "ช่างประจำเคส") {
                        finalTechName = context?.let { ctx ->
                            val prefs = EncryptedPrefsManager(ctx)
                            prefs.getSavedTechnicianName() ?: prefs.getTechnicianName()
                        } ?: "ช่างประจำเคส"
                    }

                    resolveDocumentReference(case.caseId, onResolved = { docRef ->
                        val finalCaseId = docRef.id
                        val finalCaseIdField = if (case.caseId.isBlank()) finalCaseId else case.caseId
                        val caseWithId = case.copy(caseId = finalCaseIdField)

                        // Map fields cleanly without undefined/null values
                        val docData = hashMapOf<String, Any>(
                            "caseId" to finalCaseIdField,
                            "customerName" to (caseWithId.customerName.ifBlank { "ลูกค้าทั่วไป" }),
                            "encryptedPhone" to (caseWithId.encryptedPhone ?: ""),
                            "decryptedPhone" to (caseWithId.decryptedPhone ?: ""),
                            "phone" to (caseWithId.decryptedPhone ?: ""),
                            "encryptedAddress" to (caseWithId.encryptedAddress ?: ""),
                            "decryptedAddress" to (caseWithId.decryptedAddress ?: ""),
                            "address" to (caseWithId.decryptedAddress ?: ""),
                            "details" to (caseWithId.details ?: ""),
                            "status" to when (caseWithId.status.trim().uppercase()) {
                                "ปิดเคส", "ซ่อมเสร็จแล้ว", "CLOSED" -> "CLOSED"
                                "เปิดเคส", "รอดำเนินการ", "OPEN" -> "OPEN"
                                else -> caseWithId.status.ifBlank { "OPEN" }
                            },
                            "technicianName" to finalTechName,
                            "assignedTechName" to finalTechName,
                            "techName" to finalTechName,
                            "technicianId" to finalTechId,
                            "assignedTechId" to finalTechId,
                            "techId" to finalTechId,
                            "createdBy" to createdBy,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "timestamp" to if (caseWithId.timestampLong > 0) com.google.firebase.Timestamp(java.util.Date(caseWithId.timestampLong)) else com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "createdTimestamp" to System.currentTimeMillis(),
                            "imageUrl" to (caseWithId.imageUrl ?: ""),
                            "imageUrls" to (caseWithId.imageUrls ?: emptyList<String>()),
                            "checkInImageUrl" to (caseWithId.checkInImageUrl ?: ""),
                            "checkOutImageUrl" to (caseWithId.checkOutImageUrl ?: "")
                        )

                        // 1. Save directly to root collection "repair_cases"
                        docRef.set(docData, SetOptions.merge())
                            .addOnSuccessListener {
                                Log.d(TAG, "Case saved successfully to root 'repair_cases/$finalCaseId'")

                                // 2. Backup to users/{currentAuthUid}/repair_cases/{caseId}
                                // Rule 14: Use Firebase Auth UID for user backup path
                                if (!currentAuthUid.isNullOrBlank()) {
                                    fs.collection("users").document(currentAuthUid).collection("repair_cases").document(finalCaseId)
                                        .set(docData, SetOptions.merge())
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Case backed up to users/$currentAuthUid/repair_cases/$finalCaseId")
                                        }
                                }

                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to save case to root 'repair_cases': ${e.message}", e)
                                showToast("บันทึกเคสลง Firebase ไม่สำเร็จ: ${e.localizedMessage ?: e.message}")
                                onError(e)
                            }
                    }, onError = { e ->
                        Log.e(TAG, "saveCase reference resolution failed: ${e.message}", e)
                        onError(e)
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in saveCase: ${e.message}", e)
                    showToast("Error: ${e.localizedMessage ?: e.message}")
                    onError(e)
                }
            },
            onFailure = { e ->
                Log.e(TAG, "saveCase auth failed: ${e.message}", e)
                onError(e)
            }
        )
    }

    /**
     * Updates specific fields of an existing case document in Firestore root "repair_cases"
     */
    fun updateCaseFields(
        caseId: String,
        fields: Map<String, Any?>,
        userId: String? = null,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val fs = firestore
        if (fs == null) {
            val ex = Exception("Firestore is not initialized")
            showToast("Firebase Error: Firestore ไม่เปิดใช้งาน")
            onError(ex)
            return
        }

        val cleanFields = fields.filterValues { it != null }.mapValues { entry ->
            val k = entry.key
            val v = entry.value!!
            if (k == "status" && v is String) {
                when (v.trim().uppercase()) {
                    "ปิดเคส", "ซ่อมเสร็จแล้ว", "CLOSED" -> "CLOSED"
                    "เปิดเคส", "รอดำเนินการ", "OPEN" -> "OPEN"
                    else -> v
                }
            } else {
                v
            }
        }
        if (cleanFields.isEmpty()) {
            onSuccess()
            return
        }

        ensureAuth(
            onSuccess = {
                try {
                    val uid = userId ?: currentUserId ?: ""
                    val trimmedCaseId = caseId.trim()
                    
                    resolveDocumentReference(
                        caseId = trimmedCaseId,
                        onResolved = { resolvedRef ->
                            val resolvedDocId = resolvedRef.id
                            resolvedRef.set(cleanFields, SetOptions.merge())
                                .addOnSuccessListener {
                                    Log.d(TAG, "Successfully updated resolved document repair_cases/$resolvedDocId with fields: ${cleanFields.keys}")
                                    if (uid.isNotBlank()) {
                                        fs.collection("users").document(uid).collection("repair_cases").document(resolvedDocId)
                                            .set(cleanFields, SetOptions.merge())
                                            .addOnSuccessListener {
                                                Log.d(TAG, "Successfully updated backup users/$uid/repair_cases/$resolvedDocId")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e(TAG, "Failed to update backup users/$uid/repair_cases/$resolvedDocId: ${e.message}")
                                            }
                                    }
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Failed to update resolved document repair_cases/$resolvedDocId: ${e.message}", e)
                                    showToast("อัปเดตข้อมูลเคสไม่สำเร็จ: ${e.localizedMessage ?: e.message}")
                                    onError(e)
                                }
                        },
                        onError = { e ->
                            Log.e(TAG, "Failed to resolve document reference for $trimmedCaseId: ${e.message}", e)
                            onError(e)
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in updateCaseFields: ${e.message}", e)
                    showToast("Error: ${e.localizedMessage ?: e.message}")
                    onError(e)
                }
            },
            onFailure = { e ->
                Log.e(TAG, "updateCaseFields auth failed: ${e.message}", e)
                onError(e)
            }
        )
    }

    /**
     * Updates the status of a case document by executing a lookup query that resolves the
     * correct Firestore document reference by searching for the field 'caseId' before
     * executing the update, ensuring compatibility with Web Dashboard.
     */
    fun updateStatusByCaseId(
        caseId: String,
        newStatus: String,
        userId: String? = null,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val mappedStatus = when (newStatus.trim().uppercase()) {
            "ปิดเคส", "ซ่อมเสร็จแล้ว", "CLOSED" -> "CLOSED"
            "เปิดเคส", "รอดำเนินการ", "OPEN" -> "OPEN"
            else -> newStatus.trim()
        }

        val updates = mutableMapOf<String, Any?>(
            "status" to mappedStatus,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        if (mappedStatus == "CLOSED") {
            updates["closedAt"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
        }

        updateCaseFields(
            caseId = caseId,
            fields = updates,
            userId = userId,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * ลบเคสใน Firestore Collection "repair_cases"
     * รองรับทั้งการส่ง caseId ตรงๆ หรือ Firestore Document ID
     */
    fun deleteCase(
        caseId: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val fs = firestore
        if (fs == null) {
            onError(Exception("Firestore is not initialized"))
            return
        }

        val trimmedId = caseId.trim()
        if (trimmedId.isBlank()) {
            onSuccess()
            return
        }

        ensureAuth(
            onSuccess = {
                // ค้นหาว่า caseId นี้คือ document id หรือ field caseId
                fs.collection("repair_cases")
                    .whereEqualTo("caseId", trimmedId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val docRef = if (querySnapshot != null && !querySnapshot.isEmpty) {
                            val docId = querySnapshot.documents.first().id
                            fs.collection("repair_cases").document(docId)
                        } else {
                            fs.collection("repair_cases").document(trimmedId)
                        }

                        val resolvedDocId = docRef.id
                        docRef.delete()
                            .addOnSuccessListener {
                                Log.d(TAG, "Successfully deleted case repair_cases/$resolvedDocId from Firestore")
                                currentUserId?.let { uid ->
                                    if (uid.isNotBlank()) {
                                        fs.collection("users").document(uid).collection("repair_cases").document(resolvedDocId).delete()
                                    }
                                }
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to delete case $resolvedDocId: ${e.message}", e)
                                onError(e)
                            }
                    }
                    .addOnFailureListener { e ->
                        // Fallback delete directly by document ID
                        fs.collection("repair_cases").document(trimmedId).delete()
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { ex -> onError(ex) }
                    }
            },
            onFailure = { e ->
                Log.e(TAG, "deleteCase auth failed: ${e.message}", e)
                onError(e)
            }
        )
    }

    /**
     * Real-time listener (onSnapshot) for Repair Cases under root "repair_cases"
     * Automatically filters by technicianId for the logged-in technician.
     * Uses in-memory sorting to avoid Firestore Composite Index requirements.
     */
    fun getRealtimeCases(assignedTechId: String? = null): Flow<List<RepairCase>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var listener: com.google.firebase.firestore.ListenerRegistration? = null

        ensureAuth(
            onSuccess = {
                val prefs = context?.let { EncryptedPrefsManager(it) }
                val targetTechId = (assignedTechId ?: prefs?.getTechnicianId() ?: currentUserId ?: "").trim()

                if (targetTechId.isNotBlank()) {
                    val query = fs.collection("repair_cases")
                        .whereEqualTo("technicianId", targetTechId)

                    listener = query.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Snapshot listener note for repair_cases: ${error.message}")
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val parsedCases = parseCasesFromSnapshot(snapshot).sortedByDescending { it.timestampLong }
                            trySend(parsedCases)
                        }
                    }
                } else {
                    Log.w(TAG, "getRealtimeCases: technicianId is empty or blank")
                    trySend(emptyList())
                }
            },
            onFailure = { e ->
                Log.w(TAG, "getRealtimeCases auth status: ${e.message}")
                trySend(emptyList())
            }
        )

        awaitClose { listener?.remove() }
    }

    /**
     * Data class for parsing notifications
     */
    data class NotificationDoc(
        val id: String = "",
        val caseId: String = "",
        val customerName: String = "",
        val isRead: Boolean = false,
        val technicianId: String = "",
        val timestampLong: Long = 0L
    )

    /**
     * Real-time listener for "notifications" collection
     */
    fun getUnreadNotifications(technicianId: String): Flow<List<NotificationDoc>> = callbackFlow {
        val fs = firestore
        if (fs == null || technicianId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var listener: com.google.firebase.firestore.ListenerRegistration? = null

        ensureAuth(
            onSuccess = {
                try {
                    val query = fs.collection("notifications")
                        .whereEqualTo("technicianId", technicianId.trim())

                    listener = query.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Notification stream unavailable or restricted: ${error.message}")
                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val notifications = snapshot.documents.mapNotNull { doc ->
                                try {
                                    NotificationDoc(
                                        id = doc.id,
                                        caseId = doc.getString("caseId") ?: "",
                                        customerName = doc.getString("customerName") ?: "",
                                        isRead = doc.getBoolean("isRead") ?: false,
                                        technicianId = doc.getString("technicianId") ?: "",
                                        timestampLong = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            // Filter in-memory to avoid compound index/rules issues
                            trySend(notifications.filter { !it.isRead && it.technicianId == technicianId.trim() })
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Setup notifications listener exception: ${e.message}")
                    trySend(emptyList())
                }
            },
            onFailure = {
                trySend(emptyList())
            }
        )

        awaitClose { listener?.remove() }
    }

    /**
     * Mark a notification as read
     */
    fun markNotificationAsRead(notificationId: String) {
        val fs = firestore ?: return
        if (notificationId.isBlank()) return

        fs.collection("notifications").document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                Log.d(TAG, "Notification $notificationId marked as read")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to mark notification $notificationId as read", e)
            }
    }


    private fun parseCasesFromSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot): List<RepairCase> {
        return snapshot.documents.mapNotNull { doc ->
            try {
                val c = doc.toObject(RepairCase::class.java)
                val docAssignedId = (doc.getString("assignedTechId") ?: doc.getString("technicianId") ?: "").trim()
                val docAssignedPin = (doc.getString("assignedTechPin") ?: doc.getString("technicianPin") ?: doc.getString("pin") ?: "").trim()
                val docTechName = (doc.getString("technicianName") ?: doc.getString("assignedTechName") ?: "").trim()
                val docPhone = (doc.getString("decryptedPhone") ?: doc.getString("phone") ?: doc.getString("encryptedPhone") ?: "").trim()
                val docAddress = (doc.getString("decryptedAddress") ?: doc.getString("address") ?: doc.getString("encryptedAddress") ?: "").trim()

                val finalCaseId = if (c?.caseId.isNullOrBlank()) doc.id else c!!.caseId

                (c ?: RepairCase()).copy(
                    caseId = finalCaseId,
                    customerName = if (c?.customerName.isNullOrBlank()) (doc.getString("customerName") ?: "ลูกค้าทั่วไป") else c!!.customerName,
                    encryptedPhone = if (c?.encryptedPhone.isNullOrBlank()) docPhone else c!!.encryptedPhone,
                    encryptedAddress = if (c?.encryptedAddress.isNullOrBlank()) docAddress else c!!.encryptedAddress,
                    details = if (c?.details.isNullOrBlank()) (doc.getString("details") ?: "") else c!!.details,
                    status = if (c?.status.isNullOrBlank()) (doc.getString("status") ?: "OPEN") else c!!.status,
                    technicianName = if (c?.technicianName.isNullOrBlank()) docTechName else c!!.technicianName,
                    assignedTechId = if (c?.assignedTechId.isNullOrBlank()) docAssignedId else c!!.assignedTechId,
                    technicianId = if (c?.technicianId.isNullOrBlank()) docAssignedId else c!!.technicianId,
                    assignedTechPin = if (c?.assignedTechPin.isNullOrBlank()) docAssignedPin else c!!.assignedTechPin,
                    imageUrl = if (c?.imageUrl.isNullOrBlank()) (doc.getString("imageUrl") ?: "") else c!!.imageUrl,
                    checkInImageUrl = if (c?.checkInImageUrl.isNullOrBlank()) (doc.getString("checkInImageUrl") ?: "") else c!!.checkInImageUrl,
                    checkOutImageUrl = if (c?.checkOutImageUrl.isNullOrBlank()) (doc.getString("checkOutImageUrl") ?: "") else c!!.checkOutImageUrl
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing case document ${doc.id}: ${e.message}")
                null
            }
        }
    }

    // ==========================================
    // Firebase Storage: Multi-technician Image Uploads
    // ==========================================

    private suspend fun uploadFileToStorageInternal(
        stor: FirebaseStorage,
        bytes: ByteArray?,
        localUriOrPath: String,
        storagePath: String,
        metadata: StorageMetadata
    ): String = suspendCancellableCoroutine { continuation ->
        val storageRef = stor.reference.child(storagePath)
        val uploadTask = if (bytes != null && bytes.isNotEmpty()) {
            storageRef.putBytes(bytes, metadata)
        } else {
            val fileUri = if (localUriOrPath.startsWith("content://") || localUriOrPath.startsWith("file://")) {
                Uri.parse(localUriOrPath)
            } else {
                Uri.fromFile(File(localUriOrPath))
            }
            storageRef.putFile(fileUri, metadata)
        }

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storageRef.downloadUrl
        }.addOnSuccessListener { downloadUri ->
            val downloadUrl = downloadUri.toString()
            Log.d(TAG, "Upload succeeded to $storagePath, URL: $downloadUrl")
            if (continuation.isActive) {
                continuation.resume(downloadUrl)
            }
        }.addOnFailureListener { ex ->
            if (continuation.isActive) {
                continuation.resumeWithException(ex)
            }
        }

        continuation.invokeOnCancellation {
            uploadTask.cancel()
        }
    }

    /**
     * Uploads a single local file (Uri string or absolute file path) to Firebase Storage
     * and returns the public download URL. Automatically retries with alternative bucket names if a 404 occurs.
     */
    suspend fun uploadFileToStorage(localUriOrPath: String, storagePath: String): String {
        val primaryStor = storage ?: throw IllegalStateException("Firebase Storage is not initialized")

        val bytes: ByteArray? = try {
            if (localUriOrPath.startsWith("content://") || localUriOrPath.startsWith("file://")) {
                val uri = Uri.parse(localUriOrPath)
                context?.contentResolver?.openInputStream(uri)?.use { it.readBytes() }
            } else {
                val file = File(localUriOrPath)
                if (file.exists()) file.readBytes() else {
                    val uri = Uri.parse(localUriOrPath)
                    context?.contentResolver?.openInputStream(uri)?.use { it.readBytes() }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading bytes directly for $localUriOrPath: ${e.message}")
            null
        }

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        try {
            return uploadFileToStorageInternal(primaryStor, bytes, localUriOrPath, storagePath, metadata)
        } catch (ex: Exception) {
            val is404 = ex is com.google.firebase.storage.StorageException && ex.errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND || (ex.message?.contains("404") == true) || (ex.message?.contains("does not exist") == true)
            
            if (is404) {
                val projectId = try { FirebaseApp.getInstance().options.projectId } catch (e: Exception) { null }
                if (!projectId.isNullOrBlank()) {
                    val currentBucket = try { FirebaseApp.getInstance().options.storageBucket } catch (e: Exception) { null } ?: ""
                    val fallbackBucket = if (currentBucket.contains("firebasestorage.app")) {
                        "gs://$projectId.appspot.com"
                    } else {
                        "gs://$projectId.firebasestorage.app"
                    }

                    try {
                        Log.d(TAG, "Primary storage upload returned 404. Retrying with fallback bucket: $fallbackBucket")
                        val fallbackStor = FirebaseStorage.getInstance(fallbackBucket)
                        return uploadFileToStorageInternal(fallbackStor, bytes, localUriOrPath, storagePath, metadata)
                    } catch (fallbackEx: Exception) {
                        Log.w(TAG, "Fallback Firebase Storage upload failed: ${fallbackEx.message}")
                    }
                }
            }
            Log.w(TAG, "Upload skipped or failed for $storagePath: ${ex.message}")
            throw ex
        }
    }

    /**
     * Uploads multiple image URIs for a case to Firebase Storage under "users/{userId}/cases/{caseId}/..."
     * and returns a list of resulting download URLs.
     */
    suspend fun uploadImagesAndGetUrls(
        caseId: String,
        imageUriString: String?,
        prefix: String = "img",
        userId: String? = null
    ): List<String> {
        if (imageUriString.isNullOrBlank()) return emptyList()
        val uid = userId ?: currentUserId ?: "common"
        val paths = imageUriString.split("|").map { it.trim() }.filter { it.isNotBlank() }
        val urls = mutableListOf<String>()
        paths.forEachIndexed { index, path ->
            if (path.startsWith("http://") || path.startsWith("https://")) {
                urls.add(path)
                return@forEachIndexed
            }
            try {
                val fileName = "${prefix}_${System.currentTimeMillis()}_${index + 1}.jpg"
                val storagePath = "users/$uid/cases/$caseId/$fileName"
                val downloadUrl = uploadFileToStorage(path, storagePath)
                urls.add(downloadUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Failed uploading image $path for case $caseId: ${e.message}")
            }
        }
        return urls
    }

    /**
     * Uploads any attached images (main images, Check IN, Check OUT) to Firebase Storage
     * and writes their download URLs into the case Document in Firestore "users/{userId}/repair_cases/{caseId}".
     */
    suspend fun uploadAndSyncCaseImages(
        caseId: String,
        imageUriString: String? = null,
        checkInImageUri: String? = null,
        checkOutImageUri: String? = null,
        userId: String? = null,
        onSuccess: (Map<String, Any>) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        try {
            val uid = userId ?: currentUserId
            val updates = mutableMapOf<String, Any>()

            // 1. Upload main images if any
            if (!imageUriString.isNullOrBlank()) {
                val uploadedUrls = uploadImagesAndGetUrls(caseId, imageUriString, prefix = "attach", userId = uid)
                if (uploadedUrls.isNotEmpty()) {
                    updates["imageUrl"] = uploadedUrls.first()
                    updates["imageUrls"] = uploadedUrls
                }
            }

            // 2. Process Check IN image to Base64 & save in case_images collection if any
            if (!checkInImageUri.isNullOrBlank()) {
                if (checkInImageUri.startsWith("http://") || checkInImageUri.startsWith("https://")) {
                    updates["checkInImageUrl"] = checkInImageUri
                } else {
                    try {
                        val ctx = context
                        if (ctx != null) {
                            val base64Image = com.example.util.compressImageToBase64(
                                context = ctx,
                                imageUri = checkInImageUri
                            )
                            if (base64Image.isNotBlank()) {
                                val fs = firestore ?: com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val checkInImageDoc = mapOf(
                                    "caseId" to caseId,
                                    "type" to "checkin",
                                    "imageBase64" to base64Image,
                                    "technicianId" to uid,
                                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                )
                                fs.collection("case_images")
                                    .document("${caseId}_checkin")
                                    .set(checkInImageDoc)
                                    .await()
                                
                                updates["checkInImageStored"] = true
                                updates["checkInImageRef"] = "${caseId}_checkin"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Check-in image base64 processing failed: ${e.message}", e)
                    }
                }
            }

            // 3. Process Check OUT image to Base64 & save in case_images collection if any
            if (!checkOutImageUri.isNullOrBlank()) {
                if (checkOutImageUri.startsWith("http://") || checkOutImageUri.startsWith("https://")) {
                    updates["checkOutImageUrl"] = checkOutImageUri
                } else {
                    try {
                        val ctx = context
                        if (ctx != null) {
                            val base64Image = com.example.util.compressImageToBase64(
                                context = ctx,
                                imageUri = checkOutImageUri
                            )
                            if (base64Image.isNotBlank()) {
                                val fs = firestore ?: com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val checkOutImageDoc = mapOf(
                                    "caseId" to caseId,
                                    "type" to "checkout",
                                    "imageBase64" to base64Image,
                                    "technicianId" to uid,
                                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                )
                                fs.collection("case_images")
                                    .document("${caseId}_checkout")
                                    .set(checkOutImageDoc)
                                    .await()
                                
                                updates["checkOutImageStored"] = true
                                updates["checkOutImageRef"] = "${caseId}_checkout"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Check-out image base64 processing failed: ${e.message}", e)
                    }
                }
            }

            // 4. Update the Firestore Document with download URLs
            if (updates.isNotEmpty()) {
                updateCaseFields(
                    caseId = caseId,
                    fields = updates,
                    userId = uid,
                    onSuccess = {
                        Log.d(TAG, "Successfully synced Storage download URLs to Firestore for case users/$uid/repair_cases/$caseId")
                        onSuccess(updates)
                    },
                    onError = { ex ->
                        onError(ex)
                    }
                )
            } else {
                onSuccess(emptyMap())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in uploadAndSyncCaseImages: ${e.message}", e)
            onError(e)
        }
    }

    /**
     * Helper to get saved technician ID from EncryptedPrefsManager.
     */
    fun getSavedTechnicianId(): String? {
        return context?.let { EncryptedPrefsManager(it).getSavedTechnicianId() }
    }

    /**
     * Helper to get saved technician Name from EncryptedPrefsManager.
     */
    fun getSavedTechnicianName(): String? {
        return context?.let { EncryptedPrefsManager(it).getSavedTechnicianName() }
    }

    /**
     * Records activity log in Firestore "activity_logs" collection.
     *
     * Document Schema:
     * - technicianId: Document ID of technician in "technicians" collection
     * - technicianName: Display name of technician
     * - action: "LOGIN" | "LOGOUT" | "OPEN_CASE" | "CLOSE_CASE"
     * - caseId: Document ID of case (if applicable)
     * - caseTitle: Title or Customer Name of case (if applicable)
     * - timestamp: FieldValue.serverTimestamp()
     * - deviceInfo: android.os.Build.MODEL
     */
    fun logActivity(
        technicianId: String,
        technicianName: String,
        action: String,
        caseId: String? = null,
        caseTitle: String? = null
    ) {
        val fs = firestore ?: return
        val cleanTechId = technicianId.trim()
        val cleanTechName = technicianName.trim()

        if (cleanTechId.isBlank()) {
            Log.w(TAG, "logActivity skipped: technicianId is blank")
            return
        }

        try {
            val logData = hashMapOf<String, Any?>(
                "technicianId" to cleanTechId,
                "technicianName" to cleanTechName,
                "action" to action,
                "caseId" to caseId,
                "caseTitle" to caseTitle,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "deviceInfo" to android.os.Build.MODEL
            )

            fs.collection("activity_logs")
                .add(logData)
                .addOnSuccessListener {
                    Log.d(TAG, "Activity logged successfully: action=$action, tech=$cleanTechName, caseId=$caseId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to log activity: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in logActivity: ${e.message}", e)
        }
    }
}
