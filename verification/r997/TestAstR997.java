package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;
import java.util.*;import java.nio.file.*;import java.io.*;
/** Explicit synthetic fixture template; not a registered production candidate or trained model. */
public final class TestAstR997 {
 public static void main(String[] args)throws Exception {
  AtomSpecCP4 atom=null;
  try(BufferedReader in=Files.newBufferedReader(Path.of(args[0]))){String line;
   while((line=in.readLine())!=null){AtomSpecCP4 a=AtomSpecCP4.fromAssetLine(line);
    if(a.outputType.equals("SCALAR")&&!a.requiresBinding&&a.minimumHistory<=20){atom=a;break;}}
  }
  if(atom==null)throw new AssertionError("scalar catalog fixture absent");
  List<Object> nodes=new ArrayList<>();List<String> parents=new ArrayList<>();
  for(int i=0;i<10;i++) {
   parents.add("n"+i);nodes.add(ModernModelContractTest.map("id","n"+i,
    "operator",i==0?"ATOM":"CONSTANT","outputType","DOUBLE","atomId",i==0?atom.atomId:"",
    "parents",Collections.emptyList(),"parameters",i==0?Collections.emptyList():Collections.singletonList(Double.toHexString(i+1)),
    "stage","FROZEN_PREDRAW","unit","dimensionless"));
  }
  nodes.add(ModernModelContractTest.map("id","n10","operator","PACK10","outputType","SCORE_VECTOR10",
   "atomId","","parents",parents,"parameters",Collections.emptyList(),"stage","FROZEN_PREDRAW","unit","dimensionless"));
  Map<String,Object> manifest=ModernModelContractTest.map("revision","CP4_FEATURE_1","graphHash",ModernModelContractTest.H,
   "orderedFeatureIds",Collections.emptyList(),"featureTypes",Collections.emptyList(),"requiredHistory",atom.minimumHistory,
   "atomSpecHashes",Collections.singletonList(atom.specHash),"bindingHashes",Collections.emptyList(),"matrixSpecRefs",Collections.emptyList(),
   "normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","missingPolicyPerFeature",Collections.emptyList(),"inputDimension",10,"estimatedStateBytes",100000);
  Map<String,Object> root=ModernModelContractTest.map("compilerRevision","AST3_CP4_1","experts",Collections.emptyList(),
   "family","FREQUENCY","featureManifest",manifest,"featureOutputIds",Collections.emptyList(),"game",V51Repository.GAME_CODE,
   "inputDimension",10,"modelKind","FIXED_GRAPH","nodes",nodes,"normalizerPolicy","TRAIN_ONLY_ZSCORE_V1",
   "outputId","n10","position",atom.position,"version",3);
  manifest.put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(root));
  String ast="AST3:"+StrictCanonicalJson.write(root);CandidateAstR952.parse(ast);
  Files.writeString(Path.of(args[1]),ast);System.out.println("EXPLICIT_TEST_AST "+V51ChecksumEngine.sha256(ast));
 }
}
