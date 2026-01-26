import styles from './PlatformSupportGrid.module.css';

export interface PlatformFeature {
  name: string;
  android: 'full' | 'partial' | 'none';
  ios: 'full' | 'partial' | 'none';
  desktop: 'full' | 'partial' | 'none';
  web: 'full' | 'partial' | 'none';
}

export interface PlatformCardData {
  platform: 'Android' | 'iOS' | 'Desktop' | 'Web';
  features: string[];
}

export interface PlatformSupportGridProps {
  variant: 'compact' | 'detailed' | 'cards';
  /** For 'compact' variant - feature matrix data */
  features?: PlatformFeature[];
  /** For 'cards' variant - platform card data */
  cards?: PlatformCardData[];
  /** For 'detailed' variant - show full platform capabilities */
  showRequirements?: boolean;
}

// Status icon components
const StatusIcon = ({ status }: { status: 'full' | 'partial' | 'none' }) => {
  const className = {
    full: styles.statusIconYes,
    partial: styles.statusIconPartial,
    none: styles.statusIconNa,
  }[status];
  return <span className={`${styles.statusIcon} ${className}`} />;
};

// Default platform features for compact view
const DEFAULT_FEATURES: PlatformFeature[] = [
  { name: 'Stack navigation', android: 'full', ios: 'full', desktop: 'full', web: 'full' },
  { name: 'Tab navigation', android: 'full', ios: 'full', desktop: 'full', web: 'full' },
  { name: 'Pane layouts', android: 'full', ios: 'full', desktop: 'full', web: 'full' },
  { name: 'Deep links', android: 'full', ios: 'full', desktop: 'partial', web: 'full' },
  { name: 'Predictive back', android: 'full', ios: 'full', desktop: 'none', web: 'none' },
  { name: 'Shared elements', android: 'full', ios: 'full', desktop: 'full', web: 'full' },
  { name: 'Browser history', android: 'none', ios: 'none', desktop: 'none', web: 'full' },
];

// Default platform cards
const DEFAULT_CARDS: PlatformCardData[] = [
  {
    platform: 'Android',
    features: [
      'System back button integration',
      'Predictive back gestures (Android 13+)',
      'Deep link support',
      'SavedStateHandle integration',
    ],
  },
  {
    platform: 'iOS',
    features: [
      'Native swipe gestures',
      'Predictive back animations',
      'Universal Links support',
      'Navigation bar integration',
    ],
  },
  {
    platform: 'Desktop',
    features: [
      'Keyboard shortcuts (Alt+Left/Right)',
      'Mouse button navigation',
      'Window state persistence',
      'All core features',
    ],
  },
  {
    platform: 'Web',
    features: [
      'Browser history integration',
      'URL routing',
      'Forward/back buttons',
      'Deep linking via URLs',
    ],
  },
];

export function PlatformSupportGrid({
  variant,
  features = DEFAULT_FEATURES,
  cards = DEFAULT_CARDS,
  showRequirements = false,
}: PlatformSupportGridProps) {
  if (variant === 'compact') {
    return (
      <table className={styles.compactTable}>
        <thead>
          <tr>
            <th>Feature</th>
            <th style={{ textAlign: 'center' }}>Android</th>
            <th style={{ textAlign: 'center' }}>iOS</th>
            <th style={{ textAlign: 'center' }}>Desktop</th>
            <th style={{ textAlign: 'center' }}>Web</th>
          </tr>
        </thead>
        <tbody>
          {features.map((feature) => (
            <tr key={feature.name}>
              <td className={styles.featureCell}>{feature.name}</td>
              <td><StatusIcon status={feature.android} /></td>
              <td><StatusIcon status={feature.ios} /></td>
              <td><StatusIcon status={feature.desktop} /></td>
              <td><StatusIcon status={feature.web} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  if (variant === 'cards') {
    return (
      <div className={styles.platformGrid}>
        {cards.map((card) => (
          <div key={card.platform} className={styles.platformCard}>
            <h3>{card.platform}</h3>
            <ul>
              {card.features.map((feature, idx) => (
                <li key={idx}>{feature}</li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    );
  }

  // 'detailed' variant - combines table with requirements
  return (
    <div className={styles.detailedGrid}>
      <table className={styles.detailedTable}>
        <thead>
          <tr>
            <th>Platform</th>
            <th>Targets</th>
            <th style={{ textAlign: 'center' }}>Status</th>
            {showRequirements && <th>Requirements</th>}
          </tr>
        </thead>
        <tbody>
          <tr>
            <td className={styles.featureCell}>Android</td>
            <td><span className={styles.platformBadge}>android</span></td>
            <td style={{ textAlign: 'center' }}><span className={styles.statusFull}>Full</span></td>
            {showRequirements && <td>API 21+</td>}
          </tr>
          <tr>
            <td className={styles.featureCell}>iOS</td>
            <td>
              <div className={styles.platformBadges}>
                <span className={styles.platformBadge}>iosArm64</span>
                <span className={styles.platformBadge}>iosSimulatorArm64</span>
                <span className={styles.platformBadge}>iosX64</span>
              </div>
            </td>
            <td style={{ textAlign: 'center' }}><span className={styles.statusFull}>Full</span></td>
            {showRequirements && <td>iOS 14+</td>}
          </tr>
          <tr>
            <td className={styles.featureCell}>Desktop</td>
            <td><span className={styles.platformBadge}>jvm</span></td>
            <td style={{ textAlign: 'center' }}><span className={styles.statusFull}>Full</span></td>
            {showRequirements && <td>macOS, Windows, Linux</td>}
          </tr>
          <tr>
            <td className={styles.featureCell}>JavaScript</td>
            <td><span className={styles.platformBadge}>js (IR)</span></td>
            <td style={{ textAlign: 'center' }}><span className={styles.statusFull}>Full</span></td>
            {showRequirements && <td>Modern browsers</td>}
          </tr>
          <tr>
            <td className={styles.featureCell}>WebAssembly</td>
            <td><span className={styles.platformBadge}>wasmJs</span></td>
            <td style={{ textAlign: 'center' }}><span className={styles.statusFull}>Full</span></td>
            {showRequirements && <td>WASM-compatible browsers</td>}
          </tr>
        </tbody>
      </table>
    </div>
  );
}
