package com.example

import com.example.data.security.SecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DurgaMaaLoanSecurityTest {

    @Before
    fun setUp() {
        SecurityHelper.resetLockoutStateForTesting()
    }

    @Test
    fun testValidIndianPhoneNumbers() {
        assertTrue(SecurityHelper.isValidIndianPhone("9876543210"))
        assertTrue(SecurityHelper.isValidIndianPhone("8123456789"))
        assertTrue(SecurityHelper.isValidIndianPhone("7012345678"))
        assertTrue(SecurityHelper.isValidIndianPhone("6987654321"))

        // Invalid cases
        assertFalse(SecurityHelper.isValidIndianPhone("1234567890")) // Doesn't start with 6-9
        assertFalse(SecurityHelper.isValidIndianPhone("987654321")) // Only 9 digits
        assertFalse(SecurityHelper.isValidIndianPhone("98765432101")) // 11 digits
        assertFalse(SecurityHelper.isValidIndianPhone("98765abcde")) // Non-digits
    }

    @Test
    fun testValidUtrNumbers() {
        assertTrue(SecurityHelper.isValidUtr("410294857201")) // Standard 12-digit UTR
        assertTrue(SecurityHelper.isValidUtr("AXIS9482018471")) // Alphanumeric
        assertFalse(SecurityHelper.isValidUtr("12345")) // Too short (< 10)
        assertFalse(SecurityHelper.isValidUtr("12345678901234567890")) // Too long (> 18)
    }

    @Test
    fun testOwnerCredentialVerificationSuccess() {
        val result = SecurityHelper.verifyOwnerCredential("singhariya@8309")
        assertTrue(result.isSuccess)
        assertFalse(result.isLockedOut)
    }

    @Test
    fun testOwnerRateLimitingAndLockout() {
        // Attempt with wrong credential
        val fail1 = SecurityHelper.verifyOwnerCredential("wrong_password_1")
        assertFalse(fail1.isSuccess)

        val fail2 = SecurityHelper.verifyOwnerCredential("wrong_password_2")
        assertFalse(fail2.isSuccess)

        val fail3 = SecurityHelper.verifyOwnerCredential("wrong_password_3")
        assertFalse(fail3.isSuccess)
        assertTrue(fail3.isLockedOut)
        assertTrue(fail3.remainingLockoutSeconds > 0)

        // Subsequent attempt while locked out must be blocked
        val blockedAttempt = SecurityHelper.verifyOwnerCredential("singhariya@8309")
        assertFalse(blockedAttempt.isSuccess)
        assertTrue(blockedAttempt.isLockedOut)
    }

    @Test
    fun testOtpGenerationAndVerification() {
        val testPhone = "9876543210"
        val otp = SecurityHelper.generateOtpForPhone(testPhone)
        assertNotNull(otp)
        assertEquals(6, otp.length)
        assertTrue(otp.all { it.isDigit() })

        // Incorrect OTP must fail
        assertFalse(SecurityHelper.verifyOtp(testPhone, "000000"))

        // Correct OTP must succeed
        assertTrue(SecurityHelper.verifyOtp(testPhone, otp))

        // OTP is single-use, subsequent verification must fail
        assertFalse(SecurityHelper.verifyOtp(testPhone, otp))
    }
}
