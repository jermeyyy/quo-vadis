# Maven Local Publishing Configuration - Summary

## Date: October 15, 2025
## Status: ✅ Complete

---

## What Was Configured

### 1. Build Configuration (`quo-vadis-core/build.gradle.kts`)

**Added:**
- `maven-publish` plugin
- Group ID: `com.jermey.quo.vadis`
- Version: `0.1.0-SNAPSHOT`
- Complete Maven publishing block with:
  - POM metadata (name, description, license, developers, SCM)
  - All KMP target publications (Android, iOS x64, iOS Arm64, iOS Simulator Arm64)
  - Maven Local repository configuration

### 2. Helper Script (`publish-local.sh`)

**Created executable script that:**
- Stops Gradle daemon
- Cleans previous builds
- Publishes all artifacts
- Shows success message with usage instructions
- Handles errors gracefully

**Usage:**
```bash
./publish-local.sh
```

### 3. Documentation

**Created:**
- `PUBLISHING.md` - Comprehensive publishing guide (full documentation)
- `PUBLISHING_QUICKREF.md` - Quick reference card

---

## Published Artifacts

When you run the publish command, the following artifacts will be created:

```
~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/0.1.0-SNAPSHOT/
├── quo-vadis-core-0.1.0-SNAPSHOT.module          # Gradle metadata
├── quo-vadis-core-0.1.0-SNAPSHOT.pom             # Maven POM
├── quo-vadis-core-android-0.1.0-SNAPSHOT.aar     # Android library
├── quo-vadis-core-iosx64-0.1.0-SNAPSHOT.klib     # iOS x64
├── quo-vadis-core-iosarm64-0.1.0-SNAPSHOT.klib   # iOS Arm64 (device)
└── quo-vadis-core-iossimulatorarm64-0.1.0-SNAPSHOT.klib  # iOS Simulator
```

---

## How to Publish

### Quick Method (Recommended)
```bash
./publish-local.sh
```

### Manual Method
```bash
./gradlew :quo-vadis-core:publishToMavenLocal
```

### With Clean Build
```bash
./gradlew :quo-vadis-core:clean :quo-vadis-core:publishToMavenLocal
```

---

## How to Use in Another Project

### Add to `build.gradle.kts`:

```kotlin
repositories {
    mavenLocal()  // Add this to use local artifacts
    mavenCentral()
    google()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.jermey.quo.vadis:quo-vadis-core:0.1.0-SNAPSHOT")
            }
        }
    }
}
```

---

## Library Coordinates

```
Group:    com.jermey.quo.vadis
Artifact: quo-vadis-core
Version:  0.1.0-SNAPSHOT
```

**Full Dependency:**
```kotlin
implementation("com.jermey.quo.vadis:quo-vadis-core:0.1.0-SNAPSHOT")
```

---

## Version Management

To change the version, edit `quo-vadis-core/build.gradle.kts`:

```kotlin
group = "com.jermey.quo.vadis"
version = "0.1.0-SNAPSHOT"  // ← Change this
```

**Recommended versioning:**
- Development: `X.Y.Z-SNAPSHOT` (e.g., `0.1.0-SNAPSHOT`)
- Alpha: `X.Y.Z-alpha.N` (e.g., `0.1.0-alpha.1`)
- Beta: `X.Y.Z-beta.N` (e.g., `0.1.0-beta.1`)
- Release: `X.Y.Z` (e.g., `0.1.0`, `1.0.0`)

---

## Gradle Tasks Available

```bash
# Publish all targets to Maven Local
./gradlew :quo-vadis-core:publishToMavenLocal

# List all publishing tasks
./gradlew :quo-vadis-core:tasks --group publishing

# Publish specific target
./gradlew :quo-vadis-core:publishAndroidReleasePublicationToMavenLocal
./gradlew :quo-vadis-core:publishIosX64PublicationToMavenLocal
./gradlew :quo-vadis-core:publishIosArm64PublicationToMavenLocal
./gradlew :quo-vadis-core:publishIosSimulatorArm64PublicationToMavenLocal
./gradlew :quo-vadis-core:publishKotlinMultiplatformPublicationToMavenLocal
```

---

## Troubleshooting

### Clear Maven Local Cache
```bash
rm -rf ~/.m2/repository/com/jermey/quo/vadis/
```

### Verify Published Files
```bash
ls -la ~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/0.1.0-SNAPSHOT/
```

### Memory Issues
If you encounter OutOfMemoryError during iOS framework linking:

1. **Already configured** in `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx8192M -XX:MaxMetaspaceSize=2048M
   kotlin.daemon.jvmargs=-Xmx6144M -XX:MaxMetaspaceSize=1024M
   ```

2. **Publish targets separately**:
   ```bash
   ./gradlew :quo-vadis-core:publishAndroidReleasePublicationToMavenLocal
   ./gradlew :quo-vadis-core:publishIosX64PublicationToMavenLocal
   # etc.
   ```

---

## Files Created/Modified

### Modified:
1. **`quo-vadis-core/build.gradle.kts`**
   - Added `maven-publish` plugin
   - Added `group` and `version` properties
   - Added complete `publishing` configuration block

### Created:
2. **`publish-local.sh`** - Executable helper script
3. **`PUBLISHING.md`** - Full documentation
4. **`PUBLISHING_QUICKREF.md`** - Quick reference
5. **`MAVEN_LOCAL_PUBLISHING_SUMMARY.md`** - This file

---

## POM Metadata Configured

- **Name:** Quo Vadis - Navigation Library
- **Description:** Comprehensive type-safe navigation library for Compose Multiplatform
- **URL:** https://github.com/jermeyyy/quo-vadis
- **License:** Apache License 2.0
- **Developer:** jermeyyy
- **SCM:** GitHub repository URLs

This metadata is automatically included in the generated POM files.

---

## Next Steps

1. **Test the publishing:**
   ```bash
   ./publish-local.sh
   ```

2. **Verify artifacts:**
   ```bash
   ls -la ~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/0.1.0-SNAPSHOT/
   ```

3. **Use in another project:**
   - Add `mavenLocal()` to repositories
   - Add dependency: `implementation("com.jermey.quo.vadis:quo-vadis-core:0.1.0-SNAPSHOT")`

4. **For public release later:**
   - See `PUBLISHING.md` for Maven Central and GitHub Packages configuration

---

## Summary

✅ **Maven Local publishing fully configured**  
✅ **All KMP targets supported** (Android + 3 iOS variants)  
✅ **Helper script created** for easy publishing  
✅ **Comprehensive documentation** provided  
✅ **POM metadata** properly configured  
✅ **Version management** ready (0.1.0-SNAPSHOT)  
✅ **Memory settings** optimized for iOS builds  

**Ready to publish!** Run `./publish-local.sh` to publish the library to your local Maven repository.
