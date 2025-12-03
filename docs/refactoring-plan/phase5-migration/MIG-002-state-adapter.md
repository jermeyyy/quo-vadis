# Task MIG-002: Navigator State Adapter

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | MIG-002 |
| **Name** | Navigator.state Extension Property |
| **Phase** | 5 - Migration Utilities |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | MIG-001 |

## Overview

Add `navigator.state: StateFlow<NavNode>` extension that wraps the legacy backStack, enabling code to use the new API while the underlying implementation migrates.

## Implementation

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../migration/NavigatorExtensions.kt

/**
 * Provides NavNode state view of legacy Navigator.
 * Use during migration period.
 */
@Deprecated(
    message = "Use Navigator.state directly after migration",
    level = DeprecationLevel.WARNING
)
val Navigator.stateCompat: StateFlow<NavNode>
    get() = backStackFlow.map { entries ->
        entries.toStackNode()
    }.stateIn(
        scope = navigatorScope,
        started = SharingStarted.Eagerly,
        initialValue = backStack.toStackNode()
    )
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-core/src/commonMain/kotlin/.../migration/NavigatorExtensions.kt` | New |

## Acceptance Criteria

- [ ] StateFlow updates on navigation changes
- [ ] Correctly wraps backStack as NavNode tree
- [ ] Deprecation warning guides migration
