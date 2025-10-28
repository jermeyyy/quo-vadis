# Maven Central Release v0.1.0

## Release Information
- **Version**: 0.1.0
- **Group ID**: io.github.jermeyyy
- **Artifact IDs**: quo-vadis-core, quo-vadis-annotations, quo-vadis-ksp
- **Release Date**: January 2025
- **Maven Central URL**: https://central.sonatype.com/artifact/io.github.jermeyyy/quo-vadis-core

## Published Artifacts
1. quo-vadis-core (7 platform variants)
   - Android AAR
   - iOS x64, Arm64, Simulator Arm64
   - Desktop JAR
   - JavaScript
   - WebAssembly
2. quo-vadis-annotations (6 platform variants)
3. quo-vadis-ksp (JVM only)

## Installation from Maven Central

### Gradle Kotlin DSL
```kotlin
repositories {
    mavenCentral()
    google()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.github.jermeyyy:quo-vadis-core:0.1.0")
                implementation("io.github.jermeyyy:quo-vadis-annotations:0.1.0")
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "io.github.jermeyyy:quo-vadis-ksp:0.1.0")
}
```

### Version Catalog
```toml
[versions]
quoVadis = "0.1.0"
ksp = "2.2.20-1.0.29"

[libraries]
quo-vadis-core = { module = "io.github.jermeyyy:quo-vadis-core", version.ref = "quoVadis" }
quo-vadis-annotations = { module = "io.github.jermeyyy:quo-vadis-annotations", version.ref = "quoVadis" }
quo-vadis-ksp = { module = "io.github.jermeyyy:quo-vadis-ksp", version.ref = "quoVadis" }
```

## Maven Local (Development)
For local development, SNAPSHOT versions can still be published to Maven Local:
```bash
./publish-local.sh
```
Location: `~/.m2/repository/io/github/jermeyyy/quo-vadis-core/0.1.0-SNAPSHOT/`

## Publishing Process
- Uses Vanniktech Maven Publish plugin
- Automated via Gradle task: `publishAndReleaseToMavenCentral`
- Requires GPG signing and Sonatype credentials
- Sync time: 15-30 minutes to Maven Central

## Documentation Updates Completed

### Documentation Files
✅ README.md - Added Maven Central badges, updated coordinates
✅ PUBLISHING.md - Added Maven Central publishing guide, restructured for releases + local dev
✅ quo-vadis-core/docs/ANNOTATION_API.md - Updated installation with Maven Central coordinates

### Website Updates
✅ Navbar - Added v0.1.0 version badge with link to Maven Central
✅ GettingStarted page - Added Maven Central release note, updated installation code
✅ Home page - Added Maven Central badge, updated version catalog code

### Version Display
- Version badge in navbar: Links to Maven Central artifact page
- Displays "v0.1.0" with responsive design (hides label on mobile)
- Color: Gradient from primary to primary-dark
- Style: Rounded badge with hover animation

### Coordinates Changed
- OLD: `com.jermey.quo.vadis:quo-vadis-*:0.1.0-SNAPSHOT`
- NEW: `io.github.jermeyyy:quo-vadis-*:0.1.0`

### Repository Changed
- OLD: `mavenLocal()` for all usage
- NEW: `mavenCentral()` for stable releases, `mavenLocal()` for development only