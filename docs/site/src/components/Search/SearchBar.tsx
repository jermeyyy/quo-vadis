import { useSearch } from '@/contexts/SearchContext'
import styles from './SearchBar.module.css'

export default function SearchBar() {
  const { openSearch } = useSearch()

  return (
    <button className={styles.searchButton} onClick={openSearch}>
      <svg className={styles.icon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.35-4.35" />
      </svg>
      <span className={styles.placeholder}>Search...</span>
      <kbd className={styles.kbd}>⌘K</kbd>
    </button>
  )
}
