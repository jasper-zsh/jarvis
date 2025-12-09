package pro.sihao.jarvis.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.ui.components.*
import pro.sihao.jarvis.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AnimatedChatScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Animation states
    var isEntering by remember { mutableStateOf(true) }
    val fadeInAlpha by animateFloatAsState(
        targetValue = if (isEntering) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = EaseOutQuad),
        label = "fade_in"
    )
    val slideOffsetY by animateDpAsState(
        targetValue = if (isEntering) 0.dp else 20.dp,
        animationSpec = tween(durationMillis = 300, easing = EaseOutQuad),
        label = "slide_offset"
    )

    // Collect messages and handle animations
    LaunchedEffect(uiState.messages) {
        if (uiState.messages.isNotEmpty()) {
            delay(100) // Small delay for smooth animation
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    // Refresh API key status when screen becomes visible
    LaunchedEffect(Unit) {
        delay(300)
        isEntering = false
        viewModel.refreshApiKeyStatus()
        viewModel.refreshAvailableModels()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AnimatedVisibility(
                visible = !isEntering,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300, easing = EaseOutQuad)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(300, easing = EaseInQuad)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                TopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Jarvis",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            ModelStatusIndicator()
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list with animations
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    AnimatedEmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.messages,
                            key = { index, message -> message.id ?: index }
                        ) { index, message ->
                            AnimatedMessageItem(
                                message = message,
                                animationDelay = index * 50L
                            )
                        }
                    }
                }
            }

            // Animated error message
            AnimatedErrorCard(
                errorMessage = uiState.errorMessage,
                onErrorDismiss = viewModel::clearError
            )

            // Animated API key setup prompt
            AnimatedApiSetupPrompt(
                isVisible = !uiState.hasApiKey,
                onSetupClick = onNavigateToSettings
            )

            // Animated input area
            AnimatedMessageInput(
                message = uiState.inputMessage,
                isLoading = uiState.isLoading,
                isRecording = uiState.isRecording,
                recordingDuration = uiState.recordingDuration,
                onMessageChange = viewModel::onMessageChanged,
                onSendClick = viewModel::sendMessage,
                onVoiceRecordStart = viewModel::startVoiceRecording,
                onVoiceRecordStop = viewModel::stopVoiceRecording,
                onVoiceRecordCancel = viewModel::cancelVoiceRecording,
                onCameraClick = viewModel::capturePhoto,
                onGalleryClick = viewModel::selectPhotoFromGallery,
                permissionStatus = uiState.permissionStatus
            )
        }
    }
}

@Composable
private fun AnimatedEmptyState() {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(600, easing = EaseOutQuad)) +
               slideInVertically(
                   initialOffsetY = { it / 3 },
                   animationSpec = tween(600, easing = EaseOutQuad)
               ),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🤖",
                    fontSize = 40.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to Jarvis!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Start a conversation with your AI assistant",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedMessageItem(
    message: Message,
    animationDelay: Long
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(message.id) {
        delay(animationDelay)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { if (message.isFromUser) it else -it },
            animationSpec = tween(300, easing = EaseOutQuad)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(
            targetOffsetX = { if (message.isFromUser) -it else it },
            animationSpec = tween(300, easing = EaseInQuad)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        MessageBubble(message = message)
    }
}

@Composable
private fun AnimatedErrorCard(
    errorMessage: String?,
    onErrorDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = errorMessage != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300, easing = EaseOutBack)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300, easing = EaseInBack)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onErrorDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedApiSetupPrompt(
    isVisible: Boolean,
    onSetupClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(400, easing = EaseOutBack)
        ) + fadeIn(animationSpec = tween(400)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(400, easing = EaseInBack)
        ) + fadeOut(animationSpec = tween(400))
    ) {
        APISetupPrompt(onSetupClick = onSetupClick)
    }
}

@Composable
private fun AnimatedMessageInput(
    message: String,
    isLoading: Boolean,
    isRecording: Boolean,
    recordingDuration: Long,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onVoiceRecordStart: () -> Unit,
    onVoiceRecordStop: () -> Unit,
    onVoiceRecordCancel: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    permissionStatus: pro.sihao.jarvis.permission.PermissionManager.MediaPermissionsSummary
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(500, easing = EaseOutBack)
        ) + fadeIn(animationSpec = tween(500)),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        MessageInput(
            message = message,
            isLoading = isLoading,
            isRecording = isRecording,
            recordingDuration = recordingDuration,
            onMessageChange = onMessageChange,
            onSendClick = onSendClick,
            permissionStatus = pro.sihao.jarvis.permission.PermissionManager.MediaPermissionsSummary(
                voiceRecordingStatus = pro.sihao.jarvis.permission.PermissionManager.PermissionStatus.NOT_REQUIRED,
                cameraStatus = pro.sihao.jarvis.permission.PermissionManager.PermissionStatus.NOT_REQUIRED,
                galleryStatus = pro.sihao.jarvis.permission.PermissionManager.PermissionStatus.NOT_REQUIRED,
                hasMicrophoneHardware = false,
                hasCameraHardware = false
            )
        )
    }
}
