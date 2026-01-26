# GettingStarted.tsx Refactoring - Implementation Plan

## Overview

This plan details the refactoring of `src/pages/GettingStarted/GettingStarted.tsx` to use shared infrastructure components from `docs-site-shared-infrastructure.md`.

**File Location**: `src/pages/GettingStarted/GettingStarted.tsx`

**Prerequisites**: 
- `src/data/constants.ts` must be created (Task 1.1 from shared infrastructure)
- `src/data/codeExamples.ts` must be created (Task 1.2 from shared infrastructure)

---

## Duplication Analysis Summary

| Content Type | Line Numbers | Current State | Target State |
|--------------|--------------|---------------|--------------|
| Version strings ("0.3.4") | L19, L25, L26, L44, L45, L55 | Hardcoded 6× | Import from constants.ts |
| KSP version ("2.3.0") | L18, L41 | Hardcoded 2× | Import from constants.ts |
| installationGradlePluginCode | L5-36 | Inline const | Import `gradlePluginInstallation` |
| installationManualCode | L38-61 | Inline const | Import `manualKspConfiguration` |
| defineStackCode | L64-81 | Inline const | Import `stackDestinationWithSettings` |
| bindScreensCode | L84-108 | Inline const | Import `screenBindingWithImports` |
| setupNavHostCode | L111-133 | Inline const | Import `navigationHostWithImports` |

**Total reduction**: ~130 lines of duplicated code → ~5 import lines

---

## Task 1: Add Required Imports

**Dependency**: None (first task)

**Current State** (L1-3):
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './GettingStarted.module.css'
```

**Target State**:
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './GettingStarted.module.css'
import { LIBRARY_VERSION } from '@data/constants'
import {
  stackDestinationWithSettings,
  screenBindingWithImports,
  navigationHostWithImports,
  gradlePluginInstallation,
  manualKspConfiguration,
} from '@data/codeExamples'
```

**Notes**:
- `@data` alias should be configured in vite.config.ts (same pattern as `@components`)
- If alias not configured, use relative path: `../../data/constants`

---

## Task 2: Replace installationGradlePluginCode

**Dependency**: Task 1 complete

**Current State** (L5-36):
```tsx
// Installation - Option 1: Gradle Plugin (Recommended)
const installationGradlePluginCode = `// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp") version "2.3.0"
    id("io.github.jermeyyy.quo-vadis") version "0.3.4"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.jermeyyy:quo-vadis-core:0.3.4")
            implementation("io.github.jermeyyy:quo-vadis-annotations:0.3.4")
        }
    }
}

// Optional: Configure the plugin
quoVadis {
    modulePrefix = "MyApp"  // Generates MyAppNavigationConfig
}`
```

**Target State**:
```tsx
// Imported: gradlePluginInstallation from '@data/codeExamples'
// (DELETE the entire const declaration - lines 5-36)
```

**Usage Update** (L152):
```tsx
// Before
<CodeBlock code={installationGradlePluginCode} language="kotlin" title="build.gradle.kts" />

// After
<CodeBlock code={gradlePluginInstallation} language="kotlin" title="build.gradle.kts" />
```

---

## Task 3: Replace installationManualCode

**Dependency**: Task 1 complete

**Current State** (L38-61):
```tsx
// Installation - Option 2: Manual Configuration
const installationManualCode = `// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp") version "2.3.0"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.jermeyyy:quo-vadis-core:0.3.4")
            implementation("io.github.jermeyyy:quo-vadis-annotations:0.3.4")
        }
    }
    
    ksp {
        arg("quoVadis.modulePrefix", "MyApp")
    }
}

dependencies {
    add("kspCommonMainMetadata", "io.github.jermeyyy:quo-vadis-ksp:0.3.4")
}

// Register generated sources
kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

// Fix task dependencies
afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        if (!name.startsWith("ksp") && !name.contains("Test", ignoreCase = true)) {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }
}`
```

**Target State**:
```tsx
// Imported: manualKspConfiguration from '@data/codeExamples'
// (DELETE the entire const declaration - lines 38-61)
```

**Usage Update** (L158):
```tsx
// Before
<CodeBlock code={installationManualCode} language="kotlin" title="build.gradle.kts" />

// After
<CodeBlock code={manualKspConfiguration} language="kotlin" title="build.gradle.kts" />
```

---

## Task 4: Replace defineStackCode

**Dependency**: Task 1 complete

**Current State** (L64-81):
```tsx
// Define Navigation Stack
const defineStackCode = `import com.jermey.quo.vadis.annotations.*
import com.jermey.quo.vadis.core.navigation.NavDestination

@Stack(name = "home", startDestination = Feed::class)
sealed class HomeDestination : NavDestination {

    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    @Destination(route = "home/article/{articleId}")
    data class Article(
        @Argument val articleId: String,
        @Argument(optional = true) val showComments: Boolean = false
    ) : HomeDestination()

    @Destination(route = "home/settings")
    data object Settings : HomeDestination()
}`
```

**Target State**:
```tsx
// Imported: stackDestinationWithSettings from '@data/codeExamples'
// (DELETE the entire const declaration - lines 64-81)
```

**Usage Update** (L171):
```tsx
// Before
<CodeBlock code={defineStackCode} language="kotlin" title="HomeDestination.kt" />

// After
<CodeBlock code={stackDestinationWithSettings} language="kotlin" title="HomeDestination.kt" />
```

**Note**: The `stackDestinationWithSettings` example in codeExamples.ts includes the Settings destination, making it an exact match for GettingStarted.tsx's needs (vs. the basic version in Home.tsx which lacks Settings).

---

## Task 5: Replace bindScreensCode

**Dependency**: Task 1 complete

**Current State** (L84-108):
```tsx
// Bind Screens with @Screen
const bindScreensCode = `import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator

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
        if (destination.showComments) {
            Text("Comments visible")
        }
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}`
```

**Target State**:
```tsx
// Imported: screenBindingWithImports from '@data/codeExamples'
// (DELETE the entire const declaration - lines 84-108)
```

**Usage Update** (L181):
```tsx
// Before
<CodeBlock code={bindScreensCode} language="kotlin" title="Screens.kt" />

// After
<CodeBlock code={screenBindingWithImports} language="kotlin" title="Screens.kt" />
```

---

## Task 6: Replace setupNavHostCode

**Dependency**: Task 1 complete

**Current State** (L111-133):
```tsx
// Setup NavigationHost
const setupNavHostCode = `import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.compose.NavigationHost
import com.jermey.quo.vadis.core.navigation.navigator.TreeNavigator

@Composable
fun App() {
    // Generated config combines all registries
    val config = GeneratedNavigationConfig
    
    // Build initial navigation state
    val initialState = remember {
        config.buildNavNode(
            destinationClass = HomeDestination::class,
            parentKey = null
        )!!
    }
    
    // Create the navigator
    val navigator = remember {
        TreeNavigator(
            config = config,
            initialState = initialState
        )
    }
    
    // Render navigation
    NavigationHost(
        navigator = navigator,
        screenRegistry = config.screenRegistry
    )
}`
```

**Target State**:
```tsx
// Imported: navigationHostWithImports from '@data/codeExamples'
// (DELETE the entire const declaration - lines 111-133)
```

**Usage Update** (L189):
```tsx
// Before
<CodeBlock code={setupNavHostCode} language="kotlin" title="App.kt" />

// After
<CodeBlock code={navigationHostWithImports} language="kotlin" title="App.kt" />
```

---

## Task 7: Keep validationExampleError (No Change)

**Status**: KEEP AS-IS

**Current State** (L135-136):
```tsx
const validationExampleError = `Missing @Screen binding for 'HomeDestination.Feed' in file 'HomeDestination.kt' (line 12). 
Fix: Add a @Composable function annotated with @Screen(HomeDestination.Feed::class)`
```

**Rationale**: This error message is specific to the GettingStarted page's compile-time safety section. Not duplicated elsewhere. Keep inline.

---

## Task 8: Verify Path Alias Configuration

**Dependency**: Before Task 1

**Purpose**: Ensure `@data` path alias exists for clean imports.

**Location**: `docs/site/vite.config.ts`

**Check/Add**:
```typescript
resolve: {
  alias: {
    '@components': path.resolve(__dirname, './src/components'),
    '@data': path.resolve(__dirname, './src/data'),
    // ... other aliases
  }
}
```

**Also update**: `docs/site/tsconfig.app.json`
```json
{
  "compilerOptions": {
    "paths": {
      "@components/*": ["./src/components/*"],
      "@data/*": ["./src/data/*"]
    }
  }
}
```

**Fallback**: If alias configuration is out of scope, use relative imports:
```tsx
import { LIBRARY_VERSION } from '../../data/constants'
import { ... } from '../../data/codeExamples'
```

---

## Final Refactored File Structure

After all tasks complete, `GettingStarted.tsx` should have:

```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './GettingStarted.module.css'
import { LIBRARY_VERSION } from '@data/constants'
import {
  stackDestinationWithSettings,
  screenBindingWithImports,
  navigationHostWithImports,
  gradlePluginInstallation,
  manualKspConfiguration,
} from '@data/codeExamples'

// Validation error example - kept inline (not duplicated elsewhere)
const validationExampleError = `Missing @Screen binding for 'HomeDestination.Feed' in file 'HomeDestination.kt' (line 12). 
Fix: Add a @Composable function annotated with @Screen(HomeDestination.Feed::class)`

export default function GettingStarted() {
  return (
    <article className={styles.gettingStarted}>
      {/* ... rest of JSX unchanged, except CodeBlock code props updated */}
    </article>
  )
}
```

---

## Task Dependencies Diagram

```
Task 8: Verify Path Aliases
         │
         ▼
Task 1: Add Imports
         │
         ├─────┬─────┬─────┬─────┐
         ▼     ▼     ▼     ▼     ▼
Task 2  Task 3 Task 4 Task 5 Task 6
(gradle) (manual) (stack) (screen) (navhost)
         │
         ▼
Task 7: Keep validationExampleError (no-op)
```

**Execution Order**:
1. Task 8 (verify aliases) - prep work
2. Task 1 (add imports) - required first
3. Tasks 2-6 (parallel) - can be done in any order
4. Task 7 (verification only)

---

## Code Changes Summary

### Lines Removed: ~128 lines
- installationGradlePluginCode: 32 lines
- installationManualCode: 24 lines
- defineStackCode: 18 lines
- bindScreensCode: 25 lines
- setupNavHostCode: 23 lines

### Lines Added: ~10 lines
- Import statements: 8 lines
- (validationExampleError remains: 2 lines)

### Net Change: ~118 lines reduced

---

## Acceptance Criteria

- [ ] All imports resolve without errors
- [ ] No hardcoded "0.3.4" version strings remain
- [ ] No hardcoded "2.3.0" KSP version strings remain
- [ ] `npm run build` completes successfully
- [ ] Page renders correctly with all code blocks displayed
- [ ] Code examples display identical content to current state
- [ ] No TypeScript errors in IDE

---

## Verification Checklist

After implementation:

1. **Visual Verification**:
   - [ ] Navigate to `/getting-started`
   - [ ] Verify all 5 code blocks render correctly
   - [ ] Verify Installation section shows both options
   - [ ] Verify Compile-Time Safety section shows error message

2. **Build Verification**:
   ```bash
   cd docs/site
   npm run build
   npm run preview
   ```

3. **diff Check**:
   - The rendered code blocks should be byte-for-byte identical to current

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Path alias not configured | Use relative imports as fallback |
| codeExamples.ts has slightly different code | Compare before importing; adjust if needed |
| LIBRARY_VERSION not used directly | This page uses full code blocks (already includes version) |
| Breaking existing links | No URL changes - internal refactor only |

---

## Notes

- The `validationExampleError` is the only inline code example that should remain - it's specific to this page and not duplicated elsewhere
- LIBRARY_VERSION constant may not be directly used in this page since version numbers are embedded in the code examples from codeExamples.ts (which uses the constant internally)
- Consider adding `LIBRARY_VERSION` import for future-proofing even if not immediately used
