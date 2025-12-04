# CORE-005: Comprehensive Unit Test Suite

> **Note**: This was previously CORE-006 but renumbered after backward compat task was removed.

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | CORE-005 |
| **Task Name** | Comprehensive Unit Test Suite |
| **Phase** | Phase 1: Core State Refactoring |
| **Complexity** | Medium |
| **Estimated Time** | 3-4 days |
| **Dependencies** | CORE-001, CORE-002, CORE-003, CORE-004 |
| **Blocked By** | All other CORE tasks |
| **Blocks** | Phase 2 |

---

## Overview

This task implements a comprehensive unit test suite for all Phase 1 components. The tests ensure correctness, document expected behavior, and prevent regressions as the codebase evolves.

### Test Strategy

| Component | Test Focus | Approach |
|-----------|------------|----------|
| NavNode Hierarchy | Instantiation, validation, properties | Property-based testing |
| TreeMutator | Pure function correctness | Input/output verification |
| TreeNavigator | State flow behavior | Coroutine testing |
| Serialization | Round-trip integrity | Snapshot testing |

> ~~Backward Compat~~ - **Removed** (no backward compatibility layer)

---

## Test File Locations

```
quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/
├── core/
│   ├── NavNodeTest.kt
│   ├── ScreenNodeTest.kt
│   ├── StackNodeTest.kt
│   ├── TabNodeTest.kt
│   ├── PaneNodeTest.kt
│   ├── TreeMutatorPushTest.kt
│   ├── TreeMutatorPopTest.kt
│   ├── TreeMutatorTabTest.kt
│   ├── TreeMutatorPaneTest.kt
│   ├── TreeMutatorEdgeCasesTest.kt
│   ├── TreeNavigatorTest.kt
│   └── TransitionStateTest.kt
├── serialization/
│   ├── NavNodeSerializerTest.kt
│   └── StateRestorationTest.kt
└── compat/
    ├── BackwardCompatExtensionsTest.kt
    └── MigrationUtilsTest.kt
```

---

## Test Implementation

### 1. NavNode Hierarchy Tests

#### NavNodeTest.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.*

class NavNodeTest {
    
    // =========================================================================
    // SCREEN NODE TESTS
    // =========================================================================
    
    @Test
    fun `ScreenNode holds destination correctly`() {
        val destination = BasicDestination("profile")
        val node = ScreenNode(
            key = "screen-1",
            parentKey = "stack-1",
            destination = destination
        )
        
        assertEquals("screen-1", node.key)
        assertEquals("stack-1", node.parentKey)
        assertEquals(destination, node.destination)
    }
    
    @Test
    fun `ScreenNode with null parentKey is valid root screen`() {
        val node = ScreenNode(
            key = "root-screen",
            parentKey = null,
            destination = BasicDestination("home")
        )
        
        assertNull(node.parentKey)
    }
    
    // =========================================================================
    // STACK NODE TESTS
    // =========================================================================
    
    @Test
    fun `StackNode activeChild returns last element`() {
        val screen1 = ScreenNode("s1", "stack", BasicDestination("a"))
        val screen2 = ScreenNode("s2", "stack", BasicDestination("b"))
        val screen3 = ScreenNode("s3", "stack", BasicDestination("c"))
        
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(screen1, screen2, screen3)
        )
        
        assertEquals(screen3, stack.activeChild)
    }
    
    @Test
    fun `StackNode activeChild returns null when empty`() {
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = emptyList()
        )
        
        assertNull(stack.activeChild)
    }
    
    @Test
    fun `StackNode canGoBack true when multiple children`() {
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "stack", BasicDestination("a")),
                ScreenNode("s2", "stack", BasicDestination("b"))
            )
        )
        
        assertTrue(stack.canGoBack)
    }
    
    @Test
    fun `StackNode canGoBack false when single child`() {
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(ScreenNode("s1", "stack", BasicDestination("a")))
        )
        
        assertFalse(stack.canGoBack)
    }
    
    @Test
    fun `StackNode isEmpty true when no children`() {
        val stack = StackNode("stack", null, emptyList())
        
        assertTrue(stack.isEmpty)
        assertEquals(0, stack.size)
    }
    
    // =========================================================================
    // TAB NODE TESTS
    // =========================================================================
    
    @Test
    fun `TabNode requires at least one stack`() {
        assertFailsWith<IllegalArgumentException> {
            TabNode(
                key = "tabs",
                parentKey = null,
                stacks = emptyList(),
                activeStackIndex = 0
            )
        }
    }
    
    @Test
    fun `TabNode validates activeStackIndex bounds`() {
        val stack = StackNode("s1", "tabs", emptyList())
        
        assertFailsWith<IllegalArgumentException> {
            TabNode(
                key = "tabs",
                parentKey = null,
                stacks = listOf(stack),
                activeStackIndex = 5
            )
        }
    }
    
    @Test
    fun `TabNode validates negative activeStackIndex`() {
        val stack = StackNode("s1", "tabs", emptyList())
        
        assertFailsWith<IllegalArgumentException> {
            TabNode(
                key = "tabs",
                parentKey = null,
                stacks = listOf(stack),
                activeStackIndex = -1
            )
        }
    }
    
    @Test
    fun `TabNode activeStack returns correct stack`() {
        val stack0 = StackNode("s0", "tabs", emptyList())
        val stack1 = StackNode("s1", "tabs", emptyList())
        val stack2 = StackNode("s2", "tabs", emptyList())
        
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack0, stack1, stack2),
            activeStackIndex = 1
        )
        
        assertEquals(stack1, tabs.activeStack)
        assertEquals(3, tabs.tabCount)
    }
    
    @Test
    fun `TabNode stackAt returns correct stack`() {
        val stack0 = StackNode("s0", "tabs", emptyList())
        val stack1 = StackNode("s1", "tabs", emptyList())
        
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )
        
        assertEquals(stack0, tabs.stackAt(0))
        assertEquals(stack1, tabs.stackAt(1))
    }
    
    // =========================================================================
    // PANE NODE TESTS
    // =========================================================================
    
    @Test
    fun `PaneNode requires at least one pane`() {
        assertFailsWith<IllegalArgumentException> {
            PaneNode(
                key = "panes",
                parentKey = null,
                panes = emptyList(),
                activePaneIndex = 0
            )
        }
    }
    
    @Test
    fun `PaneNode validates activePaneIndex bounds`() {
        val pane = ScreenNode("p1", "panes", BasicDestination("a"))
        
        assertFailsWith<IllegalArgumentException> {
            PaneNode(
                key = "panes",
                parentKey = null,
                panes = listOf(pane),
                activePaneIndex = 3
            )
        }
    }
    
    @Test
    fun `PaneNode activePane returns correct pane`() {
        val pane0 = ScreenNode("p0", "panes", BasicDestination("list"))
        val pane1 = ScreenNode("p1", "panes", BasicDestination("detail"))
        
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            panes = listOf(pane0, pane1),
            activePaneIndex = 1
        )
        
        assertEquals(pane1, panes.activePane)
        assertEquals(2, panes.paneCount)
    }
    
    // =========================================================================
    // EXTENSION FUNCTION TESTS
    // =========================================================================
    
    @Test
    fun `findByKey finds root node`() {
        val root = StackNode("root", null, emptyList())
        
        assertEquals(root, root.findByKey("root"))
    }
    
    @Test
    fun `findByKey finds nested screen`() {
        val screen = ScreenNode("target", "stack", BasicDestination("test"))
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("other", "root", BasicDestination("other")),
                screen
            )
        )
        
        assertEquals(screen, root.findByKey("target"))
    }
    
    @Test
    fun `findByKey returns null when not found`() {
        val root = StackNode("root", null, emptyList())
        
        assertNull(root.findByKey("nonexistent"))
    }
    
    @Test
    fun `findByKey finds node in TabNode`() {
        val targetScreen = ScreenNode("target", "tab1", BasicDestination("test"))
        val root = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", listOf(targetScreen))
            ),
            activeStackIndex = 0
        )
        
        assertEquals(targetScreen, root.findByKey("target"))
    }
    
    @Test
    fun `activePathToLeaf returns complete path`() {
        val screen = ScreenNode("leaf", "stack", BasicDestination("test"))
        val stack = StackNode("stack", "tabs", listOf(screen))
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack),
            activeStackIndex = 0
        )
        
        val path = tabs.activePathToLeaf()
        
        assertEquals(3, path.size)
        assertEquals(tabs, path[0])
        assertEquals(stack, path[1])
        assertEquals(screen, path[2])
    }
    
    @Test
    fun `activeLeaf returns deepest ScreenNode`() {
        val screen = ScreenNode("leaf", "stack", BasicDestination("test"))
        val stack = StackNode("stack", null, listOf(screen))
        
        assertEquals(screen, stack.activeLeaf())
    }
    
    @Test
    fun `activeLeaf returns null when no screens`() {
        val stack = StackNode("stack", null, emptyList())
        
        assertNull(stack.activeLeaf())
    }
    
    @Test
    fun `activeStack returns deepest active StackNode`() {
        val innerStack = StackNode(
            key = "inner",
            parentKey = "outer",
            children = listOf(ScreenNode("s", "inner", BasicDestination("test")))
        )
        val outerStack = StackNode(
            key = "outer",
            parentKey = null,
            children = listOf(innerStack)
        )
        
        assertEquals(innerStack, outerStack.activeStack())
    }
    
    @Test
    fun `allScreens returns all ScreenNodes in tree`() {
        val screen1 = ScreenNode("s1", "tab0", BasicDestination("a"))
        val screen2 = ScreenNode("s2", "tab0", BasicDestination("b"))
        val screen3 = ScreenNode("s3", "tab1", BasicDestination("c"))
        
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(screen1, screen2)),
                StackNode("tab1", "tabs", listOf(screen3))
            ),
            activeStackIndex = 0
        )
        
        val allScreens = tabs.allScreens()
        
        assertEquals(3, allScreens.size)
        assertTrue(allScreens.contains(screen1))
        assertTrue(allScreens.contains(screen2))
        assertTrue(allScreens.contains(screen3))
    }
}
```

### 2. TreeMutator Tests

#### TreeMutatorPushTest.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.*

class TreeMutatorPushTest {
    
    private val homeDestination = BasicDestination("home")
    private val profileDestination = BasicDestination("profile")
    private val settingsDestination = BasicDestination("settings")
    
    private fun createKeyGenerator(): () -> String {
        var counter = 0
        return { "key-${counter++}" }
    }
    
    @Test
    fun `push adds screen to empty stack`() {
        val root = StackNode("root", null, emptyList())
        
        val result = TreeMutator.push(
            root = root,
            destination = homeDestination,
            generateKey = createKeyGenerator()
        )
        
        assertTrue(result is StackNode)
        assertEquals(1, (result as StackNode).children.size)
        assertEquals(homeDestination, (result.activeChild as ScreenNode).destination)
    }
    
    @Test
    fun `push appends to existing stack`() {
        val screen1 = ScreenNode("s1", "root", homeDestination)
        val root = StackNode("root", null, listOf(screen1))
        
        val result = TreeMutator.push(
            root = root,
            destination = profileDestination,
            generateKey = createKeyGenerator()
        )
        
        assertTrue(result is StackNode)
        assertEquals(2, (result as StackNode).children.size)
        assertEquals(homeDestination, (result.children[0] as ScreenNode).destination)
        assertEquals(profileDestination, (result.activeChild as ScreenNode).destination)
    }
    
    @Test
    fun `push targets deepest active stack in tabs`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode("tab0", "tabs", listOf(
                            ScreenNode("s1", "tab0", homeDestination)
                        )),
                        StackNode("tab1", "tabs", emptyList())
                    ),
                    activeStackIndex = 0
                )
            )
        )
        
        val result = TreeMutator.push(
            root = root,
            destination = profileDestination,
            generateKey = createKeyGenerator()
        )
        
        // New screen should be in tab0
        val tabs = (result as StackNode).children[0] as TabNode
        val tab0 = tabs.stacks[0]
        
        assertEquals(2, tab0.children.size)
        assertEquals(profileDestination, (tab0.activeChild as ScreenNode).destination)
        
        // tab1 should be unchanged
        assertEquals(0, tabs.stacks[1].children.size)
    }
    
    @Test
    fun `push preserves structural sharing`() {
        val screen1 = ScreenNode("s1", "root", homeDestination)
        val root = StackNode("root", null, listOf(screen1))
        
        val result = TreeMutator.push(
            root = root,
            destination = profileDestination,
            generateKey = createKeyGenerator()
        ) as StackNode
        
        // Original screen should be same reference
        assertSame(screen1, result.children[0])
    }
    
    @Test
    fun `pushToStack targets specific stack`() {
        val root = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", emptyList())
            ),
            activeStackIndex = 0
        )
        
        // Push to tab1 even though tab0 is active
        val result = TreeMutator.pushToStack(
            root = root,
            stackKey = "tab1",
            destination = profileDestination,
            generateKey = createKeyGenerator()
        ) as TabNode
        
        assertEquals(0, result.stacks[0].children.size)
        assertEquals(1, result.stacks[1].children.size)
    }
    
    @Test
    fun `pushToStack throws for invalid key`() {
        val root = StackNode("root", null, emptyList())
        
        assertFailsWith<IllegalArgumentException> {
            TreeMutator.pushToStack(root, "nonexistent", homeDestination)
        }
    }
    
    @Test
    fun `pushToStack throws for non-stack node`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("screen", "root", homeDestination)
            )
        )
        
        assertFailsWith<IllegalArgumentException> {
            TreeMutator.pushToStack(root, "screen", profileDestination)
        }
    }
}
```

#### TreeMutatorPopTest.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.*

class TreeMutatorPopTest {
    
    private val homeDestination = BasicDestination("home")
    private val profileDestination = BasicDestination("profile")
    private val settingsDestination = BasicDestination("settings")
    
    @Test
    fun `pop removes last screen from stack`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", homeDestination),
                ScreenNode("s2", "root", profileDestination)
            )
        )
        
        val result = TreeMutator.pop(root)
        
        assertNotNull(result)
        assertEquals(1, (result as StackNode).children.size)
        assertEquals(homeDestination, (result.activeChild as ScreenNode).destination)
    }
    
    @Test
    fun `pop returns null when single item at root`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", homeDestination)
            )
        )
        
        val result = TreeMutator.pop(root)
        
        assertNull(result)
    }
    
    @Test
    fun `pop returns null on empty stack`() {
        val root = StackNode("root", null, emptyList())
        
        val result = TreeMutator.pop(root)
        
        assertNull(result)
    }
    
    @Test
    fun `pop with PRESERVE_EMPTY does not cascade`() {
        val innerStack = StackNode(
            key = "inner",
            parentKey = "outer",
            children = listOf(
                ScreenNode("s1", "inner", homeDestination)
            )
        )
        val outerStack = StackNode(
            key = "outer",
            parentKey = null,
            children = listOf(innerStack)
        )
        
        val result = TreeMutator.pop(outerStack, TreeMutator.PopBehavior.PRESERVE_EMPTY)
        
        assertNull(result) // Can't pop - would leave empty
    }
    
    @Test
    fun `pop from tab affects active tab only`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s1", "tab0", homeDestination),
                    ScreenNode("s2", "tab0", profileDestination)
                )),
                StackNode("tab1", "tabs", listOf(
                    ScreenNode("s3", "tab1", settingsDestination)
                ))
            ),
            activeStackIndex = 0
        )
        
        val result = TreeMutator.pop(tabs)
        
        assertNotNull(result)
        val resultTabs = result as TabNode
        
        // Tab0 should have 1 item
        assertEquals(1, resultTabs.stacks[0].children.size)
        assertEquals(homeDestination, (resultTabs.stacks[0].activeChild as ScreenNode).destination)
        
        // Tab1 should be unchanged
        assertEquals(1, resultTabs.stacks[1].children.size)
    }
    
    @Test
    fun `popTo removes screens until predicate matches`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", homeDestination),
                ScreenNode("s2", "root", profileDestination),
                ScreenNode("s3", "root", settingsDestination)
            )
        )
        
        val result = TreeMutator.popTo(root, inclusive = false) { node ->
            node is ScreenNode && node.destination.route == "profile"
        }
        
        assertEquals(2, (result as StackNode).children.size)
        assertEquals(profileDestination, (result.activeChild as ScreenNode).destination)
    }
    
    @Test
    fun `popTo with inclusive removes matching screen`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", homeDestination),
                ScreenNode("s2", "root", profileDestination),
                ScreenNode("s3", "root", settingsDestination)
            )
        )
        
        val result = TreeMutator.popTo(root, inclusive = true) { node ->
            node is ScreenNode && node.destination.route == "profile"
        }
        
        assertEquals(1, (result as StackNode).children.size)
        assertEquals(homeDestination, (result.activeChild as ScreenNode).destination)
    }
    
    @Test
    fun `popTo returns original when predicate not matched`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", homeDestination)
            )
        )
        
        val result = TreeMutator.popTo(root) { false }
        
        assertEquals(root, result)
    }
    
    @Test
    fun `popToRoute finds screen by route`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", homeDestination),
                ScreenNode("s2", "root", profileDestination),
                ScreenNode("s3", "root", settingsDestination)
            )
        )
        
        val result = TreeMutator.popToRoute(root, "home")
        
        assertEquals(1, (result as StackNode).children.size)
        assertEquals(homeDestination, (result.activeChild as ScreenNode).destination)
    }
}
```

#### TreeMutatorTabTest.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.*

class TreeMutatorTabTest {
    
    @Test
    fun `switchTab updates activeStackIndex`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", emptyList()),
                StackNode("tab2", "tabs", emptyList())
            ),
            activeStackIndex = 0
        )
        
        val result = TreeMutator.switchTab(tabs, "tabs", 2)
        
        assertEquals(2, (result as TabNode).activeStackIndex)
    }
    
    @Test
    fun `switchTab preserves all stacks`() {
        val stack0 = StackNode("tab0", "tabs", listOf(
            ScreenNode("s1", "tab0", BasicDestination("a"))
        ))
        val stack1 = StackNode("tab1", "tabs", listOf(
            ScreenNode("s2", "tab1", BasicDestination("b")),
            ScreenNode("s3", "tab1", BasicDestination("c"))
        ))
        
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )
        
        val result = TreeMutator.switchTab(tabs, "tabs", 1) as TabNode
        
        assertEquals(1, result.activeStackIndex)
        assertEquals(1, result.stacks[0].children.size) // tab0 unchanged
        assertEquals(2, result.stacks[1].children.size) // tab1 unchanged
    }
    
    @Test
    fun `switchTab returns same state when already on target tab`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", emptyList())
            ),
            activeStackIndex = 1
        )
        
        val result = TreeMutator.switchTab(tabs, "tabs", 1)
        
        // Should be same reference (no change)
        assertSame(tabs, result)
    }
    
    @Test
    fun `switchTab throws for invalid index`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(StackNode("tab0", "tabs", emptyList())),
            activeStackIndex = 0
        )
        
        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchTab(tabs, "tabs", 5)
        }
    }
    
    @Test
    fun `switchTab throws for negative index`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(StackNode("tab0", "tabs", emptyList())),
            activeStackIndex = 0
        )
        
        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchTab(tabs, "tabs", -1)
        }
    }
    
    @Test
    fun `switchActiveTab finds TabNode in active path`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode("tab0", "tabs", emptyList()),
                        StackNode("tab1", "tabs", emptyList())
                    ),
                    activeStackIndex = 0
                )
            )
        )
        
        val result = TreeMutator.switchActiveTab(root, 1)
        
        val tabs = (result as StackNode).children[0] as TabNode
        assertEquals(1, tabs.activeStackIndex)
    }
    
    @Test
    fun `switchActiveTab throws when no TabNode in path`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", BasicDestination("home"))
            )
        )
        
        assertFailsWith<IllegalStateException> {
            TreeMutator.switchActiveTab(root, 1)
        }
    }
}
```

### 3. Serialization Tests

#### NavNodeSerializerTest.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.serialization

import com.jermey.quo.vadis.core.navigation.core.*
import kotlin.test.*

class NavNodeSerializerTest {
    
    @Test
    fun `ScreenNode serialization round-trip`() {
        val original = ScreenNode(
            key = "screen-1",
            parentKey = "stack",
            destination = BasicDestination("profile")
        )
        
        val json = NavNodeSerializer.toJson(original)
        val restored = NavNodeSerializer.fromJson(json)
        
        assertEquals(original, restored)
    }
    
    @Test
    fun `StackNode serialization round-trip`() {
        val original = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "stack", BasicDestination("home")),
                ScreenNode("s2", "stack", BasicDestination("profile"))
            )
        )
        
        val json = NavNodeSerializer.toJson(original)
        val restored = NavNodeSerializer.fromJson(json)
        
        assertEquals(original, restored)
    }
    
    @Test
    fun `TabNode serialization round-trip`() {
        val original = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s1", "tab0", BasicDestination("feed"))
                )),
                StackNode("tab1", "tabs", listOf(
                    ScreenNode("s2", "tab1", BasicDestination("profile"))
                ))
            ),
            activeStackIndex = 1
        )
        
        val json = NavNodeSerializer.toJson(original)
        val restored = NavNodeSerializer.fromJson(json)
        
        assertEquals(original, restored)
        assertEquals(1, (restored as TabNode).activeStackIndex)
    }
    
    @Test
    fun `PaneNode serialization round-trip`() {
        val original = PaneNode(
            key = "panes",
            parentKey = null,
            panes = listOf(
                StackNode("list", "panes", listOf(
                    ScreenNode("s1", "list", BasicDestination("items"))
                )),
                StackNode("detail", "panes", listOf(
                    ScreenNode("s2", "detail", BasicDestination("item-detail"))
                ))
            ),
            activePaneIndex = 0
        )
        
        val json = NavNodeSerializer.toJson(original)
        val restored = NavNodeSerializer.fromJson(json)
        
        assertEquals(original, restored)
    }
    
    @Test
    fun `complex nested tree serialization round-trip`() {
        val original = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("home", "root", BasicDestination("home")),
                TabNode(
                    key = "tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode("tab0", "tabs", listOf(
                            ScreenNode("feed-list", "tab0", BasicDestination("feed")),
                            ScreenNode("feed-detail", "tab0", BasicDestination("feed-item"))
                        )),
                        StackNode("tab1", "tabs", listOf(
                            ScreenNode("profile", "tab1", BasicDestination("profile"))
                        ))
                    ),
                    activeStackIndex = 0
                )
            )
        )
        
        val json = NavNodeSerializer.toJson(original)
        val restored = NavNodeSerializer.fromJson(json)
        
        assertEquals(original, restored)
        
        // Verify structure
        val restoredStack = restored as StackNode
        assertEquals(2, restoredStack.children.size)
        
        val restoredTabs = restoredStack.children[1] as TabNode
        assertEquals(2, restoredTabs.tabCount)
        assertEquals(0, restoredTabs.activeStackIndex)
    }
    
    @Test
    fun `fromJsonOrNull returns null on invalid JSON`() {
        val result = NavNodeSerializer.fromJsonOrNull("invalid json {{}")
        assertNull(result)
    }
    
    @Test
    fun `fromJsonOrNull returns null on null input`() {
        val result = NavNodeSerializer.fromJsonOrNull(null)
        assertNull(result)
    }
    
    @Test
    fun `fromJsonOrNull returns null on empty string`() {
        val result = NavNodeSerializer.fromJsonOrNull("")
        assertNull(result)
    }
    
    @Test
    fun `serialization includes type discriminator`() {
        val node = ScreenNode("s1", null, BasicDestination("test"))
        val json = NavNodeSerializer.toJson(node)
        
        assertTrue(json.contains("\"_type\""))
        assertTrue(json.contains("\"screen\""))
    }
}
```

### 4. TreeNavigator Tests

#### TreeNavigatorTest.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TreeNavigatorTest {
    
    private val homeDestination = BasicDestination("home")
    private val profileDestination = BasicDestination("profile")
    private val settingsDestination = BasicDestination("settings")
    
    @Test
    fun `setStartDestination initializes state`() = runTest {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        
        val state = navigator.state.value
        assertTrue(state is StackNode)
        assertEquals(1, (state as StackNode).children.size)
        assertEquals(homeDestination, navigator.currentDestination.first())
    }
    
    @Test
    fun `navigate pushes to active stack`() = runTest {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        navigator.navigate(profileDestination)
        
        val state = navigator.state.value as StackNode
        assertEquals(2, state.children.size)
        assertEquals(profileDestination, navigator.currentDestination.first())
        assertEquals(homeDestination, navigator.previousDestination.first())
    }
    
    @Test
    fun `navigateBack pops from active stack`() = runTest {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        navigator.navigate(profileDestination)
        
        val result = navigator.navigateBack()
        
        assertTrue(result)
        assertEquals(homeDestination, navigator.currentDestination.first())
    }
    
    @Test
    fun `navigateBack returns false at root`() = runTest {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        
        val result = navigator.navigateBack()
        
        assertFalse(result)
        assertEquals(homeDestination, navigator.currentDestination.first())
    }
    
    @Test
    fun `navigateAndReplace replaces current screen`() = runTest {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        navigator.navigate(profileDestination)
        
        navigator.navigateAndReplace(settingsDestination)
        
        val state = navigator.state.value as StackNode
        assertEquals(2, state.children.size)
        assertEquals(settingsDestination, navigator.currentDestination.first())
    }
    
    @Test
    fun `navigateAndClearAll resets to single destination`() = runTest {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        navigator.navigate(profileDestination)
        navigator.navigate(settingsDestination)
        
        navigator.navigateAndClearAll(homeDestination)
        
        val state = navigator.state.value as StackNode
        assertEquals(1, state.children.size)
        assertEquals(homeDestination, navigator.currentDestination.first())
    }
    
    @Test
    fun `switchTab changes active tab`() = runTest {
        val initialState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode("tab0", "tabs", listOf(
                            ScreenNode("s1", "tab0", homeDestination)
                        )),
                        StackNode("tab1", "tabs", listOf(
                            ScreenNode("s2", "tab1", profileDestination)
                        ))
                    ),
                    activeStackIndex = 0
                )
            )
        )
        val navigator = TreeNavigator(initialState = initialState)
        
        val result = navigator.switchTab(1)
        
        assertTrue(result)
        val tabs = (navigator.state.value as StackNode).children[0] as TabNode
        assertEquals(1, tabs.activeStackIndex)
        assertEquals(profileDestination, navigator.currentDestination.first())
    }
    
    @Test
    fun `transitionState starts idle`() = runTest {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        
        val transition = navigator.transitionState.first()
        assertTrue(transition is TransitionState.Idle)
    }
}
```

### 5. Backward Compatibility Tests

#### BackwardCompatExtensionsTest.kt

```kotlin
package com.jermey.quo.vadis.core.navigation.compat

import com.jermey.quo.vadis.core.navigation.core.*
import kotlin.test.*

@Suppress("DEPRECATION")
class BackwardCompatExtensionsTest {
    
    private val homeDestination = BasicDestination("home")
    private val profileDestination = BasicDestination("profile")
    
    @Test
    fun `activeStack extension returns correct stack`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        navigator.navigate(profileDestination)
        
        val stack = navigator.activeStack
        
        assertNotNull(stack)
        assertEquals(2, stack.size)
    }
    
    @Test
    fun `backStackEntries extension returns screen list`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        navigator.navigate(profileDestination)
        
        val entries = navigator.backStackEntries
        
        assertEquals(2, entries.size)
        assertEquals(homeDestination, entries[0].destination)
        assertEquals(profileDestination, entries[1].destination)
    }
    
    @Test
    fun `backStackSize extension returns correct count`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        navigator.navigate(profileDestination)
        
        assertEquals(2, navigator.backStackSize)
    }
    
    @Test
    fun `canGoBack extension returns correct value`() {
        val navigator = TreeNavigator()
        navigator.setStartDestination(homeDestination)
        
        assertFalse(navigator.canGoBack)
        
        navigator.navigate(profileDestination)
        
        assertTrue(navigator.canGoBack)
    }
    
    @Test
    fun `toBackStack converts tree to entries`() {
        val tree = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", homeDestination),
                ScreenNode("s2", "root", profileDestination)
            )
        )
        
        val backStack = tree.toBackStack()
        
        assertEquals(2, backStack.size)
        assertEquals(homeDestination, backStack[0].destination)
        assertEquals(profileDestination, backStack[1].destination)
    }
    
    @Test
    fun `toStackNode converts entries to tree`() {
        val entries = listOf(
            BackStackEntry(homeDestination, null),
            BackStackEntry(profileDestination, null)
        )
        
        val stack = entries.toStackNode(key = "test-stack")
        
        assertEquals("test-stack", stack.key)
        assertEquals(2, stack.children.size)
        assertEquals(homeDestination, (stack.children[0] as ScreenNode).destination)
        assertEquals(profileDestination, (stack.children[1] as ScreenNode).destination)
    }
}
```

---

## Test Coverage Requirements

| Component | Minimum Coverage | Critical Paths |
|-----------|-----------------|----------------|
| NavNode hierarchy | 90% | Validation, properties |
| TreeMutator | 95% | All operations, edge cases |
| TreeNavigator | 85% | Navigation flow, state updates |
| Serialization | 90% | Round-trip, error handling |
| Backward compat | 80% | API equivalence |

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| All test files listed above | Create | Unit test implementations |
| `quo-vadis-core/build.gradle.kts` | Modify | Add test dependencies |

### Test Dependencies

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
        }
    }
}
```

---

## Acceptance Criteria

- [ ] All NavNode subclasses have property tests
- [ ] All TreeMutator operations have unit tests
- [ ] Edge cases documented and tested
- [ ] Serialization round-trip tests pass
- [ ] TreeNavigator state flow tests pass
- [ ] Backward compatibility tests verify API equivalence
- [ ] Test coverage meets minimum requirements
- [ ] Tests run on all target platforms
- [ ] CI integration configured
- [ ] Test documentation in KDoc

---

## References

- [INDEX](../INDEX.md) - Phase 1 Overview
- [CORE-001](./CORE-001-navnode-hierarchy.md) - NavNode definitions
- [CORE-002](./CORE-002-tree-mutator.md) - TreeMutator operations
- [CORE-003](./CORE-003-navigator-refactor.md) - TreeNavigator implementation
- [CORE-004](./CORE-004-state-serialization.md) - Serialization
- [Kotlin Test documentation](https://kotlinlang.org/api/latest/kotlin.test/)
- [kotlinx-coroutines-test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)
