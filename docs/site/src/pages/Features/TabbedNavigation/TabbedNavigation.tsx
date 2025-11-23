import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const setupCode = `@TabGraph(
    name = "main_tabs",
    initialTab = "Home",
    primaryTab = "Home"
)
sealed class MainTabs : TabDefinition {

    @Tab(
        route = "tab_home",
        label = "Home",
        icon = "home",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Home::class
    )
    data object Home : MainTabs() {
        override val route = "tab_home"
        override val rootDestination = TabDestination.Home
    }

    @Tab(
        route = "tab_profile",
        label = "Profile",
        icon = "person",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Profile::class
    )
    data object Profile : MainTabs() {
        override val route = "tab_profile"
        override val rootDestination = TabDestination.Profile
    }

    @Tab(
        route = "tab_settings",
        label = "Settings",
        icon = "settings",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Settings::class
    )
    data object Settings : MainTabs() {
        override val route = "tab_settings"
        override val rootDestination = TabDestination.Settings
    }
}`

const usageCode = `@Composable
fun MainTabsScreen(parentNavigator: Navigator) {
    val tabState = rememberTabNavigator(MainTabsConfig, parentNavigator)
    val selectedTab by tabState.selectedTab.collectAsState()
    val tabGraph = remember { buildAppDestinationGraph() }

    TabbedNavHost(
        tabState = tabState,
        tabGraphs = MainTabsConfig.allTabs.associateWith { tabGraph },
        navigator = parentNavigator,
        tabUI = { content ->
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        currentTab = selectedTab,
                        onTabSelected = { tab -> tabState.selectTab(tab) }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    content()
                }
            }
        }
    )
}`

const bottomNavCode = `@Composable
fun BottomNavigationBar(
    currentTab: TabDefinition?,
    onTabSelected: (TabDefinition) -> Unit
) {
    NavigationBar {
        MainTabsConfig.allTabs.forEach { tab ->
            NavigationBarItem(
                icon = { Icon(getIconForTab(tab), contentDescription = tab.label) },
                label = { Text(tab.label ?: "") },
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}`

const navigationInTabCode = `@Composable
fun HomeScreen(navigator: Navigator) {
    Column {
        Button(onClick = {
            // Navigate within current tab
            navigator.navigate(AppDestination.Details("item123"))
        }) {
            Text("View Details")
        }
        
        Button(onClick = {
            // This opens another screen in the same tab
            navigator.navigate(AppDestination.Search)
        }) {
            Text("Search")
        }
    }
}`

export default function TabbedNavigation() {
  return (
    <article className={styles.features}>
      <h1>Tabbed Navigation</h1>
      <p className={styles.intro}>
        Generate complex tab layouts with <strong>independent backstacks</strong> using simple annotations.
        Each tab maintains its own navigation history and state, providing a native app experience
        similar to popular apps like Instagram, Twitter, or Google Maps.
      </p>

      <section>
        <h2 id="overview">Overview</h2>
        <p>
          Quo Vadis provides an annotation-driven system for tabbed navigation that eliminates boilerplate
          while ensuring type safety. KSP (Kotlin Symbol Processing) generates all the necessary
          configuration code at compile time.
        </p>
        <h3>Key Features</h3>
        <ul>
          <li><strong>Zero Boilerplate</strong> - KSP generates configuration code automatically</li>
          <li><strong>Type-Safe</strong> - Compile-time checked tab definitions</li>
          <li><strong>Independent Stacks</strong> - Each tab has its own navigation history</li>
          <li><strong>State Preservation</strong> - Tab content survives tab switches</li>
          <li><strong>Smart Back Press</strong> - Hierarchical navigation across tabs</li>
          <li><strong>Flexible UI</strong> - Works with BottomNavigationBar, NavigationRail, or custom UI</li>
        </ul>
      </section>

      <section>
        <h2 id="setup">Setup</h2>
        <p>
          Define your tab structure using <code>@TabGraph</code> and <code>@Tab</code> annotations.
          KSP will generate a <code>MainTabsConfig</code> object containing all tab configuration.
        </p>
        <CodeBlock code={setupCode} language="kotlin" />
        <h3>Annotation Parameters</h3>
        <ul>
          <li><code>name</code> - Base name for generated code (e.g., "main_tabs" → MainTabsConfig)</li>
          <li><code>initialTab</code> - Tab to display on first launch</li>
          <li><code>primaryTab</code> - Primary tab for smart back navigation (defaults to initialTab)</li>
          <li><code>route</code> - Unique identifier for each tab</li>
          <li><code>label</code> - Display name for UI</li>
          <li><code>icon</code> - Icon identifier (Material Icons name or custom)</li>
          <li><code>rootDestination</code> - Initial destination when tab is selected</li>
        </ul>
      </section>

      <section>
        <h2 id="usage">Usage in UI</h2>
        <p>
          Use the <code>TabbedNavHost</code> composable to render your tabs with custom UI.
          This gives you full control over the tab interface (bottom bar, navigation rail, etc.).
        </p>
        <CodeBlock code={usageCode} language="kotlin" />
      </section>

      <section>
        <h2 id="bottom-navigation">Bottom Navigation Bar</h2>
        <p>
          Create a custom bottom navigation bar that integrates with the tab state:
        </p>
        <CodeBlock code={bottomNavCode} language="kotlin" />
      </section>

      <section>
        <h2 id="navigation-in-tabs">Navigation Within Tabs</h2>
        <p>
          Each tab has its own <code>Navigator</code> instance. Navigate within a tab just like regular navigation:
        </p>
        <CodeBlock code={navigationInTabCode} language="kotlin" />
        <p>
          The navigation stays within the current tab, building up that tab's backstack. When the user
          switches tabs and returns, they'll see the exact same state.
        </p>
      </section>

      <section>
        <h2 id="back-behavior">Smart Back Press Behavior</h2>
        <p>Quo Vadis implements intelligent hierarchical navigation:</p>
        <ol>
          <li><strong>Not at root:</strong> Pop from current tab's navigation stack</li>
          <li><strong>At root (not primary tab):</strong> Switch to the primary tab</li>
          <li><strong>At root (primary tab):</strong> Delegate to parent navigator (usually exits app)</li>
        </ol>
        <p>
          This provides a familiar experience like Instagram or Twitter: pressing back multiple times
          returns you to the home tab before exiting the app.
        </p>
      </section>

      <section>
        <h2 id="architecture">Architecture</h2>
        <p>
          Each tab maintains an independent navigation stack. Switching tabs preserves the entire
          navigation state, including scroll positions and form data.
        </p>
        <pre className={styles.codeBlock}>
{`┌─────────────────────────────────────────────┐
│            TabbedNavHost                     │
│  ┌─────────┬─────────┬─────────┬─────────┐  │
│  │ Home    │ Search  │ Profile │Settings │  │
│  │ Nav     │ Nav     │ Nav     │ Nav     │  │
│  │         │         │         │         │  │
│  │┌──────┐ │┌──────┐ │┌──────┐ │┌──────┐ │  │
│  ││Scr C │ ││Scr F │ ││Scr H │ ││Scr J │ │  │
│  │├──────┤ │├──────┤ │└──────┘ │└──────┘ │  │
│  ││Scr B │ ││Scr E │ │         │         │  │
│  │├──────┤ │├──────┤ │         │         │  │
│  ││Scr A │ ││Scr D │ │         │         │  │
│  │└──────┘ │└──────┘ │         │         │  │
│  └─────────┴─────────┴─────────┴─────────┘  │
└─────────────────────────────────────────────┘`}
        </pre>
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <h3>✅ DO:</h3>
        <ul>
          <li>Keep tabs at top level for main app sections</li>
          <li>Use 3-5 tabs maximum for mobile</li>
          <li>Set a logical primary tab for back navigation</li>
          <li>Use clear, recognizable icons</li>
          <li>Test tab switching and back behavior</li>
        </ul>
        <h3>❌ DON'T:</h3>
        <ul>
          <li>Don't use tabs for linear flows (use regular navigation)</li>
          <li>Don't nest too deeply (max 2 levels)</li>
          <li>Don't change tab structure dynamically</li>
          <li>Don't ignore back behavior configuration</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/quo-vadis/api/index.html">API Reference</a> - Complete API documentation</li>
          <li><a href="/features/annotation-api">Annotation API</a> - Learn more about code generation</li>
          <li><a href="/demo">Live Demo</a> - See tabbed navigation in action</li>
        </ul>
      </section>
    </article>
  )
}
