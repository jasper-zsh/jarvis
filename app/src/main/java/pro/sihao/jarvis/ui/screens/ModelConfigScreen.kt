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
import pro.sihao.jarvis.data.database.entity.ModelConfigEntity
import pro.sihao.jarvis.ui.viewmodel.ModelConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigScreen(
    onBackClick: () -> Unit,
    providerId: Long,
    providerName: String,
    modelId: Long? = null,
    viewModel: ModelConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(modelId) {
        viewModel.loadModel(modelId, providerId)
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
                        text = if (modelId == null) "Add Model" else "Edit Model",
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
                        onClick = { viewModel.saveModel() },
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
                // Model Name
                OutlinedTextField(
                    value = uiState.modelName,
                    onValueChange = { viewModel.updateModelName(it) },
                    label = { Text("Model Name *") },
                    placeholder = { Text("e.g., gpt-3.5-turbo") },
                    enabled = !uiState.isLoading,
                    isError = uiState.modelNameError != null,
                    supportingText = uiState.modelNameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Display Name
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = { viewModel.updateDisplayName(it) },
                    label = { Text("Display Name *") },
                    placeholder = { Text("e.g., GPT-3.5 Turbo") },
                    enabled = !uiState.isLoading,
                    isError = uiState.displayNameError != null,
                    supportingText = uiState.displayNameError?.let { { Text(it) } },
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

                // Context Window
                OutlinedTextField(
                    value = uiState.contextWindow,
                    onValueChange = { viewModel.updateContextWindow(it) },
                    label = { Text("Context Window") },
                    placeholder = { Text("8192") },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.contextWindowError != null,
                    supportingText = uiState.contextWindowError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Temperature
                OutlinedTextField(
                    value = uiState.temperature,
                    onValueChange = { viewModel.updateTemperature(it) },
                    label = { Text("Temperature") },
                    placeholder = { Text("0.7") },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.temperatureError != null,
                    supportingText = uiState.temperatureError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Top P
                OutlinedTextField(
                    value = uiState.topP,
                    onValueChange = { viewModel.updateTopP(it) },
                    label = { Text("Top P") },
                    placeholder = { Text("1.0") },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.topPError != null,
                    supportingText = uiState.topPError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Input Cost per 1K
                OutlinedTextField(
                    value = uiState.inputCostPer1K,
                    onValueChange = { viewModel.updateInputCostPer1K(it) },
                    label = { Text("Input Cost per 1K tokens") },
                    placeholder = { Text("0.001") },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.inputCostPer1KError != null,
                    supportingText = uiState.inputCostPer1KError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Output Cost per 1K
                OutlinedTextField(
                    value = uiState.outputCostPer1K,
                    onValueChange = { viewModel.updateOutputCostPer1K(it) },
                    label = { Text("Output Cost per 1K tokens") },
                    placeholder = { Text("0.002") },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.outputCostPer1KError != null,
                    supportingText = uiState.outputCostPer1KError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    placeholder = { Text("Optional description of the model") },
                    enabled = !uiState.isLoading,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // Set as Default
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.isDefault,
                        onCheckedChange = { viewModel.updateIsDefault(it) },
                        enabled = !uiState.isLoading
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set as default model for this provider",
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