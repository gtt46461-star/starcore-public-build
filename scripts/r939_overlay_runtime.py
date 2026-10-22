#!/usr/bin/env python3
"""Exercise the actual owner-signed R929 -> R939 upgrade on an isolated rooted Android emulator."""
import hashlib, json, re, shutil, sqlite3, subprocess, time
from pathlib import Path

R=Path(__file__).resolve().parents[1]
OUT=R/'reports/r939/overlay'; OUT.mkdir(parents=True,exist_ok=True)
PKG='com.ethk3.novaonepro'; DATA='/data/user/0/'+PKG

def adb(*args):
    r=subprocess.run(['adb',*args],check=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=120)
    return r.stdout.decode('utf-8',errors='replace').strip()

def launch(label):
    adb('logcat','-c')
    started=adb('shell','am','start','-W','-n',PKG+'/.MainActivity')
    (OUT/(label+'-start.txt')).write_text(started+'\n')
    assert 'Error:' not in started,started
    # First-ever database initialization can be slow; wait for the service's durable
    # identity result instead of killing a still-running migration after a fixed sleep.
    ready=False
    for attempt in range(18):
        time.sleep(10)
        try:
            databases('startup-poll')
            for path in (OUT/'startup-poll-databases').glob('*.db'):
                with sqlite3.connect(path,timeout=1) as c:
                    row=c.execute("SELECT state_value FROM app_state WHERE state_key='build_identity_status'").fetchone()
                    if row and row[0]: ready=True
        except (subprocess.CalledProcessError,sqlite3.Error):
            pass
        if ready:break
    assert ready,'SERVICE_IDENTITY_NOT_READY:'+label
    assert adb('shell','pidof',PKG),'APP_NOT_ALIVE:'+label
    log=adb('logcat','-d','-v','brief'); (OUT/(label+'-logcat.txt')).write_text(log)
    assert not re.search(r'FATAL EXCEPTION.*?Process: '+re.escape(PKG),log,re.S),'APP_CRASH:'+label
    adb('shell','am','force-stop',PKG)

def state():
    text=adb('shell','dumpsys','package',PKG)
    packages=adb('shell','pm','list','packages','-U','--user','0',PKG)
    uid=re.search(r'^package:'+re.escape(PKG)+r'\s+uid:(\d+)\s*$',packages,re.M)
    assert uid,'PACKAGE_UID_NOT_FOUND:'+packages
    version=re.search(r'versionCode=(\d+)',text)
    installed=re.search(r'firstInstallTime=(.*)',text)
    assert version and installed,'PACKAGE_INSTALL_METADATA_NOT_FOUND'
    return {'versionCode':int(re.search(r'versionCode=(\d+)',text).group(1)),
            'uid':int(uid.group(1)),
            'firstInstallTime':installed.group(1).strip()}

def databases(label):
    dest=OUT/(label+'-databases');shutil.rmtree(dest,ignore_errors=True)
    adb('pull',DATA+'/databases',str(dest))
    return {p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in dest.iterdir() if p.is_file()}

def main():
    old=R/'delivery/r929-final/Starcore-Tianshu-V57.21.1.0.4-R5.4.1.0.4-929-OVERLAY.apk'
    new=json.loads((R/'delivery/r939/release-artifact.json').read_text())
    assert hashlib.sha256(old.read_bytes()).hexdigest()=='26aba97437f7204cbfb0d66db118b02432110f05dd3eab4835093686cdfd2dc8'
    adb('root');adb('wait-for-device')
    assert 'Success' in adb('install','-r',str(old)); before=state();assert before['versionCode']==577250929
    launch('r929')
    adb('root');adb('wait-for-device')
    adb('shell','mkdir','-p',DATA+'/files')
    adb('shell','sh','-c',"'printf R939_OVERLAY_DATA_RETENTION > "+DATA+"/files/r939-upgrade-marker'")
    adb('shell','chown',str(before['uid'])+':'+str(before['uid']),DATA+'/files/r939-upgrade-marker')
    old_db=databases('before');assert any(k.endswith('.db') for k in old_db),'NO_OLD_DATABASE'
    baseline_tables={};baseline_counts={}
    for path in (OUT/'before-databases').glob('*.db'):
        with sqlite3.connect(path) as c:
            baseline_tables[path.name]={r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")}
            baseline_counts[path.name]={name:c.execute('SELECT COUNT(*) FROM "'+name.replace('"','""')+'"').fetchone()[0] for name in baseline_tables[path.name] if any(x in name for x in ('draw','trade','canonical','decision')) and not name.startswith('sqlite_')}

    assert 'Success' in adb('install','-r',str(R/new['apk_path']))
    after=state(); assert after['versionCode']==new['versionCode']
    assert before['uid']==after['uid'] and before['firstInstallTime']==after['firstInstallTime']
    assert adb('shell','cat',DATA+'/files/r939-upgrade-marker')=='R939_OVERLAY_DATA_RETENTION'
    bytes_equal=old_db==databases('after-install')
    launch('r939');databases('after-launch')
    checks={}; runtime_state={}
    for path in (OUT/'after-launch-databases').glob('*.db'):
        with sqlite3.connect(path) as c:
            checks[path.name]=c.execute('PRAGMA quick_check').fetchone()[0]
            tables={r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")}
            assert baseline_tables.get(path.name,set())<=tables,'PREDECESSOR_TABLE_REMOVED'
            for name,count in baseline_counts.get(path.name,{}).items():
                assert c.execute('SELECT COUNT(*) FROM "'+name.replace('"','""')+'"').fetchone()[0]>=count,'PREDECESSOR_HISTORY_ROWS_LOST:'+name
            if c.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name='app_state'").fetchone():
                runtime_state.update(dict(c.execute("SELECT state_key,state_value FROM app_state WHERE state_key IN ('build_identity_status','build_identity_hard_block','autofetch_build_identity_status','last_decision_block','r811_schema_revision')")))
    assert checks and all(v=='ok' for v in checks.values()),checks
    assert runtime_state.get('build_identity_status')=='BUILD_IDENTITY_OK_R915_CONTROLLED_CANDIDATE',runtime_state
    report={'runtime_state':runtime_state,'status':'PASS','scope':'ANDROID_EMULATOR_R929_TO_R939_NOT_USER_PHONE',
            'before':before,'after':after,'same_uid':True,'same_first_install_time':True,
            'private_file_preserved':True,'old_database_bytes_equal_on_install':bytes_equal,'predecessor_tables_preserved':True,'history_table_counts_preserved':baseline_counts,
            'database_quick_check_after_start':checks,'old_apk_sha256':hashlib.sha256(old.read_bytes()).hexdigest(),
            'new_apk_sha256':new['apk_sha256'],'cold_start_alive':True,'physical_device_verified':False,
            'live_network_prediction_verified':False}
    (OUT/'OVERLAY_ACCEPTANCE.json').write_text(json.dumps(report,indent=2)+'\n')
    print('R939_OWNER_SIGNED_R929_OVERLAY_AND_COLD_START_PASS')

if __name__=='__main__':main()
