````markdown
# HIER-009: @TabWrapper Annotation

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-009 |
| **Task Name** | Add @TabWrapper Annotation |
| **Phase** | Phase 2: KSP Updates |
| **Complexity** | Small |
| **Estimated Time** | 0.5-1 day |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | HIER-012, HIER-013 |

---

## Overview

This task creates the `@TabWrapper` annotation for marking composable functions as tab wrappers. Tab wrappers provide the UI chrome (scaffold, bottom navigation, etc.) that surrounds tab content in the hierarchical rendering engine.

### Context

In the hierarchical rendering architecture, tab wrappers are no longer passed as runtime lambdas but instead declared via annotations and resolved through a KSP-generated registry. This enables:

- **Compile-time validation**: KSP validates that wrapper functions have correct signatures
- **Type-safe binding**: Wrappers are associated with specific `@Tab`-annotated classes
- **Consistent composition**: The rendering engine knows exactly which wrapper to use for each tab container

### Key Design Principle

The `@TabWrapper` annotation follows the same pattern as `@Screen`:
- Applied to `@Composable` functions
- References a target class (`tabClass`)
- Function signature is validated by KSP

---

## File Location

```
quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabWrapper.kt
```

---

## Implementation

### TabWrapper Annotation

```kotlin
package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a composable function as a tab wrapper for a specific [@Tab] container.
 *
 * Tab wrappers provide the UI chrome (scaffold, navigation bar, FAB, etc.) that
 * surrounds tab content. The wrapper function receives a [TabWrapperScope] for
 * accessing tab state and a content slot for rendering the active tab's content.
 *
 * ## Usage
 *
 * Apply to a @Composable function that provides tab UI chrome:
 * ```kotlin
 * @TabWrapper(MainTabs::class)
 * @Composable
 * fun MainTabsWrapper(
 *     scope: TabWrapperScope,
 *     content: @Composable () -> Unit
 * ) {
 *     Scaffold(
 *         bottomBar = {
 *             NavigationBar {
 *                 scope.tabMetadata.forEachIndexed { index, metadata ->
 *                     NavigationBarItem(
 *                         selected = scope.activeIndex == index,
 *                         onClick = { scope.switchTab(index) },
 *                         icon = { Icon(metadata.icon, metadata.label) },
 *                         label = { Text(metadata.label) }
 *                     )
 *                 }
 *             }
 *         }
 *     ) { padding ->
 *         Box(Modifier.padding(padding)) {
 *             content()  // Active tab content slot
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
 *     scope: TabWrapperScope,
 *     content: @Composable () -> Unit
 * )
 * ```
 *
 * - First parameter: `TabWrapperScope` - provides access to tab state
 * - Second parameter: `@Composable () -> Unit` - content slot for active tab
 * - Return type: `Unit` (implicit for @Composable)
 *
 * ## Hierarchical Rendering
 *
 * In hierarchical rendering, the tab wrapper **contains** the tab content as a
 * child in the Compose tree. This enables:
 * - Coordinated animations (wrapper + content animate together)
 * - Predictive back gestures transform the entire tab as a unit
 * - Proper slot semantics (content is inside wrapper, not beside it)
 *
 * ## Generated Code
 *
 * KSP generates a `WrapperRegistry` that dispatches to wrapper functions:
 * ```kotlin
 * // Generated: GeneratedWrapperRegistry.kt
 * object GeneratedWrapperRegistry : WrapperRegistry {
 *     @Composable
 *     override fun TabWrapper(
 *         scope: TabWrapperScope,
 *         tabNodeKey: String,
 *         content: @Composable () -> Unit
 *     ) {
 *         when {
 *             tabNodeKey.contains("mainTabs") -> MainTabsWrapper(scope, content)
 *             else -> content() // Default: no wrapper
 *         }
 *     }
 * }
 * ```
 *
 * ## Multiple Tab Containers
 *
 * An app can have multiple `@Tab` containers, each with its own wrapper:
 * ```kotlin
 * @Tab(name = "mainTabs", items = [...])
 * object MainTabs
 *
 * @TabWrapper(MainTabs::class)
 * @Composable
 * fun MainTabsWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) { ... }
 *
 * @Tab(name = "settingsTabs", items = [...])
 * object SettingsTabs
 *
 * @TabWrapper(SettingsTabs::class)
 * @Composable
 * fun SettingsTabsWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) { ... }
 * ```
 *
 * ## Default Wrapper
 *
 * If no `@TabWrapper` is defined for a `@Tab` container, the generated registry
 * uses a default wrapper that simply renders the content without chrome.
 *
 * @property tabClass The `@Tab`-annotated class this wrapper is for. Must be a
 *   class or object annotated with [@Tab]. KSP validates this reference.
 *
 * @see Tab
 * @see TabWrapperScope
 * @see PaneWrapper
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class TabWrapper(
    /**
     * The @Tab-annotated class this wrapper is for.
     */
    val tabClass: KClass<*>
)
```

### TabWrapperScope Interface

Create the scope interface in the core module for use with the annotation:

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/scope/TabWrapperScope.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.scope

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Scope provided to [@TabWrapper] composable functions.
 *
 * Contains all necessary state and actions for rendering a tabbed navigation
 * container. The wrapper uses this scope to build tab bar UI and handle
 * tab switching.
 *
 * ## Usage in Wrapper
 *
 * ```kotlin
 * @TabWrapper(MainTabs::class)
 * @Composable
 * fun MainTabsWrapper(
 *     scope: TabWrapperScope,
 *     content: @Composable () -> Unit
 * ) {
 *     Scaffold(
 *         bottomBar = {
 *             NavigationBar {
 *                 scope.tabMetadata.forEachIndexed { index, metadata ->
 *                     NavigationBarItem(
 *                         selected = scope.activeIndex == index,
 *                         onClick = { scope.switchTab(index) },
 *                         icon = { Icon(metadata.icon, metadata.label) },
 *                         label = { Text(metadata.label) }
 *                     )
 *                 }
 *             }
 *         }
 *     ) { padding ->
 *         Box(Modifier.padding(padding)) {
 *             content()
 *         }
 *     }
 * }
 * ```
 *
 * ## State Changes
 *
 * When [switchTab] is called, the navigator updates the navigation state,
 * which triggers recomposition with the new [activeIndex]. The wrapper
 * should update its UI accordingly (e.g., highlight the new tab).
 *
 * @see TabWrapper
 * @see TabMetadata
 */
@Stable
interface TabWrapperScope {
    /**
     * The navigator for performing navigation actions.
     *
     * Can be used for advanced navigation operations beyond tab switching.
     */
    val navigator: Navigator

    /**
     * Index of the currently active tab (0-based).
     *
     * Use this to highlight the selected tab in the tab bar UI.
     */
    val activeIndex: Int

    /**
     * Total number of tabs in this container.
     */
    val tabCount: Int

    /**
     * Metadata for each tab (label, icon, etc.).
     *
     * Use this to build the tab bar UI. The list order matches tab indices.
     */
    val tabMetadata: List<TabMetadata>

    /**
     * Switch to the tab at the given index.
     *
     * @param index The 0-based index of the tab to switch to
     * @throws IndexOutOfBoundsException if index is out of range
     */
    fun switchTab(index: Int)
}

/**
 * UI metadata for a tab.
 *
 * Contains display information for rendering a tab in the tab bar.
 * This is extracted from [@TabItem] annotations during KSP processing.
 *
 * @property label Display label for the tab
 * @property icon Icon identifier (platform-specific interpretation)
 */
data class TabMetadata(
    val label: String,
    val icon: String
)
```

---

## Integration Points

### 1. KSP Processing (HIER-012)

The `@TabWrapper` annotation is processed by the `WrapperProcessor`:
- Finds all functions annotated with `@TabWrapper`
- Validates function signature (TabWrapperScope + content lambda)
- Extracts `tabClass` reference
- Validates `tabClass` is annotated with `@Tab`

### 2. Registry Generation (HIER-013)

The `WrapperRegistryGenerator` uses extracted wrapper info:
- Generates `when` expression mapping tab node keys to wrapper functions
- Provides default wrapper fallback for tabs without custom wrappers

### 3. Runtime Resolution (TabRenderer - HIER-021)

The `TabRenderer` invokes wrappers through the registry:
```kotlin
scope.wrapperRegistry.TabWrapper(
    wrapperScope = tabWrapperScope,
    tabNodeKey = node.key
) {
    // Tab content slot
}
```

---

## Testing Requirements

### Unit Tests

```kotlin
class TabWrapperAnnotationTest {

    @Test
    fun `annotation has correct target`() {
        val targets = TabWrapper::class.annotations
            .filterIsInstance<Target>()
            .first()
            .allowedTargets
        
        assertContains(targets, AnnotationTarget.FUNCTION)
    }

    @Test
    fun `annotation has source retention`() {
        val retention = TabWrapper::class.annotations
            .filterIsInstance<Retention>()
            .first()
            .value
        
        assertEquals(AnnotationRetention.SOURCE, retention)
    }

    @Test
    fun `annotation requires tabClass parameter`() {
        val parameters = TabWrapper::class.primaryConstructor?.parameters
        assertNotNull(parameters)
        assertEquals(1, parameters.size)
        assertEquals("tabClass", parameters[0].name)
    }
}
```

### KSP Integration Tests (HIER-012)

```kotlin
@Test
fun `KSP validates correct wrapper signature`() {
    // Given
    val source = """
        @Tab(name = "main", items = [HomeTab::class])
        object MainTabs
        
        @TabWrapper(MainTabs::class)
        @Composable
        fun MainTabsWrapper(
            scope: TabWrapperScope,
            content: @Composable () -> Unit
        ) {
            content()
        }
    """
    
    // When processed by KSP
    // Then no errors
}

@Test
fun `KSP rejects wrapper with wrong parameter types`() {
    // Given
    val source = """
        @TabWrapper(MainTabs::class)
        @Composable
        fun BadWrapper(
            scope: String,  // Wrong type!
            content: @Composable () -> Unit
        ) { }
    """
    
    // When processed by KSP
    // Then error: "First parameter must be TabWrapperScope"
}

@Test
fun `KSP rejects wrapper for non-Tab class`() {
    // Given
    val source = """
        @Stack(name = "home", startDestination = "Feed")
        sealed class HomeDestination : Destination
        
        @TabWrapper(HomeDestination::class)  // Not a @Tab!
        @Composable
        fun BadWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) { }
    """
    
    // When processed by KSP
    // Then error: "tabClass must be annotated with @Tab"
}
```

---

## Acceptance Criteria

- [ ] `@TabWrapper` annotation defined in `quo-vadis-annotations` module
- [ ] `@Target(AnnotationTarget.FUNCTION)` specified
- [ ] `@Retention(AnnotationRetention.SOURCE)` specified
- [ ] `tabClass: KClass<*>` parameter defined
- [ ] `TabWrapperScope` interface defined in `quo-vadis-core`
- [ ] `TabMetadata` data class defined
- [ ] Comprehensive KDoc documentation with examples
- [ ] Unit tests for annotation properties
- [ ] Annotation compiles on all target platforms (Android, iOS, Desktop, Web)

---

## Notes

### Open Questions

1. **Icon Type**: Should `TabMetadata.icon` be a `String` or a more type-safe representation (e.g., `ImageVector`, `Painter`)? String allows flexibility but loses type safety.

2. **Badge Support**: Should `TabMetadata` include badge information (count, dot, etc.) for notification badges on tabs?

### Future Enhancements

- Consider adding `@TabWrapper.order` parameter if multiple wrappers for the same tab are ever needed (unlikely)
- Consider `enabled` parameter to conditionally disable tabs

---

## References

- [RENDER-011-hierarchical-engine.md](RENDER-011-hierarchical-engine.md) - Architecture overview
- [TabAnnotations.kt](../../../quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt) - Existing @Tab annotation
- [KSP-001-graph-type-enum.md](../phase3-ksp/KSP-001-graph-type-enum.md) - KSP extractor pattern

````