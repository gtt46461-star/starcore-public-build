#!/usr/bin/env python3
"""Independent R26 protocol/math regression and real additive SQLite DDL checks."""
from pathlib import Path
import hashlib,json,re,sqlite3,subprocess,time,shutil,tempfile
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/r26';OUT.mkdir(parents=True,exist_ok=True)

def main():
    checks=[]
    def run(name,argv,cwd=ROOT):
        start=time.monotonic();p=subprocess.run(list(map(str,argv)),cwd=cwd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        (OUT/(name+'.log')).write_text(p.stdout);checks.append(dict(name=name,returncode=p.returncode,seconds=round(time.monotonic()-start,3)));print(name,'PASS' if p.returncode==0 else 'FAIL',flush=True)
        if p.returncode:raise RuntimeError(p.stdout[-4000:])
        return p.stdout
    prod=ROOT/'app/src/main/java/com/ethk3/novaonepro'
    sources=[p for p in (prod/'strategyv2').glob('*.java') if not any(s in p.read_text() for s in ['import android.','import com.ethk3.novaonepro.DbStore;','import com.ethk3.novaonepro.*;'])]
    sources += [ROOT / 'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java']
    sources += [prod/'r952'/n for n in ['AtomEvaluatorCP4.java','AtomWindowCP4.java','MemoryBudgetCP4.java','MatrixMetricsCP11.java','StrictCanonicalJson.java']]
    sources += [ROOT/'tools/strategy_su2/tests'/name for name in ['R26KernelTest.java','R26FamiliesTest.java','R26ExpertTest.java','R26RecoveryTest.java','R26EvidenceTest.java','R26FrozenSurfaceTest.java','R26ChunkedCheckpointTest.java','R26CapabilityServiceTest.java','R26ForecastSnapshotTest.java','R26FamilyMechanismTest.java','R26IncumbentBudgetTest.java','R26F08MechanismTest.java','R26F20MechanismTest.java','R26WarmSeedTest.java','R26TensorMechanismTest.java','R26FragmentDiagnosticsTest.java','R26MatchingDiagnosticsTest.java','R26NeuralDiagnosticsTest.java']]
    classes=OUT/'classes';classes.mkdir(exist_ok=True)
    run('compile',['java','com.sun.tools.javac.Main','-encoding','UTF-8','-d',classes]+sources)
    log=run('kernels',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26KernelTest']);assert 'R26_KERNEL_PASS' in log
    run('experts',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26ExpertTest'])
    run('recovery',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26RecoveryTest'])
    run('chunked_checkpoint',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26ChunkedCheckpointTest'])
    run('capability_service',['java','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26CapabilityServiceTest'])
    run('forecast_snapshot',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26ForecastSnapshotTest'])
    run('hidden_state_mechanism',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26F08MechanismTest'])
    run('fragment_memory_mechanism',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26F20MechanismTest',OUT/'f20-state-sample.bin'])
    run('warm_model_seed',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26WarmSeedTest'])
    run('tensor_mechanism',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26TensorMechanismTest',OUT/'f19-tensor-state-sample.bin'])
    run('fragment_training_diagnostics',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26FragmentDiagnosticsTest'])
    run('matching_training_diagnostics',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26MatchingDiagnosticsTest',OUT/'f17-state-sample.bin'])
    neuralSamples=OUT/'neural-state-samples';neuralSamples.mkdir(exist_ok=True)
    run('neural_training_diagnostics',['java','-Xmx768m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26NeuralDiagnosticsTest',neuralSamples])
    run('incumbent_budget',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26IncumbentBudgetTest'])
    run('family_mechanisms',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26FamilyMechanismTest'])
    run('evidence',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26EvidenceTest'])
    run('frozen_surface',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26FrozenSurfaceTest'])
    plan=ROOT/'construction/plans/STARCORE_R26_MERGED_20261026'
    family_log=run('families',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26FamiliesTest']);assert 'R26_FAMILIES_PASS' in family_log
    expected=json.loads((plan/'evidence/identity_golden_vectors.json').read_text())['vectors']
    actual=[line.split('\t')[1:] for line in log.splitlines() if line.startswith('GOLDEN\t')]
    assert actual==[[x['canonical'],x['sha256']] for x in expected]
    checks.append(dict(name='eleven_cross_language_golden_bytes',returncode=0))
    source=(prod/'strategyv2/RuntimeSchemaR26.java').read_text();feedback=json.loads(re.search(r'FEEDBACK = ("[^\n]+")',source)[1])
    ddls=[feedback]+[json.loads(m) for m in re.findall(r'^        ("(?:[^"\\]|\\.)*")[,]?$',source,re.M)]
    db=sqlite3.connect(':memory:');db.execute(feedback);db.execute("INSERT INTO r26_feedback VALUES('old',1,'source','payload',1)")
    for _ in range(2):
        for sql in ddls:db.execute(sql)
    assert db.execute('SELECT count(*) FROM r26_feedback').fetchone()[0]==1
    db.execute("INSERT INTO r26_task(task_id,lane,mechanism) VALUES('train','P2','actual')")
    db.execute("UPDATE r26_task SET fence=fence+1,owner='new',lease_until=300,status='RUNNING' WHERE task_id='train'")
    assert db.execute("SELECT 1 FROM r26_task WHERE task_id='train' AND owner='old' AND fence=0 AND lease_until>200").fetchone() is None
    # A stale-parent sample CAS cannot silently consume a second sample.
    db.execute("INSERT INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at) VALUES('c','s',2,X'0000','RUNNING',1)")
    db.execute("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,operation) VALUES('c','p','spec','test')")
    assert db.execute("UPDATE r26_candidate SET cursor=1 WHERE campaign_id='c' AND policy_hash='p' AND cursor=0").rowcount==1
    assert db.execute("UPDATE r26_candidate SET cursor=2 WHERE campaign_id='c' AND policy_hash='p' AND cursor=0").rowcount==0
    db.commit()
    try:
        db.execute('BEGIN');db.execute("UPDATE r26_candidate SET cursor=2 WHERE campaign_id='c'");raise RuntimeError('injected')
    except RuntimeError:db.rollback()
    assert db.execute('SELECT cursor FROM r26_candidate').fetchone()[0]==1
    db.execute("INSERT INTO r26_frozen VALUES('p',10,9,'ck','state','source',1,100,101,zeroblob(400))")
    try:
        db.execute("UPDATE r26_frozen SET source_hash='changed' WHERE policy_hash='p'")
        raise AssertionError('mutable frozen evidence')
    except sqlite3.IntegrityError:pass
    try:
        db.execute("INSERT INTO r26_frozen VALUES('bad',10,10,'ck','state','source',1,100,101,zeroblob(400))")
        raise AssertionError('target leaked into frozen input')
    except sqlite3.IntegrityError:pass
    checks.append(dict(name='actual_sql_migration_fencing_cas_rollback_immutable_freeze',returncode=0))
    with tempfile.TemporaryDirectory(prefix='r26-reference-') as tmp:
        copied=Path(tmp)/'plan'
        shutil.copytree(plan,copied,ignore=shutil.ignore_patterns('archive','downloads','__pycache__','.work'))
        run('plan_reference_contracts',['python3','code/run_checks.py'],copied)
        shutil.copytree(copied/'evidence/host_run',OUT/'plan-reference',dirs_exist_ok=True)
        shutil.copy2(copied/'evidence/validation_summary.json',OUT/'plan-reference/validation_summary.json')
    result=dict(status='PASS',scope='Production pure Java math/identity and additive SQLite; reference contracts separately labelled. No physical-device or natural-quality inference.',checks=checks,source_sha256={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources})
    (OUT/'verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print('R26_PRODUCTION_PROTOCOLS_PASS')
if __name__=='__main__':main()
