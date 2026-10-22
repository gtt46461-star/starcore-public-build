package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;import java.nio.file.*;
/** Independent count oracle. Synthetic permutations are not future qualification evidence. */
public final class MatrixSelectorsR79 {
 static int checks;static void ok(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static String h(String s){return V51ChecksumEngine.sha256(s);}
 static void rejected(Runnable r){checks++;try{r.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("invalid selector accepted");}
 @SuppressWarnings("unchecked") static Program program(AtomSpecCP4 atom,int position,String selector,String metric){
  Map<String,Object> root=ModernModelContractTest.astRoot(ModelKind.SOFTMAX_LINEAR,CandidateAstR952.Family.LINEAR);root.put("position",position);
  for(Object raw:(List<Object>)root.get("nodes")){Map<String,Object> n=(Map<String,Object>)raw;n.put("operator","ATOM");n.put("outputType","DOUBLE");n.put("atomId",atom.atomId);n.put("parameters",Collections.emptyList());}
  Map<String,Object> fm=(Map<String,Object>)root.get("featureManifest");String hash=MatrixBindingsCP12.specManifest(MatrixProjectionCP12.Type.TransitionMatrix,20,selector,metric);
  Map<String,Object> matrix=ModernModelContractTest.map("type","TransitionMatrix","partitionRule","CP12/20/"+selector+"/"+metric+"/n0","manifest",hash);
  fm.put("matrixSpecRefs",Collections.singletonList(matrix));fm.put("atomSpecHashes",Collections.singletonList(atom.specHash));fm.put("requiredHistory",MatrixHistoricalDrawCP12.requiredHistoryCP12(Collections.singletonList(matrix)));fm.put("estimatedStateBytes",200000);fm.put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(root));
  CandidateAstR952 ast=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));return CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,Collections.singletonList(atom));
 }
 static double oracle(List<Models.Draw> rows,int target,int position,int source,int lag,int selectedSource,int selectedLag,int digit){
  int condition=rows.get(target-selectedLag).getNumberAtPosition(selectedSource),n=0,hit=0;
  for(int i=target-20;i<target;i++)if(rows.get(i-lag).getNumberAtPosition(source)==condition){n++;if(rows.get(i).getNumberAtPosition(position)==digit+1)hit++;}
  return (hit+.5)/(n+5.0);
 }
 static void check(SQLiteDatabase db,List<Models.Draw> rows,AtomSpecCP4 atom,int position,String selector,int source,int lag,String metric,boolean legacy)throws Exception{
  int target=rows.size()-1,digit=4;Program p=program(atom,position,selector,metric+"~"+digit);double expected=oracle(rows,target,position,source,lag,legacy?position%10+1:source,legacy?1:lag,digit);
  TypedAtomMoleculeGraphR930.Value trained;
  try(DevelopmentInputCP9.Input input=MatrixHistoricalDrawCP12Test.input(rows)){trained=MatrixHistoricalDrawCP12.evaluateCP12(p,input,target).get("CP4:n0");ok(trained.present(0),"present:"+selector);ok(Double.doubleToLongBits(trained.get(0))==Double.doubleToLongBits(expected),"independent count:"+selector+"/"+metric);}
  StringBuilder wire=new StringBuilder("R958_PIT_V2|").append(rows.get(target).issue);for(int i=target-1;i>=0;i--)wire.append('|').append(rows.get(i).issue).append(':').append(rows.get(i).numbersText());PITSnapshotContractR952 pit=PITSnapshotContractR952.fromFrozenHistory(rows.get(target).issue,rows.subList(0,target),h(wire.toString()));
  MatrixBindingsCP12.Frozen live=MatrixBindingsCP12.capture(db,p,pit).get("CP4:n0");ok(Double.doubleToLongBits(live.value)==Double.doubleToLongBits(expected),"live independent count");ok(live.atomValue(rows.get(target-1).openTime).valueHash.equals(trained.valueHash),"train/live proof identity");
  int a=rows.get(target).numbers[0];rows.get(target).numbers[0]=rows.get(target).numbers[1];rows.get(target).numbers[1]=a;
  try(DevelopmentInputCP9.Input input=MatrixHistoricalDrawCP12Test.input(rows)){ok(MatrixHistoricalDrawCP12.evaluateCP12(p,input,target).get("CP4:n0").valueHash.equals(trained.valueHash),"target excluded");}
 }
 public static void main(String[] args)throws Exception{try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){
  Map<Integer,AtomSpecCP4> atoms=new HashMap<>();for(String line:Files.readAllLines(Path.of(args[2]))){AtomSpecCP4 a=AtomSpecCP4.fromAssetLine(line);if(a.family.equals("FREQUENCY")&&a.mathSpec.w==20&&a.mathSpec.digit==0)atoms.put(a.position,a);}
  List<Models.Draw> rows=new ArrayList<>();Random random=new Random(79);for(int n=0;n<64;n++){Models.Draw d=new Models.Draw();d.issue=String.format(Locale.ROOT,"20260923%04d",n+1);d.openTime=1000L*(n+1);d.fetchTime=d.openTime;List<Integer> permutation=new ArrayList<>();for(int i=1;i<=10;i++)permutation.add(i);Collections.shuffle(permutation,random);for(int p=0;p<10;p++)d.numbers[p]=permutation.get(p);rows.add(d);}
  for(int position=1;position<=10;position++){
   AtomSpecCP4 atom=atoms.get(position);ok(atom!=null,"registered atom");
   check(db,rows,atom,position,"CROSSNEXT",position%10+1,1,"sourceSmoothedRowV2",false);
   check(db,rows,atom,position,"CROSSNEXT",position%10+1,1,"latestSmoothedRow",true);
   for(int lag:new int[]{1,3}){check(db,rows,atom,position,"POSLAG_"+lag,position,lag,"sourceSmoothedRowV2",false);check(db,rows,atom,position,"POSLAG_"+lag,position,lag,"latestSmoothedRow",true);}
  }
  String encoded="L_"+android.util.Base64.encodeToString("position:1|sourcePosition:10|lag:2".getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
  check(db,rows,atoms.get(1),1,encoded,10,2,"sourceSmoothedRowV2",false);
  ok(!program(atoms.get(1),1,"POSLAG_3","sourceSmoothedRowV2~4").ast.structuralHash.equals(program(atoms.get(1),1,"POSLAG_3","latestSmoothedRow~4").ast.structuralHash),"new semantics require distinct AST identity");
  rejected(()->program(atoms.get(1),1,"POSITION","sourceRowV2~0"));
  ok(MatrixBindingsCP12.resolveMetricCP12("sourceRowV2~4",6).equals("row0_6~4"),"observed row V2 mapping");
  System.out.println("R79_MATRIX_SELECTORS_PASS assertions="+checks+" independentCountOracle=PASS legacyCP12=UNCHANGED trainLiveIdentity=PASS targetExcluded=PASS");
 }}
}
