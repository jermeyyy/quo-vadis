# Task TEST-006: Multiplatform Compatibility Tests

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | TEST-006 |
| **Name** | Cross-Platform Test Suite |
| **Phase** | 8 - Testing Infrastructure |
| **Complexity** | High |
| **Estimated Time** | 4 days |
| **Dependencies** | All core implementations |

## Overview

Ensure new architecture works across Android, iOS, Desktop, and Web.

## Test Categories

### Common Tests (commonTest)

```kotlin
class NavNodeCommonTest {
    @Test
    fun `NavNode operations work on all platforms`() {
        val root = StackNode("root", null, emptyList())
        val mutated = TreeMutator.push(root, HomeDestination)
        assertThat(mutated.children).hasSize(1)
    }
}
```

### Platform-Specific Tests

```kotlin
// androidTest
class AndroidNavigationTest {
    @Test
    fun `SavedStateHandle integration works`() { ... }
    
    @Test
    fun `PredictiveBackHandler integration works`() { ... }
}

// iosTest
class IosNavigationTest {
    @Test
    fun `swipe back gesture works`() { ... }
}

// jsTest
class WebNavigationTest {
    @Test
    fun `browser back button works`() { ... }
}

// desktopTest
class DesktopNavigationTest {
    @Test
    fun `keyboard shortcuts work`() { ... }
}
```

## Test Matrix

| Feature | Android | iOS | Desktop | Web |
|---------|---------|-----|---------|-----|
| NavNode serialization | ✅ | ✅ | ✅ | ✅ |
| TreeMutator operations | ✅ | ✅ | ✅ | ✅ |
| Predictive back | ✅ | ✅ | N/A | N/A |
| State restoration | ✅ | ✅ | ✅ | ✅ |
| SharedElement | ✅ | ✅ | ✅ | ✅ |

## Acceptance Criteria

- [ ] NavNode serialization tested on all platforms
- [ ] Platform-specific gesture handling tested
- [ ] SavedStateHandle alternatives tested
- [ ] expect/actual pattern verified
