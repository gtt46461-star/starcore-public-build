package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.*;

/** A separately preregistered collection window. Never relabels old CONFIRM/FUTURE receipts. */
public final class OnlineResearchWindowR30R1 {
    private OnlineResearchWindowR30R1(){}
    public static final String STATUS="ONLINE_MODEL_ROLE_REGISTERED",VERSION="ONLINE_MODEL_COLLECTION_R30_REPAIR_1";
    public static final class Window {
        public final String id;public final long first,last,registeredAt;
        Window(String id,long first,long last,long at){this.id=id;this.first=first;this.last=last;registeredAt=at;}
        public void require(long ordinal,long observedAt){if(ordinal<first||ordinal>last||observedAt<=registeredAt)throw new IllegalStateException("ONLINE_RESEARCH_WINDOW_OR_AVAILABILITY_MISMATCH");}
    }
    public static Window find(SQLiteDatabase db,String dataset,long ordinal){
        ResearchInputPinR30R1.requireReady(db);
        try(Cursor c=db.rawQuery("SELECT run_id,from_ordinal,through_ordinal,recorded_at FROM r30r1_analysis_run WHERE dataset_hash=? AND policy_hash=? AND role='ONLINE_RESEARCH' AND status=? AND from_ordinal<=? AND through_ordinal>=? AND ("+ResearchInputPinR30R1.validRunSql("r30r1_analysis_run")+") ORDER BY from_ordinal DESC LIMIT 1",new String[]{dataset,HashingSU1.text(AtomNetSessionR30.OWNER),STATUS,String.valueOf(ordinal),String.valueOf(ordinal)})){
            return c.moveToFirst()?new Window(c.getString(0),c.getLong(1),c.getLong(2),c.getLong(3)):null;
        }
    }
    /** Register before collection, with one issue excluded for an in-flight HTTP request. */
    public static void register(ResearchPoolR27.Worker worker,String dataset)throws Exception{
        String head=RuntimeR27.latestIssue(worker.ro);if(head==null)return;
        long ordinal=TargetIssueResolver.sequenceOrdinalSU1(head),first=ordinal+2,last=first+1439,now=System.currentTimeMillis(),open=RuntimeR27.latestOpen(worker.ro);
        if(open<=0||now>=open+120000-5000)return; // A stale head cannot prove a future collection window.
        if(find(worker.ro,dataset,first)!=null)return;
        long revision=SchemaR27.liveSourceRevision(worker.ro);if(revision<0)return;
        Map<String,Object> contract=new LinkedHashMap<>();contract.put("version",VERSION);contract.put("owner",AtomNetSessionR30.OWNER);contract.put("role","ONLINE_RESEARCH");contract.put("fromOrdinal",first);contract.put("throughOrdinal",last);contract.put("registeredBeforeCollecting",true);contract.put("excludedInFlightOrdinal",ordinal+1);contract.put("source","new canonical draws collected after this registration; never formal confirmation or qualification tables");contract.put("qualityEligible",false);
        String json=JsonR27.write(contract),hash=HashingSU1.text(json),id=HashingSU1.fields(VERSION,dataset,hash,BuildConfig.SOURCE_COMMIT_R26);
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
        batch.guardBefore(db->head.equals(RuntimeR27.latestIssue(db))&&revision==SchemaR27.liveSourceRevision(db)&&System.currentTimeMillis()<open+120000-5000?0:-1);
        // Explicit column names avoid pretending that a collection contract is a completed analysis.
        batch.add("INSERT OR IGNORE INTO r30r1_analysis_run(run_id,dataset_hash,policy_hash,role,source_commit,source_revision,from_ordinal,through_ordinal,contract_hash,status,summary_json,recorded_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,dataset,HashingSU1.text(AtomNetSessionR30.OWNER),"ONLINE_RESEARCH",BuildConfig.SOURCE_COMMIT_R26,revision,first,last,hash,STATUS,json,now);
        batch.after=batch.before;
        try{RuntimeR27.submit(worker,"ONLINE_RESEARCH_WINDOW",batch);}catch(IllegalStateException stale){if(!QualificationInboxR29.precondition(stale))throw stale;}
    }
}
