import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './Home.module.css'

const quickstartCode = `[versions]
quoVadis = "0.1.0"
ksp = "2.2.20-1.0.29"

[libraries]
quo-vadis-core = { module = "io.github.jermeyyy:quo-vadis-core", version.ref = "quoVadis" }
quo-vadis-annotations = { module = "io.github.jermeyyy:quo-vadis-annotations", version.ref = "quoVadis" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }`

const step1Code = `// Define your graph
@Graph("app")
sealed class AppDestination : Destination

// Add destinations with routes
@Route("app/home")
data object Home : AppDestination()

@Serializable
data class DetailData(val id: String)

@Route("app/details")
@Argument(DetailData::class)
data class Details(val id: String) : AppDestination(), 
    TypedDestination<DetailData> {
    override val data = DetailData(id)
}`

const step2Code = `@Content(Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
    HomeScreen(
        onNavigateToDetails = { id ->
            // Use generated extension
            navigator.navigateToDetails(id = id)
        }
    )
}

@Content(Details::class)
@Composable
fun DetailsContent(data: DetailData, navigator: Navigator) {
    DetailsScreen(
        itemId = data.id,
        onBack = { navigator.navigateBack() }
    )
}`

const step3Code = `@Composable
fun App() {
    val navigator = rememberNavigator()
    val graph = remember { buildAppDestinationGraph() }
    
    GraphNavHost(
        graph = graph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
}`

const manualDSLCode = `// Define destinations manually
sealed class AppDestination : Destination {
    object Home : AppDestination() {
        override val route = "home"
    }
    
    data class Details(val id: String) : AppDestination() {
        override val route = "details"
        override val arguments = mapOf("id" to id)
    }
}

// Build graph manually
val appGraph = navigationGraph("app") {
    startDestination(AppDestination.Home)
    
    destination(AppDestination.Home) { _, navigator ->
        HomeScreen(
            onNavigateToDetails = { id ->
                navigator.navigate(AppDestination.Details(id))
            }
        )
    }
    
    destination(AppDestination.Details) { dest, navigator ->
        val details = dest as AppDestination.Details
        DetailsScreen(
            itemId = details.id,
            onBack = { navigator.navigateBack() }
        )
    }
}`

export default function Home() {
  return (
    <article className={styles.home}>
      {/* Hero Section */}
      <section className={styles.hero}>
        <h1>Quo Vadis Navigation Library</h1>
        <p className={styles.subtitle}>
          Type-safe, multiplatform navigation for Compose Multiplatform
        </p>
        
        <div className={styles.badges}>
          <a href="https://central.sonatype.com/artifact/io.github.jermeyyy/quo-vadis-core" target="_blank" rel="noopener noreferrer">
            <img src="https://img.shields.io/maven-central/v/io.github.jermeyyy/quo-vadis-core" alt="Maven Central" />
          </a>
          <a href="https://opensource.org/licenses/MIT" target="_blank" rel="noopener noreferrer">
            <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT" />
          </a>
          <img src="https://img.shields.io/github/last-commit/jermeyyy/quo-vadis" alt="Last Commit" />
          <img src="https://img.shields.io/github/stars/jermeyyy/quo-vadis" alt="GitHub Stars" />
        </div>
        
        <div className={styles.badges}>
          <img src="https://img.shields.io/badge/-android-6EDB8D.svg?style=flat&logo=android" alt="Android" />
          <img src="https://img.shields.io/badge/-ios-CDCDCD.svg?style=flat&logo=apple" alt="iOS" />
          <img src="https://img.shields.io/badge/-desktop-4D76CD.svg?style=flat&logo=windows" alt="Desktop" />
          <img src="https://img.shields.io/badge/-js-F8DB5D.svg?style=flat&logo=javascript" alt="JavaScript" />
          <img src="https://img.shields.io/badge/-wasm-624FE8.svg?style=flat" alt="WebAssembly" />
        </div>
        
        <div className={styles.heroButtons}>
          <a href="/getting-started" className={styles.btnPrimary}>Get Started in 10 Minutes</a>
          <a href="https://github.com/jermeyyy/quo-vadis" className={styles.btnSecondary} target="_blank" rel="noopener noreferrer">View on GitHub</a>
        </div>
      </section>

      {/* Overview */}
      <section>
        <h2 id="overview">Overview</h2>
        <p>
          <strong>Quo Vadis</strong> (Latin for "Where are you going?") is a comprehensive, type-safe navigation 
          library for Kotlin Multiplatform and Compose Multiplatform. It provides a clean, intuitive API for 
          managing navigation across Android, iOS, Desktop, and Web platforms with zero string-based routing.
        </p>
      </section>

            {/* Why Quo Vadis */}
      <section>
        <h2 id="why-quo-vadis">Why Quo Vadis?</h2>
        <div className={styles.features}>
          <div className={styles.featureCard}>
            <h4>Zero String Routes</h4>
            <p>Compile-time safe navigation with no runtime crashes. Say goodbye to string-based routing errors and hello to type safety.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>True Multiplatform</h4>
            <p>One codebase, 5+ platforms. Android, iOS, Desktop, JavaScript, and WebAssembly - all from the same navigation code.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>70% Less Code</h4>
            <p>Annotation processor eliminates boilerplate. KSP generates type-safe navigation extensions automatically.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Modern Architecture</h4>
            <p>Built for Compose & MVI. Reactive state management with Flow, modular design, and clean separation of concerns.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Zero Dependencies</h4>
            <p>Self-contained core library with no external navigation dependencies. Smaller app sizes, no conflicts.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Deep Links & URLs</h4>
            <p>URI-based navigation with pattern matching. Handle deep links and universal links elegantly.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Beautiful Transitions</h4>
            <p>Predictive back gestures and shared element transitions. Native-feeling animations on all platforms.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Fully Testable</h4>
            <p>Test navigation without UI. FakeNavigator verifies behavior in milliseconds, not seconds.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Tabbed Navigation</h4>
            <p>Generate complex tab layouts with independent backstacks using simple <code>@TabGraph</code> annotations.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>FlowMVI Ready</h4>
            <p>First-class support for FlowMVI state management, allowing navigation to be treated as a pure side-effect.</p>
          </div>

          <div className={styles.featureCard}>
            <h4>Comprehensive Docs</h4>
            <p>Complete API documentation, guides, and examples. Get productive quickly with excellent documentation.</p>
          </div>
        </div>
      </section>

      {/* Quickstart Section */}
      <section className={styles.quickstart}>
        <h2>Get Started in 10 Minutes</h2>
        <p>
          Add Quo Vadis to your project with just a few lines of code. Copy the configuration below
          and you'll have type-safe navigation running in minutes.
        </p>
        <CodeBlock code={quickstartCode} language="bash" title="libs.versions.toml" />
        
        <div className={styles.callout}>
          <div>
            <strong>Pro Tip:</strong> Use the annotation-based API to write 70% less boilerplate code!
            The KSP processor generates all navigation infrastructure automatically.
          </div>
        </div>
      </section>

      {/* Show Me The Code */}
      <section>
        <h2 id="code-example">Show Me The Code!</h2>
        <p style={{ marginBottom: '2rem' }}>
          Build type-safe navigation in three simple steps:
        </p>
        
        <div className={styles.steps}>
          <div className={styles.step}>
            <h3>Step 1: Define Your Destinations</h3>
            <p>Use annotations to declare your navigation graph and screens:</p>
            <CodeBlock code={step1Code} language="kotlin" title="AppDestination.kt" />
            <div className={styles.stepNote}>
              <strong>What's Generated:</strong> KSP creates type-safe navigation extensions like <code>navigator.navigateToDetails(id)</code>
            </div>
          </div>

          <div className={styles.step}>
            <h3>Step 2: Define Your Screens</h3>
            <p>Connect Composable functions to destinations using @Content:</p>
            <CodeBlock code={step2Code} language="kotlin" title="Screens.kt" />
            <div className={styles.stepNote}>
              <strong>Type Safety:</strong> Arguments are validated at compile time - no runtime errors!
            </div>
          </div>

          <div className={styles.step}>
            <h3>Step 3: Set Up Navigation</h3>
            <p>Use the generated graph builder in your app:</p>
            <CodeBlock code={step3Code} language="kotlin" title="App.kt" />
            <div className={styles.stepNote}>
              <strong>Success!</strong> You now have fully functional, type-safe navigation!
            </div>
          </div>
        </div>
        
        <div className={styles.calloutSuccess}>
          <div>
            <strong>That's it!</strong> KSP generates all route registration, graph builders, and typed navigation extensions automatically. 
            No manual wiring required.
          </div>
        </div>
      </section>

      {/* Manual DSL */}
      <section>
        <h2 id="manual-dsl">Or Use Manual DSL for Full Control</h2>
        <p style={{ marginBottom: '1rem' }}>
          Prefer full control? The manual DSL API is still available for advanced use cases:
        </p>
        <CodeBlock code={manualDSLCode} language="kotlin" />
      </section>

      {/* Resources */}
      <section>
        <h2 id="resources">Resources</h2>
        <ul>
          <li><a href="/getting-started">Getting Started Guide</a> - Installation and basic setup</li>
          <li><a href="/features">Features Documentation</a> - Detailed feature explanations</li>
          <li><a href="/quo-vadis/api/index.html">API Reference</a> - Complete API documentation</li>
          <li><a href="/demo">Demo Application</a> - See all features in action</li>
          <li><a href="https://github.com/jermeyyy/quo-vadis" target="_blank" rel="noopener noreferrer">GitHub Repository</a> - Source code and issues</li>
        </ul>
      </section>
    </article>
  )
}
