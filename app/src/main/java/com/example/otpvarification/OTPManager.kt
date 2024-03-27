package com.example.otpvarification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.phone.SmsRetrieverClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.concurrent.TimeUnit

class OTPManager(
    private val auth: FirebaseAuth,
    private val callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
    private val firebaseAuthManager: FirebaseAuthManager
) : OTPVerification {

    override fun startPhoneNumberVerification(
        phoneNumber: String?,
        context: Context,
    ) {
        val phoneNumberUtil = PhoneNumberUtil.getInstance()
        try {
            val phoneNumberProto = phoneNumberUtil.parse(phoneNumber, "IN")
            val formattedPhoneNumber =
                phoneNumberUtil.format(phoneNumberProto, PhoneNumberUtil.PhoneNumberFormat.E164)

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(context as Activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            // Handle parsing/formatting error
        }
    }

    override fun verifyPhoneNumberWithCode(verificationId: String?, code: String){
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
//        signInWithPhoneAuthCredential(credential)
        firebaseAuthManager.signInWithPhoneAuthCredential(credential)
    }
}



class FirebaseAuthManager(private val auth: FirebaseAuth) : FirebaseAuthenticator {



    override fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {

    }
}

//class SMSReceiverManager(private val context: Context) : SMSReceiver {
//    private var smsBroadcastReceiver: SmsBroadcastReceiver? = null
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun startSmartUserConsent() {
//        smsBroadcastReceiver = SmsBroadcastReceiver()
//        smsBroadcastReceiver?.smsBroadcastReceiverListener = object : SmsBroadcastReceiver.SmsBroadcastReceiverListener {
//            override fun onSuccess(intent: Intent?) {
//                // Handle success
//            }
//
//            override fun onFailure() {
//                // Handle failure
//            }
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            context.registerReceiver(smsBroadcastReceiver,
//                IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
//                Context.RECEIVER_NOT_EXPORTED)
//        }
//        val client: SmsRetrieverClient = SmsRetriever.getClient(context)
//        client.startSmsUserConsent(null)
//    }
//
//    override fun unregisterReceiver() {
//        smsBroadcastReceiver?.let {
//            context.unregisterReceiver(it)
//            smsBroadcastReceiver = null
//        }
//    }
//}
