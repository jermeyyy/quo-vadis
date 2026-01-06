import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const stackExample = `@Stack(name = "home", startDestination = HomeDestination.Feed::class)
sealed class HomeDestination : NavDestination {
    // Destinations defined here
}`

const destinationExample = `@Stack(name = "home", startDestination = HomeDestination.Feed::class)
sealed class HomeDestination : NavDestination {

    // Simple destination (no arguments)
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    // Destination with a path parameter
    @Destination(route = "home/article/{articleId}")
    data class Article(@Argument val articleId: String) : HomeDestination()

    // Destination with multiple parameters
    @Destination(route = "home/user/{userId}/post/{postId}")
    data class UserPost(
        @Argument val userId: String,
        @Argument val postId: String
    ) : HomeDestination()
}`

const argumentExample = `@Destination(route = "products/detail/{id}")
data class ProductDetail(
    @Argument val id: String,                              // Required argument
    @Argument(optional = true) val referrer: String? = null,  // Optional argument
    @Argument(key = "show") val showReviews: Boolean = false  // Custom URL key
) : ProductsDestination()`

const screenExample = `// Simple destination (data object) - navigator only
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    Column {
        Text("Feed")
        Button(onClick = { navigator.navigate(HomeDestination.Article("123")) }) {
            Text("View Article")
        }
    }
}

// Destination with arguments (data class) - access destination data
@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(
    destination: HomeDestination.Article,
    navigator: Navigator
) {
    Column {
        Text("Article: \${destination.articleId}")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}`

const tabsExample = `@Tabs(
    name = "mainTabs",
    initialTab = MainTabs.HomeTab::class,
    items = [MainTabs.HomeTab::class, MainTabs.ExploreTab::class, MainTabs.ProfileTab::class]
)
sealed class MainTabs : NavDestination {

    @TabItem(label = "Home", icon = "home")
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    @TabItem(label = "Explore", icon = "explore")
    @Destination(route = "main/explore")
    data object ExploreTab : MainTabs()

    @TabItem(label = "Profile", icon = "person")
    @Destination(route = "main/profile")
    data object ProfileTab : MainTabs()
}`

const tabsContainerExample = `@TabsContainer(MainTabs::class)
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
                        icon = { Icon(getIcon(meta.icon), meta.label) },
                        label = { Text(meta.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) { content() }
    }
}`

const paneExample = `@Pane(name = "messagesPane", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class MessagesPane : NavDestination {

    @PaneItem(role = PaneRole.PRIMARY)
    @Destination(route = "messages/conversations")
    data object ConversationList : MessagesPane()

    @PaneItem(role = PaneRole.SECONDARY, adaptStrategy = AdaptStrategy.OVERLAY)
    @Destination(route = "messages/conversation/{id}")
    data class ConversationDetail(
        @Argument val id: String
    ) : MessagesPane()
}`

const paneContainerExample = `@PaneContainer(MessagesPane::class)
@Composable
fun MessagesPaneContainer(
    scope: PaneContainerScope,
    content: @Composable () -> Unit
) {
    if (scope.isExpanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            scope.paneContents.filter { it.isVisible }.forEach { pane ->
                Box(
                    modifier = Modifier.weight(
                        if (pane.role == PaneRole.PRIMARY) 0.4f else 0.6f
                    )
                ) {
                    pane.content()
                }
            }
        }
    } else {
        content()  // Single pane mode
    }
}`

const transitionExample = `@Transition(type = TransitionType.SlideHorizontal)
@Destination(route = "details/{id}")
data class Details(@Argument val id: String) : HomeDestination()

@Transition(type = TransitionType.SlideVertical)
@Destination(route = "modal")
data object Modal : HomeDestination()

@Transition(type = TransitionType.Fade)
@Destination(route = "help")
data object Help : HomeDestination()

@Transition(type = TransitionType.None)
@Destination(route = "instant")
data object InstantSwitch : HomeDestination()`

const generatedExample = `// Generated NavigationConfig usage
val navigator = TreeNavigator(
    config = GeneratedNavigationConfig,
    initialState = GeneratedNavigationConfig.buildNavNode(
        HomeDestination::class, 
        null
    )!!
)

// Type-safe navigation (generated)
navigator.navigate(HomeDestination.Article(articleId = "123"))
navigator.navigate(MainTabs.ProfileTab)`

const completeExample = `// Define a stack with destinations
@Stack(name = "app", startDestination = AppDestination.Main::class)
sealed class AppDestination : NavDestination {

    @Destination(route = "app/main")
    data object Main : AppDestination()

    @Transition(type = TransitionType.SlideHorizontal)
    @Destination(route = "app/detail/{itemId}")
    data class Detail(
        @Argument val itemId: String,
        @Argument(optional = true) val highlight: Boolean = false
    ) : AppDestination()

    @Transition(type = TransitionType.SlideVertical)
    @Destination(route = "app/settings")
    data object Settings : AppDestination()
}

// Bind screens to destinations
@Screen(AppDestination.Main::class)
@Composable
fun MainScreen(navigator: Navigator) {
    Column {
        Text("Welcome to the App")
        Button(onClick = { navigator.navigate(AppDestination.Detail("item-1")) }) {
            Text("View Details")
        }
        Button(onClick = { navigator.navigate(AppDestination.Settings) }) {
            Text("Settings")
        }
    }
}

@Screen(AppDestination.Detail::class)
@Composable
fun DetailScreen(destination: AppDestination.Detail, navigator: Navigator) {
    Column {
        Text("Item: \${destination.itemId}")
        if (destination.highlight) {
            Text("✨ Highlighted!")
        }
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

@Screen(AppDestination.Settings::class)
@Composable
fun SettingsScreen(navigator: Navigator) {
    Column {
        Text("Settings")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Close")
        }
    }
}

// Initialize navigation
@Composable
fun App() {
    val navigator = remember {
        TreeNavigator(
            config = GeneratedNavigationConfig,
            initialState = GeneratedNavigationConfig.buildNavNode(
                AppDestination::class, 
                null
            )!!
        )
    }
    
    NavigationHost(navigator = navigator)
}`

export default function AnnotationAPI() {
  return (
    <article className={styles.features}>
      <h1>Annotation-Based API</h1>
      <p className={styles.intro}>
        Quo Vadis uses a powerful annotation system to define your navigation structure with minimal boilerplate. 
        The KSP processor generates all the necessary code for type-safe navigation, deep linking, and screen binding.
      </p>

      <div className={styles.highlights}>
        <ul>
          <li><strong>Zero Boilerplate:</strong> Annotations generate graph builders, screen registries, and route handlers</li>
          <li><strong>Type-Safe Arguments:</strong> Automatic serialization with support for primitives, enums, and @Serializable classes</li>
          <li><strong>Deep Link Ready:</strong> Route patterns with path parameters are generated automatically</li>
          <li><strong>Compile-Time Safety:</strong> Catch navigation errors before runtime</li>
        </ul>
      </div>

      <section>
        <h2 id="core-annotations">Core Annotations</h2>
        <div className={styles.annotationGrid}>
          <a href="#stack-annotation" className={styles.annotationCard}>
            <h4>@Stack</h4>
            <p>Defines a navigation stack with push/pop behavior and a start destination.</p>
          </a>
          <a href="#destination-annotation" className={styles.annotationCard}>
            <h4>@Destination</h4>
            <p>Marks a class as a navigation destination with an optional route for deep linking.</p>
          </a>
          <a href="#argument-annotation" className={styles.annotationCard}>
            <h4>@Argument</h4>
            <p>Marks constructor parameters as navigation arguments with optional keys.</p>
          </a>
          <a href="#screen-annotation" className={styles.annotationCard}>
            <h4>@Screen</h4>
            <p>Binds a composable function to render a specific destination.</p>
          </a>
        </div>

        <div className={styles.annotationGrid}>
          <a href="#tabs-annotations" className={styles.annotationCard}>
            <h4>@Tabs / @TabItem</h4>
            <p>Creates tabbed navigation with independent backstacks per tab.</p>
          </a>
          <a href="#pane-annotations" className={styles.annotationCard}>
            <h4>@Pane / @PaneItem</h4>
            <p>Defines adaptive multi-pane layouts for different screen sizes.</p>
          </a>
          <a href="#transition-annotation" className={styles.annotationCard}>
            <h4>@Transition</h4>
            <p>Specifies transition animations for destination entries and exits.</p>
          </a>
          <a href="#tabs-container-annotation" className={styles.annotationCard}>
            <h4>@TabsContainer / @PaneContainer</h4>
            <p>Custom UI wrappers for tabs and pane layouts.</p>
          </a>
        </div>
      </section>

      <section>
        <h2 id="stack-annotation">@Stack Annotation</h2>
        <p>
          The <code>@Stack</code> annotation defines a navigation stack — a collection of destinations 
          that supports push and pop operations. Every navigation graph starts with a stack.
        </p>

        <CodeBlock code={stackExample} language="kotlin" />

        <h3>Properties</h3>
        <ul>
          <li><code>name: String</code> — Unique identifier for the stack</li>
          <li><code>startDestination: KClass&lt;*&gt;</code> — The initial destination when the stack is created</li>
        </ul>
      </section>

      <section>
        <h2 id="destination-annotation">@Destination Annotation</h2>
        <p>
          The <code>@Destination</code> annotation marks a class as a navigation destination. 
          Use <code>data object</code> for destinations without arguments and <code>data class</code> for those with arguments.
        </p>

        <CodeBlock code={destinationExample} language="kotlin" />

        <h3>Route Patterns</h3>
        <ul>
          <li><strong>Static route:</strong> <code>"home/feed"</code> — No parameters</li>
          <li><strong>Path parameter:</strong> <code>"article/&#123;articleId&#125;"</code> — Required parameter in URL path</li>
          <li><strong>Multiple parameters:</strong> <code>"user/&#123;userId&#125;/post/&#123;postId&#125;"</code> — Multiple path params</li>
          <li><strong>Empty route:</strong> Omit or use <code>""</code> for destinations that aren't deep-linkable</li>
        </ul>
      </section>

      <section>
        <h2 id="argument-annotation">@Argument Annotation</h2>
        <p>
          The <code>@Argument</code> annotation marks constructor parameters as navigation arguments. 
          These are automatically serialized for deep linking and state restoration.
        </p>

        <CodeBlock code={argumentExample} language="kotlin" />

        <h3>Properties</h3>
        <ul>
          <li><code>key: String</code> — Custom URL parameter key (defaults to property name)</li>
          <li><code>optional: Boolean</code> — Whether the argument can be omitted in deep links</li>
        </ul>

        <h3>Supported Types</h3>
        <table>
          <thead>
            <tr>
              <th>Type</th>
              <th>Example</th>
            </tr>
          </thead>
          <tbody>
            <tr><td>String</td><td><code>@Argument val id: String</code></td></tr>
            <tr><td>Int</td><td><code>@Argument val count: Int</code></td></tr>
            <tr><td>Long</td><td><code>@Argument val timestamp: Long</code></td></tr>
            <tr><td>Float</td><td><code>@Argument val rating: Float</code></td></tr>
            <tr><td>Double</td><td><code>@Argument val price: Double</code></td></tr>
            <tr><td>Boolean</td><td><code>@Argument val enabled: Boolean</code></td></tr>
            <tr><td>Enum</td><td><code>@Argument val status: OrderStatus</code></td></tr>
            <tr><td>@Serializable</td><td><code>@Argument val filter: FilterData</code></td></tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="screen-annotation">@Screen Annotation</h2>
        <p>
          The <code>@Screen</code> annotation binds a composable function to render a specific destination. 
          The function can receive the navigator and the destination instance as parameters.
        </p>

        <CodeBlock code={screenExample} language="kotlin" />

        <h3>Function Parameters</h3>
        <ul>
          <li><code>navigator: Navigator</code> — The navigation controller for performing navigation actions</li>
          <li><code>destination: T</code> — The destination instance (for data class destinations with arguments)</li>
        </ul>
      </section>

      <section>
        <h2 id="tabs-annotations">@Tabs and @TabItem Annotations</h2>
        <p>
          For tabbed navigation where each tab maintains its own backstack. 
          Use <code>@Tabs</code> on the sealed class and <code>@TabItem</code> on each tab destination.
        </p>

        <CodeBlock code={tabsExample} language="kotlin" />

        <h3>@Tabs Properties</h3>
        <ul>
          <li><code>name: String</code> — Unique identifier for the tab container</li>
          <li><code>initialTab: KClass&lt;*&gt;</code> — The initially selected tab</li>
          <li><code>items: Array&lt;KClass&lt;*&gt;&gt;</code> — Tab classes in display order</li>
        </ul>

        <h3>@TabItem Properties</h3>
        <ul>
          <li><code>label: String</code> — Display label for the tab</li>
          <li><code>icon: String</code> — Icon identifier for the tab</li>
        </ul>
      </section>

      <section>
        <h2 id="tabs-container-annotation">@TabsContainer Annotation</h2>
        <p>
          Define a custom tab bar UI with <code>@TabsContainer</code>. 
          The composable receives a scope with tab metadata and switching functionality.
        </p>

        <CodeBlock code={tabsContainerExample} language="kotlin" />

        <h3>TabsContainerScope</h3>
        <ul>
          <li><code>tabMetadata: List&lt;TabMetadata&gt;</code> — Label and icon info for each tab</li>
          <li><code>activeTabIndex: Int</code> — Currently selected tab index</li>
          <li><code>switchTab(index: Int)</code> — Function to switch to a different tab</li>
        </ul>
      </section>

      <section>
        <h2 id="pane-annotations">@Pane and @PaneItem Annotations</h2>
        <p>
          For adaptive multi-pane layouts that adjust based on screen size. 
          Use <code>@Pane</code> on the sealed class and <code>@PaneItem</code> on each pane destination.
        </p>

        <CodeBlock code={paneExample} language="kotlin" />

        <h3>@Pane Properties</h3>
        <ul>
          <li><code>name: String</code> — Unique identifier for the pane container</li>
          <li><code>backBehavior: PaneBackBehavior</code> — Back navigation strategy</li>
        </ul>

        <h3>@PaneItem Properties</h3>
        <ul>
          <li><code>role: PaneRole</code> — <code>PRIMARY</code>, <code>SECONDARY</code>, or <code>EXTRA</code></li>
          <li><code>adaptStrategy: AdaptStrategy</code> — <code>HIDE</code>, <code>COLLAPSE</code>, <code>OVERLAY</code>, or <code>REFLOW</code></li>
        </ul>
      </section>

      <section>
        <h2 id="pane-container-annotation">@PaneContainer Annotation</h2>
        <p>
          Define custom pane layout behavior with <code>@PaneContainer</code>. 
          Control how panes are arranged based on screen size.
        </p>

        <CodeBlock code={paneContainerExample} language="kotlin" />
      </section>

      <section>
        <h2 id="transition-annotation">@Transition Annotation</h2>
        <p>
          Specify transition animations for destinations with <code>@Transition</code>. 
          Different destinations can have different transition styles.
        </p>

        <CodeBlock code={transitionExample} language="kotlin" />

        <h3>TransitionType Options</h3>
        <div className={styles.transitionGrid}>
          <div className={styles.transitionCard}>
            <h4>SlideHorizontal</h4>
            <p>Platform-like horizontal slide (default)</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>SlideVertical</h4>
            <p>Bottom-to-top for modals</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>Fade</h4>
            <p>Simple crossfade</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>None</h4>
            <p>Instant switch</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>Custom</h4>
            <p>User-defined animation</p>
          </div>
        </div>
      </section>

      <section>
        <h2 id="what-gets-generated">What Gets Generated</h2>
        <p>
          The KSP processor generates several components from your annotated classes:
        </p>

        <ul>
          <li><strong>NavigationConfig:</strong> Central configuration object containing all routes and screen mappings</li>
          <li><strong>ScreenRegistry:</strong> Maps destinations to their composable screens</li>
          <li><strong>RouteParser:</strong> Handles deep link parsing and argument extraction</li>
          <li><strong>NavNode Builders:</strong> Functions to construct the navigation tree</li>
          <li><strong>Type-safe Extensions:</strong> Generated <code>navigate()</code> extensions for each destination</li>
        </ul>

        <CodeBlock code={generatedExample} language="kotlin" />
      </section>

      <section>
        <h2 id="complete-example">Complete Example</h2>
        <p>
          Here's a complete example combining multiple annotation types:
        </p>

        <CodeBlock code={completeExample} language="kotlin" />
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/getting-started">Get started</Link> with the quick start guide</li>
          <li><Link to="/features/type-safe">Type-Safe Navigation</Link> — Learn about programmatic destination building</li>
          <li><Link to="/features/deep-linking">Deep Linking</Link> — Configure deep link handling</li>
          <li><Link to="/features/transitions">Transitions</Link> — Customize navigation animations</li>
          <li><Link to="/demo">See the demo</Link> to explore features in action</li>
        </ul>
      </section>
    </article>
  )
}
