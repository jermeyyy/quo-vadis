export interface NavItem {
  label: string
  path?: string
  href?: string
  icon?: string
  children?: NavItem[]
  external?: boolean
}

export const navigationData: NavItem[] = [
  {
    label: 'Home',
    path: '/'
  },
  {
    label: 'Getting Started',
    path: '/getting-started'
  },
  {
    label: 'Features',
    children: [
      { label: 'Annotation-Based API', path: '/features/annotation-api' },
      { label: 'Type-Safe Navigation', path: '/features/type-safe' },
      { label: 'Multiplatform Support', path: '/features/multiplatform' },
      { label: 'BackStack Management', path: '/features/backstack' },
      { label: 'Deep Links', path: '/features/deep-links' },
      { label: 'Predictive Back', path: '/features/predictive-back' },
      { label: 'Shared Elements', path: '/features/shared-elements' },
      { label: 'Transitions & Animations', path: '/features/transitions' },
      { label: 'Tabbed Navigation', path: '/features/tabbed-navigation' },
      { label: 'FlowMVI Integration', path: '/features/flow-mvi' },
      { label: 'Testing Support', path: '/features/testing' },
      { label: 'MVI Concepts', path: '/features/mvi' },
      { label: 'Modular Architecture', path: '/features/modular' },
      { label: 'DI Integration', path: '/features/di-integration' },
      { label: 'Performance', path: '/features/performance' }
    ]
  },
  {
    label: 'Demo',
    path: '/demo'
  },
  {
    label: 'API Reference',
    href: '/quo-vadis/api/index.html',
    external: true
  },
  {
    label: 'GitHub',
    href: 'https://github.com/jermeyyy/quo-vadis',
    external: true,
    icon: 'github'
  }
]
