import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import styles from './TableOfContents.module.css'

interface Heading {
  id: string
  text: string
  level: number
}

export default function TableOfContents() {
  const [headings, setHeadings] = useState<Heading[]>([])
  const [activeId, setActiveId] = useState<string>('')
  const location = useLocation()

  useEffect(() => {
    // Extract headings from page
    const elements = document.querySelectorAll('main h2, main h3')
    const extractedHeadings: Heading[] = []

    elements.forEach(element => {
      const id = element.id || element.textContent?.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '') || ''
      if (!element.id && id) {
        element.id = id
      }
      
      if (id) {
        extractedHeadings.push({
          id,
          text: element.textContent || '',
          level: parseInt(element.tagName[1])
        })
      }
    })

    setHeadings(extractedHeadings)
  }, [location.pathname, location.key])

  useEffect(() => {
    if (headings.length === 0) return

    // Scroll spy implementation
    const observer = new IntersectionObserver(
      entries => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            setActiveId(entry.target.id)
          }
        })
      },
      {
        rootMargin: '-80px 0px -80% 0px',
        threshold: 1.0
      }
    )

    headings.forEach(heading => {
      const element = document.getElementById(heading.id)
      if (element) observer.observe(element)
    })

    return () => observer.disconnect()
  }, [headings])

  if (headings.length === 0) return null

  const handleClick = (id: string) => {
    const element = document.getElementById(id)
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' })
      // Update URL hash without triggering scroll
      window.history.pushState(null, '', `#${id}`)
      setActiveId(id)
    }
  }

  return (
    <aside className={styles.toc}>
      <nav>
        <h3 className={styles.title}>On this page</h3>
        <ul className={styles.list}>
          {headings.map(heading => (
            <li
              key={heading.id}
              className={`${styles.item} ${styles[`level${heading.level}`]} ${
                activeId === heading.id ? styles.active : ''
              }`}
            >
              <a 
                href={`#${heading.id}`} 
                className={styles.link}
                onClick={(e) => {
                  e.preventDefault()
                  handleClick(heading.id)
                }}
              >
                {heading.text}
              </a>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  )
}
