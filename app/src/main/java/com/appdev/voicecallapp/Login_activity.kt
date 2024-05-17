package com.appdev.voicecallapp

import android.R
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.appdev.voicecallapp.Utils.UserValidation
import com.appdev.voicecallapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth


class Login_activity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (firebaseAuth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.newId.setOnClickListener { v ->
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.loginBtn.setOnClickListener { v ->
            val email: String = binding.suMail.getText().toString().trim()
            val password: String = binding.suPass.getText().toString()
            val checkMessage = UserValidation().userValidationCheck(email, password)
            val userValidationReceived =
                UserValidation().userValidationCheckReturn(email, password)
            if (userValidationReceived) {
                binding.pg.visibility = View.VISIBLE
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        binding.pg.visibility = View.GONE
                        if (it.isSuccessful) {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            binding.pg.visibility = View.GONE
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