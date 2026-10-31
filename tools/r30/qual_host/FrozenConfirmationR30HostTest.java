package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

/** Production confirmation/registration over disk SQLite, followed by actual JVM death and reopen. */
public final class FrozenConfirmationR30HostTest {
    static void require(boolean yes,String code) { if (!yes) throw new AssertionError(code); }
    static void commit(Connection c, SQLiteDatabase db, ResearchWriteQueueR27.Batch batch) {
        try { c.setAutoCommit(false); ResearchWriteQueueR27.execute(db,batch,"frozen-host"); c.commit(); c.setAutoCommit(true); }
        catch (Exception e) { try { c.rollback(); c.setAutoCommit(true); } catch (Exception ignored) {} throw new IllegalStateException(e); }
    }
    static long count(Connection c,String sql) throws Exception { try (Statement s=c.createStatement(); ResultSet r=s.executeQuery(sql)) { r.next(); return r.getLong(1); } }
    static String text(Connection c,String sql) throws Exception { try (Statement s=c.createStatement(); ResultSet r=s.executeQuery(sql)) { return r.next()?r.getString(1):null; } }
    static PolicySpecR27 policy(int position) {
        return new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("cross",2,1)),position,PolicySpecR27.Rule.MARGIN,0.01,0,1,false,0,0,
                IdeaFamilyR27.meta("I04","BIRTH",0,1,"synthetic frozen confirmation fixture"));
    }
    static ReplayDatasetR27 data() { return ReplayDatasetR27.synthetic(5000,11,new int[0]).planted(0.6,5); }
    static EvolutionLoopR27.Config config() { EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.nullControl=false;return cfg; }
    public static void main(String[] args) throws Exception {
        if (args.length==0) {
            Path file=Files.createTempFile("r30-frozen-confirm-",".db");
            try { child("write-and-kill",file);child("restart-and-check",file); } finally { Files.deleteIfExists(file); }
            return;
        }
        Class.forName("org.sqlite.JDBC");
        try (Connection c=DriverManager.getConnection("jdbc:sqlite:"+args[1])) {
            SQLiteDatabase db=new SQLiteDatabase(c);
            if (args[0].equals("write-and-kill")) try (Statement s=c.createStatement()) {
                for (String ddl:SchemaR27.DDL) if (ddl.startsWith("CREATE TABLE")) s.execute(ddl);
                s.execute(HoldoutLedgerR27.CONFIRM_DDL);for (String ddl:FrozenConfirmationStoreR30.DDL) s.execute(ddl);
                s.execute("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER NOT NULL)");s.execute("INSERT INTO draws VALUES('202610310001',0)");
                s.execute("CREATE TABLE r26_evolution(attempt_id TEXT PRIMARY KEY,campaign_id TEXT,policy_hash TEXT,parent TEXT,operation TEXT,category TEXT,structure_hash TEXT,behavior_novel INTEGER,research_accepted INTEGER DEFAULT 0,verified_improvement INTEGER DEFAULT 0,prospective_qualified INTEGER DEFAULT 0)");
            }
            Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);Unsafe u=(Unsafe)f.get(null);
            ResearchPoolR27 pool=(ResearchPoolR27)u.allocateInstance(ResearchPoolR27.class);
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((op,batch)->commit(c,db,batch),()->false);
            u.putObject(pool,u.objectFieldOffset(ResearchPoolR27.class.getDeclaredField("writes")),writes);
            ResearchPoolR27.Worker worker=pool.new Worker(db,"frozen-confirm-test");
            RuntimeR27 runtime=(RuntimeR27)u.allocateInstance(RuntimeR27.class);
            runtime.operatorAbi=new HashMap<>();for (String op:SemanticAbiR27.operators(policy(1)))runtime.operatorAbi.put(op,"host-fixture-abi");
            if (args[0].equals("write-and-kill")) {
                StoreR27.EvolutionSink sink=new StoreR27.EvolutionSink(RuntimeR27.LOOP,"202610310001");
                EvolutionLoopR27 loop=new EvolutionLoopR27(config(),new FeatureCacheR27(data()),sink,()->1L);
                loop.stepDataset();loop.champion=policy(1);loop.championTau=1;
                loop.i00Champion=new IdeaI00RandomBaselineR27().generate(new StatsR27.SplitMix(13),new IdeaFamilyR27.Context());loop.i00Tau=1;
                sink.policy(loop.champion,"BORN","fixture",0,loop.context().data.datasetHash);
                loop.stepHoldout();loop.stepRecord();loop.stepPromote();loop.step=9;
                FrozenConfirmationR30.Task task=loop.pendingConfirmation;FrozenConfirmationStoreR30.freeze(sink.batch,task,loop.context().data);
                sink.batch.add("INSERT INTO r27_evolution_cursor(loop,generation,step,dataset_hash,rng_state,snapshot,updated_at) VALUES(?,?,?,?,?,?,?)",
                        RuntimeR27.LOOP,0,9,loop.context().data.datasetHash,loop.rng.state,loop.snapshot(),1);
                commit(c,db,sink.batch);ConfirmReadStoreR30.restore(db,loop.ledger());ConfirmReadStoreR30.bind(worker,loop.ledger());
                FrozenConfirmationR30.Result result=runtime.confirmFrozen(worker,loop,task);
                require(result.verified,"PLANTED_FROZEN_CANDIDATE_DID_NOT_VERIFY:"+result.json());
                require(count(c,"SELECT count(*) FROM r30_confirm_trace")==2,"RAW_OUTCOMES_NOT_COMMITTED");
                ResearchWriteQueueR27.Batch report=new ResearchWriteQueueR27.Batch();FrozenConfirmationStoreR30.generationReport(report,db,0,task,result);commit(c,db,report);
                require(count(c,"SELECT verified FROM r27_generation WHERE generation=0")==1,"REPORT_NOT_PERSISTED");
                require("FROZEN_PENDING_EXTERNAL".equals(((Map<?,?>)loop.lastRecord.get("holdout")).get("status")),"CONFIRM_RESULT_FED_BACK_TO_EVOLUTION");
                runtime.registerConfirmed(worker,0);
                require(count(c,"SELECT registered_at FROM r30_frozen_confirm")==0,"FULL_POOL_LOST_QUEUED_CANDIDATE");
                runtime.registerConfirmed(worker,32);
                require(count(c,"SELECT count(*) FROM r27_eprocess")==1,"FUTURE_NOT_REGISTERED");
                require(count(c,"SELECT n FROM r27_eprocess")==0,"HISTORY_COUNTED_AS_FUTURE");
                require(task.champion.spec().fullJson().equals(text(c,"SELECT spec_json FROM r27_live_shadow WHERE status='ACTIVE'")),"REGISTERED_POLICY_NOT_FROZEN_SPEC");
                // Another candidate dies after its committed reservation, before any outcome can be saved.
                loop.champion=policy(2);loop.stepHoldout();FrozenConfirmationR30.Task consumed=loop.pendingConfirmation;
                ResearchWriteQueueR27.Batch pending=new ResearchWriteQueueR27.Batch();FrozenConfirmationStoreR30.freeze(pending,consumed,loop.context().data);commit(c,db,pending);
                loop.ledger().confirm(consumed.champion.experimentId,consumed.datasetHash,consumed.block,consumed.from,consumed.to);
                // A third pending packet must survive even if the normal evolution cursor/dataset has advanced.
                loop.champion=policy(3);loop.stepHoldout();ResearchWriteQueueR27.Batch orphan=new ResearchWriteQueueR27.Batch();
                FrozenConfirmationStoreR30.freeze(orphan,loop.pendingConfirmation,loop.context().data);commit(c,db,orphan);
                require(count(c,"SELECT count(*) FROM r30_confirm_read")==3,"RESERVATION_NOT_DURABLE");
                System.out.println("R30_FROZEN_SQLITE|POSITIVE_CONFIRMED|REPORT_ONLY_NO_SELECTION_FEEDBACK|FUTURE_INDEX_1|FULL_POOL_RETAINS_PACKET|HALT_AFTER_SECOND_RESERVATION");System.out.flush();
                Runtime.getRuntime().halt(0);
            }
            for (String ddl:FrozenConfirmationStoreR30.DDL)db.execSQL(ddl); // additive/idempotent restoration
            String cursor=text(c,"SELECT snapshot FROM r27_evolution_cursor WHERE loop='MAIN'");
            EvolutionLoopR27 loop=EvolutionLoopR27.restore(cursor,config(),new FeatureCacheR27(data()),new StoreR27.EvolutionSink(RuntimeR27.LOOP,"202610310001"),()->1L);
            require(loop.step==9&&loop.confirmationDispatched,"DISPATCH_NOT_RESTORED");
            ConfirmReadStoreR30.restore(db,loop.ledger());ConfirmReadStoreR30.bind(worker,loop.ledger());
            EvalContextR27 context=loop.context();int[][] poison=context.labels.clone();for(int t=context.split.selectTo;t<poison.length;t++)poison[t]=null;
            Field field=EvolutionLoopR27.class.getDeclaredField("ctx");field.setAccessible(true);field.set(loop,new EvalContextR27(context.cache,context.split,poison));
            FrozenConfirmationR30.Task original=FrozenConfirmationStoreR30.task(db,loop.pendingConfirmation.key);
            FrozenConfirmationR30.Result result=runtime.confirmFrozen(worker,loop,original);
            require(result.verified,"COMMITTED_RESULT_LOST_ON_RESTART");
            FrozenConfirmationR30.Result replay=FrozenConfirmationR30.evaluate(original,loop.context(),loop.ledger(),FrozenConfirmationStoreR30.evidence(db,(op,b)->commit(c,db,b)));
            Map<String,Object> before=new LinkedHashMap<>(result.report),after=new LinkedHashMap<>(replay.report);
            // The second candidate's committed reservation changed the current budget after the original result.
            // Compare all scores/verdicts exactly; exposure totals describe their own observation times.
            before.remove("blockExposures");after.remove("blockExposures");before.remove("controlExposures");after.remove("controlExposures");
            require(JsonR27.write(before).equals(JsonR27.write(after)),"CACHED_RAW_OUTCOMES_CHANGED:"+JsonR27.write(before)+" vs "+JsonR27.write(after));
            require(count(c,"SELECT count(*) FROM r30_confirm_read")==3,"RESTART_REREAD_CONFIRMATION");
            runtime.registerConfirmed(worker,32);require(count(c,"SELECT global_index FROM r27_eprocess")==1,"RETRY_RESET_GLOBAL_ALPHA_INDEX");
            require(count(c,"SELECT count(*) FROM r27_eprocess")==1,"RETRY_DUPLICATED_FUTURE");
            String consumedText=text(c,"SELECT packet_json FROM r30_frozen_confirm WHERE state='PENDING' AND policy_hash='"+policy(2).policyHash()+"'");
            FrozenConfirmationR30.Task consumed=FrozenConfirmationR30.Task.parse(consumedText);
            FrozenConfirmationR30.Result incomplete=runtime.confirmFrozen(worker,loop,consumed);
            require(!incomplete.verified&&"INSUFFICIENT".equals(incomplete.report.get("status")),"CONSUMED_WITHOUT_OUTCOMES_NOT_REJECTED");
            require(String.valueOf(incomplete.report.get("reason")).contains(HoldoutLedgerR27.CONFIRM_REUSED),"MISSING_CONSUMED_REASON");
            require(count(c,"SELECT count(*) FROM r30_confirm_trace")==2&&count(c,"SELECT count(*) FROM r30_confirm_read")==3,"LOST_OUTCOME_WAS_REREAD");
            FrozenConfirmationR30.Task orphan=FrozenConfirmationR30.Task.parse(text(c,"SELECT packet_json FROM r30_frozen_confirm WHERE state='PENDING'"));
            ReplayDatasetR27 advanced=ReplayDatasetR27.synthetic(5000,99,new int[0]);
            field.set(loop,new EvalContextR27(new FeatureCacheR27(advanced),SplitR27.of(advanced,null),null));
            FrozenConfirmationR30.Result recovered=runtime.confirmFrozen(worker,loop,orphan);
            require("COMPLETED".equals(recovered.report.get("status")),"ORPHAN_FROZEN_DATASET_NOT_RECOVERED");
            require(count(c,"SELECT count(*) FROM r30_confirm_dataset")==1,"DUPLICATED_FROZEN_DATASET");
            require(count(c,"SELECT count(*) FROM r30_confirm_read")==4&&count(c,"SELECT count(*) FROM r30_confirm_trace")==3,"ORPHAN_EVIDENCE_COUNTS_WRONG");
            field.set(loop,new EvalContextR27(context.cache,context.split,poison));
            // A new generation cannot replace the first committed readout/control choice.
            loop.champion=original.champion.spec();loop.championTau=7;loop.stepHoldout();ResearchWriteQueueR27.Batch duplicate=new ResearchWriteQueueR27.Batch();
            FrozenConfirmationStoreR30.freeze(duplicate,loop.pendingConfirmation,loop.context().data);commit(c,db,duplicate);
            require(FrozenConfirmationStoreR30.task(db,original.key).json().equals(original.json()),"FIRST_FROZEN_PACKET_WAS_REPLACED");
            String raw=text(c,"SELECT trace_json FROM r30_confirm_trace WHERE experiment_id='"+original.champion.experimentId+"'");
            db.execSQL("UPDATE r30_confirm_trace SET trace_json=trace_json||' ' WHERE experiment_id=?",new Object[]{original.champion.experimentId});
            try { FrozenConfirmationStoreR30.evidence(db,(op,b)->commit(c,db,b)).load(original,original.champion);throw new AssertionError("CORRUPT_TRACE_ACCEPTED"); }
            catch(IllegalStateException expected){require(expected.getMessage().equals("R30_CONFIRM_TRACE_CORRUPT"),"WRONG_CORRUPTION_FAILURE");}
            db.execSQL("UPDATE r30_confirm_trace SET trace_json=? WHERE experiment_id=?",new Object[]{raw,original.champion.experimentId});
            System.out.println("R30_FROZEN_SQLITE|PROCESS_RESTART|RAW_CACHE_NO_REREAD_WITH_NULL_CONFIRM_LABELS|CONSUMED_WITHOUT_OUTCOME_INSUFFICIENT|ORPHAN_DATASET_RECOVERED|FIRST_PACKET_IMMUTABLE|QUALIFICATION_INDEX_PRESERVED|CORRUPT_TRACE_REJECTED|PASS");
        }
    }
    static void child(String mode,Path database)throws Exception{
        Process p=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),FrozenConfirmationR30HostTest.class.getName(),mode,database.toString()).redirectErrorStream(true).start();
        if(!p.waitFor(45,TimeUnit.SECONDS)){p.destroyForcibly();throw new AssertionError("FROZEN_CHILD_TIMEOUT:"+mode);}
        String output=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);System.out.print(output);require(p.exitValue()==0,"FROZEN_CHILD_FAILED:"+mode);
    }
}
