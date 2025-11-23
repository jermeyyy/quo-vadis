import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const contractCode = `// State - What the UI displays
sealed interface ProfileState : MVIState {
    data object Loading : ProfileState
    data class Content(val user: User, val isEditing: Boolean = false) : ProfileState
    data class Error(val message: String) : ProfileState
}

// Intent - What the user wants to do
sealed interface ProfileIntent : MVIIntent {
    data object LoadProfile : ProfileIntent
    data object StartEditing : ProfileIntent
    data class UpdateName(val name: String) : ProfileIntent
    data object SaveChanges : ProfileIntent
    data object NavigateToSettings : ProfileIntent
    data object NavigateBack : ProfileIntent
}

// Action - Side effects (toasts, navigation, etc.)
sealed interface ProfileAction : MVIAction {
    data class ShowToast(val message: String) : ProfileAction
    data object ProfileSaved : ProfileAction
}`

const containerCode = `class ProfileContainer(
    private val navigator: Navigator,
    private val repository: ProfileRepository
) : Container<ProfileState, ProfileIntent, ProfileAction> {

    override val store = store(ProfileState.Loading) {
        configure {
            name = "ProfileStore"
            parallelIntents = false
        }
        
        init {
            intent(ProfileIntent.LoadProfile)
        }
        
        reduce { intent ->
            when (intent) {
                is ProfileIntent.LoadProfile -> {
                    updateState { ProfileState.Loading }
                    val user = repository.getUser()
                    updateState { ProfileState.Content(user) }
                }
                
                is ProfileIntent.NavigateToSettings -> {
                    // Navigation as a side-effect
                    navigator.navigate(SettingsDestination)
                }
                
                is ProfileIntent.SaveChanges -> {
                    val current = state as ProfileState.Content
                    repository.saveUser(current.user)
                    action(ProfileAction.ProfileSaved)
                    action(ProfileAction.ShowToast("Profile saved"))
                }
                
                is ProfileIntent.NavigateBack -> {
                    navigator.navigateBack()
                }
            }
        }
        
        recover { exception ->
            action(ProfileAction.ShowToast("Error: \${exception.message}"))
            null
        }
    }
}`

const uiCode = `@Composable
fun ProfileScreen(
    navigator: Navigator = koinInject(),
    container: ProfileContainer = koinInject { parametersOf(navigator) }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    StoreScreen(
        container = container,
        onAction = { action ->
            when (action) {
                is ProfileAction.ShowToast -> {
                    launch { snackbarHostState.showSnackbar(action.message) }
                }
                ProfileAction.ProfileSaved -> {
                    // Handle success
                }
            }
        }
    ) { state, intentReceiver ->
        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
            when (state) {
                ProfileState.Loading -> CircularProgressIndicator()
                is ProfileState.Content -> ProfileContent(
                    user = state.user,
                    isEditing = state.isEditing,
                    onIntent = intentReceiver::intent
                )
                is ProfileState.Error -> ErrorView(state.message)
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    isEditing: Boolean,
    onIntent: (ProfileIntent) -> Unit
) {
    Column {
        Text("Name: \${user.name}")
        Text("Email: \${user.email}")
        
        Button(onClick = { onIntent(ProfileIntent.NavigateToSettings) }) {
            Text("Settings")
        }
        
        if (isEditing) {
            Button(onClick = { onIntent(ProfileIntent.SaveChanges) }) {
                Text("Save")
            }
        }
    }
}`

const diCode = `val profileModule = module {
    single { ProfileRepository() }
    
    factory { (navigator: Navigator) ->
        ProfileContainer(
            navigator = navigator,
            repository = get()
        )
    }
}`

const testingCode = `@Test
fun \`navigateToSettings should call navigator\`() = runTest {
    val navigator = FakeNavigator()
    val repository = FakeProfileRepository()
    val container = ProfileContainer(navigator, repository)
    
    container.store.subscribeAndTest {
        container.store.intent(ProfileIntent.NavigateToSettings)
        
        assertEquals(SettingsDestination, navigator.lastDestination)
    }
}

@Test
fun \`saveChanges should update repository and show toast\`() = runTest {
    val navigator = FakeNavigator()
    val repository = FakeProfileRepository()
    val container = ProfileContainer(navigator, repository)
    
    container.store.subscribeAndTest {
        // Load profile
        val content = awaitItem() as ProfileState.Content
        
        // Save changes
        container.store.intent(ProfileIntent.SaveChanges)
        
        // Verify action
        val action = awaitAction() as ProfileAction.ProfileSaved
        assertTrue(repository.saveCalled)
    }
}`

export default function FlowMVI() {
  return (
    <article className={styles.features}>
      <h1>FlowMVI Integration</h1>
      <p className={styles.intro}>
        First-class support for <a href="https://github.com/respawn-app/FlowMVI" target="_blank" rel="noopener noreferrer">FlowMVI</a> state management.
        Treat navigation as a <strong>side-effect of your business logic</strong>, keeping your UI clean and testable.
      </p>

      <section>
        <h2 id="overview">Overview</h2>
        <p>
          FlowMVI is a Kotlin Multiplatform MVI (Model-View-Intent) framework built on Kotlin Flow.
          Quo Vadis integrates seamlessly with FlowMVI to provide a clean architecture where
          navigation decisions are made in your business logic layer, not in your UI.
        </p>
        <h3>Why This Matters</h3>
        <p>
          Traditional approach mixes navigation with UI code, making it hard to test and maintain.
          With FlowMVI integration, your UI components simply dispatch intents and render state —
          they don't need to know where navigation leads.
        </p>
        <h3>Key Benefits</h3>
        <ul>
          <li><strong>Testable</strong> - Test navigation logic without UI using FakeNavigator</li>
          <li><strong>Predictable</strong> - Navigation as pure side-effect of state changes</li>
          <li><strong>Centralized</strong> - All navigation logic in one place (Container)</li>
          <li><strong>Decoupled</strong> - UI doesn't know about destination types</li>
          <li><strong>Scalable</strong> - Easy to maintain as app grows in complexity</li>
        </ul>
      </section>

      <section>
        <h2 id="installation">Installation</h2>
        <CodeBlock code={`// build.gradle.kts
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("io.github.jermeyyy:quo-vadis-core:0.1.0")
                implementation("io.github.jermeyyy:quo-vadis-core-flow-mvi:0.1.0")
                
                implementation("pro.respawn.flowmvi:core:2.6.1")
                implementation("pro.respawn.flowmvi:compose:2.6.1")
                
                // For testing
                testImplementation("pro.respawn.flowmvi:test:2.6.1")
            }
        }
    }
}`} language="kotlin" />
      </section>

      <section>
        <h2 id="concept">Core Concepts</h2>
        <p>
          FlowMVI uses three main components: <strong>State</strong> (what UI displays),
          <strong>Intent</strong> (what user wants to do), and <strong>Action</strong> (side effects like navigation or toasts).
        </p>
        <CodeBlock code={contractCode} language="kotlin" />
      </section>

      <section>
        <h2 id="container">Creating a Container</h2>
        <p>
          The Container is where you inject the <code>Navigator</code> and handle navigation as part
          of your business logic. When an intent comes in, you can call <code>navigator.navigate()</code>
          directly in the reduce block.
        </p>
        <CodeBlock code={containerCode} language="kotlin" />
        <h3>Key Points</h3>
        <ul>
          <li>Navigator is injected via constructor (makes testing easy)</li>
          <li>Navigation happens in <code>reduce</code> block alongside state updates</li>
          <li>Use <code>action()</code> for side effects like toasts</li>
          <li>Use <code>recover</code> plugin for graceful error handling</li>
          <li>State updates use <code>updateState</code> DSL</li>
        </ul>
      </section>

      <section>
        <h2 id="ui">Usage in UI</h2>
        <p>
          The <code>quo-vadis-core-flow-mvi</code> module provides the <code>StoreScreen</code> composable
          that handles state subscription and action dispatching automatically.
        </p>
        <CodeBlock code={uiCode} language="kotlin" />
        <h3>StoreScreen Benefits</h3>
        <ul>
          <li>Automatic state subscription and lifecycle management</li>
          <li>Type-safe state and intent handling</li>
          <li>Centralized action handling (toasts, analytics, etc.)</li>
          <li>Clean separation of UI and business logic</li>
        </ul>
      </section>

      <section>
        <h2 id="di">Dependency Injection</h2>
        <p>
          Use Koin (or your preferred DI framework) to inject the Navigator into your Container.
          This makes testing easy and keeps your code decoupled.
        </p>
        <CodeBlock code={diCode} language="kotlin" />
      </section>

      <section>
        <h2 id="testing">Testing Navigation Logic</h2>
        <p>
          One of the biggest advantages of this architecture is testability. You can test navigation
          logic without any UI code using <code>FakeNavigator</code> and FlowMVI's test DSL.
        </p>
        <CodeBlock code={testingCode} language="kotlin" />
        <h3>Testing Benefits</h3>
        <ul>
          <li>No UI required - tests run in milliseconds</li>
          <li>Test navigation logic in isolation</li>
          <li>Verify state changes and side effects</li>
          <li>Easy to mock dependencies</li>
        </ul>
      </section>

      <section>
        <h2 id="patterns">Common Patterns</h2>
        <h3>Navigation with Arguments</h3>
        <CodeBlock code={`sealed interface ProductIntent : MVIIntent {
    data class NavigateToDetails(val productId: String) : ProductIntent
}

reduce { intent ->
    when (intent) {
        is ProductIntent.NavigateToDetails -> {
            navigator.navigate(
                ProductDetailsDestination(intent.productId)
            )
        }
    }
}`} language="kotlin" />

        <h3>Conditional Navigation</h3>
        <CodeBlock code={`reduce { intent ->
    when (intent) {
        ProfileIntent.SaveAndExit -> {
            try {
                repository.save(state.user)
                action(ProfileAction.ShowToast("Saved"))
                // Only navigate on success
                navigator.navigateBack()
            } catch (e: ValidationException) {
                action(ProfileAction.ShowError(e.message))
                // Stay on screen if validation fails
            }
        }
    }
}`} language="kotlin" />

        <h3>Navigation with Multiple Destinations</h3>
        <CodeBlock code={`reduce { intent ->
    when (intent) {
        ProfileIntent.Logout -> {
            repository.clearSession()
            // Navigate to different destinations based on state
            if (hasCompletedOnboarding) {
                navigator.navigate(LoginDestination)
            } else {
                navigator.navigate(OnboardingDestination)
            }
        }
    }
}`} language="kotlin" />
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <h3>✅ DO:</h3>
        <ul>
          <li>Keep all navigation logic in Container, not UI</li>
          <li>Use meaningful intent names (e.g., <code>OpenSettings</code> not <code>Navigate</code>)</li>
          <li>Handle errors gracefully with <code>recover</code> plugin</li>
          <li>Test navigation logic with <code>FakeNavigator</code></li>
          <li>Inject Navigator via constructor for testability</li>
          <li>Use actions for all side effects (toasts, analytics, etc.)</li>
        </ul>
        <h3>❌ DON'T:</h3>
        <ul>
          <li>Don't navigate directly from UI - always use intents</li>
          <li>Don't skip error handling - use recover plugin</li>
          <li>Don't ignore actions - handle all side effects in UI</li>
          <li>Don't hardcode destinations - use sealed classes</li>
          <li>Don't mix business logic with UI code</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="https://github.com/respawn-app/FlowMVI" target="_blank" rel="noopener noreferrer">FlowMVI Documentation</a> - Learn more about FlowMVI</li>
          <li><a href="/quo-vadis/api/index.html">API Reference</a> - Complete API documentation</li>
          <li><a href="/features/mvi">MVI Architecture</a> - General MVI patterns</li>
          <li><a href="/demo">Live Demo</a> - See FlowMVI integration in action</li>
        </ul>
      </section>
    </article>
  )
}
