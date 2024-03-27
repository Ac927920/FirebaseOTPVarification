package com.example.otpvarification

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential

interface OTPVerification {
    fun startPhoneNumberVerification(phoneNumber: String?,context: Context)
    fun verifyPhoneNumberWithCode(verificationId: String?, code: String)
}