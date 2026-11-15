package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.sql.*;import java.nio.file.*;import java.util.*;
/** All nine actual default-128 phases on the original 5000-row engineering input; no quality claim. */
public final class EvolutionPublicationPhasesR31Host {
 static int checks;static void need(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  Class.forName("org.sqlite.JDBC");Path a=Files.createTempFile("r31-evo-phase-old-",".db"),b=Files.createTempFile("r31-evo-phase-new-",".db");
  try(Connection ac=DriverManager.getConnection("jdbc:sqlite:"+a);Connection bc=DriverManager.getConnection("jdbc:sqlite:"+b);FeatureCacheR27 cache=new FeatureCacheR27(ReplayDatasetR27.synthetic(5000,3185,new int[]{700,1900}))){
   SQLiteDatabase old=new SQLiteDatabase(ac),db=new SQLiteDatabase(bc);EvolutionPublicationProtocolR31Host.schema(old);EvolutionPublicationProtocolR31Host.schema(db);db.execSQL("PRAGMA journal_mode=WAL");
   try(R31OwnedRuntimeFixture reference=new R31OwnedRuntimeFixture(old,"r27-evolution");R31OwnedRuntimeFixture owner=new R31OwnedRuntimeFixture(db,"r27-evolution")){
    EvolutionPublicationProtocolR31Host.stagingSchema(owner);EvolutionPublicationProtocolR31Host.existingEvidence(reference);EvolutionPublicationProtocolR31Host.existingEvidence(owner);
    EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.seed=3185;cfg.births=128;cfg.nullControl=false;
    try(EvolutionLoopR27 loop=new EvolutionLoopR27(cfg,cache,null,System::currentTimeMillis)){
     loop.sourceCommit=com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26;List<Object> observations=new ArrayList<>();
     for(int phase=1;phase<=9;phase++){
      StoreR27.EvolutionSink sink=new StoreR27.EvolutionSink(RuntimeR27.LOOP,cache.data.issues[cache.data.size()-1]);loop.setSink(sink);loop.step();
      if(phase==2)need(((List<?>)((Map<?,?>)JsonR27.parse(loop.snapshot())).get("pending")).size()==128,"full default candidate supply");
      if(phase==8&&loop.confirmationDispatched)FrozenConfirmationStoreR30.freeze(sink.batch,loop.pendingConfirmation,cache.data);
      sink.batch.add("INSERT OR REPLACE INTO r27_evolution_cursor(loop,generation,step,dataset_hash,rng_state,snapshot,updated_at) VALUES(?,?,?,?,?,?,?)",RuntimeR27.LOOP,loop.generation,loop.step,cache.data.datasetHash,loop.rng.state,loop.snapshot(),System.currentTimeMillis());
      final int p=phase;final String snapshot=loop.snapshot();ResearchWriteQueueR27.Batch original=EvolutionPublicationProtocolR31Host.copy(sink.batch);
      reference.owned(()->{EvolutionPublicationProtocolR31Host.submit(reference,"EVOLUTION_STEP_PHASE_REFERENCE_"+p,original);return null;});
      owner.owned(()->{try(EvolutionPublicationStoreR31.Prepared prepared=sink.preparePublicationR31(db,RuntimeCoordinatorR26.currentClaimR30(),"de9a30834025c18ce291c241a722abe437717539",EvolutionPublicationStoreR31.CursorState.capture(db,RuntimeR27.LOOP))){
       int chunk=0;for(ResearchWriteQueueR27.Batch batch:prepared.stagingBatches())EvolutionPublicationProtocolR31Host.submit(owner,"EVOLUTION_PHASE_STAGING_"+p+"_"+chunk++,batch);
       prepared.verifyPrepared(db);long start=System.nanoTime();EvolutionPublicationProtocolR31Host.submit(owner,"EVOLUTION_STEP_PHASE_PUBLISH_"+p,prepared.publicationBatch());long nanos=System.nanoTime()-start;
       try(Cursor c=db.rawQuery("SELECT snapshot FROM r27_evolution_cursor WHERE loop=?",new String[]{RuntimeR27.LOOP})){need(c.moveToFirst()&&snapshot.equals(EvolutionPublicationStoreR31.resolveSnapshot(db,c.getString(0))),"phase exact model/RNG/exposure checkpoint "+p);}
       List<Object> hold=new ArrayList<>();for(WriterHoldLedgerR30.Stat stat:WriterHoldLedgerR30.snapshot())if(stat.operation.contains("PHASE_PUBLISH_"+p))hold.add(Arrays.asList(stat.operation,stat.count(),stat.holdMax(),stat.holdP(.99),stat.overBudget(),stat.overHard()));observations.add(Arrays.asList(p,prepared.originalOps,chunk,nanos,hold,prepared.envelope));return null;}});
      for(String table:new String[]{"r27_policy","r27_lineage","r26_evolution","r27_replay_score","r27_expert_proposal","r27_generation","r27_ablation","r30_frozen_confirm","r27_holdout_exposure","r27_eprocess"})need(EvolutionPublicationProtocolR31Host.dump(old,table).equals(EvolutionPublicationProtocolR31Host.dump(db,table)),"phase original canonical output unchanged "+phase+":"+table);
      System.out.println("R31_EVOLUTION_PHASE_DONE phase="+phase+" generation="+loop.generation+" next="+loop.step);
     }
     need(loop.generation==1&&loop.step==1&&loop.counter("attempts")==128,"complete original nine-step factory without resetting count/RNG");
     System.out.println("R31_EVOLUTION_PHASES_HOST_PASS "+JsonR27.write(Arrays.asList(checks,observations)));
    }
   }
  }finally{for(Path p:new Path[]{a,b}){Files.deleteIfExists(p);Files.deleteIfExists(Path.of(p+"-wal"));Files.deleteIfExists(Path.of(p+"-shm"));}}
 }
}
