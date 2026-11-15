#!/usr/bin/env python3
"""Real launcher/service regression; historical READY preferences are never acceptance."""
import argparse, subprocess, time, glob, json, shlex, xml.etree.ElementTree as ET
from pathlib import Path
from wait_r13_android_ready import wait_ready
parser=argparse.ArgumentParser();parser.add_argument('--baseline',action='store_true');parser.add_argument('--installed',action='store_true');parser.add_argument('--apk-already-installed',action='store_true');args=parser.parse_args()
PKG='com.starcore.research.r13'
out=Path('startup-evidence');out.mkdir(exist_ok=True)
def adb(*argv,check=True):
    r=subprocess.run(['adb',*argv],capture_output=True,text=True,timeout=30)
    if check and r.returncode:raise RuntimeError('ADB_FAILED: '+r.stderr)
    return r.stdout
_root_prefs_r30=None
pref_read_retries={}
pref_read_unstable={}
def prefs(name):
    global _root_prefs_r30
    # Release APKs are not debuggable: run-as is unavailable. Root access is allowed only on the disposable emulator.
    if _root_prefs_r30 is None:
        emulator=adb('get-serialno',check=False).strip().startswith('emulator-') and adb('shell','getprop','ro.kernel.qemu',check=False).strip()=='1'
        _root_prefs_r30=emulator and adb('shell','id','-u',check=False).strip()=='0'
    root=_root_prefs_r30
    # Shell status is separate from XML: exec-out can mix a missing-file error into stdout.
    path=('/data/user/0/'+PKG+'/shared_prefs/' if root else 'shared_prefs/')+name+'.xml'
    actual=shlex.quote(path);backup=shlex.quote(path+'.bak')
    reader=("if [ -e "+actual+" ]; then printf 'R30_PREF_PRESENT\\n'; cat "+actual
            +"; elif [ -e "+backup+" ]; then printf 'R30_PREF_BACKUP\\n'; cat "+backup
            +"; else printf 'R30_PREF_ABSENT\\n'; fi")
    command=reader if root else 'run-as '+shlex.quote(PKG)+' sh -c '+shlex.quote(reader)
    # Retry the same original file or its committed backup, never cached READY data.
    read_deadline=time.monotonic()+1.0
    while True:
        raw=adb('shell',command,check=False)
        status,separator,xml=raw.partition('\n')
        if status.strip()=='R30_PREF_ABSENT' and name!='v20_service_runtime_light':return {}
        try:
            if status.strip() not in ('R30_PREF_PRESENT','R30_PREF_BACKUP') or not separator:
                raise ET.ParseError('preference read status unavailable')
            return {e.attrib['name']:e.text or e.attrib.get('value','') for e in ET.fromstring(xml)}
        except ET.ParseError:
            if time.monotonic()>=read_deadline:
                pref_read_unstable[name]=pref_read_unstable.get(name,0)+1
                if name=='v20_crash_snapshot':raise RuntimeError('CRASH_SNAPSHOT_UNREADABLE')
                return {}
            pref_read_retries[name]=pref_read_retries.get(name,0)+1
            time.sleep(.025)

def snapshot(phase):
    p=prefs('v20_service_runtime_light')
    row={k:v for k,v in p.items() if k.startswith(('init_','recovery_lease_','last_recovery_')) or k in ('service_ready_at','user_enabled','process_id','heartbeat_at')}
    row.update(phase=phase,observed_at_ms=int(adb('shell','date','+%s').strip())*1000,
               actual_pid=adb('shell','pidof',PKG,check=False).strip())
    return row
def capture(label):
    adb('shell','uiautomator','dump','/sdcard/startup.xml',check=False)
    adb('pull','/sdcard/startup.xml',str(out/(label+'.xml')),check=False)
    adb('shell','screencap','-p','/sdcard/startup.png')
    adb('pull','/sdcard/startup.png',str(out/(label+'.png')))
wait_ready()
if not args.installed:
    if not args.apk_already_installed:
        print(adb('install','-r',glob.glob('delivered/app/build/outputs/apk/debug/*.apk')[0]),flush=True)
        wait_ready()
    assert 'Success' in adb('shell','pm','clear',PKG)
adb('logcat','-c')
rows=[];passed=False;baseline_reproduced=False;previous_pid='';previous_ready=0
try:
    for phase in ('cold','restart'):
        if phase=='restart':
            adb('shell','am','force-stop',PKG)
            assert not adb('shell','pidof',PKG,check=False).strip(),'OLD_PROCESS_NOT_STOPPED'
        print(adb('shell','am','start','-W','-n',PKG+'/com.ethk3.novaonepro.MainActivity'),flush=True)
        deadline=time.monotonic()+(90 if phase=='restart' else 180)
        ready_since=None;previous=None;fresh=False;first_heartbeat=0
        while time.monotonic()<deadline:
            row=snapshot(phase);rows.append(row)
            if row!=previous:print(json.dumps(row,ensure_ascii=False),flush=True);previous=row
            assert not row.get('init_last_error'),'STARTUP_ERROR: '+row.get('init_last_error','')
            assert row.get('init_retry_count','0')=='0','STARTUP_RETRIED'
            pid=row.get('actual_pid','');recorded=row.get('process_id','')
            ready_at=int(row.get('service_ready_at',0));heartbeat=int(row.get('heartbeat_at',0))
            fresh=(row.get('init_status')=='READY' and pid and pid==recorded
                   and ready_at>previous_ready and (phase!='restart' or pid!=previous_pid)
                   and -2000 <= row['observed_at_ms']-heartbeat <= 10000)
            if fresh:
                if ready_since is None:ready_since=time.monotonic();first_heartbeat=heartbeat
                if time.monotonic()-ready_since>=60 and heartbeat>first_heartbeat:
                    previous_pid=pid;previous_ready=ready_at;break
            else:ready_since=None
            time.sleep(2)
        else:
            if args.baseline and phase=='restart' and not any(
                x['phase']=='restart' and int(x.get('service_ready_at',0))>previous_ready for x in rows):
                baseline_reproduced=True
                print('BASELINE_RESTART_NO_NEW_SERVICE_REPRODUCED',flush=True)
                capture(phase);break
            raise AssertionError('Fresh service/heartbeat never remained READY for 60 seconds: '+phase)
        assert not prefs('v20_crash_snapshot').get('type'),'Caught failure: '+json.dumps(prefs('v20_crash_snapshot'))
        capture(phase)
    if args.baseline:assert baseline_reproduced,'Expected baseline restart failure did not reproduce'
    else:passed=True
finally:
    passed_before_final=passed;final_crash={};final_crash_error=''
    try:final_crash=prefs('v20_crash_snapshot')
    except Exception as error:final_crash_error='CRASH_SNAPSHOT_READ_FAILED_'+type(error).__name__;passed=False
    if final_crash.get('type'):final_crash_error='CAUGHT_FAILURE_IN_FINAL_SNAPSHOT';passed=False
    (out/'startup.json').write_text(json.dumps({'passed':passed,'baseline_reproduced':baseline_reproduced,'samples':rows,'crash':final_crash,'crashReadError':final_crash_error,'preferenceReadRetries':pref_read_retries,'unstableReadsAfterRetry':pref_read_unstable},ensure_ascii=False,indent=2))
    print(json.dumps({'crash':final_crash,'crashReadError':final_crash_error},ensure_ascii=False),flush=True)
    (out/'logcat.txt').write_text(adb('logcat','-d',check=False))
    capture('final')
    if passed_before_final and final_crash_error:raise RuntimeError(final_crash_error)
