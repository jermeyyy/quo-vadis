import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

// Status badge components
const StatusFull = ({ children = 'Full' }: { children?: string }) => (
  <span className={`${styles.statusBadge} ${styles.statusFull}`}>{children}</span>
)

const StatusPartial = ({ children = 'Partial' }: { children?: string }) => (
  <span className={`${styles.statusBadge} ${styles.statusPartial}`}>{children}</span>
)

const StatusNo = ({ children = 'No' }: { children?: string }) => (
  <span className={`${styles.statusBadge} ${styles.statusNo}`}>{children}</span>
)

const enablePredictiveBackCode = `NavigationHost(
    navigator = navigator,
    enablePredictiveBack = true  // Default
)`

const disablePredictiveBackCode = `NavigationHost(
    navigator = navigator,
    enablePredictiveBack = false  // Disable gesture preview
)`

const visualBehaviorCode = `// Current screen slides right and scales down
Box(
    modifier = Modifier.graphicsLayer {
        translationX = size.width * progress
        val scale = 1f - (progress * 0.15f)
        scaleX = scale
        scaleY = scale
    }
)

// Previous screen has parallax effect
Box(
    modifier = Modifier.graphicsLayer {
        translationX = -size.width * 0.15f * (1f - progress)
    }
)`

const controllerStateCode = `@Stable
class PredictiveBackController {
    val isActive: State<Boolean>    // Whether gesture is active
    val progress: State<Float>      // 0.0 to 1.0 (clamped during gesture)
    val cascadeState: State<CascadeBackState?>  // What will be removed
}`

const navigatorBackOpsCode = `// Predictive back methods on TreeNavigator
fun startPredictiveBack()
fun updatePredictiveBack(progress: Float, touchX: Float, touchY: Float)
fun cancelPredictiveBack()
fun commitPredictiveBack()
fun completeTransition()`

const transitionStateCode = `sealed interface TransitionState {
    data object Idle : TransitionState
    
    data class PredictiveBack(
        val progress: Float,
        val currentKey: String?,
        val previousKey: String?,
        val touchX: Float,           // 0.0 to 1.0
        val touchY: Float,           // 0.0 to 1.0
        val isCommitted: Boolean
    ) : TransitionState
}`

const observingStateCode = `val transitionState by (navigator as? TreeNavigator)
    ?.transitionState
    ?.collectAsState()
    ?: remember { mutableStateOf(TransitionState.Idle) }

when (transitionState) {
    is TransitionState.PredictiveBack -> {
        val state = transitionState as TransitionState.PredictiveBack
        // Show preview with progress: state.progress
        // Touch position: state.touchX, state.touchY
    }
    else -> { /* Normal state */ }
}`

const androidManifestCode = `<application
    ...
    android:enableOnBackInvokedCallback="true">
</application>`

export default function PredictiveBack() {
  return (
    <article className={styles.features}>
      <h1>Predictive Back Navigation</h1>
      <p className={styles.intro}>
        Quo Vadis supports Android's predictive back gesture API (Android 13+) and iOS 
        swipe-back gesture, providing smooth gesture-driven animations with visual preview 
        before completing navigation.
      </p>

      <section>
        <h2 id="platform-support">Platform Support</h2>
        <table>
          <thead>
            <tr>
              <th>Platform</th>
              <th>Support</th>
              <th>Features</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Android 13+</td>
              <td><StatusFull /></td>
              <td>Predictive back gestures with visual preview</td>
            </tr>
            <tr>
              <td>iOS</td>
              <td><StatusFull /></td>
              <td>Edge swipe-back gesture</td>
            </tr>
            <tr>
              <td>Desktop</td>
              <td><StatusPartial /></td>
              <td>Keyboard back (Escape key)</td>
            </tr>
            <tr>
              <td>Web</td>
              <td><StatusPartial /></td>
              <td>Browser back button integration</td>
            </tr>
          </tbody>
        </table>
        <p>
          For general platform capabilities, see{' '}
          <Link to="/features/multiplatform">Multiplatform Support</Link>.
        </p>
      </section>

      <section>
        <h2 id="enabling">Enabling Predictive Back</h2>
        <p>
          Predictive back is enabled by default in <code>NavigationHost</code>:
        </p>
        <CodeBlock code={enablePredictiveBackCode} language="kotlin" />
        <p>
          Disable for specific scenarios where gesture preview isn't desired:
        </p>
        <CodeBlock code={disablePredictiveBackCode} language="kotlin" />
      </section>

      <section>
        <h2 id="how-it-works">How It Works</h2>
        <p>
          The predictive back gesture follows a defined lifecycle:
        </p>
        <ol>
          <li><strong>Gesture Start:</strong> <code>PredictiveBackController</code> activates when the user begins the back gesture</li>
          <li><strong>Progress Updates:</strong> Visual transforms are applied based on gesture progress (0.0 to 1.0)</li>
          <li><strong>Completion:</strong> When gesture is committed, animated transition to final state occurs</li>
          <li><strong>Cancellation:</strong> If gesture is released early, animated return to original state</li>
        </ol>
      </section>

      <section>
        <h2 id="visual-behavior">Visual Behavior</h2>
        <p>
          During the gesture, visual transforms create a preview effect:
        </p>
        <CodeBlock code={visualBehaviorCode} language="kotlin" />
        <p>
          The current screen slides right and scales down slightly, while the previous screen 
          has a parallax effect sliding in from the left.
        </p>
      </section>

      <section>
        <h2 id="controller-state">PredictiveBackController State</h2>
        <p>
          The <code>PredictiveBackController</code> manages gesture state:
        </p>
        <CodeBlock code={controllerStateCode} language="kotlin" />
        <h3>Progress Ranges</h3>
        <ul>
          <li><strong>During gesture:</strong> <code>0.0</code> to <code>0.17</code> (clamped to prevent excessive movement)</li>
          <li><strong>During completion:</strong> <code>0.0</code> to <code>1.0</code></li>
          <li><strong>During cancellation:</strong> Animates back to <code>0.0</code></li>
        </ul>
      </section>

      <section>
        <h2 id="navigator-operations">Navigator Back Operations</h2>
        <p>
          <code>TreeNavigator</code> provides methods for programmatic control:
        </p>
        <CodeBlock code={navigatorBackOpsCode} language="kotlin" />
        <p>
          These methods are typically called by the platform-specific gesture handlers, 
          but can be invoked directly for custom implementations.
        </p>
      </section>

      <section>
        <h2 id="transition-state">TransitionState Types</h2>
        <p>
          The <code>TransitionState</code> sealed interface represents the current state:
        </p>
        <CodeBlock code={transitionStateCode} language="kotlin" />
        <p>
          The <code>touchX</code> and <code>touchY</code> values are normalized to 0.0-1.0, 
          representing the position on screen where the gesture originated.
        </p>
      </section>

      <section>
        <h2 id="observing-state">Observing State in Compose</h2>
        <p>
          You can observe the transition state to react to gesture progress:
        </p>
        <CodeBlock code={observingStateCode} language="kotlin" />
      </section>

      <section>
        <h2 id="context-behavior">Context-Specific Behavior</h2>
        <p>
          Predictive back behavior varies by navigation context:
        </p>
        <table>
          <thead>
            <tr>
              <th>Context</th>
              <th>Predictive Back</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Stack navigation</td>
              <td><StatusFull>Enabled</StatusFull></td>
            </tr>
            <tr>
              <td>Tab switching</td>
              <td><StatusNo>Disabled</StatusNo></td>
            </tr>
            <tr>
              <td>Pane transitions</td>
              <td>Depends on compact/expanded mode</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="android-setup">Android Manifest Setup</h2>
        <p>
          To enable predictive back on Android, add this to your <code>AndroidManifest.xml</code>:
        </p>
        <CodeBlock code={androidManifestCode} language="xml" />
        <p>
          This is required for Android 13+ to opt-in to the new back gesture API.
        </p>
      </section>

      <section>
        <h2 id="ios-integration">iOS Integration</h2>
        <p>
          On iOS, predictive back integrates with the system edge swipe gesture through 
          a <code>NavigateBackHandler</code> in the SwiftUI integration layer. The edge 
          swipe gesture is automatically handled when using the standard iOS navigation setup.
        </p>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/transitions">Transitions & Animations</a> - Transition types and customization</li>
          <li><a href="/features/stack-management">Stack Management</a> - NavNode tree operations</li>
          <li><a href="/demo">See the demo</a> with predictive back in action</li>
        </ul>
      </section>
    </article>
  )
}
