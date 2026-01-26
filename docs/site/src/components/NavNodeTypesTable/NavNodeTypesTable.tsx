import styles from './NavNodeTypesTable.module.css';

export interface NavNodeTypeData {
  type: string;
  purpose: string;
  contains: string;
}

export interface NavNodeTypesTableProps {
  /** Show expanded descriptions (default: false) */
  expanded?: boolean;
  /** Custom data (optional) */
  nodes?: NavNodeTypeData[];
}

const DEFAULT_NODES: NavNodeTypeData[] = [
  {
    type: 'ScreenNode',
    purpose: 'Leaf destination',
    contains: 'Destination data',
  },
  {
    type: 'StackNode',
    purpose: 'Linear navigation',
    contains: 'List of children (last = active)',
  },
  {
    type: 'TabNode',
    purpose: 'Parallel tabs',
    contains: 'List of StackNodes',
  },
  {
    type: 'PaneNode',
    purpose: 'Adaptive panes',
    contains: 'Map of PaneRole to configuration',
  },
];

export function NavNodeTypesTable({
  expanded: _expanded = false,
  nodes = DEFAULT_NODES,
}: NavNodeTypesTableProps) {
  return (
    <table className={styles.nodeTable}>
      <thead>
        <tr>
          <th>Type</th>
          <th>Purpose</th>
          <th>Contains</th>
        </tr>
      </thead>
      <tbody>
        {nodes.map((node) => (
          <tr key={node.type}>
            <td><code>{node.type}</code></td>
            <td>{node.purpose}</td>
            <td>{node.contains}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
