package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.RetentionRulesSU2;
import java.util.*;

/** Cross-owner retirement: immutable counted receipt in RP, exact Root deletion, then RP acknowledgement.
 * A failed Root deletion retains its receipt and source row; retry never increments a denominator again.
 * All owner transactions finish before the other owner is acquired. Settled raw decisions retain the original
 * 10,080-period window; unresolved or referenced authority rows are not discarded. */
public final class LegacyDecisionRetentionR29 {
    private LegacyDecisionRetentionR29(){}
    public static final int PAGE=60;
    static final String RECEIPT="r29_legacy_decision_retention",SCHEMA="r29_legacy_decision_retention_schema";
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS "+SCHEMA+"(schema_hash TEXT PRIMARY KEY,create_sql TEXT NOT NULL,schema_json TEXT NOT NULL,columns_json TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS "+RECEIPT+"(genome_key TEXT NOT NULL,sequence_ordinal INTEGER NOT NULL,schema_hash TEXT NOT NULL,row_hash TEXT NOT NULL,payload BLOB NOT NULL,day_key INTEGER NOT NULL,actions INTEGER NOT NULL,hits INTEGER NOT NULL,counted_at INTEGER NOT NULL,acked_at INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(genome_key,sequence_ordinal))",
        "CREATE INDEX IF NOT EXISTS r29_legacy_decision_retention_pending ON "+RECEIPT+"(acked_at,sequence_ordinal)",
        "CREATE TRIGGER IF NOT EXISTS r29_legacy_decision_retention_immutable BEFORE UPDATE OF genome_key,sequence_ordinal,schema_hash,row_hash,payload,day_key,actions,hits,counted_at ON "+RECEIPT+" BEGIN SELECT RAISE(ABORT,'R29_RETENTION_RECEIPT_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r29_legacy_decision_retention_no_delete BEFORE DELETE ON "+RECEIPT+" BEGIN SELECT RAISE(ABORT,'R29_RETENTION_RECEIPT_REQUIRED_FOR_DEDUP'); END"
    };
    static void tx(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R29_RETENTION_OWNER_TRANSACTION_REQUIRED");}
    static boolean table(SQLiteDatabase db,String name){return ResearchInputMirrorR29.table(db,name);}
    static String q(String name){return MigrationCopyR29.id(name);}
    static long n(SQLiteDatabase db,String sql,String[] args){return MigrationCopyR29.scalar(db,sql,args);}
    static void ensure(SQLiteDatabase rp){tx(rp);for(String sql:DDL)rp.execSQL(sql);}
    static final class Row {
        final MigrationCopyR29.Table table;final byte[] payload;final Object[] cells;final String hash;
        Row(MigrationCopyR29.Table table,byte[] payload){this.table=table;this.payload=payload;cells=ResearchCutoverR29.decode(payload,table.columns.size());hash=StatsR27.sha256(table.schemaHash+":"+StatsR27.sha256(payload));}
        Object value(String name){int at=table.columns.indexOf(name);if(at<0)throw new IllegalStateException("R29_RETENTION_SOURCE_COLUMN_MISSING:"+name);return cells[at];}
        String text(String name){Object value=value(name);return value==null?null:String.valueOf(value);}
        long number(String name){Object value=value(name);if(!(value instanceof Number))throw new IllegalStateException("R29_RETENTION_INVALID_NUMERICAL_FIELD:"+name);return ((Number)value).longValue();}
        String policy(){return text("genome_key");}long ordinal(){return number("sequence_ordinal");}
    }
    static String fields(MigrationCopyR29.Table t){StringBuilder b=new StringBuilder();for(String col:t.columns){if(b.length()>0)b.append(',');b.append(q(col));}return b.toString();}
    static List<Row> rows(SQLiteDatabase db,String name,String where,String[] args,String order,int limit){
        MigrationCopyR29.Table t=ResearchInputMirrorR29.descriptor(db,name);List<Row> rows=new ArrayList<>();if(t==null)return rows;
        try(Cursor c=db.rawQuery("SELECT "+fields(t)+" FROM "+q(name)+" WHERE "+where+(order.isEmpty()?"":" ORDER BY "+order)+" LIMIT "+limit,args)){while(c.moveToNext())rows.add(new Row(t,MigrationCopyR29.encode(c,0,t.columns.size())));}return rows;
    }
    static List<Row> capture(SQLiteDatabase root,long current){
        if(root.inTransaction())throw new IllegalStateException("R29_RETENTION_CAPTURE_INSIDE_ROOT_WRITER");if(!table(root,"su2_live_decision"))return Collections.emptyList();long cut=current-RetentionRulesSU2.RAW_DECISION_PERIODS,low;
        try(Cursor c=root.rawQuery("SELECT min(sequence_ordinal) FROM su2_live_decision WHERE hit IS NOT NULL AND sequence_ordinal<?",new String[]{String.valueOf(cut)})){if(!c.moveToFirst()||c.isNull(0))return Collections.emptyList();low=c.getLong(0);}
        return rows(root,"su2_live_decision","hit IS NOT NULL AND sequence_ordinal>=? AND sequence_ordinal<?",new String[]{String.valueOf(low),String.valueOf(Math.min(cut,low+60))},"sequence_ordinal,genome_key",PAGE);
    }
    @SuppressWarnings("unchecked") static List<Row> pending(SQLiteDatabase rp){
        List<Row> out=new ArrayList<>();if(!table(rp,RECEIPT))return out;
        try(Cursor c=rp.rawQuery("SELECT r.payload,r.row_hash,s.create_sql,s.schema_json,s.columns_json FROM "+RECEIPT+" r JOIN "+SCHEMA+" s ON s.schema_hash=r.schema_hash WHERE r.acked_at=0 ORDER BY r.sequence_ordinal,r.genome_key LIMIT "+PAGE,null)){
            while(c.moveToNext()){List<String> columns=new ArrayList<>();for(Object value:(List<Object>)JsonR27.parse(c.getString(4)))columns.add((String)value);Row row=new Row(new MigrationCopyR29.Table("su2_live_decision",c.getString(2),columns,c.getString(3)),c.getBlob(0));if(!row.hash.equals(c.getString(1)))throw new IllegalStateException("R29_RETENTION_DURABLE_PAYLOAD_CONFLICT");out.add(row);}
        }return out;
    }
    static long countWithin(SQLiteDatabase rp,List<Row> rows,long now){
        tx(rp);ensure(rp);if(!table(rp,"su2_live_daily"))throw new IllegalStateException("R29_RETENTION_DAILY_SCHEMA_MISSING");long added=0;
        for(Row row:rows){
            long act=row.number("act"),hit=row.number("hit"),ord=row.ordinal();if(row.policy()==null||row.policy().isEmpty()||ord<0||act<0||act>1||hit<0||hit>1)throw new IllegalStateException("R29_RETENTION_INVALID_SETTLED_ROW");
            rp.execSQL("INSERT OR IGNORE INTO "+SCHEMA+" VALUES(?,?,?,?)",new Object[]{row.table.schemaHash,row.table.createSql,row.table.schemaJson,JsonR27.write(row.table.columns)});
            try(Cursor c=rp.rawQuery("SELECT row_hash FROM "+RECEIPT+" WHERE genome_key=? AND sequence_ordinal=?",new String[]{row.policy(),String.valueOf(ord)})){if(c.moveToFirst()){if(!row.hash.equals(c.getString(0)))throw new IllegalStateException("R29_RETENTION_COUNTED_ROW_CONFLICT");continue;}}
            rp.execSQL("INSERT INTO "+RECEIPT+" VALUES(?,?,?,?,?,?,?,?,?,0)",new Object[]{row.policy(),ord,row.table.schemaHash,row.hash,row.payload,ord/1440,act,act==1?hit:0,now});
            rp.execSQL("INSERT OR IGNORE INTO su2_live_daily VALUES(?,?,0,0,0,?)",new Object[]{row.policy(),ord/1440,now});
            rp.execSQL("UPDATE su2_live_daily SET periods=periods+1,actions=actions+?,hits=hits+?,closed_at=max(closed_at,?) WHERE genome_key=? AND day_key=?",new Object[]{act,act==1?hit:0,now,row.policy(),ord/1440});added++;
        }return added;
    }
    static Row current(SQLiteDatabase root,Row expected,String where,String[] args){
        MigrationCopyR29.Table actual=ResearchInputMirrorR29.descriptor(root,expected.table.name);if(actual==null||!actual.schemaHash.equals(expected.table.schemaHash))throw new IllegalStateException("R29_RETENTION_SOURCE_SCHEMA_CHANGED");List<Row> rows=rows(root,actual.name,where,args,"",2);if(rows.size()>1)throw new IllegalStateException("R29_RETENTION_NON_UNIQUE_SOURCE_KEY");return rows.isEmpty()?null:rows.get(0);
    }
    /** Must only receive rows whose independent counted-receipt transaction has committed. */
    static long deleteCountedWithin(SQLiteDatabase root,List<Row> counted,long current){
        tx(root);long removed=0,cut=current-RetentionRulesSU2.RAW_DECISION_PERIODS;
        for(Row row:counted){if(row.ordinal()>=cut)throw new IllegalStateException("R29_RETENTION_CUTOFF_CHANGED");String[] key={row.policy(),String.valueOf(row.ordinal())};Row present=current(root,row,"genome_key=? AND sequence_ordinal=?",key);
            if(present==null)continue;if(!row.hash.equals(present.hash))throw new IllegalStateException("R29_RETENTION_ROOT_CAS_CONFLICT");
            root.execSQL("DELETE FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=? AND hit IS NOT NULL",new Object[]{row.policy(),row.ordinal()});removed+=n(root,"SELECT changes()",null);
        }return removed;
    }
    static long acknowledgeWithin(SQLiteDatabase rp,List<Row> counted,long now){
        tx(rp);long ack=0;for(Row row:counted){rp.execSQL("UPDATE "+RECEIPT+" SET acked_at=? WHERE genome_key=? AND sequence_ordinal=? AND row_hash=? AND acked_at=0",new Object[]{Math.max(1,now),row.policy(),row.ordinal(),row.hash});ack+=n(rp,"SELECT changes()",null);}return ack;
    }
    public static long step(DbStore store,long currentSequence,long now){
        SQLiteDatabase root=LegacyResearchScopeR29.realtime(LegacyResearchScopeR29.rawRealtime(store));if(!ResearchRouteR29.active(root))return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_SINGLE_DOMAIN_DECISION_RETENTION",db->{RuntimeCoordinatorR26.checkThreadFence(db);long units=RetentionRulesSU2.foldDecisions(db,currentSequence,now);RuntimeCoordinatorR26.checkThreadFence(db);return units;});
        if(ResearchRouteR29.writerHeldByCurrentThread()||root.inTransaction())throw new IllegalStateException("R29_RETENTION_MUST_START_OUTSIDE_WRITERS");
        SQLiteDatabase rp=ResearchRouteR29.reader(LegacyResearchScopeR29.rawRealtime(store));List<Row> work=pending(rp);boolean recovered=!work.isEmpty();if(!recovered)work=capture(root,currentSequence);if(work.isEmpty())return 0;final List<Row> counted=work;
        long added=recovered?0:ResearchRouteR29.write(store,"R29_COUNT_RETIRED_DECISIONS",db->{RuntimeCoordinatorR26.checkThreadFence(db);long units=countWithin(db,counted,now);RuntimeCoordinatorR26.checkThreadFence(db);return units;});
        long deleted=LegacyResearchScopeR29.realtimeWrite(store,"R29_DELETE_COUNTED_ROOT_DECISIONS",db->deleteCountedWithin(db,counted,currentSequence));
        ResearchRouteR29.write(store,"R29_ACK_RETIRED_DECISIONS",db->{RuntimeCoordinatorR26.checkThreadFence(db);long units=acknowledgeWithin(db,counted,now);RuntimeCoordinatorR26.checkThreadFence(db);return units;});return added+deleted;
    }

    static final class AuthorityBatch {
        final List<Row> feedback=new ArrayList<>(),frozen=new ArrayList<>(),incumbent=new ArrayList<>();
        final Set<String> protectedCheckpoints=new HashSet<>(),protectedTargets=new HashSet<>(),protectedFeedback=new HashSet<>(),folded=new HashSet<>();
    }
    static String key(String policy,long ordinal){return policy+"|"+ordinal;}
    static String deliveryId(Row row){return ResearchEventBridgeR29.terminalFeedback(row.text("event_id"),row.number("issue_ordinal"),row.text("source_hash"),row.text("payload"),row.number("ready_at")).id;}
    static boolean bound(SQLiteDatabase root,String policy,long ordinal){return table(root,"su2_mainline_binding")&&n(root,"SELECT count(*) FROM su2_mainline_binding WHERE target_issue IN (?,?) AND genome_key=?",new String[]{String.valueOf(ordinal),com.ethk3.novaonepro.TargetIssueResolver.fromSequenceOrdinalSU1(ordinal),policy})>0;}
    static String placeholders(int count){StringBuilder b=new StringBuilder();for(int i=0;i<count;i++){if(i>0)b.append(',');b.append('?');}return b.toString();}
    static AuthorityBatch authorityCapture(SQLiteDatabase root,SQLiteDatabase rp,long current){
        if(root.inTransaction()||rp.inTransaction())throw new IllegalStateException("R29_AUTHORITY_CAPTURE_INSIDE_WRITER");AuthorityBatch batch=new AuthorityBatch();
        batch.feedback.addAll(rows(root,"r26_feedback","issue_ordinal<?",new String[]{String.valueOf(current-5000)},"issue_ordinal",256));
        batch.frozen.addAll(rows(root,"r26_frozen","target_ordinal<?",new String[]{String.valueOf(current-10080)},"target_ordinal,policy_hash",512));
        batch.incumbent.addAll(rows(root,"r26_incumbent_frozen","target_ordinal<?",new String[]{String.valueOf(current-10080)},"target_ordinal",512));
        Set<String> checkpoints=new HashSet<>();for(Row row:batch.frozen)checkpoints.add(row.text("checkpoint_hash"));String[] checkpointArgs=checkpoints.toArray(new String[0]);
        for(String[] ref:new String[][]{{"r26_live_model","checkpoint_hash"},{"r26_candidate","checkpoint_hash"},{"r26_consumption","checkpoint_hash"},{"r26_checkpoint_receipt","checkpoint_hash"}})if(checkpointArgs.length>0&&table(rp,ref[0]))try(Cursor c=rp.rawQuery("SELECT DISTINCT "+q(ref[1])+" FROM "+q(ref[0])+" WHERE "+q(ref[1])+" IN ("+placeholders(checkpointArgs.length)+")",checkpointArgs)){while(c.moveToNext())if(!c.isNull(0))batch.protectedCheckpoints.add(c.getString(0));}
        if(table(rp,"r26_prepared_forecast"))try(Cursor c=rp.rawQuery("SELECT policy_hash,target_ordinal FROM r26_prepared_forecast",null)){while(c.moveToNext())batch.protectedTargets.add(key(c.getString(0),c.getLong(1)));}
        for(Row row:batch.feedback){String id=row.text("event_id");if(table(rp,"r26_consumption")&&n(rp,"SELECT count(*) FROM r26_consumption WHERE event_id=?",new String[]{id})>0)batch.protectedFeedback.add(id);
            if(table(root,"r29_event_outbox")){String delivery=deliveryId(row);if(n(root,"SELECT count(*) FROM r29_event_outbox WHERE event_id=?",new String[]{delivery})>0&&(!table(rp,"r29_event_inbox")||n(rp,"SELECT count(*) FROM r29_event_inbox WHERE event_id=?",new String[]{delivery})==0))batch.protectedFeedback.add(id);}}
        if(table(rp,RECEIPT))for(Row row:batch.frozen){String policy=row.text("policy_hash");long ord=row.number("target_ordinal");if(n(rp,"SELECT count(*) FROM "+RECEIPT+" WHERE genome_key=? AND sequence_ordinal=? AND acked_at>0",new String[]{policy,String.valueOf(ord)})>0)batch.folded.add(key(policy,ord));}
        if(table(rp,RECEIPT))for(Row row:batch.incumbent){long ord=row.number("target_ordinal");if(n(rp,"SELECT count(*) FROM "+RECEIPT+" WHERE genome_key=? AND sequence_ordinal=? AND acked_at>0",new String[]{IncumbentLoopSU2.KEY,String.valueOf(ord)})>0)batch.folded.add(key(IncumbentLoopSU2.KEY,ord));}
        return batch;
    }
    static boolean settled(SQLiteDatabase root,AuthorityBatch batch,String policy,long ordinal){
        if(!table(root,"su2_live_decision"))return false;try(Cursor c=root.rawQuery("SELECT hit FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=?",new String[]{policy,String.valueOf(ordinal)})){return c.moveToFirst()?!c.isNull(0):batch.folded.contains(key(policy,ordinal));}
    }
    static long pruneAuthoritiesWithin(SQLiteDatabase root,AuthorityBatch batch,long current){
        tx(root);long deleted=0,deadline=System.nanoTime()+100_000_000L;
        for(Row row:batch.feedback){if(System.nanoTime()>deadline)return deleted;String id=row.text("event_id");if(row.number("issue_ordinal")>=current-5000||batch.protectedFeedback.contains(id))continue;
            if(table(root,"r29_event_outbox")&&n(root,"SELECT count(*) FROM r29_event_outbox WHERE event_id=? AND acked_at=0",new String[]{deliveryId(row)})>0)continue;
            if(table(root,"su2_live_decision")&&n(root,"SELECT count(*) FROM su2_live_decision WHERE sequence_ordinal=? AND hit IS NULL",new String[]{String.valueOf(row.number("issue_ordinal"))})>0)continue;
            Row actual=current(root,row,"event_id=?",new String[]{id});if(actual==null)continue;if(!row.hash.equals(actual.hash))throw new IllegalStateException("R29_FEEDBACK_RETENTION_CAS_CONFLICT");root.execSQL("DELETE FROM r26_feedback WHERE event_id=?",new Object[]{id});deleted+=n(root,"SELECT changes()",null);
        }
        for(Row row:batch.frozen){if(System.nanoTime()>deadline)return deleted;String policy=row.text("policy_hash");long ord=row.number("target_ordinal");if(bound(root,policy,ord)||ord>=current-10080||batch.protectedCheckpoints.contains(row.text("checkpoint_hash"))||batch.protectedTargets.contains(key(policy,ord))||!settled(root,batch,policy,ord))continue;
            Row actual=current(root,row,"policy_hash=? AND target_ordinal=?",new String[]{policy,String.valueOf(ord)});if(actual==null)continue;if(!row.hash.equals(actual.hash))throw new IllegalStateException("R29_FREEZE_RETENTION_CAS_CONFLICT");root.execSQL("DELETE FROM r26_frozen WHERE policy_hash=? AND target_ordinal=?",new Object[]{policy,ord});deleted+=n(root,"SELECT changes()",null);
        }
        for(Row row:batch.incumbent){if(System.nanoTime()>deadline)return deleted;long ord=row.number("target_ordinal");if(bound(root,IncumbentLoopSU2.KEY,ord)||ord>=current-10080||batch.protectedTargets.contains(key(IncumbentLoopSU2.KEY,ord))||!settled(root,batch,IncumbentLoopSU2.KEY,ord))continue;
            Row actual=current(root,row,"target_ordinal=?",new String[]{String.valueOf(ord)});if(actual==null)continue;if(!row.hash.equals(actual.hash))throw new IllegalStateException("R29_INCUMBENT_RETENTION_CAS_CONFLICT");root.execSQL("DELETE FROM r26_incumbent_frozen WHERE target_ordinal=?",new Object[]{ord});deleted+=n(root,"SELECT changes()",null);
        }return deleted;
    }
    /** Invoke only after the research-side retention transaction has committed. */
    public static long pruneAuthorities(DbStore store,long currentSequence,long now){
        SQLiteDatabase root=LegacyResearchScopeR29.realtime(LegacyResearchScopeR29.rawRealtime(store));if(!ResearchRouteR29.active(root))return 0;if(ResearchRouteR29.writerHeldByCurrentThread())throw new IllegalStateException("R29_AUTHORITY_PRUNE_INSIDE_RESEARCH_WRITER");
        AuthorityBatch batch=authorityCapture(root,ResearchRouteR29.reader(LegacyResearchScopeR29.rawRealtime(store)),currentSequence);
        return LegacyResearchScopeR29.realtimeWrite(store,"R29_PRUNE_RETIRED_ROOT_AUTHORITIES",db->pruneAuthoritiesWithin(db,batch,currentSequence));
    }
}
