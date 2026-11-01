#!/usr/bin/env bash
# Original complete gates plus the repair's actual production-bytecode/native-SQLite regressions.
set -euo pipefail
cd "$(dirname "$0")/../.."
bash tools/r30/ci_build.sh
repair_log=evidence/r30-repair/ci
mkdir -p "$repair_log"
python3 - <<'PY'
import json,pathlib,subprocess,xml.etree.ElementTree as ET
files=sorted(pathlib.Path('app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'));assert files,'NO_ACTUAL_JUNIT_XML'
totals={k:sum(int(ET.parse(f).getroot().get(k,'0')) for f in files) for k in ('tests','failures','errors','skipped')}
assert totals['tests']>0 and totals['failures']==totals['errors']==totals['skipped']==0,totals
record={'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'source':'ACTUAL_GRADLE_JUNIT_XML','xmlSuites':len(files),**totals,'scope':'HOST_UNIT_NOT_ANDROID_PHYSICAL_OR_NATURAL'}
pathlib.Path('evidence/r30-repair/ci/unit-summary.json').write_text(json.dumps(record,indent=2)+'\n')
print('R30_REPAIR_ACTUAL_UNIT_SUMMARY '+json.dumps(record))
PY
repair_deps=$(mktemp -d)
trap 'rm -rf "$repair_deps"' EXIT
export SQLITE_JDBC="$repair_deps/sqlite-jdbc.jar"
curl -fLsS --connect-timeout 15 --max-time 90 -o "$SQLITE_JDBC" https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar
printf '%s  %s\n' 4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa "$SQLITE_JDBC" | sha256sum -c -
for check in diagnosis writer retention timeline commit_failure progress neural qualification input_pin matrix research observatory snapshot; do
  if ! python3 "tools/r30-repair/run_${check}_host.py" > "$repair_log/$check.log" 2>&1; then tail -n 80 "$repair_log/$check.log"; exit 1; fi
  echo "R30_REPAIR_NATIVE:$check:PASS"
done
echo R30_REPAIR_ORIGINAL_AND_ADDITIONAL_GATES_PASS
