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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stylemate.R

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
                title = { Text(stringResource(R.string.ai_settings_title), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_desc))
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
                    label = stringResource(R.string.ai_settings_location),
                    value = stringResource(R.string.ai_settings_location_default),
                    onClick = onNavigateToLocation
                )
                SettingsItem(
                    label = stringResource(R.string.ai_settings_closet),
                    value = stringResource(R.string.ai_closet_settings_all_items),
                    onClick = onNavigateToCloset
                )
                SettingsItem(
                    label = stringResource(R.string.ai_settings_temperature_feel),
                    value = stringResource(R.string.ai_settings_temperature_moderate),
                    onClick = { /* Open Bottom Sheet or Dialog */ }
                )
                SettingsItem(
                    label = stringResource(R.string.ai_settings_excluded_items),
                    onClick = { /* Navigate */ }
                )
                SettingsItem(
                    label = stringResource(R.string.ai_settings_incompatible_styles),
                    onClick = { /* Navigate */ }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsItem(
                    label = stringResource(R.string.ai_settings_stylist_notes),
                    description = stringResource(R.string.ai_settings_stylist_notes_desc),
                    onClick = onNavigateToNotes
                )
                SettingsItem(
                    label = stringResource(R.string.ai_settings_favorite_brands),
                    value = stringResource(R.string.ai_settings_budget_brands),
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
