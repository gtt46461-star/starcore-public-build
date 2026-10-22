package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import java.nio.file.*;
import org.json.*;

/** Executes reported programs on a synthetic PIT stream; it never inserts predictions by hand. */
public final class ReportedCaptureHarnessR945 {
 static void check(boolean x,String m){if(!x)throw new AssertionError(m);System.out.println(m+"=PASS");}
 static long scalar(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static void addDraw(SQLiteDatabase db,String issue,Random random){
  ArrayList<Integer> n=new ArrayList<Integer>();for(int i=1;i<=10;i++)n.add(i);Collections.shuffle(n,random);
  Object[] args=new Object[11];args[0]=issue;for(int i=0;i<10;i++)args[i+1]=n.get(i);
  db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+"(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,?,?,?,?,?,?,?,?,?,?,0)",args);
 }
 static DeferredEvidenceSnapshotR36.Snapshot snapshot(String base,String target){
  String h=V51ChecksumEngine.sha256("TEST_PIT_"+base);
  ArrayList<DeferredEvidenceSnapshotR36.ActionRow> actions=new ArrayList<DeferredEvidenceSnapshotR36.ActionRow>();
  for(int p=1;p<=10;p++)for(int a=1;a<=8;a++)for(int b=a+1;b<=9;b++)for(int c=b+1;c<=10;c++){
   int i=K3ActionSpaceEngine.globalOrdinal(p,a,b,c),d=1;while(d==a||d==b||d==c)d++;
   actions.add(new DeferredEvidenceSnapshotR36.ActionRow(p,a,b,c,d,.3,.3+i*.00001,.3,i+1,h,"0.1,0.1,0.5,0.5",h,h));
  }
  ArrayList<DeferredEvidenceSnapshotR36.NumberRow> numbers=new ArrayList<>();
  for(int p=1;p<=10;p++)for(int n=1;n<=10;n++)numbers.add(new DeferredEvidenceSnapshotR36.NumberRow(p,n,.1,.1,n,"TEST","TEST",h));
  StringBuilder payload=new StringBuilder();
  for(DeferredEvidenceSnapshotR36.ActionRow a:actions)payload.append(K3ActionSpaceEngine.canonicalPayloadRow(a.position,a.n1,a.n2,a.n3,a.n4,a.rawActionP,a.rankScore,a.rank,a.fingerprint,a.actionHash));
  String actionHash=K3ActionSpaceEngine.actionSpaceHashFromPayloadBody(payload.toString(),actions.size());
  String recordHash=V51ChecksumEngine.sha256("R36_SNAPSHOT|"+target+'|'+h+'|'+h+'|'+h+'|'+actionHash);
  return new DeferredEvidenceSnapshotR36.Snapshot(target,base,h,h,h,actionHash,recordHash,"",h,0,0,1,true,false,"",numbers,actions,Collections.<String>emptyList(),Collections.<RuntimeTraceR36.Span>emptyList());
 }
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   DbStore store=new DbStore(db);DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);RuntimeEventLedgerR926.ensureSchema(db);DbHelper.ensureR906CriticalSchema(db);
   check(ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorR36(),8),"SCHEMA_READY");
   db.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER DEFAULT 0)");
   JSONObject fixture=new JSONObject(new String(Files.readAllBytes(Paths.get(args[1])),"UTF-8"));
   for(String table:new String[]{DbHelper.TABLE_R908_ALGORITHM_GENOME,AssetGenesisEngineR915.TABLE_ASSET_GENOME,DbHelper.TABLE_R906_RESEARCH_ASSET}){
    JSONObject t=fixture.getJSONObject(table);JSONArray cols=t.getJSONArray("columns"),rows=t.getJSONArray("rows");StringBuilder names=new StringBuilder(),marks=new StringBuilder();
    for(int j=0;j<cols.length();j++){if(j>0){names.append(',');marks.append(',');}names.append(cols.getString(j));marks.append('?');}
    db.beginTransaction();try{for(int i=0;i<rows.length();i++){Object[] values=new Object[cols.length()];for(int j=0;j<values.length;j++)values[j]=rows.getJSONArray(i).getString(j);db.execSQL("INSERT INTO "+table+"("+names+") VALUES("+marks+")",values);}db.setTransactionSuccessful();}finally{db.endTransaction();}
   }
   Random random=new Random(945);String base="202609140001";
   db.beginTransaction();try{for(int i=0;i<320;i++){addDraw(db,base,random);if(i<319)base=TargetIssueResolver.next(base).targetIssue;}db.setTransactionSuccessful();}finally{db.endTransaction();}
   HistoricalStrategyInventionR908.Profile before=HistoricalStrategyInventionR908.buildProfileAt(store,base);
   check(before==HistoricalStrategyInventionR908.buildProfileAt(store,base),"IDENTICAL_HISTORY_REUSES_PROFILE");
   db.execSQL("UPDATE "+DbHelper.TABLE_DRAWS+" SET n1=n2,n2=n1 WHERE issue=?",new Object[]{base});
   check(before!=HistoricalStrategyInventionR908.buildProfileAt(store,base),"SAME_ISSUE_CHANGED_HISTORY_INVALIDATES_PROFILE");
   db.execSQL("UPDATE "+DbHelper.TABLE_DRAWS+" SET n1=n2,n2=n1 WHERE issue=?",new Object[]{base});
   for(int period=0;period<3;period++){
    String target=TargetIssueResolver.next(base).targetIssue;DeferredEvidenceSnapshotR36.Snapshot snap=snapshot(base,target);
    boolean full=args.length>2&&"full".equals(args[2]);
    if(full){byte[] blob=DeferredEvidenceSnapshotR36.encode(snap);
     db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue,job_type,status,created_at,updated_at,record_hash,snapshot_blob,snapshot_hash) VALUES(?,?,?,0,0,?,?,?)",new Object[]{target,"TEST","PENDING",snap.recordHash,blob,DeferredEvidenceSnapshotR36.sha256(blob)});
    }
    GlobalResearchScheduleR932.beginShared(store,target,snap.recordHash);
    try{
     long start=System.nanoTime();if(!full)GlobalResearchScheduleR932.prepare(store,target,snap.recordHash);System.out.println("ROSTER_MS="+(System.nanoTime()-start)/1000000);
     int alg=0,asset=0,formula=0;
     for(int retry=0;retry<120;retry++){
      if(full){try{System.out.println("ENTRY="+ResearchPredrawCaptureR923.captureTarget(store,target,1600).canonical());}catch(ResearchWorkBudgetR927.Yield y){System.out.println("ENTRY=YIELD");}
       alg=(int)scalar(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE target_issue='"+target+"'");
       asset=(int)scalar(db,"SELECT COUNT(*) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" WHERE target_issue='"+target+"'");
      }else{
      ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(1600);
      try{alg=AlgorithmInventionLabR908.capturePredraw(store,snap,96,1600);}catch(ResearchWorkBudgetR927.Yield y){}finally{ResearchWorkBudgetR927.leave(scope);}
      scope=ResearchWorkBudgetR927.enter(1600);try{asset=AssetGenesisEngineR915.capturePredraw(store,snap,96,1600);}catch(ResearchWorkBudgetR927.Yield y){}finally{ResearchWorkBudgetR927.leave(scope);}
      scope=ResearchWorkBudgetR927.enter(1600);try{ResearchShadowChallengerRuntimeR817.captureR906Batch(store,snap,96,1600);}catch(ResearchWorkBudgetR927.Yield y){}finally{ResearchWorkBudgetR927.leave(scope);}
      }
      formula=(int)scalar(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE candidate_id IN (SELECT asset_id FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+") AND target_issue='"+target+"'");
      System.out.println("CAPTURE period="+period+" retry="+retry+" algorithms="+alg+" assets="+asset+" formulas="+formula);
      if(alg==8&&asset==8&&formula==693)break;
      if(retry==119)throw new AssertionError("CAPTURE_STALLED");
     }
     check(alg==8&&asset==8&&formula==693,"ALL_REPORTED_PROGRAMS_FROZEN_BEFORE_DRAW_"+period);
     addDraw(db,target,random);Models.Draw draw=store.getDrawByIssue(target);
     AlgorithmInventionLabR908.settleDraw(store,draw);AssetGenesisEngineR915.settleDraw(store,draw);ResearchShadowChallengerRuntimeR817.settle(store,draw);
     check(scalar(db,"SELECT MIN(state_updates) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==period+1,"ALL_ALGORITHMS_LEARN_NEXT_DRAW_"+period);
     check(scalar(db,"SELECT MIN(state_updates) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME)==period+1,"ALL_ASSETS_LEARN_NEXT_DRAW_"+period);
     check(scalar(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE candidate_id IN (SELECT asset_id FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+") AND target_issue='"+target+"' AND hit_label IN(0,1)")==693,"ALL_FORMULAS_SETTLED_"+period);
    }finally{GlobalResearchScheduleR932.endShared();}base=target;
   }
  }finally{db.shutdown();}
 }
}
