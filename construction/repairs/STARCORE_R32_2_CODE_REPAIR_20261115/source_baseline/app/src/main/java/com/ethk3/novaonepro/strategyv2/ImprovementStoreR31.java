package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Optional observer of original natural R27 freezes. It never predicts, opens
 * HOLDOUT again, changes a champion, registers a shadow or authorizes REAL. */
public final class ImprovementStoreR31 {
    private ImprovementStoreR31() {}
    public static final String VERSION="R31_PROSPECTIVE_PAIRED_IMPROVEMENT_1";
    public static final long MAX_BYTES=32L<<20;
    public static final int WINDOW=SplitR27.BLOCK;
    public static final String[] DDL;
    static {
        List<String> sql=new ArrayList<>();
        sql.add("CREATE TABLE IF NOT EXISTS r31_improvement_quota(singleton INTEGER PRIMARY KEY CHECK(singleton=1),used_bytes INTEGER NOT NULL CHECK(used_bytes>=0 AND used_bytes<=33554432),source_revision INTEGER NOT NULL DEFAULT 0 CHECK(source_revision>=0))");
        sql.add("CREATE TABLE IF NOT EXISTS r31_improvement_registration(segment_key TEXT PRIMARY KEY,experiment_id TEXT NOT NULL UNIQUE,packet_key TEXT NOT NULL,parent_runtime_id TEXT NOT NULL,child_runtime_id TEXT NOT NULL,window_from INTEGER NOT NULL,window_to INTEGER NOT NULL,registered_at INTEGER NOT NULL,registration_json TEXT NOT NULL CHECK(length(CAST(registration_json AS BLOB))<=32768),registration_hash TEXT NOT NULL)");
        sql.add("CREATE TABLE IF NOT EXISTS r31_improvement_cost(policy_hash TEXT NOT NULL,target_ordinal INTEGER NOT NULL,cpu_nanos INTEGER NOT NULL CHECK(cpu_nanos>=0),freeze_hash TEXT NOT NULL,measured_at INTEGER NOT NULL,PRIMARY KEY(policy_hash,target_ordinal))");
        sql.add("CREATE TABLE IF NOT EXISTS r31_improvement_claim(segment_key TEXT PRIMARY KEY,claim_json TEXT NOT NULL CHECK(length(CAST(claim_json AS BLOB))<=262144),claim_hash TEXT NOT NULL,evidence_hash TEXT NOT NULL,source_revision INTEGER NOT NULL,frozen_revision INTEGER NOT NULL,recorded_at INTEGER NOT NULL)");
        sql.add("CREATE INDEX IF NOT EXISTS idx_r31_improvement_window ON r31_improvement_registration(window_to,segment_key)");
        for(String table:new String[]{"r31_improvement_registration","r31_improvement_cost","r31_improvement_claim"}){
            String charge=table.endsWith("registration")?"length(CAST(NEW.registration_json AS BLOB))+1024":table.endsWith("claim")?"length(CAST(NEW.claim_json AS BLOB))+512":"512";
            sql.add("CREATE TRIGGER IF NOT EXISTS "+table+"_no_update BEFORE UPDATE ON "+table+" BEGIN SELECT RAISE(ABORT,'R31_IMPROVEMENT_ORIGINAL_IMMUTABLE'); END");
            sql.add("CREATE TRIGGER IF NOT EXISTS "+table+"_no_delete BEFORE DELETE ON "+table+" BEGIN SELECT RAISE(ABORT,'R31_IMPROVEMENT_ORIGINAL_IMMUTABLE'); END");
            String key=table.endsWith("cost")?"policy_hash=NEW.policy_hash AND target_ordinal=NEW.target_ordinal":"segment_key=NEW.segment_key";
            sql.add("CREATE TRIGGER IF NOT EXISTS "+table+"_no_replace BEFORE INSERT ON "+table+" WHEN EXISTS(SELECT 1 FROM "+table+" WHERE "+key+") BEGIN SELECT RAISE(IGNORE); END");
            sql.add("CREATE TRIGGER IF NOT EXISTS "+table+"_admit BEFORE INSERT ON "+table+" BEGIN SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM r31_improvement_quota WHERE singleton=1 AND used_bytes+"+charge+"<=33554432) THEN RAISE(ABORT,'R31_IMPROVEMENT_RESOURCE_WAIT') END; END");
            sql.add("CREATE TRIGGER IF NOT EXISTS "+table+"_account AFTER INSERT ON "+table+" BEGIN UPDATE r31_improvement_quota SET used_bytes=used_bytes+"+charge+",source_revision=source_revision+1 WHERE singleton=1; END");
        }
        DDL=sql.toArray(new String[0]);
    }
    public static boolean ready(SQLiteDatabase db){
        Map<String,String> expected=new LinkedHashMap<>();StringBuilder names=new StringBuilder();
        List<String> required=new ArrayList<>(Arrays.asList(DDL));Collections.addAll(required,sourceGuardsR31(db));
        for(String sql:required){String tail=sql.substring(sql.indexOf("IF NOT EXISTS ")+14),name=tail.split("[ (]",2)[0];expected.put(name,sql.replaceFirst(" IF NOT EXISTS "," "));if(names.length()>0)names.append(',');names.append('\'').append(name).append('\'');}
        try(Cursor c=db.rawQuery("SELECT name,sql FROM sqlite_master WHERE name IN ("+names+")",null)){while(c.moveToNext()){String sql=expected.remove(c.getString(0));if(sql==null||!sql.equals(c.getString(1)))return false;}}
        if(!expected.isEmpty())return false;try(Cursor c=db.rawQuery("SELECT used_bytes FROM r31_improvement_quota WHERE singleton=1",null)){return c.moveToFirst()&&c.getLong(0)>=0&&c.getLong(0)<=MAX_BYTES;}
    }
    public static String[] sourceGuardsR31(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r27_live_frozen'",null)){if(!c.moveToFirst())return new String[0];}
        List<String> guards=new ArrayList<>();for(String op:new String[]{"INSERT","UPDATE","DELETE"}){
            String condition="UPDATE".equals(op)?registeredOriginal("OLD")+" OR "+registeredOriginal("NEW"):registeredOriginal("DELETE".equals(op)?"OLD":"NEW");
            guards.add("CREATE TRIGGER IF NOT EXISTS r31_improvement_frozen_"+op.toLowerCase(Locale.ROOT)+" AFTER "+op+" ON r27_live_frozen WHEN ("+condition+") BEGIN UPDATE r31_improvement_quota SET source_revision=source_revision+1 WHERE singleton=1; END");
        }return guards.toArray(new String[0]);
    }
    private static String registeredOriginal(String row){return "EXISTS(SELECT 1 FROM r31_improvement_registration r WHERE r.segment_key='R31_NATURAL_CALENDAR_BLOCK:'||CAST("+row+".target_ordinal/720 AS INTEGER) AND (r.parent_runtime_id="+row+".policy_hash OR r.child_runtime_id="+row+".policy_hash))";}
    private static long frozenRevision(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT source_revision FROM r31_improvement_quota WHERE singleton=1",null)){return c.moveToFirst()?c.getLong(0):-1;}}
    /** MIGRATION owner only; an old nonempty store cannot acquire a zero debt. */
    public static void seedWithinMigrationR31(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R31_IMPROVEMENT_MIGRATION_REQUIRED");
        try(Cursor c=db.rawQuery("SELECT 1 FROM r31_improvement_quota WHERE singleton=1",null)){if(c.moveToFirst())return;}
        for(String table:new String[]{"r31_improvement_registration","r31_improvement_cost","r31_improvement_claim"})try(Cursor c=db.rawQuery("SELECT 1 FROM "+table+" LIMIT 1",null)){if(c.moveToFirst())throw new IllegalStateException("R31_IMPROVEMENT_QUOTA_DEBT_UNKNOWN");}
        db.execSQL("INSERT INTO r31_improvement_quota VALUES(1,0,0)");
    }
    private static boolean space(SQLiteDatabase db,long bytes){try(Cursor c=db.rawQuery("SELECT used_bytes FROM r31_improvement_quota WHERE singleton=1",null)){return c.moveToFirst()&&bytes>=0&&c.getLong(0)<=MAX_BYTES-bytes;}}
    private static Map<String,Object> map(){return new LinkedHashMap<>();}
    @SuppressWarnings("unchecked") private static Map<String,Object> parse(String s){return (Map<String,Object>)JsonR27.parse(s);}
    private static long number(Map<?,?> m,String k){Object v=m.get(k);if(!(v instanceof Number))throw new IllegalStateException("R31_IMPROVEMENT_NUMBER:"+k);return ((Number)v).longValue();}
    private static String segment(long ordinal){return "R31_NATURAL_CALENDAR_BLOCK:"+Math.floorDiv(ordinal,(long)WINDOW);}
    static Map<String,Object> selection(EvolutionLoopR27 loop){
        Map<String,Object> m=map();m.put("protocol",VERSION);m.put("selectionOwner","EvolutionLoopR27 original TRAIN/SELECT, no CONFIRM optimizer input");m.put("sourceCommit",loop.sourceCommit);m.put("semanticAbi",FrozenConfirmationR30.currentSemantics());m.put("datasetHash",loop.context().data.datasetHash);m.put("split",loop.context().split.describe());m.put("generation",loop.generation);
        EvolutionLoopR27.Config c=loop.cfg;m.put("seed",c.seed);m.put("births",c.births);m.put("populationCap",c.populationCap);m.put("creationCostUnits",c.creationCostUnits);m.put("creationSliceUnits",c.creationSliceUnits);m.put("mutationShare",c.mutationShare);m.put("crossoverShare",c.crossoverShare);m.put("fusionShare",c.fusionShare);m.put("expertShare",c.expertShare);m.put("nullShare",c.nullShare);m.put("selectionStateSha256",StatsR27.sha256(loop.snapshot()));return m;
    }
    /** Keep the existing historical CONFIRM result. It cannot prove a natural
     * predraw timestamp or grant a new prospective improvement declaration. */
    static Map<String,Object> outerReceipt(FrozenConfirmationR30.Task task,EvalContextR27 ctx,ReplayEvaluatorR27.Trace child,ReplayEvaluatorR27.Trace parent){
        Map<String,Object> m=map();m.put("version",VERSION);m.put("status","PROVISIONAL");m.put("evidenceRole","INDEPENDENT_HISTORICAL_CONFIRM_NOT_PROSPECTIVE");m.put("reason",parent==null?"NO_PARENT_FOR_PAIRED_COMPARISON":"HISTORICAL_REPLAY_HAS_NO_NATURAL_PREOUTCOME_FREEZE_OR_ACTUAL_PAIR_CPU_COST");m.put("taskKey",task.key);m.put("packetSha256",StatsR27.sha256(task.json()));m.put("parentHash",task.previous==null?null:task.previous.policyHash);m.put("childHash",task.champion.policyHash);m.put("datasetHash",task.datasetHash);m.put("selectionProcessHash",task.selectionProcessR31==null?null:StatsR27.sha256(JsonR27.write(task.selectionProcessR31)));m.put("windowFrom",ctx.data.ordinals[task.from]);m.put("windowTo",ctx.data.ordinals[task.to-1]);m.put("expectedCalendarOpportunities",ctx.data.ordinals[task.to-1]-ctx.data.ordinals[task.from]+1);m.put("observedDatasetRows",task.to-task.from);m.put("childTraceSha256",StatsR27.sha256(FrozenConfirmationStoreR30.encode(task,child)));m.put("parentTraceSha256",parent==null?null:StatsR27.sha256(FrozenConfirmationStoreR30.encode(task,parent)));m.put("traceOriginals","r30_confirm_trace keyed by experiment_id,dataset_hash,block_id; packet original r30_frozen_confirm; outcomes original r30_confirm_dataset");m.put("packetCreatedAt",task.frozenAt);m.put("predictionFrozenAt",null);m.put("actualCostCpuNanos",null);m.put("championReplacedByThisReceipt",false);m.put("formalAuthorityChanged",false);
        if(parent!=null)m.put("pairedSelectCompatibleDescription",VerifiedImprovementR31.developmentReceipt(task.previous.policyHash,task.champion.policyHash,parent,child,task.from,task.to));return Collections.unmodifiableMap(m);
    }
    /** Add to the original frozen packet transaction. Missing optional schema,
     * quota, unknown old selection binding or no parent never stalls CONFIRM. */
    public static void appendRegistration(ResearchWriteQueueR27.Batch batch,FrozenConfirmationR30.Task task){
        if(task.previous==null||task.selectionProcessR31==null)return;
        batch.guardAfter(db->{if(!ready(db))return 0;registerWithin(db,task,System.currentTimeMillis());return 0;});
    }
    static void registerWithin(SQLiteDatabase db,FrozenConfirmationR30.Task task,long at){
        if(!db.inTransaction())throw new IllegalStateException("R31_IMPROVEMENT_REGISTRATION_WRITER_REQUIRED");
        if(task.previous==null||task.selectionProcessR31==null||!ready(db))return;
        String issue;long open;int[] numbers=new int[10];
        try(Cursor c=db.rawQuery("SELECT issue,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(!c.moveToFirst()||c.isNull(1)||c.getLong(1)<=0)return;issue=c.getString(0);open=c.getLong(1);for(int i=0;i<10;i++)numbers[i]=c.getInt(i+2);}
        if(open>at||!TargetIssueResolver.isValid(issue))return;Models.Draw head=new Models.Draw();head.issue=issue;head.openTime=open;head.numbers=numbers;if(!head.isValid())return;long first=TargetIssueResolver.firstFutureOrdinalSU1(head,at),block=Math.floorDiv(first,(long)WINDOW)+1,from=Math.multiplyExact(block,(long)WINDOW),to=from+WINDOW-1;
        Map<String,Object> m=map();m.put("version",VERSION);m.put("sourceCommit",task.sourceCommit);m.put("semanticAbi",task.researchSemanticHash);m.put("taskKey",task.key);m.put("packetSha256",StatsR27.sha256(task.json()));m.put("selectionProcess",task.selectionProcessR31);m.put("selectionProcessHash",StatsR27.sha256(JsonR27.write(task.selectionProcessR31)));m.put("parentPolicy",task.previous.spec().fullJson());m.put("parentTau",task.previous.temperature);m.put("childPolicy",task.champion.spec().fullJson());m.put("childTau",task.champion.temperature);m.put("parentHash",task.previous.policyHash);m.put("childHash",task.champion.policyHash);m.put("parentRuntimeId",pairId(segment(from),"P",task.previous.policyHash));m.put("childRuntimeId",pairId(segment(from),"C",task.champion.policyHash));m.put("windowFrom",from);m.put("windowTo",to);m.put("registeredAt",at);m.put("sourceHead",issue);m.put("sourceRevision",SchemaR27.liveSourceRevision(db));m.put("role","NATURAL_FUTURE_OBSERVER_ONLY");m.put("errorBudget",.05);m.put("costRule","ACTUAL_CALLING_THREAD_CPU_NANOS; CHILD_TOTAL_LE_PARENT_TOTAL; NO_UTILITY_PRICE_INFERRED");m.put("opportunityRule","ALL_720_CALENDAR_ORDINALS; UNKNOWN_ROWS_NEVER_ZERO");m.put("noNewShadowOrRealPermission",true);
        String json=JsonR27.write(m);if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>32768||!space(db,json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length+1024))return;String key=segment(from),experiment=StatsR27.sha256(VERSION+"|"+key+"|"+StatsR27.sha256(json));
        db.execSQL("INSERT OR IGNORE INTO r31_improvement_registration VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{key,experiment,task.key,m.get("parentRuntimeId"),m.get("childRuntimeId"),from,to,at,json,StatsR27.sha256(json)});
    }
    public static final String PAIR_STATUS="OBSERVE_PAIR_R31",PAIR_RETIRED="OBSERVE_PAIR_R31_RETIRED";
    static String pairId(String segment,String arm,String policy){return StatsR27.sha256(VERSION+"|"+segment+"|"+arm+"|"+policy);}
    /** Reserve from the existing24 exploration slots, after its original
     * protected confirmation queue. No qualification/alpha slot is acquired. */
    public static int reservedObserverSlots(SQLiteDatabase db){
        if(!ready(db))return 0;String latest=RuntimeR27.latestIssue(db);if(latest==null)return 0;long head=TargetIssueResolver.sequenceOrdinalSU1(latest);
        try(Cursor c=db.rawQuery("SELECT registration_json FROM r31_improvement_registration r WHERE window_to>=? AND NOT EXISTS(SELECT 1 FROM r31_improvement_claim c WHERE c.segment_key=r.segment_key) ORDER BY window_from,segment_key LIMIT 1",new String[]{Long.toString(head)})){return c.moveToFirst()&&currentRegistration(parse(c.getString(0)))?2:0;}
    }
    private static boolean currentRegistration(Map<String,Object> r){return VERSION.equals(r.get("version"))&&BuildConfig.SOURCE_COMMIT_R26.equals(r.get("sourceCommit"))&&FrozenConfirmationR30.currentSemantics().equals(r.get("semanticAbi"));}
    private static int occupiedObserverSlots(SQLiteDatabase db,String parent,String child){
        String sql="SELECT count(*) FROM r27_live_shadow s WHERE s.policy_hash NOT IN (?,?) AND (s.status IN ('OBSERVE','OBSERVE_REPAIR1',?) OR (s.status IN ('ACTIVE','ACTIVE_REPAIR1','OBSERVE','OBSERVE_RETIRED','OBSERVE_REPAIR1','OBSERVE_REPAIR1_RETIRED') AND EXISTS(SELECT 1 FROM r27_eprocess e WHERE e.policy_hash=s.policy_hash AND e.global_index>0 AND e.state NOT IN ('"+SequentialQualificationR27.QUALIFIED+"','"+SequentialQualificationR27.NOT_BETTER+"'))))";
        try(Cursor c=db.rawQuery(sql,new String[]{parent,child,PAIR_STATUS})){return c.moveToFirst()?c.getInt(0):RuntimeR27.OBSERVE_CAP;}
    }
    static boolean routePair(SQLiteDatabase read,Writer writer){
        String latest=RuntimeR27.latestIssue(read);if(latest==null)return false;long head=TargetIssueResolver.sequenceOrdinalSU1(latest);String json=null;
        try(Cursor c=read.rawQuery("SELECT registration_json FROM r31_improvement_registration r WHERE window_to>=? AND NOT EXISTS(SELECT 1 FROM r31_improvement_claim c WHERE c.segment_key=r.segment_key) ORDER BY window_from,segment_key LIMIT 1",new String[]{Long.toString(head)})){if(c.moveToFirst())json=c.getString(0);}
        Map<String,Object> reg=json==null?null:parse(json);if(reg!=null&&!currentRegistration(reg))reg=null;
        final Map<String,Object> desired=reg;final String parent=reg==null?"":(String)reg.get("parentRuntimeId"),child=reg==null?"":(String)reg.get("childRuntimeId");
        boolean retire=false;try(Cursor c=read.rawQuery("SELECT 1 FROM r27_live_shadow WHERE status=? AND policy_hash NOT IN (?,?) LIMIT 1",new String[]{PAIR_STATUS,parent,child})){retire=c.moveToFirst();}
        int present=0;try(Cursor c=read.rawQuery("SELECT count(*) FROM r27_live_shadow WHERE status=? AND policy_hash IN (?,?)",new String[]{PAIR_STATUS,parent,child})){if(c.moveToFirst())present=c.getInt(0);}
        if(!retire&&(reg==null||present==2))return false;
        int occupied=occupiedObserverSlots(read,parent,child);
        // The original observer publication first retires exploration to make
        // room. Never evict a qualification queue or expand the original cap.
        final boolean activate=reg!=null&&occupied+2<=RuntimeR27.OBSERVE_CAP;
        if(!retire&&!activate)return false;ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        b.guardBefore(db->{if(!ready(db))return -1;if(activate&&occupiedObserverSlots(db,parent,child)+2>RuntimeR27.OBSERVE_CAP)return -1;return 0;});
        b.add("UPDATE r27_live_shadow SET status=?,reason='PAIRED_SEGMENT_ENDED_OR_INACTIVE',updated_at=? WHERE status=? AND policy_hash NOT IN (?,?)",PAIR_RETIRED,System.currentTimeMillis(),PAIR_STATUS,parent,child);
        if(activate)for(String arm:new String[]{"parent","child"}){
            String id=(String)reg.get(arm+"RuntimeId"),policy=(String)reg.get(arm+"Policy");double tau=((Number)reg.get(arm+"Tau")).doubleValue();String base=(String)reg.get(arm+"Hash");PolicySpecR27 spec=PolicySpecR27.parse(policy);if(!spec.policyHash().equals(base)||!Double.isFinite(tau)||tau<=0)throw new IllegalStateException("R31_IMPROVEMENT_OBSERVER_BINDING");
            b.add("INSERT OR IGNORE INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi) VALUES(?,?,?,?,0,?,'PROSPECTIVE_PAIRED_OBSERVER_NO_ALPHA',?,?,-1,?,?)",id,policy,tau,StoreR27.pack(new double[10]),PAIR_STATUS,System.currentTimeMillis(),System.currentTimeMillis(),base,reg.get("semanticAbi"));
        }
        writer.commit("PAIRED_OBSERVER_SUBSCRIPTION",b);return true;
    }
    /** Used by the original freeze's observer lane. Outside the immutable
     * registered future window a pair consumes no numerical work or rows. */
    public static boolean permitsPairFreeze(SQLiteDatabase db,String id,long target){
        if(!ready(db))return false;try(Cursor c=db.rawQuery("SELECT registration_json FROM r31_improvement_registration WHERE segment_key=? AND window_from<=? AND window_to>=? AND (parent_runtime_id=? OR child_runtime_id=?)",new String[]{segment(target),Long.toString(target),Long.toString(target),id,id})){return c.moveToFirst()&&currentRegistration(parse(c.getString(0)));}
    }
    /** Optional same-freeze writer cost observer. The before guard proves this
     * transaction created the original; missing schema never rejects a freeze. */
    public static void appendFreezeCost(SQLiteDatabase read,ResearchWriteQueueR27.Batch batch,String policy,long target,long cpuNanos){
        if(cpuNanos<0||!ready(read))return;final String key=segment(target);
        try(Cursor c=read.rawQuery("SELECT 1 FROM r31_improvement_registration WHERE segment_key=? AND window_from<=? AND window_to>=? AND (parent_runtime_id=? OR child_runtime_id=?)",new String[]{key,Long.toString(target),Long.toString(target),policy,policy})){if(!c.moveToFirst())return;}
        final boolean[] absent={false};
        batch.guardBefore(db->{if(!ready(db))return 0;try(Cursor c=db.rawQuery("SELECT 1 FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new String[]{policy,Long.toString(target)})){absent[0]=!c.moveToFirst();}return 0;});
        batch.guardAfter(db->{if(!absent[0]||!ready(db)||!space(db,512))return 0;String hash=freezeHash(db,policy,target);if(hash==null)return 0;db.execSQL("INSERT OR IGNORE INTO r31_improvement_cost VALUES(?,?,?,?,?)",new Object[]{policy,target,cpuNanos,hash,System.currentTimeMillis()});return 0;});
    }
    private static final String FREEZE_COLUMNS="target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at";
    static String freezeHash(SQLiteDatabase db,String policy,long target){try(Cursor c=db.rawQuery("SELECT "+FREEZE_COLUMNS+" FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=? AND length(probabilities)=80",new String[]{policy,Long.toString(target)})){if(!c.moveToFirst())return null;return freezeHash(c,0,policy,target);}}
    private static String freezeHash(Cursor c,int off,String policy,long target){if(c.isNull(off+2))return null;List<Object> row=new ArrayList<>();row.add(policy);row.add(target);for(int i=0;i<10;i++)row.add(c.isNull(off+i)?null:i==2?StatsR27.sha256(c.getBlob(off+i)):c.getString(off+i));return StatsR27.sha256(JsonR27.write(row));}
    static final class Prepared {final String segment,registration,claim,evidence;final long revision,frozenRevision;Prepared(String s,String r,String c,String e,long v,long f){segment=s;registration=r;claim=c;evidence=e;revision=v;frozenRevision=f;}}
    /** Called after the old CONFIRM work, once per original worker slice. At most
     * one720-row read and one immutable claim commit; no numerical work in writer. */
    public static boolean advance(SQLiteDatabase read,Writer writer){
        if(!ready(read))return false;if(routePair(read,writer))return true;String key,json,hash;long to;
        try(Cursor c=read.rawQuery("SELECT r.segment_key,r.registration_json,r.registration_hash,r.window_to FROM r31_improvement_registration r WHERE NOT EXISTS(SELECT 1 FROM r31_improvement_claim c WHERE c.segment_key=r.segment_key) ORDER BY r.window_to,r.segment_key LIMIT 1",null)){if(!c.moveToFirst())return false;key=c.getString(0);json=c.getString(1);hash=c.getString(2);to=c.getLong(3);}
        String latest;try(Cursor c=read.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(!c.moveToFirst())return false;latest=c.getString(0);}if(TargetIssueResolver.sequenceOrdinalSU1(latest)<=to)return false;
        MemoryBudgetR31.Admission admitted=MemoryBudgetR31.processBudget().tryAcquire("prospective-improvement-prepare",MemoryBudgetR31.Kind.RESEARCH,8L<<20,()->Thread.currentThread().isInterrupted());if(!admitted.acquired())return false;
        try(MemoryBudgetR31.Lease memory=admitted.lease){Prepared p=prepare(read,key,json,hash);if(p==null)return false;
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();batch.guardBefore(db->{if(!ready(db)||SchemaR27.liveSourceRevision(db)!=p.revision||frozenRevision(db)!=p.frozenRevision||!space(db,p.claim.getBytes(java.nio.charset.StandardCharsets.UTF_8).length+512))return -1;try(Cursor c=db.rawQuery("SELECT registration_hash FROM r31_improvement_registration WHERE segment_key=?",new String[]{p.segment})){return c.moveToFirst()&&p.registration.equals(c.getString(0))?0:-1;}});
        batch.add("INSERT OR IGNORE INTO r31_improvement_claim VALUES(?,?,?,?,?,?,?)",p.segment,p.claim,StatsR27.sha256(p.claim),p.evidence,p.revision,p.frozenRevision,System.currentTimeMillis());writer.commit("PROSPECTIVE_PAIRED_IMPROVEMENT",batch);return true;}
    }
    public interface Writer {void commit(String operation,ResearchWriteQueueR27.Batch batch);}
    private static String columns(String alias){StringBuilder s=new StringBuilder();for(String column:FREEZE_COLUMNS.split(",")){if(s.length()>0)s.append(',');if(column.equals("probabilities"))s.append("CASE WHEN length(").append(alias).append(".probabilities)=80 THEN ").append(alias).append(".probabilities END");else s.append(alias).append('.').append(column);}return s.toString();}
    static Prepared prepare(SQLiteDatabase db,String key,String json,String hash){return prepareAt(db,key,json,hash,System.currentTimeMillis());}
    // Explicit synthetic-clock adapter for native protocol fixtures; production uses prepare().
    static Prepared prepareAt(SQLiteDatabase db,String key,String json,String hash,long asOf){
        if(!StatsR27.sha256(json).equals(hash))throw new IllegalStateException("R31_IMPROVEMENT_REGISTRATION_HASH");Map<String,Object> reg=parse(json);long from=number(reg,"windowFrom"),to=number(reg,"windowTo"),registered=number(reg,"registeredAt");if(to-from+1!=WINDOW||!segment(from).equals(key)||Math.floorMod(from,(long)WINDOW)!=0)throw new IllegalStateException("R31_IMPROVEMENT_WINDOW");
        String parent=(String)reg.get("parentRuntimeId"),child=(String)reg.get("childRuntimeId");long revision=SchemaR27.liveSourceRevision(db),frozenRevision=frozenRevision(db);if(revision<0||frozenRevision<0)return null;
        StringBuilder values=new StringBuilder();for(long n=from;n<=to;n++){if(values.length()>0)values.append(',');values.append('(').append(n).append(",'" ).append(TargetIssueResolver.fromSequenceOrdinalSU1(n)).append("')");}
        String sql="WITH opportunity(n,issue) AS (VALUES "+values+") SELECT o.n,o.issue,"+columns("p")+",p.hit,p.settled_at,"+columns("c")+",c.hit,c.settled_at,d.open_time,d.first_seen_at,d.dirty,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10,pc.cpu_nanos,pc.freeze_hash,cc.cpu_nanos,cc.freeze_hash FROM opportunity o LEFT JOIN r27_live_frozen p ON p.policy_hash=? AND p.target_ordinal=o.n LEFT JOIN r27_live_frozen c ON c.policy_hash=? AND c.target_ordinal=o.n LEFT JOIN draws d ON d.issue=o.issue LEFT JOIN r31_improvement_cost pc ON pc.policy_hash=? AND pc.target_ordinal=o.n LEFT JOIN r31_improvement_cost cc ON cc.policy_hash=? AND cc.target_ordinal=o.n ORDER BY o.n";
        List<VerifiedImprovementR31.Opportunity> series=new ArrayList<>();List<Object> originals=new ArrayList<>();Map<String,Long> missing=new LinkedHashMap<>();long pcpu=0,ccpu=0;boolean costKnown=true;
        try(Cursor c=db.rawQuery(sql,new String[]{parent,child,parent,child})){
            while(c.moveToNext()){
                CooperativeContinuationR26.poll();long n=c.getLong(0);String issue=c.getString(1);String reason=null;
                String ph=c.isNull(2)?null:freezeHash(c,2,parent,n),ch=c.isNull(14)?null:freezeHash(c,14,child,n);List<Object> original=new ArrayList<>();original.add(n);original.add(ph);original.add(ch);for(int col:new int[]{12,13,24,25})original.add(c.isNull(col)?null:c.getString(col));for(int col=26;col<43;col++)original.add(c.isNull(col)?null:c.getString(col));originals.add(original);
                if(c.isNull(26)||c.isNull(27)||c.getLong(26)<=0||c.getLong(27)<=0||c.getInt(28)!=0)reason="DRAW_TIME_OR_ORIGINAL_UNKNOWN";
                else if(c.getLong(26)>asOf||c.getLong(27)>asOf)reason="OUTCOME_CLOCK_AFTER_OBSERVATION";
                else if(!validDraw(c))reason="INVALID_DRAW_ORIGINAL";
                else if(ph==null||ch==null)reason="MISSING_DUAL_ARM_ORIGINAL";
                else if(!issue.equals(c.getString(2))||!issue.equals(c.getString(14)))reason="TARGET_IDENTITY_MISMATCH";
                else if(!validArm(c,2,registered,Math.min(c.getLong(26),c.getLong(27)))||!validArm(c,14,registered,Math.min(c.getLong(26),c.getLong(27))))reason="NOT_REGISTERED_PREOUTCOME_OR_INVALID_ORIGINAL";
                else if(c.getInt(12)<0||c.getInt(24)<0||c.getLong(13)<=0||c.getLong(25)<=0)reason="ORIGINAL_NOT_SETTLED";
                if(reason!=null){missing.put(reason,missing.getOrDefault(reason,0L)+1);continue;}
                int pp=c.getInt(3),cp=c.getInt(15);int py=c.getInt(28+pp),cy=c.getInt(28+cp);boolean pa=c.getInt(8)==1,ca=c.getInt(20)==1;boolean pHit=hit(c,5,py),cHit=hit(c,17,cy);
                if(c.getInt(12)!=(pHit?1:0)||c.getInt(24)!=(cHit?1:0)){missing.put("SETTLEMENT_ORIGINAL_CONFLICT",missing.getOrDefault("SETTLEMENT_ORIGINAL_CONFLICT",0L)+1);continue;}
                double[] pv=StoreR27.unpack(c.getBlob(4)),cv=StoreR27.unpack(c.getBlob(16));double pm=pv[c.getInt(5)-1]+pv[c.getInt(6)-1]+pv[c.getInt(7)-1],cm=cv[c.getInt(17)-1]+cv[c.getInt(18)-1]+cv[c.getInt(19)-1];
                series.add(new VerifiedImprovementR31.Opportunity(n,Math.max(c.getLong(10),c.getLong(22)),Math.min(c.getLong(26),c.getLong(27)),pa,pHit,pm,ca,cHit,cm));
                if(c.isNull(39)||c.isNull(41)||c.getLong(39)<0||c.getLong(41)<0||!ph.equals(c.getString(40))||!ch.equals(c.getString(42))){costKnown=false;continue;}
                pcpu=Math.addExact(pcpu,c.getLong(39));ccpu=Math.addExact(ccpu,c.getLong(41));
            }
        }
        if(SchemaR27.liveSourceRevision(db)!=revision||frozenRevision(db)!=frozenRevision)return null;String evidence=StatsR27.sha256(JsonR27.write(originals));long cost=costKnown?Math.addExact(pcpu,ccpu):0;
        boolean costDominance=costKnown&&ccpu<=pcpu&&cost>0;
        VerifiedImprovementR31.Evidence e=new VerifiedImprovementR31.Evidence(StatsR27.sha256(hash+"|"+key),(String)reg.get("parentHash"),(String)reg.get("childHash"),from,to,key,(String)reg.get("selectionProcessHash"),evidence,evidence,.05,cost,costDominance?0:Double.NaN,series);
        VerifiedImprovementR31.Claim claim=VerifiedImprovementR31.record(new VerifiedImprovementR31.Ledger(),e,VerifiedImprovementR31.z(.05));Map<String,Object> out=new LinkedHashMap<>(claim.trace());
        if(!missing.isEmpty()||!costKnown||!costDominance){out.put("status","PROVISIONAL");List<Object> reasons=new ArrayList<>(claim.reasons);if(!missing.isEmpty())reasons.add("INCOMPLETE_OR_INVALID_ORIGINAL_OPPORTUNITIES");if(!costKnown)reasons.add("MISSING_MEASURED_PAIR_CPU_COST");if(costKnown&&!costDominance)reasons.add("CHILD_CPU_COST_NONINFERIORITY_NOT_ESTABLISHED");out.put("reasons",reasons);}
        out.put("version",VERSION);out.put("role","NATURAL_FUTURE_OBSERVER_ONLY_NOT_REAL_AUTHORITY");out.put("registrationHash",hash);out.put("parentRuntimeId",parent);out.put("childRuntimeId",child);out.put("expectedCalendarOpportunities",WINDOW);out.put("validPairedOpportunities",series.size());out.put("unknownOrInvalidByReason",missing);out.put("originalsHash",evidence);out.put("originalLocator","r27_live_frozen parent+child policy_hash,target_ordinal; draws.issue; r31_improvement_cost; immutable registration packet");out.put("parentCpuNanos",costKnown?pcpu:null);out.put("childCpuNanos",costKnown?ccpu:null);out.put("cpuCostScope","ACTUAL_CALLING_THREAD_ONLY_NOT_PROCESS_OR_NATIVE_TOTAL");out.put("costRule",reg.get("costRule"));out.put("championReplacedByThisClaim",false);out.put("formalAuthorityChanged",false);String encoded=JsonR27.write(out);if(encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>262144)throw new IllegalStateException("R31_IMPROVEMENT_CLAIM_BOUND");return new Prepared(key,hash,encoded,evidence,revision,frozenRevision);
    }
    private static boolean validDraw(Cursor c){int bits=0;for(int col=29;col<=38;col++){int n=c.getInt(col);if(n<1||n>10||(bits&(1<<n))!=0)return false;bits|=1<<n;}return true;}
    private static boolean hit(Cursor c,int off,int actual){return actual==c.getInt(off)||actual==c.getInt(off+1)||actual==c.getInt(off+2);}
    private static boolean validArm(Cursor c,int off,long registered,long observed){
        if(!"FROZEN".equals(c.getString(off+7))||c.getInt(off+1)<1||c.getInt(off+1)>10||c.getBlob(off+2)==null||c.getBlob(off+2).length!=80||c.getLong(off+8)<=registered||c.getLong(off+8)>=observed||c.getLong(off+9)-LiveShadowsR27.FREEZE_MARGIN_MS<c.getLong(off+8)||c.getInt(off+6)<0||c.getInt(off+6)>1)return false;
        int a=c.getInt(off+3),b=c.getInt(off+4),d=c.getInt(off+5);if(a<1||a>10||b<1||b>10||d<1||d>10||a==b||a==d||b==d)return false;double sum=0;for(double v:StoreR27.unpack(c.getBlob(off+2))){if(!Double.isFinite(v)||v<0||v>1)return false;sum+=v;}return Math.abs(sum-1)<=PitGuardR30.PROB_SUM_TOLERANCE;
    }
    /** Bounded SQL projection; reports never start/settle a comparison. */
    public static Map<String,Object> report(SQLiteDatabase db){Map<String,Object> out=map();out.put("version",VERSION);out.put("role","PROSPECTIVE_OBSERVER_NOT_CHAMPION_OR_REAL_AUTHORITY");out.put("championReplaced",false);if(!ready(db)){out.put("state","UNKNOWN_SCHEMA_OR_QUOTA");out.put("registrations",null);out.put("claims",null);return out;}
        try(Cursor c=db.rawQuery("SELECT (SELECT count(*) FROM r31_improvement_registration),(SELECT count(*) FROM r31_improvement_claim),used_bytes FROM r31_improvement_quota WHERE singleton=1",null)){if(c.moveToFirst()){out.put("registrations",c.getLong(0));out.put("claims",c.getLong(1));out.put("accountedBytes",c.getLong(2));out.put("hardByteLimit",MAX_BYTES);out.put("state",c.getLong(2)>=MAX_BYTES-262656?"RESOURCE_WAIT":"RECORDED_OR_WAITING_NATURAL_FUTURE");}}
        List<Object> claims=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT segment_key,CASE WHEN length(CAST(claim_json AS BLOB))<=262144 THEN claim_json END,claim_hash,evidence_hash,source_revision,frozen_revision,recorded_at FROM r31_improvement_claim ORDER BY recorded_at DESC,segment_key LIMIT 8",null)){while(c.moveToNext()){Map<String,Object> item=map();item.put("segment",c.getString(0));item.put("claimHash",c.getString(2));item.put("originalsHash",c.getString(3));item.put("sourceRevisionAtClaim",c.getLong(4));item.put("frozenRevisionAtClaim",c.getLong(5));item.put("recordedAt",c.getLong(6));if(c.isNull(1)||!StatsR27.sha256(c.getString(1)).equals(c.getString(2)))item.put("status","UNKNOWN_CLAIM_HASH");else{Map<String,Object> value=parse(c.getString(1));for(String key:new String[]{"status","reasons","parentHash","childHash","windowFrom","windowTo","expectedCalendarOpportunities","validPairedOpportunities","unknownOrInvalidByReason","parentActs","childActs","meanDifference","lowerBound","parentCpuNanos","childCpuNanos","originalLocator"})item.put(key,value.get(key));item.put("validationScope","IMMUTABLE_RECORDED_CLAIM; CURRENT_ORIGINALS_NOT_REHASHED_BY_REPORT");}claims.add(item);}}
        out.put("lastClaims",claims);out.put("naturalQuality","NOT_PROVEN_BY_REGISTRATION_OR_CLAIM_ALONE");return Collections.unmodifiableMap(out);}
}
