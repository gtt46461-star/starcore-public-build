package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

/** R27 persistence helpers: dataset loading from a read-only connection and the evolution sink that turns one
 * evolution step into one atomic write batch (step outputs + cursor), so a killed process never duplicates births or
 * lineage (all statements are keyed; INSERT OR IGNORE / guarded UPDATE). */
public final class StoreR27 {
    private StoreR27() {}

    /** R27-031: R27 attempts feed the R26 seven-counter ledger (also executed by tools/r27/test_r27_migration.py). */
    public static final String R26_EVOLUTION_INSERT = "INSERT OR IGNORE INTO r26_evolution(attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash,behavior_novel) VALUES(?,?,?,?,?,?,?,?)";
    public static final String R26_EVOLUTION_ACCEPTED = "UPDATE r26_evolution SET research_accepted=1 WHERE attempt_id=?";
    public static final String R26_EVOLUTION_VERIFIED = "UPDATE r26_evolution SET verified_improvement=1 WHERE attempt_id=?";
    public static final String R26_EVOLUTION_QUALIFIED = "UPDATE r26_evolution SET prospective_qualified=1 WHERE policy_hash=? AND (attempt_id LIKE 'R27:%:BORN:%' OR (attempt_id LIKE 'R27:%' AND behavior_novel=1))";

    /** Newest `limit` clean draws, ascending, with SU1 sequence ordinals (gaps preserved). */
    /** R30-B03-02 (M15): the hot draws table holds at most this many rows (AppConfig.MAX_LOCAL_HISTORY_SAVE, enforced by
     * the CP12 retention tail); older replay rows come only from verified imported history (r27_history_import). */
    public static final int HOT_DRAWS_LIMIT_R30 = com.ethk3.novaonepro.AppConfig.MAX_LOCAL_HISTORY_SAVE;
    public static ReplayDatasetR27 loadDataset(SQLiteDatabase ro,int limit) {
        return loadDataset(ro,limit,null,null,null,true);
    }
    /** For online learning the exact label anchors a bounded read of canonical hot rows. */
    static ReplayDatasetR27 loadDatasetThrough(SQLiteDatabase ro,int limit,String issue) {
        if(!TargetIssueResolver.isValid(issue))throw new IllegalArgumentException("R34_ATOMNET_LABEL_ISSUE");
        return loadDataset(ro,limit,issue,null,null,false);
    }
    static ReplayDatasetR27 loadDatasetPageR341(SQLiteDatabase ro,int limit,String high,String low,String after) {
        return loadDataset(ro,limit,high,low,after,false);
    }
    private static ReplayDatasetR27 loadDataset(SQLiteDatabase ro,int limit,String through,String from,String after,boolean includeImports) {
        int hotLimit = Math.min(limit, HOT_DRAWS_LIMIT_R30);
        List<ReplayDatasetR27.Row> rows = new ArrayList<>(hotLimit);
        final class RawDrawR33 { final String issue; final int[] numbers; final long opened,known;
            RawDrawR33(String issue,int[] numbers,long opened,long known){this.issue=issue;this.numbers=numbers;this.opened=opened;this.known=known;} }
        // Bounded hot tail only. Close its SQLite cursor before any parkable
        // calendar/identity validation, so yielding does not pin a WAL reader.
        List<RawDrawR33> rawR33=new ArrayList<>(hotLimit);
        java.util.Set<String> sourceColumns=new java.util.HashSet<>();try(Cursor schema=ro.rawQuery("PRAGMA table_info(draws)",null)){while(schema.moveToNext())sourceColumns.add(schema.getString(1));}
        String opened=sourceColumns.contains("open_time")?"open_time":"NULL",known=sourceColumns.contains("first_seen_at")?"first_seen_at":"NULL";
        List<String> bindings=new ArrayList<>(3);
        String filter="";
        if(through!=null){filter+=" AND issue<=?";bindings.add(through);}
        if(from!=null){filter+=" AND issue>=?";bindings.add(from);}
        if(after!=null&&!after.isEmpty()){filter+=" AND issue<?";bindings.add(after);}
        try (Cursor c = ro.rawQuery("SELECT issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,"+opened+","+known
                +" FROM draws WHERE dirty=0"+filter+" ORDER BY issue DESC LIMIT "+hotLimit,bindings.toArray(new String[0]))) {
            while (c.moveToNext()) {
                String issue = c.getString(0);
                int[] p = new int[10]; for (int i = 0; i < 10; i++) p[i] = c.getInt(i + 1);
                rawR33.add(new RawDrawR33(issue,p,c.isNull(11)?-1:c.getLong(11),c.isNull(12)?-1:c.getLong(12)));
            }
        }
        for(int i=0;i<rawR33.size();i++){
            if((i&63)==0)ExecutionBudgetR26.poll();RawDrawR33 raw=rawR33.get(i);
            if(!TargetIssueResolver.isValid(raw.issue))continue;
            rows.add(new ReplayDatasetR27.Row(raw.issue,TargetIssueResolver.sequenceOrdinalSU1(raw.issue),raw.numbers,raw.opened,raw.known,ReplayDatasetR27.READER_ABI_R31,"DRAW_FIRST_SEEN"));
        }
        rawR33.clear();
        // Short consumers keep their original CSV cutoff. The combined active replay, including
        // verified older imports, must still fit the same rolling history as the live reader.
        int remaining = HOT_DRAWS_LIMIT_R30 - rows.size();
        if (includeImports && !rows.isEmpty() && remaining > 0)
            HistoryImportR27.appendOlder(ro, rows, rows.get(rows.size() - 1).issue, remaining);
        return ReplayDatasetR27.of(rows);
    }

    /** Physical census is collected outside the writer. A missing run is not a measured zero-byte app. */
    public static Map<String,Object> storageR30(SQLiteDatabase read){
        Map<String,Object> out=new java.util.LinkedHashMap<>();out.put("status","UNKNOWN");out.put("budgetBytes",ResearchRetentionR30.DEFAULT_BUDGET_BYTES);
        try(Cursor c=read.rawQuery("SELECT report_json,measured_at,budget_bytes FROM r30_retention_run ORDER BY measured_at DESC,run_id DESC LIMIT 1",null)){
            if(c.moveToFirst()){out.put("lastRun",JsonR27.parse(c.getString(0)));out.put("measuredAt",c.getLong(1));out.put("budgetBytes",c.getLong(2));out.put("status","RECORDED_UNVALIDATED");}
        }catch(RuntimeException unavailable){out.put("sourceError",unavailable.getClass().getSimpleName());}
        out.put("collectionAction","ResearchRetentionStoreR30.step：在原retention任务测量完整应用目录；不可读或保护证据导致超预算时保持未知/受限，不自动删证据");return out;
    }

    public static byte[] pack(double[] v) { ByteBuffer b = ByteBuffer.allocate(v.length * 8); for (double x : v) b.putDouble(x); return b.array(); }
    public static double[] unpack(byte[] b) { ByteBuffer x = ByteBuffer.wrap(b); double[] v = new double[b.length / 8]; for (int i = 0; i < v.length; i++) v[i] = x.getDouble(); return v; }

    /** Only the proposed identities are queried, regardless of the number of previous generations. */
    private static final String RETRYABLE_INPUT_REJECTION = "state='REJECTED' "
            + "AND length(dataset_hash)=64 AND dataset_hash<>? "
            + "AND state_reason GLOB 'BIRTH_REJECTED(OUTPUT):AVAILABLE_ROWS=[0-9]*'";

    static Set<String> recordedPolicies(SQLiteDatabase db, Collection<String> candidates, String datasetHash) {
        List<String> hashes = new ArrayList<>(candidates);
        Set<String> recorded = new HashSet<>();
        for (int start = 0; start < hashes.size(); start += 128) {
            List<String> page = hashes.subList(start, Math.min(start + 128, hashes.size()));
            String parameters = String.join(",", Collections.nCopies(page.size(), "?"));
            String sql = "SELECT policy_hash FROM r27_policy WHERE policy_hash IN (" + parameters + ")";
            List<String> arguments = new ArrayList<>(page);
            if (datasetHash != null && datasetHash.matches("[0-9a-f]{64}")) {
                sql += " AND (NOT (" + RETRYABLE_INPUT_REJECTION + ") OR EXISTS(SELECT 1 FROM r26_evolution e"
                        + " WHERE e.attempt_id='R27:REJECTED:'||r27_policy.policy_hash||':'||?))";
                arguments.add(datasetHash);
                arguments.add(datasetHash);
            }
            try (Cursor rows = db.rawQuery(sql, arguments.toArray(new String[0]))) {
                while (rows.moveToNext()) recorded.add(rows.getString(0));
            }
        }
        return recorded;
    }

    /** Sink buffering one step into a batch. */
    public static final class EvolutionSink implements EvolutionLoopR27.Sink {
        public final ResearchWriteQueueR27.Batch batch = new ResearchWriteQueueR27.Batch(); private final String loop, birthIssue;
        private final Set<String> births = new HashSet<>();
        private String birthDataset;
        public final List<Object[]> promotions = new ArrayList<>(); public final List<String[]> demotions = new ArrayList<>();
        public EvolutionSink(String loop, String birthIssue) { this.loop = loop; this.birthIssue = birthIssue == null ? "" : birthIssue; }
        /** Freeze the complete owner batch, including its original admission guards and cursor. */
        public EvolutionPublicationStoreR31.Prepared preparePublicationR31(SQLiteDatabase read, RuntimeCoordinatorR26.Claim claim, String sourceCommit, EvolutionPublicationStoreR31.CursorState expectedCursor) {
            if (!births.isEmpty()) {
                batch.guardBefore(writer -> {
                    if (!recordedPolicies(writer, births, birthDataset).isEmpty()) throw new ResearchStoreR30R1.StaleResearch();
                    return 0;
                });
            }
            return EvolutionPublicationStoreR31.prepare(read,loop,claim,sourceCommit,expectedCursor,batch,promotions.size());
        }
        public void policy(PolicySpecR27 spec, String state, String reason, int generation, String datasetHash) {
            recordPolicy(spec,state,reason,generation,datasetHash,FormulaIdentityR341.origin(spec),false);
        }

        @Override public void admitted(PolicySpecR27 spec,String reason,int generation,String datasetHash,
                String category,boolean behaviorNovel) {
            recordPolicy(spec,"BORN",reason,generation,datasetHash,category,behaviorNovel);
        }

        private void recordPolicy(PolicySpecR27 spec,String state,String reason,int generation,String datasetHash,
                String category,boolean behaviorNovel) {
            long now = System.currentTimeMillis(); String idea = spec.meta == null ? "" : spec.meta.idea;
            String policy = spec.policyHash();
            String acceptedAttempt = "R27:" + loop + ":BORN:" + policy;
            String legacyAttempt = "R27:" + loop + ':' + policy;
            boolean born = "BORN".equals(state);
            if (born) {
                if (datasetHash == null || !datasetHash.matches("[0-9a-f]{64}")
                        || (birthDataset != null && !birthDataset.equals(datasetHash))) {
                    throw new IllegalStateException("R341_BIRTH_DATASET_SCOPE_MISMATCH");
                }
                birthDataset = datasetHash;
                batch.add("INSERT OR IGNORE INTO r26_evolution(attempt_id,campaign_id,policy_hash,operation,category,structure_hash) "
                        + "SELECT 'R27:REJECTED:'||policy_hash||':'||dataset_hash,'R27_'||?,policy_hash,state_reason,"
                        + "'ORIGINAL_INPUT_REJECTION',? FROM r27_policy WHERE policy_hash=? AND " + RETRYABLE_INPUT_REJECTION,
                        loop, FormulaIdentityR341.structure(spec), policy, datasetHash);
                batch.add("UPDATE r27_policy SET spec_json=?,idea_family=?,birth_generation=?,birth_issue=?,"
                        + "dataset_hash=?,state='BORN',state_reason=?,updated_at=? WHERE policy_hash=? AND " + RETRYABLE_INPUT_REJECTION,
                        spec.fullJson(), idea, generation, birthIssue, datasetHash, trim(reason), now, policy, datasetHash);
                births.add(policy);
            }
            batch.add("INSERT OR IGNORE INTO r27_policy(policy_hash,spec_json,idea_family,birth_generation,birth_issue,dataset_hash,state,state_reason,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    policy, spec.fullJson(), idea, generation, birthIssue, datasetHash, state, trim(reason), now, now);
            if (!"BORN".equals(state) && !"REJECTED".equals(state)) {
                batch.add("UPDATE r27_policy SET state=?,state_reason=?,updated_at=? WHERE policy_hash=? AND state NOT IN ('LIVE')", state, trim(reason), now, spec.policyHash());
            }
            // R27-031: the R26 seven-counter ledger (r26_evolution) is fed by the R27 loop, one keyed row per attempt.
            if (born || state.equals("REJECTED")) {
                String op = spec.meta == null || spec.meta.operator == null ? "SEED" : spec.meta.operator;
                String attempt = born ? acceptedAttempt : "R27:REJECTED:" + policy + ':' + datasetHash;
                batch.add(R26_EVOLUTION_INSERT,
                        attempt, "R27_" + loop + "_G" + generation, policy, spec.meta == null || spec.meta.parents.isEmpty() ? null : spec.meta.parents.get(0),
                        born ? op : trim(reason), born ? category : "BIRTH_REJECTION",
                        FormulaIdentityR341.structure(spec), behaviorNovel ? 1 : 0);
            } else if (state.equals("ACTIVE")) {
                batch.add(R26_EVOLUTION_ACCEPTED, acceptedAttempt);
                batch.add(R26_EVOLUTION_ACCEPTED + " AND behavior_novel=1", legacyAttempt);
            }
        }
        public void lineage(String child, String parent, String role, String operator, int generation) {
            batch.add("INSERT OR IGNORE INTO r27_lineage(child_hash,parent_hash,role,operator,generation,created_at,origin) VALUES(?,?,?,?,?,?,?)", child, parent, role, operator == null ? "" : operator, generation, System.currentTimeMillis(), loop);
        }
        public void score(String h, String d, ReplayScoreR27 s) {
            batch.add("INSERT OR REPLACE INTO r27_replay_score(policy_hash,dataset_hash,split,n_sched,n_act,hits,brier,logloss,ece,worst_block_hr,max_losing_streak,paired_u_mean,paired_u_lcb,skipped_gap,computed_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    h, d, s.split, s.nSched, s.nAct, s.hits, n(s.brier), n(s.logloss), n(s.ece), n(s.worstBlockHr), s.maxLosingStreak, s.pairedUMean, n(s.pairedULcb), s.skippedGap, System.currentTimeMillis());
        }
        @SuppressWarnings("unchecked")
        public void generation(Map<String, Object> r) {
            Map<String, Object> h = (Map<String, Object>) r.get("holdout");
            Double ch = holdHr(h, "championHoldout"), pv = holdHr(h, "previousHoldout"), nu = holdHr(h, "nullHoldout"), i0 = holdHr(h, "i00Holdout");
            Object lcb = h == null ? null : h.get("pairedLcbVsPrevious");
            batch.add("INSERT OR REPLACE INTO r27_generation(generation,loop,dataset_hash,births,rejected,no_op,duplicates,survivors,retired,champion_hash,champion_select_u,champion_holdout_hr,prev_champion_holdout_hr,null_champion_holdout_hr,i00_champion_holdout_hr,paired_lcb,verified,detail_json,started_at,finished_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    num(r.get("generation")), loop, String.valueOf(r.get("datasetHash")), num(r.get("births")), num(r.get("rejected")), num(r.get("noOp")), num(r.get("duplicates")), num(r.get("survivors")), num(r.get("retired")),
                    r.get("championHash"), r.get("championSelectU"), ch, pv, nu, i0, lcb, h != null && Boolean.TRUE.equals(h.get("verified")) ? 1 : 0, JsonR27.write(r), num(r.get("startedAt")), num(r.get("finishedAt")));
        }
        @SuppressWarnings("unchecked") static Double holdHr(Map<String, Object> h, String k) {
            if (h == null || !(h.get(k) instanceof Map)) return null; Object v = ((Map<String, Object>) h.get(k)).get("hitRate"); return v instanceof Number ? ((Number) v).doubleValue() : null;
        }
        public void promote(PolicySpecR27 spec, double tau, int generation, String reason) {
            promotions.add(new Object[] {spec, tau, generation, reason});
            batch.add(R26_EVOLUTION_VERIFIED, "R27:" + loop + ":BORN:" + spec.policyHash());
            batch.add(R26_EVOLUTION_VERIFIED + " AND behavior_novel=1", "R27:" + loop + ":" + spec.policyHash());
        }
        public void demote(String hash, String reason) { demotions.add(new String[] {hash, reason}); batch.add("UPDATE r27_live_shadow SET status='RETIRED_TO_ARCHIVE',reason=?,updated_at=? WHERE policy_hash=? AND status IN ('ACTIVE','ACTIVE_REPAIR1')", reason, System.currentTimeMillis(), hash); }
        public void expert(ExpertProposalsR27.Proposal p, int generation) {
            batch.add("INSERT OR IGNORE INTO r27_expert_proposal(generation,expert,proposal_json,policy_hash,recorded_at) VALUES(?,?,?,?,?)", generation, p.expert, p.json(), p.spec == null ? null : p.spec.policyHash(), System.currentTimeMillis());
        }
        public void ablation(int generation, String champion, List<Object[]> rows) {
            for (Object[] r : rows) batch.add("INSERT OR REPLACE INTO r27_ablation(generation,champion_hash,node_index,removed_child,branch,delta_u,delta_logloss) VALUES(?,?,?,?,?,?,?)", generation, champion, r[0], r[1], r[2], r[3], r[4]);
        }
        public void holdoutLook(long block, String h) { batch.add("INSERT OR IGNORE INTO r27_holdout_exposure(block_id,policy_hash,looked_at) VALUES(?,?,?)", block, h, System.currentTimeMillis()); }
        public void behaviorDuplicate(BehaviorSignatureR30.Duplicate d) {
            batch.add("INSERT OR IGNORE INTO r30_behavior_dup(dataset_hash,probe_set_hash,signature,kept_policy,dropped_policy,stage,recorded_at) VALUES(?,?,?,?,?,?,?)",
                    d.datasetHash, d.probeSetHash, d.signature, d.keptPolicy, d.droppedPolicy, d.stage, System.currentTimeMillis());
        }
        static Object n(double d) { return Double.isFinite(d) ? (Object) d : null; }
        static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0; }
        static String trim(String s) { return s == null ? "" : s.length() > 400 ? s.substring(0, 400) : s; }
    }

    /** Inside a write transaction: registers a verified champion as a live shadow with a permanent global index. */
    public static void insertShadow(SQLiteDatabase db, PolicySpecR27 spec, double tau, String reason, String semanticAbi, String id, String basePolicy) {
        insertShadow(db, spec, tau, reason, semanticAbi, id, basePolicy, System::currentTimeMillis);
    }

    static void insertShadow(SQLiteDatabase db, PolicySpecR27 spec, double tau, String reason, String semanticAbi, String id, String basePolicy,
                             java.util.function.LongSupplier registrationClock) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM r27_live_shadow WHERE policy_hash=?", new String[] {id})) { if (c.moveToFirst()) return; }
        long index = 1;
        try (Cursor c = db.rawQuery("SELECT coalesce(max(global_index),0)+1 FROM r27_eprocess", null)) { if (c.moveToFirst()) index = Math.max(1, c.getLong(0)); }
        try (Cursor c = db.rawQuery("SELECT coalesce(max(global_index),0)+1 FROM r27_live_shadow", null)) { if (c.moveToFirst()) index = Math.max(index, c.getLong(0)); }
        long now = registrationClock.getAsLong();
        String latest = RuntimeR27.latestIssue(db);
        if (latest == null) throw new IllegalStateException("R29_REGISTRATION_REQUIRES_CALENDAR");
        long firstOrdinal=TargetIssueResolver.sequenceOrdinalSU1(latest)+1;
        long open=RuntimeR27.latestOpen(db);
        if(open>0&&open<=now&&now>=open+60_000L-LiveShadowsR27.FREEZE_MARGIN_MS)
            firstOrdinal+=1+(now-(open+60_000L-LiveShadowsR27.FREEZE_MARGIN_MS))/60_000L;
        db.execSQL("INSERT INTO r29_qual_calendar(experiment_id,first_ordinal,registered_at,origin) VALUES(?,?,?,'REGISTERED_BEFORE_FUTURE')",
                new Object[] {id, firstOrdinal, now});
        double rate = SelectorPolicyR30.registeredRate(spec);
        db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi) VALUES(?,?,?,?,?,?,?, ?,?,-1,?,?)",
                new Object[] {id, spec.fullJson(), tau, pack(new double[10]), index, RepairCompatibilityR30R1.activeStatus(spec), reason, now, now, basePolicy, semanticAbi});
        SequentialQualificationR27.State s = new SequentialQualificationR27.State(id, index, rate);
        db.execSQL("INSERT OR IGNORE INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,target_rate,updated_at) VALUES(?,?,?,0,0,0,0,0,0,0,-1,?,?,?)",
                new Object[] {id, index, s.alpha, SequentialQualificationR27.ACCUMULATING, rate, now});
        db.execSQL("UPDATE r27_policy SET state='LIVE',state_reason=?,updated_at=? WHERE policy_hash=?", new Object[] {reason, now, spec.policyHash()});
    }

    public static SequentialQualificationR27.State readEprocess(SQLiteDatabase db, String id) {
        try (Cursor c = db.rawQuery("SELECT global_index,target_rate,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state FROM r27_eprocess WHERE policy_hash=?", new String[] {id})) {
            if (!c.moveToFirst()) return null;
            SequentialQualificationR27.State s = new SequentialQualificationR27.State(id, c.getLong(0), c.getDouble(1));
            s.logUp = c.getDouble(2); s.logDown = c.getDouble(3); s.n = c.getLong(4); s.acts = c.getLong(5); s.hits = c.getLong(6); s.sumU = c.getDouble(7); s.sumU2 = c.getDouble(8); s.lastOrdinal = c.getLong(9); s.state = c.getString(10);
            return s;
        }
    }
    public static void writeEprocess(ResearchWriteQueueR27.Batch b, SequentialQualificationR27.State s, long expectedLastOrdinal) {
        b.add("UPDATE r27_eprocess SET log_e_up=?,log_e_down=?,n=?,acts=?,hits=?,sum_u=?,sum_u2=?,last_ordinal=?,state=?,updated_at=? WHERE policy_hash=? AND last_ordinal=?",
                s.logUp, s.logDown, s.n, s.acts, s.hits, s.sumU, s.sumU2, s.lastOrdinal, s.state, System.currentTimeMillis(), s.policyHash, expectedLastOrdinal);
        if (SequentialQualificationR27.QUALIFIED.equals(s.state)) b.add(R26_EVOLUTION_QUALIFIED, s.policyHash);
    }
}
