#!/bin/bash

# Java Formatting and Import Organization Script
# Использует Spotless Maven Plugin для:
# - Форматирования кода (Google Java Format)
# - Автоматической организации импортов
# - Удаления неиспользуемых импортов
#
# Usage: ./scripts/format-java.sh
# Example: ./scripts/format-java.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Get the base directory (project root)
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    print_info "Please install Maven: https://maven.apache.org/download.cgi"
    exit 1
fi

# Check if pom.xml exists
if [ ! -f "pom.xml" ]; then
    print_error "pom.xml not found in project root"
    exit 1
fi

print_info "Java Code Formatting and Import Organization Script"
print_info "Using Spotless Maven Plugin"
echo ""

print_step "Step 1: Formatting code with Google Java Format..."
if mvn spotless:apply -q; then
    print_info "✓ Code formatted successfully"
else
    print_error "Failed to format code"
    exit 1
fi

echo ""
print_step "Step 2: Verifying formatting..."
if mvn spotless:check -q; then
    print_info "✓ All files are properly formatted"
else
    print_warning "Some files may need additional formatting"
fi

echo ""
print_info "Formatting completed!"
print_info ""
print_info "What was done:"
print_info "  ✓ Code formatted according to Google Java Style"
print_info "  ✓ Imports organized in correct order (java → javax → org → com)"
print_info "  ✓ Unused imports removed"
print_info "  ✓ Trailing whitespace removed"
print_info "  ✓ Files end with newline"

