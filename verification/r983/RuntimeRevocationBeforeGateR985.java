package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
/** Isolated real-owner revocation after model preparation, before the sole DecisionGate. */
public final class RuntimeRevocationBeforeGateR985 {
 public static void main(String[] a)throws Exception{
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore store=HostProductR974.store(db);store.setState(BuildIdentity.STATE_STATUS,BuildIdentity.STATUS_OK);
   String certificate;try(Cursor c=db.rawQuery("SELECT certificate_hash FROM research_certificate_r929 WHERE state='ACTIVE'",null)){if(!c.moveToFirst())throw new AssertionError("No test certificate");certificate=c.getString(0);}
   db.beforeDecisionGate=()->{if(db.inTransaction())throw new AssertionError("Gate inside writer");if(!ResearchPlatformStoreR929.revoke(store,certificate,"EXPLICIT_TEST_ONLY_REVOKED_AFTER_PREPARE_BEFORE_GATE"))throw new AssertionError("Real revocation owner failed");System.out.println("ACTUAL_REVOCATION_AFTER_PREPARE_BEFORE_GATE");};
   String status=RuntimeConsumerChainR984.runFollowingPeriod(store,db,"202609230360","202609230361");
   if(db.beforeDecisionGate!=null)throw new AssertionError("Race was not injected");
   if(RuntimeConsumerChainR984.n(db,"SELECT COUNT(*) FROM trades WHERE target_issue='202609230361' AND action='BET'")!=0)throw new AssertionError("Revoked proof authorized REAL");
   if(RuntimeConsumerChainR984.n(db,"SELECT COUNT(*) FROM v57_shadow_observation_r35 WHERE target_issue='202609230361' AND pre_draw_frozen=1")!=1)throw new AssertionError("Actual NO_BET canonical missing");
   try(Cursor c=db.rawQuery("SELECT g.payload_text,s.canonical_reason_json,c.reason FROM v57_runtime_event_r926 g JOIN v57_shadow_observation_r35 s ON s.target_issue=g.target_issue JOIN research_consumer_choice_r932 f ON f.target_issue=s.target_issue JOIN research_certificate_r929 c ON c.certificate_hash=f.certificate_hash WHERE g.target_issue='202609230361' AND g.event_type='GATE_EVIDENCE'",null)){
    if(!c.moveToFirst()||!c.getString(0).contains("R974_EXACT_NO_REAL_AUTHORIZATION|certificate_reason=CERTIFICATE_INVALID_OR_CHANGED")||!c.getString(1).contains("NO_VALID_CERTIFICATE")||!c.getString(2).equals("EXPLICIT_TEST_ONLY_REVOKED_AFTER_PREPARE_BEFORE_GATE"))throw new AssertionError("Original gate, reason code and revocation cause lost");
   }
   System.out.println("RACE_NO_REAL_CANONICAL_PASS status="+status);
  }
 }
}
