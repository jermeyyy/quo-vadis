package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.models.WrapperInfo
import com.jermey.quo.vadis.ksp.models.WrapperType
import com.squareup.kotlinpoet.CodeBlock

/**
 * Result of wrapper block generation containing code and imports.
 *
 * @property codeBlock The generated wrapper registration code
 * @property imports Set of fully qualified function names that need to be imported
 */
data class WrapperBlockResult(
    val codeBlock: CodeBlock,
    val imports: Set<String>
)

/**
 * Generates `tabWrapper` and `paneWrapper` DSL blocks.
 *
 * This generator transforms [WrapperInfo] data into KotlinPoet [CodeBlock]s
 * representing wrapper registration DSL calls within the `navigationConfig { }` block.
 *
 * Wrappers provide custom chrome/UI around container content, such as:
 * - Tab bars / Navigation bars / Navigation rails
 * - Adaptive pane layouts
 * - Custom container decorations
 *
 * ## Input
 *
 * List of [WrapperInfo] from WrapperExtractor containing:
 * - Function declaration for the wrapper composable
 * - Function name
 * - Target class (the container being wrapped)
 * - Wrapper type (TAB or PANE)
 *
 * ## Output
 *
 * ```kotlin
 * tabWrapper("com.example.MainTabs") {
 *     MainTabsWrapper()
 * }
 *
 * paneWrapper("com.example.MasterDetail") {
 *     MasterDetailWrapper()
 * }
 * ```
 *
 * ## Wrapper Scope
 *
 * Generated wrapper blocks execute within a scope that provides:
 *
 * ### TabWrapperScope
 * - `tabs` - List of tab metadata
 * - `activeTabIndex` - Currently selected tab index
 * - `switchTab(index)` - Function to switch tabs
 *
 * ### PaneWrapperScope
 * - Pane layout utilities
 * - Adaptive layout helpers
 *
 * @property logger KSP logger for debugging output
 */
class WrapperBlockGenerator(
    private val logger: KSPLogger
) {

    /**
     * Generates wrapper registration blocks.
     *
     * @param wrappers List of wrapper info from extractor
     * @return CodeBlock containing all wrapper definitions
     */
    fun generate(wrappers: List<WrapperInfo>): CodeBlock {
        return generateWithImports(wrappers).codeBlock
    }

    /**
     * Generates wrapper registration blocks with import information.
     *
     * @param wrappers List of wrapper info from extractor
     * @return [WrapperBlockResult] containing code and required imports
     */
    fun generateWithImports(wrappers: List<WrapperInfo>): WrapperBlockResult {
        if (wrappers.isEmpty()) {
            logger.info("WrapperBlockGenerator: No wrappers to generate")
            return WrapperBlockResult(CodeBlock.of(""), emptySet())
        }

        val tabWrappers = wrappers.filter { it.wrapperType == WrapperType.TAB }
        val paneWrappers = wrappers.filter { it.wrapperType == WrapperType.PANE }

        logger.info("WrapperBlockGenerator: Generating ${wrappers.size} wrapper blocks " +
            "(${tabWrappers.size} tab, ${paneWrappers.size} pane)")

        val builder = CodeBlock.builder()
        val imports = mutableSetOf<String>()

        // Generate tab wrappers
        tabWrappers.forEachIndexed { index, wrapper ->
            builder.add(generateTabWrapperBlock(wrapper))
            if (index < tabWrappers.size - 1 || paneWrappers.isNotEmpty()) {
                builder.add("\n")
            }
            // Collect import for the wrapper function
            val qualifiedName = "${wrapper.packageName}.${wrapper.functionName}"
            imports.add(qualifiedName)
        }

        // Generate pane wrappers
        paneWrappers.forEachIndexed { index, wrapper ->
            builder.add(generatePaneWrapperBlock(wrapper))
            if (index < paneWrappers.size - 1) {
                builder.add("\n")
            }
            // Collect import for the wrapper function
            val qualifiedName = "${wrapper.packageName}.${wrapper.functionName}"
            imports.add(qualifiedName)
        }

        return WrapperBlockResult(builder.build(), imports)
    }

    /**
     * Generates a `tabWrapper` block.
     *
     * The wrapper key is the qualified name of the target class, which allows
     * the wrapper to be matched to its container at runtime.
     *
     * @param wrapper The wrapper info to generate code for
     * @return CodeBlock for the tab wrapper registration
     */
    private fun generateTabWrapperBlock(wrapper: WrapperInfo): CodeBlock {
        val wrapperKey = wrapper.targetClassQualifiedName

        return CodeBlock.builder()
            .beginControlFlow("tabWrapper(%S)", wrapperKey)
            .add(generateWrapperContent(wrapper))
            .endControlFlow()
            .build()
    }

    /**
     * Generates a `paneWrapper` block.
     *
     * The wrapper key is the qualified name of the target class, which allows
     * the wrapper to be matched to its container at runtime.
     *
     * @param wrapper The wrapper info to generate code for
     * @return CodeBlock for the pane wrapper registration
     */
    private fun generatePaneWrapperBlock(wrapper: WrapperInfo): CodeBlock {
        val wrapperKey = wrapper.targetClassQualifiedName

        return CodeBlock.builder()
            .beginControlFlow("paneWrapper(%S)", wrapperKey)
            .add(generateWrapperContent(wrapper))
            .endControlFlow()
            .build()
    }

    /**
     * Generates the wrapper content (the composable function call).
     *
     * The tabWrapper DSL provides a lambda with signature:
     * `@Composable TabWrapperScope.(@Composable () -> Unit) -> Unit`
     *
     * This means:
     * - `this` is TabWrapperScope (or PaneWrapperScope for pane wrappers)
     * - The single parameter is the content composable
     *
     * So we generate:
     * ```kotlin
     * WrapperFunction(scope = this, content = it)
     * ```
     *
     * @param wrapper The wrapper info
     * @return CodeBlock for the wrapper content
     */
    private fun generateWrapperContent(wrapper: WrapperInfo): CodeBlock {
        // The wrapper function expects (scope, content) parameters
        // In the DSL lambda, 'this' is the scope and 'it' is the content
        return CodeBlock.of("%L(scope = this, content = it)\n", wrapper.functionName)
    }
}
