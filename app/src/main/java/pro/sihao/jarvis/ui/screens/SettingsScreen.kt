package pro.sihao.jarvis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pro.sihao.jarvis.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onBackClick()
        }
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
            // API Configuration Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "API Configuration",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // API Provider Selection
                    Text(
                        text = "API Provider",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedProvider,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            viewModel.getAvailableProviders().forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider) },
                                    onClick = {
                                        viewModel.onProviderChanged(provider)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Base URL
                    Text(
                        text = "Base URL (Optional)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = uiState.baseUrl,
                        onValueChange = viewModel::onBaseUrlChanged,
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.openai.com/") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                text = "Leave empty to use provider default. Custom endpoints must support OpenAI-compatible format.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    // API Key
                    Text(
                        text = "API Key",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = viewModel::onApiKeyChanged,
                        label = { Text("API Key") },
                        placeholder = { Text(getApiKeyPlaceholder(uiState.selectedProvider)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        isError = uiState.errorMessage != null,
                        supportingText = uiState.errorMessage?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    )
                }
            }

            // Model Configuration Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Model Configuration",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Model Name
                    Text(
                        text = "Model",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Fetch models when API key and base URL are available
                    LaunchedEffect(uiState.apiKey, uiState.baseUrl) {
                        if (uiState.apiKey.isNotBlank() && uiState.baseUrl.isNotBlank()) {
                            viewModel.fetchAvailableModels(uiState.apiKey, uiState.baseUrl)
                        }
                    }

                    if (uiState.isLoadingModels) {
                        OutlinedTextField(
                            value = "Loading models...",
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        )
                    } else if (uiState.modelsError != null) {
                        OutlinedTextField(
                            value = uiState.modelName,
                            onValueChange = viewModel::onModelChanged,
                            label = { Text("Model Name") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = true,
                            supportingText = {
                                Text(
                                    text = "Error fetching models: ${uiState.modelsError}. Using manual input.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    } else if (uiState.availableModels.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = uiState.modelName,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Model") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                uiState.availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            viewModel.onModelChanged(model)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = uiState.modelName,
                            onValueChange = viewModel::onModelChanged,
                            label = { Text("Model Name") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                Text(
                                    text = "e.g., gpt-3.5-turbo, deepseek-chat, llama2",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }

                    // Temperature Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Temperature",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.temperature.toString(),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = uiState.temperature,
                            onValueChange = viewModel::onTemperatureChanged,
                            valueRange = 0.0f..2.0f,
                            steps = 19
                        )
                    }

                    // Max Tokens
                    OutlinedTextField(
                        value = uiState.maxTokens.toString(),
                        onValueChange = viewModel::onMaxTokensChanged,
                        label = { Text("Max Tokens") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        supportingText = {
                            Text(
                                text = "Maximum response length (100-4000)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Save Button
            Button(
                onClick = viewModel::saveSettings,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.apiKey.isNotBlank() && !uiState.isLoading && uiState.errorMessage == null
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save All Settings")
                }
            }

            // App Info Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Current Configuration",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoRow("Provider", uiState.selectedProvider)
                        InfoRow("Base URL", if (uiState.baseUrl.isBlank()) "Default" else uiState.baseUrl)
                        InfoRow("Model", uiState.modelName)
                        InfoRow("Temperature", String.format("%.1f", uiState.temperature))
                        InfoRow("Max Tokens", uiState.maxTokens.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun getApiKeyPlaceholder(provider: String): String {
    return when (provider) {
        "OPENAI", "TOGETHER_AI" -> "sk-..."
        "GROQ" -> "gsk_..."
        "DEEPSEEK" -> "Enter your DeepSeek API key"
        "LOCAL_AI" -> "Enter your API key (if required)"
        else -> "Enter your API key"
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium
        )
    }
}