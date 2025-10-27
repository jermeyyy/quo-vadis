import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const mviCode = `// Navigation as intents
sealed class Intent {
    data class NavigateToDetails(val id: String) : Intent()
    object NavigateBack : Intent()
}

// Handle in reducer
fun reduce(intent: Intent) {
    when (intent) {
        is Intent.NavigateToDetails -> {
            navigator.navigate(DetailsDestination(intent.id))
        }
        is Intent.NavigateBack -> {
            navigator.navigateBack()
        }
    }
}

// Or as side effects
sealed class SideEffect {
    data class Navigate(val destination: Destination) : SideEffect()
}

// Handle side effects
viewModel.sideEffects.collect { effect ->
    when (effect) {
        is SideEffect.Navigate -> navigator.navigate(effect.destination)
    }
}`

export default function MVI() {
  return (
    <article className={styles.features}>
      <h1>MVI Architecture Support</h1>
      <p className={styles.intro}>
        First-class integration with MVI (Model-View-Intent) architecture pattern. 
        Navigation intents, state, and side effects are handled cleanly.
      </p>

      <section>
        <h2 id="integration">Integration</h2>
        <CodeBlock code={mviCode} language="kotlin" />
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/testing">Testing Support</a> - Test your MVI navigation</li>
          <li><a href="/demo">See the demo</a> with MVI examples</li>
        </ul>
      </section>
    </article>
  )
}
