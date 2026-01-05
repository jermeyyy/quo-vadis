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
        provides its own NavigationConfig, which are then combined in the app module. 
        This enables independent feature development, clear module boundaries, 
        reusable feature modules, and easier testing and maintenance.
      </p>

      <section>
        <h2 id="module-structure">Module Structure Pattern</h2>
        <p>
          A typical multi-module project organizes navigation across feature modules, 
          with each module containing its destinations, screens, and a generated 
          NavigationConfig:
        </p>
        <CodeBlock code={moduleStructure} language="bash" />
      </section>

      <section>
        <h2 id="ksp-prefix">KSP Module Prefix Configuration</h2>
        <p>
          Each module needs a unique prefix to generate distinct NavigationConfig classes. 
          Configure this in each feature module's build file:
        </p>
        <CodeBlock code={kspPrefixCode} language="kotlin" />
      </section>

      <section>
        <h2 id="combining-configs">Combining Configurations</h2>
        <p>
          Use the <code>+</code> operator to combine NavigationConfig instances from 
          multiple modules. This is typically done in your app module's dependency 
          injection setup:
        </p>
        <CodeBlock code={combiningConfigsCode} language="kotlin" />
        <p>
          <strong>Priority rule:</strong> The right-hand config takes priority for 
          duplicate registrations. This allows feature modules to override app-level 
          defaults when needed.
        </p>
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
          Each feature module defines its own destinations and screens. KSP generates 
          the NavigationConfig automatically:
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
        <h2 id="generated-files">Generated Files Per Module</h2>
        <table>
          <thead>
            <tr>
              <th>Module</th>
              <th>Generated Config</th>
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
