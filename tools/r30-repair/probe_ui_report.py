#!/usr/bin/env python3
"""Exercise the installed app's real report button on a disposable rooted emulator.

No database, preferences, models or service controls are injected. The UI starts
the production exporter; the published ZIP is read back and verified on the host.
"""
import argparse
import hashlib
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

PKG = 'com.starcore.research.r13'
ROOT = f'/data/user/0/{PKG}/files/r981_reports'
parser = argparse.ArgumentParser()
parser.add_argument('--out', type=Path, required=True)
args = parser.parse_args()
args.out.mkdir(parents=True, exist_ok=True)


def adb(*argv, timeout=30):
    result = subprocess.run(['adb', *argv], capture_output=True, text=True, timeout=timeout)
    if result.returncode:
        raise RuntimeError('ADB_COMMAND_FAILED:' + argv[0] + ':' + result.stderr[-800:])
    return result.stdout


def capture(label):
    remote = '/sdcard/r30-ui-report.png'
    adb('shell', 'screencap', '-p', remote)
    adb('pull', remote, str(args.out / (label + '.png')))


def hierarchy():
    remote = '/sdcard/r30-ui-report.xml'
    adb('shell', 'uiautomator', 'dump', remote)
    text = adb('shell', 'cat', remote)
    (args.out / 'last-ui.xml').write_text(text)
    return ET.fromstring(text)


def click_text(label, scrolls=0):
    size = re.findall(r'(\d+)x(\d+)', adb('shell', 'wm', 'size'))[-1]
    width, height = map(int, size)
    for step in range(scrolls + 1):
        for node in hierarchy().iter('node'):
            if node.get('text') != label or node.get('enabled') == 'false':
                continue
            points = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
            if len(points) != 4:
                continue
            left, top, right, bottom = points
            if right > left and bottom > top and 0 <= top < bottom <= height:
                adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))
                time.sleep(.5)
                return
        if step < scrolls:
            adb('shell', 'input', 'swipe', str(width // 2), str(height * 85 // 100),
                str(width // 2), str(height * 20 // 100), '250')
            time.sleep(.25)
    raise AssertionError('UI_TEXT_NOT_FOUND:' + label)


def acknowledgements():
    command = f"if [ -d {ROOT}/requests ]; then find {ROOT}/requests -maxdepth 1 -name '*.ack.json' -type f; fi"
    return set(adb('shell', command).splitlines())


def read_json(path):
    return json.loads(adb('shell', 'cat', path))


def main():
    assert adb('get-serialno').strip().startswith('emulator-'), 'EMULATOR_REQUIRED'
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1', 'EMULATOR_REQUIRED'
    assert adb('shell', 'id', '-u').strip() == '0', 'ROOTED_DISPOSABLE_EMULATOR_REQUIRED'
    expected_source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
    before = acknowledgements()
    adb('shell', 'am', 'start', '-W', '-n', PKG + '/com.ethk3.novaonepro.MainActivity')
    click_text('导出完整报告（含数据库和训练附件）', scrolls=45)
    capture('export-menu')
    # The production menu removes report cache only. This is the disposable CI
    # emulator, never the original phone or its protected database/backup files.
    click_text('清理旧报告并生成当前完整报告')
    deadline = time.monotonic() + 360
    last = None
    publication = None
    while time.monotonic() < deadline:
        for path in sorted(acknowledgements() - before):
            try:
                state = read_json(path)
            except json.JSONDecodeError:
                continue  # Retry a state file changing at the exact read boundary.
            stage = state.get('stage', '')
            progress = (path.rsplit('/', 1)[-1], stage)
            if progress != last:
                print('R30_UI_REPORT_PROGRESS:' + ':'.join(progress), flush=True)
                last = progress
            if stage in ('FAILED', 'CANCELLED'):
                raise AssertionError('UI_REPORT_FAILED:' + str(state.get('reason', stage)))
            if stage == 'READY_TO_SHARE':
                job = state.get('jobId', '')
                assert re.fullmatch('[a-f0-9]{64}', job), 'UI_REPORT_JOB_ID'
                publication = (job, read_json(f'{ROOT}/jobs/{job}/delivery/index.json'))
                break
        if publication:
            break
        time.sleep(2)
    assert publication, 'UI_REPORT_PUBLICATION_TIMEOUT'
    job, index = publication
    assert index['format'] == 'SCREPORT_SINGLE_ZIP_R992', 'UI_REPORT_SINGLE_ZIP_REQUIRED'
    assert index['evidenceComplete'] and index['checksComplete'], 'UI_REPORT_INCOMPLETE'
    files = index['files']
    assert len(files) == 1 and files[0]['name'] == 'STARCORE_FULL_REPORT.zip'
    member = files[0]
    assert member['path'] == 'delivery/STARCORE_FULL_REPORT.zip'
    local = args.out / 'UI_EXPORTED_FULL_REPORT.zip'
    adb('pull', f'{ROOT}/jobs/{job}/delivery/STARCORE_FULL_REPORT.zip', str(local), timeout=90)
    digest = hashlib.sha256(local.read_bytes()).hexdigest()
    assert local.stat().st_size == member['bytes'] > 0 and digest == member['sha256'], 'UI_REPORT_PUBLISHED_BYTES_MISMATCH'
    with zipfile.ZipFile(local) as archive:
        assert archive.testzip() is None, 'UI_REPORT_ZIP_CRC_FAILED'
        names = archive.namelist()
        assert len(names) == len(set(names)) and len(names) > 10, 'UI_REPORT_ENTRIES_INVALID'
        required = ['DIAGNOSTIC_SCOPE.json', 'R30_REPORT.json', 'R30_REPORT.md', 'source-manifest.json', 'runtime-observation.json']
        originals = {}
        for name in required:
            body = archive.read(name)
            assert body, 'UI_REPORT_EMPTY_ENTRY:' + name
            originals[name] = {'bytes': len(body), 'sha256': hashlib.sha256(body).hexdigest()}
        source = json.loads(archive.read('source-manifest.json'))
        assert source['sourceCommit'] == expected_source, 'UI_REPORT_SOURCE_MISMATCH'
        runtime = json.loads(archive.read('runtime-observation.json'))
        assert runtime.get('researchSchemaReady') is True, 'UI_REPORT_RESEARCH_NOT_READY'
        summary = runtime.get('schemaStartupSummary', '')
        assert summary.startswith('critical=READY') and '|research=READY' in summary, 'UI_REPORT_STARTUP_SCHEMA_NOT_READY:' + summary
        scope = json.loads(archive.read('DIAGNOSTIC_SCOPE.json'))
        domains = next(row['rows'] for row in scope['recentEvidence'] if row['table'] == 'v57_schema_domain_r926')
        for domain in ('CRITICAL', 'RESEARCH'):
            assert any(row['domain'] == domain and row['state'] == 'READY' for row in domains), 'UI_REPORT_DOMAIN_NOT_READY:' + domain
    capture('published-report')
    receipt = {'sourceCommit': expected_source, 'result': 'PASS', 'environment': 'DISPOSABLE_ANDROID_EMULATOR',
               'entry': 'MainActivity production export button and menu', 'jobId': job,
               'zipBytes': local.stat().st_size, 'zipSha256': digest, 'entryCount': len(names),
               'requiredNonemptyMembers': originals, 'researchSchemaReady': True, 'schemaStartupSummary': summary,
               'evidenceComplete': True, 'checksComplete': True, 'allChecksPassed': index['allChecksPassed'],
               'physicalDevice': 'NOT_RUN', 'user9656647680ByteDatabase': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN'}
    (args.out / 'ui-report-acceptance.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')
    print('R30_REAL_UI_EXPORT|PRODUCTION_BUTTON|READBACK_SHA_CRC_NONEMPTY_ZIP|CRITICAL_RESEARCH_READY|PASS', flush=True)


try:
    main()
except BaseException:
    try:
        capture('failure')
        (args.out / 'failure-logcat.txt').write_text(adb('logcat', '-d', '-v', 'threadtime'))
    except Exception:
        pass
    raise
