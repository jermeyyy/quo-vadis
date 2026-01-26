# DSLConfig.tsx Page Refactoring - Implementation Plan

## Overview

This plan details the refactoring of [DSLConfig.tsx](../docs/site/src/pages/Features/DSLConfig/DSLConfig.tsx) to use shared infrastructure components and eliminate content duplication.

**Prerequisite**: [docs-site-shared-infrastructure.md](docs-site-shared-infrastructure.md) must be implemented first.

**Current State**: 559 lines with 6 identified duplications  
**Target State**: ~380 lines using shared components and centralized code examples

---

## Identified Duplications

| Content | Lines | Duplicated In | Shared Solution |
|---------|-------|---------------|-----------------|
| `navigationConfigInterface` | L14-26 | Modular.tsx, TypeSafe.tsx | Import from `codeExamples.ts` |
| `tabsExample` | L63-77 | AnnotationAPI.tsx, TabbedNavigation.tsx | Import from `codeExamples.ts` |
| `panesExample` | L79-93 | AnnotationAPI.tsx, PaneLayouts.tsx | Import from `codeExamples.ts` |
| TabsContainerScope table | L490-519 | TabbedNavigation.tsx | `<ScopePropertiesTable scopeType="tabs" />` |
| PaneContainerScope table | L521-550 | PaneLayouts.tsx | `<ScopePropertiesTable scopeType="pane" />` |
| Preset transitions table | L557-577 | Transitions.tsx | `<TransitionTypesDisplay variant="table" />` |

---

## Task 1: Import Shared Components and Data

**Description**: Add imports for shared components and centralized code examples.

**File**: `src/pages/Features/DSLConfig/DSLConfig.tsx`

### Before (L1-4)
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'
```

### After
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { ScopePropertiesTable } from '@components/ScopePropertiesTable'
import { TransitionTypesDisplay } from '@components/TransitionTypesDisplay'
import {
  navigationConfigInterface,
  tabsAnnotationDSL,
  panesAnnotationDSL,
} from '@data/codeExamples'
import styles from '../Features.module.css'
```

**Dependencies**: 
- Task 1.2 from shared infrastructure (codeExamples.ts)
- Task 2.2 from shared infrastructure (TransitionTypesDisplay)
- Task 2.4 from shared infrastructure (ScopePropertiesTable)

---

## Task 2: Add New Code Examples to codeExamples.ts

**Description**: The DSL config page uses DSL-style code examples that differ from annotation-based examples. These need to be added to the centralized file.

**File**: `src/data/codeExamples.ts`

### Add the following examples:

```typescript
// ============================================================================
// DSL CONFIGURATION EXAMPLES
// ============================================================================

/**
 * NavigationConfig interface definition
 */
export const navigationConfigInterface = `interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val scopeRegistry: ScopeRegistry
    val transitionRegistry: TransitionRegistry
    val containerRegistry: ContainerRegistry
    val deepLinkRegistry: DeepLinkRegistry
    val paneRoleRegistry: PaneRoleRegistry
    
    fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String? = null,
        parentKey: String? = null
    ): NavNode?
    
    operator fun plus(other: NavigationConfig): NavigationConfig
}`;

/**
 * DSL-style tabs configuration
 */
export const tabsAnnotationDSL = `navigationConfig {
    tabs<MainTabs>("main-tabs") {
        initialTab = 0
        
        // Simple flat tabs
        tab(HomeTab, title = "Home", icon = Icons.Default.Home)
        tab(SearchTab, title = "Search", icon = Icons.Default.Search)
        
        // Tab with nested navigation stack
        tab(ProfileTab, title = "Profile", icon = Icons.Default.Person) {
            screen<ProfileScreen>()
            screen<EditProfileScreen>()
            screen<ProfileSettingsScreen>()
        }
    }
}`;

/**
 * DSL-style panes configuration
 */
export const panesAnnotationDSL = `navigationConfig {
    panes<ListDetailPanes>("list-detail") {
        initialPane = PaneRole.Primary
        backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        
        primary(weight = 0.4f, minWidth = 300.dp) {
            root(ListScreen)
            alwaysVisible()
        }
        
        secondary(weight = 0.6f) {
            root(DetailPlaceholder)
        }
    }
}`;
```

**Also update the type export**:
```typescript
export type CodeExampleKey = 
  // ... existing keys ...
  | 'navigationConfigInterface'
  | 'tabsAnnotationDSL'
  | 'panesAnnotationDSL';
```

**Dependencies**: Task 1.1 from shared infrastructure (constants.ts)

---

## Task 3: Remove Inline Code Constants

**Description**: Remove the inline code example constants that will be imported from centralized file.

**File**: `src/pages/Features/DSLConfig/DSLConfig.tsx`

### Remove the following blocks:

**Remove `navigationConfigInterface` (L14-26)**:
```tsx
// DELETE THIS BLOCK
const navigationConfigInterface = `interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val scopeRegistry: ScopeRegistry
    val transitionRegistry: TransitionRegistry
    val containerRegistry: ContainerRegistry
    val deepLinkRegistry: DeepLinkRegistry
    val paneRoleRegistry: PaneRoleRegistry
    
    fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String? = null,
        parentKey: String? = null
    ): NavNode?
    
    operator fun plus(other: NavigationConfig): NavigationConfig
}`
```

**Remove `tabsExample` (L63-77)**:
```tsx
// DELETE THIS BLOCK
const tabsExample = `navigationConfig {
    tabs<MainTabs>("main-tabs") {
        initialTab = 0
        
        // Simple flat tabs
        tab(HomeTab, title = "Home", icon = Icons.Default.Home)
        tab(SearchTab, title = "Search", icon = Icons.Default.Search)
        
        // Tab with nested navigation stack
        tab(ProfileTab, title = "Profile", icon = Icons.Default.Person) {
            screen<ProfileScreen>()
            screen<EditProfileScreen>()
            screen<ProfileSettingsScreen>()
        }
    }
}`
```

**Remove `panesExample` (L79-93)**:
```tsx
// DELETE THIS BLOCK
const panesExample = `navigationConfig {
    panes<ListDetailPanes>("list-detail") {
        initialPane = PaneRole.Primary
        backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        
        primary(weight = 0.4f, minWidth = 300.dp) {
            root(ListScreen)
            alwaysVisible()
        }
        
        secondary(weight = 0.6f) {
            root(DetailPlaceholder)
        }
    }
}`
```

**Dependencies**: Task 1, Task 2

---

## Task 4: Update Code Block References

**Description**: Update CodeBlock components to use imported example names.

**File**: `src/pages/Features/DSLConfig/DSLConfig.tsx`

### 4.1 Update tabsExample reference

**Before**:
```tsx
<CodeBlock code={tabsExample} language="kotlin" />
```

**After**:
```tsx
<CodeBlock code={tabsAnnotationDSL} language="kotlin" />
```

### 4.2 Update panesExample reference

**Before**:
```tsx
<CodeBlock code={panesExample} language="kotlin" />
```

**After**:
```tsx
<CodeBlock code={panesAnnotationDSL} language="kotlin" />
```

**Note**: The `navigationConfigInterface` CodeBlock reference already matches the import name, no change needed.

**Dependencies**: Task 1, Task 3

---

## Task 5: Replace TabsContainerScope Properties Table

**Description**: Replace the inline table with the shared ScopePropertiesTable component.

**File**: `src/pages/Features/DSLConfig/DSLConfig.tsx`

### Before (L490-519)
```tsx
        <h4>TabsContainerScope Properties</h4>
        <table>
          <thead>
            <tr>
              <th>Property</th>
              <th>Type</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>navigator</code></td>
              <td><code>Navigator</code></td>
              <td>Navigator instance for programmatic navigation</td>
            </tr>
            <tr>
              <td><code>activeTabIndex</code></td>
              <td><code>Int</code></td>
              <td>Currently selected tab (0-based)</td>
            </tr>
            <tr>
              <td><code>tabCount</code></td>
              <td><code>Int</code></td>
              <td>Total number of tabs</td>
            </tr>
            <tr>
              <td><code>tabMetadata</code></td>
              <td><code>List&lt;TabMetadata&gt;</code></td>
              <td>Labels, icons, and routes for all tabs</td>
            </tr>
            <tr>
              <td><code>isTransitioning</code></td>
              <td><code>Boolean</code></td>
              <td>Whether tab switch animation is in progress</td>
            </tr>
          </tbody>
        </table>
```

### After
```tsx
        <ScopePropertiesTable scopeType="tabs" />
```

**Dependencies**: Task 1

---

## Task 6: Replace PaneContainerScope Properties Table

**Description**: Replace the inline table with the shared ScopePropertiesTable component.

**File**: `src/pages/Features/DSLConfig/DSLConfig.tsx`

### Before (L521-550)
```tsx
        <h4>PaneContainerScope Properties</h4>
        <table>
          <thead>
            <tr>
              <th>Property</th>
              <th>Type</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>navigator</code></td>
              <td><code>Navigator</code></td>
              <td>Navigator instance</td>
            </tr>
            <tr>
              <td><code>activePaneRole</code></td>
              <td><code>PaneRole</code></td>
              <td>Currently active pane</td>
            </tr>
            <tr>
              <td><code>isExpanded</code></td>
              <td><code>Boolean</code></td>
              <td>Multi-pane mode active</td>
            </tr>
            <tr>
              <td><code>paneContents</code></td>
              <td><code>List&lt;PaneContent&gt;</code></td>
              <td>Content slots for custom layout</td>
            </tr>
            <tr>
              <td><code>isTransitioning</code></td>
              <td><code>Boolean</code></td>
              <td>Pane transition in progress</td>
            </tr>
          </tbody>
        </table>
```

### After
```tsx
        <ScopePropertiesTable scopeType="pane" />
```

**Dependencies**: Task 1

---

## Task 7: Replace Preset Transitions Table

**Description**: Replace the inline transitions table with the shared TransitionTypesDisplay component.

**File**: `src/pages/Features/DSLConfig/DSLConfig.tsx`

### Before (L557-577)
```tsx
        <h3>Preset Transitions</h3>
        <table>
          <thead>
            <tr>
              <th>Transition</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>NavTransition.SlideHorizontal</code></td>
              <td>Slide from right with fade (default for stacks)</td>
            </tr>
            <tr>
              <td><code>NavTransition.SlideVertical</code></td>
              <td>Slide from bottom with fade (modal-style)</td>
            </tr>
            <tr>
              <td><code>NavTransition.Fade</code></td>
              <td>Simple fade in/out</td>
            </tr>
            <tr>
              <td><code>NavTransition.ScaleIn</code></td>
              <td>Scale with fade (zoom effect)</td>
            </tr>
            <tr>
              <td><code>NavTransition.None</code></td>
              <td>Instant switch, no animation</td>
            </tr>
          </tbody>
        </table>
```

### After
```tsx
        <h3>Preset Transitions</h3>
        <TransitionTypesDisplay variant="table" />
```

**Note**: The TransitionTypesDisplay component uses default data that includes more transitions (FadeThrough). If exact parity is required, pass custom `transitions` prop.

**Dependencies**: Task 1

---

## Task 8: Add Cross-Reference Links

**Description**: Add helpful cross-reference links to related documentation pages where more detailed information is available.

**File**: `src/pages/Features/DSLConfig/DSLConfig.tsx`

### 8.1 After TabsContainerScope section, add note:
```tsx
        <ScopePropertiesTable scopeType="tabs" />
        <div className={styles.note}>
          <p>
            See <Link to="/features/tabbed-navigation">Tabbed Navigation</Link> for complete 
            tab configuration patterns and advanced examples.
          </p>
        </div>
```

### 8.2 After PaneContainerScope section, add note:
```tsx
        <ScopePropertiesTable scopeType="pane" />
        <div className={styles.note}>
          <p>
            See <Link to="/features/pane-layouts">Pane Layouts</Link> for comprehensive 
            adaptive layout patterns.
          </p>
        </div>
```

### 8.3 After Preset Transitions section, add note:
```tsx
        <TransitionTypesDisplay variant="table" />
        <div className={styles.note}>
          <p>
            For detailed transition configuration and custom animations, see{' '}
            <Link to="/features/transitions">Transitions & Animations</Link>.
          </p>
        </div>
```

**Dependencies**: Task 5, Task 6, Task 7

---

## Task 9: Clean Up Unused CSS

**Description**: Review and remove any CSS classes that become unused after refactoring. 

**Note**: Since DSLConfig.tsx imports `Features.module.css` which is shared, no CSS removal is needed within this file. The shared components bring their own CSS modules.

**Status**: No action required for this page specifically.

---

## Task Dependencies Graph

```
Task 2 (Add to codeExamples.ts)
    │
    ├──→ Task 1 (Import shared components/data)
    │        │
    │        └──→ Task 3 (Remove inline constants)
    │                  │
    │                  └──→ Task 4 (Update CodeBlock refs)
    │
    └──→ Tasks 5, 6, 7 (Replace tables with components)
              │
              └──→ Task 8 (Add cross-references)
```

**Execution Order**:
1. Task 2 - Add examples to codeExamples.ts
2. Task 1 - Update imports in DSLConfig.tsx
3. Task 3 - Remove inline constants
4. Task 4 - Update CodeBlock variable references
5. Tasks 5, 6, 7 - Replace tables (can be parallel)
6. Task 8 - Add cross-reference notes

---

## Validation Checklist

### Before Each Task
- [ ] Prerequisite tasks completed
- [ ] Shared infrastructure components exist

### After All Tasks
- [ ] `npm run build` completes without errors
- [ ] `npm run dev` loads the page correctly
- [ ] All code blocks render with syntax highlighting
- [ ] All tables display with correct data
- [ ] Cross-reference links navigate correctly
- [ ] No console errors in browser
- [ ] Visual regression: page looks identical to before refactoring

---

## Estimated Impact

| Metric | Before | After | Change |
|--------|--------|-------|--------|
| Lines of code | 559 | ~380 | -32% |
| Inline code examples | 14 | 11 | -3 |
| Duplicate tables | 3 | 0 | -100% |
| Shared components used | 0 | 2 | +2 |
| Centralized imports | 0 | 3 | +3 |

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Import path mismatch | Low | Medium | Verify `@components` and `@data` aliases exist in vite.config.ts |
| Component prop mismatch | Low | Low | TransitionTypesDisplay default data covers all DSLConfig table entries |
| Missing cross-reference routes | Low | Low | Verify routes exist in router configuration |
| CSS variable differences | Low | Low | Shared components use same CSS variables as Features.module.css |

---

## Notes for Implementation

1. **ScopePropertiesTable Note**: The shared component includes `switchTab(index)` function in tabs scope properties which the original DSLConfig.tsx doesn't list. This is an improvement, not a regression.

2. **TransitionTypesDisplay Note**: The shared component includes `FadeThrough` which isn't in the original DSLConfig.tsx table. This adds information rather than removing it.

3. **Code Example Naming**: Using `tabsAnnotationDSL` and `panesAnnotationDSL` to distinguish from annotation-based examples (`tabsAnnotationBasic`) in the shared file.
