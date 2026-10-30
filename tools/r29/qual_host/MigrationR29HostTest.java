package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

/** Real source/destination FILES, independent locks, per-commit interruption, reopen, corruption and SQLITE_FULL.
 * Tests the staged backup only; no claim of complete routing/cutover or physical-device evidence. */
public final class MigrationR29HostTest {
    static int checks;static long now=10_000;
    interface Body{void run()throws Exception;}
    static void check(String name,boolean ok){System.out.println("R29_MIGRATION|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static SQLiteDatabase open(Path path)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+path));
        db.execSQL("PRAGMA journal_mode=WAL");db.execSQL("PRAGMA synchronous=FULL");return db;
    }
    static void tx(SQLiteDatabase db,Body body,boolean kill)throws Exception{
        db.jdbc.setAutoCommit(false);try{body.run();if(kill)throw new IllegalStateException("SIMULATED_PROCESS_KILL");db.jdbc.commit();}
        catch(Exception failure){db.jdbc.rollback();throw failure;}finally{db.jdbc.setAutoCommit(true);}
    }
    static MigrationCopyR29.Transaction writer(SQLiteDatabase db,boolean kill){return body->tx(db,()->body.run(db),kill);}
    static long n(SQLiteDatabase db,String sql){return MigrationCopyR29.scalar(db,sql,null);}
    static String value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static void ddl(SQLiteDatabase destination)throws Exception{tx(destination,()->{for(String sql:MigrationCopyR29.DESTINATION_DDL)destination.execSQL(sql);},false);}
    static void prepare(SQLiteDatabase source,SQLiteDatabase destination,List<MigrationCopyR29.Table> tables)throws Exception{
        for(MigrationCopyR29.Table t:tables)tx(source,()->MigrationCopyR29.trackSource(source,t),false);
        tx(destination,()->MigrationCopyR29.register(destination,tables),false);
    }
    static MigrationCopyR29.Result step(SQLiteDatabase source,SQLiteDatabase destination,List<MigrationCopyR29.Table> tables)throws Exception{
        now+=2000;return MigrationCopyR29.step(source,destination,tables,writer(destination,false),now);
    }
    static String sourceDigest(SQLiteDatabase source,List<MigrationCopyR29.Table> tables){
        StringBuilder all=new StringBuilder();
        for(MigrationCopyR29.Table t:tables){Long last=null;String hash=t.seed();for(;;){List<MigrationCopyR29.Row> page=MigrationCopyR29.readPage(source,t,last);if(page.isEmpty())break;for(MigrationCopyR29.Row row:page){hash=MigrationCopyR29.chain(hash,row.rowid,row.hash);last=row.rowid;}}all.append(t.name).append(':').append(hash).append('|');}
        return StatsR27.sha256(all.toString());
    }
    static void finish(SQLiteDatabase source,SQLiteDatabase destination,List<MigrationCopyR29.Table> tables)throws Exception{
        for(int i=0;i<1000&&n(destination,"SELECT count(*) FROM r29_copy_manifest WHERE state<>'VERIFIED_BASELINE'")>0;i++)step(source,destination,tables);
        if(n(destination,"SELECT count(*) FROM r29_copy_manifest WHERE state<>'VERIFIED_BASELINE'")!=0)throw new AssertionError("migration failed to finish stable source");
    }
    static String text(DataInputStream in)throws Exception{int n=in.readInt();byte[] b=new byte[n];in.readFully(b);return new String(b,StandardCharsets.UTF_8);}
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory("starcore-r29-migration-");Path src=dir.resolve("source.db"),dst=dir.resolve("stage.db");
        SQLiteDatabase source=open(src),destination=open(dst);
        source.execSQL("CREATE TABLE r29_fixture(id INTEGER PRIMARY KEY,i INTEGER,r REAL,t TEXT,b BLOB,n TEXT)");
        source.execSQL("CREATE TABLE r29_empty(key TEXT PRIMARY KEY)");
        source.execSQL("CREATE TABLE r29_event_outbox(secret TEXT)");source.execSQL("CREATE TABLE r27_chart_cache(derived TEXT)");source.execSQL("CREATE TABLE r27_fetch_reject(realtime TEXT)");
        for(int i=0;i<113;i++)source.execSQL("INSERT INTO r29_fixture VALUES(?,?,?,?,?,?)",new Object[]{i-40,i%2==0?Long.MAX_VALUE-i:null,i*.25,"汉字\n\u0000"+i,new byte[]{0,1,(byte)i},i%2==0?null:"NULL"});
        source.execSQL("INSERT INTO r29_fixture VALUES(?,1,1.5,'min-rowid',X'FF',NULL)",new Object[]{Long.MIN_VALUE});
        List<MigrationCopyR29.Table> tables=MigrationCopyR29.tables(source);String before=sourceDigest(source,tables);
        check("M01_OWNERSHIP_EXCLUDES_REALTIME_AND_RECOMPUTABLE",tables.size()==2&&tables.stream().noneMatch(t->t.name.equals("r29_event_outbox")||t.name.equals("r27_chart_cache")||t.name.equals("r27_fetch_reject")));
        ddl(destination);prepare(source,destination,tables);
        try(Cursor c=source.rawQuery("SELECT NULL,'NULL',CAST(1 AS INTEGER),CAST(1.0 AS REAL),X'0001'",null)){
            c.moveToFirst();DataInputStream in=new DataInputStream(new ByteArrayInputStream(MigrationCopyR29.encode(c,0,5)));
            boolean ok=in.readInt()==0x29DB0001&&in.readInt()==5&&in.readByte()==0&&in.readByte()==3&&text(in).equals("NULL")&&in.readByte()==1&&in.readLong()==1&&in.readByte()==2&&in.readLong()==Double.doubleToLongBits(1.0)&&in.readByte()==4&&in.readInt()==2&&in.readByte()==0&&in.readByte()==1&&in.available()==0;
            check("M02_TYPED_CODEC_NULL_TEXT_INTEGER_FLOAT_BLOB",ok);
        }
        // Kill after executing a destination page but before its transaction commits, then reopen the real file.
        int interrupted=0,reopened=0;
        for(int i=0;i<1000&&n(destination,"SELECT count(*) FROM r29_copy_manifest WHERE state<>'VERIFIED_BASELINE'")>0;i++){
            // Kill once at EACH next commit boundary, then retry that boundary successfully.
            // Random sampling had completed the small fixture after only three injected interruptions.
            now+=2000;boolean kill=i%2==0;
            try{MigrationCopyR29.step(source,destination,tables,writer(destination,kill),now);}catch(IllegalStateException expected){if(!expected.getMessage().equals("SIMULATED_PROCESS_KILL"))throw expected;interrupted++;}
            if(i%3==0){destination.jdbc.close();destination=open(dst);reopened++;}
        }
        System.out.println("R29_MIGRATION|INTERRUPTION_COVERAGE|kills="+interrupted+"|reopens="+reopened);
        check("M03_REOPEN_AND_EACH_COMMIT_ROLLBACK_RESUME",interrupted>3&&reopened>3&&n(destination,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==2);
        check("M04_NO_LOST_DUPLICATE_OR_NEGATIVE_ROWIDS",n(destination,"SELECT count(*) FROM r29_copy_rows")==114&&n(destination,"SELECT count(*) FROM r29_copy_rows WHERE source_rowid=-9223372036854775808")==1&&n(destination,"SELECT count(*) FROM (SELECT table_name,attempt,source_rowid,count(*) n FROM r29_copy_rows GROUP BY 1,2,3 HAVING n<>1)")==0);
        check("M05_SOURCE_DATA_UNCHANGED",before.equals(sourceDigest(source,tables))&&n(source,"SELECT count(*) FROM r29_fixture")==114);
        check("M06_COUNTS_AND_CANONICAL_CHAINS_MATCH",n(destination,"SELECT count(*) FROM r29_copy_manifest WHERE source_rows=copied_rows AND copied_rows=verified_rows AND copy_hash=verify_hash AND verified_at>0")==2);
        boolean update=false,delete=false,insert=false;
        try{destination.execSQL("UPDATE r29_copy_rows SET row_hash='changed'");}catch(RuntimeException expected){update=true;}
        try{destination.execSQL("DELETE FROM r29_copy_rows");}catch(RuntimeException expected){delete=true;}
        try{destination.execSQL("INSERT INTO r29_copy_rows SELECT table_name,attempt,999999,payload,row_hash FROM r29_copy_rows LIMIT 1");}catch(RuntimeException expected){insert=true;}
        check("M07_VERIFIED_BACKUP_REJECTS_UPDATE_DELETE_INSERT",update&&delete&&insert&&n(destination,"SELECT count(*) FROM r29_copy_rows")==114);
        source.execSQL("UPDATE r29_fixture SET t='changed after verified snapshot' WHERE id=1");
        Map<String,Object> diag=MigrationCopyR29.diagnostics(source,destination);boolean catchup=false;
        for(Object row:(List<?>)diag.get("tables")){Map<?,?> m=(Map<?,?>)row;if(m.get("table").equals("r29_fixture"))catchup=Boolean.TRUE.equals(m.get("catchUpRequired"));}
        check("M08_LATER_WRITES_REQUIRE_CATCHUP_NEVER_CUTOVER",catchup&&Boolean.FALSE.equals(diag.get("cutoverEnabled"))&&n(destination,"SELECT count(*) FROM r29_copy_rows")==114);
        // A source mutation in the middle of an incomplete copy invalidates that attempt, not a verified snapshot.
        SQLiteDatabase changed=open(dir.resolve("changed.db"));ddl(changed);prepare(source,changed,tables);
        for(int i=0;i<20&&n(changed,"SELECT count(*) FROM r29_copy_rows WHERE table_name='r29_fixture'")==0;i++)step(source,changed,tables);
        source.execSQL("UPDATE r29_fixture SET i=123 WHERE id=2");
        boolean retried=false;for(int i=0;i<30;i++){MigrationCopyR29.Result r=step(source,changed,tables);if(r.table!=null&&r.table.equals("r29_fixture")&&r.phase.startsWith("SOURCE_CHANGED")){retried=true;break;}}
        finish(source,changed,tables);
        check("M09_SOURCE_MUTATION_RESTARTS_INCOMPLETE_COPY",retried&&n(changed,"SELECT count(*) FROM r29_copy_rows")==114&&n(changed,"SELECT attempt FROM r29_copy_manifest WHERE table_name='r29_fixture'")>=2);
        // The target writer does not request a realtime write lock. Hold a separate writer on the old file.
        SQLiteDatabase independent=open(dir.resolve("independent.db"));ddl(independent);prepare(source,independent,tables);
        SQLiteDatabase held=open(src);held.jdbc.setAutoCommit(false);held.execSQL("UPDATE r29_fixture SET i=i WHERE id=3");
        try{for(int i=0;i<8;i++)step(source,independent,tables);}finally{held.jdbc.rollback();held.jdbc.setAutoCommit(true);held.jdbc.close();}
        check("M10_INDEPENDENT_DESTINATION_WHILE_REALTIME_WRITER_HELD",n(independent,"SELECT count(*) FROM r29_copy_rows")>0);
        // Verify uses destination bytes, not the source's earlier hash or a stored success count.
        SQLiteDatabase corrupt=open(dir.resolve("corrupt.db"));ddl(corrupt);prepare(source,corrupt,tables);
        for(int i=0;i<100&&n(corrupt,"SELECT count(*) FROM r29_copy_manifest WHERE table_name='r29_fixture' AND state='VERIFYING'")==0;i++)step(source,corrupt,tables);
        corrupt.execSQL("UPDATE r29_copy_rows SET payload=X'00' WHERE table_name='r29_fixture' AND source_rowid=(SELECT min(source_rowid) FROM r29_copy_rows WHERE table_name='r29_fixture')");
        boolean rejected=false;for(int i=0;i<30;i++)try{step(source,corrupt,tables);}catch(IllegalStateException expected){rejected=expected.getMessage().startsWith("R29_DESTINATION_ROW_CHECKSUM");break;}
        check("M11_CORRUPT_BYTES_CANNOT_BECOME_VERIFIED",rejected&&n(corrupt,"SELECT count(*) FROM r29_copy_manifest WHERE table_name='r29_fixture' AND verified_at>0")==0);
        // A real SQLITE_FULL abort must not advance the page cursor or destroy the old data.
        SQLiteDatabase fullSource=open(dir.resolve("full-source.db")),full=open(dir.resolve("full-stage.db"));
        fullSource.execSQL("CREATE TABLE r29_large(id INTEGER PRIMARY KEY,payload BLOB)");fullSource.execSQL("INSERT INTO r29_large VALUES(1,?)",new Object[]{new byte[96<<10]});
        List<MigrationCopyR29.Table> large=MigrationCopyR29.tables(fullSource);ddl(full);prepare(fullSource,full,large);step(fullSource,full,large);
        full.execSQL("PRAGMA max_page_count="+(n(full,"PRAGMA page_count")+1));
        rejected=false;try{step(fullSource,full,large);}catch(RuntimeException expected){rejected=expected.getMessage().toLowerCase().contains("full");}
        check("M12_REAL_SQLITE_FULL_ROLLS_BACK_PAGE_AND_CURSOR",rejected&&n(full,"SELECT count(*) FROM r29_copy_rows")==0&&n(full,"SELECT copied_rows FROM r29_copy_manifest")==0&&n(fullSource,"SELECT length(payload) FROM r29_large")== (96<<10));
        full.execSQL("PRAGMA max_page_count=100000");finish(fullSource,full,large);
        check("M13_DISK_CAPACITY_RECOVERY_RESUMES",n(full,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==1);
        // Stale initializers must be fenced BEFORE deleting the previous incomplete attempt's rows.
        SQLiteDatabase stale=open(dir.resolve("stale.db"));ddl(stale);prepare(fullSource,stale,large);
        stale.execSQL("INSERT INTO r29_copy_rows VALUES('r29_large',0,1,X'01','old-incomplete')");
        MigrationCopyR29.Transaction competing=body->{tx(stale,()->stale.execSQL("UPDATE r29_copy_manifest SET attempt=attempt+1"),false);writer(stale,false).write(body);};
        rejected=false;try{MigrationCopyR29.step(fullSource,stale,large,competing,now+=2000);}catch(IllegalStateException expected){rejected=expected.getMessage().equals("R29_STALE_MIGRATION_PAGE");}
        check("M14_STALE_PAGE_CANNOT_DELETE_OTHER_ATTEMPT",rejected&&n(stale,"SELECT count(*) FROM r29_copy_rows")==1);
        // Schema drift must be explicit; a typed row's old column set must not silently become the new schema.
        SQLiteDatabase drift=open(dir.resolve("drift.db"));ddl(drift);prepare(fullSource,drift,large);fullSource.execSQL("ALTER TABLE r29_large ADD COLUMN new_field TEXT");
        rejected=false;try{step(fullSource,drift,large);}catch(IllegalStateException expected){rejected=expected.getMessage().startsWith("R29_SOURCE_SCHEMA_CHANGED_DURING_COPY");}
        check("M15_SCHEMA_DRIFT_STOPS_WITHOUT_SOURCE_LOSS",rejected&&n(fullSource,"SELECT length(payload) FROM r29_large")== (96<<10)&&n(drift,"SELECT count(*) FROM r29_copy_manifest WHERE verified_at>0")==0);
        try(SQLiteDatabase earlySource=open(dir.resolve("early-source.db"));SQLiteDatabase earlyDestination=open(dir.resolve("early-stage.db"))){
            earlySource.execSQL("CREATE TABLE r29_a_empty(key TEXT PRIMARY KEY)");for(int i=0;i<30;i++)earlySource.execSQL("CREATE TABLE r29_z_"+i+"(key TEXT PRIMARY KEY)");
            ddl(earlyDestination);List<MigrationCopyR29.Table> earlyTables=MigrationCopyR29.tables(earlySource);prepare(earlySource,earlyDestination,earlyTables);
            for(int i=0;i<3;i++)step(earlySource,earlyDestination,earlyTables);
            check("M16_FIRST_TABLE_VERIFIES_WITHOUT_ROTATING_EVERY_PENDING_TABLE",n(earlyDestination,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==1&&n(earlyDestination,"SELECT count(*) FROM r29_copy_manifest WHERE state='PENDING'")==30);
        }
        for(SQLiteDatabase db:new SQLiteDatabase[]{source,destination,changed,independent,corrupt,fullSource,full,stale,drift})db.jdbc.close();
        try(java.util.stream.Stream<Path> files=Files.walk(dir)){for(Path file:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(file);}
        System.out.println("R29_MIGRATION|ALL_PASS|checks="+checks+"|STAGED_BASELINE_ONLY_CUTOVER_PENDING");
    }
}
