#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
record_generated() {
  git add release/research_dependencies_r967.json release/r82_training_invalidation.json release/r982_source_calls.tsv.gz release/r982_source_calls.manifest.json
  if ! git diff --cached --quiet; then
    git -c user.name='STARCORE Build' -c user.email='starcore-build@users.noreply.github.com' commit -m "$1"
  fi
}
python3 scripts/prepare_strategy_su1_identity.py
record_generated 'Bind restored R13 source identities'
python3 scripts/test_r13_production_ddl.py
python3 scripts/verify_catalog_assets_cp12.py --check
python3 scripts/verify_strategy_su1.py
bash ./gradlew --no-daemon :app:compileDebugJavaWithJavac -x :app:generateR982SourceCallAsset
python3 scripts/r982_source_call_asset.py --freeze --classes app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes
python3 scripts/prepare_strategy_su1_identity.py
record_generated 'Bind compiled R13 call inventory'
bash ./gradlew --no-daemon :app:verifyCanonicalPlanBuildCP3 :app:assembleDebug :app:assembleDebugAndroidTest
git push origin HEAD:"${GITHUB_REF_NAME}"
