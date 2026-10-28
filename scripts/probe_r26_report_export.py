#!/usr/bin/env python3
"""Actual APK UI -> diagnostics process -> hashed complete ZIP; no custom test export owner."""
from pathlib import Path
import glob
import hashlib
import io
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
import zipfile
from wait_r13_android_ready import wait_ready

PKG='com.starcore.research.r13'
ROOT='files/r981_reports'
OUT=Path('report-export-probe');OUT.mkdir(exist_ok=True)


def adb(*args,check=True):
    r=subprocess.run(['adb',*args],capture_output=True,timeout=45)
    if check and r.returncode:raise RuntimeError('ADB_'+args[0]+': '+r.stderr.decode('utf-8','replace')[:1000])
    return r.stdout


def private(path):return adb('exec-out','run-as',PKG,'cat',path,check=False)
def json_file(path):
    try:return json.loads(private(path))
    except (ValueError,UnicodeError):return None


def dump_ui(name):
    adb('shell','uiautomator','dump','/sdcard/report-probe.xml',check=False)
    raw=adb('exec-out','cat','/sdcard/report-probe.xml',check=False);view=ET.fromstring(raw)
    safe=ET.fromstring(raw)
    for node in safe.iter('node'):
        for key in ['text','content-desc','hint']:node.attrib.pop(key,None)
    (OUT/(name+'.xml')).write_bytes(ET.tostring(safe,encoding='utf-8'))
    return view


def full_report_job():
    # Retention work can advance the shared current pointer after a report publishes.
    listing=adb('exec-out','run-as',PKG,'find',ROOT+'/jobs','-name','job.json',check=False).decode('utf-8','replace')
    found=[]
    for path in listing.splitlines():
        if re.fullmatch(re.escape(ROOT)+r'/jobs/[0-9a-f]{64}/job.json',path):
            row=json_file(path)
            if row and row.get('mode')=='FULL_REPORT':found.append(row)
    return max(found,key=lambda x:x['createdWallMs']) if found else None


def main():
    receipt={'schema':'R26_REPORT_UI_TO_ZIP_V1','status':'RUNNING','scope':'API29_ACTUAL_APP_UI_AND_DIAGNOSTIC_EXPORT_NOT_USER_DATABASE_OR_STRATEGY_QUALITY','sourceCommit':Path('delivered/package-receipt/source-commit.txt').read_text().strip(),'packageVersion':577271028}
    assert receipt['sourceCommit']=='60f2bfad6d75ae55e1ef1faa6053c1941c5fd8fa'
    started=time.monotonic();stages=[]
    try:
        wait_ready();apk=glob.glob('delivered/app/build/outputs/apk/debug/*.apk');assert len(apk)==1
        adb('shell','input','keyevent','224');adb('shell','wm','dismiss-keyguard',check=False)
        adb('shell','settings','put','system','screen_off_timeout','1800000')
        adb('shell','svc','power','stayon','true',check=False)
        assert b'Success' in adb('install','-r',apk[0]);assert b'Success' in adb('shell','pm','clear',PKG)
        package=adb('shell','dumpsys','package',PKG).decode('utf-8','replace');assert 'versionCode=577271028' in package
        adb('shell','am','start','-W','-n',PKG+'/com.ethk3.novaonepro.MainActivity')
        deadline=time.monotonic()+180
        while time.monotonic()<deadline:
            try:
                prefs=ET.fromstring(private('shared_prefs/v20_service_runtime_light.xml'));values={e.attrib['name']:e.text or e.attrib.get('value','') for e in prefs}
            except ET.ParseError:values={}
            if values.get('init_status')=='READY' and values.get('init_stage')=='BUSINESS_READY':break
            time.sleep(1)
        else:raise RuntimeError('APP_NOT_READY_FOR_REPORT')
        # Use the ordinary visible diagnosis button, with the screen awake throughout.
        selected=None
        for scroll in range(30):
            view=dump_ui('before-export-'+str(scroll))
            for node in view.iter('node'):
                if node.get('resource-id','').endswith(':id/btn_open_diagnosis'):
                    bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
                    if len(bounds)==4 and bounds[2]>bounds[0] and bounds[3]-bounds[1]>24:selected=bounds;break
            if selected is not None:break
            screen=list(map(int,re.findall(r'\d+',next(view.iter('node')).get('bounds',''))))
            width,height=screen[2],screen[3]
            adb('shell','input','swipe',str(width//2),str(height*4//5),str(width//2),str(height//3),'450')
            time.sleep(.5)
        assert selected is not None,'VISIBLE_DIAGNOSIS_BUTTON_MISSING'
        x=(selected[0]+selected[2])//2;y=(selected[1]+selected[3])//2
        adb('shell','input','tap',str(x),str(y))
        receipt['uiTrigger']='VISIBLE_MAIN_ACTIVITY_DIAGNOSIS_BUTTON'
        deadline=time.monotonic()+480;job=None;firstPoll=time.monotonic()
        while time.monotonic()<deadline:
            pointer=json_file(ROOT+'/current-job.json')
            if pointer and re.fullmatch('[0-9a-f]{64}',pointer.get('jobId','')):
                candidate=json_file(ROOT+'/jobs/'+pointer['jobId']+'/job.json')
                if candidate and candidate.get('mode')=='FULL_REPORT':job=candidate
            if job is None or time.monotonic()-firstPoll>10:job=full_report_job() or job
            if job is None and time.monotonic()-firstPoll>45:
                raise RuntimeError('UI_EXPORT_DID_NOT_REGISTER_FULL_REPORT_JOB')
            if job:
                stage=job['stage']
                if not stages or stages[-1]['stage']!=stage:stages.append({'stage':stage,'elapsedSeconds':round(time.monotonic()-started,3)});print('REPORT_EXPORT_STAGE='+json.dumps(stages[-1]),flush=True)
                if stage=='FAILED':raise RuntimeError('REPORT_EXPORT_FAILED:'+job.get('lastReason',''))
                if stage=='READY_TO_SHARE':break
            time.sleep(1)
        else:raise RuntimeError('REPORT_EXPORT_NOT_READY:'+json.dumps(stages))
        base=ROOT+'/jobs/'+job['jobId'];index=json_file(base+'/delivery/index.json');assert index and index['format']=='SCREPORT_SINGLE_ZIP_R992'
        assert len(index['files'])==1;item=index['files'][0];assert item['name']=='STARCORE_FULL_REPORT.zip' and item['kind']=='SINGLE_ZIP'
        archive=adb('exec-out','run-as',PKG,'cat',base+'/delivery/STARCORE_FULL_REPORT.zip')
        digest=hashlib.sha256(archive).hexdigest();assert len(archive)==item['bytes'] and digest==item['sha256']
        with zipfile.ZipFile(io.BytesIO(archive)) as z:
            assert z.testzip() is None
            names=z.namelist();assert 'DIAGNOSTIC_SCOPE.json' in names and 'R26_REPORT.json' in names and 'REPORT_DETAILS.txt' in names
            assert 'original-failure.txt' not in names
            scope=json.loads(z.read('DIAGNOSTIC_SCOPE.json'));campaign=next(x for x in scope['tables'] if x['table']=='su2_campaign')
            assert any(c['name']=='campaign_id' and c['primaryKeyOrder']>0 for c in campaign['columns'])
            catalog=[]
            for name in names:
                if re.search(r'catalog-[0-9]+\.jsonl$',name):catalog.extend(json.loads(x) for x in z.read(name).splitlines() if x)
            contracts=[x for x in catalog if x.get('kind')=='check_contract'];results=[x for x in catalog if x.get('kind')=='check_result']
            assert len(contracts)==len(results)==96 and len({x['id'] for x in results})==96
            r26=json.loads(z.read('R26_REPORT.json'));assert len(r26['sections'])==40
        receipt.update(status='PASS_UI_TO_FINAL_ZIP',elapsedSeconds=round(time.monotonic()-started,3),zipBytes=len(archive),zipSha256=digest,zipEntries=len(names),diagnosticScopeIncludesActualWithoutRowidCampaign=True,canonicalCheckContracts=96,canonicalCheckResults=96,r26ReportSections=40,evidenceComplete=index['evidenceComplete'],checksComplete=index['checksComplete'],allChecksPassed=index['allChecksPassed'],checksScope='Execution completeness and health remain distinct; this is not a guarantee that every health check passes',stages=stages)
    except Exception as error:
        receipt.update(status='FAIL',error=repr(error),stages=stages);raise
    finally:
        if receipt['status']=='FAIL':
            try:
                dump_ui('failure-ui')
                listing=adb('exec-out','run-as',PKG,'find',ROOT,'-maxdepth','3','-type','f',check=False);(OUT/'report-files.txt').write_bytes(listing)
                (OUT/'report-logcat.txt').write_bytes(adb('logcat','-d','-s','ReportExportCP12:V','AndroidRuntime:E','*:S',check=False))
                failures=[]
                for path in listing.decode('utf-8','replace').splitlines():
                    if re.fullmatch(re.escape(ROOT)+r'/requests/[0-9a-f]{64}\.ack\.json',path):
                        row=json_file(path)
                        if row:failures.append({k:row.get(k) for k in ['requestId','jobId','mode','stage','reason']})
                receipt['requestAcknowledgements']=failures
            except Exception as observation:receipt['failureObservationError']=repr(observation)
        (OUT/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
        print('REPORT_EXPORT_UI_TO_ZIP_RECEIPT='+json.dumps(receipt,ensure_ascii=False),flush=True)


if __name__=='__main__':main()
