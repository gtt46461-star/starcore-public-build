#!/usr/bin/env bash
# Wait for the "STARCORE R31 construction gate" CI run of HEAD, then pull the BUILD_SUMMARY it committed back.
# Usage: bash tools/r31/ci_wait.sh K02      (run in background; a full build takes ~20-40 min)
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
B="${1:?batch id}"; SHA="$(git rev-parse HEAD)"
BR="construction/r31-final-20261104"; REPO="gtt46461-star/starcore-public-build"
echo "waiting for CI gate of ${SHA:0:12} ($B)"
for i in $(seq 1 90); do
  ROW="$(gh api "repos/$REPO/actions/runs?head_sha=$SHA&per_page=30" \
        --jq '[.workflow_runs[]|select(.name=="STARCORE R31 construction gate")][0]|"\(.id) \(.status) \(.conclusion)"' 2>/dev/null || true)"
  case "$ROW" in
    *" completed "*) echo "CI run: $ROW"; break ;;
    "" | null*) [ "$i" -gt 5 ] && { echo "R31_CI_NO_RUN_FOR_HEAD (was a source file pushed? evidence/docs-only pushes do not trigger CI)"; exit 3; } ;;
  esac
  sleep 60
done
git pull -q --rebase origin "$BR"
S="verification/r31/batches/$B/BUILD_SUMMARY.json"
python3 - "$S" "$SHA" <<'PY'
import json, sys
s = json.load(open(sys.argv[1]))
fresh = s["source_head"] == sys.argv[2]
fails = sorted(k for k, v in s["tests"].items() if v["status"] != "PASS")
print(json.dumps({"summary_is_for_pushed_head": fresh, "gradle": s["gradle"], "unit": s["unit"],
                  "failing_tests": fails[:40], "host_failed": s["host_failed"]}, ensure_ascii=False, indent=1))
if not fresh:
    print("R31_CI_SUMMARY_STALE: CI summary is for", s["source_head"][:12]); sys.exit(4)
PY
echo "gradle log: verification/r31/batches/$B/logs/gradle.log ; next: fix and push again, or run r31_gate.py close --batch $B"
