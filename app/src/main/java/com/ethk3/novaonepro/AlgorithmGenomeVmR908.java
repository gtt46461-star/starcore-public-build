package com.ethk3.novaonepro;

import java.util.List;
import java.util.Locale;

/** Deterministic VM for invented algorithms. Score is read-only; update is settlement-only. */
public final class AlgorithmGenomeVmR908 {
    private static final String STATE_V2 = "R914STATE2";
    private static final String STATE_V1 = "R908STATE1";

    private AlgorithmGenomeVmR908() {}

    public static final class State {
        public final double[] global = new double[AlgorithmGenomeR908.GLOBAL_STATE_SLOTS];
        public final double[][] position =
                new double[AlgorithmGenomeR908.POSITIONS][AlgorithmGenomeR908.STATE_SLOTS];
        public long updates;

        public String encode() {
            StringBuilder b = new StringBuilder(STATE_V2).append('|').append(updates);
            append(b, global);
            for (double[] local : position) append(b, local);
            return b.toString();
        }

        /** Reads R914 three-level state and the earlier R908 local-only state without data loss. */
        public static State decode(String encoded) {
            State out = new State();
            String[] parts = safe(encoded).split("\\|");
            if (parts.length < 2) return out;
            if (STATE_V2.equals(parts[0])) {
                int expected = 2 + AlgorithmGenomeR908.GLOBAL_STATE_SLOTS
                        + AlgorithmGenomeR908.POSITIONS * AlgorithmGenomeR908.STATE_SLOTS;
                if (parts.length != expected) return out;
                out.updates = parseLong(parts[1]);
                int k = 2;
                for (int i = 0; i < out.global.length; i++, k++) out.global[i] = parseState(parts[k]);
                for (int p = 0; p < out.position.length; p++) {
                    for (int s = 0; s < out.position[p].length; s++, k++) {
                        out.position[p][s] = parseState(parts[k]);
                    }
                }
                return out;
            }
            int oldExpected = 2 + AlgorithmGenomeR908.POSITIONS * AlgorithmGenomeR908.STATE_SLOTS;
            if (!STATE_V1.equals(parts[0]) || parts.length != oldExpected) return out;
            out.updates = parseLong(parts[1]);
            int k = 2;
            for (int p = 0; p < out.position.length; p++) {
                for (int s = 0; s < out.position[p].length; s++, k++) {
                    out.position[p][s] = parseState(parts[k]);
                }
            }
            return out;
        }

        /** Strict migration boundary. Legacy decode() deliberately remains backward compatible.
         * Unknown formats, truncation, non-finite/clamped values and bad counters must not become
         * a silently successful all-zero model. V1 local memory converts explicitly to V2.
         */
        public static State decodeStrict(String encoded) {
            if (encoded == null || !encoded.equals(encoded.trim()))
                throw new IllegalArgumentException("R970_STATE_EMPTY_OR_WHITESPACE");
            String[] parts = encoded.split("\\|", -1);
            boolean v2 = parts.length > 0 && STATE_V2.equals(parts[0]);
            boolean v1 = parts.length > 0 && STATE_V1.equals(parts[0]);
            int expected = 2 + AlgorithmGenomeR908.POSITIONS * AlgorithmGenomeR908.STATE_SLOTS
                    + (v2 ? AlgorithmGenomeR908.GLOBAL_STATE_SLOTS : 0);
            if ((!v1 && !v2) || parts.length != expected)
                throw new IllegalArgumentException("R970_STATE_SCHEMA_OR_LENGTH");
            if (!parts[1].matches("0|[1-9][0-9]{0,18}"))
                throw new IllegalArgumentException("R970_STATE_COUNTER_FORMAT");
            final long updates;
            try { updates = Long.parseLong(parts[1]); }
            catch (NumberFormatException bad) { throw new IllegalArgumentException("R970_STATE_COUNTER_OVERFLOW", bad); }
            for (int k = 2; k < parts.length; k++) {
                if (!parts[k].matches("[0-9a-f]{1,16}"))
                    throw new IllegalArgumentException("R970_STATE_HEX_FORMAT");
                double value = Double.longBitsToDouble(Long.parseUnsignedLong(parts[k], 16));
                if (Double.isNaN(value) || Double.isInfinite(value) || value < -8d || value > 8d)
                    throw new IllegalArgumentException("R970_STATE_NONFINITE_OR_OUT_OF_RANGE");
                if (!Long.toHexString(Double.doubleToLongBits(value)).equals(parts[k]))
                    throw new IllegalArgumentException("R970_STATE_NONCANONICAL");
            }
            State state = decode(encoded);
            if (state.updates != updates || (v2 && !state.encode().equals(encoded)))
                throw new IllegalArgumentException("R970_STATE_LOSSY_DECODE");
            return state;
        }

        private static void append(StringBuilder b, double[] values) {
            for (double value : values) {
                b.append('|').append(Long.toHexString(Double.doubleToLongBits(finite(value))));
            }
        }
    }

    public static State initialState(AlgorithmGenomeR908 genome) {
        return State.decode(genome == null ? "" : genome.initialParameters);
    }

    public static double score(AlgorithmGenomeR908 genome, State state, int position, double[] inputs) {
        if (genome == null || state == null || position < 1 || position > AlgorithmGenomeR908.POSITIONS) {
            return 0.0d;
        }
        double[] registers = new double[AlgorithmGenomeR908.REGS];
        double[] local = state.position[position - 1];
        double[] peer = peerMean(state, position - 1);
        run(genome.scoreProgram, registers, local, state.global, peer, inputs, false);
        return clamp01(0.5d + 0.5d * Math.tanh(registers[genome.scoreOutputReg]));
    }

    /** One immutable state snapshot per full action scan. Reuses scratch registers and the
     * ten peer means instead of allocating/reducing them for every one of 1200 actions.
     * Owned by the caller's scan; never shared between threads or across learner updates. */
    public static final class Scorer {
        private final AlgorithmGenomeR908 genome;
        private final State snapshot = new State();
        private final double[][] peers = new double[AlgorithmGenomeR908.POSITIONS][];
        private final double[] registers = new double[AlgorithmGenomeR908.REGS];
        public Scorer(AlgorithmGenomeR908 g, State state) {
            if (g == null || state == null) throw new IllegalArgumentException("R963_SCORER_INPUT");
            genome = g;
            System.arraycopy(state.global,0,snapshot.global,0,state.global.length);
            for(int p=0;p<state.position.length;p++)
                System.arraycopy(state.position[p],0,snapshot.position[p],0,state.position[p].length);
            for(int p=0;p<peers.length;p++) peers[p]=peerMean(snapshot,p);
        }
        public double score(int position,double[] inputs) {
            if(position<1||position>AlgorithmGenomeR908.POSITIONS)return 0.0d;
            java.util.Arrays.fill(registers,0.0d);
            run(genome.scoreProgram,registers,snapshot.position[position-1],snapshot.global,
                    peers[position-1],inputs,false);
            return clamp01(0.5d+0.5d*Math.tanh(registers[genome.scoreOutputReg]));
        }
    }

    /** Called only after exact settlement using the immutable feature vector frozen before draw. */
    public static void update(
            AlgorithmGenomeR908 genome, State state, int position, double[] frozenInputs) {
        if (genome == null || state == null || position < 1 || position > AlgorithmGenomeR908.POSITIONS) {
            return;
        }
        double[] registers = new double[AlgorithmGenomeR908.REGS];
        double[] local = state.position[position - 1];
        // Peer view is snapshotted before the update so one program cannot observe partial writes.
        double[] peer = peerMean(state, position - 1);
        run(genome.updateProgram, registers, local, state.global, peer, frozenInputs, true);
        sanitize(state.global);
        for (double[] one : state.position) sanitize(one);
        state.updates++;
    }

    private static void run(
            List<AlgorithmGenomeR908.Insn> program,
            double[] registers,
            double[] local,
            double[] global,
            double[] peer,
            double[] inputs,
            boolean allowStateWrite) {
        for (AlgorithmGenomeR908.Insn in : program) {
            double a = reg(registers, in.a), b = reg(registers, in.b), value;
            switch (in.op) {
                case LOAD_X: value = read(inputs, in.a); break;
                case LOAD_S: value = read(local, in.a); break;
                case LOAD_G: value = read(global, in.a); break;
                case LOAD_PEER: value = read(peer, in.a); break;
                case CONST: value = in.k; break;
                case ADD: value = a + b; break;
                case SUB: value = a - b; break;
                case MUL: value = a * b; break;
                case SAFE_DIV: value = Math.abs(b) < 1e-6d ? a : a / b; break;
                case MIN: value = Math.min(a, b); break;
                case MAX: value = Math.max(a, b); break;
                case ABS: value = Math.abs(a); break;
                case NEG: value = -a; break;
                case TANH: value = Math.tanh(clamp(a, -12.0d, 12.0d)); break;
                case SIGMOID:
                    value = 1.0d / (1.0d + Math.exp(-clamp(a, -20.0d, 20.0d)));
                    break;
                case SOFTSIGN: value = a / (1.0d + Math.abs(a)); break;
                case CLIP01: value = clamp01(a); break;
                case SQ: value = clamp(a * a, -16.0d, 16.0d); break;
                case GT_SELECT: value = a > in.k ? a : b; break;
                case MIX:
                    double alpha = clamp01(Math.abs(in.k));
                    value = (1.0d - alpha) * a + alpha * b;
                    break;
                case WRITE_S:
                    value = reg(registers, in.dst);
                    if (allowStateWrite && in.a >= 0 && in.a < local.length) {
                        local[in.a] = clamp(value, -8.0d, 8.0d);
                    }
                    break;
                case WRITE_G:
                    value = reg(registers, in.dst);
                    if (allowStateWrite && in.a >= 0 && in.a < global.length) {
                        global[in.a] = clamp(value, -8.0d, 8.0d);
                    }
                    break;
                default: value = 0.0d; break;
            }
            registers[in.dst] = clamp(finite(value), -16.0d, 16.0d);
        }
    }

    public static String explainInputs() {
        return "X0=位置转移;X1=跨位迁移;X2=短长残差;X3=母题后继;X4=遗漏压力;"
                + "X5=频率斜率;X6=状态区制;X7=位置属性;X8=号码属性;X9=命中;"
                + "X10=选择边际;X11=冻结概率;X12=实际号码;X13=命中概率残差;"
                + "S=位置记忆;G=全局记忆;PEER=其他九位置记忆均值";
    }

    /** Hashes both inference and learning dynamics to block behaviorally equivalent fake novelty. */
    public static String behaviorHash(AlgorithmGenomeR908 genome) {
        State state = initialState(genome);
        StringBuilder trace = new StringBuilder();
        for (int position = 1; position <= AlgorithmGenomeR908.POSITIONS; position++) {
            for (int z = 0; z < 24; z++) {
                double[] x = new double[AlgorithmGenomeR908.INPUTS];
                for (int i = 0; i < 9; i++) {
                    x[i] = ((position * 17 + z * 31 + i * 13) % 101) / 100.0d;
                }
                double before = score(genome, state, position, x);
                x[9] = ((position + z) % 3 == 0) ? 1.0d : 0.0d;
                x[10] = ((position * 7 + z * 11) % 41) / 100.0d;
                x[11] = before;
                x[12] = ((position * 3 + z * 5) % 10) / 9.0d;
                x[13] = x[9] - before;
                update(genome, state, position, x);
                double after = score(genome, state, position, x);
                trace.append(String.format(
                        Locale.US,
                        "%.6f/%.6f/L%.6f/G%.6f;",
                        before,
                        after,
                        state.position[position - 1][(position + z) % AlgorithmGenomeR908.STATE_SLOTS],
                        state.global[(position * 3 + z) % AlgorithmGenomeR908.GLOBAL_STATE_SLOTS]));
            }
        }
        // Do not include structural canonical text here. Structurally different programs that
        // produce the same inference/update trajectory must collapse to one behavioral identity.
        return V51ChecksumEngine.sha256("R914_BEHAVIOR_V3|" + trace + '|' + state.encode());
    }

    private static double[] peerMean(State state, int excludedPosition) {
        double[] out = new double[AlgorithmGenomeR908.STATE_SLOTS];
        int n = 0;
        for (int p = 0; p < state.position.length; p++) {
            if (p == excludedPosition) continue;
            n++;
            for (int s = 0; s < out.length; s++) out[s] += finite(state.position[p][s]);
        }
        if (n > 0) for (int s = 0; s < out.length; s++) out[s] /= n;
        return out;
    }

    private static void sanitize(double[] values) {
        for (int i = 0; i < values.length; i++) values[i] = clamp(values[i], -8.0d, 8.0d);
    }

    private static long parseLong(String x) {
        try { return Math.max(0L, Long.parseLong(x)); }
        catch (Throwable ignore) { return 0L; }
    }

    private static double parseState(String x) {
        try {
            return clamp(Double.longBitsToDouble(Long.parseUnsignedLong(x, 16)), -8.0d, 8.0d);
        } catch (Throwable ignore) {
            return 0.0d;
        }
    }

    private static double reg(double[] values, int index) {
        return index >= 0 && index < values.length ? finite(values[index]) : 0.0d;
    }

    private static double read(double[] values, int index) {
        return values != null && index >= 0 && index < values.length ? finite(values[index]) : 0.0d;
    }

    private static double finite(double x) {
        return Double.isNaN(x) || Double.isInfinite(x) ? 0.0d : x;
    }

    private static double clamp01(double x) {
        return clamp(x, 0.0d, 1.0d);
    }

    private static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, finite(x)));
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }
}
