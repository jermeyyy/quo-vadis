import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const modularCode = `// Feature exposes navigation graph
object ProfileFeature {
    fun navigationGraph(): NavigationGraph = navigationGraph("profile") {
        startDestination(ProfileDestination.List)
        
        destination(ProfileDestination.List) { _, nav -> ... }
        destination(ProfileDestination.Details) { _, nav -> ... }
    }
    
    // Public entry points
    val entryPoint: Destination = ProfileDestination.List
}

// Main app integrates feature
val mainGraph = navigationGraph("main") {
    includeGraph(ProfileFeature.navigationGraph())
    
    // Navigate to feature
    navigator.navigate(ProfileFeature.entryPoint)
}`

export default function Modular() {
  return (
    <article className={styles.features}>
      <h1>Modular Architecture</h1>
      <p className={styles.intro}>
        Gray box pattern support enables true modular architecture. Features can 
        define their own navigation graphs and expose public entry points, enabling 
        independent feature development, clear module boundaries, reusable feature modules, 
        and easier testing and maintenance.
      </p>

      <section>
        <h2 id="example">Feature Module Example</h2>
        <CodeBlock code={modularCode} language="kotlin" />
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/di-integration">DI Integration</a> - Inject dependencies</li>
          <li><a href="/demo">See the demo</a> with modular structure</li>
        </ul>
      </section>
    </article>
  )
}
