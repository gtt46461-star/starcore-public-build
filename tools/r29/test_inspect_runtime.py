#!/usr/bin/env python3
"""Offline negative controls for the independent-file APK acceptance reader. No device pass is inferred."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import sqlite3
import struct
import subprocess
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('inspect_runtime', Path(__file__).with_name('inspect_runtime.py'))
reader = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reader)


class RuntimeInspection(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='r29-active-protocol-')
        self.path = Path(self.tmp.name)
        self.root = self.path/'main.db'
        self.research = self.path/'research.db'
        self.stage = self.path/'stage.db'
        self.baseline = self.path/'sealed.db'
        self.schema, self.source = 'a'*64, 'b'*64
        payload = '{"issue":"202610300001"}'
        digest = hashlib.sha256(payload.encode()).hexdigest()
        self.mutate(self.root, [
            ('CREATE TABLE r29_database_route(singleton,phase,database_name,schema_hash,source_root,epoch)', ()),
            ("INSERT INTO r29_database_route VALUES(1,'ACTIVE','starcore-r29-research.db',?,?,42)", (self.schema,self.source)),
            ('CREATE TABLE r29_migration_publication(singleton,phase,epoch,baseline_name,baseline_sha,source_root)', ()),
            ("INSERT INTO r29_migration_publication VALUES(1,'PUBLISHED',42,'starcore-r29-baseline-42.db','',?)", (self.source,)),
            ('CREATE TABLE r29_event_outbox(seq,event_id,issue,payload_json,payload_hash,origin,acked_at)', ()),
            ("INSERT INTO r29_event_outbox VALUES(1,'event','202610300001',?,?,'API',123)", (payload,digest)),
            ('CREATE TABLE r29_source_watermark(table_name,generation)', ()),
            ("INSERT INTO r29_source_watermark VALUES('draws',3)", ()),
            ('CREATE TABLE r27_task_error(error_id,task_id,error_class,message)', ()),
        ])
        # Deliberately do NOT create inbox or learner tables in Root: an old-owner query must fail.
        self.mutate(self.research, [
            ('CREATE TABLE r29_research_owner(singleton,phase,schema_hash,source_root,epoch)', ()),
            ("INSERT INTO r29_research_owner VALUES(1,'READY',?,?,42)", (self.schema,self.source)),
            ('CREATE TABLE r29_event_inbox(event_id,source_seq,issue,payload_hash,origin)', ()),
            ("INSERT INTO r29_event_inbox VALUES('event',1,'202610300001',?,'API')", (digest,)),
            ('CREATE TABLE r29_learning_work(consumer,event_id,issue,status,state_hash)', ()),
            ("INSERT INTO r29_learning_work VALUES('ATOMNET_ONLINE','event','202610300001','COMMITTED','state')", ()),
            ('CREATE TABLE r29_learning_receipt(consumer,issue,state_hash)', ()),
            ("INSERT INTO r29_learning_receipt VALUES('ATOMNET_ONLINE','202610300001','state')", ()),
            ('CREATE TABLE r27_task_error(error_id,task_id,error_class,message)', ()),
        ])
        shutil.copy2(self.research,self.baseline)
        self.pin_baseline()
        schema_json = json.dumps({'columns':['issue']},separators=(',',':'))
        schema_hash = hashlib.sha256(schema_json.encode()).hexdigest()
        text = '202610300001'.encode()
        typed = struct.pack('>II',0x29DB0001,1)+b'\x03'+struct.pack('>I',len(text))+text
        row_hash = hashlib.sha256(typed).hexdigest()
        chain = hashlib.sha256(('R29_TYPED_ROW_CHAIN_V1|draws|'+schema_hash).encode()).hexdigest()
        chain = hashlib.sha256((chain+':1:'+row_hash).encode()).hexdigest()
        self.mutate(self.stage, [
            ('CREATE TABLE r29_copy_manifest(table_name,state,schema_json,schema_hash,attempt,source_generation,copied_rows,source_rows,verified_rows,copy_hash,verify_hash)',()),
            ("INSERT INTO r29_copy_manifest VALUES('draws','VERIFIED_BASELINE',?,?,1,3,1,1,1,?,?)",(schema_json,schema_hash,chain,chain)),
            ('CREATE TABLE r29_copy_rows(table_name,attempt,source_rowid,payload,row_hash)',()),
            ("INSERT INTO r29_copy_rows VALUES('draws',1,1,?,?)",(typed,row_hash)),
        ])

    def tearDown(self):
        self.tmp.cleanup()

    @staticmethod
    def mutate(path, commands):
        with sqlite3.connect(path) as db:
            for sql,args in commands: db.execute(sql,args)

    def pin_baseline(self):
        self.mutate(self.root,[('UPDATE r29_migration_publication SET baseline_sha=?',(hashlib.sha256(self.baseline.read_bytes()).hexdigest(),))])

    def inspect(self):
        result={}
        reader.inspect(self.root,self.stage,result,self.research,self.baseline)
        return result

    def rejected(self,reason):
        with self.assertRaisesRegex(AssertionError,reason): self.inspect()

    def test_active_owner_reads_real_rp_without_root_learner_tables(self):
        r=self.inspect()
        self.assertTrue(r['cutoverEnabled'])
        self.assertEqual('starcore-r29-research.db',r['activeOwner'])
        self.assertEqual(1,r['modelReceipts'])
        self.assertTrue(r['sealedBaselineVerified'])

    def test_no_actual_rp_copy_cannot_fallback_to_root(self):
        self.research.unlink()
        self.rejected('ACTIVE_RESEARCH_DATABASE_MISSING')
        self.assertFalse(self.research.exists())

    def test_route_owner_epoch_mismatch_rejected(self):
        self.mutate(self.research,[('UPDATE r29_research_owner SET epoch=43',())])
        self.rejected('ACTIVE_RESEARCH_OWNER_MISMATCH')

    def test_unpublished_route_rejected(self):
        self.mutate(self.root,[("UPDATE r29_migration_publication SET phase='RENAMED'",())])
        self.rejected('RESEARCH_PUBLICATION_MISMATCH')

    def test_source_hash_mismatch_rejected(self):
        self.mutate(self.root,[('UPDATE r29_migration_publication SET source_root=?',('c'*64,))])
        self.rejected('RESEARCH_PUBLICATION_MISMATCH')

    def test_sealed_baseline_bytes_not_merely_receipt_checked(self):
        with self.baseline.open('ab') as out: out.write(b'corrupt')
        self.rejected('SEALED_RESEARCH_BASELINE_SHA_MISMATCH')

    def test_rehashed_wrong_baseline_owner_rejected(self):
        self.mutate(self.baseline,[('UPDATE r29_research_owner SET epoch=99',())])
        self.pin_baseline()
        self.rejected('SEALED_RESEARCH_BASELINE_OWNER_MISMATCH')

    def test_equal_counts_do_not_hide_ack_for_wrong_event(self):
        self.mutate(self.research,[("UPDATE r29_event_inbox SET event_id='other'",())])
        self.rejected('CROSS_FILE_EVENT_PROTOCOL_MISMATCH')

    def test_same_event_wrong_hash_rejected(self):
        self.mutate(self.research,[("UPDATE r29_event_inbox SET payload_hash='wrong'",())])
        self.rejected('CROSS_FILE_EVENT_PROTOCOL_MISMATCH')

    def test_same_event_wrong_sequence_rejected(self):
        self.mutate(self.research,[('UPDATE r29_event_inbox SET source_seq=2',())])
        self.rejected('CROSS_FILE_EVENT_PROTOCOL_MISMATCH')

    def test_outbox_payload_corruption_rejected(self):
        self.mutate(self.root,[("UPDATE r29_event_outbox SET payload_json='{}'",())])
        self.rejected('CROSS_FILE_EVENT_PROTOCOL_MISMATCH')

    def test_committed_work_without_real_receipt_rejected(self):
        self.mutate(self.research,[('DELETE FROM r29_learning_receipt',())])
        self.rejected('')

    def test_stage_typed_copy_hash_still_checked(self):
        self.mutate(self.stage,[("UPDATE r29_copy_rows SET row_hash='wrong'",())])
        self.rejected('')

    def test_rp_task_failure_is_not_hidden_by_empty_root_log(self):
        self.mutate(self.research,[("INSERT INTO r27_task_error VALUES(1,'r29-event-inbox','Failure','real retained error')",())])
        self.rejected('NEW_R29_TASK_ERRORS')

    def test_root_task_failure_remains_visible(self):
        self.mutate(self.root,[("INSERT INTO r27_task_error VALUES(1,'r29-research-migration','Failure','real retained error')",())])
        self.rejected('NEW_R29_TASK_ERRORS')

    def test_no_events_report_insufficient_instead_of_delivery_pass(self):
        self.mutate(self.root,[('DELETE FROM r29_event_outbox',())])
        self.mutate(self.research,[('DELETE FROM r29_event_inbox',())])
        self.assertEqual('INSUFFICIENT_NO_ACCEPTED_EVENT',self.inspect()['eventDeliveryStatus'])

    def test_legacy_reader_cannot_silently_ignore_active_route(self):
        with self.assertRaisesRegex(AssertionError,'ACTIVE_ROUTE_REQUIRES_INDEPENDENT_RESEARCH_COPY'):
            reader.inspect(self.root,self.stage,{})

    def test_old_unpublished_stage_owner_keeps_original_assertions(self):
        with sqlite3.connect(self.root) as db:
            db.execute('DELETE FROM r29_database_route')
            db.execute('ATTACH DATABASE ? AS rp',(str(self.research),))
            for table in ('r29_event_inbox','r29_learning_work','r29_learning_receipt'):
                db.execute('CREATE TABLE '+table+' AS SELECT * FROM rp.'+table)
        result={}
        reader.inspect(self.root,self.stage,result)
        self.assertFalse(result['cutoverEnabled'])
        self.assertEqual('ORIGINAL_DATABASE',result['activeOwner'])
        self.assertEqual('PASS_BASELINE_ONLY_CATCHUP_ROUTING_PENDING',result['stagedBackupStatus'])

    def test_observation_without_rp_preserves_counts_without_accepting(self):
        self.research.unlink()
        result=reader.observe(self.root,self.stage,{})
        self.assertEqual('OBSERVATION_ONLY_NOT_ACCEPTANCE',result['status'])
        self.assertEqual('NOT_INSPECTED',result['researchCopy'])
        self.assertEqual(1,result['trackedSourceTables'])
        self.assertEqual(1,result['stageManifestTotal'])
        self.assertFalse(self.research.exists())
        self.rejected('ACTIVE_RESEARCH_DATABASE_MISSING')

    def test_missing_stage_is_observed_without_creating_fake_database(self):
        self.stage.unlink()
        result=reader.observe(self.root,self.stage,{})
        self.assertFalse(result['stageAvailable'])
        self.assertFalse(self.stage.exists())
        self.assertEqual('OBSERVATION_ONLY_NOT_ACCEPTANCE',result['status'])

    def test_actual_shell_keeps_observation_then_fails_missing_rp(self):
        # Execute the actual checked-in helper with file-copy pulls; no adb, network or device is used.
        script=Path(__file__).with_name('emulator_smoke.sh').read_text()
        helpers=script[script.index('pull_database() {'):script.index('adb uninstall "$PKG"')]
        stub='''
pull_database() {
  case "$1" in
    binance_k3_nova_one_pro_v57.db) cp "$TEST_ROOT" "$OUT/$2.db" ;;
    starcore-research-r29-stage.db) cp "$TEST_STAGE" "$OUT/$2.db" ;;
    *) return 19 ;;
  esac
}
pull_runtime upgrade after
echo UNREACHABLE_ACCEPTANCE
'''
        out=self.path/'smoke';out.mkdir()
        env=dict(os.environ,OUT=str(out),TEST_ROOT=str(self.root),TEST_STAGE=str(self.stage))
        run=subprocess.run(['bash','-c','set -euo pipefail\n'+helpers+stub],cwd=Path(__file__).resolve().parents[2],env=env,capture_output=True,text=True,timeout=10)
        self.assertEqual(19,run.returncode,run.stdout+run.stderr)
        self.assertNotIn('UNREACHABLE_ACCEPTANCE',run.stdout)
        receipt=json.loads((out/'upgrade-migration-observation.json').read_text())
        self.assertEqual('OBSERVATION_ONLY_NOT_ACCEPTANCE',receipt['status'])
        self.assertEqual(1,receipt['stageManifestTotal'])


if __name__=='__main__': unittest.main(verbosity=2)
