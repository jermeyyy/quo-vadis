// src/data/constants.ts

/**
 * Library version - single source of truth for version strings across documentation
 * Update this when releasing new versions
 */
export const LIBRARY_VERSION = '0.3.4';

/**
 * KSP version compatible with current library version
 */
export const KSP_VERSION = '2.3.0';

/**
 * Maven artifact coordinates
 */
export const MAVEN_ARTIFACTS = {
  core: 'io.github.jermeyyy:quo-vadis-core',
  annotations: 'io.github.jermeyyy:quo-vadis-annotations',
  ksp: 'io.github.jermeyyy:quo-vadis-ksp',
  flowMvi: 'io.github.jermeyyy:quo-vadis-core-flow-mvi',
} as const;

/**
 * Gradle plugin ID
 */
export const GRADLE_PLUGIN_ID = 'io.github.jermeyyy.quo-vadis';

/**
 * Repository URLs
 */
export const REPOSITORY_URLS = {
  github: 'https://github.com/jermeyyy/quo-vadis',
  mavenCentral: 'https://central.sonatype.com/artifact/io.github.jermeyyy/quo-vadis-core',
  apiDocs: '/quo-vadis/api/index.html',
} as const;

/**
 * Helper to generate full artifact coordinate with version
 */
export const getArtifactCoordinate = (artifact: keyof typeof MAVEN_ARTIFACTS): string => {
  return `${MAVEN_ARTIFACTS[artifact]}:${LIBRARY_VERSION}`;
};
