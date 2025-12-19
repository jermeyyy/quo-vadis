package com.jermey.navplayground.demo.ui.screens.profile

import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.flowmvi.BaseContainer
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.enableLogging
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.recover
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed

private typealias Ctx = PipelineContext<ProfileState, ProfileIntent, ProfileAction>

/**
 * Profile feature container with FlowMVI store.
 *
 * Demonstrates:
 * - Complex state management (loading, content, error)
 * - Form validation
 * - Async operations with loading states
 * - Navigation integration
 * - Error recovery
 * - Side effects (actions)
 */
class ProfileContainer(
    navigator: Navigator,
    screenKey: String,
    private val repository: ProfileRepository,
    private val debuggable: Boolean = false
) : BaseContainer<ProfileState, ProfileIntent, ProfileAction>(navigator, screenKey) {

    override val store = store(initial = ProfileState.Loading) {
        configure {
            debuggable = this@ProfileContainer.debuggable
            name = "ProfileStore"
            parallelIntents = false // Process intents sequentially
        }

        // Initialize: load profile on start
        init {
            intent(ProfileIntent.LoadProfile)
        }

        // Reduce: handle all intents
        reduce { intent ->
            when (intent) {
                is ProfileIntent.LoadProfile -> handleLoadProfile()
                is ProfileIntent.StartEditing -> handleStartEditing()
                is ProfileIntent.UpdateName -> handleUpdateName(intent.name)
                is ProfileIntent.UpdateEmail -> handleUpdateEmail(intent.email)
                is ProfileIntent.UpdateBio -> handleUpdateBio(intent.bio)
                is ProfileIntent.SaveChanges -> handleSaveChanges()
                is ProfileIntent.CancelEdit -> handleCancelEdit()
                is ProfileIntent.NavigateToSettings -> handleNavigateToSettings()
                is ProfileIntent.NavigateBack -> handleNavigateBack()
                is ProfileIntent.Logout -> handleLogout()
            }
        }

        // Recover: handle errors gracefully
        recover { exception ->
            when (exception) {
                is ValidationException -> {
                    // Validation error - keep current state, show action
                    action(ProfileAction.ValidationFailed(exception.errors))
                    null // Suppress exception
                }

                else -> {
                    // Other errors - show error state
                    action(ProfileAction.ShowError(exception.message ?: "Unknown error"))
                    updateState { ProfileState.Error(exception.message ?: "An error occurred") }
                    null // Suppress exception
                }
            }
        }

        // WhileSubscribed: operations while UI is subscribed
        whileSubscribed {
            // Could add analytics, logging, etc.
        }

        // Enable logging for debugging
        if (debuggable) {
            enableLogging()
        }
    }

    /**
     * Load profile data.
     */
    private suspend fun Ctx.handleLoadProfile() {
        updateState { ProfileState.Loading }
        try {
            val user = repository.getUser()
            updateState {
                ProfileState.Content(user = user)
            }
        } catch (e: Exception) {
            updateState {
                ProfileState.Error(e.message ?: "Failed to load profile")
            }
            action(ProfileAction.ShowError("Failed to load profile"))
        }
    }

    /**
     * Enter edit mode.
     */
    private suspend fun Ctx.handleStartEditing() {
        withContentState<ProfileState.Content> { content ->
            updateState {
                content.copy(
                    isEditing = true,
                    editedName = content.user.name,
                    editedEmail = content.user.email,
                    editedBio = content.user.bio,
                    validationErrors = emptyMap()
                )
            }
        }
    }

    /**
     * Update name field.
     */
    private suspend fun Ctx.handleUpdateName(name: String) {
        withContentState<ProfileState.Content> { content ->
            if (content.isEditing) {
                updateState {
                    content.copy(editedName = name)
                }
            }
        }
    }

    /**
     * Update email field.
     */
    private suspend fun Ctx.handleUpdateEmail(email: String) {
        withContentState<ProfileState.Content> { content ->
            if (content.isEditing) {
                updateState {
                    content.copy(editedEmail = email)
                }
            }
        }
    }

    /**
     * Update bio field.
     */
    private suspend fun Ctx.handleUpdateBio(bio: String) {
        withContentState<ProfileState.Content> { content ->
            if (content.isEditing) {
                updateState {
                    content.copy(editedBio = bio)
                }
            }
        }
    }

    /**
     * Save changes.
     */
    private suspend fun Ctx.handleSaveChanges() {
        withContentState<ProfileState.Content> { content ->
            if (!content.isEditing || !content.hasChanges) return@withContentState

            // Show saving state
            updateState { content.copy(isSaving = true) }

            try {
                val result = repository.updateUser(
                    name = content.editedName,
                    email = content.editedEmail,
                    bio = content.editedBio
                )

                result.fold(
                    onSuccess = { updatedUser ->
                        updateState {
                            ProfileState.Content(
                                user = updatedUser,
                                isEditing = false
                            )
                        }
                        action(ProfileAction.ProfileSaved)
                        action(ProfileAction.ShowToast("Profile updated successfully"))
                    },
                    onFailure = { exception ->
                        if (exception is ValidationException) {
                            updateState {
                                content.copy(
                                    isSaving = false,
                                    validationErrors = exception.errors
                                )
                            }
                            action(ProfileAction.ValidationFailed(exception.errors))
                        } else {
                            updateState { content.copy(isSaving = false) }
                            action(ProfileAction.ShowError(exception.message ?: "Save failed"))
                        }
                    }
                )
            } catch (e: Exception) {
                updateState { content.copy(isSaving = false) }
                action(ProfileAction.NetworkError(e.message ?: "Network error"))
            }
        }
    }

    /**
     * Cancel editing.
     */
    private suspend fun Ctx.handleCancelEdit() {
        withContentState<ProfileState.Content> { content ->
            updateState {
                content.copy(
                    isEditing = false,
                    editedName = content.user.name,
                    editedEmail = content.user.email,
                    editedBio = content.user.bio,
                    validationErrors = emptyMap()
                )
            }
        }
    }

    /**
     * Navigate to settings.
     */
    private suspend fun Ctx.handleNavigateToSettings() {
        try {
            navigator.navigate(MainTabs.SettingsTab.Main)
        } catch (e: Exception) {
            action(ProfileAction.ShowError("Navigation failed"))
        }
    }

    /**
     * Navigate back.
     */
    private suspend fun Ctx.handleNavigateBack() {
        try {
            navigator.navigateBack()
        } catch (e: Exception) {
            // Already at root, ignore
        }
    }

    /**
     * Logout.
     */
    private suspend fun Ctx.handleLogout() {
        updateState { ProfileState.Loading }
        try {
            repository.logout()
            action(ProfileAction.LogoutSuccess)
            action(ProfileAction.ShowToast("Logged out successfully"))
            // In real app: navigate to login screen
            navigator.navigateBack()
        } catch (e: Exception) {
            action(ProfileAction.ShowError("Logout failed"))
        }
    }

    /**
     * Helper to safely access Content state.
     */
    private suspend inline fun <reified T : ProfileState> Ctx.withContentState(crossinline block: suspend (T) -> Unit) {
        withState {
            if (this is T) {
                block(this)
            }
        }
    }
}
