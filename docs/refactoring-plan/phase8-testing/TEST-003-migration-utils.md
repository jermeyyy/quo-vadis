# Task TEST-003: Migration Utility Tests

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | TEST-003 |
| **Name** | Migration Adapter Test Suite |
| **Phase** | 8 - Testing Infrastructure |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | MIG-001 through MIG-004 |

## Overview

Test all migration utilities for correctness and edge cases.

## Test Cases

```kotlin
class MigrationTests {
    
    @Test
    fun `BackStack converts to StackNode correctly`() {
        val backStack = listOf(
            BackStackEntry("1", HomeDestination),
            BackStackEntry("2", DetailDestination("123"))
        )
        
        val stackNode = backStack.toStackNode()
        
        assertThat(stackNode.children).hasSize(2)
        assertThat(stackNode.activeChild?.destination).isEqualTo(DetailDestination("123"))
    }
    
    @Test
    fun `empty BackStack produces empty StackNode`() {
        val stackNode = emptyList<BackStackEntry>().toStackNode()
        assertThat(stackNode.children).isEmpty()
    }
    
    @Test
    fun `Navigator stateCompat reflects changes`() = runTest {
        val navigator = Navigator()
        navigator.push(HomeDestination)
        
        val states = navigator.stateCompat.take(2).toList()
        
        navigator.push(DetailDestination("1"))
        
        assertThat(states[1].children).hasSize(2)
    }
    
    @Test
    fun `GraphNavHostCompat delegates to QuoVadisHost`() {
        // Compose test
    }
}
```

## Acceptance Criteria

- [ ] BackStack to NavNode conversion tested
- [ ] Empty/null handling verified
- [ ] Navigator.stateCompat reactivity tested
- [ ] GraphNavHost wrapper tested
