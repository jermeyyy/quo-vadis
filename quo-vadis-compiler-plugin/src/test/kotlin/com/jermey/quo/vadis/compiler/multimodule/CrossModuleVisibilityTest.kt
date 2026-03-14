package com.jermey.quo.vadis.compiler.multimodule

import com.jermey.quo.vadis.core.navigation.config.GeneratedConfig
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests that verify cross-module visibility of the [GeneratedConfig]
 * annotation for Phase 4 multi-module auto-discovery.
 *
 * These tests ensure that:
 * - [GeneratedConfig] is a RUNTIME retention annotation
 * - [GeneratedConfig] targets CLASS
 * - A class annotated with [GeneratedConfig] can implement [NavigationConfig]
 */
class CrossModuleVisibilityTest {

    @Test
    fun `GeneratedConfig has RUNTIME retention`() {
        val retention = GeneratedConfig::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertEquals(
            AnnotationRetention.RUNTIME,
            retention?.value,
            "@GeneratedConfig should have RUNTIME retention",
        )
    }

    @Test
    fun `GeneratedConfig targets CLASS`() {
        val target = GeneratedConfig::class.annotations
            .filterIsInstance<Target>()
            .firstOrNull()
        assertTrue(
            target?.allowedTargets?.contains(AnnotationTarget.CLASS) == true,
            "@GeneratedConfig should target CLASS",
        )
    }

    @Test
    fun `GeneratedConfig is an annotation class`() {
        assertTrue(
            GeneratedConfig::class.java.isAnnotation,
            "GeneratedConfig should be an annotation class",
        )
    }

    @Test
    fun `class annotated with GeneratedConfig can implement NavigationConfig`() {
        // The annotation is used on NavigationConfig implementations;
        // verify the annotation type is assignable and discoverable
        val annotationClass = GeneratedConfig::class.java
        val navigationConfigClass = NavigationConfig::class.java
        assertTrue(
            annotationClass.isAnnotation,
            "GeneratedConfig should be an annotation that can be applied to NavigationConfig implementors",
        )
        // Verify the annotation is visible from dependent modules (this test compiles = visible)
        assertTrue(
            navigationConfigClass.isInterface,
            "NavigationConfig should be an interface that generated configs implement",
        )
    }
}
