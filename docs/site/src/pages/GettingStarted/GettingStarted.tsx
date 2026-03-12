import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './GettingStarted.module.css'
import {
  stackDestinationWithSettings,
  screenBindingWithImports,
  navigationHostWithImports,
  gradlePluginInstallation,
  manualKspConfiguration,
} from '@data/codeExamples'

const validationExampleError = `Missing @Screen binding for 'HomeDestination.Feed' in file 'HomeDestination.kt' (line 12). 
Fix: Add a @Composable function annotated with @Screen(HomeDestination.Feed::class)`

export default function GettingStarted() {
  return (
    <article className={styles.gettingStarted}>
      <h1>Getting Started</h1>

      <p>
        This guide will walk you through setting up Quo Vadis in your Kotlin Multiplatform project.
        Follow these steps to get navigation working in minutes.
      </p>

      <section>
        <h2 id="installation">Installation</h2>

        <p>
          Choose your annotation backend before wiring Quo Vadis into the build. KSP remains the
          stable recommendation for production use today, while the compiler plugin is available as
          an experimental alternative.
        </p>

        <h3 id="gradle-plugin">Option 1: Using Gradle Plugin with KSP (Recommended)</h3>
        <p>
          The Gradle plugin automatically configures KSP and registers generated sources, making the
          stable setup simpler and more reliable.
        </p>
        <CodeBlock code={gradlePluginInstallation} language="kotlin" title="build.gradle.kts" />

        <div className={styles.note}>
          <p>
            Want to evaluate the newer backend instead? See <Link to="/features/compiler-plugin">Compiler Plugin (Experimental)</Link>
            {' '}for the current trade-offs, setup direction, and rollback guidance.
          </p>
        </div>

        <h3 id="manual-config">Option 2: Manual KSP Configuration</h3>
        <p>
          If you need more control over the build configuration, you can set up KSP manually.
        </p>
        <CodeBlock code={manualKspConfiguration} language="kotlin" title="build.gradle.kts" />
      </section>

      <section>
        <h2 id="define-stack">Define Your Navigation Stack</h2>
        <p>
          Create a sealed class extending <code>NavDestination</code> and annotate it with <code>@Stack</code>.
          Each destination is defined as a nested class with the <code>@Destination</code> annotation.
        </p>
        <CodeBlock code={stackDestinationWithSettings} language="kotlin" title="HomeDestination.kt" />

        <div className={styles.note}>
          <p><strong>💡 Tip:</strong> Use <code>@Argument</code> on data class properties to enable 
          automatic serialization for deep links. Optional arguments should have default values.</p>
        </div>
      </section>

      <section>
        <h2 id="bind-screens">Bind Screens with @Screen</h2>
        <p>
          Use the <code>@Screen</code> annotation to connect your composables to destinations.
          The navigator is automatically injected, and destination data classes receive the destination instance.
        </p>
        <CodeBlock code={screenBindingWithImports} language="kotlin" title="Screens.kt" />
      </section>

      <section>
        <h2 id="setup-navigation-host">Setup NavigationHost</h2>
        <p>
          Finally, set up the <code>NavigationHost</code> in your app's entry point. Start with the
          module-level generated config for your app module, such as <code>MyAppNavigationConfig</code>.
          If your app spans multiple modules, combine those module configs explicitly with the
          <code>+</code> operator in one place.
        </p>
        <CodeBlock code={navigationHostWithImports} language="kotlin" title="App.kt" />
      </section>

      <section>
        <h2 id="generated-code">What Gets Generated</h2>
        <p>
          Annotation-based configuration produces several classes from your annotations. In KSP mode
          you can inspect generated sources directly; the compiler plugin exposes the same core APIs
          through compiler synthesis instead. The prefix is configurable via <code>modulePrefix</code>
          in your Gradle configuration.
        </p>
        <p>
          For example, setting <code>modulePrefix = "MyApp"</code> produces
          <code> MyAppNavigationConfig</code>. In multi-module apps, keep each module's generated config
          separate and compose them explicitly where you create your navigator.
        </p>

        <table>
          <thead>
            <tr>
              <th>Generated Class</th>
              <th>Purpose</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>{'{Prefix}'}NavigationConfig</code></td>
              <td>Main configuration with all registries</td>
            </tr>
            <tr>
              <td><code>{'{Prefix}'}ScreenRegistry</code></td>
              <td>Maps destinations to composables</td>
            </tr>
            <tr>
              <td><code>{'{Prefix}'}DeepLinkHandler</code></td>
              <td>Handles URI-based navigation</td>
            </tr>
            <tr>
              <td><code>{'{Prefix}'}ScopeRegistry</code></td>
              <td>Scope membership for containers</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="compile-time-safety">Compile-Time Safety</h2>
        <p>
          Quo Vadis validates your navigation configuration at compile time. If there are
          issues with your annotations, the build will fail with clear error messages
          showing exactly what's wrong and how to fix it.
        </p>
        <p>
          Example error:
        </p>
        <CodeBlock code={validationExampleError} language="text" />
        <p>
          See <Link to="/features/annotation-api#validation">Validation & Error Messages</Link> for
          the complete list of validation rules.
        </p>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <p>
          Now that you have basic navigation working, explore more advanced features:
        </p>
        <ul>
          <li>
            <a href="/features/annotation-api">Annotation API</a> — Full reference for all annotations
            including <code>@Tabs</code>, <code>@Pane</code>, and more
          </li>
          <li>
            <a href="/features/compiler-plugin">Compiler Plugin (Experimental)</a> — Evaluate the newer
            annotation backend and understand when it differs from KSP
          </li>
          <li>
            <a href="/features/tabbed-navigation">Tabbed Navigation</a> — Set up tab-based navigation
            with independent backstacks
          </li>
          <li>
            <a href="/features/transitions">Transitions</a> — Add custom animations and shared element
            transitions
          </li>
          <li>
            <a href="/features/di-integration">FlowMVI & Koin Integration</a> — Integrate MVI state
            management with navigation-scoped containers
          </li>
        </ul>
      </section>

      <div className={styles.note}>
        <p><strong>💡 Pro Tip:</strong> Start with simple stack navigation and gradually add features 
        like tabs, panes, and transitions as your app grows. The library is designed 
        to be incrementally adoptable.</p>
      </div>
    </article>
  )
}
