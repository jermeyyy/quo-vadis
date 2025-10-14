package com.jermey.quo.vadis.core.navigation.utils

/**
 * Platform-agnostic logger for navigation debugging.
 */
internal expect object NavigationLogger {
    fun d(tag: String, message: String)
}

/**
 * Log navigation debug information.
 */
internal fun logNav(message: String) {
    NavigationLogger.d("NAV_DEBUG", message)
}
