package com.jermey.quo.vadis.ksp.validation

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
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
 * providing clear error messages with source locations to help developers quickly identify
 * and fix configuration issues at compile time.
 *
 * ## Validation Categories
 *
 * The engine validates across four main categories:
 *
 * ### 1. Structural Validations
 * - Orphan destinations (not inside @Stack, @Tab, or @Pane)
 * - Invalid start destination references
 * - Invalid initial tab references
 * - Empty containers (no destinations)
 *
 * ### 2. Route Validations
 * - Route parameter mismatches (route param without constructor param)
 * - Duplicate routes across destinations
 *
 * ### 3. Reference Validations
 * - Invalid root graph references (@TabItem/@PaneItem referencing non-@Stack class)
 * - Missing screen bindings (warning)
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
        validateOrphanDestinations(allDestinations, stacks, tabs, panes)
        validateContainerStartDestinations(stacks)
        validateTabInitialTabs(tabs)
        validateEmptyContainers(stacks, tabs, panes)

        // Route validations
        validateRouteParameters(allDestinations)
        validateDuplicateRoutes(allDestinations)

        // Argument validations
        validateArgumentAnnotations(allDestinations)

        // Reference validations
        validateRootGraphReferences(tabs, panes)
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
     * Validates that @Destination classes are contained within a @Stack, @Tab, or @Pane.
     *
     * Note: Standalone destinations (not inside any container) are allowed but produce
     * a warning. These are useful for destinations that can be navigated to from
     * anywhere (e.g., detail screens that can be pushed onto any stack).
     */
    private fun validateOrphanDestinations(
        destinations: List<DestinationInfo>,
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>
    ) {
        val containedDestinations = mutableSetOf<String>()

        stacks.forEach { stack ->
            stack.destinations.forEach { containedDestinations.add(it.qualifiedName) }
        }
        // Collect destinations from FLAT_SCREEN tabs
        tabs.forEach { tab ->
            tab.tabs.forEach { tabItem ->
                // FLAT_SCREEN tabs have destinationInfo
                tabItem.destinationInfo?.let { containedDestinations.add(it.qualifiedName) }
            }
        }
        panes.forEach { pane ->
            pane.panes.forEach { containedDestinations.add(it.destination.qualifiedName) }
        }

        destinations.forEach { destination ->
            if (destination.qualifiedName !in containedDestinations) {
                // Standalone destinations are allowed but produce a warning
                // These can be navigated to from any stack
                reportWarning(
                    destination.classDeclaration,
                    "@Destination \"${destination.className}\" is a standalone destination " +
                        "(not inside @Stack, @Tab, or @Pane). This is allowed but ensure it " +
                        "has a valid @Screen binding."
                )
            }
        }
    }

    /**
     * Validates that @Stack(startDestinationLegacy) references an existing destination.
     */
    private fun validateContainerStartDestinations(stacks: List<StackInfo>) {
        stacks.forEach { stack ->
            if (stack.resolvedStartDestination == null) {
                val availableDestinations = stack.destinations.map { it.className }
                reportError(
                    stack.classDeclaration,
                    "@Stack(startDestinationLegacy = \"${stack.startDestination}\") - " +
                        "No destination named \"${stack.startDestination}\" found in ${stack.className}. " +
                        "Available destinations: $availableDestinations"
                )
            }
        }
    }

    /**
     * Validates that @Tab(initialTab) references an existing tab item.
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
                        "@Tab(initialTab = ${tab.initialTabClass.simpleName.asString()}::class) - " +
                            "Class not found in items/tabs. " +
                            "Available tabs: $availableTabs"
                    )
                }
            }
        }
    }

    /**
     * Validates that @Stack, @Tab, and @Pane containers have at least one destination.
     */
    private fun validateEmptyContainers(
        stacks: List<StackInfo>,
        tabs: List<TabInfo>,
        panes: List<PaneInfo>
    ) {
        stacks.filter { it.destinations.isEmpty() }.forEach { stack ->
            reportError(
                stack.classDeclaration,
                "@Stack on \"${stack.className}\" - Stack must contain at least one @Destination"
            )
        }

        tabs.filter { it.tabs.isEmpty() }.forEach { tab ->
            reportError(
                tab.classDeclaration,
                "@Tab on \"${tab.className}\" - Tab container must contain at least one @TabItem"
            )
        }

        panes.filter { it.panes.isEmpty() }.forEach { pane ->
            reportError(
                pane.classDeclaration,
                "@Pane on \"${pane.className}\" - Pane container must contain at least one @PaneItem"
            )
        }
    }

    // =========================================================================
    // Route Validations
    // =========================================================================

    /**
     * Validates that route parameters have matching constructor parameters.
     * Also warns about constructor params not in route (won't be available via deep linking).
     */
    private fun validateRouteParameters(destinations: List<DestinationInfo>) {
        destinations.forEach { destination ->
            if (destination.route != null) {
                val constructorParamNames = destination.constructorParams.map { it.name }.toSet()

                // Check route params have matching constructor params
                destination.routeParams.forEach { routeParam ->
                    if (routeParam !in constructorParamNames) {
                        reportError(
                            destination.classDeclaration,
                            "@Destination(route = \"${destination.route}\") on ${destination.className} - " +
                                "Route param \"{$routeParam}\" has no matching constructor parameter. " +
                                "Available params: $constructorParamNames"
                        )
                    }
                }

                // Warn about constructor params not in route (only for data classes)
                if (destination.isDataClass) {
                    val routeParamSet = destination.routeParams.toSet()
                    constructorParamNames
                        .filter { it !in routeParamSet }
                        .forEach { missingParam ->
                            reportWarning(
                                destination.classDeclaration,
                                "Constructor param \"$missingParam\" in ${destination.className} " +
                                    "is not in route pattern. This param won't be available via deep linking."
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
            val destNames = dests.map { it.qualifiedName }
            dests.forEach { dest ->
                reportError(
                    dest.classDeclaration,
                    "Duplicate route \"$route\" found on: ${destNames.joinToString(", ")}. " +
                        "Each destination must have a unique route pattern."
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
                        "Duplicate argument key \"$key\" in ${destination.className}. " +
                            "Each @Argument must have a unique key."
                    )
                }
                seenKeys.add(key)

                // Optional argument must have default value
                if (param.isOptionalArgument && !param.hasDefault) {
                    reportError(
                        destination.classDeclaration,
                        "@Argument(optional = true) on \"${param.name}\" in ${destination.className} " +
                            "requires a default value"
                    )
                }

                // Path parameter cannot be optional
                if (param.isOptionalArgument && key in pathParams) {
                    reportError(
                        destination.classDeclaration,
                        "Path parameter \"{$key}\" in ${destination.className} cannot be optional. " +
                            "Only query parameters can be marked as @Argument(optional = true)"
                    )
                }

                // If route exists and key is specified, it should match a route param
                if (destination.route != null && key.isNotEmpty() && key !in routeParams) {
                    // Only warn if there are route params - otherwise the argument might be
                    // used for internal state without deep linking
                    if (routeParams.isNotEmpty()) {
                        reportWarning(
                            destination.classDeclaration,
                            "@Argument key \"$key\" on \"${param.name}\" in ${destination.className} " +
                                "is not found in route pattern \"${destination.route}\". " +
                                "This argument won't be available via deep linking."
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

    // =========================================================================
    // Reference Validations
    // =========================================================================

    /**
     * Validates that rootGraph references in @PaneItem point to @Stack classes.
     * 
     * Note: For tabs in the new pattern, NESTED_STACK tabs have stackInfo which contains
     * the @Stack class. Validation for tabs with @Stack is done separately in validateTabItems.
     */
    private fun validateRootGraphReferences(
        tabs: List<TabInfo>,
        panes: List<PaneInfo>
    ) {
        // Note: Tab validation for NESTED_STACK is handled via stackInfo in validateTabItems
        // No legacy rootGraphClass validation needed anymore

        panes.forEach { pane ->
            pane.panes.forEach { paneItem ->
                validateRootGraphClass(paneItem.rootGraphClass, paneItem.destination.classDeclaration)
            }
        }
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
                "rootGraph = ${rootGraphClass.simpleName.asString()}::class - " +
                    "Referenced class must be annotated with @Stack"
            )
        }
    }

    /**
     * Validates screen bindings:
     * - @Screen must reference a valid @Destination class
     * - Each destination should have at most one @Screen (error if duplicates)
     * - Warn if destination has no @Screen
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
                    "@Screen(${screen.destinationClass.simpleName.asString()}::class) - " +
                        "Referenced class is not annotated with @Destination"
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
                        "${screenNames.joinToString(", ")}. Each destination can only have one screen."
                )
            }
        }

        // Warn about destinations without screens
        val boundDestinations = screenDestinations.keys
        destinations.forEach { destination ->
            if (destination.qualifiedName !in boundDestinations) {
                reportWarning(
                    destination.classDeclaration,
                    "No @Screen found for ${destination.className} - " +
                        "This destination will have no content. " +
                        "Add a @Screen function to render this destination."
                )
            }
        }
    }

    // =========================================================================
    // Type Validations
    // =========================================================================

    /**
     * Validates that @Stack, @Tab, and @Pane are applied to sealed classes.
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
                    "@Stack on \"${stack.className}\" - Must be applied to a sealed class"
                )
            }
        }

        tabs.forEach { tab ->
            if (!tab.classDeclaration.isSealed()) {
                reportError(
                    tab.classDeclaration,
                    "@Tab on \"${tab.className}\" - Must be applied to a sealed class"
                )
            }
        }

        panes.forEach { pane ->
            if (!pane.classDeclaration.isSealed()) {
                reportError(
                    pane.classDeclaration,
                    "@Pane on \"${pane.className}\" - Must be applied to a sealed class"
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
                    "@Destination on \"${destination.className}\" - " +
                        "Must be applied to a data object or data class"
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
                            "@TabItem '${tabItem.classDeclaration.simpleName.asString()}' cannot have both " +
                                "@Stack and @Destination. Use @Stack for nested navigation or @Destination for flat screen."
                        )
                    }
                    !hasStack && !hasDestination -> {
                        reportError(
                            tabItem.classDeclaration,
                            "@TabItem '${tabItem.classDeclaration.simpleName.asString()}' must have either " +
                                "@Stack (for nested navigation) or @Destination (for flat screen)."
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
                            "must be annotated with @Stack"
                    )
                } else if (stackInfo.destinations.isEmpty()) {
                    reportError(
                        tabItem.classDeclaration,
                        "@Stack '${stackInfo.name}' on tab '${tabItem.classDeclaration.simpleName.asString()}' " +
                            "has no @Destination subclasses"
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
                        "FLAT_SCREEN tab '${classDecl.simpleName.asString()}' must be a data object"
                    )
                }

                // Must have @Destination with route
                val destInfo = tabItem.destinationInfo
                if (destInfo == null) {
                    reportError(
                        classDecl,
                        "FLAT_SCREEN tab '${classDecl.simpleName.asString()}' must have @Destination"
                    )
                } else if (destInfo.route.isNullOrEmpty()) {
                    reportWarning(
                        classDecl,
                        "@Destination on FLAT_SCREEN tab '${classDecl.simpleName.asString()}' " +
                            "should have a route for deep linking"
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

    private fun reportError(node: KSClassDeclaration, message: String) {
        hasErrors = true
        logger.error(message, node)
    }

    private fun reportError(node: KSFunctionDeclaration, message: String) {
        hasErrors = true
        logger.error(message, node)
    }

    private fun reportWarning(node: KSClassDeclaration, message: String) {
        logger.warn(message, node)
    }
}
