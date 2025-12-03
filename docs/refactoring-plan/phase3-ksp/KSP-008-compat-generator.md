# Task KSP-008: Generate Backward Compatibility Extensions

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | KSP-008 |
| **Name** | Backward Compatibility Extension Generator |
| **Phase** | 3 - KSP Processor Updates |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | KSP-004 (NavNode Builder Generator) |

## Overview

Generate extension properties and functions that provide a backward-compatible API surface, allowing existing code using `navigator.backStack` (List-based) to continue working while the underlying implementation uses the new tree-based `NavNode` architecture.

## Goals

1. **Minimize Breaking Changes**: Existing code should compile with deprecation warnings
2. **Clear Migration Path**: IDE quick-fixes guide developers to new APIs
3. **Gradual Adoption**: Teams can migrate incrementally

## Generated Extensions

### Navigator Extensions

```kotlin
// Generated: NavigatorCompatExtensions.kt

import com.jermey.quo.vadis.core.navigation.core.*

/**
 * Provides backward-compatible access to the navigation stack.
 * 
 * @deprecated Use `navigator.state` for tree-based navigation.
 * @see Navigator.state
 */
@Deprecated(
    message = "Use navigator.state for tree-based navigation",
    replaceWith = ReplaceWith(
        "this.state.value.activeStack",
        "com.jermey.quo.vadis.core.navigation.core.activeStack"
    ),
    level = DeprecationLevel.WARNING
)
val Navigator.backStack: List<BackStackEntry>
    get() = state.value.toBackStackEntries()

/**
 * Returns the active linear stack from the current navigation state.
 * Traverses to the deepest active StackNode.
 */
val NavNode.activeStack: List<BackStackEntry>
    get() = toBackStackEntries()

/**
 * Converts a NavNode tree to a flat list of BackStackEntry for compatibility.
 */
fun NavNode.toBackStackEntries(): List<BackStackEntry> {
    return when (this) {
        is ScreenNode -> listOf(
            BackStackEntry(
                id = key,
                destination = destination
            )
        )
        is StackNode -> children.flatMap { it.toBackStackEntries() }
        is TabNode -> stacks.getOrNull(activeStackIndex)
            ?.toBackStackEntries() 
            ?: emptyList()
        is PaneNode -> panes.flatMap { it.toBackStackEntries() }
    }
}

/**
 * Gets the current destination from the navigation state.
 * 
 * @deprecated Use navigator.state.value.activeDestination
 */
@Deprecated(
    message = "Use navigator.state.value.activeDestination",
    replaceWith = ReplaceWith("this.state.value.activeDestination"),
    level = DeprecationLevel.WARNING
)
val Navigator.currentDestination: Destination?
    get() = state.value.activeDestination

/**
 * Gets the active destination from the NavNode tree.
 */
val NavNode.activeDestination: Destination?
    get() = when (this) {
        is ScreenNode -> destination
        is StackNode -> activeChild?.activeDestination
        is TabNode -> stacks.getOrNull(activeStackIndex)?.activeDestination
        is PaneNode -> panes.firstOrNull()?.activeDestination
    }
```

### BackStack Compatibility

```kotlin
// Generated: BackStackCompatExtensions.kt

/**
 * Compatibility wrapper providing List-like access to navigation state.
 * 
 * @deprecated Direct backstack manipulation is deprecated.
 * Use TreeMutator operations via Navigator instead.
 */
@Deprecated(
    message = "Use Navigator.push/pop/navigateTo instead",
    level = DeprecationLevel.WARNING
)
class BackStackCompat(private val navigator: Navigator) {
    
    val size: Int
        get() = navigator.state.value.toBackStackEntries().size
    
    val current: BackStackEntry?
        get() = navigator.state.value.toBackStackEntries().lastOrNull()
    
    val canGoBack: Boolean
        get() = size > 1
    
    /**
     * @deprecated Use navigator.push(destination)
     */
    @Deprecated(
        message = "Use navigator.push(destination)",
        replaceWith = ReplaceWith("navigator.push(entry.destination)")
    )
    fun push(entry: BackStackEntry) {
        navigator.push(entry.destination)
    }
    
    /**
     * @deprecated Use navigator.pop()
     */
    @Deprecated(
        message = "Use navigator.pop()",
        replaceWith = ReplaceWith("navigator.pop()")
    )
    fun pop(): BackStackEntry? {
        val current = this.current
        navigator.pop()
        return current
    }
}

/**
 * Provides backward-compatible BackStack access.
 */
@Deprecated(
    message = "Use Navigator directly for navigation operations",
    level = DeprecationLevel.WARNING
)
val Navigator.backStackCompat: BackStackCompat
    get() = BackStackCompat(this)
```

### StateFlow Compatibility

```kotlin
// Generated: StateFlowCompatExtensions.kt

/**
 * Provides a StateFlow of BackStackEntry list for compatibility.
 * 
 * @deprecated Observe navigator.state directly
 */
@Deprecated(
    message = "Observe navigator.state and use .activeStack extension",
    replaceWith = ReplaceWith(
        "this.state.map { it.activeStack }",
        "kotlinx.coroutines.flow.map"
    ),
    level = DeprecationLevel.WARNING
)
val Navigator.backStackFlow: StateFlow<List<BackStackEntry>>
    get() = state.map { it.toBackStackEntries() }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main.immediate),
            started = SharingStarted.Lazily,
            initialValue = state.value.toBackStackEntries()
        )

/**
 * Provides a StateFlow of the current destination for compatibility.
 */
@Deprecated(
    message = "Observe navigator.state and use .activeDestination extension",
    level = DeprecationLevel.WARNING
)
val Navigator.currentDestinationFlow: StateFlow<Destination?>
    get() = state.map { it.activeDestination }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main.immediate),
            started = SharingStarted.Lazily,
            initialValue = state.value.activeDestination
        )
```

## Implementation

### CompatibilityGenerator Class

```kotlin
// quo-vadis-ksp/src/main/kotlin/.../CompatibilityGenerator.kt

class CompatibilityGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {
    
    fun generate(graphInfo: GraphInfo) {
        generateNavigatorExtensions()
        generateBackStackCompat()
        generateStateFlowCompat()
        generateGraphSpecificCompat(graphInfo)
    }
    
    private fun generateNavigatorExtensions() {
        val fileSpec = FileSpec.builder(
            "com.jermey.quo.vadis.core.navigation.compat",
            "NavigatorCompatExtensions"
        ).apply {
            addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "DEPRECATION")
                    .build()
            )
            
            // Add all Navigator extension properties
            addProperty(generateBackStackProperty())
            addProperty(generateCurrentDestinationProperty())
            addFunction(generateToBackStackEntriesFunction())
            addProperty(generateActiveDestinationProperty())
        }.build()
        
        fileSpec.writeTo(codeGenerator, aggregating = true)
    }
    
    private fun generateGraphSpecificCompat(graphInfo: GraphInfo) {
        // Generate graph-specific compatibility if needed
        // e.g., old-style graph builders that wrap new NavNode builders
    }
}
```

### Integration with GraphProcessor

```kotlin
// Update GraphProcessor.kt

override fun process(resolver: Resolver): List<KSAnnotatedSymbol> {
    // ... existing processing ...
    
    // Generate compatibility layer
    val compatGenerator = CompatibilityGenerator(codeGenerator, logger)
    graphs.forEach { graph ->
        compatGenerator.generate(extractGraphInfo(graph))
    }
    
    return emptyList()
}
```

## Deprecation Timeline

| Version | Deprecation Level | Behavior |
|---------|-------------------|----------|
| 2.0.0 | `WARNING` | Compiler warnings, IDE suggestions |
| 3.0.0 | `WARNING` | Same, mentioned in release notes |
| 4.0.0 | `ERROR` | Compilation fails |
| 5.0.0 | Removed | Extensions deleted |

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-ksp/src/main/kotlin/.../CompatibilityGenerator.kt` | New |
| `quo-vadis-ksp/src/main/kotlin/.../GraphProcessor.kt` | Modify |
| Generated: `NavigatorCompatExtensions.kt` | New (generated) |
| Generated: `BackStackCompatExtensions.kt` | New (generated) |
| Generated: `StateFlowCompatExtensions.kt` | New (generated) |

## Acceptance Criteria

- [ ] `navigator.backStack` returns compatible `List<BackStackEntry>`
- [ ] `navigator.currentDestination` returns active destination
- [ ] `NavNode.toBackStackEntries()` correctly flattens tree
- [ ] `NavNode.activeDestination` traverses to leaf
- [ ] All deprecated APIs have `ReplaceWith` for IDE quick-fix
- [ ] StateFlow compatibility wrappers work correctly
- [ ] Deprecation levels follow timeline
- [ ] Generated code compiles without errors

## Testing Notes

```kotlin
@Test
fun `backStack compatibility returns correct entries`() {
    val navigator = Navigator()
    navigator.push(HomeDestination)
    navigator.push(DetailDestination("123"))
    
    @Suppress("DEPRECATION")
    val backStack = navigator.backStack
    
    assertThat(backStack).hasSize(2)
    assertThat(backStack.last().destination).isEqualTo(DetailDestination("123"))
}

@Test
fun `activeDestination traverses TabNode correctly`() {
    val tabNode = TabNode(
        key = "tabs",
        parentKey = null,
        stacks = listOf(
            StackNode("s0", "tabs", listOf(ScreenNode("home", "s0", HomeDestination))),
            StackNode("s1", "tabs", listOf(ScreenNode("profile", "s1", ProfileDestination)))
        ),
        activeStackIndex = 1
    )
    
    assertThat(tabNode.activeDestination).isEqualTo(ProfileDestination)
}
```

## References

- [KSP-004: NavNode Builder Generator](./KSP-004-navnode-generator.md)
- [CORE-005: Backward Compatibility Layer](../phase1-core/CORE-005-backward-compat.md)
- [MIG-005: Type-Safe Deprecation Warnings](../phase5-migration/MIG-005-deprecation-warnings.md)
