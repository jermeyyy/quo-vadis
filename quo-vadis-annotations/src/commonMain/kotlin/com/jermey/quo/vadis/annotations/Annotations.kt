package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a sealed class as a navigation graph.
 * 
 * The sealed class should extend Destination and contain destination objects/classes
 * representing the screens in this graph.
 * 
 * @param name The unique identifier for this navigation graph
 * 
 * @sample
 * ```
 * @Graph("master_detail")
 * sealed class MasterDetailDestination : Destination
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Graph(val name: String)

/**
 * Specifies the route path for a destination.
 * 
 * The route is used for navigation and deep linking.
 * 
 * @param path The route path (e.g., "master_detail/list")
 * 
 * @sample
 * ```
 * @Route("master_detail/list")
 * object List : MasterDetailDestination()
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Route(val path: String)

/**
 * Specifies the data class type for typed destinations.
 * 
 * The data class should be serializable (using kotlinx.serialization).
 * 
 * @param dataClass The KClass of the serializable data type
 * 
 * @sample
 * ```
 * @Route("master_detail/detail")
 * @Argument(DetailData::class)
 * data class Detail(val data: DetailData) : MasterDetailDestination()
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Argument(val dataClass: KClass<*>)

/**
 * Marks a Composable function as the content renderer for a specific destination.
 * 
 * The Composable function signature should match the destination type:
 * - For simple destinations: `@Composable (Navigator) -> Unit`
 * - For typed destinations: `@Composable (DataType, Navigator) -> Unit`
 * 
 * KSP will automatically generate graph builder code that wires this Composable
 * function to the specified destination.
 * 
 * @param destination The destination class that this Composable renders
 * 
 * @sample
 * ```
 * @Content(MasterDetailDestination.Detail::class)
 * @Composable
 * fun DetailScreen(data: DetailData, navigator: Navigator) {
 *     // UI implementation
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Content(val destination: KClass<*>)
