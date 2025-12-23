# Lifecycle & SavedState Refactoring Plan

## Executive Summary

This document outlines a comprehensive refactoring of Quo Vadis navigation library's lifecycle management and saved state handling, inspired by Tiamat's architecture but tailored for FlowMVI-first approach with full AndroidX compatibility.

**Goals:**
1. Replace custom `NavigationLifecycle` with proper `androidx.lifecycle.Lifecycle` integration
2. Add `SavedStateRegistryOwner` and `ViewModelStoreOwner` per screen for full AndroidX ecosystem compatibility
3. Integrate FlowMVI's savedstate module for process death recovery of MVI container state
4. Per-screen `rememberSaveable` scope for Compose state preservation

**Breaking Changes:** Yes - this is a major architectural change. The library is in development stage, so breaking changes are acceptable.

---

## Part 1: Problem Analysis

### Current State Issues

#### 1.1 Custom NavigationLifecycle (Not Standard)

**Current implementation** in [NavigationLifecycle.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavigationLifecycle.kt):

```kotlin
interface NavigationLifecycle {
    fun onEnter()   // Screen becomes active
    fun onExit()    // Screen becomes inactive
    fun onDestroy() // Screen removed from stack
}
```

**Problems:**
- Custom interface not compatible with AndroidX lifecycle-aware components
- No integration with `collectAsStateWithLifecycle()`, `repeatOnLifecycle()`, etc.
- Missing standard lifecycle states (INITIALIZED, CREATED, STARTED, RESUMED)
- Manual registration/unregistration required

#### 1.2 No SavedState Support

**Current state:**
- No `SavedStateRegistryOwner` per screen
- No integration with `rememberSaveable` for Compose state
- No `SavedStateHandle` support for ViewModels
- Process death results in complete state loss

#### 1.3 FlowMVI Integration Deficiencies

**Current** [BaseContainer.kt](quo-vadis-core-flow-mvi/src/commonMain/kotlin/com/jermey/quo/vadis/flowmvi/BaseContainer.kt):

```kotlin
abstract class BaseContainer<S, I, A>(
    protected val navigator: Navigator,
    protected val screenKey: String,
) : Container<S, I, A>, NavigationLifecycle, DestinationScope {
    // No SavedState integration
    // Manual lifecycle registration
}
```

**Problems:**
- No integration with FlowMVI's `savedstate` module
- MVI state lost on process death
- Complex manual lifecycle management
- Koin scope management coupled with container

---

## Part 2: Tiamat Reference Architecture

### 2.1 Key Tiamat Design Decisions

Tiamat's `NavEntry` implements:
```kotlin
class NavEntry<Args : Any> : ViewModelStoreOwner, LifecycleOwner {
    // Lifecycle management
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry
    
    // ViewModel storage
    override val viewModelStore: ViewModelStore = ViewModelStore()
    
    // Retained values (Compose 1.8+ retain API)
    internal val retainedValuesStore = ManagedRetainedValuesStore()
    
    // State persistence
    internal var savedState: SavedState? = null
}
```

### 2.2 Composition Local Providers

Tiamat's [ComposableNavEntry.kt](https://github.com/ComposeGears/Tiamat/blob/main/tiamat/src/commonMain/kotlin/com/composegears/tiamat/compose/ComposableNavEntry.kt) provides:

```kotlin
CompositionLocalProvider(
    LocalLifecycleOwner provides entryLifecycleOwner,
    LocalRetainedValuesStore provides entry.retainedValuesStore,
    LocalSaveableStateRegistry provides entrySaveableStateRegistry,
    LocalViewModelStoreOwner provides entryViewModelStoreOwner,
    LocalSavedStateRegistryOwner provides savedStateRegistryOwner,
) {
    // Screen content
}
```

### 2.3 Lifecycle State Transitions

```
attachToNavController() → INITIALIZED (entry exists but not displayed)
attachToUI()           → RESUMED (entry is being displayed)
detachFromUI()         → STARTED (entry not displayed but in stack)
detachFromNavController() + close() → DESTROYED (entry removed from stack)
```

---

## Part 3: Proposed Architecture

### 3.0 Lifecycle Hierarchy Design

The navigation tree has three levels that require lifecycle management:

```
Navigator (Application Lifecycle)
│
├── TabNode / PaneNode (Container Lifecycle)
│   ├── Wrapper Composable renders container UI
│   ├── Can host shared MVI containers for children
│   └── Has its own LifecycleOwner, ViewModelStoreOwner
│
└── ScreenNode (Screen Lifecycle)
    ├── Renders individual screen content
    └── Has its own LifecycleOwner, ViewModelStoreOwner
```

**Key insight**: `TabNode` and `PaneNode` wrappers are destinations too - they render container UI (tab bars, pane layouts) and can host **shared MVI containers** that are accessible to all child screens.

**Use cases for container-level MVI:**
- Tab-wide state (e.g., unread badge count across all screens in a tab)
- Pane coordination (e.g., master list selection shared with detail pane)
- Cross-screen communication within a container scope

### 3.1 Navigation Node Base Interface

First, define a common interface for lifecycle-aware nodes:

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../core/LifecycleAwareNode.kt

/**
 * Interface for navigation nodes that provide lifecycle and state management.
 * 
 * Both container nodes (TabNode, PaneNode) and screen nodes (ScreenNode)
 * implement this to provide consistent lifecycle behavior.
 */
interface LifecycleAwareNode : NavNode, LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    
    /** Unique stable identifier for state restoration */
    val uuid: String
    
    /** Compose retained values store */
    val retainedValuesStore: ManagedRetainedValuesStore
    
    /** Saved state for rememberSaveable */
    var composeSavedState: Map<String, List<Any?>>?
    
    // Lifecycle state queries
    val isAttachedToNavigator: Boolean
    val isDisplayed: Boolean
    
    // Lifecycle transitions
    fun attachToNavigator()
    fun attachToUI()
    fun detachFromUI()
    fun detachFromNavigator()
    
    // Serialization
    fun saveToBundle(): SavedState
}
```

### 3.2 Enhanced Container Nodes (TabNode, PaneNode)

Container nodes need lifecycle for their wrapper composables:

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../core/TabNode.kt

@Stable
class TabNode(
    override val key: String,
    override val parentKey: String?,
    val stacks: List<StackNode>,
    val activeStackIndex: Int = 0,
    val wrapperKey: String? = null,
    val tabMetadata: List<GeneratedTabMetadata> = emptyList(),
    val scopeKey: String? = null
) : LifecycleAwareNode {
    
    // Lifecycle infrastructure (same as ScreenNode)
    @OptIn(ExperimentalUuidApi::class)
    override val uuid: String = Uuid.random().toHexString()
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    
    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore
    
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry 
        get() = savedStateRegistryController.savedStateRegistry
    
    override val retainedValuesStore = ManagedRetainedValuesStore()
    override var composeSavedState: Map<String, List<Any?>>? = null
    
    // State tracking
    override var isAttachedToNavigator: Boolean = false
        private set
    override var isDisplayed: Boolean = false
        private set
    
    // Lifecycle transitions (same implementation as ScreenNode)
    override fun attachToNavigator() { /* ... */ }
    override fun attachToUI() { /* ... */ }
    override fun detachFromUI() { /* ... */ }
    override fun detachFromNavigator() { /* ... */ }
    
    // Tab-specific properties
    val activeStack: StackNode get() = stacks[activeStackIndex]
    val tabCount: Int get() = stacks.size
    
    // Serialization includes child stacks
    override fun saveToBundle(): SavedState {
        return SavedState(
            KEY_KEY to key,
            KEY_PARENT_KEY to parentKey,
            KEY_UUID to uuid,
            KEY_STACKS to stacks.map { it.saveToBundle() },
            KEY_ACTIVE_INDEX to activeStackIndex,
            KEY_WRAPPER_KEY to wrapperKey,
            KEY_TAB_METADATA to tabMetadata.map { it.saveToBundle() },
            KEY_SCOPE_KEY to scopeKey,
            KEY_COMPOSE_SAVED_STATE to composeSavedState,
            KEY_SAVED_STATE_REGISTRY to savedStateRegistryController.performSave(),
        )
    }
}
```

Similarly for `PaneNode`:

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../core/PaneNode.kt

@Stable
class PaneNode(
    override val key: String,
    override val parentKey: String?,
    val paneConfigurations: Map<PaneRole, PaneConfiguration>,
    val activePaneRole: PaneRole = PaneRole.Primary,
    val backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
    val scopeKey: String? = null
) : LifecycleAwareNode {
    
    // Same lifecycle infrastructure as TabNode
    // ...
}
```

### 3.3 Enhanced ScreenNode

Transform `ScreenNode` from a simple data class to a lifecycle-aware component:

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../core/ScreenNode.kt

@Stable
class ScreenNode(
    override val key: String,
    override val parentKey: String?,
    val destination: NavDestination,
) : NavNode, LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    
    // Unique identifier (survives config changes)
    @OptIn(ExperimentalUuidApi::class)
    internal val uuid: String = Uuid.random().toHexString()
    
    // Lifecycle
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    
    // ViewModel Store
    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore
    
    // SavedState Registry
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry 
        get() = savedStateRegistryController.savedStateRegistry
    
    // Compose Retained Values (Compose 1.8+)
    internal val retainedValuesStore = ManagedRetainedValuesStore()
    
    // Saved state for Compose rememberSaveable
    internal var composeSavedState: Map<String, List<Any?>>? = null
    
    // State transitions
    internal var isAttachedToNavigator: Boolean = false
        private set
    internal var isDisplayed: Boolean = false
        private set
    
    // --- Lifecycle Management ---
    
    internal fun attachToNavigator() {
        check(!isAttachedToNavigator) { "ScreenNode already attached to navigator" }
        isAttachedToNavigator = true
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
    
    internal fun attachToUI() {
        check(isAttachedToNavigator) { "Must attach to navigator first" }
        isDisplayed = true
        retainedValuesStore.enableRetainingExitedValues()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
    
    internal fun detachFromUI() {
        isDisplayed = false
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        if (!isAttachedToNavigator) close()
    }
    
    internal fun detachFromNavigator() {
        isAttachedToNavigator = false
        if (!isDisplayed) close()
    }
    
    private fun close() {
        retainedValuesStore.disableRetainingExitedValues()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
    
    // --- Serialization for State Restoration ---
    
    internal fun saveToBundle(): SavedState {
        return SavedState(
            KEY_KEY to key,
            KEY_PARENT_KEY to parentKey,
            KEY_DESTINATION to destination.saveToBundle(),
            KEY_UUID to uuid,
            KEY_COMPOSE_SAVED_STATE to composeSavedState,
            KEY_SAVED_STATE_REGISTRY to savedStateRegistryController.performSave(),
        )
    }
    
    companion object {
        private const val KEY_KEY = "key"
        private const val KEY_PARENT_KEY = "parentKey"
        private const val KEY_DESTINATION = "destination"
        private const val KEY_UUID = "uuid"
        private const val KEY_COMPOSE_SAVED_STATE = "composeSavedState"
        private const val KEY_SAVED_STATE_REGISTRY = "savedStateRegistry"
        
        internal fun restoreFromBundle(savedState: SavedState): ScreenNode {
            return ScreenNode(
                key = savedState[KEY_KEY] as String,
                parentKey = savedState[KEY_PARENT_KEY] as String?,
                destination = NavDestination.restoreFromBundle(savedState[KEY_DESTINATION] as SavedState)
            ).apply {
                uuid = savedState[KEY_UUID] as String
                composeSavedState = savedState[KEY_COMPOSE_SAVED_STATE] as? Map<String, List<Any?>>
                savedStateRegistryController.performRestore(
                    savedState[KEY_SAVED_STATE_REGISTRY] as? SavedState
                )
            }
        }
    }
}
```

### 3.4 Container Wrapper Rendering

Update container rendering to provide lifecycle context:

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../compose/render/ContainerRenderer.kt

@Composable
internal fun TabContainerContent(
    node: TabNode,
    scope: NavRenderScope,
) {
    // Create lifecycle owner for the container itself
    val parentLifecycle = LocalLifecycleOwner.current.lifecycle
    val containerLifecycleOwner = rememberContainerLifecycleOwner(node, parentLifecycle)
    
    // SaveableStateRegistry for container's rememberSaveable
    val containerSaveableStateRegistry = rememberContainerSaveableStateRegistry(node)
    
    // ViewModelStoreOwner for container-scoped ViewModels/MVI containers
    val containerViewModelStoreOwner = rememberContainerViewModelStoreOwner(node)
    
    // Provide container-level composition locals
    CompositionLocalProvider(
        LocalContainerNode provides node,
        LocalLifecycleOwner provides containerLifecycleOwner,
        LocalRetainedValuesStore provides node.retainedValuesStore,
        LocalSaveableStateRegistry provides containerSaveableStateRegistry,
        LocalViewModelStoreOwner provides containerViewModelStoreOwner,
        LocalSavedStateRegistryOwner provides node,
    ) {
        scope.saveableStateHolder.SaveableStateProvider(node.uuid) {
            DisposableEffect(node) {
                node.attachToUI()
                onDispose {
                    if (node.isAttachedToNavigator) {
                        node.composeSavedState = containerSaveableStateRegistry.performSave()
                    }
                    node.detachFromUI()
                }
            }
            
            // Get wrapper composable from registry
            val wrapper = scope.containerRegistry.getTabWrapper(node.wrapperKey)
            
            // Render wrapper with tab scope
            val tabScope = rememberTabsContainerScope(node, scope)
            wrapper?.invoke(tabScope)
                ?: DefaultTabWrapper(tabScope) // Fallback if no custom wrapper
        }
    }
}

/**
 * Composition local for accessing the current container node.
 * Used by shared MVI containers to access container-level state.
 */
val LocalContainerNode = compositionLocalOf<LifecycleAwareNode?> { null }
```

### 3.5 Shared MVI Containers for Tab/Pane Scopes

Create infrastructure for container-level MVI containers that children can access:

```kotlin
// quo-vadis-core-flow-mvi/src/commonMain/kotlin/.../flowmvi/SharedContainerNavigation.kt

/**
 * Base class for MVI containers shared across all screens within a Tab or Pane container.
 *
 * Example: A TabContainer with a badge count that updates based on events from any child screen.
 *
 * Usage in container wrapper:
 * ```kotlin
 * @TabsContainer(MainTabs::class)
 * @Composable
 * fun MainTabsWrapper(scope: TabsContainerScope) {
 *     val sharedStore = rememberSharedContainer<MainTabsContainer>()
 *     
 *     // Provide to children via CompositionLocal or scope
 *     CompositionLocalProvider(LocalMainTabsStore provides sharedStore) {
 *         scope.TabContent()
 *     }
 * }
 * ```
 *
 * Usage in child screen:
 * ```kotlin
 * @Screen(HomeDestination::class)
 * @Composable
 * fun HomeScreen() {
 *     val sharedStore = LocalMainTabsStore.current
 *     val tabState by sharedStore.subscribe() // Access tab-wide state
 * }
 * ```
 */
abstract class SharedNavigationContainer<S : MVIState, I : MVIIntent, A : MVIAction>(
    private val scope: SharedContainerScope,
) : Container<S, I, A> {
    
    protected val navigator: Navigator get() = scope.navigator
    protected val containerKey: String get() = scope.containerNode.key
    
    protected val savedStateHandle: SavedStateHandle by lazy {
        createSavedStateHandle(scope.containerNode.savedStateRegistry)
    }
    
    protected val containerScope: CoroutineScope get() = scope.coroutineScope
    
    abstract override val store: Store<S, I, A>
}

/**
 * Scope for shared containers bound to a Tab/Pane container node.
 */
@Stable
class SharedContainerScope(
    override val scope: Scope,
    val containerNode: LifecycleAwareNode,
    val navigator: Navigator,
) : KoinScopeComponent {
    
    val coroutineScope: CoroutineScope by scope.inject()
    
    init {
        containerNode.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                scope.close()
            }
        })
    }
}

/**
 * Remember a shared container scoped to the current Tab/Pane container.
 * Must be called within a container wrapper composable.
 */
@FlowMVIDSL
@Composable
inline fun <reified C : SharedNavigationContainer<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> rememberSharedContainer(
    noinline params: ParametersDefinition = { emptyParametersHolder() },
): Store<S, I, A> {
    val containerNode = LocalContainerNode.current 
        ?: error("rememberSharedContainer must be called within a container wrapper")
    val navigator = LocalNavigator.current
        ?: error("rememberSharedContainer must be called within NavigationHost")
    
    val koin = getKoin()
    
    val scope = remember(containerNode.uuid) {
        val koinScope = koin.getOrCreateScope<SharedContainerScope>(containerNode.uuid)
        koinScope.declare(instance = CoroutineScope(Dispatchers.Main + SupervisorJob()))
        
        SharedContainerScope(
            scope = koinScope,
            containerNode = containerNode,
            navigator = navigator,
        )
    }
    
    val container = remember(scope) {
        koin.get<C>(scope = scope.scope, parameters = params)
    }
    
    return container.store
}

/**
 * Koin module extension for declaring shared containers.
 */
@FlowMVIDSL
inline fun <reified C : SharedNavigationContainer<*, *, *>> Module.sharedNavigationContainer(
    noinline definition: Definition<C>,
) {
    scope<SharedContainerScope> {
        scoped { params ->
            definition(this, params).apply {
                store.start(get<CoroutineScope>())
            }
        }
    }
}
```

### 3.6 Screen Content Rendering

Update the screen rendering to provide proper CompositionLocals:

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../compose/render/ScreenRenderer.kt

@Composable
internal fun ScreenContent(
    node: ScreenNode,
    scope: NavRenderScope,
) {
    // Create lifecycle owner that combines screen lifecycle with parent
    val parentLifecycle = LocalLifecycleOwner.current.lifecycle
    val entryLifecycleOwner = rememberScreenLifecycleOwner(node, parentLifecycle)
    
    // Create SaveableStateRegistry for this screen
    val entrySaveableStateRegistry = rememberScreenSaveableStateRegistry(node)
    
    // Create ViewModelStoreOwner with SavedStateHandle support
    val entryViewModelStoreOwner = rememberScreenViewModelStoreOwner(node)
    
    // Provide all composition locals for this screen
    CompositionLocalProvider(
        LocalScreenNode provides node,
        LocalLifecycleOwner provides entryLifecycleOwner,
        LocalRetainedValuesStore provides node.retainedValuesStore,
        LocalSaveableStateRegistry provides entrySaveableStateRegistry,
        LocalViewModelStoreOwner provides entryViewModelStoreOwner,
        LocalSavedStateRegistryOwner provides node,
    ) {
        // Use SaveableStateProvider for rememberSaveable isolation
        scope.saveableStateHolder.SaveableStateProvider(node.uuid) {
            DisposableEffect(node) {
                node.attachToUI()
                onDispose {
                    // Save state before disposal
                    if (node.isAttachedToNavigator) {
                        node.composeSavedState = entrySaveableStateRegistry.performSave()
                    }
                    node.detachFromUI()
                }
            }
            
            // Render screen content
            val content = scope.screenRegistry.getContent(node.destination::class)
            content?.invoke(node.destination, scope.navigator)
        }
    }
}

@Composable
private fun rememberScreenLifecycleOwner(
    node: ScreenNode,
    parentLifecycle: Lifecycle,
): LifecycleOwner {
    return remember(node) {
        ScreenLifecycleOwner(parentLifecycle, node.lifecycle)
    }
}

private class ScreenLifecycleOwner(
    private val parentLifecycle: Lifecycle,
    private val screenLifecycle: Lifecycle,
) : LifecycleOwner {
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val observer = LifecycleEventObserver { _, _ -> updateState() }
    
    override val lifecycle: Lifecycle = lifecycleRegistry
    
    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        parentLifecycle.addObserver(observer)
        screenLifecycle.addObserver(observer)
    }
    
    private fun updateState() {
        val parentState = parentLifecycle.currentState
        val screenState = screenLifecycle.currentState
        val targetState = minOf(parentState, screenState)
        lifecycleRegistry.currentState = targetState
    }
    
    fun close() {
        parentLifecycle.removeObserver(observer)
        screenLifecycle.removeObserver(observer)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}

@Composable
private fun rememberScreenSaveableStateRegistry(
    node: ScreenNode,
): SaveableStateRegistry {
    val parentRegistry = LocalSaveableStateRegistry.current
    return remember(node) {
        SaveableStateRegistry(
            restoredValues = node.composeSavedState,
            canBeSaved = { parentRegistry?.canBeSaved(it) ?: true }
        )
    }
}

@Composable
private fun rememberScreenViewModelStoreOwner(
    node: ScreenNode,
): ViewModelStoreOwner {
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
    return remember(node) {
        object : ViewModelStoreOwner, 
                 SavedStateRegistryOwner by savedStateRegistryOwner,
                 HasDefaultViewModelProviderFactory {
            
            override val viewModelStore: ViewModelStore
                get() = node.viewModelStore
            
            override val defaultViewModelProviderFactory: ViewModelProvider.Factory
                get() = SavedStateViewModelFactory()
            
            override val defaultViewModelCreationExtras: CreationExtras
                get() = MutableCreationExtras().apply {
                    this[SAVED_STATE_REGISTRY_OWNER_KEY] = this@object
                    this[VIEW_MODEL_STORE_OWNER_KEY] = this@object
                }
            
            init {
                enableSavedStateHandles()
            }
        }
    }
}
```

### 3.7 Navigation State Persistence

The Navigator needs to handle saving/restoring the entire navigation tree:

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../core/NavigationStateSaver.kt

interface NavigationStateSaver {
    /**
     * Save the current navigation state to a SavedState bundle.
     * Called by the platform layer (Activity.onSaveInstanceState, etc.)
     */
    fun saveState(navigator: Navigator): SavedState
    
    /**
     * Restore navigation state from a SavedState bundle.
     * Called during process restart before first navigation.
     */
    fun restoreState(savedState: SavedState): NavNode?
}

class DefaultNavigationStateSaver : NavigationStateSaver {
    
    override fun saveState(navigator: Navigator): SavedState {
        return SavedState(
            KEY_NAV_TREE to navigator.state.value.saveToBundle(),
        )
    }
    
    override fun restoreState(savedState: SavedState): NavNode? {
        val treeBundle = savedState[KEY_NAV_TREE] as? SavedState ?: return null
        return NavNode.restoreFromBundle(treeBundle)
    }
    
    companion object {
        private const val KEY_NAV_TREE = "navTree"
    }
}
```

### 3.8 Screen-Scoped FlowMVI Integration

Create a new FlowMVI integration that leverages the lifecycle system:

```kotlin
// quo-vadis-core-flow-mvi/src/commonMain/kotlin/.../flowmvi/FlowMVINavigation.kt

/**
 * Scope for FlowMVI containers tied to a screen's lifecycle.
 */
@Stable
class NavigationContainerScope(
    override val scope: Scope,
    val screenNode: ScreenNode,
    val navigator: Navigator,
) : KoinScopeComponent {
    
    val coroutineScope: CoroutineScope by scope.inject()
    
    // Auto-cancel when screen is destroyed
    init {
        screenNode.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                scope.close()
            }
        })
    }
}

/**
 * Base container with proper lifecycle and SavedState integration.
 */
abstract class NavigationContainer<S : MVIState, I : MVIIntent, A : MVIAction>(
    private val scope: NavigationContainerScope,
) : Container<S, I, A> {
    
    protected val navigator: Navigator get() = scope.navigator
    protected val screenKey: String get() = scope.screenNode.key
    
    // Access saved state through the SavedStateRegistry
    protected val savedStateHandle: SavedStateHandle by lazy {
        createSavedStateHandle(scope.screenNode.savedStateRegistry)
    }
    
    // Lifecycle-aware coroutine scope
    protected val containerScope: CoroutineScope get() = scope.coroutineScope
    
    /**
     * Override to define store configuration with savedstate plugin.
     */
    abstract override val store: Store<S, I, A>
    
    /**
     * Helper to create store with savedstate integration.
     */
    protected inline fun <reified S : MVIState> savedStateStore(
        initial: S,
        key: String = "mvi_state",
        noinline configure: StoreBuilder<S, I, A>.() -> Unit,
    ): Store<S, I, A> {
        return store(initial) {
            // FlowMVI savedstate plugin integration
            savedState(
                saver = savedStateSaver(savedStateHandle, key),
            )
            configure()
        }
    }
}

/**
 * Composable function to get or create a container scoped to the current screen.
 */
@FlowMVIDSL
@Composable
inline fun <reified C : NavigationContainer<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> rememberContainer(
    noinline params: ParametersDefinition = { emptyParametersHolder() },
): Store<S, I, A> {
    val screenNode = LocalScreenNode.current 
        ?: error("rememberContainer must be called within a screen")
    val navigator = LocalNavigator.current
        ?: error("rememberContainer must be called within NavigationHost")
    
    val koin = getKoin()
    
    val scope = remember(screenNode.uuid) {
        val koinScope = koin.getOrCreateScope<NavigationContainerScope>(screenNode.uuid)
        
        // Declare dependencies in scope
        koinScope.declare(instance = CoroutineScope(Dispatchers.Main + SupervisorJob()))
        
        NavigationContainerScope(
            scope = koinScope,
            screenNode = screenNode,
            navigator = navigator,
        )
    }
    
    val container = remember(scope) {
        koin.get<C>(scope = scope.scope, parameters = params)
    }
    
    return container.store
}

/**
 * Koin module extension for declaring containers.
 */
@FlowMVIDSL
inline fun <reified C : NavigationContainer<*, *, *>> Module.navigationContainer(
    noinline definition: Definition<C>,
) {
    scope<NavigationContainerScope> {
        scoped { params ->
            definition(this, params).apply {
                store.start(get<CoroutineScope>())
            }
        }
    }
}
```

### 3.9 FlowMVI SavedState Integration

Create helpers for FlowMVI's savedstate module:

```kotlin
// quo-vadis-core-flow-mvi/src/commonMain/kotlin/.../flowmvi/SavedStateIntegration.kt

import pro.respawn.flowmvi.savedstate.api.Saver
import pro.respawn.flowmvi.savedstate.dsl.TypedSaver

/**
 * Create a Saver that uses AndroidX SavedStateHandle for persistence.
 */
@Suppress("FunctionName")
inline fun <reified S : MVIState> SavedStateHandleSaver(
    savedStateHandle: SavedStateHandle,
    key: String = "flowmvi_state",
): Saver<S> = object : Saver<S> {
    
    override suspend fun save(state: S) {
        // Serialize state using kotlinx.serialization
        val json = Json.encodeToString(state)
        savedStateHandle[key] = json
    }
    
    override suspend fun restore(): S? {
        val json = savedStateHandle.get<String>(key) ?: return null
        return try {
            Json.decodeFromString<S>(json)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Extension to create SavedStateHandle from SavedStateRegistry.
 */
fun createSavedStateHandle(registry: SavedStateRegistry): SavedStateHandle {
    return SavedStateHandle(registry.consumeRestoredStateForKey("savedStateHandle") ?: emptyMap())
}
```

### 3.10 FlowMVI Store Composition Support

Support FlowMVI's delegate pattern for composing stores across navigation hierarchy:

```kotlin
// quo-vadis-core-flow-mvi/src/commonMain/kotlin/.../flowmvi/StoreComposition.kt

/**
 * Support for FlowMVI store composition (delegates pattern).
 * 
 * This enables parent stores (container-scoped) to delegate to child stores
 * (screen-scoped) and vice versa, following FlowMVI's composition model:
 * https://opensource.respawn.pro/FlowMVI/plugins/delegates
 * 
 * ## Use Cases
 * 
 * 1. **Parent → Child Delegation (Top-Down)**
 *    Container-scoped store delegates part of its state to screen stores
 *    Example: Tab container delegates feed loading to individual screens
 * 
 * 2. **Child → Parent Delegation (Bottom-Up)**
 *    Screen stores report state up to container store
 *    Example: Screen updates badge count in parent tab container
 * 
 * 3. **Sibling Communication (Horizontal)**
 *    Screens within same container coordinate through shared store
 *    Example: Master-detail pane synchronization
 */

/**
 * Extension to access parent container's store from a screen container.
 * 
 * Usage:
 * ```kotlin
 * class DetailScreenContainer(scope: NavigationContainerScope) : 
 *     NavigationContainer<DetailState, DetailIntent, DetailAction>(scope) {
 *     
 *     // Access shared store from parent TabNode
 *     private val sharedStore by parentStore<MainTabsContainer>()
 *     
 *     override val store = store(DetailState()) {
 *         // Delegate to parent store for shared state
 *         val sharedState by delegate(sharedStore) { action ->
 *             // Handle actions from parent
 *         }
 *         
 *         whileSubscribed {
 *             combine(state, sharedState) { detail, shared ->
 *                 // Compose states
 *             }.consume()
 *         }
 *     }
 * }
 * ```
 */
inline fun <reified C : SharedNavigationContainer<*, *, *>> NavigationContainerScope.parentStore(): Lazy<Store<*, *, *>> {
    return lazy {
        // Find parent container node from the navigation tree
        val screenNode = this.screenNode
        val parentContainerKey = findParentContainerKey(screenNode) 
            ?: error("No parent container found for screen ${screenNode.key}")
        
        // Get the store from the parent container's scope
        val parentScope = getKoin().getScopeOrNull(parentContainerKey)
            ?: error("Parent container scope not found: $parentContainerKey")
        
        parentScope.get<C>().store
    }
}

/**
 * Extension for shared containers to access child screen stores.
 * 
 * Usage:
 * ```kotlin
 * class MainTabsContainer(scope: SharedContainerScope) :
 *     SharedNavigationContainer<TabState, TabIntent, TabAction>(scope) {
 *     
 *     override val store = store(TabState()) {
 *         // When a specific screen is active, delegate to it
 *         onIntent<TabIntent.FocusDetail> {
 *             val detailStore = childStoreOrNull<DetailScreenContainer>()
 *             detailStore?.let { 
 *                 val detailState by delegate(it) { /* handle actions */ }
 *             }
 *         }
 *     }
 * }
 * ```
 */
inline fun <reified C : NavigationContainer<*, *, *>> SharedContainerScope.childStoreOrNull(
    screenKey: String? = null,
): Store<*, *, *>? {
    // Find child screen in container's children
    val targetKey = screenKey ?: findActiveScreenKey(containerNode)
        ?: return null
    
    val childScope = getKoin().getScopeOrNull(targetKey)
        ?: return null
    
    return runCatching { childScope.get<C>().store }.getOrNull()
}

/**
 * Provide store from one navigation scope to another via CompositionLocal.
 * 
 * This is the simplest approach for store sharing without direct coupling.
 * 
 * Usage in container wrapper:
 * ```kotlin
 * @TabsContainer(MainTabs::class)
 * @Composable
 * fun MainTabsWrapper(scope: TabsContainerScope) {
 *     val sharedStore = rememberSharedContainer<MainTabsContainer>()
 *     
 *     ProvideStore(sharedStore) {
 *         scope.TabContent()
 *     }
 * }
 * ```
 * 
 * Usage in child screen:
 * ```kotlin
 * @Composable
 * fun DetailScreen() {
 *     // Get the shared store without direct container dependency
 *     val sharedStore = LocalSharedStore.current
 * }
 * ```
 */
@Composable
fun <S : MVIState, I : MVIIntent, A : MVIAction> ProvideStore(
    store: Store<S, I, A>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSharedStore provides store,
        content = content,
    )
}

/**
 * CompositionLocal for sharing stores across navigation hierarchy.
 * 
 * Note: For type-safe access, prefer defining your own typed CompositionLocal
 * per store type (e.g., LocalMainTabsStore).
 */
val LocalSharedStore = compositionLocalOf<Store<*, *, *>?> { null }

/**
 * Helper to find parent container key from navigation tree.
 */
private fun findParentContainerKey(node: NavNode): String? {
    var current: NavNode? = node
    while (current != null) {
        if (current is LifecycleAwareNode && current !is ScreenNode) {
            return current.uuid
        }
        current = current.parent // Assuming parent reference exists
    }
    return null
}

/**
 * Helper to find active screen in a container.
 */
private fun findActiveScreenKey(container: LifecycleAwareNode): String? {
    return when (container) {
        is TabNode -> container.activeStack.children.lastOrNull()?.let { 
            (it as? ScreenNode)?.uuid 
        }
        is PaneNode -> container.activePaneContent?.let {
            when (it) {
                is ScreenNode -> it.uuid
                is StackNode -> (it.children.lastOrNull() as? ScreenNode)?.uuid
                else -> null
            }
        }
        else -> null
    }
}
```

### 3.11 Advanced Store Composition Patterns

Document common patterns for store composition in navigation context:

```kotlin
// Pattern 1: Composed State (Parent aggregates child states)
// =========================================================

/**
 * Container store that aggregates state from active screens.
 */
class MainTabsContainer(scope: SharedContainerScope) :
    SharedNavigationContainer<ComposedTabState, TabIntent, TabAction>(scope) {
    
    // Child stores (lazy - created when screens are active)
    private var feedStore: Store<FeedState, *, *>? = null
    private var profileStore: Store<ProfileState, *, *>? = null
    
    override val store = store(ComposedTabState.Initial) {
        // Register as parent for child stores
        init {
            // Listen for child store registrations via containerScope
        }
        
        // Delegate pattern: compose state from children
        whileSubscribed {
            combine(
                feedStore?.state ?: flowOf(FeedState.Empty),
                profileStore?.state ?: flowOf(ProfileState.Empty),
            ) { feed, profile ->
                updateState { ComposedTabState(feed, profile, unreadCount) }
            }.consume()
        }
    }
    
    // Called when screen stores register/unregister
    fun registerChildStore(key: String, store: Store<*, *, *>) {
        when {
            key.contains("feed") -> feedStore = store as? Store<FeedState, *, *>
            key.contains("profile") -> profileStore = store as? Store<ProfileState, *, *>
        }
    }
}

// Pattern 2: Event Bus (Sibling communication via parent)
// =======================================================

/**
 * Container acts as event bus for screen communication.
 */
sealed interface TabEvent : MVIAction {
    data class BadgeUpdated(val count: Int) : TabEvent
    data class SelectionChanged(val id: String) : TabEvent
}

class EventBusContainer(scope: SharedContainerScope) :
    SharedNavigationContainer<EventBusState, EventBusIntent, TabEvent>(scope) {
    
    override val store = store(EventBusState()) {
        // Screens can send events that other screens observe
        onIntent<EventBusIntent.UpdateBadge> { intent ->
            action(TabEvent.BadgeUpdated(intent.count))
        }
        
        onIntent<EventBusIntent.SelectItem> { intent ->
            action(TabEvent.SelectionChanged(intent.id))
        }
    }
}

// Usage in screen:
class DetailScreenContainer(scope: NavigationContainerScope) :
    NavigationContainer<DetailState, DetailIntent, DetailAction>(scope) {
    
    private val eventBus by parentStore<EventBusContainer>()
    
    override val store = store(DetailState()) {
        // Subscribe to events from siblings
        init {
            launch {
                eventBus.actions.collect { event ->
                    when (event) {
                        is TabEvent.SelectionChanged -> {
                            // React to selection from sibling screen
                            intent(DetailIntent.LoadItem(event.id))
                        }
                        else -> {}
                    }
                }
            }
        }
        
        // Send events to siblings via parent
        onIntent<DetailIntent.MarkAsRead> {
            val count = state.value.unreadCount - 1
            eventBus.send(EventBusIntent.UpdateBadge(count))
        }
    }
}

// Pattern 3: Master-Detail Pane Coordination
// ==========================================

/**
 * Pane container that coordinates master and detail stores.
 */
class ListDetailContainer(scope: SharedContainerScope) :
    SharedNavigationContainer<ListDetailState, ListDetailIntent, ListDetailAction>(scope) {
    
    override val store = store(ListDetailState.Initial) {
        // Coordinate selection between panes
        onIntent<ListDetailIntent.SelectItem> { intent ->
            updateState { copy(selectedId = intent.id) }
            // Navigate detail pane to selected item
            scope.navigator.navigateToPane(
                PaneRole.Supporting,
                DetailDestination(intent.id)
            )
        }
    }
}

// Master screen subscribes to shared state
class ListScreenContainer(scope: NavigationContainerScope) :
    NavigationContainer<ListState, ListIntent, ListAction>(scope) {
    
    private val sharedStore by parentStore<ListDetailContainer>()
    
    override val store = store(ListState.Loading) {
        // Highlight selected item based on shared state
        val sharedState by delegate(sharedStore)
        
        whileSubscribed {
            combine(state, sharedState) { list, shared ->
                updateState { copy(highlightedId = shared.selectedId) }
            }.consume()
        }
    }
}
```

---

## Part 4: Implementation Plan

### Phase 1: Core Lifecycle Infrastructure (Week 1)

**Files to create:**
1. `LifecycleAwareNode.kt` - Common interface for lifecycle-aware nodes

**Files to modify:**
2. `ScreenNode.kt` - Transform to class implementing `LifecycleAwareNode`
3. `TabNode.kt` - Transform to class implementing `LifecycleAwareNode`
4. `PaneNode.kt` - Transform to class implementing `LifecycleAwareNode`
5. `StackNode.kt` - Update to track child node lifecycle transitions
6. `NavNode.kt` - Add serialization support for state restoration
7. Add new `NavigationStateSaver.kt`

**Dependencies to add:**
```toml
[libraries]
# Already present, ensure versions:
androidx-lifecycle-viewmodelCompose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }
androidx-lifecycle-runtimeCompose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }
# New additions:
androidx-savedstate = { module = "org.jetbrains.androidx.savedstate:savedstate", version = "1.2.1" }
```

### Phase 2: Compose Integration (Week 2)

**Files to create:**
1. `ScreenRenderer.kt` - Render screens with proper CompositionLocals
2. `ContainerRenderer.kt` - Render Tab/Pane containers with lifecycle

**Files to modify:**
3. `NavTreeRenderer.kt` - Use new ScreenRenderer and ContainerRenderer
4. `CompositionLocals.kt` - Add `LocalNavigator`, `LocalContainerNode`, update others
5. `NavigationHost.kt` - Handle SavedState at root level
6. `TabsContainerScope.kt` - Update to receive container lifecycle
7. `PaneContainerScope.kt` - Update to receive container lifecycle

**Key composables to implement:**
- `rememberScreenLifecycleOwner()` / `rememberContainerLifecycleOwner()`
- `rememberScreenSaveableStateRegistry()` / `rememberContainerSaveableStateRegistry()`
- `rememberScreenViewModelStoreOwner()` / `rememberContainerViewModelStoreOwner()`

### Phase 3: FlowMVI Refactoring (Week 3)

**Files to create:**
1. `NavigationContainer.kt` - Base container for screen-scoped MVI
2. `SharedNavigationContainer.kt` - Base container for container-scoped MVI (shared across children)
3. `SavedStateIntegration.kt` - FlowMVI savedstate helpers
4. `FlowMVINavigation.kt` - New composable APIs for both scopes

**Files to modify:**
5. Remove/deprecate `BaseContainer.kt`
6. Remove/deprecate `DestinationScope.kt`

**Breaking changes:**
- `BaseContainer` → `NavigationContainer` (screen-scoped)
- New `SharedNavigationContainer` for container-scoped MVI
- `container()` composable → `rememberContainer()` (screen-scoped)
- New `rememberSharedContainer()` (container-scoped)
- Module definition: `container { }` → `navigationContainer { }` / `sharedNavigationContainer { }`

### Phase 4: Remove Legacy Code (Week 4)

**Files to remove:**
1. `NavigationLifecycle.kt` - Replaced by androidx.lifecycle.Lifecycle
2. `NavigationLifecycleManager.kt` - No longer needed
3. `NavigatorLifecycleExtensions.kt` - Replace with standard lifecycle patterns

**Update dependent code:**
- Any code using old `NavigationLifecycle` interface
- Demo app screens and containers
- Tests

---

## Part 5: Migration Guide

### 5.1 For Screens Using rememberSaveable

**Before (no change needed):**
```kotlin
@Composable
fun MyScreen() {
    var count by rememberSaveable { mutableStateOf(0) }
    // Works automatically - now persists across process death
}
```

### 5.2 For FlowMVI Containers (Screen-Scoped)

**Before:**
```kotlin
abstract class BaseContainer<S, I, A>(
    protected val navigator: Navigator,
    protected val screenKey: String,
) : Container<S, I, A>, NavigationLifecycle

// Usage
class MyContainer(navigator: Navigator, screenKey: String) : 
    BaseContainer<MyState, MyIntent, MyAction>(navigator, screenKey) {
    
    override val store = store(MyState()) {
        // No savedstate
    }
}
```

**After:**
```kotlin
abstract class NavigationContainer<S, I, A>(
    scope: NavigationContainerScope,
) : Container<S, I, A>

// Usage
class MyContainer(scope: NavigationContainerScope) : 
    NavigationContainer<MyState, MyIntent, MyAction>(scope) {
    
    override val store = savedStateStore(MyState()) {
        // State automatically saved/restored
    }
}
```

### 5.3 For Shared MVI Containers (Container-Scoped)

**New pattern for Tab/Pane-wide state:**

```kotlin
// 1. Define shared container
class MainTabsContainer(scope: SharedContainerScope) : 
    SharedNavigationContainer<TabState, TabIntent, TabAction>(scope) {
    
    override val store = savedStateStore(TabState()) {
        // Shared state accessible from all child screens
    }
}

// 2. Register in Koin module
val tabModule = module {
    sharedNavigationContainer<MainTabsContainer> { scope ->
        MainTabsContainer(scope)
    }
}

// 3. Use in container wrapper
@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(scope: TabsContainerScope) {
    val sharedStore = rememberSharedContainer<MainTabsContainer>()
    
    CompositionLocalProvider(LocalMainTabsStore provides sharedStore) {
        Column {
            scope.TabContent()  // Children can access LocalMainTabsStore
            TabBar(scope, sharedStore)
        }
    }
}

// 4. Access from child screens
@Screen(HomeDestination::class)
@Composable
fun HomeScreen() {
    val sharedStore = LocalMainTabsStore.current
    val tabState by sharedStore.subscribe()
    
    Button(onClick = { sharedStore.send(UpdateBadge(5)) }) {
        Text("Update tab badge")
    }
}
```

### 5.4 For Custom Lifecycle Observers

**Before:**
```kotlin
class MyObserver(navigator: Navigator) : NavigationLifecycle {
    init { navigator.registerNavigationLifecycle(this) }
    
    override fun onEnter() { /* ... */ }
    override fun onExit() { /* ... */ }
    override fun onDestroy() { 
        navigator.unregisterNavigationLifecycle(this)
    }
}
```

**After:**
```kotlin
@Composable
fun MyScreen() {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { /* onEnter */ }
                Lifecycle.Event.ON_PAUSE -> { /* onExit */ }
                Lifecycle.Event.ON_DESTROY -> { /* cleanup */ }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
```

---

## Part 6: API Reference

### 6.1 New Composition Locals

| Local | Type | Purpose |
|-------|------|---------|
| `LocalScreenNode` | `ScreenNode?` | Access current screen's node (unchanged) |
| `LocalContainerNode` | `LifecycleAwareNode?` | Access current Tab/Pane container node (new) |
| `LocalNavigator` | `Navigator?` | Access navigator instance (new) |
| `LocalLifecycleOwner` | `LifecycleOwner` | Standard AndroidX lifecycle (provided per screen/container) |
| `LocalViewModelStoreOwner` | `ViewModelStoreOwner` | ViewModel scoping (provided per screen/container) |
| `LocalSavedStateRegistryOwner` | `SavedStateRegistryOwner` | SavedStateHandle access (provided per screen/container) |

### 6.2 Lifecycle States

**ScreenNode / TabNode / PaneNode (all LifecycleAwareNode):**

| State | When | Description |
|-------|------|-------------|
| `INITIALIZED` | Created | Node exists but not attached |
| `CREATED` | `attachToNavigator()` | In navigation tree but not displayed |
| `STARTED` | Transitioning | Being animated in/out |
| `RESUMED` | `attachToUI()` | Actively displayed |
| `DESTROYED` | `close()` | Removed from tree |

### 6.3 FlowMVI DSL

**Screen-scoped containers:**
```kotlin
// In Koin module
val myModule = module {
    navigationContainer<MyContainer> { scope ->
        MyContainer(scope)
    }
}

// In Screen Composable
@Composable
fun MyScreen() {
    val store = rememberContainer<MyContainer>()
}
```

**Container-scoped (shared) containers:**
```kotlin
// In Koin module
val myModule = module {
    sharedNavigationContainer<MyTabsContainer> { scope ->
        MyTabsContainer(scope)
    }
}

// In Container Wrapper Composable
@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(scope: TabsContainerScope) {
    val sharedStore = rememberSharedContainer<MyTabsContainer>()
    // Provide to children via CompositionLocal
}
```

### 6.4 FlowMVI Store Composition

**Access parent store from screen:**
```kotlin
class DetailContainer(scope: NavigationContainerScope) : NavigationContainer<...>(scope) {
    private val parentStore by parentStore<MainTabsContainer>()
    
    override val store = store(State()) {
        val parentState by delegate(parentStore) { action -> /* handle */ }
    }
}
```

**Access child store from container:**
```kotlin
class TabsContainer(scope: SharedContainerScope) : SharedNavigationContainer<...>(scope) {
    override val store = store(State()) {
        onIntent<Intent.CheckChild> {
            val childStore = childStoreOrNull<DetailContainer>()
            // Use child store if screen is active
        }
    }
}
```

**Share store via CompositionLocal:**
```kotlin
// In wrapper
ProvideStore(sharedStore) { scope.TabContent() }

// In child
val store = LocalSharedStore.current
```

---

## Part 7: Testing Strategy

### 7.1 Unit Tests

1. **ScreenNode lifecycle transitions**
   - Test state transitions (CREATED → RESUMED → STARTED → DESTROYED)
   - Test ViewModelStore clearing on destroy
   - Test SavedStateRegistry save/restore

2. **NavigationStateSaver**
   - Test full tree serialization/deserialization
   - Test handling of nested structures (Tabs, Panes)

3. **FlowMVI SavedState**
   - Test state persistence across simulated process death
   - Test container scope cleanup

### 7.2 Integration Tests

1. **Compose UI Tests**
   - Test rememberSaveable persistence across navigation
   - Test ViewModel scoping per screen
   - Test lifecycle observer behavior

2. **Process Death Simulation**
   - Save state → recreate → verify restoration
   - Test with complex navigation trees

---

## Part 8: Open Questions

### 8.1 Resolved ✅

1. **Lifecycle API choice** → Use `androidx.lifecycle.Lifecycle`
2. **SavedState scope** → Per-screen isolation
3. **ViewModel support level** → Full (ViewModelStoreOwner + SavedStateRegistryOwner)
4. **FlowMVI priority** → Primary focus, ViewModel is nice-to-have

### 8.2 Remaining Considerations

1. **Compose Retain API**: Tiamat uses `ManagedRetainedValuesStore` from Compose 1.8+. Should we require this minimum version?

2. **Nested NavControllers**: Tiamat supports nested `NavController` instances with inherited saveable settings. Do we need similar for nested Navigators?

3. **Platform-specific SavedState**: Android uses `Bundle`, other platforms use Map. Should we abstract this or use kotlinx.serialization throughout?

---

## References

- [Tiamat NavEntry.kt](https://github.com/ComposeGears/Tiamat/blob/main/tiamat/src/commonMain/kotlin/com/composegears/tiamat/navigation/NavEntry.kt) - Lifecycle/SavedState implementation
- [Tiamat ComposableNavEntry.kt](https://github.com/ComposeGears/Tiamat/blob/main/tiamat/src/commonMain/kotlin/com/composegears/tiamat/compose/ComposableNavEntry.kt) - CompositionLocal providers
- [FlowMVI SavedState Module](https://respawn.pro/FlowMVI/savedstate/) - FlowMVI savedstate integration
- [AndroidX Lifecycle](https://developer.android.com/reference/androidx/lifecycle/Lifecycle) - Lifecycle states reference
- [AndroidX SavedState](https://developer.android.com/reference/androidx/savedstate/SavedStateRegistry) - SavedStateRegistry API
