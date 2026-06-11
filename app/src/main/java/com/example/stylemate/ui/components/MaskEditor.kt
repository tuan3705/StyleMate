package com.example.stylemate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.stylemate.R

/**
 * MaskEditor provides a simple drawing canvas to refine auto-segmentation masks.
 * 
 * @param onMaskChanged Callback with the updated drawing paths (simplified for scaffold).
 */
@Composable
fun MaskEditor(
    modifier: Modifier = Modifier,
    onMaskChanged: (List<List<Offset>>) -> Unit
) {
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val paths = remember { mutableStateListOf<List<Offset>>() }

    Column(modifier = modifier) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = { paths.clear() }) {
                Text(stringResource(R.string.mask_editor_reset))
            }
            Button(onClick = { /* Undo logic */ }) {
                Text(stringResource(R.string.mask_editor_undo))
            }
        }

        // Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = listOf(offset)
                                paths.add(currentPath)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath = currentPath + change.position
                                // Update the last path in the list
                                if (paths.isNotEmpty()) {
                                    paths[paths.size - 1] = currentPath
                                }
                            },
                            onDragEnd = {
                                onMaskChanged(paths.toList())
                            }
                        )
                    }
            ) {
                paths.forEach { path ->
                    for (i in 0 until path.size - 1) {
                        drawLine(
                            color = Color.Red.copy(alpha = 0.5f),
                            start = path[i],
                            end = path[i + 1],
                            strokeWidth = 40f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
        
        Text(
            text = stringResource(R.string.mask_editor_hint),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMaskEditor() {
    MaskEditor(onMaskChanged = {})
}
