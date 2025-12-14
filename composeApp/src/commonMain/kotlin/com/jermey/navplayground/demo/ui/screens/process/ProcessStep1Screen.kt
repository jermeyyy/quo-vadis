package com.jermey.navplayground.demo.ui.screens.process

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
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
import com.jermey.navplayground.demo.ui.components.AccountTypeOption
import com.jermey.navplayground.demo.ui.components.ProcessStepIndicator
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Process Step 1 - User type selection (branches the flow)
 */
@Screen(ProcessDestination.Step1::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessStep1Screen(
    destination: ProcessDestination.Step1,
    navigator: Navigator
) {
    var selectedType by remember { mutableStateOf(destination.userType ?: "personal") }
    var name by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 1: Account Type") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
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
            ProcessStep1Content(
                selectedType = selectedType,
                name = name,
                onSelectedTypeChange = { selectedType = it },
                onNameChange = { name = it },
                onBack = { navigator.navigateBack() },
                onNext = { userType, data ->
                    if (userType == "personal") {
                        navigator.navigate(ProcessDestination.Step2A(stepData = data))
                    } else {
                        navigator.navigate(ProcessDestination.Step2B(stepData = data))
                    }
                }
            )
        }
    }
}

@Composable
private fun ProcessStep1Content(
    selectedType: String,
    name: String,
    onSelectedTypeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: (String, String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProcessStepIndicator(currentStep = 1, totalSteps = 4)

        Text(
            "Choose Account Type",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Select the type of account you want to create. This will determine the next steps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Column(Modifier.selectableGroup()) {
            AccountTypeOption(
                type = "personal",
                title = "Personal Account",
                description = "For individual use",
                icon = Icons.Default.Person,
                selected = selectedType == "personal",
                onSelect = { onSelectedTypeChange("personal") }
            )

            Spacer(Modifier.height(8.dp))

            AccountTypeOption(
                type = "business",
                title = "Business Account",
                description = "For companies and organizations",
                icon = Icons.Default.Business,
                selected = selectedType == "business",
                onSelect = { onSelectedTypeChange("business") }
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your Name") },
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
            onClick = { onNext(selectedType, name) },
            modifier = Modifier.weight(1f),
            enabled = name.isNotBlank()
        ) {
            Text("Next")
        }
    }
}
