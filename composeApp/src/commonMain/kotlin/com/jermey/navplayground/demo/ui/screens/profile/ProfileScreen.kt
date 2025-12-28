package com.jermey.navplayground.demo.ui.screens.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.flowmvi.rememberContainer
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.compose.dsl.subscribe

/**
 * Profile screen demonstrating FlowMVI patterns.
 *
 * Features showcased:
 * - StoreScreen pattern for automatic state subscription
 * - Loading, content, and error states
 * - Form editing with validation
 * - Intent dispatching for user actions
 * - Action handling for side effects (toasts, navigation)
 * - Koin dependency injection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(MainTabs.ProfileTab::class)
@Composable
fun ProfileScreen(
    container: Store<ProfileState, ProfileIntent, ProfileAction> = rememberContainer<ProfileContainer, ProfileState, ProfileIntent, ProfileAction>()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val state by container.subscribe { action ->
        scope.launch {
            when (action) {
                is ProfileAction.ShowToast -> {
                    snackbarHostState.showSnackbar(
                        message = action.message,
                        duration = SnackbarDuration.Short
                    )
                }

                is ProfileAction.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = action.error,
                        duration = SnackbarDuration.Long
                    )
                }

                is ProfileAction.ProfileSaved -> {
                    // Already handled by ShowToast
                }

                is ProfileAction.LogoutSuccess -> {
                    // Navigation already handled in container
                }

                is ProfileAction.ValidationFailed -> {
                    snackbarHostState.showSnackbar(
                        message = "Please fix validation errors",
                        duration = SnackbarDuration.Short
                    )
                }

                is ProfileAction.NetworkError -> {
                    snackbarHostState.showSnackbar(
                        message = "Network error: ${action.message}",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    if ((state as? ProfileState.Content)?.isEditing ?: false) {
                        IconButton(onClick = { container.intent(ProfileIntent.NavigateToSettings) }) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state) {
                is ProfileState.Loading -> LoadingContent()
                is ProfileState.Content -> ProfileContent(
                    state = state as ProfileState.Content,
                    onIntent = container::intent
                )

                is ProfileState.Error -> ErrorContent(
                    message = (state as ProfileState.Error).message,
                    onRetry = { container.intent(ProfileIntent.LoadProfile) }
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading profile...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileState.Content,
    onIntent: (ProfileIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar section
        ProfileAvatar(user = state.user)

        Divider()

        if (state.isEditing) {
            // Edit mode
            ProfileEditForm(
                state = state,
                onIntent = onIntent
            )
        } else {
            // View mode
            ProfileViewContent(
                user = state.user,
                onIntent = onIntent
            )
        }
    }
}

@Composable
private fun ProfileAvatar(user: UserData) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Avatar placeholder
        Surface(
            modifier = Modifier.size(100.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Member since ${user.joinedDate}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileViewContent(
    user: UserData,
    onIntent: (ProfileIntent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Email
        ProfileInfoItem(
            label = "Email",
            value = user.email,
            icon = Icons.Default.Email
        )

        // Bio
        ProfileInfoItem(
            label = "Bio",
            value = user.bio,
            icon = Icons.Default.Info
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Button(
            onClick = { onIntent(ProfileIntent.StartEditing) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile")
        }

        OutlinedButton(
            onClick = { onIntent(ProfileIntent.Logout) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.ExitToApp, "Logout", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

@Composable
private fun ProfileInfoItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ProfileEditForm(
    state: ProfileState.Content,
    onIntent: (ProfileIntent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name field
        OutlinedTextField(
            value = state.editedName,
            onValueChange = { onIntent(ProfileIntent.UpdateName(it)) },
            label = { Text("Name") },
            leadingIcon = { Icon(Icons.Default.Person, "Name") },
            isError = state.validationErrors.containsKey("name"),
            supportingText = state.validationErrors["name"]?.let { { Text(it) } },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        // Email field
        OutlinedTextField(
            value = state.editedEmail,
            onValueChange = { onIntent(ProfileIntent.UpdateEmail(it)) },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, "Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.validationErrors.containsKey("email"),
            supportingText = state.validationErrors["email"]?.let { { Text(it) } },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        // Bio field
        OutlinedTextField(
            value = state.editedBio,
            onValueChange = { onIntent(ProfileIntent.UpdateBio(it)) },
            label = { Text("Bio") },
            leadingIcon = { Icon(Icons.Default.Info, "Bio") },
            minLines = 3,
            maxLines = 5,
            isError = state.validationErrors.containsKey("bio"),
            supportingText = state.validationErrors["bio"]?.let { { Text(it) } } ?: {
                Text("${state.editedBio.length}/500 characters")
            },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onIntent(ProfileIntent.CancelEdit) },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = { onIntent(ProfileIntent.SaveChanges) },
                enabled = !state.isSaving && state.hasChanges,
                modifier = Modifier.weight(1f)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (state.isSaving) "Saving..." else "Save")
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, "Retry", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}
