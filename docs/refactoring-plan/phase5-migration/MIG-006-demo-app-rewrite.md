# MIG-006: Demo App Rewrite

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-006 |
| **Complexity** | High |
| **Estimated Time** | 3-4 days |
| **Dependencies** | Phase 1-4 complete, MIG-001 through MIG-005 |
| **Output** | Updated `composeApp/` module |

## Objective

Rewrite the demo application (`composeApp`) to showcase the new NavNode architecture, serving as both a reference implementation and an integration test.

## Scope

### Files to Modify

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/
├── App.kt                          # Main entry point
├── demo/
│   ├── DemoApp.kt                  # App setup with QuoVadisHost
│   ├── destinations/               # All destination definitions
│   │   ├── AppDestination.kt       # Root app destinations
│   │   ├── MainTabsDestination.kt  # @Tab annotated
│   │   ├── HomeDestination.kt      # @Stack for Home tab
│   │   ├── SearchDestination.kt    # @Stack for Search tab
│   │   ├── ProfileDestination.kt   # @Stack for Profile tab
│   │   ├── SettingsDestination.kt  # @Stack for settings flow
│   │   └── OnboardingDestination.kt # @Stack for onboarding wizard
│   ├── screens/                    # @Screen annotated composables
│   │   ├── home/
│   │   ├── search/
│   │   ├── profile/
│   │   ├── settings/
│   │   └── onboarding/
│   └── ui/
│       ├── components/             # Shared UI components
│       └── theme/                  # Theme configuration
```

## Rewrite Checklist

### Phase A: Destination Definitions (Day 1)

#### A.1. Update Root Destinations

```kotlin
// OLD: composeApp/.../destinations/AppDestination.kt
@Graph("app", startDestination = "MainTabs")
sealed class AppDestination : Destination {
    @Route("app/main_tabs")
    data object MainTabs : AppDestination()
    
    @Route("app/onboarding")
    data object Onboarding : AppDestination()
}

// NEW: composeApp/.../destinations/AppDestination.kt
@Stack(name = "app", startDestination = "MainTabs")
sealed class AppDestination : Destination {
    @Destination(route = "app/main_tabs")
    data object MainTabs : AppDestination()
    
    @Destination(route = "app/onboarding")
    data object Onboarding : AppDestination()
}
```

#### A.2. Convert Tab Configuration to Annotations

```kotlin
// OLD: TabbedNavigatorConfig object + separate graphs
object MainTabsConfig : TabbedNavigatorConfig<MainTab> { ... }

// NEW: @Tab annotated sealed class
@Tab(name = "mainTabs", initialTab = "Home")
sealed class MainTabsDestination : Destination {
    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tabs/home")
    data object Home : MainTabsDestination()
    
    @TabItem(label = "Search", icon = "search", rootGraph = SearchDestination::class)
    @Destination(route = "tabs/search")
    data object Search : MainTabsDestination()
    
    @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    @Destination(route = "tabs/profile")
    data object Profile : MainTabsDestination()
}
```

#### A.3. Convert Each Tab's Graph

```kotlin
// OLD
@Graph("home", startDestination = "feed")
sealed class HomeDestination : Destination {
    @Route("home/feed")
    data object Feed : HomeDestination()
    
    @Route("home/article")
    @Argument(ArticleData::class)
    data class Article(val id: String) : HomeDestination(), TypedDestination<ArticleData>
}

// NEW
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
    
    @Destination(route = "home/article/{articleId}")
    data class Article(val articleId: String) : HomeDestination()
}
```

#### A.4. Task Checklist - Destinations

- [ ] Convert `AppDestination` from `@Graph` to `@Stack`
- [ ] Create `MainTabsDestination` with `@Tab` annotation
- [ ] Convert `HomeDestination` from `@Graph` to `@Stack`
- [ ] Convert `SearchDestination` from `@Graph` to `@Stack`
- [ ] Convert `ProfileDestination` from `@Graph` to `@Stack`
- [ ] Convert `SettingsDestination` from `@Graph` to `@Stack`
- [ ] Convert `OnboardingDestination` from `@Graph` to `@Stack`
- [ ] Remove all `@Argument` annotations
- [ ] Remove all `TypedDestination<T>` implementations
- [ ] Add route templates with `{param}` placeholders
- [ ] Verify all route strings are unique

---

### Phase B: Screen Bindings (Day 1-2)

#### B.1. Rename @Content to @Screen

```kotlin
// OLD
@Content(HomeDestination.Feed::class)
@Composable
fun FeedContent(navigator: Navigator) { ... }

// NEW
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) { ... }
```

#### B.2. Update Screens with Arguments

```kotlin
// OLD
@Content(HomeDestination.Article::class)
@Composable
fun ArticleContent(
    data: ArticleData,      // Separate data class
    navigator: Navigator
) {
    Text("Article: ${data.articleId}")
}

// NEW
@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(
    destination: HomeDestination.Article,  // Destination instance
    navigator: Navigator
) {
    Text("Article: ${destination.articleId}")
}
```

#### B.3. Task Checklist - Screens

- [ ] Rename all `@Content` to `@Screen`
- [ ] Rename all `*Content` functions to `*Screen`
- [ ] Update parameter types from `data: T` to `destination: Dest`
- [ ] Update all references from `data.property` to `destination.property`
- [ ] Remove unused data class definitions
- [ ] Verify each destination has exactly one `@Screen`

---

### Phase C: App Entry Point (Day 2)

#### C.1. Update DemoApp.kt

```kotlin
// OLD
@Composable
fun DemoApp() {
    remember { initializeQuoVadisRoutes() }
    
    val navigator = rememberNavigator()
    val appGraph = remember { appRootGraph() }
    
    LaunchedEffect(navigator, appGraph) {
        navigator.registerGraph(appGraph)
        navigator.setStartDestination(AppDestination.MainTabs)
    }
    
    GraphNavHost(
        graph = appGraph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal,
        enablePredictiveBack = true
    )
}

// NEW
@Composable
fun DemoApp() {
    val navTree = remember { buildMainTabsNavNode() }
    val navigator = rememberNavigator(navTree)
    
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        animationRegistry = demoAnimations,
        tabWrapper = { tabNode, tabContent ->
            DemoTabWrapper(
                tabNode = tabNode,
                onTabSelected = { navigator.switchTab(it) },
                content = tabContent
            )
        }
    )
}

@Composable
private fun DemoTabWrapper(
    tabNode: TabNode,
    onTabSelected: (MainTabsDestination) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = { DemoTopAppBar() },
        bottomBar = {
            DemoBottomNavigation(
                activeTabIndex = tabNode.activeStackIndex,
                onTabSelected = onTabSelected
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
```

#### C.2. Animation Registry Configuration

```kotlin
// NEW: demoAnimations.kt
val demoAnimations = AnimationRegistry {
    // Tab to tab: crossfade
    withinGraph(MainTabsDestination::class) {
        default uses CrossFade
    }
    
    // Within home tab: slide horizontal
    withinGraph(HomeDestination::class) {
        forward uses SlideHorizontal
        backward uses SlideHorizontalReverse
    }
    
    // Full-screen transitions
    from(HomeDestination.Feed::class)
        .to(FullscreenDestination.ProductDetail::class)
        .uses(SharedElementTransition)
    
    // Onboarding: slide up
    from(AppDestination.MainTabs::class)
        .to(AppDestination.Onboarding::class)
        .uses(SlideVertical)
    
    // Default
    default(FadeThrough)
}
```

#### C.3. Task Checklist - App Entry

- [ ] Remove `initializeQuoVadisRoutes()` call
- [ ] Remove `rememberNavigator()` without args
- [ ] Use `rememberNavigator(navTree)` with KSP-generated tree
- [ ] Replace `GraphNavHost` with `QuoVadisHost`
- [ ] Add `screenRegistry` parameter
- [ ] Create `tabWrapper` composable
- [ ] Create `AnimationRegistry` configuration
- [ ] Remove `LaunchedEffect` setup code
- [ ] Remove manual graph registration

---

### Phase D: Tab Navigation (Day 2-3)

#### D.1. Remove TabbedNavHost Usage

```kotlin
// OLD: MainTabsContent.kt
@Content(AppDestination.MainTabs::class)
@Composable
fun MainTabsContent(parentNavigator: Navigator, parentEntry: BackStackEntry) {
    val tabState = rememberTabNavigator(
        config = MainTabsConfig,
        parentNavigator = parentNavigator,
        parentEntry = parentEntry
    )
    
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = MainTabsConfig.allTabs.associateWith { tabGraph },
        tabUI = { content -> /* scaffold */ }
    )
}

// NEW: MainTabsContent.kt - DELETED!
// Tab wrapper is now in DemoApp.kt via tabWrapper parameter
// No separate @Screen needed for the tab container itself
```

#### D.2. Update Tab Switching

```kotlin
// OLD
tabState.switchTab(MainTab.Profile)

// NEW
navigator.switchTab(MainTabsDestination.Profile)
```

#### D.3. Task Checklist - Tabs

- [ ] Delete `MainTabsContent.kt` (tab container screen)
- [ ] Delete `MainTabsConfig` object
- [ ] Remove all `rememberTabNavigator()` calls
- [ ] Remove all `TabbedNavHost` composables
- [ ] Update tab switching to `navigator.switchTab()`
- [ ] Move scaffold/bottom nav to `tabWrapper`
- [ ] Verify tab state preservation works

---

### Phase E: Navigation Calls (Day 3)

#### E.1. Update Navigate Calls

```kotlin
// OLD
navigator.navigate(
    destination = HomeDestination.Article("123"),
    transition = NavigationTransitions.SlideHorizontal
)

// NEW
navigator.navigate(HomeDestination.Article("123"))
// Transition from AnimationRegistry
```

#### E.2. Update PopTo/Clear Calls

```kotlin
// OLD
navigator.navigateAndClearTo(
    destination = AppDestination.MainTabs,
    upToRoute = "onboarding",
    inclusive = true
)

// NEW
navigator.navigateAndClear(
    destination = AppDestination.MainTabs,
    clearUpTo = OnboardingDestination::class,
    inclusive = true
)
```

#### E.3. Task Checklist - Navigation

- [ ] Remove transition parameter from all `navigate()` calls
- [ ] Update `navigateAndClearTo()` to `navigateAndClear()` with class refs
- [ ] Update `popTo()` to use destination class or instance
- [ ] Replace parent navigator usage with single navigator
- [ ] Verify all navigation flows work correctly

---

### Phase F: Testing & Verification (Day 3-4)

#### F.1. Compile & Run Tests

```bash
# Verify compilation
./gradlew :composeApp:assembleDebug

# Run unit tests
./gradlew :composeApp:testDebugUnitTest

# Run on device
./gradlew :composeApp:installDebug
```

#### F.2. Manual Testing Checklist

- [ ] App launches without crash
- [ ] Tab navigation works (all 3 tabs)
- [ ] Tab state preserved when switching
- [ ] Back navigation within tabs works
- [ ] Full-screen detail covers tab bar
- [ ] Back from full-screen returns to tab
- [ ] Predictive back gesture works
- [ ] Shared element transitions animate
- [ ] Deep links resolve correctly
- [ ] Process death restoration works
- [ ] Onboarding flow completes
- [ ] Settings navigation works

#### F.3. Platform Testing

- [ ] Android: Emulator & physical device
- [ ] iOS: Simulator (if available)
- [ ] Desktop: JVM target (if available)
- [ ] Web: Browser (if available)

---

## Demo App Feature Showcase

The rewritten demo app should demonstrate:

| Feature | Location in App |
|---------|-----------------|
| Simple stack navigation | Settings flow |
| Master-detail pattern | Home → Article |
| Tabbed navigation | Main tabs (Home/Search/Profile) |
| Tab state preservation | Switch tabs, verify history |
| Full-screen over tabs | Product detail from Home |
| Process/wizard flow | Onboarding screens |
| Deep linking | URL → specific screen |
| Shared elements | Product image animation |
| Predictive back | Swipe gesture throughout |
| Conditional navigation | Onboarding branching |

## Acceptance Criteria

- [ ] All old annotations replaced with new
- [ ] App compiles without errors
- [ ] All screens render correctly
- [ ] Navigation flows work as before
- [ ] Tab state preservation works
- [ ] Predictive back works
- [ ] No regressions from old behavior
- [ ] Demo showcases all major patterns

## Rollback Plan

If issues arise during rewrite:
1. Keep old code in `demo-legacy/` branch
2. Incremental migration: one screen at a time
3. Feature flags if needed: `USE_NEW_NAVIGATION`

## Related Tasks

- [MIG-001](./MIG-001-simple-stack-example.md) through [MIG-005](./MIG-005-nested-tabs-detail-example.md) - Reference examples
- [TEST-003](../phase8-testing/TEST-003-integration-tests.md) - Integration tests with demo app
