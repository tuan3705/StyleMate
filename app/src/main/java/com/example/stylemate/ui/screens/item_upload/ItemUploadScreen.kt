package com.example.stylemate.ui.screens.item_upload

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.R
import com.example.stylemate.ui.components.MaskEditor
import com.example.stylemate.ui.components.StylistButton
import com.example.stylemate.ui.components.StylistTextField
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemUploadScreen(
    viewModel: ItemUploadViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val step by viewModel.currentStep.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.upload_item_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step == UploadStep.SELECT_IMAGE) onBack() else viewModel.previousStep()
                    }) {
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
                .padding(16.dp)
        ) {
            when (step) {
                UploadStep.SELECT_IMAGE -> SelectImageStep(onNext = { viewModel.nextStep() })
                UploadStep.MASK_EDITOR -> MaskEditorStep(onNext = { viewModel.nextStep() })
                UploadStep.REVIEW_METADATA -> ReviewMetadataStep(onComplete = { onBack() })
            }
        }
    }
}

@Composable
fun SelectImageStep(onNext: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.select_image_hint))
        }
        Spacer(modifier = Modifier.height(24.dp))
        StylistButton(text = stringResource(R.string.next_edit_mask), onClick = onNext)
    }
}

@Composable
fun MaskEditorStep(onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        MaskEditor(
            modifier = Modifier.weight(1f),
            onMaskChanged = {}
        )
        Spacer(modifier = Modifier.height(16.dp))
        StylistButton(text = stringResource(R.string.next_review_details), onClick = onNext)
    }
}

@Composable
fun ReviewMetadataStep(onComplete: () -> Unit) {
    var category by remember { mutableStateOf("Shirt") }
    var color by remember { mutableStateOf("#FFFFFF") }

    Column {
        StylistTextField(value = category, onValueChange = { category = it }, label = stringResource(R.string.category_label))
        Spacer(modifier = Modifier.height(8.dp))
        StylistTextField(value = color, onValueChange = { color = it }, label = stringResource(R.string.color_hex_label))
        
        Spacer(modifier = Modifier.weight(1f))
        StylistButton(text = stringResource(R.string.save_to_closet_button), onClick = onComplete)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewItemUploadScreen() {
    ItemUploadScreen()
}
