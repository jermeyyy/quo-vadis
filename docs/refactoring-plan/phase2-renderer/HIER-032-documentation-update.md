# HIER-032: Documentation Update

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-032 |
| **Task Name** | Update API Documentation |
| **Phase** | Phase 5: Migration |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | HIER-031 |
| **Blocked By** | HIER-031 |
| **Blocks** | HIER-033 |

---

## Overview

Update all library documentation to reflect the new hierarchical rendering system as the primary approach.

---

## Files to Update

### Core Library Docs

```
quo-vadis-core/docs/
├── ARCHITECTURE.md                  # Update rendering section
├── API_REFERENCE.md                 # Add hierarchical API
├── NAVIGATION_IMPLEMENTATION.md     # Update host usage
└── MULTIPLATFORM_PREDICTIVE_BACK.md # Update gesture handling
```

### Root Docs

```
/
├── README.md                        # Update quick start
└── docs/
    ├── examples/                    # Update all examples
    └── migration-examples/          # Add hierarchical examples
```

### Website

```
docs/site/src/
├── pages/
│   ├── getting-started.tsx          # Update getting started
│   └── api/                         # Add API pages
└── components/
    └── CodeExamples.tsx             # Update examples
```

---

## Documentation Changes

### ARCHITECTURE.md

Add section:
```markdown
## Rendering Architecture

### Hierarchical Rendering (Default)

The library uses hierarchical rendering where the composable tree
mirrors the navigation state tree:

\```
NavTreeRenderer
├── TabRenderer (wrapper containing...)
│   └── StackRenderer
│       └── ScreenRenderer
└── ...
\```

This ensures:
- Wrappers contain their content
- Animations respect parent/child relationships
- Predictive back transforms entire subtrees

### Legacy Flattened Rendering (Deprecated)

The previous RenderableSurface system has been deprecated.
See [Migration Guide](MIGRATION_HIERARCHICAL_RENDERING.md).
```

### README.md Quick Start

Update to:
```kotlin
@Composable
fun App() {
    val navigator = rememberNavigator(startDestination = Home)
    
    HierarchicalQuoVadisHost(
        navigator = navigator,
        modifier = Modifier.fillMaxSize()
    )
}
```

### API_REFERENCE.md

Add:
- `HierarchicalQuoVadisHost` documentation
- `NavRenderScope` documentation
- `@TabWrapper` and `@PaneWrapper` documentation
- `NavTransition` presets

---

## Acceptance Criteria

- [ ] ARCHITECTURE.md updated with rendering section
- [ ] API_REFERENCE.md has all new APIs
- [ ] README.md quick start uses HierarchicalQuoVadisHost
- [ ] All examples updated
- [ ] Website pages updated
- [ ] Dokka comments complete
- [ ] Cross-links to migration guide
