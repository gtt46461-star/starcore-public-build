#!/usr/bin/env python3
"""Read disposable-emulator database COPIES, emit aggregate protocol/staged-backup checks, never row payloads.
The caller removes every private database/WAL before uploading artifacts. This is not a physical/quality test.
"""
import argparse, hashlib, json, sqlite3, struct
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


def inspect(main, stage, result):
    result.update(scope='API34_EMULATOR_ENGINEERING_NOT_NATURAL_QUALITY',
                  cutoverEnabled=False, activeOwner='ORIGINAL_DATABASE')
    with sqlite3.connect(main) as realtime:
        result['mainIntegrity'] = scalar(realtime, 'PRAGMA integrity_check')
        result['eventsOutbox'] = scalar(realtime, 'SELECT count(*) FROM r29_event_outbox')
        result['eventsAcked'] = scalar(realtime, 'SELECT count(*) FROM r29_event_outbox WHERE acked_at>0')
        result['eventsDelivered'] = scalar(realtime, 'SELECT count(*) FROM r29_event_inbox')
        result['modelReceipts'] = scalar(realtime, "SELECT count(*) FROM r29_learning_receipt WHERE consumer='ATOMNET_ONLINE'")
        result['modelWorkPending'] = scalar(realtime, "SELECT count(*) FROM r29_learning_work WHERE consumer='ATOMNET_ONLINE' AND status='PENDING'")
        result['workWithoutCompletionReceipt'] = scalar(realtime, "SELECT count(*) FROM r29_learning_work w WHERE w.status='COMMITTED' AND NOT EXISTS(SELECT 1 FROM r29_learning_receipt r WHERE r.consumer=w.consumer AND r.issue=w.issue AND r.state_hash=w.state_hash)")
        result['newTaskErrors'] = scalar(realtime, "SELECT count(*) FROM r27_task_error WHERE task_id IN ('r29-event-inbox','r29-research-migration')")
        result['newTaskFailureReasons'] = [{'task': row[0], 'errorClass': row[1], 'message': row[2][:500]}
                                          for row in realtime.execute("SELECT task_id,error_class,message FROM r27_task_error WHERE task_id IN ('r29-event-inbox','r29-research-migration') ORDER BY error_id DESC LIMIT 5")]
        result['sourceWatermarks'] = dict(realtime.execute('SELECT table_name,generation FROM r29_source_watermark'))
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
    result['stagedBackupStatus'] = 'PASS_BASELINE_ONLY_CATCHUP_ROUTING_PENDING'
    return result


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--main', type=Path, required=True)
    p.add_argument('--stage', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    a = p.parse_args()
    result = {'scope': 'API34_EMULATOR_ONLY', 'status': 'FAIL'}
    try:
        inspect(a.main, a.stage, result)
        result['status'] = 'PASS_ENGINEERING_SUBSET_ONLY'
    except Exception as failed:
        result['failure'] = type(failed).__name__ + ':' + str(failed)[:500]
        raise
    finally:
        a.out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({k: result[k] for k in ['status', 'eventsDelivered', 'modelReceipts', 'stagedBackupStatus']}))


if __name__ == '__main__':
    main()
