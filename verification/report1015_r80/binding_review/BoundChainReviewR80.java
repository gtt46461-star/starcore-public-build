package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.JSONObject;
/** Independent original-producer exercise against normal Gradle product bytecode.
 * Draws and the two seed learners are explicitly synthetic host fixtures. Every
 * source prediction, bound receipt, child registration and training is a product
 * owner call. This creates no live Future, certificate, REAL trade or quality claim. */
public final class BoundChainReviewR80 {
 static int checks;
 static String h(String s){return V51ChecksumEngine.sha256(s);}
 static Object mutable(Object v){if(v instanceof Map){Map<String,Object> m=new TreeMap<>();for(Map.Entry<?,?> e:((Map<?,?>)v).entrySet())m.put((String)e.getKey(),mutable(e.getValue()));return m;}if(v instanceof List){List<Object> l=new ArrayList<>();for(Object x:(List<?>)v)l.add(mutable(x));return l;}return v;}
 static String issue(int n){String s="202609230001";for(int i=1;i<n;i++)s=TargetIssueResolver.next(s).targetIssue;return s;}
 static void ok(boolean b,String message){if(!b)throw new AssertionError(message);checks++;System.out.println("PASS "+message);}
 static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 static void tx(SQLiteDatabase db,Runnable body){db.beginTransaction();try{body.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
 static DbStore store(SQLiteDatabase db,String files,String assets)throws Exception{
  DbStore s=HostProductR974.store(db);File root=new File(files);root.mkdirs();android.content.Context app=new android.app.Application(root,new File(assets));
  HostProductR974.set(s,"researchAppCP12",app);HostProductR974.set(s.coordinatorForResearchR952(),"app",app);HostProductR974.set(s,"researchInputRootCP11",new File(root,"research-input-v2"));s.researchInputRootCP11().mkdirs();return s;
 }
 static Models.Draw draw(SQLiteDatabase db,int index,long open,boolean settle){
  Models.Draw d=new Models.Draw();d.issue=issue(index);d.openTime=open;d.fetchTime=Math.max(open+1,System.currentTimeMillis());d.sourceLine="R80_EXPLICIT_SYNTHETIC_HOST";d.rawJson="synthetic fixture draw "+index;
  // Predetermined calibration regime shift deliberately makes both real
  // fitted calibrators fail held-out quality, without forging quality rows.
  int first=index>=442&&index<=561?9:index>=562&&index<=682?1:index%10+1;
  for(int k=0;k<10;k++)d.numbers[k]=(first-1+k)%10+1;
  DrawCanonicalizerR811.PersistenceDecision canonical=DrawCanonicalizerR811.observeWithinWrite(db,d);if(!canonical.confirmed)throw new AssertionError("fixture canonicalizer "+d.issue);
  db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,source_line,raw_json,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,draw_hash_version,draw_hash_v2)VALUES(?,?,?,?,?,?,0,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{d.issue,d.openTime,d.fetchTime,d.fetchTime,d.sourceLine,d.rawJson,d.numbers[0],d.numbers[1],d.numbers[2],d.numbers[3],d.numbers[4],d.numbers[5],d.numbers[6],d.numbers[7],d.numbers[8],d.numbers[9],DrawHashR36.CURRENT_VERSION,DrawHashR36.sha256V2(d)});
  if(settle){terminal(db,d);settleLabels(db,d);}
  return d;
 }
 static void terminal(SQLiteDatabase db,Models.Draw d){String terminal=h("R80_SYNTHETIC_AUTHORITATIVE_TERMINAL|"+d.issue);long at=Math.max(d.fetchTime,System.currentTimeMillis());db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash)VALUES(?,?,'COMPLETED',?,?,?,?,?)",new Object[]{d.issue,d.issue,at,at,at,DrawHashR36.sha256V2(d),terminal});}
 static void settleLabels(SQLiteDatabase db,Models.Draw d){FrozenCandidateAdapterR958.settle(db,d,h("R80_SYNTHETIC_AUTHORITATIVE_TERMINAL|"+d.issue));CandidateCalibrationR932.settle(db,d);}
 static void verifyDelayedLabel(SQLiteDatabase db,DbStore s,int index,long at){
  Models.Draw[] pending=new Models.Draw[1];tx(db,()->{pending[0]=draw(db,index,at,false);terminal(db,pending[0]);});
  tx(db,()->DrawDependencyInputCP12.preserveIfReferencedWithinWrite(db,n(db,"SELECT rowid FROM draws WHERE issue='"+issue(index)+"'"),issue(index)));
  ok(n(db,"SELECT COUNT(*) FROM "+DrawDependencyInputCP12.TABLE+" WHERE issue='"+issue(index)+"'")==1,"pending bound source pins exact evicted-draw dependency");
  BoundAtomLifecycleR80.Cohort c=BoundAtomLifecycleR80.activeR80(db);List<AtomSpecCP4> specs=new ArrayList<>();
  try(Cursor rows=db.rawQuery("SELECT atom_id,spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE position=?",new String[]{Integer.toString(c.ast.position)})){while(rows.moveToNext()){AtomSpecCP4 spec=AtomSpecCP4.fromRegistry(rows.getString(0),rows.getString(1),rows.getString(2),rows.getString(3),rows.getInt(4),rows.getString(5),rows.getString(6));if(c.ast.graphSpec.manifest.atomSpecHashes.contains(spec.specHash))specs.add(spec);}}
  CandidateCompilerR952.Program program=CandidateCompilerR952.compile(c.ast,c.ast.graphSpec.manifest,specs);long used=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used();boolean waited=false;
  try{BoundAtomLifecycleR80.prepareInputsR80(db,c,program,issue(index+1),System.currentTimeMillis());}catch(DevelopmentProductionCP12.Wait wait){waited="WAIT_BOUND_SOURCE_SETTLEMENT".equals(wait.reason);}
  ok(waited,"delayed original label yields typed settlement wait rather than corruption");ok(RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used()==used,"delayed label releases input memory leases");ok(BoundAtomLifecycleR80.activeR80(db).hash.equals(c.hash),"delayed label preserves active cohort");tx(db,()->settleLabels(db,pending[0]));tx(db,()->DrawDependencyInputCP12.releaseConsumedWithinWrite(db,32));ok(n(db,"SELECT COUNT(*) FROM "+DrawDependencyInputCP12.TABLE+" WHERE issue='"+issue(index)+"'")==0,"settled authoritative source releases raw draw dependency");
 }
 static void addSpec(SQLiteDatabase db,AtomSpecCP4 s){db.execSQL("INSERT OR IGNORE INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state)VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{s.atomId,s.family,s.version,s.canonicalSpec,s.position,s.outputType,s.minimumHistory,s.specHash,s.missingPolicy,s.state});}
 static void seed(SQLiteDatabase db,DbStore s,String assets)throws Exception{
  tx(db,()->{Models.Draw unrelated=new Models.Draw();unrelated.issue=issue(1);unrelated.openTime=Long.MAX_VALUE;ok(BoundAtomLifecycleR80.settleR80(db,unrelated,"unrelated-legacy-terminal")==0,"optional bound settlement imposes no clocks or authority on unrelated legacy catch-up");});
  tx(db,()->{
   // The existing independently reviewed FIXED_GRAPH owner fixture consumes this
   // explicit five-draw atom. Bound specs below are the exact shipped catalog bytes.
   String spec="{\"context\":\"PIT_DRAW\",\"family\":\"FREQUENCY\",\"params\":{\"digit\":0,\"window\":5},\"position\":1,\"regime\":\"ALL\",\"transform\":\"IDENTITY\",\"version\":\"CP2\"}";
   addSpec(db,AtomSpecCP4.fromRegistry("ATOM2-"+h(spec).substring(0,24),h(spec),spec,"SCALAR",5,"INSUFFICIENT_HISTORY; NEVER_DEFAULT_ZERO","DISCOVERED"));
  });
  try(BufferedReader reader=Files.newBufferedReader(Path.of(assets,"cp4_atom_registry.jsonl"))){String line;List<AtomSpecCP4> specs=new ArrayList<>();while((line=reader.readLine())!=null){AtomSpecCP4 spec=AtomSpecCP4.fromAssetLine(line);if(spec.position==1&&spec.requiresBinding)specs.add(spec);}ok(specs.size()==13,"shipped position1 catalog contains exactly13 bound atoms");tx(db,()->{for(AtomSpecCP4 spec:specs)addSpec(db,spec);});}
  MainlineCaptureR75.seed(db,"r80-independent-parent-a",false);
  String initialAst=System.getProperty("starcore.test.ast");Map<String,Object> variant=(Map<String,Object>)mutable(StrictCanonicalJson.readObject(Files.readString(Path.of(initialAst)).substring(5)));
  @SuppressWarnings("unchecked") List<Map<String,Object>> nodes=(List<Map<String,Object>>)(List<?>)variant.get("nodes");nodes.get(1).put("parameters",Collections.singletonList(Double.toHexString(2.01)));
  @SuppressWarnings("unchecked") Map<String,Object> manifest=(Map<String,Object>)variant.get("featureManifest");manifest.put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(variant));
  Path variantFile=Path.of(s.researchInputRootCP11().getParent(),"independent-parent-b.ast");Files.writeString(variantFile,"AST3:"+StrictCanonicalJson.write(variant));
  System.setProperty("starcore.test.ast",variantFile.toString());try{MainlineCaptureR75.seed(db,"r80-independent-parent-b",false);}finally{System.setProperty("starcore.test.ast",initialAst);}
  long base=System.currentTimeMillis()-1000L;tx(db,()->{for(int i=1;i<=441;i++)draw(db,i,base-(441-i)*60000L,true);});
  String bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash;
  for(int i=442;i<=682;i++){
   final int index=i;String target=issue(i);DeferredEvidenceSnapshotR36.Snapshot snapshot=MainlineCaptureR75.snapshot(issue(i-1),target,bundle);
   tx(db,()->{db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,1,2,3,?,?)",new Object[]{target,bundle,snapshot.recordHash,selector,h("R80_SYNTHETIC_CHAMPION|"+target),System.currentTimeMillis()});GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,target,bundle,selector);});
   int captured=FrozenCandidateAdapterR958.captureReservedCP12(s,snapshot);if(captured!=2)throw new AssertionError("calibration capture expected2 actual="+captured+" target="+target);
   long at=System.currentTimeMillis()+1;Thread.sleep(2);tx(db,()->draw(db,index,at,true));
   if(i%30==0)System.out.println("CALIBRATION_PRODUCER target="+target+" observations="+n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS));
  }
  CandidateCalibrationR932.fitOne(s);CandidateCalibrationR932.fitOne(s);
  ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.QUALITY_CP12+" WHERE state='FITTED_BUT_NOT_QUALIFIED'")==2,"both source parents genuinely fitted but not qualified by actual calibration owner");
  ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE state IN('RESERVED','ACTIVE')")==0,"completed calibration releases both reservation slots");
  ok(n(db,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW)==482,"482 actual original calibration predictions remain purpose-separated");
  BoundAtomLifecycleR80.Cohort cohort=BoundAtomLifecycleR80.prepareAndRegisterR80(s,issue(682));
  ok(cohort.state.equals("CAPTURING")&&cohort.ready==0&&cohort.first.equals(issue(683)),"new cohort starts prospectively with zero ready features");
  ok(BoundAtomLifecycleR80.occupiedR80(db)==2,"two source parents count within existing capture capacity");
  ok(n(db,"SELECT COUNT(*) FROM research_atom_binding_v2")==14,"13 consumed atoms bind14 exact source hashes");
  ok(n(db,"SELECT COUNT(*) FROM research_atom_spec_v2 WHERE state='ACTIVE'")==0,"binding registration does not confer atom ACTIVE admission");
  ok(n(db,"SELECT COUNT(*) FROM r952_candidate")==2&&n(db,"SELECT COUNT(*) FROM research_action_v2")==0,"preregistration invents no child action or candidate");
 }
 static void verifySourceReplay(SQLiteDatabase db,DbStore s,String target){
  BoundAtomLifecycleR80.Cohort c=BoundAtomLifecycleR80.activeR80(db);String parent=BoundAtomLifecycleR80.parentsR80(db,c).get(0);FrozenCandidateAdapterR958.Frozen f=FrozenCandidateAdapterR958.load(db,parent);
  try(Cursor r=db.rawQuery("SELECT input_hash,context_json,context_hash,n1,n2,n3,score,captured_at,deadline_at FROM "+BoundAtomLifecycleR80.SOURCE+" WHERE cohort_hash=? AND candidate_id=? AND target_issue=?",new String[]{c.hash,parent,target})){ok(r.moveToFirst(),"original source replay row exists");String input=r.getString(0),context=r.getString(1),contextHash=r.getString(2);int n1=r.getInt(3),n2=r.getInt(4),n3=r.getInt(5);double score=r.getDouble(6);long captured=r.getLong(7),deadline=r.getLong(8);
   tx(db,()->BoundAtomLifecycleR80.commitSourceR80(db,c,f,target,input,context,contextHash,n1,n2,n3,score,captured,deadline));ok(true,"exact original source replay accepted idempotently");
   boolean rejected=false;db.beginTransaction();try{BoundAtomLifecycleR80.commitSourceR80(db,c,f,target,input,context,contextHash,n1,n2,n3,score<.5?score+.001:score-.001,captured,deadline);}catch(IllegalArgumentException conflict){rejected=conflict.getMessage().contains("BOUND_SOURCE_REPLAY_CONFLICT");}finally{db.endTransaction();}ok(rejected,"different original source replay rejected without overwrite");
  }
 }
 static void capture(SQLiteDatabase db,DbStore s,int start,int end)throws Exception{
  BoundAtomLifecycleR80.Cohort prior=BoundAtomLifecycleR80.activeR80(db);ok(prior!=null,"reopened durable cohort exists");String original=prior.hash;
  long formal=n(db,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW),cal=n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS),modelBudget=RuntimeMemoryGuardR811.algorithmModelBudget().used(),atomBudget=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used();
  String bundle=ModelBundleRuntimeR932.current().hash;
  for(int i=start;i<=end;i++){
   final int index=i;DeferredEvidenceSnapshotR36.Snapshot snapshot=MainlineCaptureR75.snapshot(issue(i-1),issue(i),bundle);
   int captured=FrozenCandidateAdapterR958.captureBoundSourcesR80(s,snapshot);if(captured!=2)throw new AssertionError("bound source capture expected2 actual="+captured+" target="+issue(i));
   if(i==start)ok(FrozenCandidateAdapterR958.captureBoundSourcesR80(s,snapshot)==0,"same-target owner retry creates no duplicate source or feature receipt");
   if(i==start)verifySourceReplay(db,s,issue(i));
   if(i==1403){BoundAtomLifecycleR80.Cohort selected=BoundAtomLifecycleR80.activeR80(db);boolean waiting=false;try{BoundAtomLifecycleR80.trainingHistoryR80(db,selected,issue(1404),721);}catch(DevelopmentProductionCP12.Wait expected){waiting="WAIT_BOUND_SOURCE_SETTLEMENT".equals(expected.reason);}ok(waiting,"pending fixed selection target yields typed original-history wait before draw arrival");ok(BoundAtomLifecycleR80.activeR80(db).state.equals("READY"),"pending fixed selection preserves ready original cohort");}
   long at=System.currentTimeMillis()+1;Thread.sleep(2);if(i==start)verifyDelayedLabel(db,s,index,at);else tx(db,()->draw(db,index,at,true));
   if(i==1403)ok(BoundAtomLifecycleR80.trainingHistoryR80(db,BoundAtomLifecycleR80.activeR80(db),issue(1404),721).size()==721,"genuine selection settlement unlocks identical finite original history");
   BoundAtomLifecycleR80.Cohort c=BoundAtomLifecycleR80.activeR80(db);if(!c.hash.equals(original))throw new AssertionError("cohort changed");
   int expected=Math.min(421,Math.max(0,i-982));if(c.ready!=expected)throw new AssertionError("ready prefix target="+i+" expected="+expected+" actual="+c.ready);
   if(i%30==0||i==end)System.out.println("BOUND_ORIGINAL_PRODUCER target="+issue(i)+" ready="+c.ready+" state="+c.state);
  }
  ok(n(db,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW)==formal&&n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==cal,"bound role adds no frozen formal/calibration observations");
  ok(RuntimeMemoryGuardR811.algorithmModelBudget().used()==modelBudget&&RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used()==atomBudget,"original source and typed receipt leases released after every successful capture");
  BoundAtomLifecycleR80.Cohort c=BoundAtomLifecycleR80.activeR80(db);if(end==1403){ok(c.state.equals("READY")&&c.ready==421&&c.selectionIssue.equals(issue(1403)),"300-source warmup precedes421 consecutive original feature targets");ok(n(db,"SELECT COUNT(*) FROM "+BoundAtomLifecycleR80.INPUT+" WHERE available=0")==300,"all300 warmup targets persist truthful typed unavailable receipts");ok(n(db,"SELECT COUNT(*) FROM "+BoundAtomLifecycleR80.INPUT+" WHERE available=1")==421,"exact421 original pre-draw feature targets captured");}
 }
 static void register(SQLiteDatabase db,DbStore s)throws Exception{
  BoundAtomLifecycleR80.Cohort before=BoundAtomLifecycleR80.activeR80(db);ok(before.state.equals("READY")||before.state.equals("PROPOSED"),"registration resumes ready or proposed cohort");String cohort=before.hash;
  int turns=0;for(;turns<430;turns++){
   String status=BoundAtomLifecycleR80.maintainR80(s,System.nanoTime()+10_000_000_000L);BoundAtomLifecycleR80.Cohort c=BoundAtomLifecycleR80.readR80(db,cohort);
   if(turns%30==0)System.out.println("BOUND_REGISTER turn="+turns+" state="+c.state+" status="+status);
   if(c.state.equals("REGISTERED"))break;if(c.state.equals("RETIRED"))throw new AssertionError("cohort retired "+status);
   if(!c.selectionIssue.equals(issue(1403)))throw new AssertionError("selection boundary moved");
  }
  BoundAtomLifecycleR80.Cohort c=BoundAtomLifecycleR80.readR80(db,cohort);ok(c.state.equals("REGISTERED"),"actual fresh child registration completed after original prefix");
  ok(n(db,"SELECT future_n FROM r952_candidate WHERE candidate_id='"+c.child+"'")==0,"fresh child Future starts at zero");
  ok(n(db,"SELECT COUNT(*) FROM research_training_registration_row_cp12 WHERE action_id='"+c.action+"'")==420,"300 train and120 validation original receipts published; embargo excluded");
  Map<String,Object> protocol=StrictCanonicalJson.readContextObject(text(db,"SELECT protocol_json FROM research_training_protocol_v2 WHERE candidate_id='"+c.child+"'"));
  ok("BOUND_TAIL300".equals(protocol.get("inputSelectionPolicy"))&&((Number)protocol.get("trainEndIndex")).intValue()-((Number)protocol.get("trainFirstIndex")).intValue()==300,"original BOUND_TAIL300 protocol preserved");
  ok(issue(1403).equals(protocol.get("sourceIssue"))&&issue(1404).equals(protocol.get("selectionBoundary")),"fresh action keeps fixed post-prefix selection boundary");
 }
 static void train(SQLiteDatabase db,DbStore s)throws Exception{
  BoundAtomLifecycleR80.Cohort c=BoundAtomLifecycleR80.activeR80(db);String id=c.child,target=TargetIssueResolver.next(s.getLatestDrawIssue()).targetIssue;boolean done=false;
  for(int turn=0;turn<180&&!done;turn++){
   ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(1000);
   try{ResearchTaskQueueR967.Lease lease=s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R80_REVIEW_CLAIM_TRAIN",d->ResearchTaskQueueR967.claimFor(d,"TRAIN_CANDIDATE",id,System.currentTimeMillis()));if(lease!=null){ResearchTrainingHookRegistryR952.DevelopmentBatchCP11 b=DevelopmentProductionCP12.prepareBatch(s,lease,System.nanoTime()+850_000_000L);if(b!=null)s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R80_REVIEW_COMMIT_TRAIN",d->ResearchDirectorR952.commitCandidateBatchCP11(d,lease,b));}}
   finally{ResearchWorkBudgetR927.leave(scope);}
   done="COMPLETED".equals(text(db,"SELECT status FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND candidate_id='"+id+"'"));
   if(turn%10==0||done)System.out.println("BOUND_TRAIN turn="+turn+" done="+done+" featureRows="+n(db,"SELECT COUNT(*) FROM research_training_feature_row_cp11"));if(!done)Thread.sleep(270);
  }
  ok(done,"bound child's actual numerical training completes");ResearchTaskQueueR967.Lease freeze=s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R80_REVIEW_CLAIM_FREEZE",d->ResearchTaskQueueR967.claimFor(d,"FREEZE_CANDIDATE",id,System.currentTimeMillis()));ok(freeze!=null,"normal freeze task exists");
  try(FrozenCandidateAdapterR958.PreparedFreezeCP8 prepared=FrozenCandidateAdapterR958.prepareDevelopmentFreezeCP8(s,freeze,target)){String f=s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R80_REVIEW_FREEZE",d->FrozenCandidateAdapterR958.commitDevelopmentFreezeCP8(d,freeze,prepared));ok(f!=null&&!f.isEmpty(),"normal frozen child consumes the registered bound graph");}
  ok(n(db,"SELECT COUNT(*) FROM research_development_receipt_v2 p JOIN research_training_protocol_v2 t ON p.protocol_hash=t.protocol_hash WHERE t.candidate_id='"+id+"'")==1,"one actual development receipt");
  ok(n(db,"SELECT future_n FROM r952_candidate WHERE candidate_id='"+id+"'")==0,"training and freezing do not inherit parent Future");
 }
 static void retirement(SQLiteDatabase db,DbStore s)throws Exception{
  BoundAtomLifecycleR80.Cohort cohort=BoundAtomLifecycleR80.activeR80(db);ok(cohort!=null&&cohort.state.equals("REGISTERED"),"bound child exists before retirement regression");
  String initial=System.getProperty("starcore.test.ast");Map<String,Object> variant=(Map<String,Object>)mutable(StrictCanonicalJson.readObject(Files.readString(Path.of(initial)).substring(5)));List<Map<String,Object>> nodes=(List<Map<String,Object>>)(List<?>)variant.get("nodes");nodes.get(1).put("parameters",Collections.singletonList(Double.toHexString(2.03)));Map<String,Object> manifest=(Map<String,Object>)variant.get("featureManifest");manifest.put("graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(variant));Path healthy=Path.of(s.researchInputRootCP11().getParent(),"independent-healthy-retirement.ast");Files.writeString(healthy,"AST3:"+StrictCanonicalJson.write(variant));System.setProperty("starcore.test.ast",healthy.toString());try{MainlineCaptureR75.seed(db,"r80-retirement-healthy",false);}finally{System.setProperty("starcore.test.ast",initial);}
  long now=System.currentTimeMillis()-2;tx(db,()->draw(db,1404,now,true));String target=issue(1405),bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash;DeferredEvidenceSnapshotR36.Snapshot snapshot=MainlineCaptureR75.snapshot(issue(1404),target,bundle);
  tx(db,()->{db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,1,2,3,?,?)",new Object[]{target,bundle,snapshot.recordHash,selector,h("R80_RETIREMENT_CHAMPION|"+target),System.currentTimeMillis()});GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,target,bundle,selector);});
  String childFreeze=text(db,"SELECT candidate_id FROM "+ResearchEvaluationRunR967.EVAL+" WHERE learner_id='"+cohort.child+"' ORDER BY created_at DESC LIMIT 1");String childKey="algorithm:"+childFreeze;
  ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE state IN('RESERVED','ACTIVE')")==2,"batch contains bound child and independent healthy candidate");long originals=n(db,"SELECT COUNT(*) FROM "+BoundAtomLifecycleR80.SOURCE),inputs=n(db,"SELECT COUNT(*) FROM "+BoundAtomLifecycleR80.INPUT);
  // Simulates a persisted retired cohort encountered by the next capture callback,
  // without invoking today's eager cleanup; this is the reported upgrade counterexample.
  tx(db,()->db.execSQL("UPDATE "+BoundAtomLifecycleR80.COHORT+" SET state='RETIRED',last_reason='R80_REVIEW_RETIRED_DEPENDENCY',generation=generation+1 WHERE cohort_hash=?",new Object[]{cohort.hash}));
  ok(!ResearchEvaluationRunR967.mayCapture(db,FrozenCandidateAdapterR958.load(db,childFreeze),target),"retired bound child fails ordinary frozen eligibility");
  ok(FrozenCandidateAdapterR958.captureReservedCP12(s,snapshot)==1,"healthy candidate capture continues past retired bound child");
  ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE candidate_key='"+childKey+"' AND state='RETIRED'")==1,"existing scheduler releases retired bound child slot");
  tx(db,()->GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,target,bundle,selector));ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE candidate_key='"+childKey+"' AND state IN('RESERVED','ACTIVE')")==0,"scheduler does not readmit retired bound dependency");
  ok(n(db,"SELECT COUNT(*) FROM "+BoundAtomLifecycleR80.SOURCE)==originals&&n(db,"SELECT COUNT(*) FROM "+BoundAtomLifecycleR80.INPUT)==inputs,"retirement preserves every original bound source and input");ok(n(db,"SELECT future_n FROM r952_candidate WHERE candidate_id='"+cohort.child+"'")==0,"retirement does not rewrite child Future evidence");
 }
 public static void main(String[] a)throws Exception{
  String mode=a[0];RealtimePressureGateR811.resetForTests();try(SQLiteDatabase db=new SQLiteDatabase(a[1],a[2])){
   db.setForeignKeyConstraintsEnabled(true);DbStore s=store(db,a[3],a[4]);ok(BoundAtomLifecycleR80.readyR80(db),"exact bound schema installed by product migration owner");
   if(mode.equals("seed"))seed(db,s,a[4]);else if(mode.equals("capture_first"))capture(db,s,683,1042);else if(mode.equals("capture_resume"))capture(db,s,1043,1403);else if(mode.equals("register"))register(db,s);else if(mode.equals("train"))train(db,s);else if(mode.equals("retirement"))retirement(db,s);else throw new IllegalArgumentException(mode);
   ok(n(db,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(db,"SELECT COUNT(*) FROM trades")==0,"no certificate or REAL manufactured");
   JSONObject result=new JSONObject().put("status","PASS").put("checks",checks).put("mode",mode).put("scope","ACTUAL_PRODUCT_ORIGINAL_PRODUCER_SYNTHETIC_HOST_NOT_LIVE_FUTURE_OR_DEVICE_TIMING").put("sourceRows",n(db,"SELECT COUNT(*) FROM "+BoundAtomLifecycleR80.SOURCE)).put("typedInputs",n(db,"SELECT COUNT(*) FROM "+BoundAtomLifecycleR80.INPUT));Files.writeString(Path.of(a[5]),result.toString()+"\n");System.out.println(result);
  }
 }
}
