import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const customTransitionCode = `val customTransition = NavigationTransition(
    enter = slideInVertically { it } + fadeIn(),
    exit = slideOutVertically { -it / 2 } + fadeOut(),
    popEnter = slideInVertically { -it / 2 } + fadeIn(),
    popExit = slideOutVertically { it } + fadeOut()
)`

export default function Transitions() {
  return (
    <article className={styles.features}>
      <h1>Transitions & Animations</h1>
      <p className={styles.intro}>
        Rich set of built-in transitions and support for custom animations. 
        Create polished, professional navigation experiences.
      </p>

      <section>
        <h2 id="built-in">Built-in Transitions</h2>
        <div className={styles.transitionGrid}>
          <div className={styles.transitionCard}>
            <h4>SlideHorizontal</h4>
            <p>Standard horizontal slide, ideal for hierarchical navigation</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>SlideVertical</h4>
            <p>Vertical slide, perfect for modal presentations</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>Fade</h4>
            <p>Simple cross-fade between screens</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>FadeThrough</h4>
            <p>Material Design fade through pattern</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>ScaleIn</h4>
            <p>Scale animation for emphasizing content</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>None</h4>
            <p>Instant navigation without animation</p>
          </div>
        </div>
      </section>

      <section>
        <h2 id="custom">Custom Transitions</h2>
        <CodeBlock code={customTransitionCode} language="kotlin" />
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/shared-elements">Shared Elements</a> - Advanced animations</li>
          <li><a href="/features/predictive-back">Predictive Back</a> - Gesture-driven navigation</li>
          <li><a href="/demo">See the demo</a> for all transitions</li>
        </ul>
      </section>
    </article>
  )
}
