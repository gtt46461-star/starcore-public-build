#!/usr/bin/env python3
"""Record actual debug APK identity and create bounded transfer parts.

This receipt explicitly does not prove original-signature upgrade acceptance.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

root = Path(__file__).resolve().parents[1]
out = root / 'current-evidence'
out.mkdir(exist_ok=True)
apk_directory = root / 'app/build/outputs/apk/debug'
metadata_path = apk_directory / 'output-metadata.json'
if not metadata_path.is_file():
    raise SystemExit('CURRENT_DEBUG_APK_METADATA_MISSING')
metadata = json.loads(metadata_path.read_text())
expected = json.loads((root/'release/release_manifest.json').read_text())
elements = metadata.get('elements', [])
if (metadata.get('variantName') != 'debug'
        or metadata.get('applicationId') != expected['applicationId']
        or metadata.get('artifactType', {}).get('type') != 'APK'
        or len(elements) != 1):
    raise SystemExit('CURRENT_DEBUG_APK_METADATA_IDENTITY')
element = elements[0]
filename = element.get('outputFile', '')
if (Path(filename).name != filename or not filename.endswith('.apk')
        or element.get('versionName') != expected['versionName']
        or element.get('versionCode') != expected['versionCode']):
    raise SystemExit('CURRENT_DEBUG_APK_OUTPUT_IDENTITY')
apk = apk_directory / filename
if not apk.is_file():
    raise SystemExit('CURRENT_DEBUG_APK_MISSING')
build_tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
receipt = {
    'source_commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root).decode().strip(),
    'artifact_kind': 'DEBUG_VERIFICATION_ONLY',
    'upgrade_status': 'ORIGINAL_SIGNATURE_NOT_VERIFIED',
    'release_status': 'NOT_CUSTOMER_RELEASE',
    'output_metadata': metadata,
    'output_metadata_sha256': hashlib.sha256(metadata_path.read_bytes()).hexdigest(),
    'source_apk_path': str(apk.relative_to(root)),
    'bytes': apk.stat().st_size,
    'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
    'commands': [],
    'parts': [],
}
for name, command in [('apk-badging', [build_tools/'aapt', 'dump', 'badging', apk]),
                      ('apk-signature', [build_tools/'apksigner', 'verify', '--verbose', '--print-certs', apk])]:
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True)
    (out/(name+'.log')).write_text(result.stdout+result.stderr)
    receipt['commands'].append({'name': name, 'command': list(map(str, command)), 'exit_code': result.returncode})
    if result.returncode:
        raise SystemExit(name+'_FAILED')
    if name == 'apk-badging':
        identity = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", result.stdout, re.M)
        if not identity or identity.groups() != (expected['applicationId'], str(expected['versionCode']), expected['versionName']):
            raise SystemExit('CURRENT_DEBUG_APK_PACKAGED_IDENTITY')
with zipfile.ZipFile(apk) as archive:
    provenance = root/'app/build/generated/r815/source-manifest.json'
    if not provenance.is_file() or archive.read('assets/diagnostics/source-provenance.json') != provenance.read_bytes():
        raise SystemExit('CURRENT_PACKAGED_SOURCE_PROVENANCE_MISMATCH')
    receipt['source_provenance'] = json.loads(provenance.read_text())
    receipt['source_provenance_sha256'] = hashlib.sha256(provenance.read_bytes()).hexdigest()
    receipt['native_libraries'] = {
        name: hashlib.sha256(archive.read(name)).hexdigest()
        for name in sorted(archive.namelist()) if name.startswith('lib/') and name.endswith('.so')
    }
    required_native = {'lib/'+abi+'/libstarcore_report_cp4.so' for abi in ['arm64-v8a','armeabi-v7a','x86_64']}
    if not required_native.issubset(receipt['native_libraries']):
        raise SystemExit('CURRENT_DEBUG_APK_NATIVE_ABI_MISSING')
with apk.open('rb') as stream:
    for index in range(1, 6):
        block = stream.read(24*1024*1024)
        if not block:
            break
        path = out/('app-debug.apk.part-'+str(index))
        path.write_bytes(block)
        receipt['parts'].append({'path': path.name, 'bytes': len(block), 'sha256': hashlib.sha256(block).hexdigest()})
    if stream.read(1):
        raise SystemExit('APK_TRANSFER_PART_LIMIT_EXCEEDED')
(out/'CURRENT_DEBUG_APK_RECEIPT.json').write_text(json.dumps(receipt, indent=2)+'\n')
print(json.dumps({'sha256': receipt['sha256'], 'bytes': receipt['bytes'], 'parts': len(receipt['parts']),
                  'scope': receipt['artifact_kind']}))
