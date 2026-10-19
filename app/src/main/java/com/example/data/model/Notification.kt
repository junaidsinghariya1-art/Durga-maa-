package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class NotificationType {
    PAYMENT_SUBMITTED,
    PAYMENT_VERIFIED,
    PAYMENT_REJECTED,
    LOAN_APPROVED,
    LOAN_REJECTED,
    LOAN_CLOSED,
    KYC_UPDATE,
    UPI_CONFIG_CHANGE,
    SECURITY_ALERT
}

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["recipientPhone"])]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipientPhone: String, // Customer phone or "OWNER"
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val referenceId: String? = null // Loan ID, Txn ID, or KYC ID
)
