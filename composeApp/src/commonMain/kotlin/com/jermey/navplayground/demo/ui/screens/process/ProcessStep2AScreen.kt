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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Email
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
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.ui.components.ProcessStepIndicator
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.Navigator
import org.koin.compose.koinInject

/**
 * Process Step 2A - Personal account configuration
 */
@Screen(ProcessDestination.Step2A::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessStep2AScreen(
    destination: ProcessDestination.Step2A,
    navigator: Navigator = koinInject()
) {
    var email by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 2: Personal Details") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        ProcessStep2AContent(
            padding = padding,
            previousData = destination.stepData,
            email = email,
            birthdate = birthdate,
            onEmailChange = { email = it },
            onBirthdateChange = { birthdate = it },
            onBack = { navigator.navigateBack() },
            onNext = { data ->
                navigator.navigate(
                    ProcessDestination.Step3(
                        previousData = data,
                        branch = "personal"
                    )
                )
            }
        )
    }
}

@Composable
private fun ProcessStep2AContent(
    padding: PaddingValues,
    previousData: String,
    email: String,
    birthdate: String,
    onEmailChange: (String) -> Unit,
    onBirthdateChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: (String) -> Unit
) {
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
                "Personal Information",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                "Hello $previousData! Please provide your personal details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = birthdate,
                onValueChange = onBirthdateChange,
                label = { Text("Birth Date (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
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
                onClick = { onNext("$previousData|$email|$birthdate") },
                modifier = Modifier.weight(1f),
                enabled = email.isNotBlank() && birthdate.isNotBlank()
            ) {
                Text("Next")
            }
        }
    }
}
