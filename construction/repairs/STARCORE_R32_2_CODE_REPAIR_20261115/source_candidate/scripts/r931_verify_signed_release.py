#!/usr/bin/env python3
"""Bind an owner-signed APK to the exact source/resource/asset and build input hashes."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def verify():
    record = json.loads((ROOT/'delivery/r939/release-artifact.json').read_text())
    for name, wanted in record['build_inputs'].items():
        if digest(ROOT/name) != wanted: raise RuntimeError('R931_BUILD_INPUT_DRIFT:'+name)
    actual_files = {str(p.relative_to(ROOT)) for p in (ROOT/'app/src/main').rglob('*') if p.is_file()}
    if actual_files != {n for n in record['build_inputs'] if n.startswith('app/src/main/')}:
        raise RuntimeError('R931_PRODUCTION_FILE_SET_DRIFT')
    if digest(ROOT/record['apk_path']) != record['apk_sha256']: raise RuntimeError('R931_APK_DRIFT')
    if record['certificate_sha256'] != record['predecessor_certificate_sha256']:
        raise RuntimeError('R931_CERTIFICATE_CHANGED')
    manifest = json.loads((ROOT/'release/release_manifest.json').read_text())
    for key in ('applicationId','versionName','versionCode'):
        if record[key] != manifest[key]: raise RuntimeError('R931_IDENTITY_DRIFT:'+key)
    if record['versionCode'] <= record['predecessor_versionCode']: raise RuntimeError('R931_NOT_UPGRADE')
    return record

if __name__=='__main__':
    r=verify()
    print(json.dumps({'status':'PASS','build_inputs':len(r['build_inputs']),
                      'apk_sha256':r['apk_sha256'],'physical_device':'NOT_RUN',
                      'plan_completion':'SEPARATE_ACCEPTANCE_LEDGER'},indent=2))
