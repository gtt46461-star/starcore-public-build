package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.ResearchEventBridgeR29;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Real validated-receipt and post-commit callback clocks. Provider scheduled open is a separate field;
 * source publication time is UNKNOWN. No inferred source timestamp is manufactured from device receipt time.
 * The callback records only memory; the next accepted draw transaction persists it without delaying dispatch. */
public final class FetchObservationR29 {
    private FetchObservationR29(){}
    public static final String DDL="CREATE TABLE IF NOT EXISTS r29_fetch_observation(issue TEXT NOT NULL,source_hash TEXT NOT NULL,first_validated_wall INTEGER NOT NULL,first_validated_mono INTEGER NOT NULL,clock_session TEXT NOT NULL,provider_scheduled_open INTEGER,source_available_wall INTEGER,accepted_in_transaction_wall INTEGER NOT NULL,accepted_in_transaction_mono INTEGER NOT NULL,commit_observed_wall INTEGER,commit_observed_mono INTEGER,origin TEXT NOT NULL,PRIMARY KEY(issue,source_hash))";
    private static final ConcurrentHashMap<String,Seen> SEEN=new ConcurrentHashMap<>();private static final ConcurrentLinkedQueue<String> ORDER=new ConcurrentLinkedQueue<>();
    private static final class Seen{
        final long wall,mono,scheduled;final String session;final AtomicReference<long[]> commit=new AtomicReference<>();
        Seen(Models.Draw d){wall=System.currentTimeMillis();mono=TimeSourceR811.monoNowMs();scheduled=d.openTime;session=TimeSourceR811.clockSessionCP12();}
    }
    static String hash(Models.Draw d){return ResearchEventBridgeR29.Event.draw(0,d.issue,d.numbers,"VALIDATED_LATEST",0).hash;}
    public static void validated(Models.Draw draw){
        if(draw==null||!draw.isValid())return;String hash=hash(draw);Seen seen=new Seen(draw);
        if(SEEN.putIfAbsent(hash,seen)==null)ORDER.offer(hash);
        while(SEEN.size()>256){String old=ORDER.poll();if(old==null)break;SEEN.remove(old);}
    }
    public static void committed(Models.Draw draw){if(draw==null||!draw.isValid())return;Seen seen=SEEN.get(hash(draw));if(seen!=null)seen.commit.compareAndSet(null,new long[]{System.currentTimeMillis(),TimeSourceR811.monoNowMs()});}
    public static void acceptedWithin(SQLiteDatabase realtime,String issue,String hash){
        Seen seen=SEEN.get(hash);if(seen==null)return;if(!realtime.inTransaction())throw new IllegalStateException("R29_FETCH_OBSERVATION_TRANSACTION");
        ResearchEventBridgeR29.ensureFetchObservationSchema(realtime);
        realtime.execSQL("INSERT OR IGNORE INTO r29_fetch_observation VALUES(?,?,?,?,?,?,NULL,?,?,NULL,NULL,'API_LATEST_VALIDATED')",new Object[]{issue,hash,seen.wall,seen.mono,seen.session,seen.scheduled>0?seen.scheduled:null,System.currentTimeMillis(),TimeSourceR811.monoNowMs()});
        long[] commit=seen.commit.get();if(commit!=null)realtime.execSQL("UPDATE r29_fetch_observation SET commit_observed_wall=?,commit_observed_mono=? WHERE issue=? AND source_hash=? AND commit_observed_wall IS NULL AND clock_session=? AND first_validated_wall=?",new Object[]{commit[0],commit[1],issue,hash,seen.session,seen.wall});
    }
}
