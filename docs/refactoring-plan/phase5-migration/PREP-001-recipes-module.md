# PREP-001: Create quo-vadis-recipes Module

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | PREP-001 |
| **Complexity** | Medium |
| **Estimated Time** | 1.5 days |
| **Dependencies** | None (can start immediately) |
| **Output** | `quo-vadis-recipes/` module skeleton |

## Objective

Create a new Kotlin Multiplatform module `quo-vadis-recipes` containing self-contained, pattern-based navigation examples. This module serves as:

1. **LLM Training Material** - Pattern-based packages optimized for agentic workflow integration
2. **Developer Reference** - Runnable examples demonstrating best practices
3. **Integration Guide** - Clear patterns for adopting Quo Vadis in new projects

> **Note**: This module is NOT published to Maven. It exists purely as documentation and example code.

## Module Structure

```
quo-vadis-recipes/
├── build.gradle.kts                      # KMP module configuration
├── README.md                             # Module documentation
└── src/
    └── commonMain/
        └── kotlin/
            └── com/jermey/quo/vadis/recipes/
                ├── package-info.kt       # Module-level KDoc
                ├── stack/                # MIG-001: Linear stack patterns
                │   ├── package-info.kt
                │   └── SettingsStackRecipe.kt
                ├── masterdetail/         # MIG-002: Master-detail patterns
                │   ├── package-info.kt
                │   └── ListDetailRecipe.kt
                ├── tabs/                 # MIG-003, MIG-005: Tab patterns
                │   ├── package-info.kt
                │   ├── BottomTabsRecipe.kt
                │   └── TabWithNestedStackRecipe.kt
                ├── wizard/               # MIG-004: Process flow patterns
                │   ├── package-info.kt
                │   ├── LinearWizardRecipe.kt
                │   └── BranchingWizardRecipe.kt
                ├── deeplink/             # MIG-006: Deep linking patterns
                │   ├── package-info.kt
                │   ├── BasicDeepLinkRecipe.kt
                │   └── NestedDeepLinkRecipe.kt
                ├── pane/                 # Adaptive layout patterns
                │   ├── package-info.kt
                │   └── AdaptivePaneRecipe.kt
                └── shared/               # Common utilities
                    ├── package-info.kt
                    └── RecipeScaffold.kt
```

## Deliverables

### 1. Module Configuration (`build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

kotlin {
    // Platform targets matching composeApp
    androidTarget()
    jvm("desktop")
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "QuoVadisRecipes"
            isStatic = true
        }
    }
    
    js(IR) {
        browser()
    }
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            // Quo Vadis navigation library
            implementation(project(":quo-vadis-core"))
            implementation(project(":quo-vadis-annotations"))
            
            // Compose dependencies
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// KSP configuration for code generation
dependencies {
    add("kspCommonMainMetadata", project(":quo-vadis-ksp"))
}
```

### 2. Settings Update (`settings.gradle.kts`)

Add to existing includes:

```kotlin
include(":quo-vadis-recipes")
```

### 3. Module README (`quo-vadis-recipes/README.md`)

```markdown
# Quo Vadis Recipes

Pattern-based navigation examples for the Quo Vadis navigation library.

## Purpose

This module provides self-contained, runnable examples demonstrating common 
navigation patterns. Each recipe is optimized for:

- **LLM Integration** - Clear patterns for agentic workflow assistance
- **Copy-Paste Ready** - Each recipe is self-contained
- **Best Practices** - Demonstrates recommended approaches

## Recipe Categories

### Stack Navigation (`stack/`)
Linear navigation with push/pop semantics.
- [SettingsStackRecipe](src/commonMain/kotlin/.../stack/SettingsStackRecipe.kt)

### Master-Detail (`masterdetail/`)
List-to-detail navigation with typed arguments.
- [ListDetailRecipe](src/commonMain/kotlin/.../masterdetail/ListDetailRecipe.kt)

### Tabbed Navigation (`tabs/`)
Bottom navigation with tab state preservation.
- [BottomTabsRecipe](src/commonMain/kotlin/.../tabs/BottomTabsRecipe.kt)
- [TabWithNestedStackRecipe](src/commonMain/kotlin/.../tabs/TabWithNestedStackRecipe.kt)

### Wizard/Process Flows (`wizard/`)
Multi-step flows with conditional branching.
- [LinearWizardRecipe](src/commonMain/kotlin/.../wizard/LinearWizardRecipe.kt)
- [BranchingWizardRecipe](src/commonMain/kotlin/.../wizard/BranchingWizardRecipe.kt)

### Deep Linking (`deeplink/`)
URI-based navigation and path reconstruction.
- [BasicDeepLinkRecipe](src/commonMain/kotlin/.../deeplink/BasicDeepLinkRecipe.kt)
- [NestedDeepLinkRecipe](src/commonMain/kotlin/.../deeplink/NestedDeepLinkRecipe.kt)

### Adaptive Layouts (`pane/`)
Responsive navigation for different screen sizes.
- [AdaptivePaneRecipe](src/commonMain/kotlin/.../pane/AdaptivePaneRecipe.kt)

## Usage

Each recipe file contains:
1. **KDoc Overview** - Pattern description and use cases
2. **Destination Definitions** - Annotated sealed classes
3. **Screen Composables** - @Screen annotated functions  
4. **Entry Point** - Ready-to-use App composable
5. **Usage Notes** - Integration tips

## For LLM Agents

When integrating Quo Vadis:
1. Identify the navigation pattern needed
2. Find the matching recipe package
3. Copy the recipe structure as a starting template
4. Customize destinations and screens for your use case
```

### 4. Root Package Info (`package-info.kt`)

```kotlin
/**
 * # Quo Vadis Recipes
 * 
 * Pattern-based navigation examples demonstrating the Quo Vadis navigation library.
 * 
 * ## Package Structure
 * 
 * - [com.jermey.quo.vadis.recipes.stack] - Linear stack navigation patterns
 * - [com.jermey.quo.vadis.recipes.masterdetail] - Master-detail list navigation
 * - [com.jermey.quo.vadis.recipes.tabs] - Tabbed navigation with state preservation
 * - [com.jermey.quo.vadis.recipes.wizard] - Multi-step process/wizard flows
 * - [com.jermey.quo.vadis.recipes.deeplink] - URI-based deep linking
 * - [com.jermey.quo.vadis.recipes.pane] - Adaptive multi-pane layouts
 * - [com.jermey.quo.vadis.recipes.shared] - Common utilities
 * 
 * ## LLM Integration Notes
 * 
 * Each recipe package is self-contained and follows these conventions:
 * 
 * 1. **Destination sealed class** - Defines navigation targets with `@Stack`, `@Tab`, or `@Pane`
 * 2. **Screen composables** - Functions annotated with `@Screen` for each destination
 * 3. **Entry composable** - A `*RecipeApp()` function demonstrating the full pattern
 * 
 * To integrate Quo Vadis into a new project:
 * 1. Identify the navigation pattern needed (stack, tabs, master-detail, etc.)
 * 2. Copy the corresponding recipe as a template
 * 3. Customize destinations and screens for your use case
 * 4. Ensure KSP processor is configured to generate navigation code
 * 
 * @see com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
 * @see com.jermey.quo.vadis.annotations.Stack
 * @see com.jermey.quo.vadis.annotations.Tab
 * @see com.jermey.quo.vadis.annotations.Destination
 * @see com.jermey.quo.vadis.annotations.Screen
 */
package com.jermey.quo.vadis.recipes
```

### 5. Shared Utilities (`shared/RecipeScaffold.kt`)

```kotlin
package com.jermey.quo.vadis.recipes.shared

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared scaffold component for recipe examples.
 * 
 * Provides consistent UI structure across all recipes while keeping
 * the navigation logic in focus.
 * 
 * ## Usage
 * 
 * ```kotlin
 * @Screen(MyDestination.Home::class)
 * @Composable
 * fun HomeScreen(navigator: Navigator) {
 *     RecipeScaffold(title = "Home") {
 *         // Screen content
 *     }
 * }
 * ```
 * 
 * @param title Screen title displayed in top app bar
 * @param showBackButton Whether to show back navigation button
 * @param onBackClick Callback when back button is clicked
 * @param bottomBar Optional bottom bar content
 * @param content Main screen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScaffold(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBackClick) {
                            // Back arrow icon
                            Text("←")
                        }
                    }
                }
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        content(padding)
    }
}

/**
 * Navigation button for recipe examples.
 * 
 * Standard button style for triggering navigation actions.
 * 
 * @param text Button label
 * @param onClick Navigation action
 * @param modifier Optional modifier
 */
@Composable
fun NavigationButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}
```

## Implementation Steps

### Step 1: Create Module Directory Structure

```bash
mkdir -p quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/{stack,masterdetail,tabs,wizard,deeplink,pane,shared}
```

### Step 2: Create build.gradle.kts

Create the module build configuration as specified above.

### Step 3: Update settings.gradle.kts

Add `include(":quo-vadis-recipes")` to the project settings.

### Step 4: Create Package Info Files

Create `package-info.kt` in each package with appropriate KDoc:

- Root package: Overview and LLM integration notes
- Each recipe package: Pattern description and usage

### Step 5: Create Shared Utilities

Implement `RecipeScaffold.kt` and any other shared components.

### Step 6: Verify Build

```bash
./gradlew :quo-vadis-recipes:compileKotlinMetadata
```

## LLM Optimization Guidelines

### Package-Level KDoc

Each package should have KDoc that:
- Describes the pattern in 1-2 sentences
- Lists when to use this pattern
- Links to related patterns
- Provides a minimal code example

### Recipe File Structure

```kotlin
/**
 * # [Recipe Name]
 * 
 * [One-line description]
 * 
 * ## When to Use
 * 
 * - [Use case 1]
 * - [Use case 2]
 * 
 * ## Pattern Overview
 * 
 * [Brief explanation of the pattern]
 * 
 * ## Key Components
 * 
 * - [Destination]: Navigation targets
 * - [Screen]: Screen composables
 * - [RecipeApp]: Full example entry point
 * 
 * ## Integration Steps
 * 
 * 1. Copy destination sealed class
 * 2. Create @Screen composables for each destination
 * 3. Use QuoVadisHost in your app entry point
 * 
 * @see [Related pattern]
 */
```

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Recipe file | `{Pattern}Recipe.kt` | `SettingsStackRecipe.kt` |
| Destination class | `{Recipe}Destination` | `SettingsDestination` |
| Screen function | `{Destination}Screen` | `SettingsMainScreen` |
| App entry | `{Recipe}App` | `SettingsStackApp` |

## Acceptance Criteria

- [ ] Module compiles without errors
- [ ] All platform targets configured
- [ ] Package structure matches specification
- [ ] Root README.md created
- [ ] Package-info.kt files in all packages
- [ ] RecipeScaffold utility implemented
- [ ] KDoc optimized for LLM consumption
- [ ] Verified with `./gradlew :quo-vadis-recipes:compileKotlinMetadata`

## Related Tasks

- [MIG-001](./MIG-001-simple-stack-example.md) - Populates `stack/` package
- [MIG-002](./MIG-002-master-detail-example.md) - Populates `masterdetail/` package
- [MIG-003](./MIG-003-tabbed-navigation-example.md) - Populates `tabs/` package
- [MIG-004](./MIG-004-process-flow-example.md) - Populates `wizard/` package
- [MIG-005](./MIG-005-nested-tabs-detail-example.md) - Populates `tabs/` package
- [MIG-006](./MIG-006-deep-linking-recipe.md) - Populates `deeplink/` package
