package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import java.util.Arrays;

public final class SlowStateHarnessR932 {
    static void check(boolean ok,String code){if(!ok)throw new AssertionError(code);System.out.println(code+"=PASS");}
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try {
            DbStore store=new DbStore(db);double[] x=new double[1049],initial=new double[1049];Arrays.fill(x,1);
            String model=V51ChecksumEngine.sha256("D2_SYNTHETIC"),base="202609100001",first=TargetIssueResolver.next(base).targetIssue,next=TargetIssueResolver.next(first).targetIssue;
            double[] memory=SlowStateR932.capture(store,model,first,x,initial,base);check(memory[0]==0,"D2_FIRST_ISSUE_READS_PREVIOUS_MEMORY");
            check(SlowStateR932.capture(store,model,first,x,initial,base)[0]==0,"D2_RETRY_DOES_NOT_ADVANCE_MEMORY_TWICE");
            check(SlowStateR932.capture(new DbStore(db),model,next,x,initial,base)[0]==.05,"D2_RESTART_LOADS_PREVIOUS_CAPTURE");
            x[0]=2;boolean rejected=false;try{SlowStateR932.capture(store,model,next,x,initial,base);}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"D2_SAME_ISSUE_CHANGED_INPUT_REJECTED");x[0]=1;
            String after=TargetIssueResolver.next(next).targetIssue;rejected=false;try{SlowStateR932.capture(store,model,TargetIssueResolver.next(after).targetIssue,x,initial,base);}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"D2_GAP_REQUIRES_PAST_FEATURE_REPLAY");
            check(Math.abs(SlowStateR932.capture(store,model,after,x,initial,base)[0]-.0975)<1e-15,"D2_FAILED_CAPTURE_PRESERVES_CURSOR");
            check(SlowStateR932.capture(store,V51ChecksumEngine.sha256("OTHER_MODEL"),first,x,initial,base)[0]==0,"D2_MODEL_GENERATIONS_HAVE_SEPARATE_MEMORY");
            check(SlowStateR932.frozenPast(store,model,first,x)[0]==0&&SlowStateR932.frozenPast(store,model,next,x)[0]==.05,"D2_OLD_FROZEN_MEMORY_READ_WITHOUT_CURSOR_REWIND");
            String fresh=TargetIssueResolver.next(after).targetIssue;
            db.execSQL("CREATE TRIGGER fixture_slow_abort BEFORE INSERT ON "+SlowStateR932.HISTORY+" BEGIN SELECT RAISE(ABORT,'INJECTED'); END");
            rejected=false;try{SlowStateR932.capture(store,model,fresh,x,initial,base);}catch(IllegalStateException expected){rejected=true;}
            db.execSQL("DROP TRIGGER fixture_slow_abort");
            check(rejected&&Math.abs(SlowStateR932.capture(store,model,fresh,x,initial,base)[0]-.142625)<1e-15,"D2_HISTORY_AND_CURSOR_ATOMIC_ON_FAILURE");
            System.out.println("R932_SLOW_STATE_PASS|scope=REAL_SQLITE_PREDRAW_FEATURES_NO_LABEL_INPUT");
        }finally{db.shutdown();}
    }
}
