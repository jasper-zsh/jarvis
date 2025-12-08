package pro.sihao.jarvis.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import pro.sihao.jarvis.data.service.ProviderHealthResult
import pro.sihao.jarvis.ui.viewmodel.ProviderListViewModel

@Composable
fun ProviderHealthIndicator(
    providerId: Long,
    providerName: String,
    modifier: Modifier = Modifier,
    viewModel: ProviderListViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var healthResult by remember { mutableStateOf<ProviderHealthResult?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    fun checkHealth() {
        coroutineScope.launch {
            isChecking = true
            try {
                healthResult = viewModel.checkProviderHealth(providerId)
            } catch (e: Exception) {
                healthResult = ProviderHealthResult(
                    providerId = providerId,
                    providerName = providerName,
                    isHealthy = false,
                    responseTime = 0L,
                    error = e.message ?: "Unknown error"
                )
            } finally {
                isChecking = false
            }
        }
    }

    // Auto-check health when component first loads
    LaunchedEffect(providerId) {
        if (healthResult == null && !isChecking) {
            checkHealth()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isChecking -> MaterialTheme.colorScheme.surfaceVariant
                healthResult?.isHealthy == true -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    isChecking -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    healthResult?.isHealthy == true -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Healthy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Unhealthy",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = when {
                            isChecking -> "Checking..."
                            healthResult?.isHealthy == true -> "Connected"
                            else -> "Connection Failed"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isChecking -> MaterialTheme.colorScheme.onSurfaceVariant
                            healthResult?.isHealthy == true -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )

                    healthResult?.let { result ->
                        if (result.isHealthy) {
                            Text(
                                text = "${result.responseTime}ms • ${result.availableModels?.size ?: 0} models",
                                fontSize = 10.sp,
                                color = if (result.isHealthy) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                        } else {
                            result.error?.let { error ->
                                Text(
                                    text = error,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Refresh button
            IconButton(
                onClick = { checkHealth() },
                modifier = Modifier.size(24.dp),
                enabled = !isChecking
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Refresh health",
                    tint = if (isChecking) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProviderHealthDialog(
    onDismiss: () -> Unit,
    viewModel: ProviderListViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    var healthResults by remember { mutableStateOf<List<ProviderHealthResult>>(emptyList()) }
    var isChecking by remember { mutableStateOf(false) }

    fun checkAllProviders() {
        coroutineScope.launch {
            isChecking = true
            try {
                healthResults = viewModel.checkAllProvidersHealth()
            } catch (e: Exception) {
                // Handle error silently or show error state
            } finally {
                isChecking = false
            }
        }
    }

    // Auto-check when dialog opens
    LaunchedEffect(Unit) {
        if (healthResults.isEmpty()) {
            checkAllProviders()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Provider Health Check",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isChecking && healthResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    healthResults.forEach { result ->
                        ProviderHealthSummaryItem(result = result)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { checkAllProviders() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isChecking
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Refresh All")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ProviderHealthSummaryItem(result: ProviderHealthResult) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (result.isHealthy) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (result.isHealthy) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = if (result.isHealthy) "Healthy" else "Unhealthy",
                    tint = if (result.isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        text = result.providerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (result.isHealthy) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )

                    if (result.isHealthy) {
                        Text(
                            text = "Response: ${result.responseTime}ms • Models: ${result.availableModels?.size ?: 0}",
                            fontSize = 12.sp,
                            color = if (result.isHealthy) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    } else {
                        result.error?.let { error ->
                            Text(
                                text = error,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}