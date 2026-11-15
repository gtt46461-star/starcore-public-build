#!/usr/bin/env python3
"""Acceptance rescheduling safety, independent of artifact presence/sign-off."""
import copy
import unittest
from verify_package import validate_acceptance_migration, validate_file_card_coverage


class AcceptanceMigration(unittest.TestCase):
    def setUp(self):
        self.original=[{'id':'R31-B05-C05#A1','oldRun':'P1','runAt':'P1',
            'textSha256':'original-text-hash','observable':'原文不得改：有根保全，无根环可删，pin与GC只有一方成功',
            'card':'P1.06','ownerFile':'CheckpointGcStoreR31.java'}]
        self.current=copy.deepcopy(self.original)
        self.current[0].update(currentBatchContract='batches/P1.md')
        self.files={'CheckpointGcStoreR31.java':{'owner':'P1'}}
        self.cards=[{'id':'P1.06','batch':'P1','files':['CheckpointGcStoreR31.java']},
                    {'id':'P2.06','batch':'P2','files':['CheckpointGcStoreR31.java']}]

    def check(self):
        return validate_acceptance_migration(self.current,self.files,self.cards,self.original)

    def test_unchanged_contract(self):
        self.assertEqual(self.check()['rescheduled'],0)

    def test_actual_owner_moved_but_old_acceptance_falsely_early(self):
        self.files['CheckpointGcStoreR31.java']['owner']='P2'
        with self.assertRaisesRegex(ValueError,'RUN_BEFORE_SOURCE_OWNER'):self.check()

    def test_moved_owner_with_current_run_card_reason(self):
        self.files['CheckpointGcStoreR31.java']['owner']='P2'
        self.current[0].update(runAt='P2',card='P2.06',currentBatchContract='batches/P2.md',migration_reason='跨文件闭环整体迁到P2，P1不提前签收')
        self.assertEqual(self.check()['rescheduled'],1)

    def test_moved_run_cannot_keep_old_card(self):
        self.current[0].update(runAt='P2',currentBatchContract='batches/P2.md',migration_reason='移动')
        with self.assertRaisesRegex(ValueError,'CARD_BATCH_DIFFERS'):self.check()

    def test_current_document_cannot_point_future_or_old_batch(self):
        self.current[0]['currentBatchContract']='batches/P2.md'
        with self.assertRaisesRegex(ValueError,'DOCUMENT_DIFFERS'):self.check()

    def test_original_schedule_is_not_rewritten(self):
        self.current[0]['oldRun']='P2'
        with self.assertRaisesRegex(ValueError,'ORIGINAL_CHANGED'):self.check()

    def test_original_clause_text_is_not_weakened(self):
        self.current[0]['observable']='无根环可以永久保留'
        with self.assertRaisesRegex(ValueError,'ORIGINAL_CHANGED'):self.check()

    def test_original_text_hash_is_not_rewritten(self):
        self.current[0]['textSha256']='new-hash'
        with self.assertRaisesRegex(ValueError,'ORIGINAL_CHANGED'):self.check()

    def test_migration_requires_explicit_reason(self):
        self.current[0].update(runAt='P2',card='P2.06',currentBatchContract='batches/P2.md')
        with self.assertRaisesRegex(ValueError,'REASON_MISSING'):self.check()

    def test_owner_file_must_belong_to_current_issue(self):
        self.cards[0]['files']=[]
        with self.assertRaisesRegex(ValueError,'OWNER_FILE_NOT_IN_CURRENT_CARD'):self.check()

    def test_original_acceptance_cannot_disappear(self):
        self.current=[]
        with self.assertRaisesRegex(ValueError,'ORIGINAL_ID_SET_CHANGED'):self.check()

    def test_batch_names_are_not_arbitrarily_accepted(self):
        self.current[0]['runAt']='P0'
        with self.assertRaisesRegex(ValueError,'RUN_OR_CARD_UNKNOWN'):self.check()



class FileCardCoverage(unittest.TestCase):
    def test_missing_owned_reference_is_refused(self):
        with self.assertRaisesRegex(ValueError,'FILE_CARD_COVERAGE missing=b'):
            validate_file_card_coverage({'a':{},'b':{}},[{'id':'P1.01','files':['a']}])
    def test_complete_coverage_allows_cross_issue_references(self):
        value=validate_file_card_coverage({'a':{'cards':['P1.01','P1.02']},'b':{'cards':['P1.02']}},
            [{'id':'P1.01','files':['a']},{'id':'P1.02','files':['a','b']}])
        self.assertEqual(value['covered_paths'],2)
        self.assertEqual(value['issue_file_relations'],3)
    def test_stale_manifest_binding_is_refused(self):
        with self.assertRaisesRegex(ValueError,'FILE_CARD_MANIFEST_BINDING_MISMATCH'):
            validate_file_card_coverage({'a':{'cards':[]}},[{'id':'P1.01','files':['a']}])

if __name__=='__main__':unittest.main(verbosity=2)
