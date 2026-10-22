#!/usr/bin/env bash
set -euo pipefail
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TOTAL=$(CDPATH= cd -- "$ROOT/.." && pwd)
export STARCORE_BASE_JAVA_ROOT="$TOTAL/10_BASELINE_R924/02_COMPLETE_SOURCE/app/src/main/java/com/ethk3/novaonepro"
python "$ROOT/scripts/r925_build_from_signing_kit.py" \
 --base-apk "$TOTAL/05_INPUT_EVIDENCE/539_input.apk" \
 --certificate-base-apk "$TOTAL/07_ROLLBACK/Starcore-Tianshu-V57.20.8-R5.3.8-922-release.apk" \
 --signing-kit "$TOTAL/06_PRIVATE_SIGNING/STARCORE-V57-release-signing-kit.zip" \
 --toolchain "$TOTAL/08_TOOLCHAIN" --out "$ROOT/build/r925-release"
