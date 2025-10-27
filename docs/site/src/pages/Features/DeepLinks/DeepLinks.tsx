import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const deepLinkCode = `val graph = navigationGraph("main") {
    // Simple path
    deepLink("myapp://home") {
        HomeDestination
    }
    
    // Path parameters
    deepLink("myapp://user/{userId}") { args ->
        UserDestination(userId = args["userId"] as String)
    }
    
    // Query parameters
    deepLink("myapp://search?q={query}") { args ->
        SearchDestination(query = args["query"] as String)
    }
    
    // Optional parameters
    deepLink("myapp://settings/{section?}") { args ->
        SettingsDestination(section = args["section"] as? String)
    }
}`

export default function DeepLinks() {
  return (
    <article className={styles.features}>
      <h1>Deep Link Support</h1>
      <p className={styles.intro}>
        Comprehensive deep linking system that works across all platforms. Define URL 
        patterns and automatically map them to type-safe destinations.
      </p>

      <section>
        <h2 id="pattern-matching">Pattern Matching</h2>
        <CodeBlock code={deepLinkCode} language="kotlin" />
      </section>

      <section>
        <h2 id="platform-integration">Platform Integration</h2>
        <ul>
          <li><strong>Android:</strong> Intent filters and App Links</li>
          <li><strong>iOS:</strong> Universal Links and custom URL schemes</li>
          <li><strong>Web:</strong> Direct URL navigation</li>
          <li><strong>Desktop:</strong> Custom protocol handlers</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/multiplatform">Multiplatform Support</a> - Deep links on all platforms</li>
          <li><a href="/getting-started">Get started</a> with deep link setup</li>
        </ul>
      </section>
    </article>
  )
}
