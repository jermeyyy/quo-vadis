# quo-vadis-core-flow-mvi Module Implementation Progress

## Overview
FlowMVI integration module for Quo Vadis navigation library. Provides MVI pattern implementation using stores and reducers with full Compose Multiplatform support.

## Implementation Status

### ✅ Phase 1: Project Setup & Module Creation (COMPLETED)
**Files Created:**
- `quo-vadis-core-flow-mvi/build.gradle.kts` - KMP module configuration
- Updated `settings.gradle.kts` - Added module to project
- Updated `gradle/libs.versions.toml` - Added FlowMVI 3.2.0, Koin 4.0.1, AndroidX Security

**Configuration:**
- Module namespace: `com.jermey.quo.vadis.flowmvi`
- Platforms: Android, iOS (x3), JS, WasmJS, Desktop
- Dependencies: FlowMVI (core, compose, savedstate), Koin (core, compose, viewmodel)

### ✅ Phase 2: Core FlowMVI Integration (COMPLETED)
**Files Created:**

1. **NavigationContract.kt** (`core/`)
   - `NavigationState` interface - base state with currentDestination, backStackSize, canGoBack
   - `NavigationIntent` sealed interface - Navigate, NavigateBack, NavigateAndClearTo, NavigateAndReplace, NavigateAndClearAll, NavigateToGraph
   - `NavigationAction` sealed interface - ShowError, NavigationFailed, DeepLinkFailed

2. **NavigatorStore.kt** (`core/`)
   - `NavigatorContainer` class - wraps Navigator with FlowMVI store
   - Plugins: init (sync state), reduce (handle intents), recover (error handling), whileSubscribed, enableLogging
   - Auto-syncs Navigator.currentDestination flow to store state
   - Sequential intent processing (parallelIntents = false)

3. **StateManager.kt** (`utils/`)
   - `BackStack.toNavigationState()` - conversion utility
   - `NavigationState.isValid()` - state validation
   - `emptyNavigationState()`, `navigationStateOf()` - factory functions
   - `NavigationStateHistory` class - debug history tracking (max 50 entries)
   - `NavigationStateSnapshot` data class - immutable state snapshot
   - Extensions: isEquivalentTo(), copy(), toDebugString()

### ✅ Phase 3: Compose Integration (COMPLETED)
**Files Created:**

1. **NavigationStoreCompose.kt** (`compose/`)
   - `Container.subscribeNavigation()` - subscribe with action handling
   - `rememberNavigationIntentReceiver()` - stable intent receiver
   - Extensions: navigateTo(), navigateBack(), navigateAndClearTo(), navigateAndReplace(), navigateAndClearAll()

2. **StoreBasedScreen.kt** (`compose/`)
   - `StoreScreen` - standard screen pattern with state + intent receiver
   - `StoreScreenWithActions` - separate action handler
   - `StoreContent` - lightweight wrapper without action handling
   - `StoreScreenWithReceiver` - receiver as implicit scope

3. **StoreAwareNavHost.kt** (`compose/`)
   - `StoreAwareNavHost` - integrates FlowMVI with Quo Vadis NavHost
   - `StoreAwareNavHostInline` - inline content builder variant
   - `StoreAwareNavHostWithState` - explicit state access variant
   - Full predictive back support

### ✅ Phase 4: Dependency Injection (Koin) (COMPLETED)
**Files Created:**

1. **KoinIntegration.kt** (`integration/`)
   - `flowMviNavigationModule` - core module (Navigator, NavigatorContainer)
   - `featureStoreModule<S,I,A>()` - factory for feature stores
   - `featureStoresModule()` - multiple related stores
   - `storeInfrastructureModule` - shared infrastructure
   - Extensions: getContainer(), injectContainer()

2. **ContainerViewModel.kt** (`androidMain/integration/`)
   - `ContainerViewModel<S,I,A>` - Android ViewModel wrapper
   - Auto-starts store with viewModelScope
   - Closes store on onCleared()
   - `containerViewModel()` factory function

### ✅ Phase 5: State Restoration (COMPLETED)
**Files Created:**

1. **StateSerializer.kt** (`savedstate/`)
   - `SerializableNavigationState` data class - JSON-serializable state
   - `NavigationStateSerializer` interface - serialize/deserialize contract
   - `DefaultNavigationStateSerializer` - basic implementation (class names as routes)
   - Extensions: toJsonString(), fromJsonString()

2. **SavedStateManager.kt** (`savedstate/`)
   - `SavedStateManager` interface - platform-agnostic storage
   - Methods: save(), restore(), delete(), clearAll(), exists()
   - `InMemorySavedStateManager` - testing implementation
   - `expect fun createPlatformSavedStateManager()` - platform factory

3. **Platform Implementations:**
   - **AndroidSavedStateManager.kt** (`androidMain/savedstate/`)
     - Uses SharedPreferences
     - Optional encryption via EncryptedSharedPreferences
     - Survives configuration changes, process death
   
   - **IosSavedStateManager.kt** (`iosMain/savedstate/`)
     - Uses NSUserDefaults
     - Optional custom suite name
     - Survives app background/foreground
   
   - **JsSavedStateManager.kt** (`jsMain/savedstate/`)
     - Uses localStorage or sessionStorage
     - Prefix-based key namespacing
     - ~5-10MB storage limit
   
   - **WasmJsSavedStateManager.kt** (`wasmJsMain/savedstate/`)
     - Currently in-memory (TODO: WASM interop)
   
   - **DesktopSavedStateManager.kt** (`desktopMain/savedstate/`)
     - Uses Java Preferences API or file-based storage
     - Preferences: ~/.java/.userPrefs (Linux), Registry (Windows), ~/Library/Preferences (macOS)
     - File storage: ~/.quo-vadis/state/

4. **RestorationPlugin.kt** (`savedstate/`)
   - `navigationStateRestorationPlugin()` - FlowMVI plugin
   - Auto-saves on state change
   - Auto-restores on store start
   - `autoNavigationStateRestoration()` - simplified variant
   - Helpers: clearNavigationState(), hasNavigationState()

## Architecture Decisions

1. **FlowMVI Version**: Using 3.2.0 (4.0.0 not yet on Maven Central)
2. **State Synchronization**: Navigator's StateFlow → Store state updates (reactive)
3. **Intent Processing**: Sequential (not parallel) for navigation safety
4. **Error Handling**: Recover plugin emits NavigationAction.NavigationFailed
5. **Serialization**: Kotlinx.serialization with platform-specific storage
6. **Encryption**: Optional AndroidX Security for Android SharedPreferences

## Module Structure

```
quo-vadis-core-flow-mvi/
├── build.gradle.kts
├── docs/ (TODO)
└── src/
    ├── commonMain/kotlin/com/jermey/quo/vadis/flowmvi/
    │   ├── core/
    │   │   ├── NavigationContract.kt
    │   │   └── NavigatorStore.kt
    │   ├── compose/
    │   │   ├── NavigationStoreCompose.kt
    │   │   ├── StoreBasedScreen.kt
    │   │   └── StoreAwareNavHost.kt
    │   ├── integration/
    │   │   └── KoinIntegration.kt
    │   ├── savedstate/
    │   │   ├── StateSerializer.kt
    │   │   ├── SavedStateManager.kt
    │   │   └── RestorationPlugin.kt
    │   └── utils/
    │       └── StateManager.kt
    ├── androidMain/kotlin/.../
    │   ├── integration/ContainerViewModel.kt
    │   └── savedstate/AndroidSavedStateManager.kt
    ├── iosMain/kotlin/.../savedstate/
    │   └── IosSavedStateManager.kt
    ├── jsMain/kotlin/.../savedstate/
    │   └── JsSavedStateManager.kt
    ├── wasmJsMain/kotlin/.../savedstate/
    │   └── WasmJsSavedStateManager.kt
    └── desktopMain/kotlin/.../savedstate/
        └── DesktopSavedStateManager.kt
```

## Next Steps

### Phase 6: Demo Implementation (Profile Screen)
- Create ProfileContract (state, intents, actions)
- Create ProfileContainer (store with repository)
- Create ProfileScreen UI with FlowMVI patterns
- Update composeApp DemoApp with profile navigation
- Add Koin module for profile feature

### Phase 7: Testing
- Unit tests for NavigatorContainer
- Plugin behavior tests
- State serialization tests
- Platform-specific storage tests
- Integration tests

### Phase 8: Documentation
- ARCHITECTURE.md - design patterns and best practices
- API_REFERENCE.md - complete API documentation
- INTEGRATION_GUIDE.md - getting started guide
- Demo README - profile screen walkthrough
- Update project README

### Phase 9: Polish & QA
- Code review checklist
- Platform verification (all 7 targets)
- Performance testing
- Final cleanup

## Usage Examples

### Basic Setup
```kotlin
// Create container
val navigator = DefaultNavigator()
val container = NavigatorContainer(
    navigator = navigator,
    debuggable = BuildConfig.DEBUG
)

// In Compose
@Composable
fun MyScreen() {
    with(container.store) {
        val state by subscribe { action ->
            when (action) {
                is NavigationAction.ShowError -> /* handle */
            }
        }
        
        Button(onClick = { intent(NavigationIntent.Navigate(DetailsDestination)) }) {
            Text("Navigate")
        }
    }
}
```

### With Koin
```kotlin
val myModule = module {
    single<Navigator> { DefaultNavigator() }
    single { NavigatorContainer(get()) }
}

@Composable
fun MyScreen(container: NavigatorContainer = koinInject()) {
    StoreScreen(container) { state, intentReceiver ->
        // UI with state and intentReceiver
    }
}
```

### With State Restoration
```kotlin
val container = NavigatorContainer(navigator).apply {
    store {
        install(
            navigationStateRestorationPlugin(
                key = "main_nav",
                stateManager = createPlatformSavedStateManager()
            )
        )
    }
}
```

## Build Status
✅ All code compiles successfully
✅ Module builds for all platforms (Android, iOS x3, JS, Wasm, Desktop)
✅ Dependencies resolved (FlowMVI 3.2.0, Koin 4.0.1)

## Notes
- AndroidX Security requires min SDK 23, but module min SDK is 24 (compatible)
- WasmJS state persistence pending (using in-memory for now)
- Default serializer uses class names (not production-ready - implement custom for real apps)
- State restoration plugin should be installed before other plugins for proper initialization
