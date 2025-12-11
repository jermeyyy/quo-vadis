# Stack Scope Navigation Implementation Plan

## Executive Summary

This document outlines the implementation plan to extend scope-aware navigation to `StackNode`, completing the scope support that already exists for `TabNode` and `PaneNode`.

**Current State**: Tabs and panes already have `scopeKey` properties and are supported by `ScopeRegistry` and `TreeMutator.findTargetStackForPush()`. Stacks lack this capability.

**Goal**: Allow `@Stack` annotated sealed classes to define navigation scopes, enabling destinations to be constrained within a stack's scope boundary.

---

## Problem Analysis

### Current Behavior

Currently, `StackNode` has no `scopeKey` property:

```kotlin
data class StackNode(
    override val key: String,
    override val parentKey: String?,
    val children: List<NavNode> = emptyList()
) : NavNode
```

When navigating within a stack, all destinations are pushed to the deepest active stack regardless of scope. The `TreeMutator.findTargetStackForPush()` only checks `TabNode` and `PaneNode` for scope violations:

```kotlin
// Current implementation only handles TabNode and PaneNode
when (node) {
    is TabNode -> { /* scope check */ }
    is PaneNode -> { /* scope check */ }
    else -> { /* Continue checking */ }  // StackNode falls through!
}
```

### Use Case: Nested Stack Scopes

Consider a flow-based navigation pattern:

```kotlin
@Stack(name = "auth")
sealed class AuthFlow : Destination {
    @Destination(route = "auth/login")
    data object Login : AuthFlow()
    
    @Destination(route = "auth/register")
    data object Register : AuthFlow()
    
    @Destination(route = "auth/forgot-password")
    data object ForgotPassword : AuthFlow()
}

@Stack(name = "main")
sealed class MainFlow : Destination {
    @Destination(route = "main/home")
    data object Home : MainFlow()
    
    @Destination(route = "main/profile")
    data object Profile : MainFlow()
}
```

**Desired Behavior**:
- From `AuthFlow.Login`, navigating to `MainFlow.Home` should:
  1. Detect that `MainFlow.Home` is out of `AuthFlow` scope
  2. Navigate to the parent stack (above `AuthFlow`)
  3. Preserve `AuthFlow` stack for predictive back

**Current Behavior**:
- `MainFlow.Home` is pushed directly into `AuthFlow`'s stack, mixing flow boundaries

### Desired Tree Structure

```
Before Navigation (from AuthFlow.Login to MainFlow.Home):
StackNode (root)
  └── StackNode (auth, scopeKey = "AuthFlow") ← ACTIVE
       └── ScreenNode (AuthFlow.Login)

After Navigation (DESIRED):
StackNode (root)
  ├── StackNode (auth, scopeKey = "AuthFlow") ← preserved
  │    └── ScreenNode (AuthFlow.Login)
  └── StackNode (main, scopeKey = "MainFlow") ← NEW, active
       └── ScreenNode (MainFlow.Home)
```

---

## Architecture Design

### 1. StackNode Enhancement

Add `scopeKey` property to `StackNode`:

```kotlin
// NavNode.kt
@Serializable
@SerialName("stack")
data class StackNode(
    override val key: String,
    override val parentKey: String?,
    val children: List<NavNode> = emptyList(),
    val scopeKey: String? = null  // NEW: Scope identifier for scope checking
) : NavNode
```

**KDoc Addition**:
```kotlin
/**
 * Container node representing a linear navigation stack.
 * ...existing doc...
 *
 * ## Scope-Aware Navigation
 *
 * When [scopeKey] is set, [TreeMutator.push] with a [ScopeRegistry] will check
 * if destinations belong to this stack's scope. Out-of-scope destinations
 * navigate to the parent stack instead, preserving this stack for
 * predictive back gestures.
 *
 * @property scopeKey Identifier for scope-aware navigation. When set, destinations
 *   not in this scope will navigate outside the stack. Typically the
 *   sealed class simple name (e.g., "AuthFlow"). Defaults to null (no scope enforcement).
 */
```

### 2. TreeMutator Enhancement

Extend `findTargetStackForPush()` to handle `StackNode`:

```kotlin
// TreeMutator.kt - findTargetStackForPush()
private fun findTargetStackForPush(
    root: NavNode,
    destination: Destination,
    scopeRegistry: ScopeRegistry
): StackNode? {
    val activeStack = root.activeStack() ?: return null
    val activePath = root.activePathToLeaf()

    for (node in activePath.reversed()) {
        when (node) {
            is TabNode -> {
                val scopeKey = node.scopeKey
                if (scopeKey != null && !scopeRegistry.isInScope(scopeKey, destination)) {
                    val parentKey = node.parentKey ?: return null
                    val parent = root.findByKey(parentKey)
                    if (parent is StackNode) return parent
                }
            }
            is PaneNode -> {
                val scopeKey = node.scopeKey
                if (scopeKey != null && !scopeRegistry.isInScope(scopeKey, destination)) {
                    val parentKey = node.parentKey ?: return null
                    val parent = root.findByKey(parentKey)
                    if (parent is StackNode) return parent
                }
            }
            // NEW: Handle StackNode scope checking
            is StackNode -> {
                val scopeKey = node.scopeKey
                if (scopeKey != null && !scopeRegistry.isInScope(scopeKey, destination)) {
                    // Out of scope - find parent stack
                    val parentKey = node.parentKey ?: return null
                    val parent = root.findByKey(parentKey)
                    // Parent could be another StackNode, TabNode, or PaneNode
                    // We need to find the containing stack
                    return findContainingStack(root, parent)
                }
            }
            else -> { /* Continue checking */ }
        }
    }

    return activeStack
}

/**
 * Finds the StackNode that contains the given node.
 * For TabNode/PaneNode, returns their parent stack.
 * For StackNode, returns that stack.
 */
private fun findContainingStack(root: NavNode, node: NavNode?): StackNode? {
    return when (node) {
        is StackNode -> node
        is TabNode, is PaneNode -> {
            val parentKey = node.parentKey ?: return null
            val parent = root.findByKey(parentKey)
            if (parent is StackNode) parent else null
        }
        else -> null
    }
}
```

### 3. KSP Model Enhancement

Add scope-related fields to `StackInfo`:

```kotlin
// StackInfo.kt
data class StackInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,  // This becomes the scopeKey
    val packageName: String,
    val startDestination: String,
    val startDestinationClass: KSClassDeclaration?,
    val destinations: List<DestinationInfo>,
    val resolvedStartDestination: DestinationInfo?,
    val scopeKey: String = className  // NEW: Default to className for scope identification
)
```

### 4. ScopeRegistryGenerator Enhancement

Update generator to include `@Stack` annotations:

```kotlin
// ScopeRegistryGenerator.kt

public fun generate(
    tabInfos: List<TabInfo>,
    paneInfos: List<PaneInfo>,
    stackInfos: List<StackInfo>,  // NEW parameter
    @Suppress("UNUSED_PARAMETER") packageName: String = GENERATED_PACKAGE
) {
    // Only generate if there are scoped containers
    if (tabInfos.isEmpty() && paneInfos.isEmpty() && stackInfos.isEmpty()) {
        logger.info("No @Tab, @Pane, or @Stack annotations found, skipping ScopeRegistry generation")
        return
    }
    // ...
}

private fun buildScopeMapProperty(
    tabInfos: List<TabInfo>,
    paneInfos: List<PaneInfo>,
    stackInfos: List<StackInfo>  // NEW parameter
): PropertySpec {
    // ... existing tab and pane scope building ...
    
    // NEW: Add stack scopes
    for (stackInfo in stackInfos) {
        val scopeKey = stackInfo.className  // e.g., "AuthFlow"
        val memberClassRefs = stackInfo.destinations.map { destInfo ->
            val destClassName = buildClassName(destInfo.classDeclaration)
            CodeBlock.of("%T::class", destClassName)
        }
        if (memberClassRefs.isNotEmpty()) {
            val setOfClasses = CodeBlock.builder()
                .add("setOf(")
                .add(memberClassRefs.joinToCode(", "))
                .add(")")
                .build()
            entries.add(CodeBlock.of("%S to %L", scopeKey, setOfClasses))
        }
    }
    // ...
}
```

### 5. NavNodeBuilderGenerator Enhancement

Update generated stack builders to include `scopeKey`:

```kotlin
// Generated code example:
fun buildAuthFlowNode(
    parentKey: String? = null,
    generateKey: () -> String = { ... }
): StackNode {
    val stackKey = generateKey()
    val screenKey = generateKey()
    
    return StackNode(
        key = stackKey,
        parentKey = parentKey,
        children = listOf(
            ScreenNode(
                key = screenKey,
                parentKey = stackKey,
                destination = AuthFlow.Login
            )
        ),
        scopeKey = "AuthFlow"  // NEW: Auto-generated from sealed class name
    )
}
```

### 6. QuoVadisSymbolProcessor Update

Pass `stackInfos` to `ScopeRegistryGenerator`:

```kotlin
// QuoVadisSymbolProcessor.kt
private fun generateScopeRegistry() {
    if (tabInfos.isEmpty() && paneInfos.isEmpty() && stackInfos.isEmpty()) {
        logger.info("No @Tab, @Pane, or @Stack annotations found, skipping ScopeRegistry generation")
        return
    }
    
    try {
        scopeRegistryGenerator.generate(tabInfos, paneInfos, stackInfos)  // Updated call
    } catch (e: Exception) {
        logger.error("Error generating ScopeRegistry: ${e.message}")
    }
}
```

---

## Generated Code Example

For the following annotations:

```kotlin
@Stack(name = "auth")
sealed class AuthFlow : Destination {
    @Destination(route = "auth/login")
    data object Login : AuthFlow()
    
    @Destination(route = "auth/register")
    data object Register : AuthFlow()
}

@Tabs(name = "main")
sealed class MainTabs : Destination {
    @TabItem(route = "home", label = "Home")
    data object HomeTab : MainTabs()
}
```

**Generated ScopeRegistry**:

```kotlin
public object GeneratedScopeRegistry : ScopeRegistry {
    private val scopeMap: Map<String, Set<KClass<out Destination>>> = mapOf(
        "AuthFlow" to setOf(AuthFlow.Login::class, AuthFlow.Register::class),
        "MainTabs" to setOf(MainTabs.HomeTab::class)
    )
    
    override fun isInScope(scopeKey: String, destination: Destination): Boolean {
        val scopes = scopeMap[scopeKey] ?: return true
        return scopes.any { it.isInstance(destination) }
    }
    
    override fun getScopeKey(destination: Destination): String? {
        return scopeMap.entries.find { (_, classes) ->
            classes.any { it.isInstance(destination) }
        }?.key
    }
}
```

---

## Implementation Phases

### Phase 1: Core Library Changes (1-2 days)

**Files to modify:**

| File | Change |
|------|--------|
| [NavNode.kt](../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavNode.kt) | Add `scopeKey: String? = null` to `StackNode` with KDoc |
| [TreeMutator.kt](../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt) | Add `StackNode` case to `findTargetStackForPush()` |
| [ScopeRegistry.kt](../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/ScopeRegistry.kt) | Update KDoc to mention `StackNode.scopeKey` |

**Backward Compatibility:**
- Default `scopeKey = null` means no scope enforcement (existing behavior preserved)
- Serialization: Add `@EncodeDefault` if needed for explicit null serialization

### Phase 2: KSP Generator Updates (1-2 days)

**Files to modify:**

| File | Change |
|------|--------|
| [StackInfo.kt](../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/StackInfo.kt) | No change needed (className already available) |
| [ScopeRegistryGenerator.kt](../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ScopeRegistryGenerator.kt) | Add `stackInfos` parameter and generation logic |
| [NavNodeBuilderGenerator.kt](../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/NavNodeBuilderGenerator.kt) | Include `scopeKey` in generated `StackNode` builders |
| [QuoVadisSymbolProcessor.kt](../../quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt) | Pass `stackInfos` to `ScopeRegistryGenerator.generate()` |

### Phase 3: Testing (1-2 days)

**New test files:**

| File | Purpose |
|------|---------|
| `TreeMutatorStackScopeTest.kt` | Test `StackNode` scope checking in `TreeMutator` |
| Update `ScopeRegistryTest.kt` | Add tests for stack scopes |

**Test scenarios:**
1. Push in-scope destination → stays in current stack
2. Push out-of-scope destination → navigates to parent stack
3. Nested stacks with different scopes
4. Stack inside TabNode with both having scopes
5. Empty scope registry preserves existing behavior

### Phase 4: Demo App Update (0.5 days)

**Tasks:**
- Add a scoped stack flow to demo app (e.g., auth flow)
- Verify scope-aware navigation works end-to-end
- Test predictive back with scoped stacks

---

## Risk Assessment

### Low Risk
1. **Backward compatibility** - Default `scopeKey = null` preserves existing behavior
   - *Mitigation*: Extensive tests for both scoped and non-scoped stacks

2. **Serialization** - Adding nullable property to `StackNode`
   - *Mitigation*: Kotlin serialization handles nullable properties gracefully

### Medium Risk
3. **Nested scope resolution** - Stack inside tab inside pane with all having scopes
   - *Mitigation*: Clear documentation of scope precedence (deepest wins)
   - *Mitigation*: Comprehensive test cases for nested scenarios

4. **KSP generator complexity** - Coordinating three container types
   - *Mitigation*: Follow existing patterns for tabs and panes

---

## Test Scenarios

### Unit Tests

**TreeMutator Stack Scope Tests:**
```kotlin
@Test
fun `push in-scope destination stays in stack`() {
    // Given stack with scopeKey = "AuthFlow"
    // When push AuthFlow.Register
    // Then pushed to same stack
}

@Test
fun `push out-of-scope destination navigates to parent`() {
    // Given stack with scopeKey = "AuthFlow" 
    // When push MainFlow.Home (not in AuthFlow scope)
    // Then new stack created as sibling
}

@Test
fun `nested stacks respect innermost scope first`() {
    // Given: root > outerStack(scopeKey=A) > innerStack(scopeKey=B)
    // When push destination in A but not in B
    // Then: pushes to outerStack
}

@Test
fun `stack without scopeKey accepts all destinations`() {
    // Given stack with scopeKey = null
    // When push any destination
    // Then pushed to same stack (existing behavior)
}
```

**ScopeRegistry Stack Tests:**
```kotlin
@Test
fun `generated registry includes stack destinations`() {
    val registry = GeneratedScopeRegistry
    assertTrue(registry.isInScope("AuthFlow", AuthFlow.Login))
    assertTrue(registry.isInScope("AuthFlow", AuthFlow.Register))
    assertFalse(registry.isInScope("AuthFlow", MainFlow.Home))
}

@Test
fun `getScopeKey returns stack scope for stack destinations`() {
    val registry = GeneratedScopeRegistry
    assertEquals("AuthFlow", registry.getScopeKey(AuthFlow.Login))
}
```

### Integration Tests

1. **Stack → Out-of-scope navigation**
   - Navigate from AuthFlow.Login to MainFlow.Home
   - Verify AuthFlow stack preserved
   - Verify back navigation restores AuthFlow.Login

2. **Predictive back with scoped stack**
   - Gesture back from MainFlow.Home
   - Verify AuthFlow stack visible during gesture
   - Complete gesture and verify AuthFlow.Login visible

---

## Migration Guide

### For Existing Apps

**No action required.** The default `scopeKey = null` on `StackNode` preserves current behavior.

### For Apps Wanting Scoped Stack Navigation

1. **Update library** - Get latest quo-vadis with stack scope support

2. **Regenerate KSP code** - Stack destinations will be automatically included in `GeneratedScopeRegistry`

3. **Pass ScopeRegistry to NavigationHost** (if not already):
```kotlin
NavigationHost(
    navigator = navigator,
    scopeRegistry = GeneratedScopeRegistry
)
```

4. **Verify navigation flows** - Out-of-scope destinations now navigate above stacks

### Opting Out of Stack Scopes

If you want stacks without scope enforcement:
- The KSP generator will still include stack destinations in the scope map
- But `TreeMutator` only checks scope when `StackNode.scopeKey` is set
- Generated builders will set `scopeKey` automatically; override if needed:

```kotlin
// Manual stack creation without scope
StackNode(
    key = "auth",
    parentKey = null,
    children = listOf(...),
    scopeKey = null  // Explicitly disable scope checking
)
```

---

## Open Questions

1. **Should `@Stack` have an opt-out for scope generation?**
   - E.g., `@Stack(name = "auth", generateScope = false)`
   - **Recommendation**: Not needed initially; manual `scopeKey = null` override suffices

2. **How to handle deeply nested stacks?**
   - Current design: Each stack can have its own scope, checked innermost-first
   - **Recommendation**: This is correct behavior; document clearly

3. **Should scope key be customizable via annotation?**
   - E.g., `@Stack(name = "auth", scopeKey = "CustomScopeKey")`
   - **Recommendation**: Not needed initially; class name is sufficient

---

## Timeline Estimate

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Core | 1-2 days | None |
| Phase 2: KSP | 1-2 days | Phase 1 |
| Phase 3: Testing | 1-2 days | Phase 1, 2 |
| Phase 4: Demo | 0.5 days | Phase 1, 2, 3 |

**Total: 3.5-6.5 days**

---

## Code Location Reference

### Core Library
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/`
  - `NavNode.kt` - `StackNode` data class
  - `TreeMutator.kt` - `findTargetStackForPush()` function
  - `ScopeRegistry.kt` - Interface (update KDoc only)

### KSP Processor  
- `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/`
  - `generators/ScopeRegistryGenerator.kt` - Add stack support
  - `generators/NavNodeBuilderGenerator.kt` - Add scopeKey to generated builders
  - `QuoVadisSymbolProcessor.kt` - Pass stackInfos to generator

### Tests
- `quo-vadis-core/src/commonTest/kotlin/.../core/`
  - `TreeMutatorStackScopeTest.kt` - NEW
  - `ScopeRegistryTest.kt` - Add stack tests

---

## Comparison: Tab/Pane vs Stack Scope Support

| Aspect | TabNode/PaneNode | StackNode (NEW) |
|--------|------------------|-----------------|
| `scopeKey` property | ✅ Implemented | 🆕 To implement |
| KDoc documentation | ✅ Complete | 🆕 To add |
| `TreeMutator` support | ✅ `findTargetStackForPush` handles | 🆕 Add case |
| `ScopeRegistryGenerator` | ✅ Generates from `@Tab`/`@Pane` | 🆕 Add `@Stack` |
| `NavNodeBuilderGenerator` | ✅ Includes `scopeKey` | 🆕 Add for stacks |
| Unit tests | ✅ `TreeMutatorScopeTest` | 🆕 Add stack tests |
| Demo app usage | ✅ `GeneratedScopeRegistry` | 🆕 Add scoped stack flow |

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-11 | Architecture Agent | Initial plan |
