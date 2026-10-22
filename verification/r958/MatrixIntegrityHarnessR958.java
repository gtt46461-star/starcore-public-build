package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import com.ethk3.novaonepro.MatrixSnapshotFeedbackR930.*;
import java.util.*;

/** Synthetic fault/replay evidence, with actual owners and SQLite; not device or live proof. */
public final class MatrixIntegrityHarnessR958 {
    private static int checks;
    private static final String TARGET="202609190101",BASE="202609190100";
    private static String h(String x){return V51ChecksumEngine.sha256(x);}
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;System.out.println(name+"=PASS");}
    private static void rejects(Runnable work,String name){boolean rejected=false;try{work.run();}catch(IllegalArgumentException|IllegalStateException expected){rejected=true;}check(rejected,name);}
    private static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    private static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    private static List<CaptureRecord> records(long latency){
        List<CaptureRecord> out=new ArrayList<CaptureRecord>();for(CaptureStage stage:CaptureStage.values())out.add(new CaptureRecord(stage,TARGET,BASE,1000,h("freeze"),h("input"),100,100,0,latency,h(stage.name())));return out;
    }
    private static FrozenMatrix matrix(String model,boolean present,double value,long latency){
        List<MatrixCell> cells=new ArrayList<MatrixCell>();for(int p=0;p<10;p++)for(int n=0;n<10;n++)cells.add(new MatrixCell(p,n,present,value,12,.7,latency,true,h("node"+p+'|'+n)));
        return new FrozenMatrix(MatrixKind.EVIDENCE_TEACHER,TARGET,BASE,1000,"FROZEN_PREDRAW",h("graph"),h("input"),model,cells);
    }
    private static CommonSnapshot common(FrozenMatrix matrix){return new CommonSnapshot(TARGET,BASE,1000,h("graph"),h("input"),h("model"),new CaptureBundle(records(1)),Arrays.asList(matrix),Collections.singleton(MatrixKind.EVIDENCE_TEACHER));}
    public static void main(String[] args)throws Exception{
        FrozenMatrix matrix=matrix(h("model"),true,.1,1);CommonSnapshot snapshot=common(matrix);
        check(matrix.matrixHash.equals(matrix(h("model"),true,.1,10000).matrixHash),"MEASURED_LATENCY_DOES_NOT_CHANGE_MATRIX_IDENTITY");
        List<CaptureRecord> reversed=records(10000);Collections.reverse(reversed);
        check(new CaptureBundle(records(1)).bundleHash.equals(new CaptureBundle(reversed).bundleHash),"CAPTURE_REPLAY_INDEPENDENT_OF_TIMING_AND_LIST_ORDER");
        rejects(()->common(matrix(h("other_model"),true,.1,1)),"MIXED_MODEL_REVISIONS_REJECTED");
        rejects(()->new CaptureRecord(CaptureStage.S1_ATOM_INPUT,TARGET,BASE,1000,h("f"),h("i"),100,100,1,0,h("p")),"CAPTURE_DENOMINATOR_OVERCOUNT_REJECTED");
        SettlementLabel label=new SettlementLabel(TARGET,new int[]{1,2,3,4,5,6,7,8,9,10},2000,2001,"SYNTHETIC","1");
        FeedbackDelta delta=MatrixSnapshotFeedbackR930.settle(snapshot,label,"202609190102");
        check(Math.abs(delta.brier-.09)<1e-12&&Math.abs(delta.crossEntropy-Math.log(10))<1e-12,"ACTUAL_NUMERIC_FEEDBACK_FROM_FROZEN_PROBABILITIES");
        check(Math.abs(delta.teacherResidual(0,0)-.9)<1e-12&&Math.abs(delta.teacherResidual(0,1)+.1)<1e-12,"ACTUAL_COUNTERFACTUAL_RESIDUAL_VECTOR");
        rejects(()->MatrixSnapshotFeedbackR930.settle(snapshot,label,TARGET),"FEEDBACK_CANNOT_AFFECT_SETTLED_ISSUE");
        rejects(()->MatrixSnapshotFeedbackR930.settle(common(matrix(h("model"),false,.1,1)),label,"202609190102"),"MISSING_MATRIX_NOT_IMPUTED_AS_ZERO_EVIDENCE");
        rejects(()->MatrixSnapshotFeedbackR930.settle(common(matrix(h("model"),true,.2,1)),label,"202609190102"),"UNNORMALIZED_SCORE_IS_NOT_PROBABILITY");
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try{
            DbStore store=new DbStore(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,status TEXT,settlement_hash TEXT,completed_at INTEGER)");
            String frozen=MatrixSnapshotBusR952.publish(store,TARGET,h("features"),h("payload"));
            String before=text(db,"SELECT payload_hash FROM r952_matrix_snapshot WHERE target_issue='"+TARGET+"'");
            long clock=System.currentTimeMillis()+60000;String settlement=h("settlement");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,1,2,3,4,5,6,7,8,9,10,0,?,?)",new Object[]{TARGET,clock,clock+1});
            final String[] feedback={null};
            db.beginTransaction();try{rejects(()->FeedbackMatrixRegistryR952.postDraw(db,TARGET,settlement),"FEEDBACK_REQUIRES_AUTHORITATIVE_TERMINAL");}finally{db.endTransaction();}
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,?)",new Object[]{TARGET,settlement,clock+2});
            for(int retry=0;retry<2;retry++){
                db.beginTransaction();try{String result=FeedbackMatrixRegistryR952.postDraw(db,TARGET,settlement);if(feedback[0]!=null)check(feedback[0].equals(result),"FEEDBACK_RETRY_EXACT");feedback[0]=result;db.setTransactionSuccessful();}finally{db.endTransaction();}
            }
            check(before.equals(text(db,"SELECT payload_hash FROM r952_matrix_snapshot WHERE target_issue='"+TARGET+"'")),"POSTDRAW_PRESERVES_ORIGINAL_PAYLOAD_HASH");
            check("PREDRAW_FROZEN".equals(text(db,"SELECT state FROM r952_matrix_snapshot WHERE target_issue='"+TARGET+"'")),"PREDRAW_STATE_STAYS_FROZEN");
            check(count(db,"SELECT COUNT(*) FROM r952_matrix_feedback")==1,"FEEDBACK_EXACTLY_ONCE");
            check(frozen.equals(text(db,"SELECT snapshot_hash FROM r952_matrix_feedback"))&&"202609190102".equals(text(db,"SELECT effective_from_issue FROM r952_matrix_feedback")),"FEEDBACK_BINDS_ORIGINAL_AND_NEXT_ISSUE");
            check(frozen.equals(MatrixSnapshotBusR952.publish(store,TARGET,h("features"),h("payload"))),"FROZEN_REPLAY_AFTER_SETTLEMENT_NO_NEW_CAPTURE");
            rejects(()->db.execSQL("UPDATE r952_matrix_snapshot SET payload_hash='TAMPER'"),"DIRECT_FROZEN_MUTATION_REJECTED");
            rejects(()->db.execSQL("UPDATE r952_matrix_feedback SET actual_numbers='TAMPER'"),"DIRECT_FEEDBACK_MUTATION_REJECTED");
            rejects(()->MatrixSnapshotBusR952.publish(store,"invalid",h("f"),h("p")),"INVALID_TARGET_REJECTED");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES('202609190102',1,2,3,4,5,6,7,8,9,10,0,?,?)",new Object[]{clock,clock+1});
            rejects(()->MatrixSnapshotBusR952.publish(store,"202609190102",h("f"),h("p")),"POSTDRAW_NEW_SNAPSHOT_REJECTED");
            for(int i=1;i<=90;i++){
                String issue=String.format(Locale.US,"20260919%04d",i);Object[] row=new Object[14];row[0]=issue;for(int j=0;j<10;j++)row[j+1]=(i+j)%10+1;row[11]=0;row[12]=clock-(100-i)*60000;row[13]=clock-(100-i)*60000+1;
                db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",row);
            }
            ResearchDirectorR952.capturePredraw(store,"202609190103",h("shared_upstream"),1);
            for(int i=2;i<=3;i++){
                CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.POSITION_FREQUENCY,i,i==2?20:80);
                db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,parent_id,state,future_n,created_issue,may_create_real,created_at) VALUES(?,?,?,?,?,'CREATED',0,?,0,0)",new Object[]{"TEST_"+i,"TEST_INTENT",ast.canonical,ast.structuralHash,"","202609190090"});
            }
            check(ResearchDirectorR952.capturePredraw(store,"202609190103",h("shared_upstream"),1)==1&&ResearchDirectorR952.capturePredraw(store,"202609190103",h("shared_upstream"),1)==1,"DIFFERENT_LOOKBACK_BATCHES_SHARE_COMMON_SNAPSHOT");
            check(count(db,"SELECT COUNT(DISTINCT snapshot_hash) FROM r952_candidate_predraw WHERE target_issue='202609190103'")==1,"ONE_TARGET_ONE_PIT_REVISION_ACROSS_BATCHES");
        }finally{db.shutdown();}
        System.out.println("R958_MATRIX_INTEGRITY_PASS|checks="+checks+"|scope=SYNTHETIC_NOT_FULL_MATRIX_PRODUCTION_INTEGRATION_NOT_DEVICE");
    }
}
