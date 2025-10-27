import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const predictiveBackCode = `GraphNavHost(
    graph = mainGraph,
    navigator = navigator,
    enablePredictiveBack = true,
    predictiveBackAnimationType = PredictiveBackAnimationType.Material3
)`

export default function PredictiveBack() {
  return (
    <article className={styles.features}>
      <h1>Predictive Back Navigation</h1>
      <p className={styles.intro}>
        Modern, gesture-driven back navigation with smooth animations. Users can preview 
        the previous screen before committing to navigation.
      </p>

      <section>
        <h2 id="platforms">Supported Platforms</h2>
        <ul>
          <li><strong>Android 13+:</strong> System predictive back API</li>
          <li><strong>iOS:</strong> Interactive pop gesture</li>
          <li><strong>Custom Implementations:</strong> Desktop and Web</li>
        </ul>
      </section>

      <section>
        <h2 id="features">Features</h2>
        <ul>
          <li>Smooth, interruptible animations</li>
          <li>Cross-fade between screens</li>
          <li>Scale and position transitions</li>
          <li>Cancelable gestures</li>
        </ul>
      </section>

      <section>
        <h2 id="setup">Setup</h2>
        <CodeBlock code={predictiveBackCode} language="kotlin" />
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/transitions">Transitions & Animations</a> - All transition types</li>
          <li><a href="/demo">See the demo</a> with predictive back in action</li>
        </ul>
      </section>
    </article>
  )
}
