import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const koinCode = `val graph = navigationGraph("app") {
    destination(HomeDestination) { _, navigator ->
        val viewModel: HomeViewModel = koinInject()
        HomeScreen(viewModel, navigator)
    }
}`

const customInjectionCode = `// Create custom destination factory
interface DestinationFactory {
    @Composable
    fun create(destination: Destination, navigator: Navigator)
}

// Use in navigation graph
val graph = navigationGraph("app") {
    factory = myDependencyContainer.destinationFactory
}`

export default function DIIntegration() {
  return (
    <article className={styles.features}>
      <h1>DI Framework Integration</h1>
      <p className={styles.intro}>
        Easy integration with popular DI frameworks like Koin, Kodein, and others. 
        Inject dependencies into destination composables seamlessly.
      </p>

      <section>
        <h2 id="koin">Koin Example</h2>
        <CodeBlock code={koinCode} language="kotlin" />
      </section>

      <section>
        <h2 id="custom">Custom Injection</h2>
        <CodeBlock code={customInjectionCode} language="kotlin" />
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/modular">Modular Architecture</a> - Structure your features</li>
          <li><a href="/demo">See the demo</a> with DI integration</li>
        </ul>
      </section>
    </article>
  )
}
