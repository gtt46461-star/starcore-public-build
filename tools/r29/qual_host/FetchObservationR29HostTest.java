package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/** Accepted-draw timing rows roll back with the canonical draw and retain UNKNOWN provider publication time. */
public final class FetchObservationR29HostTest {
    static int checks;static void check(String name,boolean ok){System.out.println("R29_FETCH|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){return EventBridgeR29HostTest.count(db,sql);}
    public static void main(String[]args)throws Exception{
        try(SQLiteDatabase db=EventBridgeR29HostTest.open(false)){
            Models.Draw d=new Models.Draw();d.issue="202610300011";d.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};d.openTime=System.currentTimeMillis();FetchObservationR29.validated(d);
            boolean killed=false;try{EventBridgeR29HostTest.publish(db,d.issue,true);}catch(IllegalStateException expected){killed=true;}
            check("F1_DRAW_AND_OBSERVATION_ROLL_BACK_TOGETHER",killed&&n(db,"SELECT count(*) FROM r29_fetch_observation")==0&&n(db,"SELECT count(*) FROM draws")==0);
            EventBridgeR29HostTest.publish(db,d.issue,false);
            check("F2_VALIDATED_RECEIPT_HAS_WALL_MONOTONIC_AND_SESSION",n(db,"SELECT count(*) FROM r29_fetch_observation WHERE first_validated_wall>0 AND first_validated_mono>0 AND length(clock_session)>0")==1);
            check("F3_SOURCE_PUBLICATION_IS_NOT_INVENTED_FROM_RECEIPT",n(db,"SELECT count(*) FROM r29_fetch_observation WHERE source_available_wall IS NULL AND provider_scheduled_open IS NOT NULL")==1);
            check("F4_TRANSACTION_TIMESTAMP_IS_NOT_A_POST_COMMIT_RECEIPT",n(db,"SELECT count(*) FROM r29_fetch_observation WHERE commit_observed_wall IS NULL")==1);
            long first=n(db,"SELECT first_validated_wall FROM r29_fetch_observation");Thread.sleep(2);FetchObservationR29.committed(d);FetchObservationR29.validated(d);EventBridgeR29HostTest.publish(db,d.issue,false);
            check("F5_CALLBACK_TIME_IS_PERSISTED_WITHOUT_RESETTING_FIRST_RECEIPT",n(db,"SELECT first_validated_wall FROM r29_fetch_observation")==first&&n(db,"SELECT count(*) FROM r29_fetch_observation WHERE commit_observed_wall>=first_validated_wall AND commit_observed_mono>=first_validated_mono")==1);
            EventBridgeR29HostTest.publish(db,d.issue,false);check("F6_REPEATED_POLL_CANNOT_DUPLICATE_TIMING_SAMPLE",n(db,"SELECT count(*) FROM r29_fetch_observation")==1);
            EventBridgeR29HostTest.publish(db,"202610300012",false);check("F7_UNOBSERVED_HISTORY_IS_NOT_GIVEN_A_LIVE_RECEIPT",n(db,"SELECT count(*) FROM r29_fetch_observation")==1);
            check("F8_TIMING_AUTHORITY_STAYS_IN_REALTIME",!MigrationCopyR29.ownedResearch("r29_fetch_observation"));
        }
        System.out.println("R29_FETCH|ALL_PASS|checks="+checks+"|HOST_ONLY_REAL_NETWORK_TIMING_PENDING");
    }
}
