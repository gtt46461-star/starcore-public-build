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
    for i in range(60):
        p=prefs('v20_service_runtime_light')
        row={k:v for k,v in p.items() if k.startswith('init_') or k in ('service_ready_at','user_enabled')}
        rows.append(row)
        if i%5==0 or row.get('init_last_error'):print(json.dumps(row,ensure_ascii=False),flush=True)
        if row.get('init_status')=='READY':ready=True;break
        if row.get('init_last_error'):break
        time.sleep(2)
finally:
    (out/'startup.json').write_text(json.dumps({'ready':ready,'samples':rows,'crash':prefs('v20_crash_snapshot')},ensure_ascii=False,indent=2))
    print(json.dumps(prefs('v20_crash_snapshot'),ensure_ascii=False),flush=True)
    (out/'logcat.txt').write_text(adb('logcat','-d'))
    adb('shell','uiautomator','dump','/sdcard/startup.xml')
    adb('pull','/sdcard/startup.xml',str(out/'startup.xml'))
    adb('shell','screencap','-p','/sdcard/startup.png')
    adb('pull','/sdcard/startup.png',str(out/'startup.png'))
assert ready,'Full default-enabled startup never became READY; see startup-evidence'
