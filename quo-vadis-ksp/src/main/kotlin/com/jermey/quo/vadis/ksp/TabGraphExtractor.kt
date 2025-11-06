package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

/**
 * Extracts [TabGraphInfo] from @TabGraph annotated sealed classes.
 */
object TabGraphExtractor {

    fun extract(tabGraphClass: KSClassDeclaration, logger: KSPLogger): TabGraphInfo {
        val tabGraphAnnotation = tabGraphClass.annotations
            .first { it.shortName.asString() == "TabGraph" }

        val name = tabGraphAnnotation.arguments
            .first { it.name?.asString() == "name" }
            .value as String

        val initialTab = (tabGraphAnnotation.arguments
            .first { it.name?.asString() == "initialTab" }
            .value as? String)
            ?.takeIf { it.isNotEmpty() }

        val primaryTab = (tabGraphAnnotation.arguments
            .first { it.name?.asString() == "primaryTab" }
            .value as? String)
            ?.takeIf { it.isNotEmpty() }

        val packageName = tabGraphClass.packageName.asString()
        val className = tabGraphClass.simpleName.asString()

        // Validate sealed class
        if (!tabGraphClass.modifiers.contains(Modifier.SEALED)) {
            logger.error("@TabGraph can only be applied to sealed classes", tabGraphClass)
            error("TabGraph class must be sealed")
        }

        // Validate extends TabDefinition
        val extendsTabDefinition = tabGraphClass.superTypes.any { superType ->
            val declaration = superType.resolve().declaration
            declaration.qualifiedName?.asString()?.endsWith("TabDefinition") == true
        }

        if (!extendsTabDefinition) {
            logger.error(
                "@TabGraph annotated class must extend TabDefinition",
                tabGraphClass
            )
            error("TabGraph class must extend TabDefinition")
        }

        // Extract tabs from sealed subclasses
        val tabs = extractTabs(tabGraphClass, className, packageName, logger)

        if (tabs.isEmpty()) {
            logger.error("@TabGraph must contain at least one @Tab annotated subclass", tabGraphClass)
            error("TabGraph must have at least one tab")
        }

        // Validate initialTab and primaryTab references
        if (initialTab != null && tabs.none { it.name == initialTab }) {
            logger.error(
                "initialTab '$initialTab' not found in tab subclasses",
                tabGraphClass
            )
        }

        if (primaryTab != null && tabs.none { it.name == primaryTab }) {
            logger.error(
                "primaryTab '$primaryTab' not found in tab subclasses",
                tabGraphClass
            )
        }

        return TabGraphInfo(
            name = name,
            packageName = packageName,
            className = className,
            initialTab = initialTab,
            primaryTab = primaryTab,
            tabs = tabs,
            classDeclaration = tabGraphClass
        )
    }

    private fun extractTabs(
        tabGraphClass: KSClassDeclaration,
        parentClassName: String,
        packageName: String,
        logger: KSPLogger
    ): List<TabInfo> {
        return tabGraphClass.getSealedSubclasses().mapNotNull { tabClass ->
            extractTabInfo(tabClass, parentClassName, packageName, logger)
        }.toList()
    }

    private fun extractTabInfo(
        tabClass: KSClassDeclaration,
        parentClassName: String,
        packageName: String,
        logger: KSPLogger
    ): TabInfo? {
        // Extract @Tab annotation
        val tabAnnotation = tabClass.annotations
            .firstOrNull { it.shortName.asString() == "Tab" }

        if (tabAnnotation == null) {
            val tabName = tabClass.simpleName.asString()
            logger.warn(
                "Tab subclass $tabName has no @Tab annotation, skipping",
                tabClass
            )
            return null
        }

        return extractTabInfoFromAnnotation(tabAnnotation, tabClass, parentClassName, packageName, logger)
    }

    @Suppress("ReturnCount")
    private fun extractTabInfoFromAnnotation(
        tabAnnotation: com.google.devtools.ksp.symbol.KSAnnotation,
        tabClass: KSClassDeclaration,
        parentClassName: String,
        packageName: String,
        logger: KSPLogger
    ): TabInfo? {

        // Extract annotation parameters
        val route = tabAnnotation.arguments
            .first { it.name?.asString() == "route" }
            .value as String

        val label = tabAnnotation.arguments
            .first { it.name?.asString() == "label" }
            .value as String

        val icon = tabAnnotation.arguments
            .first { it.name?.asString() == "icon" }
            .value as String

        val rootGraphType = tabAnnotation.arguments
            .first { it.name?.asString() == "rootGraph" }
            .value as KSType

        val rootGraph = rootGraphType.declaration.qualifiedName?.asString()
            ?: run {
                logger.error("Could not resolve rootGraph type", tabClass)
                return null
            }

        val rootDestinationType = tabAnnotation.arguments
            .first { it.name?.asString() == "rootDestination" }
            .value as? KSType

        val rootDestination = rootDestinationType?.let {
            val qualifiedName = it.declaration.qualifiedName?.asString()
            // Filter out Nothing::class (default value)
            if (qualifiedName == "kotlin.Nothing") null else qualifiedName
        }

        val name = tabClass.simpleName.asString()

        return TabInfo(
            name = name,
            route = route,
            label = label,
            icon = icon,
            rootGraph = rootGraph,
            rootDestination = rootDestination,
            packageName = packageName,
            parentClassName = parentClassName,
            classDeclaration = tabClass
        )
    }
}
