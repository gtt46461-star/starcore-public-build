package com.ethk3.novaonepro.r952;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;import java.nio.file.*;import java.util.*;
public final class ResidualOwnersContractCP12 {
 static int checks;static void ok(boolean value){checks++;if(!value)throw new AssertionError("assertion "+checks);}
 interface Call {void run();}static void bad(Call op){checks++;try{op.run();}catch(RuntimeException pass){return;}throw new AssertionError("invalid accepted");}
 static long count(SQLiteDatabase db,String t){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+t,null)){c.moveToFirst();return c.getLong(0);}}
 static void candidate(SQLiteDatabase db,String id,CandidateAstR952 a,String issue){db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,parent_id,state,future_n,created_issue,may_create_real,created_at) VALUES(?,'intent',?,?,'','NURSERY',0,?,0,1)",new Object[]{id,a.canonical,a.structuralHash,issue});}
 public static void main(String[] args)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);String issue="202609230001",next=TargetIssueResolver.next(issue).targetIssue,hash=V51ChecksumEngine.sha256("proof");
  CandidateAstR952 p=new CandidateAstR952(CandidateAstR952.Operator.RECENCY_BLEND,1,120),c=p.withLookback(140);
  ok(ResearchTinyDeepLearnerR909.updateOne(null).status.equals("STORE_NULL"));ok(EvolutionEvidenceR975.next(null,issue)==null);
  bad(()->EvolutionEvidenceR975.advance(null,null));bad(()->EvolutionEvidenceR975.finish(null,null,"CONSUMED","x","c"));
  bad(()->MutationPolicyR952.propose("p",p,hash,20,20));bad(()->MutationPolicyR952.proposeNovel("p",p,hash,20,20,Collections.emptySet()));bad(()->MutationPolicyR952.propose("p",p,hash));
  bad(()->ComputationReceiptR974.lineage(db,"c","p","PRIMARY",p.canonical,hash,issue));
  db.beginTransaction();try{candidate(db,"p",p,issue);candidate(db,"c",c,issue);db.setTransactionSuccessful();}finally{db.endTransaction();}
  db.beginTransaction();try{ComputationReceiptR974.lineage(db,"c","p","PRIMARY",p.canonical,hash,issue);}finally{db.endTransaction();}ok(count(db,"r974_model_lineage")==0);
  db.beginTransaction();try{ComputationReceiptR974.lineage(db,"c","p","PRIMARY",p.canonical,hash,issue);ComputationReceiptR974.lineage(db,"c","p","PRIMARY",p.canonical,hash,issue);db.setTransactionSuccessful();}finally{db.endTransaction();}ok(count(db,"r974_model_lineage")==1);
  db.beginTransaction();try{bad(()->ComputationReceiptR974.lineage(db,"c","p","PRIMARY",p.canonical,V51ChecksumEngine.sha256("changed"),issue));bad(()->ComputationReceiptR974.lineage(db,"p","p","PRIMARY",p.canonical,hash,issue));bad(()->ComputationReceiptR974.lineage(db,"c","p","FALSE_ROLE",p.canonical,hash,issue));}finally{db.endTransaction();}
  String settlement=V51ChecksumEngine.sha256("settlement"),label=V51ChecksumEngine.sha256("label"),input=V51ChecksumEngine.sha256("input");
  bad(()->ResearchOutboxR952.enqueue(db,issue,"p",settlement,label,input));
  ResearchOutboxR952.Event event;
  db.beginTransaction();try{String d=ResearchOutboxR952.enqueue(db,issue,"p",settlement,label,input);ok(d.equals(ResearchOutboxR952.enqueue(db,issue,"p",settlement,label,input)));bad(()->ResearchOutboxR952.enqueue(db,issue,"p",settlement,V51ChecksumEngine.sha256("badlabel"),input));ResearchOutboxR952.enqueue(db,next,"p",settlement,label,input);db.setTransactionSuccessful();}finally{db.endTransaction();}
  db.beginTransaction();try{event=ResearchOutboxR952.claim(db,System.currentTimeMillis());ok(event.targetIssue.equals(issue));db.setTransactionSuccessful();}finally{db.endTransaction();}
  db.beginTransaction();try{ok(ResearchOutboxR952.claim(db,System.currentTimeMillis())==null);ok(ResearchOutboxR952.owns(db,event));ok(ResearchOutboxR952.release(db,event,"TEST_RETRY",0));db.setTransactionSuccessful();}finally{db.endTransaction();}
  ResearchOutboxR952.Event newer;db.beginTransaction();try{newer=ResearchOutboxR952.claim(db,System.currentTimeMillis());ok(newer.targetIssue.equals(issue)&&newer.attempt==event.attempt+1);ok(!ResearchOutboxR952.owns(db,event));String task=PostSettlementResearchLoopR952.enqueueCommitted(db,newer);ok(task.equals(PostSettlementResearchLoopR952.enqueueCommitted(db,newer)));ok(count(db,ResearchTaskQueueR967.TABLE)==1);ok(count(db,ResearchTaskQueueR967.PIN)==4);db.setTransactionSuccessful();}finally{db.endTransaction();}
  try(Cursor f=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!f.moveToFirst());}db.shutdown();Files.writeString(Path.of(args[2]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"scope\":\"Actual legacy retirement, immutable lineage, outbox ordering/lease fences and POST idempotent enqueue on host SQLite/JVM. No Android scheduler claim.\"}\n");
 }
}
