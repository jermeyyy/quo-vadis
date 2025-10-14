package com.jermey.quo.vadis.core.navigation.utils

/**
 * iOS implementation of NavigationLogger using println.
 */
internal actual object NavigationLogger {
    actual fun d(tag: String, message: String) {
        println("[$tag] $message")
    }
}
