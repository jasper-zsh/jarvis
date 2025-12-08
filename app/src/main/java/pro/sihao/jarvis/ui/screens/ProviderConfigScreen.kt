package pro.sihao.jarvis.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity
import pro.sihao.jarvis.ui.viewmodel.ProviderConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderConfigScreen(
    onBackClick: () -> Unit,
    providerId: Long? = null,
    viewModel: ProviderConfigViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(providerId) {
        viewModel.loadProvider(providerId)
    }

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
                        text = if (providerId == null) "Add Provider" else "Edit Provider",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveProvider() },
                        enabled = !uiState.isLoading && uiState.isFormValid()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Provider Name
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Provider Name *") },
                    placeholder = { Text("e.g., OPENAI") },
                    enabled = !uiState.isLoading,
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Display Name
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = { viewModel.updateDisplayName(it) },
                    label = { Text("Display Name *") },
                    placeholder = { Text("e.g., OpenAI") },
                    enabled = !uiState.isLoading,
                    isError = uiState.displayNameError != null,
                    supportingText = uiState.displayNameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Base URL
                OutlinedTextField(
                    value = uiState.baseUrl,
                    onValueChange = { viewModel.updateBaseUrl(it) },
                    label = { Text("Base URL *") },
                    placeholder = { Text("https://api.openai.com/v1/") },
                    enabled = !uiState.isLoading,
                    isError = uiState.baseUrlError != null,
                    supportingText = uiState.baseUrlError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )

                // Authentication Type
                ExposedDropdownMenuBox(
                    expanded = uiState.authTypeMenuExpanded,
                    onExpandedChange = { viewModel.toggleAuthTypeMenu() }
                ) {
                    OutlinedTextField(
                        value = uiState.authenticationType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Authentication Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.authTypeMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = uiState.authTypeMenuExpanded,
                        onDismissRequest = { viewModel.toggleAuthTypeMenu() }
                    ) {
                        listOf("API_KEY", "BEARER_TOKEN").forEach { authType ->
                            DropdownMenuItem(
                                text = { Text(authType) },
                                onClick = {
                                    viewModel.updateAuthenticationType(authType)
                                    viewModel.toggleAuthTypeMenu()
                                }
                            )
                        }
                    }
                }

                // Default Model
                OutlinedTextField(
                    value = uiState.defaultModel,
                    onValueChange = { viewModel.updateDefaultModel(it) },
                    label = { Text("Default Model") },
                    placeholder = { Text("gpt-3.5-turbo") },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // API Key
                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = { Text("API Key") },
                    placeholder = { Text("Enter your API key...") },
                    enabled = !uiState.isLoading,
                    isError = uiState.apiKeyError != null,
                    supportingText = uiState.apiKeyError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Max Tokens
                OutlinedTextField(
                    value = uiState.maxTokens,
                    onValueChange = { viewModel.updateMaxTokens(it) },
                    label = { Text("Max Tokens") },
                    placeholder = { Text("4096") },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.maxTokensError != null,
                    supportingText = uiState.maxTokensError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    placeholder = { Text("Optional description of the provider") },
                    enabled = !uiState.isLoading,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // Supports Model Discovery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.supportsModelDiscovery,
                        onCheckedChange = { viewModel.updateSupportsModelDiscovery(it) },
                        enabled = !uiState.isLoading
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Supports model discovery",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Error Message
                uiState.error?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}