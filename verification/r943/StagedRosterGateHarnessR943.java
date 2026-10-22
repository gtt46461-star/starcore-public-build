package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
public final class StagedRosterGateHarnessR943 {
 static void check(boolean p,String m){if(!p)throw new AssertionError(m);System.out.println(m+"=PASS");}
 static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):-1;}}
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   DbStore store=new DbStore(db);db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY)");
   GlobalResearchScheduleR932.schema(db);StagedResearchRosterR943.schema(db);String target="202609130100",input=V51ChecksumEngine.sha256("FROZEN_INPUT");
   JSONArray rows=new JSONArray();for(int i=0;i<554;i++){JSONObject row=new JSONObject();String hash=V51ChecksumEngine.sha256("f"+i);row.put("key","formula:f"+i);row.put("family","family"+(i%8));row.put("structure",hash);row.put("semantic",hash);row.put("tier","C");rows.put(row);}
   String payload=rows.toString();db.execSQL("INSERT INTO "+StagedResearchRosterR943.TABLE+" VALUES(?,?,?,?,0,1)",new Object[]{target,input,payload,V51ChecksumEngine.sha256(payload)});
   boolean ready=StagedResearchRosterR943.preparePage(store,target,input);
   check(!ready&&scalar(db,"SELECT cursor FROM "+StagedResearchRosterR943.TABLE)==32,"REGISTRATION_PAGE_SURVIVES_COOPERATIVE_YIELD");
   check(scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.PLAN)==0,"PARTIAL_ROSTER_NOT_PUBLISHED");
   db.execSQL("CREATE TRIGGER fixture_page_abort BEFORE INSERT ON "+GlobalResearchScheduleR932.TASK+" WHEN NEW.queue_rank=40 BEGIN SELECT RAISE(ABORT,'INJECTED'); END");
   boolean failed=false;try{StagedResearchRosterR943.prepare(store,target,input);}catch(IllegalStateException expected){failed=true;}
   check(failed&&scalar(db,"SELECT cursor FROM "+StagedResearchRosterR943.TABLE)==32&&scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK)==32,"FAILED_PAGE_ROLLS_BACK_WITH_PRIOR_PROGRESS_PRESERVED");
   db.execSQL("DROP TRIGGER fixture_page_abort");
   try{StagedResearchRosterR943.prepare(store,target,input);}catch(ResearchWorkBudgetR927.Yield expected){}
   StagedResearchRosterR943.prepare(store,target,input);StagedResearchRosterR943.prepare(store,target,input);
   check(scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.PLAN)==1&&scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE selected=1")==554,"RESTART_RESUMES_ALL_CANDIDATES_WITHOUT_DUPLICATES");
   String next="202609130101";
   db.execSQL("INSERT INTO "+StagedResearchRosterR943.TABLE+" VALUES(?,?,?,?,0,1)",new Object[]{next,input,payload,V51ChecksumEngine.sha256(payload)});
   StagedResearchRosterR943.prepare(store,next,input);
   check(scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE target_issue='"+next+"'")==554,"NEXT_ISSUE_REUSES_554_IDENTITIES");
   check(scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.IDENTITY)==554,"NO_DUPLICATE_STATISTICAL_REGISTRATION");
   String corrupt="202609130102";
   db.execSQL("INSERT INTO "+StagedResearchRosterR943.TABLE+" VALUES(?,?,?,?,0,1)",new Object[]{corrupt,input,payload,V51ChecksumEngine.sha256(payload)});
   db.execSQL("UPDATE "+GlobalResearchScheduleR932.IDENTITY+" SET structure_hash=? WHERE candidate_key='formula:f0'",new Object[]{V51ChecksumEngine.sha256("corrupt")});
   boolean rejected=false;try{StagedResearchRosterR943.prepare(store,corrupt,input);}catch(IllegalStateException expected){rejected=true;}
   check(rejected&&scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE target_issue='"+corrupt+"'")==0,"REGISTERED_IDENTITY_DRIFT_ROLLS_BACK_PAGE");
   check(!GlobalResearchScheduleR932.proxy(store,target,"formula:f1"),"CREATED_ALGORITHM_REQUIRES_FULL_ACTION_EVALUATION");
   boolean corruptNotSchedulable=false;try{GlobalResearchScheduleR932.proxy(store,target,"formula:f0");}catch(IllegalStateException expected){corruptNotSchedulable=true;}
   check(corruptNotSchedulable,"CORRUPTED_IDENTITY_NOT_ADMITTED_BY_COMMITTED_PAGE");
   boolean drift=false;try{StagedResearchRosterR943.prepare(store,target,V51ChecksumEngine.sha256("changed"));}catch(IllegalStateException expected){drift=true;}
   check(drift,"CHANGED_TARGET_INPUT_CANNOT_REUSE_ROSTER");
   CertificateBundleServiceR929.EvidenceInput e=new CertificateBundleServiceR929.EvidenceInput(true,true,true,360,360,2,.34,.301,1,.23,.02,.03,.01,true,true,V51ChecksumEngine.sha256("test"));
   CertificateBundleServiceR929.Evaluation pass=CertificateBundleServiceR929.evaluate(e);
   check(CertificateBundleServiceR929.validatedInfluenceCap(pass.gates)==.05,"34_PERCENT_WITH_STABILITY_AND_EVIDENCE_CAN_INFLUENCE");
   CertificateBundleServiceR929.EvidenceInput bad=new CertificateBundleServiceR929.EvidenceInput(true,true,true,360,360,2,.339,.301,1,.23,.02,.03,.01,true,true,V51ChecksumEngine.sha256("bad"));
   check(CertificateBundleServiceR929.validatedInfluenceCap(CertificateBundleServiceR929.evaluate(bad).gates)==0,"BELOW_34_PERCENT_CANNOT_INFLUENCE");
   System.out.println("R943_STAGED_ROSTER_AND_34_GATE_PASS|scope=SYNTHETIC_REAL_SQLITE_NOT_EFFECT");
  }finally{db.shutdown();}
 }
}
