package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Runs the production canonicalizer on real SQLite via the existing test adapter. */
public final class DrawIngestIdempotency {
    private static int checks;
    private static void check(boolean x){if(!x)throw new AssertionError("check="+checks);checks++;}
    private static long scalar(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){check(c.moveToFirst());return c.getLong(0);}}
    private static DrawCanonicalizerR811.PersistenceDecision observe(SQLiteDatabase db,Models.Draw d){
        db.beginTransaction();try{DrawCanonicalizerR811.PersistenceDecision r=DrawCanonicalizerR811.observeWithinWrite(db,d);db.setTransactionSuccessful();return r;}finally{db.endTransaction();}
    }
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try {
            db.execSQL("CREATE TABLE draws(issue TEXT UNIQUE,open_time INTEGER,fetch_time INTEGER,source_line TEXT,raw_json TEXT,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,dirty_reason TEXT)");
            db.execSQL("CREATE TABLE v57_draw_provenance_r811(provenance_id TEXT PRIMARY KEY,endpoint_id TEXT,request_at INTEGER,response_at INTEGER,result_code TEXT,raw_payload_sha256 TEXT,parsed_issue TEXT,permutation TEXT,server_open_time INTEGER,parser_version TEXT,source_line TEXT,draw_hash TEXT,observed_at INTEGER)");
            db.execSQL("CREATE TABLE v57_draw_canonical_r811(target_issue TEXT PRIMARY KEY,status TEXT,canonical_draw_hash TEXT,evidence_hash TEXT,source_count INTEGER,conflict_count INTEGER,updated_at INTEGER)");
            db.execSQL("CREATE TABLE revision(generation INTEGER PRIMARY KEY AUTOINCREMENT,source_rowid INTEGER UNIQUE)");
            db.execSQL("CREATE TRIGGER revision_insert AFTER INSERT ON v57_draw_canonical_r811 BEGIN INSERT OR REPLACE INTO revision(source_rowid) VALUES(NEW.rowid);END");
            db.execSQL("CREATE TRIGGER revision_update AFTER UPDATE ON v57_draw_canonical_r811 BEGIN INSERT OR REPLACE INTO revision(source_rowid) VALUES(NEW.rowid);END");
            db.execSQL("CREATE TABLE v57_settlement_task_r36(target_issue TEXT,status TEXT,updated_at INTEGER,completed_at INTEGER,lease_owner TEXT,lease_until INTEGER,next_attempt_at INTEGER,last_error TEXT,terminal_code TEXT)");
            db.execSQL("CREATE TABLE v57_oos_label_r35(target_issue TEXT,strict_oos INTEGER,error_code TEXT)");
            Models.Draw d=new Models.Draw();d.issue="202610150253";d.openTime=1792037583000L;d.fetchTime=1792037586412L;d.sourceLine="PRIMARY";d.rawJson="immutable-body";
            d.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};
            check(observe(db,d).confirmed);
            long row=scalar(db,"SELECT rowid FROM v57_draw_canonical_r811"),generation=scalar(db,"SELECT generation FROM revision"),writes=scalar(db,"SELECT total_changes()");
            for(int i=0;i<100;i++){d.fetchTime++;check(observe(db,d).confirmed);}
            check(scalar(db,"SELECT total_changes()")==writes);
            check(scalar(db,"SELECT rowid FROM v57_draw_canonical_r811")==row);
            check(scalar(db,"SELECT generation FROM revision")==generation);
            check(scalar(db,"SELECT COUNT(*) FROM revision")==1);
            d.sourceLine="BACKUP";check(observe(db,d).confirmed);
            check(scalar(db,"SELECT rowid FROM v57_draw_canonical_r811")==row);
            check(scalar(db,"SELECT source_count FROM v57_draw_canonical_r811")==2);
            check(scalar(db,"SELECT COUNT(*) FROM revision")==1);
            writes=scalar(db,"SELECT total_changes()");check(observe(db,d).confirmed);check(scalar(db,"SELECT total_changes()")==writes);
            // Exercise DbStore's actual existing-row path. Its constructor owns Android
            // application startup; this isolated branch uses only the passed real SQLite.
            for(String column:new String[]{"first_seen_at INTEGER","draw_hash_version INTEGER","draw_hash_v2 TEXT"})db.execSQL("ALTER TABLE draws ADD COLUMN "+column);
            db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,source_line,raw_json,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,dirty_reason,draw_hash_version,draw_hash_v2) VALUES(?,?,?,?,?,?,1,2,3,4,5,6,7,8,9,10,0,'',?,?)",
                    new Object[]{d.issue,d.openTime,d.fetchTime,d.fetchTime,d.sourceLine,d.rawJson,DrawHashR36.CURRENT_VERSION,DrawHashR36.sha256V2(d)});
            db.execSQL("CREATE TRIGGER forbid_duplicate_insert BEFORE INSERT ON draws BEGIN SELECT RAISE(ABORT,'DUPLICATE_INSERT_ATTEMPT');END");
            java.lang.reflect.Field unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
            DbStore store=(DbStore)((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(DbStore.class);
            java.lang.reflect.Method insert=DbStore.class.getDeclaredMethod("insertDrawInternal",SQLiteDatabase.class,Models.Draw.class);insert.setAccessible(true);
            writes=scalar(db,"SELECT total_changes()");
            for(int i=0;i<20;i++)check(((Long)insert.invoke(store,db,d))==-1L);
            check(scalar(db,"SELECT total_changes()")==writes);
            db.execSQL("UPDATE draws SET open_time=0 WHERE issue='202610150253'");
            check(((Long)insert.invoke(store,db,d))==-1L);
            check(scalar(db,"SELECT open_time FROM draws")==d.openTime);
            check(scalar(db,"SELECT first_seen_at FROM draws")==d.fetchTime);
            d.numbers=new int[]{2,1,3,4,5,6,7,8,9,10};
            DrawCanonicalizerR811.PersistenceDecision conflict=observe(db,d);
            check(conflict.conflict&&!conflict.confirmed);
            check(scalar(db,"SELECT conflict_count FROM v57_draw_canonical_r811")==1);
            check(scalar(db,"SELECT rowid FROM v57_draw_canonical_r811")==row);
            System.out.println("DRAW_INGEST_SQLITE_PASS|checks="+checks+"|duplicate_polls=100|store_duplicate_calls=20|canonical_writes_on_duplicates=0|duplicate_insert_attempts=0|rowid_preserved=1|metadata_repair=1|conflict_still_blocked=1");
        }finally{db.shutdown();}
    }
}
