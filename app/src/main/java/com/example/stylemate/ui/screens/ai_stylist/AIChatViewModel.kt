package com.example.stylemate.ui.screens.ai_stylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylemate.network.ChatRequest
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.SuggestedOutfitDto
import com.example.stylemate.network.OutfitSectionDto
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

    private val weatherRepository = com.example.stylemate.repository.WeatherRepository()

    private val _uiState = MutableStateFlow<AIChatUiState>(AIChatUiState.Welcome)
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentRecommendation = MutableStateFlow<AiRecommendation?>(null)
    val currentRecommendation: StateFlow<AiRecommendation?> = _currentRecommendation.asStateFlow()

    private var currentSessionId: String? = null

    init {
        // Initial state or message if needed
    }

    fun sendMessage(text: String) {
        sendMessageInternal(text)
    }

    fun sendWizardMessage(text: String, destination: String?, dateMillis: Long?) {
        viewModelScope.launch {
            var weatherContext = ""
            val locationName = destination ?: "Hà Nội"
            val dateStr = if (dateMillis != null) {
                java.text.SimpleDateFormat("dd 'thg' MM", java.util.Locale.getDefault()).format(java.util.Date(dateMillis))
            } else "Hôm nay"
            var tempStr = "26 / 22°C"
            var lat: Double? = null
            var lon: Double? = null

            if (destination != null) {
                try {
                    // Mock coordinates for cities for now
                    val coords = getCoordinatesForCity(destination)
                    lat = coords.first
                    lon = coords.second
                    
                    val weather = weatherRepository.getWeatherForecast(lat, lon)
                    tempStr = "${weather.current.tempC.toInt()} / ${weather.forecast.forecastDay.firstOrNull()?.day?.minTempC?.toInt()}°C"
                    weatherContext = "\nThời tiết tại $destination vào $dateStr: ${weather.current.condition.text}, $tempStr."
                } catch (ignore: Exception) {
                    // Fallback if weather fails
                }
            }

            val finalMessage = if (weatherContext.isNotEmpty()) {
                "$text. $weatherContext"
            } else {
                text
            }

            sendMessageInternal(
                finalMessage, 
                overrideLocation = locationName, 
                overrideDate = dateStr, 
                overrideTemp = tempStr,
                lat = lat,
                lon = lon,
                dateMillis = dateMillis
            )
        }
    }

    private fun sendMessageInternal(
        text: String,
        overrideLocation: String? = null,
        overrideDate: String? = null,
        overrideTemp: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        dateMillis: Long? = null
    ) {
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
                        sessionId = currentSessionId,
                        lat = lat,
                        lon = lon,
                        dateMillis = dateMillis
                    )
                )

                if (response.isSuccessful) {
                    val chatData = response.body()
                    currentSessionId = chatData?.sessionId
                    val responseText = chatData?.message ?: "Tôi xin lỗi, tôi gặp chút trục trặc."
                    
                    val newMessage = ChatMessage(
                        text = responseText,
                        isFromUser = false,
                        suggestedOutfits = chatData?.suggested_outfits ?: emptyList(),
                        followups = chatData?.followups ?: emptyList()
                    )
                    _messages.value += newMessage

                    // Transition to recommendation view if outfits are present
                    if (chatData?.suggested_outfits?.isNotEmpty() == true) {
                        val outfit = chatData.suggested_outfits.first()
                        _currentRecommendation.value = AiRecommendation(
                            styleTitle = outfit.style_title ?: "Phong cách phù hợp",
                            description = outfit.description ?: responseText,
                            date = overrideDate ?: outfit.date ?: "09 thg 6",
                            location = overrideLocation ?: outfit.location ?: "Hà Nội",
                            temp = overrideTemp ?: outfit.temp ?: "26 / 22°C",
                            sections = outfit.sections ?: emptyList(),
                            outfit = outfit
                        )
                        _uiState.value = AIChatUiState.Recommendation
                    } else {
                        _uiState.value = AIChatUiState.Idle
                    }
                } else {
                    _uiState.value = AIChatUiState.Error("Lỗi từ server: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = AIChatUiState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    private fun getCoordinatesForCity(city: String): Pair<Double, Double> {
        return when (city) {
            "Tokyo" -> 35.6895 to 139.6917
            "Seoul" -> 37.5665 to 126.9780
            "Bangkok" -> 13.7563 to 100.5018
            "Hồng Kông" -> 22.3193 to 114.1694
            "Singapore" -> 1.3521 to 103.8198
            "Paris" -> 48.8566 to 2.3522
            "Luân Đôn" -> 51.5074 to -0.1278
            "Rome" -> 41.9028 to 12.4964
            "Barcelona" -> 41.3851 to 2.1734
            "Amsterdam" -> 52.3676 to 4.9041
            "New York" -> 40.7128 to -74.0060
            "Los Angeles" -> 34.0522 to -118.2437
            "Toronto" -> 43.6532 to -79.3832
            "Vancouver" -> 49.2827 to -123.1207
            "Rio de Janeiro" -> -22.9068 to -43.1729
            "Buenos Aires" -> -34.6037 to -58.3816
            "Sydney" -> -33.8688 to 151.2093
            "Melbourne" -> -37.8136 to 144.9631
            "Dubai" -> 25.2048 to 55.2708
            else -> 21.0285 to 105.8542 // Hanoi
        }
    }

    fun startOver() {
        _messages.value = emptyList()
        _currentRecommendation.value = null
        _uiState.value = AIChatUiState.Welcome
    }

    fun handleAction(action: String, outfit: SuggestedOutfitDto) {
        viewModelScope.launch {
            // Action handling logic
        }
    }
}

sealed class AIChatUiState {
    object Welcome : AIChatUiState()
    object Typing : AIChatUiState()
    object Recommendation : AIChatUiState()
    object Idle : AIChatUiState()
    data class Error(val message: String) : AIChatUiState()
}

data class AiRecommendation(
    val styleTitle: String,
    val description: String,
    val date: String,
    val location: String,
    val temp: String,
    val sections: List<OutfitSectionDto>,
    val outfit: SuggestedOutfitDto
)

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val suggestedOutfits: List<SuggestedOutfitDto> = emptyList(),
    val followups: List<String> = emptyList()
)
