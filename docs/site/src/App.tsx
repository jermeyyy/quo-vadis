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
import BackStack from '@pages/Features/BackStack/BackStack'
import DeepLinks from '@pages/Features/DeepLinks/DeepLinks'
import PredictiveBack from '@pages/Features/PredictiveBack/PredictiveBack'
import SharedElements from '@pages/Features/SharedElements/SharedElements'
import Transitions from '@pages/Features/Transitions/Transitions'
import Testing from '@pages/Features/Testing/Testing'
import MVI from '@pages/Features/MVI/MVI'
import Modular from '@pages/Features/Modular/Modular'
import DIIntegration from '@pages/Features/DIIntegration/DIIntegration'
import Performance from '@pages/Features/Performance/Performance'

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
              <Route path="/features/backstack" element={<BackStack />} />
              <Route path="/features/deep-links" element={<DeepLinks />} />
              <Route path="/features/predictive-back" element={<PredictiveBack />} />
              <Route path="/features/shared-elements" element={<SharedElements />} />
              <Route path="/features/transitions" element={<Transitions />} />
              <Route path="/features/testing" element={<Testing />} />
              <Route path="/features/mvi" element={<MVI />} />
              <Route path="/features/modular" element={<Modular />} />
              <Route path="/features/di-integration" element={<DIIntegration />} />
              <Route path="/features/performance" element={<Performance />} />
              
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
