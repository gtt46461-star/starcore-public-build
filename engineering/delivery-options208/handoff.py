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
candidate=tmp/"candidate-208.apk"
old_sha=os.environ["BASELINE_SHA"]
assert ci.sha(baseline)==old_sha and "Success" in ci.adb(serial,"install",baseline)
capture=ci.signed_report_campaign(serial,out,old_sha,old_sha)
capture("baseline","COMPACT_DIAGNOSTIC","85678fcfd12816aec8b47474cd2349350a5684d7")
original=json.loads((out/"signed-report-readbacks.json").read_text())["reports"][0]
assert "Success" in ci.adb(serial,"install","-r",candidate)
ui.commit=os.environ["SOURCE"]
ui.out={"reportReadbacks":[],"activeProfile":"COMPACT_DIAGNOSTIC"}
ui.report_entry(ui.CREATE)
dialog=ui.wait_for_report('delivery208')
one=ui.save_report(dialog,'delivery208-single')
ui.CREATE="取回已生成的 ZIP"
def reopen():
    ui.report_entry(ui.CREATE)
    return ui.wait_for_report("delivery208-reopen")
assert ui.tap(reopen(),"分享完整ZIP")
def foreground():
    return ui.adb('shell','dumpsys','activity','activities')
for tick in range(30):
    active=foreground()
    if any(('mResumedActivity' in line or 'topResumedActivity' in line) and ('Chooser' in line or 'intentresolver' in line) for line in active.splitlines()):break
    time.sleep(1)
else:
    raise AssertionError('Android share chooser was not foreground')
ui.adb('shell','input','keyevent','KEYCODE_BACK')
folder='StarcoreReport208'
for directory in ['/sdcard','/sdcard/Download','/sdcard/Documents']:
    ui.adb('shell','mkdir','-p',directory+'/'+folder)
assert ui.tap(reopen(),"保存到文件夹")
def choose_folder():
    navigated=False
    for tick in range(22):
        xml,tree=ui.ui()
        nodes=list(tree.iter('node'))
        labels=[n.get('text','') or n.get('content-desc','') for n in nodes]
        if folder in labels and ui.tap(tree,folder):
            time.sleep(1)
            _,inside=ui.ui()
            if ui.tap(inside,'Use this folder') or ui.tap(inside,'使用此文件夹'):
                time.sleep(1)
                _,confirm=ui.ui()
                if not (ui.tap(confirm,'Allow') or ui.tap(confirm,'允许')):raise AssertionError('Directory permission confirmation missing')
                return
        if tick in (2,8,15):
            ui.tap(tree,'Show roots') or ui.tap(tree,'Open navigation drawer') or ui.tap(tree,'显示根目录')
        elif ('Downloads' in labels or '下载' in labels) and not navigated:
            navigated=ui.tap(tree,'Downloads') or ui.tap(tree,'下载')
        else:
            ui.adb('shell','input','swipe','250','740','250','230','250')
        time.sleep(1)
    raise AssertionError('Cannot navigate DocumentsUI to dedicated test directory: '+str(labels))
choose_folder()
for tick in range(120):
    choices=ui.adb('shell','find','/sdcard/'+folder,'/sdcard/Download/'+folder,'/sdcard/Documents/'+folder,'-type','f',check=False).splitlines()
    matches=[p for p in choices if p.endswith('STARCORE_FULL_REPORT.zip') or p.lower().endswith('.zip')]
    if any(ui.adb('shell','sha256sum',p).split()[0]==one['sha256'] for p in matches):break
    time.sleep(1)
else:raise AssertionError('Saved directory ZIP did not match verified single-file export')
result={'shareChooserOpened':True,'folderPublicReadbackSha256':one['sha256'],'singleZipBytes':one['bytes'],'sourceCommit':ui.commit,'jobId':one['jobId']}
print(json.dumps(result),flush=True)
(out/'historical-readback.json').write_text(json.dumps(result)+'\n')
