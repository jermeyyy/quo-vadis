import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './Demo.module.css'

const bottomNavCode = `val bottomNavGraph = navigationGraph("bottom_nav") {
    tab("home") {
        startDestination(HomeDestination.Main)
        destination(HomeDestination.Main) { _, nav -> HomeScreen(nav) }
        destination(HomeDestination.Details) { _, nav -> DetailsScreen(nav) }
    }
    
    tab("profile") {
        startDestination(ProfileDestination.Main)
        destination(ProfileDestination.Main) { _, nav -> ProfileScreen(nav) }
    }
    
    tab("settings") {
        startDestination(SettingsDestination.Main)
        destination(SettingsDestination.Main) { _, nav -> SettingsScreen(nav) }
    }
}`

const masterDetailCode = `// List screen
LazyColumn {
    items(products) { product ->
        ProductCard(
            product = product,
            onClick = { 
                navigator.navigate(ProductDestination.Details(product.id)) 
            },
            imageModifier = Modifier.sharedElement(
                sharedConfig = SharedElementConfig(
                    key = "product_\${product.id}",
                    type = SharedElementType.Bounds
                ),
                navigator = navigator
            )
        )
    }
}`

const multiStepCode = `sealed class ProcessStep : Destination {
    object Step1 : ProcessStep() { override val route = "step1" }
    object Step2 : ProcessStep() { override val route = "step2" }
    object Step3 : ProcessStep() { override val route = "step3" }
    object Complete : ProcessStep() { override val route = "complete" }
}

// Navigate forward through steps
fun onNext(currentStep: ProcessStep) {
    when (currentStep) {
        is ProcessStep.Step1 -> navigator.navigate(ProcessStep.Step2)
        is ProcessStep.Step2 -> navigator.navigate(ProcessStep.Step3)
        is ProcessStep.Step3 -> navigator.navigateAndClearTo(
            destination = ProcessStep.Complete,
            clearRoute = "step1",
            inclusive = false
        )
    }
}`

const cloneRunCode = `# Clone the repository
git clone https://github.com/jermeyyy/quo-vadis.git
cd quo-vadis

# Run on Android
./gradlew :composeApp:installDebug

# Run on iOS (macOS only)
open iosApp/iosApp.xcodeproj

# Run on Desktop
./gradlew :composeApp:run

# Run on Web (JavaScript)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Run on Web (WebAssembly)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun`

const mainAppCode = `@Composable
fun DemoApp() {
    val navigator = rememberNavigator(startDestination = MainDestination.Home)
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = navigator.currentDestination.value?.route,
                onTabSelected = { destination ->
                    navigator.navigate(destination)
                }
            )
        }
    ) { paddingValues ->
        NavigationHost(
            navigator = navigator,
            screenRegistry = MainDestinationScreenRegistry,
            modifier = Modifier.padding(paddingValues),
            defaultTransition = NavigationTransitions.SlideHorizontal,
            predictiveBackMode = PredictiveBackMode.FULL_CASCADE
        )
    }
}`

const featureModuleCode = `// Using annotation-based API
@Graph("products")
sealed class ProductDestination : Destination

@Route("products/list")
data object ProductList : ProductDestination()

@Serializable
data class ProductDetailsData(val productId: String)

@Route("products/details")
@Argument(ProductDetailsData::class)
data class ProductDetails(val productId: String) 
    : ProductDestination(), TypedDestination<ProductDetailsData> {
    override val data = ProductDetailsData(productId)
}

@Content(ProductList::class)
@Composable
fun ProductListContent(navigator: Navigator) {
    ProductListScreen(
        onProductClick = { id ->
            navigator.navigateToProductDetails(productId = id)
        }
    )
}

@Content(ProductDetails::class)
@Composable
fun ProductDetailsContent(data: ProductDetailsData, navigator: Navigator) {
    ProductDetailScreen(
        productId = data.productId,
        onBack = { navigator.navigateBack() }
    )
}

// Use: ProductDestinationScreenRegistry`

export default function Demo() {
  return (
    <article className={styles.demo}>
      <h1>Demo Application</h1>
      <p className={styles.intro}>
        The Quo Vadis demo application showcases all navigation features and patterns in a 
        comprehensive, real-world example. Explore the various navigation techniques and 
        see how they work together to create a smooth user experience.
      </p>

      <section>
        <h2 id="demo-features">Demo Features</h2>
        <div className={styles.featuresGrid}>
          <div className={styles.featureCard}>
            <h3>Bottom Navigation</h3>
            <p>Tab-based navigation between main sections with independent navigation stacks via TabNode.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Drawer Navigation</h3>
            <p>Side drawer with quick access to different features and settings.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Master-Detail Flow</h3>
            <p>Classic list-to-detail pattern with shared element transitions.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Multi-Step Process</h3>
            <p>Wizard-style flow with TreeMutator-based stack management and validation.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Nested Navigation</h3>
            <p>Multiple navigation levels with tab containers and hierarchical screens.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Transitions Showcase</h3>
            <p>Demonstration of all built-in transitions and custom animations.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Shared Elements</h3>
            <p>Beautiful shared element transitions between screens with bidirectional support.</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Predictive Back</h3>
            <p>Gesture-driven back navigation with preview animations (Android 13+ / iOS).</p>
          </div>

          <div className={styles.featureCard}>
            <h3>Deep Links</h3>
            <p>Navigate directly to any screen via URL or custom scheme.</p>
          </div>
        </div>
      </section>

      <section>
        <h2 id="screenshots">Screenshots</h2>
        <p>Screenshots from the demo application running on Android:</p>
        
        <div className={styles.screenshots}>
          <div className={styles.screenshotCard}>
            <img src={`${import.meta.env.BASE_URL}images/main_screen.png`} alt="Main navigation screen with bottom tabs" />
            <div className={styles.caption}>
              <strong>Main Screen</strong><br />
              Bottom navigation with multiple tabs and drawer access
            </div>
          </div>

          <div className={styles.screenshotCard}>
            <img src={`${import.meta.env.BASE_URL}images/master_detail_pattern.png`} alt="Master-detail list view" />
            <div className={styles.caption}>
              <strong>Master-Detail Pattern</strong><br />
              List view with cards ready for navigation to detail screens
            </div>
          </div>

          <div className={styles.screenshotCard}>
            <img src={`${import.meta.env.BASE_URL}images/modal_bottom_sheet.png`} alt="Modal bottom sheet navigation" />
            <div className={styles.caption}>
              <strong>Modal Bottom Sheet</strong><br />
              Bottom sheet navigation with smooth transitions
            </div>
          </div>

          <div className={styles.screenshotCard}>
            <img src={`${import.meta.env.BASE_URL}images/multistep_process_pattern.png`} alt="Multi-step process flow" />
            <div className={styles.caption}>
              <strong>Multi-Step Process</strong><br />
              Wizard-style flow with step indicators and navigation controls
            </div>
          </div>

          <div className={styles.screenshotCard}>
            <img src={`${import.meta.env.BASE_URL}images/predictive_back.png`} alt="Predictive back navigation" />
            <div className={styles.caption}>
              <strong>Predictive Back Navigation</strong><br />
              Interactive back gesture with preview animation
            </div>
          </div>
        </div>
      </section>

      <section>
        <h2 id="navigation-patterns">Navigation Patterns Demonstrated</h2>
        
        <h3 id="bottom-nav-pattern">1. Bottom Navigation Pattern</h3>
        <p>
          The demo uses bottom navigation for main app sections (Home, Profile, Settings). 
          Each tab is represented as a child of a TabNode in the NavNode tree, maintaining 
          independent navigation stacks within each tab.
        </p>
        <CodeBlock code={bottomNavCode} language="kotlin" />

        <h3 id="master-detail-flow">2. Master-Detail Flow</h3>
        <p>
          Classic list-to-detail pattern enhanced with shared element transitions. 
          Images and text smoothly animate from the list to the detail screen.
        </p>
        <CodeBlock code={masterDetailCode} language="kotlin" />

        <h3 id="multi-step-process">3. Multi-Step Process</h3>
        <p>
          Wizard-style flows are handled elegantly with TreeMutator operations. 
          The demo includes validation, progress tracking, and the ability to jump 
          to specific steps using immutable state transformations.
        </p>
        <CodeBlock code={multiStepCode} language="kotlin" />

        <h3 id="nested-navigation">4. Nested Navigation</h3>
        <p>
          The demo shows how to implement tabs within tabs using nested TabNode and 
          StackNode structures in the NavNode tree. Each level of navigation is independent 
          and maintains its own state.
        </p>

        <h3 id="transition-showcase">5. Transition Showcase</h3>
        <p>A dedicated screen demonstrates all available transitions:</p>
        <ul>
          <li><strong>Slide Horizontal:</strong> Standard left/right slide</li>
          <li><strong>Slide Vertical:</strong> Bottom-up modal style</li>
          <li><strong>Fade:</strong> Simple cross-fade</li>
          <li><strong>Fade Through:</strong> Material Design fade through</li>
          <li><strong>Scale In:</strong> Zoom-in effect</li>
          <li><strong>Custom:</strong> User-defined transitions</li>
        </ul>
      </section>

      <section>
        <h2 id="platform-features">Platform-Specific Features</h2>
        
        <div className={styles.platformGrid}>
          <div className={styles.platformCard}>
            <h3>Android</h3>
            <ul>
              <li><strong>System Back Button:</strong> Integrated with Android's back button</li>
              <li><strong>Predictive Back (Android 13+):</strong> Gesture preview animations</li>
              <li><strong>Deep Links:</strong> Handle app links and custom schemes</li>
              <li><strong>State Restoration:</strong> Survive configuration changes and process death</li>
            </ul>
          </div>

          <div className={styles.platformCard}>
            <h3>iOS</h3>
            <ul>
              <li><strong>Swipe Gestures:</strong> Native-feeling swipe-to-go-back</li>
              <li><strong>Predictive Back:</strong> Interactive pop gesture with preview</li>
              <li><strong>Universal Links:</strong> Deep linking via HTTPS URLs</li>
              <li><strong>Navigation Bar:</strong> Automatic back button integration</li>
            </ul>
          </div>

          <div className={styles.platformCard}>
            <h3>Web</h3>
            <ul>
              <li><strong>Browser History:</strong> Forward/back buttons work naturally</li>
              <li><strong>URL Routing:</strong> Deep linkable URLs for every screen</li>
              <li><strong>Bookmarking:</strong> Users can bookmark specific screens</li>
            </ul>
          </div>

          <div className={styles.platformCard}>
            <h3>Desktop</h3>
            <ul>
              <li><strong>Keyboard Shortcuts:</strong> Alt+Left/Right for navigation</li>
              <li><strong>Mouse Buttons:</strong> Back/forward mouse buttons</li>
              <li><strong>Window State:</strong> Navigation state persists across sessions</li>
            </ul>
          </div>
        </div>
      </section>

      <section>
        <h2 id="running-demo">Running the Demo</h2>
        
        <h3>Prerequisites</h3>
        <ul>
          <li>Kotlin 2.2.20+</li>
          <li>Android Studio or IntelliJ IDEA</li>
          <li>For iOS: macOS with Xcode installed</li>
        </ul>

        <h3>Clone and Run</h3>
        <CodeBlock code={cloneRunCode} language="bash" />

        <div className={styles.note}>
          <p><strong>💡 Tip:</strong> The demo application source code is fully documented and serves as a 
            comprehensive example of best practices. Check the 
            <code> composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/</code> 
            directory for implementation details.
          </p>
        </div>
      </section>

      <section>
        <h2 id="code-examples">Code Examples from Demo</h2>
        
        <h3>Main Application Structure</h3>
        <CodeBlock code={mainAppCode} language="kotlin" />

        <h3>Feature Module Example</h3>
        <CodeBlock code={featureModuleCode} language="kotlin" />
      </section>

      <section>
        <h2 id="explore-more">Explore More</h2>
        <ul>
          <li><a href="https://github.com/jermeyyy/quo-vadis/tree/main/composeApp" target="_blank" rel="noopener noreferrer">Demo Source Code</a> - Full implementation on GitHub</li>
          <li><a href="/getting-started">Getting Started</a> - Build your own navigation</li>
          <li><a href="/features">Features</a> - Learn about all capabilities</li>
          <li><a href="/quo-vadis/api/index.html">API Reference</a> - Detailed API documentation</li>
        </ul>
      </section>
    </article>
  )
}
