package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Actual CONFIRM owner/Store and SQLite protocol. Time-compressed rows below
 * are explicit SYNTHETIC MECHANICS, never old natural predictions or quality. */
public final class ImprovementStoreR31Host {
    static int assertions;
    static void ok(boolean v,String why){assertions++;if(!v)throw new AssertionError(why);}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    static void tx(SQLiteDatabase db,Runnable r){db.beginTransaction();try{r.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static void write(SQLiteDatabase db,ResearchWriteQueueR27.Batch b){tx(db,()->{try{if(b.before!=null&&b.before.call(db)<0)throw new IllegalStateException("HOST_ACTUAL_GUARD_REJECTED");for(ResearchWriteQueueR27.Op op:b.ops)db.execSQL(op.sql,op.args);if(b.after!=null&&b.after.call(db)<0)throw new IllegalStateException("HOST_ACTUAL_POST_GUARD_REJECTED");}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}});}
    static SQLiteDatabase open(Path p)throws Exception{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+p));}
    static void schema(SQLiteDatabase db){tx(db,()->{
        db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,first_seen_at INTEGER,dirty INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER)");
        for(String sql:SchemaR27.DDL)if(sql.startsWith("CREATE TABLE IF NOT EXISTS r27_live_shadow(")||sql.startsWith("CREATE TABLE IF NOT EXISTS r27_live_frozen(")||sql.startsWith("CREATE TRIGGER IF NOT EXISTS r27_live_frozen_immutable ")||sql.startsWith("CREATE TABLE IF NOT EXISTS r27_eprocess(")||sql.startsWith("CREATE TABLE IF NOT EXISTS r27_replay_score(")||sql.startsWith("CREATE TABLE IF NOT EXISTS r27_holdout_exposure("))db.execSQL(sql);
        for(String sql:FrozenConfirmationStoreR30.DDL)db.execSQL(sql);db.execSQL(HoldoutLedgerR27.CONFIRM_DDL);
        db.execSQL("CREATE TABLE r27_history_import(issue TEXT,ordinal INTEGER,numbers BLOB,status TEXT)");
        for(String sql:SchemaR27.sourceRevisionDefinitionsR31())db.execSQL(sql);db.execSQL("INSERT INTO r30_live_source_revision VALUES(1,0)");
        for(String sql:ImprovementStoreR31.DDL)db.execSQL(sql);for(String sql:ImprovementStoreR31.sourceGuardsR31(db))db.execSQL(sql);ImprovementStoreR31.seedWithinMigrationR31(db);
    });}
    static void draw(SQLiteDatabase db,long ordinal,long at){Object[] row=new Object[14];row[0]=TargetIssueResolver.fromSequenceOrdinalSU1(ordinal);row[1]=at;row[2]=at+1;row[3]=0;for(int n=1;n<=10;n++)row[n+3]=n;db.execSQL("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",row);}
    static PolicySpecR27 policy(int position,int window){return new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,window)),position,PolicySpecR27.Rule.ALWAYS,0,1,1,false,.01,0,IdeaFamilyR27.meta("I09","SYNTHETIC_PAIR_MECHANICS",1,window,"NOT_NATURAL_QUALITY"));}
    static FrozenConfirmationR30.Task packet(PolicySpecR27 parent,PolicySpecR27 child,ReplayDatasetR27 d,long at){
        try(FeatureCacheR27 cache=new FeatureCacheR27(d);EvolutionLoopR27 loop=new EvolutionLoopR27(new EvolutionLoopR27.Config(),cache,null,()->at)){
            loop.cfg.nullControl=false;loop.sourceCommit=BuildConfig.SOURCE_COMMIT_R26;loop.generation=3;loop.champion=child;loop.championTau=1;loop.previousChampion=parent;loop.previousTau=1;return FrozenConfirmationR30.freeze(loop,at);
        }
    }
    @SuppressWarnings("unchecked") static Map<String,Object> parse(String json){return (Map<String,Object>)JsonR27.parse(json);}
    static String[] registration(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT segment_key,registration_json,registration_hash FROM r31_improvement_registration ORDER BY window_from LIMIT 1",null)){if(!c.moveToFirst())throw new AssertionError("NO_REGISTRATION");return new String[]{c.getString(0),c.getString(1),c.getString(2)};}}
    static void original(SQLiteDatabase db,String id,long ord,long frozen,long open,int first,boolean acting,long cpu){double[] p=new double[10];Arrays.fill(p,.1);ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();ImprovementStoreR31.appendFreezeCost(db,b,id,ord,cpu);b.add("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit,settled_at) VALUES(?,?,?,1,?,?,?,?,?,'FROZEN',?,?,?,?)",id,ord,TargetIssueResolver.fromSequenceOrdinalSU1(ord),StoreR27.pack(p),first,first+1,first+2,acting?1:0,frozen,open,first==1?1:0,open+2);write(db,b);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path file=Path.of(args[0]);long at=System.currentTimeMillis();long head=TargetIssueResolver.sequenceOrdinalSU1("202611130900");
        try(SQLiteDatabase db=open(file)){
            schema(db);ok(ImprovementStoreR31.ready(db),"actual20DDL plus migration seed ready");
            List<ReplayDatasetR27.Row> rows=new ArrayList<>();for(int t=0;t<3600;t++){long n=head-3599+t;int[] numbers=new int[10];for(int p=0;p<10;p++)numbers[p]=(p+t)%10+1;String issue=TargetIssueResolver.fromSequenceOrdinalSU1(n);rows.add(new ReplayDatasetR27.Row(issue,n,numbers,at-(3600-t)*60000L,at-(3600-t)*60000L+1,ReplayDatasetR27.READER_ABI_R31,"SYNTHETIC_MECHANICS"));}
            ReplayDatasetR27 data=ReplayDatasetR27.of(rows);PolicySpecR27 parent=policy(1,32),child=policy(1,128);FrozenConfirmationR30.Task task=packet(parent,child,data,at);ok(task!=null&&task.selectionProcessR31!=null,"actual production packet freeze binds original selection process");
            Map<String,Object> old=parse(task.json());old.remove("selectionProcessR31");String oldJson=JsonR27.write(old);ok(oldJson.equals(FrozenConfirmationR30.Task.parse(oldJson).json()),"B06 old packet codec exact roundtrip preserved");
            tx(db,()->draw(db,head,at-59000));ResearchWriteQueueR27.Batch frozen=new ResearchWriteQueueR27.Batch();FrozenConfirmationStoreR30.freeze(frozen,task,data);write(db,frozen);
            ok(scalar(db,"SELECT count(*) FROM r31_improvement_registration")==1,"real FrozenConfirmationStore.freeze writer enrolls future segment");String[] r=registration(db);Map<String,Object> reg=parse(r[1]);long from=((Number)reg.get("windowFrom")).longValue(),to=((Number)reg.get("windowTo")).longValue();String pid=(String)reg.get("parentRuntimeId"),cid=(String)reg.get("childRuntimeId");
            ok(from>head&&to-from+1==720&&from%720==0,"next full720calendar block exclusively future");ok(((Number)reg.get("registeredAt")).longValue()==at||((Number)reg.get("registeredAt")).longValue()>at,"real registration time never backdated");
            write(db,frozen);ok(scalar(db,"SELECT count(*) FROM r31_improvement_registration")==1,"same packet replay does not acquire another segment");
            ResearchWriteQueueR27.Batch renamed=new ResearchWriteQueueR27.Batch();ImprovementStoreR31.appendRegistration(renamed,packet(policy(2,32),policy(2,128),data,at+1));write(db,renamed);ok(scalar(db,"SELECT count(*) FROM r31_improvement_registration")==1,"renamed pair cannot reopen globally spent calendar segment");
            try(FeatureCacheR27 cache=new FeatureCacheR27(data);EvalContextR27 ctx=new EvalContextR27(cache,SplitR27.of(data,null),null)){
                HoldoutLedgerR27 ledger=new HoldoutLedgerR27();ledger.setConfirmPersistence(read->write(db,ConfirmReadStoreR30.reservation(read)));
                FrozenConfirmationR30.Result result=FrozenConfirmationR30.evaluate(task,ctx,ledger,FrozenConfirmationStoreR30.evidence(db,(op,b)->write(db,b)));
                Map<String,Object> outer=(Map<String,Object>)result.report.get("pairedImprovementR31");ok("PROVISIONAL".equals(outer.get("status"))&&outer.get("predictionFrozenAt")==null,"actual independent HOLDOUT receipt never fabricates historical pre-outcome freeze time");
                ok(scalar(db,"SELECT count(*) FROM r30_confirm_trace")==2&&scalar(db,"SELECT count(*) FROM r30_confirm_read")==2,"original two committed exposures and full traces back paired receipt");
                ResearchWriteQueueR27.Batch completed=new ResearchWriteQueueR27.Batch();FrozenConfirmationStoreR30.complete(completed,task,result);write(db,completed);
                ok(FrozenConfirmationStoreR30.result(db,task.key).report.get("pairedImprovementR31") instanceof Map,"original completion consumer persists and reads actual traceable receipt");
                ConfirmReadStoreR30.restore(db,new HoldoutLedgerR27());boolean reused=false;try{ledger.confirm(task.champion.policyHash,data.datasetHash,task.block,task.from,task.to,false);}catch(RuntimeException expected){reused=true;}ok(reused,"old exposure cannot reopen under paired receipt");
            }
            ok(ImprovementStoreR31.reservedObserverSlots(db)==2,"optional pair reserves2 from existing24exploration slots");
            tx(db,()->{for(int i=0;i<24;i++)db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi) VALUES(?,?,1,?,0,'OBSERVE_REPAIR1','SYNTHETIC',?,?,-1,?,'')",new Object[]{"explore"+i,parent.fullJson(),StoreR27.pack(new double[10]),at,at,parent.policyHash()});});
            ok(!ImprovementStoreR31.routePair(db,(op,b)->write(db,b)),"full original observer pool waits and never expands");ok(scalar(db,"SELECT count(*) FROM r27_live_shadow WHERE status IN ('OBSERVE_REPAIR1','OBSERVE_PAIR_R31')")==24,"pool remains24while waiting");
            // Setup-only release models the original refresh retiring2 lower
            // exploration slots; no qualification row/alpha is reset or stolen.
            tx(db,()->db.execSQL("UPDATE r27_live_shadow SET status='OBSERVE_REPAIR1_RETIRED' WHERE policy_hash IN ('explore22','explore23')"));
            ok(ImprovementStoreR31.routePair(db,(op,b)->write(db,b)),"real subscription writer activates reachable dual-arm original freeze lane");ok(scalar(db,"SELECT count(*) FROM r27_live_shadow WHERE status='OBSERVE_PAIR_R31'")==2,"exact2pair arms");ok(scalar(db,"SELECT count(*) FROM r27_eprocess")==0,"no eprocess/alpha/qualification allocated");ok(scalar(db,"SELECT count(*) FROM r27_live_shadow WHERE status IN ('OBSERVE_REPAIR1','OBSERVE_PAIR_R31')")==24,"subscribed pair stays inside original24cap");
            ok(!ImprovementStoreR31.permitsPairFreeze(db,pid,from-1)&&ImprovementStoreR31.permitsPairFreeze(db,pid,from),"no pre-registration-window freeze route");
            long registered=((Number)reg.get("registeredAt")).longValue();long observed=registered+10000;tx(db,()->draw(db,from,observed));original(db,pid,from,registered+1,observed,4,true,200);original(db,cid,from,registered+1,observed,1,false,100);ok(scalar(db,"SELECT count(*) FROM r31_improvement_cost")==2,"same actual freeze writer callbacks persist bound pair CPU receipts");
            ResearchWriteQueueR27.Batch retry=new ResearchWriteQueueR27.Batch();ImprovementStoreR31.appendFreezeCost(db,retry,pid,from,9999);write(db,retry);ok(scalar(db,"SELECT cpu_nanos FROM r31_improvement_cost WHERE policy_hash='"+pid+"'")==200,"cost cannot be overwritten or attributed to a retry of existing frozen row");
            // Remaining controlled rows compress720future periods for protocol
            // validation only. They are not natural predictions or runtime quality.
            for(long n=from+1;n<=to;n++){final long ord=n,opened=observed+(n-from)*60000;tx(db,()->draw(db,ord,opened));original(db,pid,n,opened-10000,opened,4,true,200);original(db,cid,n,opened-10000,opened,1,n%2==0,100);}
            long asOf=observed+721*60000;ImprovementStoreR31.Prepared p=ImprovementStoreR31.prepareAt(db,r[0],r[1],r[2],asOf);Map<String,Object> claim=parse(p.claim);ok("VERIFIED".equals(claim.get("status")),"complete controlled paired original720window passes actual helper+Store protocol");ok(((Number)claim.get("expectedCalendarOpportunities")).longValue()==720&&((Number)claim.get("validPairedOpportunities")).longValue()==720,"all scheduled opportunities retained despite different action counts");ok(((Number)claim.get("parentActs")).longValue()==720&&((Number)claim.get("childActs")).longValue()<720,"differentACT denominators do not select only child wins");ok(Boolean.FALSE.equals(claim.get("championReplacedByThisClaim")),"claim never changes original champion rule");
            ImprovementStoreR31.Prepared clock=ImprovementStoreR31.prepare(db,r[0],r[1],r[2]);ok("PROVISIONAL".equals(parse(clock.claim).get("status")),"real current clock rejects compressed future outcomes");
            tx(db,()->db.execSQL("DELETE FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new Object[]{cid,from+9}));ImprovementStoreR31.Prepared missing=ImprovementStoreR31.prepareAt(db,r[0],r[1],r[2],asOf);ok("PROVISIONAL".equals(parse(missing.claim).get("status")),"selected/missing child opportunity cannot verify");
            tx(db,()->db.execSQL("INSERT INTO r31_improvement_claim VALUES(?,?,?,?,?,?,?)",new Object[]{p.segment,p.claim,StatsR27.sha256(p.claim),p.evidence,p.revision,p.frozenRevision,asOf}));long charge=scalar(db,"SELECT used_bytes FROM r31_improvement_quota");boolean changed=false;try{tx(db,()->db.execSQL("UPDATE r31_improvement_claim SET claim_hash='bad'"));}catch(RuntimeException expected){changed=true;}ok(changed,"immutable persisted claim rejects update");
            tx(db,()->db.execSQL("INSERT OR REPLACE INTO r31_improvement_claim VALUES(?,?,?,?,?,?,?)",new Object[]{p.segment,"{}","bad","bad",0,0,asOf}));ok(scalar(db,"SELECT used_bytes FROM r31_improvement_quota")==charge,"REPLACE cannot change original or double-charge segment");
            SQLiteDatabase.trace.clear();Map<String,Object> report=ImprovementStoreR31.report(db);ok(((Number)report.get("claims")).longValue()==1,"actual readonly report consumes persisted claim");ok(SQLiteDatabase.trace.stream().noneMatch(x->x.contains("\texecSQL\t")),"report performs zeroDDL/DML");
        }
        try(SQLiteDatabase db=open(file)){ok(scalar(db,"SELECT count(*) FROM r31_improvement_registration")==1&&scalar(db,"SELECT count(*) FROM r31_improvement_claim")==1,"fresh SQLite connection keeps one registration/budget/claim after restart");}
        try(SQLiteDatabase empty=open(Path.of(args[0]+"-empty"))){ok("UNKNOWN_SCHEMA_OR_QUOTA".equals(ImprovementStoreR31.report(empty).get("state")),"missing optional schema remainsUNKNOWN and no DDL");}
        System.out.println("IMPROVEMENT_NATIVE_PASS assertions="+assertions+" actualPacketFreezeRegistration=true actualSubscriptionWriter=true actualCostWriterCallback=true controlledCalendarRows=720 scope=SYNTHETIC_NATIVE_PROTOCOL_NOT_NATURAL_OR_ANDROID");
    }
}
