import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSearch } from '@/contexts/SearchContext'
import styles from './SearchModal.module.css'

export default function SearchModal() {
  const { query, results, isOpen, search, closeSearch } = useSearch()
  const navigate = useNavigate()
  const inputRef = useRef<HTMLInputElement>(null)
  const [selectedIndex, setSelectedIndex] = useState(0)

  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus()
    }
  }, [isOpen])

  useEffect(() => {
    setSelectedIndex(0)
  }, [results])

  useEffect(() => {
    if (!isOpen) return

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowDown') {
        e.preventDefault()
        setSelectedIndex(prev => Math.min(prev + 1, results.length - 1))
      } else if (e.key === 'ArrowUp') {
        e.preventDefault()
        setSelectedIndex(prev => Math.max(prev - 1, 0))
      } else if (e.key === 'Enter' && results[selectedIndex]) {
        e.preventDefault()
        handleResultClick(results[selectedIndex].route)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, results, selectedIndex])

  const handleResultClick = (route: string) => {
    navigate(route)
    closeSearch()
  }

  if (!isOpen) return null

  return (
    <div className={styles.overlay} onClick={closeSearch}>
      <div className={styles.modal} onClick={e => e.stopPropagation()}>
        <div className={styles.searchBox}>
          <svg className={styles.searchIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
          <input
            ref={inputRef}
            type="text"
            className={styles.input}
            placeholder="Search documentation..."
            value={query}
            onChange={e => search(e.target.value)}
          />
          <button className={styles.closeButton} onClick={closeSearch} aria-label="Close search">
            <kbd>ESC</kbd>
          </button>
        </div>

        <div className={styles.results}>
          {query.length > 0 && query.length < 2 && (
            <div className={styles.hint}>Type at least 2 characters to search...</div>
          )}
          {results.length === 0 && query.length >= 2 && (
            <div className={styles.noResults}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              <p>No results found for "{query}"</p>
            </div>
          )}
          {results.map((result, index) => (
            <button
              key={result.id}
              className={`${styles.resultItem} ${index === selectedIndex ? styles.selected : ''}`}
              onClick={() => handleResultClick(result.route)}
              onMouseEnter={() => setSelectedIndex(index)}
            >
              <div className={styles.resultIcon}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M9 12h6M9 16h6M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9l-7-7z" />
                  <path d="M13 2v7h7" />
                </svg>
              </div>
              <div className={styles.resultContent}>
                <div className={styles.resultTitle}>{result.title}</div>
                <div className={styles.resultText}>
                  {result.content.slice(0, 150)}...
                </div>
              </div>
              <div className={styles.resultAction}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="9 10 4 15 9 20" />
                  <path d="M20 4v7a4 4 0 0 1-4 4H4" />
                </svg>
              </div>
            </button>
          ))}
        </div>

        {results.length > 0 && (
          <div className={styles.footer}>
            <div className={styles.footerHint}>
              <kbd>↑</kbd>
              <kbd>↓</kbd>
              <span>to navigate</span>
              <kbd>↵</kbd>
              <span>to select</span>
              <kbd>ESC</kbd>
              <span>to close</span>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
