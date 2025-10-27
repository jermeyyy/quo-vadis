import { Link, useLocation } from 'react-router-dom'
import { navigationData } from '@/data/navigation'
import type { NavItem } from '@/data/navigation'
import styles from './MobileMenu.module.css'

interface MobileMenuProps {
  isOpen: boolean
  onClose: () => void
}

export default function MobileMenu({ isOpen, onClose }: MobileMenuProps) {
  const location = useLocation()

  const renderNavItem = (item: NavItem) => {
    const isActive = item.path === location.pathname

    return (
      <li key={item.label} className={styles.navItem}>
        {item.path ? (
          <Link
            to={item.path}
            className={`${styles.navLink} ${isActive ? styles.active : ''}`}
            onClick={onClose}
          >
            {item.label}
          </Link>
        ) : item.href ? (
          <a
            href={item.href}
            className={styles.navLink}
            target={item.external ? '_blank' : undefined}
            rel={item.external ? 'noopener noreferrer' : undefined}
            onClick={onClose}
          >
            {item.label}
            {item.external && <span className={styles.externalIcon}>↗</span>}
          </a>
        ) : null}
      </li>
    )
  }

  if (!isOpen) return null

  return (
    <>
      <div className={styles.overlay} onClick={onClose} />
      <div className={styles.menu}>
        <div className={styles.header}>
          <h2 className={styles.title}>Navigation</h2>
          <button className={styles.closeButton} onClick={onClose} aria-label="Close menu">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
        <nav className={styles.nav}>
          <ul className={styles.navList}>
            {navigationData.map(item => renderNavItem(item))}
          </ul>
        </nav>
      </div>
    </>
  )
}
