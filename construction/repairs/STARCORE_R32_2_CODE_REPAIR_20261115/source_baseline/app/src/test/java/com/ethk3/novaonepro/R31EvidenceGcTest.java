package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B05-C05#A1-#A3: rooted closure kept, rootless cycle collected, pin/claim race, crash recovery, protected bytes. */
public final class R31EvidenceGcTest {
    static final class Mem implements StorageGcR31.Store, StorageGcR31.Files {
        final Map<String, EvidenceReachabilityR31.RootKind> roots = new HashMap<>(); final Map<String, Set<String>> edges = new HashMap<>();
        final Map<String, StorageGcR31.State> state = new HashMap<>(); final Map<String, Long> version = new HashMap<>(), bytes = new HashMap<>();
        final Set<String> files = new HashSet<>(); final List<String> receipts = new ArrayList<>(); boolean failUnlink;
        void add(String a, long b) { state.put(a, StorageGcR31.State.LIVE); version.put(a, 0L); bytes.put(a, b); files.add(a); edges.put(a, new HashSet<>()); }
        void edge(String a, String b) { edges.get(a).add(b); }
        public Map<String, EvidenceReachabilityR31.RootKind> roots() { return roots; }
        public Set<String> edgesFrom(String a) { return edges.getOrDefault(a, Collections.<String>emptySet()); }
        public Set<String> artifacts() { return state.keySet(); }
        public long bytes(String a) { return bytes.get(a); }
        public StorageGcR31.State state(String a) { return state.get(a); }
        public long version(String a) { return version.get(a); }
        public boolean compareAndSet(String a, StorageGcR31.State expected, long v, StorageGcR31.State next) { if (state.get(a) != expected || version.get(a) != v) return false; state.put(a, next); version.put(a, v + 1); return true; }
        public boolean addEdgeIfLive(String from, String to) { if (state.get(to) != StorageGcR31.State.LIVE) return false; edges.get(from).add(to); version.put(to, version.get(to) + 1); return true; }
        public void detach(String a) { edges.get(a).clear(); }
        public void receipt(String a, String phase) { receipts.add(a + ":" + phase); }
        public boolean unlink(String a) { if (failUnlink) return false; return files.remove(a); }
        public boolean exists(String a) { return files.contains(a); }
    }
    static Mem fixture() {
        Mem m = new Mem(); for (String a : new String[]{"model", "checkpoint", "frozen", "cycleA", "cycleB", "orphan"}) m.add(a, 100);
        m.roots.put("model", EvidenceReachabilityR31.RootKind.CURRENT_MODEL); m.edge("model", "checkpoint"); m.edge("checkpoint", "frozen");
        m.edge("cycleA", "cycleB"); m.edge("cycleB", "cycleA");
        return m;
    }

    @Test public void rootedClosureIsKeptRootlessCycleCollectedAndPinOrClaimWinsAlone() {
        Mem m = fixture(); Set<String> marked = EvidenceReachabilityR31.mark(m, 100);
        assertEquals(new HashSet<>(Arrays.asList("model", "checkpoint", "frozen")), marked);
        assertEquals(Arrays.asList("cycleA", "cycleB", "orphan"), EvidenceReachabilityR31.unreachable(m, marked));
        for (String a : new String[]{"model", "checkpoint", "frozen"}) assertFalse("rooted " + a + " is never claimed", StorageGcR31.claim(m, a, m.version(a), 100));
        for (String a : new String[]{"cycleA", "cycleB"}) assertTrue(StorageGcR31.claim(m, a, m.version(a), 100));
        // race 1: the pin comes first -> the claim at the version it observed fails
        long observed = m.version("orphan"); assertTrue(m.addEdgeIfLive("model", "orphan"));
        assertFalse("pinned after observation: the claim loses", StorageGcR31.claim(m, "orphan", observed, 100));
        assertEquals(StorageGcR31.State.LIVE, m.state("orphan"));
        // race 2: the claim comes first -> a later pin is refused
        Mem n = fixture(); assertTrue(StorageGcR31.claim(n, "orphan", n.version("orphan"), 100));
        assertFalse("claimed: the pin loses", n.addEdgeIfLive("model", "orphan"));
        try { EvidenceReachabilityR31.mark(m, 2); fail("partial mark used"); } catch (IllegalStateException e) { assertTrue(e.getMessage().startsWith("R31_GC_MARK_BOUND_EXCEEDED")); }
    }

    @Test public void everyStepRecoversAfterACrashAndNothingIsReportedReleasedBeforeTheUnlink() {
        Mem m = fixture(); assertTrue(StorageGcR31.claim(m, "orphan", 0, 100));
        // crash after claim, before unlink: state DELETING, file present, nothing released yet
        m.failUnlink = true; assertEquals("a failed unlink releases nothing", 0, StorageGcR31.unlink(m, m, "orphan"));
        assertEquals(StorageGcR31.State.DELETING, m.state("orphan")); assertTrue(m.receipts.contains("orphan:TOMBSTONE_RETRY")); assertTrue(m.exists("orphan"));
        assertFalse("the tombstoned path is not re-accepted", m.addEdgeIfLive("model", "orphan"));
        m.failUnlink = false; assertEquals(100, StorageGcR31.recover(m, m, Collections.singleton("orphan")));
        assertEquals(StorageGcR31.State.DELETED, m.state("orphan")); assertFalse(m.exists("orphan"));
        // crash after the unlink but before the DELETED record: recovery finishes without double release
        Mem k = fixture(); assertTrue(StorageGcR31.claim(k, "cycleA", 0, 100)); k.files.remove("cycleA");
        assertEquals(100, StorageGcR31.recover(k, k, Collections.singleton("cycleA"))); assertEquals(0, StorageGcR31.recover(k, k, Collections.singleton("cycleA")));
        try { StorageGcR31.unlink(k, k, "model"); fail("unclaimed unlink"); } catch (IllegalStateException e) { assertEquals("R31_GC_NOT_CLAIMED:model", e.getMessage()); }
    }

    @Test public void protectedRootsOverTheBudgetBlockWithReleaseConditions() {
        Mem m = fixture(); m.roots.put("frozen", EvidenceReachabilityR31.RootKind.PENDING_FREEZE);
        Map<String, Object> ok = EvidenceReachabilityR31.protectedBudget(m, EvidenceReachabilityR31.mark(m, 100), 1000);
        assertEquals("WITHIN_PROTECTED_BUDGET", ok.get("status"));
        Map<String, Object> blocked = EvidenceReachabilityR31.protectedBudget(m, EvidenceReachabilityR31.mark(m, 100), 250);
        assertEquals("BLOCKED_PROTECTED_BYTES", blocked.get("status")); assertEquals(300L, blocked.get("rootedBytes")); assertEquals(0L, blocked.get("deletedRootedBytes"));
        assertTrue(blocked.get("releaseConditions").toString().contains("THE_FROZEN_DECISION_IS_SETTLED"));
        for (EvidenceReachabilityR31.RootKind k : EvidenceReachabilityR31.RootKind.values()) assertNotNull(k.name(), EvidenceReachabilityR31.RELEASE_CONDITION.get(k));
    }
}
