#!/usr/bin/env python3
"""Build a cumulative, hash-verified source/APK/evidence delivery from an exact Git tree index."""
import argparse, hashlib, json, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def sha(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for b in iter(lambda:f.read(1048576),b''): h.update(b)
    return h.hexdigest()

def main():
    p=argparse.ArgumentParser();p.add_argument('--source-index',type=Path,required=True)
    p.add_argument('--source-commit',required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
    entries=json.loads(a.source_index.read_text());entries=entries.get('tree',entries) if isinstance(entries,dict) else entries
    entries=[x for x in entries if x.get('type','blob')=='blob']
    ledger=json.loads((ROOT/'reports/r939/ACCEPTANCE_LEDGER.json').read_text())
    release=json.loads((ROOT/'delivery/r939/release-artifact.json').read_text())
    overlay=json.loads((ROOT/'reports/r939/overlay/OVERLAY_ACCEPTANCE.json').read_text())
    assert ledger['passed']==ledger['total']==64
    assert overlay['status']=='PASS' and overlay['new_apk_sha256']==release['apk_sha256']
    assert 'OK (4 tests)' in (ROOT/'reports/r939/MAIN_CHAIN_ANDROID.txt').read_text()
    assert sha(ROOT/release['apk_path'])==release['apk_sha256']
    payload={}
    for row in entries:
        path=row['path'];f=ROOT/path;assert f.is_file(),path
        data=f.read_bytes();gitsha=hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
        assert gitsha==row['sha'],'SOURCE_TREE_DRIFT:'+path
        payload['02_COMPLETE_SOURCE/'+path]=f
    payload['01_APK/'+Path(release['apk_path']).name]=ROOT/release['apk_path']
    for directory,prefix in [('reports/r939','04_VERIFICATION'),('delivery/r939/evidence','04_VERIFICATION/APK')]:
        for f in (ROOT/directory).rglob('*'):
            if f.is_file() and str(f.relative_to(ROOT)) in {x['path'] for x in entries}:
                payload[prefix+'/'+str(f.relative_to(ROOT/directory))]=f
    payload['03_CHANGE_LEDGER/CHANGE_LEDGER.json']=ROOT/'reports/r939/CHANGE_LEDGER.json'
    payload['05_PROGRESS/ACCEPTANCE_LEDGER.json']=ROOT/'reports/r939/ACCEPTANCE_LEDGER.json'
    payload['05_PROGRESS/STAGE_PROGRESS.json']=ROOT/'reports/r932/STAGE_PROGRESS.json'
    payload['06_ROLLBACK/R929_BASELINE.apk']=ROOT/'delivery/r929-final/Starcore-Tianshu-V57.21.1.0.4-R5.4.1.0.4-929-OVERLAY.apk'
    payload['07_BUILD_REPRO/HANDOFF_ZH.md']=ROOT/'delivery/r939/HANDOFF_ZH.md'
    for row in entries:
        path=row['path']
        if path.startswith('docs/5.4.2/'):
            payload['08_PLAN_SNAPSHOT/'+path[len('docs/5.4.2/'):]]=ROOT/path
        elif path.startswith('app/src/main/assets/'):
            payload['09_DATA_AND_ASSETS/'+path[len('app/src/main/assets/'):]]=ROOT/path
    payload['README_FIRST_ZH.md']=ROOT/'delivery/r939/HANDOFF_ZH.md'
    info={'version':release['versionName'],'versionCode':release['versionCode'],'source_commit':a.source_commit,
          'source_file_count':len(entries),'source_materialization':'ALL_BLOBS_MATCH_GIT_TREE',
          'engineering_acceptance':{'passed':64,'total':64},'android_scope':'EMULATOR_NOT_USER_PHONE',
          'physical_device_verified':False,'future_oos_effect_proven':False,
          'apk_sha256':release['apk_sha256'],'original_input_parts_preserved':True}
    a.out.mkdir(parents=True,exist_ok=True)
    dest=a.out/'STARCORE_5.4.1.0.8_R939_BATCH34_CUMULATIVE_TOTAL.zip'
    manifest=[]
    with zipfile.ZipFile(dest,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
        for name,f in sorted(payload.items()):
            z.write(f,name);manifest.append(sha(f)+'  '+name)
        data=(json.dumps(info,ensure_ascii=False,indent=2)+'\n').encode()
        z.writestr('PACKAGE_INFO.json',data);manifest.append(hashlib.sha256(data).hexdigest()+'  PACKAGE_INFO.json')
        z.writestr('MANIFEST_SHA256.txt','\n'.join(manifest)+'\n')
    with zipfile.ZipFile(dest) as z:assert z.testzip() is None
    receipt={'path':str(dest.resolve()),'bytes':dest.stat().st_size,'sha256':sha(dest),'files':len(payload)+2,**info}
    (a.out/'PACKAGE_RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(receipt,ensure_ascii=False))

if __name__=='__main__':main()
