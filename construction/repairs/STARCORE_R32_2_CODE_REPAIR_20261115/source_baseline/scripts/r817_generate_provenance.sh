#!/usr/bin/env bash
# Canonical R817 provenance entry point; the implementation remains source-compatible.
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
exec bash "$ROOT/scripts/r815_generate_provenance.sh" "$@"
