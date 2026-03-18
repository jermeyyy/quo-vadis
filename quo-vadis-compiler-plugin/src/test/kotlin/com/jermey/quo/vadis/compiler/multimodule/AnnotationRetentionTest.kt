package com.jermey.quo.vadis.compiler.multimodule

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.NavigationRoot
import com.jermey.quo.vadis.annotations.Pane
import com.jermey.quo.vadis.annotations.PaneItem
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Tabs
import com.jermey.quo.vadis.annotations.TabsContainer
import com.jermey.quo.vadis.annotations.PaneContainer
import com.jermey.quo.vadis.annotations.Transition
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests that verify annotation retention levels are correct for multi-module
 * auto-discovery (Phase 4).
 *
 * Annotations changed to BINARY retention survive compilation into .class files
 * and are readable from compiled artifacts, enabling the compiler plugin to
 * discover annotated classes in dependency modules.
 */
class AnnotationRetentionTest {

    // --- BINARY retention annotations (changed for Phase 4) ---

    @Test
    fun `Stack annotation has BINARY retention`() {
        val retention = Stack::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@Stack should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.BINARY, retention.value)
    }

    @Test
    fun `Destination annotation has BINARY retention`() {
        val retention = Destination::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@Destination should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.BINARY, retention.value)
    }

    @Test
    fun `Screen annotation has BINARY retention`() {
        val retention = Screen::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@Screen should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.BINARY, retention.value)
    }

    @Test
    fun `Tabs annotation has BINARY retention`() {
        val retention = Tabs::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@Tabs should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.BINARY, retention.value)
    }

    @Test
    fun `TabItem annotation has BINARY retention`() {
        val retention = TabItem::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@TabItem should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.BINARY, retention.value)
    }

    @Test
    fun `Pane annotation has BINARY retention`() {
        val retention = Pane::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@Pane should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.BINARY, retention.value)
    }

    @Test
    fun `PaneItem annotation has BINARY retention`() {
        val retention = PaneItem::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@PaneItem should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.BINARY, retention.value)
    }

    @Test
    fun `NavigationRoot annotation has BINARY retention`() {
        val retention = NavigationRoot::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@NavigationRoot should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.BINARY, retention.value)
    }

    // --- Negative tests: annotations that should NOT have been changed ---

    @Test
    fun `Argument annotation stays SOURCE retention`() {
        val retention = Argument::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@Argument should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.SOURCE, retention.value)
    }

    @Test
    fun `TabsContainer annotation stays RUNTIME retention`() {
        val retention = TabsContainer::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@TabsContainer should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }

    @Test
    fun `PaneContainer annotation stays RUNTIME retention`() {
        val retention = PaneContainer::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@PaneContainer should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }

    @Test
    fun `Transition annotation stays RUNTIME retention`() {
        val retention = Transition::class.annotations
            .filterIsInstance<Retention>()
            .firstOrNull()
        assertNotNull(retention, "@Transition should have @Retention meta-annotation")
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }
}
