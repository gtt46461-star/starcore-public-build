"""Exercise retained report selection while a newer generation owns the worker."""
import json
import os
from pathlib import Path
import sys
import time

source=Path("source").resolve()
sys.path.insert(0,str(source/"tools/r342"))
import ci_candidate_tests as ci
import report_ui_driver as ui

tmp=Path(os.environ["RUNNER_TEMP"])
out=tmp/"history-evidence"
out.mkdir()
serial=ui.DEVICE
assert serial.startswith("emulator-") and ci.adb(serial,"shell","getprop","ro.kernel.qemu")=="1"
baseline=tmp/"installed-204.apk"
candidate=tmp/"candidate-207.apk"
old_sha=os.environ["BASELINE_SHA"]
assert ci.sha(baseline)==old_sha and "Success" in ci.adb(serial,"install",baseline)
capture=ci.signed_report_campaign(serial,out,old_sha,old_sha)
capture("baseline","COMPACT_DIAGNOSTIC","85678fcfd12816aec8b47474cd2349350a5684d7")
original=json.loads((out/"signed-report-readbacks.json").read_text())["reports"][0]
assert "Success" in ci.adb(serial,"install","-r",candidate)
root="/data/user/0/"+ci.PACKAGE+"/files/r981_reports"
def read(name):
    return json.loads(ci.adb(serial,"shell","cat",root+"/"+name))
def waiting():
    try:
        head=read("current-job.json")["jobId"]
        if head!=original["jobId"]:
            state=read("jobs/"+head+"/job.json")
            if state["mode"]=="FULL_REPORT" and state["stage"] not in {"READY_TO_SHARE","FAILED","CANCELLED"}:
                return head,state["stage"]
    except (ValueError,KeyError,RuntimeError):
        pass
    return None
ui.report_entry(ui.CREATE)
for attempt in range(180):
    head=waiting()
    if head:break
    time.sleep(0.25)
assert head,"New request did not become active"
ui.CREATE="取回已生成的 ZIP"
ui.report_entry(ui.CREATE)
tree=ui.wait_for_report("historical-selection")
at_selection=read("jobs/"+head[0]+"/job.json")["stage"]
ui.commit="85678fcfd12816aec8b47474cd2349350a5684d7"
ui.out={"reportReadbacks":[],"activeProfile":"COMPACT_DIAGNOSTIC"}
saved=ui.save_report(tree,"selected-old-zip")
assert saved["jobId"]==original["jobId"] and saved["sha256"]==original["sha256"] and saved["bytes"]==original["bytes"]
receipt={"activeNewerRequest":head[0],"stageAtOpen":head[1],"newerStageAtDialog":at_selection,"previousRequest":saved["jobId"],"previousSource":"85678fcfd12816aec8b47474cd2349350a5684d7","restoredSha256":saved["sha256"],"savedBytes":saved["bytes"],"sameBytesAsPreviousExport":True}
(out/"historical-readback.json").write_text(json.dumps(receipt,indent=2)+"\n")
print(json.dumps(receipt),flush=True)
