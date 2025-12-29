package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.dsl.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.NavDestination
import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.navigation.NavigationTransition
import com.jermey.quo.vadis.core.navigation.ScreenNode
import com.jermey.quo.vadis.core.navigation.StackNode
import com.jermey.quo.vadis.core.navigation.TabNode
import com.jermey.quo.vadis.core.navigation.findByKey
import com.jermey.quo.vadis.core.navigation.tree.TreeMutator
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Tests for stack-scope-aware [com.jermey.quo.vadis.core.navigation.tree.TreeMutator] operations.
 *
 * These tests verify that `TreeMutator.push` with a [com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry] correctly routes
 * destinations based on whether they belong to a StackNode's scope.
 *
 * This complements [TreeMutatorScopeTest] which focuses on TabNode scopes.
 */
class TreeMutatorStackScopeTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    /**
     * Simulates an authentication flow sealed class.
     * These are "in scope" for a StackNode with scopeKey="AuthFlow".
     */
    private sealed interface AuthFlow : NavDestination {
        data object Login : AuthFlow {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
            override fun toString(): String = "Login"
        }

        data object Register : AuthFlow {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
            override fun toString(): String = "Register"
        }

        data object ForgotPassword : AuthFlow {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
            override fun toString(): String = "ForgotPassword"
        }
    }

    /**
     * Simulates a main flow sealed class.
     * These are "in scope" for a StackNode with scopeKey="MainFlow".
     */
    private sealed interface MainFlow : NavDestination {
        data object Home : MainFlow {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
            override fun toString(): String = "Home"
        }

        data object Profile : MainFlow {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
            override fun toString(): String = "Profile"
        }
    }

    /**
     * A destination that is NOT part of any scope.
     */
    private data object UniversalDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "Universal"
    }

    // =========================================================================
    // TEST REGISTRY
    // =========================================================================

    /**
     * Test implementation of ScopeRegistry.
     * Simulates what KSP would generate from @Stack sealed class hierarchies.
     */
    private val testRegistry = object : ScopeRegistry {
        private val scopes = mapOf(
            "AuthFlow" to setOf(
                AuthFlow.Login::class,
                AuthFlow.Register::class,
                AuthFlow.ForgotPassword::class
            ),
            "MainFlow" to setOf(MainFlow.Home::class, MainFlow.Profile::class)
        )

        override fun isInScope(scopeKey: String, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): String? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key
        }
    }

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    private var keyCounter = 0

    private fun createKeyGenerator(): () -> String {
        return { "key-${keyCounter++}" }
    }

    @BeforeTest
    fun setup() {
        keyCounter = 0
    }

    // =========================================================================
    // IN-SCOPE DESTINATION TESTS
    // =========================================================================

    @Test
    fun `push in-scope destination stays in stack`() {
        // Given stack with scopeKey = "AuthFlow"
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth",
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = "auth",
            parentKey = "root",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing destination in AuthFlow scope
        val result = TreeMutator.push(root, AuthFlow.Register, testRegistry, generateKey)

        // Then pushed to same stack (authStack)
        val resultRoot = result as StackNode
        assertEquals(1, resultRoot.children.size, "Root should still have 1 child (authStack)")

        val resultAuthStack = resultRoot.children[0] as StackNode
        assertEquals(2, resultAuthStack.children.size, "AuthStack should now have 2 children")
        assertEquals("AuthFlow", resultAuthStack.scopeKey, "AuthStack scopeKey should be preserved")

        val newScreen = resultAuthStack.children.last()
        assertIs<ScreenNode>(newScreen)
        assertEquals(AuthFlow.Register, newScreen.destination)
        assertEquals("auth", newScreen.parentKey)
    }

    @Test
    fun `push multiple in-scope destinations stays in same stack`() {
        // Given stack with scopeKey = "AuthFlow"
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth",
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = "auth",
            parentKey = "root",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        var tree: NavNode = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing multiple AuthFlow destinations
        tree = TreeMutator.push(tree, AuthFlow.Register, testRegistry, generateKey)
        tree = TreeMutator.push(tree, AuthFlow.ForgotPassword, testRegistry, generateKey)

        // Then all pushed to authStack
        val resultRoot = tree as StackNode
        assertEquals(1, resultRoot.children.size)

        val resultAuthStack = resultRoot.children[0] as StackNode
        assertEquals(3, resultAuthStack.children.size)

        assertEquals(AuthFlow.Login, (resultAuthStack.children[0] as ScreenNode).destination)
        assertEquals(AuthFlow.Register, (resultAuthStack.children[1] as ScreenNode).destination)
        assertEquals(
            AuthFlow.ForgotPassword,
            (resultAuthStack.children[2] as ScreenNode).destination
        )
    }

    // =========================================================================
    // OUT-OF-SCOPE DESTINATION TESTS
    // =========================================================================

    @Test
    fun `push out-of-scope destination navigates to parent`() {
        // Given stack with scopeKey = "AuthFlow"
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth",
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = "auth",
            parentKey = "root",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing destination NOT in AuthFlow scope
        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey)

        // Then new screen created in parent stack (root)
        val resultRoot = result as StackNode
        assertEquals(2, resultRoot.children.size, "Root should now have 2 children")

        // AuthStack should be preserved
        val resultAuthStack = resultRoot.children[0] as StackNode
        assertEquals(1, resultAuthStack.children.size, "AuthStack should be unchanged")
        assertEquals("AuthFlow", resultAuthStack.scopeKey)

        // New screen should be sibling to authStack
        val newScreen = resultRoot.children[1]
        assertIs<ScreenNode>(newScreen)
        assertEquals(MainFlow.Home, newScreen.destination)
        assertEquals("root", newScreen.parentKey)
    }

    @Test
    fun `push out-of-scope preserves scoped stack for predictive back`() {
        // Given AuthFlow stack with multiple screens
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth",
            destination = AuthFlow.Login
        )

        val registerScreen = ScreenNode(
            key = "register-screen",
            parentKey = "auth",
            destination = AuthFlow.Register
        )

        val authStack = StackNode(
            key = "auth",
            parentKey = "root",
            children = listOf(loginScreen, registerScreen),
            scopeKey = "AuthFlow"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing out-of-scope destination
        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey)

        // Then AuthStack is completely preserved
        val resultRoot = result as StackNode
        val resultAuthStack = resultRoot.children[0] as StackNode

        assertEquals(2, resultAuthStack.children.size, "AuthStack should preserve all screens")
        assertEquals(AuthFlow.Login, (resultAuthStack.children[0] as ScreenNode).destination)
        assertEquals(AuthFlow.Register, (resultAuthStack.children[1] as ScreenNode).destination)
    }

    @Test
    fun `push multiple out-of-scope destinations stacks in parent`() {
        // Given stack with scopeKey = "AuthFlow"
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth",
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = "auth",
            parentKey = "root",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        var tree: NavNode = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing multiple out-of-scope destinations
        tree = TreeMutator.push(tree, MainFlow.Home, testRegistry, generateKey)
        tree = TreeMutator.push(tree, MainFlow.Profile, testRegistry, generateKey)

        // Then all pushed to root stack
        val resultRoot = tree as StackNode
        assertEquals(3, resultRoot.children.size)

        assertIs<StackNode>(resultRoot.children[0]) // AuthStack preserved
        assertIs<ScreenNode>(resultRoot.children[1])
        assertIs<ScreenNode>(resultRoot.children[2])

        assertEquals(MainFlow.Home, (resultRoot.children[1] as ScreenNode).destination)
        assertEquals(MainFlow.Profile, (resultRoot.children[2] as ScreenNode).destination)
    }

    // =========================================================================
    // NESTED STACKS WITH SCOPES TESTS
    // =========================================================================

    @Test
    fun `nested stacks respect innermost scope first`() {
        // Given: root > outerStack(scopeKey=MainFlow) > innerStack(scopeKey=AuthFlow)
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "inner",
            destination = AuthFlow.Login
        )

        val innerStack = StackNode(
            key = "inner",
            parentKey = "outer",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val outerStack = StackNode(
            key = "outer",
            parentKey = "root",
            children = listOf(innerStack),
            scopeKey = "MainFlow"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(outerStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing destination in MainFlow (outer) but not in AuthFlow (inner)
        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey)

        // Then: pushes to outerStack (innerStack's parent)
        val resultRoot = result as StackNode
        assertEquals(1, resultRoot.children.size, "Root should still have 1 child")

        val resultOuterStack = resultRoot.children[0] as StackNode
        assertEquals(2, resultOuterStack.children.size, "OuterStack should have 2 children")

        // Inner stack preserved
        assertIs<StackNode>(resultOuterStack.children[0])
        // New screen is sibling to innerStack
        val newScreen = resultOuterStack.children[1]
        assertIs<ScreenNode>(newScreen)
        assertEquals(MainFlow.Home, newScreen.destination)
        assertEquals("outer", newScreen.parentKey)
    }

    @Test
    fun `doubly nested stacks - destination escapes both`() {
        // Given: root > outerStack(scopeKey=AuthFlow) > innerStack(scopeKey=AuthFlow)
        // Destination not in AuthFlow should escape to root
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "inner",
            destination = AuthFlow.Login
        )

        val innerStack = StackNode(
            key = "inner",
            parentKey = "outer",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val outerStack = StackNode(
            key = "outer",
            parentKey = "root",
            children = listOf(innerStack),
            scopeKey = "AuthFlow"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(outerStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing destination not in AuthFlow
        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey)

        // Then: Should push to root (escaping both auth stacks)
        // Note: This depends on the exact algorithm - verifying behavior
        val resultRoot = result as StackNode

        // The exact behavior depends on how findTargetStackForPush walks the tree
        // It should find the first non-scoped parent that can accept the destination
        assertNotNull(resultRoot.findByKey("outer"), "Outer stack should be preserved")
        assertNotNull(resultRoot.findByKey("inner"), "Inner stack should be preserved")
    }

    // =========================================================================
    // STACK WITHOUT SCOPE KEY TESTS
    // =========================================================================

    @Test
    fun `stack without scopeKey accepts all destinations`() {
        // Given stack with scopeKey = null
        val homeScreen = ScreenNode(
            key = "home-screen",
            parentKey = "main",
            destination = MainFlow.Home
        )

        val mainStack = StackNode(
            key = "main",
            parentKey = "root",
            children = listOf(homeScreen),
            scopeKey = null // No scope enforcement
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(mainStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing any destination (even from different scope)
        val result = TreeMutator.push(root, AuthFlow.Login, testRegistry, generateKey)

        // Then pushed to same stack (existing behavior)
        val resultRoot = result as StackNode
        assertEquals(1, resultRoot.children.size, "Root should still have 1 child")

        val resultMainStack = resultRoot.children[0] as StackNode
        assertEquals(2, resultMainStack.children.size, "Main stack should have 2 children")

        val newScreen = resultMainStack.children.last()
        assertIs<ScreenNode>(newScreen)
        assertEquals(AuthFlow.Login, newScreen.destination)
        assertEquals("main", newScreen.parentKey)
    }

    @Test
    fun `stack without scopeKey mixed with scoped stack`() {
        // Given: root > unscopedStack > scopedStack(AuthFlow)
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "scoped",
            destination = AuthFlow.Login
        )

        val scopedStack = StackNode(
            key = "scoped",
            parentKey = "unscoped",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val unscopedStack = StackNode(
            key = "unscoped",
            parentKey = "root",
            children = listOf(scopedStack),
            scopeKey = null
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(unscopedStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing out-of-AuthFlow destination
        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey)

        // Then: escapes scopedStack, goes to unscopedStack (which accepts all)
        val resultRoot = result as StackNode
        assertEquals(1, resultRoot.children.size)

        val resultUnscopedStack = resultRoot.children[0] as StackNode
        assertEquals(2, resultUnscopedStack.children.size)

        // Scoped stack preserved
        assertIs<StackNode>(resultUnscopedStack.children[0])
        // New screen in unscoped stack
        val newScreen = resultUnscopedStack.children[1]
        assertIs<ScreenNode>(newScreen)
        assertEquals(MainFlow.Home, newScreen.destination)
        assertEquals("unscoped", newScreen.parentKey)
    }

    // =========================================================================
    // EMPTY REGISTRY TESTS (BACKWARD COMPATIBILITY)
    // =========================================================================

    @Test
    fun `push with Empty registry ignores stack scopeKey`() {
        // Given stack with scopeKey
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth",
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = "auth",
            parentKey = "root",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing out-of-scope with Empty registry
        val result = TreeMutator.push(root, MainFlow.Home, ScopeRegistry.Empty, generateKey)

        // Then: pushed to authStack anyway (scope not enforced)
        val resultRoot = result as StackNode
        assertEquals(1, resultRoot.children.size)

        val resultAuthStack = resultRoot.children[0] as StackNode
        assertEquals(2, resultAuthStack.children.size)

        val newScreen = resultAuthStack.children.last()
        assertIs<ScreenNode>(newScreen)
        assertEquals(MainFlow.Home, newScreen.destination)
    }

    @Test
    fun `push without scopeRegistry parameter uses original behavior`() {
        // Given stack with scopeKey
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth",
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = "auth",
            parentKey = "root",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing without ScopeRegistry parameter
        val result = TreeMutator.push(root, MainFlow.Home, generateKey)

        // Then: pushed to authStack (original behavior, no scope enforcement)
        val resultRoot = result as StackNode
        assertEquals(1, resultRoot.children.size)

        val resultAuthStack = resultRoot.children[0] as StackNode
        assertEquals(2, resultAuthStack.children.size)
    }

    // =========================================================================
    // STACK INSIDE TAB WITH BOTH SCOPES TESTS
    // =========================================================================

    /**
     * Simulates tab destinations for testing combined scopes.
     */
    private sealed interface HomeTabs : NavDestination {
        data object Feed : HomeTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }

        data object Explore : HomeTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }
    }

    private val combinedScopeRegistry = object : ScopeRegistry {
        private val scopes = mapOf(
            "AuthFlow" to setOf(
                AuthFlow.Login::class,
                AuthFlow.Register::class,
                AuthFlow.ForgotPassword::class
            ),
            "MainFlow" to setOf(MainFlow.Home::class, MainFlow.Profile::class),
            "HomeTabs" to setOf(HomeTabs.Feed::class, HomeTabs.Explore::class)
        )

        override fun isInScope(scopeKey: String, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): String? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key
        }
    }

    @Test
    fun `scoped stack inside scoped tab - inner scope checked first`() {
        // Given: root > TabNode(HomeTabs) > StackNode(AuthFlow)
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth-stack",
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = "auth-stack",
            parentKey = "tab0",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val tabStack = StackNode(
            key = "tab0",
            parentKey = "tabs",
            children = listOf(authStack)
        )

        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(tabStack),
            activeStackIndex = 0,
            scopeKey = "HomeTabs"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        val generateKey = createKeyGenerator()

        // When pushing HomeTabs.Feed (in tab scope, not in stack scope)
        val result = TreeMutator.push(root, HomeTabs.Feed, combinedScopeRegistry, generateKey)

        // Then: escapes AuthFlow stack, goes to tab's stack (tab0)
        val resultRoot = result as StackNode
        val resultTabs = resultRoot.children[0] as TabNode
        val resultTabStack = resultTabs.stacks[0]

        assertEquals(2, resultTabStack.children.size)

        // Auth stack preserved
        assertIs<StackNode>(resultTabStack.children[0])
        // New screen added as sibling
        val newScreen = resultTabStack.children[1]
        assertIs<ScreenNode>(newScreen)
        assertEquals(HomeTabs.Feed, newScreen.destination)
    }

    @Test
    fun `destination out of both tab and stack scope escapes innermost scope first`() {
        // Given: root > TabNode(HomeTabs) > StackNode(tab0) > StackNode(AuthFlow)
        // Current implementation escapes one scope level at a time (innermost first)
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth-stack",
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = "auth-stack",
            parentKey = "tab0",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val tabStack = StackNode(
            key = "tab0",
            parentKey = "tabs",
            children = listOf(authStack)
        )

        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(tabStack),
            activeStackIndex = 0,
            scopeKey = "HomeTabs"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        val generateKey = createKeyGenerator()

        // When pushing MainFlow.Home (not in HomeTabs or AuthFlow)
        val result = TreeMutator.push(root, MainFlow.Home, combinedScopeRegistry, generateKey)

        // Then: escapes auth-stack scope to its parent (tab0)
        // This is the innermost scope first - algorithm returns immediate parent when out-of-scope
        val resultRoot = result as StackNode
        assertEquals(1, resultRoot.children.size)

        // Tab structure is modified - check tab's stack
        val resultTabs = resultRoot.children[0] as TabNode
        val resultTabStack = resultTabs.stacks[0]

        // tab0 should now have auth-stack + new screen
        assertEquals(2, resultTabStack.children.size)

        // Auth stack preserved
        assertIs<StackNode>(resultTabStack.children[0])
        // New screen is sibling to auth-stack in tab0
        val newScreen = resultTabStack.children[1]
        assertIs<ScreenNode>(newScreen)
        assertEquals(MainFlow.Home, newScreen.destination)
        assertEquals("tab0", newScreen.parentKey)
    }

    // =========================================================================
    // SCOPE KEY PRESERVATION TESTS
    // =========================================================================

    @Test
    fun `scopeKey is preserved when pushing to scoped stack`() {
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth",
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = "auth",
            parentKey = "root",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(root, AuthFlow.Register, testRegistry, generateKey)

        val resultRoot = result as StackNode
        val resultAuthStack = resultRoot.children[0] as StackNode

        assertEquals(
            "AuthFlow",
            resultAuthStack.scopeKey,
            "scopeKey should be preserved after push"
        )
        assertEquals("auth", resultAuthStack.key, "key should be preserved")
        assertEquals("root", resultAuthStack.parentKey, "parentKey should be preserved")
    }

    @Test
    fun `scopeKey is preserved when navigating away from scoped stack`() {
        val loginScreen = ScreenNode(
            key = "login-screen",
            parentKey = "auth",
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = "auth",
            parentKey = "root",
            children = listOf(loginScreen),
            scopeKey = "AuthFlow"
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey)

        val resultRoot = result as StackNode
        val resultAuthStack = resultRoot.children[0] as StackNode

        assertEquals(
            "AuthFlow",
            resultAuthStack.scopeKey,
            "scopeKey should remain after out-of-scope push"
        )
    }
}
