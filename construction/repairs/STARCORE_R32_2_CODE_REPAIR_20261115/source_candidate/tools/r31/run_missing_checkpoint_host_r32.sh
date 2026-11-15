#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${R32_HOST_OUT:?fresh R32 host output required}"
: "${JAVA_HOME:?activate build environment}"
test ! -e "$R32_HOST_OUT"
mkdir -p "$R32_HOST_OUT/runtime" "$R32_HOST_OUT/tests"
r32_sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:?}}
r32_classes=${CLASSES:-app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes}
r32_junit=$(find "$HOME/.gradle/caches/modules-2/files-2.1/junit/junit/4.13.2" -name '*.jar' -type f | head -n 1)
r32_hamcrest=$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3" -name '*.jar' -type f | head -n 1)
test -s "$r32_junit"; test -s "$r32_hamcrest"
mapfile -t r32_json < <(find verification/r932/android-json -name '*.java' -type f | sort)
javac -encoding UTF-8 -d "$R32_HOST_OUT/runtime" "${r32_json[@]}" verification/canonical/adapters/android/util/AtomicFile.java tools/r31/storage_host/platform/android/util/Log.java
r32_cp="$R32_HOST_OUT/runtime:$r32_classes:$r32_sdk/platforms/android-36/android.jar:$r32_junit:$r32_hamcrest"
javac -encoding UTF-8 -proc:none -cp "$r32_cp" -d "$R32_HOST_OUT/tests" tools/r31/storage_host/r32_tests/*.java
java -cp "$R32_HOST_OUT/tests:$r32_cp" org.junit.runner.JUnitCore com.ethk3.novaonepro.R32FailureReceiptTest com.ethk3.novaonepro.R32CheckpointReportAuditTest | tee "$R32_HOST_OUT/junit.txt"
python3 - "$R32_HOST_OUT" <<'PY'
import json,subprocess,sys
from pathlib import Path
out=Path(sys.argv[1]);log=(out/'junit.txt').read_text();assert 'OK (7 tests)' in log,log
receipt={'status':'PASS','tests':7,'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'ACTUAL_PRODUCTION_CLASSES_WITH_REAL_HOST_JSON_AND_ATOMICFILE_TRANSPORT_NOT_ANDROID_OR_HANDSET','checks':['missing-root-no-mkdir','missing-corrupt-and-good-objects-observed','manifest-missing-child','cancel-and-symlink-refusal','failed-before-recovery-original-durable','bounded-UTF8-and-credential-redaction','legacy-journal-and-latest-failure-phase']}
(out/'R32_HOST_REGRESSION.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt))
PY
