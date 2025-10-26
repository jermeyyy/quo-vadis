# Quo Vadis Documentation Update Implementation Plan

## Executive Summary

This specification outlines a comprehensive plan to update all documentation for the Quo Vadis navigation library to reflect recent major additions:
1. **Annotation-based API** (KSP code generation with `@Graph`, `@Route`, `@Argument`, `@Content`)
2. **Type-safe destination arguments** (TypedDestination and serialization)
3. **Shared element transitions** (already documented)
4. **Dokka integration for all modules** (core, annotations, ksp)

The goal is to ensure all documentation is accurate, complete, and showcases the new simplified API approach.

---

## Current State Analysis

### Recent Major Changes (from Git History)

#### Annotation-based API (Commits: 43f6735 → 930c25e)
- **New modules created:**
  - `quo-vadis-annotations` - Annotation definitions (@Graph, @Route, @Argument, @Content)
  - `quo-vadis-ksp` - KSP processor for code generation
- **Key features:**
  - `@Graph(name)` - Mark sealed classes as navigation graphs
  - `@Route(path)` - Define route paths for destinations
  - `@Argument(dataClass)` - Specify typed data for destinations
  - `@Content(destination)` - Wire Composable functions to destinations
  - Auto-generated graph builders (`buildXxxGraph()`)
  - Auto-generated route registration
  - Auto-generated typed destination extensions

#### Type-safe Destination Args (Commit: f01c760)
- `TypedDestination<T>` interface with serializable data
- `typedDestination()` DSL function for automatic deserialization
- Integration with kotlinx.serialization
- Eliminates manual argument parsing
- Full multiplatform support

#### Dokka Integration (Commits: 4198aa1, 930c25e)
- Dokka 2.0.0 configured for all modules
- HTML output with GitHub source links
- External documentation links (Android, Coroutines)
- Auto-deployment to GitHub Pages at `/api/`

### Existing Documentation Status

#### ✅ Well-documented
- **Shared element transitions** - `SHARED_ELEMENT_TRANSITIONS.md` is comprehensive
- **Predictive back** - `MULTIPLATFORM_PREDICTIVE_BACK.md` is complete
- **Architecture** - `ARCHITECTURE.md` covers design patterns well

#### ⚠️ Needs Major Updates
- **API_REFERENCE.md** - Missing annotation-based API, TypedDestination
- **NAVIGATION_IMPLEMENTATION.md** - Shows only manual DSL approach
- **README.md** - Quick start examples use old manual approach
- **Getting Started (website)** - No annotation examples
- **Features (website)** - No mention of KSP code generation

#### ❌ Missing Documentation
- No dedicated guide for annotation-based API
- No documentation on KSP processor usage
- No guide on choosing between manual DSL vs annotations
- Limited examples of TypedDestination usage

---

## Implementation Plan

### Phase 1: Create New Documentation Files

#### Task 1.1: Create Annotation-based API Guide
**File:** `quo-vadis-core/docs/ANNOTATION_API.md`

**Content structure:**
1. **Overview**
   - What is the annotation-based API
   - Benefits over manual DSL (less boilerplate, auto-wiring, compile-time safety)
   - When to use annotations vs manual DSL

2. **Setup**
   - Adding dependencies (annotations, ksp)
   - Gradle KSP plugin configuration
   - Module structure requirements

3. **Core Annotations**
   - `@Graph(name)` - Graph definition with examples
   - `@Route(path)` - Route definition with examples
   - `@Argument(dataClass)` - Typed arguments with examples
   - `@Content(destination)` - Content wiring with examples

4. **Code Generation**
   - What gets generated (route registration, graph builders, typed extensions)
   - Generated file locations and naming conventions
   - How to use generated code

5. **Complete Examples**
   - Simple destinations without arguments
   - Typed destinations with serializable data
   - Complex graph with multiple destination types
   - Integration with existing manual DSL

6. **Advanced Topics**
   - Dynamic routes (Tab example from codebase)
   - Mixing annotations with manual graph building
   - Testing strategies
   - Troubleshooting KSP generation

#### Task 1.2: Create TypedDestination Guide
**File:** `quo-vadis-core/docs/TYPED_DESTINATIONS.md`

**Content structure:**
1. **Overview**
   - Problem: Manual argument parsing is error-prone
   - Solution: Type-safe data classes with serialization

2. **TypedDestination Interface**
   - Interface definition and purpose
   - The `data` property requirement
   - Serialization requirements

3. **Using with Annotations**
   - `@Argument(dataClass)` annotation
   - TypedDestination implementation
   - Auto-generated typed extensions

4. **Using with Manual DSL**
   - `typedDestination<T>()` function
   - Serialization configuration
   - Manual registration

5. **Serialization**
   - kotlinx.serialization setup
   - Supported types
   - Custom serializers
   - Multiplatform considerations

6. **Complete Examples**
   - Simple data class (DetailData)
   - Complex nested data
   - Optional fields
   - Collections and maps

7. **Migration Guide**
   - From manual arguments to TypedDestination
   - Before/after code examples

---

### Phase 2: Update Existing Documentation

#### Task 2.1: Update API_REFERENCE.md

**Changes needed:**

1. **Add new section: "Annotation-based API"** (after "Core Package")
   - Document all annotations with signatures
   - Show generated code examples
   - Link to ANNOTATION_API.md for details

2. **Add TypedDestination to Destination section**
   - Interface definition
   - Usage examples
   - Serialization requirements

3. **Update destination() DSL section**
   - Add typedDestination() variant
   - Show both manual and typed approaches
   - Clarify when to use each

4. **Add code generation section**
   - buildXxxGraph() functions
   - Route initializers
   - Typed destination extensions

5. **Update examples throughout**
   - Add annotation-based examples alongside manual DSL
   - Show both approaches for fairness

#### Task 2.2: Update NAVIGATION_IMPLEMENTATION.md

**Changes needed:**

1. **Update "Defining Destinations" section**
   - Add annotation-based approach
   - Show @Route, @Argument decorators
   - Compare with manual approach

2. **Update "Creating Navigation Graphs" section**
   - Show generated graph builders
   - Explain auto-wiring with @Content
   - Demonstrate include() for generated graphs

3. **Add "TypedDestination" subsection**
   - Explain typed arguments
   - Show serialization integration
   - Demonstrate automatic deserialization

4. **Update all code examples**
   - Provide both manual and annotation versions
   - Highlight which is recommended for new projects

#### Task 2.3: Update ARCHITECTURE.md

**Changes needed:**

1. **Add "Code Generation Layer" to architecture diagram**
   - KSP processor as build-time layer
   - Annotations → Generated code flow
   - Integration points with core library

2. **Update "API Layers" section**
   - Add "Annotation API" as highest-level layer
   - Show layering: Annotations → Generated DSL → Core

3. **Add "Design Decisions" subsection**
   - Why annotations are optional (not required)
   - Keeping core library dependency-free
   - KSP as optional build-time dependency

4. **Update "Extensibility" section**
   - How to extend annotation processor
   - Custom annotations (future)

#### Task 2.4: Update README.md

**Changes needed:**

1. **Update "Key Features" section**
   - Add "Annotation-based API with KSP code generation"
   - Add "Type-safe serializable arguments"

2. **Update "Project Structure" section**
   - Add `quo-vadis-annotations/` module
   - Add `quo-vadis-ksp/` module

3. **Replace "Quick Start" section**
   - Show annotation-based approach FIRST (as recommended)
   - Provide manual DSL as alternative
   - Update all code examples to use @Content pattern

4. **Update "Documentation" section**
   - Add links to ANNOTATION_API.md
   - Add links to TYPED_DESTINATIONS.md
   - Reorganize for better flow

5. **Add "Module Overview" section**
   - `quo-vadis-core` - Core navigation library
   - `quo-vadis-annotations` - Annotation definitions
   - `quo-vadis-ksp` - KSP code generator
   - `composeApp` - Demo application

6. **Update dependency examples**
   - Show how to add annotations + ksp
   - Show version catalog setup
   - Show KSP plugin configuration

---

### Phase 3: Update Website Documentation

#### Task 3.1: Update getting-started.html

**Changes needed:**

1. **Update "Installation" section**
   - Add quo-vadis-annotations dependency
   - Add KSP plugin setup instructions
   - Show complete build.gradle.kts example

2. **Rewrite "Basic Setup" section**
   - Replace Step 1 with annotation-based destinations
   - Replace Step 2 with @Content functions
   - Replace Step 3 with generated graph usage

3. **Add "Two Approaches" section**
   - Explain annotation-based (recommended) vs manual DSL
   - Provide side-by-side comparison
   - Link to full guides

4. **Update all code examples**
   - Use @Graph, @Route, @Argument, @Content
   - Show generated buildXxxGraph() usage
   - Add TypedDestination examples

#### Task 3.2: Update features.html

**Changes needed:**

1. **Add new "Annotation-based API" section** (after "Type-Safe Navigation")
   - Explain KSP code generation
   - Show benefits (less boilerplate, auto-wiring)
   - Provide complete example with all annotations

2. **Update "Type-Safe Navigation" section**
   - Add TypedDestination subsection
   - Show serializable data classes
   - Demonstrate type-safe argument passing

3. **Add "Code Generation" subsection**
   - Explain what gets generated
   - Show before/after comparison
   - Highlight compile-time safety

4. **Update examples throughout**
   - Prefer annotation-based examples
   - Show manual DSL as alternative where relevant

#### Task 3.3: Update index.html

**Changes needed:**

1. **Update "Key Features" section**
   - Add feature item for "Annotation-based API"
   - Add feature item for "Type-Safe Arguments"
   - Update descriptions to mention KSP generation

2. **Update "Quick Example" section** (if present)
   - Use annotation-based approach
   - Show minimal but complete example

#### Task 3.4: Update demo.html

**Changes needed:**

1. **Add "Annotation-based API" demo section**
   - Show MasterDetailDestination with @Graph, @Route, @Argument
   - Show ContentDefinitions.kt with @Content
   - Show generated graph usage in NavigationGraphs.kt

2. **Update existing demos**
   - Add notes about annotation-based implementation
   - Link to actual demo code on GitHub

---

### Phase 4: Update API Documentation (Dokka)

#### Task 4.1: Verify Dokka Configuration

**Files to check:**
- `quo-vadis-core/build.gradle.kts`
- `quo-vadis-annotations/build.gradle.kts`
- `quo-vadis-ksp/build.gradle.kts`

**Verification:**
- All modules have Dokka plugin applied
- Module names are descriptive
- Source links configured correctly
- Internal packages suppressed
- External docs linked

#### Task 4.2: Enhance KDoc Comments

**Focus areas:**

1. **quo-vadis-annotations module**
   - Enhance @Graph annotation KDoc with complete example
   - Enhance @Route annotation KDoc with path patterns
   - Enhance @Argument annotation KDoc with serialization requirements
   - Enhance @Content annotation KDoc with signature requirements

2. **quo-vadis-core module**
   - Add/enhance TypedDestination KDoc
   - Add/enhance typedDestination() function KDoc
   - Update RouteRegistry KDoc to mention auto-registration
   - Add examples showing annotation usage

3. **quo-vadis-ksp module**
   - Add module-level KDoc explaining purpose
   - Document public APIs (if any)
   - Add processor behavior documentation

#### Task 4.3: Test Dokka Generation

**Steps:**
1. Run `./gradlew :quo-vadis-core:dokkaGenerateHtml`
2. Run `./gradlew :quo-vadis-annotations:dokkaGenerateHtml`
3. Run `./gradlew :quo-vadis-ksp:dokkaGenerateHtml`
4. Verify all output is correct and complete
5. Check that navigation between modules works
6. Verify GitHub source links work

---

### Phase 5: Update Demo Application Comments

#### Task 5.1: Update Destinations.kt

**Changes:**
- Enhance comments explaining annotation usage
- Add comments showing generated code structure
- Document TypedDestination implementations
- Add notes on dynamic routes (Tab example)

#### Task 5.2: Update ContentDefinitions.kt

**Changes:**
- Add comprehensive file-level comment explaining pattern
- Document @Content annotation usage
- Show how it replaces manual graph builders
- Add notes on function signature requirements

#### Task 5.3: Update NavigationGraphs.kt

**Changes:**
- Explain auto-generated graph builders
- Document include() pattern
- Show how everything is wired together
- Add comparison with old manual approach (in comments)

---

### Phase 6: Create Migration Guide

#### Task 6.1: Create MIGRATION.md
**File:** `quo-vadis-core/docs/MIGRATION.md`

**Content structure:**

1. **Overview**
   - What changed in recent versions
   - Why migrate to annotation-based API
   - Compatibility and breaking changes

2. **Migration to Annotation-based API**
   - Step-by-step guide
   - Code transformation examples
   - Common patterns before/after

3. **Migration to TypedDestination**
   - Converting manual arguments to serializable data
   - Updating destination definitions
   - Updating graph registrations

4. **Incremental Migration**
   - How to mix old and new approaches
   - Which parts to migrate first
   - Testing during migration

5. **Complete Example**
   - Full before/after comparison
   - Real-world feature migration

---

### Phase 7: Update Memory Files

#### Task 7.1: Update project_overview memory

**Add:**
- Annotation-based API overview
- New modules (annotations, ksp)
- TypedDestination capability
- Updated publishing info (3 modules)

#### Task 7.2: Create annotation_api_guide memory

**Content:**
- Quick reference for annotations
- Generated code patterns
- Common usage examples
- Integration patterns

#### Task 7.3: Update codebase_structure memory

**Add:**
- quo-vadis-annotations module structure
- quo-vadis-ksp module structure
- Generated code locations
- Build-time code generation flow

---

### Phase 8: Verification and Quality Assurance

#### Task 8.1: Documentation Review Checklist

**For each documentation file:**
- [ ] All code examples compile and run
- [ ] Links between documents work
- [ ] Table of contents updated (if present)
- [ ] No references to deprecated patterns
- [ ] Consistent terminology throughout
- [ ] Proper code formatting
- [ ] All annotations documented
- [ ] TypedDestination covered
- [ ] Both manual and annotation approaches shown

#### Task 8.2: Website Review Checklist

**For each HTML file:**
- [ ] Navigation links work
- [ ] Code highlighting works
- [ ] Examples are accurate and complete
- [ ] Mobile responsive layout
- [ ] Theme switcher works
- [ ] API reference link works
- [ ] GitHub links work
- [ ] Images load correctly

#### Task 8.3: Dokka Review Checklist

**For generated API docs:**
- [ ] All modules generate successfully
- [ ] Navigation between modules works
- [ ] Source links go to correct files
- [ ] External docs links work
- [ ] No internal packages exposed
- [ ] Search functionality works
- [ ] All public APIs documented
- [ ] Examples render correctly

#### Task 8.4: Demo Code Review

**For demo application:**
- [ ] Comments explain annotation usage
- [ ] Examples showcase best practices
- [ ] Generated code mentioned in comments
- [ ] All patterns demonstrated
- [ ] Code matches documentation

---

## Detailed File Changes

### New Files to Create

1. **`quo-vadis-core/docs/ANNOTATION_API.md`** (~800 lines)
2. **`quo-vadis-core/docs/TYPED_DESTINATIONS.md`** (~500 lines)
3. **`quo-vadis-core/docs/MIGRATION.md`** (~400 lines)

### Existing Files to Update

1. **`quo-vadis-core/docs/API_REFERENCE.md`**
   - Add 300+ lines (annotation API section)
   - Update 100+ lines (destination sections)
   
2. **`quo-vadis-core/docs/NAVIGATION_IMPLEMENTATION.md`**
   - Update ~200 lines (throughout)
   - Add ~150 lines (new sections)

3. **`quo-vadis-core/docs/ARCHITECTURE.md`**
   - Add ~100 lines (code generation layer)
   - Update ~50 lines (architecture diagrams)

4. **`README.md`**
   - Update Quick Start section (~100 lines)
   - Update features list (~30 lines)
   - Add module overview (~80 lines)
   - Update documentation section (~40 lines)

5. **`docs/site/getting-started.html`**
   - Rewrite installation section (~50 lines)
   - Rewrite basic setup section (~150 lines)
   - Add approach comparison (~80 lines)

6. **`docs/site/features.html`**
   - Add annotation API section (~120 lines)
   - Update type-safety section (~60 lines)
   - Update examples (~100 lines)

7. **`docs/site/index.html`**
   - Update features list (~30 lines)
   - Update quick example (~40 lines)

8. **`docs/site/demo.html`**
   - Add annotation demo section (~100 lines)
   - Update existing demos (~50 lines)

### KDoc Updates

1. **`quo-vadis-annotations/src/commonMain/kotlin/.../Annotations.kt`**
   - Enhance all 4 annotation KDocs
   - Add comprehensive examples
   - ~80 lines of enhanced documentation

2. **`quo-vadis-core/src/commonMain/kotlin/.../Destination.kt`**
   - Add TypedDestination examples
   - Update interface KDoc
   - ~40 lines

3. **`quo-vadis-core/src/commonMain/kotlin/.../DestinationDsl.kt`**
   - Enhance typedDestination() KDoc
   - Add annotation-based examples
   - ~30 lines

---

## Git History Analysis Summary

Based on commit history analysis:

### Annotation System Evolution (5 iterations)
1. **annotations v1** (43f6735) - Initial implementation
2. **annotations v2** (4a454f5) - Refinements
3. **annotations v3** (440f8e2) - Further improvements
4. **annotations v4** (e457b26) - Near-final
5. **annotations v5** (2390cb2) - Production-ready

### Key Implementation Commits
- **f01c760** - Type-safe destination args foundation
- **930c25e** - Dokka integration for annotations and ksp modules
- **8332ed9** - Implementation summary (consolidation)
- **367a99e** - GitHub Pages v2 merge

### Demo Evolution
The demo app (composeApp) was completely refactored to use:
- Annotation-based destination definitions
- @Content functions for UI wiring
- Generated graph builders
- TypedDestination for complex arguments

---

## Documentation Principles

### 1. Show New API First
- Annotation-based approach is the recommended way
- Show it in all "quick start" and primary examples
- Manual DSL becomes "advanced" or "alternative"

### 2. Complete Examples Only
- Never show partial code that won't compile
- Include all imports and dependencies
- Provide context for each example

### 3. Progressive Disclosure
- Start simple (basic destinations)
- Build to complex (typed arguments, nested graphs)
- Advanced topics at end

### 4. Consistency
- Use same example domains across docs (master-detail, tabs, process)
- Consistent naming conventions
- Same code style everywhere

### 5. Multiplatform Focus
- Emphasize that everything works on all platforms
- Call out platform-specific features explicitly
- Show multiplatform setup examples

---

## Success Criteria

### Documentation is complete when:

1. ✅ All annotations are fully documented with examples
2. ✅ TypedDestination has dedicated guide
3. ✅ README quick start uses annotation-based API
4. ✅ Website getting-started shows annotation approach first
5. ✅ All existing docs updated to mention new APIs
6. ✅ Migration guide exists for existing users
7. ✅ Dokka generates clean API docs for all modules
8. ✅ Demo app comments explain annotation patterns
9. ✅ Memory files reflect current architecture
10. ✅ No stale/incorrect information remains

### Quality metrics:

- **Completeness**: Every public API documented
- **Accuracy**: All examples compile and run
- **Clarity**: Beginners can follow without confusion
- **Depth**: Advanced users find detailed explanations
- **Findability**: Information is easy to locate
- **Consistency**: Same terminology and patterns throughout

---

## Implementation Priority

### Critical (Must Do)
1. Create ANNOTATION_API.md (Task 1.1)
2. Update README.md (Task 2.4)
3. Update getting-started.html (Task 3.1)
4. Update API_REFERENCE.md (Task 2.1)

### High Priority (Should Do)
5. Create TYPED_DESTINATIONS.md (Task 1.2)
6. Update features.html (Task 3.2)
7. Update NAVIGATION_IMPLEMENTATION.md (Task 2.2)
8. Enhance annotation KDocs (Task 4.2)

### Medium Priority (Nice to Have)
9. Update index.html (Task 3.3)
10. Create MIGRATION.md (Task 6.1)
11. Update ARCHITECTURE.md (Task 2.3)
12. Update demo comments (Tasks 5.1-5.3)

### Low Priority (Can Wait)
13. Update demo.html (Task 3.4)
14. Update memory files (Tasks 7.1-7.3)
15. Dokka verification (Tasks 4.1, 4.3)

---

## Estimated Effort

**Total effort:** ~40-50 hours

**Breakdown by phase:**
- Phase 1 (New docs): 12-15 hours
- Phase 2 (Update existing): 10-12 hours
- Phase 3 (Website): 8-10 hours
- Phase 4 (Dokka): 4-5 hours
- Phase 5 (Demo comments): 2-3 hours
- Phase 6 (Migration guide): 3-4 hours
- Phase 7 (Memory files): 1-2 hours
- Phase 8 (QA): 4-6 hours

**Can be parallelized:**
- Website updates independent of markdown docs
- Dokka KDoc improvements independent of guides
- Demo comments independent of documentation files

---

## Dependencies and Prerequisites

### Before Starting:
1. ✅ Annotation system is stable (v5 in production)
2. ✅ Type-safe args implementation is complete
3. ✅ Dokka integration working for all modules
4. ✅ Demo app fully migrated to annotation-based API
5. ✅ GitHub Pages deployment pipeline working

### Required Tools:
- Text editor with markdown support
- HTML/CSS/JS editor for website
- Kotlin IDE for KDoc updates
- Git for version control
- Local web server for testing website
- Gradle for running Dokka

### Required Knowledge:
- Quo Vadis API (both annotation and manual)
- KSP basics (what it generates)
- kotlinx.serialization
- Compose Multiplatform
- Markdown formatting
- HTML/CSS (for website)
- Dokka documentation system

---

## Risk Assessment

### Low Risk:
- Creating new documentation files (doesn't break anything)
- Adding to existing docs (append new sections)
- Enhancing KDoc comments (doesn't affect API)

### Medium Risk:
- Rewriting README quick start (main entry point, must be perfect)
- Updating website getting-started (critical for new users)
- Changing documentation organization (could confuse existing users)

### Mitigation Strategies:
1. **Test all code examples** - Every snippet must compile
2. **Preview website locally** - Catch broken links/styling
3. **Peer review** - Have someone review before pushing
4. **Incremental updates** - Don't change everything at once
5. **Git branches** - Use feature branch for documentation work
6. **Backup old docs** - Keep old versions in case rollback needed

---

## Post-Implementation Tasks

### After documentation is complete:

1. **Announce updates**
   - GitHub release notes
   - Update CHANGELOG.md
   - Social media/community channels

2. **Gather feedback**
   - Monitor GitHub issues for documentation questions
   - Track which docs are most viewed
   - Collect user feedback on clarity

3. **Iterate**
   - Fix reported issues quickly
   - Improve based on feedback
   - Add FAQ section if common questions emerge

4. **Maintain**
   - Update docs with each release
   - Keep examples working with latest version
   - Review quarterly for accuracy

---

## Notes from Git History

### Key Observations:

1. **Demo app is the reference** - The composeApp module showcases best practices
2. **Gradual evolution** - 5 iterations of annotations shows careful refinement
3. **Documentation lag** - Website/docs not updated in sync with code
4. **Shared elements documented well** - Use as template for quality
5. **GitHub Pages already set up** - Deployment pipeline ready

### Files to Reference:

- `composeApp/src/.../destinations/Destinations.kt` - Perfect annotation usage examples
- `composeApp/src/.../content/ContentDefinitions.kt` - @Content pattern showcase
- `composeApp/src/.../graphs/NavigationGraphs.kt` - Generated graph usage
- `quo-vadis-core/docs/SHARED_ELEMENT_TRANSITIONS.md` - Quality documentation template

### Generated Code Patterns to Document:

1. **Route initializers**: `XxxDestinationRouteInitializer`
2. **Graph builders**: `buildXxxDestinationGraph()`
3. **Typed extensions**: `typedDestinationXxx()`

---

## Conclusion

This plan provides a comprehensive roadmap for updating all Quo Vadis documentation to reflect the major improvements added in recent months. The annotation-based API dramatically simplifies navigation setup, and this needs to be the primary story told in documentation.

By following this phased approach with clear priorities, the documentation will:
- Accurately reflect the current API
- Guide new users to the best practices
- Support existing users with migration guidance
- Maintain quality and consistency throughout
- Scale well for future additions

The annotation-based API is a significant value proposition and should be showcased prominently as the recommended approach for all new projects.
