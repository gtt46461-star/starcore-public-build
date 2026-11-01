#!/usr/bin/env bash
# Original complete gates plus the repair's actual production-bytecode/native-SQLite regressions.
set -euo pipefail
cd "$(dirname "$0")/../.."
bash tools/r30/ci_build.sh
repair_log=evidence/r30-repair/ci
mkdir -p "$repair_log"
repair_deps=$(mktemp -d)
trap 'rm -rf "$repair_deps"' EXIT
export SQLITE_JDBC="$repair_deps/sqlite-jdbc.jar"
curl -fLsS --connect-timeout 15 --max-time 90 -o "$SQLITE_JDBC" https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar
printf '%s  %s\n' 4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa "$SQLITE_JDBC" | sha256sum -c -
for check in diagnosis writer retention timeline commit_failure progress neural qualification input_pin matrix research observatory snapshot; do
  python3 "tools/r30-repair/run_${check}_host.py" > "$repair_log/$check.log" 2>&1
  echo "R30_REPAIR_NATIVE:$check:PASS"
done
echo R30_REPAIR_ORIGINAL_AND_ADDITIONAL_GATES_PASS
