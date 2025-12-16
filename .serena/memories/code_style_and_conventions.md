# Code Style and Conventions

## Kotlin Style Guide

Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).

### Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Classes/Interfaces | `PascalCase` | `Navigator`, `NavNode`, `TreeMutator` |
| Functions/Properties | `camelCase` | `navigate()`, `navigateBack()`, `mutate()` |
| Constants | `SCREAMING_SNAKE_CASE` | `DEFAULT_TRANSITION`, `MAX_STACK_SIZE` |
| Destinations | `PascalCase` | `HomeDestination`, `DetailDestination` |
| Test Fakes | `Fake + Name` | `FakeNavigator`, `FakeNavRenderScope` |
| Default Implementations | `Default + Name` | `DefaultNavigator` |
| Node Types | `*Node` suffix | `ScreenNode`, `StackNode`, `TabNode`, `PaneNode` |

### Formatting

- **Indentation**: 4 spaces (no tabs)
- **Max line length**: 120 characters
- **Imports**: No wildcard imports (except `java.util.*`)
- **Braces**: K&R style (opening brace on same line)

### File Structure

```kotlin
// 1. Package declaration
package com.jermey.quo.vadis.core.navigation.core

// 2. Imports (no wildcards, sorted)
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

// 3. Class/Interface documentation
/**
 * Brief description of the class.
 *
 * Detailed explanation if needed.
 */
interface Navigator {
    // 4. Properties first
    val state: StateFlow<NavNode>
    
    // 5. Methods second
    fun navigate(destination: Destination)
}
```

## Documentation Style

### KDoc Comments

All public APIs **must** have KDoc comments:

```kotlin
/**
 * Navigates to the specified destination.
 *
 * This function pushes a new destination onto the navigation stack.
 *
 * @param destination The target destination to navigate to
 * @param transition Optional custom transition animation
 * @return true if navigation was successful, false otherwise
 * @throws IllegalStateException if the navigator is not initialized
 */
fun navigate(
    destination: Destination,
    transition: NavigationTransition? = null
): Boolean
```

### Code Comments

- Use `//` for single-line comments
- Use `/* */` for multi-line comments
- Avoid `TODO:`, `FIXME:`, `STOPSHIP:` markers (enforced by detekt)

## Compose Conventions

### Composable Functions

- Use `@Composable` annotation
- Function names can start with uppercase (detekt allows this)
- Use `Modifier` as first optional parameter
- Follow slot-based API patterns

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade,
    content: @Composable () -> Unit = {}
) {
    // Implementation
}
```

### State Management

- Use `remember` for local state
- Use `StateFlow` for navigation state
- Prefer immutable data classes for state

## Testing Conventions

### Test Naming

Use descriptive test names with backticks:

```kotlin
@Test
fun `navigate should push destination onto stack`() {
    // Test implementation
}

@Test
fun `navigateBack should pop the current destination`() {
    // Test implementation
}
```

### Test Structure

Follow Arrange-Act-Assert pattern:

```kotlin
@Test
fun `test description`() {
    // Arrange
    val navigator = FakeNavigator()
    val destination = HomeDestination
    
    // Act
    navigator.navigate(destination)
    
    // Assert
    assertTrue(navigator.verifyNavigateTo("home"))
}
```

## Detekt Rules

Key detekt rules enforced (see `config/detekt/detekt.yml`):

- **CyclomaticComplexMethod**: Max complexity 14
- **LargeClass**: Max 600 lines
- **LongMethod**: Max 60 lines
- **LongParameterList**: Max 5 function params, 6 constructor params
- **TooManyFunctions**: Max 11 per file/class
- **ReturnCount**: Max 2 returns per function
- **MagicNumber**: Avoid magic numbers (except -1, 0, 1, 2)
- **WildcardImport**: Forbidden (except `java.util.*`)

## Architecture Patterns

### Destination Pattern

```kotlin
// Sealed class for type-safe destinations
sealed class AppDestination : Destination {
    data object Home : AppDestination() {
        override val route = "home"
    }
    
    data class Detail(val id: String) : AppDestination() {
        override val route = "detail"
        override val arguments = mapOf("id" to id)
    }
}
```

### TypedDestination Pattern

```kotlin
@Serializable
data class DetailData(val itemId: String, val mode: String = "view")

data class DetailDestination(
    val itemId: String,
    val mode: String = "view"
) : Destination, TypedDestination<DetailData> {
    override val route = "detail"
    override val data = DetailData(itemId, mode)
}
```

## Commit Message Format

Follow conventional commits:

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting)
- `refactor:` Code refactoring
- `test:` Test additions or changes
- `chore:` Build/tooling changes

Example: `feat: add predictive back navigation for iOS`
