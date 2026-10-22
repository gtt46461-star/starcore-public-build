package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;

/** Actual reserved capture owner under a timely, expired, or crossing-deadline source clock. */
public final class CaptureDeadlineR77 {
    static int checks;
    static void ok(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
    static final class SlowDb extends SQLiteDatabase {
        boolean delay;
        SlowDb(String bridge,String path)throws Exception{super(bridge,path);}
        @Override public synchronized void execSQL(String sql,Object[] args){
            super.execSQL(sql,args);
            if(delay&&sql.startsWith("INSERT OR IGNORE INTO research_candidate_cal_observation_r932")){
                delay=false;try{Thread.sleep(5000L);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
            }
        }
    }
    public static void main(String[] a)throws Exception{
        String mode=a[2];
        try(SlowDb db=new SlowDb(a[0],a[1])){
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            MainlineCaptureR75.seed(db,"r77-capture-deadline",false);
            // Controlled fixture time: base draw is recent and the next declared draw
            // remains future. Old fixture's 1970 dates could not prove this boundary.
            long now=System.currentTimeMillis();
            long base="expired".equals(mode)?now-61000L:"crossing".equals(mode)?now-56000L:now-1000L;
            db.beginTransaction();try{for(int i=342;i<=441;i++)MainlineCaptureR75.draw(db,i,base-(441-i)*60000L);db.setTransactionSuccessful();}finally{db.endTransaction();}
            String target=MainlineCaptureR75.issue(442),bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash;
            DeferredEvidenceSnapshotR36.Snapshot snap=MainlineCaptureR75.snapshot(MainlineCaptureR75.issue(441),target,bundle);
            db.beginTransaction();try{
                db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,1,2,3,?,?)",new Object[]{target,bundle,snap.recordHash,selector,MainlineCaptureR75.h("r77-champion:"+target),System.currentTimeMillis()});
                GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,target,bundle,selector);db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            db.delay="crossing".equals(mode);
            int count=0;RuntimeException failure=null;
            try{count=FrozenCandidateAdapterR958.captureReservedCP12(store,snap);}catch(RuntimeException e){failure=e;}
            if("timely".equals(mode)){
                ok(failure==null&&count==1,"timely real capture commits");
                ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==1,"timely one CAL observation");
                ok(MainlineCaptureR75.n(db,"SELECT captured_n FROM "+CandidateCalibrationR932.STUDY)==1,"timely reservation advances one sample");
                ok(FrozenCandidateAdapterR958.captureReservedCP12(store,snap)==0,"replayed reservation cannot duplicate capture");
            }else{
                ok(failure!=null&&count==0,"expired or crossing owner fails truthfully");
                ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==0,"no late CAL row survives");
                ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW)==0,"no late frozen prediction survives");
                ok(MainlineCaptureR75.n(db,"SELECT captured_n FROM "+CandidateCalibrationR932.STUDY)==0,"no late reservation evidence increment");
                if("crossing".equals(mode))ok(!db.delay,"failure crosses deadline inside actual publication transaction");
            }
            ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM research_certificate_r929")==0,"test cannot manufacture certificate");
            Files.writeString(Path.of(a[3]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"mode\":\""+mode+"\",\"scope\":\"ACTUAL_RESERVED_CAPTURE_SQLITE_HOST_CONTROLLED_TIME\"}\n");
        }
    }
}
