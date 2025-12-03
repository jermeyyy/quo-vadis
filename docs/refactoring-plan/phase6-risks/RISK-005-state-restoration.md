# Task RISK-005: State Restoration Validation

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | RISK-005 |
| **Name** | Process Death Survival Testing |
| **Phase** | 6 - Risk Mitigation |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | CORE-004 |

## Risk Being Mitigated

**State Loss**: NavNode tree not surviving process death.

## Implementation

```kotlin
// quo-vadis-core/src/commonTest/kotlin/.../StateRestorationTest.kt

class StateRestorationTest {
    
    @Test
    fun `NavNode serializes and deserializes correctly`() {
        val original = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", DetailDestination("123"))
            )
        )
        
        val json = Json.encodeToString(original)
        val restored = Json.decodeFromString<NavNode>(json)
        
        assertEquals(original, restored)
    }
    
    @Test
    fun `deeply nested tree survives serialization`() {
        // Test 10+ levels of nesting
    }
    
    @Test
    fun `TabNode with all stacks serializes correctly`() {
        // Test TabNode with multiple populated stacks
    }
}
```

## Acceptance Criteria

- [ ] @Serializable works for all NavNode types
- [ ] Large trees (100+ nodes) serialize correctly
- [ ] SavedStateHandle integration tested on Android
- [ ] Edge cases: empty trees, circular refs handled
