import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { NavNodeTypesTable } from '@components/NavNodeTypesTable'
import {
  navigatorInterfaceCode,
  treeNavigatorCode,
  treeMutatorCode,
  treeStructureExample,
  treeTraversalCode,
  navigatorBackCode,
  treeMutatorOperationsCode,
  tabOperationsCode,
  paneOperationsCode,
  backWithTabsCode,
  backResultCode,
  checkBackCode,
} from '@data/codeExamples'
import styles from '../Features.module.css'

// Architecture Diagram Styles
const diagramStyles = {
  container: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    gap: '0.5rem',
    padding: '2rem 1rem',
    fontFamily: 'var(--font-mono)',
    fontSize: '0.85rem',
  },
  layer: {
    width: '100%',
    maxWidth: '500px',
    borderRadius: '12px',
    padding: '1.25rem',
    border: '1px solid var(--color-border)',
  },
  layerTitle: {
    fontSize: '0.7rem',
    fontWeight: '600' as const,
    textTransform: 'uppercase' as const,
    letterSpacing: '0.1em',
    marginBottom: '1rem',
    opacity: 0.7,
  },
  box: {
    background: 'var(--color-bg-elevated)',
    borderRadius: '8px',
    padding: '0.75rem 1rem',
    textAlign: 'center' as const,
    border: '1px solid var(--color-border)',
    fontWeight: '500' as const,
  },
  renderingLayer: {
    background: 'rgba(59, 130, 246, 0.08)',
    borderColor: 'rgba(59, 130, 246, 0.3)',
  },
  logicLayer: {
    background: 'rgba(34, 197, 94, 0.08)',
    borderColor: 'rgba(34, 197, 94, 0.3)',
  },
  treeLayer: {
    background: 'rgba(139, 92, 246, 0.08)',
    borderColor: 'rgba(139, 92, 246, 0.3)',
  },
  renderersRow: {
    display: 'flex',
    gap: '0.5rem',
    justifyContent: 'center',
    flexWrap: 'wrap' as const,
  },
  smallBox: {
    fontSize: '0.75rem',
    padding: '0.5rem 0.75rem',
  },
  flowLabel: {
    fontSize: '0.7rem',
    color: 'var(--color-text-muted)',
    fontStyle: 'italic' as const,
  },
  treeContent: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '0.5rem',
    flexWrap: 'wrap' as const,
  },
  treeNode: {
    padding: '0.4rem 0.6rem',
    borderRadius: '4px',
    fontSize: '0.75rem',
    background: 'var(--color-bg-elevated)',
    border: '1px solid rgba(139, 92, 246, 0.4)',
  },
  treeArrow: {
    color: 'var(--color-text-muted)',
  },
}

const Arrow = () => (
  <div style={{
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '0.25rem 0',
  }}>
    <div style={{
      width: '2px',
      height: '20px',
      background: 'var(--color-text-muted, #9ca3af)',
    }} />
    <div style={{
      width: '0',
      height: '0',
      borderLeft: '6px solid transparent',
      borderRight: '6px solid transparent',
      borderTop: '8px solid var(--color-text-muted, #9ca3af)',
    }} />
  </div>
)

const ArchitectureDiagram = () => (
  <div style={diagramStyles.container}>
    {/* Application */}
    <div style={{ ...diagramStyles.box, maxWidth: '500px', width: '100%' }}>
      Application
    </div>
    
    <Arrow />
    
    {/* Rendering Layer */}
    <div style={{ ...diagramStyles.layer, ...diagramStyles.renderingLayer }}>
      <div style={diagramStyles.layerTitle}>Rendering Layer</div>
      
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.75rem' }}>
        <div style={diagramStyles.box}>NavigationHost</div>
        <Arrow />
        <div style={diagramStyles.box}>NavNodeRenderer</div>
        <Arrow />
        <div style={diagramStyles.renderersRow}>
          <div style={{ ...diagramStyles.box, ...diagramStyles.smallBox }}>ScreenRenderer</div>
          <div style={{ ...diagramStyles.box, ...diagramStyles.smallBox }}>StackRenderer</div>
          <div style={{ ...diagramStyles.box, ...diagramStyles.smallBox }}>TabRenderer</div>
          <div style={{ ...diagramStyles.box, ...diagramStyles.smallBox }}>PaneRenderer</div>
        </div>
      </div>
    </div>
    
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <Arrow />
      <span style={diagramStyles.flowLabel}>observes StateFlow&lt;NavNode&gt;</span>
    </div>
    
    {/* Logic Layer */}
    <div style={{ ...diagramStyles.layer, ...diagramStyles.logicLayer }}>
      <div style={diagramStyles.layerTitle}>Logic Layer</div>
      
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.75rem' }}>
        <div style={diagramStyles.box}>Navigator <span style={{ opacity: 0.6, fontSize: '0.75rem' }}>(interface)</span></div>
        <Arrow />
        <div style={diagramStyles.box}>TreeNavigator <span style={{ opacity: 0.6, fontSize: '0.75rem' }}>(impl)</span></div>
        <Arrow />
        <div style={diagramStyles.box}>TreeMutator <span style={{ opacity: 0.6, fontSize: '0.75rem' }}>(pure functions)</span></div>
      </div>
    </div>
    
    <Arrow />
    
    {/* NavNode Tree */}
    <div style={{ ...diagramStyles.layer, ...diagramStyles.treeLayer }}>
      <div style={diagramStyles.layerTitle}>NavNode Tree</div>
      
      <div style={diagramStyles.treeContent}>
        <span style={diagramStyles.treeNode}>StackNode</span>
        <span style={diagramStyles.treeArrow}>→</span>
        <span style={diagramStyles.treeNode}>TabNode</span>
        <span style={diagramStyles.treeArrow}>→</span>
        <span style={diagramStyles.treeNode}>ScreenNode</span>
      </div>
    </div>
  </div>
)

// Data Flow Diagram Styles
const dataFlowStyles = {
  container: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    gap: '0',
    padding: '2rem 1rem',
    fontFamily: 'var(--font-mono)',
    fontSize: '0.85rem',
  },
  box: {
    width: '100%',
    maxWidth: '320px',
    background: 'var(--color-bg-elevated)',
    borderRadius: '10px',
    padding: '1rem 1.25rem',
    textAlign: 'center' as const,
    border: '2px solid',
    position: 'relative' as const,
  },
  boxTitle: {
    fontWeight: '600' as const,
    fontSize: '0.95rem',
    marginBottom: '0.25rem',
  },
  boxNote: {
    fontSize: '0.75rem',
    opacity: 0.7,
    fontStyle: 'italic' as const,
  },
  userAction: {
    background: 'rgba(234, 179, 8, 0.12)',
    borderColor: 'rgba(234, 179, 8, 0.5)',
  },
  navigator: {
    background: 'rgba(34, 197, 94, 0.1)',
    borderColor: 'rgba(34, 197, 94, 0.4)',
  },
  navNodeTree: {
    background: 'rgba(139, 92, 246, 0.1)',
    borderColor: 'rgba(139, 92, 246, 0.4)',
  },
  navigationHost: {
    background: 'rgba(59, 130, 246, 0.1)',
    borderColor: 'rgba(59, 130, 246, 0.4)',
  },
  navNodeRenderer: {
    background: 'rgba(20, 184, 166, 0.1)',
    borderColor: 'rgba(20, 184, 166, 0.4)',
  },
  composeUI: {
    background: 'rgba(236, 72, 153, 0.1)',
    borderColor: 'rgba(236, 72, 153, 0.4)',
  },
}

const DataFlowArrow = () => (
  <div style={{
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '0.25rem 0',
  }}>
    <div style={{
      width: '2px',
      height: '28px',
      background: 'var(--color-text-muted, #9ca3af)',
    }} />
    <div style={{
      width: '0',
      height: '0',
      borderLeft: '6px solid transparent',
      borderRight: '6px solid transparent',
      borderTop: '8px solid var(--color-text-muted, #9ca3af)',
    }} />
  </div>
)

const DataFlowDiagram = () => (
  <div style={dataFlowStyles.container}>
    <div style={{ ...dataFlowStyles.box, ...dataFlowStyles.userAction }}>
      <div style={dataFlowStyles.boxTitle}>User Action</div>
      <div style={dataFlowStyles.boxNote}>tap, gesture, intent</div>
    </div>
    
    <DataFlowArrow />
    
    <div style={{ ...dataFlowStyles.box, ...dataFlowStyles.navigator }}>
      <div style={dataFlowStyles.boxTitle}>Navigator</div>
      <div style={dataFlowStyles.boxNote}>navigate(), navigateBack()</div>
    </div>
    
    <DataFlowArrow />
    
    <div style={{ ...dataFlowStyles.box, ...dataFlowStyles.navNodeTree }}>
      <div style={dataFlowStyles.boxTitle}>NavNode Tree</div>
      <div style={dataFlowStyles.boxNote}>Immutable state</div>
    </div>
    
    <DataFlowArrow />
    
    <div style={{ ...dataFlowStyles.box, ...dataFlowStyles.navigationHost }}>
      <div style={dataFlowStyles.boxTitle}>NavigationHost</div>
      <div style={dataFlowStyles.boxNote}>collectAsState()</div>
    </div>
    
    <DataFlowArrow />
    
    <div style={{ ...dataFlowStyles.box, ...dataFlowStyles.navNodeRenderer }}>
      <div style={dataFlowStyles.boxTitle}>NavNodeRenderer</div>
      <div style={dataFlowStyles.boxNote}>AnimatedNavContent</div>
    </div>
    
    <DataFlowArrow />
    
    <div style={{ ...dataFlowStyles.box, ...dataFlowStyles.composeUI }}>
      <div style={dataFlowStyles.boxTitle}>Compose UI</div>
      <div style={dataFlowStyles.boxNote}>Screen content</div>
    </div>
  </div>
)

export default function CoreConcepts() {
  return (
    <article className={styles.features}>
      <h1>Core Concepts</h1>
      <p className={styles.intro}>
        Quo Vadis uses a tree-based navigation architecture where navigation state 
        is an immutable tree of NavNode objects. This design provides clean separation 
        between logic and rendering, enabling predictable, testable, and serializable 
        navigation behavior.
      </p>

      <section>
        <h2 id="overview">Overview</h2>
        <p>
          The architecture is divided into two main layers that interact through a 
          unidirectional data flow pattern:
        </p>
        <ul>
          <li><strong>Logic Layer</strong> manages navigation state as an immutable tree of <code>NavNode</code> objects</li>
          <li><strong>Rendering Layer</strong> recursively renders the tree to Compose UI with animations</li>
        </ul>
        <ArchitectureDiagram />
      </section>

      <section>
        <h2 id="navnode-tree">NavNode Tree Structure</h2>
        <p>
          The navigation state is represented as an immutable tree of NavNode objects. 
          Different node types support different navigation patterns:
        </p>
        <pre className={styles.treeStructure}>
{treeStructureExample}
        </pre>

        <h3>NavNode Types</h3>
        <NavNodeTypesTable />

        <div className={styles.note}>
          <p>
            The NavNode tree represents <strong>logical navigation state</strong>, not visual layout. 
            The renderer determines which nodes are visible and how they're arranged based on 
            window size and adaptation strategies.
          </p>
        </div>
      </section>

      <section>
        <h2 id="logic-layer">Logic Layer</h2>
        <p>
          The logic layer is responsible for managing navigation state as an immutable tree structure.
          It consists of three key components:
        </p>

        <h3 id="navigator-interface">Navigator Interface</h3>
        <p>
          <code>Navigator</code> is the central navigation controller interface that defines the 
          contract for all navigation operations:
        </p>
        <CodeBlock code={navigatorInterfaceCode} language="kotlin" />
        
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
              <td><code>state</code></td>
              <td><code>StateFlow&lt;NavNode&gt;</code></td>
              <td>Current navigation state as immutable tree</td>
            </tr>
            <tr>
              <td><code>transitionState</code></td>
              <td><code>StateFlow&lt;TransitionState&gt;</code></td>
              <td>Animation/transition state</td>
            </tr>
            <tr>
              <td><code>currentDestination</code></td>
              <td><code>StateFlow&lt;NavDestination?&gt;</code></td>
              <td>Active destination (derived)</td>
            </tr>
            <tr>
              <td><code>canNavigateBack</code></td>
              <td><code>StateFlow&lt;Boolean&gt;</code></td>
              <td>Whether back navigation is possible</td>
            </tr>
          </tbody>
        </table>

        <h3 id="tree-navigator">TreeNavigator Implementation</h3>
        <p>
          <code>TreeNavigator</code> is the concrete implementation of Navigator using a tree-based state model:
        </p>
        <CodeBlock code={treeNavigatorCode} language="kotlin" />
        <p>Key features of TreeNavigator:</p>
        <ul>
          <li><strong>Immutable State Management:</strong> Uses <code>MutableStateFlow&lt;NavNode&gt;</code> internally, exposes as immutable <code>StateFlow</code></li>
          <li><strong>Container-Aware Navigation:</strong> Automatically creates Tab/Pane containers when navigating to containerized destinations</li>
          <li><strong>Scope-Aware Navigation:</strong> Out-of-scope destinations push to parent stack</li>
          <li><strong>Lifecycle Event Dispatch:</strong> Dispatches <code>onScreenExited</code> and <code>onScreenDestroyed</code> events</li>
        </ul>

        <h3 id="tree-mutator">TreeMutator</h3>
        <p>
          <code>TreeMutator</code> is a singleton object containing pure functions for tree manipulation.
          All operations take a tree and return a new tree without modifying the original:
        </p>
        <CodeBlock code={treeMutatorCode} language="kotlin" />
        <p>Design principles:</p>
        <ul>
          <li><strong>Pure Functions:</strong> All operations take a tree and return a new tree</li>
          <li><strong>Immutability:</strong> Original tree is never modified</li>
          <li><strong>Structural Sharing:</strong> Unchanged subtrees are reused for efficiency</li>
        </ul>
      </section>

      <section>
        <h2 id="stack-operations">Stack Operations</h2>

        <h3 id="tree-traversal">Tree Traversal Extensions</h3>
        <p>
          NavNode provides extension functions for traversing and querying the tree:
        </p>
        <CodeBlock code={treeTraversalCode} language="kotlin" />

        <h3 id="navigator-back">Navigator Back Operations</h3>
        <p>
          Navigator provides high-level methods for common back navigation scenarios:
        </p>
        <CodeBlock code={navigatorBackCode} language="kotlin" />

        <h3 id="mutator-operations">TreeMutator Operations</h3>
        <p>
          For advanced scenarios, use TreeMutator's pure functions for tree manipulation. 
          All operations return new immutable trees without modifying the original:
        </p>
        <CodeBlock code={treeMutatorOperationsCode} language="kotlin" />

        <h3 id="pop-behavior">PopBehavior Options</h3>
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
        <p>
          For comprehensive tab navigation patterns including tab containers, nested stacks, 
          and custom tab UI, see <Link to="/features/tabbed-navigation">Tabbed Navigation</Link>.
        </p>
      </section>

      <section>
        <h2 id="pane-operations">Pane Operations</h2>
        <p>
          For adaptive multi-pane layouts, use pane-specific operations:
        </p>
        <CodeBlock code={paneOperationsCode} language="kotlin" />
        <p>
          For comprehensive pane navigation patterns including adaptive layouts and 
          pane containers, see <Link to="/features/pane-layouts">Pane Layouts</Link>.
        </p>
      </section>

      <section>
        <h2 id="back-with-tabs">Back Navigation with Tab Awareness</h2>
        <p>
          Handle back navigation that properly accounts for tab state:
        </p>
        <CodeBlock code={backWithTabsCode} language="kotlin" />

        <h3>BackResult Types</h3>
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
        <h2 id="rendering-layer">Rendering Layer</h2>
        <p>
          The rendering layer converts the navigation tree to Compose UI with animations 
          and gesture support.
        </p>

        <h3>NavigationHost</h3>
        <p>
          <code>NavigationHost</code> is the main entry point for rendering navigation content. 
          It sets up the infrastructure for navigation:
        </p>
        <ul>
          <li><strong>State Collection:</strong> Collects <code>navigator.state</code> as Compose state</li>
          <li><strong>Infrastructure Setup:</strong> Creates <code>SaveableStateHolder</code>, <code>ComposableCache</code>, <code>AnimationCoordinator</code></li>
          <li><strong>Predictive Back:</strong> Wraps content in <code>NavigateBackHandler</code> for gesture support</li>
          <li><strong>Shared Elements:</strong> Wraps content in <code>SharedTransitionLayout</code> for shared element transitions</li>
        </ul>

        <h3>NavNodeRenderer</h3>
        <p>
          <code>NavNodeRenderer</code> is the core recursive renderer that dispatches to specialized 
          renderers based on node type:
        </p>
        <ul>
          <li><strong>ScreenRenderer:</strong> Renders leaf <code>ScreenNode</code> content via the screen registry</li>
          <li><strong>StackRenderer:</strong> Renders <code>StackNode</code> with animated push/pop transitions</li>
          <li><strong>TabRenderer:</strong> Renders <code>TabNode</code> with tab wrapper and switching animations</li>
          <li><strong>PaneRenderer:</strong> Renders <code>PaneNode</code> with adaptive multi-pane layouts</li>
        </ul>
      </section>

      <section>
        <h2 id="data-flow">Data Flow</h2>
        <p>
          Navigation follows a unidirectional data flow pattern:
        </p>
        <DataFlowDiagram />
        
        <h3>Key Principles</h3>
        <ul>
          <li><strong>Unidirectional Data Flow:</strong> State flows down, events flow up</li>
          <li><strong>Single Source of Truth:</strong> <code>Navigator.state</code> is the only source of navigation state</li>
          <li><strong>Immutable Updates:</strong> Every navigation operation creates a new tree</li>
          <li><strong>Separation of Concerns:</strong> Logic layer doesn't know about UI, UI observes state</li>
        </ul>
      </section>

      <section>
        <h2 id="best-practices">Best Practices</h2>
        <ul>
          <li><strong>Use Navigator for standard operations:</strong> It handles common cases and provides type safety</li>
          <li><strong>Use TreeMutator for batch operations:</strong> When you need multiple mutations or custom logic</li>
          <li><strong>The original tree is never modified:</strong> All operations are immutable</li>
          <li><strong>Unchanged subtrees are reused:</strong> Structural sharing ensures efficiency</li>
          <li><strong>All TreeMutator operations are thread-safe:</strong> Pure functions can be called from any context</li>
          <li><strong>Clean state management:</strong> All navigation logic is isolated in the logic layer</li>
          <li><strong>Flexible rendering:</strong> UI adapts to different screen sizes and platforms</li>
          <li><strong>Testability:</strong> Logic can be unit tested without UI dependencies</li>
        </ul>
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/features/stack-management">Stack Management</Link> - Linear navigation stacks and back stack operations</li>
          <li><Link to="/features/tabbed-navigation">Tabbed Navigation</Link> - Parallel tab stacks with independent history</li>
          <li><Link to="/features/pane-layouts">Pane Layouts</Link> - Adaptive multi-pane navigation</li>
          <li><Link to="/features/transitions">Transitions & Animations</Link> - Customize navigation animations</li>
          <li><Link to="/features/predictive-back">Predictive Back</Link> - Gesture-driven back navigation</li>
        </ul>
      </section>
    </article>
  )
}
