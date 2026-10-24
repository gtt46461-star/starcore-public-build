package com.ethk3.novaonepro.r952;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Matrix refs own exact ATOM scalar nodes. The selected frozen value is a real graph input. */
public final class MatrixBindingsCP12 {
 private MatrixBindingsCP12(){}
 public static final class Ref {
  public final MatrixProjectionCP12.Type type;public final int window;public final String selector,metric,nodeId,manifest;
  Ref(FeatureManifestR952.MatrixRef ref){
   String[] p=ref.partitionRule.split("/",-1);need(p.length==5&&p[0].equals("CP12")&&p[1].matches("[1-9][0-9]{0,3}"),"MATRIX_PARTITION_RULE");
   type=MatrixProjectionCP12.Type.valueOf(ref.type);window=Integer.parseInt(p[1]);selector=p[2];metric=p[3];nodeId=p[4];manifest=ref.manifest;
   need(selector.equals("POSITION")||selector.equals("CROSSNEXT")||selector.matches("POSLAG_[1-9][0-9]{0,3}")||selector.matches("POSLAG_[1-9][0-9]{0,3}_REGIME_[0-4]")||selector.matches("L_[A-Za-z0-9_-]{1,342}"),"MATRIX_SELECTOR");
   need(nodeId.matches("n[0-9]{1,3}")&&metric.matches("[A-Za-z][A-Za-z0-9_]{0,63}(~[0-9])?"),"MATRIX_METRIC_NODE");
   validateMetric(type,metric);
   metricSourceCP13(1); // Explicit V2 row metrics must resolve a real lagged draw partition.
   need(manifest.equals(specManifest(type,window,selector,metric)),"MATRIX_SPEC_MANIFEST_HASH");
   new MatrixProjectionCP12.Spec(type,partition(1),window,manifest); // bounds validated before capture.
  }
  public String partition(int position){need(position>=1&&position<=10,"MATRIX_POSITION");
   if(selector.equals("CROSSNEXT"))return "position:"+position+"|sourcePosition:"+(position%10+1)+"|lag:1";
   if(selector.equals("POSITION"))return "position:"+position;
   if(selector.startsWith("POSLAG_")){String[] p=selector.split("_");return "position:"+position+"|lag:"+p[1]+(p.length==4?"|regime:"+p[3]+"|regimePolicy:"+AtomRegimePolicyCP12.POLICY_HASH:"");}
   byte[] bytes=android.util.Base64.decode(selector.substring(2),android.util.Base64.URL_SAFE|android.util.Base64.NO_PADDING|android.util.Base64.NO_WRAP);String result=new String(bytes,StandardCharsets.UTF_8);need(Arrays.equals(bytes,result.getBytes(StandardCharsets.UTF_8))&&android.util.Base64.encodeToString(bytes,android.util.Base64.URL_SAFE|android.util.Base64.NO_PADDING|android.util.Base64.NO_WRAP).equals(selector.substring(2)),"MATRIX_SELECTOR_CANONICAL");return result;
  }
  /** Old latestRow metrics deliberately retain CP12 next-position/lag-one semantics.
   * New V2 names have distinct spec hashes and condition on the declared partition. */
  int[] metricSourceCP13(int position){
   if(!(metric.startsWith("sourceRowV2~")||metric.startsWith("sourceSmoothedRowV2~")))return new int[]{position%10+1,1};
   java.util.regex.Matcher p=java.util.regex.Pattern.compile("position:([1-9]|10)(?:\\|sourcePosition:([1-9]|10))?\\|lag:([1-9][0-9]{0,3})").matcher(partition(position));
   need(p.matches(),"MATRIX_V2_SOURCE_PARTITION");
   return new int[]{Integer.parseInt(p.group(2)==null?p.group(1):p.group(2)),Integer.parseInt(p.group(3))};
  }
  public String node(){return "CP4:"+nodeId;}
 }
 public static void validateMetric(MatrixProjectionCP12.Type type,String metric){
  String pattern;switch(type){
   case TransitionMatrix:pattern="(rowCounts0_[0-9]|row0_[0-9]|latestRow|smoothedRow0_[0-9]|latestSmoothedRow|sourceRowV2|sourceSmoothedRowV2)~[0-9]";break;
   case ConditionalTransitionMatrix:pattern="(rowCounts[0-4]_[0-9]|row[0-4]_[0-9])~[0-9]";break;
   case CoOccurrenceMatrix:pattern="N|jointMiss|(eventCounts|eventRates)~[0-3]";break;
   case FrequencyMatrix:pattern="N|(probabilities|count|rate|z)~[0-9]";break;
   case OmissionMatrix:pattern="(omission|gapVariance|lastSeen|gap|gapMean|gapSupport)[0-9]";break;
   case AtomTargetMatrix:pattern="N|contributionN|oosContribution|oosContributionSE|missing|mutualInformation|correlation|meanDifference";break;
   case AtomRegimeMatrix:pattern="N|scoreKSLikeDrift|driftReferenceN|driftRecentN|availability|ablationMean|ablationSE|scoreMean|scoreVariance";break;
   case CandidateWindowMatrix:pattern="N|eligibleN|coverage|hitRate|wilsonLower|brier|ece|maxLossStreak";break;
   case CandidateErrorMatrix:pattern="N|missingCapture|missSeverity[0-9]|(missCounts|missSeverityN|missBuckets)~[0-9]";break;
   case CandidateSimilarityMatrix:pattern="N|jaccard|predictionCorrelation";break;
   case CandidateDiversityMatrix:pattern="N|diversity|jointMiss|residualCorrelation|predictionCorrelation|errorCorrelation|disagreement|complementarity|verifiedRegimeN|regimeSupport~[0-3]|regimeComplementarity[0-3]";break;
   case ParentChildMatrix:pattern="N|resourceN|resourceElapsedNanosDelta|resourceHeapBytesDelta|mean|pairedSE|ci95Lower|ci95Upper|pairedTailBound";break;
   case DriftMatrix:pattern="js|psi|ksLike|referenceN|recentN";break;
   case ConfidenceMatrix:pattern="N|rawConfidenceN|raw_conf|cal_conf|LCB|ece|brier";break;
   case LatencyMatrix:pattern="p50Micros|p95Micros|p99Micros|count";break;
   case TeacherMatrix:pattern="support|agreement|utility|hitUtility|brierDelta";break;
   case RuleHealthMatrix:pattern="drift|driftKSLike|driftReferenceN|driftRecentN|missingRate|integrityRate|coverage|successRate|bins~[0-9]";break;
   case FreezeMatrix:pattern="componentCount";break;
   case FailureMatrix:pattern="failureCount|severityCounts~[0-3]|recoveryLagMean";break;
   case ResidualMatrix:pattern="mean|bias|variance|lag1Correlation";break;
   default:throw new AssertionError(type);
  }need(metric.matches(pattern),"MATRIX_UNKNOWN_METRIC");
 }
 public static String specManifest(MatrixProjectionCP12.Type type,int window,String selector,String metric){return MatrixProjectionCP12.hash("MATRIX_SPEC_CP12|"+type+'|'+window+'|'+selector+'|'+metric);}
 public static List<Ref> validate(CandidateAstR952 ast,FeatureManifestR952 manifest){
  ArrayList<Ref> refs=new ArrayList<>();Set<String> nodes=new HashSet<>();Set<String> matrixSources=new HashSet<>();
  for(FeatureManifestR952.MatrixRef input:manifest.matrixSpecRefs){Ref ref=new Ref(input);need(nodes.add(ref.node()),"DUPLICATE_MATRIX_NODE");
   TypedAtomMoleculeGraphR930.NodeSpec node=ast.graphSpec.typedGraph.require(ref.node());
   need(node.kind==TypedAtomMoleculeGraphR930.NodeKind.ATOM&&node.shape.elements()==1&&(node.valueType.name().equals("DOUBLE")||node.valueType.name().equals("PROBABILITY")),"MATRIX_BOUND_ATOM_SCALAR");
   need(manifest.requiredHistory>=MatrixHistoricalDrawCP12.minimumHistoryCP12(ref),"MATRIX_REQUIRED_HISTORY");
   refs.add(ref);
  }
  return Collections.unmodifiableList(refs);
 }
 public static void registerSpecsCP12(SQLiteDatabase db,CandidateAstR952 ast){
  if(!db.inTransaction())throw new IllegalStateException("MATRIX_SPEC_TRANSACTION");FeatureManifestR952 manifest=FeatureManifestR952.forAst(ast);if(!manifest.modern)return;
  for(Ref ref:validate(ast,manifest))MatrixSnapshotBusR952.registerSpecCP12(db,new MatrixProjectionCP12.Spec(ref.type,ref.partition(ast.position),ref.window,ref.manifest));
 }
 public static Set<String> nodeIds(CandidateAstR952 ast,FeatureManifestR952 manifest){Set<String> out=new HashSet<>();for(Ref r:validate(ast,manifest))out.add(r.node());return out;}
 public static Set<String> atomSources(CandidateAstR952 ast,FeatureManifestR952 manifest){Set<String> out=new HashSet<>();Set<String> bound=nodeIds(ast,manifest);for(Ref r:validate(ast,manifest)){String source=ast.graphSpec.typedGraph.require(r.node()).source;boolean exclusive=true;for(TypedAtomMoleculeGraphR930.NodeSpec node:ast.graphSpec.nodes)if(node.kind==TypedAtomMoleculeGraphR930.NodeKind.ATOM&&node.source.equals(source)&&!bound.contains(node.qualifiedId()))exclusive=false;if(exclusive)out.add(source);}return out;}
 public static final class Frozen {
  public final String type,partition,manifest,matrixId,throughIssue,lastReceipt,payloadHash,metric,nodeId,bindingHash;
  public final int window;public final long version;public final double value;public final boolean present;
  Frozen(Ref ref,String partition,MatrixSnapshotBusR952.CheckpointCP4 cp,double value,boolean present){
   type=ref.type.name();this.partition=partition;manifest=ref.manifest;matrixId=cp.matrixId;throughIssue=cp.throughIssue;lastReceipt=cp.lastReceipt;payloadHash=cp.payloadHash;metric=ref.metric;nodeId=ref.nodeId;window=ref.window;version=cp.version;this.value=value;this.present=present;bindingHash=MatrixProjectionCP12.hash(canonicalIdentity());
  }
  private Frozen(Map<String,Object> o){CandidateAstR952.keys(o,"type","partition","manifest","matrixId","throughIssue","lastReceipt","payloadHash","metric","nodeId","window","version","value","present","bindingHash");
   type=CandidateAstR952.str(o,"type");partition=CandidateAstR952.str(o,"partition");manifest=CandidateAstR952.str(o,"manifest");matrixId=CandidateAstR952.str(o,"matrixId");throughIssue=CandidateAstR952.str(o,"throughIssue");lastReceipt=CandidateAstR952.str(o,"lastReceipt");payloadHash=CandidateAstR952.str(o,"payloadHash");metric=CandidateAstR952.str(o,"metric");nodeId=CandidateAstR952.str(o,"nodeId");window=CandidateAstR952.integer(o,"window");
   Object v=o.get("version");need(v instanceof Long||v instanceof Integer,"MATRIX_VERSION");version=((Number)v).longValue();need(version>=0,"MATRIX_VERSION");value=Double.parseDouble(CandidateAstR952.str(o,"value"));need(Double.isFinite(value)&&Double.toHexString(value).equals(o.get("value")),"MATRIX_VALUE_CANONICAL");need(o.get("present") instanceof Boolean,"MATRIX_PRESENT");present=(Boolean)o.get("present");need(present||value==0,"MATRIX_MISSING_ENCODING");bindingHash=CandidateAstR952.str(o,"bindingHash");
   MatrixProjectionCP12.Spec spec=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.valueOf(type),partition,window,manifest);need(spec.matrixId.equals(matrixId)&&hashOk(lastReceipt)&&hashOk(payloadHash)&&TargetIssueResolver.isValid(throughIssue)&&bindingHash.equals(MatrixProjectionCP12.hash(canonicalIdentity())),"FROZEN_MATRIX_IDENTITY");
  }
  private String canonicalIdentity(){return "CP12_MATRIX_BOUND|"+type+'|'+partition+'|'+window+'|'+manifest+'|'+matrixId+'|'+version+'|'+throughIssue+'|'+lastReceipt+'|'+payloadHash+'|'+metric+'|'+nodeId+'|'+Double.toHexString(value)+'|'+present;}
  public TypedAtomMoleculeGraphR930.Value atomValue(long at){return new TypedAtomMoleculeGraphR930.Value(new double[]{value},new boolean[]{present},throughIssue,at,0,bindingHash);}
  public Map<String,Object> canonicalObject(){Map<String,Object> o=new TreeMap<>();o.put("type",type);o.put("partition",partition);o.put("manifest",manifest);o.put("matrixId",matrixId);o.put("throughIssue",throughIssue);o.put("lastReceipt",lastReceipt);o.put("payloadHash",payloadHash);o.put("metric",metric);o.put("nodeId",nodeId);o.put("window",window);o.put("version",version);o.put("value",Double.toHexString(value));o.put("present",present);o.put("bindingHash",bindingHash);return o;}
 }
 public static Map<String,Frozen> capture(SQLiteDatabase db,CandidateCompilerR952.Program program,PITSnapshotContractR952 pit){
  Map<String,Frozen> result=new TreeMap<>();for(Ref ref:validate(program.ast,program.manifest)){
   try(MemoryBudgetCP4.Lease scratch=com.ethk3.novaonepro.RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(1572864)){
   String partition=ref.partition(program.ast.position);MatrixProjectionCP12.Spec spec=new MatrixProjectionCP12.Spec(ref.type,partition,ref.window,ref.manifest);
   MatrixSnapshotBusR952.CheckpointCP4 cp=MatrixSnapshotBusR952.readAtCP4(db,spec.matrixId,ref.manifest,pit.targetIssue);
   if(cp==null||MatrixHistoricalDrawCP12.isDrawRefCP12(ref)&&!cp.throughIssue.equals(pit.lastIssue))cp=MatrixHistoricalDrawCP12.isDrawRefCP12(ref)?MatrixHistoricalDrawCP12.checkpointFromPitCP12(ref,program.ast.position,pit):replayAt(db,spec,pit);
   need(cp!=null&&cp.throughIssue.equals(pit.lastIssue)&&cp.throughIssue.compareTo(pit.targetIssue)<0,"WAIT_MATRIX_WATERMARK");
   need(MatrixProjectionCP12.SCHEMA_HASH.equals(cp.schemaHash),"MATRIX_SCHEMA_MISMATCH");
   Map<String,double[]> metrics=MatrixProjectionCP12.metrics(MatrixProjectionCP12.decode(spec,cp.payload()));int[] source=ref.metricSourceCP13(program.ast.position);need(source[1]<=pit.history.size(),"WAIT_MATRIX_HISTORY");String selectedMetric=resolveMetricCP12(ref.metric,pit.history.get(source[1]-1).getNumberAtPosition(source[0])-1);String[] selected=selectedMetric.split("~",-1);double[] values=metrics.get(selected[0]);int index=selected.length==2?Integer.parseInt(selected[1]):0;
   boolean present=values!=null&&index<values.length;double value=present?values[index]:0;
   TypedAtomMoleculeGraphR930.NodeSpec node=program.ast.graphSpec.typedGraph.require(ref.node());if(present&&node.valueType.name().equals("PROBABILITY"))need(value>=0&&value<=1,"MATRIX_PROBABILITY_RANGE");
   result.put(ref.node(),new Frozen(ref,partition,cp,value,present));
   }
  }return Collections.unmodifiableMap(result);
 }
 /** Same explicit probability metric in historical training and frozen inference. */
 static String resolveMetricCP12(String metric,int latestDigit){
  need(latestDigit>=0&&latestDigit<10,"MATRIX_SOURCE_DIGIT");
  if(metric.startsWith("sourceSmoothedRowV2~"))return "smoothedRow0_"+latestDigit+metric.substring("sourceSmoothedRowV2".length());
  if(metric.startsWith("sourceRowV2~"))return "row0_"+latestDigit+metric.substring("sourceRowV2".length());
  if(metric.startsWith("latestSmoothedRow~"))return "smoothedRow0_"+latestDigit+metric.substring("latestSmoothedRow".length());
  return metric.startsWith("latestRow~")?"row0_"+latestDigit+metric.substring("latestRow".length()):metric;
 }
 /** Independent PIT namespace. Production checkpoint is never rewound or overwritten. */
 private static MatrixSnapshotBusR952.CheckpointCP4 replayAt(SQLiteDatabase db,MatrixProjectionCP12.Spec spec,PITSnapshotContractR952 pit){
  MatrixProjectionCP12.State state=MatrixProjectionCP12.empty(spec);String through=null,receipt=null;long ordinal=-1;int count=0;
  // Largest ring is 5000; first take the latest 5000 immutable inputs, then apply ascending.
  try(Cursor c=db.rawQuery("SELECT source_issue,source_ordinal,source_receipt_hash,input_payload,input_hash,proof_hash FROM (SELECT source_issue,source_ordinal,source_receipt_hash,input_payload,input_hash,proof_hash FROM research_matrix_input_cp12 WHERE matrix_id=? AND source_issue<? AND state IN ('DONE','READY') ORDER BY source_ordinal DESC,source_receipt_hash DESC LIMIT 5000) ORDER BY source_ordinal,source_receipt_hash",new String[]{spec.matrixId,pit.targetIssue})){
   while(c.moveToNext()){through=c.getString(0);ordinal=c.getLong(1);receipt=c.getString(2);byte[] payload=c.getBlob(3);need(c.getString(4).equals(MatrixProjectionCP12.hash("CP12_MATRIX_INPUT|"+spec.matrixId+'|'+through+'|'+ordinal+'|'+receipt+'|'+c.getString(5)+'|'+MatrixProjectionCP12.bytesHash(payload))),"MATRIX_REPLAY_INPUT_CORRUPT");MatrixProjectionCP12.apply(state,ordinal,MatrixProjectionCP12.sampleFromBytes(payload),MatrixProjectionCP12.componentsFromBytes(payload));count++;}
  }
  if(count==0||!pit.lastIssue.equals(through))return null;
  return new MatrixSnapshotBusR952.CheckpointCP4(spec.type.name(),spec.partition,spec.window,spec.manifest,MatrixProjectionCP12.SCHEMA_HASH,0,through,receipt,state.samples(),MatrixProjectionCP12.encode(state));
 }
 public static Map<String,Frozen> fromCanonicalObject(Object value){Map<String,Frozen> out=new TreeMap<>();for(Map.Entry<String,Object> e:CandidateAstR952.object(value).entrySet()){Frozen f=new Frozen(CandidateAstR952.object(e.getValue()));need(e.getKey().equals("CP4:"+f.nodeId),"MATRIX_NODE_KEY");out.put(e.getKey(),f);}return Collections.unmodifiableMap(out);}
 public static Map<String,Object> canonicalObject(Map<String,Frozen> snapshots){Map<String,Object> out=new TreeMap<>();for(Map.Entry<String,Frozen> e:snapshots.entrySet())out.put(e.getKey(),e.getValue().canonicalObject());return out;}
 public static void validateFrozen(CandidateCompilerR952.Program program,String target,String base,long at,Map<String,Frozen> matrices,Map<String,TypedAtomMoleculeGraphR930.Value> atoms){
  List<Ref> refs=validate(program.ast,program.manifest);Set<String> expected=new HashSet<>();for(Ref ref:refs){expected.add(ref.node());Frozen f=matrices.get(ref.node());
   need(f!=null&&f.type.equals(ref.type.name())&&f.window==ref.window&&f.partition.equals(ref.partition(program.ast.position))&&f.manifest.equals(ref.manifest)&&f.metric.equals(ref.metric)&&f.nodeId.equals(ref.nodeId)&&f.throughIssue.equals(base)&&f.throughIssue.compareTo(target)<0,"FROZEN_MATRIX_CLOSURE_OR_PIT");
   TypedAtomMoleculeGraphR930.Value v=atoms.get(ref.node()),planned=f.atomValue(at);need(v!=null&&v.valueHash.equals(planned.valueHash)&&v.sourceHash.equals(f.bindingHash),"MATRIX_GRAPH_VALUE_MISMATCH");
  }need(matrices.keySet().equals(expected),"FROZEN_MATRIX_CLOSURE");
 }
 private static boolean hashOk(String s){return s!=null&&s.matches("[0-9a-f]{64}");}
 private static void need(boolean b,String code){if(!b)throw new IllegalArgumentException(code);}
}
