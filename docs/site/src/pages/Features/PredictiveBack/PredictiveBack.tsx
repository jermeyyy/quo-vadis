import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const predictiveBackCode = `NavigationHost(
    navigator = navigator,
    screenRegistry = MainScreenRegistry,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE
)`

const predictiveBackModesCode = `// Available predictive back modes
enum class PredictiveBackMode {
    NONE,           // Disabled
    STANDARD,       // Pop single screen
    FULL_CASCADE    // Support cascade pop for TabNode structures
}

// Example with full cascade support
NavigationHost(
    navigator = navigator,
    screenRegistry = AppScreenRegistry,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE,
    defaultTransition = NavigationTransitions.SlideHorizontal
)`

const cascadeBackCode = `// Cascade pop handles complex TabNode structures
// When on the start destination of a tab:
// 1. Predictive back shows preview of previous tab
// 2. Gesture completion switches to previous tab
// 3. Smooth animation between tab states

// TreeMutator provides cascade-aware operations
val result = TreeMutator.popWithTabBehavior(navState)
when (result) {
    is BackResult.Handled -> navigator.updateState(result.newState)
    is BackResult.DelegateToSystem -> // Let system handle back
}`

export default function PredictiveBack() {
  return (
    <article className={styles.features}>
      <h1>Predictive Back Navigation</h1>
      <p className={styles.intro}>
        Modern, gesture-driven back navigation with smooth animations. Users can preview 
        the previous screen before committing to navigation. Built directly into NavigationHost 
        via the <code>predictiveBackMode</code> parameter.
      </p>

      <section>
        <h2 id="platforms">Supported Platforms</h2>
        <ul>
          <li><strong>Android 13+:</strong> System predictive back API integration</li>
          <li><strong>iOS:</strong> Interactive pop gesture with preview</li>
          <li><strong>Desktop:</strong> Custom gesture implementations</li>
          <li><strong>Web:</strong> Browser back button support</li>
        </ul>
      </section>

      <section>
        <h2 id="features">Features</h2>
        <ul>
          <li>Smooth, interruptible animations</li>
          <li>Cross-fade between screens during gesture</li>
          <li>Scale and position transitions</li>
          <li>Cancelable gestures - release to cancel</li>
          <li>Cascade pop support for TabNode structures</li>
          <li>Works with NavNode tree state</li>
        </ul>
      </section>

      <section>
        <h2 id="setup">Setup</h2>
        <p>
          Predictive back is enabled through the <code>predictiveBackMode</code> parameter 
          in NavigationHost:
        </p>
        <CodeBlock code={predictiveBackCode} language="kotlin" />
      </section>

      <section>
        <h2 id="modes">Predictive Back Modes</h2>
        <p>
          Choose the appropriate mode based on your navigation structure:
        </p>
        <CodeBlock code={predictiveBackModesCode} language="kotlin" />
      </section>

      <section>
        <h2 id="cascade-back">Cascade Back for Tabs</h2>
        <p>
          When using TabNode structures, cascade back provides intelligent handling 
          of back gestures that span tab boundaries:
        </p>
        <CodeBlock code={cascadeBackCode} language="kotlin" />
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/transitions">Transitions & Animations</a> - All transition types</li>
          <li><a href="/features/stack-management">Stack Management</a> - NavNode tree operations</li>
          <li><a href="/demo">See the demo</a> with predictive back in action</li>
        </ul>
      </section>
    </article>
  )
}
