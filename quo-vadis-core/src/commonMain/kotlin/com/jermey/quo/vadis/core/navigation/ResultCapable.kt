package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.InternalQuoVadisApi

/**
 * Internal interface for navigators that support result passing.
 *
 * Used internally by [navigateForResult] and [navigateBackWithResult]
 * extension functions. Not intended for direct use.
 *
 * @suppress This is an internal API
 */
@InternalQuoVadisApi
interface ResultCapable {
    /**
     * Manager for navigation result passing between screens.
     */
    val resultManager: NavigationResultManager
}
