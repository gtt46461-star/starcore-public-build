#!/usr/bin/env python3
"""Resume inspection regressions; synthetic source files never sign app acceptance."""
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('resume_tool',HERE/'inspect_resume.py')
resume=importlib.util.module_from_spec(spec);spec.loader.exec_module(resume)


class ResumeInspectionTest(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.root=Path(self.tmp.name);self.package=self.root/'package';self.repo=self.root/'repo'
        self.package.mkdir();self.repo.mkdir();(self.package/'tools').mkdir()
        shutil.copy2(HERE/'apply_batch.py',self.package/'tools/apply_batch.py')
        self.apply=resume.load_apply(self.package)
        rows={}
        for name in ['a','b','c','d']:
            (self.repo/name).write_bytes(b'old');(self.package/name).write_bytes(b'new')
            rows[name]={'path':name,'source':name,'action':'REPLACE',
                'before_sha256':resume.digest(b'old'),'after_sha256':resume.digest(b'new'),
                'owner':'P1' if name in ['a','b'] else 'P2'}
        self.manifest={'revision':'TEST_RESUME_ONLY','batches':{
            'P1':{'prerequisites':[],'files':[rows['a'],rows['b']]},
            'P2':{'prerequisites':['P1'],'files':[rows['c'],rows['d']]}}}
        (self.package/'code_manifest.json').write_text(json.dumps(self.manifest))

    def tearDown(self):
        self.tmp.cleanup()

    def inspect(self):
        return resume.inspect(self.package,self.repo)

    def file_tree(self):
        return {str(p.relative_to(self.repo)):p.read_bytes() for p in self.repo.rglob('*') if p.is_file()}

    def kill_after_a(self):
        script="""
import pathlib,sys,types,os
path=pathlib.Path(sys.argv[1]);m=types.ModuleType('apply');m.__file__=str(path)
exec(compile(path.read_bytes(),str(path),'exec'),m.__dict__)
p,r=pathlib.Path(sys.argv[2]),pathlib.Path(sys.argv[3]);original=m.atomic_write
def kill(path,data,mode=0o644):
    original(path,data,mode)
    if path==r/'a':os._exit(74)
m.atomic_write=kill;m.execute(p,r,'P1',True)
"""
        child=subprocess.run([sys.executable,'-c',script,str(self.package/'tools/apply_batch.py'),str(self.package),str(self.repo)])
        self.assertEqual(child.returncode,74)

    def test_baseline_is_readonly_and_resumes_p1(self):
        before=self.file_tree();result=self.inspect();self.assertEqual(self.file_tree(),before)
        self.assertEqual(result['status'],'BASELINE_READY')
        self.assertEqual(result['next_source_batch'],'P1');self.assertEqual(result['writes_performed'],0)
        self.assertFalse((self.repo/self.apply.STATE).exists())

    def test_applied_stage_requires_current_regression_before_next(self):
        self.apply.execute(self.package,self.repo,'P1',True)
        result=self.inspect()
        self.assertEqual(result['furthest_matching_source_batch'],'P1')
        self.assertEqual(result['next_source_batch'],'P2')
        self.assertEqual(result['verify_current_batch_before_advancing'],'P1')
        self.assertEqual(result['business_acceptance'],'NOT_EVALUATED')

    def test_complete_bytes_do_not_sign_device_or_quality(self):
        self.apply.execute(self.package,self.repo,'P1',True);self.apply.execute(self.package,self.repo,'P2',True)
        result=self.inspect()
        self.assertEqual(result['status'],'SOURCE_COMPLETE');self.assertIsNone(result['next_source_batch'])
        self.assertEqual(result['android_device_status'],'NOT_EVALUATED')
        self.assertEqual(result['natural_quality'],'NOT_EVALUATED')

    def test_partial_known_bytes_without_journal_resume_same_batch(self):
        (self.repo/'a').write_bytes(b'new')
        result=self.inspect()
        self.assertEqual(result['status'],'PARTIAL_REVIEWED_BATCH_WITHOUT_JOURNAL')
        self.assertEqual(result['resume_batch'],'P1')
        self.assertEqual(result['pending_source_files_for_identified_batch'],1)

    def test_real_interruption_identified_and_successful_resume(self):
        self.kill_after_a();before=self.file_tree();result=self.inspect()
        self.assertEqual(self.file_tree(),before)
        self.assertEqual(result['status'],'INTERRUPTED_BATCH');self.assertEqual(result['resume_batch'],'P1')
        self.apply.execute(self.package,self.repo,result['resume_batch'],True)
        self.assertEqual(self.inspect()['furthest_matching_source_batch'],'P1')

    def test_different_plan_cannot_claim_old_transaction(self):
        self.kill_after_a()
        self.manifest['revision']='DIFFERENT_PLAN'
        (self.package/'code_manifest.json').write_text(json.dumps(self.manifest))
        with self.assertRaisesRegex(ValueError,'UNFINISHED_OTHER_PLAN'):
            self.inspect()

    def test_corrupted_backup_refused_without_writing(self):
        self.kill_after_a();state=self.repo/self.apply.STATE
        pointer=json.loads((state/'active.json').read_text());tx=state/'transactions'/pointer['transaction']
        journal=json.loads((tx/'journal.json').read_text());(tx/journal['rows'][0]['backup']).write_bytes(b'broken')
        before=self.file_tree()
        with self.assertRaisesRegex(ValueError,'JOURNAL_BACKUP_CORRUPT'):
            self.inspect()
        self.assertEqual(self.file_tree(),before)

    def test_unknown_edit_and_future_out_of_order_bytes_refused(self):
        (self.repo/'a').write_bytes(b'custom edit')
        with self.assertRaisesRegex(ValueError,'SOURCE_STATE_UNKNOWN_OR_DRIFT'):
            self.inspect()
        (self.repo/'a').write_bytes(b'old');(self.repo/'c').write_bytes(b'new')
        with self.assertRaisesRegex(ValueError,'SOURCE_STATE_UNKNOWN_OR_DRIFT'):
            self.inspect()

    def test_stale_receipt_does_not_advance_source_stage(self):
        state=self.repo/self.apply.STATE;state.mkdir()
        (state/'P2.json').write_text(json.dumps({'manifest_sha256':'old','scope':'SOURCE_BYTES_ONLY_NOT_BUSINESS_ACCEPTANCE','status':'PASS'}))
        result=self.inspect()
        self.assertEqual(result['status'],'BASELINE_READY')
        self.assertEqual(result['source_receipts']['P2'],'STALE_OR_DIFFERENT_SCOPE_NOT_EVIDENCE')

    def test_declared_permission_drift_is_readonly_refusal(self):
        for b in self.manifest['batches'].values():
            for entry in b['files']:entry['mode']='644'
        (self.package/'code_manifest.json').write_text(json.dumps(self.manifest))
        (self.repo/'a').chmod(0o600);before=self.file_tree()
        with self.assertRaisesRegex(ValueError,'SOURCE_STATE_UNKNOWN_OR_DRIFT a'):
            self.inspect()
        self.assertEqual(self.file_tree(),before)
        self.assertEqual((self.repo/'a').stat().st_mode & 0o777,0o600)

    def test_mode_only_transition_is_identified(self):
        entry=self.manifest['batches']['P1']['files'][0]
        entry.update(action='PRESERVE_REFERENCE',after_sha256=resume.digest(b'old'),before_mode='644',mode='755')
        (self.package/'a').write_bytes(b'old')
        (self.package/'code_manifest.json').write_text(json.dumps(self.manifest))
        self.assertEqual(self.inspect()['status'],'BASELINE_READY')
        (self.repo/'a').chmod(0o755)
        result=self.inspect()
        self.assertEqual(result['status'],'PARTIAL_REVIEWED_BATCH_WITHOUT_JOURNAL')
        self.assertEqual(result['resume_batch'],'P1')
        self.apply.execute(self.package,self.repo,'P1',True)
        self.assertEqual(self.inspect()['furthest_matching_source_batch'],'P1')

    def test_tampered_journal_mode_refused_without_writing(self):
        self.kill_after_a();state=self.repo/self.apply.STATE
        pointer=json.loads((state/'active.json').read_text());tx=state/'transactions'/pointer['transaction']
        journal=json.loads((tx/'journal.json').read_text());journal['rows'][0]['after_mode']=0o1777
        (tx/'journal.json').write_text(json.dumps(journal));before=self.file_tree()
        with self.assertRaisesRegex(ValueError,'JOURNAL_MODE_MISMATCH'):
            self.inspect()
        self.assertEqual(self.file_tree(),before)

    def test_inspection_refuses_while_writer_holds_lock(self):
        with self.apply.repo_lock(self.repo):
            with self.assertRaisesRegex(ValueError,'REPO_BUSY_ANOTHER_APPLY'):
                self.inspect()


if __name__=='__main__':
    unittest.main(verbosity=2)
