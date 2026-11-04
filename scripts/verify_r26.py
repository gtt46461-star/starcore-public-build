#!/usr/bin/env python3
"""Independent R26 protocol/math regression and real additive SQLite DDL checks."""
from pathlib import Path
import hashlib,json,re,sqlite3,subprocess,time,shutil,tempfile
from host_product_classpath import product_classpath
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/r26';OUT.mkdir(parents=True,exist_ok=True)

def java_string_constant(source,name):
    literal=re.search(r'public static final String\s+'+re.escape(name)+r'\s*=\s*("(?:[^"\\]|\\.)*")\s*;',source)
    if literal is None:raise ValueError('R26_SQL_FIXTURE_CONSTANT_MISSING:'+name)
    return json.loads(literal[1])

def java_string_array(source,name,constants):
    """Read the named owner array, including explicit constant members; reject unsupported syntax."""
    array=re.search(r'public static final String\[\]\s+'+re.escape(name)+r'\s*=\s*\{(.*?)^    \};',source,re.S|re.M)
    if array is None:raise ValueError('R26_SQL_FIXTURE_ARRAY_MISSING:'+name)
    statements=[]
    for line in array[1].splitlines():
        member=line.strip().removesuffix(',')
        if not member or member.startswith('//'):continue
        if member in constants:statements.append(constants[member])
        elif re.fullmatch(r'"(?:[^"\\]|\\.)*"',member):statements.append(json.loads(member))
        else:raise ValueError('R26_SQL_FIXTURE_UNSUPPORTED_MEMBER:'+name+':'+member)
    if not statements:raise ValueError('R26_SQL_FIXTURE_EMPTY_ARRAY:'+name)
    return statements

def runtime_schema_ddls(source):
    feedback=java_string_constant(source,'FEEDBACK')
    # RuntimeSchemaR26.ensure executes base DDL before SchemaDomain's feedback migration.
    # Declaration order differs: the feedback extension appears first in the Java source.
    base=java_string_array(source,'DDL',{'FEEDBACK':feedback})
    extension=java_string_array(source,'R31_FEEDBACK_DDL',{'FEEDBACK':feedback})
    if base[0]!=feedback:raise ValueError('R26_SQL_FIXTURE_BASE_FEEDBACK_ORDER')
    return feedback,base+extension

def check_feedback_extension(db,source):
    """Execute actual trigger/immutability SQL on synthetic rows, with no model-quality claim."""
    protocol=java_string_constant(source,'R31_MODEL_CONSUMER_PROTOCOL')
    def event(key,kind,issue,source_hash,legacy):
        db.execute('INSERT INTO r31_feedback_event VALUES(?,?,?,?,?,?,?,?,?,?,?)',
                   (key,kind,issue,1,source_hash,'',legacy,'payload',hashlib.sha256(b'payload').hexdigest(),'HOST_SQL_FIXTURE',1))
    event('typed-old','DRAW_LABEL_REVEALED','host-old','source','old')
    event('typed-other-source','DRAW_LABEL_REVEALED','host-other','different-source','old')
    event('typed-frozen','FROZEN_RESULT','host-old','source','old')
    for policy,through in [('model',1),('before-label',0),('rollback-model',1)]:
        db.execute('INSERT INTO r26_checkpoint_artifact VALUES(?,?,?,?,?,?,?)',
                   ('ck-'+policy,'artifact-'+policy,policy,'source','',through,7))
    db.execute("INSERT INTO r26_consumption VALUES('old','model','ck-model','EXPOSED_HISTORY')")
    expected=('model|'+protocol,'typed-old','source','ck-model','artifact-model','model','EXPOSED_HISTORY',7)
    assert db.execute('SELECT * FROM r31_feedback_receipt').fetchall()==[expected]
    db.execute("INSERT OR IGNORE INTO r26_consumption VALUES('old','model','ck-model','EXPOSED_HISTORY')")
    assert db.execute('SELECT count(*) FROM r31_feedback_receipt').fetchone()[0]==1
    db.execute("INSERT INTO r26_consumption VALUES('old','before-label','ck-before-label','EXPOSED_HISTORY')")
    db.execute("INSERT INTO r26_consumption VALUES('old','missing-artifact','absent','EXPOSED_HISTORY')")
    assert db.execute('SELECT * FROM r31_feedback_receipt').fetchall()==[expected]
    # A checkpoint receipt is part of its actual consumption transaction, not a separate count.
    db.execute('SAVEPOINT feedback_atomic')
    db.execute("INSERT INTO r26_consumption VALUES('old','rollback-model','ck-rollback-model','EXPOSED_HISTORY')")
    assert db.execute('SELECT count(*) FROM r31_feedback_receipt').fetchone()[0]==2
    db.execute('ROLLBACK TO feedback_atomic');db.execute('RELEASE feedback_atomic')
    assert db.execute("SELECT count(*) FROM r26_consumption WHERE policy_hash='rollback-model'").fetchone()[0]==0
    assert db.execute('SELECT * FROM r31_feedback_receipt').fetchall()==[expected]
    for sql,code in [
        ("UPDATE r31_feedback_event SET payload='changed' WHERE event_id='typed-old'",'R31_FEEDBACK_EVENT_IMMUTABLE'),
        ("INSERT OR REPLACE INTO r31_feedback_event SELECT * FROM r31_feedback_event WHERE event_id='typed-old'",'R31_FEEDBACK_EVENT_IMMUTABLE'),
        ("DELETE FROM r31_feedback_event WHERE event_id='typed-old'",'R31_FEEDBACK_EVENT_DELETE_REQUIRES_VERSIONED_RETENTION_PROTOCOL')]:
        try:db.execute(sql)
        except sqlite3.IntegrityError as rejected:assert code in str(rejected)
        else:raise AssertionError('mutable R31 feedback evidence:'+sql)
    assert db.execute("SELECT payload FROM r31_feedback_event WHERE event_id='typed-old'").fetchone()[0]=='payload'
    assert db.execute('SELECT count(*) FROM r31_feedback_event').fetchone()[0]==3
    # An additive repeat retains all original and newly typed evidence and exact trigger output.
    for ddl in runtime_schema_ddls(source)[1]:db.execute(ddl)
    assert db.execute('SELECT * FROM r31_feedback_receipt').fetchall()==[expected]
    assert db.execute("SELECT * FROM r26_feedback WHERE event_id='old'").fetchone()==('old',1,'source','payload',1)

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
    sources += [ROOT/'tools/strategy_su2/tests'/name for name in ['R26KernelTest.java','R26FamiliesTest.java','R26ExpertTest.java','R26RecoveryTest.java','R26EvidenceTest.java','R26FrozenSurfaceTest.java','R26ChunkedCheckpointTest.java','R26CapabilityServiceTest.java','R26ForecastSnapshotTest.java','R26FamilyMechanismTest.java','R26IncumbentBudgetTest.java','R26F08MechanismTest.java','R26F20MechanismTest.java','R26WarmSeedTest.java','R26TensorMechanismTest.java','R26FragmentDiagnosticsTest.java','R26MatchingDiagnosticsTest.java','R26NeuralDiagnosticsTest.java','R26StructuredDiagnosticsTest.java','R26SparseDiagnosticsTest.java','R26DurationDiagnosticsTest.java','R26StatisticalDiagnosticsTest.java','R26SuffixDiagnosticsTest.java','R26RetrievalDiagnosticsTest.java','R26RelationStatisticsTest.java','R26SymbolicDiagnosticsTest.java','R26ResidualDiagnosticsTest.java','R26DistillationDiagnosticsTest.java']]
    classes=OUT/'classes';classes.mkdir(exist_ok=True)
    run('compile',['java','com.sun.tools.javac.Main','-encoding','UTF-8','-cp',product_classpath(ROOT),'-d',classes]+sources)
    classes=product_classpath(ROOT,classes)
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
    structuredSamples=OUT/'structured-state-samples';structuredSamples.mkdir(exist_ok=True)
    run('structured_training_diagnostics',['java','-Xmx768m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26StructuredDiagnosticsTest',structuredSamples])
    run('sparse_training_diagnostics',['java','-Xmx768m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26SparseDiagnosticsTest',OUT/'f01-state-sample.bin'])
    run('duration_training_diagnostics',['java','-Xmx768m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26DurationDiagnosticsTest',OUT/'f09-state-sample.bin'])
    run('statistical_training_diagnostics',['java','-Xmx768m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26StatisticalDiagnosticsTest',OUT/'f08-state-sample.bin'])
    run('suffix_training_diagnostics',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26SuffixDiagnosticsTest',OUT/'f12-state-sample.bin'])
    run('retrieval_training_diagnostics',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26RetrievalDiagnosticsTest',OUT/'f11-state-sample.bin'])
    run('relation_training_statistics',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26RelationStatisticsTest',OUT/'f21-state-sample.bin'])
    symbolicSamples=OUT/'symbolic-state-samples';symbolicSamples.mkdir(exist_ok=True)
    run('symbolic_training_diagnostics',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26SymbolicDiagnosticsTest',symbolicSamples])
    run('residual_training_diagnostics',['java','-Xmx768m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26ResidualDiagnosticsTest',OUT/'f22-state-sample.bin'])
    distillationSamples=OUT/'distillation-state-samples';distillationSamples.mkdir(exist_ok=True)
    run('distillation_training_diagnostics',['java','-Xmx512m','-cp',classes,'com.ethk3.novaonepro.strategyv2.R26DistillationDiagnosticsTest',distillationSamples])
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
    source=(prod/'strategyv2/RuntimeSchemaR26.java').read_text();feedback,ddls=runtime_schema_ddls(source)
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
    check_feedback_extension(db,source)
    checks.append(dict(name='actual_sql_r31_feedback_after_base_immutable_exact_receipt_join',returncode=0))
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
