# Implementation Plan: quo-vadis-core-flow-mvi Module

## Executive Summary

This specification outlines the comprehensive implementation of the `quo-vadis-core-flow-mvi` module, which integrates the FlowMVI architecture framework with the Quo Vadis navigation library. The module will provide seamless MVI pattern implementation using stores and reducers (plugins) across all layers: Compose integration, dependency injection (Koin), and state restoration.

**Target**: Kotlin Multiplatform module with full platform support (Android, iOS, Desktop, JS, Wasm)  
**Framework**: FlowMVI by Respawn Team  
**Integration Point**: Quo Vadis navigation library  
**Demo**: Profile screen in composeApp

---

## High-Level Goals

1. Create new KMP module `quo-vadis-core-flow-mvi` with FlowMVI integration
2. Implement navigation-aware MVI components (stores, intents, actions, states)
3. Integrate with Compose Multiplatform navigation system
4. Add Koin dependency injection support
5. Implement state restoration for all platforms
6. Create comprehensive demo in Profile screen
7. Document architecture patterns and best practices

---

## Phase 1: Project Setup & Module Creation

### 1.1 Module Structure Creation

**Prerequisites:**
- Review FlowMVI documentation (already fetched)
- Understand Quo Vadis navigation architecture
- Plan module dependencies

**Steps:**

1. **Create module directory structure**
   ```
   quo-vadis-core-flow-mvi/
   ├── build.gradle.kts
   ├── docs/
   │   ├── ARCHITECTURE.md
   │   ├── API_REFERENCE.md
   │   └── INTEGRATION_GUIDE.md
   └── src/
       ├── commonMain/
       │   └── kotlin/com/jermey/quo/vadis/flowmvi/
       │       ├── core/           # Core FlowMVI integration
       │       ├── compose/        # Compose extensions
       │       ├── integration/    # DI integration (Koin)
       │       ├── navigation/     # Navigation-aware components
       │       ├── savedstate/     # State restoration
       │       └── utils/          # Utilities
       ├── commonTest/
       │   └── kotlin/
       ├── androidMain/
       │   └── kotlin/
       ├── iosMain/
       │   └── kotlin/
       ├── jsMain/
       │   └── kotlin/
       ├── wasmJsMain/
       │   └── kotlin/
       └── desktopMain/
           └── kotlin/
   ```

2. **Create build.gradle.kts**
   - Configure as Kotlin Multiplatform library
   - Add FlowMVI dependencies (core, compose, savedstate)
   - Add Koin dependencies (core, compose)
   - Add Compose Multiplatform dependencies
   - Depend on `:quo-vadis-core` module
   - Configure all targets (Android, iOS x3, JS, Wasm, Desktop)

3. **Update settings.gradle.kts**
   - Add `include(":quo-vadis-core-flow-mvi")`

4. **Update gradle/libs.versions.toml**
   - Add FlowMVI version (latest stable)
   - Add Koin version (latest compatible)
   - Add library declarations for:
     - `flowmvi-core`
     - `flowmvi-compose`
     - `flowmvi-savedstate`
     - `koin-core`
     - `koin-compose`

**Considerations:**
- Follow project's naming conventions
- Match Kotlin and Compose versions exactly
- Ensure all platforms are configured correctly
- FlowMVI version compatibility with Compose 1.9.0

**Expected Outcome:**
- Empty but buildable KMP module
- All dependencies resolved
- Module included in project

---

## Phase 2: Core FlowMVI Integration

### 2.1 Base Navigation Contracts

**File**: `core/NavigationContract.kt`

**Components to implement:**

1. **Base NavigationState interface**
   ```kotlin
   interface NavigationState : MVIState {
       val currentDestination: Destination?
       val backStackSize: Int
       val canGoBack: Boolean
   }
   ```

2. **Base NavigationIntent interface**
   ```kotlin
   sealed interface NavigationIntent : MVIIntent {
       data class Navigate(
           val destination: Destination,
           val transition: NavigationTransition? = null
       ) : NavigationIntent
       
       data object NavigateBack : NavigationIntent
       
       data class NavigateAndClearTo(
           val destination: Destination,
           val popUpTo: String,
           val inclusive: Boolean = false
       ) : NavigationIntent
       
       data class NavigateToGraph(
           val graphId: String,
           val startDestination: Destination
       ) : NavigationIntent
   }
   ```

3. **Base NavigationAction interface**
   ```kotlin
   sealed interface NavigationAction : MVIAction {
       data class ShowError(val message: String) : NavigationAction
       data class NavigationFailed(val error: Throwable) : NavigationAction
   }
   ```

**Considerations:**
- Type-safe destination handling
- Support for all Quo Vadis navigation operations
- Extensible for custom implementations
- Clear separation of concerns

### 2.2 Navigator Store Integration

**File**: `core/NavigatorStore.kt`

**Implementation:**

1. **NavigatorContainer class**
   - Wraps Quo Vadis Navigator
   - Implements FlowMVI Container interface
   - Manages navigation state reactively
   
2. **Store configuration**
   - Reduce plugin for intent handling
   - Recover plugin for error handling
   - WhileSubscribed plugin for lifecycle awareness
   - Logging plugin (debuggable builds only)
   - State serialization plugin

3. **Plugin setup**
   ```kotlin
   class NavigatorContainer(
       private val navigator: Navigator
   ) : Container<NavigationState, NavigationIntent, NavigationAction> {
       
       override val store = store<NavigationState, NavigationIntent, NavigationAction>(
           initial = NavigationState.Initial
       ) {
           configure {
               debuggable = BuildConfig.DEBUG
               name = "NavigatorStore"
               coroutineContext = Dispatchers.Main
           }
           
           reduce { intent ->
               when (intent) {
                   is NavigationIntent.Navigate -> handleNavigate(intent)
                   is NavigationIntent.NavigateBack -> handleNavigateBack()
                   // ... other intents
               }
           }
           
           recover { exception ->
               action(NavigationAction.NavigationFailed(exception))
               null // handled
           }
           
           whileSubscribed {
               navigator.backStack.current.collect { entry ->
                   updateState {
                       copy(
                           currentDestination = entry?.destination,
                           backStackSize = navigator.backStack.stack.value.size,
                           canGoBack = navigator.backStack.canGoBack.value
                       )
                   }
               }
           }
           
           enableLogging()
       }
   }
   ```

**Considerations:**
- Thread-safe state updates
- Proper coroutine scope management
- Error recovery strategy
- State synchronization with Navigator

### 2.3 State Management Utilities

**File**: `core/StateManager.kt`

**Components:**

1. **State transformation extensions**
   - `NavigationState.toBackStackState()`
   - `BackStackEntry.toNavigationState()`

2. **State validation**
   - Ensure state consistency
   - Validate navigation operations
   - Prevent invalid states

3. **State history tracking**
   - Optional state history for debugging
   - Time-travel debugging support

**Expected Outcome:**
- Fully functional NavigatorStore
- Type-safe navigation through MVI
- Reactive state management
- Error handling

---

## Phase 3: Compose Integration

### 3.1 Compose Extensions

**File**: `compose/NavigationStoreCompose.kt`

**Components:**

1. **Subscribe extension for navigation**
   ```kotlin
   @Composable
   fun Container<NavigationState, NavigationIntent, NavigationAction>.subscribeNavigation(
       onAction: (NavigationAction) -> Unit
   ): State<NavigationState>
   ```

2. **Navigation-aware NavHost**
   ```kotlin
   @Composable
   fun StoreAwareNavHost(
       container: Container<NavigationState, NavigationIntent, NavigationAction>,
       modifier: Modifier = Modifier,
       content: @Composable (Destination, Navigator) -> Unit
   )
   ```

3. **Intent helper composables**
   ```kotlin
   @Composable
   fun rememberNavigationIntentReceiver(
       container: Container<NavigationState, NavigationIntent, NavigationAction>
   ): IntentReceiver<NavigationIntent>
   ```

**Considerations:**
- Proper lifecycle handling
- Composition-local scope management
- Performance optimization
- Integration with existing NavHost

### 3.2 Store-based Screen Pattern

**File**: `compose/StoreBasedScreen.kt`

**Pattern implementation:**

```kotlin
@Composable
fun <S : MVIState, I : MVIIntent, A : MVIAction> StoreScreen(
    container: Container<S, I, A>,
    content: @Composable (S, IntentReceiver<I>) -> Unit
) {
    with(container.store) {
        val state by subscribe { action ->
            // Handle actions
        }
        content(state, this)
    }
}
```

**Benefits:**
- Consistent screen structure
- Automatic state subscription
- Action handling pattern
- Testability

### 3.3 Predictive Back Integration

**File**: `compose/PredictiveBackStore.kt`

**Implementation:**
- Bridge FlowMVI store with predictive back animation coordinator
- State-driven animation progress
- Store-based gesture handling
- Seamless integration with existing predictive back

**Expected Outcome:**
- Compose extensions for FlowMVI
- Navigation-aware composables
- Predictive back support
- Clean, reusable patterns

---

## Phase 4: Dependency Injection (Koin)

### 4.1 Koin Module Definition

**File**: `integration/KoinIntegration.kt`

**Components:**

1. **Core Koin module**
   ```kotlin
   val flowMviNavigationModule = module {
       // Navigator (from quo-vadis-core)
       single<Navigator> { DefaultNavigator() }
       
       // FlowMVI Container
       single<NavigatorContainer> {
           NavigatorContainer(navigator = get())
       }
       
       // Store lifecycle management
       single {
           get<NavigatorContainer>().store.start(
               scope = get<CoroutineScope>()
           )
       }
   }
   ```

2. **Feature-specific module factory**
   ```kotlin
   inline fun <reified S : MVIState, reified I : MVIIntent, reified A : MVIAction> 
   featureStoreModule(
       crossinline factory: (Navigator) -> Container<S, I, A>
   ) = module {
       single { factory(get()) }
   }
   ```

3. **Compose integration**
   ```kotlin
   @Composable
   fun <T> koinContainerViewModel(): T where T : Container<*, *, *>
   ```

**Considerations:**
- Singleton vs. factory scopes
- Lifecycle awareness
- Memory leak prevention
- Easy testing with Koin test utils

### 4.2 ViewModel Wrapper (Android)

**File**: `integration/ContainerViewModel.kt` (androidMain)

**Implementation:**
```kotlin
class ContainerViewModel<S : MVIState, I : MVIIntent, A : MVIAction>(
    private val container: Container<S, I, A>
) : ViewModel(), Container<S, I, A> by container {
    
    override val store: Store<S, I, A>
        get() = container.store.start(viewModelScope)
    
    override fun onCleared() {
        super.onCleared()
        store.close()
    }
}
```

**Benefits:**
- Android ViewModel integration
- Automatic lifecycle management
- Configuration change survival
- Process death handling

**Expected Outcome:**
- Complete Koin integration
- Easy dependency injection
- ViewModel wrappers (Android)
- Testable DI configuration

---

## Phase 5: State Restoration

### 5.1 Serialization Support

**File**: `savedstate/StateSerializer.kt`

**Components:**

1. **Serializable state wrapper**
   ```kotlin
   @Serializable
   data class SerializableNavigationState(
       val destinationRoute: String,
       val arguments: Map<String, String>,
       val backStackRoutes: List<String>
   )
   ```

2. **State conversion**
   ```kotlin
   interface NavigationStateSerializer {
       fun serialize(state: NavigationState): SerializableNavigationState
       fun deserialize(serialized: SerializableNavigationState): NavigationState
   }
   ```

3. **FlowMVI savedstate plugin integration**
   ```kotlin
   fun navigationStatePlugin(
       serializer: NavigationStateSerializer,
       stateManager: SavedStateManager
   ) = plugin<NavigationState, NavigationIntent, NavigationAction> {
       // Serialize on state change
       // Restore on start
   }
   ```

**Considerations:**
- Kotlinx.serialization usage
- Platform-specific state managers
- Partial state restoration
- Error handling for corrupted state

### 5.2 Platform-Specific Implementations

**Android** (`androidMain/savedstate/AndroidStateManager.kt`):
- Use `SavedStateHandle`
- Bundle serialization
- Process death recovery

**iOS** (`iosMain/savedstate/IosStateManager.kt`):
- Use `NSUserDefaults` or `NSCoder`
- App background/foreground handling
- State restoration on launch

**Web/Desktop** (`jsMain/wasmJsMain/desktopMain`):
- localStorage (Web)
- File-based (Desktop)
- Session storage options

### 5.3 State Restoration Plugin

**File**: `savedstate/RestorationPlugin.kt`

**Implementation:**
```kotlin
fun restorationPlugin(
    key: String,
    serializer: NavigationStateSerializer
) = plugin<NavigationState, NavigationIntent, NavigationAction> {
    name = "NavigationStateRestoration"
    
    onStart {
        // Attempt to restore state
        val restored = platformStateManager.restore(key)
        if (restored != null) {
            updateState { serializer.deserialize(restored) }
        }
    }
    
    onState { old, new ->
        // Save state on change
        launch {
            val serialized = serializer.serialize(new)
            platformStateManager.save(key, serialized)
        }
        new
    }
}
```

**Expected Outcome:**
- Full state restoration support
- All platforms covered
- Automatic save/restore
- Configurable serialization

---

## Phase 6: Demo Implementation (Profile Screen)

### 6.1 Profile Feature Contract

**File**: `composeApp/src/commonMain/.../demo/profile/ProfileContract.kt`

**Components:**

1. **Profile State**
   ```kotlin
   sealed interface ProfileState : MVIState {
       data object Loading : ProfileState
       
       data class Content(
           val user: User,
           val settings: Settings,
           val isEditing: Boolean = false
       ) : ProfileState
       
       data class Error(val message: String) : ProfileState
   }
   ```

2. **Profile Intent**
   ```kotlin
   sealed interface ProfileIntent : MVIIntent {
       data object LoadProfile : ProfileIntent
       data object EditProfile : ProfileIntent
       data class UpdateName(val name: String) : ProfileIntent
       data class UpdateEmail(val email: String) : ProfileIntent
       data object SaveChanges : ProfileIntent
       data object CancelEdit : ProfileIntent
       data object NavigateToSettings : ProfileIntent
       data object NavigateBack : ProfileIntent
   }
   ```

3. **Profile Action**
   ```kotlin
   sealed interface ProfileAction : MVIAction {
       data class ShowToast(val message: String) : ProfileAction
       data class ShowError(val error: String) : ProfileAction
   }
   ```

**Considerations:**
- Realistic user data model
- Form validation
- Navigation intents
- Side effects (toasts, dialogs)

### 6.2 Profile Container Implementation

**File**: `composeApp/.../profile/ProfileContainer.kt`

**Implementation:**
```kotlin
class ProfileContainer(
    private val navigator: Navigator,
    private val repository: ProfileRepository
) : Container<ProfileState, ProfileIntent, ProfileAction> {
    
    override val store = store(ProfileState.Loading) {
        configure {
            debuggable = BuildConfig.DEBUG
            name = "ProfileStore"
        }
        
        init {
            intent(ProfileIntent.LoadProfile)
        }
        
        reduce { intent ->
            when (intent) {
                is ProfileIntent.LoadProfile -> loadProfile()
                is ProfileIntent.EditProfile -> enableEditing()
                is ProfileIntent.UpdateName -> updateName(intent.name)
                is ProfileIntent.SaveChanges -> saveChanges()
                is ProfileIntent.NavigateToSettings -> {
                    navigator.navigate(SettingsDestination)
                }
                is ProfileIntent.NavigateBack -> {
                    navigator.navigateBack()
                }
                // ... other intents
            }
        }
        
        recover { exception ->
            updateState { ProfileState.Error(exception.message ?: "Unknown error") }
            null
        }
        
        whileSubscribed {
            // Observe external data sources
            repository.userFlow.collect { user ->
                updateState<ProfileState.Content, _> {
                    copy(user = user)
                }
            }
        }
        
        enableLogging()
    }
    
    private suspend fun PipelineContext<ProfileState, ProfileIntent, ProfileAction>.loadProfile() {
        try {
            val user = repository.getUser()
            val settings = repository.getSettings()
            updateState { 
                ProfileState.Content(user = user, settings = settings) 
            }
        } catch (e: Exception) {
            updateState { ProfileState.Error(e.message ?: "Failed to load") }
        }
    }
    
    // ... other reducer methods
}
```

**Considerations:**
- Business logic separation
- Proper error handling
- Repository pattern
- Navigation delegation

### 6.3 Profile UI Implementation

**File**: `composeApp/.../profile/ProfileScreen.kt`

**Implementation:**
```kotlin
@Composable
fun ProfileScreen(
    container: ProfileContainer = koinInject()
) {
    with(container.store) {
        val state by subscribe { action ->
            when (action) {
                is ProfileAction.ShowToast -> {
                    // Show toast
                }
                is ProfileAction.ShowError -> {
                    // Show error dialog
                }
            }
        }
        
        ProfileContent(
            state = state,
            onIntent = ::intent
        )
    }
}

@Composable
private fun IntentReceiver<ProfileIntent>.ProfileContent(
    state: ProfileState,
    modifier: Modifier = Modifier
) {
    when (state) {
        is ProfileState.Loading -> LoadingScreen()
        is ProfileState.Content -> ProfileContentLoaded(
            user = state.user,
            isEditing = state.isEditing,
            onEditClick = { intent(ProfileIntent.EditProfile) },
            onSaveClick = { intent(ProfileIntent.SaveChanges) },
            onNameChange = { intent(ProfileIntent.UpdateName(it)) },
            // ...
        )
        is ProfileState.Error -> ErrorScreen(
            message = state.message,
            onRetry = { intent(ProfileIntent.LoadProfile) }
        )
    }
}
```

**Features to demonstrate:**
1. State-driven UI
2. Loading states
3. Error handling
4. Form editing with validation
5. Navigation integration
6. Side effect handling (toasts)
7. Settings navigation (sub-screen)
8. State restoration (rotate/background)

### 6.4 Koin Module for Demo

**File**: `composeApp/.../demo/DemoKoinModule.kt`

**Update:**
```kotlin
val demoModule = module {
    // Profile feature
    single { ProfileRepository() }
    factory { ProfileContainer(navigator = get(), repository = get()) }
}
```

### 6.5 Navigation Graph Update

**File**: `composeApp/.../demo/graphs/NavigationGraphs.kt`

**Add profile destination:**
```kotlin
navigationGraph("profile") {
    startDestination(ProfileDestination)
    
    destination(ProfileDestination) { _, nav ->
        ProfileScreen()
    }
    
    destination(ProfileSettingsDestination) { _, nav ->
        ProfileSettingsScreen()
    }
}
```

**Expected Outcome:**
- Complete Profile screen implementation
- All FlowMVI features demonstrated
- Navigation integrated
- State restoration working
- Koin DI configured

---

## Phase 7: Testing

### 7.1 Unit Tests for Store

**File**: `commonTest/.../core/NavigatorContainerTest.kt`

**Test cases:**
1. Initial state verification
2. Navigation intent handling
3. Back navigation
4. Error recovery
5. State transformation
6. Plugin behavior

**Example:**
```kotlin
class NavigatorContainerTest {
    
    @Test
    fun `navigate intent updates state correctly`() = runTest {
        val fakeNavigator = FakeNavigator()
        val container = NavigatorContainer(fakeNavigator)
        
        container.store.subscribeAndTest {
            container.store.intent(NavigationIntent.Navigate(TestDestination))
            
            states.test {
                val state = awaitItem()
                assertEquals(TestDestination, state.currentDestination)
            }
        }
    }
    
    @Test
    fun `error recovery works`() = runTest {
        // Test recover plugin
    }
}
```

### 7.2 Plugin Tests

**File**: `commonTest/.../savedstate/RestorationPluginTest.kt`

**Test cases:**
1. State serialization
2. State deserialization
3. Save on state change
4. Restore on start
5. Corrupted state handling

### 7.3 Integration Tests

**File**: `androidTest/.../integration/ProfileFlowTest.kt`

**Test scenarios:**
1. Complete user flow (load → edit → save)
2. Navigation flow
3. Error handling
4. State restoration after process death

**Expected Outcome:**
- >80% code coverage
- All critical paths tested
- Plugin behavior verified
- Integration tests passing

---

## Phase 8: Documentation

### 8.1 Architecture Documentation

**File**: `quo-vadis-core-flow-mvi/docs/ARCHITECTURE.md`

**Contents:**
1. Module purpose and goals
2. FlowMVI integration approach
3. Component architecture diagram
4. State management patterns
5. Navigation integration
6. Plugin usage patterns
7. Best practices
8. Performance considerations

### 8.2 API Reference

**File**: `quo-vadis-core-flow-mvi/docs/API_REFERENCE.md`

**Contents:**
1. Core interfaces (contracts)
2. NavigatorContainer API
3. Compose extensions
4. Koin integration API
5. State restoration API
6. Custom plugin guide
7. Code examples for each API

### 8.3 Integration Guide

**File**: `quo-vadis-core-flow-mvi/docs/INTEGRATION_GUIDE.md`

**Contents:**
1. Getting started
2. Adding module to project
3. Setting up Koin
4. Creating first store
5. Compose integration
6. State restoration setup
7. Testing guide
8. Migration from plain MVI
9. Troubleshooting

### 8.4 Demo Documentation

**File**: `composeApp/src/commonMain/.../demo/profile/README.md`

**Contents:**
1. Profile screen overview
2. Features demonstrated
3. State machine diagram
4. Code walkthrough
5. Testing examples
6. Common patterns

### 8.5 Update Project README

**File**: `README.md`

**Add section:**
- FlowMVI integration module
- Link to module docs
- Quick start example
- Demo reference

**Expected Outcome:**
- Complete documentation suite
- Easy onboarding for developers
- Clear examples
- Troubleshooting guides

---

## Phase 9: Polish & Quality Assurance

### 9.1 Code Review Checklist

**Areas to review:**
1. ✅ Kotlin official style guide compliance
2. ✅ All public APIs have KDoc
3. ✅ No platform code in commonMain
4. ✅ Proper null safety
5. ✅ Thread-safe state updates
6. ✅ No memory leaks
7. ✅ Performance optimizations
8. ✅ Error handling complete
9. ✅ Tests passing (>80% coverage)
10. ✅ Documentation complete

### 9.2 Platform Verification

**Test on all platforms:**
- ✅ Android (Min SDK 24, Target SDK 36)
- ✅ iOS (Physical device + Simulator)
- ✅ Desktop (macOS, Windows, Linux)
- ✅ JavaScript (Chrome, Firefox, Safari)
- ✅ WebAssembly (Modern browsers)

**Verification steps:**
1. Build successful
2. Navigation works
3. State restoration works
4. Koin DI works
5. No crashes
6. Performance acceptable

### 9.3 Performance Testing

**Metrics to measure:**
1. Store creation time
2. Intent processing time
3. State update latency
4. Memory usage
5. Frame drops during navigation
6. State serialization time

**Optimization targets:**
- Store creation: <10ms
- Intent processing: <5ms
- State update: <1ms
- No frame drops during transitions

### 9.4 Final Cleanup

**Tasks:**
1. Remove debug code
2. Remove TODOs
3. Remove commented code
4. Organize imports
5. Format code (Kotlin official style)
6. Run detekt
7. Fix all warnings
8. Update dependencies to latest stable

**Expected Outcome:**
- Production-ready code
- All platforms verified
- Performance optimized
- Clean codebase

---

## Dependencies Summary

### FlowMVI Dependencies
```toml
[versions]
flowmvi = "4.0.0"  # Latest stable

[libraries]
flowmvi-core = { module = "pro.respawn.flowmvi:core", version.ref = "flowmvi" }
flowmvi-compose = { module = "pro.respawn.flowmvi:compose", version.ref = "flowmvi" }
flowmvi-savedstate = { module = "pro.respawn.flowmvi:savedstate", version.ref = "flowmvi" }
flowmvi-test = { module = "pro.respawn.flowmvi:test", version.ref = "flowmvi" }
```

### Koin Dependencies
```toml
[versions]
koin = "4.0.1"  # Latest compatible with KMP

[libraries]
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-test = { module = "io.insert-koin:koin-test", version.ref = "koin" }
```

### Internal Dependencies
- `quo-vadis-core` (navigation library)
- Compose Multiplatform 1.9.0
- Kotlin 2.2.20
- Kotlinx Serialization

---

## Success Criteria

### Phase Completion Criteria

**Phase 1 (Setup):**
- ✅ Module builds successfully
- ✅ All dependencies resolved
- ✅ Empty module structure created

**Phase 2 (Core):**
- ✅ NavigatorContainer implemented
- ✅ All navigation intents supported
- ✅ State management working
- ✅ Error handling functional

**Phase 3 (Compose):**
- ✅ Compose extensions created
- ✅ StoreAwareNavHost working
- ✅ Predictive back integrated

**Phase 4 (DI):**
- ✅ Koin modules defined
- ✅ Dependency injection working
- ✅ ViewModel wrappers (Android)

**Phase 5 (State):**
- ✅ State serialization working
- ✅ All platforms support restoration
- ✅ Automatic save/restore

**Phase 6 (Demo):**
- ✅ Profile screen complete
- ✅ All features demonstrated
- ✅ Navigation working
- ✅ State restoration working

**Phase 7 (Testing):**
- ✅ >80% code coverage
- ✅ All tests passing
- ✅ Integration tests working

**Phase 8 (Docs):**
- ✅ Architecture documented
- ✅ API reference complete
- ✅ Integration guide ready

**Phase 9 (Polish):**
- ✅ Code review passed
- ✅ All platforms verified
- ✅ Performance acceptable
- ✅ Production-ready

### Overall Success Metrics

1. **Functionality**: All FlowMVI features integrated with navigation
2. **Quality**: >80% test coverage, no critical bugs
3. **Performance**: No perceptible lag, smooth animations
4. **Documentation**: Complete and clear
5. **Maintainability**: Clean architecture, well-structured code
6. **Reusability**: Easy to integrate in new features

---

## Risk Assessment & Mitigation

### Technical Risks

**Risk 1: FlowMVI version compatibility**
- **Impact**: High
- **Probability**: Medium
- **Mitigation**: Use latest stable version, test thoroughly, pin versions

**Risk 2: Platform-specific state restoration issues**
- **Impact**: Medium
- **Probability**: Medium
- **Mitigation**: Test on all platforms early, provide fallback mechanisms

**Risk 3: Performance degradation**
- **Impact**: Medium
- **Probability**: Low
- **Mitigation**: Benchmark early, optimize hot paths, use GPU acceleration

**Risk 4: Complex plugin interactions**
- **Impact**: Medium
- **Probability**: Medium
- **Mitigation**: Follow FlowMVI plugin ordering rules, extensive testing

### Integration Risks

**Risk 5: Koin lifecycle conflicts**
- **Impact**: Medium
- **Probability**: Low
- **Mitigation**: Use proper scopes, test configuration changes

**Risk 6: Navigation state synchronization**
- **Impact**: High
- **Probability**: Low
- **Mitigation**: Use reactive streams, SSTs for consistency

### Timeline Risks

**Risk 7: Underestimated complexity**
- **Impact**: Medium
- **Probability**: Medium
- **Mitigation**: Break into smaller phases, iterate quickly

---

## Implementation Timeline

### Estimated Effort (Development Days)

- **Phase 1**: 1 day (setup)
- **Phase 2**: 3 days (core implementation)
- **Phase 3**: 2 days (Compose integration)
- **Phase 4**: 2 days (Koin integration)
- **Phase 5**: 3 days (state restoration)
- **Phase 6**: 3 days (demo implementation)
- **Phase 7**: 2 days (testing)
- **Phase 8**: 2 days (documentation)
- **Phase 9**: 2 days (polish & QA)

**Total**: ~20 development days (4 weeks)

### Milestones

- **Week 1**: Phases 1-2 complete (Core working)
- **Week 2**: Phases 3-4 complete (Full integration)
- **Week 3**: Phases 5-6 complete (Demo ready)
- **Week 4**: Phases 7-9 complete (Production-ready)

---

## Next Steps (Post-Implementation)

1. **Publishing**
   - Prepare Maven Central publishing
   - Version tagging
   - Release notes

2. **Additional Features**
   - Advanced plugin examples
   - More demo screens
   - Performance profiling tools

3. **Community**
   - Open-source contribution guide
   - Sample projects
   - Blog post / article

4. **Maintenance**
   - Update to new FlowMVI versions
   - Bug fixes
   - Feature requests

---

## References

### FlowMVI Documentation
- Main site: https://opensource.respawn.pro/FlowMVI/
- Quickstart: https://opensource.respawn.pro/FlowMVI/quickstart
- Plugins (prebuilt): https://opensource.respawn.pro/FlowMVI/plugins/prebuilt
- Custom plugins: https://opensource.respawn.pro/FlowMVI/plugins/custom
- Plugin delegates: https://opensource.respawn.pro/FlowMVI/plugins/delegates
- State management: https://opensource.respawn.pro/FlowMVI/state/statemanagement

### Project Documentation
- Project overview memory: `.serena/memories/project_overview.md`
- Architecture patterns memory: `.serena/memories/architecture_patterns.md`
- Codebase structure memory: `.serena/memories/codebase_structure.md`
- Tech stack memory: `.serena/memories/tech_stack.md`
- Quo Vadis docs: `quo-vadis-core/docs/`

### External Resources
- Koin documentation: https://insert-koin.io/
- Compose Multiplatform: https://www.jetbrains.com/compose-multiplatform/
- Kotlin Serialization: https://github.com/Kotlin/kotlinx.serialization

---

## Appendix A: Key FlowMVI Concepts

### Store
Central state container with plugins that form a pipeline. Handles:
- State updates (via `updateState`)
- Intent processing (via `reduce`)
- Side effects (via `action`)
- Lifecycle management

### Plugins
Modular pieces of logic that intercept events:
- `reduce` - Process intents
- `init` - Initialize on start
- `recover` - Handle errors
- `whileSubscribed` - Run while UI subscribes
- `enableLogging` - Debug logging
- Custom plugins for any cross-cutting concern

### Pipeline
Chain of responsibility pattern where plugins execute in order. **Order matters!**

### Serialized State Transactions (SSTs)
Thread-safe state updates using mutex. Prevents data races.

### Container
Wrapper class holding the Store and providing context/dependencies.

---

## Appendix B: Code Examples

### Basic Store Creation
```kotlin
val store = store<MyState, MyIntent, MyAction>(initial = MyState.Initial) {
    configure {
        debuggable = true
        name = "MyStore"
    }
    
    reduce { intent ->
        when (intent) {
            is MyIntent.DoSomething -> handleSomething()
        }
    }
}
```

### Compose Integration
```kotlin
@Composable
fun MyScreen(container: MyContainer = koinInject()) {
    with(container.store) {
        val state by subscribe { action ->
            // Handle actions
        }
        
        MyContent(state = state, onIntent = ::intent)
    }
}
```

### Custom Plugin
```kotlin
fun analyticsPlugin(analytics: Analytics) = plugin<State, Intent, Action> {
    onIntent { intent ->
        analytics.log(intent.name)
        intent // pass through
    }
}
```

---

## Appendix C: FlowMVI vs Plain MVI Comparison

| Aspect | Plain MVI | FlowMVI |
|--------|-----------|---------|
| State Management | Manual StateFlow/ViewModel | Store with SSTs |
| Intent Handling | Manual when/switch | Reduce plugin |
| Error Handling | Try-catch everywhere | Recover plugin |
| Side Effects | Manual handling | Action system |
| Lifecycle | ViewModel/manual | WhileSubscribed plugin |
| Testing | Mock ViewModels | Store DSL + TimeTravel |
| Reusability | Low (ViewModel coupling) | High (Container pattern) |
| Thread Safety | Manual mutex | Built-in SSTs |
| Debugging | Manual logging | Logging plugin + IDE plugin |

---

## Approval & Sign-off

This specification should be reviewed and approved before implementation begins.

**Review Checklist:**
- [ ] Technical approach validated
- [ ] Dependencies verified
- [ ] Timeline realistic
- [ ] Success criteria clear
- [ ] Risks identified
- [ ] Documentation plan adequate
- [ ] Demo scope appropriate
