package com.ethk3.novaonepro;
import android.content.ContentValues;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;
/** Actual handoff/reconciler with exact clocks and projection-only SQLite faults. */
public final class RequestHandoffR79 {
    static int checks;
    static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){ok(c.moveToFirst(),q);return c.getLong(0);}}
    static String s(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){ok(c.moveToFirst(),q);return c.getString(0);}}
    static final class FaultDb extends SQLiteDatabase {
        int transaction,projectionWrites,projectionFailures;boolean armed;Set<Integer> transactions=new HashSet<>();
        FaultDb(String bridge,String file)throws Exception{super(bridge,file);}
        @Override public synchronized void beginTransaction(){super.beginTransaction();transaction++;}
        @Override public synchronized long insertWithOnConflict(String table,String nullColumn,ContentValues cv,int conflict){
            String key=cv.getAsString("state_key");
            if("app_state".equals(table)&&key!=null&&(key.startsWith("r906_business_current_")||key.equals("r906_supervisor_pulse_requested"))){
                projectionWrites++;transactions.add(transaction);
                if(armed&&projectionWrites==2){armed=false;projectionFailures++;throw new IllegalStateException("SYNTHETIC_SECOND_PROJECTION_WRITE_FAULT");}
            }
            return super.insertWithOnConflict(table,nullColumn,cv,conflict);
        }
        void reset(boolean fail){projectionWrites=0;transactions.clear();armed=fail;}
    }
    static Models.Draw draw(String issue,long opened){Models.Draw d=new Models.Draw();d.issue=issue;d.openTime=opened;d.fetchTime=opened+1;d.sourceLine="SYNTHETIC_HOST";for(int i=0;i<10;i++)d.numbers[i]=i+1;return d;}
    static void seed(SQLiteDatabase db,Models.Draw d){
        db.beginTransaction();try{
            db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,first_seen_at,dirty,draw_hash_version,draw_hash_v2) VALUES(?,?,?,1,2,3,4,5,6,7,8,9,10,?,0,?,?)",
                    new Object[]{d.issue,d.openTime,d.fetchTime,d.fetchTime,DrawHashR36.CURRENT_VERSION,DrawHashR36.sha256V2(d)});
            ok(DrawCanonicalizerR811.observeWithinWrite(db,d).confirmed,"confirmed synthetic source");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public static void main(String[] args)throws Exception{
        try(FaultDb db=new FaultDb(args[0],args[1])){
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            Models.Draw d=draw("202610150500",System.currentTimeMillis()-1000);seed(db,d);
            long deadline=TargetIssueResolver.predrawDeadlineWallR20261008(d);
            ok(deadline>System.currentTimeMillis(),"fixture has a real usable pre-draw window");
            SettlementCoordinatorR815.queueCurrentExactR914(store,d,SettlementCoordinatorR815.Reason.RECONCILER);
            if("baseline".equals(args[2])){
                ok(n(db,"SELECT COUNT(*) FROM v57_analysis_request_r36")==0,"R78 handoff fails before durable analysis admission");
                ok(s(db,"SELECT state_value FROM app_state WHERE state_key='r947_request_first_status'").contains("F01_REQUEST_DEADLINE_MISMATCH"),"original deadline contract failure reproduced");
                System.out.println("R78_HANDOFF_DEADLINE_BUG_REPRODUCED checks="+checks);return;
            }
            ok(n(db,"SELECT COUNT(*) FROM v57_analysis_request_r36 WHERE target_issue='202610150501'")==1,"public handoff persists next legal request immediately");
            ok(n(db,"SELECT deadline_at FROM v57_analysis_request_r36")==deadline,"stored deadline exactly matches draw-derived deadline");
            String hash=s(db,"SELECT request_hash FROM v57_analysis_request_r36");long generation=n(db,"SELECT generation FROM v57_analysis_request_r36");
            for(int i=0;i<3;i++)SettlementCoordinatorR815.queueCurrentExactR914(store,d,SettlementCoordinatorR815.Reason.RECONCILER);
            ok(n(db,"SELECT COUNT(*) FROM v57_analysis_request_r36")==1&&n(db,"SELECT deadline_at FROM v57_analysis_request_r36")==deadline,"repeated handoff neither duplicates nor renews deadline");
            ok(hash.equals(s(db,"SELECT request_hash FROM v57_analysis_request_r36"))&&generation==n(db,"SELECT generation FROM v57_analysis_request_r36"),"same immutable request identity retained");
            db.reset(false);new BusinessReconcilerR36().reconcile(store,d,"SYNTHETIC_R79_PROJECTION",null);
            ok(db.projectionWrites==5&&db.transactions.size()==1,"five reconciler keys share one existing atomic writer transaction");
            ok(n(db,"SELECT COUNT(*) FROM app_state WHERE state_key LIKE 'r906_business_current_%' OR state_key='r906_supervisor_pulse_requested'")==5,"complete projection published");
            db.execSQL("DELETE FROM app_state WHERE state_key LIKE 'r906_business_current_%' OR state_key='r906_supervisor_pulse_requested'");
            db.reset(true);new BusinessReconcilerR36().reconcile(store,d,"SYNTHETIC_R79_PROJECTION_FAULT",null);
            ok(db.projectionFailures==1&&db.projectionWrites==2,"fault hits second real projection write");
            ok(n(db,"SELECT COUNT(*) FROM app_state WHERE state_key LIKE 'r906_business_current_%' OR state_key='r906_supervisor_pulse_requested'")==0,"optional projection rolls back every partial key");
            ok(n(db,"SELECT COUNT(*) FROM v57_analysis_request_r36")==1&&hash.equals(s(db,"SELECT request_hash FROM v57_analysis_request_r36")),"projection failure preserves durable analysis request");
            ok(n(db,"SELECT COUNT(*) FROM v57_settlement_task_r36 WHERE target_issue='202610150500'")==1,"exact settlement handoff retained");
            Models.Draw expired=draw("202610150502",System.currentTimeMillis()-120000);seed(db,expired);
            SettlementCoordinatorR815.queueCurrentExactR914(store,expired,SettlementCoordinatorR815.Reason.RECONCILER);
            ok(n(db,"SELECT COUNT(*) FROM v57_analysis_request_r36 WHERE target_issue='202610150503'")==0,"expired target never gets a renewed window");
            ok(n(db,"SELECT deadline_at FROM v57_analysis_request_r36 WHERE target_issue='202610150501'")==deadline,"newer draw cannot mutate original deadline");
            ok(n(db,"SELECT COUNT(*) FROM trades")==0,"handoff creates no REAL");
            System.out.println("R79_REQUEST_HANDOFF_PASS checks="+checks+"|exact_deadline=1|duplicate=0|projection_transactions=1|fault_rollback=1|scope=SYNTHETIC_PRODUCT_HOST");
        }
    }
}
