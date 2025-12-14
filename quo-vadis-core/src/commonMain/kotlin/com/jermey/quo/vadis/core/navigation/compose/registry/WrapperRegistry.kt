package com.jermey.quo.vadis.core.navigation.compose.registry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope

/**
 * Registry interface for KSP-generated wrapper composables.
 *
 * This interface provides a lookup mechanism for custom wrapper composables
 * that are registered via `@TabWrapper` and `@PaneWrapper` annotations. The
 * KSP processor generates an implementation of this interface containing all
 * discovered wrappers.
 *
 * ## Purpose
 *
 * Wrappers allow users to customize the chrome/UI surrounding navigation content:
 * - **Tab wrappers**: Custom tab bars, bottom navigation, navigation rails
 * - **Pane wrappers**: Custom multi-pane layouts, master-detail arrangements
 *
 * ## KSP Generation
 *
 * The KSP processor scans for annotated functions:
 *
 * ```kotlin
 * @TabWrapper(MyTabsDestination::class)
 * @Composable
 * fun MyTabWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) {
 *     Scaffold(
 *         bottomBar = { CustomBottomBar(scope) }
 *     ) { padding ->
 *         Box(Modifier.padding(padding)) { content() }
 *     }
 * }
 * ```
 *
 * And generates:
 *
 * ```kotlin
 * object GeneratedWrapperRegistry : WrapperRegistry {
 *     @Composable
 *     override fun TabWrapper(
 *         tabNodeKey: String,
 *         scope: TabWrapperScope,
 *         content: @Composable () -> Unit
 *     ) {
 *         when (tabNodeKey) {
 *             "MyTabsDestination" -> MyTabWrapper(scope, content)
 *             else -> DefaultTabWrapper(scope, content)
 *         }
 *     }
 *     // ... similar for PaneWrapper
 * }
 * ```
 *
 * ## Thread Safety
 *
 * Implementations are marked [Stable], indicating they can be safely read
 * during composition without triggering unnecessary recompositions.
 *
 * @see TabWrapperScope
 * @see PaneWrapperScope
 */
@Stable
public interface WrapperRegistry {

    /**
     * Renders the tab wrapper for the given tab node.
     *
     * If a custom wrapper is registered for [tabNodeKey], it is invoked with
     * the provided [scope] and [content]. Otherwise, the default wrapper
     * is used, which simply renders [content] directly.
     *
     * ## Wrapper Contract
     *
     * Wrappers **must** invoke [content] exactly once to render the tab content.
     * The wrapper is responsible for:
     * - Rendering navigation UI (tab bar, bottom navigation, etc.)
     * - Positioning the content appropriately
     * - Forwarding any necessary modifiers or padding
     *
     * ## Example
     *
     * ```kotlin
     * wrapperRegistry.TabWrapper(
     *     tabNodeKey = tabNode.key,
     *     scope = tabWrapperScope
     * ) {
     *     // Tab content rendered here
     *     AnimatedNavContent(activeTab) { tab ->
     *         NavTreeRenderer(tab, scope)
     *     }
     * }
     * ```
     *
     * @param tabNodeKey Unique identifier for the tab node (typically destination class name)
     * @param scope Scope providing tab state and navigation actions
     * @param content Composable lambda that renders the actual tab content
     */
    @Composable
    public fun TabWrapper(
        tabNodeKey: String,
        scope: TabWrapperScope,
        content: @Composable () -> Unit
    )

    /**
     * Renders the pane wrapper for the given pane node.
     *
     * If a custom wrapper is registered for [paneNodeKey], it is invoked with
     * the provided [scope] and [content]. Otherwise, the default wrapper
     * is used, which implements a basic split-pane layout.
     *
     * ## Wrapper Contract
     *
     * Wrappers **must** invoke [content] exactly once to render the pane content.
     * The wrapper is responsible for:
     * - Determining pane layout based on screen size
     * - Positioning multiple panes (in expanded mode)
     * - Handling pane visibility and transitions
     *
     * ## Example
     *
     * ```kotlin
     * wrapperRegistry.PaneWrapper(
     *     paneNodeKey = paneNode.key,
     *     scope = paneWrapperScope
     * ) {
     *     // Pane content rendered here
     *     Row {
     *         PrimaryPane()
     *         if (scope.isExpanded) {
     *             DetailPane()
     *         }
     *     }
     * }
     * ```
     *
     * @param paneNodeKey Unique identifier for the pane node (typically destination class name)
     * @param scope Scope providing pane state and layout information
     * @param content Composable lambda that renders the pane content
     */
    @Composable
    public fun PaneWrapper(
        paneNodeKey: String,
        scope: PaneWrapperScope,
        content: @Composable () -> Unit
    )

    /**
     * Checks whether a custom tab wrapper is registered for the given key.
     *
     * This method allows renderers to optimize by skipping wrapper invocation
     * when no custom wrapper exists, potentially reducing composition overhead.
     *
     * @param tabNodeKey Unique identifier for the tab node
     * @return `true` if a custom wrapper is registered, `false` if default will be used
     */
    public fun hasTabWrapper(tabNodeKey: String): Boolean

    /**
     * Checks whether a custom pane wrapper is registered for the given key.
     *
     * This method allows renderers to optimize by skipping wrapper invocation
     * when no custom wrapper exists, potentially reducing composition overhead.
     *
     * @param paneNodeKey Unique identifier for the pane node
     * @return `true` if a custom wrapper is registered, `false` if default will be used
     */
    public fun hasPaneWrapper(paneNodeKey: String): Boolean

    /**
     * Companion object providing a default empty implementation.
     */
    public companion object {

        /**
         * An empty [WrapperRegistry] that provides default behavior for all wrappers.
         *
         * ## Default Behavior
         *
         * - [TabWrapper]: Renders [content] directly without any wrapper UI
         * - [PaneWrapper]: Renders [content] directly without any wrapper UI
         * - [hasTabWrapper]: Always returns `false`
         * - [hasPaneWrapper]: Always returns `false`
         *
         * ## Usage
         *
         * Use this when no custom wrappers are registered or during testing:
         *
         * ```kotlin
         * val scope = NavRenderScopeImpl(
         *     // ...
         *     wrapperRegistry = WrapperRegistry.Empty
         * )
         * ```
         */
        public val Empty: WrapperRegistry = object : WrapperRegistry {

            @Composable
            override fun TabWrapper(
                tabNodeKey: String,
                scope: TabWrapperScope,
                content: @Composable () -> Unit
            ) {
                // Default: render content directly without wrapper
                content()
            }

            @Composable
            override fun PaneWrapper(
                paneNodeKey: String,
                scope: PaneWrapperScope,
                content: @Composable () -> Unit
            ) {
                // Default: render content directly without wrapper
                content()
            }

            override fun hasTabWrapper(tabNodeKey: String): Boolean = false

            override fun hasPaneWrapper(paneNodeKey: String): Boolean = false
        }
    }
}
