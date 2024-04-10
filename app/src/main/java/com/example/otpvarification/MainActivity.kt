package com.example.otpvarification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {

    private lateinit var cc: EditText
    private lateinit var number: EditText
    private lateinit var submit: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cc = findViewById(R.id.ccp)
        number = findViewById(R.id.number)

        submit = findViewById(R.id.GetOTP)
        auth = Firebase.auth


        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            Log.d("DATA1","User Details: ${user}")
            // User has already verified phone number, proceed with JWT authentication
            val jwtToken = sharedPreferences.getString("jwtToken", null)
            if (jwtToken != null) {
                signInWithJwtToken(jwtToken)
                val intent = Intent(this, Dashboard::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } else {
                // Handle case where JWT token is not available (possibly due to logout)
                // Redirect user to login screen
                hintNumber()
            }
        } else {
            // User hasn't verified phone number yet, initiate phone number verification
            // Your existing code for phone number verification goes here
        }







        submit.setOnClickListener {
            val num = number.text.toString().trim()
            val countryCode = cc.text.toString().trim()
            val intent = Intent(this@MainActivity, OTPManage::class.java)
            intent.putExtra("phoneNumber", num)
            intent.putExtra("countryCode", countryCode)
            startActivity(intent)
        }
    }


    private fun hintNumber(){
        val request: GetPhoneNumberHintIntentRequest = GetPhoneNumberHintIntentRequest.builder().build()

        val phoneNumberHintIntentResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                try {
                    val phoneNumber = Identity.getSignInClient(applicationContext).getPhoneNumberFromIntent(result.data)
                    number.setText(phoneNumber.substring(3))
                    cc.setText(phoneNumber.substring(0,3))
                } catch(e: Exception) {
                    Log.e(ContentValues.TAG, "Phone Number Hint failed")
                }
            }


        Identity.getSignInClient(applicationContext)
            .getPhoneNumberHintIntent(request)
            .addOnSuccessListener { result: PendingIntent ->
                try {
                    phoneNumberHintIntentResultLauncher.launch(
                        IntentSenderRequest.Builder(result).build()
                    )
                } catch (e: Exception) {
                    Log.e(ContentValues.TAG, "Launching the PendingIntent failed")
                }
            }
            .addOnFailureListener {
                Log.e(ContentValues.TAG, "Phone Number Hint failed")
            }
    }


    private fun signInWithJwtToken(jwtToken: String) {
        // Implement Firebase authentication with JWT token
        Log.d("DATA1","SignInJWT $jwtToken")
        auth.addIdTokenListener(FirebaseAuth.IdTokenListener {
            val user1 = it.currentUser
            user1?.getIdToken(true)
                ?.addOnCompleteListener{
                    val currentToken = it.result.token
                    Log.d("DATA1","Current JWT $currentToken")
                }
        })
        /*
        auth.signInWithCustomToken(jwtToken)
        .addOnCompleteListener(this) { task ->
        if (task.isSuccessful) {
        // User authenticated successfully
        task.result.user.let {
        it?.tenantId
        }
        // Proceed to dashboard or main activity
        startActivity(Intent(applicationContext, Dashboard::class.java))
        } else {
        // Authentication failed
        Toast.makeText(
        this@MainActivity,
        "Authentication failed.${task.exception}",
        Toast.LENGTH_SHORT
        ).show()
        Log.d("DATA1","Authentication failed: ${task.exception}")
        }
        }
        */

    }




}