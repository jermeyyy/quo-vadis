import { useState } from 'react'
import type { ReactNode } from 'react'
import Navbar from './Navbar'
import Sidebar from './Sidebar'
import TableOfContents from './TableOfContents'
import MobileMenu from '../MobileMenu/MobileMenu'
import styles from './Layout.module.css'

interface LayoutProps {
  children: ReactNode
}

export default function Layout({ children }: LayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  return (
    <div className={styles.layout}>
      <Navbar 
        onMenuToggle={() => setMobileMenuOpen(!mobileMenuOpen)}
      />
      
      <div className={styles.mainContainer}>
        <Sidebar 
          isOpen={sidebarOpen}
          onToggle={() => setSidebarOpen(!sidebarOpen)}
        />
        
        <main className={styles.content}>
          {children}
        </main>
        
        <TableOfContents />
      </div>

      <MobileMenu 
        isOpen={mobileMenuOpen}
        onClose={() => setMobileMenuOpen(false)}
      />
    </div>
  )
}
