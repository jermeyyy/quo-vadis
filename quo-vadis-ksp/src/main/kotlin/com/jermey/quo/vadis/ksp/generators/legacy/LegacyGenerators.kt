package com.jermey.quo.vadis.ksp.generators.legacy

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.generators.ContainerRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.DeepLinkHandlerGenerator
import com.jermey.quo.vadis.ksp.generators.NavNodeBuilderGenerator
import com.jermey.quo.vadis.ksp.generators.ScopeRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.ScreenRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.TransitionRegistryGenerator
import com.jermey.quo.vadis.ksp.generators.WrapperRegistryGenerator
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TransitionInfo
import com.jermey.quo.vadis.ksp.models.WrapperInfo

/**
 * Orchestrates all legacy generators for backward compatibility.
 *
 * This class provides a single entry point for generating legacy code during
 * the transition period while the new DSL-based `GeneratedNavigationConfig`
 * is being adopted. When `addDeprecations` is true, all generated code will
 * include `@Deprecated` annotations pointing users to the new API.
 *
 * ## Usage
 *
 * ```kotlin
 * val legacyGenerators = LegacyGenerators(codeGenerator, logger)
 * legacyGenerators.generate(
 *     screens = screenInfos,
 *     tabInfos = tabInfos,
 *     paneInfos = paneInfos,
 *     stackInfos = stackInfos,
 *     transitions = transitionInfos,
 *     tabWrappers = tabWrapperInfos,
 *     paneWrappers = paneWrapperInfos,
 *     destinations = destinationInfos,
 *     addDeprecations = true // Mark all generated code as deprecated
 * )
 * ```
 *
 * ## Generation Flow
 *
 * 1. **ScreenRegistryGenerator** - Generates `GeneratedScreenRegistry`
 * 2. **ContainerRegistryGenerator** - Generates `GeneratedContainerRegistry`
 * 3. **ScopeRegistryGenerator** - Generates `GeneratedScopeRegistry`
 * 4. **TransitionRegistryGenerator** - Generates `GeneratedTransitionRegistry`
 * 5. **WrapperRegistryGenerator** - Generates `GeneratedWrapperRegistry`
 * 6. **NavNodeBuilderGenerator** - Generates `build*NavNode()` functions
 * 7. **DeepLinkHandlerGenerator** - Generates `GeneratedDeepLinkHandlerImpl`
 *
 * @property codeGenerator KSP code generator for writing output files
 * @property logger KSP logger for diagnostic output
 */
class LegacyGenerators(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {
    private val screenRegistryGenerator = ScreenRegistryGenerator(codeGenerator, logger)
    private val containerRegistryGenerator = ContainerRegistryGenerator(codeGenerator, logger)
    private val scopeRegistryGenerator = ScopeRegistryGenerator(codeGenerator, logger)
    private val transitionRegistryGenerator = TransitionRegistryGenerator(codeGenerator, logger)
    private val wrapperRegistryGenerator = WrapperRegistryGenerator(codeGenerator, logger)
    private val navNodeBuilderGenerator = NavNodeBuilderGenerator(codeGenerator, logger)
    private val deepLinkHandlerGenerator = DeepLinkHandlerGenerator(codeGenerator, logger)

    /**
     * Generate all legacy code files.
     *
     * This method orchestrates the generation of all legacy registries and builder
     * functions. When `addDeprecations` is true, all generated code will include
     * `@Deprecated` annotations with migration instructions pointing to
     * `GeneratedNavigationConfig`.
     *
     * @param screens List of ScreenInfo models for @Screen annotated functions
     * @param tabInfos List of TabInfo models for @Tab annotated classes
     * @param paneInfos List of PaneInfo models for @Pane annotated classes
     * @param stackInfos List of StackInfo models for @Stack annotated classes
     * @param transitions List of TransitionInfo models for @Transition annotations
     * @param tabWrappers List of WrapperInfo models for @TabWrapper functions
     * @param paneWrappers List of WrapperInfo models for @PaneWrapper functions
     * @param destinations List of DestinationInfo models for @Destination classes
     * @param addDeprecations When true, adds @Deprecated annotations to all generated code
     */
    fun generate(
        screens: List<ScreenInfo> = emptyList(),
        tabInfos: List<TabInfo> = emptyList(),
        paneInfos: List<PaneInfo> = emptyList(),
        stackInfos: List<StackInfo> = emptyList(),
        transitions: List<TransitionInfo> = emptyList(),
        tabWrappers: List<WrapperInfo> = emptyList(),
        paneWrappers: List<WrapperInfo> = emptyList(),
        destinations: List<DestinationInfo> = emptyList(),
        addDeprecations: Boolean = false
    ) {
        logger.info("LegacyGenerators: Starting legacy code generation (addDeprecations=$addDeprecations)")

        // Generate registry objects
        generateScreenRegistry(screens, addDeprecations)
        generateContainerRegistry(tabInfos, paneInfos, addDeprecations)
        generateScopeRegistry(tabInfos, paneInfos, stackInfos, addDeprecations)
        generateTransitionRegistry(transitions, addDeprecations)
        generateWrapperRegistry(tabWrappers, paneWrappers, addDeprecations)

        // Generate NavNode builder functions
        generateNavNodeBuilders(stackInfos, tabInfos, paneInfos, addDeprecations)

        // Generate deep link handler
        generateDeepLinkHandler(destinations)

        logger.info("LegacyGenerators: Completed legacy code generation")
    }

    /**
     * Generate only the registry objects (without builder functions).
     *
     * Use this when you only need the registry implementations for testing
     * or partial generation scenarios.
     */
    fun generateRegistries(
        screens: List<ScreenInfo> = emptyList(),
        tabInfos: List<TabInfo> = emptyList(),
        paneInfos: List<PaneInfo> = emptyList(),
        stackInfos: List<StackInfo> = emptyList(),
        transitions: List<TransitionInfo> = emptyList(),
        tabWrappers: List<WrapperInfo> = emptyList(),
        paneWrappers: List<WrapperInfo> = emptyList(),
        addDeprecations: Boolean = false
    ) {
        generateScreenRegistry(screens, addDeprecations)
        generateContainerRegistry(tabInfos, paneInfos, addDeprecations)
        generateScopeRegistry(tabInfos, paneInfos, stackInfos, addDeprecations)
        generateTransitionRegistry(transitions, addDeprecations)
        generateWrapperRegistry(tabWrappers, paneWrappers, addDeprecations)
    }

    /**
     * Generate only the NavNode builder functions.
     *
     * Use this when you only need the builder functions for testing
     * or partial generation scenarios.
     */
    fun generateBuilders(
        stackInfos: List<StackInfo> = emptyList(),
        tabInfos: List<TabInfo> = emptyList(),
        paneInfos: List<PaneInfo> = emptyList(),
        addDeprecations: Boolean = false
    ) {
        generateNavNodeBuilders(stackInfos, tabInfos, paneInfos, addDeprecations)
    }

    // =========================================================================
    // Private Generation Methods
    // =========================================================================

    private fun generateScreenRegistry(screens: List<ScreenInfo>, addDeprecations: Boolean) {
        if (screens.isNotEmpty()) {
            screenRegistryGenerator.generate(screens, addDeprecations)
        }
    }

    private fun generateContainerRegistry(
        tabInfos: List<TabInfo>,
        paneInfos: List<PaneInfo>,
        addDeprecations: Boolean
    ) {
        if (tabInfos.isNotEmpty() || paneInfos.isNotEmpty()) {
            containerRegistryGenerator.generate(tabInfos, paneInfos, addDeprecations)
        }
    }

    private fun generateScopeRegistry(
        tabInfos: List<TabInfo>,
        paneInfos: List<PaneInfo>,
        stackInfos: List<StackInfo>,
        addDeprecations: Boolean
    ) {
        if (tabInfos.isNotEmpty() || paneInfos.isNotEmpty() || stackInfos.isNotEmpty()) {
            scopeRegistryGenerator.generate(
                tabInfos = tabInfos,
                paneInfos = paneInfos,
                stackInfos = stackInfos,
                addDeprecations = addDeprecations
            )
        }
    }

    private fun generateTransitionRegistry(transitions: List<TransitionInfo>, addDeprecations: Boolean) {
        if (transitions.isNotEmpty()) {
            transitionRegistryGenerator.generate(transitions, addDeprecations = addDeprecations)
        }
    }

    private fun generateWrapperRegistry(
        tabWrappers: List<WrapperInfo>,
        paneWrappers: List<WrapperInfo>,
        addDeprecations: Boolean
    ) {
        if (tabWrappers.isNotEmpty() || paneWrappers.isNotEmpty()) {
            wrapperRegistryGenerator.generate(tabWrappers, paneWrappers, addDeprecations = addDeprecations)
        }
    }

    private fun generateNavNodeBuilders(
        stackInfos: List<StackInfo>,
        tabInfos: List<TabInfo>,
        paneInfos: List<PaneInfo>,
        addDeprecations: Boolean
    ) {
        // Build stack builder map for tab generation
        val stackBuilders = stackInfos.associateBy { it.className }

        // Generate stack builders first (they may be referenced by tab builders)
        stackInfos.forEach { stackInfo ->
            navNodeBuilderGenerator.generateStackBuilder(stackInfo, addDeprecations)
        }

        // Generate tab builders
        tabInfos.forEach { tabInfo ->
            navNodeBuilderGenerator.generateTabBuilder(tabInfo, stackBuilders, addDeprecations)
        }

        // Generate pane builders
        paneInfos.forEach { paneInfo ->
            navNodeBuilderGenerator.generatePaneBuilder(paneInfo, addDeprecations)
        }
    }

    private fun generateDeepLinkHandler(destinations: List<DestinationInfo>) {
        if (destinations.isNotEmpty()) {
            deepLinkHandlerGenerator.generate(destinations)
        }
    }
}
