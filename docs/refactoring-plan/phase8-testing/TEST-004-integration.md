# Task TEST-004: Integration Tests with Demo App

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | TEST-004 |
| **Name** | Demo App Integration Test Suite |
| **Phase** | 8 - Testing Infrastructure |
| **Complexity** | High |
| **Estimated Time** | 4 days |
| **Dependencies** | All core implementations |

## Overview

Create integration tests using demo app for end-to-end validation.

## Test Scenarios

```kotlin
class NavigationIntegrationTest {
    
    @get:Rule
    val composeRule = createComposeRule()
    
    @Test
    fun `full navigation flow completes successfully`() {
        composeRule.setContent { DemoApp() }
        
        // Navigate through tabs
        composeRule.onNodeWithText("Profile").performClick()
        composeRule.onNodeWithText("Profile Screen").assertIsDisplayed()
        
        // Navigate to detail
        composeRule.onNodeWithText("Item 1").performClick()
        composeRule.onNodeWithText("Detail Screen").assertIsDisplayed()
        
        // Back navigation
        Espresso.pressBack()
        composeRule.onNodeWithText("Profile Screen").assertIsDisplayed()
    }
    
    @Test
    fun `shared element transition triggers`() {
        // Verify AnimatedVisibilityScope provided
    }
    
    @Test
    fun `predictive back gesture works`() {
        // Test gesture handling
    }
    
    @Test
    fun `deep link navigation works end-to-end`() {
        // Test deep link handling
    }
}
```

## Acceptance Criteria

- [ ] Full navigation flows tested
- [ ] SharedElement transitions verified
- [ ] PredictiveBack gesture handling tested
- [ ] Deep link end-to-end tested
- [ ] Uses Compose testing APIs
