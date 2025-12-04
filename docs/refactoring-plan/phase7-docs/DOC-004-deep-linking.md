# Task DOC-004: Create Deep Linking Documentation

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | DOC-004 |
| **Name** | Deep Linking with Tree State Guide |
| **Phase** | 7 - Documentation |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | KSP-006, RISK-003 |

## Overview

Document how deep linking works with the new tree-based navigation state.

## Document Structure

```markdown
# Deep Linking Guide

## How Path Reconstruction Works
- Static graph analysis at compile time
- Runtime parameter extraction
- Tree building from leaf to root

## Defining Deep Links

@Route(path = "/product/{id}")
data class ProductDetail(val id: String) : CatalogGraph()

## TabNode Deep Links
- Automatic tab selection
- Stack preservation

## PaneNode Deep Links
- Multi-pane targeting

## Testing Deep Links

## Troubleshooting
```

## Files Affected

| File | Change Type |
|------|-------------|
| `docs/DEEP_LINKING.md` | New |

## Acceptance Criteria

- [ ] Path reconstruction explained
- [ ] URL pattern examples
- [ ] TabNode and PaneNode scenarios
- [ ] Troubleshooting section
