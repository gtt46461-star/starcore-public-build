package com.ethk3.novaonepro;

/** Read-only UI adapter; it never writes legacy decisions or creates a Trade. */
public final class CanonicalDecisionViewAdapterR36 {
    private CanonicalDecisionViewAdapterR36() {}

    public static Models.Decision read(DbStore store, String targetIssue) {
        CanonicalObservationWriterR36.CanonicalView v =
                CanonicalObservationWriterR36.readView(store, targetIssue);
        if (v == null) return placeholder(store, targetIssue);
        Models.Decision d = new Models.Decision();
        d.baseIssue = v.baseIssue;
        d.targetIssue = v.targetIssue;
        d.position = v.position;
        d.n1 = v.n1;
        d.n2 = v.n2;
        d.n3 = v.n3;
        d.k3Mask = AppConfig.buildK3Mask(v.n1, v.n2, v.n3);
        d.finalP = v.selectedHitP;
        d.calibratedP = v.selectedHitP;
        d.honestProbability = v.selectedHitP;
        /* R901 UI canonical parity: the home card must not depend on a transient decision
         * broadcast for its policy gate.  The active unproven-model policy gate is a frozen
         * release constant; edge stays unavailable until a legal SelectedHitP exists. */
        d.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        d.riskAdjustedGate = d.dynamicGate;
        d.gate = d.dynamicGate;
        if (!Double.isNaN(d.finalP) && !Double.isInfinite(d.finalP) && d.finalP > 0.0d) {
            d.edge = d.finalP - d.dynamicGate;
            d.rawEdge = d.edge;
        } else {
            d.edge = Double.NaN;
            d.rawEdge = Double.NaN;
        }
        Models.Trade real = "BET".equals(v.decisionAction) && store != null
                ? store.getTradeByTargetIssue(v.targetIssue) : null;
        boolean controlledReal = real != null
                && DecisionAuthorizationEngine.isTradeAuthorized(real)
                && real.position == v.position
                && real.n1 == v.n1 && real.n2 == v.n2 && real.n3 == v.n3;
        d.decision = controlledReal ? AppConfig.DECISION_BET : v.decisionAction;
        if (!AppConfig.DECISION_BET.equals(d.decision)
                && !AppConfig.DECISION_SHADOW.equals(d.decision)) d.decision = AppConfig.DECISION_HOLD;
        d.action = d.decision;
        d.statScope =
                controlledReal
                        ? AppConfig.STAT_SCOPE_REAL_TRADE
                        : AppConfig.DECISION_SHADOW.equals(d.decision)
                        ? AppConfig.STAT_SCOPE_SHADOW
                        : AppConfig.STAT_SCOPE_HOLD;
        d.sourceType = controlledReal ? real.sourceType : Models.SOURCE_TYPE_UNKNOWN;
        d.isRealtimeReal = controlledReal ? 1 : 0;
        d.authorizationValid = controlledReal ? 1 : 0;
        d.authorizedMode = d.decision;
        d.authorizationRejectCode = controlledReal ? "" : v.rejectCode;
        if (controlledReal) {
            d.authorizationId = real.authorizationId;
            d.authorizedAt = real.authorizedAt;
            d.contractDecisionId = real.contractDecisionId;
            d.candidateHash = real.candidateHash;
            d.preDrawReasonFingerprint = real.preDrawReasonFingerprint;
            d.contractValid = real.contractValid;
            d.contractStatus = real.contractStatus;
            d.featureSchemaVersion = real.featureSchemaVersion;
            d.rankingVersion = real.rankingVersion;
            d.calibrationVersion = real.calibrationVersion;
            d.gatePolicyVersion = real.gatePolicyVersion;
            d.snapshotMaxIssue = real.snapshotMaxIssue;
            d.trainedUntilIssue = real.trainedUntilIssue;
        } else if ("BET".equals(v.decisionAction)) {
            d.decision = AppConfig.DECISION_HOLD;
            d.action = AppConfig.DECISION_HOLD;
            d.statScope = AppConfig.STAT_SCOPE_HOLD;
            d.authorizationRejectCode = "R915_REAL_LEDGER_INTEGRITY_MISSING";
        }
        d.laneCandidate = v.lane;
        d.createdAt = v.createdAt;
        d.settled = v.settled;
        d.actualNumber = v.actualNumber;
        d.hit = v.hitLabel;
        d.result = v.settled == 1 ? (v.hitLabel == 1 ? "HIT" : "MISS") : AppConfig.RESULT_WAIT;
        d.buildId = V51BuildManifest.BUILD_ID;
        d.cohortId = V51BuildManifest.COHORT_ID;
        d.schemaVersion = V51BuildManifest.STATE_SCHEMA;
        d.pipelineVersion = AppConfig.R36_RUNTIME_REPAIR_VERSION;
        // R900 selected-action truth. These are persisted at pre-draw canonical time; no model
        // invocation or post-draw reconstruction occurs here.
        String selectedEvidenceStatus = hydrateSelectedEvidence(store, d, targetIssue);
        String evidence = evidenceStatus(store, targetIssue);
        String settlement = settlementStatus(store, targetIssue, v.settled);
        d.blockCode = controlledReal ? "" : d.authorizationRejectCode;
        d.reasonSummary =
                "R3.6预开奖冻结Canonical｜"
                        + v.lane
                        + "｜计算=CANONICAL_COMMITTED｜证据="
                        + evidence
                        + "｜选中动作证据="
                        + selectedEvidenceStatus
                        + "｜结算="
                        + settlement;
        d.reasonText = CanonicalReasonCP12.existingReceiptText(v.reasonJson,v.reasonHash,d.reasonSummary);
        d.selfDiagnosisText = d.reasonSummary;
        return d;
    }

    /**
     * Same-target placeholder. It intentionally never borrows a previous issue's decision.
     */
    public static Models.Decision placeholder(DbStore store, String targetIssue) {
        Models.Decision d = new Models.Decision();
        d.targetIssue = safe(targetIssue);
        d.baseIssue = store == null ? "" : safe(store.getLatestDrawIssue());
        d.decision = AppConfig.DECISION_HOLD;
        d.action = AppConfig.DECISION_HOLD;
        d.statScope = AppConfig.STAT_SCOPE_HOLD;
        d.isRealtimeReal = 0;
        d.authorizationValid = 0;
        d.finalP = Double.NaN;
        d.calibratedP = Double.NaN;
        d.honestProbability = Double.NaN;
        d.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        d.riskAdjustedGate = d.dynamicGate;
        d.gate = d.dynamicGate;
        d.edge = Double.NaN;
        d.rawEdge = Double.NaN;
        String state = "WAITING_CANONICAL";
        if (d.targetIssue.length() == 0) {
            state = "ANALYZING";
        } else if (d.baseIssue.length() > 0
                && BinanceGameContract.compareIssue(d.baseIssue, d.targetIssue) < 0) {
            String expected = AppConfig.nextIssueText(d.baseIssue);
            state = d.targetIssue.equals(expected) ? "ANALYZING" : "WAITING_DRAW";
        }
        d.blockCode = state;
        d.reasonSummary = state + "｜target=" + d.targetIssue + "｜禁止旧期决策回填";
        d.reasonText = d.reasonSummary;
        d.selfDiagnosisText = d.reasonSummary;
        d.buildId = V51BuildManifest.BUILD_ID;
        d.cohortId = V51BuildManifest.COHORT_ID;
        d.pipelineVersion = AppConfig.R36_RUNTIME_REPAIR_VERSION;
        return d;
    }

    private static String hydrateSelectedEvidence(DbStore store, Models.Decision d, String target) {
        if (store == null || d == null || safe(target).length() == 0) return "INVALID_INPUT";
        android.database.Cursor c = null;
        try {
            c = store.readable().rawQuery(
                    "SELECT raw_action_p,rank_score,rank_no,third_margin,entropy,stability,window_consensus,assignment_support,"
                            + "shape_lifecycle,shape_prototype,shape_run_length,shape_change_risk,shape_confidence"
                            + " FROM " + V51Repository.SELECTED + " WHERE target_issue=? LIMIT 1",
                    new String[] {target});
            if (!c.moveToFirst()) return hydrateFromActionSpace(store, d, target, "SELECTED_ROW_MISSING");
            double raw = c.getDouble(0), rankScore = c.getDouble(1), third = c.getDouble(3);
            double entropy = c.getDouble(4), stability = c.getDouble(5), window = c.getDouble(6), assignment = c.getDouble(7);
            d.rawP = raw;
            d.modelP = rankScore;
            d.effectiveSample = Math.max(0, c.getInt(2)); // canonical rank, not a statistical sample.
            d.championGap = third;
            d.trendLife = finite01(stability);
            // PositionAction.entropy is already normalized/clamped by the canonical V51 model.
            d.driftRisk = entropy < 0 ? -1.0d : finite01(entropy);
            d.comboGap = finite01(window);
            d.recoveryScore = finite01(assignment);
            d.stateMode = safe(c.getString(8));
            d.stateKey = safe(c.getString(9));
            d.trendKey = "run=" + Math.max(0, c.getInt(10));
            d.breakKey = c.getDouble(11) < 0 ? "changeRisk=N/A" : "changeRisk=" + c.getDouble(11);
            d.recoveryKey = c.getDouble(12) < 0 ? "confidence=N/A" : "confidence=" + c.getDouble(12);
            d.shapeKey = "R900_V51_SELECTED_EVIDENCE";
            return "SELECTED_CORE_EXACT";
        } catch (Throwable selectedReadFailure) {
            return hydrateFromActionSpace(
                    store,
                    d,
                    target,
                    "SELECTED_EXTENDED_READ_FAILED_"
                            + selectedReadFailure.getClass().getSimpleName());
        } finally { if (c != null) c.close(); }
    }

    /**
     * Compatibility fallback for an upgraded database whose selected-action extension columns
     * have not become readable yet.  It uses only the same target/position/K3 frozen action row;
     * it never borrows another issue and never recomputes a model.
     */
    private static String hydrateFromActionSpace(
            DbStore store, Models.Decision d, String target, String primaryStatus) {
        android.database.Cursor c = null;
        try {
            c = store.readable().rawQuery(
                    "SELECT raw_action_p,rank_score,rank_no,meta_blob FROM "
                            + V51Repository.ACTION_SPACE
                            + " WHERE target_issue=? AND position=? AND n1=? AND n2=? AND n3=? LIMIT 1",
                    new String[] {
                        target,
                        String.valueOf(d.position),
                        String.valueOf(d.n1),
                        String.valueOf(d.n2),
                        String.valueOf(d.n3)
                    });
            if (!c.moveToFirst()) return primaryStatus + "|ACTION_SPACE_PENDING";
            double[] meta = parseMeta(c.getString(3));
            d.rawP = c.getDouble(0);
            d.modelP = c.getDouble(1);
            d.effectiveSample = Math.max(0, c.getInt(2));
            d.championGap = value(meta, 1, -1.0d);
            d.trendLife = finite01(value(meta, 3, -1.0d));
            double entropy = value(meta, 2, -1.0d);
            d.driftRisk = entropy < 0.0d ? -1.0d : finite01(entropy);
            d.comboGap = finite01(value(meta, 20, -1.0d));
            d.recoveryScore = finite01(value(meta, 15, -1.0d));
            d.stateMode = "UNKNOWN";
            d.stateKey = "旧预测未冻结形态标签";
            d.trendKey = "run=N/A";
            d.breakKey = "changeRisk=N/A";
            d.recoveryKey = "confidence=N/A";
            d.shapeKey = "R900_V51_SELECTED_EVIDENCE";
            return primaryStatus + "|ACTION_SPACE_EXACT_FALLBACK";
        } catch (Throwable fallbackFailure) {
            return primaryStatus
                    + "|ACTION_SPACE_READ_FAILED_"
                    + fallbackFailure.getClass().getSimpleName();
        } finally {
            if (c != null) c.close();
        }
    }

    private static double[] parseMeta(String blob) {
        if (blob == null || blob.length() == 0) return new double[0];
        String[] parts = blob.split(",", -1);
        double[] out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { out[i] = Double.parseDouble(parts[i]); }
            catch (Throwable ignored) { out[i] = Double.NaN; }
        }
        return out;
    }

    private static double value(double[] values, int index, double fallback) {
        if (values == null || index < 0 || index >= values.length) return fallback;
        double value = values[index];
        return Double.isNaN(value) || Double.isInfinite(value) ? fallback : value;
    }

    private static double finite01(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x) || x < 0.0d) return -1.0d;
        return Math.max(0.0d, Math.min(1.0d, x));
    }

    public static String evidenceStatus(DbStore store, String target) {
        if (store == null || safe(target).length() == 0) return "WAITING_CANONICAL";
        android.database.Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT status FROM "
                                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                            + " WHERE target_issue=? LIMIT 1",
                                    new String[] {target});
            return c.moveToFirst() ? safe(c.getString(0)) : "MISSING";
        } catch (Throwable e) {
            return "READ_ERROR";
        } finally {
            if (c != null) c.close();
        }
    }

    public static String settlementStatus(DbStore store, String target, int settled) {
        if (store == null || safe(target).length() == 0) return "WAITING_CANONICAL";
        android.database.Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT status FROM "
                                            + DbHelper.TABLE_R36_SETTLEMENT_TASK
                                            + " WHERE target_issue=? LIMIT 1",
                                    new String[] {target});
            if (c.moveToFirst()) return safe(c.getString(0));
            return settled == 1 ? "COMPLETED" : "WAITING_DRAW";
        } catch (Throwable e) {
            return "READ_ERROR";
        } finally {
            if (c != null) c.close();
        }
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }
}
