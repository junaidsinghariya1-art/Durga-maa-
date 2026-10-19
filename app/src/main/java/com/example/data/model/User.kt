package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    CUSTOMER,
    OWNER
}

enum class KycStatus {
    NOT_SUBMITTED,
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val phone: String, // 10-digit mobile number as primary identifier
    val fullName: String,
    val email: String,
    val role: UserRole = UserRole.CUSTOMER,
    val kycStatus: KycStatus = KycStatus.NOT_SUBMITTED,
    val createdAt: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false
)
