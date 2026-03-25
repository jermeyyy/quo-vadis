# Analysis: quo-vadis Cross-Module Tab Merge Ordering

## Problem Statement

When two separate KSP-generated configs define `tabs<MainNavigation>` with different tab items, the runtime merge produces tabs in concatenation order rather than ordinal order:

- **feature-dashboard** generates tabs: `DashboardDestination(0), Transfers(1), Finance(2), Benefits(4)`
- **feature-investments** generates tab: `InvestmentsTab(3)`
- **Actual result**: `Dashboard(0), Transfers(1), Finance(2), Benefits(4), InvestmentsTab(3)` — wrong
- **Expected result**: `Dashboard(0), Transfers(1), Finance(2), InvestmentsTab(3), Benefits(4)`

---

## Current State: Complete Code Flow

### 1. Annotation Layer — Ordinal IS Defined

In `quo-vadis-annotations`, the `@TabItem` annotation declares an `ordinal` field:

```kotlin
// quo-vadis-annotations/.../TabAnnotations.kt
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TabItem(
    val parent: KClass<*>,
    val ordinal: Int,
)
```

### 2. KSP Extraction — Ordinal IS Preserved and Sorted

The `TabExtractor` reads the ordinal from annotations and sorts by it:

```kotlin
// quo-vadis-ksp/.../TabExtractor.kt
val tabs = children
    .sortedBy { (_, ordinal) -> ordinal }  // ← sorted by ordinal at KSP time
    .mapNotNull { (childDecl, ordinal) -> extractTabItem(childDecl, ordinal) }
```

### 3. KSP Code Generation — Ordinal Used ONLY for Ordering `tab()` Calls

The `ContainerBlockGenerator` generates DSL code with tabs sorted by ordinal:

```kotlin
// quo-vadis-ksp/.../ContainerBlockGenerator.kt
private fun generateTabsBlock(tab: TabInfo): CodeBlock {
    // ...
    // Generate tab entries sorted by ordinal; index 0 is initial tab
    tab.tabs.sortedBy { it.ordinal }.forEach { tabItem ->
        builder.add(generateTabEntry(tabItem))
    }
    // ...
}
```

The generated DSL code looks like:

```kotlin
// Generated for feature-dashboard (sees ordinals 0,1,2,4):
tabs<MainNavigation>(scopeKey = "mainNavigationTabs") {
    tab(DashboardDestination)  // ordinal 0
    tab(Transfers)             // ordinal 1
    tab(Finance)               // ordinal 2
    tab(Benefits)              // ordinal 4
}

// Generated for feature-investments (sees ordinal 3):
tabs<MainNavigation>(scopeKey = "mainNavigationTabs") {
    tab(InvestmentsTab)        // ordinal 3
}
```

**Critical observation**: The ordinal value is used to ORDER the `tab()` calls during code generation, but the ordinal value itself is NOT emitted into the generated DSL code. The `tab()` DSL method has no `ordinal` parameter.

### 4. Runtime DSL — No Ordinal Information

The `TabsBuilder.tab()` method stores tabs as a simple list in insertion order:

```kotlin
// quo-vadis-core/.../TabsBuilder.kt
fun tab(
    destination: NavDestination,
    title: String? = null,
    icon: Any? = null
) {
    tabs.add(     // ← just appends to list, no ordinal info
        TabEntry.FlatScreen(
            destination = destination,
            destinationClass = destination::class,
            title = title,
            icon = icon
        )
    )
}
```

`TabEntry` has NO ordinal field:

```kotlin
sealed class TabEntry {
    abstract val title: String?
    abstract val icon: Any?
    // ← NO ordinal property
    
    data class FlatScreen(
        val destination: NavDestination,
        val destinationClass: KClass<out NavDestination>,
        override val title: String?,
        override val icon: Any?
    ) : TabEntry()
    // ...
}
```

### 5. `DslNavigationConfig.buildTabNode` — Ordinal Lost

When building `TabNode`, tabs are processed in list order (insertion order):

```kotlin
// quo-vadis-core/.../DslNavigationConfig.kt
private fun buildTabNode(builder: ContainerBuilder.Tabs, key: String, parentKey: String?): TabNode {
    val config = builder.config
    val stacks = config.tabs.mapIndexed { index, tabEntry ->
        buildTabStack(tabEntry, key, index)    // ← index is position in list, not ordinal
    }
    val tabMetadata = config.tabs.map { tabEntry ->
        GeneratedTabMetadata(route = getTabRoute(tabEntry))  // ← only route, no ordinal
    }
    // ...
}
```

`GeneratedTabMetadata` stores only the route:

```kotlin
data class GeneratedTabMetadata(
    val route: String   // ← NO ordinal field
)
```

### 6. The `+` Operator — Creates `CompositeNavigationConfig`

```kotlin
// NavigationConfig interface
operator fun plus(other: NavigationConfig): NavigationConfig

// DslNavigationConfig implementation
override fun plus(other: NavigationConfig): NavigationConfig {
    if (other === EmptyNavigationConfig) return this
    return CompositeNavigationConfig(this, other)
}
```

### 7. `CompositeNavigationConfig.buildNavNode` — The Merge Point

When both configs define a `TabNode` for the same scope key, `buildNavNode` detects this and calls `mergeTabNodes`:

```kotlin
override fun buildNavNode(
    destinationClass: KClass<out NavDestination>,
    key: String?,
    parentKey: String?
): NavNode? {
    val secondaryNode = secondary.buildNavNode(destinationClass, key, parentKey)
    val primaryNode = primary.buildNavNode(destinationClass, key, parentKey)

    // Merge TabNodes from different modules (cross-module tab support)
    if (primaryNode is TabNode && secondaryNode is TabNode) {
        return mergeTabNodes(primaryNode, secondaryNode)
    }

    return secondaryNode ?: primaryNode
}
```

### 8. `mergeTabNodes` — Pure Concatenation, No Sorting

```kotlin
private fun mergeTabNodes(primary: TabNode, secondary: TabNode): TabNode {
    val primaryRoutes = primary.tabMetadata.map { it.route }.toSet()

    // Find secondary tabs that aren't already in primary (by route)
    val newIndices = secondary.tabMetadata.indices
        .filter { i -> secondary.tabMetadata[i].route !in primaryRoutes }

    // Re-key secondary stacks with indices continuing after primary
    val tabNodeKey = primary.key.value
    val additionalStacks = newIndices.mapIndexed { offset, secondaryIndex ->
        val newTabIndex = primary.stacks.size + offset
        val newStackKey = "$tabNodeKey/tab$newTabIndex"
        rekeyStack(secondary.stacks[secondaryIndex], newStackKey, tabNodeKey)
    }
    val additionalMetadata = newIndices.map { secondary.tabMetadata[it] }

    // ← CONCATENATION: primary.stacks + additionalStacks
    // ← NO SORTING BY ORDINAL (ordinal info doesn't exist at this point)
    return TabNode(
        key = primary.key,
        parentKey = primary.parentKey,
        stacks = primary.stacks + additionalStacks,
        activeStackIndex = ...,
        tabMetadata = primary.tabMetadata + additionalMetadata,
        scopeKey = primary.scopeKey ?: secondary.scopeKey
    )
}
```

---

## Root Cause Analysis

The ordinal information is **lost at the DSL boundary**:

```
@TabItem(ordinal=3)  →  KSP reads ordinal  →  sorts tab() calls by ordinal
                                                        ↓
                         Generated DSL:  tab(InvestmentsTab)   // no ordinal param
                                                        ↓
                         TabsBuilder.tab()  →  appends to List<TabEntry>  // no ordinal
                                                        ↓
                         BuiltTabsConfig(tabs = listOf(...))  // position = implicit ordinal
                                                        ↓
                         buildTabNode()  →  TabNode(stacks = [...])  // index-based
                                                        ↓
                         mergeTabNodes()  →  primary.stacks + secondary.stacks  // CONCATENATION
```

Within a single module, the ordinals work correctly because KSP generates `tab()` calls in ordinal order, so list position == ordinal. But across modules, each module generates its own subset of tabs in their local ordinal order. When merged at runtime, secondary tabs are always appended after primary tabs.

In this case:
- **primary** (dashboard): `[Dashboard(0), Transfers(1), Finance(2), Benefits(4)]` — note: positions 0,1,2,3 in list
- **secondary** (investments): `[InvestmentsTab(3)]` — position 0 in its own list
- **merged**: `[Dashboard, Transfers, Finance, Benefits, InvestmentsTab]` — Benefits before InvestmentsTab

---

## Where a Fix Should Be Applied

### Option A: Library Fix — Add Ordinal to `GeneratedTabMetadata` (Recommended)

**Where**: quo-vadis library

1. Add `ordinal: Int` to `GeneratedTabMetadata`
2. Add `ordinal: Int?` parameter to `TabsBuilder.tab()` and `TabEntry`
3. Have KSP-generated code pass ordinal: `tab(DashboardDestination, ordinal = 0)`
4. Store ordinal in `TabNode.tabMetadata`
5. In `CompositeNavigationConfig.mergeTabNodes()`, sort the merged stacks by ordinal

**Pros**: Fully automatic, ordinal semantics preserved end-to-end  
**Cons**: Requires quo-vadis library change, new release

### Option B: Library Fix — Sort in `mergeTabNodes` by Route Convention

**Where**: `CompositeNavigationConfig.mergeTabNodes()` in quo-vadis

Since ordinal info is lost, could use a convention-based sort if routes encode order. Not practical in the general case.

**Pros**: No API change  
**Cons**: Fragile, convention-dependent

### Option C: App-Level Workaround — Manual Tab Ordering in `@TabsContainer`

**Where**: `feature-dashboard/.../DashboardScreen.kt` (the `@TabsContainer`)

The `@TabsContainer` wrapper receives `scope.tabs: List<NavDestination>` and renders them. The app could manually reorder `scope.tabs` by pattern-matching destination types, regardless of their list position.

**Pros**: No library change needed  
**Cons**: Manual, fragile, must be updated when tabs change

### Option D: App-Level Workaround — Put All Tabs in One Module

Move the `InvestmentsTab` `@TabItem` annotation to `feature-dashboard` (or a shared navigation module) so all tabs are in one KSP compilation unit.

**Pros**: Simple, works today  
**Cons**: Defeats the purpose of cross-module tab discovery, creates module coupling

---

## Recommendation

**Option A** is the correct long-term fix. The specific changes needed in quo-vadis:

1. **`GeneratedTabMetadata`** — add `val ordinal: Int = -1`
2. **`TabEntry`** — add `val ordinal: Int = -1` to each variant
3. **`TabsBuilder.tab()`** — add `ordinal: Int = -1` parameter, pass through to `TabEntry`
4. **KSP `ContainerBlockGenerator.generateTabEntry()`** — emit `ordinal = N` in generated code
5. **`DslNavigationConfig.buildTabNode()`** — pass ordinal to `GeneratedTabMetadata`
6. **`CompositeNavigationConfig.mergeTabNodes()`** — after concatenating, sort by ordinal:

```kotlin
private fun mergeTabNodes(primary: TabNode, secondary: TabNode): TabNode {
    // ... existing dedup and rekey logic ...

    val mergedStacks = primary.stacks + additionalStacks
    val mergedMetadata = primary.tabMetadata + additionalMetadata

    // Sort by ordinal if available
    val sortedIndices = mergedMetadata.indices.sortedBy { mergedMetadata[it].ordinal }
    val sortedStacks = sortedIndices.map { mergedStacks[it] }
    val sortedMetadata = sortedIndices.map { mergedMetadata[it] }

    return TabNode(
        key = primary.key,
        parentKey = primary.parentKey,
        stacks = sortedStacks,
        activeStackIndex = ...,
        tabMetadata = sortedMetadata,
        scopeKey = primary.scopeKey ?: secondary.scopeKey
    )
}
```

---

## Relevant Files

### quo-vadis library (`/Users/kcelebi/Projects/quo-vadis`)

| File | Relevance |
|------|-----------|
| `quo-vadis-annotations/.../TabAnnotations.kt` | `@TabItem` annotation with `ordinal` param |
| `quo-vadis-ksp/.../TabExtractor.kt` | Reads ordinal from annotations, sorts by ordinal |
| `quo-vadis-ksp/.../ContainerBlockGenerator.kt` | Generates DSL code sorted by ordinal, but doesn't emit ordinal value |
| `quo-vadis-core/.../TabsBuilder.kt` | `tab()` DSL — no ordinal parameter |
| `quo-vadis-core/.../TabsBuilder.kt` → `TabEntry` | No ordinal field |
| `quo-vadis-core/.../GeneratedTabMetadata.kt` | Only has `route`, no ordinal |
| `quo-vadis-core/.../DslNavigationConfig.kt` → `buildTabNode` | Builds TabNode from list position |
| `quo-vadis-core/.../CompositeNavigationConfig.kt` → `mergeTabNodes` | **THE FIX POINT** — concatenates without sorting |
| `quo-vadis-core/.../NavigationConfig.kt` | Interface defining `plus` operator |

### peopay-kmp (`/Users/kcelebi/Projects/peopay-kmp`)

| File | Relevance |
|------|-----------|
| `feature-main-navigation-api/.../MainNavigation.kt` | `@Tabs(name = "mainNavigationTabs")` definition |
| `feature-dashboard/.../DashboardDestination.kt` | Defines tabs with ordinals 0, 1, 2, 4 |
| `feature-investments/.../InvestmentsDestination.kt` | Defines tab with ordinal 3 |
| `composeApp/.../AppModule.kt` | Composes configs: `FeatureDashboardNavigationConfig + FeatureInvestmentsNavigationConfig + ...` |

---

## Open Questions

1. **Backward compatibility**: Adding `ordinal` to `TabEntry` and `TabsBuilder.tab()` with default `-1` preserves backward compat for hand-written DSL code. Only KSP-generated code would pass explicit ordinals. The sort in `mergeTabNodes` should treat `-1` as "append at end" (stable sort, no reorder).

2. **Re-keying after sort**: After sorting merged stacks by ordinal, the stack keys (e.g., `"mainNavigationTabs/tab0"`, `"mainNavigationTabs/tab4"`) need to be reassigned sequentially (`tab0`, `tab1`, `tab2`, `tab3`, `tab4`) to avoid gaps that might break `switchTab(index)` calls.

3. **Cross-module ordinal collisions**: The KSP validation (`validateOrdinalCollisions`) currently skips cross-module `@Tabs` since it can only see a subset. Collisions across modules (e.g., two modules both define ordinal=2) should be detected at runtime in `mergeTabNodes` or reported as a warning.
