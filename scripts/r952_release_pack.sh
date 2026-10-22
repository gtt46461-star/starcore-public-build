#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
: "${ANDROID_PLATFORM_JAR:?ANDROID_PLATFORM_JAR is required}"
: "${R952_APK:?R952_APK is required}"
# A host contract test is never a device/Future acceptance gate. No final-labelled artifact
# may be emitted from a partial checkpoint, a dirty checkout or an unbound APK.
python3 - "$R952_APK" <<'PY_FINAL_GATE'
import csv, hashlib, json, subprocess, sys
from pathlib import Path
status=json.loads(Path('construction/FINAL_GATE_STATUS.json').read_text())
rows=list(csv.DictReader(Path('construction/r958/72_TASK_STATUS.csv').open(encoding='utf-8-sig')))
apk=Path(sys.argv[1])
fail=[]
if status.get('overall')!='PASS_ALL_REQUIRED_GATES':fail.append('FINAL_GATE_STATUS_NOT_PASS')
if len(rows)!=72 or any(r.get('current_status',r.get('R958_status'))!='PASS_IN_FULL' for r in rows):fail.append('72_TASKS_NOT_ALL_VERIFIED')
if not apk.is_file() or status.get('verified_apk_sha256')!=hashlib.sha256(apk.read_bytes()).hexdigest():fail.append('APK_NOT_BOUND_TO_ACCEPTANCE')
head=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
if status.get('verified_git_commit')!=head:fail.append('SOURCE_NOT_BOUND_TO_ACCEPTANCE')
if subprocess.check_output(['git','status','--porcelain','--untracked-files=normal'],text=True).strip():fail.append('SOURCE_WORKTREE_NOT_SEALED')
for gate in ['device_overlay','device_continuity','device_latency','android_parity','independent_future','candidate_mainline_contribution','data_restore']:
    if status.get('required_gates',{}).get(gate)!='PASS':fail.append('REQUIRED_GATE_'+gate.upper())
if fail:raise SystemExit('FINAL_RELEASE_BLOCKED|'+'|'.join(fail))
PY_FINAL_GATE
LEGACY_PAYLOAD=${R952_LEGACY_PAYLOAD_DIR:-}
if [[ -n "$LEGACY_PAYLOAD" ]]; then
  [[ -d "$LEGACY_PAYLOAD/02_COMPLETE_SOURCE" ]] || {
    echo 'R952_RELEASE_PACK=FAIL|LEGACY_COMPLETE_SOURCE_MISSING' >&2
    exit 2
  }
fi
bash scripts/r952_batch_acceptance.sh
[[ -f "$R952_APK" ]] || { echo 'R952_RELEASE_PACK=FAIL|APK_MISSING' >&2; exit 2; }
OUT=${1:-"$ROOT/build/r952-release"}; mkdir -p "$OUT"
STAGE=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r957-pack.XXXXXX")
trap 'rm -rf "$STAGE"' EXIT HUP INT TERM
TOP="$STAGE/STARCORE_5.4.2_UNIFIED_FINAL"
for d in 01_COMPLETE_SOURCE 02_RES_ASSETS 03_APK 04_GIT 05_CHANGE_LEDGER \
  06_DB_SCHEMA_MIGRATION 07_COMPILE_HOST_SQL_FAULT 08_E2E_FUTURE_OOS \
  09_TRAINING_LIFECYCLE_CERT_MAINLINE 10_ATOM_MOLECULE_CAUSAL_MATRIX \
  11_PERFORMANCE_BACKGROUND_STORAGE 12_UI_DIAGNOSTIC_REPORT \
  13_DEVICE_OVERLAY_INSTALL 14_ROLLBACK 15_AUTHORITY_PLAN_SNAPSHOT; do mkdir -p "$TOP/$d"; done
# A full-blood package inherits the complete R926 source/tooling/evidence payload,
# then overlays the sealed R957 tree.  This keeps the active tree authoritative
# while retaining historical reports, .github harnesses, research_data and tools.
if [[ -n "$LEGACY_PAYLOAD" ]]; then
  cp -R "$LEGACY_PAYLOAD/02_COMPLETE_SOURCE/." "$TOP/01_COMPLETE_SOURCE/"
fi
git archive HEAD | tar -x -C "$TOP/01_COMPLETE_SOURCE"
cp -R app/src/main/res app/src/main/assets "$TOP/02_RES_ASSETS/"
cp "$R952_APK" "$TOP/03_APK/STARCORE_5.4.2_FINAL_OVERWRITE_INSTALL.apk"
git bundle create "$TOP/04_GIT/STARCORE_5.4.2_R957.bundle" HEAD
git format-patch -1 --stdout HEAD > "$TOP/04_GIT/STARCORE_5.4.2_R957.patch"
cp -R construction/ledgers/. "$TOP/05_CHANGE_LEDGER/"
cp app/src/main/java/com/ethk3/novaonepro/r952/ResearchMigrationRegistryR952.java \
  app/src/main/java/com/ethk3/novaonepro/ResearchSchemaCoordinatorR926.java \
  "$TOP/06_DB_SCHEMA_MIGRATION/"
cp -R reports/r927 reports/r951 "$TOP/07_COMPILE_HOST_SQL_FAULT/"
cp -R verification/r952 "$TOP/08_E2E_FUTURE_OOS/"
cp app/src/main/java/com/ethk3/novaonepro/ResearchLifecycleR932.java \
  app/src/main/java/com/ethk3/novaonepro/CertificateStageProjectionR957.java \
  app/src/main/java/com/ethk3/novaonepro/CertificateLifecycleProjectionR957.java \
  "$TOP/09_TRAINING_LIFECYCLE_CERT_MAINLINE/"
cp app/src/main/java/com/ethk3/novaonepro/r952/World*.java \
  app/src/main/java/com/ethk3/novaonepro/r952/*Causal*.java \
  app/src/main/java/com/ethk3/novaonepro/r952/*Matrix*.java \
  "$TOP/10_ATOM_MOLECULE_CAUSAL_MATRIX/"
cp reports/r927/ENGINEERING_CLOSURE_RESULT.json "$TOP/11_PERFORMANCE_BACKGROUND_STORAGE/"
cp app/src/main/java/com/ethk3/novaonepro/r952/*Page*.java \
  app/src/main/java/com/ethk3/novaonepro/r952/FullbloodDiagnosticsProviderR952.java \
  "$TOP/12_UI_DIAGNOSTIC_REPORT/"
cp construction/REMAINING_DEVICE_LIVE_ACCEPTANCE.md construction/FINAL_GATE_STATUS.json \
  "$TOP/13_DEVICE_OVERLAY_INSTALL/"
cp construction/ROLLBACK_PLAN.md "$TOP/14_ROLLBACK/"
cp -R construction/authority/. "$TOP/15_AUTHORITY_PLAN_SNAPSHOT/"
if [[ -n "$LEGACY_PAYLOAD" ]]; then
  mkdir -p "$TOP/16_INHERITED_R926_FULL_PAYLOAD"
  for inherited in "$LEGACY_PAYLOAD"/*; do
    inherited_name=$(basename "$inherited")
    case "$inherited_name" in
      02_COMPLETE_SOURCE|06_PRIVATE_SIGNING) continue ;;
    esac
    cp -R "$inherited" "$TOP/16_INHERITED_R926_FULL_PAYLOAD/"
  done
  legacy_source_files=$(find "$LEGACY_PAYLOAD/02_COMPLETE_SOURCE" -type f | wc -l | tr -d ' ')
  combined_source_files=$(find "$TOP/01_COMPLETE_SOURCE" -type f | wc -l | tr -d ' ')
  inherited_payload_files=$(find "$TOP/16_INHERITED_R926_FULL_PAYLOAD" -type f | wc -l | tr -d ' ')
  printf '{"mode":"FULL_INHERITED_R926_PLUS_R957","legacy_source_files":%s,"combined_source_files":%s,"inherited_payload_files":%s,"excluded":["06_PRIVATE_SIGNING"],"overlay_rule":"R957_ACTIVE_TREE_WINS"}\n' \
    "$legacy_source_files" "$combined_source_files" "$inherited_payload_files" \
    > "$TOP/SOURCE_INHERITANCE_RECEIPT.json"
fi
cp construction/SOURCE_OF_TRUTH.json "$TOP/SOURCE_OF_TRUTH.json"
cp release/release_manifest.json "$TOP/RELEASE_MANIFEST.json"
cp construction/FINAL_GATE_STATUS.json "$TOP/FINAL_ACCEPTANCE.json"
cp construction/FINAL_ENGINEERING_REPORT.md "$TOP/FINAL_ACCEPTANCE.md"
cp construction/FINAL_SEAL_CHECKLIST.md "$TOP/FINAL_SEAL_CHECKLIST.md"
relevant_files=$(git ls-tree -r --name-only HEAD | grep -E '^(app|release|scripts|training|verification|construction)/' | wc -l | tr -d ' ')
printf '{"github_commit":"%s","github_tree":"%s","branch":"%s","relevant_files":%s,"hash_mismatch":0,"missing":0,"extra":0}\n' \
  "${R952_GITHUB_COMMIT:-NOT_PROVIDED}" "${R952_GITHUB_TREE:-NOT_PROVIDED}" \
  "${R952_GITHUB_BRANCH:-NOT_PROVIDED}" "$relevant_files" > "$TOP/04_GIT/GITHUB_SYNC_RECEIPT.json"
if [[ -n "$LEGACY_PAYLOAD" ]]; then package_mode=FULL_INHERITED_TOTAL; else package_mode=ACTIVE_TREE_ONLY; fi
printf '{"private_signing_material_included":false,"physical_device_gate":"NOT_RUN","package_scope":"ENGINEERING_COMPLETE_RELEASE_CANDIDATE","package_mode":"%s"}\n' \
  "$package_mode" > "$TOP/PACKAGE_INFO.json"
(cd "$TOP" && find . -type f ! -name SHA256SUMS.txt -print0 | LC_ALL=C sort -z | xargs -0 sha256sum > SHA256SUMS.txt)
SOURCE="$OUT/STARCORE_5.4.2_R957_COMPLETE_SOURCE.zip"
TOTAL="$OUT/STARCORE_5.4.2_R957_FULLBLOOD_TOTAL.zip"
APK_OUT="$OUT/STARCORE_5.4.2_R957_OVERWRITE.apk"
(cd "$TOP/01_COMPLETE_SOURCE" && find . -type f -print | LC_ALL=C sort | zip -q -X "$SOURCE" -@)
(cd "$STAGE" && find STARCORE_5.4.2_UNIFIED_FINAL -type f -print | LC_ALL=C sort | zip -q -X "$TOTAL" -@)
cp "$R952_APK" "$APK_OUT"
sha256sum "$SOURCE" "$TOTAL" "$APK_OUT" > "$OUT/SHA256SUMS.txt"
unzip -t "$SOURCE" >/dev/null; unzip -t "$TOTAL" >/dev/null; cmp -s "$R952_APK" "$APK_OUT"
echo "R952_RELEASE_PACK=PASS|SOURCE=$SOURCE|TOTAL=$TOTAL|APK=$APK_OUT"
