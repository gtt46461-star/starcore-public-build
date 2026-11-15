#!/usr/bin/env python3
"""Execute every source-bound selector; retain every failure and exact denominator."""
from pathlib import Path
import hashlib,json,re,subprocess,sys

TARGET='com.starcore.research.r13'
RUNNER=TARGET+'.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner'

def decoded(value):
    return value.decode('utf-8','replace') if isinstance(value,bytes) else value or ''

def run_suite(manifest_path,source_root=Path('.'),output=Path('android-r13.txt'),receipt=Path('r26-instrumentation-results.json'),execute=subprocess.run):
    manifest=json.loads(manifest_path.read_text());classes=manifest['classes']
    assert manifest['schema']=='R26_INSTRUMENTATION_MANIFEST_V1' and classes
    assert manifest['total']==sum(c['count'] for c in classes)
    selectors=[]
    for item in classes:
        name=item['className'];assert hashlib.sha256((source_root/item['sourcePath']).read_bytes()).hexdigest()==item['sourceSha256'],'Instrumentation source/artifact mismatch: '+name
        rows=[(name+'#'+method,1) for method in item['methods']] if item.get('isolateEachMethod',False) else [(name,item['count'])]
        assert rows and sum(count for _,count in rows)==item['count']
        selectors.extend((name,selector,count) for selector,count in rows)
    results=[]
    with output.open('w') as report:
        for name,selector,expected in selectors:
            entry={'selector':selector,'expected':expected,'executed':0,'status':'FAIL'};text=''
            try:
                execute(['adb','shell','am','force-stop',TARGET],check=True)
                run=execute(['adb','shell','am','instrument','-w','-e','class',selector,RUNNER],capture_output=True,text=True,timeout=600)
                text=decoded(run.stdout)+decoded(run.stderr);entry['returncode']=run.returncode
                passed=re.search(r'OK \((\d+) tests?\)',text);failed=re.search(r'Tests run:\s*(\d+),\s*Failures:\s*(\d+)',text)
                entry['executed']=int(passed.group(1)) if passed else int(failed.group(1)) if failed else 0
                if failed:entry['reportedFailures']=int(failed.group(2))
                if run.returncode==0 and passed and not re.search(r'FAILURES|INSTRUMENTATION_FAILED|Process crashed|shortMsg=',text) and entry['executed']==expected:entry['status']='PASS'
                else:entry['category']='ASSERTION_CRASH_OR_DENOMINATOR'
            except subprocess.TimeoutExpired as failure:
                text=decoded(failure.stdout)+decoded(failure.stderr);entry['category']='INSTRUMENTATION_TIMEOUT'
            except subprocess.CalledProcessError as failure:
                entry['category']='ADB_PROCESS_FAILURE';entry['returncode']=failure.returncode
            if name.endswith('.R26RuntimeDeviceTest'):
                with output.with_name('r26-owner-logcat.txt').open('w') as trace:execute(['adb','logcat','-d','-s','STARCORE_R26:V','TestRunner:V','AndroidRuntime:E','*:S'],stdout=trace,stderr=subprocess.STDOUT,check=False)
            heading=f'R26_SELECTOR|selector={selector}|expected={expected}|status={entry["status"]}\n'
            report.write(heading+text+'\n');report.flush();print(heading+text,flush=True);results.append(entry)
    executed=sum(r['executed'] for r in results);failures=[r for r in results if r['status']!='PASS'];complete=executed==manifest['total'] and not failures
    summary={'schema':'R26_INSTRUMENTATION_RESULT_V1','scope':'SOURCE_BOUND_ENGINEERING_NOT_NATURAL_QUALITY','manifestSha256':hashlib.sha256(manifest_path.read_bytes()).hexdigest(),'declaredTests':manifest['total'],'executedTests':executed,'notExecutedTests':max(0,manifest['total']-executed),'failedSelectors':len(failures),'selectors':results,'status':'PASS' if complete else 'FAIL'}
    receipt.write_text(json.dumps(summary,indent=2)+'\n')
    with output.open('a') as report:report.write(f'Isolated class aggregate: {summary["status"]} (executed={executed}; declared={manifest["total"]}; failedSelectors={len(failures)})\n')
    print('R26_INSTRUMENTATION_AGGREGATE='+json.dumps({k:v for k,v in summary.items() if k!='selectors'},sort_keys=True),flush=True)
    return 0 if complete else 1

def self_test():
    import tempfile,unittest
    class Regression(unittest.TestCase):
        def invoke(self,answers,isolated=False):
            with tempfile.TemporaryDirectory(prefix='r26-suite-test-') as root:
                root=Path(root);source=root/'fixture.java';source.write_text('SYNTHETIC_RUNNER_FIXTURE');rows=[{'className':'SyntheticA','count':2 if isolated else 1,'methods':['one','two'] if isolated else ['one'],'isolateEachMethod':isolated},{'className':'SyntheticB','count':1,'methods':['one']}]
                for row in rows:row.update(sourcePath=source.name,sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest())
                path=root/'manifest.json';path.write_text(json.dumps({'schema':'R26_INSTRUMENTATION_MANIFEST_V1','classes':rows,'total':sum(r['count'] for r in rows)}));calls=[];answers=iter(answers)
                def fake(args,**kwargs):
                    calls.append(args)
                    if 'instrument' not in args:return subprocess.CompletedProcess(args,0,'','')
                    answer=next(answers)
                    if isinstance(answer,Exception):raise answer
                    return subprocess.CompletedProcess(args,0,answer,'')
                from unittest.mock import patch
                with patch('builtins.print'):status=run_suite(path,root,root/'report.txt',root/'result.json',fake)
                return status,json.loads((root/'result.json').read_text()),calls
        def test_failure_does_not_skip_later_selector(self):
            status,result,calls=self.invoke(['FAILURES!!!\nTests run: 1,  Failures: 1\n','OK (1 test)\n']);self.assertEqual(1,status);self.assertEqual(2,result['executedTests']);self.assertEqual(1,result['failedSelectors']);self.assertEqual('PASS',result['selectors'][1]['status'])
        def test_crash_after_ok_never_passes_or_skips_next(self):
            status,result,_=self.invoke(['OK (1 test)\nINSTRUMENTATION_FAILED: Process crashed\n','OK (1 test)\n']);self.assertEqual(1,status);self.assertEqual('FAIL',result['selectors'][0]['status']);self.assertEqual('PASS',result['selectors'][1]['status'])
        def test_wrong_denominator_never_passes(self):
            status,result,_=self.invoke(['OK (2 tests)\n','OK (1 test)\n']);self.assertEqual(1,status);self.assertEqual('FAIL',result['status'])
        def test_method_isolation_forces_a_new_process_each_time(self):
            status,result,calls=self.invoke(['OK (1 test)\n']*3,True);self.assertEqual(0,status);self.assertEqual(3,result['executedTests']);self.assertEqual(3,sum('force-stop' in c for c in calls));self.assertEqual('SyntheticA#two',result['selectors'][1]['selector'])
        def test_timeout_retains_failure_and_continues(self):
            status,result,_=self.invoke([subprocess.TimeoutExpired(['SYNTHETIC'],600,output=b'partial'), 'OK (1 test)\n']);self.assertEqual(1,status);self.assertEqual('INSTRUMENTATION_TIMEOUT',result['selectors'][0]['category']);self.assertEqual('PASS',result['selectors'][1]['status']);self.assertEqual(1,result['notExecutedTests'])
    result=unittest.TextTestRunner().run(unittest.defaultTestLoader.loadTestsFromTestCase(Regression));return 0 if result.wasSuccessful() else 1

if __name__=='__main__':sys.exit(self_test() if '--self-test' in sys.argv else run_suite(Path('delivered/package-receipt/instrumentation-tests.json')))
