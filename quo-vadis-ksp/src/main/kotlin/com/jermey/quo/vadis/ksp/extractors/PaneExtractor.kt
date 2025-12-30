package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.AdaptStrategy
import com.jermey.quo.vadis.ksp.models.PaneBackBehavior
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.PaneItemInfo
import com.jermey.quo.vadis.ksp.models.PaneRole

/**
 * Extracts @Pane and @PaneItem annotations into PaneInfo models.
 *
 * A pane container represents an adaptive layout with multiple panes
 * (e.g., list-detail pattern). This extractor handles:
 * - Pane container extraction from @Pane annotation
 * - Pane item extraction from @PaneItem annotations on subclasses
 * - Role, adapt strategy, and root graph resolution for each pane
 *
 * @property destinationExtractor Extractor for @Destination metadata
 * @property logger KSP logger for error/warning output
 */
class PaneExtractor(
    private val destinationExtractor: DestinationExtractor,
    private val logger: KSPLogger
) {

    /**
     * Extract PaneInfo from a class declaration.
     *
     * @param classDeclaration The sealed class annotated with @Pane
     * @return PaneInfo or null if extraction fails
     */
    fun extract(classDeclaration: KSClassDeclaration): PaneInfo? {
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Pane"
        } ?: return null

        val name = annotation.arguments.find {
            it.name?.asString() == "name"
        }?.value as? String ?: return null

        val backBehaviorValue = annotation.arguments.find {
            it.name?.asString() == "backBehavior"
        }?.value

        // Parse enum from annotation value - extract simple name from enum entry
        val backBehavior = parseBackBehavior(backBehaviorValue)

        val sealedSubclasses = classDeclaration.getSealedSubclasses().toList()

        // Extract @PaneItem annotated destinations (root destinations for each pane)
        val panes = sealedSubclasses
            .mapNotNull { extractPaneItem(it) }
            .toList()

        // Extract ALL destinations from the sealed class (for scope registration)
        // This includes both @PaneItem destinations and regular @Destination destinations
        val allDestinations = sealedSubclasses
            .mapNotNull { destinationExtractor.extract(it) }
            .toList()

        return PaneInfo(
            classDeclaration = classDeclaration,
            name = name,
            className = classDeclaration.simpleName.asString(),
            packageName = classDeclaration.packageName.asString(),
            backBehavior = backBehavior,
            panes = panes,
            allDestinations = allDestinations
        )
    }

    /**
     * Extract PaneItemInfo from a class declaration.
     *
     * @param classDeclaration The class annotated with @PaneItem
     * @return PaneItemInfo or null if not a pane item or extraction fails
     */
    private fun extractPaneItem(classDeclaration: KSClassDeclaration): PaneItemInfo? {
        val paneItemAnnotation = classDeclaration.annotations.find {
            it.shortName.asString() == "PaneItem"
        } ?: return null

        val destination = destinationExtractor.extract(classDeclaration) ?: return null

        val roleValue = paneItemAnnotation.arguments.find {
            it.name?.asString() == "role"
        }?.value
        val role = parseRole(roleValue)

        val adaptStrategyValue = paneItemAnnotation.arguments.find {
            it.name?.asString() == "adaptStrategy"
        }?.value
        val adaptStrategy = parseAdaptStrategy(adaptStrategyValue)

        return PaneItemInfo(
            destination = destination,
            role = role,
            adaptStrategy = adaptStrategy
        )
    }

    /**
     * Parse PaneBackBehavior from annotation value.
     *
     * @param value The raw annotation value from KSP
     * @return Parsed PaneBackBehavior enum value
     */
    private fun parseBackBehavior(value: Any?): PaneBackBehavior {
        val simpleName = extractEnumSimpleName(value)
        return try {
            PaneBackBehavior.valueOf(simpleName)
        } catch (e: IllegalArgumentException) {
            logger.warn("Unknown PaneBackBehavior: $simpleName, defaulting to PopUntilScaffoldValueChange")
            PaneBackBehavior.PopUntilScaffoldValueChange
        }
    }

    /**
     * Parse PaneRole from annotation value.
     *
     * @param value The raw annotation value from KSP
     * @return Parsed PaneRole enum value
     */
    private fun parseRole(value: Any?): PaneRole {
        val simpleName = extractEnumSimpleName(value)
        return try {
            PaneRole.valueOf(simpleName)
        } catch (e: IllegalArgumentException) {
            logger.warn("Unknown PaneRole: $simpleName, defaulting to PRIMARY")
            PaneRole.PRIMARY
        }
    }

    /**
     * Parse AdaptStrategy from annotation value.
     *
     * @param value The raw annotation value from KSP
     * @return Parsed AdaptStrategy enum value
     */
    private fun parseAdaptStrategy(value: Any?): AdaptStrategy {
        val simpleName = extractEnumSimpleName(value)
        return try {
            AdaptStrategy.valueOf(simpleName)
        } catch (e: IllegalArgumentException) {
            logger.warn("Unknown AdaptStrategy: $simpleName, defaulting to HIDE")
            AdaptStrategy.HIDE
        }
    }

    /**
     * Extract simple enum name from KSP annotation value.
     *
     * KSP returns enum values in various formats depending on context.
     * This handles: "VALUE", "EnumClass.VALUE", KSType objects, etc.
     *
     * @param value The raw annotation value from KSP
     * @return The simple name of the enum entry
     */
    private fun extractEnumSimpleName(value: Any?): String {
        if (value == null) return ""
        val str = value.toString()
        // Handle "EnumClass.VALUE" format
        return if (str.contains(".")) {
            str.substringAfterLast(".")
        } else {
            str
        }
    }

    /**
     * Extract all @Pane-annotated classes from the resolver.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of PaneInfo for all @Pane-annotated classes
     */
    fun extractAll(resolver: Resolver): List<PaneInfo> {
        return resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Pane")
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }
}
