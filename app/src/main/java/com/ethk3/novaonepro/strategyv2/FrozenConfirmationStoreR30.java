package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Additive persistence on the existing research writer. Preparing/serializing/replaying always happens before
 * submitting the short write batch. A reservation with no durable outcomes is never silently reopened. */
public final class FrozenConfirmationStoreR30 {
    private FrozenConfirmationStoreR30() {}
    public static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS r30_frozen_confirm(task_key TEXT PRIMARY KEY,policy_hash TEXT NOT NULL,dataset_hash TEXT NOT NULL,block_id INTEGER NOT NULL,generation INTEGER NOT NULL,packet_json TEXT NOT NULL,state TEXT NOT NULL DEFAULT 'PENDING',result_json TEXT,verified INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,completed_at INTEGER,registered_at INTEGER NOT NULL DEFAULT 0,registration_state TEXT NOT NULL DEFAULT 'PENDING')",
        "CREATE INDEX IF NOT EXISTS r30_frozen_confirm_pending ON r30_frozen_confirm(verified,registered_at,created_at)",
        "CREATE TABLE IF NOT EXISTS r30_confirm_trace(experiment_id TEXT NOT NULL,policy_hash TEXT NOT NULL,dataset_hash TEXT NOT NULL,block_id INTEGER NOT NULL,temperature REAL NOT NULL,trace_json TEXT NOT NULL,trace_hash TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(experiment_id,dataset_hash,block_id))",
        "CREATE TABLE IF NOT EXISTS r30_confirm_dataset(dataset_hash TEXT PRIMARY KEY,payload BLOB NOT NULL,payload_hash TEXT NOT NULL,recorded_at INTEGER NOT NULL)"
    };
    public interface Writer { void commit(String operation, ResearchWriteQueueR27.Batch batch); }

    public static void freeze(ResearchWriteQueueR27.Batch batch, FrozenConfirmationR30.Task task, ReplayDatasetR27 data) {
        if (task == null) return;
        if (!task.datasetHash.equals(data.datasetHash)) throw new IllegalArgumentException("R30_FROZEN_DATASET_MISMATCH");
        byte[] payload = datasetBytes(data);
        batch.add("INSERT OR IGNORE INTO r30_confirm_dataset(dataset_hash,payload,payload_hash,recorded_at) VALUES(?,?,?,?)",data.datasetHash,payload,hexHash(payload),task.frozenAt);
        batch.add("INSERT OR IGNORE INTO r30_frozen_confirm(task_key,policy_hash,dataset_hash,block_id,generation,packet_json,created_at) VALUES(?,?,?,?,?,?,?)",
                task.key,task.champion.policyHash,task.datasetHash,task.block,task.generation,task.json(),task.frozenAt);
    }
    public static FrozenConfirmationR30.Task nextPending(SQLiteDatabase ro) {
        try (Cursor c = ro.rawQuery("SELECT packet_json FROM r30_frozen_confirm WHERE state='PENDING' ORDER BY created_at,task_key LIMIT 1",null)) {
            return c.moveToFirst() ? FrozenConfirmationR30.Task.parse(c.getString(0)) : null;
        }
    }
    /** A process may restart after draws have advanced. Confirmation uses the exact committed snapshot. */
    @SuppressWarnings("unchecked") public static EvalContextR27 context(SQLiteDatabase ro, FrozenConfirmationR30.Task task) {
        try (Cursor c = ro.rawQuery("SELECT payload,payload_hash FROM r30_confirm_dataset WHERE dataset_hash=?",new String[]{task.datasetHash})) {
            if (!c.moveToFirst()) throw new IllegalStateException("R30_CONFIRM_DATASET_MISSING");
            byte[] payload = c.getBlob(0);
            if (!hexHash(payload).equals(c.getString(1))) throw new IllegalStateException("R30_CONFIRM_DATASET_CORRUPT");
            try (GZIPInputStream zip = new GZIPInputStream(new ByteArrayInputStream(payload)); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[8192]; int n; while ((n=zip.read(chunk)) >= 0) out.write(chunk,0,n);
                List<Object> raw = (List<Object>)JsonR27.parse(new String(out.toByteArray(),StandardCharsets.UTF_8));
                List<ReplayDatasetR27.Row> rows = new ArrayList<>();
                for (Object value : raw) {
                    List<Object> r = (List<Object>)value; int[] draw = new int[10]; for (int p=0;p<10;p++) draw[p]=(int)JsonR27.num(r.get(p+2));
                    rows.add(new ReplayDatasetR27.Row((String)r.get(0),((Number)r.get(1)).longValue(),draw));
                }
                ReplayDatasetR27 data = ReplayDatasetR27.of(rows);
                if (!data.datasetHash.equals(task.datasetHash)) throw new IllegalStateException("R30_CONFIRM_DATASET_CONTENT_MISMATCH");
                return new EvalContextR27(new FeatureCacheR27(data),SplitR27.of(data,null),null);
            } catch (java.io.IOException e) { throw new IllegalStateException("R30_CONFIRM_DATASET_CORRUPT",e); }
        }
    }
    private static byte[] datasetBytes(ReplayDatasetR27 data) {
        List<Object> rows = new ArrayList<>();
        for(int t=0;t<data.size();t++) { List<Object> row=new ArrayList<>();row.add(data.issues[t]);row.add(data.ordinals[t]);for(int n:data.draws[t])row.add((long)n);rows.add(row); }
        try (ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            try (GZIPOutputStream zip=new GZIPOutputStream(out)) { zip.write(JsonR27.write(rows).getBytes(StandardCharsets.UTF_8)); }
            return out.toByteArray();
        } catch(java.io.IOException e) { throw new IllegalStateException("R30_CONFIRM_DATASET_ENCODE",e); }
    }
    private static String hexHash(byte[] bytes) {
        try { byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder b=new StringBuilder();for(byte v:hash)b.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return b.toString(); }
        catch(java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    /** The first committed packet wins; a later generation cannot substitute controls or a new readout. */
    public static FrozenConfirmationR30.Task task(SQLiteDatabase ro, String key) {
        try (Cursor c = ro.rawQuery("SELECT packet_json FROM r30_frozen_confirm WHERE task_key=?",new String[]{key})) {
            if (!c.moveToFirst()) throw new IllegalStateException("R30_CONFIRM_PACKET_NOT_COMMITTED");
            FrozenConfirmationR30.Task task = FrozenConfirmationR30.Task.parse(c.getString(0));
            if (!key.equals(task.key)) throw new IllegalStateException("R30_CONFIRM_PACKET_IDENTITY_MISMATCH"); return task;
        }
    }
    public static FrozenConfirmationR30.Result result(SQLiteDatabase ro, String key) {
        try (Cursor c = ro.rawQuery("SELECT result_json FROM r30_frozen_confirm WHERE task_key=? AND state='DONE'",new String[]{key})) {
            return c.moveToFirst() && !c.isNull(0) ? FrozenConfirmationR30.Result.parse(c.getString(0)) : null;
        }
    }
    public static FrozenConfirmationR30.Evidence evidence(SQLiteDatabase ro, Writer writer) {
        return new FrozenConfirmationR30.Evidence() {
            public ReplayEvaluatorR27.Trace load(FrozenConfirmationR30.Task task, FrozenConfirmationR30.Policy policy) {
                try (Cursor c = ro.rawQuery("SELECT policy_hash,temperature,trace_json,trace_hash FROM r30_confirm_trace WHERE experiment_id=? AND dataset_hash=? AND block_id=?",
                        new String[]{policy.experimentId,task.datasetHash,Long.toString(task.block)})) {
                    if (!c.moveToFirst()) return null;
                    String text = c.getString(2);
                    if (!policy.policyHash.equals(c.getString(0)) || Double.doubleToLongBits(policy.temperature) != Double.doubleToLongBits(c.getDouble(1)) || !StatsR27.sha256(text).equals(c.getString(3)))
                        throw new IllegalStateException("R30_CONFIRM_TRACE_CORRUPT");
                    return decode(text);
                }
            }
            public void save(FrozenConfirmationR30.Task task, FrozenConfirmationR30.Policy policy, ReplayEvaluatorR27.Trace trace) {
                String text = encode(task,trace); // materialize and hash outside the database transaction
                ResearchWriteQueueR27.Batch batch = new ResearchWriteQueueR27.Batch();
                batch.before = db -> {
                    try (Cursor c = db.rawQuery("SELECT 1 FROM r30_confirm_read WHERE policy_hash=? AND dataset_hash=? AND block_id=?",
                            new String[]{policy.experimentId,task.datasetHash,Long.toString(task.block)})) {
                        if (!c.moveToFirst()) throw new IllegalStateException("R30_CONFIRM_TRACE_WITHOUT_COMMITTED_RESERVATION");
                    }
                    return 0;
                };
                batch.add("INSERT INTO r30_confirm_trace(experiment_id,policy_hash,dataset_hash,block_id,temperature,trace_json,trace_hash,recorded_at) VALUES(?,?,?,?,?,?,?,?)",
                        policy.experimentId,policy.policyHash,task.datasetHash,task.block,policy.temperature,text,StatsR27.sha256(text),System.currentTimeMillis());
                if (!policy.control) {
                    StoreR27.EvolutionSink sink = new StoreR27.EvolutionSink(RuntimeR27.LOOP,"");
                    sink.score(policy.policyHash,task.datasetHash,ReplayScoreR27.of(SplitR27.HOLDOUT,trace,task.from,task.to));
                    batch.ops.addAll(sink.batch.ops); // the same score persistence contract, no alternate scoring path
                }
                writer.commit("CONFIRM_TRACE",batch);
            }
        };
    }
    public static void complete(ResearchWriteQueueR27.Batch batch, FrozenConfirmationR30.Task task, FrozenConfirmationR30.Result result) {
        batch.add("UPDATE r30_frozen_confirm SET state='DONE',result_json=?,verified=?,completed_at=? WHERE task_key=? AND state='PENDING'",
                result.json(),result.verified ? 1 : 0,System.currentTimeMillis(),task.key);
        if (result.verified) {
            batch.add(StoreR27.R26_EVOLUTION_VERIFIED,"R27:" + RuntimeR27.LOOP + ":" + task.champion.policyHash);
            if (task.previous != null && !task.previous.policyHash.equals(task.champion.policyHash)) batch.add(
                    "INSERT OR IGNORE INTO r27_lineage(child_hash,parent_hash,role,operator,generation,created_at,origin) VALUES(?,?,'CHAMPION_VERIFIED','SUCCESSION',?,?,?)",
                    task.champion.policyHash,task.previous.policyHash,task.generation,System.currentTimeMillis(),RuntimeR27.LOOP);
        }
    }
    public static FrozenConfirmationR30.Task nextRegistration(SQLiteDatabase ro) {
        try (Cursor c = ro.rawQuery("SELECT packet_json FROM r30_frozen_confirm WHERE state='DONE' AND verified=1 AND registered_at=0 AND registration_state='PENDING' ORDER BY created_at,task_key LIMIT 1",null)) {
            return c.moveToFirst() ? FrozenConfirmationR30.Task.parse(c.getString(0)) : null;
        }
    }
    public static ResearchWriteQueueR27.Batch register(FrozenConfirmationR30.Task task, String abi, int cap) {
        PolicySpecR27 spec = task.champion.spec(); String reason = "INDEPENDENT_CONFIRMATION:" + task.key;
        ResearchWriteQueueR27.Batch batch = new ResearchWriteQueueR27.Batch();
        batch.before = db -> {
            boolean exists;
            try (Cursor c = db.rawQuery("SELECT 1 FROM r27_live_shadow WHERE policy_hash=?",new String[]{spec.policyHash()})) { exists = c.moveToFirst(); }
            if (!exists) try (Cursor c = db.rawQuery("SELECT count(*) FROM r27_live_shadow WHERE status='ACTIVE'",null)) {
                if (c.moveToFirst() && c.getLong(0) >= cap) throw new IllegalStateException("R30_CONFIRM_WAIT_SLOT");
            }
            StoreR27.insertShadow(db,spec,task.champion.temperature,reason,abi,spec.policyHash(),spec.policyHash()); return 0;
        };
        batch.add("UPDATE r30_frozen_confirm SET registered_at=?,registration_state='REGISTERED' WHERE task_key=? AND registered_at=0",System.currentTimeMillis(),task.key);
        return batch;
    }

    /** Enrich only the persisted report. The evolving loop receives no confirmation score/verdict. */
    @SuppressWarnings("unchecked") public static void generationReport(ResearchWriteQueueR27.Batch batch, SQLiteDatabase ro, int generation, FrozenConfirmationR30.Task task, FrozenConfirmationR30.Result result) {
        Map<String,Object> record;
        try (Cursor c = ro.rawQuery("SELECT detail_json FROM r27_generation WHERE loop=? AND generation=? AND dataset_hash=?",new String[]{RuntimeR27.LOOP,Integer.toString(generation),task.datasetHash})) {
            if (!c.moveToFirst()) return; // the immutable result remains authoritative even if its old report is absent
            record = new LinkedHashMap<>((Map<String,Object>)JsonR27.parse(c.getString(0)));
        }
        Map<String,Object> holdout = new LinkedHashMap<>(result.report);
        if (task.generation != generation) {
            holdout.put("originalVerified",result.verified); holdout.put("verified",false);
            holdout.put("sourceGeneration",(long)task.generation); holdout.put("reason","PREVIOUSLY_CONFIRMED_IMMUTABLE_EVIDENCE");
        }
        record.put("holdout",holdout);
        record.put("live",count(ro,"SELECT count(*) FROM r27_live_shadow WHERE status='ACTIVE'"));
        Map<String,Object> counters = new LinkedHashMap<>((Map<String,Object>)record.get("counters"));
        try (Cursor c = ro.rawQuery("SELECT count(*) FROM r30_frozen_confirm WHERE verified=1 OR (task_key=? AND ?=1)",new String[]{task.key,result.verified ? "1" : "0"})) {
            if (c.moveToFirst()) counters.put("verifiedImprovements",c.getLong(0));
        }
        counters.put("prospectiveQualified",count(ro,"SELECT count(*) FROM r27_eprocess WHERE state='QUALIFIED'")); record.put("counters",counters);
        batch.add("UPDATE r27_generation SET champion_holdout_hr=?,prev_champion_holdout_hr=?,null_champion_holdout_hr=?,i00_champion_holdout_hr=?,paired_lcb=?,verified=?,detail_json=? WHERE loop=? AND generation=? AND dataset_hash=?",
                StoreR27.EvolutionSink.holdHr(holdout,"championHoldout"),StoreR27.EvolutionSink.holdHr(holdout,"previousHoldout"),StoreR27.EvolutionSink.holdHr(holdout,"nullHoldout"),StoreR27.EvolutionSink.holdHr(holdout,"i00Holdout"),
                holdout.get("pairedLcbVsPrevious"),Boolean.TRUE.equals(holdout.get("verified")) ? 1 : 0,JsonR27.write(record),RuntimeR27.LOOP,generation,task.datasetHash);
    }
    static long count(SQLiteDatabase ro, String sql) { try (Cursor c = ro.rawQuery(sql,null)) { return c.moveToFirst() ? c.getLong(0) : 0; } }

    private static String encode(FrozenConfirmationR30.Task task, ReplayEvaluatorR27.Trace trace) {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("n",(long)trace.u.length); m.put("from",(long)task.from); m.put("to",(long)task.to);
        m.put("position",(long)trace.position); m.put("temperature",trace.temperature); List<Object> rows = new ArrayList<>();
        for (int t = task.from; t < task.to; t++) rows.add(Arrays.asList(trace.skipped[t],trace.act[t],(long)trace.hit[t],trace.pHit[t],trace.pActual[t],trace.u[t],
                (long)trace.k3[t*3],(long)trace.k3[t*3+1],(long)trace.k3[t*3+2]));
        m.put("rows",rows); return JsonR27.write(m);
    }
    @SuppressWarnings("unchecked") private static ReplayEvaluatorR27.Trace decode(String text) {
        Map<String,Object> m = (Map<String,Object>)JsonR27.parse(text);
        ReplayEvaluatorR27.Trace trace = new ReplayEvaluatorR27.Trace((int)JsonR27.num(m.get("n")),(int)JsonR27.num(m.get("from")),(int)JsonR27.num(m.get("to")),(int)JsonR27.num(m.get("position")),JsonR27.num(m.get("temperature")));
        List<Object> rows = (List<Object>)m.get("rows");
        if (rows.size() != trace.to - trace.from) throw new IllegalStateException("R30_CONFIRM_TRACE_ROWS");
        for (int t = trace.from; t < trace.to; t++) {
            List<Object> r = (List<Object>)rows.get(t-trace.from); trace.skipped[t] = Boolean.TRUE.equals(r.get(0)); trace.act[t] = Boolean.TRUE.equals(r.get(1));
            trace.hit[t] = (byte)JsonR27.num(r.get(2)); trace.pHit[t] = JsonR27.num(r.get(3)); trace.pActual[t] = JsonR27.num(r.get(4)); trace.u[t] = JsonR27.num(r.get(5));
            for (int k = 0; k < 3; k++) trace.k3[t*3+k] = (int)JsonR27.num(r.get(6+k));
        }
        return trace;
    }
}
