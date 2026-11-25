package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.FileOutputStream;

public final class R342RetentionInterruption {
    private R342RetentionInterruption() {}

    public static void observe(Context context) throws Exception {
        File root = new File(context.getCacheDir(), "r342-retention-interruption");
        File existing = new File(root, "code_cache/preserved");
        assertTrue(existing.getParentFile().mkdirs() || existing.getParentFile().isDirectory());
        try (FileOutputStream bytes = new FileOutputStream(existing)) {
            bytes.write(new byte[]{1, 2, 3});
        }
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL(ResearchRetentionStoreR30.FILE_DDL);
            db.execSQL("INSERT INTO r30_retention_file_action(action_id,relative_path,kind,bytes,modified_at,"
                    +"copy_reference,state,recorded_at) VALUES "
                    +"('absent','code_cache/already-unlinked','RECOMPUTABLE_CACHE',3,0,'cache','DELETE_PENDING',1),"
                    +"('present','code_cache/preserved','RECOMPUTABLE_CACHE',3,0,'cache','DELETE_PENDING',1),"
                    +"('unsafe','../code_cache/preserved','RECOMPUTABLE_CACHE',3,0,'cache','DELETE_PENDING',1)");
            ResearchRetentionR30.Result observation = ResearchRetentionStoreR30.cleanFiles(db,root,100,
                    (operation,batch) -> ResearchWriteQueueR27.exec(db,batch));
            assertEquals(0L, observation.json().get("unlinkedLogicalFileBytes"));
            assertNull(observation.json().get("observedUsableSpaceDelta"));
            assertTrue(existing.isFile());
            try (Cursor states = db.rawQuery("SELECT state FROM r30_retention_file_action ORDER BY action_id",null)) {
                assertTrue(states.moveToNext());
                assertEquals("PREAUTHORIZED_FILE_ABSENT_PHYSICAL_DELTA_UNKNOWN",states.getString(0));
                assertTrue(states.moveToNext());
                assertEquals("RECOVERY_PRESERVED_FOR_RECHECK",states.getString(0));
                assertTrue(states.moveToNext());
                assertEquals("RECOVERY_PATH_UNVERIFIED",states.getString(0));
                assertFalse(states.moveToNext());
            }
        }
    }
}
