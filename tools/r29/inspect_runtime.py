#!/usr/bin/env python3
"""Read disposable-emulator database COPIES, emit aggregate protocol/staged-backup checks, never row payloads.
The caller removes every private database/WAL before uploading artifacts. This is not a physical/quality test.
"""
import argparse, hashlib, json, re, sqlite3, struct
from pathlib import Path


def scalar(db, sql):
    return db.execute(sql).fetchone()[0]


def typed_payload(payload, count):
    data = memoryview(payload)
    pos = 0

    def take(n):
        nonlocal pos
        if n < 0 or pos + n > len(data):
            raise AssertionError('TYPED_ROW_TRUNCATED')
        out = data[pos:pos + n]
        pos += n
        return out

    assert struct.unpack('>II', take(8)) == (0x29DB0001, count), 'TYPED_ROW_ABI_OR_SHAPE'
    for _ in range(count):
        kind = take(1)[0]
        if kind == 0:
            continue
        if kind in (1, 2):
            take(8)
        elif kind in (3, 4):
            n = struct.unpack('>I', take(4))[0]
            raw = take(n)
            if kind == 3:
                bytes(raw).decode('utf-8', errors='strict')
        else:
            raise AssertionError('TYPED_ROW_TYPE')
    assert pos == len(data), 'TYPED_ROW_TRAILING_BYTES'


def inspect_owner(realtime, research, baseline, result):
    route = realtime.execute('SELECT phase,database_name,schema_hash,source_root,epoch FROM r29_database_route WHERE singleton=1').fetchone()
    assert route and route[0] == 'ACTIVE' and route[1] == 'starcore-r29-research.db', 'ACTIVE_RESEARCH_ROUTE_REQUIRED'
    result.update(cutoverEnabled=True, activeOwner=route[1], researchIdentityMatched=False)
    owner = research.execute('SELECT phase,schema_hash,source_root,epoch FROM r29_research_owner WHERE singleton=1').fetchone()
    assert owner and owner[0] == 'READY' and tuple(owner[1:]) == tuple(route[2:]), 'ACTIVE_RESEARCH_OWNER_MISMATCH'
    assert all(re.fullmatch('[a-f0-9]{64}', x) for x in route[2:4]) and route[4] > 0, 'ACTIVE_RESEARCH_IDENTITY_INVALID'
    publication = realtime.execute('SELECT phase,epoch,baseline_name,baseline_sha,source_root FROM r29_migration_publication WHERE singleton=1').fetchone()
    assert publication and publication[0] == 'PUBLISHED' and publication[1] == route[4] and publication[4] == route[3], 'RESEARCH_PUBLICATION_MISMATCH'
    assert re.fullmatch(r'starcore-r29-baseline-[0-9]+\.db', publication[2]), 'RESEARCH_BASELINE_NAME_INVALID'
    assert baseline and baseline.is_file(), 'SEALED_RESEARCH_BASELINE_MISSING'
    assert hashlib.sha256(baseline.read_bytes()).hexdigest() == publication[3], 'SEALED_RESEARCH_BASELINE_SHA_MISMATCH'
    with sqlite3.connect(baseline) as sealed:
        assert scalar(sealed, 'PRAGMA integrity_check') == 'ok', 'SEALED_RESEARCH_BASELINE_INTEGRITY'
        assert sealed.execute('SELECT phase,schema_hash,source_root,epoch FROM r29_research_owner WHERE singleton=1').fetchone() == owner, 'SEALED_RESEARCH_BASELINE_OWNER_MISMATCH'
    result.update(cutoverEnabled=True, activeOwner=route[1], researchIdentityMatched=True,
                  publicationPhase=publication[0], baselineName=publication[2],
                  sealedBaselineSha256=publication[3], sealedBaselineVerified=True)


def inspect_events(realtime, research, result):
    # Counts alone can falsely accept an ACK for a different event. Match each durable identity across files.
    inbox = {r[0]: r[1:] for r in research.execute('SELECT event_id,source_seq,issue,payload_hash,origin FROM r29_event_inbox')}
    root_ids = set()
    ack_missing = delivered_mismatch = corrupt_payload = 0
    for seq, event, issue, payload, digest, origin, acked in realtime.execute('SELECT seq,event_id,issue,payload_json,payload_hash,origin,acked_at FROM r29_event_outbox'):
        root_ids.add(event)
        corrupt_payload += hashlib.sha256(payload.encode()).hexdigest() != digest
        matching = inbox.get(event) == (seq, issue, digest, origin)
        if acked > 0 and not matching:
            ack_missing += 1
        if event in inbox and not matching:
            delivered_mismatch += 1
    result.update(ackedEventsWithoutMatchingInbox=ack_missing,
                  deliveredIdentityMismatches=delivered_mismatch,
                  inboxWithoutRootEvent=len(set(inbox)-root_ids), corruptOutboxPayloads=corrupt_payload)
    assert not any(result[k] for k in ('ackedEventsWithoutMatchingInbox', 'deliveredIdentityMismatches', 'inboxWithoutRootEvent', 'corruptOutboxPayloads')), 'CROSS_FILE_EVENT_PROTOCOL_MISMATCH'


def inspect(main, stage, result, research_path=None, baseline=None):
    assert main.is_file(), 'REALTIME_DATABASE_MISSING'
    if research_path is not None:
        assert research_path.is_file(), 'ACTIVE_RESEARCH_DATABASE_MISSING'
    result.update(scope='API34_EMULATOR_ENGINEERING_NOT_NATURAL_QUALITY',
                  cutoverEnabled=False, activeOwner='ORIGINAL_DATABASE')
    with sqlite3.connect(main) as realtime:
        result['mainIntegrity'] = scalar(realtime, 'PRAGMA integrity_check')
        result['eventsOutbox'] = scalar(realtime, 'SELECT count(*) FROM r29_event_outbox')
        result['eventsAcked'] = scalar(realtime, 'SELECT count(*) FROM r29_event_outbox WHERE acked_at>0')
        result['sourceWatermarks'] = dict(realtime.execute('SELECT table_name,generation FROM r29_source_watermark'))
        route_exists = scalar(realtime, "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='r29_database_route'")
        active = route_exists and scalar(realtime, 'SELECT count(*) FROM r29_database_route')
        assert research_path is not None or not active, 'ACTIVE_ROUTE_REQUIRES_INDEPENDENT_RESEARCH_COPY'
        with sqlite3.connect(research_path or main) as research:
            if research_path is not None:
                inspect_owner(realtime, research, baseline, result)
            result['researchIntegrity'] = scalar(research, 'PRAGMA integrity_check')
            assert result['researchIntegrity'] == 'ok', 'RESEARCH_INTEGRITY'
            result['eventsDelivered'] = scalar(research, 'SELECT count(*) FROM r29_event_inbox')
            result['modelReceipts'] = scalar(research, "SELECT count(*) FROM r29_learning_receipt WHERE consumer='ATOMNET_ONLINE'")
            result['modelWorkPending'] = scalar(research, "SELECT count(*) FROM r29_learning_work WHERE consumer='ATOMNET_ONLINE' AND status='PENDING'")
            result['workWithoutCompletionReceipt'] = scalar(research, "SELECT count(*) FROM r29_learning_work w WHERE w.status='COMMITTED' AND NOT EXISTS(SELECT 1 FROM r29_learning_receipt r WHERE r.consumer=w.consumer AND r.issue=w.issue AND r.state_hash=w.state_hash)")
            inspect_events(realtime, research, result)
            result['newTaskErrorsByOwner'] = {}
            result['allResearchTaskErrorsByOwner'] = {}
            result['newTaskFailureReasons'] = []
            for name, db in [('REALTIME', realtime)] + ([('RESEARCH', research)] if research_path is not None else []):
                result['allResearchTaskErrorsByOwner'][name] = scalar(db, 'SELECT count(*) FROM r27_task_error')
                result['newTaskErrorsByOwner'][name] = scalar(db, "SELECT count(*) FROM r27_task_error WHERE task_id IN ('r29-event-inbox','r29-research-migration')")
                result['newTaskFailureReasons'].extend({'owner': name, 'task': row[0], 'errorClass': row[1], 'message': row[2][:500]} for row in db.execute("SELECT task_id,error_class,message FROM r27_task_error WHERE task_id IN ('r29-event-inbox','r29-research-migration') ORDER BY error_id DESC LIMIT 5"))
            result['newTaskErrors'] = sum(result['newTaskErrorsByOwner'].values())
    assert result['mainIntegrity'] == 'ok' and result['workWithoutCompletionReceipt'] == 0
    assert result['eventsAcked'] <= result['eventsDelivered'] <= result['eventsOutbox']
    assert result['newTaskErrors'] == 0, 'NEW_R29_TASK_ERRORS'
    result['eventDeliveryStatus'] = 'PASS' if result['eventsDelivered'] else 'INSUFFICIENT_NO_ACCEPTED_EVENT'
    assert stage.is_file(), 'INDEPENDENT_STAGE_DATABASE_MISSING'
    with sqlite3.connect(stage) as research:
        result['stageIntegrity'] = scalar(research, 'PRAGMA integrity_check')
        result['stagePhases'] = dict(research.execute('SELECT state,count(*) FROM r29_copy_manifest GROUP BY state'))
        verified = []
        for name, schema_json, schema_hash, attempt, generation, rows, source_rows, verified_rows, copy_hash, verify_hash in research.execute("SELECT table_name,schema_json,schema_hash,attempt,source_generation,copied_rows,source_rows,verified_rows,copy_hash,verify_hash FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE' ORDER BY table_name"):
            assert hashlib.sha256(schema_json.encode()).hexdigest() == schema_hash
            schema = json.loads(schema_json)
            chain = hashlib.sha256(('R29_TYPED_ROW_CHAIN_V1|' + name + '|' + schema_hash).encode()).hexdigest()
            count = 0
            for rowid, payload, row_hash in research.execute('SELECT source_rowid,payload,row_hash FROM r29_copy_rows WHERE table_name=? AND attempt=? ORDER BY source_rowid', (name, attempt)):
                assert hashlib.sha256(payload).hexdigest() == row_hash
                typed_payload(payload, len(schema['columns']))
                chain = hashlib.sha256((chain + ':' + str(rowid) + ':' + row_hash).encode()).hexdigest()
                count += 1
            assert count == rows == source_rows == verified_rows and chain == copy_hash == verify_hash
            verified.append({'table': name, 'rows': count, 'canonicalHash': chain,
                             'sourceWatermark': generation,
                             'catchUpRequired': result['sourceWatermarks'].get(name) != generation})
        result['verifiedBaselines'] = verified
    assert result['stageIntegrity'] == 'ok' and verified, 'NO_VERIFIED_BASELINE_TABLE'
    result['stagedBackupScope'] = 'IMMUTABLE_STAGE_BASELINES_ONLY_NOT_CURRENT_ACTIVE_RP_CONTENT_EQUIVALENCE'
    result['stagedBackupStatus'] = 'PASS_IMMUTABLE_STAGE_BASELINES_ONLY' if research_path is not None else 'PASS_BASELINE_ONLY_CATCHUP_ROUTING_PENDING'
    if research_path is not None:
        result['activeResearchStatus'] = 'PASS_OWNER_PUBLICATION_BASELINE_AND_EVENT_PROTOCOL'
    return result


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--main', type=Path, required=True)
    p.add_argument('--stage', type=Path, required=True)
    p.add_argument('--research', type=Path)
    p.add_argument('--baseline', type=Path)
    p.add_argument('--out', type=Path, required=True)
    a = p.parse_args()
    result = {'scope': 'API34_EMULATOR_ONLY', 'status': 'FAIL'}
    try:
        inspect(a.main, a.stage, result, a.research, a.baseline)
        result['status'] = 'PASS_ENGINEERING_SUBSET_ONLY'
    except Exception as failed:
        result['failure'] = type(failed).__name__ + ':' + str(failed)[:500]
        raise
    finally:
        a.out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({k: result[k] for k in ['status', 'eventsDelivered', 'modelReceipts', 'stagedBackupStatus']}))


if __name__ == '__main__':
    main()
