#!/usr/bin/env python3
import importlib.util,json,tempfile,pathlib,hashlib,unittest
spec=importlib.util.spec_from_file_location('apply_batch',pathlib.Path(__file__).with_name('apply_batch.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
h=lambda b:hashlib.sha256(b).hexdigest()
class ApplyTest(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory();self.root=pathlib.Path(self.t.name);self.package=self.root/'p';self.repo=self.root/'r';self.package.mkdir();self.repo.mkdir()
  self.entries=[]
  for n in ['a','b']:
   (self.repo/n).write_bytes(b'old');(self.package/n).write_bytes(b'new')
   self.entries.append(dict(path=n,source=n,before_sha256=h(b'old'),after_sha256=h(b'new'),action='REPLACE'))
  self.doc={'revision':'TEST','batches':{'P1':{'prerequisites':[],'files':self.entries},'P2':{'prerequisites':['P1'],'files':[]}}};self.save()
 def tearDown(self):self.t.cleanup()
 def save(self):(self.package/'code_manifest.json').write_text(json.dumps(self.doc))
 def test_check_is_readonly(self):
  r=m.execute(self.package,self.repo,'P1');self.assertEqual(r['pending_files'],2);self.assertEqual((self.repo/'a').read_bytes(),b'old')
 def test_apply_and_idempotence(self):
  self.assertEqual(m.execute(self.package,self.repo,'P1',True)['changed_files'],2);self.assertEqual(m.execute(self.package,self.repo,'P1',True)['changed_files'],0)
 def test_drift_refuses_before_any_write(self):
  (self.repo/'b').write_bytes(b'user edit')
  with self.assertRaisesRegex(ValueError,'SOURCE_DRIFT'):m.execute(self.package,self.repo,'P1',True)
  self.assertEqual((self.repo/'a').read_bytes(),b'old')
 def test_prerequisite_cannot_skip(self):
  with self.assertRaisesRegex(ValueError,'PREREQUISITE_DRIFT'):m.execute(self.package,self.repo,'P2',True)
 def test_package_corrupt(self):
  (self.package/'b').write_bytes(b'corrupt')
  with self.assertRaisesRegex(ValueError,'PACKAGE_CORRUPT'):m.execute(self.package,self.repo,'P1',True)
  self.assertEqual((self.repo/'a').read_bytes(),b'old')
 def test_path_escape(self):
  self.entries[0]['path']='../escape';self.save()
  with self.assertRaisesRegex(ValueError,'UNSAFE_PATH'):m.execute(self.package,self.repo,'P1',True)
 def test_symlink_escape(self):
  (self.repo/'a').unlink();(self.root/'external').write_bytes(b'old');(self.repo/'a').symlink_to(self.root/'external')
  with self.assertRaisesRegex(ValueError,'SYMLINK_ESCAPE'):m.execute(self.package,self.repo,'P1',True)
 def test_partial_interruption_rolls_forward(self):
  (self.repo/'a').write_bytes(b'new');self.assertEqual(m.execute(self.package,self.repo,'P1',True)['changed_files'],1)
 def test_rollback_on_write_failure(self):
  orig=m.os.replace;calls=[]
  def fail_second(a,b):
   calls.append(str(b))
   if len(calls)==2:raise OSError('injected')
   return orig(a,b)
  m.os.replace=fail_second
  try:
   with self.assertRaises(OSError):m.execute(self.package,self.repo,'P1',True)
  finally:m.os.replace=orig
  self.assertEqual((self.repo/'a').read_bytes(),b'old');self.assertEqual((self.repo/'b').read_bytes(),b'old')
if __name__=='__main__':unittest.main()
