package pro.sihao.jarvis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import pro.sihao.jarvis.permission.PermissionManager
import pro.sihao.jarvis.domain.model.Message
import pro.sihao.jarvis.ui.components.*
import pro.sihao.jarvis.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
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
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear chat")
                    }
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
            // Messages list
            Box(modifier = Modifier.weight(1f)) {
                val displayMessages = uiState.streamingContent?.let { partial ->
                    uiState.messages + Message(
                        content = partial,
                        timestamp = Date(),
                        isFromUser = false,
                        isLoading = true
                    )
                } ?: uiState.messages

                if (displayMessages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Start a new conversation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your messages and media will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        items(displayMessages) { message ->
                            MessageBubble(
                                message = message,
                                onPhotoClick = { _ ->
                                    // Handle photo tap for full-screen viewing
                                },
                                onVoicePlay = viewModel::playVoiceMessage,
                                onVoicePause = viewModel::pauseVoicePlayback,
                                onCancelLoading = viewModel::cancelPendingResponse
                            )
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

    if (uiState.showPhotoPreview && uiState.pendingPhoto != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingPhoto,
            confirmButton = {
                TextButton(onClick = viewModel::confirmPendingPhoto) {
                    Text("Send photo")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPendingPhoto) {
                    Text("Cancel")
                }
            },
            text = {
                uiState.pendingPhoto?.let { bitmap ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Photo preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )
                    }
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearConversation()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Clear conversation?") },
            text = { Text("This will delete all messages and media in this chat.") },
        )
    }
}
