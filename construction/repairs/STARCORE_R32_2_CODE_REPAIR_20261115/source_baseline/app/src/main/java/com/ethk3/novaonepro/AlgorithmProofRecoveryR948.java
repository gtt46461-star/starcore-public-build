package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/** Research evidence repair, not a new scheduler, settlement owner or authority gate.
 * Missing historical proof is UNVERIFIABLE, never retrospectively manufactured.
 * A contaminated/unverifiable HEAD is archived in full and reinitialized. Old predictions,
 * outcomes and paid statistical looks remain immutable and cannot enter the new evidence epoch.
 */
public final class AlgorithmProofRecoveryR948 {
    public static final String HEAD="research_algorithm_head_integrity_r948";
    public static final String AUDIT="research_algorithm_head_archive_r948";
    public static final String QUARANTINE="research_algorithm_proof_quarantine_r948";
    private AlgorithmProofRecoveryR948() {}

    static void schema(SQLiteDatabase db) {
        if(TransactionSchemaCacheR941.readyCommitted(db,"AlgorithmProofRecoveryR948"))return;
        db.execSQL("CREATE TABLE IF NOT EXISTS "+HEAD+"(genome_id TEXT PRIMARY KEY,status TEXT NOT NULL,cursor_issue TEXT NOT NULL,scanned_n INTEGER NOT NULL,invalid_n INTEGER NOT NULL,input_state_hash TEXT NOT NULL,evidence_hash TEXT NOT NULL,epoch_start_issue TEXT NOT NULL,reason TEXT NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+AUDIT+"(audit_hash TEXT PRIMARY KEY,genome_id TEXT NOT NULL,previous_head_json TEXT NOT NULL,previous_state_hash TEXT NOT NULL,new_state_hash TEXT NOT NULL,epoch_start_issue TEXT NOT NULL,invalid_n INTEGER NOT NULL,evidence_hash TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+QUARANTINE+"(genome_id TEXT NOT NULL,invalid_issue TEXT NOT NULL,record_hash TEXT NOT NULL,original_hit INTEGER NOT NULL,original_outcome_hash TEXT NOT NULL,reason TEXT NOT NULL,detected_at INTEGER NOT NULL,PRIMARY KEY(genome_id,invalid_issue))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r948_integrity_pending ON "+HEAD+"(status,genome_id)");
        TransactionSchemaCacheR941.markCommitted(db,"AlgorithmProofRecoveryR948");
    }

    static String stateHash(Cursor row) {
        return V51ChecksumEngine.sha256(row.getString(0)+'|'+row.getString(1)+'|'+row.getLong(2)+'|'+row.getLong(3)+'|'+row.getString(4));
    }
    static Cursor headState(SQLiteDatabase db,String id) {
        return db.rawQuery("SELECT structural_hash,state_blob,state_updates,future_n,trained_until_issue FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{id});
    }
    /** New, truly untrained algorithms can enroll inside their first freeze transaction. */
    static void requireCleanForFreeze(SQLiteDatabase db,String id) {
        schema(db);
        try(Cursor c=db.rawQuery("SELECT status FROM "+HEAD+" WHERE genome_id=?",new String[]{id})) {
            if(c.moveToFirst()) {
                if(!"CLEAN".equals(c.getString(0))&&!"RESET_CLEAN".equals(c.getString(0)))throw new IllegalStateException("R948_HEAD_INTEGRITY_AUDIT_REQUIRED");
                return;
            }
        }
        try(Cursor g=headState(db,id);Cursor p=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE genome_id=? LIMIT 1",new String[]{id})) {
            if(!g.moveToFirst()||g.getLong(2)!=0||g.getLong(3)!=0||p.moveToFirst())throw new IllegalStateException("R948_LEGACY_HEAD_AUDIT_REQUIRED");
            db.execSQL("INSERT INTO "+HEAD+" VALUES(?,'CLEAN','',0,0,?,?,'','NEW_UNTRAINED_HEAD',?)",new Object[]{id,stateHash(g),V51ChecksumEngine.sha256("R948_NEW_HEAD|"+id),System.currentTimeMillis()});
        }
    }
    static boolean acceptsIssue(SQLiteDatabase db,String id,String target) {
        try(Cursor c=db.rawQuery("SELECT status,epoch_start_issue FROM "+HEAD+" WHERE genome_id=?",new String[]{id})) {
            return c.moveToFirst() && ("CLEAN".equals(c.getString(0))||"RESET_CLEAN".equals(c.getString(0)))
                    && (c.getString(1).isEmpty()||target.compareTo(c.getString(1))>=0);
        }
    }
    /**
     * SQL eligibility for a new pre-draw freeze. Legacy mutable heads must finish integrity audit
     * before they can consume the one-minute future window. A truly untrained genome may enroll
     * its CLEAN head atomically inside the first freeze transaction.
     */
    static String freezeReadySql(String genomeExpression,String target) {
        if(genomeExpression==null||genomeExpression.trim().isEmpty()||!TargetIssueResolver.isValid(target))
            throw new IllegalArgumentException("R950_FREEZE_READY_INPUT");
        String q="'"+target+"'";
        return "(EXISTS(SELECT 1 FROM "+HEAD+" h950 WHERE h950.genome_id="+genomeExpression
                +" AND h950.status IN('CLEAN','RESET_CLEAN') AND (h950.epoch_start_issue='' OR "+q+">=h950.epoch_start_issue))"
                +" OR (COALESCE(g.state_updates,0)=0 AND COALESCE(g.future_n,0)=0"
                +" AND NOT EXISTS(SELECT 1 FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" old950 WHERE old950.genome_id="+genomeExpression+")))";
    }
    static String epoch(SQLiteDatabase db,String key) {
        if(!key.startsWith("algorithm:"))return "NOT_ALGORITHM";
        try(Cursor c=db.rawQuery("SELECT epoch_start_issue,evidence_hash FROM "+HEAD+" WHERE genome_id=?",new String[]{key.substring(10)})) {
            return c.moveToFirst()?V51ChecksumEngine.sha256(c.getString(0)+'|'+c.getString(1)):"UNAUDITED";
        }
    }
    /** A qualified SQL subset for queue selection, followed by cryptographic validation at use. */
    static String eligible(String id,String target,String prediction) {
        return "EXISTS(SELECT 1 FROM "+HEAD+" h948 WHERE h948.genome_id="+id+" AND h948.status IN('CLEAN','RESET_CLEAN') AND (h948.epoch_start_issue='' OR "+target+">=h948.epoch_start_issue))"
            +" AND EXISTS(SELECT 1 FROM "+CandidateExecutionAuditR932.TABLE+" e948 JOIN "+ResearchCaptureLedgerR932.TABLE+" c948 ON c948.target_issue=e948.target_issue AND c948.candidate_key=e948.candidate_key AND c948.record_hash=e948.prediction_hash AND c948.frozen=1 AND c948.full1200=1"
            +" WHERE e948.candidate_key='algorithm:'||"+id+" AND e948.target_issue="+target+" AND e948.prediction_hash="+prediction+" AND e948.action_count=1200)"
            +" AND NOT EXISTS(SELECT 1 FROM "+QUARANTINE+" q948 WHERE q948.genome_id="+id+" AND q948.invalid_issue="+target+")";
    }
    /** A clean replacement head cannot consume old labels. That boundary does not
     * invalidate a separately authenticated prediction's historical outcome. */
    static boolean historicalOutcomeOnlyR79(SQLiteDatabase db,String id,String target) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+HEAD+" WHERE genome_id=? AND status='RESET_CLEAN'"
                +" AND length(epoch_start_issue)=12 AND epoch_start_issue>?"
                +" AND NOT EXISTS(SELECT 1 FROM "+QUARANTINE+" q WHERE q.genome_id=? AND q.invalid_issue=?"
                +" AND q.reason NOT IN('SETTLEMENT_REJECTED:VALID','MIRROR_SETTLEMENT_REJECTED_UNQUALIFIED_CAPTURE'))",
                new String[]{id,target,id,target})) {
            return c.moveToFirst();
        }
    }
    /** Queue admission only. The original payload, all pre-draw clocks and the exact
     * authoritative draw are revalidated by the settlement owner before any label. */
    static String settlementEligibleR79(String id,String target,String prediction) {
        return "("+eligible(id,target,prediction)+") OR ("
            +"EXISTS(SELECT 1 FROM "+HEAD+" h79 WHERE h79.genome_id="+id
            +" AND h79.status='RESET_CLEAN' AND length(h79.epoch_start_issue)=12 AND "+target+"<h79.epoch_start_issue)"
            +" AND EXISTS(SELECT 1 FROM "+CandidateExecutionAuditR932.TABLE+" e79 JOIN "+ResearchCaptureLedgerR932.TABLE
            +" c79 ON c79.target_issue=e79.target_issue AND c79.candidate_key=e79.candidate_key"
            +" AND c79.record_hash=e79.prediction_hash AND c79.frozen=1 AND c79.full1200=1"
            +" WHERE e79.candidate_key='algorithm:'||"+id+" AND e79.target_issue="+target
            +" AND e79.prediction_hash="+prediction+" AND e79.action_count=1200)"
            +" AND NOT EXISTS(SELECT 1 FROM "+QUARANTINE+" q79 WHERE q79.genome_id="+id+" AND q79.invalid_issue="+target
            +" AND q79.reason NOT IN('SETTLEMENT_REJECTED:VALID','MIRROR_SETTLEMENT_REJECTED_UNQUALIFIED_CAPTURE')))";
    }
    static void quarantine(SQLiteDatabase db,String id,String issue,String reason) {
        if(!db.inTransaction())throw new IllegalStateException("R948_QUARANTINE_TRANSACTION_REQUIRED");
        db.execSQL("INSERT OR IGNORE INTO "+QUARANTINE+" SELECT genome_id,target_issue,record_hash,hit_label,outcome_hash,?,? FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE genome_id=? AND target_issue=?",new Object[]{reason,System.currentTimeMillis(),id,issue});
    }

    /** At most 16 old records per atomic page; committed audit cursors survive a yield/restart. */
    public static int maintain(final DbStore store,int maxPages) {
        if(store==null||RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return 0;
        if(!store.coordinatorR36().runAtomicWrite("R948_RESEARCH_INTEGRITY_SCHEMA",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){schema(db);CandidateExecutionAuditR932.schema(db);ResearchCaptureLedgerR932.schema(db);}}))throw new IllegalStateException("R948_INTEGRITY_SCHEMA_FAILED");
        int completed=0;
        for(int i=0;i<Math.max(1,Math.min(32,maxPages));i++) {
            ResearchWorkBudgetR927.checkpoint();
            final String id;
            try(Cursor c=BudgetedQueryR927.query(store.readable(),"SELECT g.genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" g LEFT JOIN "+HEAD+" h ON h.genome_id=g.genome_id WHERE h.genome_id IS NULL OR h.status='AUDITING' ORDER BY g.created_at,g.genome_id LIMIT 1",null)) {
                if(!c.moveToFirst())break;id=c.getString(0);
            }
            final RuntimeException[] cause=new RuntimeException[1];
            Boolean done=store.coordinatorR36().runAtomicWrite("R948_RESEARCH_INTEGRITY_PAGE",new DatabaseCoordinator.WriteCallable<Boolean>(){public Boolean call(SQLiteDatabase db){
                try{return auditPage(db,id);}catch(org.json.JSONException e){cause[0]=new IllegalStateException("R948_AUDIT_JSON_FAILED",e);throw cause[0];}
                catch(RuntimeException error){cause[0]=error;throw error;}
            }},null);
            if(done==null)throw new IllegalStateException("R948_INTEGRITY_PAGE_FAILED",cause[0]);
            if(done.booleanValue())completed++;
        }
        return completed;
    }
    private static boolean auditPage(SQLiteDatabase db,String id)throws org.json.JSONException {
        String observedHash;long updates,future;
        try(Cursor g=headState(db,id)) {if(!g.moveToFirst())return true;observedHash=stateHash(g);updates=g.getLong(2);future=g.getLong(3);}
        db.execSQL("INSERT OR IGNORE INTO "+HEAD+" VALUES(?,'AUDITING','',0,0,?,?,'','VERIFY_ORIGINAL_PROOFS',?)",new Object[]{id,observedHash,V51ChecksumEngine.sha256("R948_AUDIT|"+id),System.currentTimeMillis()});
        String cursor,evidence,input;int scanned,invalid;
        try(Cursor h=db.rawQuery("SELECT cursor_issue,scanned_n,invalid_n,input_state_hash,evidence_hash FROM "+HEAD+" WHERE genome_id=?",new String[]{id})) {
            if(!h.moveToFirst())throw new IllegalStateException("R948_AUDIT_CURSOR_MISSING");cursor=h.getString(0);scanned=h.getInt(1);invalid=h.getInt(2);input=h.getString(3);evidence=h.getString(4);
        }
        // A changed HEAD is not audited using the prior mutable state's checkpoint.
        if(!input.equals(observedHash)) {cursor="";scanned=0;invalid=0;input=observedHash;evidence=V51ChecksumEngine.sha256("R948_RESTART_CHANGED_HEAD|"+id+'|'+input);}
        List<String> issues=new ArrayList<String>();
        try(Cursor c=db.rawQuery("SELECT target_issue FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE genome_id=? AND target_issue>? ORDER BY target_issue LIMIT 16",new String[]{id,cursor})){while(c.moveToNext())issues.add(c.getString(0));}
        int visited=0;
        for(String issue:issues) {
            try{ResearchWorkBudgetR927.checkpoint();}catch(ResearchWorkBudgetR927.Yield y){if(visited==0)throw y;break;}
            String reason=CandidateExecutionAuditR932.verifyAlgorithm(db,id,issue);
            if(!"VALID".equals(reason)){quarantine(db,id,issue,"UNVERIFIABLE_HISTORICAL_CAPTURE:"+reason);invalid++;}
            evidence=V51ChecksumEngine.sha256(evidence+'|'+issue+'|'+reason);cursor=issue;scanned++;visited++;
        }
        db.execSQL("UPDATE "+HEAD+" SET cursor_issue=?,scanned_n=?,invalid_n=?,input_state_hash=?,evidence_hash=?,updated_at=? WHERE genome_id=?",new Object[]{cursor,scanned,invalid,input,evidence,System.currentTimeMillis(),id});
        boolean more;
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE genome_id=? AND target_issue>? LIMIT 1",new String[]{id,cursor})){more=c.moveToFirst();}
        if(more)return false;
        if(invalid>0||updates>scanned||future>scanned) {
            String latest="";try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
            if(!TargetIssueResolver.isValid(latest))return false;
            resetClean(db,id,TargetIssueResolver.next(latest).targetIssue,invalid,evidence,observedHash);
        } else db.execSQL("UPDATE "+HEAD+" SET status='CLEAN',reason='ORIGINAL_CAPTURE_PROOFS_VERIFIED',updated_at=? WHERE genome_id=?",new Object[]{System.currentTimeMillis(),id});
        return true;
    }
    private static void resetClean(SQLiteDatabase db,String id,String first,int invalid,String evidence,String previousState)throws org.json.JSONException {
        JSONObject previous=new JSONObject();
        try(Cursor c=db.rawQuery("SELECT * FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{id})) {
            if(!c.moveToFirst())throw new IllegalStateException("R948_RESET_HEAD_MISSING");
            for(int i=0;i<c.getColumnCount();i++)previous.put(c.getColumnName(i),c.isNull(i)?JSONObject.NULL:c.getString(i));
        }
        String cleanState=AlgorithmGenomeVmR908.initialState(AlgorithmGenomeR908.parse(previous.getString("genome_spec"))).encode(),newHash=V51ChecksumEngine.sha256(cleanState);
        String reason="CLEAN_STATE_REINITIALIZED_UNVERIFIABLE_PRIOR_TRAINING_NOT_REUSED";
        String audit=V51ChecksumEngine.sha256(id+'|'+previousState+'|'+newHash+'|'+first+'|'+evidence);
        db.execSQL("INSERT OR IGNORE INTO "+AUDIT+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{audit,id,previous.toString(),previousState,newHash,first,invalid,evidence,reason,System.currentTimeMillis()});
        ContentValues v=new ContentValues();v.put("state_blob",cleanState);v.put("state_updates",0);v.put("trained_until_issue","");v.put("earliest_use_issue",first);
        v.put("future_n",0);v.put("future_hit",0);v.put("future_hit_rate",0d);v.put("future_wilson_lower",0d);v.put("future_last_issue","");
        v.put("future_evidence_hash",audit);v.put("future_fdr_q",1d);v.put("future_brier",1d);v.put("future_ece",1d);
        v.put("future_coverage",0d);v.put("future_block_min",0d);v.put("future_regime_min",0d);v.put("future_max_miss",0);
        v.put("paired_mainline_n",0);v.put("paired_mainline_delta",0d);v.put("lifecycle_reason",reason);v.put("updated_at",System.currentTimeMillis());
        if(db.update(DbHelper.TABLE_R908_ALGORITHM_GENOME,v,"genome_id=?",new String[]{id})!=1)throw new IllegalStateException("R948_RESET_HEAD_WRITE_FAILED");
        db.execSQL("UPDATE "+HEAD+" SET status='RESET_CLEAN',epoch_start_issue=?,reason=?,evidence_hash=?,updated_at=? WHERE genome_id=?",new Object[]{first,reason,audit,System.currentTimeMillis(),id});
        String key="algorithm:"+id;
        // Preserve paid looks and historical evidence. The old window is not silently restarted.
        if(tableExists(db,FixedResearchWindowR932.WINDOW)) db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET state='PAUSED_DEPENDENCY',reason=? WHERE candidate_key=? AND state='OPEN'",new Object[]{reason,key});
        if(tableExists(db,ResearchPlatformStoreR929.TABLE_CERTIFICATE)) {
            db.execSQL("INSERT OR IGNORE INTO "+ResearchPlatformStoreR929.TABLE_REVOCATION+" SELECT certificate_hash,?,? FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE consumer='INVENTED_ALGORITHM' AND candidate_id=? AND state='ACTIVE'",new Object[]{reason,System.currentTimeMillis(),id});
            db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" SET state='REVOKED',maximum_weight=0,reason=?,updated_at=? WHERE consumer='INVENTED_ALGORITHM' AND candidate_id=? AND state='ACTIVE'",new Object[]{reason,System.currentTimeMillis(),id});
        }
        if(tableExists(db,ResearchLifecycleR932.ELITE))db.execSQL("DELETE FROM "+ResearchLifecycleR932.ELITE+" WHERE candidate_key=?",new Object[]{key});
    }
    private static boolean tableExists(SQLiteDatabase db,String name) {try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
}
