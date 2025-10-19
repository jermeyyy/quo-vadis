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
        ProcessStep2BContent(padding, previousData, companyName, taxId, industry, onBack, onNext)
    }
}

@Composable
private fun ProcessStep2BContent(
    padding: PaddingValues,
    previousData: String,
    companyName: String,
    taxId: String,
    industry: String,
    onBack: () -> Unit,
    onNext: (String) -> Unit
) {
    var companyName1 = companyName
    var taxId1 = taxId
    var industry1 = industry
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
                value = companyName1,
                onValueChange = { companyName1 = it },
                label = { Text("Company Name") },
                leadingIcon = { Icon(Icons.Default.Business, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = taxId1,
                onValueChange = { taxId1 = it },
                label = { Text("Tax ID / EIN") },
                leadingIcon = { Icon(Icons.Default.Badge, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = industry1,
                onValueChange = { industry1 = it },
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
                onClick = { onNext("$previousData|$companyName1|$taxId1|$industry1") },
                modifier = Modifier.weight(1f),
                enabled = companyName1.isNotBlank() && taxId1.isNotBlank()
            ) {
                Text("Next")
            }
        }
    }
}
