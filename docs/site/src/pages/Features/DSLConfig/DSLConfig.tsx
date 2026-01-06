import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

// Status badge components
const StatusFull = ({ children = 'Full' }: { children?: string }) => (
  <span className={`${styles.statusBadge} ${styles.statusFull}`}>{children}</span>
)

const StatusNo = ({ children = 'No' }: { children?: string }) => (
  <span className={`${styles.statusBadge} ${styles.statusNo}`}>{children}</span>
)

const navigationConfigInterface = `interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val scopeRegistry: ScopeRegistry
    val transitionRegistry: TransitionRegistry
    val containerRegistry: ContainerRegistry
    val deepLinkRegistry: DeepLinkRegistry
    val paneRoleRegistry: PaneRoleRegistry
    
    fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String? = null,
        parentKey: String? = null
    ): NavNode?
    
    operator fun plus(other: NavigationConfig): NavigationConfig
}`

const basicConfigExample = `val config = navigationConfig {
    // Register screens
    screen<HomeScreen> { destination, sharedScope, animScope ->
        { HomeContent(destination) }
    }
    
    // Register containers
    stack<MainStack>("main-scope") {
        screen<HomeScreen>()
        screen<DetailScreen>()
    }
    
    // Register transitions
    transition<DetailScreen>(NavTransition.SlideHorizontal)
    
    // Register container wrappers
    tabsContainer("main-tabs") { content ->
        MyCustomTabBar(content)
    }
}`

const combiningConfigs = `// Feature modules provide their own configs
val featureAConfig = navigationConfig { /* ... */ }
val featureBConfig = navigationConfig { /* ... */ }

// App module combines all configs
val appConfig = featureAConfig + featureBConfig

// Use with navigator
val navigator = rememberQuoVadisNavigator(MainTabs::class, appConfig)
NavigationHost(navigator)`

const screenRegistryExample = `navigationConfig {
    // Basic screen registration
    screen<HomeScreen> { destination, _, _ ->
        { HomeScreenContent() }
    }
    
    // With destination parameters
    screen<ProfileScreen> { destination, _, _ ->
        { ProfileContent(userId = destination.userId) }
    }
    
    // With shared element transitions
    screen<DetailScreen> { destination, sharedScope, animScope ->
        {
            DetailContent(
                item = destination.item,
                sharedTransitionScope = sharedScope,
                animatedVisibilityScope = animScope
            )
        }
    }
}`

const stackExample = `navigationConfig {
    stack<MainStack>("main-scope") {
        screen<HomeScreen>()
        screen<DetailScreen>()
        screen<SettingsScreen>()
    }
}`

const tabsExample = `navigationConfig {
    tabs<MainTabs>("main-tabs") {
        initialTab = 0
        
        // Simple flat tabs
        tab(HomeTab, title = "Home", icon = Icons.Default.Home)
        tab(SearchTab, title = "Search", icon = Icons.Default.Search)
        
        // Tab with nested navigation stack
        tab(ProfileTab, title = "Profile", icon = Icons.Default.Person) {
            screen<ProfileScreen>()
            screen<EditProfileScreen>()
            screen<ProfileSettingsScreen>()
        }
    }
}`

const panesExample = `navigationConfig {
    panes<ListDetailPanes>("list-detail") {
        initialPane = PaneRole.Primary
        backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        
        primary(weight = 0.4f, minWidth = 300.dp) {
            root(ListScreen)
            alwaysVisible()
        }
        
        secondary(weight = 0.6f) {
            root(DetailPlaceholder)
        }
    }
}`

const tabsContainerWrapper = `navigationConfig {
    tabsContainer("main-tabs") { content ->
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabMetadata.forEachIndexed { index, meta ->
                        NavigationBarItem(
                            selected = activeTabIndex == index,
                            onClick = { switchTab(index) },
                            icon = { Icon(meta.icon, meta.label) },
                            label = { Text(meta.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                content()  // Library renders active tab content
            }
        }
    }
}`

const paneContainerWrapper = `navigationConfig {
    paneContainer("list-detail") { content ->
        if (isExpanded) {
            Row(Modifier.fillMaxSize()) {
                paneContents.filter { it.isVisible }.forEach { pane ->
                    val weight = when (pane.role) {
                        PaneRole.Primary -> 0.4f
                        PaneRole.Supporting -> 0.6f
                        PaneRole.Extra -> 0.25f
                    }
                    Box(Modifier.weight(weight).fillMaxHeight()) {
                        pane.content()
                    }
                }
            }
        } else {
            content()  // Single pane mode
        }
    }
}`

const transitionsExample = `navigationConfig {
    transition<DetailScreen>(NavTransition.SlideHorizontal)
    transition<ModalScreen>(NavTransition.SlideVertical)
    transition<SettingsScreen>(NavTransition.Fade)
    transition<QuickViewScreen>(NavTransition.ScaleIn)
}`

const customTransition = `val myTransition = NavTransition(
    enter = fadeIn() + expandHorizontally(),
    exit = fadeOut() + shrinkHorizontally(),
    popEnter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
    popExit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
)

navigationConfig {
    transition<MyScreen>(myTransition)
}`

const scopeExample = `navigationConfig {
    // Explicit scope definition
    scope("main-tabs") {
        +HomeScreen::class
        +SearchScreen::class
        +ProfileScreen::class
        +SettingsScreen::class
    }
    
    // Alternative using include
    scope("profile-stack") {
        include(ProfileScreen::class)
        include(EditProfileScreen::class)
        include(ProfileSettingsScreen::class)
    }
}`

const autoScopeExample = `navigationConfig {
    // This automatically registers "main" scope with all listed screens
    stack<MainStack>("main") {
        screen<HomeScreen>()
        screen<DetailScreen>()
    }
    
    // This automatically registers "main-tabs" scope with all tab destinations
    tabs<MainTabs>("main-tabs") {
        tab(HomeTab, title = "Home")
        tab(ProfileTab, title = "Profile")
    }
}`

const completeExample = `val appNavigationConfig = navigationConfig {
    
    // ═══════════════════════════════════════════════════════
    // SCREEN REGISTRATIONS
    // ═══════════════════════════════════════════════════════
    
    screen<HomeScreen> { destination, _, _ ->
        { HomeContent() }
    }
    
    screen<ProfileScreen> { destination, _, _ ->
        { ProfileContent(userId = destination.userId) }
    }
    
    screen<DetailScreen> { destination, sharedScope, animScope ->
        {
            DetailContent(
                itemId = destination.itemId,
                sharedTransitionScope = sharedScope,
                animatedVisibilityScope = animScope
            )
        }
    }
    
    // ═══════════════════════════════════════════════════════
    // TAB CONTAINER
    // ═══════════════════════════════════════════════════════
    
    tabs<MainTabs>("main-tabs") {
        initialTab = 0
        
        tab(HomeTab, title = "Home", icon = Icons.Default.Home) {
            screen<HomeScreen>()
            screen<DetailScreen>()
        }
        
        tab(SearchTab, title = "Search", icon = Icons.Default.Search)
        
        tab(ProfileTab, title = "Profile", icon = Icons.Default.Person) {
            screen<ProfileScreen>()
            screen<EditProfileScreen>()
        }
    }
    
    // ═══════════════════════════════════════════════════════
    // PANE CONTAINER (Master-Detail)
    // ═══════════════════════════════════════════════════════
    
    panes<MessagesPanes>("messages") {
        initialPane = PaneRole.Primary
        backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        
        primary(weight = 0.35f, minWidth = 280.dp) {
            root(ConversationListScreen)
            alwaysVisible()
        }
        
        secondary(weight = 0.65f, minWidth = 400.dp) {
            root(ConversationDetailPlaceholder)
        }
    }
    
    // ═══════════════════════════════════════════════════════
    // TRANSITIONS
    // ═══════════════════════════════════════════════════════
    
    transition<DetailScreen>(NavTransition.SlideHorizontal)
    transition<ModalScreen>(NavTransition.SlideVertical)
    transition<SettingsScreen>(NavTransition.Fade)
    
    // ═══════════════════════════════════════════════════════
    // CONTAINER WRAPPERS
    // ═══════════════════════════════════════════════════════
    
    tabsContainer("main-tabs") { content ->
        Column {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
            NavigationBar {
                tabMetadata.forEachIndexed { index, meta ->
                    NavigationBarItem(
                        selected = activeTabIndex == index,
                        onClick = { switchTab(index) },
                        icon = { 
                            Icon(
                                imageVector = meta.icon ?: Icons.Default.Circle,
                                contentDescription = meta.label
                            )
                        },
                        label = { Text(meta.label) },
                        enabled = !isTransitioning
                    )
                }
            }
        }
    }
    
    paneContainer("messages") { content ->
        if (isExpanded) {
            Row(Modifier.fillMaxSize()) {
                paneContents.filter { it.isVisible }.forEach { pane ->
                    val weight = when (pane.role) {
                        PaneRole.Primary -> 0.35f
                        PaneRole.Supporting -> 0.65f
                        PaneRole.Extra -> 0.25f
                    }
                    Box(Modifier.weight(weight).fillMaxHeight()) {
                        pane.content()
                    }
                }
            }
        } else {
            content()
        }
    }
    
    // ═══════════════════════════════════════════════════════
    // ADDITIONAL SCOPES
    // ═══════════════════════════════════════════════════════
    
    scope("modal-flow") {
        +ModalScreen::class
        +ConfirmationScreen::class
        +ResultScreen::class
    }
}`

const usingConfig = `@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(
        initialDestination = MainTabs::class,
        config = appNavigationConfig
    )
    
    NavigationHost(
        navigator = navigator,
        modifier = Modifier.fillMaxSize(),
        enablePredictiveBack = true,
        windowSizeClass = calculateWindowSizeClass()
    )
}`

const hybridExample = `// KSP-generated config
val generatedConfig = GeneratedNavigationConfig

// Manual additions
val dynamicConfig = navigationConfig {
    // Feature-flagged screens
    if (featureFlags.isNewProfileEnabled) {
        screen<NewProfileScreen> { dest, _, _ ->
            { NewProfileContent(dest) }
        }
    }
}

// Combine them
val finalConfig = generatedConfig + dynamicConfig

val navigator = rememberQuoVadisNavigator(MainTabs::class, finalConfig)`

export default function DSLConfig() {
  return (
    <article className={styles.features}>
      <h1>DSL Configuration</h1>
      <p className={styles.intro}>
        The programmatic DSL approach for configuring Quo Vadis navigation,
        offering maximum control and flexibility for dynamic navigation setups.
      </p>

      <section>
        <h2 id="overview">Overview</h2>
        <p>
          Quo Vadis supports two configuration approaches. Choose the one that best fits your project needs:
        </p>
        <table>
          <thead>
            <tr>
              <th>Approach</th>
              <th>Best For</th>
              <th>Characteristics</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><strong>Annotations</strong></td>
              <td>Most projects</td>
              <td>Declarative, compile-time safety, KSP generates config</td>
            </tr>
            <tr>
              <td><strong>DSL</strong></td>
              <td>Dynamic setups</td>
              <td>Runtime flexibility, manual control, no code generation</td>
            </tr>
          </tbody>
        </table>

        <h3>When to Choose DSL Configuration</h3>
        <p>Use the DSL approach when you need:</p>
        <ul>
          <li><strong>Dynamic navigation graphs</strong> that change based on user state or feature flags</li>
          <li><strong>Multi-module composition</strong> where modules provide their own navigation config</li>
          <li><strong>Testing flexibility</strong> with customizable navigation setups</li>
          <li><strong>Full control</strong> over registration without annotation processing</li>
          <li><strong>Hybrid setups</strong> combining generated and manual configurations</li>
        </ul>

        <h3>Key Components</h3>
        <table>
          <thead>
            <tr>
              <th>Component</th>
              <th>Purpose</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>NavigationConfig</code></td>
              <td>Unified configuration object containing all registries</td>
            </tr>
            <tr>
              <td><code>ScreenRegistry</code></td>
              <td>Maps destinations to composable content</td>
            </tr>
            <tr>
              <td><code>ContainerRegistry</code></td>
              <td>Provides container structures and wrapper composables</td>
            </tr>
            <tr>
              <td><code>TransitionRegistry</code></td>
              <td>Maps destinations to custom transitions</td>
            </tr>
            <tr>
              <td><code>ScopeRegistry</code></td>
              <td>Defines navigation scope membership</td>
            </tr>
            <tr>
              <td><code>RouteRegistry</code></td>
              <td>Maps routes to destination classes</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="navigation-config">NavigationConfig</h2>
        <p>
          <code>NavigationConfig</code> is the central configuration interface that aggregates all navigation
          registries. It provides a unified contract for the navigation system.
        </p>

        <h3>Interface Structure</h3>
        <CodeBlock code={navigationConfigInterface} language="kotlin" />

        <h3>Creating Configuration with DSL</h3>
        <p>
          Use the <code>navigationConfig</code> builder function to create a complete configuration:
        </p>
        <CodeBlock code={basicConfigExample} language="kotlin" />

        <h3>Combining Configurations</h3>
        <p>
          Configurations can be combined using the <code>+</code> operator for multi-module setups:
        </p>
        <CodeBlock code={combiningConfigs} language="kotlin" />
        <div className={styles.note}>
          <p>
            <strong>Priority Rule:</strong> When configs are combined, the right-hand config takes priority
            for duplicate registrations.
          </p>
        </div>
      </section>

      <section>
        <h2 id="screen-registry">Screen Registry</h2>
        <p>
          The <code>ScreenRegistry</code> maps navigation destinations to their composable content.
          Use the <code>screen&lt;D&gt;</code> function in the configuration builder:
        </p>
        <CodeBlock code={screenRegistryExample} language="kotlin" />

        <h3>Content Lambda Parameters</h3>
        <table>
          <thead>
            <tr>
              <th>Parameter</th>
              <th>Type</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>destination</code></td>
              <td><code>D</code></td>
              <td>The destination instance with navigation arguments</td>
            </tr>
            <tr>
              <td><code>sharedTransitionScope</code></td>
              <td><code>SharedTransitionScope?</code></td>
              <td>Scope for shared element transitions</td>
            </tr>
            <tr>
              <td><code>animatedVisibilityScope</code></td>
              <td><code>AnimatedVisibilityScope?</code></td>
              <td>Scope for coordinated animations</td>
            </tr>
          </tbody>
        </table>
        <p>
          The lambda returns a <code>@Composable () -&gt; Unit</code> that renders the screen content.
        </p>
      </section>

      <section>
        <h2 id="container-registry">Container Registry</h2>
        <p>
          The <code>ContainerRegistry</code> handles two responsibilities: building container structures
          and rendering custom wrapper UIs around navigation content.
        </p>

        <h3>Stack Registration</h3>
        <p>Register linear navigation stacks:</p>
        <CodeBlock code={stackExample} language="kotlin" />

        <h3>Tab Registration</h3>
        <p>Register tab-based navigation with the <code>tabs&lt;D&gt;</code> builder:</p>
        <CodeBlock code={tabsExample} language="kotlin" />

        <h3>Pane Registration</h3>
        <p>Register adaptive multi-pane layouts:</p>
        <CodeBlock code={panesExample} language="kotlin" />

        <h3>Container Wrappers</h3>
        <p>
          Register custom UI wrappers for tab containers. The wrapper receives the active tab content
          and can customize the surrounding UI:
        </p>
        <CodeBlock code={tabsContainerWrapper} language="kotlin" />

        <h4>TabsContainerScope Properties</h4>
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
              <td>Navigator instance for programmatic navigation</td>
            </tr>
            <tr>
              <td><code>activeTabIndex</code></td>
              <td><code>Int</code></td>
              <td>Currently selected tab (0-based)</td>
            </tr>
            <tr>
              <td><code>tabCount</code></td>
              <td><code>Int</code></td>
              <td>Total number of tabs</td>
            </tr>
            <tr>
              <td><code>tabMetadata</code></td>
              <td><code>List&lt;TabMetadata&gt;</code></td>
              <td>Labels, icons, and routes for all tabs</td>
            </tr>
            <tr>
              <td><code>isTransitioning</code></td>
              <td><code>Boolean</code></td>
              <td>Whether tab switch animation is in progress</td>
            </tr>
          </tbody>
        </table>

        <p>Register custom pane layout wrappers:</p>
        <CodeBlock code={paneContainerWrapper} language="kotlin" />

        <h4>PaneContainerScope Properties</h4>
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
              <td>Navigator instance</td>
            </tr>
            <tr>
              <td><code>activePaneRole</code></td>
              <td><code>PaneRole</code></td>
              <td>Currently active pane</td>
            </tr>
            <tr>
              <td><code>isExpanded</code></td>
              <td><code>Boolean</code></td>
              <td>Multi-pane mode active</td>
            </tr>
            <tr>
              <td><code>paneContents</code></td>
              <td><code>List&lt;PaneContent&gt;</code></td>
              <td>Content slots for custom layout</td>
            </tr>
            <tr>
              <td><code>isTransitioning</code></td>
              <td><code>Boolean</code></td>
              <td>Pane transition in progress</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="transition-registry">Transition Registry</h2>
        <p>
          The <code>TransitionRegistry</code> maps destinations to custom transition animations:
        </p>
        <CodeBlock code={transitionsExample} language="kotlin" />

        <h3>Preset Transitions</h3>
        <table>
          <thead>
            <tr>
              <th>Transition</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>NavTransition.SlideHorizontal</code></td>
              <td>Slide from right with fade (default for stacks)</td>
            </tr>
            <tr>
              <td><code>NavTransition.SlideVertical</code></td>
              <td>Slide from bottom with fade (modal-style)</td>
            </tr>
            <tr>
              <td><code>NavTransition.Fade</code></td>
              <td>Simple fade in/out</td>
            </tr>
            <tr>
              <td><code>NavTransition.ScaleIn</code></td>
              <td>Scale with fade (zoom effect)</td>
            </tr>
            <tr>
              <td><code>NavTransition.None</code></td>
              <td>Instant switch, no animation</td>
            </tr>
          </tbody>
        </table>

        <h3>Custom Transitions</h3>
        <p>Create custom transitions by combining Compose animation primitives:</p>
        <CodeBlock code={customTransition} language="kotlin" />
      </section>

      <section>
        <h2 id="scope-registry">Scope Registry</h2>
        <p>
          The <code>ScopeRegistry</code> determines navigation scope membership, controlling whether
          destinations stay within containers or navigate outside.
        </p>
        <p>
          When navigating from within a tab or pane container:
        </p>
        <ul>
          <li><strong>In scope:</strong> Destination belongs to this container → navigate within</li>
          <li><strong>Out of scope:</strong> Destination is external → navigate to parent stack</li>
        </ul>

        <h3>Defining Scopes</h3>
        <CodeBlock code={scopeExample} language="kotlin" />

        <h3>Automatic Scope Registration</h3>
        <p>
          When using <code>stack</code>, <code>tabs</code>, or <code>panes</code> builders, scopes are automatically registered:
        </p>
        <CodeBlock code={autoScopeExample} language="kotlin" />
      </section>

      <section>
        <h2 id="complete-example">Complete Example</h2>
        <p>
          Here's a full DSL configuration demonstrating all features:
        </p>
        <CodeBlock code={completeExample} language="kotlin" />

        <h3>Using the Configuration</h3>
        <CodeBlock code={usingConfig} language="kotlin" />
      </section>

      <section>
        <h2 id="comparison">DSL vs Annotations</h2>
        <table>
          <thead>
            <tr>
              <th>Feature</th>
              <th>DSL</th>
              <th>Annotations</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Runtime flexibility</td>
              <td><StatusFull>Full control</StatusFull></td>
              <td><StatusNo>Compile-time only</StatusNo></td>
            </tr>
            <tr>
              <td>Code generation</td>
              <td><StatusNo>None needed</StatusNo></td>
              <td><StatusFull>KSP generates</StatusFull></td>
            </tr>
            <tr>
              <td>Type safety</td>
              <td><StatusFull>Compile-time</StatusFull></td>
              <td><StatusFull>Compile-time</StatusFull></td>
            </tr>
            <tr>
              <td>Boilerplate</td>
              <td>Medium</td>
              <td>Low</td>
            </tr>
            <tr>
              <td>Dynamic graphs</td>
              <td><StatusFull>Supported</StatusFull></td>
              <td><StatusNo>Not supported</StatusNo></td>
            </tr>
            <tr>
              <td>Multi-module</td>
              <td><StatusFull>Manual</StatusFull></td>
              <td><StatusFull>Auto-merged</StatusFull></td>
            </tr>
            <tr>
              <td>Learning curve</td>
              <td>Medium</td>
              <td>Low</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="hybrid">Hybrid Approach</h2>
        <p>
          You can combine generated config with manual DSL additions for dynamic features
          like feature flags:
        </p>
        <CodeBlock code={hybridExample} language="kotlin" />
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <ul>
          <li><strong>Use descriptive scope keys</strong> – Name scopes after their containers (e.g., <code>"main-tabs"</code>, <code>"profile-stack"</code>)</li>
          <li><strong>Register all screens</strong> – Every destination that can be navigated to needs a screen registration</li>
          <li><strong>Match container and wrapper keys</strong> – Use the same key for container and wrapper registration</li>
          <li><strong>Leverage automatic scope registration</strong> – Let <code>stack</code>/<code>tabs</code>/<code>panes</code> builders handle scope membership</li>
          <li><strong>Test with <code>NavigationConfig.Empty</code></strong> – Use as baseline in tests to isolate behavior</li>
          <li><strong>Compose configs in app module</strong> – Feature modules export configs, app module combines them</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/features/annotation-api">Annotation-Based API</Link> – Declarative approach with code generation</li>
          <li><Link to="/features/transitions">Transitions & Animations</Link> – Animation configuration in depth</li>
          <li><Link to="/features/tabbed-navigation">Tabbed Navigation</Link> – Tab-based navigation patterns</li>
          <li><Link to="/features/modular">Modular Architecture</Link> – Multi-module project setup</li>
        </ul>
      </section>
    </article>
  )
}
