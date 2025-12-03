# Task TEST-001: KSP Generator Unit Tests

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | TEST-001 |
| **Name** | NavNode Generator Test Suite |
| **Phase** | 8 - Testing Infrastructure |
| **Complexity** | Medium |
| **Estimated Time** | 3 days |
| **Dependencies** | KSP-004 |

## Overview

Create comprehensive unit tests for new KSP generators using compile-testing library.

## Implementation

```kotlin
// quo-vadis-ksp/src/test/kotlin/.../NavNodeGeneratorTest.kt

class NavNodeGeneratorTest {
    
    @Test
    fun `STACK graph generates StackNode builder`() {
        val result = compile("""
            @Graph(name = "test", type = GraphType.STACK)
            sealed class TestGraph {
                @Route object Screen1 : TestGraph()
                @Route object Screen2 : TestGraph()
            }
        """)
        
        assertThat(result.exitCode).isEqualTo(OK)
        assertThat(result.generatedFile("TestGraphNavNodeBuilder.kt"))
            .contains("StackNode")
            .contains("ScreenNode")
    }
    
    @Test
    fun `TAB graph generates TabNode builder`() { ... }
    
    @Test
    fun `PANE graph generates PaneNode builder`() { ... }
    
    @Test
    fun `nested graph generates correct parent references`() { ... }
    
    @Test
    fun `invalid annotation usage produces error`() { ... }
}
```

## Files Affected

| File | Change Type |
|------|-------------|
| `quo-vadis-ksp/src/test/kotlin/.../NavNodeGeneratorTest.kt` | New |

## Acceptance Criteria

- [ ] Uses compile-testing library
- [ ] Tests each GraphType generation
- [ ] Tests nested graph scenarios
- [ ] Tests error handling
- [ ] Verifies generated code compiles
