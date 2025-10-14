package com.jermey.quo.vadis.core.navigation.utils

import android.util.Log

/**
 * Android implementation of NavigationLogger using Android Log.
 */
internal actual object NavigationLogger {
    actual fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
}
