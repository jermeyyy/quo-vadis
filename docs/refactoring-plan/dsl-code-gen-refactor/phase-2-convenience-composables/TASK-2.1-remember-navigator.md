````markdown
# Task 2.1: Create rememberQuoVadisNavigator

> **Task Status**: ⬜ Not Started  
> **Estimated Effort**: 2 days  
> **Dependencies**: Task 1.1 (NavigationConfig), Task 1.3 (DslNavigationConfig)  
> **Blocks**: Task 2.2 (QuoVadisNavigation One-Liner)

---

## Objective

Create a Composable function `rememberQuoVadisNavigator` that creates and memoizes a Navigator instance using `NavigationConfig`. This function handles:

- Proper memoization based on config and root destination
- Coroutine scope lifecycle management
- Building the initial NavNode from config
- Integration with the generated or manual NavigationConfig

**Target Usage Pattern**:
```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(MainTabs::class)
    
    NavigationHost(
        navigator = navigator,
        config = GeneratedNavigationConfig
    )
}

// Or with explicit config
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig + FeatureConfig,
        key = "main"
    )
    // ...
}
```

---

## Files to Create/Modify

### New Files

| File | Path | Description |
|------|------|-------------|
| `QuoVadisComposables.kt` | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisComposables.kt` | Convenience composables |
| `QuoVadisComposablesTest.kt` | `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisComposablesTest.kt` | Unit tests |

### Files to Reference (Read-Only)

| File | Purpose |
|------|---------|
| `quo-vadis-core/.../navigation/NavigationConfig.kt` | Config interface (Task 1.1) |
| `quo-vadis-core/.../navigation/TreeNavigator.kt` | Navigator implementation |
| `quo-vadis-core/.../navigation/compose/NavigationHost.kt` | Existing NavigationHost |

---

## Function Signature and Implementation

### QuoVadisComposables.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.jermey.quo.vadis.core.navigation.NavigationConfig
import com.jermey.quo.vadis.core.navigation.TreeNavigator
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import kotlin.reflect.KClass

/**
 * Remember and create a Navigator with the given configuration.
 * 
 * This is the recommended way to create a Navigator in Compose. It handles:
 * - Proper memoization: Navigator is recreated only when [rootDestination], [config], or [key] changes
 * - Coroutine scope: Automatically provides a scope that follows the Composable lifecycle
 * - Initial state: Builds the initial NavNode from the config's container definitions
 * 
 * ## Basic Usage
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberQuoVadisNavigator(MainTabs::class)
 *     
 *     NavigationHost(
 *         navigator = navigator,
 *         config = GeneratedNavigationConfig
 *     )
 * }
 * ```
 * 
 * ## With Custom Config
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberQuoVadisNavigator(
 *         rootDestination = MainTabs::class,
 *         config = GeneratedNavigationConfig + FeatureModuleConfig,
 *         key = "main-navigator"
 *     )
 *     // ...
 * }
 * ```
 * 
 * ## Multi-Module Composition
 * ```kotlin
 * val combinedConfig = AppConfig + FeatureAConfig + FeatureBConfig
 * val navigator = rememberQuoVadisNavigator(
 *     rootDestination = MainTabs::class,
 *     config = combinedConfig
 * )
 * ```
 * 
 * @param rootDestination The destination class for the root container (e.g., your main tabs class).
 *                        This must be a registered container in the config.
 * @param config The NavigationConfig providing registry access and initial state building.
 *               Defaults to GeneratedNavigationConfig if available via service locator pattern,
 *               or must be provided explicitly.
 * @param key Optional custom key for the root navigator node. If null, uses the destination's
 *            default key (typically the class simple name).
 * 
 * @return A [Navigator] instance that is stable across recompositions (unless inputs change).
 * 
 * @throws IllegalStateException if [rootDestination] is not a registered container in the config.
 * 
 * @see NavigationHost for displaying navigation content
 * @see QuoVadisNavigation for a one-liner setup combining navigator + host
 * @see NavigationConfig for configuration details
 */
@Composable
fun rememberQuoVadisNavigator(
    rootDestination: KClass<out Destination>,
    config: NavigationConfig,
    key: String? = null
): Navigator {
    val coroutineScope = rememberCoroutineScope()
    
    return remember(rootDestination, config, key, coroutineScope) {
        // Build the initial navigation state from config
        val initialState = config.buildNavNode(
            destinationClass = rootDestination,
            key = key,
            parentKey = null
        ) ?: error(
            "No container registered for ${rootDestination.simpleName}. " +
            "Make sure the destination is annotated with @Tabs, @Stack, or @Pane, " +
            "or manually registered in the NavigationConfig."
        )
        
        // Create the TreeNavigator with config registries
        TreeNavigator(
            initialState = initialState,
            scopeRegistry = config.scopeRegistry,
            containerRegistry = config.containerRegistry,
            deepLinkHandler = config.deepLinkHandler,
            coroutineScope = coroutineScope
        )
    }
}

/**
 * Overload of [rememberQuoVadisNavigator] that accepts a destination instance
 * instead of a class, for cases where the root destination has constructor parameters.
 * 
 * ## Usage
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberQuoVadisNavigator(
 *         rootDestination = MainTabs(initialTab = 1),
 *         config = GeneratedNavigationConfig
 *     )
 *     // ...
 * }
 * ```
 * 
 * @param rootDestination The destination instance for the root container.
 * @param config The NavigationConfig providing registry access.
 * @param key Optional custom key for the root navigator node.
 * 
 * @return A [Navigator] instance.
 */
@Composable
fun rememberQuoVadisNavigator(
    rootDestination: Destination,
    config: NavigationConfig,
    key: String? = null
): Navigator {
    val coroutineScope = rememberCoroutineScope()
    
    return remember(rootDestination, config, key, coroutineScope) {
        val initialState = config.buildNavNode(
            destinationClass = rootDestination::class,
            key = key,
            parentKey = null
        ) ?: error(
            "No container registered for ${rootDestination::class.simpleName}. " +
            "Make sure the destination is annotated with @Tabs, @Stack, or @Pane, " +
            "or manually registered in the NavigationConfig."
        )
        
        TreeNavigator(
            initialState = initialState,
            scopeRegistry = config.scopeRegistry,
            containerRegistry = config.containerRegistry,
            deepLinkHandler = config.deepLinkHandler,
            coroutineScope = coroutineScope
        )
    }
}
```

---

## Integration with NavigationConfig

### How buildNavNode Works

The `rememberQuoVadisNavigator` function relies on `NavigationConfig.buildNavNode()` to construct the initial navigation tree:

```kotlin
// Inside NavigationConfig implementation (from Task 1.3)
override fun buildNavNode(
    destinationClass: KClass<out Destination>,
    key: String?,
    parentKey: String?
): NavNode? {
    val containerBuilder = containers[destinationClass] ?: return null
    
    return when (containerBuilder) {
        is ContainerBuilder.Tabs -> buildTabNode(containerBuilder, key, parentKey)
        is ContainerBuilder.Stack -> buildStackNode(containerBuilder, key, parentKey)
        is ContainerBuilder.Panes -> buildPaneNode(containerBuilder, key, parentKey)
    }
}
```

### Registry Integration

The navigator uses registries from the config for runtime operations:

| Registry | Navigator Usage |
|----------|-----------------|
| `scopeRegistry` | Scope membership queries during navigation |
| `containerRegistry` | Container metadata for nested navigation |
| `deepLinkHandler` | Processing incoming deep links |

Note: `screenRegistry` and `transitionRegistry` are used by `NavigationHost`, not the navigator itself. Wrapper functionality (formerly `wrapperRegistry`) is now part of `containerRegistry`.

---

## Coroutine Scope Handling

### Lifecycle Binding

```kotlin
val coroutineScope = rememberCoroutineScope()
```

- The scope is created by Compose and automatically cancelled when the Composable leaves composition
- This ensures any coroutines launched by the navigator (e.g., for deep link processing) are properly cleaned up
- The scope is included in `remember`'s key set to trigger recreation if scope identity changes

### Scope Stability

The coroutine scope from `rememberCoroutineScope()` is stable across recompositions:
- Same scope instance returned unless the Composable is removed from composition
- Navigator instance remains stable as long as inputs don't change

---

## Memoization Strategy

### Remember Keys

```kotlin
remember(rootDestination, config, key, coroutineScope) { ... }
```

The navigator is recreated when ANY of these change:

| Key | Type | Change Behavior |
|-----|------|-----------------|
| `rootDestination` | `KClass<*>` | New navigator with different root |
| `config` | `NavigationConfig` | New navigator with new registries |
| `key` | `String?` | New navigator with different node key |
| `coroutineScope` | `CoroutineScope` | New navigator (rare, lifecycle change) |

### Why Include coroutineScope?

Even though coroutine scope is typically stable, including it ensures:
1. Navigator is never used with a cancelled scope
2. Proper cleanup if composition is restructured
3. Consistent behavior across all Compose scenarios

---

## Dependencies on Phase 1

### From Task 1.1 (NavigationConfig Interface)

```kotlin
interface NavigationConfig {
    val scopeRegistry: ScopeRegistry
    val containerRegistry: ContainerRegistry
    val deepLinkHandler: DeepLinkHandler
    
    fun buildNavNode(
        destinationClass: KClass<out Destination>,
        key: String?,
        parentKey: String?
    ): NavNode?
}
```

### From Task 1.3 (DslNavigationConfig)

The actual implementation that:
- Stores container definitions from DSL
- Implements `buildNavNode()` to construct navigation trees
- Provides runtime registries

---

## Acceptance Criteria Checklist

### Core Functionality
- [ ] `rememberQuoVadisNavigator(KClass, config)` creates Navigator correctly
- [ ] `rememberQuoVadisNavigator(Destination, config)` overload works
- [ ] Navigator is memoized (same instance on recomposition)
- [ ] Navigator is recreated when `rootDestination` changes
- [ ] Navigator is recreated when `config` changes
- [ ] Navigator is recreated when `key` changes
- [ ] Error thrown when root destination not registered

### Coroutine Scope
- [ ] Coroutine scope follows Composable lifecycle
- [ ] Navigator operations work within provided scope
- [ ] No scope leaks when Composable leaves composition

### Integration
- [ ] Works with `DslNavigationConfig` (Task 1.3)
- [ ] Works with composed configs (`config1 + config2`)
- [ ] Works with `EmptyNavigationConfig` (returns error as expected)
- [ ] Compatible with existing `NavigationHost`

### Documentation
- [ ] KDoc with usage examples
- [ ] Parameter documentation complete
- [ ] Exception behavior documented
- [ ] See also links to related APIs

### Testing
- [ ] Unit test: Navigator created correctly
- [ ] Unit test: Memoization - same instance on recomposition
- [ ] Unit test: Recreation on input change
- [ ] Unit test: Error handling for unregistered destination
- [ ] Unit test: Coroutine scope cancellation behavior

### Code Quality
- [ ] No compiler warnings
- [ ] Follows project code style
- [ ] Proper imports organized

---

## Unit Test Scenarios

### Test File: QuoVadisComposablesTest.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.jermey.quo.vadis.core.navigation.NavigationConfig
import com.jermey.quo.vadis.core.navigation.core.Destination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertFailsWith

class RememberQuoVadisNavigatorTest {
    
    @get:Rule
    val composeRule = createComposeRule()
    
    // ─────────────────────────────────────────────────────────
    // Memoization Tests
    // ─────────────────────────────────────────────────────────
    
    @Test
    fun `navigator is memoized across recompositions`() {
        var navigator1: Navigator? = null
        var navigator2: Navigator? = null
        var recomposeCount by mutableStateOf(0)
        
        composeRule.setContent {
            // Force recomposition by reading state
            val _ = recomposeCount
            
            val navigator = rememberQuoVadisNavigator(
                rootDestination = TestTabs::class,
                config = TestNavigationConfig
            )
            
            if (navigator1 == null) {
                navigator1 = navigator
            } else {
                navigator2 = navigator
            }
        }
        
        // Trigger recomposition
        recomposeCount++
        composeRule.waitForIdle()
        
        assertSame(navigator1, navigator2, "Navigator should be same instance")
    }
    
    @Test
    fun `navigator is recreated when rootDestination changes`() {
        var navigator1: Navigator? = null
        var navigator2: Navigator? = null
        var destination by mutableStateOf<KClass<out Destination>>(TestTabs::class)
        
        composeRule.setContent {
            val navigator = rememberQuoVadisNavigator(
                rootDestination = destination,
                config = TestNavigationConfig
            )
            
            if (destination == TestTabs::class) {
                navigator1 = navigator
            } else {
                navigator2 = navigator
            }
        }
        
        // Change destination
        destination = OtherTabs::class
        composeRule.waitForIdle()
        
        assertNotSame(navigator1, navigator2, "Navigator should be different instance")
    }
    
    @Test
    fun `navigator is recreated when config changes`() {
        var navigator1: Navigator? = null
        var navigator2: Navigator? = null
        var config by mutableStateOf(TestNavigationConfig)
        
        composeRule.setContent {
            val navigator = rememberQuoVadisNavigator(
                rootDestination = TestTabs::class,
                config = config
            )
            
            if (config === TestNavigationConfig) {
                navigator1 = navigator
            } else {
                navigator2 = navigator
            }
        }
        
        // Change config
        config = TestNavigationConfig + AdditionalConfig
        composeRule.waitForIdle()
        
        assertNotSame(navigator1, navigator2, "Navigator should be different instance")
    }
    
    @Test
    fun `navigator is recreated when key changes`() {
        var navigator1: Navigator? = null
        var navigator2: Navigator? = null
        var key by mutableStateOf<String?>(null)
        
        composeRule.setContent {
            val navigator = rememberQuoVadisNavigator(
                rootDestination = TestTabs::class,
                config = TestNavigationConfig,
                key = key
            )
            
            if (key == null) {
                navigator1 = navigator
            } else {
                navigator2 = navigator
            }
        }
        
        // Change key
        key = "custom-key"
        composeRule.waitForIdle()
        
        assertNotSame(navigator1, navigator2, "Navigator should be different instance")
    }
    
    // ─────────────────────────────────────────────────────────
    // Error Handling Tests
    // ─────────────────────────────────────────────────────────
    
    @Test
    fun `throws error for unregistered destination`() {
        assertFailsWith<IllegalStateException> {
            composeRule.setContent {
                rememberQuoVadisNavigator(
                    rootDestination = UnregisteredDestination::class,
                    config = TestNavigationConfig
                )
            }
        }
    }
    
    // ─────────────────────────────────────────────────────────
    // Integration Tests
    // ─────────────────────────────────────────────────────────
    
    @Test
    fun `works with composed configs`() {
        var navigator: Navigator? = null
        val composedConfig = TestNavigationConfig + FeatureConfig
        
        composeRule.setContent {
            navigator = rememberQuoVadisNavigator(
                rootDestination = TestTabs::class,
                config = composedConfig
            )
        }
        
        composeRule.waitForIdle()
        assertNotNull(navigator)
    }
    
    @Test
    fun `destination instance overload works`() {
        var navigator: Navigator? = null
        
        composeRule.setContent {
            navigator = rememberQuoVadisNavigator(
                rootDestination = TestTabs(initialTab = 1),
                config = TestNavigationConfig
            )
        }
        
        composeRule.waitForIdle()
        assertNotNull(navigator)
    }
    
    // ─────────────────────────────────────────────────────────
    // Test Fixtures
    // ─────────────────────────────────────────────────────────
    
    sealed class TestTabs : Destination {
        data class HomeTab(val initialTab: Int = 0) : TestTabs()
        object ExploreTab : TestTabs()
    }
    
    sealed class OtherTabs : Destination {
        object Tab1 : OtherTabs()
    }
    
    class UnregisteredDestination : Destination
    
    // Mock configs would be implemented using DslNavigationConfig or test doubles
}
```

---

## Implementation Notes

### Design Decisions

1. **Two Overloads**
   - `KClass<*>` version for standard container classes
   - `Destination` instance version for parameterized destinations

2. **No Default Config**
   - Config is required to avoid implicit service locator pattern
   - Makes dependency explicit and testable

3. **Error on Missing Registration**
   - Fails fast with clear error message
   - Better developer experience than silent failures

4. **Coroutine Scope from Compose**
   - Uses `rememberCoroutineScope()` for lifecycle binding
   - Avoids manual scope management

### Potential Issues

1. **TreeNavigator Constructor**
   - Verify exact constructor signature matches current implementation
   - May need adjustment based on actual TreeNavigator API

2. **Config Reference Stability**
   - Object configs (like `GeneratedNavigationConfig`) have stable identity
   - Composed configs (`a + b`) create new instances - could cause unexpected recreation
   - Document this behavior

3. **Testing on All Platforms**
   - Compose test infrastructure varies by platform
   - May need platform-specific test implementations

---

## Related Files

- [Phase 2 Summary](./SUMMARY.md)
- [Task 2.2 - QuoVadisNavigation One-Liner](./TASK-2.2-one-liner-composable.md)
- [Task 2.3 - NavigationHost Config Overload](./TASK-2.3-navigation-host-overload.md)
- [Task 1.1 - NavigationConfig Interface](../phase-1-core-dsl-infrastructure/TASK-1.1-navigation-config-interface.md)
- [Task 1.3 - DslNavigationConfig Implementation](../phase-1-core-dsl-infrastructure/TASK-1.3-dsl-navigation-config-impl.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)

````
