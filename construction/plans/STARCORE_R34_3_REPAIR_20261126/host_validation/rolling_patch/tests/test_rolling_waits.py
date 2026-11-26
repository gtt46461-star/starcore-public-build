#!/usr/bin/env python3
"""Execute extracted production Java scheduling methods against real Python SQLite.

The pipe is an Android Cursor/SQLite API adapter only. Coordinator framing,
wall-clock integration, real APK and phone runtime remain NOT_RUN.
"""
import argparse,base64,hashlib,json,re,sqlite3,subprocess,tempfile
from pathlib import Path

def method(text, signature):
    start=text.index(signature); at=text.index('{',start)+1; depth=1; state='code'
    while depth:
        c=text[at]; n=text[at+1:at+2]
        if state=='code':
            if c=='"':state='string'
            elif c=="'":state='char'
            elif c=='/' and n=='/':state='line';at+=1
            elif c=='/' and n=='*':state='block';at+=1
            elif c=='{':depth+=1
            elif c=='}':depth-=1
        elif state in ('string','char'):
            if c=='\\':at+=1
            elif c==('"' if state=='string' else "'"):state='code'
        elif state=='line' and c=='\n':state='code'
        elif state=='block' and c=='*' and n=='/':state='code';at+=1
        at+=1
    return text[start:at]

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--after',type=Path,required=True);ap.add_argument('--source',type=Path,required=True);ap.add_argument('--out',type=Path,required=True);ap.add_argument('--before',type=Path,required=True)
    a=ap.parse_args(); a.out.mkdir(parents=True,exist_ok=True)
    app='app/src/main/java/com/ethk3/novaonepro/'
    lp=a.after/app/'r952/RollingLearnerR343.java'; pp=a.after/app/'r952/RollingFrozenPublisherR343.java'
    learner=lp.read_text(); publisher=pp.read_text(); queue=(a.source/app/'ResearchTaskQueueR967.java').read_text()
    lm='\n'.join(method(learner,s) for s in ['public static void schema(', 'public static boolean cleanup(', 'static boolean waitWithinR343(', 'static String writeWaitReasonR343(', 'static boolean queueHasCapacityR343(', 'public static long sourceEpochR343(', 'private static long bytes(', 'private static int changes(', 'private static void need('])
    pm='\n'.join(method(publisher,s) for s in ['public static void schema(', 'public static boolean installed(', 'public static boolean pinsRawVersion(', 'private static boolean retired(', 'private static String currentTarget(', 'private static boolean commit(', 'private static void need('])
    y=method(queue,'public static boolean yield(')
    old_l=(a.before/app/'r952/RollingLearnerR343.java').read_text();old_p=(a.before/app/'r952/RollingFrozenPublisherR343.java').read_text()
    lm+='\n'+method(old_l,'public static boolean cleanup(').replace('boolean cleanup(', 'boolean cleanupBeforeR343(',1)
    pm+='\n'+method(old_p,'private static boolean commit(').replace('boolean commit(', 'boolean commitBeforeR343(',1)
    bridge=r'''import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class RollingWaitHarness {
 static final BufferedReader IN=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
 static String token(Object o){if(o==null)return "n";if(o instanceof Number)return "l"+o;if(o instanceof byte[])return "b"+Base64.getEncoder().encodeToString((byte[])o);return "s"+Base64.getEncoder().encodeToString(o.toString().getBytes(StandardCharsets.UTF_8));}
 static Object decode(String t){if(t.equals("n"))return null;if(t.startsWith("l"))return Long.valueOf(t.substring(1));if(t.startsWith("b"))return Base64.getDecoder().decode(t.substring(1));return new String(Base64.getDecoder().decode(t.substring(1)),StandardCharsets.UTF_8);}
 static String line(){try{String x=IN.readLine();if(x==null)throw new RuntimeException("PIPE_CLOSED");if(x.startsWith("ERR\t"))throw new RuntimeException((String)decode(x.split("\t",2)[1]));return x;}catch(IOException e){throw new RuntimeException(e);}}
 static void request(String op,String sql,Object[] args){StringBuilder s=new StringBuilder(op).append('\t').append(token(sql));if(args!=null)for(Object x:args)s.append('\t').append(token(x));System.out.println(s);System.out.flush();}
 static final class Cursor implements AutoCloseable {final List<Object[]> rows;int at=-1;Cursor(List<Object[]> r){rows=r;}boolean moveToFirst(){at=0;return at<rows.size();}boolean moveToNext(){return ++at<rows.size();}String getString(int i){Object x=rows.get(at)[i];return x==null?null:x.toString();}long getLong(int i){Object x=rows.get(at)[i];return x instanceof Number?((Number)x).longValue():Long.parseLong(x.toString());}int getInt(int i){return (int)getLong(i);}boolean isNull(int i){return rows.get(at)[i]==null;}public void close(){}}
 static final class SQLiteDatabase {boolean inTransaction(){return true;}Cursor rawQuery(String sql,String[] args){request("Q",sql,args);String[] count=line().split("\t");int n=Integer.parseInt(count[1]);List<Object[]> r=new ArrayList<>();for(int i=0;i<n;i++){String[] cells=line().split("\t",-1);Object[] v=new Object[cells.length];for(int j=0;j<v.length;j++)v[j]=decode(cells[j]);r.add(v);}return new Cursor(r);}void execSQL(String sql){execSQL(sql,null);}void execSQL(String sql,Object[] args){request("E",sql,args);if(!line().equals("OK"))throw new RuntimeException("PIPE_ACK");}}
 static class DbHelper {static final String TABLE_DRAWS="draws",TABLE_R811_DRAW_CANONICAL="v57_draw_canonical_r811";}
 static class DrawDependencyInputCP12 {static final String TABLE="draw_dependency_input_cp12";}
 static class ResearchLifecycleR932 {static final String TABLE="life";}
 static class TrainingCodeIdentityR964 {static final String SHA256="rule";}
 static class TargetIssueResolver {String targetIssue;TargetIssueResolver(String s){targetIssue=s;}static boolean isValid(String x){return x!=null&&x.matches("[0-9]{12}");}static TargetIssueResolver next(String x){return new TargetIssueResolver(String.format("%012d",Long.parseLong(x)+1));}}
 static class DevelopmentProductionCP12 {static class Wait extends RuntimeException{final String reason;Wait(String s){super(s);reason=s;}}}
 static class ResearchTaskQueueR967 {
  static final String TABLE="v57_research_task_r967";static final int MAX_ACTIVE=4096;static final Map<String,Lease> ACTIVE_CP4=new HashMap<>();
  static class Lease {String id="task",kind="R343_ROLLING_FREEZE",cursor="ORIGINALS|420|prefix",owner="owner";long generation=3;}
  static void transaction(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("TX");}
  static int changed(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){c.moveToFirst();return c.getInt(0);}}
  static boolean owns(SQLiteDatabase db,Lease t){try(Cursor c=db.rawQuery("SELECT 1 FROM "+TABLE+" WHERE task_id=? AND generation=? AND cursor=? AND lease_owner=? AND status='RUNNING'",new String[]{t.id,Long.toString(t.generation),t.cursor,t.owner})){return c.moveToFirst();}}
  static void complete(SQLiteDatabase db,Lease t,String result){if(!owns(db,t))throw new IllegalStateException("COMPLETE_CAS");db.execSQL("UPDATE "+TABLE+" SET status='COMPLETED',lease_owner='',lease_until=0,result_hash=? WHERE task_id=?",new Object[]{result,t.id});}
  YIELD
 }
 static class LongSummaryBindingsR80 {static void validateProtocolSources(SQLiteDatabase d,Map<String,Object> m){if(Boolean.TRUE.equals(m.get("invalid")))throw new IllegalStateException("REAL_INTEGRITY_FAILURE");}static void registerFrozenSourcesR81(SQLiteDatabase d,String s,List<String> x){}}
 static class MatrixSourceRouterCP12 {static void registerFrozenPolicyMatricesCP12(SQLiteDatabase d,String x){}static void registerFrozenAtomStudiesCP12(SQLiteDatabase d,String x,Ast a,Model m){}static void enqueueFreezeCP12(SQLiteDatabase d,String s,String t,String r,Map<String,String> c,long n){}}
 static class Ast {String canonical="AST3:fixture";Graph graphSpec=new Graph();}static class Graph {Manifest manifest=new Manifest();}static class Manifest {String hash="manifest";}static class Model {Modern modern(){return new Modern();}}static class Modern {String normalizerHash="normalizer";}
 static class Sources {List<String> epochs=new ArrayList<>();void validateAtCommit(SQLiteDatabase d){}}
 static class ResearchEvaluationRunR967 {static final String EVAL="r967_frozen_evaluation_v2",SPLIT="split";static final int MAX_ACTIVE_PER_LEARNER=2,MAX_ACTIVE_GLOBAL=32;static boolean isDrawn;static boolean drawn(SQLiteDatabase d,String t){return isDrawn;}static String frozenEligibilitySqlR343(SQLiteDatabase d,String a,String t){return "1=1";}static void initializeRollingFrozenLifecycleR343(SQLiteDatabase d,String id){}}
 static class RollingLearnerR343 {
  static final String EVENT="research_rolling_event_r343",VERSION="research_rolling_version_r343",HEAD="research_rolling_head_r343",FEATURE="research_rolling_feature_r343",OFFER="research_rolling_offer_r343";
  static final long WORKING_LIMIT=64L<<20;
  LEARNER_METHODS
 }
 static class RollingFrozenPublisherR343 {
  static final String TABLE="research_rolling_frozen_publication_r343",REJECT="research_rolling_publication_reject_r343",PAGE="research_rolling_publication_page_r343",TERMINAL="('QUALITY_PRUNED','QUARANTINED','RESOURCE_RETIRED')";
  static class Offer {String version="result",candidate="base",modelPayload="payload",modelHash="model",cutoff="202611260100",astHash="ast",protocolHash="protocol",inputHash="input",receiptHash="receipt",checkpointHash="checkpoint",prefix="prefix";int trainN=300;}
  static class Prepared {Offer offer=new Offer();String target="202611260101",id="new-freeze",structure="structure",artifact="artifact",run="run",epoch="epoch",json="{}",hash="hash";long sourceEpoch=0;Ast ast=new Ast();Model model=new Model();Map<String,Object> protocol=new TreeMap<>();Sources sources=new Sources();}
  static void metadataCas(SQLiteDatabase d,Offer v){}
  PUBLISHER_METHODS
 }
 public static void main(String[] args){SQLiteDatabase db=new SQLiteDatabase();try{
  Object result;
  switch(args[0]){
   case "schema":RollingLearnerR343.schema(db);RollingFrozenPublisherR343.schema(db);result=true;break;
   case "cleanup":result=RollingLearnerR343.cleanup(db);break;
   case "cleanup_before":result=RollingLearnerR343.cleanupBeforeR343(db);break;
   case "wait":result=RollingLearnerR343.waitWithinR343(db,new ResearchTaskQueueR967.Lease(),args[1]);break;
   case "reason":result=RollingLearnerR343.writeWaitReasonR343(db,Long.parseLong(args[1]),Boolean.parseBoolean(args[2]),Long.parseLong(args[3]));break;
   case "capacity":result=RollingLearnerR343.queueHasCapacityR343(db);break;
   case "commit":case "commit_before":RollingFrozenPublisherR343.Prepared p=new RollingFrozenPublisherR343.Prepared();p.sourceEpoch=Long.parseLong(args[1]);p.target=args[2];ResearchEvaluationRunR967.isDrawn=Boolean.parseBoolean(args[3]);if(args.length>4)p.protocol.put("invalid",true);result=args[0].equals("commit_before")?RollingFrozenPublisherR343.commitBeforeR343(db,new ResearchTaskQueueR967.Lease(),p):RollingFrozenPublisherR343.commit(db,new ResearchTaskQueueR967.Lease(),p);break;
   default:throw new IllegalArgumentException("MODE");
  }System.out.println("RESULT\t"+token(String.valueOf(result)));
 }catch(RuntimeException x){System.out.println("EXCEPTION\t"+token(x.getClass().getName()+":"+x.getMessage()));}System.out.flush();}
}'''.replace('YIELD',y).replace('LEARNER_METHODS',lm).replace('PUBLISHER_METHODS',pm)
    java=a.out/'RollingWaitHarness.java';java.write_text(bridge)
    subprocess.run(['java','com.sun.tools.javac.Main','-d',str(a.out),str(java)],check=True)
    def encode(x):
        if x is None:return 'n'
        if isinstance(x,(int,float)):return 'l'+str(x)
        if isinstance(x,bytes):return 'b'+base64.b64encode(x).decode()
        return 's'+base64.b64encode(str(x).encode()).decode()
    def decode(x):
        if x=='n':return None
        if x.startswith('l'):return int(x[1:])
        b=base64.b64decode(x[1:]);return b if x.startswith('b') else b.decode()
    traces=[]
    def run(db,*args):
        db.commit();db.execute('BEGIN');proc=subprocess.Popen(['java','-cp',str(a.out),'RollingWaitHarness',*map(str,args)],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
        trace=[];result=None;error=None
        for line in proc.stdout:
            parts=line.rstrip('\n').split('\t');op=parts[0]
            if op in ('RESULT','EXCEPTION'):
                if op=='RESULT':result=decode(parts[1])
                else:error=decode(parts[1])
                break
            sql=decode(parts[1]);params=[decode(x) for x in parts[2:]];trace.append({'operation':op,'sql':sql,'parameters':len(params)})
            try:
                cur=db.execute(sql,params)
                if op=='Q':
                    rows=cur.fetchall();reply='ROWS\t'+str(len(rows))+'\n'+''.join('\t'.join(encode(x) for x in row)+'\n' for row in rows)
                else:reply='OK\n'
            except Exception as exc:reply='ERR\t'+encode(str(exc))+'\n'
            proc.stdin.write(reply);proc.stdin.flush()
        _,err=proc.communicate(timeout=30)
        if proc.returncode or err:raise AssertionError(err)
        if error:db.rollback()
        else:db.commit()
        traces.append({'args':list(args),'result':result,'error':error,'queries':trace})
        return result,error
    def fresh():
        db=sqlite3.connect(':memory:');db.execute('PRAGMA foreign_keys=ON')
        db.execute('CREATE TABLE r952_candidate(candidate_id TEXT PRIMARY KEY)')
        db.execute('CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)')
        for t in ('draws','draw_dependency_input_cp12'):db.execute(f'CREATE TABLE {t}(issue TEXT PRIMARY KEY,dirty INTEGER,n1 INTEGER)')
        db.execute('CREATE TABLE v57_draw_canonical_r811(target_issue TEXT PRIMARY KEY,status TEXT,canonical_draw_hash TEXT)')
        db.execute('CREATE TABLE life(candidate_key TEXT PRIMARY KEY,state TEXT)')
        db.execute('CREATE TABLE r967_frozen_evaluation_v2(candidate_id TEXT PRIMARY KEY,learner_id TEXT,ast TEXT,model_payload TEXT,structural_hash TEXT,model_hash TEXT,trained_until_issue TEXT,created_issue TEXT,source_artifact TEXT,created_at INTEGER,run_hash TEXT,rule_hash TEXT,integrity_epoch TEXT,update_endpoint INTEGER)')
        db.execute("CREATE TABLE v57_research_task_r967(task_id TEXT PRIMARY KEY,kind TEXT NOT NULL,status TEXT,cursor TEXT,generation INTEGER,lease_owner TEXT,lease_until INTEGER,available_at INTEGER,last_error TEXT,updated_at INTEGER,result_hash TEXT)")
        assert run(db,'schema')==('true',None)
        return db
    checks=[]
    def ok(name,value):
        assert value,name;checks.append(name)
    def scalar(db,sql,p=()):return db.execute(sql,p).fetchone()[0]
    def task(db):db.execute("INSERT INTO v57_research_task_r967 VALUES('task','R343_ROLLING_FREEZE','RUNNING','ORIGINALS|420|prefix',3,'owner',123,0,'',1,'')")
    def version(db,v,base=None,state='COMPLETE'):
        base=base or v
        db.execute('INSERT OR IGNORE INTO r952_candidate VALUES(?)',(base,))
        cols='version_id,candidate_id,predecessor,cutoff_issue,input_first_issue,ast_hash,protocol_hash,protocol_json,input_hash,input_bytes,input_blob,train_n,train_first,train_last,validation_first,validation_last,seed,state,prefix_hash,created_at,updated_at,checkpoint,model_payload'
        values=(v,base,'','202611260100','202611250080','ast','p'+v,'{}','input',16,b'0123456789012345',300,'202611250100','202611250399','202611250401','202611260100',42,state,'prefix',1,1,b'checkpoint','model')
        db.execute('INSERT INTO research_rolling_version_r343('+cols+') VALUES('+','.join('?'for _ in values)+')',values)
    def publication(db,v,frozen):
        db.execute('INSERT INTO r967_frozen_evaluation_v2(candidate_id,learner_id) VALUES(?,?)',(frozen,v))
        db.execute("INSERT INTO research_rolling_offer_r343 VALUES(?,?,?,'model','receipt','NEEDS_INDEPENDENT_PUBLICATION_ADAPTER',1)",(v,v,'202611260100'))
        vals=(v,frozen,'run'+v,v,'202611260101','ast','rule',300,'artifact'+v,'split','epoch','model','normalizer','manifest','p'+v,'input','receipt','cp','prefix','{}','hash'+v,1)
        db.execute('INSERT INTO research_rolling_frozen_publication_r343 VALUES('+','.join('?'for _ in vals)+')',vals)
        db.execute("INSERT INTO life VALUES(?,'ACTIVE')",('algorithm:'+frozen,))
    # Exact transaction callback waits: neither callback exceptions nor publication writes.
    for name,target,drawn,source_delta,per,glob,missing in [
        ('target_changed','202611260102',False,0,0,0,False),('target_drawn','202611260101',True,0,0,0,False),
        ('source_epoch_changed','202611260101',False,1,0,0,False),('learner_capacity','202611260101',False,0,2,0,False),
        ('global_capacity','202611260101',False,0,0,32,False),('target_source_missing','202611260101',False,0,0,0,True)]:
        db=fresh();task(db)
        if not missing:db.execute("INSERT INTO draws VALUES('202611260100',0,1)")
        epoch=int(scalar(db,"SELECT state_value FROM app_state WHERE state_key='r343_rolling_canonical_epoch'"))
        for i in range(per+glob):db.execute('INSERT INTO r967_frozen_evaluation_v2(candidate_id,learner_id) VALUES(?,?)',('old'+str(i),'base' if i<per else 'other'+str(i)))
        before=scalar(db,'SELECT COUNT(*) FROM r967_frozen_evaluation_v2')
        old_result,old_error=run(db,'commit_before',epoch-source_delta,target,str(drawn).lower())
        ok(name+'_old_callback_was_fault_and_running',old_result is None and 'Wait' in old_error and scalar(db,"SELECT status FROM v57_research_task_r967 WHERE task_id='task'")=='RUNNING')
        r,e=run(db,'commit',epoch-source_delta,target,str(drawn).lower())
        ok(name+'_returns_scheduling_result',r=='true' and e is None)
        row=db.execute("SELECT status,cursor,lease_owner,result_hash,last_error FROM v57_research_task_r967 WHERE task_id='task'").fetchone()
        expected={'target_changed':'TARGET_SUPERSEDED','target_drawn':'TARGET_SUPERSEDED','source_epoch_changed':'ROLLING_PUBLICATION_SOURCE_CHANGED','learner_capacity':'WAIT_FROZEN_LEARNER_CAPACITY','global_capacity':'WAIT_FROZEN_GLOBAL_CAPACITY','target_source_missing':'WAIT_FUTURE_PUBLICATION_TARGET'}[name]
        ok(name+'_exact_wait_reason',row==('YIELDED','ORIGINALS|420|prefix','','',expected))
        ok(name+'_no_new_publication',scalar(db,'SELECT COUNT(*) FROM r967_frozen_evaluation_v2')==before and scalar(db,'SELECT COUNT(*) FROM research_rolling_frozen_publication_r343')==0)
        db.close()
    # Original valid publication branch still reaches its same insert/complete path.
    db=fresh();task(db);version(db,'result','base');db.execute("INSERT INTO draws VALUES('202611260100',0,1)")
    epoch=int(scalar(db,"SELECT state_value FROM app_state WHERE state_key='r343_rolling_canonical_epoch'"))
    ok('valid_publication_branch_preserved',run(db,'commit',epoch,'202611260101','false')==('true',None))
    ok('valid_publication_one_frozen_and_receipt',scalar(db,"SELECT COUNT(*) FROM r967_frozen_evaluation_v2 WHERE candidate_id='new-freeze'")==1 and scalar(db,"SELECT COUNT(*) FROM research_rolling_frozen_publication_r343 WHERE frozen_id='new-freeze'")==1)
    ok('valid_publication_task_complete',db.execute("SELECT status,result_hash FROM v57_research_task_r967 WHERE task_id='task'").fetchone()==('COMPLETED','new-freeze'))
    db.close()
    # Genuine failures and failed scheduling CAS remain failures and roll back.
    db=fresh();task(db);db.execute("INSERT INTO draws VALUES('202611260100',0,1)")
    epoch=int(scalar(db,"SELECT state_value FROM app_state WHERE state_key='r343_rolling_canonical_epoch'"))
    r,e=run(db,'commit',epoch,'202611260101','false','invalid')
    ok('integrity_failure_not_downgraded',r is None and 'REAL_INTEGRITY_FAILURE' in e and scalar(db,"SELECT status FROM v57_research_task_r967")=="RUNNING")
    db.execute("UPDATE v57_research_task_r967 SET generation=4")
    r,e=run(db,'wait','ROLLING_STORAGE_CAPACITY')
    ok('stale_lease_not_released',r is None and 'ROLLING_WAIT_QUEUE_CAS' in e and scalar(db,"SELECT status FROM v57_research_task_r967")=="RUNNING")
    db.close()
    # Production budget/source helper handles backpressure without an exception.
    db=fresh();version(db,'v')
    ok('memory_capacity_reason',run(db,'reason',64<<20,'false',0)==('ROLLING_STORAGE_CAPACITY',None))
    ok('source_changed_reason',run(db,'reason',1,'true',99)==('ROLLING_SOURCE_CHANGED_DURING_COMMIT',None))
    ok('budget_within_capacity',run(db,'reason',1,'false',0)==('',None))
    ok('negative_incoming_is_failure',run(db,'reason',-1,'false',0)[1] is not None)
    # Queue capacity excludes maintenance and terminals, uses installed global payload bound.
    db.executemany("INSERT INTO v57_research_task_r967(task_id,kind,status) VALUES(?,'OTHER','PENDING')",[(str(i),) for i in range(4096)])
    ok('full_payload_capacity',run(db,'capacity')==('false',None))
    db.execute("UPDATE v57_research_task_r967 SET status='COMPLETED' WHERE task_id='0'")
    db.executemany("INSERT INTO v57_research_task_r967(task_id,kind,status) VALUES(?,'MAINTENANCE','PENDING')",[('m'+str(i),) for i in range(22)])
    ok('maintenance_reserved_and_terminal_excluded',run(db,'capacity')==('true',None));db.close()
    # Exercise the exact cleanup method, original pin resolver and real production guards.
    db=fresh()
    for i in range(16):
        v=f'{i:03d}';version(db,v);publication(db,v,'freeze'+v)
    version(db,'999')
    for i in range(70):db.execute("INSERT INTO research_rolling_feature_r343 VALUES('999',?,? ,1,'raw',?,'row','{}','source')",(i,f'20261125{i:04d}',b'x'*18))
    db.execute("INSERT INTO research_rolling_head_r343(candidate_id,version_id,updated_at) VALUES('999','999',1)")
    for _ in range(3):ok('old_cleanup_stuck_behind_first16_'+str(_),run(db,'cleanup_before')==('false',None) and scalar(db,"SELECT COUNT(*) FROM research_rolling_feature_r343 WHERE version_id='999'")==70)
    before=db.execute("SELECT version_id,input_blob,checkpoint,model_payload FROM research_rolling_version_r343 WHERE version_id<'999' ORDER BY version_id").fetchall()
    ok('pinned_first_16_keep_raw',run(db,'cleanup')==('false',None))
    ok('cleanup_cursor_advanced_past_pins',scalar(db,"SELECT state_value FROM app_state WHERE state_key='r343_rolling_cleanup_after'")=='015')
    ok('later_reclaimable_reached',run(db,'cleanup')==('true',None))
    ok('cleanup_bounded_64_rows',scalar(db,"SELECT COUNT(*) FROM research_rolling_feature_r343 WHERE version_id='999'")==6)
    ok('cursor_wrap_no_fault',run(db,'cleanup')==('false',None) and scalar(db,"SELECT state_value FROM app_state WHERE state_key='r343_rolling_cleanup_after'")=='')
    for _ in range(8):run(db,'cleanup')
    ok('raw_reclaimed_after_revisit',scalar(db,"SELECT COUNT(*) FROM research_rolling_feature_r343 WHERE version_id='999'")==0 and db.execute("SELECT input_blob,checkpoint FROM research_rolling_version_r343 WHERE version_id='999'").fetchone()==(None,None))
    ok('all_frozen_originals_identical',before==db.execute("SELECT version_id,input_blob,checkpoint,model_payload FROM research_rolling_version_r343 WHERE version_id<'999' ORDER BY version_id").fetchall())
    # HEAD model retained, yet no longer prevents scanning later versions.
    for _ in range(6):run(db,'cleanup')
    ok('head_model_retained',scalar(db,"SELECT model_payload FROM research_rolling_version_r343 WHERE version_id='999'")=='model')
    version(db,'active',state='FEATURES')
    for _ in range(4):run(db,'cleanup')
    ok('active_input_retained',scalar(db,"SELECT length(input_blob) FROM research_rolling_version_r343 WHERE version_id='active'")==16)
    ok('sqlite_integrity',db.execute('PRAGMA integrity_check').fetchall()==[('ok',)] and db.execute('PRAGMA foreign_key_check').fetchall()==[])
    # Check the integration ordering under the actual source, without counting it as runtime evidence.
    drain=method(learner,'public static boolean drainOne(')
    ok('cleanup_reachable_before_claim',drain.index('RollingLearnerR343::cleanup')<drain.index('ResearchTaskQueueR967.claim'))
    for owner in (learner,publisher):
        ok('no_expected_wait_throw_inside_commit_'+('learner' if owner is learner else 'publisher'),'throw new DevelopmentProductionCP12.Wait("ROLLING_STORAGE_CAPACITY")' not in owner and 'throw new DevelopmentProductionCP12.Wait("WAIT_FROZEN_' not in owner)
    result={'status':'PASS','assertions':len(checks),'checks':checks,'source_hashes':{str(lp):hashlib.sha256(lp.read_bytes()).hexdigest(),str(pp):hashlib.sha256(pp.read_bytes()).hexdigest()},'sqlite_version':sqlite3.sqlite_version,'scope':'Extracted unchanged Java methods + real SQLite via pipe adapter. Publisher metadata/codec/eligibility stubs are preconditions, not tested here. Android/coordinator timing remain NOT_RUN.','android':'NOT_RUN','device':'NOT_RUN','natural_quality':'NOT_PROVEN'}
    (a.out/'RESULT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    (a.out/'sql_traces.json').write_text(json.dumps(traces,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(result,ensure_ascii=False))
if __name__=='__main__':main()
