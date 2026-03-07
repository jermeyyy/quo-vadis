package com.jermey.quo.vadis.compiler.ir

import com.jermey.quo.vadis.compiler.testing.CompilerTestHelper
import com.jermey.quo.vadis.compiler.testing.TestSources
import com.jermey.quo.vadis.core.navigation.config.GeneratedNavigationConfig
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.registry.PaneRoleRegistry
import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.JvmCompilationResult
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.spi.ToolProvider
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * IR codegen box tests for the Quo Vadis compiler plugin.
 *
 * These tests compile source code with the plugin, load the generated
 * `{modulePrefix}NavigationConfig` object via reflection, and verify
 * the generated navigation configuration structure is correct.
 *
 * Tests verify structural correctness:
 * - Config class generation and interface implementation
 * - Registry properties are accessible
 * - The `roots` property exists and returns a Set
 * - Methods (`buildNavNode`, `plus`) are callable with correct signatures
 *
 * Note: The FIR-to-IR metadata bridge uses IrMetadataCollector to scan
 * annotations directly from IR, and BaseConfigIrGenerator produces the
 * _baseConfig lazy property. Tests verify both structural correctness
 * and runtime behavior (populated registries, buildNavNode returning nodes).
 */
@OptIn(ExperimentalCompilerApi::class)
class IrCodegenTests {

    // ── Helpers ──────────────────────────────────────────────────────

    private companion object {
        const val GENERATED_PKG = "com.jermey.quo.vadis.generated"
    }

    private fun loadConfig(
        result: CompilationResult,
        modulePrefix: String = "Test",
    ): NavigationConfig {
        val jvmResult = result as JvmCompilationResult
        val configClass = jvmResult.classLoader.loadClass("$GENERATED_PKG.${modulePrefix}NavigationConfig")
        val instance = configClass.kotlin.objectInstance
        assertNotNull(instance, "${modulePrefix}NavigationConfig object should exist")
        assertIs<NavigationConfig>(instance)
        return instance
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadDestinationClass(
        result: CompilationResult,
        fqName: String,
    ): KClass<out NavDestination> {
        val jvmResult = result as JvmCompilationResult
        return jvmResult.classLoader.loadClass(fqName).kotlin as KClass<out NavDestination>
    }

    private fun loadObjectDestination(
        result: CompilationResult,
        fqName: String,
    ): NavDestination {
        val jvmResult = result as JvmCompilationResult
        val instance = jvmResult.classLoader.loadClass(fqName).kotlin.objectInstance
        assertNotNull(instance, "$fqName object should exist")
        assertIs<NavDestination>(instance)
        return instance
    }

    private fun instantiateDestination(
        result: CompilationResult,
        fqName: String,
        vararg args: Any?,
    ): NavDestination {
        val jvmResult = result as JvmCompilationResult
        val destinationClass = jvmResult.classLoader.loadClass(fqName)
        val constructor = destinationClass.constructors.single { it.parameterCount == args.size }
        val instance = constructor.newInstance(*args)
        assertIs<NavDestination>(instance)
        return instance
    }

    private fun disassembleClass(targetClass: Class<*>): String {
        val javap = ToolProvider.findFirst("javap").orElseThrow {
            AssertionError("javap tool is required for compiler-plugin dispatch regression tests")
        }
        val stdout = StringWriter()
        val stderr = StringWriter()
        val classPath = File(targetClass.protectionDomain.codeSource.location.toURI()).absolutePath
        val exitCode = javap.run(
            PrintWriter(stdout),
            PrintWriter(stderr),
            "-classpath",
            classPath,
            "-c",
            "-p",
            targetClass.name,
        )

        assertEquals(
            0,
            exitCode,
            "javap failed for ${targetClass.name}: ${stderr.toString().trim()}",
        )

        return stdout.toString()
    }

    private fun outputDirectoryForClass(
        result: CompilationResult,
        fqName: String,
    ): File {
        val jvmResult = result as JvmCompilationResult
        val location = jvmResult.classLoader.loadClass(fqName).protectionDomain.codeSource.location
        return File(location.toURI())
    }

    // ── 5C.1: Basic NavigationConfig Exists ─────────────────────────

    @Test
    fun `NavigationConfig is generated for basic stack`() {
        val result = CompilerTestHelper.compile(TestSources.basicStack)
        val jvmResult = result as JvmCompilationResult
        val configClass = jvmResult.classLoader.loadClass("$GENERATED_PKG.TestNavigationConfig")
        val instance = configClass.kotlin.objectInstance
        assertNotNull(instance, "TestNavigationConfig object should exist")
        assertIs<NavigationConfig>(instance)
        assertIs<GeneratedNavigationConfig>(instance)
    }

    // ── 5C.2: roots property is accessible ──────────────────────────

    @Test
    fun `roots property exists and returns a Set`() {
        val result = CompilerTestHelper.compile(TestSources.basicStack)
        val jvmResult = result as JvmCompilationResult
        val configClass = jvmResult.classLoader.loadClass("$GENERATED_PKG.TestNavigationConfig")
        val instance = configClass.kotlin.objectInstance!!

        // Access getRoots() via Java reflection — not part of NavigationConfig interface
        val getRoots = configClass.getMethod("getRoots")
        val roots = getRoots.invoke(instance)
        assertIs<Set<*>>(roots, "roots should return a Set")
    }

    // ── 5C.3: buildNavNode is callable with correct signature ───────

    @Test
    fun `buildNavNode is callable with correct signature`() {
        val result = CompilerTestHelper.compile(TestSources.basicStack)
        val config = loadConfig(result)
        val destClass = loadDestinationClass(result, "test.BasicDestination")

        // buildNavNode should return a non-null NavNode for a registered destination
        val navNode = config.buildNavNode(destClass)
        assertNotNull(navNode, "buildNavNode should return a NavNode for a registered destination")
    }

    // ── 5C.4: Tabs source compiles with valid config structure ──────

    @Test
    fun `tabs source generates config with accessible registries`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabsWithItems,
            TestSources.tabsContainerWrapper,
        )
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        // Verify tab destination classes are loadable from compilation output
        val homeTab = loadDestinationClass(result, "test.HomeTab")
        val exploreTab = loadDestinationClass(result, "test.ExploreTab")
        assertNotNull(homeTab, "HomeTab should be loadable")
        assertNotNull(exploreTab, "ExploreTab should be loadable")
    }

    @Test
    fun `mixed tabs preserve stack-backed tab items in generated config`() {
        val result = CompilerTestHelper.compile(TestSources.mixedTabsWithStackBackedItem)
        val config = loadConfig(result)

        val mixedTabsClass = loadDestinationClass(result, "test.MixedTabs")
        val settingsTabClass = loadDestinationClass(result, "test.SettingsTab")

        val settingsTabNode = config.buildNavNode(settingsTabClass)
        assertNotNull(settingsTabNode, "Stack-backed tab item should be registered as a stack container")
        assertIs<StackNode>(settingsTabNode)
        assertFalse(settingsTabNode.children.isEmpty(), "Stack-backed tab node should include its start destination")

        val mixedTabsNode = config.buildNavNode(mixedTabsClass)
        assertNotNull(mixedTabsNode, "Tabs container should build a TabNode for mixed tabs")
        assertIs<TabNode>(mixedTabsNode)
        assertEquals(2, mixedTabsNode.stacks.size, "Mixed tabs should include both flat and stack-backed items")
        assertFalse(mixedTabsNode.stacks[0].children.isEmpty(), "Flat tabs should keep their root screen")

        val nestedTabStack = mixedTabsNode.stacks[1]
        assertFalse(nestedTabStack.children.isEmpty(), "Stack-backed tabs should not be dropped from TabNode stacks")

        val nestedContainerNode = nestedTabStack.children.single()
        assertIs<StackNode>(nestedContainerNode)
        assertFalse(nestedContainerNode.children.isEmpty(), "Nested stack-backed tabs should resolve to a reachable screen")
    }

    // ── 5C.5: Pane source compiles with valid config structure ──────

    @Test
    fun `pane source generates config with accessible registries`() {
        val result = CompilerTestHelper.compile(TestSources.paneWithRoles)
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        // Verify pane destination class is loadable
        val paneClass = loadDestinationClass(result, "test.CatalogPane")
        assertNotNull(paneClass, "CatalogPane should be loadable")
    }

    // ── 5C.6: Multiple argument types compile ───────────────────────

    @Test
    fun `multiple argument types compile and generate valid config`() {
        val result = CompilerTestHelper.compile(TestSources.stackWithMultipleArgs)
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        // Verify destination class is loadable from compilation output
        val destClass = loadDestinationClass(result, "test.MultiArgDestination")
        assertNotNull(destClass, "MultiArgDestination should be loadable")
    }

    // ── 5C.7: Transition annotation compiles with codegen ───────────

    @Test
    fun `transition annotations compile and generate valid config`() {
        val result = CompilerTestHelper.compile(TestSources.destinationWithTransition)
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        val destClass = loadDestinationClass(result, "test.AnimatedDestination")
        assertNotNull(destClass, "AnimatedDestination should be loadable")
    }

    // ── 5C.8: Full navigation graph ─────────────────────────────────

    @Test
    fun `full navigation graph generates config with all registries`() {
        val result = CompilerTestHelper.compile(TestSources.fullNavigationGraph)
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        // Verify all destination classes from full graph are loadable
        assertNotNull(loadDestinationClass(result, "test.HomeDestinations"))
        assertNotNull(loadDestinationClass(result, "test.SettingsDestinations"))
        assertNotNull(loadDestinationClass(result, "test.MessagesPane"))

        // buildNavNode should construct the stack-backed tab container
        val destClass = loadDestinationClass(result, "test.HomeDestinations")
        val navNode = config.buildNavNode(destClass)
        assertNotNull(navNode, "buildNavNode should return a node for HomeDestinations")
        assertIs<StackNode>(navNode)
    }

    // ── 5C.9: Screen binding compiles and config is valid ───────────

    @Test
    fun `screen binding generates config with screenRegistry`() {
        val result = CompilerTestHelper.compile(TestSources.validScreenBinding)
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        // Verify screen destination class is loadable
        val destClass = loadDestinationClass(result, "test.ScreenDestination")
        assertNotNull(destClass, "ScreenDestination should be loadable")
    }

    // ── 5C.10: Different module prefix ──────────────────────────────

    @Test
    fun `custom module prefix generates config with correct name`() {
        val result = CompilerTestHelper.compile(
            TestSources.basicStack,
            modulePrefix = "Feature1",
        )
        val config = loadConfig(result, modulePrefix = "Feature1")
        assertIs<GeneratedNavigationConfig>(config)
    }

    // ── 5C.3: Deep Link Sources Compile and Config is Valid ────────

    @Test
    fun `deep link destinations compile and generate valid config`() {
        val result = CompilerTestHelper.compile(TestSources.deepLinkDestinations)
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)
        val registry = config.deepLinkRegistry

        // Verify destination classes are loadable
        assertNotNull(loadDestinationClass(result, "test.DeepLinkDestination"))

        val home = loadObjectDestination(result, "test.DeepLinkDestination\$Home")
        val detail = instantiateDestination(result, "test.DeepLinkDestination\$Detail", "123")
        val userPost = instantiateDestination(result, "test.DeepLinkDestination\$UserPost", "u1", "p2")
        val uriDetail = instantiateDestination(result, "test.DeepLinkDestination\$Detail", "456")
        val uriUserPost = instantiateDestination(result, "test.DeepLinkDestination\$UserPost", "u2", "p9")

        assertEquals(home, registry.resolve("app://deep/home"))
        assertEquals(detail, registry.resolve("app://deep/detail/123"))
        assertEquals(userPost, registry.resolve("app://deep/user/u1/post/p2"))
        assertNull(registry.resolve("app://nonexistent"))
        assertEquals("app://deep/home", registry.createUri(home))
        assertEquals("app://deep/detail/456", registry.createUri(uriDetail))
        assertEquals("app://deep/user/u2/post/p9", registry.createUri(uriUserPost))
    }

    @Test
    fun `deep link destinations with multiple path parameters compile`() {
        val result = CompilerTestHelper.compile(TestSources.deepLinkDestinations)
        val jvmResult = result as JvmCompilationResult

        // Verify multi-param destination class exists and has expected properties
        val userPostClass = jvmResult.classLoader.loadClass("test.DeepLinkDestination\$UserPost")
        assertNotNull(userPostClass)
        // Verify the constructor has the expected parameter names
        val constructor = userPostClass.constructors.first()
        assertTrue(constructor.parameterCount >= 2, "UserPost should have at least 2 parameters")
    }

    @Test
    fun `deep link registry resolves flat tab items and creates URIs`() {
        val result = CompilerTestHelper.compile(TestSources.mixedTabsWithStackBackedItem)
        val config = loadConfig(result)
        val registry = config.deepLinkRegistry

        val overviewTab = loadObjectDestination(result, "test.OverviewTab")
        val settingsRoot = loadObjectDestination(result, "test.SettingsTab\$Root")
        val settingsDetail = instantiateDestination(result, "test.SettingsTab\$Detail", "privacy")

        assertEquals(overviewTab, registry.resolve("app://overview"))
        assertEquals(settingsRoot, registry.resolve("app://settings/root"))
        assertEquals(settingsDetail, registry.resolve("app://settings/detail/privacy"))
        assertEquals("app://overview", registry.createUri(overviewTab))
        assertEquals("app://settings/detail/privacy", registry.createUri(settingsDetail))
    }

    @Test
    fun `deep link registry resolves pane items and creates URIs`() {
        val result = CompilerTestHelper.compile(TestSources.messagesPaneContainerSource)
        val config = loadConfig(result)
        val registry = config.deepLinkRegistry

        val conversationList = loadObjectDestination(result, "test.MessagesPane\$ConversationList")
        val conversationDetail = instantiateDestination(
            result,
            "test.MessagesPane\$ConversationDetail",
            "thread-42",
        )

        assertEquals(conversationList, registry.resolve("app://messages/conversations"))
        assertEquals(conversationDetail, registry.resolve("app://messages/conversation/thread-42"))
        assertEquals("app://messages/conversations", registry.createUri(conversationList))
        assertEquals("app://messages/conversation/thread-42", registry.createUri(conversationDetail))
    }

    @Test
    fun `deep link registry resolves standalone destinations and creates URIs`() {
        val result = CompilerTestHelper.compile(TestSources.standaloneDeepLinkDestinations)
        val config = loadConfig(result)
        val registry = config.deepLinkRegistry

        val standaloneLanding = loadObjectDestination(result, "test.StandaloneTabs\$Landing")
        val standaloneProfile = instantiateDestination(result, "test.StandaloneTabs\$Profile", "user-7")

        assertEquals(standaloneLanding, registry.resolve("app://standalone/landing"))
        assertEquals(standaloneProfile, registry.resolve("app://standalone/profile/user-7"))
        assertEquals("app://standalone/landing", registry.createUri(standaloneLanding))
        assertEquals("app://standalone/profile/user-7", registry.createUri(standaloneProfile))
    }

    @Test
    fun `deep link registry resolves query-backed arguments with path precedence`() {
        val result = CompilerTestHelper.compile(TestSources.queryBackedDeepLinkDestinations)
        val config = loadConfig(result)
        val registry = config.deepLinkRegistry

        val expected = instantiateDestination(
            result,
            "test.QueryBackedDestination\$Detail",
            "path-123",
            "email",
        )

        assertEquals(
            expected,
            registry.resolve("app://query/detail/path-123?ref=email&id=query-999"),
        )
    }

    // ── 5C.7: Scope Registry via Tabs ──────────────────────────────

    @Test
    fun `scoped tab destinations compile and generate valid config`() {
        val result = CompilerTestHelper.compile(TestSources.scopedDestinations)
        val jvmResult = result as JvmCompilationResult
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        // Verify all scoped destination classes are loadable
        assertNotNull(loadDestinationClass(result, "test.ProfileTab"))
        assertNotNull(loadDestinationClass(result, "test.NotificationsTab"))

        val profileOverview = jvmResult.classLoader.loadClass("test.ProfileTab\$Overview").kotlin.objectInstance
        assertNotNull(profileOverview, "ProfileTab.Overview should be loadable as an object instance")

        val notificationsList = jvmResult.classLoader.loadClass("test.NotificationsTab\$List").kotlin.objectInstance
        assertNotNull(notificationsList, "NotificationsTab.List should be loadable as an object instance")

        val notificationsDetail = jvmResult.classLoader.loadClass("test.NotificationsTab\$Detail")
            .getConstructor(String::class.java)
            .newInstance("notif-1") as NavDestination

        assertTrue(
            config.scopeRegistry.isInScope(ScopeKey("scopedTabs"), profileOverview as NavDestination),
            "Nested stack leaf destinations should remain in the outer tabs scope",
        )
        assertTrue(
            config.scopeRegistry.isInScope(ScopeKey("scopedTabs"), notificationsList as NavDestination),
            "Each stack-backed tab should register its reachable leaf destinations in the tabs scope",
        )
        assertTrue(
            config.scopeRegistry.isInScope(ScopeKey("scopedTabs"), notificationsDetail),
            "Parameterized nested stack leaves should also remain in the tabs scope",
        )
        assertEquals(
            ScopeKey("profileTab"),
            config.scopeRegistry.getScopeKey(profileOverview),
            "Nested stack leaves should keep the inner stack as their primary scope",
        )
    }

    // ── 5C.8: Deep Link Argument Type Tests ────────────────────────

    @Test
    fun `all seven argument types compile and generate valid config`() {
        val result = CompilerTestHelper.compile(TestSources.deepLinkArgumentTypes)
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        // Verify destination class is loadable
        val destClass = loadDestinationClass(result, "test.ArgTypeDestination")
        assertNotNull(destClass, "ArgTypeDestination should be loadable")

        // Verify enum class is loadable
        val jvmResult = result as JvmCompilationResult
        val enumClass = jvmResult.classLoader.loadClass("test.Status")
        assertNotNull(enumClass, "Status enum should be loadable")
        assertTrue(enumClass.isEnum, "Status should be an enum")
    }

    @Test
    fun `argument type destinations have expected constructor parameters`() {
        val result = CompilerTestHelper.compile(TestSources.deepLinkArgumentTypes)
        val jvmResult = result as JvmCompilationResult

        // Verify each argument type destination has the correct parameter type
        val stringArgClass = jvmResult.classLoader.loadClass("test.ArgTypeDestination\$StringArg")
        assertTrue(stringArgClass.constructors.any {
            it.parameterTypes.any { p -> p == String::class.java }
        }, "StringArg should have a String parameter")

        val intArgClass = jvmResult.classLoader.loadClass("test.ArgTypeDestination\$IntArg")
        assertTrue(intArgClass.constructors.any {
            it.parameterTypes.any { p -> p == Int::class.javaPrimitiveType || p == Int::class.javaObjectType }
        }, "IntArg should have an Int parameter")

        val longArgClass = jvmResult.classLoader.loadClass("test.ArgTypeDestination\$LongArg")
        assertTrue(longArgClass.constructors.any {
            it.parameterTypes.any { p -> p == Long::class.javaPrimitiveType || p == Long::class.javaObjectType }
        }, "LongArg should have a Long parameter")

        val boolArgClass = jvmResult.classLoader.loadClass("test.ArgTypeDestination\$BooleanArg")
        assertTrue(boolArgClass.constructors.any {
            it.parameterTypes.any { p -> p == Boolean::class.javaPrimitiveType || p == Boolean::class.javaObjectType }
        }, "BooleanArg should have a Boolean parameter")

        // TODO: When baseConfig is implemented, verify:
        // - resolve("args/string/hello") returns StringArg(name="hello")
        // - resolve("args/int/42") returns IntArg(count=42)
        // - resolve("args/long/9999999999") returns LongArg(id=9999999999L)
        // - resolve("args/float/3.14") returns FloatArg(score=3.14f)
        // - resolve("args/double/3.14159") returns DoubleArg(precise=3.14159)
        // - resolve("args/boolean/true") returns BooleanArg(flag=true)
        // - resolve("args/enum/ACTIVE") returns EnumArg(status=Status.ACTIVE)
    }

    // ── 5C.9 expanded: Complex Graph Structural Verification ───────

    @Test
    fun `complex graph has all destination subclass types loadable`() {
        val result = CompilerTestHelper.compile(TestSources.fullNavigationGraph)
        val jvmResult = result as JvmCompilationResult

        // Verify all leaf destination classes from the full graph
        assertNotNull(jvmResult.classLoader.loadClass("test.HomeDestinations\$Feed"))
        assertNotNull(jvmResult.classLoader.loadClass("test.HomeDestinations\$Article"))
        assertNotNull(jvmResult.classLoader.loadClass("test.SettingsDestinations\$Root"))
        assertNotNull(jvmResult.classLoader.loadClass("test.SettingsDestinations\$Account"))
        assertNotNull(jvmResult.classLoader.loadClass("test.MessagesPane\$Inbox"))
        assertNotNull(jvmResult.classLoader.loadClass("test.MessagesPane\$Thread"))
    }

    @Test
    fun `complex graph config implements GeneratedNavigationConfig marker`() {
        val result = CompilerTestHelper.compile(TestSources.fullNavigationGraph)
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        // Verify the plus() method exists with correct signature via reflection
        val jvmResult = result as JvmCompilationResult
        val configClass = jvmResult.classLoader.loadClass("$GENERATED_PKG.TestNavigationConfig")
        val plusMethod = configClass.methods.find { it.name == "plus" }
        assertNotNull(plusMethod, "plus() operator method should exist")

        // TODO: When baseConfig is implemented, verify:
        // - config + NavigationConfig.Empty returns valid config
        // - NavigationConfig.Empty + config returns valid config
    }

    // ── 5C.10: Multi-Module Aggregation ────────────────────────────

    @Test
    fun `feature module compiles with different prefix`() {
        val result = CompilerTestHelper.compile(
            TestSources.featureModuleSource,
            modulePrefix = "Feature1",
        )
        val config = loadConfig(result, modulePrefix = "Feature1")
        assertIs<GeneratedNavigationConfig>(config)

        // Verify feature destination class is loadable
        val destClass = loadDestinationClass(result, "test.feature.FeatureDestination")
        assertNotNull(destClass, "FeatureDestination should be loadable")
    }

    @Test
    fun `two modules compile independently with different prefixes`() {
        // Compile "module 1" with one prefix
        val result1 = CompilerTestHelper.compile(
            TestSources.basicStack,
            modulePrefix = "Module1",
        )
        val config1 = loadConfig(result1, modulePrefix = "Module1")
        assertIs<GeneratedNavigationConfig>(config1)

        // Compile "module 2" with a different prefix
        val result2 = CompilerTestHelper.compile(
            TestSources.featureModuleSource,
            modulePrefix = "Module2",
        )
        val config2 = loadConfig(result2, modulePrefix = "Module2")
        assertIs<GeneratedNavigationConfig>(config2)

        // Both configs exist independently and implement the same interface
        assertIs<NavigationConfig>(config1)
        assertIs<NavigationConfig>(config2)

        // TODO: When baseConfig is implemented, verify:
        // - config1 + config2 produces a valid merged config
        // - merged config contains destinations from both modules
        // - screenRegistry dispatches correctly across modules
        // - deepLinkRegistry resolves URIs from either module
    }

    @Test
    fun `aggregated config includes dependency-module screen registries`() {
        val featureOne = CompilerTestHelper.compile(
            *TestSources.crossModuleFeatureOneSources,
            modulePrefix = "Feature1",
        )
        val featureTwo = CompilerTestHelper.compile(
            *TestSources.crossModuleFeatureTwoSources,
            modulePrefix = "Feature2",
        )

        val appResult = CompilerTestHelper.compile(
            *TestSources.crossModuleAggregationAppSources,
            modulePrefix = "App",
            classpaths = listOf(
                outputDirectoryForClass(featureOne, "test.feature1.FeatureOneDestination"),
                outputDirectoryForClass(featureTwo, "test.feature2.FeatureTwoDestination"),
            ),
        )
        val jvmResult = appResult as JvmCompilationResult

        val resolverClass = jvmResult.classLoader.loadClass("test.app.ConfigResolverKt")
        val config = resolverClass.getMethod("resolveConfig").invoke(null) as NavigationConfig

        assertEquals(
            "$GENERATED_PKG.App__AggregatedConfig",
            config::class.java.name,
            "navigationConfig<AppRoot>() should resolve to the aggregated config in app modules",
        )

        val featureOneHome = jvmResult.classLoader
            .loadClass("test.feature1.FeatureOneDestination\$Home")
            .kotlin.objectInstance as NavDestination
        val featureTwoLogin = jvmResult.classLoader
            .loadClass("test.feature2.FeatureTwoDestination\$Login")
            .kotlin.objectInstance as NavDestination

        assertTrue(
            config.screenRegistry.hasContent(featureOneHome),
            "Aggregated config should expose screen content from dependency module Feature1",
        )
        assertTrue(
            config.screenRegistry.hasContent(featureTwoLogin),
            "Aggregated config should expose screen content from dependency module Feature2",
        )
        assertNotNull(
            config.buildNavNode(loadDestinationClass(appResult, "test.feature1.FeatureOneDestination")),
            "Aggregated config should build nav nodes for Feature1 destinations",
        )
        assertNotNull(
            config.buildNavNode(loadDestinationClass(appResult, "test.feature2.FeatureTwoDestination")),
            "Aggregated config should build nav nodes for Feature2 destinations",
        )

        val aggregatedDisassembly = disassembleClass(config::class.java)
        assertTrue(
            aggregatedDisassembly.contains("Feature1NavigationConfig") &&
                aggregatedDisassembly.contains("Feature2NavigationConfig"),
            "Aggregated config should compose dependency-module generated configs.\n$aggregatedDisassembly",
        )
    }

    @Test
    fun `navigation root source compiles`() {
        val result = CompilerTestHelper.compile(
            TestSources.navigationRootSource,
            modulePrefix = "App",
        )
        // NavigationRoot should generate a config even without destination stacks
        // (it aggregates from dependencies in a real build)
        val jvmResult = result as JvmCompilationResult
        // The class should at minimum compile without errors
        assertNotNull(jvmResult.classLoader, "ClassLoader should be available")
    }

    // ── Phase 4: Single-Module Config Resolution ────────────────────

    @Test
    fun `navigationConfig call resolves to generated config in single module`() {
        val result = CompilerTestHelper.compile(
            *TestSources.singleModuleConfigResolution,
            modulePrefix = "App",
        )
        val jvmResult = result as JvmCompilationResult

        // Invoke the compiled resolveConfig() function via reflection.
        // Without the compiler plugin rewriting, this would throw an
        // IllegalStateException from the stub body.
        val callerClass = jvmResult.classLoader.loadClass("test.app.ConfigCallerKt")
        val resolveConfig = callerClass.getMethod("resolveConfig")
        val config = resolveConfig.invoke(null)

        assertNotNull(config, "navigationConfig<T>() should resolve to generated config")
        assertIs<NavigationConfig>(config)
    }

    // ── Phase 1: Pane Back Behavior Ordinal ─────────────────────────

    @Test
    fun `pane back behavior produces correct PaneNode backBehavior for each variant`() {
        val result = CompilerTestHelper.compile(TestSources.paneBackBehaviorVariants)
        val config = loadConfig(result)

        // Default: PopUntilScaffoldValueChange (annotation ordinal maps to builder int 1)
        val defaultPaneClass = loadDestinationClass(result, "test.DefaultPane")
        val defaultNode = config.buildNavNode(defaultPaneClass)
        assertNotNull(defaultNode, "buildNavNode should return a node for DefaultPane")
        assertIs<PaneNode>(defaultNode)
        assertEquals(
            PaneBackBehavior.PopUntilScaffoldValueChange,
            defaultNode.backBehavior,
            "Default @Pane should produce PopUntilScaffoldValueChange",
        )

        // PopLatest (annotation ordinal maps to builder int 0)
        val popLatestClass = loadDestinationClass(result, "test.PopLatestPane")
        val popLatestNode = config.buildNavNode(popLatestClass)
        assertNotNull(popLatestNode, "buildNavNode should return a node for PopLatestPane")
        assertIs<PaneNode>(popLatestNode)
        assertEquals(
            PaneBackBehavior.PopLatest,
            popLatestNode.backBehavior,
            "PopLatest @Pane should produce PopLatest",
        )

        // PopUntilContentChange (annotation ordinal maps to builder int 2)
        val contentChangeClass = loadDestinationClass(result, "test.ContentChangePane")
        val contentChangeNode = config.buildNavNode(contentChangeClass)
        assertNotNull(contentChangeNode, "buildNavNode should return a node for ContentChangePane")
        assertIs<PaneNode>(contentChangeNode)
        assertEquals(
            PaneBackBehavior.PopUntilContentChange,
            contentChangeNode.backBehavior,
            "PopUntilContentChange @Pane should produce PopUntilContentChange",
        )
    }

    // ── Phase 3: Deep Link Resolve with Path Parameters ─────────────

    @Test
    fun `deep link registry resolves parameterized URI to destination`() {
        val result = CompilerTestHelper.compile(TestSources.deepLinkDestinations)
        val config = loadConfig(result)
        val registry = config.deepLinkRegistry

        // Resolve a simple path parameter: "deep/detail/abc123"
        val detail = registry.resolve("deep/detail/abc123")
        assertNotNull(detail, "resolve should return a destination for 'deep/detail/abc123'")
        assertEquals(
            "test.DeepLinkDestination\$Detail",
            detail::class.java.name,
            "Resolved destination should be DeepLinkDestination.Detail",
        )
        val idGetter = detail.javaClass.getMethod("getId")
        assertEquals("abc123", idGetter.invoke(detail), "Detail.id should be 'abc123'")

        // Resolve multi-param path: "deep/user/u1/post/p2"
        val userPost = registry.resolve("deep/user/u1/post/p2")
        assertNotNull(userPost, "resolve should return a destination for 'deep/user/u1/post/p2'")
        assertEquals(
            "test.DeepLinkDestination\$UserPost",
            userPost::class.java.name,
            "Resolved destination should be DeepLinkDestination.UserPost",
        )
        val userIdGetter = userPost.javaClass.getMethod("getUserId")
        val postIdGetter = userPost.javaClass.getMethod("getPostId")
        assertEquals("u1", userIdGetter.invoke(userPost), "UserPost.userId should be 'u1'")
        assertEquals("p2", postIdGetter.invoke(userPost), "UserPost.postId should be 'p2'")

        // Non-matching URI returns null
        val notFound = registry.resolve("nonexistent/path")
        assertNull(notFound, "resolve should return null for unknown URI")
    }

    // ── Phase 2: Container Registry Structural Test ─────────────────

    @Test
    fun `container registry is generated and accessible on config`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabsWithItems,
            TestSources.tabsContainerWrapper,
        )
        val config = loadConfig(result)

        val containerRegistry = config.containerRegistry
        assertNotNull(containerRegistry, "containerRegistry should be non-null")
        assertIs<ContainerRegistry>(containerRegistry)

        assertTrue(
            containerRegistry.hasTabsContainer("mainTabs"),
            "hasTabsContainer should return true when a wrapper is registered",
        )
        assertFalse(
            containerRegistry.hasTabsContainer("unknown"),
            "hasTabsContainer should return false for unknown key",
        )
    }

    @Test
    fun `container registry hasTabsContainer returns true when wrapper is registered`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabsWithItems,
            TestSources.tabsContainerWrapper,
        )
        val config = loadConfig(result)

        val containerRegistry = config.containerRegistry
        assertNotNull(containerRegistry, "containerRegistry should be non-null")
        assertIs<ContainerRegistry>(containerRegistry)

        // @TabsContainer(MainTabs::class) wrapper IS defined → hasTabsContainer should return true
        assertTrue(
            containerRegistry.hasTabsContainer("mainTabs"),
            "hasTabsContainer should return true when a wrapper is registered for mainTabs. " +
                "Registry class: ${containerRegistry::class.java.name}",
        )
        assertFalse(
            containerRegistry.hasTabsContainer("unknown"),
            "hasTabsContainer should return false for an unknown key",
        )
    }

    @Test
    fun `container registry TabsContainer dispatches mainTabs branch to generated wrapper`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabsWithItems,
            TestSources.tabsContainerWrapper,
        )
        val config = loadConfig(result)

        val containerRegistry = config.containerRegistry
        assertNotNull(containerRegistry, "containerRegistry should be non-null")
        assertIs<ContainerRegistry>(containerRegistry)
        assertTrue(
            containerRegistry.hasTabsContainer("mainTabs"),
            "wrapper registration should remain visible while checking dispatch generation",
        )

        val disassembly = disassembleClass(containerRegistry::class.java)
        val dispatchPattern = Regex(
            """public void TabsContainer\(.*?mainTabs.*?invokestatic.*?MainTabsWrapper""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )

        assertTrue(
            dispatchPattern.containsMatchIn(disassembly),
            "Generated TabsContainer dispatch should branch on mainTabs and invoke MainTabsWrapper.\n" +
                disassembly,
        )
    }

    @Test
    fun `container registry TabsContainer dispatches companion-targeted stateDrivenDemo branch to generated wrapper`() {
        val result = CompilerTestHelper.compile(
            TestSources.companionTargetedTabsContainerSource,
            TestSources.companionTargetedTabsContainerWrapper,
        )
        val config = loadConfig(result)

        val containerRegistry = config.containerRegistry
        assertNotNull(containerRegistry, "containerRegistry should be non-null")
        assertIs<ContainerRegistry>(containerRegistry)
        assertTrue(
            containerRegistry.hasTabsContainer("stateDrivenDemo"),
            "companion-targeted wrapper should normalize to the stateDrivenDemo tabs scope key",
        )

        val disassembly = disassembleClass(containerRegistry::class.java)
        val dispatchPattern = Regex(
            """public void TabsContainer\(.*?stateDrivenDemo.*?invokestatic.*?StateDrivenDemoContainer""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )

        assertTrue(
            dispatchPattern.containsMatchIn(disassembly),
            "Generated TabsContainer dispatch should branch on stateDrivenDemo and invoke StateDrivenDemoContainer.\n" +
                disassembly,
        )
    }

    @Test
    fun `container registry TabsContainer dispatches wrapper with compose synthetic params`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabsWithItems,
            TestSources.composeSyntheticTabsContainerWrapper,
        )
        val config = loadConfig(result)

        val containerRegistry = config.containerRegistry
        assertNotNull(containerRegistry, "containerRegistry should be non-null")
        assertIs<ContainerRegistry>(containerRegistry)
        assertTrue(
            containerRegistry.hasTabsContainer("mainTabs"),
            "compose-style wrapper should remain registered after validation",
        )

        val disassembly = disassembleClass(containerRegistry::class.java)
        val dispatchPattern = Regex(
            """public void TabsContainer\(.*?mainTabs.*?invokestatic.*?MainTabsComposeTransformedWrapper""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )

        assertTrue(
            dispatchPattern.containsMatchIn(disassembly),
            "Generated TabsContainer dispatch should branch on mainTabs and invoke MainTabsComposeTransformedWrapper.\n" +
                disassembly,
        )
    }

    @Test
    fun `container registry PaneContainer dispatches messagesPane branch to generated wrapper`() {
        val result = CompilerTestHelper.compile(
            TestSources.messagesPaneContainerSource,
            TestSources.messagesPaneContainerWrapper,
        )
        val config = loadConfig(result)

        val containerRegistry = config.containerRegistry
        assertNotNull(containerRegistry, "containerRegistry should be non-null")
        assertIs<ContainerRegistry>(containerRegistry)
        assertTrue(
            containerRegistry.hasPaneContainer("messagesPane"),
            "wrapper registration should remain visible while checking pane dispatch generation",
        )

        val disassembly = disassembleClass(containerRegistry::class.java)
        val dispatchPattern = Regex(
            """public void PaneContainer\(.*?messagesPane.*?invokestatic.*?MessagesPaneContainer""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )

        assertTrue(
            dispatchPattern.containsMatchIn(disassembly),
            "Generated PaneContainer dispatch should branch on messagesPane and invoke MessagesPaneContainer.\n" +
                disassembly,
        )
    }

    // ── Phase 5: Pane Role Registry Scope Key ───────────────────────

    @Test
    fun `pane role registry returns correct roles per scope key`() {
        val result = CompilerTestHelper.compile(TestSources.multiplePaneScopes)
        val config = loadConfig(result)
        val jvmResult = result as JvmCompilationResult

        val paneRoleRegistry = config.paneRoleRegistry
        assertNotNull(paneRoleRegistry, "paneRoleRegistry should be non-null")
        assertIs<PaneRoleRegistry>(paneRoleRegistry)

        // Load destination object instances from the compiled output
        val productsObj = jvmResult.classLoader.loadClass("test.CatalogPane2\$Products")
            .kotlin.objectInstance as NavDestination
        val productInfoObj = jvmResult.classLoader.loadClass("test.CatalogPane2\$ProductInfo")
            .kotlin.objectInstance as NavDestination
        val orderListObj = jvmResult.classLoader.loadClass("test.OrdersPane\$OrderList")
            .kotlin.objectInstance as NavDestination

        // catalogPane scope: Products → Primary, ProductInfo → Supporting
        val catalogScope = ScopeKey("catalogPane")
        assertEquals(
            PaneRole.Primary,
            paneRoleRegistry.getPaneRole(catalogScope, productsObj),
            "Products should be Primary in catalogPane scope",
        )
        assertEquals(
            PaneRole.Supporting,
            paneRoleRegistry.getPaneRole(catalogScope, productInfoObj),
            "ProductInfo should be Supporting in catalogPane scope",
        )

        // ordersPane scope: OrderList → Primary
        val ordersScope = ScopeKey("ordersPane")
        assertEquals(
            PaneRole.Primary,
            paneRoleRegistry.getPaneRole(ordersScope, orderListObj),
            "OrderList should be Primary in ordersPane scope",
        )

        // Cross-scope: catalogPane destinations not in ordersPane scope
        assertNull(
            paneRoleRegistry.getPaneRole(ordersScope, productsObj),
            "Products should return null for ordersPane scope",
        )

        // Unknown scope returns null
        val unknownScope = ScopeKey("unknown")
        assertNull(
            paneRoleRegistry.getPaneRole(unknownScope, productsObj),
            "Any destination should return null for unknown scope",
        )
    }
}
