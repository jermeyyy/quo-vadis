# Shared Infrastructure Components - Implementation Plan

## Overview

This plan establishes the shared infrastructure components required for deduplicating content across the Quo Vadis documentation site. Based on analysis of 12+ pages, **42+ content duplications** were identified across categories including version constants, code examples, platform support grids, transition type displays, NavNode type tables, and container scope property tables.

**Critical**: These infrastructure components must be created BEFORE any page-level refactoring can begin.

---

## Phase 1: Data Files

### Task 1.1: Create constants.ts

**File**: `src/data/constants.ts`

**Purpose**: Centralize version numbers and other frequently-used constants to enable single-point updates.

**Current Duplication Sources**:
- [Home.tsx](src/pages/Home/Home.tsx) - `quoVadis = "0.3.4"` in quickstartCode
- [GettingStarted.tsx](src/pages/GettingStarted/GettingStarted.tsx) - Multiple occurrences in installation code blocks
- [DIIntegration.tsx](src/pages/Features/DIIntegration/DIIntegration.tsx) - dependency version
- [Multiplatform.tsx](src/pages/Features/Multiplatform/Multiplatform.tsx) - Gradle setup code

**Implementation**:

```typescript
// src/data/constants.ts

/**
 * Library version - single source of truth for version strings across documentation
 * Update this when releasing new versions
 */
export const LIBRARY_VERSION = '0.3.4';

/**
 * KSP version compatible with current library version
 */
export const KSP_VERSION = '2.3.0';

/**
 * Maven artifact coordinates
 */
export const MAVEN_ARTIFACTS = {
  core: 'io.github.jermeyyy:quo-vadis-core',
  annotations: 'io.github.jermeyyy:quo-vadis-annotations',
  ksp: 'io.github.jermeyyy:quo-vadis-ksp',
  flowMvi: 'io.github.jermeyyy:quo-vadis-core-flow-mvi',
} as const;

/**
 * Gradle plugin ID
 */
export const GRADLE_PLUGIN_ID = 'io.github.jermeyyy.quo-vadis';

/**
 * Repository URLs
 */
export const REPOSITORY_URLS = {
  github: 'https://github.com/jermeyyy/quo-vadis',
  mavenCentral: 'https://central.sonatype.com/artifact/io.github.jermeyyy/quo-vadis-core',
  apiDocs: '/quo-vadis/api/index.html',
} as const;

/**
 * Helper to generate full artifact coordinate with version
 */
export const getArtifactCoordinate = (artifact: keyof typeof MAVEN_ARTIFACTS): string => {
  return `${MAVEN_ARTIFACTS[artifact]}:${LIBRARY_VERSION}`;
};
```

**Acceptance Criteria**:
- [ ] File created at `src/data/constants.ts`
- [ ] All constants properly typed with TypeScript
- [ ] `as const` assertions used for object literals to ensure literal types
- [ ] JSDoc comments explain purpose of each constant
- [ ] `getArtifactCoordinate` helper function included
- [ ] File exports are named exports (no default export)

---

### Task 1.2: Create codeExamples.ts

**File**: `src/data/codeExamples.ts`

**Purpose**: Centralize frequently-duplicated Kotlin code examples for reuse across multiple documentation pages.

**Current Duplication Sources**:

| Example Category | Found In | Occurrences |
|------------------|----------|-------------|
| Stack/Destination definition | Home.tsx, GettingStarted.tsx, AnnotationAPI.tsx | 3+ |
| Screen binding | Home.tsx, GettingStarted.tsx, AnnotationAPI.tsx | 3+ |
| NavigationHost setup | Home.tsx, GettingStarted.tsx | 2+ |
| Tabs annotation | TabbedNavigation.tsx, AnnotationAPI.tsx | 2+ |
| Pane annotation | PaneLayouts.tsx, AnnotationAPI.tsx | 2+ |

**Implementation**:

```typescript
// src/data/codeExamples.ts
import { LIBRARY_VERSION, KSP_VERSION, GRADLE_PLUGIN_ID, getArtifactCoordinate } from './constants';

// ============================================================================
// STACK & DESTINATION EXAMPLES
// ============================================================================

/**
 * Basic stack definition with destinations - used in quickstarts and overviews
 */
export const stackDestinationBasic = `@Stack(name = "home", startDestination = Feed::class)
sealed class HomeDestination : NavDestination {

    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    @Destination(route = "home/article/{articleId}")
    data class Article(
        @Argument val articleId: String,
        @Argument(optional = true) val showComments: Boolean = false
    ) : HomeDestination()
}`;

/**
 * Extended stack definition with settings - used in Getting Started
 */
export const stackDestinationWithSettings = `import com.jermey.quo.vadis.annotations.*
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
}`;

/**
 * Comprehensive destination example with multiple parameter types
 */
export const stackDestinationComprehensive = `@Stack(name = "home", startDestination = HomeDestination.Feed::class)
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
}`;

// ============================================================================
// SCREEN BINDING EXAMPLES
// ============================================================================

/**
 * Basic screen binding with navigator
 */
export const screenBindingBasic = `@Screen(HomeDestination.Feed::class)
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
}`;

/**
 * Screen binding with full imports
 */
export const screenBindingWithImports = `import androidx.compose.runtime.Composable
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
}`;

// ============================================================================
// NAVIGATION HOST SETUP EXAMPLES
// ============================================================================

/**
 * Basic NavigationHost setup
 */
export const navigationHostBasic = `@Composable
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
}`;

/**
 * NavigationHost setup with full imports
 */
export const navigationHostWithImports = `import androidx.compose.runtime.Composable
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
}`;

// ============================================================================
// TABS ANNOTATION EXAMPLES
// ============================================================================

/**
 * Basic tabs annotation
 */
export const tabsAnnotationBasic = `@Tabs(
    name = "mainTabs",
    initialTab = MainTabs.HomeTab::class,
    items = [MainTabs.HomeTab::class, MainTabs.ExploreTab::class, MainTabs.ProfileTab::class]
)
sealed class MainTabs : NavDestination {

    @TabItem(label = "Home", icon = "home")
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    @TabItem(label = "Explore", icon = "explore")
    @Destination(route = "main/explore")
    data object ExploreTab : MainTabs()

    @TabItem(label = "Profile", icon = "person")
    @Destination(route = "main/profile")
    data object ProfileTab : MainTabs()
}`;

/**
 * Extended tabs with nested stack and transitions
 */
export const tabsAnnotationWithNestedStack = `@Tabs(
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
}`;

/**
 * TabsContainer wrapper example
 */
export const tabsContainerWrapper = `@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                scope.tabMetadata.forEachIndexed { index, meta ->
                    NavigationBarItem(
                        selected = index == scope.activeTabIndex,
                        onClick = { scope.switchTab(index) },
                        icon = { Icon(getTabIcon(meta.icon), meta.label) },
                        label = { Text(meta.label) },
                        enabled = !scope.isTransitioning
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            content()
        }
    }
}`;

// ============================================================================
// PANE ANNOTATION EXAMPLES
// ============================================================================

/**
 * Basic pane annotation
 */
export const paneAnnotationBasic = `@Pane(name = "messagesPane", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class MessagesPane : NavDestination {

    @PaneItem(role = PaneRole.PRIMARY)
    @Destination(route = "messages/conversations")
    data object ConversationList : MessagesPane()

    @PaneItem(role = PaneRole.SECONDARY)
    @Destination(route = "messages/conversation/{conversationId}")
    data class ConversationDetail(
        @Argument val conversationId: String
    ) : MessagesPane()
}`;

/**
 * PaneContainer wrapper example
 */
export const paneContainerWrapper = `@PaneContainer(MessagesPane::class)
@Composable
fun MessagesPaneContainer(
    scope: PaneContainerScope,
    content: @Composable () -> Unit
) {
    if (scope.isExpanded) {
        // Expanded: Two-column layout
        Row(modifier = Modifier.fillMaxSize()) {
            scope.paneContents.filter { it.isVisible }.forEach { pane ->
                val weight = when (pane.role) {
                    PaneRole.Primary -> 0.4f
                    PaneRole.Supporting -> 0.6f
                    else -> 0.25f
                }
                Box(modifier = Modifier.weight(weight).fillMaxHeight()) {
                    if (pane.hasContent) pane.content()
                    else EmptyPlaceholder()
                }
            }
        }
    } else {
        // Compact: Single pane
        content()
    }
}`;

// ============================================================================
// GRADLE/BUILD CONFIGURATION EXAMPLES
// ============================================================================

/**
 * Version catalog configuration (libs.versions.toml)
 */
export const versionCatalogConfig = `[versions]
quoVadis = "${LIBRARY_VERSION}"
ksp = "${KSP_VERSION}"

[libraries]
quo-vadis-core = { module = "${getArtifactCoordinate('core').split(':').slice(0, 2).join(':')}", version.ref = "quoVadis" }
quo-vadis-annotations = { module = "${getArtifactCoordinate('annotations').split(':').slice(0, 2).join(':')}", version.ref = "quoVadis" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
quo-vadis = { id = "${GRADLE_PLUGIN_ID}", version.ref = "quoVadis" }`;

/**
 * Gradle plugin installation (recommended)
 */
export const gradlePluginInstallation = `// settings.gradle.kts
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
    id("com.google.devtools.ksp") version "${KSP_VERSION}"
    id("${GRADLE_PLUGIN_ID}") version "${LIBRARY_VERSION}"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("${getArtifactCoordinate('core')}")
            implementation("${getArtifactCoordinate('annotations')}")
        }
    }
}

// Optional: Configure the plugin
quoVadis {
    modulePrefix = "MyApp"  // Generates MyAppNavigationConfig
}`;

/**
 * Manual KSP configuration
 */
export const manualKspConfiguration = `// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp") version "${KSP_VERSION}"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("${getArtifactCoordinate('core')}")
            implementation("${getArtifactCoordinate('annotations')}")
        }
    }
    
    ksp {
        arg("quoVadis.modulePrefix", "MyApp")
    }
}

dependencies {
    add("kspCommonMainMetadata", "${getArtifactCoordinate('ksp')}")
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
}`;

// ============================================================================
// TYPE EXPORTS
// ============================================================================

export type CodeExampleKey = 
  | 'stackDestinationBasic'
  | 'stackDestinationWithSettings'
  | 'stackDestinationComprehensive'
  | 'screenBindingBasic'
  | 'screenBindingWithImports'
  | 'navigationHostBasic'
  | 'navigationHostWithImports'
  | 'tabsAnnotationBasic'
  | 'tabsAnnotationWithNestedStack'
  | 'tabsContainerWrapper'
  | 'paneAnnotationBasic'
  | 'paneContainerWrapper'
  | 'versionCatalogConfig'
  | 'gradlePluginInstallation'
  | 'manualKspConfiguration';
```

**Acceptance Criteria**:
- [ ] File created at `src/data/codeExamples.ts`
- [ ] Imports constants from `./constants`
- [ ] All code examples use template literals with proper escaping
- [ ] Version numbers are interpolated from constants
- [ ] Each example has JSDoc comment explaining its use case
- [ ] Logical groupings with section comment headers
- [ ] TypeScript type for example keys exported
- [ ] All examples compile-valid Kotlin syntax

---

## Phase 2: Shared Components

### Task 2.1: Create PlatformSupportGrid Component

**Location**: `src/components/PlatformSupportGrid/`

**Purpose**: Unified component for displaying platform support information in either compact (feature matrix) or detailed (feature cards) format.

**Current Duplication Sources**:
- [Home.tsx](src/pages/Home/Home.tsx) - "Why Quo Vadis" feature cards with platform mentions
- [Features.tsx](src/pages/Features/Features.tsx#L168-198) - platformGrid with Android/iOS/Desktop/Web cards
- [Demo.tsx](src/pages/Demo/Demo.tsx#L170-213) - platformGrid showing platform-specific features
- [Multiplatform.tsx](src/pages/Features/Multiplatform/Multiplatform.tsx#L103-176) - Platform support table + feature matrix

**Implementation**:

**File: `src/components/PlatformSupportGrid/index.ts`**
```typescript
export { PlatformSupportGrid } from './PlatformSupportGrid';
export type { PlatformSupportGridProps } from './PlatformSupportGrid';
```

**File: `src/components/PlatformSupportGrid/PlatformSupportGrid.tsx`**
```tsx
import styles from './PlatformSupportGrid.module.css';

export interface PlatformFeature {
  name: string;
  android: 'full' | 'partial' | 'none';
  ios: 'full' | 'partial' | 'none';
  desktop: 'full' | 'partial' | 'none';
  web: 'full' | 'partial' | 'none';
}

export interface PlatformCardData {
  platform: 'Android' | 'iOS' | 'Desktop' | 'Web';
  features: string[];
}

export interface PlatformSupportGridProps {
  variant: 'compact' | 'detailed' | 'cards';
  /** For 'compact' variant - feature matrix data */
  features?: PlatformFeature[];
  /** For 'cards' variant - platform card data */
  cards?: PlatformCardData[];
  /** For 'detailed' variant - show full platform capabilities */
  showRequirements?: boolean;
}

// Status icon components
const StatusIcon = ({ status }: { status: 'full' | 'partial' | 'none' }) => {
  const className = {
    full: styles.statusIconYes,
    partial: styles.statusIconPartial,
    none: styles.statusIconNa,
  }[status];
  return <span className={`${styles.statusIcon} ${className}`} />;
};

// Default platform features for compact view
const DEFAULT_FEATURES: PlatformFeature[] = [
  { name: 'Stack navigation', android: 'full', ios: 'full', desktop: 'full', web: 'full' },
  { name: 'Tab navigation', android: 'full', ios: 'full', desktop: 'full', web: 'full' },
  { name: 'Pane layouts', android: 'full', ios: 'full', desktop: 'full', web: 'full' },
  { name: 'Deep links', android: 'full', ios: 'full', desktop: 'partial', web: 'full' },
  { name: 'Predictive back', android: 'full', ios: 'full', desktop: 'none', web: 'none' },
  { name: 'Shared elements', android: 'full', ios: 'full', desktop: 'full', web: 'full' },
  { name: 'Browser history', android: 'none', ios: 'none', desktop: 'none', web: 'full' },
];

// Default platform cards
const DEFAULT_CARDS: PlatformCardData[] = [
  {
    platform: 'Android',
    features: [
      'System back button integration',
      'Predictive back gestures (Android 13+)',
      'Deep link support',
      'SavedStateHandle integration',
    ],
  },
  {
    platform: 'iOS',
    features: [
      'Native swipe gestures',
      'Predictive back animations',
      'Universal Links support',
      'Navigation bar integration',
    ],
  },
  {
    platform: 'Desktop',
    features: [
      'Keyboard shortcuts (Alt+Left/Right)',
      'Mouse button navigation',
      'Window state persistence',
      'All core features',
    ],
  },
  {
    platform: 'Web',
    features: [
      'Browser history integration',
      'URL routing',
      'Forward/back buttons',
      'Deep linking via URLs',
    ],
  },
];

export function PlatformSupportGrid({
  variant,
  features = DEFAULT_FEATURES,
  cards = DEFAULT_CARDS,
  showRequirements = false,
}: PlatformSupportGridProps) {
  if (variant === 'compact') {
    return (
      <table className={styles.compactTable}>
        <thead>
          <tr>
            <th>Feature</th>
            <th style={{ textAlign: 'center' }}>Android</th>
            <th style={{ textAlign: 'center' }}>iOS</th>
            <th style={{ textAlign: 'center' }}>Desktop</th>
            <th style={{ textAlign: 'center' }}>Web</th>
          </tr>
        </thead>
        <tbody>
          {features.map((feature) => (
            <tr key={feature.name}>
              <td className={styles.featureCell}>{feature.name}</td>
              <td><StatusIcon status={feature.android} /></td>
              <td><StatusIcon status={feature.ios} /></td>
              <td><StatusIcon status={feature.desktop} /></td>
              <td><StatusIcon status={feature.web} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  if (variant === 'cards') {
    return (
      <div className={styles.platformGrid}>
        {cards.map((card) => (
          <div key={card.platform} className={styles.platformCard}>
            <h3>{card.platform}</h3>
            <ul>
              {card.features.map((feature, idx) => (
                <li key={idx}>{feature}</li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    );
  }

  // 'detailed' variant - combines table with requirements
  return (
    <div className={styles.detailedGrid}>
      <table className={styles.detailedTable}>
        <thead>
          <tr>
            <th>Platform</th>
            <th>Targets</th>
            <th style={{ textAlign: 'center' }}>Status</th>
            {showRequirements && <th>Requirements</th>}
          </tr>
        </thead>
        <tbody>
          <tr>
            <td className={styles.featureCell}>Android</td>
            <td><span className={styles.platformBadge}>android</span></td>
            <td style={{ textAlign: 'center' }}><span className={styles.statusFull}>Full</span></td>
            {showRequirements && <td>API 21+</td>}
          </tr>
          <tr>
            <td className={styles.featureCell}>iOS</td>
            <td>
              <div className={styles.platformBadges}>
                <span className={styles.platformBadge}>iosArm64</span>
                <span className={styles.platformBadge}>iosSimulatorArm64</span>
                <span className={styles.platformBadge}>iosX64</span>
              </div>
            </td>
            <td style={{ textAlign: 'center' }}><span className={styles.statusFull}>Full</span></td>
            {showRequirements && <td>iOS 14+</td>}
          </tr>
          <tr>
            <td className={styles.featureCell}>Desktop</td>
            <td><span className={styles.platformBadge}>jvm</span></td>
            <td style={{ textAlign: 'center' }}><span className={styles.statusFull}>Full</span></td>
            {showRequirements && <td>macOS, Windows, Linux</td>}
          </tr>
          <tr>
            <td className={styles.featureCell}>JavaScript</td>
            <td><span className={styles.platformBadge}>js (IR)</span></td>
            <td style={{ textAlign: 'center' }}><span className={styles.statusFull}>Full</span></td>
            {showRequirements && <td>Modern browsers</td>}
          </tr>
          <tr>
            <td className={styles.featureCell}>WebAssembly</td>
            <td><span className={styles.platformBadge}>wasmJs</span></td>
            <td style={{ textAlign: 'center' }}><span className={styles.statusFull}>Full</span></td>
            {showRequirements && <td>WASM-compatible browsers</td>}
          </tr>
        </tbody>
      </table>
    </div>
  );
}
```

**File: `src/components/PlatformSupportGrid/PlatformSupportGrid.module.css`**
```css
.compactTable {
  width: 100%;
  border-collapse: collapse;
}

.compactTable th,
.compactTable td {
  padding: 0.75rem;
  border-bottom: 1px solid var(--color-border);
}

.compactTable td {
  text-align: center;
}

.featureCell {
  font-weight: 500;
  text-align: left !important;
}

.platformGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
}

.platformCard {
  background: var(--color-bg-elevated);
  border-radius: 12px;
  padding: 1.5rem;
  border: 1px solid var(--color-border);
}

.platformCard h3 {
  margin-bottom: 1rem;
  font-size: 1.125rem;
}

.platformCard ul {
  margin: 0;
  padding-left: 1.25rem;
}

.platformCard li {
  margin-bottom: 0.5rem;
  line-height: 1.5;
}

.statusIcon {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 50%;
}

.statusIconYes {
  background: #22c55e;
}

.statusIconPartial {
  background: #eab308;
}

.statusIconNa {
  background: var(--color-text-muted);
  opacity: 0.3;
}

.platformBadge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  background: var(--color-bg-muted);
  border-radius: 4px;
  font-size: 0.875rem;
  font-family: var(--font-mono);
}

.platformBadges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.statusFull {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: rgba(34, 197, 94, 0.15);
  color: #16a34a;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
}

.detailedTable {
  width: 100%;
  border-collapse: collapse;
}

.detailedTable th,
.detailedTable td {
  padding: 1rem;
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}
```

**Acceptance Criteria**:
- [ ] Component supports three variants: `compact`, `detailed`, `cards`
- [ ] Props interface properly typed with TypeScript
- [ ] CSS module created with all necessary styles
- [ ] Default data provided for common use cases
- [ ] Status icons match existing design (green=full, yellow=partial, gray=none)
- [ ] Responsive grid layout for cards variant
- [ ] Component exported from index.ts barrel file

---

### Task 2.2: Create TransitionTypesDisplay Component

**Location**: `src/components/TransitionTypesDisplay/`

**Purpose**: Unified display of available transition types in either grid (visual cards) or table (detailed comparison) format.

**Current Duplication Sources**:
- [Features.tsx](src/pages/Features/Features.tsx#L256-276) - transitionGrid with transition cards
- [Transitions.tsx](src/pages/Features/Transitions/Transitions.tsx#L96-119) - Built-in transitions table
- [AnnotationAPI.tsx](src/pages/Features/AnnotationAPI/AnnotationAPI.tsx#L134-162) - TransitionType options grid
- [DSLConfig.tsx](src/pages/Features/DSLConfig/DSLConfig.tsx#L218-232) - Preset transitions table

**Implementation**:

**File: `src/components/TransitionTypesDisplay/index.ts`**
```typescript
export { TransitionTypesDisplay } from './TransitionTypesDisplay';
export type { TransitionTypesDisplayProps, TransitionTypeData } from './TransitionTypesDisplay';
```

**File: `src/components/TransitionTypesDisplay/TransitionTypesDisplay.tsx`**
```tsx
import styles from './TransitionTypesDisplay.module.css';

export interface TransitionTypeData {
  name: string;
  description: string;
  enter?: string;
  exit?: string;
  bestFor?: string;
}

export interface TransitionTypesDisplayProps {
  variant: 'grid' | 'table';
  /** Custom transitions to display (optional - uses defaults if not provided) */
  transitions?: TransitionTypeData[];
  /** Show enter/exit columns in table variant */
  showAnimationDetails?: boolean;
}

const DEFAULT_TRANSITIONS: TransitionTypeData[] = [
  {
    name: 'SlideHorizontal',
    description: 'Standard horizontal slide for hierarchical navigation',
    enter: 'Slide from right',
    exit: 'Slide to left',
    bestFor: 'Stack navigation (default)',
  },
  {
    name: 'SlideVertical',
    description: 'Vertical slide for modal presentations',
    enter: 'Slide from bottom',
    exit: 'Slide to top',
    bestFor: 'Modal sheets',
  },
  {
    name: 'Fade',
    description: 'Simple cross-fade between screens',
    enter: 'Fade in',
    exit: 'Fade out',
    bestFor: 'Tab switching, overlays',
  },
  {
    name: 'FadeThrough',
    description: 'Material Design fade through pattern',
    enter: 'Scale + fade in',
    exit: 'Scale + fade out',
    bestFor: 'Navigation rail, top-level changes',
  },
  {
    name: 'ScaleIn',
    description: 'Scale animation emphasizing detail views',
    enter: 'Scale from 80%',
    exit: 'Scale to 95%',
    bestFor: 'Detail view transitions',
  },
  {
    name: 'None',
    description: 'Instant navigation without animation',
    enter: 'Instant',
    exit: 'Instant',
    bestFor: 'Testing, custom animations',
  },
];

export function TransitionTypesDisplay({
  variant,
  transitions = DEFAULT_TRANSITIONS,
  showAnimationDetails = false,
}: TransitionTypesDisplayProps) {
  if (variant === 'grid') {
    return (
      <div className={styles.transitionGrid}>
        {transitions.map((transition) => (
          <div key={transition.name} className={styles.transitionCard}>
            <h4>{transition.name}</h4>
            <p>{transition.description}</p>
          </div>
        ))}
      </div>
    );
  }

  // Table variant
  return (
    <table className={styles.transitionTable}>
      <thead>
        <tr>
          <th>Transition</th>
          {showAnimationDetails && (
            <>
              <th>Enter</th>
              <th>Exit</th>
            </>
          )}
          <th>Best For</th>
        </tr>
      </thead>
      <tbody>
        {transitions.map((transition) => (
          <tr key={transition.name}>
            <td><code>{transition.name}</code></td>
            {showAnimationDetails && (
              <>
                <td>{transition.enter}</td>
                <td>{transition.exit}</td>
              </>
            )}
            <td>{transition.bestFor}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

**File: `src/components/TransitionTypesDisplay/TransitionTypesDisplay.module.css`**
```css
.transitionGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.transitionCard {
  background: var(--color-bg-elevated);
  border-radius: 8px;
  padding: 1rem 1.25rem;
  border: 1px solid var(--color-border);
}

.transitionCard h4 {
  margin: 0 0 0.5rem;
  font-size: 1rem;
  font-weight: 600;
}

.transitionCard p {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.transitionTable {
  width: 100%;
  border-collapse: collapse;
}

.transitionTable th,
.transitionTable td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}

.transitionTable th {
  font-weight: 600;
  background: var(--color-bg-muted);
}

.transitionTable code {
  font-family: var(--font-mono);
  font-size: 0.875rem;
  background: var(--color-bg-elevated);
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
}
```

**Acceptance Criteria**:
- [ ] Component supports `grid` and `table` variants
- [ ] Default transition data includes all 6 types (SlideHorizontal, SlideVertical, Fade, FadeThrough, ScaleIn, None)
- [ ] `showAnimationDetails` prop controls enter/exit column visibility in table
- [ ] CSS module with responsive grid layout
- [ ] Types exported for external use

---

### Task 2.3: Create NavNodeTypesTable Component

**Location**: `src/components/NavNodeTypesTable/`

**Purpose**: Standardized table showing NavNode types (ScreenNode, StackNode, TabNode, PaneNode) with their purpose and contents.

**Current Duplication Sources**:
- [Architecture.tsx](src/pages/Features/Architecture/Architecture.tsx#L269-295) - NavNode types table
- [BackStack.tsx](src/pages/Features/BackStack/BackStack.tsx#L41-67) - NavNode types with same structure

**Implementation**:

**File: `src/components/NavNodeTypesTable/index.ts`**
```typescript
export { NavNodeTypesTable } from './NavNodeTypesTable';
export type { NavNodeTypesTableProps } from './NavNodeTypesTable';
```

**File: `src/components/NavNodeTypesTable/NavNodeTypesTable.tsx`**
```tsx
import styles from './NavNodeTypesTable.module.css';

export interface NavNodeTypeData {
  type: string;
  purpose: string;
  contains: string;
}

export interface NavNodeTypesTableProps {
  /** Show expanded descriptions (default: false) */
  expanded?: boolean;
  /** Custom data (optional) */
  nodes?: NavNodeTypeData[];
}

const DEFAULT_NODES: NavNodeTypeData[] = [
  {
    type: 'ScreenNode',
    purpose: 'Leaf destination',
    contains: 'Destination data',
  },
  {
    type: 'StackNode',
    purpose: 'Linear navigation',
    contains: 'List of children (last = active)',
  },
  {
    type: 'TabNode',
    purpose: 'Parallel tabs',
    contains: 'List of StackNodes',
  },
  {
    type: 'PaneNode',
    purpose: 'Adaptive panes',
    contains: 'Map of PaneRole to configuration',
  },
];

export function NavNodeTypesTable({
  expanded = false,
  nodes = DEFAULT_NODES,
}: NavNodeTypesTableProps) {
  return (
    <table className={styles.nodeTable}>
      <thead>
        <tr>
          <th>Type</th>
          <th>Purpose</th>
          <th>Contains</th>
        </tr>
      </thead>
      <tbody>
        {nodes.map((node) => (
          <tr key={node.type}>
            <td><code>{node.type}</code></td>
            <td>{node.purpose}</td>
            <td>{node.contains}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

**File: `src/components/NavNodeTypesTable/NavNodeTypesTable.module.css`**
```css
.nodeTable {
  width: 100%;
  border-collapse: collapse;
}

.nodeTable th,
.nodeTable td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}

.nodeTable th {
  font-weight: 600;
  background: var(--color-bg-muted);
}

.nodeTable code {
  font-family: var(--font-mono);
  font-size: 0.875rem;
  background: var(--color-bg-elevated);
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
}
```

**Acceptance Criteria**:
- [ ] Component renders table with Type, Purpose, Contains columns
- [ ] Default data includes all 4 NavNode types
- [ ] Types styled with `<code>` formatting
- [ ] CSS module follows existing design patterns
- [ ] Props interface exported for consumers

---

### Task 2.4: Create ScopePropertiesTable Component

**Location**: `src/components/ScopePropertiesTable/`

**Purpose**: Unified component for displaying container scope properties for both TabsContainerScope and PaneContainerScope.

**Current Duplication Sources**:
- [TabbedNavigation.tsx](src/pages/Features/TabbedNavigation/TabbedNavigation.tsx#L102-132) - TabsContainerScope properties
- [DSLConfig.tsx](src/pages/Features/DSLConfig/DSLConfig.tsx#L154-184) - TabsContainerScope same table
- [PaneLayouts.tsx](src/pages/Features/PaneLayouts/PaneLayouts.tsx#L177-207) - PaneContainerScope properties
- [DSLConfig.tsx](src/pages/Features/DSLConfig/DSLConfig.tsx#L186-216) - PaneContainerScope same table

**Implementation**:

**File: `src/components/ScopePropertiesTable/index.ts`**
```typescript
export { ScopePropertiesTable } from './ScopePropertiesTable';
export type { ScopePropertiesTableProps, ScopeProperty } from './ScopePropertiesTable';
```

**File: `src/components/ScopePropertiesTable/ScopePropertiesTable.tsx`**
```tsx
import styles from './ScopePropertiesTable.module.css';

export interface ScopeProperty {
  property: string;
  type: string;
  description: string;
}

export interface ScopePropertiesTableProps {
  scopeType: 'tabs' | 'pane';
  /** Custom properties (optional) */
  properties?: ScopeProperty[];
}

const TABS_SCOPE_PROPERTIES: ScopeProperty[] = [
  { property: 'navigator', type: 'Navigator', description: 'Navigation operations' },
  { property: 'activeTabIndex', type: 'Int', description: 'Currently selected tab (0-based)' },
  { property: 'tabCount', type: 'Int', description: 'Total number of tabs' },
  { property: 'tabMetadata', type: 'List<TabMetadata>', description: 'Labels, icons, routes for tabs' },
  { property: 'isTransitioning', type: 'Boolean', description: 'Whether transition is in progress' },
  { property: 'switchTab(index)', type: 'Function', description: 'Switch to different tab' },
];

const PANE_SCOPE_PROPERTIES: ScopeProperty[] = [
  { property: 'navigator', type: 'Navigator', description: 'Navigator instance for programmatic navigation' },
  { property: 'activePaneRole', type: 'PaneRole', description: 'Currently active/focused pane' },
  { property: 'paneCount', type: 'Int', description: 'Total configured panes' },
  { property: 'visiblePaneCount', type: 'Int', description: 'Currently visible panes' },
  { property: 'isExpanded', type: 'Boolean', description: 'Whether multi-pane mode is active' },
  { property: 'isTransitioning', type: 'Boolean', description: 'Whether pane transition is in progress' },
  { property: 'paneContents', type: 'List<PaneContent>', description: 'Content slots for custom layout rendering' },
];

export function ScopePropertiesTable({
  scopeType,
  properties,
}: ScopePropertiesTableProps) {
  const data = properties ?? (scopeType === 'tabs' ? TABS_SCOPE_PROPERTIES : PANE_SCOPE_PROPERTIES);
  const scopeName = scopeType === 'tabs' ? 'TabsContainerScope' : 'PaneContainerScope';

  return (
    <div className={styles.container}>
      <h4 className={styles.title}>{scopeName} Properties</h4>
      <table className={styles.scopeTable}>
        <thead>
          <tr>
            <th>Property</th>
            <th>Type</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          {data.map((prop) => (
            <tr key={prop.property}>
              <td><code>{prop.property}</code></td>
              <td><code>{prop.type}</code></td>
              <td>{prop.description}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

**File: `src/components/ScopePropertiesTable/ScopePropertiesTable.module.css`**
```css
.container {
  margin: 1rem 0;
}

.title {
  margin: 0 0 0.75rem;
  font-size: 1rem;
  font-weight: 600;
}

.scopeTable {
  width: 100%;
  border-collapse: collapse;
}

.scopeTable th,
.scopeTable td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}

.scopeTable th {
  font-weight: 600;
  background: var(--color-bg-muted);
}

.scopeTable code {
  font-family: var(--font-mono);
  font-size: 0.875rem;
  background: var(--color-bg-elevated);
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
}
```

**Acceptance Criteria**:
- [ ] Component supports `tabs` and `pane` scope types
- [ ] Default data provided for both scope types
- [ ] Table shows Property, Type, Description columns
- [ ] Title dynamically shows scope name
- [ ] All properties/types styled with `<code>` formatting
- [ ] CSS module follows existing patterns

---

## Task Dependencies & Sequencing

```
Phase 1 (Data Files)
├── Task 1.1: constants.ts ────────────┐
│                                      │
└── Task 1.2: codeExamples.ts ────────┤ (depends on 1.1)
                                       │
Phase 2 (Components)                   │
├── Task 2.1: PlatformSupportGrid ────┤ (independent)
│                                      │
├── Task 2.2: TransitionTypesDisplay ─┤ (independent)
│                                      │
├── Task 2.3: NavNodeTypesTable ──────┤ (independent)
│                                      │
└── Task 2.4: ScopePropertiesTable ───┘ (independent)
```

**Execution Order**:
1. Task 1.1 (constants.ts) - FIRST, no dependencies
2. Task 1.2 (codeExamples.ts) - after 1.1 (imports from constants)
3. Tasks 2.1-2.4 - can be parallelized after Phase 1 completes

---

## Acceptance Criteria Summary

### Phase 1 Complete When:
- [ ] `src/data/constants.ts` exists with all version constants
- [ ] `src/data/codeExamples.ts` exists with all code examples
- [ ] Both files have proper TypeScript types
- [ ] Version strings are centralized (no hardcoded "0.3.4" except in constants.ts)
- [ ] All exports are named exports

### Phase 2 Complete When:
- [ ] All 4 component folders created under `src/components/`
- [ ] Each component has: index.ts, ComponentName.tsx, ComponentName.module.css
- [ ] All components support required variants/props
- [ ] CSS modules use existing CSS variables (--color-*, --font-*)
- [ ] TypeScript interfaces exported for all props
- [ ] No runtime errors when components render with default props

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| CSS variable mismatches | Medium | Low | Verify variables exist in global styles before use |
| Missing variant coverage | Medium | Medium | Each component tested with all variant values |
| Import path issues | Low | Low | Use relative imports within data folder |
| Type export conflicts | Low | Low | Use descriptive names, export via barrel files |
| Breaking existing styles | Medium | High | Create new CSS modules, don't modify Features.module.css initially |

---

## Post-Implementation Notes

After infrastructure is complete, page refactoring can begin by:

1. Importing constants/examples from `src/data/`
2. Replacing inline code blocks with centralized examples
3. Replacing duplicated tables/grids with shared components
4. Running `npm run build` to verify no regressions

Recommended refactoring order (by duplication impact):
1. GettingStarted.tsx - highest version duplication
2. Home.tsx - uses multiple shared patterns
3. Multiplatform.tsx - platform grid heavy
4. TabbedNavigation.tsx + PaneLayouts.tsx - scope tables
5. Features.tsx + Transitions.tsx - transition displays
6. Architecture.tsx + BackStack.tsx - NavNode tables
