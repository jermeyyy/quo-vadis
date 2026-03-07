@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package com.jermey.quo.vadis.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class SymbolResolver(private val pluginContext: IrPluginContext) {

    // region Class Symbol Cache
    private val classCache = mutableMapOf<ClassId, IrClassSymbol>()

    fun resolveClass(classId: ClassId): IrClassSymbol {
        return classCache.getOrPut(classId) {
            pluginContext.referenceClass(classId)
                ?: error("Quo Vadis compiler plugin: cannot resolve class ${classId.asFqNameString()}")
        }
    }

    fun resolveClass(packageFqn: String, name: String): IrClassSymbol {
        return resolveClass(ClassId(FqName(packageFqn), Name.identifier(name)))
    }
    // endregion

    // region Function Symbol Resolution
    fun resolveFunctions(packageFqn: String, name: String): Collection<IrSimpleFunctionSymbol> {
        val callableId = CallableId(FqName(packageFqn), Name.identifier(name))
        return pluginContext.referenceFunctions(callableId)
    }
    // endregion

    // region Core Navigation Types
    val navigationConfigClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.navigation.config", "NavigationConfig")
    }

    val navDestinationClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.navigation.destination", "NavDestination")
    }

    val navNodeClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.navigation.node", "NavNode")
    }

    val navigatorClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.navigation.navigator", "Navigator")
    }

    val deepLinkClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.navigation.destination", "DeepLink")
    }
    // endregion

    // region Registry Types
    val screenRegistryClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.registry", "ScreenRegistry")
    }

    val scopeRegistryClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.registry", "ScopeRegistry")
    }

    val transitionRegistryClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.registry", "TransitionRegistry")
    }

    val containerRegistryClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.registry", "ContainerRegistry")
    }

    val deepLinkRegistryClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.registry", "DeepLinkRegistry")
    }

    val paneRoleRegistryClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.registry", "PaneRoleRegistry")
    }

    val scopeKeyClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.navigation.node", "ScopeKey")
    }
    // endregion

    // region DSL Types
    val navigationConfigBuilderClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.dsl", "NavigationConfigBuilder")
    }

    val stackBuilderClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.dsl", "StackBuilder")
    }

    val tabsBuilderClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.dsl", "TabsBuilder")
    }

    val panesBuilderClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.dsl", "PanesBuilder")
    }

    val navigationConfigDslFunctions by lazy {
        resolveFunctions("com.jermey.quo.vadis.core.dsl", "navigationConfig")
    }

    val navigationConfigBuilderConstructor by lazy {
        navigationConfigBuilderClass.owner.declarations
            .filterIsInstance<IrConstructor>()
            .firstOrNull { it.isPrimary }
            ?: error("Expected primary constructor in NavigationConfigBuilder. Ensure quo-vadis-core version is compatible with compiler plugin.")
    }

    val registerTabsContainerFun by lazy {
        navigationConfigBuilderClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == "registerTabsContainer" }
            ?: error("Expected function 'registerTabsContainer' in NavigationConfigBuilder. Ensure quo-vadis-core version is compatible with compiler plugin.")
    }

    val registerStackContainerFun by lazy {
        navigationConfigBuilderClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == "registerStackContainer" }
            ?: error("Expected function 'registerStackContainer' in NavigationConfigBuilder. Ensure quo-vadis-core version is compatible with compiler plugin.")
    }

    val registerScopeFun by lazy {
        navigationConfigBuilderClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == "registerScope" }
            ?: error("Expected function 'registerScope' in NavigationConfigBuilder. Ensure quo-vadis-core version is compatible with compiler plugin.")
    }

    val registerPaneContainerFun by lazy {
        navigationConfigBuilderClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == "registerPaneContainer" }
            ?: error("Expected function 'registerPaneContainer' in NavigationConfigBuilder. Ensure quo-vadis-core version is compatible with compiler plugin.")
    }

    val registerTransitionFun by lazy {
        navigationConfigBuilderClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == "registerTransition" }
            ?: error("Expected function 'registerTransition' in NavigationConfigBuilder. Ensure quo-vadis-core version is compatible with compiler plugin.")
    }

    val configBuilderBuildFun by lazy {
        navigationConfigBuilderClass.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .firstOrNull { it.name.asString() == "build" }
            ?: error("Expected function 'build' in NavigationConfigBuilder. Ensure quo-vadis-core version is compatible with compiler plugin.")
    }
    // endregion

    // region Transition Types
    val navTransitionClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.compose.transition", "NavTransition")
    }
    // endregion

    // region Internal Types
    val compositeNavigationConfigClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.navigation.internal.config", "CompositeNavigationConfig")
    }
    // endregion

    // region Compose Types (resolved lazily, may not be available in all compilations)
    val composableAnnotation: IrClassSymbol? by lazy {
        try {
            resolveClass("androidx.compose.runtime", "Composable")
        } catch (_: IllegalStateException) {
            null
        }
    }

    val tabsContainerScopeClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.compose.scope", "TabsContainerScope")
    }

    val paneContainerScopeClass by lazy {
        resolveClass("com.jermey.quo.vadis.core.compose.scope", "PaneContainerScope")
    }
    // endregion

    // region Kotlin Standard Library Types
    val kClassClass by lazy {
        resolveClass(ClassId(FqName("kotlin.reflect"), Name.identifier("KClass")))
    }

    val lazyClass by lazy {
        resolveClass(ClassId(FqName("kotlin"), Name.identifier("Lazy")))
    }

    val lazyFunctions by lazy {
        resolveFunctions("kotlin", "lazy")
    }

    val setOfFunctions by lazy {
        resolveFunctions("kotlin.collections", "setOf")
    }

    val listOfFunctions by lazy {
        resolveFunctions("kotlin.collections", "listOf")
    }

    val regexClass by lazy {
        resolveClass(ClassId(FqName("kotlin.text"), Name.identifier("Regex")))
    }
    // endregion
}
