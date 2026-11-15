#!/usr/bin/env python3
"""Require stable adb/boot/system services before product checks; no app pass is inferred."""
from pathlib import Path
import json,subprocess,time,sys

def wait_ready(timeout=60,execute=subprocess.run,clock=time.monotonic,pause=time.sleep,receipt=Path('r26-adb-preflight.json')):
    start=clock();deadline=start+timeout;stable=0;prior_pid=None;observations=[]
    checks=[(['adb','get-state'],lambda s:s.strip()=='device'),(['adb','shell','getprop','sys.boot_completed'],lambda s:s.strip()=='1'),(['adb','shell','pidof','system_server'],lambda s:s.strip().isdigit()),*[( ['adb','shell','service','check',name],lambda s:'found' in s and 'not found' not in s) for name in ('package','activity','battery')]]
    while clock()<deadline:
        ok=True;pid='';failed=''
        for index,(args,valid) in enumerate(checks):
            remaining=deadline-clock()
            if remaining<=0:ok=False;failed='DEADLINE';break
            try:r=execute(args,capture_output=True,text=True,timeout=min(5,remaining));text=r.stdout or '';ok=r.returncode==0 and valid(text)
            except (subprocess.TimeoutExpired,subprocess.CalledProcessError):ok=False;text=''
            if index==2:pid=text.strip()
            if not ok:failed=('ADB_STATE','BOOT_COMPLETE','SYSTEM_SERVER','PACKAGE_SERVICE','ACTIVITY_SERVICE','BATTERY_SERVICE')[index];break
        stable=stable+1 if ok and pid==prior_pid else 1 if ok else 0;prior_pid=pid if ok else None
        observations.append({'elapsedMs':int((clock()-start)*1000),'healthy':ok,'stableObservations':stable,'failedPrerequisite':failed});observations=observations[-16:]
        if stable>=3:break
        remaining=deadline-clock()
        if remaining>0:pause(min(1,remaining))
    result={'scope':'CI_ADB_BOOT_SERVICES_ONLY_NOT_APP_STARTUP_OR_STRATEGY_ACCEPTANCE','result':'READY' if stable>=3 else 'FAIL_TIMEOUT','elapsedMs':int((clock()-start)*1000),'stableObservations':stable,'observations':observations};receipt.write_text(json.dumps(result,indent=2)+'\n')
    if stable<3:raise RuntimeError('ANDROID_TRANSPORT_OR_BOOT_SERVICES_NOT_STABLE; product checks not started')
    print('R26_ADB_PREFLIGHT_READY|stableObservations='+str(stable),flush=True);return result

def self_test():
    import tempfile,unittest
    class Regression(unittest.TestCase):
        def run_case(self,states,timeout=8):
            with tempfile.TemporaryDirectory(prefix='r13-adb-ready-') as root:
                now=[0.0];poll=[0];states=list(states)
                def pause(delta):now[0]+=delta
                def fake(args,**kwargs):
                    state=states[min(poll[0],len(states)-1)]
                    if args==['adb','get-state']:
                        poll[0]+=1;return subprocess.CompletedProcess(args,0,'device' if state.get('online',True) else 'offline','')
                    state=states[min(poll[0]-1,len(states)-1)]
                    if 'getprop' in args:text='1' if state.get('boot',True) else '0'
                    elif 'pidof' in args:text=state.get('pid','100')
                    else:text='Service '+args[-1]+(': found' if state.get('services',True) else ': not found')
                    return subprocess.CompletedProcess(args,0,text,'')
                p=Path(root)/'receipt.json'
                try:r=wait_ready(timeout,fake,lambda:now[0],pause,p);return r,poll[0]
                except RuntimeError:return json.loads(p.read_text()),poll[0]
        def test_offline_never_passes(self):r,_=self.run_case([{'online':False}]);self.assertEqual('FAIL_TIMEOUT',r['result'])
        def test_offline_before_healthy_needs_three_observations(self):r,n=self.run_case([{'online':False},{},{},{}]);self.assertEqual('READY',r['result']);self.assertEqual(4,n)
        def test_new_system_server_resets_stability(self):r,n=self.run_case([{'pid':'100'},{'pid':'200'},{'pid':'200'},{'pid':'200'}]);self.assertEqual('READY',r['result']);self.assertEqual(4,n)
        def test_boot_without_services_is_not_ready(self):r,_=self.run_case([{'services':False}]);self.assertEqual('FAIL_TIMEOUT',r['result'])
        def test_dropped_transport_resets_stable_count(self):r,n=self.run_case([{}, {'online':False},{},{},{}]);self.assertEqual('READY',r['result']);self.assertEqual(5,n)
    r=unittest.TextTestRunner().run(unittest.defaultTestLoader.loadTestsFromTestCase(Regression));return 0 if r.wasSuccessful() else 1
if __name__=='__main__':sys.exit(self_test() if '--self-test' in sys.argv else (wait_ready() and 0))
