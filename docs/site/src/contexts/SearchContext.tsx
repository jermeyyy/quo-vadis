import { createContext, useContext, useState, useEffect } from 'react'
import type { ReactNode } from 'react'

interface SearchResult {
  id: string
  title: string
  route: string
  content: string
}

interface SearchContextType {
  query: string
  results: SearchResult[]
  isOpen: boolean
  search: (q: string) => void
  openSearch: () => void
  closeSearch: () => void
}

const SearchContext = createContext<SearchContextType | undefined>(undefined)

export function SearchProvider({ children }: { children: ReactNode }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SearchResult[]>([])
  const [isOpen, setIsOpen] = useState(false)

  // Temporary mock data until we build the search index
  const mockSearchData = [
    {
      id: 'home',
      title: 'Home',
      route: '/',
      content: 'Quo Vadis is a type-safe, multiplatform navigation library for Compose Multiplatform'
    },
    {
      id: 'getting-started',
      title: 'Getting Started',
      route: '/getting-started',
      content: 'Learn how to install and configure Quo Vadis in your project'
    },
    {
      id: 'features',
      title: 'Features',
      route: '/features',
      content: 'Explore the annotation-based API and type-safe navigation features'
    },
    {
      id: 'demo',
      title: 'Demo',
      route: '/demo',
      content: 'See Quo Vadis in action with live examples'
    }
  ]

  const search = (q: string) => {
    setQuery(q)
    if (q.length < 2) {
      setResults([])
      return
    }

    // Simple mock search - will be replaced with FlexSearch
    const filtered = mockSearchData.filter(item =>
      item.title.toLowerCase().includes(q.toLowerCase()) ||
      item.content.toLowerCase().includes(q.toLowerCase())
    )
    setResults(filtered)
  }

  const openSearch = () => setIsOpen(true)
  const closeSearch = () => {
    setIsOpen(false)
    setQuery('')
    setResults([])
  }

  // Keyboard shortcut (Cmd/Ctrl + K)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        openSearch()
      }
      if (e.key === 'Escape' && isOpen) {
        closeSearch()
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen])

  return (
    <SearchContext.Provider value={{ query, results, isOpen, search, openSearch, closeSearch }}>
      {children}
    </SearchContext.Provider>
  )
}

export function useSearch() {
  const context = useContext(SearchContext)
  if (!context) throw new Error('useSearch must be used within SearchProvider')
  return context
}
