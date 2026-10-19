package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
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
import com.example.data.security.SecurityHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.pow
import kotlin.math.roundToInt

class LoanRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val loanDao = database.loanDao()
    private val repaymentDao = database.repaymentDao()
    private val upiDao = database.upiConfigDao()
    private val kycDao = database.kycDao()
    private val auditDao = database.auditLogDao()
    private val notificationDao = database.notificationDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialData()
        }
    }

    private suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        // Seed Central UPI gateway with reference UPI ID if empty
        if (upiDao.getConfigSync() == null) {
            val defaultConfig = UpiConfigEntity(
                id = 1,
                upiId = "nr853950-1@okicici",
                receiverName = "Durga Maa Loan Financial Services",
                bankName = "ICICI Bank Ltd",
                updatedAt = System.currentTimeMillis(),
                updatedBy = "System Initialization"
            )
            upiDao.saveConfig(defaultConfig)
            auditDao.insertLog(
                AuditLogEntity(
                    actor = "System",
                    action = "INITIAL_SETUP",
                    details = "Initialized Central UPI Gateway: nr853950-1@okicici (ICICI Bank Ltd) for Durga Maa Loan"
                )
            )
        }

        // Check if demo customer exists
        val demoPhone = "9876543210"
        if (userDao.getUser(demoPhone) == null) {
            val demoUser = UserEntity(
                phone = demoPhone,
                fullName = "Rahul Sharma",
                email = "rahul.sharma@durgamaaloan.in",
                role = UserRole.CUSTOMER,
                kycStatus = KycStatus.APPROVED,
                createdAt = System.currentTimeMillis() - 90L * 24 * 3600 * 1000
            )
            userDao.insertUser(demoUser)

            val cal = Calendar.getInstance()
            
            // 1. Sea Money (₹ 7,000) - Due 07 Oct 2026
            cal.set(2026, Calendar.OCTOBER, 7, 12, 0, 0)
            val dueSea = cal.timeInMillis
            val loanSea = LoanEntity(
                id = "LN-SM-7000",
                customerPhone = demoPhone,
                loanType = "Short-term Credit",
                principalAmount = 7000.0,
                annualInterestRate = 12.0,
                tenureMonths = 1,
                totalRepaymentAmount = 7280.0,
                balanceRemaining = 4500.0,
                monthlyEmi = 7280.0,
                status = LoanStatus.ACTIVE,
                purpose = "Consumer purchase",
                providerName = "Sea Money",
                providerCode = "SM",
                progressPercent = 35,
                isSample = true,
                appliedDate = dueSea - 15L * 24 * 3600 * 1000,
                approvedDate = dueSea - 14L * 24 * 3600 * 1000,
                nextDueDate = dueSea
            )
            loanDao.insertLoan(loanSea)

            // 2. See Money (Featured: ₹ 5,000 / Repay ₹ 5,200) - Due 12 Oct 2026
            cal.set(2026, Calendar.OCTOBER, 12, 12, 0, 0)
            val dueSee = cal.timeInMillis
            val loanSee = LoanEntity(
                id = "LN-SE-5000",
                customerPhone = demoPhone,
                loanType = "Personal Credit",
                principalAmount = 5000.0,
                annualInterestRate = 12.0,
                tenureMonths = 1,
                totalRepaymentAmount = 5200.0,
                balanceRemaining = 5000.0,
                monthlyEmi = 5200.0,
                status = LoanStatus.ACTIVE,
                purpose = "Personal expense buffer",
                providerName = "See Money",
                providerCode = "SE",
                progressPercent = 45,
                isSample = true,
                appliedDate = dueSee - 12L * 24 * 3600 * 1000,
                approvedDate = dueSee - 11L * 24 * 3600 * 1000,
                nextDueDate = dueSee
            )
            loanDao.insertLoan(loanSee)

            // 3. Hello Cash (₹ 6,000) - Due 15 Oct 2026
            cal.set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
            val dueHello = cal.timeInMillis
            val loanHello = LoanEntity(
                id = "LN-HC-6000",
                customerPhone = demoPhone,
                loanType = "Instant Cash",
                principalAmount = 6000.0,
                annualInterestRate = 12.0,
                tenureMonths = 1,
                totalRepaymentAmount = 6300.0,
                balanceRemaining = 4000.0,
                monthlyEmi = 6300.0,
                status = LoanStatus.ACTIVE,
                purpose = "Utility bills payment",
                providerName = "Hello Cash",
                providerCode = "HC",
                progressPercent = 20,
                isSample = true,
                appliedDate = dueHello - 10L * 24 * 3600 * 1000,
                approvedDate = dueHello - 9L * 24 * 3600 * 1000,
                nextDueDate = dueHello
            )
            loanDao.insertLoan(loanHello)

            // 4. One Cash (₹ 4,000) - Due 01 Oct 2026 (Settled / Closed)
            cal.set(2026, Calendar.OCTOBER, 1, 12, 0, 0)
            val dueOne = cal.timeInMillis
            val loanOne = LoanEntity(
                id = "LN-OC-4000",
                customerPhone = demoPhone,
                loanType = "Micro Loan",
                principalAmount = 4000.0,
                annualInterestRate = 10.0,
                tenureMonths = 1,
                totalRepaymentAmount = 4000.0,
                balanceRemaining = 0.0,
                monthlyEmi = 4000.0,
                status = LoanStatus.CLOSED,
                purpose = "Travel tickets",
                providerName = "One Cash",
                providerCode = "OC",
                progressPercent = 100,
                isSample = true,
                appliedDate = dueOne - 20L * 24 * 3600 * 1000,
                approvedDate = dueOne - 19L * 24 * 3600 * 1000,
                closedDate = dueOne,
                nextDueDate = null
            )
            loanDao.insertLoan(loanOne)

            val now = System.currentTimeMillis()

            // Seed Initial KYC for demo customer
            val demoKyc = KycSubmissionEntity(
                id = "KYC-DEMO-1",
                customerPhone = demoPhone,
                documentType = DocumentType.AADHAAR_CARD,
                idNumber = "9823 **** 1942",
                documentUri = "content://media/external/images/media/kyc_proof",
                selfieUri = "content://media/external/images/media/selfie_proof",
                consentGiven = true,
                consentTimestamp = now - 95L * 24 * 3600 * 1000,
                status = KycStatus.APPROVED,
                reviewedAt = now - 94L * 24 * 3600 * 1000,
                reviewedBy = "Compliance Officer",
                reviewNotes = "Verified against national registry."
            )
            kycDao.saveKyc(demoKyc)

            // Seed notifications
            notificationDao.insertNotification(
                NotificationEntity(
                    recipientPhone = demoPhone,
                    title = "Loan Application Approved",
                    message = "Your Personal Credit loan from See Money for ₹5,000 has been sanctioned and disbursed.",
                    type = NotificationType.LOAN_APPROVED,
                    timestamp = now - 118L * 24 * 3600 * 1000,
                    isRead = true,
                    referenceId = loanSee.id
                )
            )
        }
    }

    // Mathematical EMI calculation
    private fun calculateEmi(principal: Double, annualRate: Double, tenureMonths: Int): Double {
        val monthlyRate = (annualRate / 12.0) / 100.0
        val factor = (1 + monthlyRate).pow(tenureMonths.toDouble())
        val emi = principal * monthlyRate * factor / (factor - 1)
        return (emi * 100.0).roundToInt() / 100.0
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    // Central UPI Gateway Flow
    fun getUpiConfig(): Flow<UpiConfigEntity?> = upiDao.getConfig()

    suspend fun updateUpiGateway(
        newUpiId: String,
        newReceiverName: String,
        newBankName: String,
        updatedBy: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanUpi = newUpiId.trim()
        val cleanReceiver = newReceiverName.trim()
        val cleanBank = newBankName.trim()

        if (!cleanUpi.contains("@") || cleanUpi.length < 5) {
            return@withContext Result.failure(IllegalArgumentException("Invalid UPI ID format. Must contain '@' (e.g. name@bank)"))
        }
        if (cleanReceiver.isEmpty() || cleanBank.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Receiver name and bank name cannot be empty."))
        }

        val config = UpiConfigEntity(
            id = 1,
            upiId = cleanUpi,
            receiverName = cleanReceiver,
            bankName = cleanBank,
            updatedAt = System.currentTimeMillis(),
            updatedBy = updatedBy
        )
        upiDao.saveConfig(config)

        // Audit log without sensitive passwords
        auditDao.insertLog(
            AuditLogEntity(
                actor = updatedBy,
                action = "UPI_GATEWAY_CONFIG_UPDATED",
                details = "Updated Central UPI to $cleanUpi ($cleanBank) for receiver $cleanReceiver"
            )
        )

        // Broadcast notification to all customers
        notificationDao.insertNotification(
            NotificationEntity(
                recipientPhone = "ALL",
                title = "Payment Gateway Updated",
                message = "The Central UPI repayment address has been updated to $cleanUpi. Please verify before paying.",
                type = NotificationType.UPI_CONFIG_CHANGE,
                timestamp = System.currentTimeMillis()
            )
        )

        Result.success(Unit)
    }

    // Customer Auth Flows
    suspend fun requestOtp(phone: String): Result<String> = withContext(Dispatchers.IO) {
        if (!SecurityHelper.isValidIndianPhone(phone)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number starting with 6, 7, 8, or 9."))
        }
        val otp = SecurityHelper.generateOtpForPhone(phone)
        // Log generation in audit trail
        auditDao.insertLog(
            AuditLogEntity(
                actor = phone,
                action = "OTP_REQUESTED",
                details = "Generated secure 6-digit OTP session for mobile $phone"
            )
        )
        Result.success(otp)
    }

    suspend fun verifyOtpAndLogin(
        phone: String,
        otp: String,
        fullName: String = ""
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        if (!SecurityHelper.verifyOtp(phone, otp)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid or expired OTP. Please check and try again."))
        }

        var user = userDao.getUser(phone)
        if (user == null) {
            user = UserEntity(
                phone = phone,
                fullName = if (fullName.isNotBlank()) fullName else "Customer $phone",
                email = "$phone@durgamaaloan.in",
                role = UserRole.CUSTOMER,
                kycStatus = KycStatus.NOT_SUBMITTED,
                createdAt = System.currentTimeMillis()
            )
            userDao.insertUser(user)
        }

        auditDao.insertLog(
            AuditLogEntity(
                actor = phone,
                action = "CUSTOMER_LOGIN_SUCCESS",
                details = "Authenticated via secure 6-digit OTP verification."
            )
        )

        Result.success(user)
    }

    suspend fun getUserProfile(phone: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUser(phone)
    }

    // Loan Management
    fun getCustomerLoans(phone: String): Flow<List<LoanEntity>> = loanDao.getLoansForCustomer(phone)

    fun getAllLoans(): Flow<List<LoanEntity>> = loanDao.getAllLoans()

    fun getInstallments(loanId: String): Flow<List<InstallmentEntity>> = loanDao.getInstallmentsForLoan(loanId)

    suspend fun applyForLoan(
        customerPhone: String,
        loanType: String,
        principalAmount: Double,
        tenureMonths: Int,
        purpose: String
    ): Result<LoanEntity> = withContext(Dispatchers.IO) {
        if (principalAmount < 5000) {
            return@withContext Result.failure(IllegalArgumentException("Minimum loan amount is ₹5,000."))
        }
        if (tenureMonths !in 3..60) {
            return@withContext Result.failure(IllegalArgumentException("Tenure must be between 3 and 60 months."))
        }

        // Tiered interest rate based on loan type
        val interestRate = when (loanType) {
            "Personal Loan" -> 12.5
            "Business Loan" -> 14.0
            "Education Loan" -> 9.5
            "Vehicle Loan" -> 10.5
            else -> 12.0
        }

        val emi = calculateEmi(principalAmount, interestRate, tenureMonths)
        val totalRepayment = emi * tenureMonths
        val loanId = "LN-" + (10000 + (Math.random() * 89999).toInt())

        val newLoan = LoanEntity(
            id = loanId,
            customerPhone = customerPhone,
            loanType = loanType,
            principalAmount = principalAmount,
            annualInterestRate = interestRate,
            tenureMonths = tenureMonths,
            totalRepaymentAmount = totalRepayment,
            balanceRemaining = totalRepayment,
            monthlyEmi = emi,
            status = LoanStatus.PENDING_APPROVAL,
            purpose = purpose,
            appliedDate = System.currentTimeMillis()
        )

        loanDao.insertLoan(newLoan)

        auditDao.insertLog(
            AuditLogEntity(
                actor = customerPhone,
                action = "LOAN_APPLICATION_SUBMITTED",
                details = "Applied for $loanType ($loanId) for ₹$principalAmount with tenure $tenureMonths months"
            )
        )

        notificationDao.insertNotification(
            NotificationEntity(
                recipientPhone = "OWNER",
                title = "New Loan Application",
                message = "Customer $customerPhone submitted a new $loanType application ($loanId) for ₹$principalAmount.",
                type = NotificationType.LOAN_APPROVED,
                timestamp = System.currentTimeMillis(),
                referenceId = loanId
            )
        )

        Result.success(newLoan)
    }

    suspend fun approveLoan(loanId: String, approvedBy: String): Result<Unit> = withContext(Dispatchers.IO) {
        val loan = loanDao.getLoanById(loanId) ?: return@withContext Result.failure(NoSuchElementException("Loan not found."))
        if (loan.status != LoanStatus.PENDING_APPROVAL) {
            return@withContext Result.failure(IllegalStateException("Loan is not in pending approval state."))
        }

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.add(Calendar.MONTH, 1)
        val firstDueDate = cal.timeInMillis

        val updatedLoan = loan.copy(
            status = LoanStatus.ACTIVE,
            approvedDate = now,
            nextDueDate = firstDueDate
        )
        loanDao.updateLoan(updatedLoan)

        // Generate Installments schedule
        val installments = mutableListOf<InstallmentEntity>()
        val emi = loan.monthlyEmi
        for (i in 1..loan.tenureMonths) {
            cal.timeInMillis = now
            cal.add(Calendar.MONTH, i)
            installments.add(
                InstallmentEntity(
                    id = "INS-$loanId-$i",
                    loanId = loanId,
                    installmentNumber = i,
                    dueDate = cal.timeInMillis,
                    emiAmount = emi,
                    principalComponent = (emi * 0.85).round(2),
                    interestComponent = (emi * 0.15).round(2),
                    status = InstallmentStatus.UPCOMING
                )
            )
        }
        loanDao.insertInstallments(installments)

        auditDao.insertLog(
            AuditLogEntity(
                actor = approvedBy,
                action = "LOAN_APPROVED",
                details = "Approved loan $loanId for customer ${loan.customerPhone}. Generated ${loan.tenureMonths} installments."
            )
        )

        notificationDao.insertNotification(
            NotificationEntity(
                recipientPhone = loan.customerPhone,
                title = "Loan Approved & Disbursed!",
                message = "Congratulations! Your loan $loanId for ₹${loan.principalAmount} has been approved.",
                type = NotificationType.LOAN_APPROVED,
                timestamp = now,
                referenceId = loanId
            )
        )

        Result.success(Unit)
    }

    suspend fun rejectLoan(loanId: String, reason: String, rejectedBy: String): Result<Unit> = withContext(Dispatchers.IO) {
        val loan = loanDao.getLoanById(loanId) ?: return@withContext Result.failure(NoSuchElementException("Loan not found."))
        val updated = loan.copy(status = LoanStatus.REJECTED)
        loanDao.updateLoan(updated)

        auditDao.insertLog(
            AuditLogEntity(
                actor = rejectedBy,
                action = "LOAN_REJECTED",
                details = "Rejected loan $loanId: $reason"
            )
        )

        notificationDao.insertNotification(
            NotificationEntity(
                recipientPhone = loan.customerPhone,
                title = "Loan Application Update",
                message = "Your loan application $loanId could not be approved at this time: $reason",
                type = NotificationType.LOAN_REJECTED,
                timestamp = System.currentTimeMillis(),
                referenceId = loanId
            )
        )

        Result.success(Unit)
    }

    // Repayments & Verification
    fun getRepaymentsForCustomer(phone: String): Flow<List<RepaymentEntity>> = repaymentDao.getRepaymentsForCustomer(phone)

    fun getPendingRepayments(): Flow<List<RepaymentEntity>> = repaymentDao.getRepaymentsByStatus(PaymentStatus.PENDING_VERIFICATION)

    fun getAllRepayments(): Flow<List<RepaymentEntity>> = repaymentDao.getAllRepayments()

    suspend fun submitRepayment(
        customerPhone: String,
        loanId: String,
        installmentId: String?,
        amount: Double,
        utrNumber: String,
        paymentMethod: String,
        receiptUri: String?
    ): Result<RepaymentEntity> = withContext(Dispatchers.IO) {
        val cleanUtr = utrNumber.trim().uppercase()
        if (!SecurityHelper.isValidUtr(cleanUtr)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 10-18 character alphanumeric UTR/Transaction reference number."))
        }
        if (amount <= 0) {
            return@withContext Result.failure(IllegalArgumentException("Repayment amount must be greater than zero."))
        }

        // Duplicate UTR prevention: Check database for any existing transaction with this UTR
        val existing = repaymentDao.findRepaymentByUtr(cleanUtr)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("Duplicate UTR detected! Transaction reference '$cleanUtr' has already been recorded (Status: ${existing.status}). Please verify your receipt."))
        }

        val txnId = "TXN-" + (100000 + (Math.random() * 899999).toInt())
        val repayment = RepaymentEntity(
            id = txnId,
            loanId = loanId,
            installmentId = installmentId,
            customerPhone = customerPhone,
            amount = amount,
            utrNumber = cleanUtr,
            paymentMethod = paymentMethod,
            receiptUri = receiptUri,
            status = PaymentStatus.PENDING_VERIFICATION,
            submittedAt = System.currentTimeMillis()
        )

        repaymentDao.insertRepayment(repayment)

        auditDao.insertLog(
            AuditLogEntity(
                actor = customerPhone,
                action = "REPAYMENT_SUBMITTED",
                details = "Submitted payment $txnId with UTR $cleanUtr for amount ₹$amount on loan $loanId. Pending admin review."
            )
        )

        // Notify Owner of new repayment to verify
        notificationDao.insertNotification(
            NotificationEntity(
                recipientPhone = "OWNER",
                title = "Payment Verification Required",
                message = "New repayment $txnId of ₹$amount with UTR $cleanUtr submitted by customer $customerPhone.",
                type = NotificationType.PAYMENT_SUBMITTED,
                timestamp = System.currentTimeMillis(),
                referenceId = txnId
            )
        )

        // Notify Customer of submission
        notificationDao.insertNotification(
            NotificationEntity(
                recipientPhone = customerPhone,
                title = "Repayment Submitted",
                message = "Your payment of ₹$amount with UTR $cleanUtr has been submitted and is undergoing verification by our team.",
                type = NotificationType.PAYMENT_SUBMITTED,
                timestamp = System.currentTimeMillis(),
                referenceId = txnId
            )
        )

        Result.success(repayment)
    }

    suspend fun verifyRepayment(
        repaymentId: String,
        verifiedBy: String,
        notes: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val repayment = repaymentDao.getRepaymentById(repaymentId) ?: return@withContext Result.failure(NoSuchElementException("Repayment record not found."))
        if (repayment.status != PaymentStatus.PENDING_VERIFICATION) {
            return@withContext Result.failure(IllegalStateException("Repayment is already ${repayment.status}."))
        }

        val now = System.currentTimeMillis()
        val updatedRepayment = repayment.copy(
            status = PaymentStatus.VERIFIED,
            verifiedAt = now,
            verifiedBy = verifiedBy,
            adminNotes = notes
        )
        repaymentDao.updateRepayment(updatedRepayment)

        // Deduct from Loan balance
        val loan = loanDao.getLoanById(repayment.loanId)
        if (loan != null) {
            val newBalance = (loan.balanceRemaining - repayment.amount).coerceAtLeast(0.0)
            val isClosed = newBalance <= 0.0

            val updatedLoan = loan.copy(
                balanceRemaining = newBalance,
                status = if (isClosed) LoanStatus.CLOSED else loan.status,
                closedDate = if (isClosed) now else loan.closedDate
            )
            loanDao.updateLoan(updatedLoan)

            // Mark installment as paid if tied to one
            if (repayment.installmentId != null) {
                val installment = loanDao.getInstallmentById(repayment.installmentId)
                if (installment != null) {
                    loanDao.updateInstallment(
                        installment.copy(
                            status = InstallmentStatus.PAID,
                            paidDate = now,
                            transactionRef = repayment.utrNumber
                        )
                    )
                }
            } else {
                // Find next unpaid installment and mark paid
                val allInstallments = loanDao.getInstallmentsSync(loan.id)
                val nextUnpaid = allInstallments.firstOrNull { it.status != InstallmentStatus.PAID }
                if (nextUnpaid != null) {
                    loanDao.updateInstallment(
                        nextUnpaid.copy(
                            status = InstallmentStatus.PAID,
                            paidDate = now,
                            transactionRef = repayment.utrNumber
                        )
                    )
                }
            }

            auditDao.insertLog(
                AuditLogEntity(
                    actor = verifiedBy,
                    action = "PAYMENT_VERIFIED",
                    details = "Verified payment ${repayment.id} (₹${repayment.amount}, UTR ${repayment.utrNumber}) for loan ${loan.id}. Remaining balance: ₹$newBalance. Closed: $isClosed."
                )
            )

            // Notify Customer of Verified Settlement
            notificationDao.insertNotification(
                NotificationEntity(
                    recipientPhone = repayment.customerPhone,
                    title = if (isClosed) "Loan Fully Settled & Closed!" else "Payment Verified",
                    message = if (isClosed) {
                        "Congratulations! Your repayment of ₹${repayment.amount} has been verified and loan ${loan.id} is now fully paid off and closed."
                    } else {
                        "Your payment of ₹${repayment.amount} (UTR ${repayment.utrNumber}) has been verified. Remaining loan balance: ₹$newBalance."
                    },
                    type = if (isClosed) NotificationType.LOAN_CLOSED else NotificationType.PAYMENT_VERIFIED,
                    timestamp = now,
                    referenceId = repayment.id
                )
            )
        }

        Result.success(Unit)
    }

    suspend fun rejectRepayment(
        repaymentId: String,
        reason: String,
        rejectedBy: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val repayment = repaymentDao.getRepaymentById(repaymentId) ?: return@withContext Result.failure(NoSuchElementException("Repayment record not found."))
        val now = System.currentTimeMillis()
        val updated = repayment.copy(
            status = PaymentStatus.REJECTED,
            verifiedAt = now,
            verifiedBy = rejectedBy,
            adminNotes = reason
        )
        repaymentDao.updateRepayment(updated)

        auditDao.insertLog(
            AuditLogEntity(
                actor = rejectedBy,
                action = "PAYMENT_REJECTED",
                details = "Rejected payment ${repayment.id} (UTR ${repayment.utrNumber}): $reason"
            )
        )

        notificationDao.insertNotification(
            NotificationEntity(
                recipientPhone = repayment.customerPhone,
                title = "Payment Verification Unsuccessful",
                message = "Payment with UTR ${repayment.utrNumber} could not be verified: $reason. Please verify with your bank or resubmit with a clear receipt.",
                type = NotificationType.PAYMENT_REJECTED,
                timestamp = now,
                referenceId = repayment.id
            )
        )

        Result.success(Unit)
    }

    // Consent-based KYC operations
    fun getKycForCustomer(phone: String): Flow<KycSubmissionEntity?> = kycDao.getKycForCustomer(phone)

    fun getAllPendingKyc(): Flow<List<KycSubmissionEntity>> = kycDao.getKycByStatus(KycStatus.PENDING_REVIEW)

    fun getAllKyc(): Flow<List<KycSubmissionEntity>> = kycDao.getAllKyc()

    suspend fun submitKycWithConsent(
        phone: String,
        docType: DocumentType,
        idNumber: String,
        documentUri: String?,
        selfieUri: String?,
        consentGiven: Boolean
    ): Result<KycSubmissionEntity> = withContext(Dispatchers.IO) {
        if (!consentGiven) {
            return@withContext Result.failure(IllegalStateException("Explicit consent is required by law before KYC details can be collected."))
        }
        if (idNumber.trim().length < 4) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid identification document number."))
        }

        val kycId = "KYC-" + (10000 + (Math.random() * 89999).toInt())
        val submission = KycSubmissionEntity(
            id = kycId,
            customerPhone = phone,
            documentType = docType,
            idNumber = idNumber.trim(),
            documentUri = documentUri,
            selfieUri = selfieUri,
            consentGiven = true,
            consentTimestamp = System.currentTimeMillis(),
            status = KycStatus.PENDING_REVIEW,
            submittedAt = System.currentTimeMillis()
        )

        kycDao.saveKyc(submission)
        userDao.updateKycStatus(phone, KycStatus.PENDING_REVIEW)

        auditDao.insertLog(
            AuditLogEntity(
                actor = phone,
                action = "KYC_CONSENT_AND_SUBMISSION",
                details = "Recorded explicit consent for statutory verification under Privacy Policy v2.4-2026. Submitted $docType."
            )
        )

        notificationDao.insertNotification(
            NotificationEntity(
                recipientPhone = phone,
                title = "KYC Submitted",
                message = "Your identity verification documents have been received and are undergoing review by our compliance team.",
                type = NotificationType.KYC_UPDATE,
                timestamp = System.currentTimeMillis(),
                referenceId = kycId
            )
        )

        Result.success(submission)
    }

    suspend fun reviewKyc(
        phone: String,
        approved: Boolean,
        notes: String,
        reviewer: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val kyc = kycDao.getKycSync(phone) ?: return@withContext Result.failure(NoSuchElementException("KYC submission not found."))
        val newStatus = if (approved) KycStatus.APPROVED else KycStatus.REJECTED
        val now = System.currentTimeMillis()

        val updated = kyc.copy(
            status = newStatus,
            reviewedAt = now,
            reviewedBy = reviewer,
            reviewNotes = notes
        )
        kycDao.saveKyc(updated)
        userDao.updateKycStatus(phone, newStatus)

        auditDao.insertLog(
            AuditLogEntity(
                actor = reviewer,
                action = if (approved) "KYC_APPROVED" else "KYC_REJECTED",
                details = "KYC review for customer $phone: ${if (approved) "Approved" else "Rejected"} - $notes"
            )
        )

        notificationDao.insertNotification(
            NotificationEntity(
                recipientPhone = phone,
                title = if (approved) "KYC Verified" else "KYC Needs Attention",
                message = if (approved) {
                    "Your identity documents have been approved by compliance."
                } else {
                    "Your KYC submission was rejected: $notes. Please resubmit."
                },
                type = NotificationType.KYC_UPDATE,
                timestamp = now,
                referenceId = kyc.id
            )
        )

        Result.success(Unit)
    }

    suspend fun withdrawConsentAndRequestDeletion(phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        val kyc = kycDao.getKycSync(phone) ?: return@withContext Result.failure(NoSuchElementException("No KYC records found."))
        val now = System.currentTimeMillis()
        val updated = kyc.copy(
            consentWithdrawn = true,
            consentWithdrawnAt = now,
            status = KycStatus.NOT_SUBMITTED
        )
        kycDao.saveKyc(updated)
        userDao.updateKycStatus(phone, KycStatus.NOT_SUBMITTED)

        auditDao.insertLog(
            AuditLogEntity(
                actor = phone,
                action = "KYC_CONSENT_WITHDRAWN",
                details = "Customer withdrew KYC consent and requested data erasure under data protection policies."
            )
        )

        notificationDao.insertNotification(
            NotificationEntity(
                recipientPhone = phone,
                title = "Consent Withdrawn",
                message = "Your KYC consent withdrawal has been recorded. Personal verification data scheduled for purge in accordance with statutory retention limits.",
                type = NotificationType.SECURITY_ALERT,
                timestamp = now
            )
        )

        Result.success(Unit)
    }

    // Notifications
    fun getNotifications(recipientPhone: String): Flow<List<NotificationEntity>> = notificationDao.getNotificationsForRecipient(recipientPhone)

    suspend fun markNotificationAsRead(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead(recipientPhone: String) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(recipientPhone)
    }

    // Audit logs for Owner inspection
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>> = auditDao.getAllLogs()

    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
}
