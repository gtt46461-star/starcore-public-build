package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.*;

/** Research publishes only committed, versioned projections. The realtime receiver owns its own transaction and
 * retains original immutable freezes. Ack loss cannot repeat an increment, and stale revisions cannot roll back a
 * revocation. No realtime write transaction opens or waits for a research connection. */
public final class RealtimeResearchProjectionR29 {
    private RealtimeResearchProjectionR29(){}
    public static final String[] REALTIME_DDL={
        "CREATE TABLE IF NOT EXISTS r29_projection_applied(table_name TEXT NOT NULL,key_a TEXT NOT NULL,key_b INTEGER NOT NULL,epoch INTEGER NOT NULL,revision INTEGER NOT NULL,payload_hash TEXT NOT NULL,deleted INTEGER NOT NULL,applied_at INTEGER NOT NULL,PRIMARY KEY(table_name,key_a,key_b))",
        "CREATE TABLE IF NOT EXISTS r29_research_heartbeat(singleton INTEGER PRIMARY KEY CHECK(singleton=1),epoch INTEGER NOT NULL,through_seq INTEGER NOT NULL,seen_at INTEGER NOT NULL)"
    };
    public static final String PENDING="CREATE TABLE IF NOT EXISTS r29_projection_pending(table_name TEXT NOT NULL,key_a TEXT NOT NULL,key_b INTEGER NOT NULL,revision INTEGER NOT NULL,acked_revision INTEGER NOT NULL DEFAULT 0,queued_at INTEGER NOT NULL,PRIMARY KEY(table_name,key_a,key_b))";
    private static final String[] TABLES={"r27_live_shadow","r27_eprocess","r27_certificate","r27_live_frozen","r27_chart_cache","r26_evolution"};
    private static final Set<String> FROZEN_IMMUTABLE=new HashSet<>(Arrays.asList("target_issue","probabilities","n1","n2","n3","act","frozen_at","deadline_at","position","manifest_hash","input_through","commit_verified_at","commit_verified_mono","commit_session"));
    public static void ensureRealtime(SQLiteDatabase db){for(String sql:REALTIME_DDL)db.execSQL(sql);}
    static void supported(String table){if(!Arrays.asList(TABLES).contains(table))throw new IllegalArgumentException("R29_PROJECTION_TABLE");}
    static String keyColumn(String table){return table.equals("r26_evolution")?"attempt_id":table.equals("r27_chart_cache")?"panel_id":"policy_hash";}
    static String where(String table){return table.equals("r27_live_frozen")?"policy_hash=? AND target_ordinal=?":table.equals("r27_chart_cache")?"panel_id='mainline' AND key='choice'":keyColumn(table)+"=?";}
    static String[] args(String table,String a,long b){return table.equals("r27_live_frozen")?new String[]{a,String.valueOf(b)}:table.equals("r27_chart_cache")?null:new String[]{a};}
    public static void installResearch(SQLiteDatabase research){
        if(!research.inTransaction())throw new IllegalStateException("R29_PROJECTION_INSTALL_TRANSACTION");
        research.execSQL(PENDING);
        for(String table:TABLES)for(String operation:new String[]{"INSERT","UPDATE","DELETE"}){
            String row=operation.equals("DELETE")?"OLD":"NEW",a=table.equals("r27_chart_cache")?"'mainline'":row+"."+keyColumn(table),b=table.equals("r27_live_frozen")?row+".target_ordinal":"0";
            String condition=table.equals("r27_chart_cache")?" WHEN "+row+".panel_id='mainline' AND "+row+".key='choice'":table.equals("r26_evolution")?" WHEN "+row+".attempt_id LIKE 'R27:%'":"";
            research.execSQL("CREATE TRIGGER IF NOT EXISTS r29_project_"+table+"_"+operation.toLowerCase(Locale.ROOT)+" AFTER "+operation+" ON "+table+condition+" BEGIN INSERT OR IGNORE INTO r29_projection_pending(table_name,key_a,key_b,revision,queued_at) VALUES('"+table+"',"+a+","+b+",0,strftime('%s','now')*1000); UPDATE r29_projection_pending SET revision=revision+1,queued_at=strftime('%s','now')*1000 WHERE table_name='"+table+"' AND key_a="+a+" AND key_b="+b+"; END");
        }
        for(String table:new String[]{"r27_live_shadow","r27_eprocess","r27_certificate"})research.execSQL("INSERT OR IGNORE INTO r29_projection_pending(table_name,key_a,key_b,revision,queued_at) SELECT ?,policy_hash,0,1,? FROM "+table,new Object[]{table,System.currentTimeMillis()});
        research.execSQL("INSERT OR IGNORE INTO r29_projection_pending(table_name,key_a,key_b,revision,queued_at) SELECT 'r27_live_frozen',policy_hash,target_ordinal,1,? FROM r27_live_frozen WHERE target_ordinal>=(SELECT coalesce(max(target_ordinal),0)-1 FROM r27_live_frozen)",new Object[]{System.currentTimeMillis()});
        research.execSQL("INSERT OR IGNORE INTO r29_projection_pending(table_name,key_a,key_b,revision,queued_at) SELECT 'r27_chart_cache','mainline',0,1,? FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'",new Object[]{System.currentTimeMillis()});
    }
    static final class Row{
        String table,a,hash;long b,revision;List<String> columns;Object[] cells;boolean deleted;
    }
    static List<Row> capture(SQLiteDatabase research){
        List<Row> out=new ArrayList<>();
        try(Cursor q=research.rawQuery("SELECT table_name,key_a,key_b,revision FROM r29_projection_pending WHERE revision>acked_revision ORDER BY CASE WHEN table_name='r26_evolution' THEN 1 ELSE 0 END,queued_at,table_name,key_a LIMIT 32",null)){
            while(q.moveToNext()){
                Row row=new Row();row.table=q.getString(0);row.a=q.getString(1);row.b=q.getLong(2);row.revision=q.getLong(3);supported(row.table);
                row.columns=new ArrayList<>();try(Cursor c=research.rawQuery("PRAGMA main.table_info("+MigrationCopyR29.id(row.table)+")",null)){while(c.moveToNext())row.columns.add(c.getString(1));}
                try(Cursor c=research.rawQuery("SELECT * FROM main."+MigrationCopyR29.id(row.table)+" WHERE "+where(row.table),args(row.table,row.a,row.b))){
                    row.deleted=!c.moveToFirst();
                    if(row.deleted)row.hash=StatsR27.sha256("R29_PROJECTION_DELETE|"+row.table+"|"+row.a+"|"+row.b);
                    else{byte[] bytes=MigrationCopyR29.encode(c,0,row.columns.size());row.cells=ResearchCutoverR29.decode(bytes,row.columns.size());row.hash=StatsR27.sha256(bytes);}
                }
                if(row.table.equals("r27_live_frozen")&&!row.deleted&&"PREPARED".equals(row.cells[row.columns.indexOf("status")]))continue;
                try(Cursor c=research.rawQuery("SELECT revision FROM r29_projection_pending WHERE table_name=? AND key_a=? AND key_b=?",new String[]{row.table,row.a,String.valueOf(row.b)})){
                    if(c.moveToFirst()&&c.getLong(0)==row.revision)out.add(row);
                }
            }
        }
        return out;
    }
    static void apply(SQLiteDatabase realtime,Row row,long epoch,long now){
        long seenEpoch=-1,seenRevision=-1;String digest=null;
        try(Cursor c=realtime.rawQuery("SELECT epoch,revision,payload_hash FROM r29_projection_applied WHERE table_name=? AND key_a=? AND key_b=?",new String[]{row.table,row.a,String.valueOf(row.b)})){if(c.moveToFirst()){seenEpoch=c.getLong(0);seenRevision=c.getLong(1);digest=c.getString(2);}}
        if(seenEpoch>epoch||seenEpoch==epoch&&seenRevision>row.revision)return;
        if(seenEpoch==epoch&&seenRevision==row.revision){if(!row.hash.equals(digest))throw new IllegalStateException("R29_PROJECTION_REVISION_CONFLICT");return;}
        if(row.deleted){
            if(!row.table.equals("r26_evolution")&&!row.table.equals("r27_chart_cache"))realtime.execSQL("UPDATE r27_certificate SET status='REVOKED_SOURCE_REMOVED' WHERE policy_hash=?",new Object[]{row.a});
        }else{
            StringBuilder columns=new StringBuilder(),binds=new StringBuilder();
            for(String column:row.columns){if(columns.length()>0){columns.append(',');binds.append(',');}columns.append(MigrationCopyR29.id(column));binds.append('?');}
            realtime.execSQL("INSERT OR IGNORE INTO "+row.table+"("+columns+") VALUES("+binds+")",row.cells);
            if(row.table.equals("r27_live_frozen"))try(Cursor c=realtime.rawQuery("SELECT * FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new String[]{row.a,String.valueOf(row.b)})){
                if(!c.moveToFirst())throw new IllegalStateException("R29_PROJECTION_FREEZE_MISSING");
                for(int i=0;i<row.columns.size();i++)if(FROZEN_IMMUTABLE.contains(row.columns.get(i))){Object actual=c.isNull(i)?null:c.getType(i)==Cursor.FIELD_TYPE_BLOB?c.getBlob(i):c.getType(i)==Cursor.FIELD_TYPE_STRING?c.getString(i):c.getLong(i);Object wanted=row.cells[i];boolean equal=actual instanceof byte[]&&wanted instanceof byte[]?Arrays.equals((byte[])actual,(byte[])wanted):Objects.equals(actual,wanted);if(!equal)throw new IllegalStateException("R29_PROJECTION_IMMUTABLE_FREEZE_CONFLICT");}
            }
            StringBuilder update=new StringBuilder();List<Object> values=new ArrayList<>();
            for(int i=0;i<row.columns.size();i++){
                String column=row.columns.get(i);
                if(column.equals(keyColumn(row.table))||row.table.equals("r27_live_frozen")&&(column.equals("target_ordinal")||FROZEN_IMMUTABLE.contains(column))||row.table.equals("r27_chart_cache")&&column.equals("key"))continue;
                if(update.length()>0)update.append(',');update.append(MigrationCopyR29.id(column)).append("=?");values.add(row.cells[i]);
            }
            String[] keys=args(row.table,row.a,row.b);if(keys!=null)Collections.addAll(values,(Object[])keys);
            if(update.length()>0)realtime.execSQL("UPDATE "+row.table+" SET "+update+" WHERE "+where(row.table),values.toArray());
        }
        realtime.execSQL("INSERT OR IGNORE INTO r29_projection_applied VALUES(?,?,?,?,?,?,?,?)",new Object[]{row.table,row.a,row.b,epoch,row.revision,row.hash,row.deleted?1:0,now});
        realtime.execSQL("UPDATE r29_projection_applied SET epoch=?,revision=?,payload_hash=?,deleted=?,applied_at=? WHERE table_name=? AND key_a=? AND key_b=?",new Object[]{epoch,row.revision,row.hash,row.deleted?1:0,now,row.table,row.a,row.b});
    }
    public static long drain(ResearchPoolR27.Worker worker){
        if(!ResearchRouteR29.active(worker.realtimeRo()))return 0;
        long epoch;try(Cursor c=worker.ro.rawQuery("SELECT epoch FROM r29_research_owner WHERE singleton=1",null)){if(!c.moveToFirst())throw new IllegalStateException("R29_PROJECTION_OWNER_MISSING");epoch=c.getLong(0);}
        List<Row> rows=capture(worker.ro);
        if(!rows.isEmpty()){
            worker.store().coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_PROJECT_COMMITTED_RESEARCH",db->{for(Row row:rows)apply(db,row,epoch,System.currentTimeMillis());return 0;});
            ResearchRouteR29.write(worker.store(),"R29_PROJECTION_ACK",db->{for(Row row:rows)db.execSQL("UPDATE r29_projection_pending SET acked_revision=max(acked_revision,?) WHERE table_name=? AND key_a=? AND key_b=? AND revision>=?",new Object[]{row.revision,row.table,row.a,row.b,row.revision});return 0;});
        }
        long critical=MigrationCopyR29.scalar(worker.ro,"SELECT count(*) FROM r29_projection_pending WHERE revision>acked_revision AND table_name<>'r26_evolution'",null);
        if(critical==0){
            long through=0;try(Cursor c=worker.ro.rawQuery("SELECT last_seq FROM r29_event_cursor WHERE source='REALTIME'",null)){if(c.moveToFirst())through=c.getLong(0);}final long watermark=through;
            worker.store().coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_RESEARCH_PROJECTION_HEALTH",db->{db.execSQL("INSERT OR IGNORE INTO r29_research_heartbeat VALUES(1,?,?,?)",new Object[]{epoch,watermark,System.currentTimeMillis()});db.execSQL("UPDATE r29_research_heartbeat SET epoch=?,through_seq=?,seen_at=? WHERE singleton=1",new Object[]{epoch,watermark,System.currentTimeMillis()});return 0;});
        }
        return rows.size();
    }
    public static boolean freshForIntent(SQLiteDatabase realtime,String policy,long ordinal,long now){
        if(!ResearchRouteR29.active(realtime))return true;
        long epoch;try(Cursor c=realtime.rawQuery("SELECT r.epoch,h.through_seq,h.seen_at FROM r29_database_route r JOIN r29_research_heartbeat h ON h.singleton=r.singleton AND h.epoch=r.epoch WHERE r.singleton=1",null)){
            if(!c.moveToFirst()||now-c.getLong(2)<-2000||now-c.getLong(2)>3000)return false;epoch=c.getLong(0);
            if(c.getLong(1)<MigrationCopyR29.scalar(realtime,"SELECT coalesce(max(seq),0) FROM r29_event_outbox",null))return false;
        }
        for(String table:new String[]{"r27_live_shadow","r27_certificate","r27_eprocess","r27_live_frozen","r27_chart_cache"}){
            String a=table.equals("r27_chart_cache")?"mainline":policy;long b=table.equals("r27_live_frozen")?ordinal:0;
            try(Cursor c=realtime.rawQuery("SELECT 1 FROM r29_projection_applied WHERE table_name=? AND key_a=? AND key_b=? AND epoch=? AND deleted=0",new String[]{table,a,String.valueOf(b),String.valueOf(epoch)})){if(!c.moveToFirst())return false;}
        }
        String latest=RuntimeR27.latestIssue(realtime);if(latest==null)return false;
        boolean manifestColumns=false;try(Cursor c=realtime.rawQuery("PRAGMA table_info(r27_live_frozen)",null)){while(c.moveToNext())if("manifest_hash".equals(c.getString(1)))manifestColumns=true;}
        if(manifestColumns)try(Cursor c=realtime.rawQuery("SELECT manifest_hash,commit_verified_at,input_through,deadline_at FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new String[]{policy,String.valueOf(ordinal)})){
            if(!c.moveToFirst())return false;
            if(c.getString(0)!=null&&!c.getString(0).isEmpty()&&(c.getLong(1)<=0||c.getLong(1)>=c.getLong(3)-5000||c.getLong(2)>=ordinal))return false;
        }
        try(Cursor c=realtime.rawQuery("SELECT last_ordinal FROM r27_eprocess WHERE policy_hash=?",new String[]{policy})){if(!c.moveToFirst()||c.getLong(0)<TargetIssueResolver.sequenceOrdinalSU1(latest))return false;}
        try(Cursor c=realtime.rawQuery("SELECT frozen_at,deadline_at FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new String[]{policy,String.valueOf(ordinal)})){return c.moveToFirst()&&c.getLong(0)<=c.getLong(1)-5000&&now<c.getLong(1)-5000;}
    }
}
