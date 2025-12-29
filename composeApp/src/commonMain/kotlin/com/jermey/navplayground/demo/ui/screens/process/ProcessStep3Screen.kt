package com.jermey.navplayground.demo.ui.screens.process

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.ui.components.ProcessStepIndicator
import com.jermey.navplayground.demo.ui.components.ReviewRow
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.Navigator
import org.koin.compose.koinInject

private const val DATA_INDEX_NAME = 0
private const val DATA_INDEX_SECOND_FIELD = 1
private const val DATA_INDEX_THIRD_FIELD = 2
private const val DATA_INDEX_INDUSTRY = 3

/**
 * Process Step 3 - Review and confirmation
 */
@Screen(ProcessDestination.Step3::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessStep3Screen(
    destination: ProcessDestination.Step3,
    navigator: Navigator = koinInject()
) {
    val dataParts = destination.previousData.split("|")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 3: Review") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        ProcessStep3Content(
            padding = padding,
            branch = destination.branch,
            dataParts = dataParts,
            onBack = { navigator.navigateBack() },
            onComplete = { navigator.navigate(ProcessDestination.Complete) }
        )
    }
}

@Composable
private fun ProcessStep3Content(
    padding: PaddingValues,
    branch: String,
    dataParts: List<String>,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProcessStepIndicator(currentStep = 3, totalSteps = 4)

            Text(
                "Review Your Information",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                "Please review the information you've provided before completing the setup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Account Type",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        branch.uppercase(),
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider()

                    if (dataParts.isNotEmpty()) {
                        ReviewRow("Name", dataParts.getOrNull(DATA_INDEX_NAME) ?: "")

                        if (branch == "personal") {
                            ReviewRow(
                                "Email",
                                dataParts.getOrNull(DATA_INDEX_SECOND_FIELD) ?: ""
                            )
                            ReviewRow(
                                "Birth Date",
                                dataParts.getOrNull(DATA_INDEX_THIRD_FIELD) ?: ""
                            )
                        } else {
                            ReviewRow(
                                "Company",
                                dataParts.getOrNull(DATA_INDEX_SECOND_FIELD) ?: ""
                            )
                            ReviewRow(
                                "Tax ID",
                                dataParts.getOrNull(DATA_INDEX_THIRD_FIELD) ?: ""
                            )
                            ReviewRow(
                                "Industry",
                                dataParts.getOrNull(DATA_INDEX_INDUSTRY) ?: ""
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Info, null)
                    Text(
                        "By continuing, you agree to our Terms of Service and Privacy Policy.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f)
            ) {
                Text("Complete")
            }
        }
    }
}
