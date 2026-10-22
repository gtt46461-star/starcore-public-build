package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.*;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
import java.util.*;import java.io.*;import java.nio.file.*;import java.lang.reflect.*;
/** Original report draws; independent old dataAtoms implementation versus actual page method.
 * This only recomputes DEV input features. It does not insert Future or evidence rows. */
public final class FeaturePageParityR79 {
 static int checks,rows;static long oldNs,newNs;
 static void ok(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static Map<String,Object> map(Object...kv){Map<String,Object>m=new TreeMap<>();for(int i=0;i<kv.length;i+=2)m.put((String)kv[i],kv[i+1]);return m;}
 static CandidateAstR952 ast(AtomSpecCP4 atom){
  List<Object>nodes=new ArrayList<>();List<String>parents=new ArrayList<>();for(int i=0;i<10;i++){parents.add("n"+i);nodes.add(map("id","n"+i,"operator",i==0&&atom!=null?"ATOM":"CONSTANT","outputType","DOUBLE","atomId",i==0&&atom!=null?atom.atomId:"","parents",Collections.emptyList(),"parameters",i==0&&atom!=null?Collections.emptyList():Collections.singletonList(Double.toHexString(i+1)),"stage","FROZEN_PREDRAW","unit","dimensionless"));}
  nodes.add(map("id","n10","operator","PACK10","outputType","SCORE_VECTOR10","atomId","","parents",parents,"parameters",Collections.emptyList(),"stage","FROZEN_PREDRAW","unit","dimensionless"));
  Map<String,Object>fm=map("revision","CP4_FEATURE_1","graphHash",V51ChecksumEngine.sha256("placeholder"),"orderedFeatureIds",Collections.emptyList(),"featureTypes",Collections.emptyList(),"requiredHistory",atom==null?0:atom.minimumHistory,"atomSpecHashes",atom==null?Collections.emptyList():Collections.singletonList(atom.specHash),"bindingHashes",Collections.emptyList(),"matrixSpecRefs",Collections.emptyList(),"normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","missingPolicyPerFeature",Collections.emptyList(),"inputDimension",10,"estimatedStateBytes",100000);
  Map<String,Object>root=map("compilerRevision","AST3_CP4_1","experts",Collections.emptyList(),"family","FREQUENCY","featureManifest",fm,"featureOutputIds",Collections.emptyList(),"game",V51Repository.GAME_CODE,"inputDimension",10,"modelKind","FIXED_GRAPH","nodes",nodes,"normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","outputId","n10","position",atom==null?1:atom.position,"version",3);
  fm.put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(root));return CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));
 }
 // Independently retained old implementation: rebuild the entire prefix at every target.
 static Map<String,Value> old(Program program,DevelopmentInputCP9.Input input,int index,HistoricalTrainingReceiptCP12.Row bound,MemoryBudgetCP4 budget){
  Map<String,Value>out=new TreeMap<>(bound.atoms);out.putAll(MatrixHistoricalDrawCP12.evaluateCP12(program,input,index));long at=input.openTime(index)-1;String prior=input.issue(index-1);Set<String>matrixNodes=MatrixBindingsCP12.nodeIds(program.ast,program.manifest);
  try(AtomWindowCP4 window=new AtomWindowCP4(budget)){
   for(int i=Math.max(0,index-5000);i<index;i++)window.append(i,input.actual(i,program.ast.position)-1);
   for(NodeSpec node:program.ast.graphSpec.nodes)if(node.kind==NodeKind.ATOM){if(matrixNodes.contains(node.qualifiedId())){if(!out.containsKey(node.qualifiedId()))throw new DevelopmentProductionCP12.Wait("WAIT_BOUND_FEATURE_HISTORY");continue;}AtomSpecCP4 spec=program.specById(node.source);if(spec.requiresBinding){if(!out.containsKey(node.qualifiedId()))throw new DevelopmentProductionCP12.Wait("WAIT_BOUND_FEATURE_HISTORY");continue;}AtomEvaluatorCP4.Result r=AtomEvaluatorCP4.evaluate(spec.mathSpec,window,null,index);out.put(node.qualifiedId(),new Value(new double[]{r.present?r.value:0},new boolean[]{r.present},prior,at,0,V51ChecksumEngine.sha256("SCDEV_ATOM|"+input.inputHash+'|'+input.issue(index)+'|'+spec.specHash)));}
  }return out;
 }
 @SuppressWarnings("unchecked") public static void main(String[]a)throws Exception{
  Map<String,AtomSpecCP4>families=new TreeMap<>();try(BufferedReader r=Files.newBufferedReader(Path.of(a[0]))){String line;while((line=r.readLine())!=null){AtomSpecCP4 atom=AtomSpecCP4.fromAssetLine(line);if(!atom.requiresBinding&&atom.transform.equals("IDENTITY")&&atom.outputType.equals("SCALAR")&&atom.minimumHistory<=1000)families.putIfAbsent(atom.family,atom);}}
  List<Models.Draw>history=new ArrayList<>();try(BufferedReader r=Files.newBufferedReader(Path.of(a[1]))){String line;while((line=r.readLine())!=null){String[]v=line.split("\t");Models.Draw d=new Models.Draw();d.issue=v[0];d.openTime=Long.parseLong(v[1]);for(int p=0;p<10;p++)d.numbers[p]=Integer.parseInt(v[p+2]);history.add(d);}}
  ok(history.size()==5000,"original report draw count");
  DevelopmentInputCP9.Header h=new DevelopmentInputCP9.Header(StrictCanonicalJson.writeContext(map("revision","SCDEV1","game","PK10","build","R79_EXPLICIT_FEATURE_TEST","sourceManifestHash",V51ChecksumEngine.sha256("RECOMPUTED_FROM_REPORT_DRAWS_NOT_ORIGINAL_PHONE_CAS"),"scheduleRevision","TARGET_ISSUE_RESOLVER_R980","bindings",Collections.emptyList())));
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DevelopmentInputCP9.Receipt receipt=DevelopmentInputCP9.write(bytes,h,history);MemoryBudgetCP4 inputMemory=new MemoryBudgetCP4(4L<<20);
  Method changed=DevelopmentProductionCP12.class.getDeclaredMethod("dataAtoms",Program.class,DevelopmentInputCP9.Input.class,int.class,HistoricalTrainingReceiptCP12.Row.class,DevelopmentProductionCP12.FeatureAtomHistoryR79.class);changed.setAccessible(true);
  try(DevelopmentInputCP9.Input input=DevelopmentInputCP9.read(new ByteArrayInputStream(bytes.toByteArray()),receipt.byteCount,receipt.inputHash,h,inputMemory)){
   for(AtomSpecCP4 atom:families.values()){
    CandidateAstR952 ast=ast(atom);Program program=CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,Collections.singletonList(atom));
    for(int start:new int[]{1500,4900}){
     MemoryBudgetCP4 pageMemory=new MemoryBudgetCP4(8L<<20),scalarMemory=new MemoryBudgetCP4(8L<<20);
     try(DevelopmentProductionCP12.FeatureAtomHistoryR79 cache=new DevelopmentProductionCP12.FeatureAtomHistoryR79(input,ast.position,pageMemory)){
      for(int target=start;target<start+64;target++){if(target==start+31)continue;
       HistoricalTrainingReceiptCP12.Row bound=HistoricalTrainingReceiptCP12.readFor(null,program,Collections.emptyMap(),input.issue(target),input.openTime(target));
       long before=System.nanoTime();Map<String,Value>x=old(program,input,target,bound,scalarMemory);oldNs+=System.nanoTime()-before;
       before=System.nanoTime();Map<String,Value>y=(Map<String,Value>)changed.invoke(null,program,input,target,bound,cache);newNs+=System.nanoTime()-before;
       ok(x.keySet().equals(y.keySet()),"map keys");for(String key:x.keySet()){Value u=x.get(key),v=y.get(key);ok(u.valueHash.equals(v.valueHash)&&u.sourceHash.equals(v.sourceHash)&&u.maxSourceIssue.equals(v.maxSourceIssue)&&u.availableAtMs==v.availableAtMs,"value/hash parity "+atom.family+" "+target);for(int k=0;k<u.size();k++)ok(u.present(k)==v.present(k)&&Double.doubleToRawLongBits(u.get(k))==Double.doubleToRawLongBits(v.get(k)),"bits");}
       // The same actual downstream feature compiler must emit identical authenticated rows.
       if(atom.family.equals("FREQUENCY")||atom.family.equals("ENTROPY")){
        DevelopmentTrainerCP9.Row rx=WorldFeatureComputationR974.rawTrainingRowCP11(program,input,target,receipt.inputHash,x,bound.experts,bound.hash,scalarMemory),ry=WorldFeatureComputationR974.rawTrainingRowCP11(program,input,target,receipt.inputHash,y,bound.experts,bound.hash,pageMemory);
        ok(rx.inputHash.equals(ry.inputHash)&&rx.issue.equals(ry.issue)&&rx.actual==ry.actual&&Arrays.equals(DevelopmentProductionCP12.encode(rx.valuesCopy(),rx.presentCopy()),DevelopmentProductionCP12.encode(ry.valuesCopy(),ry.presentCopy())),"raw feature payload");
       }
       ok(scalarMemory.used()==0,"scalar memory");rows++;
      }
      boolean rejected=false;try{cache.before(start);}catch(IllegalArgumentException e){rejected=e.getMessage().equals("TRAIN_ATOM_HISTORY_ORDER");}ok(rejected,"backwards target rejected");
     }ok(pageMemory.used()==0,"page memory");
    }
   }
   CandidateAstR952 noAtoms=ast(null);Program noProgram=CandidateCompilerR952.compile(noAtoms,noAtoms.graphSpec.manifest,Collections.emptyList());MemoryBudgetCP4 zero=new MemoryBudgetCP4(0);
   try(DevelopmentProductionCP12.FeatureAtomHistoryR79 empty=new DevelopmentProductionCP12.FeatureAtomHistoryR79(input,1,zero)){
    HistoricalTrainingReceiptCP12.Row bound=HistoricalTrainingReceiptCP12.readFor(null,noProgram,Collections.emptyMap(),input.issue(1500),input.openTime(1500));ok(((Map<?,?>)changed.invoke(null,noProgram,input,1500,bound,empty)).isEmpty(),"no atoms need no window reservation");ok(zero.used()==0,"zero budget retained");
   }
   MemoryBudgetCP4 cancelled=new MemoryBudgetCP4(8L<<20);try(DevelopmentProductionCP12.FeatureAtomHistoryR79 page=new DevelopmentProductionCP12.FeatureAtomHistoryR79(input,1,cancelled)){page.before(1500);throw new IOException("EXPLICIT_INTERRUPTED_PAGE");}catch(IOException expected){ok(cancelled.used()==0,"interrupted page releases memory");}
  }
  ok(inputMemory.used()==0,"input memory");System.out.println("{\"status\":\"PASS\",\"assertions\":"+checks+",\"families\":"+families.size()+",\"rows\":"+rows+",\"old_method_ns\":"+oldNs+",\"page_method_ns\":"+newNs+",\"scope\":\"ORIGINAL_REPORT_DRAWS_ACTUAL_PRODUCT_FEATURE_BIT_HASH_PARITY_NOT_PHONE_SCHEDULING\"}");
 }
}
