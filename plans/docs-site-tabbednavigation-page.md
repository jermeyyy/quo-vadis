# TabbedNavigation.tsx Page Refactoring - Implementation Plan

## Overview

This plan details the refactoring of [TabbedNavigation.tsx](src/pages/Features/TabbedNavigation/TabbedNavigation.tsx) to eliminate duplicated content by leveraging shared infrastructure components.

**Important**: TabbedNavigation.tsx is the **canonical source** for tabbed navigation documentation. While we centralize code examples and components for consistency, this page remains the authoritative reference that other pages link TO.

**Prerequisite**: [plans/docs-site-shared-infrastructure.md](plans/docs-site-shared-infrastructure.md) must be implemented first.

---

## Current File Analysis

### File Location
`docs/site/src/pages/Features/TabbedNavigation/TabbedNavigation.tsx`

### Current Structure
- **Lines 1-4**: Imports
- **Lines 6-7**: IconNa component (status icon)
- **Lines 9-40**: `tabsAnnotationCode` - @Tabs/@TabItem example **(DUPLICATED)**
- **Lines 42-54**: `tabNodeStructureCode` - TabNode data class
- **Lines 56-77**: `tabsContainerCode` - @TabsContainer example
- **Lines 79-104**: `nestedStacksCode` - Nested stacks within tabs
- **Lines 106-135**: `sharedStateCode` - SharedNavigationContainer example
- **Lines 137-244**: Component JSX with sections

### Identified Duplications

| Content | Location | Also Found In | Resolution |
|---------|----------|---------------|------------|
| `tabsAnnotationCode` (L9-40) | @Tabs/@TabItem with nested stack | `tabsAnnotationBasic` in AnnotationAPI.tsx, similar in DSLConfig.tsx | Import `tabsAnnotationWithNestedStack` from codeExamples.ts |
| TabsContainerScope properties table (L207-231) | 6 properties | DSLConfig.tsx (L287-318) | Use `ScopePropertiesTable` component |
| `IconNa` component (L6-7) | Status icon | Multiple features pages | Already in Features.module.css, but consider shared StatusIcon |

---

## Implementation Tasks

### Task 1: Import Shared Components and Data

**File**: `src/pages/Features/TabbedNavigation/TabbedNavigation.tsx`

**Action**: Add imports for shared infrastructure

**Before (Lines 1-4)**:
```tsx
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

// Status icon for required/no default values in tables
const IconNa = () => <span className={`${styles.statusIcon} ${styles.statusIconNa}`} />
```

**After**:
```tsx
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { ScopePropertiesTable } from '@components/ScopePropertiesTable'
import { tabsAnnotationWithNestedStack } from '@data/codeExamples'
import styles from '../Features.module.css'
```

**Notes**:
- Remove `IconNa` component (not used in refactored version - replaced by removing the tables that use it or using shared component)
- Verify `@components` and `@data` path aliases exist in tsconfig.json

**Acceptance Criteria**:
- [ ] Imports added for `ScopePropertiesTable` component
- [ ] Import added for `tabsAnnotationWithNestedStack` from codeExamples
- [ ] `IconNa` component definition removed (the @Tabs/@TabItem properties tables still need it - see Task 4)

---

### Task 2: Replace tabsAnnotationCode with Shared Example

**File**: `src/pages/Features/TabbedNavigation/TabbedNavigation.tsx`

**Action**: Remove inline code definition and use imported constant

**Before (Lines 9-40)**:
```tsx
const tabsAnnotationCode = `@Tabs(
    name = "mainTabs",
    initialTab = MainTabs.HomeTab::class,
    items = [MainTabs.HomeTab::class, MainTabs.ExploreTab::class, 
             MainTabs.ProfileTab::class, MainTabs.SettingsTab::class]
)
sealed class MainTabs : NavDestination {

    @TabItem(label = "Home", icon = "home")
    @Destination(route = "main/home")
    @Transition(type = TransitionType.Fade)
    data object HomeTab : MainTabs()

    @TabItem(label = "Explore", icon = "explore")
    @Destination(route = "main/explore")
    @Transition(type = TransitionType.Fade)
    data object ExploreTab : MainTabs()

    @TabItem(label = "Profile", icon = "person")
    @Destination(route = "main/profile")
    @Transition(type = TransitionType.Fade)
    data object ProfileTab : MainTabs()

    @TabItem(label = "Settings", icon = "settings")
    @Stack(name = "settingsTabStack", startDestination = SettingsTab.Main::class)
    @Transition(type = TransitionType.Fade)
    sealed class SettingsTab : MainTabs() {
        @Destination(route = "settings/main")
        data object Main : SettingsTab()

        @Destination(route = "settings/profile")
        @Transition(type = TransitionType.SlideHorizontal)
        data object Profile : SettingsTab()
    }
}`
```

**After**:
```tsx
// tabsAnnotationCode removed - using tabsAnnotationWithNestedStack from @data/codeExamples
```

**Update CodeBlock usage (around L149)**:

**Before**:
```tsx
<CodeBlock code={tabsAnnotationCode} language="kotlin" />
```

**After**:
```tsx
<CodeBlock code={tabsAnnotationWithNestedStack} language="kotlin" />
```

**Acceptance Criteria**:
- [ ] `tabsAnnotationCode` constant removed
- [ ] CodeBlock uses `tabsAnnotationWithNestedStack` import
- [ ] No visual change in rendered output (same code example displayed)

---

### Task 3: Replace TabsContainerScope Properties Table

**File**: `src/pages/Features/TabbedNavigation/TabbedNavigation.tsx`

**Action**: Replace inline table with `ScopePropertiesTable` component

**Before (Lines 207-231)**:
```tsx
<section>
  <h2 id="container-scope">TabsContainerScope Properties</h2>
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
        <td>Navigation operations</td>
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
        <td>Labels, icons, routes for tabs</td>
      </tr>
      <tr>
        <td><code>isTransitioning</code></td>
        <td><code>Boolean</code></td>
        <td>Whether transition is in progress</td>
      </tr>
      <tr>
        <td><code>switchTab(index)</code></td>
        <td>Function</td>
        <td>Switch to different tab</td>
      </tr>
    </tbody>
  </table>
</section>
```

**After**:
```tsx
<section>
  <h2 id="container-scope">TabsContainerScope Properties</h2>
  <ScopePropertiesTable scopeType="tabs" />
</section>
```

**Acceptance Criteria**:
- [ ] Inline table removed
- [ ] `ScopePropertiesTable` component used with `scopeType="tabs"`
- [ ] Same 6 properties displayed (navigator, activeTabIndex, tabCount, tabMetadata, isTransitioning, switchTab)
- [ ] Section heading preserved

---

### Task 4: Keep IconNa for Remaining Tables

**File**: `src/pages/Features/TabbedNavigation/TabbedNavigation.tsx`

**Analysis**: The `IconNa` component is used in the `@Tabs Properties` and `@TabItem Properties` tables (Lines 152-185). These are **not duplicated** content - they're specific to this canonical page.

**Action**: Keep the `IconNa` component for these remaining tables

**After Task 1 (revised imports)**:
```tsx
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { ScopePropertiesTable } from '@components/ScopePropertiesTable'
import { tabsAnnotationWithNestedStack } from '@data/codeExamples'
import styles from '../Features.module.css'

// Status icon for required/no default values in tables
const IconNa = () => <span className={`${styles.statusIcon} ${styles.statusIconNa}`} />
```

**Acceptance Criteria**:
- [ ] `IconNa` component retained
- [ ] @Tabs Properties table (L152-172) renders correctly with IconNa
- [ ] @TabItem Properties table (L174-194) renders correctly with IconNa

---

### Task 5: Verify Other Pages Reference This Page

**Context**: As the canonical source for tabbed navigation, other pages should link TO this page rather than duplicating content.

**Pages to Verify**:

| Page | Expected Reference | Verify |
|------|-------------------|--------|
| [AnnotationAPI.tsx](src/pages/Features/AnnotationAPI/AnnotationAPI.tsx) | Should link to `/features/tabbed-navigation` for details | Check "Next Steps" section |
| [DSLConfig.tsx](src/pages/Features/DSLConfig/DSLConfig.tsx) | Should link to `/features/tabbed-navigation` for tab details | Check "Next Steps" section |
| [Features.tsx](src/pages/Features/Features.tsx) | Feature card should link to tabbed navigation page | Check feature grid |

**Action**: After shared infrastructure is implemented, verify these pages use shared components AND link to TabbedNavigation as canonical source.

**No code changes required for TabbedNavigation.tsx** - this is a verification task for the orchestrator.

**Acceptance Criteria**:
- [ ] AnnotationAPI.tsx links to `/features/tabbed-navigation` (already does: L271)
- [ ] DSLConfig.tsx links to `/features/tabbed-navigation` (already does: L341)
- [ ] Features.tsx feature card links to this page

---

### Task 6: Clean Up Unused Code

**File**: `src/pages/Features/TabbedNavigation/TabbedNavigation.tsx`

**Action**: Verify no orphaned code after refactoring

**Checklist**:
- [ ] No unused imports
- [ ] No unused const declarations
- [ ] `tabsAnnotationCode` removed (replaced by import)
- [ ] All remaining const declarations (`tabNodeStructureCode`, `tabsContainerCode`, `nestedStacksCode`, `sharedStateCode`) are still used

**Final Code Constants to Keep**:
1. `tabNodeStructureCode` - Unique to this page (TabNode structure)
2. `tabsContainerCode` - Could potentially be shared, but keep for now as primary source
3. `nestedStacksCode` - Unique to this page (detailed nested example)
4. `sharedStateCode` - Unique to this page (MVI integration)

**Acceptance Criteria**:
- [ ] No ESLint warnings for unused variables
- [ ] Build completes without errors
- [ ] Page renders correctly

---

## Final File Structure

After refactoring, the file structure will be:

```tsx
// Imports
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { ScopePropertiesTable } from '@components/ScopePropertiesTable'
import { tabsAnnotationWithNestedStack } from '@data/codeExamples'
import styles from '../Features.module.css'

// Local component (still needed for @Tabs/@TabItem property tables)
const IconNa = () => <span className={`${styles.statusIcon} ${styles.statusIconNa}`} />

// Unique code examples (kept - canonical source)
const tabNodeStructureCode = `...`
const tabsContainerCode = `...`
const nestedStacksCode = `...`
const sharedStateCode = `...`

// Component
export default function TabbedNavigation() {
  return (
    <article className={styles.features}>
      {/* ... sections using:
        - tabsAnnotationWithNestedStack (shared)
        - tabNodeStructureCode (local)
        - tabsContainerCode (local)
        - ScopePropertiesTable component (shared)
        - nestedStacksCode (local)
        - sharedStateCode (local)
      */}
    </article>
  )
}
```

---

## Dependencies

```
docs-site-shared-infrastructure.md
├── Task 1.1: constants.ts
├── Task 1.2: codeExamples.ts ─────────────────┐
│   └── tabsAnnotationWithNestedStack          │
└── Task 2.4: ScopePropertiesTable ────────────┤
                                               │
docs-site-tabbednavigation-page.md ────────────┘
├── Task 1: Import shared components (depends on infrastructure)
├── Task 2: Replace tabsAnnotationCode (depends on Task 1)
├── Task 3: Replace TabsContainerScope table (depends on Task 1)
├── Task 4: Keep IconNa (no dependency)
├── Task 5: Verify references (post-implementation)
└── Task 6: Clean up (after Tasks 2-4)
```

---

## Acceptance Criteria Summary

### Before Completion:
- [ ] Shared infrastructure implemented (constants.ts, codeExamples.ts, ScopePropertiesTable)
- [ ] Path aliases configured (`@components`, `@data`)

### After Completion:
- [ ] `tabsAnnotationWithNestedStack` imported from `@data/codeExamples`
- [ ] `ScopePropertiesTable` component imported and used
- [ ] Inline `tabsAnnotationCode` removed
- [ ] Inline TabsContainerScope table replaced
- [ ] `IconNa` kept for @Tabs/@TabItem property tables
- [ ] No unused code
- [ ] Page renders identically to before (visual regression check)
- [ ] `npm run build` succeeds without errors

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Path alias not configured | Medium | High | Check tsconfig.json and vite.config.ts before starting |
| Shared component styling mismatch | Low | Medium | ScopePropertiesTable uses same CSS variables as Features.module.css |
| Missing export in codeExamples.ts | Low | High | Verify `tabsAnnotationWithNestedStack` is exported |
| Table content drift | Low | Low | ScopePropertiesTable has default data matching current content |

---

## Post-Implementation Notes

After this refactoring:

1. **codeExamples.ts becomes single source** for `tabsAnnotationWithNestedStack`
2. **ScopePropertiesTable** is reusable for DSLConfig.tsx (which also has TabsContainerScope table)
3. **TabbedNavigation.tsx remains canonical** - other pages should reference it for details
4. **Unique content preserved** - tabNodeStructureCode, nestedStacksCode, sharedStateCode stay local

### Future Considerations

- `tabsContainerCode` could be moved to codeExamples.ts as `tabsContainerWrapper` (already defined in infrastructure plan)
- Consider creating `AnnotationPropertiesTable` component for @Tabs/@TabItem properties if pattern repeats
