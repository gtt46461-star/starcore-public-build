package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.DrawDependencyInputCP12;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.TimeSourceR811;
import java.util.*;

/** Read replicas of critical inputs. Root alone owns forecasts, results and retained draw inputs. Root triggers
 * index changes from EVERY producer, including late settlements without a new draw. Capture holds no Root writer;
 * apply is a separate research transaction, with no ATTACH. Bootstrap is bounded and durably paged; its incomplete
 * replica is never usable by a legacy writer. The journal is reconstructible control, never prediction evidence. */
public final class CriticalInputMirrorR29 {
    private CriticalInputMirrorR29(){}
    public static final String CLOCK="r29_critical_input_clock",CHANGE="r29_critical_input_change",SCHEMA="r29_critical_input_schema";
    static final String STATE="r29_critical_input_mirror",GUARD="r29_critical_input_write_guard";
    public static final int PAGE_ROWS=128,CHANGE_KEYS=64,MAX_CHANGES=16384,MAX_PAGE_BYTES=2<<20,MAX_KEY_CHARS=512;
    public static final long MAX_DATABASE_BYTES=128L<<20;
    private static final String CLOCK_DDL="CREATE TABLE IF NOT EXISTS "+CLOCK+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),seq INTEGER NOT NULL,schema_epoch INTEGER NOT NULL,floor_seq INTEGER NOT NULL,entries INTEGER NOT NULL)";
    private static final String CHANGE_DDL="CREATE TABLE IF NOT EXISTS "+CHANGE+"(seq INTEGER PRIMARY KEY,table_name TEXT NOT NULL,key1 TEXT NOT NULL,key2 TEXT NOT NULL,key3 TEXT NOT NULL,operation TEXT NOT NULL,UNIQUE(table_name,key1,key2,key3))";
    private static final String SCHEMA_DDL="CREATE TABLE IF NOT EXISTS "+SCHEMA+"(table_name TEXT PRIMARY KEY,schema_hash TEXT NOT NULL)";
    private static final Set<String> TABLES=Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(DbHelper.TABLE_R35_SHADOW_OBSERVATION,DbHelper.TABLE_R35_OOS_LABEL,DrawDependencyInputCP12.TABLE,"r26_frozen","r26_incumbent_frozen","su2_live_decision")));
    public static Set<String> tables(){return TABLES;}
    private static final Map<DbStore,Integer> INSTALLED=new WeakHashMap<>();
    static String q(String value){return MigrationCopyR29.id(value);}
    static long scalar(SQLiteDatabase db,String sql){return MigrationCopyR29.scalar(db,sql,null);}
    static void tx(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R29_CRITICAL_MIRROR_TRANSACTION_REQUIRED");}
    public static long sourceSequence(SQLiteDatabase root){return scalar(root,"SELECT seq FROM "+CLOCK+" WHERE singleton=1");}
    public static long sourceEpoch(SQLiteDatabase root){return scalar(root,"SELECT schema_epoch FROM "+CLOCK+" WHERE singleton=1");}
    static long floor(SQLiteDatabase root){return scalar(root,"SELECT floor_seq FROM "+CLOCK+" WHERE singleton=1");}
    public static String sourceViewSql(SQLiteDatabase root){try(Cursor c=root.rawQuery("SELECT sql FROM sqlite_master WHERE type='view' AND name=?",new String[]{DrawDependencyInputCP12.SOURCE})){return c.moveToFirst()?c.getString(0):null;}}
    static final class Definition{
        final MigrationCopyR29.Table table;final List<String> keys;
        Definition(SQLiteDatabase root,MigrationCopyR29.Table table){this.table=table;TreeMap<Integer,String> pk=new TreeMap<>();try(Cursor c=root.rawQuery("PRAGMA table_info("+q(table.name)+")",null)){while(c.moveToNext())if(c.getInt(5)>0)pk.put(c.getInt(5),c.getString(1));}keys=new ArrayList<>(pk.values());if(keys.isEmpty())keys.add("rowid");if(keys.size()>3)throw new IllegalStateException("R29_CRITICAL_MIRROR_KEY_ARITY:"+table.name);}
        String value(String prefix,int i){return i<keys.size()?"CAST("+prefix+"."+q(keys.get(i))+" AS TEXT)":"''";}
        String[] keys(Cursor c){String[] out={"","",""};for(int i=0;i<keys.size();i++){int index=keys.get(i).equals("rowid")?0:table.columns.indexOf(keys.get(i))+1;if(c.isNull(index))throw new IllegalStateException("R29_CRITICAL_INPUT_NULL_PRIMARY_KEY");out[i]=c.getString(index);}return out;}
    }
    static List<Definition> definitions(SQLiteDatabase root){List<Definition> out=new ArrayList<>();for(String name:TABLES){MigrationCopyR29.Table t=ResearchInputMirrorR29.descriptor(root,name);if(t!=null)out.add(new Definition(root,t));}return out;}
    static Map<String,String> triggers(Definition d){
        Map<String,String> out=new LinkedHashMap<>();for(String event:new String[]{"INSERT","DELETE","UPDATE"}){
            String name="r29_critical_track_"+d.table.name+"_"+event,prefix=event.equals("INSERT")?"NEW":"OLD";
            out.put(name,"CREATE TRIGGER IF NOT EXISTS "+q(name)+" AFTER "+event+" ON "+q(d.table.name)+" BEGIN "+journal(d,prefix,event.substring(0,1))+" END");
        }
        StringBuilder changed=new StringBuilder();for(String key:d.keys){if(changed.length()>0)changed.append(" OR ");changed.append("OLD.").append(q(key)).append(" IS NOT NEW.").append(q(key));}
        String name="r29_critical_track_"+d.table.name+"_UPDATE_KEY";out.put(name,"CREATE TRIGGER IF NOT EXISTS "+q(name)+" AFTER UPDATE ON "+q(d.table.name)+" WHEN "+changed+" BEGIN "+journal(d,"NEW","I")+" END");return out;
    }
    static String sql(SQLiteDatabase db,String type,String name){try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type=? AND name=?",new String[]{type,name})){return c.moveToFirst()?c.getString(0):null;}}
    static boolean sameSql(String a,String b){return a!=null&&b!=null&&a.replace(" IF NOT EXISTS "," ").equals(b.replace(" IF NOT EXISTS "," "));}
    /** A fresh process must not take Root's writer merely to repeat IF NOT EXISTS. Validate every installed
     * descriptor and exact tracker body read-only; missing/new/changed tables or views require real installation. */
    static boolean installedRealtime(SQLiteDatabase root){
        int version=(int)scalar(root,"PRAGMA schema_version");
        if(!sameSql(sql(root,"table",CLOCK),CLOCK_DDL)||!sameSql(sql(root,"table",CHANGE),CHANGE_DDL)||!sameSql(sql(root,"table",SCHEMA),SCHEMA_DDL))return false;
        if(scalar(root,"SELECT count(*) FROM "+CLOCK+" WHERE singleton=1")!=1)return false;
        List<Definition> all=definitions(root);if(all.isEmpty())return false;
        for(Definition d:all){try(Cursor c=root.rawQuery("SELECT schema_hash FROM "+SCHEMA+" WHERE table_name=?",new String[]{d.table.name})){if(!c.moveToFirst()||!d.table.schemaHash.equals(c.getString(0)))return false;}
            for(Map.Entry<String,String> trigger:triggers(d).entrySet())if(!sameSql(sql(root,"trigger",trigger.getKey()),trigger.getValue()))return false;
        }
        String view=sourceViewSql(root);if(view==null)return false;
        try(Cursor c=root.rawQuery("SELECT schema_hash FROM "+SCHEMA+" WHERE table_name=?",new String[]{DrawDependencyInputCP12.SOURCE})){if(!c.moveToFirst()||!StatsR27.sha256(view).equals(c.getString(0)))return false;}
        return scalar(root,"SELECT count(*) FROM "+SCHEMA)==all.size()+1&&version==(int)scalar(root,"PRAGMA schema_version");
    }
    static void recordSchema(SQLiteDatabase root,String name,String hash){String old=null;try(Cursor c=root.rawQuery("SELECT schema_hash FROM "+SCHEMA+" WHERE table_name=?",new String[]{name})){if(c.moveToFirst())old=c.getString(0);}if(!hash.equals(old)){root.execSQL("UPDATE "+CLOCK+" SET schema_epoch=schema_epoch+1 WHERE singleton=1");root.execSQL("INSERT OR REPLACE INTO "+SCHEMA+" VALUES(?,?)",new Object[]{name,hash});}}
    /** Existing short RT migration transaction only. These three control tables must remain REALTIME_ONLY. */
    public static void ensureRealtime(SQLiteDatabase root){
        tx(root);root.execSQL(CLOCK_DDL);
        root.execSQL("INSERT OR IGNORE INTO "+CLOCK+" VALUES(1,0,0,0,0)");
        root.execSQL(CHANGE_DDL);root.execSQL(SCHEMA_DDL);
        for(Definition d:definitions(root)){
            recordSchema(root,d.table.name,d.table.schemaHash);
            for(Map.Entry<String,String> trigger:triggers(d).entrySet())if(!sameSql(sql(root,"trigger",trigger.getKey()),trigger.getValue())){root.execSQL("DROP TRIGGER IF EXISTS "+q(trigger.getKey()));root.execSQL(trigger.getValue());root.execSQL("UPDATE "+CLOCK+" SET schema_epoch=schema_epoch+1 WHERE singleton=1");}
        }
        String view=sourceViewSql(root);if(view!=null)recordSchema(root,DrawDependencyInputCP12.SOURCE,StatsR27.sha256(view));
    }
    private static String journal(Definition d,String prefix,String operation){
        String a=d.value(prefix,0),b=d.value(prefix,1),c=d.value(prefix,2),identity="table_name='"+d.table.name+"' AND key1="+a+" AND key2="+b+" AND key3="+c;
        String valid="coalesce((length("+a+")<="+MAX_KEY_CHARS+" AND length("+b+")<="+MAX_KEY_CHARS+" AND length("+c+")<="+MAX_KEY_CHARS+"),0)=1";
        // Bounded overflow invalidates lagging replicas through floor_seq. It never rejects critical ingress,
        // deletes an actual forecast/task/result, or silently treats a truncated control journal as complete.
        return "UPDATE "+CLOCK+" SET seq=seq+1 WHERE singleton=1;"
            +"UPDATE "+CLOCK+" SET floor_seq=max(floor_seq,(SELECT seq FROM "+CHANGE+" ORDER BY seq LIMIT 1 OFFSET 127)) WHERE singleton=1 AND entries>="+MAX_CHANGES+" AND NOT EXISTS(SELECT 1 FROM "+CHANGE+" WHERE "+identity+");"
            +"UPDATE "+CLOCK+" SET entries=entries-(SELECT count(*) FROM "+CHANGE+" WHERE seq<=floor_seq) WHERE singleton=1;"
            +"DELETE FROM "+CHANGE+" WHERE seq<=(SELECT floor_seq FROM "+CLOCK+" WHERE singleton=1);"
            +"UPDATE "+CLOCK+" SET floor_seq=seq WHERE singleton=1 AND NOT("+valid+");"
            +"UPDATE "+CLOCK+" SET entries=entries+1 WHERE singleton=1 AND ("+valid+") AND NOT EXISTS(SELECT 1 FROM "+CHANGE+" WHERE "+identity+");"
            +"INSERT OR REPLACE INTO "+CHANGE+" SELECT seq,'"+d.table.name+"',"+a+","+b+","+c+",'"+operation+"' FROM "+CLOCK+" WHERE singleton=1 AND ("+valid+");";
    }
    /** Offline cutover may install this after excluding Root's original triggers on these replicas. */
    public static void installResearch(SQLiteDatabase research){
        tx(research);research.execSQL("CREATE TABLE IF NOT EXISTS "+STATE+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),phase TEXT NOT NULL,source_epoch INTEGER NOT NULL,source_seq INTEGER NOT NULL,base_seq INTEGER NOT NULL,table_index INTEGER NOT NULL,key1 TEXT NOT NULL,key2 TEXT NOT NULL,key3 TEXT NOT NULL,key_present INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        research.execSQL("CREATE TABLE IF NOT EXISTS "+GUARD+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),active INTEGER NOT NULL CHECK(active IN(0,1)))");research.execSQL("INSERT OR IGNORE INTO "+GUARD+" VALUES(1,0)");
        for(String name:TABLES)if(ResearchInputMirrorR29.table(research,name))for(String event:new String[]{"INSERT","UPDATE","DELETE"})research.execSQL("CREATE TRIGGER IF NOT EXISTS "+q("r29_critical_replica_"+name+"_"+event)+" BEFORE "+event+" ON "+q(name)+" WHEN (SELECT active FROM "+GUARD+" WHERE singleton=1)<>1 BEGIN SELECT RAISE(ABORT,'R29_ROOT_AUTHORITY_INPUT_IS_READ_ONLY_REPLICA'); END");
    }
    static void view(SQLiteDatabase target,String sql){if(sql==null)throw new IllegalStateException("CP3_DB_BUSY_R29_CRITICAL_SOURCE_VIEW_PENDING");target.execSQL("DROP VIEW IF EXISTS "+q(DrawDependencyInputCP12.SOURCE));target.execSQL(sql);}
    /** Caller must prove seq/epoch unchanged across its complete initial copy, not merely read them afterwards. */
    public static void initialized(SQLiteDatabase target,long sourceEpoch,long sourceSeq,String originalViewSql){installResearch(target);view(target,originalViewSql);target.execSQL("INSERT OR REPLACE INTO "+STATE+" VALUES(1,'READY',?,?,?,0,'','','',0,?)",new Object[]{sourceEpoch,sourceSeq,sourceSeq,System.currentTimeMillis()});}
    static final class State{
        String phase;long epoch,seq,base;int index;String[] key;boolean keyPresent;
        String identity(){return JsonR27.write(Arrays.asList(phase,epoch,seq,base,index,key[0],key[1],key[2],keyPresent));}
    }
    static State state(SQLiteDatabase db){if(!ResearchInputMirrorR29.table(db,STATE))return null;try(Cursor c=db.rawQuery("SELECT phase,source_epoch,source_seq,base_seq,table_index,key1,key2,key3,key_present FROM "+STATE+" WHERE singleton=1",null)){if(!c.moveToFirst())return null;State s=new State();s.phase=c.getString(0);s.epoch=c.getLong(1);s.seq=c.getLong(2);s.base=c.getLong(3);s.index=c.getInt(4);s.key=new String[]{c.getString(5),c.getString(6),c.getString(7)};s.keyPresent=c.getInt(8)!=0;return s;}}
    static final class Change {Definition d;String[] key;MigrationCopyR29.Row row;long seq;}
    static final class Step{
        State expected,next;long head,epoch;String view;List<Definition> definitions;Definition page;List<MigrationCopyR29.Row> rows=new ArrayList<>();List<Change> changes=new ArrayList<>();boolean reset;
    }
    static String select(Definition d){StringBuilder b=new StringBuilder(ResearchRowsR29.withoutRowid(d.table)?"0":"rowid");for(String column:d.table.columns)b.append(',').append(q(column));return b.toString();}
    static List<String> bindKeys(Definition d,String[] key,StringBuilder predicate,boolean after){
        List<String> args=new ArrayList<>();if(after){for(int i=0;i<d.keys.size();i++){if(i>0)predicate.append(" OR ");predicate.append('(');for(int j=0;j<i;j++){predicate.append(q(d.keys.get(j))).append("=? AND ");args.add(key[j]);}predicate.append(q(d.keys.get(i))).append(">?)");args.add(key[i]);}}
        else for(int i=0;i<d.keys.size();i++){if(i>0)predicate.append(" AND ");predicate.append(q(d.keys.get(i))).append("=?");args.add(key[i]);}return args;
    }
    static Step capture(SQLiteDatabase root,SQLiteDatabase target){
        Step step=new Step();step.head=sourceSequence(root);step.epoch=sourceEpoch(root);step.view=sourceViewSql(root);step.definitions=definitions(root);step.expected=state(target);
        for(String required:Arrays.asList(DbHelper.TABLE_R35_SHADOW_OBSERVATION,DbHelper.TABLE_R35_OOS_LABEL,DrawDependencyInputCP12.TABLE))if(!ResearchInputMirrorR29.table(root,required))throw new IllegalStateException("CP3_DB_BUSY_R29_CRITICAL_SOURCE_SCHEMA_PENDING:"+required);
        State old=step.expected;step.reset=old==null||old.epoch!=step.epoch||old.seq<floor(root)||(!"READY".equals(old.phase)&&old.base<floor(root));
        if(step.reset){State next=new State();next.phase="BOOTSTRAP";next.epoch=step.epoch;next.seq=next.base=step.head;next.key=new String[]{"","",""};step.next=next;}
        else {State next=new State();next.phase=old.phase;next.epoch=old.epoch;next.seq=old.seq;next.base=old.base;next.index=old.index;next.key=old.key.clone();next.keyPresent=old.keyPresent;step.next=next;}
        State next=step.next;
        if("BOOTSTRAP".equals(next.phase)){
            if(next.index>=step.definitions.size()){next.phase="CATCHUP";next.key=new String[]{"","",""};next.keyPresent=false;}
            else{
                Definition d=step.page=step.definitions.get(next.index);StringBuilder predicate=new StringBuilder();List<String> args=!next.keyPresent?Collections.emptyList():bindKeys(d,next.key,predicate,true);if(predicate.length()==0)predicate.append("1=1");StringBuilder order=new StringBuilder();for(String k:d.keys){if(order.length()>0)order.append(',');order.append(q(k));}
                int bytes=0;try(Cursor c=root.rawQuery("SELECT "+select(d)+" FROM "+q(d.table.name)+" WHERE "+predicate+" ORDER BY "+order+" LIMIT "+PAGE_ROWS,args.toArray(new String[0]))){while(c.moveToNext()){byte[] payload=MigrationCopyR29.encode(c,1,d.table.columns.size());if(bytes+payload.length>MAX_PAGE_BYTES){if(step.rows.isEmpty())throw new IllegalStateException("R29_CRITICAL_INPUT_MIRROR_ROW_EXCEEDS_PAGE_BUDGET");break;}bytes+=payload.length;step.rows.add(new MigrationCopyR29.Row(c.getLong(0),payload,StatsR27.sha256(payload)));next.key=d.keys(c);next.keyPresent=true;}}
                if(step.rows.isEmpty()){next.index++;next.key=new String[]{"","",""};next.keyPresent=false;if(next.index==step.definitions.size())next.phase="CATCHUP";}
            }
        }else{
            Map<String,Definition> byName=new HashMap<>();for(Definition d:step.definitions)byName.put(d.table.name,d);long last=next.seq;
            try(Cursor c=root.rawQuery("SELECT seq,table_name,key1,key2,key3 FROM "+CHANGE+" WHERE seq>? AND seq<=? ORDER BY seq LIMIT "+CHANGE_KEYS,new String[]{String.valueOf(next.seq),String.valueOf(step.head)})){
                while(c.moveToNext()){Change change=new Change();change.seq=c.getLong(0);change.d=byName.get(c.getString(1));if(change.d==null)throw new IllegalStateException("R29_CRITICAL_SOURCE_TABLE_DISAPPEARED");change.key=new String[]{c.getString(2),c.getString(3),c.getString(4)};step.changes.add(change);}
            }
            int bytes=0,processed=0;for(Change change:step.changes){StringBuilder predicate=new StringBuilder();List<String> args=bindKeys(change.d,change.key,predicate,false);try(Cursor row=root.rawQuery("SELECT "+select(change.d)+" FROM "+q(change.d.table.name)+" WHERE "+predicate,args.toArray(new String[0]))){if(row.moveToFirst()){byte[] payload=MigrationCopyR29.encode(row,1,change.d.table.columns.size());if(bytes+payload.length>MAX_PAGE_BYTES){if(processed==0)throw new IllegalStateException("R29_CRITICAL_INPUT_CHANGE_ROW_EXCEEDS_BUDGET");break;}bytes+=payload.length;change.row=new MigrationCopyR29.Row(row.getLong(0),payload,StatsR27.sha256(payload));}}processed++;last=change.seq;}
            if(processed<step.changes.size())step.changes=new ArrayList<>(step.changes.subList(0,processed));
            next.seq=step.changes.isEmpty()?step.head:last;next.phase=next.seq==step.head?"READY":"CATCHUP";
        }
        if(step.head!=sourceSequence(root)||step.epoch!=sourceEpoch(root))throw new IllegalStateException("CP3_DB_BUSY_R29_CRITICAL_SOURCE_CHANGED_DURING_CAPTURE");return step;
    }
    static void targetSchema(SQLiteDatabase target,Definition d){
        if(!ResearchInputMirrorR29.table(target,d.table.name)){target.execSQL(d.table.createSql);return;}
        MigrationCopyR29.Table actual=ResearchInputMirrorR29.descriptor(target,d.table.name);
        if(!actual.columns.equals(d.table.columns))throw new IllegalStateException("R29_CRITICAL_INPUT_REPLICA_SCHEMA_MIGRATION_REQUIRED:"+d.table.name);
    }
    static void deleteKey(SQLiteDatabase target,Definition d,String[] key){StringBuilder predicate=new StringBuilder();List<String> args=bindKeys(d,key,predicate,false);target.execSQL("DELETE FROM "+q(d.table.name)+" WHERE "+predicate,args.toArray(new Object[0]));}
    static void restoreRows(SQLiteDatabase target,Definition d,List<MigrationCopyR29.Row> rows){
        if(d.keys.equals(Collections.singletonList("rowid"))){ResearchCutoverR29.restorePage(target,d.table,rows);return;}
        // Cross-domain identity is the declared PK (and every explicit _id column), not SQLite's recyclable
        // hidden rowid. Preserving an unrelated old rowid can collide when a deleted source key is reinserted.
        StringBuilder columns=new StringBuilder(),values=new StringBuilder();for(String c:d.table.columns){if(columns.length()>0){columns.append(',');values.append(',');}columns.append(q(c));values.append('?');}
        for(MigrationCopyR29.Row row:rows){if(!StatsR27.sha256(row.payload).equals(row.hash))throw new IllegalStateException("R29_CRITICAL_INPUT_PAYLOAD_CHECKSUM");target.execSQL("INSERT INTO "+q(d.table.name)+"("+columns+") VALUES("+values+")",ResearchCutoverR29.decode(row.payload,d.table.columns.size()));}
    }
    static boolean apply(SQLiteDatabase target,Step step){
        tx(target);State actual=state(target);if(!Objects.equals(actual==null?null:actual.identity(),step.expected==null?null:step.expected.identity()))throw new IllegalStateException("CP3_DB_BUSY_R29_STALE_CRITICAL_INPUT_CAPTURE");
        if(scalar(target,"PRAGMA page_count")*scalar(target,"PRAGMA page_size")+MAX_PAGE_BYTES>MAX_DATABASE_BYTES)throw new IllegalStateException("R29_CRITICAL_INPUT_REPLICA_DATABASE_STORAGE_BUDGET");
        installResearch(target);target.execSQL("UPDATE "+GUARD+" SET active=1 WHERE singleton=1");
        for(Definition d:step.definitions)targetSchema(target,d);
        if(step.reset){for(Definition d:step.definitions)target.execSQL("DELETE FROM "+q(d.table.name));view(target,step.view);}
        if(step.page!=null)restoreRows(target,step.page,step.rows);
        for(Change change:step.changes)deleteKey(target,change.d,change.key);
        for(Change change:step.changes)if(change.row!=null)restoreRows(target,change.d,Collections.singletonList(change.row));
        installResearch(target); // newly created replica tables also get read-only guards
        target.execSQL("UPDATE "+GUARD+" SET active=0 WHERE singleton=1");State next=step.next;
        target.execSQL("INSERT OR REPLACE INTO "+STATE+" VALUES(1,?,?,?,?,?,?,?,?,?,?)",new Object[]{next.phase,next.epoch,next.seq,next.base,next.index,next.key[0],next.key[1],next.key[2],next.keyPresent?1:0,System.currentTimeMillis()});return "READY".equals(next.phase);
    }
    static void acknowledge(SQLiteDatabase root,long applied){
        tx(root);root.execSQL("UPDATE "+CLOCK+" SET floor_seq=max(floor_seq,coalesce((SELECT max(seq) FROM (SELECT seq FROM "+CHANGE+" WHERE seq<=? ORDER BY seq LIMIT 256)),0)) WHERE singleton=1",new Object[]{applied});
        root.execSQL("DELETE FROM "+CHANGE+" WHERE seq IN(SELECT seq FROM "+CHANGE+" WHERE seq<=? ORDER BY seq LIMIT 256)",new Object[]{applied});
        root.execSQL("UPDATE "+CLOCK+" SET entries=max(0,entries-changes()) WHERE singleton=1");
    }
    static void cleanupAck(Runnable cleanup,java.util.function.Consumer<RuntimeException> deferred){try{cleanup.run();}catch(RuntimeException failure){deferred.accept(failure);}}
    public static void refresh(DbStore store){
        if(ResearchRouteR29.writerHeldByCurrentThread())throw new IllegalStateException("R29_CRITICAL_INPUT_CAPTURE_INSIDE_RESEARCH_WRITER_FORBIDDEN");
        SQLiteDatabase root=LegacyResearchScopeR29.rawRealtime(store);if(!ResearchRouteR29.active(root))return;
        int version=(int)scalar(root,"PRAGMA schema_version");boolean install;synchronized(INSTALLED){install=!Objects.equals(INSTALLED.get(store),version);}
        if(install){int installedVersion=version;
            if(!installedRealtime(root))installedVersion=LegacyResearchScopeR29.realtimeWrite(store,"R29_INSTALL_CRITICAL_INPUT_CHANGE_INDEX",db->{ensureRealtime(db);if(!installedRealtime(db))throw new IllegalStateException("CP3_DB_BUSY_R29_CRITICAL_CHANGE_SCHEMA_NOT_READY");return (int)scalar(db,"PRAGMA schema_version");});
            synchronized(INSTALLED){INSTALLED.put(store,installedVersion);}}
        SQLiteDatabase research=ResearchRouteR29.reader(root);State before=state(research);long head=sourceSequence(root),epoch=sourceEpoch(root);
        if(before!=null&&"READY".equals(before.phase)&&before.seq==head&&before.epoch==epoch)return;
        Step step=capture(root,research);boolean ready=ResearchRouteR29.write(store,"R29_REFRESH_CRITICAL_INPUT_REPLICAS",db->apply(db,step));
        if(!"BOOTSTRAP".equals(step.next.phase))cleanupAck(()->{try(DatabaseCoordinator.WriterDeadlineScopeCP3 ignored=DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs()+25)){LegacyResearchScopeR29.realtimeWrite(store,"R29_ACK_CRITICAL_INPUT_REPLICA",db->{acknowledge(db,step.next.seq);return 0;});}},failure->android.util.Log.w("STARCORE_R29","critical input cleanup ACK deferred; research replica already committed",failure));
        if(!ready)throw new IllegalStateException("CP3_DB_BUSY_R29_CRITICAL_INPUT_BOOTSTRAP_OR_CATCHUP");
    }
}
