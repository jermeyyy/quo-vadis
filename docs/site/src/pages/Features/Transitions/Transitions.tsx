import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const transitionAnnotationCode = `@Stack(name = "home", startDestination = HomeDestination.List::class)
sealed class HomeDestination : NavDestination {

    @Destination(route = "list")
    data object List : HomeDestination()

    @Transition(type = TransitionType.SlideHorizontal)
    @Destination(route = "details/{id}")
    data class Details(@Argument val id: String) : HomeDestination()

    @Transition(type = TransitionType.SlideVertical)
    @Destination(route = "filter")
    data object Filter : HomeDestination()

    @Transition(type = TransitionType.Fade)
    @Destination(route = "help")
    data object Help : HomeDestination()
}`

const customTransitionBuilderCode = `// Using TransitionBuilder DSL
val myTransition = customTransition {
    enter = fadeIn() + expandHorizontally()
    exit = fadeOut() + shrinkHorizontally()
    popEnter = fadeIn() + expandHorizontally(expandFrom = Alignment.End)
    popExit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
}

// Using NavTransition directly
val scaleAndFade = NavTransition(
    enter = scaleIn(initialScale = 0.8f) + fadeIn(),
    exit = scaleOut(targetScale = 1.2f) + fadeOut(),
    popEnter = scaleIn(initialScale = 1.2f) + fadeIn(),
    popExit = scaleOut(targetScale = 0.8f) + fadeOut()
)`

const dslRegistrationCode = `val config = navigationConfig {
    transition<DetailScreen>(NavTransition.SlideHorizontal)
    transition<ModalScreen>(NavTransition.SlideVertical)
    transition<SettingsScreen>(NavTransition.Fade)
}`

const transitionScopeAccessCode = `@Composable
fun MyScreen() {
    val transitionScope = LocalTransitionScope.current
    
    transitionScope?.let { scope ->
        // scope.sharedTransitionScope - for sharedElement/sharedBounds
        // scope.animatedVisibilityScope - for animateEnterExit
    }
}`

const sharedElementCode = `Icon(
    modifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "icon-\${item.id}"),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else Modifier
)`

const sharedBoundsCode = `Card(
    modifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "card-\${item.id}"),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else Modifier
)`

export default function Transitions() {
  return (
    <article className={styles.features}>
      <h1>Transitions & Animations</h1>
      <p className={styles.intro}>
        Rich set of built-in transitions and support for custom animations. 
        Create polished, professional navigation experiences with Quo Vadis's 
        tree-based transition system.
      </p>

      <section>
        <h2 id="overview">How Transitions Work</h2>
        <p>
          Transitions in Quo Vadis are deeply integrated with the tree-based navigation 
          architecture. The system provides smooth, coordinated animations across all 
          navigation contexts.
        </p>
        <ul>
          <li><strong>NavigationHost</strong> wraps content in <code>SharedTransitionLayout</code> for shared element support</li>
          <li><strong>NavNodeRenderer</strong> dispatches to specialized renderers for each node type</li>
          <li>Each renderer uses <strong>AnimatedNavContent</strong> for coordinated transitions</li>
        </ul>
        <p>
          This architecture ensures transitions work consistently whether you're navigating 
          within stacks, switching tabs, or using pane layouts.
        </p>
      </section>

      <section>
        <h2 id="built-in">Built-in Transitions</h2>
        <p>
          Quo Vadis provides five built-in transition presets optimized for common 
          navigation patterns:
        </p>
        <table>
          <thead>
            <tr>
              <th>Transition</th>
              <th>Enter</th>
              <th>Exit</th>
              <th>Best For</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>SlideHorizontal</code></td>
              <td>Slide from right</td>
              <td>Slide to left</td>
              <td>Stack navigation (default)</td>
            </tr>
            <tr>
              <td><code>SlideVertical</code></td>
              <td>Slide from bottom</td>
              <td>Slide to top</td>
              <td>Modal sheets</td>
            </tr>
            <tr>
              <td><code>Fade</code></td>
              <td>Fade in</td>
              <td>Fade out</td>
              <td>Tab switching, overlays</td>
            </tr>
            <tr>
              <td><code>ScaleIn</code></td>
              <td>Scale from 80%</td>
              <td>Scale to 95%</td>
              <td>Detail view transitions</td>
            </tr>
            <tr>
              <td><code>None</code></td>
              <td>Instant</td>
              <td>Instant</td>
              <td>Testing, custom animations</td>
            </tr>
          </tbody>
        </table>

        <div className={styles.transitionGrid}>
          <div className={styles.transitionCard}>
            <h4>SlideHorizontal</h4>
            <p>Standard horizontal slide for hierarchical navigation</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>SlideVertical</h4>
            <p>Vertical slide for modal presentations</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>Fade</h4>
            <p>Cross-fade for tab switches and overlays</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>ScaleIn</h4>
            <p>Scale animation emphasizing detail views</p>
          </div>
          <div className={styles.transitionCard}>
            <h4>None</h4>
            <p>Instant navigation without animation</p>
          </div>
        </div>
      </section>

      <section>
        <h2 id="transition-annotation">@Transition Annotation</h2>
        <p>
          Apply transitions per-destination using the <code>@Transition</code> annotation. 
          This allows fine-grained control over how each screen enters and exits.
        </p>
        <CodeBlock code={transitionAnnotationCode} language="kotlin" />
        <p>
          Each destination can specify its own transition type, overriding the default 
          for its navigation context.
        </p>
      </section>

      <section>
        <h2 id="phases">Transition Phases</h2>
        <p>
          Every transition defines four animation phases to handle both forward 
          navigation (push) and backward navigation (pop):
        </p>
        <table>
          <thead>
            <tr>
              <th>Phase</th>
              <th>Direction</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>enter</code></td>
              <td>Forward</td>
              <td>New screen appearing (push)</td>
            </tr>
            <tr>
              <td><code>exit</code></td>
              <td>Forward</td>
              <td>Current screen disappearing (push)</td>
            </tr>
            <tr>
              <td><code>popEnter</code></td>
              <td>Back</td>
              <td>Previous screen re-appearing (pop)</td>
            </tr>
            <tr>
              <td><code>popExit</code></td>
              <td>Back</td>
              <td>Current screen disappearing (pop)</td>
            </tr>
          </tbody>
        </table>
        <p>
          This four-phase model ensures animations look correct in both directions, 
          providing a polished user experience.
        </p>
      </section>

      <section>
        <h2 id="custom">Custom Transitions</h2>
        <p>
          Create custom transitions using either the DSL builder or by constructing 
          <code>NavTransition</code> directly. Compose's animation combinators 
          (like <code>+</code>) allow combining multiple effects.
        </p>
        <CodeBlock code={customTransitionBuilderCode} language="kotlin" />
        <p>
          Custom transitions give you complete control over timing, easing, and 
          animation composition for unique navigation effects.
        </p>
      </section>

      <section>
        <h2 id="dsl-registration">DSL-based Registration</h2>
        <p>
          Register transitions programmatically using the navigation config DSL. 
          This approach works well when transitions are determined at runtime or 
          managed centrally.
        </p>
        <CodeBlock code={dslRegistrationCode} language="kotlin" />
      </section>

      <section>
        <h2 id="shared-elements">Shared Element Transitions</h2>
        <p>
          Quo Vadis integrates with Compose's <code>SharedTransitionLayout</code> for 
          fluid shared element animations. Access the transition scope via 
          <code>LocalTransitionScope</code>.
        </p>
        
        <h3>Accessing the Transition Scope</h3>
        <CodeBlock code={transitionScopeAccessCode} language="kotlin" />

        <h3>sharedElement (Exact Visual Matches)</h3>
        <p>
          Use <code>sharedElement</code> for content that remains visually identical 
          between screens, such as icons, images, or avatars.
        </p>
        <CodeBlock code={sharedElementCode} language="kotlin" />

        <h3>sharedBounds (Containers with Different Content)</h3>
        <p>
          Use <code>sharedBounds</code> for containers where the bounds transition 
          but the content may differ, such as expanding cards.
        </p>
        <CodeBlock code={sharedBoundsCode} language="kotlin" />
      </section>

      <section>
        <h2 id="shared-element-rules">Key Rules for Shared Elements</h2>
        <ul>
          <li><strong>Keys must match exactly</strong> between source and destination screens</li>
          <li><strong>Use unique, stable identifiers:</strong> <code>"icon-{'${item.id}'}"</code>, <code>"card-{'${itemId}'}"</code></li>
          <li>Works in <strong>both forward AND backward</strong> navigation</li>
          <li>Use <code>sharedElement</code> for icons/images, <code>sharedBounds</code> for containers</li>
          <li>Always check for <code>null</code> when accessing <code>LocalTransitionScope</code></li>
        </ul>
      </section>

      <section>
        <h2 id="context-behavior">Context-Specific Behavior</h2>
        <p>
          Different navigation contexts use different default transitions optimized 
          for their use case:
        </p>
        <table>
          <thead>
            <tr>
              <th>Context</th>
              <th>Default Transition</th>
              <th>Notes</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Stack navigation</td>
              <td><code>SlideHorizontal</code></td>
              <td>Hierarchical push/pop</td>
            </tr>
            <tr>
              <td>Tab switching</td>
              <td><code>Fade</code></td>
              <td>Parallel content switching</td>
            </tr>
            <tr>
              <td>Pane transitions</td>
              <td><code>Fade</code></td>
              <td>Based on adaptive mode</td>
            </tr>
          </tbody>
        </table>
        <p>
          These defaults can be overridden per-destination using the <code>@Transition</code> 
          annotation or programmatically via the DSL.
        </p>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/predictive-back">Predictive Back</a> - Gesture-driven navigation with preview</li>
          <li><a href="/features/deep-links">Deep Links</a> - URL-based navigation</li>
          <li><a href="/demo">See the demo</a> - Experience all transitions in action</li>
        </ul>
      </section>
    </article>
  )
}
