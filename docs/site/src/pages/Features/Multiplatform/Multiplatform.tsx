import styles from '../Features.module.css'
import CodeBlock from '@components/CodeBlock/CodeBlock'

export default function Multiplatform() {
  return (
    <article className={styles.features}>
      <h1>Multiplatform Support</h1>
      <p className={styles.intro}>
        Quo Vadis is built on Kotlin Multiplatform and Compose Multiplatform, providing identical 
        navigation behavior across all platforms from a single codebase.
      </p>

      <section>
        <h2 id="platform-support">Platform Support</h2>
        <p>
          Quo Vadis supports all major Kotlin Multiplatform targets with full feature parity:
        </p>
        
        <table>
          <thead>
            <tr>
              <th>Platform</th>
              <th>Target</th>
              <th>Status</th>
              <th>Notes</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Android</td>
              <td><code>android</code></td>
              <td>✅ Full</td>
              <td>API 21+</td>
            </tr>
            <tr>
              <td>iOS</td>
              <td><code>iosArm64</code>, <code>iosSimulatorArm64</code>, <code>iosX64</code></td>
              <td>✅ Full</td>
              <td>iOS 14+</td>
            </tr>
            <tr>
              <td>Desktop</td>
              <td><code>jvm</code></td>
              <td>✅ Full</td>
              <td>macOS, Windows, Linux</td>
            </tr>
            <tr>
              <td>JavaScript</td>
              <td><code>js</code> (IR)</td>
              <td>✅ Full</td>
              <td>Browser target</td>
            </tr>
            <tr>
              <td>WebAssembly</td>
              <td><code>wasmJs</code></td>
              <td>✅ Full</td>
              <td>WASM browser target</td>
            </tr>
          </tbody>
        </table>

        <div className={styles.badges}>
          <span className={styles.badge}>Android</span>
          <span className={styles.badge}>iOS</span>
          <span className={styles.badge}>Desktop (JVM)</span>
          <span className={styles.badge}>JavaScript (IR)</span>
          <span className={styles.badge}>WebAssembly</span>
        </div>
      </section>

      <section>
        <h2 id="single-codebase">Single Codebase Benefits</h2>
        <p>
          Write your navigation logic once in <code>commonMain</code> and it runs identically 
          on all platforms:
        </p>
        
        <CodeBlock language="kotlin" code={`// commonMain - shared across ALL platforms
@Stack(name = "home", startDestination = Feed::class)
sealed class HomeDestination : NavDestination {
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
}

@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    // Same code runs on Android, iOS, Desktop, Web
}`} />
      </section>

      <section>
        <h2 id="platform-features">Platform-Specific Features</h2>
        <p>
          While core navigation works identically everywhere, each platform has its own 
          native capabilities:
        </p>
        
        <table>
          <thead>
            <tr>
              <th>Feature</th>
              <th>Android</th>
              <th>iOS</th>
              <th>Desktop</th>
              <th>Web</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Stack navigation</td>
              <td>✅</td>
              <td>✅</td>
              <td>✅</td>
              <td>✅</td>
            </tr>
            <tr>
              <td>Tab navigation</td>
              <td>✅</td>
              <td>✅</td>
              <td>✅</td>
              <td>✅</td>
            </tr>
            <tr>
              <td>Pane layouts</td>
              <td>✅</td>
              <td>✅</td>
              <td>✅</td>
              <td>✅</td>
            </tr>
            <tr>
              <td>Deep links</td>
              <td>✅</td>
              <td>✅</td>
              <td>⚡</td>
              <td>✅</td>
            </tr>
            <tr>
              <td>Predictive back</td>
              <td>✅</td>
              <td>✅</td>
              <td>—</td>
              <td>—</td>
            </tr>
            <tr>
              <td>Shared elements</td>
              <td>✅</td>
              <td>✅</td>
              <td>✅</td>
              <td>✅</td>
            </tr>
            <tr>
              <td>Browser history</td>
              <td>—</td>
              <td>—</td>
              <td>—</td>
              <td>✅</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="gradle-setup">Gradle Setup for All Platforms</h2>
        <p>
          Configure your <code>build.gradle.kts</code> to target all platforms:
        </p>
        
        <CodeBlock language="kotlin" code={`// build.gradle.kts
kotlin {
    androidTarget()
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm("desktop")
    
    js(IR) {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.jermeyyy:quo-vadis-core:0.3.3")
            implementation("io.github.jermeyyy:quo-vadis-annotations:0.3.3")
        }
    }
}`} />
      </section>

      <section>
        <h2 id="running-platforms">Running on Different Platforms</h2>
        <p>
          Use these Gradle commands to run your app on each platform:
        </p>
        
        <CodeBlock language="bash" code={`# Android
./gradlew :composeApp:installDebug

# Desktop (JVM)
./gradlew :composeApp:run

# iOS (requires Xcode)
./gradlew :composeApp:iosSimulatorArm64Test

# JavaScript Browser
./gradlew :composeApp:jsBrowserDevelopmentRun

# WebAssembly Browser
./gradlew :composeApp:wasmJsBrowserDevelopmentRun`} />
      </section>

      <section>
        <h2 id="web-features">Web-Specific Features</h2>
        <p>
          On web platforms, Quo Vadis integrates with browser history and URL routing:
        </p>
        
        <CodeBlock language="kotlin" code={`// Enable browser history integration
NavigationHost(
    navigator = navigator,
    enableBrowserHistory = true  // Syncs with browser URL
)

// Deep links work as URLs
// https://yourapp.com/home/article/123`} />
      </section>

      <section>
        <h2 id="ios-features">iOS-Specific Features</h2>
        <p>
          On iOS, predictive back works with native swipe gestures:
        </p>
        
        <CodeBlock language="kotlin" code={`// Predictive back via edge swipe
NavigationHost(
    navigator = navigator,
    enablePredictiveBack = true  // Works with iOS swipe gesture
)`} />
      </section>

      <section>
        <h2 id="desktop-features">Desktop-Specific Features</h2>
        <p>
          Desktop platforms support keyboard navigation and adaptive window layouts:
        </p>
        
        <CodeBlock language="kotlin" code={`// Keyboard navigation
NavigationHost(
    navigator = navigator,
    enableKeyboardBack = true  // Escape key for back
)

// Window size class for adaptive layouts
val windowSizeClass = calculateWindowSizeClass()
NavigationHost(
    navigator = navigator,
    windowSizeClass = windowSizeClass  // Triggers pane layout changes
)`} />
      </section>

      <section>
        <h2 id="architecture">Platform-Agnostic Architecture</h2>
        <p>
          The architecture keeps platform-specific code minimal while maximizing code sharing:
        </p>
        
        <CodeBlock language="text" code={`┌─────────────────────────────────────────────────────────────┐
│                        commonMain                            │
│  - Destinations (@Stack, @Destination)                       │
│  - Screens (@Screen)                                         │
│  - Containers (NavigationContainer)                          │
│  - Business logic                                            │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        androidMain       iosMain        desktopMain
        - MainActivity    - AppDelegate  - Main window
        - Deep links      - Universal    - System tray
        - Back handler      links        - Keyboard`} />
        
        <p>
          This means 90%+ of your navigation code lives in <code>commonMain</code> and is shared 
          across all platforms, while platform-specific integrations are handled automatically.
        </p>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/predictive-back">Predictive Back</a> - Platform-specific back navigation</li>
          <li><a href="/features/deep-links">Deep Links</a> - URL patterns and deep linking</li>
          <li><a href="/demo">See the demo</a> running on multiple platforms</li>
        </ul>
      </section>
    </article>
  )
}
