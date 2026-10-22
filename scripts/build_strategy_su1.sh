#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
record_generated() {
    git add release/research_dependencies_r967.json release/r82_training_invalidation.json release/r982_source_calls.tsv.gz release/r982_source_calls.manifest.json
    if ! git diff --cached --quiet; then
        git -c user.name='STARCORE Build' -c user.email='starcore-build@users.noreply.github.com' commit -m "$1"
    fi
}
python3 scripts/r926_release_manifest.py --check
python3 scripts/prepare_strategy_su1_identity.py
record_generated 'Record generated SU1 source identities'
python3 scripts/verify_catalog_assets_cp12.py --check
python3 scripts/verify_strategy_su1.py
# The compiled call index is bound to actual Java class files. Bootstrap compilation
# intentionally precedes its refresh; the final normal assembly verifies that index.
bash ./gradlew --no-daemon :app:compileDebugJavaWithJavac -x :app:generateR982SourceCallAsset
python3 scripts/r982_source_call_asset.py --freeze --classes app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes
python3 scripts/prepare_strategy_su1_identity.py
record_generated 'Record call index from compiled SU1 production classes'
build_status=0
bash ./gradlew --no-daemon --stacktrace :app:verifyCanonicalPlanBuildCP3 :app:assembleDebug :app:assembleDebugAndroidTest || build_status=$?
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
for report in Path('app/build/reports').glob('lint-results-*.xml'):
    for issue in ET.parse(report).getroot().findall('issue'):
        if issue.get('severity') in ('Error','Fatal'):
            location=issue.find('location')
            print('LINT_BLOCKER',issue.get('id'),issue.get('message'),location.attrib if location is not None else {},flush=True)
PY
if [[ "$build_status" != 0 ]]; then exit "$build_status"; fi
if [[ "${GITHUB_ACTIONS:-false}" == true ]]; then
    git push origin HEAD:construction/su1-20261022
fi
