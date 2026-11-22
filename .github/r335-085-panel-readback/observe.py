"""Read every production chart and logical table in an isolated normal API 34 service session."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

kit = Path('.github/r335-independent-window').resolve()
sys.path.insert(0,str(kit))
spec = importlib.util.spec_from_file_location('window_support',kit/'observe.py')
observer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(observer)
field = observer.base
directory = Path(__file__).resolve().parent
source = '02b7e3b46dcec7f1ae274c665c40b7ba1f35d62d'
digest = os.environ['REPAIRED_APK_SHA']
observer_digest = '5e460aded12a3baa1dc8b4929d7daf03ce960c2d4d4db16a4fc4963c35563699'
package = field.PKG
db = '/data/user/0/'+package+'/databases/'+observer.DB
tables = ('draws','v57_algorithm_genome_r908','v57_algorithm_predraw_r908','r27_policy','r27_replay_dataset','r26_candidate','r26_evolution','r27_live_frozen','r30r1_chart_cache','r33_research_applied','r26_training_step')
record = {'sourceCommit':source,'apkSha256':digest,'observerDexSha256':observer_digest,'api':34,
          'scope':'All 47 production chart readers and 16 table adapters on an unseeded network service, isolated emulator, no user database','samples':[]}
out = field.TEMP/('R335-085-panels-'+os.environ['GITHUB_RUN_ID']+'.json')

def save():
    out.write_text(json.dumps(record,indent=2)+'\n')

def snapshot(install,observer_dex,elapsed):
    proc = subprocess.run(['adb','shell','env','CLASSPATH='+install+':'+observer_dex,'app_process','-Xms16m','-Xmx256m','/system/bin','com.ethk3.novaonepro.InstalledChartReadback',db],capture_output=True,timeout=240)
    entries = {}
    for line in proc.stdout.decode(errors='replace').splitlines():
        panel = re.fullmatch(r'PANEL ([a-zA-Z0-9_]{1,55}) ([A-Z_0-9]{1,100}) ([0-9]+) ([0-9]+) ([0-9]+) ([0-9]+)',line.strip())
        page = re.fullmatch(r'TABLE (T[0-9]{2}) ([a-zA-Z_0-9]{1,70}) (null|[0-9]+) (null|[0-9]+) (true|false|null)',line.strip())
        if panel:
            entries[panel[1]] = {'status':panel[2],**dict(zip(('points','edges','matrixRows','matrixColumns'),map(int,panel.groups()[2:])))}
        if page:
            entries[page[1]] = {'status':page[2],'shown':None if page[3]=='null' else int(page[3]),'totalMatching':None if page[4]=='null' else int(page[4]),'hasMore':{'true':True,'false':False,'null':None}[page[5]]}
    counts={}
    for table in tables:
        read = subprocess.run(['adb','shell','sqlite3','-readonly','-batch',db],input='.timeout 3000\nSELECT count(*) FROM '+table+';\n',capture_output=True,text=True,timeout=15)
        counts[table] = int(read.stdout.strip()) if read.returncode==0 and read.stdout.strip().isdigit() else ('TABLE_ABSENT' if 'no such table' in read.stderr else 'READ_UNAVAILABLE')
    record['samples'].append({'elapsedSeconds':elapsed,'exitCode':proc.returncode,'rawLogSha256':hashlib.sha256(proc.stdout+proc.stderr).hexdigest(),'entries':entries,'sourceCounts':counts})
    save()
    assert proc.returncode==0 and len(entries)==63

def main():
    assert field.adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    field.EVID.mkdir(exist_ok=True)
    observer.RAW.mkdir(exist_ok=True,parents=True)
    field.adb('root')
    field.adb('wait-for-device')
    previous = observer.get_apk('B08',577311065,'a724adbfb31dd96f1459aa2e5cbd94582dd6e82d')
    assert field.fingerprint(previous)=='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'
    assert b'Success' in field.adb('install',str(previous))
    field.adb('shell','pm','grant',package,'android.permission.POST_NOTIFICATIONS')
    field.adb('shell','am','start','-W','-n',field.ACTIVITY)
    time.sleep(30)
    old_uid,_=field.installed(field.EVID/'before.txt',577311065)
    release=observer.get_apk('B08',577311085,source)
    assert field.fingerprint(release)==digest and b'Success' in field.adb('install','-r',str(release))
    assert field.installed(field.EVID/'after.txt',577311085)==(old_uid,digest)
    dex=directory/'observer.dex.jar'
    assert field.fingerprint(dex)==observer_digest
    remote='/data/local/tmp/read-only-chart-observer.jar'
    field.adb('push',str(dex),remote)
    field.adb('shell','chmod','444',remote)
    assert field.adb('shell','sha256sum',remote).decode().split()[0]==observer_digest
    install=field.adb('shell','pm','path',package).decode().strip().split('package:')[-1]
    assert field.adb('shell','sha256sum',install).decode().split()[0]==digest
    assert observer.tap_start()
    start=time.monotonic()
    for delay in (90,900,1500):
        time.sleep(max(0,start+delay-time.monotonic()))
        snapshot(install,remote,int(time.monotonic()-start))
        if delay==90:field.adb('shell','input','keyevent','KEYCODE_HOME')
    assert field.installed(field.EVID/'end.txt',577311085)==(old_uid,digest)
    record['completed']=True
    save()

if __name__=='__main__':
    try:
        main()
    except Exception as exc:
        record.update(completed=False,failureType=type(exc).__name__)
        save()
        raise
