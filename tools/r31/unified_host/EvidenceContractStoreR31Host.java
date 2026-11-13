package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Real production Store/Batch on native SQLite. Injected arm records exercise
 * activation/revocation mechanics only; they are NEVER capability/power evidence. */
public final class EvidenceContractStoreR31Host {
    static int assertions;
    static void check(boolean value,String code){assertions++;if(!value)throw new AssertionError(code);}
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static void schema(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS r27_chart_cache(panel_id TEXT,key TEXT,payload_json TEXT,updated_at INTEGER,PRIMARY KEY(panel_id,key))");}
    static void execute(Connection c,SQLiteDatabase db,ResearchWriteQueueR27.Batch b)throws Exception{c.setAutoCommit(false);try{ResearchWriteQueueR27.exec(db,b);c.commit();}catch(Exception|Error failure){c.rollback();throw failure;}finally{c.setAutoCommit(true);}}
    static EvidenceContractStoreR31.Registration register(Connection c,SQLiteDatabase db,String source)throws Exception{
        ResearchWriteQueueR27.Batch first=new ResearchWriteQueueR27.Batch();check(EvidenceContractStoreR31.beforeFactory(db,first,source,1000)==null,"FIRST_SLICE_ONLY_PREPARES_REGISTRATION");execute(c,db,first);
        ResearchWriteQueueR27.Batch second=new ResearchWriteQueueR27.Batch();EvidenceContractStoreR31.Registration r=EvidenceContractStoreR31.beforeFactory(db,second,source,1001);check(r!=null,"COMMITTED_REGISTRATION_READ_AFTER_RESTART");return r;
    }
    static Map<String,Object> artifact(EvidenceContractStoreR31.Registration r,String arm,long seed,String status){
        Map<String,Object> ledger=new LinkedHashMap<>();ledger.put("INJECTED_NATIVE_SQLITE_MECHANICS_ONLY",Collections.singletonList("NOT_FACTORY_OR_NATURAL_EVIDENCE"));
        Map<String,Object> a=new LinkedHashMap<>();a.put("sourceCommit",r.source);a.put("protocolHash",r.protocolHash);a.put("namespace",FactoryCapabilityExperimentR31.NAMESPACE);a.put("arm",arm);a.put("seed",seed);a.put("status",status);a.put("realPermission",false);a.put("naturalQualityEligible",false);a.put("candidateLedger",ledger);a.put("candidateLedgerSha256",StatsR27.sha256(JsonR27.write(ledger)));return a;
    }
    static Map<String,Object> summary(EvidenceContractStoreR31.Registration r,int count,int failures){Map<String,Object> m=new LinkedHashMap<>();m.put("sourceCommit",r.source);m.put("protocolHash",r.protocolHash);m.put("completedTrials",count);m.put("failedTrials",failures);m.put("status",count==120?"COMPLETE_MEASURED_REQUIRES_REGISTERED_POWER_ASSESSMENT":"RUNNING_INCOMPLETE");m.put("meetsRegisteredExperiment",true);return m;}
    static Map<String,Object> fill(Connection c,SQLiteDatabase db,EvidenceContractStoreR31.Registration r,boolean strong)throws Exception{
        int count=0;Map<String,Object> last=null;
        for(ResearchCapabilityExperimentR31.Arm arm:ResearchCapabilityExperimentR31.Arm.values())for(long seed=710000;seed<710020;seed++){
            Map<String,Object> a=artifact(r,arm.name(),seed,strong&&arm==ResearchCapabilityExperimentR31.Arm.SIGNAL?"DETECTED":"NOT_DETECTED");String encoded=FactoryStateCodecR31.encode(JsonR27.write(a));
            ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();last=EvidenceContractStoreR31.afterFactory(db,batch,r,summary(r,++count,0),a,encoded,2000+count);execute(c,db,batch);
        }
        check(count==120,"FULL_SIX_ARMS_TWENTY_UNIQUE_SEEDS_FIXTURE");return last;
    }
    static void fixedFuture(){
        int n=FactoryCapabilityExperimentR31.FUTURE;long[] ordinals=new long[n];ReplayEvaluatorR27.Trace trace=new ReplayEvaluatorR27.Trace(n,0,n,3,1);for(int i=0;i<n;i++){ordinals[i]=10000+i;trace.skipped[i]=false;trace.act[i]=true;trace.hit[i]=1;}
        SequentialQualificationR27.State early=new SequentialQualificationR27.State("INJECTED_FIXED_WINDOW",1,.15);Map<String,Object> observed=FactoryCapabilityExperimentR31.observeFixedFuture(early,trace,ordinals,0,n);
        check(SequentialQualificationR27.QUALIFIED.equals(early.state),"EARLY_QUALIFICATION_EXISTS");check(((Number)observed.get("firstBoundaryActs")).longValue()<CertificateGateR27.MIN_ACTS,"ORIGINAL_BUG_QUALIFIED_BEFORE_MIN_SUPPORT");check(early.n==n&&early.acts==n,"ALL_6000_OPPORTUNITIES_AND_SUPPORT_AFTER_BOUNDARY");check(Boolean.FALSE.equals(observed.get("earlyStopping")),"FIXED_WINDOW_RULE_REPORTED");
        SequentialQualificationR27.State rejected=new SequentialQualificationR27.State("INJECTED_REJECTED_FIXED_WINDOW",1,.15);for(int i=0;i<1000;i++)trace.hit[i]=0;FactoryCapabilityExperimentR31.observeFixedFuture(rejected,trace,ordinals,0,n);check(rejected.n==n&&rejected.acts==n,"NULL_TERMINAL_STILL_HAS_COMPLETE_WINDOW");check(SequentialQualificationR27.NOT_BETTER.equals(rejected.state),"FIRST_NULL_REJECTION_CANNOT_REVIVE_FROM_LATER_HITS");
        check(FactoryCapabilityExperimentR31.VERSION.equals("R31_FULL_FACTORY_EXPERIMENT_2_FIXED_FUTURE"),"NEW_PROTOCOL_DOES_NOT_REJUDGE_OLD_EXPERIMENTS");
        boolean range=false;try{FactoryCapabilityExperimentR31.observeFixedFuture(new SequentialQualificationR27.State("INJECTED_BAD_WINDOW",1,.15),trace,ordinals,0,100);}catch(IllegalArgumentException expected){range=true;}check(range,"RESULT_DEPENDENT_OR_SHORT_WINDOW_REFUSED");
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");
        if(args.length==2&&args[0].startsWith("crash-")){
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+args[1])){SQLiteDatabase db=new SQLiteDatabase(c);ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();FactoryCapabilityStoreR31.advance(db,b,"CONTROLLED_CRASH_NAMESPACE",1000,true);c.setAutoCommit(false);ResearchWriteQueueR27.exec(db,b);if(args[0].equals("crash-before"))Runtime.getRuntime().halt(91);c.commit();Runtime.getRuntime().halt(92);}return;
        }
        fixedFuture();Path file=Files.createTempFile("r31-contract-kill-",".db");
        try{
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file)){schema(new SQLiteDatabase(c));}
            for(String phase:Arrays.asList("crash-before","crash-after")){
                Process p=new ProcessBuilder(Paths.get(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("java.class.path"),EvidenceContractStoreR31Host.class.getName(),phase,file.toString()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();int exit=p.waitFor();check(exit==(phase.equals("crash-before")?91:92),"ACTUAL_PROCESS_HALT_"+phase);
                try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file)){SQLiteDatabase db=new SQLiteDatabase(c);check(Integer.parseInt(scalar(db,"SELECT count(*) FROM r27_chart_cache WHERE panel_id='r31-risk-contract'"))==(phase.equals("crash-before")?0:1),"SQLITE_REOPEN_REGISTRATION_ATOMIC_"+phase);check("0".equals(scalar(db,"SELECT count(*) FROM r27_chart_cache WHERE panel_id='r31-factory'")),"NO_FIRST_FACTORY_STEP_BEFORE_COMMITTED_REGISTRATION");}
            }
            try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file)){SQLiteDatabase db=new SQLiteDatabase(c);ResearchWriteQueueR27.Batch next=new ResearchWriteQueueR27.Batch();FactoryCapabilityStoreR31.advance(db,next,"CONTROLLED_CRASH_NAMESPACE",1001,false);execute(c,db,next);check("1".equals(scalar(db,"SELECT count(*) FROM r27_chart_cache WHERE panel_id='r31-factory'")),"ACTUAL_FACTORY_OWNER_RESUMES_AFTER_REGISTRATION");}
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(Paths.get(file+"-journal"));}
        try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(c);schema(db);EvidenceContractStoreR31.Registration weak=register(c,db,"CONTROLLED_WEAK_NATIVE_STORE_NOT_POWER_EVIDENCE");Map<String,Object> low=fill(c,db,weak,false);
            check("OBSERVE_ONLY".equals(low.get("state")),"POWER_ZERO_CANNOT_ACTIVATE");check(Boolean.FALSE.equals(low.get("validationPassed")),"UNTRUSTED_SUMMARY_TRUE_CANNOT_OVERRIDE_MEASURED_POWER");check(((Number)low.get("signalPowerLower95")).doubleValue()==0,"RECOMPUTED_SIGNAL_POWER_ZERO");check(Boolean.FALSE.equals(low.get("realPermission")),"NO_REAL_FROM_SYNTHETIC_OR_STORE_TESTS");
            Map<String,Object> wrong=artifact(weak,"SIGNAL",710000,"DETECTED");wrong.put("sourceCommit","FOREIGN");boolean foreign=false;try{EvidenceContractStoreR31.trial(weak,wrong,"fixture");}catch(IllegalArgumentException expected){foreign=true;}check(foreign,"FOREIGN_SOURCE_CANNOT_VALIDATE_CURRENT_REGISTRATION");wrong.put("sourceCommit",weak.source);wrong.put("candidateLedgerSha256",StatsR27.sha256("CORRUPT"));boolean ledger=false;try{EvidenceContractStoreR31.trial(weak,wrong,"fixture");}catch(IllegalArgumentException expected){ledger=true;}check(ledger,"CHANGED_ORIGINAL_SEARCH_LEDGER_CANNOT_VALIDATE");
            Map<String,Object> last=artifact(weak,"GAPS",710019,"NOT_DETECTED");ResearchWriteQueueR27.Batch duplicate=new ResearchWriteQueueR27.Batch();EvidenceContractStoreR31.afterFactory(db,duplicate,weak,summary(weak,120,0),last,FactoryStateCodecR31.encode(JsonR27.write(last)),2200);execute(c,db,duplicate);check(EvidenceContractStoreR31.trials(db,weak).size()==120,"REPEATED_TRIAL_IS_NOT_DOUBLE_COUNTED");
            ResearchWriteQueueR27.Batch stale=new ResearchWriteQueueR27.Batch();EvidenceContractStoreR31.afterFactory(db,stale,weak,summary(weak,120,0),null,null,2201);String changed=EvidenceContractStoreR31.trials(db,weak).keySet().iterator().next();db.execSQL("UPDATE r27_chart_cache SET payload_json='{}' WHERE panel_id=? AND key=?",new Object[]{EvidenceContractStoreR31.TRIAL,changed});check(stale.before.call(db)<0,"TRIAL_CHANGE_AFTER_PREPARE_REJECTS_WHOLE_BATCH");boolean corrupt=false;try{EvidenceContractStoreR31.afterFactory(db,new ResearchWriteQueueR27.Batch(),weak,summary(weak,120,0),null,null,2202);}catch(IllegalArgumentException expected){corrupt=true;}check(corrupt,"CHANGED_COMPACT_RECORD_CANNOT_COUNT");
        }
        try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(c);schema(db);EvidenceContractStoreR31.Registration strong=register(c,db,"CONTROLLED_POSITIVE_NATIVE_STORE_NOT_POWER_EVIDENCE");Map<String,Object> high=fill(c,db,strong,true);check("ACTIVE".equals(high.get("state")),"CONTROLLED_DESIGN_ACTIVATION_PATH_REACHABLE");check(Boolean.FALSE.equals(high.get("realPermission")),"DESIGN_ACTIVATION_NEVER_CHANGES_REAL_AUTHORITY");
            ResearchWriteQueueR27.Batch revoke=new ResearchWriteQueueR27.Batch();EvidenceContractStoreR31.revoke(db,revoke,strong.binding,"RISK_BREACH",3000);execute(c,db,revoke);check("REVOKED".equals(EvidenceContractStoreR31.report(db).get("state")),"READONLY_REPORT_CONSUMES_DURABLE_REVOCATION");ResearchWriteQueueR27.Batch retry=new ResearchWriteQueueR27.Batch();Map<String,Object> after=EvidenceContractStoreR31.afterFactory(db,retry,strong,summary(strong,120,0),null,null,3001);execute(c,db,retry);check("REVOKED".equals(after.get("state")),"HEALTH_RETRY_CANNOT_REACTIVATE_REVOKED_CONTRACT");
            EvidenceContractStoreR31.Registration replacement=register(c,db,"CONTROLLED_NEW_SOURCE");check(!replacement.binding.equals(strong.binding),"SOURCE_REPLACEMENT_NEW_EXPERIMENT_IDENTITY");check(EvidenceContractStoreR31.read(db,EvidenceContractStoreR31.REG,strong.binding)!=null,"OLD_ORIGINAL_REGISTRATION_PRESERVED");
            db.execSQL("CREATE TABLE r27_eprocess(policy_hash TEXT,global_index INTEGER)");db.execSQL("INSERT INTO r27_eprocess VALUES('controlled',1)");db.execSQL("CREATE TABLE r27_live_frozen(policy_hash TEXT,target_ordinal INTEGER,status TEXT,act INTEGER,PRIMARY KEY(policy_hash,target_ordinal))");db.execSQL("INSERT INTO r27_live_frozen VALUES('controlled',10,'FROZEN',1),('controlled',12,'ABSTAIN_SET_SUPPORT',0),('controlled',13,'SKIPPED_GAP',0),('controlled',15,'DEADLINE_MISSED',0)");
            @SuppressWarnings("unchecked") Map<String,Object> opportunity=(Map<String,Object>)EvidenceContractStoreR31.report(db).get("opportunities");check(((Number)opportunity.get("eligible")).longValue()==2&&((Number)opportunity.get("act")).longValue()==1&&((Number)opportunity.get("abstain")).longValue()==1,"ACT_ABSTAIN_CANONICAL_OPPORTUNITY_DENOMINATOR");check(((Number)opportunity.get("missedInput")).longValue()==1&&((Number)opportunity.get("missedDeadline")).longValue()==1&&((Number)opportunity.get("unknown")).longValue()==2,"MISSED_AND_UNRECORDED_ORDINALS_NOT_SILENTLY_REMOVED");
            db.execSQL("UPDATE r27_chart_cache SET payload_json='{}' WHERE panel_id=? AND key='CURRENT'",new Object[]{EvidenceContractStoreR31.STATE});check("UNKNOWN_REGISTRY_UNAVAILABLE_OR_CORRUPT".equals(EvidenceContractStoreR31.report(db).get("state")),"CORRUPT_REPORT_STATE_EXPLICIT_UNKNOWN");
        }
        System.out.println("EVIDENCE_CONTRACT_STORE_NATIVE_PASS assertions="+assertions+" actualFactoryOwner=YES actualBatch=YES realSQLiteKillReopen=YES injectedTrials=MECHANICS_ONLY naturalQuality=NOT_PROVEN");
    }
}
