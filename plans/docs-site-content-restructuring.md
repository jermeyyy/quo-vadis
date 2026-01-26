# Documentation Site Content Restructuring Plan

## Overview

This plan restructures the Quo Vadis documentation site to eliminate content duplication and establish clear information ownership. The goal: **each piece of information appears ONCE in its appropriate category, then is LINKED/REFERENCED from elsewhere**.

### Current Issues
- ~15+ code examples hardcoded across pages instead of centralized
- NavNode tree architecture explained 3+ times
- Platform support lists repeated in 4+ pages  
- "Independent tab backstacks" described in 3 places
- Type-safe/compile-time safety benefits repeated across 4 pages
- Existing reusable components underutilized (NavNodeTypesTable, PlatformSupportGrid)

### Target State
- Single source of truth for each concept
- Centralized code examples in `codeExamples.ts`
- Cross-references via links instead of duplicated content
- Reusable components used consistently

---

## Phase 1: Infrastructure Changes

### Task 1.1: Extend codeExamples.ts with Missing Examples

**Description:** Centralize all hardcoded code examples from pages into `codeExamples.ts`.

**Files Affected:**
- [docs/site/src/data/codeExamples.ts](docs/site/src/data/codeExamples.ts)

**Content to Add:**

| Example Key | Source | Current Location |
|-------------|--------|------------------|
| `navigatorInterfaceCode` | Architecture.tsx | inline |
| `treeNavigatorCode` | Architecture.tsx | inline |
| `treeMutatorCode` | Architecture.tsx & BackStack.tsx | inline (DUPLICATED) |
| `treeStructureExample` | Architecture.tsx & BackStack.tsx | inline (DUPLICATED) |
| `treeTraversalCode` | BackStack.tsx | inline |
| `navigatorBackCode` | BackStack.tsx | inline |
| `tabOperationsCode` | BackStack.tsx | inline |
| `paneOperationsCode` | BackStack.tsx | inline |
| `backWithTabsCode` | BackStack.tsx | inline |
| `backResultCode` | BackStack.tsx | inline |
| `checkBackCode` | BackStack.tsx | inline |
| `deepLinkCode` | Features.tsx | inline |
| `predictiveBackCode` | Features.tsx | inline |
| `sharedElementCode` | Features.tsx | inline |
| `customTransitionCode` | Features.tsx | inline |
| `testingCode` | Features.tsx | inline |
| `modularCode` | Features.tsx | inline |
| `koinCode` | Features.tsx | inline |
| `customInjectionCode` | Features.tsx | inline |

**Dependencies:** None (first task)

---

### Task 1.2: Update Navigation Structure

**Description:** Remove Features.tsx landing page from routes; keep Features children as sidebar items.

**Files Affected:**
- [docs/site/src/data/navigation.ts](docs/site/src/data/navigation.ts)
- [docs/site/src/App.tsx](docs/site/src/App.tsx) (routes)
- `docs/site/src/components/Layout/` (sidebar rendering if needed)

**Changes:**

```diff
// navigation.ts - Features no longer has a path
{
  label: 'Features',
- path: '/features',  // REMOVE - no landing page
  children: [
-   { label: 'Architecture', path: '/features/architecture' },
+   { label: 'Core Concepts', path: '/features/core-concepts' },  // Merged
    { label: 'Annotation-Based API', path: '/features/annotation-api' },
    { label: 'DSL Configuration', path: '/features/dsl-config' },
    { label: 'Type-Safe Navigation', path: '/features/type-safe' },
    { label: 'Multiplatform Support', path: '/features/multiplatform' },
-   { label: 'Stack Management', path: '/features/stack-management' },  // Merged into Core Concepts
    { label: 'Deep Links', path: '/features/deep-links' },
    { label: 'Predictive Back', path: '/features/predictive-back' },
    { label: 'Transitions & Animations', path: '/features/transitions' },
    { label: 'Tabbed Navigation', path: '/features/tabbed-navigation' },
    { label: 'Pane Layouts', path: '/features/pane-layouts' },
-   { label: 'FlowMVI & Koin', path: '/features/di-integration' },
+   { label: 'FlowMVI & Koin', children: [  // Split into subpages
+     { label: 'Core Concepts', path: '/features/di-integration/core-concepts' },
+     { label: 'Usage Guide', path: '/features/di-integration/usage' },
+   ]},
    { label: 'Testing Support', path: '/features/testing' },
    { label: 'Modular Architecture', path: '/features/modular' },
  ]
}
```

**Dependencies:** Task 2.1 (Core Concepts page must exist before navigation update)

---

## Phase 2: Page Merging (Architecture + BackStack → Core Concepts)

### Task 2.1: Create Core Concepts Page

**Description:** Merge Architecture.tsx and BackStack.tsx into a single comprehensive "Core Concepts" page covering:
- NavNode tree architecture
- Node types (StackNode, TabNode, PaneNode, ScreenNode)
- Navigator interface
- TreeMutator operations
- State management patterns

**Files Affected:**
- Create: `docs/site/src/pages/Features/CoreConcepts/CoreConcepts.tsx`
- Create: `docs/site/src/pages/Features/CoreConcepts/index.ts`

**Content Organization:**

```
Core Concepts
├── Overview (from Architecture.tsx)
├── NavNode Tree Structure
│   ├── Tree diagram (from Architecture.tsx)
│   ├── Node types table (USE NavNodeTypesTable component)
│   └── Tree structure example (from Architecture.tsx)
├── Logic Layer
│   ├── Navigator Interface (from Architecture.tsx)
│   ├── TreeNavigator Implementation (from Architecture.tsx)
│   └── TreeMutator (from Architecture.tsx + BackStack.tsx TreeMutator section)
├── Stack Operations (from BackStack.tsx)
│   ├── Navigator back operations
│   ├── TreeMutator operations
│   ├── PopBehavior options
│   └── Tree traversal extensions
├── Tab Operations (from BackStack.tsx, LINK to TabbedNavigation for details)
├── Pane Operations (from BackStack.tsx, LINK to PaneLayouts for details)
├── Data Flow (from Architecture.tsx)
└── Best Practices (combined from both)
```

**Content that becomes LINKS:**
- Tab details → Link to `/features/tabbed-navigation`
- Pane details → Link to `/features/pane-layouts`
- Transitions → Link to `/features/transitions`
- Predictive back → Link to `/features/predictive-back`

**Dependencies:** Task 1.1 (code examples must be centralized first)

---

### Task 2.2: Remove Architecture.tsx and BackStack.tsx

**Description:** Delete the original pages after verifying Core Concepts works.

**Files to Delete:**
- `docs/site/src/pages/Features/Architecture/` (entire directory)
- `docs/site/src/pages/Features/BackStack/` (entire directory)

**Dependencies:** Task 2.1

---

## Phase 3: Page Removal (Features.tsx)

### Task 3.1: Remove Features.tsx Landing Page

**Description:** Delete the Features landing page. All feature navigation moves to sidebar only.

**Files to Delete:**
- [docs/site/src/pages/Features/Features.tsx](docs/site/src/pages/Features/Features.tsx)
- [docs/site/src/pages/Features/Features.module.css](docs/site/src/pages/Features/Features.module.css) (if not shared)

**Note:** Verify `Features.module.css` is not imported by other Feature pages. If shared, keep it.

**Dependencies:** Task 1.2 (navigation must be updated first)

---

## Phase 4: DIIntegration Split

### Task 4.1: Create DIIntegration Core Concepts Subpage

**Description:** Extract MVI fundamentals into dedicated page.

**Files Affected:**
- Create: `docs/site/src/pages/Features/DIIntegration/CoreConcepts/CoreConcepts.tsx`
- Create: `docs/site/src/pages/Features/DIIntegration/CoreConcepts/index.ts`

**Content to Include:**
- MVI Contract (State, Intent, Action)
- Container Types overview:
  - `NavigationContainer` vs `SharedNavigationContainer`
  - `NavigationContainerScope` vs `SharedContainerScope`
- Lifecycle Management (diagram + scope lifecycle table)
- Koin Setup (module registration, both DSL and Annotations approaches)

**Content Stays Here (canonical source):**
- MVI Contract code example
- Lifecycle diagram component
- Scope properties tables
- Container type comparison

---

### Task 4.2: Create DIIntegration Usage Subpage

**Description:** Extract practical usage patterns into dedicated page.

**Files Affected:**
- Create: `docs/site/src/pages/Features/DIIntegration/Usage/Usage.tsx`
- Create: `docs/site/src/pages/Features/DIIntegration/Usage/index.ts`

**Content to Include:**
- rememberContainer usage
- rememberSharedContainer usage
- Common patterns:
  - Navigation from container
  - Conditional navigation
  - Sharing state between tabs
- Multi-module organization
- Complete app setup example
- Best practices (DO/DON'T sections)

**Content that becomes LINKS:**
- Tabbed navigation details → Link to `/features/tabbed-navigation`
- Modular architecture → Link to `/features/modular`
- Deep links with MVI → Link to `/features/deep-links`

---

### Task 4.3: Create DIIntegration Index/Landing Page

**Description:** Simple landing page that links to Core Concepts and Usage subpages.

**Files Affected:**
- Modify or replace: `docs/site/src/pages/Features/DIIntegration/DIIntegration.tsx`

**Content:**
```tsx
// Simple redirect or overview with links to subpages
<article>
  <h1>FlowMVI & Koin Integration</h1>
  <p>Introduction paragraph...</p>
  
  <div className={styles.subpageLinks}>
    <Link to="core-concepts">Core Concepts → MVI contract, lifecycle, container types</Link>
    <Link to="usage">Usage Guide → rememberContainer, patterns, setup</Link>
  </div>
  
  <Dependencies /> // Show dependency code
</article>
```

**Dependencies:** Tasks 4.1, 4.2

---

## Phase 5: Content Deduplication

### Task 5.1: Deduplicate NavNode Tree Architecture

**Description:** Remove duplicate NavNode tree explanations from multiple pages.

**Canonical Source:** Core Concepts page (Task 2.1)

**Pages to Update:**

| Page | Current Content | Replace With |
|------|-----------------|--------------|
| TabbedNavigation.tsx | NavNode tree explanation | Link: "See [Core Concepts](/features/core-concepts#navnode-types) for NavNode tree architecture" |
| PaneLayouts.tsx | NavNode tree explanation | Same link pattern |
| TypeSafe.tsx | Tree state description | Brief mention + link |
| Getting Started | Tree overview | Brief intro + link to Core Concepts |

**Pattern for replacement:**
```tsx
// Before
<p>Quo Vadis uses an immutable NavNode tree to represent navigation state...</p>
// [5-10 lines of explanation]

// After
<p>
  Navigation state is an immutable <Link to="/features/core-concepts#navnode-tree">NavNode tree</Link>.
  Different node types support different patterns.
</p>
```

---

### Task 5.2: Deduplicate Type-Safety Benefits

**Description:** Remove repeated "compile-time safety" and "type-safe" explanations.

**Canonical Source:** TypeSafe.tsx

**Pages to Update:**

| Page | Current Content | Action |
|------|-----------------|--------|
| Features.tsx | Type-safe section | REMOVED (page deleted) |
| AnnotationAPI.tsx | Type-safety mentions | Keep minimal, link to TypeSafe |
| GettingStarted.tsx | Type-safety benefits | Brief mention + link |
| Architecture.tsx | Type-safe references | MERGED into Core Concepts |

---

### Task 5.3: Deduplicate "Independent Tab Backstacks"

**Description:** Consolidate all tab backstack explanations.

**Canonical Source:** TabbedNavigation.tsx

**Pages to Update:**

| Page | Current Content | Replace With |
|------|-----------------|--------------|
| Core Concepts (new) | Brief tab mention | Link: "See [Tabbed Navigation](/features/tabbed-navigation)" |
| Features.tsx | TabNode description | REMOVED (page deleted) |
| Architecture.tsx | Tab example | MERGED, link to TabbedNavigation |

---

### Task 5.4: Deduplicate TreeMutator Code

**Description:** TreeMutator code appears in both Architecture.tsx and BackStack.tsx.

**Action:** After Phase 2, single TreeMutator section exists in Core Concepts.

**Dependencies:** Task 2.1

---

## Phase 6: Component Utilization

### Task 6.1: Use NavNodeTypesTable Component

**Description:** Replace inline node type tables with the existing `NavNodeTypesTable` component.

**Files Affected:**
- Core Concepts (new page)
- BackStack.tsx (before deletion) or verify merged content uses component

**Component Location:** [docs/site/src/components/NavNodeTypesTable/](docs/site/src/components/NavNodeTypesTable/)

**Current Inline Table (to replace):**
```tsx
<table>
  <thead>
    <tr><th>Type</th><th>Purpose</th><th>Contains</th></tr>
  </thead>
  <tbody>
    <tr><td><code>ScreenNode</code></td><td>Leaf destination</td><td>Destination data</td></tr>
    // ... more rows
  </tbody>
</table>
```

**Replace With:**
```tsx
import { NavNodeTypesTable } from '@components/NavNodeTypesTable'

<NavNodeTypesTable />
```

---

### Task 6.2: Expand PlatformSupportGrid Usage

**Description:** Use `PlatformSupportGrid` consistently across all pages that mention platform support.

**Component Location:** [docs/site/src/components/PlatformSupportGrid/](docs/site/src/components/PlatformSupportGrid/)

**Current Usage:** Multiplatform.tsx only

**Add To:**
- Core Concepts (brief variant + link to Multiplatform)
- Getting Started (brief variant)
- Any page listing platforms

**Pattern:**
```tsx
// Full details on Multiplatform page
<PlatformSupportGrid variant="detailed" />

// Brief/card view on other pages with link
<PlatformSupportGrid variant="cards" />
<p>See <Link to="/features/multiplatform">Multiplatform Support</Link> for details.</p>
```

---

### Task 6.3: Use ScopePropertiesTable Consistently

**Description:** Replace inline scope property tables with `ScopePropertiesTable` component.

**Component Location:** [docs/site/src/components/ScopePropertiesTable/](docs/site/src/components/ScopePropertiesTable/)

**Files Affected:**
- DIIntegration Core Concepts (new)
- DIIntegration Usage (new)

**Tables to Replace:**
- NavigationContainerScope properties
- SharedContainerScope properties

---

### Task 6.4: Ensure TransitionTypesDisplay Is Used

**Description:** Verify `TransitionTypesDisplay` is used on Transitions.tsx and Features.tsx (before removal).

**Component Location:** [docs/site/src/components/TransitionTypesDisplay/](docs/site/src/components/TransitionTypesDisplay/)

**Current Usage:** Features.tsx (will be removed)

**Action:** Ensure Transitions.tsx page includes:
```tsx
import { TransitionTypesDisplay } from '@components/TransitionTypesDisplay'

<TransitionTypesDisplay variant="detailed" />
```

---

## Phase 7: Platform Info Consolidation

### Task 7.1: Enhance Multiplatform.tsx as Canonical Platform Reference

**Description:** Consolidate all platform-specific information into Multiplatform.tsx.

**Files Affected:**
- [docs/site/src/pages/Features/Multiplatform/Multiplatform.tsx](docs/site/src/pages/Features/Multiplatform/Multiplatform.tsx)

**Content to Consolidate:**

| Topic | Current Locations | Move to Multiplatform |
|-------|-------------------|----------------------|
| Platform list | Features.tsx, Multiplatform.tsx, Getting Started, Deep Links | Single source |
| Platform-specific setup | Scattered | Unified section |
| Platform feature matrix | Multiplatform.tsx | Enhance with complete data |
| Platform limitations | Various | Dedicated section |

**Structure After Enhancement:**
```
Multiplatform Support
├── Overview (brief, what platforms are supported)
├── Platform Feature Matrix (detailed table)
│   - Android (features, versions, notes)
│   - iOS (features, versions, notes)
│   - Desktop (features, notes)
│   - Web JS/WASM (features, notes)
├── Platform-Specific Setup
│   - Android configuration
│   - iOS configuration
│   - Desktop configuration
│   - Web configuration
├── Platform-Specific Behaviors
│   - Predictive back (Android 13+)
│   - Swipe gestures (iOS)
│   - Deep links per platform
└── Known Limitations
```

---

### Task 7.2: Update Other Pages to Link to Multiplatform

**Description:** Replace inline platform lists with links.

**Pages to Update:**

| Page | Current Content | Replace With |
|------|-----------------|--------------|
| Getting Started | Full platform list | Brief mention + link |
| Deep Links | Platform-specific integration | Link to Multiplatform#platform-specific-setup |
| Predictive Back | Platform support info | Link to Multiplatform#predictive-back |

**Pattern:**
```tsx
// Before
<ul>
  <li><strong>Android:</strong> Intent filters and App Links</li>
  <li><strong>iOS:</strong> Universal Links and custom URL schemes</li>
  <li><strong>Web:</strong> Direct URL navigation</li>
  <li><strong>Desktop:</strong> Custom protocol handlers</li>
</ul>

// After
<p>
  Deep linking works across all <Link to="/features/multiplatform">supported platforms</Link>.
  See platform-specific configuration in the{' '}
  <Link to="/features/multiplatform#deep-link-setup">Multiplatform Guide</Link>.
</p>
```

---

## Phase 8: AnnotationAPI Page Refinement

### Task 8.1: Update AnnotationAPI.tsx

**Description:** Keep AnnotationAPI as annotation reference; remove detailed UI implementation content.

**Content that STAYS:**
- `@Stack`, `@Destination`, `@Screen`, `@Transition` annotation reference
- `@Tabs`, `@TabItem`, `@TabsContainer` annotation syntax
- `@Pane`, `@PaneItem`, `@PaneContainer` annotation syntax
- Generated code overview

**Content that becomes LINKS:**
- TabsContainer UI implementation → Link to TabbedNavigation
- PaneContainer UI implementation → Link to PaneLayouts
- Screen binding patterns → Link to Getting Started

**Pattern:**
```tsx
// Keep annotation syntax
<h3>@TabsContainer</h3>
<p>Marks a composable as the wrapper for a tabs component.</p>
<CodeBlock code={tabsContainerAnnotation} language="kotlin" />

// Replace detailed UI implementation with link
<p>
  For complete UI implementation patterns including custom tab bars and navigation,
  see <Link to="/features/tabbed-navigation#container-wrapper">Tabbed Navigation</Link>.
</p>
```

---

## Implementation Order Summary

```
Phase 1: Infrastructure
├── 1.1 Extend codeExamples.ts (no dependencies)
└── 1.2 Update navigation (depends on 2.1)

Phase 2: Page Merging
├── 2.1 Create Core Concepts (depends on 1.1)
└── 2.2 Remove Architecture + BackStack (depends on 2.1)

Phase 3: Page Removal
└── 3.1 Remove Features.tsx (depends on 1.2)

Phase 4: DIIntegration Split
├── 4.1 Create Core Concepts subpage (no dependencies)
├── 4.2 Create Usage subpage (no dependencies)
└── 4.3 Update DIIntegration index (depends on 4.1, 4.2)

Phase 5: Content Deduplication
├── 5.1 Deduplicate NavNode tree (depends on 2.1)
├── 5.2 Deduplicate type-safety (depends on 3.1)
├── 5.3 Deduplicate tab backstacks (depends on 2.1)
└── 5.4 Deduplicate TreeMutator (depends on 2.1)

Phase 6: Component Utilization
├── 6.1 Use NavNodeTypesTable (depends on 2.1)
├── 6.2 Expand PlatformSupportGrid (no dependencies)
├── 6.3 Use ScopePropertiesTable (depends on 4.1, 4.2)
└── 6.4 Ensure TransitionTypesDisplay used (no dependencies)

Phase 7: Platform Consolidation
├── 7.1 Enhance Multiplatform.tsx (no dependencies)
└── 7.2 Update pages to link (depends on 7.1)

Phase 8: AnnotationAPI Refinement
└── 8.1 Update AnnotationAPI.tsx (depends on 5.x completion)
```

---

## Validation Checklist

After implementation, verify:

- [ ] No code examples are hardcoded in page components (all from codeExamples.ts)
- [ ] NavNode tree architecture explained ONCE (Core Concepts)
- [ ] Platform support detailed ONCE (Multiplatform.tsx)
- [ ] Type-safety benefits explained ONCE (TypeSafe.tsx)
- [ ] Tab backstack behavior explained ONCE (TabbedNavigation.tsx)
- [ ] TreeMutator operations explained ONCE (Core Concepts)
- [ ] `NavNodeTypesTable` component used where node types are shown
- [ ] `PlatformSupportGrid` component used where platforms are listed
- [ ] `ScopePropertiesTable` component used in DIIntegration pages
- [ ] All internal links work correctly
- [ ] Navigation sidebar renders correctly
- [ ] No 404s for removed pages (redirects if needed)
- [ ] Features landing page returns 404 or redirects to first child

---

## Risk Mitigation

### Breaking Links
- Search codebase for all internal links before removing pages
- Implement redirects for commonly linked URLs:
  - `/features` → `/features/core-concepts`
  - `/features/architecture` → `/features/core-concepts`
  - `/features/stack-management` → `/features/core-concepts#stack-operations`

### Lost Content
- Review each removed page thoroughly before deletion
- Verify all code examples are captured in codeExamples.ts
- Ensure diagrams/components are preserved

### Navigation Confusion
- Test sidebar collapse/expand behavior
- Ensure mobile menu works with nested items
- Verify breadcrumbs (if applicable)

---

## Estimated Effort

| Phase | Tasks | Estimated Hours |
|-------|-------|-----------------|
| Phase 1 | 2 | 3-4 |
| Phase 2 | 2 | 4-6 |
| Phase 3 | 1 | 1 |
| Phase 4 | 3 | 4-5 |
| Phase 5 | 4 | 3-4 |
| Phase 6 | 4 | 2-3 |
| Phase 7 | 2 | 3-4 |
| Phase 8 | 1 | 2 |
| **Total** | **19** | **22-29** |

---

## Success Metrics

1. **Code example reuse:** 100% of examples imported from codeExamples.ts
2. **Component utilization:** All 4 reusable components used appropriately
3. **Content uniqueness:** Each major concept explained in exactly one location
4. **Link health:** Zero broken internal links
5. **Page count:** Net reduction of 2 pages (Architecture + BackStack merged, Features removed)
