package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Bounded research turnover. Resource retirement is not a statistical quality verdict. */
final class ResearchPopulationR966 {
    static final int CAPACITY=64;
    static final String ACTIVE="status NOT LIKE 'PRUNED%' AND status NOT LIKE 'QUARANTINED%' AND status NOT IN ('RETIRED','ARCHIVED')";
    private ResearchPopulationR966(){}
    static int active(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE "+ACTIVE,null)){return c.moveToFirst()?c.getInt(0):0;}
    }
    /** Empty means free capacity; null means no safely replaceable research member. */
    static String replacement(SQLiteDatabase db){
        if(active(db)<CAPACITY)return "";
        return eligible(db);
    }
    /** A finished historical child can take its OWN untrained parent's resource slot.
     * This is not promotion or a quality verdict. Live updates, formal calibration,
     * windows, challenges, elites and certificates protect the parent. Unsettled proxy
     * predictions alone do not reserve a live population slot forever: their immutable
     * program, lineage and outcomes remain available to the settlement owner. Retirement
     * and child registration still commit together; no historical evidence is inherited.
     */
    static String replacementForDevelopment(SQLiteDatabase db,String parent) {
        if(!db.inTransaction())throw new IllegalStateException("R980_DEVELOPMENT_HANDOFF_TRANSACTION");
        if(active(db)<CAPACITY)return "";
        if(parent!=null&&!parent.isEmpty()) {
            String sql="SELECT 1 FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" g JOIN "+ResearchLifecycleR932.TABLE
                    +" l ON l.candidate_key='algorithm:'||g.genome_id WHERE g.genome_id=?"
                    +" AND g.future_n=0 AND g.state_updates=0 AND g.dev_n=0 AND l.state IN ('IDEA','NURSERY')"
                    +" AND g.status NOT LIKE 'PRUNED%' AND g.status NOT LIKE 'QUARANTINED%' AND g.status NOT IN ('RETIRED','ARCHIVED')"
                    +" AND NOT EXISTS(SELECT 1 FROM "+CandidateCalibrationR932.OBS+" o WHERE o.candidate_key=l.candidate_key)"
                    +" AND NOT EXISTS(SELECT 1 FROM "+FixedResearchWindowR932.WINDOW+" w WHERE w.candidate_key=l.candidate_key)"
                    +" AND NOT EXISTS(SELECT 1 FROM "+ResearchLifecycleR932.ELITE+" e WHERE e.candidate_key=l.candidate_key)"
                    +(ResearchGatePlanR967.installed().isCp3?" AND NOT EXISTS(SELECT 1 FROM "+FixedResearchWindowR932.WINDOW+" cp4 WHERE cp4.candidate_key=l.candidate_key AND cp4.state='OPEN')":" AND NOT EXISTS(SELECT 1 FROM "+PairedResearchChallengeR932.TABLE+" p WHERE p.state='OPEN' AND (p.parent_key=l.candidate_key OR p.child_key=l.candidate_key))")
                    +" AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c WHERE c.consumer='INVENTED_ALGORITHM' AND c.candidate_id=g.genome_id AND c.state='ACTIVE')";
            try(Cursor c=db.rawQuery(sql,new String[]{parent})){if(c.moveToFirst())return parent;}
        }
        return eligible(db);
    }
    /** Old builds could publish trained children beyond the cap. Drain at most eight per slice. */
    static boolean normalize(SQLiteDatabase db,String issue){
        if(!db.inTransaction())throw new IllegalStateException("R966_POPULATION_TRANSACTION_REQUIRED");
        for(int i=0;i<8&&active(db)>CAPACITY;i++){
            ResearchWorkBudgetR927.checkpoint();String victim=eligible(db);
            if(victim==null)return false;
            retire(db,victim,"LEGACY_OVERCAPACITY_REBALANCE",issue);
        }
        return active(db)<=CAPACITY;
    }
    private static String eligible(SQLiteDatabase db){
        // A completed 120-label research observation, or a parent whose structural child
        // has its own 12 labels, can release a slot. Preserve elite/certified/reference members.
        String sql="SELECT g.genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" g JOIN "+ResearchLifecycleR932.TABLE+" l ON l.candidate_key='algorithm:'||g.genome_id"
                +" WHERE g.status NOT LIKE 'PRUNED%' AND g.status NOT LIKE 'QUARANTINED%' AND g.status NOT IN ('RETIRED','ARCHIVED')"
                +" AND l.state IN ('NURSERY','OBSERVING') AND (g.future_n>=120 OR (g.future_n>=12 AND EXISTS(SELECT 1 FROM "
                +DirectedResearchCreationR932.ATTEMPT+" a JOIN "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" child ON a.child_key='algorithm:'||child.genome_id"
                +" WHERE a.parent_key=l.candidate_key AND a.state='CREATED' AND a.kind='PREDRAW_FAILURE_REPAIR' AND child.future_n>=12"
                +" AND child.status NOT LIKE 'PRUNED%' AND child.status NOT LIKE 'QUARANTINED%' AND child.status NOT IN ('RETIRED','ARCHIVED'))))"
                +" AND NOT EXISTS(SELECT 1 FROM "+ResearchLifecycleR932.ELITE+" e WHERE e.candidate_key=l.candidate_key)"
                +(ResearchGatePlanR967.installed().isCp3?" AND NOT EXISTS(SELECT 1 FROM "+FixedResearchWindowR932.WINDOW+" cp4 WHERE cp4.candidate_key=l.candidate_key AND cp4.state='OPEN')":" AND NOT EXISTS(SELECT 1 FROM "+PairedResearchChallengeR932.TABLE+" p WHERE p.state='OPEN' AND (p.parent_key=l.candidate_key OR p.child_key=l.candidate_key))")
                +" AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c WHERE c.consumer='INVENTED_ALGORITHM' AND c.candidate_id=g.genome_id AND c.state='ACTIVE')"
                +" ORDER BY g.future_hit*1.0/MAX(1,g.future_n),g.generation,g.genome_id LIMIT 1";
        try(Cursor c=db.rawQuery(sql,null)){
            while(c.moveToNext()){
                ResearchWorkBudgetR927.checkpoint();String id=c.getString(0);
                return id;
            }
        }
        return null;
    }
    static void committed(SQLiteDatabase db,String victim,String child,String issue){
        if(!db.inTransaction())throw new IllegalStateException("R966_POPULATION_TRANSACTION_REQUIRED");
        retire(db,victim,child,issue);
        if(active(db)>CAPACITY)throw new IllegalStateException("R966_POPULATION_CAPACITY_COMMIT");
    }
    static void archiveReplaced(SQLiteDatabase db,String parent,String child,String proof,String issue){
        if(!parent.startsWith("algorithm:")||!child.startsWith("algorithm:"))return;
        String id=parent.substring(10);
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{DbHelper.TABLE_R908_ALGORITHM_GENOME})){if(!c.moveToFirst())return;}
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{id})){if(!c.moveToFirst())return;}
        // Program/spec/lineage and historical evidence remain intact. Revoke only future authority.
        try(Cursor c=db.rawQuery("SELECT certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE consumer='INVENTED_ALGORITHM' AND candidate_id=? AND state='ACTIVE'",new String[]{id})){
            while(c.moveToNext())ResearchPlatformStoreR929.revokeInTransaction(db,c.getString(0),"R966_PAIRED_SUCCESSOR|"+child+'|'+proof,System.currentTimeMillis());
        }
        retire(db,id,child+"|PAIRED_REPLACEMENT_PROOF="+proof,issue);
    }
    private static void retire(SQLiteDatabase db,String victim,String child,String issue){
        if(victim!=null&&!victim.isEmpty()){
            String key="algorithm:"+victim,reason="R966_RESOURCE_REPLACEMENT|child="+child+"|NOT_QUALITY_OR_CERTIFICATE_VERDICT";
            int n=0;try(Cursor c=db.rawQuery("SELECT future_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{victim})){if(c.moveToFirst())n=c.getInt(0);}
            ResearchLifecycleR932.transition(db,key,ResearchLifecycleR932.State.RESOURCE_RETIRED,reason,issue,V51ChecksumEngine.sha256(reason+'|'+key+'|'+issue),n,0);
        }
    }

static int activeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db){
        try(Cursor c=report.select("SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE "+ACTIVE,null)){return c.moveToFirst()?c.getInt(0):0;}
    }
}
