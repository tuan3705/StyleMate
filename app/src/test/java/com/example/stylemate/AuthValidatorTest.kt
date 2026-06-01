package com.example.stylemate

import com.example.stylemate.data.auth.AuthValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun `valid email passes`() {
        assertTrue(AuthValidator.isValidEmail("user@example.com"))
    }

    @Test
    fun `invalid email fails`() {
        assertFalse(AuthValidator.isValidEmail("invalid-email"))
    }

    @Test
    fun `password min length`() {
        assertTrue(AuthValidator.isValidPassword("123456"))
        assertFalse(AuthValidator.isValidPassword("12345"))
    }
}

