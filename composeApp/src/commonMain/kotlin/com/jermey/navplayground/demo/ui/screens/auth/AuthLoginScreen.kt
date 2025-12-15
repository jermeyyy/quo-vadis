package com.jermey.navplayground.demo.ui.screens.auth

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.AuthFlowDestination
import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Auth Login Screen - Entry point for the auth flow.
 *
 * Demonstrates:
 * - In-scope navigation: Login → Register (stays within AuthFlow stack)
 * - Out-of-scope navigation: Login → MainTabs (navigates above AuthFlow stack)
 */
@Screen(AuthFlowDestination.Login::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthLoginScreen(navigator: Navigator) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login") },
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
                "Welcome Back",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                "Scope-Aware Navigation Demo",
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

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            // In-scope navigation: stays within AuthFlow stack
            TextButton(
                onClick = {
                    navigator.navigate(
                        AuthFlowDestination.ForgotPassword,
                        NavigationTransitions.SlideHorizontal
                    )
                }
            ) {
                Text("Forgot Password?")
            }

            Spacer(Modifier.height(8.dp))

            // OUT-OF-SCOPE navigation: MainTabs is not in AuthFlow scope
            // With scope-aware navigation, this will navigate ABOVE the AuthFlow stack
            Button(
                onClick = {
                    navigator.navigate(
                        MainTabs.HomeTab,
                        NavigationTransitions.SlideHorizontal
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login (→ MainTabs - Out of Scope)")
            }

            // In-scope navigation: stays within AuthFlow stack
            OutlinedButton(
                onClick = {
                    navigator.navigate(
                        AuthFlowDestination.Register,
                        NavigationTransitions.SlideHorizontal
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Account (→ Register - In Scope)")
            }

            Spacer(Modifier.weight(1f))

            Text(
                "In-scope navigation stays within AuthFlow.\n" +
                    "Out-of-scope navigation goes above AuthFlow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
