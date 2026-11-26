#!/usr/bin/env python3
"""Compile the actual source SQL builders, run their DDL and predicates in SQLite.

Android APIs are adapters only in this harness: this is a SQL fixture regression,
not an APK or a device test. Source text is extracted, never a reimplementation
of the production predicate or CREATE statements.
"""
from pathlib import Path
import base64, hashlib, json, re, sqlite3, subprocess, tempfile

ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / 'source-installed/app/src/main/java/com/ethk3/novaonepro'
AFTER = ROOT / 'audit/rolling_after/app/src/main/java/com/ethk3/novaonepro'
PUB = AFTER / 'r952/RollingFrozenPublisherR343.java'
RULE = 'a'*64
OLDER = 'b'*64
TARGET = '202611260010'


def method(text, signature):
    start = text.index(signature)
    brace = text.index('{', start)
    # Count Java braces while ignoring literals/comments.
    level=1; at=brace+1; state='code'
    while level:
        c=text[at]; n=text[at+1:at+2]
        if state=='code':
            if c=='"': state='string'
            elif c=="'": state='char'
            elif c=='/' and n=='/': state='line'; at+=1
            elif c=='/' and n=='*': state='block'; at+=1
            elif c=='{': level+=1
            elif c=='}': level-=1
        elif state in ('string','char'):
            if c=='\\': at+=1
            elif c==('"' if state=='string' else "'"): state='code'
        elif state=='line' and c=='\n': state='code'
        elif state=='block' and c=='*' and n=='/': state='code'; at+=1
        at+=1
    return text[start:at]


def run():
    p=PUB.read_text(); r=(AFTER/'ResearchEvaluationRunR967.java').read_text()
    router=(BASE/'r952/MatrixSourceRouterCP12.java').read_text()
    enqueue=method(router,'public static void enqueueFreezeCP12(')
    accepted=set(re.findall(r'"([a-z]+)"',re.search(r'Arrays\.asList\((.*?)\)',enqueue).group(1)))
    provided=set(re.findall(r'components\.put\("([A-Za-z]+)"',p))
    assert accepted==provided,'publisher changed the exact FreezeMatrix component ABI'

    predicate=method(r,'public static String frozenEligibilitySqlR343(')
    schema=method(p,'public static void schema(')
    rschema=method(r,'public static void schema(')
    continuation=method((BASE/'TrainingContinuationR973.java').read_text(),'public static String predicate(')
    predicate=predicate.replace('com.ethk3.novaonepro.r952.RollingFrozenPublisherR343','RollingFrozenPublisherR343')
    java='''import java.util.*;import java.nio.charset.StandardCharsets;
public final class PublisherSqlHarness {
 static boolean installed=true;
 static void output(String k,String s){System.out.println(k+"\\t"+Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)));}
 static class SQLiteDatabase {boolean inTransaction(){return true;}void execSQL(String sql){output("DDL",sql);}}
 static class TransactionSchemaCacheR941 {static boolean ready(SQLiteDatabase d,String k){return false;}static void mark(SQLiteDatabase d,String k){}}
 static class TargetIssueResolver {static boolean isValid(String s){return s!=null&&s.matches("[0-9]{12}")&&Integer.parseInt(s.substring(8))>=1&&Integer.parseInt(s.substring(8))<=1440;}}
 static class TrainingCodeIdentityR964 {static final String SHA256="RULE";}
 static class TrainingContinuationR973 {static final String[] PREDECESSORS={"OLDER"}; CONTINUATION }
 static class RollingLearnerR343 {static final String VERSION="research_rolling_version_r343";}
 static class RollingFrozenPublisherR343 {
  static final String TABLE="research_rolling_frozen_publication_r343",REJECT="research_rolling_publication_reject_r343",PAGE="research_rolling_publication_page_r343";
  static boolean installed(SQLiteDatabase d){return PublisherSqlHarness.installed;}
  static void need(boolean x,String code){if(!x)throw new IllegalStateException(code);}
  PUBSCHEMA
 }
 static class ResearchEvaluationRunR967 {
  static final String RUN="r967_numeric_training_run",INPUT="r967_numeric_training_input",EVAL="r967_frozen_evaluation_v2",ALL="r967_frozen_candidate_all",SPLIT="EVERY_ISSUE_SEPARATE_FROZEN_EVALUATION_V3";
  RUNSCHEMA
  PREDICATE
 }
 public static void main(String[] args){SQLiteDatabase db=new SQLiteDatabase();if(args[0].equals("schema")){ResearchEvaluationRunR967.schema(db);RollingFrozenPublisherR343.schema(db);}else{installed=Boolean.parseBoolean(args[1]);output("SQL",ResearchEvaluationRunR967.frozenEligibilitySqlR343(db,args[2],args[3]));}}
}
'''.replace('RULE',RULE).replace('OLDER',OLDER).replace('CONTINUATION',continuation).replace('PUBSCHEMA',schema).replace('RUNSCHEMA',rschema).replace('PREDICATE',predicate)
    with tempfile.TemporaryDirectory(prefix='r343-publisher-sql-') as temp:
        f=Path(temp)/'PublisherSqlHarness.java'; f.write_text(java)
        subprocess.run(['java','com.sun.tools.javac.Main','-d',temp,str(f)],check=True)
        def execute(*args):
            out=subprocess.check_output(['java','-cp',temp,'PublisherSqlHarness',*args],text=True)
            return [base64.b64decode(line.split('\t',1)[1]).decode() for line in out.splitlines()]
        ddls=execute('schema')
        full=execute('predicate','true','f',TARGET)[0]
        old_only=execute('predicate','false','f',TARGET)[0]
        for alias,target in [('f; DROP TABLE x',TARGET),('f',"202611260010' OR 1=1")]:
            rejected=subprocess.run(['java','-cp',temp,'PublisherSqlHarness','predicate','true',alias,target],text=True,capture_output=True)
            assert rejected.returncode!=0 and 'R343_FROZEN_SQL_INPUT' in rejected.stderr
    db=sqlite3.connect(':memory:');db.execute('PRAGMA foreign_keys=ON')
    db.execute('CREATE TABLE r952_candidate(candidate_id TEXT PRIMARY KEY,ast TEXT,structural_hash TEXT,state TEXT)')
    db.execute('CREATE TABLE r958_frozen_candidate(candidate_id,learner_id,ast,model_payload,structural_hash,model_hash,trained_until_issue,created_issue,source_artifact,created_at)')
    db.execute('CREATE TABLE research_rolling_version_r343(version_id TEXT PRIMARY KEY)')
    for ddl in ddls: db.execute(ddl)
    db.execute("INSERT INTO r952_candidate VALUES('base','AST3:original','astHash','ACTIVE')")
    split='EVERY_ISSUE_SEPARATE_FROZEN_EVALUATION_V3'
    def add_run(hash,first,rule=RULE):
        db.execute('INSERT INTO r967_numeric_training_run VALUES(?,?,?,?,?,?,?,?,?,?,?)',(hash,'base',first,'astHash',rule,'initial',300,'artifact-'+hash,split,'epoch-'+hash,1))
    def add_eval(id,run,first,rule=RULE,epoch=None,trained='2026112501440',ast='AST3:original',model=None):
        model=model or 'model-'+id
        row=(id,'base',ast,'payload-'+id,'structure-'+id,model,trained,first,'source-'+id,2,run,rule,epoch or 'epoch-'+run,120*db.execute('SELECT COUNT(*) FROM r967_frozen_evaluation_v2 WHERE run_hash=?',(run,)).fetchone()[0])
        db.execute('INSERT INTO r967_frozen_evaluation_v2 VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)',row)
        return row
    add_run('old-run','202611260001',OLDER)
    old=add_eval('old','old-run','202611260001',OLDER)
    before=db.execute('SELECT * FROM r967_frozen_evaluation_v2 WHERE candidate_id=?',('old',)).fetchone()
    add_run('new-base-run','202611260002')
    add_eval('new-base','new-base-run','202611260002')
    rolling=add_eval('rolling','independent-run','202611260003')
    db.execute("INSERT INTO research_rolling_version_r343 VALUES('v1')")
    pub=('v1','rolling','independent-run','base','202611260003','astHash',RULE,300,'source-rolling',split,'epoch-independent-run','model-rolling','normalizer','manifest','protocol','input','training-receipt','checkpoint','prefix','{}','publication',3)
    db.execute('INSERT INTO research_rolling_frozen_publication_r343 VALUES('+','.join('?'*22)+')',pub)
    def selected(predicate): return {x[0] for x in db.execute('SELECT f.candidate_id FROM r967_frozen_evaluation_v2 f WHERE '+predicate)}
    assert selected(full)=={'old','new-base','rolling'}
    assert selected(old_only)=={'old','new-base'}
    partial=sqlite3.connect(':memory:');partial.deserialize(db.serialize());partial.execute('DROP TABLE research_rolling_frozen_publication_r343')
    assert {x[0] for x in partial.execute('SELECT f.candidate_id FROM r967_frozen_evaluation_v2 f WHERE '+old_only)}=={'old','new-base'}
    partial.close()
    baseline="f.run_hash IN (SELECT r.run_hash FROM r967_numeric_training_run r WHERE r.learner_id=f.learner_id AND r.first_issue=(SELECT MAX(r2.first_issue) FROM r967_numeric_training_run r2 WHERE r2.learner_id=f.learner_id AND r2.first_issue<='202611260010'))"
    assert selected(baseline)=={'new-base'},'baseline loss of old/rolling lineage was not reproduced'
    assert db.execute('SELECT COUNT(*) FROM r967_numeric_training_run').fetchone()[0]==2
    assert db.execute('SELECT * FROM r967_frozen_evaluation_v2 WHERE candidate_id=?',('old',)).fetchone()==before
    add_eval('bad-epoch','old-run','202611260004',OLDER,epoch='different')
    add_eval('bad-rule','old-run','202611260004','c'*64)
    add_eval('missing-run','missing','202611260004')
    add_eval('bad-ast','old-run','202611260004',OLDER,ast='AST3:changed')
    add_eval('future','new-base-run','202611260011')
    add_eval('postdraw-trained','new-base-run','202611260004',trained=TARGET)
    assert selected(full)=={'old','new-base','rolling'},'invalid identity/domain admitted'
    db.execute("UPDATE r952_candidate SET state='QUARANTINED'")
    assert not selected(full)
    db.execute("UPDATE r952_candidate SET state='ACTIVE',structural_hash='changed'")
    assert not selected(full)
    db.execute("UPDATE r952_candidate SET structural_hash='astHash'")
    immutable_rejected=0
    for sql,args in [("UPDATE research_rolling_frozen_publication_r343 SET first_issue='202611260008'",()),('DELETE FROM research_rolling_frozen_publication_r343',()),('INSERT OR REPLACE INTO research_rolling_frozen_publication_r343 VALUES('+','.join('?'*22)+')',pub),('UPDATE r967_frozen_evaluation_v2 SET model_payload=? WHERE candidate_id=?',('hindsight','old')),('DELETE FROM r967_numeric_training_run WHERE run_hash=?',('old-run',))]:
        try: db.execute(sql,args)
        except sqlite3.IntegrityError: immutable_rejected+=1
        else: raise AssertionError('immutable source accepted mutation: '+sql)
    db.execute("INSERT INTO research_rolling_publication_page_r343 VALUES('v1',0,32,'prefix0','prefix32','page-hash',4)")
    for sql in ["UPDATE research_rolling_publication_page_r343 SET end_index=31", "INSERT OR REPLACE INTO research_rolling_publication_page_r343 VALUES('v1',0,32,'prefix0','changed','changed',5)","INSERT INTO research_rolling_publication_page_r343 VALUES('v1',32,65,'x','y','z',6)"]:
        try: db.execute(sql)
        except sqlite3.IntegrityError: immutable_rejected+=1
        else: raise AssertionError(sql)
    try: db.execute("INSERT INTO research_rolling_publication_reject_r343 VALUES('v1','CONFLICT','{}','reject-conflict',1)")
    except sqlite3.IntegrityError as e:
        assert 'R343_PUBLICATION_OUTCOME_CONFLICT' in str(e);immutable_rejected+=1
    else: raise AssertionError('published version also accepted a rejection')
    db.execute("INSERT INTO research_rolling_version_r343 VALUES('v2')")
    db.execute("INSERT INTO research_rolling_publication_reject_r343 VALUES('v2','TEST_FIXTURE','{}','reject',1)")
    for sql in ["DELETE FROM research_rolling_publication_reject_r343","INSERT OR REPLACE INTO research_rolling_publication_reject_r343 VALUES('v2','CHANGED','{}','changed',2)"]:
        try: db.execute(sql)
        except sqlite3.IntegrityError: immutable_rejected+=1
        else: raise AssertionError(sql)
    result={'status':'PASS','scope':'Actual source Java SQL builders and DDL executed against synthetic SQLite fixtures; not Android or real model publication','ddl_statements':len(ddls),'schema_tables':3,'immutable_or_bounds_rejections':immutable_rejected,'baseline_latest_run_exclusion_reproduced':True,'old_frozen_continues_after_new_training_head':True,'independent_rolling_frozen_admitted_without_new_training_run':True,'incompatible_or_missing_identity_rejected':6,'retired_parent_and_ast_drift_rejected':True,'partial_schema_fails_closed_without_missing_table_sql':True,'sql_injection_inputs_rejected':2,'freeze_matrix_exact_component_contract_preserved':True,'old_frozen_bytes_unchanged':True,'original_training_run_count_unchanged':True,'predicate_sha256':hashlib.sha256(predicate.encode()).hexdigest(),'source_files_sha256':{str(x.relative_to(ROOT)):hashlib.sha256(x.read_bytes()).hexdigest() for x in [PUB,AFTER/'ResearchEvaluationRunR967.java']},'sqlite_version':sqlite3.sqlite_version,'android_build':'NOT_RUN','device_publication_and_interleaving':'NOT_RUN'}
    print(json.dumps(result,ensure_ascii=False,indent=2))

if __name__=='__main__': run()
