# Compiler Plugin IR Phase 3 — Detailed Context

## File Locations
- Base: `quo-vadis-compiler-plugin/src/main/kotlin/com/jermey/quo/vadis/compiler/`
- IR files: `ir/` subdirectory
- Generators: `ir/generators/` and `ir/generators/dsl/`
- Common: `common/NavigationMetadata.kt`, `common/PluginMetadataStore.kt`

## SymbolResolver Key Symbols
- `screenRegistryClass` → `com.jermey.quo.vadis.core.registry.ScreenRegistry`
- `containerRegistryClass` → `com.jermey.quo.vadis.core.registry.ContainerRegistry`
- `deepLinkRegistryClass` → `com.jermey.quo.vadis.core.registry.DeepLinkRegistry`
- `paneRoleRegistryClass` → `com.jermey.quo.vadis.core.registry.PaneRoleRegistry`
- `scopeRegistryClass` → `com.jermey.quo.vadis.core.registry.ScopeRegistry`
- `transitionRegistryClass` → `com.jermey.quo.vadis.core.registry.TransitionRegistry`
- `navigationConfigClass` → `com.jermey.quo.vadis.core.navigation.config.NavigationConfig`
- `navigatorClass` → `com.jermey.quo.vadis.core.navigation.navigator.Navigator`
- `deepLinkClass` → `com.jermey.quo.vadis.core.navigation.destination.DeepLink`
- `navDestinationClass` → `com.jermey.quo.vadis.core.navigation.destination.NavDestination`
- `compositeNavigationConfigClass` → `com.jermey.quo.vadis.core.navigation.internal.config.CompositeNavigationConfig`
- `navTransitionClass` → `com.jermey.quo.vadis.core.compose.transition.NavTransition`
- `kClassClass`, `lazyClass`, `regexClass`, `setOfFunctions`, `listOfFunctions`

## DeepLinkRegistry Interface Methods (all override)
1. `resolve(uri: String): NavDestination?`
2. `resolve(deepLink: DeepLink): NavDestination?`
3. `register(pattern: String, factory: (Map<String, String>) -> NavDestination)`
4. `registerAction(pattern: String, action: (Navigator, Map<String, String>) -> Unit)`
5. `handle(uri: String, navigator: Navigator): Boolean`
6. `createUri(destination: NavDestination, scheme: String): String?`
7. `canHandle(uri: String): Boolean`
8. `getRegisteredPatterns(): List<String>`

## PaneRoleRegistry Interface Methods
1. `getPaneRole(scopeKey: ScopeKey, destination: NavDestination): PaneRole?`
2. `getPaneRole(scopeKey: ScopeKey, destinationClass: KClass<out NavDestination>): PaneRole?`
3. `hasPaneRole(scopeKey: ScopeKey, destination: NavDestination): Boolean` (has default impl)

## NavigationConfigIrGenerator Pattern
- `generate(irClass)` iterates class declarations matching properties/functions by name
- Property names: screenRegistry, scopeRegistry, transitionRegistry, containerRegistry, deepLinkRegistry, paneRoleRegistry, roots
- Function names: buildNavNode, plus
- Each delegates to specialized generator or generates inline

## BodySynthesisTransformer.kt
```kotlin
private fun synthesizeDeepLinkHandlerBody(irClass: IrClass) {
    // TODO: Phase 3E will implement body synthesis for DeepLinkHandler
}
```

## FIR DeepLinkHandler declarations generated
The FIR extension generates a `{Prefix}DeepLinkHandler : DeepLinkRegistry` object with all 8 override methods declared but no bodies.

## Current Status of NavigationConfigIrGenerator methods:
- `generateScreenRegistryProperty` → delegates to ScreenRegistryIrGenerator (returns Empty)
- `generateContainerRegistryProperty` → delegates to ContainerRegistryIrGenerator (returns Empty)
- `generatePaneRoleRegistryProperty` → placeholder (irGetObject on PaneRoleRegistry)
- `generateDeepLinkRegistryProperty` → placeholder (irGetObject on deepLinkHandlerClass)
- `generateDelegatedRegistryProperty` → placeholder for scope/transition (irGetObject)
- `generateRootsProperty` → fully implemented (setOf(rootClasses))
- `generateBuildNavNodeBody` → returns null placeholder
- `generatePlusBody` → fully implemented (CompositeNavigationConfig constructor)

## API Docs
https://github.com/JetBrains/kotlin/tree/master/compiler/ir