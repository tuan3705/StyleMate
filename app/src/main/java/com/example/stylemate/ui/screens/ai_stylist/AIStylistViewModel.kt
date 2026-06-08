package com.example.stylemate.ui.screens.ai_stylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylemate.network.ChatRequest
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.SuggestedOutfitDto
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

    private var currentSessionId: String? = null

    init {
        // Sample greeting
        _messages.value = listOf(
            ChatMessage("Xin chào! Tôi là AI Stylist của bạn. Hôm nay tôi có thể giúp gì cho bạn?", isFromUser = false)
        )
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // Add user message
            _messages.value += ChatMessage(text, isFromUser = true)
            _uiState.value = AIStylistUiState.Typing

            try {
                val response = RetrofitClient.stylemateApiService.chatWithAi(
                    ChatRequest(
                        userId = "HungBu", // Stub user ID
                        message = text,
                        sessionId = currentSessionId
                    )
                )

                if (response.isSuccessful) {
                    val chatData = response.body()
                    currentSessionId = chatData?.sessionId

                    val responseText = chatData?.message ?: "Tôi xin lỗi, tôi gặp chút trục trặc."
                    
                    // Add AI response
                    _messages.value += ChatMessage(
                        text = responseText,
                        isFromUser = false,
                        suggestedOutfits = chatData?.suggested_outfits ?: emptyList(),
                        followups = chatData?.followups ?: emptyList()
                    )
                    _uiState.value = AIStylistUiState.Idle
                } else {
                    _uiState.value = AIStylistUiState.Error("Lỗi từ server: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = AIStylistUiState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    fun handleAction(action: String, outfit: SuggestedOutfitDto) {
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
    val suggestedOutfits: List<SuggestedOutfitDto> = emptyList(),
    val followups: List<String> = emptyList()
)
