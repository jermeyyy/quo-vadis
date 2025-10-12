package com.jermey.navplayground.demo.ui.screens.process

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.components.ProcessStepIndicator

/**
 * Process Step 2B - Business account configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessStep2BScreen(
    previousData: String,
    onNext: (data: String) -> Unit,
    onBack: () -> Unit
) {
    var companyName by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 2: Business Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProcessStepIndicator(currentStep = 2, totalSteps = 4)

                Text(
                    "Business Information",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    "Hello $previousData! Please provide your business details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name") },
                    leadingIcon = { Icon(Icons.Default.Business, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = taxId,
                    onValueChange = { taxId = it },
                    label = { Text("Tax ID / EIN") },
                    leadingIcon = { Icon(Icons.Default.Badge, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = industry,
                    onValueChange = { industry = it },
                    label = { Text("Industry") },
                    leadingIcon = { Icon(Icons.Default.Work, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
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
                    onClick = { onNext("$previousData|$companyName|$taxId|$industry") },
                    modifier = Modifier.weight(1f),
                    enabled = companyName.isNotBlank() && taxId.isNotBlank()
                ) {
                    Text("Next")
                }
            }
        }
    }
}
