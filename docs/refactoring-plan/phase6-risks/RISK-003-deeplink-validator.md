# Task RISK-003: Deep Link Tree Validator

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | RISK-003 |
| **Name** | Deep Link Path Validation |
| **Phase** | 6 - Risk Mitigation |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | KSP-006 |

## Risk Being Mitigated

**Invalid Deep Link State**: Reconstructed paths might have invalid parent nodes or missing arguments.

## Implementation

```kotlin
// quo-vadis-core/src/commonMain/kotlin/.../DeepLinkValidator.kt

class DeepLinkValidator {
    
    fun validate(node: NavNode): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Check parent-child relationships
        validateParentKeys(node, null, errors)
        
        // Check TabNode has valid index
        if (node is TabNode && node.activeStackIndex !in node.stacks.indices) {
            errors += InvalidTabIndex(node.activeStackIndex, node.stacks.size)
        }
        
        // Check required arguments
        validateArguments(node, errors)
        
        return ValidationResult(errors.isEmpty(), errors)
    }
}

sealed class ValidationError {
    data class InvalidParentKey(val nodeKey: String, val expected: String?, val actual: String?) : ValidationError()
    data class InvalidTabIndex(val index: Int, val maxSize: Int) : ValidationError()
    data class MissingArgument(val destination: String, val argument: String) : ValidationError()
}
```

## Acceptance Criteria

- [ ] Validates parent-child key relationships
- [ ] Validates TabNode activeStackIndex bounds
- [ ] Validates required destination arguments
- [ ] Provides clear error messages
