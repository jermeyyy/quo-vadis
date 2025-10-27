import styles from '../Features.module.css'

export default function Multiplatform() {
  return (
    <article className={styles.features}>
      <h1>Multiplatform Support</h1>
      <p className={styles.intro}>
        Truly multiplatform navigation that works identically across all supported platforms.
        Write your navigation logic once and deploy everywhere.
      </p>

      <section>
        <h2 id="platforms">Supported Platforms</h2>
        
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
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="/features/predictive-back">Predictive Back</a> - Platform-specific back navigation</li>
          <li><a href="/features/deep-links">Deep Links</a> - URL patterns and deep linking</li>
          <li><a href="/demo">See the demo</a> running on multiple platforms</li>
        </ul>
      </section>
    </article>
  )
}
