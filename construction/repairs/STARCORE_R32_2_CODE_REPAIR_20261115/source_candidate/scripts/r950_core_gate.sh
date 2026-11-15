#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
python3 scripts/r950_research_continuity_regression.py
bash scripts/r926_batch2_gate.sh
ANDROID_PLATFORM_JAR="${ANDROID_PLATFORM_JAR:-/mnt/data/starcore_r949/toolchain/android.jar}" python3 scripts/r930_batch34_sql_harness.py
python3 scripts/r947_next_roster_harness.py
printf '%s\n' 'R950_CORE_GATE=PASS|scope=SOURCE_REGRESSION+BATCH2+BATCH34_SQL+NEXT_ROSTER'
