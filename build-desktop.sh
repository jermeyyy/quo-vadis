#!/bin/bash

# Desktop Build & Run Script for NavPlayground
# Usage: ./build-desktop.sh [command]

set -e

GRADLE="./gradlew"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function print_help() {
    echo -e "${BLUE}NavPlayground Desktop Build Script${NC}"
    echo ""
    echo "Usage: ./build-desktop.sh [command]"
    echo ""
    echo "Commands:"
    echo "  run              - Run desktop app in development mode"
    echo "  build            - Build composeApp JAR (library JAR, not executable)"
    echo "  dist             - Create distributable app bundle (use this for runnable app)"
    echo "  package          - Create native distribution (DMG/MSI/DEB)"
    echo "  package-dmg      - Create macOS DMG (macOS only)"
    echo "  package-msi      - Create Windows MSI (Windows only)"
    echo "  package-deb      - Create Linux DEB (Linux only)"
    echo "  clean            - Clean build artifacts"
    echo "  help             - Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./build-desktop.sh run           # Quick start (currently blocked by icons)"
    echo "  ./build-desktop.sh build         # Build composeApp JAR"
    echo "  ./build-desktop.sh dist          # Create runnable app bundle"
    echo ""
    echo "Note: Demo app currently blocked by Material Icons Extended incompatibility."
    echo "      The library (quo-vadis-core) works perfectly on desktop."
}

function run_app() {
    echo -e "${GREEN}Running desktop application...${NC}"
    echo -e "${YELLOW}Note: Currently has runtime issues (version conflicts or Material Icons)${NC}"
    echo -e "${YELLOW}See DESKTOP_STATUS.md for details and solutions${NC}"
    $GRADLE :composeApp:run --no-configuration-cache
}

function build_jar() {
    echo -e "${GREEN}Building composeApp desktop JAR...${NC}"
    echo -e "${YELLOW}Note: This creates a library JAR, not an executable. Use 'dist' for runnable app.${NC}"
    $GRADLE :composeApp:desktopJar
    echo -e "${GREEN}JAR built: composeApp/build/libs/composeApp-desktop.jar${NC}"
}

function build_distributable() {
    echo -e "${GREEN}Creating distributable app bundle...${NC}"
    $GRADLE :composeApp:createDistributable
    echo -e "${GREEN}App bundle created in: composeApp/build/compose/binaries/main/app/${NC}"
}

function package_all() {
    echo -e "${GREEN}Creating native distribution for current OS...${NC}"
    $GRADLE :composeApp:packageDistributionForCurrentOS
    echo -e "${GREEN}Distribution created in: composeApp/build/compose/binaries/main/${NC}"
}

function package_dmg() {
    echo -e "${GREEN}Creating macOS DMG...${NC}"
    $GRADLE :composeApp:packageDmg
    echo -e "${GREEN}DMG created in: composeApp/build/compose/binaries/main/dmg/${NC}"
}

function package_msi() {
    echo -e "${GREEN}Creating Windows MSI...${NC}"
    $GRADLE :composeApp:packageMsi
    echo -e "${GREEN}MSI created in: composeApp/build/compose/binaries/main/msi/${NC}"
}

function package_deb() {
    echo -e "${GREEN}Creating Linux DEB...${NC}"
    $GRADLE :composeApp:packageDeb
    echo -e "${GREEN}DEB created in: composeApp/build/compose/binaries/main/deb/${NC}"
}

function clean_build() {
    echo -e "${YELLOW}Cleaning build artifacts...${NC}"
    $GRADLE clean
    echo -e "${GREEN}Clean complete${NC}"
}

# Main script logic
case "${1:-help}" in
    run)
        run_app
        ;;
    build)
        build_jar
        ;;
    dist)
        build_distributable
        ;;
    package)
        package_all
        ;;
    package-dmg)
        package_dmg
        ;;
    package-msi)
        package_msi
        ;;
    package-deb)
        package_deb
        ;;
    clean)
        clean_build
        ;;
    help|*)
        print_help
        ;;
esac
