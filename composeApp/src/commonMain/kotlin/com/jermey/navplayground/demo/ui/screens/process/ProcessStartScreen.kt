package com.jermey.navplayground.demo.ui.screens.process

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssistantDirection
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.components.ProcessStep
import com.jermey.navplayground.demo.ui.components.ProcessStepIndicator

/**
 * Process Start Screen - Entry point for the wizard/process flow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessStartScreen(
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Process") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, "Cancel")
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
            ProcessStartContent(onStart)
        }
    }
}

@Composable
private fun ProcessStartContent(onStart: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Filled.AssistantDirection,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            "Welcome to Setup",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            "This wizard demonstrates a multi-step process with branching logic based on user choices.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        ProcessStepIndicator(currentStep = 0, totalSteps = 4)

        Spacer(Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("What you'll do:", style = MaterialTheme.typography.titleMedium)
                ProcessStep("Choose account type", Icons.Default.Person)
                ProcessStep("Configure settings", Icons.Default.Settings)
                ProcessStep("Review choices", Icons.Default.CheckCircle)
                ProcessStep("Complete setup", Icons.Default.Done)
            }
        }
    }

    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Start Setup")
    }
}
