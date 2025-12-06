package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TabItemInfo

/**
 * Extracts @Tab and @TabItem annotations into TabInfo models.
 *
 * A tab container represents a navigation pattern with multiple tabs,
 * each containing its own navigation stack. This extractor handles:
 * - Tab container extraction from @Tab annotation
 * - Tab item extraction from @TabItem annotations on subclasses
 * - Root graph reference resolution for each tab
 *
 * @property destinationExtractor Extractor for @Destination metadata
 * @property logger KSP logger for error/warning output
 */
class TabExtractor(
    private val destinationExtractor: DestinationExtractor,
    private val logger: KSPLogger
) {

    /**
     * Extract TabInfo from a class declaration.
     *
     * @param classDeclaration The sealed class annotated with @Tab
     * @return TabInfo or null if extraction fails
     */
    fun extract(classDeclaration: KSClassDeclaration): TabInfo? {
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Tab"
        } ?: return null

        val name = annotation.arguments.find {
            it.name?.asString() == "name"
        }?.value as? String ?: return null

        val initialTab = annotation.arguments.find {
            it.name?.asString() == "initialTab"
        }?.value as? String ?: ""

        val tabs = classDeclaration.getSealedSubclasses()
            .mapNotNull { extractTabItem(it) }
            .toList()

        return TabInfo(
            classDeclaration = classDeclaration,
            name = name,
            className = classDeclaration.simpleName.asString(),
            packageName = classDeclaration.packageName.asString(),
            initialTab = initialTab,
            tabs = tabs
        )
    }

    /**
     * Extract TabItemInfo from a class declaration.
     *
     * @param classDeclaration The class annotated with @TabItem
     * @return TabItemInfo or null if not a tab item or extraction fails
     */
    private fun extractTabItem(classDeclaration: KSClassDeclaration): TabItemInfo? {
        val tabItemAnnotation = classDeclaration.annotations.find {
            it.shortName.asString() == "TabItem"
        } ?: return null

        val destination = destinationExtractor.extract(classDeclaration) ?: return null

        val label = tabItemAnnotation.arguments.find {
            it.name?.asString() == "label"
        }?.value as? String ?: ""

        val icon = tabItemAnnotation.arguments.find {
            it.name?.asString() == "icon"
        }?.value as? String ?: ""

        val rootGraphType = tabItemAnnotation.arguments.find {
            it.name?.asString() == "rootGraph"
        }?.value as? KSType

        val rootGraphClass = rootGraphType?.declaration as? KSClassDeclaration
        if (rootGraphClass == null) {
            logger.warn("Could not resolve rootGraph for tab item ${classDeclaration.simpleName.asString()}")
            return null
        }

        return TabItemInfo(
            destination = destination,
            label = label,
            icon = icon,
            rootGraphClass = rootGraphClass
        )
    }

    /**
     * Extract all @Tab-annotated classes from the resolver.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of TabInfo for all @Tab-annotated classes
     */
    fun extractAll(resolver: Resolver): List<TabInfo> {
        return resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Tab")
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }
}
