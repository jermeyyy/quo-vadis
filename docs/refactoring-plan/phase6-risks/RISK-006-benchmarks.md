# Task RISK-006: Performance Benchmarks

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | RISK-006 |
| **Name** | Navigation Performance Benchmark Suite |
| **Phase** | 6 - Risk Mitigation |
| **Complexity** | Medium |
| **Estimated Time** | 3 days |
| **Dependencies** | All core implementations |

## Risk Being Mitigated

**Performance Regression**: New architecture slower than old.

## Implementation

```kotlin
// composeApp/src/androidTest/kotlin/.../NavigationBenchmarks.kt

@RunWith(AndroidJUnit4::class)
class NavigationBenchmarks {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    @Test
    fun benchmarkTreeFlattening() {
        val largeTree = createTestTree(depth = 5, breadth = 10) // ~10K nodes
        
        benchmarkRule.measureRepeated {
            flattenState(largeTree)
        }
    }
    
    @Test
    fun benchmarkPushOperation() {
        val navigator = Navigator()
        
        benchmarkRule.measureRepeated {
            navigator.push(TestDestination)
            navigator.pop()
        }
    }
    
    @Test
    fun benchmarkTabSwitch() {
        val navigator = Navigator(initialState = createTabNode(5))
        
        benchmarkRule.measureRepeated {
            repeat(5) { navigator.switchTab(it) }
        }
    }
}
```

## Metrics to Track

| Metric | Baseline | Threshold |
|--------|----------|-----------|
| flattenState (100 nodes) | <1ms | <2ms |
| push/pop cycle | <0.5ms | <1ms |
| Tab switch | <0.5ms | <1ms |
| Deep link reconstruction | <5ms | <10ms |

## Acceptance Criteria

- [ ] Benchmarks for all critical paths
- [ ] Baseline established before refactor
- [ ] CI fails if regression >20%
- [ ] Memory usage tracked
