import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

const treeTraversalCode = `// Find node by key
val node = rootNode.findByKey("screen-123")

// Get active path from root to leaf
val path: List<NavNode> = rootNode.activePathToLeaf()

// Get deepest active screen
val activeScreen: ScreenNode? = rootNode.activeLeaf()

// Get active stack (for push/pop operations)
val stack: StackNode? = rootNode.activeStack()

// Get all screens in subtree
val screens: List<ScreenNode> = rootNode.allScreens()

// Check if node can handle back internally
val canHandleBack: Boolean = rootNode.canHandleBackInternally()`

const navigatorBackCode = `// Simple back navigation
navigator.navigateBack()

// Navigate and clear to specific point
navigator.navigateAndClearTo(
    destination = HomeDestination.Feed,
    clearRoute = "auth/login",
    inclusive = true
)

// Replace current screen
navigator.navigateAndReplace(
    destination = ProfileDestination.Edit,
    transition = NavigationTransitions.Fade
)

// Clear entire backstack
navigator.navigateAndClearAll(AuthDestination.Login)`

const treeMutatorCode = `// Push onto active stack
val newTree = TreeMutator.push(currentTree, destination)
navigator.updateState(newTree, transition)

// Pop from active stack
val newTree = TreeMutator.pop(currentTree, PopBehavior.CASCADE)

// Pop to specific destination type
val newTree = TreeMutator.popToDestination<HomeDestination>(currentTree, inclusive = false)

// Pop to route string
val newTree = TreeMutator.popToRoute(currentTree, "home/feed", inclusive = false)

// Push multiple destinations
val newTree = TreeMutator.pushAll(currentTree, listOf(
    OrderListDestination,
    OrderDetailDestination("123"),
    TrackingDestination("123")
))

// Clear and push
val newTree = TreeMutator.clearAndPush(currentTree, HomeDestination.Feed)

// Replace current
val newTree = TreeMutator.replaceCurrent(currentTree, DashboardDestination)`

const tabOperationsCode = `// Switch to specific tab
val newTree = TreeMutator.switchTab(currentTree, tabNodeKey, newIndex)

// Switch active TabNode's tab
val newTree = TreeMutator.switchActiveTab(currentTree, 0)`

const paneOperationsCode = `// Navigate within specific pane
val newTree = TreeMutator.navigateToPane(
    root = currentTree,
    nodeKey = paneNode.key,
    role = PaneRole.Supporting,
    destination = ItemDetailDestination(itemId),
    switchFocus = true
)

// Pop from specific pane
val newTree = TreeMutator.popPane(currentTree, paneNode.key, PaneRole.Supporting)

// Switch pane focus
val newTree = TreeMutator.switchActivePane(currentTree, paneNode.key, PaneRole.Primary)`

const backWithTabsCode = `fun handleBack(): Boolean {
    val result = TreeMutator.popWithTabBehavior(
        root = navigator.state.value,
        isCompact = windowSizeClass.isCompact
    )
    
    return when (result) {
        is BackResult.Handled -> {
            navigator.updateState(result.newState, null)
            true
        }
        is BackResult.DelegateToSystem -> false
        is BackResult.CannotHandle -> false
    }
}`

const backResultCode = `sealed class BackResult {
    data class Handled(val newState: NavNode) : BackResult()
    data object DelegateToSystem : BackResult()
    data object CannotHandle : BackResult()
}`

const checkBackCode = `// Simple check
val canGoBack = navigator.canNavigateBack.value

// Or via TreeMutator
val canHandle = TreeMutator.canHandleBackNavigation(navigator.state.value)`

export default function BackStack() {
  return (
    <article className={styles.features}>
      <h1>Back Stack Management</h1>
      <p className={styles.intro}>
        Navigation state in Quo Vadis is an immutable tree of NavNode objects. 
        TreeMutator provides pure functions for tree manipulation, while Navigator 
        handles high-level operations. This architecture ensures predictable, 
        testable, and serializable navigation behavior.
      </p>

      <section>
        <h2 id="navnode-tree">NavNode Tree Structure</h2>
        <p>
          The navigation state is represented as an immutable tree of NavNode objects. 
          Different node types support different navigation patterns:
        </p>
        <pre className={styles.treeStructure}>
{`StackNode (root)
└── TabNode (MainTabs)
    ├── StackNode (HomeTab)
    │   ├── ScreenNode (Home)
    │   └── ScreenNode (Detail)
    └── StackNode (ProfileTab)
        └── ScreenNode (Profile)`}
        </pre>
      </section>

      <section>
        <h2 id="navnode-types">NavNode Types</h2>
        <p>
          Each node type serves a specific navigation pattern:
        </p>
        <table>
          <thead>
            <tr>
              <th>Type</th>
              <th>Purpose</th>
              <th>Contains</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>ScreenNode</code></td>
              <td>Leaf destination</td>
              <td>Destination data</td>
            </tr>
            <tr>
              <td><code>StackNode</code></td>
              <td>Linear navigation</td>
              <td>List of children (last = active)</td>
            </tr>
            <tr>
              <td><code>TabNode</code></td>
              <td>Parallel tabs</td>
              <td>List of StackNodes</td>
            </tr>
            <tr>
              <td><code>PaneNode</code></td>
              <td>Adaptive panes</td>
              <td>Map of PaneRole to PaneConfiguration</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="tree-traversal">Tree Traversal Extensions</h2>
        <p>
          NavNode provides extension functions for traversing and querying the tree:
        </p>
        <CodeBlock code={treeTraversalCode} language="kotlin" />
      </section>

      <section>
        <h2 id="navigator-back">Navigator Back Operations</h2>
        <p>
          Navigator provides high-level methods for common back navigation scenarios:
        </p>
        <CodeBlock code={navigatorBackCode} language="kotlin" />
      </section>

      <section>
        <h2 id="tree-mutator">TreeMutator Operations</h2>
        <p>
          For advanced scenarios, use TreeMutator's pure functions for tree manipulation. 
          All operations return new immutable trees without modifying the original:
        </p>
        <CodeBlock code={treeMutatorCode} language="kotlin" />
      </section>

      <section>
        <h2 id="pop-behavior">PopBehavior Options</h2>
        <p>
          Control how empty stacks are handled after a pop operation:
        </p>
        <table>
          <thead>
            <tr>
              <th>Behavior</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>PRESERVE_EMPTY</code></td>
              <td>Keep empty stacks in place</td>
            </tr>
            <tr>
              <td><code>CASCADE</code></td>
              <td>Remove empty stacks, cascading up the tree</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="tab-operations">Tab Operations</h2>
        <p>
          TreeMutator provides operations for tab-based navigation:
        </p>
        <CodeBlock code={tabOperationsCode} language="kotlin" />
      </section>

      <section>
        <h2 id="pane-operations">Pane Operations</h2>
        <p>
          For adaptive multi-pane layouts, use pane-specific operations:
        </p>
        <CodeBlock code={paneOperationsCode} language="kotlin" />
      </section>

      <section>
        <h2 id="back-with-tabs">Back Navigation with Tab Awareness</h2>
        <p>
          Handle back navigation that properly accounts for tab state:
        </p>
        <CodeBlock code={backWithTabsCode} language="kotlin" />
      </section>

      <section>
        <h2 id="back-result">BackResult Types</h2>
        <p>
          The <code>popWithTabBehavior</code> function returns a sealed class indicating the result:
        </p>
        <CodeBlock code={backResultCode} language="kotlin" />
        <ul>
          <li><strong>Handled:</strong> Back was processed, apply the new state</li>
          <li><strong>DelegateToSystem:</strong> Let the platform handle back (e.g., exit app)</li>
          <li><strong>CannotHandle:</strong> Navigation cannot process this back gesture</li>
        </ul>
      </section>

      <section>
        <h2 id="check-back">Checking Back Availability</h2>
        <p>
          Check whether back navigation is possible before showing UI elements:
        </p>
        <CodeBlock code={checkBackCode} language="kotlin" />
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <ul>
          <li><strong>Use Navigator for standard operations:</strong> It handles common cases and provides type safety</li>
          <li><strong>Use TreeMutator for batch operations:</strong> When you need multiple mutations or custom logic</li>
          <li><strong>The original tree is never modified:</strong> All operations are immutable</li>
          <li><strong>Unchanged subtrees are reused:</strong> Structural sharing ensures efficiency</li>
          <li><strong>All TreeMutator operations are thread-safe:</strong> Pure functions can be called from any context</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/tabbed-navigation">Tabbed Navigation</a> - Deep dive into TabNode patterns</li>
          <li><a href="/features/multi-pane">Multi-Pane Layouts</a> - Adaptive pane navigation</li>
          <li><a href="/features/transitions">Transitions</a> - Animate navigation changes</li>
          <li><a href="/demo">See the demo</a> - Interactive examples</li>
        </ul>
      </section>
    </article>
  )
}
