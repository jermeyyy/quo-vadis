# Task TEST-002: Path Reconstructor Tests

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | TEST-002 |
| **Name** | Deep Link Path Reconstruction Tests |
| **Phase** | 8 - Testing Infrastructure |
| **Complexity** | High |
| **Estimated Time** | 3 days |
| **Dependencies** | KSP-006 |

## Overview

Test path reconstruction logic for various navigation scenarios.

## Test Cases

```kotlin
class PathReconstructorTest {
    
    @Test
    fun `simple stack path reconstructs correctly`() {
        val node = reconstructPathToSettings()
        assertThat(node).isInstanceOf<StackNode>()
        assertThat(node.activeDestination).isEqualTo(SettingsDestination)
    }
    
    @Test
    fun `TabNode reconstruction selects correct tab`() {
        val node = reconstructPathToProfile() // Profile is in tab 2
        assertThat(node).isInstanceOf<StackNode>()
        val tabNode = node.children.first() as TabNode
        assertThat(tabNode.activeStackIndex).isEqualTo(2)
    }
    
    @Test
    fun `deeply nested destination builds full path`() {
        // Tab -> Stack -> Screen
        val node = reconstructPathToNestedScreen()
        assertThat(node.depth).isEqualTo(3)
    }
    
    @Test
    fun `arguments are extracted from URL`() {
        val uri = Uri.parse("/product/abc123")
        val node = matchDeepLink(uri)
        val screen = node.findScreen<ProductDetail>()
        assertThat(screen.destination.productId).isEqualTo("abc123")
    }
    
    @Test
    fun `invalid URL returns null`() {
        val uri = Uri.parse("/unknown/path")
        assertThat(matchDeepLink(uri)).isNull()
    }
}
```

## Acceptance Criteria

- [ ] Simple Stack paths tested
- [ ] TabNode reconstruction (correct tab selection)
- [ ] PaneNode reconstruction
- [ ] Deeply nested scenarios
- [ ] Parameter extraction from URLs
- [ ] Error cases
