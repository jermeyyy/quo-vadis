````markdown
# RENDER-002B: TabNode Flattening with User Wrapper Support

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-002B |
| **Task Name** | TabNode Flattening with User Wrapper Support |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | High |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-002A, RENDER-008 |
| **Blocked By** | RENDER-002A |
| **Blocks** | RENDER-004 |

---

## Overview

Handle TabNode with user-provided wrapper composable pattern. User controls wrapper (scaffold, tabs, bottom nav), library provides slot content.

### Purpose

This task extends the `TreeFlattener` from RENDER-002A to handle `TabNode` flattening with a user-provided wrapper pattern. The key design principle is that:

- **User controls the wrapper** - Scaffold, tabs, bottom navigation bar
- **Library provides slot content** - Tab content rendered into user's wrapper slot

### Why User Wrapper Pattern?

Users often need custom tab layouts with:
- Bottom navigation bars
- Tab rails for different screen sizes
- Custom scaffolds with FABs, app bars
- Platform-specific tab presentations

By allowing user-provided wrappers with a `@Composable` lambda parameter, the library remains flexible while handling complex tab state management.

### Flattening Problem (Tab Focus)

```
TabNode (tabs)
├── wrapper: @Composable { (content: @Composable () -> Unit) -> /* user scaffold */ }
├── StackNode (home-stack)     ← TAB 0 (previous)
│   └── ScreenNode (home)
└── StackNode (profile-stack)  ← TAB 1 (active)
    └── ScreenNode (profile)

Output:
FlattenResult(
    surfaces = [
        RenderableSurface(
            id = "tabs-wrapper",
            renderingMode = TAB_WRAPPER,
            content = { userWrapper(slotContent) }
        ),
        RenderableSurface(
            id = "tabs-content-1",
            renderingMode = TAB_CONTENT,
            parentWrapperId = "tabs-wrapper",
            previousSurfaceId = "tabs-content-0"
        )
    ],
    animationPairs = [
        AnimationPair(currentId="tabs-content-1", previousId="tabs-content-0", transitionType=TAB_SWITCH)
    ],
    cachingHints = CachingHints(
        wrapperIds = setOf("tabs-wrapper"),
        contentIds = setOf("tabs-content-1"),
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

### 1. User Wrapper Composable Pattern

User provides wrapper composable with `@Composable` lambda parameter for tab content:

```kotlin
@Composable
fun MyTabScaffold(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    content: @Composable () -> Unit  // Library fills this slot
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                // Tab items...
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            content()  // Tab content rendered here
        }
    }
}
```

### 2. Dual Surface Output

Library renders two surfaces:

| Surface | Rendering Mode | Purpose |
|---------|---------------|---------|
| Wrapper Surface | `TAB_WRAPPER` | User's scaffold/container |
| Content Surface | `TAB_CONTENT` | Active tab's content |

### 3. Caching Strategy

Different caching behavior based on navigation type:

| Navigation Type | Cache Behavior |
|-----------------|----------------|
| Cross-node navigation | Cache WHOLE wrapper |
| Intra-tab navigation | Cache ONLY content (not wrapper) |

**Example:**
- Navigating from `StackNode` to `TabNode` → cache entire tab wrapper
- Switching tabs within `TabNode` → only cache tab content surfaces

### 4. Animation Support

- Track active + previous tab content for animations
- Content surface has `previousSurfaceId` pointing to previous tab's content
- Support `TAB_SWITCH` transition type

### 5. Parent-Child Linking

Content surface has `parentWrapperId` pointing to wrapper surface for proper rendering coordination.

---

## Implementation

### Extended CachingHints

```kotlin
/**
 * Extended caching hints for tab flattening.
 * 
 * @property wrapperIds IDs of wrapper surfaces (TAB_WRAPPER, PANE_WRAPPER)
 * @property contentIds IDs of content surfaces
 * @property isCrossNodeTypeNavigation True if navigating between different node types
 */
@Immutable
data class CachingHints(
    val shouldCacheWrapper: Boolean = false,
    val shouldCacheContent: Boolean = true,
    val cacheableIds: Set<String> = emptySet(),
    val invalidatedIds: Set<String> = emptySet(),
    val wrapperIds: Set<String> = emptySet(),
    val contentIds: Set<String> = emptySet(),
    val isCrossNodeTypeNavigation: Boolean = false
) {
    companion object {
        val Default = CachingHints()
        val NoCache = CachingHints(
            shouldCacheWrapper = false,
            shouldCacheContent = false
        )
    }
}
```

### TabNode Flattening Implementation

```kotlin
/**
 * Flattens a TabNode into wrapper and content surfaces.
 * 
 * Produces two surfaces:
 * 1. TAB_WRAPPER - Contains user's wrapper composable (scaffold, nav bar, etc.)
 * 2. TAB_CONTENT - Contains active tab's content
 * 
 * ## Caching Strategy
 * 
 * - Cross-node navigation (e.g., Stack → Tab): Cache WHOLE wrapper
 * - Intra-tab navigation (tab switch): Cache ONLY content surfaces
 * 
 * ## Animation Support
 * 
 * Content surface tracks `previousSurfaceId` for tab switch animations.
 * The previous tab index is stored to enable smooth transitions.
 * 
 * @param tab The TabNode to flatten
 * @param context Current flattening context
 * @param accumulator Accumulator for results
 */
private fun flattenTab(
    tab: TabNode,
    context: FlattenContext,
    accumulator: FlattenAccumulator
) {
    val activeStackIndex = tab.activeStackIndex
    val activeStack = tab.activeStack
    val previousTabIndex = tab.previousTabIndex // Track for animation
    
    // Generate wrapper surface ID
    val wrapperSurfaceId = "${tab.key}-wrapper"
    
    // Generate content surface ID (includes tab index for uniqueness)
    val contentSurfaceId = "${tab.key}-content-${activeStackIndex}"
    val previousContentSurfaceId = if (previousTabIndex != null && previousTabIndex != activeStackIndex) {
        "${tab.key}-content-${previousTabIndex}"
    } else {
        null
    }
    
    // Determine if this is a cross-node type navigation
    val isCrossNodeNavigation = context.transitionType != TransitionType.TAB_SWITCH &&
                                 context.previousSiblingId != null &&
                                 !context.previousSiblingId.startsWith(tab.key)
    
    // 1. Create wrapper surface
    val wrapperAnimationSpec = if (isCrossNodeNavigation) {
        animationResolver.resolve(null, tab, context.transitionType)
    } else {
        SurfaceAnimationSpec.None // No animation for wrapper during tab switches
    }
    
    val wrapperSurface = RenderableSurface(
        id = wrapperSurfaceId,
        zOrder = context.baseZOrder,
        nodeType = SurfaceNodeType.TAB,
        renderingMode = SurfaceRenderingMode.TAB_WRAPPER,
        transitionState = SurfaceTransitionState.Visible,
        animationSpec = wrapperAnimationSpec,
        content = createWrapperContent(tab, contentSurfaceId),
        parentWrapperId = context.parentId,
        previousSurfaceId = if (isCrossNodeNavigation) context.previousSiblingId else null,
        metadata = mapOf(
            "activeTabIndex" to activeStackIndex,
            "tabCount" to tab.stacks.size
        )
    )
    
    accumulator.addSurface(wrapperSurface)
    
    // 2. Create content surface for active tab
    val contentAnimationSpec = animationResolver.resolve(
        from = null,
        to = activeStack,
        transitionType = TransitionType.TAB_SWITCH
    )
    
    val contentSurface = RenderableSurface(
        id = contentSurfaceId,
        zOrder = context.baseZOrder + zOrderIncrement,
        nodeType = SurfaceNodeType.TAB,
        renderingMode = SurfaceRenderingMode.TAB_CONTENT,
        transitionState = SurfaceTransitionState.Visible,
        animationSpec = contentAnimationSpec,
        content = contentResolver.resolve(activeStack),
        parentWrapperId = wrapperSurfaceId,
        previousSurfaceId = previousContentSurfaceId,
        metadata = mapOf(
            "tabIndex" to activeStackIndex,
            "stackKey" to activeStack.key
        )
    )
    
    accumulator.addSurface(contentSurface)
    
    // 3. Add animation pair for tab switch
    if (previousContentSurfaceId != null) {
        accumulator.addAnimationPair(
            AnimationPair(
                currentId = contentSurfaceId,
                previousId = previousContentSurfaceId,
                transitionType = TransitionType.TAB_SWITCH
            )
        )
    }
    
    // 4. Add animation pair for cross-node navigation
    if (isCrossNodeNavigation && context.previousSiblingId != null) {
        accumulator.addAnimationPair(
            AnimationPair(
                currentId = wrapperSurfaceId,
                previousId = context.previousSiblingId,
                transitionType = context.transitionType
            )
        )
    }
    
    // 5. Update caching hints
    accumulator.wrapperIds.add(wrapperSurfaceId)
    accumulator.contentIds.add(contentSurfaceId)
    accumulator.isCrossNodeNavigation = isCrossNodeNavigation
    
    // 6. Set cacheable IDs based on navigation type
    if (isCrossNodeNavigation) {
        // Cross-node: cache whole wrapper
        accumulator.cacheableIds.add(wrapperSurfaceId)
        accumulator.cacheableIds.add(contentSurfaceId)
    } else {
        // Intra-tab: only cache content
        accumulator.cacheableIds.add(contentSurfaceId)
        if (previousContentSurfaceId != null) {
            accumulator.invalidatedIds.add(previousContentSurfaceId)
        }
    }
    
    // 7. Recursively flatten active stack's content
    val contentContext = FlattenContext(
        baseZOrder = context.baseZOrder + (2 * zOrderIncrement),
        parentId = contentSurfaceId,
        previousSiblingId = null, // Reset for nested content
        transitionType = TransitionType.NONE
    )
    
    // Flatten the active stack's children (screens within the tab)
    flattenStackContent(activeStack, contentContext, accumulator)
}

/**
 * Creates wrapper content that integrates user's wrapper with slot content.
 * 
 * @param tab The TabNode containing wrapper configuration
 * @param contentSurfaceId ID of the content surface to render in slot
 */
@Composable
private fun createWrapperContent(
    tab: TabNode,
    contentSurfaceId: String
): @Composable () -> Unit {
    return {
        // User's wrapper receives the slot content lambda
        tab.wrapper?.invoke { slotContent ->
            // Library provides slot content - rendered by the renderer
            // using contentSurfaceId to look up the actual content
            SurfaceSlot(surfaceId = contentSurfaceId)
        }
    }
}

/**
 * Flattens the content within a stack (for nested tab content).
 * 
 * @param stack The StackNode to flatten
 * @param context Current flattening context
 * @param accumulator Accumulator for results
 */
private fun flattenStackContent(
    stack: StackNode,
    context: FlattenContext,
    accumulator: FlattenAccumulator
) {
    val activeChild = stack.activeChild ?: return
    
    when (activeChild) {
        is ScreenNode -> {
            val surface = RenderableSurface(
                id = activeChild.key,
                zOrder = context.baseZOrder,
                nodeType = SurfaceNodeType.SCREEN,
                renderingMode = SurfaceRenderingMode.STACK_CONTENT,
                transitionState = SurfaceTransitionState.Visible,
                animationSpec = SurfaceAnimationSpec.None,
                content = contentResolver.resolve(activeChild),
                parentWrapperId = context.parentId,
                previousSurfaceId = context.previousSiblingId
            )
            accumulator.addSurface(surface)
        }
        else -> flatten(activeChild, context, accumulator)
    }
}
```

### Updated FlattenAccumulator

```kotlin
/**
 * Accumulated state during flattening.
 */
private class FlattenAccumulator {
    val surfaces = mutableListOf<RenderableSurface>()
    val animationPairs = mutableListOf<AnimationPair>()
    val cacheableIds = mutableSetOf<String>()
    val invalidatedIds = mutableSetOf<String>()
    val wrapperIds = mutableSetOf<String>()
    val contentIds = mutableSetOf<String>()
    var isCrossNodeNavigation = false
    
    fun addSurface(surface: RenderableSurface) {
        surfaces.add(surface)
    }
    
    fun addAnimationPair(pair: AnimationPair) {
        animationPairs.add(pair)
    }
    
    fun toResult(): FlattenResult {
        return FlattenResult(
            surfaces = surfaces.sortedBy { it.zOrder },
            animationPairs = animationPairs.toList(),
            cachingHints = CachingHints(
                shouldCacheWrapper = isCrossNodeNavigation,
                shouldCacheContent = true,
                cacheableIds = cacheableIds.toSet(),
                invalidatedIds = invalidatedIds.toSet(),
                wrapperIds = wrapperIds.toSet(),
                contentIds = contentIds.toSet(),
                isCrossNodeTypeNavigation = isCrossNodeNavigation
            )
        )
    }
}
```

---

## Implementation Steps

### Step 1: Extend CachingHints

1. Add `wrapperIds: Set<String>` property
2. Add `contentIds: Set<String>` property
3. Add `isCrossNodeTypeNavigation: Boolean` flag

### Step 2: Update FlattenAccumulator

1. Add `wrapperIds` mutable set
2. Add `contentIds` mutable set
3. Add `isCrossNodeNavigation` flag
4. Update `toResult()` to populate extended CachingHints

### Step 3: Implement Tab Wrapper Surface

1. Generate wrapper surface ID: `"${tab.key}-wrapper"`
2. Create `RenderableSurface` with `TAB_WRAPPER` mode
3. Set up wrapper content that integrates user's composable

### Step 4: Implement Tab Content Surface

1. Generate content surface ID: `"${tab.key}-content-${activeStackIndex}"`
2. Create `RenderableSurface` with `TAB_CONTENT` mode
3. Set `parentWrapperId` to wrapper surface ID
4. Set `previousSurfaceId` to previous tab's content ID

### Step 5: Implement Caching Logic

1. Detect cross-node vs intra-tab navigation
2. For cross-node: mark wrapper + content as cacheable
3. For intra-tab: mark only content as cacheable

### Step 6: Implement Animation Pairing

1. Create `AnimationPair` for tab switches
2. Create `AnimationPair` for cross-node navigation
3. Set correct `TransitionType` for each pair

### Step 7: Recursively Flatten Tab Content

1. Create child context with updated parent ID
2. Flatten active stack's content

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/FlattenResult.kt` | Modify | Extend CachingHints with wrapper/content IDs |
| `quo-vadis-core/.../compose/TreeFlattener.kt` | Modify | Implement `flattenTab()` method |
| `quo-vadis-core/.../compose/RenderableSurface.kt` | Reference | Uses TAB_WRAPPER and TAB_CONTENT modes |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| RENDER-002A (Core flattenState) | Hard | Must complete first |
| RENDER-008 (Legacy Migration) | Soft | Informs wrapper pattern |
| RENDER-001 (RenderableSurface) | Reference | Uses surface types |

---

## Acceptance Criteria

- [ ] TAB_WRAPPER surface created for user's wrapper composable
- [ ] TAB_CONTENT surface created for active tab content
- [ ] `parentWrapperId` links content surface to wrapper surface
- [ ] `previousSurfaceId` tracks previous tab content for animations
- [ ] CachingHints distinguish wrapper vs content IDs
- [ ] Intra-tab navigation only caches content (not wrapper)
- [ ] Cross-node navigation caches whole wrapper
- [ ] AnimationPair generated for tab switch transitions
- [ ] AnimationPair generated for cross-node transitions
- [ ] Wrapper content integrates user's composable with slot
- [ ] Nested stack content properly flattened
- [ ] Comprehensive KDoc documentation
- [ ] Unit tests for tab wrapper surface creation
- [ ] Unit tests for tab content surface creation
- [ ] Unit tests for intra-tab caching strategy
- [ ] Unit tests for cross-node caching strategy
- [ ] Unit tests for animation pair generation
- [ ] Code compiles on all target platforms

---

## Testing Notes

```kotlin
class TreeFlattenerTabTest {

    private val mockContentResolver = TreeFlattener.ContentResolver { {} }
    private val flattener = TreeFlattener(mockContentResolver)

    @Test
    fun `flattenTab produces wrapper and content surfaces`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile")))
        val tab = TabNode("tabs", null, listOf(stack1, stack2), activeStackIndex = 1)
        
        val result = flattener.flattenState(tab)
        
        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_WRAPPER }
        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }
        
        assertNotNull(wrapperSurface)
        assertNotNull(contentSurface)
        assertEquals("tabs-wrapper", wrapperSurface.id)
        assertEquals("tabs-content-1", contentSurface.id)
    }

    @Test
    fun `content surface has parentWrapperId pointing to wrapper`() {
        val stack = StackNode("home-stack", "tabs", listOf(mockScreen("home")))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)
        
        val result = flattener.flattenState(tab)
        
        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }
        
        assertNotNull(contentSurface)
        assertEquals("tabs-wrapper", contentSurface.parentWrapperId)
    }

    @Test
    fun `tab switch populates previousSurfaceId for animation`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile")))
        val tab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 1,
            previousTabIndex = 0
        )
        
        val result = flattener.flattenState(tab)
        
        val contentSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_CONTENT }
        
        assertNotNull(contentSurface)
        assertEquals("tabs-content-0", contentSurface.previousSurfaceId)
    }

    @Test
    fun `tab switch generates AnimationPair with TAB_SWITCH type`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile")))
        val tab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 1,
            previousTabIndex = 0
        )
        
        val result = flattener.flattenState(tab)
        
        val animationPair = result.animationPairs.find { it.transitionType == TransitionType.TAB_SWITCH }
        
        assertNotNull(animationPair)
        assertEquals("tabs-content-1", animationPair.currentId)
        assertEquals("tabs-content-0", animationPair.previousId)
    }

    @Test
    fun `intra-tab navigation only caches content not wrapper`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile")))
        val tab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 1,
            previousTabIndex = 0
        )
        
        val result = flattener.flattenState(tab)
        
        assertFalse(result.cachingHints.shouldCacheWrapper)
        assertTrue(result.cachingHints.shouldCacheContent)
        assertTrue(result.cachingHints.contentIds.contains("tabs-content-1"))
    }

    @Test
    fun `cross-node navigation caches whole wrapper`() {
        val stack = StackNode("stack", null, listOf(mockScreen("screen1")))
        val tab = TabNode("tabs", null, listOf(
            StackNode("home-stack", "tabs", listOf(mockScreen("home")))
        ), activeStackIndex = 0)
        
        // Simulate navigation from stack to tab
        val previousResult = flattener.flattenState(stack)
        val result = flattener.flattenState(tab, previousRoot = stack)
        
        assertTrue(result.cachingHints.shouldCacheWrapper)
        assertTrue(result.cachingHints.isCrossNodeTypeNavigation)
        assertTrue(result.cachingHints.wrapperIds.contains("tabs-wrapper"))
    }

    @Test
    fun `CachingHints contains separate wrapper and content IDs`() {
        val stack = StackNode("home-stack", "tabs", listOf(mockScreen("home")))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)
        
        val result = flattener.flattenState(tab)
        
        assertTrue(result.cachingHints.wrapperIds.isNotEmpty())
        assertTrue(result.cachingHints.contentIds.isNotEmpty())
        assertFalse(result.cachingHints.wrapperIds.intersect(result.cachingHints.contentIds).isNotEmpty())
    }

    @Test
    fun `wrapper surface has no animation for intra-tab navigation`() {
        val stack1 = StackNode("home-stack", "tabs", listOf(mockScreen("home")))
        val stack2 = StackNode("profile-stack", "tabs", listOf(mockScreen("profile")))
        val tab = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack1, stack2),
            activeStackIndex = 1,
            previousTabIndex = 0
        )
        
        val result = flattener.flattenState(tab)
        
        val wrapperSurface = result.surfaces.find { it.renderingMode == SurfaceRenderingMode.TAB_WRAPPER }
        
        assertNotNull(wrapperSurface)
        assertEquals(SurfaceAnimationSpec.None, wrapperSurface.animationSpec)
    }

    @Test
    fun `nested stack content is properly flattened`() {
        val screen1 = mockScreen("screen1")
        val screen2 = mockScreen("screen2")
        val stack = StackNode("home-stack", "tabs", listOf(screen1, screen2))
        val tab = TabNode("tabs", null, listOf(stack), activeStackIndex = 0)
        
        val result = flattener.flattenState(tab)
        
        // Should have wrapper + content + nested screen
        assertTrue(result.surfaces.size >= 2)
        
        val screenSurface = result.surfaces.find { it.id == "screen2" }
        assertNotNull(screenSurface)
        assertEquals(SurfaceRenderingMode.STACK_CONTENT, screenSurface.renderingMode)
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
- *This task replaced the original combined RENDER-002 task*
- [RENDER-004](./RENDER-004-quovadis-host.md) - Consumer of flattened surfaces
- [RENDER-008](./RENDER-008-legacy-migration.md) - Legacy migration patterns

````
