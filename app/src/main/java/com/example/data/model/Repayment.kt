package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaymentStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    REJECTED
}

@Entity(
    tableName = "repayments",
    indices = [
        Index(value = ["utrNumber"], unique = true),
        Index(value = ["loanId"]),
        Index(value = ["customerPhone"])
    ]
)
data class RepaymentEntity(
    @PrimaryKey
    val id: String, // e.g. "TXN-74921"
    val loanId: String,
    val installmentId: String?,
    val customerPhone: String,
    val amount: Double,
    val utrNumber: String, // 12-digit UTR/Reference number
    val paymentMethod: String = "UPI",
    val receiptUri: String? = null,
    val status: PaymentStatus = PaymentStatus.PENDING_VERIFICATION,
    val submittedAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null,
    val verifiedBy: String? = null,
    val adminNotes: String? = null
)
