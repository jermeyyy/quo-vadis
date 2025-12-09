# HIER-025: Rendering Mode Feature Flag

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-025 |
| **Task Name** | Add Rendering Mode Feature Flag |
| **Phase** | Phase 4: Integration |
| **Complexity** | Small |
| **Estimated Time** | 0.5 day |
| **Dependencies** | HIER-024 |
| **Blocked By** | HIER-024 |
| **Blocks** | HIER-028 |

---

## Overview

Add a feature flag to switch between the existing `QuoVadisHost` (flattened) and the new `HierarchicalQuoVadisHost` rendering systems. This enables gradual rollout and A/B testing.

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/RenderingMode.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Rendering mode for the navigation host.
 */
enum class RenderingMode {
    /**
     * Legacy flattened rendering (RenderableSurface-based).
     */
    Flattened,
    
    /**
     * New hierarchical rendering (NavTreeRenderer-based).
     */
    Hierarchical
}

/**
 * CompositionLocal for the current rendering mode.
 *
 * Default is [RenderingMode.Hierarchical] for new projects.
 */
val LocalRenderingMode: ProvidableCompositionLocal<RenderingMode> =
    staticCompositionLocalOf { RenderingMode.Hierarchical }

/**
 * Smart host that chooses renderer based on [LocalRenderingMode].
 *
 * This is the recommended entry point during migration.
 *
 * @param navigator Navigator instance
 * @param modifier Modifier to apply
 * @param renderingMode Override rendering mode (uses LocalRenderingMode if null)
 */
@Composable
fun QuoVadisNavHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    renderingMode: RenderingMode? = null
) {
    val mode = renderingMode ?: LocalRenderingMode.current
    
    when (mode) {
        RenderingMode.Flattened -> {
            QuoVadisHost(
                navigator = navigator,
                modifier = modifier
            )
        }
        RenderingMode.Hierarchical -> {
            HierarchicalQuoVadisHost(
                navigator = navigator,
                modifier = modifier
            )
        }
    }
}
```

---

## Usage

```kotlin
// Use hierarchical (default)
QuoVadisNavHost(navigator = navigator)

// Force flattened (legacy)
QuoVadisNavHost(
    navigator = navigator,
    renderingMode = RenderingMode.Flattened
)

// Set app-wide default
CompositionLocalProvider(LocalRenderingMode provides RenderingMode.Flattened) {
    QuoVadisNavHost(navigator = navigator)
}
```

---

## Acceptance Criteria

- [ ] `RenderingMode` enum with `Flattened` and `Hierarchical`
- [ ] `LocalRenderingMode` CompositionLocal
- [ ] `QuoVadisNavHost` composable dispatching based on mode
- [ ] Default is `Hierarchical`
- [ ] KDoc documentation
