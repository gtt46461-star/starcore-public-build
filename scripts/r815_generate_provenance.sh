#!/usr/bin/env bash
# Compatibility entry point; all output stays under app/build/generated.
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
exec python3 "$ROOT/scripts/r815_generate_provenance.py" "$@"
