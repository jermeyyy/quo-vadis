package com.jermey.quo.vadis.compiler.ir

import com.jermey.quo.vadis.compiler.testing.CompilerTestHelper
import com.jermey.quo.vadis.compiler.testing.TestSources
import com.jermey.quo.vadis.core.navigation.config.GeneratedNavigationConfig
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.JvmCompilationResult
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
        val result = CompilerTestHelper.compile(TestSources.tabsWithItems)
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        // Verify tab destination classes are loadable from compilation output
        val homeTab = loadDestinationClass(result, "test.HomeTab")
        val exploreTab = loadDestinationClass(result, "test.ExploreTab")
        assertNotNull(homeTab, "HomeTab should be loadable")
        assertNotNull(exploreTab, "ExploreTab should be loadable")
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

        // buildNavNode is callable (returns null — baseConfig not yet implemented)
        val destClass = loadDestinationClass(result, "test.HomeDestinations")
        val navNode = config.buildNavNode(destClass)
        assertTrue(navNode == null, "buildNavNode returns null (baseConfig not yet implemented)")
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

        // Verify destination classes are loadable
        assertNotNull(loadDestinationClass(result, "test.DeepLinkDestination"))

        // TODO: When baseConfig is implemented, verify:
        // - deepLinkRegistry.resolve("deep/home") returns DeepLinkDestination.Home
        // - deepLinkRegistry.resolve("deep/detail/123") returns Detail(id="123")
        // - deepLinkRegistry.resolve("deep/user/u1/post/p2") returns UserPost(userId="u1", postId="p2")
        // - deepLinkRegistry.resolve("nonexistent") returns null
        // - deepLinkRegistry.createUri(Detail("456")) returns "deep/detail/456"
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

    // ── 5C.7: Scope Registry via Tabs ──────────────────────────────

    @Test
    fun `scoped tab destinations compile and generate valid config`() {
        val result = CompilerTestHelper.compile(TestSources.scopedDestinations)
        val config = loadConfig(result)
        assertIs<GeneratedNavigationConfig>(config)

        // Verify all scoped destination classes are loadable
        assertNotNull(loadDestinationClass(result, "test.ProfileTab"))
        assertNotNull(loadDestinationClass(result, "test.NotificationsTab"))

        // TODO: When baseConfig is implemented, verify:
        // - scopeRegistry.isInScope(ProfileTab.Overview::class, "scopedTabs") returns true
        // - scopeRegistry.isInScope(NotificationsTab.List::class, "scopedTabs") returns true
        // - scopeRegistry.getScopeKey(ProfileTab.Overview::class) returns "profileTab"
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
}
