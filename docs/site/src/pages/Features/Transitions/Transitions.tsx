import { Link } from 'react-router-dom'
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

const navigationHostCode = `@Composable
fun NavigationHost(...) {
    SharedTransitionLayout(modifier = modifier) {
        // NavRenderScope gets sharedTransitionScope = this
        NavNodeRenderer(...)
    }
}`

const transitionScopeInterfaceCode = `interface TransitionScope {
    val sharedTransitionScope: SharedTransitionScope
    val animatedVisibilityScope: AnimatedVisibilityScope
}`

const sharedElementListCode = `@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ItemCard(item: Item, onClick: () -> Unit) {
    val transitionScope = LocalTransitionScope.current

    Card(onClick = onClick) {
        Row {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                modifier = if (transitionScope != null) {
                    with(transitionScope.sharedTransitionScope) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(
                                key = "icon-\${item.id}"
                            ),
                            animatedVisibilityScope = transitionScope.animatedVisibilityScope
                        )
                    }
                } else Modifier
            )
            Text(item.title)
        }
    }
}`

const sharedElementDetailCode = `@Screen(MasterDetailDestination.Detail::class)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(destination: MasterDetailDestination.Detail) {
    val transitionScope = LocalTransitionScope.current
    val itemId = destination.itemId

    Column {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            modifier = if (transitionScope != null) {
                with(transitionScope.sharedTransitionScope) {
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(
                            key = "icon-$itemId"
                        ),
                        animatedVisibilityScope = transitionScope.animatedVisibilityScope
                    )
                }
            } else Modifier
        )
        Text("Details for $itemId")
    }
}`

const sharedBoundsCode = `// List item card
Card(
    modifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "card-container-\${item.id}"
                ),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else Modifier
) {
    // List item content
}

// Detail card (same key, bounds morph)
Card(
    modifier = if (transitionScope != null) {
        with(transitionScope.sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(
                    key = "card-container-$itemId"
                ),
                animatedVisibilityScope = transitionScope.animatedVisibilityScope
            )
        }
    } else Modifier
) {
    // Expanded detail content
}`

const animateEnterExitCode = `@Composable
fun DetailScreen() {
    val transitionScope = LocalTransitionScope.current

    Column {
        // Shared element (morphs)
        Icon(modifier = sharedElementModifier)
        
        // Non-shared content with enter/exit animation
        Text(
            "Description",
            modifier = Modifier.animateEnterExit(
                transitionScope,
                enter = fadeIn(tween(300, delayMillis = 100)),
                exit = fadeOut(tween(200))
            )
        )
    }
}

// Helper extension
private fun Modifier.animateEnterExit(
    transitionScope: TransitionScope?,
    enter: EnterTransition,
    exit: ExitTransition
): Modifier = if (transitionScope != null) {
    with(transitionScope.animatedVisibilityScope) {
        this@animateEnterExit.animateEnterExit(enter = enter, exit = exit)
    }
} else this`

const transitionTypeNoneCode = `@Transition(type = TransitionType.None)
@Destination(route = "master_detail/detail/{itemId}")
data class Detail(@Argument val itemId: String) : MasterDetailDestination()`

export default function Transitions() {
  return (
    <article className={styles.features}>
      <h1>Transitions & Animations</h1>
      <p className={styles.intro}>
        Rich set of built-in transitions, custom animations, and shared element support. 
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

      {/* =========================================== */}
      {/* SHARED ELEMENTS SECTION - EXPANDED FROM MERGE */}
      {/* =========================================== */}

      <section>
        <h2 id="shared-elements">Shared Element Transitions</h2>
        <p>
          Quo Vadis integrates with Compose's <code>SharedTransitionLayout</code> for seamless 
          element morphing between screens. Elements with matching keys animate smoothly during 
          navigation, creating stunning visual continuity.
        </p>

        <h3 id="shared-how-it-works">How It Works</h3>
        <p>
          <code>NavigationHost</code> automatically wraps content in <code>SharedTransitionLayout</code>, 
          providing the transition scope to all child screens:
        </p>
        <CodeBlock code={navigationHostCode} language="kotlin" />

        <h3 id="accessing-transition-scope">Accessing TransitionScope</h3>
        <p>
          Access the transition scope in any screen using <code>LocalTransitionScope</code>:
        </p>
        <CodeBlock code={transitionScopeAccessCode} language="kotlin" />
        <p>The <code>TransitionScope</code> interface provides:</p>
        <CodeBlock code={transitionScopeInterfaceCode} language="kotlin" />

        <h3 id="sharedelement-vs-sharedbounds">sharedElement vs sharedBounds</h3>
        <p>Choose the right modifier based on your use case:</p>
        <table>
          <thead>
            <tr>
              <th>Modifier</th>
              <th>Use Case</th>
              <th>Content Behavior</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>sharedElement</code></td>
              <td>Exact visual matches (same icon, same image)</td>
              <td>Content stays identical during transition</td>
            </tr>
            <tr>
              <td><code>sharedBounds</code></td>
              <td>Container morphing (card expanding to full screen)</td>
              <td>Content can change while bounds morph</td>
            </tr>
          </tbody>
        </table>

        <h3 id="sharedelement-example">sharedElement Example</h3>
        <p>
          Use <code>sharedElement</code> for icons, images, and other elements that remain 
          visually identical between screens.
        </p>
        <h4>List Screen</h4>
        <CodeBlock code={sharedElementListCode} language="kotlin" />
        <h4>Detail Screen</h4>
        <CodeBlock code={sharedElementDetailCode} language="kotlin" />

        <h3 id="sharedbounds-example">sharedBounds Example</h3>
        <p>
          Use <code>sharedBounds</code> for cards and containers where the content changes 
          but the bounds should morph smoothly:
        </p>
        <CodeBlock code={sharedBoundsCode} language="kotlin" />

        <h3 id="combining-animateenterexit">Combining with animateEnterExit</h3>
        <p>
          For non-shared content, use <code>animateEnterExit</code> alongside shared elements 
          to create coordinated animations:
        </p>
        <CodeBlock code={animateEnterExitCode} language="kotlin" />

        <h3 id="transitiontype-none">Using TransitionType.None</h3>
        <p>
          When shared elements are your primary transition effect, disable the default 
          screen transition with <code>TransitionType.None</code>:
        </p>
        <CodeBlock code={transitionTypeNoneCode} language="kotlin" />
        <p>
          This gives you full control of animations using shared elements and <code>animateEnterExit</code>.
        </p>

        <h3 id="shared-element-rules">Key Rules for Shared Elements</h3>
        <ol>
          <li><strong>Keys must match exactly</strong> between source and destination screens</li>
          <li>Use unique, stable identifiers: <code>"icon-$&#123;item.id&#125;"</code>, <code>"card-$&#123;itemId&#125;"</code></li>
          <li>Works in <strong>both forward AND backward</strong> navigation</li>
          <li>Pattern: <code>"element-type-unique-id"</code> (e.g., <code>"avatar-user123"</code>)</li>
          <li>Always handle <code>null</code> gracefully for <code>transitionScope</code></li>
        </ol>
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <h3>Screen Transitions</h3>
        <ul>
          <li>Use <code>SlideHorizontal</code> for hierarchical navigation</li>
          <li>Use <code>SlideVertical</code> for modal presentations</li>
          <li>Use <code>Fade</code> for tab switches and overlays</li>
          <li>Define all four phases (enter, exit, popEnter, popExit) for custom transitions</li>
        </ul>
        <h3>Shared Elements</h3>
        <ul>
          <li><strong>Keep keys consistent and predictable</strong> - Use a clear naming convention</li>
          <li><strong>Use TransitionType.None</strong> when shared elements are the primary transition</li>
          <li><strong>Test both directions</strong> - Verify forward and backward navigation</li>
          <li><strong>Don't overdo it</strong> - Avoid sharing too many elements at once</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/features/predictive-back">Predictive Back</Link> - Gesture-driven navigation with preview</li>
          <li><Link to="/features/deep-links">Deep Links</Link> - URL-based navigation</li>
          <li><Link to="/demo">Live Demo</Link> - Experience all transitions in action</li>
        </ul>
      </section>
    </article>
  )
}
