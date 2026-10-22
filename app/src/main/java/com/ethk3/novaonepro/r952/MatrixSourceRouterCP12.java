package com.ethk3.novaonepro.r952;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;import java.util.*;
/** Pulls authoritative source rows into the durable projection inbox; never mutates their owners. */
public final class MatrixSourceRouterCP12 {
 private MatrixSourceRouterCP12(){}
 public static final class PreparedInput {
  public final MatrixProjectionCP12.Spec spec;public final String issue,receipt,proof;public final long ordinal;
  public final MatrixProjectionCP12.Sample sample;
  PreparedInput(MatrixProjectionCP12.Spec spec,String issue,String receipt,String proof,long ordinal,MatrixProjectionCP12.Sample sample){this.spec=spec;this.issue=issue;this.receipt=receipt;this.proof=proof;this.ordinal=ordinal;this.sample=sample;}
 }
 public static void registerFrozenPolicyMatricesCP12(SQLiteDatabase db,String freezeHash){
  if(!db.inTransaction()||!hash(freezeHash))throw new IllegalArgumentException("MATRIX_FROZEN_POLICY_IDENTITY");
  String selector="L_"+android.util.Base64.encodeToString(freezeHash.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
  for(int window:new int[]{300})for(MatrixProjectionCP12.Type type:new MatrixProjectionCP12.Type[]{MatrixProjectionCP12.Type.CandidateWindowMatrix,MatrixProjectionCP12.Type.CandidateErrorMatrix,MatrixProjectionCP12.Type.ConfidenceMatrix,MatrixProjectionCP12.Type.ResidualMatrix}){
   String metric=type==MatrixProjectionCP12.Type.CandidateWindowMatrix?"hitRate":type==MatrixProjectionCP12.Type.CandidateErrorMatrix?"missingCapture":type==MatrixProjectionCP12.Type.ConfidenceMatrix?"ece":"mean";
   MatrixSnapshotBusR952.registerSpecCP12(db,new MatrixProjectionCP12.Spec(type,freezeHash,window,policyManifest(type,window,selector,metric)));
  }
 }
 public static MatrixProjectionCP12.Spec frozenPolicySpecCP12(MatrixProjectionCP12.Type type,String freezeHash,int window){
  if(!hash(freezeHash)||window!=300)throw new IllegalArgumentException("MATRIX_FROZEN_POLICY_MASTER_WINDOW");String selector="L_"+android.util.Base64.encodeToString(freezeHash.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
  String metric=type==MatrixProjectionCP12.Type.CandidateWindowMatrix?"hitRate":type==MatrixProjectionCP12.Type.CandidateErrorMatrix?"missingCapture":type==MatrixProjectionCP12.Type.ConfidenceMatrix?"ece":type==MatrixProjectionCP12.Type.ResidualMatrix?"mean":null;
  if(metric==null)throw new IllegalArgumentException("MATRIX_POLICY_TYPE");return new MatrixProjectionCP12.Spec(type,freezeHash,window,policyManifest(type,window,selector,metric));
 }
 private static String policyManifest(MatrixProjectionCP12.Type type,int window,String selector,String metric){return MatrixProjectionCP12.hash("CP12_POLICY_MATRIX_SPEC|"+MatrixProjectionCP12.POLICY_VIEWS_HASH+'|'+MatrixBindingsCP12.specManifest(type,window,selector,metric));}
 public static void enqueueFreezeCP12(SQLiteDatabase db,String freezeHash,String target,String developmentReceipt,Map<String,String> components,long now){
  if(!db.inTransaction()||!hash(freezeHash)||!hash(developmentReceipt)||components==null||!components.keySet().equals(new HashSet<>(Arrays.asList("ast","model","features","development","protocol")))||!developmentReceipt.equals(components.get("development")))throw new IllegalArgumentException("MATRIX_FREEZE_SOURCE_PROOF");
  MatrixProjectionCP12.Spec spec=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.FreezeMatrix,freezeHash,1,MatrixProjectionCP12.hash("CP12_FREEZE_PROOF_V1"));long ordinal=issueOrdinal(target);String receipt=MatrixProjectionCP12.hash("CP12_FREEZE_EVENT|"+freezeHash+'|'+developmentReceipt);
  MatrixSnapshotBusR952.enqueueCP12(db,spec,target,ordinal,receipt,developmentReceipt,new MatrixProjectionCP12.Sample(ordinal,0,0,0,0,0,0,0,0,0),components,now);
 }
 public static MatrixProjectionCP12.Spec atomRegimeStudySpecCP12(String freezeHash,CandidateCompilerR952.Model model,FeatureManifestR952 manifest,AtomSpecCP4 atom,String binding){if(!atom.family.equals("REGIME_SCORE"))throw new IllegalArgumentException("ATOM_REGIME_STUDY_FAMILY");String partition=atom.specHash+'|'+atomSourceScopeHashCP12(freezeHash,model,manifest,binding)+"|label:"+atom.mathSpec.regime+"|policy:"+ATOM_SCORE5_POLICY_HASH;return new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.AtomRegimeMatrix,partition,300,MatrixProjectionCP12.hash("CP12_ATOM_REGIME_STUDY_SPEC|"+partition+"|300"));}
 public static final String ATOM_ALL_STUDY_POLICY_HASH=MatrixProjectionCP12.hash("CP12_ALL_CONSUMED_TYPED_ATOM_RMS_ZERO_NODE_V2|"+AtomAblationReplayCP12.POLICY_HASH);
 /** Only nodes on the actual prediction input closure receive a study; dormant graph nodes do not acquire evidence. */
 public static List<TypedAtomMoleculeGraphR930.NodeSpec> consumedAtomsCP12(CandidateAstR952 ast){
  if(ast==null||ast.formatVersion!=3||ast.graphSpec.modelKind==CandidateCompilerR952.ModelKind.EXPERT_HEDGE)return Collections.emptyList();Set<String> closure=new HashSet<>();ArrayDeque<String> pending=new ArrayDeque<>();if(ast.graphSpec.modelKind==CandidateCompilerR952.ModelKind.FIXED_GRAPH)pending.add("CP4:"+ast.graphSpec.outputId);else for(String id:ast.graphSpec.featureOutputIds)pending.add("CP4:"+id);
  while(!pending.isEmpty()){String id=pending.removeFirst();if(closure.add(id))pending.addAll(ast.graphSpec.typedGraph.require(id).parents);}List<TypedAtomMoleculeGraphR930.NodeSpec> out=new ArrayList<>();for(TypedAtomMoleculeGraphR930.NodeSpec node:ast.graphSpec.nodes)if(node.kind==TypedAtomMoleculeGraphR930.NodeKind.ATOM&&closure.contains(node.qualifiedId()))out.add(node);return Collections.unmodifiableList(out);
 }
 public static MatrixProjectionCP12.Spec atomStudySpecCP12(String freezeHash,CandidateCompilerR952.Model model,FeatureManifestR952 manifest,AtomSpecCP4 atom,String binding,TypedAtomMoleculeGraphR930.NodeSpec node){
  if(atom==null||node==null||node.kind!=TypedAtomMoleculeGraphR930.NodeKind.ATOM||!node.source.equals(atom.atomId))throw new IllegalArgumentException("ATOM_STUDY_NODE_IDENTITY");String scope=MatrixProjectionCP12.hash("CP12_ATOM_NODE_SCOPE|"+atomSourceScopeHashCP12(freezeHash,model,manifest,binding)+'|'+node.qualifiedId()+'|'+node.structuralHash+'|'+ATOM_ALL_STUDY_POLICY_HASH);String partition=atom.specHash+'|'+freezeHash+"|node:"+node.id+"|policy:"+ATOM_ALL_STUDY_POLICY_HASH;return new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.AtomRegimeMatrix,partition,300,MatrixProjectionCP12.hash("CP12_ALL_ATOM_STUDY_SPEC|"+scope+'|'+partition+"|300"));
 }
 public static void registerFrozenAtomStudiesCP12(SQLiteDatabase db,String freezeHash,CandidateAstR952 ast,CandidateCompilerR952.Model model){
  if(!db.inTransaction()||ast.formatVersion!=3)return;FeatureManifestR952 manifest=FeatureManifestR952.forAst(ast);model.modern().requireManifest(manifest);
  for(TypedAtomMoleculeGraphR930.NodeSpec node:consumedAtomsCP12(ast)){try(Cursor c=db.rawQuery("SELECT spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE atom_id=?",new String[]{node.source})){if(!c.moveToFirst())throw new IllegalStateException("ATOM_STUDY_SPEC_MISSING");AtomSpecCP4 atom=AtomSpecCP4.fromRegistry(node.source,c.getString(0),c.getString(1),c.getString(2),c.getInt(3),c.getString(4),c.getString(5));String binding=atomBindingIdentityCP12(db,ast,manifest,node,atom);MatrixSnapshotBusR952.registerSpecCP12(db,atomStudySpecCP12(freezeHash,model,manifest,atom,binding,node));}}
 }
 public static long issueOrdinal(String issue){if(!TargetIssueResolver.isValid(issue))throw new IllegalArgumentException("MATRIX_SOURCE_ISSUE");int ordinal=TargetIssueResolver.distance("200001010001",issue);if(ordinal<0||ordinal==Integer.MAX_VALUE)throw new IllegalArgumentException("MATRIX_SOURCE_ORDINAL_RANGE");return ordinal;}
 public static PreparedInput prepareNext(DbStore store,long deadlineNanos){return prepareNext(store,deadlineNanos,Collections.<String>emptySet());}
 public static PreparedInput prepareNext(DbStore store,long deadlineNanos,Set<String> excluded){
  SQLiteDatabase db=store.readable();
  if(db.inTransaction())throw new IllegalStateException("MATRIX_SOURCE_PREPARE_IN_WRITE");
  // 64 active partitions per pulse; this is a bounded scan and absent optional evidence does not fabricate a sample.
  ArrayList<MatrixProjectionCP12.Spec> specs=new ArrayList<>();
  String cursor=store.getState("cp12_matrix_source_cursor","");
  for(int pass=0;pass<2&&specs.isEmpty();pass++){
   try(Cursor c=db.rawQuery("SELECT matrix_type,partition_key,window_size,spec_manifest_hash FROM research_matrix_spec_cp12 WHERE matrix_id>? ORDER BY matrix_id LIMIT 64",new String[]{pass==0?cursor:""})){
    while(c.moveToNext())specs.add(new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.valueOf(c.getString(0)),c.getString(1),c.getInt(2),c.getString(3)));
   }
  }
  String attempted=null;RuntimeException sourceFailure=null;
  try {
  for(MatrixProjectionCP12.Spec spec:specs){if(System.nanoTime()+50000000L>=deadlineNanos)return null;attempted=spec.matrixId;if(excluded.contains(spec.matrixId))continue;PreparedInput input;
   switch(spec.type){case FrequencyMatrix:case OmissionMatrix:case TransitionMatrix:case ConditionalTransitionMatrix:case DriftMatrix:input=prepareHistoryInputCP12(db,spec);break;
    case CandidateWindowMatrix:case CandidateErrorMatrix:case ConfidenceMatrix:case ResidualMatrix:input=prepareFormalCandidateInputCP12(db,spec);break;
    case CoOccurrenceMatrix:case CandidateSimilarityMatrix:case CandidateDiversityMatrix:case ParentChildMatrix:case TeacherMatrix:input=preparePairedFormalCP12(db,spec);break;
    case AtomTargetMatrix:case AtomRegimeMatrix:input=prepareFrozenAtomCP12(db,spec);break;
    case RuleHealthMatrix:input=prepareRuleHealthCP12(db,spec);break;
    case LatencyMatrix:input=prepareLatencyCP12(db,spec);break;
    default:input=null;break;
   }if(input!=null)return input;
  }return null;
  }catch(RuntimeException failed){sourceFailure=failed;throw failed;}finally{if(attempted!=null)try{store.setState("cp12_matrix_source_cursor",attempted);}catch(RuntimeException cursorFailure){if(sourceFailure==null)throw cursorFailure;sourceFailure.addSuppressed(cursorFailure);}}
 }
 public static void commitPrepared(SQLiteDatabase db,PreparedInput p,long now){
  if(!db.inTransaction())throw new IllegalStateException("MATRIX_SOURCE_TRANSACTION");
  MatrixSnapshotBusR952.enqueueCP12(db,p.spec,p.issue,p.ordinal,p.receipt,p.proof,p.sample,null,now);
  if(p.spec.type==MatrixProjectionCP12.Type.CandidateWindowMatrix)ProspectiveDiversityCP12.registerFormalPeersCP12(db,p.spec.partition,p.issue);
 }
 static PreparedInput prepareHistoryInputCP12(SQLiteDatabase db,MatrixProjectionCP12.Spec spec){
  java.util.regex.Matcher part=java.util.regex.Pattern.compile("position:([1-9]|10)(?:\\|sourcePosition:([1-9]|10))?(?:\\|lag:([1-9][0-9]{0,3}))?(?:\\|regime:([0-4])\\|regimePolicy:([0-9a-f]{64}))?").matcher(spec.partition);if(!part.matches())return null;
  int position=Integer.parseInt(part.group(1)),sourcePosition=part.group(2)==null?position:Integer.parseInt(part.group(2)),lag=part.group(3)==null?0:Integer.parseInt(part.group(3));if((spec.type==MatrixProjectionCP12.Type.TransitionMatrix||spec.type==MatrixProjectionCP12.Type.ConditionalTransitionMatrix)&&lag==0)return null;
  int regime=part.group(4)==null?-1:Integer.parseInt(part.group(4));boolean conditional=spec.type==MatrixProjectionCP12.Type.ConditionalTransitionMatrix;
  if(conditional&&(regime<0||!AtomRegimePolicyCP12.POLICY_HASH.equals(part.group(5))))throw new IllegalArgumentException("MATRIX_REGIME5_POLICY_REQUIRED");
  String bootstrap="";
  if(MatrixRegistrationR75.spec(spec.type,spec.partition).matrixId.equals(spec.matrixId)){
   // R75 registration pins this lower bound once. Later draws cannot move it past unconsumed input.
   // The existing lag reader still obtains the real preceding draw outside the retained ring.
   bootstrap=" AND d.issue>=COALESCE((SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key='"+MatrixRegistrationR75.bootstrapKey(spec)+"'),'')";
  }
  String[] args={spec.matrixId,spec.matrixId};Models.Draw draw=null;
  try(Cursor c=db.rawQuery("SELECT d.issue,d.open_time,d.fetch_time,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.dirty=0 "+(lag>0?" AND d.issue>(SELECT prior.issue FROM "+DbHelper.TABLE_DRAWS+" prior WHERE prior.dirty=0 ORDER BY prior.issue LIMIT 1 OFFSET "+(lag-1)+")":"")+(conditional?" AND EXISTS(SELECT 1 FROM research_matrix_regime5_cp12 r WHERE r.target_issue=d.issue AND r.position="+position+" AND r.policy_hash='"+AtomRegimePolicyCP12.POLICY_HASH+"' AND r.regime_id="+regime+" AND r.captured_at<d.open_time)":"")+bootstrap+" AND d.issue>COALESCE((SELECT through_issue FROM research_matrix_checkpoint_v2 WHERE matrix_id=?),'') AND NOT EXISTS(SELECT 1 FROM research_matrix_input_cp12 i WHERE i.matrix_id=? AND i.source_issue=d.issue) ORDER BY d.issue LIMIT 1",args)){
   if(c.moveToFirst()){draw=new Models.Draw();draw.issue=c.getString(0);draw.openTime=c.getLong(1);draw.fetchTime=c.getLong(2);for(int i=0;i<10;i++)draw.numbers[i]=c.getInt(3+i);}
  }if(draw==null)return null;if(!draw.isValid()||draw.dirty!=0)throw new IllegalStateException("MATRIX_HISTORY_TRUTH_INVALID");
  long ordinal=issueOrdinal(draw.issue);int actual=draw.getNumberAtPosition(position)-1,prior=0;
  StringBuilder proof=new StringBuilder("CP12_HISTORY_MATRIX|").append(draw.issue).append('|').append(draw.numbersText());
  if(lag>0){int n=0;String previous=draw.issue;try(Cursor c=db.rawQuery("SELECT issue,n"+sourcePosition+" FROM "+DbHelper.TABLE_DRAWS+" WHERE issue<? AND dirty=0 ORDER BY issue DESC LIMIT ?",new String[]{draw.issue,Integer.toString(lag)})){
    while(c.moveToNext()){String older=c.getString(0);if(!TargetIssueResolver.exact(older,previous).valid)return null;prior=c.getInt(1)-1;if(prior<0||prior>9)throw new IllegalStateException("MATRIX_HISTORY_TRUTH_INVALID");proof.append('|').append(older).append(':').append(prior);previous=older;n++;}}
   if(n!=lag)return null;
  }
  if(conditional){try(Cursor r=db.rawQuery("SELECT policy_hash,history_hash,score_hash,regime_id,scores,captured_at FROM research_matrix_regime5_cp12 WHERE target_issue=? AND position=?",new String[]{draw.issue,Integer.toString(position)})){
   if(!r.moveToFirst()||!AtomRegimePolicyCP12.POLICY_HASH.equals(r.getString(0))||r.getInt(3)!=regime||r.getLong(5)<=0||r.getLong(5)>=draw.openTime)throw new IllegalStateException("REGIME5_SOURCE_PROOF");
   byte[] scores=r.getBlob(4);if(scores==null||scores.length!=40)throw new IllegalStateException("REGIME5_SCORE_SHAPE");java.nio.ByteBuffer buffer=java.nio.ByteBuffer.wrap(scores);double sum=0;int best=0;double[] values=new double[5];StringBuilder identity=new StringBuilder(AtomRegimePolicyCP12.POLICY).append('|').append(draw.issue).append('|').append(position).append('|').append(r.getString(1));for(int i=0;i<5;i++){values[i]=buffer.getDouble();if(!Double.isFinite(values[i])||values[i]<0||values[i]>1)throw new IllegalStateException("REGIME5_SCORE_VALUE");sum+=values[i];identity.append('|').append(Double.toHexString(values[i]));if(values[i]>values[best])best=i;}if(Math.abs(sum-1)>1e-9||best!=regime||!r.getString(2).equals(MatrixProjectionCP12.hash(identity.toString())))throw new IllegalStateException("REGIME5_SCORE_HASH");proof.append('|').append(r.getString(0)).append('|').append(r.getString(2)).append('|').append(r.getLong(5));
  }}
  String ph=MatrixProjectionCP12.hash(proof.toString());String receipt=MatrixProjectionCP12.hash("CP12_HISTORY_RECEIPT|"+spec.matrixId+'|'+ph);
  MatrixProjectionCP12.Sample row=new MatrixProjectionCP12.Sample(ordinal,0,(spec.type==MatrixProjectionCP12.Type.TransitionMatrix||conditional)?prior:actual,actual,conditional?regime:actual,0,0,0,0,0);
  return new PreparedInput(spec,draw.issue,receipt,ph,ordinal,row);
 }
 static PreparedInput prepareFormalCandidateInputCP12(SQLiteDatabase db,MatrixProjectionCP12.Spec spec){
  String freeze=spec.partition;if(!hash(freeze))return null;
  String sql="SELECT d.issue,w.window_id,b.plan_hash,l.identity_hash,p.candidate_hash,p.position,p.n1,p.n2,p.n3,p.selected_hit_p,p.calibration_hash,p.frozen_at,p.candidate_hit,p.settled_at,t.settlement_hash,t.completed_at,d.open_time,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10 FROM research_fixed_window_r932 w JOIN research_cp4_window_binding b ON b.window_id=w.window_id JOIN research_cp4_frozen_lifetime l ON l.lifetime_id=b.lifetime_id JOIN "+DbHelper.TABLE_DRAWS+" d ON d.issue BETWEEN b.first_issue AND b.last_issue AND d.issue BETWEEN l.first_issue AND l.last_issue JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t ON t.target_issue=d.issue LEFT JOIN research_fixed_pair_r932 p ON p.window_id=w.window_id AND p.target_issue=d.issue WHERE w.structural_hash=? AND w.formal_plan_revision='R967_FORMAL_PLAN_V1' AND t.status='COMPLETED' AND d.dirty=0 AND d.issue>COALESCE((SELECT through_issue FROM research_matrix_checkpoint_v2 WHERE matrix_id=?),'') AND NOT EXISTS(SELECT 1 FROM research_matrix_input_cp12 i WHERE i.matrix_id=? AND i.source_issue=d.issue) ORDER BY d.issue,b.stage LIMIT 1";
  try(Cursor c=db.rawQuery(sql,new String[]{freeze,spec.matrixId,spec.matrixId})){
   if(!c.moveToFirst())return null;String issue=c.getString(0),window=c.getString(1),plan=c.getString(2),identity=c.getString(3),terminal=c.getString(14);long completed=c.getLong(15),opened=c.getLong(16);
   if(!hash(plan)||!hash(identity)||!hash(terminal)||opened<=0||completed<opened)throw new IllegalStateException("MATRIX_FORMAL_TERMINAL_PROOF");
   long ordinal=issueOrdinal(issue);int flags=MatrixProjectionCP12.ELIGIBLE,actual=0,mask=0;double p=0;String source;
   if(c.isNull(4)){flags|=MatrixProjectionCP12.MISSING;source="MISSING_CAPTURE|"+window;}
   else{
    int position=c.getInt(5),n1=c.getInt(6),n2=c.getInt(7),n3=c.getInt(8),hit=c.getInt(12);p=c.getDouble(9);String rawProbability=c.isNull(9)?"NULL":Double.toHexString(p);String pred=c.getString(4),cal=c.getString(10);long captured=c.getLong(11);
    if(hit==-1)return null; // committed draw waits for the unique Future-label owner; never counted as a miss.
    if(!DecisionGateEngine.b12K3(n1,n2,n3)||position<1||position>10||!hash(pred)||captured<=0||captured>=opened||c.getLong(13)<opened)throw new IllegalStateException("MATRIX_FORMAL_SOURCE_PROOF_INVALID");
    actual=c.getInt(16+position);mask=AppConfig.buildK3Mask(n1,n2,n3);if(actual<1||actual>10||hit!=((mask&(1<<(actual-1)))!=0?1:0))throw new IllegalStateException("MATRIX_FORMAL_SOURCE_LABEL_DRIFT");
    if(Double.isFinite(p)&&p>=0&&p<=1&&hash(cal)){flags|=MatrixProjectionCP12.OBSERVED;if(hit==1)flags|=MatrixProjectionCP12.HIT_A;}
    else{p=0;} // masked unavailable probability; no Brier, hit or calibration contribution.
    source=pred+'|'+mask+'|'+rawProbability+'|'+cal+'|'+captured+'|'+actual+'|'+hit+'|'+(flags&MatrixProjectionCP12.OBSERVED);
   }
   String proof=MatrixProjectionCP12.hash("CP12_FORMAL_SOURCE|"+issue+'|'+freeze+'|'+window+'|'+source+'|'+terminal+'|'+completed+'|'+plan+'|'+identity);
   return new PreparedInput(spec,issue,MatrixProjectionCP12.hash("CP12_FORMAL_MATRIX_RECEIPT|"+spec.matrixId+'|'+proof),proof,ordinal,new MatrixProjectionCP12.Sample(ordinal,flags,Math.max(0,actual-1),0,0,mask,0,p,0,0));
  }
 }
 static PreparedInput preparePairedFormalCP12(SQLiteDatabase db,MatrixProjectionCP12.Spec spec){
  String[] pair=spec.partition.split("\\|",-1);if(pair.length!=2||!hash(pair[0])||!hash(pair[1])||pair[0].equals(pair[1]))return null;
  if(spec.type==MatrixProjectionCP12.Type.CoOccurrenceMatrix||spec.type==MatrixProjectionCP12.Type.CandidateSimilarityMatrix||spec.type==MatrixProjectionCP12.Type.CandidateDiversityMatrix){if(pair[0].compareTo(pair[1])>=0)throw new IllegalArgumentException("MATRIX_PAIR_CANONICAL_ORDER");}
  String issue=null;try(Cursor c=db.rawQuery("SELECT DISTINCT a.target_issue FROM research_fixed_pair_r932 a JOIN research_fixed_window_r932 wa ON wa.window_id=a.window_id JOIN research_fixed_pair_r932 b ON b.target_issue=a.target_issue JOIN research_fixed_window_r932 wb ON wb.window_id=b.window_id WHERE wa.structural_hash=? AND wb.structural_hash=? AND a.candidate_hit IN(0,1) AND b.candidate_hit IN(0,1) AND a.target_issue>COALESCE((SELECT through_issue FROM research_matrix_checkpoint_v2 WHERE matrix_id=?),'') AND NOT EXISTS(SELECT 1 FROM research_matrix_input_cp12 i WHERE i.matrix_id=? AND i.source_issue=a.target_issue) ORDER BY a.target_issue LIMIT 1",new String[]{pair[0],pair[1],spec.matrixId,spec.matrixId})){if(c.moveToFirst())issue=c.getString(0);}
  if(issue==null)return null;Formal a=loadFormalCP12(db,pair[0],issue),b=loadFormalCP12(db,pair[1],issue);if(a==null||b==null)return null;
  if(a.position!=b.position)throw new IllegalStateException("MATRIX_PAIR_POSITION_MISMATCH");
  if(a.actual!=b.actual)throw new IllegalStateException("MATRIX_PAIR_LABEL_SNAPSHOT_CHANGED");
  if(spec.type==MatrixProjectionCP12.Type.ParentChildMatrix){try(Cursor lineage=db.rawQuery("SELECT 1 FROM r974_model_lineage l JOIN r967_frozen_evaluation_v2 child ON child.learner_id=l.child_id JOIN r967_frozen_evaluation_v2 parent ON parent.learner_id=l.parent_id WHERE parent.structural_hash=? AND child.structural_hash=? AND l.created_issue<=? AND child.created_issue<=?",new String[]{pair[0],pair[1],issue,issue})){if(!lineage.moveToFirst())throw new IllegalStateException("MATRIX_PARENT_CHILD_LINEAGE_PROOF");}}
  if(spec.type==MatrixProjectionCP12.Type.TeacherMatrix&&!MatrixRegistrationR75.teacherPair(db,pair[0],pair[1],issue))throw new IllegalStateException("MATRIX_TEACHER_RELATION_PROOF");
  int flags=MatrixProjectionCP12.ELIGIBLE|MatrixProjectionCP12.OBSERVED|MatrixProjectionCP12.PAIRED|(a.hit==1?MatrixProjectionCP12.HIT_A:0)|(b.hit==1?MatrixProjectionCP12.HIT_B:0);
  long ordinal=issueOrdinal(issue);String proof=MatrixProjectionCP12.hash("CP12_PAIRED_FORMAL|"+a.proof+'|'+b.proof+'|'+spec.type);
  return new PreparedInput(spec,issue,MatrixProjectionCP12.hash("CP12_PAIRED_MATRIX_RECEIPT|"+spec.matrixId+'|'+proof),proof,ordinal,new MatrixProjectionCP12.Sample(ordinal,flags,a.actual-1,0,0,a.mask,b.mask,a.p,b.p,0));
 }
 private static final class Formal {final int position,actual,mask,hit;final double p;final String proof;Formal(int position,int actual,int mask,int hit,double p,String proof){this.position=position;this.actual=actual;this.mask=mask;this.hit=hit;this.p=p;this.proof=proof;}}
 private static Formal loadFormalCP12(SQLiteDatabase db,String freeze,String issue){
  String sql="SELECT p.candidate_hash,p.position,p.n1,p.n2,p.n3,p.selected_hit_p,p.calibration_hash,p.frozen_at,p.candidate_hit,p.settled_at,t.settlement_hash,t.completed_at,d.open_time,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10,b.plan_hash,l.identity_hash,l.first_issue,l.last_issue FROM research_fixed_pair_r932 p JOIN research_fixed_window_r932 w ON w.window_id=p.window_id JOIN research_cp4_window_binding b ON b.window_id=w.window_id JOIN research_cp4_frozen_lifetime l ON l.lifetime_id=b.lifetime_id JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t ON t.target_issue=p.target_issue JOIN "+DbHelper.TABLE_DRAWS+" d ON d.issue=p.target_issue WHERE w.structural_hash=? AND p.target_issue=? AND w.formal_plan_revision='R967_FORMAL_PLAN_V1' AND p.target_issue BETWEEN b.first_issue AND b.last_issue AND p.target_issue BETWEEN l.first_issue AND l.last_issue AND p.candidate_hit IN(0,1) AND p.settled_at>0 AND t.status='COMPLETED' AND d.dirty=0 ORDER BY b.stage LIMIT 1";
  try(Cursor c=db.rawQuery(sql,new String[]{freeze,issue})){if(!c.moveToFirst())return null;String pred=c.getString(0),cal=c.getString(6),terminal=c.getString(10),plan=c.getString(23),identity=c.getString(24);int position=c.getInt(1),a=c.getInt(2),b=c.getInt(3),d=c.getInt(4),hit=c.getInt(8);double p=c.getDouble(5);long at=c.getLong(7),opened=c.getLong(12),complete=c.getLong(11);
   if(position<1||position>10||!DecisionGateEngine.b12K3(a,b,d)||!Double.isFinite(p)||p<0||p>1||!hash(pred)||!hash(cal)||!hash(terminal)||!hash(plan)||!hash(identity)||at<=0||at>=opened||complete<opened||c.getLong(9)<opened)throw new IllegalStateException("MATRIX_PAIRED_FORMAL_PROOF");
   int actual=c.getInt(12+position),mask=AppConfig.buildK3Mask(a,b,d);if(actual<1||actual>10||hit!=((mask&(1<<(actual-1)))!=0?1:0))throw new IllegalStateException("MATRIX_PAIRED_LABEL_DRIFT");
   String proof=MatrixProjectionCP12.hash("CP12_FORMAL_PAIRED_SOURCE|"+issue+'|'+freeze+'|'+pred+'|'+mask+'|'+Double.toHexString(p)+'|'+cal+'|'+at+'|'+actual+'|'+hit+'|'+terminal+'|'+complete+'|'+plan+'|'+identity);return new Formal(position,actual,mask,hit,p,proof);
  }
 }
 public static void enqueueFailureCP12(SQLiteDatabase db,String issue,String reason,String domain,String regimePolicyHash,String regimeLabel,
     int severity,String failureReceiptHash,long canonicalIssueOrdinal,Long recoveredLagMicros){
  if(!db.inTransaction()||!TargetIssueResolver.isValid(issue)||!reason.matches("[A-Z][A-Z0-9_]{0,63}")||!(domain.equals("MODEL")||domain.equals("SYSTEM"))||!hash(regimePolicyHash)||!regimeLabel.matches("[A-Z][A-Z0-9_]{0,31}")||severity<0||severity>3||!hash(failureReceiptHash)||canonicalIssueOrdinal!=issueOrdinal(issue))throw new IllegalArgumentException("MATRIX_FAILURE_SOURCE_IDENTITY");
  String unknown=MatrixProjectionCP12.hash("CP12_REGIME_UNAVAILABLE_V1");if(regimeLabel.equals("UNKNOWN")!=regimePolicyHash.equals(unknown))throw new IllegalArgumentException("MATRIX_FAILURE_REGIME_POLICY");
  String partition=reason+"|domain:"+domain+"|regimePolicy:"+regimePolicyHash+"|regime:"+regimeLabel;
  MatrixProjectionCP12.Spec spec=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.FailureMatrix,partition,5000,MatrixProjectionCP12.hash("CP12_FAILURE_MATRIX_V1"));
  String receipt=MatrixProjectionCP12.hash("CP12_FAILURE_EVENT|"+failureReceiptHash);long eventOrdinal;int flags=0;double lag=0;
  try(Cursor existing=db.rawQuery("SELECT source_ordinal,input_payload FROM research_matrix_input_cp12 WHERE matrix_id=? AND source_receipt_hash=?",new String[]{spec.matrixId,receipt})){
   if(existing.moveToFirst()){eventOrdinal=MatrixProjectionCP12.sampleFromBytes(existing.getBlob(1)).ordinal;if(recoveredLagMicros==null)return;}
   else{if(recoveredLagMicros!=null)throw new IllegalStateException("MATRIX_RECOVERY_ORIGINAL_ARCHIVED_OR_MISSING");eventOrdinal=nextFailureSequenceCP12(db);}
  }
  long sourceOrdinal=eventOrdinal;if(recoveredLagMicros!=null){String recoveryReceipt=MatrixProjectionCP12.hash("CP12_FAILURE_RECOVERY|"+failureReceiptHash+'|'+recoveredLagMicros);try(Cursor priorRecovery=db.rawQuery("SELECT 1 FROM research_matrix_input_cp12 WHERE matrix_id=? AND source_receipt_hash=?",new String[]{spec.matrixId,recoveryReceipt})){if(priorRecovery.moveToFirst())return;}if(recoveredLagMicros<0)throw new IllegalArgumentException("MATRIX_RECOVERY_NEGATIVE_LAG");flags|=MatrixProjectionCP12.RECOVERED;lag=recoveredLagMicros;sourceOrdinal=nextFailureSequenceCP12(db);receipt=MatrixProjectionCP12.hash("CP12_FAILURE_RECOVERY|"+failureReceiptHash+'|'+recoveredLagMicros);}
  String proof=MatrixProjectionCP12.hash("CP12_FAILURE_SOURCE|"+failureReceiptHash+'|'+issue+'|'+canonicalIssueOrdinal+'|'+partition+'|'+severity+'|'+(recoveredLagMicros==null?"UNRECOVERED":recoveredLagMicros));
  MatrixSnapshotBusR952.enqueueCP12(db,spec,issue,sourceOrdinal,receipt,proof,new MatrixProjectionCP12.Sample(eventOrdinal,flags,severity,0,0,0,0,0,0,lag),null,System.currentTimeMillis());
 }
 private static long nextFailureSequenceCP12(SQLiteDatabase db){long previous=0;try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key='cp12_matrix_failure_sequence'",null)){if(c.moveToFirst())previous=Long.parseLong(c.getString(0));}if(previous>=9007199254740000L)throw new IllegalStateException("MATRIX_FAILURE_SEQUENCE_OVERFLOW");long next=previous+1;db.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES('cp12_matrix_failure_sequence',?,?)",new Object[]{Long.toString(next),System.currentTimeMillis()});return next;}
 public static final String ATOM_SCORE5_POLICY_HASH=MatrixProjectionCP12.hash("CP12_ATOM_REGIME_SCORE5_CONTINUOUS_V1|"+AtomAblationReplayCP12.POLICY_HASH);
 public static String atomSourceScopeHashCP12(String freezeHash,CandidateCompilerR952.Model model,FeatureManifestR952 manifest,String binding){if(!hash(freezeHash)||model==null||model.isLegacy()||manifest==null||!(hash(binding)||"NONE".equals(binding)))throw new IllegalArgumentException("ATOM_SOURCE_SCOPE");return MatrixProjectionCP12.hash("CP12_ATOM_SOURCE_SCOPE|"+freezeHash+'|'+model.hash()+'|'+manifest.hash+'|'+model.modern().normalizerHash+'|'+binding+'|'+AtomAblationReplayCP12.POLICY_HASH);}
 public static String atomBinPolicyHash(String normalizerHash,int featureIndex){if(!hash(normalizerHash)||featureIndex<0||featureIndex>31)throw new IllegalArgumentException("ATOM_BIN_POLICY");return MatrixProjectionCP12.hash("CP12_FROZEN_ZSCORE_BINS_V1|"+normalizerHash+'|'+featureIndex+"|-3,-2,-1,-.5,0,.5,1,2,3");}
 static PreparedInput prepareFrozenAtomCP12(SQLiteDatabase db,MatrixProjectionCP12.Spec spec){
  String[] part=spec.partition.split("\\|",-1);boolean target=spec.type==MatrixProjectionCP12.Type.AtomTargetMatrix;
  boolean scopedTarget=target&&part.length==4&&part[1].startsWith("freeze:")&&hash(part[1].substring(7));
  if(part.length!=4||!hash(part[0])||(!hash(part[1])&&!scopedTarget))return null;
  int digit=-1;String label=null,nodeId=null;boolean general=false;if(target){if(!part[2].matches("digit:[0-9]")||!part[3].startsWith("bins:")||!hash(part[3].substring(5)))return null;digit=Integer.parseInt(part[2].substring(6));}
  else if(part[2].matches("node:n[0-9]{1,3}")&&part[3].equals("policy:"+ATOM_ALL_STUDY_POLICY_HASH)){general=true;nodeId=part[2].substring(5);}
  else{if(!part[2].startsWith("label:")||!part[3].equals("policy:"+ATOM_SCORE5_POLICY_HASH))return null;label=part[2].substring(6);if(!Arrays.asList(AtomRegimePolicyCP12.LABELS).contains(label))throw new IllegalArgumentException("ATOM_SCORE5_TEMPLATE_LABEL");}
  String formalFields=general?",q.window_id,b.plan_hash,l.identity_hash,q.calibration_hash,q.selected_hit_p,q.candidate_hit,q.settled_at,q.frozen_at":"";
  String formalJoin=general?" JOIN research_fixed_window_r932 w ON w.structural_hash=f.structural_hash JOIN research_cp4_window_binding b ON b.window_id=w.window_id JOIN research_cp4_frozen_lifetime l ON l.lifetime_id=b.lifetime_id JOIN research_fixed_pair_r932 q ON q.window_id=w.window_id AND q.target_issue=p.target_issue":"";
  String formalGuard=general?" AND f.structural_hash=? AND l.candidate_key=('algorithm:'||f.candidate_id) AND w.formal_plan_revision='R967_FORMAL_PLAN_V1' AND q.candidate_hash=p.record_hash AND q.position=p.position AND q.n1=p.n1 AND q.n2=p.n2 AND q.n3=p.n3 AND q.candidate_hit IN(0,1) AND q.frozen_at<d.open_time AND q.settled_at>=d.open_time AND w.registered_at<=q.frozen_at AND p.target_issue BETWEEN b.first_issue AND b.last_issue AND p.target_issue BETWEEN l.first_issue AND l.last_issue":"";
  if(scopedTarget)formalGuard=" AND f.structural_hash=?";
  String sql="SELECT p.target_issue,p.record_hash,p.label_hash,p.context_hash,p.frozen_context,p.captured_at,p.position,p.settlement_hash,f.ast,f.model_payload,f.structural_hash,d.open_time,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10,t.completed_at,p.n1,p.n2,p.n3,p.score"+formalFields+" FROM r958_frozen_predraw p JOIN r967_frozen_candidate_all f ON f.candidate_id=p.candidate_id JOIN "+DbHelper.TABLE_DRAWS+" d ON d.issue=p.target_issue JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t ON t.target_issue=p.target_issue"+formalJoin+" WHERE p.settled=1 AND p.proof_error='' AND p.context_hash<>'' AND p.target_issue>COALESCE((SELECT through_issue FROM research_matrix_checkpoint_v2 WHERE matrix_id=?),'') AND NOT EXISTS(SELECT 1 FROM research_matrix_input_cp12 i WHERE i.matrix_id=? AND i.source_issue=p.target_issue) AND d.dirty=0 AND t.status='COMPLETED' AND p.settlement_hash=t.settlement_hash"+formalGuard+" ORDER BY p.target_issue,p.candidate_id LIMIT 64";
  try(Cursor c=db.rawQuery(sql,(general||scopedTarget)?new String[]{spec.matrixId,spec.matrixId,scopedTarget?part[1].substring(7):part[1]}:new String[]{spec.matrixId,spec.matrixId})){while(c.moveToNext()){
   String issue=c.getString(0),contextHash=c.getString(3),json=c.getString(4);long captured=c.getLong(5),opened=c.getLong(11);int position=c.getInt(6);
   if(json==null||json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>262144||!contextHash.equals(V51ChecksumEngine.sha256(json))||captured<=0||captured>=opened||c.getLong(22)<opened||!hash(c.getString(1))||!hash(c.getString(2))||!hash(c.getString(7)))throw new IllegalStateException("ATOM_FROZEN_SOURCE_CONTEXT_PROOF");
   CandidateAstR952 ast=CandidateAstR952.parse(c.getString(8));CandidateCompilerR952.Model model=CandidateCompilerR952.Model.parse(c.getString(9));if(ast.formatVersion!=3||model.isLegacy())continue;
   Map<String,Object> context=StrictCanonicalJson.readContextObject(json);if(!CandidateAstR952.str(context,"target").equals(issue)||!CandidateAstR952.str(context,"freezeHash").equals(c.getString(10))||!CandidateAstR952.str(context,"modelHash").equals(model.hash())||model.modern().trainedUntil.compareTo(issue)>=0)throw new IllegalStateException("ATOM_FROZEN_MODEL_PROOF");
   List<AtomSpecCP4> specs=new ArrayList<>();for(Object o:CandidateAstR952.array(context.get("specs"))){Map<String,Object> r=CandidateAstR952.object(o);specs.add(AtomSpecCP4.fromRegistry(CandidateAstR952.str(r,"atomId"),CandidateAstR952.str(r,"specHash"),CandidateAstR952.str(r,"specJson"),CandidateAstR952.str(r,"outputType"),CandidateAstR952.integer(r,"minimumHistory"),CandidateAstR952.str(r,"missingPolicy"),CandidateAstR952.str(r,"state")));}
   CandidateCompilerR952.Program program=CandidateCompilerR952.compile(ast,FeatureManifestR952.forAst(ast),specs);model.modern().requireManifest(program.manifest);
   if(!program.codeHash.equals(CandidateAstR952.str(context,"programHash"))||!program.manifest.hash.equals(CandidateAstR952.str(context,"manifestHash"))||!model.modern().normalizerHash.equals(CandidateAstR952.str(context,"normalizerHash")))throw new IllegalStateException("ATOM_FROZEN_PROGRAM_PROOF");
   for(com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeSpec node:ast.graphSpec.nodes){if(node.kind!=com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeKind.ATOM)continue;AtomSpecCP4 atom=program.specById(node.source);if(!atom.specHash.equals(part[0]))continue;
    if(general){if(!node.id.equals(nodeId)||!consumedAtomsCP12(ast).contains(node))continue;}else if(!target&&(!atom.family.equals("REGIME_SCORE")||!atom.mathSpec.regime.equals(label)))continue;
    String binding=atomBindingIdentityCP12(db,ast,program.manifest,node,atom);if(general){if(!atomStudySpecCP12(c.getString(10),model,program.manifest,atom,binding,node).matrixId.equals(spec.matrixId))continue;}else if(scopedTarget){int feature=ast.graphSpec.featureOutputIds.indexOf(node.id);if(feature<0||node.shape.elements()!=1||!MatrixRegistrationR75.atomTargetSpec(c.getString(10),model,program.manifest,atom,binding,feature,digit).matrixId.equals(spec.matrixId))continue;}else if(!atomSourceScopeHashCP12(c.getString(10),model,program.manifest,binding).equals(part[1]))continue;
    Map<String,Object> raw=CandidateAstR952.object(CandidateAstR952.object(context.get("atoms")).get(node.qualifiedId()));List<?> values=CandidateAstR952.array(raw.get("values")),present=CandidateAstR952.array(raw.get("present"));if(values.size()!=node.shape.elements()||present.size()!=values.size()||(!general&&values.size()!=1))throw new IllegalStateException("ATOM_FROZEN_SHAPE");double[] numeric=new double[values.size()];boolean[] masks=new boolean[values.size()];double scale=0;int observedN=0;for(int i=0;i<numeric.length;i++){if(!(values.get(i) instanceof String)||!(present.get(i) instanceof Boolean))throw new IllegalStateException("ATOM_FROZEN_ENCODING");numeric[i]=Double.parseDouble((String)values.get(i));masks[i]=(Boolean)present.get(i);if(!Double.isFinite(numeric[i])||(!masks[i]&&numeric[i]!=0))throw new IllegalStateException("ATOM_FROZEN_VALUE_PROOF");if(masks[i]){observedN++;scale=Math.max(scale,Math.abs(numeric[i]));}}boolean observed=observedN==numeric.length;double x=general?0:numeric[0];if(general&&observedN>0&&scale>0){double squares=0;for(int i=0;i<numeric.length;i++)if(masks[i]){double normalized=numeric[i]/scale;squares+=normalized*normalized;}x=scale*Math.sqrt(squares/observedN);}Object atRaw=raw.get("availableAt");long at=((Number)atRaw).longValue();String source=CandidateAstR952.str(raw,"sourceHash"),maxIssue=CandidateAstR952.str(raw,"maxSourceIssue");
    TypedAtomMoleculeGraphR930.Value v=new TypedAtomMoleculeGraphR930.Value(numeric,masks,maxIssue,at,0,source);
    if(!Double.isFinite(x)||at<=0||at>=opened||!TargetIssueResolver.isValid(maxIssue)||maxIssue.compareTo(issue)>=0||!v.valueHash.equals(CandidateAstR952.str(raw,"valueHash")))throw new IllegalStateException("ATOM_FROZEN_VALUE_PROOF");
    int bin=0,flags=observed?0:MatrixProjectionCP12.MISSING;if(target){int feature=ast.graphSpec.featureOutputIds.indexOf(node.id);if(feature<0||feature>=model.modern().featureN||!part[3].substring(5).equals(atomBinPolicyHash(model.modern().normalizerHash,feature)))continue;
     double z=(x-model.modern().means()[feature])/model.modern().standardDeviations()[feature];double[] edge={-3,-2,-1,-.5,0,.5,1,2,3};while(bin<edge.length&&z>=edge[bin])bin++;int actual=c.getInt(11+position)-1;if(actual==digit)flags|=MatrixProjectionCP12.HIT_A;
    }
    double ablation=0;String ablationProof="UNAVAILABLE";if(!target&&observed){int selected=AppConfig.buildK3Mask(c.getInt(23),c.getInt(24),c.getInt(25));try{AtomAblationReplayCP12.Contrast contrast=AtomAblationReplayCP12.contrast(program,model.modern(),context,contextHash,node.qualifiedId(),selected,c.getInt(11+position),c.getDouble(26),opened);ablation=contrast.delta;ablationProof=contrast.proof;flags|=MatrixProjectionCP12.PAIRED;}catch(IllegalStateException unavailable){if(!Arrays.asList("ATOM_ABLATION_UNAVAILABLE","ATOM_ABLATION_OUTPUT_MISSING","ATOM_ABLATION_REQUIRED_MISSING").contains(unavailable.getMessage()))throw unavailable;ablationProof=unavailable.getMessage();}}
    String formalProof="LEGACY_ATOM_SOURCE";if(general){int actual=c.getInt(11+position),mask=AppConfig.buildK3Mask(c.getInt(23),c.getInt(24),c.getInt(25));if(!hash(c.getString(28))||!hash(c.getString(29))||!hash(c.getString(30))||!Double.isFinite(c.getDouble(31))||c.getDouble(31)<0||c.getDouble(31)>1||c.getInt(32)!=((mask&(1<<(actual-1)))!=0?1:0))throw new IllegalStateException("ATOM_STUDY_FORMAL_FUTURE_PROOF");formalProof=c.getString(27)+'|'+c.getString(28)+'|'+c.getString(29)+'|'+c.getString(30)+'|'+Double.toHexString(c.getDouble(31))+'|'+c.getInt(32)+'|'+c.getLong(33)+'|'+c.getLong(34);}
    long ordinal=issueOrdinal(issue);String proof=MatrixProjectionCP12.hash("CP12_ATOM_SOURCE|"+formalProof+'|'+contextHash+'|'+v.valueHash+'|'+c.getString(1)+'|'+c.getString(2)+'|'+c.getString(7)+'|'+spec.partition+'|'+bin+'|'+flags+'|'+ablationProof);
    return new PreparedInput(spec,issue,MatrixProjectionCP12.hash("CP12_ATOM_MATRIX_RECEIPT|"+spec.matrixId+'|'+proof),proof,ordinal,new MatrixProjectionCP12.Sample(ordinal,flags,0,0,bin,0,0,x,0,ablation));
   }
  }}return null;
 }
 public static String atomBindingIdentityCP12(SQLiteDatabase db,CandidateAstR952 ast,FeatureManifestR952 manifest,TypedAtomMoleculeGraphR930.NodeSpec node,AtomSpecCP4 atom){
  for(MatrixBindingsCP12.Ref ref:MatrixBindingsCP12.validate(ast,manifest))if(ref.node().equals(node.qualifiedId()))return ref.manifest;
  if(!atom.requiresBinding)return "NONE";List<String> keys=new ArrayList<>();for(String binding:manifest.bindingHashes)try(Cursor c=db.rawQuery("SELECT atom_id FROM research_atom_binding_v2 WHERE binding_hash=?",new String[]{binding})){if(c.moveToFirst()&&atom.atomId.equals(c.getString(0)))keys.add(binding);}if(keys.isEmpty())throw new IllegalStateException("ATOM_BOUND_SOURCE_IDENTITY");Collections.sort(keys);return keys.size()==1?keys.get(0):MatrixProjectionCP12.hash("CP12_ATOM_BINDING_SET|"+keys);
 }
 static PreparedInput prepareRuleHealthCP12(SQLiteDatabase db,MatrixProjectionCP12.Spec spec){
  String freeze=spec.partition;if(!hash(freeze))return null;
  String sql="SELECT o.target_issue,o.candidate_key,o.execution_started,o.frozen,o.status,o.reason,o.record_hash,p.score,f.structural_hash,t.settlement_hash,t.completed_at,d.open_time FROM research_capture_opportunity_r932 o JOIN r967_frozen_candidate_all f ON o.candidate_key=('algorithm:'||f.candidate_id) LEFT JOIN r958_frozen_predraw p ON p.candidate_id=f.candidate_id AND p.target_issue=o.target_issue JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t ON t.target_issue=o.target_issue JOIN "+DbHelper.TABLE_DRAWS+" d ON d.issue=o.target_issue WHERE f.structural_hash=? AND o.execution_started=1 AND t.status='COMPLETED' AND d.dirty=0 AND o.target_issue>COALESCE((SELECT through_issue FROM research_matrix_checkpoint_v2 WHERE matrix_id=?),'') AND NOT EXISTS(SELECT 1 FROM research_matrix_input_cp12 i WHERE i.matrix_id=? AND i.source_issue=o.target_issue) ORDER BY o.target_issue LIMIT 1";
  try(Cursor c=db.rawQuery(sql,new String[]{freeze,spec.matrixId,spec.matrixId})){if(!c.moveToFirst())return null;String issue=c.getString(0),status=c.getString(4),reason=c.getString(5),record=c.getString(6),terminal=c.getString(9);if(!hash(terminal)||c.getLong(10)<c.getLong(11))throw new IllegalStateException("RULE_HEALTH_TERMINAL_PROOF");
   int flags=0,bin=0;if(c.getInt(3)!=1||c.isNull(7)){flags|=MatrixProjectionCP12.MISSING;}else{double p=c.getDouble(7);if(!Double.isFinite(p)||p<0||p>1||!hash(record))throw new IllegalStateException("RULE_HEALTH_FROZEN_SCORE");bin=Math.min(9,(int)Math.floor(p*10));}
   if(Arrays.asList("HASH_MISMATCH","PROOF_REJECTED","MODEL_FEATURE_HASH_MISMATCH","INTEGRITY_FAILED").contains(reason))flags|=MatrixProjectionCP12.INTEGRITY;
   long ordinal=issueOrdinal(issue);String proof=MatrixProjectionCP12.hash("CP12_RULE_HEALTH|"+freeze+'|'+issue+'|'+status+'|'+reason+'|'+record+'|'+terminal+'|'+bin+'|'+flags);
   return new PreparedInput(spec,issue,MatrixProjectionCP12.hash("CP12_RULE_HEALTH_RECEIPT|"+spec.matrixId+'|'+proof),proof,ordinal,new MatrixProjectionCP12.Sample(ordinal,flags,0,0,bin,0,0,0,0,0));
  }
 }
 static PreparedInput prepareLatencyCP12(SQLiteDatabase db,MatrixProjectionCP12.Spec spec){
  java.util.regex.Matcher part=java.util.regex.Pattern.compile("stage:([A-Z][A-Z0-9_]{0,63})\\|unit:(WALL_MICROS|CPU_MICROS)\\|device:ANDROID\\|release:([0-9]{1,9})").matcher(spec.partition);if(!part.matches())return null;String stage=part.group(1),unit=part.group(2),release=part.group(3);
  String sql="SELECT rowid,execution_id,target_issue,stage_samples,budget_hash,finished_at FROM "+RuntimeResourceAuditR927.TABLE+" WHERE release_code=? AND finished_at>0 AND stage_samples LIKE ? AND rowid>=COALESCE((SELECT source_ordinal/1024 FROM research_matrix_input_cp12 WHERE matrix_id=? ORDER BY source_ordinal DESC LIMIT 1),0) ORDER BY rowid LIMIT 64";
  try(Cursor c=db.rawQuery(sql,new String[]{release,"%CP12EX1|"+stage+"|%",spec.matrixId})){while(c.moveToNext()){
   String issue=c.getString(2),execution=c.getString(1),spans=c.getString(3),budget=c.getString(4);if(!TargetIssueResolver.isValid(issue)||!hash(budget)||spans.length()>65536)throw new IllegalStateException("LATENCY_EXCLUSIVE_RECEIPT_BOUND");String[] entries=spans.split(";",-1);if(entries.length>512)throw new IllegalStateException("LATENCY_SPAN_LIMIT");
   for(int index=0;index<entries.length;index++){String[] span=entries[index].split("\\|",-1);if(span.length!=6||!span[0].equals("CP12EX1")||!span[1].equals(stage))continue;
    long wall=Long.parseLong(span[2]),cpu=Long.parseLong(span[3]),thread=Long.parseLong(span[4]);if(wall<0||cpu<-1||thread<0||!(span[5].equals("SYNC")||span[5].equals("QUEUE")))throw new IllegalStateException("LATENCY_EXCLUSIVE_SPAN_PROOF");long nanos=unit.equals("WALL_MICROS")?wall:cpu;if(nanos<0)continue;
    long ordinal=Math.addExact(Math.multiplyExact(c.getLong(0),1024L),index);String receipt=MatrixProjectionCP12.hash("CP12_EXCLUSIVE_LATENCY|"+spec.matrixId+'|'+execution+'|'+index);
    try(Cursor old=db.rawQuery("SELECT 1 FROM research_matrix_input_cp12 WHERE matrix_id=? AND source_receipt_hash=?",new String[]{spec.matrixId,receipt})){if(old.moveToFirst())continue;}
    try(Cursor ack=db.rawQuery("SELECT 1 FROM research_projection_ack_v2 WHERE consumer=? AND source_hash=?",new String[]{"MATRIX_CP4/"+spec.matrixId,receipt})){if(ack.moveToFirst())continue;}
    String proof=MatrixProjectionCP12.hash("CP12_EXCLUSIVE_LATENCY_SOURCE|"+execution+'|'+issue+'|'+entries[index]+'|'+budget+'|'+c.getLong(5));return new PreparedInput(spec,issue,receipt,proof,ordinal,new MatrixProjectionCP12.Sample(ordinal,0,0,0,0,0,0,nanos/1000L,0,0));
   }
  }}return null;
 }
 private static boolean hash(String s){return s!=null&&s.matches("[0-9a-f]{64}");}
}
