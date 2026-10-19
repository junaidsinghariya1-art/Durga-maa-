package com.example.data.security

import java.security.MessageDigest
import kotlin.random.Random

object SecurityHelper {
    // Salted SHA-256 hash of the initial authorized owner credential "singhariya@8309"
    // Computed with salt "DurgaMaaLoan_Salt_2026"
    private const val SALT = "DurgaMaaLoan_Salt_2026"
    
    // Compute SHA-256 hex string
    fun hashCredential(input: String): String {
        val bytes = (input + SALT).toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    // Default owner hashed credential for "singhariya@8309"
    private val DEFAULT_OWNER_HASH: String = hashCredential("singhariya@8309")
    
    @Volatile
    private var currentOwnerHash: String = DEFAULT_OWNER_HASH

    // Owner rate limiting state
    private var failedAttempts = 0
    private var lockoutUntilMs: Long = 0L
    private const val MAX_FAILED_ATTEMPTS = 3
    private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds recoverable lockout

    @Synchronized
    fun resetLockoutStateForTesting() {
        failedAttempts = 0
        lockoutUntilMs = 0L
        currentOwnerHash = DEFAULT_OWNER_HASH
    }

    fun isValidIndianPhone(phone: String): Boolean {
        val trimmed = phone.trim()
        return trimmed.length == 10 && trimmed.all { it.isDigit() } && (trimmed.startsWith("6") || trimmed.startsWith("7") || trimmed.startsWith("8") || trimmed.startsWith("9"))
    }

    fun isValidUtr(utr: String): Boolean {
        val trimmed = utr.trim()
        // Standard Indian banking UTR is 12 digits, or 12-16 alphanumeric reference
        return trimmed.length in 10..18 && trimmed.all { it.isLetterOrDigit() }
    }

    data class OwnerAuthResult(
        val isSuccess: Boolean,
        val isLockedOut: Boolean,
        val remainingLockoutSeconds: Long = 0,
        val remainingAttempts: Int = 0,
        val message: String
    )

    @Synchronized
    fun verifyOwnerCredential(enteredCredential: String): OwnerAuthResult {
        val now = System.currentTimeMillis()
        if (now < lockoutUntilMs) {
            val remainingSec = ((lockoutUntilMs - now) / 1000).coerceAtLeast(1)
            return OwnerAuthResult(
                isSuccess = false,
                isLockedOut = true,
                remainingLockoutSeconds = remainingSec,
                message = "Too many failed attempts. Security lockout active for $remainingSec seconds."
            )
        }

        val hashed = hashCredential(enteredCredential.trim())
        if (hashed == currentOwnerHash) {
            // Reset attempts on successful auth
            failedAttempts = 0
            lockoutUntilMs = 0L
            return OwnerAuthResult(
                isSuccess = true,
                isLockedOut = false,
                message = "Owner authenticated successfully"
            )
        } else {
            failedAttempts++
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                lockoutUntilMs = now + LOCKOUT_DURATION_MS
                val remainingSec = LOCKOUT_DURATION_MS / 1000
                failedAttempts = 0 // Reset attempt counter for next cycle after lockout
                return OwnerAuthResult(
                    isSuccess = false,
                    isLockedOut = true,
                    remainingLockoutSeconds = remainingSec,
                    message = "Maximum attempts exceeded. Temporarily locked out for $remainingSec seconds."
                )
            } else {
                val left = MAX_FAILED_ATTEMPTS - failedAttempts
                return OwnerAuthResult(
                    isSuccess = false,
                    isLockedOut = false,
                    remainingAttempts = left,
                    message = "Invalid owner credential. $left attempt(s) remaining before temporary lockout."
                )
            }
        }
    }

    @Synchronized
    fun updateOwnerCredential(oldPass: String, newPass: String): Boolean {
        if (hashCredential(oldPass.trim()) == currentOwnerHash && newPass.trim().length >= 6) {
            currentOwnerHash = hashCredential(newPass.trim())
            return true
        }
        return false
    }

    // OTP Mock Engine for testing and demonstration
    data class OtpSession(
        val phone: String,
        val otp: String,
        val generatedAt: Long,
        val expiresAt: Long
    )

    private val otpCache = mutableMapOf<String, OtpSession>()

    @Synchronized
    fun generateOtpForPhone(phone: String): String {
        // Secure random 6-digit OTP
        val otpVal = (100000 + Random.nextInt(900000)).toString()
        val now = System.currentTimeMillis()
        otpCache[phone] = OtpSession(
            phone = phone,
            otp = otpVal,
            generatedAt = now,
            expiresAt = now + (5 * 60 * 1000L) // 5 minutes validity
        )
        return otpVal
    }

    @Synchronized
    fun verifyOtp(phone: String, enteredOtp: String): Boolean {
        val session = otpCache[phone] ?: return false
        val now = System.currentTimeMillis()
        if (now > session.expiresAt) {
            otpCache.remove(phone)
            return false
        }
        if (session.otp == enteredOtp.trim()) {
            otpCache.remove(phone)
            return true
        }
        return false
    }

    @Synchronized
    fun getActiveOtpForTesting(phone: String): String? {
        val session = otpCache[phone] ?: return null
        val now = System.currentTimeMillis()
        return if (now <= session.expiresAt) session.otp else null
    }
}
