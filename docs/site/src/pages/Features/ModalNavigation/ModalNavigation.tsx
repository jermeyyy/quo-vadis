import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import {
  modalAnnotationBasic,
  modalAnnotationContainer,
  modalDSLConfig,
  modalScreenExample,
  modalNestedExample,
  modalCrossModuleExample,
} from '@data/codeExamples'
import styles from '../Features.module.css'

// Diagram styles following CoreConcepts/Multiplatform pattern
const diagramStyles = {
  container: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    gap: '0.5rem',
    padding: '2rem 1rem',
    fontFamily: 'var(--font-mono)',
    fontSize: '0.85rem',
  },
  outerBox: {
    width: '100%',
    maxWidth: '420px',
    borderRadius: '12px',
    padding: '1.25rem',
    border: '2px solid',
    position: 'relative' as const,
  },
  layerLabel: {
    fontSize: '0.7rem',
    fontWeight: '600' as const,
    textTransform: 'uppercase' as const,
    letterSpacing: '0.1em',
    marginBottom: '0.75rem',
    opacity: 0.7,
  },
  innerBox: {
    background: 'var(--color-bg-elevated)',
    borderRadius: '8px',
    padding: '0.75rem 1rem',
    textAlign: 'center' as const,
    border: '1px solid var(--color-border)',
    fontWeight: '500' as const,
  },
  note: {
    fontSize: '0.75rem',
    opacity: 0.7,
    fontStyle: 'italic' as const,
  },
  stackItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.5rem 0.75rem',
    borderRadius: '6px',
    fontSize: '0.8rem',
    fontWeight: '500' as const,
    border: '1px solid',
  },
  badge: {
    fontSize: '0.65rem',
    fontWeight: '600' as const,
    padding: '0.125rem 0.375rem',
    borderRadius: '4px',
    textTransform: 'uppercase' as const,
    letterSpacing: '0.05em',
  },
}

const DrawBehindDiagram = () => (
  <div style={diagramStyles.container}>
    {/* Outer Box = ModalContent */}
    <div style={{
      ...diagramStyles.outerBox,
      background: 'rgba(59, 130, 246, 0.06)',
      borderColor: 'rgba(59, 130, 246, 0.3)',
    }}>
      <div style={diagramStyles.layerLabel}>Box (ModalContent)</div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {/* Background layer */}
        <div style={{
          ...diagramStyles.innerBox,
          background: 'rgba(34, 197, 94, 0.1)',
          borderColor: 'rgba(34, 197, 94, 0.4)',
        }}>
          <div style={{ fontWeight: '600', marginBottom: '0.25rem' }}>Layer 0: Background</div>
          <div style={diagramStyles.note}>NavNodeRenderer(previousScreen)</div>
        </div>

        {/* Modal layer — overlapping via negative margin */}
        <div style={{
          ...diagramStyles.innerBox,
          background: 'rgba(139, 92, 246, 0.12)',
          borderColor: 'rgba(139, 92, 246, 0.5)',
          marginTop: '-0.25rem',
          marginLeft: '1.5rem',
          marginRight: '0.5rem',
          position: 'relative' as const,
        }}>
          <div style={{ fontWeight: '600', marginBottom: '0.25rem' }}>Layer 1: Modal</div>
          <div style={diagramStyles.note}>NavNodeRenderer(yourComposable)</div>
        </div>
      </div>

      <div style={{
        marginTop: '0.75rem',
        fontSize: '0.7rem',
        color: 'var(--color-text-muted)',
        textAlign: 'center' as const,
      }}>
        Both layers use StaticAnimatedVisibilityScope
      </div>
    </div>
  </div>
)

const NestedModalDiagram = () => (
  <div style={diagramStyles.container}>
    {/* Stack visualization */}
    <div style={{
      display: 'flex',
      gap: '0.5rem',
      flexWrap: 'wrap' as const,
      justifyContent: 'center',
      marginBottom: '0.5rem',
    }}>
      <div style={{
        ...diagramStyles.stackItem,
        background: 'rgba(34, 197, 94, 0.1)',
        borderColor: 'rgba(34, 197, 94, 0.4)',
      }}>
        Home
      </div>
      <span style={{ alignSelf: 'center', color: 'var(--color-text-muted)' }}>→</span>
      <div style={{
        ...diagramStyles.stackItem,
        background: 'rgba(234, 179, 8, 0.1)',
        borderColor: 'rgba(234, 179, 8, 0.4)',
      }}>
        Menu
        <span style={{
          ...diagramStyles.badge,
          background: 'rgba(234, 179, 8, 0.2)',
          color: '#ca8a04',
        }}>modal</span>
      </div>
      <span style={{ alignSelf: 'center', color: 'var(--color-text-muted)' }}>→</span>
      <div style={{
        ...diagramStyles.stackItem,
        background: 'rgba(139, 92, 246, 0.1)',
        borderColor: 'rgba(139, 92, 246, 0.4)',
      }}>
        Dialog
        <span style={{
          ...diagramStyles.badge,
          background: 'rgba(139, 92, 246, 0.2)',
          color: '#7c3aed',
        }}>modal</span>
      </div>
    </div>

    {/* findNonModalBaseIndex walkback */}
    <div style={{
      width: '100%',
      maxWidth: '420px',
      background: 'var(--color-bg-elevated)',
      borderRadius: '8px',
      padding: '0.75rem 1rem',
      border: '1px solid var(--color-border)',
      fontSize: '0.8rem',
      marginBottom: '0.5rem',
    }}>
      <div style={{ fontWeight: '600', marginBottom: '0.5rem', fontSize: '0.75rem', textTransform: 'uppercase' as const, letterSpacing: '0.05em', opacity: 0.7 }}>
        findNonModalBaseIndex
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
        <div>index 2 → Dialog → <span style={{ color: '#dc2626' }}>isModal = true</span> → skip</div>
        <div>index 1 → Menu → <span style={{ color: '#dc2626' }}>isModal = true</span> → skip</div>
        <div>index 0 → Home → <span style={{ color: '#16a34a', fontWeight: '600' }}>isModal = false → BASE</span></div>
      </div>
    </div>

    {/* Layered rendering result */}
    <div style={{
      ...diagramStyles.outerBox,
      background: 'rgba(59, 130, 246, 0.06)',
      borderColor: 'rgba(59, 130, 246, 0.3)',
    }}>
      <div style={diagramStyles.layerLabel}>Rendered Layers</div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
        {/* Base layer */}
        <div style={{
          ...diagramStyles.innerBox,
          background: 'rgba(34, 197, 94, 0.1)',
          borderColor: 'rgba(34, 197, 94, 0.4)',
        }}>
          <span style={{ fontWeight: '600' }}>Home</span>
          <span style={{ ...diagramStyles.note, marginLeft: '0.5rem' }}>(base)</span>
        </div>

        {/* Middle modal layer */}
        <div style={{
          ...diagramStyles.innerBox,
          background: 'rgba(234, 179, 8, 0.1)',
          borderColor: 'rgba(234, 179, 8, 0.4)',
          marginLeft: '1rem',
          marginRight: '0.25rem',
        }}>
          <span style={{ fontWeight: '600' }}>Menu</span>
          <span style={{ ...diagramStyles.note, marginLeft: '0.5rem' }}>(modal layer)</span>
        </div>

        {/* Top modal layer */}
        <div style={{
          ...diagramStyles.innerBox,
          background: 'rgba(139, 92, 246, 0.12)',
          borderColor: 'rgba(139, 92, 246, 0.5)',
          marginLeft: '2rem',
          marginRight: '0rem',
        }}>
          <span style={{ fontWeight: '600' }}>Dialog</span>
          <span style={{ ...diagramStyles.note, marginLeft: '0.5rem' }}>(top modal)</span>
        </div>
      </div>
    </div>
  </div>
)

export default function ModalNavigation() {
  return (
    <article className={styles.features}>
      <h1>Modal Navigation</h1>
      <p className={styles.intro}>
        Draw-behind rendering for bottom sheets, dialogs, and overlays.
        The library renders background + modal layers — you control all visual treatment.
      </p>

      <section>
        <h2 id="how-it-works">How It Works</h2>
        <p>
          When a modal destination is pushed onto a stack, <code>StackRenderer</code> checks
          the <code>ModalRegistry</code> and renders both layers in a <code>Box</code> via{' '}
          <code>ModalContent</code> instead of cross-fading between screens.
        </p>
        <DrawBehindDiagram />
      </section>

      <section>
        <h2 id="annotation-based">Annotation-Based (@Modal)</h2>
        <p>
          Add <code>@Modal</code> alongside <code>@Destination</code>, <code>@Tabs</code>,{' '}
          <code>@Stack</code>, or <code>@Pane</code>. No parameters — it's a marker annotation.
        </p>
        <h3>Modal Destination</h3>
        <CodeBlock code={modalAnnotationBasic} language="kotlin" />
        <h3>Modal Container</h3>
        <CodeBlock code={modalAnnotationContainer} language="kotlin" />
      </section>

      <section>
        <h2 id="dsl-based">DSL Configuration</h2>
        <p>
          Register modals manually with <code>modal&lt;D&gt;()</code> and{' '}
          <code>modalContainer("name")</code>:
        </p>
        <CodeBlock code={modalDSLConfig} language="kotlin" />
      </section>

      <section>
        <h2 id="custom-presentation">Custom Presentation</h2>
        <p>
          The library provides no scrim, chrome, or dismiss behavior.
          Your composable handles all visual treatment:
        </p>
        <CodeBlock code={modalScreenExample} language="kotlin" />
      </section>

      <section>
        <h2 id="nested-modals">Nested Modals</h2>
        <p>
          Multiple modals can stack. The renderer walks backwards to find the
          non-modal base, then renders all layers in order:
        </p>
        <CodeBlock code={modalNestedExample} language="kotlin" />
        <NestedModalDiagram />
      </section>

      <section>
        <h2 id="back-navigation">Dismissal</h2>
        <p>
          Modals are regular stack entries — dismiss with <code>navigator.navigateBack()</code>.
          Predictive back gestures (Android 13+, iOS swipe) work automatically.
        </p>
      </section>

      <section>
        <h2 id="cross-module">Cross-Module</h2>
        <p>
          Each module declares <code>@Modal</code> independently. Combined
          via <code>CompositeModalRegistry</code> when configs are merged with <code>+</code>.
        </p>
        <CodeBlock code={modalCrossModuleExample} language="kotlin" />
      </section>

      <section>
        <h2 id="next-steps">Next Steps</h2>
        <ul>
          <li><Link to="/features/annotation-api#modal-annotation">@Modal Annotation Reference</Link></li>
          <li><Link to="/features/dsl-config#modal-registry">Modal DSL Configuration</Link></li>
          <li><Link to="/features/transitions#modal-transitions">Modal Transitions</Link></li>
        </ul>
      </section>
    </article>
  )
}
