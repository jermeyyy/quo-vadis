package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a Composable function as a tab wrapper for the specified tab destination class.
 *
 * Tab wrappers provide the surrounding UI chrome (tab bar, navigation rail, etc.) for
 * tabbed navigation containers. The wrapper receives a `TabWrapperScope` that provides
 * access to tab state and navigation, and a content slot where the active tab's content
 * is rendered.
 *
 * ## Function Signature Requirements
 *
 * The annotated function must follow this signature:
 * ```kotlin
 * @TabWrapper(MainTabs::class)
 * @Composable
 * fun MainTabsWrapper(scope: TabWrapperScope, content: @Composable () -> Unit) {
 *     // Wrapper implementation
 * }
 * ```
 *
 * Parameters (detected by KSP based on types):
 * 1. `scope: TabWrapperScope` - Provides access to tab state and navigation
 * 2. `content: @Composable () -> Unit` - The active tab's content to render
 *
 * ## TabWrapperScope
 *
 * The scope provides:
 * - `navigator` - The Navigator instance for navigation operations
 * - `activeIndex` - The currently selected tab index
 * - `tabs` - List of TabMetadata for all tabs (label, icon)
 * - `switchTab(index)` - Function to switch to a different tab
 *
 * ## Example: Bottom Navigation Wrapper
 *
 * ```kotlin
 * @TabWrapper(MainTabs::class)
 * @Composable
 * fun MainTabsBottomNavWrapper(
 *     scope: TabWrapperScope,
 *     content: @Composable () -> Unit
 * ) {
 *     Scaffold(
 *         bottomBar = {
 *             NavigationBar {
 *                 scope.tabs.forEachIndexed { index, tab ->
 *                     NavigationBarItem(
 *                         selected = index == scope.activeIndex,
 *                         onClick = { scope.switchTab(index) },
 *                         icon = { Icon(tabIcon(tab.icon), contentDescription = tab.label) },
 *                         label = { Text(tab.label) }
 *                     )
 *                 }
 *             }
 *         }
 *     ) { padding ->
 *         Box(Modifier.padding(padding)) {
 *             content()
 *         }
 *     }
 * }
 * ```
 *
 * ## Example: Navigation Rail Wrapper
 *
 * ```kotlin
 * @TabWrapper(MainTabs::class)
 * @Composable
 * fun MainTabsRailWrapper(
 *     scope: TabWrapperScope,
 *     content: @Composable () -> Unit
 * ) {
 *     Row {
 *         NavigationRail {
 *             scope.tabs.forEachIndexed { index, tab ->
 *                 NavigationRailItem(
 *                     selected = index == scope.activeIndex,
 *                     onClick = { scope.switchTab(index) },
 *                     icon = { Icon(tabIcon(tab.icon), contentDescription = tab.label) },
 *                     label = { Text(tab.label) }
 *                 )
 *             }
 *         }
 *         Box(Modifier.weight(1f)) {
 *             content()
 *         }
 *     }
 * }
 * ```
 *
 * ## KSP Processing
 *
 * KSP generates entries in `GeneratedWrapperRegistry` mapping each tab class
 * to its wrapper function. The registry is used by the hierarchical renderer
 * to resolve which wrapper to use for each `TabNode`.
 *
 * ## Hierarchical Rendering
 *
 * With the hierarchical rendering engine, the wrapper and its content are
 * composed as a parent-child relationship (not siblings), enabling:
 * - Coordinated animations during tab switches
 * - Unified predictive back gesture handling
 * - Proper state preservation across tab changes
 *
 * @property tabClass The tab container class this wrapper wraps.
 *   Must be a class annotated with [@Tab].
 *
 * @see Tab
 * @see TabItem
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class TabWrapper(
    /**
     * The tab container class this wrapper wraps.
     * Must be a class annotated with @Tab.
     */
    val tabClass: KClass<*>
)
