# Task 1.3: Create DslNavigationConfig Implementation

> **Task Status**: ⬜ Not Started  
> **Estimated Effort**: 3-4 days  
> **Dependencies**: Task 1.1 (NavigationConfig Interface), Task 1.2 (DSL Builders)  
> **Blocks**: Phase 2 (Convenience Composables)

---

## Objective

Implement `DslNavigationConfig`, the concrete class that converts DSL builder configurations into runtime-usable navigation registries. This class bridges the gap between the declarative DSL and the existing registry interfaces used by `NavigationHost`.

**Key Responsibilities**:
1. Convert DSL screen registrations to `ScreenRegistry`
2. Convert DSL container definitions to `ContainerRegistry`
3. Convert DSL scope definitions to `ScopeRegistry`
4. Convert DSL transition registrations to `TransitionRegistry`
5. Convert DSL wrapper registrations to `WrapperRegistry`
6. Implement `buildNavNode()` to create NavNode trees from container definitions

---

## Files to Create

### Main Implementation Files

**Base Path**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/`

| File | Description |
|------|-------------|
| `DslNavigationConfig.kt` | Main implementation class |

**DSL Package**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/dsl/`

| File | Description |
|------|-------------|
| `DslScreenRegistry.kt` | DSL-based screen registry implementation |
| `DslContainerRegistry.kt` | DSL-based container registry implementation |
| `DslScopeRegistry.kt` | DSL-based scope registry implementation |
| `DslTransitionRegistry.kt` | DSL-based transition registry implementation |
| `DslWrapperRegistry.kt` | DSL-based wrapper registry implementation |
| `NavNodeBuilder.kt` | Helper for building NavNode trees |

---

## Implementation Details

### DslNavigationConfig.kt

```kotlin
package com.jermey.quo.vadis.core.navigation

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.compose.registry.*
import com.jermey.quo.vadis.core.navigation.compose.wrapper.*
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.core.*
import com.jermey.quo.vadis.core.navigation.dsl.*
import kotlin.reflect.KClass

/**
 * NavigationConfig implementation that converts DSL configurations to runtime registries.
 * 
 * This class is the concrete implementation of [NavigationConfig] created by
 * [NavigationConfigBuilder.build()]. It transforms the declarative DSL structures
 * into functional registry implementations.
 * 
 * ## Internal Structure
 * 
 * The class maintains maps of configurations and lazily creates registry implementations:
 * - Screen configurations → [DslScreenRegistry]
 * - Container configurations → [DslContainerRegistry]
 * - Scope configurations → [DslScopeRegistry]
 * - Transition configurations → [DslTransitionRegistry]
 * - Wrapper configurations → [DslWrapperRegistry]
 * 
 * ## Thread Safety
 * 
 * This class is designed for single-threaded construction during app initialization.
 * The resulting registries are immutable and safe for concurrent access.
 * 
 * @property screens Map of destination class to screen entry
 * @property containers Map of destination class to container builder
 * @property scopes Map of scope key to destination classes
 * @property transitions Map of destination class to transition
 * @property tabWrappers Map of wrapper key to tab wrapper composable
 * @property paneWrappers Map of wrapper key to pane wrapper composable
 */
internal class DslNavigationConfig(
    private val screens: Map<KClass<out Destination>, ScreenEntry>,
    private val containers: Map<KClass<out Destination>, ContainerBuilder>,
    private val scopes: Map<String, Set<KClass<out Destination>>>,
    private val transitions: Map<KClass<out Destination>, NavTransition>,
    private val tabWrappers: Map<String, @Composable TabWrapperScope.() -> Unit>,
    private val paneWrappers: Map<String, @Composable PaneWrapperScope.() -> Unit>
) : NavigationConfig {
    
    // ========================================
    // Lazily Initialized Registries
    // ========================================
    
    override val screenRegistry: ScreenRegistry by lazy {
        DslScreenRegistry(screens)
    }
    
    override val wrapperRegistry: WrapperRegistry by lazy {
        DslWrapperRegistry(tabWrappers, paneWrappers)
    }
    
    override val scopeRegistry: ScopeRegistry by lazy {
        // Combine explicit scopes with auto-inferred scopes from containers
        val combinedScopes = buildCombinedScopes()
        DslScopeRegistry(combinedScopes)
    }
    
    override val transitionRegistry: TransitionRegistry by lazy {
        DslTransitionRegistry(transitions)
    }
    
    override val containerRegistry: ContainerRegistry by lazy {
        DslContainerRegistry(containers, ::buildNavNode)
    }
    
    override val deepLinkHandler: DeepLinkHandler by lazy {
        // Default implementation - can be extended later
        EmptyDeepLinkHandler
    }
    
    // ========================================
    // buildNavNode Implementation
    // ========================================
    
    /**
     * Builds a NavNode tree for the given destination class.
     * 
     * This method interprets the container DSL and constructs the appropriate
     * NavNode hierarchy (StackNode, TabNode, or PaneNode).
     * 
     * ## Algorithm
     * 1. Look up container builder for destinationClass
     * 2. Based on container type, invoke appropriate NavNode builder
     * 3. Recursively resolve nested containers (containerTab references)
     * 4. Return fully constructed NavNode tree
     * 
     * @param destinationClass The container destination to build
     * @param key Optional custom key for the root node
     * @param parentKey Optional parent key for nested contexts
     * @return The constructed NavNode, or null if not registered
     */
    override fun buildNavNode(
        destinationClass: KClass<out Destination>,
        key: String?,
        parentKey: String?
    ): NavNode? {
        val containerBuilder = containers[destinationClass] ?: return null
        val effectiveKey = key ?: containerBuilder.scopeKey
        
        return when (containerBuilder) {
            is ContainerBuilder.Stack -> buildStackNode(containerBuilder, effectiveKey, parentKey)
            is ContainerBuilder.Tabs -> buildTabNode(containerBuilder, effectiveKey, parentKey)
            is ContainerBuilder.Panes -> buildPaneNode(containerBuilder, effectiveKey, parentKey)
        }
    }
    
    // ========================================
    // Private Node Builders
    // ========================================
    
    private fun buildStackNode(
        builder: ContainerBuilder.Stack,
        key: String,
        parentKey: String?
    ): StackNode {
        val stackBuilder = StackBuilder().apply(builder.builder)
        val builtScreens = stackBuilder.build()
        
        val children = builtScreens.mapIndexed { index, entry ->
            val screenKey = "$key/${entry.key}"
            val destination = entry.destination 
                ?: createDestinationInstance(entry.destinationClass!!)
            
            ScreenNode(
                key = screenKey,
                parentKey = key,
                destination = destination
            )
        }
        
        return StackNode(
            key = key,
            parentKey = parentKey,
            children = children
        )
    }
    
    private fun buildTabNode(
        builder: ContainerBuilder.Tabs,
        key: String,
        parentKey: String?
    ): TabNode {
        val tabsBuilder = TabsBuilder().apply(builder.builder)
        val builtTabs = tabsBuilder.build()
        
        val stacks = builtTabs.tabs.mapIndexed { index, tabEntry ->
            buildStackFromTabEntry(tabEntry, key, index)
        }
        
        return TabNode(
            key = key,
            parentKey = parentKey,
            stacks = stacks,
            activeStackIndex = builtTabs.initialTab,
            wrapperKey = builder.wrapperKey
        )
    }
    
    private fun buildStackFromTabEntry(
        tabEntry: TabEntry,
        parentKey: String,
        tabIndex: Int
    ): StackNode {
        val tabKey = "$parentKey/tab$tabIndex"
        
        return when (tabEntry) {
            is TabEntry.FlatScreen -> {
                val screenKey = "$tabKey/root"
                StackNode(
                    key = tabKey,
                    parentKey = parentKey,
                    children = listOf(
                        ScreenNode(
                            key = screenKey,
                            parentKey = tabKey,
                            destination = tabEntry.destination
                        )
                    )
                )
            }
            
            is TabEntry.NestedStack -> {
                val stackBuilder = StackBuilder().apply(tabEntry.stackBuilder)
                val nestedScreens = stackBuilder.build()
                
                // Add root screen from destination
                val rootScreen = ScreenNode(
                    key = "$tabKey/root",
                    parentKey = tabKey,
                    destination = tabEntry.destination
                )
                
                // Add any additional nested screens
                val additionalScreens = nestedScreens.mapIndexed { index, entry ->
                    val destination = entry.destination 
                        ?: createDestinationInstance(entry.destinationClass!!)
                    ScreenNode(
                        key = "$tabKey/${entry.key}",
                        parentKey = tabKey,
                        destination = destination
                    )
                }
                
                StackNode(
                    key = tabKey,
                    parentKey = parentKey,
                    children = listOf(rootScreen) + additionalScreens
                )
            }
            
            is TabEntry.ContainerReference -> {
                // Recursively build referenced container
                val referencedNode = buildNavNode(
                    destinationClass = tabEntry.containerClass,
                    key = tabKey,
                    parentKey = parentKey
                )
                
                // If referenced container is a stack, use it directly
                // Otherwise, wrap in a stack
                when (referencedNode) {
                    is StackNode -> referencedNode.copy(key = tabKey, parentKey = parentKey)
                    else -> {
                        // Wrap non-stack container in a stack
                        StackNode(
                            key = tabKey,
                            parentKey = parentKey,
                            children = listOf(
                                // Create wrapper screen node
                                ScreenNode(
                                    key = "$tabKey/container",
                                    parentKey = tabKey,
                                    destination = createDestinationInstance(tabEntry.containerClass)
                                )
                            )
                        )
                    }
                }
            }
        }
    }
    
    private fun buildPaneNode(
        builder: ContainerBuilder.Panes,
        key: String,
        parentKey: String?
    ): PaneNode {
        val panesBuilder = PanesBuilder().apply(builder.builder)
        val builtPanes = panesBuilder.build()
        
        val paneStacks = builtPanes.panes.mapValues { (role, paneEntry) ->
            val paneBuilder = PaneContentBuilder().apply(paneEntry.builder)
            val builtContent = paneBuilder.build()
            
            val paneKey = "$key/${role.name.lowercase()}"
            
            StackNode(
                key = paneKey,
                parentKey = key,
                children = if (builtContent.rootDestination != null) {
                    listOf(
                        ScreenNode(
                            key = "$paneKey/root",
                            parentKey = paneKey,
                            destination = builtContent.rootDestination
                        )
                    )
                } else {
                    emptyList()
                }
            )
        }
        
        return PaneNode(
            key = key,
            parentKey = parentKey,
            panes = paneStacks,
            activePane = builtPanes.initialPane,
            backBehavior = builtPanes.backBehavior,
            wrapperKey = builder.wrapperKey
        )
    }
    
    // ========================================
    // Helper Methods
    // ========================================
    
    /**
     * Builds combined scope map from explicit definitions and container inference.
     */
    private fun buildCombinedScopes(): Map<String, Set<KClass<out Destination>>> {
        val combined = scopes.toMutableMap()
        
        // Auto-infer scopes from container definitions
        containers.forEach { (destClass, builder) ->
            val scopeKey = builder.scopeKey
            combined.getOrPut(scopeKey) { mutableSetOf() }
            
            // Add container destination to its scope
            val scopeSet = combined[scopeKey]!!.toMutableSet()
            scopeSet.add(destClass)
            
            // Infer from nested content
            when (builder) {
                is ContainerBuilder.Tabs -> {
                    val tabsBuilder = TabsBuilder().apply(builder.builder)
                    tabsBuilder.tabs.forEach { tabEntry ->
                        when (tabEntry) {
                            is TabEntry.FlatScreen -> 
                                scopeSet.add(tabEntry.destination::class)
                            is TabEntry.NestedStack -> 
                                scopeSet.add(tabEntry.destination::class)
                            is TabEntry.ContainerReference -> 
                                scopeSet.add(tabEntry.containerClass)
                        }
                    }
                }
                is ContainerBuilder.Stack -> {
                    val stackBuilder = StackBuilder().apply(builder.builder)
                    stackBuilder.screens.forEach { entry ->
                        val destClass = entry.destinationClass 
                            ?: entry.destination!!::class
                        scopeSet.add(destClass)
                    }
                }
                is ContainerBuilder.Panes -> {
                    val panesBuilder = PanesBuilder().apply(builder.builder)
                    panesBuilder.panes.values.forEach { paneEntry ->
                        val paneBuilder = PaneContentBuilder().apply(paneEntry.builder)
                        paneBuilder.rootDestination?.let {
                            scopeSet.add(it::class)
                        }
                    }
                }
            }
            
            combined[scopeKey] = scopeSet
        }
        
        return combined
    }
    
    /**
     * Creates a destination instance from its class.
     * 
     * Note: This requires destinations to have a no-arg constructor
     * or use object declarations. For more complex cases, the DSL
     * should provide the instance directly.
     */
    @Suppress("UNCHECKED_CAST")
    private fun createDestinationInstance(kClass: KClass<out Destination>): Destination {
        // Try to get object instance first
        return kClass.objectInstance 
            ?: try {
                // Fall back to no-arg constructor
                kClass.java.getDeclaredConstructor().newInstance() as Destination
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Cannot create instance of $kClass. " +
                    "Destinations must be objects or have a no-arg constructor, " +
                    "or provide an instance directly in the DSL.",
                    e
                )
            }
    }
    
    // ========================================
    // Composition Support
    // ========================================
    
    override fun plus(other: NavigationConfig): NavigationConfig {
        return CompositeNavigationConfig(this, other)
    }
}

/**
 * Empty deep link handler - no-op implementation.
 */
private object EmptyDeepLinkHandler : DeepLinkHandler {
    override fun handle(deepLink: DeepLink, navigator: Navigator): Boolean = false
    override fun canHandle(deepLink: DeepLink): Boolean = false
}
```

### DslScreenRegistry.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.compose.registry.ScreenRegistry
import com.jermey.quo.vadis.core.navigation.compose.render.NavRenderScope
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import kotlin.reflect.KClass

/**
 * ScreenRegistry implementation backed by DSL screen registrations.
 * 
 * Looks up screen content by destination class and invokes the
 * registered composable.
 */
internal class DslScreenRegistry(
    private val screens: Map<KClass<out Destination>, ScreenEntry>
) : ScreenRegistry {
    
    @Composable
    override fun Content(
        destination: Destination,
        navigator: Navigator,
        scope: NavRenderScope
    ) {
        val entry = screens[destination::class]
        
        if (entry != null) {
            val screenScope = object : ScreenScope {
                override val navigator: Navigator = navigator
            }
            entry.content(destination, screenScope)
        } else {
            // Handle unregistered destination
            // Could throw, show error UI, or log warning
            UnregisteredDestinationContent(destination)
        }
    }
    
    /**
     * Checks if a screen is registered for the given destination.
     */
    fun hasScreen(destination: Destination): Boolean {
        return screens.containsKey(destination::class)
    }
}

/**
 * Fallback content for unregistered destinations.
 */
@Composable
private fun UnregisteredDestinationContent(destination: Destination) {
    // In debug builds, show error
    // In release builds, could be empty or show generic error
    // This implementation can be adjusted based on requirements
}
```

### DslContainerRegistry.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerInfo
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavNode
import kotlin.reflect.KClass

/**
 * ContainerRegistry implementation backed by DSL container definitions.
 */
internal class DslContainerRegistry(
    private val containers: Map<KClass<out Destination>, ContainerBuilder>,
    private val navNodeBuilder: (KClass<out Destination>, String?, String?) -> NavNode?
) : ContainerRegistry {
    
    override fun getContainerInfo(destination: Destination): ContainerInfo? {
        val builder = containers[destination::class] ?: return null
        
        return when (builder) {
            is ContainerBuilder.Stack -> ContainerInfo.StackContainer(
                buildNavNode = { key, parentKey -> 
                    navNodeBuilder(destination::class, key, parentKey) 
                },
                scopeKey = builder.scopeKey
            )
            
            is ContainerBuilder.Tabs -> {
                val tabsBuilder = TabsBuilder().apply(builder.builder)
                ContainerInfo.TabContainer(
                    buildNavNode = { key, parentKey -> 
                        navNodeBuilder(destination::class, key, parentKey) 
                    },
                    initialTabIndex = tabsBuilder.initialTab,
                    scopeKey = builder.scopeKey,
                    wrapperKey = builder.wrapperKey
                )
            }
            
            is ContainerBuilder.Panes -> {
                val panesBuilder = PanesBuilder().apply(builder.builder)
                ContainerInfo.PaneContainer(
                    buildNavNode = { key, parentKey -> 
                        navNodeBuilder(destination::class, key, parentKey) 
                    },
                    initialPane = panesBuilder.initialPane,
                    scopeKey = builder.scopeKey,
                    wrapperKey = builder.wrapperKey,
                    backBehavior = panesBuilder.backBehavior
                )
            }
        }
    }
    
    /**
     * Returns all registered container destination classes.
     */
    fun getRegisteredContainers(): Set<KClass<out Destination>> {
        return containers.keys
    }
}
```

### DslScopeRegistry.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.core.Destination
import kotlin.reflect.KClass

/**
 * ScopeRegistry implementation backed by DSL scope definitions.
 */
internal class DslScopeRegistry(
    private val scopes: Map<String, Set<KClass<out Destination>>>
) : ScopeRegistry {
    
    // Reverse lookup map: destination class -> scope key
    private val destinationToScope: Map<KClass<out Destination>, String> by lazy {
        scopes.flatMap { (scopeKey, destinations) ->
            destinations.map { it to scopeKey }
        }.toMap()
    }
    
    override fun getScopeKey(destination: Destination): String? {
        return destinationToScope[destination::class]
    }
    
    override fun getDestinationsInScope(scopeKey: String): Set<KClass<out Destination>> {
        return scopes[scopeKey] ?: emptySet()
    }
    
    /**
     * Returns all registered scope keys.
     */
    fun getAllScopeKeys(): Set<String> {
        return scopes.keys
    }
}
```

### DslTransitionRegistry.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import com.jermey.quo.vadis.core.navigation.compose.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.core.Destination
import kotlin.reflect.KClass

/**
 * TransitionRegistry implementation backed by DSL transition registrations.
 */
internal class DslTransitionRegistry(
    private val transitions: Map<KClass<out Destination>, NavTransition>
) : TransitionRegistry {
    
    override fun getTransition(destination: Destination): NavTransition? {
        return transitions[destination::class]
    }
    
    /**
     * Checks if a custom transition is registered for the destination.
     */
    fun hasTransition(destination: Destination): Boolean {
        return transitions.containsKey(destination::class)
    }
}
```

### DslWrapperRegistry.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.dsl

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.compose.registry.WrapperRegistry
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneWrapperScope

/**
 * WrapperRegistry implementation backed by DSL wrapper registrations.
 */
internal class DslWrapperRegistry(
    private val tabWrappers: Map<String, @Composable TabWrapperScope.() -> Unit>,
    private val paneWrappers: Map<String, @Composable PaneWrapperScope.() -> Unit>
) : WrapperRegistry {
    
    @Composable
    override fun TabWrapper(wrapperKey: String, scope: TabWrapperScope) {
        val wrapper = tabWrappers[wrapperKey]
        if (wrapper != null) {
            wrapper(scope)
        } else {
            // Default: just render content without wrapper
            scope.content()
        }
    }
    
    @Composable
    override fun PaneWrapper(wrapperKey: String, scope: PaneWrapperScope) {
        val wrapper = paneWrappers[wrapperKey]
        if (wrapper != null) {
            wrapper(scope)
        } else {
            // Default: just render content without wrapper
            scope.content()
        }
    }
    
    /**
     * Checks if a tab wrapper is registered for the given key.
     */
    fun hasTabWrapper(key: String): Boolean = tabWrappers.containsKey(key)
    
    /**
     * Checks if a pane wrapper is registered for the given key.
     */
    fun hasPaneWrapper(key: String): Boolean = paneWrappers.containsKey(key)
}
```

---

## How DSL Configuration Converts to Runtime Registries

### Data Flow

```
┌─────────────────────────┐
│ navigationConfig { }    │  DSL Code (compile-time)
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ NavigationConfigBuilder │  Builder State (configuration-time)
│ - screens: Map          │
│ - containers: Map       │
│ - scopes: Map           │
│ - transitions: Map      │
│ - wrappers: Map         │
└───────────┬─────────────┘
            │ .build()
            ▼
┌─────────────────────────┐
│ DslNavigationConfig     │  Runtime Configuration
│ ├─ DslScreenRegistry    │
│ ├─ DslContainerRegistry │
│ ├─ DslScopeRegistry     │
│ ├─ DslTransitionRegistry│
│ └─ DslWrapperRegistry   │
└───────────┬─────────────┘
            │ used by
            ▼
┌─────────────────────────┐
│ NavigationHost          │  UI Rendering
│ + TreeNavigator         │
└─────────────────────────┘
```

### Conversion Process

1. **Screens**: `Map<KClass, ScreenEntry>` → `DslScreenRegistry`
   - Direct mapping, looked up by `destination::class`

2. **Containers**: `Map<KClass, ContainerBuilder>` → `DslContainerRegistry`
   - ContainerBuilder sealed class converted to `ContainerInfo`
   - NavNode builder function reference captured for lazy construction

3. **Scopes**: `Map<String, Set<KClass>>` → `DslScopeRegistry`
   - Explicit scopes merged with auto-inferred scopes
   - Reverse lookup map built for `getScopeKey()`

4. **Transitions**: `Map<KClass, NavTransition>` → `DslTransitionRegistry`
   - Direct mapping

5. **Wrappers**: `Map<String, Composable>` → `DslWrapperRegistry`
   - Direct mapping with default fallback

---

## buildNavNode() Implementation Approach

### Algorithm

```
buildNavNode(destinationClass, key, parentKey):
    1. containerBuilder = containers[destinationClass]
    2. if containerBuilder == null: return null
    3. effectiveKey = key ?: containerBuilder.scopeKey
    
    4. match containerBuilder:
        Stack:
            a. Execute stackBuilder DSL to get screen entries
            b. Create ScreenNode for each entry
            c. Return StackNode with children
            
        Tabs:
            a. Execute tabsBuilder DSL to get tab entries
            b. For each tab:
               - FlatScreen: Create StackNode with single ScreenNode
               - NestedStack: Create StackNode with root + nested screens
               - ContainerReference: Recursively call buildNavNode
            c. Return TabNode with stacks
            
        Panes:
            a. Execute panesBuilder DSL to get pane entries
            b. For each pane role:
               - Create StackNode with root destination
            c. Return PaneNode with pane stacks
```

### Key Construction Pattern

```
Container Key: "MainTabs"
├── Tab 0 Key: "MainTabs/tab0"
│   └── Screen Key: "MainTabs/tab0/root"
├── Tab 1 Key: "MainTabs/tab1"
│   ├── Screen Key: "MainTabs/tab1/root"
│   └── Screen Key: "MainTabs/tab1/detail"
└── Tab 2 Key: "MainTabs/tab2"
    └── Screen Key: "MainTabs/tab2/root"
```

---

## Dependencies on Other Tasks

| Task | Dependency Type | Description |
|------|-----------------|-------------|
| Task 1.1 | **Hard** | Implements `NavigationConfig` interface |
| Task 1.2 | **Hard** | Uses all DSL builder classes |

**This Task Blocks**:
- Phase 2 Task 2.1 (rememberQuoVadisNavigator) - uses `buildNavNode()`
- Phase 2 Task 2.3 (NavigationHost overload) - passes config to existing APIs

---

## Acceptance Criteria Checklist

### DslNavigationConfig
- [ ] Implements all `NavigationConfig` properties
- [ ] Lazily initializes registry implementations
- [ ] `buildNavNode()` handles Stack containers
- [ ] `buildNavNode()` handles Tabs containers with all tab types
- [ ] `buildNavNode()` handles Panes containers
- [ ] `buildNavNode()` handles nested container references
- [ ] `plus()` returns CompositeNavigationConfig
- [ ] Proper key generation for nested nodes
- [ ] Auto-infers scopes from container definitions

### DslScreenRegistry
- [ ] Looks up screens by destination class
- [ ] Invokes registered composable content
- [ ] Handles unregistered destinations gracefully
- [ ] `hasScreen()` helper method

### DslContainerRegistry
- [ ] Returns correct ContainerInfo for each container type
- [ ] Includes buildNavNode function reference
- [ ] Returns null for unregistered containers

### DslScopeRegistry
- [ ] `getScopeKey()` returns correct scope for destination
- [ ] `getDestinationsInScope()` returns all destinations in scope
- [ ] Handles destinations in multiple scopes (last wins or error?)

### DslTransitionRegistry
- [ ] Returns registered transition for destination
- [ ] Returns null for unregistered destinations

### DslWrapperRegistry
- [ ] Invokes tab wrapper for registered key
- [ ] Invokes pane wrapper for registered key
- [ ] Falls back to default content() for unregistered keys

### Testing
- [ ] Unit test: DslNavigationConfig creation from builder
- [ ] Unit test: Screen lookup
- [ ] Unit test: buildNavNode for Stack
- [ ] Unit test: buildNavNode for Tabs (flat, nested, container)
- [ ] Unit test: buildNavNode for Panes
- [ ] Unit test: Scope auto-inference
- [ ] Unit test: Transition lookup
- [ ] Unit test: Wrapper invocation
- [ ] Integration test: Full DSL → NavigationHost flow

### Code Quality
- [ ] All classes marked internal where appropriate
- [ ] No compiler warnings
- [ ] KDoc on all public/internal APIs
- [ ] Consistent error handling

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| DslNavigationConfig structure | 0.5 days |
| buildNavNode() for Stack | 0.25 days |
| buildNavNode() for Tabs | 0.5 days |
| buildNavNode() for Panes | 0.25 days |
| DslScreenRegistry | 0.25 days |
| DslContainerRegistry | 0.25 days |
| DslScopeRegistry | 0.25 days |
| DslTransitionRegistry | 0.25 days |
| DslWrapperRegistry | 0.25 days |
| Unit tests | 0.75 days |
| Integration tests | 0.5 days |
| Documentation & review | 0.5 days |
| **Total** | **3-4 days** |

---

## Integration Test Requirements

### End-to-End Test Scenario

```kotlin
@Test
fun `DSL config integrates with NavigationHost`() {
    val config = navigationConfig {
        screen<HomeDestination> { Text("Home") }
        screen<DetailDestination> { Text("Detail: ${it.id}") }
        
        tabs<MainTabs>(scopeKey = "main") {
            tab(MainTabs.Home)
            tab(MainTabs.Profile)
        }
    }
    
    composeTestRule.setContent {
        val navigator = remember {
            val initialState = config.buildNavNode(MainTabs::class)!!
            TreeNavigator(
                initialState = initialState,
                scopeRegistry = config.scopeRegistry,
                containerRegistry = config.containerRegistry,
                deepLinkHandler = config.deepLinkHandler
            )
        }
        
        NavigationHost(
            navigator = navigator,
            screenRegistry = config.screenRegistry,
            wrapperRegistry = config.wrapperRegistry,
            transitionRegistry = config.transitionRegistry,
            scopeRegistry = config.scopeRegistry
        )
    }
    
    // Verify home screen is displayed
    composeTestRule.onNodeWithText("Home").assertExists()
}
```

### Multi-Module Composition Test

```kotlin
@Test
fun `Multiple configs compose correctly`() {
    val baseConfig = navigationConfig {
        screen<HomeDestination> { Text("Home") }
    }
    
    val featureConfig = navigationConfig {
        screen<FeatureDestination> { Text("Feature") }
    }
    
    val combined = baseConfig + featureConfig
    
    // Both screens should be accessible
    assertTrue(combined.screenRegistry.hasScreen(HomeDestination()))
    assertTrue(combined.screenRegistry.hasScreen(FeatureDestination()))
}
```

---

## Implementation Notes

### Design Decisions

1. **Lazy Registry Initialization**
   - Registries created lazily to minimize startup cost
   - Thread-safe via Kotlin's `by lazy`

2. **Scope Auto-Inference**
   - Containers automatically add their destinations to scopes
   - Explicit `scope()` calls can override or extend

3. **Destination Instance Creation**
   - Support both object instances and classes
   - Fallback to reflection for no-arg constructors
   - Clear error message when instance cannot be created

4. **Key Generation**
   - Hierarchical keys: `container/tab/screen`
   - Enables predictable navigation state serialization

### Potential Issues

1. **Reflection Limitations on iOS/JS**
   - `getDeclaredConstructor()` may not work on all platforms
   - Solution: Prefer object instances or explicit destination in DSL

2. **ContainerInfo API Compatibility**
   - Verify existing ContainerInfo sealed class matches expected structure
   - May need to add `wrapperKey` property if not present

3. **Circular Container References**
   - `containerTab` could reference itself
   - Add cycle detection or document limitation

---

## Related Files

- [Phase 1 Summary](./SUMMARY.md)
- [Task 1.1 - NavigationConfig Interface](./TASK-1.1-navigation-config-interface.md)
- [Task 1.2 - DSL Builder Infrastructure](./TASK-1.2-dsl-builder-infrastructure.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)
