# **Strategic Implementation Plan: Migrating Quo-Vadis Navigation Library from Kotlin Symbol Processing to a Native Kotlin K2 Compiler Plugin**

## **Executive Summary and Strategic Rationale**

The continuous evolution of the Kotlin metaprogramming ecosystem has reached a definitive inflection point with the stabilization of the K2 compiler in Kotlin version 2.0.0.1 Historically, code generation within the Kotlin multiplatform environment relied initially on the Kotlin Annotation Processing Tool (KAPT) and subsequently on Kotlin Symbol Processing (KSP).3 While KSP represented a significant architectural leap by eliminating the expensive generation of Java stubs—resulting in compilation speeds up to twice as fast as KAPT—it remained fundamentally constrained by its operational paradigm: the physical generation of intermediate source files that require subsequent compilation passes.3

The Quo-Vadis navigation library currently operates upon this KSP paradigm. By processing a highly expressive suite of annotations—including @Stack, @Tabs, @Pane, @Destination, @Argument, and @Transition—the library synthesizes zero-boilerplate routing structures and deep-link handling mechanisms.6 The primary outputs of this process are the {Prefix}NavigationConfig and {Prefix}DeepLinkHandler classes, which orchestrate the hierarchical NavNode tree architecture that defines the library's core value proposition.6 However, the reliance on KSP introduces substantial friction, particularly within Kotlin Multiplatform (KMP) codebases. KSP mandates extensive manual source-set configuration, often requiring developers to write dozens of lines of Gradle boilerplate per target platform just to expose the generated source directories to the compiler.6 Furthermore, the physical generation of files disrupts the developer's "flow state" by delaying error reporting and IDE synchronization until a full build cycle completes.4

Migrating the Quo-Vadis architecture to a native Kotlin K2 compiler plugin addresses these systemic inefficiencies by hooking directly into the compiler's memory and abstract syntax tree (AST).9 This paradigm shift bypasses physical code generation entirely, instead utilizing the Frontend Intermediate Representation (FIR) to inject synthetic declarations directly into the IDE's symbol table, and the Intermediate Representation (IR) to weave the navigation logic directly into the compiled binaries.10 The performance implications of this architectural shift are profound; empirical data from large-scale migrations, such as the Anki-Droid project, demonstrate that the K2 compiler yields up to a 94% improvement in overall compilation speed, with initialization phases operating up to 488% faster and analysis phases up to 376% faster.8 Similarly, the migration of the Koin dependency injection framework from KSP to a compiler plugin—a process conceptually identical to the required Quo-Vadis migration—resulted in a 90% reduction in build configuration overhead and the total elimination of KSP dependencies across the project topography.7

This comprehensive implementation plan delineates the exhaustive strategy required to transition Quo-Vadis from a KSP-based code generator to a native K2 compiler plugin. The ensuing analysis covers the necessary topological restructuring of the project modules, the precise mechanics of FIR and IR transformations, the translation of URI-based deep link handling into low-level AST logic, the resolution of multi-module graph aggregation, and the rigorous testing methodologies required to ensure compiler stability.

## **Architectural Topography and Module Restructuring**

Compiler plugins operate under stringent environmental constraints that differ fundamentally from standard application code or KSP processors. Because the Kotlin compiler must parse, analyze, and lower code across multiple target platforms (JVM, JavaScript, WebAssembly, and Native), the plugin infrastructure must be explicitly segmented to align with these varying compilation environments.2 The transition requires deprecating the existing quo-vadis-ksp module and establishing a sophisticated, multi-module plugin topology.6

To support the full spectrum of Kotlin Multiplatform targets, the Quo-Vadis repository must be restructured into four distinct logical modules, cleanly separating the runtime API from the build-time instrumentation.

| Module Identifier | Architectural Responsibility | Target Compilation Platforms | Key Dependencies and Artifacts |
| :---- | :---- | :---- | :---- |
| quo-vadis-core | Defines the runtime API, NavNode tree models, and the declarative annotations (@Stack, @Destination, @Pane, etc.). | All KMP Targets (Android, iOS, Desktop, Web) | kotlinx-serialization-json, kotlinx-coroutines, FlowMVI (optional) 6 |
| quo-vadis-gradle-plugin | Acts as the build-system integration layer. Intercepts Gradle compilation tasks, parses user configurations, and injects the compiler plugin artifact into the Kotlin compilation classpath. | Gradle JVM | Implements KotlinCompilerPluginSupportPlugin 10 |
| quo-vadis-compiler-plugin | Houses the core compiler extension logic. Implements the FIR and IR transformation phases to synthesize the navigation configuration and deep link handlers. | Non-Native Platforms (JVM, JS, Wasm) | Depends on org.jetbrains.kotlin:kotlin-compiler-embedded 14 |
| quo-vadis-compiler-plugin-native | Provides a dedicated compilation wrapper for Native targets. Mirrors the core plugin logic but utilizes an unshadowed compiler dependency to resolve package structure discrepancies. | Kotlin/Native Platforms (iOS, macOS, Linux) | Depends on org.jetbrains.kotlin:kotlin-compiler 14 |

### **The Gradle Support Plugin Interface**

The entry point for the metaprogramming pipeline is the quo-vadis-gradle-plugin. In the legacy KSP architecture, this plugin was primarily responsible for configuring the kspCommonMainMetadata dependency, applying the modulePrefix logic, and manually wiring the build/generated/ksp/metadata/commonMain/kotlin output directories into the project's source sets.6 The native compiler plugin paradigm fundamentally alters this responsibility.

The Gradle plugin must now implement the KotlinCompilerPluginSupportPlugin interface, which serves as the critical bridge between the Gradle build lifecycle and the internal mechanisms of the Kotlin compiler.10 This implementation must fulfill several exacting requirements to ensure frictionless integration for the end user. First, it must dictate the artifact resolution strategy via the getPluginArtifact() and getPluginArtifactForNative() functions.14 This bifurcation is mandatory because Kotlin/Native compiler plugins must be compiled against the raw org.jetbrains.kotlin:kotlin-compiler artifact, whereas all other targets utilize the kotlin-compiler-embedded artifact to avoid classpath collisions in the Gradle daemon.14 To maintain a single source of truth and avoid code duplication, the build scripts must be configured to automatically synchronize the source files from the standard plugin module to the native plugin module during the build process.14

Secondly, the Gradle plugin is responsible for configuration translation. The user-facing extension properties, such as the customizable modulePrefix (e.g., modulePrefix \= "MyApp"), must be extracted from the build.gradle.kts file and translated into SubpluginOption instances.6 These options are serialized as command-line arguments and passed directly into the compiler plugin's CommandLineProcessor during the initialization phase.10

Finally, adopting the patterns observed in highly successful migrations like Koin, the Gradle plugin should automatically apply the quo-vadis-core dependency to the user's project.7 By encapsulating both the metaprogramming artifact and the runtime dependency within a single convention plugin, developers can integrate the entire Quo-Vadis architecture with a single line of code in their build scripts, achieving the ultimate goal of zero-boilerplate configuration.7

### **Service Loader Initialization and Plugin Registration**

The transition from Gradle to the compiler's internal lifecycle occurs via standard Java Service Provider Interfaces (SPI). The quo-vadis-compiler-plugin must define its entry points within the META-INF/services directory, a process typically automated using annotation processors like @AutoService.10

Two critical services must be registered. The first is the CommandLineProcessor, which declares the unique plugin identifier and intercepts the SubpluginOption arguments forwarded by the Gradle plugin.10 The processor decodes the modulePrefix and stores it within the immutable CompilerConfiguration map, making the prefix accessible to all subsequent compilation phases.14 The second service is the CompilerPluginRegistrar.10 This registrar acts as the grand orchestrator, systematically binding the custom Frontend Intermediate Representation (FIR) and Backend Intermediate Representation (IR) extensions into the compiler's execution pipeline.10

## **Frontend Intermediate Representation (FIR): Synthetic Declaration Synthesis**

The architectural foundation of the K2 compiler relies on the Frontend Intermediate Representation (FIR), a robust phase responsible for parsing the initial syntax tree, performing deep semantic analysis, resolving function calls, and executing type inference.8 Within the KSP paradigm, Quo-Vadis fulfilled its code generation duties by physically writing strings of Kotlin code to disk, producing actual .kt files representing the NavigationConfig and DeepLinkHandler.6 The IDE would subsequently index these physical files, enabling syntax highlighting and auto-completion.

The K2 compiler plugin paradigm completely abolishes physical file generation. Instead, the plugin interacts with the FIR extensions to inject *synthetic declarations* directly into the compiler's internal symbol table.10 Because modern integrated development environments—such as IntelliJ IDEA 2024.1+ and recent iterations of Android Studio—utilize the compiler's FIR frontend natively for their K2 mode analysis, these synthetic classes and functions appear instantaneously in the developer's autocomplete menus exactly as if they were written by hand, despite possessing no physical representation on disk.10

### **Predicate-Based Annotation Discovery**

To synthesize the navigation structures, the compiler plugin must first identify every instance of a Quo-Vadis annotation (@Stack, @Tabs, @Pane, @Destination, @Screen) scattered across the codebase.6 Unlike KSP, which provides a straightforward sequence of annotated elements, FIR requires a more sophisticated approach due to the concurrent nature of semantic analysis.

The optimal strategy involves leveraging the FirPredicateBasedProvider service.17 This service allows the plugin to define a DeclarationPredicate that continuously monitors the abstract syntax tree for specific conditions during the resolution phases.17 The plugin will instantiate predicates targeting the fully qualified names of the Quo-Vadis annotations. As the compiler traverses the user's codebase, the predicate provider aggregates all classes matching these constraints, storing their FirClassSymbol representations for processing.17 This approach ensures that the plugin can perform global lookups across the current compilation module without relying on hardcoded class names or fragile string matching.17

### **Implementing FirDeclarationGenerationExtension**

The actual creation of the {Prefix}NavigationConfig and {Prefix}DeepLinkHandler entities is achieved by extending the FirDeclarationGenerationExtension.11 It is critical to understand that this extension operates strictly on the frontend; its exclusive purpose is to declare the *existence*, *shape*, and *signatures* of the synthetic elements, explicitly ignoring any implementation logic or method bodies.18

The synthesis process unfolds across several meticulously orchestrated overrides within the extension:

1. **Top-Level Declaration Initiation:** The extension must override the getTopLevelClassIds function.17 When the compiler polls this function, the plugin retrieves the modulePrefix from the configuration context and responds with the highly specific ClassId objects representing the intent to create the {Prefix}NavigationConfig object and the {Prefix}DeepLinkHandler class.6  
2. **Structural Definition:** Following the top-level declaration, the compiler invokes generateTopLevelClassLikeDeclaration. Here, the plugin constructs the structural blueprint of the configuration object. It must explicitly define the class as an object (singleton), assign the appropriate supertypes to ensure it conforms to the internal NavigationConfig interfaces expected by the TreeNavigator, and register a GeneratedDeclarationKey to link the synthetic symbol to the plugin's origin.6  
3. **Member Synthesis:** The plugin overrides getCallableNamesForClass to declare the specific methods that reside within the newly synthesized classes.10 For the configuration object, this might include methods required to expose the NavNode tree or transition definitions.6  
4. **Signature Resolution:** In the final frontend step, the compiler invokes generateFunctions. The plugin must build the FirSimpleFunction structures, meticulously ensuring that the return types and parameter definitions align flawlessly with the core Quo-Vadis runtime types, such as TransitionType, NavDestination, and PaneRole.6

Because the FIR phase guarantees the structural contract of these synthetic symbols, if a developer writes val navigator \= TreeNavigator(config \= MyAppNavigationConfig) in their application code, the IDE will validate the statement as syntactically correct and type-safe, despite the fact that the underlying implementation logic does not yet exist.6

### **Managing Visibility and Encapsulation**

The architecture of a multi-module navigation library often demands strict control over the encapsulation of its configuration objects.6 A configuration object generated within a feature module should ideally remain internal to that module unless explicitly aggregated into a higher-level host. To achieve this, the compiler plugin can implement the FirStatusTransformerExtension.11 This extension intercepts the status resolution phase for all declarations and allows the plugin to dynamically mutate the visibility modifiers. By evaluating the synthetic classes, the plugin can enforce internal or public visibility based on the module's build configuration, ensuring robust architectural boundaries that KSP string generation struggles to maintain gracefully.11

## **Developer Experience (DX) and Real-Time Diagnostics**

One of the most profound advantages of migrating from KSP to a native compiler plugin is the monumental enhancement of the Developer Experience (DX) through real-time diagnostics.8 In the legacy KSP workflow, structural errors within the navigation graph—such as assigning identical route strings to two different @Destination annotations, or applying a transition to a container that does not support it—were only detected late in the development cycle, typically causing the KSP build task to crash.4

By implementing the FirAdditionalCheckersExtension, the Quo-Vadis compiler plugin can elevate domain-specific validation to the same level as native language features.11 This extension allows the plugin to register custom FirDeclarationChecker instances that run synchronously during the compiler's semantic analysis phase.11

### **Diagnostic Validation Strategies**

The implementation plan must incorporate comprehensive diagnostic checkers to validate the integrity of the navigation annotations before they are lowered into executable logic:

| Diagnostic Target | Validation Logic | IDE Presentation |
| :---- | :---- | :---- |
| **Route Collisions** | Iterates across all discovered @Destination and @Screen annotations.6 Evaluates the constant string values assigned to the route parameters. If a duplicate string is identified across the compilation module, an error is generated. | Red underline beneath the duplicate @Destination annotation, displaying: *"Duplicate route detected. Routes must be unique."* |
| **Deep Link Argument Parity** | Analyzes the URL path segments defined in a route (e.g., "catalog/detail/{id}").6 Cross-references the extracted variable names against the properties within the data class annotated with @Argument.6 | Yellow warning or red error on the @Argument property if the name does not exactly match the placeholder in the route string. |
| **Transition Compatibility** | Validates the context of the @Transition annotation.6 For example, if TransitionType.SlideVertical is applied to an element within a @Tabs container that strictly prohibits vertical transitions, the checker intervenes. | Red underline indicating structural incompatibility within the navigation tree. |
| **Container Role Validation** | Inspects @Pane layouts to ensure that components annotated with @PaneItem correctly define PaneRole.PRIMARY and PaneRole.SECONDARY.6 | Error highlighting missing or duplicated primary roles within an adaptive layout. |

By weaving these constraints directly into the compiler, developers receive instantaneous feedback. The moment a malformed annotation is typed, the IDE highlights the infraction.8 This real-time validation drastically reduces context-switching, prevents runtime crashes due to malformed URLs, and fosters a significantly more reliable and efficient development environment.8

## **Backend Intermediate Representation (IR): Logic and AST Transformation**

While the FIR frontend guarantees the structural contract and manages the IDE experience, the actual execution logic is entirely absent.18 Once the semantic analysis concludes successfully, the compiler lowers the code into the Intermediate Representation (IR) phase.14 IR is a highly granular, platform-agnostic abstract syntax tree that represents the precise executable behavior of the program before it is translated into JVM bytecode, JavaScript, WebAssembly, or Native machine code.10

The IrGenerationExtension is responsible for materializing the logic of the synthetic declarations registered by FIR.11 It is within this extension that the complex, boilerplate-heavy string concatenation utilized by KSP is replaced by precise, programmatic AST manipulation. Because the IR backend cannot influence the IDE's analysis, its sole purpose is to mutate the compiled binary.11

### **The Two-Pass IR Generation Strategy**

Compiler plugins synthesizing complex, interconnected structures typically employ a rigorous two-pass strategy within the IR phase to prevent infinite recursion and ensure that all references resolve correctly.18

1. **Pass 1: Stub Materialization:** The plugin must traverse the IrModuleFragment to locate the synthetic symbols generated by the frontend (the {Prefix}NavigationConfig and {Prefix}DeepLinkHandler).14 In this pass, the plugin creates physical IR stubs—empty class and function representations—binding them to the specific signatures dictated by the FIR declarations.18  
2. **Pass 2: Execution Body Synthesis:** Utilizing an implementation of IrElementTransformerVoid, the plugin traverses the newly created stubs, injecting IrBlockBody elements into the synthetic functions.21 This pass weaves the actual execution logic into the application.

### **Synthesizing the NavNode Tree Architecture**

The fundamental value proposition of Quo-Vadis is its hierarchical NavNode tree architecture, which encapsulates the navigation state as a strict hierarchy of nodes, representing stacks, tabs, and adaptive panes.6 In the KSP paradigm, synthesizing this tree required writing extensive strings of Kotlin code to instantiate nested objects. In the IR paradigm, the plugin must construct these instantiations programmatically using the IrPluginContext.14

The context provides access to the compiler's master symbol table, allowing the plugin to resolve exact references to core library classes, such as TreeNavigator, TransitionType, NavDestination, and StackNode.14

Consider a scenario where a developer defines a navigation stack using Quo-Vadis annotations:

Kotlin

@Stack(name \= "home", startDestination \= Feed::class)  
sealed class HomeDestination : NavDestination {  
    @Destination(route \= "home/feed")  
    data object Feed : HomeDestination()  
      
    @Transition(type \= TransitionType.SlideHorizontal)  
    @Destination(route \= "details/{id}")  
    data class Details(@Argument val id: String) : HomeDestination()  
}

6

When the IrElementTransformerVoid encounters this structure, the plugin orchestrates a complex sequence of AST manipulations:

1. **Reference Acquisition:** The plugin utilizes the symbol table to look up the constructor for the internal StackNode class and the target destination representing the startDestination (Feed).21  
2. **Parent Node Construction:** It generates an IrConstructorCall targeting the StackNode constructor.21 The name parameter of the constructor is populated by creating a constant string expression (IrConst.string("home")).6  
3. **Hierarchical Assembly:** The transformer iterates through all subclasses of HomeDestination annotated with @Destination or @Screen.6 For each subclass, it generates the corresponding DestinationNode constructor calls. These child node constructions are then injected as an IrVararg array into the parent StackNode constructor call, physically building the tree structure in memory.6  
4. **Transition Mapping:** Upon analyzing the Details class, the plugin detects the @Transition(type \= TransitionType.SlideHorizontal) annotation.6 It resolves the IR symbol for the TransitionType.SlideHorizontal enum constant and injects an IrGetEnumValue expression into the transition parameter of the destination node's constructor.6

This meticulous, node-by-node construction results in an execution graph that is overwhelmingly optimized. By constructing the object instantiations directly within the AST, the library bypasses the need for reflection or string interpretation entirely.11 Furthermore, the logic generated in IR is inherently robust; the Kotlin compiler enforces strict type safety at the AST level. If the plugin attempts to construct a malformed tree—such as passing a string where an integer is expected—the compilation will explicitly fail the \-Xverify-ir backend check, guaranteeing that invalid bytecode is never produced.22

## **Deep Link Resolution and URI Parameter Extraction**

Beyond static navigation, Quo-Vadis provides sophisticated capabilities for deep-link handling. By mapping standard URIs directly to specific navigation destinations, the library enables seamless transitions from external triggers (e.g., push notifications, web links) into complex application states.26 The {Prefix}DeepLinkHandler serves as the engine for this capability, utilizing the @Argument annotation to achieve automatic deep link serialization and parameter extraction.6

Migrating this feature from KSP to the IR backend requires translating declarative string templates into hyper-efficient executable parsing logic. In the legacy architecture, the generated code heavily relied on runtime string matching and dynamic parameter casting.6 The compiler plugin approach allows this parsing infrastructure to be pre-computed and hardcoded directly into the application's binary.

### **Synthesizing the Parsing Protocol**

When the IR phase processes deep link destinations, it must transform the URI patterns into executable matchers:

1. **Route Pattern Compilation:** The plugin extracts the route parameters from all @Destination annotations. For instance, analyzing route \= "catalog/detail/{id}" reveals a static path segment (catalog/detail/) and a dynamic variable segment ({id}).6  
2. **Matcher Logic Injection:** Instead of passing raw strings to a runtime parser, the plugin synthesizes the exact sequence of IrCall instances required to match the URL. It can generate hardcoded if statements or optimized when blocks that compare incoming URI path segments directly against the static components of the route, ensuring negligible runtime overhead.  
3. **Type-Safe Casting and Instantiation:** When the plugin analyzes the Details data class containing @Argument val id: String 6, it knows the precise type required by the constructor. The IR transformer generates the specific IrCall to extract the variable from the URI segment array, injects the necessary nullability checks, and subsequently constructs the destination object via an IrConstructorCall. If the argument type was an integer (@Argument val count: Int), the IR would automatically inject the String.toIntOrNull() conversion logic directly into the generated handler.

This implementation guarantees that deep link resolution is entirely type-safe. By shifting the burden of string parsing from runtime interpretation to compile-time generated logic, the {Prefix}DeepLinkHandler operates with maximum efficiency, ensuring instantaneous deep link resolution even in deeply nested navigation graphs.

## **Multi-Module Graph Aggregation and Global Resolution**

A formidable architectural challenge encountered when migrating from KSP to a native compiler plugin involves the mechanics of cross-module aggregation.30 KSP operates as an isolated build task that can potentially be configured to aggregate outputs across specific compilation boundaries or generate distinct files per module that are manually linked by the developer.3 Compiler plugins, conversely, are strictly constrained by their execution context; they operate exclusively on the current IrModuleFragment and cannot directly mutate or access the AST of previously compiled modules.14

Currently, Quo-Vadis supports a multi-module topology by generating discrete configuration objects for each module, compelling the developer to manually combine them using an overloaded \+ operator (e.g., AppNavigationConfig \+ Feature1NavigationConfig \+ Feature2NavigationConfig).6 While functional, this manual wiring contradicts the philosophy of zero-boilerplate navigation. To resolve this limitation and enhance the library's multi-module capabilities, the architecture must adopt a metadata-driven auto-discovery pattern, drawing direct inspiration from the successful migration strategies employed by the Koin dependency injection framework.7

### **The Typed API and Metadata Auto-Discovery Pattern**

Rather than forcing developers to manually chain generated configuration objects, the compiler plugin can orchestrate an elegant discovery mechanism leveraging the compiler's internal metadata.17

The implementation unfolds across two distinct phases:

1. **Module-Level FIR Metadata Embedding:** When the compiler plugin processes a distinct feature module (e.g., the catalog module), it synthesizes the CatalogNavigationConfig utilizing the mechanisms detailed previously. Crucially, because this configuration is declared during the FIR phase, the compiler bakes the existence and signature of this synthetic object directly into the module's generated .klib or .jar metadata.17 The object becomes a fully resolvable symbol for any module that depends upon the catalog module.  
2. **The Application Composition Root:** In the primary application module, developers adopt a typed API approach.7 Quo-Vadis will introduce a specialized annotation, such as @QuoVadisHost or @NavigationRoot, to be placed on the central application component.  
3. **Global Aggregation via IR Assembly:** When the compiler plugin processes the primary application module containing the @NavigationRoot annotation, it utilizes the IrPluginContext symbol table to scan the entire visible classpath.7 The plugin searches for all types that implement the internal, core library NavigationConfig interface. Because the FIR metadata of the dependent feature modules exposes these synthetic configurations, the application module's IR extension can retrieve their symbols. The IR transformer then generates a unified root tree, programmatically iterating through the discovered configurations and injecting their respective sub-graphs into the global TreeNavigator initialization sequence.6

This paradigm shift entirely eradicates the need for manual \+ operator chaining across module boundaries. By adopting this auto-discovery pattern, Quo-Vadis achieves the extreme brevity observed in Koin's compiler plugin migration, where complex multiplatform configuration boilerplate was reduced from approximately 25 lines of code down to a single declarative line.7 Furthermore, this architecture aligns flawlessly with Kotlin Multiplatform's expect/actual declarations. Developers can define platform-specific sub-graphs—such as an adaptive @Pane layout specifically for Desktop targets, juxtaposed against a specialized @Tabs layout for iOS—and the compiler plugin will dynamically discover and assemble the correct, target-specific navigation tree during the respective compilation passes.6

## **Testing, Verification, and Deployment Pipelines**

The testing and deployment methodologies for native compiler plugins are substantially more complex than those utilized for standard unit testing or KSP validation.14 Because compiler plugins operate by directly mutating the internal abstract syntax tree, malformed logic generated during the IR phase can lead to catastrophic downstream failures, manifesting as invalid JVM bytecode, corrupted JavaScript outputs, or fatal crashes during Native compilation.11 The implementation plan requires the establishment of a rigorous verification pipeline.

### **Integration Testing Framework**

The cornerstone of the testing infrastructure is the kotlin-compile-testing library.14 This specialized library enables the test suite to pass raw Kotlin source code strings directly into an embedded, isolated compiler instance that has been pre-configured with the Quo-Vadis CompilerPluginRegistrar.14 This methodology allows for the rapid iteration and validation of the entire compilation lifecycle.

The testing strategy must be bisected to isolate the frontend and backend transformations:

| Testing Vector | Verification Methodology | Target Outcome |
| :---- | :---- | :---- |
| **Diagnostic Tests** | Injecting malformed Kotlin strings (e.g., duplicated routes or mismatched @Argument definitions) into the embedded compiler.15 | Asserting that the FirAdditionalCheckersExtension intercepts the compilation and emits the precise error messages and line numbers expected. |
| **Codegen Tests (Box Tests)** | Compiling complex, fully annotated navigation graphs. The test framework then utilizes reflection to instantiate the synthetic {Prefix}NavigationConfig and {Prefix}DeepLinkHandler classes.15 | Simulating routing requests and deep link URIs against the instantiated classes to ensure the generated IR logic produces the mathematically correct execution behavior and routing outcomes.15 |

### **IR Verification and Textual Dumping**

To guarantee backend stability and prevent regression, the build pipeline must leverage the compiler's internal verification tools.22

First, all automated test runs must be configured with the \-Xverify-ir compiler option.22 This flag acts as an uncompromising, strict linter for the IR tree. If the plugin's IrGenerationExtension produces a malformed AST structure—such as an IrCall with the incorrect number of arguments, or an IrGetEnumValue pointing to a non-existent symbol—the compilation will immediately fail with a descriptive error, preventing invalid transformations from progressing to the bytecode generation phase.22

Secondly, the testing framework must implement textual IR dumping.19 By utilizing the \-Xphases-to-dump-before=ExternalPackageParentPatcherLowering flag, the compiler outputs a highly detailed .fir.ir.txt file representing the finalized AST.19 The test suite compares the newly generated dump files against a repository of expected outputs. This stringent textual comparison provides an airtight regression testing mechanism, ensuring that subtle future modifications to the plugin's logic do not inadvertently warp the generated execution graph of the navigation tree.19

### **Artifact Publishing and Distribution**

Deploying a multiplatform compiler plugin requires careful orchestration to shield the end user from the underlying complexity of the compiler artifacts.14 Because the Kotlin/Native artifact must utilize a completely different package structure to function correctly (relying on org.jetbrains.kotlin:kotlin-compiler rather than the shadowed kotlin-compiler-embedded), the Gradle publishing scripts must dynamically copy the core plugin sources into the native module, programmatically adjusting package imports during the build and deployment process.14

This complexity, however, remains entirely confined to the Quo-Vadis deployment pipeline. Because the quo-vadis-gradle-plugin manages the artifact resolution intelligently based on the target compilation platform, downstream developers integrate the library simply by defining the plugin ID in their project, remaining blissfully insulated from the internal topological intricacies.10

## **Conclusion**

The strategic migration of the Quo-Vadis navigation library from a Kotlin Symbol Processing code generator to a native Kotlin K2 compiler plugin constitutes a monumental architectural upgrade. By systematically dismantling the constraints of physical file generation and embracing the advanced capabilities of the Frontend Intermediate Representation (FIR) and Intermediate Representation (IR), the library transitions into a state-of-the-art metaprogramming paradigm.

This exhaustive implementation plan demonstrates how to leverage FIR extensions to project synthetic declarations directly into the IDE's symbol table, yielding instantaneous error diagnostics and an unprecedented, fluid developer experience. It illustrates the precise methodologies required to traverse the IR tree, transforming declarative annotations into a highly optimized, allocation-efficient execution graph that assembles the NavNode hierarchy and extracts deep link URI parameters with absolute type safety.

Furthermore, by adopting a metadata-driven auto-discovery architecture, Quo-Vadis can eradicate the substantial configuration burdens traditionally associated with multi-module, multiplatform routing, mirroring the extraordinary operational efficiencies achieved by industry leaders like Koin. The successful execution of this architectural transition will ensure that Quo-Vadis remains a hyper-efficient, uniquely expressive, and future-proof navigation solution intricately aligned with the evolving trajectory of the Kotlin compiler ecosystem.

#### **Cytowane prace**

1. What's new in Kotlin 2.0.0, otwierano: marca 2, 2026, [https://kotlinlang.org/docs/whatsnew20.html](https://kotlinlang.org/docs/whatsnew20.html)  
2. Kotlin 2.0.0, otwierano: marca 2, 2026, [https://book.kotlincn.net/text/whatsnew20.html](https://book.kotlincn.net/text/whatsnew20.html)  
3. Migrate from kapt to KSP | Android Studio, otwierano: marca 2, 2026, [https://developer.android.com/build/migrate-to-ksp](https://developer.android.com/build/migrate-to-ksp)  
4. Nativeblocks Server-driven UI \- Extending Kotlin compiler with KSP, otwierano: marca 2, 2026, [https://nativeblocks.io/blog/extending-kotlin-compiler-with-ksp/](https://nativeblocks.io/blog/extending-kotlin-compiler-with-ksp/)  
5. How Kotlin Annotations Work — Part 1 \- ProAndroidDev, otwierano: marca 2, 2026, [https://proandroiddev.com/how-kotlin-annotations-work-8d06798a32d2](https://proandroiddev.com/how-kotlin-annotations-work-8d06798a32d2)  
6. jermeyyy/quo-vadis: Compose Multiplatform navigation library \- GitHub, otwierano: marca 2, 2026, [https://github.com/jermeyyy/quo-vadis](https://github.com/jermeyyy/quo-vadis)  
7. koin-compiler-plugin/docs/MIGRATION\_FROM\_KSP.md at main, otwierano: marca 2, 2026, [https://github.com/InsertKoinIO/koin-compiler-plugin/blob/main/docs/MIGRATION\_FROM\_KSP.md](https://github.com/InsertKoinIO/koin-compiler-plugin/blob/main/docs/MIGRATION_FROM_KSP.md)  
8. All New K2 Compiler\! \- Sakhawat Hossain \- Medium, otwierano: marca 2, 2026, [https://shakilbd.medium.com/all-new-k2-compiler-766dd7d24d14](https://shakilbd.medium.com/all-new-k2-compiler-766dd7d24d14)  
9. Compiler plugins | Kotlin Documentation, otwierano: marca 2, 2026, [https://kotlinlang.org/docs/compiler-plugins-overview.html](https://kotlinlang.org/docs/compiler-plugins-overview.html)  
10. How to Build Your First Kotlin Compiler Plugin (Part 1\) \- HackerNoon, otwierano: marca 2, 2026, [https://hackernoon.com/how-to-build-your-first-kotlin-compiler-plugin-part-1](https://hackernoon.com/how-to-build-your-first-kotlin-compiler-plugin-part-1)  
11. Kotlin Compiler Plugins \- Kt. Academy, otwierano: marca 2, 2026, [https://kt.academy/article/ak-compiler-plugin](https://kt.academy/article/ak-compiler-plugin)  
12. K2 compiler migration guide | Kotlin Documentation, otwierano: marca 2, 2026, [https://kotlinlang.org/docs/k2-compiler-migration-guide.html](https://kotlinlang.org/docs/k2-compiler-migration-guide.html)  
13. InsertKoinIO/koin-compiler-plugin: Koin Kotlin Compiler ... \- GitHub, otwierano: marca 2, 2026, [https://github.com/InsertKoinIO/koin-compiler-plugin](https://github.com/InsertKoinIO/koin-compiler-plugin)  
14. Writing Your Second Kotlin Compiler Plugin, Part 1 — Project Setup, otwierano: marca 2, 2026, [https://bnorm.medium.com/writing-your-second-kotlin-compiler-plugin-part-1-project-setup-7b05c7d93f6c](https://bnorm.medium.com/writing-your-second-kotlin-compiler-plugin-part-1-project-setup-7b05c7d93f6c)  
15. Kotlin/compiler-plugin-template \- GitHub, otwierano: marca 2, 2026, [https://github.com/Kotlin/compiler-plugin-template](https://github.com/Kotlin/compiler-plugin-template)  
16. Koin Compiler Plugin Setup, otwierano: marca 2, 2026, [https://insert-koin.io/docs/setup/compiler-plugin/](https://insert-koin.io/docs/setup/compiler-plugin/)  
17. kotlin/docs/fir/fir-plugins.md at master · JetBrains/kotlin \- GitHub, otwierano: marca 2, 2026, [https://github.com/JetBrains/kotlin/blob/master/docs/fir/fir-plugins.md](https://github.com/JetBrains/kotlin/blob/master/docs/fir/fir-plugins.md)  
18. How kotlinx.serialization generates code: a compiler plugin deep dive, otwierano: marca 2, 2026, [https://www.revenuecat.com/blog/engineering/kotlinx-serialization/](https://www.revenuecat.com/blog/engineering/kotlinx-serialization/)  
19. "Symbol not found for T": When registering an annotation with a, otwierano: marca 2, 2026, [https://youtrack.jetbrains.com/projects/KT/issues/KT-79267/Symbol-not-found-for-T-When-registering-an-annotation-with-a-generic-type-using-FirDeclarationPredicateRegistrar](https://youtrack.jetbrains.com/projects/KT/issues/KT-79267/Symbol-not-found-for-T-When-registering-an-annotation-with-a-generic-type-using-FirDeclarationPredicateRegistrar)  
20. I would like to generate a class for top level functions tha kotlinlang, otwierano: marca 2, 2026, [https://slack-chats.kotlinlang.org/t/16768544/i-would-like-to-generate-a-class-for-top-level-functions-tha](https://slack-chats.kotlinlang.org/t/16768544/i-would-like-to-generate-a-class-for-top-level-functions-tha)  
21. How to Build Your First Kotlin Compiler Plugin (Part 2\) | HackerNoon, otwierano: marca 2, 2026, [https://hackernoon.com/how-to-build-your-first-kotlin-compiler-plugin-part-2](https://hackernoon.com/how-to-build-your-first-kotlin-compiler-plugin-part-2)  
22. Custom compiler plugins | Kotlin Documentation, otwierano: marca 2, 2026, [https://kotlinlang.org/docs/custom-compiler-plugins.html](https://kotlinlang.org/docs/custom-compiler-plugins.html)  
23. The Java Virtual Machine & The Kotlin Compiler | PDF \- Scribd, otwierano: marca 2, 2026, [https://www.scribd.com/presentation/808813351/The-Java-Virtual-Machine-the-Kotlin-Compiler](https://www.scribd.com/presentation/808813351/The-Java-Virtual-Machine-the-Kotlin-Compiler)  
24. I have developed a plugin that requires replacing the body o ... \- Kotlin, otwierano: marca 2, 2026, [https://slack-chats.kotlinlang.org/t/16630825/i-have-developed-a-plugin-that-requires-replacing-the-body-o](https://slack-chats.kotlinlang.org/t/16630825/i-have-developed-a-plugin-that-requires-replacing-the-body-o)  
25. ClassGeneratorExtension.kt \- GitHub, otwierano: marca 2, 2026, [https://github.com/JetBrains/kotlin/blob/master/compiler/ir/backend.jvm/src/org/jetbrains/kotlin/backend/jvm/extensions/ClassGeneratorExtension.kt](https://github.com/JetBrains/kotlin/blob/master/compiler/ir/backend.jvm/src/org/jetbrains/kotlin/backend/jvm/extensions/ClassGeneratorExtension.kt)  
26. Deep Linking \- Mapp Documentation, otwierano: marca 2, 2026, [https://docs.mapp.com/docs/deep-linking-1](https://docs.mapp.com/docs/deep-linking-1)  
27. Using Android Navigation Deeplink: An Easy Way to Use Deep Link, otwierano: marca 2, 2026, [https://oozou.com/blog/using-android-navigation-deeplink-an-easy-way-to-use-deep-link-in-navigation-component-on-a-big-project-97](https://oozou.com/blog/using-android-navigation-deeplink-an-easy-way-to-use-deep-link-in-navigation-component-on-a-big-project-97)  
28. Demystifying Deeplinks in Android using Navigation Components, otwierano: marca 2, 2026, [https://medium.com/@gabriel\_theCode/demystifying-deeplinks-in-android-using-navigation-components-991bdacfdbe4](https://medium.com/@gabriel_theCode/demystifying-deeplinks-in-android-using-navigation-components-991bdacfdbe4)  
29. App Architecture: Deeplinks \- ProAndroidDev, otwierano: marca 2, 2026, [https://proandroiddev.com/app-architecture-deeplinks-9b2c6b8a36a3](https://proandroiddev.com/app-architecture-deeplinks-9b2c6b8a36a3)  
30. Is it possiable to write a kotlin compiler plugin to modify classes from, otwierano: marca 2, 2026, [https://discuss.kotlinlang.org/t/is-it-possiable-to-write-a-kotlin-compiler-plugin-to-modify-classes-from-all-modules/22623](https://discuss.kotlinlang.org/t/is-it-possiable-to-write-a-kotlin-compiler-plugin-to-modify-classes-from-all-modules/22623)  
31. kitakkun/k2-compiler-plugin-template \- GitHub, otwierano: marca 2, 2026, [https://github.com/kitakkun/k2-compiler-plugin-template](https://github.com/kitakkun/k2-compiler-plugin-template)