package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** Expands an already-published CP07 R27-only database without resetting either learning domain.
 * Capture/verification finish before the RP transaction begins. Original Root tables are never rewritten.
 * RP's non-R27 state must be an identical subset of the captured authority; divergent state fails closed.
 * This is an upgrade at session startup, with the pool stopped and the previous dispatcher drained. */
public final class LegacyDomainUpgradeR29 {
    private LegacyDomainUpgradeR29(){}
    public static final String DDL="CREATE TABLE IF NOT EXISTS r29_legacy_domain_owner(singleton INTEGER PRIMARY KEY CHECK(singleton=1),phase TEXT NOT NULL,route_epoch INTEGER NOT NULL,source_hash TEXT NOT NULL,completed_at INTEGER NOT NULL)";
    public static final long MAX_CAPTURE_BYTES=64L<<20,BUDGET_MS=30_000;
    private static final Map<String,ReentrantLock> LOCKS=new HashMap<>();
    private static ReentrantLock lock(String path){synchronized(LOCKS){return LOCKS.computeIfAbsent(path,k->new ReentrantLock());}}
    static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    static String predicate(String table){return table.equals("r26_task")?"lane<>'R27'":table.equals("r26_evolution")?"attempt_id NOT LIKE 'R27:%'":"1=1";}
    static boolean included(String name){return LegacyResearchOwnershipR29.owned(name)||name.equals("r27_legacy_roster_alias");}
    static long epoch(SQLiteDatabase root){return MigrationCopyR29.scalar(root,"SELECT epoch FROM r29_database_route WHERE singleton=1",null);}
    public static boolean ready(SQLiteDatabase root){
        if(!ResearchRouteR29.active(root))return false;SQLiteDatabase rp=ResearchRouteR29.reader(root);
        if(!table(rp,"r29_legacy_domain_owner"))return false;
        try(Cursor c=rp.rawQuery("SELECT phase,route_epoch,source_hash,completed_at FROM main.r29_legacy_domain_owner WHERE singleton=1",null)){
            if(!c.moveToFirst())return false;
            if(!"FULL".equals(c.getString(0))||c.getLong(1)!=epoch(root)||c.isNull(2)||!c.getString(2).matches("[a-f0-9]{64}")||c.getLong(3)<=0)throw new IllegalStateException("R29_LEGACY_DOMAIN_MARKER_CONFLICT");return true;
        }
    }
    public static void ensure(DbStore store)throws Exception{
        SQLiteDatabase root=LegacyResearchScopeR29.rawRealtime(store);if(!ResearchRouteR29.active(root)||ready(root))return;
        if(root.inTransaction()||ResearchRouteR29.writerHeldByCurrentThread())throw new IllegalStateException("R29_LEGACY_UPGRADE_REQUIRES_NO_WRITER_LOCK");
        ReentrantLock serial=lock(root.getPath());if(!serial.tryLock(200,TimeUnit.MILLISECONDS))throw new IllegalStateException("CP3_DB_BUSY_LEGACY_DOMAIN_UPGRADE");
        try{
            if(ready(root))return;RuntimeR27 runtime=RuntimeR27.active();if(runtime!=null&&!runtime.pool().isClosed())throw new IllegalStateException("R29_LEGACY_DOMAIN_UPGRADE_REQUIRES_STOPPED_RESEARCH_POOL");
            try(ResearchRouteR29.Handoff handoff=ResearchRouteR29.handoff(store)){
                if(handoff==null)throw new IllegalStateException("CP3_DB_BUSY_LEGACY_DOMAIN_HANDOFF");
                String blocked=LegacyCapabilityAdapterR29.enterContinuity(store,false);if(blocked!=null)throw new IllegalStateException("CP3_DB_BUSY_LEGACY_DOMAIN_DRAIN:"+blocked);
                try{
                    Check check=new Check(System::nanoTime,System.nanoTime()+BUDGET_MS*1_000_000L,()->Thread.currentThread().isInterrupted());
                    Snapshot captured=capture(root,check,MAX_CAPTURE_BYTES);long routeEpoch=epoch(root);check.run();
                    ResearchRouteR29.write(store,"R29_EXPAND_RETAINED_LEGACY_DOMAIN",rp->{mergeWithin(rp,captured,routeEpoch,check,name->{});return 0;});
                }finally{LegacyCapabilityAdapterR29.leaveContinuity();}
            }
        }finally{serial.unlock();}
    }
    static final class Check {
        final LongSupplier clock;final long deadline;final BooleanSupplier cancelled;
        Check(LongSupplier clock,long deadline,BooleanSupplier cancelled){this.clock=clock;this.deadline=deadline;this.cancelled=cancelled;}
        void run(){if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new IllegalStateException("R29_LEGACY_DOMAIN_UPGRADE_CANCELLED");if(clock.getAsLong()>deadline)throw new IllegalStateException("R29_LEGACY_DOMAIN_UPGRADE_BUDGET");}
    }
    static final class State {
        final MigrationCopyR29.Table table;final List<byte[]> rows;final String hash;final Map<String,Integer> counts;
        State(MigrationCopyR29.Table table,List<byte[]> rows){this.table=table;this.rows=rows;counts=counts(rows);hash=hash(table,counts);}
    }
    static final class Snapshot {
        final List<State> tables;final List<String[]> objects;final Map<String,Long> sequences;final String hash;
        Snapshot(List<State> tables,List<String[]> objects,Map<String,Long> sequences){this.tables=tables;this.objects=objects;this.sequences=sequences;StringBuilder b=new StringBuilder("R29_LEGACY_DOMAIN_CAPTURE_1");for(State s:tables)b.append('|').append(s.table.name).append(':').append(s.rows.size()).append(':').append(s.hash);for(String[] o:objects)b.append('|').append(Arrays.toString(o));b.append('|').append(JsonR27.write(sequences));hash=StatsR27.sha256(b.toString());}
    }
    static List<MigrationCopyR29.Table> tables(SQLiteDatabase root){List<MigrationCopyR29.Table> out=new ArrayList<>();try(Cursor c=root.rawQuery("SELECT name,sql FROM sqlite_master WHERE type='table' ORDER BY name",null)){while(c.moveToNext())if(included(c.getString(0)))out.add(MigrationCopyR29.table(root,c.getString(0),c.getString(1)));}return out;}
    static List<byte[]> rows(SQLiteDatabase db,MigrationCopyR29.Table t,Check check,long[] bytes,long maximum){
        List<byte[]> rows=new ArrayList<>();Long after=null;
        for(;;){
            check.run();List<MigrationCopyR29.Row> page=ResearchRowsR29.readPage(db,t,after,predicate(t.name),32,2L<<20,Long.MAX_VALUE,check::run);if(page.isEmpty())break;
            for(MigrationCopyR29.Row row:page){bytes[0]+=row.payload.length;if(bytes[0]>maximum)throw new IllegalStateException("R29_LEGACY_DOMAIN_CAPTURE_STORAGE_BUDGET:"+t.name);rows.add(row.payload);after=row.rowid;}
        }
        return rows;
    }
    static Snapshot capture(SQLiteDatabase root,Check check,long maximum){return capture(root,check,maximum,()->{});}
    static Snapshot capture(SQLiteDatabase root,Check check,long maximum,Runnable afterCapture){
        if(root.inTransaction())throw new IllegalStateException("R29_LEGACY_CAPTURE_MUST_RELEASE_REALTIME_WRITER");
        List<State> states=new ArrayList<>();long[] bytes={0};List<MigrationCopyR29.Table> descriptors=tables(root);
        for(MigrationCopyR29.Table t:descriptors){check.run();states.add(new State(t,rows(root,t,check,bytes,maximum)));}
        Set<String> included=new HashSet<>();for(State s:states)included.add(s.table.name);
        List<String[]> objects=new ArrayList<>();try(Cursor c=root.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE type IN ('index','trigger') AND sql IS NOT NULL ORDER BY type,name",null)){while(c.moveToNext())if(included.contains(c.getString(2))&&!c.getString(1).startsWith("r29_copy_track_"))objects.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)});}
        Map<String,Long> sequences=new TreeMap<>();if(table(root,"sqlite_sequence"))try(Cursor c=root.rawQuery("SELECT name,seq FROM sqlite_sequence ORDER BY name",null)){while(c.moveToNext())if(included.contains(c.getString(0)))sequences.put(c.getString(0),c.getLong(1));}
        Snapshot snapshot=new Snapshot(states,objects,sequences);afterCapture.run();
        // The startup drain excludes all pure-research writers. A changed source still refuses publication.
        List<MigrationCopyR29.Table> again=tables(root);if(again.size()!=states.size())throw new IllegalStateException("R29_LEGACY_DOMAIN_SOURCE_SCHEMA_CHANGED");
        for(int i=0;i<states.size();i++){check.run();State state=states.get(i);MigrationCopyR29.Table current=again.get(i);if(!state.table.schemaHash.equals(current.schemaHash)||!state.hash.equals(hash(current,counts(rows(root,current,check,new long[]{0},maximum)))))throw new IllegalStateException("R29_LEGACY_DOMAIN_SOURCE_CHANGED:"+state.table.name);}
        List<String[]> currentObjects=new ArrayList<>();try(Cursor c=root.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE type IN ('index','trigger') AND sql IS NOT NULL ORDER BY type,name",null)){while(c.moveToNext())if(included.contains(c.getString(2))&&!c.getString(1).startsWith("r29_copy_track_"))currentObjects.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)});}
        Map<String,Long> currentSequences=new TreeMap<>();if(table(root,"sqlite_sequence"))try(Cursor c=root.rawQuery("SELECT name,seq FROM sqlite_sequence ORDER BY name",null)){while(c.moveToNext())if(included.contains(c.getString(0)))currentSequences.put(c.getString(0),c.getLong(1));}
        if(!snapshot.hash.equals(new Snapshot(states,currentObjects,currentSequences).hash))throw new IllegalStateException("R29_LEGACY_DOMAIN_SOURCE_METADATA_CHANGED");
        return snapshot;
    }
    static Map<String,Integer> counts(List<byte[]> rows){Map<String,Integer> counts=new TreeMap<>();Map<String,byte[]> collision=new HashMap<>();for(byte[] bytes:rows){String hash=StatsR27.sha256(bytes);byte[] prior=collision.putIfAbsent(hash,bytes);if(prior!=null&&!Arrays.equals(prior,bytes))throw new IllegalStateException("R29_LEGACY_TYPED_HASH_COLLISION");counts.merge(hash,1,Integer::sum);}return counts;}
    static String hash(MigrationCopyR29.Table table,Map<String,Integer> counts){return StatsR27.sha256("R29_LEGACY_TYPED_MULTISET_1|"+table.schemaJson+"|"+predicate(table.name)+"|"+JsonR27.write(counts));}
    interface Fault {void afterTable(String name);}
    static void mergeWithin(SQLiteDatabase rp,Snapshot snapshot,long routeEpoch,Check check,Fault fault){
        if(!rp.inTransaction())throw new IllegalStateException("R29_LEGACY_UPGRADE_TRANSACTION_REQUIRED");if(routeEpoch<1)throw new IllegalArgumentException("R29_LEGACY_UPGRADE_ROUTE_EPOCH");check.run();
        if(table(rp,"r29_legacy_domain_owner"))try(Cursor c=rp.rawQuery("SELECT phase,route_epoch,source_hash FROM r29_legacy_domain_owner WHERE singleton=1",null)){if(c.moveToFirst()){if("FULL".equals(c.getString(0))&&c.getLong(1)==routeEpoch&&snapshot.hash.equals(c.getString(2)))return;throw new IllegalStateException("R29_LEGACY_DOMAIN_ALREADY_PUBLISHED_CONFLICT");}}
        rp.execSQL(DDL);
        for(State state:snapshot.tables){
            check.run();MigrationCopyR29.Table t=state.table;
            if(!table(rp,t.name))rp.execSQL(t.createSql);
            String actualSql;try(Cursor c=rp.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{t.name})){if(!c.moveToFirst())throw new IllegalStateException("R29_LEGACY_DESTINATION_TABLE_MISSING");actualSql=c.getString(0);}
            MigrationCopyR29.Table actual=MigrationCopyR29.table(rp,t.name,actualSql);if(!t.schemaHash.equals(actual.schemaHash))throw new IllegalStateException("R29_LEGACY_DOMAIN_SCHEMA_CONFLICT:"+t.name);
            Map<String,Integer> present=counts(rows(rp,t,check,new long[]{0},MAX_CAPTURE_BYTES));
            for(Map.Entry<String,Integer> row:present.entrySet())if(row.getValue()>state.counts.getOrDefault(row.getKey(),0))throw new IllegalStateException("R29_LEGACY_DOMAIN_EXISTING_STATE_CONFLICT:"+t.name);
            StringBuilder names=new StringBuilder(),binds=new StringBuilder();for(String name:t.columns){if(names.length()>0){names.append(',');binds.append(',');}names.append(MigrationCopyR29.id(name));binds.append('?');}
            Map<String,Integer> remaining=new HashMap<>(present);String insert="INSERT INTO "+MigrationCopyR29.id(t.name)+"("+names+") VALUES("+binds+")";
            for(byte[] payload:state.rows){check.run();String hash=StatsR27.sha256(payload);int existing=remaining.getOrDefault(hash,0);if(existing>0){remaining.put(hash,existing-1);continue;}rp.execSQL(insert,ResearchCutoverR29.decode(payload,t.columns.size()));}
            if(!state.hash.equals(hash(t,counts(rows(rp,t,check,new long[]{0},MAX_CAPTURE_BYTES)))))throw new IllegalStateException("R29_LEGACY_DOMAIN_TYPED_VERIFICATION_FAILED:"+t.name);
            fault.afterTable(t.name);
        }
        for(String[] object:snapshot.objects){check.run();try(Cursor c=rp.rawQuery("SELECT type,tbl_name,sql FROM sqlite_master WHERE name=?",new String[]{object[1]})){if(c.moveToFirst()){if(!object[0].equals(c.getString(0))||!object[2].equals(c.getString(1))||!object[3].equals(c.getString(2)))throw new IllegalStateException("R29_LEGACY_DOMAIN_OBJECT_CONFLICT:"+object[1]);continue;}}rp.execSQL(object[3]);}
        if(!snapshot.sequences.isEmpty()){if(!table(rp,"sqlite_sequence"))throw new IllegalStateException("R29_LEGACY_AUTOINCREMENT_SCHEMA_CONFLICT");for(Map.Entry<String,Long> e:snapshot.sequences.entrySet()){check.run();rp.execSQL("UPDATE sqlite_sequence SET seq=max(seq,?) WHERE name=?",new Object[]{e.getValue(),e.getKey()});rp.execSQL("INSERT INTO sqlite_sequence(name,seq) SELECT ?,? WHERE NOT EXISTS(SELECT 1 FROM sqlite_sequence WHERE name=?)",new Object[]{e.getKey(),e.getValue(),e.getKey()});}}
        check.run();rp.execSQL("INSERT INTO r29_legacy_domain_owner VALUES(1,'FULL',?,?,?)",new Object[]{routeEpoch,snapshot.hash,Math.max(1,System.currentTimeMillis())});
    }
}
