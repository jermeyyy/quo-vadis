import styles from '../Features.module.css'

export default function Performance() {
  return (
    <article className={styles.features}>
      <h1>Performance</h1>
      <p className={styles.intro}>
        Optimized for performance with minimal overhead. Lazy initialization, 
        efficient state management, and smart recomposition.
      </p>

      <section>
        <h2 id="optimizations">Optimizations</h2>
        <ul>
          <li><strong>Lazy Loading:</strong> Destinations created only when needed</li>
          <li><strong>Efficient State:</strong> StateFlow with structural sharing</li>
          <li><strong>Smart Recomposition:</strong> Minimal recomposition on navigation</li>
          <li><strong>No Reflection:</strong> Zero runtime reflection overhead</li>
          <li><strong>Small Footprint:</strong> No external dependencies</li>
        </ul>
      </section>

      <section>
        <h2 id="no-dependencies">No External Dependencies</h2>
        <p>
          Quo Vadis is completely self-contained with zero external navigation dependencies. 
          This means:
        </p>
        <ul>
          <li>Smaller app size</li>
          <li>No version conflicts</li>
          <li>No dependency chain issues</li>
          <li>Full control over updates</li>
          <li>Better long-term stability</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/getting-started">Get started</a> with Quo Vadis</li>
          <li><a href="/demo">See the demo</a> to experience performance</li>
        </ul>
      </section>
    </article>
  )
}
