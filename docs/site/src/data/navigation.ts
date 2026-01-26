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
      { label: 'Core Concepts', path: '/features/core-concepts' },
      { label: 'Annotation-Based API', path: '/features/annotation-api' },
      { label: 'DSL Configuration', path: '/features/dsl-config' },
      { label: 'Type-Safe Navigation', path: '/features/type-safe' },
      { label: 'Multiplatform Support', path: '/features/multiplatform' },
      { label: 'Deep Links', path: '/features/deep-links' },
      { label: 'Predictive Back', path: '/features/predictive-back' },
      { label: 'Transitions & Animations', path: '/features/transitions' },
      { label: 'Tabbed Navigation', path: '/features/tabbed-navigation' },
      { label: 'Pane Layouts', path: '/features/pane-layouts' },
      {
        label: 'FlowMVI & Koin',
        children: [
          { label: 'Overview', path: '/features/di-integration' },
          { label: 'Core Concepts', path: '/features/di-integration/core-concepts' },
          { label: 'Usage Guide', path: '/features/di-integration/usage' },
        ]
      },
      { label: 'Testing Support', path: '/features/testing' },
      { label: 'Modular Architecture', path: '/features/modular' },
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
