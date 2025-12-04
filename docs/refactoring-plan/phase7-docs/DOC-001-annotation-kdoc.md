# Task DOC-001: Update KSP Annotation Documentation

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | DOC-001 |
| **Name** | Annotation KDoc Enhancement |
| **Phase** | 7 - Documentation |
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Dependencies** | All annotation changes |

## Overview

Update all annotation KDoc with new GraphType usage examples and migration hints.

## Changes Required

1. Update `@Graph` KDoc with `type` parameter examples
2. Add `@sample` code blocks for each GraphType
3. Add `@see` references to related annotations
4. Document backward compatibility behavior

## Example

```kotlin
/**
 * Marks a sealed class as a navigation graph.
 *
 * @param name The unique name for this graph
 * @param type The structural type (default: STACK)
 *
 * @sample com.jermey.quo.vadis.samples.stackGraphSample
 * @sample com.jermey.quo.vadis.samples.tabGraphSample
 * @see GraphType
 * @see Route
 */
annotation class Graph(...)
```

## Files Affected

- `quo-vadis-annotations/src/commonMain/kotlin/.../annotations/*.kt`

## Acceptance Criteria

- [ ] All annotations have comprehensive KDoc
- [ ] Examples for each GraphType included
- [ ] @see references added
- [ ] Migration hints documented
