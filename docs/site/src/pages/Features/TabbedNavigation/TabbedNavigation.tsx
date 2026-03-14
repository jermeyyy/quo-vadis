import CodeBlock from '@components/CodeBlock/CodeBlock'
import { ScopePropertiesTable } from '@components/ScopePropertiesTable/ScopePropertiesTable'
import { tabsAnnotationWithNestedStack, crossModuleTabsExample } from '@data/codeExamples'
import styles from '../Features.module.css'

// Status icon for required/no default values in tables
const IconNa = () => <span className={`${styles.statusIcon} ${styles.statusIconNa}`} />

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

const tabUiCustomizationCode = `@TabsContainer(AppTabs::class)
@Composable
fun AppTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                scope.tabMetadata.forEachIndexed { index, metadata ->
                    NavigationBarItem(
                        selected = index == scope.activeTabIndex,
                        onClick = { scope.switchTab(index) },
                        icon = { /* Use metadata.destination for type matching */ },
                        label = { /* Tab label */ }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) { content() }
    }
}`

const nestedStacksCode = `@Tabs(name = "appTabs")
class AppTabs : NavDestination {
    companion object : NavDestination
}

@TabItem(parent = AppTabs::class, ordinal = 0)
@Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
sealed class HomeTab : NavDestination {
    @Destination(route = "home/feed")
    data object Feed : HomeTab()

    @Destination(route = "home/article/{articleId}")
    data class Article(@Argument val articleId: String) : HomeTab()
}

@TabItem(parent = AppTabs::class, ordinal = 1)
@Stack(name = "searchStack", startDestination = SearchTab.Search::class)
sealed class SearchTab : NavDestination {
    @Destination(route = "search")
    data object Search : SearchTab()

    @Destination(route = "search/results/{query}")
    data class Results(@Argument val query: String) : SearchTab()
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
          <li><strong>Cross-Module Tabs</strong> - Feature modules register as tabs independently</li>
        </ul>
      </section>

      <section>
        <h2 id="tabs-annotation">@Tabs + @TabItem Annotations</h2>
        <p>
          Define your tab structure using <code>@Tabs</code> on a sealed class and <code>@TabItem</code> 
          on each tab destination. The KSP processor generates all configuration code automatically.
        </p>
        <CodeBlock code={tabsAnnotationWithNestedStack} language="kotlin" />
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
              <td><IconNa /></td>
              <td>Unique identifier for the tab container</td>
            </tr>
          </tbody>
        </table>
        <p>
          The initial tab and ordering are determined by <code>@TabItem</code> ordinals.
        </p>
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
              <td><code>parent</code></td>
              <td><code>KClass&lt;*&gt;</code></td>
              <td><IconNa /></td>
              <td>The <code>@Tabs</code>-annotated class this tab belongs to</td>
            </tr>
            <tr>
              <td><code>ordinal</code></td>
              <td><code>Int</code></td>
              <td><IconNa /></td>
              <td>Display position (0-based). <code>ordinal = 0</code> is the initial tab.</td>
            </tr>
          </tbody>
        </table>
        <h3>Ordinal Validation Rules</h3>
        <ul>
          <li>Ordinals must be contiguous integers starting at 0 (e.g., 0, 1, 2, …, N-1)</li>
          <li>No gaps allowed (e.g., 0, 1, 3 is invalid because 2 is missing)</li>
          <li>No duplicate ordinals</li>
          <li>KSP validates these rules at compile time</li>
          <li>Cross-module ordinal validation is relaxed (only partial tab items visible to the processor)</li>
        </ul>
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

      <section id="container-scope">
        <ScopePropertiesTable scopeType="tabs" />
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
        <h2 id="tab-ui-customization">Tab UI Customization</h2>
        <p>
          Icons, labels, and all tab bar UI are handled in your <code>@TabsContainer</code> wrapper
          composable. Use <code>scope.tabMetadata</code> and destination type matching to dynamically
          assign icons and labels, giving you full control over tab bar appearance per platform.
        </p>
        <CodeBlock code={tabUiCustomizationCode} language="kotlin" />
        <p>
          This approach decouples visual presentation from navigation structure, letting you
          customize tab appearance freely without modifying annotations.
        </p>
      </section>

      <section>
        <h2 id="cross-module-tabs">Cross-Module Tabs</h2>
        <p>
          Feature modules can register as tabs independently using{' '}
          <code>@TabItem(parent = SharedTabs::class, ordinal = N)</code>. The shared{' '}
          <code>@Tabs</code> class lives in a common API module, and each feature module
          declares itself as a tab item pointing back to it. Ordinal validation is relaxed
          for cross-module scenarios where only partial tab items are visible to the processor.
        </p>
        <CodeBlock code={crossModuleTabsExample} language="kotlin" />
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
