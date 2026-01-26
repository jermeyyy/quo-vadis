import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { ScopePropertiesTable } from '@components/ScopePropertiesTable/ScopePropertiesTable'
import styles from '../Features.module.css'

const paneNodeStructureCode = `@Serializable
@SerialName("pane")
data class PaneNode(
    override val key: String,
    override val parentKey: String?,
    val paneConfigurations: Map<PaneRole, PaneConfiguration>,
    val activePaneRole: PaneRole = PaneRole.Primary,
    val backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
    val scopeKey: String? = null
) : NavNode {
    val activePaneContent: NavNode?   // Content of focused pane
    val paneCount: Int                // Number of configured panes
    val configuredRoles: Set<PaneRole> // All configured roles
}`

const annotationExampleCode = `@Pane(name = "messagesPane", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class MessagesPane : NavDestination {

    @PaneItem(role = PaneRole.PRIMARY)
    @Destination(route = "messages/conversations")
    data object ConversationList : MessagesPane()

    @PaneItem(role = PaneRole.SECONDARY)
    @Destination(route = "messages/conversation/{conversationId}")
    data class ConversationDetail(
        @Argument val conversationId: String
    ) : MessagesPane()
}`

const dslExampleCode = `navigationConfig {
    panes<ListDetailPanes>("list-detail") {
        initialPane = PaneRole.Primary
        backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
        
        primary(weight = 0.4f, minWidth = 300.dp) {
            root(ListScreen)
            alwaysVisible()
        }
        
        secondary(weight = 0.6f, minWidth = 400.dp) {
            root(DetailPlaceholder)
        }
    }
}`

const containerExampleCode = `@PaneContainer(MessagesPane::class)
@Composable
fun MessagesPaneContainer(
    scope: PaneContainerScope,
    content: @Composable () -> Unit
) {
    if (scope.isExpanded) {
        // Expanded: Two-column layout
        Row(modifier = Modifier.fillMaxSize()) {
            scope.paneContents.filter { it.isVisible }.forEach { pane ->
                val weight = when (pane.role) {
                    PaneRole.Primary -> 0.4f
                    PaneRole.Supporting -> 0.6f
                    else -> 0.25f
                }
                Box(modifier = Modifier.weight(weight).fillMaxHeight()) {
                    if (pane.hasContent) pane.content()
                    else EmptyPlaceholder()
                }
            }
        }
    } else {
        // Compact: Single pane
        content()
    }
}`

const navigationExampleCode = `// Navigate to detail pane
navigator.navigate(MessagesPane.ConversationDetail("conv-123"))

// The library automatically:
// - On expanded screens: Shows detail in Supporting pane
// - On compact screens: Pushes detail, hiding Primary pane

// Check pane state via PaneContainerScope
if (scope.isExpanded) {
    // Multi-pane mode - both panes visible
} else {
    // Compact mode - only active pane visible
}`

const treeStructureCode = `// PaneNode tree structure
StackNode (root)
└── PaneNode (MessagesPane)
    ├── Primary: StackNode
    │   └── ScreenNode (ConversationList)
    └── Supporting: StackNode
        └── ScreenNode (ConversationDetail)`

const masterDetailExampleCode = `@Pane(name = "emailPane", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class EmailPane : NavDestination {

    @PaneItem(role = PaneRole.PRIMARY)
    @Destination(route = "email/inbox")
    data object Inbox : EmailPane()

    @PaneItem(role = PaneRole.SECONDARY)
    @Destination(route = "email/message/{messageId}")
    data class MessageView(
        @Argument val messageId: String
    ) : EmailPane()

    @PaneItem(role = PaneRole.EXTRA)
    @Destination(route = "email/compose")
    data object Compose : EmailPane()
}`

const dslWrapperExampleCode = `navigationConfig {
    paneContainer("list-detail") { content ->
        if (isExpanded) {
            Row(Modifier.fillMaxSize()) {
                paneContents.filter { it.isVisible }.forEach { pane ->
                    val weight = when (pane.role) {
                        PaneRole.Primary -> 0.35f
                        PaneRole.Supporting -> 0.65f
                        PaneRole.Extra -> 0.25f
                    }
                    Box(Modifier.weight(weight).fillMaxHeight()) {
                        pane.content()
                    }
                }
            }
        } else {
            content()  // Single pane mode
        }
    }
}`

export default function PaneLayouts() {
  return (
    <article className={styles.features}>
      <h1>Pane Layouts</h1>
      <p className={styles.intro}>
        Adaptive multi-pane layouts that automatically adjust to different screen sizes,
        showing multiple panes side-by-side on large screens and collapsing to single-pane
        navigation on compact screens.
      </p>

      <section>
        <h2 id="overview">Overview</h2>
        <p>
          <code>PaneNode</code> represents adaptive layouts where multiple panes can be displayed 
          simultaneously on large screens (tablets, desktops, foldables) or collapsed 
          to single-pane navigation on compact screens (phones).
        </p>
        <h3>When to Use Panes</h3>
        <ul>
          <li><strong>Master-detail patterns</strong> - Lists with detail views side-by-side</li>
          <li><strong>Supporting panels</strong> - Main content with contextual information</li>
          <li><strong>Foldable/tablet layouts</strong> - Take advantage of larger screens</li>
          <li><strong>Email/messaging apps</strong> - Conversation list + message view</li>
          <li><strong>Three-column layouts</strong> - Navigation + content + detail (Extra role)</li>
        </ul>
        <h3>Key Features</h3>
        <ul>
          <li><strong>Automatic Adaptation</strong> - Responds to screen size changes</li>
          <li><strong>Independent Stacks</strong> - Each pane has its own navigation history</li>
          <li><strong>Configurable Back Behavior</strong> - Control how back navigation works</li>
          <li><strong>Custom Layouts</strong> - Full control over pane arrangement</li>
          <li><strong>Shared State</strong> - State can be shared across panes</li>
        </ul>
      </section>

      <section>
        <h2 id="pane-node-structure">PaneNode Structure</h2>
        <p>
          At runtime, pane navigation is represented by a <code>PaneNode</code> in the navigation tree. 
          This node holds all pane configurations and tracks which pane is currently active.
        </p>
        <CodeBlock code={paneNodeStructureCode} language="kotlin" />
        <h3>Key Properties</h3>
        <table>
          <thead>
            <tr>
              <th>Property</th>
              <th>Type</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>paneConfigurations</code></td>
              <td><code>Map&lt;PaneRole, PaneConfiguration&gt;</code></td>
              <td>Pane role to configuration mapping</td>
            </tr>
            <tr>
              <td><code>activePaneRole</code></td>
              <td><code>PaneRole</code></td>
              <td>Pane with navigation focus</td>
            </tr>
            <tr>
              <td><code>backBehavior</code></td>
              <td><code>PaneBackBehavior</code></td>
              <td>Back navigation strategy</td>
            </tr>
            <tr>
              <td><code>scopeKey</code></td>
              <td><code>String?</code></td>
              <td>Identifier for scope-aware navigation</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="pane-roles">Pane Roles</h2>
        <p>
          Each pane is assigned a role that determines its purpose and behavior within the layout.
        </p>
        <table>
          <thead>
            <tr>
              <th>Role</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>PaneRole.Primary</code></td>
              <td>Main content pane (required). Always present in the layout.</td>
            </tr>
            <tr>
              <td><code>PaneRole.Supporting</code></td>
              <td>Detail/secondary content. Shows alongside Primary on large screens.</td>
            </tr>
            <tr>
              <td><code>PaneRole.Extra</code></td>
              <td>Additional content (rare). For three-column layouts on very large screens.</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="adaptation-strategies">Adaptation Strategies</h2>
        <p>
          Each pane can have an <code>AdaptStrategy</code> that defines how it behaves when screen space is limited.
        </p>
        <table>
          <thead>
            <tr>
              <th>Strategy</th>
              <th>Behavior</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>AdaptStrategy.Hide</code></td>
              <td>Hide the pane completely when space is limited</td>
            </tr>
            <tr>
              <td><code>AdaptStrategy.Levitate</code></td>
              <td>Show as overlay (modal-like) over other panes</td>
            </tr>
            <tr>
              <td><code>AdaptStrategy.Reflow</code></td>
              <td>Stack vertically under another pane</td>
            </tr>
          </tbody>
        </table>

        <h3>Screen Size Behavior</h3>
        <table>
          <thead>
            <tr>
              <th>Screen Size</th>
              <th>Behavior</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><strong>Compact</strong> (phones)</td>
              <td>Only <code>activePaneRole</code> is visible</td>
            </tr>
            <tr>
              <td><strong>Medium</strong> (small tablets)</td>
              <td>Primary visible, others can levitate as overlays</td>
            </tr>
            <tr>
              <td><strong>Expanded</strong> (tablets, desktop)</td>
              <td>Multiple panes displayed side-by-side</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="back-behavior">Back Navigation Behavior</h2>
        <p>
          <code>PaneBackBehavior</code> controls how the back button/gesture behaves within pane layouts.
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
              <td><code>PopUntilScaffoldValueChange</code></td>
              <td>Pop until the visible pane layout changes (e.g., going from two-pane to one-pane view)</td>
            </tr>
            <tr>
              <td><code>PopUntilContentChange</code></td>
              <td>Pop until any visible pane's content changes (more granular back navigation)</td>
            </tr>
          </tbody>
        </table>
        <p>
          Choose <code>PopUntilContentChange</code> for messaging apps where you want each message 
          to be a back step. Use <code>PopUntilScaffoldValueChange</code> for master-detail layouts 
          where back should return to the list view.
        </p>
      </section>

      <section>
        <h2 id="annotation-definition">Annotation-Based Definition</h2>
        <p>
          Define your pane structure using <code>@Pane</code> on a sealed class and <code>@PaneItem</code> 
          on each pane destination. The KSP processor generates all configuration code automatically.
        </p>
        <CodeBlock code={annotationExampleCode} language="kotlin" />
        <h3>@Pane Properties</h3>
        <table>
          <thead>
            <tr>
              <th>Property</th>
              <th>Type</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>name</code></td>
              <td><code>String</code></td>
              <td>Unique identifier for the pane container</td>
            </tr>
            <tr>
              <td><code>backBehavior</code></td>
              <td><code>PaneBackBehavior</code></td>
              <td>Back navigation strategy</td>
            </tr>
          </tbody>
        </table>
        <h3>@PaneItem Properties</h3>
        <table>
          <thead>
            <tr>
              <th>Property</th>
              <th>Type</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><code>role</code></td>
              <td><code>PaneRole</code></td>
              <td>Which pane this destination belongs to (PRIMARY, SECONDARY, EXTRA)</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section>
        <h2 id="dsl-definition">DSL-Based Definition</h2>
        <p>
          For dynamic configurations, use the <code>panes&lt;D&gt;</code> builder in the DSL:
        </p>
        <CodeBlock code={dslExampleCode} language="kotlin" />
        <p>
          The DSL approach offers fine-grained control over pane dimensions, weights, 
          and visibility rules that may not be available through annotations.
        </p>
      </section>

      <section>
        <h2 id="pane-container">PaneContainer Implementation</h2>
        <p>
          Use <code>@PaneContainer</code> to create custom pane layout UI with full control 
          over how panes are arranged and displayed.
        </p>
        <CodeBlock code={containerExampleCode} language="kotlin" />
        
        <ScopePropertiesTable scopeType="pane" />

        <h3>DSL-Based Container Wrapper</h3>
        <p>
          You can also register pane containers via DSL:
        </p>
        <CodeBlock code={dslWrapperExampleCode} language="kotlin" />
      </section>

      <section>
        <h2 id="navigation">Navigating Between Panes</h2>
        <p>
          Navigate to pane destinations just like any other destination. The library automatically 
          handles showing content in the appropriate pane based on screen size.
        </p>
        <CodeBlock code={navigationExampleCode} language="kotlin" />
        <p>
          The navigation system automatically determines where to show content based on:
        </p>
        <ul>
          <li>The destination's <code>@PaneItem</code> role annotation</li>
          <li>Current screen size (compact vs expanded)</li>
          <li>The configured adaptation strategies</li>
        </ul>
      </section>

      <section>
        <h2 id="tree-structure">Tree Structure</h2>
        <p>
          In the <Link to="/features/core-concepts#navnode-tree">NavNode tree</Link>, a <code>PaneNode</code> contains separate <code>StackNode</code>s 
          for each pane role:
        </p>
        <CodeBlock code={treeStructureCode} language="text" />
        <p>
          Each pane's stack operates independently, maintaining its own navigation history. 
          This allows users to navigate within one pane without affecting the other.
        </p>
      </section>

      <section>
        <h2 id="patterns">Common Patterns</h2>
        
        <h3>Master-Detail (List + Detail)</h3>
        <p>
          The most common pane pattern. Show a list in the Primary pane and details 
          in the Supporting pane.
        </p>
        <CodeBlock code={masterDetailExampleCode} language="kotlin" />
        
        <h3>Supporting Panel</h3>
        <p>
          Main content with a contextual sidebar. The Supporting pane shows additional 
          information related to the Primary content without replacing it.
        </p>
        
        <h3>Three-Column Layout</h3>
        <p>
          For very large screens (desktop), use the <code>Extra</code> role to add a 
          third column. Common in email clients: folders | messages | message detail.
        </p>
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <ul>
          <li>
            <strong>Always provide empty state placeholders</strong> - When the Supporting pane 
            has no content, show a helpful placeholder instead of blank space.
          </li>
          <li>
            <strong>Choose appropriate back behavior</strong> - Use <code>PopUntilContentChange</code> 
            for fine-grained back navigation, <code>PopUntilScaffoldValueChange</code> for 
            layout-changing navigation.
          </li>
          <li>
            <strong>Test on multiple screen sizes</strong> - Ensure your pane layout works well 
            on phones (single pane), tablets (dual pane), and desktop (potentially three panes).
          </li>
          <li>
            <strong>Consider foldables</strong> - Foldable devices may switch between expanded and 
            compact modes during use. Test fold/unfold scenarios.
          </li>
          <li>
            <strong>Keep Primary pane always visible when expanded</strong> - The Primary pane 
            serves as an anchor. Use <code>alwaysVisible()</code> in DSL configuration.
          </li>
          <li>
            <strong>Handle orientation changes gracefully</strong> - The navigation state persists 
            across configuration changes; only the visual layout adapts.
          </li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/features/architecture">Architecture</Link> - Overall navigation architecture</li>
          <li><Link to="/features/tabbed-navigation">Tabbed Navigation</Link> - Tab-based navigation patterns</li>
          <li><Link to="/features/dsl-config">DSL Configuration</Link> - Manual configuration options</li>
          <li><Link to="/features/multiplatform">Multiplatform</Link> - Platform-specific considerations</li>
          <li><Link to="/demo">Live Demo</Link> - See pane layouts in action</li>
        </ul>
      </section>
    </article>
  )
}
