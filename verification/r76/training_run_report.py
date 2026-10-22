"""Real modern Owner recovery from the R76 report, across independent JVMs."""
from pathlib import Path
import argparse, hashlib, json, shutil, sqlite3, subprocess, sys
p=argparse.ArgumentParser()
for key in ('schema','report','classes','jdk','out'):p.add_argument('--'+key,type=Path,required=True)
p.add_argument('--overlay',type=Path,action='append',default=[])
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True);cl=o/'classes';cl.mkdir()
cp=':'.join(map(str,[cl,a.schema.resolve()/'classes',*[x.resolve() for x in a.overlay],a.classes.resolve(),(a.schema/'host-classpath').read_text().strip().split(':')[-1]]))
commands=[]
def run(command,log):
    command=list(map(str,command));commands.append(command)
    with (o/log).open('w') as f:subprocess.run(command,check=True,stdout=f,stderr=subprocess.STDOUT)
run([sys.executable,r/'verification/r76/training_prepare_report.py','--schema',a.schema,'--report',a.report,'--out',o/'fixture'],'fixture.log')
run([a.jdk/'bin/javac','-cp',cp,'-d',cl,r/'verification/r76/training_ReportOwnerR76.java',r/'verification/r72/realtime/android/os/SystemClock.java'],'compile.log')
db=o/'fixture/report.db';base=[a.jdk/'bin/java','-Xmx512m','-Djava.library.path='+str(r.parent/'host-native'),'-cp',cp,'com.ethk3.novaonepro.training_ReportOwnerR76',r/'verification/canonical/schema/sqlite_bridge.py',db,o/'app',r/'app/src/main/assets']
run([*base,o/'cold',20,'training'],'cold.log')
c=sqlite3.connect(db);prefixes=[]
for row in json.loads((o/'fixture/reported-feature-cursors.json').read_text()):
    h=hashlib.sha256(('CP12_FEATURE_PREFIX|'+row['task_id']+'|'+row['protocol_hash']+'|'+row['input_hash']).encode()).hexdigest()
    features=c.execute('SELECT row_index,feature_hash FROM research_training_feature_row_cp11 WHERE task_id=? AND row_index<? ORDER BY row_index',[row['task_id'],int(row['next_index'])]).fetchall()
    for i,(index,f) in enumerate(features):
        assert i==index;h=hashlib.sha256((h+'|'+f).encode()).hexdigest()
    assert len(features)==int(row['next_index']) and h==row['prefix_hash']
    prefixes.append({'task_id':row['task_id'],'next_index':len(features),'prefix_hash':h,'status':'EXACT_REPORT_MATCH'})
c.close();run([*base,o/'resumed',400,'training'],'resumed.log')
def state():
    with sqlite3.connect(db) as c:
        c.row_factory=sqlite3.Row
        models=[dict(x) for x in c.execute('SELECT p.candidate_id,p.model_kind,p.validation_last,a.artifact_hash,a.numeric_updates,a.development_sample_n,a.validation_sample_n,a.validation_pass FROM research_training_protocol_v2 p JOIN r952_artifact a ON a.candidate_id=p.candidate_id')]
        frozen=[dict(x) for x in c.execute('SELECT f.candidate_id,f.learner_id,f.model_hash,f.trained_until_issue,f.created_issue,f.source_artifact,f.run_hash,f.rule_hash FROM r967_frozen_evaluation_v2 f JOIN research_training_protocol_v2 p ON p.candidate_id=f.learner_id')]
        task_states=[dict(x) for x in c.execute("SELECT kind,status,COUNT(*) AS n FROM v57_research_task_r967 WHERE kind IN ('TRAIN_CANDIDATE','FREEZE_CANDIDATE') GROUP BY kind,status")]
        assert len(models)==6 and len(frozen)==6
        assert all(x['development_sample_n']==1200 and x['validation_sample_n']==120 for x in models)
        assert len(task_states)==2 and all(x['status']=='COMPLETED' and x['n']==6 for x in task_states)
        assert c.execute('SELECT COUNT(*) FROM research_training_feature_row_cp11').fetchone()[0]==7920
        assert c.execute('SELECT COUNT(*) FROM research_development_receipt_v2').fetchone()[0]==6
        return {'models':models,'frozen':frozen,'task_states':task_states}
completed=state();run([*base,o/'restarted',1,'training'],'restarted.log');assert state()==completed
summary={'status':'PASS','scope':'CONTROLLED_REPORT_ORIGINAL_CAS_AND_PREFIX_ACTUAL_OWNERS_NOT_PHONE_LATENCY_OR_FUTURE_QUALITY','product_classes':str(a.classes.resolve()),'explicit_product_overlays':list(map(str,a.overlay)),'commands':commands,'original_input_identities':json.loads((o/'cold-inputs.json').read_text()),'original_feature_prefixes':prefixes,'feature_rows':7920,'development_receipts':6,'restart_duplicate_models':0,**completed}
(o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps({'status':'PASS','feature_rows':7920,'receipts':6,'frozen':6,'original_input_hashes':6,'original_prefixes':2},indent=2))
