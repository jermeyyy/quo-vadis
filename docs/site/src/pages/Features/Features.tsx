import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { PlatformSupportGrid } from '@components/PlatformSupportGrid/PlatformSupportGrid'
import { TransitionTypesDisplay } from '@components/TransitionTypesDisplay/TransitionTypesDisplay'
import styles from './Features.module.css'

const annotationExample = `// 1. Define your graph with annotations
@Graph("shop")
sealed class ShopDestination : Destination

// 2. Add destinations with routes
@Route("shop/products")
data object ProductList : ShopDestination()

// 3. Add typed destinations with arguments
@Serializable
data class ProductData(val productId: String, val mode: String = "view")

@Route("shop/product/detail")
@Argument(ProductData::class)
data class ProductDetail(
    val productId: String,
    val mode: String = "view"
) : ShopDestination(), TypedDestination<ProductData> {
    override val data = ProductData(productId, mode)
}

// 4. Define content with @Content annotation
@Content(ProductList::class)
@Composable
fun ProductListContent(navigator: Navigator) {
    ProductListScreen(
        onProductClick = { id ->
            // Type-safe navigation with generated extension
            navigator.navigateToProductDetail(
                productId = id,
                mode = "view"
            )
        }
    )
}

@Content(ProductDetail::class)
@Composable
fun ProductDetailContent(data: ProductData, navigator: Navigator) {
    ProductDetailScreen(
        productId = data.productId,
        mode = data.mode,
        onBack = { navigator.navigateBack() }
    )
}

// 5. Use generated graph builder
val shopGraph = buildShopDestinationGraph()

// That's it! KSP generates:
// - Route registration (ShopDestinationRouteInitializer)
// - Graph builder (buildShopDestinationGraph())
// - Typed navigation extensions (navigateToProductDetail())`

const typeSafeAnnotationCode = `// Define destinations with annotations
@Graph("feature")
sealed class FeatureDestination : Destination

@Route("feature/list")
data object List : FeatureDestination()

@Serializable
data class DetailData(val id: String, val mode: ViewMode = ViewMode.READ)

@Route("feature/details")
@Argument(DetailData::class)
data class Details(val id: String, val mode: ViewMode = ViewMode.READ) 
    : FeatureDestination(), TypedDestination<DetailData> {
    override val data = DetailData(id, mode)
}

// Navigate with generated extension
navigator.navigateToDetails(id = "123", mode = ViewMode.EDIT)`

const typeSafeManualCode = `// Define destinations manually
sealed class FeatureDestination : Destination {
    object List : FeatureDestination() {
        override val route = "list"
    }
    
    data class Details(
        val id: String,
        val mode: ViewMode = ViewMode.READ
    ) : FeatureDestination() {
        override val route = "details"
        override val arguments = mapOf(
            "id" to id,
            "mode" to mode.name
        )
    }
}

// Navigate with destination instance
navigator.navigate(FeatureDestination.Details("123", ViewMode.EDIT))`

const stackManagementCode = `// Access current NavNode tree state
val navState = navigator.state.value

// Pop from active stack
navigator.navigateBack()

// Clear to specific destination using TreeMutator
val newState = TreeMutator.clearAndPush(navState, HomeDestination)
navigator.updateState(newState)

// Navigate and replace current screen
navigator.navigateAndReplace(NewDestination)

// Clear everything and start fresh
navigator.navigateAndClearAll(StartDestination)

// Switch active tab (for TabNode)
val tabState = TreeMutator.switchActiveTab(navState, newIndex = 1)
navigator.updateState(tabState)`

const deepLinkCode = `val graph = navigationGraph("main") {
    // Simple path
    deepLink("myapp://home") {
        HomeDestination
    }
    
    // Path parameters
    deepLink("myapp://user/{userId}") { args ->
        UserDestination(userId = args["userId"] as String)
    }
    
    // Query parameters
    deepLink("myapp://search?q={query}") { args ->
        SearchDestination(query = args["query"] as String)
    }
    
    // Optional parameters
    deepLink("myapp://settings/{section?}") { args ->
        SettingsDestination(section = args["section"] as? String)
    }
}`

const predictiveBackCode = `NavigationHost(
    navigator = navigator,
    screenRegistry = MainScreenRegistry,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE
)`

const sharedElementCode = `// Define shared element configuration
val imageConfig = SharedElementConfig(
    key = "product_image_\${product.id}",
    type = SharedElementType.Bounds
)

// Source screen
Image(
    modifier = Modifier.sharedElement(
        sharedConfig = imageConfig,
        navigator = navigator
    )
)

// Destination screen (same key!)
Image(
    modifier = Modifier.sharedElement(
        sharedConfig = imageConfig,
        navigator = navigator
    )
)`

const customTransitionCode = `val customTransition = NavigationTransition(
    enter = slideInVertically { it } + fadeIn(),
    exit = slideOutVertically { -it / 2 } + fadeOut(),
    popEnter = slideInVertically { -it / 2 } + fadeIn(),
    popExit = slideOutVertically { it } + fadeOut()
)`

const testingCode = `@Test
fun \`navigates to details when item clicked\`() {
    // Arrange
    val navigator = FakeNavigator()
    val viewModel = ProductListViewModel(navigator)
    
    // Act
    viewModel.onProductClicked("product-123")
    
    // Assert
    assertEquals(
        ProductDestination.Details("product-123"),
        navigator.lastDestination
    )
}

@Test
fun \`clears navigation stack on logout\`() {
    // Arrange
    val navigator = FakeNavigator()
    val viewModel = SettingsViewModel(navigator)
    
    // Act
    viewModel.onLogout()
    
    // Assert
    assertTrue(navigator.stackCleared)
    assertEquals(LoginDestination, navigator.lastDestination)
}`

const modularCode = `// Feature exposes navigation graph
object ProfileFeature {
    fun navigationGraph(): NavigationGraph = navigationGraph("profile") {
        startDestination(ProfileDestination.List)
        
        destination(ProfileDestination.List) { _, nav -> ... }
        destination(ProfileDestination.Details) { _, nav -> ... }
    }
    
    // Public entry points
    val entryPoint: Destination = ProfileDestination.List
}

// Main app integrates feature
val mainGraph = navigationGraph("main") {
    includeGraph(ProfileFeature.navigationGraph())
    
    // Navigate to feature
    navigator.navigate(ProfileFeature.entryPoint)
}`

const koinCode = `// Define content with DI
@Content(HomeDestination::class)
@Composable
fun HomeContent(navigator: Navigator) {
    val viewModel: HomeViewModel = koinInject()
    HomeScreen(viewModel, navigator)
}

// Use NavigationHost with generated registry
NavigationHost(
    navigator = navigator,
    screenRegistry = AppScreenRegistry
)`

const customInjectionCode = `// Create custom destination factory
interface DestinationFactory {
    @Composable
    fun create(destination: Destination, navigator: Navigator)
}

// Use in navigation graph
val graph = navigationGraph("app") {
    factory = myDependencyContainer.destinationFactory
}`

export default function Features() {
  return (
    <article className={styles.features}>
      <h1>Features</h1>
      <p className={styles.intro}>
        Explore the comprehensive feature set that makes Quo Vadis the ideal navigation 
        solution for Kotlin Multiplatform projects.
      </p>

      <section>
        <h2 id="annotation-api">Annotation-Based API with Code Generation</h2>
        <p>
          The modern, recommended approach to building navigation in Quo Vadis. 
          Use simple annotations on your destinations and let KSP generate all the boilerplate 
          code automatically. This approach combines compile-time safety with minimal code.
        </p>

        <h3>Key Features</h3>
        <ul>
          <li><strong>Zero Boilerplate:</strong> No manual graph builders, route registration, or destination factories</li>
          <li><strong>Type-Safe Arguments:</strong> Automatic serialization/deserialization with kotlinx.serialization</li>
          <li><strong>IDE Support:</strong> Full autocompletion and navigation for generated code</li>
          <li><strong>Compile-Time Verification:</strong> Catch errors before runtime</li>
        </ul>

        <h3>The Four Annotations</h3>
        <div className={styles.annotationGrid}>
          <div className={styles.annotationCard}>
            <h4>@Graph</h4>
            <p>Marks a sealed class as a navigation graph. Generates graph builder functions.</p>
          </div>
          <div className={styles.annotationCard}>
            <h4>@Route</h4>
            <p>Specifies the route path. Automatically registers routes with the system.</p>
          </div>
          <div className={styles.annotationCard}>
            <h4>@Argument</h4>
            <p>Defines typed, serializable arguments. Generates typed destination extensions.</p>
          </div>
          <div className={styles.annotationCard}>
            <h4>@Content</h4>
            <p>Connects Composable functions to destinations. Wired automatically in graph.</p>
          </div>
        </div>

        <h3>Complete Example</h3>
        <CodeBlock code={annotationExample} language="kotlin" />

        <h3>What Gets Generated</h3>
        <ul>
          <li><strong>Route Initializers:</strong> Automatic route registration objects</li>
          <li><strong>Graph Builders:</strong> <code>build&#123;GraphName&#125;Graph()</code> functions</li>
          <li><strong>Typed Extensions:</strong> <code>navigateTo&#123;DestinationName&#125;()</code> functions</li>
          <li><strong>Serialization Code:</strong> Argument encoding/decoding logic</li>
        </ul>

        <h3>Benefits Over Manual DSL</h3>
        <ul>
          <li>Write 50-70% less code</li>
          <li>No manual route registration needed</li>
          <li>Automatic argument serialization</li>
          <li>Generated code is type-safe and tested</li>
          <li>Easier to maintain and refactor</li>
        </ul>

        <div className={styles.note}>
          <strong>Note:</strong> The manual DSL approach is still fully supported for advanced use cases. 
          See <Link to="/getting-started#manual-dsl">Getting Started - Alternative Approach</Link>.
        </div>
      </section>

      <section>
        <h2 id="type-safe">Type-Safe Navigation</h2>
        <p>
          Quo Vadis eliminates string-based routing entirely, providing compile-time safety 
          for all navigation operations through two approaches: the annotation-based API (recommended) 
          and the manual DSL (for advanced use cases).
        </p>

        <h3>Benefits</h3>
        <ul>
          <li><strong>Compile-time Safety:</strong> Catch navigation errors at compile time with sealed classes</li>
          <li><strong>IDE Support:</strong> Full autocompletion and refactoring for both approaches</li>
          <li><strong>Type-safe Arguments:</strong> Pass complex data types, not just strings</li>
          <li><strong>Two Approaches:</strong> Use annotations (recommended) or manual DSL (advanced)</li>
        </ul>

        <h3>Annotation-Based Approach (Recommended)</h3>
        <CodeBlock code={typeSafeAnnotationCode} language="kotlin" />

        <h3>Manual DSL Approach</h3>
        <CodeBlock code={typeSafeManualCode} language="kotlin" />

        <p>
          Both approaches provide complete type safety and work seamlessly together. 
          The annotation-based approach requires less code and generates helpful extensions,
          while the manual DSL offers more control for complex scenarios.
        </p>
      </section>

      <section>
        <h2 id="multiplatform">Multiplatform Support</h2>
        <p>
          Truly multiplatform navigation that works identically across all supported platforms.
          Write your navigation logic once and deploy everywhere.
        </p>

        <PlatformSupportGrid variant="cards" />

        <div className={styles.note}>
          <strong>📖 Platform Details:</strong> See the{' '}
          <Link to="/features/multiplatform">Multiplatform page</Link> for 
          platform-specific setup, requirements, and feature matrix.
        </div>
      </section>

      <section>
        <h2 id="backstack">Stack Management (NavNode Tree)</h2>
        <p>
          Quo Vadis uses an immutable NavNode tree to represent navigation state. 
          All state mutations are performed through TreeMutator operations, providing 
          predictable and testable navigation behavior.
        </p>

        <h3>Operations</h3>
        <CodeBlock code={stackManagementCode} language="kotlin" />

        <h3>Use Cases</h3>
        <ul>
          <li>Multi-step wizards with immutable state transformations</li>
          <li>Authentication flows that clear navigation stacks</li>
          <li>Tab-based navigation with independent TabNode children</li>
          <li>State restoration via NavNode serialization</li>
        </ul>
      </section>

      <section>
        <h2 id="deep-links">Deep Link Support</h2>
        <p>
          Comprehensive deep linking system that works across all platforms. Define URL 
          patterns and automatically map them to type-safe destinations.
        </p>

        <h3>Pattern Matching</h3>
        <CodeBlock code={deepLinkCode} language="kotlin" />

        <h3>Platform Integration</h3>
        <ul>
          <li><strong>Android:</strong> Intent filters and App Links</li>
          <li><strong>iOS:</strong> Universal Links and custom URL schemes</li>
          <li><strong>Web:</strong> Direct URL navigation</li>
          <li><strong>Desktop:</strong> Custom protocol handlers</li>
        </ul>
      </section>

      <section>
        <h2 id="predictive-back">Predictive Back Navigation</h2>
        <p>
          Modern, gesture-driven back navigation with smooth animations. Users can preview 
          the previous screen before committing to navigation. Built into NavigationHost 
          via the <code>predictiveBackMode</code> parameter.
        </p>

        <h3>Supported Platforms</h3>
        <ul>
          <li><strong>Android 13+:</strong> System predictive back API</li>
          <li><strong>iOS:</strong> Interactive pop gesture</li>
          <li><strong>Custom Implementations:</strong> Desktop and Web</li>
        </ul>

        <h3>Features</h3>
        <ul>
          <li>Smooth, interruptible animations</li>
          <li>Cross-fade between screens</li>
          <li>Scale and position transitions</li>
          <li>Cancelable gestures</li>
          <li>Cascade pop support for TabNode structures</li>
        </ul>

        <CodeBlock code={predictiveBackCode} language="kotlin" />
      </section>

      <section>
        <h2 id="shared-elements">Shared Element Transitions</h2>
        <p>
          Material Design 3 shared element transitions with full bidirectional support. 
          Create stunning visual continuity between screens.
        </p>

        <h3>Key Features</h3>
        <ul>
          <li><strong>Bidirectional:</strong> Works for both forward and back navigation</li>
          <li><strong>Type-Safe:</strong> Compile-time verified shared element keys</li>
          <li><strong>Flexible:</strong> Bounds, content, or both transitions</li>
          <li><strong>Platform-Aware:</strong> Native behavior on each platform</li>
        </ul>

        <h3>Example</h3>
        <CodeBlock code={sharedElementCode} language="kotlin" />

        <h3>Transition Types</h3>
        <ul>
          <li><strong>Bounds:</strong> Animate position and size</li>
          <li><strong>Content:</strong> Cross-fade content</li>
          <li><strong>Both:</strong> Animate bounds and content together</li>
        </ul>
      </section>

      <section>
        <h2 id="mvi">MVI Architecture Support</h2>
        <p>
          First-class integration with MVI (Model-View-Intent) architecture pattern through 
          the <strong>quo-vadis-core-flow-mvi</strong> module. Navigation is treated as a 
          side effect of business logic, keeping your UI clean and testable.
        </p>

        <h3>Key Benefits</h3>
        <ul>
          <li><strong>Testable:</strong> Test navigation logic without UI using FakeNavigator</li>
          <li><strong>Predictable:</strong> Navigation as pure side effect of state changes</li>
          <li><strong>Centralized:</strong> All navigation logic in one place (Container)</li>
          <li><strong>Decoupled:</strong> UI doesn't need to know about destination types</li>
        </ul>

        <div className={styles.note}>
          <strong>📦 Module:</strong> Add <code>io.github.jermeyyy:quo-vadis-core-flow-mvi</code> to your dependencies.
          See the <Link to="/features/di-integration">FlowMVI & Koin Integration Guide</Link> for complete documentation.
        </div>
      </section>

      <section>
        <h2 id="transitions">Transitions & Animations</h2>
        <p>
          Rich set of built-in transitions and support for custom animations. 
          Create polished, professional navigation experiences.
        </p>

        <h3>Built-in Transitions</h3>
        <TransitionTypesDisplay variant="grid" />

        <div className={styles.note}>
          <strong>📖 Deep Dive:</strong> See the{' '}
          <Link to="/features/transitions">Transitions page</Link> for custom 
          transitions, per-destination configuration, and animation details.
        </div>

        <h3>Custom Transitions</h3>
        <CodeBlock code={customTransitionCode} language="kotlin" />
      </section>

      <section>
        <h2 id="testing">Testing Support</h2>
        <p>
          Built-in testing utilities make it easy to verify navigation behavior without 
          UI testing. Test navigation logic in fast, reliable unit tests.
        </p>

        <h3>FakeNavigator</h3>
        <CodeBlock code={testingCode} language="kotlin" />

        <h3>Verification Methods</h3>
        <ul>
          <li><code>verifyNavigate(destination)</code> - Verify navigation to destination</li>
          <li><code>verifyNavigateBack()</code> - Verify back navigation</li>
          <li><code>clearCalls()</code> - Reset navigation call history</li>
          <li><code>navigationCalls</code> - Access full navigation history</li>
        </ul>

        <div className={styles.note}>
          <strong>📖 More Examples:</strong> See the{' '}
          <Link to="/features/testing">Testing page</Link> for comprehensive 
          FakeNavigator usage, integration testing, and state verification.
        </div>
      </section>

      <section>
        <h2 id="modular">Modular Architecture</h2>
        <p>
          Gray box pattern support enables true modular architecture. Features can 
          define their own navigation graphs and expose public entry points.
        </p>

        <h3>Feature Module Example</h3>
        <CodeBlock code={modularCode} language="kotlin" />

        <h3>Benefits</h3>
        <ul>
          <li>Independent feature development</li>
          <li>Clear module boundaries</li>
          <li>Reusable feature modules</li>
          <li>Easier testing and maintenance</li>
        </ul>
      </section>

      <section>
        <h2 id="di-integration">DI Framework Integration</h2>
        <p>
          Easy integration with popular DI frameworks like Koin, Kodein, and others. 
          Inject dependencies into destination composables seamlessly.
        </p>

        <h3>Koin Example</h3>
        <CodeBlock code={koinCode} language="kotlin" />

        <h3>Custom Injection</h3>
        <CodeBlock code={customInjectionCode} language="kotlin" />
      </section>

      <section>
        <h2 id="performance">Performance</h2>
        <p>
          Optimized for performance with minimal overhead. Lazy initialization, 
          efficient state management, and smart recomposition.
        </p>

        <h3>Optimizations</h3>
        <ul>
          <li><strong>Lazy Loading:</strong> Destinations created only when needed</li>
          <li><strong>Efficient State:</strong> StateFlow with structural sharing</li>
          <li><strong>Smart Recomposition:</strong> Minimal recomposition on navigation</li>
          <li><strong>No Reflection:</strong> Zero runtime reflection overhead</li>
          <li><strong>Small Footprint:</strong> No external dependencies</li>
        </ul>
      </section>

      <section>
        <h2 id="no-dependencies">No External Dependencies</h2>
        <p>
          Quo Vadis is completely self-contained with zero external navigation dependencies. 
          This means:
        </p>
        <ul>
          <li>Smaller app size</li>
          <li>No version conflicts</li>
          <li>No dependency chain issues</li>
          <li>Full control over updates</li>
          <li>Better long-term stability</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/getting-started">Get started</Link> with the quick start guide</li>
          <li><Link to="/demo">See the demo</Link> to explore features in action</li>
          <li><a href="/quo-vadis/api/index.html">Browse API docs</a> for detailed reference</li>
          <li><a href="https://github.com/jermeyyy/quo-vadis/tree/main/quo-vadis-core/docs" target="_blank" rel="noopener noreferrer">Read detailed docs</a> on GitHub</li>
        </ul>
      </section>
    </article>
  )
}
