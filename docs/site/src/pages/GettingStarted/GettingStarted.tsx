import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './GettingStarted.module.css'

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
    id("io.github.jermeyyy.quo-vadis") version "0.3.3"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.jermeyyy:quo-vadis-core:0.3.3")
            implementation("io.github.jermeyyy:quo-vadis-annotations:0.3.3")
        }
    }
}

// Optional: Configure the plugin
quoVadis {
    modulePrefix = "MyApp"  // Generates MyAppNavigationConfig
}`

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
            implementation("io.github.jermeyyy:quo-vadis-core:0.3.3")
            implementation("io.github.jermeyyy:quo-vadis-annotations:0.3.3")
        }
    }
    
    ksp {
        arg("quoVadis.modulePrefix", "MyApp")
    }
}

dependencies {
    add("kspCommonMainMetadata", "io.github.jermeyyy:quo-vadis-ksp:0.3.3")
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

const validationExampleError = `Missing @Screen binding for 'HomeDestination.Feed' in file 'HomeDestination.kt' (line 12). 
Fix: Add a @Composable function annotated with @Screen(HomeDestination.Feed::class)`

export default function GettingStarted() {
  return (
    <article className={styles.gettingStarted}>
      <h1>Getting Started</h1>

      <p>
        This guide will walk you through setting up Quo Vadis in your Kotlin Multiplatform project.
        Follow these steps to get navigation working in minutes.
      </p>

      <section>
        <h2 id="installation">Installation</h2>

        <p>
          Choose one of the following installation methods. The Gradle plugin approach is recommended
          as it handles KSP configuration automatically.
        </p>

        <h3 id="gradle-plugin">Option 1: Using Gradle Plugin (Recommended)</h3>
        <p>
          The Gradle plugin automatically configures KSP and registers generated sources, making setup
          simpler and more reliable.
        </p>
        <CodeBlock code={installationGradlePluginCode} language="kotlin" title="build.gradle.kts" />

        <h3 id="manual-config">Option 2: Manual Configuration</h3>
        <p>
          If you need more control over the build configuration, you can set up KSP manually.
        </p>
        <CodeBlock code={installationManualCode} language="kotlin" title="build.gradle.kts" />
      </section>

      <section>
        <h2 id="define-stack">Define Your Navigation Stack</h2>
        <p>
          Create a sealed class extending <code>NavDestination</code> and annotate it with <code>@Stack</code>.
          Each destination is defined as a nested class with the <code>@Destination</code> annotation.
        </p>
        <CodeBlock code={defineStackCode} language="kotlin" title="HomeDestination.kt" />

        <div className={styles.note}>
          <p><strong>💡 Tip:</strong> Use <code>@Argument</code> on data class properties to enable 
          automatic serialization for deep links. Optional arguments should have default values.</p>
        </div>
      </section>

      <section>
        <h2 id="bind-screens">Bind Screens with @Screen</h2>
        <p>
          Use the <code>@Screen</code> annotation to connect your composables to destinations.
          The navigator is automatically injected, and destination data classes receive the destination instance.
        </p>
        <CodeBlock code={bindScreensCode} language="kotlin" title="Screens.kt" />
      </section>

      <section>
        <h2 id="setup-navigation-host">Setup NavigationHost</h2>
        <p>
          Finally, set up the <code>NavigationHost</code> in your app's entry point. The generated
          configuration combines all registries from your annotated destinations.
        </p>
        <CodeBlock code={setupNavHostCode} language="kotlin" title="App.kt" />
      </section>

      <section>
        <h2 id="generated-code">What Gets Generated</h2>
        <p>
          KSP generates several classes based on your annotations. The prefix is configurable
          via <code>modulePrefix</code> in your Gradle configuration.
        </p>

        <table>
          <thead>
            <tr>
              <th>Generated Class</th>
              <th>Purpose</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>{'{Prefix}'}NavigationConfig</code></td>
              <td>Main configuration with all registries</td>
            </tr>
            <tr>
              <td><code>{'{Prefix}'}ScreenRegistry</code></td>
              <td>Maps destinations to composables</td>
            </tr>
            <tr>
              <td><code>{'{Prefix}'}DeepLinkHandler</code></td>
              <td>Handles URI-based navigation</td>
            </tr>
            <tr>
              <td><code>{'{Prefix}'}ScopeRegistry</code></td>
              <td>Scope membership for containers</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="compile-time-safety">Compile-Time Safety</h2>
        <p>
          Quo Vadis validates your navigation configuration at compile time. If there are
          issues with your annotations, the build will fail with clear error messages
          showing exactly what's wrong and how to fix it.
        </p>
        <p>
          Example error:
        </p>
        <CodeBlock code={validationExampleError} language="text" />
        <p>
          See <Link to="/features/annotation-api#validation">Validation & Error Messages</Link> for
          the complete list of validation rules.
        </p>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <p>
          Now that you have basic navigation working, explore more advanced features:
        </p>
        <ul>
          <li>
            <a href="/features/annotation-api">Annotation API</a> — Full reference for all annotations
            including <code>@Tabs</code>, <code>@Pane</code>, and more
          </li>
          <li>
            <a href="/features/tabbed-navigation">Tabbed Navigation</a> — Set up tab-based navigation
            with independent backstacks
          </li>
          <li>
            <a href="/features/transitions">Transitions</a> — Add custom animations and shared element
            transitions
          </li>
          <li>
            <a href="/features/di-integration">FlowMVI & Koin Integration</a> — Integrate MVI state
            management with navigation-scoped containers
          </li>
        </ul>
      </section>

      <div className={styles.note}>
        <p><strong>💡 Pro Tip:</strong> Start with simple stack navigation and gradually add features 
        like tabs, panes, and transitions as your app grows. The library is designed 
        to be incrementally adoptable.</p>
      </div>
    </article>
  )
}
