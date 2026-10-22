package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;

/** Observe actual SQL, reject only the useless full-capacity admission scan. */
public final class MainlineCapacityR75 {
    static final class ObservedDb extends SQLiteDatabase {
        boolean full;int queries;
        ObservedDb(String bridge,String file)throws Exception{super(bridge,file);}
        @Override public synchronized Cursor rawQuery(String sql,String[] args){
            if(full){queries++;if(sql.contains("SELECT f.candidate_id,f.structural_hash,f.created_issue,f.created_at"))throw new AssertionError("FULL_CAPTURE_CAPACITY_STILL_SCANS_WAITING_CANDIDATES");}
            return super.rawQuery(sql,args);
        }
    }
    public static void main(String[] a)throws Exception{
        try(ObservedDb db=new ObservedDb(a[0],a[1])){
            db.beginTransaction();try{
                db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,1,2,3,?,1)",new Object[]{CaptureAdmissionR74.TARGET,CaptureAdmissionR74.BUNDLE,CaptureAdmissionR74.h("snapshot"),CaptureAdmissionR74.SELECTOR,CaptureAdmissionR74.h("champion")});
                for(int i=1;i<=40;i++)CaptureAdmissionR74.candidate(db,i,false,TrainingCodeIdentityR964.SHA256);
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            if(CaptureAdmissionR74.reserve(db,true)!=8)throw new AssertionError("initial capacity");
            String scan=CaptureAdmissionR74.scan(db);
            db.full=true;
            if(CaptureAdmissionR74.reserve(db,true)!=0)throw new AssertionError("full capacity admitted new candidate");
            if(!scan.equals(CaptureAdmissionR74.scan(db)))throw new AssertionError("full capacity advanced scan cursor");
            if(CaptureAdmissionR74.n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE state IN('RESERVED','ACTIVE')")!=8)throw new AssertionError("active reservations changed");
            if(CaptureAdmissionR74.n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.STUDY)!=8)throw new AssertionError("waiting studies created");
            System.out.println("PASS full capacity serves existing captures, zero waiting scan; bounded queries="+db.queries);
            Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"full_capacity_queries\":"+db.queries+",\"scope\":\"ACTUAL_RESERVATION_SQL_FULL_INSTALLED_SCHEMA\"}\n");
        }
    }
}
