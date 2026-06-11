package com.example.stylemate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stylemate.R
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.viewmodel.OutfitViewModel
import kotlin.math.roundToInt
import kotlin.math.min
import kotlin.math.max

// ═════════════════════════════════════════════════════════════════
// 🎨 Helpers (màu & icon theo category)
// ═════════════════════════════════════════════════════════════════

fun getCategoryColor(category: String): Color = when (category) {
    "Tops" -> Color(0xFF42A5F5)      // Xanh dương
    "Bottoms" -> Color(0xFF66BB6A)    // Xanh lá
    "Dresses" -> Color(0xFFEC407A)    // Hồng
    "Footwear" -> Color(0xFF8D6E63)   // Nâu
    "Bags" -> Color(0xFFAB47BC)       // Tím
    "Accessories" -> Color(0xFFFFA726) // Cam
    "Jewelry" -> Color(0xFFD4E157)     // Vàng chanh
    else -> Color(0xFFBDBDBD)          // Xám
}

fun getCategoryIcon(category: String): String = when (category) {
    "Tops" -> "👕"
    "Bottoms" -> "👖"
    "Dresses" -> "👗"
    "Footwear" -> "👟"
    "Bags" -> "👜"
    "Accessories" -> "⌚"
    "Jewelry" -> "💍"
    else -> "🧥"
}

// ═════════════════════════════════════════════════════════════════
// 🖼️ rememberItemImageModel — Tạo Coil ImageRequest cho item
// ═════════════════════════════════════════════════════════════════

@Composable
fun rememberItemImageModel(item: ClothingItemEntity): ImageRequest? {
    val context = LocalContext.current
    return remember(item.imageOriginal, item.imageNoBg) {
        val path = item.imageNoBg.ifBlank { item.imageOriginal }
        if (path.isNotBlank()) {
            // ⚡ Dùng resolveImageData để xử lý cả relative path (/uploads/...)
            val data = com.example.stylemate.ui.common.resolveImageData(context, path)
            if (data != null) {
                ImageRequest.Builder(context)
                    .data(data)
                    .crossfade(false)
                    .build()
            } else null
        } else null
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔲 OutfitCanvasPreview — Preview nhỏ (dùng trong SavedOutfitCard)
// ═════════════════════════════════════════════════════════════════

@Composable
fun OutfitCanvasPreview(
    items: List<OutfitViewModel.OutfitItemPlacement>,
    modifier: Modifier = Modifier
) {
    val itemSize = 72.dp
    val density = LocalDensity.current
    val itemSizePx = with(density) { itemSize.toPx() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F2EA))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_items_in_outfit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@BoxWithConstraints
            }

            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()

            items.forEach { placement ->
                val item = placement.item
                val imageModel = rememberItemImageModel(item)
                val scaledSizePx = itemSizePx * placement.scale
                val maxX = (canvasWidth - scaledSizePx).coerceAtLeast(1f)
                val maxY = (canvasHeight - scaledSizePx).coerceAtLeast(1f)
                val offsetX = (placement.posX * maxX).roundToInt()
                val offsetY = (placement.posY * maxY).roundToInt()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX, offsetY) }
                        .size(itemSize * placement.scale)
                ) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = item.name.ifBlank { item.category },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(getCategoryColor(item.category).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = getCategoryIcon(item.category), fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔲 OutfitCanvas — Canvas chính để kéo thả, resize items
// ═════════════════════════════════════════════════════════════════

@Composable
fun OutfitCanvas(
    items: List<OutfitViewModel.OutfitItemPlacement>,
    selectedItemId: String?,
    onSelectItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onPositionChange: (String, Float, Float) -> Unit,
    onScaleChange: (String, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemSize = 96.dp
    val density = LocalDensity.current
    val itemSizePx = with(density) { itemSize.toPx() }
    val minScale = 0.4f
    val maxScale = 2.0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F2EA))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        ) {
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_items_in_outfit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@BoxWithConstraints
            }

            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()

            items.forEach { placement ->
                val item = placement.item
                val imageModel = rememberItemImageModel(item)
                val isSelected = item.id == selectedItemId
                var localPos by remember(item.id) {
                    mutableStateOf(Offset(placement.posX, placement.posY))
                }
                var localScale by remember(item.id) {
                    mutableStateOf(placement.scale)
                }
                LaunchedEffect(placement.posX, placement.posY) {
                    localPos = Offset(placement.posX, placement.posY)
                }
                LaunchedEffect(placement.scale) {
                    localScale = placement.scale
                }

                val scaledSizePx = itemSizePx * localScale
                val maxX = (canvasWidth - scaledSizePx).coerceAtLeast(1f)
                val maxY = (canvasHeight - scaledSizePx).coerceAtLeast(1f)

                val offsetX = (localPos.x * maxX).roundToInt()
                val offsetY = (localPos.y * maxY).roundToInt()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX, offsetY) }
                        .size(itemSize * localScale)
                        .pointerInput(item.id) {
                            detectTapGestures(onTap = { onSelectItem(item.id) })
                        }
                        .pointerInput(item.id, maxX, maxY) {
                            detectDragGestures(
                                onDragStart = { onSelectItem(item.id) },
                                onDrag = { change, dragAmount ->
                                    change.consumePositionChange()
                                    val newX = (localPos.x * maxX + dragAmount.x).coerceIn(0f, maxX)
                                    val newY = (localPos.y * maxY + dragAmount.y).coerceIn(0f, maxY)
                                    localPos = Offset(newX / maxX, newY / maxY)
                                },
                                onDragEnd = {
                                    onPositionChange(item.id, localPos.x, localPos.y)
                                }
                            )
                        }
                ) {
                    // Item image hoặc placeholder
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = item.name.ifBlank { item.category },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(getCategoryColor(item.category).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = getCategoryIcon(item.category), fontSize = 28.sp)
                        }
                    }

                    // Overlay controls khi được chọn
                    if (isSelected) {
                        // Viền chọn
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp))
                        )
                        // Handle resize (góc dưới phải)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = 6.dp)
                                .size(18.dp)
                                .background(Color(0xFFFFD54F), CircleShape)
                                .pointerInput(item.id, canvasWidth, canvasHeight) {
                                    detectDragGestures(
                                        onDragStart = { onSelectItem(item.id) },
                                        onDrag = { change, dragAmount ->
                                            change.consumePositionChange()
                                            val currentMaxX = (canvasWidth - itemSizePx * localScale).coerceAtLeast(1f)
                                            val currentMaxY = (canvasHeight - itemSizePx * localScale).coerceAtLeast(1f)
                                            val currentPxX = localPos.x * currentMaxX
                                            val currentPxY = localPos.y * currentMaxY
                                            val delta = (dragAmount.x + dragAmount.y) / itemSizePx
                                            val newScale = (localScale + delta).coerceIn(minScale, maxScale)
                                            val newMaxX = (canvasWidth - itemSizePx * newScale).coerceAtLeast(1f)
                                            val newMaxY = (canvasHeight - itemSizePx * newScale).coerceAtLeast(1f)
                                            val newPosX = (currentPxX.coerceIn(0f, newMaxX) / newMaxX)
                                            val newPosY = (currentPxY.coerceIn(0f, newMaxY) / newMaxY)
                                            localScale = newScale
                                            localPos = Offset(newPosX, newPosY)
                                        },
                                        onDragEnd = {
                                            onScaleChange(item.id, localScale)
                                            onPositionChange(item.id, localPos.x, localPos.y)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⇔", fontSize = 10.sp, color = Color.Black)
                        }
                        // Nút xoá (góc trên phải)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 5.dp, y = (-5).dp)
                                .size(16.dp)
                                .zIndex(1f)
                                .background(Color.Red.copy(alpha = 0.85f), CircleShape)
                                .clickable { onDeleteItem(item.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.remove_item_content_desc),
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔲 OutfitCanvasEditorDialog — Dialog chỉnh sửa Outfit trên Canvas
// ═════════════════════════════════════════════════════════════════

@Composable
fun OutfitCanvasEditorDialog(
    outfitName: String,
    items: List<OutfitViewModel.OutfitItemPlacement>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAddItems: () -> Unit,
    onSave: () -> Unit,
    onPositionChange: (String, Float, Float) -> Unit,
    onScaleChange: (String, Float) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Header ──────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = outfitName.ifBlank { stringResource(R.string.outfit_canvas_title) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_content_desc))
                    }
                }

                // ── Canvas ──────────────────────────────────────
                OutfitCanvas(
                    items = items,
                    selectedItemId = selectedItemId,
                    onSelectItem = { selectedItemId = it },
                    onDeleteItem = { itemId ->
                        onDeleteItem(itemId)
                        if (selectedItemId == itemId) selectedItemId = null
                    },
                    onPositionChange = onPositionChange,
                    onScaleChange = onScaleChange
                )

                // ── Action buttons ──────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onAddItems,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.outfit_canvas_add_items))
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = items.isNotEmpty() && !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.saving_label))
                        } else {
                            Text(stringResource(R.string.outfit_save_outfit))
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔲 AddItemsBottomSheet — BottomSheet chọn items để thêm vào canvas
// ═════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemsBottomSheet(
    allItems: List<ClothingItemEntity>,
    existingIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<ClothingItemEntity>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(existingIds) {
        selectedIds = emptySet()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.outfit_canvas_add_items),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                items(allItems, key = { it.id }) { item ->
                    val alreadyAdded = item.id in existingIds
                    val isSelected = item.id in selectedIds || alreadyAdded
                    AddItemSelectCard(
                        item = item,
                        isSelected = isSelected,
                        enabled = !alreadyAdded,
                        onToggle = {
                            selectedIds = if (item.id in selectedIds) {
                                selectedIds - item.id
                            } else {
                                selectedIds + item.id
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel_button))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val selectedItems = allItems.filter { it.id in selectedIds }
                        onConfirm(selectedItems)
                    },
                    enabled = selectedIds.isNotEmpty()
                ) {
                    Text(stringResource(R.string.outfit_canvas_add_count, selectedIds.size))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔲 AddItemSelectCard — Card item có thể chọn trong AddItemsBottomSheet
// ═════════════════════════════════════════════════════════════════

@Composable
fun AddItemSelectCard(
    item: ClothingItemEntity,
    isSelected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF4CAF50) else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp
    val alphaVal = if (enabled) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onToggle() }
            .alpha(alphaVal),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getCategoryColor(item.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = getCategoryIcon(item.category), fontSize = 24.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.name.ifBlank { item.category },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.selected_vn_content_desc),
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔧 Utility: defaultGridPosition — vị trí lưới mặc định cho items
// ═════════════════════════════════════════════════════════════════

fun defaultGridPosition(index: Int): Pair<Float, Float> {
    val col = index % 2
    val row = index / 2
    val x = if (col == 0) 0.1f else 0.55f
    val y = min(0.1f + row * 0.25f, 0.8f)
    return x to y
}
