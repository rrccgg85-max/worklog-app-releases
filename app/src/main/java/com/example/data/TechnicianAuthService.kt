package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.util.EncryptedPrefsManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * TechnicianAuthService
 *
 * Secure authentication service that retrieves technician records from the 'technicians'
 * collection in Firestore, compares provided PINs against stored 'pinCode' fields,
 * and manages secure session persistence using EncryptedSharedPreferences.
 */
class TechnicianAuthService(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val TAG = "TechnicianAuthService"
    private val PREF_FILE_NAME = "secure_technician_session_prefs"

    // Session keys
    private val KEY_TECH_ID = "key_tech_id"
    private val KEY_TECH_NAME = "key_tech_name"
    private val KEY_TECH_EMAIL = "key_tech_email"
    private val KEY_TECH_ROLE = "key_tech_role"
    private val KEY_TECH_PIN = "key_tech_pin"
    private val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private val KEY_LOGIN_TIMESTAMP = "key_login_timestamp"

    private val prefsManager = EncryptedPrefsManager(context)

    // EncryptedSharedPreferences instance using MasterKey AES256 encryption
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing EncryptedSharedPreferences: ${e.message}", e)
            context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Data class representing an active technician session.
     */
    data class TechnicianSession(
        val id: String,
        val name: String,
        val email: String,
        val role: String,
        val pin: String,
        val loginTimestamp: Long = System.currentTimeMillis()
    )

    /**
     * Result sealed class for authentication operations.
     */
    sealed class AuthResult {
        data class Success(val session: TechnicianSession) : AuthResult()
        data class InvalidPin(val message: String = "รหัสผ่าน (PIN) ไม่ถูกต้อง") : AuthResult()
        data class UserNotFound(val message: String = "ไม่พบข้อมูลช่างในระบบ") : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    /**
     * Retrieves all registered technician records from the 'technicians' collection in Firestore.
     */
    suspend fun getTechnicianRecords(): List<FirestoreCaseRepository.TechnicianProfile> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("technicians").get().await()
            val dummyIds = setOf("tech_001", "tech_002", "tech_003", "tech_admin")
            
            snapshot.documents.mapNotNull { doc ->
                if (dummyIds.contains(doc.id)) return@mapNotNull null
                val data = doc.data ?: return@mapNotNull null
                val name = (data["name"] ?: data["displayName"] ?: data["fullName"] ?: data["technicianName"] ?: "") as? String ?: ""
                val email = (data["email"] ?: data["mail"] ?: "") as? String ?: ""
                val role = (data["role"] ?: data["position"] ?: "ช่างบริการภาคสนาม") as? String ?: "ช่างบริการภาคสนาม"
                val pinCode = (data["pinCode"] ?: data["pin"] ?: data["passcode"] ?: "") as? String ?: ""

                if (name.isNotBlank()) {
                    FirestoreCaseRepository.TechnicianProfile(
                        id = doc.id,
                        name = name.trim(),
                        email = email.trim(),
                        role = role.trim(),
                        pin = pinCode.trim()
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching technician records: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Authenticates a technician by looking up their document in the 'technicians' collection in Firestore,
     * comparing the provided PIN against the stored 'pinCode' field, and persisting the session
     * using EncryptedSharedPreferences upon successful verification.
     *
     * @param technicianId Document ID or ID of the technician in Firestore
     * @param providedPin PIN code entered by the technician
     * @return AuthResult indicating Success, InvalidPin, UserNotFound, or Error
     */
    suspend fun authenticateTechnician(
        technicianId: String,
        providedPin: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val cleanTechId = technicianId.trim()
        val cleanProvidedPin = providedPin.trim()

        if (cleanTechId.isBlank()) {
            return@withContext AuthResult.Error("กรุณาเลือกหรือระบุช่างบริการ")
        }
        if (cleanProvidedPin.isBlank()) {
            return@withContext AuthResult.InvalidPin("กรุณากรอกรหัสผ่าน (PIN)")
        }

        try {
            // 1. Retrieve document from 'technicians' collection
            val docSnap = firestore.collection("technicians").document(cleanTechId).get().await()

            if (!docSnap.exists()) {
                // Fallback search by ID field if document key differs
                val querySnap = firestore.collection("technicians")
                    .whereEqualTo("id", cleanTechId)
                    .get()
                    .await()

                val matchedDoc = querySnap.documents.firstOrNull()
                if (matchedDoc == null || !matchedDoc.exists()) {
                    Log.w(TAG, "Technician record not found for ID: $cleanTechId")
                    return@withContext AuthResult.UserNotFound()
                }
                return@withContext verifyAndSaveSession(matchedDoc.id, matchedDoc.data, cleanProvidedPin)
            }

            return@withContext verifyAndSaveSession(docSnap.id, docSnap.data, cleanProvidedPin)

        } catch (e: Exception) {
            Log.e(TAG, "Error during technician authentication: ${e.message}", e)
            return@withContext AuthResult.Error(e.localizedMessage ?: "เกิดข้อผิดพลาดในการเชื่อมต่อระบบ")
        }
    }

    /**
     * Verifies stored 'pinCode' against provided PIN and persists session on match.
     */
    private fun verifyAndSaveSession(
        docId: String,
        data: Map<String, Any>?,
        providedPin: String
    ): AuthResult {
        if (data == null) {
            return AuthResult.UserNotFound()
        }

        val name = (data["name"] ?: data["displayName"] ?: data["fullName"] ?: "ช่างบริการ") as? String ?: "ช่างบริการ"
        val email = (data["email"] ?: data["mail"] ?: "") as? String ?: ""
        val role = (data["role"] ?: data["position"] ?: "ช่างบริการภาคสนาม") as? String ?: "ช่างบริการภาคสนาม"
        
        // Extract stored pinCode (with fallbacks for legacy field naming)
        val storedPinCode = (data["pinCode"] ?: data["pin"] ?: data["passcode"] ?: data["password"] ?: "") as? String ?: ""
        val cleanStoredPin = storedPinCode.trim()

        if (cleanStoredPin.isBlank()) {
            Log.w(TAG, "No PIN configured for technician docId: $docId")
            return AuthResult.Error("ยังไม่ได้ตั้งค่ารหัส PIN สำหรับช่างรายนี้")
        }

        // Compare provided PIN with stored 'pinCode'
        if (providedPin == cleanStoredPin) {
            val session = TechnicianSession(
                id = docId,
                name = name.trim(),
                email = email.trim(),
                role = role.trim(),
                pin = cleanStoredPin
            )

            // Save session to EncryptedSharedPreferences
            saveSessionToEncryptedPrefs(session)

            Log.i(TAG, "Successfully authenticated technician: ${session.name} (ID: $docId)")
            return AuthResult.Success(session)
        } else {
            Log.w(TAG, "Invalid PIN attempt for technician: $name (ID: $docId)")
            return AuthResult.InvalidPin()
        }
    }

    /**
     * Persists technician session details to EncryptedSharedPreferences.
     */
    fun saveSessionToEncryptedPrefs(session: TechnicianSession) {
        try {
            encryptedPrefs.edit()
                .putString(KEY_TECH_ID, session.id)
                .putString(KEY_TECH_NAME, session.name)
                .putString(KEY_TECH_EMAIL, session.email)
                .putString(KEY_TECH_ROLE, session.role)
                .putString(KEY_TECH_PIN, session.pin)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putLong(KEY_LOGIN_TIMESTAMP, session.loginTimestamp)
                .apply()

            // Also update shared EncryptedPrefsManager for app-wide compatibility
            prefsManager.saveTechnicianSession(
                id = session.id,
                name = session.name,
                pin = session.pin,
                email = session.email,
                role = session.role
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session to EncryptedSharedPreferences: ${e.message}", e)
        }
    }

    /**
     * Retrieves the current persisted technician session from EncryptedSharedPreferences.
     */
    fun getSavedSession(): TechnicianSession? {
        if (!isLoggedIn()) return null

        val id = encryptedPrefs.getString(KEY_TECH_ID, "") ?: ""
        val name = encryptedPrefs.getString(KEY_TECH_NAME, "") ?: ""
        val email = encryptedPrefs.getString(KEY_TECH_EMAIL, "") ?: ""
        val role = encryptedPrefs.getString(KEY_TECH_ROLE, "ช่างบริการภาคสนาม") ?: "ช่างบริการภาคสนาม"
        val pin = encryptedPrefs.getString(KEY_TECH_PIN, "") ?: ""
        val timestamp = encryptedPrefs.getLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())

        if (id.isBlank() && name.isBlank()) return null

        return TechnicianSession(
            id = id,
            name = name,
            email = email,
            role = role,
            pin = pin,
            loginTimestamp = timestamp
        )
    }

    /**
     * Checks whether a technician is currently logged in via EncryptedSharedPreferences.
     */
    fun isLoggedIn(): Boolean {
        return encryptedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Clears active session data from EncryptedSharedPreferences upon logout.
     */
    fun logout() {
        try {
            encryptedPrefs.edit()
                .remove(KEY_TECH_ID)
                .remove(KEY_TECH_NAME)
                .remove(KEY_TECH_EMAIL)
                .remove(KEY_TECH_ROLE)
                .remove(KEY_TECH_PIN)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_LOGIN_TIMESTAMP)
                .apply()

            prefsManager.clearSession()
            Log.i(TAG, "Technician session cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing EncryptedSharedPreferences session: ${e.message}", e)
        }
    }
}
