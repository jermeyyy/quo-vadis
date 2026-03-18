import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const moduleStructure = `app/
├── build.gradle.kts
├── src/commonMain/kotlin/
│   └── DI.kt  (combines all configs)
│
feature1/
├── build.gradle.kts
├── src/commonMain/kotlin/
│   ├── Feature1Destinations.kt
│   ├── Feature1Screens.kt
│   └── Feature1NavigationConfig.kt  (generated)
│
feature2/
├── build.gradle.kts
├── src/commonMain/kotlin/
│   ├── Feature2Destinations.kt
│   ├── Feature2Screens.kt
│   └── Feature2NavigationConfig.kt  (generated)`

const kspPrefixCode = `// feature1/build.gradle.kts
quoVadis {
    modulePrefix = "Feature1"  // Generates Feature1NavigationConfig
}

// feature2/build.gradle.kts
quoVadis {
    modulePrefix = "Feature2"  // Generates Feature2NavigationConfig
}`

const combiningConfigsCode = `// app/src/commonMain/kotlin/DI.kt
val navigationModule = module {
    single<NavigationConfig> {
        AppNavigationConfig +      // App module screens
            Feature1NavigationConfig +  // Feature 1 module
            Feature2NavigationConfig    // Feature 2 module
    }
    
    single<Navigator> {
        val config = get<NavigationConfig>()
        val initialState = config.buildNavNode(MainTabs::class, null)!!
        TreeNavigator(config = config, initialState = initialState)
    }
}`

  const navigationRootCode = `@NavigationRoot
  object AppRoot

  val navigator = rememberQuoVadisNavigator(
    MainTabs::class,
    AppRootNavigationConfig
  )`

const navigationConfigInterface = `interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val scopeRegistry: ScopeRegistry
    val transitionRegistry: TransitionRegistry
    val containerRegistry: ContainerRegistry
    val deepLinkRegistry: DeepLinkRegistry
    val paneRoleRegistry: PaneRoleRegistry
    
    fun buildNavNode(destinationClass: KClass<*>, parentKey: String?): NavNode?
    
    operator fun plus(other: NavigationConfig): NavigationConfig
}`

const featureModuleCode = `// feature1/src/commonMain/kotlin/Feature1Destinations.kt
@Stack(name = "feature1", startDestination = Feature1List::class)
sealed class Feature1Destination : NavDestination {
    @Destination(route = "feature1/list")
    data object Feature1List : Feature1Destination()
    
    @Destination(route = "feature1/detail/{id}")
    data class Feature1Detail(@Argument val id: String) : Feature1Destination()
}

// feature1/src/commonMain/kotlin/Feature1Screens.kt
@Screen(Feature1Destination.Feature1List::class)
@Composable
fun Feature1ListScreen(navigator: Navigator) { /* ... */ }

@Screen(Feature1Destination.Feature1Detail::class)
@Composable
fun Feature1DetailScreen(
    destination: Feature1Destination.Feature1Detail, 
    navigator: Navigator
) { /* ... */ }

// After KSP generates Feature1NavigationConfig`

const crossModuleCode = `// In feature1, navigate to feature2 destination
navigator.navigate(Feature2Destination.SomeScreen)

// Works because configs are combined in app module`

const crossModuleTabsCode = `// shared-api module — just the @Tabs declaration
@Tabs(name = "main")
object MainTabs

// feature1 module — registers itself as a tab
@TabItem(parent = MainTabs::class, ordinal = 0)
@Stack(name = "feature1", startDestination = Feature1List::class)
sealed class Feature1Destination : NavDestination { /* ... */ }

// feature2 module — registers itself as another tab
@TabItem(parent = MainTabs::class, ordinal = 1)
@Stack(name = "feature2", startDestination = Feature2Home::class)
sealed class Feature2Destination : NavDestination { /* ... */ }`

const featureDependenciesCode = `// feature1/build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
    id("io.github.jermeyyy.quo-vadis")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":quo-vadis-core"))
            implementation(project(":quo-vadis-annotations"))
        }
    }
}

quoVadis {
    modulePrefix = "Feature1"
}`

const hybridConfigCode = `val generatedConfig = AppNavigationConfig

val dynamicConfig = navigationConfig {
    if (featureFlags.isNewProfileEnabled) {
        screen<NewProfileScreen> { dest, _, _ ->
            { NewProfileContent(dest) }
        }
    }
}

val finalConfig = generatedConfig + dynamicConfig`

export default function Modular() {
  return (
    <article className={styles.features}>
      <h1>Modular Architecture</h1>
      <p className={styles.intro}>
        Quo Vadis supports multi-module architectures where each feature module 
        provides its own NavigationConfig. The backend-neutral default is to combine those module-level configs
        explicitly in the app module, while the compiler plugin can optionally add automatic aggregation.
        This enables independent feature development, clear module boundaries, 
        reusable feature modules, and easier testing and maintenance.
      </p>

      <section>
        <h2 id="module-structure">Module Structure Pattern</h2>
        <p>
          A typical multi-module project organizes navigation across feature modules, 
          with each module containing its destinations, screens, and a module-level
          navigation config output:
        </p>
        <CodeBlock code={moduleStructure} language="bash" />
      </section>

      <section>
        <h2 id="ksp-prefix">Module Prefix Configuration</h2>
        <p>
          Each module needs a unique prefix to produce a distinct NavigationConfig. Configure this
          in each feature module's build file:
        </p>
        <CodeBlock code={kspPrefixCode} language="kotlin" />
      </section>

      <section>
        <h2 id="combining-configs">Combining Configurations</h2>
        <p>
          Use the <code>+</code> operator to combine NavigationConfig instances from 
          multiple modules. This explicit composition is the default path across backends and is typically
          done in your app module's dependency injection setup:
        </p>
        <CodeBlock code={combiningConfigsCode} language="kotlin" />
        <p>
          <strong>Priority rule:</strong> The right-hand config takes priority for 
          duplicate registrations. This allows feature modules to override app-level 
          defaults when needed.
        </p>

        <div className={styles.note}>
          <p>
            Keep explicit <code>+</code> composition as your baseline, even if you plan to evaluate the experimental compiler backend.
            In compiler-plugin mode, you can additionally add <code>@NavigationRoot</code> in the app module and let the experimental
            backend aggregate visible module configs automatically. See <Link to="/features/compiler-plugin">Compiler Plugin (Experimental)</Link>
            {' '}for the backend-specific setup and trade-offs.
          </p>
        </div>

        <h3>Compiler-Only Experimental Aggregation</h3>
        <p>
          If you opt into the compiler plugin, <code>@NavigationRoot</code> can reduce the app-level wiring. This is a
          compiler-only enhancement and should not replace the baseline mental model of module configs plus explicit composition.
        </p>
        <CodeBlock code={navigationRootCode} language="kotlin" />
      </section>

      <section>
        <h2 id="config-interface">NavigationConfig Interface</h2>
        <p>
          The NavigationConfig interface combines all navigation-related registries 
          and provides the <code>plus</code> operator for composition:
        </p>
        <CodeBlock code={navigationConfigInterface} language="kotlin" />
      </section>

      <section>
        <h2 id="feature-module">Feature Module Example</h2>
        <p>
          Each feature module defines its own destinations and screens. The annotation backends
          produce the module NavigationConfig automatically, while the DSL can still be composed manually:
        </p>
        <CodeBlock code={featureModuleCode} language="kotlin" />
      </section>

      <section>
        <h2 id="cross-module">Cross-Module Navigation</h2>
        <p>
          Navigate between modules using type-safe destination references. This works 
          because all configs are combined in the app module:
        </p>
        <CodeBlock code={crossModuleCode} language="kotlin" />
      </section>

      <section>
        <h2 id="cross-module-tabs">Cross-Module Tab Registration</h2>
        <p>
          Feature modules can register themselves as tabs in a shared tab container
          using the <code>@TabItem</code> annotation. The parent <code>@Tabs</code> declaration
          lives in a shared API module, while each feature module independently declares
          its tab membership and position via <code>ordinal</code>:
        </p>
        <CodeBlock code={crossModuleTabsCode} language="kotlin" />
        <p>
          Each <code>@TabItem</code> specifies its <code>parent</code> (the <code>@Tabs</code>-annotated class)
          and an <code>ordinal</code> (0-based display position, where <code>ordinal = 0</code> is the initial tab).
          Since tab items are spread across modules, ordinal continuity validation is
          skipped at compile time for cross-module tabs.
          See <a href="/features/tabbed-navigation">Tabbed Navigation</a> for full details
          on the <code>@Tabs</code> / <code>@TabItem</code> API.
        </p>
      </section>

      <section>
        <h2 id="dependencies">Feature Module Dependencies</h2>
        <p>
          Each feature module requires the Quo Vadis plugin and core dependencies:
        </p>
        <CodeBlock code={featureDependenciesCode} language="kotlin" />
      </section>

      <section>
        <h2 id="hybrid-config">Hybrid Configuration</h2>
        <p>
          Combine generated configs with manual additions for dynamic features 
          like feature flags:
        </p>
        <CodeBlock code={hybridConfigCode} language="kotlin" />
      </section>

      <section>
        <h2 id="generated-files">Module Configs Per Module</h2>
        <table>
          <thead>
            <tr>
              <th>Module</th>
              <th>Config</th>
              <th>Contains</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>app</code></td>
              <td><code>AppNavigationConfig</code></td>
              <td>Tab containers, main screens</td>
            </tr>
            <tr>
              <td><code>feature1</code></td>
              <td><code>Feature1NavigationConfig</code></td>
              <td>Feature 1 screens</td>
            </tr>
            <tr>
              <td><code>feature2</code></td>
              <td><code>Feature2NavigationConfig</code></td>
              <td>Feature 2 screens</td>
            </tr>
          </tbody>
        </table>

        <div className={styles.note}>
          <p>
            KSP exposes these configs as generated source files. The compiler plugin exposes equivalent module-level configs through
            compiler synthesis instead of writing visible <code>.kt</code> outputs.
          </p>
        </div>
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <ul>
          <li><strong>Consistent naming:</strong> Use <code>{'{ModuleName}'}NavigationConfig</code> pattern</li>
          <li><strong>Co-location:</strong> Define destinations close to their screens</li>
          <li><strong>Independence:</strong> Keep feature modules independent of each other</li>
          <li><strong>Central composition:</strong> Combine configs only in app module</li>
          <li><strong>Cross-boundary testing:</strong> Test navigation across module boundaries</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/di-integration">DI Integration</a> - Set up dependency injection with navigation</li>
          <li><a href="/features/deep-linking">Deep Linking</a> - Add URL-based navigation</li>
          <li><a href="/demo">See the demo</a> with modular structure</li>
        </ul>
      </section>
    </article>
  )
}
