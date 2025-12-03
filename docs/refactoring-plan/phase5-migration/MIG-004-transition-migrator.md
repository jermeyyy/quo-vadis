# Task MIG-004: Transition to AnimationRegistry Migrator

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | MIG-004 |
| **Name** | Per-Screen Transition Migration Tool |
| **Phase** | 5 - Migration Utilities |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-006 (AnimationRegistry) |

## Overview

Create tool to extract per-screen transitions from existing code and generate `AnimationRegistry` configuration.

## Implementation

KSP processor scans for existing transition annotations and generates registry calls:

```kotlin
// Generated output
fun registerMigratedTransitions(registry: AnimationRegistry) {
    // From: destination(Screen1, enterTransition = slideIn)
    registry.register(Screen1::class, Direction.FORWARD, slideInSpec)
}
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-ksp/src/main/kotlin/.../TransitionMigrationGenerator.kt` | New |

## Acceptance Criteria

- [ ] Scans existing @Route for transition info
- [ ] Generates AnimationRegistry.register() calls
- [ ] Provides migration report
