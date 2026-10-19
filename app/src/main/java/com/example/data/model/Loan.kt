package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class LoanStatus {
    PENDING_APPROVAL,
    ACTIVE,
    CLOSED,
    REJECTED
}

enum class InstallmentStatus {
    UPCOMING,
    PAID,
    OVERDUE
}

@Entity(
    tableName = "loans",
    indices = [Index(value = ["customerPhone"])]
)
data class LoanEntity(
    @PrimaryKey
    val id: String, // e.g. "LN-10842"
    val customerPhone: String,
    val loanType: String, // e.g. "Personal Loan", "Business Credit", "Vehicle Loan"
    val principalAmount: Double,
    val annualInterestRate: Double, // e.g. 12.5%
    val tenureMonths: Int,
    val totalRepaymentAmount: Double,
    val balanceRemaining: Double,
    val monthlyEmi: Double,
    val status: LoanStatus = LoanStatus.PENDING_APPROVAL,
    val purpose: String,
    val providerName: String = "See Money", // e.g. "Sea Money", "See Money", "Hello Cash", "One Cash"
    val providerCode: String = "SE", // "SM", "SE", "HC", "OC"
    val progressPercent: Int = 45,
    val isSample: Boolean = false,
    val appliedDate: Long = System.currentTimeMillis(),
    val approvedDate: Long? = null,
    val closedDate: Long? = null,
    val nextDueDate: Long? = null
)

@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["loanId"])]
)
data class InstallmentEntity(
    @PrimaryKey
    val id: String, // e.g. "INS-LN-10842-1"
    val loanId: String,
    val installmentNumber: Int,
    val dueDate: Long,
    val emiAmount: Double,
    val principalComponent: Double,
    val interestComponent: Double,
    val status: InstallmentStatus = InstallmentStatus.UPCOMING,
    val penaltyAmount: Double = 0.0,
    val paidDate: Long? = null,
    val transactionRef: String? = null
)
