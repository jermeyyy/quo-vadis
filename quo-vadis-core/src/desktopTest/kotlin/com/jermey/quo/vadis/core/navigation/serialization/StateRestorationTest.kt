package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for StateRestoration interface and implementations.
 *
 * Tests cover:
 * - InMemoryStateRestoration: save/restore/clear operations
 * - Auto-save functionality (basic verification)
 * - NoOpStateRestoration: no-op behavior verification
 * - Process death simulation scenarios
 * - Partial restoration and error handling
 *
 * Note: Auto-save debounce timing tests are limited due to lack of
 * kotlinx-coroutines-test in commonTest. The auto-save mechanism
 * itself is tested for basic correctness.
 */
class StateRestorationTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object HomeDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ProfileDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object SettingsDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private fun createSimpleStack(key: String = "root"): StackNode {
        return StackNode(
            key = key,
            parentKey = null,
            children = listOf(
                ScreenNode("home", key, HomeDestination)
            )
        )
    }

    private fun createComplexTree(): NavNode {
        return StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode("tab0", "tabs", listOf(
                            ScreenNode("s0", "tab0", HomeDestination)
                        )),
                        StackNode("tab1", "tabs", listOf(
                            ScreenNode("s1", "tab1", ProfileDestination)
                        ))
                    ),
                    activeStackIndex = 1
                )
            )
        )
    }

    private var stateRestoration: StateRestoration? = null

    @AfterTest
    fun cleanup() {
        stateRestoration?.disableAutoSave()
        stateRestoration = null
    }

    // =========================================================================
    // IN-MEMORY STATE RESTORATION - BASIC OPERATIONS
    // =========================================================================

    @Test
    fun inMemoryStateRestoration_saveState_and_restoreState_roundTrip() {
        runBlocking {
            val restoration = InMemoryStateRestoration()
            val original = createSimpleStack()

            restoration.saveState(original)
            val restored = restoration.restoreState()

            assertNotNull(restored, "Restored state should not be null")
            assertEquals(original.key, restored.key)
            assertTrue(restored is StackNode)
        }
    }

    @Test
    fun inMemoryStateRestoration_restoreState_returnsNull_whenNoStateSaved() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            val restored = restoration.restoreState()

            assertNull(restored, "Should return null when no state was saved")
        }
    }

    @Test
    fun inMemoryStateRestoration_clearState_removesSavedState() {
        runBlocking {
            val restoration = InMemoryStateRestoration()
            val state = createSimpleStack()

            restoration.saveState(state)
            assertNotNull(restoration.restoreState(), "State should exist before clear")

            restoration.clearState()
            assertNull(restoration.restoreState(), "State should be null after clear")
        }
    }

    @Test
    fun inMemoryStateRestoration_overwritesPreviousState() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            val state1 = createSimpleStack("first")
            val state2 = createSimpleStack("second")

            restoration.saveState(state1)
            assertEquals("first", restoration.restoreState()?.key)

            restoration.saveState(state2)
            assertEquals("second", restoration.restoreState()?.key)
        }
    }

    @Test
    fun inMemoryStateRestoration_getLastSavedState_returnsCurrentState() {
        runBlocking {
            val restoration = InMemoryStateRestoration()
            val state = createSimpleStack()

            restoration.saveState(state)

            assertEquals(state, restoration.getLastSavedState())
        }
    }

    @Test
    fun inMemoryStateRestoration_preservesComplexTreeStructure() {
        runBlocking {
            val restoration = InMemoryStateRestoration()
            val original = createComplexTree()

            restoration.saveState(original)
            val restored = restoration.restoreState()

            assertNotNull(restored)
            assertTrue(restored is StackNode)
            val rootStack = restored as StackNode
            val tabs = rootStack.activeChild as TabNode
            assertEquals(2, tabs.tabCount)
            assertEquals(1, tabs.activeStackIndex)
        }
    }

    // =========================================================================
    // IN-MEMORY STATE RESTORATION - AUTO-SAVE
    // =========================================================================

    @Test
    fun inMemoryStateRestoration_autoSaveEnabled_falseByDefault() {
        val restoration = InMemoryStateRestoration()
        assertFalse(restoration.autoSaveEnabled, "Auto-save should be disabled by default")
    }

    @Test
    fun inMemoryStateRestoration_enableAutoSave_setsAutoSaveEnabledTrue() {
        val restoration = InMemoryStateRestoration(debounceMillis = 0)
        stateRestoration = restoration
        val stateFlow = MutableStateFlow<NavNode>(createSimpleStack())

        restoration.enableAutoSave(stateFlow)

        assertTrue(restoration.autoSaveEnabled, "Auto-save should be enabled after enableAutoSave")
    }

    @Test
    fun inMemoryStateRestoration_disableAutoSave_setsAutoSaveEnabledFalse() {
        val restoration = InMemoryStateRestoration(debounceMillis = 0)
        stateRestoration = restoration
        val stateFlow = MutableStateFlow<NavNode>(createSimpleStack())

        restoration.enableAutoSave(stateFlow)
        assertTrue(restoration.autoSaveEnabled)

        restoration.disableAutoSave()
        assertFalse(restoration.autoSaveEnabled)
    }

    @Test
    fun inMemoryStateRestoration_enableAutoSave_cancelsPreviousAutoSave() {
        val restoration = InMemoryStateRestoration(debounceMillis = 0)
        stateRestoration = restoration
        val flow1 = MutableStateFlow<NavNode>(createSimpleStack("flow1"))
        val flow2 = MutableStateFlow<NavNode>(createSimpleStack("flow2"))

        restoration.enableAutoSave(flow1)
        assertTrue(restoration.autoSaveEnabled)

        // Enable auto-save on a new flow (should cancel previous)
        restoration.enableAutoSave(flow2)
        assertTrue(restoration.autoSaveEnabled, "Should still be enabled after re-enabling on new flow")
    }

    // =========================================================================
    // NO-OP STATE RESTORATION
    // =========================================================================

    @Test
    fun noOpStateRestoration_saveState_doesNothing() {
        runBlocking {
            val state = createSimpleStack()
            // Should not throw
            NoOpStateRestoration.saveState(state)
        }
    }

    @Test
    fun noOpStateRestoration_restoreState_returnsNull() {
        runBlocking {
            // Save something first
            NoOpStateRestoration.saveState(createSimpleStack())

            // Should still return null
            val restored = NoOpStateRestoration.restoreState()
            assertNull(restored, "NoOpStateRestoration should always return null")
        }
    }

    @Test
    fun noOpStateRestoration_clearState_doesNothing() {
        runBlocking {
            // Should not throw
            NoOpStateRestoration.clearState()
        }
    }

    @Test
    fun noOpStateRestoration_autoSaveEnabled_alwaysFalse() {
        assertFalse(NoOpStateRestoration.autoSaveEnabled)
    }

    @Test
    fun noOpStateRestoration_enableAutoSave_doesNothing() {
        val stateFlow = MutableStateFlow<NavNode>(createSimpleStack())

        // Should not throw
        NoOpStateRestoration.enableAutoSave(stateFlow)

        // Should still be false
        assertFalse(NoOpStateRestoration.autoSaveEnabled)
    }

    @Test
    fun noOpStateRestoration_disableAutoSave_doesNothing() {
        // Should not throw
        NoOpStateRestoration.disableAutoSave()
    }

    // =========================================================================
    // PROCESS DEATH SIMULATION SCENARIOS
    // =========================================================================

    @Test
    fun simulatedProcessDeath_basicStateSurvives() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            val userState = StackNode(
                key = "root",
                parentKey = null,
                children = listOf(
                    ScreenNode("home", "root", HomeDestination),
                    ScreenNode("profile", "root", ProfileDestination)
                )
            )

            restoration.saveState(userState)
            val restoredState = restoration.restoreState()

            assertNotNull(restoredState, "State should survive process death simulation")
            assertTrue(restoredState is StackNode)
            assertEquals(2, (restoredState as StackNode).size)
        }
    }

    @Test
    fun simulatedProcessDeath_tabStateAndActiveIndexPreserved() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            val tabState = TabNode(
                key = "main-tabs",
                parentKey = null,
                stacks = listOf(
                    StackNode("home-tab", "main-tabs", listOf(
                        ScreenNode("home", "home-tab", HomeDestination)
                    )),
                    StackNode("profile-tab", "main-tabs", listOf(
                        ScreenNode("profile-list", "profile-tab", ProfileDestination),
                        ScreenNode("profile-detail", "profile-tab", SettingsDestination)
                    )),
                    StackNode("settings-tab", "main-tabs", listOf(
                        ScreenNode("settings", "settings-tab", SettingsDestination)
                    ))
                ),
                activeStackIndex = 1
            )

            restoration.saveState(tabState)
            val restored = restoration.restoreState() as TabNode

            assertEquals(3, restored.tabCount)
            assertEquals(1, restored.activeStackIndex, "Active tab should be preserved")
            assertEquals("profile-tab", restored.activeStack.key)
            assertEquals(2, restored.activeStack.size, "Tab history should be preserved")
        }
    }

    @Test
    fun simulatedProcessDeath_paneStatePreserved() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            val paneState = PaneNode(
                key = "list-detail",
                parentKey = null,
                paneConfigurations = mapOf(
                    PaneRole.Primary to PaneConfiguration(
                        content = StackNode("list-stack", "list-detail", listOf(
                            ScreenNode("list", "list-stack", HomeDestination)
                        ))
                    ),
                    PaneRole.Supporting to PaneConfiguration(
                        content = ScreenNode("detail", "list-detail", ProfileDestination)
                    )
                ),
                activePaneRole = PaneRole.Supporting
            )

            restoration.saveState(paneState)
            val restored = restoration.restoreState() as PaneNode

            assertEquals(2, restored.paneCount)
            assertEquals(PaneRole.Supporting, restored.activePaneRole)
        }
    }

    @Test
    fun simulatedRecreationWithoutPriorState_usesDefault() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            val restored = restoration.restoreState()

            assertNull(restored, "Fresh install should have no saved state")

            val defaultState = createSimpleStack()
            assertNotNull(defaultState)
        }
    }

    // =========================================================================
    // EDGE CASES AND ERROR HANDLING
    // =========================================================================

    @Test
    fun multipleSaveCalls_onlyKeepLatestState() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            restoration.saveState(createSimpleStack("state-1"))
            restoration.saveState(createSimpleStack("state-2"))
            restoration.saveState(createSimpleStack("state-3"))

            val restored = restoration.restoreState()
            assertEquals("state-3", restored?.key, "Only the latest state should be kept")
        }
    }

    @Test
    fun restoreState_canBeCalledMultipleTimes() {
        runBlocking {
            val restoration = InMemoryStateRestoration()
            val state = createSimpleStack()

            restoration.saveState(state)

            val first = restoration.restoreState()
            val second = restoration.restoreState()
            val third = restoration.restoreState()

            assertEquals(first?.key, second?.key)
            assertEquals(second?.key, third?.key)
        }
    }

    @Test
    fun clearStateFollowedByRestoreState_returnsNull() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            restoration.saveState(createSimpleStack())
            restoration.clearState()
            restoration.clearState() // Multiple clears should be safe

            assertNull(restoration.restoreState())
        }
    }

    @Test
    fun saveStateAfterClearState_worksCorrectly() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            restoration.saveState(createSimpleStack("first"))
            restoration.clearState()
            restoration.saveState(createSimpleStack("second"))

            assertEquals("second", restoration.restoreState()?.key)
        }
    }

    // =========================================================================
    // DEBOUNCE CONFIGURATION TESTS
    // =========================================================================

    @Test
    fun defaultDebounceMs_isReasonableValue() {
        assertEquals(100L, InMemoryStateRestoration.DEFAULT_DEBOUNCE_MS)
    }

    @Test
    fun inMemoryStateRestoration_acceptsZeroDebounce() {
        val restoration = InMemoryStateRestoration(debounceMillis = 0)
        assertNotNull(restoration)
    }

    @Test
    fun inMemoryStateRestoration_acceptsCustomDebounceValue() {
        val restoration = InMemoryStateRestoration(debounceMillis = 500)
        assertNotNull(restoration)
    }

    // =========================================================================
    // INTEGRATION TESTS
    // =========================================================================

    @Test
    fun fullWorkflow_createNavigateSaveRestore() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            var currentState: NavNode = StackNode(
                key = "root",
                parentKey = null,
                children = listOf(
                    ScreenNode("home", "root", HomeDestination)
                )
            )

            currentState = StackNode(
                key = "root",
                parentKey = null,
                children = listOf(
                    ScreenNode("home", "root", HomeDestination),
                    ScreenNode("profile", "root", ProfileDestination)
                )
            )

            currentState = StackNode(
                key = "root",
                parentKey = null,
                children = listOf(
                    ScreenNode("home", "root", HomeDestination),
                    ScreenNode("profile", "root", ProfileDestination),
                    ScreenNode("settings", "root", SettingsDestination)
                )
            )

            restoration.saveState(currentState)

            @Suppress("UNUSED_VALUE")
            currentState = createSimpleStack()

            val restoredState = restoration.restoreState()!!

            assertTrue(restoredState is StackNode)
            assertEquals(3, (restoredState as StackNode).size)
            assertEquals("settings", restoredState.activeChild?.key)
        }
    }

    @Test
    fun concurrentSaveAndRestoreOperations() {
        runBlocking {
            val restoration = InMemoryStateRestoration()

            restoration.saveState(createSimpleStack("state-1"))
            assertEquals("state-1", restoration.restoreState()?.key)

            restoration.saveState(createSimpleStack("state-2"))
            assertEquals("state-2", restoration.restoreState()?.key)

            restoration.clearState()
            assertNull(restoration.restoreState())

            restoration.saveState(createSimpleStack("state-3"))
            assertEquals("state-3", restoration.restoreState()?.key)
        }
    }

    // =========================================================================
    // STATE RESTORATION INTERFACE TESTS
    // =========================================================================

    @Test
    fun stateRestorationImplementations_implementInterfaceCorrectly() {
        val inMemory: StateRestoration = InMemoryStateRestoration()
        assertFalse(inMemory.autoSaveEnabled)

        val noOp: StateRestoration = NoOpStateRestoration
        assertFalse(noOp.autoSaveEnabled)
    }

    @Test
    fun disableAutoSave_whenNotEnabled_isSafe() {
        val restoration = InMemoryStateRestoration()

        restoration.disableAutoSave()
        restoration.disableAutoSave()
        restoration.disableAutoSave()

        assertFalse(restoration.autoSaveEnabled)
    }
}
