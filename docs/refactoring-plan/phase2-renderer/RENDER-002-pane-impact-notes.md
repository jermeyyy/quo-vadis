# RENDER-002 Impact Notes: PaneNode Enhancement Effects

## Overview

This document describes the impact of CORE-001 `PaneNode` enhancements on RENDER-002 (flattenState Algorithm).

**Related Changes:** CORE-001 `PaneNode` redesign with role-based pane configurations and adaptation strategies.

**Critical Insight:** The `flattenState` algorithm is where LOGICAL navigation state meets VISUAL layout. This is where `WindowSizeClass` is consumed and `AdaptStrategy` is applied.

---

## Key Design Principle

> **NavNode stores LOGICAL state. flattenState produces VISUAL state.**

The `PaneNode` stores:
- Which panes exist (`paneConfigurations`)
- Which pane has navigation focus (`activePaneRole`)
- How each pane SHOULD adapt (`AdaptStrategy`)

The `flattenState` algorithm determines:
- Which panes are actually VISIBLE (based on `WindowSizeClass`)
- How panes are POSITIONED (side-by-side, stacked, levitated)
- Z-ordering for animations and shared elements

---

## Enhanced RenderableSurface

### Additional Properties for Pane Rendering

```kotlin
/**
 * Represents a flattened surface ready for rendering.
 * Enhanced with pane-specific information for adaptive layouts.
 */
data class RenderableSurface(
    // ═══════════════════════════════════════════════════════════════════
    // EXISTING PROPERTIES
    // ═══════════════════════════════════════════════════════════════════
    
    /** The NavNode this surface represents */
    val node: NavNode,
    
    /** Z-index for layering (higher = on top) */
    val zIndex: Int,
    
    /** Whether this surface is the "active" one receiving gestures */
    val isActive: Boolean,
    
    // ═══════════════════════════════════════════════════════════════════
    // NEW: PANE-SPECIFIC PROPERTIES
    // ═══════════════════════════════════════════════════════════════════
    
    /** 
     * Adapted visibility state of this surface.
     * Determines how the surface should be rendered.
     */
    val adaptedValue: PaneAdaptedValue,
    
    /**
     * Pane role if this surface originated from a PaneNode.
     * Null for non-pane surfaces (StackNode, TabNode children).
     */
    val paneRole: PaneRole?,
    
    /**
     * Layout hints for the renderer.
     */
    val layoutHints: PaneLayoutHints?
)

/**
 * Adapted visibility state for a pane.
 * Mirrors Material3 adaptive states.
 */
enum class PaneAdaptedValue {
    /** Pane is fully visible in its designated space */
    Expanded,
    
    /** Pane is not rendered */
    Hidden,
    
    /** Pane is shown as an overlay/modal (from AdaptStrategy.Levitate) */
    Levitated,
    
    /** Pane is shown under another pane (from AdaptStrategy.Reflow) */
    Reflowed
}

/**
 * Layout hints for positioning panes.
 */
data class PaneLayoutHints(
    /** Suggested width fraction (0.0-1.0) or null for natural sizing */
    val widthFraction: Float?,
    
    /** Position relative to other panes */
    val position: PanePosition,
    
    /** Whether this pane should receive scrim behind it */
    val showScrim: Boolean = false
)

enum class PanePosition {
    Start,      // Left side (LTR) / Right side (RTL)
    End,        // Right side (LTR) / Left side (RTL)
    Center,     // Centered overlay (for Levitated)
    Below       // Below another pane (for Reflowed on compact)
}
```

---

## flattenState Algorithm Changes

### Input: WindowSizeClass Dependency

```kotlin
/**
 * Flattens the NavNode tree into renderable surfaces.
 *
 * @param root The root NavNode to flatten
 * @param windowSizeClass Current window size class (from platform)
 * @return List of surfaces ready for rendering, ordered by z-index
 */
fun flattenState(
    root: NavNode,
    windowSizeClass: WindowSizeClass
): List<RenderableSurface>
```

### Pane Flattening Logic

```kotlin
private fun flattenPaneNode(
    paneNode: PaneNode,
    windowSizeClass: WindowSizeClass,
    baseZIndex: Int
): List<RenderableSurface> {
    val surfaces = mutableListOf<RenderableSurface>()
    
    // Determine how many panes can be shown based on window size
    val maxVisiblePanes = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 1
        WindowWidthSizeClass.Medium -> 1  // Or 2 with dense mode
        WindowWidthSizeClass.Expanded -> 3
    }
    
    // Calculate adapted values for each configured pane
    val adaptedValues = calculatePaneAdaptations(
        paneNode = paneNode,
        maxVisiblePanes = maxVisiblePanes
    )
    
    // Create surfaces for visible panes
    var currentZIndex = baseZIndex
    
    for ((role, adaptedValue) in adaptedValues) {
        val config = paneNode.paneConfigurations[role] ?: continue
        
        when (adaptedValue) {
            PaneAdaptedValue.Expanded -> {
                surfaces += createExpandedPaneSurface(
                    role = role,
                    content = config.content,
                    isActive = role == paneNode.activePaneRole,
                    zIndex = currentZIndex++,
                    windowSizeClass = windowSizeClass
                )
            }
            
            PaneAdaptedValue.Levitated -> {
                // Levitated panes have higher z-index and scrim
                surfaces += createLevitatedPaneSurface(
                    role = role,
                    content = config.content,
                    isActive = role == paneNode.activePaneRole,
                    zIndex = baseZIndex + 100, // Above all expanded panes
                    windowSizeClass = windowSizeClass
                )
            }
            
            PaneAdaptedValue.Reflowed -> {
                surfaces += createReflowedPaneSurface(
                    role = role,
                    content = config.content,
                    isActive = role == paneNode.activePaneRole,
                    zIndex = currentZIndex++,
                    reflowUnder = PaneRole.Primary
                )
            }
            
            PaneAdaptedValue.Hidden -> {
                // Don't add to surfaces - not rendered
            }
        }
    }
    
    return surfaces
}
```

### Adaptation Calculation Logic

```kotlin
/**
 * Calculates adapted values for each pane based on available space.
 */
private fun calculatePaneAdaptations(
    paneNode: PaneNode,
    maxVisiblePanes: Int
): Map<PaneRole, PaneAdaptedValue> {
    val result = mutableMapOf<PaneRole, PaneAdaptedValue>()
    
    // Priority order: Primary always visible, then active role, then others
    val prioritizedRoles = buildPriorityList(paneNode)
    var remainingSlots = maxVisiblePanes
    
    for (role in prioritizedRoles) {
        val config = paneNode.paneConfigurations[role] ?: continue
        
        if (remainingSlots > 0) {
            result[role] = PaneAdaptedValue.Expanded
            remainingSlots--
        } else {
            // No space - apply adapt strategy
            result[role] = when (config.adaptStrategy) {
                AdaptStrategy.Hide -> PaneAdaptedValue.Hidden
                AdaptStrategy.Levitate -> {
                    // Only levitate if this is the active role
                    if (role == paneNode.activePaneRole) {
                        PaneAdaptedValue.Levitated
                    } else {
                        PaneAdaptedValue.Hidden
                    }
                }
                AdaptStrategy.Reflow -> PaneAdaptedValue.Reflowed
            }
        }
    }
    
    return result
}

private fun buildPriorityList(paneNode: PaneNode): List<PaneRole> {
    // Primary is always first priority
    // Active role (if different from Primary) is second
    // Others follow in enum order
    
    return buildList {
        add(PaneRole.Primary)
        
        if (paneNode.activePaneRole != PaneRole.Primary) {
            add(paneNode.activePaneRole)
        }
        
        PaneRole.entries
            .filter { it != PaneRole.Primary && it != paneNode.activePaneRole }
            .filter { paneNode.paneConfigurations.containsKey(it) }
            .forEach { add(it) }
    }
}
```

---

## Window Size Class Handling

### Size Class to Pane Visibility Mapping

| Window Width | Max Expanded Panes | Typical Layout |
|-------------|-------------------|----------------|
| Compact (<600dp) | 1 | Single pane, others Hidden or Levitated |
| Medium (600-840dp) | 1-2 | Primary + potentially reflowed Supporting |
| Expanded (>840dp) | 2-3 | Side-by-side Primary + Supporting + optional Extra |

### Platform Integration

```kotlin
/**
 * Expected in QuoVadisHost (RENDER-004):
 */
@Composable
fun QuoVadisHost(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    // Observe window size class from platform
    val windowSizeClass = currentWindowSizeClass()
    
    // Collect navigation state
    val navState by navigator.state.collectAsState()
    
    // Flatten with current window size
    val surfaces = remember(navState, windowSizeClass) {
        flattenState(navState, windowSizeClass)
    }
    
    // Render surfaces...
}
```

---

## Updated Acceptance Criteria for RENDER-002

Add these acceptance criteria:

- [ ] `flattenState` accepts `WindowSizeClass` parameter
- [ ] `RenderableSurface` includes `adaptedValue: PaneAdaptedValue`
- [ ] `RenderableSurface` includes `paneRole: PaneRole?`
- [ ] `RenderableSurface` includes `layoutHints: PaneLayoutHints?`
- [ ] `PaneAdaptedValue` enum with Expanded, Hidden, Levitated, Reflowed
- [ ] `calculatePaneAdaptations` correctly prioritizes Primary and active roles
- [ ] AdaptStrategy.Hide results in surface not being created
- [ ] AdaptStrategy.Levitate creates elevated surface with scrim hint
- [ ] AdaptStrategy.Reflow creates surface with Below position
- [ ] Compact window shows only 1 expanded pane
- [ ] Expanded window shows up to 3 expanded panes
- [ ] Unit tests for each window size class scenario
- [ ] Unit tests for each AdaptStrategy
- [ ] Integration test: morphing from compact to expanded

---

## Animation Considerations

### Morphing Transitions

When `WindowSizeClass` changes (e.g., device rotation):

```kotlin
/**
 * The flattenState output may change significantly.
 * QuoVadisHost must animate between layouts:
 *
 * Compact → Expanded:
 * - Hidden Supporting pane becomes Expanded
 * - Animate from single-pane to side-by-side
 *
 * Expanded → Compact:
 * - Supporting pane becomes Hidden or Levitated
 * - Animate from side-by-side to single-pane
 */
```

### Predictive Back with Panes

During predictive back gesture:
1. If popping within a pane → standard back animation
2. If switching panes (back behavior) → cross-fade between panes
3. If morphing due to back → coordinate with window size

---

## Testing Considerations

```kotlin
@Test
fun `flattenState with Compact shows only active pane`() {
    val paneNode = createMasterDetailPaneNode()
    
    val surfaces = flattenState(
        root = paneNode,
        windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Compact,
            heightSizeClass = WindowHeightSizeClass.Medium
        )
    )
    
    assertEquals(1, surfaces.count { it.adaptedValue == PaneAdaptedValue.Expanded })
}

@Test
fun `flattenState with Expanded shows multiple panes`() {
    val paneNode = createMasterDetailPaneNode()
    
    val surfaces = flattenState(
        root = paneNode,
        windowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Expanded,
            heightSizeClass = WindowHeightSizeClass.Medium
        )
    )
    
    assertEquals(2, surfaces.count { it.adaptedValue == PaneAdaptedValue.Expanded })
}

@Test
fun `flattenState applies Levitate strategy for active non-visible pane`() {
    val paneNode = PaneNode(
        key = "panes",
        parentKey = null,
        paneConfigurations = mapOf(
            PaneRole.Primary to PaneConfiguration(primaryContent),
            PaneRole.Supporting to PaneConfiguration(
                content = supportingContent,
                adaptStrategy = AdaptStrategy.Levitate
            )
        ),
        activePaneRole = PaneRole.Supporting // Supporting is active
    )
    
    val surfaces = flattenState(
        root = paneNode,
        windowSizeClass = compactWindowSize // Only 1 slot available
    )
    
    // Primary should be Expanded (always visible)
    // Supporting should be Levitated (active but no space)
    val primarySurface = surfaces.find { it.paneRole == PaneRole.Primary }
    val supportingSurface = surfaces.find { it.paneRole == PaneRole.Supporting }
    
    assertEquals(PaneAdaptedValue.Expanded, primarySurface?.adaptedValue)
    assertEquals(PaneAdaptedValue.Levitated, supportingSurface?.adaptedValue)
    assertTrue(supportingSurface!!.zIndex > primarySurface!!.zIndex)
}
```

---

## References

- [CORE-001](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode hierarchy with enhanced PaneNode
- [RENDER-001](./RENDER-001-renderable-surface.md) - RenderableSurface definition
- [RENDER-002](./RENDER-002-flatten-algorithm.md) - flattenState algorithm task
- [Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Original refactoring plan
