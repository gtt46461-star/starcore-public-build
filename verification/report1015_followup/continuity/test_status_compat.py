import importlib.util
from pathlib import Path
import json
import unittest
P=Path(__file__).with_name('status_compat.py')
spec=importlib.util.spec_from_file_location('status_compat',P);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def chain(rows):
 out=[];prev='0'*64
 for action,payload in rows:
  e={'action':action,'payload':payload,'previous_sha256':prev};e['event_sha256']=m.digest(m.canonical(e));prev=e['event_sha256'];out.append(e)
 return out
class Tests(unittest.TestCase):
 def test_hash_chain_valid_and_tamper_rejected(self):
  rows=chain([('INIT',{}),('CHECKPOINT',{'kind':'PARTIAL'})]);raw=b''.join(m.canonical(e)+b'\n' for e in rows);self.assertEqual(2,len(m.verify_chain(raw)))
  with self.assertRaises(ValueError):m.verify_chain(raw.replace(b'PARTIAL',b'CHANGED'))
 def test_truncated_rejected(self):
  with self.assertRaises(ValueError):m.verify_chain(b'{}')
 def test_partial_never_overwrites_standard_failed(self):
  events=chain([('INIT',{}),('APPLIED',{'stage':'O09','source_sha256':'a'}),('CHECKPOINT',{'stage':'O09','lane':'engineering','status':'FAILED','source_sha256':'a'}),('CHECKPOINT',{'stage':'O09','kind':'SCOPED','engineering':'PARTIAL_SCOPE_PASS'})]);s,a,p,u=m.replay_read_only(events);self.assertEqual('FAILED',s['O09']['engineering']);self.assertEqual('NOT_CLOSED',p[0]['effective']);self.assertEqual(1,len(p))
 def test_unknown_retained(self):
  events=chain([('INIT',{}),('FUTURE_EXTENSION',{'status':'PASS'})]);s,a,p,u=m.replay_read_only(events);self.assertEqual('NOT_CLOSED',u[0]['effective'])
 def test_invalid_lane_not_accepted(self):
  events=chain([('INIT',{}),('CHECKPOINT',{'stage':'O09','lane':'all','status':'VERIFIED','source_sha256':'a'})]);s,a,p,u=m.replay_read_only(events);self.assertEqual({},s);self.assertEqual(1,len(p))
 def test_unknown_status_not_accepted(self):
  events=chain([('INIT',{}),('CHECKPOINT',{'stage':'O09','lane':'engineering','status':'PASS','source_sha256':'a'})]);s,a,p,u=m.replay_read_only(events);self.assertEqual({},s);self.assertEqual('NOT_CLOSED',p[0]['effective'])
 def test_path_traversal_rejected(self):
  with self.assertRaises(ValueError):m.safe(Path('/tmp/audit-sandbox'),'../escape')
if __name__=='__main__':unittest.main()
