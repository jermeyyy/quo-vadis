import styles from './ScopePropertiesTable.module.css';

export interface ScopeProperty {
  property: string;
  type: string;
  description: string;
}

export interface ScopePropertiesTableProps {
  scopeType: 'tabs' | 'pane';
  /** Custom properties (optional) */
  properties?: ScopeProperty[];
}

const TABS_SCOPE_PROPERTIES: ScopeProperty[] = [
  { property: 'navigator', type: 'Navigator', description: 'Navigation operations' },
  { property: 'activeTabIndex', type: 'Int', description: 'Currently selected tab (0-based)' },
  { property: 'tabCount', type: 'Int', description: 'Total number of tabs' },
  { property: 'tabs', type: 'List<NavDestination>', description: 'Tab destinations for custom labels, icons, and behavior' },
  { property: 'isTransitioning', type: 'Boolean', description: 'Whether transition is in progress' },
  { property: 'switchTab(index)', type: 'Function', description: 'Switch to different tab' },
];

const PANE_SCOPE_PROPERTIES: ScopeProperty[] = [
  { property: 'navigator', type: 'Navigator', description: 'Navigator instance for programmatic navigation' },
  { property: 'activePaneRole', type: 'PaneRole', description: 'Currently active/focused pane' },
  { property: 'paneCount', type: 'Int', description: 'Total configured panes' },
  { property: 'visiblePaneCount', type: 'Int', description: 'Currently visible panes' },
  { property: 'isExpanded', type: 'Boolean', description: 'Whether multi-pane mode is active' },
  { property: 'isTransitioning', type: 'Boolean', description: 'Whether pane transition is in progress' },
  { property: 'paneContents', type: 'List<PaneContent>', description: 'Content slots for custom layout rendering' },
];

export function ScopePropertiesTable({
  scopeType,
  properties,
}: ScopePropertiesTableProps) {
  const data = properties ?? (scopeType === 'tabs' ? TABS_SCOPE_PROPERTIES : PANE_SCOPE_PROPERTIES);
  const scopeName = scopeType === 'tabs' ? 'TabsContainerScope' : 'PaneContainerScope';

  return (
    <div className={styles.container}>
      <h4 className={styles.title}>{scopeName} Properties</h4>
      <table className={styles.scopeTable}>
        <thead>
          <tr>
            <th>Property</th>
            <th>Type</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          {data.map((prop) => (
            <tr key={prop.property}>
              <td><code>{prop.property}</code></td>
              <td><code>{prop.type}</code></td>
              <td>{prop.description}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
