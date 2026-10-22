package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
import java.util.*;import java.nio.file.*;
/** New explicit test fixture helper. Original helper was absent from the supplied package. */
public final class ModernModelContractTest {
 public static final String H=V51ChecksumEngine.sha256("EXPLICIT_REPLACEMENT_TEST_FIXTURE");
 public static Map<String,Object> map(Object...pairs){Map<String,Object> out=new TreeMap<>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;}
 @SuppressWarnings("unchecked") private static Object mutable(Object value){if(value instanceof Map){Map<String,Object> copy=new TreeMap<>();for(Map.Entry<String,Object> entry:((Map<String,Object>)value).entrySet())copy.put(entry.getKey(),mutable(entry.getValue()));return copy;}if(value instanceof List){List<Object> copy=new ArrayList<>();for(Object item:(List<?>)value)copy.add(mutable(item));return copy;}return value;}
 public static CandidateAstR952.Family family(ModelKind k){switch(k){case FIXED_GRAPH:return CandidateAstR952.Family.FREQUENCY;case SOFTMAX_LINEAR:return CandidateAstR952.Family.LINEAR;case OVR_LOGISTIC:return CandidateAstR952.Family.LOGISTIC;case HISTOGRAM_TREE:return CandidateAstR952.Family.TREE;case EXPERT_HEDGE:return CandidateAstR952.Family.BANDIT;default:return CandidateAstR952.Family.NEURAL;}}
 @SuppressWarnings("unchecked") public static Map<String,Object> astRoot(ModelKind kind,CandidateAstR952.Family family){
  try {
   Map<String,Object> root=(Map<String,Object>)mutable(StrictCanonicalJson.readObject(Files.readString(Path.of(System.getProperty("starcore.test.ast"))).substring(5)));
   List<Object> nodes=new ArrayList<>();int f=kind==ModelKind.FIXED_GRAPH||kind==ModelKind.EXPERT_HEDGE?0:2,d=f>0?4:kind==ModelKind.EXPERT_HEDGE?20:10;
   int constants=kind==ModelKind.FIXED_GRAPH?10:2;
   for(int n=0;n<constants;n++)nodes.add(map("id","n"+n,"operator","CONSTANT","outputType","DOUBLE","atomId","","parents",Collections.emptyList(),"parameters",new ArrayList<>(Collections.singletonList(Double.toHexString(n+1))),"stage","FROZEN_PREDRAW","unit","dimensionless"));
   if(kind==ModelKind.FIXED_GRAPH){List<String> parents=new ArrayList<>();for(int n=0;n<10;n++)parents.add("n"+n);nodes.add(map("id","n10","operator","PACK10","outputType","SCORE_VECTOR10","atomId","","parents",parents,"parameters",Collections.emptyList(),"stage","FROZEN_PREDRAW","unit","dimensionless"));}
   List<Object> experts=new ArrayList<>();if(kind==ModelKind.EXPERT_HEDGE)for(int n=0;n<2;n++)experts.add(map("freezeHash",V51ChecksumEngine.sha256("TEST_TEACHER_FREEZE:"+n),"modelHash",H,"featureManifestHash",H,"position",1));
   List<String> ids=f==0?Collections.emptyList():Arrays.asList("n0","n1");root.put("nodes",nodes);root.put("family",family.name());root.put("modelKind",kind.name());root.put("experts",experts);root.put("featureOutputIds",ids);root.put("inputDimension",d);root.put("outputId",kind==ModelKind.FIXED_GRAPH?"n10":"@MODEL");
   Map<String,Object> fm=(Map<String,Object>)root.get("featureManifest");fm.put("atomSpecHashes",Collections.emptyList());fm.put("matrixSpecRefs",Collections.emptyList());fm.put("orderedFeatureIds",ids);fm.put("featureTypes",f==0?Collections.emptyList():Arrays.asList("DOUBLE","DOUBLE"));fm.put("missingPolicyPerFeature",f==0?Collections.emptyList():Arrays.asList("REJECT","MASK"));fm.put("inputDimension",d);fm.put("requiredHistory",0);fm.put("estimatedStateBytes",100000);fm.put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(root));return root;
  }catch(java.io.IOException e){throw new IllegalStateException("TEST_AST_TEMPLATE_MISSING",e);}
 }
 public static Modern sample(ModelKind k,CandidateAstR952.Family family){
  int f=k==ModelKind.FIXED_GRAPH||k==ModelKind.EXPERT_HEDGE?0:2,e=k==ModelKind.EXPERT_HEDGE?2:0,d=f>0?4:e>0?20:10;double[] std=new double[f];Arrays.fill(std,1);int[] tree=k==ModelKind.HISTOGRAM_TREE?new int[]{-1}:new int[0];double[] probabilities=new double[tree.length*10];Arrays.fill(probabilities,.1);
  List<ExpertRef> refs=e==0?Collections.emptyList():Arrays.asList(new ExpertRef(V51ChecksumEngine.sha256("TEST_TEACHER_FREEZE:0"),H,H,1),new ExpertRef(V51ChecksumEngine.sha256("TEST_TEACHER_FREEZE:1"),H,H,1));
  return new Modern(k,family,d,f,e,300,42,"202609230002",H,H,H,H,new double[f],std,new double[Modern.expectedWeights(k,d,f,e)],tree,new double[tree.length],tree,tree,probabilities,refs);
 }
 public static Modern bound(Modern m,FeatureManifestR952 fm){return new Modern(m.kind,m.family,m.inputDimension,m.featureN,m.expertN,m.uniqueTrainN,m.seed,m.trainedUntil,fm.hash,Modern.normalizerHash(fm.orderedFeatureIds,m.means(),m.standardDeviations()),fm.graphHash,m.developmentReceiptHash,m.means(),m.standardDeviations(),m.weights(),m.treeFeatures(),m.treeCuts(),m.treeLeft(),m.treeRight(),m.treeLeafProbabilities(),m.experts);}
}
