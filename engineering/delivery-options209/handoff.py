"""Save, share and copy the same verified ZIP using the released Android app."""
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
candidate=tmp/"candidate-209.apk"
apk_sha="2055f03345ef27686c63e3ac02b76fc2615ba9260e2d5046634243be66d946e7"
assert ci.sha(candidate)==apk_sha and "Success" in ci.adb(serial,"install",candidate)
ci.signed_report_campaign(serial,out,apk_sha,apk_sha)
ci.adb(serial,"shell","pm","grant",ci.PACKAGE,"android.permission.POST_NOTIFICATIONS")
ui.commit=os.environ["SOURCE"]
ui.out={"reportReadbacks":[],"activeProfile":"COMPACT_DIAGNOSTIC"}
ui.report_entry(ui.CREATE)
dialog=ui.wait_for_report('delivery209')
one=ui.save_report(dialog,'delivery209-single')
ui.CREATE="取回已生成的 ZIP"
def reopen():
    ui.report_entry(ui.CREATE)
    return ui.wait_for_report("delivery209-reopen")
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
folder='00_StarcoreReport209'
for directory in ['/sdcard','/sdcard/Download','/sdcard/Documents']:
    ui.adb('shell','mkdir','-p',directory+'/'+folder)
assert ui.tap(reopen(),"保存到文件夹")
def choose_folder():
    import re

    def click(node):
        left,top,right,bottom=map(int,re.findall('[0-9]+',node.get('bounds')))
        ui.adb('shell','input','tap',str((left+right)//2),str((top+bottom)//2))

    provider_root_labels={'Alarms','Android','Movies','Music','Pictures','Podcasts','Notifications','Ringtones'}
    for tick in range(22):
        _,tree=ui.ui()
        nodes=list(tree.iter('node'))
        labels={n.get('text') or n.get('content-desc','') for n in nodes}
        folder_row=next((n for n in nodes if n.get('resource-id','').endswith(':id/title') and n.get('text')==folder),None)
        if folder_row is not None:
            click(folder_row)
            time.sleep(1)
            _,inside=ui.ui()
            observed=list(inside.iter('node'))
            still_root=len(provider_root_labels & {n.get('text') for n in observed})>=4
            selector=next((n for n in observed if n.get('enabled')=='true' and n.get('text','').casefold() in {'use this folder','使用此文件夹'}),None)
            if selector is not None and not still_root:
                click(selector)
                for retry in range(10):
                    time.sleep(1)
                    _,confirm=ui.ui()
                    nodes=list(confirm.iter('node'))
                    if not any(n.get('package','').endswith('documentsui') for n in nodes):return
                    positive=next((n for n in nodes if n.get('enabled')=='true' and
                        n.get('text','').casefold() in {'allow','允许','ok','确定','select'}),None)
                    if positive is not None:
                        click(positive)
                return
        elif tick in (2,8,15):
            ui.tap(tree,'Show roots') or ui.tap(tree,'Open navigation drawer') or ui.tap(tree,'显示根目录')
        elif 'Downloads' in labels or '下载' in labels:
            ui.tap(tree,'Downloads') or ui.tap(tree,'下载')
        else:
            ui.adb('shell','input','swipe','250','720','250','260','250')
        time.sleep(1)
    raise AssertionError('DocumentsUI did not enter the test folder: '+str(sorted(labels)))
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
