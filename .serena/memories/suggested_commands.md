# Suggested Commands for NavPlayground

## Build Commands

> **Note**: Prefer using **Gradle MCP tools** (`mcp_gradle-mcp_run_task`) over terminal commands for all Gradle tasks. Terminal commands are shown for reference only.

### Android

#### Build Debug APK (FASTEST VERIFICATION)
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:assembleDebug")
```
**Terminal:**
```bash
./gradlew :composeApp:assembleDebug
```

#### Build Release APK
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:assembleRelease")
```
**Terminal:**
```bash
./gradlew :composeApp:assembleRelease
```

#### Install Debug on Connected Device
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:installDebug")
```
**Terminal:**
```bash
./gradlew :composeApp:installDebug
```

#### Build Library Module
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":quo-vadis-core:build")
```
**Terminal:**
```bash
./gradlew :quo-vadis-core:build
```

### iOS

#### Build iOS Framework (M1/M2 Macs)
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:linkDebugFrameworkIosSimulatorArm64")
```
**Terminal:**
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

#### Open iOS Project in Xcode
**Terminal (Non-Gradle):**
```bash
open iosApp/iosApp.xcodeproj
```

## Publishing Commands

### Publish to Maven Local
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":quo-vadis-core:publishToMavenLocal")
```
**Terminal:**
```bash
./gradlew :quo-vadis-core:publishToMavenLocal
```
Publishes all platform artifacts to `~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/0.1.0-SNAPSHOT/`

### Verify Published Artifacts
**Terminal (Non-Gradle):**
```bash
ls ~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/0.1.0-SNAPSHOT/
```

Expected artifacts:
- `quo-vadis-core-android-0.1.0-SNAPSHOT.aar`
- `quo-vadis-core-desktop-0.1.0-SNAPSHOT.jar`
- `quo-vadis-core-iosarm64-0.1.0-SNAPSHOT.klib`
- `quo-vadis-core-iossimulatorarm64-0.1.0-SNAPSHOT.klib`
- `quo-vadis-core-iosx64-0.1.0-SNAPSHOT.klib`
- `quo-vadis-core-js-0.1.0-SNAPSHOT.klib`
- `quo-vadis-core-wasm-js-0.1.0-SNAPSHOT.klib`

## Testing Commands

### Run All Tests
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="test")
```
**Terminal:**
```bash
./gradlew test
```

### Run Android Tests
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:testDebugUnitTest")
mcp_gradle-mcp_run_task(task=":quo-vadis-core:testDebugUnitTest")
```
**Terminal:**
```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :quo-vadis-core:testDebugUnitTest
```

### Run Android Instrumented Tests
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":quo-vadis-core:connectedAndroidTest")
```
**Terminal:**
```bash
./gradlew :quo-vadis-core:connectedAndroidTest
```

### Run Web Tests
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:jsTest")              # JavaScript tests
mcp_gradle-mcp_run_task(task=":quo-vadis-core:jsTest")          # Library JS tests
```
**Terminal:**
```bash
./gradlew :composeApp:jsTest              # JavaScript tests
./gradlew :quo-vadis-core:jsTest          # Library JS tests
```

## Cleaning Commands

### Clean Build
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_clean()
```
**Terminal:**
```bash
./gradlew clean
```

### Clean and Build
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_clean()
mcp_gradle-mcp_run_task(task="build")
```
**Terminal:**
```bash
./gradlew clean build
```

### Clean Configuration Cache (if issues arise)
**Terminal (Non-Gradle):**
```bash
./gradlew --stop
rm -rf .gradle/configuration-cache
./gradlew clean
```

### Full Clean (including dependencies)
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_clean()
mcp_gradle-mcp_run_task(task="build", args=["--refresh-dependencies"])
```
**Terminal:**
```bash
./gradlew clean --refresh-dependencies
```

## Dependency Commands

### Show Dependencies
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:dependencies")
mcp_gradle-mcp_run_task(task=":quo-vadis-core:dependencies")
```
**Terminal:**
```bash
./gradlew :composeApp:dependencies
./gradlew :quo-vadis-core:dependencies
```

### Show Desktop Runtime Dependencies
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:dependencies", args=["--configuration", "desktopRuntimeClasspath"])
```
**Terminal:**
```bash
./gradlew :composeApp:dependencies --configuration desktopRuntimeClasspath
```

### Check for Dependency Updates
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="dependencyUpdates")
```
**Terminal:**
```bash
./gradlew dependencyUpdates
```

## Verification Commands

### Lint Check (Android)
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="lint")
```
**Terminal:**
```bash
./gradlew lint
```

### Lint Report Location
After running lint, check: `composeApp/build/reports/lint-results.html`

### Check Errors
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:compileKotlinAndroid")
mcp_gradle-mcp_run_task(task=":composeApp:compileKotlinDesktop")
mcp_gradle-mcp_run_task(task=":composeApp:compileKotlinJs")
```
Use IDE tools or:
**Terminal:**
```bash
./gradlew :composeApp:compileKotlinAndroid
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileKotlinJs
```

## Gradle Commands

### List All Tasks
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_list_project_tasks()        # Root project tasks
mcp_gradle-mcp_list_project_tasks(project=":composeApp")
mcp_gradle-mcp_list_project_tasks(project=":quo-vadis-core")
```
**Terminal:**
```bash
./gradlew tasks
```

### List All Projects
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_list_projects()
```
**Terminal:**
```bash
./gradlew projects
```

### Show Project Info
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:properties")
mcp_gradle-mcp_run_task(task=":quo-vadis-core:properties")
```
**Terminal:**
```bash
./gradlew :composeApp:properties
./gradlew :quo-vadis-core:properties
```

### Build Scan
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="build", args=["--scan"])
```
**Terminal:**
```bash
./gradlew build --scan
```

## Daemon Management

### Stop Gradle Daemon
**Terminal (Non-Gradle):**
```bash
./gradlew --stop
```

### Gradle Daemon Status
**Terminal (Non-Gradle):**
```bash
./gradlew --status
```

## Running Applications

### Android (via Gradle)
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:installDebug")
```
**Terminal:**
```bash
./gradlew :composeApp:installDebug
adb shell am start -n com.jermey.navplayground/.MainActivity
```

### iOS (via Xcode)
**Terminal (Non-Gradle):**
1. Open project: `open iosApp/iosApp.xcodeproj`
2. Select simulator/device
3. Press Run (⌘R)

### Web (Development Server)
**MCP (PREFERRED):**
```python
# JavaScript with hot reload
mcp_gradle-mcp_run_task(task=":composeApp:jsBrowserDevelopmentRun", args=["--continuous"])

# WebAssembly with hot reload
mcp_gradle-mcp_run_task(task=":composeApp:wasmJsBrowserDevelopmentRun", args=["--continuous"])
```
**Terminal:**
```bash
# JavaScript with hot reload
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous

# WebAssembly with hot reload
./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous
```
Both open at `http://localhost:8080`

### Web (Production - Static Server)
**Terminal (Non-Gradle):**
```bash
# After building production bundle
cd composeApp/build/dist/js/productionExecutable
python3 -m http.server 8000
# Open http://localhost:8000
```

### Desktop
**MCP (PREFERRED):**
```python
# Direct run (fastest for development)
mcp_gradle-mcp_run_task(task=":composeApp:run", args=["--no-configuration-cache"])

# Or create distributable
mcp_gradle-mcp_run_task(task=":composeApp:createDistributable")
```
**Terminal:**
```bash
# Direct run (fastest for development)
./gradlew :composeApp:run --no-configuration-cache

# Or run distributable
./gradlew :composeApp:createDistributable
./composeApp/build/compose/binaries/main/app/NavPlayground/NavPlayground
```

## Performance Analysis

### Profile Build
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="assembleDebug", args=["--profile"])
```
**Terminal:**
```bash
./gradlew assembleDebug --profile
# Report in: build/reports/profile/
```

### Build with Stack Traces
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="build", args=["--stacktrace"])          # Abbreviated
mcp_gradle-mcp_run_task(task="build", args=["--full-stacktrace"])     # Full traces
```
**Terminal:**
```bash
./gradlew build --stacktrace          # Abbreviated
./gradlew build --full-stacktrace     # Full traces
```

## Useful Gradle Options

### Parallel Builds
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="build", args=["--parallel"])
```
**Terminal:**
```bash
./gradlew build --parallel
```

### Offline Mode
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="build", args=["--offline"])
```
**Terminal:**
```bash
./gradlew build --offline
```

### Refresh Dependencies
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="build", args=["--refresh-dependencies"])
```
**Terminal:**
```bash
./gradlew build --refresh-dependencies
```

### No Build Cache
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_clean()
mcp_gradle-mcp_run_task(task="build", args=["--no-build-cache"])
```
**Terminal:**
```bash
./gradlew clean build --no-build-cache
```

### No Configuration Cache
**MCP (PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="build", args=["--no-configuration-cache"])
```
**Terminal:**
```bash
./gradlew build --no-configuration-cache
```

## Quick Reference

### Most Common Commands

**Development (MCP PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":composeApp:installDebug")                      # Android
mcp_gradle-mcp_run_task(task=":composeApp:jsBrowserDevelopmentRun")           # Web JS
mcp_gradle-mcp_run_task(task=":composeApp:run")                               # Desktop
```

**Development (Terminal):**
```bash
./gradlew :composeApp:installDebug                      # Android
./gradlew :composeApp:jsBrowserDevelopmentRun           # Web JS
./gradlew :composeApp:run                               # Desktop
open iosApp/iosApp.xcodeproj                            # iOS
```

**Testing (MCP PREFERRED):**
```python
mcp_gradle-mcp_run_task(task="test")                                          # All tests
mcp_gradle-mcp_clean()                                                         # Clean
mcp_gradle-mcp_run_task(task="build")                                         # Build
```

**Testing (Terminal):**
```bash
./gradlew test                                          # All tests
./gradlew clean build                                   # Clean build
```

**Publishing (MCP PREFERRED):**
```python
mcp_gradle-mcp_run_task(task=":quo-vadis-core:publishToMavenLocal")           # Publish library
```

**Publishing (Terminal):**
```bash
./gradlew :quo-vadis-core:publishToMavenLocal           # Publish library
```

## Notes

- **PREFER Gradle MCP tools** (`mcp_gradle-mcp_run_task`, `mcp_gradle-mcp_clean`) over terminal commands
- Terminal commands shown for reference only (use for non-Gradle operations like file ops, git, etc.)
- All commands assume you're in the project root directory
- Use `./gradlew` on macOS/Linux, `gradlew.bat` on Windows (when using terminal)
- Configuration cache is enabled by default (improves build performance)
- Build cache is enabled by default (reuses outputs across builds)
- Gradle daemon uses 6GB heap, builds use 8GB heap
- For desktop, use `:composeApp:run` task (not `desktopRun`)
- For web, dev server runs on port 8080 by default
- Desktop JAR task creates library JAR, not executable
