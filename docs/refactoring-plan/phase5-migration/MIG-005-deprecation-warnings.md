# Task MIG-005: Type-Safe Deprecation Warnings

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | MIG-005 |
| **Name** | Compile-Time Migration Hints |
| **Phase** | 5 - Migration Utilities |
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Dependencies** | All migration adapters |

## Overview

Add `@Deprecated` annotations with `ReplaceWith` hints for all migrating APIs, enabling IDE quick-fix support.

## Implementation

```kotlin
@Deprecated(
    message = "Use navigator.push(destination) instead",
    replaceWith = ReplaceWith(
        "navigator.push(destination)",
        "com.jermey.quo.vadis.core.navigation.core.push"
    ),
    level = DeprecationLevel.WARNING
)
fun Navigator.navigateTo(destination: Destination) { ... }
```

## Deprecation Levels Timeline

| Version | Level | User Experience |
|---------|-------|-----------------|
| 2.0 | WARNING | Yellow highlight, suggestion |
| 3.0 | WARNING | Same + release notes |
| 4.0 | ERROR | Red error, must fix |
| 5.0 | Hidden/Removed | Compilation fails |

## Files Affected

Multiple files across quo-vadis-core

## Acceptance Criteria

- [ ] All deprecated APIs have ReplaceWith
- [ ] IDE shows quick-fix suggestions
- [ ] Documentation links included
