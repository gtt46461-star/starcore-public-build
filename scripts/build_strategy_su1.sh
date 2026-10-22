#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/prepare_strategy_su1_identity.py
python3 scripts/verify_catalog_assets_cp12.py --check
python3 scripts/verify_strategy_su1.py
# The compiled call index is bound to actual Java class files. Bootstrap compilation
# intentionally precedes its refresh; the final normal assembly verifies that index.
bash ./gradlew --no-daemon :app:compileDebugJavaWithJavac -x :app:generateR982SourceCallAsset
python3 scripts/r982_source_call_asset.py --freeze --classes app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes
python3 scripts/prepare_strategy_su1_identity.py
bash ./gradlew --no-daemon --stacktrace :app:verifyCanonicalPlanBuildCP3 :app:assembleDebug :app:assembleDebugAndroidTest
