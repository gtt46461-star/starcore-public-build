#!/usr/bin/env python3
"""Compile exact production sources with the local JDK, only into isolated /tmp.

No Gradle, Android device, production DB, owner claim, writer, or tools mutation.
Expected counterexample failures remain exit=1 with their original stack traces;
the orchestration receipt distinguishes them from numerical recovery passes.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,os,re,shutil,subprocess,tempfile,time


def digest(data):
    return hashlib.sha256(data).hexdigest()


def method(source, marker):
    start=source.index(marker)
    opening=source.index('{',start)
    depth=1
    end=opening+1
    while depth:
        depth+=(source[end]=='{')-(source[end]=='}')
        end+=1
    return {'marker':marker,'start_line':source[:start].count('\n')+1,
            'end_line':source[:end].count('\n')+1,'sha256_utf8':digest(source[start:end].encode()),
            'exact_source':source[start:end]}


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[4])
    parser.add_argument('--out',type=Path)
    args=parser.parse_args()
    root=args.root.resolve()
    stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    output=(args.out or Path(__file__).resolve().parent/'runs'/stamp).resolve()
    allowed=root/'verification/r31/p03_preparation'
    if not output.is_relative_to(allowed):
        raise RuntimeError('Evidence output must be inside verification/r31/p03_preparation')
    output.mkdir(parents=True,exist_ok=False)
    temporary=Path(tempfile.mkdtemp(prefix='r31-p03-publication-repro-'))
    snapshot=temporary/'source';classes=temporary/'classes';dependencies=temporary/'dependencies'
    fixtures=temporary/'fixtures'
    for p in [snapshot,classes,fixtures]:p.mkdir()
    appclasses=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
    if not appclasses.is_dir():raise RuntimeError('Missing existing app classes for unrelated runtime linkage; do not run Gradle from this harness')
    shutil.copytree(appclasses,dependencies)
    sdk=Path(os.environ.get('ANDROID_HOME','/workspace/starcore-environment/android-sdk'))/'platforms/android-36/android.jar'
    if not sdk.is_file():raise RuntimeError('Missing Android SDK linkage jar')
    prefix='app/src/main/java/com/ethk3/novaonepro/strategyv2/'
    names=['AtomNetSessionR30','AtomNetTrainerR27','AtomNetR27','ModelArtifactR29','FtrlR27','StatsR27',
           'ModelSurfaceProviderR30','NumberRankerAdapterR30','PitGuardR30','HashingSU1','JsonR27',
           'ResearchContractR30R1','LogisticRidgeSU2','SplitR27','ReplayDatasetR27','RuntimeR27',
           'ReplayEvaluatorR27','MemoryBudgetR31','FeatureCacheR27','SharedFeatureStoreR31']
    paths=[prefix+name+'.java' for name in names]
    paths+=['app/src/main/java/com/ethk3/novaonepro/'+name+'.java'
            for name in ['StorageLedgerR31','StorageBudgetR31','StorageCensusSU2','ReportNativeCP4','DirectoryCursorR31']]
    current={};sources=[]
    for relative in paths:
        data=(root/relative).read_bytes();target=snapshot/relative;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
        current[relative]={'sha256':digest(data),'bytes':len(data)};sources.append(target)
    harness=Path(__file__).resolve().parent/'PublicationCounterexampleR31.java'
    hcopy=snapshot/harness.name;shutil.copyfile(harness,hcopy);sources.append(hcopy)
    evidence_snapshot=output/'source_snapshot';evidence_snapshot.mkdir()
    shutil.copyfile(Path(__file__).resolve(),evidence_snapshot/'run_counterexamples.py')
    for source in sources:
        target=evidence_snapshot/source.relative_to(snapshot);target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
    target_markers={
        prefix+'AtomNetSessionR30.java':['public int work(Samples samples)','public void online(','public String trainedThrough()','public long trainedThroughOrdinal()','public boolean ready()','public Map<String,Object> report()','public byte[] encode()','public static AtomNetSessionR30 decode('],
        prefix+'RuntimeR27.java':['void publishAtomStateR30('],
        prefix+'ModelSurfaceProviderR30.java':['private FrozenModelRef(','public static Surface predict('],
    }
    method_id={relative:[method((snapshot/relative).read_text(),marker) for marker in markers]
               for relative,markers in target_markers.items()}
    receipt={'schema':'R31_P03_EXECUTED_COUNTEREXAMPLE_1','started_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
             'planning_baseline':'5237b265a27fe5ad27fd842901c445d5f8c27749',
             'head_at_capture':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),
             'worktree_status_at_capture':subprocess.check_output(['git','status','--short'],cwd=root,text=True).splitlines(),
             'source_snapshot':current,'method_identity':method_id,'harness_sha256':digest(harness.read_bytes()),
             'runner_sha256':digest(Path(__file__).read_bytes()),'java':shutil.which('java'),
             'temporary_workspace':str(temporary),'sdk_linkage_jar':str(sdk),'sdk_sha256':digest(sdk.read_bytes()),
             'existing_unrelated_dependency_classes':str(appclasses),
             'execution_scope':'FRESH_EXACT_SOURCE_NUMERIC_CODEC_RUNTIME_PUBLICATION_AND_SURFACE_METHODS_ON_HOST_JDK',
             'limitations':['Unsafe bypasses the Android runtime constructor; owner scheduling/fence/writer/SQLite are not executed.',
                            'Checkpoint files are isolated synthetic host fixtures, not production DB commits or restored original device model bytes.',
                            'Actual child JVM halt/restart proves file codec numerical recovery only; not Android process lifecycle or DB atomicity.',
                            'ModelSurfaceProvider.predict executes on the partial published ref; no live database call/freeze receipt is claimed.',
                            'Unrelated runtime linkage uses copied existing P01 debug classes; no integrated P02/P03 build or device acceptance is claimed.'],
             'gradle_run':False,'production_or_tools_written':False,'device_run':False,'commands':[],'tests':[]}

    def run(name,argv,expected=0,timeout=120):
        begun=time.monotonic()
        result=subprocess.run([str(x) for x in argv],cwd=temporary,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=timeout)
        log=output/(name+'.log');log.write_text(result.stdout)
        record={'name':name,'argv':[str(x) for x in argv],'exit_code':result.returncode,'expected_exit_code':expected,
                'wall_seconds':time.monotonic()-begun,'log':log.name,'log_sha256':digest(log.read_bytes()),
                'exit_matches_expected':result.returncode==expected}
        receipt['commands'].append(record)
        (output/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
        print(json.dumps({'name':name,'exit_code':result.returncode,'expected':expected,'log':str(log)}),flush=True)
        if result.returncode!=expected:raise RuntimeError('Unexpected result: '+name+'; see '+str(log))
        return record,result.stdout

    try:
        run('java-version',['java','-version'])
        cp=os.pathsep.join(str(p) for p in [dependencies,sdk])
        compiler=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-g','-sourcepath',str(snapshot/'EMPTY_SOURCEPATH'),'-cp',cp,'-d',classes,*sources]
        run('compile-exact-production-and-harness',compiler)
        javacp=os.pathsep.join(str(p) for p in [classes,dependencies,sdk])
        entry='com.ethk3.novaonepro.strategyv2.PublicationCounterexampleR31'
        def scenario(name,mode,position=None,expected=0,verdict=None,signature=None):
            args=['java','-Xmx768m','-Xlog:class+load=info:file='+str(temporary/(name+'.classload.log')),'-cp',javacp,entry,mode,fixtures]
            if position is not None:args.append(str(position))
            record,log=run(name,args,expected)
            if signature is not None and signature not in log:raise RuntimeError('Expected original assertion signature absent: '+name)
            observations=[json.loads(line[len('OBSERVATION '):]) for line in log.splitlines() if line.startswith('OBSERVATION ')]
            receipt['tests'].append({'name':name,'position':position,'verdict':verdict,'exit_code':record['exit_code'],'observations':observations})
        scenario('prepare','prepare',verdict='HOST_FIXTURE_PREPARED')
        scenario('pretrain-boundary-resume','pretrain-boundary-resume',verdict='HOST_PRETRAIN_CODEC_RESUME_PASS_NOT_PROCESS_KILL')
        scenario('draft-isolation','draft-isolation',verdict='HOST_DRAFT_ISOLATION_PASS_NOT_DATABASE_TEST')
        for position in range(1,10):
            scenario('halt-position-'+str(position),'checkpoint-halt',position,83,'ACTUAL_JVM_HALT_AT_CHECKPOINT')
            scenario('restore-position-'+str(position),'restore',position,0,'HOST_NUMERICAL_RECOVERY_PASS')
            scenario('partial-publication-position-'+str(position),'assert-no-partial-publication',position,1,
                     'BASELINE_FAIL_REPRODUCED','R31_PARTIAL_PUBLICATION_VIOLATION')
        scenario('restart-publication-identity','assert-stable-restart-identity',expected=1,
                 verdict='BASELINE_FAIL_REPRODUCED',signature='R31_RESTART_PUBLICATION_IDENTITY_VIOLATION')
        receipt['isolated_fixture_files']=[{'name':p.name,'sha256':digest(p.read_bytes()),'bytes':p.stat().st_size} for p in sorted(fixtures.iterdir())]
        loaded={}
        for log in temporary.glob('*.classload.log'):
            for line in log.read_text().splitlines():
                match=re.search(r'\] (com\.ethk3\.novaonepro\.[^ ]+) source: (.+)$',line)
                if match:
                    name,origin=match.groups();file=Path(origin.removeprefix('file:'))/Path(*name.split('.')).with_suffix('.class')
                    if file.is_file():loaded[name]={'origin':origin,'sha256':digest(file.read_bytes())}
        receipt['loaded_application_class_identities']=loaded
        receipt['source_changed_after_snapshot']=[relative for relative,identity in current.items() if digest((root/relative).read_bytes())!=identity['sha256']]
        receipt['completed_utc']=datetime.datetime.now(datetime.timezone.utc).isoformat()
        receipt['verdict']='COUNTEREXAMPLES_REPRODUCED_AND_NUMERICAL_RECOVERY_EXECUTED_NOT_PRODUCT_PASS'
        pretrain=next(test for test in receipt['tests'] if test['name']=='pretrain-boundary-resume')
        receipt['summary']={'pretrain_work_boundaries_exact':pretrain['observations'][0]['boundaries'],'actual_jvm_halts':9,'exact_numerical_restores':9,'partial_publication_contract_baseline_failures':9,'restart_identity_contract_baseline_failures':1}
    except Exception as failure:
        receipt['verdict']='HARNESS_OR_EXECUTION_FAILURE_NOT_PRODUCT_PASS';receipt['failure']=str(failure)
        raise
    finally:
        receipt['finished_at_utc']=datetime.datetime.now(datetime.timezone.utc).isoformat()
        (output/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
        print('RECEIPT '+str(output/'receipt.json'),flush=True)


if __name__=='__main__':
    main()
