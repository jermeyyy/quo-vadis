package com.jermey.feature2.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.feature2.AuthFlowDestination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.Navigator
import org.koin.compose.koinInject

/**
 * Forgot Password Screen - Part of the auth flow.
 *
 * Demonstrates in-scope navigation within AuthFlow.
 * All navigation from here stays within the AuthFlow stack.
 */
@Screen(AuthFlowDestination.ForgotPassword::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthForgotPasswordScreen(navigator: Navigator = koinInject()) {
    var email by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Forgot Password?",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                "Still within AuthFlow scope",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Enter your email address and we'll send you a link to reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // In-scope navigation: back to Login within AuthFlow
            Button(
                onClick = {
                    navigator.navigate(
                        AuthFlowDestination.Login,
                        NavigationTransitions.SlideHorizontal
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Reset Link (→ Login - In Scope)")
            }

            Spacer(Modifier.weight(1f))

            Text(
                "Navigating back to Login stays within AuthFlow.\n" +
                        "Use back button to pop from the stack.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
