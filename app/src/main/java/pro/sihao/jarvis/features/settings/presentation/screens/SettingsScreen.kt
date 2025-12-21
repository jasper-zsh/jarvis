package pro.sihao.jarvis.features.settings.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pro.sihao.jarvis.features.settings.presentation.viewmodel.SettingsViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToGlasses: () -> Unit,
    onNavigateToRealtime: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    var showApiKey by remember { mutableStateOf(false) }

    // TextFieldValue states for form inputs
    var baseUrlTextFieldValue by remember { mutableStateOf(TextFieldValue(uiState.baseUrl)) }
    var apiKeyTextFieldValue by remember { mutableStateOf(TextFieldValue(uiState.apiKey)) }
    var botIdTextFieldValue by remember { mutableStateOf(TextFieldValue(uiState.botId)) }

    // Show success/error messages
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            // Success message is handled in the UI
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        // Error message is handled in the UI
    }

    // Sync TextFieldValue with uiState
    LaunchedEffect(uiState.baseUrl) {
        baseUrlTextFieldValue = baseUrlTextFieldValue.copy(text = uiState.baseUrl)
    }
    LaunchedEffect(uiState.apiKey) {
        apiKeyTextFieldValue = apiKeyTextFieldValue.copy(text = uiState.apiKey)
    }
    LaunchedEffect(uiState.botId) {
        botIdTextFieldValue = botIdTextFieldValue.copy(text = uiState.botId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Success/Error Messages
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { settingsViewModel.clearError() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            if (uiState.saveSuccess) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings saved successfully!",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { settingsViewModel.clearSuccessMessage() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            // PipeCat Configuration Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "PipeCat Configuration",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Configure PipeCat server connection using HTTP/HTTPS URLs for SmallWebRTC transport",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "For local development: use http://localhost:7860 (run server from pipecat-examples)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Base URL
                    OutlinedTextField(
                        value = baseUrlTextFieldValue,
                        onValueChange = { newValue ->
                            settingsViewModel.updateBaseUrl(newValue.text)
                            baseUrlTextFieldValue = newValue
                        },
                        label = { Text("Server URL") },
                        placeholder = { Text("http://localhost:7860") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        isError = uiState.baseUrlError != null,
                        supportingText = {
                            val baseUrlError = uiState.baseUrlError
                            if (baseUrlError != null) {
                                Text(baseUrlError, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text(
                                    "Use HTTP/HTTPS URLs for SmallWebRTC transport",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // API Key
                    OutlinedTextField(
                        value = apiKeyTextFieldValue,
                        onValueChange = { newValue ->
                            settingsViewModel.updateApiKey(newValue.text)
                            apiKeyTextFieldValue = newValue
                        },
                        label = { Text("API Key") },
                        placeholder = { Text("Enter your API key") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bot ID
                    OutlinedTextField(
                        value = botIdTextFieldValue,
                        onValueChange = { newValue: TextFieldValue ->
                            settingsViewModel.updateBotId(newValue.text)
                            botIdTextFieldValue = newValue
                        },
                        label = { Text("Bot ID") },
                        placeholder = { Text("jarvis-assistant") },
                        isError = uiState.botIdError != null,
                        supportingText = {
                            val botIdError = uiState.botIdError
                            if (botIdError != null) {
                                Text(botIdError, color = MaterialTheme.colorScheme.error)
                            } else {
                                null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Audio/Video Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Microphone",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Enable voice input",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.enableMic,
                            onCheckedChange = { settingsViewModel.updateMicrophoneEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Camera",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Enable video input",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.enableCam,
                            onCheckedChange = { settingsViewModel.updateCameraEnabled(it) }
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { settingsViewModel.resetToDefaults() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSaving
                        ) {
                            Text(if (uiState.isSaving) "Resetting..." else "Reset to Defaults")
                        }

                        Button(
                            onClick = { settingsViewModel.saveSettings() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSaving && uiState.hasChanges
                        ) {
                            Text(if (uiState.isSaving) "Saving..." else "Save Settings")
                        }
                    }
                }
            }

            // Rokid Glasses Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Rokid Glasses",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Connect, configure, and manage Rokid glasses integration",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onNavigateToGlasses,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Glasses Tab")
                    }
                }
            }

            // Real-time Voice Chat Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Real-time Voice Chat",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Start a voice conversation with AI assistant",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onNavigateToRealtime,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Voice Chat")
                    }
                }
            }

            // About Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "About",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Jarvis AI Assistant\n" +
                              "Version: 1.0.0\n" +
                              "Real-time voice chat with glasses integration",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
