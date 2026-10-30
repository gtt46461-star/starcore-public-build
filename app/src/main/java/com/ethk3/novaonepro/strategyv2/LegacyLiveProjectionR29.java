package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.util.*;

/** Small versioned realtime references to immutable research checkpoints. No learner state crosses this bridge.
 * Research commits its state and pending revision together. The realtime owner commits the projection separately;
 * lost acknowledgements are harmless, and a freeze requires exactly the captured research revision. */
public final class LegacyLiveProjectionR29 {
    private LegacyLiveProjectionR29(){}
    public static final String PENDING="CREATE TABLE IF NOT EXISTS r29_legacy_live_pending(policy_hash TEXT PRIMARY KEY,revision INTEGER NOT NULL,acked_revision INTEGER NOT NULL DEFAULT 0)";
    public static final String PROJECTION="CREATE TABLE IF NOT EXISTS r29_legacy_live_projection(policy_hash TEXT PRIMARY KEY,route_epoch INTEGER NOT NULL,revision INTEGER NOT NULL,checkpoint_hash TEXT,state_hash TEXT,through_ordinal INTEGER,status TEXT NOT NULL,payload_hash TEXT NOT NULL,projected_at INTEGER NOT NULL)";
    public static void ensureRealtime(SQLiteDatabase db){db.execSQL(PROJECTION);}
    public static void installResearch(SQLiteDatabase db){
        db.execSQL(PENDING);
        for(String event:new String[]{"INSERT","UPDATE","DELETE"}){
            String row=event.equals("DELETE")?"OLD":"NEW",name="r29_legacy_live_"+event.toLowerCase(java.util.Locale.ROOT);
            // Outer INSERT OR REPLACE also controls conflict handling inside triggers. Never use OR IGNORE here.
            String sql="CREATE TRIGGER "+name+" AFTER "+event+" ON r26_live_model BEGIN UPDATE r29_legacy_live_pending SET revision=revision+1 WHERE policy_hash="+row+".policy_hash; INSERT INTO r29_legacy_live_pending SELECT "+row+".policy_hash,1,0 WHERE NOT EXISTS(SELECT 1 FROM r29_legacy_live_pending WHERE policy_hash="+row+".policy_hash); END";
            String old=null;try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='trigger' AND name=?",new String[]{name})){if(c.moveToFirst())old=c.getString(0);}
            if(!sql.equals(old)){if(old!=null){if(!db.inTransaction())throw new IllegalStateException("R29_LEGACY_PROJECTION_TRIGGER_UPGRADE_REQUIRES_TRANSACTION");db.execSQL("DROP TRIGGER "+name);}db.execSQL(sql);}
        }
        db.execSQL("INSERT OR IGNORE INTO r29_legacy_live_pending SELECT policy_hash,1,0 FROM r26_live_model");
    }
    static final class Row{
        String policy,checkpoint,hash,status;long revision,through;
        String payload(){return StatsR27.sha256(policy+"|"+revision+"|"+checkpoint+"|"+hash+"|"+through+"|"+status);}
    }
    static boolean apply(SQLiteDatabase realtime,Row row,long epoch,long now){
        if(!realtime.inTransaction())throw new IllegalStateException("R29_LEGACY_PROJECTION_REQUIRES_REALTIME_TRANSACTION");
        long current=MigrationCopyR29.scalar(realtime,"SELECT epoch FROM r29_database_route WHERE singleton=1 AND phase='ACTIVE'",null);
        if(current!=epoch)throw new IllegalStateException("R29_LEGACY_PROJECTION_ROUTE_EPOCH_CHANGED");
        String payload=row.payload();
        try(Cursor c=realtime.rawQuery("SELECT route_epoch,revision,payload_hash FROM r29_legacy_live_projection WHERE policy_hash=?",new String[]{row.policy})){
            if(c.moveToFirst()){
                long previousEpoch=c.getLong(0),previousRevision=c.getLong(1);
                if(previousEpoch>epoch||previousEpoch==epoch&&previousRevision>row.revision)return false;
                if(previousEpoch==epoch&&previousRevision==row.revision){if(!payload.equals(c.getString(2)))throw new IllegalStateException("R29_LEGACY_PROJECTION_SAME_REVISION_CONFLICT");return false;}
            }
        }
        realtime.execSQL("INSERT OR REPLACE INTO r29_legacy_live_projection VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{row.policy,epoch,row.revision,row.checkpoint,row.hash,row.through,row.status,payload,now});return true;
    }
    public static long drain(DbStore store){
        SQLiteDatabase original=LegacyResearchScopeR29.realtime(store.readable());
        if(original.inTransaction())throw new IllegalStateException("R29_LEGACY_PROJECTION_MUST_NOT_HOLD_REALTIME_TRANSACTION");
        if(!ResearchRouteR29.active(original))return 0;
        ResearchRouteR29.write(store,"R29_LEGACY_LIVE_PROJECTION_SETUP",db->{installResearch(db);return 0;});
        SQLiteDatabase research=ResearchRouteR29.reader(original);long epoch=MigrationCopyR29.scalar(research,"SELECT epoch FROM main.r29_research_owner WHERE singleton=1 AND phase='READY'",null);
        List<Row> rows=new ArrayList<>();
        try(Cursor c=research.rawQuery("SELECT q.policy_hash,q.revision,m.checkpoint_hash,m.state_hash,m.through_ordinal,m.status FROM r29_legacy_live_pending q LEFT JOIN r26_live_model m ON m.policy_hash=q.policy_hash WHERE q.revision>q.acked_revision ORDER BY q.policy_hash LIMIT 64",null)){
            while(c.moveToNext()){Row r=new Row();r.policy=c.getString(0);r.revision=c.getLong(1);r.checkpoint=c.isNull(2)?null:c.getString(2);r.hash=c.isNull(3)?null:c.getString(3);r.through=c.isNull(4)?-1:c.getLong(4);r.status=c.isNull(5)?"REMOVED":c.getString(5);rows.add(r);}
        }
        if(rows.isEmpty())return 0;
        long applied=LegacyResearchScopeR29.realtimeWrite(store,"R29_LEGACY_LIVE_PROJECTION",db->{ensureRealtime(db);long changed=0;for(Row r:rows)if(apply(db,r,epoch,System.currentTimeMillis()))changed++;return changed;});
        ResearchRouteR29.write(store,"R29_LEGACY_LIVE_PROJECTION_ACK",db->{for(Row r:rows)db.execSQL("UPDATE r29_legacy_live_pending SET acked_revision=max(acked_revision,?) WHERE policy_hash=? AND revision>=?",new Object[]{r.revision,r.policy,r.revision});return 0;});return applied;
    }
    static long revision(SQLiteDatabase research,String policy){return MigrationCopyR29.scalar(research,"SELECT revision FROM r29_legacy_live_pending WHERE policy_hash=?",new String[]{policy});}
    static boolean matches(SQLiteDatabase realtime,String policy,String checkpoint,String hash,long through,long revision,long epoch){
        if(revision<=0||epoch<=0)return false;
        try(Cursor c=realtime.rawQuery("SELECT 1 FROM r29_legacy_live_projection p JOIN r29_database_route r ON r.singleton=1 AND r.phase='ACTIVE' AND r.epoch=p.route_epoch WHERE p.policy_hash=? AND p.checkpoint_hash=? AND p.state_hash=? AND p.through_ordinal=? AND p.status='ACTIVE' AND p.revision=? AND p.route_epoch=?",new String[]{policy,checkpoint,hash,Long.toString(through),Long.toString(revision),Long.toString(epoch)})){return c.moveToFirst();}
    }
}
