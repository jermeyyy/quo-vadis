# Multiplatform Predictive Back Implementation

## Summary

Successfully refactored the predictive back animation implementation to work on **both iOS and Android** using the Compose Multiplatform `PredictiveBackHandler` API.

## Changes Made

### 1. Created Multiplatform Implementation
**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/navigation/compose/PredictiveBackNavigation.kt`

- Uses `androidx.compose.ui.backhandler.PredictiveBackHandler` (multiplatform API)
- Supports both Android 13+ and iOS predictive back gestures
- Implements proper z-index layering:
  - **Top (z-index 1.0)**: Current screen with animation (no scrim)
  - **Middle (z-index 0.5)**: Black scrim fading from opaque → transparent
  - **Bottom (z-index 0.0)**: Previous screen (visible beneath)

### 2. Updated Platform-Specific Hosts
Both Android and iOS implementations now use the same multiplatform `PredictiveBackNavigation`:

- `composeApp/src/androidMain/kotlin/.../PlatformAwareNavHost.android.kt`
- `composeApp/src/iosMain/kotlin/.../PlatformAwareNavHost.ios.kt`

### 3. Removed Old Android-Specific Code
- Deleted `composeApp/src/androidMain/kotlin/.../android/PredictiveBackHandler.kt`
- No longer needed as we have a unified multiplatform implementation

## Features

### Animation Types
Three built-in animation types:
1. **Material3** (default): Scale + translate + rounded corners + shadow
2. **Scale**: Simple scale down with fade
3. **Slide**: Slide right with fade

### Configuration Options
- `enabled`: Enable/disable predictive back
- `animationType`: Choose animation style
- `sensitivity`: Adjust gesture sensitivity (multiplier)
- `customAnimation`: Provide custom animation callback

### Correct Visual Behavior
✅ Current screen scales without scrim on top  
✅ Scrim positioned between screens (z-index 0.5)  
✅ Scrim animates from 50% opacity → transparent  
✅ Previous screen content visible beneath scrim  

## Usage

Already integrated in `DemoApp.kt`:
```kotlin
PlatformAwareNavHost(
    graph = mainBottomNavGraph(),
    navigator = navigator,
    modifier = Modifier,
    defaultTransition = NavigationTransitions.Fade,
    enablePredictiveBack = true  // Works on both iOS and Android!
)
```

## Platform Support

- **Android 13+ (API 33+)**: Full predictive back gesture support
- **iOS**: Native iOS back gesture support with same animations
- **Older Android**: Gracefully falls back to standard navigation

## Technical Details

The implementation uses:
- `@OptIn(ExperimentalComposeUiApi::class)` for multiplatform back handler
- `backEvent.collect { }` to track gesture progress (changed from `collectLatest` for multiplatform compatibility)
- `graphicsLayer` for performant animations
- Proper state management to prevent animation glitches

