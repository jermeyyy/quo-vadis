package com.jermey.quo.vadis.ksp.validation

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.PaneInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TabItemType

/**
 * Comprehensive validation engine for Quo Vadis navigation annotations.
 *
 * This engine performs validation of all navigation annotations and their relationships,
 * providing clear error messages with source locations and fix suggestions to help developers
 * quickly identify and resolve configuration issues at compile time.
 *
 * ## Error Message Format
 *
 * All validation messages follow a consistent inline format:
 * ```
 * {Description} in file '{fileName}' (line {lineNumber}). Fix: {Suggestion}
 * ```
 *
 * ## Validation Categories
 *
 * The engine validates across four main categories:
 *
 * ### 1. Structural Validations
 * - Invalid start destination references
 * - Invalid initial tab references
 * - Empty containers (no destinations)
 *
 * ### 2. Route Validations
 * - Route parameter mismatches (route param without constructor param)
 * - Constructor params not in route pattern
 * - Duplicate routes across destinations
 *
 * ### 3. Reference Validations
 * - Invalid root graph references (@TabItem/@PaneItem referencing non-@Stack class)
 * - Missing screen bindings
 * - Duplicate screen bindings for same destination
 *
 * ### 4. Type Validations
 * - Non-sealed class containers
 * - Non-data object/class destinations
 *
 * ## Usage
 *
 * ```kotlin
 * val validationEngine = ValidationEngine(logger)
 * val isValid = validationEngine.validate(stacks, tabs, panes, screens, allDestinations, resolver)
 * if (!isValid) {
 *     // Stop processing - errors have been reported via logger
 *     return emptyList()
 * }
 * ```
 *
 * @param logger KSP logger for reporting errors and warnings with source locations
 */
class ValidationEngine(
    private val logger: KSPLogger
) {

    private var hasErrors = false

    /**
     * Validates all extracted annotation metadata and reports issues.
     *
     * Performs comprehensive validation across all annotation types and their relationships.
     * Errors are reported directly via the KSPLogger with source locations, allowing the IDE
     * to display them at the correct positions.
     *
     * The validation is performed in this order:
     * 1. Structural validations (container relationships)
     * 2. Route validations (route patterns and parameters)
     * 3. Reference validations (cross-references between annotations)
     * 4. Type validations (class modifiers and types)
     *
     * @param stacks List of all extracted @Stack annotations with their metadata
     * @param tabs List of all extracted @Tab annotations with their metadata
     * @param panes List of all extracted @Pane annotations with their metadata
     * @param screens List of all extracted @Screen annotations with their metadata
     * @param allDestinations List of all @Destination classes found in the codebase
     * @param resolver KSP resolver for additional symbol lookups
     * @return `true` if validation passed with no errors, `false` if any errors were found.
     *         Note that warnings do not cause validation to fail.
     */
    fun validate(
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>,
        screens: List<ScreenInfo>,
        allDestinations: List<DestinationInfo>,
        resolver: Resolver
    ): Boolean {
        hasErrors = false

        // Structural validations
        validateContainerStartDestinations(stacks)
        validateTabInitialTabs(tabs)
        validateEmptyContainers(stacks, tabs, panes)

        // Route validations
        validateRouteParameters(allDestinations)
        validateDuplicateRoutes(allDestinations)

        // Argument validations
        validateArgumentAnnotations(allDestinations)

        // Reference validations
        validateScreenBindings(screens, allDestinations)

        // Type validations
        validateContainerTypes(stacks, tabs, panes)
        validateDestinationTypes(allDestinations)

        // Mixed tab type validations
        validateTabItemAnnotations(tabs)
        validateNestedStackTabs(tabs)
        validateFlatScreenTabs(tabs)

        return !hasErrors
    }

    // =========================================================================
    // Structural Validations
    // =========================================================================

    /**
     * Validates that @Stack(startDestination) references an existing destination.
     */
    private fun validateContainerStartDestinations(stacks: List<StackInfo>) {
        stacks.forEach { stack ->
            if (stack.resolvedStartDestination == null) {
                val availableDestinations = stack.destinations.map { it.className }
                reportError(
                    stack.classDeclaration,
                    "Invalid startDestination '${stack.startDestination}' for @Stack '${stack.name}'",
                    "Use one of the available destinations: $availableDestinations"
                )
            }
        }
    }

    /**
     * Validates that @Tabs(initialTab) references an existing tab item.
     */
    private fun validateTabInitialTabs(tabs: List<TabInfo>) {
        tabs.forEach { tab ->
            // New pattern uses type-safe initialTabClass; null means use first tab
            if (tab.initialTabClass != null) {
                val initialQualifiedName = tab.initialTabClass.qualifiedName?.asString()
                val initialTabExists = tab.tabs.any { 
                    it.classDeclaration.qualifiedName?.asString() == initialQualifiedName 
                }
                if (!initialTabExists) {
                    val availableTabs = tab.tabs.map { it.classDeclaration.simpleName.asString() }
                    reportError(
                        tab.classDeclaration,
                        "Invalid initialTab '${tab.initialTabClass.simpleName.asString()}' for @Tabs '${tab.name}'",
                        "Use one of the available tabs: $availableTabs"
                    )
                }
            }
        }
    }

    /**
     * Validates that @Stack, @Tabs, and @Pane containers have at least one destination.
     */
    private fun validateEmptyContainers(
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>
    ) {
        stacks.filter { it.destinations.isEmpty() }.forEach { stack ->
            reportError(
                stack.classDeclaration,
                "@Stack '${stack.className}' has no destinations",
                "Add at least one @Destination annotated subclass inside this sealed class"
            )
        }

        tabs.filter { it.tabs.isEmpty() }.forEach { tab ->
            reportError(
                tab.classDeclaration,
                "@Tabs container '${tab.className}' has no @TabItem entries",
                "Add at least one @TabItem annotated class to the items array"
            )
        }

        panes.filter { it.panes.isEmpty() }.forEach { pane ->
            reportError(
                pane.classDeclaration,
                "@Pane container '${pane.className}' has no @PaneItem entries",
                "Add at least one @PaneItem annotated class to the items array"
            )
        }
    }

    // =========================================================================
    // Route Validations
    // =========================================================================

    /**
     * Validates that route parameters have matching constructor parameters.
     * Only @Argument-annotated params not in route are errors (they're intended for deep linking).
     * Regular constructor params without @Argument can be passed programmatically.
     */
    private fun validateRouteParameters(destinations: List<DestinationInfo>) {
        destinations.forEach { destination ->
            if (destination.route != null) {
                val constructorParamNames = destination.constructorParams.map { it.name }.toSet()
                val argumentParamNames = destination.constructorParams
                    .filter { it.isArgument }
                    .map { it.name }
                    .toSet()

                // Check route params have matching constructor params
                destination.routeParams.forEach { routeParam ->
                    if (routeParam !in constructorParamNames) {
                        reportError(
                            destination.classDeclaration,
                            "Route param '{$routeParam}' in @Destination on ${destination.className} " +
                                "has no matching constructor parameter",
                            "Add a constructor parameter named '$routeParam' or remove '{$routeParam}' from the route"
                        )
                    }
                }

                // Error about @Argument params not in route (only for params explicitly marked @Argument)
                if (destination.isDataClass) {
                    val routeParamSet = destination.routeParams.toSet()
                    argumentParamNames
                        .filter { it !in routeParamSet }
                        .forEach { missingParam ->
                            reportError(
                                destination.classDeclaration,
                                "@Argument param '$missingParam' in ${destination.className} " +
                                    "is not in route pattern '${destination.route}'",
                                "Add '{$missingParam}' to the route pattern or remove @Argument annotation"
                            )
                        }
                }
            }
        }
    }

    /**
     * Validates that no two destinations have the same route pattern.
     */
    private fun validateDuplicateRoutes(destinations: List<DestinationInfo>) {
        val routeToDestinations = destinations
            .filter { it.route != null }
            .groupBy { it.route }

        routeToDestinations.filter { it.value.size > 1 }.forEach { (route, dests) ->
            val destNames = dests.map { it.className }
            dests.forEach { dest ->
                val otherDests = destNames.filter { it != dest.className }
                reportError(
                    dest.classDeclaration,
                    "Duplicate route '$route' - also used by: ${otherDests.joinToString(", ")}",
                    "Use a unique route pattern for this destination"
                )
            }
        }
    }

    // =========================================================================
    // Argument Validations
    // =========================================================================

    /**
     * Validates @Argument annotation usage on constructor parameters.
     *
     * Checks:
     * - Optional argument must have a default value
     * - Path parameters cannot be marked as optional
     * - Argument key must match route parameter if specified
     * - No duplicate argument keys within a destination
     */
    private fun validateArgumentAnnotations(destinations: List<DestinationInfo>) {
        destinations.forEach { destination ->
            val argumentParams = destination.constructorParams.filter { it.isArgument }
            val routeParams = destination.routeParams.toSet()
            val pathParams = extractPathParams(destination.route)
            val seenKeys = mutableSetOf<String>()

            argumentParams.forEach { param ->
                val key = param.argumentKey

                // Check for duplicate keys
                if (key in seenKeys) {
                    reportError(
                        destination.classDeclaration,
                        "Duplicate argument key '$key' in ${destination.className}",
                        "Use unique keys for each @Argument parameter"
                    )
                }
                seenKeys.add(key)

                // Optional argument must have default value
                if (param.isOptionalArgument && !param.hasDefault) {
                    reportError(
                        destination.classDeclaration,
                        "@Argument(optional = true) on '${param.name}' in ${destination.className} " +
                            "requires a default value",
                        "Add a default value: ${param.name}: ${param.type} = defaultValue"
                    )
                }

                // Path parameter cannot be optional
                if (param.isOptionalArgument && key in pathParams) {
                    reportError(
                        destination.classDeclaration,
                        "Path parameter '{$key}' in ${destination.className} cannot be optional",
                        "Move this parameter to query parameters (after '?') or remove @Argument(optional = true)"
                    )
                }

                // If route exists and key is specified, it should match a route param
                if (destination.route != null && key.isNotEmpty() && key !in routeParams) {
                    // Error if there are route params - argument key should match
                    if (routeParams.isNotEmpty()) {
                        reportError(
                            destination.classDeclaration,
                            "@Argument key '$key' on '${param.name}' in ${destination.className} " +
                                "is not found in route pattern '${destination.route}'",
                            "Add '{$key}' to the route pattern, or change the argument key to match: $routeParams"
                        )
                    }
                }
            }
        }
    }

    /**
     * Extract path parameters (not query parameters) from a route pattern.
     *
     * Path parameters are those in the URL path, not after the '?' query separator.
     * Example: "user/{userId}/post/{postId}?tab={tab}" → ["userId", "postId"]
     *
     * @param route The route pattern string or null
     * @return Set of path parameter names
     */
    private fun extractPathParams(route: String?): Set<String> {
        if (route == null) return emptySet()
        val pathPart = route.substringBefore('?')
        val regex = Regex("\\{([^}]+)\\}")
        return regex.findAll(pathPart).map { it.groupValues[1] }.toSet()
    }

    /**
     * Validates that a rootGraph class has the @Stack annotation.
     */
    private fun validateRootGraphClass(
        rootGraphClass: KSClassDeclaration,
        usageSite: KSClassDeclaration
    ) {
        val hasStackAnnotation = rootGraphClass.annotations.any {
            it.shortName.asString() == "Stack"
        }

        if (!hasStackAnnotation) {
            reportError(
                usageSite,
                "rootGraph '${rootGraphClass.simpleName.asString()}' is not annotated with @Stack",
                "Add @Stack annotation to ${rootGraphClass.simpleName.asString()}"
            )
        }
    }

    /**
     * Validates screen bindings:
     * - @Screen must reference a valid @Destination class
     * - Each destination should have at most one @Screen (error if duplicates)
     * - Error if destination has no @Screen (destinations must be rendered)
     */
    private fun validateScreenBindings(
        screens: List<ScreenInfo>,
        destinations: List<DestinationInfo>
    ) {
        val destinationQualifiedNames = destinations.map { it.qualifiedName }.toSet()
        val screenDestinations = mutableMapOf<String, MutableList<ScreenInfo>>()

        // Check each @Screen references a valid destination
        screens.forEach { screen ->
            val destName = screen.destinationClass.qualifiedName?.asString() ?: return@forEach

            if (destName !in destinationQualifiedNames) {
                reportError(
                    screen.functionDeclaration,
                    "@Screen(${screen.destinationClass.simpleName.asString()}::class) " +
                        "references a class without @Destination",
                    "Add @Destination annotation to ${screen.destinationClass.simpleName.asString()} " +
                        "or reference a valid destination"
                )
            }

            screenDestinations.getOrPut(destName) { mutableListOf() }.add(screen)
        }

        // Check for duplicate screen bindings
        screenDestinations.filter { it.value.size > 1 }.forEach { (_, screenList) ->
            val screenNames = screenList.map { it.functionName }
            screenList.forEach { screen ->
                reportError(
                    screen.functionDeclaration,
                    "Multiple @Screen bindings for ${screen.destinationClass.simpleName.asString()}: " +
                        "${screenNames.joinToString(", ")}",
                    "Keep only one @Screen function for this destination"
                )
            }
        }

        // Error: destinations must have screens
        val boundDestinations = screenDestinations.keys
        destinations.forEach { destination ->
            if (destination.qualifiedName !in boundDestinations) {
                reportError(
                    destination.classDeclaration,
                    "Missing @Screen binding for '${destination.className}'",
                    "Add a @Composable function annotated with @Screen(${destination.className}::class)"
                )
            }
        }
    }

    // =========================================================================
    // Type Validations
    // =========================================================================

    /**
     * Validates that @Stack, @Tabs, and @Pane are applied to sealed classes.
     */
    private fun validateContainerTypes(
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>
    ) {
        stacks.forEach { stack ->
            if (!stack.classDeclaration.isSealed()) {
                reportError(
                    stack.classDeclaration,
                    "@Stack '${stack.className}' must be a sealed class",
                    "Change 'class ${stack.className}' to 'sealed class ${stack.className}'"
                )
            }
        }

        tabs.forEach { tab ->
            if (!tab.classDeclaration.isSealed()) {
                reportError(
                    tab.classDeclaration,
                    "@Tabs '${tab.className}' must be a sealed class",
                    "Change 'class ${tab.className}' to 'sealed class ${tab.className}'"
                )
            }
        }

        panes.forEach { pane ->
            if (!pane.classDeclaration.isSealed()) {
                reportError(
                    pane.classDeclaration,
                    "@Pane '${pane.className}' must be a sealed class",
                    "Change 'class ${pane.className}' to 'sealed class ${pane.className}'"
                )
            }
        }
    }

    /**
     * Validates that @Destination is applied to data objects or data classes.
     *
     * Sealed classes that are also containers (@Tabs, @Stack, @Pane) are allowed
     * to have @Destination for deep linking purposes but are not validated here.
     */
    private fun validateDestinationTypes(destinations: List<DestinationInfo>) {
        destinations.forEach { destination ->
            // Skip sealed classes that are also containers - they're valid with @Destination
            val isContainer = destination.classDeclaration.annotations.any {
                val name = it.shortName.asString()
                name == "Tabs" || name == "Tab" || name == "Stack" || name == "Pane"
            }
            if (isContainer) return@forEach

            // Also skip plain sealed classes - they may be container markers
            if (destination.isSealedClass) return@forEach

            if (!destination.isDataObject && !destination.isDataClass) {
                reportError(
                    destination.classDeclaration,
                    "@Destination '${destination.className}' must be a data object or data class",
                    "Use 'data object ${destination.className}' for destinations without parameters, " +
                        "or 'data class ${destination.className}(...)' for destinations with parameters"
                )
            }
        }
    }

    // =========================================================================
    // Mixed Tab Type Validations
    // =========================================================================

    /**
     * Validates that each @TabItem has valid annotation combinations.
     *
     * A @TabItem must have exactly one of:
     * - @Stack (for nested navigation with NESTED_STACK type)
     * - @Destination (for flat screen with FLAT_SCREEN type)
     *
     * Having both or neither is an error.
     */
    private fun validateTabItemAnnotations(tabs: List<TabInfo>) {
        tabs.forEach { tab ->
            tab.tabs.forEach { tabItem ->
                val hasStack = tabItem.stackInfo != null ||
                    tabItem.classDeclaration.annotations.any { it.shortName.asString() == "Stack" }
                val hasDestination = tabItem.destinationInfo != null ||
                    tabItem.classDeclaration.annotations.any { it.shortName.asString() == "Destination" }

                when {
                    hasStack && hasDestination -> {
                        reportError(
                            tabItem.classDeclaration,
                            "@TabItem '${tabItem.classDeclaration.simpleName.asString()}' " +
                                "has both @Stack and @Destination",
                            "Use @Stack for nested navigation OR @Destination for flat screen, not both"
                        )
                    }
                    !hasStack && !hasDestination -> {
                        reportError(
                            tabItem.classDeclaration,
                            "@TabItem '${tabItem.classDeclaration.simpleName.asString()}' " +
                                "has neither @Stack nor @Destination",
                            "Add @Stack for nested navigation or @Destination for flat screen"
                        )
                    }
                }
            }
        }
    }

    /**
     * Validates that NESTED_STACK tabs have a valid @Stack with destinations.
     *
     * For tabs with [TabItemType.NESTED_STACK], the tab class must:
     * - Be annotated with @Stack
     * - Have at least one @Destination subclass
     */
    private fun validateNestedStackTabs(tabs: List<TabInfo>) {
        tabs.forEach { tab ->
            tab.tabs.filter { it.tabType == TabItemType.NESTED_STACK }.forEach { tabItem ->
                val stackInfo = tabItem.stackInfo
                if (stackInfo == null) {
                    reportError(
                        tabItem.classDeclaration,
                        "NESTED_STACK tab '${tabItem.classDeclaration.simpleName.asString()}' " +
                            "is missing @Stack annotation",
                        "Add @Stack annotation to this tab class"
                    )
                } else if (stackInfo.destinations.isEmpty()) {
                    reportError(
                        tabItem.classDeclaration,
                        "@Stack '${stackInfo.name}' on NESTED_STACK tab " +
                            "'${tabItem.classDeclaration.simpleName.asString()}' has no destinations",
                        "Add at least one @Destination subclass to this Stack"
                    )
                }
            }
        }
    }

    /**
     * Validates that FLAT_SCREEN tabs are data objects with valid destinations.
     *
     * For tabs with [TabItemType.FLAT_SCREEN], the tab class must:
     * - Be a data object
     * - Be annotated with @Destination
     * - Have a route (warning if missing, for deep linking support)
     */
    private fun validateFlatScreenTabs(tabs: List<TabInfo>) {
        tabs.forEach { tab ->
            tab.tabs.filter { it.tabType == TabItemType.FLAT_SCREEN }.forEach { tabItem ->
                val classDecl = tabItem.classDeclaration

                // Must be data object
                val isDataObject = classDecl.classKind == ClassKind.OBJECT &&
                    classDecl.modifiers.contains(Modifier.DATA)

                if (!isDataObject) {
                    reportError(
                        classDecl,
                        "FLAT_SCREEN tab '${classDecl.simpleName.asString()}' must be a data object",
                        "Change to 'data object ${classDecl.simpleName.asString()}'"
                    )
                }

                // Must have @Destination with route
                val destInfo = tabItem.destinationInfo
                if (destInfo == null) {
                    reportError(
                        classDecl,
                        "FLAT_SCREEN tab '${classDecl.simpleName.asString()}' is missing @Destination",
                        "Add @Destination annotation with a route"
                    )
                } else if (destInfo.route.isNullOrEmpty()) {
                    // Keep as warning since it still works without route
                    reportWarning(
                        classDecl,
                        "@Destination on FLAT_SCREEN tab '${classDecl.simpleName.asString()}' has no route",
                        "Add a route parameter for deep linking support"
                    )
                }
            }
        }
    }

    // =========================================================================
    // Helper Functions
    // =========================================================================

    /**
     * Checks if a class declaration has the SEALED modifier.
     */
    private fun KSClassDeclaration.isSealed(): Boolean =
        modifiers.contains(Modifier.SEALED)

    // =========================================================================
    // Error Reporting
    // =========================================================================

    /**
     * Formats a validation message with file location and fix suggestion.
     * Format: "{Description} in file '{fileName}' (line {lineNumber}). Fix: {Suggestion}"
     */
    private fun formatMessage(description: String, fix: String, node: KSNode): String {
        val location = node.location as? FileLocation
        val fileName = location?.filePath?.substringAfterLast('/') ?: "unknown"
        val lineNumber = location?.lineNumber ?: 0
        return if (lineNumber > 0) {
            "$description in file '$fileName' (line $lineNumber). Fix: $fix"
        } else {
            "$description in file '$fileName'. Fix: $fix"
        }
    }

    private fun reportError(node: KSNode, description: String, fix: String) {
        hasErrors = true
        logger.error(formatMessage(description, fix, node), node)
    }

    private fun reportWarning(node: KSNode, description: String, fix: String) {
        logger.warn(formatMessage(description, fix, node), node)
    }
}
