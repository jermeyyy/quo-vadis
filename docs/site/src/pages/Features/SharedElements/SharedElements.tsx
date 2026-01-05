import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const navigationHostCode = `@Composable
fun NavigationHost(...) {
    SharedTransitionLayout(modifier = modifier) {
        // NavRenderScope gets sharedTransitionScope = this
        NavNodeRenderer(...)
    }
}`

const transitionScopeAccessCode = `@Composable
fun MyScreen() {
    val transitionScope = LocalTransitionScope.current
    
    transitionScope?.let { scope ->
        // scope.sharedTransitionScope - for sharedElement/sharedBounds
        // scope.animatedVisibilityScope - for animateEnterExit
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

export default function SharedElements() {
  return (
    <article className={styles.features}>
      <h1>Shared Element Transitions</h1>
      <p className={styles.intro}>
        Quo Vadis integrates with Compose's <code>SharedTransitionLayout</code> for seamless 
        element morphing between screens. Elements with matching keys animate smoothly during 
        navigation, creating stunning visual continuity.
      </p>

      <section>
        <h2 id="how-it-works">How It Works</h2>
        <p>
          <code>NavigationHost</code> automatically wraps content in <code>SharedTransitionLayout</code>, 
          providing the transition scope to all child screens:
        </p>
        <CodeBlock code={navigationHostCode} language="kotlin" />
      </section>

      <section>
        <h2 id="accessing-transition-scope">Accessing TransitionScope</h2>
        <p>
          Access the transition scope in any screen using <code>LocalTransitionScope</code>:
        </p>
        <CodeBlock code={transitionScopeAccessCode} language="kotlin" />
        <p>The <code>TransitionScope</code> interface provides:</p>
        <CodeBlock code={transitionScopeInterfaceCode} language="kotlin" />
      </section>

      <section>
        <h2 id="sharedelement-vs-sharedbounds">sharedElement vs sharedBounds</h2>
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
      </section>

      <section>
        <h2 id="sharedelement-example">sharedElement Example</h2>
        <p>
          Use <code>sharedElement</code> for icons, images, and other elements that remain 
          visually identical between screens.
        </p>
        <h3>List Screen</h3>
        <CodeBlock code={sharedElementListCode} language="kotlin" />
        <h3>Detail Screen</h3>
        <CodeBlock code={sharedElementDetailCode} language="kotlin" />
      </section>

      <section>
        <h2 id="sharedbounds-example">sharedBounds Example</h2>
        <p>
          Use <code>sharedBounds</code> for cards and containers where the content changes 
          but the bounds should morph smoothly:
        </p>
        <CodeBlock code={sharedBoundsCode} language="kotlin" />
      </section>

      <section>
        <h2 id="key-rules">Key Rules</h2>
        <ol>
          <li><strong>Keys must match exactly</strong> between source and destination screens</li>
          <li>Use unique, stable identifiers: <code>"icon-$&#123;item.id&#125;"</code>, <code>"card-$&#123;itemId&#125;"</code></li>
          <li>Works in <strong>both forward AND backward</strong> navigation</li>
          <li>Pattern: <code>"element-type-unique-id"</code> (e.g., <code>"avatar-user123"</code>)</li>
        </ol>
      </section>

      <section>
        <h2 id="combining-with-animateenterexit">Combining with animateEnterExit</h2>
        <p>
          For non-shared content, use <code>animateEnterExit</code> alongside shared elements 
          to create coordinated animations:
        </p>
        <CodeBlock code={animateEnterExitCode} language="kotlin" />
      </section>

      <section>
        <h2 id="using-transitiontype-none">Using TransitionType.None</h2>
        <p>
          When shared elements are your primary transition effect, disable the default 
          screen transition with <code>TransitionType.None</code>:
        </p>
        <CodeBlock code={transitionTypeNoneCode} language="kotlin" />
        <p>
          This gives you full control of animations using shared elements and <code>animateEnterExit</code>.
        </p>
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <ul>
          <li><strong>Keep keys consistent and predictable</strong> - Use a clear naming convention</li>
          <li><strong>Use TransitionType.None</strong> when shared elements are the primary transition</li>
          <li><strong>Test both directions</strong> - Verify forward and backward navigation</li>
          <li><strong>Handle null gracefully</strong> - Always provide a fallback for null transitionScope</li>
          <li><strong>Don't overdo it</strong> - Avoid sharing too many elements at once</li>
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
