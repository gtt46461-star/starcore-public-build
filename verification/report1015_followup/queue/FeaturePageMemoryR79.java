package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.*;import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
import java.util.*;import java.io.*;import java.nio.file.*;import java.lang.reflect.*;
/** Near-limit maximal AST fixture, actual production classes, no checkpoint or evidence writes. */
public final class FeaturePageMemoryR79 {
 static Map<String,Object> map(Object...v){return FeaturePageParityR79.map(v);}
 static void ok(boolean b,String why){if(!b)throw new AssertionError(why);}
 static CandidateAstR952 ast(List<AtomSpecCP4> atoms){
  List<Object> nodes=new ArrayList<>();List<String> parents=new ArrayList<>();TreeSet<String>hashes=new TreeSet<>();int required=0;
  for(int i=0;i<127;i++){AtomSpecCP4 a=atoms.get(i);if(i<10)parents.add("n"+i);hashes.add(a.specHash);required=Math.max(required,a.minimumHistory);nodes.add(map("id","n"+i,"operator","ATOM","outputType","DOUBLE","atomId",a.atomId,"parents",Collections.emptyList(),"parameters",Collections.emptyList(),"stage","FROZEN_PREDRAW","unit","dimensionless"));}
  nodes.add(map("id","n127","operator","PACK10","outputType","SCORE_VECTOR10","atomId","","parents",parents,"parameters",Collections.emptyList(),"stage","FROZEN_PREDRAW","unit","dimensionless"));
  Map<String,Object>fm=map("revision","CP4_FEATURE_1","graphHash",V51ChecksumEngine.sha256("placeholder"),"orderedFeatureIds",Collections.emptyList(),"featureTypes",Collections.emptyList(),"requiredHistory",required,"atomSpecHashes",new ArrayList<>(hashes),"bindingHashes",Collections.emptyList(),"matrixSpecRefs",Collections.emptyList(),"normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","missingPolicyPerFeature",Collections.emptyList(),"inputDimension",10,"estimatedStateBytes",8L<<20);
  Map<String,Object>root=map("compilerRevision","AST3_CP4_1","experts",Collections.emptyList(),"family","FREQUENCY","featureManifest",fm,"featureOutputIds",Collections.emptyList(),"game",V51Repository.GAME_CODE,"inputDimension",10,"modelKind","FIXED_GRAPH","nodes",nodes,"normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","outputId","n127","position",1,"version",3);
  fm.put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(root));return CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));
 }
 @SuppressWarnings("unchecked")public static void main(String[]args)throws Exception{
  Map<String,List<AtomSpecCP4>>groups=new TreeMap<>();try(BufferedReader r=Files.newBufferedReader(Path.of(args[0]))){String s;while((s=r.readLine())!=null){AtomSpecCP4 a=AtomSpecCP4.fromAssetLine(s);if(!a.requiresBinding&&a.position==1&&a.transform.equals("IDENTITY")&&a.outputType.equals("SCALAR")&&a.minimumHistory<=4000)groups.computeIfAbsent(a.family,k->new ArrayList<>()).add(a);}}
  List<AtomSpecCP4>atoms=new ArrayList<>(groups.get("FREQUENCY").subList(0,10));Set<String>used=new HashSet<>();for(AtomSpecCP4 a:atoms)used.add(a.atomId);for(int i=0;atoms.size()<127;i++)for(List<AtomSpecCP4>g:groups.values())if(i<g.size()&&atoms.size()<127&&used.add(g.get(i).atomId))atoms.add(g.get(i));
  CandidateAstR952 ast=ast(atoms);Program program=CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,atoms);long rawBytes=4096;for(NodeSpec n:program.ast.graphSpec.typedGraph.orderedNodes)rawBytes+=512L+32L*n.shape.elements();long atomBytes=CandidateCompilerR952.estimateStateBytes(ast,atoms)-(rawBytes-4096);
  List<Models.Draw>draws=new ArrayList<>();try(BufferedReader r=Files.newBufferedReader(Path.of(args[1]))){String s;while((s=r.readLine())!=null){String[]v=s.split("\t");Models.Draw d=new Models.Draw();d.issue=v[0];d.openTime=Long.parseLong(v[1]);for(int i=0;i<10;i++)d.numbers[i]=Integer.parseInt(v[i+2]);draws.add(d);}}
  DevelopmentInputCP9.Header h=new DevelopmentInputCP9.Header(StrictCanonicalJson.writeContext(map("revision","SCDEV1","game","PK10","build","R79_EXPLICIT_MEMORY_TEST","sourceManifestHash",V51ChecksumEngine.sha256("REPORT_DRAWS_EXPLICIT_FIXTURE"),"scheduleRevision","TARGET_ISSUE_RESOLVER_R980","bindings",Collections.emptyList())));
  ByteArrayOutputStream b=new ByteArrayOutputStream();DevelopmentInputCP9.Receipt receipt=DevelopmentInputCP9.write(b,h,draws);MemoryBudgetCP4 inputBudget=new MemoryBudgetCP4(4L<<20);int rows=0,yields=0;
  Method changed=DevelopmentProductionCP12.class.getDeclaredMethod("dataAtoms",Program.class,DevelopmentInputCP9.Input.class,int.class,HistoricalTrainingReceiptCP12.Row.class,DevelopmentProductionCP12.FeatureAtomHistoryR79.class);changed.setAccessible(true);
  try(DevelopmentInputCP9.Input input=DevelopmentInputCP9.read(new ByteArrayInputStream(b.toByteArray()),receipt.byteCount,receipt.inputHash,h,inputBudget)){
   for(long limit:new long[]{atomBytes/2,atomBytes-1,atomBytes,atomBytes+rawBytes-1,atomBytes+rawBytes,8L<<20}){
    MemoryBudgetCP4 oldBudget=new MemoryBudgetCP4(limit),pageBudget=new MemoryBudgetCP4(limit);
    for(int page=0;page<2;page++){try(DevelopmentProductionCP12.FeatureAtomHistoryR79 cache=new DevelopmentProductionCP12.FeatureAtomHistoryR79(input,1,pageBudget)){
     for(int target=4900+page*16;target<4916+page*16;target++){
      HistoricalTrainingReceiptCP12.Row bound=HistoricalTrainingReceiptCP12.readFor(null,program,Collections.emptyMap(),input.issue(target),input.openTime(target));
      Map<String,Value>x=FeaturePageParityR79.old(program,input,target,bound,oldBudget);Map<String,Value>y=(Map<String,Value>)changed.invoke(null,program,input,target,bound,cache);
      ok(x.keySet().equals(y.keySet()),"keys");for(String k:x.keySet())ok(x.get(k).valueHash.equals(y.get(k).valueHash)&&x.get(k).sourceHash.equals(y.get(k).sourceHash),"masks/values "+k);
      DevelopmentTrainerCP9.Row rx=WorldFeatureComputationR974.rawTrainingRowCP11(program,input,target,receipt.inputHash,x,bound.experts,bound.hash,oldBudget);
      if(target==4900)System.out.println("OLD_RAW_SUCCESS limit="+limit+" cached="+pageBudget.used()+" atomBytes="+atomBytes+" rawBytes="+rawBytes);
      DevelopmentTrainerCP9.Row ry=WorldFeatureComputationR974.rawTrainingRowCP11(program,input,target,receipt.inputHash,y,bound.experts,bound.hash,pageBudget);
      ok(rx.inputHash.equals(ry.inputHash)&&Arrays.equals(DevelopmentProductionCP12.encode(rx.valuesCopy(),rx.presentCopy()),DevelopmentProductionCP12.encode(ry.valuesCopy(),ry.presentCopy())),"raw payload");rows++;
     }
     if(page==0){ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(4000);RealtimePressureGateR811.beginAnalysis();
      try{ResearchWorkBudgetR927.checkpoint();throw new AssertionError("expected P0 yield");}finally{RealtimePressureGateR811.endAnalysis();ResearchWorkBudgetR927.leave(scope);}}
    }catch(ResearchWorkBudgetR927.Yield expected){ok(page==0&&expected.getMessage().contains("REALTIME_PREEMPTED"),"P0 yield reason");yields++;}
    ok(oldBudget.used()==0&&pageBudget.used()==0,"release on actual P0/page boundary");
    }

   }
  }ok(inputBudget.used()==0,"input release");System.out.println("{\"status\":\"PASS\",\"max_ast_nodes\":128,\"atom_nodes\":127,\"families\":"+groups.size()+",\"atom_state_bytes\":"+atomBytes+",\"raw_row_bytes\":"+rawBytes+",\"rows\":"+rows+",\"p0_yields\":"+yields+",\"scope\":\"ACTUAL_PRODUCT_CONSTRAINED_MEMORY_FEATURE_PARITY\"}");
 }
}
