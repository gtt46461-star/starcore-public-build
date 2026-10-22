package com.ethk3.novaonepro;
import java.lang.reflect.Field;
/** Actual VM and candidate code, synthetic inputs; no database and no future claims. */
public final class GenesisResumeHarnessR967 {
 static int checks;static void check(boolean v,String name){if(!v)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 static String hash(String s){return V51ChecksumEngine.sha256(s);}
 public static void main(String[] args)throws Exception{
  Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);sun.misc.Unsafe u=(sun.misc.Unsafe)f.get(null);
  DbStore store=(DbStore)u.allocateInstance(DbStore.class);
  String base="202608300915",target="202608300916",hist=hash("h"),input=hash("surface");
  int[] atoms=new int[12];int used=0;boolean[] groups=new boolean[12];
  for(int i=0;i<GenesisSchemaRegistryR915.atoms().size()&&used<12;i++)
   if(GenesisSchemaRegistryR915.atoms().get(i).predrawComputable()&&(!groups[i/20]||used>=6)){atoms[used++]=i;groups[i/20]=true;}
  GenesisAssetGenomeR915 genome=new GenesisAssetGenomeR915("AF01",3,"","HOST_ONLY","202608300914",GenesisAssetGenomeR915.OutputType.MATRIX_FIELD,atoms,AlgorithmGenomeR908.seed(202608300917L,3,"TEST"));
  GenesisAssetVmR915.State state=new GenesisAssetVmR915.State();String before=state.encode();
  V51Models.PositionAction[] actions=new V51Models.PositionAction[1200];
  final GenesisAssetVmR915.PredrawContext[] contexts=new GenesisAssetVmR915.PredrawContext[1200];
  for(int p=1;p<=10;p++)for(int a=1;a<=8;a++)for(int b=a+1;b<=9;b++)for(int c=b+1;c<=10;c++){
   int i=K3ActionSpaceEngine.globalOrdinal(p,a,b,c);
   int fourth=1;while(fourth==a||fourth==b||fourth==c)fourth++;
   actions[i]=new V51Models.PositionAction(p,a,b,c,fourth,.4,.67,.02,.01,.38,.8,new double[32],"HOST",i+1,hash("a"+i));
   double[] x=new double[240];for(int j=0;j<x.length;j++)x[j]=((j*29+i*17)%997)/996d;
   contexts[i]=new GenesisAssetVmR915.PredrawContext(base,target,hist,input,384,p,a,b,c,i,x);
  }
  final int[] built={0};GenesisScanR967.ContextFactory factory=new GenesisScanR967.ContextFactory(){
   public GenesisAssetVmR915.PredrawContext build(int i){built[0]++;return contexts[i];}
  };
  GenesisScanR967.Surface surface=GenesisScanR967.acquireSurface(store,"HOST",base,target,input,hist,384,0,actions);
  check(!surface.prepare(factory,0L)&&surface.builtCount()==0,"EXPIRED_CONTEXT_NO_PROGRESS");
  int tries=0,last=0;
  while(!surface.prepare(factory,TimeSourceR811.monoNowMs()+17)){
   check(surface.builtCount()>=last,"CONTEXT_CURSOR_MONOTONE");last=surface.builtCount();
   if(++tries>1000)throw new AssertionError("CONTEXT_STALL");
   check(surface==GenesisScanR967.acquireSurface(store,"HOST",base,target,input,hist,384,0,actions.clone()),"STABLE_HANDLE_NEW_ACTION_ARRAY");
  }
  check(tries>0&&built[0]==1200,"CONTEXT_FACTORY_EXACTLY_1200_NOT_EVERY_SLICE");
  check(surface.prepare(factory,Long.MAX_VALUE)&&built[0]==1200,"READY_SURFACE_NO_REBUILD");
  // Independent whole-scan oracle BEFORE mutating caller-owned context arrays.
  int best=-1;double bestQ=-1;CandidateExecutionAuditR932.Builder full=new CandidateExecutionAuditR932.Builder();
  for(int i=0;i<1200;i++){
   GenesisAssetVmR915.AssetValue v=GenesisAssetVmR915.evaluateCompact(genome,state,contexts[i]);
   double q=v.primary*(.65+.35*v.stability)-.12*v.risk;full.add(i,q,q,i);if(best<0||q>bestQ){best=i;bestQ=q;}
  }
  contexts[0].atoms[0]=.999;contexts[0]=contexts[1]; // Must not mutate prepared Surface.
  GenesisScanR967 scan=surface.scan(genome,state,false);
  check(!scan.advance(genome,state,0L)&&scan.cursor==0,"EXPIRED_SCAN_NO_PROGRESS");
  tries=0;last=0;
  while(!scan.advance(genome,state,TimeSourceR811.monoNowMs()+19)){
   check(scan.cursor>=last,"SCAN_CURSOR_MONOTONE");last=scan.cursor;if(++tries>1000)throw new AssertionError("SCAN_STALL");
   check(surface.scan(genome,state,false)==scan,"SAME_STATE_SAME_SCAN");
  }
  check(tries>0&&scan.cursor==1200,"SLICED_FULL_SCAN");
  check(scan.best==best&&Double.compare(scan.bestScore,bestQ)==0,"ACTUAL_VM_INDEPENDENT_SELECTION_PARITY");
  CandidateExecutionAuditR932.Proof a=scan.audit.finish(scan.best,scan.programBefore,genome.canonical(),scan.stateBefore,state.encode(),scan.replayState.encode());
  CandidateExecutionAuditR932.Proof b=full.finish(best,genome.canonical(),genome.canonical(),before,state.encode(),before);
  check(a.scoreHash.equals(b.scoreHash)&&a.rankingHash.equals(b.rankingHash),"FULL1200_SCORE_RANK_HASH_PARITY");
  check(before.equals(state.encode()),"SCORE_DOES_NOT_TRAIN");
  GenesisAssetVmR915.PredrawContext selected=scan.selectedContext();selected.atoms[0]=.777;
  check(scan.selectedContext().atoms[0]!=.777,"SELECTED_CONTEXT_DEFENSIVE_COPY");
  GenesisScanR967 proxy=surface.scan(genome,state,true);
  check(proxy.advance(genome,state,Long.MAX_VALUE)&&proxy.audit==null&&proxy.cursor==1200,"PROXY120_NO_FULL_PROOF");
  surface.release(genome.assetId,true);check(proxy!=surface.scan(genome,state,true),"RELEASE_SCAN");
  state.updates++;check(surface.scan(genome,state,false)!=scan,"CHANGED_LEARNER_STATE_NEW_SCAN");
  try{scan.advance(genome,state,Long.MAX_VALUE);throw new AssertionError("state accepted");}catch(IllegalStateException expected){check(expected.getMessage().contains("STATE_DRIFT"),"OLD_SCAN_REJECTS_CHANGED_STATE");}state.updates--;
  GenesisScanR967.Surface source2=GenesisScanR967.acquireSurface(store,"OTHER",base,target,input,hist,384,0,actions);
  check(source2!=surface&&surface==GenesisScanR967.acquireSurface(store,"HOST",base,target,input,hist,384,0,actions),"TWO_SOURCE_SURFACES_COEXIST");
  GlobalResearchScheduleR932.releaseCompleted(store,target);
  try{surface.prepare(factory,Long.MAX_VALUE);throw new AssertionError("retired accepted");}catch(IllegalStateException expected){check(expected.getMessage().contains("RETIRED"),"GLOBAL_TARGET_CLEANUP_RETIRES_SCAN");}
  check(surface!=GenesisScanR967.acquireSurface(store,"HOST",base,target,input,hist,384,0,actions),"TARGET_RELEASE_NEW_SURFACE");
  System.out.println("GENESIS_V4_PASS checks="+checks+"; actual_vm=YES; device=NOT_RUN; database=NOT_RUN; future_created=0");
 }
}