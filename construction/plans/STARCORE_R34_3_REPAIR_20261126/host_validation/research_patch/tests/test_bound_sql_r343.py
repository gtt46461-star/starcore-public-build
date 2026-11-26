#!/usr/bin/env python3
"""Execute actual bound SQL-builder methods, then prepare both SQL statements in SQLite."""
from pathlib import Path
import importlib.util,subprocess,tempfile,json,base64,sqlite3,hashlib
root=Path(__file__).resolve().parents[3];base=root/'source-installed/app/src/main/java/com/ethk3/novaonepro';after=root/'audit/research_patch/rolling_candidate/app/src/main/java/com/ethk3/novaonepro';pub=root/'audit/rolling_after/app/src/main/java/com/ethk3/novaonepro'
spec=importlib.util.spec_from_file_location('sqlh',root/'audit/rolling_patch/tests/test_frozen_lineage_sql.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);method=mod.method
text=(after/'r952/BoundAtomLifecycleR80.java').read_text();select=method(text,'static String parentSelectionSqlR81(');reason=method(text,'private static String parentReasonR80(');predicate=method((pub/'ResearchEvaluationRunR967.java').read_text(),'public static String frozenEligibilitySqlR343(').replace('com.ethk3.novaonepro.r952.RollingFrozenPublisherR343','RollingFrozenPublisherR343');cont=method((base/'TrainingContinuationR973.java').read_text(),'public static String predicate(')
code='''import java.util.*;import java.nio.charset.StandardCharsets;
public final class BoundSqlHarness {
 static final String PARENT="research_bound_parent_r80";static final int MAX_PARENTS=2;
 static void out(String s,int n){System.out.println(n+"|"+Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)));}
 static class Cursor implements AutoCloseable {public boolean moveToNext(){return false;}public void close(){}}
 static class SQLiteDatabase {Cursor rawQuery(String s,String[]a){out(s,a.length);return new Cursor();}}
 static class TargetIssueResolver {static boolean isValid(String s){return s!=null&&s.matches("[0-9]{12}");}}
 static class TrainingCodeIdentityR964 {static final String SHA256="__CURRENT_HASH__";}
 static class TrainingContinuationR973 {static final String[] PREDECESSORS={"__PRIOR_HASH__"}; __CONTINUATION_BODY__}
 static class RollingFrozenPublisherR343 {static final String TABLE="research_rolling_frozen_publication_r343";static boolean installed(SQLiteDatabase db){return true;}}
 static class ResearchEvaluationRunR967 {static final String PREFIX="R958-F-V3-",RUN="r967_numeric_training_run",EVAL="r967_frozen_evaluation_v2",SPLIT="EVERY_ISSUE_SEPARATE_FROZEN_EVALUATION_V3";__PREDICATE_BODY__}
 static class ResearchLifecycleR932 {static final String TABLE="research_lifecycle_r932";}
 static class FixedResearchWindowR932 {static final String WINDOW="research_fixed_window_r932";}
 static class FixedWindowCertificatePublisherR932 {static final String TABLE="research_window_publication_r932";}
 static class CandidateCalibrationR932 {static final String STUDY="research_candidate_cal_study_r932",QUALITY_CP12="research_calibration_quality_cp12",QUALITY_POLICY_HASH_CP12="__POLICY_HASH__";}
 __SELECTION_BODY__
 __REASON_BODY__
 public static void main(String[]a){SQLiteDatabase db=new SQLiteDatabase();out(parentSelectionSqlR81(db,"202611260100"),4);parentReasonR80(db,"cohort","202611260100");}
}
'''.replace('__CURRENT_HASH__','a'*64).replace('__PRIOR_HASH__','b'*64).replace('__POLICY_HASH__','c'*64).replace('__CONTINUATION_BODY__',cont).replace('__PREDICATE_BODY__',predicate).replace('__SELECTION_BODY__',select).replace('__REASON_BODY__',reason)
with tempfile.TemporaryDirectory(prefix='bound-r343-')as temp:
 p=Path(temp)/'BoundSqlHarness.java';p.write_text(code);r=subprocess.run(['java','com.sun.tools.javac.Main','-d',temp,str(p)],capture_output=True,text=True)
 if r.returncode:print(r.stderr);raise SystemExit(r.returncode)
 output=subprocess.check_output(['java','-cp',temp,'BoundSqlHarness'],text=True)
rows=[(int(l.split('|',1)[0]),base64.b64decode(l.split('|',1)[1]).decode())for l in output.splitlines()]
db=sqlite3.connect(':memory:')
# Synthetic metadata shapes only; executable SQL comes from the actual builders.
tables={
'r967_frozen_evaluation_v2':'candidate_id,learner_id,created_at,created_issue,trained_until_issue,ast,model_payload,run_hash,integrity_epoch,rule_hash,structural_hash,model_hash,source_artifact',
'r967_numeric_training_run':'run_hash,learner_id,rule_hash,integrity_epoch,first_issue,ast_hash',
'research_rolling_frozen_publication_r343':'run_hash,learner_id,frozen_id,first_issue,model_hash,source_artifact,split_rule,rule_hash,integrity_epoch,ast_hash',
'r952_candidate':'candidate_id,ast,structural_hash,state',
'research_bound_parent_r80':'candidate_id,freeze_hash,model_hash,learner_id,learner_ast_hash,cohort_hash',
'research_lifecycle_r932':'candidate_key,state','research_capture_reservation_v2':'candidate_key,state',
'research_fixed_window_r932':'candidate_key,state','research_window_publication_r932':'candidate_key,certificate_hash',
'research_candidate_cal_study_r932':'candidate_key,state,study_hash','research_calibration_quality_cp12':'study_hash,state,policy_hash'}
for name,columns in tables.items():db.execute('CREATE TABLE '+name+'('+','.join(c+' TEXT'for c in columns.split(','))+')')
for count,sql in rows:
 assert sql.count('?')==count,(sql.count('?'),count)
 db.execute('EXPLAIN QUERY PLAN '+sql,['fixture']*count).fetchall()
 assert 'MAX(rr.first_issue)' not in sql
 assert 'research_rolling_frozen_publication_r343' in sql
 assert "q.state='FITTED_BUT_NOT_QUALIFIED'" in sql
 assert 'CALIBRATION_QUALIFIED' in sql and 'RESERVED' in sql and 'OPEN' in sql
assert rows[0][0]==4 and rows[1][0]==3
result={'status':'PASS','actual_source_builders':2,'sql_bind_counts':[n for n,_ in rows],'scope':'actual parent selection/reason Java SQL methods compiled and prepared in synthetic SQLite; eligibility predicate separately exercised by publisher fixture; not Android','source_sha256':hashlib.sha256((after/'r952/BoundAtomLifecycleR80.java').read_bytes()).hexdigest()}
(root/'audit/research_patch/verification/bound_sql_result.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
