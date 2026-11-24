"""Capture signed UI export and reopen the saved Android DocumentsProvider archives."""
from pathlib import Path
import hashlib
import json
import os
import re
import subprocess
import sys
import time

work = Path(os.environ['GITHUB_WORKSPACE'])
temp = Path(os.environ['RUNNER_TEMP'])
helper = work / '.github/r34-report-readback/android.py'
root = temp / 'test-documents'
source = os.environ['R341_SOURCE']
version = int(os.environ['R341_VERSION'])
old_source = '5562f9b19aa09f9f7d9e69ca085db56c537967fc'
old_hash = '5b887677a727e3b8d07362c02bc3cd497ae26de052944e8b399b5cefc9d96cc6'
package = 'com.starcore.research.r13'


def sha(path):
    with path.open('rb') as raw:
        return hashlib.file_digest(raw, 'sha256').hexdigest()


def observed(value, version_code, adb):
    path = adb('shell', 'pm', 'path', package).strip().split(':', 1)[1]
    raw = adb('shell', 'dumpsys', 'package', package)
    assert f'versionCode={version_code} ' in raw
    uid = int(re.search(r'\b(?:userId|appId)=(\d+)', raw)[1])
    assert adb('shell','sha256sum',path).split()[0] == value
    return {'versionCode':version_code, 'uid':uid, 'apkSha256':value}


def evidence():
    suffix = str(version)[-3:]
    definition = helper.read_text().split('\nimport importlib.util\n', 1)[0]
    definition = definition.replace('starcore121r', f'starcore{suffix}r')
    namespace = {}
    exec(compile(definition, str(helper), 'exec'), namespace)
    adb = namespace['adb']
    serial = namespace['DEVICE']
    assert serial.startswith('emulator-') and adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1'
    assert adb('shell','getprop','ro.build.version.sdk').strip()=='34'
    assert not adb('shell', 'pm', 'list', 'packages', package).strip()
    adb('root')
    adb('wait-for-device')
    adb('shell','cmd','connectivity','airplane-mode','enable')
    adb('shell','svc','wifi','disable')
    assert adb('shell','settings','get','global','airplane_mode_on').strip()=='1'
    assert not adb('shell','ip','route','show').strip()
    adb('shell','wm','size','480x850')
    adb('shell','wm','density','195')
    adb('shell','settings','put','system','screen_off_timeout','7200000')
    adb('shell','svc','power','stayon','true')
    assert sha(temp/'baseline122.apk') == old_hash
    candidate = temp / 'candidate.apk'
    delivery = json.loads((temp/'received-delivery.json').read_bytes())
    candidate_hash = sha(candidate)
    assert candidate_hash == delivery['apk']['sha256'] and delivery['sourceCommit'] == source
    apk_identity = delivery['r341']
    assert apk_identity['versionCode']==version and apk_identity['sourceCommit']==source
    result = {'scope':'ORIGINAL_SIGNED_ANDROID_API34_OFFLINE_UI_AND_SAF', 'physicalDevice':False,
              'api':34, 'serial':serial, 'baselineSource':old_source, 'sourceCommit':source,
              'apkSha256':candidate_hash,'guiDriverSha256':sha(helper),'reports':[]}
    def save():
        (root/'report-campaign.json').write_text(json.dumps(result, indent=2, ensure_ascii=False)+'\n')
    def export_report(name,profile,bound_source):
        result['phase']=name
        save()
        namespace['commit']=bound_source
        namespace['out']={'reportReadbacks':result['reports'],'activeProfile':profile}
        namespace['report_entry']('另存完整原始备份（体积大）' if profile=='FULL_RAW' else namespace['CREATE'])
        record=namespace['save_report'](namespace['wait_for_report'](name),name)
        assert record['rawEvidenceIncluded'] == (profile=='FULL_RAW')
        result['reports'].append(dict(**record,sourceCommit=bound_source))
        save()
        print(name,record['bytes'],record['sha256'],flush=True)
    try:
        assert 'Success' in adb('install',str(temp/'baseline122.apk'),timeout=240)
        result['before']=observed(old_hash,577311122,adb)
        adb('shell','pm','grant',package,'android.permission.POST_NOTIFICATIONS')
        export_report('baseline-1','COMPACT_DIAGNOSTIC',old_source)
        adb('shell','am','force-stop',package)
        assert 'Success' in adb('install','-r',str(candidate),timeout=240)
        result['after']=observed(candidate_hash,version,adb)
        assert result['after']['uid']==result['before']['uid']
        adb('shell','pm','grant',package,'android.permission.POST_NOTIFICATIONS')
        for i,profile in enumerate(('COMPACT_DIAGNOSTIC','FULL_RAW','COMPACT_DIAGNOSTIC'),1):
            export_report(f'candidate-{i}',profile,source)
        result['status']='THREE_DISTINCT_SIGNED_CANDIDATE_REPORTS_REOPENED'
    except Exception as error:
        result.update(status='REPORT_CAMPAIGN_INCOMPLETE',errorType=type(error).__name__)
        if re.fullmatch(r'[A-Z0-9_]{1,120}',str(error)):
            result['errorCode']=str(error)
        raise
    finally:
        save()


if __name__=='__main__':
    evidence()
