#!/usr/bin/env bash
# R816 compatibility entry point; the generated manifest is owned by current R817.
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
exec "$ROOT/scripts/r815_generate_provenance.sh" "$@"
