package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.strategyv2.ResearchRetentionStoreR30;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
/** Real Android SQLite mechanics on isolated memory DB; no natural quality or handset duration claim. */
@RunWith(AndroidJUnit4.class)
public final class R30ResearchRetentionAndroidTest {
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){assertTrue(c.moveToFirst());return c.getLong(0);}}
    @Test public void withoutRowidEvidenceArchivesExactlyAndRollbackKeepsOriginal(){
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            db.execSQL("CREATE TABLE r30_confirm_read(task_key TEXT PRIMARY KEY,original_state BLOB) WITHOUT ROWID");
            db.beginTransaction();try{ResearchRetentionStoreR30.ensureProtected(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            byte[] original={0,1,2,-1,0,9};db.execSQL("INSERT INTO r30_confirm_read VALUES('isolated-synthetic',?)",new Object[]{original});
            db.beginTransaction();try{db.execSQL("DELETE FROM r30_confirm_read");assertEquals(1,n(db,"SELECT count(*) FROM r30_keep_r30_confirm_read"));}finally{db.endTransaction();}
            assertEquals(1,n(db,"SELECT count(*) FROM r30_confirm_read"));assertEquals(0,n(db,"SELECT count(*) FROM r30_keep_r30_confirm_read"));
            db.beginTransaction();try{db.execSQL("DELETE FROM r30_confirm_read");db.setTransactionSuccessful();}finally{db.endTransaction();}
            try(Cursor c=db.rawQuery("SELECT task_key,original_state FROM r30_keep_r30_confirm_read",null)){assertTrue(c.moveToFirst());assertEquals("isolated-synthetic",c.getString(0));assertArrayEquals(original,c.getBlob(1));}
            try{db.execSQL("DELETE FROM r30_keep_r30_confirm_read");fail("protected evidence deleted");}catch(android.database.sqlite.SQLiteException expected){}
            System.out.println("R30_ANDROID_RETENTION|WITHOUT_ROWID_ORIGINAL_KEY_AND_BLOB|SAME_TX_ROLLBACK|IMMUTABLE_ARCHIVE|ANDROID_TEST_NOT_HANDSET_ACCEPTANCE|PASS");
        }
    }
}
