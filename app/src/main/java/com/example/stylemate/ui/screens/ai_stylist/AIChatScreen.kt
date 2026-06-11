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
    var showSaveSheet by remember { mutableStateOf(false) }
    var isChatHistoryVisible by remember { mutableStateOf(false) }
    
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
                        stringResource(R.string.prompt_wedding)
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
                            IconButton(onClick = { isChatHistoryVisible = !isChatHistoryVisible }) {
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
                                } else {
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
            onDismiss = { showWizard = false },
            onFinish = { topic, style, items ->
                val itemList = items.joinToString { it.name }
                val wizardResult = context.getString(R.string.wizard_result_message, topic, style, itemList)
                viewModel.sendMessage(wizardResult)
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
    var showDetails by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(500)
        showDetails = true
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // User's request bubble
        Surface(
            color = Color.Gray.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.End).padding(bottom = 48.dp)
        ) {
            Text(
                lastMessageText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 16.sp,
                color = Color.Black.copy(alpha = 0.8f)
            )
        }

        AnimatedVisibility(
            visible = showDetails,
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
                    stringResource(R.string.loading_generating_outfit),
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
                        lineHeight = 20.sp,
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
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = recommendation.styleTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.7f),
                    lineHeight = 22.sp,
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
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.top_recommendations_from),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.all_clothes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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
                Text(stringResource(R.string.get_outfit_suggestion), color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
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
                        val fullUrl = if (item.imageNoBg.startsWith("http")) item.imageNoBg 
                                     else "${STYLEMATE_BASE_URL.removeSuffix("/")}${item.imageNoBg}"
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
    onDismiss: () -> Unit,
    onFinish: (String, String, List<ClothingItemEntity>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableIntStateOf(1) }
    
    val context2 = LocalContext.current
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
                if (step == 2) {
                    IconButton(onClick = { step = 1 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_desc))
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                Text(
                    text = stringResource(R.string.wizard_step_format, step),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (step == 1) {
                WizardStepOne(
                    selectedTopic = selectedTopic,
                    onTopicSelected = { selectedTopic = it },
                    selectedStyle = selectedStyle,
                    onStyleSelected = { selectedStyle = it },
                    onNext = { step = 2 },
                    onSkip = { onFinish(context2.getString(R.string.no_topic_option), context2.getString(R.string.wizard_style_none), emptyList()) }
                )
            } else {
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
                    onFinish = { onFinish(selectedTopic, selectedStyle, selectedItems.toList()) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun WizardStepOne(
    selectedTopic: String,
    onTopicSelected: (String) -> Unit,
    selectedStyle: String,
    onStyleSelected: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val topics = listOf(
        context.getString(R.string.no_topic_option),
        context.getString(R.string.topic_home_relax),
        context.getString(R.string.topic_loungewear),
        context.getString(R.string.topic_cafe_hangout),
        context.getString(R.string.topic_exhibition),
        context.getString(R.string.topic_movie)
    )
    val styles = listOf(
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
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.wizard_step_two_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.wizard_all_label), color = Color.Gray)
            Column {
                Text(stringResource(R.string.wizard_category_tops), fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.width(20.dp).height(2.dp).background(Color.Black))
            }
            Text(stringResource(R.string.wizard_category_bottoms), color = Color.Gray)
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridItems(allItems) { item ->
                Box(
                    modifier = Modifier
                        .aspectRatio(0.8f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { onItemSelected(item) }
                        .border(if (selectedItems.contains(item)) 2.dp else 0.dp, Color.Black, RoundedCornerShape(8.dp))
                ) {
                    val fullUrl = if (item.imageNoBg.startsWith("http")) item.imageNoBg 
                                 else "${STYLEMATE_BASE_URL.removeSuffix("/")}${item.imageNoBg}"
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
