# Debug: ResultDemoDestination Tab Missing

## Problem
Only 4 tabs visible in MainTabs instead of 5. ResultDemoDestination from feature1 is missing.

## Root Cause Chain
1. ComposeAppNavigationConfig has `containerTab<ResultDemoDestination>()` - CORRECT
2. Feature1NavigationConfig has `stack<ResultDemoDestination>()` - CORRECT  
3. Configs composed via `+` operator in DI.kt - CORRECT
4. TabRenderer's `getTabDestinations()` uses `mapNotNull` + `findFirstScreenDestination(stack)` - filters out tabs with empty stacks
5. buildTabStack -> ContainerReference -> buildNavNode(ResultDemoDestination) returns null locally -> nodeResolver should fallback

## CRITICAL: Generated code imports
ComposeAppNavigationConfig imports:
```
import com.jermey.quo.vadis.core.navigation.`internal`.config.CompositeNavigationConfig
```
This means: The `+` operator on DslNavigationConfig (public) creates an INTERNAL CompositeNavigationConfig.

### Key observation in generated config:
```kotlin
override fun plus(other: NavigationConfig): NavigationConfig {
    if (other === EmptyNavigationConfig) return this
    return CompositeNavigationConfig(this, other)  // This CompositeNavigationConfig is the INTERNAL variant
}
```

## Files to check:
- Internal CompositeNavigationConfig: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/config/CompositeNavigationConfig.kt` - needs init block with setNodeResolver
- Public DslNavigationConfig: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/DslNavigationConfig.kt` - nodeResolver at line 83
- Internal DslNavigationConfig: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/internal/DslNavigationConfig.kt`
- NavigationConfig interface: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/config/NavigationConfig.kt`

## Generated config pattern
The `navigationConfig {}` DSL builder (public) creates a PUBLIC DslNavigationConfig.
The `plus()` operator on the public DslNavigationConfig creates an INTERNAL CompositeNavigationConfig.
So: CompositeNavigationConfig(INTERNAL).init must call setNodeResolver on PUBLIC DslNavigationConfig children.

## Check: Does public DslNavigationConfig.plus() use public or internal CompositeNavigationConfig?
See line ~150 in public DslNavigationConfig.kt
