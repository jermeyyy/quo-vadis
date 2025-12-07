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
 * ## KSP KMP Workaround
 *
 * Due to KSP limitations with Kotlin Multiplatform metadata compilation,
 * `getSealedSubclasses()` may return empty results. This extractor uses
 * a two-phase approach:
 * 1. Extract all @TabItem-annotated classes via `getSymbolsWithAnnotation()`
 * 2. Match them to parent @Tab classes using `parentDeclaration`
 *
 * @property destinationExtractor Extractor for @Destination metadata
 * @property logger KSP logger for error/warning output
 */
class TabExtractor(
    private val destinationExtractor: DestinationExtractor,
    private val logger: KSPLogger
) {

    // Cache of TabItem classes indexed by parent qualified name
    private var tabItemCache: Map<String, List<KSClassDeclaration>>? = null

    /**
     * Extract TabInfo from a class declaration.
     *
     * Uses cached @TabItem classes if available to work around KSP
     * sealed subclass resolution issues in KMP metadata compilation.
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

        // Try getSealedSubclasses first (works in some KSP configurations)
        var tabs = classDeclaration.getSealedSubclasses()
            .mapNotNull { extractTabItem(it) }
            .toList()

        // Fallback to cached @TabItem classes if getSealedSubclasses returns empty
        if (tabs.isEmpty() && tabItemCache != null) {
            val parentQualifiedName = classDeclaration.qualifiedName?.asString()
            if (parentQualifiedName != null) {
                tabs = tabItemCache!![parentQualifiedName]
                    ?.mapNotNull { extractTabItem(it) }
                    ?: emptyList()
            }
        }

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
     * Populate the @TabItem cache from resolver.
     *
     * This must be called before extracting @Tab containers to ensure
     * the fallback mechanism works correctly.
     *
     * @param resolver KSP resolver to query for @TabItem symbols
     */
    fun populateTabItemCache(resolver: Resolver) {
        val tabItems = resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.TabItem")
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        logger.info("Found ${tabItems.size} @TabItem classes")
        tabItems.forEach { item ->
            val parentName = (item.parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString()
            logger.info("  - ${item.simpleName.asString()} (parent: $parentName)")
        }

        tabItemCache = tabItems
            .groupBy { tabItemClass ->
                // Get the parent sealed class qualified name
                (tabItemClass.parentDeclaration as? KSClassDeclaration)
                    ?.qualifiedName?.asString()
                    ?: ""
            }
            .filterKeys { it.isNotEmpty() }

        logger.info("TabItem cache has ${tabItemCache?.size ?: 0} parent entries")
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
     * Automatically populates the @TabItem cache before extraction
     * to ensure sealed subclass resolution works in KMP metadata compilation.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of TabInfo for all @Tab-annotated classes
     */
    fun extractAll(resolver: Resolver): List<TabInfo> {
        // Populate cache first to work around KSP sealed subclass issues
        populateTabItemCache(resolver)

        return resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Tab")
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }
}
