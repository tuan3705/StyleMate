package com.example.stylemate.data.auth

object AuthValidator {
    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValidEmail(email: String): Boolean {
        return emailRegex.matches(email.trim())
    }

    fun isValidPassword(password: String): Boolean {
        return password.trim().length >= 6
    }
}

