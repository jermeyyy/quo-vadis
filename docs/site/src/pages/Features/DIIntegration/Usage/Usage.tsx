import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../../Features.module.css'

// Styled heading components for Do/Don't sections
const DoHeading = () => (
  <h3><span className={`${styles.statusBadge} ${styles.statusFull}`}>DO</span></h3>
)

const DontHeading = () => (
  <h3><span className={`${styles.statusBadge} ${styles.statusNo}`}>DON'T</span></h3>
)

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

export default function DIIntegrationUsage() {
  return (
    <article className={styles.features}>
      <h1>FlowMVI & Koin: Usage Guide</h1>
      <p className={styles.intro}>
        Learn how to use containers in your screens, organize multi-module projects, and follow best practices.
      </p>

      {/* Section: Using rememberContainer */}
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

      {/* Section: Using rememberSharedContainer */}
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

      {/* Section: Multi-Module Organization */}
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

      {/* Section: Common Patterns */}
      <section>
        <h2 id="patterns">Common Patterns</h2>
        <h3>Navigation from Container</h3>
        <CodeBlock code={navigationFromContainerCode} language="kotlin" />

        <h3>Conditional Navigation</h3>
        <CodeBlock code={conditionalNavigationCode} language="kotlin" />

        <h3>Sharing State Between Tabs</h3>
        <CodeBlock code={sharingStateBetweenTabsCode} language="kotlin" />
      </section>

      {/* Section: Complete App Setup */}
      <section>
        <h2 id="complete-setup">Complete App Setup</h2>
        <p>
          Here's a complete example showing how to wire everything together:
        </p>
        <CodeBlock code={completeSetupCode} language="kotlin" />
      </section>

      {/* Section: Best Practices */}
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

      {/* Navigation */}
      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/features/di-integration/core-concepts">Core Concepts</Link> - Learn about MVI contract, container types, and lifecycle</li>
          <li><Link to="/features/di-integration">Overview</Link> - Back to FlowMVI & Koin overview</li>
          <li><a href="https://github.com/respawn-app/FlowMVI" target="_blank" rel="noopener noreferrer">FlowMVI Documentation</a> - Learn more about FlowMVI</li>
          <li><Link to="/features/tabbed-navigation">Tabbed Navigation</Link> - Use shared containers with tabs</li>
          <li><Link to="/features/modular">Modular Architecture</Link> - Structure your features</li>
          <li><Link to="/demo">Live Demo</Link> - See FlowMVI integration in action</li>
        </ul>
      </section>
    </article>
  )
}
