# Task TEST-005: Performance Regression Tests

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | TEST-005 |
| **Name** | Performance Baseline Tests |
| **Phase** | 8 - Testing Infrastructure |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | RISK-006 |

## Overview

Create tests that fail if performance regresses beyond thresholds.

## Implementation

```kotlin
class PerformanceRegressionTest {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    companion object {
        // Baselines from current implementation
        const val FLATTEN_BASELINE_MS = 1.0
        const val PUSH_POP_BASELINE_MS = 0.5
        const val TAB_SWITCH_BASELINE_MS = 0.5
        const val REGRESSION_THRESHOLD = 0.2 // 20%
    }
    
    @Test
    fun `tree flattening within threshold`() {
        val tree = createTestTree(100)
        
        val measurement = benchmarkRule.measureRepeated {
            flattenState(tree)
        }
        
        val avgMs = measurement.getMetric("timeNs").median / 1_000_000.0
        assertThat(avgMs).isLessThan(FLATTEN_BASELINE_MS * (1 + REGRESSION_THRESHOLD))
    }
    
    @Test
    fun `push pop cycle within threshold`() { ... }
    
    @Test
    fun `tab switch within threshold`() { ... }
}
```

## CI Integration

```yaml
# .github/workflows/benchmark.yml
- name: Run Benchmarks
  run: ./gradlew :composeApp:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=PerformanceRegressionTest
```

## Acceptance Criteria

- [ ] Baseline metrics established
- [ ] <20% variance threshold
- [ ] CI integration
- [ ] Clear failure messages
