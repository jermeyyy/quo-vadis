import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const testingCode = `@Test
fun \`navigates to details when item clicked\`() {
    // Arrange
    val navigator = FakeNavigator()
    val viewModel = ProductListViewModel(navigator)
    
    // Act
    viewModel.onProductClicked("product-123")
    
    // Assert
    assertEquals(
        ProductDestination.Details("product-123"),
        navigator.lastDestination
    )
}

@Test
fun \`clears navigation stack on logout\`() {
    // Arrange
    val navigator = FakeNavigator()
    val viewModel = SettingsViewModel(navigator)
    
    // Act
    viewModel.onLogout()
    
    // Assert
    assertTrue(navigator.stackCleared)
    assertEquals(LoginDestination, navigator.lastDestination)
}`

export default function Testing() {
  return (
    <article className={styles.features}>
      <h1>Testing Support</h1>
      <p className={styles.intro}>
        Built-in testing utilities make it easy to verify navigation behavior without 
        UI testing. Test navigation logic in fast, reliable unit tests.
      </p>

      <section>
        <h2 id="fake-navigator">FakeNavigator</h2>
        <CodeBlock code={testingCode} language="kotlin" />
      </section>

      <section>
        <h2 id="verification-methods">Verification Methods</h2>
        <ul>
          <li><code>verifyNavigate(destination)</code> - Verify navigation to destination</li>
          <li><code>verifyNavigateBack()</code> - Verify back navigation</li>
          <li><code>clearCalls()</code> - Reset navigation call history</li>
          <li><code>navigationCalls</code> - Access full navigation history</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/getting-started#testing">Testing guide</a> in Getting Started</li>
          <li><a href="/demo">See the demo</a> application tests</li>
        </ul>
      </section>
    </article>
  )
}
