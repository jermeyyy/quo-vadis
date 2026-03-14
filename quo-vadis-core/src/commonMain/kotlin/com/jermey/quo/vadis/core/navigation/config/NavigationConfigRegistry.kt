package com.jermey.quo.vadis.core.navigation.config

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import kotlin.reflect.KClass

/**
 * Internal registry for NavigationConfig instances populated by generated code.
 *
 * KSP-generated aggregated configs register themselves here during class loading.
 * The [navigationConfig] function checks this registry at runtime when the compiler
 * plugin's compile-time rewrite is not active.
 *
 * The compiler plugin bypasses this registry entirely — it rewrites call sites to
 * reference the generated object directly. The registry exists solely to support
 * the KSP backend.
 */
@InternalQuoVadisApi
object NavigationConfigRegistry {
    private val configs = mutableMapOf<KClass<*>, NavigationConfig>()

    fun register(rootClass: KClass<*>, config: NavigationConfig) {
        configs[rootClass] = config
    }

    fun get(rootClass: KClass<*>): NavigationConfig? = configs[rootClass]
}
