package com.example.stylemate.ui.screens.ai_stylist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
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
fun AILocationSettingsScreen(
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        placeholder = { Text(stringResource(R.string.ai_location_search_placeholder), color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.ai_location_search_clear))
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_desc))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Current Location Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.ai_location_use_current),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
            }

            HorizontalDivider(color = Color(0xFFF0F0F0))

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.ai_location_recent_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Recent Search Item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ai_location_default_result),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
