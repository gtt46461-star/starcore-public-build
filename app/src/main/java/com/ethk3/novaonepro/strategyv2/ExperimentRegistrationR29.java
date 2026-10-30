package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cross-database registration handshake. The realtime writer fixes the first future issue in its draw transaction
 * order. A research transaction must never infer that boundary from a stale draw mirror. Candidates remain pending
 * until the immutable authority event is consumed; the permanent research index is never reallocated on retry. */
public final class ExperimentRegistrationR29 {
    private ExperimentRegistrationR29() {}
    public static final String KIND="EXPERIMENT_REGISTERED", PENDING="PENDING_REALTIME_REGISTRATION";
    public static final String AUTHORITY="CREATE TABLE IF NOT EXISTS r29_registration_authority(experiment_id TEXT PRIMARY KEY,global_index INTEGER NOT NULL UNIQUE,spec_hash TEXT NOT NULL,first_ordinal INTEGER NOT NULL,registered_at INTEGER NOT NULL,event_id TEXT NOT NULL UNIQUE)";
    public static final String[] AUTHORITY_GUARDS={
        "CREATE TRIGGER IF NOT EXISTS r29_registration_authority_no_update BEFORE UPDATE ON r29_registration_authority BEGIN SELECT RAISE(ABORT,'R29_REGISTRATION_AUTHORITY_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r29_registration_authority_no_delete BEFORE DELETE ON r29_registration_authority BEGIN SELECT RAISE(ABORT,'R29_REGISTRATION_AUTHORITY_IMMUTABLE'); END"
    };
    public static final String[] RESEARCH_DDL={
        "CREATE TABLE IF NOT EXISTS r29_registration_request(experiment_id TEXT PRIMARY KEY,global_index INTEGER NOT NULL UNIQUE,spec_hash TEXT NOT NULL,desired_status TEXT NOT NULL,requested_at INTEGER NOT NULL,acked_at INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS r29_registration_receipt(experiment_id TEXT PRIMARY KEY,event_id TEXT NOT NULL UNIQUE,first_ordinal INTEGER NOT NULL,registered_at INTEGER NOT NULL,consumed_at INTEGER NOT NULL)",
        "CREATE TRIGGER IF NOT EXISTS r29_registration_receipt_immutable BEFORE UPDATE ON r29_registration_receipt BEGIN SELECT RAISE(ABORT,'R29_REGISTRATION_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r29_registration_receipt_no_delete BEFORE DELETE ON r29_registration_receipt BEGIN SELECT RAISE(ABORT,'R29_REGISTRATION_IMMUTABLE'); END"
    };
    public static void request(SQLiteDatabase research,String id,long index,String specHash,String desired,long now){
        if(!research.inTransaction())throw new IllegalStateException("R29_REGISTRATION_TRANSACTION_REQUIRED");
        research.execSQL("INSERT OR IGNORE INTO r29_registration_request(experiment_id,global_index,spec_hash,desired_status,requested_at) VALUES(?,?,?,?,?)",new Object[]{id,index,specHash,desired,now});
        try(Cursor c=research.rawQuery("SELECT global_index,spec_hash FROM r29_registration_request WHERE experiment_id=?",new String[]{id})){
            if(!c.moveToFirst()||c.getLong(0)!=index||!specHash.equals(c.getString(1)))throw new IllegalStateException("R29_REGISTRATION_REQUEST_CONFLICT");
        }
    }
    public static void desiredStatus(SQLiteDatabase db,String id,String status){
        db.execSQL("UPDATE r29_registration_request SET desired_status=? WHERE experiment_id=? AND acked_at=0",new Object[]{status,id});
        db.execSQL("UPDATE r27_live_shadow SET status=? WHERE policy_hash=? AND status<>?",new Object[]{status,id,PENDING});
    }
    static final class Request{
        final String id,hash;final long index;
        Request(String id,long index,String hash){this.id=id;this.index=index;this.hash=hash;}
    }
    public static long publishRequests(ResearchPoolR27.Worker worker){
        List<Request> pending=new ArrayList<>();
        try(Cursor c=worker.ro.rawQuery("SELECT experiment_id,global_index,spec_hash FROM r29_registration_request WHERE acked_at=0 ORDER BY global_index LIMIT 16",null)){
            while(c.moveToNext())pending.add(new Request(c.getString(0),c.getLong(1),c.getString(2)));
        }
        if(pending.isEmpty())return 0;
        return worker.store().coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_REGISTER_FUTURE_BOUNDARY",db->{
            long created=0;for(Request request:pending){publish(db,request,System.currentTimeMillis());created++;}return created;
        });
    }
    static void publish(SQLiteDatabase realtime,Request request,long now){
        if(!realtime.inTransaction())throw new IllegalStateException("R29_REGISTRATION_REALTIME_TRANSACTION_REQUIRED");
        ResearchEventBridgeR29.ensureRealtime(realtime);
        long ordinal,registered;
        try(Cursor c=realtime.rawQuery("SELECT global_index,spec_hash,first_ordinal,registered_at FROM r29_registration_authority WHERE experiment_id=?",new String[]{request.id})){
            if(c.moveToFirst()){
                if(c.getLong(0)!=request.index||!request.hash.equals(c.getString(1)))throw new IllegalStateException("R29_REGISTRATION_AUTHORITY_CONFLICT");
                ordinal=c.getLong(2);registered=c.getLong(3);
            }else{
                String latest=RuntimeR27.latestIssue(realtime);
                if(latest==null)throw new IllegalStateException("R29_REGISTRATION_WAITING_AUTHORITATIVE_DRAW");
                ordinal=TargetIssueResolver.sequenceOrdinalSU1(latest)+1;registered=now;
            }
        }
        Map<String,Object> p=new LinkedHashMap<>();p.put("abi","R29_REALTIME_REGISTRATION_V1");p.put("experimentId",request.id);p.put("globalIndex",request.index);p.put("specHash",request.hash);p.put("firstOrdinal",ordinal);p.put("registeredAt",registered);
        String issue=TargetIssueResolver.fromSequenceOrdinalSU1(ordinal),payload=JsonR27.write(p),hash=StatsR27.sha256(payload);
        ResearchEventBridgeR29.Event event=new ResearchEventBridgeR29.Event(0,StatsR27.sha256(KIND+"|"+issue+"|"+hash),issue,payload,hash,"REALTIME_REGISTRATION_AUTHORITY",registered,KIND);
        realtime.execSQL("INSERT OR IGNORE INTO r29_registration_authority(experiment_id,global_index,spec_hash,first_ordinal,registered_at,event_id) VALUES(?,?,?,?,?,?)",new Object[]{request.id,request.index,request.hash,ordinal,registered,event.id});
        ResearchEventBridgeR29.append(realtime,event);
    }
    static void validate(String issue,Map<String,Object> p){
        if(!"R29_REALTIME_REGISTRATION_V1".equals(p.get("abi"))||!(p.get("experimentId") instanceof String)||!((String)p.get("experimentId")).matches("[0-9a-f]{64}")
                ||!(p.get("specHash") instanceof String)||!((String)p.get("specHash")).matches("[0-9a-f]{64}")
                ||integer(p,"globalIndex")<=0||integer(p,"registeredAt")<=0||integer(p,"firstOrdinal")!=TargetIssueResolver.sequenceOrdinalSU1(issue))throw new IllegalArgumentException("R29_INVALID_REGISTRATION_EVENT");
    }
    static long integer(Map<String,Object> p,String key){Object v=p.get(key);if(!(v instanceof Number)||((Number)v).doubleValue()!=((Number)v).longValue())throw new IllegalArgumentException("R29_REGISTRATION_INTEGER:"+key);return ((Number)v).longValue();}
    @SuppressWarnings("unchecked")
    static void consume(SQLiteDatabase research,ResearchEventBridgeR29.Event event,long now){
        Map<String,Object> p=(Map<String,Object>)JsonR27.parse(event.payload);validate(event.issue,p);
        String id=(String)p.get("experimentId"),desired;long index=integer(p,"globalIndex"),first=integer(p,"firstOrdinal"),registered=integer(p,"registeredAt");
        try(Cursor c=research.rawQuery("SELECT r.global_index,r.spec_hash,r.desired_status,s.global_index,s.spec_json,s.semantic_abi,s.base_policy FROM r29_registration_request r JOIN r27_live_shadow s ON s.policy_hash=r.experiment_id WHERE r.experiment_id=?",new String[]{id})){
            if(!c.moveToFirst()||c.getLong(0)!=index||c.getLong(3)!=index||!p.get("specHash").equals(c.getString(1))||!c.getString(1).equals(StatsR27.sha256(c.getString(4)+"|"+c.getString(5)+"|"+c.getString(6))))throw new IllegalStateException("R29_REGISTRATION_SOURCE_IDENTITY_MISMATCH");desired=c.getString(2);
        }
        research.execSQL("INSERT OR IGNORE INTO r29_qual_calendar(experiment_id,first_ordinal,registered_at,origin) VALUES(?,?,?,'REALTIME_AUTHORITY_EVENT')",new Object[]{id,first,registered});
        try(Cursor c=research.rawQuery("SELECT first_ordinal,registered_at,origin FROM r29_qual_calendar WHERE experiment_id=?",new String[]{id})){
            if(!c.moveToFirst()||c.getLong(0)!=first||c.getLong(1)!=registered||!"REALTIME_AUTHORITY_EVENT".equals(c.getString(2)))throw new IllegalStateException("R29_REGISTRATION_CALENDAR_CONFLICT");
        }
        research.execSQL("INSERT OR IGNORE INTO r29_registration_receipt(experiment_id,event_id,first_ordinal,registered_at,consumed_at) VALUES(?,?,?,?,?)",new Object[]{id,event.id,first,registered,now});
        research.execSQL("UPDATE r27_live_shadow SET status=?,updated_at=? WHERE policy_hash=? AND status=?",new Object[]{desired,now,id,PENDING});
        research.execSQL("UPDATE r29_registration_request SET acked_at=? WHERE experiment_id=? AND acked_at=0",new Object[]{now,id});
    }
}
