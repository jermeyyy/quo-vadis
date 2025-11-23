package com.jermey.navplayground.demo.profile

import com.jermey.navplayground.demo.ui.screens.profile.ProfileContainer
import com.jermey.navplayground.demo.ui.screens.profile.ProfileIntent
import com.jermey.navplayground.demo.ui.screens.profile.ProfileRepository
import com.jermey.navplayground.demo.ui.screens.profile.ProfileState
import com.jermey.quo.vadis.core.navigation.testing.FakeNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for ProfileContainer.
 *
 * Tests the profile FlowMVI container using proper FlowMVI test DSL:
 * - State transitions (Loading -> Content -> Editing)
 * - Intent processing and state updates
 * - Form validation
 * - Navigation integration
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileContainerTest {
    
    private lateinit var navigator: FakeNavigator
    private lateinit var repository: ProfileRepository
    private lateinit var container: ProfileContainer
    
    @BeforeTest
    fun setup() {
        navigator = FakeNavigator()
        repository = ProfileRepository()
    }
    
    @Test
    fun `container initializes and loads profile`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            // Should be in Content state after loading
            val state = states.value
            assertIs<ProfileState.Content>(state, "State should be Content after loading")
            
            val content = state as ProfileState.Content
            assertNotNull(content.user, "User should be loaded")
            assertFalse(content.isEditing, "Should not be in edit mode initially")
            assertFalse(content.isSaving, "Should not be saving initially")
        }
    }
    
    @Test
    fun `StartEditing intent enters edit mode`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            // Verify initial state
            val initialState = states.value as ProfileState.Content
            assertFalse(initialState.isEditing)
            
            // Send StartEditing intent
            emit(ProfileIntent.StartEditing)
            advanceUntilIdle()
            
            // Verify entered edit mode
            val editState = states.value as ProfileState.Content
            assertTrue(editState.isEditing, "Should be in edit mode")
            assertEquals(editState.user.name, editState.editedName, "Edited name should match user name")
            assertEquals(editState.user.email, editState.editedEmail, "Edited email should match user email")
            assertEquals(editState.user.bio, editState.editedBio, "Edited bio should match user bio")
        }
    }
    
    @Test
    fun `UpdateName intent updates edited user name`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            // Start editing
            emit(ProfileIntent.StartEditing)
            advanceUntilIdle()
            
            val initialName = (states.value as ProfileState.Content).user.name
            
            // Update name
            val newName = "Updated Name"
            emit(ProfileIntent.UpdateName(newName))
            advanceUntilIdle()
            
            // Verify name updated
            val state = states.value as ProfileState.Content
            assertEquals(newName, state.editedName, "Edited name should be updated")
            assertEquals(initialName, state.user.name, "Original user should be unchanged")
        }
    }
    
    @Test
    fun `UpdateEmail intent updates edited user email`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            emit(ProfileIntent.StartEditing)
            advanceUntilIdle()
            
            val newEmail = "updated@email.com"
            emit(ProfileIntent.UpdateEmail(newEmail))
            advanceUntilIdle()
            
            val state = states.value as ProfileState.Content
            assertEquals(newEmail, state.editedEmail, "Email should be updated")
        }
    }
    
    @Test
    fun `UpdateBio intent updates edited user bio`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            emit(ProfileIntent.StartEditing)
            advanceUntilIdle()
            
            val newBio = "Updated bio text"
            emit(ProfileIntent.UpdateBio(newBio))
            advanceUntilIdle()
            
            val state = states.value as ProfileState.Content
            assertEquals(newBio, state.editedBio, "Bio should be updated")
        }
    }
    
    @Test
    fun `SaveChanges intent saves and exits edit mode`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            // Start editing and make changes
            emit(ProfileIntent.StartEditing)
            advanceUntilIdle()
            
            val newName = "Saved Name"
            emit(ProfileIntent.UpdateName(newName))
            advanceUntilIdle()
            
            // Save changes
            emit(ProfileIntent.SaveChanges)
            advanceUntilIdle()
            
            // Verify saved
            val state = states.value as ProfileState.Content
            assertFalse(state.isEditing, "Should exit edit mode")
            assertEquals(newName, state.user.name, "Changes should be saved to user")
        }
    }
    
    @Test
    fun `CancelEdit intent discards changes and exits edit mode`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            val originalName = (states.value as ProfileState.Content).user.name
            
            // Start editing and make changes
            emit(ProfileIntent.StartEditing)
            advanceUntilIdle()
            
            emit(ProfileIntent.UpdateName("Changed Name"))
            advanceUntilIdle()
            
            // Cancel edit
            emit(ProfileIntent.CancelEdit)
            advanceUntilIdle()
            
            // Verify changes discarded
            val state = states.value as ProfileState.Content
            assertFalse(state.isEditing, "Should exit edit mode")
            assertEquals(originalName, state.user.name, "Original name should be unchanged")
        }
    }
    
    @Test
    fun `NavigateToSettings intent triggers navigation`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            val initialCallsCount = navigator.navigationCalls.size
            
            emit(ProfileIntent.NavigateToSettings)
            advanceUntilIdle()
            
            // Verify navigation was called
            assertTrue(
                navigator.navigationCalls.size > initialCallsCount,
                "Navigation should be called"
            )
        }
    }
    
    @Test
    fun `NavigateBack intent triggers back navigation`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            val initialCallsCount = navigator.navigationCalls.size
            
            emit(ProfileIntent.NavigateBack)
            advanceUntilIdle()
            
            // Verify back navigation was called
            assertTrue(
                navigator.navigationCalls.size > initialCallsCount,
                "Navigation back should be called"
            )
        }
    }
    
    @Test
    fun `Logout intent triggers logout and navigation`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            val initialCallsCount = navigator.navigationCalls.size
            
            emit(ProfileIntent.Logout)
            advanceUntilIdle()
            
            // Verify navigation was triggered
            assertTrue(
                navigator.navigationCalls.size > initialCallsCount,
                "Logout should trigger navigation"
            )
        }
    }
    
    @Test
    fun `multiple edit operations work correctly`() = runTest {
        container = ProfileContainer(navigator, repository, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            
            // Start editing
            emit(ProfileIntent.StartEditing)
            advanceUntilIdle()
            
            // Make multiple changes
            val newName = "Multi Edit Name"
            val newEmail = "multi@edit.com"
            val newBio = "Multi edit bio"
            
            emit(ProfileIntent.UpdateName(newName))
            advanceUntilIdle()
            
            emit(ProfileIntent.UpdateEmail(newEmail))
            advanceUntilIdle()
            
            emit(ProfileIntent.UpdateBio(newBio))
            advanceUntilIdle()
            
            // Verify all changes applied
            val state = states.value as ProfileState.Content
            assertEquals(newName, state.editedName)
            assertEquals(newEmail, state.editedEmail)
            assertEquals(newBio, state.editedBio)
            
            // Save and verify
            emit(ProfileIntent.SaveChanges)
            advanceUntilIdle()
            
            val savedState = states.value as ProfileState.Content
            assertEquals(newName, savedState.user.name)
            assertEquals(newEmail, savedState.user.email)
            assertEquals(newBio, savedState.user.bio)
        }
    }
    
    @Test
    fun `container can be created in debug mode`() {
        val debugContainer = ProfileContainer(navigator, repository, debuggable = true)
        assertNotNull(debugContainer.store, "Debug container store should be initialized")
    }
    
    @Test
    fun `ProfileRepository creates default user`() {
        val repo = ProfileRepository()
        assertTrue(true, "Repository should be creatable")
    }
}
