package com.appdev.voicecallapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.appdev.voicecallapp.DataModel.UserInfo
import com.appdev.voicecallapp.Utils.UserValidation
import com.appdev.voicecallapp.databinding.ActivitySignUpActivtiyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class SignUpActivity : AppCompatActivity() {
    lateinit var binding: ActivitySignUpActivtiyBinding

    private var firebaseAuth: FirebaseAuth? = null
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpActivtiyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance();

        binding.registerBtn.setOnClickListener {


            val email = binding.suMail.text.toString()
            val userName = binding.suUser.text.toString()
            val password = binding.suPass.text.toString()
            val confirmPassword = binding.suCpass.text.toString()

            val checkMessage: String =
                UserValidation().newUserValidationCheck(email, password, confirmPassword)
            val userValidationReceived: Boolean =
                UserValidation().newUserConfirmValidationCheck(email, password, confirmPassword)



            if (userValidationReceived) {
                binding.pg.visibility = View.VISIBLE
                firebaseAuth!!.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        binding.pg.visibility = View.GONE
                        if (it.isSuccessful) {
                            val userInfo = UserInfo(userName, password, email)
                            firebaseDatabase.reference.child("userProfiles")
                                .child(firebaseAuth!!.uid!!)
                                .setValue(userInfo)
                                .addOnSuccessListener {
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                it.exception!!.localizedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                binding.pg.visibility = View.GONE
                Toast.makeText(this, checkMessage, Toast.LENGTH_SHORT).show()
            }
        }

    }
}