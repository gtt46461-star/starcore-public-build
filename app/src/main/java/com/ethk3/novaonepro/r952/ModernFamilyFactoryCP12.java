package com.ethk3.novaonepro.r952;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;import java.util.*;import java.io.*;
import static com.ethk3.novaonepro.r952.ModernSeedFactoryCP12.*;
/** Fifteen executable foundation families, consumed only by the existing Director IMPORT slot.
 * No scheduler, model publication, Future counter, certificate or REAL capability. */
public final class ModernFamilyFactoryCP12 {
 private ModernFamilyFactoryCP12(){}
 public static final String WHEEL="research_family_wheel_cp12",CURSOR="research_family_cursor_cp12",CREATION="research_family_creation_cp12";
 public static final int LIMIT=40;private static final int[] WINDOWS={50,120,300,500};
 public static void schema(SQLiteDatabase db){need(db.inTransaction(),"FAMILY_SCHEMA_TX");
  db.execSQL("CREATE TABLE IF NOT EXISTS "+WHEEL+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),next_family INTEGER NOT NULL CHECK(next_family BETWEEN 0 AND 14),generation INTEGER NOT NULL DEFAULT 0)");
  db.execSQL("INSERT OR IGNORE INTO "+WHEEL+" VALUES(1,0,0)");
  db.execSQL("CREATE TABLE IF NOT EXISTS "+CURSOR+"(family TEXT PRIMARY KEY,next_variant INTEGER NOT NULL DEFAULT 0 CHECK(next_variant BETWEEN 0 AND 40),last_reason TEXT NOT NULL DEFAULT '',generation INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL)");
  db.execSQL("CREATE TABLE IF NOT EXISTS "+CREATION+"(family TEXT NOT NULL,source_hash TEXT NOT NULL CHECK(length(source_hash)=64),variant INTEGER NOT NULL CHECK(variant BETWEEN 0 AND 39),ast_hash TEXT NOT NULL CHECK(length(ast_hash)=64),ast TEXT NOT NULL CHECK(length(CAST(ast AS BLOB))<=32768),action_id TEXT NOT NULL UNIQUE REFERENCES research_action_v2(action_id),candidate_id TEXT NOT NULL UNIQUE,source_issue TEXT NOT NULL,state TEXT NOT NULL CHECK(state IN('PROPOSED','REGISTERED','NO_OP','HELD')),last_reason TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(family,source_hash),UNIQUE(family,variant))");
  db.execSQL("CREATE INDEX IF NOT EXISTS cp12_family_state ON "+CREATION+"(family,state,variant)");
  db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_family_creation_identity BEFORE UPDATE ON "+CREATION+" WHEN OLD.family<>NEW.family OR OLD.source_hash<>NEW.source_hash OR OLD.variant<>NEW.variant OR OLD.ast_hash<>NEW.ast_hash OR OLD.ast<>NEW.ast OR OLD.action_id<>NEW.action_id OR OLD.candidate_id<>NEW.candidate_id OR OLD.source_issue<>NEW.source_issue BEGIN SELECT RAISE(ABORT,'FAMILY_CREATION_IMMUTABLE'); END");
  db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_family_creation_no_delete BEFORE DELETE ON "+CREATION+" BEGIN SELECT RAISE(ABORT,'FAMILY_CREATION_PERMANENT'); END");
  db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_family_creation_no_replace BEFORE INSERT ON "+CREATION+" WHEN EXISTS(SELECT 1 FROM "+CREATION+" c WHERE c.family=NEW.family AND(c.source_hash=NEW.source_hash OR c.variant=NEW.variant)) BEGIN SELECT RAISE(ABORT,'FAMILY_CREATION_REPLACE_FORBIDDEN'); END");
  for(CandidateAstR952.Family family:CandidateAstR952.Family.values())db.execSQL("INSERT OR IGNORE INTO "+CURSOR+"(family,updated_at)VALUES(?,?)",new Object[]{family.name(),System.currentTimeMillis()});
 }
 public static final class Proposal {
  public final CandidateAstR952 ast;public final String sourceHash;public final List<AtomSpecCP4> specs;
  Proposal(CandidateAstR952 a,String source,List<AtomSpecCP4>s){ast=a;sourceHash=source;specs=Collections.unmodifiableList(new ArrayList<>(s));}
 }
 /** Compiles actual mathematical graphs; family labels alone never supply a feature. */
 public static Proposal prepare(SQLiteDatabase db,CandidateAstR952.Family family,int variant){
  need(!db.inTransaction()&&variant>=0&&variant<LIMIT,"FAMILY_PREPARE_INPUT");guardComplete(db);int position=variant%10+1,w=WINDOWS[variant/10];
  if(family==CandidateAstR952.Family.BANDIT||family==CandidateAstR952.Family.EXPERT||family==CandidateAstR952.Family.ENSEMBLE)return teachers(db,family,position);
  if(family==CandidateAstR952.Family.LINEAR||family==CandidateAstR952.Family.LOGISTIC||family==CandidateAstR952.Family.TREE||family==CandidateAstR952.Family.NEURAL||family==CandidateAstR952.Family.HYBRID){
   String raw=null;try(Cursor c=db.rawQuery("SELECT seed_hash,source_seed_id,source_hash,graph_json FROM research_molecule_seed_v2 WHERE compiler_revision=? AND state='VALIDATED' ORDER BY seed_hash LIMIT 1 OFFSET ?",new String[]{REVISION,Integer.toString(variant)})){if(c.moveToFirst())raw=StrictCanonicalJson.write(fields("graphHash",c.getString(0),"seedId",c.getString(1),"sourceSha256",c.getString(2),"graph",StrictCanonicalJson.readObject(c.getString(3))));}
   if(raw==null)throw new DevelopmentProductionCP12.Wait("WAIT_FEATURE_SEED");SeedRow seed=new SeedRow(raw,variant,db);
   CandidateAstR952 ast=learnedGraphCP12(seed.ast,family,variant,new Builder(db,seed.position),registered(db,seed.ast));return new Proposal(ast,seed.hash,registered(db,ast));
  }
  return fixedGraphCP12(new Builder(db,position),family,position,w);
 }
 /** The same family mathematics is used by foundation creation and Failure-Research NEW_FAMILY.
  * Pure input: immutable parent graph, verified registered specs and exact frozen teacher refs. */
 public static CandidateAstR952 newFamilyFromParentCP12(CandidateAstR952 parent,CandidateAstR952.Family family,List<AtomSpecCP4> registry,List<CandidateCompilerR952.ExpertRef> frozenParents,int variant){
  need(parent!=null&&parent.formatVersion==3&&family!=null&&registry!=null&&variant>=0&&variant<LIMIT,"NEW_FAMILY_INPUT");
  if(family==CandidateAstR952.Family.BANDIT||family==CandidateAstR952.Family.EXPERT||family==CandidateAstR952.Family.ENSEMBLE){
   List<Object> refs=new ArrayList<>();int wanted=family==CandidateAstR952.Family.ENSEMBLE?3:2;TreeSet<String> seen=new TreeSet<>();
   if(frozenParents!=null)for(CandidateCompilerR952.ExpertRef r:frozenParents){if(r==null||r.position!=parent.position||!seen.add(r.freezeHash))continue;refs.add(fields("freezeHash",r.freezeHash,"modelHash",r.modelHash,"featureManifestHash",r.featureManifestHash,"position",r.position));if(refs.size()==wanted)break;}
   if(refs.size()!=wanted)throw new DevelopmentProductionCP12.Wait("WAIT_FROZEN_TEACHER");return teacherGraphCP12(family,parent.position,refs);
  }
  Builder builder=new Builder(registry,parent.position);
  if(family==CandidateAstR952.Family.LINEAR||family==CandidateAstR952.Family.LOGISTIC||family==CandidateAstR952.Family.TREE||family==CandidateAstR952.Family.NEURAL||family==CandidateAstR952.Family.HYBRID)return learnedGraphCP12(parent,family,variant,builder,registeredCP12(parent,registry));
  return fixedGraphCP12(builder,family,parent.position,WINDOWS[variant/10]).ast;
 }
 private static List<AtomSpecCP4> registeredCP12(CandidateAstR952 ast,List<AtomSpecCP4> registry){TreeMap<String,AtomSpecCP4> byId=new TreeMap<>();for(AtomSpecCP4 s:registry)byId.put(s.atomId,s);TreeMap<String,AtomSpecCP4> used=new TreeMap<>();for(com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeSpec n:ast.graphSpec.nodes)if(n.kind==com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeKind.ATOM){AtomSpecCP4 s=byId.get(n.source);if(s==null)throw new DevelopmentProductionCP12.Wait("WAIT_ATOM_REGISTRATION");used.put(n.source,s);}return new ArrayList<>(used.values());}
 private static CandidateAstR952 learnedGraphCP12(CandidateAstR952 seed,CandidateAstR952.Family family,int variant,Builder b,List<AtomSpecCP4> specs){
  return learnedGraphCP12(seed,family,variant,b,specs,null);
 }
 /** A new DEV-only child may remove a proven never-observed parent feature.
  * The parent AST/model/checkpoint is never edited. Catalog inputs retain truthful masks. */
 public static CandidateAstR952 repairFeatureCoverageR78(SQLiteDatabase db,CandidateAstR952 parent,Set<String> retained){
  need(db!=null&&!db.inTransaction()&&parent!=null&&parent.formatVersion==3&&retained!=null,"DEV_REPAIR_GRAPH_INPUT");
  need(Arrays.asList(CandidateAstR952.Family.LINEAR,CandidateAstR952.Family.LOGISTIC,CandidateAstR952.Family.TREE,CandidateAstR952.Family.NEURAL,CandidateAstR952.Family.HYBRID).contains(parent.family),"DEV_REPAIR_LEARNED_FAMILY");
  need(parent.graphSpec.featureOutputIds.containsAll(retained),"DEV_REPAIR_UNKNOWN_FEATURE");
  return learnedGraphCP12(parent,parent.family,0,new Builder(db,parent.position),registered(db,parent),retained);
 }
 private static CandidateAstR952 learnedGraphCP12(CandidateAstR952 seed,CandidateAstR952.Family family,int variant,Builder b,List<AtomSpecCP4> specs,Set<String> retained){
  Map<String,Object> root=new TreeMap<>(seed.graphSpec.canonicalObject());root.put("family",family.name());root.put("modelKind",family==CandidateAstR952.Family.LOGISTIC?"OVR_LOGISTIC":family==CandidateAstR952.Family.TREE?"HISTOGRAM_TREE":family==CandidateAstR952.Family.NEURAL?"MLP_D32_16_10":"SOFTMAX_LINEAR");root.put("experts",Collections.emptyList());root.put("outputId","@MODEL");
  LinkedHashSet<String> features=new LinkedHashSet<>();
  // Preserve scalar parent outputs instead of silently throwing all but one away.
  for(Object raw:list(root.get("featureOutputIds"))){String id=(String)raw;if(retained!=null&&!retained.contains(id))continue;String type=str(map(list(root.get("nodes")).get(Integer.parseInt(id.substring(1)))),"outputType");if(Arrays.asList("DOUBLE","INTEGER","BOOLEAN","PROBABILITY").contains(type)&&features.size()<32)features.add(id);}
  if(features.isEmpty()&&retained==null)for(Object raw:list(root.get("nodes"))){Map<String,Object> n=map(raw);if(str(n,"operator").equals("ATOM")&&Arrays.asList("DOUBLE","INTEGER","BOOLEAN","PROBABILITY").contains(str(n,"outputType"))){features.add(str(n,"id"));break;}}
  if(features.isEmpty()&&retained==null)throw new DevelopmentProductionCP12.Wait("WAIT_NUMERIC_PARENT_FEATURE");
  b.nodes.addAll(list(root.get("nodes")));for(AtomSpecCP4 spec:specs)b.specs.put(spec.atomId,spec);List<Object> refs=list(map(root.get("featureManifest")).get("matrixSpecRefs"));b.matrixRefs.addAll(refs);
  b.reuseExistingNodes();
  // New children get explicit catalog-backed short/medium/long inputs. Existing
  // immutable candidates/checkpoints keep their original graph and dimensions.
  // A rare conditional feature can be absent without removing every model input;
  // masks remain truthful and the trainer's all-missing rejection stays intact.
  int digit=variant%10;
  LinkedHashSet<String> requiredBankR80=new LinkedHashSet<>();
  for(int window:new int[]{20,120,500})requiredBankR80.add(b.atom("FREQUENCY",fields("digit",digit,"window",window)));
  for(int window:new int[]{20,500})requiredBankR80.add(b.atom("ENTROPY",fields("window",window)));
  String gap=b.atom("OMISSION",fields("digit",digit));
  String scaledGap=b.node("SAFE_DIVIDE","DOUBLE",Arrays.asList(b.node("MINIMUM","DOUBLE",Arrays.asList(gap,b.constant(500))),b.constant(500)));
  requiredBankR80.add(scaledGap);features.addAll(requiredBankR80);
  if(family==CandidateAstR952.Family.HYBRID){String feature=features.iterator().next(),atom=b.atom("FREQUENCY",fields("digit",digit,"window",50)),numericFeature=feature;if(!str(map(b.nodes.get(Integer.parseInt(feature.substring(1)))),"outputType").equals("DOUBLE"))numericFeature=b.node("NUMERIC_TO_DOUBLE","DOUBLE",Arrays.asList(feature));String interaction=b.node("MULTIPLY","DOUBLE",Arrays.asList(numericFeature,atom));requiredBankR80.add(interaction);features.add(interaction);}
  // The feature cap includes the R78 bank. Only previously invalid overflowing
  // children change: retain that bank and the largest stable prefix of parent
  // outputs that fits. The parent AST, masks and qualifications are never edited.
  List<String> orderedR80=new ArrayList<>(features);
  for(int i=orderedR80.size()-1;features.size()>32&&i>=0;i--){String id=orderedR80.get(i);if(!requiredBankR80.contains(id))features.remove(id);}
  need(features.size()<=32,"R80_FEATURE_BANK_CAPACITY");
  if(b.nodes.size()>128)throw new IllegalArgumentException("R78_FEATURE_BANK_NODE_CAP");
  root.put("nodes",b.nodes);root.put("featureOutputIds",new ArrayList<>(features));root.put("inputDimension",2*features.size());specs=new ArrayList<>(b.specs.values());
  finishManifest(root,specs,b.matrixRefs);CandidateAstR952 ast=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,specs);return ast;
 }
 /** A preregistered typed input graph, not an untrained frozen prediction model. */
 public static CandidateAstR952 boundFeatureGraphR80(List<AtomSpecCP4> specs,List<String> bindings,int position){
  need(specs!=null&&!specs.isEmpty()&&bindings!=null&&!bindings.isEmpty(),"BOUND_FACTORY_INPUT");
  List<Object> nodes=new ArrayList<>();List<String> features=new ArrayList<>();
  for(AtomSpecCP4 spec:specs){need(spec.requiresBinding&&spec.position==position,"BOUND_FACTORY_SPEC");String type=spec.outputType.equals("PROBABILITY")?"PROBABILITY":"DOUBLE";String id="n"+nodes.size();nodes.add(nodeMap(nodes.size(),"ATOM",type,Collections.emptyList(),Collections.emptyList(),spec.atomId));features.add(id);}
  Map<String,Object> root=base(CandidateAstR952.Family.LINEAR,position,"SOFTMAX_LINEAR",nodes,features,"@MODEL",2*features.size(),Collections.emptyList());
  root.put("featureManifest",fields("bindingHashes",new ArrayList<>(new TreeSet<>(bindings))));finishManifest(root,specs);
  CandidateAstR952 ast=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,specs);return ast;
 }
 /** New graph identity for data-only parents. Existing bound cohorts cannot
  * authorize a modified AST; their original capture lifecycle stays separate.
  * Three explicit long scales add six real features to the original scalar
  * prefix, respecting the original32-feature bound and immutable parent. */
 public static Proposal longSummaryFromDataParentR81(CandidateAstR952 parent,List<AtomSpecCP4> registry,String epoch,int digit){
  need(parent!=null&&parent.formatVersion==3&&registry!=null&&epoch!=null&&epoch.matches("[0-9a-f]{64}")&&digit>=0&&digit<10,"LONG_FACTORY_INPUT");
  need(parent.graphSpec.experts.isEmpty()&&parent.graphSpec.manifest.bindingHashes.isEmpty()&&parent.graphSpec.manifest.summarySpecRefs.isEmpty(),"LONG_FACTORY_FRESH_DATA_PARENT_REQUIRED");
  need(Arrays.asList(CandidateAstR952.Family.LINEAR,CandidateAstR952.Family.LOGISTIC,CandidateAstR952.Family.TREE,CandidateAstR952.Family.NEURAL,CandidateAstR952.Family.HYBRID).contains(parent.family),"LONG_FACTORY_LEARNED_PARENT");
  Map<String,Object> root=new TreeMap<>(parent.graphSpec.canonicalObject());List<Object> nodes=new ArrayList<>(list(root.get("nodes")));
  List<AtomSpecCP4> specs=new ArrayList<>(registeredCP12(parent,registry));List<String> added=new ArrayList<>();
  for(int window:LongScaleCountsR80.windows())for(String metric:new String[]{"FREQUENCY","ENTROPY"}){
   Map<String,Object> params=metric.equals("FREQUENCY")?fields("epochHash",epoch,"window",window,"digit",digit):fields("epochHash",epoch,"window",window);
   Map<String,Object> definition=fields("context","LONG_SUMMARY_R80","family",metric.equals("FREQUENCY")?"LONG_FREQUENCY_R80":"LONG_ENTROPY_R80","params",params,"position",parent.position,"regime","ALL","transform","IDENTITY","version","LONG_R80");
   String canonical=StrictCanonicalJson.writeAtom(definition),hash=V51ChecksumEngine.sha256(canonical),id="ATOM2-"+hash.substring(0,24);
   AtomSpecCP4 spec=AtomSpecCP4.fromRegistry(id,hash,canonical,"SCALAR",1,"REJECT_UNAVAILABLE_OR_STALE_LONG_SOURCE","OBSERVING");
   need(nodes.size()<128,"LONG_FACTORY_NODE_CAP");String node="n"+nodes.size();nodes.add(nodeMap(nodes.size(),"ATOM","DOUBLE",Collections.emptyList(),Collections.emptyList(),id));added.add(node);specs.add(spec);
  }
  List<String> features=new ArrayList<>();for(Object raw:list(root.get("featureOutputIds"))){String id=(String)raw;String type=str(map(nodes.get(Integer.parseInt(id.substring(1)))),"outputType");if(Arrays.asList("DOUBLE","INTEGER","BOOLEAN","PROBABILITY").contains(type)&&features.size()<32-added.size())features.add(id);}
  features.addAll(added);root.put("nodes",nodes);root.put("featureOutputIds",features);root.put("inputDimension",2*features.size());
  List<Object> matrices=new ArrayList<>(list(map(root.get("featureManifest")).get("matrixSpecRefs")));finishManifest(root,specs,matrices);LongSummaryBindingsR80.decorateManifest(root,specs);
  CandidateAstR952 ast=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,specs);
  return new Proposal(ast,V51ChecksumEngine.sha256("R81_LONG_CHILD|"+parent.structuralHash+'|'+epoch+'|'+digit),specs);
 }
 private static Proposal fixedGraphCP12(Builder b,CandidateAstR952.Family family,int position,int w){String output;
  switch(family){
   case FREQUENCY:output=b.vector("FREQUENCY",w,0);break;
   case OMISSION:output=b.omission(w);break;
   case MARKOV:output=b.vector("MARKOV_SCORE",w,0);break;
   case TRANSITION:output=b.cross(w);break;
   case STATISTICAL:{String f=b.vector("FREQUENCY",w,0),o=b.omission(w),entropy=b.atom("ENTROPY",fields("window",w));String scale=b.node("SAFE_DIVIDE","DOUBLE",Arrays.asList(entropy,b.constant(Math.log(10))));String weighted=b.node("SCALAR_MULTIPLY_VECTOR10","SCORE_VECTOR10",Arrays.asList(scale,o));output=b.weighted(Arrays.asList(f,weighted),1,.25);break;}
   case RULE:{List<String> scores=new ArrayList<>();String threshold=b.constant(.1);for(int d=0;d<10;d++){String f=b.atom("FREQUENCY",fields("digit",d,"window",w)),o=b.atom("OMISSION",fields("digit",d));o=b.node("LOG1P_ABS","DOUBLE",Arrays.asList(b.node("MINIMUM","DOUBLE",Arrays.asList(o,b.constant(w)))));String test=b.node("THRESHOLD_GE","BOOLEAN",Arrays.asList(f,threshold));scores.add(b.node("CONDITIONAL","DOUBLE",Arrays.asList(test,f,o)));}output=b.node("PACK10","SCORE_VECTOR10",scores);break;}
   case REGIME:{String[] vectors={b.vector("FREQUENCY",20,0),b.vector("FREQUENCY",w,0),b.vector("MARKOV_SCORE",w,0),b.omission(w),b.cross(w)};List<String> mixture=new ArrayList<>();for(int i=0;i<5;i++){String score=b.atom("REGIME_SCORE",fields("regime",AtomRegimePolicyCP12.LABELS[i],"window",w));String probability=b.node("NORMALIZE_POSITIVE","SCORE_VECTOR10",Arrays.asList(vectors[i]));mixture.add(b.node("SCALAR_MULTIPLY_VECTOR10","SCORE_VECTOR10",Arrays.asList(score,probability)));}output=b.weighted(mixture,1,1,1,1,1);break;}
   default:throw new IllegalArgumentException("FAMILY_NOT_EXECUTABLE:"+family);
  }
  output=b.node("NORMALIZE_POSITIVE","SCORE_VECTOR10",Arrays.asList(output));Map<String,Object> root=base(family,position,"FIXED_GRAPH",b.nodes,Collections.emptyList(),output,10,Collections.emptyList());List<AtomSpecCP4> specs=new ArrayList<>(b.specs.values());finishManifest(root,specs,b.matrixRefs);CandidateAstR952 ast=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,specs);return new Proposal(ast,V51ChecksumEngine.sha256("CP12_FAMILY_FOUNDATION|"+family+'|'+position+'|'+w),specs);
 }
 private static Proposal teachers(SQLiteDatabase db,CandidateAstR952.Family family,int position){
  need(position>=1&&position<=10,"TEACHER_POSITION");
  // Canonical AST3 root ends with position/version. This suffix cannot match an expert nested position.
  // Filter before LIMIT: unrelated positions must never hide eligible older teachers.
  String positionTail="%,\"position\":"+position+",\"version\":3}";
  List<Object> refs=new ArrayList<>();StringBuilder source=new StringBuilder("CP12_FAMILY_TEACHER|").append(family).append('|').append(position);int wanted=family==CandidateAstR952.Family.ENSEMBLE?3:2;
  try(Cursor c=db.rawQuery("SELECT f.candidate_id FROM "+ResearchEvaluationRunR967.EVAL+" f JOIN r952_candidate p ON p.candidate_id=f.learner_id WHERE f.ast LIKE 'AST3:%' AND f.ast LIKE ? AND length(CAST(f.ast AS BLOB)) BETWEEN 1 AND 32768 AND length(CAST(f.model_payload AS BLOB)) BETWEEN 1 AND 262144 AND p.state NOT IN('QUARANTINED','QUALITY_PRUNED','RESOURCE_RETIRED') ORDER BY f.created_at DESC,f.candidate_id LIMIT 128",new String[]{positionTail})){
   while(c.moveToNext()&&refs.size()<wanted){ResearchWorkBudgetR927.checkpoint();try(MemoryBudgetCP4.Lease modelMemory=RuntimeMemoryGuardR811.algorithmModelBudget().acquire(4L<<20)){FrozenCandidateAdapterR958.Frozen f=FrozenCandidateAdapterR958.load(db,c.getString(0));if(f==null||f.ast.position!=position||f.ast.formatVersion!=3)continue;boolean duplicate=false;for(Object r:refs)if(str(map(r),"freezeHash").equals(f.structure))duplicate=true;if(duplicate)continue;
    refs.add(fields("freezeHash",f.structure,"modelHash",f.model.hash(),"featureManifestHash",f.ast.graphSpec.manifest.hash,"position",position));source.append('|').append(f.structure).append('|').append(f.model.hash());
   }}
  }
  if(refs.size()<wanted)throw new DevelopmentProductionCP12.Wait("WAIT_FROZEN_TEACHER");
  return new Proposal(teacherGraphCP12(family,position,refs),V51ChecksumEngine.sha256(source.toString()),Collections.emptyList());
 }
 private static CandidateAstR952 teacherGraphCP12(CandidateAstR952.Family family,int position,List<Object> refs){List<Object> nodes=new ArrayList<>();nodes.add(nodeMap(0,"CONSTANT","DOUBLE",Collections.emptyList(),Collections.singletonList(Double.toHexString(0)),""));Map<String,Object> root=base(family,position,"EXPERT_HEDGE",nodes,Collections.emptyList(),"@MODEL",10*refs.size(),refs);finishManifest(root,Collections.emptyList());CandidateAstR952 ast=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,Collections.emptyList());return ast;}

 public static String maintain(DbStore store,long deadlineNs){
  need(store!=null&&!store.readable().inTransaction(),"FAMILY_MAINTAIN_INPUT");if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return "YIELD_P0_PRESSURE";
  if(!ModernSeedFactoryCP12.completeCP12(store.readable()))return "WAIT_MOLECULE_CATALOG_VERIFICATION";
  // Full training queues must drain before rereading/hashing the same large input.
  // The commit repeats this check; no capacity or acceptance gate is relaxed.
  if(!ResearchDirectorR952.trainingQueueHasCapacityR65(store.readable()))return "WAIT_TRAIN_INPUT_CAPACITY";
  Integer turn=store.coordinatorForResearchR952().runAtomicWrite("CP12_FAMILY_WHEEL",db->{int next;long generation;try(Cursor c=db.rawQuery("SELECT next_family,generation FROM "+WHEEL+" WHERE singleton=1",null)){need(c.moveToFirst(),"FAMILY_WHEEL_MISSING");next=c.getInt(0);generation=c.getLong(1);}db.execSQL("UPDATE "+WHEEL+" SET next_family=?,generation=generation+1 WHERE singleton=1 AND generation=?",new Object[]{(next+1)%15,generation});need(changes(db)==1,"FAMILY_WHEEL_CAS");return next;},null);if(turn==null)throw new IllegalStateException("FAMILY_WHEEL_COMMIT_FAILED");
  CandidateAstR952.Family family=CandidateAstR952.Family.values()[turn];SQLiteDatabase db=store.readable();int variant;try(Cursor c=db.rawQuery("SELECT next_variant FROM "+CURSOR+" WHERE family=?",new String[]{family.name()})){need(c.moveToFirst(),"FAMILY_CURSOR_MISSING");variant=c.getInt(0);}if(variant>=LIMIT)return "FAMILY_FOUNDATION_COMPLETE:"+family;
  try(MemoryBudgetCP4.Lease memory=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(1L<<20)){
   String latest="";try(Cursor c=db.rawQuery("SELECT d.issue FROM draws d JOIN "+DbHelper.TABLE_R811_DRAW_CANONICAL+" c ON c.target_issue=d.issue AND c.status='CONFIRMED_CANONICAL_DRAW' AND c.canonical_draw_hash=d.draw_hash_v2 WHERE d.dirty=0 ORDER BY d.issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}if(!TargetIssueResolver.isValid(latest))return holdCursor(store,family,variant,"WAIT_AUTHORITATIVE_DRAW");
   Proposal proposal;String existingSource=null,existingAst=null,sourceIssue=latest;
   try(Cursor c=db.rawQuery("SELECT source_hash,ast,source_issue FROM "+CREATION+" WHERE family=? AND variant=? AND state='PROPOSED'",new String[]{family.name(),Integer.toString(variant)})){if(c.moveToFirst()){existingSource=c.getString(0);existingAst=c.getString(1);sourceIssue=c.getString(2);}}
   try{proposal=existingAst==null?prepare(db,family,variant):new Proposal(CandidateAstR952.parse(existingAst),existingSource,registered(db,CandidateAstR952.parse(existingAst)));}catch(DevelopmentProductionCP12.Wait wait){return holdCursor(store,family,variant,wait.reason);}
   try(Cursor c=db.rawQuery("SELECT candidate_id FROM "+CREATION+" WHERE family=? AND source_hash=? AND state IN('REGISTERED','NO_OP','HELD')",new String[]{family.name(),proposal.sourceHash})){
    if(c.moveToFirst()){final String existing=c.getString(0);Boolean done=store.coordinatorForResearchR952().runAtomicWrite("CP12_FAMILY_EXISTING_SOURCE",tx->{tx.execSQL("UPDATE "+CURSOR+" SET next_variant=next_variant+1,last_reason='NO_OP_EXISTING_SOURCE',generation=generation+1,updated_at=? WHERE family=? AND next_variant=?",new Object[]{System.currentTimeMillis(),family.name(),variant});need(changes(tx)==1,"FAMILY_EXISTING_CAS");return true;},false);if(!Boolean.TRUE.equals(done))throw new IllegalStateException("FAMILY_EXISTING_COMMIT_FAILED");return "NO_OP:"+existing;}
   }
   return register(store,proposal,family,variant,sourceIssue,TargetIssueResolver.next(latest).targetIssue,deadlineNs);
  }catch(IllegalStateException resource){if(!"ALGORITHM_MEMORY_BUDGET".equals(resource.getMessage()))throw resource;return holdCursor(store,family,variant,"MEMORY_PRESSURE");}
 }
 private static void guardComplete(SQLiteDatabase db){if(!ModernSeedFactoryCP12.completeCP12(db))throw new DevelopmentProductionCP12.Wait("WAIT_MOLECULE_CATALOG_VERIFICATION");}
 private static String holdCursor(DbStore store,CandidateAstR952.Family f,int variant,String reason){Boolean ok=store.coordinatorForResearchR952().runAtomicWrite("CP12_FAMILY_WAIT",db->{db.execSQL("UPDATE "+CURSOR+" SET last_reason=?,generation=generation+1,updated_at=? WHERE family=? AND next_variant=?",new Object[]{reason,System.currentTimeMillis(),f.name(),variant});need(changes(db)==1,"FAMILY_WAIT_CAS");return true;},false);if(!Boolean.TRUE.equals(ok))throw new IllegalStateException("FAMILY_WAIT_COMMIT_FAILED");return "HELD:"+f+':'+reason;}
 private static String register(DbStore store,Proposal p,CandidateAstR952.Family family,int variant,String sourceIssue,String target,long deadlineNs){
  final String evidence=V51ChecksumEngine.sha256("CP12_FAMILY_EVIDENCE|"+family+'|'+p.sourceHash+'|'+p.ast.structuralHash),action=V51ChecksumEngine.sha256("CP12_FAMILY_ACTION|"+evidence),finding=V51ChecksumEngine.sha256("CP12_FAMILY_FINDING|"+evidence),candidate="CP12-FAMILY-"+p.ast.structuralHash.substring(0,24);
  final boolean[] proposedNow={false};
  String intent=store.coordinatorForResearchR952().runAtomicWrite("CP12_FAMILY_PROPOSAL",db->{
   if(System.nanoTime()>=deadlineNs)throw new DevelopmentProductionCP12.Wait("TIME_BUDGET_EXCEEDED");String ii=ResearchIntentLedgerR952.put(db,"MODERN_FAMILY_FOUNDATION",p.ast.canonical,"UNTRAINED_NO_FUTURE","INDEPENDENT_DEVELOPMENT_AND_FUTURE",family.name(),1,sourceIssue);
   try(Cursor c=db.rawQuery("SELECT ast_hash,ast,action_id,state FROM "+CREATION+" WHERE family=? AND variant=?",new String[]{family.name(),Integer.toString(variant)})){if(c.moveToFirst()){need(p.ast.structuralHash.equals(c.getString(0))&&p.ast.canonical.equals(c.getString(1))&&action.equals(c.getString(2))&&c.getString(3).equals("PROPOSED"),"FAMILY_PROPOSAL_REPLAY");return ii;}}
   db.execSQL("INSERT INTO research_finding_v2(finding_id,game_code,candidate_version,source_issue,episode_start_issue,problem_type,severity,evidence_hash,evidence_json,hypothesis,falsification_test,state,created_at,updated_at)VALUES(?,'PK10',?,?,?,'NEW_FAMILY_FOUNDATION','P2',?,?,?,'DEVELOPMENT_AND_PROSPECTIVE_FUTURE','PLANNED',?,?)",new Object[]{finding,candidate,sourceIssue,sourceIssue,evidence,p.ast.canonical,"Pre-registered family feature structure may improve independent performance",System.currentTimeMillis(),System.currentTimeMillis()});
   db.execSQL("INSERT INTO research_action_v2(action_id,finding_id,action_type,operator,proposal_json,evidence_hash,seed,parent_hashes_json,protocol_hash,planned_endpoint,alpha_identity,state)VALUES(?,?,'NEW_FAMILY','CREATE',?,?,0,'[]',?,360,'','PROPOSED')",new Object[]{action,finding,StrictCanonicalJson.write(fields("family",family.name())),evidence,V51ChecksumEngine.sha256("CP12_FAMILY_EXPERIMENT|"+evidence)});
   String boundary=TargetIssueResolver.next(sourceIssue).targetIssue;db.execSQL("INSERT INTO research_action_input_boundary_cp12 VALUES(?,?,?,?,?,?)",new Object[]{action,sourceIssue,boundary,evidence,V51ChecksumEngine.sha256("CP12_ACTION_BOUNDARY|"+action+'|'+sourceIssue+'|'+boundary+'|'+evidence),System.currentTimeMillis()});
   db.execSQL("INSERT INTO "+CREATION+" VALUES(?,?,?,?,?,?,?,?, 'PROPOSED','',?,?)",new Object[]{family.name(),p.sourceHash,variant,p.ast.structuralHash,p.ast.canonical,action,candidate,sourceIssue,System.currentTimeMillis(),System.currentTimeMillis()});proposedNow[0]=true;return ii;
  },null);if(intent==null)throw new IllegalStateException("FAMILY_PROPOSAL_COMMIT_FAILED");
  // The immutable proposal is a real checkpoint. Do not begin expensive input IO
  // when proposal construction already consumed this callback; resume next turn.
  if(proposedNow[0]&&System.nanoTime()+250_000_000L>=deadlineNs)return "FAMILY_PROPOSAL_READY";
  try(DevelopmentProductionCP12.PreparedProtocol protocol=ResearchDirectorR952.prepareTrainingProtocolCP5(store,p.ast,candidate,action,target)){
   ResearchDirectorR952.PreparedEvolution child=new ResearchDirectorR952.PreparedEvolution(candidate,action,"",intent,"",evidence);
   String result=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("CP12_FAMILY_REGISTER",db->{String task=ResearchDirectorR952.commitPreparedCandidateCP5(db,protocol,child);if(task.startsWith("NO_OP:"))db.execSQL("UPDATE research_finding_v2 SET state='HOLD',updated_at=? WHERE finding_id=? AND state='PLANNED'",new Object[]{System.currentTimeMillis(),finding});finish(db,family,variant,action,task.startsWith("NO_OP:")?"NO_OP":"REGISTERED","");return task;});return result;
  }catch(RuntimeException failure){for(Throwable cause=failure;cause!=null;cause=cause.getCause())if(cause instanceof DevelopmentProductionCP12.Wait&&"TRAIN_INPUT_CAPACITY".equals(((DevelopmentProductionCP12.Wait)cause).reason))return "WAIT_TRAIN_INPUT_CAPACITY";final String reason=terminalPreparationReasonCP12(failure);if(reason==null)throw failure;
   Boolean held=store.coordinatorForResearchR952().runAtomicWrite("CP12_FAMILY_FIXED_INPUT_HOLD",db->{db.execSQL("UPDATE research_action_v2 SET state='REJECTED',last_error=? WHERE action_id=? AND state='PROPOSED' AND evidence_hash=?",new Object[]{reason,action,evidence});need(changes(db)==1,"FAMILY_ACTION_HOLD_CAS");db.execSQL("UPDATE research_finding_v2 SET state='HOLD',updated_at=? WHERE finding_id=? AND state='PLANNED' AND evidence_hash=?",new Object[]{System.currentTimeMillis(),finding,evidence});need(changes(db)==1,"FAMILY_FINDING_HOLD_CAS");finish(db,family,variant,action,"HELD",reason);return true;},false);if(!Boolean.TRUE.equals(held))throw new IllegalStateException("FAMILY_HOLD_COMMIT_FAILED");return "HELD:"+reason;
  }catch(IOException e){throw new DevelopmentProductionCP12.Wait("WAIT_INPUT_IO",e);}
 }
 /** Deterministic integrity/size failures never enter the temporary capacity retry lane. */
 static String terminalPreparationReasonCP12(Throwable failure){for(Throwable c=failure;c!=null;c=c.getCause()){String code=c instanceof DevelopmentProductionCP12.Wait?((DevelopmentProductionCP12.Wait)c).reason:c.getMessage();if("WAIT_HISTORY".equals(code)||"WAIT_BOUND_FEATURE_HISTORY".equals(code)||"MODEL_STORAGE_HASH_SIZE_CONFLICT".equals(code)||"MODEL_STORAGE_SINGLE_PAYLOAD_BOUND".equals(code)||"TRAINING_WORKING_SINGLE_PAYLOAD_BOUND".equals(code))return code;}return null;}
 private static void finish(SQLiteDatabase db,CandidateAstR952.Family family,int variant,String action,String state,String reason){db.execSQL("UPDATE "+CREATION+" SET state=?,last_reason=?,updated_at=? WHERE family=? AND variant=? AND action_id=? AND state='PROPOSED'",new Object[]{state,reason,System.currentTimeMillis(),family.name(),variant,action});need(changes(db)==1,"FAMILY_FINISH_CAS");db.execSQL("UPDATE "+CURSOR+" SET next_variant=next_variant+1,last_reason=?,generation=generation+1,updated_at=? WHERE family=? AND next_variant=?",new Object[]{reason,System.currentTimeMillis(),family.name(),variant});need(changes(db)==1,"FAMILY_CURSOR_CAS");}
 private static List<AtomSpecCP4> registered(SQLiteDatabase db,CandidateAstR952 ast){TreeMap<String,AtomSpecCP4>s=new TreeMap<>();for(com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeSpec n:ast.graphSpec.nodes)if(n.kind==com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeKind.ATOM)s.put(n.source,spec(db,n.source));return new ArrayList<>(s.values());}
 private static Map<String,Object> base(CandidateAstR952.Family family,int position,String kind,List<Object> nodes,List<String> features,String output,int d,List<?> experts){return fields("version",3,"compilerRevision","AST3_CP4_1","game",ReleaseManifestR926.GAME_CODE,"position",position,"family",family.name(),"modelKind",kind,"normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","nodes",nodes,"featureOutputIds",features,"outputId",output,"inputDimension",d,"experts",experts);}
 private static void finishManifest(Map<String,Object> root,List<AtomSpecCP4>specs){finishManifest(root,specs,Collections.emptyList());}
 private static void finishManifest(Map<String,Object> root,List<AtomSpecCP4>specs,List<Object> matrixRefs){List<String> ids=new ArrayList<>();for(Object f:list(root.get("featureOutputIds")))ids.add((String)f);List<String> types=new ArrayList<>(),missing=new ArrayList<>();for(String id:ids){types.add(str(map(list(root.get("nodes")).get(Integer.parseInt(id.substring(1)))),"outputType"));missing.add("MASK");}TreeSet<String> hashes=new TreeSet<>();int minimum=0;for(AtomSpecCP4 s:specs){hashes.add(s.specHash);minimum=Math.max(minimum,s.minimumHistory);}for(Object raw:matrixRefs)minimum=Math.max(minimum,MatrixHistoricalDrawCP12.minimumHistoryCP12(new MatrixBindingsCP12.Ref(new FeatureManifestR952.MatrixRef(map(raw)))));root.put("featureManifest",fields("revision","CP4_FEATURE_1","graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(root),"orderedFeatureIds",ids,"featureTypes",types,"requiredHistory",minimum,"atomSpecHashes",new ArrayList<>(hashes),"bindingHashes",root.containsKey("featureManifest")?map(root.get("featureManifest")).get("bindingHashes"):Collections.emptyList(),"matrixSpecRefs",matrixRefs,"normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","missingPolicyPerFeature",missing,"inputDimension",root.get("inputDimension"),"estimatedStateBytes",8*1024*1024));LongSummaryBindingsR80.decorateManifest(root,specs);}
 private static Map<String,Object> nodeMap(int n,String op,String type,List<String> parents,List<String>parameters,String atom){return fields("id","n"+n,"operator",op,"outputType",type,"atomId",atom,"stage","FROZEN_PREDRAW","unit","dimensionless","parents",parents,"parameters",parameters);}
 private static final class Builder {
  final SQLiteDatabase db;final Map<String,AtomSpecCP4> registeredById;final int position;final List<Object> nodes=new ArrayList<>();final TreeMap<String,AtomSpecCP4>specs=new TreeMap<>();final Map<String,String> atomNodes=new HashMap<>(),constants=new HashMap<>();final List<Object> matrixRefs=new ArrayList<>();Builder(SQLiteDatabase d,int p){db=d;registeredById=null;position=p;}Builder(List<AtomSpecCP4> registered,int p){db=null;registeredById=new TreeMap<>();for(AtomSpecCP4 s:registered)registeredById.put(s.atomId,s);position=p;}
  boolean reuse;
  void reuseExistingNodes(){reuse=true;for(Object raw:nodes){Map<String,Object> n=map(raw);String op=str(n,"operator"),id=str(n,"id");if(op.equals("CONSTANT"))constants.put((String)list(n.get("parameters")).get(0),id);if(op.equals("ATOM")&&str(n,"outputType").equals("DOUBLE"))atomNodes.put(str(n,"atomId"),id);if(op.equals("NUMERIC_TO_DOUBLE")){List<?> parents=list(n.get("parents"));Map<String,Object> parent=map(nodes.get(Integer.parseInt(((String)parents.get(0)).substring(1))));if(str(parent,"operator").equals("ATOM"))atomNodes.put(str(parent,"atomId"),id);}}}
  String node(String op,String type,List<String> parents,double... parameters){List<String> ps=new ArrayList<>();for(double v:parameters)ps.add(Double.toHexString(v==0?0:v));if(reuse)for(Object raw:nodes){Map<String,Object> old=map(raw);if(op.equals(old.get("operator"))&&type.equals(old.get("outputType"))&&parents.equals(old.get("parents"))&&ps.equals(old.get("parameters")))return str(old,"id");}String id="n"+nodes.size();nodes.add(nodeMap(nodes.size(),op,type,parents,ps,""));return id;}
  String constant(double value){String key=Double.toHexString(value);String old=constants.get(key);if(old!=null)return old;String id=node("CONSTANT","DOUBLE",Collections.emptyList(),value);constants.put(key,id);return id;}
  String atom(String family,Map<String,Object>params){ResearchWorkBudgetR927.checkpoint();Map<String,Object>s=fields("context","PIT_DRAW","family",family,"params",params,"position",position,"regime","ALL","transform","IDENTITY","version","CP2");String specJson=StrictCanonicalJson.write(s);if(params.containsKey("alpha"))specJson=specJson.replace("\"alpha\":\"0.5\"","\"alpha\":0.5");String hash=V51ChecksumEngine.sha256(specJson),id="ATOM2-"+hash.substring(0,24);AtomSpecCP4 spec=db!=null?spec(db,id):registeredById.get(id);if(spec==null)throw new DevelopmentProductionCP12.Wait("WAIT_ATOM_REGISTRATION");need(spec.canonicalSpec.equals(specJson),"FAMILY_ATOM_SPEC_IDENTITY");specs.put(id,spec);String old=atomNodes.get(id);if(old!=null)return old;String type=spec.outputType.equals("PROBABILITY")?"PROBABILITY":spec.outputType.equals("BOOLEAN")?"BOOLEAN":"DOUBLE",node="n"+nodes.size();nodes.add(nodeMap(nodes.size(),"ATOM",type,Collections.emptyList(),Collections.emptyList(),id));if(!type.equals("DOUBLE"))node=node("NUMERIC_TO_DOUBLE","DOUBLE",Arrays.asList(node));atomNodes.put(id,node);return node;}
  String vector(String family,int w,int from){List<String>x=new ArrayList<>();for(int d=0;d<10;d++)x.add(atom(family,family.equals("TRANSITION_P1")?fields("from",from,"to",d,"window",w):family.equals("MARKOV_SCORE")?fields("alpha","0.5","digit",d,"order",1,"window",w):fields("digit",d,"window",w)));return node("PACK10","SCORE_VECTOR10",x);}
  String cross(int w){List<String>x=new ArrayList<>();for(int d=0;d<10;d++){
   String ordinary=atom("FREQUENCY",fields("digit",d,"window",w));Map<String,Object> existing=map(nodes.get(Integer.parseInt(ordinary.substring(1))));
   String node="n"+nodes.size();nodes.add(nodeMap(nodes.size(),"ATOM","DOUBLE",Collections.emptyList(),Collections.emptyList(),str(existing,"atomId")));
   String metric="latestSmoothedRow~"+d;matrixRefs.add(fields("type","TransitionMatrix","partitionRule","CP12/"+w+"/CROSSNEXT/"+metric+'/'+node,"manifest",MatrixBindingsCP12.specManifest(MatrixProjectionCP12.Type.TransitionMatrix,w,"CROSSNEXT",metric)));x.add(node);
  }matrixRefs.sort((a,b)->{Map<String,Object> aa=map(a),bb=map(b);int c=str(aa,"type").compareTo(str(bb,"type"));return c!=0?c:str(aa,"partitionRule").compareTo(str(bb,"partitionRule"));});return node("PACK10","SCORE_VECTOR10",x);}
  String omission(int w){List<String>x=new ArrayList<>();String cap=constant(w);for(int d=0;d<10;d++){String a=atom("OMISSION",fields("digit",d));x.add(node("LOG1P_ABS","DOUBLE",Arrays.asList(node("MINIMUM","DOUBLE",Arrays.asList(a,cap)))));}return node("PACK10","SCORE_VECTOR10",x);}
  String weighted(List<String> parents,double...w){return node("WEIGHTED_SUM","SCORE_VECTOR10",parents,w);}
 }
}
