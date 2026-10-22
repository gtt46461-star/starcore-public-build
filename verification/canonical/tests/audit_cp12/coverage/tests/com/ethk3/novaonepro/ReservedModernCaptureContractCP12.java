package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import com.ethk3.novaonepro.r952.*;import java.nio.file.*;import java.util.*;
public final class ReservedModernCaptureContractCP12 {
 static int n;static void ok(boolean b){if(!b)throw new AssertionError("check"+n);n++;}
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 static String H(String s){return V51ChecksumEngine.sha256(s);}
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
  try{
   DbStore store=HostProductR974.store(db);String target="202609230442",base="202609230441",learner="numeric-child";
   TrainingFixtureCP11 fixture=new TrainingFixtureCP11();CandidateAstR952 ast=fixture.ast;
   CandidateCompilerR952.Model model;
   try(ResearchTrainingHookRegistryR952.PreparedNumericsCP9 trained=ResearchTrainingHookRegistryR952.prepareNumericsCP9(fixture.dataset,null,System.nanoTime()+10_000_000_000L,RuntimeMemoryGuardR811.algorithmModelBudget())){
    ok(trained.complete());model=trained.model(H("actual_development_receipt"));
   }
   String rule=TrainingCodeIdentityR964.SHA256,artifact=H("artifact");
   String epoch=new ResearchSemanticsR967.Builder("R952_NUMERIC_RUN_ORIGIN_V1").text("learner",learner).text("first",target).hash("ast",ast.structuralHash).hash("rule",rule).hash("initial",model.hash()).text("artifact",artifact).digest();
   String run=new ResearchSemanticsR967.Builder("R967_NUMERIC_RUN_V2").text("learner",learner).text("first",target).hash("ast",ast.structuralHash).hash("rule",rule).hash("initial",model.hash()).text("artifact",artifact).text("split",ResearchEvaluationRunR967.SPLIT).hash("epoch",epoch).digest();
   String structure=ResearchEvaluationRunR967.evaluationHash(ast,model,rule,epoch),id=ResearchEvaluationRunR967.PREFIX+structure.substring(0,24),bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash;
   db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,state,created_issue,may_create_real,created_at)VALUES(?,?,?,?,'VALIDATED',?,0,1)",new Object[]{learner,learner,ast.canonical,ast.structuralHash,target});
   db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.RUN+" VALUES(?,?,?,?,?,?,300,?,?,?,1)",new Object[]{run,learner,target,ast.structuralHash,rule,model.payload(),artifact,ResearchEvaluationRunR967.SPLIT,epoch});
   db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.EVAL+" VALUES(?,?,?,?,?,?,?,?,?,1,?,?,?,300)",new Object[]{id,learner,ast.canonical,model.payload(),structure,model.hash(),model.modern().trainedUntil,target,artifact,run,rule,epoch});
   for(int i=422;i<=441;i++)db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10)VALUES(?,?,?,0,?,?,?,?,?,?,?,?,?,?)",new Object[]{String.format("20260923%04d",i),1000L*i,1000L*i+1,(i+0)%10+1,(i+1)%10+1,(i+2)%10+1,(i+3)%10+1,(i+4)%10+1,(i+5)%10+1,(i+6)%10+1,(i+7)%10+1,(i+8)%10+1,(i+9)%10+1});
   ArrayList<DeferredEvidenceSnapshotR36.NumberRow> numbers=new ArrayList<>();for(int p=1;p<=10;p++)for(int a=1;a<=10;a++)numbers.add(new DeferredEvidenceSnapshotR36.NumberRow(p,a,0,.1,a,"state","domain",H("expert")));
   ArrayList<DeferredEvidenceSnapshotR36.ActionRow> actions=new ArrayList<>();StringBuilder actionBody=new StringBuilder();
   for(K3ActionSpaceEngine.ActionIdentity a:K3ActionSpaceEngine.enumerateIdentityUniverse()){
    String fp=H("fingerprint:"+a.globalOrdinal),hash=H("action:"+a.globalOrdinal);int fourth=1;while(fourth==a.n1||fourth==a.n2||fourth==a.n3)fourth++;
    DeferredEvidenceSnapshotR36.ActionRow row=new DeferredEvidenceSnapshotR36.ActionRow(a.position,a.n1,a.n2,a.n3,fourth,.3,1-a.globalOrdinal/1200d,-1,a.globalOrdinal+1,fp,"0,0,0,0",hash,H("row"+a.globalOrdinal));actions.add(row);
    actionBody.append(K3ActionSpaceEngine.canonicalPayloadRow(a.position,a.n1,a.n2,a.n3,fourth,.3,row.rankScore,row.rank,fp,hash));
   }
   String actionHash=K3ActionSpaceEngine.actionSpaceHashFromPayloadBody(actionBody.toString(),1200),historyHash=H("history"),contractHash=H("contract"),record=H("R36_SNAPSHOT|"+target+'|'+historyHash+'|'+bundle+'|'+contractHash+'|'+actionHash);
   DeferredEvidenceSnapshotR36.Snapshot snapshot=new DeferredEvidenceSnapshotR36.Snapshot(target,base,historyHash,bundle,contractHash,actionHash,record,"",H("permutation"),0,0,1,true,false,"",numbers,actions,Collections.emptyList(),Collections.emptyList());snapshot.validate();
   db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,1,2,3,?,1)",new Object[]{target,bundle,record,selector,H("champion")});
   db.beginTransaction();try{ok(GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,target,bundle,selector)==1);db.setTransactionSuccessful();}finally{db.endTransaction();}
   // A pre-existing rejected formal generation must not block upstream prospective calibration.
   db.execSQL("INSERT INTO "+FixedResearchWindowR932.WINDOW+"(window_id,candidate_key,structural_hash,champion_hash,selection_hash,first_issue,last_issue,planned_n,window_no,registered_at,state,reason) VALUES(?,?,?,?,?,?,?,120,1,1,'FAIL','PRIOR_FORMAL_FAILED')",new Object[]{H("prior-window"),"algorithm:"+id,structure,bundle,selector,"202609230100","202609230219"});
   // Warm the independent pure scoring/JIT path, leaving all publication to the real method.
   PITSnapshotContractR952 pit=PITSnapshotContractR952.capture(store,target,5000);for(int i=0;i<3;i++)try(FrozenGraphContextCP4 ctx=FrozenCandidateAdapterR958.prepareContextCP8(store.readable(),FrozenCandidateAdapterR958.load(db,id),pit,System.currentTimeMillis())){FrozenCandidateAdapterR958.score(ast,model,pit,ctx,true);}
   int captured=0,yields=0;for(int i=0;i<5&&captured==0;i++)try{captured=FrozenCandidateAdapterR958.captureReservedCP12(store,snapshot);}catch(ResearchWorkBudgetR927.Yield y){yields++;}
   ok(captured==1);ok(count(db,FrozenCandidateAdapterR958.PREDRAW)==1);ok(count(db,CandidateCalibrationR932.OBS)==1);ok(count(db,CandidateExecutionAuditR932.TABLE)==1);
   ok(FrozenCandidateAdapterR958.captureReservedCP12(store,snapshot)==0);ok(count(db,CandidateCalibrationR932.MODEL)==0);ok(count(db,FixedResearchWindowR932.WINDOW)==1);ok(count(db,DbHelper.TABLE_TRADES)==0);
   try(Cursor c=db.rawQuery("SELECT state,next_issue,generation FROM research_capture_reservation_v2",null)){ok(c.moveToFirst()&&"ACTIVE".equals(c.getString(0))&&"202609230443".equals(c.getString(1))&&c.getInt(2)==1);}
   Files.writeString(Path.of(args[2]),"{\"status\":\"PASS\",\"assertions\":"+n+",\"warmup_yields\":"+yields+",\"actual_frozen_capture\":true,\"scope\":\"Actual reserved public caller + real developed AST3/FIXED_GRAPH/model, valid 100/1200 Snapshot, PIT, full1200 score/replay proof, prediction/calibration/advance transaction despite prior rejected formal window on host SQLite. Android performance and device timing not asserted.\"}\n");
  }finally{db.shutdown();}
 }
}
