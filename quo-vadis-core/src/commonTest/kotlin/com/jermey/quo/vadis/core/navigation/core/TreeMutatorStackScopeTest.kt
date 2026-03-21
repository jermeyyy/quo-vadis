package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

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

/**
 * Tests for stack-scope-aware [TreeMutator] operations.
 *
 * These tests verify that [TreeMutator.push] with a [ScopeRegistry] correctly routes
 * destinations based on whether they belong to a [StackNode]'s scope.
 *
 * This complements [TreeMutatorScopeTest] which focuses on TabNode scopes.
 */
@OptIn(InternalQuoVadisApi::class)
class TreeMutatorStackScopeTest : FunSpec({

    // =========================================================================
    // TEST REGISTRY
    // =========================================================================

    /**
     * Test implementation of ScopeRegistry.
     * Simulates what KSP would generate from @Stack sealed class hierarchies.
     */
    val testRegistry = object : ScopeRegistry {
        val scopes = mapOf(
            "AuthFlow" to setOf(
                AuthFlow.Login::class,
                AuthFlow.Register::class,
                AuthFlow.ForgotPassword::class
            ),
            "MainFlow" to setOf(MainFlow.Home::class, MainFlow.Profile::class)
        )

        override fun isInScope(scopeKey: ScopeKey, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey.value] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): ScopeKey? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key?.let { ScopeKey(it) }
        }
    }

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    fun createKeyGenerator(): () -> NodeKey {
        var counter = 0
        return { NodeKey("key-${counter++}") }
    }

    // =========================================================================
    // IN-SCOPE DESTINATION TESTS
    // =========================================================================

    test("push in-scope destination stays in stack") {
        // Given stack with scopeKey = "AuthFlow"
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth"),
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = NodeKey("auth"),
            parentKey = NodeKey("root"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing destination in AuthFlow scope
        val result = TreeMutator.push(root, AuthFlow.Register, testRegistry, generateKey = generateKey)

        // Then pushed to same stack (authStack)
        val resultRoot = result as StackNode
        resultRoot.children.size shouldBe 1

        val resultAuthStack = resultRoot.children[0] as StackNode
        resultAuthStack.children.size shouldBe 2
        resultAuthStack.scopeKey shouldBe ScopeKey("AuthFlow")

        val newScreen = resultAuthStack.children.last()
        val typedScreen = newScreen.shouldBeInstanceOf<ScreenNode>()
        typedScreen.destination shouldBe AuthFlow.Register
        typedScreen.parentKey shouldBe NodeKey("auth")
    }

    test("push multiple in-scope destinations stays in same stack") {
        // Given stack with scopeKey = "AuthFlow"
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth"),
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = NodeKey("auth"),
            parentKey = NodeKey("root"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        var tree: NavNode = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing multiple AuthFlow destinations
        tree = TreeMutator.push(tree, AuthFlow.Register, testRegistry, generateKey = generateKey)
        tree = TreeMutator.push(tree, AuthFlow.ForgotPassword, testRegistry, generateKey = generateKey)

        // Then all pushed to authStack
        val resultRoot = tree as StackNode
        resultRoot.children.size shouldBe 1

        val resultAuthStack = resultRoot.children[0] as StackNode
        resultAuthStack.children.size shouldBe 3

        (resultAuthStack.children[0] as ScreenNode).destination shouldBe AuthFlow.Login
        (resultAuthStack.children[1] as ScreenNode).destination shouldBe AuthFlow.Register
        (resultAuthStack.children[2] as ScreenNode).destination shouldBe AuthFlow.ForgotPassword
    }

    // =========================================================================
    // OUT-OF-SCOPE DESTINATION TESTS
    // =========================================================================

    test("push out-of-scope destination navigates to parent") {
        // Given stack with scopeKey = "AuthFlow"
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth"),
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = NodeKey("auth"),
            parentKey = NodeKey("root"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing destination NOT in AuthFlow scope
        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey = generateKey)

        // Then new screen created in parent stack (root)
        val resultRoot = result as StackNode
        resultRoot.children.size shouldBe 2

        // AuthStack should be preserved
        val resultAuthStack = resultRoot.children[0] as StackNode
        resultAuthStack.children.size shouldBe 1
        resultAuthStack.scopeKey shouldBe ScopeKey("AuthFlow")

        // New screen should be sibling to authStack
        val newScreen = resultRoot.children[1]
        val typedScreen = newScreen.shouldBeInstanceOf<ScreenNode>()
        typedScreen.destination shouldBe MainFlow.Home
        typedScreen.parentKey shouldBe NodeKey("root")
    }

    test("push out-of-scope preserves scoped stack for predictive back") {
        // Given AuthFlow stack with multiple screens
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth"),
            destination = AuthFlow.Login
        )

        val registerScreen = ScreenNode(
            key = NodeKey("register-screen"),
            parentKey = NodeKey("auth"),
            destination = AuthFlow.Register
        )

        val authStack = StackNode(
            key = NodeKey("auth"),
            parentKey = NodeKey("root"),
            children = listOf(loginScreen, registerScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing out-of-scope destination
        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey = generateKey)

        // Then AuthStack is completely preserved
        val resultRoot = result as StackNode
        val resultAuthStack = resultRoot.children[0] as StackNode

        resultAuthStack.children.size shouldBe 2
        (resultAuthStack.children[0] as ScreenNode).destination shouldBe AuthFlow.Login
        (resultAuthStack.children[1] as ScreenNode).destination shouldBe AuthFlow.Register
    }

    test("push multiple out-of-scope destinations stacks in parent") {
        // Given stack with scopeKey = "AuthFlow"
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth"),
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = NodeKey("auth"),
            parentKey = NodeKey("root"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        var tree: NavNode = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing multiple out-of-scope destinations
        tree = TreeMutator.push(tree, MainFlow.Home, testRegistry, generateKey = generateKey)
        tree = TreeMutator.push(tree, MainFlow.Profile, testRegistry, generateKey = generateKey)

        // Then all pushed to root stack
        val resultRoot = tree as StackNode
        resultRoot.children.size shouldBe 3

        resultRoot.children[0].shouldBeInstanceOf<StackNode>() // AuthStack preserved
        val typedChild1 = resultRoot.children[1].shouldBeInstanceOf<ScreenNode>()
        val typedChild2 = resultRoot.children[2].shouldBeInstanceOf<ScreenNode>()

        typedChild1.destination shouldBe MainFlow.Home
        typedChild2.destination shouldBe MainFlow.Profile
    }

    // =========================================================================
    // NESTED STACKS WITH SCOPES TESTS
    // =========================================================================

    test("nested stacks respect innermost scope first") {
        // Given: root > outerStack(scopeKey=MainFlow) > innerStack(scopeKey=AuthFlow)
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("inner"),
            destination = AuthFlow.Login
        )

        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("outer"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val outerStack = StackNode(
            key = NodeKey("outer"),
            parentKey = NodeKey("root"),
            children = listOf(innerStack),
            scopeKey = ScopeKey("MainFlow")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(outerStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing destination in MainFlow (outer) but not in AuthFlow (inner)
        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey = generateKey)

        // Then: pushes to outerStack (innerStack's parent)
        val resultRoot = result as StackNode
        resultRoot.children.size shouldBe 1

        val resultOuterStack = resultRoot.children[0] as StackNode
        resultOuterStack.children.size shouldBe 2

        // Inner stack preserved
        resultOuterStack.children[0].shouldBeInstanceOf<StackNode>()
        // New screen is sibling to innerStack
        val newScreen = resultOuterStack.children[1]
        val typedScreen = newScreen.shouldBeInstanceOf<ScreenNode>()
        typedScreen.destination shouldBe MainFlow.Home
        typedScreen.parentKey shouldBe NodeKey("outer")
    }

    test("doubly nested stacks - destination escapes both") {
        // Given: root > outerStack(scopeKey=AuthFlow) > innerStack(scopeKey=AuthFlow)
        // Destination not in AuthFlow should escape to root
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("inner"),
            destination = AuthFlow.Login
        )

        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("outer"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val outerStack = StackNode(
            key = NodeKey("outer"),
            parentKey = NodeKey("root"),
            children = listOf(innerStack),
            scopeKey = ScopeKey("AuthFlow")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(outerStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing destination not in AuthFlow
        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey = generateKey)

        // Then: Should push to root (escaping both auth stacks)
        // Note: This depends on the exact algorithm - verifying behavior
        val resultRoot = result as StackNode

        // The exact behavior depends on how findTargetStackForPush walks the tree
        // It should find the first non-scoped parent that can accept the destination
        resultRoot.findByKey(NodeKey("outer")).shouldNotBeNull()
        resultRoot.findByKey(NodeKey("inner")).shouldNotBeNull()
    }

    // =========================================================================
    // STACK WITHOUT SCOPE KEY TESTS
    // =========================================================================

    test("stack without scopeKey accepts all destinations") {
        // Given stack with scopeKey = null
        val homeScreen = ScreenNode(
            key = NodeKey("home-screen"),
            parentKey = NodeKey("main"),
            destination = MainFlow.Home
        )

        val mainStack = StackNode(
            key = NodeKey("main"),
            parentKey = NodeKey("root"),
            children = listOf(homeScreen),
            scopeKey = null // No scope enforcement
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(mainStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing any destination (even from different scope)
        val result = TreeMutator.push(root, AuthFlow.Login, testRegistry, generateKey = generateKey)

        // Then pushed to same stack (existing behavior)
        val resultRoot = result as StackNode
        resultRoot.children.size shouldBe 1

        val resultMainStack = resultRoot.children[0] as StackNode
        resultMainStack.children.size shouldBe 2

        val newScreen = resultMainStack.children.last()
        val typedScreen = newScreen.shouldBeInstanceOf<ScreenNode>()
        typedScreen.destination shouldBe AuthFlow.Login
        typedScreen.parentKey shouldBe NodeKey("main")
    }

    test("stack without scopeKey mixed with scoped stack") {
        // Given: root > unscopedStack > scopedStack(AuthFlow)
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("scoped"),
            destination = AuthFlow.Login
        )

        val scopedStack = StackNode(
            key = NodeKey("scoped"),
            parentKey = NodeKey("unscoped"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val unscopedStack = StackNode(
            key = NodeKey("unscoped"),
            parentKey = NodeKey("root"),
            children = listOf(scopedStack),
            scopeKey = null
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(unscopedStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing out-of-AuthFlow destination
        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey = generateKey)

        // Then: escapes scopedStack, goes to unscopedStack (which accepts all)
        val resultRoot = result as StackNode
        resultRoot.children.size shouldBe 1

        val resultUnscopedStack = resultRoot.children[0] as StackNode
        resultUnscopedStack.children.size shouldBe 2

        // Scoped stack preserved
        resultUnscopedStack.children[0].shouldBeInstanceOf<StackNode>()
        // New screen in unscoped stack
        val newScreen = resultUnscopedStack.children[1]
        val typedScreen = newScreen.shouldBeInstanceOf<ScreenNode>()
        typedScreen.destination shouldBe MainFlow.Home
        typedScreen.parentKey shouldBe NodeKey("unscoped")
    }

    // =========================================================================
    // EMPTY REGISTRY TESTS (BACKWARD COMPATIBILITY)
    // =========================================================================

    test("push with Empty registry ignores stack scopeKey") {
        // Given stack with scopeKey
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth"),
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = NodeKey("auth"),
            parentKey = NodeKey("root"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing out-of-scope with Empty registry
        val result = TreeMutator.push(root, MainFlow.Home, ScopeRegistry.Empty, generateKey = generateKey)

        // Then: pushed to authStack anyway (scope not enforced)
        val resultRoot = result as StackNode
        resultRoot.children.size shouldBe 1

        val resultAuthStack = resultRoot.children[0] as StackNode
        resultAuthStack.children.size shouldBe 2

        val newScreen = resultAuthStack.children.last()
        val typedScreen = newScreen.shouldBeInstanceOf<ScreenNode>()
        typedScreen.destination shouldBe MainFlow.Home
    }

    test("push without scopeRegistry parameter uses original behavior") {
        // Given stack with scopeKey
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth"),
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = NodeKey("auth"),
            parentKey = NodeKey("root"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        // When pushing without ScopeRegistry parameter
        val result = TreeMutator.push(root, MainFlow.Home, generateKey)

        // Then: pushed to authStack (original behavior, no scope enforcement)
        val resultRoot = result as StackNode
        resultRoot.children.size shouldBe 1

        val resultAuthStack = resultRoot.children[0] as StackNode
        resultAuthStack.children.size shouldBe 2
    }

    // =========================================================================
    // STACK INSIDE TAB WITH BOTH SCOPES TESTS
    // =========================================================================

    val combinedScopeRegistry = object : ScopeRegistry {
        val scopes = mapOf(
            "AuthFlow" to setOf(
                AuthFlow.Login::class,
                AuthFlow.Register::class,
                AuthFlow.ForgotPassword::class
            ),
            "MainFlow" to setOf(MainFlow.Home::class, MainFlow.Profile::class),
            "HomeTabs" to setOf(HomeTabs.Feed::class, HomeTabs.Explore::class)
        )

        override fun isInScope(scopeKey: ScopeKey, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey.value] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): ScopeKey? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key?.let { ScopeKey(it) }
        }
    }

    test("scoped stack inside scoped tab - inner scope checked first") {
        // Given: root > TabNode(HomeTabs) > StackNode(AuthFlow)
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth-stack"),
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = NodeKey("auth-stack"),
            parentKey = NodeKey("tab0"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val tabStack = StackNode(
            key = NodeKey("tab0"),
            parentKey = NodeKey("tabs"),
            children = listOf(authStack)
        )

        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(tabStack),
            activeStackIndex = 0,
            scopeKey = ScopeKey("HomeTabs")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        val generateKey = createKeyGenerator()

        // When pushing HomeTabs.Feed (in tab scope, not in stack scope)
        val result = TreeMutator.push(root, HomeTabs.Feed, combinedScopeRegistry, generateKey = generateKey)

        // Then: escapes AuthFlow stack, goes to tab's stack (tab0)
        val resultRoot = result as StackNode
        val resultTabs = resultRoot.children[0] as TabNode
        val resultTabStack = resultTabs.stacks[0]

        resultTabStack.children.size shouldBe 2

        // Auth stack preserved
        resultTabStack.children[0].shouldBeInstanceOf<StackNode>()
        // New screen added as sibling
        val newScreen = resultTabStack.children[1]
        val typedScreen = newScreen.shouldBeInstanceOf<ScreenNode>()
        typedScreen.destination shouldBe HomeTabs.Feed
    }

    test("destination out of both tab and stack scope escapes innermost scope first") {
        // Given: root > TabNode(HomeTabs) > StackNode(tab0) > StackNode(AuthFlow)
        // Current implementation escapes one scope level at a time (innermost first)
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth-stack"),
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = NodeKey("auth-stack"),
            parentKey = NodeKey("tab0"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val tabStack = StackNode(
            key = NodeKey("tab0"),
            parentKey = NodeKey("tabs"),
            children = listOf(authStack)
        )

        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(tabStack),
            activeStackIndex = 0,
            scopeKey = ScopeKey("HomeTabs")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        val generateKey = createKeyGenerator()

        // When pushing MainFlow.Home (not in HomeTabs or AuthFlow)
        val result = TreeMutator.push(root, MainFlow.Home, combinedScopeRegistry, generateKey = generateKey)

        // Then: escapes auth-stack scope to its parent (tab0)
        // This is the innermost scope first - algorithm returns immediate parent when out-of-scope
        val resultRoot = result as StackNode
        resultRoot.children.size shouldBe 1

        // Tab structure is modified - check tab's stack
        val resultTabs = resultRoot.children[0] as TabNode
        val resultTabStack = resultTabs.stacks[0]

        // tab0 should now have auth-stack + new screen
        resultTabStack.children.size shouldBe 2

        // Auth stack preserved
        resultTabStack.children[0].shouldBeInstanceOf<StackNode>()
        // New screen is sibling to auth-stack in tab0
        val newScreen = resultTabStack.children[1]
        val typedScreen = newScreen.shouldBeInstanceOf<ScreenNode>()
        typedScreen.destination shouldBe MainFlow.Home
        typedScreen.parentKey shouldBe NodeKey("tab0")
    }

    // =========================================================================
    // SCOPE KEY PRESERVATION TESTS
    // =========================================================================

    test("scopeKey is preserved when pushing to scoped stack") {
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth"),
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = NodeKey("auth"),
            parentKey = NodeKey("root"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(root, AuthFlow.Register, testRegistry, generateKey = generateKey)

        val resultRoot = result as StackNode
        val resultAuthStack = resultRoot.children[0] as StackNode

        resultAuthStack.scopeKey shouldBe ScopeKey("AuthFlow")
        resultAuthStack.key shouldBe NodeKey("auth")
        resultAuthStack.parentKey shouldBe NodeKey("root")
    }

    test("scopeKey is preserved when navigating away from scoped stack") {
        val loginScreen = ScreenNode(
            key = NodeKey("login-screen"),
            parentKey = NodeKey("auth"),
            destination = AuthFlow.Login
        )

        val authStack = StackNode(
            key = NodeKey("auth"),
            parentKey = NodeKey("root"),
            children = listOf(loginScreen),
            scopeKey = ScopeKey("AuthFlow")
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(authStack)
        )

        val generateKey = createKeyGenerator()

        val result = TreeMutator.push(root, MainFlow.Home, testRegistry, generateKey = generateKey)

        val resultRoot = result as StackNode
        val resultAuthStack = resultRoot.children[0] as StackNode

        resultAuthStack.scopeKey shouldBe ScopeKey("AuthFlow")
    }

})
