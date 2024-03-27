package com.example.otpvarification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity

class MainActivity : AppCompatActivity() {

    private lateinit var cc: EditText
    private lateinit var number: EditText
    private lateinit var submit: Button
    private  lateinit var Animation : Animation
//    private lateinit var  animeText: TextView
//    private lateinit var animeBTN : Button

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cc = findViewById(R.id.ccp)
        number = findViewById(R.id.number)

        submit = findViewById(R.id.GetOTP)
//
//        animeText = findViewById(R.id.anime)
//        animeBTN = findViewById(R.id.animeBTN)



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





        submit.setOnClickListener {
            val num = number.text.toString().trim()
            val countryCode = cc.text.toString().trim()
            val intent = Intent(this@MainActivity, OTPManage::class.java)
            intent.putExtra("phoneNumber", num)
            intent.putExtra("countryCode", countryCode)
            startActivity(intent)
        }
//        animeBTN.setOnClickListener(View.OnClickListener {
//            val animation = AnimationUtils.loadAnimation(this,R.animator.fade_in)
//            animeText.startAnimation(animation) })
    }
}