package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Bounded growth for the tables that grew per period without a TTL (P33).
 * Evidence from the 5.4.3 report: research_row_revision_v2 184,964 rows and
 * v57_runtime_event_r926 72,883 rows for only 4,995 draws (3.5 days).
 *
 *  - SU2 live decisions: raw rows kept 7 days (10,080 periods), then folded into
 *    su2_live_daily (one row per strategy per day) - league scoring needs 3,000.
 *  - SU2 lineage/genomes: RESEARCH genomes older than 30 campaigns and not in the
 *    hall of fame / league are deleted; lineage rows of deleted children go with them.
 *  - runtime events: identical (stage, target, detail) repeats are collapsed into one
 *    row with first/last/count by the writer (patch to the R926 writer); rows older
 *    than 7 days leave through the existing archive protocol (permit + CAS), whose
 *    CAS files are themselves capped by su2_storage_budget RESEARCH_CAS.
 * Each pass deletes at most 2,000 rows inside one short writer transaction.
 */
public final class RetentionRulesSU2 {
    private RetentionRulesSU2() {}
    public static final int RAW_DECISION_PERIODS = 10080, BATCH = 2000;

    static int changes(SQLiteDatabase db) { try (Cursor c = db.rawQuery("SELECT changes()", null)) { return c.moveToFirst() ? c.getInt(0) : 0; } }

    public static int foldDecisions(SQLiteDatabase db, long currentSequence, long now) {
        return commitFold(db,prepareFold(db,currentSequence),now);
    }

    /** Immutable exact source keys and original sufficient statistics, prepared outside the writer. */
    public static final class PreparedFold {
        public final String genomeKey,stepHash;public final long dayKey,throughOrdinal;public final int periods,actions,hits;
        private final long[] ordinals;private final String selection;private final String[] bindings;private final String sourceText;
        PreparedFold(String genome,long day,long[] ordinals,int acts,int hits,String sourceText){this.genomeKey=genome;dayKey=day;this.ordinals=ordinals.clone();periods=ordinals.length;actions=acts;this.hits=hits;throughOrdinal=ordinals.length==0?-1:ordinals[ordinals.length-1];this.sourceText=sourceText;stepHash=V51ChecksumEngine.sha256("SU2_DAILY_FOLD_EXACT_KEYS_V2|"+genome+'|'+day+'|'+sourceText);StringBuilder q=new StringBuilder("genome_key=? AND sequence_ordinal IN (");bindings=new String[ordinals.length+1];bindings[0]=genome;for(int i=0;i<ordinals.length;i++){if(i>0)q.append(',');q.append('?');bindings[i+1]=Long.toString(ordinals[i]);}selection=q.append(')').toString();}
    }
    public static PreparedFold prepareFold(SQLiteDatabase read,long currentSequence){
        long cut=currentSequence-RAW_DECISION_PERIODS,first;String genome;
        try(Cursor c=read.rawQuery("SELECT genome_key,sequence_ordinal FROM su2_live_decision WHERE hit IS NOT NULL AND sequence_ordinal<? ORDER BY sequence_ordinal,genome_key LIMIT 1",new String[]{Long.toString(cut)})){if(!c.moveToFirst())return null;genome=c.getString(0);first=c.getLong(1);}
        long day=first/1440;java.util.List<Long> keys=new java.util.ArrayList<>();StringBuilder text=new StringBuilder();int acts=0,hits=0;
        try(Cursor c=read.rawQuery("SELECT sequence_ordinal,act,hit FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal>=? AND sequence_ordinal<? AND hit IS NOT NULL ORDER BY sequence_ordinal LIMIT 64",new String[]{genome,Long.toString(day*1440),Long.toString(Math.min(cut,(day+1)*1440))})){while(c.moveToNext()){long key=c.getLong(0);int act=c.getInt(1),hit=c.getInt(2);keys.add(key);acts+=act;hits+=act==1?hit:0;text.append(key).append(':').append(act).append(':').append(hit).append(';');}}
        if(keys.isEmpty())return null;long[] ordinals=new long[keys.size()];for(int i=0;i<ordinals.length;i++)ordinals[i]=keys.get(i);return new PreparedFold(genome,day,ordinals,acts,hits,text.toString());
    }
    public static int commitFold(SQLiteDatabase db,PreparedFold prepared,long now){
        if(!db.inTransaction())throw new IllegalStateException("SU2_RETENTION_WRITER");if(prepared==null)return 0;
        StringBuilder actual=new StringBuilder();int present=0;try(Cursor c=db.rawQuery("SELECT sequence_ordinal,act,hit FROM su2_live_decision WHERE "+prepared.selection+" AND hit IS NOT NULL ORDER BY sequence_ordinal",prepared.bindings)){while(c.moveToNext()){present++;actual.append(c.getLong(0)).append(':').append(c.getInt(1)).append(':').append(c.getInt(2)).append(';');}}
        if(present==0)return 0; // Earlier successful retry already folded/deleted this exact source set.
        if(present!=prepared.periods||!prepared.sourceText.equals(actual.toString()))throw new IllegalStateException("SU2_RETENTION_PREPARED_SOURCE_CHANGED");
        db.execSQL("INSERT OR IGNORE INTO su2_live_daily(genome_key,day_key,periods,actions,hits,closed_at) VALUES(?,?,0,0,0,?)",new Object[]{prepared.genomeKey,prepared.dayKey,now});
        db.execSQL("UPDATE su2_live_daily SET periods=periods+?,actions=actions+?,hits=hits+?,closed_at=? WHERE genome_key=? AND day_key=?",new Object[]{prepared.periods,prepared.actions,prepared.hits,now,prepared.genomeKey,prepared.dayKey});
        db.execSQL("DELETE FROM su2_live_decision WHERE "+prepared.selection,prepared.bindings);int deleted=changes(db);if(deleted!=prepared.periods)throw new IllegalStateException("SU2_RETENTION_DELETE_COUNT_CHANGED");return deleted;
    }

    public static int pruneResearchGenomes(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("SU2_RETENTION_WRITER");
        java.util.List<String> keys=new java.util.ArrayList<>();try(Cursor c=db.rawQuery("SELECT genome_key FROM su2_model_genome g WHERE state='RESEARCH' AND born_campaign NOT IN (SELECT campaign_id FROM su2_campaign ORDER BY started_at DESC LIMIT 30) AND NOT EXISTS(SELECT 1 FROM su2_league l WHERE l.genome_key=g.genome_key AND l.state='ACTIVE') AND NOT EXISTS(SELECT 1 FROM su2_certificate c WHERE c.genome_key=g.genome_key AND c.expires_at>?) ORDER BY genome_key LIMIT 16",new String[]{Long.toString(System.currentTimeMillis())})){while(c.moveToNext())keys.add(c.getString(0));}
        for(String key:keys){db.execSQL("DELETE FROM su2_lineage WHERE child_key=?",new Object[]{key});db.execSQL("DELETE FROM su2_model_genome WHERE genome_key=? AND state='RESEARCH'",new Object[]{key});}
        return keys.size();
    }
}
