#!/usr/bin/env bash
# R29 CI build: bind identities to the committed source, run every existing suite, assemble and receipt the APKs.
# Requires ANDROID_HOME with platforms;android-36, build-tools;36.0.0, ndk;28.2.13676358, cmake;3.22.1.
# Signing comes from the environment (R13_SIGNING_P12/R13_SIGNING_PASSWORD/R13_EXPECTED_CERT_SHA256, R29_SIGNING).
# Without a signing identity the build still binds identities and runs all tests, and reports APK_NOT_BUILT.
#   PUSH_REF=<branch>  push the generated identity commit to that branch (CI only)
set -euo pipefail
cd "$(dirname "$0")/../.."
LOG=evidence/r30/ci; mkdir -p "$LOG"
commit_generated() {
  git add release/ app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java
  # The two generated identity files are tracked but live under the ignored app/build/: git refuses a plain add there.
  git add -f app/build/generated/r967/semantic_manifest.json app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java
  if ! git diff --cached --quiet; then
    git -c user.name='STARCORE build identity' -c user.email='starcore-build@users.noreply.github.com' commit -q -m "$1"
  fi
}
python3 tools/r29/rebind_identity.py | tail -n 3
commit_generated 'R29: bind generated identities to reviewed implementation bytes'
# The compiled call index is bound to actual class files: bootstrap compile, then refreeze and re-pin.
./gradlew --no-daemon -q :app:compileDebugJavaWithJavac -x :app:generateR982SourceCallAsset 2>&1 | tail -n 40 | tee "$LOG/bootstrap_compile.log"
python3 tools/r29/rebind_identity.py --classes app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes | tail -n 3
commit_generated 'R29: bind compiled call inventory'
git diff --exit-code -- app/src/main release app/build/generated
if [ -n "${PUSH_REF:-}" ]; then timeout 45s git push origin "HEAD:$PUSH_REF"; fi
python3 scripts/r926_release_manifest.py --check
python3 scripts/r967_semantic_identity.py --check
python3 scripts/test_r13_production_ddl.py
python3 scripts/generate_strategy_su2_schema.py --check
python3 scripts/verify_strategy_su1.py > "$LOG/su1.log"
python3 scripts/verify_plan1025.py > "$LOG/plan1025.log"
python3 scripts/verify_r26.py > "$LOG/r26.log"
python3 tools/r29/host_checks.py --out "$LOG/r29_host_checks.json"
bash tools/r30/ci_build_r30.sh "$LOG"
./gradlew --no-daemon :app:testDebugUnitTest 2>&1 | tee "$LOG/unit.log" | tail -n 30
# R29-010/035: contiguous qualification + late/out-of-order settlement on a real SQLite engine.
JDBC=$(mktemp -d)/sqlite-jdbc.jar
curl -fLsS --connect-timeout 15 --max-time 90 -o "$JDBC" https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar
echo "4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa  $JDBC" | sha256sum -c -
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes SQLITE_JDBC="$JDBC" OUT="$LOG" bash tools/r29/run_qual_host.sh
# R30-B01-04-a: chunked settlement equals the pre-R30 batch; process death between chunks resumes exactly once.
T30=$(mktemp -d); javac -nowarn -encoding UTF-8 -proc:none -d "$T30/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes" -d "$T30/test" tools/r29/qual_host/QualInboxR29HostTest.java tools/r30/qual_host/SettleChunkR30HostTest.java
java -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC" com.ethk3.novaonepro.strategyv2.SettleChunkR30HostTest | tee "$LOG/r30_settle_chunk_host.log"
# R30-B02-03-a: missing draw backfilled later settles the original freeze once; recheck backoff; STALE kept.
javac -nowarn -encoding UTF-8 -proc:none -cp "$T30/shim:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes" -d "$T30/test" tools/r29/qual_host/QualInboxR29HostTest.java tools/r30/qual_host/PendingDrawR30HostTest.java
java -cp "$T30/shim:$T30/test:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes:$JDBC" com.ethk3.novaonepro.strategyv2.PendingDrawR30HostTest | tee "$LOG/r30_pending_draw_host.log"
if [ -z "${R13_SIGNING_P12:-}" ]; then
  echo "APK_NOT_BUILT: no existing signing identity reachable from this branch" | tee "$LOG/apk_status.txt"
  exit 0
fi
REL=''
if [ -n "${DUOQU_API_KEY:-}" ]; then REL=':app:assembleRelease'; else echo 'RELEASE_NOT_BUILT: no API credential' | tee "$LOG/release_status.txt"; fi
./gradlew --no-daemon :app:verifyCanonicalPlanBuildCP3 $REL :app:assembleDebug 2>&1 | tee "$LOG/assemble.log" | tail -n 40
python3 tools/r29/package_receipt.py
