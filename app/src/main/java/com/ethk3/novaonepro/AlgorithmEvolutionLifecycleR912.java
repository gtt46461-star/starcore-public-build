package com.ethk3.novaonepro;

import android.database.Cursor;

/** Compatibility entry; R932 owns the live three-consumer state machine. */
public final class AlgorithmEvolutionLifecycleR912 {
    public static final int MAX_RUNNABLE=8;
    /** Historical R925 constant only. Live scheduling uses the global 8/24/64 roster. */
    @Deprecated public static final int MAX_OBSERVATION_COHORT_R925=32;
    public static final int MAX_ASSESSMENTS_PER_TICK_R923=ResearchLifecycleR932.MAX_REVIEWS_PER_PULSE_R961;
    private AlgorithmEvolutionLifecycleR912(){}
    public static Result maintain(DbStore store){
        if(store==null)return new Result(0,0,0,0,"STORE_NULL");
        int assessed=ResearchLifecycleR932.maintain(store),challenger=0,observing=0,pruned=0;
        int qualityPruned=0,resourceRetired=0,quarantined=0;
        try(Cursor c=store.readable().rawQuery("SELECT state,COUNT(*) FROM "+ResearchLifecycleR932.TABLE+" GROUP BY state",null)){
            while(c.moveToNext()){
                String state=c.getString(0);int n=c.getInt(1);
                if(state.equals("QUALITY_PRUNED"))qualityPruned+=n;
                else if(state.equals("RESOURCE_RETIRED"))resourceRetired+=n;
                else if(state.equals("QUARANTINED"))quarantined+=n;
                if(state.equals("CHALLENGER")||state.equals("SHADOW")||state.equals("INFLUENCE")||state.equals("CHAMPION"))challenger+=n;
                else if(state.equals("QUALITY_PRUNED")||state.equals("RESOURCE_RETIRED")||state.equals("QUARANTINED"))pruned+=n;
                else observing+=n;
            }
        }
        AtomQualityLedgerR925.maintain(store);
        String detail="R961_PER_CANDIDATE_ATOMIC_LIFECYCLE|assessed="+assessed+"|limit="+MAX_ASSESSMENTS_PER_TICK_R923+"|challenger_or_later="+challenger+"|observing="+observing+"|closed="+pruned+"|quality_pruned="+qualityPruned+"|resource_retired="+resourceRetired+"|quarantined="+quarantined+"|CERTIFICATE_UPDATE_PRIORITY|ALL_EXECUTABLE_PER_ISSUE_R943|NO_CAPACITY_QUALITY_PRUNE|FIXED_ENDPOINTS_ONLY";
        if(ResearchLifecycleR932.hasPendingPeriodReview(store))detail="LIFECYCLE_PENDING|"+detail;
        store.setState("r912_algorithm_lifecycle",detail);
        return new Result(assessed,challenger,observing,pruned,detail);
    }
    public static final class Result {
        public final int assessed,challenger,observing,pruned;public final String detail;
        Result(int a,int c,int o,int p,String d){assessed=a;challenger=c;observing=o;pruned=p;detail=d;}
    }
}
