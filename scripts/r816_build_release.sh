#!/usr/bin/env bash
# R816 public entry point for the clean Java17/D8, aligned and signed APK build.
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
exec "$ROOT/scripts/r815_build_release.sh" "$@"
