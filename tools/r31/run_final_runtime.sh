#!/usr/bin/env bash
# One exact release after actual UI/SAF export; keep both independent outcomes.
set -uo pipefail
built=${1:?exact build artifact}
public=${2:?public evidence directory}
old=${3:?original overlay baseline}
set +e
bash tools/r31/run_phone_report_android.sh "$built" "$public" "$old"
report_exit=$?
python3 -B tools/r31/profile_signed_release.py --built "$built" --support "$built" --out "$public/warm-live" --expected-source "$R31_PROFILE_SOURCE" --expected-apk-sha "$R31_PROFILE_APK_SHA" --test-source "$R31_PROFILE_SOURCE"
live_exit=$?
set -e
python3 - "$public" "$report_exit" "$live_exit" <<'PY'
import json,sys
from pathlib import Path
out=Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
record={'reportUiSafExit':int(sys.argv[2]),'warmLiveExit':int(sys.argv[3]),'physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN'}
(out/'FINAL_RUNTIME_OUTCOMES.json').write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record))
PY
test "$report_exit" = 0 && test "$live_exit" = 0
