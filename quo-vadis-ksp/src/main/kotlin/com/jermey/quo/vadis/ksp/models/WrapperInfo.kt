package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Type of wrapper - distinguishes between tab and pane wrappers.
 */
public enum class WrapperType {
    /**
     * Tab wrapper for [com.jermey.quo.vadis.annotations.TabWrapper].
     */
    TAB,

    /**
     * Pane wrapper for [com.jermey.quo.vadis.annotations.PaneWrapper].
     */
    PANE
}

/**
 * Extracted metadata from a [@TabWrapper][com.jermey.quo.vadis.annotations.TabWrapper]
 * or [@PaneWrapper][com.jermey.quo.vadis.annotations.PaneWrapper] annotation.
 *
 * Wrapper info connects a @Composable wrapper function to a tab/pane class.
 * The wrapper provides the UI chrome (tab bar, navigation rail, split view, etc.)
 * that surrounds the actual content.
 *
 * ## Generated Code Usage
 *
 * The KSP generator uses this info to create entries in `GeneratedWrapperRegistry`:
 *
 * ```kotlin
 * @Composable
 * override fun TabWrapper(tabNodeKey: String, scope: TabWrapperScope, content: @Composable () -> Unit) {
 *     when (tabNodeKey) {
 *         "com.example.MainTabs" -> MainTabsWrapper(scope, content) // uses targetClass
 *         // ...
 *     }
 * }
 * ```
 *
 * @property functionDeclaration The KSP function declaration for this wrapper
 * @property functionName Simple function name (e.g., "MainTabsWrapper")
 * @property packageName Package containing this function
 * @property targetClass The class declaration for the tab/pane class this wrapper wraps
 * @property targetClassQualifiedName Qualified name of the target class for when-expression key
 * @property targetClassSimpleName Simple name of the target class for readable code generation
 * @property containingFile File path containing this function, for KSP incremental compilation
 * @property wrapperType Whether this is a TAB or PANE wrapper
 */
public data class WrapperInfo(
    val functionDeclaration: KSFunctionDeclaration,
    val functionName: String,
    val packageName: String,
    val targetClass: KSClassDeclaration,
    val targetClassQualifiedName: String,
    val targetClassSimpleName: String,
    val containingFile: String,
    val wrapperType: WrapperType
)
