package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/** Synthetic ledger fixtures against actual product classes. No production overlays,
 * natural Future evidence, quality claims, or wager authorization. */
public final class Batch34EpochAcceptanceR79 {
    private static int checks;
    private static String issue(int n) { return String.format(Locale.ROOT,"202610120%03d",n); }
    private static String hash(String value) { return V51ChecksumEngine.sha256(value); }
    private static void ok(boolean value,String message) {
        if(!value)throw new AssertionError(message); checks++; System.out.println("PASS "+message);
    }
    private static long count(SQLiteDatabase db,String sql) {
        try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}
    }
    private static Object field(Object object,String name)throws Exception {
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    private static Object invoke(String method,Class<?>[] types,Object...arguments)throws Exception {
        Method m=Batch34ChallengerCertificateBridgeR930.class.getDeclaredMethod(method,types);
        m.setAccessible(true);return m.invoke(null,arguments);
    }
    private static Object metrics(DbStore store,String id)throws Exception {
        return invoke("freshMetrics",new Class<?>[]{DbStore.class,String.class,String.class,String.class},store,id,issue(200),issue(219));
    }
    private static void fresh(DbStore store)throws Exception {
        invoke("syncFreshCertificates",new Class<?>[]{DbStore.class,String.class},store,issue(219));
    }
    private static void mirror(DbStore store,int target)throws Exception {
        invoke("mirrorPredrawApplications",new Class<?>[]{DbStore.class,String.class},store,issue(target));
    }
    private static void freeze(SQLiteDatabase db,AlgorithmGenomeR908 g,int n) {
        StringBuilder features=new StringBuilder("R908F1");
        for(int i=0;i<AlgorithmGenomeR908.INPUTS;i++)features.append("|0");
        String input=hash("R908_ALG_FEATURE|"+features),record=hash("R79_BATCH34_SYNTHETIC|"+n);
        AlgorithmProofRecoveryR948.requireCleanForFreeze(db,g.genomeId);
        db.execSQL("INSERT INTO "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                +"(genome_id,target_issue,position,n1,n2,n3,selected_ordinal,score,feature_blob,feature_hash,frozen_at,record_hash) VALUES(?,?,1,1,2,3,0,.3,?,?,1,?)",
                new Object[]{g.genomeId,issue(n),features.toString(),input,record});
        db.execSQL("INSERT INTO "+DbHelper.TABLE_R817_RESEARCH_PREDRAW
                +"(candidate_id,target_issue,phase,position,n1,n2,n3,selected_ordinal,score,probability,formula_hash,input_hash,frozen_at,record_hash) VALUES(?,?,'R908_INVENTED_ALGORITHM_FUTURE_OOS',1,1,2,3,0,.3,-1,?,?,1,?)",
                new Object[]{g.genomeId,issue(n),g.structuralHash,input,record});
        if(n==206)return; // Explicit old row never had an execution proof or resource receipt.
        CandidateExecutionAuditR932.Builder audit=new CandidateExecutionAuditR932.Builder();
        for(int i=0;i<1200;i++)audit.add(i,.3,.3,i);
        String state=AlgorithmGenomeVmR908.initialState(g).encode();
        audit.finish(0,g.canonical(),g.canonical(),state,state,state).freeze(db,"algorithm:"+g.genomeId,g.structuralHash,input,issue(n-1),issue(n),record);
        ResearchCaptureLedgerR932.record(db,issue(n),"algorithm:"+g.genomeId,"FROZEN","R79_BATCH34_SYNTHETIC",record,1,true);
    }
    private static void draw(SQLiteDatabase db,int n) {
        long opened=System.currentTimeMillis()+1;while(System.currentTimeMillis()<=opened)Thread.yield();
        db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{issue(n),opened,opened+1});
    }
    private static void epoch(SQLiteDatabase db,String id,int start) {
        db.execSQL("UPDATE "+AlgorithmProofRecoveryR948.HEAD+" SET status='RESET_CLEAN',epoch_start_issue=?,evidence_hash=? WHERE genome_id=?",
                new Object[]{issue(start),hash("R79_EPOCH_"+start),id});
    }
    private static long support(SQLiteDatabase db,String id) {
        return count(db,"SELECT future_support FROM "+Batch34SchemaR930.TABLE_LIFECYCLE+" WHERE algorithm_id='"+id+"'");
    }
    private static long mirrors(SQLiteDatabase db) {
        return count(db,"SELECT COUNT(*) FROM "+Batch34SchemaR930.TABLE_EVIDENCE_LINK+" WHERE evidence_type='LEGACY_PREDRAW_OBSERVATION'");
    }
    public static void main(String[] args)throws Exception {
        try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])) {
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(79340,1,"R79_BATCH34_SYNTHETIC");String id=g.genomeId;
            db.beginTransaction();try {
                Batch34SchemaR930.ensure(db);AlgorithmProofRecoveryR948.schema(db);
                for(int n=1;n<200;n++)db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{issue(n),System.currentTimeMillis()-20000000L+n*60000L,System.currentTimeMillis()-20000000L+n*60000L+1});
                AlgorithmInventionLabR908.registerDirected(db,g,issue(199),issue(200),"R79_TEST","SYNTHETIC_HISTORICAL_ADMISSION");
                // These counters exercise lawful historical L4 admission only.
                db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET future_n=1000,future_hit=400,future_hit_rate=.4,future_wilson_lower=.35,future_block_min=.25,future_regime_min=.2,future_coverage=.6 WHERE genome_id=?",new Object[]{id});
                db.execSQL("INSERT INTO "+AlgorithmProofRecoveryR948.HEAD+" VALUES(?,'CLEAN','',0,0,?,?,'','SYNTHETIC',?)",new Object[]{id,hash("state"),hash("head"),System.currentTimeMillis()});
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            ok(Batch34ChallengerCertificateBridgeR930.sync(store,issue(200)).admitted==1,"historical metrics still admit zero-weight L4");
            ok(support(db,id)==0,"historical admission starts at zero fresh support");
            db.beginTransaction();try {
                draw(db,200);
                for(int n=201;n<=207;n++) {
                    freeze(db,g,n);
                    draw(db,n);
                }
                // Synthetic already-settled ledger rows exercise the consumer, not settlement owner.
                db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" SET hit_label=1,actual_number=1,settled_at=? WHERE genome_id=?",new Object[]{System.currentTimeMillis(),id});
                epoch(db,id,202);
                for(int n:new int[]{203})db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET payload='broken' WHERE target_issue=?",new Object[]{issue(n)});
                for(int n:new int[]{204})AlgorithmProofRecoveryR948.quarantine(db,id,issue(n),"SYNTHETIC_BAD_CAPTURE");
                for(int n:new int[]{205})db.execSQL("UPDATE "+ResearchCaptureLedgerR932.TABLE+" SET full1200=0 WHERE target_issue=?",new Object[]{issue(n)});
                db.execSQL("DELETE FROM "+ResearchCaptureLedgerR932.EVENT+" WHERE target_issue=?",new Object[]{issue(207)});
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            ok("VALID".equals(CandidateExecutionAuditR932.verifyAlgorithm(db,id,issue(201))),"historical original proof remains valid");
            Object observed=metrics(store,id);
            if(args.length>2&&"baseline".equals(args[2])) {
                ok(((Integer)field(observed,"n"))==6,"baseline counts pre-epoch and invalid-proof historical rows as fresh");
                System.out.println("R79_BATCH34_BASELINE_REPRODUCED checks="+checks);return;
            }
            ok((Boolean)field(observed,"readSucceeded"),"qualified metrics read succeeded");
            ok(((Integer)field(observed,"n"))==1,"only current-epoch full1200 proof-bound row contributes");
            ok(((Integer)field(observed,"available"))==6,"coverage denominator begins at current head epoch");
            fresh(store);ok(support(db,id)==1,"writer projects qualified fresh support");
            Object head=field(observed,"head");
            epoch(db,id,203);
            ok(!(Boolean)invoke("sameHead",new Class<?>[]{SQLiteDatabase.class,String.class,head.getClass()},db,id,head),"writer head guard rejects captured evidence after reset");
            fresh(store);ok(support(db,id)==0,"successful empty current-head scan clears prior support");
            long evidence=count(db,"SELECT COUNT(*) FROM "+Batch34SchemaR930.TABLE_EVIDENCE_LINK);
            fresh(store);ok(evidence==count(db,"SELECT COUNT(*) FROM "+Batch34SchemaR930.TABLE_EVIDENCE_LINK),"empty-scan replay keeps evidence exactly once");
            db.execSQL("UPDATE "+Batch34SchemaR930.TABLE_LIFECYCLE+" SET future_support=77 WHERE algorithm_id=?",new Object[]{id});
            db.execSQL("ALTER TABLE "+AlgorithmProofRecoveryR948.HEAD+" RENAME TO r79_unavailable_head");
            try {
                ok(!(Boolean)field(metrics(store,id),"readSucceeded"),"failed read is distinguishable from successful zero");
                fresh(store);ok(support(db,id)==77,"failed read does not erase existing projection");
            }finally{db.execSQL("ALTER TABLE r79_unavailable_head RENAME TO "+AlgorithmProofRecoveryR948.HEAD);}
            fresh(store);ok(support(db,id)==0,"projection reconciles once readable again");
            db.execSQL("UPDATE "+AlgorithmProofRecoveryR948.HEAD+" SET status='AUDITING' WHERE genome_id=?",new Object[]{id});
            db.execSQL("UPDATE "+Batch34SchemaR930.TABLE_LIFECYCLE+" SET future_support=33 WHERE algorithm_id=?",new Object[]{id});
            ok((Boolean)field(metrics(store,id),"readSucceeded"),"known auditing head is readable ineligible evidence");
            fresh(store);ok(support(db,id)==0,"known auditing head clears stale support");
            db.execSQL("UPDATE "+AlgorithmProofRecoveryR948.HEAD+" SET genome_id=? WHERE genome_id=?",new Object[]{id+"-saved",id});
            db.execSQL("UPDATE "+Batch34SchemaR930.TABLE_LIFECYCLE+" SET future_support=44 WHERE algorithm_id=?",new Object[]{id});
            ok((Boolean)field(metrics(store,id),"readSucceeded"),"absent head is readable ineligible evidence");
            fresh(store);ok(support(db,id)==0,"absent head clears stale support without inventing a head");
            ok(count(db,"SELECT COUNT(*) FROM "+AlgorithmProofRecoveryR948.HEAD+" WHERE genome_id='"+id+"'")==0,"bridge never creates missing integrity evidence");
            db.execSQL("UPDATE "+AlgorithmProofRecoveryR948.HEAD+" SET genome_id=? WHERE genome_id=?",new Object[]{id,id+"-saved"});
            epoch(db,id,203);
            for(int n=208;n<220;n++)draw(db,n);
            for(int n=220;n<=224;n++) {
                db.beginTransaction();try {
                    freeze(db,g,n);
                    if(n==221)db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET payload='broken' WHERE target_issue=?",new Object[]{issue(n)});
                    if(n==222)AlgorithmProofRecoveryR948.quarantine(db,id,issue(n),"SYNTHETIC_BAD_CAPTURE");
                    if(n==223)db.execSQL("UPDATE "+ResearchCaptureLedgerR932.TABLE+" SET full1200=0 WHERE target_issue=?",new Object[]{issue(n)});
                    if(n==224)epoch(db,id,225);
                    db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                mirror(store,n);
                ok(mirrors(db)==1,n==220?"valid current-head undrawn prediction mirrors":"invalid or pre-epoch prediction cannot mirror "+n);
                if(n==220){mirror(store,n);ok(mirrors(db)==1,"mirror retry is exactly once");}
                draw(db,n);
            }
            ok(count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE hit_label=1")==7,"historical and rejected labels remain immutable");
            ok(count(db,"SELECT COUNT(*) FROM "+Batch34SchemaR930.TABLE_EVIDENCE_LINK+" WHERE evidence_type='LEGACY_L4_ADMISSION'")==1,"historical L4 admission remains intact");
            ok(count(db,"SELECT COUNT(*) FROM "+Batch34SchemaR930.TABLE_LIFECYCLE+" WHERE state='CHALLENGER'")==1,"bridge remains zero-weight challenger");
            ok(count(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0&&count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_TRADES)==0,"no unified qualification or REAL created");
            System.out.println("R79_BATCH34_EPOCH_PASS checks="+checks+"|scope=ACTUAL_PRODUCT_SYNTHETIC_SQLITE|natural_future=0|real=0");
        }
    }
}
