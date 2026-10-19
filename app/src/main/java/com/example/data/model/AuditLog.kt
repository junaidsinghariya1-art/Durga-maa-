package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actor: String, // e.g. "Owner", customer phone, or "System"
    val action: String, // e.g. "UPI_GATEWAY_UPDATED", "PAYMENT_VERIFIED", "LOAN_APPROVED", "KYC_CONSENT_RECORDED"
    val details: String, // Sanitized summary (never passwords or secret tokens)
    val ipOrDevice: String = "Android Device / App Session"
)
