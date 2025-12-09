# HIER-027: Integration Tests

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-027 |
| **Task Name** | Write Integration Tests |
| **Phase** | Phase 4: Integration |
| **Complexity** | Medium |
| **Estimated Time** | 3-4 days |
| **Dependencies** | HIER-024, HIER-026 |
| **Blocked By** | HIER-024, HIER-026 |
| **Blocks** | HIER-028 |

---

## Overview

Comprehensive integration tests for the hierarchical rendering system, covering all navigation patterns: simple stacks, tabbed navigation, adaptive panes, nested navigation, and predictive back.

---

## File Location

```
quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/HierarchicalRenderingTest.kt
```

---

## Test Categories

### 1. Simple Stack Navigation

```kotlin
class StackNavigationTest {
    
    @Test
    fun `stack renders active screen`() = runComposeTest {
        val navigator = createTestNavigator(
            startDestination = HomeScreen,
            graph = { stack { screen<HomeScreen>(); screen<DetailScreen>() } }
        )
        
        setContent {
            HierarchicalQuoVadisHost(navigator = navigator)
        }
        
        onNodeWithText("Home").assertExists()
    }
    
    @Test
    fun `stack animates push navigation`() = runComposeTest {
        val navigator = createTestNavigator(startDestination = HomeScreen)
        
        setContent {
            HierarchicalQuoVadisHost(navigator = navigator)
        }
        
        // Navigate
        navigator.navigate(DetailScreen)
        
        // During animation, both screens may be visible
        advanceUntilIdle()
        
        onNodeWithText("Detail").assertExists()
    }
    
    @Test
    fun `stack animates pop navigation`() = runComposeTest {
        val navigator = createTestNavigator(startDestination = HomeScreen)
        navigator.navigate(DetailScreen)
        
        setContent {
            HierarchicalQuoVadisHost(navigator = navigator)
        }
        
        navigator.navigateBack()
        advanceUntilIdle()
        
        onNodeWithText("Home").assertExists()
        onNodeWithText("Detail").assertDoesNotExist()
    }
}
```

### 2. Tab Navigation

```kotlin
class TabNavigationTest {
    
    @Test
    fun `tabs render with wrapper`() = runComposeTest {
        val navigator = createTestNavigator(
            graph = {
                tabs {
                    tab { screen<HomeTab>() }
                    tab { screen<ProfileTab>() }
                }
            }
        )
        
        setContent {
            HierarchicalQuoVadisHost(
                navigator = navigator,
                wrapperRegistry = TestTabWrapperRegistry
            )
        }
        
        // Wrapper should be visible
        onNodeWithTag("tab-bar").assertExists()
        // Active tab content
        onNodeWithText("Home Tab").assertExists()
    }
    
    @Test
    fun `tab switching animates within wrapper`() = runComposeTest {
        // ... test that wrapper stays, only content animates
    }
    
    @Test
    fun `tabs preserve state when switching`() = runComposeTest {
        // ... test that tab state survives switching
    }
}
```

### 3. Pane Navigation

```kotlin
class PaneNavigationTest {
    
    @Test
    fun `panes render multi-pane in expanded`() = runComposeTest {
        // Test with WindowSizeClass.Expanded
    }
    
    @Test
    fun `panes render single-pane in compact`() = runComposeTest {
        // Test with WindowSizeClass.Compact
    }
    
    @Test
    fun `pane focus change triggers animation in compact`() = runComposeTest {
        // Test pane switching animation
    }
}
```

### 4. Nested Navigation

```kotlin
class NestedNavigationTest {
    
    @Test
    fun `nested tabs in stack animate correctly`() = runComposeTest {
        // Stack -> Tabs -> Screen
    }
    
    @Test
    fun `stack in tab preserves history`() = runComposeTest {
        // Tab -> Stack -> multiple screens
    }
    
    @Test
    fun `pane with tabs adapts layout`() = runComposeTest {
        // Pane -> Tabs in secondary pane
    }
}
```

### 5. Predictive Back

```kotlin
class PredictiveBackTest {
    
    @Test
    fun `predictive back shows both screens`() = runComposeTest {
        // Simulate gesture start
    }
    
    @Test
    fun `predictive back completes navigation`() = runComposeTest {
        // Simulate gesture complete
    }
    
    @Test
    fun `predictive back cancels on release`() = runComposeTest {
        // Simulate gesture cancel
    }
    
    @Test
    fun `predictive back transforms tab subtree`() = runComposeTest {
        // Verify entire tab (wrapper + content) transforms
    }
}
```

### 6. Shared Elements

```kotlin
class SharedElementTest {
    
    @Test
    fun `shared element animates between screens`() = runComposeTest {
        // Basic shared element
    }
    
    @Test
    fun `shared element works across tabs`() = runComposeTest {
        // Cross-tab shared element
    }
}
```

---

## Acceptance Criteria

- [ ] Stack navigation tests: render, push animation, pop animation
- [ ] Tab navigation tests: wrapper render, tab switch, state preservation
- [ ] Pane navigation tests: expanded, compact, focus change
- [ ] Nested navigation tests: various combinations
- [ ] Predictive back tests: gesture start, complete, cancel, subtree transform
- [ ] Shared element tests: basic, cross-tab
- [ ] All tests pass on Android and iOS
- [ ] Test utilities documented
