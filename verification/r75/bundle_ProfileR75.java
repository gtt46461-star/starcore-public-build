package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/** Actual production open(), unmodified SQL, isolated SQLite. Measures host paths only. */
public final class bundle_ProfileR75 {
    static final class TimedDb extends SQLiteDatabase {
        final ArrayList<String> observations = new ArrayList<String>();
        String phase = "BOOT";
        TimedDb(String bridge, String file) throws Exception { super(bridge, file); }
        void record(String sql,long start) {
            observations.add(phase+"\t"+((System.nanoTime()-start)/1000000.0)+"\t"+sql.replace('\n',' '));
        }
        @Override public synchronized Cursor rawQuery(String sql,String[] args) {
            long start=System.nanoTime();try{return super.rawQuery(sql,args);}finally{record(sql,start);}
        }
        @Override public synchronized void execSQL(String sql) {
            long start=System.nanoTime();try{super.execSQL(sql);}finally{record(sql,start);}
        }
        @Override public synchronized void execSQL(String sql,Object[] args) {
            long start=System.nanoTime();try{super.execSQL(sql,args);}finally{record(sql,start);}
        }
    }
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    public static void main(String[] args)throws Exception {
        try(TimedDb db=new TimedDb(args[0],args[1])) {
            DbStore store=HostProductR974.store(db);
            RealtimePressureGateR811.resetForTests();
            // Point loader at an empty folder, selecting its actual installed seed snapshot.
            Field root=ModelBundleRuntimeR932.class.getDeclaredField("filesRoot");root.setAccessible(true);
            root.set(null,new File(args[2]));
            db.execSQL("DELETE FROM app_state WHERE state_key IN (?,?,?,?,?,?)",new Object[]{
                    "r932_bundle_capture_issue","r932_bundle_capture_hash",ModelBundleManagerR929.STATE_ACTIVE,
                    ModelBundleManagerR929.STATE_STAGED,ModelBundleManagerR929.STATE_PREVIOUS,"r932_bundle_reset_pending"});
            long revisions=n(db,"SELECT count(*) FROM research_row_revision_v2");
            ArrayList<String> phases=new ArrayList<String>();
            for(int i=0;i<3;i++) {
                db.phase=i==0?"COLD_REACTIVATE":i==1?"WARM_NEXT_ISSUE":"SAME_ISSUE";
                String target=i==0?"202610139001":"202610139002";
                long start=System.nanoTime();
                try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(store,target,System.currentTimeMillis())) {
                    if(!target.equals(scope.targetIssue))throw new AssertionError("target binding lost");
                }
                phases.add(db.phase+"="+((System.nanoTime()-start)/1000000.0));
            }
            if(n(db,"SELECT count(*) FROM research_model_bundle_r929 WHERE state='ACTIVE'")!=1)throw new AssertionError("active cardinality");
            if(n(db,"SELECT count(*) FROM research_certificate_r929")!=0)throw new AssertionError("unexpected fabricated certificates");
            Path out=Path.of(args[3]);Files.write(out,db.observations);
            System.out.println("REVISION_ROWS="+revisions);
            for(String line:phases)System.out.println(line);
            System.out.println("PROFILE="+out);
            System.out.println("PASS ACTUAL_PRODUCT_SEED_BUNDLE_HOST_SQL_TIMING_NOT_DEVICE");
        }
    }
}
