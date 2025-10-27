import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const sharedElementCode = `// Define shared element configuration
val imageConfig = SharedElementConfig(
    key = "product_image_\${product.id}",
    type = SharedElementType.Bounds
)

// Source screen
Image(
    modifier = Modifier.sharedElement(
        sharedConfig = imageConfig,
        navigator = navigator
    )
)

// Destination screen (same key!)
Image(
    modifier = Modifier.sharedElement(
        sharedConfig = imageConfig,
        navigator = navigator
    )
)`

export default function SharedElements() {
  return (
    <article className={styles.features}>
      <h1>Shared Element Transitions</h1>
      <p className={styles.intro}>
        Material Design 3 shared element transitions with full bidirectional support. 
        Create stunning visual continuity between screens with bidirectional animations, 
        type-safe keys, flexible bounds/content transitions, and platform-aware native behavior.
      </p>

      <section>
        <h2 id="example">Example</h2>
        <CodeBlock code={sharedElementCode} language="kotlin" />
      </section>

      <section>
        <h2 id="transition-types">Transition Types</h2>
        <ul>
          <li><strong>Bounds:</strong> Animate position and size</li>
          <li><strong>Content:</strong> Cross-fade content</li>
          <li><strong>Both:</strong> Animate bounds and content together</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/transitions">Transitions & Animations</a> - All animation options</li>
          <li><a href="/demo">See the demo</a> with shared element examples</li>
        </ul>
      </section>
    </article>
  )
}
