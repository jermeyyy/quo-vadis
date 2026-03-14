package com.jermey.quo.vadis.ksp.discovery

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Discovers @GeneratedConfig-annotated NavigationConfig implementations
 * from compiled dependency modules on the classpath.
 *
 * Uses [Resolver.getDeclarationsFromPackage] to find classes in the generated
 * package and filters by annotation presence.
 */
class ClasspathConfigDiscovery(
    private val logger: KSPLogger
) {
    /**
     * Discovers config class names from the classpath.
     *
     * Scans the generated package for classes annotated with @GeneratedConfig,
     * excluding aggregated configs and internal types.
     *
     * @param resolver KSP resolver
     * @param currentModuleConfigName The current module's config simple name to exclude
     * @return List of fully qualified class names of discovered configs, sorted by FQN
     */
    @OptIn(com.google.devtools.ksp.KspExperimental::class)
    fun discoverConfigs(
        resolver: Resolver,
        currentModuleConfigName: String
    ): List<String> {
        val discoveredConfigs = mutableListOf<String>()

        val declarations = resolver.getDeclarationsFromPackage(GENERATED_PACKAGE)

        declarations.filterIsInstance<KSClassDeclaration>().forEach { declaration ->
            val qualifiedName = declaration.qualifiedName?.asString() ?: return@forEach

            // Exclude aggregated configs
            if (qualifiedName.endsWith("__AggregatedConfig")) return@forEach

            // Exclude internal types
            if (qualifiedName.endsWith("EmptyNavigationConfig") ||
                qualifiedName.endsWith("CompositeNavigationConfig")
            ) return@forEach

            // Exclude current module's config (it will be added separately)
            if (qualifiedName == "$GENERATED_PACKAGE.$currentModuleConfigName") return@forEach

            // Check for @GeneratedConfig annotation
            val hasAnnotation = declaration.annotations.any { annotation ->
                annotation.annotationType.resolve().declaration.qualifiedName?.asString() ==
                    GENERATED_CONFIG_FQN
            }

            if (hasAnnotation) {
                discoveredConfigs.add(qualifiedName)
                logger.info("QuoVadis: Discovered dependency config: $qualifiedName")
            }
        }

        // Sort for deterministic output
        return discoveredConfigs.sorted()
    }

    private companion object {
        const val GENERATED_PACKAGE = "com.jermey.quo.vadis.generated"
        const val GENERATED_CONFIG_FQN = "com.jermey.quo.vadis.core.navigation.config.GeneratedConfig"
    }
}
