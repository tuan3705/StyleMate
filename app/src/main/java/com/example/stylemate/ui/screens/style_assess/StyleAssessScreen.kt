package com.example.stylemate.ui.screens.style_assess

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.R
import com.example.stylemate.ui.components.SelectedItemsStrip
import com.example.stylemate.ui.components.StylistButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleAssessScreen(
    viewModel: StyleAssessViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val selectedItems by viewModel.selectedItems.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.style_assessment_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_desc))
                    }
                }
            )
        },
        bottomBar = {
            if (selectedItems.isNotEmpty() && uiState is StyleAssessUiState.Idle) {
                Column {
                    SelectedItemsStrip(
                        items = selectedItems,
                        onRemove = { viewModel.removeItem(it) }
                    )
                    StylistButton(
                        text = stringResource(R.string.assess_style_button),
                        onClick = { viewModel.runAssessment() },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is StyleAssessUiState.Idle -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.assess_style_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is StyleAssessUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is StyleAssessUiState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.style_score_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "${state.result.score}/10",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = state.result.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        StylistButton(
                            text = stringResource(R.string.try_again_button),
                            onClick = { viewModel.reset() }
                        )
                    }
                }
                is StyleAssessUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(16.dp))
                            StylistButton(
                                text = stringResource(R.string.try_again_button),
                                onClick = { viewModel.reset() }
                            )
                        }
                    }
                }
            }
        }
    }
}