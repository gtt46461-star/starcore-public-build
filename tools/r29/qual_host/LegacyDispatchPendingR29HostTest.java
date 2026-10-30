package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import java.util.*;

/** Real SQLite multi-page bootstrap through the production adapter boundary. The Android probe additionally
 * uses DatabaseCoordinator's actual F01 wrapper; HOST reproduces that wrapper around real capture/apply pages. */
public final class LegacyDispatchPendingR29HostTest {
    static int checks;static void check(String name,boolean good){System.out.println("R29_DISPATCH_PENDING|"+name+"|"+(good?"OK":"FAIL"));if(!good)throw new AssertionError(name);checks++;}
    static RuntimeException f01(Throwable cause){return new IllegalStateException("F01_WRITE_FAILED|operation=R26_TASK_CLAIM",cause);}
    static String s(SQLiteDatabase db,String sql){try(android.database.Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static ResearchPoolR27.Outcome wrappedClaim(SQLiteDatabase root,SQLiteDatabase rp)throws Exception{
        try{
            CriticalInputMirrorR29.Step page=CriticalInputMirrorR29.capture(root,rp);boolean[] ready={false};
            EventBridgeR29HostTest.tx(rp,()->ready[0]=CriticalInputMirrorR29.apply(rp,page),false);
            if(!ready[0])throw new IllegalStateException("CP3_DB_BUSY_R29_CRITICAL_INPUT_BOOTSTRAP_OR_CATCHUP");
            EventBridgeR29HostTest.tx(rp,()->rp.execSQL("UPDATE r26_task SET cursor=cursor+1,status='YIELDED' WHERE task_id='foundry-su1'"),false);
            return ResearchPoolR27.Outcome.progress(1,CriticalInputMirrorR29HostTest.n(rp,"SELECT cursor FROM r26_task WHERE task_id='foundry-su1'"));
        }catch(Exception cause){throw f01(cause);}
    }
    public static void main(String[]args)throws Exception{
        RuntimeException busy=f01(new IllegalStateException("CP3_DB_BUSY_R29_CRITICAL_INPUT_BOOTSTRAP_OR_CATCHUP"));
        check("P1_NESTED_F01_BUSY_CAUSE_IS_RECOGNIZED",ResearchWriteQueueR27.writerBusy(busy));
        check("P2_TRUE_SQL_ERROR_REMAINS_ERROR",!ResearchWriteQueueR27.writerBusy(f01(new SQLiteException("no such table: actual_missing_table"))));
        check("P3_SQL_TEXT_CONTAINING_BUSY_TOKEN_IS_NOT_BUSY",!ResearchWriteQueueR27.writerBusy(f01(new SQLiteException("no such table: CP3_DB_BUSY_fixture"))));
        check("P4_BUSY_WRAPPER_CANNOT_HIDE_SQL_FAILURE",!ResearchWriteQueueR27.writerBusy(new IllegalStateException("CP3_DB_BUSY_wrapper",new SQLiteException("malformed schema"))));
        check("P5_BUSY_WRAPPER_CANNOT_HIDE_CAPACITY_FAILURE",!ResearchWriteQueueR27.writerBusy(new IllegalStateException("CP3_DB_BUSY_wrapper",new IllegalStateException("R26_UNSLICED_COMPUTATION_CAPACITY"))));
        check("P6_UNRELATED_ILLEGAL_STATE_IS_NOT_SWALLOWED",!ResearchWriteQueueR27.writerBusy(f01(new IllegalStateException("R29_SCHEDULER_EPOCH_FENCE"))));
        check("P7_BUSY_TOKEN_MUST_BEGIN_ITS_CAUSE_MESSAGE",!ResearchWriteQueueR27.writerBusy(new IllegalStateException("query text mentions CP3_DB_BUSY_fixture")));
        IllegalStateException a=new IllegalStateException("outer"),b=new IllegalStateException("CP3_DB_BUSY_R29_STALE_MIRROR_CAPTURE");a.initCause(b);b.initCause(a);
        check("P8_CAUSE_CYCLE_IS_BOUNDED",ResearchWriteQueueR27.writerBusy(a));
        try(SQLiteDatabase root=CriticalInputMirrorR29HostTest.db();SQLiteDatabase rp=CriticalInputMirrorR29HostTest.db()){
            CriticalInputMirrorR29HostTest.setup(root);
            EventBridgeR29HostTest.tx(root,()->{for(int i=0;i<1300;i++)root.execSQL("INSERT INTO "+CriticalInputMirrorR29HostTest.SHADOW+" VALUES(?,1,0,-1,0)",new Object[]{String.format(Locale.ROOT,"pending-%06d",i)});},false);
            rp.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,cursor INTEGER,status TEXT,error_count INTEGER)");rp.execSQL("INSERT INTO r26_task VALUES('foundry-su1',0,'READY',2)");
            int pending=0;ResearchPoolR27.Outcome out=null;boolean allPendingZero=true,allPendingDependency=true,historyPreserved=true;
            for(int attempts=0;attempts<100;attempts++){
                out=LegacyCapabilityAdapterR29.dispatch(()->wrappedClaim(root,rp));if(out.units>0)break;pending++;
                allPendingZero&=out.units==0&&CriticalInputMirrorR29HostTest.n(rp,"SELECT cursor FROM r26_task")==0;
                allPendingDependency&=WaitKindR27.DEPENDENCY.equals(WaitKindR27.classify(out.units,out.reason,null,out.cursor,out.latest,null,false).kind);
                historyPreserved&=CriticalInputMirrorR29HostTest.n(rp,"SELECT error_count FROM r26_task")==2&&"READY".equals(s(rp,"SELECT status FROM r26_task"));
            }
            check("P9_MORE_THAN_QUARANTINE_WINDOW_REAL_PAGES_WAIT",pending>WaitKindR27.MAX_ERRORS_PER_WINDOW);
            check("P10_PENDING_CANNOT_ADVANCE_TASK_CURSOR",allPendingZero);
            check("P11_EACH_PENDING_IS_DEPENDENCY_NOT_ERROR",allPendingDependency);
            check("P12_EXISTING_ERROR_HISTORY_NEITHER_ADDED_NOR_CLEARED",historyPreserved&&CriticalInputMirrorR29HostTest.n(rp,"SELECT error_count FROM r26_task")==2);
            check("P13_ORIGINAL_TASK_RECOVERS_REAL_COMMITTED_PROGRESS",out!=null&&out.units==1&&out.cursor==1&&"READY".equals(CriticalInputMirrorR29.state(rp).phase));
            check("P14_BOOTSTRAP_RETAINS_ALL_REAL_INPUT_ROWS",CriticalInputMirrorR29HostTest.n(rp,"SELECT count(*) FROM "+CriticalInputMirrorR29HostTest.SHADOW)==1430&&CriticalInputMirrorR29HostTest.n(root,"SELECT count(*) FROM "+CriticalInputMirrorR29HostTest.SHADOW)==1430);
            RuntimeException sql=f01(new SQLiteException("no such table: real_error"));boolean exact=false;try{LegacyCapabilityAdapterR29.dispatch(()->{throw sql;});}catch(RuntimeException actual){exact=actual==sql;}
            check("P15_REAL_SQL_EXCEPTION_ESCAPES_ADAPTER_UNCHANGED",exact);
            RuntimeException capacity=f01(new IllegalStateException("R26_UNSLICED_COMPUTATION_CAPACITY"));exact=false;try{LegacyCapabilityAdapterR29.dispatch(()->{throw capacity;});}catch(RuntimeException actual){exact=actual==capacity;}
            check("P16_REAL_CAPACITY_EXCEPTION_ESCAPES_ADAPTER_UNCHANGED",exact);
            System.out.println("R29_DISPATCH_PENDING|REAL_BOOTSTRAP_PENDING_TURNS="+pending);
        }
        System.out.println("R29_DISPATCH_PENDING|ALL_PASS|checks="+checks+"|HOST_SQL_ONLY_ANDROID_F01_WRAPPER_PENDING");
    }
}
