import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const typeSafeAnnotationCode = `// Define destinations with annotations
@Graph("feature")
sealed class FeatureDestination : Destination

@Route("feature/list")
data object List : FeatureDestination()

@Serializable
data class DetailData(val id: String, val mode: ViewMode = ViewMode.READ)

@Route("feature/details")
@Argument(DetailData::class)
data class Details(val id: String, val mode: ViewMode = ViewMode.READ) 
    : FeatureDestination(), TypedDestination<DetailData> {
    override val data = DetailData(id, mode)
}

// Navigate with generated extension
navigator.navigateToDetails(id = "123", mode = ViewMode.EDIT)`

const typeSafeManualCode = `// Define destinations manually
sealed class FeatureDestination : Destination {
    object List : FeatureDestination() {
        override val route = "list"
    }
    
    data class Details(
        val id: String,
        val mode: ViewMode = ViewMode.READ
    ) : FeatureDestination() {
        override val route = "details"
        override val arguments = mapOf(
            "id" to id,
            "mode" to mode.name
        )
    }
}

// Navigate with destination instance
navigator.navigate(FeatureDestination.Details("123", ViewMode.EDIT))`

export default function TypeSafe() {
  return (
    <article className={styles.features}>
      <h1>Type-Safe Navigation</h1>
      <p className={styles.intro}>
        Quo Vadis eliminates string-based routing entirely, providing compile-time safety 
        for all navigation operations through two approaches: the annotation-based API (recommended) 
        and the manual DSL (for advanced use cases).
      </p>

      <div className={styles.highlights}>
        <ul>
          <li><strong>Compile-time Safety:</strong> Catch navigation errors at compile time with sealed classes</li>
          <li><strong>IDE Support:</strong> Full autocompletion and refactoring for both approaches</li>
          <li><strong>Type-safe Arguments:</strong> Pass complex data types, not just strings</li>
          <li><strong>Two Approaches:</strong> Use annotations (recommended) or manual DSL (advanced)</li>
        </ul>
      </div>

      <section>
        <h2 id="annotation-approach">Annotation-Based Approach (Recommended)</h2>
        <CodeBlock code={typeSafeAnnotationCode} language="kotlin" />
      </section>

      <section>
        <h2 id="manual-approach">Manual DSL Approach</h2>
        <CodeBlock code={typeSafeManualCode} language="kotlin" />

        <p>
          Both approaches provide complete type safety and work seamlessly together. 
          The annotation-based approach requires less code and generates helpful extensions,
          while the manual DSL offers more control for complex scenarios.
        </p>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/annotation-api">Annotation-Based API</a> - Learn about code generation</li>
          <li><a href="/features/multiplatform">Multiplatform Support</a> - Works on all platforms</li>
          <li><a href="/getting-started">Get started</a> with the quick start guide</li>
        </ul>
      </section>
    </article>
  )
}
