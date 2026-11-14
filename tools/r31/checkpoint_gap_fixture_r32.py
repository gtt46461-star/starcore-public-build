"""Explicit disposable-emulator missing-CAS fixture, never a user database tool."""
import hashlib
import json
import re
import sqlite3
import time
from pathlib import Path

PKG = 'com.starcore.research.r13'
MISSING = format(32, '064x')
PIN = 'R32_REPORT_MISSING_ARTIFACT_FIXTURE'
FIXTURE_DETAILS = {}


def install(adb, private):
    assert adb('get-serialno').strip().startswith('emulator-'), 'R32_FIXTURE_EMULATOR_REQUIRED'
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1', 'R32_FIXTURE_EMULATOR_REQUIRED'
    assert adb('shell', 'id', '-u').strip() == '0', 'R32_FIXTURE_ROOTED_DISPOSABLE_EMULATOR_REQUIRED'
    manifest = json.loads(Path('release/release_manifest.json').read_text())
    assert manifest['applicationId'] == PKG and manifest['versionCode'] == 577311032, 'R32_FIXTURE_EXACT_BUILD_REQUIRED'
    name = manifest['databaseName']
    assert re.fullmatch('[A-Za-z0-9_.-]+', name), 'R32_FIXTURE_DATABASE_NAME'
    path = f'/data/user/0/{PKG}/databases/{name}'
    absent = f'/data/user/0/{PKG}/files/r26-checkpoints/{MISSING}.bin'
    assert adb('shell', f'if [ ! -e {absent} ]; then echo MISSING; fi').strip() == 'MISSING', 'R32_FIXTURE_WOULD_DELETE_EXISTING_OBJECT'
    adb('shell', 'am', 'force-stop', PKG)
    uid = adb('shell', 'stat', '-c', '%u', f'/data/user/0/{PKG}/files').strip()
    assert uid.isdigit(), 'R32_FIXTURE_UID'
    folder = private / 'controlled-missing-checkpoint'; folder.mkdir()
    local = folder / name
    for suffix in ['', '-wal']:
        remote = path + suffix
        if adb('shell', f'if [ -f {remote} ]; then echo YES; fi').strip() == 'YES':
            adb('pull', remote, str(folder / (name + suffix)))
    assert local.is_file(), 'R32_FIXTURE_INITIAL_DATABASE_MISSING'
    with sqlite3.connect(local) as db:
        existing = db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='r31_gc_pin'").fetchone()
        ddl = 'CREATE TABLE IF NOT EXISTS r31_gc_pin(pin_id TEXT PRIMARY KEY,artifact_hash TEXT NOT NULL,root_kind TEXT NOT NULL)'
        assert ddl in Path('app/src/main/java/com/ethk3/novaonepro/strategyv2/CheckpointGcStoreR31.java').read_text(), 'R32_FIXTURE_PRODUCTION_DDL_MISMATCH'
        db.execute(ddl)
        FIXTURE_DETAILS['registryCreatedByFixture'] = not bool(existing)
        FIXTURE_DETAILS['registryPreparation'] = 'CONTROLLED_TEST_ONLY_PRODUCTION_DECLARATION_CREATE_IF_ABSENT' 
        db.execute('PRAGMA wal_checkpoint(TRUNCATE)')
        db.execute('PRAGMA journal_mode=DELETE')
        db.execute('INSERT OR REPLACE INTO r31_gc_pin(pin_id,artifact_hash,root_kind) VALUES(?,?,?)', (PIN, MISSING, 'PERSISTED_REFERENCE'))
        db.commit()
        assert db.execute('PRAGMA integrity_check').fetchall() == [('ok',)], 'R32_FIXTURE_DATABASE_INTEGRITY'
    staged = path + '.r32-fixture-new'
    adb('push', str(local), staged)
    adb('shell', f'chown {uid}:{uid} {staged} && chmod 600 {staged} && rm -f {path}-wal {path}-shm && mv {staged} {path}')
    adb('shell', 'am', 'start', '-W', '-n', PKG + '/com.ethk3.novaonepro.MainActivity')
    time.sleep(10)
    (private / 'controlled-checkpoint-gap.json').write_text(json.dumps({'scope': 'CONTROLLED_DISPOSABLE_EMULATOR_REFERENCE_TO_ABSENT_FILE_NOT_NATURAL_DATA', 'sourceTable': 'r31_gc_pin', 'pinId': PIN, 'missingHash': MISSING, **FIXTURE_DETAILS}) + '\n')


def verify(archive, source, index, out):
    audit = source['r26CheckpointAuditR32']
    assert index['evidenceComplete'] and index['checksComplete'], 'R32_GAP_DIAGNOSTIC_INCOMPLETE'
    assert audit['missingObjects'] >= 1 and not audit['sourceClosureVerified'], 'R32_MISSING_ORIGINAL_NOT_REPORTED'
    assert not index['allChecksPassed'], 'R32_MISSING_ORIGINAL_FAKE_HEALTHY'
    records = [json.loads(line) for line in archive.read(audit['metadataMember']).decode().splitlines()]
    missing = [r for r in records if r.get('kind') == 'checkpoint_object' and r.get('expectedSha256') == MISSING]
    assert len(missing) == 1 and missing[0]['availability'] == 'MISSING_OBJECT' and not missing[0]['bodyIncluded'], 'R32_MISSING_ORIGINAL_BODY_FABRICATED_OR_OMITTED'
    details = archive.read('REPORT_DETAILS.txt').decode()
    assert '原始检查点附件状态=' in details and 'CHECKPOINT_EVIDENCE_GAPS' in details, 'R32_HUMAN_GAP_EXPLANATION_MISSING'
    receipt = {'scope': 'EXACT_SIGNED_ANDROID14_PRODUCTION_REPORT_WITH_CONTROLLED_MISSING_CHECKPOINT', 'result': 'PASS', 'missingHash': MISSING, 'missingObjects': audit['missingObjects'], 'sourceClosureVerified': False, 'allChecksPassed': False, 'diagnosticCompleted': bool(index['evidenceComplete'] and index['checksComplete']), 'rawBodyFabricated': False, **FIXTURE_DETAILS, 'metadataMemberSha256': hashlib.sha256(archive.read(audit['metadataMember'])).hexdigest()}
    (out / 'R32_CHECKPOINT_GAP_PROOF.json').write_text(json.dumps(receipt, indent=2) + '\n')
