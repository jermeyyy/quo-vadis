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
 * Extracts `@Tabs` and `@TabItem` annotations into [TabInfo] models.
 *
 * Uses a **child-to-parent** discovery pattern: each `@TabItem` declares its
 * parent `@Tabs` container via `@TabItem(parent = ...)`. This enables cross-module
 * tab discovery without requiring the parent to list its children.
 *
 * ## Example
 * ```kotlin
 * @Tabs(name = "mainTabs")
 * object MainTabs
 *
 * @TabItem(parent = MainTabs::class, ordinal = 0)
 * @Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
 * sealed class HomeTab : NavDestination { ... }
 *
 * @TabItem(parent = MainTabs::class, ordinal = 1)
 * @Stack(name = "exploreStack", startDestination = ExploreTab.Root::class)
 * sealed class ExploreTab : NavDestination { ... }
 * ```
 *
 * @property destinationExtractor Extractor for `@Destination` metadata
 * @property logger KSP logger for error/warning output
 * @property stackExtractor Extractor for `@Stack` metadata (optional)
 */
class TabExtractor(
    private val destinationExtractor: DestinationExtractor,
    private val logger: KSPLogger,
    private val stackExtractor: StackExtractor? = null
) {

    /**
     * Extract all tab containers from the resolver.
     *
     * 1. Discovers all `@TabItem`-annotated classes and groups them by parent
     * 2. Discovers all `@Tabs`-annotated classes and matches children from step 1
     * 3. Returns fully assembled [TabInfo] list with children sorted by ordinal
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of [TabInfo] for all `@Tabs`-annotated classes
     */
    fun extractAll(resolver: Resolver): List<TabInfo> {
        val tabItemsByParent = extractAllTabItems(resolver)
        return extractAllTabs(resolver, tabItemsByParent)
    }

    /**
     * Discover all `@TabItem`-annotated classes and group by parent qualified name.
     *
     * @return Map of parent qualified name → list of (classDeclaration, ordinal)
     */
    private fun extractAllTabItems(
        resolver: Resolver
    ): Map<String, List<Pair<KSClassDeclaration, Int>>> {
        val tabItems = resolver
            .getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.TabItem")
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        logger.info("Found ${tabItems.size} @TabItem classes")

        return tabItems.mapNotNull { classDecl ->
            val annotation = classDecl.annotations.first {
                it.shortName.asString() == "TabItem"
            }

            val parentType = annotation.arguments
                .first { it.name?.asString() == "parent" }
                .value as? KSType
            val parentQualifiedName = parentType?.declaration?.qualifiedName?.asString()

            val ordinal = annotation.arguments
                .first { it.name?.asString() == "ordinal" }
                .value as? Int ?: 0

            if (parentQualifiedName == null) {
                logger.error(
                    "@TabItem '${classDecl.simpleName.asString()}' has unresolvable parent",
                    classDecl
                )
                return@mapNotNull null
            }

            logger.info(
                "  - ${classDecl.simpleName.asString()} " +
                    "(parent=$parentQualifiedName, ordinal=$ordinal)"
            )

            Triple(parentQualifiedName, classDecl, ordinal)
        }.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second to it.third }
        )
    }

    /**
     * Discover all `@Tabs`-annotated classes and build [TabInfo] for each,
     * matching children from the pre-grouped [tabItemsByParent] map.
     */
    private fun extractAllTabs(
        resolver: Resolver,
        tabItemsByParent: Map<String, List<Pair<KSClassDeclaration, Int>>>
    ): List<TabInfo> {
        val tabsClasses = resolver
            .getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Tabs")
            .filterIsInstance<KSClassDeclaration>()
            .toMutableList()

        val tabsQualifiedNames = tabsClasses.mapNotNull { it.qualifiedName?.asString() }.toMutableSet()

        // Resolve cross-module @Tabs parents from compiled dependencies.
        // When a @TabItem references a parent that isn't in this module's sources,
        // look it up from the classpath (requires @Tabs to have BINARY retention).
        val orphanedParents = tabItemsByParent.keys - tabsQualifiedNames
        orphanedParents.forEach { parentName ->
            val parentDecl = resolver.getClassDeclarationByName(
                resolver.getKSNameFromString(parentName)
            )
            if (parentDecl != null) {
                val hasTabsAnnotation = parentDecl.annotations.any {
                    it.shortName.asString() == "Tabs"
                }
                if (hasTabsAnnotation) {
                    logger.info(
                        "Resolved cross-module @Tabs parent '$parentName' from classpath"
                    )
                    tabsClasses.add(parentDecl)
                    tabsQualifiedNames.add(parentName)
                } else {
                    val orphans = tabItemsByParent[parentName] ?: return@forEach
                    orphans.forEach { (classDecl, _) ->
                        logger.warn(
                            "@TabItem '${classDecl.simpleName.asString()}' references parent " +
                                "'$parentName' which has no @Tabs annotation",
                            classDecl
                        )
                    }
                }
            } else {
                val orphans = tabItemsByParent[parentName] ?: return@forEach
                orphans.forEach { (classDecl, _) ->
                    logger.warn(
                        "@TabItem '${classDecl.simpleName.asString()}' references parent " +
                            "'$parentName' which could not be resolved",
                        classDecl
                    )
                }
            }
        }

        // Track which @Tabs parents were resolved from classpath (cross-module)
        val crossModuleParents = mutableSetOf<String>()

        return tabsClasses.mapNotNull { classDecl ->
            val annotation = classDecl.annotations.find {
                it.shortName.asString() == "Tabs"
            } ?: return@mapNotNull null

            val name = annotation.arguments
                .find { it.name?.asString() == "name" }
                ?.value as? String ?: return@mapNotNull null

            val qualifiedName = classDecl.qualifiedName?.asString()
            val children = tabItemsByParent[qualifiedName] ?: emptyList()
            val isCrossModule = classDecl.containingFile == null

            if (children.isEmpty()) {
                logger.info(
                    "@Tabs '$name' has no @TabItem children in this module — " +
                        "skipping (children may be in downstream modules)"
                )
                return@mapNotNull null
            }

            val tabs = children
                .sortedBy { (_, ordinal) -> ordinal }
                .mapNotNull { (childDecl, ordinal) -> extractTabItem(childDecl, ordinal) }

            logger.info("@Tabs '$name' assembled with ${tabs.size} tab items" +
                if (isCrossModule) " (cross-module)" else "")

            TabInfo(
                classDeclaration = classDecl,
                name = name,
                className = classDecl.simpleName.asString(),
                packageName = classDecl.packageName.asString(),
                tabs = tabs,
                isCrossModule = isCrossModule
            )
        }
    }

    /**
     * Build a [TabItemInfo] from a `@TabItem`-annotated class.
     *
     * @param classDeclaration The `@TabItem` class
     * @param ordinal The tab's ordinal position
     * @return [TabItemInfo] or null if extraction fails
     */
    private fun extractTabItem(
        classDeclaration: KSClassDeclaration,
        ordinal: Int
    ): TabItemInfo? {
        val tabType = detectTabItemType(classDeclaration) ?: return null
        val (destinationInfo, stackInfo) = extractTypeSpecificInfo(classDeclaration, tabType)

        logger.info(
            "Extracted @TabItem '${classDeclaration.simpleName.asString()}' " +
                "(tabType=$tabType, ordinal=$ordinal)"
        )

        return TabItemInfo(
            classDeclaration = classDeclaration,
            tabType = tabType,
            ordinal = ordinal,
            destinationInfo = destinationInfo,
            stackInfo = stackInfo
        )
    }

    /**
     * Detect the [TabItemType] based on which annotations are present.
     *
     * - `@Destination` → [TabItemType.DESTINATION]
     * - `@Tabs` → [TabItemType.TABS]
     * - `@Stack` → [TabItemType.STACK]
     * - None → logs error and returns null
     */
    private fun detectTabItemType(classDeclaration: KSClassDeclaration): TabItemType? {
        val hasDestination = classDeclaration.annotations.any {
            it.shortName.asString() == "Destination"
        }
        val hasTabs = classDeclaration.annotations.any {
            it.shortName.asString() == "Tabs"
        }
        val hasStack = classDeclaration.annotations.any {
            it.shortName.asString() == "Stack"
        }

        val present = buildList {
            if (hasStack) add("@Stack")
            if (hasDestination) add("@Destination")
            if (hasTabs) add("@Tabs")
        }

        if (present.size > 1) {
            logger.error(
                "@TabItem '${classDeclaration.simpleName.asString()}' " +
                    "has conflicting annotations: ${present.joinToString(" and ")}. " +
                    "Use exactly one of @Stack, @Destination, or @Tabs",
                classDeclaration
            )
            return null
        }

        return when {
            hasDestination -> TabItemType.DESTINATION
            hasTabs -> TabItemType.TABS
            hasStack -> TabItemType.STACK
            else -> {
                logger.error(
                    "@TabItem '${classDeclaration.simpleName.asString()}' " +
                        "has neither @Destination, @Stack, nor @Tabs",
                    classDeclaration
                )
                null
            }
        }
    }

    /**
     * Extract type-specific info (destination or stack) based on tab type.
     */
    private fun extractTypeSpecificInfo(
        classDeclaration: KSClassDeclaration,
        tabType: TabItemType
    ): Pair<DestinationInfo?, StackInfo?> = when (tabType) {
        TabItemType.DESTINATION -> {
            val destInfo = destinationExtractor.extract(classDeclaration)
            if (destInfo == null) {
                logger.warn(
                    "DESTINATION TabItem '${classDeclaration.simpleName.asString()}' " +
                        "has @Destination but extraction failed",
                    classDeclaration
                )
            }
            destInfo to null
        }
        TabItemType.STACK -> {
            val stackInfo = stackExtractor?.extract(classDeclaration)
            if (stackInfo == null && stackExtractor != null) {
                logger.warn(
                    "STACK TabItem '${classDeclaration.simpleName.asString()}' " +
                        "has @Stack but extraction failed",
                    classDeclaration
                )
            } else if (stackExtractor == null) {
                logger.warn(
                    "STACK TabItem '${classDeclaration.simpleName.asString()}' " +
                        "detected but no StackExtractor provided",
                    classDeclaration
                )
            }
            null to stackInfo
        }
        TabItemType.TABS -> {
            // Nested tabs — no internal extraction needed here;
            // the nested @Tabs will be processed in its own extractAll pass
            null to null
        }
    }
}
