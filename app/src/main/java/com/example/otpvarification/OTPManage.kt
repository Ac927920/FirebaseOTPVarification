package com.example.otpvarification

import android.content.ContentResolver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
    private lateinit var sharedPreferences: SharedPreferences
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
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId

                Log.d("DATA1","STORE ID $storedVerificationId")
//                getOtp()

            }



            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential)
                Log.d("DATA1", " On Verification $credential")
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

    private fun getOtp() {
        try {
            val cr: ContentResolver = applicationContext.contentResolver
            val st: Cursor? = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null)
            Log.d("DATA1", "SMS $st")
            if (st != null) {
                if (st.moveToFirst()) {
                    val body: String = st.getString(st.getColumnIndexOrThrow(Telephony.Sms.BODY))
                    Log.d("DATA1", "SMS $body")
                    val smsOTP: String? = extractOTPFromSMS(body)
                    Log.d("DATA1", "SMS $smsOTP")
                    otpInput.setText(smsOTP)
                    st.moveToNext()
                }
                st.close()
            }
        } catch (e: Exception) {
            // Error
            Log.d("DATA1", "Catch Error $e")
        }
    }


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
                    savePhoneNumberVerified()
                    saveJwtToken()
                    val intent = Intent(this, Dashboard::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
    }

    private fun savePhoneNumberVerified() {
        // Save user verification status in shared preferences
        val editor = sharedPreferences.edit()
        editor.putBoolean("isPhoneNumberVerified", true)
        editor.apply()

    }


    private fun saveJwtToken() {
        // Save JWT token in shared preferences
        val user = Firebase.auth.currentUser
        user.let {
            val name = it?.displayName
            val email = it?.email
            val phone = it?.phoneNumber
            val photo = it?.photoUrl
            val uid = it?.uid
            val emailVerified = it?.isEmailVerified
        }
        try {
            user?.getIdToken(true)


                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userToken = task.result.token
                        val editor = sharedPreferences.edit()
                        editor.putString("jwtToken", userToken)
//                        editor.putString("jwtUserId",uId)
                        editor.apply()
                        Log.d("DATA1", "User Token $userToken")
                    } else {
                        // Handle error while retrieving token
                        Log.e("DATA1", "Error retrieving JWT token: ${task.exception}")
                    }
                }
            Log.d("DATA1", "User from Sign $user")
        } catch (e: Exception) {
            // Handle exception
            Log.e("DATA1", "Exception while retrieving JWT token: $e")
        }
    }


}

