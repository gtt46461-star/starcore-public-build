#!/usr/bin/env python3
"""Real launcher/service regression; historical READY preferences are never acceptance."""
import argparse, subprocess, time, glob, json, xml.etree.ElementTree as ET
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('--baseline',action='store_true');parser.add_argument('--installed',action='store_true');args=parser.parse_args()
PKG='com.starcore.research.r13'
out=Path('startup-evidence');out.mkdir(exist_ok=True)
def adb(*argv,check=True):
    r=subprocess.run(['adb',*argv],capture_output=True,text=True,timeout=30)
    if check and r.returncode:raise RuntimeError('ADB_FAILED: '+r.stderr)
    return r.stdout
def prefs(name):
    raw=adb('exec-out','run-as',PKG,'cat','shared_prefs/'+name+'.xml',check=False)
    try:return {e.attrib['name']:e.text or e.attrib.get('value','') for e in ET.fromstring(raw)}
    except ET.ParseError:return {}
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
if not args.installed:
    print(adb('install','-r',glob.glob('delivered/app/build/outputs/apk/debug/*.apk')[0]),flush=True)
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
    (out/'startup.json').write_text(json.dumps({'passed':passed,'baseline_reproduced':baseline_reproduced,'samples':rows,'crash':prefs('v20_crash_snapshot')},ensure_ascii=False,indent=2))
    print(json.dumps(prefs('v20_crash_snapshot'),ensure_ascii=False),flush=True)
    (out/'logcat.txt').write_text(adb('logcat','-d',check=False))
    capture('final')
