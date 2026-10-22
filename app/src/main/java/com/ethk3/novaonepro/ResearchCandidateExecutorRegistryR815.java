package com.ethk3.novaonepro;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Registry for real, typed shadow-only research executors. Missing executors are a hard reject. */
public final class ResearchCandidateExecutorRegistryR815 {
    public interface Executor {
        String type();

        Execution execute(ResearchRepositoryR815.Candidate candidate, ResearchFeatureVectorR815 vector);
    }

    public static final class Execution {
        public final boolean success;
        public final boolean selected;
        public final int position;
        public final int n1;
        public final int n2;
        public final int n3;
        public final double probability;
        public final String artifactHash;
        public final String detail;

        public Execution(
                boolean success,
                boolean selected,
                int position,
                int n1,
                int n2,
                int n3,
                double probability,
                String artifactHash,
                String detail) {
            this.success = success;
            this.selected = selected;
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.probability = probability;
            this.artifactHash = safe(artifactHash);
            this.detail = safe(detail);
            if (success
                    && (!AppConfig.isValidPosition(position)
                            || !DecisionGateEngine.b12K3(n1, n2, n3)
                            || Double.isNaN(probability)
                            || Double.isInfinite(probability)
                            || probability < 0.0d
                            || probability > 1.0d
                            || this.artifactHash.length() != 64))
                throw new IllegalArgumentException("R815_EXECUTION_OUTPUT_INVALID");
        }
    }

    private static final Object LOCK = new Object();
    private static final HashMap<String, Executor> EXECUTORS = new HashMap<String, Executor>();

    private ResearchCandidateExecutorRegistryR815() {}

    public static void register(Executor executor) {
        if (executor == null || safe(executor.type()).length() == 0)
            throw new IllegalArgumentException("R815_EXECUTOR_INVALID");
        synchronized (LOCK) {
            String type = safe(executor.type());
            if (EXECUTORS.containsKey(type)) throw new IllegalStateException("R815_EXECUTOR_DUPLICATE:" + type);
            EXECUTORS.put(type, executor);
        }
    }

    public static void unregister(String type) {
        synchronized (LOCK) {
            EXECUTORS.remove(safe(type));
        }
    }

    public static Execution execute(ResearchRepositoryR815.Candidate candidate, ResearchFeatureVectorR815 vector) {
        if (candidate == null || vector == null) throw new IllegalArgumentException("R815_EXECUTOR_ARGUMENT_NULL");
        if (!candidate.schemaSha256.equals(vector.binding.schemaSha256)
                || !candidate.datasetSha256.equals(vector.binding.datasetSha256)
                || !candidate.createdTargetIssue.equals(vector.binding.targetIssue))
            throw new IllegalArgumentException("R815_EXECUTOR_BINDING_MISMATCH");
        Executor executor;
        synchronized (LOCK) {
            executor = EXECUTORS.get(candidate.candidateType);
        }
        if (executor == null)
            throw new IllegalStateException("R815_REJECT_NO_EXECUTOR:" + candidate.candidateType);
        Execution output = executor.execute(candidate, vector);
        if (output == null || !output.success) throw new IllegalStateException("R815_EXECUTOR_FAILED");
        return output;
    }

    public static Map<String, Executor> snapshot() {
        synchronized (LOCK) {
            return Collections.unmodifiableMap(new HashMap<String, Executor>(EXECUTORS));
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
