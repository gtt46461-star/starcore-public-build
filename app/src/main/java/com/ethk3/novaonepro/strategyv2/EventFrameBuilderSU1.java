package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.r952.PITSnapshotContractR952;

/** Uses the canonical resolver and source availability; never synthesizes issue arithmetic. */
public final class EventFrameBuilderSU1 {
    private EventFrameBuilderSU1() {}
    public static long generation(android.database.sqlite.SQLiteDatabase db){
        try(android.database.Cursor c=db.rawQuery("SELECT generation FROM su1_source_clock WHERE singleton=1",null)){if(!c.moveToFirst())throw new IllegalStateException("SU1_SOURCE_CLOCK");return c.getLong(0);}
    }
    public static EventFrameSU1 capture(DbStore store,String target) {return capture(store,target,true);}
    public static EventFrameSU1 captureWithGapsR26(DbStore store,String target){return capture(store,target,false);}
    private static EventFrameSU1 capture(DbStore store,String target,boolean consecutive) {
        long generation=generation(LegacyResearchScopeR29.observationReader(store));
        PITSnapshotContractR952 pit=PITSnapshotContractR952.capture(store,target,5000);
        if(consecutive){pit=pit.newestContiguous();pit.requireConsecutive();}
        long now=System.currentTimeMillis();
        long deadline=TargetIssueResolver.predrawDeadlineWallR20261008(pit.history.get(0));
        int n=pit.history.size();long[] issues=new long[n],availability=new long[n];int[][] values=new int[n][10];
        for(int i=0;i<n;i++) {
            Models.Draw d=pit.history.get(n-1-i);issues[i]=Long.parseLong(d.issue);
            availability[i]=d.fetchTime;
            for(int p=0;p<10;p++)values[i][p]=d.getNumberAtPosition(p+1);
        }
        if(generation!=generation(LegacyResearchScopeR29.observationReader(store)))throw new IllegalStateException("SU1_SOURCE_CHANGED_DURING_READ");
        return new EventFrameSU1(Long.parseLong(target),deadline,now,"CANONICAL_PK10_V1",issues,availability,values,generation);
    }
    public static boolean unchanged(DbStore store,EventFrameSU1 expected) {
        if(System.currentTimeMillis()>=expected.targetOpenTime||store.getDrawByIssue(String.valueOf(expected.targetIssue))!=null)return false;
        return generation(LegacyResearchScopeR29.observationReader(store))==expected.sourceGeneration;
    }
}
