package com.jermey.quo.vadis.core.dsl

import com.jermey.quo.vadis.core.navigation.NavDestination
import kotlin.reflect.KClass

/**
 * DSL builder for configuring stack container screens.
 *
 * Stack containers represent linear navigation with push/pop semantics.
 * This builder allows specifying which screens belong to the stack and
 * their configuration.
 *
 * ## Usage
 *
 * ```kotlin
 * stack<MainStack>("main") {
 *     // Add screen by instance
 *     screen(HomeScreen, key = "home")
 *
 *     // Add screen by type (preferred)
 *     screen<DetailScreen>()
 *     screen<SettingsScreen>()
 * }
 * ```
 *
 * @see NavigationConfigBuilder.stack
 * @see StackScreenEntry
 */
@NavigationConfigDsl
class StackBuilder {

    /**
     * List of screen entries configured for this stack.
     */
    @PublishedApi
    internal val screens: MutableList<StackScreenEntry> = mutableListOf()

    /**
     * Adds a screen by destination instance.
     *
     * Use this when you have a specific destination instance (e.g., data object).
     *
     * ## Example
     *
     * ```kotlin
     * screen(HomeScreen, key = "home")
     * screen(SettingsScreen) // key auto-generated from class name
     * ```
     *
     * @param destination The destination instance
     * @param key Optional explicit key (auto-generated from class name if null)
     */
    fun screen(
        destination: NavDestination,
        key: String = destination::class.simpleName ?: "screen-${screens.size}"
    ) {
        screens.add(
            StackScreenEntry(
                destination = destination,
                destinationClass = destination::class,
                key = key
            )
        )
    }

    /**
     * Adds a screen by destination type.
     *
     * This is the preferred way to add screens when you don't need a specific
     * instance. The actual destination instance will be resolved at navigation time.
     *
     * ## Example
     *
     * ```kotlin
     * screen<HomeScreen>()
     * screen<DetailScreen>(key = "detail")
     * ```
     *
     * @param D The destination type
     * @param key Optional explicit key (auto-generated from class name if null)
     */
    inline fun <reified D : NavDestination> screen(key: String? = null) {
        screens.add(
            StackScreenEntry(
                destination = null,
                destinationClass = D::class,
                key = key ?: D::class.simpleName ?: "screen-${screens.size}"
            )
        )
    }

    /**
     * Builds the list of stack screen entries.
     *
     * @return List of configured [StackScreenEntry] instances
     */
    fun build(): List<StackScreenEntry> = screens.toList()
}

/**
 * Represents a screen entry within a stack container.
 *
 * Each entry can specify either a concrete destination instance or just the
 * destination class. The key is used for identification and state restoration.
 *
 * @property destination Optional concrete destination instance
 * @property destinationClass The class of the destination (always provided)
 * @property key Unique identifier for this screen entry
 */
data class StackScreenEntry(
    val destination: NavDestination? = null,
    val destinationClass: KClass<out NavDestination>? = null,
    val key: String
)
