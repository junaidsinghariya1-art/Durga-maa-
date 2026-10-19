package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuditLogEntity
import com.example.data.model.InstallmentEntity
import com.example.data.model.KycStatus
import com.example.data.model.KycSubmissionEntity
import com.example.data.model.LoanEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PaymentStatus
import com.example.data.model.RepaymentEntity
import com.example.data.model.UpiConfigEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUser(phone: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET kycStatus = :status WHERE phone = :phone")
    suspend fun updateKycStatus(phone: String, status: KycStatus)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE customerPhone = :phone ORDER BY appliedDate DESC")
    fun getLoansForCustomer(phone: String): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :loanId LIMIT 1")
    suspend fun getLoanById(loanId: String): LoanEntity?

    @Query("SELECT * FROM loans ORDER BY appliedDate DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<InstallmentEntity>)

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    fun getInstallmentsForLoan(loanId: String): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    suspend fun getInstallmentsSync(loanId: String): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE id = :installmentId LIMIT 1")
    suspend fun getInstallmentById(installmentId: String): InstallmentEntity?

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)
}

@Dao
interface RepaymentDao {
    @Query("SELECT * FROM repayments WHERE customerPhone = :phone ORDER BY submittedAt DESC")
    fun getRepaymentsForCustomer(phone: String): Flow<List<RepaymentEntity>>

    @Query("SELECT * FROM repayments WHERE loanId = :loanId ORDER BY submittedAt DESC")
    fun getRepaymentsForLoan(loanId: String): Flow<List<RepaymentEntity>>

    @Query("SELECT * FROM repayments WHERE status = :status ORDER BY submittedAt ASC")
    fun getRepaymentsByStatus(status: PaymentStatus): Flow<List<RepaymentEntity>>

    @Query("SELECT * FROM repayments ORDER BY submittedAt DESC")
    fun getAllRepayments(): Flow<List<RepaymentEntity>>

    @Query("SELECT * FROM repayments WHERE utrNumber = :utr LIMIT 1")
    suspend fun findRepaymentByUtr(utr: String): RepaymentEntity?

    @Query("SELECT * FROM repayments WHERE id = :id LIMIT 1")
    suspend fun getRepaymentById(id: String): RepaymentEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRepayment(repayment: RepaymentEntity)

    @Update
    suspend fun updateRepayment(repayment: RepaymentEntity)
}

@Dao
interface UpiConfigDao {
    @Query("SELECT * FROM upi_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<UpiConfigEntity?>

    @Query("SELECT * FROM upi_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): UpiConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: UpiConfigEntity)
}

@Dao
interface KycDao {
    @Query("SELECT * FROM kyc_submissions WHERE customerPhone = :phone ORDER BY submittedAt DESC LIMIT 1")
    fun getKycForCustomer(phone: String): Flow<KycSubmissionEntity?>

    @Query("SELECT * FROM kyc_submissions WHERE customerPhone = :phone ORDER BY submittedAt DESC LIMIT 1")
    suspend fun getKycSync(phone: String): KycSubmissionEntity?

    @Query("SELECT * FROM kyc_submissions WHERE status = :status ORDER BY submittedAt ASC")
    fun getKycByStatus(status: KycStatus): Flow<List<KycSubmissionEntity>>

    @Query("SELECT * FROM kyc_submissions ORDER BY submittedAt DESC")
    fun getAllKyc(): Flow<List<KycSubmissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveKyc(kyc: KycSubmissionEntity)

    @Query("DELETE FROM kyc_submissions WHERE customerPhone = :phone")
    suspend fun deleteKycForCustomer(phone: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertLog(log: AuditLogEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE recipientPhone = :recipientPhone OR recipientPhone = 'ALL' ORDER BY timestamp DESC")
    fun getNotificationsForRecipient(recipientPhone: String): Flow<List<NotificationEntity>>

    @Insert
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientPhone = :recipientPhone")
    suspend fun markAllAsRead(recipientPhone: String)
}
