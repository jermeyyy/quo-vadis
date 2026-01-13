package com.jermey.navplayground.demo

import coil3.PlatformContext

/**
 * Get platform-specific cache directory for Android.
 */
internal actual fun getCacheDirectory(context: PlatformContext): String {
    return context.cacheDir.absolutePath + "/coil_cache"
}
