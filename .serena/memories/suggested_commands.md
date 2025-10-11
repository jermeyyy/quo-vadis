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

#### Build iOS Framework
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

#### Build for Physical Device
```bash
./gradlew :composeApp:linkDebugFrameworkIosArm64
```

#### Open iOS Project in Xcode
```bash
open iosApp/iosApp.xcodeproj
```

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

## Dependency Commands

### Show Dependencies
```bash
./gradlew :composeApp:dependencies
./gradlew :quo-vadis-core:dependencies
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

## Git Commands (macOS/Darwin specific)

### Standard Git Operations
```bash
git status
git add .
git commit -m "message"
git push
git pull
```

### View Git Log
```bash
git log --oneline --graph --decorate
```

### Check Git Diff
```bash
git diff
```

## File System Commands (macOS/Darwin)

### List Files
```bash
ls -la                    # List all with details
ls -lh                    # List with human-readable sizes
```

### Find Files
```bash
find . -name "*.kt"       # Find all Kotlin files
find . -name "*.swift"    # Find all Swift files
find . -type d -name build # Find all build directories
```

### Search in Files (grep)
```bash
grep -r "Navigator" composeApp/src/
grep -r "Destination" quo-vadis-core/src/
```

### Tree View (if tree is installed)
```bash
tree -L 3                 # Show 3 levels
tree -I 'build|.gradle'   # Exclude build and .gradle
```

### Disk Usage
```bash
du -sh build/             # Size of build directory
du -sh .gradle/           # Size of gradle cache
```

## IDE Commands

### Generate IDE Metadata
```bash
./gradlew idea           # For IntelliJ IDEA (rarely needed)
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

## Running the Application

### Android (via Gradle)
```bash
./gradlew :composeApp:installDebug
adb shell am start -n com.jermey.navplayground/.MainActivity
```

### iOS (via Xcode)
1. Open project: `open iosApp/iosApp.xcodeproj`
2. Select simulator/device
3. Press Run (⌘R)

## Documentation Generation

### Generate KDoc (if configured)
```bash
./gradlew dokkaHtml
```

## Performance Analysis

### Profile Build
```bash
./gradlew assembleDebug --profile
# Report in: build/reports/profile/
```

### Build with Stack Traces
```bash
./gradlew build --stacktrace    # Abbreviated
./gradlew build --full-stacktrace # Full traces
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

## Notes

- All commands assume you're in the project root directory
- Use `./gradlew` on macOS/Linux, `gradlew.bat` on Windows
- Configuration cache is enabled by default (improves build performance)
- Build cache is enabled by default (reuses outputs across builds)
