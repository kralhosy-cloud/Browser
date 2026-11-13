#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
echo "== Calyra build-all (Linux x64) =="
echo "Checking prereqs..."
bash "$ROOT/scripts/check-prereqs.sh"

echo "== C policy module =="
cmake -S "$ROOT/src/c-policy" -B "$ROOT/build/c-policy" -DCMAKE_BUILD_TYPE=Release
cmake --build "$ROOT/build/c-policy"
ctest --test-dir "$ROOT/build/c-policy" --output-on-failure

echo "== Native core (C++) =="
cmake -S "$ROOT/src/native-core" -B "$ROOT/build/native-core" -DCMAKE_BUILD_TYPE=Release -DCMAKE_PREFIX_PATH="$ROOT/src/c-policy/include"
cmake --build "$ROOT/build/native-core"
ctest --test-dir "$ROOT/build/native-core" --output-on-failure

if command -v dotnet >/dev/null 2>&1; then
  echo "== .NET UI =="
  dotnet build "$ROOT/src/calyra-ui/Calyra.sln" -c Release
else
  echo "WARN: dotnet not found — skipping .NET build (CI’da kurulmalı)"
fi

if command -v mvn >/dev/null 2>&1; then
  echo "== Java helper =="
  mvn -f "$ROOT/src/calyra-helper/pom.xml" verify
else
  echo "WARN: mvn not found — skipping Java build"
fi

echo "Build complete. Artifacts: build/"
