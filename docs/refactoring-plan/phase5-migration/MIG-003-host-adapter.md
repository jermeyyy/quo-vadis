# Task MIG-003: GraphNavHost to QuoVadisHost Adapter

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | MIG-003 |
| **Name** | GraphNavHost Compatibility Wrapper |
| **Phase** | 5 - Migration Utilities |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-004 (QuoVadisHost) |

## Overview

Create adapter allowing existing `GraphNavHost` calls to delegate to `QuoVadisHost` internally, enabling gradual UI migration.

## Implementation

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../migration/GraphNavHostCompat.kt

@Deprecated(
    message = "Use QuoVadisHost directly",
    replaceWith = ReplaceWith("QuoVadisHost(navigator)")
)
@Composable
fun GraphNavHost(
    navigator: Navigator,
    graph: NavigationGraph,
    modifier: Modifier = Modifier
) {
    // Convert legacy graph to NavNode
    val rootNode = remember(graph) { graph.toNavNode() }
    
    QuoVadisHost(
        navigator = navigator,
        rootNode = rootNode,
        modifier = modifier
    )
}
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-core/src/commonMain/kotlin/.../migration/GraphNavHostCompat.kt` | New |

## Acceptance Criteria

- [ ] Matches existing GraphNavHost signature
- [ ] Internally uses QuoVadisHost
- [ ] Deprecation with ReplaceWith for IDE support
