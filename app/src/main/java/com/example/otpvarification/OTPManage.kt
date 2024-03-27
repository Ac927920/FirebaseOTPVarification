package com.example.otpvarification

import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


class OTPManage : AppCompatActivity() {
    private lateinit var otpInput: EditText
    private lateinit var submitButton: Button
    private lateinit var auth: FirebaseAuth
    private var storedVerificationId: String? = null
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    private val REQ_USER_CONSENT = 200

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otpmanage)

        otpInput = findViewById(R.id.OTP_v)
        submitButton = findViewById(R.id.submit_btn_v)

        auth = FirebaseAuth.getInstance()
        
        Firebase.auth.initializeRecaptchaConfig()

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId

                Log.d("DATA1","STORE ID $storedVerificationId")
                getOtp()

            }

//            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
//                val otp : String = credential.smsCode.toString()
//                Log.d("DATA1"," On Varification $credential OR $otp")
//                signInWithPhoneAuthCredential(credential)
//            }

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
                Log.d("DATA1"," On Varification $credential")
                val otp: String? = credential.smsCode
                if (otp != null) {
                    if (otp.isNotEmpty()) {
                        otpInput.setText(credential.smsCode!!)
                        Log.d("DATA1", "OTP : $otpInput " + " " + credential.smsCode)
                        verifyPhoneNumberWithCode(storedVerificationId, otp)
                    }else{
                        Log.d("DATA1"," OTP GET Error")
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.d("DATA1"," OTP OnFailed $e")
                // Handle verification failure
                Toast.makeText(this@OTPManage, "SMS verification code request failed", Toast.LENGTH_LONG).show()


            }
        }
        startPhoneNumberVerification(intent.getStringExtra("phoneNumber")!!)
        submitButton.setOnClickListener {
            val otpValue = otpInput.text.toString()
            if (otpValue.isNotEmpty()) {
                verifyPhoneNumberWithCode(storedVerificationId, otpValue)
            } else {
                Toast.makeText(this@OTPManage, "Enter OTP", Toast.LENGTH_LONG).show()
            }
        }
    }



    private fun extractOTPFromSMS(message: String): String? {
        val pattern = Pattern.compile("\\b\\d{6}\\b") // Pattern to match 6 digit OTP
        val matcher = pattern.matcher(message)
        return if (matcher.find()) {
            matcher.group() // Extracting the matched OTP
        } else {
            null // Return null if no match found
        }

    }
    private fun getOtp(){
        try {
            val cr: ContentResolver = applicationContext.contentResolver
            val st : Cursor? = cr.query(Telephony.Sms.CONTENT_URI,null,null,null,null)
            Log.d("DATA1","SMS $st")
            if (st!=null){
                if(st.moveToFirst()){
                    val body: String = st.getString(st.getColumnIndexOrThrow(Telephony.Sms.BODY))
                    Log.d("DATA1","SMS $body")
                    val smsOTP : String? = extractOTPFromSMS(body)
                    Log.d("DATA1","SMS $smsOTP")
                    otpInput.setText(smsOTP)
                    st.moveToNext()
                }
                st.close()
            }
        } catch (e : Exception){
            // Error
            Log.d("DATA1","Catch Error $e")
        }
    }


//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        Log.d("DATA1","onActivityResult $resultCode $requestCode")
//        if (requestCode==REQ_USER_CONSENT){
//            if (resultCode == RESULT_OK && data != null){
//                val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
//                otpCheck(message!!)
//            }
//        }
//    }
//    private fun otpCheck(message:String){
//        Log.d("DATA1","otpCheck $message")
//        val otpPatter = Pattern.compile(/* regex = */ "(|^)\\d{6}")
//        val matcher = otpPatter.matcher(message)
//        if (matcher.find()){
//            otpInput.setText(matcher.group(0))
//        }
//    }


    private fun startPhoneNumberVerification(phoneNumber: String?) {
        Log.d("DATA1","Phone Number $phoneNumber")
        val phoneNumberUtil = PhoneNumberUtil.getInstance()
        try {
            val phoneNumberProto = phoneNumberUtil.parse(phoneNumber, "IN")
            val formattedPhoneNumber = phoneNumberUtil.format(phoneNumberProto, PhoneNumberUtil.PhoneNumberFormat.E164)
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            // Handle parsing/formatting error
        }
    }


    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Log.d("DATA1","Credential $credential")
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(applicationContext, Dashboard::class.java))
                    val user = task.result?.user
                    Log.d("DATA1","User from Sign $user")
                } else {
                    // Handle sign-in failure
                }
            }
    }
}

