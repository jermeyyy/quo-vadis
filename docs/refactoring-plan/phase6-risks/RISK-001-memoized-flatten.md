# Task RISK-001: Memoized Tree Flattening

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | RISK-001 |
| **Name** | Performance-Optimized flattenState |
| **Phase** | 6 - Risk Mitigation |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-002 (flattenState) |

## Risk Being Mitigated

**Performance Overhead**: Flattening a large tree on every frame could cause jank.

## Implementation

```kotlin
@Composable
fun QuoVadisHost(navigator: Navigator) {
    val state by navigator.state.collectAsState()
    
    // Memoize flattening - only recalculate when state changes
    val visibleSurfaces = remember(state) {
        flattenState(state)
    }
    
    // Or use derivedStateOf for fine-grained reactivity
    val surfaces by remember {
        derivedStateOf { flattenState(navigator.state.value) }
    }
}
```

## Additional Optimizations

1. **Structural Sharing**: Kotlin data classes already provide this
2. **Shallow Comparison**: Compare tree roots before deep traversal
3. **Incremental Flattening**: Only re-flatten changed subtrees

## Acceptance Criteria

- [ ] flattenState memoized with remember(state)
- [ ] Benchmark shows <1ms flatten time for 100+ nodes
- [ ] No jank during rapid navigation
