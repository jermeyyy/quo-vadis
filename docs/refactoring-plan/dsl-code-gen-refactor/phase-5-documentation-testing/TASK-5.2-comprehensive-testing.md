```markdown
# Task 5.2: Comprehensive Testing

> **Task Status**: ⬜ Not Started  
> **Estimated Effort**: 3-4 days  
> **Dependencies**: Phase 1-4 complete  
> **Blocks**: Release

---

## Objective

Achieve comprehensive test coverage for all new DSL-based navigation APIs. The target is 90%+ code coverage on new code, with thorough testing of all edge cases, permutations, and integration scenarios.

**Target Outcome**:
- 90%+ code coverage on new DSL code
- All builder permutations tested
- Integration tests verify end-to-end flows
- Regression tests ensure old APIs still work

---

## Test Categories

### 1. DSL Builder Unit Tests

**Location**: `quo-vadis-core/src/commonTest/kotlin/.../navigation/dsl/`

| Test Class | Coverage |
|------------|----------|
| `NavigationConfigBuilderTest` | Main builder DSL |
| `StackBuilderTest` | Stack container DSL |
| `TabsBuilderTest` | Tabs container DSL |
| `PanesBuilderTest` | Panes container DSL |

### 2. NavigationConfig Unit Tests

**Location**: `quo-vadis-core/src/commonTest/kotlin/.../navigation/`

| Test Class | Coverage |
|------------|----------|
| `NavigationConfigTest` | Interface contract |
| `EmptyNavigationConfigTest` | Empty implementation |
| `CompositeNavigationConfigTest` | Composition logic |
| `DslNavigationConfigTest` | DSL implementation |

### 3. KSP Output Tests

**Location**: `quo-vadis-ksp/src/test/kotlin/.../`

| Test Class | Coverage |
|------------|----------|
| `NavigationConfigGeneratorTest` | Generated code validation |
| `DslBlockGeneratorTest` | DSL block generation |
| `GeneratedCodeCompilationTest` | Compilation verification |

### 4. Integration Tests

**Location**: `composeApp/src/commonTest/kotlin/.../`

| Test Class | Coverage |
|------------|----------|
| `NavigationFlowTest` | End-to-end navigation |
| `MultiModuleCompositionTest` | Config composition |
| `DeepLinkIntegrationTest` | Deep link handling |

### 5. Migration/Regression Tests

**Location**: `quo-vadis-core/src/commonTest/kotlin/.../`

| Test Class | Coverage |
|------------|----------|
| `LegacyApiCompatibilityTest` | Old APIs still work |
| `DeprecationMigrationTest` | Deprecated path works |

---

## Test Specifications

### DSL Builder Tests

```kotlin
class NavigationConfigBuilderTest {
    
    @Test
    fun `screen registration adds to screenRegistry`() {
        val config = navigationConfig {
            screen<TestDestination> { Text("Test") }
        }
        
        // Verify screen is registered
        assertNotNull(config.screenRegistry)
    }
    
    @Test
    fun `tabs builder creates TabNode correctly`() {
        val config = navigationConfig {
            tabs<TestTabs>(scopeKey = "TestTabs") {
                initialTab = 1
                tab(TestTabs.Tab1, title = "Tab 1")
                tab(TestTabs.Tab2, title = "Tab 2")
            }
        }
        
        val node = config.buildNavNode(TestTabs::class)
        assertIs<TabNode>(node)
        assertEquals(2, node.stacks.size)
        assertEquals(1, node.activeStackIndex)
    }
    
    @Test
    fun `nested tab creates proper stack structure`() {
        val config = navigationConfig {
            tabs<TestTabs> {
                tab(TestTabs.Tab1) {
                    screen(TestDestination.Screen1)
                    screen(TestDestination.Screen2)
                }
            }
        }
        
        val node = config.buildNavNode(TestTabs::class) as TabNode
        val stack = node.stacks[0]
        assertEquals(2, stack.children.size)
    }
    
    @Test
    fun `panes builder creates PaneNode correctly`() {
        val config = navigationConfig {
            panes<TestPanes> {
                primary(weight = 0.4f) { root(TestPanes.List) }
                secondary(weight = 0.6f) { root(TestPanes.Detail) }
            }
        }
        
        val node = config.buildNavNode(TestPanes::class)
        assertIs<PaneNode>(node)
    }
    
    @Test
    fun `scope registration creates proper mappings`() {
        val config = navigationConfig {
            scope("TestScope", Dest1::class, Dest2::class, Dest3::class)
        }
        
        val destinations = config.scopeRegistry.getDestinationsInScope("TestScope")
        assertEquals(3, destinations.size)
    }
    
    @Test
    fun `transition registration maps correctly`() {
        val config = navigationConfig {
            transition<TestDestination>(NavTransitions.Slide)
        }
        
        val transition = config.transitionRegistry.getTransition(TestDestination())
        assertEquals(NavTransitions.Slide, transition)
    }
}
```

### Composition Tests

```kotlin
class CompositeNavigationConfigTest {
    
    @Test
    fun `plus operator combines configs correctly`() {
        val config1 = navigationConfig {
            screen<Dest1> { Text("1") }
        }
        val config2 = navigationConfig {
            screen<Dest2> { Text("2") }
        }
        
        val combined = config1 + config2
        
        // Both screens should be available
        assertNotNull(combined.screenRegistry)
    }
    
    @Test
    fun `secondary config takes priority`() {
        val primary = navigationConfig {
            scope("Scope", Dest1::class)
        }
        val secondary = navigationConfig {
            scope("Scope", Dest2::class)
        }
        
        val combined = primary + secondary
        val scopeKey = combined.scopeRegistry.getScopeKey(Dest2())
        
        assertEquals("Scope", scopeKey)
    }
    
    @Test
    fun `Empty is identity element`() {
        val config = navigationConfig {
            screen<TestDestination> { }
        }
        
        val result = config + NavigationConfig.Empty
        
        // Should behave identically
        assertNotNull(result.buildNavNode(TestDestination::class))
    }
    
    @Test
    fun `chained composition works`() {
        val a = navigationConfig { screen<Dest1> { } }
        val b = navigationConfig { screen<Dest2> { } }
        val c = navigationConfig { screen<Dest3> { } }
        
        val combined = a + b + c
        
        // All three should be accessible
        assertNotNull(combined)
    }
    
    @Test
    fun `buildNavNode delegates with priority`() {
        val primary = navigationConfig {
            stack<TestStack> { screen(TestDestination()) }
        }
        val secondary = navigationConfig {
            stack<TestStack> { 
                screen(TestDestination())
                screen(AnotherDestination())
            }
        }
        
        val combined = primary + secondary
        val node = combined.buildNavNode(TestStack::class) as StackNode
        
        // Secondary's definition should be used
        assertEquals(2, node.children.size)
    }
}
```

### KSP Output Tests

```kotlin
class NavigationConfigGeneratorTest {
    
    @Test
    fun `generates valid Kotlin code`() {
        val input = createTestInput(
            screens = listOf(TestScreen::class),
            containers = listOf(TestTabs::class)
        )
        
        val output = generator.generate(input)
        
        assertCompiles(output)
    }
    
    @Test
    fun `generated config includes all screens`() {
        val input = createTestInput(
            screens = listOf(Screen1::class, Screen2::class, Screen3::class)
        )
        
        val output = generator.generate(input)
        
        assertContains(output, "screen<Screen1>")
        assertContains(output, "screen<Screen2>")
        assertContains(output, "screen<Screen3>")
    }
    
    @Test
    fun `generated DSL matches expected structure`() {
        val input = createTestInput(
            tabs = listOf(
                TabDefinition(MainTabs::class, tabs = listOf(Tab1, Tab2))
            )
        )
        
        val output = generator.generate(input)
        
        assertContains(output, "tabs<MainTabs>")
        assertContains(output, "tab(MainTabs.Tab1")
        assertContains(output, "tab(MainTabs.Tab2")
    }
}
```

### Integration Tests

```kotlin
class NavigationFlowTest {
    
    @Test
    fun `one-liner setup works end to end`() = runComposeUiTest {
        setContent {
            QuoVadisNavigation(TestTabs::class)
        }
        
        // Verify initial screen renders
        onNodeWithText("Home").assertIsDisplayed()
    }
    
    @Test
    fun `tab navigation works with DSL config`() = runComposeUiTest {
        setContent {
            val navigator = rememberQuoVadisNavigator(TestTabs::class)
            NavigationHost(navigator, config = TestConfig)
        }
        
        // Navigate between tabs
        onNodeWithText("Tab 2").performClick()
        onNodeWithText("Tab 2 Content").assertIsDisplayed()
    }
    
    @Test
    fun `multi-module config composition works`() = runComposeUiTest {
        val combined = AppConfig + FeatureAConfig + FeatureBConfig
        
        setContent {
            val navigator = rememberQuoVadisNavigator(
                rootDestination = MainTabs::class,
                config = combined
            )
            NavigationHost(navigator, config = combined)
        }
        
        // Navigate to feature destination
        // Verify feature screen renders
    }
}
```

### Regression Tests

```kotlin
class LegacyApiCompatibilityTest {
    
    @Test
    fun `old NavigationHost signature still works`() = runComposeUiTest {
        setContent {
            NavigationHost(
                navigator = testNavigator,
                screenRegistry = GeneratedScreenRegistry,
                wrapperRegistry = GeneratedWrapperRegistry,
                scopeRegistry = GeneratedScopeRegistry,
                // ... old parameters
            )
        }
        
        onNodeWithText("Home").assertIsDisplayed()
    }
    
    @Test
    fun `deprecated buildNavNode functions still work`() {
        @Suppress("DEPRECATION")
        val node = buildMainTabsNavNode()
        
        assertNotNull(node)
        assertIs<TabNode>(node)
    }
    
    @Test
    fun `old registry objects still function`() {
        @Suppress("DEPRECATION")
        val scope = GeneratedScopeRegistry.getScopeKey(TestDestination())
        
        assertNotNull(scope)
    }
}
```

---

## Coverage Targets

| Module | Target | Measurement |
|--------|--------|-------------|
| `quo-vadis-core` (new code) | 90%+ | Line coverage |
| `quo-vadis-ksp` (new code) | 85%+ | Line coverage |
| DSL Builders | 95%+ | Branch coverage |
| Composition Logic | 95%+ | Branch coverage |

### Coverage Tools

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.kotlinx.kover")
}

kover {
    reports {
        verify {
            rule {
                minBound(90) // 90% minimum coverage
            }
        }
    }
}
```

---

## Acceptance Criteria Checklist

### Unit Tests
- [ ] DSL builder tests cover all permutations
- [ ] NavigationConfig interface contract tested
- [ ] Composition priority logic tested
- [ ] Empty config identity tested
- [ ] All registry types tested

### KSP Tests
- [ ] Generated code compiles
- [ ] Generated DSL structure correct
- [ ] All annotation types handled
- [ ] Error cases handled gracefully

### Integration Tests
- [ ] One-liner pattern works
- [ ] Standard pattern works
- [ ] Advanced pattern works
- [ ] Multi-module composition works
- [ ] Deep link handling works

### Regression Tests
- [ ] Old NavigationHost signature works
- [ ] Deprecated functions work with warnings
- [ ] Old registry objects functional

### Coverage
- [ ] 90%+ coverage on new core code
- [ ] 85%+ coverage on new KSP code
- [ ] Coverage report generated in CI

### Quality
- [ ] All tests pass on all platforms
- [ ] Tests are deterministic (no flaky tests)
- [ ] Test execution time reasonable (<5 min)
- [ ] Tests documented where complex

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| DSL builder unit tests | 1 day |
| Config/composition tests | 0.5 days |
| KSP output tests | 0.5 days |
| Integration tests | 1 day |
| Regression tests | 0.5 days |
| Coverage analysis & gaps | 0.5 days |
| **Total** | **3-4 days** |

---

## Implementation Notes

### Test Utilities

Create shared test utilities:

```kotlin
// TestDestinations.kt
object TestDestinations {
    data object Screen1 : Destination
    data object Screen2 : Destination
    sealed class TestTabs : Destination {
        data object Tab1 : TestTabs()
        data object Tab2 : TestTabs()
    }
}

// TestConfigBuilder.kt
fun testConfig(builder: NavigationConfigBuilder.() -> Unit): NavigationConfig {
    return navigationConfig(builder)
}
```

### Platform-Specific Tests

Some tests may need platform-specific implementations:

```kotlin
// commonTest
expect class NavigationTestRunner {
    fun runTest(block: suspend () -> Unit)
}

// androidTest
actual class NavigationTestRunner {
    actual fun runTest(block: suspend () -> Unit) {
        // Android-specific test runner
    }
}
```

### CI Integration

Ensure tests run in CI pipeline:

```yaml
# .github/workflows/test.yml
- name: Run Tests
  run: ./gradlew check

- name: Generate Coverage Report
  run: ./gradlew koverReport

- name: Upload Coverage
  uses: codecov/codecov-action@v3
```

---

## Related Files

- [Phase 5 Summary](./SUMMARY.md)
- [Task 5.1 - API Documentation](./TASK-5.1-api-documentation.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)

```
