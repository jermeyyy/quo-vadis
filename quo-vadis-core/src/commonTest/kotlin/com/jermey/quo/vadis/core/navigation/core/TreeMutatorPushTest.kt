package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for TreeMutator push operations.
 *
 * Tests cover:
 * - `push`: adds to deepest active stack
 * - `pushToStack`: targets specific stack by key
 * - `pushAll`: adds multiple destinations at once
 * - `clearAndPush`: clears stack and pushes single destination
 * - `clearStackAndPush`: clears specific stack and pushes
 */
class TreeMutatorPushTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object HomeDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "home"
    }

    private object ProfileDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "profile"
    }

    private object SettingsDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "settings"
    }

    private object DetailDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "detail"
    }

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    private fun createKeyGenerator(): () -> String {
        var counter = 0
        return { "key-${counter++}" }
    }

    @BeforeTest
    fun setup() {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // PUSH TESTS
    // =========================================================================

    @Test
    fun `push adds screen to empty stack`() {
        val root = StackNode("root", null, emptyList())

        val result = TreeMutator.push(
            root = root,
            destination = HomeDestination,
            generateKey = createKeyGenerator()
        )

        assertTrue(result is StackNode)
        assertEquals(1, (result as StackNode).children.size)
        val screen = result.activeChild as ScreenNode
        assertEquals(HomeDestination, screen.destination)
        assertEquals("root", screen.parentKey)
    }

    @Test
    fun `push appends to existing stack`() {
        val screen1 = ScreenNode("s1", "root", HomeDestination)
        val root = StackNode("root", null, listOf(screen1))

        val result = TreeMutator.push(
            root = root,
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        )

        assertTrue(result is StackNode)
        val resultStack = result as StackNode
        assertEquals(2, resultStack.children.size)
        assertEquals(HomeDestination, (resultStack.children[0] as ScreenNode).destination)
        assertEquals(ProfileDestination, (resultStack.activeChild as ScreenNode).destination)
    }

    @Test
    fun `push targets deepest active stack in tabs`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode("tab0", "tabs", listOf(
                            ScreenNode("s1", "tab0", HomeDestination)
                        )),
                        StackNode("tab1", "tabs", emptyList())
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = TreeMutator.push(
            root = root,
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        )

        // New screen should be in tab0
        val tabs = (result as StackNode).children[0] as TabNode
        val tab0 = tabs.stacks[0]

        assertEquals(2, tab0.children.size)
        assertEquals(ProfileDestination, (tab0.activeChild as ScreenNode).destination)

        // tab1 should be unchanged
        assertEquals(0, tabs.stacks[1].children.size)
    }

    @Test
    fun `push targets deepest active stack in nested tabs`() {
        val innerStack = StackNode("inner-stack", "tab0", listOf(
            ScreenNode("s1", "inner-stack", HomeDestination)
        ))
        val root = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(innerStack)),
                StackNode("tab1", "tabs", emptyList())
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.push(
            root = root,
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        )

        // New screen should be in inner-stack (deepest active stack)
        val resultTabs = result as TabNode
        val tab0 = resultTabs.stacks[0]
        val resultInnerStack = tab0.children[0] as StackNode

        assertEquals(2, resultInnerStack.children.size)
        assertEquals(ProfileDestination, (resultInnerStack.activeChild as ScreenNode).destination)
    }

    @Test
    fun `push preserves structural sharing`() {
        val screen1 = ScreenNode("s1", "root", HomeDestination)
        val root = StackNode("root", null, listOf(screen1))

        val result = TreeMutator.push(
            root = root,
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        // Original screen should be same reference (structural sharing)
        assertSame(screen1, result.children[0])
    }

    @Test
    fun `push preserves structural sharing in tabs for inactive stacks`() {
        val screen1 = ScreenNode("s1", "tab0", HomeDestination)
        val screen2 = ScreenNode("s2", "tab1", ProfileDestination)
        val tab1Stack = StackNode("tab1", "tabs", listOf(screen2))

        val root = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(screen1)),
                tab1Stack
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.push(
            root = root,
            destination = SettingsDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        // tab1 stack should be same reference (unchanged)
        assertSame(tab1Stack, result.stacks[1])
        // screen2 should be same reference
        assertSame(screen2, result.stacks[1].children[0])
    }

    @Test
    fun `push throws when no active stack found`() {
        // ScreenNode has no active stack
        val root = ScreenNode("screen", null, HomeDestination)

        assertFailsWith<IllegalStateException> {
            TreeMutator.push(root, ProfileDestination)
        }
    }

    // =========================================================================
    // PUSH TO STACK TESTS
    // =========================================================================

    @Test
    fun `pushToStack targets specific stack`() {
        val root = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", emptyList())
            ),
            activeStackIndex = 0
        )

        // Push to tab1 even though tab0 is active
        val result = TreeMutator.pushToStack(
            root = root,
            stackKey = "tab1",
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        assertEquals(0, result.stacks[0].children.size)
        assertEquals(1, result.stacks[1].children.size)
        assertEquals(ProfileDestination, (result.stacks[1].activeChild as ScreenNode).destination)
    }

    @Test
    fun `pushToStack adds to existing stack content`() {
        val screen1 = ScreenNode("s1", "stack", HomeDestination)
        val root = StackNode("stack", null, listOf(screen1))

        val result = TreeMutator.pushToStack(
            root = root,
            stackKey = "stack",
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        assertEquals(2, result.children.size)
        assertEquals(HomeDestination, (result.children[0] as ScreenNode).destination)
        assertEquals(ProfileDestination, (result.children[1] as ScreenNode).destination)
    }

    @Test
    fun `pushToStack throws for invalid key`() {
        val root = StackNode("root", null, emptyList())

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.pushToStack(root, "nonexistent", HomeDestination)
        }
    }

    @Test
    fun `pushToStack throws for non-stack node`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("screen", "root", HomeDestination)
            )
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.pushToStack(root, "screen", ProfileDestination)
        }
    }

    @Test
    fun `pushToStack works with deeply nested stack`() {
        val targetStack = StackNode("target", "inner-tabs", emptyList())
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "outer-tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode("tab0", "outer-tabs", listOf(
                            TabNode(
                                key = "inner-tabs",
                                parentKey = "tab0",
                                stacks = listOf(targetStack),
                                activeStackIndex = 0
                            )
                        ))
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = TreeMutator.pushToStack(
            root = root,
            stackKey = "target",
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        )

        // Find the target stack and verify it has the new screen
        val foundStack = result.findByKey("target") as StackNode
        assertEquals(1, foundStack.children.size)
        assertEquals(ProfileDestination, (foundStack.activeChild as ScreenNode).destination)
    }

    // =========================================================================
    // PUSH ALL TESTS
    // =========================================================================

    @Test
    fun `pushAll adds multiple screens`() {
        val root = StackNode("root", null, emptyList())

        val result = TreeMutator.pushAll(
            root = root,
            destinations = listOf(HomeDestination, ProfileDestination, SettingsDestination),
            generateKey = createKeyGenerator()
        ) as StackNode

        assertEquals(3, result.children.size)
        assertEquals(HomeDestination, (result.children[0] as ScreenNode).destination)
        assertEquals(ProfileDestination, (result.children[1] as ScreenNode).destination)
        assertEquals(SettingsDestination, (result.children[2] as ScreenNode).destination)
    }

    @Test
    fun `pushAll appends to existing stack`() {
        val screen1 = ScreenNode("s1", "root", HomeDestination)
        val root = StackNode("root", null, listOf(screen1))

        val result = TreeMutator.pushAll(
            root = root,
            destinations = listOf(ProfileDestination, SettingsDestination),
            generateKey = createKeyGenerator()
        ) as StackNode

        assertEquals(3, result.children.size)
        assertEquals(HomeDestination, (result.children[0] as ScreenNode).destination)
        assertEquals(ProfileDestination, (result.children[1] as ScreenNode).destination)
        assertEquals(SettingsDestination, (result.children[2] as ScreenNode).destination)
    }

    @Test
    fun `pushAll with empty list returns same tree`() {
        val screen1 = ScreenNode("s1", "root", HomeDestination)
        val root = StackNode("root", null, listOf(screen1))

        val result = TreeMutator.pushAll(
            root = root,
            destinations = emptyList(),
            generateKey = createKeyGenerator()
        )

        assertSame(root, result)
    }

    @Test
    fun `pushAll targets deepest active stack`() {
        val root = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s1", "tab0", HomeDestination)
                )),
                StackNode("tab1", "tabs", emptyList())
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.pushAll(
            root = root,
            destinations = listOf(ProfileDestination, SettingsDestination),
            generateKey = createKeyGenerator()
        ) as TabNode

        assertEquals(3, result.stacks[0].children.size)
        assertEquals(0, result.stacks[1].children.size)
    }

    @Test
    fun `pushAll throws when no active stack found`() {
        val root = ScreenNode("screen", null, HomeDestination)

        assertFailsWith<IllegalStateException> {
            TreeMutator.pushAll(root, listOf(ProfileDestination))
        }
    }

    // =========================================================================
    // CLEAR AND PUSH TESTS
    // =========================================================================

    @Test
    fun `clearAndPush clears and adds single screen`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", SettingsDestination)
            )
        )

        val result = TreeMutator.clearAndPush(
            root = root,
            destination = DetailDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        assertEquals(1, result.children.size)
        assertEquals(DetailDestination, (result.activeChild as ScreenNode).destination)
    }

    @Test
    fun `clearAndPush works on empty stack`() {
        val root = StackNode("root", null, emptyList())

        val result = TreeMutator.clearAndPush(
            root = root,
            destination = HomeDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        assertEquals(1, result.children.size)
        assertEquals(HomeDestination, (result.activeChild as ScreenNode).destination)
    }

    @Test
    fun `clearAndPush targets deepest active stack in tabs`() {
        val root = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s1", "tab0", HomeDestination),
                    ScreenNode("s2", "tab0", ProfileDestination)
                )),
                StackNode("tab1", "tabs", listOf(
                    ScreenNode("s3", "tab1", SettingsDestination)
                ))
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.clearAndPush(
            root = root,
            destination = DetailDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        // tab0 should be cleared and have only DetailDestination
        assertEquals(1, result.stacks[0].children.size)
        assertEquals(DetailDestination, (result.stacks[0].activeChild as ScreenNode).destination)

        // tab1 should be unchanged
        assertEquals(1, result.stacks[1].children.size)
        assertEquals(SettingsDestination, (result.stacks[1].activeChild as ScreenNode).destination)
    }

    @Test
    fun `clearAndPush throws when no active stack found`() {
        val root = ScreenNode("screen", null, HomeDestination)

        assertFailsWith<IllegalStateException> {
            TreeMutator.clearAndPush(root, ProfileDestination)
        }
    }

    // =========================================================================
    // CLEAR STACK AND PUSH TESTS
    // =========================================================================

    @Test
    fun `clearStackAndPush targets specific stack`() {
        val root = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s1", "tab0", HomeDestination)
                )),
                StackNode("tab1", "tabs", listOf(
                    ScreenNode("s2", "tab1", ProfileDestination),
                    ScreenNode("s3", "tab1", SettingsDestination)
                ))
            ),
            activeStackIndex = 0
        )

        // Clear and push to tab1 even though tab0 is active
        val result = TreeMutator.clearStackAndPush(
            root = root,
            stackKey = "tab1",
            destination = DetailDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        // tab0 should be unchanged
        assertEquals(1, result.stacks[0].children.size)
        assertEquals(HomeDestination, (result.stacks[0].activeChild as ScreenNode).destination)

        // tab1 should be cleared and have only DetailDestination
        assertEquals(1, result.stacks[1].children.size)
        assertEquals(DetailDestination, (result.stacks[1].activeChild as ScreenNode).destination)
    }

    @Test
    fun `clearStackAndPush throws for invalid key`() {
        val root = StackNode("root", null, emptyList())

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.clearStackAndPush(root, "nonexistent", HomeDestination)
        }
    }

    @Test
    fun `clearStackAndPush throws for non-stack node`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("screen", "root", HomeDestination)
            )
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.clearStackAndPush(root, "screen", ProfileDestination)
        }
    }

    @Test
    fun `clearStackAndPush preserves structural sharing for other branches`() {
        val tab1Stack = StackNode("tab1", "tabs", listOf(
            ScreenNode("s2", "tab1", ProfileDestination)
        ))
        val root = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s1", "tab0", HomeDestination)
                )),
                tab1Stack
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.clearStackAndPush(
            root = root,
            stackKey = "tab0",
            destination = DetailDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        // tab1 stack should be same reference (unchanged)
        assertSame(tab1Stack, result.stacks[1])
    }
}
