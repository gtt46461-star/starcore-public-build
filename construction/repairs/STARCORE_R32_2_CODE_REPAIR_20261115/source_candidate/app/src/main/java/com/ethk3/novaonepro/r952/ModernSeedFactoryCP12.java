package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** IMPORT substage adapter. Director owns dispatch, registration and TRAIN publication.
 * Catalog feature seeds acquire no Future count, certificate or real authority. */
public final class ModernSeedFactoryCP12 {
 private ModernSeedFactoryCP12(){}
 public static final String ASSET="research/cp4_molecule_seeds.jsonl";
 public static final String ASSET_SHA="b4ab9a61e2a36a4a426d0588ffb52350a25c09165273f802b9aa4c63e3c726ad";
 public static final int TOTAL=4200,BATCH=25;
 public static final String REVISION="CP12_SEED_AST3_1",IMPORT="research_molecule_import_cp12",USE="research_molecule_use_cp12";
 public static final class SeedRow {
  public final String hash,sourceId,sourceHash,graphJson,outputType;public final int position,row;
  public final CandidateAstR952 ast;
  SeedRow(String line,int row,SQLiteDatabase db){
   Map<String,Object> r=StrictCanonicalJson.readObject(line);need(r.keySet().equals(new TreeSet<>(Arrays.asList("graph","graphHash","seedId","sourceSha256"))),"SEED_ROW_KEYS");
   Map<String,Object> g=map(r.get("graph"));graphJson=StrictCanonicalJson.write(g);hash=str(r,"graphHash");sourceId=str(r,"seedId");sourceHash=str(r,"sourceSha256");
   need(hash.matches("[0-9a-f]{64}")&&hash.equals(V51ChecksumEngine.sha256(graphJson))&&sourceHash.matches("[0-9a-f]{64}")&&sourceId.matches("MOL-[0-9a-f]{20}"),"SEED_SOURCE_HASH");
   position=((Number)g.get("position")).intValue();this.row=row;need(position>=1&&position<=10&&g.get("pit").equals("STRICT_PAST")&&g.get("missing").equals("PROPAGATE")&&g.get("role").equals("FEATURE_ONLY")&&g.get("revision").equals("CP4_SEED_BINDING_1"),"SEED_FEATURE_CONTRACT");
   Map<String,Object> root=fields("version",3,"compilerRevision","AST3_CP4_1","game",ReleaseManifestR926.GAME_CODE,"position",position,"family","LINEAR","modelKind","SOFTMAX_LINEAR","normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","experts",new ArrayList<>(),"inputDimension",2,"outputId","@MODEL");
   Map<String,String> renamed=new HashMap<>();int oldOrdinal=0;List<Object> nodes=new ArrayList<>();List<AtomSpecCP4> specs=new ArrayList<>();TreeSet<String> specHashes=new TreeSet<>();int minimum=0;
   for(Object raw:list(g.get("nodes"))){Map<String,Object> n=map(raw);String op=str(n,"op"),type=str(n,"type"),id=str(n,"id");need(id.equals("n"+oldOrdinal++),"SEED_TOPOLOGY");String newId="n"+nodes.size();List<String> parents=new ArrayList<>();for(Object parent:list(n.get("parents"))){need(renamed.containsKey(parent),"SEED_PARENT_ORDER");parents.add(renamed.get(parent));}
    String atom="";String modernType=type.equals("BOOL")?"BOOLEAN":type.equals("SCALAR")?"DOUBLE":"INVALID";need(!modernType.equals("INVALID"),"SEED_OUTPUT_TYPE");
    if(op.equals("ATOM")){atom=str(n,"atomId");AtomSpecCP4 s=spec(db,atom);need(s.position==position&&!s.state.equals("RETIRED"),"SEED_ATOM_STATE_POSITION");specs.add(s);specHashes.add(s.specHash);minimum=Math.max(minimum,s.minimumHistory);
     // Catalog scalar math primitives preserve their exact declared output type.
     modernType=s.outputType.equals("BOOLEAN")?"BOOLEAN":s.outputType.equals("PROBABILITY")?"PROBABILITY":"DOUBLE";
    }
    List<String> params=new ArrayList<>();for(Object p:list(n.get("parameters"))){double d=Double.parseDouble((String)p);need(Double.isFinite(d),"SEED_PARAMETER");params.add(Double.toHexString(d==0?0:d));}
    nodes.add(fields("id",newId,"operator",op,"outputType",modernType,"atomId",atom,"stage","FROZEN_PREDRAW","unit","dimensionless","parents",parents,"parameters",params));
    if(op.equals("ATOM")&&!modernType.equals("DOUBLE")){String castId="n"+nodes.size();nodes.add(fields("id",castId,"operator","NUMERIC_TO_DOUBLE","outputType","DOUBLE","atomId","","stage","FROZEN_PREDRAW","unit","dimensionless","parents",Collections.singletonList(newId),"parameters",new ArrayList<>()));newId=castId;}
    renamed.put(id,newId);
   }
   String output=renamed.get(str(g,"outputId"));need(output!=null,"SEED_ROOT");int at=Integer.parseInt(output.substring(1));need(at>=0&&at<nodes.size(),"SEED_ROOT");String type=str(map(nodes.get(at)),"outputType");outputType=type.equals("BOOLEAN")?"BOOL":"SCALAR";
   root.put("nodes",nodes);root.put("featureOutputIds",Collections.singletonList(output));
   root.put("featureManifest",fields("revision","CP4_FEATURE_1","graphHash",CandidateAstR952.hashGraphKernelWithoutManifest(root),"orderedFeatureIds",Collections.singletonList(output),"featureTypes",Collections.singletonList(type),"requiredHistory",minimum,"atomSpecHashes",new ArrayList<>(specHashes),"bindingHashes",new ArrayList<>(),"matrixSpecRefs",new ArrayList<>(),"normalizerPolicy","TRAIN_ONLY_ZSCORE_V1","missingPolicyPerFeature",Collections.singletonList("MASK"),"inputDimension",2,"estimatedStateBytes",8*1024*1024));
   FeatureMetadataR82.decorate(root,specs);
   ast=CandidateAstR952.parse("AST3:"+StrictCanonicalJson.write(root));CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,specs);
  }
 }
 public static void schema(SQLiteDatabase db){
  need(db!=null&&db.inTransaction(),"SEED_SCHEMA_TX");
  db.execSQL("CREATE TABLE IF NOT EXISTS "+IMPORT+"(source_hash TEXT PRIMARY KEY CHECK(length(source_hash)=64),asset_name TEXT NOT NULL,expected_rows INTEGER NOT NULL CHECK(expected_rows=4200),next_row INTEGER NOT NULL CHECK(next_row BETWEEN 0 AND 4200),state TEXT NOT NULL CHECK(state IN('IMPORTING','COMPLETE')),generation INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL)");
  db.execSQL("CREATE TABLE IF NOT EXISTS "+USE+"(seed_hash TEXT PRIMARY KEY REFERENCES research_molecule_seed_v2(seed_hash),ast_hash TEXT NOT NULL,action_id TEXT NOT NULL UNIQUE REFERENCES research_action_v2(action_id),candidate_id TEXT NOT NULL UNIQUE,state TEXT NOT NULL CHECK(state IN('PROPOSED','REGISTERED','NO_OP','HELD')),created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
  db.execSQL("CREATE INDEX IF NOT EXISTS idx_cp12_seed_use_state ON "+USE+"(state,seed_hash)");
  db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_seed_asset_identity BEFORE UPDATE ON "+IMPORT+" WHEN OLD.source_hash<>NEW.source_hash OR OLD.asset_name<>NEW.asset_name OR OLD.expected_rows<>NEW.expected_rows BEGIN SELECT RAISE(ABORT,'SEED_ASSET_IDENTITY_IMMUTABLE'); END");
  db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_seed_use_identity BEFORE UPDATE ON "+USE+" WHEN OLD.seed_hash<>NEW.seed_hash OR OLD.ast_hash<>NEW.ast_hash OR OLD.action_id<>NEW.action_id OR OLD.candidate_id<>NEW.candidate_id BEGIN SELECT RAISE(ABORT,'SEED_USE_IDENTITY_IMMUTABLE'); END");
  db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_seed_use_delete BEFORE DELETE ON "+USE+" BEGIN SELECT RAISE(ABORT,'SEED_USE_COMPACT_PERMANENT'); END");
 }
 public static long importPrepared(SQLiteDatabase db,List<SeedRow> rows,long cursor){
  need(db!=null&&db.inTransaction()&&rows!=null&&rows.size()<=BATCH&&cursor>=0&&cursor<=TOTAL&&cursor+rows.size()<=TOTAL,"SEED_IMPORT_TX");
  Checkpoint checkpoint=readCheckpoint(db);need(checkpoint!=null&&checkpoint.state.equals("MATERIALIZING")&&checkpoint.scan.fullyVerified(),"SEED_RAW_VERIFICATION_REQUIRED");
  long actual;try(Cursor c=db.rawQuery("SELECT next_row FROM "+IMPORT+" WHERE source_hash=?",new String[]{ASSET_SHA})){need(c.moveToFirst(),"SEED_IMPORT_MISSING");actual=c.getLong(0);}
  boolean replay=actual==cursor+rows.size();need(actual==cursor||replay,"SEED_CURSOR_STALE");
  for(int i=0;i<rows.size();i++){SeedRow r=rows.get(i);need(r.row==cursor+i,"SEED_PAGE_ORDER");
   // A prepared graph loses validity if its registry dependency changes before publication.
   for(com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeSpec node:r.ast.graphSpec.nodes)if(node.kind==com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeKind.ATOM){AtomSpecCP4 s=spec(db,node.source);need(s.position==r.position&&!s.state.equals("RETIRED")&&r.ast.graphSpec.manifest.atomSpecHashes.contains(s.specHash),"SEED_REGISTRY_CAS");}
   db.execSQL("INSERT OR IGNORE INTO research_molecule_seed_v2(seed_hash,source_seed_id,position,graph_json,output_type,state,compiler_revision,source_hash,alias_of,imported_at)VALUES(?,?,?,?,?,'VALIDATED',?,?,NULL,?)",new Object[]{r.hash,r.sourceId,r.position,r.graphJson,r.outputType,REVISION,r.sourceHash,System.currentTimeMillis()});
   try(Cursor c=db.rawQuery("SELECT source_seed_id,position,graph_json,output_type,compiler_revision,source_hash FROM research_molecule_seed_v2 WHERE seed_hash=?",new String[]{r.hash})){need(c.moveToFirst()&&r.sourceId.equals(c.getString(0))&&r.position==c.getInt(1)&&r.graphJson.equals(c.getString(2))&&r.outputType.equals(c.getString(3))&&REVISION.equals(c.getString(4))&&r.sourceHash.equals(c.getString(5)),"SEED_REPLAY_DRIFT");}
  }
  long next=cursor+rows.size();if(!replay){db.execSQL("UPDATE "+IMPORT+" SET next_row=?,state=?,generation=generation+1,updated_at=? WHERE source_hash=? AND next_row=?",new Object[]{next,next==TOTAL?"COMPLETE":"IMPORTING",System.currentTimeMillis(),ASSET_SHA,cursor});need(changes(db)==1,"SEED_CURSOR_CAS");}
  if(next==TOTAL)try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM research_molecule_seed_v2 WHERE compiler_revision=?",new String[]{REVISION})){need(c.moveToFirst()&&c.getInt(0)==TOTAL,"SEED_COMPLETION_COUNT");}
  return next;
 }
 public static final String CHECKPOINT="research_molecule_import_checkpoint_cp12";
 private static final long COMMIT_RESERVE_NS=10_000_000L;
 // Heuristic admission reserve for another typed row, not a progress guarantee:
 // cold rows or cost spikes can still exhaust the unchanged owner slice budget.
 private static final long NEXT_TYPED_ROW_RESERVE_NS=50_000_000L;
 /** Only ResearchMigrationRegistry owns this DDL. The hot importer never installs schema. */
 public static void checkpointSchemaCP12(SQLiteDatabase db){
  need(db!=null&&db.inTransaction(),"SEED_CHECKPOINT_SCHEMA_TX");
  db.execSQL("CREATE TABLE IF NOT EXISTS "+CHECKPOINT+"(source_hash TEXT PRIMARY KEY REFERENCES "+IMPORT+"(source_hash),asset_name TEXT NOT NULL,expected_rows INTEGER NOT NULL CHECK(expected_rows=4200),expected_bytes INTEGER NOT NULL CHECK(expected_bytes=3265020),legacy_target_row INTEGER NOT NULL CHECK(legacy_target_row BETWEEN 0 AND 4200),scan_row INTEGER NOT NULL CHECK(scan_row BETWEEN 0 AND 4200),scan_offset INTEGER NOT NULL CHECK(scan_offset BETWEEN 0 AND 3265020),scan_prefix TEXT NOT NULL CHECK(length(scan_prefix)=64),scan_sha_state BLOB NOT NULL CHECK(length(scan_sha_state)=111),material_row INTEGER NOT NULL CHECK(material_row BETWEEN 0 AND 4200),material_offset INTEGER NOT NULL CHECK(material_offset BETWEEN 0 AND 3265020),material_prefix TEXT NOT NULL CHECK(length(material_prefix)=64),material_sha_state BLOB NOT NULL CHECK(length(material_sha_state)=111),generation INTEGER NOT NULL DEFAULT 0 CHECK(generation>=0),state TEXT NOT NULL CHECK(state IN('SCANNING','MATERIALIZING','COMPLETE')),updated_at INTEGER NOT NULL,CHECK(state='SCANNING' OR(scan_row=4200 AND scan_offset=3265020 AND scan_prefix=source_hash)),CHECK(state<>'COMPLETE' OR(material_row=4200 AND material_offset=3265020 AND material_prefix=source_hash)))");
  db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_molecule_checkpoint_identity BEFORE UPDATE ON "+CHECKPOINT+" WHEN OLD.source_hash<>NEW.source_hash OR OLD.asset_name<>NEW.asset_name OR OLD.expected_rows<>NEW.expected_rows OR OLD.expected_bytes<>NEW.expected_bytes OR OLD.legacy_target_row<>NEW.legacy_target_row BEGIN SELECT RAISE(ABORT,'SEED_CHECKPOINT_IDENTITY_IMMUTABLE'); END");
  db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_molecule_checkpoint_no_delete BEFORE DELETE ON "+CHECKPOINT+" BEGIN SELECT RAISE(ABORT,'SEED_CHECKPOINT_COMPACT_PERMANENT'); END");
  db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_molecule_checkpoint_no_replace BEFORE INSERT ON "+CHECKPOINT+" WHEN EXISTS(SELECT 1 FROM "+CHECKPOINT+" WHERE source_hash=NEW.source_hash) BEGIN SELECT RAISE(ABORT,'SEED_CHECKPOINT_REPLACE_FORBIDDEN'); END");
 }
 private static final class Checkpoint {
  final String state;final int legacy;final long generation,baseRow,baseGeneration;
  final MoleculeSeedPageCP12.Cursor scan,material;
  Checkpoint(Cursor c){
   need(ASSET.equals(c.getString(0))&&c.getInt(1)==TOTAL&&c.getInt(2)==MoleculeSeedPageCP12.BYTES,"SEED_CHECKPOINT_IDENTITY");
   legacy=c.getInt(3);generation=c.getLong(12);state=c.getString(13);baseRow=c.getLong(14);baseGeneration=c.getLong(15);
   need(legacy>=0&&legacy<=TOTAL&&baseRow>=legacy&&baseRow<=TOTAL&&baseGeneration>=0,"SEED_LEGACY_CURSOR_RANGE");
   scan=new MoleculeSeedPageCP12.Cursor(c.getInt(4),c.getLong(5),generation,c.getString(6),c.getBlob(7));
   material=new MoleculeSeedPageCP12.Cursor(c.getInt(8),c.getLong(9),generation,c.getString(10),c.getBlob(11));
   need(state.equals("SCANNING")||state.equals("MATERIALIZING")||state.equals("COMPLETE"),"SEED_CHECKPOINT_PHASE");
   need(state.equals("SCANNING")||scan.fullyVerified(),"SEED_CHECKPOINT_RAW_PROOF");
   need(!state.equals("COMPLETE")||(material.fullyVerified()&&baseRow==TOTAL),"SEED_CHECKPOINT_TYPED_PROOF");
  }
 }
 private static Checkpoint readCheckpoint(SQLiteDatabase db){
  try(Cursor c=db.rawQuery("SELECT c.asset_name,c.expected_rows,c.expected_bytes,c.legacy_target_row,c.scan_row,c.scan_offset,c.scan_prefix,c.scan_sha_state,c.material_row,c.material_offset,c.material_prefix,c.material_sha_state,c.generation,c.state,i.next_row,i.generation FROM "+CHECKPOINT+" c JOIN "+IMPORT+" i ON i.source_hash=c.source_hash WHERE c.source_hash=?",new String[]{ASSET_SHA})){return c.moveToFirst()?new Checkpoint(c):null;}
 }
 private static void initializeCheckpoint(SQLiteDatabase db){
  need(db.inTransaction(),"SEED_CHECKPOINT_INIT_TX");
  db.execSQL("INSERT OR IGNORE INTO "+IMPORT+" VALUES(?,?,4200,0,'IMPORTING',0,?)",new Object[]{ASSET_SHA,ASSET,System.currentTimeMillis()});
  if(readCheckpoint(db)!=null)return;
  int legacy;try(Cursor c=db.rawQuery("SELECT asset_name,expected_rows,next_row FROM "+IMPORT+" WHERE source_hash=?",new String[]{ASSET_SHA})){need(c.moveToFirst()&&ASSET.equals(c.getString(0))&&c.getInt(1)==TOTAL,"SEED_BASE_IDENTITY");legacy=c.getInt(2);}
  MoleculeSeedPageCP12.Cursor zero=MoleculeSeedPageCP12.Cursor.zero();
  db.execSQL("INSERT INTO "+CHECKPOINT+"(source_hash,asset_name,expected_rows,expected_bytes,legacy_target_row,scan_row,scan_offset,scan_prefix,scan_sha_state,material_row,material_offset,material_prefix,material_sha_state,generation,state,updated_at)VALUES(?,?,4200,3265020,?,0,0,?,?,0,0,?,?,0,'SCANNING',?)",new Object[]{ASSET_SHA,ASSET,legacy,zero.prefix,zero.shaState(),zero.prefix,zero.shaState(),System.currentTimeMillis()});
 }
 /** COMPLETE means verified immutable catalog only. Training/Future/CAL/REAL remain independent. */
 public static boolean completeCP12(SQLiteDatabase db){
  Checkpoint checkpoint=readCheckpoint(db);
  return checkpoint!=null&&checkpoint.state.equals("COMPLETE")&&checkpoint.scan.fullyVerified()&&checkpoint.material.fullyVerified()&&checkpoint.baseRow==TOTAL;
 }
 private static boolean equalCursor(MoleculeSeedPageCP12.Cursor a,MoleculeSeedPageCP12.Cursor b){return a.row==b.row&&a.offset==b.offset&&a.prefix.equals(b.prefix)&&Arrays.equals(a.shaState(),b.shaState());}
 private static boolean same(Checkpoint a,Checkpoint b){return b!=null&&a.generation==b.generation&&a.legacy==b.legacy&&a.state.equals(b.state)&&a.baseRow==b.baseRow&&a.baseGeneration==b.baseGeneration&&equalCursor(a.scan,b.scan)&&equalCursor(a.material,b.material);}
 private static String scanCommit(SQLiteDatabase db,Checkpoint before,MoleculeSeedPageCP12.Page page){
  need(db.inTransaction()&&page!=null&&equalCursor(page.before,before.scan)&&page.before.generation==before.generation,"SEED_SCAN_PREPARED_IDENTITY");
  Checkpoint actual=readCheckpoint(db);String phase=page.after.fullyVerified()?"MATERIALIZING":"SCANNING";
  if(actual!=null&&actual.generation==before.generation+1&&actual.state.equals(phase)&&actual.legacy==before.legacy&&actual.baseRow==before.baseRow&&actual.baseGeneration==before.baseGeneration&&equalCursor(actual.scan,page.after)&&equalCursor(actual.material,before.material))return "SEEDS_SCAN_REPLAY:"+actual.scan.row;
  need(before.state.equals("SCANNING")&&same(before,actual),"SEED_SCAN_CAS_STALE");
  db.execSQL("UPDATE "+CHECKPOINT+" SET scan_row=?,scan_offset=?,scan_prefix=?,scan_sha_state=?,generation=generation+1,state=?,updated_at=? WHERE source_hash=? AND state='SCANNING' AND generation=? AND scan_row=? AND scan_offset=? AND scan_prefix=?",new Object[]{page.after.row,page.after.offset,page.after.prefix,page.after.shaState(),phase,System.currentTimeMillis(),ASSET_SHA,before.generation,before.scan.row,before.scan.offset,before.scan.prefix});
  need(changes(db)==1,"SEED_SCAN_CAS");return "SEEDS_RAW_SCAN:"+page.after.row+"/"+TOTAL;
 }
 /** Immutable legacy rows retain their original state and model lineage. No INSERT OR REPLACE. */
 private static void verifyLegacy(SQLiteDatabase db,SeedRow r){
  for(com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeSpec node:r.ast.graphSpec.nodes)if(node.kind==com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.NodeKind.ATOM){AtomSpecCP4 s=spec(db,node.source);need(s.position==r.position&&!s.state.equals("RETIRED")&&r.ast.graphSpec.manifest.atomSpecHashes.contains(s.specHash),"SEED_REGISTRY_CAS");}
  try(Cursor c=db.rawQuery("SELECT source_seed_id,position,graph_json,output_type,compiler_revision,source_hash FROM research_molecule_seed_v2 WHERE seed_hash=?",new String[]{r.hash})){need(c.moveToFirst()&&r.sourceId.equals(c.getString(0))&&r.position==c.getInt(1)&&r.graphJson.equals(c.getString(2))&&r.outputType.equals(c.getString(3))&&REVISION.equals(c.getString(4))&&r.sourceHash.equals(c.getString(5)),"SEED_LEGACY_TYPED_DRIFT");}
 }
 private static String materialCommit(SQLiteDatabase db,Checkpoint before,MoleculeSeedPageCP12.Page done,List<SeedRow> checked){
  need(db.inTransaction()&&done!=null&&checked!=null&&checked.size()==done.rawRows.size()&&equalCursor(done.before,before.material)&&done.before.generation==before.generation,"SEED_MATERIAL_PREPARED_IDENTITY");
  Checkpoint actual=readCheckpoint(db);String phase=done.after.fullyVerified()?"COMPLETE":"MATERIALIZING";
  long expectedBase=Math.max(before.baseRow,done.after.row),expectedBaseGeneration=before.baseGeneration+(expectedBase>before.baseRow?1:0);
  if(actual!=null&&actual.generation==before.generation+1&&actual.state.equals(phase)&&actual.legacy==before.legacy&&actual.baseRow==expectedBase&&actual.baseGeneration==expectedBaseGeneration&&equalCursor(actual.scan,before.scan)&&equalCursor(actual.material,done.after)){
   for(SeedRow r:checked)verifyLegacy(db,r);return "SEEDS_MATERIAL_REPLAY:"+actual.material.row;
  }
  need(before.state.equals("MATERIALIZING")&&before.scan.fullyVerified()&&same(before,actual),"SEED_MATERIAL_CAS_STALE");
  List<SeedRow> fresh=new ArrayList<>();for(int i=0;i<checked.size();i++){SeedRow r=checked.get(i);need(r.row==before.material.row+i,"SEED_COMPLETED_ORDER");if(r.row<before.baseRow)verifyLegacy(db,r);else fresh.add(r);}
  if(!fresh.isEmpty())need(importPrepared(db,fresh,before.baseRow)==expectedBase,"SEED_BASE_ADVANCE");
  if(done.after.fullyVerified()){
   need(expectedBase==TOTAL,"SEED_BASE_COMPLETION");
   try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM research_molecule_seed_v2 WHERE compiler_revision=?",new String[]{REVISION})){need(c.moveToFirst()&&c.getInt(0)==TOTAL,"SEED_COMPLETION_COUNT");}
  }
  db.execSQL("UPDATE "+CHECKPOINT+" SET material_row=?,material_offset=?,material_prefix=?,material_sha_state=?,generation=generation+1,state=?,updated_at=? WHERE source_hash=? AND state='MATERIALIZING' AND generation=? AND material_row=? AND material_offset=? AND material_prefix=? AND scan_row=4200 AND scan_offset=3265020 AND scan_prefix=source_hash",new Object[]{done.after.row,done.after.offset,done.after.prefix,done.after.shaState(),phase,System.currentTimeMillis(),ASSET_SHA,before.generation,before.material.row,before.material.offset,before.material.prefix});
  need(changes(db)==1,"SEED_MATERIAL_CAS");return "SEEDS_TYPED_MATERIALIZED:"+done.after.row+"/"+TOTAL;
 }
 /** Invoked only by the existing Director IMPORT slot. IO/AST/compiler run outside writer. */
 public static String maintain(DbStore store,long deadlineNs){
  need(store!=null&&!store.readable().inTransaction(),"SEED_MAINTAIN_INPUT");
  if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return "YIELD_P0_PRESSURE";
  if(!AtomCatalogImportCP12.completeCP12(store.readable()))return AtomCatalogImportCP12.drainOneCP12(store,deadlineNs);
  try(MemoryBudgetCP4.Lease memory=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(1L<<20)){
   SQLiteDatabase db=store.readable();Checkpoint before=readCheckpoint(db);
   if(before==null){Boolean ok=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("CP12_MOLECULE_CHECKPOINT_INIT",tx->{initializeCheckpoint(tx);return true;});if(!Boolean.TRUE.equals(ok))throw new IllegalStateException("SEED_CHECKPOINT_INIT_COMMIT_FAILED");return "SEEDS_CHECKPOINT_INITIALIZED";}
   if(before.state.equals("COMPLETE"))return ModernFamilyFactoryCP12.maintain(store,deadlineNs);
   long prepareDeadline=deadlineNs-COMMIT_RESERVE_NS;if(System.nanoTime()>=prepareDeadline)return "SEEDS_YIELD_TIME_BUDGET";
   MoleculeSeedPageCP12.Cursor cursor=before.state.equals("SCANNING")?before.scan:before.material;
   MoleculeSeedPageCP12.Page raw;try(InputStream in=store.openResearchMoleculeCatalogAtCP12(cursor.offset)){raw=MoleculeSeedPageCP12.prepare(in,cursor,TOTAL,prepareDeadline,System::nanoTime);}
   if(raw==null)return "SEEDS_YIELD_TIME_BUDGET";
   if(before.state.equals("SCANNING")){
    String result=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("CP12_MOLECULE_RAW_SCAN",tx->scanCommit(tx,before,raw));if(result==null)throw new IllegalStateException("SEED_SCAN_COMMIT_FAILED");return result;
   }
   List<SeedRow> checked=new ArrayList<>();
   long observedRowNanos=0L;
   for(int i=0;i<raw.rawRows.size();i++){
    long available=deadlineNs-System.nanoTime();
    if(!checked.isEmpty()&&(available<=NEXT_TYPED_ROW_RESERVE_NS||observedRowNanos>=available-NEXT_TYPED_ROW_RESERVE_NS))break;
    if(System.nanoTime()>=prepareDeadline||RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())break;
    try{ResearchWorkBudgetR927.checkpoint();long rowStarted=System.nanoTime();checked.add(new SeedRow(raw.rawRows.get(i),cursor.row+i,db));observedRowNanos=Math.max(observedRowNanos,System.nanoTime()-rowStarted);}
    catch(DevelopmentProductionCP12.Wait wait){if(!"TIME_BUDGET_EXCEEDED".equals(wait.reason)||checked.isEmpty())throw wait;break;}
   }
   if(checked.isEmpty())return "SEEDS_YIELD_TIME_BUDGET";
   MoleculeSeedPageCP12.Page done=MoleculeSeedPageCP12.completedPrefix(raw,checked.size());
   String result=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("CP12_MOLECULE_TYPED_COMMIT",tx->materialCommit(tx,before,done,checked));if(result==null)throw new IllegalStateException("SEED_MATERIAL_COMMIT_FAILED");return result;
  }catch(IOException e){throw new DevelopmentProductionCP12.Wait("WAIT_INPUT_IO",e);}
  catch(IllegalStateException resource){if("ALGORITHM_MEMORY_BUDGET".equals(resource.getMessage()))throw new DevelopmentProductionCP12.Wait("MEMORY_PRESSURE",resource);throw resource;}
 }
 static AtomSpecCP4 spec(SQLiteDatabase db,String id){try(Cursor c=db.rawQuery("SELECT atom_id,spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE atom_id=?",new String[]{id})){if(!c.moveToFirst())throw new DevelopmentProductionCP12.Wait("WAIT_ATOM_REGISTRATION");return AtomSpecCP4.fromRegistry(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4),c.getString(5),c.getString(6));}}
 static int changes(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){need(c.moveToFirst(),"SEED_CHANGES");return c.getInt(0);}}
 static void need(boolean b,String e){if(!b)throw new IllegalArgumentException(e);}
 @SuppressWarnings("unchecked") static Map<String,Object> map(Object o){return(Map<String,Object>)o;}
 @SuppressWarnings("unchecked") static List<Object> list(Object o){return(List<Object>)o;}
 static String str(Map<String,Object> m,String k){need(m.get(k) instanceof String,"SEED_STRING:"+k);return(String)m.get(k);}
 static Map<String,Object> fields(Object... v){Map<String,Object> m=new TreeMap<>();for(int i=0;i<v.length;i+=2)m.put((String)v[i],v[i+1]);return m;}
 static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.ROOT,"%02x",x&255));return s.toString();}
}
