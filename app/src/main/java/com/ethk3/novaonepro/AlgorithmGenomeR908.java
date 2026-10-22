package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Executable genome for a genuinely stateful, post-settlement learning algorithm.
 *
 * <p>The genome is intentionally below named model families. It receives only primitive
 * observations and instructions, and must invent both an inference program and an outcome-driven
 * update program. Local position memory, global memory and cross-position peer memory are
 * separate address spaces. A structure without observation diversity, persistent memory and a
 * settled-outcome update law is a formula, not an algorithm.</p>
 */
public final class AlgorithmGenomeR908 {
    public static final int INPUTS = 14;
    public static final int REGS = 16;
    /** Compatibility name retained for the per-position state width. */
    public static final int STATE_SLOTS = 8;
    public static final int GLOBAL_STATE_SLOTS = 8;
    public static final int POSITIONS = 10;
    public static final int MAX_PROGRAM = 64;

    /** Low-level search primitives; none is a packaged Markov/forest/network/bandit model. */
    public enum Op {
        LOAD_X, LOAD_S, LOAD_G, LOAD_PEER, CONST,
        ADD, SUB, MUL, SAFE_DIV, MIN, MAX,
        ABS, NEG, TANH, SIGMOID, SOFTSIGN, CLIP01, SQ,
        GT_SELECT, MIX,
        WRITE_S, WRITE_G
    }

    /**
     * Structural repair intent selected from settled Future-OOS failure evidence.  These are not
     * model templates: each intent only biases which low-level instructions and memory topology
     * are added, while the executable child remains a newly hashed program.
     */
    public enum MutationIntent {
        CALIBRATION_SHAPE,
        LONG_MISS_MEMORY,
        REGIME_ROUTER,
        BLOCK_STABILITY,
        MAINLINE_COMPLEMENT,
        RESOURCE_SIMPLIFY,
        OPEN_EXPLORATION
    }

    public static final class Insn {
        public final Op op;
        public final int dst, a, b;
        public final double k;

        public Insn(Op op, int dst, int a, int b, double k) {
            this.op = op == null ? Op.CONST : op;
            this.dst = bound(dst, 0, REGS - 1);
            this.a = a;
            this.b = b;
            this.k = finite(k);
        }

        public String canonical() {
            return op.name() + ',' + dst + ',' + a + ',' + b + ',' + hex(k);
        }

        public static Insn parse(String source) {
            String[] x = safe(source).split(",", -1);
            if (x.length != 5) throw new IllegalArgumentException("R914_GENOME_INSN_PARTS");
            return new Insn(
                    Op.valueOf(x[0]),
                    Integer.parseInt(x[1]),
                    Integer.parseInt(x[2]),
                    Integer.parseInt(x[3]),
                    Double.longBitsToDouble(Long.parseUnsignedLong(x[4], 16)));
        }
    }

    public final String genomeId;
    public final int generation;
    public final String parentIds;
    public final List<Insn> scoreProgram;
    public final List<Insn> updateProgram;
    public final int scoreOutputReg;
    public final String structuralHash;
    public final String initialParameters;

    public AlgorithmGenomeR908(
            int generation,
            String parentIds,
            List<Insn> scoreProgram,
            List<Insn> updateProgram,
            int scoreOutputReg) {
        this(generation, parentIds, scoreProgram, updateProgram, scoreOutputReg, "");
    }

    public AlgorithmGenomeR908(int generation, String parentIds, List<Insn> scoreProgram,
            List<Insn> updateProgram, int scoreOutputReg, String initialParameters) {
        this.initialParameters = safe(initialParameters);
        if (!this.initialParameters.isEmpty()) {
            AlgorithmGenomeVmR908.State state = AlgorithmGenomeVmR908.State.decode(this.initialParameters);
            if (!state.encode().equals(this.initialParameters) || state.updates != 0)
                throw new IllegalArgumentException("R959_INITIAL_PARAMETER_CONTRACT");
        }
        this.generation = Math.max(1, generation);
        this.parentIds = safe(parentIds);
        this.scoreProgram = immutable(scoreProgram);
        this.updateProgram = immutable(updateProgram);
        this.scoreOutputReg = bound(scoreOutputReg, 0, REGS - 1);
        validate();
        // Lineage and generation never manufacture novelty: executable structure alone owns ID.
        this.structuralHash = V51ChecksumEngine.sha256(programStructureCanonical());
        // Stable prefix preserves identity of already persisted R908 genomes on cover-install.
        this.genomeId = "R908-ALG-" + structuralHash.substring(0, 24);
    }

    public String canonical() {
        return "R908_GENOME|id=" + genomeId
                + "|generation=" + generation
                + "|parents=" + parentIds
                + '|' + programStructureCanonical();
    }

    /** Canonical executable identity; deliberately excludes ID, name, generation and parents. */
    public String programStructureCanonical() {
        return "out=" + scoreOutputReg
                + "|score=" + program(scoreProgram)
                + "|update=" + program(updateProgram)
                + (initialParameters.isEmpty() ? "" : "|initial=" + initialParameters.replace('|', ','));
    }

    public static AlgorithmGenomeR908 parse(String canonical) {
        String s = safe(canonical);
        if (!s.startsWith("R908_GENOME|")) throw new IllegalArgumentException("R914_GENOME_PREFIX");
        return new AlgorithmGenomeR908(
                Integer.parseInt(field(s, "generation")),
                field(s, "parents"),
                parseProgram(field(s, "score")),
                parseProgram(field(s, "update")),
                Integer.parseInt(field(s, "out")), field(s, "initial").replace(',', '|'));
    }

    /** Hard boundary between an expression and an invented algorithm family. */
    public boolean algorithmGrade() {
        boolean scoreReadsMemory = false;
        boolean writesMemory = false;
        boolean updateReadsOutcome = false;
        boolean[] historical = new boolean[9];
        for (Insn i : scoreProgram) {
            if (isMemoryRead(i.op)) scoreReadsMemory = true;
            if (i.op == Op.LOAD_X && i.a >= 0 && i.a < historical.length) historical[i.a] = true;
        }
        for (Insn i : updateProgram) {
            if (isMemoryWrite(i.op)) writesMemory = true;
            // X9=hit; X12=actual number; X13=hit minus frozen probability.
            if (i.op == Op.LOAD_X && (i.a == 9 || i.a == 12 || i.a == 13)) {
                updateReadsOutcome = true;
            }
        }
        int distinctHistorical = 0;
        for (boolean used : historical) if (used) distinctHistorical++;
        return scoreReadsMemory
                && writesMemory
                && updateReadsOutcome
                && distinctHistorical >= 2
                && scoreProgram.size() >= 7
                && updateProgram.size() >= 7;
    }

    /** Quality-diversity cell derived from executable topology, not its label. */
    public String mechanismCell() {
        Mechanism m = mechanism();
        return "L" + bucket(m.localReads + m.localWrites)
                + "G" + bucket(m.globalReads + m.globalWrites)
                + "P" + bucket(m.peerReads)
                + "N" + bucket(m.nonlinear)
                + "B" + bucket(m.branches)
                + "I" + bucket(m.interactions)
                + "F" + bucket(m.feedback)
                + "X" + Integer.toHexString(m.inputMask);
    }

    public double complexity() {
        Mechanism m = mechanism();
        return scoreProgram.size()
                + 1.25d * updateProgram.size()
                + 0.50d * m.nonlinear
                + 0.80d * m.branches
                + 0.35d * (m.localReads + m.localWrites)
                + 0.45d * (m.globalReads + m.globalWrites)
                + 0.55d * m.peerReads;
    }

    /** Stable Chinese name generated from observation, memory and inference topology. */
    public String chineseName() {
        Mechanism m = mechanism();
        String[] prefix = {"星衡", "玄策", "曜机", "辰演", "天工", "流枢"};
        int h = structuralHash.hashCode() & 0x7fffffff;
        return prefix[h % prefix.length]
                + '·' + observationTitle(m.inputCounts)
                + '·' + memoryTitle(m)
                + inferenceTitle(m)
                + "算法·第" + two(generation) + "代";
    }

    /** Human-visible explanation is generated from the same executable mechanism as the name. */
    public String mechanismDescriptionZh() {
        Mechanism m = mechanism();
        return "观察=" + observationDescription(m.inputCounts)
                + "；记忆=" + memoryDescription(m)
                + "；推断=" + inferenceDescription(m)
                + "；结算更新=" + updateDescription(m)
                + "；机制单元=" + mechanismCell()
                + "；评分程序=" + scoreProgram.size() + "条"
                + "；更新程序=" + updateProgram.size() + "条"
                + "；结构哈希=" + structuralHash;
    }

    public static AlgorithmGenomeR908 seed(long seed, int generation, String parentIds) {
        Rng r = new Rng(seed);
        ArrayList<Insn> score = new ArrayList<Insn>();
        int x0 = r.nextInt(9), x1 = (x0 + 1 + r.nextInt(8)) % 9;
        // Three independent memory views make the initial search richer than a state-less formula.
        score.add(new Insn(Op.LOAD_X, 0, x0, 0, 0));
        score.add(new Insn(Op.LOAD_X, 1, x1, 0, 0));
        score.add(new Insn(Op.LOAD_S, 2, r.nextInt(STATE_SLOTS), 0, 0));
        score.add(new Insn(Op.LOAD_G, 3, r.nextInt(GLOBAL_STATE_SLOTS), 0, 0));
        score.add(new Insn(Op.LOAD_PEER, 4, r.nextInt(STATE_SLOTS), 0, 0));
        score.add(new Insn(Op.MUL, 5, 0, 2, 0));
        score.add(new Insn(Op.ADD, 6, 5, 1, 0));
        score.add(new Insn(Op.MIX, 7, 6, 3, 0.08d + 0.42d * r.nextDouble()));
        score.add(new Insn(Op.GT_SELECT, 8, 7, 4, r.nextDouble() - 0.5d));
        int last = 8;
        for (int i = 0, n = 3 + r.nextInt(7); i < n; i++) {
            Insn next = randomScoreInsn(r);
            score.add(next);
            last = next.dst;
        }

        ArrayList<Insn> update = new ArrayList<Insn>();
        int outcome = new int[] {9, 12, 13}[r.nextInt(3)];
        update.add(new Insn(Op.LOAD_S, 0, r.nextInt(STATE_SLOTS), 0, 0));
        update.add(new Insn(Op.LOAD_G, 1, r.nextInt(GLOBAL_STATE_SLOTS), 0, 0));
        update.add(new Insn(Op.LOAD_X, 2, outcome, 0, 0));
        update.add(new Insn(Op.LOAD_X, 3, r.nextInt(9), 0, 0));
        update.add(new Insn(Op.SUB, 4, 2, 3, 0));
        update.add(new Insn(Op.MIX, 5, 0, 4, 0.05d + 0.35d * r.nextDouble()));
        update.add(new Insn(Op.WRITE_S, 5, r.nextInt(STATE_SLOTS), 0, 0));
        update.add(new Insn(Op.MIX, 6, 1, 4, 0.02d + 0.22d * r.nextDouble()));
        update.add(new Insn(Op.WRITE_G, 6, r.nextInt(GLOBAL_STATE_SLOTS), 0, 0));
        for (int i = 0, n = 2 + r.nextInt(5); i < n; i++) update.add(randomUpdateInsn(r));
        normalize(score, update, r);
        return new AlgorithmGenomeR908(generation, parentIds, score, update, last);
    }

    /** Program-level crossover recombines inference topology and the learning law. */
    public AlgorithmGenomeR908 crossover(AlgorithmGenomeR908 other, long seed, int newGeneration) {
        if (other == null) return mutate(seed, newGeneration);
        Rng r = new Rng(seed ^ structuralHash.hashCode() ^ other.structuralHash.hashCode());
        ArrayList<Insn> score = splice(scoreProgram, other.scoreProgram, r);
        ArrayList<Insn> update = splice(updateProgram, other.updateProgram, r);
        normalize(score, update, r);
        int out = r.nextBoolean() ? scoreOutputReg : other.scoreOutputReg;
        AlgorithmGenomeR908 child = new AlgorithmGenomeR908(
                newGeneration, genomeId + "," + other.genomeId, score, update, out);
        if (child.structuralHash.equals(structuralHash)
                || child.structuralHash.equals(other.structuralHash)) {
            return child.mutate(seed ^ 0xD1B54A32D192ED03L, newGeneration);
        }
        return child;
    }

    /** Structural mutation changes opcode, wiring, memory address or program length. */
    public AlgorithmGenomeR908 mutate(long seed, int newGeneration) {
        Rng r = new Rng(seed ^ structuralHash.hashCode());
        ArrayList<Insn> score = new ArrayList<Insn>(scoreProgram);
        ArrayList<Insn> update = new ArrayList<Insn>(updateProgram);
        for (int e = 0, edits = 2 + r.nextInt(5); e < edits; e++) {
            if (r.nextBoolean()) mutateProgram(score, r, true);
            else mutateProgram(update, r, false);
        }
        normalize(score, update, r);
        int out = scoreOutputReg;
        if (r.nextInt(4) == 0) out = score.get(r.nextInt(score.size())).dst;
        AlgorithmGenomeR908 mutant = new AlgorithmGenomeR908(newGeneration, genomeId, score, update, out);
        if (mutant.structuralHash.equals(structuralHash)) {
            makeRoom(score);
            score.add(new Insn(Op.SOFTSIGN, r.nextInt(REGS), r.nextInt(REGS), 0, 0));
            mutant = new AlgorithmGenomeR908(newGeneration, genomeId, score, update, out);
        }
        return mutant;
    }

    /**
     * Failure-directed structural mutation.  It changes program topology, memory routing or the
     * outcome update law; it never merely changes a production threshold or claims the child
     * inherited its parent's evidence.
     */
    public AlgorithmGenomeR908 mutateForIntent(
            long seed, int newGeneration, MutationIntent requestedIntent) {
        MutationIntent intent = requestedIntent == null
                ? MutationIntent.OPEN_EXPLORATION : requestedIntent;
        if (intent == MutationIntent.OPEN_EXPLORATION) return mutate(seed, newGeneration);

        Rng r = new Rng(seed ^ structuralHash.hashCode() ^ intent.ordinal());
        ArrayList<Insn> score = new ArrayList<Insn>(scoreProgram);
        ArrayList<Insn> update = new ArrayList<Insn>(updateProgram);
        // One free edit prevents an intent from collapsing into a single fixed hand-written
        // topology. The intent-specific block below then guarantees a mechanism-level response.
        boolean freeScoreEdit = r.nextBoolean();
        mutateProgram(freeScoreEdit ? score : update, r, freeScoreEdit);
        int out = scoreOutputReg;

        switch (intent) {
            case CALIBRATION_SHAPE: {
                int shaped = (out + 5) % REGS;
                makeRoom(score);
                score.add(new Insn(r.nextBoolean() ? Op.SOFTSIGN : Op.SIGMOID,
                        shaped, out, 0, 0.0d));
                out = shaped;
                // Persist probability residual rather than only the binary label.
                makeRoom(update);
                update.add(new Insn(Op.LOAD_X, 12, 13, 0, 0.0d));
                makeRoom(update);
                update.add(new Insn(Op.MIX, 13, 12, r.nextInt(REGS), 0.10d + 0.20d * r.nextDouble()));
                makeRoom(update);
                update.add(new Insn(Op.WRITE_G, 13, r.nextInt(GLOBAL_STATE_SLOTS), 0, 0.0d));
                break;
            }
            case LONG_MISS_MEMORY: {
                int memory = (out + 3) % REGS;
                int routed = (out + 7) % REGS;
                makeRoom(score);
                score.add(new Insn(Op.LOAD_S, memory, r.nextInt(STATE_SLOTS), 0, 0.0d));
                makeRoom(score);
                score.add(new Insn(Op.GT_SELECT, routed, memory, out, -0.15d));
                out = routed;
                makeRoom(update);
                update.add(new Insn(Op.LOAD_X, 12, 13, 0, 0.0d));
                makeRoom(update);
                update.add(new Insn(Op.MIX, 13, memory, 12, 0.18d + 0.24d * r.nextDouble()));
                makeRoom(update);
                update.add(new Insn(Op.WRITE_S, 13, r.nextInt(STATE_SLOTS), 0, 0.0d));
                break;
            }
            case REGIME_ROUTER: {
                int regime = (out + 2) % REGS;
                int global = (out + 4) % REGS;
                int routed = (out + 6) % REGS;
                makeRoom(score);
                score.add(new Insn(Op.LOAD_X, regime, 6, 0, 0.0d));
                makeRoom(score);
                score.add(new Insn(Op.LOAD_G, global, r.nextInt(GLOBAL_STATE_SLOTS), 0, 0.0d));
                makeRoom(score);
                score.add(new Insn(Op.GT_SELECT, routed, regime, global, 0.50d));
                makeRoom(score);
                score.add(new Insn(Op.MIX, routed, out, routed, 0.22d + 0.40d * r.nextDouble()));
                out = routed;
                int slot = r.nextInt(GLOBAL_STATE_SLOTS);
                makeRoom(update);
                update.add(new Insn(Op.LOAD_X, 12, 13, 0, 0.0d));
                makeRoom(update);
                update.add(new Insn(Op.LOAD_G, 13, slot, 0, 0.0d));
                makeRoom(update);
                update.add(new Insn(Op.MIX, 14, 13, 12, 0.12d + 0.26d * r.nextDouble()));
                makeRoom(update);
                update.add(new Insn(Op.WRITE_G, 14, slot, 0, 0.0d));
                break;
            }
            case BLOCK_STABILITY: {
                int peer = (out + 3) % REGS;
                int stable = (out + 8) % REGS;
                makeRoom(score);
                score.add(new Insn(Op.LOAD_PEER, peer, r.nextInt(STATE_SLOTS), 0, 0.0d));
                makeRoom(score);
                score.add(new Insn(Op.MIX, stable, out, peer, 0.12d + 0.28d * r.nextDouble()));
                makeRoom(score);
                score.add(new Insn(Op.CLIP01, stable, stable, 0, 0.0d));
                out = stable;
                break;
            }
            case MAINLINE_COMPLEMENT: {
                int migration = (out + 1) % REGS;
                int residual = (out + 2) % REGS;
                int interaction = (out + 3) % REGS;
                makeRoom(score);
                score.add(new Insn(Op.LOAD_X, migration, 1, 0, 0.0d));
                makeRoom(score);
                score.add(new Insn(Op.LOAD_X, residual, 2, 0, 0.0d));
                makeRoom(score);
                score.add(new Insn(Op.MUL, interaction, migration, residual, 0.0d));
                makeRoom(score);
                score.add(new Insn(Op.MIX, interaction, out, interaction,
                        0.16d + 0.32d * r.nextDouble()));
                out = interaction;
                break;
            }
            case RESOURCE_SIMPLIFY: {
                // Remove only non-contract instructions; normalize restores any mandatory
                // observation/memory/outcome primitives. This creates a genuinely smaller child
                // whenever redundant instructions exist.
                for (int i = 0; i < 3 && score.size() > 9; i++) score.remove(r.nextInt(score.size()));
                for (int i = 0; i < 3 && update.size() > 9; i++) update.remove(r.nextInt(update.size()));
                break;
            }
            default:
                break;
        }

        normalize(score, update, r);
        AlgorithmGenomeR908 child = new AlgorithmGenomeR908(
                newGeneration, genomeId, score, update, out);
        if (child.structuralHash.equals(structuralHash)) {
            return mutate(seed ^ 0xA24BAED4963EE407L, newGeneration);
        }
        return child;
    }

    private static void normalize(ArrayList<Insn> score, ArrayList<Insn> update, Rng r) {
        trim(score);
        trim(update);
        if (!containsMemoryRead(score)) {
            makeRoom(score);
            score.add(0, new Insn(Op.LOAD_S, 2, r.nextInt(STATE_SLOTS), 0, 0));
        }
        boolean[] historical = new boolean[9];
        for (Insn i : score) if (i.op == Op.LOAD_X && i.a >= 0 && i.a < 9) historical[i.a] = true;
        int count = 0;
        for (boolean used : historical) if (used) count++;
        while (count < 2) {
            int input = r.nextInt(9);
            if (historical[input]) continue;
            makeRoom(score);
            score.add(0, new Insn(Op.LOAD_X, r.nextInt(REGS), input, 0, 0));
            historical[input] = true;
            count++;
        }
        if (!containsOutcomeFeedback(update)) {
            makeRoom(update);
            update.add(0, new Insn(Op.LOAD_X, 2, new int[] {9, 12, 13}[r.nextInt(3)], 0, 0));
        }
        if (!containsMemoryRead(update)) {
            makeRoom(update);
            update.add(0, new Insn(Op.LOAD_S, 0, r.nextInt(STATE_SLOTS), 0, 0));
        }
        if (!containsMemoryWrite(update)) {
            makeRoom(update);
            update.add(new Insn(Op.WRITE_S, r.nextInt(REGS), r.nextInt(STATE_SLOTS), 0, 0));
        }
        while (score.size() < 7) score.add(randomScoreInsn(r));
        while (update.size() < 7) update.add(randomUpdateInsn(r));
        trim(score);
        trim(update);
    }

    private static void mutateProgram(ArrayList<Insn> program, Rng r, boolean score) {
        if (program.isEmpty() || (program.size() < MAX_PROGRAM && r.nextInt(5) == 0)) {
            program.add(score ? randomScoreInsn(r) : randomUpdateInsn(r));
            return;
        }
        int at = r.nextInt(program.size());
        if (program.size() > 8 && r.nextInt(7) == 0) {
            program.remove(at);
            return;
        }
        Insn old = program.get(at);
        switch (r.nextInt(5)) {
            case 0:
                program.set(at, score ? randomScoreInsn(r) : randomUpdateInsn(r));
                break;
            case 1:
                program.set(at, new Insn(old.op, old.dst, addressFor(old.op, r, score), old.b, old.k));
                break;
            case 2:
                program.set(at, new Insn(old.op, old.dst, old.a, r.nextInt(REGS), old.k));
                break;
            case 3:
                program.set(at, new Insn(old.op, r.nextInt(REGS), old.a, old.b, old.k));
                break;
            default:
                program.set(at, new Insn(old.op, old.dst, old.a, old.b,
                        old.k + (r.nextDouble() - 0.5d) * 0.8d));
                break;
        }
    }

    private static Insn randomScoreInsn(Rng r) {
        Op[] ops = {
                Op.LOAD_X, Op.LOAD_S, Op.LOAD_G, Op.LOAD_PEER, Op.CONST,
                Op.ADD, Op.SUB, Op.MUL, Op.SAFE_DIV, Op.MIN, Op.MAX,
                Op.ABS, Op.NEG, Op.TANH, Op.SIGMOID, Op.SOFTSIGN, Op.CLIP01, Op.SQ,
                Op.GT_SELECT, Op.MIX
        };
        return randomInsn(ops[r.nextInt(ops.length)], r, true);
    }

    private static Insn randomUpdateInsn(Rng r) {
        Op[] ops = {
                Op.LOAD_X, Op.LOAD_S, Op.LOAD_G, Op.LOAD_PEER, Op.CONST,
                Op.ADD, Op.SUB, Op.MUL, Op.SAFE_DIV, Op.MIN, Op.MAX,
                Op.ABS, Op.NEG, Op.TANH, Op.SIGMOID, Op.SOFTSIGN, Op.CLIP01, Op.SQ,
                Op.GT_SELECT, Op.MIX, Op.WRITE_S, Op.WRITE_G
        };
        return randomInsn(ops[r.nextInt(ops.length)], r, false);
    }

    private static Insn randomInsn(Op op, Rng r, boolean score) {
        int dst = r.nextInt(REGS), a = r.nextInt(REGS), b = r.nextInt(REGS);
        double k = r.nextDouble() * 2.0d - 1.0d;
        if (op == Op.LOAD_X) a = r.nextInt(score ? 9 : INPUTS);
        else if (op == Op.LOAD_S || op == Op.LOAD_PEER || op == Op.WRITE_S) {
            a = r.nextInt(STATE_SLOTS);
        } else if (op == Op.LOAD_G || op == Op.WRITE_G) {
            a = r.nextInt(GLOBAL_STATE_SLOTS);
        }
        return new Insn(op, dst, a, b, k);
    }

    private static int addressFor(Op op, Rng r, boolean score) {
        if (op == Op.LOAD_X) return r.nextInt(score ? 9 : INPUTS);
        if (op == Op.LOAD_S || op == Op.LOAD_PEER || op == Op.WRITE_S) {
            return r.nextInt(STATE_SLOTS);
        }
        if (op == Op.LOAD_G || op == Op.WRITE_G) return r.nextInt(GLOBAL_STATE_SLOTS);
        return r.nextInt(REGS);
    }

    private static ArrayList<Insn> splice(List<Insn> a, List<Insn> b, Rng r) {
        ArrayList<Insn> out = new ArrayList<Insn>();
        int left = a.isEmpty() ? 0 : 1 + r.nextInt(a.size());
        int right = b.isEmpty() ? 0 : r.nextInt(b.size());
        for (int i = 0; i < left && i < a.size(); i++) out.add(a.get(i));
        for (int i = right; i < b.size(); i++) out.add(b.get(i));
        if (out.isEmpty()) out.add(new Insn(Op.CONST, 0, 0, 0, 0));
        trim(out);
        return out;
    }

    private void validate() {
        if (scoreProgram.isEmpty()) throw new IllegalArgumentException("R914_SCORE_EMPTY");
        if (updateProgram.isEmpty()) throw new IllegalArgumentException("R914_UPDATE_EMPTY");
        if (scoreProgram.size() > MAX_PROGRAM || updateProgram.size() > MAX_PROGRAM) {
            throw new IllegalArgumentException("R914_PROGRAM_TOO_LARGE");
        }
        if (!containsMemoryWrite(updateProgram)) {
            throw new IllegalArgumentException("R914_ALGORITHM_REQUIRES_STATE_WRITE");
        }
        for (Insn i : scoreProgram) validateInsn(i, false);
        for (Insn i : updateProgram) validateInsn(i, true);
    }

    private static void validateInsn(Insn i, boolean update) {
        if (i == null) throw new IllegalArgumentException("R914_INSN_NULL");
        if (i.op == Op.LOAD_X) {
            int max = update ? INPUTS : 9;
            if (i.a < 0 || i.a >= max) throw new IllegalArgumentException("R914_INPUT_RANGE");
        }
        if (i.op == Op.LOAD_S || i.op == Op.LOAD_PEER || i.op == Op.WRITE_S) {
            if (i.a < 0 || i.a >= STATE_SLOTS) throw new IllegalArgumentException("R914_LOCAL_STATE_RANGE");
        }
        if (i.op == Op.LOAD_G || i.op == Op.WRITE_G) {
            if (i.a < 0 || i.a >= GLOBAL_STATE_SLOTS) throw new IllegalArgumentException("R914_GLOBAL_STATE_RANGE");
        }
        if (!update && isMemoryWrite(i.op)) {
            throw new IllegalArgumentException("R914_PREDRAW_SCORE_MAY_NOT_MUTATE_STATE");
        }
    }

    private Mechanism mechanism() {
        Mechanism m = new Mechanism();
        accumulate(scoreProgram, m, true);
        accumulate(updateProgram, m, false);
        return m;
    }

    private static void accumulate(List<Insn> program, Mechanism m, boolean score) {
        for (Insn i : program) {
            if (i.op == Op.LOAD_X && i.a >= 0 && i.a < 9) {
                m.inputMask |= 1 << i.a;
                m.inputCounts[i.a]++;
            }
            if (i.op == Op.LOAD_S) m.localReads++;
            if (i.op == Op.LOAD_G) m.globalReads++;
            if (i.op == Op.LOAD_PEER) m.peerReads++;
            if (i.op == Op.WRITE_S) m.localWrites++;
            if (i.op == Op.WRITE_G) m.globalWrites++;
            if (i.op == Op.TANH || i.op == Op.SIGMOID || i.op == Op.SOFTSIGN || i.op == Op.SQ) {
                m.nonlinear++;
            }
            if (i.op == Op.GT_SELECT || i.op == Op.MIX) m.branches++;
            if (i.op == Op.MUL || i.op == Op.SAFE_DIV) m.interactions++;
            if (!score && i.op == Op.LOAD_X && (i.a == 9 || i.a == 12 || i.a == 13)) m.feedback++;
        }
    }

    private static String observationTitle(int[] counts) {
        int first = bestInput(counts, null), second = bestInput(counts, new boolean[] {
                first == 0, first == 1, first == 2, first == 3, first == 4,
                first == 5, first == 6, first == 7, first == 8
        });
        if (first < 0) return "原子事实";
        return INPUT_SHORT[first] + (second < 0 ? "" : INPUT_SHORT[second]);
    }

    private static String observationDescription(int[] counts) {
        StringBuilder b = new StringBuilder();
        boolean[] used = new boolean[counts.length];
        for (int n = 0; n < counts.length; n++) {
            int best = bestInput(counts, used);
            if (best < 0) break;
            used[best] = true;
            if (b.length() > 0) b.append('、');
            b.append(INPUT_LONG[best]);
        }
        return b.length() == 0 ? "未形成有效历史观察组合" : b.toString();
    }

    private static int bestInput(int[] counts, boolean[] excluded) {
        int best = -1;
        for (int i = 0; i < counts.length; i++) {
            if ((excluded != null && excluded[i]) || counts[i] <= 0) continue;
            if (best < 0 || counts[i] > counts[best]) best = i;
        }
        return best;
    }

    private static String memoryTitle(Mechanism m) {
        boolean local = m.localReads + m.localWrites > 0;
        boolean global = m.globalReads + m.globalWrites > 0;
        boolean peer = m.peerReads > 0;
        if (local && global && peer) return "局部全局跨位三层记忆";
        if (local && global) return "局部全局双层记忆";
        if (local && peer) return "局部跨位双层记忆";
        if (global && peer) return "全局跨位双层记忆";
        if (local) return "位置记忆";
        if (global) return "全局记忆";
        if (peer) return "跨位记忆";
        return "无状态";
    }

    private static String memoryDescription(Mechanism m) {
        return memoryTitle(m)
                + "（局部读" + m.localReads + "/写" + m.localWrites
                + "，全局读" + m.globalReads + "/写" + m.globalWrites
                + "，跨位读" + m.peerReads + "）";
    }

    private static String inferenceTitle(Mechanism m) {
        StringBuilder b = new StringBuilder();
        if (m.branches > 0) b.append("分流");
        if (m.interactions > 0) b.append("耦合");
        if (m.nonlinear > 0) b.append("非线性");
        if (b.length() == 0) b.append("自适应");
        return b.toString();
    }

    private static String inferenceDescription(Mechanism m) {
        return inferenceTitle(m)
                + "（条件/混合" + m.branches
                + "，乘除交互" + m.interactions
                + "，非线性" + m.nonlinear + "）";
    }

    private static String updateDescription(Mechanism m) {
        return "仅在精确结算后读取结果反馈" + m.feedback + "次并更新持久状态；赛前评分只读";
    }

    private static boolean containsOutcomeFeedback(List<Insn> program) {
        for (Insn i : program) {
            if (i.op == Op.LOAD_X && (i.a == 9 || i.a == 12 || i.a == 13)) return true;
        }
        return false;
    }

    private static boolean containsMemoryRead(List<Insn> program) {
        for (Insn i : program) if (isMemoryRead(i.op)) return true;
        return false;
    }

    private static boolean containsMemoryWrite(List<Insn> program) {
        for (Insn i : program) if (isMemoryWrite(i.op)) return true;
        return false;
    }

    private static boolean isMemoryRead(Op op) {
        return op == Op.LOAD_S || op == Op.LOAD_G || op == Op.LOAD_PEER;
    }

    private static boolean isMemoryWrite(Op op) {
        return op == Op.WRITE_S || op == Op.WRITE_G;
    }

    private static void trim(ArrayList<Insn> program) {
        while (program.size() > MAX_PROGRAM) program.remove(program.size() - 1);
    }

    private static void makeRoom(ArrayList<Insn> program) {
        if (program.size() >= MAX_PROGRAM) program.remove(program.size() - 1);
    }

    private static String program(List<Insn> program) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < program.size(); i++) {
            if (i > 0) b.append(';');
            b.append(program.get(i).canonical());
        }
        return b.toString();
    }

    private static List<Insn> parseProgram(String source) {
        ArrayList<Insn> out = new ArrayList<Insn>();
        if (safe(source).length() == 0) return out;
        String[] parts = source.split(";");
        for (String part : parts) out.add(Insn.parse(part));
        return out;
    }

    private static String field(String canonical, String key) {
        String prefix = key + '=';
        String[] parts = canonical.split("\\|");
        for (String part : parts) if (part.startsWith(prefix)) return part.substring(prefix.length());
        return "";
    }

    private static List<Insn> immutable(List<Insn> source) {
        ArrayList<Insn> out = new ArrayList<Insn>();
        if (source != null) out.addAll(source);
        return Collections.unmodifiableList(out);
    }

    private static int bucket(int x) {
        return x <= 0 ? 0 : (x == 1 ? 1 : (x <= 3 ? 2 : 3));
    }

    private static int bound(int x, int lo, int hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    private static double finite(double x) {
        return Double.isNaN(x) || Double.isInfinite(x) ? 0.0d : x;
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }

    private static String hex(double x) {
        return Long.toHexString(Double.doubleToLongBits(finite(x)));
    }

    private static String two(int x) {
        return x < 10 ? "0" + x : String.valueOf(x);
    }

    private static final String[] INPUT_SHORT = {
            "位转", "跨位迁移", "长短残差", "母题后继", "遗漏压力",
            "频率斜率", "状态区制", "位置坐标", "号码属性"
    };

    private static final String[] INPUT_LONG = {
            "位置转移抬升", "号码跨位迁移", "短窗长窗残差", "相似母题后继",
            "遗漏危险压力", "频率变化斜率", "市场状态区制", "位置坐标属性", "号码归一属性"
    };

    private static final class Mechanism {
        final int[] inputCounts = new int[9];
        int inputMask;
        int localReads, localWrites, globalReads, globalWrites, peerReads;
        int nonlinear, branches, interactions, feedback;
    }

    private static final class Rng {
        private long state;
        Rng(long seed) { state = seed == 0L ? 0x9E3779B97F4A7C15L : seed; }
        long nextLong() {
            state ^= state >>> 12;
            state ^= state << 25;
            state ^= state >>> 27;
            return state * 2685821657736338717L;
        }
        int nextInt(int bound) {
            return bound <= 1 ? 0 : (int) Math.floorMod(nextLong(), bound);
        }
        double nextDouble() {
            return (nextLong() >>> 11) * 0x1.0p-53;
        }
        boolean nextBoolean() {
            return (nextLong() & 1L) != 0L;
        }
    }
}
