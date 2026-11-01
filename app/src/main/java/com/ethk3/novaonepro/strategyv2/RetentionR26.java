package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import android.database.sqlite.SQLiteDatabase;
import java.util.Map;
/** Bounded raw feedback and freeze history. Lag revokes authority without discarding learned parameters. */
public final class RetentionR26 {
    private RetentionR26() {}
    /** Per-statement upper bound; actual duration still comes from the writer ledger.
     * Each live maintenance pass executes one persisted phase. A fold and its exact delete remain atomic.
     * prune() is the historical full-cycle adapter for existing diagnostics and regression callers. */
    static final int SLICE_R30 = 500;
    private static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    private static void fold(SQLiteDatabase db,String kind,long n){if(n==0)return;db.execSQL("INSERT OR IGNORE INTO r26_retention_total VALUES(?,0)",new Object[]{kind});db.execSQL("UPDATE r26_retention_total SET folded=folded+? WHERE kind=?",new Object[]{n,kind});}
    public static final int PHASES_R30_REPAIR=24;
    public static final String PHASE_VERSION="R30_RETENTION_PHASES_1",PHASE_PANEL="r30-retention-control",PHASE_KEY="maintenance-phase";
    public static final class Checkpoint {
        public final String original;public final int phase;public final long cycleSequence,lastCompletedSequence,workCursor;
        Checkpoint(String original,int phase,long cycle,long completed,long cursor){this.original=original;this.phase=phase;cycleSequence=cycle;lastCompletedSequence=completed;workCursor=cursor;}
    }
    public static Checkpoint checkpoint(SQLiteDatabase db){
        String text=null;try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{PHASE_PANEL,PHASE_KEY})){if(c.moveToFirst())text=c.getString(0);}
        if(text==null)return new Checkpoint(null,0,-1,-1,0);Map<String,Object> m=MetricEvaluatorR29.map(JsonR27.parse(text));
        if(!PHASE_VERSION.equals(m.get("version"))||!(m.get("phase") instanceof Number)||!(m.get("cycleSequence") instanceof Number)||!(m.get("lastCompletedSequence") instanceof Number)||!(m.get("workCursor") instanceof Number))throw new IllegalStateException("R30_RETENTION_CHECKPOINT_UNREADABLE");
        int phase=((Number)m.get("phase")).intValue();long cycle=((Number)m.get("cycleSequence")).longValue(),completed=((Number)m.get("lastCompletedSequence")).longValue(),cursor=((Number)m.get("workCursor")).longValue();if(phase<0||phase>PHASES_R30_REPAIR+2||cycle<0||completed< -1||cursor<0)throw new IllegalStateException("R30_RETENTION_CHECKPOINT_RANGE");return new Checkpoint(text,phase,cycle,completed,cursor);
    }
    public static void commitCheckpoint(SQLiteDatabase db,Checkpoint expected,long cycleSequence,String foldHash,long foldThrough){
        if(!db.inTransaction())throw new IllegalStateException("R26_RETENTION_WRITER");Checkpoint actual=checkpoint(db);if(!java.util.Objects.equals(expected.original,actual.original))throw new IllegalStateException("R30_RETENTION_PHASE_SOURCE_CHANGED");
        int next=(expected.phase+1)%(PHASES_R30_REPAIR+3);Map<String,Object> m=new java.util.LinkedHashMap<>();m.put("version",PHASE_VERSION);m.put("phase",next);m.put("cycleSequence",cycleSequence);m.put("lastCompletedSequence",next==0?cycleSequence:expected.lastCompletedSequence);m.put("workCursor",expected.workCursor+1);m.put("workUnit","PHASE");m.put("foldStepHash",foldHash);m.put("foldThroughOrdinal",foldThrough<0?null:foldThrough);m.put("scope","durable phase and its SQL effects commit together; external file lifecycle has its original intent/receipt protocol");
        db.execSQL("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,?,?,?)",new Object[]{PHASE_PANEL,PHASE_KEY,JsonR27.write(m),System.currentTimeMillis()});
    }
    /** Legacy full-cycle adapter; the live worker calls exactly one pruneStep per logical commit. */
    public static long prune(SQLiteDatabase db,long seq){long originalTotals=0;for(int phase=0;phase<PHASES_R30_REPAIR;phase++){long n=pruneStep(db,seq,phase);if(phase==8||phase==11||phase==23)originalTotals+=n;}return originalTotals;}
    public static long pruneStep(SQLiteDatabase db,long seq,int phase){
        if(!db.inTransaction())throw new IllegalStateException("R26_RETENTION_WRITER");
        if(phase<0||phase>=PHASES_R30_REPAIR)throw new IllegalArgumentException("R26_RETENTION_PHASE");
        long cut=seq-5000;
        switch(phase){
            case 0:{
        db.execSQL("INSERT OR IGNORE INTO r26_certificate_revocation SELECT c.certificate_id,'FEEDBACK_RETENTION_LAG',? FROM su2_certificate c JOIN r26_live_model l ON l.policy_hash=c.genome_key WHERE l.status IN ('ACTIVE','BLOCKED_RETENTION_LAG') AND l.through_ordinal<? AND NOT EXISTS(SELECT 1 FROM r26_certificate_revocation r WHERE r.certificate_id=c.certificate_id) LIMIT 64",new Object[]{System.currentTimeMillis(),cut});
        // P0 already requires through_ordinal=target-1. Keep P2 eligible so a lagged model can
        // catch up from retained canonical labels; actual holes reset temporal context, not weights.
        // Recover the earlier status only when its family and global capacity remain available.
        try(Cursor c=db.rawQuery("SELECT policy_hash,canonical_text,family_id FROM r26_live_model l WHERE status='BLOCKED_RETENTION_LAG' AND NOT EXISTS(SELECT 1 FROM r26_live_model a WHERE a.family_id=l.family_id AND a.status='ACTIVE') ORDER BY updated_at,policy_hash LIMIT 1",null)){
            if(c.moveToFirst()&&CurrentPolicyR26.matches(c.getString(1),c.getString(0))&&count(db,"SELECT count(*) FROM r26_live_model WHERE status='ACTIVE'")<32&&(!c.getString(2).startsWith("L:")||count(db,"SELECT count(*) FROM r26_live_model WHERE status='ACTIVE' AND family_id LIKE 'L:%'")<8))
                db.execSQL("UPDATE r26_live_model SET status='ACTIVE' WHERE policy_hash=? AND status='BLOCKED_RETENTION_LAG'",new Object[]{c.getString(0)});
        }
                return 0;
            }
            case 1:{
        db.execSQL("UPDATE r26_foundry SET status='EXPIRED_RETENTION',snapshot=X'' WHERE rowid IN (SELECT rowid FROM r26_foundry WHERE source_first<? AND length(snapshot)>0 ORDER BY source_first LIMIT 16)",new Object[]{cut});
                return 0;
            }
            case 2:{
        WarmCandidateR26.preserveExpired(db);
                return 0;
            }
            case 3:{
        WarmCandidateR26.pruneInitializers(db);
        // Expired generations must not pin a hidden raw calendar or a cache of old per-period outputs.
                return 0;
            }
            case 4:{
        db.execSQL("UPDATE r26_legacy_campaign SET status='EXPIRED_RETENTION',snapshot=X'' WHERE rowid IN (SELECT rowid FROM r26_legacy_campaign WHERE source_first<? AND length(snapshot)>0 ORDER BY source_first LIMIT 16)",new Object[]{cut});
        db.execSQL("UPDATE r26_legacy_campaign SET status='NUMERICAL_ABI_CHANGED',snapshot=X'' WHERE rowid IN (SELECT rowid FROM r26_legacy_campaign WHERE numerical_abi<>? AND status='RUNNING' LIMIT 16)",new Object[]{ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
                return 0;
            }
            case 5:{
        db.execSQL("DELETE FROM r26_replay_output_chunk WHERE rowid IN (SELECT o.rowid FROM r26_replay_output_chunk o JOIN r26_legacy_campaign c ON c.campaign_id=o.campaign_id WHERE c.source_first<? OR c.numerical_abi<>? LIMIT 256)",new Object[]{cut,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
                return 0;
            }
            case 6:{
        db.execSQL("DELETE FROM r26_member_replay WHERE rowid IN (SELECT m.rowid FROM r26_member_replay m JOIN r26_legacy_campaign c ON c.campaign_id=m.campaign_id WHERE (c.source_first<? OR c.numerical_abi<>?) AND NOT EXISTS(SELECT 1 FROM r26_replay_output_chunk o WHERE o.campaign_id=m.campaign_id AND o.cache_key=m.cache_key) LIMIT 64)",new Object[]{cut,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
                return 0;
            }
            case 7:{
        db.execSQL("DELETE FROM r26_prepared_policy WHERE rowid IN (SELECT rowid FROM r26_prepared_policy WHERE genesis_ordinal<? OR numerical_abi<>? LIMIT "+SLICE_R30+")",new Object[]{cut,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
                return 0;
            }
            case 8:{
        String oldEvents="SELECT event_id FROM r26_feedback WHERE issue_ordinal<"+cut+" ORDER BY issue_ordinal LIMIT 256";
        long n=count(db,"SELECT count(*) FROM r26_consumption WHERE event_id IN ("+oldEvents+")");
        fold(db,"consumed_feedback_rows",n);db.execSQL("DELETE FROM r26_consumption WHERE event_id IN ("+oldEvents+")");
        db.execSQL("DELETE FROM r26_feedback WHERE event_id IN ("+oldEvents+")");
        long removed=count(db,"SELECT changes()");fold(db,"feedback_rows",removed);
                return removed;
            }
            case 9:{
        db.execSQL("DELETE FROM r26_prepared_forecast WHERE rowid IN (SELECT rowid FROM r26_prepared_forecast WHERE policy_hash NOT IN (SELECT policy_hash FROM r26_live_model WHERE status='ACTIVE') LIMIT "+SLICE_R30+")");
                return 0;
            }
            case 10:{
        db.execSQL("DELETE FROM r26_incumbent_frozen WHERE target_ordinal IN (SELECT target_ordinal FROM r26_incumbent_frozen WHERE target_ordinal<? ORDER BY target_ordinal LIMIT 512)",new Object[]{seq-10080});
                return 0;
            }
            case 11:{
        db.execSQL("DELETE FROM r26_frozen WHERE rowid IN (SELECT rowid FROM r26_frozen WHERE target_ordinal<? ORDER BY target_ordinal LIMIT 512)",new Object[]{seq-10080});
        long frozen=count(db,"SELECT changes()");fold(db,"frozen_rows",frozen);
                return frozen;
            }
            case 12:{
        db.execSQL("DELETE FROM r26_expert_audit WHERE rowid IN (SELECT rowid FROM r26_expert_audit ORDER BY recorded_at DESC,rowid DESC LIMIT 256 OFFSET 2048)");
                return 0;
            }
            case 13:{
        ExpertContinuationR26.preserveExpired(db);
        db.execSQL("DELETE FROM r26_expert_experiment WHERE rowid IN (SELECT rowid FROM r26_expert_experiment WHERE campaign_id NOT IN (SELECT campaign_id FROM r26_foundry) AND (length(state)=0 OR cursor=0 AND checkpoint_hash='') LIMIT "+SLICE_R30+")");
                return 0;
            }
            case 14:{
        db.execSQL("UPDATE r26_expert_experiment SET status='NUMERICAL_ABI_CHANGED' WHERE rowid IN (SELECT rowid FROM r26_expert_experiment WHERE numerical_abi<>? AND status='RUNNING' LIMIT 16)",new Object[]{ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
        // Keep complete identities for live/retained candidates plus a bounded historical tail.
                return 0;
            }
            case 15:{
        db.execSQL("DELETE FROM r26_policy_identity WHERE policy_hash IN (SELECT p.policy_hash FROM r26_policy_identity p WHERE instr(p.genotype_text,'_warm_kernel=')=0 AND p.policy_hash NOT IN (SELECT policy_hash FROM r26_live_model UNION SELECT policy_hash FROM r26_candidate UNION SELECT policy_hash FROM r26_prepared_policy UNION SELECT genome_key FROM su2_model_genome WHERE state IN ('LEAGUE','HALL_OF_FAME') UNION SELECT substr(genotype_text,instr(genotype_text,'_warm_parent=')+13,64) FROM r26_policy_identity WHERE instr(genotype_text,'_warm_parent=')>0) ORDER BY p.created_at DESC,p.policy_hash LIMIT 256 OFFSET 4096)");
        // Current policy checkpoints own their full state. Superseded sample receipts are bounded metadata.
                return 0;
            }
            case 16:{
        db.execSQL("DELETE FROM r26_checkpoint_receipt WHERE rowid IN (SELECT rowid FROM r26_checkpoint_receipt ORDER BY created_at DESC,rowid DESC LIMIT 256 OFFSET 4096)");
                return 0;
            }
            case 17:{
        db.execSQL("DELETE FROM r26_live_failure WHERE rowid IN (SELECT rowid FROM r26_live_failure ORDER BY recorded_at DESC,rowid DESC LIMIT 256 OFFSET 2048)");
                return 0;
            }
            case 18:{
        db.execSQL("DELETE FROM r26_gap_receipt WHERE rowid IN (SELECT rowid FROM r26_gap_receipt ORDER BY recorded_at DESC,rowid DESC LIMIT 256 OFFSET 2048)");
                return 0;
            }
            case 19:{
        db.execSQL("DELETE FROM r26_session WHERE session_id IN (SELECT session_id FROM r26_session WHERE state<>'RUNNING' ORDER BY heartbeat DESC LIMIT 32 OFFSET 32)");
                return 0;
            }
            case 20:{
        ServiceTotalsR26.foldExpired(db);
                return 0;
            }
            case 21:{
        if(FailureDirectedResearchR26.installed(db))db.execSQL("DELETE FROM r26_failure_research_cursor WHERE parent_policy IN (SELECT parent_policy FROM r26_failure_research_cursor f WHERE NOT EXISTS(SELECT 1 FROM r26_live_model l WHERE l.policy_hash=f.parent_policy) LIMIT 256)");
        // No removed campaign may keep a second hidden copy of raw labels or per-sample consumption.
                return 0;
            }
            case 22:{
        String orphan="SELECT c.rowid FROM r26_consumption c WHERE NOT EXISTS(SELECT 1 FROM r26_feedback f WHERE f.event_id=c.event_id) AND NOT EXISTS(SELECT 1 FROM r26_candidate a WHERE a.policy_hash=c.policy_hash) LIMIT 512";
        db.execSQL("DELETE FROM r26_consumption WHERE rowid IN ("+orphan+")");
                return 0;
            }
            case 23:{
        String oldEvolution="SELECT rowid FROM r26_evolution WHERE campaign_id NOT IN (SELECT campaign_id FROM r26_foundry) LIMIT 512";
        long attempts=count(db,"SELECT count(*) FROM r26_evolution WHERE rowid IN ("+oldEvolution+")");fold(db,"historical_evolution_attempts",attempts);
        db.execSQL("DELETE FROM r26_evolution WHERE rowid IN ("+oldEvolution+")");
                return attempts;
            }
            default:throw new AssertionError(phase);
        }
    }
}
