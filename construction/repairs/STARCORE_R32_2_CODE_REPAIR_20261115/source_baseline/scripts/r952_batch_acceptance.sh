#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
ANDROID_PLATFORM_JAR=${ANDROID_PLATFORM_JAR:-}
python3 scripts/r926_release_manifest.py --check
python3 scripts/r926_release_manifest.py --drift-test
python3 verification/r952/r952_contract.py
if [[ -n "$ANDROID_PLATFORM_JAR" && -f "$ANDROID_PLATFORM_JAR" ]]; then
  ANDROID_PLATFORM_JAR="$ANDROID_PLATFORM_JAR" bash scripts/r925_compile_host.sh
  OUT=build/r952-contract
  mkdir -p "$OUT"
  java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 17 -target 17 -cp "build/r925-host/compiled-classes:$ANDROID_PLATFORM_JAR" -d "$OUT" verification/r952/FullbloodContractHarness.java
  java -cp "$OUT:build/r925-host/compiled-classes:$ANDROID_PLATFORM_JAR" com.ethk3.novaonepro.r952.FullbloodContractHarness
else
  echo 'R952_ANDROID_COMPILE=NOT_RUN|ANDROID_PLATFORM_JAR_REQUIRED'
  exit 23
fi
python3 verification/r956/r956_source_contract.py
python3 scripts/r952_execution_first_regression.py
echo 'R952_BATCH_ACCEPTANCE=PASS'
