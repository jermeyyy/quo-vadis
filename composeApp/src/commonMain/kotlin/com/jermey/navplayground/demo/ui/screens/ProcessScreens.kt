package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AssistantDirection
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

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
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.AssistantDirection,
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
    }
}

@Composable
private fun ProcessStep(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Process Step 1 - User type selection (branches the flow)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessStep1Screen(
    initialUserType: String?,
    onNext: (userType: String, data: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialUserType ?: "personal") }
    var name by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 1: Account Type") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                        onSelect = { selectedType = "personal" }
                    )

                    Spacer(Modifier.height(8.dp))

                    AccountTypeOption(
                        type = "business",
                        title = "Business Account",
                        description = "For companies and organizations",
                        icon = Icons.Default.Business,
                        selected = selectedType == "business",
                        onSelect = { selectedType = "business" }
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
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
    }
}

@Composable
private fun AccountTypeOption(
    type: String,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}

/**
 * Process Step 2A - Personal account configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessStep2AScreen(
    previousData: String,
    onNext: (data: String) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 2: Personal Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = birthdate,
                    onValueChange = { birthdate = it },
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
}

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
                        Icon(Icons.Default.ArrowBack, "Back")
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

/**
 * Process Step 3 - Review and confirmation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessStep3Screen(
    previousData: String,
    branch: String,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val dataParts = previousData.split("|")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 3: Review") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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

                        Divider()

                        if (dataParts.isNotEmpty()) {
                            ReviewRow("Name", dataParts.getOrNull(0) ?: "")

                            if (branch == "personal") {
                                ReviewRow("Email", dataParts.getOrNull(1) ?: "")
                                ReviewRow("Birth Date", dataParts.getOrNull(2) ?: "")
                            } else {
                                ReviewRow("Company", dataParts.getOrNull(1) ?: "")
                                ReviewRow("Tax ID", dataParts.getOrNull(2) ?: "")
                                ReviewRow("Industry", dataParts.getOrNull(3) ?: "")
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
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Process Complete Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessCompleteScreen(
    onDone: () -> Unit,
    onRestart: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Complete") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Setup Complete!",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Your account has been successfully configured.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go to Home")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Over")
            }
        }
    }
}

@Composable
private fun ProcessStepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { step ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
            ) {
                LinearProgressIndicator(
                    progress = if (step < currentStep) 1f else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

