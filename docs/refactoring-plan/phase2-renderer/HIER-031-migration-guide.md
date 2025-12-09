# HIER-031: Migration Guide

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-031 |
| **Task Name** | Write Migration Guide |
| **Phase** | Phase 5: Migration |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | HIER-029, HIER-030 |
| **Blocked By** | HIER-029, HIER-030 |
| **Blocks** | HIER-032 |

---

## Overview

Comprehensive migration guide for users upgrading from the flattened rendering system to hierarchical rendering.

---

## File Location

```
quo-vadis-core/docs/MIGRATION_HIERARCHICAL_RENDERING.md
```

---

## Content Outline

```markdown
# Migrating to Hierarchical Rendering

## Overview

Version X.Y.Z introduces hierarchical rendering, replacing the previous flattened
`RenderableSurface` approach. This provides:

- ✅ Proper wrapper/content relationships
- ✅ Correct predictive back for tabs and panes
- ✅ Simplified shared element transitions
- ✅ Better performance via composable caching

## Quick Migration

**Before:**
\```kotlin
QuoVadisHost(
    navigator = navigator,
    modifier = Modifier.fillMaxSize()
)
\```

**After:**
\```kotlin
HierarchicalQuoVadisHost(
    navigator = navigator,
    modifier = Modifier.fillMaxSize()
)
\```

## Migrating Tab Wrappers

### Before: Runtime Registration

\```kotlin
@Composable
fun MyApp() {
    val navigator = rememberNavigator(...)
    
    // Wrapper was separate from content
    MyTabBar(navigator)
    
    QuoVadisHost(navigator)
}
\```

### After: Annotation-Based

\```kotlin
@TabWrapper(MainTabs::class)
@Composable
fun MainTabsWrapper(
    scope: TabWrapperScope,
    tabContent: @Composable () -> Unit  // Content is a slot!
) {
    Scaffold(
        bottomBar = { MyTabBar(scope) }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            tabContent()  // Content renders INSIDE wrapper
        }
    }
}

@Composable
fun MyApp() {
    HierarchicalQuoVadisHost(
        navigator = navigator,
        wrapperRegistry = GeneratedWrapperRegistry
    )
}
\```

## Migrating Pane Wrappers

Similar pattern - wrapper now contains content as a slot.

## Handling Breaking Changes

### Predictive Back Behavior

Predictive back now transforms the ENTIRE tab subtree (wrapper + content).
If you had custom transforms on content only, adjust your wrapper.

### Animation Timing

Animations now respect wrapper/content hierarchy. If you had timing
adjustments, they may need updating.

### Shared Elements

Shared elements now work more naturally across any screens.
The `SharedTransitionScope` is provided at the root.

## Feature Flag

For gradual migration:

\```kotlin
CompositionLocalProvider(
    LocalRenderingMode provides RenderingMode.Flattened  // Use old system
) {
    QuoVadisNavHost(navigator)  // Picks renderer based on mode
}
\```

## Troubleshooting

### Content Not Appearing in Tabs

Ensure you're rendering `tabContent()` inside your wrapper.

### Animations Missing

Check that your wrapper doesn't interfere with `AnimatedContent`.

### Shared Elements Not Working

Verify you're using `LocalAnimatedVisibilityScope.current`.

## Timeline

- **v1.0.0**: Hierarchical rendering introduced, flattened deprecated
- **v1.1.0**: Flattened rendering requires opt-in
- **v2.0.0**: Flattened rendering removed
```

---

## Acceptance Criteria

- [ ] Complete migration guide document
- [ ] Before/after code examples
- [ ] Tab wrapper migration section
- [ ] Pane wrapper migration section
- [ ] Breaking changes documented
- [ ] Feature flag usage explained
- [ ] Troubleshooting section
- [ ] Deprecation timeline
- [ ] Link from deprecated APIs
