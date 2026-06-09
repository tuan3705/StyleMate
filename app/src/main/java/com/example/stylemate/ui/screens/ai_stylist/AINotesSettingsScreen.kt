package com.example.stylemate.ui.screens.ai_stylist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AINotesSettingsScreen(
    onBack: () -> Unit
) {
    var noteText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ghi chú cho Nhà tạo mẫu", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (noteText.isNotEmpty()) Color.Black else Color.LightGray.copy(alpha = 0.5f)
                ),
                enabled = noteText.isNotEmpty()
            ) {
                Text("Lưu", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Hãy cho nhà tạo mẫu biết nhu cầu hoặc quy tắc cụ thể của bạn",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ghi chú này chỉ được tham khảo khi sử dụng Nhà tạo mẫu cá nhân.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 300) noteText = it },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                placeholder = { 
                    Text("Ví dụ: Mình thích phong cách tối giản, tránh giày cao gót.", color = Color.LightGray) 
                },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFFBFBFB),
                    unfocusedContainerColor = Color(0xFFFBFBFB)
                )
            )
            
            Text(
                text = "${noteText.length}/300",
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }
    }
}
