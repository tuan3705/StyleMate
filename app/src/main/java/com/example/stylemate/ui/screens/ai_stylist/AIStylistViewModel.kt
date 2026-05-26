package com.example.stylemate.ui.screens.ai_stylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylemate.data.models.SuggestedOutfit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for AI Stylist Chat.
 * Manages chat history, loading states, and backend interactions.
 */
class AIStylistViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AIStylistUiState>(AIStylistUiState.Idle)
    val uiState: StateFlow<AIStylistUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        // Sample greeting
        _messages.value = listOf(
            ChatMessage("Hello! I'm your AI Stylist. How can I help you dress today?", isFromUser = false)
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // Add user message
            _messages.value += ChatMessage(text, isFromUser = true)
            _uiState.value = AIStylistUiState.Typing

            try {
                // TODO: POST /api/ai-stylist/chat
                // Mocking response
                kotlinx.coroutines.delay(1500)
                
                val mockOutfit = SuggestedOutfit(
                    id = "o_${System.currentTimeMillis()}",
                    items = listOf("item_1", "item_2"),
                    imageUrls = listOf("https://via.placeholder.com/150", "https://via.placeholder.com/150"),
                    reason = "Based on your meeting tomorrow and the rainy weather, I suggest this formal yet practical look.",
                    confidence = 0.89
                )

                _messages.value += ChatMessage(
                    text = "I suggest this outfit for your occasion:",
                    isFromUser = false,
                    suggestedOutfit = mockOutfit,
                    followups = listOf("Make it more formal", "Different colors?")
                )
                _uiState.value = AIStylistUiState.Idle
            } catch (e: Exception) {
                _uiState.value = AIStylistUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun handleAction(action: String, outfit: SuggestedOutfit) {
        viewModelScope.launch {
            when (action) {
                "tryon" -> {
                    // TODO: POST /api/ai-stylist/virtual-tryon
                }
                "save" -> {
                    // TODO: POST /api/outfits
                }
                "buy" -> {
                    // Navigate to shopping or external link
                }
            }
        }
    }
}

sealed class AIStylistUiState {
    object Idle : AIStylistUiState()
    object Typing : AIStylistUiState()
    data class Error(val message: String) : AIStylistUiState()
}

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val suggestedOutfit: SuggestedOutfit? = null,
    val followups: List<String> = emptyList()
)
