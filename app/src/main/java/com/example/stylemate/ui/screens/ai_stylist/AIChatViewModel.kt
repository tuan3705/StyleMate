package com.example.stylemate.ui.screens.ai_stylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylemate.network.ChatRequest
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.SuggestedOutfitDto
import com.example.stylemate.data.auth.AuthStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * ViewModel for AI Stylist Chat.
 * Manages chat history, loading states, and backend interactions.
 */
class AIChatViewModel(
    private val authStorage: AuthStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIChatUiState>(AIChatUiState.Idle)
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var currentSessionId: String? = null

    init {
        // Initial state or message if needed
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val userId = authStorage.userIdFlow.firstOrNull()
            if (userId == null) {
                _uiState.value = AIChatUiState.Error("Vui lòng đăng nhập lại")
                return@launch
            }

            _messages.value += ChatMessage(text, isFromUser = true)
            _uiState.value = AIChatUiState.Typing

            try {
                val response = RetrofitClient.stylemateApiService.chatWithAi(
                    ChatRequest(
                        userId = userId,
                        message = text,
                        sessionId = currentSessionId
                    )
                )

                if (response.isSuccessful) {
                    val chatData = response.body()
                    currentSessionId = chatData?.sessionId
                    val responseText = chatData?.message ?: "Tôi xin lỗi, tôi gặp chút trục trặc."
                    
                    _messages.value += ChatMessage(
                        text = responseText,
                        isFromUser = false,
                        suggestedOutfits = chatData?.suggested_outfits ?: emptyList(),
                        followups = chatData?.followups ?: emptyList()
                    )
                    _uiState.value = AIChatUiState.Idle
                } else {
                    _uiState.value = AIChatUiState.Error("Lỗi từ server: \${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = AIChatUiState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    fun handleAction(action: String, outfit: SuggestedOutfitDto) {
        viewModelScope.launch {
            // Action handling logic
        }
    }
}

sealed class AIChatUiState {
    object Idle : AIChatUiState()
    object Typing : AIChatUiState()
    data class Error(val message: String) : AIChatUiState()
}

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val suggestedOutfits: List<SuggestedOutfitDto> = emptyList(),
    val followups: List<String> = emptyList()
)
