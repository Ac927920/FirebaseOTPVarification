package com.example.otpvarification

import com.google.firebase.auth.PhoneAuthCredential

interface FirebaseAuthenticator {
    fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential)
}
