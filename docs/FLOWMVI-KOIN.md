# FlowMVI Integration with Koin DI

This document covers the `quo-vadis-core-flow-mvi` module, which provides MVI state management integrated with Quo Vadis navigation lifecycle using [FlowMVI](https://github.com/respawn-app/FlowMVI) and [Koin](https://insert-koin.io/) dependency injection.

## Table of Contents

- [Overview](#overview)
- [Dependencies](#dependencies)
- [Screen-Scoped Containers](#screen-scoped-containers-navigationcontainer)
- [Container-Scoped Containers](#container-scoped-containers-sharednavigationcontainer)
- [Composable Integration](#composable-integration)
- [Koin DI Setup](#koin-di-setup)
- [Koin Annotations Approach](#koin-annotations-approach-alternative)
- [Lifecycle Integration](#lifecycle-integration)
- [State Flow Integration](#state-flow-integration)
- [Common Patterns](#common-patterns)
- [Testing](#testing)

---

## Overview

The `quo-vadis-core-flow-mvi` module bridges FlowMVI's powerful state management with Quo Vadis navigation lifecycle:

| Feature | Description |
|---------|-------------|
| **Screen-scoped containers** | MVI containers tied to individual screen lifecycle |
| **Container-scoped shared state** | State shared across all screens in a Tab/Pane container |
| **Automatic lifecycle management** | Containers are created, started, and cleaned up automatically |
| **Koin integration** | Type-safe dependency injection with scoped instances |
| **CoroutineScope management** | Scopes tied to navigation node lifecycle |

### Module Purpose

```
┌─────────────────────────────────────────────────────────────┐
│                    NavigationHost                           │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐     ┌─────────────────────────────┐   │
│  │   TabNode       │     │  SharedNavigationContainer  │   │
│  │   (tabs)        │────▶│  (shared state)             │   │
│  └─────────────────┘     └─────────────────────────────┘   │
│          │                                                  │
│          ▼                                                  │
│  ┌─────────────────┐     ┌─────────────────────────────┐   │
│  │   ScreenNode    │────▶│  NavigationContainer        │   │
│  │   (home)        │     │  (screen state)             │   │
│  └─────────────────┘     └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## Dependencies

Add the module to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.jermey.quo.vadis:quo-vadis-core-flow-mvi:$version")
}
```

The module transitively includes:

| Dependency | Purpose |
|------------|---------|
| `quo-vadis-core` | Core navigation library |
| `flowmvi-core` | FlowMVI state management |
| `flowmvi-compose` | Compose integration for FlowMVI |
| `flowmvi-savedstate` | State persistence support |
| `koin-core` | Koin dependency injection |
| `koin-compose` | Koin Compose integration |
| `koin-compose-viewmodel` | ViewModel support |

> **Note:** For Koin Annotations support (alternative to DSL registration), ensure you're using Koin 4.1+ and have configured the Koin Compiler Plugin.

---

## Screen-Scoped Containers (NavigationContainer)

`NavigationContainer` is the base class for MVI containers tied to a single screen's lifecycle.

### Purpose

- Container is created when screen enters composition
- Container is destroyed when screen is removed from navigation tree
- Provides access to `Navigator` for navigation operations
- Manages a `CoroutineScope` tied to screen lifecycle

### NavigationContainerScope Properties

| Property | Type | Description |
|----------|------|-------------|
| `navigator` | `Navigator` | Navigator instance for navigation operations |
| `screenKey` | `String` | Unique identifier for this screen instance |
| `coroutineScope` | `CoroutineScope` | Scope tied to screen lifecycle |
| `screenNode` | `ScreenNode` | The navigation node this container is attached to |

### Complete Example

**Contract (State, Intent, Action):**

```kotlin
// ProfileContract.kt
package com.jermey.navplayground.demo.ui.screens.profile

import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

sealed interface ProfileState : MVIState {
    data object Loading : ProfileState
    
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
    }
    
    data class Error(val message: String) : ProfileState
}

sealed interface ProfileIntent : MVIIntent {
    data object LoadProfile : ProfileIntent
    data object StartEditing : ProfileIntent
    data class UpdateName(val name: String) : ProfileIntent
    data class UpdateEmail(val email: String) : ProfileIntent
    data class UpdateBio(val bio: String) : ProfileIntent
    data object SaveChanges : ProfileIntent
    data object CancelEdit : ProfileIntent
    data object NavigateToSettings : ProfileIntent
    data object NavigateBack : ProfileIntent
    data object Logout : ProfileIntent
}

sealed interface ProfileAction : MVIAction {
    data class ShowToast(val message: String) : ProfileAction
    data class ShowError(val error: String) : ProfileAction
    data object ProfileSaved : ProfileAction
    data object LogoutSuccess : ProfileAction
    data class ValidationFailed(val errors: Map<String, String>) : ProfileAction
    data class NetworkError(val message: String) : ProfileAction
}
```

**Container Implementation:**

```kotlin
// ProfileContainer.kt
package com.jermey.navplayground.demo.ui.screens.profile

import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.quo.vadis.flowmvi.NavigationContainer
import com.jermey.quo.vadis.flowmvi.NavigationContainerScope
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.enableLogging
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.recover
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed

private typealias Ctx = PipelineContext<ProfileState, ProfileIntent, ProfileAction>

class ProfileContainer(
    scope: NavigationContainerScope,
    private val repository: ProfileRepository,
    private val debuggable: Boolean = false
) : NavigationContainer<ProfileState, ProfileIntent, ProfileAction>(scope) {

    override val store = store(initial = ProfileState.Loading) {
        configure {
            debuggable = this@ProfileContainer.debuggable
            name = "ProfileStore"
            parallelIntents = false
        }

        // Initialize: load profile on start
        init {
            intent(ProfileIntent.LoadProfile)
        }

        // Reduce: handle all intents
        reduce { intent ->
            when (intent) {
                is ProfileIntent.LoadProfile -> handleLoadProfile()
                is ProfileIntent.StartEditing -> handleStartEditing()
                is ProfileIntent.UpdateName -> handleUpdateName(intent.name)
                is ProfileIntent.UpdateEmail -> handleUpdateEmail(intent.email)
                is ProfileIntent.UpdateBio -> handleUpdateBio(intent.bio)
                is ProfileIntent.SaveChanges -> handleSaveChanges()
                is ProfileIntent.CancelEdit -> handleCancelEdit()
                is ProfileIntent.NavigateToSettings -> handleNavigateToSettings()
                is ProfileIntent.NavigateBack -> handleNavigateBack()
                is ProfileIntent.Logout -> handleLogout()
            }
        }

        // Recover: handle errors gracefully
        recover { exception ->
            action(ProfileAction.ShowError(exception.message ?: "Unknown error"))
            updateState { ProfileState.Error(exception.message ?: "An error occurred") }
            null // Suppress exception
        }

        // Enable logging for debugging
        if (debuggable) {
            enableLogging()
        }
    }

    private suspend fun Ctx.handleLoadProfile() {
        updateState { ProfileState.Loading }
        try {
            val user = repository.getUser()
            updateState { ProfileState.Content(user = user) }
        } catch (e: Exception) {
            updateState { ProfileState.Error(e.message ?: "Failed to load profile") }
            action(ProfileAction.ShowError("Failed to load profile"))
        }
    }

    private suspend fun Ctx.handleNavigateToSettings() {
        try {
            navigator.navigate(MainTabs.SettingsTab.Main)
        } catch (e: Exception) {
            action(ProfileAction.ShowError("Navigation failed"))
        }
    }

    private fun Ctx.handleNavigateBack() {
        navigator.navigateBack()
    }
    
    // ... other handlers
}
```

---

## Container-Scoped Containers (SharedNavigationContainer)

`SharedNavigationContainer` is for state shared across all screens within a Tab or Pane container.

### Purpose

- Container lives as long as the TabNode/PaneNode
- State is accessible from all child screens
- Enables cross-screen communication and coordination
- Perfect for tab-wide badges, master-detail selection, or shared preferences

### SharedContainerScope Properties

| Property | Type | Description |
|----------|------|-------------|
| `navigator` | `Navigator` | Navigator instance for navigation operations |
| `containerKey` | `String` | Unique identifier for this container instance |
| `coroutineScope` | `CoroutineScope` | Scope tied to container lifecycle |
| `containerNode` | `LifecycleAwareNode` | The TabNode or PaneNode this container is attached to |

### Complete Example

**Contract:**

```kotlin
// DemoTabsContainer.kt
package com.jermey.navplayground.demo.ui.screens.tabs

import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

data class DemoTabsState(
    val totalItemsViewed: Int = 0,
    val favoriteItems: List<String> = emptyList(),
    val notifications: List<String> = emptyList()
) : MVIState

sealed interface DemoTabsIntent : MVIIntent {
    data object IncrementViewed : DemoTabsIntent
    data class AddFavorite(val itemId: String) : DemoTabsIntent
    data class RemoveFavorite(val itemId: String) : DemoTabsIntent
    data class AddNotification(val message: String) : DemoTabsIntent
    data object ClearNotifications : DemoTabsIntent
}

sealed interface DemoTabsAction : MVIAction
```

**Container Implementation:**

```kotlin
// DemoTabsContainer.kt
package com.jermey.navplayground.demo.ui.screens.tabs

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.jermey.quo.vadis.flowmvi.SharedContainerScope
import com.jermey.quo.vadis.flowmvi.SharedNavigationContainer
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

class DemoTabsContainer(
    scope: SharedContainerScope,
) : SharedNavigationContainer<DemoTabsState, DemoTabsIntent, DemoTabsAction>(scope) {

    override val store: Store<DemoTabsState, DemoTabsIntent, DemoTabsAction> =
        store(DemoTabsState()) {
            configure {
                name = "DemoTabsStore"
            }
            reduce { intent ->
                when (intent) {
                    is DemoTabsIntent.IncrementViewed -> updateState {
                        copy(totalItemsViewed = totalItemsViewed + 1)
                    }
                    is DemoTabsIntent.AddFavorite -> updateState {
                        if (intent.itemId !in favoriteItems) {
                            copy(favoriteItems = favoriteItems + intent.itemId)
                        } else this
                    }
                    is DemoTabsIntent.RemoveFavorite -> updateState {
                        copy(favoriteItems = favoriteItems - intent.itemId)
                    }
                    is DemoTabsIntent.AddNotification -> updateState {
                        copy(notifications = notifications + intent.message)
                    }
                    is DemoTabsIntent.ClearNotifications -> updateState {
                        copy(notifications = emptyList())
                    }
                }
            }
        }
}

/**
 * CompositionLocal providing access to the DemoTabs shared store.
 */
val LocalDemoTabsStore: ProvidableCompositionLocal<Store<DemoTabsState, DemoTabsIntent, DemoTabsAction>> =
    staticCompositionLocalOf { throw IllegalStateException("No DemoTabsStore provided") }
```

---

## Composable Integration

### rememberContainer

Get a screen-scoped container:

```kotlin
@FlowMVIDSL
@Composable
inline fun <reified C : NavigationContainer<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> rememberContainer(
    noinline params: ParametersDefinition = { emptyParametersHolder() },
): Store<S, I, A>
```

**Type Parameters:**

| Parameter | Description |
|-----------|-------------|
| `C` | Container class type (e.g., `ProfileContainer`) |
| `S` | State type implementing `MVIState` |
| `I` | Intent type implementing `MVIIntent` |
| `A` | Action type implementing `MVIAction` |

**Usage:**

```kotlin
@Screen(MainTabs.ProfileTab::class)
@Composable
fun ProfileScreen(
    container: Store<ProfileState, ProfileIntent, ProfileAction> = 
        rememberContainer<ProfileContainer, ProfileState, ProfileIntent, ProfileAction>()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val state by container.subscribe { action ->
        scope.launch {
            when (action) {
                is ProfileAction.ShowToast -> {
                    snackbarHostState.showSnackbar(
                        message = action.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is ProfileAction.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = action.error,
                        duration = SnackbarDuration.Long
                    )
                }
                // Handle other actions...
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (state) {
            is ProfileState.Loading -> LoadingContent()
            is ProfileState.Content -> ProfileContent(
                state = state as ProfileState.Content,
                onIntent = container::intent
            )
            is ProfileState.Error -> ErrorContent(
                message = (state as ProfileState.Error).message,
                onRetry = { container.intent(ProfileIntent.LoadProfile) }
            )
        }
    }
}
```

### rememberSharedContainer

Get a container-scoped shared container:

```kotlin
@FlowMVIDSL
@Composable
inline fun <reified C : SharedNavigationContainer<S, I, A>, S, I, A> rememberSharedContainer(
    noinline params: ParametersDefinition = { emptyParametersHolder() },
): Store<S, I, A> where S : MVIState, I : MVIIntent, A : MVIAction
```

**Usage in Tab Container Wrapper:**

```kotlin
@TabsContainer(DemoTabs.Companion::class)
@Composable
fun DemoTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    val sharedStore = rememberSharedContainer<DemoTabsContainer, DemoTabsState, DemoTabsIntent, DemoTabsAction>()
    val state by sharedStore.subscribe()

    CompositionLocalProvider(LocalDemoTabsStore provides sharedStore) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tabs Demo (${state.totalItemsViewed} viewed)") }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = scope.activeTabIndex) {
                    // Tab strip with badges from shared state
                }
                content()
            }
        }
    }
}
```

---

## Koin DI Setup

### navigationContainer

Register screen-scoped containers:

```kotlin
inline fun <reified C : NavigationContainer<*, *, *>> Module.navigationContainer(
    crossinline factory: (NavigationContainerScope) -> C,
)
```

**Usage:**

```kotlin
val profileModule = module {
    // Register dependencies
    single { ProfileRepository() }
    
    // Register container
    navigationContainer<ProfileContainer> { scope ->
        ProfileContainer(
            scope = scope,
            repository = scope.get(),  // Inject from parent scope
            debuggable = true
        )
    }
}
```

### sharedNavigationContainer

Register container-scoped shared containers:

```kotlin
inline fun <reified C : SharedNavigationContainer<*, *, *>> Module.sharedNavigationContainer(
    crossinline factory: (SharedContainerScope) -> C,
)
```

**Usage:**

```kotlin
val tabsDemoModule = module {
    sharedNavigationContainer<DemoTabsContainer> { scope ->
        DemoTabsContainer(scope)
    }
}
```

### Multi-Module Registration

Organize containers across feature modules:

```kotlin
// feature1/src/commonMain/.../DI.kt
val feature1Module = module {
    navigationContainer<ResultDemoContainer> { scope ->
        ResultDemoContainer(scope)
    }
    navigationContainer<ItemPickerContainer> { scope ->
        ItemPickerContainer(scope)
    }
}

// composeApp/src/commonMain/.../DI.kt
val profileModule = module {
    single { ProfileRepository() }
    navigationContainer<ProfileContainer> { scope ->
        ProfileContainer(scope, scope.get(), debuggable = true)
    }
}

val stateDrivenDemoModule = module {
    sharedNavigationContainer<StateDrivenContainer> { scope ->
        StateDrivenContainer(scope)
    }
}

val tabsDemoModule = module {
    sharedNavigationContainer<DemoTabsContainer> { scope ->
        DemoTabsContainer(scope)
    }
}

// App initialization
fun initKoin() {
    startKoin {
        modules(
            navigationModule,     // Navigator setup
            profileModule,
            feature1Module,
            stateDrivenDemoModule,
            tabsDemoModule
        )
    }
}
```

---

## Koin Annotations Approach (Alternative)

As an alternative to the manual DSL registration, you can use **Koin Annotations** for compile-time dependency resolution. This approach requires **Koin 4.1+** and the Koin Compiler Plugin.

### Annotating Screen-Scoped Containers

Use `@Scoped` with `@Scope(NavigationContainerScope::class)` to register a container that's scoped to the screen's lifecycle:

```kotlin
@Scoped
@Scope(NavigationContainerScope::class)
@Qualifier(ProfileContainer::class)
class ProfileContainer(
    scope: NavigationContainerScope,
    private val repository: ProfileRepository
) : NavigationContainer<ProfileState, ProfileIntent, ProfileAction>(scope) {
    // Container implementation...
}
```

### Annotating Shared Containers

Use `@Scoped` with `@Scope(SharedContainerScope::class)` for containers shared across tabs/panes:

```kotlin
@Scoped
@Scope(SharedContainerScope::class)
@Qualifier(DemoTabsContainer::class)
class DemoTabsContainer(
    scope: SharedContainerScope,
) : SharedNavigationContainer<DemoTabsState, DemoTabsIntent, DemoTabsAction>(scope) {
    // Container implementation...
}
```

### Annotating Dependencies

Use `@Factory` for stateless dependencies (new instance each time) or `@Single` for singletons:

```kotlin
@Factory
class ProfileRepository {
    suspend fun getUser(): UserData { /* ... */ }
}
```

### Module with ComponentScan

Create a Koin module that automatically discovers annotated components:

```kotlin
@Module
@ComponentScan("com.example.feature.profile")
class ProfileModule

@Module
@ComponentScan("com.example.feature.tabs")
class TabsModule
```

### Choosing Between Approaches

| Use DSL When | Use Annotations When |
|--------------|---------------------|
| You need runtime dependency customization | You prefer compile-time safety |
| Complex factory logic is required | Standard dependency patterns suffice |
| Migrating from existing DSL code | Starting a new feature module |
| Advanced Koin features needed | Minimal boilerplate is preferred |

Both approaches work correctly with `rememberContainer` and `rememberSharedContainer` - the choice depends on your project's needs.

---

## Lifecycle Integration

### Container Creation and Destruction

```
┌─────────────────────────────────────────────────────────────────┐
│                     Screen Lifecycle                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Screen enters composition                                       │
│         │                                                        │
│         ▼                                                        │
│  ┌──────────────────┐                                           │
│  │ rememberContainer │                                           │
│  │ is called        │                                           │
│  └────────┬─────────┘                                           │
│           │                                                      │
│           ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐       │
│  │ Koin scope created (keyed by screenNode.key)         │       │
│  │   - CoroutineScope declared                          │       │
│  │   - NavigationContainerScope declared                │       │
│  │   - Container instance created                       │       │
│  │   - Store started with coroutineScope                │       │
│  └──────────────────────────────────────────────────────┘       │
│           │                                                      │
│           ▼                                                      │
│  Screen is active (container processes intents)                  │
│           │                                                      │
│           ▼                                                      │
│  Screen removed from navigation tree                             │
│           │                                                      │
│           ▼                                                      │
│  ┌──────────────────────────────────────────────────────┐       │
│  │ ScreenNode.onDestroy callback triggered              │       │
│  │   - Koin scope closed                                │       │
│  │   - CoroutineScope cancelled                         │       │
│  │   - Container and Store cleaned up                   │       │
│  └──────────────────────────────────────────────────────┘       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Lifecycle Callbacks

The lifecycle is managed through `addOnDestroyCallback`:

```kotlin
// From NavigationContainerScope
init {
    // Cancel coroutine scope when Koin scope is closed
    scope.registerCallback(object : ScopeCallback {
        override fun onScopeClose(scope: Scope) {
            coroutineScope.cancel()
        }
    })

    // Close Koin scope when screen node is destroyed
    screenNode.addOnDestroyCallback(onDestroyCallback)
}
```

### Automatic Scope Cleanup

- **Screen-scoped**: Cleaned up when screen is popped from navigation stack
- **Container-scoped**: Cleaned up when TabNode/PaneNode is destroyed
- **CoroutineScope**: Automatically cancelled, stopping all ongoing operations

---

## State Flow Integration

### Using StateFlow from Containers

FlowMVI exposes state as a `StateFlow`, which integrates seamlessly with Compose:

```kotlin
@Composable
fun ProfileScreen() {
    val container = rememberContainer<ProfileContainer, ProfileState, ProfileIntent, ProfileAction>()
    
    // Subscribe to state and actions
    val state by container.subscribe { action ->
        // Handle one-time actions/side effects
        when (action) {
            is ProfileAction.ShowToast -> { /* show toast */ }
            is ProfileAction.ShowError -> { /* show error */ }
            // ...
        }
    }
    
    // Render based on state
    when (state) {
        is ProfileState.Loading -> LoadingScreen()
        is ProfileState.Content -> ContentScreen(state)
        is ProfileState.Error -> ErrorScreen(state.message)
    }
}
```

### Sending Intents to Containers

```kotlin
// Direct call
container.intent(ProfileIntent.LoadProfile)

// From lambda
Button(onClick = { container.intent(ProfileIntent.SaveChanges) }) {
    Text("Save")
}

// Pass as callback
ProfileContent(
    state = state,
    onIntent = container::intent
)
```

### Advanced: Observing Navigator State

Shared containers can observe navigator state for advanced use cases:

```kotlin
class StateDrivenContainer(
    scope: SharedContainerScope,
) : SharedNavigationContainer<StateDrivenState, StateDrivenIntent, StateDrivenAction>(scope) {

    override val store = store(initial = StateDrivenState()) {
        // Observe Navigator state and sync to MVI state
        whileSubscribed {
            navigator.state
                .onEach { navNode ->
                    updateState { copy(entries = extractEntriesFromNavNode(navNode)) }
                }
                .launchIn(this)
        }
        
        reduce { intent ->
            // Handle intents that manipulate navigator state
            when (intent) {
                is StateDrivenIntent.PushDestination -> navigator.navigate(intent.destination)
                is StateDrivenIntent.Pop -> navigator.navigateBack()
                is StateDrivenIntent.Reset -> navigator.navigateAndClearAll(DemoTab.Home)
                // ...
            }
        }
    }
}
```

---

## Common Patterns

### Pattern 1: Screen with Its Own Container

The most common pattern - each screen has its own MVI container:

```kotlin
// DI registration
val profileModule = module {
    single { ProfileRepository() }
    navigationContainer<ProfileContainer> { scope ->
        ProfileContainer(scope, scope.get())
    }
}

// Screen usage
@Screen(MainTabs.ProfileTab::class)
@Composable
fun ProfileScreen() {
    val store = rememberContainer<ProfileContainer, ProfileState, ProfileIntent, ProfileAction>()
    val state by store.subscribe()
    // Render UI...
}
```

### Pattern 2: Tab Wrapper with Shared Container

Provide shared state to all child screens via CompositionLocal:

```kotlin
// Container with CompositionLocal
val LocalDemoTabsStore: ProvidableCompositionLocal<Store<DemoTabsState, DemoTabsIntent, DemoTabsAction>> =
    staticCompositionLocalOf { throw IllegalStateException("No DemoTabsStore provided") }

// Wrapper provides the store
@TabsContainer(DemoTabs.Companion::class)
@Composable
fun DemoTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    val sharedStore = rememberSharedContainer<DemoTabsContainer, DemoTabsState, DemoTabsIntent, DemoTabsAction>()
    
    CompositionLocalProvider(LocalDemoTabsStore provides sharedStore) {
        Scaffold {
            content()  // Child screens can access LocalDemoTabsStore
        }
    }
}
```

### Pattern 3: Child Screens Accessing Parent's Shared State

Child screens can access shared state from parent container:

```kotlin
@Screen(DemoTabs.Music::class)
@Composable
fun MusicTabScreen() {
    // Access shared state from parent tab container
    val sharedStore = LocalDemoTabsStore.current
    val sharedState by sharedStore.subscribe()
    
    // Screen's own container
    val screenStore = rememberContainer<MusicContainer, MusicState, MusicIntent, MusicAction>()
    val state by screenStore.subscribe()
    
    Column {
        // Use shared state
        Text("Total items viewed: ${sharedState.totalItemsViewed}")
        
        // Update shared state
        Button(onClick = { sharedStore.intent(DemoTabsIntent.IncrementViewed) }) {
            Text("View Item")
        }
        
        // Use screen state
        ItemList(items = state.items)
    }
}
```

### Pattern 4: Multi-Feature Navigation with Scoped Containers

Organize containers by feature module:

```kotlin
// feature1/DI.kt
val feature1Module = module {
    navigationContainer<ResultDemoContainer> { scope ->
        ResultDemoContainer(scope)
    }
    navigationContainer<ItemPickerContainer> { scope ->
        ItemPickerContainer(scope)
    }
}

// feature2/DI.kt  
val feature2Module = module {
    navigationContainer<AnalyticsContainer> { scope ->
        AnalyticsContainer(scope, scope.get())
    }
}

// app/DI.kt - combine all modules
fun initKoin() {
    startKoin {
        modules(
            navigationModule,
            feature1Module,
            feature2Module,
            // ...
        )
    }
}
```

### Pattern 5: Navigation with Result

Use containers with `navigateForResult`:

```kotlin
class ResultDemoContainer(
    scope: NavigationContainerScope,
) : NavigationContainer<ResultDemoState, Intent, Action>(scope) {

    override val store = store(ResultDemoState()) {
        reduce { intent ->
            when (intent) {
                Intent.PickItem -> pickItem()
                // ...
            }
        }
    }

    private suspend fun Ctx.pickItem() {
        updateState { copy(isLoading = true) }
        
        // Launch in container's coroutine scope
        coroutineScope.launch {
            val result: SelectedItem? = navigator.navigateForResult(
                ResultDemoDestination.ItemPicker
            )
            
            updateState {
                copy(
                    selectedItem = result,
                    isLoading = false,
                    message = if (result != null) "Selected: ${result.name}" else "Cancelled"
                )
            }
        }
    }
}
```

---

## Testing

### Testing Containers in Isolation

FlowMVI provides testing utilities:

```kotlin
class ProfileContainerTest {
    
    private lateinit var repository: FakeProfileRepository
    private lateinit var navigator: FakeNavigator
    private lateinit var container: ProfileContainer
    
    @BeforeTest
    fun setup() {
        repository = FakeProfileRepository()
        navigator = FakeNavigator()
        
        // Create a test scope
        val testScope = TestNavigationContainerScope(
            navigator = navigator,
            screenKey = "test_profile_screen"
        )
        
        container = ProfileContainer(
            scope = testScope,
            repository = repository,
            debuggable = false
        )
    }
    
    @Test
    fun `loading profile emits loading then content state`() = runTest {
        val testUser = UserData(id = "1", name = "Test", email = "test@test.com", bio = "Bio")
        repository.setUser(testUser)
        
        container.store.test {
            // Initial state
            assertEquals(ProfileState.Loading, awaitState())
            
            // After load
            container.store.intent(ProfileIntent.LoadProfile)
            assertEquals(ProfileState.Loading, awaitState())
            assertEquals(ProfileState.Content(user = testUser), awaitState())
        }
    }
    
    @Test
    fun `navigate to settings calls navigator`() = runTest {
        container.store.test {
            container.store.intent(ProfileIntent.NavigateToSettings)
            
            assertEquals(MainTabs.SettingsTab.Main, navigator.lastDestination)
        }
    }
}
```

### Mocking Navigator for Tests

Create a `FakeNavigator` for testing:

```kotlin
class FakeNavigator : Navigator {
    var lastDestination: Destination? = null
    var backPressed = false
    
    override fun navigate(destination: Destination) {
        lastDestination = destination
    }
    
    override fun navigateBack(): Boolean {
        backPressed = true
        return true
    }
    
    override suspend fun <R> navigateForResult(destination: Destination): R? {
        lastDestination = destination
        return null
    }
    
    // ... other methods
}
```

### Testing with FakeNavigator

```kotlin
class TestNavigationContainerScope(
    override val navigator: Navigator,
    override val screenKey: String,
) : NavigationContainerScopeInterface {
    
    override val coroutineScope: CoroutineScope = TestScope()
    
    // Implement other required properties/methods
}

// Usage in test
@Test
fun `logout navigates back`() = runTest {
    val navigator = FakeNavigator()
    val scope = TestNavigationContainerScope(navigator, "test")
    val container = ProfileContainer(scope, FakeRepository())
    
    container.store.intent(ProfileIntent.Logout)
    advanceUntilIdle()
    
    assertTrue(navigator.backPressed)
}
```

### Integration Testing with Compose

```kotlin
class ProfileScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `profile screen shows loading then content`() {
        val fakeStore = FakeStore<ProfileState, ProfileIntent, ProfileAction>(
            initialState = ProfileState.Loading
        )
        
        composeTestRule.setContent {
            ProfileScreen(container = fakeStore)
        }
        
        // Verify loading
        composeTestRule.onNodeWithText("Loading profile...").assertIsDisplayed()
        
        // Emit content state
        fakeStore.emitState(ProfileState.Content(user = testUser))
        
        // Verify content
        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
    }
}
```

---

## See Also

- [FlowMVI Documentation](https://github.com/respawn-app/FlowMVI)
- [Koin Documentation](https://insert-koin.io/docs/quickstart/kotlin)
- [Navigation Architecture](ARCHITECTURE.md)
- [Navigator API](NAVIGATOR.md)
- [Navigation Nodes](NAV-NODES.md)
