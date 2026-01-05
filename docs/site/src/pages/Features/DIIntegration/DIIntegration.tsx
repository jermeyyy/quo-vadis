import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

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

const screenScopedContainerCode = `inline fun <reified C : NavigationContainer<*, *, *>> Module.navigationContainer(
    crossinline factory: (NavigationContainerScope) -> C
)

// Usage
val profileModule = module {
    single { ProfileRepository() }
    
    navigationContainer<ProfileContainer> { scope ->
        ProfileContainer(
            scope = scope,
            repository = scope.get()  // Inject from parent scope
        )
    }
}`

const containerScopedCode = `inline fun <reified C : SharedNavigationContainer<*, *, *>> Module.sharedNavigationContainer(
    crossinline factory: (SharedContainerScope) -> C
)

// Usage
val tabsDemoModule = module {
    sharedNavigationContainer<DemoTabsContainer> { scope ->
        DemoTabsContainer(scope)
    }
}`

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

const navigatorInjectionCode = `@Screen(MainTabs.HomeTab::class)
@Composable
fun HomeScreen(navigator: Navigator = koinInject()) {
    Column {
        Button(onClick = { 
            navigator.navigate(ProfileDestination.Main) 
        }) {
            Text("Go to Profile")
        }
    }
}`

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
      <h1>Koin DI Integration</h1>
      <p className={styles.intro}>
        Koin integration for injecting Navigator, containers, and screen-scoped dependencies 
        with proper lifecycle management. Quo Vadis provides first-class support for Koin 
        dependency injection throughout the navigation system.
      </p>

      <section>
        <h2 id="navigator-registration">Navigator Registration</h2>
        <p>
          Register the <code>Navigator</code> as a singleton in Koin. Combine multiple 
          <code>NavigationConfig</code> instances from different feature modules using the 
          <code>+</code> operator:
        </p>
        <CodeBlock code={navigatorRegistrationCode} language="kotlin" />
        <p>
          The <code>NavigationConfig</code> handles route resolution, destination mapping, 
          and container registration. The <code>TreeNavigator</code> is initialized with 
          the combined config and builds the initial navigation tree from the start destination.
        </p>
      </section>

      <section>
        <h2 id="screen-scoped-containers">Screen-Scoped Containers</h2>
        <p>
          Use <code>navigationContainer</code> to register MVI containers that are tied 
          to individual screen lifecycles. The container is created when the screen enters 
          composition and destroyed when removed from the navigation tree.
        </p>
        <CodeBlock code={screenScopedContainerCode} language="kotlin" />
        <p>
          The <code>NavigationContainerScope</code> provides access to:
        </p>
        <ul>
          <li><code>navigator</code> - Navigator instance for navigation operations</li>
          <li><code>screenKey</code> - Unique identifier for this screen instance</li>
          <li><code>coroutineScope</code> - Scope tied to screen lifecycle</li>
          <li><code>get()</code> - Inject dependencies from parent Koin scope</li>
        </ul>
      </section>

      <section>
        <h2 id="container-scoped">Container-Scoped Containers</h2>
        <p>
          Use <code>sharedNavigationContainer</code> for state shared across all screens 
          within a Tab or Pane container. The container lives as long as the TabNode/PaneNode.
        </p>
        <CodeBlock code={containerScopedCode} language="kotlin" />
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

      <section>
        <h2 id="injecting-navigator">Injecting Navigator in Screens</h2>
        <p>
          Inject the <code>Navigator</code> directly into screen composables using 
          <code>koinInject()</code>:
        </p>
        <CodeBlock code={navigatorInjectionCode} language="kotlin" />
        <p>
          When using the <code>@Screen</code> annotation, the Navigator can also be 
          provided as a parameter automatically by the code generator.
        </p>
      </section>

      <section>
        <h2 id="scope-lifecycle">Scope Lifecycle</h2>
        <p>
          Koin scopes are automatically tied to navigation node lifecycle:
        </p>
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
        <p>
          When a scope is closed:
        </p>
        <ul>
          <li>The associated <code>CoroutineScope</code> is cancelled</li>
          <li>All ongoing operations are stopped</li>
          <li>Container instances are garbage collected</li>
          <li>Resources are automatically cleaned up</li>
        </ul>
      </section>

      <section>
        <h2 id="complete-setup">Complete App Setup</h2>
        <p>
          Here's a complete example showing how to wire everything together:
        </p>
        <CodeBlock code={completeSetupCode} language="kotlin" />
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <ul>
          <li><strong>Register Navigator as singleton</strong> - One Navigator instance per app</li>
          <li><strong>Use <code>scope.get()</code></strong> - Inject dependencies from Koin scope, not directly</li>
          <li><strong>Keep container factories simple</strong> - Defer complex logic to the container itself</li>
          <li><strong>Organize modules by feature</strong> - One Koin module per feature for maintainability</li>
          <li><strong>Don't inject containers directly</strong> - Use <code>rememberContainer</code> instead</li>
          <li><strong>Avoid circular dependencies</strong> - Navigator should not depend on containers</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/flowmvi">FlowMVI Integration</a> - MVI state management with containers</li>
          <li><a href="/features/modular">Modular Architecture</a> - Structure your features</li>
          <li><a href="/demo">See the demo</a> with DI integration</li>
        </ul>
      </section>
    </article>
  )
}
