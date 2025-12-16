# Task 1.1: Create NavigationConfig Interface

> **Task Status**: ⬜ Not Started  
> **Estimated Effort**: 2-3 days  
> **Dependencies**: None (first task)  
> **Blocks**: Task 1.2, Task 1.3

---

## Objective

Create the `NavigationConfig` interface that serves as the unified contract for all navigation registries. This interface will consolidate access to screens, wrappers, scopes, transitions, containers, and deep link handlers into a single, composable configuration object.

**Target Usage Pattern**:
```kotlin
// Access individual registries
val screens = config.screenRegistry
val scopes = config.scopeRegistry

// Build initial navigation state
val navNode = config.buildNavNode(MainTabs::class)

// Compose multiple configs (multi-module support)
val combined = AppConfig + FeatureAConfig + FeatureBConfig
```

---

## Files to Create/Modify

### New Files

| File | Path | Description |
|------|------|-------------|
| `NavigationConfig.kt` | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/NavigationConfig.kt` | Main interface definition |
| `EmptyNavigationConfig.kt` | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/EmptyNavigationConfig.kt` | No-op default implementation |
| `CompositeNavigationConfig.kt` | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/CompositeNavigationConfig.kt` | Composition support |

### Files to Reference (Read-Only)

| File | Purpose |
|------|---------|
| `quo-vadis-core/.../compose/registry/ScreenRegistry.kt` | Screen registry interface |
| `quo-vadis-core/.../compose/registry/WrapperRegistry.kt` | Wrapper registry interface |
| `quo-vadis-core/.../compose/registry/ScopeRegistry.kt` | Scope registry interface |
| `quo-vadis-core/.../compose/registry/TransitionRegistry.kt` | Transition registry interface |
| `quo-vadis-core/.../compose/registry/ContainerRegistry.kt` | Container registry interface |
| `quo-vadis-core/.../core/GeneratedDeepLinkHandler.kt` | Deep link handler interface |

---

## Interface Definitions

### NavigationConfig.kt

```kotlin
package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.navigation.compose.registry.*
import com.jermey.quo.vadis.core.navigation.core.*
import kotlin.reflect.KClass

/**
 * Unified configuration for all navigation registries.
 * 
 * NavigationConfig consolidates all navigation-related registries into a single
 * composable interface. This enables:
 * - Single-point configuration for NavigationHost
 * - Multi-module composition via the [plus] operator
 * - Type-safe initial state building via [buildNavNode]
 * 
 * ## Usage
 * 
 * ### Basic Usage
 * ```kotlin
 * NavigationHost(
 *     navigator = navigator,
 *     config = GeneratedNavigationConfig
 * )
 * ```
 * 
 * ### Multi-Module Composition
 * ```kotlin
 * val combinedConfig = AppNavigationConfig + 
 *                      FeatureAConfig + 
 *                      FeatureBConfig
 * ```
 * 
 * ### Building Initial State
 * ```kotlin
 * val initialState = config.buildNavNode(MainTabs::class)
 * ```
 * 
 * @see DslNavigationConfig for DSL-based implementation
 * @see CompositeNavigationConfig for composition support
 */
interface NavigationConfig {
    
    /**
     * Registry for screen content.
     * Maps destinations to their Composable content.
     */
    val screenRegistry: ScreenRegistry
    
    /**
     * Registry for tab and pane wrappers.
     * Provides custom wrapper Composables for containers.
     */
    val wrapperRegistry: WrapperRegistry
    
    /**
     * Registry for navigation scopes.
     * Defines which destinations belong to which scopes.
     */
    val scopeRegistry: ScopeRegistry
    
    /**
     * Registry for navigation transitions.
     * Maps destinations to their enter/exit animations.
     */
    val transitionRegistry: TransitionRegistry
    
    /**
     * Registry for container information.
     * Provides metadata about how containers are structured.
     */
    val containerRegistry: ContainerRegistry
    
    /**
     * Handler for deep link navigation.
     * Processes deep link URIs into navigation actions.
     */
    val deepLinkHandler: DeepLinkHandler
    
    /**
     * Builds the initial NavNode for the given destination class.
     * 
     * This method constructs the complete navigation tree starting from
     * the specified container destination. The resulting NavNode can be
     * used to initialize a Navigator.
     * 
     * @param destinationClass The root destination class (must be a registered container)
     * @param key Optional custom key for the root node. If null, uses destination's default key.
     * @param parentKey Optional parent key for nested navigation scenarios.
     * @return The constructed NavNode, or null if no container is registered for the class.
     * 
     * @throws IllegalArgumentException if destinationClass is not a registered container
     * 
     * ## Example
     * ```kotlin
     * val tabNode = config.buildNavNode(
     *     destinationClass = MainTabs::class,
     *     key = "main",
     *     parentKey = null
     * )
     * ```
     */
    fun buildNavNode(
        destinationClass: KClass<out Destination>,
        key: String? = null,
        parentKey: String? = null
    ): NavNode?
    
    /**
     * Combines this config with another, returning a composite config.
     * 
     * When looking up registrations, the resulting config will:
     * - Check [other] first (later configs take priority)
     * - Fall back to [this] if not found in [other]
     * 
     * This enables modular composition:
     * ```kotlin
     * val combined = BaseConfig + FeatureConfig
     * // FeatureConfig registrations override BaseConfig
     * ```
     * 
     * @param other The config to combine with this one
     * @return A new NavigationConfig that combines both
     */
    operator fun plus(other: NavigationConfig): NavigationConfig
    
    companion object {
        /**
         * An empty NavigationConfig with no registrations.
         * Useful as a default or for testing.
         */
        val Empty: NavigationConfig = EmptyNavigationConfig
    }
}
```

### EmptyNavigationConfig.kt

```kotlin
package com.jermey.quo.vadis.core.navigation

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.compose.registry.*
import com.jermey.quo.vadis.core.navigation.compose.wrapper.*
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.core.*
import kotlin.reflect.KClass

/**
 * Empty implementation of NavigationConfig.
 * 
 * All lookups return null/empty/no-op results. This is useful as:
 * - A default value for optional config parameters
 * - A base for composition in multi-module setups
 * - A testing mock
 * 
 * @see NavigationConfig.Empty for the singleton instance
 */
internal object EmptyNavigationConfig : NavigationConfig {
    
    override val screenRegistry: ScreenRegistry = object : ScreenRegistry {
        @Composable
        override fun Content(
            destination: Destination,
            navigator: Navigator,
            scope: NavRenderScope
        ) {
            // No-op: no screens registered
        }
    }
    
    override val wrapperRegistry: WrapperRegistry = object : WrapperRegistry {
        @Composable
        override fun TabWrapper(
            wrapperKey: String,
            scope: TabWrapperScope
        ) {
            scope.content()
        }
        
        @Composable
        override fun PaneWrapper(
            wrapperKey: String,
            scope: PaneWrapperScope
        ) {
            scope.content()
        }
    }
    
    override val scopeRegistry: ScopeRegistry = object : ScopeRegistry {
        override fun getScopeKey(destination: Destination): String? = null
        override fun getDestinationsInScope(scopeKey: String): Set<KClass<out Destination>> = emptySet()
    }
    
    override val transitionRegistry: TransitionRegistry = object : TransitionRegistry {
        override fun getTransition(destination: Destination): NavTransition? = null
    }
    
    override val containerRegistry: ContainerRegistry = object : ContainerRegistry {
        override fun getContainerInfo(destination: Destination): ContainerInfo? = null
    }
    
    override val deepLinkHandler: DeepLinkHandler = object : DeepLinkHandler {
        override fun handle(deepLink: DeepLink, navigator: Navigator): Boolean = false
        override fun canHandle(deepLink: DeepLink): Boolean = false
    }
    
    override fun buildNavNode(
        destinationClass: KClass<out Destination>,
        key: String?,
        parentKey: String?
    ): NavNode? = null
    
    override fun plus(other: NavigationConfig): NavigationConfig = other
}
```

### CompositeNavigationConfig.kt

```kotlin
package com.jermey.quo.vadis.core.navigation

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.compose.registry.*
import com.jermey.quo.vadis.core.navigation.compose.wrapper.*
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.core.*
import kotlin.reflect.KClass

/**
 * A NavigationConfig that combines multiple configs with priority ordering.
 * 
 * When looking up registrations:
 * 1. [secondary] is checked first (higher priority)
 * 2. [primary] is checked if not found in secondary
 * 
 * This enables a layered configuration approach where feature modules
 * can override or extend the base configuration.
 * 
 * ## Example
 * ```kotlin
 * val base = AppNavigationConfig
 * val feature = FeatureModuleConfig
 * val combined = CompositeNavigationConfig(base, feature)
 * // feature registrations take priority over base
 * ```
 * 
 * @property primary The base configuration (lower priority)
 * @property secondary The override configuration (higher priority)
 */
internal class CompositeNavigationConfig(
    private val primary: NavigationConfig,
    private val secondary: NavigationConfig
) : NavigationConfig {
    
    override val screenRegistry: ScreenRegistry = CompositeScreenRegistry(
        primary = primary.screenRegistry,
        secondary = secondary.screenRegistry
    )
    
    override val wrapperRegistry: WrapperRegistry = CompositeWrapperRegistry(
        primary = primary.wrapperRegistry,
        secondary = secondary.wrapperRegistry
    )
    
    override val scopeRegistry: ScopeRegistry = CompositeScopeRegistry(
        primary = primary.scopeRegistry,
        secondary = secondary.scopeRegistry
    )
    
    override val transitionRegistry: TransitionRegistry = CompositeTransitionRegistry(
        primary = primary.transitionRegistry,
        secondary = secondary.transitionRegistry
    )
    
    override val containerRegistry: ContainerRegistry = CompositeContainerRegistry(
        primary = primary.containerRegistry,
        secondary = secondary.containerRegistry
    )
    
    override val deepLinkHandler: DeepLinkHandler = CompositeDeepLinkHandler(
        primary = primary.deepLinkHandler,
        secondary = secondary.deepLinkHandler
    )
    
    override fun buildNavNode(
        destinationClass: KClass<out Destination>,
        key: String?,
        parentKey: String?
    ): NavNode? {
        // Try secondary (higher priority) first
        return secondary.buildNavNode(destinationClass, key, parentKey)
            ?: primary.buildNavNode(destinationClass, key, parentKey)
    }
    
    override fun plus(other: NavigationConfig): NavigationConfig {
        return CompositeNavigationConfig(this, other)
    }
}

// ============================================================
// Composite Registry Implementations
// ============================================================

private class CompositeScreenRegistry(
    private val primary: ScreenRegistry,
    private val secondary: ScreenRegistry
) : ScreenRegistry {
    @Composable
    override fun Content(
        destination: Destination,
        navigator: Navigator,
        scope: NavRenderScope
    ) {
        // TODO: Implement lookup logic to check secondary first
        // For now, delegate to secondary with fallback to primary
        secondary.Content(destination, navigator, scope)
    }
}

private class CompositeWrapperRegistry(
    private val primary: WrapperRegistry,
    private val secondary: WrapperRegistry
) : WrapperRegistry {
    @Composable
    override fun TabWrapper(wrapperKey: String, scope: TabWrapperScope) {
        secondary.TabWrapper(wrapperKey, scope)
    }
    
    @Composable
    override fun PaneWrapper(wrapperKey: String, scope: PaneWrapperScope) {
        secondary.PaneWrapper(wrapperKey, scope)
    }
}

private class CompositeScopeRegistry(
    private val primary: ScopeRegistry,
    private val secondary: ScopeRegistry
) : ScopeRegistry {
    override fun getScopeKey(destination: Destination): String? {
        return secondary.getScopeKey(destination) ?: primary.getScopeKey(destination)
    }
    
    override fun getDestinationsInScope(scopeKey: String): Set<KClass<out Destination>> {
        val secondarySet = secondary.getDestinationsInScope(scopeKey)
        val primarySet = primary.getDestinationsInScope(scopeKey)
        return secondarySet + primarySet
    }
}

private class CompositeTransitionRegistry(
    private val primary: TransitionRegistry,
    private val secondary: TransitionRegistry
) : TransitionRegistry {
    override fun getTransition(destination: Destination): NavTransition? {
        return secondary.getTransition(destination) ?: primary.getTransition(destination)
    }
}

private class CompositeContainerRegistry(
    private val primary: ContainerRegistry,
    private val secondary: ContainerRegistry
) : ContainerRegistry {
    override fun getContainerInfo(destination: Destination): ContainerInfo? {
        return secondary.getContainerInfo(destination) ?: primary.getContainerInfo(destination)
    }
}

private class CompositeDeepLinkHandler(
    private val primary: DeepLinkHandler,
    private val secondary: DeepLinkHandler
) : DeepLinkHandler {
    override fun handle(deepLink: DeepLink, navigator: Navigator): Boolean {
        // Try secondary first, fall back to primary
        return secondary.handle(deepLink, navigator) || primary.handle(deepLink, navigator)
    }
    
    override fun canHandle(deepLink: DeepLink): Boolean {
        return secondary.canHandle(deepLink) || primary.canHandle(deepLink)
    }
}
```

---

## Dependencies on Other Tasks

| Task | Dependency Type | Description |
|------|-----------------|-------------|
| None | - | This is the first task in Phase 1 |

**Blocks**:
- Task 1.2 (DSL Builder Infrastructure) - needs `NavigationConfig` return type
- Task 1.3 (DslNavigationConfig) - implements this interface

---

## Acceptance Criteria Checklist

### Interface Design
- [ ] `NavigationConfig` interface defined with all 6 registry properties
- [ ] `buildNavNode()` method with proper signature and documentation
- [ ] `plus` operator defined for composition
- [ ] `Empty` companion object property providing default instance
- [ ] All public APIs have KDoc documentation

### EmptyNavigationConfig
- [ ] Implements all `NavigationConfig` properties
- [ ] All registries return no-op/empty results
- [ ] `buildNavNode()` returns `null`
- [ ] `plus()` returns the other config (identity element for composition)
- [ ] Marked as `internal object`

### CompositeNavigationConfig
- [ ] Correctly combines two configs with priority ordering
- [ ] Secondary (later) config takes priority
- [ ] All composite registries implemented
- [ ] Chained composition works (`a + b + c`)
- [ ] Marked as `internal class`

### Testing
- [ ] Unit test: Empty config returns appropriate defaults
- [ ] Unit test: Composition priority (secondary over primary)
- [ ] Unit test: Chained composition (`a + b + c` = `(a + b) + c`)
- [ ] Unit test: Empty as identity element (`config + Empty == config`)
- [ ] Unit test: `buildNavNode()` delegation to primary/secondary

### Code Quality
- [ ] No compiler warnings
- [ ] Follows project code style
- [ ] Imports organized properly
- [ ] Package structure matches existing patterns

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| Interface design & review | 0.5 days |
| `NavigationConfig.kt` implementation | 0.5 days |
| `EmptyNavigationConfig.kt` implementation | 0.25 days |
| `CompositeNavigationConfig.kt` implementation | 0.75 days |
| Unit tests | 0.5 days |
| Documentation & code review | 0.5 days |
| **Total** | **2-3 days** |

---

## Implementation Notes

### Design Decisions

1. **Interface vs Abstract Class**
   - Using `interface` to allow multiple inheritance (generated configs may implement other interfaces)
   - Composition handled via explicit wrapper classes

2. **Composition Priority**
   - Later configs (via `+`) have higher priority
   - Matches intuitive "override" semantics: `base + overrides`

3. **DeepLinkHandler Type**
   - Using existing `DeepLinkHandler` interface (or equivalent)
   - May need to define this if not already present

4. **Visibility**
   - `NavigationConfig` is public (API)
   - `EmptyNavigationConfig` is internal (implementation detail)
   - `CompositeNavigationConfig` is internal (implementation detail)

### Potential Issues

1. **ScreenRegistry Composition**
   - Current `ScreenRegistry.Content()` doesn't have a "not found" return
   - May need to add a `hasScreen(destination)` method or handle differently
   - Consider throwing on "not found" vs falling through

2. **NavRenderScope Import**
   - Verify the exact type/location of `NavRenderScope`
   - May need adjustment based on existing API

3. **DeepLinkHandler Interface**
   - Verify this interface exists or needs to be created
   - The plan references `GeneratedDeepLinkHandler` - check actual interface

### Testing Strategy

```kotlin
class NavigationConfigTest {
    
    @Test
    fun `Empty config returns null for buildNavNode`() {
        val result = NavigationConfig.Empty.buildNavNode(TestDestination::class)
        assertNull(result)
    }
    
    @Test
    fun `Composition prioritizes secondary config`() {
        val primary = createConfig(scopeKey = "primary")
        val secondary = createConfig(scopeKey = "secondary")
        val composite = primary + secondary
        
        val scope = composite.scopeRegistry.getScopeKey(TestDestination())
        assertEquals("secondary", scope)
    }
    
    @Test
    fun `Empty is identity element for composition`() {
        val config = createConfig()
        val result = config + NavigationConfig.Empty
        
        // Should behave identically to original config
        assertEquals(config.scopeRegistry.getScopeKey(TestDestination()), 
                     result.scopeRegistry.getScopeKey(TestDestination()))
    }
}
```

---

## Related Files

- [Phase 1 Summary](./SUMMARY.md)
- [Task 1.2 - DSL Builder Infrastructure](./TASK-1.2-dsl-builder-infrastructure.md)
- [Task 1.3 - DslNavigationConfig Implementation](./TASK-1.3-dsl-navigation-config-impl.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)
