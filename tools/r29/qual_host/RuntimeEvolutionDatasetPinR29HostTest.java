package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

/** Actual RuntimeR27.EvolutionTask and its 40ms continuation, with only the DB writer bound to a real SQLite TX.
 * Synthetic engineering fixture; no runtime task, restore, continuation or pin behavior is mocked. */
public final class RuntimeEvolutionDatasetPinR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_RUNTIME_EVOLUTION_PIN|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static String cursor(SQLiteDatabase db){return text(db,"SELECT generation||'|'||step||'|'||rng_state||'|'||dataset_hash||'|'||updated_at||'|'||snapshot FROM r27_evolution_cursor WHERE loop='MAIN'");}
    static ReplayDatasetR27 future(ReplayDatasetR27 old){List<ReplayDatasetR27.Row> rows=new ArrayList<>();for(int t=0;t<old.size();t++)rows.add(new ReplayDatasetR27.Row(old.issues[t],old.ordinals[t],old.draws[t]));StatsR27.SplitMix rng=new StatsR27.SplitMix(1007);for(int t=0;t<1000;t++){long ordinal=old.ordinals[old.size()-1]+1+t;rows.add(new ReplayDatasetR27.Row("NEW"+ordinal,ordinal,ReplayDatasetR27.randomPermutation(rng)));}return ReplayDatasetR27.of(rows).tail(5000);}
    static final class Fixture implements AutoCloseable{
        final Path directory;final SQLiteDatabase db,ro;final RuntimeR27 runtime;final RuntimeR27.EvolutionTask task;final ResearchPoolR27.Worker worker;int commits;
        Fixture(ReplayDatasetR27 old,ReplayDatasetR27 current,EvolutionLoopR27 saved,boolean pin,String replacement,int stepDelta,long rngDelta)throws Exception{
            directory=Files.createTempDirectory("r29-runtime-evolution-pin-");Path path=directory.resolve("research.db");db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+path));db.execSQL("PRAGMA journal_mode=WAL");
            db.beginTransaction();try{db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER DEFAULT 0)");RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            ResearchWriteQueueR27.Batch initial=new ResearchWriteQueueR27.Batch();if(pin)EvolutionDatasetPinR29.append(initial,EvolutionDatasetPinR29.prepare(old));initial.add("INSERT INTO r27_evolution_cursor VALUES('MAIN',?,?,?,?,?,1)",saved.generation,saved.step+stepDelta,old.datasetHash,saved.rng.state+rngDelta,replacement==null?saved.snapshot():replacement);execute(initial);
            ro=SQLiteDatabase.openDatabase(path.toString(),null,SQLiteDatabase.OPEN_READONLY);runtime=new RuntimeR27(null,"SYNTHETIC_RUNTIME_PIN_"+UUID.randomUUID());runtime.evoData=current;runtime.evoCache=new FeatureCacheR27(current);
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((operation,batch)->{try{execute(batch);commits++;}catch(Exception e){throw e instanceof RuntimeException?(RuntimeException)e:new IllegalStateException(e);}},()->false);
            Field field=ResearchPoolR27.class.getDeclaredField("writes");field.setAccessible(true);field.set(runtime.pool,writes);worker=runtime.pool.new Worker(ro,ro,"synthetic-actual-evolution");task=runtime.new EvolutionTask();
        }
        void execute(ResearchWriteQueueR27.Batch batch)throws Exception{db.beginTransaction();try{ResearchWriteQueueR27.execute(db,batch,"RUNTIME_PIN_HOST");db.setTransactionSuccessful();}finally{db.endTransaction();}}
        ResearchPoolR27.Outcome turn()throws Exception{long deadline=System.nanoTime()+60_000_000_000L;for(int i=0;i<2000&&System.nanoTime()<deadline;i++){ResearchPoolR27.Outcome out=task.step(worker);if(out.units>0||out.reason!=null&&out.reason.startsWith("WAITING_IMMUTABLE_EVOLUTION_DATASET_PIN"))return out;if(out.reason==null||!out.reason.startsWith("COOPERATIVE_"))throw new AssertionError("UNEXPECTED_ACTUAL_TASK_WAIT:"+out.reason);}throw new AssertionError("ACTUAL_RUNTIME_TURN_EXCEEDED_60S");}
        public void close(){task.close();runtime.close();ro.close();db.close();try(java.util.stream.Stream<Path>files=Files.walk(directory)){files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}catch(Exception ignored){}}
    }
    static void missing(ReplayDatasetR27 old,ReplayDatasetR27 current,EvolutionLoopR27 saved,String id)throws Exception{
        try(Fixture f=new Fixture(old,current,saved,false,null,0,0)){String before=cursor(f.db);for(int i=0;i<3;i++){ResearchPoolR27.Outcome out=f.turn();check(id+"_WAIT_"+i,out.units==0&&out.reason.startsWith("WAITING_IMMUTABLE_EVOLUTION_DATASET_PIN")&&before.equals(cursor(f.db))&&f.commits==0&&f.runtime.evo==null);}check(id+"_DOES_NOT_CREATE_FAKE_PIN",!SchemaR27.tableExists(f.db,EvolutionDatasetPinR29.META));}
    }
    static void failsUnchanged(ReplayDatasetR27 old,ReplayDatasetR27 current,EvolutionLoopR27 saved,String bad,int delta,long rng,String id)throws Exception{
        try(Fixture f=new Fixture(old,current,saved,true,bad,delta,rng)){String before=cursor(f.db);boolean threw=false;try{f.turn();}catch(RuntimeException expected){threw=true;}check(id,threw&&before.equals(cursor(f.db))&&f.commits==0&&f.runtime.evo==null);}
    }
    public static void main(String[]args)throws Exception{
        ReplayDatasetR27 old=ReplayDatasetR27.synthetic(4200,101,new int[]{1000,3100}),current=future(old);EvolutionLoopR27.Config cfg=EvolutionDatasetPinR29HostTest.config();EvolutionLoopR27 saved=new EvolutionLoopR27(cfg,new FeatureCacheR27(old),new EvolutionDatasetPinR29HostTest.Sink(),()->123L);saved.ledger().restore(8,StatsR27.sha256("kept-look"));saved.ledger().restoreControl(9,StatsR27.sha256("kept-control"));saved.step();saved.step();String checkpoint=saved.snapshot();
        check("R1_ACTUAL_FIXTURE_IS_MIDGEN_AND_NEW_5000_WINDOW_EXPIRED_OLD_TAIL",saved.step==3&&current.size()==5000&&current.ordinals[0]>old.ordinals[0]&&!current.datasetHash.equals(old.datasetHash));
        try(Fixture f=new Fixture(old,current,saved,true,null,0,0)){
            EvolutionLoopR27 expected=EvolutionLoopR27.restore(checkpoint,cfg,new FeatureCacheR27(old),new EvolutionDatasetPinR29HostTest.Sink(),System::currentTimeMillis);expected.leafWeight=f.runtime.leafWeights();expected.derivedLeaves=new ArrayList<>(f.runtime.derived);expected.step();String next=expected.snapshot();ResearchPoolR27.Outcome out=f.turn();
            check("R2_ACTUAL_RUNTIME_TURN_RESUMES_OLD_PIN_AND_COMMITS_NEXT_GATE",out.units==1&&f.commits==1&&f.runtime.evo!=null&&f.runtime.evo.step==4&&f.runtime.evo.context().data.datasetHash.equals(old.datasetHash)&&f.runtime.evoData.datasetHash.equals(old.datasetHash));
            check("R3_ACTUAL_NEXT_SNAPSHOT_RNG_AND_BOTH_LEDGERS_ARE_BYTE_EXACT",next.equals(f.runtime.evo.snapshot())&&next.equals(text(f.db,"SELECT snapshot FROM r27_evolution_cursor WHERE loop='MAIN'"))&&expected.rng.state==f.runtime.evo.rng.state&&expected.ledger().snapshot().equals(f.runtime.evo.ledger().snapshot())&&expected.ledger().controlSnapshot().equals(f.runtime.evo.ledger().controlSnapshot()));
            check("R4_NEWER_WINDOW_STAYS_PENDING_UNTIL_GENERATION_BOUNDARY",f.runtime.pendingData!=null&&f.runtime.pendingData.datasetHash.equals(current.datasetHash)&&f.runtime.pendingCache.data.datasetHash.equals(current.datasetHash));
            check("R5_ACTUAL_COMMIT_BINDS_PIN_CURSOR_AND_CHECKSUM",EvolutionDatasetPinR29.read(f.db,old.datasetHash)!=null&&n(f.db,"SELECT step FROM r27_evolution_cursor WHERE loop='MAIN'")==4&&StatsR27.sha256(next).equals(text(f.db,"SELECT snapshot_hash FROM r29_evolution_checkpoint_checksum WHERE loop='MAIN'")));
        }
        EvolutionLoopR27 boundary=new EvolutionLoopR27(cfg,new FeatureCacheR27(old),new EvolutionDatasetPinR29HostTest.Sink(),()->123L);boundary.ledger().restore(81,StatsR27.sha256("boundary-kept-look"));ReplayDatasetR27 freshest=future(current);
        try(Fixture f=new Fixture(old,current,boundary,true,null,0,0)){
            f.runtime.pendingData=freshest;f.runtime.pendingCache=new FeatureCacheR27(freshest);ResearchPoolR27.Outcome out=f.turn();
            check("R13_REAL_STEP1_RESTORE_REBINDS_FRESHEST_WINDOW_BEFORE_DATASET_STEP",out.units==1&&f.commits==1&&f.runtime.evo.step==2&&f.runtime.evo.context().data.datasetHash.equals(freshest.datasetHash)&&f.runtime.evoData.datasetHash.equals(freshest.datasetHash)&&freshest.datasetHash.equals(text(f.db,"SELECT dataset_hash FROM r27_evolution_cursor WHERE loop='MAIN'"))&&EvolutionDatasetPinR29.read(f.db,freshest.datasetHash)!=null);
            check("R14_STEP1_CONSUMES_PENDING_ONCE_AND_RETAINS_RESTORED_RNG_LEDGER",f.runtime.pendingData==null&&f.runtime.pendingCache==null&&f.runtime.evo.rng.state==boundary.rng.state&&f.runtime.evo.ledger().snapshot().equals(boundary.ledger().snapshot())&&EvolutionDatasetPinR29.read(f.db,old.datasetHash)!=null);
        }
        failsUnchanged(old,current,boundary,"invalid-boundary-checkpoint",0,0,"R15_STEP1_REBIND_CANNOT_BYPASS_INVALID_OLD_SNAPSHOT");
        missing(old,current,saved,"R6_MISSING_PIN_DIFFERENT_HASH");
        List<ReplayDatasetR27.Row> shift=new ArrayList<>();for(int t=0;t<old.size();t++)shift.add(new ReplayDatasetR27.Row(old.issues[t],old.ordinals[t]+100,old.draws[t]));ReplayDatasetR27 sameHashDifferentOrdinals=ReplayDatasetR27.of(shift);check("R7_V1_HASH_DOES_NOT_BIND_ABSOLUTE_ORDINALS",sameHashDifferentOrdinals.datasetHash.equals(old.datasetHash));missing(old,sameHashDifferentOrdinals,saved,"R8_MISSING_PIN_SAME_HASH");
        failsUnchanged(old,current,saved,"not-valid-json",0,0,"R9_BAD_SNAPSHOT_THROWS_WITHOUT_NEW_LOOP_OR_CURSOR_CHANGE");
        failsUnchanged(old,current,saved,null,1,0,"R10_COLUMN_STEP_VS_JSON_MISMATCH_THROWS_AND_PRESERVES_CHECKPOINT");
        failsUnchanged(old,current,saved,null,0,1,"R11_COLUMN_RNG_VS_JSON_MISMATCH_THROWS_AND_PRESERVES_CHECKPOINT");
        try(Fixture f=new Fixture(old,current,saved,true,null,0,0)){
            EvolutionDatasetPinR29HostTest.removeGuards(f.db,EvolutionDatasetPinR29.CHUNKS);f.db.execSQL("UPDATE "+EvolutionDatasetPinR29.CHUNKS+" SET chunk_blob=zeroblob(length(chunk_blob)) WHERE chunk_index=0");String before=cursor(f.db);boolean failed=false;try{f.turn();}catch(RuntimeException expected){failed=true;}check("R12_ACTUAL_RUNTIME_CORRUPT_PIN_THROWS_WITHOUT_REINITIALIZING",failed&&before.equals(cursor(f.db))&&f.commits==0&&f.runtime.evo==null);
        }
        System.out.println("R29_RUNTIME_EVOLUTION_PIN|ALL_PASS|checks="+checks+"|ACTUAL_RUNTIME_TURN_AND_40MS_CONTINUATION_REAL_SQLITE_SYNTHETIC_ONLY");
    }
}
