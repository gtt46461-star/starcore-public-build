package com.ethk3.novaonepro;

import java.util.Locale;

/**
 * Deterministic point-in-time VM for {@link GenesisAssetGenomeR915}.
 *
 * <p>Predraw evaluation cannot mutate memory. Only exact authoritative settlement may run the
 * genome's update law, and the state checkpoint rejects duplicate or out-of-order outcomes. The VM
 * materializes all 240 L0 atoms from raw PIT/profile/action truth, so a freeze is reproducible and
 * cannot be recomputed after seeing the target draw.</p>
 */
public final class GenesisAssetVmR915 {
    private static final String CONTEXT_VERSION = "R915CTX1";
    private static final String STATE_VERSION = "R915ASTATE1";

    private GenesisAssetVmR915() {}

    public static final class State {
        public final AlgorithmGenomeVmR908.State kernel;
        public String lastSettledIssue;
        public long updates;

        public State() { kernel = new AlgorithmGenomeVmR908.State(); lastSettledIssue = ""; }
        private State(AlgorithmGenomeVmR908.State kernel, String issue, long updates) {
            this.kernel = kernel == null ? new AlgorithmGenomeVmR908.State() : kernel;
            lastSettledIssue = safe(issue);
            this.updates = Math.max(0L, updates);
        }

        public String encode() {
            return STATE_VERSION + '|' + lastSettledIssue + '|' + updates + '|' + kernel.encode();
        }

        public String hash() { return V51ChecksumEngine.sha256(encode()); }

        public static State decode(String encoded) {
            String value = safe(encoded);
            if (!value.startsWith(STATE_VERSION + '|')) return new State();
            String[] head = value.split("\\|", 4);
            if (head.length != 4) return new State();
            return new State(
                    AlgorithmGenomeVmR908.State.decode(head[3]),
                    head[1],
                    parseLong(head[2]));
        }
    }

    public static final class PredrawContext {
        public final String baseIssue, targetIssue, historyHash, surfaceHash;
        public final int historyRows, position, n1, n2, n3, actionOrdinal;
        public final double[] atoms;
        public final AtomCaptureR932.Values observation;

        PredrawContext(
                String base,
                String target,
                String historyHash,
                String surfaceHash,
                int historyRows,
                int position,
                int n1,
                int n2,
                int n3,
                int actionOrdinal,
                double[] atoms) {
            this(base,target,historyHash,surfaceHash,historyRows,position,n1,n2,n3,actionOrdinal,atoms,null);
        }
        PredrawContext(String base,String target,String historyHash,String surfaceHash,int historyRows,
                int position,int n1,int n2,int n3,int actionOrdinal,double[] atoms,AtomCaptureR932.Values observation) {
            this.observation=observation;
            baseIssue = safe(base);
            targetIssue = safe(target);
            this.historyHash = safe(historyHash);
            this.surfaceHash = safe(surfaceHash);
            this.historyRows = Math.max(0, historyRows);
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.actionOrdinal = Math.max(0, actionOrdinal);
            this.atoms = sanitizeCopy(atoms, GenesisSchemaRegistryR915.atomCount());
            validate();
        }

        public String encodeFrozen() {
            StringBuilder out = new StringBuilder(CONTEXT_VERSION)
                    .append('|').append(baseIssue)
                    .append('|').append(targetIssue)
                    .append('|').append(historyHash)
                    .append('|').append(surfaceHash)
                    .append('|').append(historyRows)
                    .append('|').append(position)
                    .append('|').append(n1)
                    .append('|').append(n2)
                    .append('|').append(n3)
                    .append('|').append(actionOrdinal);
            for (double atom : atoms)
                out.append('|').append(Long.toHexString(Double.doubleToLongBits(finite(atom))));
            return out.toString();
        }

        public String digest() {
            return V51ChecksumEngine.sha256("R915_FROZEN_ATOMS|" + encodeFrozen());
        }

        public static PredrawContext decodeFrozen(String encoded) {
            String[] fields = safe(encoded).split("\\|", -1);
            int expected = 11 + GenesisSchemaRegistryR915.atomCount();
            if (fields.length != expected || !CONTEXT_VERSION.equals(fields[0]))
                throw new IllegalArgumentException("R915_FROZEN_CONTEXT_PARTS");
            double[] atoms = new double[GenesisSchemaRegistryR915.atomCount()];
            for (int i = 0; i < atoms.length; i++) atoms[i] = parseHex(fields[11 + i]);
            return new PredrawContext(
                    fields[1], fields[2], fields[3], fields[4], parseInt(fields[5]),
                    parseInt(fields[6]), parseInt(fields[7]), parseInt(fields[8]),
                    parseInt(fields[9]), parseInt(fields[10]), atoms);
        }

        private void validate() {
            if (!TargetIssueResolver.exact(baseIssue, targetIssue).valid)
                throw new IllegalArgumentException("R915_CONTEXT_ISSUE_NOT_EXACT");
            if (historyHash.length() != 64 || surfaceHash.length() != 64)
                throw new IllegalArgumentException("R915_CONTEXT_HASH");
            if (position < 1 || position > 10 || !number(n1) || !number(n2) || !number(n3)
                    || n1 == n2 || n1 == n3 || n2 == n3)
                throw new IllegalArgumentException("R915_CONTEXT_ACTION");
        }
    }

    public static final class AssetValue {
        public final double primary, confidence, risk, stability;
        public final double[] shaped;
        public final String outputType, outputShape, valueHash;

        AssetValue(
                GenesisAssetGenomeR915.OutputType type,
                double primary,
                double confidence,
                double risk,
                double stability,
                double[] shaped) {
            this.primary = clamp01(primary);
            this.confidence = clamp01(confidence);
            this.risk = clamp01(risk);
            this.stability = clamp01(stability);
            this.shaped = sanitizeCopy(shaped, shaped == null ? 0 : shaped.length);
            outputType = type.name();
            outputShape = type.shape;
            StringBuilder canonical = new StringBuilder(outputType)
                    .append('|').append(outputShape)
                    .append('|').append(this.primary)
                    .append('|').append(this.confidence)
                    .append('|').append(this.risk)
                    .append('|').append(this.stability);
            for (double value : this.shaped) canonical.append('|').append(value);
            valueHash = V51ChecksumEngine.sha256(canonical.toString());
        }

        public String encodeOutput() {
            StringBuilder out = new StringBuilder(outputType).append('|').append(outputShape);
            for (double value : shaped)
                out.append('|').append(Long.toHexString(Double.doubleToLongBits(value)));
            return out.toString();
        }
    }

    public static final class SettlementResult {
        public final boolean updated;
        public final String code, beforeHash, afterHash;
        SettlementResult(boolean updated, String code, String before, String after) {
            this.updated = updated;
            this.code = safe(code);
            beforeHash = safe(before);
            afterHash = safe(after);
        }
    }

    public static PredrawContext buildContext(
            HistoricalStrategyInventionR908.Profile profile,
            V51RuntimeResult runtime,
            V51Models.PositionAction action) {
        if (profile == null || !profile.ready || runtime == null || !runtime.success
                || runtime.context == null || action == null) return null;
        String surfaceHash = runtime.checksum != null && runtime.checksum.length() == 64
                ? runtime.checksum : V51ChecksumEngine.sha256("R915_SURFACE|" + runtime.status
                        + '|' + runtime.context.targetIssue + '|' + runtime.actions.length);
        return buildContextSurface(profile,runtime.context.baseIssue,runtime.context.targetIssue,
                runtime.context.historyHash,surfaceHash,runtime.context.historyRows,runtime.elapsedMs,action);
    }

    /** Rebuilds the same PIT atom world from the immutable canonical snapshot after a process restart. */
    public static PredrawContext buildContext(
            HistoricalStrategyInventionR908.Profile profile,
            DeferredEvidenceSnapshotR36.Snapshot snapshot,
            int historyRows,
            V51Models.PositionAction action) {
        if(profile==null||!profile.ready||snapshot==null||action==null)return null;
        long elapsed=0L;if(snapshot.spans!=null)for(RuntimeTraceR36.Span span:snapshot.spans)
            if(span!=null)elapsed=Math.max(elapsed,Math.max(0L,span.elapsedMs));
        return buildContextSurface(profile,snapshot.baseIssue,snapshot.targetIssue,snapshot.historyHash,
                snapshot.recordHash,Math.max(0,historyRows),elapsed,action);
    }

    static PredrawContext buildContextSurface(
            HistoricalStrategyInventionR908.Profile profile,String baseIssue,String targetIssue,
            String historyHash,String surfaceHash,int historyRows,long elapsedMs,
            V51Models.PositionAction action) {
        // R920: every named atom is materialized by its own PIT time/topology/statistical/risk
        // semantics.  The former generic rotation over sixteen base features is intentionally no
        // longer on the production path.
        AtomCaptureR932.Values observed = GenesisAtomicWorldR920.materializeObserved(
                profile, action, historyRows, historyHash, elapsedMs);
        return new PredrawContext(
                baseIssue,
                targetIssue,
                historyHash,
                surfaceHash,
                historyRows,
                action.position,
                action.n1,
                action.n2,
                action.n3,
                Math.max(0, action.rank - 1),
                observed.values,observed);
    }

    public static AssetValue evaluate(
            GenesisAssetGenomeR915 genome, State state, PredrawContext context) {
        return evaluateInternal(genome, state, context, true);
    }

    /** Fast scalar envelope for 1200-action scanning; the selected action is materialized later. */
    public static AssetValue evaluateCompact(
            GenesisAssetGenomeR915 genome, State state, PredrawContext context) {
        return evaluateInternal(genome, state, context, false);
    }

    private static AssetValue evaluateInternal(
            GenesisAssetGenomeR915 genome,
            State state,
            PredrawContext context,
            boolean materializeShape) {
        if (genome == null || state == null || context == null || !genome.structureGrade())
            throw new IllegalArgumentException("R915_ASSET_VM_INPUT");
        GenesisAtomDependencyR932.require(genome, context, materializeShape);
        String before = state.encode();
        double[] input = inputs(genome, context, 0, 0.0d);
        double score = AlgorithmGenomeVmR908.score(genome.kernel, state.kernel, context.position, input);
        if (!before.equals(state.encode())) throw new IllegalStateException("R915_PREDRAW_STATE_MUTATION");
        double uncertainty = clamp01(0.55d * context.atoms[154] + 0.45d * context.atoms[157]);
        double risk = clamp01(0.45d * uncertainty + 0.35d * context.atoms[155]
                + 0.20d * context.atoms[15]);
        double stability = clamp01(1.0d - risk * 0.65d + context.atoms[173] * 0.20d);
        double primary = semanticTransform(genome.outputType, score, risk, stability, context);
        double[] shaped = materializeShape && genome.outputType.shaped
                ? shaped(genome, primary, context) : new double[0];
        return new AssetValue(genome.outputType, primary, 1.0d - uncertainty, risk, stability, shaped);
    }

    public static double evaluatePrimary(
            GenesisAssetGenomeR915 genome, State state, PredrawContext context) {
        return evaluateCompact(genome, state, context).primary;
    }

    /** Predraw-only regime bucket retained for stratified Future-OOS stability evidence. */
    public static int regimeBucket(PredrawContext context) {
        if (context == null || context.atoms.length <= 132) return -1;
        double value = clamp01(context.atoms[132]);
        if (value < 1.0d / 3.0d) return 0;
        if (value < 2.0d / 3.0d) return 1;
        return 2;
    }

    public static SettlementResult settle(
            GenesisAssetGenomeR915 genome,
            State state,
            PredrawContext frozen,
            String actualIssue,
            int actualNumber,
            int hit,
            double frozenScore) {
        if (genome == null || state == null || frozen == null || !genome.structureGrade())
            return new SettlementResult(false, "INVALID_INPUT", "", "");
        if (!frozen.targetIssue.equals(safe(actualIssue)) || !number(actualNumber)
                || (hit != 0 && hit != 1))
            return new SettlementResult(false, "TARGET_OR_LABEL_INVALID", state.hash(), state.hash());
        if (state.lastSettledIssue.length() > 0
                && TargetIssueResolver.compare(actualIssue, state.lastSettledIssue) <= 0)
            return new SettlementResult(false, "EXACTLY_ONCE_OR_ORDER_REJECT", state.hash(), state.hash());
        String before = state.hash();
        double[] input = inputs(genome, frozen, hit, frozenScore);
        input[12] = (actualNumber - 1) / 9.0d;
        input[13] = hit - clamp01(frozenScore);
        AlgorithmGenomeVmR908.update(genome.kernel, state.kernel, frozen.position, input);
        state.lastSettledIssue = actualIssue;
        state.updates++;
        String after = state.hash();
        if (before.equals(after))
            throw new IllegalStateException("R915_SETTLEMENT_DID_NOT_UPDATE_STATE");
        return new SettlementResult(true, "UPDATED", before, after);
    }

    public static String behaviorHash(GenesisAssetGenomeR915 genome) {
        if (genome == null) return "";
        State state = new State();
        StringBuilder trace = new StringBuilder(genome.outputType.name());
        for (int z = 0; z < 40; z++) {
            double[] atoms = new double[240];
            for (int i = 0; i < atoms.length; i++) atoms[i] = ((z * 37 + i * 19) % 997) / 996.0d;
            String base = String.format(Locale.US, "202608%02d%04d", 1 + z / 20, 100 + z);
            String target = TargetIssueResolver.next(base).targetIssue;
            PredrawContext context = new PredrawContext(
                    base, target, V51ChecksumEngine.sha256("H" + z),
                    V51ChecksumEngine.sha256("S" + z), 320 + z, 1 + z % 10,
                    1 + z % 8, 2 + z % 8, 3 + z % 8, z, atoms);
            AssetValue before = evaluate(genome, state, context);
            SettlementResult update = settle(
                    genome, state, context, target, 1 + (z * 7) % 10,
                    z % 3 == 0 ? 1 : 0, before.primary);
            trace.append(String.format(Locale.US, "%.6f/%.6f/%.6f/%s;",
                    before.primary, before.risk, before.stability, update.afterHash));
        }
        return V51ChecksumEngine.sha256("R915_ASSET_BEHAVIOR_PROBE|" + trace + '|' + state.encode());
    }

    private static double[] inputs(
            GenesisAssetGenomeR915 genome, PredrawContext context, int hit, double frozenScore) {
        double[] out = new double[AlgorithmGenomeR908.INPUTS];
        for (int i = 0; i < 9; i++) {
            double sum = 0.0d, weight = 0.0d;
            for (int j = i; j < genome.atomIndices.length; j += 9) {
                double w = 1.0d + (j % 3) * 0.25d;
                sum += context.atoms[genome.atomIndices[j]] * w;
                weight += w;
            }
            out[i] = weight <= 0.0d ? 0.0d : clamp01(sum / weight);
        }
        out[9] = hit;
        // This program is part of the genome hash, shared by predraw scoring and settlement learning.
        if(genome.molecule!=null)out[0]=clamp01(0.5d+0.5d*Math.tanh(genome.molecule.evaluate(context)));
        out[10] = clamp01(context.atoms[172]);
        out[11] = clamp01(frozenScore);
        out[12] = 0.0d;
        out[13] = hit - clamp01(frozenScore);
        return out;
    }

    private static double semanticTransform(
            GenesisAssetGenomeR915.OutputType type,
            double score,
            double risk,
            double stability,
            PredrawContext context) {
        switch (type) {
            case RISK_SURFACE: return clamp01(0.55d * score + 0.45d * risk);
            case STABILITY_SURFACE: return clamp01(0.55d * score + 0.45d * stability);
            case UNCERTAINTY_SURFACE: return clamp01(0.65d * score + 0.35d * (1.0d - stability));
            case OPPORTUNITY_SURFACE: return clamp01(score * stability * (1.0d - risk));
            case RESIDUAL_FIELD: return clamp01(0.5d + (score - context.atoms[GenesisAtomDependencyR932.residualSemantic()]) * 0.5d);
            case RELATION_FIELD: return clamp01(0.6d * score + 0.4d * context.atoms[GenesisAtomDependencyR932.relationSemantic()]);
            case TRANSITION_FIELD: return clamp01(0.6d * score + 0.4d * context.atoms[GenesisAtomDependencyR932.transitionSemantic()]);
            case MOTIF_FIELD: return clamp01(0.6d * score + 0.4d * context.atoms[GenesisAtomDependencyR932.motifSemantic()]);
            case REGIME_FIELD: return clamp01(0.65d * score + 0.35d * context.atoms[GenesisAtomDependencyR932.regimeSemantic()]);
            case CALIBRATION_RESIDUAL: return clamp01(0.5d + 0.5d * (score - context.atoms[GenesisAtomDependencyR932.residualSemantic()]));
            case EVIDENCE_DENSITY: return clamp01(score * (1.0d - risk));
            default: return clamp01(score);
        }
    }

    private static double[] shaped(
            GenesisAssetGenomeR915 genome,
            double primary,
            PredrawContext context) {
        GenesisAssetGenomeR915.OutputType type = genome.outputType;
        double[] out = new double[Math.max(0, type.flatSize)];
        for (int i = 0; i < out.length; i++) {
            int number = 1 + i % 10;
            double distance = Math.abs(number - context.n1) + Math.abs(number - context.n2)
                    + Math.abs(number - context.n3);
            double semantic;
            switch (type) {
                case MATRIX_FIELD: {
                    double atom = context.atoms[GenesisAtomDependencyR932.shapeSignal(genome, i)];
                    double topology = context.atoms[GenesisAtomDependencyR932.shapeTopology(i)];
                    semantic = 0.45d * atom + 0.35d * topology + 0.20d * primary;
                    break;
                }
                case GRAPH_FIELD: {
                    double atom = context.atoms[GenesisAtomDependencyR932.shapeSignal(genome, i)];
                    double topology = context.atoms[GenesisAtomDependencyR932.shapeTopology(i)];
                    double time = context.atoms[GenesisAtomDependencyR932.shapeTime(i)];
                    semantic = 0.40d * atom + 0.40d * Math.abs(topology - time)
                            + 0.20d * primary;
                    break;
                }
                case TENSOR_FIELD: {
                    double atom = context.atoms[GenesisAtomDependencyR932.shapeSignal(genome, i)];
                    double topology = context.atoms[GenesisAtomDependencyR932.shapeTopology(i)];
                    double time = context.atoms[GenesisAtomDependencyR932.shapeTime(i)];
                    semantic = 0.30d * atom + 0.30d * topology + 0.25d * time
                            + 0.15d * primary;
                    break;
                }
                case SEQUENCE_FIELD: {
                    double atom = context.atoms[GenesisAtomDependencyR932.shapeSignal(genome, i)];
                    double time = context.atoms[GenesisAtomDependencyR932.shapeTime(i)];
                    semantic = 0.45d * time + 0.30d * atom + 0.25d * primary;
                    break;
                }
                case STATE_MACHINE_FIELD:
                    semantic = 0.50d * context.atoms[GenesisAtomDependencyR932.shapeMemory(i)]
                            + 0.50d * primary;
                    break;
                case PROTOTYPE_LIBRARY: {
                    double atom = context.atoms[GenesisAtomDependencyR932.shapeSignal(genome, i)];
                    double time = context.atoms[GenesisAtomDependencyR932.shapeTime(i)];
                    semantic = 0.35d * atom + 0.35d * time + 0.30d * primary;
                    break;
                }
                default:
                    semantic = primary * (0.70d + 0.30d * (1.0d - distance / 27.0d));
                    break;
            }
            out[i] = clamp01(semantic);
        }
        return out;
    }

    private static boolean number(int value) { return value >= 1 && value <= 10; }
    private static int parseInt(String value) { try { return Integer.parseInt(value); } catch (Throwable e) { return 0; } }
    private static long parseLong(String value) { try { return Long.parseLong(value); } catch (Throwable e) { return 0L; } }
    private static double parseHex(String value) {
        try { return finite(Double.longBitsToDouble(Long.parseUnsignedLong(value, 16))); }
        catch (Throwable e) { return 0.0d; }
    }
    private static double[] sanitizeCopy(double[] source, int length) {
        double[] out = new double[Math.max(0, length)];
        if (source != null) for (int i = 0; i < out.length && i < source.length; i++) out[i] = clamp01(source[i]);
        return out;
    }
    private static double finite(double value) { return Double.isNaN(value) || Double.isInfinite(value) ? 0.0d : value; }
    private static double clamp01(double value) { return Math.max(0.0d, Math.min(1.0d, finite(value))); }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
