package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

/**
 * R900 bounded Research lifecycle.
 *
 * <p>It never mutates Production and never authorizes REAL. It continuously scores only
 * future-live frozen Research outcomes, retains evidence-bearing candidates, prunes candidates
 * that are statistically below the fixed K3 random baseline, and replaces pruned capacity with
 * deterministic DSL mutations. Heavy candidate assets are deleted on prune while a compact audit
 * tombstone remains bounded.
 */
public final class ResearchEvolutionLifecycleR900 {
    private static final int MIN_OBSERVE_N = 12;
    private static final int PRUNE_MIN_N = 24;
    private static final int KEEP_MIN_N = 24;
    private static final int ACTIVE_POOL_MAX = 96;
    private static final int NEW_PER_OOS = 2;
    private static final double BASELINE = 0.30d;

    private ResearchEvolutionLifecycleR900() {}

    public static String maintain(final DbStore store) {
        if (store == null) return "R900_RESEARCH_STORE_NULL";
        if (RealtimePressureGateR811.hasActiveCriticalWork()) return "YIELD_ACTIVE_CRITICAL";
        if (!store.coordinatorR36().isR811ResearchSchemaReady()) return "YIELD_SCHEMA_NOT_READY";
        final Summary summary = new Summary();
        try {
            store.coordinatorR36().runWrite("R900_RESEARCH_LIFECYCLE", new DatabaseCoordinator.WriteRunnable() {
                public void run(SQLiteDatabase db) {
                    DbHelper.ensureR811ResearchSchema(db);
                    scoreAndPrune(db, summary);
                    generateReplacement(db, summary);
                    generateMethodBlueprintsR905(db, summary);
                    // Historical definitions, scores and prune causes remain available to full exports.
                }
            });
            String text = summary.canonical();
            store.setState("r900_research_lifecycle", text);
            store.setState("r900_research_active", String.valueOf(summary.active));
            store.setState("r900_research_kept", String.valueOf(summary.kept));
            store.setState("r900_research_pruned", String.valueOf(summary.pruned));
            store.setState("r900_research_generated", String.valueOf(summary.generated));
            ResearchEvolutionGovernorR906.Summary r906 = ResearchEvolutionGovernorR906.govern(store);
            store.setState("r906_evolution_summary", r906.canonical());
            return text;
        } catch (Throwable e) {
            String text = "R900_RESEARCH_LIFECYCLE_ERROR:" + e.getClass().getSimpleName();
            store.setState("r900_research_lifecycle", text);
            return text;
        }
    }

    private static void scoreAndPrune(SQLiteDatabase db, Summary summary) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT c.candidate_id,c.status,COUNT(o.target_issue),"
                            + "COALESCE(SUM(CASE WHEN o.hit_label=1 THEN 1 ELSE 0 END),0),"
                            + "COALESCE(MAX(o.target_issue),'')"
                            + " FROM v57_research_candidate_r811 c LEFT JOIN v57_research_candidate_outcome_r811 o"
                            + " ON o.candidate_id=c.candidate_id"
                            + " WHERE c.candidate_type='FORMULA' AND c.runtime_executable=1"
                            + " AND c.status NOT LIKE 'PRUNED_R900%' AND c.status NOT LIKE 'REJECT:%'"
                            + " GROUP BY c.candidate_id,c.status ORDER BY c.candidate_id", null);
            while (c.moveToNext()) {
                String id = safe(c.getString(0));
                String oldState = safe(c.getString(1));
                int n = Math.max(0, c.getInt(2));
                int hit = Math.max(0, c.getInt(3));
                String lastIssue = safe(c.getString(4));
                double rate = n <= 0 ? 0.0d : (double) hit / (double) n;
                double[] interval = wilson(hit, n);
                String state;
                if (n >= PRUNE_MIN_N && interval[1] < BASELINE) state = "PRUNED_R900_INEFFECTIVE";
                else if (n >= KEEP_MIN_N && interval[0] >= BASELINE) state = "KEEP_ACTIVE_R900";
                else if (n >= MIN_OBSERVE_N) state = "ACTIVE_OBSERVE_R900";
                else state = "INCUBATING_R900";

                upsertScore(db, id, n, hit, rate, interval[0], interval[1], state, lastIssue);
                if (state.startsWith("PRUNED_R900")) {
                    prune(db, id, n, hit, rate, interval[1], state);
                    summary.pruned++;
                } else {
                    summary.active++;
                    if ("KEEP_ACTIVE_R900".equals(state)) summary.kept++;
                    // Preserve exploratory/ready states before enough evidence; after N=12 expose
                    // explicit lifecycle state so UI/rotation reports effective progress.
                    if (n >= MIN_OBSERVE_N && !state.equals(oldState)) {
                        ContentValues cv = new ContentValues();
                        cv.put("status", state);
                        cv.put("updated_at", TimeSourceR811.wallNowMs());
                        db.update("v57_research_candidate_r811", cv, "candidate_id=?", new String[]{id});
                    }
                }
            }
        } finally {
            if (c != null) c.close();
        }
    }

    private static void upsertScore(SQLiteDatabase db, String id, int n, int hit, double rate,
                                    double lower, double upper, String state, String lastIssue) {
        ContentValues v = new ContentValues();
        v.put("candidate_id", id); v.put("future_n", n); v.put("hit_n", hit);
        v.put("hit_rate", rate); v.put("wilson_lower", lower); v.put("wilson_upper", upper);
        v.put("brier", -1.0d); v.put("state", state); v.put("last_issue", lastIssue);
        v.put("updated_at", TimeSourceR811.wallNowMs());
        db.insertWithOnConflict("v57_research_candidate_score_r900", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static void prune(SQLiteDatabase db, String id, int n, int hit, double rate,
                              double upper, String reason) {
        String snap = V51ChecksumEngine.sha256(id + '|' + n + '|' + hit + '|' + rate + '|' + upper + '|' + reason);
        ContentValues log = new ContentValues();
        log.put("candidate_id", id); log.put("reason", reason); log.put("future_n", n);
        log.put("hit_n", hit); log.put("hit_rate", rate); log.put("wilson_upper", upper);
        log.put("snapshot_hash", snap); log.put("created_at", TimeSourceR811.wallNowMs());
        db.insert("v57_research_prune_log_r900", null, log);

        // Delete heavy assets. Keep compact candidate tombstone + score + prune log for audit.
        db.delete("v57_research_codegen_r811", "candidate_id=?", new String[]{id});
        db.delete("v57_research_experiment_r811", "candidate_id=?", new String[]{id});
        // R906: outcome and pre-draw rows are immutable scientific evidence and MUST be retained.
        // Only recreatable generated code/temporary experiment payloads are removed.
        ContentValues cv = new ContentValues();
        cv.put("status", reason); cv.put("runtime_executable", 0); cv.put("updated_at", TimeSourceR811.wallNowMs());
        db.update("v57_research_candidate_r811", cv, "candidate_id=?", new String[]{id});
    }

    private static void generateReplacement(SQLiteDatabase db, Summary summary) {
        int active = scalarInt(db, "SELECT COUNT(*) FROM v57_research_candidate_r811 WHERE candidate_type='FORMULA'"
                + " AND runtime_executable=1 AND status NOT LIKE 'PRUNED_R900%' AND status NOT LIKE 'REJECT:%'");
        summary.active = active;
        if (active >= ACTIVE_POOL_MAX) return;
        String latest = scalarText(db, "SELECT MAX(target_issue) FROM " + DbHelper.TABLE_R35_OOS_LABEL + " WHERE strict_oos=1");
        if (!TargetIssueResolver.isValid(latest)) return;
        String lastGen = scalarText(db, "SELECT state_value FROM " + DbHelper.TABLE_APP_STATE + " WHERE state_key='r900_last_generated_oos'");
        if (latest.equals(lastGen)) return;
        int n = scalarInt(db, "SELECT COUNT(*) FROM " + DbHelper.TABLE_R35_OOS_LABEL + " WHERE strict_oos=1");
        if (n <= 0) return;
        String datasetHash = V51ChecksumEngine.sha256("R900_DATASET|" + latest + '|' + n);
        StrategyChallengerFactoryEngine.ResearchDataset dataset =
                new StrategyChallengerFactoryEngine.ResearchDataset(datasetHash, latest, n);
        StrategyChallengerFactoryEngine.FindingBundle finding =
                new StrategyChallengerFactoryEngine.FindingBundle(
                        "R900_DYNAMIC", "future-live formula replacement", "prune/keep only from future frozen outcomes",
                        "PRODUCTION_FROZEN", "ResearchDslCandidateRuntimeR811", "DecisionGateEngine,SettlementEngine,REAL_BET_UNLOCK");

        Cursor c = null;
        ArrayList<String> parentSpecs = new ArrayList<String>();
        try {
            c = db.rawQuery("SELECT canonical_spec FROM v57_research_candidate_r811 WHERE candidate_type='FORMULA'"
                    + " AND runtime_executable=1 AND status NOT LIKE 'PRUNED_R900%'"
                    + " ORDER BY CASE status WHEN 'KEEP_ACTIVE_R900' THEN 0 WHEN 'ACTIVE_OBSERVE_R900' THEN 1 ELSE 2 END,updated_at DESC LIMIT 8", null);
            while (c.moveToNext()) parentSpecs.add(safe(c.getString(0)));
        } finally { if (c != null) c.close(); }
        if (parentSpecs.isEmpty()) return;
        int create = Math.min(NEW_PER_OOS, ACTIVE_POOL_MAX - active);
        long root = ResearchDeterministicRngR811.seed("R900|" + latest + '|' + active);
        java.util.List<ResearchStrategyDslEngine.Node> novel =
                ResearchNovelFormulaSynthesizerR903.create(
                        finding.failureLayer, finding.hypothesis, dataset.datasetHash, create);
        for (int i=0; i<create; i++) {
            String parent = parentSpecs.get(i % parentSpecs.size());
            ResearchStrategyDslEngine.Node node;
            String operator;
            try {
                if ((i & 1) == 0 && i < novel.size()) {
                    node = novel.get(i);
                    operator = "R903_STRUCTURAL_SYNTHESIS";
                } else {
                    node = ResearchStrategyDslEngine.mutate(
                            ResearchStrategyDslEngine.parse(parent), root + i);
                    operator = "R900_CONTINUOUS_MUTATION";
                }
            }
            catch (Throwable bad) { continue; }
            String spec = ResearchStrategyDslEngine.canonical(node);
            StrategyChallengerFactoryEngine.CandidateSpec candidate =
                    StrategyChallengerFactoryEngine.fromHypothesis(finding, dataset,
                            StrategyChallengerFactoryEngine.CandidateType.FORMULA, spec, true, root + i);
            if (scalarInt(db, "SELECT COUNT(*) FROM v57_research_candidate_r811 WHERE candidate_id='" + sql(candidate.candidateId) + "'") > 0) continue;
            persistCandidate(db, candidate, latest, operator);
            summary.generated++;
        }
        ContentValues state = new ContentValues();
        state.put("state_key", "r900_last_generated_oos"); state.put("state_value", latest);
        state.put("updated_at", TimeSourceR811.wallNowMs());
        db.insertWithOnConflict(DbHelper.TABLE_APP_STATE, null, state, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** R905 method-level creation is independent of the 96 executable formula slots. */
    private static void generateMethodBlueprintsR905(SQLiteDatabase db, Summary summary) {
        String latest = scalarText(db, "SELECT MAX(target_issue) FROM " + DbHelper.TABLE_R35_OOS_LABEL
                + " WHERE strict_oos=1");
        if (!TargetIssueResolver.isValid(latest)) return;
        int issueN = scalarInt(db, "SELECT COUNT(*) FROM " + DbHelper.TABLE_R35_OOS_LABEL
                + " WHERE strict_oos=1");
        if (issueN <= 0) return;
        String last = scalarText(db, "SELECT state_value FROM " + DbHelper.TABLE_APP_STATE
                + " WHERE state_key='r905_last_method_blueprint_oos'");
        // A new architecture cohort every 8 strict OOS labels; first cohort is immediate.
        int lastN = scalarInt(db, "SELECT CAST(state_value AS INTEGER) FROM " + DbHelper.TABLE_APP_STATE
                + " WHERE state_key='r905_last_method_blueprint_n'");
        if (latest.equals(last) || (lastN > 0 && issueN - lastN < 8)) return;
        String datasetHash = V51ChecksumEngine.sha256("R905_METHOD_DATASET|" + latest + '|' + issueN);
        StrategyChallengerFactoryEngine.ResearchDataset dataset =
                new StrategyChallengerFactoryEngine.ResearchDataset(datasetHash, latest, issueN);
        StrategyChallengerFactoryEngine.FindingBundle finding =
                new StrategyChallengerFactoryEngine.FindingBundle(
                        "R905_METHOD_CREATION",
                        "invent architecture-level strategy from live failure distribution",
                        "future frozen OOS + ablation + calibration + regime stability",
                        "PRODUCTION_FROZEN",
                        "representation+learner+calibration+risk+falsifier",
                        "DecisionGateEngine,SettlementEngine,REAL_BET_UNLOCK");
        java.util.List<StrategyChallengerFactoryEngine.CandidateSpec> models =
                StrategyChallengerFactoryEngine.generateModelCandidates(finding, dataset);
        long now = TimeSourceR811.wallNowMs();
        int made = 0;
        for (int i = 0; i < models.size() && i < 12; i++) {
            StrategyChallengerFactoryEngine.CandidateSpec c = models.get(i);
            if (scalarInt(db, "SELECT COUNT(*) FROM v57_research_candidate_r811 WHERE candidate_id='"
                    + sql(c.candidateId) + "'") > 0) continue;
            ContentValues v = new ContentValues();
            v.put("candidate_id", c.candidateId); v.put("run_id", "R905-METHOD-" + latest);
            v.put("hypothesis_id", "R905-METHOD-CREATION"); v.put("candidate_type", "MODEL");
            v.put("parent_champion_hash", c.parentChampionHash); v.put("canonical_spec", c.canonicalSpec);
            v.put("dataset_hash", c.datasetHash); v.put("formula_ast", "");
            v.put("changed_symbols", c.changedSymbols); v.put("forbidden_symbols", c.forbiddenSymbols);
            v.put("seed", c.seed); v.put("complexity", c.complexity); v.put("code_hash", c.candidateHash);
            v.put("generation", c.generation + 1); v.put("genetic_operator", c.geneticOperator);
            v.put("parent_formula_ids", c.parentFormulaIds); v.put("feature_dependencies", c.featureDependencies);
            v.put("artifact_hash", c.candidateHash); v.put("runtime_executable", 0);
            v.put("status", "R905_METHOD_BLUEPRINT_AWAITING_ARTIFACT");
            v.put("created_at", now); v.put("updated_at", now);
            db.insertWithOnConflict("v57_research_candidate_r811", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            made++;
        }
        summary.methodBlueprints += made;
        ContentValues a = new ContentValues();
        a.put("state_key", "r905_last_method_blueprint_oos"); a.put("state_value", latest); a.put("updated_at", now);
        db.insertWithOnConflict(DbHelper.TABLE_APP_STATE, null, a, SQLiteDatabase.CONFLICT_REPLACE);
        ContentValues b = new ContentValues();
        b.put("state_key", "r905_last_method_blueprint_n"); b.put("state_value", String.valueOf(issueN)); b.put("updated_at", now);
        db.insertWithOnConflict(DbHelper.TABLE_APP_STATE, null, b, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static void persistCandidate(
            SQLiteDatabase db,
            StrategyChallengerFactoryEngine.CandidateSpec c,
            String runId,
            String geneticOperator) {
        long now = TimeSourceR811.wallNowMs();
        CandidateCodeSynthesisEngine.Bundle bundle = CandidateCodeSynthesisEngine.synthesize(c);
        ContentValues v = new ContentValues();
        v.put("candidate_id", c.candidateId); v.put("run_id", "R900-DYNAMIC-" + runId);
        v.put("hypothesis_id", "R900-DYNAMIC"); v.put("candidate_type", c.type.name());
        v.put("parent_champion_hash", c.parentChampionHash); v.put("canonical_spec", c.canonicalSpec);
        v.put("dataset_hash", c.datasetHash); v.put("formula_ast", c.canonicalSpec);
        v.put("changed_symbols", c.changedSymbols); v.put("forbidden_symbols", c.forbiddenSymbols);
        v.put("seed", c.seed); v.put("complexity", c.complexity); v.put("code_hash", bundle.bundleHash);
        v.put("generation", c.generation + 1); v.put("genetic_operator", safe(geneticOperator));
        v.put("parent_formula_ids", c.parentFormulaIds); v.put("feature_dependencies", c.featureDependencies);
        v.put("artifact_hash", c.candidateHash); v.put("runtime_executable", 1);
        v.put("status", "EXPLORATORY_COLLECT_ONLY"); v.put("created_at", now); v.put("updated_at", now);
        db.insertWithOnConflict("v57_research_candidate_r811", null, v, SQLiteDatabase.CONFLICT_IGNORE);

        ContentValues code = new ContentValues();
        code.put("candidate_id", c.candidateId); code.put("bundle_path", "research/generated/" + c.candidateId + ".java");
        code.put("java_files", "1"); code.put("patch_sha256", V51ChecksumEngine.sha256(bundle.patch));
        code.put("manifest_sha256", V51ChecksumEngine.sha256(bundle.manifest)); code.put("compile_status", bundle.status);
        code.put("owner_audit_status", "PASS_RESEARCH_ONLY_NO_OWNER"); code.put("java_source", bundle.javaSource);
        code.put("patch_text", bundle.patch); code.put("manifest_text", bundle.manifest); code.put("fixtures_text", bundle.fixtures);
        code.put("bundle_hash", bundle.bundleHash); code.put("generated_at", now);
        db.insertWithOnConflict("v57_research_codegen_r811", null, code, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static double[] wilson(int hit, int n) {
        if (n <= 0) return new double[]{0.0d,1.0d};
        double z=1.959963984540054d, p=(double)hit/(double)n, z2=z*z;
        double den=1.0d+z2/n;
        double center=(p+z2/(2.0d*n))/den;
        double half=z*Math.sqrt((p*(1.0d-p)+z2/(4.0d*n))/n)/den;
        return new double[]{Math.max(0.0d,center-half),Math.min(1.0d,center+half)};
    }

    private static int scalarInt(SQLiteDatabase db, String sql) { Cursor c=null; try { c=db.rawQuery(sql,null); return c.moveToFirst()?c.getInt(0):0; } finally { if(c!=null)c.close(); } }
    private static String scalarText(SQLiteDatabase db, String sql) { Cursor c=null; try { c=db.rawQuery(sql,null); return c.moveToFirst()?safe(c.getString(0)):""; } finally { if(c!=null)c.close(); } }
    private static String safe(String s){ return s==null?"":s.trim(); }
    private static String sql(String s){ return safe(s).replace("'","''"); }

    private static final class Summary {
        int active, kept, pruned, generated, methodBlueprints;
        String canonical(){ return "R905_LIFECYCLE|active="+active+"|kept="+kept+"|pruned="+pruned+"|generated="+generated+"|methodBlueprints="+methodBlueprints+"|baseline="+BASELINE+"|pruneN="+PRUNE_MIN_N+"|poolMax="+ACTIVE_POOL_MAX; }
    }
}
