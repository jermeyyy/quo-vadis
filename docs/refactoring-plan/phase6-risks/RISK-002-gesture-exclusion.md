# Task RISK-002: Gesture Exclusion Modifier

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | RISK-002 |
| **Name** | System Gesture Conflict Resolution |
| **Phase** | 6 - Risk Mitigation |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | None |

## Risk Being Mitigated

**System Gesture Conflicts**: Predictive back handler conflicts with swipeable components (Maps, HorizontalPager).

## Implementation

```kotlin
// quo-vadis-core/src/androidMain/kotlin/.../GestureExclusionModifier.kt

fun Modifier.excludeFromBackGesture(): Modifier = this.then(
    Modifier.onGloballyPositioned { coordinates ->
        val view = (LocalView.current as? View)
        view?.systemGestureExclusionRects = listOf(
            coordinates.boundsInWindow().toAndroidRect()
        )
    }
)

// commonMain - no-op on non-Android
expect fun Modifier.excludeFromBackGesture(): Modifier
```

## Usage

```kotlin
HorizontalPager(
    modifier = Modifier.excludeFromBackGesture()
) { ... }
```

## Acceptance Criteria

- [ ] Modifier excludes region on Android
- [ ] No-op on other platforms
- [ ] Works with Maps, Pagers, carousels
