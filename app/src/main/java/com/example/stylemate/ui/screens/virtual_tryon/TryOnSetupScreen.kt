package com.example.stylemate.ui.screens.virtual_tryon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stylemate.R
import com.example.stylemate.data.models.JobStatus
import com.example.stylemate.ui.components.StylistButton
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryOnSetupScreen(
    viewModel: TryOnViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val jobState by viewModel.jobState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.virtual_tryon_title)) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (jobState == null) {
                // Setup Phase
                Text(stringResource(R.string.tryon_setup_hint))
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.tryon_body_placeholder))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.tryon_item_placeholder))
                }

                Spacer(modifier = Modifier.weight(1f))
                
                StylistButton(
                    text = stringResource(R.string.start_try_on),
                    onClick = { viewModel.startTryOn("dummy_uri", "item_1") }
                )
            } else {
                // Progress / Result Phase
                val job = jobState!!
                when (job.status) {
                    JobStatus.QUEUED, JobStatus.IN_PROGRESS -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(progress = { job.progress / 100f })
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.tryon_processing_label, job.progress))
                            Text(stringResource(R.string.tryon_status_label, job.status.toString()), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    JobStatus.COMPLETED -> {
                        AsyncImage(
                            model = job.resultUrls.firstOrNull(),
                            contentDescription = stringResource(R.string.tryon_result_content_desc),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        StylistButton(text = stringResource(R.string.save_outfit_simple), onClick = { /* TODO */ })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.try_another_button))
                        }
                    }
                    JobStatus.FAILED -> {
                        Text(stringResource(R.string.tryon_failed_label, job.error ?: ""), color = MaterialTheme.colorScheme.error)
                        StylistButton(text = stringResource(R.string.retry_button), onClick = { viewModel.reset() })
                    }
                    else -> {}
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTryOnSetupScreen() {
    TryOnSetupScreen()
}