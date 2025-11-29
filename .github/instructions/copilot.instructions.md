---
applyTo: '**'
---
# Quo Vadis Navigation Library - Development Guide

## 🎯 PROJECT ESSENTIALS

### Project Structure
**NavPlayground** = Kotlin Multiplatform navigation library demonstration

**TWO COMPONENTS:**
1. **`quo-vadis-core`** - Navigation library (independent, reusable, publishable)
2. **`composeApp`** - Demo app (showcases all patterns)

### Technology Stack
- **Kotlin** 2.2.20 (Multiplatform)
- **Compose Multiplatform** 1.9.0
- **Android** Min SDK 24, Target/Compile SDK 36
- **Platforms**: Android, iOS (x3), JavaScript, WebAssembly, Desktop (JVM)
- **NO external navigation dependencies** in core module

### Core Principles
1. **TYPE SAFETY FIRST** - Compile-time safe, NO string routes
2. **REACTIVE STATE** - StateFlow for state, SharedFlow for events
3. **MODULARIZATION** - Gray box pattern for features
4. **MVI SUPPORT** - First-class MVI integration
5. **TESTABILITY** - Use `FakeNavigator`
6. **PLATFORM AGNOSTIC** - Platform code ONLY in platform source sets

---

## 🚨 CRITICAL WORKFLOW RULES

### BEFORE ANY CHANGES:

1. **READ DOCS** → `quo-vadis-core/docs/` + `README.md`
2. **CHECK MEMORIES** → Use `mcp_serena_read_memory` (see Memory Files below)
3. **USE SYMBOL TOOLS** → `mcp_serena_get_symbols_overview` BEFORE reading files
4. **ASK IF UNCERTAIN** → Use `mcp_serena_ask_user` (NEVER finish by asking!)
5. **VERIFY CHANGES** → Run `./gradlew :composeApp:assembleDebug` (FASTEST)
6. **RESEARCH APIS** → Use `mcp_docs-mcp_*` or `mcp_javadoc-mcp_*` tools

### NEVER DO:
❌ String-based navigation (use type-safe destinations)  
❌ Add external navigation libraries to `quo-vadis-core`  
❌ Platform-specific code in `commonMain`  
❌ Commit without running tests  
❌ Skip documentation updates for public API  
❌ Generate summary markdown files (use memories!)  
❌ Trust IDE errors blindly (Gradle = source of truth for KMP)  
❌ Hallucinate API usage (verify with docs!)  

---

## 🔧 TOOLS & COMMANDS

### Tool Priority (Serena IDE-Assistant Mode)

**For File Operations** (PREFER BUILTIN):
- `read_file` - Read file contents
- `replace_string_in_file` - Targeted replacements (PRIMARY)
- `create_file` - Create new files

**For Symbol Discovery** (USE SERENA):
- `mcp_serena_get_symbols_overview` - File structure overview
- `mcp_serena_find_symbol` - Find by name path
- `mcp_serena_find_referencing_symbols` - Find usages
- `mcp_serena_search_for_pattern` - Pattern search
- `mcp_serena_list_dir` - Directory listing

**For Decisions & Memory**:
- `mcp_serena_ask_user` - Ask when uncertain
- `mcp_serena_read_memory` - Access memories
- `mcp_serena_write_memory` - Save to memories

**For API Research**:
- `mcp_docs-mcp_search_docs` - Library docs
- `mcp_javadoc-mcp_search_artifact` - Maven artifacts
- `mcp_javadoc-mcp_get_class_documentation` - Class docs

**For Gradle Builds** (PRIMARY):
- `mcp_gradle-mcp_run_task` - Run any Gradle task (PREFERRED over terminal!)
- `mcp_gradle-mcp_list_projects` - List projects
- `mcp_gradle-mcp_list_project_tasks` - List available tasks
- `mcp_gradle-mcp_clean` - Clean build artifacts

**For Verification**:
- `get_errors` - IDE errors (may show KMP false positives)
- `run_in_terminal` - Only for non-Gradle commands (file ops, git, etc.)

### Essential Commands

**FASTEST VERIFICATION** (Priority #1 - Use Gradle MCP):
```python
mcp_gradle-mcp_run_task(task=":composeApp:assembleDebug")  # 2 seconds
```

**Other Build Commands** (Use Gradle MCP):
```python
mcp_gradle-mcp_run_task(task="clean")                      # Clean build
mcp_gradle-mcp_run_task(task="build")                      # Full build
mcp_gradle-mcp_run_task(task=":quo-vadis-core:build", args=["-x", "detekt"])
mcp_gradle-mcp_run_task(task=":composeApp:linkDebugFrameworkIosSimulatorArm64")
mcp_gradle-mcp_run_task(task=":composeApp:jsBrowserDevelopmentRun")
mcp_gradle-mcp_run_task(task=":composeApp:wasmJsBrowserDevelopmentRun")
mcp_gradle-mcp_run_task(task=":composeApp:run")
```

**Testing** (Use Gradle MCP):
```python
mcp_gradle-mcp_run_task(task="test")
mcp_gradle-mcp_run_task(task=":quo-vadis-core:testDebugUnitTest")
```

**See `gradle_mcp_tools` memory** for complete tool documentation and examples.

**Cleaning**:
```bash
./gradlew --stop
rm -rf .gradle/configuration-cache
./gradlew clean
```

### Memory Files

**Available via `mcp_serena_read_memory`**:
- `project_overview` - Structure, purpose, platforms
- `codebase_structure` - File organization
- `tech_stack` - Dependencies, versions
- `architecture_patterns` - Design patterns
- `code_style_and_conventions` - Naming, style
- `suggested_commands` - Build/test commands
- `task_completion_checklist` - QA checklist

**Save via `mcp_serena_write_memory`** (NOT markdown files!)

### Documentation References

**Library Docs** (`quo-vadis-core/docs/`):
- `ARCHITECTURE.md` - Design principles
- `API_REFERENCE.md` - Complete API
- `NAVIGATION_IMPLEMENTATION.md` - Implementation
- `MULTIPLATFORM_PREDICTIVE_BACK.md` - Platform features

**Project Root**:
- `README.md` - Platform support, quick start
- `PUBLISHING.md` - Maven publishing
- `CONTRIBUTING.md` - Contribution guidelines
- `docs/SITE_MAINTENANCE.md` - Website maintenance guide

**Documentation Website**:
- URL: https://jermeyyy.github.io/quo-vadis/
- Static site: `/docs/site/` (HTML, CSS, JS, images)
- API docs: Auto-generated from Dokka during deployment
- Workflow: `.github/workflows/deploy-pages.yml`
- Deployment: Automatic on push to `main` branch

---

## ✅ VERIFICATION WORKFLOW

### Task Completion Checklist

**BEFORE COMMITTING**:
1. ✅ Kotlin official style
2. ✅ ALL public APIs have KDoc
3. ✅ Run `./gradlew :composeApp:assembleDebug` (FASTEST FIRST!)
4. ✅ Run `./gradlew test` (MUST pass)
5. ✅ Verify with Gradle (IDE may show false KMP errors)
6. ✅ Update docs if API/architecture changed
7. ✅ No platform code in `commonMain`
8. ✅ Test Android + iOS if UI changed
9. ✅ Use `FakeNavigator` for nav tests
10. ✅ No debug prints, commented code, TODOs
11. ✅ Save important info to memories
12. ✅ NO summary markdown files

**Quality Gates**:
- No compilation errors (Gradle = truth)
- All tests pass
- Proper null safety
- Thread-safe (StateFlow, not mutable state)
- No memory leaks

---

## 📝 CODE GUIDELINES

### Naming Conventions (STRICT)

| Type | Convention | Example |
|------|-----------|---------|
| Classes/Interfaces | `PascalCase` | `Navigator`, `BackStack` |
| Functions/Properties | `camelCase` | `navigate()`, `navigateBack()` |
| Destinations | `PascalCase + Destination` | `HomeDestination` |
| Test Fakes | `Fake + Name` | `FakeNavigator` |
| Default Impls | `Default + Name` | `DefaultNavigator` |

### Package Structure

**Demo**: `com.jermey.navplayground.demo.<feature>`  
**Library**: `com.jermey.quo.vadis.core.navigation.<package>`
- Subpackages: `core`, `compose`, `integration`, `testing`, `utils`, `serialization`
- **FlowMVI**: Use `quo-vadis-core-flow-mvi` module for MVI architecture

### Kotlin Best Practices

**DO**:
- Sealed classes for destination hierarchies
- Data classes for destinations with arguments
- Extension functions in `utils`
- DSL builders with lambda receivers
- StateFlow/SharedFlow (NOT callbacks)
- Explicit null safety

**Compose**:
- Composables: `PascalCase`
- `Modifier` parameter last with default
- State hoisting
- `remember` for stable state
- `LaunchedEffect` for side effects

### Documentation

**ALL public APIs MUST have KDoc**:
```kotlin
/**
 * Brief description.
 * 
 * Detailed explanation.
 * 
 * @param paramName Parameter description
 * @return Return value description
 */
```

Update markdown docs when architecture/API changes.

### Updating Documentation Website

**Static Site Content** (`/docs/site/`):
- Edit HTML files directly for content changes
- Update CSS in `/docs/site/css/style.css`
- Update JavaScript in `/docs/site/js/main.js`
- Add images to `/docs/site/images/`

**API Documentation**:
- Update KDoc comments in source code
- Dokka auto-generates during GitHub Actions deployment
- No manual regeneration needed

**Deployment**:
- Automatic on push to `main` branch
- GitHub Actions workflow: `.github/workflows/deploy-pages.yml`
- Monitor at: https://github.com/jermeyyy/quo-vadis/actions

**Local Preview**:
```bash
# Static site
open docs/site/index.html

# Dokka docs
./gradlew :quo-vadis-core:dokkaGenerateHtml
open quo-vadis-core/build/dokka/html/index.html
```

See `docs/SITE_MAINTENANCE.md` for detailed instructions.

### Multiplatform Rules

**Source Sets**:
- `commonMain` - ALL core logic, Compose UI, platform-agnostic
- `androidMain` - Android ONLY (system back, deep links)
- `iosMain` - iOS ONLY (navigation bar, universal links)
- NEVER platform APIs in `commonMain`
- Use expect/actual for platform differences

**Build Targets**: Android, iOS (x64/Arm64/Simulator), JS, Wasm, Desktop

---

## 📚 QUICK REFERENCE

### Key Components

- **Destination** - Navigation target (sealed classes preferred)
- **Navigator** - Central controller (reactive state)
- **BackStack** - Direct stack manipulation
- **NavigationGraph** - Modular graphs (DSL)
- **NavHost** - Compose rendering + transitions

### State Flow
```
User Action → Intent → Navigator → BackStack → State Change → UI Recomposition
```

### Common Patterns

**Type-Safe Destination**:
```kotlin
sealed class FeatureDestination : Destination {
    object Home : FeatureDestination() {
        override val route = "home"
    }
    data class Details(val id: String) : FeatureDestination() {
        override val route = "details"
        override val arguments = mapOf("id" to id)
    }
}
```

**Navigation Graph**:
```kotlin
navigationGraph("feature") {
    startDestination(Screen1)
    destination(Screen1) { _, nav -> Screen1UI(nav) }
    destination(Screen2) { _, nav -> Screen2UI(nav) }
}
```

**Navigation Operations**:
```kotlin
navigator.navigate(DetailsDestination("123"))
navigator.navigate(Details, NavigationTransitions.SlideHorizontal)
navigator.navigateAndClearTo(Home, "login", inclusive = true)
navigator.navigateBack()
```

**Testing**:
```kotlin
val fakeNavigator = FakeNavigator()
viewModel.navigate(Details)
assert(fakeNavigator.lastDestination == Details)
```

### File Structure

**Demo** (`composeApp/src/commonMain/kotlin/`):
```
com/jermey/navplayground/
├── App.kt
└── demo/
    ├── DemoApp.kt
    ├── destinations/
    ├── graphs/
    └── ui/screens/
```

**Library** (`quo-vadis-core/src/commonMain/kotlin/`):
```
com/jermey/quo/vadis/core/navigation/
├── core/
├── compose/
├── integration/
├── serialization/
├── testing/
└── utils/
```

**FlowMVI Module** (`quo-vadis-core-flow-mvi/`): FlowMVI architecture integration

---

## 🎯 GITHUB COPILOT OPTIMIZATION

**For Best Results**:
1. **Activate project** - Use `mcp_serena_activate_project`
2. **Use structured queries** - Reference specific components
3. **Leverage context** - Open relevant files in VS Code
4. **Check memories first** - Use `mcp_serena_read_memory`
5. **Ask early** - Use `mcp_serena_ask_user` when uncertain
6. **Verify fast** - Prioritize Android debug builds
7. **Trust Gradle** - Not IDE for KMP errors
8. **Research APIs** - Use MCP docs/javadoc tools
9. **Save to memories** - NOT markdown files

**Problem-Solving Approach**:
1. Read docs/memories FIRST
2. Use symbol tools (not blind file reading)
3. Make minimal changes (`replace_string_in_file`)
4. Verify immediately (errors after EVERY edit)
5. Test thoroughly
6. Document changes
7. Reference existing docs (don't duplicate)

---

## 🔴 FINAL CRITICAL REMINDERS

1. **READ DOCUMENTATION BEFORE CODING**
2. **USE SYMBOL DISCOVERY TOOLS** (not blind file reading)
3. **VERIFY WITH GRADLE** (IDE shows false KMP errors)
4. **RUN TESTS BEFORE COMPLETING**
5. **MAINTAIN TYPE SAFETY** (no string routing)
6. **KEEP commonMain PLATFORM-AGNOSTIC**
7. **DOCUMENT ALL PUBLIC APIs**
8. **USE FakeNavigator FOR TESTING**
9. **ASK USER WHEN UNCERTAIN**
10. **SAVE TO MEMORIES, NOT FILES**

**When in doubt**: Memories → Docs → Symbol Tools → Tests → Ask User

