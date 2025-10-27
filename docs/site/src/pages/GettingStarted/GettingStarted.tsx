import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './GettingStarted.module.css'

const installationCode = `// build.gradle.kts
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.2.20"
    id("com.google.devtools.ksp") version "2.2.20-1.0.29"
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core navigation library
                implementation("com.jermey.quo.vadis:quo-vadis-core:0.1.0")
                
                // Annotation-based API (recommended)
                implementation("com.jermey.quo.vadis:quo-vadis-annotations:0.1.0")
                
                // For type-safe arguments
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }
    }
}

dependencies {
    // KSP code generator
    add("kspCommonMainMetadata", "com.jermey.quo.vadis:quo-vadis-ksp:0.1.0")
}`

const versionCatalogCode = `# libs.versions.toml
[versions]
quoVadis = "0.1.0"
ksp = "2.2.20-1.0.29"
kotlinxSerialization = "1.6.0"

[libraries]
quo-vadis-core = { module = "com.jermey.quo.vadis:quo-vadis-core", version.ref = "quoVadis" }
quo-vadis-annotations = { module = "com.jermey.quo.vadis:quo-vadis-annotations", version.ref = "quoVadis" }
quo-vadis-ksp = { module = "com.jermey.quo.vadis:quo-vadis-ksp", version.ref = "quoVadis" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }

# In build.gradle.kts
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(libs.quo.vadis.core)
    implementation(libs.quo.vadis.annotations)
    implementation(libs.kotlinx.serialization.json)
    add("kspCommonMainMetadata", libs.quo.vadis.ksp)
}`

const destinationsCode = `import com.jermey.quo.vadis.annotations.*
import com.jermey.quo.vadis.core.navigation.core.*
import kotlinx.serialization.Serializable

// Define serializable data for typed arguments
@Serializable
data class ProfileData(val userId: String, val tab: String = "posts")

@Graph("app")
sealed class AppDestination : Destination {
    @Route("app/home")
    data object Home : AppDestination()
    
    @Route("app/profile")
    @Argument(ProfileData::class)
    data class UserProfile(val userId: String, val tab: String = "posts") 
        : AppDestination(), TypedDestination<ProfileData> {
        override val data = ProfileData(userId, tab)
    }
    
    @Route("app/settings")
    data object Settings : AppDestination()
}`

const contentCode = `import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.annotations.Content

@Content(AppDestination.Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
    HomeScreen(
        onNavigateToProfile = { userId ->
            navigator.navigate(AppDestination.UserProfile(userId))
        },
        onNavigateToSettings = {
            navigator.navigate(AppDestination.Settings)
        }
    )
}

// Typed destinations receive data as first parameter
@Content(AppDestination.UserProfile::class)
@Composable
fun ProfileContent(data: ProfileData, navigator: Navigator) {
    ProfileScreen(
        userId = data.userId,
        initialTab = data.tab,
        onBack = { navigator.navigateBack() }
    )
}

@Content(AppDestination.Settings::class)
@Composable
fun SettingsContent(navigator: Navigator) {
    SettingsScreen(
        onBack = { navigator.navigateBack() }
    )
}`

const graphCode = `import com.example.app.destinations.buildAppDestinationGraph

// Use the generated graph builder
fun rootGraph() = navigationGraph("root") {
    startDestination(AppDestination.Home)
    include(buildAppDestinationGraph())  // Auto-generated!
}`

const navHostCode = `@Composable
fun App() {
    val navigator = rememberNavigator()
    
    LaunchedEffect(Unit) {
        navigator.registerGraph(rootGraph())
        navigator.setStartDestination(AppDestination.Home)
    }
    
    GraphNavHost(
        graph = rootGraph(),
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
}`

const manualDSLCode = `// Define destinations manually
sealed class AppDestination : Destination {
    object Home : AppDestination() {
        override val route = "home"
    }
    
    data class UserProfile(val userId: String) : AppDestination() {
        override val route = "profile"
        override val arguments = mapOf("userId" to userId)
    }
}

// Build graph manually
val mainGraph = navigationGraph("main") {
    startDestination(AppDestination.Home)
    
    destination(AppDestination.Home) { _, navigator ->
        HomeScreen(
            onNavigateToProfile = { userId ->
                navigator.navigate(AppDestination.UserProfile(userId))
            }
        )
    }
    
    destination(SimpleDestination("profile")) { dest, navigator ->
        val userId = dest.arguments["userId"] as String
        ProfileScreen(
            userId = userId,
            onBack = { navigator.navigateBack() }
        )
    }
}`

const basicNavCode = `// Navigate to a destination
navigator.navigate(AppDestination.UserProfile("user123"))

// Navigate with custom transition
navigator.navigate(
    destination = AppDestination.Settings(),
    transition = NavigationTransitions.FadeThrough
)

// Navigate back
navigator.navigateBack()

// Navigate up (parent destination)
navigator.navigateUp()`

const advancedNavCode = `// Navigate and replace current screen
navigator.navigateAndReplace(AppDestination.Home)

// Navigate and clear entire backstack
navigator.navigateAndClearAll(AppDestination.Home)

// Navigate and clear to specific destination
navigator.navigateAndClearTo(
    destination = AppDestination.Home,
    clearRoute = "login",
    inclusive = true
)`

const transitionsCode = `// Available transitions
NavigationTransitions.SlideHorizontal
NavigationTransitions.SlideVertical
NavigationTransitions.Fade
NavigationTransitions.FadeThrough
NavigationTransitions.ScaleIn
NavigationTransitions.None

// Use with navigation
navigator.navigate(
    destination = AppDestination.Details("123"),
    transition = NavigationTransitions.SlideVertical
)`

const customTransitionCode = `val customTransition = NavigationTransition(
    enter = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(300)
    ) + fadeIn(),
    exit = slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = tween(300)
    ) + fadeOut()
)`

const sharedElementCode = `// Define shared element configuration
val sharedConfig = SharedElementConfig(
    key = "image_\${item.id}",
    type = SharedElementType.Bounds
)

// Source screen
Image(
    painter = painterResource(item.image),
    contentDescription = null,
    modifier = Modifier
        .sharedElement(
            sharedConfig = sharedConfig,
            navigator = navigator
        )
)

// Destination screen - same key!
Image(
    painter = painterResource(item.image),
    contentDescription = null,
    modifier = Modifier
        .sharedElement(
            sharedConfig = sharedConfig,
            navigator = navigator
        )
)`

const deepLinksCode = `// Register deep link patterns
val graph = navigationGraph("app") {
    deepLink("myapp://profile/{userId}") { args ->
        AppDestination.UserProfile(args["userId"] as String)
    }
    
    deepLink("myapp://settings/{section}") { args ->
        AppDestination.Settings(args["section"] as String)
    }
}

// Handle deep link
navigator.handleDeepLink(
    DeepLink("myapp://profile/user123")
)`

const testingCode = `@Test
fun \`test navigation to profile\`() {
    val fakeNavigator = FakeNavigator()
    val viewModel = MyViewModel(fakeNavigator)
    
    viewModel.onUserClicked("user123")
    
    assertEquals(
        AppDestination.UserProfile("user123"),
        fakeNavigator.lastDestination
    )
}

@Test
fun \`test back navigation\`() {
    val fakeNavigator = FakeNavigator()
    val viewModel = MyViewModel(fakeNavigator)
    
    viewModel.onBackPressed()
    
    assertTrue(fakeNavigator.backPressed)
}`

const androidCode = `// Handle system back button
GraphNavHost(
    graph = mainGraph,
    navigator = navigator,
    enablePredictiveBack = true  // Android 13+ predictive back
)`

export default function GettingStarted() {
  return (
    <article className={styles.gettingStarted}>
      <h1>Getting Started</h1>

      <section>
        <h2 id="installation">Installation</h2>
        <p>Add the Quo Vadis library to your Kotlin Multiplatform project.</p>

        <h3 id="gradle-kotlin">Gradle (Kotlin DSL) - Recommended</h3>
        <CodeBlock code={installationCode} language="kotlin" title="build.gradle.kts" />

        <h3 id="version-catalog">Version Catalog</h3>
        <CodeBlock code={versionCatalogCode} language="bash" title="libs.versions.toml" />
      </section>

      <section>
        <h2 id="basic-setup">Basic Setup (Annotation-based API)</h2>
        <p>The <strong>annotation-based API</strong> is the recommended approach. It uses KSP to generate navigation code automatically, reducing boilerplate by 70%.</p>
        
        <h3 id="step1">Step 1: Define Destinations with Annotations</h3>
        <p>Create type-safe destinations using sealed classes with annotations:</p>
        <CodeBlock code={destinationsCode} language="kotlin" title="AppDestination.kt" />

        <h3 id="step2">Step 2: Define Content Functions</h3>
        <p>Use <code>@Content</code> to wire Composables to destinations:</p>
        <CodeBlock code={contentCode} language="kotlin" title="Screens.kt" />

        <h3 id="step3">Step 3: Use Generated Graph</h3>
        <p>KSP automatically generates a graph builder function:</p>
        <CodeBlock code={graphCode} language="kotlin" title="Navigation.kt" />

        <h3 id="step4">Step 4: Setup NavHost</h3>
        <p>Integrate the navigation host in your app:</p>
        <CodeBlock code={navHostCode} language="kotlin" title="App.kt" />

        <div className={styles.note}>
          <p><strong>What KSP Generates:</strong></p>
          <ul>
            <li><code>AppDestinationRouteInitializer</code> - Automatic route registration</li>
            <li><code>buildAppDestinationGraph()</code> - Complete graph with all destinations wired</li>
            <li><code>typedDestinationXxx()</code> - Type-safe extensions for destinations with @Argument</li>
          </ul>
        </div>
      </section>

      <section>
        <h2 id="manual-dsl">Alternative: Manual DSL Approach</h2>
        <p>For dynamic navigation or fine-grained control, use the manual DSL approach:</p>
        <CodeBlock code={manualDSLCode} language="kotlin" showLineNumbers />
      </section>

      <section>
        <h2 id="navigation-operations">Navigation Operations</h2>
        
        <h3 id="basic-navigation">Basic Navigation</h3>
        <CodeBlock code={basicNavCode} language="kotlin" />

        <h3 id="advanced-navigation">Advanced Navigation</h3>
        <CodeBlock code={advancedNavCode} language="kotlin" />
      </section>

      <section>
        <h2 id="transitions">Transitions</h2>
        <p>Quo Vadis includes several built-in transitions:</p>
        <CodeBlock code={transitionsCode} language="kotlin" />

        <h3 id="custom-transitions">Custom Transitions</h3>
        <CodeBlock code={customTransitionCode} language="kotlin" />
      </section>

      <section>
        <h2 id="shared-elements">Shared Element Transitions</h2>
        <p>Create stunning shared element animations:</p>
        <CodeBlock code={sharedElementCode} language="kotlin" />
      </section>

      <section>
        <h2 id="deep-links">Deep Links</h2>
        <p>Handle deep links across all platforms:</p>
        <CodeBlock code={deepLinksCode} language="kotlin" />
      </section>

      <section>
        <h2 id="testing">Testing</h2>
        <p>Use FakeNavigator for easy unit testing:</p>
        <CodeBlock code={testingCode} language="kotlin" />
      </section>

      <section>
        <h2 id="platform-specific">Platform-Specific Setup</h2>
        
        <h3 id="android">Android</h3>
        <CodeBlock code={androidCode} language="kotlin" />

        <h3 id="ios">iOS</h3>
        <div className={styles.platformNote}>
          <p>Swipe gestures work automatically. Universal links handled via deep link system.</p>
        </div>

        <h3 id="web">Web</h3>
        <div className={styles.platformNote}>
          <p>Browser history integration. URL updates automatically with navigation.</p>
        </div>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features">Explore all features</a> - Deep dive into advanced capabilities</li>
          <li><a href="/demo">Check out the demo</a> - See real-world examples</li>
          <li><a href="/quo-vadis/api/index.html">Browse API reference</a> - Complete API documentation</li>
          <li><a href="https://github.com/jermeyyy/quo-vadis/tree/main/quo-vadis-core/docs" target="_blank" rel="noopener noreferrer">Read detailed docs</a> - Architecture and implementation guides</li>
        </ul>
      </section>

      <div className={styles.note}>
        <p><strong>💡 Pro Tip:</strong> Start with simple navigation and gradually add features like shared elements, 
        deep links, and custom transitions as your app grows. The library is designed 
        to be incrementally adoptable.</p>
      </div>
    </article>
  )
}
