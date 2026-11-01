#!/usr/bin/env bash
# Keep the original14 separately; verify12 new production-research transaction faults.
set -euo pipefail
out=${1:?evidence directory required}
mkdir -p "$out"
app_id=com.starcore.research.r13
runner="$app_id.test/com.ethk3.novaonepro.R30FaultAndroidRunner"
[[ $(adb get-serialno) == emulator-* ]] && [[ $(adb shell getprop ro.kernel.qemu | tr -d '\r') == 1 ]] || { echo EMULATOR_REQUIRED; exit 1; }
instrument() {
  adb shell am instrument -w -r -e class com.ethk3.novaonepro.strategyv2.RepairResearchFaultAndroidTest#faultCell -e r30Sandbox "$1" -e repairStage "$2" -e repairFault "$3" -e r30Phase "$4" "$runner"
}
trap 'adb shell am force-stop "$app_id" >/dev/null 2>&1 || true' EXIT
: > "$out/cells.tsv"
for stage in analysis consume; do
  for fault in BUSY FULL CANCEL KILL CORRUPT SOURCE_CHANGE; do
    scope="research-${GITHUB_RUN_ID:-local}-$stage-$fault"
    adb shell am force-stop "$app_id"
    adb logcat -c
    if [[ $fault == KILL ]]; then
      instrument "$scope" "$stage" "$fault" arm > "$out/$stage-$fault-arm.txt" 2>&1 || true
      adb logcat -d -v threadtime > "$out/$stage-$fault-arm-logcat.txt"
      grep -F R30_REPAIR_KILL_POINT "$out/$stage-$fault-arm-logcat.txt" | grep -F "$stage|ORIGINAL_BATCH_BEFORE_COMMIT|pid=" > "$out/$stage-$fault-kill-point.txt" || { cat "$out/$stage-$fault-arm.txt"; exit 1; }
      instrument "$scope" "$stage" "$fault" recover > "$out/$stage-$fault-result.txt" 2>&1
      grep -Fq "R30_REPAIR_KILL_RECOVERY|$stage|ACTUAL_REOPEN_NO_HALF_CHAIN" "$out/$stage-$fault-result.txt" || { cat "$out/$stage-$fault-result.txt"; exit 1; }
    else
      instrument "$scope" "$stage" "$fault" run > "$out/$stage-$fault-result.txt" 2>&1
    fi
    adb logcat -d -v threadtime > "$out/$stage-$fault-logcat.txt"
    grep -Fq 'OK (1 test)' "$out/$stage-$fault-result.txt" && grep -F "R30_REPAIR_FAULT_CELL|$stage|$fault|" "$out/$stage-$fault-result.txt" | grep -Fq '|PASS' || { cat "$out/$stage-$fault-result.txt"; exit 1; }
    printf '%s\t%s\n' "$stage" "$fault" >> "$out/cells.tsv"
    echo "R30_REPAIR_FAULT_PROGRESS:$stage:$fault:PASS"
  done
done
python3 - "$out" <<'PY'
import json,pathlib,subprocess,sys,hashlib
p=pathlib.Path(sys.argv[1]);rows=[x.split('\t') for x in (p/'cells.tsv').read_text().splitlines()]
assert set(map(tuple,rows))=={(s,f) for s in ('analysis','consume') for f in ('BUSY','FULL','CANCEL','KILL','CORRUPT','SOURCE_CHANGE')} and len(rows)==12
proof={'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'environment':'ANDROID_EMULATOR','result':'PASS','cells':[{'stage':s,'fault':f,'pass':True,'outputSha256':hashlib.sha256((p/f'{s}-{f}-result.txt').read_bytes()).hexdigest()} for s,f in rows],'semantics':'Real original writer admission; real SQLiteFullException; cancellation before write admission; SIGKILL after original batch guards before commit then real same-scope process reopen; exact child/hash and real input correction guards. Invalid source/payload stays blocked. No REAL certificate or formal exposure.','physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN'}
(p/'research-fault-matrix.json').write_text(json.dumps(proof,ensure_ascii=False,indent=2)+'\n')
print('R30_REPAIR_RESEARCH_FAULTS|12_ACTUAL_ANDROID_CELLS|PASS')
PY
