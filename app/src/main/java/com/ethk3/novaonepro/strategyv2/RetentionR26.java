package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import android.database.sqlite.SQLiteDatabase;
/** Bounded raw feedback and freeze history. Lag revokes authority without discarding learned parameters. */
public final class RetentionR26 {
    private RetentionR26() {}
    private static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    private static void fold(SQLiteDatabase db,String kind,long n){if(n==0)return;db.execSQL("INSERT OR IGNORE INTO r26_retention_total VALUES(?,0)",new Object[]{kind});db.execSQL("UPDATE r26_retention_total SET folded=folded+? WHERE kind=?",new Object[]{n,kind});}
    public static long prune(SQLiteDatabase db,long seq){
        if(!db.inTransaction())throw new IllegalStateException("R26_RETENTION_WRITER");
        long cut=seq-5000;
        db.execSQL("INSERT OR IGNORE INTO r26_certificate_revocation SELECT c.certificate_id,'FEEDBACK_RETENTION_LAG',? FROM su2_certificate c JOIN r26_live_model l ON l.policy_hash=c.genome_key WHERE l.status IN ('ACTIVE','BLOCKED_RETENTION_LAG') AND l.through_ordinal<?",new Object[]{System.currentTimeMillis(),cut});
        // P0 already requires through_ordinal=target-1. Keep P2 eligible so a lagged model can
        // catch up from retained canonical labels; actual holes reset temporal context, not weights.
        // Recover the earlier status only when its family and global capacity remain available.
        try(Cursor c=db.rawQuery("SELECT policy_hash,canonical_text,family_id FROM r26_live_model l WHERE status='BLOCKED_RETENTION_LAG' AND NOT EXISTS(SELECT 1 FROM r26_live_model a WHERE a.family_id=l.family_id AND a.status='ACTIVE') ORDER BY updated_at,policy_hash LIMIT 1",null)){
            if(c.moveToFirst()&&CurrentPolicyR26.matches(c.getString(1),c.getString(0))&&count(db,"SELECT count(*) FROM r26_live_model WHERE status='ACTIVE'")<32&&(!c.getString(2).startsWith("L:")||count(db,"SELECT count(*) FROM r26_live_model WHERE status='ACTIVE' AND family_id LIKE 'L:%'")<8))
                db.execSQL("UPDATE r26_live_model SET status='ACTIVE' WHERE policy_hash=? AND status='BLOCKED_RETENTION_LAG'",new Object[]{c.getString(0)});
        }
        db.execSQL("UPDATE r26_foundry SET status='EXPIRED_RETENTION',snapshot=X'' WHERE source_first<? AND length(snapshot)>0",new Object[]{cut});
        db.execSQL("DELETE FROM r26_candidate WHERE campaign_id IN (SELECT campaign_id FROM r26_foundry WHERE status IN ('EXPIRED_RETENTION','ABANDONED_NUMERICAL_ABI'))");
        // Expired generations must not pin a hidden raw calendar or a cache of old per-period outputs.
        db.execSQL("UPDATE r26_legacy_campaign SET status='EXPIRED_RETENTION',snapshot=X'' WHERE source_first<? AND length(snapshot)>0",new Object[]{cut});
        db.execSQL("UPDATE r26_legacy_campaign SET status='NUMERICAL_ABI_CHANGED',snapshot=X'' WHERE numerical_abi<>? AND status='RUNNING'",new Object[]{ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
        db.execSQL("DELETE FROM r26_replay_output_chunk WHERE rowid IN (SELECT o.rowid FROM r26_replay_output_chunk o JOIN r26_legacy_campaign c ON c.campaign_id=o.campaign_id WHERE c.source_first<? OR c.numerical_abi<>? LIMIT 256)",new Object[]{cut,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
        db.execSQL("DELETE FROM r26_member_replay WHERE rowid IN (SELECT m.rowid FROM r26_member_replay m JOIN r26_legacy_campaign c ON c.campaign_id=m.campaign_id WHERE (c.source_first<? OR c.numerical_abi<>?) AND NOT EXISTS(SELECT 1 FROM r26_replay_output_chunk o WHERE o.campaign_id=m.campaign_id AND o.cache_key=m.cache_key) LIMIT 64)",new Object[]{cut,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
        db.execSQL("DELETE FROM r26_prepared_policy WHERE genesis_ordinal<? OR numerical_abi<>?",new Object[]{cut,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
        String oldEvents="SELECT event_id FROM r26_feedback WHERE issue_ordinal<"+cut+" ORDER BY issue_ordinal LIMIT 256";
        long n=count(db,"SELECT count(*) FROM r26_consumption WHERE event_id IN ("+oldEvents+")");
        fold(db,"consumed_feedback_rows",n);db.execSQL("DELETE FROM r26_consumption WHERE event_id IN ("+oldEvents+")");
        db.execSQL("DELETE FROM r26_feedback WHERE event_id IN ("+oldEvents+")");
        long removed=count(db,"SELECT changes()");fold(db,"feedback_rows",removed);
        db.execSQL("DELETE FROM r26_prepared_forecast WHERE policy_hash NOT IN (SELECT policy_hash FROM r26_live_model WHERE status='ACTIVE')");
        db.execSQL("DELETE FROM r26_frozen WHERE rowid IN (SELECT rowid FROM r26_frozen WHERE target_ordinal<? ORDER BY target_ordinal LIMIT 512)",new Object[]{seq-10080});
        long frozen=count(db,"SELECT changes()");fold(db,"frozen_rows",frozen);
        db.execSQL("DELETE FROM r26_expert_audit WHERE rowid IN (SELECT rowid FROM r26_expert_audit ORDER BY recorded_at DESC,rowid DESC LIMIT 256 OFFSET 2048)");
        db.execSQL("DELETE FROM r26_expert_experiment WHERE campaign_id NOT IN (SELECT campaign_id FROM r26_foundry) AND status<>'RUNNING'");
        db.execSQL("UPDATE r26_expert_experiment SET status='NUMERICAL_ABI_CHANGED' WHERE numerical_abi<>? AND status='RUNNING'",new Object[]{ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
        // Keep complete identities for live/retained candidates plus a bounded historical tail.
        db.execSQL("DELETE FROM r26_policy_identity WHERE policy_hash IN (SELECT p.policy_hash FROM r26_policy_identity p WHERE p.policy_hash NOT IN (SELECT policy_hash FROM r26_live_model UNION SELECT policy_hash FROM r26_candidate UNION SELECT policy_hash FROM r26_prepared_policy UNION SELECT genome_key FROM su2_model_genome WHERE state IN ('LEAGUE','HALL_OF_FAME')) ORDER BY p.created_at DESC,p.policy_hash LIMIT 256 OFFSET 4096)");
        // Current policy checkpoints own their full state. Superseded sample receipts are bounded metadata.
        db.execSQL("DELETE FROM r26_checkpoint_receipt WHERE rowid IN (SELECT rowid FROM r26_checkpoint_receipt ORDER BY created_at DESC,rowid DESC LIMIT 256 OFFSET 4096)");
        db.execSQL("DELETE FROM r26_live_failure WHERE rowid IN (SELECT rowid FROM r26_live_failure ORDER BY recorded_at DESC,rowid DESC LIMIT 256 OFFSET 2048)");
        db.execSQL("DELETE FROM r26_gap_receipt WHERE rowid IN (SELECT rowid FROM r26_gap_receipt ORDER BY recorded_at DESC,rowid DESC LIMIT 256 OFFSET 2048)");
        db.execSQL("DELETE FROM r26_session WHERE session_id IN (SELECT session_id FROM r26_session WHERE state<>'RUNNING' ORDER BY heartbeat DESC LIMIT 32 OFFSET 32)");
        // No removed campaign may keep a second hidden copy of raw labels or per-sample consumption.
        String orphan="SELECT c.rowid FROM r26_consumption c WHERE NOT EXISTS(SELECT 1 FROM r26_feedback f WHERE f.event_id=c.event_id) AND NOT EXISTS(SELECT 1 FROM r26_candidate a WHERE a.policy_hash=c.policy_hash) LIMIT 512";
        db.execSQL("DELETE FROM r26_consumption WHERE rowid IN ("+orphan+")");
        String oldEvolution="SELECT rowid FROM r26_evolution WHERE campaign_id NOT IN (SELECT campaign_id FROM r26_foundry) LIMIT 512";
        long attempts=count(db,"SELECT count(*) FROM r26_evolution WHERE rowid IN ("+oldEvolution+")");fold(db,"historical_evolution_attempts",attempts);
        db.execSQL("DELETE FROM r26_evolution WHERE rowid IN ("+oldEvolution+")");return removed+frozen+attempts;
    }
}
