import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const tabsAnnotationCode = `@Tabs(
    name = "mainTabs",
    initialTab = MainTabs.HomeTab::class,
    items = [MainTabs.HomeTab::class, MainTabs.ExploreTab::class, 
             MainTabs.ProfileTab::class, MainTabs.SettingsTab::class]
)
sealed class MainTabs : NavDestination {

    @TabItem(label = "Home", icon = "home")
    @Destination(route = "main/home")
    @Transition(type = TransitionType.Fade)
    data object HomeTab : MainTabs()

    @TabItem(label = "Explore", icon = "explore")
    @Destination(route = "main/explore")
    @Transition(type = TransitionType.Fade)
    data object ExploreTab : MainTabs()

    @TabItem(label = "Profile", icon = "person")
    @Destination(route = "main/profile")
    @Transition(type = TransitionType.Fade)
    data object ProfileTab : MainTabs()

    @TabItem(label = "Settings", icon = "settings")
    @Stack(name = "settingsTabStack", startDestination = SettingsTab.Main::class)
    @Transition(type = TransitionType.Fade)
    sealed class SettingsTab : MainTabs() {
        @Destination(route = "settings/main")
        data object Main : SettingsTab()

        @Destination(route = "settings/profile")
        @Transition(type = TransitionType.SlideHorizontal)
        data object Profile : SettingsTab()
    }
}`

const tabNodeStructureCode = `@Serializable
@SerialName("tab")
data class TabNode(
    override val key: String,
    override val parentKey: String?,
    val stacks: List<StackNode>,
    val activeStackIndex: Int = 0,
    val wrapperKey: String? = null,
    val tabMetadata: List<GeneratedTabMetadata> = emptyList(),
    val scopeKey: String? = null
) : NavNode {
    val activeStack: StackNode   // Currently selected tab's stack
    val tabCount: Int            // Number of tabs
}`

const tabsContainerCode = `@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                scope.tabMetadata.forEachIndexed { index, meta ->
                    NavigationBarItem(
                        selected = index == scope.activeTabIndex,
                        onClick = { scope.switchTab(index) },
                        icon = { Icon(getTabIcon(meta.icon), meta.label) },
                        label = { Text(meta.label) },
                        enabled = !scope.isTransitioning
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            content()
        }
    }
}`

const nestedStacksCode = `@Tabs(
    name = "appTabs",
    initialTab = AppTabs.HomeTab::class,
    items = [AppTabs.HomeTab::class, AppTabs.SearchTab::class]
)
sealed class AppTabs : NavDestination {

    // Tab with its own nested navigation stack
    @TabItem(label = "Home", icon = "home")
    @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
    sealed class HomeTab : AppTabs() {
        @Destination(route = "home/feed")
        data object Feed : HomeTab()
        
        @Destination(route = "home/article/{id}")
        data class Article(@Argument val id: String) : HomeTab()
        
        @Destination(route = "home/comments/{articleId}")
        data class Comments(@Argument val articleId: String) : HomeTab()
    }

    // Another tab with its own stack
    @TabItem(label = "Search", icon = "search")
    @Stack(name = "searchStack", startDestination = SearchTab.Main::class)
    sealed class SearchTab : AppTabs() {
        @Destination(route = "search/main")
        data object Main : SearchTab()
        
        @Destination(route = "search/results/{query}")
        data class Results(@Argument val query: String) : SearchTab()
    }
}`

const sharedStateCode = `class DemoTabsContainer(scope: SharedContainerScope) :
    SharedNavigationContainer<DemoTabsState, DemoTabsIntent, DemoTabsAction>(scope) {
    
    override val store = store(DemoTabsState()) {
        reduce { intent ->
            when (intent) {
                is DemoTabsIntent.IncrementViewed -> updateState {
                    copy(totalItemsViewed = totalItemsViewed + 1)
                }
            }
        }
    }
}

// Provide via CompositionLocal
val LocalDemoTabsStore = staticCompositionLocalOf<Store<...>>()

@TabsContainer(DemoTabs::class)
@Composable
fun DemoTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    val store = rememberSharedContainer<DemoTabsContainer, 
        DemoTabsState, DemoTabsIntent, DemoTabsAction>()
    
    CompositionLocalProvider(LocalDemoTabsStore provides store) {
        content()
    }
}`

export default function TabbedNavigation() {
  return (
    <article className={styles.features}>
      <h1>Tabbed Navigation</h1>
      <p className={styles.intro}>
        TabNode maintains multiple StackNode instances—one for each tab—with each tab preserving 
        its own navigation history independently. This enables rich, app-like experiences where 
        users can switch between sections without losing their place.
      </p>

      <section>
        <h2 id="overview">Overview</h2>
        <p>
          Quo Vadis provides a powerful annotation-driven system for tabbed navigation that eliminates 
          boilerplate while ensuring type safety. Each tab operates as an independent navigation stack, 
          preserving scroll positions, form data, and navigation history when switching between tabs.
        </p>
        <h3>Key Features</h3>
        <ul>
          <li><strong>Independent Backstacks</strong> - Each tab maintains its own navigation history</li>
          <li><strong>State Preservation</strong> - Tab content survives tab switches</li>
          <li><strong>Nested Stacks</strong> - Tabs can contain their own deep navigation</li>
          <li><strong>Custom UI</strong> - Full control over tab bar appearance</li>
          <li><strong>Shared State</strong> - Share data across tabs with MVI containers</li>
          <li><strong>Platform Icons</strong> - Native icon support per platform</li>
        </ul>
      </section>

      <section>
        <h2 id="tabs-annotation">@Tabs + @TabItem Annotations</h2>
        <p>
          Define your tab structure using <code>@Tabs</code> on a sealed class and <code>@TabItem</code> 
          on each tab destination. The KSP processor generates all configuration code automatically.
        </p>
        <CodeBlock code={tabsAnnotationCode} language="kotlin" />
        <p>
          Notice how <code>SettingsTab</code> is both a tab item and contains its own nested stack 
          with multiple destinations. This enables deep navigation within individual tabs.
        </p>
      </section>

      <section>
        <h2 id="tabs-properties">@Tabs Properties</h2>
        <table>
          <thead>
            <tr>
              <th>Property</th>
              <th>Type</th>
              <th>Default</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>name</code></td>
              <td><code>String</code></td>
              <td>—</td>
              <td>Unique identifier for the tab container</td>
            </tr>
            <tr>
              <td><code>initialTab</code></td>
              <td><code>KClass&lt;*&gt;</code></td>
              <td><code>Unit::class</code></td>
              <td>Initially selected tab (Unit = first tab)</td>
            </tr>
            <tr>
              <td><code>items</code></td>
              <td><code>Array&lt;KClass&lt;*&gt;&gt;</code></td>
              <td><code>[]</code></td>
              <td>Tab classes in display order</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="tabitem-properties">@TabItem Properties</h2>
        <table>
          <thead>
            <tr>
              <th>Property</th>
              <th>Type</th>
              <th>Default</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>label</code></td>
              <td><code>String</code></td>
              <td>—</td>
              <td>Display label for the tab</td>
            </tr>
            <tr>
              <td><code>icon</code></td>
              <td><code>String</code></td>
              <td><code>""</code></td>
              <td>Icon identifier (platform-specific)</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="tabnode-structure">TabNode Structure</h2>
        <p>
          At runtime, tab navigation is represented by a <code>TabNode</code> in the navigation tree. 
          This node holds all tab stacks and tracks which tab is currently active.
        </p>
        <CodeBlock code={tabNodeStructureCode} language="kotlin" />
      </section>

      <section>
        <h2 id="tabnode-behavior">TabNode Behavior</h2>
        <p>
          Understanding how TabNode responds to navigation operations helps you design intuitive flows:
        </p>
        <table>
          <thead>
            <tr>
              <th>Operation</th>
              <th>Effect</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><strong>Switch Tab</strong></td>
              <td>Updates <code>activeStackIndex</code></td>
            </tr>
            <tr>
              <td><strong>Push</strong></td>
              <td>Affects only the active stack</td>
            </tr>
            <tr>
              <td><strong>Pop</strong></td>
              <td>Removes from active stack; may switch tabs</td>
            </tr>
          </tbody>
        </table>
        <p>
          When popping from an empty non-primary tab, Quo Vadis can automatically switch to the 
          primary tab, providing a familiar back-navigation experience.
        </p>
      </section>

      <section>
        <h2 id="tabs-container">@TabsContainer - Custom Tab UI</h2>
        <p>
          Use <code>@TabsContainer</code> to create custom tab bar UI with full control over 
          appearance and behavior. The container receives a <code>TabsContainerScope</code> with 
          all necessary state and actions.
        </p>
        <CodeBlock code={tabsContainerCode} language="kotlin" />
        <p>
          This pattern works with any tab UI: bottom navigation bars, navigation rails, 
          top tabs, or completely custom designs.
        </p>
      </section>

      <section>
        <h2 id="container-scope">TabsContainerScope Properties</h2>
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
              <td>Navigation operations</td>
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
              <td>Labels, icons, routes for tabs</td>
            </tr>
            <tr>
              <td><code>isTransitioning</code></td>
              <td><code>Boolean</code></td>
              <td>Whether transition is in progress</td>
            </tr>
            <tr>
              <td><code>switchTab(index)</code></td>
              <td>Function</td>
              <td>Switch to different tab</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="nested-stacks">Tabs with Nested Stacks</h2>
        <p>
          Tabs can contain their own navigation stacks for deep navigation within each tab. 
          This is perfect for sections like a home feed where users drill into articles, 
          then comments, while preserving the ability to switch tabs.
        </p>
        <CodeBlock code={nestedStacksCode} language="kotlin" />
        <p>
          Each tab's stack operates independently. Navigating within the Home tab doesn't 
          affect the Search tab's state, and vice versa.
        </p>
      </section>

      <section>
        <h2 id="icon-support">Icon Platform Support</h2>
        <p>
          The <code>icon</code> property in <code>@TabItem</code> is interpreted differently per platform:
        </p>
        <table>
          <thead>
            <tr>
              <th>Platform</th>
              <th>Interpretation</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Android</td>
              <td>Material icon name or drawable resource</td>
            </tr>
            <tr>
              <td>iOS</td>
              <td>SF Symbol name</td>
            </tr>
            <tr>
              <td>Desktop/Web</td>
              <td>Icon library identifier</td>
            </tr>
          </tbody>
        </table>
        <p>
          Your <code>@TabsContainer</code> implementation is responsible for mapping these 
          identifiers to actual icons using platform-appropriate APIs.
        </p>
      </section>

      <section>
        <h2 id="shared-state">Shared State with SharedNavigationContainer</h2>
        <p>
          When tabs need to share state (like a shopping cart count or user preferences), 
          use <code>SharedNavigationContainer</code> with the FlowMVI integration. The container 
          is scoped to the tab node's lifecycle.
        </p>
        <CodeBlock code={sharedStateCode} language="kotlin" />
        <p>
          Screens within any tab can access the shared store via the CompositionLocal, 
          enabling coordinated state across the entire tab structure.
        </p>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/adaptive-panes">Adaptive Panes</a> - Multi-pane layouts for larger screens</li>
          <li><a href="/features/mvi-integration">MVI Integration</a> - State management with FlowMVI</li>
          <li><a href="/features/transitions">Transitions</a> - Animate between tabs and screens</li>
          <li><a href="/demo">Live Demo</a> - See tabbed navigation in action</li>
        </ul>
      </section>
    </article>
  )
}
