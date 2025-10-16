# Suggested Commands for NavPlayground

## Build Commands

### Android

#### Build Debug APK
```bash
./gradlew :composeApp:assembleDebug
```

#### Build Release APK
```bash
./gradlew :composeApp:assembleRelease
```

#### Install Debug on Connected Device
```bash
./gradlew :composeApp:installDebug
```

#### Build Library Module
```bash
./gradlew :quo-vadis-core:build
```

### iOS

#### Build iOS Framework (M1/M2 Macs)
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

#### Build iOS Framework (Intel Macs)
```bash
./gradlew :composeApp:linkDebugFrameworkIosX64
```

#### Build for Physical Device
```bash
./gradlew :composeApp:linkDebugFrameworkIosArm64
```

#### Open iOS Project in Xcode
```bash
open iosApp/iosApp.xcodeproj
```

### Web Targets

#### JavaScript (Development with Hot Reload)
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous
```
Opens at `http://localhost:8080` with automatic reload on code changes.

#### WebAssembly (Development with Hot Reload)
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous
```
Opens at `http://localhost:8080` with automatic reload on code changes.

#### JavaScript (Production Build)
```bash
./gradlew :composeApp:jsBrowserDistribution
```
Output: `composeApp/build/dist/js/productionExecutable/`

#### WebAssembly (Production Build)
```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```
Output: `composeApp/build/dist/wasmJs/productionExecutable/`

#### Build Web Libraries
```bash
./gradlew :quo-vadis-core:jsJar          # JavaScript library
./gradlew :quo-vadis-core:wasmJsJar      # WebAssembly library
```

### Desktop (JVM)

#### Run Desktop App
```bash
./gradlew :composeApp:run --no-configuration-cache
```
Launches native window application.

#### Create Distributable Bundle
```bash
./gradlew :composeApp:createDistributable
```
Output: `composeApp/build/compose/binaries/main/app/NavPlayground`

#### Create Native Installer (Current OS)
```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

#### Platform-Specific Installers
```bash
./gradlew :composeApp:packageDmg        # macOS DMG (macOS only)
./gradlew :composeApp:packageMsi        # Windows MSI (Windows only)
./gradlew :composeApp:packageDeb        # Linux DEB (Linux only)
```

#### Build Desktop Library
```bash
./gradlew :quo-vadis-core:desktopJar
```
Output: `quo-vadis-core/build/libs/quo-vadis-core-desktop.jar`

**Note**: The `desktopJar` task creates a library JAR (not executable). Use `:composeApp:run` to run the app.

## Publishing Commands

### Publish to Maven Local
```bash
./gradlew :quo-vadis-core:publishToMavenLocal
```
Publishes all platform artifacts to `~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/0.1.0-SNAPSHOT/`

### Verify Published Artifacts
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
```bash
./gradlew test
```

### Run Android Tests
```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :quo-vadis-core:testDebugUnitTest
```

### Run Android Instrumented Tests
```bash
./gradlew :quo-vadis-core:connectedAndroidTest
```

### Run Web Tests
```bash
./gradlew :composeApp:jsTest              # JavaScript tests
./gradlew :quo-vadis-core:jsTest          # Library JS tests
```

## Cleaning Commands

### Clean Build
```bash
./gradlew clean
```

### Clean and Build
```bash
./gradlew clean build
```

### Clean Configuration Cache (if issues arise)
```bash
./gradlew --stop
rm -rf .gradle/configuration-cache
./gradlew clean
```

### Full Clean (including dependencies)
```bash
./gradlew clean --refresh-dependencies
```

## Dependency Commands

### Show Dependencies
```bash
./gradlew :composeApp:dependencies
./gradlew :quo-vadis-core:dependencies
```

### Show Desktop Runtime Dependencies
```bash
./gradlew :composeApp:dependencies --configuration desktopRuntimeClasspath
```

### Check for Dependency Updates
```bash
./gradlew dependencyUpdates
```

## Verification Commands

### Lint Check (Android)
```bash
./gradlew lint
```

### Lint Report Location
After running lint, check: `composeApp/build/reports/lint-results.html`

### Check Errors
Use IDE tools or:
```bash
./gradlew :composeApp:compileKotlinAndroid
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileKotlinJs
```

## Gradle Commands

### List All Tasks
```bash
./gradlew tasks
```

### List All Projects
```bash
./gradlew projects
```

### Show Project Info
```bash
./gradlew :composeApp:properties
./gradlew :quo-vadis-core:properties
```

### Build Scan
```bash
./gradlew build --scan
```

## Daemon Management

### Stop Gradle Daemon
```bash
./gradlew --stop
```

### Gradle Daemon Status
```bash
./gradlew --status
```

## Running Applications

### Android (via Gradle)
```bash
./gradlew :composeApp:installDebug
adb shell am start -n com.jermey.navplayground/.MainActivity
```

### iOS (via Xcode)
1. Open project: `open iosApp/iosApp.xcodeproj`
2. Select simulator/device
3. Press Run (⌘R)

### Web (Development Server)
```bash
# JavaScript with hot reload
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous

# WebAssembly with hot reload
./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous
```
Both open at `http://localhost:8080`

### Web (Production - Static Server)
```bash
# After building production bundle
cd composeApp/build/dist/js/productionExecutable
python3 -m http.server 8000
# Open http://localhost:8000
```

### Desktop
```bash
# Direct run (fastest for development)
./gradlew :composeApp:run --no-configuration-cache

# Or run distributable
./gradlew :composeApp:createDistributable
./composeApp/build/compose/binaries/main/app/NavPlayground/NavPlayground
```

## Performance Analysis

### Profile Build
```bash
./gradlew assembleDebug --profile
# Report in: build/reports/profile/
```

### Build with Stack Traces
```bash
./gradlew build --stacktrace          # Abbreviated
./gradlew build --full-stacktrace     # Full traces
```

## Useful Gradle Options

### Parallel Builds
```bash
./gradlew build --parallel
```

### Offline Mode
```bash
./gradlew build --offline
```

### Refresh Dependencies
```bash
./gradlew build --refresh-dependencies
```

### No Build Cache
```bash
./gradlew clean build --no-build-cache
```

### No Configuration Cache
```bash
./gradlew build --no-configuration-cache
```

## Quick Reference

### Most Common Commands

**Development:**
```bash
./gradlew :composeApp:installDebug                      # Android
./gradlew :composeApp:jsBrowserDevelopmentRun           # Web JS
./gradlew :composeApp:run                               # Desktop
open iosApp/iosApp.xcodeproj                            # iOS
```

**Testing:**
```bash
./gradlew test                                          # All tests
./gradlew clean build                                   # Clean build
```

**Publishing:**
```bash
./gradlew :quo-vadis-core:publishToMavenLocal           # Publish library
```

## Notes

- All commands assume you're in the project root directory
- Use `./gradlew` on macOS/Linux, `gradlew.bat` on Windows
- Configuration cache is enabled by default (improves build performance)
- Build cache is enabled by default (reuses outputs across builds)
- Gradle daemon uses 6GB heap, builds use 8GB heap
- For desktop, use `:composeApp:run` task (not `desktopRun`)
- For web, dev server runs on port 8080 by default
- Desktop JAR task creates library JAR, not executable
