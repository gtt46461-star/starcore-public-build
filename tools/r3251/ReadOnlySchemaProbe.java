package com.starcore.r3251probe;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;

/** Disposable-emulator observer: one read-only SQL snapshot, no app controls or data edits. */
public final class ReadOnlySchemaProbe {
    public static void main(String[] args) throws Exception {
        JSONObject result=new JSONObject();
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(args[0],null,SQLiteDatabase.OPEN_READONLY)) {
            try(Cursor row=db.rawQuery("SELECT "
                    +"(SELECT state FROM v57_schema_domain_r926 WHERE domain='CRITICAL'),"
                    +"(SELECT state FROM v57_schema_domain_r926 WHERE domain='RESEARCH'),"
                    +"(SELECT count(*) FROM v57_schema_migration_journal_r926 WHERE domain='RESEARCH' AND revision=(SELECT revision FROM v57_schema_domain_r926 WHERE domain='RESEARCH') AND step_id='R952_EXTENSION' AND status='COMPLETED'),"
                    +"(SELECT state_value FROM app_state WHERE state_key='su2_real_authorized_by_user'),"
                    +"(SELECT count(*) FROM v57_schema_migration_journal_r926 WHERE domain='RESEARCH' AND revision=(SELECT revision FROM v57_schema_domain_r926 WHERE domain='RESEARCH') AND status='COMPLETED')",null)) {
                if(!row.moveToFirst())throw new IllegalStateException("SCHEMA_OBSERVATION_EMPTY");
                boolean real="1".equals(row.getString(3));
                boolean ready="READY".equals(row.getString(0))&&"READY".equals(row.getString(1))&&row.getLong(2)>0;
                result.put("criticalState",row.getString(0)).put("researchState",row.getString(1))
                        .put("extensionRootCompleted",row.getLong(2)>0).put("realAuthorized",real)
                        .put("researchCompletedUnits",row.getLong(4))
                        .put("ready",ready).put("observer","ANDROID_SQLITE_OPEN_READONLY_SINGLE_SELECT");
                System.out.println(result.toString());System.exit(real?3:ready?0:2);
            }
        } catch(Exception missing) {
            result.put("ready",false).put("realAuthorized",JSONObject.NULL)
                    .put("observer","ANDROID_SQLITE_OPEN_READONLY_SINGLE_SELECT")
                    .put("pendingExceptionClass",missing.getClass().getSimpleName());
            System.out.println(result.toString());System.exit(2);
        }
    }
}
