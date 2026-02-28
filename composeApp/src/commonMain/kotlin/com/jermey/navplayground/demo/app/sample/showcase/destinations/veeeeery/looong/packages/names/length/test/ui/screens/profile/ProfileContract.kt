package com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.ui.screens.profile

import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

/**
 * Profile feature state.
 * 
 * Demonstrates FlowMVI state management patterns:
 * - Loading states (Loading, Content, Error)
 * - Form editing with validation
 * - User data management
 */
sealed interface ProfileState : MVIState {
    /**
     * Initial loading state.
     */
    data object Loading : ProfileState
    
    /**
     * Content state with user data.
     */
    data class Content(
        val user: UserData,
        val isEditing: Boolean = false,
        val editedName: String = user.name,
        val editedEmail: String = user.email,
        val editedBio: String = user.bio,
        val isSaving: Boolean = false,
        val validationErrors: Map<String, String> = emptyMap()
    ) : ProfileState {
        val hasChanges: Boolean
            get() = editedName != user.name || 
                    editedEmail != user.email || 
                    editedBio != user.bio
        
        val isValid: Boolean
            get() = validationErrors.isEmpty()
    }
    
    /**
     * Error state with retry option.
     */
    data class Error(val message: String) : ProfileState
}

/**
 * User data model.
 */
data class UserData(
    val id: String,
    val name: String,
    val email: String,
    val bio: String,
    val avatarUrl: String? = null,
    val joinedDate: String = "2024-01-01"
)

/**
 * Profile feature intents.
 * 
 * Demonstrates various intent types:
 * - Data loading
 * - Form manipulation
 * - Validation
 * - Navigation
 */
sealed interface ProfileIntent : MVIIntent {
    /**
     * Load profile data (initial or refresh).
     */
    data object LoadProfile : ProfileIntent
    
    /**
     * Enter edit mode.
     */
    data object StartEditing : ProfileIntent
    
    /**
     * Update name in edit mode.
     */
    data class UpdateName(val name: String) : ProfileIntent
    
    /**
     * Update email in edit mode.
     */
    data class UpdateEmail(val email: String) : ProfileIntent
    
    /**
     * Update bio in edit mode.
     */
    data class UpdateBio(val bio: String) : ProfileIntent
    
    /**
     * Save changes to backend.
     */
    data object SaveChanges : ProfileIntent
    
    /**
     * Cancel editing and revert changes.
     */
    data object CancelEdit : ProfileIntent
    
    /**
     * Navigate to settings.
     */
    data object NavigateToSettings : ProfileIntent
    
    /**
     * Navigate back.
     */
    data object NavigateBack : ProfileIntent
    
    /**
     * Logout (clear session).
     */
    data object Logout : ProfileIntent
}

/**
 * Profile feature actions (side effects).
 * 
 * Demonstrates various action types:
 * - User feedback (toasts, snackbars)
 * - Navigation events
 * - Error handling
 */
sealed interface ProfileAction : MVIAction {
    /**
     * Show a toast message.
     */
    data class ShowToast(val message: String) : ProfileAction
    
    /**
     * Show an error message.
     */
    data class ShowError(val error: String) : ProfileAction
    
    /**
     * Profile saved successfully.
     */
    data object ProfileSaved : ProfileAction
    
    /**
     * Logout successful, navigate to login.
     */
    data object LogoutSuccess : ProfileAction
    
    /**
     * Validation failed with errors.
     */
    data class ValidationFailed(val errors: Map<String, String>) : ProfileAction
    
    /**
     * Network error occurred.
     */
    data class NetworkError(val message: String) : ProfileAction
}
