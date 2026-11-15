package com.ethk3.novaonepro;

import java.util.*;

/**
 * R31-B05-C05: claim -> detach -> unlink -> recover for unreachable artifacts.
 *
 * <p>The claim runs in the single writer: it re-marks from the roots and moves LIVE to DELETING only with the version
 * it observed, so a pin (which adds an edge only to a LIVE artifact and bumps its version) and a claim cannot both
 * succeed. File IO happens outside the transaction; each step leaves a durable receipt. A failed unlink keeps a
 * tombstone (DELETING) for retry and the path is not reused; bytes count as released only once DELETED.</p>
 */
public final class StorageGcR31 {
    private StorageGcR31() {}
    public enum State { LIVE, DELETING, DELETED }
    /** Transactional metadata of the single writer. */
    public interface Store extends EvidenceReachabilityR31.Graph {
        State state(String artifact); long version(String artifact);
        /** Atomic compare-and-set of state at an exact version (bumps the version). */
        boolean compareAndSet(String artifact, State expected, long expectedVersion, State next);
        /** Pin: adds a reference only to a LIVE artifact (bumps its version); refused otherwise. */
        boolean addEdgeIfLive(String from, String to);
        void detach(String artifact);
        void receipt(String artifact, String phase);
    }
    public interface Files { boolean unlink(String artifact); boolean exists(String artifact); }

    /** Claims an artifact still unreachable at the version the caller observed. */
    public static boolean claim(Store s, String artifact, long observedVersion, int maxMark) {
        if (s.state(artifact) != State.LIVE) return false;
        if (EvidenceReachabilityR31.mark(s, maxMark).contains(artifact)) return false;   // re-mark inside the writer
        if (!s.compareAndSet(artifact, State.LIVE, observedVersion, State.DELETING)) return false;
        s.detach(artifact); s.receipt(artifact, "CLAIMED_DELETING"); return true;
    }
    /** Outside the transaction. Returns the bytes actually released (0 until the unlink really happened). */
    public static long unlink(Store s, Files f, String artifact) {
        if (s.state(artifact) != State.DELETING) throw new IllegalStateException("R31_GC_NOT_CLAIMED:" + artifact);
        long bytes = s.bytes(artifact);
        if (!f.unlink(artifact) && f.exists(artifact)) { s.receipt(artifact, "TOMBSTONE_RETRY"); return 0; }
        return completeUnlink(s, artifact, s.version(artifact));
    }
    /** The IO owner invokes this in one writer transaction only after the exact claimed
     * file is verified absent. State and the release receipt therefore commit together. */
    public static long completeUnlink(Store s, String artifact, long expectedVersion) {
        if(s.state(artifact)!=State.DELETING)throw new IllegalStateException("R31_GC_NOT_CLAIMED:"+artifact);
        long bytes=s.bytes(artifact);
        if(!s.compareAndSet(artifact,State.DELETING,expectedVersion,State.DELETED))throw new IllegalStateException("R31_GC_STATE_RACE:"+artifact);
        s.receipt(artifact,"UNLINKED");return bytes;
    }
    /** After a crash: finish DELETING artifacts whose file is gone; retry the others. Returns bytes released now. */
    public static long recover(Store s, Files f, Collection<String> deleting) {
        long released = 0;
        for (String a : deleting) if (s.state(a) == State.DELETING) released += unlink(s, f, a);
        return released;
    }
}
