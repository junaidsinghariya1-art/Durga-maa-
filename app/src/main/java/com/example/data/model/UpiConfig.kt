package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "upi_config")
data class UpiConfigEntity(
    @PrimaryKey
    val id: Int = 1, // Single central record
    val upiId: String, // e.g. "nr853950-1@okicici"
    val receiverName: String, // e.g. "Durga Maa Loan Financial Services"
    val bankName: String, // e.g. "ICICI Bank Ltd"
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "Owner"
)
