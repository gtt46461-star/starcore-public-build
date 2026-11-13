#!/usr/bin/env python3
"""Exact signed product: original live test and sixteen real UI readers.

Runs only on a rooted disposable emulator. Raw protocol/UI/database stay private.
No business inputs, schema or predictions are inserted by this harness.
"""
import argparse
import hashlib
import json
import re
import subprocess
import tempfile
import shutil
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PKG = 'com.starcore.research.r13'


def run(*args, check=True, timeout=30):
    return subprocess.run(args, capture_output=True, check=check, timeout=timeout)


def adb(*args, timeout=30, check=True):
    return run('adb', *args, timeout=timeout, check=check).stdout.decode(errors='replace').strip()


def only(root, pattern):
    paths = list(root.rglob(pattern))
    assert len(paths) == 1, 'EXACT_ARTIFACT_FILE_REQUIRED:' + pattern
    return paths[0]


def hierarchy():
    adb('shell', 'uiautomator', 'dump', '/sdcard/r31-final-ui.xml')
    return ET.fromstring(adb('shell', 'cat', '/sdcard/r31-final-ui.xml'))


def tap(node):
    points = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
    assert len(points) == 4 and points[2] > points[0] and points[3] > points[1], 'UI_NODE_NOT_VISIBLE'
    adb('shell', 'input', 'tap', str((points[0]+points[2])//2), str((points[1]+points[3])//2))
    time.sleep(.4)


def click(predicate, scrolls=0):
    width, height = map(int, re.findall(r'(\d+)x(\d+)', adb('shell', 'wm', 'size'))[-1])
    for attempt in range(scrolls+1):
        for node in hierarchy().iter('node'):
            if predicate(node) and node.get('enabled') != 'false':
                tap(node)
                return
        if attempt < scrolls:
            adb('shell', 'input', 'swipe', str(width//2), str(height*8//10), str(width//2), str(height*3//10), '250')
            time.sleep(.3)
    raise AssertionError('ACTUAL_UI_CONTROL_NOT_FOUND')


def read_tables(out, source, apk_sha):
    adb('shell', 'am', 'start', '-W', '-n', PKG+'/com.ethk3.novaonepro.ResearchTablesActivityR31')
    result = {'sourceCommit': source, 'apkSha256': apk_sha,
              'scope': 'ACTUAL_PRODUCTION_ACTIVITY_READONLY_PAGED_READERS; EMPTY_IS_NOT_EFFECTIVENESS',
              'physicalDevice': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN', 'reads': [], 'state': 'INCOMPLETE'}
    path = out/'TABLE_UI_READERS.json'
    for index in range(1, 17):
        table = 'T%02d' % index
        try:
            click(lambda node: node.get('class') == 'android.widget.Spinner')
            click(lambda node: node.get('text') == table, scrolls=8)
            click(lambda node: node.get('text') == '读取')
            until = time.monotonic()+25
            record = None
            while time.monotonic() < until:
                for node in hierarchy().iter('node'):
                    text = node.get('text', '')
                    if not text.startswith('{'):
                        continue
                    try:
                        value = json.loads(text)
                    except json.JSONDecodeError:
                        continue
                    if value.get('id') == table and value.get('status'):
                        record = {'id': table, 'actualStatus': value['status'],
                                  'shown': value.get('shown'), 'bytes': value.get('bytes'),
                                  'readOnly': value.get('readOnly')}
                        reason=str(value.get('reason',''))
                        public='\n'.join(p.read_text(errors='replace') for p in Path('app/src/main/java').rglob('*.java'))
                        record['failureClass']=reason.split(':',1)[0] if re.fullmatch(r'[A-Za-z0-9_.$]+',reason.split(':',1)[0]) else 'UNLISTED'
                        identifiers=re.findall(r'no such (?:table|column): ([A-Za-z_][A-Za-z0-9_.]*)',reason)
                        record['missingPublicSchemaIdentifiers']=[name for name in identifiers if name in public]
                        record['errorSymbols']=sorted(set(re.findall(r'\b[A-Z][A-Z0-9_]{2,80}\b',reason)).intersection(re.findall(r'\b[A-Z][A-Z0-9_]{2,80}\b',public)))
                if record:
                    break
                time.sleep(.4)
            assert record is not None, 'ACTUAL_TABLE_READ_DID_NOT_RETURN'
            result['reads'].append(record)
        except Exception as failure:
            result['reads'].append({'id': table, 'actualStatus': 'HARNESS_OR_UI_FAILURE', 'failureClass': type(failure).__name__})
        path.write_text(json.dumps(result, indent=2)+'\n')
        print('TABLE_READER_STATUS|'+json.dumps(result['reads'][-1]),flush=True)
    valid = len(result['reads']) == 16 and all(row.get('actualStatus') in ['READ_OK', 'EMPTY_NO_ROWS'] and row.get('readOnly') is True for row in result['reads'])
    result['state'] = 'OBSERVED_16_ACTUAL_READERS' if valid else 'FAILED_OR_MISSING_READER'
    path.write_text(json.dumps(result, indent=2)+'\n')
    return valid


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--built', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    p.add_argument('--source', required=True)
    p.add_argument('--mode', choices=['both','live','tables'], default='both')
    args = p.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    assert adb('shell', 'getprop', 'ro.kernel.qemu') == '1' and adb('shell', 'id', '-u') == '0', 'ROOTED_DISPOSABLE_EMULATOR_REQUIRED'
    assert only(args.built, 'source-commit.txt').read_text().strip() == args.source, 'SOURCE_BINDING_MISMATCH'
    apk, test = only(args.built, '*-release.apk'), only(args.built, '*-androidTest.apk')
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    adb('install', '-r', '-g', str(apk), timeout=90)
    adb('install', '-r', '-t', str(test), timeout=90)
    installed = adb('shell', 'pm', 'path', PKG).split(':',1)[1]
    assert hashlib.sha256(run('adb','exec-out','cat',installed,timeout=90).stdout).hexdigest() == digest, 'INSTALLED_RELEASE_BYTES_DIFFER'
    adb('shell', 'pm', 'grant', PKG, 'android.permission.POST_NOTIFICATIONS', check=False)
    if args.mode=='tables':
        adb('shell','am','start','-W','-n',PKG+'/com.ethk3.novaonepro.MainActivity')
        until=time.monotonic()+60
        while True:
            try:
                click(lambda node:node.get('resource-id','').endswith('/btn_start'))
                break
            except AssertionError:
                if time.monotonic()>=until:raise
                time.sleep(1)
        first=read_tables(args.out,args.source,digest)
        shutil.copyfile(args.out/'TABLE_UI_READERS.json',args.out/'TABLE_UI_READERS_FIRST.json')
        time.sleep(60)
        second=read_tables(args.out,args.source,digest)
        return 0 if second else 1
    with tempfile.TemporaryDirectory(prefix='r31-final-private-') as temporary:
        private = Path(temporary)
        command = ['adb','shell','am','instrument','-w','-r','-e','r30Sandbox','r31-final-current-live','-e','r31qRealService','true','-e','class','com.ethk3.novaonepro.R31QDeliveryInstrumentedTest#liveServiceFetchesFreezesAndSettlesNewFutureObservations',PKG+'.test/com.ethk3.novaonepro.R30FaultAndroidRunner']
        # Observer ceiling includes the unchanged 180s schema ready and 360s production test windows.
        protocol = run(*command, check=False, timeout=600)
        raw = protocol.stdout+protocol.stderr
        text = raw.decode(errors='replace')
        logs=adb('logcat','-d','-v','threadtime')
        marker='R31Q_ANDROID|REAL_API_FETCH_AND_NEW_FREEZE_SETTLEMENT|'
        passed = protocol.returncode == 0 and bool(re.search(r'OK \(1 tests?\)', text)) and marker in text+logs and 'FAILURES!!!' not in text
        receipt = {'sourceCommit':args.source,'apkSha256':digest,'testPassed':passed,
                   'scope':'UNCHANGED_ORIGINAL_LIVE_TEST_ON_EXACT_SIGNED_RELEASE',
                   'protocolSha256':hashlib.sha256(raw).hexdigest(),
                   'publicFailures':sorted(set(re.findall(r'\b(?:NO_FRESH_API_FETCH|NO_NEW_FUTURE_FREEZE|NO_NEW_FROZEN_SETTLEMENT|LEGACY_NOT_READY)\b',text))),
                   'physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN'}
        (args.out/'SIGNED_LIVE_RESULT.json').write_text(json.dumps(receipt,indent=2)+'\n')
        tables = read_tables(args.out,args.source,digest) if args.mode=='both' else True
        print(json.dumps({'liveTestPassed':passed,'sixteenActualReaders':tables,'apkSha256':digest,'sourceCommit':args.source}))
        return 0 if passed and tables else 1


if __name__ == '__main__':
    raise SystemExit(main())
