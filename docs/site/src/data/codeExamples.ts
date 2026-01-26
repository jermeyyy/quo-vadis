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
