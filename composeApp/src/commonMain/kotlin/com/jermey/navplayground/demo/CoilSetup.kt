package com.jermey.navplayground.demo

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import okio.Path.Companion.toPath

/**
 * Configures the singleton Coil ImageLoader with crossfade transitions and caching.
 *
 * This should be called once at app startup, before any AsyncImage composables are used.
 */
@Suppress("FunctionNaming") // Composable functions can start with uppercase
@Composable
fun ConfigureCoilImageLoader() {
    setSingletonImageLoaderFactory { context ->
        createImageLoader(context)
    }
}

/**
 * Creates a configured ImageLoader for loading images across all platforms.
 *
 * Features:
 * - Crossfade animation enabled (250ms)
 * - Memory cache enabled (25% of app memory)
 * - Disk cache enabled (100MB) on platforms that support it
 * - Ktor networking for KMP support
 */
private fun createImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory())
        }
        .crossfade(true)
        .crossfade(CROSSFADE_DURATION_MS)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, MEMORY_CACHE_PERCENT)
                .build()
        }
        .apply {
            // Only enable disk cache on platforms that support it
            val cacheDir = getCacheDirectory(context)
            if (cacheDir.isNotEmpty()) {
                diskCachePolicy(CachePolicy.ENABLED)
                diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.toPath())
                        .maxSizeBytes(DISK_CACHE_SIZE_BYTES)
                        .build()
                }
            } else {
                diskCachePolicy(CachePolicy.DISABLED)
            }
        }
        .build()
}

/**
 * Get platform-specific cache directory.
 */
internal expect fun getCacheDirectory(context: PlatformContext): String

private const val CROSSFADE_DURATION_MS = 150
private const val MEMORY_CACHE_PERCENT = 0.25
private const val DISK_CACHE_SIZE_BYTES = 100L * 1024 * 1024 // 100MB
