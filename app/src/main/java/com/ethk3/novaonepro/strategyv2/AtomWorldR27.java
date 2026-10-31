package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R27-033/034/035/036/043/044 atom world on the research pool: atom replay frame (chunked), atom scoring,
 * derived atoms and molecules, AtomNet pre-training / continual training with FUTURE Brier pairs, and the FTRL
 * linear learner on the same samples. Training samples come from draws by the PIT rule, never only from freezes. */
public final class AtomWorldR27 {
    private AtomWorldR27() {}
    static final int CHUNK = 120;
    static final boolean RAW = true; // 7 group features + 165 raw atoms (docs/06 6.2, configurable)

    // ------------------------------------------------------------------ atoms
    static ResearchPoolR27.Outcome atomsStep(RuntimeR27 rt, ResearchPoolR27.Worker w) throws Exception {
        ReplayDatasetR27 d = rt.evoData; EvolutionLoopR27 evo = rt.evo;
        if (d == null || evo == null) return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-evolution)", 0, -1);
        SplitR27 sp = evo.context().split;
        if (!d.datasetHash.equals(rt.frameDataset)) {
            synchronized (rt) { rt.frameDataset = d.datasetHash; rt.frameCursor = 0; int n = sp.holdoutTo; rt.frameValues = new double[n][]; rt.framePresent = new boolean[n][]; rt.frameHit = new int[n]; rt.framePos = new int[n]; rt.frameActual = new int[n]; rt.frameIssue = new String[n]; }
        }
        if (rt.frameCursor < rt.frameValues.length) {
            int from = rt.frameCursor, to = Math.min(rt.frameValues.length, from + CHUNK);
            AtomReplayR27.Frame f = AtomReplayR27.buildLegacy(d, from, to);
            for (int i = 0; i < f.rows(); i++) { int t = from + i; rt.frameValues[t] = f.values[i]; rt.framePresent[t] = f.present[i]; rt.frameHit[t] = f.hit[i]; rt.framePos[t] = f.position[i]; rt.frameActual[t] = f.actual[i]; rt.frameIssue[t] = d.issues[t]; }
            rt.frameCursor = to;
            if (to < rt.frameValues.length) return ResearchPoolR27.Outcome.progress(to - from, to);
            score(rt, w, d, sp);
            return ResearchPoolR27.Outcome.progress(to - from, to);
        }
        long units = 0;
        if (rt.derivedDue) { units += derived(rt, w, evo); rt.derivedDue = false; }
        units += molecules(rt, w, evo);
        return units > 0 ? ResearchPoolR27.Outcome.progress(units, evo.generation) : ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", evo.generation, evo.generation);
    }

    static void score(RuntimeR27 rt, ResearchPoolR27.Worker w, ReplayDatasetR27 d, SplitR27 sp) throws Exception {
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis();
        int[] el = AtomReplayR27.eligible(); Map<Integer, String> states = new LinkedHashMap<>();
        for (int a : el) {
            AtomScoreR27.Score s = AtomScoreR27.score(a, rt.frameValues, rt.framePresent, rt.frameHit, sp.selectFrom,sp.selectTo);
            states.put(a, s.state);
            contribution(b,GenesisSchemaRegistryR915.atoms().get(a).id,d.datasetHash,sp.selectFrom,sp.selectTo,s.contribution(),now);
            b.add("INSERT OR REPLACE INTO r27_atom_score(atom_id,atom_index,dataset_hash,n_select,gain_select,lcb_select,ucb_select,n_holdout,gain_holdout,lcb_holdout,missing_rate,state,derived,definition,computed_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0,'',?)",
                    GenesisSchemaRegistryR915.atoms().get(a).id, a, d.datasetHash, s.nSelect, f(s.select.mean), f(s.select.lcb(StatsR27.Z95)), f(s.select.ucb(StatsR27.Z95)), s.nHoldout, f(s.holdout.mean), f(s.holdout.lcb(StatsR27.Z95)), s.missingRate(), s.state, now);
        }
        RuntimeR27.submit(w, "ATOM_SCORES", b);
        synchronized (rt.atomStates) { rt.atomStates.clear(); rt.atomStates.putAll(states); }
    }

    static long derived(RuntimeR27 rt, ResearchPoolR27.Worker w, EvolutionLoopR27 evo) throws Exception {
        List<PolicySpecR27> programs = new ArrayList<>(); for (EvolutionLoopR27.Member m : new ArrayList<>(evo.population)) programs.add(m.spec);
        List<DerivedAtomR27.Candidate> cands = DerivedAtomR27.frequentSubtrees(programs, 8);
        DerivedAtomR27.Candidate best = null; StringBuilder why = new StringBuilder();
        for (DerivedAtomR27.Candidate c : cands) {
            c.contributionStats=DerivedAtomR27.pairedContribution(c.subtree,programs,evo.context());c.contribution=c.contributionStats.mean();
            if (!(c.contribution > 0)||!(c.contributionStats.lower95()>0)) { why.append(c.id()).append(":contribution=").append(c.contribution).append(";"); continue; }
            int[] rows = {evo.context().split.selectFrom + 1, evo.context().split.selectTo - 1};
            if (!DerivedAtomR27.leakFree(c.subtree, evo.context().data.tail(3000), 1, new int[] {1500, 2900}, 27)) { why.append(c.id()).append(":LEAK;"); continue; }
            if (best == null || c.contribution * c.frequency > best.contribution * best.frequency) best = c;
        }
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis();
        if (best != null) {
            best.reason = "frequency=" + best.frequency + ",H_SELECT paired BrierGain=" + best.contribution+",oneSided95="+best.contributionStats.lower95()+",periods="+best.contributionStats.gains.n;
            b.add("INSERT OR REPLACE INTO r27_atom_score(atom_id,atom_index,dataset_hash,n_select,gain_select,lcb_select,ucb_select,n_holdout,gain_holdout,lcb_holdout,missing_rate,state,derived,definition,computed_at) VALUES(?,-1,?,0,?,NULL,NULL,0,NULL,NULL,0,'DERIVED',1,?,?)",
                    best.id(), evo.context().data.datasetHash, best.contribution, best.subtree.toString(), now);
            contribution(b,best.id(),evo.context().data.datasetHash,evo.context().split.selectFrom,evo.context().split.selectTo,best.contributionStats,now);
        }
        Map<String, Object> m = new LinkedHashMap<>(); m.put("generation", (long) evo.generation); m.put("registered", best == null ? null : DerivedAtomR27.json(best)); m.put("candidates", (long) cands.size()); m.put("rejected", why.toString());
        m.put("status", best == null ? "NO_QUALIFYING_SUBTREE" : "DERIVED_ATOM_REGISTERED");
        b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('atoms','derived',?,?)", JsonR27.write(m), now);
        RuntimeR27.submit(w, "DERIVED_ATOM", b);
        if(best!=null) synchronized(rt){List<PolicyAstR27.Node> list=new ArrayList<>(rt.derived);list.add(best.subtree);while(list.size()>32)list.remove(0);rt.derived=list;}
        return 1;
    }

    static int moleculeGeneration = -1;
    static long molecules(RuntimeR27 rt, ResearchPoolR27.Worker w, EvolutionLoopR27 evo) throws Exception {
        final int generation=evo.generation;
        if (generation == moleculeGeneration) return 0;
        Map<String, Double> lw = rt.leafWeights(); List<String> kinds = new ArrayList<>();
        for (String k : MutationOpsR27.ALL_KINDS) { Double x = lw.get(k); if (x == null || x >= 1.0) kinds.add(k); } // USEFUL/NEUTRAL sources only
        if (kinds.size() < 2) return 0;
        String[] ks = kinds.toArray(new String[0]); EvalContextR27 ctx = evo.context(); StatsR27.SplitMix r = new StatsR27.SplitMix(evo.rng.state ^ 0x6D6F6CL);
        // Mutual-information prior on H_TRAIN for source selection.
        IdeaFamilyR27.Context ic = new IdeaFamilyR27.Context(); ic.leafWeight = new java.util.HashMap<>();
        for (String k : ks) { int tp = 1; double mi = 0; try { mi = DerivedAtomR27.mutualInformation(k, k.equals("knn") ? 3 : 1, k.equals("knn") ? 20 : 32, ctx.cache, tp, ctx.split.trainFrom, Math.min(ctx.split.trainTo, ctx.split.trainFrom + 1500)); } catch (RuntimeException shapeMismatch) { } ic.leafWeight.put(k, 1 + 100 * mi); }
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis(); int survived = 0;
        List<PolicyAstR27.Node> keep = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            int tp = 1 + r.nextInt(10); PolicyAstR27.Node mol = DerivedAtomR27.molecule(ks, tp, r, ic);
            PolicySpecR27 s = new PolicySpecR27(mol, tp, PolicySpecR27.Rule.QUANTILE, 0, 0.15, 1.0, true, 0.01, 0.01, null);
            String state, reason; Double gain = null;AtomScoreR27.Contribution paired=new AtomScoreR27.Contribution();
            try {
                double tau = ctx.temperature(s); ReplayEvaluatorR27.Trace tr = ctx.selectTrace(s, tau);
                ReplayScoreR27 sc = ReplayScoreR27.of(SplitR27.SELECT, tr, ctx.split.selectFrom, ctx.split.selectTo); gain = sc.pairedUMean;
                paired=DerivedAtomR27.pairedContribution(mol,java.util.Collections.singletonList(s),ctx);
                boolean ok = paired.lower95()>0 && sc.pairedULcb > 0 && sc.brier <= ReplayScoreR27.constantBrier(tr, ctx.split.selectFrom, ctx.split.selectTo) + SuccessiveHalvingR27.BRIER_SLACK;
                state = ok ? "SURVIVED" : "RETIRED"; reason = "H_SELECT paired Brier LCB="+paired.lower95()+",legacy paired u LCB="+sc.pairedULcb+",Brier="+sc.brier;
                if (ok) { survived++; keep.add(mol); }
            } catch (RuntimeException e) { state = "REJECTED"; reason = e.getClass().getSimpleName() + ":" + e.getMessage(); }
            String moleculeHash=StatsR27.sha256(mol+"@"+tp);contribution(b,"MOLECULE:"+moleculeHash,ctx.data.datasetHash,ctx.split.selectFrom,ctx.split.selectTo,paired,now);
            b.add("INSERT OR IGNORE INTO r27_molecule(molecule_hash,definition,generation,state,select_gain,reason,created_at) VALUES(?,?,?,?,?,?,?)", StatsR27.sha256(mol + "@" + tp), mol.toString() + "@" + tp, generation, state, gain, reason, now);
        }
        RuntimeR27.submit(w, "MOLECULES", b);
        moleculeGeneration=generation;
        if (!keep.isEmpty()) synchronized (rt) { List<PolicyAstR27.Node> l = new ArrayList<>(rt.derived); l.addAll(keep); while (l.size() > 32) l.remove(0); rt.derived = l; }
        return 32;
    }

    // ------------------------------------------------------------------ AtomNet + FTRL
    static ResearchPoolR27.Outcome atomNetStep(RuntimeR27 rt, ResearchPoolR27.Worker w) throws Exception {
        return AtomNetRuntimeR30.step(rt,w);
    }

    /** Original ABI implementation retained for rollback and legacy evidence. */
    static ResearchPoolR27.Outcome legacyAtomNetStep(RuntimeR27 rt, ResearchPoolR27.Worker w) throws Exception {
        ReplayDatasetR27 d = rt.evoData; EvolutionLoopR27 evo = rt.evo;
        if (d == null || evo == null || rt.frameValues == null || !d.datasetHash.equals(rt.frameDataset) || rt.frameCursor < rt.frameValues.length)
            return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-atoms:frame " + rt.frameCursor + "/" + (rt.frameValues == null ? 0 : rt.frameValues.length) + ")", rt.frameCursor, rt.frameValues == null ? -1 : rt.frameValues.length);
        if (rt.atomNet == null) {
            try (Cursor c = w.ro.rawQuery("SELECT artifact,state_hash,dataset_hash,trained_through,feature_abi FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL'", null)) {
                if (c.moveToFirst()) {
                    if (!ModelArtifactR29.ABI.equals(c.getString(4))) throw new IllegalStateException("R29_MODEL_FEATURE_ABI_MISMATCH");
                    ModelArtifactR29 restored = ModelArtifactR29.decode(c.getBlob(0), c.getString(1));
                    if (restored.trainer.net.in != 7 + AtomReplayR27.eligible().length || restored.ftrl.d != 7) throw new IllegalStateException("R29_MODEL_INPUT_SHAPE_MISMATCH");
                    if (!restored.datasetHash.equals(c.getString(2)) || !restored.trainedThrough.equals(c.getString(3))) throw new IllegalStateException("R29_MODEL_CURSOR_MISMATCH");
                    // CP04 artifacts lack this receipt counter. Recover its lower bound from immutable database rows.
                    try (Cursor epochs = w.ro.rawQuery("SELECT coalesce(max(epoch),-1)+1 FROM r27_atomnet_epoch WHERE run_id=?", new String[] {restored.datasetHash.substring(0,16)})) {
                        if (epochs.moveToFirst()) restored.trainer.nextEpochReceipt = Math.max(restored.trainer.nextEpochReceipt, epochs.getInt(0));
                    }
                    rt.atomNet = restored.trainer; rt.atomFtrl = restored.ftrl; rt.atomTrainedThrough = restored.trainedThrough; rt.atomNetDataset = restored.datasetHash;
                    return ResearchPoolR27.Outcome.idle("CHECKPOINT_RESTORED;NO_TRAINING_YET", restored.trainer.onlineSteps, -1);
                }
            }
            pretrain(rt, w, d, evo.context().split); return ResearchPoolR27.Outcome.progress(1, 0);
        }
        // A new replay-window hash is not a new learning rule. Continue the full persisted optimizer/RNG state.
        return online(rt, w);
    }

    static List<AtomNetTrainerR27.Sample> samples(RuntimeR27 rt, int from, int to) {
        List<AtomNetTrainerR27.Sample> out = new ArrayList<>();
        for (int t = from; t < to; t++) if (rt.frameValues[t] != null) out.add(new AtomNetTrainerR27.Sample(AtomReplayR27.features(rt.frameValues[t], rt.framePresent[t], RAW), rt.frameHit[t], rt.frameActual[t] - 1, rt.frameIssue[t]));
        return out;
    }

    static void pretrain(RuntimeR27 rt, ResearchPoolR27.Worker w, ReplayDatasetR27 d, SplitR27 sp) throws Exception {
        List<AtomNetTrainerR27.Sample> train = samples(rt, sp.trainFrom, sp.trainTo), select = samples(rt, sp.selectFrom, sp.selectTo), hold = samples(rt, sp.holdoutFrom, sp.holdoutTo);
        if (train.isEmpty()) return;
        AtomNetTrainerR27 tr = new AtomNetTrainerR27(train.get(0).x.length, 27);
        long t0 = System.currentTimeMillis(); tr.pretrain(train, select); tr.check(hold);
        String run = d.datasetHash.substring(0, 16); long now = System.currentTimeMillis();
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
        for (AtomNetTrainerR27.Epoch e : tr.epochs) b.add("INSERT OR REPLACE INTO r27_atomnet_epoch(run_id,epoch,split,logloss,brier,recorded_at) VALUES(?,?,?,?,?,?)", run, e.epoch, e.split, e.logloss, e.brier, now);
        // FTRL on the 7 group features (same samples).
        double[][] gx = new double[train.size()][]; int[] gy = new int[train.size()];
        for (int i = 0; i < gx.length; i++) { gx[i] = java.util.Arrays.copyOf(train.get(i).x, 7); gy[i] = train.get(i).y; }
        FtrlR27 f = FtrlR27.pretrain(gx, gy, gx.length);
        for (AtomNetTrainerR27.Sample s : select) f.update(java.util.Arrays.copyOf(s.x, 7), s.y);
        // REPLAY Brier pairs on the holdout block (chart A initial evidence; FUTURE pairs follow live).
        for (AtomNetTrainerR27.Sample s : hold) {
            double p = tr.net.predict(s.x), q = f.predict(java.util.Arrays.copyOf(s.x, 7));
            b.add("INSERT OR REPLACE INTO r27_brier_pair(target_issue,source,model,p_model,p_base,hit,d,recorded_at) VALUES(?,?,?,?,?,?,?,?)", s.issue, "REPLAY", "ATOMNET", p, StatsR27.BASE, s.y, sq(StatsR27.BASE - s.y) - sq(p - s.y), now);
            b.add("INSERT OR REPLACE INTO r27_brier_pair(target_issue,source,model,p_model,p_base,hit,d,recorded_at) VALUES(?,?,?,?,?,?,?,?)", s.issue, "REPLAY", "FTRL", q, StatsR27.BASE, s.y, sq(StatsR27.BASE - s.y) - sq(q - s.y), now);
        }
        b.add("INSERT OR REPLACE INTO r27_atomnet_state(owner,artifact,artifact_hash,trained_through,online_steps,learning_rate,last_check_logloss,updated_at) VALUES('ATOMNET',?,?,?,?,?,?,?)",
                tr.net.encode(), StatsR27.sha256(tr.net.encode()), d.issues[sp.holdoutTo - 1], 0, tr.onlineLr, f(tr.lastCheckLogloss), now);
        Map<String, Object> m = new LinkedHashMap<>(); m.put("pretrainMs", now - t0); m.put("epochs", (long) tr.epochs.size()); m.put("params", (long) tr.net.parameterCount()); m.put("events", new ArrayList<Object>(tr.events));
        b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('atomnet','pretrain',?,?)", JsonR27.write(m), now);
        checkpoint(b, new ModelArtifactR29(tr, f, d.datasetHash, d.issues[sp.holdoutTo - 1]), now);
        RuntimeR27.submit(w, "ATOMNET_PRETRAIN", b);
        rt.atomNet = tr; rt.atomNetDataset = d.datasetHash; rt.atomFtrl = f; rt.atomTrainedThrough = d.issues[sp.holdoutTo - 1];
    }

    /** Settles pending FUTURE predictions, trains one online step per new draw, and freezes the next prediction. */
    static ResearchPoolR27.Outcome online(RuntimeR27 rt, ResearchPoolR27.Worker w) throws Exception {
        ReplayDatasetR27 recent = StoreR27.loadDataset(w.ro, AtomReplayR27.HISTORY + 64);
        if (recent.size() < AtomReplayR27.MIN_ROWS + 1) return ResearchPoolR27.Outcome.idle("WAITING_DRAWS", 0, 0);
        AtomNetTrainerR27 tr = rt.atomNet; FtrlR27 f = rt.atomFtrl; String trainedThrough = rt.atomTrainedThrough;
        boolean staged = false; long units = 0; long now = System.currentTimeMillis();
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
        for (int t = 0; t < recent.size(); t++) {
            if (recent.issues[t].compareTo(trainedThrough) <= 0 || t < AtomReplayR27.MIN_ROWS) continue;
            AtomReplayR27.Frame fr = AtomReplayR27.buildLegacy(recent, t, t + 1);
            if (fr.values[0] == null) continue;
            double[] x = AtomReplayR27.features(fr.values[0], fr.present[0], RAW); int y = fr.hit[0];
            if (!staged) {
                byte[] bytes = new ModelArtifactR29(tr, f, rt.atomNetDataset, trainedThrough).encode();
                ModelArtifactR29 copy = ModelArtifactR29.decode(bytes, StatsR27.sha256(bytes));
                tr = copy.trainer; f = copy.ftrl; staged = true;
            }
            for (String model : new String[] {"ATOMNET", "FTRL"}) {
                try (Cursor c = w.ro.rawQuery("SELECT p_model FROM r27_atomnet_pending WHERE target_issue=? AND model=? AND frozen_at<deadline_at", new String[] {recent.issues[t], model})) {
                    if (c.moveToFirst()) { double p = c.getDouble(0); b.add("INSERT OR IGNORE INTO r27_brier_pair(target_issue,source,model,p_model,p_base,hit,d,recorded_at) VALUES(?,?,?,?,?,?,?,?)", recent.issues[t], "FUTURE", model, p, StatsR27.BASE, y, sq(StatsR27.BASE - y) - sq(p - y), now); }
                }
            }
            tr.online(new AtomNetTrainerR27.Sample(x, y, fr.actual[0] - 1, recent.issues[t])); f.update(java.util.Arrays.copyOf(x, 7), y);
            trainedThrough = recent.issues[t]; units++;
            if (tr.sinceCheck >= AtomNetTrainerR27.CHECK_EVERY && rt.frameValues != null) {
                SplitR27 sp = rt.evo.context().split; boolean rolled = tr.check(samples(rt, Math.max(sp.selectFrom, sp.holdoutTo - SplitR27.BLOCK), sp.holdoutTo));
                b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('atomnet','last_check',?,?)", JsonR27.write(java.util.Collections.<String, Object>singletonMap("event", rolled ? "ROLLBACK_DEGRADED" : "CHECK_OK")), now);
                for (AtomNetTrainerR27.Epoch e : tr.epochs) b.add("INSERT OR REPLACE INTO r27_atomnet_epoch(run_id,epoch,split,logloss,brier,recorded_at) VALUES(?,?,?,?,?,?)", rt.atomNetDataset.substring(0, 16), e.epoch, e.split, e.logloss, e.brier, now);
            }
            // One newly learned issue per slice; the full replay/optimizer cursor is committed before another slice.
            break;
        }
        if (units > 0) {
            b.add("UPDATE r27_atomnet_state SET artifact=?,artifact_hash=?,trained_through=?,online_steps=?,learning_rate=?,updated_at=? WHERE owner='ATOMNET'", tr.net.encode(), StatsR27.sha256(tr.net.encode()), trainedThrough, tr.onlineSteps, tr.onlineLr, now);
            checkpoint(b, new ModelArtifactR29(tr, f, rt.atomNetDataset, trainedThrough), now);
            RuntimeR27.submit(w, "ATOMNET_ONLINE", b);
            // Failed/cancelled transactions cannot advance the in-memory learner ahead of durable state.
            rt.atomNet = tr; rt.atomFtrl = f; rt.atomTrainedThrough = trainedThrough;
        }
        // Freeze the next target's probability before its deadline (FUTURE evidence; never back-filled).
        String latest = recent.issues[recent.size() - 1]; long latestOrd = recent.ordinals[recent.size() - 1];
        String target = TargetIssueResolver.fromSequenceOrdinalSU1(latestOrd + 1); long deadline = RuntimeR27.latestOpen(w.ro) + 60_000L;
        boolean pending; try (Cursor c = w.ro.rawQuery("SELECT 1 FROM r27_atomnet_pending WHERE target_issue=?", new String[] {target})) { pending = c.moveToFirst(); }
        ResearchWriteQueueR27.Batch forecast = new ResearchWriteQueueR27.Batch();
        if (!pending && trainedThrough.compareTo(target) < 0 && System.currentTimeMillis() < deadline - LiveShadowsR27.FREEZE_MARGIN_MS) {
            ReplayDatasetR27 slot = recent.append(new ReplayDatasetR27.Row(target, latestOrd + 1, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}));
            AtomReplayR27.Frame fr = AtomReplayR27.buildLegacy(slot, slot.size() - 1, slot.size());
            if (fr.values[0] != null) {
                double[] x = AtomReplayR27.features(fr.values[0], fr.present[0], RAW);
                long frozenAt = System.currentTimeMillis();
                forecast.add("INSERT OR IGNORE INTO r27_atomnet_pending(target_issue,model,p_model,frozen_at,deadline_at) VALUES(?,?,?,?,?)", target, "ATOMNET", tr.net.predict(x), frozenAt, deadline);
                forecast.add("INSERT OR IGNORE INTO r27_atomnet_pending(target_issue,model,p_model,frozen_at,deadline_at) VALUES(?,?,?,?,?)", target, "FTRL", f.predict(java.util.Arrays.copyOf(x, 7)), frozenAt, deadline);
                byte[] state = new ModelArtifactR29(tr, f, rt.atomNetDataset, trainedThrough).encode();
                for (String model : new String[] {"ATOMNET", "FTRL"}) forecast.add("INSERT OR IGNORE INTO r29_atomnet_forecast VALUES(?,?,?,?,?,?,?)",
                        target, model, StatsR27.sha256(state), trainedThrough, ModelArtifactR29.ABI, frozenAt, deadline);
                forecast.before = tx -> System.currentTimeMillis() < deadline - LiveShadowsR27.FREEZE_MARGIN_MS ? 0 : -1;
                forecast.after = forecast.before;
            }
        }
        if (!forecast.isEmpty()) try { RuntimeR27.submit(w, "ATOMNET_FREEZE", forecast); }
        catch (IllegalStateException late) { if (!QualificationInboxR29.precondition(late)) throw late; }
        return units > 0 ? ResearchPoolR27.Outcome.progress(units, tr.onlineSteps) : ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", tr.onlineSteps, tr.onlineSteps);
    }

    static void checkpoint(ResearchWriteQueueR27.Batch batch, ModelArtifactR29 state, long now) {
        byte[] bytes = state.encode();
        batch.add("INSERT OR REPLACE INTO r29_model_checkpoint(owner,feature_abi,dataset_hash,trained_through,state_hash,artifact,updated_at) VALUES('ATOMNET_FTRL',?,?,?,?,?,?)",
                ModelArtifactR29.ABI, state.datasetHash, state.trainedThrough, StatsR27.sha256(bytes), bytes, now);
    }

    static void contribution(ResearchWriteQueueR27.Batch b,String atom,String dataset,int from,int to,AtomScoreR27.Contribution value,long now){
        b.add("INSERT OR REPLACE INTO r30_atom_contribution VALUES(?,?,?,?,?,?,?,?,?,?)",atom,dataset,from,to,value.gains.n,value.comparisons,f(value.mean()),f(value.lower95()),"H_SELECT_SAME_PERIOD_PAIRED_BRIER",now);
    }

    static double sq(double x) { return x * x; }
    static Object f(double d) { return Double.isFinite(d) ? (Object) d : null; }
}
