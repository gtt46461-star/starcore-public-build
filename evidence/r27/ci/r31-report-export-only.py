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
import tempfile
import os
import io
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


def safe_failure_symbols(ack_path, state):
    """Read the original recovery cause locally; export public code symbols only.

    No raw recovery archive, stack message, report data, private path, URL or
    credentials are written to the diagnostic output or Actions console.
    """
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
    java = list(Path('app/src').rglob('*.java'))
    public_files = {p.name for p in java}
    public_text = '\n'.join(p.read_text(errors='replace') for p in java)
    public_keys = set(re.findall(r'"([A-Za-z][A-Za-z0-9_]{0,80})"', public_text))
    request = ack_path.rsplit('/', 1)[-1].removesuffix('.ack.json')
    assert re.fullmatch('[a-f0-9]{64}', request), 'DIAGNOSTIC_REQUEST_ID_INVALID'
    result = {'sourceCommit': source, 'runId': os.environ.get('GITHUB_RUN_ID', 'LOCAL'),
              'scope': 'PUBLIC_CODE_SYMBOLS_ONLY_NO_ORIGINAL_LOG_OR_REPORT_DATA',
              'recoveryAvailable': False, 'publicSourceFrames': [], 'missingPublicJsonKeys': []}
    original = str(state.get('recoveryFailure', ''))
    try:
        # FAILED is durable and may be acknowledged before the owner finishes
        # its immutable recovery ZIP. Wait for that independent publication;
        # never misclassify this normal ordering as a missing original cause.
        recovery_deadline = time.monotonic() + 90
        while True:
            try:
                receipt = read_json(f'{ROOT}/recovery/{request}/receipt.json')
                break
            except (RuntimeError, json.JSONDecodeError):
                if time.monotonic() >= recovery_deadline:
                    raise
                time.sleep(1)
        digest = receipt['sha256']
        assert re.fullmatch('[a-f0-9]{64}', digest), 'DIAGNOSTIC_RECOVERY_ID_INVALID'
        with tempfile.TemporaryDirectory(prefix='r31-private-recovery-') as private:
            recovery = Path(private) / 'recovery.zip'
            adb('pull', f'{ROOT}/recovery/{request}/{digest}.zip', str(recovery), timeout=90)
            assert hashlib.sha256(recovery.read_bytes()).hexdigest() == digest, 'DIAGNOSTIC_RECOVERY_HASH_MISMATCH'
            with zipfile.ZipFile(recovery) as archive:
                entry = archive.getinfo('original-failure.txt')
                assert entry.file_size <= 2 * 1024 * 1024, 'DIAGNOSTIC_STACK_BOUND'
                raw = archive.read(entry)
                original = raw.decode(errors='replace')
                result['originalFailureMemberSha256'] = hashlib.sha256(raw).hexdigest()
                result['recoveryAvailable'] = True
    except Exception as unavailable:
        # Class names only, never the exception's potentially sensitive message.
        result['collectorFailureClass'] = type(unavailable).__name__
    for cls, method, file, line in re.findall(
            r'at (com\.ethk3\.novaonepro\.[A-Za-z0-9_.$]+)\.([A-Za-z0-9_$]+)\(([A-Za-z0-9_]+\.java):(\d+)\)', original):
        if file in public_files:
            row = [cls, method, file, int(line)]
            if row not in result['publicSourceFrames'] and len(result['publicSourceFrames']) < 80:
                result['publicSourceFrames'].append(row)
    missing = set(re.findall(r'No value for ([A-Za-z][A-Za-z0-9_]*)', original))
    missing.update(re.findall(r' at ([A-Za-z][A-Za-z0-9_]*) of type ', original))
    result['missingPublicJsonKeys'] = sorted(public_keys.intersection(missing))
    result['nonfiniteJsonRejected'] = bool(re.search(r'Forbidden numeric value|not a finite double', original))
    result['jsonTypeMismatch'] = 'cannot be converted to' in original
    (args.out / 'safe-recovery-failure.json').write_text(json.dumps(result, indent=2) + '\n')
    print('R31_SAFE_UI_FAILURE|' + json.dumps(result, sort_keys=True), flush=True)


def safe_report_semantics(archive, index):
    """Export public check IDs/statuses/source frames, never report evidence."""
    java = list(Path('app/src').rglob('*.java'))
    source = '\n'.join(p.read_text(errors='replace') for p in java)
    files = {p.name for p in java}
    codes = set(re.findall(r'\b[A-Z][A-Z0-9_]{2,80}\b', source))
    keys = set(re.findall(r'"([A-Za-z][A-Za-z0-9_]{0,80})"', source))
    result = {'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
              'scope': 'PUBLIC_CHECK_IDENTIFIERS_AND_SOURCE_SYMBOLS_ONLY_NO_EVIDENCE_VALUES',
              'evidenceComplete': bool(index['evidenceComplete']), 'checksComplete': bool(index['checksComplete']),
              'allChecksPassed': bool(index['allChecksPassed']), 'catalogPages': 0, 'catalogResults': 0,
              'incompleteChecks': [], 'catalogErrors': []}
    def symbols(text):
        return {'sourceEnums': sorted(codes.intersection(re.findall(r'\b[A-Z][A-Z0-9_]{2,80}\b', text)))[:80],
                'missingPublicJsonKeys': sorted(keys.intersection(re.findall(r'No value for ([A-Za-z][A-Za-z0-9_]*)', text))),
                'publicFrames': [list(row) for row in re.findall(
                    r'at (com\.ethk3\.novaonepro\.[A-Za-z0-9_.$]+)\.([A-Za-z0-9_$]+)\(([A-Za-z0-9_]+\.java):(\d+)\)', text) if row[2] in files][:30]}
    for entry in archive.infolist():
        if not re.fullmatch(r'(?:catalog/)?catalog-[0-9]+\.jsonl', entry.filename):
            continue
        result['catalogPages'] += 1
        assert entry.file_size <= 4 * 1024 * 1024, 'DIAGNOSTIC_CATALOG_PAGE_BOUND'
        with io.TextIOWrapper(archive.open(entry), encoding='utf-8') as rows:
            for line in rows:
                assert len(line) <= 256 * 1024, 'DIAGNOSTIC_CATALOG_ROW_BOUND'
                row = json.loads(line)
                if row.get('kind') == 'check_result':
                    result['catalogResults'] += 1
                if row.get('kind') == 'check_result' and not row.get('completes_contract'):
                    check = row.get('id', '')
                    assert re.fullmatch(r'RSEC-[0-9]{3}', check), 'DIAGNOSTIC_PUBLIC_CHECK_ID'
                    record = {'id': check, **symbols(str(row.get('reason', '')) + '\n' + str(row.get('error_detail', '')))}
                    for key in ('execution_state', 'domain_outcome', 'health'):
                        record[key] = row.get(key) if row.get(key) in codes else 'UNLISTED_DYNAMIC_OUTCOME'
                    result['incompleteChecks'].append(record)
                if row.get('kind') in ('catalog_error', 'table_error', 'index_error'):
                    record = symbols(str(row.get('reason', '')) + '\n' + str(row.get('error_detail', '')))
                    record['kind'] = row['kind']
                    name = row.get('table', row.get('name', ''))
                    record['publicObject'] = name if name and name in source else 'UNLISTED_OBJECT'
                    result['catalogErrors'].append(record)
    (args.out / 'safe-report-semantics.json').write_text(json.dumps(result, indent=2) + '\n')
    print('R31_SAFE_REPORT_SEMANTICS|' + json.dumps(result, sort_keys=True), flush=True)


def click_node(predicate):
    for node in hierarchy().iter('node'):
        if not predicate(node) or node.get('enabled')=='false':continue
        p=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
        if len(p)==4 and p[2]>p[0] and p[3]>p[1]:
            adb('shell','input','tap',str((p[0]+p[2])//2),str((p[1]+p[3])//2));time.sleep(.7);return True
    return False

def save_via_real_document_picker(job,member,local):
    before=acknowledgements();time.sleep(2)
    click_text('一次保存完整ZIP文件');time.sleep(2)
    # Select the real Downloads root in Android's ACTION_CREATE_DOCUMENT UI.
    if click_node(lambda n:n.get('content-desc','').lower() in ['show roots','open navigation drawer','显示根目录','打开导航抽屉']):
        time.sleep(.8)
        if not click_node(lambda n:n.get('text','') in ['Downloads','下载','Download']):raise AssertionError('REPORT_SAF_DOWNLOADS_ROOT_NOT_FOUND')
    saved=False
    for _ in range(20):
        if click_node(lambda n:n.get('resource-id','').endswith('/action_menu_save') or n.get('text','').strip().lower() in ['save','保存']):saved=True;break
        time.sleep(.5)
    assert saved,'REPORT_SAF_PICKER_SAVE_NOT_FOUND'
    end=time.monotonic()+180;verified=None
    while time.monotonic()<end:
        for path in acknowledgements()-before:
            try:state=read_json(path)
            except (json.JSONDecodeError,RuntimeError):continue
            stage=state.get('state',state.get('stage',''))
            if stage in ['FAILED','CANCELLED']:raise AssertionError('REPORT_SAF_TRANSFER_FAILED')
            if stage=='VERIFIED_READBACK':
                assert state.get('jobId')==job and state.get('volumeName')=='STARCORE_FULL_REPORT.zip','REPORT_SAF_ACK_IDENTITY'
                assert state.get('actualSha256')==member['sha256'] and state.get('actualBytes')==member['bytes'],'REPORT_SAF_ACK_READBACK_MISMATCH'
                assert state.get('localReceiptDurable') is True,'REPORT_SAF_RECEIPT_NOT_DURABLE'
                verified=True;break
        if verified:break
        time.sleep(.5)
    assert verified,'REPORT_SAF_VERIFIED_ACK_TIMEOUT'
    downloaded=args.out/'SAF_SAVED_FULL_REPORT.zip'
    adb('pull','/sdcard/Download/STARCORE_FULL_REPORT.zip',str(downloaded),timeout=90)
    digest=hashlib.sha256(downloaded.read_bytes()).hexdigest()
    assert digest==member['sha256'] and downloaded.stat().st_size==member['bytes'],'REPORT_SAF_EXTERNAL_READBACK_MISMATCH'
    return {'state':'VERIFIED_READBACK','externalFileSha256':digest,'bytes':downloaded.stat().st_size,'actualSystemPicker':True,'sameAsPublishedZip':True}

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
                safe_failure_symbols(path, state)
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
        safe_report_semantics(archive, index)
        assert index['evidenceComplete'] and index['checksComplete'], 'UI_REPORT_INCOMPLETE'
        # User-requested report-only scope: report completeness is mandatory;
        # business/model health remains a reported finding, not a fake PASS.
        document=json.loads(archive.read('R30_REPORT.json'))
        semantic=json.loads(archive.read('R31_SEMANTIC_RECEIPT.json'))
        assert semantic['jobId']==job and semantic['reportSha256']==hashlib.sha256(archive.read('R30_REPORT.json')).hexdigest(), 'REPORT_SEMANTIC_DOCUMENT_HASH'
        assert len(document['chapters'])==12 and len({c['sectionId'] for c in document['chapters']})==12, 'REPORT_TWELVE_UNIQUE_CHAPTERS'
        assert len(document['metricResults'])==44 and len({m['metricId'] for m in document['metricResults']})==44, 'REPORT_44_UNIQUE_METRICS'
        assert len(semantic['checks'])==96, 'REPORT_96_CHECK_RESULTS'
        for key,count in [('legacyCharts',29),('researchCharts',18),('matrices',20),('logicalTables',16),('capabilities',82)]:
            assert len(document['objectIndex'][key]['objects'])==count, 'REPORT_OBJECT_SET:'+key
        for required_original in ['R26_REPORT.json','R27_REPORT.md','R26_POLICY_IDENTITIES.jsonl','R26_FROZEN_SURFACE.json','STORAGE_CENSUS.txt']:
            assert required_original in names, 'REPORT_ORIGINAL_MEMBER_MISSING:'+required_original
        summary=runtime.get('schemaStartupSummary','')
    saf=save_via_real_document_picker(job,member,local)
    capture('published-report')
    receipt = {'sourceCommit': expected_source, 'result': 'PASS', 'environment': 'DISPOSABLE_ANDROID_EMULATOR',
               'entry': 'MainActivity production export button and menu', 'jobId': job,
               'zipBytes': local.stat().st_size, 'zipSha256': digest, 'entryCount': len(names),
               'requiredNonemptyMembers': originals, 'researchSchemaReady': runtime.get('researchSchemaReady'), 'schemaStartupSummarySha256': hashlib.sha256(summary.encode()).hexdigest(),
               'evidenceComplete': True, 'checksComplete': True, 'allChecksPassed': index['allChecksPassed'],
               'physicalDevice': 'NOT_RUN', 'user9656647680ByteDatabase': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN', 'scope':'REPORT_GENERATION_COMPLETENESS_AND_REAL_SAF_EXPORT_ONLY', 'saf':saf, 'businessAndAlgorithmHealth':'DEFERRED_BY_EXPLICIT_USER_SCOPE_NOT_PASSED'}
    (args.out / 'ui-report-acceptance.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')
    print('R30_REAL_UI_EXPORT|PRODUCTION_BUTTON|READBACK_SHA_CRC_NONEMPTY_ZIP|REAL_SAF_VERIFIED|REPORT_ONLY_PASS', flush=True)


try:
    main()
except BaseException:
    try:
        capture('failure')
        (args.out / 'failure-logcat.txt').write_text(adb('logcat', '-d', '-v', 'threadtime'))
    except Exception:
        pass
    raise
