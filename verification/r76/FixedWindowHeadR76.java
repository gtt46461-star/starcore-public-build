package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Method;
import java.nio.file.*;

/** Synthetic frozen-label inputs; actual fixed-window maintenance and SQLite owners.
 * No test labels, counts, models or certificates are transferred into a user database. */
public final class FixedWindowHeadR76 {
    static int checks;
    static void ok(boolean b,String what){if(!b)throw new AssertionError(what);checks++;System.out.println("PASS "+what);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static String sql()throws Exception{Method m=FixedResearchWindowR932.class.getDeclaredMethod("pendingIssueSql");m.setAccessible(true);return (String)m.invoke(null);}
    public static void main(String[] a)throws Exception {
        if(a[0].equals("sql")){Files.writeString(Path.of(a[1]),sql()+" ORDER BY target_issue LIMIT 1\n");return;}
        try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            ok(n(db,"SELECT COUNT(*) FROM draws")==5000,"fixture contains a full 5000-row hot history");
            ok("202609190003".equals(text(db,sql()+" ORDER BY target_issue LIMIT 1")),"oldest retained-only valid input is eligible before hot input");
            ok(FixedResearchWindowR932.hasRunnableWork(db),"actual maintenance readiness sees pending valid labels");
            String result="";
            for(int i=0;i<6&&n(db,"SELECT SUM(settled_n) FROM "+CandidateCalibrationR932.STUDY)<3;i++){
                ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(1000);
                try{result=FixedResearchWindowR932.maintainReviewed(store);}finally{ResearchWorkBudgetR927.leave(scope);}
            }
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS+" WHERE target_issue IN('202609190003','202609190004','202609200001') AND hit=1")==3,"public maintenance settles retained-only and valid hot exact outcomes");
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS+" WHERE target_issue IN('202609190001','202609190002','202609250001') AND hit=-1")==3,"missing dirty-shadowed and future draws remain unsettled");
            ok(n(db,"SELECT SUM(settled_n) FROM "+CandidateCalibrationR932.STUDY)==3,"only actual valid outcomes increment settlement counters");
            ok("FIXED_WINDOWS_REVIEWED".equals(result)&&!FixedResearchWindowR932.hasRunnableWork(db),"invalid source heads do not keep the worker permanently runnable");
            FixedResearchWindowR932.maintainReviewed(store);FixedResearchWindowR932.maintainReviewed(store);
            ok(n(db,"SELECT SUM(settled_n) FROM "+CandidateCalibrationR932.STUDY)==3,"owner replay does not double-settle counters");
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)==0&&n(db,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_TRADES)==0,"head optimization creates no fitted model certificate or REAL");
            Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_PUBLIC_MAINTENANCE_SYNTHETIC_FROZEN_INPUTS_NOT_PHONE_OR_QUALITY\"}\n");
        }
    }
}
