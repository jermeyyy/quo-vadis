import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { ThemeProvider } from '@/contexts/ThemeContext'
import { SearchProvider } from '@/contexts/SearchContext'
import Layout from '@components/Layout/Layout'
import SearchModal from '@components/Search/SearchModal'
import Home from '@pages/Home/Home'
import GettingStarted from '@pages/GettingStarted/GettingStarted'
import Demo from '@pages/Demo/Demo'

// Feature pages
import AnnotationAPI from '@pages/Features/AnnotationAPI/AnnotationAPI'
import TypeSafe from '@pages/Features/TypeSafe/TypeSafe'
import Multiplatform from '@pages/Features/Multiplatform/Multiplatform'
import DeepLinks from '@pages/Features/DeepLinks/DeepLinks'
import PredictiveBack from '@pages/Features/PredictiveBack/PredictiveBack'
import Transitions from '@pages/Features/Transitions/Transitions'
import Testing from '@pages/Features/Testing/Testing'
import Modular from '@pages/Features/Modular/Modular'
import DSLConfig from '@pages/Features/DSLConfig/DSLConfig'
import DIIntegration from '@pages/Features/DIIntegration/DIIntegration'
import DIIntegrationCoreConcepts from '@pages/Features/DIIntegration/CoreConcepts/CoreConcepts'
import DIIntegrationUsage from '@pages/Features/DIIntegration/Usage/Usage'
import TabbedNavigation from '@pages/Features/TabbedNavigation/TabbedNavigation'
import CoreConcepts from '@pages/Features/CoreConcepts/CoreConcepts'
import PaneLayouts from '@pages/Features/PaneLayouts/PaneLayouts'

function App() {
  const basename = import.meta.env.PROD ? '/quo-vadis' : '/'
  
  return (
    <ThemeProvider>
      <SearchProvider>
        <BrowserRouter basename={basename}>
          <Layout>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/getting-started" element={<GettingStarted />} />
              
              {/* Feature subpages */}
              <Route path="/features/annotation-api" element={<AnnotationAPI />} />
              <Route path="/features/type-safe" element={<TypeSafe />} />
              <Route path="/features/multiplatform" element={<Multiplatform />} />
              <Route path="/features/deep-links" element={<DeepLinks />} />
              <Route path="/features/predictive-back" element={<PredictiveBack />} />
              <Route path="/features/transitions" element={<Transitions />} />
              <Route path="/features/testing" element={<Testing />} />
              <Route path="/features/modular" element={<Modular />} />
              <Route path="/features/dsl-config" element={<DSLConfig />} />
              <Route path="/features/di-integration" element={<DIIntegration />} />
              <Route path="/features/di-integration/core-concepts" element={<DIIntegrationCoreConcepts />} />
              <Route path="/features/di-integration/usage" element={<DIIntegrationUsage />} />
              <Route path="/features/tabbed-navigation" element={<TabbedNavigation />} />
              <Route path="/features/pane-layouts" element={<PaneLayouts />} />
              <Route path="/features/core-concepts" element={<CoreConcepts />} />
              
              <Route path="/demo" element={<Demo />} />
              <Route path="*" element={<Home />} />
            </Routes>
          </Layout>
          <SearchModal />
        </BrowserRouter>
      </SearchProvider>
    </ThemeProvider>
  )
}

export default App
