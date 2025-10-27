import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const backStackCode = `// Access current stack
val backStack = navigator.backStack.value

// Pop multiple destinations
navigator.popBackStack(count = 3)

// Clear to specific destination
navigator.navigateAndClearTo(
    destination = HomeDestination,
    clearRoute = "onboarding",
    inclusive = true
)

// Replace current destination
navigator.navigateAndReplace(NewDestination)

// Clear everything and start fresh
navigator.navigateAndClearAll(StartDestination)`

export default function BackStack() {
  return (
    <article className={styles.features}>
      <h1>BackStack Management</h1>
      <p className={styles.intro}>
        Direct access to the navigation back stack provides unprecedented control over 
        navigation state. Manipulate the stack programmatically for complex navigation flows.
      </p>

      <section>
        <h2 id="operations">Operations</h2>
        <CodeBlock code={backStackCode} language="kotlin" />
      </section>

      <section>
        <h2 id="use-cases">Use Cases</h2>
        <ul>
          <li>Multi-step wizards with complex navigation</li>
          <li>Authentication flows that clear login screens</li>
          <li>Tab-based navigation with independent stacks</li>
          <li>Undo/redo functionality</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/demo">See the demo</a> for multi-step process examples</li>
          <li><a href="/getting-started">Get started</a> with navigation basics</li>
        </ul>
      </section>
    </article>
  )
}
