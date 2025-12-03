# **Architectural Refactoring and Development Planning for the Quo Vadis Library**

## **Executive Summary**

The Kotlin Multiplatform (KMP) ecosystem has reached a pivotal level of maturity, demanding navigation solutions that transcend simple screen switching. Modern user interfaces require fluid, physics-based interactions—specifically predictive back gestures and shared element transitions—that traditional, nested navigation architectures struggle to support. The *Quo Vadis* library has evolved from a "Direct BackStack" manager into a holistic "Navigation Rendering Engine."

This document provides an exhaustive architectural analysis of the *Quo Vadis* library's "Single Rendering Component" architecture. In this model, the navigation state is decoupled from the UI hierarchy and represented as a reactive, immutable tree structure capable of modeling linear backstacks, tabbed environments, and adaptive pane layouts. A single rendering component, QuoVadisHost, consumes this state to project the UI, enabling seamless shared element transitions and coordinated animations that are mathematically impossible in fragmented, nested host architectures.

The document is structured into seven comprehensive chapters. It begins with an architectural audit of the library, proceeds to a theoretical framework for unified rendering, and culminates in a granular "Prompt File" designed for a development planning agent. This document serves as the authoritative blueprint for the library, ensuring that the core philosophy of type safety and modularity is preserved while the rendering capabilities are elevated to state-of-the-art standards.

## ---

**1\. Architectural Audit and Contextual Analysis**

To prescribe a refactoring plan of this magnitude, it is imperative to first dissect the existing anatomy of the *Quo Vadis* library. The current architecture, while robust for standard navigation, presents specific limitations when faced with the requirements of "Single Component Rendering."

### **1.1 The Current Module Ecosystem**

The library follows a strict modular design pattern, utilizing a "Gray box" approach to feature modules.1 This modularity is a significant asset and must be preserved during the refactor.

* **quo-vadis-core**: This module is the nucleus of the library, containing the runtime navigation primitives and the Navigator class. It is designed to be platform-agnostic, supporting Android, iOS, Desktop, and Web.1 Currently, it likely manages a linear backStack and exposes basic push/pop operations.  
* **quo-vadis-annotations**: A lightweight, reflection-free module containing the following annotations that map directly to NavNode types:

| Annotation | NavNode Type | Purpose |
|------------|--------------|---------|  
| `@Destination` | `ScreenNode` | Marks a class/object as a navigation target |
| `@Stack` | `StackNode` | Linear navigation container (push/pop) |
| `@Tab` | `TabNode` | Parallel stacks container (tabbed UI) |
| `@Pane` | `PaneNode` | Adaptive layout container (split views) |
| `@Screen` | N/A (Registry entry) | Binds a Composable to a destination |

This separation allows feature modules to define navigation contracts without depending on the heavy core logic.

* **quo-vadis-ksp**: The Kotlin Symbol Processing (KSP) artifact. This build-time dependency generates the glue code—specifically NavNode builders, screen registries, and deep link handlers—that eliminates the need for manual string routing. The reliance on KSP is a critical strength, allowing for complex compile-time validation of the navigation graph.  
* **composeApp**: The reference implementation, showcasing usage patterns like bottom navigation, drawers, and deep stacks.

The architecture currently emphasizes "Direct BackStack Access" 1, exposing the stack as a mutable or observable list. While this offers great power, a simple list structure is insufficient for modeling complex, nested states (like preserved history in multiple tabs) without resorting to multiple, nested Navigator instances.

### **1.2 The "Nested Host" Problem**

The prevailing pattern in Compose navigation (including the official Google library and likely the current *Quo Vadis* implementation) relies on nesting NavHost composables to achieve hierarchy. For example, a root NavHost might swap between a "Login" screen and a "Main" screen. The "Main" screen then contains a Scaffold with a BottomNavigation bar and a secondary, inner NavHost to handle tab switching.

#### **1.2.1 The Clipping Boundary**

The fundamental flaw in this architecture regarding **Shared Element Transitions** is the clipping boundary. In a nested system, the inner NavHost (displaying a list of items) is structurally distinct from the root NavHost (displaying the detail view). The layout system (Compose) sees them as separate sub-trees. When a user taps an item to navigate to the detail view (which lives in the root host to cover the bottom bar), the image element must effectively "teleport" from the inner scope to the outer scope. Without a shared layout root (specifically a SharedTransitionLayout that encompasses *both* destinations), the transition cannot be calculated continuously. The element will disappear from the list and reappear in the detail view, breaking the illusion of continuity.

#### **1.2.2 The Animation Synchronization Gap**

Similarly, **Predictive Back** gestures suffer in nested environments. When a user swipes back, the system must decide which Navigator handles the event. If the inner navigator pops a stack, it animates. If the outer navigator pops, the entire screen slides away. Synchronizing these—for instance, shrinking the entire "Main" screen while simultaneously cross-fading a tab—requires complex communication between two decoupled Navigator instances. A "Single Rendering Component" eliminates this by managing all animations in a single coordinate space.

### **1.3 Theoretical vs. Practical State**

Current navigation libraries often conflate "UI Hierarchy" with "Navigation State."

* **Current Model**: The structure of the navigation is defined by where NavHost composables are placed in the code. To change the structure (e.g., move a screen from a tab to a full-screen modal), one must refactor the UI code.  
* **Target Model**: The structure is defined purely in data (the State Tree). The UI is merely a projection. To move a screen, one simply moves the node in the tree; the renderer automatically adjusts the Z-index and layout placement without UI refactoring.

## ---

**2\. Theoretical Framework: The Omni-Render Architecture**

The request calls for a refactor where a single component renders "linear backstacks, tabbed navigators, and panes." This necessitates a move from a List-based state to a **Tree-based State**, coupled with a **Flattening Renderer**.

### **2.1 The Reactive State Tree**

The foundation of the new architecture is the NavNode hierarchy. Unlike a linear list, a tree can accurately model the application's logical state independent of the active UI.

#### **2.1.1 Node Taxonomy**

| Node Type | Description | Navigation Behavior | Theoretical Equivalent |
| :---- | :---- | :---- | :---- |
| **ScreenNode** | A leaf node representing a specific destination (e.g., Profile, Settings). | Terminal state. | Vertex |
| **StackNode** | A linear history of nodes. Maintains a list where index N covers index N-1. | Push appends to list. Pop removes tail. | Directed Path |
| **TabNode** | A collection of parallel StackNodes. Maintains an activeStackIndex. | SwitchTab changes index. Push affects active stack. | Disjoint Union |
| **PaneNode** | A collection of nodes displayed simultaneously (e.g., Split View). | All children are active. Push might replace a specific child. | Cartesian Product |

This recursive structure allows for infinite nesting (e.g., a Stack inside a Pane inside a Tab inside a Stack), covering all requested use cases.

#### **2.1.2 State Immutability**

The Navigator will hold a StateFlow\<NavNode\> representing the root. Operations are functional reducers:

$$S\_{new} \= f(S\_{old}, Action)$$

This immutability is crucial for the renderer. By comparing $S\_{old}$ and $S\_{new}$, the renderer can calculate the precise "diff"—which nodes were added, removed, or moved—and generate the corresponding transitions.

### **2.2 The Rendering Projection: $f(State) \\rightarrow UI$**

The "Single Component" requirement implies a specific rendering pipeline. The QuoVadisHost does not simply recurse. If it recursed (i.e., TabNode composable calling StackNode composable), we would re-introduce the nesting problem.

Instead, the renderer performs a **Flattening Operation**.

1. **Traversal**: The renderer traverses the active path of the NavNode tree.  
2. **Collection**: It collects all nodes that should be visible (e.g., the active child of a stack, all children of a pane).  
3. **Z-Ordering**: It assigns a Z-index based on depth. A modal pushed onto a root stack has a higher Z-index than a screen inside a tab.  
4. **Composition**: It emits a single Compose Layout (or Box) containing all these collected surfaces as direct children.

This flatness ensures that every active screen shares the same parent Layout. This is the "Magic Key" that unlocks Shared Element Transitions. Since Screen A (in a tab) and Screen B (full screen) are siblings in the Compose render tree, LookaheadScope can measure them both and interpolate their bounds seamlessly.

### **2.3 The Animation Registry**

In this model, animations are not properties of the screen, but properties of the *transition edges*.

* The library must maintain an AnimationRegistry that maps (FromClass, ToClass, Direction) to an AnimationSpec.  
* Standard animations (Slide, Fade, Scale) 1 are implemented as Modifier factories that function within the flattened layout.

## ---

**3\. Detailed Implementation Plan**

This section outlines the granular steps required to execute the refactor, serving as the basis for the agent prompt.

### **3.1 Phase 1: Core State Refactoring (quo-vadis-core)**

The Navigator class must be rewritten. The existing List\<Destination\> backstack must be replaced with the NavNode tree.

#### **3.1.1 Defining the Node Hierarchy**

The core data structures must be serializable to support process death survival (Android).

Kotlin

@Serializable  
sealed interface NavNode {  
    val key: String  
    val parentKey: String?  
}

@Serializable  
data class ScreenNode(  
    override val key: String,  
    override val parentKey: String?,  
    val destination: Destination  
) : NavNode

@Serializable  
data class StackNode(  
    override val key: String,  
    override val parentKey: String?,  
    val children: List\<NavNode\>  
) : NavNode {  
    val activeChild: NavNode? get() \= children.lastOrNull()  
}

#### **3.1.2 The Reducer Logic**

The Navigator will expose methods that mutate this tree.

* **push(Destination)**: Finds the deeply active StackNode and appends a new ScreenNode.  
* **pop()**: Removes the last node from the deeply active StackNode. If that stack becomes empty, it might trigger a pop on the parent stack (cascading back).  
* **popTo(Destination)**: Traversing up the tree to find the target and slicing the stack.

### **3.2 Phase 2: The Unified Renderer (QuoVadisHost)**

This is the most complex component. It bridges the logic (Tree) and the physics (UI).

#### **3.2.1 The "Omni-Layout"**

The QuoVadisHost will use androidx.compose.animation.SharedTransitionLayout (currently experimental but essential for this goal) as its root.

Inside, a SaveableStateHolder is required. Since we are flattening the tree, we lose the automatic state saving of nested navigators. We must manually wrap each screen's content:

Kotlin

saveableStateHolder.SaveableStateProvider(key \= node.key) {  
    // Render content  
}

#### **3.2.2 The Frame Manager**

To support predictive back and smooth transitions, the state cannot just "snap" from State A to State B. We need a TransitionState.

* **Idle(state)**: Showing the current state.  
* **Proposed(current, next, progress)**: Used during a predictive back gesture. The user is dragging, and we render *both* states interpolated by progress.  
* **Animating(current, next, progress)**: The user let go, and we are animating to the final state.

The Renderer listens to this TransitionState. If in Proposed or Animating mode, it renders **both** the entering and exiting nodes.

### **3.3 Phase 3: Integration with KSP (quo-vadis-ksp)**

The KSP processor generates the navigation infrastructure from annotations.

#### **Annotation Specifications**

```kotlin
// @Destination - Navigation Target
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Destination(
    val route: String = ""  // Deep link route, supports "{param}" placeholders
)

// @Stack - Linear Navigation Container
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Stack(
    val name: String,
    val startDestination: String = ""
)

// @Tab - Tabbed Navigation Container
@Target(AnnotationTarget.CLASS)
annotation class Tab(
    val name: String,
    val initialTab: String = ""
)

// @TabItem - Tab Metadata
@Target(AnnotationTarget.CLASS)
annotation class TabItem(
    val label: String,
    val icon: String,
    val rootGraph: KClass<*>
)

// @Pane - Adaptive Layout Container
@Target(AnnotationTarget.CLASS)
annotation class Pane(
    val name: String,
    val backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
)

// @PaneItem - Pane Metadata
@Target(AnnotationTarget.CLASS)
annotation class PaneItem(
    val role: PaneRole,
    val adaptStrategy: AdaptStrategy = AdaptStrategy.HIDE,
    val rootGraph: KClass<*>
)

// @Screen - Composable Content Binding
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Screen(
    val destination: KClass<*>
)
```

#### **Usage Examples**

```kotlin
// Stack-based navigation graph
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
    
    @Destination(route = "home/detail/{id}")
    data class Detail(val id: String) : HomeDestination()
}

// Tabbed navigation
@Tab(name = "mainTabs", initialTab = "Home")
sealed class MainTabs : Destination {
    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tabs/home")
    data object Home : MainTabs()
    
    @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    @Destination(route = "tabs/profile")
    data object Profile : MainTabs()
}

// Screen binding
@Screen(HomeDestination.Detail::class)
@Composable
fun DetailScreen(destination: HomeDestination.Detail, navigator: Navigator) {
    Text("Detail: ${destination.id}")
}
```

#### **KSP Module Structure**

```
quo-vadis-ksp/
├── QuoVadisSymbolProcessor.kt      # Main processor entry point
├── QuoVadisClassNames.kt           # Type-safe class references
├── extractors/                     # Annotation parsing
│   ├── DestinationExtractor.kt
│   ├── StackExtractor.kt
│   ├── TabExtractor.kt
│   ├── PaneExtractor.kt
│   └── ScreenExtractor.kt
├── generators/                     # Code generation
│   ├── NavNodeBuilderGenerator.kt
│   ├── ScreenRegistryGenerator.kt
│   ├── DeepLinkHandlerGenerator.kt
│   └── NavigatorExtGenerator.kt
└── models/                         # Intermediate representations
    ├── DestinationInfo.kt
    ├── StackInfo.kt
    ├── TabInfo.kt
    ├── PaneInfo.kt
    └── ScreenInfo.kt
```

#### **Generated Artifacts**

| Input | Generated Output | Purpose |
|-------|------------------|---------|  
| `@Stack` sealed class | `build{Name}NavNode()` | Creates initial StackNode tree |
| `@Tab` sealed class | `build{Name}NavNode()` | Creates initial TabNode tree |
| `@Pane` sealed class | `build{Name}NavNode()` | Creates initial PaneNode tree |
| All `@Destination` | `GeneratedDeepLinkHandler` | URI → Destination parsing |
| All `@Screen` | `GeneratedScreenRegistry` | Destination → Composable mapping |
| All containers | `NavigatorExtensions.kt` | Convenience navigation methods |

The processor generates a `build{Name}NavNode()` function for each container that returns the appropriate NavNode structure. This ensures that when the app launches, the entire navigation structure is ready.

## ---

**4\. Advanced Rendering Mechanics: Insights & Implications**

This section explores the specific requirements of Animations, Predictive Back, and Shared Elements, weaving in second-order insights about their implementation.

### **4.1 Predictive Back: The "Speculative Pop"**

Predictive back is not just an animation; it is a "Speculative Pop." The system must allow the user to peek at the previous state without destroying the current state.

* **Insight**: This requires the Navigator to support a "transactional" state. The back gesture creates a *fork* in the state history.  
* **Implementation**: The QuoVadisHost interacts with BackHandler (Android).  
  * onStarted: The renderer locates the nodeBelow in the stack. It creates a temporary RenderableSurface for it, placing it *behind* the current node.  
  * onProgress: The renderer applies a scaling transformation (e.g., 1.0 \-\> 0.9) to the current node and a parallax shift to the node below.  
  * onCancelled: The nodeBelow is removed from the render list. The current node scales back to 1.0.  
  * onCommitted: The actual Navigator.pop() is called, making the state change permanent.

This decoupling of "Gesture Physics" from "Logical State" is the only robust way to handle predictive back in a custom renderer.

### **4.2 Shared Element Transitions (SET)**

SET requires that the entering and exiting elements exist in the same Layout pass.

* **Insight**: Standard Compose Navigation fails here because the NavHost clips content. Our "Flattening" strategy places the Detail Screen and the List Screen in the same Box.  
* **Mechanism**:  
  * The QuoVadisHost provides a SharedTransitionScope to all @Content lambdas.  
  * Developers tag elements with Modifier.sharedElement(key).  
  * Because both screens are technically siblings in the render tree during the transition, Compose's layout engine can interpolate the size and position of the shared element seamlessly.

### **4.3 Panes and Adaptive Layouts**

The user specifically requested "Panes." This implies support for large screens (foldables, tablets, desktop).

* **Insight**: Navigation state usually assumes one active screen. Panes break this assumption.  
* **Implementation**: The PaneNode allows multiple children to be "Active" simultaneously.  
  * **The "Responsive Transformer"**: We can introduce a middleware that observes WindowSizeClass.  
  * If the window is Compact: The middleware structures the state as a StackNode (List \-\> Detail).  
  * If the window is Expanded: The middleware restructures the state as a PaneNode (List | Detail).  
  * Crucially, the *Destination* objects (ListDest, DetailDest) remain unchanged. The *Graph Structure* morphs. This is a powerful "second-order" capability of the Tree-based architecture.

## ---

**5\. Migration Strategy**

Refactoring to a "Single Renderer" is a breaking change. A clear migration path is vital for adoption.

| Component | Current State | Proposed State | Migration Action |
| :---- | :---- | :---- | :---- |
| **Navigator Access** | navigator.backStack (List) | navigator.state (Tree) | Provide extension properties like navigator.activeStack to mimic the old list API for backward compatibility. |
| **Graph Definition** | @Graph on Sealed Class | `@Stack`, `@Tab`, `@Pane` containers with `@Destination` members | Update annotations. Each container type has its own annotation with specific parameters. |
| **Content Binding** | @Content on functions | `@Screen(destination = ...)` | The @Screen annotation binds a Composable to a specific destination class. |
| **Hosting** | GraphNavHost(navigator) | QuoVadisHost(navigator, screenRegistry) | The API signature includes a screen registry for destination → Composable mapping. |
| **Transitions** | Per-screen Enter/ExitTransition | AnimationRegistry | Deprecate per-screen transitions in favor of the centralized registry. |

## ---

**6\. The Development Agent Prompt File**

The following section contains the literal content for the prompt\_file.md. This file is designed to be ingested by an LLM-based coding agent to generate the necessary boilerplate and logic.

### ---

**File: refactor\_plan\_prompt.md**

# **Development Task: Quo Vadis Architecture Refactor**

## **1\. Project Overview & Goal**

Context: The "Quo Vadis" KMP library has been refactored to use a Single Rendering Component architecture.  
Objective: The library uses QuoVadisHost that projects a Tree-Based Navigation State (NavNode).  
Key Features:

* Unified rendering of Stacks, Tabs, and Panes.  
* System-integrated Predictive Back (Android/iOS).  
* Shared Element Transitions (using Compose SharedTransitionLayout).

## **2\. Navigation API**

The navigation system uses a service locator pattern with type-safe destination-based navigation:

```kotlin
// Navigate by destination instance (type-safe)
navigator.navigate(HomeDestination.Detail("123"))

// Navigate with transition
navigator.navigate(
    destination = HomeDestination.Detail("123"),
    transition = NavigationTransition.SlideHorizontal
)

// Go back
navigator.navigateBack()

// Switch tabs
navigator.switchTab(MainTabs.Profile)

// App entry point
@Composable
fun App() {
    val navTree = remember { buildMainTabsNavNode() }  // KSP-generated
    val navigator = rememberNavigator(navTree)
    
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry  // KSP-generated
    )
}
```

## **3\. Core Components**

### **Component A: The State Tree (quo-vadis-core)**

The navigation state is represented as a NavNode tree.

1. **Define the Node Hierarchy**:  
   Kotlin  
   @Serializable  
   sealed interface NavNode {  
       val id: String  
       val parentId: String?  
   }

   @Serializable  
   data class ScreenNode(  
       override val id: String,  
       override val parentId: String?,  
       val destination: Destination // Existing interface  
   ) : NavNode

   @Serializable  
   data class StackNode(  
       override val id: String,  
       override val parentId: String?,  
       val children: List\<NavNode\>  
   ) : NavNode

   @Serializable  
   data class TabNode(  
       override val id: String,  
       override val parentId: String?,  
       val stacks: List\<StackNode\>,  
       val activeStackIndex: Int  
   ) : NavNode

   @Serializable  
   data class PaneNode(  
       override val id: String,  
       override val parentId: String?,  
       val panes: List\<NavNode\>  
   ) : NavNode

2. **Tree Operations**:  
   * **Push**: fun push(root: NavNode, dest: Destination): NavNode. Logic: Traverse depth-first to the active leaf stack and append a new ScreenNode.  
   * **Pop**: fun pop(root: NavNode): NavNode?. Logic: Remove the last node of the active stack. If empty, remove the stack (if allowed) or return null (signal app exit).

### **Component B: The Unified Renderer (QuoVadisHost)**

A single Composable function that flattens and renders the tree.

1. **Structure**:  
   Kotlin  
   @Composable  
   fun QuoVadisHost(
       navigator: Navigator,
       screenRegistry: ScreenRegistry
   ) {  
       val state by navigator.state.collectAsState()

       // Root Layout for Shared Transitions  
       SharedTransitionLayout {   
           val visibleSurfaces \= remember(state) { flattenState(state) }

           // Render Loop  
           visibleSurfaces.forEach { surface \-\>  
               key(surface.id) {  
                   // Use SaveableStateHolder to preserve state of swapped tabs  
                   holder.SaveableStateProvider(surface.id) {  
                        Box(Modifier.zIndex(surface.zOrder)) {  
                            // Apply Animation Modifiers here  
                            screenRegistry.render(surface.destination)
                        }  
                   }  
               }  
           }  
       }  
   }

2. **Flattening Algorithm**:  
   * Implement flattenState(node: NavNode): List\<RenderableSurface\>.  
   * If StackNode: Return flatten(activeChild). If transitioning, also return flatten(previousChild).  
   * If PaneNode: Return panes.flatMap { flatten(it) }.  
   * If TabNode: Return flatten(stacks).

### **Component C: Predictive Back Integration**

1. **Integration Point**:  
   * Use androidx.activity.compose.PredictiveBackHandler.  
   * Capture the BackEventCompat progress (0.0 \-\> 1.0).  
2. **Visual Logic**:  
   * When the gesture starts, identify the "Top Surface" and the "Surface Below" from the tree.  
   * Temporarily add the "Surface Below" to the visibleSurfaces list if it wasn't there.  
   * Apply Modifier.graphicsLayer { scaleX \= 0.9f \+ (0.1f \* progress) } to the "Surface Below".  
   * Apply Modifier.graphicsLayer { translationX \= progress \* width } to the "Top Surface".

### **Component D: KSP Processor**

1. **Annotations**:  
   * `@Destination` - Marks navigation targets with optional deep link route
   * `@Stack` - Linear navigation container with name and start destination
   * `@Tab` - Tabbed navigation container with tab items
   * `@Pane` - Adaptive layout container with pane roles
   * `@Screen` - Binds Composable functions to destinations

2. **Generation**:  
   * Generate `build{Name}NavNode()` functions for each container
   * Generate `GeneratedScreenRegistry` object for destination → Composable mapping
   * Generate `GeneratedDeepLinkHandler` for URI → Destination parsing
   * Generate `NavigatorExtensions.kt` for convenience navigation methods

## **4\. Execution Checklist**

* \[x\] Define NavNode sealed hierarchy in core.  
* \[x\] Implement Navigator.push/pop logic using functional tree updates.  
* \[x\] Create QuoVadisHost using SharedTransitionLayout.  
* \[x\] Implement the flattenState algorithm to determine Z-ordering.  
* \[x\] Connect PredictiveBackHandler to drive the animation state.  
* \[x\] Refactor KSP generator to output NavNode definitions.
* \[x\] Define new annotation system (@Destination, @Stack, @Tab, @Pane, @Screen).
* \[x\] Implement KSP extractors for each annotation type.
* \[x\] Implement generators for NavNode builders, ScreenRegistry, and DeepLinkHandler.

## ---

**7\. Risk Analysis and Mitigation**

Implementing a custom rendering engine is a high-reward, high-risk endeavor.

### **7.1 Performance Overhead**

* **Risk**: Flattening a large tree on every frame or recomposition could cause jank.  
* **Mitigation**: The flattenState function must be memoized using remember(state). Furthermore, the state tree should utilize structural sharing (standard in Kotlin data classes) so that unchanged branches do not trigger recomposition.

### **7.2 System Gesture Conflicts**

* **Risk**: The global predictive back handler might conflict with internal swipeable components (e.g., Maps, Horizontal Pagers).  
* **Mitigation**: The QuoVadisHost must respect LocalView.current.systemGestureExclusionRects (on Android). The library should provide a Modifier.excludeFromBackGesture() that allows developers to opt-out specific regions from triggering the navigation back.

### **7.3 Deep Linking in Complex Trees**

* **Risk**: Reconstructing a specific state (e.g., "Tab 2, Item 4, Details") from a URL is difficult in a tree structure.  
* **Mitigation**: The KSP processor should generate "Path Reconstructors." When a Deep Link matches a destination, the generator knows the static graph structure. It can automatically synthesize the parent TabNode and StackNode required to hold that destination, ensuring the user lands in a valid context with a functional back button.

## **Conclusion**

The "Single Rendering Component" architecture represents the completed maturation of the *Quo Vadis* library. By adopting the **Omni-Render Model**, the library has moved beyond the limitations of platform-wrapped navigation wrappers. The architecture—grounded in a reactive NavNode tree and realized through a flattened, shared-element-aware projection—solves the "Nested Host" problem that plagues current KMP navigation.

The included "Prompt File" provides the tactical instructions for the development agent. This refactor has positioned *Quo Vadis* not just as a type-safe router, but as a premier navigation engine capable of delivering the fluid, high-fidelity experiences expected in modern mobile applications.

## ---

**Appendix: Reference Data**

### **Table 1: Comparative Architecture Analysis**

| Feature | Current Quo Vadis (Standard) | Proposed Quo Vadis (Omni-Render) | Standard Compose Navigation |
| :---- | :---- | :---- | :---- |
| **State Model** | Linear List (BackStack) | Recursive Tree (NavNode) | Nested NavHost Graph |
| **Rendering** | Decentralized (Nested Hosts) | Centralized (QuoVadisHost) | Decentralized |
| **Shared Elements** | Impossible/Difficult across Hosts | Native Support (SharedTransitionLayout) | Limited (requires one host) |
| **Predictive Back** | System Default | Custom Physics-based Rendering | System Default |
| **Adaptive Layouts** | Manual BoxWithConstraints | First-class PaneNode | AdaptiveScaffold (separate lib) |

### **Table 2: Required Dependencies for Refactor**

| Dependency | Purpose | Module |
| :---- | :---- | :---- |
| androidx.compose.animation:animation | Core animation primitives | quo-vadis-core |
| androidx.compose.animation:animation-graphics | Advanced transitions | quo-vadis-core |
| androidx.activity:activity-compose | BackHandler and PredictiveBack | quo-vadis-core (Android) |
| kotlinx.collections.immutable | Efficient tree manipulation | quo-vadis-core |
| com.google.devtools.ksp | Code generation for Graph Builders | quo-vadis-ksp |

#### **Cytowane prace**

1. jermeyyy/quo-vadis: Compose Multiplatform navigation library \- GitHub, otwierano: grudnia 2, 2025, [https://github.com/jermeyyy/quo-vadis](https://github.com/jermeyyy/quo-vadis)