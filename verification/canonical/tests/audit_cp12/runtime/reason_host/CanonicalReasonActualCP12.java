package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import android.database.Cursor;import org.json.JSONObject;
public final class CanonicalReasonActualCP12 {
 static int checks;static void need(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 public static void main(String[] a)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(a[0],a[1]);try{
   DbStore store=HostProductR974.store(db);
   for(Models.ReasonCode c:Models.ReasonCode.values())need(CanonicalReasonCP12.normalize(c.name())==c,"enum:"+c);
   for(V51Enums.RejectCode c:V51Enums.RejectCode.values())need(CanonicalReasonCP12.normalize(c.name())!=Models.ReasonCode.CHECK_UNREGISTERED,"V51:"+c);
   need(CanonicalReasonCP12.normalize("OWNER_CODE_NOT_REGISTERED")==Models.ReasonCode.CHECK_UNREGISTERED,"unknown visible");
   String h=V51ChecksumEngine.sha256("history-fixture");long now=System.currentTimeMillis();
   CanonicalObservationWriterR36.CanonicalRecord rec=new CanonicalObservationWriterR36.CanonicalRecord("202610030020","202610030021",1,1,2,3,.35,.1,"SHADOW","HOLD","DOMAIN_NOT_PROVEN",h,h,"",V51ChecksumEngine.sha256("record-fixture"),"CP12_REASON_ATTEMPT",h,h,h,now,now,now,now+5000);
   DatabaseCoordinator.TransactionContext tx=new DatabaseCoordinator.TransactionContext("CP12_REASON_TEST",rec.targetIssue,"REASON-IDEMPOTENCY");
   db.beginTransaction();try{CanonicalObservationWriterR36.WriteResult w=CanonicalObservationWriterR36.insertWithinTransaction(db,tx,rec);need(w.success&&!w.replay,"actual owner insert");db.setTransactionSuccessful();}finally{db.endTransaction();}
   String json,hash;try(Cursor c=db.rawQuery("SELECT canonical_reason_json,canonical_reason_hash FROM v57_shadow_observation_r35 WHERE target_issue=?",new String[]{rec.targetIssue})){need(c.moveToFirst(),"receipt durable");json=c.getString(0);hash=c.getString(1);}
   need(CanonicalReasonCP12.persistedCode(CanonicalReasonCP12.existingReceiptText(json,hash,"summary"))==Models.ReasonCode.NO_MAINLINE,"durable code exact");
   JSONObject o=new JSONObject(json);need(rec.historyHash.equals(o.getString("source_hash"))&&rec.attemptId.equals(o.getString("attempt_id")),"owner identities");
   need(CanonicalObservationWriterR36.readView(store,rec.targetIssue).reasonHash.equals(hash),"actual view consumes receipt");
   Models.Decision ui=CanonicalDecisionViewAdapterR36.read(store,rec.targetIssue);need(CanonicalReasonCP12.persistedCode(ui.reasonText)==Models.ReasonCode.NO_MAINLINE,"UI consumes persisted receipt");
   boolean rejected=false;try{db.execSQL("UPDATE v57_shadow_observation_r35 SET canonical_reason_hash='' WHERE target_issue=?",new Object[]{rec.targetIssue});}catch(RuntimeException expected){rejected=true;}need(rejected,"immutable reason");
   need(CanonicalReasonCP12.persistedCode(CanonicalReasonCP12.existingReceiptText(json,"0".repeat(64),"s"))==Models.ReasonCode.INTEGRITY_FAILED,"hash drift");
   need(CanonicalReasonCP12.persistedCode("historical plain summary")==Models.ReasonCode.CHECK_NOT_REEVALUATED,"legacy never fabricated");
   db.beginTransaction();try{need(CanonicalObservationWriterR36.insertWithinTransaction(db,tx,rec).replay,"replay exact owner");db.setTransactionSuccessful();}finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("SELECT canonical_reason_hash FROM v57_shadow_observation_r35 WHERE target_issue=?",new String[]{rec.targetIssue})){need(c.moveToFirst()&&hash.equals(c.getString(0)),"replay retains original observation time");}
   CanonicalReasonCP12.Receipt x=new CanonicalReasonCP12.Receipt("UNKNOWN\nA","B","T","","",0,0),y=new CanonicalReasonCP12.Receipt("UNKNOWN","A\nB","T","","",0,0);need(!x.hash.equals(y.hash),"hash length framed");
   System.out.println("PASS|CanonicalReasonActualCP12|assertions="+checks+"|actualOwnerWriter=true|actualUIConsumer=true|hostSQLite=true");
  }finally{db.shutdown();}
 }
}
