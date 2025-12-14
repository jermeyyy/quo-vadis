# Contributing to Quo Vadis Navigation Library

Thank you for your interest in contributing to Quo Vadis! This document provides guidelines and instructions for contributing to the project.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Documentation](#documentation)
- [Updating Documentation Website](#updating-documentation-website)
- [Submitting Changes](#submitting-changes)

## Code of Conduct

This project adheres to a code of conduct that all contributors are expected to follow. Please be respectful and constructive in all interactions.

## Getting Started

1. **Fork the repository**
   ```bash
   # Click "Fork" on GitHub
   ```

2. **Clone your fork**
   ```bash
   git clone https://github.com/YOUR_USERNAME/quo-vadis.git
   cd quo-vadis
   ```

3. **Add upstream remote**
   ```bash
   git remote add upstream https://github.com/jermeyyy/quo-vadis.git
   ```

4. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Workflow

### Building the Project

```bash
# Build all modules
./gradlew build

# Build only the library
./gradlew :quo-vadis-core:build

# Build demo app
./gradlew :composeApp:assembleDebug
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run library tests only
./gradlew :quo-vadis-core:test

# Run Android tests
./gradlew :quo-vadis-core:testDebugUnitTest
```

### Code Quality

```bash
# Run detekt (static analysis)
./gradlew detekt

# Format code (if configured)
./gradlew ktlintFormat
```

## Coding Standards

### Kotlin Style

Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use **PascalCase** for classes and interfaces
- Use **camelCase** for functions and properties
- Use **4 spaces** for indentation (no tabs)
- Maximum line length: **120 characters**

### Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Classes/Interfaces | `PascalCase` | `Navigator`, `NavNode`, `TreeMutator` |
| Functions/Properties | `camelCase` | `navigate()`, `navigateBack()`, `mutate()` |
| Destinations | `PascalCase + Destination` | `HomeDestination` |
| Test Fakes | `Fake + Name` | `FakeNavigator` |
| Default Implementations | `Default + Name` | `DefaultNavigator` |

### Documentation

All public APIs **must** have KDoc comments:

```kotlin
/**
 * Brief description of what this does.
 * 
 * More detailed explanation if needed.
 * 
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType When this exception is thrown
 */
fun myFunction(paramName: String): Result
```

### Package Structure

**Library (`quo-vadis-core`):**
```
com.jermey.quo.vadis.core.navigation/
├── core/          # Core navigation interfaces and classes
├── node/          # NavNode types and tree structures
├── tree/          # TreeMutator and tree operations
├── compose/       # Compose-specific components (NavigationHost)
├── render/        # Flattening and rendering logic
├── integration/   # External integrations
├── testing/       # Test utilities
├── utils/         # Utility functions
└── serialization/ # Serialization support
```

**FlowMVI Module (`quo-vadis-core-flow-mvi`):**
For MVI architecture integration with FlowMVI library.

**Demo (`composeApp`):**
```
com.jermey.navplayground.demo/
├── destinations/  # Navigation destinations
├── graphs/        # Navigation graphs
└── ui/screens/    # UI screens
```

## Testing

### Unit Tests

- Write tests for all new features
- Use `FakeNavigator` for testing navigation logic
- Aim for high code coverage

```kotlin
@Test
fun `test navigation to details screen`() {
    val fakeNavigator = FakeNavigator()
    val viewModel = MyViewModel(fakeNavigator)
    
    viewModel.onItemClicked("123")
    
    assertTrue(fakeNavigator.verifyNavigateTo("details"))
}
```

### Platform Testing

- Test on multiple platforms when adding multiplatform features
- Verify Android and iOS specific behaviors
- Test web targets when modifying Compose UI

## Documentation

### Code Documentation

- Update KDoc comments when changing public APIs
- Include usage examples in documentation
- Document platform-specific behavior

### Markdown Documentation

When updating documentation in `quo-vadis-core/docs/`:

1. Follow existing structure and formatting
2. Include code examples
3. Update table of contents if adding new sections
4. Link to related documentation

### API Documentation

Dokka automatically generates API documentation from KDoc comments:

```bash
./gradlew :quo-vadis-core:dokkaGenerateHtml
open quo-vadis-core/build/dokka/html/index.html
```

## Updating Documentation Website

The documentation website is automatically deployed via GitHub Actions when changes are pushed to the `main` branch.

### Making Changes to the Website

1. **Edit files in `/docs/site/`**
   ```bash
   # Static HTML pages
   docs/site/index.html
   docs/site/getting-started.html
   docs/site/features.html
   docs/site/demo.html
   ```

2. **Update styles**
   ```bash
   # Edit CSS
   docs/site/css/style.css
   ```

3. **Update JavaScript**
   ```bash
   # Edit JS functionality
   docs/site/js/main.js
   ```

4. **For API documentation changes**
   - Edit KDoc comments in source code
   - Dokka automatically regenerates on deployment
   - No manual steps required

### Local Testing

**Static site:**
```bash
open docs/site/index.html
```

**API documentation:**
```bash
./gradlew :quo-vadis-core:dokkaGenerateHtml
open quo-vadis-core/build/dokka/html/index.html
```

**Full preview:**
```bash
# Create preview structure
mkdir -p _site_preview
cp -r docs/site/* _site_preview/
./gradlew :quo-vadis-core:dokkaGenerateHtml
cp -r quo-vadis-core/build/dokka/html _site_preview/api

# Open in browser
open _site_preview/index.html

# Clean up
rm -rf _site_preview
```

### Deployment Process

1. Commit changes to `main` branch (or create PR)
2. GitHub Actions automatically:
   - Generates Dokka documentation
   - Copies static site files
   - Deploys to GitHub Pages
3. Site updates at: https://jermeyyy.github.io/quo-vadis/

## Submitting Changes

### Pull Request Process

1. **Update your fork**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Make your changes**
   - Follow coding standards
   - Add tests for new features
   - Update documentation

3. **Commit your changes**
   ```bash
   git add .
   git commit -m "feat: add new feature"
   ```

   **Commit message format:**
   - `feat:` New feature
   - `fix:` Bug fix
   - `docs:` Documentation changes
   - `style:` Code style changes (formatting)
   - `refactor:` Code refactoring
   - `test:` Test additions or changes
   - `chore:` Build/tooling changes

4. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

5. **Create Pull Request**
   - Go to GitHub and create a PR
   - Fill in the PR template
   - Link related issues
   - Wait for review

### PR Checklist

Before submitting a PR, ensure:

- [ ] Code follows Kotlin coding conventions
- [ ] All tests pass (`./gradlew test`)
- [ ] New tests added for new features
- [ ] All public APIs have KDoc comments
- [ ] Documentation updated if needed
- [ ] No compilation warnings
- [ ] Detekt passes (`./gradlew detekt`)
- [ ] Changes work on multiple platforms (if applicable)
- [ ] PR description clearly explains the changes

### Review Process

1. Maintainers will review your PR
2. Address any feedback or requested changes
3. Once approved, your PR will be merged
4. Your contribution will be included in the next release

## Questions?

If you have questions or need help:

- Open an issue: https://github.com/jermeyyy/quo-vadis/issues
- Check existing documentation
- Review closed PRs for examples

Thank you for contributing to Quo Vadis! 🚀
