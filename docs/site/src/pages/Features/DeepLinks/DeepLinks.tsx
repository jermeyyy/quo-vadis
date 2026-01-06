import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const definingRoutesCode = `@Stack(name = "home", startDestination = Feed::class)
sealed class HomeDestination : NavDestination {

    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    // Path parameters
    @Destination(route = "home/article/{articleId}")
    data class Article(
        @Argument val articleId: String
    ) : HomeDestination()

    // Multiple path parameters
    @Destination(route = "home/user/{userId}/post/{postId}")
    data class UserPost(
        @Argument val userId: String,
        @Argument val postId: String
    ) : HomeDestination()

    // Query parameters
    @Destination(route = "home/search")
    data class Search(
        @Argument val query: String,
        @Argument(optional = true) val page: Int = 1,
        @Argument(optional = true) val sortAsc: Boolean = true
    ) : HomeDestination()
}`

const argumentAnnotationCode = `@Destination(route = "products/detail/{id}")
data class Detail(
    @Argument val id: String,                    // Required, maps to {id}
    @Argument(key = "ref") val referrer: String? = null,  // Custom key
    @Argument(optional = true) val showReviews: Boolean = false  // Optional
) : ProductsDestination()

// Deep link: app://products/detail/123?ref=home&showReviews=true`

const handlingDeepLinksCode = `// Handle incoming URI string
val handled = navigator.handleDeepLink("app://home/article/42")

if (!handled) {
    showError("Unknown deep link")
}

// Using DeepLink object
val deepLink = DeepLink.parse("app://home/search?query=kotlin&page=2")
navigator.handleDeepLink(deepLink)`

const runtimeRegistrationCode = `@Composable
fun MyScreen() {
    LaunchedEffect(Unit) {
        navigator.getDeepLinkRegistry().register("promo/{code}") { params ->
            PromoDestination(code = params["code"]!!)
        }
    }
}

// Now "app://promo/SAVE20" navigates to PromoDestination("SAVE20")`

const androidManifestCode = `<!-- AndroidManifest.xml -->
<activity ...>
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="myapp" />
        <data android:scheme="https" android:host="myapp.com" />
    </intent-filter>
</activity>`

const androidActivityCode = `// Handle in Activity
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent?.data?.let { uri ->
        navigator.handleDeepLink(uri.toString())
    }
}`

const iosUniversalLinksCode = `// SceneDelegate or App
func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
    if let url = userActivity.webpageURL {
        navigator.handleDeepLink(url: url.absoluteString)
    }
}`

const webBrowserHistoryCode = `// Web target - browser URL sync
NavigationHost(
    navigator = navigator,
    enableBrowserHistory = true  // Syncs with browser URL
)`

const deepLinkRegistryCode = `interface DeepLinkRegistry {
    fun register(route: String, factory: (Map<String, String>) -> NavDestination)
    fun unregister(route: String)
    fun matchRoute(uri: String): NavDestination?
}`

export default function DeepLinks() {
  return (
    <article className={styles.features}>
      <h1>Deep Link Support</h1>
      <p className={styles.intro}>
        Deep linking enables URI-based navigation with automatic parameter extraction 
        using route patterns defined in <code>@Destination</code> annotations.
      </p>

      <section>
        <h2 id="defining-deep-link-routes">Defining Deep Link Routes</h2>
        <p>
          Define deep link routes using the <code>route</code> parameter in your{' '}
          <code>@Destination</code> annotations. Path parameters are enclosed in curly 
          braces and automatically extracted during navigation.
        </p>
        <CodeBlock code={definingRoutesCode} language="kotlin" />
      </section>

      <section>
        <h2 id="route-pattern-syntax">Route Pattern Syntax</h2>
        <p>
          Quo Vadis supports flexible route patterns for matching incoming deep links:
        </p>
        <table>
          <thead>
            <tr>
              <th>Pattern</th>
              <th>Example</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Static</td>
              <td><code>"home"</code></td>
              <td>Exact match</td>
            </tr>
            <tr>
              <td>Path parameter</td>
              <td><code>"article/{'{articleId}'}"</code></td>
              <td>Extracts value from path</td>
            </tr>
            <tr>
              <td>Multiple params</td>
              <td><code>"user/{'{userId}'}/post/{'{postId}'}"</code></td>
              <td>Multiple path segments</td>
            </tr>
            <tr>
              <td>Query params</td>
              <td><code>"search?q={'{query}'}"</code></td>
              <td>Query string parameters</td>
            </tr>
            <tr>
              <td>Optional</td>
              <td>(with <code>@Argument(optional = true)</code>)</td>
              <td>Can be omitted</td>
            </tr>
            <tr>
              <td>Not deep-linkable</td>
              <td>(empty route)</td>
              <td>Internal destination only</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="argument-annotation">@Argument for Deep Links</h2>
        <p>
          The <code>@Argument</code> annotation configures how parameters are extracted 
          from deep links and serialized for navigation state.
        </p>
        <CodeBlock code={argumentAnnotationCode} language="kotlin" />
      </section>

      <section>
        <h2 id="supported-argument-types">Supported Argument Types</h2>
        <p>
          Quo Vadis automatically handles serialization and deserialization for common types:
        </p>
        <table>
          <thead>
            <tr>
              <th>Type</th>
              <th>Serialization</th>
              <th>Notes</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>String</code></td>
              <td>Direct</td>
              <td>No conversion</td>
            </tr>
            <tr>
              <td><code>Int</code>, <code>Long</code>, <code>Float</code>, <code>Double</code></td>
              <td><code>.toString()</code> / <code>.toXxx()</code></td>
              <td>Numeric conversion</td>
            </tr>
            <tr>
              <td><code>Boolean</code></td>
              <td><code>"true"</code> / <code>"false"</code></td>
              <td>Case-insensitive</td>
            </tr>
            <tr>
              <td><code>Enum&lt;T&gt;</code></td>
              <td><code>.name</code> / <code>enumValueOf()</code></td>
              <td>Enum name</td>
            </tr>
            <tr>
              <td><code>@Serializable</code></td>
              <td>JSON</td>
              <td>kotlinx.serialization</td>
            </tr>
            <tr>
              <td><code>List&lt;T&gt;</code>, <code>Set&lt;T&gt;</code></td>
              <td>JSON</td>
              <td>Where T is serializable</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="handling-deep-links">Handling Deep Links</h2>
        <p>
          Process incoming deep links using the navigator's <code>handleDeepLink</code> method. 
          It returns a boolean indicating whether the link was successfully matched.
        </p>
        <CodeBlock code={handlingDeepLinksCode} language="kotlin" />
      </section>

      <section>
        <h2 id="runtime-pattern-registration">Runtime Pattern Registration</h2>
        <p>
          Register additional deep link patterns at runtime for dynamic routing scenarios 
          like promotional campaigns or A/B testing.
        </p>
        <CodeBlock code={runtimeRegistrationCode} language="kotlin" />
      </section>

      <section>
        <h2 id="android-setup">Android Deep Link Setup</h2>
        <p>
          Configure intent filters in your Android manifest to handle incoming deep links:
        </p>
        <CodeBlock code={androidManifestCode} language="xml" />
        <p>Handle the incoming intent in your Activity:</p>
        <CodeBlock code={androidActivityCode} language="kotlin" />
      </section>

      <section>
        <h2 id="ios-universal-links">iOS Universal Links</h2>
        <p>
          Handle universal links in your iOS app's SceneDelegate or App struct:
        </p>
        <CodeBlock code={iosUniversalLinksCode} language="swift" />
      </section>

      <section>
        <h2 id="web-browser-history">Web Browser History</h2>
        <p>
          On web targets, enable browser URL synchronization to keep the address bar 
          in sync with navigation state:
        </p>
        <CodeBlock code={webBrowserHistoryCode} language="kotlin" />
      </section>

      <section>
        <h2 id="deep-link-registry">DeepLinkRegistry Interface</h2>
        <p>
          The <code>DeepLinkRegistry</code> provides programmatic control over deep link 
          pattern registration:
        </p>
        <CodeBlock code={deepLinkRegistryCode} language="kotlin" />
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <ul>
          <li><strong>Consistent route naming:</strong> Use <code>feature/action/{'{param}'}</code> pattern</li>
          <li><strong>Required vs optional:</strong> Make navigation-critical params required, UI-only params optional</li>
          <li><strong>Test thoroughly:</strong> Test deep links with various parameter combinations</li>
          <li><strong>Handle gracefully:</strong> Always handle invalid or unknown deep links with appropriate fallbacks</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/features/multiplatform">Multiplatform Support</Link> - Deep links on all platforms</li>
          <li><Link to="/features/type-safety">Type Safety</Link> - Type-safe argument handling</li>
          <li><Link to="/getting-started">Get started</Link> with deep link setup</li>
        </ul>
      </section>
    </article>
  )
}
