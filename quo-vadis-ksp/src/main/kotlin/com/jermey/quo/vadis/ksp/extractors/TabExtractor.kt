package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TabItemInfo
import com.jermey.quo.vadis.ksp.models.TabItemType

/**
 * Extracts @Tab and @TabItem annotations into TabInfo models.
 *
 * Supports two patterns:
 * 1. **New Pattern**: @Tab with explicit `items` array referencing @TabItem/@Stack classes
 * 2. **Legacy Pattern**: @Tab on sealed class with @TabItem on nested subclasses (deprecated)
 *
 * The new pattern is reliable in KMP metadata compilation because it uses
 * top-level class references instead of nested class annotation discovery.
 *
 * ## New Pattern Example
 * ```kotlin
 * @TabItem(label = "Home", icon = "home")
 * @Stack(name = "homeStack", startDestination = "Feed")
 * sealed class HomeTab : Destination { ... }
 *
 * @Tab(name = "mainTabs", initialTab = HomeTab::class, items = [HomeTab::class, ExploreTab::class])
 * object MainTabs
 * ```
 *
 * ## Legacy Pattern Example
 * ```kotlin
 * @Tab(name = "mainTabs", initialTabLegacy = "Home")
 * sealed class MainTabs : Destination {
 *     @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
 *     @Destination(route = "tabs/home")
 *     data object Home : MainTabs()
 * }
 * ```
 *
 * @property destinationExtractor Extractor for @Destination metadata
 * @property stackExtractor Extractor for @Stack metadata (optional, for NESTED_STACK tab extraction)
 * @property logger KSP logger for error/warning output
 */
class TabExtractor(
    private val destinationExtractor: DestinationExtractor,
    private val logger: KSPLogger,
    private val stackExtractor: StackExtractor? = null
) {

    // Cache of TabItem classes indexed by parent qualified name (for legacy pattern)
    private var tabItemCache: Map<String, List<KSClassDeclaration>>? = null

    /**
     * Extract TabInfo from a class declaration.
     *
     * Detects the pattern used:
     * - If `items` array is non-empty → new pattern
     * - Otherwise → legacy pattern with sealed subclasses
     *
     * @param classDeclaration The class annotated with @Tab or @Tabs
     * @return TabInfo or null if extraction fails
     */
    fun extract(classDeclaration: KSClassDeclaration): TabInfo? {
        // Support both @Tab and @Tabs annotation names
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Tab" || it.shortName.asString() == "Tabs"
        } ?: return null

        val name = annotation.arguments.find {
            it.name?.asString() == "name"
        }?.value as? String ?: return null

        // Check for new pattern: items array
        val itemsArg = annotation.arguments.find {
            it.name?.asString() == "items"
        }?.value

        @Suppress("UNCHECKED_CAST")
        val itemsList = itemsArg as? List<KSType>

        val isNewPattern = !itemsList.isNullOrEmpty()

        if (isNewPattern) {
            logger.info("@Tab '$name' uses new pattern with items array (${itemsList!!.size} items)")
        } else {
            logger.warn("@Tab '$name' uses legacy pattern with sealed subclasses - may fail in KMP")
        }

        // Extract tabs based on pattern
        val tabs = if (isNewPattern) {
            extractFromItemsArray(itemsList!!, classDeclaration)
        } else {
            extractFromSealedSubclasses(classDeclaration)
        }

        if (tabs.isEmpty()) {
            logger.error("@Tab '$name' has no valid tab items", classDeclaration)
            return null
        }

        // Resolve initialTab (type-safe KClass for new pattern, string for legacy)
        val initialTabClass = resolveInitialTabClass(annotation, tabs)

        return TabInfo(
            classDeclaration = classDeclaration,
            name = name,
            className = classDeclaration.simpleName.asString(),
            packageName = classDeclaration.packageName.asString(),
            initialTabClass = initialTabClass,
            isNewPattern = isNewPattern,
            tabs = tabs
        )
    }

    /**
     * Resolve the initial tab class from annotation arguments.
     *
     * For new pattern: Resolves `initialTab: KClass<*>` (null if Unit::class)
     * For legacy pattern: Matches `initialTabLegacy: String` against tab class names
     *
     * @param annotation The @Tab annotation
     * @param tabs List of extracted tab items
     * @return The initial tab's class declaration, or null to use first tab
     */
    private fun resolveInitialTabClass(
        annotation: com.google.devtools.ksp.symbol.KSAnnotation,
        tabs: List<TabItemInfo>
    ): KSClassDeclaration? {
        // Try type-safe initialTab first (new pattern)
        val initialTabArg = annotation.arguments.find {
            it.name?.asString() == "initialTab"
        }?.value as? KSType

        if (initialTabArg != null) {
            val initialTabDecl = initialTabArg.declaration as? KSClassDeclaration
            val qualifiedName = initialTabDecl?.qualifiedName?.asString()

            // Unit::class means "use first tab"
            if (qualifiedName == "kotlin.Unit") {
                logger.info("initialTab = Unit::class, using first tab")
                return null
            }

            // Find matching tab
            val matchingTab = tabs.find {
                it.classDeclaration.qualifiedName?.asString() == qualifiedName
            }

            if (matchingTab != null) {
                logger.info("initialTab resolved to ${matchingTab.classDeclaration.simpleName.asString()}")
                return matchingTab.classDeclaration
            }

            logger.warn("initialTab class '$qualifiedName' not found in tabs, using first tab")
            return null
        }

        // Try legacy initialTabLegacy string (deprecated)
        val initialTabLegacy = annotation.arguments.find {
            it.name?.asString() == "initialTabLegacy"
        }?.value as? String

        if (!initialTabLegacy.isNullOrEmpty()) {
            val matchingTab = tabs.find {
                it.classDeclaration.simpleName.asString() == initialTabLegacy
            }

            if (matchingTab != null) {
                val tabName = matchingTab.classDeclaration.simpleName.asString()
                logger.info("initialTabLegacy '$initialTabLegacy' resolved to $tabName")
                return matchingTab.classDeclaration
            }

            logger.warn("initialTabLegacy '$initialTabLegacy' not found in tabs, using first tab")
        }

        return null
    }

    /**
     * Extract tabs from explicit items array (new pattern).
     *
     * Each class in the array must have @TabItem annotation.
     * The class is typically also annotated with @Stack (making it its own root graph).
     *
     * @param items List of KSType from items array
     * @param tabContainer The @Tab container class (for error reporting)
     * @return List of TabItemInfo
     */
    private fun extractFromItemsArray(
        items: List<KSType>,
        tabContainer: KSClassDeclaration
    ): List<TabItemInfo> {
        return items.mapNotNull { ksType ->
            val classDecl = ksType.declaration as? KSClassDeclaration
            if (classDecl == null) {
                logger.error("Could not resolve class from items array", tabContainer)
                return@mapNotNull null
            }

            extractTabItemNewPattern(classDecl)
        }
    }

    /**
     * Detects whether a @TabItem is a flat screen or nested stack based on annotations.
     *
     * Detection rules:
     * - @TabItem + @Stack + (sealed class) → NESTED_STACK
     * - @TabItem + @Destination + (data object) → FLAT_SCREEN
     *
     * @param classDeclaration The class to analyze
     * @return [TabItemType.NESTED_STACK] or [TabItemType.FLAT_SCREEN]
     */
    private fun detectTabItemType(classDeclaration: KSClassDeclaration): TabItemType {
        val hasStack = classDeclaration.annotations.any {
            it.shortName.asString() == "Stack"
        }
        val hasDestination = classDeclaration.annotations.any {
            it.shortName.asString() == "Destination"
        }

        return when {
            hasStack -> TabItemType.NESTED_STACK
            hasDestination -> TabItemType.FLAT_SCREEN
            else -> {
                // Default to FLAT_SCREEN if neither (will be validated later)
                logger.warn(
                    "TabItem '${classDeclaration.simpleName.asString()}' has neither @Stack nor @Destination",
                    classDeclaration
                )
                TabItemType.FLAT_SCREEN
            }
        }
    }

    /**
     * Extract TabItemInfo for new pattern (@TabItem on top-level class).
     *
     * In the new pattern:
     * - The class has @TabItem (label, icon)
     * - The class may have @Stack (NESTED_STACK) or @Destination (FLAT_SCREEN)
     * - rootGraphClass is null (class IS the stack or destination)
     *
     * Tab type detection:
     * - @TabItem + @Stack → [TabItemType.NESTED_STACK]
     * - @TabItem + @Destination → [TabItemType.FLAT_SCREEN]
     *
     * @param classDeclaration The @TabItem class
     * @return TabItemInfo or null if not valid
     */
    private fun extractTabItemNewPattern(classDeclaration: KSClassDeclaration): TabItemInfo? {
        val tabItemAnnotation = classDeclaration.annotations.find {
            it.shortName.asString() == "TabItem"
        }

        if (tabItemAnnotation == null) {
            logger.error(
                "Class ${classDeclaration.simpleName.asString()} in @Tab.items must have @TabItem",
                classDeclaration
            )
            return null
        }

        val label = tabItemAnnotation.arguments.find {
            it.name?.asString() == "label"
        }?.value as? String ?: ""

        val icon = tabItemAnnotation.arguments.find {
            it.name?.asString() == "icon"
        }?.value as? String ?: ""

        // Detect tab type and extract type-specific info
        val tabType = detectTabItemType(classDeclaration)
        val (destinationInfo, stackInfo) = extractTypeSpecificInfo(classDeclaration, tabType)

        logger.info(
            "Extracted @TabItem '${classDeclaration.simpleName.asString()}' " +
                "(label='$label', icon='$icon', tabType=$tabType, " +
                "stackClass=${stackInfo?.classDeclaration?.simpleName?.asString() ?: "self"})"
        )

        return TabItemInfo(
            label = label,
            icon = icon,
            classDeclaration = classDeclaration,
            tabType = tabType,
            destinationInfo = destinationInfo,
            stackInfo = stackInfo
        )
    }

    /**
     * Extract type-specific info based on tab type.
     */
    private fun extractTypeSpecificInfo(
        classDeclaration: KSClassDeclaration,
        tabType: TabItemType
    ): Pair<DestinationInfo?, StackInfo?> = when (tabType) {
        TabItemType.FLAT_SCREEN -> {
            val destInfo = destinationExtractor.extract(classDeclaration)
            if (destInfo == null) {
                logger.warn(
                    "FLAT_SCREEN TabItem '${classDeclaration.simpleName.asString()}' " +
                        "has @Destination but extraction failed",
                    classDeclaration
                )
            }
            destInfo to null
        }
        TabItemType.NESTED_STACK -> {
            val stackInfo = stackExtractor?.extract(classDeclaration)
            if (stackInfo == null && stackExtractor != null) {
                logger.warn(
                    "NESTED_STACK TabItem '${classDeclaration.simpleName.asString()}' " +
                        "has @Stack but extraction failed",
                    classDeclaration
                )
            } else if (stackExtractor == null) {
                logger.warn(
                    "NESTED_STACK TabItem '${classDeclaration.simpleName.asString()}' " +
                        "detected but no StackExtractor provided",
                    classDeclaration
                )
            }
            null to stackInfo
        }
    }

    /**
     * Extract tabs from sealed subclasses (legacy pattern).
     *
     * This may fail in KMP metadata compilation due to KSP limitations
     * with nested class annotation discovery.
     *
     * @param classDeclaration The sealed @Tab class
     * @return List of TabItemInfo from sealed subclasses
     */
    private fun extractFromSealedSubclasses(classDeclaration: KSClassDeclaration): List<TabItemInfo> {
        // Try getSealedSubclasses first (works in some KSP configurations)
        var tabs = classDeclaration.getSealedSubclasses()
            .mapNotNull { extractTabItemLegacy(it) }
            .toList()

        // Fallback to cached @TabItem classes if getSealedSubclasses returns empty
        if (tabs.isEmpty() && tabItemCache != null) {
            val parentQualifiedName = classDeclaration.qualifiedName?.asString()
            if (parentQualifiedName != null) {
                tabs = tabItemCache!![parentQualifiedName]
                    ?.mapNotNull { extractTabItemLegacy(it) }
                    ?: emptyList()
            }
        }

        return tabs
    }

    /**
     * Extract TabItemInfo for legacy pattern (@TabItem on nested subclass).
     *
     * In the legacy pattern:
     * - The class is a nested sealed subclass with @TabItem
     * - The class must also have @Destination
     * - rootGraph parameter is deprecated and ignored
     *
     * @param classDeclaration The nested @TabItem subclass
     * @return TabItemInfo or null if not valid
     */
    private fun extractTabItemLegacy(classDeclaration: KSClassDeclaration): TabItemInfo? {
        val tabItemAnnotation = classDeclaration.annotations.find {
            it.shortName.asString() == "TabItem"
        } ?: return null

        // Legacy pattern requires @Destination on the tab item
        val destination = destinationExtractor.extract(classDeclaration)
        if (destination == null) {
            logger.warn(
                "Legacy @TabItem '${classDeclaration.simpleName.asString()}' missing @Destination",
                classDeclaration
            )
            return null
        }

        val label = tabItemAnnotation.arguments.find {
            it.name?.asString() == "label"
        }?.value as? String ?: ""

        val icon = tabItemAnnotation.arguments.find {
            it.name?.asString() == "icon"
        }?.value as? String ?: ""

        // Note: rootGraph parameter is deprecated and no longer supported
        // Check if it's set and warn the user
        val rootGraphType = tabItemAnnotation.arguments.find {
            it.name?.asString() == "rootGraph"
        }?.value as? KSType
        val rootGraphQualifiedName = (rootGraphType?.declaration as? KSClassDeclaration)
            ?.qualifiedName?.asString()
        if (rootGraphQualifiedName != null && rootGraphQualifiedName != "kotlin.Unit") {
            logger.warn(
                "Legacy @TabItem '${classDeclaration.simpleName.asString()}' uses deprecated " +
                    "'rootGraph' parameter. Please migrate to the new pattern using @Stack " +
                    "annotation directly on the tab class.",
                classDeclaration
            )
        }

        // Legacy pattern is now treated as FLAT_SCREEN with the destination info
        return TabItemInfo(
            label = label,
            icon = icon,
            classDeclaration = classDeclaration,
            tabType = TabItemType.FLAT_SCREEN,
            destinationInfo = destination,
            stackInfo = null
        )
    }

    /**
     * Populate the @TabItem cache from resolver.
     *
     * This must be called before extracting @Tab containers to ensure
     * the fallback mechanism works correctly for the legacy pattern.
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

        // Only cache nested classes (for legacy pattern)
        // Top-level @TabItem classes are resolved via items array (new pattern)
        tabItemCache = tabItems
            .filter { it.parentDeclaration is KSClassDeclaration }
            .groupBy { tabItemClass ->
                (tabItemClass.parentDeclaration as KSClassDeclaration)
                    .qualifiedName?.asString() ?: ""
            }
            .filterKeys { it.isNotEmpty() }

        logger.info("TabItem cache has ${tabItemCache?.size ?: 0} parent entries (legacy pattern)")
    }

    /**
     * Extract all @Tab/@Tabs-annotated classes from the resolver.
     *
     * Automatically populates the @TabItem cache before extraction
     * to ensure sealed subclass resolution works for legacy pattern.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of TabInfo for all @Tab/@Tabs-annotated classes
     */
    fun extractAll(resolver: Resolver): List<TabInfo> {
        // Populate cache first to work around KSP sealed subclass issues (legacy pattern)
        populateTabItemCache(resolver)

        // Collect from both @Tab and @Tabs annotations
        val tabAnnotated = resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Tab")
            .filterIsInstance<KSClassDeclaration>()
        val tabsAnnotated = resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Tabs")
            .filterIsInstance<KSClassDeclaration>()

        return (tabAnnotated + tabsAnnotated)
            .distinctBy { it.qualifiedName?.asString() }
            .mapNotNull { extract(it) }
            .toList()
    }
}
