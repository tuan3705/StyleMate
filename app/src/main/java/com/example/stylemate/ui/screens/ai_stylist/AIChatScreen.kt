package com.example.stylemate.ui.screens.ai_stylist

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.example.stylemate.R
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.Categories
import com.example.stylemate.network.RetrofitClient.STYLEMATE_BASE_URL
import com.example.stylemate.network.OutfitSectionDto
import com.example.stylemate.network.SuggestedOutfitDto
import com.example.stylemate.ui.components.ChatMessageRow
import com.example.stylemate.ui.components.OutfitSuggestionCard
import com.example.stylemate.viewmodel.ClothingViewModel
import com.example.stylemate.viewmodel.ClothingViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val app = LocalContext.current.applicationContext as com.example.stylemate.StyleMateApp
    val viewModel: AIChatViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AIChatViewModel(app.authStorage) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val recommendation by viewModel.currentRecommendation.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showWizard by remember { mutableStateOf(false) }
    var selectedOccasion by remember { mutableStateOf("") }
    var showSaveSheet by remember { mutableStateOf(false) }
    var isChatHistoryVisible by remember { mutableStateOf(false) }
    var isChatBubbleDismissed by remember { mutableStateOf(false) }

    // Reset bubble dismissal when a new AI message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !messages.last().isFromUser) {
            isChatBubbleDismissed = false
        }
    }
    
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color.White,
                    0.4f to Color.White,
                    1.0f to Color(0xFFD9EBFF)
                )
            )
    ) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_desc), tint = Color.Black)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_content_desc), tint = Color.Black)
                    }
                }
            },
            bottomBar = {
                if (uiState is AIChatUiState.Welcome) {
                    val prompts = listOf(
                        stringResource(R.string.prompt_daily),
                        stringResource(R.string.prompt_school),
                        stringResource(R.string.prompt_work),
                        stringResource(R.string.prompt_travel),
                        stringResource(R.string.prompt_party),
                        stringResource(R.string.prompt_date),
                        stringResource(R.string.prompt_wedding),
                        stringResource(R.string.wizard_occasion_shopping),
                        stringResource(R.string.wizard_occasion_walk),
                        stringResource(R.string.wizard_occasion_exercise),
                        stringResource(R.string.wizard_occasion_church),
                        stringResource(R.string.wizard_occasion_gathering),
                        stringResource(R.string.wizard_occasion_dining)
                    )
                    Column {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            items(prompts) { prompt ->
                                Surface(
                                    onClick = { 
                                        inputText = context.getString(R.string.prompt_suggest_prefix) + prompt
                                        selectedOccasion = prompt
                                        showWizard = true 
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.Gray.copy(alpha = 0.05f)
                                ) {
                                    Text(
                                        text = prompt,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                        fontSize = 16.sp,
                                        color = Color.Black.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        ChatInputBar(
                            value = inputText,
                            onValueChange = { inputText = it },
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            focusRequester = focusRequester
                        )
                    }
                } else if (uiState is AIChatUiState.Recommendation || uiState is AIChatUiState.Idle) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = Color.White,
                        shadowElevation = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(onClick = { 
                                if (isChatHistoryVisible) {
                                    isChatHistoryVisible = false
                                } else if (!isChatBubbleDismissed) {
                                    isChatBubbleDismissed = true
                                } else {
                                    isChatHistoryVisible = true
                                }
                            }) {
                                Icon(
                                    imageVector = if (isChatHistoryVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.toggle_chat_content_desc),
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                            ) {
                                if (isChatHistoryVisible) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 400.dp)
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 24.dp, vertical = 8.dp)
                                    ) {
                                        messages.forEach { msg ->
                                            ChatMessageRow(
                                                message = msg.text,
                                                isFromUser = msg.isFromUser
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                } else if (!isChatBubbleDismissed) {
                                    val lastAiMessage = messages.lastOrNull { !it.isFromUser }
                                    if (lastAiMessage != null) {
                                        Text(
                                            text = lastAiMessage.text,
                                            textAlign = TextAlign.Center,
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp,
                                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            ChatInputBar(
                                value = inputText,
                                onValueChange = { inputText = it },
                                onSend = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    }
                                },
                                focusRequester = focusRequester
                            )
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val state = uiState) {
                    is AIChatUiState.Welcome -> {
                        WelcomeView()
                    }
                    is AIChatUiState.Typing -> {
                        LoadingView(lastMessageText = messages.lastOrNull { it.isFromUser }?.text ?: "")
                    }
                    is AIChatUiState.Recommendation -> {
                        recommendation?.let { rec ->
                            RecommendationView(
                                recommendation = rec,
                                onSave = { showSaveSheet = true }
                            )
                        }
                    }
                    is AIChatUiState.Idle -> {
                        // Chat history view
                        ChatHistoryView(messages = messages)
                    }
                    is AIChatUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message, color = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showWizard) {
        OutfitWizardSheet(
            occasion = selectedOccasion,
            onDismiss = { showWizard = false },
            onFinish = { topic, style, items, dest, date ->
                val itemList = items.joinToString { it.name }
                val wizardResult = context.getString(R.string.wizard_result_message, selectedOccasion, topic, style, itemList)
                viewModel.sendWizardMessage(wizardResult, dest, date)
                showWizard = false
            }
        )
    }

    if (showSaveSheet) {
        SaveOutfitSheet(
            onDismiss = { showSaveSheet = false },
            onSaveToLookbook = { /* Handle */ showSaveSheet = false },
            onSaveToCalendar = { /* Handle */ showSaveSheet = false }
        )
    }
}

@Composable
fun WelcomeView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.welcome_greeting),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            lineHeight = 40.sp,
            color = Color.Black
        )
    }
}

@Composable
fun LoadingView(lastMessageText: String) {
    var displayedPrompt by remember { mutableStateOf("") }
    var showAiStatus by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // User-friendly prompt reveal
    val promptToDisplay = remember(lastMessageText) {
        if (lastMessageText.isBlank()) {
            context.getString(R.string.loading_prompt_default)
        } else {
            // Try to parse wizard message: "Tôi muốn tìm trang phục cho dịp %1$s.\nChủ đề: %2$s\nPhong cách: %3$s\n..."
            val occasionMatch = Regex("dịp ([^.\\n]+)").find(lastMessageText)
            val topicMatch = Regex("Chủ đề: ([^\\n]+)").find(lastMessageText)
            val styleMatch = Regex("Phong cách: ([^\\n]+)").find(lastMessageText)
            
            val occasion = occasionMatch?.groupValues?.get(1)?.trim()
            val topic = topicMatch?.groupValues?.get(1)?.trim()
            val style = styleMatch?.groupValues?.get(1)?.trim()
            
            if (occasion != null) {
                if (occasion.contains("Trường học", ignoreCase = true)) {
                    val topicPart = if (topic != null && !topic.contains("Không có", ignoreCase = true)) topic else "Bài giảng"
                    val stylePart = if (style != null && !style.contains("Không có", ignoreCase = true)) style else "Thường ngày"
                    // "Trang phục [Phong cách] Trường học cho [Chủ đề]."
                    "Trang phục $stylePart Trường học cho $topicPart."
                } else {
                    val topicPart = if (topic != null && !topic.contains("Không có", ignoreCase = true)) topic else "Ở nhà/Thư giãn"
                    val stylePart = if (style != null && !style.contains("Không có", ignoreCase = true)) style else "Thường ngày"
                    // "Trang phục [Phong cách] Hàng ngày cho [Chủ đề]."
                    val occasionDisplay = if (occasion.contains("Hàng ngày", ignoreCase = true)) "Hàng ngày" else occasion
                    "Trang phục $stylePart $occasionDisplay cho $topicPart."
                }
            } else {
                context.getString(R.string.loading_prompt_default)
            }
        }
    }

    LaunchedEffect(promptToDisplay) {
        if (promptToDisplay.isNotBlank()) {
            promptToDisplay.forEachIndexed { index, _ ->
                displayedPrompt = promptToDisplay.substring(0, index + 1)
                delay(30) // Typewriter speed
            }
            delay(500)
        }
        showAiStatus = true
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Centered prompt
        if (displayedPrompt.isNotBlank()) {
            Text(
                text = displayedPrompt,
                modifier = Modifier.padding(bottom = 64.dp).fillMaxWidth(),
                fontSize = 18.sp,
                color = Color.Black.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
        }

        AnimatedVisibility(
            visible = showAiStatus,
            enter = fadeIn() + slideInVertically { it / 2 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Robot Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Face, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    stringResource(R.string.loading_status_ai),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Tip Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(R.string.tip_title), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.tip_content),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationView(
    recommendation: AiRecommendation,
    onSave: () -> Unit
) {
    val app = LocalContext.current.applicationContext as com.example.stylemate.StyleMateApp
    val clothingViewModel: ClothingViewModel = viewModel(
        factory = ClothingViewModelFactory(
            com.example.stylemate.repository.ClothingRepository(
                com.example.stylemate.network.RetrofitClient.stylemateApiService,
                app
            )
        )
    )
    val allItems by clothingViewModel.items.collectAsStateWithLifecycle()
    val itemMap = remember(allItems) { allItems.associateBy { it.id } }

    var isExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = recommendation.styleTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    lineHeight = 22.sp,
                    fontSize = 15.sp,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.see_more), 
                    color = Color.Gray, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { isExpanded = !isExpanded }
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WeatherInfoItem(Icons.Default.CalendarToday, recommendation.date)
                    WeatherInfoItem(Icons.Default.LocationOn, recommendation.location)
                    WeatherInfoItem(Icons.Outlined.Cloud, recommendation.temp)
                }

                Spacer(modifier = Modifier.height(40.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.top_recommendations_from),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = stringResource(R.string.all_clothes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF2196F3)
                    )
                }
            }
        }

        // Dynamic sections from AI
        items(recommendation.sections) { section ->
            val matchedItems = section.matching_item_ids.mapNotNull { itemMap[it] }
            CategoryItemSection(
                label = section.label,
                detail = section.item_description,
                items = matchedItems
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { /* Refresh/New suggest */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    stringResource(R.string.get_outfit_suggestion),
                    color = Color.White, 
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CategoryItemSection(label: String, detail: String, items: List<ClothingItemEntity>) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(detail, color = Color.Gray, fontSize = 14.sp)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        if (items.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .size(100.dp)
                            .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        val imagePath = if (item.imageNoBg.isNotBlank()) item.imageNoBg else item.imageOriginal
                        val fullUrl = if (imagePath.startsWith("http") || imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                            imagePath
                        } else {
                            "${STYLEMATE_BASE_URL.removeSuffix("/")}/${imagePath.removePrefix("/")}"
                        }

                        AsyncImage(
                            model = fullUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        } else {
            val context = LocalContext.current
            Text(
                text = buildAnnotatedString {
                    append(context.getString(R.string.wizard_not_found_prefix))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(detail)
                    }
                    append(context.getString(R.string.wizard_not_found_suffix))
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp),
                color = Color.Gray,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun ChatHistoryView(messages: List<ChatMessage>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(messages) { message ->
            ChatMessageRow(
                message = message.text,
                isFromUser = message.isFromUser
            )
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester
) {
    val context = LocalContext.current
    val placeholders = listOf(
        context.getString(R.string.wizard_chat_placeholder_date),
        context.getString(R.string.wizard_chat_placeholder_len),  
        context.getString(R.string.wizard_chat_placeholder_semi),
        context.getString(R.string.wizard_chat_placeholder_style),
        context.getString(R.string.wizard_chat_placeholder_friday),
        context.getString(R.string.wizard_chat_placeholder_today),
        context.getString(R.string.wizard_chat_placeholder_casual)
    )
    var placeholderIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            placeholderIndex = (placeholderIndex + 1) % placeholders.size
        }
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxWidth()
            .height(56.dp)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF64B5F6), Color(0xFFBA68C8))
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .background(Color.White, shape = RoundedCornerShape(28.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Add media */ }) {
                Icon(
                    imageVector = Icons.Default.Add, 
                    contentDescription = stringResource(R.string.chat_add_content_desc), 
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = {
                    AnimatedContent(
                        targetState = placeholders[placeholderIndex],
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "PlaceholderAnimation"
                    ) { text ->
                        Text(
                            text = text, 
                            color = Color.Gray.copy(alpha = 0.5f), 
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFF64B5F6)
                ),
                singleLine = true
            )
            
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (value.isNotBlank()) Color(0xFF64B5F6) else Color.LightGray.copy(alpha = 0.3f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, 
                    contentDescription = stringResource(R.string.chat_send_content_desc), 
                    tint = Color.White, 
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun WeatherInfoItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black.copy(alpha = 0.6f))
        Text(text, fontSize = 14.sp, color = Color.Black.copy(alpha = 0.6f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveOutfitSheet(
    onDismiss: () -> Unit,
    onSaveToLookbook: () -> Unit,
    onSaveToCalendar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showCalendar by remember { mutableStateOf(false) }

    if (showCalendar) {
        CalendarPickerSheet(
            onDismiss = { showCalendar = false },
            onDateSelected = { /* Handle date */ onDismiss() }
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.save_button), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SaveOptionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Book,
                        label = stringResource(R.string.save_to_lookbook),
                        onClick = onSaveToLookbook
                    )
                    SaveOptionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.CalendarToday,
                        label = stringResource(R.string.save_to_calendar),
                        onClick = { showCalendar = true }
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun SaveOptionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarPickerSheet(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val datePickerState = rememberDatePickerState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.select_date_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
            
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { datePickerState.selectedDateMillis?.let { onDateSelected(it) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (datePickerState.selectedDateMillis != null) Color.Black else Color.LightGray),
                shape = RoundedCornerShape(12.dp),
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text(stringResource(R.string.save_date_button), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── OUTFIT WIZARD ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitWizardSheet(
    occasion: String,
    onDismiss: () -> Unit,
    onFinish: (String, String, List<ClothingItemEntity>, String?, Long?) -> Unit
) {
    val isTravel = occasion.contains("Du lịch", ignoreCase = true) || occasion.contains("Travel", ignoreCase = true)
    val totalSteps = if (isTravel) 4 else 2
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableIntStateOf(1) }
    
    val context2 = LocalContext.current
    var selectedDestination by remember { mutableStateOf<String?>(null) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedTopic by remember { mutableStateOf(context2.getString(R.string.no_topic_option)) }
    var selectedStyle by remember { mutableStateOf(context2.getString(R.string.wizard_style_none)) }
    var selectedItems by remember { mutableStateOf(setOf<ClothingItemEntity>()) }
    
    val app = LocalContext.current.applicationContext as com.example.stylemate.StyleMateApp
    val clothingViewModel: ClothingViewModel = viewModel(
        factory = ClothingViewModelFactory(
            com.example.stylemate.repository.ClothingRepository(
                com.example.stylemate.network.RetrofitClient.stylemateApiService,
                app
            )
        )
    )
    val allItems by clothingViewModel.items.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    IconButton(onClick = { step-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_desc))
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                Text(
                    text = stringResource(if (isTravel) R.string.wizard_step_format_4 else R.string.wizard_step_format, step),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isTravel && step == 1 -> {
                    WizardDestinationStep(
                        onDestinationSelected = {
                            selectedDestination = it
                            step = 2
                        }
                    )
                }
                isTravel && step == 2 -> {
                    WizardDateStep(
                        onDateSelected = {
                            selectedDateMillis = it
                            step = 3
                        }
                    )
                }
                (!isTravel && step == 1) || (isTravel && step == 3) -> {
                    WizardStepOne(
                        occasion = occasion,
                        selectedTopic = selectedTopic,
                        onTopicSelected = { selectedTopic = it },
                        selectedStyle = selectedStyle,
                        onStyleSelected = { selectedStyle = it },
                        onNext = { step++ },
                        onSkip = { onFinish(context2.getString(R.string.no_topic_option), context2.getString(R.string.wizard_style_none), emptyList(), selectedDestination, selectedDateMillis) }
                    )
                }
                (!isTravel && step == 2) || (isTravel && step == 4) -> {
                    WizardStepTwo(
                        allItems = allItems,
                        selectedItems = selectedItems,
                        onItemSelected = { item ->
                            selectedItems = if (selectedItems.contains(item)) {
                                selectedItems - item
                            } else {
                                selectedItems + item
                            }
                        },
                        onFinish = { onFinish(selectedTopic, selectedStyle, selectedItems.toList(), selectedDestination, selectedDateMillis) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardDestinationStep(onDestinationSelected: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val cities = listOf(
        stringResource(R.string.city_tokyo), stringResource(R.string.city_seoul),
        stringResource(R.string.city_bangkok), stringResource(R.string.city_hongkong),
        stringResource(R.string.city_singapore), stringResource(R.string.city_paris),
        stringResource(R.string.city_london), stringResource(R.string.city_rome),
        stringResource(R.string.city_barcelona), stringResource(R.string.city_amsterdam),
        stringResource(R.string.city_newyork), stringResource(R.string.city_losangeles),
        stringResource(R.string.city_toronto), stringResource(R.string.city_vancouver),
        stringResource(R.string.city_riodejaneiro), stringResource(R.string.city_buenosaires),
        stringResource(R.string.city_sydney), stringResource(R.string.city_melbourne),
        stringResource(R.string.city_dubai)
    )
    val filteredCities = cities.filter { it.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(stringResource(R.string.wizard_travel_destination_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            placeholder = { Text(stringResource(R.string.ai_location_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredCities) { city ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onDestinationSelected(city) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(city, fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardDateStep(onDateSelected: (Long) -> Unit) {
    val datePickerState = rememberDatePickerState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text(stringResource(R.string.wizard_travel_date_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = null,
            headline = null,
            modifier = Modifier.weight(1f)
        )
        
        Button(
            onClick = { datePickerState.selectedDateMillis?.let { onDateSelected(it) } },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (datePickerState.selectedDateMillis != null) Color(0xFF1A1A1A) else Color.LightGray),
            enabled = datePickerState.selectedDateMillis != null
        ) {
            Text(stringResource(R.string.next_button), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WizardStepOne(
    occasion: String,
    selectedTopic: String,
    onTopicSelected: (String) -> Unit,
    selectedStyle: String,
    onStyleSelected: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val topics = remember(occasion) {
        if (occasion.contains("Trường học", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_school_regular),
                context.getString(R.string.topic_school_library),
                context.getString(R.string.topic_school_extracurricular),
                context.getString(R.string.topic_school_presentation),
                context.getString(R.string.topic_school_festival)
            )
        } else if (occasion.contains("Du lịch", ignoreCase = true) || occasion.contains("Travel", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_travel_beach),
                context.getString(R.string.topic_travel_city),
                context.getString(R.string.topic_travel_culture),
                context.getString(R.string.topic_travel_nature),
                context.getString(R.string.topic_travel_camping)
            )
        } else if (occasion.contains("Work", ignoreCase = true) || occasion.contains("Làm việc", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_work_office),
                context.getString(R.string.topic_work_meeting),
                context.getString(R.string.topic_work_dinner),
                context.getString(R.string.topic_work_home),
                context.getString(R.string.topic_work_trip)
            )
        } else if (occasion.contains("Party", ignoreCase = true) || occasion.contains("Bữa tiệc", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_party_home),
                context.getString(R.string.topic_party_prom),
                context.getString(R.string.topic_party_slumber),
                context.getString(R.string.topic_party_dinner),
                context.getString(R.string.topic_party_costume),
                context.getString(R.string.topic_party_christmas),
                context.getString(R.string.topic_party_pool),
                context.getString(R.string.topic_party_newyear),
                context.getString(R.string.topic_party_yearend),
                context.getString(R.string.topic_party_cocktail),
                context.getString(R.string.topic_party_club),
                context.getString(R.string.topic_party_birthday)
            )
        } else if (occasion.contains("Date", ignoreCase = true) || occasion.contains("Hẹn hò", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_date_first),
                context.getString(R.string.topic_date_movie),
                context.getString(R.string.topic_date_restaurant),
                context.getString(R.string.topic_date_anniversary),
                context.getString(R.string.topic_date_outdoor),
                context.getString(R.string.topic_date_exhibition)
            )
        } else if (occasion.contains("Đám cưới", ignoreCase = true) || occasion.contains("Wedding", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_wedding_hotel),
                context.getString(R.string.topic_wedding_outdoor),
                context.getString(R.string.topic_wedding_home),
                context.getString(R.string.topic_wedding_church)
            )
        } else if (occasion.contains("Mua sắm", ignoreCase = true) || occasion.contains("Shopping", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_shopping_mall),
                context.getString(R.string.topic_shopping_outlet),
                context.getString(R.string.topic_shopping_supermarket),
                context.getString(R.string.topic_shopping_store),
                context.getString(R.string.topic_shopping_street_market)
            )
        } else if (occasion.contains("Đi dạo", ignoreCase = true) || occasion.contains("Walk", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_walk_park),
                context.getString(R.string.topic_walk_river),
                context.getString(R.string.topic_walk_beach),
                context.getString(R.string.topic_walk_pets),
                context.getString(R.string.topic_walk_picnic)
            )
        } else if (occasion.contains("Tập thể dục", ignoreCase = true) || occasion.contains("Exercise", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_exercise_gym),
                context.getString(R.string.topic_exercise_running),
                context.getString(R.string.topic_exercise_yoga),
                context.getString(R.string.topic_exercise_hiking)
            )
        } else if (occasion.contains("Nhà thờ", ignoreCase = true) || occasion.contains("Church", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_church_service),
                context.getString(R.string.topic_church_small_group),
                context.getString(R.string.topic_church_event)
            )
        } else if (occasion.contains("Tụ họp", ignoreCase = true) || occasion.contains("Gathering", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_gathering_friends),
                context.getString(R.string.topic_gathering_housewarming),
                context.getString(R.string.topic_gathering_class_reunion),
                context.getString(R.string.topic_gathering_family)
            )
        } else if (occasion.contains("Ăn ngoài", ignoreCase = true) || occasion.contains("Dining", ignoreCase = true)) {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_dining_casual),
                context.getString(R.string.topic_dining_brunch),
                context.getString(R.string.topic_dining_family),
                context.getString(R.string.topic_dining_date),
                context.getString(R.string.topic_dining_work),
                context.getString(R.string.topic_dining_formal)
            )
        } else {
            listOf(
                context.getString(R.string.no_topic_option),
                context.getString(R.string.topic_home_relax),
                context.getString(R.string.topic_loungewear),
                context.getString(R.string.topic_cafe_hangout),
                context.getString(R.string.topic_exhibition),
                context.getString(R.string.topic_movie)
            )
        }
    }
    
    val styles = remember(occasion) {
        if (occasion.contains("Trường học", ignoreCase = true)) {
            listOf(
                context.getString(R.string.wizard_style_none),
                context.getString(R.string.style_school_preppy),
                context.getString(R.string.style_school_active),
                context.getString(R.string.style_school_elegant),
                context.getString(R.string.style_school_edgy),
                context.getString(R.string.style_school_vintage)
            )
        } else {
            listOf(
                context.getString(R.string.wizard_style_none),
                context.getString(R.string.style_casual),
                context.getString(R.string.style_classic),
                context.getString(R.string.style_street),
                context.getString(R.string.style_modern),
                context.getString(R.string.style_minimalist),
                context.getString(R.string.style_feminine),
                context.getString(R.string.style_bohemian),
                context.getString(R.string.style_smart_casual),
                context.getString(R.string.style_semi_formal),
                context.getString(R.string.style_formal),
                context.getString(R.string.style_party)
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(stringResource(R.string.topic_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topics.forEach { topic ->
                FilterChip(
                    selected = selectedTopic == topic,
                    onClick = { onTopicSelected(topic) },
                    label = { Text(topic) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(R.string.style_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        FlowRow(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            styles.forEach { style ->
                FilterChip(
                    selected = selectedStyle == style,
                    onClick = { onStyleSelected(style) },
                    label = { Text(style) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Text(stringResource(R.string.next_button), color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.skip_button), color = Color.Gray)
        }
    }
}

@Composable
fun WizardStepTwo(
    allItems: List<ClothingItemEntity>,
    selectedItems: Set<ClothingItemEntity>,
    onItemSelected: (ClothingItemEntity) -> Unit,
    onFinish: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String>(Categories.ALL) }
    
    val filteredItems = remember(allItems, selectedCategory) {
        if (selectedCategory == Categories.ALL) allItems
        else allItems.filter { it.category == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.wizard_step_two_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // All Tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { selectedCategory = Categories.ALL }
            ) {
                Text(
                    stringResource(R.string.wizard_all_label),
                    fontWeight = if (selectedCategory == Categories.ALL) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedCategory == Categories.ALL) Color.Black else Color.Gray
                )
                if (selectedCategory == Categories.ALL) {
                    Box(modifier = Modifier.width(20.dp).height(2.dp).background(Color.Black))
                }
            }

            // Tops Tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { selectedCategory = Categories.TOPS }
            ) {
                Text(
                    stringResource(R.string.wizard_category_tops),
                    fontWeight = if (selectedCategory == Categories.TOPS) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedCategory == Categories.TOPS) Color.Black else Color.Gray
                )
                if (selectedCategory == Categories.TOPS) {
                    Box(modifier = Modifier.width(20.dp).height(2.dp).background(Color.Black))
                }
            }

            // Bottoms Tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { selectedCategory = Categories.BOTTOMS }
            ) {
                Text(
                    stringResource(R.string.wizard_category_bottoms),
                    fontWeight = if (selectedCategory == Categories.BOTTOMS) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedCategory == Categories.BOTTOMS) Color.Black else Color.Gray
                )
                if (selectedCategory == Categories.BOTTOMS) {
                    Box(modifier = Modifier.width(20.dp).height(2.dp).background(Color.Black))
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(
                        R.string.no_matching_items_text,
                        when (selectedCategory) {
                            Categories.TOPS -> stringResource(R.string.wizard_category_tops)
                            Categories.BOTTOMS -> stringResource(R.string.wizard_category_bottoms)
                            else -> stringResource(R.string.all_clothes).lowercase()
                        }
                    ),
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gridItems(filteredItems, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.8f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable { onItemSelected(item) }
                            .border(
                                if (selectedItems.contains(item)) 2.dp else 1.dp,
                                if (selectedItems.contains(item)) Color.Black else Color.LightGray,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        val imagePath = if (item.imageNoBg.isNotBlank()) item.imageNoBg else item.imageOriginal
                        val fullUrl = if (imagePath.startsWith("http") || imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                            imagePath
                        } else {
                            "${STYLEMATE_BASE_URL.removeSuffix("/")}/${imagePath.removePrefix("/")}"
                        }
                        
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                        
                        if (selectedItems.contains(item)) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Text(stringResource(R.string.get_outfit_suggestion), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
