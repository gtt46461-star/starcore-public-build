package com.ethk3.novaonepro.r952;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.*;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.json.JSONObject;
/** Independent scalar oracle and hostile receipt/schema tests against actual
 * original source rows. No manufactured admissions or prospective support. */
public final class BoundReceiptOracleReviewR80 {
 static int checks;static void ok(boolean b,String m){if(!b)throw new AssertionError(m);checks++;System.out.println("PASS "+m);}
 static String h(String s){return V51ChecksumEngine.sha256(s);}
 static long n(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static Object copy(Object v){if(v instanceof Map){Map<String,Object> m=new TreeMap<>();for(Map.Entry<?,?> e:((Map<?,?>)v).entrySet())m.put((String)e.getKey(),copy(e.getValue()));return m;}if(v instanceof List){List<Object> l=new ArrayList<>();for(Object x:(List<?>)v)l.add(copy(x));return l;}return v;}
 @SuppressWarnings("unchecked") static Map<String,Object> map(Object x){return (Map<String,Object>)x;}
 static void denied(Runnable r,String label){boolean reject=false;try{r.run();}catch(RuntimeException expected){reject=true;System.out.println("REJECT "+label+" "+expected.getMessage());}ok(reject,label);}
 static void sqlDenied(SQLiteDatabase db,String sql,String name){db.beginTransaction();try{denied(()->db.execSQL(sql),name);}finally{db.endTransaction();}}
 static List<AtomSpecCP4> specs(SQLiteDatabase db,BoundAtomLifecycleR80.Cohort c){List<AtomSpecCP4> out=new ArrayList<>();try(Cursor rows=db.rawQuery("SELECT atom_id,spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE position=?",new String[]{Integer.toString(c.ast.position)})){while(rows.moveToNext()){AtomSpecCP4 s=AtomSpecCP4.fromRegistry(rows.getString(0),rows.getString(1),rows.getString(2),rows.getString(3),rows.getInt(4),rows.getString(5),rows.getString(6));if(c.ast.graphSpec.manifest.atomSpecHashes.contains(s.specHash))out.add(s);}}return out;}
 static double oracle(SQLiteDatabase db,BoundAtomLifecycleR80.Cohort c,AtomSpecCP4 spec,String target){
  List<String> parents=BoundAtomLifecycleR80.parentsR80(db,c);boolean disagreement=spec.family.equals("STRATEGY_DISAGREEMENT_TEMPLATE");int wanted=disagreement?1:spec.mathSpec.w;List<Integer> hits=new ArrayList<>();List<Double> scores=new ArrayList<>();int[] masks=new int[2];
  for(int p=0;p<(disagreement?2:1);p++)try(Cursor rows=db.rawQuery("SELECT hit,score,n1,n2,n3 FROM "+BoundAtomLifecycleR80.SOURCE+" WHERE cohort_hash=? AND candidate_id=? AND target_issue<? ORDER BY target_issue DESC LIMIT ?",new String[]{c.hash,parents.get(p),target,Integer.toString(wanted)})){
   int count=0;while(rows.moveToNext()){if(p==0){hits.add(rows.getInt(0));scores.add(rows.getDouble(1));}masks[p]=(1<<(rows.getInt(2)-1))|(1<<(rows.getInt(3)-1))|(1<<(rows.getInt(4)-1));count++;}ok(count==wanted,"independent oracle source cardinality "+spec.family+"/"+spec.mathSpec.w+"/parent"+p);
  }
  if(disagreement)return 1-Integer.bitCount(masks[0]&masks[1])/(double)Integer.bitCount(masks[0]|masks[1]);
  if(spec.family.equals("LOSS_STREAK_TEMPLATE")){int streak=0;for(int v:hits){if(v!=0)break;streak++;}return Math.min(wanted,streak);}
  double sum=0;for(int i=0;i<hits.size();i++)sum+=hits.get(i)-(spec.family.equals("RESIDUAL_MEAN_TEMPLATE")?scores.get(i):0);
  if(spec.family.equals("RESIDUAL_MEAN_TEMPLATE"))return sum/wanted;
  if(spec.family.equals("CONFIDENCE_TEMPLATE")){double z=1.959963984540054,z2=z*z,ph=sum/wanted;return (2*wanted*ph+z2-z*Math.sqrt(z2+4*wanted*ph*(1-ph)))/(2*(wanted+z2));}
  throw new AssertionError(spec.family);
 }
 static void review(SQLiteDatabase db)throws Exception{
  BoundAtomLifecycleR80.Cohort c=BoundAtomLifecycleR80.activeR80(db);ok(c!=null&&c.ready==421,"complete original prefix exists");List<AtomSpecCP4> specs=specs(db,c);CandidateCompilerR952.Program program=CandidateCompilerR952.compile(c.ast,c.ast.graphSpec.manifest,specs);
  ok(specs.size()==13&&program.ast.graphSpec.nodes.size()==13&&program.ast.graphSpec.featureOutputIds.size()==13&&program.manifest.bindingHashes.size()==14,"typed compiler consumes all13 actual bound atoms and14 bindings");
  ok(program.manifest.requiredHistory==300&&BoundAtomLifecycleR80.MAX_WINDOW==500,"catalog declares300 actual warmup while runtime supports bounded500");
  List<String> targets=new ArrayList<>();try(Cursor r=db.rawQuery("SELECT target_issue FROM "+BoundAtomLifecycleR80.INPUT+" WHERE available=1 ORDER BY target_issue",null)){while(r.moveToNext())targets.add(r.getString(0));}ok(targets.size()==421,"exact complete prefix");
  Map<String,Object> last=null;long lastDrawAt=0;String lastTarget="";
  for(int index:new int[]{0,59,210,420}){
   String target=targets.get(index);String payload;long drawAt;try(Cursor r=db.rawQuery("SELECT i.payload,d.open_time FROM "+BoundAtomLifecycleR80.INPUT+" i JOIN draws d ON d.issue=i.target_issue WHERE i.cohort_hash=? AND i.target_issue=?",new String[]{c.hash,target})){ok(r.moveToFirst(),"original receipt and draw available");payload=r.getString(0);drawAt=r.getLong(1);}
   Map<String,Object> receipt=StrictCanonicalJson.readContextObject(payload);BoundAtomLifecycleR80.verifyOriginalR80(program,receipt,target,drawAt);Map<String,Object> atoms=map(receipt.get("atoms"));
   for(NodeSpec node:program.ast.graphSpec.nodes){AtomSpecCP4 spec=program.specById(node.source);Map<String,Object> value=map(atoms.get(node.qualifiedId()));double actual=Double.parseDouble((String)((List<?>)value.get("values")).get(0)),expected=oracle(db,c,spec,target);ok(Math.abs(actual-expected)<1e-12,"independent "+spec.family+" W"+spec.mathSpec.w+" target="+target);}
   long a=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used(),m=RuntimeMemoryGuardR811.algorithmModelBudget().used();String historical=BoundAtomLifecycleR80.extractOriginalR80(db,program,target,drawAt);Map<String,Object> row=StrictCanonicalJson.readContextObject(historical);ok("CP12_HISTORICAL_RECEIPT_R80".equals(row.get("revision")),"historical consumer selects explicit R80 original receipt revision");ok(RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used()==a&&RuntimeMemoryGuardR811.algorithmModelBudget().used()==m,"exact source verification releases all leases");last=receipt;lastDrawAt=drawAt;lastTarget=target;
  }
  final Map<String,Object> good=last;final long drawAt=lastDrawAt;final String target=lastTarget;
  String[] keys={"role","revision","cohort","selectionHash","manifestHash","astHash","registeredSource","target"};
  for(String key:keys){Map<String,Object> bad=map(copy(good));bad.put(key,key.equals("registeredSource")?target:"WRONG");denied(()->BoundAtomLifecycleR80.verifyOriginalR80(program,bad,target,drawAt),"reject receipt identity tamper "+key);}
  for(String key:new String[]{"registeredAt","sourceReadAt","capturedAt","deadlineAt"}){Map<String,Object> bad=map(copy(good));bad.put(key,key.equals("deadlineAt")?1L:drawAt+1);denied(()->BoundAtomLifecycleR80.verifyOriginalR80(program,bad,target,drawAt),"reject receipt time tamper "+key);}
  Map<String,Object> missing=map(copy(good));map(missing.get("atoms")).remove(map(missing.get("atoms")).keySet().iterator().next());denied(()->BoundAtomLifecycleR80.verifyOriginalR80(program,missing,target,drawAt),"reject missing consumed typed node");
  Map<String,Object> extra=map(copy(good));map(extra.get("atoms")).put("CP4:n127",map(extra.get("atoms")).values().iterator().next());denied(()->BoundAtomLifecycleR80.verifyOriginalR80(program,extra,target,drawAt),"reject unused typed node");
  Map<String,Object> targetLeak=map(copy(good));map(map(targetLeak.get("atoms")).values().iterator().next()).put("maxSourceIssue",target);denied(()->BoundAtomLifecycleR80.verifyOriginalR80(program,targetLeak,target,drawAt),"reject current-target source leakage");
  Map<String,Object> late=map(copy(good));map(map(late.get("atoms")).values().iterator().next()).put("availableAt",drawAt+1);denied(()->BoundAtomLifecycleR80.verifyOriginalR80(program,late,target,drawAt),"reject source unavailable at original capture");
  Map<String,Object> missingMask=map(copy(good));map(map(missingMask.get("atoms")).values().iterator().next()).put("present",Collections.singletonList(false));denied(()->BoundAtomLifecycleR80.verifyOriginalR80(program,missingMask,target,drawAt),"missing typed source cannot become zero-filled present evidence");
  long generation=c.generation;db.beginTransaction();try{db.execSQL("UPDATE "+BoundAtomLifecycleR80.COHORT+" SET generation=generation+1 WHERE cohort_hash=?",new Object[]{c.hash});ok(BoundAtomLifecycleR80.readR80(db,c.hash).generation==generation+1,"state transition executes before rollback");}finally{db.endTransaction();}ok(BoundAtomLifecycleR80.readR80(db,c.hash).generation==generation,"rollback restores lifecycle generation");
  sqlDenied(db,"UPDATE "+BoundAtomLifecycleR80.COHORT+" SET source_issue='202609230001'","cohort registration identity immutable");
  sqlDenied(db,"INSERT OR REPLACE INTO "+BoundAtomLifecycleR80.COHORT+" SELECT 'alternate-active-key',selection_json,ast,ast_hash,manifest_hash,source_issue,first_issue,registered_at,state,next_issue,ready_n,selection_issue,action_id,child_id,intent_id,generation,last_reason FROM "+BoundAtomLifecycleR80.COHORT+" LIMIT 1","active-cohort alternate unique key cannot replace existing cohort");
  db.beginTransaction();try{
   db.execSQL("INSERT INTO "+BoundAtomLifecycleR80.COHORT+" SELECT 'rolled-back-uniqueness-fixture',selection_json,ast,ast_hash,manifest_hash,source_issue,first_issue,registered_at,'RETIRED',next_issue,ready_n,selection_issue,action_id,child_id,intent_id,generation,last_reason FROM "+BoundAtomLifecycleR80.COHORT+" LIMIT 1");
   db.execSQL("INSERT INTO "+BoundAtomLifecycleR80.PARENT+" SELECT 'rolled-back-uniqueness-fixture',0,candidate_id,freeze_hash,model_hash,learner_id,learner_ast_hash,position FROM "+BoundAtomLifecycleR80.PARENT+" WHERE cohort_hash=? AND ordinal=0",new Object[]{c.hash});
   denied(()->db.execSQL("INSERT OR REPLACE INTO "+BoundAtomLifecycleR80.PARENT+" SELECT cohort_hash,1,candidate_id,freeze_hash,model_hash,learner_id,learner_ast_hash,position FROM "+BoundAtomLifecycleR80.PARENT+" WHERE cohort_hash='rolled-back-uniqueness-fixture'"),"parent candidate alternate unique key cannot replace original ordinal");
  }finally{db.endTransaction();}
  ok(n(db,"SELECT COUNT(*) FROM "+BoundAtomLifecycleR80.COHORT+" WHERE cohort_hash='rolled-back-uniqueness-fixture'")==0,"negative uniqueness fixture never becomes durable evidence");
  for(String table:new String[]{BoundAtomLifecycleR80.PARENT,BoundAtomLifecycleR80.INPUT}){sqlDenied(db,"UPDATE "+table+" SET cohort_hash=cohort_hash","immutable "+table);sqlDenied(db,"DELETE FROM "+table,"permanent original "+table);sqlDenied(db,"INSERT OR REPLACE INTO "+table+" SELECT * FROM "+table+" LIMIT 1","replace forbidden "+table);}
  sqlDenied(db,"UPDATE "+BoundAtomLifecycleR80.SOURCE+" SET score=score+.0001 WHERE rowid IN(SELECT rowid FROM "+BoundAtomLifecycleR80.SOURCE+" LIMIT 1)","source prediction immutable");
  sqlDenied(db,"UPDATE "+BoundAtomLifecycleR80.SOURCE+" SET hit=1-hit WHERE rowid IN(SELECT rowid FROM "+BoundAtomLifecycleR80.SOURCE+" LIMIT 1)","settled source label immutable");
  sqlDenied(db,"DELETE FROM "+BoundAtomLifecycleR80.SOURCE,"source originals permanent");sqlDenied(db,"INSERT OR REPLACE INTO "+BoundAtomLifecycleR80.SOURCE+" SELECT * FROM "+BoundAtomLifecycleR80.SOURCE+" LIMIT 1","source replacement forbidden");
  sqlDenied(db,"UPDATE research_atom_binding_v2 SET model_hash=model_hash","binding tuple immutable");sqlDenied(db,"DELETE FROM research_atom_binding_v2","bound original binding not reclaimed");sqlDenied(db,"INSERT OR REPLACE INTO research_atom_binding_v2 SELECT * FROM research_atom_binding_v2 LIMIT 1","binding hash replacement forbidden");
  ok(n(db,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(db,"SELECT COUNT(*) FROM trades")==0,"review produced no certificate or REAL");
 }
 public static void main(String[] a)throws Exception{try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){db.setForeignKeyConstraintsEnabled(true);review(db);JSONObject result=new JSONObject().put("status","PASS").put("checks",checks).put("scope","INDEPENDENT_ORIGINAL_RECEIPT_FOUR_FAMILY_ORACLE_REAL_SQLITE_ACTUAL_PRODUCT_HOST");Files.writeString(Path.of(a[2]),result.toString()+"\n");System.out.println(result);}}
}
