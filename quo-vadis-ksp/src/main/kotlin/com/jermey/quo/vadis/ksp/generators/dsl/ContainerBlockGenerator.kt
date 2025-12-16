package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.PaneItemInfo
import com.jermey.quo.vadis.ksp.models.PaneRole
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TabItemInfo
import com.jermey.quo.vadis.ksp.models.TabItemType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Generates container DSL blocks (tabs, stack, panes).
 *
 * This generator transforms container metadata ([TabInfo], [StackInfo], [PaneInfo])
 * into KotlinPoet [CodeBlock]s representing container registration DSL calls.
 *
 * ## Supported Container Types
 *
 * ### Tabs
 * ```kotlin
 * tabs<MainTabs>(scopeKey = "MainTabs") {
 *     initialTab = 0
 *     tab(MainTabs.HomeTab, title = "Home", icon = "home")
 *     tab(MainTabs.ExploreTab, title = "Explore") {
 *         screen(ExploreDestination.List)
 *     }
 * }
 * ```
 *
 * ### Stack
 * ```kotlin
 * stack<ProfileStack>(scopeKey = "ProfileStack") {
 *     screen(ProfileDestination.Main)
 *     screen(ProfileDestination.Settings)
 * }
 * ```
 *
 * ### Panes
 * ```kotlin
 * panes<MasterDetail>(scopeKey = "MasterDetail") {
 *     initialPane = PaneRole.Primary
 *     backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
 *     primary { root(MasterDetail.List) }
 *     secondary { root(MasterDetail.Detail) }
 * }
 * ```
 *
 * @property logger KSP logger for debugging output
 */
class ContainerBlockGenerator(
    private val logger: KSPLogger
) {

    /**
     * Generates all container blocks.
     *
     * @param tabs List of tab container info
     * @param stacks List of stack container info
     * @param panes List of pane container info
     * @return CodeBlock containing all container definitions
     */
    fun generate(
        tabs: List<TabInfo>,
        stacks: List<StackInfo>,
        panes: List<PaneInfo>
    ): CodeBlock {
        val totalContainers = tabs.size + stacks.size + panes.size
        if (totalContainers == 0) {
            logger.info("ContainerBlockGenerator: No containers to generate")
            return CodeBlock.of("")
        }

        logger.info("ContainerBlockGenerator: Generating $totalContainers containers " +
            "(${tabs.size} tabs, ${stacks.size} stacks, ${panes.size} panes)")

        val builder = CodeBlock.builder()

        // Generate tabs
        tabs.forEachIndexed { index, tab ->
            builder.add(generateTabsBlock(tab))
            if (index < tabs.size - 1 || stacks.isNotEmpty() || panes.isNotEmpty()) {
                builder.add("\n")
            }
        }

        // Generate stacks
        stacks.forEachIndexed { index, stack ->
            builder.add(generateStackBlock(stack))
            if (index < stacks.size - 1 || panes.isNotEmpty()) {
                builder.add("\n")
            }
        }

        // Generate panes
        panes.forEachIndexed { index, pane ->
            builder.add(generatePanesBlock(pane))
            if (index < panes.size - 1) {
                builder.add("\n")
            }
        }

        return builder.build()
    }

    /**
     * Generates a `tabs<T>` block.
     *
     * @param tab The tab info to generate code for
     * @return CodeBlock for the tabs container
     */
    private fun generateTabsBlock(tab: TabInfo): CodeBlock {
        val containerClass = tab.classDeclaration.toClassName()
        val scopeKey = tab.name.ifEmpty { tab.className }

        val builder = CodeBlock.builder()
            .beginControlFlow("tabs<%T>(scopeKey = %S)", containerClass, scopeKey)

        // Add initialTab if specified
        val initialTabIndex = findInitialTabIndex(tab)
        if (initialTabIndex != 0) {
            builder.addStatement("initialTab = %L", initialTabIndex)
        }

        // Generate tab entries
        tab.tabs.forEachIndexed { index, tabItem ->
            builder.add(generateTabEntry(tabItem, tab, index))
        }

        builder.endControlFlow()

        return builder.build()
    }

    /**
     * Finds the initial tab index from TabInfo.
     *
     * @param tab The tab info
     * @return Index of the initial tab, or 0 if not specified
     */
    private fun findInitialTabIndex(tab: TabInfo): Int {
        if (tab.initialTabClass == null) return 0

        return tab.tabs.indexOfFirst { tabItem ->
            tabItem.classDeclaration.qualifiedName?.asString() ==
                tab.initialTabClass.qualifiedName?.asString()
        }.coerceAtLeast(0)
    }

    /**
     * Generates a single tab entry.
     *
     * For tab items that are data objects, uses `tab(Object, ...)` syntax.
     * For tab items that are classes (e.g., sealed classes with nested stacks),
     * uses `containerTab<Type>(...)` syntax to reference the separately defined stack.
     *
     * @param tabItem The tab item info
     * @param parentTab The parent tab container
     * @param index Index of this tab
     * @return CodeBlock for the tab entry
     */
    private fun generateTabEntry(
        tabItem: TabItemInfo,
        parentTab: TabInfo,
        @Suppress("UNUSED_PARAMETER") index: Int
    ): CodeBlock {
        val tabClassName = tabItem.classDeclaration.toClassName()
        val isObject = tabItem.classDeclaration.classKind == ClassKind.OBJECT

        return when (tabItem.tabType) {
            TabItemType.NESTED_STACK -> {
                // Tab with nested stack - use containerTab<Type>() since the stack is defined separately
                val params = buildTabParamsForContainerTab(tabItem)
                CodeBlock.of("containerTab<%T>($params)\n", tabClassName)
            }
            TabItemType.FLAT_SCREEN -> {
                // Simple flat screen tab
                if (isObject) {
                    // For data objects, use the object reference directly
                    val params = buildTabParams(tabItem)
                    CodeBlock.of("tab(%T$params)\n", tabClassName)
                } else {
                    // For classes, use containerTab (shouldn't happen for FLAT_SCREEN, but handle gracefully)
                    val params = buildTabParamsForContainerTab(tabItem)
                    CodeBlock.of("containerTab<%T>($params)\n", tabClassName)
                }
            }
        }
    }

    /**
     * Builds tab parameters string (title, icon) for tab() calls.
     *
     * @param tabItem The tab item info
     * @return Parameters string (e.g., ", title = \"Home\", icon = \"home\"")
     */
    private fun buildTabParams(tabItem: TabItemInfo): String {
        val params = mutableListOf<String>()

        if (tabItem.label.isNotEmpty()) {
            params.add("title = \"${tabItem.label}\"")
        }
        if (tabItem.icon.isNotEmpty()) {
            params.add("icon = \"${tabItem.icon}\"")
        }

        return if (params.isNotEmpty()) ", ${params.joinToString(", ")}" else ""
    }

    /**
     * Builds tab parameters string for containerTab() calls.
     * Uses named parameters without leading comma.
     *
     * @param tabItem The tab item info
     * @return Parameters string (e.g., "title = \"Home\", icon = \"home\"")
     */
    private fun buildTabParamsForContainerTab(tabItem: TabItemInfo): String {
        val params = mutableListOf<String>()

        if (tabItem.label.isNotEmpty()) {
            params.add("title = \"${tabItem.label}\"")
        }
        if (tabItem.icon.isNotEmpty()) {
            params.add("icon = \"${tabItem.icon}\"")
        }

        return params.joinToString(", ")
    }

    /**
     * Generates a `stack<T>` block.
     *
     * Uses the reified type version `screen<Type>()` for all destinations.
     *
     * @param stack The stack info to generate code for
     * @return CodeBlock for the stack container
     */
    private fun generateStackBlock(stack: StackInfo): CodeBlock {
        val containerClass = stack.classDeclaration.toClassName()
        val scopeKey = stack.name.ifEmpty { stack.className }

        val builder = CodeBlock.builder()
            .beginControlFlow("stack<%T>(scopeKey = %S)", containerClass, scopeKey)

        // Generate screen entries for each destination using reified type syntax
        stack.destinations.forEach { dest ->
            val destClass = dest.classDeclaration.toClassName()
            builder.addStatement("screen<%T>()", destClass)
        }

        builder.endControlFlow()

        return builder.build()
    }

    /**
     * Generates a `panes<T>` block.
     *
     * @param pane The pane info to generate code for
     * @return CodeBlock for the panes container
     */
    private fun generatePanesBlock(pane: PaneInfo): CodeBlock {
        val containerClass = pane.classDeclaration.toClassName()
        val scopeKey = pane.name.ifEmpty { pane.className }

        val builder = CodeBlock.builder()
            .beginControlFlow("panes<%T>(scopeKey = %S)", containerClass, scopeKey)

        // Add back behavior if not default
        if (pane.backBehavior != com.jermey.quo.vadis.ksp.models.PaneBackBehavior.PopUntilScaffoldValueChange) {
            builder.addStatement("backBehavior = PaneBackBehavior.%L", pane.backBehavior.name)
        }

        // Add initial pane if we have panes
        val primaryPane = pane.panes.find { it.role == PaneRole.PRIMARY }
        if (primaryPane == null && pane.panes.isNotEmpty()) {
            val firstPane = pane.panes.first()
            if (firstPane.role != PaneRole.PRIMARY) {
                builder.addStatement("initialPane = PaneRole.%L", firstPane.role.name)
            }
        }

        // Generate pane entries
        pane.panes.forEach { paneItem ->
            builder.add(generatePaneEntry(paneItem))
        }

        builder.endControlFlow()

        return builder.build()
    }

    /**
     * Generates a pane entry (primary, secondary, extra).
     *
     * Uses object reference for data objects, otherwise uses reified type.
     *
     * @param paneItem The pane item info
     * @return CodeBlock for the pane entry
     */
    private fun generatePaneEntry(paneItem: PaneItemInfo): CodeBlock {
        val roleName = when (paneItem.role) {
            PaneRole.PRIMARY -> "primary"
            PaneRole.SECONDARY -> "secondary"
            PaneRole.EXTRA -> "extra"
        }

        val destClass = paneItem.destination.classDeclaration.toClassName()
        val isObject = paneItem.destination.isDataObject

        return CodeBlock.builder()
            .beginControlFlow(roleName)
            .apply {
                if (isObject) {
                    // For data objects, use the object reference
                    addStatement("root(%T)", destClass)
                } else {
                    // For data classes, we can't instantiate without parameters
                    // This is a limitation - panes typically need data objects as roots
                    // Add a comment to help the user understand the issue
                    addStatement("// TODO: %T is a data class, not an object. Pane roots must be data objects.", destClass)
                    addStatement("// root(%T)", destClass)
                }
            }
            .endControlFlow()
            .build()
    }
}
