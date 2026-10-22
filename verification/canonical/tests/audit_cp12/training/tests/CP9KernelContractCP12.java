package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
import com.ethk3.novaonepro.r952.DevelopmentTrainerCP9.*;
import java.util.*;import java.nio.file.*;
public final class CP9KernelContractCP12 {
 static int assertions,restores; static long transitions;static int maxCheckpoint;
 static final String H=V51ChecksumEngine.sha256("cp9-training-fixture");
 static void ok(boolean b){assertions++;if(!b)throw new AssertionError("CP9 assertion "+assertions);}
 static void bad(Runnable r){assertions++;try{r.run();}catch(IllegalStateException|IllegalArgumentException e){return;}throw new AssertionError("accepted bad input");}
 static CandidateAstR952.Family family(ModelKind k){switch(k){case FIXED_GRAPH:return CandidateAstR952.Family.FREQUENCY;case SOFTMAX_LINEAR:return CandidateAstR952.Family.LINEAR;case OVR_LOGISTIC:return CandidateAstR952.Family.LOGISTIC;case HISTOGRAM_TREE:return CandidateAstR952.Family.TREE;case EXPERT_HEDGE:return CandidateAstR952.Family.BANDIT;default:return CandidateAstR952.Family.NEURAL;}}
 static Dataset fixture(ModelKind k,int changedValidation,int trainN){
  int f=k==ModelKind.FIXED_GRAPH||k==ModelKind.EXPERT_HEDGE?0:2,e=k==ModelKind.EXPERT_HEDGE?2:0,d=f>0?2*f:e>0?20:10;
  double[] std=new double[f];Arrays.fill(std,1);List<String> ids=f==0?Collections.emptyList():Arrays.asList("n0","n1");
  int[] tf=k==ModelKind.HISTOGRAM_TREE?new int[]{-1}:new int[0],child=tf.clone();double[] lp=new double[tf.length*10];Arrays.fill(lp,.1);
  List<ExpertRef> experts=e==0?Collections.emptyList():Arrays.asList(new ExpertRef(H,H,H,1),new ExpertRef(V51ChecksumEngine.sha256("teacher2"),H,H,1));
  ArrayList<Row> rows=new ArrayList<>();String issue="202609230001";
  for(int i=0;i<trainN+120;i++){
   if(i==trainN)issue=TargetIssueResolver.next(issue).targetIssue;
   double[] x=new double[f>0?f:d];boolean[] mask=new boolean[x.length];Arrays.fill(mask,true);int actual=i%10+1;
   if(i>=trainN&&changedValidation>0)actual=(actual+changedValidation-1)%10+1;
   if(f>0){x[0]=i%10;x[1]=Math.sin(i*.1);if(i%7==0){mask[1]=false;x[1]=0;}}
   else if(e>0){for(int a=0;a<e;a++){Arrays.fill(x,a*10,a*10+10,.05);x[a*10+(i+a*3)%10]=.55;}}
   else {if(changedValidation!=-1){Arrays.fill(x,1);x[i%10]=2;}}
   if(changedValidation==-3&&e>0){Arrays.fill(x,1e-15);x[i%10]=1-9e-15;x[10+(i+3)%10]=1-9e-15;}
   if(changedValidation==-4&&e>0){Arrays.fill(x,0);x[i%10]=1;x[10+(i+3)%10]=1;}
   if(changedValidation==-2&&f>0&&i==0){x[0]=0;mask[0]=false;}
   rows.add(new Row(issue,V51ChecksumEngine.sha256("row"+i),x,mask,actual));issue=TargetIssueResolver.next(issue).targetIssue;
  }
  Modern m=new Modern(k,family(k),d,f,e,trainN,42,rows.get(rows.size()-1).issue,H,Modern.normalizerHash(ids,new double[f],std),H,H,new double[f],std,new double[Modern.expectedWeights(k,d,f,e)],tf,new double[tf.length],child,child,lp,experts);
  return new Dataset(m,H,H,ids,f==0?Collections.emptyList():Arrays.asList("REJECT","MASK"),rows,trainN);
 }
 static State run(Dataset d,boolean restart){
  State s=DevelopmentTrainerCP9.start(d);int n=0;boolean[] seen=new boolean[Phase.values().length];
  while(!s.complete()){
   Phase before=s.phase();DevelopmentTrainerCP9.step(d,s);transitions++;n++;ok(n<2000000);
   boolean special=s.phase()!=before||s.pendingBatchRows()==1||s.pendingBatchRows()==31||!seen[s.phase().ordinal()];seen[s.phase().ordinal()]=true;
   if(restart&&(special||n%791==0)){byte[] bytes=DevelopmentTrainerCP9.checkpoint(d,s);maxCheckpoint=Math.max(maxCheckpoint,bytes.length);State old=s;s=DevelopmentTrainerCP9.restore(d,bytes);restores++;ok(Arrays.equals(bytes,DevelopmentTrainerCP9.checkpoint(d,s)));ok(s.phase()==old.phase());}
  }
  return s;
 }
 public static void main(String[] args)throws Exception{
  for(ModelKind k:ModelKind.values()){
   Dataset d=fixture(k,0,300);State a=run(d,false),b=run(d,true);Model ma=DevelopmentTrainerCP9.finish(d,a,H),mb=DevelopmentTrainerCP9.finish(d,b,H);
   ok(ma.payload().equals(mb.payload()));ok(a.orderedPredictionHash().equals(b.orderedPredictionHash()));ok(a.validationCE()==b.validationCE());ok(a.validationBrier()==b.validationBrier());ok(a.validationHits()==b.validationHits());ok(a.selectedEpoch()==b.selectedEpoch());ok(ma.updates==300);
   if(k==ModelKind.HISTOGRAM_TREE){ok(ma.modern().treeFeatures().length>1);ok(ma.modern().treeFeatures().length<=31);}
   if(k==ModelKind.EXPERT_HEDGE)ok(ma.modern().weights()[0]>ma.modern().weights()[1]);
   byte[] bytes=DevelopmentTrainerCP9.checkpoint(d,b);byte[] corrupt=bytes.clone();corrupt[100]^=1;bad(()->DevelopmentTrainerCP9.restore(d,corrupt));bad(()->DevelopmentTrainerCP9.restore(d,Arrays.copyOf(bytes,bytes.length-1)));bad(()->DevelopmentTrainerCP9.restore(fixture(k,1,300),bytes));
   System.out.println(k+" epochs="+a.executedEpochs()+" CE="+a.validationCE()+" checkpoint="+bytes.length);
  }
  Dataset d=fixture(ModelKind.SOFTMAX_LINEAR,0,300),changed=fixture(ModelKind.SOFTMAX_LINEAR,1,300);State a=DevelopmentTrainerCP9.start(d),b=DevelopmentTrainerCP9.start(changed);
  while(a.phase()!=Phase.VALIDATE){DevelopmentTrainerCP9.step(d,a);DevelopmentTrainerCP9.step(changed,b);}
  ok(a.normalizerCount(0)==300);ok(a.normalizerCount(1)==257);ok(a.normalizerMean(0)==b.normalizerMean(0));ok(a.normalizerMean(1)==b.normalizerMean(1));ok(a.optimizerSteps()==10);
  // Max sample bound uses exactly 1200 distinct training rows, not epoch*N.
  Dataset largest=fixture(ModelKind.SOFTMAX_LINEAR,0,1200);State last=run(largest,true);ok(DevelopmentTrainerCP9.finish(largest,last,H).updates==1200);
  Dataset zero=fixture(ModelKind.FIXED_GRAPH,-1,300);State uniform=run(zero,false);ok(Math.abs(uniform.validationCE()-Math.log(10))<1e-12);ok(Math.abs(uniform.validationBrier()-.09)<1e-12);bad(()->fixture(ModelKind.SOFTMAX_LINEAR,-2,300));
  Dataset extreme=fixture(ModelKind.EXPERT_HEDGE,-3,1200);State hedge=run(extreme,true);Model extremeModel=DevelopmentTrainerCP9.finish(extreme,hedge,H);ok(extremeModel.modern().weights()[1]<-700);ok(hedge.validationCE()>0&&hedge.validationCE()<1e-12);bad(()->fixture(ModelKind.EXPERT_HEDGE,-4,1200));
  MemoryBudgetCP4 memory=new MemoryBudgetCP4(3L<<20);byte[] pending;
  ResearchTrainingHookRegistryR952.PreparedNumericsCP9 slice=ResearchTrainingHookRegistryR952.prepareNumericsCP9(d,null,0,memory);
  ok(memory.used()==DevelopmentTrainerCP9.estimatedNumericsBytesCP12(d));ok(!slice.complete());pending=slice.checkpoint();ok(slice.queueCursor().endsWith(slice.checkpointHash));slice.close();slice.close();ok(memory.used()==0);bad(()->slice.checkpoint());
  try(ResearchTrainingHookRegistryR952.PreparedNumericsCP9 resumed=ResearchTrainingHookRegistryR952.prepareNumericsCP9(d,pending,System.nanoTime()+10_000_000_000L,memory)){ok(resumed.complete());ok(resumed.model(H).updates==300);ok(resumed.validationCE()>0);}
  ok(memory.used()==0);pending[20]^=1;byte[] broken=pending;bad(()->ResearchTrainingHookRegistryR952.prepareNumericsCP9(d,broken,0,memory));ok(memory.used()==0);
  String result="{\"status\":\"PASS\",\"assertions\":"+assertions+",\"checkpoint_restores\":"+restores+",\"numerical_transitions\":"+transitions+",\"model_kinds\":6,\"maximum_checkpoint_bytes\":"+maxCheckpoint+",\"scope\":\"Pure numerical kernel and persisted cursor codec; not SCDEV extraction, queue/dispatcher, Android or device integration\"}";
  Files.writeString(Path.of(args[0]),result+"\n");System.out.println(result);
 }
}
