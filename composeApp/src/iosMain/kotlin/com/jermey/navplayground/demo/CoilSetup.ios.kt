package com.jermey.navplayground.demo

import coil3.PlatformContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * Get platform-specific cache directory for iOS.
 */
internal actual fun getCacheDirectory(context: PlatformContext): String {
    val paths = NSSearchPathForDirectoriesInDomains(
        NSCachesDirectory,
        NSUserDomainMask,
        true
    )
    return (paths.firstOrNull() as? String ?: "") + "/coil_cache"
}
