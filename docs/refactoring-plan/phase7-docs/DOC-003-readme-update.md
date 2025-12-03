# Task DOC-003: Update README with New Architecture

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | DOC-003 |
| **Name** | README Architecture Section Update |
| **Phase** | 7 - Documentation |
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Dependencies** | All core implementations |

## Overview

Update main README to reflect tree-based architecture and QuoVadisHost.

## Changes Required

1. Add architecture diagram
2. Update quick-start examples
3. Highlight key features: SharedElement, PredictiveBack, Panes
4. Link to detailed documentation

## New Section: Architecture Overview

```
## Architecture

Quo Vadis uses a tree-based navigation state:

┌─────────────────────────────────────────────┐
│                 StackNode (root)            │
│  ┌─────────────────────────────────────────┐│
│  │            TabNode (main)               ││
│  │  ┌──────────┐  ┌──────────┐             ││
│  │  │StackNode │  │StackNode │  ...        ││
│  │  │ (home)   │  │(profile) │             ││
│  │  │┌────────┐│  │┌────────┐│             ││
│  │  ││Screen  ││  ││Screen  ││             ││
│  │  │└────────┘│  │└────────┘│             ││
│  │  └──────────┘  └──────────┘             ││
│  └─────────────────────────────────────────┘│
└─────────────────────────────────────────────┘
```

## Files Affected

| File | Change Type |
|------|-------------|
| `README.md` | Modify |

## Acceptance Criteria

- [ ] Architecture diagram added
- [ ] Quick-start updated for new API
- [ ] Links to all documentation
