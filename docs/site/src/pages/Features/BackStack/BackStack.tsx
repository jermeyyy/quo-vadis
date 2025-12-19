import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const navNodeTreeCode = `// Navigation state is an immutable NavNode tree
sealed interface NavNode {
    val key: String
}

// ScreenNode - represents a single destination
data class ScreenNode(
    override val key: String,
    val destination: Destination
) : NavNode

// StackNode - a stack of screens
data class StackNode(
    override val key: String,
    val children: List<ScreenNode>
) : NavNode

// TabNode - container for tab-based navigation
data class TabNode(
    override val key: String,
    val children: List<NavNode>,
    val activeIndex: Int
) : NavNode`

const treeMutatorCode = `// Access current NavNode tree state
val navState: NavNode = navigator.state.value

// Push a new destination onto the active stack
val newState = TreeMutator.push(navState, DetailsDestination(id = "123"))
navigator.updateState(newState)

// Pop from the active stack
val poppedState = TreeMutator.pop(navState)
poppedState?.let { navigator.updateState(it) }

// Replace current destination
val replacedState = TreeMutator.replaceCurrent(navState, NewDestination)
navigator.updateState(replacedState)

// Clear stack and push new destination
val clearedState = TreeMutator.clearAndPush(navState, HomeDestination)
navigator.updateState(clearedState)

// Switch active tab (for TabNode structures)
val tabState = TreeMutator.switchActiveTab(navState, newIndex = 1)
navigator.updateState(tabState)`

const navigatorMethodsCode = `// Navigator provides convenient methods that use TreeMutator internally
navigator.navigate(DetailsDestination(id = "123"))
navigator.navigateBack()
navigator.navigateAndReplace(NewDestination)
navigator.navigateAndClearAll(StartDestination)

// Direct state update for advanced scenarios
val customState = TreeMutator.clearToRoot(currentState) { it.key == "main" }
navigator.updateState(customState)`

const observingStateCode = `// Observe navigation state changes
@Composable
fun NavigationAwareScreen() {
    val navigator = LocalNavigator.current
    val navState by navigator.state.collectAsState()
    
    // React to state changes
    val currentDestination = navState.activeLeaf()?.destination
    val canGoBack = TreeMutator.canGoBack(navState)
    
    LaunchedEffect(currentDestination) {
        // Handle destination changes
    }
}`

export default function BackStack() {
  return (
    <article className={styles.features}>
      <h1>Stack Management (NavNode Tree)</h1>
      <p className={styles.intro}>
        Quo Vadis uses an immutable NavNode tree to represent navigation state. 
        All state mutations are performed through pure functional TreeMutator operations, 
        providing predictable, testable, and serializable navigation behavior.
      </p>

      <section>
        <h2 id="navnode-tree">NavNode Tree Structure</h2>
        <p>
          The navigation state is represented as an immutable tree of NavNode objects. 
          Different node types support different navigation patterns:
        </p>
        <CodeBlock code={navNodeTreeCode} language="kotlin" />
        
        <h3>Node Types</h3>
        <ul>
          <li><strong>ScreenNode:</strong> Represents a single destination/screen</li>
          <li><strong>StackNode:</strong> A stack of screens for hierarchical navigation</li>
          <li><strong>TabNode:</strong> Container for tab-based navigation with independent child stacks</li>
          <li><strong>PaneNode:</strong> For multi-pane layouts (list-detail, etc.)</li>
        </ul>
      </section>

      <section>
        <h2 id="tree-mutator">TreeMutator Operations</h2>
        <p>
          TreeMutator provides pure functional operations for transforming the NavNode tree. 
          All operations return new immutable trees without modifying the original.
        </p>
        <CodeBlock code={treeMutatorCode} language="kotlin" />
      </section>

      <section>
        <h2 id="navigator-methods">Navigator Convenience Methods</h2>
        <p>
          Navigator provides high-level methods that use TreeMutator internally:
        </p>
        <CodeBlock code={navigatorMethodsCode} language="kotlin" />
      </section>

      <section>
        <h2 id="observing-state">Observing Navigation State</h2>
        <p>
          The NavNode tree state is observable via <code>StateFlow&lt;NavNode&gt;</code>:
        </p>
        <CodeBlock code={observingStateCode} language="kotlin" />
      </section>

      <section>
        <h2 id="use-cases">Use Cases</h2>
        <ul>
          <li>Multi-step wizards with immutable state transformations</li>
          <li>Authentication flows that clear navigation stacks</li>
          <li>Tab-based navigation with independent TabNode children</li>
          <li>State persistence via NavNode serialization</li>
          <li>Time-travel debugging by storing NavNode snapshots</li>
          <li>Testing navigation logic with predictable state assertions</li>
        </ul>
      </section>

      <section>
        <h2 id="benefits">Benefits of NavNode Tree</h2>
        <ul>
          <li><strong>Immutability:</strong> No accidental state mutations, predictable behavior</li>
          <li><strong>Testability:</strong> Pure functions are easy to unit test</li>
          <li><strong>Serialization:</strong> Full tree can be serialized for state restoration</li>
          <li><strong>Observability:</strong> StateFlow provides reactive updates</li>
          <li><strong>Flexibility:</strong> Support for stacks, tabs, panes, and custom structures</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/demo">See the demo</a> for multi-step process examples</li>
          <li><a href="/features/tabbed-navigation">Tabbed Navigation</a> - TabNode patterns</li>
          <li><a href="/getting-started">Get started</a> with navigation basics</li>
        </ul>
      </section>
    </article>
  )
}
