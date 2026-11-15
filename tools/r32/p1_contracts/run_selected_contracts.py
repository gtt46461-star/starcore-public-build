#!/usr/bin/env python3
"""Execute original mainline/retirement tests against one stage's compiled classes.

Uses the repository Test annotation and explicit fail-closed host assertion API.
This is original test method execution, not a JUnit/Android Gradle platform run.
"""
import argparse, hashlib, json, os
from pathlib import Path
import subprocess, sys


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('repo','classes','sdk','out'):parser.add_argument('--'+name,required=True,type=Path)
    parser.add_argument('--selector',action='append')
    args=parser.parse_args()
    repo,classes,sdk,out=(getattr(args,n).resolve()for n in ('repo','classes','sdk','out'))
    out.mkdir(parents=True,exist_ok=False);(out/'classes').mkdir()
    here=Path(__file__).resolve().parent
    manifest=json.loads((repo/'tools/r31/retirement_manifest.json').read_text())
    tests={repo/'app/src/test/java/com/ethk3/novaonepro/strategyv2/R27QualityTest.java',repo/'app/src/test/java/com/ethk3/novaonepro/strategyv2/R27TestSupport.java',repo/'app/src/test/java/com/ethk3/novaonepro/R31RetirementManifestTest.java'}
    for capability in manifest['preserved_capabilities']:
        for test in capability['tests']:
            if not test.startswith('HOST:'):tests.add(repo/('app/src/test/java/'+test.split('#')[0].replace('.','/')+'.java'))
    support=[here/name for name in ('Assert.java','Before.java','After.java','HostSelectedTests.java')]+[repo/'tools/r30/host/junit_shim/org/junit/Test.java']
    guards=sorted(tests)+support+[Path(__file__).resolve(),repo/'app/src/main/java/com/ethk3/novaonepro/DbStore.java',repo/'tools/r31/retirement_manifest.json',repo/'verification/r31/unified-schema-review.json']
    if (repo/'verification/r32/p1_dbstore_review.json').exists():guards.append(repo/'verification/r32/p1_dbstore_review.json')
    before={str(p):sha(p)for p in guards}
    support_before={p.relative_to(classes).as_posix():sha(p)for p in sorted(classes.rglob('*.class'))}
    cp=os.pathsep.join(map(str,[out/'classes',classes,sdk]))
    compile_command=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-source','17','-target','17','-proc:none','-sourcepath',str(repo/'app/src/test/java'),'-cp',cp,'-d',str(out/'classes'),*map(str,sorted(tests)),*map(str,support)]
    compiled=subprocess.run(compile_command,cwd=repo/'app',text=True,capture_output=True);(out/'compile.log').write_text(compiled.stdout+compiled.stderr)
    if compiled.returncode:print(compiled.stderr);return compiled.returncode
    selectors=args.selector or ['com.ethk3.novaonepro.strategyv2.R27QualityTest#h15MainlineRedLines','com.ethk3.novaonepro.strategyv2.R27QualityTest#h15R32ExactReviewRejectsUnregisteredChanges','com.ethk3.novaonepro.R31RetirementManifestTest#*']
    run_command=['java','-Xmx512m','-cp',cp,'HostSelectedTests',*selectors]
    run=subprocess.run(run_command,cwd=repo/'app',text=True,capture_output=True);(out/'run.log').write_text(run.stdout+run.stderr);print(run.stdout+run.stderr)
    if before!={str(p):sha(p)for p in guards}:raise ValueError('CONTRACT_INPUT_CHANGED_DURING_RUN')
    if support_before!={p.relative_to(classes).as_posix():sha(p)for p in sorted(classes.rglob('*.class'))}:raise ValueError('SUPPORT_CLASSES_CHANGED_DURING_RUN')
    watched=sorted(tests)+support
    receipt={'state':'PASS'if run.returncode==0 else'FAIL','scope':'ACTUAL_ORIGINAL_SELECTED_TEST_METHODS_ON_STAGE_CLASSES_HOST_ASSERTION_ADAPTER_NOT_ANDROID_OR_FULL_JUNIT','selectors':selectors,'compiledTestSources':{str(p):sha(p)for p in watched},'guardedInputs':before,'supportClasses':str(classes),'supportClassManifestSha256':hashlib.sha256(json.dumps(support_before,sort_keys=True).encode()).hexdigest(),'sourceDbStore':sha(repo/'app/src/main/java/com/ethk3/novaonepro/DbStore.java'),'retirementManifest':sha(repo/'tools/r31/retirement_manifest.json'),'commands':[compile_command,run_command],'exitCode':run.returncode}
    (out/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    return run.returncode


if __name__=='__main__':sys.exit(main())
