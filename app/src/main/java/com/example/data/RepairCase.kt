package com.example.data

import com.example.util.EncryptionHelper
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Technician Data Model for Authentication and Firestore "technicians" collection
 */
@IgnoreExtraProperties
data class Technician(
    val id: String = "",
    val name: String = "",
    val pin: String = "", // 4-digit PIN
    val role: String = "Technician"
)

/**
 * Case Model stored in Firestore "repair_cases" collection
 * Sensitive information (Phone, Address) is stored as Field-Level Encrypted strings
 */
@IgnoreExtraProperties
data class RepairCase(
    val caseId: String = "",
    val customerName: String = "",
    val encryptedPhone: String = "",
    val encryptedAddress: String = "",
    val details: String = "",
    val status: String = "OPEN", // "OPEN", "IN_PROGRESS", "CLOSED"
    val technicianName: String = "",
    val timestamp: Any? = null,
    val imageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val checkInImageUrl: String = "",
    val checkOutImageUrl: String = "",
    val assignedTechId: String = "",
    val assignedTechPin: String = "",
    val technicianId: String = "",
    val technicianPin: String = ""
) {
    /**
     * Decrypted phone number for display in the app UI
     */
    val decryptedPhone: String
        get() {
            val dec = EncryptionHelper.decrypt(encryptedPhone)
            return if (dec.isNotBlank()) dec else encryptedPhone
        }

    /**
     * Decrypted address for display in the app UI
     */
    val decryptedAddress: String
        get() {
            val dec = EncryptionHelper.decrypt(encryptedAddress)
            return if (dec.isNotBlank()) dec else encryptedAddress
        }

    /**
     * Safely resolves any Timestamp / Date / Long representation of creation time.
     */
    val timestampLong: Long
        get() {
            return when (val ts = timestamp) {
                is com.google.firebase.Timestamp -> ts.seconds * 1000
                is Long -> ts
                is Double -> ts.toLong()
                is Number -> ts.toLong()
                is java.util.Date -> ts.time
                else -> System.currentTimeMillis()
            }
        }

    companion object {
        /**
         * Helper builder to create a RepairCase automatically encrypting phone & address
         */
        fun createEncryptedCase(
            caseId: String,
            customerName: String = "",
            plainPhone: String = "",
            plainAddress: String = "",
            details: String = "",
            status: String = "OPEN",
            technicianName: String = "",
            imageUrl: String = "",
            imageUrls: List<String> = emptyList(),
            checkInImageUrl: String = "",
            checkOutImageUrl: String = ""
        ): RepairCase {
            return RepairCase(
                caseId = caseId,
                customerName = customerName,
                encryptedPhone = EncryptionHelper.encrypt(plainPhone),
                encryptedAddress = EncryptionHelper.encrypt(plainAddress),
                details = details,
                status = status,
                technicianName = technicianName,
                timestamp = System.currentTimeMillis(),
                imageUrl = imageUrl,
                imageUrls = imageUrls,
                checkInImageUrl = checkInImageUrl,
                checkOutImageUrl = checkOutImageUrl
            )
        }
    }
}
