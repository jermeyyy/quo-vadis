# Implementation Plan: Drop Web Targets (JS & WASM)

## Overview

Remove JavaScript (IR) and WebAssembly (wasmJs) Kotlin Multiplatform targets from **all modules** in the Quo Vadis navigation library. This is a full cleanup covering build configs, source sets, dependencies, scripts, documentation, and tooling artifacts.

**Motivation:** Web targets are still beta in Kotlin Multiplatform, cause build issues, and are not needed for the library's primary use cases (Android, iOS, Desktop).

## Requirements

- Remove `js(IR)` and `wasmJs()` target declarations from all modules
- Delete all `jsMain`, `jsTest`, `wasmJsMain`, `wasmJsTest` source set directories
- Remove web-specific dependencies from version catalog and build files
- Delete web-only scripts and tooling files (build-web.sh, package.json, kotlin-js-store/)
- Update all documentation to remove web references
- Remove web build output directories
- Update Serena memory files
- Verify the project still compiles after all changes

## Remaining Platform Targets

After this change, the library supports:
- **Android**
- **iOS** (arm64, simulatorArm64, x64)
- **Desktop** (JVM)

---

## Phase 1: Remove Web Target Declarations from Build Configs

### Task 1.1: quo-vadis-core/build.gradle.kts

- **Description:** Remove `js(IR) { ... }` block (~L52–L58), `@OptIn(ExperimentalWasmDsl::class)` (~L61), `wasmJs { ... }` block (~L62–L69), `jsMain` source set block (~L119–L121), `wasmJsMain` source set block (~L124–L126). Remove unused `ExperimentalWasmDsl` import if present.
- **File:** `quo-vadis-core/build.gradle.kts`
- **Acceptance Criteria:** No `js`, `wasmJs`, or `ExperimentalWasmDsl` references remain in the file.

### Task 1.2: quo-vadis-annotations/build.gradle.kts

- **Description:** Remove `js(IR) { ... }` block (~L22–L24), `@OptIn(ExperimentalWasmDsl::class)` (~L26), `wasmJs { ... }` block (~L27–L29). Remove unused `ExperimentalWasmDsl` import if present.
- **File:** `quo-vadis-annotations/build.gradle.kts`
- **Acceptance Criteria:** No `js`, `wasmJs`, or `ExperimentalWasmDsl` references remain.

### Task 1.3: quo-vadis-core-flow-mvi/build.gradle.kts

- **Description:** Remove `js(IR) { ... }` block (~L49–L55), `@OptIn(ExperimentalWasmDsl::class)` (~L58), `wasmJs { ... }` block (~L59–L66), `jsMain` source set block (~L128–L130), `wasmJsMain` source set block (~L133–L135). Remove unused `ExperimentalWasmDsl` import if present.
- **File:** `quo-vadis-core-flow-mvi/build.gradle.kts`
- **Acceptance Criteria:** No `js`, `wasmJs`, or `ExperimentalWasmDsl` references remain.

### Task 1.4: composeApp/build.gradle.kts

- **Description:** Remove `js(IR) { ... }` block (~L49–L55), `@OptIn(ExperimentalWasmDsl::class)` (~L58), `wasmJs { ... }` block (~L59–L66), `jsMain` source set block (~L119–L123, includes `compose.html.core`, `compose.materialIconsExtended`, `libs.ktor.client.js` deps), `wasmJsMain` source set block (~L124–L127). Remove unused `ExperimentalWasmDsl` import if present.
- **File:** `composeApp/build.gradle.kts`
- **Acceptance Criteria:** No `js`, `wasmJs`, `ExperimentalWasmDsl`, `compose.html.core`, or `ktor.client.js` references remain.

### Task 1.5: feature1/build.gradle.kts

- **Description:** Remove `js(IR) { ... }` block (~L56–L62), `@OptIn(ExperimentalWasmDsl::class)` (~L65), `wasmJs { ... }` block (~L66–L73), `jsMain` source set block (~L130–L133, includes `compose.html.core`, `compose.materialIconsExtended` deps), `wasmJsMain` source set block (~L134–L136). Remove unused `ExperimentalWasmDsl` import if present.
- **File:** `feature1/build.gradle.kts`
- **Acceptance Criteria:** No web target references remain.

### Task 1.6: feature2/build.gradle.kts

- **Description:** Remove `js(IR) { ... }` block (~L54–L60), `@OptIn(ExperimentalWasmDsl::class)` (~L63), `wasmJs { ... }` block (~L64–L71), `jsMain` source set block (~L126–L129, includes `compose.html.core`, `compose.materialIconsExtended` deps), `wasmJsMain` source set block (~L130–L132). Remove unused `ExperimentalWasmDsl` import if present.
- **File:** `feature2/build.gradle.kts`
- **Acceptance Criteria:** No web target references remain.

### Task 1.7: navigation-api/build.gradle.kts

- **Description:** Remove `js(IR) { ... }` block (~L28–L30), `@OptIn(ExperimentalWasmDsl::class)` (~L32), `wasmJs { ... }` block (~L33–L35). Remove unused `ExperimentalWasmDsl` import if present.
- **File:** `navigation-api/build.gradle.kts`
- **Acceptance Criteria:** No web target references remain.

---

## Phase 2: Delete Web Source Sets

### Task 2.1: Delete composeApp web source sets

- **Description:** Delete directories:
  - `composeApp/src/jsMain/` (4 files: `resources/index.html`, `main.js.kt`, `Theme.js.kt`, `CoilSetup.js.kt`)
  - `composeApp/src/wasmJsMain/` (4 files: `resources/index.html`, `main.wasmJs.kt`, `Theme.wasmJs.kt`, `CoilSetup.wasmJs.kt`)
- **Command:** `rm -rf composeApp/src/jsMain composeApp/src/wasmJsMain`
- **Acceptance Criteria:** Both directories and all contents are gone.

### Task 2.2: Delete quo-vadis-core web source sets

- **Description:** Delete directories:
  - `quo-vadis-core/src/jsMain/` (2 files: `WindowSizeClass.js.kt`, `PlatformBackInput.js.kt`)
  - `quo-vadis-core/src/wasmJsMain/` (2 files: `WindowSizeClass.wasmJs.kt`, `PlatformBackInput.wasmJs.kt`)
- **Important:** Verify that `expect` declarations in `commonMain` for `WindowSizeClass` and `PlatformBackInput` still have `actual` implementations on all remaining targets (Android, iOS, Desktop). If these `expect` declarations only had `actual` on web + one other platform, this could break compilation.
- **Command:** `rm -rf quo-vadis-core/src/jsMain quo-vadis-core/src/wasmJsMain`
- **Acceptance Criteria:** Both directories gone. No missing `actual` declarations for remaining targets.

---

## Phase 3: Remove Web-Specific Dependencies

### Task 3.1: Clean up version catalog

- **Description:** Remove `ktor-client-js` entry from `gradle/libs.versions.toml` (~L88). This dependency is only used in `composeApp/jsMain` and `wasmJsMain`.
- **File:** `gradle/libs.versions.toml`
- **Acceptance Criteria:** No `ktor-client-js` entry in the catalog.

---

## Phase 4: Delete Web Scripts and Tooling Files

### Task 4.1: Delete build-web.sh

- **Description:** Delete `build-web.sh` (entire file is a web build script).
- **Command:** `rm build-web.sh`
- **Acceptance Criteria:** File no longer exists.

### Task 4.2: Delete root package.json

- **Description:** Delete root `package.json` (empty `{}`, exists only for JS/WASM yarn/npm tooling).
- **Command:** `rm package.json`
- **Acceptance Criteria:** File no longer exists.

### Task 4.3: Delete kotlin-js-store directory

- **Description:** Delete `kotlin-js-store/` directory (yarn.lock files for JS/WASM dependency management).
- **Command:** `rm -rf kotlin-js-store`
- **Acceptance Criteria:** Directory no longer exists.

### Task 4.4: Delete web build output directories

- **Description:** Delete `build/js/` and `build/wasm/` directories (build artifacts, node_modules, etc.). These should also be in `.gitignore` but may have been tracked.
- **Command:** `rm -rf build/js build/wasm`
- **Acceptance Criteria:** Directories no longer exist.

---

## Phase 5: Update Documentation

### Task 5.1: Update README.md

- **Description:** Remove/update all web references:
  - ~L36: Remove "Web (JS & WASM)" from multiplatform feature bullet
  - ~L580–L581: Remove JavaScript/WebAssembly version info lines
  - ~L656–L657: Remove JS/WASM rows from Platform Support table
  - ~L682–L686: Remove "Web (JavaScript)" and "Web (WebAssembly)" run commands
  - ~L749–L750: Remove `jsBrowserDevelopmentRun` and `wasmJsBrowserDevelopmentRun` commands
- **File:** `README.md`
- **Acceptance Criteria:** No references to JS/WASM targets remain. Platform list shows Android, iOS, Desktop only.

### Task 5.2: Update PUBLISHING.md

- **Description:** Remove JavaScript and WebAssembly artifact lines (~L137–L138). Update platform count (e.g., "7 platforms" → "5 platforms" or similar).
- **File:** `PUBLISHING.md`
- **Acceptance Criteria:** No web artifact references. Platform count is accurate.

### Task 5.3: Update CONTRIBUTING.md

- **Description:** Remove "Test web targets when modifying Compose UI" (~L170) and any other web testing references.
- **File:** `CONTRIBUTING.md`
- **Acceptance Criteria:** No web testing instructions remain.

### Task 5.4: Update .github/copilot-instructions.md

- **Description:**
  - Remove JS/WASM build commands (~L119–L120): `jsBrowserDevelopmentRun` and `wasmJsBrowserDevelopmentRun`
  - Update Platform Targets line (~L182): Remove "JavaScript (IR), WebAssembly (wasmJs)"
- **File:** `.github/copilot-instructions.md`
- **Acceptance Criteria:** Platform list shows Android, iOS, Desktop only. No web build commands.

---

## Phase 6: Update Serena Memory Files

### Task 6.1: Update Serena memory files

- **Description:** Update web references in:
  - `.serena/memories/codebase_structure.md` (~L66, L126–127)
  - `.serena/memories/project_overview.md` (~L64, L81–82)
  - `.serena/memories/suggested_commands.md` (~L94, L99)
  - `.serena/memories/maven_central_release.md` (~L15–16)
  - `.serena/memories/task_completion_checklist.md` (~L74)
  - `.serena/memories/tech_stack.md` (~L66–67)
- **Acceptance Criteria:** No web target references in Serena memory files.

---

## Phase 7: Verify Build

### Task 7.1: Run full build

- **Description:** Run `./gradlew build` to verify all remaining targets compile successfully after web target removal.
- **Command:** `./gradlew build`
- **Acceptance Criteria:** Build succeeds with no errors.

### Task 7.2: Run tests

- **Description:** Run `./gradlew test` to ensure no test regressions.
- **Command:** `./gradlew test`
- **Acceptance Criteria:** All tests pass.

### Task 7.3: Run detekt

- **Description:** Run `./gradlew detekt` to check for static analysis issues.
- **Command:** `./gradlew detekt`
- **Acceptance Criteria:** No new detekt violations.

---

## Sequencing

```
Phase 1 (Tasks 1.1–1.7)  ──► Build configs cleaned
         │
         ▼
Phase 2 (Tasks 2.1–2.2)  ──► Source sets deleted
         │
         ▼
Phase 3 (Task 3.1)       ──► Dependencies cleaned
         │
         ▼
Phase 4 (Tasks 4.1–4.4)  ──► Scripts & tooling removed
         │
         ▼
Phase 5 (Tasks 5.1–5.4)  ──► Docs updated
         │
         ▼
Phase 6 (Task 6.1)       ──► Serena memories updated
         │
         ▼
Phase 7 (Tasks 7.1–7.3)  ──► Build verification
```

Phases 1–3 must be sequential (build configs → source sets → deps). Phases 4, 5, and 6 can run in parallel after Phase 3. Phase 7 must be last.

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Missing `actual` declarations after removing web source sets | Low | High (build break) | Task 2.2 explicitly checks for this. Desktop/iOS/Android source sets should have their own `actual` impls for `WindowSizeClass` and `PlatformBackInput`. |
| Third-party lib only available on web | Very Low | Medium | Version catalog analysis showed only `ktor-client-js` is web-specific, and it's only used in web source sets. |
| KSP processor generates web-specific code | Low | Medium | KSP reads target declarations from Gradle — removing targets means no web code is generated. No KSP changes needed. |
| Published artifacts break consumers using web targets | Medium | High | Document the breaking change in CHANGELOG.md. Use a major or minor version bump. |

## Open Questions

- **Version bump:** Should this trigger a minor or major version bump? This is a breaking change for any consumers using JS/WASM artifacts.
- **CHANGELOG entry:** Should a CHANGELOG.md entry be added now or at release time?
