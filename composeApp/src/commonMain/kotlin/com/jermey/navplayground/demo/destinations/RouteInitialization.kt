package com.jermey.navplayground.demo.destinations

/**
 * Ensures all KSP-generated route registrations are initialized.
 * Call this before using any destinations.
 */
fun initializeRoutes() {
    // Reference all initializers to trigger their init blocks
    MainDestinationRouteInitializer
    MasterDetailDestinationRouteInitializer
    TabsDestinationRouteInitializer
    ProcessDestinationRouteInitializer
    
    // Test destinations
    com.jermey.navplayground.demo.test.TestGraphRouteInitializer
}
