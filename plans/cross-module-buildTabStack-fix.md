# Fix: Cross-Module Container Resolution in DslNavigationConfig.buildTabStack()

## Problem Statement

When `DslNavigationConfig.buildTabStack()` encounters a `TabEntry.ContainerReference`, it calls `this.buildNavNode(tabEntry.containerClass, ...)`. Since `this` is a specific `DslNavigationConfig` instance (not the `CompositeNavigationConfig`), cross-module container references fail — returning `null` because the referenced container is registered in a different config.

### Failing flow (initial tree construction)

```text
CompositeNavigationConfig.buildNavNode(MainTabs::class)
  → delegates to ConfigA.buildNavNode(MainTabs::class)
    → buildTabNode() → buildTabStack() for each tab entry
      → TabEntry.ContainerReference(Feature2Stack::class)
        → this.buildNavNode(Feature2Stack::class)   ← this = ConfigA!
          → ConfigA.containers[Feature2Stack] → null  ← Feature2Stack is in ConfigB
          → returns null → empty children list
```

### Working flow (rendering path)

`CompositeContainerRegistry.wrapContainerInfo()` replaces the builder functions to call `navNodeBuilder()` (the composite's `::buildNavNode`), so resolution works at rendering time. This proves the composite's `buildNavNode` has correct cross-config resolution — the issue is specifically that `buildTabStack` bypasses it.

## Investigation Findings

### Scope of the bug

- **Only `buildTabStack()`** is affected — it's the only internal method that calls `buildNavNode()` recursively for child container references.
- **`buildPaneConfigurations()`** is NOT affected — `PaneEntry` is a simple data class with `rootDestination`; it has no `ContainerReference` variant and never calls `buildNavNode()`.
- **`buildStackChildren()`** is NOT affected — creates `ScreenNode` directly, no container lookups.

### Files affected (two parallel hierarchies)

| Public API | Internal/KSP API |
|-----------|-----------------|
| `quo-vadis-core/.../dsl/DslNavigationConfig.kt` | `quo-vadis-core/.../dsl/internal/DslNavigationConfig.kt` |
| `quo-vadis-core/.../config/CompositeNavigationConfig.kt` | `quo-vadis-core/.../internal/config/CompositeNavigationConfig.kt` |

Both DslNavigationConfig copies are `internal class`. Both CompositeNavigationConfig copies are `public class`.  
A composite tree can mix both variants (e.g., `publicDslConfig + kspGeneratedConfig`), since both implement the shared `NavigationConfig` interface.

### Infinite recursion risk

Naively putting the fallback in `buildNavNode()` causes infinite recursion:

```text
configA.buildNavNode(X) → containers[X] null → nodeResolver(X)
  → composite.buildNavNode(X) → configB.buildNavNode(X) → containers[X] null → nodeResolver(X)
    → composite.buildNavNode(X) → configA.buildNavNode(X) → ...  ← INFINITE LOOP
```

This happens when the destination isn't found in ANY config. **The fallback must be placed only in `buildTabStack()`**, not in `buildNavNode()` itself.

## Recommended Approach

**Option A (targeted): Property-based node resolver, used only in `buildTabStack()`**

### Rationale

- **Eliminates infinite recursion** by keeping `buildNavNode()` unchanged — it still returns `null` for unknown destinations.
- **Minimal footprint**: 1 property + 1 interface method with default no-op + targeted usage in 1 call site.
- **Handles nesting**: Recursive propagation via interface method works for `(c1 + c2) + c3 + c4` etc.
- **Handles mixed variants**: Interface method avoids type-checking across public/internal DslNavigationConfig variants.
- **No API break**: Default no-op on `NavigationConfig` is binary-compatible; `@InternalQuoVadisApi` hides it from consumers.

### Rejected alternatives

| Approach | Why rejected |
|----------|-------------|
| **Fallback in `buildNavNode()`** | Infinite recursion when destination not found in any config |
| **Thread resolver through method signatures (Option B)** | Cascading changes to private `buildTabNode` + `buildTabStack` signatures; `buildNavNode` is an interface override, can't add parameters |
| **Wrapper configs (Option D)** | Can't intercept internal `this.buildNavNode()` calls from within DslNavigationConfig |

## Exact Changes

### 1. `NavigationConfig` interface

**File:** `quo-vadis-core/.../navigation/config/NavigationConfig.kt`

Add method with default no-op:

```kotlin
/**
 * Sets a fallback node resolver for cross-config destination resolution.
 *
 * Used internally by [CompositeNavigationConfig] to enable container references
 * that span multiple combined configs. The resolver is called when a local
 * container lookup fails during tree construction.
 *
 * @param resolver Function to resolve destinations not found in this config,
 *   or null to clear the resolver
 */
@InternalQuoVadisApi
fun setNodeResolver(resolver: ((KClass<out NavDestination>, String?, String?) -> NavNode?)?) {
    // Default no-op for implementations that don't need cross-config resolution
}
```

### 2. Both `DslNavigationConfig` copies

**Files:**
- `quo-vadis-core/.../dsl/DslNavigationConfig.kt`
- `quo-vadis-core/.../dsl/internal/DslNavigationConfig.kt`

**Add property** (inside the class body, before `screenRegistry`):

```kotlin
/**
 * Fallback resolver for cross-config container reference resolution.
 * Set by [CompositeNavigationConfig] to enable cross-module tab container references.
 */
private var nodeResolver: ((KClass<out NavDestination>, String?, String?) -> NavNode?)? = null

@InternalQuoVadisApi
override fun setNodeResolver(
    resolver: ((KClass<out NavDestination>, String?, String?) -> NavNode?)?
) {
    nodeResolver = resolver
}
```

**Modify `buildTabStack()`** — change the `TabEntry.ContainerReference` branch:

```kotlin
is TabEntry.ContainerReference -> {
    // For container references, we need to build the referenced container.
    // Try local lookup first, then fall back to the cross-config resolver
    // (set by CompositeNavigationConfig) for cross-module references.
    val containerNode = buildNavNode(
        tabEntry.containerClass,
        stackKey,
        tabNodeKey
    ) ?: nodeResolver?.invoke(
        tabEntry.containerClass,
        stackKey,
        tabNodeKey
    )
    if (containerNode != null) {
        listOf(containerNode)
    } else {
        emptyList()
    }
}
```

**`buildNavNode()` is NOT modified** — it continues to return `null` for unknown destinations, avoiding infinite recursion.

### 3. Both `CompositeNavigationConfig` copies

**Files:**
- `quo-vadis-core/.../config/CompositeNavigationConfig.kt`
- `quo-vadis-core/.../internal/config/CompositeNavigationConfig.kt`

**Add `setNodeResolver` override and init block** (after property declarations):

```kotlin
@InternalQuoVadisApi
override fun setNodeResolver(
    resolver: ((KClass<out NavDestination>, String?, String?) -> NavNode?)?
) {
    primary.setNodeResolver(resolver)
    secondary.setNodeResolver(resolver)
}

init {
    @OptIn(InternalQuoVadisApi::class)
    primary.setNodeResolver(::buildNavNode)
    @OptIn(InternalQuoVadisApi::class)
    secondary.setNodeResolver(::buildNavNode)
}
```

### How nested composites work

For `(configA + configB) + configC`:

1. **Inner composite created** (`configA + configB`):
   - `init` sets `configA.nodeResolver = innerComposite::buildNavNode`
   - `init` sets `configB.nodeResolver = innerComposite::buildNavNode`

2. **Outer composite created** (`inner + configC`):
   - `init` calls `primary.setNodeResolver(outerComposite::buildNavNode)` → inner composite propagates to `configA` and `configB`, overwriting inner resolver
   - `init` calls `secondary.setNodeResolver(outerComposite::buildNavNode)` → `configC` gets outer resolver

3. **Final state**: All leaf configs' `nodeResolver` points to the outermost composite's `buildNavNode`, which can resolve any destination across all configs.

## Edge Cases & Risks

| Edge case | Handling |
|-----------|---------|
| Destination not in any config | `buildNavNode` returns null → `nodeResolver` returns null → `emptyList()` children. No crash, no infinite loop. |
| Single config (no composite) | `nodeResolver` stays `null`, fallback branch is skipped. Zero behavioral change. |
| Config reused in multiple composites | Last composite's init wins (overwrites resolver). **Acceptable** — reusing a config in multiple composites is an unusual pattern; document as unsupported. |
| Thread safety | `nodeResolver` is set during composite construction (single thread) and read during `buildNavNode` (could be another thread). For safety, consider using `@Volatile` or `AtomicReference`. In practice, config construction and first `buildNavNode` call don't overlap. |
| Future `PaneEntry.ContainerReference` | If added, same pattern applies: add resolver fallback in `buildPaneConfigurations` at the specific call site. |

## Checklist

- [ ] Add `setNodeResolver` to `NavigationConfig` interface (default no-op, `@InternalQuoVadisApi`)
- [ ] Add `nodeResolver` property + `setNodeResolver` override to **public** `DslNavigationConfig`
- [ ] Add `nodeResolver` property + `setNodeResolver` override to **internal** `DslNavigationConfig`
- [ ] Modify `buildTabStack` in **public** `DslNavigationConfig` to use `nodeResolver` fallback
- [ ] Modify `buildTabStack` in **internal** `DslNavigationConfig` to use `nodeResolver` fallback
- [ ] Add `setNodeResolver` override + `init` block to **public** `CompositeNavigationConfig`
- [ ] Add `setNodeResolver` override + `init` block to **internal** `CompositeNavigationConfig`
- [ ] Add test: cross-module `TabEntry.ContainerReference` resolves correctly in composite config
- [ ] Add test: unknown destination in ContainerReference returns empty children (no crash/loop)
- [ ] Add test: single config (non-composite) continues to work as before
- [ ] Add test: nested composites `(A + B) + C` resolve correctly
