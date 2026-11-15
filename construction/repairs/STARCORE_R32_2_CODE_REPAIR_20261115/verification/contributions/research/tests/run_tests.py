#!/usr/bin/env python3
"""Compile the real two-file addendum against a whole-application build, then run new and original JUnit tests."""
import argparse,hashlib,json,pathlib,subprocess,os
p=argparse.ArgumentParser()
for name in ('repo','classes','android-jar','junit','hamcrest','out'):p.add_argument('--'+name,type=pathlib.Path,required=True)
a=p.parse_args();root=pathlib.Path(__file__).resolve().parents[1];out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
for name in ('repo','classes','android_jar','junit','hamcrest'):setattr(a,name,getattr(a,name).resolve())
src=root/'after/app/src/main/java/com/ethk3/novaonepro/strategyv2'
tests=[root/'after/app/src/test/java/com/ethk3/novaonepro/strategyv2/R32PublishedModelDuringTrainingTest.java']
tests += [a.repo/'app/src/test/java/com/ethk3/novaonepro/strategyv2'/n for n in ('R30TenPositionTrainingTest.java','R31AtomNetCompleteIssueConsumerTest.java')]
prod=[src/n for n in ('RuntimeR27.java','AtomNetRuntimeR30.java')]
cp=os.pathsep.join(map(str,[out,a.classes,a.android_jar,a.junit,a.hamcrest]))
receipt={'scope':'HOST_ONLY','device':'NOT_RUN','naturalQuality':'NOT_EVALUATED','inputs':{},'commands':[]}
for path in prod+tests+[a.android_jar,a.junit,a.hamcrest]:receipt['inputs'][str(path)]=hashlib.sha256(path.read_bytes()).hexdigest()
commands=[['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-source','17','-target','17','-proc:none','-cp',cp,'-d',str(out),*map(str,prod+tests)],['java','-Xmx512m','-cp',cp,'org.junit.runner.JUnitCore',*['com.ethk3.novaonepro.strategyv2.'+n for n in ('R32PublishedModelDuringTrainingTest','R30TenPositionTrainingTest','R31AtomNetCompleteIssueConsumerTest')]]]
for i,cmd in enumerate(commands):
 r=subprocess.run(cmd,text=True,capture_output=True);(out/('stage'+str(i)+'.log')).write_text(r.stdout+r.stderr)
 receipt['commands'].append({'argv':cmd,'returncode':r.returncode});(out/'RESULTS.json').write_text(json.dumps(receipt,indent=2)+'\n')
 if r.returncode:raise SystemExit(r.returncode)
print((out/'stage1.log').read_text())
