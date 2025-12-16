package com.jermey.quo.vadis.core.navigation.dsl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.compose.registry.WrapperRegistry
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope

/**
 * DSL-based implementation of [WrapperRegistry] that provides custom
 * wrapper composables for tab and pane containers.
 *
 * This registry is created by [DslNavigationConfig] from wrapper
 * registrations collected by [NavigationConfigBuilder].
 *
 * ## Purpose
 *
 * Wrappers allow customization of the chrome/UI surrounding navigation content:
 * - **Tab wrappers**: Custom tab bars, bottom navigation, navigation rails
 * - **Pane wrappers**: Custom multi-pane layouts, master-detail arrangements
 *
 * ## Usage
 *
 * Wrappers are registered via the DSL:
 *
 * ```kotlin
 * val config = navigationConfig {
 *     tabWrapper("main-tabs") { content ->
 *         Scaffold(
 *             bottomBar = {
 *                 NavigationBar {
 *                     tabMetadata.forEachIndexed { index, meta ->
 *                         NavigationBarItem(
 *                             selected = activeTabIndex == index,
 *                             onClick = { switchTab(index) },
 *                             icon = { Icon(meta.icon, meta.label) },
 *                             label = { Text(meta.label) }
 *                         )
 *                     }
 *                 }
 *             }
 *         ) { padding ->
 *             Box(Modifier.padding(padding)) {
 *                 content()
 *             }
 *         }
 *     }
 *
 *     paneWrapper("list-detail") { content ->
 *         Row(Modifier.fillMaxSize()) {
 *             content()
 *         }
 *     }
 * }
 * ```
 *
 * ## Default Behavior
 *
 * When no custom wrapper is registered for a key, the content is rendered
 * directly without any wrapper chrome.
 *
 * @param tabWrappers Map of wrapper keys to tab wrapper composables
 * @param paneWrappers Map of wrapper keys to pane wrapper composables
 *
 * @see WrapperRegistry
 * @see TabWrapperScope
 * @see PaneWrapperScope
 * @see NavigationConfigBuilder.tabWrapper
 * @see NavigationConfigBuilder.paneWrapper
 */
@Stable
internal class DslWrapperRegistry(
    private val tabWrappers: Map<String, @Composable TabWrapperScope.(@Composable () -> Unit) -> Unit>,
    private val paneWrappers: Map<String, @Composable PaneWrapperScope.(@Composable () -> Unit) -> Unit>
) : WrapperRegistry {

    /**
     * Renders the tab wrapper for the given tab node.
     *
     * If a custom wrapper is registered for [tabNodeKey], it is invoked with
     * the provided [scope] and [content]. Otherwise, the content is rendered
     * directly without any wrapper.
     *
     * @param tabNodeKey Unique identifier for the tab node
     * @param scope Scope providing tab state and navigation actions
     * @param content Composable lambda that renders the actual tab content
     */
    @Composable
    override fun TabWrapper(
        tabNodeKey: String,
        scope: TabWrapperScope,
        content: @Composable () -> Unit
    ) {
        val wrapper = tabWrappers[tabNodeKey]
        if (wrapper != null) {
            wrapper.invoke(scope, content)
        } else {
            // Default: render content directly without wrapper
            content()
        }
    }

    /**
     * Renders the pane wrapper for the given pane node.
     *
     * If a custom wrapper is registered for [paneNodeKey], it is invoked with
     * the provided [scope] and [content]. Otherwise, the content is rendered
     * directly without any wrapper.
     *
     * @param paneNodeKey Unique identifier for the pane node
     * @param scope Scope providing pane state and layout information
     * @param content Composable lambda that renders the pane content
     */
    @Composable
    override fun PaneWrapper(
        paneNodeKey: String,
        scope: PaneWrapperScope,
        content: @Composable () -> Unit
    ) {
        val wrapper = paneWrappers[paneNodeKey]
        if (wrapper != null) {
            wrapper.invoke(scope, content)
        } else {
            // Default: render content directly without wrapper
            content()
        }
    }

    /**
     * Checks whether a custom tab wrapper is registered for the given key.
     *
     * @param tabNodeKey Unique identifier for the tab node
     * @return `true` if a custom wrapper is registered, `false` if default will be used
     */
    override fun hasTabWrapper(tabNodeKey: String): Boolean {
        return tabWrappers.containsKey(tabNodeKey)
    }

    /**
     * Checks whether a custom pane wrapper is registered for the given key.
     *
     * @param paneNodeKey Unique identifier for the pane node
     * @return `true` if a custom wrapper is registered, `false` if default will be used
     */
    override fun hasPaneWrapper(paneNodeKey: String): Boolean {
        return paneWrappers.containsKey(paneNodeKey)
    }
}
