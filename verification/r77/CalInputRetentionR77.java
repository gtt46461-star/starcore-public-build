package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;

/** Installed-schema input ownership fixture, with original CAL evidence preserved. */
public final class CalInputRetentionR77 {
    static int checks;
    static String h(String x){return V51ChecksumEngine.sha256(x);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static void ok(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
    static String issue(int i){return "202610130"+i;}
    static void seed(SQLiteDatabase db,int i,String state,int hit){
        Models.Draw draw=new Models.Draw();draw.issue=issue(i);draw.openTime=1791888663000L+i;draw.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};
        db.execSQL("INSERT INTO draws(_id,issue,open_time,fetch_time,source_line,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,dirty_reason,first_seen_at,draw_hash_version,draw_hash_v2) VALUES(?,?,?,?,?,1,2,3,4,5,6,7,8,9,10,0,'',?,2,?)",
                new Object[]{i,draw.issue,draw.openTime,draw.openTime+1,"fixture",draw.openTime+1,DrawHashR36.sha256V2(draw)});
        String study=h("study"+i),key="algorithm:retention"+i;
        // Insert source observations before setting FROZEN to respect immutable
        // observation triggers; this fixture never creates a fitted model/certificate.
        db.execSQL("INSERT INTO "+CandidateCalibrationR932.STUDY+" VALUES(?,?,1,1,'COLLECTING',?)",new Object[]{study,key,draw.issue});
        db.execSQL("INSERT INTO "+CandidateCalibrationR932.OBS+" VALUES(?,?,?,?,?,?,.35,1,1,2,3,?,?,?)",new Object[]{study,draw.issue,key,h("s"),h("b"),h("sel"),h("pred"+i),draw.openTime-1,hit});
        db.execSQL("UPDATE "+CandidateCalibrationR932.STUDY+" SET state=? WHERE study_hash=?",new Object[]{state,study});
        DrawDependencyInputCP12.preserveIfReferencedWithinWrite(db,i,draw.issue);
    }
    public static void main(String[] a)throws Exception{
        try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
            db.beginTransaction();try{
                seed(db,601,"COLLECTING",0);seed(db,602,"READY",1);
                seed(db,603,"READY",-2);seed(db,604,"BLOCKED_INVALID_INPUT",1);seed(db,605,"FROZEN",1);
                ok(n(db,"SELECT COUNT(*) FROM "+DrawDependencyInputCP12.TABLE)==2,"only valid labels with unfinished fits retain draw input");
                ok(n(db,"SELECT COUNT(*) FROM "+DrawDependencyInputCP12.TABLE+" WHERE issue IN('"+issue(601)+"','"+issue(602)+"')")==2,"COLLECTING and READY own their settled source clocks");
                db.execSQL("DELETE FROM draws");
                ok(DrawDependencyInputCP12.verifiedOpenTimeR77(db,issue(601))==1791888663601L,"actual preservation authenticates copied source after hot eviction");
                ok(DrawDependencyInputCP12.releaseConsumedWithinWrite(db,32)==0,"unfinished fit input cannot be released");
                db.execSQL("UPDATE "+CandidateCalibrationR932.STUDY+" SET state='FROZEN' WHERE study_hash=?",new Object[]{h("study601")});
                ok(DrawDependencyInputCP12.releaseConsumedWithinWrite(db,32)==1,"FROZEN releases this calibration input owner");
                db.execSQL("UPDATE "+CandidateCalibrationR932.STUDY+" SET state='BLOCKED_INVALID_INPUT' WHERE study_hash=?",new Object[]{h("study602")});
                ok(DrawDependencyInputCP12.releaseConsumedWithinWrite(db,32)==1,"BLOCKED releases this calibration input owner");
                ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==5,"source cleanup never edits original calibration observations");
                ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS+" WHERE hit=-2")==1,"terminal invalid remains distinct from binary labels");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_RETENTION_OWNER_INSTALLED_SCHEMA_HOST_FIXTURE\"}\n");
        }
    }
}
