````markdown
# HIER-010: @PaneWrapper Annotation

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-010 |
| **Task Name** | Add @PaneWrapper Annotation |
| **Phase** | Phase 2: KSP Updates |
| **Complexity** | Small |
| **Estimated Time** | 0.5-1 day |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | HIER-012, HIER-013 |

---

## Overview

This task creates the `@PaneWrapper` annotation for marking composable functions as pane wrappers. Pane wrappers provide the adaptive layout structure (navigation rail, pane arrangement, etc.) for multi-pane layouts in the hierarchical rendering engine.

### Context

Pane wrappers handle the complex layout responsibilities of adaptive UIs:
- Multi-pane arrangement on large screens (side-by-side, three-column)
- Single-pane presentation on compact screens
- Layout transitions during screen size changes
- Navigation rail or drawer for pane selection

### Key Design Principle

The `@PaneWrapper` annotation follows the same pattern as `@TabWrapper`:
- Applied to `@Composable` functions
- References a target class (`paneClass`)
- Function signature is validated by KSP
- Provides content slots via `PaneContentScope`

---

## File Location

```
quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/PaneWrapper.kt
```

---

## Implementation

### PaneWrapper Annotation

```kotlin
package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a composable function as a pane wrapper for a specific [@Pane] container.
 *
 * Pane wrappers provide the adaptive layout structure for multi-pane UIs. The
 * wrapper function receives a [PaneWrapperScope] for accessing pane state and
 * multiple content slots for rendering each pane's content.
 *
 * ## Usage
 *
 * Apply to a @Composable function that provides adaptive pane layout:
 * ```kotlin
 * @PaneWrapper(CatalogPane::class)
 * @Composable
 * fun CatalogPaneWrapper(
 *     scope: PaneWrapperScope,
 *     content: @Composable PaneContentScope.() -> Unit
 * ) {
 *     if (scope.isExpanded) {
 *         // Multi-pane layout for large screens
 *         Row(Modifier.fillMaxSize()) {
 *             Box(Modifier.weight(1f)) {
 *                 content.invoke(scope.paneContent(PaneRole.PRIMARY))
 *             }
 *             Box(Modifier.weight(2f)) {
 *                 content.invoke(scope.paneContent(PaneRole.SECONDARY))
 *             }
 *         }
 *     } else {
 *         // Single-pane layout for compact screens
 *         Box(Modifier.fillMaxSize()) {
 *             content.invoke(scope.paneContent(scope.activePaneRole))
 *         }
 *     }
 * }
 * ```
 *
 * ## Function Signature Requirements
 *
 * The annotated function must have the following signature:
 * ```kotlin
 * @Composable
 * fun WrapperName(
 *     scope: PaneWrapperScope,
 *     content: @Composable PaneContentScope.() -> Unit
 * )
 * ```
 *
 * - First parameter: `PaneWrapperScope` - provides access to pane state
 * - Second parameter: `@Composable PaneContentScope.() -> Unit` - content accessor
 * - Return type: `Unit` (implicit for @Composable)
 *
 * ## Content Slot Pattern
 *
 * Unlike tab wrappers which have a single content slot, pane wrappers need
 * to render multiple panes. The `PaneContentScope` provides access to each
 * pane's content via the [PaneRole]:
 *
 * ```kotlin
 * // Render primary pane
 * content.invoke(scope.paneContent(PaneRole.PRIMARY))
 *
 * // Render secondary pane
 * content.invoke(scope.paneContent(PaneRole.SECONDARY))
 * ```
 *
 * ## Adaptive Layout Support
 *
 * The wrapper is responsible for:
 * - Detecting screen size via [PaneWrapperScope.isExpanded]
 * - Arranging panes appropriately (side-by-side vs stacked)
 * - Handling pane visibility based on [AdaptStrategy]
 * - Providing navigation UI (rail, drawer) when needed
 *
 * ## Common Patterns
 *
 * ### List-Detail (Two-Pane)
 * ```kotlin
 * @PaneWrapper(CatalogPane::class)
 * @Composable
 * fun ListDetailWrapper(scope: PaneWrapperScope, content: @Composable PaneContentScope.() -> Unit) {
 *     ListDetailPaneScaffold(
 *         listPane = { content.invoke(scope.paneContent(PaneRole.PRIMARY)) },
 *         detailPane = { content.invoke(scope.paneContent(PaneRole.SECONDARY)) }
 *     )
 * }
 * ```
 *
 * ### Three-Column (Mail App)
 * ```kotlin
 * @PaneWrapper(MailPane::class)
 * @Composable
 * fun ThreeColumnWrapper(scope: PaneWrapperScope, content: @Composable PaneContentScope.() -> Unit) {
 *     ThreeColumnLayout(
 *         navigation = { content.invoke(scope.paneContent(PaneRole.PRIMARY)) },
 *         list = { content.invoke(scope.paneContent(PaneRole.SECONDARY)) },
 *         detail = { content.invoke(scope.paneContent(PaneRole.EXTRA)) }
 *     )
 * }
 * ```
 *
 * ## Generated Code
 *
 * KSP generates a `WrapperRegistry` that dispatches to wrapper functions:
 * ```kotlin
 * // Generated: GeneratedWrapperRegistry.kt
 * object GeneratedWrapperRegistry : WrapperRegistry {
 *     @Composable
 *     override fun PaneWrapper(
 *         scope: PaneWrapperScope,
 *         paneNodeKey: String,
 *         content: @Composable PaneContentScope.() -> Unit
 *     ) {
 *         when {
 *             paneNodeKey.contains("catalog") -> CatalogPaneWrapper(scope, content)
 *             else -> DefaultPaneWrapper(scope, content)
 *         }
 *     }
 * }
 * ```
 *
 * ## Default Wrapper
 *
 * If no `@PaneWrapper` is defined for a `@Pane` container, the generated registry
 * uses a default wrapper that renders panes side-by-side when expanded and
 * shows only the active pane when compact.
 *
 * @property paneClass The `@Pane`-annotated class this wrapper is for. Must be a
 *   sealed class annotated with [@Pane]. KSP validates this reference.
 *
 * @see Pane
 * @see PaneWrapperScope
 * @see PaneContentScope
 * @see PaneRole
 * @see TabWrapper
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class PaneWrapper(
    /**
     * The @Pane-annotated class this wrapper is for.
     */
    val paneClass: KClass<*>
)
```

### PaneWrapperScope Interface

Create the scope interface in the core module:

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/scope/PaneWrapperScope.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.scope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.PaneRole

/**
 * Scope provided to [@PaneWrapper] composable functions.
 *
 * Contains all necessary state and actions for rendering an adaptive pane
 * layout. The wrapper uses this scope to determine layout configuration
 * and access individual pane contents.
 *
 * ## Usage in Wrapper
 *
 * ```kotlin
 * @PaneWrapper(CatalogPane::class)
 * @Composable
 * fun CatalogPaneWrapper(
 *     scope: PaneWrapperScope,
 *     content: @Composable PaneContentScope.() -> Unit
 * ) {
 *     if (scope.isExpanded) {
 *         Row(Modifier.fillMaxSize()) {
 *             Box(Modifier.weight(1f)) {
 *                 val primaryScope = scope.paneContent(PaneRole.PRIMARY)
 *                 content.invoke(primaryScope)
 *             }
 *             if (scope.hasPane(PaneRole.SECONDARY)) {
 *                 Box(Modifier.weight(2f)) {
 *                     val secondaryScope = scope.paneContent(PaneRole.SECONDARY)
 *                     content.invoke(secondaryScope)
 *                 }
 *             }
 *         }
 *     } else {
 *         Box(Modifier.fillMaxSize()) {
 *             val activeScope = scope.paneContent(scope.activePaneRole)
 *             content.invoke(activeScope)
 *         }
 *     }
 * }
 * ```
 *
 * ## Screen Size Detection
 *
 * The [isExpanded] property reflects the current window size class. It's derived
 * from `WindowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded`. The
 * wrapper should adapt its layout accordingly.
 *
 * ## Pane Visibility
 *
 * Not all panes may be visible at all times:
 * - On compact screens, only [activePaneRole] is typically shown
 * - Some panes may be hidden based on [AdaptStrategy]
 * - Use [isPaneVisible] to check if a pane should be rendered
 *
 * @see PaneWrapper
 * @see PaneContentScope
 * @see PaneRole
 */
@Stable
interface PaneWrapperScope {
    /**
     * The navigator for performing navigation actions.
     */
    val navigator: Navigator

    /**
     * Whether the layout is in expanded mode (large screen).
     *
     * When true, multiple panes should be shown side-by-side.
     * When false, typically only [activePaneRole] should be visible.
     */
    val isExpanded: Boolean

    /**
     * The currently active (focused) pane role.
     *
     * This is the pane that has navigation focus. On compact screens,
     * this is typically the only visible pane.
     */
    val activePaneRole: PaneRole

    /**
     * All pane roles configured in this container.
     */
    val configuredRoles: Set<PaneRole>

    /**
     * Metadata for each configured pane.
     */
    val paneMetadata: Map<PaneRole, PaneMetadata>

    /**
     * Check if a pane role is configured in this container.
     */
    fun hasPane(role: PaneRole): Boolean

    /**
     * Check if a pane should be visible in the current layout.
     *
     * Takes into account screen size, adapt strategy, and pane configuration.
     *
     * @param role The pane role to check
     * @return true if the pane should be rendered
     */
    fun isPaneVisible(role: PaneRole): Boolean

    /**
     * Get the content scope for a specific pane role.
     *
     * Use this to render individual pane content in your layout:
     * ```kotlin
     * val scope = paneWrapperScope.paneContent(PaneRole.PRIMARY)
     * content.invoke(scope)
     * ```
     *
     * @param role The pane role to get content for
     * @return PaneContentScope for rendering that pane's content
     */
    fun paneContent(role: PaneRole): PaneContentScope

    /**
     * Switch focus to a different pane.
     *
     * Updates [activePaneRole] and may trigger navigation state changes.
     *
     * @param role The pane role to activate
     */
    fun switchPane(role: PaneRole)
}

/**
 * Scope for rendering individual pane content.
 *
 * This scope is passed to the content lambda when rendering a specific pane.
 * It provides the pane's role and any pane-specific state.
 */
@Stable
interface PaneContentScope {
    /**
     * The role of this pane.
     */
    val role: PaneRole

    /**
     * Whether this pane is currently the active (focused) pane.
     */
    val isActive: Boolean

    /**
     * Whether this pane is currently visible.
     */
    val isVisible: Boolean
}

/**
 * UI metadata for a pane.
 *
 * Contains configuration information for pane rendering and adaptation.
 * This is extracted from [@PaneItem] annotations during KSP processing.
 *
 * @property role The pane's role in the layout
 * @property adaptStrategy How the pane adapts when space is limited
 */
data class PaneMetadata(
    val role: PaneRole,
    val adaptStrategy: AdaptStrategy
)

/**
 * Strategy for adapting a pane when space is limited.
 *
 * Mirrors the annotation enum for runtime use.
 */
enum class AdaptStrategy {
    /** Hide the pane completely */
    HIDE,
    /** Show as collapsed representation */
    COLLAPSE,
    /** Show as overlay/modal */
    OVERLAY,
    /** Reflow below other content */
    REFLOW
}
```

---

## Integration Points

### 1. KSP Processing (HIER-012)

The `@PaneWrapper` annotation is processed by the `WrapperProcessor`:
- Finds all functions annotated with `@PaneWrapper`
- Validates function signature (PaneWrapperScope + content lambda with PaneContentScope receiver)
- Extracts `paneClass` reference
- Validates `paneClass` is annotated with `@Pane`

### 2. Registry Generation (HIER-013)

The `WrapperRegistryGenerator` uses extracted wrapper info:
- Generates `when` expression mapping pane node keys to wrapper functions
- Provides default pane wrapper fallback

### 3. Runtime Resolution (PaneRenderer - HIER-022)

The `PaneRenderer` invokes wrappers through the registry:
```kotlin
scope.wrapperRegistry.PaneWrapper(
    wrapperScope = paneWrapperScope,
    paneNodeKey = node.key
) { paneContentScope ->
    // Content for the pane identified by paneContentScope.role
    NavTreeRenderer(
        node = paneConfigurations[paneContentScope.role]?.content,
        scope = scope
    )
}
```

---

## Testing Requirements

### Unit Tests

```kotlin
class PaneWrapperAnnotationTest {

    @Test
    fun `annotation has correct target`() {
        val targets = PaneWrapper::class.annotations
            .filterIsInstance<Target>()
            .first()
            .allowedTargets
        
        assertContains(targets, AnnotationTarget.FUNCTION)
    }

    @Test
    fun `annotation has source retention`() {
        val retention = PaneWrapper::class.annotations
            .filterIsInstance<Retention>()
            .first()
            .value
        
        assertEquals(AnnotationRetention.SOURCE, retention)
    }

    @Test
    fun `annotation requires paneClass parameter`() {
        val parameters = PaneWrapper::class.primaryConstructor?.parameters
        assertNotNull(parameters)
        assertEquals(1, parameters.size)
        assertEquals("paneClass", parameters[0].name)
    }
}

class PaneWrapperScopeTest {

    @Test
    fun `paneContent returns scope for configured role`() {
        val scope = createTestPaneWrapperScope(
            configuredRoles = setOf(PaneRole.PRIMARY, PaneRole.SECONDARY)
        )
        
        val primaryContent = scope.paneContent(PaneRole.PRIMARY)
        assertEquals(PaneRole.PRIMARY, primaryContent.role)
    }

    @Test
    fun `hasPane returns true for configured roles`() {
        val scope = createTestPaneWrapperScope(
            configuredRoles = setOf(PaneRole.PRIMARY, PaneRole.SECONDARY)
        )
        
        assertTrue(scope.hasPane(PaneRole.PRIMARY))
        assertTrue(scope.hasPane(PaneRole.SECONDARY))
        assertFalse(scope.hasPane(PaneRole.EXTRA))
    }

    @Test
    fun `isPaneVisible respects expanded state`() {
        val compactScope = createTestPaneWrapperScope(
            isExpanded = false,
            activePaneRole = PaneRole.PRIMARY,
            configuredRoles = setOf(PaneRole.PRIMARY, PaneRole.SECONDARY)
        )
        
        assertTrue(compactScope.isPaneVisible(PaneRole.PRIMARY))
        assertFalse(compactScope.isPaneVisible(PaneRole.SECONDARY))
        
        val expandedScope = createTestPaneWrapperScope(
            isExpanded = true,
            activePaneRole = PaneRole.PRIMARY,
            configuredRoles = setOf(PaneRole.PRIMARY, PaneRole.SECONDARY)
        )
        
        assertTrue(expandedScope.isPaneVisible(PaneRole.PRIMARY))
        assertTrue(expandedScope.isPaneVisible(PaneRole.SECONDARY))
    }
}
```

### KSP Integration Tests (HIER-012)

```kotlin
@Test
fun `KSP validates correct wrapper signature`() {
    // Given
    val source = """
        @Pane(name = "catalog")
        sealed class CatalogPane : Destination { ... }
        
        @PaneWrapper(CatalogPane::class)
        @Composable
        fun CatalogPaneWrapper(
            scope: PaneWrapperScope,
            content: @Composable PaneContentScope.() -> Unit
        ) {
            // Implementation
        }
    """
    
    // When processed by KSP
    // Then no errors
}

@Test
fun `KSP rejects wrapper with missing PaneContentScope receiver`() {
    // Given
    val source = """
        @PaneWrapper(CatalogPane::class)
        @Composable
        fun BadWrapper(
            scope: PaneWrapperScope,
            content: @Composable () -> Unit  // Missing PaneContentScope receiver!
        ) { }
    """
    
    // When processed by KSP
    // Then error: "Content parameter must have PaneContentScope receiver"
}

@Test
fun `KSP rejects wrapper for non-Pane class`() {
    // Given
    val source = """
        @Tab(name = "main", items = [...])
        object MainTabs
        
        @PaneWrapper(MainTabs::class)  // Not a @Pane!
        @Composable
        fun BadWrapper(scope: PaneWrapperScope, content: @Composable PaneContentScope.() -> Unit) { }
    """
    
    // When processed by KSP
    // Then error: "paneClass must be annotated with @Pane"
}
```

---

## Acceptance Criteria

- [ ] `@PaneWrapper` annotation defined in `quo-vadis-annotations` module
- [ ] `@Target(AnnotationTarget.FUNCTION)` specified
- [ ] `@Retention(AnnotationRetention.SOURCE)` specified
- [ ] `paneClass: KClass<*>` parameter defined
- [ ] `PaneWrapperScope` interface defined in `quo-vadis-core`
- [ ] `PaneContentScope` interface defined
- [ ] `PaneMetadata` data class defined
- [ ] `AdaptStrategy` runtime enum defined (mirroring annotation enum)
- [ ] Comprehensive KDoc documentation with examples
- [ ] Unit tests for annotation properties
- [ ] Unit tests for scope interfaces
- [ ] Annotation compiles on all target platforms (Android, iOS, Desktop, Web)

---

## Notes

### Design Decisions

1. **Content Slot Pattern**: Using `PaneContentScope.() -> Unit` as the receiver allows the wrapper to control which pane is rendered by providing the appropriate scope. This is more flexible than fixed slots like `listPane` and `detailPane`.

2. **AdaptStrategy Duplication**: The `AdaptStrategy` enum exists in both annotations and core modules. This is intentional to avoid core module dependencies in annotations, and KSP handles the conversion.

### Open Questions

1. **Levitate vs Overlay**: The annotation has both `LEVITATE` and `OVERLAY` potentially. Should we consolidate or keep both for semantic differences?

2. **Animation Control**: Should `PaneWrapperScope` expose animation progress for custom transition effects during layout changes?

### Future Enhancements

- Draggable pane dividers for user-adjustable pane sizes
- Pane pinning/unpinning support
- Keyboard shortcuts for pane navigation

---

## References

- [RENDER-011-hierarchical-engine.md](RENDER-011-hierarchical-engine.md) - Architecture overview
- [PaneAnnotations.kt](../../../quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/PaneAnnotations.kt) - Existing @Pane annotations
- [HIER-009-tab-wrapper-annotation.md](HIER-009-tab-wrapper-annotation.md) - Similar annotation pattern

````