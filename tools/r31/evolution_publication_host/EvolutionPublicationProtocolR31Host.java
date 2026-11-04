package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual sole writer/claim/fence/receipt and native SQLite; isolated synthetic engineering rows only. */
public final class EvolutionPublicationProtocolR31Host {
 static int checks;
 static void need(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static long number(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){need(c.moveToFirst(),sql);return c.getLong(0);}}
 static List<String> dump(SQLiteDatabase db,String table){List<String> values=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT * FROM "+table,null)){while(c.moveToNext()){List<String> row=new ArrayList<>();for(int i=0;i<c.getColumnCount();i++)row.add(c.isNull(i)?null:c.getString(i));values.add(JsonR27.write(row));}}Collections.sort(values);return values;}
 static void schema(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS draws(issue TEXT PRIMARY KEY,dirty INT,open_time INTEGER,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT)");}
 static final String ENGINEERING=StatsR27.sha256("HOST_ENGINEERING_EXISTING_QUALIFICATION_DO_NOT_RESET");
 static void existingEvidence(R31OwnedRuntimeFixture fixture){fixture.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31_ENGINEERING_EXISTING_EVIDENCE"),db->{
  db.execSQL("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,target_rate,updated_at) VALUES(?,99,.001,.33,-.44,100,20,6,1.2,2.3,123,'ACCUMULATING',.15,1)",new Object[]{ENGINEERING});
  db.execSQL("INSERT INTO r27_holdout_exposure VALUES(123,?,1)",new Object[]{ENGINEERING});return 0;});}
 static String seedAdmission(R31OwnedRuntimeFixture fixture,ResearchWriteQueueR27.Batch original){
  String[] child=null;String dataset=null;for(ResearchWriteQueueR27.Op op:original.ops){if(op.sql.startsWith("INSERT OR IGNORE INTO r27_policy")&&"BORN".equals(op.args[6])&&child==null)child=new String[]{(String)op.args[0],(String)op.args[1]};if(op.sql.startsWith("INSERT OR REPLACE INTO r27_evolution_cursor"))dataset=(String)op.args[3];}if(child==null)throw new AssertionError("real source supplied no born child");final String[] selected=child;final String data=dataset;String proposal=StatsR27.sha256("HOST_ENGINEERING_ADMISSION|"+child[0]);
  fixture.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31_ENGINEERING_ADMISSION_FIXTURE"),db->{List<String> runs=new ArrayList<>();for(int i=0;i<4;i++){String run=StatsR27.sha256("HOST_ENGINEERING_SOURCE_RUN_"+i);runs.add(run);Map<String,Object> contract=new LinkedHashMap<>();contract.put("abi",FrozenConfirmationR30.currentSemantics());Map<String,Object> summary=new LinkedHashMap<>();summary.put("scope","HOST_ENGINEERING_NOT_NATURAL_OR_GAIN_EVIDENCE");summary.put("sourceContract",contract);db.execSQL("INSERT INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{run,data,selected[0],"SELECT",com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26,SchemaR27.liveSourceRevision(db),0,1,StatsR27.sha256(run),"COMMITTED",JsonR27.write(summary),1});}
   Map<String,Object> payload=new LinkedHashMap<>();payload.put("candidateHash",selected[0]);payload.put("candidate",selected[1]);payload.put("sourceRuns",runs);payload.put("scope","HOST_ENGINEERING_PROTOCOL_ONLY");String text=JsonR27.write(payload);db.execSQL("INSERT INTO r30r1_proposal VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{proposal,runs.get(0),selected[0],"HOST_ENGINEERING_PROTOCOL",text,StatsR27.sha256(text),"READY","ENGINEERING_SOURCE_NOT_QUALITY",1});return 0;});return proposal;
 }
 static void stagingSchema(R31OwnedRuntimeFixture fixture){fixture.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31_ENGINEERING_EVOLUTION_SCHEMA"),db->{for(String ddl:EvolutionPublicationStoreR31.DDL)db.execSQL(ddl);return 0;});}
 static String previousSnapshot;
 static ResearchWriteQueueR27.Batch originalBatch()throws Exception{
  ReplayDatasetR27 data=ReplayDatasetR27.synthetic(5000,3185,new int[]{700,1900});EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.seed=3185;cfg.births=128;cfg.nullControl=false;
  try(FeatureCacheR27 cache=new FeatureCacheR27(data);EvolutionLoopR27 loop=new EvolutionLoopR27(cfg,cache,null,System::currentTimeMillis)){
   StoreR27.EvolutionSink sink=null;for(int i=1;i<=3;i++){if(i==3)previousSnapshot=loop.snapshot();sink=new StoreR27.EvolutionSink(RuntimeR27.LOOP,data.issues[data.size()-1]);loop.setSink(sink);loop.step();}
   Map<?,?> snapshot=(Map<?,?>)JsonR27.parse(loop.snapshot());need(((List<?>)snapshot.get("pending")).size()==128,"original 128 candidates, no size reduction");
   Map<String,Integer> states=new TreeMap<>();for(Object p:(List<?>)snapshot.get("pending")){String state=String.valueOf(((Map<?,?>)p).get("state"));states.put(state,states.getOrDefault(state,0)+1);}System.out.println("R31_EVOLUTION_ACTUAL_STATES "+JsonR27.write(states));
   sink.batch.add("INSERT OR REPLACE INTO r27_evolution_cursor(loop,generation,step,dataset_hash,rng_state,snapshot,updated_at) VALUES(?,?,?,?,?,?,?)",RuntimeR27.LOOP,loop.generation,loop.step,data.datasetHash,loop.rng.state,loop.snapshot(),System.currentTimeMillis());return sink.batch;
  }
 }
 static ResearchWriteQueueR27.Batch copy(ResearchWriteQueueR27.Batch batch){ResearchWriteQueueR27.Batch value=new ResearchWriteQueueR27.Batch();for(ResearchWriteQueueR27.Op op:batch.ops)value.ops.add(new ResearchWriteQueueR27.Op(op.sql,op.args));value.before=batch.before;value.after=batch.after;return value;}
 static void submit(R31OwnedRuntimeFixture fixture,String op,ResearchWriteQueueR27.Batch batch)throws Exception{RuntimeR27.submit(fixture.worker,op,batch,false,"ENGINEERING_FROZEN_ORIGINAL_128_ROWS");}
 public static void main(String[] args)throws Exception{
  Class.forName("org.sqlite.JDBC");ResearchWriteQueueR27.Batch original=originalBatch();Path reference=Files.createTempFile("r31-evo-reference-",".db"),target=Files.createTempFile("r31-evo-protocol-",".db");
  try(Connection rc=DriverManager.getConnection("jdbc:sqlite:"+reference);Connection tc=DriverManager.getConnection("jdbc:sqlite:"+target);Connection independent=DriverManager.getConnection("jdbc:sqlite:"+target)){
   SQLiteDatabase oldDb=new SQLiteDatabase(rc),db=new SQLiteDatabase(tc),reader=new SQLiteDatabase(independent);schema(oldDb);schema(db);db.execSQL("PRAGMA journal_mode=WAL");
   try(R31OwnedRuntimeFixture oldOwner=new R31OwnedRuntimeFixture(oldDb,"r27-evolution");R31OwnedRuntimeFixture owner=new R31OwnedRuntimeFixture(db,"r27-evolution")){
    stagingSchema(owner);existingEvidence(oldOwner);existingEvidence(owner);
    String proposal=seedAdmission(oldOwner,original);need(proposal.equals(seedAdmission(owner,original)),"same engineering source metadata");
    String born=null;for(ResearchWriteQueueR27.Op op:original.ops)if(op.sql.startsWith("INSERT OR IGNORE INTO r27_policy")&&"BORN".equals(op.args[6])){born=(String)op.args[0];break;}
    ResearchStoreR30R1.consume(original,proposal,born,0,"HOST_ENGINEERING_ACTUAL_CONSUME_CALLBACK",2);
    // Exercise bounded binary and UTF-8 chunk projection with the full 128 source still present.
    byte[] blob=new byte[65537];for(int i=0;i<blob.length;i++)blob[i]=(byte)i;String blobHash=StatsR27.sha256(blob);
    original.add("INSERT OR IGNORE INTO r30_confirm_dataset(dataset_hash,payload,payload_hash,recorded_at) VALUES(?,?,?,?)",blobHash,blob,blobHash,1);
    StringBuilder unicode=new StringBuilder();for(int i=0;i<5000;i++)unicode.append("汉😀éa");Map<String,Object> marker=new LinkedHashMap<>();marker.put("scope","HOST_ENGINEERING_ONLY");marker.put("unicode",unicode.toString());String unicodeJson=JsonR27.write(marker);
    original.add("INSERT OR IGNORE INTO r27_expert_proposal(generation,expert,proposal_json,policy_hash,recorded_at) VALUES(?,?,?,?,?)",0,"HOST_ENGINEERING_UTF8",unicodeJson,null,1);
    oldOwner.owned(()->{submit(oldOwner,"EVOLUTION_STEP_ENGINEERING_REFERENCE",copy(original));return null;});
    final EvolutionPublicationStoreR31.Prepared[] prepared={null};final ResearchWriteQueueR27.ImmutableBatch[] stageReceipt={null};
    owner.owned(()->{
     EvolutionPublicationStoreR31.CursorState old=EvolutionPublicationStoreR31.CursorState.capture(db,RuntimeR27.LOOP);
     prepared[0]=EvolutionPublicationStoreR31.prepare(db,RuntimeR27.LOOP,RuntimeCoordinatorR26.currentClaimR30(),"de9a30834025c18ce291c241a722abe437717539",old,original,0);
     List<ResearchWriteQueueR27.Batch> stages=prepared[0].stagingBatches();need(stages.size()>1,"source is really sliced");int index=0;
     for(ResearchWriteQueueR27.Batch stage:stages){ResearchWriteQueueR27.ImmutableBatch bound=stage.prepareR31("ENGINEERING","bound","r27-evolution",owner.owner,RuntimeCoordinatorR26.currentClaimR30().fence,"fixture",index,"fixture");need(bound.opCount<=64&&bound.bindBytes<=prepared[0].bindLimit,"every actual staging bind/operation slice is bounded");submit(owner,"EVOLUTION_STAGING_"+index++,stage);need(number(reader,"SELECT count(*) FROM r27_policy")==0,"private stages cannot expose canonical policies");need(number(reader,"SELECT count(*) FROM r27_evolution_cursor")==0,"private stages cannot advance cursor");}
     ResearchWriteQueueR27.Batch firstStage=stages.get(0);RuntimeCoordinatorR26.Claim active=RuntimeCoordinatorR26.currentClaimR30();
     stageReceipt[0]=firstStage.prepareR31("EVOLUTION_EXACT_STAGE_RECEIPT",StatsR27.sha256(prepared[0].id+"|ENGINEERING_STAGE_RECEIPT"),active.task,active.owner,active.fence,prepared[0].id,1000000,"ENGINEERING_STAGE_RECEIPT_V1");
     need(owner.worker.writes().submit("EVOLUTION_EXACT_STAGE_RECEIPT",stageReceipt[0],owner.worker::cancelled),"actual immutable stage receipt commits before complete");
     prepared[0].verifyPrepared(db);ResearchWriteQueueR27.Batch publication=prepared[0].publicationBatch();
     // The canonical transaction executes all grouped SQL, then really rolls back before COMMIT.
     publication.guardAfter(tx->{need(number(reader,"SELECT count(*) FROM r27_policy")==0,"reader sees old complete generation inside publication");throw new IllegalStateException("INJECTED_EVOLUTION_PUBLICATION_ROLLBACK");});
     boolean rolled=false;try{submit(owner,"EVOLUTION_STEP_ENGINEERING_ROLLBACK",publication);}catch(RuntimeException expected){rolled=expected.toString().contains("INJECTED_EVOLUTION_PUBLICATION_ROLLBACK");for(Throwable c=expected;c!=null;c=c.getCause())rolled|=String.valueOf(c.getMessage()).contains("INJECTED_EVOLUTION_PUBLICATION_ROLLBACK");}need(rolled,"publication rollback really injected");need(number(db,"SELECT count(*) FROM r27_policy")==0,"rollback leaves all canonical policy output absent");need(number(db,"SELECT count(*) FROM r31_evolution_publication WHERE state='COMPLETE'")==0,"rollback cannot publish manifest");
     long before=number(db,"SELECT total_changes()"),started=System.nanoTime();AtomicInteger lostAck=new AtomicInteger();
     com.ethk3.novaonepro.EvolutionDiagnosticFaultFixtureR31.installPostcommit(lostAck);
     try{submit(owner,"EVOLUTION_STEP_ENGINEERING_PUBLISH",prepared[0].publicationBatch());}finally{com.ethk3.novaonepro.EvolutionDiagnosticFaultFixtureR31.clear();}
     need(lostAck.get()==1,"real postcommit diagnostic failure does not retry business");long elapsed=System.nanoTime()-started,changes=number(db,"SELECT total_changes()")-before;need(changes<=prepared[0].canonicalRowWriteUpperBound,"all actual callback/receipt/trigger native changes fit frozen upper bound");
     System.out.println("R31_EVOLUTION_PUBLICATION_MEASURE "+JsonR27.write(Arrays.asList(elapsed,changes,prepared[0].envelope)));
     return null;
    });
    for(String table:new String[]{"r27_policy","r27_lineage","r26_evolution","r27_expert_proposal","r30r1_consume","r30r1_proposal","r30r1_research_measure","r27_holdout_exposure","r27_eprocess"})need(dump(oldDb,table).equals(dump(db,table)),"original canonical numeric/structural outputs unchanged:"+table);
    String expected,stored;try(Cursor c=oldDb.rawQuery("SELECT snapshot FROM r27_evolution_cursor",null)){need(c.moveToFirst(),"reference cursor");expected=c.getString(0);}try(Cursor c=db.rawQuery("SELECT snapshot FROM r27_evolution_cursor",null)){need(c.moveToFirst(),"published cursor");stored=c.getString(0);}need(stored.startsWith(EvolutionPublicationStoreR31.SNAPSHOT_PREFIX),"large checkpoint is a staged reference");need(expected.equals(EvolutionPublicationStoreR31.resolveSnapshot(reader,stored)),"entire model/RNG/exposure snapshot is byte exact");
    need(number(reader,"SELECT count(*) FROM r31_evolution_publication WHERE state='COMPLETE'")==1,"only complete manifest is visible");
    try(Cursor c=db.rawQuery("SELECT payload FROM r30_confirm_dataset WHERE dataset_hash=?",new String[]{blobHash})){need(c.moveToFirst()&&Arrays.equals(blob,c.getBlob(0)),"all256 byte values, embedded NUL and invalid UTF8 survive real SQLite chunk projection");}
    need(number(db,"SELECT count(*) FROM r30r1_research_measure WHERE kind='RESEARCH_SOURCE_CONSUMPTION'")==4,"all original four source-reference callbacks execute only at complete publication");
    need(number(db,"SELECT global_index FROM r27_eprocess WHERE policy_hash=?",ENGINEERING)==99,"permanent qualification index is not reset");
    long pageBefore=number(db,"PRAGMA page_count"),freeBefore=number(db,"PRAGMA freelist_count"),walBefore=Files.exists(Path.of(target+"-wal"))?Files.size(Path.of(target+"-wal")):-1;
    List<String> policiesBeforeCleanup=dump(db,"r27_policy");long scratchBefore=number(db,"SELECT count(*) FROM r31_evolution_op");
    owner.owned(()->{ResearchWriteQueueR27.Batch failed=EvolutionPublicationStoreR31.cleanupSlice(db,RuntimeR27.LOOP,RuntimeCoordinatorR26.currentClaimR30());need(failed!=null,"actual completed scratch awaits maintenance");failed.guardAfter(tx->{throw new IllegalStateException("INJECTED_CLEANUP_ROLLBACK_AFTER_DELETES");});boolean injected=false;try{submit(owner,"EVOLUTION_PRIVATE_CLEANUP_ROLLBACK",failed);}catch(RuntimeException failure){for(Throwable c=failure;c!=null;c=c.getCause())injected|=String.valueOf(c.getMessage()).contains("INJECTED_CLEANUP_ROLLBACK");}need(injected,"actual cleanup rollback is injected after native deletes");return null;});
    need(scratchBefore==number(db,"SELECT count(*) FROM r31_evolution_op"),"cleanup failure rolls back only scratch, never business");need(policiesBeforeCleanup.equals(dump(db,"r27_policy")),"business remains committed despite cleanup fault");
    owner.owned(()->{int slices=0;ResearchWriteQueueR27.Batch cleanup;while((cleanup=EvolutionPublicationStoreR31.cleanupSlice(db,RuntimeR27.LOOP,RuntimeCoordinatorR26.currentClaimR30()))!=null){need(cleanup.ops.size()<=64,"cleanup never raises64");submit(owner,"EVOLUTION_PRIVATE_CLEANUP_"+slices++,cleanup);}need(slices>1,"cleanup is really sliced");return null;});
    need(number(db,"SELECT count(*) FROM r31_evolution_op")==0,"completed op scratch actually removed");need(number(db,"SELECT count(*) FROM r31_evolution_value WHERE op_seq>=0")==0,"large argument scratch actually removed");need(expected.equals(EvolutionPublicationStoreR31.resolveSnapshot(reader,stored)),"cleanup preserves the complete model/RNG snapshot");
    owner.owned(()->{need(owner.worker.writes().submit("EVOLUTION_EXACT_STAGE_RECEIPT",stageReceipt[0],owner.worker::cancelled),"original durable stage receipt replays after its scratch was deleted");int retry=0;for(ResearchWriteQueueR27.Batch stage:prepared[0].stagingBatches())submit(owner,"EVOLUTION_ALREADY_COMPLETE_REENTRY_"+retry++,stage);prepared[0].verifyPrepared(db);need(prepared[0].publicationBatch().isEmpty(),"same id complete reentry is a readonly ACK, never reprojects business");return null;});
    need(number(db,"SELECT count(*) FROM r31_evolution_op")==0,"complete reentry does not regrow scratch");need(number(db,"SELECT count(*) FROM r30r1_consume")==1&&number(db,"SELECT count(*) FROM r30r1_research_measure WHERE kind='RESEARCH_SOURCE_CONSUMPTION'")==4,"postcleanup ACK cannot consume or train twice");need(policiesBeforeCleanup.equals(dump(db,"r27_policy")),"postcommit fault+cleanup+retry preserve all business output");
    long pageAfter=number(db,"PRAGMA page_count"),freeAfter=number(db,"PRAGMA freelist_count"),walAfter=Files.exists(Path.of(target+"-wal"))?Files.size(Path.of(target+"-wal")):-1;
    System.out.println("R31_PRIVATE_CLEANUP_PHYSICAL_SCOPE "+JsonR27.write(Arrays.asList(pageBefore,pageAfter,freeBefore,freeAfter,walBefore,walAfter,"FREELIST_IS_REUSABLE_NOT_PHYSICAL_FILE_RELEASE;NO_VACUUM_OR_WAL_DELETE")));
    prepared[0].close();
   }
   System.out.println("R31_EVOLUTION_PUBLICATION_HOST_PASS assertions="+checks+" source=de9a3083 scope=HOST_NATIVE_SQLITE_NOT_ANDROID_NOT_NATURAL");
  }finally{for(Path p:new Path[]{reference,target}){Files.deleteIfExists(p);Files.deleteIfExists(Path.of(p+"-wal"));Files.deleteIfExists(Path.of(p+"-shm"));}}
 }
}
