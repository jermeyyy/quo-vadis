import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { TransitionTypesDisplay } from '@components/TransitionTypesDisplay'
import {
  stackDestinationComprehensive,
  screenBindingBasic,
  tabsAnnotationBasic,
  tabsContainerWrapper,
  paneAnnotationBasic,
  paneContainerWrapper,
  generatedConfigUsage,
} from '@data/codeExamples'
import styles from '../Features.module.css'

const stackExample = `@Stack(name = "home", startDestination = HomeDestination.Feed::class)
sealed class HomeDestination : NavDestination {
    // Destinations defined here
}`

const argumentExample = `@Destination(route = "products/detail/{id}")
data class ProductDetail(
    @Argument val id: String,                              // Required argument
    @Argument(optional = true) val referrer: String? = null,  // Optional argument
    @Argument(key = "show") val showReviews: Boolean = false  // Custom URL key
) : ProductsDestination()`

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

const errorFormatExample = `{Description} in file '{fileName}' (line {lineNumber}). Fix: {Suggestion}`

const errorMessageExample = `Missing @Screen binding for 'HomeDestination.Feed' in file 'HomeDestination.kt' (line 12). 
Fix: Add a @Composable function annotated with @Screen(HomeDestination.Feed::class)`

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
          <a href="#validation" className={styles.annotationCard}>
            <h4>Validation</h4>
            <p>Compile-time validation rules and error messages for all annotations.</p>
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

        <CodeBlock code={stackDestinationComprehensive} language="kotlin" />

        <h3>Route Patterns</h3>
        <ul>
          <li><strong>Static route:</strong> <code>"home/feed"</code> — No parameters</li>
          <li><strong>Path parameter:</strong> <code>"article/&#123;articleId&#125;"</code> — Required parameter in URL path</li>
          <li><strong>Multiple parameters:</strong> <code>"user/&#123;userId&#125;/post/&#123;postId&#125;"</code> — Multiple path params</li>
          <li><strong>Empty route:</strong> Omit or use <code>""</code> for destinations that aren't deep-linkable</li>
        </ul>
        <p style={{ marginTop: '0.75rem', fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>
          See <Link to="/features/deep-links">Deep Linking</Link> for advanced route configuration and URL handling.
        </p>
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

        <CodeBlock code={screenBindingBasic} language="kotlin" />

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
          Tab UI customization (labels, icons) is done in the <code>@TabsContainer</code> wrapper using type-safe pattern matching.
        </p>

        <CodeBlock code={tabsAnnotationBasic} language="kotlin" />

        <h3>@Tabs Properties</h3>
        <ul>
          <li><code>name: String</code> — Unique identifier for the tab container</li>
          <li><code>initialTab: KClass&lt;*&gt;</code> — The initially selected tab</li>
          <li><code>items: Array&lt;KClass&lt;*&gt;&gt;</code> — Tab classes in display order</li>
        </ul>

        <h3>@TabItem</h3>
        <p>
          <code>@TabItem</code> is a marker annotation with no parameters. 
          Labels, icons, and other UI customization are handled in the <code>@TabsContainer</code> wrapper 
          via pattern matching on the destination types.
        </p>
      </section>

      <section>
        <h2 id="tabs-container-annotation">@TabsContainer Annotation</h2>
        <p>
          Define a custom tab bar UI with <code>@TabsContainer</code>. 
          The composable receives a scope with tab destinations that you can pattern match 
          to customize labels, icons, and behavior for each tab.
        </p>

        <CodeBlock code={tabsContainerWrapper} language="kotlin" />

        <h3>TabsContainerScope</h3>
        <ul>
          <li><code>tabs: List&lt;NavDestination&gt;</code> — Tab destinations for custom labels, icons, and behavior</li>
          <li><code>activeTabIndex: Int</code> — Currently selected tab index</li>
          <li><code>switchTab(index: Int)</code> — Function to switch to a different tab</li>
          <li><code>isTransitioning: Boolean</code> — Whether tab switching animation is in progress</li>
        </ul>
      </section>

      <section>
        <h2 id="pane-annotations">@Pane and @PaneItem Annotations</h2>
        <p>
          For adaptive multi-pane layouts that adjust based on screen size. 
          Use <code>@Pane</code> on the sealed class and <code>@PaneItem</code> on each pane destination.
        </p>

        <CodeBlock code={paneAnnotationBasic} language="kotlin" />

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

        <CodeBlock code={paneContainerWrapper} language="kotlin" />
      </section>

      <section>
        <h2 id="transition-annotation">@Transition Annotation</h2>
        <p>
          Specify transition animations for destinations with <code>@Transition</code>. 
          Different destinations can have different transition styles.
        </p>

        <CodeBlock code={transitionExample} language="kotlin" />

        <h3>TransitionType Options</h3>
        <TransitionTypesDisplay 
          variant="grid" 
          transitions={[
            { name: 'SlideHorizontal', description: 'Platform-like horizontal slide (default)' },
            { name: 'SlideVertical', description: 'Bottom-to-top for modals' },
            { name: 'Fade', description: 'Simple crossfade' },
            { name: 'None', description: 'Instant switch' },
            { name: 'Custom', description: 'User-defined animation' },
          ]}
        />
        <p style={{ marginTop: '0.75rem', fontSize: '0.9rem', color: 'var(--color-text-secondary)' }}>
          For detailed animation customization, see <Link to="/features/transitions">Transitions</Link>.
        </p>
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

        <CodeBlock code={generatedConfigUsage} language="kotlin" />
      </section>

      <section>
        <h2 id="complete-example">Complete Example</h2>
        <p>
          Here's a complete example combining multiple annotation types:
        </p>

        <CodeBlock code={completeExample} language="kotlin" />
      </section>

      <section>
        <h2 id="validation">Validation & Error Messages</h2>
        <p>
          Quo Vadis validates annotation usage at compile time. When validation fails,
          the build fails with clear error messages that include:
        </p>
        <ul>
          <li><strong>Location:</strong> File name and line number</li>
          <li><strong>Problem:</strong> What's wrong</li>
          <li><strong>Fix:</strong> How to resolve the issue</li>
        </ul>

        <h3>Error Message Format</h3>
        <p>All validation messages follow this consistent format:</p>
        <CodeBlock code={errorFormatExample} language="text" />

        <p>Example error message:</p>
        <CodeBlock code={errorMessageExample} language="text" />

        <h3>@Stack Validation</h3>
        <table>
          <thead>
            <tr>
              <th>Rule</th>
              <th>Severity</th>
              <th>Example Message</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Must be sealed class</td>
              <td>Error</td>
              <td><code>@Stack 'HomeStack' must be a sealed class in file 'HomeStack.kt' (line 5). Fix: Change 'class HomeStack' to 'sealed class HomeStack'</code></td>
            </tr>
            <tr>
              <td>Start destination must exist</td>
              <td>Error</td>
              <td><code>Invalid startDestination 'InvalidScreen' for @Stack 'homeStack' in file 'HomeStack.kt' (line 5). Fix: Use one of the available destinations: [Feed, Profile, Settings]</code></td>
            </tr>
            <tr>
              <td>Must have destinations</td>
              <td>Error</td>
              <td><code>@Stack 'EmptyStack' has no destinations in file 'EmptyStack.kt' (line 3). Fix: Add at least one @Destination annotated subclass inside this sealed class</code></td>
            </tr>
          </tbody>
        </table>

        <h3>@Destination Validation</h3>
        <table>
          <thead>
            <tr>
              <th>Rule</th>
              <th>Severity</th>
              <th>Example Message</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Must be data object/class</td>
              <td>Error</td>
              <td><code>@Destination 'InvalidDest' must be a data object or data class in file 'Destinations.kt' (line 10). Fix: Use 'data object InvalidDest' or 'data class InvalidDest(...)'</code></td>
            </tr>
            <tr>
              <td>Route param requires constructor param</td>
              <td>Error</td>
              <td><code>Route param '&#123;userId&#125;' in @Destination on UserProfile has no matching constructor parameter in file 'UserProfile.kt' (line 8). Fix: Add a constructor parameter named 'userId' or remove '&#123;userId&#125;' from the route</code></td>
            </tr>
            <tr>
              <td>@Argument param must be in route</td>
              <td>Error</td>
              <td><code>@Argument param 'extraData' in UserProfile is not in route pattern 'user/&#123;userId&#125;' in file 'UserProfile.kt' (line 8). Fix: Add '&#123;extraData&#125;' to the route pattern or remove @Argument annotation</code></td>
            </tr>
            <tr>
              <td>Duplicate routes</td>
              <td>Error</td>
              <td><code>Duplicate route 'home/feed' - also used by: FeedScreen in file 'Feed.kt' (line 12). Fix: Use a unique route pattern for this destination</code></td>
            </tr>
            <tr>
              <td>Must have @Screen binding</td>
              <td>Error</td>
              <td><code>Missing @Screen binding for 'HomeDestination.Feed' in file 'HomeDestination.kt' (line 12). Fix: Add a @Composable function annotated with @Screen(HomeDestination.Feed::class)</code></td>
            </tr>
          </tbody>
        </table>
        <p>
          <strong>Note:</strong> Constructor parameters without <code>@Argument</code> annotation are not 
          required to be in the route. They can be passed programmatically (not via deep links).
        </p>

        <h3>@Argument Validation</h3>
        <table>
          <thead>
            <tr>
              <th>Rule</th>
              <th>Severity</th>
              <th>Example Message</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Optional requires default</td>
              <td>Error</td>
              <td><code>@Argument(optional = true) on 'page' in SearchResults requires a default value in file 'Search.kt' (line 15). Fix: Add a default value: page: Int = defaultValue</code></td>
            </tr>
            <tr>
              <td>Path param cannot be optional</td>
              <td>Error</td>
              <td><code>Path parameter '&#123;userId&#125;' in UserProfile cannot be optional in file 'UserProfile.kt' (line 8). Fix: Move this parameter to query parameters (after '?') or remove @Argument(optional = true)</code></td>
            </tr>
            <tr>
              <td>Duplicate argument keys</td>
              <td>Error</td>
              <td><code>Duplicate argument key 'id' in ArticleDetail in file 'Article.kt' (line 20). Fix: Use unique keys for each @Argument parameter</code></td>
            </tr>
            <tr>
              <td>Key not in route</td>
              <td>Error</td>
              <td><code>@Argument key 'userId' on 'user' in Profile is not found in route pattern 'profile/&#123;profileId&#125;' in file 'Profile.kt' (line 10). Fix: Add '&#123;userId&#125;' to the route pattern, or change the argument key to match: [profileId]</code></td>
            </tr>
          </tbody>
        </table>

        <h3>@Screen Validation</h3>
        <table>
          <thead>
            <tr>
              <th>Rule</th>
              <th>Severity</th>
              <th>Example Message</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Invalid destination reference</td>
              <td>Error</td>
              <td><code>@Screen(InvalidClass::class) references a class without @Destination in file 'Screens.kt' (line 25). Fix: Add @Destination annotation to InvalidClass or reference a valid destination</code></td>
            </tr>
            <tr>
              <td>Duplicate screen bindings</td>
              <td>Error</td>
              <td><code>Multiple @Screen bindings for HomeDestination.Feed: FeedScreen, FeedScreenDuplicate in file 'Screens.kt' (line 30). Fix: Keep only one @Screen function for this destination</code></td>
            </tr>
          </tbody>
        </table>

        <h3>@Tabs Validation</h3>
        <table>
          <thead>
            <tr>
              <th>Rule</th>
              <th>Severity</th>
              <th>Example Message</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Must be sealed class</td>
              <td>Error</td>
              <td><code>@Tabs 'MainTabs' must be a sealed class in file 'MainTabs.kt' (line 5). Fix: Change 'class MainTabs' to 'sealed class MainTabs'</code></td>
            </tr>
            <tr>
              <td>Invalid initial tab</td>
              <td>Error</td>
              <td><code>Invalid initialTab 'InvalidTab' for @Tabs 'mainTabs' in file 'MainTabs.kt' (line 5). Fix: Use one of the available tabs: [HomeTab, ProfileTab, SettingsTab]</code></td>
            </tr>
            <tr>
              <td>Empty tabs</td>
              <td>Error</td>
              <td><code>@Tabs container 'EmptyTabs' has no @TabItem entries in file 'EmptyTabs.kt' (line 3). Fix: Add at least one @TabItem annotated class to the items array</code></td>
            </tr>
          </tbody>
        </table>

        <h3>@TabItem Validation</h3>
        <table>
          <thead>
            <tr>
              <th>Rule</th>
              <th>Severity</th>
              <th>Example Message</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Must have @Stack or @Destination</td>
              <td>Error</td>
              <td><code>@TabItem 'HomeTab' has neither @Stack nor @Destination in file 'Tabs.kt' (line 15). Fix: Add @Stack for nested navigation or @Destination for flat screen</code></td>
            </tr>
            <tr>
              <td>Cannot have both @Stack and @Destination</td>
              <td>Error</td>
              <td><code>@TabItem 'InvalidTab' has both @Stack and @Destination in file 'Tabs.kt' (line 20). Fix: Use @Stack for nested navigation OR @Destination for flat screen, not both</code></td>
            </tr>
          </tbody>
        </table>

        <h3>@Pane Validation</h3>
        <table>
          <thead>
            <tr>
              <th>Rule</th>
              <th>Severity</th>
              <th>Example Message</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Must be sealed class</td>
              <td>Error</td>
              <td><code>@Pane 'DetailPane' must be a sealed class in file 'DetailPane.kt' (line 5). Fix: Change 'class DetailPane' to 'sealed class DetailPane'</code></td>
            </tr>
            <tr>
              <td>Empty pane</td>
              <td>Error</td>
              <td><code>@Pane container 'EmptyPane' has no @PaneItem entries in file 'EmptyPane.kt' (line 3). Fix: Add at least one @PaneItem annotated class to the items array</code></td>
            </tr>
          </tbody>
        </table>
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
