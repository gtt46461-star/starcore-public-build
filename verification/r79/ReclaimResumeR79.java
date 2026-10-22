package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONObject;
/** Actual product quiet owner + real SQLite transport. Separate invocations prove restart recovery. */
public final class ReclaimResumeR79 {
    static int checks;
    static void ok(boolean value,String why){if(!value)throw new AssertionError(why);checks++;System.out.println("PASS "+why);}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    static String value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getString(0);}}
    public static void main(String[] a)throws Exception {
        String mode=a[0];long started=System.nanoTime();int turns=0;long initial,remaining,bytesBefore,bytesAfter;
        try(SQLiteDatabase db=new SQLiteDatabase(a[1],a[2])){
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            ok(scalar(db,"PRAGMA auto_vacuum")==2,"incremental auto-vacuum fixture");
            initial=scalar(db,"PRAGMA freelist_count");bytesBefore=new File(a[2]).length();
            String sentinel=value(db,"SELECT hex(payload) FROM r79_preserved_sentinel WHERE id=1");
            if(mode.equals("initial")){
                ok(initial>256,"real free-page backlog exceeds two slices");
                StorageReclaimR943.quiet(store);turns++;
                remaining=scalar(db,"PRAGMA freelist_count");
                ok(remaining>0&&remaining<initial,"first bounded owner slice frees pages and leaves backlog");
                ok(store.getState("r943_storage_reclaim","").startsWith("RECLAIM_PROGRESS|"),"partial slice explicitly reports progress");
                ok(store.getState("r943_storage_reclaim_at","").isEmpty(),"partial slice does not arm two-minute completed cooldown");
                RealtimePressureGateR811.beginAnalysis();try{StorageReclaimR943.quiet(store);}finally{RealtimePressureGateR811.endAnalysis();}
                ok(scalar(db,"PRAGMA freelist_count")==remaining,"active P0 analysis prevents reclamation");
                ok(store.getState("r943_storage_reclaim_at","").isEmpty(),"P0 yield does not arm completed cooldown");
            }else if(mode.equals("resume")){
                ok(initial>0,"restarted process observes unfinished page backlog");
                ok(store.getState("r943_storage_reclaim_at","").isEmpty(),"restart retains immediate resume eligibility");
                long before=initial;
                do{
                    StorageReclaimR943.quiet(store);turns++;
                    remaining=scalar(db,"PRAGMA freelist_count");
                    ok(remaining<before,"each quiet turn advances actual freelist");before=remaining;
                    if(turns>20000)throw new AssertionError("bounded recovery failed to reach completion");
                }while(remaining>0);
                ok(store.getState("r943_storage_reclaim","").startsWith("RECLAIM_COMPLETE|"),"completion only after actual reusable backlog empty");
                long completed=Long.parseLong(store.getState("r943_storage_reclaim_at","0"));
                ok(completed>0,"completed pass arms normal idle cooldown");
                String state=store.getState("r943_storage_reclaim","");StorageReclaimR943.quiet(store);
                ok(state.equals(store.getState("r943_storage_reclaim","")),"completed cooldown prevents unnecessary repeat work");
                ok(new File(a[2]).length()<bytesBefore,"actual database file shrinks across resumed slices");
            }else if(mode.equals("reader")){
                ok(initial==0,"reader-blocked test starts without old free pages");
                db.execSQL("DELETE FROM app_state WHERE state_key IN('r943_storage_reclaim_at','r943_storage_reclaim')");
                db.rawQuery("PRAGMA journal_mode=WAL",null).close();db.rawQuery("PRAGMA wal_autocheckpoint=0",null).close();
                db.execSQL("CREATE TABLE r79_reader_payload(id INTEGER PRIMARY KEY,payload BLOB)");
                db.execSQL("INSERT INTO r79_reader_payload VALUES(1,zeroblob(1048576))");
                db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null).close();
                try(SQLiteDatabase reader=new SQLiteDatabase(a[1],a[2])){
                    reader.execSQL("BEGIN");ok(scalar(reader,"SELECT length(payload) FROM r79_reader_payload WHERE id=1")==1048576,"independent reader pins original WAL snapshot");
                    db.execSQL("DELETE FROM r79_reader_payload");
                    do{StorageReclaimR943.quiet(store);turns++;remaining=scalar(db,"PRAGMA freelist_count");if(turns>100)throw new AssertionError("freelist did not drain");}while(remaining>0);
                    ok(store.getState("r943_storage_reclaim","").startsWith("WAL_PENDING_RETRY|"),"busy=0 incomplete PASSIVE checkpoint remains pending");
                    ok(store.getState("r943_storage_reclaim_at","").isEmpty(),"reader-held checkpoint never arms completion cooldown");
                    ok(scalar(reader,"SELECT length(payload) FROM r79_reader_payload WHERE id=1")==1048576,"maintenance preserves pinned reader view");reader.execSQL("ROLLBACK");
                }
                long heldBytes=new File(a[2]).length();StorageReclaimR943.quiet(store);turns++;
                ok(store.getState("r943_storage_reclaim","").startsWith("RECLAIM_COMPLETE|"),"next quiet turn completes after reader releases");
                ok(new File(a[2]).length()<heldBytes,"released checkpoint causes actual file truncation");
            }else throw new IllegalArgumentException(mode);
            ok(sentinel.equals(value(db,"SELECT hex(payload) FROM r79_preserved_sentinel WHERE id=1")),"all retained sentinel bytes preserved");
            ok("ok".equals(value(db,"PRAGMA quick_check")),"real SQLite integrity after maintenance");
            remaining=scalar(db,"PRAGMA freelist_count");bytesAfter=new File(a[2]).length();
            JSONObject result=new JSONObject().put("status","PASS").put("mode",mode).put("checks",checks).put("turns",turns).put("freePagesBefore",initial).put("freePagesAfter",remaining).put("bytesBefore",bytesBefore).put("bytesAfter",bytesAfter).put("elapsedMs",(System.nanoTime()-started)/1000000L).put("scope","ACTUAL_PRODUCT_OWNER_REAL_SQLITE_HOST_SYNTHETIC_FIXTURE_NOT_PHONE");
            Files.writeString(Path.of(a[3]),result.toString()+"\n");System.out.println(result.toString());
        }
    }
}
