import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const mviConceptCode = `// MVI Architecture Overview
// State - What the UI displays
sealed interface State { ... }

// Intent - What the user wants to do  
sealed interface Intent { ... }

// Side Effects - Navigation, toasts, analytics
sealed interface Action { ... }

// The store/container processes intents and updates state
// Navigation is handled as a side effect in the reducer`

export default function MVI() {
  return (
    <article className={styles.features}>
      <h1>MVI Architecture</h1>
      <p className={styles.intro}>
        MVI (Model-View-Intent) is an architecture pattern that treats navigation as a 
        side effect of business logic. Quo Vadis provides first-class support for MVI 
        through the <strong>quo-vadis-core-flow-mvi</strong> module.
      </p>

      <section>
        <h2 id="concept">The MVI Concept</h2>
        <p>
          In MVI architecture, the UI dispatches <strong>Intents</strong> (user actions), 
          a <strong>Reducer</strong> processes them and updates <strong>State</strong>, 
          and <strong>Side Effects</strong> (like navigation) are emitted separately.
        </p>
        <CodeBlock code={mviConceptCode} language="kotlin" />
      </section>

      <section>
        <h2 id="flowmvi">FlowMVI Integration</h2>
        <p>
          Quo Vadis integrates with <a href="https://github.com/respawn-app/FlowMVI" target="_blank" rel="noopener noreferrer">FlowMVI</a>, 
          a Kotlin Multiplatform MVI framework. The <code>quo-vadis-core-flow-mvi</code> module 
          provides seamless integration for handling navigation as side effects.
        </p>
        <div className={styles.note}>
          <strong>📦 Module:</strong> Add <code>io.github.jermeyyy:quo-vadis-core-flow-mvi</code> to your dependencies.
        </div>
      </section>

      <section>
        <h2 id="benefits">Benefits</h2>
        <ul>
          <li><strong>Testable:</strong> Test navigation logic without UI using FakeNavigator</li>
          <li><strong>Predictable:</strong> Navigation as pure side effect of state changes</li>
          <li><strong>Centralized:</strong> All navigation logic in one place (Container)</li>
          <li><strong>Decoupled:</strong> UI doesn't need to know about destination types</li>
          <li><strong>Scalable:</strong> Easy to maintain as app complexity grows</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/flow-mvi">FlowMVI Integration Guide</a> - Complete integration documentation</li>
          <li><a href="https://github.com/respawn-app/FlowMVI" target="_blank" rel="noopener noreferrer">FlowMVI Documentation</a> - Learn more about FlowMVI</li>
          <li><a href="/features/testing">Testing Support</a> - Test your MVI navigation</li>
          <li><a href="/demo">Live Demo</a> - See MVI in action</li>
        </ul>
      </section>
    </article>
  )
}

