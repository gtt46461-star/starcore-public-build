#!/usr/bin/env python3
"""Verify the checked-in owner-signed APK against the exact production build inputs."""
import hashlib, json, subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
    return h.hexdigest()
def verify():
    record=json.loads((ROOT/'delivery/r928-runtime/release-artifact.json').read_text())
    for name,wanted in record['build_inputs'].items():
        if sha(ROOT/name)!=wanted:raise RuntimeError('RELEASE_SOURCE_DRIFT:'+name)
    if sha(ROOT/record['apk_path'])!=record['apk_sha256']:raise RuntimeError('RELEASE_APK_DRIFT')
    if record['certificate_sha256']!=record['previous_r926_certificate_sha256']:raise RuntimeError('CERTIFICATE_DRIFT')
    if record['versionCode']<=record['previous_versionCode']:raise RuntimeError('VERSION_NOT_UPGRADE')
    manifest=json.loads((ROOT/'release/release_manifest.json').read_text())
    for key in ['versionCode','versionName','applicationId']:
        if record[key]!=manifest[key]:raise RuntimeError('RELEASE_IDENTITY_DRIFT:'+key)
    return record
if __name__=='__main__':
    info=verify()
    print(json.dumps({'status':'PASS','build_inputs':len(info['build_inputs']),'apk_sha256':info['apk_sha256'],'certificate_sha256':info['certificate_sha256'],'physical_device':'NOT_RUN'},indent=2))
