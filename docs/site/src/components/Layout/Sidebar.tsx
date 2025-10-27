import { useState, useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { navigationData } from '@/data/navigation'
import type { NavItem } from '@/data/navigation'
import styles from './Sidebar.module.css'

interface SidebarProps {
  isOpen: boolean
  onToggle: () => void
}

export default function Sidebar({ isOpen, onToggle }: SidebarProps) {
  const location = useLocation()
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set(['Features']))

  useEffect(() => {
    // Auto-expand parent if on child page
    navigationData.forEach(item => {
      if (item.children) {
        const hasActiveChild = item.children.some(
          child => child.path && location.pathname.startsWith(child.path.split('#')[0])
        )
        if (hasActiveChild) {
          setExpandedItems(prev => new Set(prev).add(item.label))
        }
      }
    })
  }, [location.pathname])

  const toggleItem = (label: string) => {
    setExpandedItems(prev => {
      const next = new Set(prev)
      if (next.has(label)) {
        next.delete(label)
      } else {
        next.add(label)
      }
      return next
    })
  }

  const renderNavItem = (item: NavItem, level = 0) => {
    const hasChildren = item.children && item.children.length > 0
    const isExpanded = expandedItems.has(item.label)
    const isActive = item.path === location.pathname
    const isHashActive = item.path?.includes('#') && location.pathname + location.hash === item.path

    return (
      <li key={item.label} className={styles.navItem}>
        {hasChildren ? (
          <>
            <button
              className={`${styles.navButton} ${isActive ? styles.active : ''}`}
              onClick={() => toggleItem(item.label)}
              style={{ paddingLeft: `${level * 1 + 1}rem` }}
            >
              <span>{item.label}</span>
              <span className={`${styles.arrow} ${isExpanded ? styles.expanded : ''}`}>
                ▶
              </span>
            </button>
            {isExpanded && (
              <ul className={styles.subMenu}>
                {item.children!.map(child => renderNavItem(child, level + 1))}
              </ul>
            )}
          </>
        ) : item.path ? (
          <Link
            to={item.path}
            className={`${styles.navLink} ${isActive || isHashActive ? styles.active : ''}`}
            style={{ paddingLeft: `${level * 1 + 1}rem` }}
            onClick={() => {
              // Close mobile sidebar on navigation
              if (window.innerWidth < 768) {
                onToggle()
              }
            }}
          >
            {item.label}
          </Link>
        ) : item.href ? (
          <a
            href={item.href}
            className={styles.navLink}
            target={item.external ? '_blank' : undefined}
            rel={item.external ? 'noopener noreferrer' : undefined}
            style={{ paddingLeft: `${level * 1 + 1}rem` }}
          >
            {item.label}
            {item.external && <span className={styles.externalIcon}>↗</span>}
          </a>
        ) : null}
      </li>
    )
  }

  return (
    <>
      <aside className={`${styles.sidebar} ${isOpen ? styles.open : ''}`}>
        <nav className={styles.nav}>
          <ul className={styles.navList}>
            {navigationData.map(item => renderNavItem(item))}
          </ul>
        </nav>
      </aside>
      {isOpen && <div className={styles.overlay} onClick={onToggle} />}
    </>
  )
}
