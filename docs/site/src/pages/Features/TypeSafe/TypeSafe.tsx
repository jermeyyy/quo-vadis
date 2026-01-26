import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const navigatorInterfaceCode = `@Stable
interface Navigator : BackPressHandler {
    val state: StateFlow<NavNode>
    val currentDestination: StateFlow<NavDestination?>
    val previousDestination: StateFlow<NavDestination?>
    val canNavigateBack: StateFlow<Boolean>
    val config: NavigationConfig

    fun navigate(destination: NavDestination, transition: NavigationTransition? = null)
    fun navigateBack(): Boolean
    fun navigateAndClearTo(destination: NavDestination, clearRoute: String?, inclusive: Boolean)
    fun navigateAndReplace(destination: NavDestination, transition: NavigationTransition?)
    fun navigateAndClearAll(destination: NavDestination)
    fun handleDeepLink(uri: String): Boolean
}`

const forwardNavigationCode = `// Simple navigation
navigator.navigate(HomeDestination.Article(articleId = "123"))

// With explicit transition
navigator.navigate(
    destination = HomeDestination.Article(articleId = "123"),
    transition = NavigationTransitions.SlideHorizontal
)`

const backNavigationCode = `val didNavigate = navigator.navigateBack()`

const clearReplaceCode = `// Navigate and clear back to specific route
navigator.navigateAndClearTo(
    destination = SettingsDestination.Main,
    clearRoute = "home/feed",
    inclusive = false
)

// Replace current screen
navigator.navigateAndReplace(
    destination = ProfileDestination.EditProfile,
    transition = NavigationTransitions.Fade
)

// Clear entire backstack
navigator.navigateAndClearAll(AuthDestination.Login)`

const navigationResultsCode = `// Define result-returning destination
data class SelectedItem(val id: String, val name: String)

@Destination(route = "picker/items")
data object ItemPicker : PickerDestination(), ReturnsResult<SelectedItem>

// Navigate for result (suspends until result)
val result: SelectedItem? = navigator.navigateForResult(ItemPicker)

// Return result and navigate back
navigator.navigateBackWithResult(SelectedItem(id = "1", name = "Item"))`

const paneNavigationCode = `// Cast to PaneNavigator for pane operations
val paneNavigator = navigator.asPaneNavigator()

// Navigate within a specific pane
paneNavigator?.navigateToPane(
    MessagesPane.ConversationDetail(conversationId),
    role = PaneRole.Supporting
)

// Check pane availability
if (paneNavigator?.isPaneAvailable(PaneRole.Extra) == true) {
    // Show extra content
}`

const stateObservationCode = `@Composable
fun NavigationAwareContent(navigator: Navigator) {
    val currentDest by navigator.currentDestination.collectAsState()
    val canBack by navigator.canNavigateBack.collectAsState()
    
    // UI based on navigation state
    IconButton(
        onClick = { navigator.navigateBack() },
        enabled = canBack
    ) {
        Icon(Icons.Default.ArrowBack, "Back")
    }
}`

const treeNavigatorSetupCode = `val navigator = TreeNavigator(
    config = GeneratedNavigationConfig,  // Combined config
    initialState = config.buildNavNode(MainTabs::class, null)!!
)`

const multiModuleConfigCode = `val combinedConfig = AppNavigationConfig + 
    Feature1NavigationConfig + 
    Feature2NavigationConfig

val navigator = TreeNavigator(
    config = combinedConfig,
    initialState = combinedConfig.buildNavNode(MainTabs::class, null)!!
)`

const koinIntegrationCode = `// DI Setup
val navigationModule = module {
    single<NavigationConfig> {
        ComposeAppNavigationConfig + Feature1NavigationConfig
    }
    
    single<Navigator> {
        val config = get<NavigationConfig>()
        val initialState = config.buildNavNode(MainTabs::class, null)!!
        TreeNavigator(config = config, initialState = initialState)
    }
}

// In Composables
@Screen(MainTabs.HomeTab::class)
@Composable
fun HomeScreen(navigator: Navigator = koinInject()) {
    // Use navigator
}`

export default function TypeSafe() {
  return (
    <article className={styles.features}>
      <h1>Type-Safe Navigation</h1>
      <p className={styles.intro}>
        Quo Vadis provides a powerful <code>Navigator</code> interface that eliminates string-based routing entirely. 
        All navigation operations are type-safe at compile time, with full IDE support for autocompletion 
        and refactoring.
      </p>

      <div className={styles.highlights}>
        <ul>
          <li><strong>Compile-time Safety:</strong> All destinations are strongly typed - no runtime route matching errors</li>
          <li><strong>Observable State:</strong> Reactive <code>StateFlow</code> properties for current destination, back stack, and more</li>
          <li><strong>Rich Operations:</strong> Navigate, replace, clear stack, and handle results with type safety</li>
          <li><strong>Pane Support:</strong> Adaptive multi-pane navigation for tablets and large screens</li>
        </ul>
      </div>

      <section>
        <h2 id="navigator-interface">Navigator Interface</h2>
        <p>
          The <code>Navigator</code> interface is the central API for all navigation operations. It exposes 
          reactive state properties and type-safe navigation methods:
        </p>
        <CodeBlock code={navigatorInterfaceCode} language="kotlin" />
      </section>

      <section>
        <h2 id="state-properties">State Properties</h2>
        <p>
          The Navigator exposes several observable properties that allow your UI to react to navigation changes:
        </p>
        <table>
          <thead>
            <tr>
              <th>Property</th>
              <th>Type</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>state</code></td>
              <td><code>StateFlow&lt;NavNode&gt;</code></td>
              <td>Complete <Link to="/features/core-concepts#navnode-tree">navigation tree</Link> (single source of truth)</td>
            </tr>
            <tr>
              <td><code>currentDestination</code></td>
              <td><code>StateFlow&lt;NavDestination?&gt;</code></td>
              <td>Active leaf destination</td>
            </tr>
            <tr>
              <td><code>previousDestination</code></td>
              <td><code>StateFlow&lt;NavDestination?&gt;</code></td>
              <td>Destination before current</td>
            </tr>
            <tr>
              <td><code>canNavigateBack</code></td>
              <td><code>StateFlow&lt;Boolean&gt;</code></td>
              <td>Whether back navigation is possible</td>
            </tr>
            <tr>
              <td><code>config</code></td>
              <td><code>NavigationConfig</code></td>
              <td>Navigation configuration</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="navigation-operations">Navigation Operations</h2>
        
        <h3>Forward Navigation</h3>
        <p>
          Navigate to a destination using the type-safe <code>navigate()</code> method. 
          Optionally specify a transition animation:
        </p>
        <CodeBlock code={forwardNavigationCode} language="kotlin" />

        <h3>Back Navigation</h3>
        <p>
          Navigate back to the previous screen. Returns <code>true</code> if navigation occurred:
        </p>
        <CodeBlock code={backNavigationCode} language="kotlin" />

        <h3>Clear and Replace</h3>
        <p>
          Advanced navigation operations for managing the back stack:
        </p>
        <CodeBlock code={clearReplaceCode} language="kotlin" />
      </section>

      <section>
        <h2 id="navigation-results">Navigation with Results</h2>
        <p>
          Quo Vadis supports type-safe result passing between screens using the <code>ReturnsResult</code> interface. 
          The calling screen suspends until a result is returned:
        </p>
        <CodeBlock code={navigationResultsCode} language="kotlin" />
      </section>

      <section>
        <h2 id="pane-navigation">Pane Navigation</h2>
        <p>
          For adaptive layouts on tablets and large screens, cast the Navigator to <code>PaneNavigator</code> 
          to access pane-specific operations:
        </p>
        <CodeBlock code={paneNavigationCode} language="kotlin" />
      </section>

      <section>
        <h2 id="state-observation">State Observation in Compose</h2>
        <p>
          Use <code>collectAsState()</code> to observe navigation state changes and update your UI reactively:
        </p>
        <CodeBlock code={stateObservationCode} language="kotlin" />
      </section>

      <section>
        <h2 id="tree-navigator-setup">TreeNavigator Setup</h2>
        <p>
          <code>TreeNavigator</code> is the concrete implementation of the Navigator interface. 
          Create it with a configuration and initial state:
        </p>
        <CodeBlock code={treeNavigatorSetupCode} language="kotlin" />
      </section>

      <section>
        <h2 id="multi-module-config">Multi-Module Configuration</h2>
        <p>
          In multi-module projects, combine navigation configs from different features using the <code>+</code> operator:
        </p>
        <CodeBlock code={multiModuleConfigCode} language="kotlin" />
      </section>

      <section>
        <h2 id="koin-integration">Koin Integration</h2>
        <p>
          Set up the Navigator as a singleton in your Koin module for dependency injection:
        </p>
        <CodeBlock code={koinIntegrationCode} language="kotlin" />
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/features/annotation-api">Annotation-Based API</Link> - Learn about <code>@Destination</code>, <code>@Stack</code>, and code generation</li>
          <li><Link to="/features/tree-architecture">Tree Architecture</Link> - Understand the navigation tree structure</li>
          <li><Link to="/features/adaptive-navigation">Adaptive Navigation</Link> - Build responsive layouts with panes</li>
          <li><Link to="/getting-started">Getting Started</Link> - Quick start guide</li>
        </ul>
      </section>
    </article>
  )
}
