package com.example.stylemate.ui.screens

import com.example.stylemate.ui.screens.ai_stylist.AIStylistViewModel
import com.example.stylemate.ui.screens.ai_stylist.AIStylistUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AIStylistViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: AIStylistViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AIStylistViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test sendMessage adds user message and set typing state`() = runTest {
        viewModel.sendMessage("Hello Stylist")
        
        val messages = viewModel.messages.value
        assertEquals(2, messages.size) // Greeting + User message
        assertEquals("Hello Stylist", messages[1].text)
        assertTrue(messages[1].isFromUser)
        
        assertEquals(AIStylistUiState.Typing, viewModel.uiState.value)
    }
}
