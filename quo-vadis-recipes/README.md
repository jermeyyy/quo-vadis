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
- [SettingsStackRecipe](src/commonMain/kotlin/com/jermey/quo/vadis/recipes/stack/SettingsStackRecipe.kt)

### Master-Detail (`masterdetail/`)
List-to-detail navigation with typed arguments.
- [ListDetailRecipe](src/commonMain/kotlin/com/jermey/quo/vadis/recipes/masterdetail/ListDetailRecipe.kt)

### Tabbed Navigation (`tabs/`)
Bottom navigation with tab state preservation.
- [BottomTabsRecipe](src/commonMain/kotlin/com/jermey/quo/vadis/recipes/tabs/BottomTabsRecipe.kt)
- [TabWithNestedStackRecipe](src/commonMain/kotlin/com/jermey/quo/vadis/recipes/tabs/TabWithNestedStackRecipe.kt)

### Wizard/Process Flows (`wizard/`)
Multi-step flows with conditional branching.
- [LinearWizardRecipe](src/commonMain/kotlin/com/jermey/quo/vadis/recipes/wizard/LinearWizardRecipe.kt)
- [BranchingWizardRecipe](src/commonMain/kotlin/com/jermey/quo/vadis/recipes/wizard/BranchingWizardRecipe.kt)

### Deep Linking (`deeplink/`)
URI-based navigation and path reconstruction.
- [BasicDeepLinkRecipe](src/commonMain/kotlin/com/jermey/quo/vadis/recipes/deeplink/BasicDeepLinkRecipe.kt)
- [NestedDeepLinkRecipe](src/commonMain/kotlin/com/jermey/quo/vadis/recipes/deeplink/NestedDeepLinkRecipe.kt)

### Adaptive Layouts (`pane/`)
Responsive navigation for different screen sizes.
- [AdaptivePaneRecipe](src/commonMain/kotlin/com/jermey/quo/vadis/recipes/pane/AdaptivePaneRecipe.kt)

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
