package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Actual revision-index owner, real SQLite replacement orphans; no evidence deletion. */
public final class RevisionSweepR79 {
    static int checks;
    static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    static long n(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){ok(c.moveToFirst(),q);return c.getLong(0);}}
    static String s(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){ok(c.moveToFirst(),q);return c.getString(0);}}
    static int cursor(){for(int i=0;i<RetentionTableRegistryCP12.ALL_INSTALLED.size();i++)
        if(RetentionTableRegistryCP12.ALL_INSTALLED.get(i).table.equals("diagnostic_log"))return i;
        throw new AssertionError("diagnostic table missing from registry");}
    static void point(SQLiteDatabase d){d.execSQL("INSERT OR REPLACE INTO app_state VALUES('retention_cp12_revision_cursor',?,1)",new Object[]{Integer.toString(cursor())});}
    static final class PreemptDb extends SQLiteDatabase {
        boolean armed;int injections;
        PreemptDb(String bridge,String file)throws Exception{super(bridge,file);}
        @Override public synchronized Cursor rawQuery(String q,String[] a){
            Cursor result=super.rawQuery(q,a);
            if(armed&&q.startsWith("SELECT r.source_rowid,r.generation,EXISTS")){
                armed=false;injections++;RealtimePressureGateR811.beginAnalysis();
            }
            return result;
        }
    }
    public static void main(String[] args)throws Exception {
        try(PreemptDb d=new PreemptDb(args[0],args[1])){
            RealtimePressureGateR811.resetForTests();point(d);
            long before=n(d,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'"),
                    live=n(d,"SELECT COUNT(*) FROM diagnostic_log"),generation=n(d,"SELECT MAX(generation) FROM research_row_revision_v2");
            if("baseline".equals(args[2])){
                d.beginTransaction();int removed;
                try{removed=DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(d,TimeSourceR811.monoNowMs()+2000);d.setTransactionSuccessful();}finally{d.endTransaction();}
                ok(removed==128,"baseline limits work to128 even with spare quiet budget");
                System.out.println("R78_REVISION_SERVICE_CAP_REPRODUCED checks="+checks+" removed="+removed);return;
            }
            // Rollback must retain both the orphan index and exact source cursor.
            String initial=s(d,"SELECT state_value FROM app_state WHERE state_key='retention_cp12_revision_cursor'");
            d.beginTransaction();int provisional;
            try{provisional=DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(d,TimeSourceR811.monoNowMs()+2000);}finally{d.endTransaction();}
            ok(provisional==2048,"usable quiet turn services16 old pages with64-row statements");
            ok(n(d,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'")==before,"rolled back sweep deletes nothing");
            ok(initial.equals(s(d,"SELECT state_value FROM app_state WHERE state_key='retention_cp12_revision_cursor'")),"rollback preserves table cursor");
            d.armed=true;d.beginTransaction();int interrupted;
            try{interrupted=DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(d,TimeSourceR811.monoNowMs()+2000);d.setTransactionSuccessful();}finally{d.endTransaction();RealtimePressureGateR811.endAnalysis();}
            ok(d.injections==1&&interrupted==64,"mid-page P0 preemption stops after one bounded page");
            ok(n(d,"SELECT COUNT(*) FROM diagnostic_log")==live,"critical preemption never deletes source evidence");
            point(d);d.beginTransaction();int resumed;
            try{resumed=DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(d,TimeSourceR811.monoNowMs()+2000);d.setTransactionSuccessful();}finally{d.endTransaction();}
            ok(resumed==2048,"cursor resumes after committed preemption");
            ok(n(d,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'")==before-64-2048,"all deletion is accounted for obsolete generations");
            ok(n(d,"SELECT COUNT(*) FROM diagnostic_log")==live,"source evidence remains exact");
            ok(n(d,"SELECT COUNT(*) FROM diagnostic_log l WHERE NOT EXISTS(SELECT 1 FROM research_row_revision_v2 r WHERE r.source_table='diagnostic_log' AND r.source_rowid=l.rowid)")==0,"every live generation survives");
            point(d);d.beginTransaction();try{ok(DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(d,TimeSourceR811.monoNowMs()-1)==0,"expired slice does no source/index work");d.setTransactionSuccessful();}finally{d.endTransaction();}
            d.execSQL("INSERT INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES('R79-NEW',1,'INFO','HOST','SYNTHETIC','new generation','test')");
            ok(n(d,"SELECT MAX(generation) FROM research_row_revision_v2")>generation,"deleted generations are never reused");
            ok(n(d,"SELECT COUNT(*) FROM trades")==0&&n(d,"SELECT COUNT(*) FROM research_certificate_r929")==0,"housekeeping cannot authorize REAL");
            System.out.println("R79_REVISION_SWEEP_PASS checks="+checks+" obsolete_removed="+(interrupted+resumed)+" source_rows_unchanged="+live+" max_statement_rows=64 scope=HOST_SQLITE_NOT_PHONE_RECLAIM");
        }
    }
}
