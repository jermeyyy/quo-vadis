import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const compilerBackendProperty = `# gradle.properties
quoVadis.backend=compiler`

const compilerPluginSetup = `plugins {
    alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.ksp)
    alias(libs.plugins.quoVadis)
}

quoVadis {
    modulePrefix = "MyApp"
}`

const navigationRootExample = `@NavigationRoot
object MyApp

val navigator = rememberQuoVadisNavigator(
    MainTabs::class,
    MyAppNavigationConfig
)`

const rollbackExample = `# gradle.properties
quoVadis.backend=ksp`

const verificationCommands = `./gradlew clean :composeApp:desktopJar
./gradlew :feature1:allMetadataJar :feature2:allMetadataJar -PquoVadis.backend=compiler`

export default function CompilerPlugin() {
  return (
    <article className={styles.features}>
      <h1>Compiler Plugin (Experimental)</h1>
      <p className={styles.intro}>
        Quo Vadis can power its annotation-based navigation API through KSP or through a newer compiler-plugin backend.
        The compiler plugin is documented here as an experimental option for early adopters, not as the default production recommendation.
      </p>

      <div className={styles.highlights}>
        <ul>
          <li><strong>Recommendation today:</strong> Prefer KSP for the most stable setup.</li>
          <li><strong>Why try it:</strong> Faster builds, better IDE autocomplete, and compiler-only enhancements like <code>@NavigationRoot</code>.</li>
          <li><strong>Important constraint:</strong> Do not enable KSP and the compiler plugin in the same module.</li>
          <li><strong>Scope:</strong> Your annotations stay the same. Only the generation backend changes.</li>
        </ul>
      </div>

      <section>
        <h2 id="overview">What It Is</h2>
        <p>
          The compiler plugin moves Quo Vadis annotation processing into the Kotlin compiler pipeline. That removes the separate
          KSP processing pass and exposes generated APIs through compiler synthesis instead of visible generated source files.
        </p>
      </section>

      <section>
        <h2 id="why-it-exists">Why It Exists</h2>
        <ul>
          <li><strong>Faster build path:</strong> Generation happens during normal compilation instead of in a separate KSP phase.</li>
          <li><strong>Better IDE story:</strong> FIR synthetic declarations can surface generated APIs without waiting for a build.</li>
          <li><strong>Cleaner multi-module discovery:</strong> <code>@NavigationRoot</code> can aggregate module configs automatically.</li>
        </ul>
      </section>

      <section>
        <h2 id="comparison">KSP vs Compiler Plugin</h2>
        <table>
          <thead>
            <tr>
              <th>Aspect</th>
              <th>KSP</th>
              <th>Compiler Plugin</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Recommendation</td>
              <td>Stable default</td>
              <td>Experimental</td>
            </tr>
            <tr>
              <td>Generated output</td>
              <td>Visible generated source files</td>
              <td>Synthesized APIs during compilation</td>
            </tr>
            <tr>
              <td>IDE autocomplete</td>
              <td>Usually after build</td>
              <td>Can appear without a full build</td>
            </tr>
            <tr>
              <td>Multi-module composition</td>
              <td>Explicit <code>+</code> composition</td>
              <td>Explicit <code>+</code> composition, optionally augmented by <code>@NavigationRoot</code></td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="getting-started">Getting Started</h2>
        <p>
          If you want to evaluate the compiler plugin on this branch, switch the root backend property first, then apply the
          Quo Vadis Gradle plugin in the module as usual.
        </p>
        <CodeBlock code={compilerBackendProperty} language="properties" title="gradle.properties" />
        <CodeBlock code={compilerPluginSetup} language="kotlin" title="build.gradle.kts" />

        <div className={styles.note}>
          <p>
            For most teams, <Link to="/getting-started">Getting Started</Link> with KSP is still the safer path. Use the compiler plugin when
            you explicitly want to test the newer backend and accept experimental rollout risk.
          </p>
        </div>

        <div className={styles.note}>
          <p>
            Kotlin 2.1.0 or later with K2 support is required. The deprecated <code>useCompilerPlugin</code> alias may still appear in older
            examples, but <code>quoVadis.backend</code> is the primary rollout switch.
          </p>
        </div>
      </section>

      <section>
        <h2 id="switching-and-rollback">Switching and Rollback</h2>
        <p>
          Backend switching should stay explicit while the compiler plugin is experimental. If you need to fall back to the stable path,
          switch the backend back to KSP and rebuild cleanly.
        </p>
        <CodeBlock code={rollbackExample} language="properties" title="gradle.properties" />
        <p>
          Always run a clean build after flipping backends so stale KSP outputs do not cause duplicate or conflicting declarations.
        </p>
        <p>
          The full migration and rollback checklist lives in the repository migration guide.
        </p>
        <p>
          <a
            href="https://github.com/jermeyyy/quo-vadis/blob/compiler-plugin/docs/MIGRATION.md"
            target="_blank"
            rel="noopener noreferrer"
          >
            Open the migration guide on GitHub
          </a>
        </p>
      </section>

      <section>
        <h2 id="multi-module">Multi-Module Support</h2>
        <p>
          The backend-neutral starting point is still explicit module-level composition, for example
          <code> ComposeAppNavigationConfig + Feature1NavigationConfig + Feature2NavigationConfig</code>.
          In compiler-plugin mode, you can additionally aggregate visible module configs automatically from an app-level root.
        </p>
        <CodeBlock code={navigationRootExample} language="kotlin" />
        <p>
          Keep this framed as a compiler-plugin-specific enhancement for now. It is useful, but it should not replace
          the stable explicit-composition workflow in the general docs.
        </p>
      </section>

      <section>
        <h2 id="limitations">Limitations and Warnings</h2>
        <ul>
          <li><strong>Experimental status:</strong> Do not present this backend as production-stable yet.</li>
          <li><strong>Version sensitivity:</strong> Kotlin compiler API changes can affect the plugin sooner than KSP-based flows.</li>
          <li><strong>No mixed backends per module:</strong> Do not enable KSP and compiler-plugin generation in the same module.</li>
          <li><strong>Different debugging experience:</strong> You do not inspect generated <code>.kt</code> files in compiler-plugin mode.</li>
          <li><strong>Local plugin development caveat:</strong> Repository changes to the compiler plugin can require publishing plugin artifacts to <code>mavenLocal</code> before downstream demo verification.</li>
        </ul>
      </section>

      <section>
        <h2 id="verification">Verification</h2>
        <p>
          After switching, verify the experimental path with a clean desktop build and a multi-module metadata pass in compiler mode.
        </p>
        <CodeBlock code={verificationCommands} language="bash" title="Verification Commands" />
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/getting-started">Getting Started</Link> – Start with the stable KSP path, then compare backends.</li>
          <li><Link to="/features/annotation-api">Annotation-Based API</Link> – Review the backend-neutral annotation model.</li>
          <li><Link to="/features/modular">Modular Architecture</Link> – Start from explicit module config composition, then compare it with <code>@NavigationRoot</code>.</li>
          <li><a href="https://github.com/jermeyyy/quo-vadis/blob/compiler-plugin/docs/MIGRATION.md" target="_blank" rel="noopener noreferrer">Migration Guide</a> – Full backend-switching details and rollback steps.</li>
        </ul>
      </section>
    </article>
  )
}