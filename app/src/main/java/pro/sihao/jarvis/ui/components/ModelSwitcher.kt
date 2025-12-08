package pro.sihao.jarvis.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import pro.sihao.jarvis.ui.viewmodel.ChatViewModel
import pro.sihao.jarvis.ui.viewmodel.ModelTestViewModel
import pro.sihao.jarvis.data.repository.ModelConfiguration

@Composable
fun ModelStatusIndicator(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSwitcher by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clickable { showSwitcher = true }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Model settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uiState.currentModel?.displayName ?: "No Model",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            uiState.currentModel?.let { model ->
                Text(
                    text = "${model.modelName} • ${model.temperature}°",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (uiState.availableModels.size > 1) {
            Text(
                text = "▼",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Model switcher popup
    if (showSwitcher && uiState.availableModels.isNotEmpty()) {
        ModelSwitcherPopup(
            availableModels = uiState.availableModels,
            currentModel = uiState.currentModel,
            onModelSelected = { model ->
                viewModel.selectModel(model)
                showSwitcher = false
            },
            onDismiss = { showSwitcher = false },
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }

    // Model switching error
    uiState.modelSwitchingError?.let { error ->
        LaunchedEffect(error) {
            // Auto-dismiss error after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.clearModelSwitchingError()
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 12.sp,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun ModelSwitcherPopup(
    availableModels: List<ModelConfiguration>,
    currentModel: ModelConfiguration?,
    onModelSelected: (ModelConfiguration) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModelTestViewModel = hiltViewModel()
) {
    var showTestDialog by remember { mutableStateOf<ModelConfiguration?>(null) }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Model",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Models list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableModels) { model ->
                        ModelSwitcherItem(
                            model = model,
                            isSelected = model.id == currentModel?.id,
                            onSelected = {
                                onModelSelected(model)
                            },
                            onTestModel = {
                                showTestDialog = model
                            }
                        )
                    }
                }

                // Test note
                Text(
                    text = "Tap • to test model performance",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // Test dialog
    showTestDialog?.let { model ->
        ModelTestDialog(
            model = model,
            onDismiss = { showTestDialog = null }
        )
    }
}

@Composable
private fun ModelSwitcherItem(
    model: ModelConfiguration,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onTestModel: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                backgroundColor,
                RoundedCornerShape(8.dp)
            )
            .clickable { onSelected() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.modelName,
                    fontSize = 12.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                model.maxTokens?.let { tokens ->
                    Text(
                        text = "${tokens}tk",
                        fontSize = 10.sp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                if (model.temperature > 0f) {
                    Text(
                        text = "${model.temperature}°",
                        fontSize = 10.sp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        if (isSelected) {
            Text(
                text = "✓",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }

        // Test button
        IconButton(
            onClick = onTestModel,
            modifier = Modifier.size(32.dp)
        ) {
            Text(
                text = "•",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}