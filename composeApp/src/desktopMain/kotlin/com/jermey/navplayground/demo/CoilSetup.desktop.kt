package com.jermey.navplayground.demo

import coil3.PlatformContext
import java.io.File

/**
 * Get platform-specific cache directory for Desktop (JVM).
 */
internal actual fun getCacheDirectory(context: PlatformContext): String {
    val userHome = System.getProperty("user.home")
    val cacheDir = File(userHome, ".cache/navplayground/coil_cache")
    cacheDir.mkdirs()
    return cacheDir.absolutePath
}
