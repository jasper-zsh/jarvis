package pro.sihao.jarvis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import pro.sihao.jarvis.domain.model.Message
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import pro.sihao.jarvis.permission.PermissionManager
import pro.sihao.jarvis.ui.components.*
import pro.sihao.jarvis.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizedChatScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshPermissions() }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshPermissions() }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshPermissions() }

    val voicePermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    val cameraPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA)
    } else {
        arrayOf(Manifest.permission.CAMERA)
    }
    val galleryPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val handleVoiceStart: () -> Unit = {
        if (uiState.permissionStatus.voiceRecordingStatus == PermissionManager.PermissionStatus.GRANTED) {
            viewModel.startVoiceRecording()
        } else {
            voicePermissionLauncher.launch(voicePermissions)
        }
    }

    val handleCameraClick: () -> Unit = {
        if (uiState.permissionStatus.cameraStatus == PermissionManager.PermissionStatus.GRANTED) {
            viewModel.capturePhoto()
        } else {
            cameraPermissionLauncher.launch(cameraPermissions)
        }
    }

    val handleGalleryClick: () -> Unit = {
        if (uiState.permissionStatus.galleryStatus == PermissionManager.PermissionStatus.GRANTED) {
            viewModel.selectPhotoFromGallery()
        } else {
            galleryPermissionLauncher.launch(galleryPermissions)
        }
    }
    val coroutineScope = rememberCoroutineScope()

    // State for lazy loading
    var allMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 20

    // Collect messages and handle pagination
    LaunchedEffect(uiState.messages) {
        // Only update allMessages if there are new messages or it's the first load
        if (allMessages.isEmpty() || uiState.messages.size > allMessages.size) {
            // For simplicity, we'll use the current approach but could optimize this further
            allMessages = uiState.messages
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(allMessages.size) {
        if (allMessages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(allMessages.size - 1)
            }
        }
    }

    // Handle infinite scrolling
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val firstVisibleIndex = listState.firstVisibleItemIndex

        // Load more messages when user scrolls near the top
        if (firstVisibleIndex <= 3 && !isLoadingMore && currentPage == 0) {
            // This would need to be implemented in the ViewModel for proper pagination
            // For now, we'll keep the current simple approach
        }
    }

    // Refresh API key status and available models when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshApiKeyStatus()
        viewModel.refreshAvailableModels()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
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
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list with optimized rendering
            Box(modifier = Modifier.weight(1f)) {
                if (allMessages.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Start a conversation!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = false
                    ) {
                        itemsIndexed(
                            items = allMessages,
                            key = { index, message -> message.id ?: index }
                        ) { _, message ->
                            // Use itemsIndexed for better performance with large lists
                            MessageBubble(message = message)
                        }

                        // Loading indicator for pagination
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Show error message if present
            uiState.errorMessage?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = viewModel::clearError,
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

            // Show API key setup prompt if needed
            if (!uiState.hasApiKey) {
                APISetupPrompt(onSetupClick = onNavigateToSettings)
            }

            // Input area
            MessageInput(
                message = uiState.inputMessage,
                isLoading = uiState.isLoading,
                isRecording = uiState.isRecording,
                recordingDuration = uiState.recordingDuration,
                onMessageChange = viewModel::onMessageChanged,
                onSendClick = viewModel::sendMessage,
                onVoiceRecordStart = handleVoiceStart,
                onVoiceRecordStop = viewModel::stopVoiceRecording,
                onVoiceRecordCancel = viewModel::cancelVoiceRecording,
                onCameraClick = handleCameraClick,
                onGalleryClick = handleGalleryClick,
                permissionStatus = uiState.permissionStatus
            )
        }
    }
}
