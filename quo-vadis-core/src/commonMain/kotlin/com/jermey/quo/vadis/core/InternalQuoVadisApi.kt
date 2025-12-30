package com.jermey.quo.vadis.core

/**
 * Marks an API as internal to Quo Vadis library.
 *
 * APIs marked with this annotation are not intended for public use and may change
 * without notice. Using these APIs requires explicit opt-in.
 */
@Suppress("ExperimentalAnnotationRetention")
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Quo Vadis API that should not be used from outside the library."
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.BINARY)
annotation class InternalQuoVadisApi
