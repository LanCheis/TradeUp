package com.example.tradeup.utils

import android.util.Patterns

object Validators {
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
