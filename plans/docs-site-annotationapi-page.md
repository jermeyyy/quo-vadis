# AnnotationAPI.tsx Refactoring - Implementation Plan

## Overview

This plan details the refactoring of the AnnotationAPI.tsx page to utilize shared infrastructure components and centralized code examples. The page documents the annotation-based API for Quo Vadis navigation.

**File**: `docs/site/src/pages/Features/AnnotationAPI/AnnotationAPI.tsx`  
**Lines**: ~640 lines  
**Prerequisite**: [plans/docs-site-shared-infrastructure.md](docs-site-shared-infrastructure.md) must be implemented first

---

## Duplication Analysis Summary

| Content | Lines | Shared Source | Impact |
|---------|-------|---------------|--------|
| `destinationExample` | L11-27 | `codeExamples.stackDestinationComprehensive` | High - 17 lines |
| `screenExample` | L40-61 | `codeExamples.screenBindingBasic` | High - 22 lines |
| `tabsExample` | L63-77 | `codeExamples.tabsAnnotationBasic` | High - 15 lines |
| `tabsContainerExample` | L79-97 | `codeExamples.tabsContainerWrapper` | Medium - 19 lines |
| `paneExample` | L99-113 | `codeExamples.paneAnnotationBasic` | High - 15 lines |
| `paneContainerExample` | L115-133 | `codeExamples.paneContainerWrapper` | Medium - 19 lines |
| `generatedExample` | L150-163 | New: `codeExamples.generatedConfigUsage` | Medium - 14 lines |
| TransitionType grid | L369-390 | `TransitionTypesDisplay` component | High - visual consistency |

**Total Estimated Savings**: ~120 lines of duplicated code + visual consistency for transition display

---

## Task 1: Import Shared Components and Data

**Dependencies**: None  
**Estimated Effort**: Small

### Current State (L1-4):
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'
```

### Target State:
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { TransitionTypesDisplay } from '@components/TransitionTypesDisplay'
import {
  stackDestinationComprehensive,
  screenBindingBasic,
  tabsAnnotationBasic,
  tabsContainerWrapper,
  paneAnnotationBasic,
  paneContainerWrapper,
  generatedConfigUsage,
} from '@data/codeExamples'
import styles from '../Features.module.css'
```

### Notes:
- Add `generatedConfigUsage` to codeExamples.ts if not present (see Task 7)
- Import path uses `@data/` alias (verify tsconfig paths)

---

## Task 2: Replace destinationExample

**Dependencies**: Task 1  
**Estimated Effort**: Small

### Current State (L11-27):
```tsx
const destinationExample = `@Stack(name = "home", startDestination = HomeDestination.Feed::class)
sealed class HomeDestination : NavDestination {

    // Simple destination (no arguments)
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    // Destination with a path parameter
    @Destination(route = "home/article/{articleId}")
    data class Article(@Argument val articleId: String) : HomeDestination()

    // Destination with multiple parameters
    @Destination(route = "home/user/{userId}/post/{postId}")
    data class UserPost(
        @Argument val userId: String,
        @Argument val postId: String
    ) : HomeDestination()
}`
```

### Target State:
**DELETE** the `destinationExample` const entirely. The import from Task 1 already provides `stackDestinationComprehensive`.

### Usage Update (L234):

**Before:**
```tsx
<CodeBlock code={destinationExample} language="kotlin" />
```

**After:**
```tsx
<CodeBlock code={stackDestinationComprehensive} language="kotlin" />
```

---

## Task 3: Replace screenExample

**Dependencies**: Task 1  
**Estimated Effort**: Small

### Current State (L40-61):
```tsx
const screenExample = `// Simple destination (data object) - navigator only
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    Column {
        Text("Feed")
        Button(onClick = { navigator.navigate(HomeDestination.Article("123")) }) {
            Text("View Article")
        }
    }
}

// Destination with arguments (data class) - access destination data
@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(
    destination: HomeDestination.Article,
    navigator: Navigator
) {
    Column {
        Text("Article: \${destination.articleId}")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}`
```

### Target State:
**DELETE** the `screenExample` const entirely. Use `screenBindingBasic` from imports.

### Usage Update (L297):

**Before:**
```tsx
<CodeBlock code={screenExample} language="kotlin" />
```

**After:**
```tsx
<CodeBlock code={screenBindingBasic} language="kotlin" />
```

---

## Task 4: Replace tabsExample

**Dependencies**: Task 1  
**Estimated Effort**: Small

### Current State (L63-77):
```tsx
const tabsExample = `@Tabs(
    name = "mainTabs",
    initialTab = MainTabs.HomeTab::class,
    items = [MainTabs.HomeTab::class, MainTabs.ExploreTab::class, MainTabs.ProfileTab::class]
)
sealed class MainTabs : NavDestination {

    @TabItem(label = "Home", icon = "home")
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()
...`
```

### Target State:
**DELETE** the `tabsExample` const entirely. Use `tabsAnnotationBasic` from imports.

### Usage Update (L307):

**Before:**
```tsx
<CodeBlock code={tabsExample} language="kotlin" />
```

**After:**
```tsx
<CodeBlock code={tabsAnnotationBasic} language="kotlin" />
```

---

## Task 5: Replace tabsContainerExample

**Dependencies**: Task 1  
**Estimated Effort**: Small

### Current State (L79-97):
```tsx
const tabsContainerExample = `@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                scope.tabMetadata.forEachIndexed { index, meta ->
...`
```

### Target State:
**DELETE** the `tabsContainerExample` const entirely. Use `tabsContainerWrapper` from imports.

### Usage Update (L325):

**Before:**
```tsx
<CodeBlock code={tabsContainerExample} language="kotlin" />
```

**After:**
```tsx
<CodeBlock code={tabsContainerWrapper} language="kotlin" />
```

---

## Task 6: Replace paneExample and paneContainerExample

**Dependencies**: Task 1  
**Estimated Effort**: Small

### Current State (L99-133):
```tsx
const paneExample = `@Pane(name = "messagesPane", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class MessagesPane : NavDestination {

    @PaneItem(role = PaneRole.PRIMARY)
    @Destination(route = "messages/conversations")
    data object ConversationList : MessagesPane()
...`

const paneContainerExample = `@PaneContainer(MessagesPane::class)
@Composable
fun MessagesPaneContainer(
    scope: PaneContainerScope,
    content: @Composable () -> Unit
) {
...`
```

### Target State:
**DELETE** both `paneExample` and `paneContainerExample` consts. Use `paneAnnotationBasic` and `paneContainerWrapper` from imports.

### Usage Updates:

**Pane section (L346):**
```tsx
// Before
<CodeBlock code={paneExample} language="kotlin" />

// After
<CodeBlock code={paneAnnotationBasic} language="kotlin" />
```

**PaneContainer section (L360):**
```tsx
// Before
<CodeBlock code={paneContainerExample} language="kotlin" />

// After
<CodeBlock code={paneContainerWrapper} language="kotlin" />
```

---

## Task 7: Add generatedConfigUsage to codeExamples.ts

**Dependencies**: Shared infrastructure (Task 1.2 from shared-infrastructure.md)  
**Estimated Effort**: Small

### Prerequisite:
The shared infrastructure plan needs an additional code example. Add to `src/data/codeExamples.ts`:

```typescript
// ============================================================================
// GENERATED CODE USAGE EXAMPLES
// ============================================================================

/**
 * Generated NavigationConfig usage - type-safe navigation
 */
export const generatedConfigUsage = `// Generated NavigationConfig usage
val navigator = TreeNavigator(
    config = GeneratedNavigationConfig,
    initialState = GeneratedNavigationConfig.buildNavNode(
        HomeDestination::class, 
        null
    )!!
)

// Type-safe navigation (generated)
navigator.navigate(HomeDestination.Article(articleId = "123"))
navigator.navigate(MainTabs.ProfileTab)`;
```

### Update CodeExampleKey Type:
```typescript
export type CodeExampleKey = 
  // ... existing keys
  | 'generatedConfigUsage';
```

---

## Task 8: Replace generatedExample

**Dependencies**: Task 7, Task 1  
**Estimated Effort**: Small

### Current State (L150-163):
```tsx
const generatedExample = `// Generated NavigationConfig usage
val navigator = TreeNavigator(
    config = GeneratedNavigationConfig,
    initialState = GeneratedNavigationConfig.buildNavNode(
        HomeDestination::class, 
        null
    )!!
)

// Type-safe navigation (generated)
navigator.navigate(HomeDestination.Article(articleId = "123"))
navigator.navigate(MainTabs.ProfileTab)`
```

### Target State:
**DELETE** the `generatedExample` const entirely. Use `generatedConfigUsage` from imports.

### Usage Update (L386):

**Before:**
```tsx
<CodeBlock code={generatedExample} language="kotlin" />
```

**After:**
```tsx
<CodeBlock code={generatedConfigUsage} language="kotlin" />
```

---

## Task 9: Replace TransitionType Grid with TransitionTypesDisplay Component

**Dependencies**: Task 1 (TransitionTypesDisplay component from shared infrastructure)  
**Estimated Effort**: Medium

### Current State (L369-390):
```tsx
<h3>TransitionType Options</h3>
<div className={styles.transitionGrid}>
  <div className={styles.transitionCard}>
    <h4>SlideHorizontal</h4>
    <p>Platform-like horizontal slide (default)</p>
  </div>
  <div className={styles.transitionCard}>
    <h4>SlideVertical</h4>
    <p>Bottom-to-top for modals</p>
  </div>
  <div className={styles.transitionCard}>
    <h4>Fade</h4>
    <p>Simple crossfade</p>
  </div>
  <div className={styles.transitionCard}>
    <h4>None</h4>
    <p>Instant switch</p>
  </div>
  <div className={styles.transitionCard}>
    <h4>Custom</h4>
    <p>User-defined animation</p>
  </div>
</div>
```

### Target State:
```tsx
<h3>TransitionType Options</h3>
<TransitionTypesDisplay 
  variant="grid" 
  transitions={[
    { name: 'SlideHorizontal', description: 'Platform-like horizontal slide (default)' },
    { name: 'SlideVertical', description: 'Bottom-to-top for modals' },
    { name: 'Fade', description: 'Simple crossfade' },
    { name: 'None', description: 'Instant switch' },
    { name: 'Custom', description: 'User-defined animation' },
  ]}
/>
```

### Notes:
- Uses `grid` variant to match existing visual style
- Custom `transitions` prop to preserve the page-specific descriptions
- Could alternatively use default transitions if descriptions are updated in shared component

---

## Task 10: Add Cross-References to Related Documentation

**Dependencies**: None (can be done independently)  
**Estimated Effort**: Small

### Enhancement:
Add explicit cross-references to related documentation pages where appropriate.

### Route Patterns Section (after L238):
Add a note linking to DeepLinks page:

**Current State:**
```tsx
<h3>Route Patterns</h3>
<ul>
  <li><strong>Static route:</strong> <code>"home/feed"</code> — No parameters</li>
  ...
</ul>
```

**Target State:**
```tsx
<h3>Route Patterns</h3>
<ul>
  <li><strong>Static route:</strong> <code>"home/feed"</code> — No parameters</li>
  ...
</ul>
<p className={styles.crossRef}>
  See <Link to="/features/deep-linking">Deep Linking</Link> for advanced route configuration and URL handling.
</p>
```

### Transitions Section (after L368):
Add cross-reference to Transitions page:

```tsx
<p className={styles.crossRef}>
  For detailed animation customization, see <Link to="/features/transitions">Transitions</Link>.
</p>
```

---

## Task 11: Remove Unused Code Examples (Cleanup)

**Dependencies**: Tasks 2-8 completed  
**Estimated Effort**: Small

### Code Examples to Delete:
After all replacements are made, remove these const declarations:

1. `stackExample` (L5-8) - not used after refactoring
2. `destinationExample` (L11-27) - replaced by import
3. `screenExample` (L40-61) - replaced by import
4. `tabsExample` (L63-77) - replaced by import
5. `tabsContainerExample` (L79-97) - replaced by import
6. `paneExample` (L99-113) - replaced by import
7. `paneContainerExample` (L115-133) - replaced by import
8. `generatedExample` (L150-163) - replaced by import

### Code Examples to Keep (unique to this page):
- `stackExample` (simplified version for @Stack section)
- `argumentExample` - specific to @Argument annotation documentation
- `transitionExample` - specific @Transition annotation examples
- `completeExample` - comprehensive example combining all annotations
- `errorFormatExample` - validation error format documentation
- `errorMessageExample` - validation error example

---

## Implementation Sequence

```
Task 7: Add generatedConfigUsage to codeExamples.ts
    │
    ▼
Task 1: Update imports in AnnotationAPI.tsx
    │
    ├──────────────────┬──────────────────┐
    ▼                  ▼                  ▼
Task 2: Replace    Task 3: Replace    Task 4: Replace
destinationExample screenExample      tabsExample
    │                  │                  │
    ▼                  ▼                  ▼
Task 5: Replace    Task 6: Replace    Task 8: Replace
tabsContainerEx    pane examples      generatedExample
    │                  │                  │
    └──────────────────┴──────────────────┘
                       │
                       ▼
            Task 9: TransitionTypesDisplay
                       │
                       ▼
            Task 10: Add cross-references
                       │
                       ▼
            Task 11: Cleanup unused consts
```

---

## Full Before/After Comparison

### Before (imports section):
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const stackExample = `@Stack(name = "home", startDestination = HomeDestination.Feed::class)
sealed class HomeDestination : NavDestination {
    // Destinations defined here
}`

const destinationExample = `@Stack(name = "home", startDestination = HomeDestination.Feed::class)
sealed class HomeDestination : NavDestination {
    // ... 17 lines omitted
}`

const argumentExample = `@Destination(route = "products/detail/{id}")
// ... kept as-is
`

const screenExample = `// Simple destination (data object) - navigator only
@Screen(HomeDestination.Feed::class)
// ... 22 lines omitted
}`

const tabsExample = `@Tabs(
// ... 15 lines omitted
}`

const tabsContainerExample = `@TabsContainer(MainTabs::class)
// ... 19 lines omitted
}`

const paneExample = `@Pane(name = "messagesPane", 
// ... 15 lines omitted
}`

const paneContainerExample = `@PaneContainer(MessagesPane::class)
// ... 19 lines omitted
}`

const transitionExample = `@Transition(type = TransitionType.SlideHorizontal)
// ... kept as-is
`

const generatedExample = `// Generated NavigationConfig usage
// ... 14 lines omitted
}`

// ... rest of consts
```

### After (imports section):
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { TransitionTypesDisplay } from '@components/TransitionTypesDisplay'
import {
  stackDestinationComprehensive,
  screenBindingBasic,
  tabsAnnotationBasic,
  tabsContainerWrapper,
  paneAnnotationBasic,
  paneContainerWrapper,
  generatedConfigUsage,
} from '@data/codeExamples'
import styles from '../Features.module.css'

const stackExample = `@Stack(name = "home", startDestination = HomeDestination.Feed::class)
sealed class HomeDestination : NavDestination {
    // Destinations defined here
}`

const argumentExample = `@Destination(route = "products/detail/{id}")
// ... kept as-is
`

const transitionExample = `@Transition(type = TransitionType.SlideHorizontal)
// ... kept as-is
`

const completeExample = `// Define a stack with destinations
// ... kept as-is (unique comprehensive example)
`

const errorFormatExample = `{Description} in file '{fileName}' (line {lineNumber}). Fix: {Suggestion}`

const errorMessageExample = `Missing @Screen binding for 'HomeDestination.Feed' ...`
```

**Lines saved**: ~100+ lines of duplicated code examples

---

## Acceptance Criteria

### Task Completion Checklist:
- [ ] Task 7: `generatedConfigUsage` added to codeExamples.ts
- [ ] Task 1: All imports added to AnnotationAPI.tsx
- [ ] Task 2: `destinationExample` replaced with import
- [ ] Task 3: `screenExample` replaced with import
- [ ] Task 4: `tabsExample` replaced with import
- [ ] Task 5: `tabsContainerExample` replaced with import
- [ ] Task 6: Pane examples replaced with imports
- [ ] Task 8: `generatedExample` replaced with import
- [ ] Task 9: TransitionType grid uses TransitionTypesDisplay component
- [ ] Task 10: Cross-references added to DeepLinks and Transitions pages
- [ ] Task 11: Unused const declarations removed

### Verification:
- [ ] Page renders correctly with no visual changes
- [ ] All code blocks display properly
- [ ] All internal links work correctly
- [ ] No TypeScript errors
- [ ] Build succeeds: `npm run build`

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Code example content differs slightly | Medium | Low | Compare character-by-character before replacing |
| Import path alias not configured | Low | Medium | Verify tsconfig.json has `@data/` path mapping |
| TransitionTypesDisplay styling mismatch | Medium | Medium | Pass custom transitions to match existing descriptions |
| Cross-reference links change | Low | Low | Use route constants if available |

---

## Notes for Implementation

1. **Order matters**: Complete shared infrastructure first, then this page refactoring
2. **Test incrementally**: After each task, verify the page still renders correctly
3. **Preserve unique content**: Keep `argumentExample`, `transitionExample`, `completeExample`, and error examples as they are unique to this page
4. **CSS considerations**: The `TransitionTypesDisplay` component uses its own module CSS - verify it matches the existing `styles.transitionGrid` appearance
5. **Consider adding CSS class for cross-references**: Add `.crossRef` to Features.module.css if not present
