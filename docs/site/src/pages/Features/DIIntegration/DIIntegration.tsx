import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

// Styled heading components for Do/Don't sections
const DoHeading = () => (
  <h3><span className={`${styles.statusBadge} ${styles.statusFull}`}>DO</span></h3>
)

const DontHeading = () => (
  <h3><span className={`${styles.statusBadge} ${styles.statusNo}`}>DON'T</span></h3>
)

// Dependencies
const dependencyCode = `dependencies {
    implementation("io.github.jermeyyy:quo-vadis-core-flow-mvi:0.3.3")
}

// Transitively includes:
// - FlowMVI (pro.respawn.flowmvi)
// - Koin (io.insert-koin)
// - quo-vadis-core`

// MVI Contract
const contractCode = `// State - What the UI displays
sealed interface ProfileState : MVIState {
    data object Loading : ProfileState
    data class Content(val user: UserData) : ProfileState
    data class Error(val message: String) : ProfileState
}

// Intent - What the user wants to do
sealed interface ProfileIntent : MVIIntent {
    data object LoadProfile : ProfileIntent
    data object NavigateToSettings : ProfileIntent
    data object NavigateBack : ProfileIntent
}

// Action - Side effects (toasts, etc.)
sealed interface ProfileAction : MVIAction {
    data class ShowToast(val message: String) : ProfileAction
}`

// Navigator Registration
const navigatorRegistrationCode = `val navigationModule = module {
    single<NavigationConfig> {
        ComposeAppNavigationConfig +
            Feature1NavigationConfig +
            Feature2NavigationConfig
    }
    
    single<Navigator> {
        val config = get<NavigationConfig>()
        val initialState = config.buildNavNode(
            destinationClass = MainTabs::class,
            parentKey = null
        ) ?: error("No container registered for MainTabs")
        
        TreeNavigator(
            config = config,
            initialState = initialState
        )
    }
}`

// Screen Container Code
const screenContainerCode = `class ProfileContainer(
    scope: NavigationContainerScope,
    private val repository: ProfileRepository
) : NavigationContainer<ProfileState, ProfileIntent, ProfileAction>(scope) {

    override val store = store(initial = ProfileState.Loading) {
        init { intent(ProfileIntent.LoadProfile) }
        
        reduce { intent ->
            when (intent) {
                is ProfileIntent.LoadProfile -> handleLoadProfile()
                is ProfileIntent.NavigateToSettings -> {
                    navigator.navigate(MainTabs.SettingsTab.Main)
                }
                is ProfileIntent.NavigateBack -> navigator.navigateBack()
            }
        }
        
        recover { exception ->
            action(ProfileAction.ShowToast(exception.message ?: "Error"))
            updateState { ProfileState.Error(exception.message ?: "Error") }
            null
        }
    }

    private suspend fun Ctx.handleLoadProfile() {
        updateState { ProfileState.Loading }
        val user = repository.getUser()
        updateState { ProfileState.Content(user = user) }
    }
}`

// Koin Module Registration
const koinModuleCode = `val profileModule = module {
    single { ProfileRepository() }
    
    // Screen-scoped container
    navigationContainer<ProfileContainer> { scope ->
        ProfileContainer(scope, scope.get())
    }
}

val tabsDemoModule = module {
    // Container-scoped shared state
    sharedNavigationContainer<DemoTabsContainer> { scope ->
        DemoTabsContainer(scope)
    }
}`

// rememberContainer Usage
const rememberContainerCode = `@Screen(MainTabs.ProfileTab::class)
@Composable
fun ProfileScreen() {
    val store = rememberContainer<ProfileContainer, ProfileState, ProfileIntent, ProfileAction>()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val state by store.subscribe { action ->
        scope.launch {
            when (action) {
                is ProfileAction.ShowToast -> {
                    snackbarHostState.showSnackbar(action.message)
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        when (state) {
            is ProfileState.Loading -> LoadingContent()
            is ProfileState.Content -> ProfileContent(
                state = state as ProfileState.Content,
                onIntent = store::intent
            )
            is ProfileState.Error -> ErrorContent(
                message = (state as ProfileState.Error).message,
                onRetry = { store.intent(ProfileIntent.LoadProfile) }
            )
        }
    }
}`

// Shared Container Code
const sharedContainerCode = `data class DemoTabsState(
    val totalItemsViewed: Int = 0,
    val favoriteItems: List<String> = emptyList()
) : MVIState

sealed interface DemoTabsIntent : MVIIntent {
    data object IncrementViewed : DemoTabsIntent
    data class AddFavorite(val itemId: String) : DemoTabsIntent
}

sealed interface DemoTabsAction : MVIAction

class DemoTabsContainer(
    scope: SharedContainerScope
) : SharedNavigationContainer<DemoTabsState, DemoTabsIntent, DemoTabsAction>(scope) {

    override val store = store(DemoTabsState()) {
        reduce { intent ->
            when (intent) {
                is DemoTabsIntent.IncrementViewed -> updateState {
                    copy(totalItemsViewed = totalItemsViewed + 1)
                }
                is DemoTabsIntent.AddFavorite -> updateState {
                    copy(favoriteItems = favoriteItems + intent.itemId)
                }
            }
        }
    }
}

// Provide shared store via CompositionLocal
val LocalDemoTabsStore = staticCompositionLocalOf<Store<DemoTabsState, DemoTabsIntent, DemoTabsAction>> {
    throw IllegalStateException("No DemoTabsStore provided")
}`

// rememberSharedContainer Usage
const rememberSharedContainerCode = `@TabsContainer(DemoTabs::class)
@Composable
fun DemoTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    val sharedStore = rememberSharedContainer<DemoTabsContainer, DemoTabsState, DemoTabsIntent, DemoTabsAction>()
    val state by sharedStore.subscribe()

    CompositionLocalProvider(LocalDemoTabsStore provides sharedStore) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Items viewed: \${state.totalItemsViewed}") })
            }
        ) { padding ->
            Box(Modifier.padding(padding)) { content() }
        }
    }
}

// Child screens access shared state via CompositionLocal
@Screen(DemoTabs.Music::class)
@Composable
fun MusicScreen() {
    val sharedStore = LocalDemoTabsStore.current
    
    LaunchedEffect(Unit) {
        sharedStore.intent(DemoTabsIntent.IncrementViewed)
    }
    
    // Screen content...
}`

// Lifecycle Diagram Styles
const lifecycleStyles = {
  container: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    gap: '0',
    padding: '2rem 1rem',
    fontFamily: 'var(--font-mono)',
    fontSize: '0.85rem',
  },
  box: {
    width: '100%',
    maxWidth: '340px',
    background: 'var(--color-bg-elevated)',
    borderRadius: '10px',
    padding: '1rem 1.25rem',
    textAlign: 'center' as const,
    border: '2px solid',
    position: 'relative' as const,
  },
  boxTitle: {
    fontWeight: '600' as const,
    fontSize: '0.95rem',
  },
  subItems: {
    marginTop: '0.5rem',
    fontSize: '0.75rem',
    opacity: 0.8,
    display: 'flex',
    flexWrap: 'wrap' as const,
    justifyContent: 'center',
    gap: '0.5rem',
  },
  subItem: {
    background: 'rgba(0, 0, 0, 0.1)',
    padding: '0.25rem 0.5rem',
    borderRadius: '4px',
  },
  creation: {
    background: 'rgba(34, 197, 94, 0.12)',
    borderColor: 'rgba(34, 197, 94, 0.5)',
  },
  setup: {
    background: 'rgba(59, 130, 246, 0.08)',
    borderColor: 'rgba(59, 130, 246, 0.4)',
  },
  active: {
    background: 'rgba(139, 92, 246, 0.1)',
    borderColor: 'rgba(139, 92, 246, 0.5)',
  },
  cleanup: {
    background: 'rgba(239, 68, 68, 0.1)',
    borderColor: 'rgba(239, 68, 68, 0.4)',
  },
}

const LifecycleArrow = () => (
  <div style={{
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '0.25rem 0',
  }}>
    <div style={{
      width: '2px',
      height: '24px',
      background: 'var(--color-text-muted, #9ca3af)',
    }} />
    <div style={{
      width: '0',
      height: '0',
      borderLeft: '6px solid transparent',
      borderRight: '6px solid transparent',
      borderTop: '8px solid var(--color-text-muted, #9ca3af)',
    }} />
  </div>
)

const LifecycleDiagram = () => (
  <div style={lifecycleStyles.container}>
    <div style={{ ...lifecycleStyles.box, ...lifecycleStyles.creation }}>
      <div style={lifecycleStyles.boxTitle}>Screen enters composition</div>
    </div>
    
    <LifecycleArrow />
    
    <div style={{ ...lifecycleStyles.box, ...lifecycleStyles.setup }}>
      <div style={lifecycleStyles.boxTitle}>rememberContainer called</div>
    </div>
    
    <LifecycleArrow />
    
    <div style={{ ...lifecycleStyles.box, ...lifecycleStyles.setup }}>
      <div style={lifecycleStyles.boxTitle}>Koin scope created</div>
      <div style={lifecycleStyles.subItems}>
        <span style={lifecycleStyles.subItem}>CoroutineScope</span>
        <span style={lifecycleStyles.subItem}>NavigationContainerScope</span>
        <span style={lifecycleStyles.subItem}>Container</span>
        <span style={lifecycleStyles.subItem}>Store</span>
      </div>
    </div>
    
    <LifecycleArrow />
    
    <div style={{ ...lifecycleStyles.box, ...lifecycleStyles.active }}>
      <div style={lifecycleStyles.boxTitle}>Screen is active</div>
    </div>
    
    <LifecycleArrow />
    
    <div style={{ ...lifecycleStyles.box, ...lifecycleStyles.cleanup }}>
      <div style={lifecycleStyles.boxTitle}>Screen removed from navigation</div>
    </div>
    
    <LifecycleArrow />
    
    <div style={{ ...lifecycleStyles.box, ...lifecycleStyles.cleanup }}>
      <div style={lifecycleStyles.boxTitle}>ScreenNode.onDestroy callback</div>
      <div style={lifecycleStyles.subItems}>
        <span style={lifecycleStyles.subItem}>Koin scope closed</span>
        <span style={lifecycleStyles.subItem}>CoroutineScope cancelled</span>
      </div>
    </div>
  </div>
)

// Multi-Module Code
const multiModuleCode = `// feature1/DI.kt
val feature1Module = module {
    navigationContainer<ResultDemoContainer> { scope ->
        ResultDemoContainer(scope)
    }
    navigationContainer<ItemPickerContainer> { scope ->
        ItemPickerContainer(scope)
    }
}

// feature2/DI.kt  
val feature2Module = module {
    navigationContainer<AnalyticsContainer> { scope ->
        AnalyticsContainer(scope, scope.get())
    }
}

// app/DI.kt
fun initKoin() {
    startKoin {
        modules(
            navigationModule,
            feature1Module,
            feature2Module,
            profileModule,
            tabsDemoModule
        )
    }
}`

// Common Patterns
const navigationFromContainerCode = `reduce { intent ->
    when (intent) {
        is ProfileIntent.NavigateToSettings -> {
            // Use navigator from scope
            navigator.navigate(SettingsDestination.Main)
        }
        is ProfileIntent.NavigateBack -> {
            navigator.navigateBack()
        }
    }
}`

const conditionalNavigationCode = `reduce { intent ->
    when (intent) {
        is ProfileIntent.SaveAndNavigate -> {
            try {
                repository.save(currentState.data)
                action(ProfileAction.ShowToast("Saved"))
                navigator.navigateBack()
            } catch (e: Exception) {
                action(ProfileAction.ShowToast("Save failed"))
                // Stay on screen
            }
        }
    }
}`

const sharingStateBetweenTabsCode = `// In tab wrapper - increment view count
@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    val store = rememberSharedContainer<MainTabsContainer, ...>()
    CompositionLocalProvider(LocalMainTabsStore provides store) {
        content()
    }
}

// In any child screen - access shared state
@Screen(MainTabs.Home::class)
@Composable
fun HomeScreen() {
    val sharedStore = LocalMainTabsStore.current
    val sharedState by sharedStore.subscribe()
    
    Text("Total favorites: \${sharedState.favorites.size}")
}`

// Complete App Setup
const completeSetupCode = `// Application.kt
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}

private fun initKoin() {
    startKoin {
        androidContext(this@MyApplication)
        modules(
            // Core navigation
            navigationModule,
            
            // Feature modules
            profileModule,
            feature1Module,
            feature2Module,
            
            // MVI containers
            tabsDemoModule,
            stateDrivenDemoModule
        )
    }
}

// MainActivity.kt
@Composable
fun App() {
    val navigator: Navigator = koinInject()
    
    NavigationHost(
        navigator = navigator,
        config = navigator.config
    )
}`

export default function DIIntegration() {
  return (
    <article className={styles.features}>
      <h1>FlowMVI & Koin Integration</h1>
      <p className={styles.intro}>
        The <code>quo-vadis-core-flow-mvi</code> module bridges{' '}
        <a href="https://github.com/respawn-app/FlowMVI" target="_blank" rel="noopener noreferrer">FlowMVI's</a>{' '}
        state management with navigation lifecycle, providing automatic container lifecycle management and Koin integration.
      </p>

      {/* Section 1: Overview */}
      <section>
        <h2 id="overview">Overview</h2>
        <p>
          This module provides a seamless integration between FlowMVI's MVI architecture and Quo Vadis navigation,
          allowing you to treat navigation as a side-effect of your business logic while maintaining proper lifecycle awareness.
        </p>
        <h3>Key Features</h3>
        <ul>
          <li><strong>Screen-scoped containers</strong> - MVI containers tied to individual screen lifecycle</li>
          <li><strong>Container-scoped shared state</strong> - Shared state across Tab or Pane navigation containers</li>
          <li><strong>Automatic lifecycle management</strong> - Containers are created and destroyed with navigation nodes</li>
          <li><strong>Koin integration</strong> - Scoped dependency injection with automatic cleanup</li>
          <li><strong>Type-safe navigation</strong> - Navigate from business logic with full type safety</li>
        </ul>
      </section>

      {/* Section 2: Dependencies */}
      <section>
        <h2 id="dependencies">Dependencies</h2>
        <p>
          Add the FlowMVI integration module to your project. This module transitively includes FlowMVI, Koin, and quo-vadis-core.
        </p>
        <CodeBlock code={dependencyCode} language="kotlin" />
      </section>

      {/* Section 3: MVI Contract */}
      <section>
        <h2 id="mvi-contract">MVI Contract</h2>
        <p>
          FlowMVI uses three main components: <strong>State</strong> (what UI displays),
          <strong>Intent</strong> (user actions), and <strong>Action</strong> (side effects like toasts).
        </p>
        <CodeBlock code={contractCode} language="kotlin" />
      </section>

      {/* Section 4: Koin Setup */}
      <section>
        <h2 id="koin-setup">Koin Module Setup</h2>
        <p>
          Register the <code>Navigator</code> as a singleton and your containers with Koin using the provided DSL functions.
          Combine multiple <code>NavigationConfig</code> instances from different feature modules using the <code>+</code> operator:
        </p>
        <CodeBlock code={navigatorRegistrationCode} language="kotlin" />
        <p>
          Register your containers with the <code>navigationContainer</code> and <code>sharedNavigationContainer</code> functions:
        </p>
        <CodeBlock code={koinModuleCode} language="kotlin" />
        <h3>Registration Functions</h3>
        <ul>
          <li><code>navigationContainer&lt;T&gt;</code> - Register a screen-scoped container</li>
          <li><code>sharedNavigationContainer&lt;T&gt;</code> - Register a container-scoped shared container</li>
          <li><code>navigationModule</code> - Required base module with Navigator binding</li>
        </ul>
      </section>

      {/* Section 5: Screen-Scoped Containers */}
      <section>
        <h2 id="screen-container">Screen-Scoped Containers</h2>
        <p>
          <code>NavigationContainer</code> extends FlowMVI's Container with navigation-aware lifecycle management.
          The container receives a <code>NavigationContainerScope</code> that provides access to the navigator and screen-specific information.
        </p>
        <CodeBlock code={screenContainerCode} language="kotlin" />
        <h3>NavigationContainerScope Properties</h3>
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
              <td><code>navigator</code></td>
              <td><code>Navigator</code></td>
              <td>For navigation operations</td>
            </tr>
            <tr>
              <td><code>screenKey</code></td>
              <td><code>String</code></td>
              <td>Unique screen identifier</td>
            </tr>
            <tr>
              <td><code>coroutineScope</code></td>
              <td><code>CoroutineScope</code></td>
              <td>Tied to screen lifecycle</td>
            </tr>
            <tr>
              <td><code>screenNode</code></td>
              <td><code>ScreenNode</code></td>
              <td>Navigation node for the screen</td>
            </tr>
          </tbody>
        </table>
      </section>

      {/* Section 6: Using rememberContainer */}
      <section>
        <h2 id="remember-container">Using rememberContainer</h2>
        <p>
          The <code>rememberContainer</code> composable creates or retrieves a screen-scoped container.
          It automatically manages the container's lifecycle and provides access to the store.
        </p>
        <CodeBlock code={rememberContainerCode} language="kotlin" />
        <h3>Key Points</h3>
        <ul>
          <li>Container is created when the screen enters composition</li>
          <li>Store is automatically started and subscribes to state</li>
          <li>Actions are handled via the subscribe callback</li>
          <li>Container is cleaned up when screen is removed from navigation</li>
        </ul>
      </section>

      {/* Section 7: Shared Containers */}
      <section>
        <h2 id="shared-container">Shared Navigation Containers</h2>
        <p>
          <code>SharedNavigationContainer</code> provides state that persists across all screens within a Tab or Pane container.
          This is useful for sharing data between screens without prop drilling or global state.
        </p>
        <CodeBlock code={sharedContainerCode} language="kotlin" />
        <h3>SharedContainerScope Properties</h3>
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
              <td><code>navigator</code></td>
              <td><code>Navigator</code></td>
              <td>For navigation operations</td>
            </tr>
            <tr>
              <td><code>containerKey</code></td>
              <td><code>String</code></td>
              <td>Unique container identifier</td>
            </tr>
            <tr>
              <td><code>coroutineScope</code></td>
              <td><code>CoroutineScope</code></td>
              <td>Tied to container lifecycle</td>
            </tr>
            <tr>
              <td><code>containerNode</code></td>
              <td><code>LifecycleAwareNode</code></td>
              <td>TabNode or PaneNode</td>
            </tr>
          </tbody>
        </table>
        <p>
          Shared containers are perfect for:
        </p>
        <ul>
          <li>Tab-wide notification badges</li>
          <li>Master-detail selection state</li>
          <li>Cross-screen communication</li>
          <li>Shared preferences within a container</li>
        </ul>
      </section>

      {/* Section 8: Using rememberSharedContainer */}
      <section>
        <h2 id="remember-shared-container">Using rememberSharedContainer</h2>
        <p>
          The <code>rememberSharedContainer</code> composable creates or retrieves a container-scoped shared store.
          Use it in <code>@TabsContainer</code> or <code>@PaneContainer</code> annotated composables to share state across child screens.
        </p>
        <CodeBlock code={rememberSharedContainerCode} language="kotlin" />
        <h3>Key Points</h3>
        <ul>
          <li>Shared container lives as long as the Tab/Pane container</li>
          <li>Use <code>CompositionLocalProvider</code> to share the store with child screens</li>
          <li>Child screens access shared state via <code>CompositionLocal</code></li>
          <li>State persists across tab switches within the container</li>
        </ul>
      </section>

      {/* Section 9: Lifecycle Management */}
      <section>
        <h2 id="lifecycle">Lifecycle Management</h2>
        <p>
          The FlowMVI integration automatically manages container lifecycle based on navigation state.
          Here's how the lifecycle flows:
        </p>
        <LifecycleDiagram />
        <h3>Scope Lifecycle Table</h3>
        <table>
          <thead>
            <tr>
              <th>Scope Type</th>
              <th>Created When</th>
              <th>Destroyed When</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Screen scope</td>
              <td>Screen enters composition</td>
              <td>Screen removed from navigation tree</td>
            </tr>
            <tr>
              <td>Container scope</td>
              <td>TabNode/PaneNode is created</td>
              <td>TabNode/PaneNode is destroyed</td>
            </tr>
          </tbody>
        </table>
        <h3>Lifecycle Guarantees</h3>
        <ul>
          <li><strong>Creation</strong> - Container and scope created on first access</li>
          <li><strong>Persistence</strong> - Same instance returned for same screen key</li>
          <li><strong>Cleanup</strong> - Automatic cleanup when screen is destroyed</li>
          <li><strong>Coroutine cancellation</strong> - All coroutines cancelled on cleanup</li>
        </ul>
      </section>

      {/* Section 10: Multi-Module Organization */}
      <section>
        <h2 id="multi-module">Multi-Module Organization</h2>
        <p>
          Organize container registrations by feature module for clean separation of concerns:
        </p>
        <CodeBlock code={multiModuleCode} language="kotlin" />
        <p>
          Each feature module exports its own Koin module containing containers and 
          dependencies. The app module combines all feature modules during Koin initialization.
        </p>
      </section>

      {/* Section 11: Common Patterns */}
      <section>
        <h2 id="patterns">Common Patterns</h2>
        <h3>Navigation from Container</h3>
        <CodeBlock code={navigationFromContainerCode} language="kotlin" />

        <h3>Conditional Navigation</h3>
        <CodeBlock code={conditionalNavigationCode} language="kotlin" />

        <h3>Sharing State Between Tabs</h3>
        <CodeBlock code={sharingStateBetweenTabsCode} language="kotlin" />
      </section>

      {/* Section 12: Complete App Setup */}
      <section>
        <h2 id="complete-setup">Complete App Setup</h2>
        <p>
          Here's a complete example showing how to wire everything together:
        </p>
        <CodeBlock code={completeSetupCode} language="kotlin" />
      </section>

      {/* Section 13: Best Practices */}
      <section>
        <h2 id="best-practices">Best Practices</h2>
        <DoHeading />
        <ul>
          <li>Keep navigation logic in containers, not UI</li>
          <li>Use <code>NavigationContainer</code> for screen-specific state</li>
          <li>Use <code>SharedNavigationContainer</code> for cross-screen state</li>
          <li>Handle all errors with <code>recover</code> plugin</li>
          <li>Use meaningful intent names describing user actions</li>
          <li>Provide shared stores via <code>CompositionLocal</code></li>
          <li>Register Navigator as singleton - One Navigator instance per app</li>
          <li>Use <code>scope.get()</code> - Inject dependencies from Koin scope, not directly</li>
          <li>Keep container factories simple - Defer complex logic to the container itself</li>
          <li>Organize modules by feature - One Koin module per feature for maintainability</li>
        </ul>
        <DontHeading />
        <ul>
          <li>Don't navigate directly from UI composables</li>
          <li>Don't create container instances manually (use <code>rememberContainer</code>)</li>
          <li>Don't pass navigator as parameter (use scope's navigator)</li>
          <li>Don't ignore the lifecycle - let the framework manage cleanup</li>
          <li>Don't inject containers directly - Use <code>rememberContainer</code> instead</li>
          <li>Avoid circular dependencies - Navigator should not depend on containers</li>
        </ul>
      </section>

      {/* Section 14: Next Steps */}
      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="https://github.com/respawn-app/FlowMVI" target="_blank" rel="noopener noreferrer">FlowMVI Documentation</a> - Learn more about FlowMVI</li>
          <li><Link to="/features/tabbed-navigation">Tabbed Navigation</Link> - Use shared containers with tabs</li>
          <li><Link to="/features/modular">Modular Architecture</Link> - Structure your features</li>
          <li><Link to="/features/deep-links">Deep Links</Link> - Handle deep links with MVI</li>
          <li><Link to="/demo">Live Demo</Link> - See FlowMVI integration in action</li>
        </ul>
      </section>
    </article>
  )
}
