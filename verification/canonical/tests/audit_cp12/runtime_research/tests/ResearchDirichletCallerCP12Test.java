package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.io.*;import java.util.*;import java.lang.reflect.*;
public final class ResearchDirichletCallerCP12Test {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
 static void policy(Method method,String kind,int n,double ece,double js,double residual,Double latency,boolean eligible,String expected)throws Exception{ok(method.invoke(null,kind,n,ece,js,residual,latency,eligible).equals(expected));}
 public static void main(String[] args)throws Exception {
  Method select=ResearchFeedbackCP12.class.getDeclaredMethod("primaryMutationOperatorCP12",String.class,int.class,double.class,double.class,double.class,Double.class,boolean.class);select.setAccessible(true);
  // Exercise the actual prepareFinding caller's problem type; the retired test
  // alias hid the production CALIBRATION_ERROR branch mismatch before R75.
  policy(select,"CALIBRATION_ERROR",19,.2,.5,.5,1000.0,true,"NONE");
  policy(select,"CALIBRATION_ERROR",120,.05,Double.NaN,.1,701.0,true,"MODEL_STRUCTURE_MUTATION");
  policy(select,"CALIBRATION_ERROR",200,.05,.11,.1,null,true,"REGIME_GATE_MUTATION");
  policy(select,"CALIBRATION_ERROR",120,.05,Double.NaN,.1,null,true,"HYPERPARAM_MUTATION");
  policy(select,"CALIBRATION_ERROR",120,.05,Double.NaN,.1,null,false,"WEIGHT_MUTATION");
  policy(select,"CALIBRATION_ERROR",119,.05,Double.NaN,.01,null,true,"SUBTREE_MUTATION");
  policy(select,"LOSS_STREAK",120,.05,Double.NaN,.01,null,true,"SUBTREE_MUTATION");
  List<AtomSpecCP4> specs=new ArrayList<>();try(BufferedReader in=Files.newBufferedReader(Path.of(args[0]),StandardCharsets.UTF_8)){for(String line;(line=in.readLine())!=null;){AtomSpecCP4 s=AtomSpecCP4.fromAssetLine(line);if(s.position==1&&s.mathSpec.w==50&&(s.family.equals("MARKOV_SCORE")&&s.mathSpec.digit==0||s.family.equals("FREQUENCY")&&s.mathSpec.digit==0||s.family.equals("ENTROPY")))specs.add(AtomSpecCP4.fromRegistry(s.atomId,s.specHash,s.canonicalSpec,s.outputType,s.minimumHistory,s.missingPolicy,"OBSERVING"));}}
  SQLiteDatabase db=new SQLiteDatabase(args[1],args[2]);try{
   for(AtomSpecCP4 s:specs)db.execSQL("INSERT INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state)VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{s.atomId,s.family,s.version,s.canonicalSpec,s.position,s.outputType,s.minimumHistory,s.specHash,s.missingPolicy,s.state});
   Method eligibility=ResearchFeedbackCP12.class.getDeclaredMethod("dirichletEligibilityCP12",SQLiteDatabase.class,CandidateAstR952.class,long.class);eligibility.setAccessible(true);
   AtomSpecCP4 old=MutationOperatorsCP12Test.find(specs,"MARKOV_SCORE",2,.5);CandidateAstR952 parent=MutationOperatorsCP12Test.ast(MutationOperatorsCP12Test.root(old,specs,false));
   @SuppressWarnings("unchecked")Map<String,String> proof=(Map<String,String>)eligibility.invoke(null,db,parent,Long.MAX_VALUE);ok(proof.get("status").equals("REGISTERED_EXACT_PAIR"));ok(proof.get("original_spec_hash").equals(old.specHash));ok(proof.get("alternate_spec_hash").equals(MutationOperatorsCP12Test.find(specs,"MARKOV_SCORE",2,1).specHash));
   db.execSQL("UPDATE research_atom_spec_v2 SET state='RETIRED' WHERE spec_hash=?",new Object[]{proof.get("alternate_spec_hash")});@SuppressWarnings("unchecked")Map<String,String> retired=(Map<String,String>)eligibility.invoke(null,db,parent,Long.MAX_VALUE);ok(retired.get("status").equals("NO_REGISTERED_EXACT_DIRICHLET_PAIR"));
   @SuppressWarnings("unchecked")Map<String,String> budget=(Map<String,String>)eligibility.invoke(null,db,parent,0L);ok(budget.get("status").equals("UNKNOWN_DIRICHLET_TIME_BUDGET"));
   AtomSpecCP4 frequency=MutationOperatorsCP12Test.find(specs,"FREQUENCY",1,.5);CandidateAstR952 matrix=MutationOperatorsCP12Test.ast(MutationOperatorsCP12Test.root(frequency,specs,true));@SuppressWarnings("unchecked")Map<String,String> mat=(Map<String,String>)eligibility.invoke(null,db,matrix,Long.MAX_VALUE);ok(mat.get("status").equals("NO_CONSUMED_MARKOV"));
  }finally{db.shutdown();}
  System.out.println("{\"status\":\"PASS\",\"assertions\":"+checks+",\"scope\":\"Actual Research caller primary policy: hold/latency/drift/eligibleformalCAL/weight/subtree priority; actualSQLite registered original+exact alternate alpha/spec hashes, retiredpair refusal, timeBudget UNKNOWN and matrix bound exclusion. Operator and reason are inserted into Finding evidence before hash/protocol construction.\"}");
 }
}
