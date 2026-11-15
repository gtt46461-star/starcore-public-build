#!/usr/bin/env python3
"""Apply every batch to the exact source snapshot and type-check each intermediate state."""
import pathlib,subprocess,json,hashlib,sys,shutil,zipfile,datetime,importlib.util,argparse
p=argparse.ArgumentParser();p.add_argument('--package',type=pathlib.Path,required=True);p.add_argument('--sdk',type=pathlib.Path,required=True);p.add_argument('--out',type=pathlib.Path,required=True);a=p.parse_args();a.package=a.package.resolve();a.out=a.out.resolve();a.out.mkdir(parents=True,exist_ok=False)
work=a.out/'project';work.mkdir()
with zipfile.ZipFile(a.package/'SOURCE_BASELINE.zip') as z:
 for n in z.namelist():
  if pathlib.PurePosixPath(n).is_absolute() or '..' in pathlib.PurePosixPath(n).parts:raise ValueError('archive path')
 z.extractall(work)
manifest=json.loads((a.package/'code_manifest.json').read_text());results=[]
# compile_rehearsal reads the already included, tracked numerical source from app/build/generated/source/r967.
for batch in ['P1','P2','P3','P4','P5']:
 cmd=[sys.executable,str(a.package/'tools/apply_batch.py'),'--package',str(a.package),'--repo',str(work),'--batch',batch,'--apply']
 app=subprocess.run(cmd,capture_output=True,text=True);(a.out/(batch+'-apply.log')).write_text(app.stdout+app.stderr);app.check_returncode()
 cmd=[sys.executable,str(a.package/'reference/R32.1/verify/compile_rehearsal.py'),'--repo',str(work),'--android-jar',str(a.sdk),'--out',str(a.out/(batch+'-classes'))]
 run=subprocess.run(cmd,capture_output=True,text=True);(a.out/(batch+'-typecheck.log')).write_text(run.stdout+run.stderr)
 row={'batch':batch,'applyExit':app.returncode,'typecheckExit':run.returncode,'scope':'HOST_JAVA_TYPECHECK_WITH_GENERATED_RESOURCE_DECLARATIONS_NOT_ANDROID','log':batch+'-typecheck.log'};results.append(row)
 (a.out/'RESULTS.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n');print(json.dumps(row),flush=True);run.check_returncode()
 # Keep the latest classes only; bytecode is not an installation artifact.
 if batch!='P5':shutil.rmtree(a.out/(batch+'-classes'))
print('PASS five sequential source batches compile without future-batch code',flush=True)
