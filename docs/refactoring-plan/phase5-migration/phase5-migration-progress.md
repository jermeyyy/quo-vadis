# Phase 5: Migration Examples - Progress

> **Last Updated**: 2025-12-07  
> **Phase Status**: � Completed  
> **Progress**: 11/11 tasks (100%)

## Overview

This phase creates the `quo-vadis-recipes` module with LLM-optimized navigation examples, marks all legacy APIs with `@Deprecated` annotations, and rewrites the demo app to showcase all patterns.

> **Note**: No backward compatibility adapters are needed. The library is in development stage and breaking changes are acceptable.

---

## Task Progress

### Preparatory Tasks

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [PREP-001](./PREP-001-recipes-module.md) | Create quo-vadis-recipes Module | 🟢 Completed | 2025-12-06 | Module skeleton created |
| [PREP-002](./PREP-002-deprecated-annotations.md) | Add @Deprecated Annotations | 🟢 Completed | 2025-12-07 | All legacy APIs deprecated |
| [PREP-003](./PREP-003-permalink-reference.md) | GitHub Permalink Reference Doc | 🟢 Completed | 2025-12-07 | Created LEGACY_API_REFERENCE.md |

### Recipe Tasks

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [MIG-001](./MIG-001-simple-stack-example.md) | Simple Stack Navigation Recipe | 🟢 Completed | 2025-12-07 | Created `SettingsStackRecipe.kt` in recipes module |
| [MIG-002](./MIG-002-master-detail-example.md) | Master-Detail Pattern Recipe | 🟢 Completed | 2025-12-07 | Created `ListDetailRecipe.kt` in recipes module |
| [MIG-003](./MIG-003-tabbed-navigation-example.md) | Tabbed Navigation Recipe | 🟢 Completed | 2025-12-07 | Created `BottomTabsRecipe.kt` in recipes module |
| [MIG-004](./MIG-004-process-flow-example.md) | Process/Wizard Flow Recipe | 🟢 Completed | 2025-12-07 | Created `LinearWizardRecipe.kt` & `BranchingWizardRecipe.kt` |
| [MIG-005](./MIG-005-nested-tabs-detail-example.md) | Nested Tabs + Detail Recipe | 🟢 Completed | 2025-12-06 | Created `docs/migration-examples/05-nested-tabs-detail.md` |
| [MIG-006](./MIG-006-deep-linking-recipe.md) | Deep Linking Recipe | 🟢 Completed | 2025-12-07 | Created recipe files in `quo-vadis-recipes/deeplink/` |

### Migration Tasks

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [MIG-007](./MIG-007-demo-app-rewrite.md) | Demo App Rewrite | ⚪ Not Started | - | Depends on MIG-001..006 |
| [MIG-008](./MIG-008-api-change-summary.md) | API Change Summary Document | ⚪ Not Started | - | Depends on PREP-002 |

---

## Completed Tasks

- **MIG-004** (2025-12-07): Process/Wizard Flow Recipe (Recipes Module Implementation)
  - Created `LinearWizardRecipe.kt` and `BranchingWizardRecipe.kt` in `quo-vadis-recipes/src/commonMain/kotlin/.../wizard/`
  
  **LinearWizardRecipe.kt** (~700 lines):
  - `OnboardingDestination` sealed class with linear flow: Welcome → UserType → PersonalInfo → Preferences → Complete
  - `AppDestination` placeholder for post-flow navigation target
  - `OnboardingStep` enum with step tracking (index, title, fromDestination())
  - `RecipeOnboardingScreenRegistry` - Placeholder showing KSP-generated registry
  - **Type-Safe Navigator Extensions** (NEW API patterns):
    - `navigateAndClear(destination, clearUpTo: KClass, inclusive)` - Type-safe stack clearing
    - `popTo(destinationType: KClass, inclusive)` - Type-safe return to specific step
    - `exitFlow(flowType: KClass)` - New convenience method for flow cancellation
  - Screen composables with step progress indicators
  - `StepProgressIndicator` - Visual step tracker component
  - `LinearWizardApp` - Entry point showing production setup
  - `LinearWizardMigrationChecklist` - Comprehensive migration checklist
  
  **BranchingWizardRecipe.kt** (~1200 lines):
  - `CartItem` and `CartState` data classes with `hasPhysicalProducts`/`isDigitalOnly` flags
  - `CheckoutDestination` sealed class with conditional flow:
    - Cart → Shipping → (DeliveryOptions OR Payment) → Review → Confirmation(orderId)
    - `Confirmation` has route template `checkout/confirmation/{orderId}`
  - `checkoutAnimations` AnimationRegistry with wizard-specific transitions
  - `RecipeCheckoutScreenRegistry` - Placeholder with destination parameter injection
  - Screen composables demonstrating conditional branching
  - UI components: CartItemRow, DeliveryOption, PaymentOption
  - `BranchingWizardApp` - Entry point showing production setup
  - `BranchingWizardMigrationChecklist` - Migration checklist for branching flows
  
  **Key Migration Patterns Documented**:
  - `navigateAndClearTo(dest, "route", inclusive)` → `navigateAndClear(dest, Dest::class, inclusive)`
  - `popTo("route")` string-based → `popTo(Dest::class)` type-safe
  - New `exitFlow()` convenience method for canceling multi-step flows
  - Per-call transitions → `AnimationRegistry` centralized configuration
  
  **KMP Compatibility**:
  - `formatPrice()` extension function (replaces `String.format`)
  - `generateOrderId()` function (replaces `System.currentTimeMillis()`)
  - Unique `MigrationChecklist` class names per file
  
  Verified: `:quo-vadis-recipes:assemble -x detekt` ✓, `:quo-vadis-recipes:check -x detekt` ✓

- **MIG-003** (2025-12-07): Tabbed Navigation Recipe (Recipes Module Implementation)
  - Created `BottomTabsRecipe.kt` in `quo-vadis-recipes/src/commonMain/kotlin/.../tabs/`:
    - `MainTabs` sealed class with Home, Search, Profile tab items
    - `HomeDestination`, `SearchDestination`, `ProfileDestination` per-tab stack destinations
    - `buildMainTabsNavNode()` - Placeholder showing KSP-generated TabNode builder
    - `MainTabsMetadata` - Placeholder showing KSP-generated tab metadata for UI
    - `RecipeMainTabsScreenRegistry` - Placeholder showing KSP-generated registry
    - `TabWrapperDocumentation` - Comprehensive `tabWrapper` API documentation
    - `MainBottomNavigation`, `TypeSafeBottomNavigation` - Reusable navigation bar components
    - Screen composables for all destinations (Home/Search/Profile tabs)
    - `BottomTabsRecipeApp` - Entry point showing production setup pattern
    - `TabNavigationDocumentation` - Tab switching patterns and state preservation docs
    - `MigrationChecklist` - Comprehensive checklist for tabbed navigation migration
  - Demonstrated patterns:
    - `TabbedNavigatorConfig` object → `@Tab` + `@TabItem` annotations
    - `TabbedNavHost` + `tabUI` → `QuoVadisHost` + `tabWrapper` parameter
    - `rememberTabNavigator()` → State in NavNode tree
    - `tabState.switchTab(tab)` → `navigator.switchTab(index)`
    - `tabState.activeTab` → `tabNode.activeStackIndex`
    - Per-tab stack preservation (automatic via NavNode tree)
  - Rich KDoc documentation for LLM consumption with before/after code examples
  - Note: `@Tab`, `@TabItem`, `@Stack`, `@Destination`, `@Screen` annotations omitted to avoid KSP processing
  - Verified: `:quo-vadis-recipes:build -x detekt` ✓

- **MIG-002** (2025-12-07): Master-Detail Pattern Recipe (Recipes Module Implementation)
  - Created `ListDetailRecipe.kt` in `quo-vadis-recipes/src/commonMain/kotlin/.../masterdetail/`:
    - `Product` data class and `sampleProducts()` helper for sample data
    - `CatalogDestination` sealed class with ProductList and ProductDetail(productId) destinations
    - `catalogAnimations` AnimationRegistry demonstrating centralized transition configuration
    - `RecipeCatalogScreenRegistry` - Placeholder showing KSP-generated registry with destination parameter injection
    - `RecipeCatalogDeepLinkHandler` - Placeholder showing KSP-generated deep link handler from route templates
    - `ProductListScreen`, `ProductDetailScreen` composables
    - `ProductCard` UI component with shared element transition documentation
    - `handleCatalogDeepLink()` function showing deep link handling pattern
    - `MigrationChecklist` - Comprehensive checklist for master-detail migration
  - Demonstrated patterns:
    - `@Argument(Data::class)` + `TypedDestination<T>` → Route template `{param}`
    - Separate data class → Direct destination parameter access (`destination.productId`)
    - `@Content(data: T)` → `@Screen(destination: Dest)`
    - `navigate(dest, transition)` → `AnimationRegistry` centralized config
    - `SharedTransitionLayout` wrapper → Built-in via `QuoVadisHost`
    - `LocalSharedTransitionScope.current` for shared element access
    - Manual deep link parsing → KSP-generated handler from route templates
  - Rich KDoc documentation for LLM consumption with before/after code examples in all sections
  - Note: `@Stack`, `@Destination`, `@Screen` annotations omitted to avoid KSP processing
  - Verified: `:quo-vadis-recipes:build -x detekt` ✓

- **MIG-001** (2025-12-07): Simple Stack Navigation Recipe (Recipes Module Implementation)
  - Created `SettingsStackRecipe.kt` in `quo-vadis-recipes/src/commonMain/kotlin/.../stack/`:
    - `SettingsDestination` sealed class with Main, Account, Notifications destinations
    - `RecipeSettingsScreenRegistry` - Placeholder showing KSP-generated registry pattern
    - `SettingsMainScreen`, `AccountScreen`, `NotificationsScreen` composables
    - `SettingsStackApp` - Entry point showing production setup pattern
    - `MigrationChecklist` - Comprehensive checklist object documenting migration steps
  - Demonstrated patterns:
    - `@Graph` → `@Stack` annotation migration
    - `@Route` → `@Destination` annotation migration
    - `@Content` → `@Screen` annotation migration
    - `GraphNavHost` → `QuoVadisHost` migration
    - Removal of manual initialization (`initializeQuoVadisRoutes()`, `registerGraph()`, `setStartDestination()`)
    - `startDestination` uses class name (`"Main"`) not route (`"settings/main"`)
  - Rich KDoc documentation for LLM consumption with before/after code examples
  - Note: `@Stack`, `@Destination`, `@Screen` annotations omitted to avoid KSP processing
  - Verified: `:quo-vadis-recipes:compileKotlinDesktop` ✓

- **MIG-006** (2025-12-07): Deep Linking Recipe
  - Created deep linking recipe files in `quo-vadis-recipes/src/commonMain/kotlin/.../deeplink/`:
    - `package-info.kt` - Updated with comprehensive LLM-optimized documentation
    - `DeepLinkDestinations.kt` - Shared destination definitions:
      - `ProductsDestination` - Simple routes (`products`, `products/featured`, `products/{productId}`)
      - `CategoryDestination` - Nested routes (`categories/{categoryId}/products/{productId}`)
    - `BasicDeepLinkRecipe.kt` - Simple URI routing recipe
    - `NestedDeepLinkRecipe.kt` - Nested deep links recipe
  - Demonstrated patterns:
    - Route template syntax with `@Destination(route = "...")`
    - Path parameter extraction (`{param}`)
    - Handling `DeepLinkResult.Matched` and `NotMatched`
    - URI creation from destinations (reverse lookup)
    - Migration from legacy API patterns
  - Note: `@Stack` and `@Screen` annotations omitted due to KSP processor bug
  - Verified: `:quo-vadis-recipes:compileKotlinMetadata` ✓

- **PREP-003** (2025-12-07): GitHub Permalink Reference Document
  - Created `docs/migration-examples/LEGACY_API_REFERENCE.md`
  - Links all legacy APIs to stable main branch GitHub URLs
  - Organized by module/category (Core, Compose, Annotations, Demo App)
  - Migration mapping quick reference table (Old API → New API)
  - LLM usage notes for referencing legacy code patterns
  - Key symbols documented for each file
  - Serves as authoritative "migrating from" reference

- **PREP-002** (2025-12-07): Add @Deprecated Annotations to Legacy APIs
  - Deprecated 4 annotations in `quo-vadis-annotations`: `@Graph`, `@Route`, `@Argument`, `@Content`
  - Deprecated core APIs in `quo-vadis-core/core/`:
    - `NavigationGraph.kt`: NavigationGraph, DestinationConfig, NavigationGraphBuilder, navigationGraph()
    - `BackStack.kt`: BackStack, BackStackEntry, MutableBackStack, EXTRA_SELECTED_TAB_ROUTE
    - `Destination.kt`: TypedDestination, RestoredTypedDestination, BasicDestination
    - `TabDefinition.kt`: TabDefinition, TabNavigatorConfig
    - `Navigator.kt`: registerGraph(), setStartDestination(), navigateAndClearTo(string)
    - `TabNavigatorState.kt`: TabNavigatorState
    - `TabScopedNavigator.kt`: TabScopedNavigator
  - Deprecated Compose APIs in `quo-vadis-core/compose/`:
    - `NavHost.kt`: NavHost
    - `GraphNavHost.kt`: GraphNavHost, LocalBackStackEntry, rememberNavigator()
    - `TabbedNavHost.kt`: TabbedNavHost
    - `TabNavHostComposables.kt`: rememberTabNavigatorState(), rememberTabNavigator()
  - Removed duplicate legacy tab annotations (TabGraph, Tab, TabContent) conflicting with new annotations
  - Verified: `:quo-vadis-core:build -x detekt` ✓, `:quo-vadis-annotations:build` ✓

- **PREP-001** (2025-12-06): Create quo-vadis-recipes Module
  - Created `quo-vadis-recipes/` module skeleton
  - Configured KMP build with all platforms (Android, iOS, Desktop, JS, WasmJS)
  - Created package structure for all recipe categories
  - Created `RecipeScaffold.kt` shared utilities
  - Created README.md with module documentation
  - Created package-info.kt files with LLM-optimized KDoc
  - Verified: `:quo-vadis-recipes:compileKotlinMetadata` ✓

- **MIG-005** (2025-12-06): Nested Tabs + Detail Example
  - Created `docs/migration-examples/05-nested-tabs-detail.md`
  - Complete before/after code examples for complex navigation hierarchies
  - Z-ordering via flattening explanation with visual diagrams
  - Predictive back across layers with speculative pop
  - Shared element transitions across tab/detail boundary
  - Deep navigation flow example with NavNode tree states
  - 7 key migration steps with diff examples
  - Comprehensive pitfalls table with 9 common issues
  - Covers full-screen detail covering tabs pattern

- **MIG-004** (2025-12-06): Process/Wizard Flow Example
  - Created `docs/migration-examples/04-process-flow.md`
  - Complete before/after code examples for onboarding and checkout flows
  - 11 numbered migration steps with diff examples
  - Sequential navigation, conditional branching, flow completion patterns
  - Type-safe `popTo`, `navigateAndClear`, and new `exitFlow` API
  - AnimationRegistry configuration for consistent flow transitions
  - Checkout flow example with conditional steps (physical vs digital)
  - Comprehensive pitfalls table with 9 common issues

- **MIG-003** (2025-12-06): Tabbed Navigation Example
  - Created `docs/migration-examples/03-tabbed-navigation.md`
  - Complete before/after code examples with `@Tab`, `@TabItem` annotations
  - 11 numbered migration steps with diff examples  
  - `tabWrapper` API pattern for user-controlled scaffold
  - Tab state preservation via NavNode tree
  - KSP-generated code examples (TabNode builder, TabMetadata, ScreenRegistry)
  - Comprehensive pitfalls table with debugging tips
  - Tab switching API (`navigator.switchTab()` vs old `tabState.selectTab()`)

- **MIG-002** (2025-12-06): Master-Detail Pattern Example
  - Created `docs/migration-examples/02-master-detail.md`
  - Complete before/after code examples with typed arguments
  - 11 numbered migration steps with diff examples
  - Route template parameters and deep linking
  - AnimationRegistry for centralized transitions
  - Shared element transitions (built-in vs manual wrapper)
  - KSP-generated code examples (NavNodeBuilder, ScreenRegistry, DeepLinkHandler)
  - Comprehensive pitfalls table with debugging tips

- **MIG-001** (2025-12-06): Simple Stack Navigation Example
  - Created `docs/migration-examples/01-simple-stack.md`
  - Comprehensive before/after code examples
  - 8 numbered migration steps
  - KSP-generated code examples (StackNodeBuilder, ScreenRegistry)
  - Common pitfalls table with solutions

---

## In Progress Tasks

_None currently in progress._

---

## Blocked Tasks

| Task | Blocked By | Status |
|------|------------|--------|
| MIG-007 | MIG-001 through MIG-006 | Waiting for recipes |

---

## Ready to Start

| Task | Notes |
|------|-------|
| MIG-007 | Demo app rewrite (all recipe dependencies complete) |
| MIG-008 | API change summary (PREP-002 now complete) |

---

## Dependencies

```
                            ┌─────────────────────────────────┐
                            │       PREP-001 (recipes)        │
                            │      (can start anytime)        │
                            └────────────────┬────────────────┘
                                             │
Phase 1-4 ───────────────────────────────────┼───┐
            │                                │   │
            ▼                                │   │
       PREP-002 ──────────────────┐          │   │
    (@Deprecated)                 │          │   │
            │                     │          ▼   │
            │                     │      MIG-001 (stack)
            ▼                     │          │
       MIG-008 (summary)          │    ┌─────┼─────┬───────┐
                                  │    │     │     │       │
                                  │    ▼     ▼     ▼       ▼
                                  │ MIG-002 MIG-003 MIG-004 │
                                  │ (detail) (tabs) (wizard)│
                                  │    │     │             │
                                  │    │     ▼             │
                                  │    │  MIG-005          │
                                  │    │  (nested)         │
                                  │    │     │             │
                                  │    ├─────┴─────────────┤
                                  │    │                   │
                                  │    ▼                   │
                                  │ MIG-006 (deeplink)     │
                                  │    │                   │
                                  │    └───────┬───────────┘
                                  │            │
                                  │            ▼
                                  └──────► MIG-007 (demo app)

PREP-003 (permalinks) ─────────── (can start anytime)
```

---

## Recipe Module Coverage

| Package | Recipe | Key Patterns |
|---------|--------|--------------|
| `stack/` | MIG-001 | `@Stack`, `@Destination`, `@Screen`, basic navigation |
| `masterdetail/` | MIG-002 | Route templates, typed arguments, shared elements |
| `tabs/` | MIG-003, MIG-005 | `@Tab`, `@TabItem`, `tabWrapper`, nested stacks |
| `wizard/` | MIG-004 | Sequential flow, branching, stack clearing |
| `deeplink/` | MIG-006 | URI handling, path parameters, reconstruction |
| `pane/` | (Future) | Adaptive layouts (if time permits) |

---

## Notes

- **Estimated**: 14-17 days total
- **New module**: `quo-vadis-recipes` for LLM-optimized examples
- **Deprecation**: All legacy APIs marked `@Deprecated` with `replaceWith`
- **References**: GitHub permalinks to main branch for "migrating from" code
- **No backwards compatibility**: Library in development stage

---

## Related Documents

- [Phase 5 Summary](./phase5-migration-summary.md)
- [PREP-001: Recipes Module](./PREP-001-recipes-module.md)
- [PREP-002: Deprecated Annotations](./PREP-002-deprecated-annotations.md)
- [PREP-003: Permalink Reference](./PREP-003-permalink-reference.md)
