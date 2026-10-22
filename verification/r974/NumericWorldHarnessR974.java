package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;
import java.util.*;
/** Synthetic numerical tests against independent oracles, not future win-rate claims. */
public final class NumericWorldHarnessR974 {
 static int checks;static void check(boolean v,String n){if(!v)throw new AssertionError(n);checks++;System.out.println("PASS "+n);}
 static void near(double a,double b,double eps,String n){check(Math.abs(a-b)<=eps,n+" error="+Math.abs(a-b));}
 static void rejects(Runnable r,String n){try{r.run();throw new AssertionError("ACCEPTED "+n);}catch(IllegalArgumentException|IllegalStateException expected){check(true,n);}}
 public static void main(String[] args){
  double[] x={.055,.065,.075,.085,.095,.105,.115,.125,.135,.145};
  CandidateCompilerR952.Model initial=CandidateCompilerR952.Model.initial();double[] params=initial.parameters();
  for(int i=80;i<params.length;i++)params[i]=.02*Math.cos(i*.73);
  CandidateCompilerR952.Model model=CandidateCompilerR952.Model.fromParameters(100,params);
  double[] analytic=model.lossGradient(x,7);double max=0;
  for(int i=0;i<params.length;i++){
   double[] a=params.clone(),b=params.clone();a[i]+=1e-6;b[i]-=1e-6;
   double numerical=(-Math.log(CandidateCompilerR952.Model.fromParameters(100,a).probabilities(x)[6])+Math.log(CandidateCompilerR952.Model.fromParameters(100,b).probabilities(x)[6]))/2e-6;
   max=Math.max(max,Math.abs(numerical-analytic[i]));near(numerical,analytic[i],2e-8,"GRADIENT_"+i);
  }
  check(analytic.length==178,"ALL_178_PARAMETERS_CHECKED");
  check(model.update(x,7).probabilities(x)[6]>model.probabilities(x)[6],"NUMERIC_UPDATE_REDUCES_THIS_LABEL_LOSS");
  check(model.hash().equals(CandidateCompilerR952.Model.parse(model.payload()).hash()),"NEURAL_PAYLOAD_REPLAY_EXACT");
  double[] oldBias={.01,.02,.03,.04,.05,-.01,-.02,-.03,-.04,-.05};String old="R958_FREQUENCY_SOFTMAX_V1|123|"+CandidateCompilerR952.encode(oldBias);
  CandidateCompilerR952.Model legacy=CandidateCompilerR952.Model.parse(old);
  check(legacy.payload().equals(old)&&!legacy.isNeural(),"OLD_PAYLOAD_REMAINS_OLD");
  CandidateCompilerR952.Model upgraded=legacy.upgradeForTraining();
  for(int i=0;i<10;i++)near(legacy.probabilities(x)[i],upgraded.probabilities(x)[i],1e-15,"PROSPECTIVE_MIGRATION_SAME_OUTPUT_"+i);
  check(upgraded.updates==123&&upgraded.isNeural(),"MIGRATION_KEEPS_ONLY_TRAINED_PARAMETERS_NOT_EVIDENCE");
  params[0]=3;check(model.parameters()[0]!=3,"PARAMETERS_ARE_DEFENSIVE_COPY");
  rejects(()->CandidateCompilerR952.Model.parse("UNKNOWN|0|0"),"UNKNOWN_MODEL_VERSION_REJECTED");
  List<Models.Draw> history=new ArrayList<Models.Draw>();
  for(int i=0;i<80;i++){Models.Draw d=new Models.Draw();d.issue=String.format(java.util.Locale.US,"20260920%04d",300-i);for(int n=0;n<10;n++)d.numbers[n]=(n+i)%10+1;history.add(d);}
  for(CandidateAstR952.Operator op:new CandidateAstR952.Operator[]{CandidateAstR952.Operator.POSITION_FREQUENCY,CandidateAstR952.Operator.RECENCY_BLEND}){
   CandidateAstR952 ast=new CandidateAstR952(op,1,60);WorldFeatureComputationR974.Result world=WorldFeatureComputationR974.compute(ast,history);
   double[] oracle=new double[10];Arrays.fill(oracle,1);for(int i=0;i<60;i++)oracle[history.get(i).numbers[0]-1]+=op==CandidateAstR952.Operator.RECENCY_BLEND?1.0/(1.0+i/20.0):1.0;
   double total=0;for(double v:oracle)total+=v;for(int i=0;i<10;i++)near(world.values()[i],oracle[i]/total,0,"LEGACY_FEATURE_BIT_PARITY_"+op+"_"+i);
   check(world.trace.contains("lagPairs=59")&&world.trace.contains("transitionCounts=")&&world.trace.contains("NOT_CAUSAL_PROOF"),"ACTUAL_MATRIX_TRACE_"+op);
  }
  CandidateAstR952 a=new CandidateAstR952(CandidateAstR952.Operator.MARKOV_TRANSITION,1,60),b=new CandidateAstR952(CandidateAstR952.Operator.CROSS_POSITION_LAG,1,60);
  CandidateAstR952 fusion=CandidateAstR952.fuse(a,b,25);
  check(CandidateAstR952.parse(fusion.canonical).structuralHash.equals(fusion.structuralHash),"FUSION_PARSE_REPLAY");
  double[] left=WorldFeatureComputationR974.compute(a,history).values(),right=WorldFeatureComputationR974.compute(b,history).values(),both=WorldFeatureComputationR974.compute(fusion,history).values();
  for(int i=0;i<10;i++)near(both[i],.25*left[i]+.75*right[i],1e-15,"TWO_PARENTS_ACTUALLY_CONSUMED_"+i);
  check(WorldFeatureComputationR974.compute(fusion,history).trace.contains("L{")&&WorldFeatureComputationR974.compute(fusion,history).trace.contains("R{"),"BOTH_PARENT_COMPUTATION_RECEIPTS");
  rejects(()->CandidateAstR952.fuse(a,new CandidateAstR952(CandidateAstR952.Operator.RECENCY_BLEND,2,60),50),"FUSION_DIFFERENT_POSITION_REJECTED");
  rejects(()->CandidateAstR952.parse("AST2(FUSE,W=5,L="+a.canonical+",R="+b.canonical+")"),"FUSION_UNSUPPORTED_WEIGHT_REJECTED");
  Collections.reverse(history);rejects(()->WorldFeatureComputationR974.compute(a,history),"LOOKAHEAD_ORDER_REJECTED");Collections.reverse(history);
  history.remove(2);WorldFeatureComputationR974.Result gap=WorldFeatureComputationR974.compute(a,history);check(gap.trace.contains("skippedGaps=1"),"GAP_NOT_FALSE_CONTIGUOUS_CAUSAL_PAIR");
  check(Integer.bitCount(CandidateCompilerR952.top3(model.probabilities(x)))==3,"FIXED_K3");
  System.out.println("NUMERIC_WORLD_CHECKS="+checks+"|MAX_GRADIENT_ERROR="+max+"|FUTURE_WIN_RATE=NOT_MEASURED");
 }
}
