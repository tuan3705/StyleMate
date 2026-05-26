package com.example.stylemate.ui.screens.ai_stylist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.ui.components.ChatMessageRow
import com.example.stylemate.ui.components.OutfitSuggestionCard
import com.example.stylemate.ui.components.QuickPromptsRow
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIStylistScreen(
    viewModel: AIStylistViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Stylist") }
            )
        },
        bottomBar = {
            Column {
                // Quick Prompts
                val lastMessage = messages.lastOrNull()
                if (lastMessage != null && !lastMessage.isFromUser && lastMessage.followups.isNotEmpty()) {
                    QuickPromptsRow(
                        prompts = lastMessage.followups,
                        onPromptClick = { viewModel.sendMessage(it) }
                    )
                }

                // Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask me anything...") },
                        maxLines = 3
                    )
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        enabled = inputText.isNotBlank() && uiState !is AIStylistUiState.Typing
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(messages) { message ->
                ChatMessageRow(
                    message = message.text,
                    isFromUser = message.isFromUser
                )
                
                message.suggestedOutfit?.let { outfit ->
                    OutfitSuggestionCard(
                        outfit = outfit,
                        onAction = { action, _ ->
                            viewModel.handleAction(action, outfit)
                        }
                    )
                }
            }
            
            if (uiState is AIStylistUiState.Typing) {
                item {
                    Text(
                        text = "AI is typing...",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAIStylistScreen() {
    AIStylistScreen()
}
