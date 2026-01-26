# Features.tsx Page Refactoring - Implementation Plan

## Overview

This plan details the refactoring of [Features.tsx](../docs/site/src/pages/Features/Features.tsx) to eliminate duplicated content by leveraging shared infrastructure components.

**Prerequisite**: [docs-site-shared-infrastructure.md](docs-site-shared-infrastructure.md) must be implemented first.

**Priority**: Highest - Features.tsx has the most duplicated content across the documentation site.

---

## Current State Analysis

### File Structure

| Section | Lines | Content Type | Duplication Status |
|---------|-------|--------------|-------------------|
| Code examples (const declarations) | 1-148 | Inline code strings | Partially duplicated with other pages |
| Annotation-Based API section | 167-227 | Unique + code block | Code block can be shared |
| Type-Safe Navigation section | 228-264 | Comparison content | Code blocks can be shared |
| Multiplatform Support section | 265-305 | Platform grid | **DUPLICATED** (Home.tsx, Demo.tsx) |
| Stack Management section | 306-330 | Unique content | Code block partially shared |
| Deep Link section | 331-358 | Unique content | - |
| Predictive Back section | 359-382 | Unique content | - |
| Shared Elements section | 383-413 | Unique content | - |
| MVI Architecture section | 414-435 | Unique content | - |
| Transitions section | 436-477 | Transition grid | **DUPLICATED** (Transitions.tsx) |
| Testing section | 478-513 | Testing content | Code can reference Testing page |
| Modular Architecture section | 514-545 | Unique content | - |
| DI Integration section | 546-573 | Unique content | - |
| Performance/Dependencies sections | 574-612 | Unique content | - |
| Next Steps section | 613-624 | Link list | - |

### Identified Duplications

1. **Platform Grid** (L272-302)
   - Duplicates: [Home.tsx](../docs/site/src/pages/Home/Home.tsx), [Demo.tsx](../docs/site/src/pages/Demo/Demo.tsx)
   - Content: 4 platform cards (Android, iOS, Desktop, Web) with feature lists

2. **Transition Type Cards** (L453-474)
   - Duplicates: [Transitions.tsx](../docs/site/src/pages/Features/Transitions/Transitions.tsx)
   - Content: 6 transition type cards (SlideHorizontal, SlideVertical, Fade, FadeThrough, ScaleIn, None)

3. **Code Examples** (L1-148)
   - Partially duplicates: Home.tsx step code, GettingStarted.tsx examples
   - Affected examples:
     - `typeSafeAnnotationCode` - similar to Home.tsx step1Code
     - `testingCode` - could reference Testing page instead

---

## Implementation Tasks

### Task 1: Add Shared Infrastructure Imports

**Depends on**: Infrastructure Phase 1 (constants.ts, codeExamples.ts) and Phase 2 (components)

**Current code** (L1-4):
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from './Features.module.css'
```

**After refactoring**:
```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import { PlatformSupportGrid } from '@components/PlatformSupportGrid'
import { TransitionTypesDisplay } from '@components/TransitionTypesDisplay'
import {
  stackDestinationBasic,
  screenBindingBasic,
} from '@/data/codeExamples'
import styles from './Features.module.css'
```

**Acceptance Criteria**:
- [ ] Import `PlatformSupportGrid` component
- [ ] Import `TransitionTypesDisplay` component
- [ ] Import relevant code examples from `codeExamples.ts`
- [ ] No TypeScript errors after imports

---

### Task 2: Replace Platform Grid with Shared Component

**Depends on**: Task 1, Infrastructure Task 2.1 (PlatformSupportGrid)

**Current code** (L272-302):
```tsx
<div className={styles.platformGrid}>
  <div className={styles.platformCard}>
    <h3>Android</h3>
    <ul>
      <li>System back button integration</li>
      <li>Predictive back gestures (Android 13+)</li>
      <li>Deep link support</li>
      <li>SavedStateHandle integration</li>
    </ul>
  </div>

  <div className={styles.platformCard}>
    <h3>iOS</h3>
    <ul>
      <li>Native swipe gestures</li>
      <li>Predictive back animations</li>
      <li>Universal Links support</li>
      <li>Navigation bar integration</li>
    </ul>
  </div>

  <div className={styles.platformCard}>
    <h3>Desktop</h3>
    <ul>
      <li>Keyboard shortcuts (Alt+Left/Right)</li>
      <li>Mouse button navigation</li>
      <li>Window state persistence</li>
      <li>All core features</li>
    </ul>
  </div>

  <div className={styles.platformCard}>
    <h3>Web</h3>
    <ul>
      <li>Browser history integration</li>
      <li>URL routing</li>
      <li>Forward/back buttons</li>
      <li>Deep linking via URLs</li>
    </ul>
  </div>
</div>
```

**After refactoring**:
```tsx
<PlatformSupportGrid variant="cards" />
```

**Notes**:
- The `PlatformSupportGrid` component uses the same default data as currently hardcoded
- `variant="cards"` produces the exact same layout and content

**Acceptance Criteria**:
- [ ] Inline platform grid replaced with `<PlatformSupportGrid variant="cards" />`
- [ ] Visual output identical to current state
- [ ] No horizontal scrolling or layout issues

---

### Task 3: Replace Transition Grid with Shared Component

**Depends on**: Task 1, Infrastructure Task 2.2 (TransitionTypesDisplay)

**Current code** (L453-474):
```tsx
<div className={styles.transitionGrid}>
  <div className={styles.transitionCard}>
    <h4>SlideHorizontal</h4>
    <p>Standard horizontal slide, ideal for hierarchical navigation</p>
  </div>
  <div className={styles.transitionCard}>
    <h4>SlideVertical</h4>
    <p>Vertical slide, perfect for modal presentations</p>
  </div>
  <div className={styles.transitionCard}>
    <h4>Fade</h4>
    <p>Simple cross-fade between screens</p>
  </div>
  <div className={styles.transitionCard}>
    <h4>FadeThrough</h4>
    <p>Material Design fade through pattern</p>
  </div>
  <div className={styles.transitionCard}>
    <h4>ScaleIn</h4>
    <p>Scale animation for emphasizing content</p>
  </div>
  <div className={styles.transitionCard}>
    <h4>None</h4>
    <p>Instant navigation without animation</p>
  </div>
</div>
```

**After refactoring**:
```tsx
<TransitionTypesDisplay variant="grid" />
```

**Notes**:
- Component default data matches current hardcoded content exactly
- `variant="grid"` produces card-style layout

**Acceptance Criteria**:
- [ ] Inline transition grid replaced with `<TransitionTypesDisplay variant="grid" />`
- [ ] Card descriptions match current text
- [ ] Grid layout matches current 3-column responsive behavior

---

### Task 4: Replace Type-Safe Annotation Code Example

**Depends on**: Task 1, Infrastructure Task 1.2 (codeExamples.ts)

**Current code** (L60-81 - `typeSafeAnnotationCode` const):
```tsx
const typeSafeAnnotationCode = `// Define destinations with annotations
@Graph("feature")
sealed class FeatureDestination : Destination

@Route("feature/list")
data object List : FeatureDestination()

@Serializable
data class DetailData(val id: String, val mode: ViewMode = ViewMode.READ)

@Route("feature/details")
@Argument(DetailData::class)
data class Details(val id: String, val mode: ViewMode = ViewMode.READ) 
    : FeatureDestination(), TypedDestination<DetailData> {
    override val data = DetailData(id, mode)
}

// Navigate with generated extension
navigator.navigateToDetails(id = "123", mode = ViewMode.EDIT)`
```

**After refactoring**:
- This example is unique to Features.tsx (uses `@Graph` pattern instead of `@Stack`)
- **Keep inline** - the example demonstrates a different annotation style
- Consider renaming to clarify it's a Features-specific example

**Decision**: Keep inline - this example is intentionally different from shared examples.

---

### Task 5: Consolidate Remaining Code Examples

**Depends on**: Task 1

**Analysis of code examples in Features.tsx**:

| Variable | Lines | Action | Reason |
|----------|-------|--------|--------|
| `annotationExample` | 5-57 | Keep inline | Complete, unique example for Features page |
| `typeSafeAnnotationCode` | 60-81 | Keep inline | Uses @Graph pattern (unique) |
| `typeSafeManualCode` | 83-103 | Keep inline | Manual DSL comparison example |
| `stackManagementCode` | 105-121 | Consider sharing | Similar to BackStack page |
| `deepLinkCode` | 123-143 | Keep inline | Features-specific format |
| `predictiveBackCode` | 145-149 | Keep inline | Simple, unique |
| `sharedElementCode` | 151-168 | Keep inline | Features-specific |
| `customTransitionCode` | 170-175 | Consider sharing | Duplicates Transitions.tsx |
| `testingCode` | 177-204 | Keep inline | Detailed, but add link to Testing page |
| `modularCode` | 206-226 | Keep inline | Unique modular architecture example |
| `koinCode` | 228-240 | Consider sharing | Similar to DI pages |
| `customInjectionCode` | 242-251 | Keep inline | Unique |

**Recommended changes**:
1. Keep most examples inline (they're page-specific demonstrations)
2. Add cross-reference link to Testing section pointing to Testing page for more detail
3. Future consideration: Extract `customTransitionCode` to codeExamples.ts when Transitions.tsx is refactored

**Acceptance Criteria**:
- [ ] No code duplication introduced
- [ ] Testing section includes link to dedicated Testing page
- [ ] Examples remain contextually appropriate

---

### Task 6: Add Cross-Reference Links

**Depends on**: Tasks 2-5

**Current State**: Features.tsx includes full explanations of concepts that have dedicated pages.

**Sections to add cross-references**:

1. **Transitions & Animations section** (after transition grid):
   ```tsx
   <div className={styles.note}>
     <strong>📖 Deep Dive:</strong> See the{' '}
     <Link to="/features/transitions">Transitions Guide</Link> for custom 
     transitions, per-destination configuration, and animation details.
   </div>
   ```

2. **Testing Support section** (after testing code):
   ```tsx
   <div className={styles.note}>
     <strong>📖 More Examples:</strong> See the{' '}
     <Link to="/features/testing">Testing Guide</Link> for comprehensive 
     FakeNavigator usage, integration testing, and state verification.
   </div>
   ```

3. **MVI Architecture section** (existing note is good, verify link):
   - Current link: `/features/di-integration` → Verify this is correct route

4. **Multiplatform Support section** (after platform grid):
   ```tsx
   <div className={styles.note}>
     <strong>📖 Platform Details:</strong> See the{' '}
     <Link to="/features/multiplatform">Multiplatform Guide</Link> for 
     platform-specific setup, requirements, and feature matrix.
   </div>
   ```

**Acceptance Criteria**:
- [ ] Cross-reference added after Transitions section
- [ ] Cross-reference added after Testing section  
- [ ] Cross-reference added after Multiplatform section
- [ ] All links verified to work

---

### Task 7: Clean Up Unused CSS Styles

**Depends on**: Tasks 2-3

**CSS classes to potentially remove from Features.module.css**:

After replacing inline grids with shared components, these styles may become unused:

```css
/* Potentially unused after refactoring */
.platformGrid { ... }
.platformCard { ... }
.transitionGrid { ... }
.transitionCard { ... }
```

**Verification process**:
1. Search entire Features.tsx for usage of each className
2. If only used in replaced sections, mark for removal
3. Run build to verify no CSS import errors

**Important**: Do NOT remove styles until Tasks 2-3 are complete and verified.

**Expected removable styles** (Features.module.css):
- `.platformGrid` - replaced by PlatformSupportGrid component
- `.platformCard` - replaced by PlatformSupportGrid component
- `.transitionGrid` - replaced by TransitionTypesDisplay component
- `.transitionCard` - replaced by TransitionTypesDisplay component

**Styles to KEEP**:
- `.features` - main container
- `.intro` - introduction paragraph
- `.note` - note boxes (used in multiple places)
- `.annotationGrid` - unique to Features page
- `.annotationCard` - unique to Features page

**Acceptance Criteria**:
- [ ] Unused styles identified
- [ ] Build passes after style removal
- [ ] No visual regressions on Features page

---

## Task Summary & Sequencing

```
Infrastructure (prerequisite)
│
├── constants.ts ─────────────┐
│                             │
└── codeExamples.ts ─────────┤
                              │
├── PlatformSupportGrid ─────┤
│                             │
└── TransitionTypesDisplay ──┘
                              │
Features.tsx Refactoring      │
│                             │
├── Task 1: Add imports ──────┴── (depends on infrastructure)
│
├── Task 2: Replace platform grid ── (depends on Task 1)
│
├── Task 3: Replace transition grid ── (depends on Task 1)
│
├── Task 4: Review code examples ── (depends on Task 1)
│
├── Task 5: Consolidate code examples ── (depends on Task 4)
│
├── Task 6: Add cross-references ── (depends on Tasks 2-5)
│
└── Task 7: Clean up CSS ── (depends on Tasks 2-3, do LAST)
```

---

## Before/After Comparison

### Lines of Code Estimate

| Content | Before | After | Reduction |
|---------|--------|-------|-----------|
| Platform grid (JSX) | ~30 lines | ~1 line | 97% |
| Transition grid (JSX) | ~24 lines | ~1 line | 96% |
| Code examples | ~250 lines | ~250 lines | 0% (kept inline) |
| Cross-reference notes | 0 lines | ~12 lines | +12 lines |
| CSS (module) | ~80 lines | ~50 lines | 37% |
| **Total** | ~400+ lines | ~320 lines | ~20% |

### Maintenance Benefits

1. **Platform information**: Updated once in `PlatformSupportGrid` defaults, reflected everywhere
2. **Transition types**: Updated once in `TransitionTypesDisplay` defaults, reflected everywhere
3. **Cross-references**: Readers directed to detailed pages instead of partial duplications

---

## Verification Checklist

### After Each Task

- [ ] `npm run build` succeeds
- [ ] No TypeScript errors
- [ ] Features page renders correctly
- [ ] Page interactions work (scroll, links)

### Final Verification

- [ ] Platform grid visually identical to before
- [ ] Transition grid visually identical to before
- [ ] All code blocks render properly
- [ ] All links navigate correctly
- [ ] Mobile responsive layout works
- [ ] Dark mode displays correctly (if applicable)
- [ ] No console errors in browser

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Component variant doesn't match existing layout | Medium | Medium | Verify component props before replacing; test with defaults |
| CSS variable mismatches | Low | Low | Components use same CSS variables as Features.module.css |
| Missing cross-reference routes | Low | Medium | Verify route exists before adding Link |
| Breaking other pages that import from Features.tsx | Low | High | Features.tsx doesn't export anything used by other pages |

---

## Implementation Notes

### Component Import Path Configuration

Verify `@components` alias is configured in:
- `tsconfig.json` paths
- Vite config (`vite.config.ts`)

Expected configuration:
```typescript
// tsconfig.json
{
  "compilerOptions": {
    "paths": {
      "@components/*": ["./src/components/*"],
      "@/*": ["./src/*"]
    }
  }
}
```

### Style Variable Verification

Shared components use these CSS variables - verify they exist:
- `--color-bg-elevated`
- `--color-border`
- `--color-text-secondary`
- `--font-mono`

---

## Acceptance Criteria Summary

### Task 1 Complete When:
- [ ] All imports added
- [ ] No import errors

### Task 2 Complete When:
- [ ] Platform grid replaced with `<PlatformSupportGrid variant="cards" />`
- [ ] Visual output unchanged

### Task 3 Complete When:
- [ ] Transition grid replaced with `<TransitionTypesDisplay variant="grid" />`
- [ ] Visual output unchanged

### Tasks 4-5 Complete When:
- [ ] Code examples reviewed
- [ ] Decision documented for each example

### Task 6 Complete When:
- [ ] Cross-references added to 3 sections
- [ ] Links verified working

### Task 7 Complete When:
- [ ] Unused CSS removed
- [ ] Build passes
- [ ] No visual regressions

### All Tasks Complete When:
- [ ] All individual task criteria met
- [ ] Final verification checklist passed
- [ ] PR review approved (if applicable)
