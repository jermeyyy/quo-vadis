package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.runtime.Composable

/**
 * Platform-specific back input registration.
 *
 * Each platform implements this to hook into system back handling.
 * This ensures the NavigationEventDispatcher receives system back events
 * on platforms that support it.
 *
 * ## Platform Behavior
 *
 * - **Android**: The Activity provides NavigationEventDispatcher via
 *   `ComponentActivity.navigationEventDispatcher` (since Activity 1.12.0).
 *   No additional registration needed.
 *
 * - **iOS**: Back gesture handled via edge swipe gesture detection.
 *   Currently no-op; future implementation via `IOSEdgeSwipeGestureDetector`.
 *
 * - **Desktop**: No platform back gesture exists. Back is handled via
 *   keyboard shortcuts or UI buttons.
 *
 * - **JS/WASM**: No platform back gesture. Browser back button uses
 *   browser history API.
 */
@Composable
internal expect fun RegisterPlatformBackInput()
