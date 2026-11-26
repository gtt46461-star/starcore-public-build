#!/usr/bin/env python3
"""Run the plan's bounded host regressions in an isolated workspace.

Needs Python 3.12+, git and a JDK with jdk.compiler. No Android claim is made.
"""
import argparse,hashlib,json,os,shutil,subprocess,sys,tarfile,tempfile,time
from pathlib import Path

BASE='4d1de9d83692635b5c5ce39144fece1c8a31c8ac'
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--plan',type=Path,required=True);p.add_argument('--source-repo',type=Path,required=True);p.add_argument('--out',type=Path,required=True)
 a=p.parse_args();plan=a.plan.resolve();repo=a.source_repo.resolve();out=a.out.resolve()
 if out.is_relative_to(plan) or out.exists():raise ValueError('Use a fresh output directory outside the sealed plan')
 out.mkdir(parents=True);work=out/'workspace';work.mkdir();audit=work/'audit';audit.mkdir();base=work/'source-installed';base.mkdir()
 # Extract exact 173 host dependencies. Never mutate the user's application.
 archive=out/'host-source.tar'
 with archive.open('wb') as f:subprocess.run(['git','-C',str(repo),'archive',BASE,'app/src/main/java','verification/canonical',
      'tools/r33/verify_plan_binding.py','tools/r33/advance_release_version.py','scripts/r926_release_manifest.py',
      'release/release_manifest.json','release/release_identity.properties','verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java'],stdout=f,check=True)
 with tarfile.open(archive) as t:t.extractall(base,filter='data')
 archive.unlink()
 for src in (plan/'host_validation').iterdir():
  if src.is_dir():shutil.copytree(src,audit/src.name)
 stages={'runtime':('runtime_patch/after',),'research_core':('research_patch/after',),
         'rolling':('research_patch/rolling_candidate','rolling_after'),'charts':('charts_patch/after',),
         'delivery':('delivery_patch/after',),'creation':('creation_patch/after',),'field_binding':('field_binding_patch/after',)}
 for group,locations in stages.items():
  for location in locations:shutil.copytree(plan/f'patches/{group}/after',audit/location,dirs_exist_ok=True)
 # Delivery code runs after field_binding in the real application. Reproduce
 # that cumulative input, including verify_plan_binding, in its host fixture.
 shutil.copytree(plan/'patches/field_binding/after',audit/'delivery_patch/after',dirs_exist_ok=True)
 for location in ('research_patch/verification','charts_patch/verification','rolling_patch','runtime_patch'):(audit/location).mkdir(parents=True,exist_ok=True)
 records=[]
 def run(name,argv,timeout=180):
  begin=time.monotonic();r=subprocess.run(list(map(str,argv)),cwd=work,capture_output=True,text=True,timeout=timeout)
  log=out/(name+'.log');log.write_text(r.stdout+'\n'+r.stderr)
  row={'id':name,'argv':list(map(str,argv)),'exit_code':r.returncode,'seconds':round(time.monotonic()-begin,3),
       'log':log.name,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest(),'scope':'HOST_CONTRACT_ONLY'}
  records.append(row);print(name+':'+('PASS' if r.returncode==0 else 'FAIL'),flush=True)
  if r.returncode:raise RuntimeError(name+' failed; inspect '+str(log))
 def java(name,sources,main_class,extra=()):
  classes=out/(name+'-classes');classes.mkdir()
  run(name+'-compile',['java','com.sun.tools.javac.Main','-source','17','-target','17','-d',classes,*sources])
  run(name,['java','-cp',classes,main_class,*extra]);return classes
 try:
  run('runtime',[sys.executable,'-B',audit/'runtime_patch/tests/run_runtime_policy_checks.py','--baseline',base])
  pure=plan/'patches/prepared_storage/after'
  java('prepared-storage',[pure/'app/src/main/java/com/ethk3/novaonepro/PreparedCanonicalAdmissionR343.java',pure/'verification/r343/PreparedCanonicalAdmissionR343Test.java'],'com.ethk3.novaonepro.PreparedCanonicalAdmissionR343Test')
  pure=plan/'patches/coverage/after'
  classes=java('coverage-java',[pure/'app/src/main/java/com/ethk3/novaonepro/CoverageSettlementR343.java',pure/'verification/r343/CoverageSettlementR343Test.java'],'com.ethk3.novaonepro.CoverageSettlementR343Test')
  run('coverage-sql',[sys.executable,'-B',pure/'verification/r343/check_coverage_sql.py','--classes',classes])
  policy=audit/'research_patch/after/app/src/main/java/com/ethk3/novaonepro/r952/TrainingInfrastructurePolicyR343.java'
  java('training-classification',[policy,audit/'research_patch/tests/TrainingInfrastructurePolicyR343Test.java',base/'app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java'],'com.ethk3.novaonepro.r952.TrainingInfrastructurePolicyR343Test')
  run('training-recovery-sql',[sys.executable,'-B',audit/'research_patch/tests/test_recovery_sql.py'])
  run('rolling-learner-sql',[sys.executable,'-B',audit/'research_patch/tests/test_rolling_sql.py'])
  run('rolling-publication-sql',[sys.executable,'-B',audit/'rolling_patch/tests/test_frozen_lineage_sql.py'])
  run('rolling-bound-sql',[sys.executable,'-B',audit/'research_patch/tests/test_bound_sql_r343.py'])
  run('rolling-waits-cleanup',[sys.executable,'-B',audit/'rolling_patch/tests/test_rolling_waits.py','--after',plan/'patches/rolling/after','--source',base,'--before',plan/'host_validation/rolling_patch/review_before','--out',out/'rolling-waits-cleanup'])
  run('rolling-six-kernels',[sys.executable,'-B',audit/'research_patch/tests/run_rolling_kernel.py'],timeout=300)
  java('chart-budget',[audit/'charts_patch/after/app/src/main/java/com/ethk3/novaonepro/strategyv2/ReportInlineBudgetR343.java',audit/'charts_patch/verification/src/com/ethk3/novaonepro/strategyv2/BudgetMain.java'],'com.ethk3.novaonepro.strategyv2.BudgetMain')
  run('chart-receipts',[sys.executable,'-B',audit/'charts_patch/verification/run_host_receipts.py'])
  run('delivery-gates',[sys.executable,'-B',audit/'delivery_patch/after/tools/r343/test_field_delivery_contract.py'])
  run('creation-classification',[sys.executable,'-B',audit/'creation_patch/tests/test_creation_classifier.py'])
  run('four-file-version-recovery',[sys.executable,'-B',audit/'field_binding_patch/tests/test_prepare_transaction.py'])
  semantic_test=plan/'patches/semantic_support/after/tools/r343/test_rebind_semantic_inputs.py'
  run('semantic-identities',[sys.executable,'-B',semantic_test,'--source-repo',repo,'--plan',plan,'--out',out/'semantic-identities.json'],timeout=600)
  run('semantic-cli',[sys.executable,'-B',semantic_test,'--source-repo',repo,'--plan',plan,'--cli-only','--out',out/'semantic-cli.json'],timeout=600)
  # A full AST parse detects Java syntax errors across the cumulative after tree.
  merged=work/'merged_after';merged.mkdir()
  for group in json.loads((plan/'PATCHSET.json').read_text())['groups']:
   shutil.copytree(plan/group['full_source_directory'],merged,dirs_exist_ok=True)
  java('all-java-syntax',[audit/'runtime_patch/tests/ParseChangedJava.java'],'ParseChangedJava',[merged])
  result={'status':'HOST_CHECKS_PASSED','source_baseline':BASE,'checks':records,'android_build':'NOT_RUN','device':'NOT_RUN','natural_quality':'NOT_PROVEN'}
 except Exception as exc:
  result={'status':'HOST_CHECK_FAILED','error':str(exc),'checks':records,'android_build':'NOT_RUN','device':'NOT_RUN','natural_quality':'NOT_PROVEN'}
 (out/'RESULT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k!='checks'},ensure_ascii=False))
 if result['status']!='HOST_CHECKS_PASSED':sys.exit(1)

if __name__=='__main__':main()
