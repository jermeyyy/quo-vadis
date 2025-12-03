# Task RISK-004: API Compatibility Checker

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | RISK-004 |
| **Name** | API Compatibility Checker |
| **Phase** | 6 - Risk Mitigation |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | None |

## Risk Being Mitigated

**Breaking Changes**: Unintentional API breaks during refactoring.

## Implementation

Use Kotlin Binary Compatibility Validator plugin:

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
}

apiValidation {
    ignoredPackages += listOf(
        "com.jermey.quo.vadis.core.navigation.internal"
    )
    nonPublicMarkers += listOf(
        "com.jermey.quo.vadis.annotations.InternalApi"
    )
}
```

Run: `./gradlew apiCheck` to validate, `./gradlew apiDump` to update baseline.

## Acceptance Criteria

- [ ] Binary compatibility plugin configured
- [ ] API dump created before refactor
- [ ] CI fails on unacknowledged breaks
