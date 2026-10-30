package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

/** Actual production R26/R27/SU1/SU2 schema and durable draw-event protocol on WAL files. This measures the
 * complete copy plus both rehashes with 4200 draws; it is not Android timing or natural learning evidence. */
public final class FullSchemaCutoverR29HostTest {
    static final int DRAWS=4200;static int checks;
    static final class Counted extends SQLiteDatabase {
        long queries,executes,smallCellQueries,encodingQueries;
        Counted(Path path)throws Exception{super(DriverManager.getConnection("jdbc:sqlite:"+path));}
        @Override public Cursor rawQuery(String sql,String[]args){queries++;if(sql.startsWith("SELECT substr(CAST("))smallCellQueries++;if(sql.equals("PRAGMA encoding"))encodingQueries++;return super.rawQuery(sql,args);}
        @Override public void execSQL(String sql,Object[]args){executes++;super.execSQL(sql,args);}
        void reset(){queries=executes=smallCellQueries=encodingQueries=0;}
    }
    static void check(String name,boolean ok){System.out.println("R29_FULL_SCHEMA_CUTOVER|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    interface Work{void run()throws Exception;}
    static void tx(SQLiteDatabase db,Work work)throws Exception{db.jdbc.setAutoCommit(false);try{work.run();db.jdbc.commit();}catch(Exception failed){db.jdbc.rollback();throw failed;}finally{db.jdbc.setAutoCommit(true);}}
    static void ddl(SQLiteDatabase db,String[] statements,String owner){for(int i=0;i<statements.length;i++)try{db.execSQL(statements[i]);}catch(RuntimeException fail){throw new IllegalStateException("ACTUAL_SCHEMA_FAILED:"+owner+":"+i+":"+statements[i].substring(0,Math.min(120,statements[i].length())),fail);}}
    static void schema(SQLiteDatabase db)throws Exception{
        // Exact final DbHelper.createBaseTables + migrateDrawColumns shape; no arbitrary synthetic text columns.
        db.execSQL("CREATE TABLE draws(_id INTEGER PRIMARY KEY AUTOINCREMENT,issue TEXT NOT NULL UNIQUE,open_time INTEGER DEFAULT 0,fetch_time INTEGER DEFAULT 0,source_line TEXT,n1 INTEGER DEFAULT 0,n2 INTEGER DEFAULT 0,n3 INTEGER DEFAULT 0,n4 INTEGER DEFAULT 0,n5 INTEGER DEFAULT 0,n6 INTEGER DEFAULT 0,n7 INTEGER DEFAULT 0,n8 INTEGER DEFAULT 0,n9 INTEGER DEFAULT 0,n10 INTEGER DEFAULT 0,raw_json TEXT,dirty INTEGER DEFAULT 0,dirty_reason TEXT)");
        db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER DEFAULT 0)");
        tx(db,()->{ddl(db,RuntimeSchemaR26.DDL,"RuntimeSchemaR26");ddl(db,SchemaR27.DDL,"SchemaR27");ddl(db,SqlStatementsSU1.SQL,"SqlStatementsSU1");ddl(db,StrategySchemaSU2.DDL,"StrategySchemaSU2");});
    }
    static void populate(SQLiteDatabase db)throws Exception{
        long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001");
        tx(db,()->{for(int i=0;i<DRAWS;i++){
            String issue=TargetIssueResolver.fromSequenceOrdinalSU1(first+i);Object[] row=new Object[17];row[0]=issue;row[1]=1792627200000L+i*60000L;row[2]=(Long)row[1]+1000;row[3]="SYNTHETIC_CANONICAL_HISTORY";for(int p=0;p<10;p++)row[4+p]=(p+i%7)%10+1;row[14]="{\"scope\":\"SYNTHETIC_NOT_NATURAL\"}";row[15]=0;row[16]="";
            db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,source_line,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,raw_json,dirty,dirty_reason) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",row);
            ResearchEventBridgeR29.appendCanonical(db,issue,true,1792627201000L+i*60000L);
        }});
        for(;;){List<ResearchEventBridgeR29.Event> events=ResearchEventBridgeR29.pending(db);if(events.isEmpty())break;ResearchWriteQueueR27.Batch batch=ResearchEventBridgeR29.prepare(events,System.currentTimeMillis());tx(db,()->ResearchWriteQueueR27.execute(db,batch,"FULL_SCHEMA_ACTUAL_DELIVERY"));tx(db,()->ResearchEventBridgeR29.acknowledge(db,events,System.currentTimeMillis()));}
    }
    public static void main(String[]args)throws Exception{
        Path directory=Files.createTempDirectory("r29-full-schema-cutover-");
        try(Counted source=new Counted(directory.resolve("source.db"));Counted target=new Counted(directory.resolve("target.db"))){
            source.execSQL("PRAGMA journal_mode=WAL");source.execSQL("PRAGMA synchronous=FULL");target.execSQL("PRAGMA journal_mode=WAL");target.execSQL("PRAGMA synchronous=FULL");
            long setup=System.nanoTime();schema(source);populate(source);
            long tableCount=n(source,"SELECT count(*) FROM sqlite_master WHERE type='table'");
            check("F1_ACTUAL_FULL_SCHEMA_AND_CANONICAL_EVENTS",tableCount>=100&&n(source,"SELECT count(*) FROM draws")==DRAWS&&n(source,"SELECT count(*) FROM r29_event_outbox")==DRAWS);
            check("F2_REAL_PROTOCOL_INBOX_AND_TWO_WORK_ITEMS_PER_DRAW",n(source,"SELECT count(*) FROM r29_event_inbox")==DRAWS&&n(source,"SELECT count(*) FROM r29_learning_work")==2L*DRAWS&&n(source,"SELECT count(*) FROM r29_learning_receipt")==0);
            System.out.println("R29_FULL_SCHEMA_CUTOVER|SETUP|tables="+tableCount+"|draws="+DRAWS+"|work="+(2*DRAWS)+"|milliseconds="+((System.nanoTime()-setup)/1000000));
            source.reset();target.reset();long started=System.nanoTime();List<MigrationCopyR29.Table> tables=ResearchCutoverR29.tables(source);long copied=0,pages=0;
            tx(target,()->{for(MigrationCopyR29.Table table:tables)target.execSQL(table.createSql);});
            for(MigrationCopyR29.Table table:tables){Long after=null;for(;;){List<MigrationCopyR29.Row> rows=ResearchCutoverR29.page(source,table,after);if(rows.isEmpty())break;tx(target,()->ResearchCutoverR29.restorePage(target,table,rows));copied+=rows.size();pages++;after=rows.get(rows.size()-1).rowid;}}
            String destination=ResearchCutoverR29.root(target,tables,Long.MAX_VALUE,()->false),original=ResearchCutoverR29.root(source,tables,Long.MAX_VALUE,()->false);long elapsed=(System.nanoTime()-started)/1000000,queries=source.queries+target.queries;
            System.out.println("R29_FULL_SCHEMA_CUTOVER|MEASURED|ownedTables="+tables.size()+"|copiedRows="+copied+"|pages="+pages+"|rawQueries="+queries+"|sourceQueries="+source.queries+"|targetQueries="+target.queries+"|cellQueries="+(source.smallCellQueries+target.smallCellQueries)+"|encodingQueries="+(source.encodingQueries+target.encodingQueries)+"|milliseconds="+elapsed);
            check("F3_WHOLE_COPY_AND_BOTH_TYPED_REHASHES_AGREE",destination.equals(original)&&n(target,"SELECT count(*) FROM draws")==DRAWS&&n(target,"SELECT count(*) FROM r29_learning_work")==2L*DRAWS);
            // This is a deterministic query-amplification boundary, never a HOST wall-clock claim about Android.
            check("F4_4200_DRAW_COPY_DOES_NOT_ISSUE_PER_SMALL_CELL_SQL",queries<12000&&source.smallCellQueries+target.smallCellQueries<1000&&source.encodingQueries+target.encodingQueries<100);
            check("F5_SOURCE_DATA_AND_PENDING_LEARNING_ARE_RETAINED",source.executes==0&&n(source,"SELECT count(*) FROM r29_learning_work WHERE status='PENDING'")==8400&&n(source,"SELECT count(*) FROM draws")==DRAWS&&n(source,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0")==DRAWS);
        }finally{try(java.util.stream.Stream<Path> files=Files.walk(directory)){files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}}
        System.out.println("R29_FULL_SCHEMA_CUTOVER|ALL_PASS|checks="+checks+"|ACTUAL_SCHEMA_REAL_WAL_HOST_ONLY_ANDROID_RUNTIME_PENDING");
    }
}
