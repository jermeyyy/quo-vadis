import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../../Features.module.css'

// Dependencies
const dependencyCode = `dependencies {
    implementation("io.github.jermeyyy:quo-vadis-core-flow-mvi:0.3.4")
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

// Koin Annotations - Container registration
const koinAnnotationsContainerCode = `// Screen-scoped container with annotations
@Scoped
@Scope(NavigationContainerScope::class)
@Qualifier(ProfileContainer::class)
class ProfileContainer(
    scope: NavigationContainerScope,
    private val repository: ProfileRepository
) : NavigationContainer<ProfileState, ProfileIntent, ProfileAction>(scope) {
    override val store = store(initial = ProfileState.Loading) {
        // Store implementation...
    }
}

// Shared container with annotations
@Scoped
@Scope(SharedContainerScope::class)
@Qualifier(DemoTabsContainer::class)
class DemoTabsContainer(
    scope: SharedContainerScope
) : SharedNavigationContainer<DemoTabsState, DemoTabsIntent, DemoTabsAction>(scope) {
    override val store = store(DemoTabsState()) {
        // Store implementation...
    }
}`

const koinAnnotationsModuleCode = `// Dependencies with annotations
@Factory
class ProfileRepository {
    suspend fun getUser(): UserData = /* ... */
}

// Module with auto-discovery
@Module
@ComponentScan("com.example.feature.profile")
class ProfileModule

@Module
@ComponentScan("com.example.feature.tabs")
class TabsModule`

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

export default function DIIntegrationCoreConcepts() {
  return (
    <article className={styles.features}>
      <h1>FlowMVI & Koin: Core Concepts</h1>
      <p className={styles.intro}>
        Understanding the fundamental building blocks: MVI contract, container types, Koin setup, and lifecycle management.
      </p>

      {/* Section: Dependencies */}
      <section>
        <h2 id="dependencies">Dependencies</h2>
        <p>
          Add the FlowMVI integration module to your project. This module transitively includes FlowMVI, Koin, and quo-vadis-core.
        </p>
        <CodeBlock code={dependencyCode} language="kotlin" />
      </section>

      {/* Section: MVI Contract */}
      <section>
        <h2 id="mvi-contract">MVI Contract</h2>
        <p>
          FlowMVI uses three main components: <strong>State</strong> (what UI displays),
          <strong>Intent</strong> (user actions), and <strong>Action</strong> (side effects like toasts).
        </p>
        <CodeBlock code={contractCode} language="kotlin" />
      </section>

      {/* Section: Koin Setup */}
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

      {/* Section: Koin Annotations Alternative */}
      <section>
        <h2 id="koin-annotations">Koin Annotations Approach</h2>
        <p>
          As an alternative to manual DSL registration, you can use <strong>Koin Annotations</strong> (4.1+)
          for compile-time dependency resolution with less boilerplate.
        </p>

        <h3>Annotating Containers</h3>
        <p>
          Use <code>@Scoped</code> with the appropriate scope class to register containers:
        </p>
        <CodeBlock code={koinAnnotationsContainerCode} language="kotlin" />

        <h3>Module with ComponentScan</h3>
        <p>
          Create modules that auto-discover annotated components in a package:
        </p>
        <CodeBlock code={koinAnnotationsModuleCode} language="kotlin" />

        <h3>Choosing an Approach</h3>
        <table>
          <thead>
            <tr>
              <th>DSL Approach</th>
              <th>Annotations Approach</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Runtime flexibility</td>
              <td>Compile-time safety</td>
            </tr>
            <tr>
              <td>Complex factory logic</td>
              <td>Standard patterns</td>
            </tr>
            <tr>
              <td>Advanced Koin features</td>
              <td>Minimal boilerplate</td>
            </tr>
          </tbody>
        </table>
        <p>
          Both approaches work with <code>rememberContainer</code> and <code>rememberSharedContainer</code> – choose based on your project needs.
        </p>
      </section>

      {/* Section: Screen-Scoped Containers */}
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

      {/* Section: Shared Containers */}
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

      {/* Section: Lifecycle Management */}
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

      {/* Navigation */}
      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/features/di-integration/usage">Usage Guide</Link> - Learn how to use containers in your screens</li>
          <li><Link to="/features/di-integration">Overview</Link> - Back to FlowMVI & Koin overview</li>
        </ul>
      </section>
    </article>
  )
}
