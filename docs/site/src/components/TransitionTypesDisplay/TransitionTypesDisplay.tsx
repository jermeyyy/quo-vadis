import styles from './TransitionTypesDisplay.module.css';

export interface TransitionTypeData {
  name: string;
  description: string;
  enter?: string;
  exit?: string;
  bestFor?: string;
}

export interface TransitionTypesDisplayProps {
  variant: 'grid' | 'table';
  /** Custom transitions to display (optional - uses defaults if not provided) */
  transitions?: TransitionTypeData[];
  /** Show enter/exit columns in table variant */
  showAnimationDetails?: boolean;
}

const DEFAULT_TRANSITIONS: TransitionTypeData[] = [
  {
    name: 'SlideHorizontal',
    description: 'Standard horizontal slide for hierarchical navigation',
    enter: 'Slide from right',
    exit: 'Slide to left',
    bestFor: 'Stack navigation (default)',
  },
  {
    name: 'SlideVertical',
    description: 'Vertical slide for modal presentations',
    enter: 'Slide from bottom',
    exit: 'Slide to top',
    bestFor: 'Modal sheets',
  },
  {
    name: 'Fade',
    description: 'Simple cross-fade between screens',
    enter: 'Fade in',
    exit: 'Fade out',
    bestFor: 'Tab switching, overlays',
  },
  {
    name: 'FadeThrough',
    description: 'Material Design fade through pattern',
    enter: 'Scale + fade in',
    exit: 'Scale + fade out',
    bestFor: 'Navigation rail, top-level changes',
  },
  {
    name: 'ScaleIn',
    description: 'Scale animation emphasizing detail views',
    enter: 'Scale from 80%',
    exit: 'Scale to 95%',
    bestFor: 'Detail view transitions',
  },
  {
    name: 'None',
    description: 'Instant navigation without animation',
    enter: 'Instant',
    exit: 'Instant',
    bestFor: 'Testing, custom animations',
  },
];

export function TransitionTypesDisplay({
  variant,
  transitions = DEFAULT_TRANSITIONS,
  showAnimationDetails = false,
}: TransitionTypesDisplayProps) {
  if (variant === 'grid') {
    return (
      <div className={styles.transitionGrid}>
        {transitions.map((transition) => (
          <div key={transition.name} className={styles.transitionCard}>
            <h4>{transition.name}</h4>
            <p>{transition.description}</p>
          </div>
        ))}
      </div>
    );
  }

  // Table variant
  return (
    <table className={styles.transitionTable}>
      <thead>
        <tr>
          <th>Transition</th>
          {showAnimationDetails && (
            <>
              <th>Enter</th>
              <th>Exit</th>
            </>
          )}
          <th>Best For</th>
        </tr>
      </thead>
      <tbody>
        {transitions.map((transition) => (
          <tr key={transition.name}>
            <td><code>{transition.name}</code></td>
            {showAnimationDetails && (
              <>
                <td>{transition.enter}</td>
                <td>{transition.exit}</td>
              </>
            )}
            <td>{transition.bestFor}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
