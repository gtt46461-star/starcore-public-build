#!/usr/bin/env python3
"""Two real menu entrances against a disposable synthetic ACK-only phone layout."""
import hashlib
import io
import json
import subprocess
import tempfile
import time
import zipfile
from pathlib import Path
import phone_report_ui as ui


def upload(local, remote):
    ui.adb('push', str(local), remote)


def main():
    assert ui.adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1', 'EMULATOR_REQUIRED'
    assert ui.adb('shell', 'getprop', 'ro.build.version.sdk').strip() == '34', 'ANDROID14_REQUIRED'
    ui.adb('shell', 'am', 'force-stop', ui.PKG)
    preserved = ui.ROOT + '-saved-before-phone-fixture'
    ui.adb('shell', f'test ! -e {preserved} && mv {ui.ROOT} {preserved}')
    uid = ui.adb('shell', 'stat', '-c', '%u', '/data/user/0/' + ui.PKG).strip()
    assert uid.isdigit(), 'APP_UID_REQUIRED'
    expected = format(200, '064x')
    other = format(201, '064x')
    retention = format(300, '064x')
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
    release = hashlib.sha256(Path('release/release_manifest.json').read_bytes()).hexdigest()
    with tempfile.TemporaryDirectory(prefix='r31-synthetic-phone-') as temporary:
        root = Path(temporary) / 'fixture'
        root.mkdir()
        def write(relative, value):
            path = root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(value))
        for request, created, label in [(expected, 200, 'CORRECT_LATEST_ORIGINAL'), (other, 100, 'OLDER_UNRELATED_ORIGINAL')]:
            request_value = dict(requestId=request, mode='FULL_REPORT', createdWallMs=created, releaseHash=release)
            body = io.BytesIO()
            with zipfile.ZipFile(body, 'w', zipfile.ZIP_DEFLATED) as archive:
                archive.writestr('EVIDENCE_BOUNDARY.json', json.dumps(dict(requestId=request, scope='EXPORT_FAILURE_RECOVERY_ONLY')))
                archive.writestr('request.json', json.dumps(request_value))
                archive.writestr('original-failure.txt', label + '\nAPI_KEY=fixture-key-must-hide\nAuthorization: Bearer fixture-token-must-hide\n')
            data = body.getvalue()
            sha = hashlib.sha256(data).hexdigest()
            directory = root / 'recovery' / request
            directory.mkdir(parents=True)
            (directory / (sha + '.zip')).write_bytes(data)
            receipt = dict(sha256=sha, bytes=len(data), scope='EXPORT_FAILURE_RECOVERY_ONLY', databaseIncluded=False)
            write('recovery/' + request + '/receipt.json', receipt)
            write('requests/' + request + '.json', request_value)
            write('requests/' + request + '.ack.json', dict(requestId=request, jobId=format(int(request,16)+100,'064x'), mode='FULL_REPORT', stage='FAILED', revision=3, reason='REPORT_PROGRESS_WRITE_FAILED', recovery=receipt))
        job = dict(revision_name='REPORT_JOB_CP12_1', jobId=retention, requestId=retention,
                   releaseHash=release, mode='RETENTION_PAGE', stage='RETENTION_READY', resumeStage='BACKUP',
                   lastReason='', snapshotSha='', publishedDir='', manifestSha='', revision=5, attempt=1,
                   createdWallMs=999999, updatedWallMs=999999, snapshotWallMs=0, snapshotBytes=0,
                   temporaryReservationBytes=0, publishedQuotaBytes=0, compressionAttempt=0, preemptions=0,
                   failureAttempts=0, evidenceComplete=False, checksComplete=False, allChecksPassed=False)
        write('jobs/' + retention + '/job.json', job)
        write('current-job.json', dict(jobId=retention, requestId=retention))
        original_hashes = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in root.rglob('*') if p.is_file()}
        ui.adb('shell', 'mkdir', '-p', ui.ROOT)
        upload(str(root) + '/.', ui.ROOT + '/')
        ui.adb('shell', 'chown', '-R', uid + ':' + uid, ui.ROOT)
        ui.adb('shell', 'am', 'start', '-W', '-n', ui.PKG + '/com.ethk3.novaonepro.MainActivity')
        results = []
        for entrance in ['查看上次导出失败详情', '取回上次报告或继续生成']:
            ui.click_text('导出完整报告（含数据库和训练附件）', scrolls=45)
            ui.click_text(entrance)
            end = time.monotonic() + 20
            while True:
                labels = [node.get('text','') for node in ui.hierarchy().iter('node')]
                text = '\n'.join(labels)
                if 'CORRECT_LATEST_ORIGINAL' in text:
                    break
                assert time.monotonic() < end, 'FAILURE_DETAIL_NOT_VISIBLE_FROM_ENTRY'
                time.sleep(.4)
            assert expected in text and other not in text, 'FAILURE_REQUEST_IDENTITY_MISMATCH'
            assert 'REPORT_PROGRESS_WRITE_FAILED' in text and 'OLDER_UNRELATED_ORIGINAL' not in text, 'ACK_OR_ORIGINAL_WRONG'
            assert 'fixture-key-must-hide' not in text and 'fixture-token-must-hide' not in text, 'UI_CREDENTIALS_EXPOSED'
            assert '分享恢复诊断' in labels and '复制详情' in labels, 'RECOVERY_SHARE_OR_COPY_ENTRY_LOST'
            results.append(dict(entrance=entrance, correctRequest=True, ackReason=True, originalFailure=True, credentialsMasked=True, recoveryShare=True, copyUsesSameMaskedText=True))
            ui.click_text('复制详情')
        for relative, expected_hash in original_hashes.items():
            actual = subprocess.check_output(['adb','exec-out','cat',ui.ROOT+'/'+relative])
            assert hashlib.sha256(actual).hexdigest() == expected_hash, 'RETRIEVAL_CHANGED_EXISTING_EVIDENCE'
        directories = ui.adb('shell', 'ls', ui.ROOT + '/jobs').split()
        assert directories == [retention], 'RETRIEVAL_RECREATED_JOB_OR_BACKUP'
        receipt = dict(result='PASS', sourceCommit=source, scope='SIGNED_RELEASE_ANDROID14_REAL_MENU_SYNTHETIC_ACK_ONLY_LAYOUT',
                       requestId=expected, onlyRemainingJob='RETENTION_PAGE', failuresDirectory=False,
                       entrances=results, existingEvidenceBytesPreserved=True, newDatabaseBackup=False,
                       physicalDevice='NOT_RUN', originalDeviceDatabase='NOT_RUN')
        (ui.args.out / 'failure-ui-acceptance.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2)+'\n')
        print('PHONE_FAILURE_TWO_REAL_UI_ENTRIES_PASS_EVIDENCE_PRESERVED')


if __name__ == '__main__':
    main()
