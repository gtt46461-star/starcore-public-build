package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.nio.file.*;
import java.util.*;

/** Synthetic input only. All candidate predictions, audit proofs, calibration
 * observations, counters and the fitted model are created by product owners.
 * Seed and resume run in distinct JVMs against the same isolated installed DB. */
public final class MainlineCaptureR75 {
    static int checks;
    static String h(String s){return V51ChecksumEngine.sha256(s);}
    static void ok(boolean value,String name){if(!value)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static String issue(int i){return String.format(java.util.Locale.ROOT,"20260923%04d",i);}
    static void draw(SQLiteDatabase db,int i,long at){
        db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10)VALUES(?,?,?,0,?,?,?,?,?,?,?,?,?,?)",
            new Object[]{issue(i),at,at+1,(i+0)%10+1,(i+1)%10+1,(i+2)%10+1,(i+3)%10+1,(i+4)%10+1,(i+5)%10+1,(i+6)%10+1,(i+7)%10+1,(i+8)%10+1,(i+9)%10+1});
    }
    static void seed(SQLiteDatabase db)throws Exception{seed(db,"r75-actual-capture",true);}
    static void seed(SQLiteDatabase db,String learner,boolean history)throws Exception{
        TrainingFixtureCP11 fixture=new TrainingFixtureCP11();CandidateAstR952 ast=fixture.ast;
        CandidateCompilerR952.Model model;
        try(ResearchTrainingHookRegistryR952.PreparedNumericsCP9 trained=ResearchTrainingHookRegistryR952.prepareNumericsCP9(fixture.dataset,null,System.nanoTime()+10_000_000_000L,RuntimeMemoryGuardR811.algorithmModelBudget())){
            ok(trained.complete(),"actual AST3 development numeric training completed");model=trained.model(h("R75_SYNTHETIC_DEVELOPMENT_RECEIPT"));
        }
        String target=issue(442),rule=TrainingCodeIdentityR964.SHA256,artifact=h("r75-artifact:"+learner);
        String epoch=new ResearchSemanticsR967.Builder("R952_NUMERIC_RUN_ORIGIN_V1").text("learner",learner).text("first",target).hash("ast",ast.structuralHash).hash("rule",rule).hash("initial",model.hash()).text("artifact",artifact).digest();
        String run=new ResearchSemanticsR967.Builder("R967_NUMERIC_RUN_V2").text("learner",learner).text("first",target).hash("ast",ast.structuralHash).hash("rule",rule).hash("initial",model.hash()).text("artifact",artifact).text("split",ResearchEvaluationRunR967.SPLIT).hash("epoch",epoch).digest();
        String structure=ResearchEvaluationRunR967.evaluationHash(ast,model,rule,epoch),id=ResearchEvaluationRunR967.PREFIX+structure.substring(0,24);
        db.beginTransaction();try{
            db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,state,created_issue,may_create_real,created_at)VALUES(?,?,?,?,'VALIDATED',?,0,1)",new Object[]{learner,learner,ast.canonical,ast.structuralHash,target});
            db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.RUN+" VALUES(?,?,?,?,?,?,300,?,?,?,1)",new Object[]{run,learner,target,ast.structuralHash,rule,model.payload(),artifact,ResearchEvaluationRunR967.SPLIT,epoch});
            db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.EVAL+" VALUES(?,?,?,?,?,?,?,?,?,1,?,?,?,300)",new Object[]{id,learner,ast.canonical,model.payload(),structure,model.hash(),model.modern().trainedUntil,target,artifact,run,rule,epoch});
            if(history)for(int i=342;i<=441;i++)draw(db,i,i*1000L);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    static DeferredEvidenceSnapshotR36.Snapshot snapshot(String base,String target,String bundle){
        ArrayList<DeferredEvidenceSnapshotR36.NumberRow> numbers=new ArrayList<>();
        for(int p=1;p<=10;p++)for(int a=1;a<=10;a++)numbers.add(new DeferredEvidenceSnapshotR36.NumberRow(p,a,0,.1,a,"state","domain",h("expert")));
        ArrayList<DeferredEvidenceSnapshotR36.ActionRow> actions=new ArrayList<>();StringBuilder body=new StringBuilder();
        for(K3ActionSpaceEngine.ActionIdentity a:K3ActionSpaceEngine.enumerateIdentityUniverse()){
            String fp=h("fingerprint:"+a.globalOrdinal),hash=h("action:"+a.globalOrdinal);int fourth=1;while(a.contains(fourth))fourth++;
            DeferredEvidenceSnapshotR36.ActionRow row=new DeferredEvidenceSnapshotR36.ActionRow(a.position,a.n1,a.n2,a.n3,fourth,.3,1-a.globalOrdinal/1200d,-1,a.globalOrdinal+1,fp,"0,0,0,0",hash,h("row"+a.globalOrdinal));actions.add(row);
            body.append(K3ActionSpaceEngine.canonicalPayloadRow(a.position,a.n1,a.n2,a.n3,fourth,.3,row.rankScore,row.rank,fp,hash));
        }
        String actionHash=K3ActionSpaceEngine.actionSpaceHashFromPayloadBody(body.toString(),1200),history=h("history:"+base),contract=h("contract"),record=h("R36_SNAPSHOT|"+target+'|'+history+'|'+bundle+'|'+contract+'|'+actionHash);
        DeferredEvidenceSnapshotR36.Snapshot snapshot=new DeferredEvidenceSnapshotR36.Snapshot(target,base,history,bundle,contract,actionHash,record,"",h("permutation"),0,0,1,true,false,"",numbers,actions,Collections.emptyList(),Collections.emptyList());snapshot.validate();return snapshot;
    }
    public static void main(String[] a)throws Exception{
        boolean initial="seed".equals(a[2]);
        try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            if(initial)seed(db);
            int begin=initial?442:562,end=initial?561:682;
            String bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash;
            String frozen=text(db,"SELECT candidate_id FROM "+ResearchEvaluationRunR967.EVAL),original=text(db,"SELECT model_hash FROM "+ResearchEvaluationRunR967.EVAL);
            if(!initial)ok(n(db,"SELECT captured_n FROM "+CandidateCalibrationR932.STUDY)==120,"fresh JVM restores 120 durable prospective observations");
            int yields=0;
            for(int i=begin;i<=end;i++){
                String target=issue(i);DeferredEvidenceSnapshotR36.Snapshot snap=snapshot(issue(i-1),target,bundle);
                db.beginTransaction();try{
                    db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,1,2,3,?,?)",new Object[]{target,bundle,snap.recordHash,selector,h("fixture-champion:"+target),System.currentTimeMillis()});
                    GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,target,bundle,selector);
                    db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                int captured=0;for(int retry=0;retry<8&&captured==0;retry++)try{captured=FrozenCandidateAdapterR958.captureReservedCP12(store,snap);}catch(ResearchWorkBudgetR927.Yield y){yields++;}
                if(captured!=1)throw new AssertionError("actual capture stalled at "+target);
                if(FrozenCandidateAdapterR958.captureReservedCP12(store,snap)!=0)throw new AssertionError("duplicate capture "+target);
                long at=System.currentTimeMillis()+2;String terminal=h("synthetic-terminal:"+target);
                db.beginTransaction();try{
                    draw(db,i,at);
                    db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash) VALUES(?,?,'COMPLETED',?,?,?,?,?)",new Object[]{target,target,at+1,at+1,at+1,terminal,terminal});
                    Models.Draw draw=store.getDrawByIssue(target);
                    if(FrozenCandidateAdapterR958.settle(db,draw,terminal)!=1)throw new AssertionError("freeze settlement "+target);
                    CandidateCalibrationR932.settle(db,draw);CandidateCalibrationR932.settle(db,draw);
                    db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                if(i%20==1)System.out.println("CAPTURE_PROGRESS "+(i-441)+"/241");
            }
            int wanted=initial?120:241;
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==wanted,"actual calibration observations="+wanted);
            ok(n(db,"SELECT captured_n FROM "+CandidateCalibrationR932.STUDY)==wanted&&n(db,"SELECT settled_n FROM "+CandidateCalibrationR932.STUDY)==wanted,"exactly once capture and settle counters="+wanted);
            ok(n(db,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW+" WHERE settled=1 AND proof_error=''")==wanted,"actual frozen predictions authoritatively settled="+wanted);
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateExecutionAuditR932.TABLE+" WHERE action_count=1200")==wanted,"complete1200 execution proofs="+wanted);
            ok(original.equals(text(db,"SELECT model_hash FROM "+ResearchEvaluationRunR967.EVAL)),"frozen version remains identical across stream");
            ok(n(db,"SELECT COUNT(*) FROM r979_calibration_restart")==0,"continuous owner makes no study resets");
            if(!initial){
                ok("READY".equals(text(db,"SELECT state FROM "+CandidateCalibrationR932.STUDY)),"241st real settlement becomes READY");
                CandidateCalibrationR932.fitOne(store);CandidateCalibrationR932.fitOne(store);
                ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)==1,"actual logistic fit publishes exactly once");
                String study=text(db,"SELECT study_hash FROM "+CandidateCalibrationR932.STUDY);
                CandidateCalibrationR932.Qualification quality=CandidateCalibrationR932.qualificationForStudyCP12(db,study);
                ok(quality.state!=CandidateCalibrationR932.QualificationState.UNFITTED,"quality evaluated from actual holdout");
                System.out.println("ACTUAL_QUALITY="+quality.state+" ECE="+quality.ece+" BRIER="+quality.brier+" BASELINE="+quality.baselineBrier);
                ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE state='COMPLETE'")==1,"reservation completes at original endpoint");
            }
            ok(n(db,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_TRADES)==0,"calibration does not fabricate formal certificate or REAL");
            Files.writeString(Path.of(a[3]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"observations\":"+wanted+",\"yields\":"+yields+",\"scope\":\"PUBLIC_AST3_CAPTURE_SETTLE_FIXED_CALIBRATION_SYNTHETIC_HOST_NOT_LIVE_QUALITY\"}\n");
        }
    }
}
