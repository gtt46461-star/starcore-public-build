import hashlib, importlib.util, json, os, pathlib, sys
root=pathlib.Path.cwd();out=pathlib.Path(os.environ['RUNNER_TEMP'])/'r3251-api35-retest/evidence'
scope={'buildSource':'bb7b49bbcc9266bad548ec37e71403256fc06a4d','originalRun':43796780479,'selectedOriginalGroup':'delivery-and-live-service','expectedOriginalTests':7,'originalLiveFutureFreezeDeadlineMs':360000,'originalAssertionsModified':False,'applicationApksRebuilt':False,'otherPassedApi35Groups':'Retained from original run; not inferred from this selected retest','physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN','apks':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (root/'built').glob('*.apk')}}
(out.parent/'RETEST_SCOPE.json').write_text(json.dumps(scope,indent=2)+'\n')
sys.path.insert(0,str(root/'tools/r31'))
import run_android_joint_protocols as original
original.SUITES=[s for s in original.SUITES if s[0]=='delivery-and-live-service']
assert len(original.SUITES)==1 and original.SUITES[0][1]==7
sys.argv=['run_android_joint_protocols.py','--out',str(out),'--sandbox-prefix','r3251v35-repeat']
original.main()
