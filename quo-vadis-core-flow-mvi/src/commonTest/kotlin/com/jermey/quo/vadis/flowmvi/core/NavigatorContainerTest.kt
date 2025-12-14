package com.jermey.quo.vadis.flowmvi.core

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.testing.FakeNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for NavigatorContainer.
 *
 * Tests the FlowMVI integration with Navigator using FlowMVI test DSL:
 * - Container initialization and state synchronization
 * - All navigation intents process correctly
 * - State updates reflect navigator changes
 * - Navigation operations call the underlying navigator
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NavigatorContainerTest {
    
    private val HomeDestination = object : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }
    
    private val DetailsDestination = object : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }
    
    private val SettingsDestination = object : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }
    
    @Test
    fun `initial state reflects empty navigator`() = runTest {
        val fakeNavigator = FakeNavigator()
        val container = NavigatorContainer(fakeNavigator, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            assertEquals(null, states.value.currentDestination, "Initial destination should be null")
            assertEquals(0, states.value.backStackSize, "Initial backstack should be empty")
            assertFalse(states.value.canGoBack, "Should not be able to go back initially")
        }
    }
    
    @Test
    fun `initial state reflects navigator with destination`() = runTest {
        val fakeNavigator = FakeNavigator()
        fakeNavigator.navigate(HomeDestination)
        val container = NavigatorContainer(fakeNavigator, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            assertEquals(HomeDestination, states.value.currentDestination, "Should have home destination")
            assertEquals(1, states.value.backStackSize, "Backstack should have 1 entry")
            assertFalse(states.value.canGoBack, "Should not be able to go back with single destination")
        }
    }
    
    @Test
    fun `Navigate intent updates state and calls navigator`() = runTest {
        val fakeNavigator = FakeNavigator()
        val container = NavigatorContainer(fakeNavigator, debuggable = false)
        
        container.store.subscribeAndTest {
            // Send navigate intent
            emit(NavigationIntent.Navigate(HomeDestination))
            advanceUntilIdle()
            
            // Verify state
            assertEquals(HomeDestination, states.value.currentDestination)
            assertEquals(1, states.value.backStackSize)
            assertFalse(states.value.canGoBack)
            
            // Verify navigator was called
            assertEquals(1, fakeNavigator.getStackSize())
            assertEquals(HomeDestination, fakeNavigator.currentDestination.value)
        }
    }
    
    @Test
    fun `Navigate with transition calls navigator correctly`() = runTest {
        val fakeNavigator = FakeNavigator()
        val container = NavigatorContainer(fakeNavigator, debuggable = false)
        val transition = com.jermey.quo.vadis.core.navigation.core.NavigationTransitions.Fade
        
        container.store.subscribeAndTest {
            emit(NavigationIntent.Navigate(HomeDestination, transition))
            advanceUntilIdle()
            
            assertEquals(HomeDestination, states.value.currentDestination)
            
            // Verify FakeNavigator recorded the navigation
            val navCall = fakeNavigator.navigationCalls.last()
            assertNotNull(navCall)
        }
    }
    
    @Test
    fun `NavigateBack updates state correctly`() = runTest {
        val fakeNavigator = FakeNavigator()
        fakeNavigator.navigate(HomeDestination)
        fakeNavigator.navigate(DetailsDestination)
        val container = NavigatorContainer(fakeNavigator, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            // Initial state: 2 destinations
            assertEquals(DetailsDestination, states.value.currentDestination)
            assertEquals(2, states.value.backStackSize)
            assertTrue(states.value.canGoBack)
            
            // Navigate back
            emit(NavigationIntent.NavigateBack)
            advanceUntilIdle()
            
            // Should be back at home
            assertEquals(HomeDestination, states.value.currentDestination)
            assertEquals(1, states.value.backStackSize)
            assertFalse(states.value.canGoBack)
        }
    }
    
    @Test
    fun `NavigateAndClearTo updates state correctly`() = runTest {
        val fakeNavigator = FakeNavigator()
        fakeNavigator.navigate(HomeDestination)
        fakeNavigator.navigate(DetailsDestination)
        fakeNavigator.navigate(SettingsDestination)
        val container = NavigatorContainer(fakeNavigator, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            assertEquals(3, states.value.backStackSize)
            
            // Clear back to home (exclusive)
            emit(NavigationIntent.NavigateAndClearTo(
                destination = SettingsDestination,
                popUpToRoute = "home",
                inclusive = false
            ))
            advanceUntilIdle()
            
            // State should be updated
            assertNotNull(states.value.currentDestination)
        }
    }
    
    @Test
    fun `NavigateAndReplace replaces current destination`() = runTest {
        val fakeNavigator = FakeNavigator()
        fakeNavigator.navigate(HomeDestination)
        val container = NavigatorContainer(fakeNavigator, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            assertEquals(1, states.value.backStackSize)
            
            emit(NavigationIntent.NavigateAndReplace(DetailsDestination))
            advanceUntilIdle()
            
            // Should still have 1 entry but different destination
            assertEquals(DetailsDestination, states.value.currentDestination)
            assertEquals(1, states.value.backStackSize)
        }
    }
    
    @Test
    fun `NavigateAndClearAll clears backstack`() = runTest {
        val fakeNavigator = FakeNavigator()
        fakeNavigator.navigate(HomeDestination)
        fakeNavigator.navigate(DetailsDestination)
        val container = NavigatorContainer(fakeNavigator, debuggable = false)
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            assertEquals(2, states.value.backStackSize)
            
            emit(NavigationIntent.NavigateAndClearAll(SettingsDestination))
            advanceUntilIdle()
            
            // Should only have the new destination
            assertEquals(SettingsDestination, states.value.currentDestination)
            assertEquals(1, states.value.backStackSize)
            assertFalse(states.value.canGoBack)
        }
    }
    
    @Test
    fun `multiple navigation intents process sequentially`() = runTest {
        val fakeNavigator = FakeNavigator()
        val container = NavigatorContainer(fakeNavigator, debuggable = false)
        
        container.store.subscribeAndTest {
            // Navigate to home
            emit(NavigationIntent.Navigate(HomeDestination))
            advanceUntilIdle()
            assertEquals(HomeDestination, states.value.currentDestination)
            assertEquals(1, states.value.backStackSize)
            
            // Navigate to details
            emit(NavigationIntent.Navigate(DetailsDestination))
            advanceUntilIdle()
            assertEquals(DetailsDestination, states.value.currentDestination)
            assertEquals(2, states.value.backStackSize)
            
            // Navigate to settings
            emit(NavigationIntent.Navigate(SettingsDestination))
            advanceUntilIdle()
            assertEquals(SettingsDestination, states.value.currentDestination)
            assertEquals(3, states.value.backStackSize)
            assertTrue(states.value.canGoBack)
            
            // Navigate back
            emit(NavigationIntent.NavigateBack)
            advanceUntilIdle()
            assertEquals(DetailsDestination, states.value.currentDestination)
            assertEquals(2, states.value.backStackSize)
        }
    }
    
    @Test
    fun `container with initial destination starts correctly`() = runTest {
        val fakeNavigator = FakeNavigator()
        val container = NavigatorContainer(
            navigator = fakeNavigator,
            initialDestination = HomeDestination,
            debuggable = false
        )
        
        container.store.subscribeAndTest {
            advanceUntilIdle()
            // Should have navigated to initial destination
            assertEquals(HomeDestination, states.value.currentDestination)
            assertEquals(1, states.value.backStackSize)
        }
    }
    
    @Test
    fun `state reflects navigator external changes`() = runTest {
        val fakeNavigator = FakeNavigator()
        val container = NavigatorContainer(fakeNavigator, debuggable = false)
        
        container.store.subscribeAndTest {
            // Directly modify navigator (simulating external change)
            fakeNavigator.navigate(HomeDestination)
            advanceUntilIdle()
            
            // State should reflect the change
            assertEquals(HomeDestination, states.value.currentDestination)
            assertEquals(1, states.value.backStackSize)
        }
    }
}
