#!/usr/bin/env bash
set -euo pipefail
need() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: $1 not found. Install $2" >&2
    exit 1
  fi
  echo "OK: $1 $($1 --version | head -n1)"
}
echo "Calyra prerequisites (Linux x64 target):"
need cmake "CMake 3.28+"
need clang "Clang 18+"
need dotnet ".NET SDK 8.0.408+"
need mvn "Maven 3.9+"
need java "JDK 21 (Temurin)"
# Optional: CEF download needs curl
if ! command -v curl >/dev/null 2>&1; then echo "WARN: curl missing — CEF download will fail"; fi
echo "All required tools present."
