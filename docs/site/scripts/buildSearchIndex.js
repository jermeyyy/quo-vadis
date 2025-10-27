import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// Extract content from source files
const pagesDir = path.join(__dirname, '../src/pages')

function extractSearchData(dirPath, basePath = '') {
  const searchData = []
  
  try {
    const entries = fs.readdirSync(dirPath, { withFileTypes: true })

    for (const entry of entries) {
      const fullPath = path.join(dirPath, entry.name)
      
      if (entry.isDirectory()) {
        searchData.push(...extractSearchData(fullPath, `${basePath}/${entry.name}`))
      } else if (entry.name.endsWith('.tsx') && !entry.name.endsWith('.module.css')) {
        try {
          const content = fs.readFileSync(fullPath, 'utf-8')
          
          // Extract text content from JSX
          // Remove imports, comments, and JSX tags to get readable text
          const cleanContent = content
            .replace(/import\s+.*?from\s+['"].*?['"]/g, '')
            .replace(/\/\*[\s\S]*?\*\//g, '')
            .replace(/\/\/.*/g, '')
            .replace(/<[^>]+>/g, ' ')
            .replace(/\s+/g, ' ')
            .trim()

          // Extract text that looks like documentation content
          const textMatches = content.match(/['"]([^'"]{20,})['"]/g) || []
          const text = textMatches
            .map(match => match.slice(1, -1))
            .filter(t => t.length > 20 && !t.includes('import') && !t.includes('className'))
            .join(' ')

          if (text.length > 50 || cleanContent.length > 100) {
            const pageName = entry.name.replace('.tsx', '')
            const route = basePath + (pageName.toLowerCase() === 'home' ? '' : `/${pageName.toLowerCase()}`)
            
            searchData.push({
              id: route.replace(/^\//, '') || 'home',
              title: pageName.replace(/([A-Z])/g, ' $1').trim(),
              route: route || '/',
              content: text || cleanContent.slice(0, 500)
            })
          }
        } catch (err) {
          console.warn(`Warning: Could not read file ${fullPath}:`, err.message)
        }
      }
    }
  } catch (err) {
    console.error(`Error reading directory ${dirPath}:`, err.message)
  }

  return searchData
}

try {
  console.log('Building search index...')
  const searchData = extractSearchData(pagesDir)
  const outputPath = path.join(__dirname, '../src/data/searchData.json')

  // Ensure data directory exists
  const dataDir = path.dirname(outputPath)
  if (!fs.existsSync(dataDir)) {
    fs.mkdirSync(dataDir, { recursive: true })
  }

  fs.writeFileSync(outputPath, JSON.stringify(searchData, null, 2))
  console.log(`✓ Search index built successfully with ${searchData.length} pages`)
  console.log(`  Output: ${outputPath}`)
} catch (err) {
  console.error('✗ Failed to build search index:', err.message)
  process.exit(1)
}
