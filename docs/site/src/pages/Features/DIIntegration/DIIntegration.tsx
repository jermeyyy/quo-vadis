import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import styles from '../Features.module.css'

// Dependencies
const dependencyCode = `dependencies {
    implementation("io.github.jermeyyy:quo-vadis-core-flow-mvi:0.3.4")
}

// Transitively includes:
// - FlowMVI (pro.respawn.flowmvi)
// - Koin (io.insert-koin)
// - quo-vadis-core`

// Card styles for subpage links
const cardGridStyles = {
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
    gap: '1.5rem',
    marginTop: '1.5rem',
  },
  card: {
    display: 'block',
    padding: '1.5rem',
    borderRadius: '12px',
    background: 'var(--color-bg-elevated)',
    border: '1px solid var(--color-border)',
    textDecoration: 'none',
    color: 'inherit',
    transition: 'transform 0.2s ease, box-shadow 0.2s ease',
  },
  cardTitle: {
    fontSize: '1.1rem',
    fontWeight: 600,
    marginBottom: '0.5rem',
    color: 'var(--color-primary)',
  },
  cardDescription: {
    fontSize: '0.9rem',
    color: 'var(--color-text-secondary)',
    margin: 0,
  },
}

export default function DIIntegration() {
  return (
    <article className={styles.features}>
      <h1>FlowMVI & Koin Integration</h1>
      <p className={styles.intro}>
        The <code>quo-vadis-core-flow-mvi</code> module bridges{' '}
        <a href="https://github.com/respawn-app/FlowMVI" target="_blank" rel="noopener noreferrer">FlowMVI's</a>{' '}
        state management with navigation lifecycle, providing automatic container lifecycle management and Koin integration.
      </p>

      {/* Section: Overview */}
      <section>
        <h2 id="overview">Overview</h2>
        <p>
          This module provides a seamless integration between FlowMVI's MVI architecture and Quo Vadis navigation,
          allowing you to treat navigation as a side-effect of your business logic while maintaining proper lifecycle awareness.
        </p>
        <h3>Key Features</h3>
        <ul>
          <li><strong>Screen-scoped containers</strong> - MVI containers tied to individual screen lifecycle</li>
          <li><strong>Container-scoped shared state</strong> - Shared state across Tab or Pane navigation containers</li>
          <li><strong>Automatic lifecycle management</strong> - Containers are created and destroyed with navigation nodes</li>
          <li><strong>Koin integration</strong> - Scoped dependency injection with automatic cleanup</li>
          <li><strong>Type-safe navigation</strong> - Navigate from business logic with full type safety</li>
        </ul>
      </section>

      {/* Section: Dependencies */}
      <section>
        <h2 id="dependencies">Dependencies</h2>
        <p>
          Add the FlowMVI integration module to your project. This module transitively includes FlowMVI, Koin, and quo-vadis-core.
        </p>
        <CodeBlock code={dependencyCode} language="kotlin" />
      </section>

      {/* Section: Documentation */}
      <section>
        <h2 id="documentation">Documentation</h2>
        <p>
          Explore the detailed documentation for FlowMVI & Koin integration:
        </p>
        <div style={cardGridStyles.grid}>
          <Link to="/features/di-integration/core-concepts" style={cardGridStyles.card}>
            <div style={cardGridStyles.cardTitle}>Core Concepts</div>
            <p style={cardGridStyles.cardDescription}>
              MVI contract, container types, Koin setup, and lifecycle management.
            </p>
          </Link>
          <Link to="/features/di-integration/usage" style={cardGridStyles.card}>
            <div style={cardGridStyles.cardTitle}>Usage Guide</div>
            <p style={cardGridStyles.cardDescription}>
              rememberContainer, common patterns, multi-module setup, and best practices.
            </p>
          </Link>
        </div>
      </section>

      {/* Section: Next Steps */}
      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><a href="https://github.com/respawn-app/FlowMVI" target="_blank" rel="noopener noreferrer">FlowMVI Documentation</a> - Learn more about FlowMVI</li>
          <li><Link to="/features/tabbed-navigation">Tabbed Navigation</Link> - Use shared containers with tabs</li>
          <li><Link to="/features/modular">Modular Architecture</Link> - Structure your features</li>
          <li><Link to="/features/deep-links">Deep Links</Link> - Handle deep links with MVI</li>
          <li><Link to="/demo">Live Demo</Link> - See FlowMVI integration in action</li>
        </ul>
      </section>
    </article>
  )
}
