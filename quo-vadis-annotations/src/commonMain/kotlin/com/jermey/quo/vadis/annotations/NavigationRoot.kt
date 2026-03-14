package com.jermey.quo.vadis.annotations

/**
 * Marks this module as the navigation root that auto-discovers all
 * compiler-plugin-generated [NavigationConfig] instances from dependency modules.
 *
 * When the Quo Vadis compiler plugin encounters this annotation, it:
 * 1. Scans the classpath for all `@GeneratedConfig`-annotated configs
 * 2. Aggregates them into a single `{Prefix}NavigationConfig` object
 * 3. Orders feature configs by fully-qualified name for deterministic output
 *
 * ## Usage
 *
 * Apply to any class or object in the application module:
 * ```kotlin
 * @NavigationRoot
 * object MyApp
 *
 * // Generated: MyAppNavigationConfig automatically includes ALL feature configs
 * val navigator = rememberQuoVadisNavigator(MainTabs::class, MyAppNavigationConfig)
 * ```
 *
 * ## Single-Root Constraint
 *
 * Only one `@NavigationRoot` annotation is allowed per compilation unit.
 * If multiple are found, the compiler plugin will report an error.
 *
 * ## Prefix Resolution
 *
 * The generated config's name prefix follows these rules:
 * 1. If [prefix] is non-empty, use it directly
 * 2. If [prefix] is empty (default), derive from the annotated class name
 *    (e.g., `MyApp` → `MyApp`, so generated config is `MyAppNavigationConfig`)
 *
 * @property prefix Optional prefix for the generated aggregated config name.
 *   If empty, derived from the annotated class name.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class NavigationRoot(
    val prefix: String = "",
)
