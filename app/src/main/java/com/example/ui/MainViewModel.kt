package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.DocumentType
import com.example.data.model.InstallmentEntity
import com.example.data.model.KycSubmissionEntity
import com.example.data.model.LoanEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.RepaymentEntity
import com.example.data.model.UpiConfigEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.LoanRepository
import com.example.data.security.SecurityHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    AUTH,
    CUSTOMER_DASHBOARD,
    REPAYMENT,
    KYC_SCREEN,
    OWNER_DASHBOARD,
    NOTIFICATIONS
}

data class AuthUiState(
    val selectedRole: UserRole = UserRole.CUSTOMER,
    val phoneInput: String = "9876543210", // Pre-fill with demo customer for rapid evaluation
    val otpInput: String = "",
    val otpSent: Boolean = false,
    val generatedOtpHint: String? = null,
    val ownerCredentialInput: String = "",
    val isOwnerLockedOut: Boolean = false,
    val lockoutRemainingSeconds: Long = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = LoanRepository(database)

    // Current User Session
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Current Active Navigation Screen
    private val _currentScreen = MutableStateFlow(AppScreen.AUTH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Auth UI State
    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    // Central UPI Gateway Flow (Reactivity across customer and owner)
    val upiConfig: StateFlow<UpiConfigEntity?> = repository.getUpiConfig()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Customer Loans
    val customerLoans: StateFlow<List<LoanEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null && user.role == UserRole.CUSTOMER) {
            repository.getCustomerLoans(user.phone)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Loan for Installment inspection & Repayment
    private val _selectedLoanId = MutableStateFlow<String?>(null)
    val selectedLoanId: StateFlow<String?> = _selectedLoanId.asStateFlow()

    val selectedLoanInstallments: StateFlow<List<InstallmentEntity>> = _selectedLoanId.flatMapLatest { loanId ->
        if (loanId != null) {
            repository.getInstallments(loanId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer Repayment History
    val customerRepayments: StateFlow<List<RepaymentEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null && user.role == UserRole.CUSTOMER) {
            repository.getRepaymentsForCustomer(user.phone)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer KYC State
    val customerKyc: StateFlow<KycSubmissionEntity?> = _currentUser.flatMapLatest { user ->
        if (user != null && user.role == UserRole.CUSTOMER) {
            repository.getKycForCustomer(user.phone)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Notifications
    val notifications: StateFlow<List<NotificationEntity>> = _currentUser.flatMapLatest { user ->
        val recipient = if (user?.role == UserRole.OWNER) "OWNER" else (user?.phone ?: "")
        if (recipient.isNotEmpty()) {
            repository.getNotifications(recipient)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Owner Data Streams
    val allLoans: StateFlow<List<LoanEntity>> = repository.getAllLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRepayments: StateFlow<List<RepaymentEntity>> = repository.getPendingRepayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRepayments: StateFlow<List<RepaymentEntity>> = repository.getAllRepayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPendingKyc: StateFlow<List<KycSubmissionEntity>> = repository.getAllPendingKyc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allKycList: StateFlow<List<KycSubmissionEntity>> = repository.getAllKyc()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Global Operation feedback
    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    fun selectScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectLoan(loanId: String) {
        _selectedLoanId.value = loanId
    }

    // Auth actions
    fun setAuthRole(role: UserRole) {
        _authUiState.value = _authUiState.value.copy(
            selectedRole = role,
            errorMessage = null,
            successMessage = null
        )
    }

    fun updatePhoneInput(phone: String) {
        _authUiState.value = _authUiState.value.copy(
            phoneInput = phone.filter { it.isDigit() }.take(10),
            errorMessage = null
        )
    }

    fun updateOtpInput(otp: String) {
        _authUiState.value = _authUiState.value.copy(
            otpInput = otp.filter { it.isDigit() }.take(6),
            errorMessage = null
        )
    }

    fun updateOwnerCredential(cred: String) {
        _authUiState.value = _authUiState.value.copy(
            ownerCredentialInput = cred,
            errorMessage = null
        )
    }

    fun requestOtp() {
        val phone = _authUiState.value.phoneInput
        viewModelScope.launch {
            _authUiState.value = _authUiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.requestOtp(phone)
            result.onSuccess { otp ->
                _authUiState.value = _authUiState.value.copy(
                    isLoading = false,
                    otpSent = true,
                    generatedOtpHint = otp, // Show OTP hint for seamless on-device testing
                    successMessage = "OTP sent to +91 $phone. Verification code: $otp"
                )
            }.onFailure { err ->
                _authUiState.value = _authUiState.value.copy(
                    isLoading = false,
                    errorMessage = err.message ?: "Failed to generate OTP."
                )
            }
        }
    }

    fun verifyCustomerOtp() {
        val phone = _authUiState.value.phoneInput
        val otp = _authUiState.value.otpInput
        viewModelScope.launch {
            _authUiState.value = _authUiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.verifyOtpAndLogin(phone, otp)
            result.onSuccess { user ->
                _currentUser.value = user
                _authUiState.value = AuthUiState() // Reset
                _currentScreen.value = AppScreen.CUSTOMER_DASHBOARD
                _snackMessage.value = "Welcome back, ${user.fullName}!"
            }.onFailure { err ->
                _authUiState.value = _authUiState.value.copy(
                    isLoading = false,
                    errorMessage = err.message ?: "Invalid OTP."
                )
            }
        }
    }

    fun loginOwner() {
        val enteredCred = _authUiState.value.ownerCredentialInput
        _authUiState.value = _authUiState.value.copy(isLoading = true, errorMessage = null)
        val authResult = SecurityHelper.verifyOwnerCredential(enteredCred)

        if (authResult.isSuccess) {
            val ownerUser = UserEntity(
                phone = "OWNER-ROOT",
                fullName = "Authorized Administrator",
                email = "admin@durgamaaloan.in",
                role = UserRole.OWNER,
                createdAt = System.currentTimeMillis()
            )
            _currentUser.value = ownerUser
            _authUiState.value = AuthUiState() // Reset
            _currentScreen.value = AppScreen.OWNER_DASHBOARD
            _snackMessage.value = "Owner administrator logged in securely."
        } else {
            _authUiState.value = _authUiState.value.copy(
                isLoading = false,
                isOwnerLockedOut = authResult.isLockedOut,
                lockoutRemainingSeconds = authResult.remainingLockoutSeconds,
                errorMessage = authResult.message
            )
        }
    }

    fun logout() {
        _currentUser.value = null
        _selectedLoanId.value = null
        _currentScreen.value = AppScreen.AUTH
        _authUiState.value = AuthUiState()
        _snackMessage.value = "Logged out securely."
    }

    // Customer Actions
    fun applyNewLoan(type: String, principal: Double, tenureMonths: Int, purpose: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.applyForLoan(user.phone, type, principal, tenureMonths, purpose)
            res.onSuccess { loan ->
                _snackMessage.value = "Loan application ${loan.id} for ₹$principal submitted successfully!"
            }.onFailure { err ->
                _snackMessage.value = err.message ?: "Error applying for loan."
            }
        }
    }

    fun submitRepayment(
        loanId: String,
        installmentId: String?,
        amount: Double,
        utrNumber: String,
        receiptUri: String?
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.submitRepayment(
                customerPhone = user.phone,
                loanId = loanId,
                installmentId = installmentId,
                amount = amount,
                utrNumber = utrNumber,
                paymentMethod = "UPI",
                receiptUri = receiptUri
            )
            res.onSuccess {
                _snackMessage.value = "Payment submitted! UTR ${it.utrNumber} is under administrator verification."
                _currentScreen.value = AppScreen.CUSTOMER_DASHBOARD
            }.onFailure { err ->
                _snackMessage.value = err.message ?: "Failed to submit repayment."
            }
        }
    }

    fun submitKyc(
        docType: DocumentType,
        idNumber: String,
        docUri: String?,
        selfieUri: String?,
        consentGiven: Boolean
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.submitKycWithConsent(
                phone = user.phone,
                docType = docType,
                idNumber = idNumber,
                documentUri = docUri,
                selfieUri = selfieUri,
                consentGiven = consentGiven
            )
            res.onSuccess {
                _snackMessage.value = "KYC documents submitted securely for compliance review."
            }.onFailure { err ->
                _snackMessage.value = err.message ?: "Failed to submit KYC."
            }
        }
    }

    fun withdrawKycConsent() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.withdrawConsentAndRequestDeletion(user.phone)
            res.onSuccess {
                _snackMessage.value = "Consent withdrawn and deletion request recorded."
            }.onFailure { err ->
                _snackMessage.value = err.message ?: "Error withdrawing consent."
            }
        }
    }

    // Owner Actions
    fun verifyRepayment(repaymentId: String, notes: String) {
        viewModelScope.launch {
            val res = repository.verifyRepayment(repaymentId, "Owner", notes)
            res.onSuccess {
                _snackMessage.value = "Repayment verified and customer balance settled."
            }.onFailure { err ->
                _snackMessage.value = err.message ?: "Error verifying repayment."
            }
        }
    }

    fun rejectRepayment(repaymentId: String, reason: String) {
        viewModelScope.launch {
            val res = repository.rejectRepayment(repaymentId, reason, "Owner")
            res.onSuccess {
                _snackMessage.value = "Repayment rejected and customer notified."
            }.onFailure { err ->
                _snackMessage.value = err.message ?: "Error rejecting repayment."
            }
        }
    }

    fun approveLoan(loanId: String) {
        viewModelScope.launch {
            val res = repository.approveLoan(loanId, "Owner")
            res.onSuccess {
                _snackMessage.value = "Loan $loanId approved and installment schedule activated."
            }.onFailure { err ->
                _snackMessage.value = err.message ?: "Error approving loan."
            }
        }
    }

    fun rejectLoan(loanId: String, reason: String) {
        viewModelScope.launch {
            val res = repository.rejectLoan(loanId, reason, "Owner")
            res.onSuccess {
                _snackMessage.value = "Loan $loanId rejected."
            }.onFailure { err ->
                _snackMessage.value = err.message ?: "Error rejecting loan."
            }
        }
    }

    fun updateUpiGateway(newUpiId: String, newReceiverName: String, newBankName: String) {
        viewModelScope.launch {
            val res = repository.updateUpiGateway(newUpiId, newReceiverName, newBankName, "Owner")
            res.onSuccess {
                _snackMessage.value = "Central UPI Gateway configuration updated across all sessions!"
            }.onFailure { err ->
                _snackMessage.value = err.message ?: "Error updating UPI configuration."
            }
        }
    }

    fun reviewKyc(phone: String, approved: Boolean, notes: String) {
        viewModelScope.launch {
            val res = repository.reviewKyc(phone, approved, notes, "Compliance Officer")
            res.onSuccess {
                _snackMessage.value = if (approved) "Customer KYC approved!" else "Customer KYC rejected."
            }.onFailure { err ->
                _snackMessage.value = err.message ?: "Error updating KYC."
            }
        }
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        val user = _currentUser.value ?: return
        val recipient = if (user.role == UserRole.OWNER) "OWNER" else user.phone
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(recipient)
        }
    }
}
