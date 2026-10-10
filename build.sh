#!/usr/bin/env bash
set -euo pipefail

mkdir -p build/classes
find src -name '*.java' -print0 | xargs -0 javac -d build/classes
