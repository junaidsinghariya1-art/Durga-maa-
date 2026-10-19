package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.AuditLogEntity
import com.example.data.model.DocumentType
import com.example.data.model.InstallmentEntity
import com.example.data.model.InstallmentStatus
import com.example.data.model.KycStatus
import com.example.data.model.KycSubmissionEntity
import com.example.data.model.LoanEntity
import com.example.data.model.LoanStatus
import com.example.data.model.NotificationEntity
import com.example.data.model.NotificationType
import com.example.data.model.PaymentStatus
import com.example.data.model.RepaymentEntity
import com.example.data.model.UpiConfigEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole

class Converters {
    @TypeConverter
    fun toUserRole(value: String): UserRole = enumValueOf(value)
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toKycStatus(value: String): KycStatus = enumValueOf(value)
    @TypeConverter
    fun fromKycStatus(value: KycStatus): String = value.name

    @TypeConverter
    fun toLoanStatus(value: String): LoanStatus = enumValueOf(value)
    @TypeConverter
    fun fromLoanStatus(value: LoanStatus): String = value.name

    @TypeConverter
    fun toInstallmentStatus(value: String): InstallmentStatus = enumValueOf(value)
    @TypeConverter
    fun fromInstallmentStatus(value: InstallmentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = enumValueOf(value)
    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toDocumentType(value: String): DocumentType = enumValueOf(value)
    @TypeConverter
    fun fromDocumentType(value: DocumentType): String = value.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType = enumValueOf(value)
    @TypeConverter
    fun fromNotificationType(value: NotificationType): String = value.name
}

@Database(
    entities = [
        UserEntity::class,
        LoanEntity::class,
        InstallmentEntity::class,
        RepaymentEntity::class,
        UpiConfigEntity::class,
        KycSubmissionEntity::class,
        AuditLogEntity::class,
        NotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun loanDao(): LoanDao
    abstract fun repaymentDao(): RepaymentDao
    abstract fun upiConfigDao(): UpiConfigDao
    abstract fun kycDao(): KycDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "durgamaaloan_database.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
