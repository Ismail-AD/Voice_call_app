package com.appdev.voicecallapp.Utils

import android.util.Patterns
import android.text.TextUtils

class UserValidation {

    fun userValidationCheck(email: String, password: String): String {
        return when {
            email.isEmpty() -> "Please enter the email to proceed !"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid Email !"
            password.isEmpty() -> "Please enter the password to proceed !"
            password.length <= 6 -> "Password must contain 7 characters !"
            else -> ""
        }
    }

    fun newUserValidationCheck(email: String, password: String, cpassword: String): String {
        return when {
            email.isEmpty() -> "Please enter the email to proceed !"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Please enter a valid Email !"
            password.isEmpty() -> "Please enter the password to proceed !"
            cpassword.isEmpty() -> "Please enter the password to proceed !"
            password.length <= 6 -> "Password must contain 7 characters !"
            password != cpassword -> "Passwords didn't match !"
            else -> ""
        }
    }

    fun userValidationCheckReturn(email: String, password: String): Boolean {
        return email.isNotEmpty() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.isNotEmpty() &&
                password.length > 6
    }

    fun newUserConfirmValidationCheck(email: String, password: String, cpassword: String): Boolean {
        return email.isNotEmpty() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.isNotEmpty() &&
                cpassword.isNotEmpty() &&
                password == cpassword &&
                password.length > 6
    }

}