package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * EncryptedSharedPreferences Manager for securely storing local session data
 * (Technician Name, ID, PIN, Login Status)
 */
class EncryptedPrefsManager(context: Context) {

    private val TAG = "EncryptedPrefsManager"
    private val PREF_FILE_NAME = "secure_technician_prefs"

    private val KEY_TECH_ID = "key_technician_id"
    private val KEY_TECH_NAME = "key_technician_name"
    private val KEY_TECH_EMAIL = "key_technician_email"
    private val KEY_TECH_ROLE = "key_technician_role"
    private val KEY_TECH_PIN = "key_technician_pin"
    private val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private val KEY_LOGIN_TIMESTAMP = "key_login_timestamp"

    // Persistent Device Assigned Technician keys
    private val KEY_SAVED_DEVICE_TECH_NAME = "key_saved_device_tech_name"
    private val KEY_SAVED_DEVICE_TECH_EMAIL = "key_saved_device_tech_email"
    private val KEY_SAVED_DEVICE_TECH_ROLE = "key_saved_device_tech_role"
    private val KEY_REMEMBER_DEVICE_ENABLED = "key_remember_device_enabled"

    private val prefs: SharedPreferences by lazy {
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
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, falling back to standard prefs: ${e.message}", e)
            context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Saves active technician session details securely.
     * PIN or passwords are NEVER stored in EncryptedSharedPreferences.
     */
    fun saveTechnicianSession(
        id: String,
        name: String,
        email: String = "",
        role: String = "Technician",
        rememberOnDevice: Boolean = true
    ) {
        try {
            val editor = prefs.edit()
                .putString(KEY_TECH_ID, id)
                .putString(KEY_TECH_NAME, name)
                .putString(KEY_TECH_EMAIL, email)
                .putString(KEY_TECH_ROLE, role)
                .remove(KEY_TECH_PIN) // Ensure PIN is never stored
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())

            if (rememberOnDevice && name.isNotBlank()) {
                editor.putString(KEY_SAVED_DEVICE_TECH_NAME, name)
                editor.putString(KEY_SAVED_DEVICE_TECH_EMAIL, email)
                editor.putString(KEY_SAVED_DEVICE_TECH_ROLE, role)
                editor.putBoolean(KEY_REMEMBER_DEVICE_ENABLED, true)
            }

            editor.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session: ${e.message}", e)
        }
    }

    /**
     * Saves active technician session details securely with technicianId and technicianName.
     */
    fun saveTechnicianSession(
        technicianId: String,
        technicianName: String
    ) {
        saveTechnicianSession(
            id = technicianId,
            name = technicianName,
            email = "",
            role = "Technician",
            rememberOnDevice = true
        )
    }

    /**
     * Backward-compatible overload for legacy callers. Ignores PIN input completely.
     */
    fun saveTechnicianSession(
        id: String,
        name: String,
        pin: String,
        email: String = "",
        role: String = "Technician",
        rememberOnDevice: Boolean = true
    ) {
        saveTechnicianSession(
            id = id,
            name = name,
            email = email,
            role = role,
            rememberOnDevice = rememberOnDevice
        )
    }

    /**
     * Returns saved technician ID or null if absent or blank.
     */
    fun getSavedTechnicianId(): String? {
        val id = prefs.getString(KEY_TECH_ID, null)?.trim()
        return if (id.isNullOrBlank()) null else id
    }

    /**
     * Returns saved technician Name or null if absent or blank.
     */
    fun getSavedTechnicianName(): String? {
        val name = prefs.getString(KEY_TECH_NAME, null)?.trim()
        return if (name.isNullOrBlank()) null else name
    }

    /**
     * Checks if there is an active technician session saved.
     */
    fun hasActiveSesssion(): Boolean {
        return !getSavedTechnicianId().isNullOrBlank()
    }

    /**
     * Alias method for hasActiveSesssion() to handle alternative spelling.
     */
    fun hasActiveSession(): Boolean {
        return hasActiveSesssion()
    }

    /**
     * Saves or updates the technician remembered as the default for this device.
     */
    fun saveRememberedDeviceTechnician(name: String, email: String, role: String = "IT Support Onsite") {
        try {
            prefs.edit()
                .putString(KEY_SAVED_DEVICE_TECH_NAME, name)
                .putString(KEY_SAVED_DEVICE_TECH_EMAIL, email)
                .putString(KEY_SAVED_DEVICE_TECH_ROLE, role)
                .putBoolean(KEY_REMEMBER_DEVICE_ENABLED, true)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving device technician: ${e.message}", e)
        }
    }

    /**
     * Gets the remembered technician name for this device.
     */
    fun getRememberedDeviceTechnicianName(): String {
        return prefs.getString(KEY_SAVED_DEVICE_TECH_NAME, "") ?: ""
    }

    /**
     * Gets the remembered technician email for this device.
     */
    fun getRememberedDeviceTechnicianEmail(): String {
        return prefs.getString(KEY_SAVED_DEVICE_TECH_EMAIL, "") ?: ""
    }

    /**
     * Gets the remembered technician role for this device.
     */
    fun getRememberedDeviceTechnicianRole(): String {
        val rawRole = prefs.getString(KEY_SAVED_DEVICE_TECH_ROLE, "IT Support Onsite") ?: "IT Support Onsite"
        return if (rawRole == "ช่างบริการภาคสนาม") "IT Support Onsite" else rawRole
    }

    /**
     * Checks if this device has a remembered technician assigned.
     */
    fun hasRememberedDeviceTechnician(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_DEVICE_ENABLED, true) && 
               getRememberedDeviceTechnicianName().isNotBlank()
    }

    /**
     * Clears the remembered device technician assignment.
     */
    fun forgetDeviceTechnician() {
        try {
            prefs.edit()
                .remove(KEY_SAVED_DEVICE_TECH_NAME)
                .remove(KEY_SAVED_DEVICE_TECH_EMAIL)
                .remove(KEY_SAVED_DEVICE_TECH_ROLE)
                .putBoolean(KEY_REMEMBER_DEVICE_ENABLED, false)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error forgetting device technician: ${e.message}", e)
        }
    }

    /**
     * Gets current logged in technician ID (Firebase UID).
     */
    fun getTechnicianId(): String {
        return prefs.getString(KEY_TECH_ID, "") ?: ""
    }

    /**
     * Gets current logged in technician name.
     */
    fun getTechnicianName(): String {
        return prefs.getString(KEY_TECH_NAME, "") ?: ""
    }

    /**
     * Gets current logged in technician email.
     */
    fun getTechnicianEmail(): String {
        return prefs.getString(KEY_TECH_EMAIL, "") ?: ""
    }

    /**
     * Gets current logged in technician role.
     */
    fun getTechnicianRole(): String {
        return prefs.getString(KEY_TECH_ROLE, "Technician") ?: "Technician"
    }

    /**
     * PINs are never stored in EncryptedSharedPreferences for security. Returns empty string.
     */
    fun getTechnicianPin(): String {
        return ""
    }

    /**
     * Checks if technician has an active session saved.
     */
    fun isLoggedIn(): Boolean {
        return hasActiveSesssion()
    }

    /**
     * Clears active session upon logout (keeps remembered device technician intact).
     */
    fun clearSession() {
        try {
            prefs.edit()
                .remove(KEY_TECH_ID)
                .remove(KEY_TECH_NAME)
                .remove(KEY_TECH_EMAIL)
                .remove(KEY_TECH_ROLE)
                .remove(KEY_TECH_PIN)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_LOGIN_TIMESTAMP)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing session: ${e.message}", e)
        }
    }
}
