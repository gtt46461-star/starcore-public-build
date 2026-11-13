package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Recover only original predraw rows from the existing immutable retention owner.
 * The scan is bounded/durable. No synthetic opportunity, e-process reset or recycled alpha is created. */
public final class QualificationGapRepairR30R1 {
    private QualificationGapRepairR30R1(){}
    public static final String PANEL="r30-qualification-gap",SEALED="SEALED_MISSING_OPPORTUNITY";
    private static final java.util.concurrent.atomic.AtomicLong FAILURES=new java.util.concurrent.atomic.AtomicLong();private static volatile String lastFailureType;
    public static void observeFailure(RuntimeException failure){lastFailureType=failure.getClass().getName();FAILURES.incrementAndGet();android.util.Log.w("STARCORE_R30","qualification original archive unavailable; original gap stays blocking: "+lastFailureType);}
    public static Map<String,Object> failureObservation(){Map<String,Object> m=new LinkedHashMap<>();m.put("scope","CURRENT_PROCESS_OBSERVATION_NOT_DB_SNAPSHOT_NOT_UPTIME");m.put("failures",FAILURES.get());m.put("lastFailureType",lastFailureType);m.put("effect","Original gap remains blocking; archive failure cannot synthesize a forecast or authorize REAL");return m;}
    static void markReplayWithin(SQLiteDatabase db,String experiment,long originalOrdinal,long through){String key=experiment+"|"+originalOrdinal;String original=text(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",PANEL,key);if(original==null)return;try{Map<String,Object> receipt=new LinkedHashMap<>(MetricEvaluatorR29.map(JsonR27.parse(original)));if(!"ORIGINAL_RESTORED_REPLAY_PENDING".equals(receipt.get("status")))return;receipt.put("status","ORIGINAL_RESTORED_CONTIGUOUS_PREFIX_ADVANCED");receipt.put("replayedThroughOrdinal",through);receipt.put("replayedAt",System.currentTimeMillis());db.execSQL("UPDATE r27_chart_cache SET payload_json=?,updated_at=? WHERE panel_id=? AND key=? AND payload_json=?",new Object[]{JsonR27.write(receipt),System.currentTimeMillis(),PANEL,key,original});}catch(RuntimeException invalid){observeFailure(invalid);}}
    static final String ARCHIVE="r30_keep_r27_live_frozen";
    static final String COLUMNS="policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit,settled_at";
    static final int SLICE=64;
    static final class Gap {String id,status;long ordinal,last,n,index;}
    static Map<String,Object> read(SQLiteDatabase db,String key){String text=text(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",PANEL,key);return text==null?new LinkedHashMap<>():new LinkedHashMap<>(MetricEvaluatorR29.map(JsonR27.parse(text)));}
    static String text(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){return c.moveToFirst()&&!c.isNull(0)?c.getString(0):null;}}
    static long num(Object o,long fallback){return o instanceof Number?((Number)o).longValue():fallback;}
    /** One archive page for one blocked experiment; other live work continues on the same pool. */
    public static long step(SQLiteDatabase ro,QualificationInboxR29.Commit sink)throws Exception{
        Gap gap=null;
        try(Cursor c=ro.rawQuery("SELECT c.experiment_id,c.blocked_ordinal,e.last_ordinal,e.n,e.global_index,s.status FROM r29_qual_cursor c JOIN r27_eprocess e ON e.policy_hash=c.experiment_id JOIN r27_live_shadow s ON s.policy_hash=e.policy_hash WHERE c.blocked_reason='MISSING_OPPORTUNITY' AND s.status IN ('ACTIVE','ACTIVE_REPAIR1','CONTROL_I00','CONTROL_NULL') AND NOT EXISTS(SELECT 1 FROM r27_chart_cache wait WHERE wait.panel_id='r30-qualification-gap-retry' AND wait.key=c.experiment_id||'|'||c.blocked_ordinal AND CAST(wait.payload_json AS INTEGER)>CAST((julianday('now')-2440587.5)*86400000 AS INTEGER)) ORDER BY c.updated_at,c.experiment_id LIMIT 1",null)){
            if(c.moveToFirst()){gap=new Gap();gap.id=c.getString(0);gap.ordinal=c.getLong(1);gap.last=c.getLong(2);gap.n=c.getLong(3);gap.index=c.getLong(4);gap.status=c.getString(5);}
        }
        if(gap==null)return 0;final Gap g=gap;String key=g.id+"|"+g.ordinal;
        String previous=text(ro,"SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",PANEL,key);
        Map<String,Object> state=previous==null?new LinkedHashMap<>():new LinkedHashMap<>(MetricEvaluatorR29.map(JsonR27.parse(previous)));
        state.put("version","R30_REPAIR_GAP_1");state.put("experiment",g.id);state.put("missingOrdinal",g.ordinal);state.put("originalGlobalIndex",g.index);state.put("originalPrefixLast",g.last);state.put("originalPrefixPeriods",g.n);state.put("alphaAction","RETAIN_ORIGINAL_GLOBAL_INDEX_AND_ALL_ALLOCATED_ALPHA");state.put("sourceScope","existing app immutable retention archive; external original backups may later permit exact recovery");
        String schemaHash=null;boolean archiveReady=false;
        try(Cursor c=ro.rawQuery("SELECT archive_table,schema_hash,columns_json FROM r30_retention_archive_index WHERE source_table='r27_live_frozen'",null)){
            if(c.moveToFirst()&&ARCHIVE.equals(c.getString(0))&&HashingSU1.text(c.getString(2)).equals(c.getString(1))){schemaHash=c.getString(1);ResearchRetentionStoreR30.requireKeepMigrationSourceR31(ro,ARCHIVE);archiveReady=true;}
        }catch(RuntimeException unavailable){state.put("sourceFailure",unavailable.getClass().getSimpleName());}
        if(g.ordinal<0||g.last>=0&&g.ordinal!=g.last+1){archiveReady=false;state.put("sourceFailure","GAP_IS_NOT_NEXT_CONTIGUOUS_ORDINAL");}
        long cursor=num(state.get("archiveCursor"),0),upper=num(state.get("archiveUpper"),-1),candidate=num(state.get("candidateOriginalRow"),-1);String signature=state.get("candidatePredrawHash") instanceof String?(String)state.get("candidatePredrawHash"):null;
        boolean conflict=Boolean.TRUE.equals(state.get("originalConflict")),complete=false;
        if(!archiveReady){state.put("status","BLOCKED_IMMUTABLE_ARCHIVE_UNAVAILABLE");state.put("action","Repair retention source readiness or supply verified original; do not invent an opportunity");}
        else {
            if(state.get("archiveSchemaHash")!=null&&!schemaHash.equals(state.get("archiveSchemaHash"))){cursor=0;upper=-1;candidate=-1;signature=null;conflict=false;}
            if(upper<0)upper=KeepMigrationStoreR31.maximumOriginalId(ro,ARCHIVE);
            state.put("archiveSchemaHash",schemaHash);state.put("archiveUpper",upper);int seen=0;
            String[] projection=(COLUMNS+",__r30_reason").split(",");
            for(KeepMigrationStoreR31.OriginalRow original:KeepMigrationStoreR31.originalRows(ro,ARCHIVE,cursor,upper,SLICE,projection)){
                Object[] row=new Object[original.values.length+1];row[0]=original.id;System.arraycopy(original.values,0,row,1,original.values.length);
                seen++;cursor=original.id;if(!g.id.equals(row[1])||g.ordinal!=number(row[2]))continue;
                boolean legal=legalOriginal(row);String current=legal?predrawHash(row):null;
                if(!legal||signature!=null&&!signature.equals(current)){conflict=true;state.put("conflictingOriginalRow",cursor);state.put("action","Original archive rows disagree or lack legal predraw timing; keep the experiment blocked");}
                else {candidate=cursor;signature=current;}
            }
            complete=cursor>=upper||seen<SLICE;state.put("archiveCursor",cursor);state.put("candidateOriginalRow",candidate<0?null:candidate);state.put("candidatePredrawHash",signature);state.put("originalConflict",conflict);state.put("archiveRowsRemainingUpperBound",Math.max(0,upper-cursor));
            if(complete)state.put("dispositionAt",System.currentTimeMillis());state.put("status",conflict?"BLOCKED_ORIGINAL_CONFLICT":!complete?"SCANNING_ORIGINAL_ARCHIVE":candidate>=0?"ORIGINAL_RESTORED_REPLAY_PENDING":"SEALED_NO_AVAILABLE_IMMUTABLE_ORIGINAL");
        }
        final boolean restore=archiveReady&&complete&&!conflict&&candidate>=0,seal=archiveReady&&complete&&!conflict&&candidate<0;final long originalRow=candidate;final String expectedSchema=schemaHash;
        final Object[] restored=restore?KeepMigrationStoreR31.readOriginal(ro,ARCHIVE,originalRow,COLUMNS.split(",")):null;
        if(restore&&restored==null)throw new IllegalStateException("R31_KEEP_GAP_ORIGINAL_DISAPPEARED");
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
        batch.guardBefore(db->{
            String current=text(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",PANEL,key);if(!Objects.equals(previous,current))return -1;
            try(Cursor c=db.rawQuery("SELECT e.last_ordinal,e.n,e.global_index,c.blocked_ordinal,c.blocked_reason,s.status FROM r27_eprocess e JOIN r29_qual_cursor c ON c.experiment_id=e.policy_hash JOIN r27_live_shadow s ON s.policy_hash=e.policy_hash WHERE e.policy_hash=?",new String[]{g.id})){if(!c.moveToFirst()||c.getLong(0)!=g.last||c.getLong(1)!=g.n||c.getLong(2)!=g.index||c.getLong(3)!=g.ordinal||!"MISSING_OPPORTUNITY".equals(c.getString(4))||!g.status.equals(c.getString(5)))return -1;}
            try(Cursor original=db.rawQuery("SELECT 1 FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new String[]{g.id,Long.toString(g.ordinal)})){if(original.moveToFirst())return -1;}
            if(restore||seal){ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,ARCHIVE);if(!Objects.equals(expectedSchema,text(db,"SELECT schema_hash FROM r30_retention_archive_index WHERE source_table='r27_live_frozen'")))return -1;}
            return 0;
        });
        if(restore){batch.add("INSERT INTO r27_live_frozen("+COLUMNS+") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",restored);batch.add("UPDATE r29_qual_cursor SET blocked_reason='ORIGINAL_RESTORED_REPLAY_PENDING',updated_at=? WHERE experiment_id=?",System.currentTimeMillis(),g.id);}
        if(seal){batch.add("UPDATE r29_qual_cursor SET blocked_reason='SEALED_MISSING_OPPORTUNITY',updated_at=? WHERE experiment_id=?",System.currentTimeMillis(),g.id);batch.add("UPDATE r27_live_shadow SET status=?,reason='Original opportunity missing from active data and available immutable app archive; segment sealed without resetting alpha or synthesizing SKIP',updated_at=? WHERE policy_hash=?",SEALED,System.currentTimeMillis(),g.id);batch.add("UPDATE r27_certificate SET status='REVOKED_MISSING_OPPORTUNITY' WHERE policy_hash=? AND status='ACTIVE'",g.id);}
        if(!archiveReady||conflict)batch.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r30-qualification-gap-retry',?,?,?)",key,Long.toString(System.currentTimeMillis()+30000),System.currentTimeMillis());
        else batch.add("DELETE FROM r27_chart_cache WHERE panel_id='r30-qualification-gap-retry' AND key=?",key);
        batch.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,?,?,?)",PANEL,key,JsonR27.write(state),System.currentTimeMillis());
        try{return sink.commit("R30_QUAL_ORIGINAL_GAP",batch)?1:0;}catch(IllegalStateException race){if(!QualificationInboxR29.precondition(race))throw race;return 0;}
    }
    private static long number(Object value){return value==null?0:((Number)value).longValue();}
    private static Object[] values(Cursor c){Object[] row=new Object[c.getColumnCount()];for(int i=0;i<row.length;i++)row[i]=c.isNull(i)?null:c.getType(i)==Cursor.FIELD_TYPE_BLOB?c.getBlob(i):c.getType(i)==Cursor.FIELD_TYPE_INTEGER?c.getLong(i):c.getType(i)==Cursor.FIELD_TYPE_FLOAT?c.getDouble(i):c.getString(i);return row;}
    static String predrawHash(Cursor c){return predrawHash(values(c));}
    static String predrawHash(Object[] row){return HashingSU1.fields("R30_ORIGINAL_PREDRAW",(String)row[1],Long.toString(number(row[2])),(String)row[3],Integer.toString((int)number(row[4])),StatsR27.sha256((byte[])row[5]),Integer.toString((int)number(row[6])),Integer.toString((int)number(row[7])),Integer.toString((int)number(row[8])),Integer.toString((int)number(row[9])),(String)row[10],Long.toString(number(row[11])),Long.toString(number(row[12])));}
    static boolean legalOriginal(Cursor c){return legalOriginal(values(c));}
    static boolean legalOriginal(Object[] row){
        if(!"BEFORE_ORIGINAL_DELETE".equals(row[15])||!com.ethk3.novaonepro.TargetIssueResolver.isValid((String)row[3])||com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1((String)row[3])!=number(row[2]))return false;
        String status=(String)row[10];if(!status.equals("FROZEN"))return (status.equals("DEADLINE_MISSED")||status.equals("SKIPPED_GAP")||status.equals("ABSTAIN_SET_SUPPORT"))&&number(row[9])==0;
        if(number(row[11])<=0||number(row[12])-number(row[11])<LiveShadowsR27.FREEZE_MARGIN_MS||number(row[4])<1||number(row[4])>10||row[5]==null||((byte[])row[5]).length!=80)return false;
        int a=(int)number(row[6]),b=(int)number(row[7]),d=(int)number(row[8]);if(a<1||b<1||d<1||a>10||b>10||d>10||a==b||a==d||b==d||number(row[9])<0||number(row[9])>1)return false;
        double sum=0;for(double p:StoreR27.unpack((byte[])row[5])){if(!Double.isFinite(p)||p<0||p>1)return false;sum+=p;}return Math.abs(sum-1)<=1e-9;
    }
}
