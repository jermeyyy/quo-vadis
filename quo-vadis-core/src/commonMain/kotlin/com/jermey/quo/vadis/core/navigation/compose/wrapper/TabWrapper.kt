package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.runtime.Composable

/**
 * Type alias for tab wrapper composables.
 *
 * A [TabWrapper] is a user-provided composable that wraps the tab content,
 * providing the overall structure (scaffold, app bar, navigation bar, etc.)
 * while delegating the actual tab content rendering to the library.
 *
 * ## Contract
 *
 * - The wrapper receives a [TabWrapperScope] as receiver for accessing tab state
 * - The wrapper receives `tabContent` lambda that MUST be called exactly once
 * - The wrapper is responsible for positioning the content appropriately
 * - The library handles tab content switching and animations
 *
 * ## Example
 *
 * ```kotlin
 * val myTabWrapper: TabWrapper = { tabContent ->
 *     Scaffold(
 *         topBar = {
 *             TopAppBar(title = { Text("My App") })
 *         },
 *         bottomBar = {
 *             NavigationBar {
 *                 tabMetadata.forEachIndexed { index, meta ->
 *                     NavigationBarItem(
 *                         selected = activeTabIndex == index,
 *                         onClick = { switchTab(index) },
 *                         icon = { meta.icon?.let { Icon(it, meta.label) } },
 *                         label = { Text(meta.label) }
 *                     )
 *                 }
 *             }
 *         }
 *     ) { padding ->
 *         Box(modifier = Modifier.padding(padding)) {
 *             tabContent() // Library renders active tab content here
 *         }
 *     }
 * }
 * ```
 *
 * ## Integration with TabNode
 *
 * ```kotlin
 * TabNode(
 *     tabs = listOf(homeTab, searchTab, profileTab),
 *     wrapper = myTabWrapper
 * )
 * ```
 *
 * @see TabWrapperScope
 * @see DefaultTabWrapper
 */
public typealias TabWrapper = @Composable TabWrapperScope.(
    tabContent: @Composable () -> Unit
) -> Unit
