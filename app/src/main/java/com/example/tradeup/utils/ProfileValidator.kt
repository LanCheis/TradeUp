package com.example.tradeup.utils

import com.example.tradeup.data.model.User

object ProfileValidator {

    /**
     * Check if user profile is complete enough to access main app
     * Required fields: displayName, bio (min 10 chars), phoneNumber
     */
    fun isProfileComplete(user: User?): Boolean {
        if (user == null) return false

        return user.displayName.isNotBlank() &&
                user.bio.isNotBlank() && user.bio.length >= 10 &&
                user.phoneNumber.isNotBlank()
    }

    /**
     * Get missing profile fields for user feedback
     */
    fun getMissingFields(user: User?): List<String> {
        val missing = mutableListOf<String>()

        if (user == null) {
            return listOf("Complete profile setup required")
        }

        if (user.displayName.isBlank()) {
            missing.add("Username/Display Name")
        }

        if (user.bio.isBlank()) {
            missing.add("Bio")
        } else if (user.bio.length < 10) {
            missing.add("Bio (minimum 10 characters)")
        }

        if (user.phoneNumber.isBlank()) {
            missing.add("Phone Number")
        }

        return missing
    }
}