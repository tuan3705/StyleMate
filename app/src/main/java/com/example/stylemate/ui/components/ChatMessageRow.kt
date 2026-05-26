package com.example.stylemate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

/**
 * ChatMessageRow displays a single message in the chat thread.
 * 
 * @param message The text content of the message.
 * @param isFromUser True if the message is from the user, false if from the AI stylist.
 */
@Composable
fun ChatMessageRow(
    message: String,
    isFromUser: Boolean
) {
    val alignment = if (isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isFromUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val shape = if (isFromUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bgColor)
                .padding(12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChatMessageRow() {
    Column {
        ChatMessageRow(message = "Hi! Suggest an outfit for a meeting.", isFromUser = true)
        ChatMessageRow(message = "Sure, I found 3 options for you.", isFromUser = false)
    }
}
