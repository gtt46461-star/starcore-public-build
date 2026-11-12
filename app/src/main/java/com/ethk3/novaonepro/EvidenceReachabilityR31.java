package com.ethk3.novaonepro;

import java.util.*;

/**
 * R31-B05-C05: reference closure of canonical evidence artifacts from their business roots.
 *
 * <p>Roots are the current and rollback models, pending (unsettled) freezes, unfinished tasks, open validation
 * segments, shares and migrations. Marking is bounded: an incomplete mark never yields a deletion candidate. Anything
 * reachable from a root is kept regardless of age; an unrooted cycle is unreachable and may be claimed. When rooted
 * bytes alone exceed the protected budget the result is BLOCKED_PROTECTED_BYTES with each root's release condition,
 * never a deletion of rooted evidence.</p>
 */
public final class EvidenceReachabilityR31 {
    private EvidenceReachabilityR31() {}
    public enum RootKind { CURRENT_MODEL, ROLLBACK_MODEL, PENDING_FREEZE, OPEN_TASK, OPEN_VALIDATION, SHARE, MIGRATION, PERSISTED_REFERENCE }
    public static final Map<RootKind, String> RELEASE_CONDITION;
    static {
        Map<RootKind, String> m = new EnumMap<>(RootKind.class);
        m.put(RootKind.CURRENT_MODEL, "A_NEWER_MODEL_IS_PUBLISHED_AND_CONSUMED");
        m.put(RootKind.ROLLBACK_MODEL, "THE_ROLLBACK_WINDOW_CLOSES");
        m.put(RootKind.PENDING_FREEZE, "THE_FROZEN_DECISION_IS_SETTLED");
        m.put(RootKind.OPEN_TASK, "THE_TASK_REACHES_A_TERMINAL_STATE_OR_ITS_FENCE_IS_CANCELLED");
        m.put(RootKind.OPEN_VALIDATION, "THE_VALIDATION_SEGMENT_IS_COMPLETED");
        m.put(RootKind.SHARE, "THE_SHARE_IS_REVOKED_OR_EXPIRES");
        m.put(RootKind.MIGRATION, "THE_MIGRATION_COMMITS_ITS_COVERAGE_RECEIPT");
        m.put(RootKind.PERSISTED_REFERENCE,"THE_ORIGINAL_OWNER_RETIRES_ITS_REFERENCE_UNDER_THE_EXISTING_ROLLBACK_AND_EVIDENCE_POLICY");
        RELEASE_CONDITION = Collections.unmodifiableMap(m);
    }
    public interface Graph {
        Map<String, RootKind> roots();
        Set<String> edgesFrom(String artifact);
        Set<String> artifacts();
        long bytes(String artifact);
    }
    public static final class MarkIncomplete extends IllegalStateException { MarkIncomplete(int n) { super("R31_GC_MARK_BOUND_EXCEEDED:" + n); } }

    /** All artifacts reachable from a root. Throws when the bound is exceeded (no partial mark is usable). */
    public static Set<String> mark(Graph g, int maxNodes) {
        Set<String> seen = new HashSet<>(); Deque<String> work = new ArrayDeque<>(g.roots().keySet());
        while (!work.isEmpty()) {
            String a = work.pop(); if (!seen.add(a)) continue;
            if (seen.size() > maxNodes) throw new MarkIncomplete(seen.size());
            for (String b : g.edgesFrom(a)) if (!seen.contains(b)) work.push(b);
        }
        return seen;
    }
    public static List<String> unreachable(Graph g, Set<String> marked) {
        List<String> out = new ArrayList<>(); for (String a : new TreeSet<>(g.artifacts())) if (!marked.contains(a)) out.add(a); return out;
    }
    /** Protected-bytes check: rooted evidence over budget blocks with release conditions instead of deleting. */
    public static Map<String, Object> protectedBudget(Graph g, Set<String> marked, long protectedLimitBytes) {
        long rooted = 0; for (String a : marked) rooted += g.bytes(a);
        Map<String, Object> m = new LinkedHashMap<>(); m.put("rootedBytes", rooted); m.put("protectedLimitBytes", protectedLimitBytes);
        if (rooted <= protectedLimitBytes) { m.put("status", "WITHIN_PROTECTED_BUDGET"); return m; }
        m.put("status", "BLOCKED_PROTECTED_BYTES");
        List<Object> release = new ArrayList<>();
        for (Map.Entry<String, RootKind> r : new TreeMap<>(g.roots()).entrySet()) { Map<String, Object> x = new LinkedHashMap<>(); x.put("root", r.getKey()); x.put("kind", r.getValue().name()); x.put("releaseCondition", RELEASE_CONDITION.get(r.getValue())); release.add(x); }
        m.put("releaseConditions", release); m.put("deletedRootedBytes", 0L);
        return m;
    }
}
