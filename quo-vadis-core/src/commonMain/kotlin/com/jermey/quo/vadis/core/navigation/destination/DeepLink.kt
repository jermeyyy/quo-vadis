package com.jermey.quo.vadis.core.navigation.destination

/**
 * Represents a deep link for navigation.
 *
 * Supports URI-based and custom deep linking schemes with proper separation
 * of scheme, path, path parameters, and query parameters.
 *
 * @param scheme The URI scheme (e.g., "app", "myapp", "https")
 * @param path The path portion of the URI without leading slash (e.g., "profile/123")
 * @param pathParams Parameters extracted from path segments by the registry during matching
 * @param queryParams Query string parameters from the URI
 */
data class DeepLink(
    val scheme: String,
    val path: String,
    val pathParams: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap()
) {
    /**
     * All parameters merged, with path params taking precedence over query params.
     */
    val allParams: Map<String, String>
        get() = queryParams + pathParams

    /**
     * Reconstruct the URI from components.
     *
     * This provides backward compatibility with code expecting a `uri` property.
     */
    val uri: String
        get() = buildString {
            append(scheme)
            append("://")
            append(path)
            if (queryParams.isNotEmpty()) {
                append("?")
                append(queryParams.entries.joinToString("&") { "${it.key}=${it.value}" })
            }
        }

    companion object {
        /**
         * Parse a URI string into a DeepLink.
         *
         * Extracts scheme, path, and query parameters from the URI.
         * Path parameters are populated by the registry during matching.
         *
         * @param uri The URI string to parse (e.g., "app://profile/123?ref=email")
         * @return Parsed DeepLink instance
         */
        fun parse(uri: String): DeepLink {
            val schemeEnd = uri.indexOf("://")
            val scheme = if (schemeEnd >= 0) uri.substring(0, schemeEnd) else "app"

            val pathStart = if (schemeEnd >= 0) schemeEnd + 3 else 0
            val queryStart = uri.indexOf("?", pathStart)

            val path = if (queryStart >= 0) {
                uri.substring(pathStart, queryStart)
            } else {
                uri.substring(pathStart)
            }.trimStart('/')

            val queryParams = if (queryStart >= 0) {
                parseQueryParams(uri.substring(queryStart + 1))
            } else {
                emptyMap()
            }

            return DeepLink(
                scheme = scheme,
                path = path,
                pathParams = emptyMap(), // Populated by registry during matching
                queryParams = queryParams
            )
        }

        private fun parseQueryParams(query: String): Map<String, String> {
            return query.split("&")
                .mapNotNull { param ->
                    val parts = param.split("=")
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toMap()
        }
    }
}
