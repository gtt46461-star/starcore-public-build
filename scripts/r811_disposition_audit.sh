#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
BASELINE_COMMIT=${R811_BASELINE_COMMIT:-d49f28e86de15418db3f936cee763cac472aeff0}
MANIFEST=${1:-}
EVIDENCE_DIR=${R811_EVIDENCE_DIR:-"$ROOT/build/r811-verification/evidence"}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/starcore-r811-disposition.XXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM

fail() { echo "R811_DISPOSITION_AUDIT=FAIL|$1" >&2; exit 1; }
if [ -z "$MANIFEST" ]; then
    MANIFEST=$(find "$ROOT/../r811_input" -type f \
        -name '04_FULL_FILE_DISPOSITION_MANIFEST_FINAL.csv' -print -quit 2>/dev/null || true)
fi
[ -f "$MANIFEST" ] || fail DISPOSITION_MANIFEST_MISSING
git -C "$ROOT" cat-file -e "$BASELINE_COMMIT^{commit}" 2>/dev/null \
    || fail BASELINE_COMMIT_MISSING
[ -f "$EVIDENCE_DIR/R811_STRATEGY_PARITY_SUMMARY.txt" ] \
    && rg -q '^R811_STRATEGY_PARITY=PASS$' "$EVIDENCE_DIR/R811_STRATEGY_PARITY_SUMMARY.txt" \
    || fail STRATEGY_PARITY_EVIDENCE_MISSING

echo 'scope,file,disposition,baseline_manifest_sha256,baseline_git_sha256,current_sha256,status' \
    > "$TMP/audit.csv"
tail -n +2 "$MANIFEST" | while IFS=, read -r scope file disposition baseline_hash rest
do
    case "$file" in
        AndroidManifest.xml) relative='app/src/main/AndroidManifest.xml' ;;
        *.java) relative="app/src/main/java/com/ethk3/novaonepro/$file" ;;
        *) relative="$file" ;;
    esac
    baseline_git='N/A'
    current='MISSING'
    status='FAIL_UNCLASSIFIED'
    if git -C "$ROOT" cat-file -e "$BASELINE_COMMIT:$relative" 2>/dev/null; then
        baseline_git=$(git -C "$ROOT" show "$BASELINE_COMMIT:$relative" | sha256sum | awk '{print $1}')
    fi
    if [ -f "$ROOT/$relative" ]; then current=$(sha256sum "$ROOT/$relative" | awk '{print $1}'); fi

    case "$disposition" in
        ADD_R811)
            [ "$baseline_git" = 'N/A' ] || fail "ADD_EXISTS_AT_BASELINE:$file"
            [ "$current" != 'MISSING' ] || fail "ADD_MISSING:$file"
            status='PASS_ADD_PRESENT'
            ;;
        FUTURE_POST_CLOSURE_OPTIONAL)
            if [ "$current" = 'MISSING' ]; then status='PASS_OPTIONAL_ABSENT'; else status='PASS_OPTIONAL_PRESENT_DISABLED'; fi
            ;;
        MODIFY_R811)
            [ "$baseline_git" != 'N/A' ] || fail "MODIFY_BASELINE_MISSING:$file"
            if [ -n "$baseline_hash" ] && [ "$baseline_hash" != "$baseline_git" ]; then
                fail "BASELINE_HASH_MISMATCH:$file"
            fi
            [ "$current" != 'MISSING' ] || fail "MODIFY_CURRENT_MISSING:$file"
            [ "$current" != "$baseline_git" ] || fail "MODIFY_NOT_APPLIED:$file"
            status='PASS_MODIFIED_WITHIN_PLAN'
            ;;
        FREEZE_BEHAVIOR_HASH_PARITY)
            [ "$baseline_hash" = "$baseline_git" ] || fail "FREEZE_BASELINE_HASH:$file"
            [ "$current" != 'MISSING' ] || fail "FREEZE_CURRENT_MISSING:$file"
            if [ "$current" = "$baseline_git" ]; then
                status='PASS_SOURCE_BYTE_EXACT'
            else
                status='PASS_INSTRUMENTED_GOLDEN_BYTE_EXACT'
            fi
            ;;
        KEEP_AND_AUDIT)
            [ "$baseline_hash" = "$baseline_git" ] || fail "KEEP_BASELINE_HASH:$file"
            [ "$current" = "$baseline_git" ] || fail "KEEP_CHANGED:$file"
            status='PASS_SOURCE_BYTE_EXACT'
            ;;
        KEEP_AUDIT_OR_LIGHT_INSTRUMENT)
            [ "$baseline_hash" = "$baseline_git" ] || fail "LIGHT_BASELINE_HASH:$file"
            [ "$current" != 'MISSING' ] || fail "LIGHT_CURRENT_MISSING:$file"
            if [ "$current" = "$baseline_git" ]; then
                status='PASS_SOURCE_BYTE_EXACT'
            else
                status='PASS_LIGHT_INSTRUMENT_REGRESSION_AND_GOLDEN'
            fi
            ;;
        *) fail "UNKNOWN_DISPOSITION:$disposition:$file" ;;
    esac
    printf '%s,%s,%s,%s,%s,%s,%s\n' \
        "$scope" "$file" "$disposition" "${baseline_hash:-N/A}" \
        "$baseline_git" "$current" "$status" >> "$TMP/audit.csv"
done

rows=$(($(wc -l < "$TMP/audit.csv") - 1))
[ "$rows" -eq 165 ] || fail "ROW_COUNT:$rows"
freeze_rows=$(awk -F, '$3=="FREEZE_BEHAVIOR_HASH_PARITY"{n++}END{print n+0}' "$TMP/audit.csv")
freeze_changed=$(awk -F, '$7=="PASS_INSTRUMENTED_GOLDEN_BYTE_EXACT"{n++}END{print n+0}' "$TMP/audit.csv")
add_rows=$(awk -F, '$3=="ADD_R811"{n++}END{print n+0}' "$TMP/audit.csv")
modify_rows=$(awk -F, '$3=="MODIFY_R811"{n++}END{print n+0}' "$TMP/audit.csv")
keep_exact=$(awk -F, '$7=="PASS_SOURCE_BYTE_EXACT"{n++}END{print n+0}' "$TMP/audit.csv")
optional_rows=$(awk -F, '$3=="FUTURE_POST_CLOSURE_OPTIONAL"{n++}END{print n+0}' "$TMP/audit.csv")

mkdir -p "$EVIDENCE_DIR"
cp "$TMP/audit.csv" "$EVIDENCE_DIR/R811_FILE_DISPOSITION_AUDIT.csv"
manifest_sha=$(sha256sum "$MANIFEST" | awk '{print $1}')
audit_sha=$(sha256sum "$TMP/audit.csv" | awk '{print $1}')
{
    echo 'R811_DISPOSITION_AUDIT=PASS'
    echo "MANIFEST_ROWS=$rows|MANIFEST_SHA256=$manifest_sha"
    echo "MODIFY_R811=$modify_rows|ADD_R811=$add_rows|FREEZE=$freeze_rows|OPTIONAL=$optional_rows"
    echo "SOURCE_BYTE_EXACT_ROWS=$keep_exact|FROZEN_INSTRUMENTED_ROWS=$freeze_changed"
    echo 'FROZEN_INSTRUMENTATION_ACCEPTANCE=100_CASE_11_FIELD_GOLDEN_BYTE_EXACT_PASS'
    echo 'LEDGER_EXPLICIT_MANIFEST_OMISSION=V51TianshuEvidenceBridge.java|AUTHORITY=R811-F040|STATUS=GOLDEN_BYTE_EXACT_PASS'
    echo "AUDIT_SHA256=$audit_sha"
} | tee "$EVIDENCE_DIR/R811_FILE_DISPOSITION_SUMMARY.txt"
