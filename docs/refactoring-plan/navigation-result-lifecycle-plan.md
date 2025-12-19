# Navigation with Result and NavigationLifecycle - Implementation Plan

## Overview

This document outlines the implementation plan for adding navigation result passing and lifecycle management APIs to the Quo Vadis navigation library. The goal is to enable MVI-style containers (business logic holders) to navigate to destinations and await results, with proper lifecycle tracking.

## Requirements Summary

Based on analysis and user clarifications:

1. **Compile-time type safety** for navigation results via explicit result type declaration
2. **Per-instance lifecycle tracking** using ScreenNode keys (unique per navigation instance)
3. **Add to `quo-vadis-core`** module (available to all users)
4. **Navigator independence from Compose** - injectable via DI, no `@Composable` API requirement
5. **Main thread execution** for navigation, coroutine-based result awaiting
6. **Nullable result type** (`R?`) - returns `null` when user navigates back without result

## API Design

### 1. Result-Returning Destination Interface

```kotlin
/**
 * Marker interface for destinations that can return a result.
 * Provides compile-time type safety for navigation result passing.
 *
 * @param R The type of result this destination returns
 */
interface ReturnsResult<R : Any>

// Example usage:
@Destination
data class SomeDestination(
    val someParams: String
) : Destination, ReturnsResult<SomeResult>

data class SomeResult(val data: String)
```

### 2. Navigator Extensions for Result-Based Navigation

```kotlin
/**
 * Navigate to a destination and suspend until it returns a result.
 *
 * The navigation is performed on the main thread. The calling coroutine
 * suspends until the destination calls [navigateBackWithResult].
 *
 * @param R The expected result type (enforced by ReturnsResult<R>)
 * @param destination The destination to navigate to (must implement ReturnsResult<R>)
 * @return The result value, or null if navigation was cancelled (user pressed back without result)
 */
suspend fun <R : Any> Navigator.navigateForResult(
    destination: Destination & ReturnsResult<R>
): R?

/**
 * Navigate back and pass a result to the previous destination.
 *
 * Must be called from a destination that was navigated to via [navigateForResult].
 * The result will be delivered to the suspended coroutine in the calling screen.
 *
 * @param R The result type
 * @param result The result to pass back
 * @throws IllegalStateException if no result request is pending for current screen
 */
fun <R : Any> Navigator.navigateBackWithResult(result: R)
```

### 3. NavigationLifecycle Interface

```kotlin
/**
 * Lifecycle callbacks for navigation-aware components.
 *
 * These callbacks are tied to a specific ScreenNode instance (identified by key),
 * allowing proper lifecycle management even with multiple instances of the same
 * destination type.
 */
interface NavigationLifecycle {
    /**
     * Called when the navigation to this screen completes (screen becomes active).
     * This is called after the navigation animation finishes.
     */
    fun onEnter()

    /**
     * Called when navigating away from this screen (screen becomes inactive).
     * This is called when another screen becomes active but this screen
     * remains in the navigation stack.
     */
    fun onExit()

    /**
     * Called when this screen is being removed from the navigation stack.
     * This is the final lifecycle callback - after this, the screen is destroyed.
     * Use this to clean up resources (cancel coroutines, unregister listeners, etc.)
     */
    fun onDestroy()
}
```

### 4. Lifecycle Registration on Navigator

```kotlin
/**
 * Register lifecycle callbacks for the current active screen.
 *
 * The Navigator automatically associates the lifecycle with the currently
 * active ScreenNode. This should be called during container initialization,
 * when the screen is already active.
 *
 * Internally, the Navigator uses the current screen's key to track the
 * lifecycle. The user doesn't need to know or manage screen keys.
 *
 * @param lifecycle The lifecycle callbacks to register
 * @throws IllegalStateException if no active screen exists
 */
fun Navigator.registerNavigationLifecycle(lifecycle: NavigationLifecycle)

/**
 * Unregister lifecycle callbacks.
 *
 * The Navigator finds and removes the lifecycle from its registered screen.
 * Safe to call even if the lifecycle was never registered (no-op).
 *
 * @param lifecycle The lifecycle callbacks to unregister
 */
fun Navigator.unregisterNavigationLifecycle(lifecycle: NavigationLifecycle)
```

**Internal Implementation Note:**
The `NavigationLifecycleManager` maintains a bidirectional mapping:
- `lifecycleToScreen: Map<NavigationLifecycle, String>` - tracks which screen each lifecycle belongs to
- `screenToLifecycles: Map<String, MutableSet<NavigationLifecycle>>` - tracks all lifecycles for each screen

When `registerNavigationLifecycle(lifecycle)` is called:
1. Get current active screen key via `state.value.activeLeaf()?.key`
2. Store mapping: `lifecycle -> screenKey` and `screenKey -> lifecycle`
3. Dispatch `onEnter()` immediately (screen is already active)

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         TreeNavigator                                │
│                                                                      │
│  ┌─────────────────────┐    ┌─────────────────────────────────────┐ │
│  │   NavigationState   │    │     NavigationResultManager         │ │
│  │   (StateFlow)       │    │                                     │ │
│  │                     │    │  - pendingResults: Map<screenKey,   │ │
│  │  - NavNode tree     │    │      CompletableDeferred<Any?>>     │ │
│  │  - Active screen    │    │                                     │ │
│  └─────────────────────┘    │  - requestResult(key): Deferred     │ │
│                             │  - completeResult(key, value)       │ │
│                             │  - cancelResult(key)                │ │
│                             └─────────────────────────────────────┘ │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │               NavigationLifecycleManager                         ││
│  │                                                                  ││
│  │  - lifecycleToScreen: Map<NavigationLifecycle, String>          ││
│  │  - screenToLifecycles: Map<String, Set<NavigationLifecycle>>    ││
│  │                                                                  ││
│  │  - register(lifecycle) → auto-associates with current screen    ││
│  │  - unregister(lifecycle) → removes from tracking                ││
│  │  - observes state changes and dispatches callbacks              ││
│  └─────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                     Business Logic Container                         │
│                     (e.g., FooContainer)                            │
│                                                                      │
│  - Has injected Navigator instance                                  │
│  - Has its own CoroutineScope                                       │
│  - Implements NavigationLifecycle                                   │
│  - Calls navigator.registerNavigationLifecycle(this) in init       │
│  - Calls navigator.navigateForResult<R>(destination)               │
│  - Result awaited on container's coroutineScope                    │
└─────────────────────────────────────────────────────────────────────┘
```

### Sequence Diagram: Navigate for Result

```
FooContainer           Navigator              ResultManager           SomeContainer
     │                     │                       │                       │
     │ navigateForResult() │                       │                       │
     │────────────────────>│                       │                       │
     │                     │ registerPending(key)  │                       │
     │                     │──────────────────────>│                       │
     │                     │                       │                       │
     │                     │ navigate(destination) │                       │
     │                     │──────(main thread)────│                       │
     │                     │                       │                       │
     │    [suspends]       │                       │                       │
     │                     │                       │   onEnter()           │
     │                     │───────────────────────│──────────────────────>│
     │                     │                       │                       │
     │                     │                       │                       │
     │                     │                       │ navigateBackWithResult│
     │                     │<──────────────────────│───────────────────────│
     │                     │                       │                       │
     │                     │ completeResult(key,v) │                       │
     │                     │──────────────────────>│                       │
     │                     │                       │                       │
     │   result returned   │                       │                       │
     │<────────────────────│                       │                       │
```

### Sequence Diagram: Back Without Result

```
FooContainer           Navigator              ResultManager           User
     │                     │                       │                    │
     │ navigateForResult() │                       │                    │
     │────────────────────>│                       │                    │
     │                     │ registerPending(key)  │                    │
     │                     │──────────────────────>│                    │
     │    [suspends]       │                       │                    │
     │                     │                       │                    │
     │                     │                       │  [presses back]    │
     │                     │<──────────────────────│────────────────────│
     │                     │                       │                    │
     │                     │ cancelResult(key)     │                    │
     │                     │──────────────────────>│                    │
     │                     │                       │                    │
     │   returns null      │                       │                    │
     │<────────────────────│                       │                    │
```

## Implementation Plan

### Phase 1: Core Interfaces and Types

**Files to create:**
- `quo-vadis-core/.../core/ReturnsResult.kt` - Result type marker interface
- `quo-vadis-core/.../core/NavigationLifecycle.kt` - Lifecycle callback interface

**Tasks:**
1. Create `ReturnsResult<R>` interface
2. Create `NavigationLifecycle` interface with `onEnter()`, `onExit()`, `onDestroy()` methods
3. Add KDoc documentation

### Phase 2: Navigation Result Manager

**Files to create:**
- `quo-vadis-core/.../core/NavigationResultManager.kt` - Result tracking and delivery

**Tasks:**
1. Create `NavigationResultManager` class:
   - Thread-safe pending results map: `Map<String, CompletableDeferred<Any?>>`
   - `registerPendingResult(fromScreenKey, toScreenKey): Deferred<Any?>`
   - `completeResult(screenKey, value)`
   - `cancelResult(screenKey)`
   - `hasPendingResult(screenKey): Boolean`

2. Handle edge cases:
   - Multiple pending results from same screen
   - Result type validation
   - Memory cleanup on completion/cancellation

### Phase 3: Navigation Lifecycle Manager

**Files to create:**
- `quo-vadis-core/.../core/NavigationLifecycleManager.kt` - Lifecycle dispatch coordination

**Tasks:**
1. Create `NavigationLifecycleManager` class:
   - Bidirectional mapping for lifecycle tracking:
     - `lifecycleToScreen: MutableMap<NavigationLifecycle, String>` - lifecycle → screenKey
     - `screenToLifecycles: MutableMap<String, MutableSet<NavigationLifecycle>>` - screenKey → lifecycles
   - `register(lifecycle)` - associates with current active screen
   - `unregister(lifecycle)` - removes lifecycle from tracking
   - Observes `Navigator.state` to detect:
     - New active screen → call `onEnter()` for new screen's lifecycles, `onExit()` for previous
     - Screen removal → call `onDestroy()` and cleanup mappings

2. Ensure callbacks are dispatched on main thread
3. Handle edge case: `register()` called when no active screen exists (throw IllegalStateException)

### Phase 4: Navigator Extensions

**Files to modify:**
- `quo-vadis-core/.../core/Navigator.kt` - Add result/lifecycle interfaces
- `quo-vadis-core/.../core/TreeNavigator.kt` - Implement managers integration

**Tasks:**
1. Add to `Navigator` interface:
   - `val resultManager: NavigationResultManager`
   - `val lifecycleManager: NavigationLifecycleManager`

3. Create extension functions in a new file `quo-vadis-core/.../core/NavigatorResultExtensions.kt`:
   ```kotlin
   suspend fun <R : Any> Navigator.navigateForResult(
       destination: Destination & ReturnsResult<R>
   ): R?

   fun <R : Any> Navigator.navigateBackWithResult(result: R)
   ```

4. Create extension functions for lifecycle in `NavigatorLifecycleExtensions.kt`:
   ```kotlin
   fun Navigator.registerNavigationLifecycle(lifecycle: NavigationLifecycle)
   fun Navigator.unregisterNavigationLifecycle(lifecycle: NavigationLifecycle)
   ```

4. Modify `TreeNavigator`:
   - Initialize `NavigationResultManager` and `NavigationLifecycleManager`
   - Hook lifecycle manager to state observations
   - Cancel pending results on back navigation (in `handleBackInternal()`)

### Phase 5: Thread Safety and Main Dispatcher

**Tasks:**
1. Ensure all navigation operations run on `Dispatchers.Main.immediate`
2. Result completion uses `withContext(Dispatchers.Main)` to ensure delivery on main thread
3. Lifecycle callbacks dispatched on main thread
4. Coroutine suspension/resumption is thread-safe

### Phase 6: FakeNavigator Updates

**Files to modify:**
- `quo-vadis-core/.../testing/FakeNavigator.kt`

**Tasks:**
1. Add test implementations of `NavigationResultManager` and `NavigationLifecycleManager`
2. Add methods for simulating result returns in tests:
   ```kotlin
   fun simulateResult(screenKey: String, result: Any?)
   fun verifyPendingResult(screenKey: String): Boolean
   ```

### Phase 7: Demo App Integration

**Files to create/modify in `composeApp`:**
- `composeApp/src/commonMain/kotlin/.../container/BaseContainer.kt` - Base container with lifecycle
- `composeApp/src/commonMain/kotlin/.../container/ResultDemoContainer.kt` - Example with result navigation
- `composeApp/src/commonMain/kotlin/.../destination/ResultDemoDestinations.kt` - Destinations with result types
- `composeApp/src/commonMain/kotlin/.../screen/ResultDemoScreen.kt` - UI for demo screens

**Tasks:**

1. **Create BaseContainer class:**
   ```kotlin
   abstract class BaseContainer<S, D : Destination>(
       protected val navigator: Navigator,
       protected val coroutineScope: CoroutineScope,
   ) : NavigationLifecycle {
   
       init {
           navigator.registerNavigationLifecycle(this)
       }
   
       override fun onEnter() {
           // Override in subclasses if needed
       }
   
       override fun onExit() {
           // Override in subclasses if needed
       }
   
       override fun onDestroy() {
           coroutineScope.cancel()
           navigator.unregisterNavigationLifecycle(this)
       }
   }
   ```

2. **Create example destinations with result types:**
   ```kotlin
   // Result type
   data class ItemPickerResult(
       val selectedItemId: String,
       val selectedItemName: String
   )
   
   // Destination that returns a result
   @Destination
   data class ItemPickerDestination(
       val availableItems: List<String>
   ) : Destination, ReturnsResult<ItemPickerResult>
   
   // Destination that requests a result
   @Destination
   data object ItemSelectionDemoDestination : Destination
   ```

3. **Create demo container that navigates for result:**
   ```kotlin
   class ItemSelectionDemoContainer(
       navigator: Navigator,
       coroutineScope: CoroutineScope,
   ) : BaseContainer<ItemSelectionState, ItemSelectionDemoDestination>(navigator, coroutineScope) {
   
       private val _state = MutableStateFlow(ItemSelectionState())
       val state: StateFlow<ItemSelectionState> = _state.asStateFlow()
   
       fun onSelectItemClicked() {
           coroutineScope.launch {
               _state.update { it.copy(isLoading = true) }
               
               val result: ItemPickerResult? = navigator.navigateForResult(
                   ItemPickerDestination(availableItems = listOf("Item A", "Item B", "Item C"))
               )
               
               _state.update { 
                   it.copy(
                       isLoading = false,
                       selectedItem = result?.selectedItemName,
                       message = if (result != null) "Selected: ${result.selectedItemName}" else "Selection cancelled"
                   )
               }
           }
       }
   
       override fun onEnter() {
           println("ItemSelectionDemoContainer: onEnter")
       }
   
       override fun onExit() {
           println("ItemSelectionDemoContainer: onExit")
       }
   }
   
   data class ItemSelectionState(
       val isLoading: Boolean = false,
       val selectedItem: String? = null,
       val message: String = "No item selected"
   )
   ```

4. **Create container that returns result:**
   ```kotlin
   class ItemPickerContainer(
       navigator: Navigator,
       coroutineScope: CoroutineScope,
       private val availableItems: List<String>
   ) : BaseContainer<ItemPickerState, ItemPickerDestination>(navigator, coroutineScope) {
   
       private val _state = MutableStateFlow(ItemPickerState(items = availableItems))
       val state: StateFlow<ItemPickerState> = _state.asStateFlow()
   
       fun onItemSelected(itemId: String, itemName: String) {
           navigator.navigateBackWithResult(
               ItemPickerResult(selectedItemId = itemId, selectedItemName = itemName)
           )
       }
   
       fun onCancelClicked() {
           navigator.navigateBack() // No result - caller receives null
       }
   }
   
   data class ItemPickerState(
       val items: List<String> = emptyList()
   )
   ```

5. **Create demo screens (Composables):**
   ```kotlin
   @Composable
   fun ItemSelectionDemoScreen(container: ItemSelectionDemoContainer) {
       val state by container.state.collectAsState()
       
       Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
           Text("Navigation Result Demo", style = MaterialTheme.typography.headlineMedium)
           
           Spacer(modifier = Modifier.height(16.dp))
           
           Text(state.message)
           
           Spacer(modifier = Modifier.height(16.dp))
           
           Button(
               onClick = { container.onSelectItemClicked() },
               enabled = !state.isLoading
           ) {
               if (state.isLoading) {
                   CircularProgressIndicator(modifier = Modifier.size(16.dp))
               } else {
                   Text("Select Item")
               }
           }
       }
   }
   
   @Composable
   fun ItemPickerScreen(container: ItemPickerContainer) {
       val state by container.state.collectAsState()
       
       Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
           Text("Pick an Item", style = MaterialTheme.typography.headlineMedium)
           
           Spacer(modifier = Modifier.height(16.dp))
           
           state.items.forEachIndexed { index, item ->
               Card(
                   modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                   onClick = { container.onItemSelected(index.toString(), item) }
               ) {
                   Text(item, modifier = Modifier.padding(16.dp))
               }
           }
           
           Spacer(modifier = Modifier.height(16.dp))
           
           TextButton(onClick = { container.onCancelClicked() }) {
               Text("Cancel")
           }
       }
   }
   ```

6. **Register screens and add navigation entry point:**
   - Register `ItemSelectionDemoDestination` and `ItemPickerDestination` in screen registry
   - Add demo entry in the app's main navigation (e.g., in demos list)
   - Wire up DI for container creation (Koin modules)

7. **Add Koin module for demo containers:**
   ```kotlin
   val resultDemoModule = module {
       factory { (navigator: Navigator, scope: CoroutineScope) ->
           ItemSelectionDemoContainer(navigator, scope)
       }
       factory { (navigator: Navigator, scope: CoroutineScope, items: List<String>) ->
           ItemPickerContainer(navigator, scope, items)
       }
   }
   ```

### Phase 8: Documentation and Examples

**Tasks:**
1. Update API documentation
2. Add usage guide in README
3. Document lifecycle callbacks behavior
4. Add migration guide if needed
5. Add inline code examples in KDoc

## File Changes Summary

### New Files

| File | Purpose |
|------|---------|
| `quo-vadis-core/.../core/ReturnsResult.kt` | Result type marker interface |
| `quo-vadis-core/.../core/NavigationLifecycle.kt` | Lifecycle callback interface |
| `quo-vadis-core/.../core/NavigationResultManager.kt` | Pending result tracking |
| `quo-vadis-core/.../core/NavigationLifecycleManager.kt` | Lifecycle dispatch coordination |
| `quo-vadis-core/.../core/NavigatorResultExtensions.kt` | Navigate for result extensions |
| `quo-vadis-core/.../core/NavigatorLifecycleExtensions.kt` | Lifecycle registration extensions |

### Modified Files

| File | Changes |
|------|---------|
| `quo-vadis-core/.../core/Navigator.kt` | Add result manager, lifecycle manager |
| `quo-vadis-core/.../core/TreeNavigator.kt` | Initialize managers, hook to state changes, cancel results on back |
| `quo-vadis-core/.../testing/FakeNavigator.kt` | Add fake manager implementations |

### Demo App Files (composeApp)

| File | Purpose |
|------|---------|
| `.../container/BaseContainer.kt` | Abstract base container with lifecycle integration |
| `.../container/ResultDemoContainer.kt` | Demo containers (ItemSelectionDemoContainer, ItemPickerContainer) |
| `.../destination/ResultDemoDestinations.kt` | Demo destinations with result types |
| `.../screen/ResultDemoScreen.kt` | Demo UI screens |
| `.../di/ResultDemoModule.kt` | Koin module for demo containers |
| `.../navigation/AppNavigation.kt` | Register demo destinations and screens |

## Testing Strategy

### Unit Tests

1. **NavigationResultManager tests:**
   - Register pending result
   - Complete result with correct type
   - Cancel result returns null
   - Multiple pending results
   - Memory cleanup after completion

2. **NavigationLifecycleManager tests:**
   - Register lifecycle associates with current screen
   - Unregister lifecycle removes from tracking
   - onEnter called on navigation
   - onExit called on navigate away
   - onDestroy called on removal
   - Multiple lifecycles per screen
   - Register without active screen throws exception

3. **Navigator extension tests:**
   - navigateForResult success flow
   - navigateForResult cancellation flow
   - navigateBackWithResult delivery
   - Type safety enforcement

### Integration Tests

1. Full navigation flow with result
2. Back navigation cancellation
3. Multiple screens with results
4. Lifecycle with result combination

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Thread safety issues | High | Use `Dispatchers.Main.immediate`, atomic operations |
| Memory leaks from orphaned results | Medium | Automatic cleanup on screen removal |
| Breaking existing Navigator API | High | Additive changes only, no breaking changes |
| Type erasure issues with generics | Medium | Runtime type checks with clear error messages |
| Complex lifecycle edge cases | Medium | Comprehensive test coverage |

## Alternatives Considered

### 1. Callback-based API instead of suspend
- **Pros:** Simpler, no coroutine dependency
- **Cons:** Less idiomatic Kotlin, callback hell potential
- **Decision:** Rejected - suspend functions are more modern and composable

### 2. Result as sealed class (Success/Cancelled/Failed)
- **Pros:** More explicit about all outcomes
- **Cons:** More verbose for simple cases
- **Decision:** Rejected - nullable return (`R?`) is simpler and sufficient

### 3. Lifecycle via annotation processing
- **Pros:** Less boilerplate
- **Cons:** More complex, harder to customize
- **Decision:** Rejected - explicit registration is more flexible

## Open Questions

1. **Should lifecycle callbacks support async operations?** 
   - Current design: All callbacks are synchronous
   - Alternative: Make callbacks suspend functions
   - Recommendation: Keep synchronous, users can launch coroutines if needed

2. **Should multiple lifecycles be allowed per screen key?**
   - Current design: Yes, Set<NavigationLifecycle> per key
   - Use case: Multiple observers for same screen
   - Recommendation: Keep as-is for flexibility

3. **Should result type be validated at runtime?**
   - Current design: Trust generic type safety
   - Alternative: Check instance type and throw if mismatch
   - Recommendation: Add runtime check with clear error message

## Timeline Estimate

| Phase | Effort | Dependencies |
|-------|--------|--------------|
| Phase 1: Interfaces | 2-3 hours | None |
| Phase 2: Result Manager | 4-6 hours | Phase 1 |
| Phase 3: Lifecycle Manager | 4-6 hours | Phase 1 |
| Phase 4: Navigator Extensions | 4-6 hours | Phase 2, 3 |
| Phase 5: Thread Safety | 2-3 hours | Phase 4 |
| Phase 6: FakeNavigator | 2-3 hours | Phase 4 |
| Phase 7: Demo App Integration | 6-8 hours | Phase 6 |
| Phase 8: Documentation | 3-4 hours | Phase 7 |

**Total: ~27-39 hours**

## Conclusion

This implementation plan provides a type-safe, lifecycle-aware navigation result API that:
- Enables MVI containers to navigate and await results
- Provides proper lifecycle callbacks for resource management  
- Maintains thread safety with main thread navigation execution
- Follows Kotlin idioms with suspend functions and nullable return types
- Is backward compatible with existing Navigator API
- Is testable via FakeNavigator

The design prioritizes simplicity and explicitness while providing the flexibility needed for complex navigation patterns.
