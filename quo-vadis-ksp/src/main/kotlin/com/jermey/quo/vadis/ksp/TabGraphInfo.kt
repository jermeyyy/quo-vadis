package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Information extracted from a @TabGraph annotated sealed class.
 */
data class TabGraphInfo(
    val name: String,
    val packageName: String,
    val className: String,
    val initialTab: String?,
    val primaryTab: String?,
    val tabs: List<TabInfo>,
    val classDeclaration: KSClassDeclaration
) {
    /**
     * Get the initial tab (first tab if not specified).
     */
    fun getInitialTab(): TabInfo {
        return if (!initialTab.isNullOrEmpty()) {
            tabs.first { it.name == initialTab }
        } else {
            tabs.first()
        }
    }

    /**
     * Get the primary tab (initial tab if not specified).
     */
    fun getPrimaryTab(): TabInfo {
        return if (!primaryTab.isNullOrEmpty()) {
            tabs.first { it.name == primaryTab }
        } else {
            getInitialTab()
        }
    }

    /**
     * Generate the config property name (e.g., "MainTabConfig").
     */
    fun getConfigName(): String = "${className}Config"

    /**
     * Generate the container function name (e.g., "MainTabContainer").
     */
    fun getContainerName(): String = "${className}Container"

    /**
     * Generate the graph builder function name (e.g., "buildMainTabGraph").
     */
    fun getGraphBuilderName(): String = "build${className}Graph"
}

/**
 * Information extracted from a @Tab annotated object/class.
 */
data class TabInfo(
    val name: String,
    val route: String,
    val label: String,
    val icon: String,
    val rootGraph: String,
    val rootDestination: String?,
    val packageName: String,
    val parentClassName: String,
    val classDeclaration: KSClassDeclaration
) {
    /**
     * Get the fully qualified name (e.g., "com.example.MainTab.Home").
     */
    fun getFullyQualifiedName(): String = "$packageName.$parentClassName.$name"

    /**
     * Get the simple reference (e.g., "MainTab.Home").
     */
    fun getSimpleReference(): String = "$parentClassName.$name"
}

/**
 * Information extracted from a @TabContent annotated function.
 */
data class TabContentInfo(
    val tabClassName: String,
    val functionName: String,
    val functionDeclaration: KSFunctionDeclaration,
    val packageName: String
)
