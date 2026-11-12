package com.ethk3.novaonepro.strategyv2;
import java.sql.*;import java.util.*;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
/** R31 K08 (B10-C01/C03) on the actual compiled MainlineChoiceStoreR31, MainlineBridgeSU2 request lane and R27 schema
 * over native SQLite. Test namespace fixture certificates only: no natural qualification, no device evidence. */
public final class R31MainlineAuthorityHost {
 static int assertions;static void check(boolean v,String s){assertions++;if(!v)throw new AssertionError(s);}
 static long count(SQLiteDatabase d,String sql,String... a){try(Cursor c=d.rawQuery(sql,a.length==0?null:a)){return c.moveToFirst()?c.getLong(0):0;}}
 static String text(SQLiteDatabase d,String sql,String... a){try(Cursor c=d.rawQuery(sql,a.length==0?null:a)){return c.moveToFirst()?c.getString(0):null;}}
 interface Tx{void run()throws Exception;}
 static void tx(Connection c,Tx body)throws Exception{c.setAutoCommit(false);try{body.run();c.commit();}catch(Exception|Error e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
 static String refused(Connection c,Tx body){try{tx(c,body);return "ACCEPTED";}catch(Exception|Error e){return String.valueOf(e.getMessage());}}
 static final long NOW=System.currentTimeMillis(),TARGET=1_000_500L;
 static String policy(String name){return StatsR27.sha256("R31_TEST_NAMESPACE|"+name);}
 static SQLiteDatabase open(Connection connection)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(connection);for(String sql:SchemaR27.DDL)if(sql.startsWith("CREATE TABLE"))db.execSQL(sql);
  // Explicit canonical-source fixture, not a natural source revision claim.
  db.execSQL("CREATE TABLE r30_live_source_revision(singleton INTEGER PRIMARY KEY,generation INTEGER NOT NULL)");
  db.execSQL("INSERT INTO r30_live_source_revision VALUES(1,1)");
  tx(connection,()->MainlineChoiceStoreR31.ensureSchemaR31(db));return db;
 }
 /** A test-namespace policy with an ACTIVE V1 certificate, a QUALIFIED e-process and one FROZEN acting row. */
 static String certified(SQLiteDatabase db,String name,long index,boolean certificate){
  String p=policy(name);PolicySpecR27 spec=new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32)),3,PolicySpecR27.Rule.ALWAYS,0,1,1,false,.01,0,IdeaFamilyR27.meta("I09","R31_TEST_NAMESPACE",0,index,"TEST_CERTIFICATE_FIXTURE_NOT_NATURAL_EVIDENCE"));
  db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi) VALUES(?,?,1,?,?,'ACTIVE','TEST',?,?,?,'','ABI')",new Object[]{p,spec.fullJson(),new byte[80],index,NOW,NOW,TARGET-1});
  db.execSQL("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,target_rate,updated_at) VALUES(?,?,?,9,0,300,300,110,1,1,?,?,0.15,?)",new Object[]{p,index,SequentialQualificationR27.alpha(index),TARGET-1,SequentialQualificationR27.QUALIFIED,NOW});
  if(certificate)db.execSQL("INSERT INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at,gate_version) VALUES(?,'ACTIVE',?,?,?,?,?)",new Object[]{p,"{\"semanticAbi\":\"ABI\"}",StatsR27.sha256("digest|"+p),NOW-1000,NOW+3_600_000L,CertificateGateR27.GATE_VERSION});
  double[] probabilities=new double[10];Arrays.fill(probabilities,.1);probabilities[1]=.2;probabilities[4]=.15;probabilities[6]=.15;
  db.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,3,?,2,5,7,1,'FROZEN',?,?)",new Object[]{p,TARGET,"T"+TARGET,StoreR27.pack(probabilities),NOW-5000,NOW+60_000});
  return p;
 }
 static void legacy(SQLiteDatabase db,String choice){db.execSQL("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('mainline','choice',?,?)",new Object[]{"{\"choice\":\""+choice+"\"}",NOW});}
 static String digest(String p){return StatsR27.sha256("digest|"+p);}
 /** The certificate transaction's dual write: legacy projection and typed row together. */
 static void certify(SQLiteDatabase db,String p,long effective,long fence){legacy(db,p);MainlineChoiceStoreR31.appendChoice(db,p,p,CertificateGateR27.GATE_VERSION,digest(p),StatsR27.sha256("SEMANTIC_ABI|ABI"),1,effective,fence,NOW);}

 public static void main(String[] args)throws Exception{Class.forName("org.sqlite.JDBC");
  try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){SQLiteDatabase db=open(connection);
   String p=certified(db,"P",1,true);tx(connection,()->certify(db,p,TARGET,5));
   // C01#A1: before the switch the legacy projection authorises only together with the typed row, and cannot be deleted
   MainlineChoiceStoreR31.Authorization a=MainlineChoiceStoreR31.authorizedChoice(db,NOW);check(p.equals(a.policyHash)&&a.reason.equals("LEGACY_AUTHORITY_MATCH"),"LEGACY_MATCH:"+a.reason);
   MainlineBridgeSU2.Intent intent=MainlineBridgeSU2.r27Intent(db,TARGET);check(intent!=null&&intent.memberId.equals("R27:"+p)&&intent.position==3,"CERTIFIED_TEST_POLICY_REACHES_REQUEST_LANE");
   check(refused(connection,()->db.execSQL("DELETE FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'")).contains("R31_MAINLINE_LEGACY_PROJECTION_IS_STILL_AUTHORITY"),"PROJECTION_DELETE_BEFORE_SWITCH_REFUSED");
   tx(connection,()->MainlineChoiceStoreR31.switchToTypedRead(db,NOW));check("TYPED_READ".equals(MainlineChoiceStoreR31.authorityMode(db)),"SWITCHED");
   tx(connection,()->db.execSQL("DELETE FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'"));
   a=MainlineChoiceStoreR31.authorizedChoice(db,NOW);check(p.equals(a.policyHash)&&a.reason.startsWith("TYPED_AUTHORITY:"),"TYPED_AUTHORITY_AFTER_PROJECTION_DELETED");
   check(MainlineBridgeSU2.r27Intent(db,TARGET)!=null,"TYPED_ONLY_REQUEST_LANE");
   check(MainlineChoiceStoreR31.authorizedChoice(db,NOW,TARGET-1).reason.equals("WAIT_CHOICE_NOT_EFFECTIVE"),"FUTURE_CHOICE_WAITS");
   db.execSQL("UPDATE r30_live_source_revision SET generation=2");
   check(MainlineBridgeSU2.r27Intent(db,TARGET)==null,"SOURCE_CHANGE_STOPS_NEW_REAL");
   db.execSQL("UPDATE r30_live_source_revision SET generation=1");
   // C01#A3: a stale fence and a conflicting choice for the same effective period are refused
   String q=certified(db,"Q",2,true);
   check(refused(connection,()->certify(db,q,TARGET,3)).contains("R31_MAINLINE_STALE_FENCE"),"STALE_FENCE_REFUSED");
   check(refused(connection,()->certify(db,q,TARGET,5)).contains("R31_MAINLINE_CONFLICTING_CHOICE"),"CONFLICTING_SAME_PERIOD_REFUSED");
   check(refused(connection,()->db.execSQL("INSERT INTO r31_mainline_choice VALUES('x',?,?,'g','d','m',1,?,1,'ACTIVE',?)",new Object[]{q,q,TARGET+1,NOW})).contains("R31_MAINLINE_STALE_FENCE"),"STALE_FENCE_TRIGGER");
   check(refused(connection,()->db.execSQL("DELETE FROM r31_mainline_choice")).contains("R31_MAINLINE_HISTORY_IMMUTABLE"),"HISTORY_NOT_DELETABLE");
   // C01#A4 / C03#A3: expired, revoked and uncertified choices never reach REAL; frozen history is untouched
   long frozen=count(db,"SELECT count(*) FROM r27_live_frozen");String frozenRow=text(db,"SELECT hex(probabilities)||status||act FROM r27_live_frozen WHERE policy_hash=?",p);
   db.execSQL("UPDATE r27_certificate SET expires_at=? WHERE policy_hash=?",new Object[]{NOW-1,p});
   check(MainlineChoiceStoreR31.authorizedChoice(db,NOW).reason.equals("WAIT_CERTIFICATE_EXPIRED_REVOKED_OR_CHANGED")&&MainlineBridgeSU2.r27Intent(db,TARGET)==null,"EXPIRED_STOPS_NEW_REAL");
   db.execSQL("UPDATE r27_certificate SET expires_at=? WHERE policy_hash=?",new Object[]{NOW+3_600_000L,p});check(MainlineBridgeSU2.r27Intent(db,TARGET)!=null,"RENEWED");
   String choiceId=MainlineChoiceStoreR31.current(db).choiceId;tx(connection,()->MainlineChoiceStoreR31.revoke(db,choiceId,"TEST_REVOCATION",1,NOW));
   check(MainlineBridgeSU2.r27Intent(db,TARGET)==null&&count(db,"SELECT count(*) FROM r31_mainline_revocation WHERE choice_id=?",choiceId)==1,"REVOKED_STOPS_NEW_REAL");
   check(frozen==count(db,"SELECT count(*) FROM r27_live_frozen")&&frozenRow.equals(text(db,"SELECT hex(probabilities)||status||act FROM r27_live_frozen WHERE policy_hash=?",p)),"FROZEN_HISTORY_UNCHANGED");
   String s=certified(db,"SYNTHETIC_NO_CERTIFICATE",3,false);tx(connection,()->certify(db,s,TARGET+2,9));
   check(MainlineChoiceStoreR31.authorizedChoice(db,NOW).policyHash==null&&MainlineBridgeSU2.r27Intent(db,TARGET)==null,"UNCERTIFIED_NEVER_REAL");
  }
  try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){SQLiteDatabase db=open(connection);
   String p=certified(db,"P",1,true),q=certified(db,"Q",2,true);tx(connection,()->certify(db,p,TARGET,5));
   // C01#A2: a failure at any write point rolls both tables back
   String beforeLegacy=MainlineChoiceStoreR31.legacyChoice(db);long beforeTyped=count(db,"SELECT count(*) FROM r31_mainline_choice");
   for(int point=0;point<2;point++){final int at=point;
    check(refused(connection,()->{legacy(db,q);if(at==0)throw new IllegalStateException("INJECTED_AFTER_LEGACY");MainlineChoiceStoreR31.appendChoice(db,q,q,CertificateGateR27.GATE_VERSION,digest(q),"m",1,TARGET+1,6,NOW);throw new IllegalStateException("INJECTED_AFTER_TYPED");}).startsWith("INJECTED"),"FAULT_INJECTED_"+at);
    check(beforeLegacy.equals(MainlineChoiceStoreR31.legacyChoice(db))&&beforeTyped==count(db,"SELECT count(*) FROM r31_mainline_choice")&&p.equals(MainlineChoiceStoreR31.current(db).policyHash),"NO_FORK_AFTER_FAULT_"+at);
   }
   // a manufactured divergence authorises nothing, never one of the two
   tx(connection,()->legacy(db,q));
   MainlineChoiceStoreR31.Authorization a=MainlineChoiceStoreR31.authorizedChoice(db,NOW);check(a.policyHash==null&&a.reason.startsWith("WAIT_MAINLINE_DIVERGED"),"DIVERGENCE_WAITS:"+a.reason);
   check(MainlineBridgeSU2.r27Intent(db,TARGET)==null,"DIVERGED_REQUEST_LANE_WAITS");
   check(refused(connection,()->MainlineChoiceStoreR31.switchToTypedRead(db,NOW)).startsWith("R31_MAINLINE_SWITCH_REQUIRES_MATCH"),"NO_SWITCH_WHILE_DIVERGED");
   check(MainlineBridgeSU2.projectionR30(db,TARGET,NOW).sources.get("mainlineAuthorityR31").toString().contains("WAIT_MAINLINE_DIVERGED"),"EXPLANATION_SHOWS_DIVERGENCE");
  }
  System.out.println("R31_MAINLINE_AUTHORITY_HOST_PASS assertions="+assertions+" legacy_read=MATCH_ONLY projection_delete_before_switch=REFUSED typed_read=AFTER_MATCH fault_points=NO_FORK divergence=WAIT stale_fence=REFUSED conflict=REFUSED expired_revoked_uncertified=NO_NEW_REAL frozen_history=UNCHANGED scope=HOST_TEST_NAMESPACE_NOT_NATURAL_NOT_DEVICE");
 }
}
