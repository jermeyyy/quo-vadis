package com.jermey.navplayground.demo

import coil3.PlatformContext

/**
 * Get platform-specific cache directory for WASM JS.
 * Note: WASM/JS doesn't support file-based disk caching, so return empty string.
 * Coil will use memory-only caching on these platforms.
 */
internal actual fun getCacheDirectory(context: PlatformContext): String {
    // WASM JS doesn't have filesystem access for disk caching
    // Coil falls back to memory-only caching when disk cache path is unavailable
    return ""
}
