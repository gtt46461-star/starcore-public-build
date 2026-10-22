#!/usr/bin/env bash
# R816 public entry point for the complete host/static acceptance gate.
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
exec "$ROOT/scripts/r815_behavior_gate.sh" "$@"
