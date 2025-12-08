`n
# RENDER-002C: PaneNode Adaptive Flattening

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-002C |
| **Task Name** | PaneNode Adaptive Flattening |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | High |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-002A, RENDER-009 |
| **Blocked By** | RENDER-002A |
| **Blocks** | RENDER-004 |

---

## Overview

PaneNode adaptive rendering based on screen size. Small screens behave like StackNode, large screens show all panes with user wrapper.

### Purpose

This task extends the `TreeFlattener` from RENDER-002A to handle `PaneNode` flattening with adaptive behavior based on `WindowSizeClass`. The key design principle is:

- **Small screens (Compact)** - Single pane visible, behaves like StackNode
- **Large screens (Medium/Expanded)** - All panes visible, user controls wrapper layout

### Why Adaptive Rendering?

Pane-based layouts (List-Detail, NavigationSuiteScaffold, etc.) require fundamentally different presentations based on screen size:

- **Phones** - Sequential navigation through panes (stack-like behavior)
- **Tablets/Desktop** - Side-by-side pane display with user-controlled layout
- **Foldables** - May switch between modes based on fold state

By accepting `WindowSizeClass` as a parameter, the flattener can produce appropriate surfaces for each scenario.

### Flattening Problem (Pane Focus)

```
PaneNode (panes)
├── windowSizeClass: WindowSizeClass
├── wrapper: @Composable { (paneStructures: List<PaneStructure>) -> /* user layout */ }
├── ScreenNode (list)       ← PANE 0 (list role)
└── ScreenNode (detail)     ← PANE 1 (detail role, active on small screens)

Small Screen Output (Compact):
FlattenResult(
    surfaces = [
        RenderableSurface(
            id = "panes-pane-1",
            renderingMode = PANE_AS_STACK,
            previousSurfaceId = "panes-pane-0",
            content = resolveContent(detailPane)
        )
    ],
    cachingHints = CachingHints(isCrossNodeTypeNavigation = ...)
)

Large Screen Output (Medium/Expanded):
FlattenResult(
    surfaces = [
        RenderableSurface(
            id = "panes-wrapper",
            renderingMode = PANE_WRAPPER,
            paneStructures = [
                PaneStructure(paneRole = LIST, content = listContent),
                PaneStructure(paneRole = DETAIL, content = detailContent)
            ]
        ),
        RenderableSurface(
            id = "panes-content-0",
            renderingMode = PANE_CONTENT,
            parentWrapperId = "panes-wrapper"
        ),
        RenderableSurface(
            id = "panes-content-1",
            renderingMode = PANE_CONTENT,
            parentWrapperId = "panes-wrapper"
        )
    ],
    cachingHints = CachingHints(
        wrapperIds = setOf("panes-wrapper"),
        contentIds = setOf("panes-content-0", "panes-content-1"),
        isCrossNodeTypeNavigation = false
    )
)
```

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TreeFlattener.kt
```

---

## Key Requirements

### 1. Accept WindowSizeClass Parameter

The `flattenPane` function must accept `WindowSizeClass` to determine rendering mode:

```kotlin
fun flattenPane(
    pane: PaneNode,
    windowSizeClass: WindowSizeClass,
    context: FlattenContext
): FlattenResult
```

### 2. Small Screens (Compact Width)

When `windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact`:

| Behavior | Description |
|----------|-------------|
| Single pane visible | Only active pane is rendered |
| Stack-like behavior | Previous pane ID tracked for back navigation |
| Output mode | `PANE_AS_STACK` |
| Caching | Cache whole screen (like StackNode) |

### 3. Large Screens (Medium/Expanded Width)

When `windowSizeClass.widthSizeClass` is `Medium` or `Expanded`:

| Behavior | Description |
|----------|-------------|
| All panes visible | User builds wrapper with all panes |
| User controls layout | Wrapper receives `List<PaneStructure>` |
| Output mode | `PANE_WRAPPER` + `PANE_CONTENT` surfaces |
| Caching | Same strategy as TabNode |

### 4. PaneStructure Provision

For multi-pane mode, provide `paneStructures` field with:

- `paneRole: PaneRole` - Identifies pane purpose (LIST, DETAIL, etc.)
- `content: @Composable () -> Unit` - Resolved content for each pane

### 5. Parent-Child Linking

Content surfaces have `parentWrapperId` pointing to wrapper surface for proper rendering coordination.

### 6. Same Caching Strategy as TabNode

| Navigation Type | Cache Behavior |
|-----------------|----------------|
| Cross-node navigation | Cache WHOLE wrapper |
| Intra-pane navigation | Cache ONLY content (not wrapper) |

---

## Implementation

### PaneNode Flattening Implementation

```kotlin
/**
 * Flattens a PaneNode with adaptive rendering based on screen size.
 * 
 * ## Compact Width (Small Screens)
 * 
 * Renders like StackNode - only active pane visible with back navigation support.
 * 
 * ## Medium/Expanded Width (Large Screens)
 * 
 * Renders all panes with user wrapper composable controlling layout.
 * 
 * @param pane The PaneNode to flatten
 * @param windowSizeClass Current window size classification
 * @param context Current flattening context
 * @return FlattenResult with appropriate surfaces for screen size
 */
fun flattenPane(
    pane: PaneNode,
    windowSizeClass: WindowSizeClass,
    context: FlattenContext
): FlattenResult {
    return if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
        flattenPaneAsStack(pane, context)
    } else {
        flattenPaneMultiPane(pane, context)
    }
}

/**
 * Flattens PaneNode for small screens (Compact width).
 * 
 * Behaves like StackNode:
 * - Only one pane visible at a time
 * - Tracks previous pane for back navigation animations
 * - Uses PANE_AS_STACK rendering mode
 * 
 * @param pane The PaneNode to flatten
 * @param context Current flattening context
 * @return FlattenResult with single stack-like surface
 */
private fun flattenPaneAsStack(
    pane: PaneNode,
    context: FlattenContext
): FlattenResult {
    val activePaneIndex = pane.activePaneIndex
    val activePane = pane.panes[activePaneIndex]
    val previousPaneIndex = pane.previousPaneIndex
    
    // Generate surface ID including pane index for uniqueness
    val surfaceId = "${pane.key}-pane-${activePaneIndex}"
    val previousPaneId = if (previousPaneIndex != null && previousPaneIndex != activePaneIndex) {
        "${pane.key}-pane-${previousPaneIndex}"
    } else {
        null
    }
    
    // Determine if this is cross-node navigation
    val isCrossNodeNavigation = context.previousSiblingId != null &&
                                 !context.previousSiblingId.startsWith(pane.key)
    
    // Resolve animation spec
    val animationSpec = if (previousPaneId != null) {
        animationResolver.resolve(
            from = pane.panes.getOrNull(previousPaneIndex ?: -1),
            to = activePane,
            transitionType = TransitionType.PANE_SWITCH
        )
    } else if (isCrossNodeNavigation) {
        animationResolver.resolve(null, activePane, context.transitionType)
    } else {
        SurfaceAnimationSpec.None
    }
    
    val surface = RenderableSurface(
        id = surfaceId,
        zOrder = context.baseZOrder,
        nodeType = SurfaceNodeType.PANE,
        renderingMode = SurfaceRenderingMode.PANE_AS_STACK,
        transitionState = SurfaceTransitionState.Visible,
        animationSpec = animationSpec,
        content = contentResolver.resolve(activePane),
        parentWrapperId = context.parentId,
        previousSurfaceId = previousPaneId ?: context.previousSiblingId,
        metadata = mapOf(
            "activePaneIndex" to activePaneIndex,
            "paneCount" to pane.panes.size,
            "paneRole" to pane.paneRoles.getOrNull(activePaneIndex)?.name
        )
    )
    
    val animationPairs = mutableListOf<AnimationPair>()
    
    // Add animation pair for pane navigation
    if (previousPaneId != null) {
        animationPairs.add(
            AnimationPair(
                currentId = surfaceId,
                previousId = previousPaneId,
                transitionType = TransitionType.PANE_SWITCH
            )
        )
    }
    
    // Add animation pair for cross-node navigation
    if (isCrossNodeNavigation && context.previousSiblingId != null) {
        animationPairs.add(
            AnimationPair(
                currentId = surfaceId,
                previousId = context.previousSiblingId,
                transitionType = context.transitionType
            )
        )
    }
    
    return FlattenResult(
        surfaces = listOf(surface),
        animationPairs = animationPairs,
        cachingHints = CachingHints(
            shouldCacheWrapper = false,
            shouldCacheContent = true,
            cacheableIds = setOf(surfaceId),
            invalidatedIds = if (previousPaneId != null) setOf(previousPaneId) else emptySet(),
            isCrossNodeTypeNavigation = isCrossNodeNavigation
        )
    )
}

/**
 * Flattens PaneNode for large screens (Medium/Expanded width).
 * 
 * All panes visible with user-controlled wrapper layout:
 * - PANE_WRAPPER surface contains user's layout composable
 * - PANE_CONTENT surfaces for each pane
 * - paneStructures provides pane metadata to user wrapper
 * 
 * @param pane The PaneNode to flatten
 * @param context Current flattening context
 * @return FlattenResult with wrapper and content surfaces
 */
private fun flattenPaneMultiPane(
    pane: PaneNode,
    context: FlattenContext
): FlattenResult {
    val wrapperSurfaceId = "${pane.key}-wrapper"
    
    // Determine if this is cross-node navigation
    val isCrossNodeNavigation = context.previousSiblingId != null &&
                                 !context.previousSiblingId.startsWith(pane.key)
    
    // Build pane structures for user wrapper
    val paneStructures = pane.panes.mapIndexed { index, node ->
        PaneStructure(
            paneRole = pane.paneRoles.getOrElse(index) { PaneRole.EXTRA },
            content = contentResolver.resolve(node)
        )
    }
    
    // Resolve wrapper animation
    val wrapperAnimationSpec = if (isCrossNodeNavigation) {
        animationResolver.resolve(null, pane, context.transitionType)
    } else {
        SurfaceAnimationSpec.None
    }
    
    // 1. Create wrapper surface
    val wrapperSurface = RenderableSurface(
        id = wrapperSurfaceId,
        zOrder = context.baseZOrder,
        nodeType = SurfaceNodeType.PANE,
        renderingMode = SurfaceRenderingMode.PANE_WRAPPER,
        transitionState = SurfaceTransitionState.Visible,
        animationSpec = wrapperAnimationSpec,
        content = createPaneWrapperContent(pane, paneStructures),
        parentWrapperId = context.parentId,
        previousSurfaceId = if (isCrossNodeNavigation) context.previousSiblingId else null,
        paneStructures = paneStructures,
        metadata = mapOf(
            "paneCount" to pane.panes.size,
            "paneRoles" to pane.paneRoles.map { it.name }
        )
    )
    
    // 2. Create content surfaces for each pane
    val contentSurfaces = pane.panes.mapIndexed { index, node ->
        RenderableSurface(
            id = "${pane.key}-content-$index",
            zOrder = context.baseZOrder + ((index + 1) * zOrderIncrement),
            nodeType = SurfaceNodeType.PANE,
            renderingMode = SurfaceRenderingMode.PANE_CONTENT,
            transitionState = SurfaceTransitionState.Visible,
            animationSpec = SurfaceAnimationSpec.None, // Content doesn't animate independently
            content = contentResolver.resolve(node),
            parentWrapperId = wrapperSurfaceId,
            previousSurfaceId = null,
            metadata = mapOf(
                "paneIndex" to index,
                "paneRole" to pane.paneRoles.getOrNull(index)?.name
            )
        )
    }
    
    val surfaces = listOf(wrapperSurface) + contentSurfaces
    val contentIds = contentSurfaces.map { it.id }.toSet()
    
    val animationPairs = mutableListOf<AnimationPair>()
    
    // Add animation pair for cross-node navigation
    if (isCrossNodeNavigation && context.previousSiblingId != null) {
        animationPairs.add(
            AnimationPair(
                currentId = wrapperSurfaceId,
                previousId = context.previousSiblingId,
                transitionType = context.transitionType
            )
        )
    }
    
    return FlattenResult(
        surfaces = surfaces,
        animationPairs = animationPairs,
        cachingHints = CachingHints(
            shouldCacheWrapper = isCrossNodeNavigation,
            shouldCacheContent = true,
            cacheableIds = if (isCrossNodeNavigation) {
                setOf(wrapperSurfaceId) + contentIds
            } else {
                contentIds
            },
            invalidatedIds = emptySet(),
            wrapperIds = setOf(wrapperSurfaceId),
            contentIds = contentIds,
            isCrossNodeTypeNavigation = isCrossNodeNavigation
        )
    )
}

/**
 * Creates wrapper content that provides paneStructures to user's wrapper.
 * 
 * @param pane The PaneNode containing wrapper configuration
 * @param paneStructures List of pane structures for user layout
 */
@Composable
private fun createPaneWrapperContent(
    pane: PaneNode,
    paneStructures: List<PaneStructure>
): @Composable () -> Unit {
    return {
        // User's wrapper receives paneStructures to build layout
        pane.wrapper?.invoke(paneStructures)
    }
}
```

### Supporting Types

```kotlin
/**
 * Represents the role of a pane in a multi-pane layout.
 * 
 * Used by user's wrapper composable to identify and position panes.
 */
enum class PaneRole {
    /** Primary list/navigation pane */
    LIST,
    
    /** Detail/content pane */
    DETAIL,
    
    /** Supporting/supplementary pane */
    SUPPORTING,
    
    /** Additional pane beyond standard roles */
    EXTRA
}

/**
 * Transition types for animations.
 */
enum class TransitionType {
    NONE,
    PUSH,
    POP,
    TAB_SWITCH,
    PANE_SWITCH,
    CROSS_NAVIGATOR
}
```

---

## Implementation Steps

### Step 1: Add WindowSizeClass Parameter

1. Update `flattenPane` signature to accept `WindowSizeClass`
2. Add width class check for routing to appropriate flattening method

### Step 2: Implement flattenPaneAsStack

1. Generate surface ID with pane index
2. Track previous pane ID for back navigation
3. Create single `RenderableSurface` with `PANE_AS_STACK` mode
4. Generate animation pairs for pane/cross-node navigation
5. Return `FlattenResult` with stack-like caching hints

### Step 3: Implement flattenPaneMultiPane

1. Generate wrapper surface ID
2. Build `paneStructures` list with roles and content
3. Create wrapper `RenderableSurface` with `PANE_WRAPPER` mode
4. Create content `RenderableSurface` for each pane with `PANE_CONTENT` mode
5. Set `parentWrapperId` on content surfaces
6. Generate animation pairs for cross-node navigation
7. Return `FlattenResult` with TabNode-like caching hints

### Step 4: Add PaneRole Enum

1. Define standard pane roles (LIST, DETAIL, SUPPORTING, EXTRA)
2. Add to PaneNode structure for role assignment

### Step 5: Update TreeFlattener Entry Point

1. Pass `WindowSizeClass` through flattening context
2. Route to `flattenPane` when encountering PaneNode

### Step 6: Add Unit Tests

1. Test Compact width → PANE_AS_STACK output
2. Test Medium/Expanded width → PANE_WRAPPER + PANE_CONTENT output
3. Test paneStructures population
4. Test parentWrapperId linking
5. Test caching hints for both modes

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/TreeFlattener.kt` | Modify | Add `flattenPane`, `flattenPaneAsStack`, `flattenPaneMultiPane` |
| `quo-vadis-core/.../compose/RenderableSurface.kt` | Reference | Uses PANE_WRAPPER, PANE_CONTENT, PANE_AS_STACK modes |
| `quo-vadis-core/.../compose/FlattenResult.kt` | Reference | Uses CachingHints with wrapper/content IDs |
| `quo-vadis-core/.../navigation/PaneRole.kt` | Create | Define PaneRole enum |
| `quo-vadis-core/.../navigation/TransitionType.kt` | Modify | Add PANE_SWITCH transition type |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| RENDER-002A (Core flattenState) | Hard | Must complete first |
| RENDER-009 (WindowSizeClass integration) | Hard | Must complete first |
| RENDER-001 (RenderableSurface) | Reference | Uses surface types |
| RENDER-002B (Tab flattening) | Reference | Same caching strategy |

---

## Acceptance Criteria

- [ ] Accepts `WindowSizeClass` parameter
- [ ] Compact width → `flattenPaneAsStack()` called
- [ ] Medium/Expanded width → `flattenPaneMultiPane()` called
- [ ] `PANE_AS_STACK` mode for single-pane rendering
- [ ] `PANE_WRAPPER` + `PANE_CONTENT` for multi-pane rendering
- [ ] `paneStructures` populated with `PaneRole` + content
- [ ] `parentWrapperId` links content surfaces to wrapper surface
- [ ] `previousSurfaceId` tracks previous pane for stack-like mode
- [ ] Same caching strategy as TabNode (wrapper vs content)
- [ ] Animation pairs generated for pane switch transitions
- [ ] Animation pairs generated for cross-node transitions
- [ ] Comprehensive KDoc documentation
- [ ] Unit tests for Compact width mode
- [ ] Unit tests for Medium/Expanded width mode
- [ ] Unit tests for paneStructures population
- [ ] Unit tests for caching hints both modes
- [ ] Code compiles on all target platforms

---

## Testing Notes

```kotlin
class TreeFlattenerPaneTest {

    private val mockContentResolver = TreeFlattener.ContentResolver { {} }
    private val flattener = TreeFlattener(mockContentResolver)

    @Test
    fun `compact width produces PANE_AS_STACK surface`() {
        val pane = createPaneNode(activePaneIndex = 1)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(400.dp, 800.dp) // Compact width
        )
        
        val result = flattener.flattenPane(pane, windowSizeClass, FlattenContext.default())
        
        assertEquals(1, result.surfaces.size)
        assertEquals(SurfaceRenderingMode.PANE_AS_STACK, result.surfaces[0].renderingMode)
        assertEquals("panes-pane-1", result.surfaces[0].id)
    }

    @Test
    fun `compact width tracks previousSurfaceId for back navigation`() {
        val pane = createPaneNode(activePaneIndex = 1, previousPaneIndex = 0)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(400.dp, 800.dp) // Compact width
        )
        
        val result = flattener.flattenPane(pane, windowSizeClass, FlattenContext.default())
        
        assertEquals("panes-pane-0", result.surfaces[0].previousSurfaceId)
    }

    @Test
    fun `expanded width produces PANE_WRAPPER and PANE_CONTENT surfaces`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(1200.dp, 800.dp) // Expanded width
        )
        
        val result = flattener.flattenPane(pane, windowSizeClass, FlattenContext.default())
        
        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }
        val contentSurfaces = result.surfaces.filter { it.renderingMode == SurfaceRenderingMode.PANE_CONTENT }
        
        assertNotNull(wrapperSurface)
        assertEquals(2, contentSurfaces.size)
        assertEquals("panes-wrapper", wrapperSurface.id)
    }

    @Test
    fun `expanded width populates paneStructures`() {
        val pane = createPaneNode(paneCount = 2, paneRoles = listOf(PaneRole.LIST, PaneRole.DETAIL))
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(1200.dp, 800.dp) // Expanded width
        )
        
        val result = flattener.flattenPane(pane, windowSizeClass, FlattenContext.default())
        
        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }
        
        assertNotNull(wrapperSurface?.paneStructures)
        assertEquals(2, wrapperSurface?.paneStructures?.size)
        assertEquals(PaneRole.LIST, wrapperSurface?.paneStructures?.get(0)?.paneRole)
        assertEquals(PaneRole.DETAIL, wrapperSurface?.paneStructures?.get(1)?.paneRole)
    }

    @Test
    fun `content surfaces have parentWrapperId pointing to wrapper`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(1200.dp, 800.dp) // Expanded width
        )
        
        val result = flattener.flattenPane(pane, windowSizeClass, FlattenContext.default())
        
        val contentSurfaces = result.surfaces.filter { it.renderingMode == SurfaceRenderingMode.PANE_CONTENT }
        
        contentSurfaces.forEach { surface ->
            assertEquals("panes-wrapper", surface.parentWrapperId)
        }
    }

    @Test
    fun `compact width generates PANE_SWITCH animation pair`() {
        val pane = createPaneNode(activePaneIndex = 1, previousPaneIndex = 0)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(400.dp, 800.dp) // Compact width
        )
        
        val result = flattener.flattenPane(pane, windowSizeClass, FlattenContext.default())
        
        val animationPair = result.animationPairs.find { it.transitionType == TransitionType.PANE_SWITCH }
        
        assertNotNull(animationPair)
        assertEquals("panes-pane-1", animationPair.currentId)
        assertEquals("panes-pane-0", animationPair.previousId)
    }

    @Test
    fun `compact width caching hints match stack behavior`() {
        val pane = createPaneNode(activePaneIndex = 0)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(400.dp, 800.dp) // Compact width
        )
        
        val result = flattener.flattenPane(pane, windowSizeClass, FlattenContext.default())
        
        assertFalse(result.cachingHints.shouldCacheWrapper)
        assertTrue(result.cachingHints.shouldCacheContent)
        assertTrue(result.cachingHints.cacheableIds.contains("panes-pane-0"))
    }

    @Test
    fun `expanded width caching hints match tab behavior`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(1200.dp, 800.dp) // Expanded width
        )
        
        val result = flattener.flattenPane(pane, windowSizeClass, FlattenContext.default())
        
        assertTrue(result.cachingHints.wrapperIds.contains("panes-wrapper"))
        assertTrue(result.cachingHints.contentIds.contains("panes-content-0"))
        assertTrue(result.cachingHints.contentIds.contains("panes-content-1"))
    }

    @Test
    fun `cross-node navigation caches whole wrapper in expanded mode`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(1200.dp, 800.dp) // Expanded width
        )
        val context = FlattenContext(
            baseZOrder = 0,
            parentId = null,
            previousSiblingId = "previous-stack-screen",
            transitionType = TransitionType.PUSH
        )
        
        val result = flattener.flattenPane(pane, windowSizeClass, context)
        
        assertTrue(result.cachingHints.shouldCacheWrapper)
        assertTrue(result.cachingHints.isCrossNodeTypeNavigation)
    }

    @Test
    fun `medium width also produces multi-pane output`() {
        val pane = createPaneNode(paneCount = 2)
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(700.dp, 800.dp) // Medium width
        )
        
        val result = flattener.flattenPane(pane, windowSizeClass, FlattenContext.default())
        
        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.PANE_WRAPPER }
        
        assertNotNull(wrapperSurface)
    }

    private fun createPaneNode(
        paneCount: Int = 2,
        activePaneIndex: Int = 0,
        previousPaneIndex: Int? = null,
        paneRoles: List<PaneRole> = listOf(PaneRole.LIST, PaneRole.DETAIL)
    ): PaneNode {
        val panes = (0 until paneCount).map { mockScreen("pane-$it") }
        return PaneNode(
            key = "panes",
            parentKey = null,
            panes = panes,
            paneRoles = paneRoles.take(paneCount),
            activePaneIndex = activePaneIndex,
            previousPaneIndex = previousPaneIndex
        )
    }
    
    private fun mockScreen(key: String): ScreenNode {
        return ScreenNode(key, null, mockDestination)
    }
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-001](./RENDER-001-renderable-surface.md) - RenderableSurface definition
- [RENDER-002A](./RENDER-002A-core-flatten.md) - Core flattening algorithm
- [RENDER-002B](./RENDER-002B-tab-flattening.md) - TabNode flattening (same caching strategy)
- [RENDER-004](./RENDER-004-quovadis-host.md) - Consumer of flattened surfaces
- [RENDER-009](./RENDER-009-window-size-class.md) - WindowSizeClass integration

````
