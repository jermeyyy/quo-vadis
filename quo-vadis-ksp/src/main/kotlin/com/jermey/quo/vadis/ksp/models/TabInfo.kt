package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Extracted metadata from a @Tab annotation.
 *
 * @property classDeclaration The KSP class declaration for this tab container
 * @property name Tab container identifier from annotation (e.g., "main")
 * @property className Simple class name (e.g., "MainTabs")
 * @property packageName Package containing this class
 * @property initialTab Simple name of the initial tab (e.g., "Home")
 * @property tabs List of all @TabItem subclasses within this sealed class
 */
data class TabInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val initialTab: String,
    val tabs: List<TabItemInfo>
)

/**
 * Extracted metadata from a @TabItem annotation.
 *
 * @property destination The destination info for this tab
 * @property label Display label for the tab (e.g., "Home")
 * @property icon Icon identifier for the tab (e.g., "home")
 * @property rootGraphClass Class declaration for the root graph of this tab
 */
data class TabItemInfo(
    val destination: DestinationInfo,
    val label: String,
    val icon: String,
    val rootGraphClass: KSClassDeclaration
)
