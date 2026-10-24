#!/usr/bin/env python3
"""Exercise the delivered APK as a new user, without an instrumentation runner."""
import subprocess, time, glob, json, xml.etree.ElementTree as ET
from pathlib import Path
PKG='com.starcore.research.r13'
out=Path('startup-evidence');out.mkdir(exist_ok=True)
def adb(*args):
    return subprocess.run(['adb',*args],capture_output=True,text=True,timeout=30).stdout
def prefs(name):
    raw=adb('exec-out','run-as',PKG,'cat','shared_prefs/'+name+'.xml')
    try:return {e.attrib['name']:e.text or e.attrib.get('value','') for e in ET.fromstring(raw)}
    except ET.ParseError:return {}
adb('install','-r',glob.glob('delivered/app/build/outputs/apk/debug/*.apk')[0])
adb('shell','pm','clear',PKG)
adb('logcat','-c')
print(adb('shell','am','start','-W','-n',PKG+'/com.ethk3.novaonepro.MainActivity'),flush=True)
rows=[];ready=False
try:
    for phase in ('cold', 'restart'):
        if phase=='restart':
            adb('shell','am','force-stop',PKG)
            print(adb('shell','am','start','-W','-n',PKG+'/com.ethk3.novaonepro.MainActivity'),flush=True)
        deadline=time.monotonic()+180;ready_since=None;previous=None
        while time.monotonic()<deadline:
            p=prefs('v20_service_runtime_light')
            row={k:v for k,v in p.items() if k.startswith('init_') or k in ('service_ready_at','user_enabled')}
            row['phase']=phase;rows.append(row)
            if row!=previous:print(json.dumps(row,ensure_ascii=False),flush=True);previous=row
            assert not row.get('init_last_error'), 'STARTUP_ERROR: '+row.get('init_last_error','')
            assert row.get('init_retry_count','0')=='0','STARTUP_RETRIED'
            if row.get('init_status')=='READY':
                if ready_since is None:ready_since=time.monotonic()
                if time.monotonic()-ready_since>=60:break
            else:ready_since=None
            time.sleep(2)
        else:raise AssertionError('Startup never remained READY for 60 seconds: '+phase)
        assert not prefs('v20_crash_snapshot').get('type'),'Caught failure: '+json.dumps(prefs('v20_crash_snapshot'))
    ready=True
finally:
    (out/'startup.json').write_text(json.dumps({'ready':ready,'samples':rows,'crash':prefs('v20_crash_snapshot')},ensure_ascii=False,indent=2))
    print(json.dumps(prefs('v20_crash_snapshot'),ensure_ascii=False),flush=True)
    (out/'logcat.txt').write_text(adb('logcat','-d'))
    adb('shell','uiautomator','dump','/sdcard/startup.xml')
    adb('pull','/sdcard/startup.xml',str(out/'startup.xml'))
    adb('shell','screencap','-p','/sdcard/startup.png')
    adb('pull','/sdcard/startup.png',str(out/'startup.png'))
assert ready,'Full default-enabled startup never became READY; see startup-evidence'
