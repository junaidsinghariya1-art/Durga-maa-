package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DocumentType {
    AADHAAR_CARD,
    PAN_CARD,
    PASSPORT,
    VOTER_ID
}

@Entity(
    tableName = "kyc_submissions",
    indices = [Index(value = ["customerPhone"])]
)
data class KycSubmissionEntity(
    @PrimaryKey
    val id: String, // e.g. "KYC-30291"
    val customerPhone: String,
    val documentType: DocumentType,
    val idNumber: String,
    val documentUri: String? = null,
    val selfieUri: String? = null,
    val consentGiven: Boolean,
    val consentTimestamp: Long,
    val consentPurpose: String = "Identity verification and statutory loan eligibility assessment",
    val privacyPolicyVersion: String = "v2.4-2026",
    val status: KycStatus = KycStatus.PENDING_REVIEW,
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val reviewNotes: String? = null,
    val consentWithdrawn: Boolean = false,
    val consentWithdrawnAt: Long? = null
)
