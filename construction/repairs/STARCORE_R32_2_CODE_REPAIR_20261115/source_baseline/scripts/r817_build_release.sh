#!/usr/bin/env bash
# Canonical R817 signed-release entry point; delegates to the retained compatibility implementation.
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
exec "$ROOT/scripts/r815_build_release.sh" "$@"
