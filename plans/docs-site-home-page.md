# Home.tsx Refactoring - Implementation Plan

## Overview

This plan details the refactoring of `src/pages/Home/Home.tsx` to eliminate content duplication by leveraging the shared infrastructure established in `plans/docs-site-shared-infrastructure.md`.

**Prerequisite**: All tasks in `plans/docs-site-shared-infrastructure.md` Phase 1 (constants.ts, codeExamples.ts) must be completed before this refactoring can begin.

**File Location**: `docs/site/src/pages/Home/Home.tsx`

---

## Current State Analysis

### Code Block Inventory

| Variable | Lines | Content | Action |
|----------|-------|---------|--------|
| `quickstartCode` | 5-16 | libs.versions.toml config | Replace with `versionCatalogConfig` |
| `step1Code` | 18-29 | @Stack/@Destination example | Replace with `stackDestinationBasic` |
| `step2Code` | 31-53 | @Screen binding example | Replace with `screenBindingBasic` |
| `step3Code` | 55-75 | NavigationHost setup | Replace with `navigationHostBasic` |
| `manualDSLCode` | 77-91 | Manual DSL config | Keep (unique to Home) |

### Version String Locations

| Line | Current Value | Source After Refactor |
|------|---------------|----------------------|
| 6 | `quoVadis = "0.3.4"` | `LIBRARY_VERSION` from constants.ts |
| 7 | `ksp = "2.3.0"` | `KSP_VERSION` from constants.ts |

### Section Inventory

| Section | Lines | Content | Action |
|---------|-------|---------|--------|
| Hero | 94-113 | Title, badges, CTA buttons | Keep as-is |
| Overview | 115-124 | Library description | Keep as-is |
| Why Quo Vadis | 126-170 | 11 feature cards | Keep (concise summaries, add links) |
| Quickstart | 172-186 | Version catalog code | Use shared example |
| Show Me The Code | 188-228 | 3-step tutorial | Use shared examples |
| Manual DSL | 230-238 | Alternative API | Keep (unique) |
| Resources | 240-251 | Links section | Keep as-is |

---

## Implementation Tasks

### Task 1: Add Imports

**Lines to modify**: 1-4 (top of file)

**Before**:
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './Home.module.css'
```

**After**:
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { LIBRARY_VERSION, KSP_VERSION, GRADLE_PLUGIN_ID, MAVEN_ARTIFACTS } from '@data/constants'
import {
  stackDestinationBasic,
  screenBindingBasic,
  navigationHostBasic,
  versionCatalogConfig,
} from '@data/codeExamples'
import styles from './Home.module.css'
```

**Dependencies**: 
- Task 1.1 from infrastructure plan (constants.ts exists)
- Task 1.2 from infrastructure plan (codeExamples.ts exists)

**Notes**:
- Assumes `@data` path alias is configured in tsconfig/vite
- If not, use relative path: `../../data/constants`

---

### Task 2: Remove Inline `quickstartCode` Definition

**Lines to remove**: 5-16

**Before**:
```tsx
const quickstartCode = `[versions]
quoVadis = "0.3.4"
ksp = "2.3.0"

[libraries]
quo-vadis-core = { module = "io.github.jermeyyy:quo-vadis-core", version.ref = "quoVadis" }
quo-vadis-annotations = { module = "io.github.jermeyyy:quo-vadis-annotations", version.ref = "quoVadis" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
quo-vadis = { id = "io.github.jermeyyy.quo-vadis", version.ref = "quoVadis" }`
```

**After**: Delete entirely - use `versionCatalogConfig` from imports

**Usage update** (in Quickstart section, around line 182):

```tsx
// Before
<CodeBlock code={quickstartCode} language="bash" title="libs.versions.toml" />

// After
<CodeBlock code={versionCatalogConfig} language="bash" title="libs.versions.toml" />
```

**Dependencies**: Task 1

---

### Task 3: Remove Inline `step1Code` Definition

**Lines to remove**: 18-29

**Before**:
```tsx
const step1Code = `// Define a navigation stack with destinations
@Stack(name = "home", startDestination = Feed::class)
sealed class HomeDestination : NavDestination {

    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    @Destination(route = "home/article/{articleId}")
    data class Article(
        @Argument val articleId: String,
        @Argument(optional = true) val showComments: Boolean = false
    ) : HomeDestination()
}`
```

**After**: Delete entirely - use `stackDestinationBasic` from imports

**Usage update** (in Step 1 section):

```tsx
// Before
<CodeBlock code={step1Code} language="kotlin" title="HomeDestination.kt" />

// After  
<CodeBlock code={stackDestinationBasic} language="kotlin" title="HomeDestination.kt" />
```

**Note**: The `stackDestinationBasic` in codeExamples.ts doesn't include the leading comment `// Define a navigation stack with destinations`. If this comment is desired, either:
1. Add it to the shared example in codeExamples.ts, OR
2. Keep a small wrapper: `const step1CodeWithComment = \`// Define a navigation stack with destinations\n${stackDestinationBasic}\``

**Recommendation**: Add comment to shared example since it improves documentation value.

**Dependencies**: Task 1

---

### Task 4: Remove Inline `step2Code` Definition

**Lines to remove**: 31-53

**Before**:
```tsx
const step2Code = `// Bind screens with @Screen annotation
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    Column {
        Text("Feed")
        Button(onClick = { 
            navigator.navigate(HomeDestination.Article(articleId = "123"))
        }) {
            Text("View Article")
        }
    }
}

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

**After**: Delete entirely - use `screenBindingBasic` from imports

**Usage update** (in Step 2 section):

```tsx
// Before
<CodeBlock code={step2Code} language="kotlin" title="Screens.kt" />

// After
<CodeBlock code={screenBindingBasic} language="kotlin" title="Screens.kt" />
```

**Note**: Similar to Task 3, the leading comment `// Bind screens with @Screen annotation` would need to be added to the shared example or handled locally.

**Dependencies**: Task 1

---

### Task 5: Remove Inline `step3Code` Definition

**Lines to remove**: 55-75

**Before**:
```tsx
const step3Code = `@Composable
fun App() {
    val config = GeneratedNavigationConfig
    
    val initialState = remember {
        config.buildNavNode(
            destinationClass = HomeDestination::class,
            parentKey = null
        )!!
    }
    
    val navigator = remember {
        TreeNavigator(
            config = config,
            initialState = initialState
        )
    }
    
    NavigationHost(
        navigator = navigator,
        screenRegistry = config.screenRegistry
    )
}`
```

**After**: Delete entirely - use `navigationHostBasic` from imports

**Usage update** (in Step 3 section):

```tsx
// Before
<CodeBlock code={step3Code} language="kotlin" title="App.kt" />

// After
<CodeBlock code={navigationHostBasic} language="kotlin" title="App.kt" />
```

**Dependencies**: Task 1

---

### Task 6: Keep `manualDSLCode` (No Change)

**Lines**: 77-91

**Rationale**: This code example is unique to the Home page and demonstrates an alternative API approach. It's not duplicated elsewhere. Keep as-is.

---

### Task 7: Update Feature Cards with Navigation Links

**Lines**: 126-170 (Why Quo Vadis section)

**Current State**: 11 feature cards with brief descriptions

**Recommended Updates**:
Add `<Link>` to relevant cards pointing to detailed documentation:

| Card | Add Link To |
|------|-------------|
| Zero String Routes | `/features#type-safety` |
| True Multiplatform | `/features/multiplatform` |
| Zero Boilerplate | `/features/annotation-api` |
| Modern Architecture | `/features/architecture` |
| Deep Links & URLs | `/features/deep-links` |
| Beautiful Transitions | `/features/transitions` |
| Tabbed Navigation | `/features/tabbed-navigation` |
| FlowMVI Ready | `/features/di-integration` |

**Example modification for one card**:

```tsx
// Before
<div className={styles.featureCard}>
  <h4>Zero String Routes</h4>
  <p>Compile-time safe navigation with no runtime crashes. Say goodbye to string-based routing errors and hello to type safety.</p>
</div>

// After
<div className={styles.featureCard}>
  <h4>Zero String Routes</h4>
  <p>Compile-time safe navigation with no runtime crashes. Say goodbye to string-based routing errors and hello to type safety.</p>
  <Link to="/features#type-safety" className={styles.featureLink}>Learn more →</Link>
</div>
```

**CSS Addition** (to Home.module.css):
```css
.featureLink {
  display: inline-block;
  margin-top: 0.75rem;
  font-size: 0.875rem;
  color: var(--color-primary);
  text-decoration: none;
}

.featureLink:hover {
  text-decoration: underline;
}
```

**Dependencies**: None (CSS change is self-contained)

---

### Task 8: Clean Up Unused CSS (Post-Refactor)

**File**: `src/pages/Home/Home.module.css`

After all code changes, audit CSS for unused classes. Potential candidates:
- Any classes only used by removed code blocks
- Duplicate styling that can be consolidated

**Process**:
1. Complete Tasks 1-7
2. Run build to verify no errors
3. Use IDE/tooling to identify unused CSS classes
4. Remove confirmed unused styles

**Dependencies**: Tasks 1-7 complete

---

## Refactored File Structure

After all tasks complete, Home.tsx should look like:

```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { LIBRARY_VERSION, KSP_VERSION, GRADLE_PLUGIN_ID, MAVEN_ARTIFACTS } from '@data/constants'
import {
  stackDestinationBasic,
  screenBindingBasic,
  navigationHostBasic,
  versionCatalogConfig,
} from '@data/codeExamples'
import styles from './Home.module.css'

// Only manualDSLCode remains as inline definition (unique to this page)
const manualDSLCode = `// Programmatic configuration with DSL
val appConfig = navigationConfig {
    // Register screens
    screen<HomeDestination.Feed> { destination, _, _ ->
        { FeedContent() }
    }
    
    screen<HomeDestination.Article> { destination, _, _ ->
        { ArticleContent(destination.articleId) }
    }
    
    // Register transitions
    transition<HomeDestination.Article>(NavTransition.SlideHorizontal)
}`

export default function Home() {
  return (
    <article className={styles.home}>
      {/* Hero Section - unchanged */}
      
      {/* Overview - unchanged */}
      
      {/* Why Quo Vadis - feature cards with added links */}
      
      {/* Quickstart Section */}
      <section className={styles.quickstart}>
        <h2>Get Started in 10 Minutes</h2>
        <p>...</p>
        <CodeBlock code={versionCatalogConfig} language="bash" title="libs.versions.toml" />
        {/* ... */}
      </section>

      {/* Show Me The Code */}
      <section>
        <h2 id="code-example">Show Me The Code!</h2>
        <div className={styles.steps}>
          <div className={styles.step}>
            <h3>Step 1: Define Your Destinations</h3>
            <CodeBlock code={stackDestinationBasic} language="kotlin" title="HomeDestination.kt" />
          </div>

          <div className={styles.step}>
            <h3>Step 2: Define Your Screens</h3>
            <CodeBlock code={screenBindingBasic} language="kotlin" title="Screens.kt" />
          </div>

          <div className={styles.step}>
            <h3>Step 3: Set Up Navigation</h3>
            <CodeBlock code={navigationHostBasic} language="kotlin" title="App.kt" />
          </div>
        </div>
      </section>

      {/* Manual DSL - uses local manualDSLCode */}
      
      {/* Resources - unchanged */}
    </article>
  )
}
```

---

## Code Diff Summary

### Lines Removed: ~71 lines
- `quickstartCode` definition: 12 lines
- `step1Code` definition: 12 lines  
- `step2Code` definition: 23 lines
- `step3Code` definition: 21 lines

### Lines Added: ~10 lines
- Import statements: 7 lines
- Feature card links: ~11 lines (optional enhancement)
- CSS for feature links: 10 lines (optional)

### Net Change: ~-50 lines (16% reduction)

---

## Task Dependencies & Sequencing

```
Infrastructure Prerequisites (from docs-site-shared-infrastructure.md)
├── Task 1.1: constants.ts ─────┐
└── Task 1.2: codeExamples.ts ──┤
                                │
Home.tsx Refactoring ───────────┘
├── Task 1: Add Imports (blocks 2-5)
├── Task 2: Remove quickstartCode
├── Task 3: Remove step1Code
├── Task 4: Remove step2Code
├── Task 5: Remove step3Code
├── Task 6: Keep manualDSLCode (no-op)
├── Task 7: Add feature card links (optional, independent)
└── Task 8: Clean up CSS (after 1-7)
```

**Execution Order**:
1. Verify infrastructure prerequisites are complete
2. Task 1 (imports) - must be first
3. Tasks 2-5 can be done in any order (recommend sequential for review clarity)
4. Task 7 (feature links) - independent, can be done in parallel
5. Task 8 (CSS cleanup) - must be last

---

## Verification Checklist

### Pre-Implementation
- [ ] `src/data/constants.ts` exists and exports `LIBRARY_VERSION`, `KSP_VERSION`
- [ ] `src/data/codeExamples.ts` exists and exports all required examples
- [ ] Path alias `@data` is configured OR relative path is used

### Post-Implementation
- [ ] `npm run build` completes without errors
- [ ] `npm run dev` - Home page renders correctly
- [ ] All code blocks display with correct syntax highlighting
- [ ] Version numbers in quickstart match `LIBRARY_VERSION` value
- [ ] No TypeScript errors in IDE
- [ ] Feature card links navigate to correct pages (if Task 7 implemented)

### Visual Regression
- [ ] Hero section appearance unchanged
- [ ] Code blocks render identically
- [ ] Feature cards layout unchanged
- [ ] Step indicators and notes display correctly
- [ ] Mobile responsive layout works

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Path alias not configured | Medium | Low | Fall back to relative imports |
| Shared examples differ slightly | Medium | Medium | Update shared examples to match or add wrapper |
| Missing comment headers in shared code | High | Low | Add comments to shared examples in codeExamples.ts |
| Feature links point to non-existent anchors | Medium | Low | Verify anchor IDs exist before merging |

---

## Notes for Implementer

1. **Comment Preservation**: The current inline code blocks have leading comments like `// Define a navigation stack with destinations`. Decide whether to:
   - Add these to the shared examples in `codeExamples.ts` (recommended)
   - Create local wrappers that prepend comments
   - Accept slight difference in displayed code

2. **versionCatalogConfig Match**: Verify `versionCatalogConfig` in `codeExamples.ts` produces identical output to current `quickstartCode`. Key differences to check:
   - Line ordering
   - Whitespace/indentation
   - All plugin/library entries present

3. **Future Consideration**: If more pages need the `manualDSLCode` example, consider moving it to `codeExamples.ts` as well.

4. **Path Alias Setup**: If `@data` alias doesn't exist, add to `vite.config.ts`:
   ```ts
   resolve: {
     alias: {
       '@data': path.resolve(__dirname, './src/data'),
     },
   },
   ```
   And to `tsconfig.json`:
   ```json
   "paths": {
     "@data/*": ["./src/data/*"]
   }
   ```
