import { Link } from 'react-router-dom'
import styles from '../Features.module.css'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { PlatformSupportGrid } from '@components/PlatformSupportGrid/PlatformSupportGrid'

// Architecture diagram styles
const diagramStyles = {
  container: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    gap: '0.5rem',
    padding: '2rem 1rem',
    fontFamily: 'var(--font-mono)',
    fontSize: '0.85rem',
  },
  arrow: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    padding: '0.25rem 0',
  },
  arrowLine: {
    width: '2px',
    height: '20px',
    background: 'var(--color-text-muted, #9ca3af)',
  },
  arrowHead: {
    width: '0',
    height: '0',
    borderLeft: '6px solid transparent',
    borderRight: '6px solid transparent',
    borderTop: '8px solid var(--color-text-muted, #9ca3af)',
  },
  mainBox: {
    width: '100%',
    maxWidth: '500px',
    background: 'rgba(139, 92, 246, 0.08)',
    borderRadius: '12px',
    padding: '1.25rem',
    border: '1px solid rgba(139, 92, 246, 0.3)',
  },
  mainBoxTitle: {
    fontSize: '0.7rem',
    fontWeight: '600' as const,
    textTransform: 'uppercase' as const,
    letterSpacing: '0.1em',
    marginBottom: '0.75rem',
    color: 'var(--color-primary)',
  },
  mainBoxContent: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '0.375rem',
    fontSize: '0.8rem',
    color: 'var(--color-text-secondary)',
  },
  platformBoxes: {
    display: 'flex',
    gap: '1rem',
    flexWrap: 'wrap' as const,
    justifyContent: 'center',
  },
  platformBox: {
    background: 'var(--color-bg-elevated)',
    borderRadius: '8px',
    padding: '1rem',
    border: '1px solid var(--color-border)',
    minWidth: '140px',
    textAlign: 'center' as const,
  },
  platformTitle: {
    fontWeight: '600' as const,
    fontSize: '0.875rem',
    marginBottom: '0.5rem',
    color: 'var(--color-text-primary)',
  },
  platformItems: {
    fontSize: '0.75rem',
    color: 'var(--color-text-muted)',
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '0.25rem',
  },
  codeSharing: {
    textAlign: 'center' as const,
    padding: '0.75rem 1.5rem',
    background: 'rgba(34, 197, 94, 0.1)',
    borderRadius: '20px',
    color: '#16a34a',
    fontWeight: '600' as const,
    fontSize: '0.875rem',
  },
}

const Arrow = () => (
  <div style={diagramStyles.arrow}>
    <div style={diagramStyles.arrowLine} />
    <div style={diagramStyles.arrowHead} />
  </div>
)

const PlatformArchitectureDiagram = () => (
  <div style={diagramStyles.container}>
    {/* commonMain box */}
    <div style={diagramStyles.mainBox}>
      <div style={diagramStyles.mainBoxTitle}>commonMain</div>
      <div style={diagramStyles.mainBoxContent}>
        <span>• Destinations (@Stack, @Destination)</span>
        <span>• Screens (@Screen)</span>
        <span>• Containers (NavigationContainer)</span>
        <span>• Business logic</span>
      </div>
    </div>
    
    <Arrow />
    
    {/* Platform-specific boxes */}
    <div style={diagramStyles.platformBoxes}>
      <div style={diagramStyles.platformBox}>
        <div style={diagramStyles.platformTitle}>androidMain</div>
        <div style={diagramStyles.platformItems}>
          <span>MainActivity</span>
          <span>Deep links</span>
          <span>Back handler</span>
        </div>
      </div>
      
      <div style={diagramStyles.platformBox}>
        <div style={diagramStyles.platformTitle}>iosMain</div>
        <div style={diagramStyles.platformItems}>
          <span>AppDelegate</span>
          <span>Universal links</span>
          <span>Swipe gestures</span>
        </div>
      </div>
      
      <div style={diagramStyles.platformBox}>
        <div style={diagramStyles.platformTitle}>desktopMain</div>
        <div style={diagramStyles.platformItems}>
          <span>Main window</span>
          <span>System tray</span>
          <span>Keyboard nav</span>
        </div>
      </div>
      
      <div style={diagramStyles.platformBox}>
        <div style={diagramStyles.platformTitle}>webMain</div>
        <div style={diagramStyles.platformItems}>
          <span>Browser entry</span>
          <span>URL routing</span>
          <span>History API</span>
        </div>
      </div>
    </div>
    
    {/* Code sharing indicator */}
    <div style={{ marginTop: '1rem' }}>
      <span style={diagramStyles.codeSharing}>90%+ shared code</span>
    </div>
  </div>
)

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
        
        <PlatformSupportGrid variant="detailed" showRequirements />
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
        
        <PlatformSupportGrid variant="compact" />
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
            implementation("io.github.jermeyyy:quo-vadis-core:0.3.4")
            implementation("io.github.jermeyyy:quo-vadis-annotations:0.3.4")
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
        <h2 id="platform-setup">Platform-Specific Setup</h2>
        <p>
          Each platform requires minimal configuration to integrate deep links and native gestures.
          See the platform sections below for setup details.
        </p>
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
        <h2 id="known-limitations">Known Limitations</h2>
        <ul>
          <li><strong>iOS:</strong> Requires CocoaPods or SPM for framework integration</li>
          <li><strong>Web (WASM):</strong> Larger initial bundle size compared to JS target</li>
          <li><strong>Desktop:</strong> No native system back gesture; uses keyboard shortcuts</li>
          <li><strong>Android &lt; 13:</strong> Predictive back preview not available (instant back still works)</li>
        </ul>
      </section>

      <section>
        <h2 id="architecture">Platform-Agnostic Architecture</h2>
        <p>
          The architecture keeps platform-specific code minimal while maximizing code sharing:
        </p>
        
        <PlatformArchitectureDiagram />
        
        <p>
          This means 90%+ of your navigation code lives in <code>commonMain</code> and is shared 
          across all platforms, while platform-specific integrations are handled automatically.
        </p>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/features/predictive-back">Predictive Back</Link> - Platform-specific back navigation</li>
          <li><Link to="/features/deep-links">Deep Links</Link> - URL patterns and deep linking</li>
          <li><Link to="/demo">See the demo</Link> running on multiple platforms</li>
        </ul>
      </section>
    </article>
  )
}
