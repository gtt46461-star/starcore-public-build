#!/usr/bin/env python3
"""Exercise the actual invocation wrapper, including same-SHA stale PASS and early failure."""
import contextlib,importlib.util,io,json,os,tempfile,types,unittest
import sys
sys.dont_write_bytecode=True
from pathlib import Path
from unittest.mock import patch
spec=importlib.util.spec_from_file_location('current_gate',Path(__file__).with_name('run_current_acceptance.py'))
current=importlib.util.module_from_spec(spec);spec.loader.exec_module(current)

class InvocationTest(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name);self.out=self.root/'output';self.out.mkdir()
  self.gate=types.SimpleNamespace(ledger=lambda:{'batches':[{'batch':'K11'}]},load_suite=lambda:{'host':[{'id':'required'}]},items=lambda:[],ref_status=lambda s,r:'PASS')
  self.result={'gradle':{'state':'PASS','exit':0,'compile_failed':False,'unit_task_executed':True},'unit':{'tests':622,'failures':0,'errors':0,'skipped':0},'host':[{'id':'required','status':'PASS'}],'host_failed':[]}
  self.gate.build=lambda b:dict(self.result)
 def tearDown(self):self.temp.cleanup()
 def run_wrapper(self,fingerprint=None):
  fake_spec=types.SimpleNamespace(loader=types.SimpleNamespace(exec_module=lambda m:None))
  with contextlib.ExitStack() as stack:
   stack.enter_context(patch.object(current,'ROOT',self.root));stack.enter_context(patch.object(current,'fingerprint',side_effect=fingerprint or ['same-inputs','same-inputs']))
   stack.enter_context(patch.object(current.subprocess,'check_output',return_value='a'*40+'\n'))
   stack.enter_context(patch.object(current.importlib.util,'spec_from_file_location',return_value=fake_spec));stack.enter_context(patch.object(current.importlib.util,'module_from_spec',return_value=self.gate))
   stack.enter_context(patch('sys.argv',['gate','--out',str(self.out)]));stack.enter_context(patch.dict(os.environ,{'R31_INVOCATION_ID':'new-run','GITHUB_RUN_ID':'test-run'}));stack.enter_context(contextlib.redirect_stdout(io.StringIO()))
   rc=current.main()
  return rc,json.loads((self.out/'BUILD_SUMMARY.json').read_text())
 def stale(self):
  (self.out/'BUILD_SUMMARY.json').write_text(json.dumps({'state':'PASS_ENGINEERING_ONLY','source_head':'a'*40,'unit':{'tests':9999},'invocationId':'old'}))
 def test_early_failure_does_not_reuse_same_head(self):
  self.stale();self.gate.build=lambda b:(_ for _ in ()).throw(SystemExit('PREFLIGHT'))
  rc,row=self.run_wrapper();self.assertEqual(1,rc);self.assertEqual(0,row['unit']['tests']);self.assertEqual('FAILED_OR_INCOMPLETE',row['state']);self.assertIn('PREFLIGHT',row['wrapperFailure']);self.assertTrue((self.out/'PREVIOUS-new-run.json').is_file())
 def test_fingerprint_failure_also_writes_new_failed_receipt(self):
  self.stale();rc,row=self.run_wrapper([FileNotFoundError('missing-input'),FileNotFoundError('missing-input')]);self.assertEqual(1,rc);self.assertEqual('FAILED_OR_INCOMPLETE',row['state']);self.assertEqual('new-run',row['invocationId'])
 def test_missing_suite_fails(self):
  self.result['host']=[];rc,row=self.run_wrapper();self.assertEqual(1,rc);self.assertTrue(any('MISSING_OR_FAILED_HOST' in p for p in row['problems']))
 def test_no_test_execution_fails(self):
  self.result['gradle']['unit_task_executed']=False;rc,row=self.run_wrapper();self.assertEqual(1,rc);self.assertIn('COMPILE_OR_UNIT_NOT_EXECUTED',row['problems'])
 def test_failed_command_fails(self):
  self.result['gradle']['exit']=1;rc,row=self.run_wrapper();self.assertEqual(1,rc);self.assertEqual('FAILED_OR_INCOMPLETE',row['state'])
 def test_changed_source_and_dropped_counts_fail(self):
  self.result['unit']['tests']=621;rc,row=self.run_wrapper(['same-inputs','changed-inputs']);self.assertEqual(1,rc);self.assertIn('SOURCE_OR_CONTROL_INPUTS_CHANGED',row['problems']);self.assertIn('UNIT_COUNT_DROPPED_OR_EMPTY',row['problems'])
 def test_fresh_complete_invocation_can_pass_engineering_only(self):
  rc,row=self.run_wrapper();self.assertEqual(0,rc);self.assertEqual('PASS_ENGINEERING_ONLY',row['state']);self.assertEqual('NOT_RUN',row['device']);self.assertEqual('NOT_PROVEN',row['naturalQuality'])
 def test_original_gate_exposes_the_actual_ledger_and_build_api(self):
  spec=importlib.util.spec_from_file_location('real_original_gate_api',current.PLAN/'tools/r31_gate.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
  self.assertTrue(callable(module.ledger));self.assertTrue(callable(module.build));self.assertTrue(any(x['batch']=='K11' for x in module.ledger()['batches']))

if __name__=='__main__':unittest.main()
