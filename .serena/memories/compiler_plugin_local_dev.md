# Critical Discovery: Compiler Plugin Local Development

## The Problem
The compiler plugin source changes in `quo-vadis-compiler-plugin/src/` are NOT picked up by `composeApp` compilation!

### Why
- `QuoVadisCompilerSubplugin.getPluginArtifact()` resolves the compiler plugin as a Maven artifact:
  ```kotlin
  SubpluginArtifact(
      groupId = "io.github.jermeyyy",
      artifactId = "quo-vadis-compiler-plugin", 
      version = BuildConfig.VERSION
  )
  ```
- Even though `include(":quo-vadis-compiler-plugin")` is in settings.gradle.kts, the Kotlin compiler plugin system resolves via Maven coordinates
- Changes to local source require publishing to mavenLocal first via `publish-local.sh`

### How to Fix (for development)
Must run `./publish-local.sh` (or equivalent) to install the modified compiler plugin to mavenLocal before testing with composeApp.

OR: Modify the Gradle plugin to use project dependency resolution for the compiler plugin artifact.

Check `publish-local.sh` for the exact command.