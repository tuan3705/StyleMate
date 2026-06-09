package com.example.stylemate.ui.screens.ai_stylist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
fun AISettingsScreen(
    onBack: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToCloset: () -> Unit,
    onNavigateToNotes: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cài đặt gợi ý trang phục", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsItem(
                    label = "Vị trí",
                    value = "Hà Nội",
                    onClick = onNavigateToLocation
                )
                SettingsItem(
                    label = "Tủ đồ để gợi ý trang phục",
                    value = "Tất cả món đồ",
                    onClick = onNavigateToCloset
                )
                SettingsItem(
                    label = "Cài đặt cảm nhận nhiệt độ",
                    value = "Vừa phải 😊",
                    onClick = { /* Open Bottom Sheet or Dialog */ }
                )
                SettingsItem(
                    label = "Món đồ bị loại trừ",
                    onClick = { /* Navigate */ }
                )
                SettingsItem(
                    label = "Các cách phối không hợp",
                    onClick = { /* Navigate */ }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsItem(
                    label = "Ghi chú cho Nhà tạo mẫu",
                    description = "Có quy tắc nào Nhà tạo mẫu AI cá nhân của bạn cần lưu ý không?",
                    onClick = onNavigateToNotes
                )
                SettingsItem(
                    label = "Thương hiệu yêu thích",
                    value = "Thương hiệu bình dân",
                    onClick = { /* Navigate */ }
                )
            }
        }
    }
}

@Composable
fun SettingsItem(
    label: String,
    value: String? = null,
    description: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )
                }
                if (value != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = value,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = Color(0xFFF0F0F0))
    }
}
