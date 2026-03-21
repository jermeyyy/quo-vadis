package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

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
@OptIn(InternalQuoVadisApi::class)
class TreeMutatorPushTest : FunSpec() {

    object HomeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "home"
    }

    object ProfileDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "profile"
    }

    object SettingsDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "settings"
    }

    object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "detail"
    }

    init {

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    fun createKeyGenerator(): () -> NodeKey {
        var counter = 0
        return { NodeKey("key-${counter++}") }
    }

    beforeTest {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // PUSH TESTS
    // =========================================================================

    test("push adds screen to empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = TreeMutator.push(
            root = root,
            destination = HomeDestination,
            generateKey = createKeyGenerator()
        )

        (result is StackNode).shouldBeTrue()
        (result as StackNode).children.size shouldBe 1
        val screen = result.activeChild as ScreenNode
        screen.destination shouldBe HomeDestination
        screen.parentKey shouldBe NodeKey("root")
    }

    test("push appends to existing stack") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(NodeKey("root"), null, listOf(screen1))

        val result = TreeMutator.push(
            root = root,
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        )

        (result is StackNode).shouldBeTrue()
        val resultStack = result as StackNode
        resultStack.children.size shouldBe 2
        (resultStack.children[0] as ScreenNode).destination shouldBe HomeDestination
        (resultStack.activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    test("push targets deepest active stack in tabs") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                            )
                        ),
                        StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
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

        tab0.children.size shouldBe 2
        (tab0.activeChild as ScreenNode).destination shouldBe ProfileDestination

        // tab1 should be unchanged
        tabs.stacks[1].children.size shouldBe 0
    }

    test("push targets deepest active stack in nested tabs") {
        val innerStack = StackNode(NodeKey("inner-stack"), NodeKey("tab0"), listOf(
                ScreenNode(NodeKey("s1"), NodeKey("inner-stack"), HomeDestination)
            )
        )
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(innerStack)),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
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

        resultInnerStack.children.size shouldBe 2
        (resultInnerStack.activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    test("push preserves structural sharing") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(NodeKey("root"), null, listOf(screen1))

        val result = TreeMutator.push(
            root = root,
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        // Original screen should be same reference (structural sharing)
        result.children[0] shouldBeSameInstanceAs screen1
    }

    test("push preserves structural sharing in tabs for inactive stacks") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
        val tab1Stack = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(screen2))

        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(screen1)),
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
        result.stacks[1] shouldBeSameInstanceAs tab1Stack
        // screen2 should be same reference
        result.stacks[1].children[0] shouldBeSameInstanceAs screen2
    }

    test("push throws when no active stack found") {
        // ScreenNode has no active stack
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        shouldThrow<IllegalStateException> {
            TreeMutator.push(root, ProfileDestination)
        }
    }

    // =========================================================================
    // PUSH TO STACK TESTS
    // =========================================================================

    test("pushToStack targets specific stack") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        // Push to tab1 even though tab0 is active
        val result = TreeMutator.pushToStack(
            root = root,
            stackKey = NodeKey("tab1"),
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        result.stacks[0].children.size shouldBe 0
        result.stacks[1].children.size shouldBe 1
        (result.stacks[1].activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    test("pushToStack adds to existing stack content") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("stack"), HomeDestination)
        val root = StackNode(NodeKey("stack"), null, listOf(screen1))

        val result = TreeMutator.pushToStack(
            root = root,
            stackKey = NodeKey("stack"),
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        result.children.size shouldBe 2
        (result.children[0] as ScreenNode).destination shouldBe HomeDestination
        (result.children[1] as ScreenNode).destination shouldBe ProfileDestination
    }

    test("pushToStack throws for invalid key") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        shouldThrow<IllegalArgumentException> {
            TreeMutator.pushToStack(root, NodeKey("nonexistent"), HomeDestination)
        }
    }

    test("pushToStack throws for non-stack node") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("screen"), NodeKey("root"), HomeDestination)
            )
        )

        shouldThrow<IllegalArgumentException> {
            TreeMutator.pushToStack(root, NodeKey("screen"), ProfileDestination)
        }
    }

    test("pushToStack works with deeply nested stack") {
        val targetStack = StackNode(NodeKey("target"), NodeKey("inner-tabs"), emptyList())
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("outer-tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("outer-tabs"), listOf(
                                TabNode(
                                    key = NodeKey("inner-tabs"),
                                    parentKey = NodeKey("tab0"),
                                    stacks = listOf(targetStack),
                                    activeStackIndex = 0
                                )
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = TreeMutator.pushToStack(
            root = root,
            stackKey = NodeKey("target"),
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        )

        // Find the target stack and verify it has the new screen
        val foundStack = result.findByKey(NodeKey("target")) as StackNode
        foundStack.children.size shouldBe 1
        (foundStack.activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    // =========================================================================
    // PUSH ALL TESTS
    // =========================================================================

    test("pushAll adds multiple screens") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = TreeMutator.pushAll(
            root = root,
            destinations = listOf(HomeDestination, ProfileDestination, SettingsDestination),
            generateKey = createKeyGenerator()
        ) as StackNode

        result.children.size shouldBe 3
        (result.children[0] as ScreenNode).destination shouldBe HomeDestination
        (result.children[1] as ScreenNode).destination shouldBe ProfileDestination
        (result.children[2] as ScreenNode).destination shouldBe SettingsDestination
    }

    test("pushAll appends to existing stack") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(NodeKey("root"), null, listOf(screen1))

        val result = TreeMutator.pushAll(
            root = root,
            destinations = listOf(ProfileDestination, SettingsDestination),
            generateKey = createKeyGenerator()
        ) as StackNode

        result.children.size shouldBe 3
        (result.children[0] as ScreenNode).destination shouldBe HomeDestination
        (result.children[1] as ScreenNode).destination shouldBe ProfileDestination
        (result.children[2] as ScreenNode).destination shouldBe SettingsDestination
    }

    test("pushAll with empty list returns same tree") {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(NodeKey("root"), null, listOf(screen1))

        val result = TreeMutator.pushAll(
            root = root,
            destinations = emptyList(),
            generateKey = createKeyGenerator()
        )

        result shouldBeSameInstanceAs root
    }

    test("pushAll targets deepest active stack") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.pushAll(
            root = root,
            destinations = listOf(ProfileDestination, SettingsDestination),
            generateKey = createKeyGenerator()
        ) as TabNode

        result.stacks[0].children.size shouldBe 3
        result.stacks[1].children.size shouldBe 0
    }

    test("pushAll throws when no active stack found") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        shouldThrow<IllegalStateException> {
            TreeMutator.pushAll(root, listOf(ProfileDestination))
        }
    }

    // =========================================================================
    // CLEAR AND PUSH TESTS
    // =========================================================================

    test("clearAndPush clears and adds single screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )

        val result = TreeMutator.clearAndPush(
            root = root,
            destination = DetailDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        result.children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    test("clearAndPush works on empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = TreeMutator.clearAndPush(
            root = root,
            destination = HomeDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        result.children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    test("clearAndPush targets deepest active stack in tabs") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s3"), NodeKey("tab1"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.clearAndPush(
            root = root,
            destination = DetailDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        // tab0 should be cleared and have only DetailDestination
        result.stacks[0].children.size shouldBe 1
        (result.stacks[0].activeChild as ScreenNode).destination shouldBe DetailDestination

        // tab1 should be unchanged
        result.stacks[1].children.size shouldBe 1
        (result.stacks[1].activeChild as ScreenNode).destination shouldBe SettingsDestination
    }

    test("clearAndPush throws when no active stack found") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        shouldThrow<IllegalStateException> {
            TreeMutator.clearAndPush(root, ProfileDestination)
        }
    }

    // =========================================================================
    // CLEAR STACK AND PUSH TESTS
    // =========================================================================

    test("clearStackAndPush targets specific stack") {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination),
                        ScreenNode(NodeKey("s3"), NodeKey("tab1"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        // Clear and push to tab1 even though tab0 is active
        val result = TreeMutator.clearStackAndPush(
            root = root,
            stackKey = NodeKey("tab1"),
            destination = DetailDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        // tab0 should be unchanged
        result.stacks[0].children.size shouldBe 1
        (result.stacks[0].activeChild as ScreenNode).destination shouldBe HomeDestination

        // tab1 should be cleared and have only DetailDestination
        result.stacks[1].children.size shouldBe 1
        (result.stacks[1].activeChild as ScreenNode).destination shouldBe DetailDestination
    }

    test("clearStackAndPush throws for invalid key") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        shouldThrow<IllegalArgumentException> {
            TreeMutator.clearStackAndPush(root, NodeKey("nonexistent"), HomeDestination)
        }
    }

    test("clearStackAndPush throws for non-stack node") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("screen"), NodeKey("root"), HomeDestination)
            )
        )

        shouldThrow<IllegalArgumentException> {
            TreeMutator.clearStackAndPush(root, NodeKey("screen"), ProfileDestination)
        }
    }

    test("clearStackAndPush preserves structural sharing for other branches") {
        val tab1Stack = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
            )
        )
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                tab1Stack
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.clearStackAndPush(
            root = root,
            stackKey = NodeKey("tab0"),
            destination = DetailDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        // tab1 stack should be same reference (unchanged)
        result.stacks[1] shouldBeSameInstanceAs tab1Stack
    }

    } // init
}
