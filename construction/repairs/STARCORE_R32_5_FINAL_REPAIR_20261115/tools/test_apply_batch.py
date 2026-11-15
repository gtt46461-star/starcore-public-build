#!/usr/bin/env python3
"""Regression cases for reviewed source application, including real process death."""
import hashlib
import importlib.util
import json
import pathlib
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

MODULE = pathlib.Path(__file__).with_name('apply_batch.py')
spec = importlib.util.spec_from_file_location('apply_batch', MODULE)
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
h = lambda data: hashlib.sha256(data).hexdigest()


class ApplyTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = pathlib.Path(self.tmp.name)
        self.package, self.repo = self.root/'p', self.root/'r'
        self.package.mkdir(); self.repo.mkdir()
        self.entries = []
        for name in ['a', 'b']:
            (self.repo/name).write_bytes(b'old')
            (self.package/name).write_bytes(b'new')
            self.entries.append(dict(path=name, source=name, before_sha256=h(b'old'),
                                     after_sha256=h(b'new'), action='REPLACE', owner='P1'))
        self.doc = {'revision': 'TEST', 'batches': {
            'P1': {'prerequisites': [], 'files': self.entries},
            'P2': {'prerequisites': ['P1'], 'files': []}}}
        self.save()

    def tearDown(self):
        self.tmp.cleanup()

    def save(self):
        (self.package/'code_manifest.json').write_text(json.dumps(self.doc))

    def apply(self, batch='P1'):
        return m.execute(self.package, self.repo, batch, True)

    def unchanged(self):
        self.assertEqual((self.repo/'a').read_bytes(), b'old')
        self.assertEqual((self.repo/'b').read_bytes(), b'old')

    def test_check_is_readonly(self):
        result = m.execute(self.package, self.repo, 'P1')
        self.assertEqual(result['pending_files'], 2)
        self.assertFalse((self.repo/m.STATE).exists())
        self.unchanged()

    def test_apply_and_idempotence(self):
        self.assertEqual(self.apply()['changed_files'], 2)
        self.assertEqual(self.apply()['changed_files'], 0)
        receipt = json.loads((self.repo/m.STATE/'P1.json').read_text())
        self.assertEqual(receipt['scope'], 'SOURCE_BYTES_ONLY_NOT_BUSINESS_ACCEPTANCE')

    def test_drift_refuses_before_any_source_write(self):
        (self.repo/'b').write_bytes(b'user edit')
        with self.assertRaisesRegex(ValueError, 'SOURCE_DRIFT'):
            self.apply()
        self.assertEqual((self.repo/'a').read_bytes(), b'old')

    def test_prerequisite_cannot_skip(self):
        with self.assertRaisesRegex(ValueError, 'PREREQUISITE_DRIFT'):
            self.apply('P2')

    def test_transitive_prerequisite_cannot_skip(self):
        self.doc['batches']['P3'] = {'prerequisites': ['P2'], 'files': []}
        self.save()
        with self.assertRaisesRegex(ValueError, 'PREREQUISITE_DRIFT P1'):
            self.apply('P3')

    def test_payload_corruption(self):
        (self.package/'b').write_bytes(b'corrupt')
        with self.assertRaisesRegex(ValueError, 'PACKAGE_CORRUPT'):
            self.apply()
        self.unchanged()

    def test_duplicate_owner(self):
        self.doc['batches']['P2']['files'] = [dict(self.entries[0], owner='P2')]
        self.save()
        with self.assertRaisesRegex(ValueError, 'DUPLICATE_OWNER'):
            self.apply()
        self.unchanged()

    def test_unknown_action(self):
        self.entries[0]['action'] = 'DO_WHATEVER'
        self.save()
        with self.assertRaisesRegex(ValueError, 'ACTION_HASH_CONTRADICTION'):
            self.apply()

    def test_preservation_cannot_hide_replacement(self):
        self.entries[0]['action'] = 'PRESERVE_REFERENCE'
        self.save()
        with self.assertRaisesRegex(ValueError, 'ACTION_HASH_CONTRADICTION'):
            self.apply()

    def test_future_prerequisite(self):
        self.doc['batches']['P1']['prerequisites'] = ['P2']
        self.save()
        with self.assertRaisesRegex(ValueError, 'FUTURE_OR_UNKNOWN_PREREQUISITE'):
            self.apply()

    def test_path_escape(self):
        self.entries[0]['path'] = '../escape'
        self.save()
        with self.assertRaisesRegex(ValueError, 'UNSAFE_PATH'):
            self.apply()

    def test_internal_symlink_is_rejected(self):
        (self.repo/'a').unlink()
        (self.repo/'a').symlink_to(self.repo/'b')
        with self.assertRaisesRegex(ValueError, 'SYMLINK_PATH'):
            self.apply()

    def test_external_symlink_is_rejected(self):
        (self.repo/'a').unlink()
        (self.root/'external').write_bytes(b'old')
        (self.repo/'a').symlink_to(self.root/'external')
        with self.assertRaisesRegex(ValueError, 'SYMLINK_PATH'):
            self.apply()
        self.assertEqual((self.root/'external').read_bytes(), b'old')

    def test_receipt_symlink_is_rejected(self):
        (self.root/'external').mkdir()
        (self.repo/m.STATE).symlink_to(self.root/'external')
        with self.assertRaisesRegex(ValueError, 'SYMLINK_PATH'):
            self.apply()

    def test_second_applicator_refused(self):
        with m.repo_lock(self.repo):
            with self.assertRaisesRegex(ValueError, 'REPO_BUSY'):
                self.apply()
        self.unchanged()

    def test_partial_preexisting_post_state_rolls_forward(self):
        (self.repo/'a').write_bytes(b'new')
        self.assertEqual(self.apply()['changed_files'], 1)

    def test_rollback_on_source_write_failure(self):
        original = m.atomic_write
        def fail_b(path, data, mode=0o644):
            if path == self.repo/'b':
                raise OSError('injected write failure')
            return original(path, data, mode)
        with mock.patch.object(m, 'atomic_write', fail_b):
            with self.assertRaisesRegex(OSError, 'injected'):
                self.apply()
        self.unchanged()
        self.assertFalse((self.repo/m.STATE/'P1.json').exists())
        self.assertFalse((self.repo/m.STATE/'active.json').exists())

    def test_concurrent_external_edit_is_not_overwritten_on_rollback(self):
        original = m.write_json
        edited = False
        def edit_after_preparation(path, value):
            nonlocal edited
            original(path, value)
            if value.get('state') == 'PREPARED' and not edited:
                edited = True
                (self.repo/'b').write_bytes(b'new external edit')
        with mock.patch.object(m, 'write_json', edit_after_preparation):
            with self.assertRaisesRegex(ValueError, 'SOURCE_CHANGED_AFTER_CHECK'):
                self.apply()
        self.assertEqual((self.repo/'a').read_bytes(), b'old')
        self.assertEqual((self.repo/'b').read_bytes(), b'new external edit')
        active = json.loads((self.repo/m.STATE/'active.json').read_text())
        journal = json.loads((self.repo/m.STATE/'transactions'/active['transaction']/'journal.json').read_text())
        self.assertEqual(journal['state'], 'RECOVERY_CONFLICT')

    def test_real_process_death_can_resume(self):
        code = """
import importlib.util, pathlib, os, sys
spec=importlib.util.spec_from_file_location('m',sys.argv[1]);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
p,r=pathlib.Path(sys.argv[2]),pathlib.Path(sys.argv[3]);original=m.atomic_write
def kill_after_first_source(path,data,mode=0o644):
    original(path,data,mode)
    if path==r/'a':os._exit(73)
m.atomic_write=kill_after_first_source
m.execute(p,r,'P1',True)
"""
        child = subprocess.run([sys.executable, '-c', code, str(MODULE), str(self.package), str(self.repo)])
        self.assertEqual(child.returncode, 73)
        self.assertEqual((self.repo/'a').read_bytes(), b'new')
        self.assertEqual((self.repo/'b').read_bytes(), b'old')
        self.assertFalse((self.repo/m.STATE/'P1.json').exists())
        with self.assertRaisesRegex(ValueError, 'UNFINISHED_TRANSACTION_RESUME P1'):
            self.apply('P2')
        result = self.apply()
        self.assertTrue(result['resumed'])
        self.assertEqual(result['changed_files'], 1)
        self.assertEqual((self.repo/'b').read_bytes(), b'new')

    def test_existing_executable_mode_preserved(self):
        (self.repo/'a').chmod(0o755)
        self.apply()
        self.assertEqual((self.repo/'a').stat().st_mode & 0o777, 0o755)

    def test_new_file_has_declared_mode(self):
        (self.repo/'a').unlink()
        self.entries[0].update(action='ADD', before_sha256=None, mode='755')
        self.save(); self.apply()
        self.assertEqual((self.repo/'a').stat().st_mode & 0o777, 0o755)

    def test_source_edit_cannot_change_manifest_during_apply(self):
        original = m.atomic_write
        def mutate_after_source(path, data, mode=0o644):
            original(path, data, mode)
            if path == self.repo/'a' and data == b'new':
                self.doc['revision'] = 'OTHER_PLAN'
                self.save()
        with mock.patch.object(m, 'atomic_write', mutate_after_source):
            with self.assertRaisesRegex(ValueError, 'PACKAGE_MANIFEST_CHANGED'):
                self.apply()
        self.unchanged()

    def test_declared_mode_transition_sets_output_and_receipt(self):
        self.entries[0].update(before_mode='644', mode='755'); self.save()
        result = self.apply()
        self.assertEqual((self.repo/'a').stat().st_mode & 0o777, 0o755)
        self.assertEqual(result['files'][0]['declared_after_mode'], '0o755')
        self.assertEqual(self.apply()['changed_files'], 0)

    def test_undeclared_old_permission_change_refused(self):
        self.entries[0]['mode'] = '755'; self.save()
        with self.assertRaisesRegex(ValueError, 'SOURCE_MODE_DRIFT a'):
            self.apply()
        self.unchanged()
        self.assertFalse((self.repo/m.STATE/'P1.json').exists())

    def test_matching_bytes_with_wrong_mode_cannot_get_receipt(self):
        self.entries[0]['mode'] = '644'; self.save()
        (self.repo/'a').write_bytes(b'new'); (self.repo/'a').chmod(0o600)
        with self.assertRaisesRegex(ValueError, 'SOURCE_MODE_DRIFT a'):
            m.execute(self.package, self.repo, 'P1')
        with self.assertRaisesRegex(ValueError, 'SOURCE_MODE_DRIFT a'):
            self.apply()
        self.assertEqual((self.repo/'a').stat().st_mode & 0o777, 0o600)
        self.assertFalse((self.repo/m.STATE/'P1.json').exists())

    def test_prerequisite_mode_drift_refused(self):
        self.entries[0]['mode'] = '644'; self.save(); self.apply()
        (self.repo/'a').chmod(0o755)
        with self.assertRaisesRegex(ValueError, 'PREREQUISITE_DRIFT P1 a'):
            self.apply('P2')

    def test_declared_mode_rollback_restores_original_permission(self):
        self.entries[0].update(before_mode='644', mode='755'); self.save()
        original = m.atomic_write
        def fail_b(path, data, mode=0o644):
            if path == self.repo/'b': raise OSError('mode rollback injected')
            return original(path, data, mode)
        with mock.patch.object(m, 'atomic_write', fail_b):
            with self.assertRaisesRegex(OSError, 'mode rollback injected'): self.apply()
        self.unchanged()
        self.assertEqual((self.repo/'a').stat().st_mode & 0o777, 0o644)
        self.assertFalse((self.repo/m.STATE/'P1.json').exists())

    def test_mode_only_reviewed_transition(self):
        self.entries[0].update(action='PRESERVE_REFERENCE', after_sha256=h(b'old'), before_mode='644', mode='755')
        (self.package/'a').write_bytes(b'old'); self.save()
        self.apply()
        self.assertEqual((self.repo/'a').read_bytes(), b'old')
        self.assertEqual((self.repo/'a').stat().st_mode & 0o777, 0o755)

    def test_real_process_death_resumes_declared_mode_transition(self):
        self.entries[0].update(before_mode='644', mode='755'); self.save()
        code = """
import importlib.util,pathlib,os,sys
spec=importlib.util.spec_from_file_location('m',sys.argv[1]);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
p,r=pathlib.Path(sys.argv[2]),pathlib.Path(sys.argv[3]);original=m.atomic_write
def kill(path,data,mode=0o644):
    original(path,data,mode)
    if path==r/'a':os._exit(75)
m.atomic_write=kill;m.execute(p,r,'P1',True)
"""
        child = subprocess.run([sys.executable,'-c',code,str(MODULE),str(self.package),str(self.repo)])
        self.assertEqual(child.returncode,75)
        self.assertEqual((self.repo/'a').stat().st_mode & 0o777,0o755)
        self.assertTrue(self.apply()['resumed'])
        self.assertEqual((self.repo/'b').read_bytes(),b'new')

    def test_permission_edit_during_apply_is_preserved_on_conflict(self):
        self.entries[0].update(before_mode='644', mode='755'); self.save()
        original = m.atomic_write
        def edit_mode(path,data,mode=0o644):
            original(path,data,mode)
            if path==self.repo/'a' and data==b'new': path.chmod(0o600)
        with mock.patch.object(m,'atomic_write',edit_mode):
            with self.assertRaisesRegex(ValueError,'WRITE_VERIFY a'): self.apply()
        self.assertEqual((self.repo/'a').read_bytes(),b'new')
        self.assertEqual((self.repo/'a').stat().st_mode & 0o777,0o600)
        pointer=json.loads((self.repo/m.STATE/'active.json').read_text())
        journal=json.loads((self.repo/m.STATE/'transactions'/pointer['transaction']/'journal.json').read_text())
        self.assertEqual(journal['state'],'RECOVERY_CONFLICT')
        self.assertIn('a',journal['conflicts'])

    def test_invalid_before_mode_declaration_refused(self):
        self.entries[0].update(before_mode='777',mode='755');self.save()
        with self.assertRaisesRegex(ValueError,'UNSUPPORTED_MODE'):self.apply()

    def add_derived(self):
        path = 'release/identity.json'
        (self.repo/'release').mkdir()
        (self.repo/path).write_bytes(b'base identity')
        (self.package/'identity1').write_bytes(b'stage one')
        (self.package/'identity2').write_bytes(b'stage two')
        self.doc['derived_file_owners'] = {path: {'owner': 'P1', 'generator': 'tools/freeze_stage_identity.py'}}
        self.doc['batches']['P1']['derived_files'] = [dict(path=path, source='identity1', owner='P1',
             action='REPLACE', before_sha256=h(b'base identity'), after_sha256=h(b'stage one'))]
        self.doc['batches']['P2']['derived_files'] = [dict(path=path, source='identity2', owner='P1',
             action='REPLACE', before_sha256=h(b'stage one'), after_sha256=h(b'stage two'))]
        self.save()
        return path

    def test_derived_identity_has_one_owner_and_serial_stage_chain(self):
        path = self.add_derived()
        self.apply()
        self.assertEqual((self.repo/path).read_bytes(), b'stage one')
        self.apply('P2')
        self.assertEqual((self.repo/path).read_bytes(), b'stage two')
        self.assertEqual(self.apply('P2')['changed_files'], 0)

    def test_derived_identity_chain_break_refused(self):
        self.add_derived()
        self.doc['batches']['P2']['derived_files'][0]['before_sha256'] = h(b'wrong predecessor')
        self.save()
        with self.assertRaisesRegex(ValueError, 'DERIVED_CHAIN_BREAK'):
            self.apply()
        self.unchanged()

    def test_derived_identity_cannot_have_manual_owner_as_well(self):
        path = self.add_derived()
        self.entries.append(dict(self.doc['batches']['P1']['derived_files'][0]))
        self.save()
        with self.assertRaisesRegex(ValueError, 'DUPLICATE_OWNER'):
            self.apply()

    def test_earlier_identity_cannot_overwrite_later_stage(self):
        self.add_derived()
        self.apply(); self.apply('P2')
        with self.assertRaisesRegex(ValueError, 'SOURCE_DRIFT'):
            self.apply('P1')



class StagePreflightTest(unittest.TestCase):
    setUp = ApplyTest.setUp
    tearDown = ApplyTest.tearDown
    save = ApplyTest.save
    unchanged = ApplyTest.unchanged

    def stage(self, added=False):
        generator = self.repo/'scripts/r967_semantic_identity.py'
        generator.parent.mkdir()
        generator.write_text("""from pathlib import Path

def controlled_paths(root):
    paths = {'a', 'b', 'scripts/r967_semantic_identity.py'}
    paths.update(p.relative_to(root).as_posix() for p in root.rglob('*.java'))
    return paths

def build(root):
    return {'training_rule_sha256': 'reviewed-test-rule', 'compatible_predecessor_rules_sha256': []}
""")
        pins = [{'path': name, 'sha256': h(b'new')} for name in ['a', 'b']]
        pins.append({'path': 'scripts/r967_semantic_identity.py', 'sha256': h(generator.read_bytes())})
        if added:
            (self.package/'new-java').write_bytes(b'final class Added {}')
            self.entries.append(dict(path='Added.java', source='new-java', before_sha256=None,
                after_sha256=h(b'final class Added {}'), action='ADD', owner='P1'))
            pins.append({'path': 'Added.java', 'sha256': h(b'final class Added {}')})
        snapshot = {'schema': 'R32_EXACT_STAGE_IDENTITY_V1', 'batch': 'P1',
                    'qualification_inheritance': False, 'controlled_inputs': pins,
                    'training_rule_sha256': 'reviewed-test-rule', 'release_inputs': []}
        data=(json.dumps(snapshot)+'\n').encode()
        (self.package/'stage.json').write_bytes(data)
        self.doc['batches']['P1']['stage_identity']={'snapshot':'stage.json','sha256':h(data)}
        self.save()

    def test_check_rejects_extra_controlled_source_before_any_write(self):
        self.stage()
        (self.repo/'Unreviewed.java').write_text('final class Unreviewed {}')
        with self.assertRaisesRegex(ValueError, 'UNREVIEWED_STAGE_PATH_SET'):
            m.execute(self.package, self.repo, 'P1')
        self.unchanged()
        self.assertFalse((self.repo/m.STATE).exists())

    def test_check_projects_reviewed_added_source(self):
        self.stage(added=True)
        result=m.execute(self.package, self.repo, 'P1')
        self.assertEqual(result['pending_files'],3)
        self.assertFalse((self.repo/'Added.java').exists())
        self.unchanged()

    def test_check_keeps_source_tree_without_python_cache(self):
        self.stage()
        before=sorted(str(p.relative_to(self.repo)) for p in self.repo.rglob('*'))
        m.execute(self.package,self.repo,'P1')
        after=sorted(str(p.relative_to(self.repo)) for p in self.repo.rglob('*'))
        self.assertEqual(before,after)
        self.assertFalse(any(self.repo.rglob('__pycache__')))

    def test_apply_checks_projected_and_actual_controlled_paths(self):
        self.stage(added=True)
        result=m.execute(self.package,self.repo,'P1',True)
        self.assertEqual(result['changed_files'],3)
        self.assertEqual((self.repo/'Added.java').read_bytes(),b'final class Added {}')
        self.assertEqual(m.execute(self.package,self.repo,'P1',True)['changed_files'],0)

if __name__ == '__main__':
    unittest.main(verbosity=2)
