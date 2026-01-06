import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const treeNavigatorCode = `val navigator = TreeNavigator(
    config = GeneratedNavigationConfig,
    initialState = config.buildNavNode(HomeDestination::class, null)!!
)

// Test navigation
navigator.navigate(HomeDestination.Article("123"))
assertEquals(HomeDestination.Article("123"), navigator.currentDestination.value)

// Test back navigation
assertTrue(navigator.navigateBack())
assertEquals(HomeDestination.Feed, navigator.currentDestination.value)`

const fakeNavigatorCode = `class FakeNavigator : Navigator {
    var lastDestination: NavDestination? = null
    var backPressed = false
    
    override fun navigate(destination: NavDestination, transition: NavigationTransition?) {
        lastDestination = destination
    }
    
    override fun navigateBack(): Boolean {
        backPressed = true
        return true
    }
    
    override val currentDestination = MutableStateFlow<NavDestination?>(null)
    override val canNavigateBack = MutableStateFlow(false)
    // ... other properties
}`

const mviContainerTestCode = `class ProfileContainerTest {
    
    private lateinit var repository: FakeProfileRepository
    private lateinit var navigator: FakeNavigator
    private lateinit var container: ProfileContainer
    
    @BeforeTest
    fun setup() {
        repository = FakeProfileRepository()
        navigator = FakeNavigator()
        
        val testScope = TestNavigationContainerScope(
            navigator = navigator,
            screenKey = "test_profile_screen"
        )
        
        container = ProfileContainer(
            scope = testScope,
            repository = repository
        )
    }
    
    @Test
    fun \`loading profile emits loading then content state\`() = runTest {
        val testUser = UserData(id = "1", name = "Test", email = "test@test.com")
        repository.setUser(testUser)
        
        container.store.test {
            assertEquals(ProfileState.Loading, awaitState())
            
            container.store.intent(ProfileIntent.LoadProfile)
            assertEquals(ProfileState.Content(user = testUser), awaitState())
        }
    }
    
    @Test
    fun \`navigate to settings calls navigator\`() = runTest {
        container.store.intent(ProfileIntent.NavigateToSettings)
        
        assertEquals(MainTabs.SettingsTab.Main, navigator.lastDestination)
    }
}`

const testScopeCode = `class TestNavigationContainerScope(
    override val navigator: Navigator,
    override val screenKey: String
) : NavigationContainerScope {
    override val coroutineScope = TestScope()
    override val screenNode = mockScreenNode()
}`

const flowMviTestCode = `import pro.respawn.flowmvi.test.test

container.store.test {
    // Collect initial state
    assertEquals(InitialState, awaitState())
    
    // Send intent
    intents { ProfileIntent.LoadProfile }
    
    // Assert state transitions
    assertEquals(ProfileState.Loading, awaitState())
    assertEquals(ProfileState.Content(...), awaitState())
    
    // Assert actions
    assertEquals(ProfileAction.ShowToast("Loaded"), awaitAction())
}`

const navigationResultsTestCode = `@Test
fun \`navigateForResult returns selected item\`() = runTest {
    val navigator = TreeNavigator(config, initialState)
    
    launch {
        // Simulate picker returning result
        delay(100)
        navigator.navigateBackWithResult(SelectedItem("1", "Item"))
    }
    
    val result = navigator.navigateForResult(ItemPicker)
    
    assertEquals(SelectedItem("1", "Item"), result)
}

@Test
fun \`navigateForResult returns null on back\`() = runTest {
    val navigator = TreeNavigator(config, initialState)
    
    launch {
        delay(100)
        navigator.navigateBack()  // No result
    }
    
    val result = navigator.navigateForResult(ItemPicker)
    
    assertNull(result)
}`

const deepLinkTestCode = `@Test
fun \`deep link navigates to article\`() {
    val navigator = TreeNavigator(config, initialState)
    
    val handled = navigator.handleDeepLink("app://home/article/42")
    
    assertTrue(handled)
    assertEquals(
        HomeDestination.Article("42"),
        navigator.currentDestination.value
    )
}

@Test
fun \`unknown deep link returns false\`() {
    val navigator = TreeNavigator(config, initialState)
    
    val handled = navigator.handleDeepLink("app://unknown/path")
    
    assertFalse(handled)
}`

const composeUiTestCode = `class ProfileScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun \`profile screen shows loading then content\`() {
        val fakeStore = FakeStore<ProfileState, ProfileIntent, ProfileAction>(
            initialState = ProfileState.Loading
        )
        
        composeTestRule.setContent {
            ProfileScreen(container = fakeStore)
        }
        
        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
        
        fakeStore.emitState(ProfileState.Content(testUser))
        
        composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
    }
}`

const stateAssertionsCode = `// Assert current destination type
assertTrue(navigator.currentDestination.value is HomeDestination.Article)

// Assert destination arguments
val article = navigator.currentDestination.value as HomeDestination.Article
assertEquals("123", article.articleId)

// Assert back navigation availability
assertTrue(navigator.canNavigateBack.value)

// Assert navigation tree depth
val depth = navigator.state.value.activePathToLeaf().size
assertEquals(3, depth)`

export default function Testing() {
  return (
    <article className={styles.features}>
      <h1>Testing Support</h1>
      <p className={styles.intro}>
        Test navigation logic without UI using TreeNavigator and FlowMVI testing utilities.
        Built-in testing support makes it easy to verify navigation behavior in fast, reliable unit tests.
      </p>

      <section>
        <h2 id="tree-navigator-unit-tests">TreeNavigator for Unit Tests</h2>
        <p>
          Use <code>TreeNavigator</code> directly in tests to verify navigation logic
          with the real navigation implementation:
        </p>
        <CodeBlock code={treeNavigatorCode} language="kotlin" />
      </section>

      <section>
        <h2 id="fake-navigator">FakeNavigator Pattern</h2>
        <p>
          Create a fake navigator for lightweight unit tests that don't need
          the full navigation tree:
        </p>
        <CodeBlock code={fakeNavigatorCode} language="kotlin" />
      </section>

      <section>
        <h2 id="testing-mvi-containers">Testing MVI Containers</h2>
        <p>
          Test FlowMVI containers with fake dependencies to verify state transitions
          and navigation side effects:
        </p>
        <CodeBlock code={mviContainerTestCode} language="kotlin" />
      </section>

      <section>
        <h2 id="test-navigation-scope">TestNavigationContainerScope</h2>
        <p>
          Create a test scope to provide dependencies for container testing:
        </p>
        <CodeBlock code={testScopeCode} language="kotlin" />
      </section>

      <section>
        <h2 id="flowmvi-store-testing">FlowMVI Store Testing</h2>
        <p>
          Use the FlowMVI test DSL to verify state transitions and actions:
        </p>
        <CodeBlock code={flowMviTestCode} language="kotlin" />
      </section>

      <section>
        <h2 id="testing-navigation-results">Testing Navigation with Results</h2>
        <p>
          Test the <code>navigateForResult</code> pattern with coroutines:
        </p>
        <CodeBlock code={navigationResultsTestCode} language="kotlin" />
      </section>

      <section>
        <h2 id="testing-deep-links">Testing Deep Links</h2>
        <p>
          Verify that deep links are handled correctly and navigate to the expected destinations:
        </p>
        <CodeBlock code={deepLinkTestCode} language="kotlin" />
      </section>

      <section>
        <h2 id="compose-ui-testing">Compose UI Testing</h2>
        <p>
          Test Compose screens with fake stores to verify UI behavior:
        </p>
        <CodeBlock code={composeUiTestCode} language="kotlin" />
      </section>

      <section>
        <h2 id="navigation-state-assertions">Navigation State Assertions</h2>
        <p>
          Common assertion patterns for verifying navigation state:
        </p>
        <CodeBlock code={stateAssertionsCode} language="kotlin" />
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <ul>
          <li>Test navigation logic separately from UI</li>
          <li>Use <code>FakeNavigator</code> for container tests</li>
          <li>Use <code>TreeNavigator</code> for integration tests</li>
          <li>Test state transitions, not implementation details</li>
          <li>Mock repositories and dependencies</li>
          <li>Test deep link handling with various patterns</li>
          <li>Verify navigation effects on state</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/di-integration">FlowMVI & Koin Integration</a> - Learn about MVI container patterns</li>
          <li><a href="/features/navigation-results">Navigation Results</a> - Understand navigating for results</li>
          <li><a href="/features/deep-links">Deep Links</a> - Configure deep link handling</li>
          <li><a href="/demo">Demo Application</a> - See testing examples in action</li>
        </ul>
      </section>
    </article>
  )
}
