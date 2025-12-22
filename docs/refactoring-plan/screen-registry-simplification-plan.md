# Screen Registry Simplification Plan

## Problem Statement

The current KSP-generated `NavigationConfig.screenRegistry` implementation has two issues with the `Content()` function's `when` expression:

### Issue 1: Navigator Parameter is Unnecessary

**Current generated code:**
```kotlin
override val screenRegistry = object : ScreenRegistry {
    @Composable
    override fun Content(
        destination: NavDestination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        when (destination) {
            is MainTabs.HomeTab -> HomeScreen(navigator = navigator)  // ❌ navigator passed
            is MasterDetailDestination.Detail -> DetailScreen(
                destination = destination as MasterDetailDestination.Detail,  // ❌ unnecessary cast
                navigator = navigator  // ❌ navigator passed
            )
            else -> error("No screen registered for destination: $destination")
        }
    }
}
```

**Problem:**
- `navigator` is passed to screen composables from the registry
- Screen composables typically get `navigator` via DI (e.g., `koinInject()`)
- The navigator has a single instance created externally and provided by the user's DI mechanism
- Passing `navigator` explicitly couples screen registration to a specific navigator instance
- This is inconsistent with the DI pattern used throughout the codebase

**Evidence from existing screens:**
```kotlin
// HomeScreen.kt
@Screen(MainTabs.HomeTab::class)
@Composable
fun HomeScreen(
    navigator: Navigator = koinInject(),  // Gets navigator from DI
    modifier: Modifier = Modifier
)

// DetailScreen.kt
@Screen(MasterDetailDestination.Detail::class)
@Composable
fun DetailScreen(
    destination: MasterDetailDestination.Detail,
    navigator: Navigator = koinInject()  // Gets navigator from DI
)
```

### Issue 2: Destination Casting is Unnecessary

**Current generated code:**
```kotlin
is MasterDetailDestination.Detail -> DetailScreen(
    destination = destination as MasterDetailDestination.Detail,  // ❌ Explicit cast
    navigator = navigator
)
```

**Problem:**
- After the smart cast in `when (destination)`, the compiler already knows `destination` is the correct type
- The explicit `as` cast is redundant and adds visual noise
- Kotlin's smart cast should be utilized instead

---

## Requirements

1. Generated `when` expression should only pass the destination parameter (no navigator)
2. Destination should be passed without explicit cast (use smart cast)
3. The `ScreenRegistry` interface should be updated to remove the `navigator` parameter
4. Screen composables should obtain `navigator` via their own DI mechanism (default parameter)
5. Backward compatibility should be considered for migration period

---

## Proposed Solution

### Phase 1: Update ScreenRegistry Interface

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/registry/ScreenRegistry.kt`

**Before:**
```kotlin
interface ScreenRegistry {
    @Composable
    fun Content(
        destination: NavDestination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    )

    fun hasContent(destination: NavDestination): Boolean
}
```

**After:**
```kotlin
interface ScreenRegistry {
    @Composable
    fun Content(
        destination: NavDestination,
        sharedTransitionScope: SharedTransitionScope? = null,
        animatedVisibilityScope: AnimatedVisibilityScope? = null
    )

    fun hasContent(destination: NavDestination): Boolean
}
```

### Phase 2: Update All ScreenRegistry Implementations and Callers

**Affected files:**
1. `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/render/NavTreeRenderer.kt` - Update call site
2. `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/dsl/DslScreenRegistry.kt` - Update DSL implementation
3. `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/CompositeNavigationConfig.kt` - Update composite if present

**NavTreeRenderer.kt update:**
```kotlin
// Before
scope.screenRegistry.Content(
    destination = node.destination,
    navigator = scope.navigator,
    sharedTransitionScope = scope.sharedTransitionScope,
    animatedVisibilityScope = LocalAnimatedVisibilityScope.current
)

// After
scope.screenRegistry.Content(
    destination = node.destination,
    sharedTransitionScope = scope.sharedTransitionScope,
    animatedVisibilityScope = LocalAnimatedVisibilityScope.current
)
```

### Phase 3: Update KSP Generator - NavigationConfigGenerator

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/NavigationConfigGenerator.kt`

#### 3.1 Update `buildScreenContentFunction()`

**Before:**
```kotlin
private fun buildScreenContentFunction(screens: List<ScreenInfo>): FunSpec {
    return FunSpec.builder("Content")
        .addAnnotation(COMPOSABLE_CLASS)
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("destination", NAV_DESTINATION_CLASS)
        .addParameter("navigator", NAVIGATOR_CLASS)  // ❌ Remove
        .addParameter(...)
}
```

**After:**
```kotlin
private fun buildScreenContentFunction(screens: List<ScreenInfo>): FunSpec {
    return FunSpec.builder("Content")
        .addAnnotation(COMPOSABLE_CLASS)
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("destination", NAV_DESTINATION_CLASS)
        // navigator parameter removed
        .addParameter(
            ParameterSpec.builder(
                "sharedTransitionScope",
                SHARED_TRANSITION_SCOPE_CLASS.copy(nullable = true)
            )
                .defaultValue("null")
                .build()
        )
        .addParameter(
            ParameterSpec.builder(
                "animatedVisibilityScope",
                ANIMATED_VISIBILITY_SCOPE_CLASS.copy(nullable = true)
            )
                .defaultValue("null")
                .build()
        )
        ...
}
```

#### 3.2 Update `buildScreenFunctionCall()`

**Before:**
```kotlin
private fun buildScreenFunctionCall(screen: ScreenInfo): String {
    val funcName = screen.functionName
    val args = mutableListOf<String>()

    // Add destination parameter if needed (cast to specific type)
    if (screen.hasDestinationParam) {
        val destClassName = buildDestinationClassName(screen.destinationClass)
        args.add("destination = destination as $destClassName")  // ❌ Explicit cast
    }

    // Navigator is always passed
    args.add("navigator = navigator")  // ❌ Remove

    // Add shared transition scopes if needed
    ...
}
```

**After:**
```kotlin
private fun buildScreenFunctionCall(screen: ScreenInfo): String {
    val funcName = screen.functionName
    val args = mutableListOf<String>()

    // Add destination parameter if needed (use smart cast, no explicit as)
    if (screen.hasDestinationParam) {
        args.add("destination = destination")  // ✅ Smart cast in when
    }

    // navigator is NOT passed - screen gets it via DI

    // Add shared transition scopes if needed
    if (screen.hasSharedTransitionScope) {
        args.add("sharedTransitionScope = sharedTransitionScope!!")
    }
    if (screen.hasAnimatedVisibilityScope) {
        args.add("animatedVisibilityScope = animatedVisibilityScope!!")
    }

    return "$funcName(${args.joinToString(", ")})"
}
```

### Phase 4: Update DslScreenRegistry (DSL-based registration)

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ScreenBlockGenerator.kt`

If this is still used for DSL-based generation, update accordingly.

### Phase 5: Update ScreenInfo Model (Optional)

Consider whether `ScreenInfo.hasNavigatorParam` is needed or if we should remove any navigator-related tracking since navigator is no longer passed.

---

## Expected Generated Output After Refactoring

```kotlin
object GeneratedNavigationConfig : NavigationConfig {

    override val screenRegistry = object : ScreenRegistry {
        @Composable
        override fun Content(
            destination: NavDestination,
            sharedTransitionScope: SharedTransitionScope? = null,
            animatedVisibilityScope: AnimatedVisibilityScope? = null
        ) {
            when (destination) {
                is MainTabs.HomeTab -> HomeScreen()  // ✅ No navigator, screen gets it via DI
                is MasterDetailDestination.Detail -> DetailScreen(
                    destination = destination  // ✅ Smart cast, no explicit as
                )
                is MainTabs.SettingsTab.Profile -> ProfileScreen(
                    destination = destination,
                    sharedTransitionScope = sharedTransitionScope!!,
                    animatedVisibilityScope = animatedVisibilityScope!!
                )
                else -> error("No screen registered for destination: $destination")
            }
        }

        override fun hasContent(destination: NavDestination): Boolean {
            return when (destination) {
                is MainTabs.HomeTab,
                is MasterDetailDestination.Detail,
                is MainTabs.SettingsTab.Profile -> true
                else -> false
            }
        }
    }

    // ... rest of config
}
```

---

## Implementation Tasks

### Task 1: Update ScreenRegistry Interface
- [ ] Remove `navigator` parameter from `Content()` function
- [ ] Add default values for optional parameters
- [ ] Update KDoc

**File:** `quo-vadis-core/.../registry/ScreenRegistry.kt`

### Task 2: Update NavTreeRenderer Call Site
- [ ] Remove `navigator` argument from `screenRegistry.Content()` call

**File:** `quo-vadis-core/.../render/NavTreeRenderer.kt`

### Task 3: Update DslScreenRegistry
- [ ] Update `Content()` implementation to not use navigator parameter
- [ ] Update internal lambda signature stored for screens

**File:** `quo-vadis-core/.../dsl/DslScreenRegistry.kt`

### Task 4: Update NavigationConfigGenerator - buildScreenContentFunction
- [ ] Remove `navigator` parameter from generated function signature
- [ ] Add default values for optional parameters

**File:** `quo-vadis-ksp/.../generators/dsl/NavigationConfigGenerator.kt`

### Task 5: Update NavigationConfigGenerator - buildScreenFunctionCall
- [ ] Remove `navigator = navigator` from generated arguments
- [ ] Change `destination = destination as Foo` to `destination = destination` (use smart cast)

**File:** `quo-vadis-ksp/.../generators/dsl/NavigationConfigGenerator.kt`

### Task 6: Update addImports (if needed)
- [ ] Remove Navigator import if no longer needed in generated file

**File:** `quo-vadis-ksp/.../generators/dsl/NavigationConfigGenerator.kt`

### Task 7: Update ScreenBlockGenerator (if still used)
- [ ] Remove navigator from generated DSL blocks

**File:** `quo-vadis-ksp/.../generators/dsl/ScreenBlockGenerator.kt`

### Task 8: Search and Update Other Usages
- [ ] Search for `ScreenRegistry.Content` calls
- [ ] Update test files if any
- [ ] Update documentation/README if any

### Task 9: Build and Test
- [ ] Run `./gradlew build` to verify compilation
- [ ] Run tests to ensure functionality
- [ ] Test on sample app

---

## Migration Notes

### Breaking Change

This is a **breaking change** for anyone implementing `ScreenRegistry` manually. The interface signature changes from:

```kotlin
fun Content(destination, navigator, sharedScope?, animatedScope?)
```

to:

```kotlin
fun Content(destination, sharedScope? = null, animatedScope? = null)
```

### Mitigation

1. **Semver**: Bump minor version (or major if following strict semver)
2. **Changelog**: Document the breaking change
3. **Migration guide**: Add note that screen composables should use DI to obtain navigator

---

## Risks and Considerations

### Risk 1: Screens Not Using DI

Some users might have screens that don't use DI and expect navigator to be passed.

**Mitigation:** Document that screens should use DI to obtain navigator. Provide example in documentation.

### Risk 2: Testing Scenarios

Test screens might need navigator to be injectable for testing.

**Mitigation:** With DI approach, tests can simply provide a mock navigator via DI framework (e.g., Koin's `single { mockNavigator }`).

### Risk 3: Compose Preview

Compose previews might need navigator for previewing.

**Mitigation:** Screen composables can have default parameter `navigator: Navigator = koinInject()` which will fail gracefully in preview (or use preview-specific DI setup).

---

## Success Criteria

1. Generated `when` expression passes only destination (no navigator)
2. Destination is passed using smart cast (no explicit `as` keyword)
3. All existing functionality works correctly
4. All tests pass
5. Sample app compiles and runs correctly
